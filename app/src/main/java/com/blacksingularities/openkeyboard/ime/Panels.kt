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

// ───────────────────────── emoji ─────────────────────────

private const val TAB_RECENT = "recent"
private const val TAB_KAOMOJI = "kaomoji"
private const val TAB_SYMBOLS = "symbols"

private val kaomoji = listOf(
    "(◕‿◕)", "(｡◕‿◕｡)", "ʕ•ᴥ•ʔ", "(ᵔᴥᵔ)", "( ͡° ͜ʖ ͡°)", "¯\\_(ツ)_/¯", "(╯°□°)╯︵ ┻━┻", "┬─┬ノ( º _ ºノ)",
    "(ง'̀-'́)ง", "(づ｡◕‿‿◕｡)づ", "(>_<)", "(T_T)", "(ಥ﹏ಥ)", "(¬‿¬)", "(•_•)", "( •_•)>⌐■-■", "(⌐■_■)",
    "(✿◠‿◠)", "(*^▽^*)", "(≧◡≦)", "(^_^)v", "(￣▽￣)ノ", "ヽ(•‿•)ノ", "\\(^o^)/", "(o_O)", "(ーー;)",
    "(>‿<)", "(っ˘ω˘ς )", "(=^･ω･^=)", "ฅ^•ﻌ•^ฅ", "(ᗒᗣᗕ)՞", "♪~ ᕕ(ᐛ)ᕗ", "(*≧ω≦*)", "(；一_一)",
    "(ノ´ヮ`)ノ*: ･ﾟ", "(☞ﾟヮﾟ)☞", "☜(ﾟヮﾟ☜)", "(ʘ‿ʘ)", "(°ロ°)", "(￢_￢)", "<(￣︶￣)>", "♥‿♥",
)

private val symbols = (
    "← → ↑ ↓ ↔ ↕ ⇐ ⇒ ⇔ ↩ ↪ ⟵ ⟶ • · … ‰ ‱ † ‡ § ¶ © ® ™ ℠ ° ′ ″ ± × ÷ ≠ ≈ ≡ ≤ ≥ ∞ √ ∛ ∑ ∏ ∫ ∂ ∆ ∇ π µ Ω " +
        "α β γ δ ε θ λ σ φ ψ ω ℃ ℉ № ½ ⅓ ⅔ ¼ ¾ ⅛ ¹ ² ³ ⁴ ⁿ ₀ ₁ ₂ € £ ¥ ₽ ₴ ₹ ₿ ¢ ✓ ✔ ✗ ✘ ★ ☆ ♥ ♡ ♦ ♣ ♠ " +
        "☀ ☁ ☂ ☃ ☎ ✉ ✈ ⚡ ♪ ♫ ♀ ♂ ⚥ ☮ ☯ ✝ ☪ ✡ ⌘ ⌥ ⇧ ⌫ ⏎ ⌚ ⌛ ■ □ ▲ △ ▼ ▽ ◆ ◇ ○ ● ◎ ◉ ※ « » ‹ › „ ” “ ‚ ’ ‘ ‐ – —"
    ).split(' ')

private fun groupIcon(id: String): ImageVector = when (id) {
    TAB_RECENT -> Icons.Rounded.History
    "Smileys & Emotion" -> Icons.Rounded.EmojiEmotions
    "People & Body" -> Icons.Rounded.EmojiPeople
    "Animals & Nature" -> Icons.Rounded.EmojiNature
    "Food & Drink" -> Icons.Rounded.EmojiFoodBeverage
    "Travel & Places" -> Icons.Rounded.EmojiTransportation
    "Activities" -> Icons.Rounded.EmojiEvents
    "Objects" -> Icons.Rounded.EmojiObjects
    "Symbols" -> Icons.Rounded.EmojiSymbols
    "Flags" -> Icons.Rounded.EmojiFlags
    else -> Icons.Rounded.TextFields
}

