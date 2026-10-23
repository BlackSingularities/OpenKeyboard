package com.blacksingularities.openkeyboard.ime

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EmojiFlags
import androidx.compose.material.icons.rounded.EmojiFoodBeverage
import androidx.compose.material.icons.rounded.EmojiNature
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.material.icons.rounded.EmojiPeople
import androidx.compose.material.icons.rounded.EmojiSymbols
import androidx.compose.material.icons.rounded.EmojiTransportation
import androidx.compose.material.icons.rounded.FirstPage
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LastPage
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material.icons.rounded.Functions
import com.blacksingularities.openkeyboard.data.EmojiKey
import com.blacksingularities.openkeyboard.data.KeySound
import com.blacksingularities.openkeyboard.features.FancyStyle
import com.blacksingularities.openkeyboard.features.TextTools
import com.blacksingularities.openkeyboard.keyboard.FieldKind
import kotlin.math.roundToInt
import com.blacksingularities.openkeyboard.data.Autocorrect
import com.blacksingularities.openkeyboard.data.KeyStyle
import com.blacksingularities.openkeyboard.data.Languages
import com.blacksingularities.openkeyboard.data.OneHanded
import com.blacksingularities.openkeyboard.dict.UserModel
import com.blacksingularities.openkeyboard.emoji.EmojiData
import com.blacksingularities.openkeyboard.features.Clip
import com.blacksingularities.openkeyboard.features.ClipboardHistory
import com.blacksingularities.openkeyboard.i18n.tr
import com.blacksingularities.openkeyboard.keyboard.KeyboardThemes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ───────────────────────── szybkie ustawienia ─────────────────────────

/** Jedna z opcji kafelka (pokazywane po przytrzymaniu). */
private class QOption(val label: String, val selected: Boolean, val swatch: Color? = null, val apply: () -> Unit)

/**
 * Kafelek szybkich ustawień: dotknięcie – [action] (przełącz / następna wartość),
 * przytrzymanie – wszystkie [options] naraz, wybór przesunięciem palca.
 */
private class QTile(
    val label: String,
    val value: String?,
    val on: Boolean,
    val swatch: Color? = null,
    val options: List<QOption> = emptyList(),
    val action: () -> Unit,
)

