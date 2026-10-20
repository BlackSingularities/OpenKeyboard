package com.blacksingularities.openkeyboard.ime

import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.KeyboardCapslock
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openkeyboard.data.GestureAction
import com.blacksingularities.openkeyboard.data.KeyFont
import com.blacksingularities.openkeyboard.data.KeyStyle
import com.blacksingularities.openkeyboard.data.SplitMode
import com.blacksingularities.openkeyboard.dict.Pt
import com.blacksingularities.openkeyboard.dict.normalizeKey
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.keyboard.KbColors
import com.blacksingularities.openkeyboard.keyboard.Key
import com.blacksingularities.openkeyboard.keyboard.KeyType
import com.blacksingularities.openkeyboard.keyboard.KeyboardLayout
import com.blacksingularities.openkeyboard.keyboard.KeyboardMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Po tylu powtórzeniach przytrzymany Backspace przechodzi na usuwanie całych słów. */
const val DELETE_WORDS_AFTER = 8

/** Położenie klawisza w pikselach (w obrębie siatki klawiszy). */

class KeyRect(val id: Int, val key: Key, val row: Int, val rect: Rect)

/** Okienko z opcjami przytrzymanego klawisza. */
class PopupState(val owner: Int, val options: List<String>, val cell: Float, val cellH: Float, val left: Float, val bottom: Float, val perRow: Int) {
    var selected by mutableStateOf(0)
    val rows: Int get() = ceil(options.size / perRow.toFloat()).toInt()
}

/** Wysokość rzędu klawiszy (dp) wg ustawień i trybu. */
fun rowHeight(s: KeyboardSettings, mode: KeyboardMode, landscape: Boolean): Dp {
    val base = if (mode == KeyboardMode.NUMBER || mode == KeyboardMode.PHONE) 56f else 52f
    val l = if (landscape) 0.78f else 1f
    return (base * s.keyHeight / 100f * l).dp
}

/** Funkcje wywoływane przez dotyk – w podglądzie ustawień ich nie ma. */
interface KeyActions {
    fun press(key: Key)
    fun commit(key: Key)
    fun text(t: String)
    fun deleteDown()
    fun deleteRepeat()
    fun longPress(key: Key): Boolean
    fun cursor(steps: Int)
    fun selectBack(words: Int)
    fun selectDone(delete: Boolean)
    fun hide()
    fun tick()
    fun long()
    /** Słowo napisane gestem: ścieżka palca, środki liter, szerokość klawisza. */
    fun glide(path: List<Pt>, centers: Map<Char, Pt>, keyW: Float)
    fun gesture(action: GestureAction)
    /** Następny (1) lub poprzedni (-1) język – przesuwanie przytrzymanej spacji. Fałsz, gdy jest jeden. */
    fun languageStep(dir: Int): Boolean
}

/**
 * Siatka klawiszy: rysowanie i dotyk wieloma palcami (szybkie pisanie dwoma kciukami),
 * przytrzymanie (znaki dodatkowe), przesuwanie palca po spacji (kursor) i po Backspace
 * (zaznaczanie słów do usunięcia), przeciągnięcie w dół (schowanie klawiatury).
 */