@Composable
fun EmojiPanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    val recent by UserModel.emojiRecent.collectAsState()
    val groups = EmojiData.groups
    val tabs = listOf(TAB_RECENT) + groups.map { it.id } + listOf(TAB_KAOMOJI, TAB_SYMBOLS)
    var tab by remember { mutableStateOf(if (recent.isEmpty()) groups.firstOrNull()?.id ?: TAB_RECENT else TAB_RECENT) }
    var tones by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    Column(Modifier.fillMaxWidth().height(height)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val wide = tab == TAB_KAOMOJI
            val list: List<String> = when (tab) {
                TAB_RECENT -> recent
                TAB_KAOMOJI -> kaomoji
                TAB_SYMBOLS -> symbols
                else -> groups.firstOrNull { it.id == tab }?.emoji?.map { it.char }.orEmpty()
            }
            if (list.isEmpty()) {
                Text(
                    tr("Tu pojawią się ostatnio używane emoji"),
                    color = colors.textDim,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            key(tab) { LazyVerticalGrid(
                columns = if (wide) GridCells.Adaptive(110.dp) else GridCells.Adaptive(44.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            ) {
                items(list) { e ->
                    Box(
                        Modifier
                            .height(if (wide) 44.dp else 46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .tap(
                                onLong = {
                                    val t = EmojiData.tonesOf(e)
                                    if (t.isNotEmpty()) {
                                        c.feedback.long()
                                        tones = e to t
                                    }
                                },
                                onClick = {
                                    c.feedback.key()
                                    if (tab == TAB_KAOMOJI || tab == TAB_SYMBOLS) c.commitRaw(e) else c.commitEmoji(e)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e, fontSize = if (wide) 14.sp else if (tab == TAB_SYMBOLS) 20.sp else 26.sp, color = colors.text, maxLines = 1)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(8.dp)) }
            } }
            // Odcienie skóry wybranego emoji.
            val tn = tones
            if (tn != null) {
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.popup)
                        .padding(4.dp),
                ) {
                    (listOf(tn.first) + tn.second).forEach { v ->
                        Text(
                            v,
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .tap {
                                    c.commitEmoji(v)
                                    tones = null
                                }
                                .padding(6.dp),
                        )
                    }
                }
            }
        }
        // Dolny pasek: ABC, kategorie, Backspace.
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ABC",
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.keySpecial)
                    .tap { c.closePanel() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (t in tabs) {
                    val sel = t == tab
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (sel) colors.accent else Color.Transparent)
                            .tap {
                                tab = t
                                tones = null
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (t == TAB_KAOMOJI) {
                            Text("^‿^", fontSize = 11.sp, color = if (sel) colors.onAccent else colors.textDim, fontWeight = FontWeight.Bold)
                        } else if (t == TAB_SYMBOLS) {
                            Text("Ω", fontSize = 15.sp, color = if (sel) colors.onAccent else colors.textDim, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(groupIcon(t), t, tint = if (sel) colors.onAccent else colors.textDim, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
            RepeatIcon(Icons.AutoMirrored.Rounded.Backspace, tr("Usuń"), c) { c.onDelete(it) }
        }
    }
}

/** Przycisk z powtarzaniem przy przytrzymaniu (Backspace, strzałki). */
@Composable
fun RepeatIcon(icon: ImageVector, label: String, c: KeyboardController, modifier: Modifier = Modifier, size: Dp = 44.dp, action: (repeat: Boolean) -> Unit) {
    val colors = LocalKb.current
    val scope = rememberCoroutineScope()
    val act by rememberUpdatedState(action)
    var down by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (down) 0.88f else 1f, label = "rep")
    Box(
        modifier
            .size(size)
            .scale(sc)
            .clip(RoundedCornerShape(12.dp))
            .background(if (down) colors.keyPressed else colors.keySpecial)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    down = true
                    c.feedback.key()
                    act(false)
                    val job = scope.launch {
                        delay(380)
                        var n = 0
                        while (true) {
                            act(true)
                            n++
                            delay((70 - n * 3).coerceAtLeast(30).toLong())
                        }
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                    down = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = colors.text, modifier = Modifier.size(20.dp))
    }
}

// ───────────────────────── schowek ─────────────────────────

@Composable
fun ClipboardPanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    val items by ClipboardHistory.items.collectAsState()
    var selected by remember { mutableStateOf<Clip?>(null) }
    Column(Modifier.fillMaxWidth().height(height)) {
        if (!c.settings.clipboardHistory) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(tr("Historia schowka jest wyłączona. Włącz ją w ustawieniach (ikona obok)."), color = colors.textDim, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(tr("Skopiuj coś – tekst pojawi się tutaj. Przytrzymaj wpis, aby go przypiąć lub usunąć."), color = colors.textDim, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp)) {
                items(items, key = { it.time.toString() + it.text.hashCode() }) { clip ->
                    val sel = selected == clip
                    Column(
                        Modifier
                            .padding(3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) colors.keyPressed else colors.key)
                            .tap(
                                onLong = {
                                    c.feedback.long()
                                    selected = if (sel) null else clip
                                },
                                onClick = {
                                    c.feedback.key()
                                    c.commitRaw(clip.text)
                                },
                            )
                            .padding(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                clip.text,
                                color = colors.text,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (clip.pinned) Icon(Icons.Rounded.PushPin, tr("Przypięte"), tint = colors.accent, modifier = Modifier.size(14.dp))
                        }
                        if (sel) {
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniAction(Icons.Rounded.PushPin, if (clip.pinned) tr("Odepnij") else tr("Przypnij")) {
                                    ClipboardHistory.togglePin(clip)
                                    selected = null
                                }
                                MiniAction(Icons.Rounded.Delete, tr("Usuń")) {
                                    ClipboardHistory.remove(clip)
                                    selected = null
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            PanelTextButton("ABC") { c.closePanel() }
            Spacer(Modifier.weight(1f))
            if (items.any { !it.pinned }) {
                PanelTextButton(tr("Wyczyść (bez przypiętych)"), icon = Icons.Rounded.DeleteSweep) { ClipboardHistory.clear() }
            }
        }
    }
}

@Composable
private fun MiniAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalKb.current
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.keySpecial)
            .tap(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = colors.text, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = colors.text, fontSize = 11.sp)
    }
}

@Composable
private fun PanelTextButton(text: String, icon: ImageVector? = null, primary: Boolean = false, onClick: () -> Unit) {
    val colors = LocalKb.current
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (primary) colors.accent else colors.keySpecial)
            .tap(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (primary) colors.onAccent else colors.text, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = if (primary) colors.onAccent else colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// ───────────────────────── kursor i edycja ─────────────────────────

@Composable
fun EditPanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    Row(Modifier.fillMaxWidth().height(height).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Strzałki – krzyżak.
        Column(Modifier.weight(1.2f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditCell(Icons.Rounded.FirstPage, tr("Początek")) { c.cursorHomeEnd(false) }
                RepeatCell(Icons.Rounded.KeyboardArrowUp, tr("W górę"), c) { c.moveCursorVertical(false) }
                EditCell(Icons.Rounded.LastPage, tr("Koniec")) { c.cursorHomeEnd(true) }
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RepeatCell(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, tr("W lewo"), c) { c.moveCursor(-1) }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (c.selecting) colors.accent else colors.keySpecial)
                        .tap { c.selecting = !c.selecting },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tr("Zaznacz"), color = if (c.selecting) colors.onAccent else colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                RepeatCell(Icons.AutoMirrored.Rounded.KeyboardArrowRight, tr("W prawo"), c) { c.moveCursor(1) }
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditCell(Icons.AutoMirrored.Rounded.Undo, tr("Cofnij")) { c.undoRedo(false) }
                RepeatCell(Icons.Rounded.KeyboardArrowDown, tr("W dół"), c) { c.moveCursorVertical(true) }
                EditCell(Icons.AutoMirrored.Rounded.Redo, tr("Ponów")) { c.undoRedo(true) }
            }
        }
        // Operacje na tekście.
        Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EditWide(Icons.Rounded.SelectAll, tr("Zaznacz wszystko")) { c.contextAction(android.R.id.selectAll) }
            EditWide(Icons.Rounded.ContentCopy, tr("Kopiuj")) { c.contextAction(android.R.id.copy) }
            EditWide(Icons.Rounded.ContentCut, tr("Wytnij")) { c.contextAction(android.R.id.cut) }
            EditWide(Icons.Rounded.ContentPaste, tr("Wklej")) { c.contextAction(android.R.id.paste) }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditCell(Icons.Rounded.Keyboard, tr("Wróć do klawiatury")) { c.closePanel() }
                RepeatCell(Icons.AutoMirrored.Rounded.Backspace, tr("Usuń"), c) { c.onDelete(it) }
            }
        }
    }
}