@Composable
private fun quickTiles(c: KeyboardController): List<QTile> {
    val s = c.settings
    val theme = KeyboardThemes.presets.firstOrNull { it.id == s.theme }
    val themeName = if (s.theme == "custom") tr("Własny motyw") else tr(theme?.name ?: KeyboardThemes.presets.first().name)
    fun set(t: (com.blacksingularities.openkeyboard.data.KeyboardSettings) -> com.blacksingularities.openkeyboard.data.KeyboardSettings) = c.updateSettings(t)
    fun onOff(on: Boolean, apply: (Boolean) -> Unit) = listOf(
        QOption(tr("Wł."), on) { apply(true) },
        QOption(tr("Wył."), !on) { apply(false) },
    )
    fun styleName(k: KeyStyle) = when (k) { KeyStyle.FILLED -> tr("Pełne"); KeyStyle.OUTLINE -> tr("Obrys"); KeyStyle.FLAT -> tr("Płaskie"); KeyStyle.RAISED -> "3D" }
    fun acName(a: Autocorrect) = when (a) { Autocorrect.OFF -> tr("Wył."); Autocorrect.GENTLE -> tr("Łagodna"); Autocorrect.NORMAL -> tr("Normalna"); Autocorrect.AGGRESSIVE -> tr("Mocna") }
    fun soundName(k: KeySound) = when (k) { KeySound.SYSTEM -> tr("Systemowy"); KeySound.CLICK -> tr("Klik"); KeySound.SOFT -> tr("Miękki"); KeySound.TYPEWRITER -> tr("Maszyna"); KeySound.BUBBLE -> tr("Bańka") }
    fun emojiName(e: EmojiKey) = when (e) { EmojiKey.OFF -> tr("Brak"); EmojiKey.SEPARATE -> tr("Osobny"); EmojiKey.COMBINED -> tr("Z ?123") }
    fun handName(o: OneHanded) = when (o) { OneHanded.OFF -> tr("Wył."); OneHanded.LEFT -> tr("Lewa"); OneHanded.RIGHT -> tr("Prawa") }
    val accent = KeyboardThemes.accentOf(s)
    return buildList {
        add(QTile(tr("Motyw"), themeName, true, options = KeyboardThemes.presets.map { p ->
            QOption(tr(p.name), p.id == s.theme, p.build(accent).background) { set { it.copy(theme = p.id, autoDayNight = false) } }
        } + QOption(tr("Własny motyw"), s.theme == "custom", Color(s.customBg.toInt())) { set { it.copy(theme = "custom", autoDayNight = false) } }) {
            val i = KeyboardThemes.presets.indexOfFirst { it.id == s.theme }
            set { it.copy(theme = KeyboardThemes.presets[(i + 1) % KeyboardThemes.presets.size].id, autoDayNight = false) }
        })
        add(QTile(tr("Akcent"), null, false, swatch = accent, options = KeyboardThemes.accents.mapIndexed { i, col ->
            QOption("", s.accentColor == 0L && s.accent == i, col) { set { it.copy(accent = i, accentColor = 0) } }
        }) { set { it.copy(accent = (it.accent + 1) % KeyboardThemes.accents.size, accentColor = 0) } })
        add(QTile(tr("Litery w akcencie"), null, s.accentLabels, options = onOff(s.accentLabels) { v -> set { it.copy(accentLabels = v) } }) {
            set { it.copy(accentLabels = !it.accentLabels) }
        })
        add(QTile(tr("Płaska czarna"), null, s.theme == "flat") {
            set { it.copy(theme = if (it.theme == "flat") "black" else "flat", autoDayNight = false) }
        })
        add(QTile(tr("Klawisze"), styleName(s.keyStyle), true, options = KeyStyle.entries.map { k -> QOption(styleName(k), k == s.keyStyle) { set { it.copy(keyStyle = k) } } }) {
            set { it.copy(keyStyle = KeyStyle.entries[(it.keyStyle.ordinal + 1) % KeyStyle.entries.size]) }
        })
        add(QTile(tr("Autokorekta"), acName(s.autocorrect), s.autocorrect != Autocorrect.OFF, options = Autocorrect.entries.map { a -> QOption(acName(a), a == s.autocorrect) { set { it.copy(autocorrect = a) } } }) {
            set { it.copy(autocorrect = Autocorrect.entries[(it.autocorrect.ordinal + 1) % Autocorrect.entries.size]) }
        })
        add(QTile(tr("Podpowiedzi"), if (s.suggestions) tr("Wł.") else tr("Wył."), s.suggestions, options = listOf(
            QOption(tr("Wył."), !s.suggestions) { set { it.copy(suggestions = false) } },
            QOption(tr("Wł."), s.suggestions) { set { it.copy(suggestions = true) } },
        )) { set { it.copy(suggestions = !it.suggestions) } })
        if (s.languages.size > 1) {
            // Wiele naraz z jednym alfabetem – nie ma czego przełączać, dotknięcie pokazuje języki.
            val switchable = c.switchTargets.size > 1
            add(QTile(tr("Język"), if (s.multilingual) s.languages.joinToString(" · ") { it.uppercase() } else Languages.get(s.activeLanguage).native, true, options = s.languages.map { code ->
                QOption(Languages.get(code).native, code == s.activeLanguage) { c.setLanguage(code) }
            } + QOption(tr("Wiele naraz"), s.multilingual) { set { it.copy(multilingual = !it.multilingual) } }) {
                if (switchable) c.nextLanguage() else c.openPanel(Panel.LANGUAGES)
            })
        }
        add(QTile(tr("Klawisz emoji"), emojiName(s.emojiKeyMode), s.emojiKeyMode != EmojiKey.OFF, options = EmojiKey.entries.map { e -> QOption(emojiName(e), e == s.emojiKeyMode) { set { it.copy(emojiKeyMode = e) } } }) {
            set { it.copy(emojiKeyMode = EmojiKey.entries[(it.emojiKeyMode.ordinal + 1) % EmojiKey.entries.size]) }
        })
        add(QTile(tr("Symbole na literach"), null, s.symbolKeys, options = onOff(s.symbolKeys) { v -> set { it.copy(symbolKeys = v) } }) { set { it.copy(symbolKeys = !it.symbolKeys) } })
        add(QTile(tr("Rząd cyfr"), null, s.numberRow, options = onOff(s.numberRow) { v -> set { it.copy(numberRow = v) } }) { set { it.copy(numberRow = !it.numberRow) } })
        add(QTile(tr("Rozmiar"), "${s.keyHeight}%", true, options = (70..140 step 10).map { h ->
            QOption("$h%", s.keyHeight == h) { set { it.copy(keyHeight = h) } }
        } + QOption(tr("Przeciągnij…"), false) { c.openPanel(Panel.RESIZE) }) { c.openPanel(Panel.RESIZE) })
        add(QTile(tr("Wibracja"), if (s.haptics) "${s.hapticStrength}%" else tr("Wył."), s.haptics, options = listOf(QOption(tr("Wył."), !s.haptics) { set { it.copy(haptics = false) } }) +
            listOf(10, 25, 50, 75, 100).map { v -> QOption("$v%", s.haptics && s.hapticStrength == v) { set { it.copy(haptics = true, hapticStrength = v) } } }) {
            set { it.copy(haptics = !it.haptics) }
        })
        add(QTile(tr("Dźwięk"), if (s.sound) soundName(s.keySound) else tr("Wył."), s.sound, options = listOf(QOption(tr("Wył."), !s.sound) { set { it.copy(sound = false) } }) +
            KeySound.entries.map { k -> QOption(soundName(k), s.sound && s.keySound == k) { set { it.copy(sound = true, keySound = k) } } }) {
            set { it.copy(sound = !it.sound) }
        })
        add(QTile(tr("Jedna ręka"), handName(s.oneHanded), s.oneHanded != OneHanded.OFF, options = OneHanded.entries.map { o -> QOption(handName(o), o == s.oneHanded) { set { it.copy(oneHanded = o) } } }) { c.toggleOneHanded() })
        add(QTile(tr("Pływająca"), null, s.floating) { c.toggleFloating() })
        add(QTile(tr("Incognito"), null, s.forceIncognito) { c.toggleIncognito() })
        add(QTile(tr("Strzałki"), null, s.arrowRow, options = onOff(s.arrowRow) { v -> set { it.copy(arrowRow = v) } }) { set { it.copy(arrowRow = !it.arrowRow) } })
        add(QTile(tr("Gest"), null, s.glideTyping, options = onOff(s.glideTyping) { v -> set { it.copy(glideTyping = v) } }) { set { it.copy(glideTyping = !it.glideTyping) } })
    }
}