@Composable
fun KeyGrid(
    layout: KeyboardLayout,
    settings: KeyboardSettings,
    colors: KbColors,
    shift: ShiftState,
    enterAction: Int,
    multiLine: Boolean,
    landscape: Boolean,
    actions: KeyActions?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val rowH = rowHeight(settings, layout.mode, landscape)
    val scope = rememberCoroutineScope()
    val pressed = remember { mutableStateMapOf<Int, Boolean>() }
    var preview by remember { mutableStateOf<KeyRect?>(null) }
    var popup by remember { mutableStateOf<PopupState?>(null) }
    val act by rememberUpdatedState(actions)
    val upper = shift != ShiftState.OFF
    var trail by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val split = layout.mode != KeyboardMode.NUMBER && layout.mode != KeyboardMode.PHONE &&
        (settings.split == SplitMode.ALWAYS || (settings.split == SplitMode.LANDSCAPE && landscape))

    BoxWithConstraints(modifier.fillMaxWidth().height(rowH * layout.rows.size)) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = with(density) { rowH.toPx() }
        // Klawiatura dzielona: przerwa na środku (w jednostkach klawisza), spacja się rozciąga.
        val gapUnits = if (split) settings.splitGap / 10f else 0f
        val unit = wPx / (layout.units + gapUnits)
        val rects = remember(layout, wPx, hPx, gapUnits) {
            var id = 0
            val mid = layout.units / 2f
            layout.rows.flatMapIndexed { r, row ->
                var u = (layout.units - row.sumOf { it.width.toDouble() }.toFloat()) / 2f
                row.map { k ->
                    val start = u
                    val end = u + k.width
                    u = end
                    val (l, rr) = when {
                        gapUnits == 0f -> start to end
                        end <= mid + 0.01f -> start to end
                        start >= mid - 0.01f -> start + gapUnits to end + gapUnits
                        else -> start to end + gapUnits
                    }
                    KeyRect(id++, k, r, Rect(l * unit, r * hPx, rr * unit, (r + 1) * hPx))
                }
            }
        }
        val gapPx = with(density) { settings.keyGap.dp.toPx() }
        val tracker = remember(rects) {
            TouchTracker(rects, layout, hPx, density.density, scope, { act }, settings) { p, k, pv ->
                if (p != null) pressed[p.first] = p.second
                if (k) popup = null
                preview = pv
            }
        }
        tracker.settings = settings
        tracker.upper = upper
        tracker.onPopup = { popup = it }
        tracker.onTrail = { trail = it }
        tracker.keyW = unit
        tracker.popupGetter = { popup }

        Box(
            Modifier
                .matchParentSize()
                .then(
                    if (actions == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(tracker) {
                            awaitPointerEventScope {
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    for (ch in ev.changes) {
                                        val id = ch.id.value
                                        when {
                                            ch.pressed && !ch.previousPressed -> tracker.down(id, ch.position)
                                            !ch.pressed && ch.previousPressed -> tracker.up(id, ch.position)
                                            ch.pressed && ch.positionChanged() -> tracker.move(id, ch.position)
                                        }
                                        ch.consume()
                                    }
                                }
                            }
                        }
                    },
                ),
        ) {
            for (kr in rects) {
                KeyView(kr, settings, colors, upper, shift, enterAction, multiLine, pressed[kr.id] == true, gapPx, layout.mode)
            }
        }

        // Ślad palca przy pisaniu gestem.
        if (trail.size > 1 && settings.glideTrail) {
            val stroke = with(density) { 5.dp.toPx() }
            Canvas(Modifier.matchParentSize()) {
                val path = Path()
                path.moveTo(trail[0].x, trail[0].y)
                for (i in 1 until trail.size) path.lineTo(trail[i].x, trail[i].y)
                drawPath(path, colors.accent.copy(alpha = 0.75f), style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        // Podgląd naciśniętego znaku nad klawiszem.
        val pv = preview
        if (pv != null && popup == null && settings.keyPreview) {
            val w = pv.rect.width * 1.25f
            val h = hPx * 1.25f
            val label = keyLabel(pv.key, upper)
            Box(
                Modifier
                    .offset { IntOffset((pv.rect.center.x - w / 2).roundToInt().coerceIn(0, (wPx - w).roundToInt()), (pv.rect.top - h - gapPx).roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                    .clip(RoundedCornerShape(settings.keyRadius.dp + 2.dp))
                    .background(colors.popup),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = colors.text, fontSize = (30 * settings.fontScale / 100f).sp, fontWeight = FontWeight.Medium)
            }
        }

        // Znaki dodatkowe (przytrzymanie).
        val pp = popup
        if (pp != null) {
            val cellDp = with(density) { pp.cell.toDp() }
            val cellH = with(density) { pp.cellH.toDp() }
            val totalH = pp.cellH * pp.rows
            Column(
                Modifier
                    .offset { IntOffset(pp.left.roundToInt(), (pp.bottom - totalH).roundToInt()) }
                    .clip(RoundedCornerShape(settings.keyRadius.dp + 4.dp))
                    .background(colors.popup)
                    .padding(2.dp),
            ) {
                // Pierwszy rząd opcji najbliżej klawisza – czyli na dole.
                for (r in (pp.rows - 1) downTo 0) {
                    Row {
                        for (c in 0 until pp.perRow) {
                            val i = r * pp.perRow + c
                            if (i >= pp.options.size) break
                            val sel = i == pp.selected
                            Box(
                                Modifier
                                    .size(cellDp - 4.dp / pp.perRow, cellH - 4.dp / pp.rows)
                                    .clip(RoundedCornerShape(settings.keyRadius.dp))
                                    .background(if (sel) colors.accent else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    pp.options[i],
                                    color = if (sel) colors.onAccent else colors.text,
                                    fontSize = ((if (pp.options[i].length > 2) 15 else 22) * settings.fontScale / 100f).sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Napis na klawiszu znaku. */
fun keyLabel(k: Key, upper: Boolean): String = when {
    k.type == KeyType.CHAR && upper && k.text != "ß" -> k.label.uppercase()
    else -> k.label
}

@Composable
private fun KeyView(
    kr: KeyRect,
    s: KeyboardSettings,
    colors: KbColors,
    upper: Boolean,
    shift: ShiftState,
    enterAction: Int,
    multiLine: Boolean,
    pressed: Boolean,
    gapPx: Float,
    mode: KeyboardMode,
) {
    val density = LocalDensity.current
    val k = kr.key
    val r = kr.rect
    val special = k.type != KeyType.CHAR && k.type != KeyType.SPACE && k.type != KeyType.TEXT
    val isAction = k.type == KeyType.ENTER && !multiLine && enterAction != EditorInfo.IME_ACTION_NONE && enterAction != EditorInfo.IME_ACTION_UNSPECIFIED
    val shiftOn = k.type == KeyType.SHIFT && shift != ShiftState.OFF
    val opacity = s.keyOpacity / 100f
    // Płasko: bez pól i obramowań – tylko przy naciśnięciu delikatne podświetlenie.
    val flat = colors.flat || s.keyStyle == KeyStyle.FLAT
    val filledAccent = (isAction || shiftOn) && !colors.flat
    val bg = when {
        pressed -> colors.keyPressed
        filledAccent -> colors.accent
        flat -> Color.Transparent
        special -> colors.keySpecial.copy(alpha = colors.keySpecial.alpha * opacity)
        else -> colors.key.copy(alpha = colors.key.alpha * opacity)
    }
    val fg = when {
        filledAccent && !pressed -> colors.onAccent
        colors.flat && (isAction || shiftOn) -> colors.accent
        else -> colors.text
    }
    val shape = RoundedCornerShape(s.keyRadius.dp)
    val half = gapPx / 2f
    // Klawisze 3D: ciemniejsza krawędź pod spodem.
    val raised = s.keyStyle == KeyStyle.RAISED && !flat && !pressed
    val shade = if (colors.light) Color.Black.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.55f)
    Box(
        Modifier
            .offset { IntOffset((r.left + half).roundToInt(), (r.top + half).roundToInt()) }
            .size(with(density) { (r.width - gapPx).coerceAtLeast(1f).toDp() }, with(density) { (r.height - gapPx).coerceAtLeast(1f).toDp() })
            .then(if (raised) Modifier.clip(shape).background(shade).padding(bottom = 2.dp) else Modifier)
            .clip(shape)
            .background(bg)
            .then(if (s.keyStyle == KeyStyle.OUTLINE && !colors.flat && !pressed && !isAction && !shiftOn) Modifier.border(1.dp, colors.textDim.copy(alpha = 0.35f), shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val scale = s.fontScale / 100f
        when (k.type) {
            KeyType.CHAR, KeyType.TEXT -> {
                val label = keyLabel(k, upper)
                val numpad = mode == KeyboardMode.NUMBER || mode == KeyboardMode.PHONE
                Text(
                    label,
                    color = if (fg == colors.text && k.type == KeyType.CHAR && !numpad) colors.keyLabel else fg,
                    fontFamily = keyFontFamily(s.keyFont),
                    fontSize = ((if (label.length > 2) 15 else if (numpad) 24 else 21) * scale).sp,
                    fontWeight = if (s.boldLabels) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
                val hint = k.hint
                if (hint != null && s.keyHints) {
                    if (numpad) {
                        Text(hint, color = colors.textDim, fontSize = (9 * scale).sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp))
                    } else {
                        Text(
                            if (upper) hint.uppercase() else hint,
                            color = colors.textDim,
                            fontSize = (10 * scale).sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 5.dp),
                        )
                    }
                }
                val hint2 = k.hint2
                if (hint2 != null && s.keyHints && !numpad) {
                    Text(
                        if (upper) hint2.uppercase() else hint2,
                        color = colors.textDim.copy(alpha = colors.textDim.alpha * 0.8f),
                        fontSize = (9 * scale).sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp, start = 5.dp),
                    )
                }
            }
            KeyType.SPACE -> {
                if (s.showSpaceLanguage && k.label.isNotEmpty()) {
                    Text(k.label, color = colors.textDim, fontSize = (12 * scale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            KeyType.SHIFT -> {
                KeyIcon(if (shift == ShiftState.LOCKED) Icons.Rounded.KeyboardCapslock else Icons.Rounded.ArrowUpward, fg)
                // Bez pól Shift nie zmienia tła – kropka pod strzałką pokazuje, że jest włączony.
                if (colors.flat && shiftOn) {
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(4.dp).clip(RoundedCornerShape(50)).background(fg))
                }
            }
            KeyType.DELETE -> KeyIcon(Icons.AutoMirrored.Rounded.Backspace, fg)
            KeyType.ENTER -> KeyIcon(enterIcon(enterAction, multiLine), fg)
            KeyType.LANGUAGE -> KeyIcon(Icons.Rounded.Language, fg)
            KeyType.EMOJI -> KeyIcon(Icons.Rounded.EmojiEmotions, fg)
            KeyType.NUMPAD -> KeyIcon(Icons.Rounded.Dialpad, fg)
            KeyType.SYMBOLS -> if (k.combo) {
                // Klawisz podwójny: dotknięcie – symbole, przytrzymanie – emoji. Ikona dzielona po skosie.
                Canvas(Modifier.matchParentSize()) {
                    drawLine(fg.copy(alpha = 0.25f), Offset(size.width * 0.78f, size.height * 0.18f), Offset(size.width * 0.22f, size.height * 0.82f), strokeWidth = 1.dp.toPx())
                }
                Text(
                    "?12",
                    color = fg,
                    fontSize = (11 * scale).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 5.dp, top = 3.dp),
                )
                Icon(Icons.Rounded.EmojiEmotions, null, tint = fg, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = 4.dp).size(15.dp))
            } else {
                Text(k.label, color = fg, fontSize = (14 * scale).sp, fontWeight = FontWeight.Medium, maxLines = 1, textAlign = TextAlign.Center)
            }
            KeyType.SYMBOLS2, KeyType.ALPHA -> Text(
                k.label,
                color = fg,
                fontSize = (14 * scale).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

fun keyFontFamily(f: KeyFont): FontFamily? = when (f) {
    KeyFont.DEFAULT -> null
    KeyFont.SANS -> FontFamily.SansSerif
    KeyFont.SERIF -> FontFamily.Serif
    KeyFont.MONO -> FontFamily.Monospace
    KeyFont.CONDENSED -> FontFamily.Cursive
}

@Composable
private fun KeyIcon(icon: ImageVector, tint: Color) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
}

fun enterIcon(action: Int, multiLine: Boolean): ImageVector = if (multiLine) {
    Icons.AutoMirrored.Rounded.KeyboardReturn
} else {
    when (action) {
        EditorInfo.IME_ACTION_SEARCH -> Icons.Rounded.Search
        EditorInfo.IME_ACTION_SEND -> Icons.AutoMirrored.Rounded.Send
        EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT -> Icons.AutoMirrored.Rounded.ArrowForward
        EditorInfo.IME_ACTION_DONE -> Icons.Rounded.Check
        else -> Icons.AutoMirrored.Rounded.KeyboardReturn
    }
}

private enum class TrackMode { NORMAL, POPUP, SPACE_CURSOR, DELETE_SWIPE, HIDE, DONE, LONG_DONE, GLIDE, SPACE_UP, SPACE_LANG, SPACE_HELD }

private class Track(val id: Long, var key: KeyRect, val down: Offset) {
    var mode = TrackMode.NORMAL
    var job: Job? = null
    var lastX = down.x
    var words = 0
    val points = ArrayList<Offset>()
}

/**
 * Obsługa dotyku klawiszy. [update] – zmiana wyglądu: (id klawisza, wciśnięty), zamknięcie okienka,
 * podgląd znaku.
 */
private class TouchTracker(
    val rects: List<KeyRect>,
    val layout: KeyboardLayout,
    val rowH: Float,
    val density: Float,
    val scope: CoroutineScope,
    val actions: () -> KeyActions?,
    var settings: KeyboardSettings,
    val update: (Pair<Int, Boolean>?, Boolean, KeyRect?) -> Unit,
) {
    var onPopup: (PopupState?) -> Unit = {}
    var upper = false
    var popupGetter: () -> PopupState? = { null }
    var onTrail: (List<Offset>) -> Unit = {}
    var keyW = 1f
    private val tracks = LinkedHashMap<Long, Track>()
    private val slop = 10f * density

    private fun hit(p: Offset): KeyRect? {
        if (rects.isEmpty()) return null
        val row = (p.y / rowH).toInt().coerceIn(0, layout.rows.size - 1)
        val inRow = rects.filter { it.row == row }
        return inRow.firstOrNull { p.x >= it.rect.left && p.x < it.rect.right }
            ?: inRow.minByOrNull { minOf(abs(p.x - it.rect.left), abs(p.x - it.rect.right)) }
    }

    fun down(id: Long, p: Offset) {
        val a = actions() ?: return
        val kr = hit(p) ?: return
        // Drugi palec przed puszczeniem pierwszego – pierwszy znak trafia od razu.
        for (t in tracks.values) {
            if (t.mode == TrackMode.NORMAL && (t.key.key.type == KeyType.CHAR || t.key.key.type == KeyType.TEXT)) {
                t.job?.cancel()
                a.commit(t.key.key)
                t.mode = TrackMode.DONE
                update(t.key.id to false, false, null)
            }
        }
        val t = Track(id, kr, p)
        tracks[id] = t
        a.press(kr.key)
        update(kr.id to true, false, if (kr.key.type == KeyType.CHAR) kr else null)
        when (kr.key.type) {
            KeyType.DELETE -> {
                a.deleteDown()
                t.job = scope.launch {
                    delay(380)
                    var n = 0
                    while (true) {
                        a.deleteRepeat()
                        n++
                        // Po kilku znakach przytrzymany Backspace usuwa całe słowa – wolniej, żeby dało się puścić w porę.
                        val words = settings.deleteWordsOnHold && n > DELETE_WORDS_AFTER
                        if (n == DELETE_WORDS_AFTER + 1 && settings.deleteWordsOnHold) a.long()
                        delay(if (words) 230L else (70 - n * 3).coerceAtLeast(35).toLong())
                    }
                }
            }
            else -> scheduleLong(t)
        }
    }

    private fun scheduleLong(t: Track) {
        t.job?.cancel()
        val k = t.key.key
        val hasLong = k.popup.isNotEmpty() || k.type in setOf(KeyType.SPACE, KeyType.SHIFT, KeyType.SYMBOLS, KeyType.LANGUAGE, KeyType.ENTER, KeyType.EMOJI) ||
            (k.text == "," && settings.commaLongPress != GestureAction.NONE)
        if (!hasLong) return
        t.job = scope.launch {
            delay(settings.longPressMs.toLong())
            onLong(t)
        }
    }

    private fun onLong(t: Track) {
        val a = actions() ?: return
        if (t.mode != TrackMode.NORMAL) return
        val k = t.key.key
        if (k.text == "," && settings.commaLongPress != GestureAction.NONE) {
            t.mode = TrackMode.LONG_DONE
            a.gesture(settings.commaLongPress)
            a.long()
            return
        }
        if (k.type == KeyType.SPACE && settings.spaceSlideLanguage) {
            // Przytrzymana spacja: przesunięcie w bok zmienia język, puszczenie bez ruchu – czynność przytrzymania.
            t.mode = TrackMode.SPACE_HELD
            t.lastX = t.down.x
            t.words = 0
            update(t.key.id to true, false, null)
            a.long()
            return
        }
        if (k.popup.isNotEmpty()) {
            val opts = if (upper) k.popup.map { if (it == "ß") it else it.uppercase() } else k.popup
            val cell = (t.key.rect.width.coerceAtMost(rowH * 1.1f)).coerceAtLeast(rowH * 0.8f)
            val perRow = minOf(opts.size, 8)
            val total = cell * perRow
            val maxW = rects.maxOf { it.rect.right }
            val left = (t.key.rect.center.x - cell / 2f).coerceAtMost(maxW - total).coerceAtLeast(0f)
            onPopup(PopupState(t.key.id, opts, cell, rowH, left, t.key.rect.top - 4f * density, perRow))
            t.mode = TrackMode.POPUP
            update(null, false, null)
            a.long()
        } else if (a.longPress(k)) {
            t.mode = TrackMode.LONG_DONE
            a.long()
        }
    }

    fun move(id: Long, p: Offset) {
        val a = actions() ?: return
        val t = tracks[id] ?: return
        val dx = p.x - t.down.x
        val dy = p.y - t.down.y
        when (t.mode) {
            TrackMode.POPUP -> {
                val pp = popupGetter() ?: return
                val col = ((p.x - pp.left) / pp.cell).toInt().coerceIn(0, pp.perRow - 1)
                val rowFromBottom = ((pp.bottom - p.y) / pp.cellH).toInt().coerceIn(0, pp.rows - 1)
                val i = (rowFromBottom * pp.perRow + col).coerceAtMost(pp.options.size - 1)
                if (i != pp.selected) {
                    pp.selected = i
                    a.tick()
                }
            }
            TrackMode.SPACE_HELD -> {
                val step = 42f * density
                val d = p.x - t.lastX
                if (abs(d) > step) {
                    if (a.languageStep(if (d > 0) 1 else -1)) {
                        t.words++
                        a.tick()
                    }
                    t.lastX = p.x
                }
            }
            TrackMode.SPACE_CURSOR -> {
                val step = 11f * density
                val steps = ((p.x - t.lastX) / step).toInt()
                if (steps != 0) {
                    a.cursor(steps)
                    t.lastX += steps * step
                    a.tick()
                }
            }
            TrackMode.GLIDE -> {
                t.points += p
                onTrail(t.points.toList())
            }
            TrackMode.DELETE_SWIPE -> {
                val w = ((-dx - slop) / (26f * density)).toInt().coerceAtLeast(0) + if (-dx > slop) 1 else 0
                if (w != t.words) {
                    t.words = w
                    if (w == 0) a.selectDone(delete = false) else a.selectBack(w)
                    a.tick()
                }
            }
            TrackMode.NORMAL -> {
                val k = t.key.key
                if (settings.swipeDown != GestureAction.NONE && dy > 64f * density && abs(dx) < dy / 2f && k.type != KeyType.DELETE && tracks.size == 1) {
                    t.job?.cancel()
                    t.mode = TrackMode.HIDE
                    update(t.key.id to false, false, null)
                    return
                }
                if (k.type == KeyType.SPACE && settings.spaceSwipeUp != GestureAction.NONE && -dy > rowH * 0.9f && abs(dx) < -dy) {
                    t.job?.cancel()
                    t.mode = TrackMode.SPACE_UP
                    update(t.key.id to false, false, null)
                    a.long()
                    return
                }
                if (k.type == KeyType.SPACE && settings.spaceSwipe != GestureAction.NONE && abs(dx) > slop * 1.5f) {
                    t.job?.cancel()
                    t.mode = if (settings.spaceSwipe == GestureAction.CURSOR) TrackMode.SPACE_CURSOR else TrackMode.SPACE_LANG
                    t.lastX = p.x
                    a.long()
                    return
                }
                // Pisanie gestem: palec przejechał dalej niż na sąsiedni klawisz.
                if (settings.glideTyping && layout.mode == KeyboardMode.ALPHA && tracks.size == 1 && k.type == KeyType.CHAR &&
                    k.text.length == 1 && k.text[0].isLetter() && hypot(dx, dy) > keyW * 1.1f
                ) {
                    t.job?.cancel()
                    t.mode = TrackMode.GLIDE
                    t.points.clear()
                    t.points += t.down
                    t.points += p
                    update(t.key.id to false, false, null)
                    onTrail(t.points.toList())
                    return
                }
                if (k.type == KeyType.DELETE && settings.deleteSwipe && dx < -slop * 2f) {
                    t.job?.cancel()
                    t.mode = TrackMode.DELETE_SWIPE
                    t.words = 0
                    move(id, p)
                    return
                }
                if (k.type == KeyType.CHAR || k.type == KeyType.TEXT) {
                    // Palec zjechał na sąsiedni klawisz – liczy się ten, z którego zostanie podniesiony.
                    val now = hit(p) ?: return
                    if (now.id != t.key.id && (now.key.type == KeyType.CHAR || now.key.type == KeyType.TEXT)) {
                        update(t.key.id to false, false, null)
                        t.key = now
                        update(now.id to true, false, if (now.key.type == KeyType.CHAR) now else null)
                        scheduleLong(t)
                    }
                }
            }
            else -> {}
        }
    }

    fun up(id: Long, p: Offset) {
        val a = actions()
        val t = tracks.remove(id) ?: return
        t.job?.cancel()
        update(t.key.id to false, false, null)
        if (a == null) return
        when (t.mode) {
            TrackMode.DONE, TrackMode.SPACE_CURSOR, TrackMode.LONG_DONE -> {}
            TrackMode.HIDE -> a.gesture(settings.swipeDown)
            TrackMode.SPACE_UP -> a.gesture(settings.spaceSwipeUp)
            TrackMode.SPACE_LANG -> a.gesture(settings.spaceSwipe)
            TrackMode.SPACE_HELD -> if (t.words == 0) a.longPress(t.key.key)

            TrackMode.GLIDE -> {
                t.points += p
                onTrail(emptyList())
                val centers = HashMap<Char, Pt>()
                for (kr in rects) {
                    if (kr.key.type == KeyType.CHAR && kr.key.text.length == 1 && kr.key.text[0].isLetter()) {
                        val c = normalizeKey(kr.key.text).firstOrNull() ?: continue
                        centers.putIfAbsent(c, Pt(kr.rect.center.x, kr.rect.center.y))
                    }
                }
                a.glide(t.points.map { Pt(it.x, it.y) }, centers, keyW)
            }
            TrackMode.DELETE_SWIPE -> a.selectDone(delete = t.words > 0)
            TrackMode.POPUP -> {
                val pp = popupGetter()
                onPopup(null)
                if (pp != null && pp.selected in pp.options.indices) a.text(pp.options[pp.selected])
            }
            TrackMode.NORMAL -> if (t.key.key.type != KeyType.DELETE) a.commit(t.key.key)
        }
        if (tracks.isEmpty()) update(null, true, null)
    }
}