@Composable
private fun RowScope.EditCell(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalKb.current
    Box(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.keySpecial)
            .tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = colors.text, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun RowScope.RepeatCell(icon: ImageVector, label: String, c: KeyboardController, action: (Boolean) -> Unit) {
    val colors = LocalKb.current
    val scope = rememberCoroutineScope()
    val act by rememberUpdatedState(action)
    var down by remember { mutableStateOf(false) }
    Box(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(if (down) colors.keyPressed else colors.key)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    down = true
                    c.feedback.key()
                    act(false)
                    val job = scope.launch {
                        delay(380)
                        var n = 0
                        while (true) {
                            act(true)
                            n++
                            delay((80 - n * 4).coerceAtLeast(30).toLong())
                        }
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                    down = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = colors.text, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun ColumnScope.EditWide(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalKb.current
    Row(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.keySpecial)
            .tap(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = colors.text, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = colors.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ───────────────────────── dyktowanie ─────────────────────────

@Composable
fun VoicePanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    val v = c.voice
    val pulse by animateFloatAsState(1f + v.level * 0.35f, label = "mic")
    Column(
        Modifier.fillMaxWidth().height(height).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (v.state) {
            VoiceState.NO_PERMISSION -> {
                Icon(Icons.Rounded.MicOff, null, tint = colors.textDim, modifier = Modifier.size(40.dp))
                Text(tr("Klawiatura potrzebuje dostępu do mikrofonu."), color = colors.text, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                PanelTextButton(tr("Zezwól w aplikacji"), primary = true) { c.openApp("voice") }
            }
            VoiceState.UNAVAILABLE -> {
                Icon(Icons.Rounded.MicOff, null, tint = colors.textDim, modifier = Modifier.size(40.dp))
                Text(tr("Brak usługi rozpoznawania mowy w systemie (np. aplikacji Google)."), color = colors.text, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
            }
            else -> {
                Text(
                    when {
                        v.state == VoiceState.ERROR -> v.error.orEmpty()
                        v.partial.isNotEmpty() -> v.partial
                        else -> tr("Mów teraz… ({0})", Languages.get(c.activeLanguage).native)
                    },
                    color = if (v.partial.isNotEmpty()) colors.text else colors.textDim,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Box(
                    Modifier
                        .size(76.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(if (v.state == VoiceState.LISTENING) colors.accent else colors.keySpecial)
                        .tap { if (v.state == VoiceState.LISTENING) v.finish() else v.start() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Mic, tr("Mikrofon"), tint = if (v.state == VoiceState.LISTENING) colors.onAccent else colors.text, modifier = Modifier.size(34.dp))
                }
                Text(
                    if (v.state == VoiceState.LISTENING) tr("Dotknij, aby zakończyć") else tr("Dotknij, aby mówić"),
                    color = colors.textDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

// ───────────────────────── języki ─────────────────────────

@Composable
fun LanguagesPanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    val s = c.settings
    Column(Modifier.fillMaxWidth().height(height).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyVerticalGrid(GridCells.Adaptive(140.dp), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(s.languages) { code ->
                val l = Languages.get(code)
                val sel = code == s.activeLanguage
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sel) colors.accent else colors.key)
                        .tap {
                            c.setLanguage(code)
                            c.closePanel()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(l.native, color = if (sel) colors.onAccent else colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        Languages.layoutName(s.layoutOverride[code] ?: l.layout),
                        color = if (sel) colors.onAccent.copy(alpha = 0.7f) else colors.textDim,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelTextButton(tr("Dodaj języki"), icon = Icons.Rounded.Add) { c.openApp("languages") }
            PanelTextButton(tr("Inna klawiatura"), icon = Icons.Rounded.Keyboard) { c.showImePicker() }
            Spacer(Modifier.weight(1f))
            PanelTextButton("ABC") { c.closePanel() }
        }
    }
}

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
            add(QTile(tr("Język"), Languages.get(s.activeLanguage).native, true, options = s.languages.map { code ->
                QOption(Languages.get(code).native, code == s.activeLanguage) { c.setLanguage(code) }
            } + QOption(tr("Wiele naraz"), s.multilingual) { set { it.copy(multilingual = !it.multilingual) } }) { c.nextLanguage() })
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

// ───────────────────────── narzędzia tekstu ─────────────────────────

/**
 * Zmiana wielkości liter, ozdobne style (tylko w zwykłych polach tekstu – nie w wyszukiwarkach,
 * adresach i hasłach), wstawki: daty i godziny, Twoje skróty, schowek, znaki; licznik znaków i słów.
 */
@Composable
fun TextPanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    val stats = remember { c.fieldStats() }
    val shortcuts by UserModel.shortcuts.collectAsState()
    val clips by ClipboardHistory.items.collectAsState()
    val rich = c.fieldKind == FieldKind.TEXT
    val dates = remember(c.activeLanguage) { TextTools.dateInserts(c.activeLanguage) }
    Column(
        Modifier.fillMaxWidth().height(height).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(tr("Wstaw datę i czas"), color = colors.textDim, fontSize = 11.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((label, value) in dates) {
                PanelTextButton(if (label == null) value else "$label  $value") { c.commitRaw(value) }
            }
        }
        if (shortcuts.isNotEmpty() || clips.isNotEmpty()) {
            Text(tr("Twoje teksty – skróty i schowek"), color = colors.textDim, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (sc in shortcuts) PanelTextButton(sc.trigger + " · " + sc.expansion.replace('\n', ' ').take(24)) { c.commitRaw(sc.expansion) }
                for (cl in clips.take(6)) PanelTextButton("📋 " + cl.text.replace('\n', ' ').take(24)) { c.commitRaw(cl.text) }
            }
        }
        Text(tr("Wstaw"), color = colors.textDim, fontSize = 11.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelTextButton(tr("Symbole"), icon = Icons.Rounded.Functions) { c.openPanel(Panel.SYMBOLS) }
            for (sym in listOf("…", "—", "–", "→", "•", "✓", "°C", "€", "„”", "«»", "©", "™", "§", "№")) {
                PanelTextButton(sym) {
                    if (sym.length == 2 && sym[0] in "„«" ) {
                        // Para cudzysłowów – kursor ląduje w środku.
                        c.commitRaw(sym)
                        c.moveCursor(-1)
                    } else {
                        c.commitRaw(sym)
                    }
                }
            }
            PanelTextButton("1–100") { c.commitRaw(TextTools.randomNumber()) }
            PanelTextButton("UUID") { c.commitRaw(TextTools.uuid()) }
        }
        if (c.fieldKind != FieldKind.PASSWORD) {
            Text(tr("Wielkość liter – zaznaczenia albo słowa przed kursorem"), color = colors.textDim, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PanelTextButton("Aa ⟳") { c.transformText(TextTools::cycleCase) }
                PanelTextButton("ABC") { c.transformText { it.uppercase() } }
                PanelTextButton("abc") { c.transformText { it.lowercase() } }
                PanelTextButton(tr("Zdanie")) { c.transformText(TextTools::sentence) }
                PanelTextButton(tr("Tytuł")) { c.transformText(TextTools::title) }
                PanelTextButton("aA ⇄") { c.transformText(TextTools::invert) }
            }
            Text(tr("Przekształć – zaznaczenie albo słowo przed kursorem"), color = colors.textDim, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PanelTextButton(tr("Bez ogonków")) { c.transformText(TextTools::stripDiacritics) }
                PanelTextButton(tr("Od tyłu")) { c.transformText(TextTools::reverse) }
                PanelTextButton("uʍop ǝpısdn") { c.transformText(TextTools::upsideDown) }
                PanelTextButton(tr("Popraw odstępy")) { c.transformText(TextTools::tidySpaces) }
            }
        }
        if (rich) {
            Text(tr("Styl pisania – dotknij, aby pisać; przytrzymaj, aby zmienić zaznaczenie"), color = colors.textDim, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StyleChip(tr("Zwykły"), c.fancy == null, onLong = null) { c.fancy = null }
                for (st in FancyStyle.entries) {
                    StyleChip(st.label, c.fancy == st, onLong = { c.transformText { TextTools.style(it, st) } }) {
                        c.fancy = if (c.fancy == st) null else st
                        if (c.fancy != null) c.closePanel()
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                tr("{0} znaków · {1} słów", stats.first, stats.second),
                color = colors.textDim,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            PanelTextButton(tr("Wyczyść pole"), icon = Icons.Rounded.DeleteSweep) { c.clearField() }
            PanelTextButton("ABC") { c.closePanel() }
        }
    }
}

@Composable
private fun StyleChip(label: String, selected: Boolean, onLong: (() -> Unit)?, onClick: () -> Unit) {
    val colors = LocalKb.current
    Text(
        label,
        color = if (selected) colors.onAccent else colors.text,
        fontSize = 15.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.accent else colors.keySpecial)
            .tap(onLong = onLong, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

// ───────────────────────── symbole ─────────────────────────

private val symbolGroups: List<Pair<String, List<String>>> = listOf(
    ",.?" to "… – — • · ¡ ¿ « » „ ” “ ‚ ’ ‘ ‹ › § ¶ † ‡ ※ ′ ″ © ® ™ ℠ № ° ℃ ℉ ‰ ‱ ¦ ⁂".split(' '),
    "±÷" to "± × ÷ ≠ ≈ ≡ ≤ ≥ ∞ √ ∛ ∑ ∏ ∫ ∂ ∆ ∇ π µ Ω ∈ ∉ ∩ ∪ ⊂ ⊃ ∀ ∃ ∅ ∝ ∠ ⊥ ∥ ½ ⅓ ⅔ ¼ ¾ ⅛ ¹ ² ³ ⁴ ⁿ ₀ ₁ ₂ ₃ ‰".split(' '),
    "→" to "← → ↑ ↓ ↔ ↕ ⇐ ⇒ ⇔ ⇑ ⇓ ↩ ↪ ↖ ↗ ↘ ↙ ⟵ ⟶ ⟷ ➜ ➔ ➤ ▶ ◀ ▲ ▼ ⤴ ⤵ ↻ ↺".split(' '),
    "€$" to "€ $ £ ¥ ¢ ₽ ₴ ₹ ₩ ₿ ₺ ₪ ₫ ₦ ₱ zł Kč Ft kr CHF".split(' '),
    "★" to "■ □ ▪ ▫ ▲ △ ▼ ▽ ◆ ◇ ○ ● ◎ ◉ ★ ☆ ♥ ♡ ♦ ♣ ♠ ✓ ✔ ✗ ✘ ☐ ☑ ☒ ✦ ✧ ❖ ♪ ♫ ☀ ☁ ☂ ☃ ☎ ✉ ✈ ⚡ ☮ ☯ ♀ ♂ ⚥ ⌘ ⌥ ⇧ ⌫ ⏎ ⌚ ⌛".split(' '),
    "αβ" to "α β γ δ ε ζ η θ ι κ λ μ ν ξ ο π ρ σ τ υ φ χ ψ ω Α Β Γ Δ Θ Λ Ξ Π Σ Φ Ψ Ω".split(' '),
    "^‿^" to listOf(
        "(◕‿◕)", "¯\\_(ツ)_/¯", "( ͡° ͜ʖ ͡°)", "(╯°□°)╯︵ ┻━┻", "ʕ•ᴥ•ʔ", "(ಥ﹏ಥ)", "(¬‿¬)", "(⌐■_■)", "♥‿♥",
        "(づ｡◕‿‿◕｡)づ", "\\(^o^)/", "(T_T)", "(o_O)", "(=^･ω･^=)", "(☞ﾟヮﾟ)☞", "(ง'̀-'́)ง",
    ),
)

/** Panel symboli: kategorie (interpunkcja, matematyka, strzałki, waluty, kształty, grecki, kaomoji). */
@Composable
fun SymbolsPanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    var group by remember { mutableStateOf(0) }
    val (_, list) = symbolGroups[group]
    val wide = list.any { it.length > 3 }
    Column(Modifier.fillMaxWidth().height(height)) {
        key(group) {
            LazyVerticalGrid(
                columns = if (wide) GridCells.Adaptive(96.dp) else GridCells.Adaptive(42.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp),
            ) {
                items(list) { sym ->
                    Box(
                        Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .tap {
                                c.feedback.key()
                                c.commitRaw(sym)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(sym, color = colors.text, fontSize = if (wide) 13.sp else 20.sp, maxLines = 1)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            PanelTextButton("ABC") { c.closePanel() }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                symbolGroups.forEachIndexed { i, (label, _) ->
                    val sel = i == group
                    Box(
                        Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) colors.accent else Color.Transparent)
                            .tap { group = i }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = if (sel) colors.onAccent else colors.textDim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            RepeatIcon(Icons.AutoMirrored.Rounded.Backspace, tr("Usuń"), c) { c.onDelete(it) }
        }
    }
}