/** Układ okienka z opcjami: prostokąty komórek w układzie współrzędnych panelu. */
private fun chooserCells(n: Int, w: Float, h: Float, pad: Float, titleH: Float, maxCell: Float): List<androidx.compose.ui.geometry.Rect> {
    if (n == 0) return emptyList()
    val cols = when {
        n <= 3 -> n
        n <= 8 -> 4
        n <= 15 -> 5
        else -> 6
    }
    val rows = (n + cols - 1) / cols
    val cellW = (w - pad * 2) / cols
    val cellH = ((h - titleH - pad * 2) / rows).coerceAtMost(maxCell)
    val top = titleH + pad
    return List(n) { i ->
        val r = i / cols
        val col = i % cols
        androidx.compose.ui.geometry.Rect(pad + col * cellW, top + r * cellH, pad + (col + 1) * cellW, top + (r + 1) * cellH)
    }
}

@Composable
fun QuickPanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    val tiles = quickTiles(c)
    val density = LocalDensity.current
    val longMs = c.settings.longPressMs.toLong()
    // Otwarte okienko opcji: indeks kafelka; [hover] – opcja pod palcem.
    var open by remember { mutableStateOf<Int?>(null) }
    var hover by remember { mutableStateOf(-1) }
    var panelOrigin by remember { mutableStateOf(Offset.Zero) }
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    val tileOrigins = remember { mutableStateMapOf<Int, Offset>() }
    val pad = with(density) { 8.dp.toPx() }
    val titleH = with(density) { 30.dp.toPx() }
    val maxCell = with(density) { 46.dp.toPx() }
    val currentTiles by rememberUpdatedState(tiles)

    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .onGloballyPositioned {
                panelOrigin = it.positionInRoot()
                panelSize = it.size
            },
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tiles.chunked(4).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { i, q ->
                            val idx = r * 4 + i
                            val active = q.on && q.value == null && q.swatch == null
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (active) colors.accent else colors.key)
                                    .onGloballyPositioned { tileOrigins[idx] = it.positionInRoot() }
                                    .pointerInput(idx) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var tapped = false
                                            val finished = withTimeoutOrNull(longMs) {
                                                tapped = waitForUpOrCancellation() != null
                                                true
                                            } != null
                                            val tile = currentTiles.getOrNull(idx) ?: return@awaitEachGesture
                                            if (finished) {
                                                if (tapped) {
                                                    c.feedback.key()
                                                    tile.action()
                                                }
                                                return@awaitEachGesture
                                            }
                                            if (tile.options.isEmpty()) return@awaitEachGesture
                                            // Przytrzymanie: wszystkie opcje, wybór przesunięciem palca.
                                            c.feedback.long()
                                            open = idx
                                            hover = tile.options.indexOfFirst { it.selected }
                                            val origin = (tileOrigins[idx] ?: Offset.Zero) - panelOrigin
                                            val cells = chooserCells(tile.options.size, panelSize.width.toFloat(), panelSize.height.toFloat(), pad, titleH, maxCell)
                                            var moved = false
                                            while (true) {
                                                val ev = awaitPointerEvent()
                                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                                val pos = origin + ch.position
                                                if ((ch.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                                                val h = cells.indexOfFirst { it.contains(pos) }
                                                if (moved && h != hover) {
                                                    hover = h
                                                    if (h >= 0) c.feedback.tick()
                                                }
                                                ch.consume()
                                                if (!ch.pressed) break
                                            }
                                            if (moved) {
                                                tile.options.getOrNull(hover)?.apply?.invoke()
                                                open = null
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val fg = if (active) colors.onAccent else colors.text
                                Text(q.label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                when {
                                    q.swatch != null -> Box(Modifier.padding(top = 3.dp).size(10.dp).clip(CircleShape).background(q.swatch))
                                    q.value != null -> Text(q.value, color = colors.textDim, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    else -> Text(if (q.on) tr("Wł.") else tr("Wył."), color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                                if (q.options.isNotEmpty()) {
                                    // Kropki: kafelek ma więcej opcji pod przytrzymaniem.
                                    Text("•••", color = fg.copy(alpha = 0.35f), fontSize = 7.sp, lineHeight = 8.sp)
                                }
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PanelTextButton(tr("Wszystkie ustawienia"), icon = Icons.Rounded.Settings) { c.openApp() }
                Text(tr("Przytrzymaj kafelek – wszystkie opcje"), color = colors.textDim, fontSize = 10.sp, maxLines = 2, modifier = Modifier.weight(1f))
                PanelTextButton("ABC") { c.closePanel() }
            }
        }

        // Okienko opcji przytrzymanego kafelka – na całym panelu.
        val oi = open
        val tile = oi?.let { tiles.getOrNull(it) }
        if (tile != null) {
            val cells = chooserCells(tile.options.size, panelSize.width.toFloat(), panelSize.height.toFloat(), pad, titleH, maxCell)
            Box(
                Modifier
                    .matchParentSize()
                    .background(colors.background.copy(alpha = 0.96f))
                    .tap { open = null },
            ) {
                Text(
                    tile.label,
                    color = colors.textDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 14.dp, top = 8.dp),
                )
                tile.options.forEachIndexed { i, o ->
                    val r = cells[i]
                    val sel = i == hover
                    Box(
                        Modifier
                            .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                            .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                            .padding(3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) colors.accent else colors.key)
                            .tap {
                                o.apply()
                                c.feedback.key()
                                open = null
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                            if (o.swatch != null) {
                                Box(
                                    Modifier
                                        .size(if (o.label.isEmpty()) 22.dp else 12.dp)
                                        .clip(CircleShape)
                                        .background(if (o.selected) colors.text else colors.textDim.copy(alpha = 0.4f))
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(o.swatch),
                                )
                                if (o.label.isNotEmpty()) Spacer(Modifier.width(5.dp))
                            } else if (o.selected) {
                                Box(Modifier.size(5.dp).clip(CircleShape).background(if (sel) colors.onAccent else colors.accent))
                                Spacer(Modifier.width(5.dp))
                            }
                            if (o.label.isNotEmpty()) {
                                Text(
                                    o.label,
                                    color = if (sel) colors.onAccent else colors.text,
                                    fontSize = 12.sp,
                                    fontWeight = if (o.selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
