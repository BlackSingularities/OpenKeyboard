package com.blacksingularities.openkeyboard.ime

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
            LazyVerticalGrid(
                columns = if (wide) GridCells.Adaptive(110.dp) else GridCells.Adaptive(44.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            ) {
                items(list) { e ->
                    Box(
                        Modifier
                            .height(if (wide) 44.dp else 46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onLongClick = {
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
            }
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
                                .combinedClickable {
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
                    .combinedClickable { c.closePanel() }
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
                            .combinedClickable {
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
                            .combinedClickable(
                                onLongClick = {
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
            .combinedClickable(onClick = onClick)
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
            .combinedClickable(onClick = onClick)
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
                        .combinedClickable { c.selecting = !c.selecting },
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
            .combinedClickable(onClickLabel = label, onClick = onClick),
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
            .combinedClickable(onClick = onClick)
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
                        .combinedClickable { if (v.state == VoiceState.LISTENING) v.finish() else v.start() },
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
                        .combinedClickable {
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

@Composable
fun QuickPanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    val s = c.settings
    val theme = KeyboardThemes.presets.firstOrNull { it.id == s.theme } ?: KeyboardThemes.presets.first()
    data class Q(val label: String, val value: String?, val on: Boolean, val action: () -> Unit)
    val tiles = listOf(
        Q(tr("Płaska czarna"), null, s.theme == "flat") {
            c.updateSettings { it.copy(theme = if (it.theme == "flat") "black" else "flat", autoDayNight = false) }
        },
        Q(tr("Motyw"), tr(theme.name), true) {
            val i = KeyboardThemes.presets.indexOf(theme)
            c.updateSettings { it.copy(theme = KeyboardThemes.presets[(i + 1) % KeyboardThemes.presets.size].id, autoDayNight = false) }
        },
        Q(tr("Akcent"), null, false) { c.updateSettings { it.copy(accent = (it.accent + 1) % KeyboardThemes.accents.size) } },
        Q(tr("Klawisze"), when (s.keyStyle) { KeyStyle.FILLED -> tr("Pełne"); KeyStyle.OUTLINE -> tr("Obrys"); KeyStyle.FLAT -> tr("Płaskie"); KeyStyle.RAISED -> "3D" }, true) {
            c.updateSettings { it.copy(keyStyle = KeyStyle.entries[(it.keyStyle.ordinal + 1) % KeyStyle.entries.size]) }
        },
        Q(tr("Rząd cyfr"), null, s.numberRow) { c.updateSettings { it.copy(numberRow = !it.numberRow) } },
        Q(tr("Autokorekta"), when (s.autocorrect) { Autocorrect.OFF -> tr("Wył."); Autocorrect.GENTLE -> tr("Łagodna"); Autocorrect.NORMAL -> tr("Normalna"); Autocorrect.AGGRESSIVE -> tr("Mocna") }, s.autocorrect != Autocorrect.OFF) {
            c.updateSettings { it.copy(autocorrect = Autocorrect.entries[(it.autocorrect.ordinal + 1) % Autocorrect.entries.size]) }
        },
        Q(tr("Podpowiedzi"), null, s.suggestions) { c.updateSettings { it.copy(suggestions = !it.suggestions) } },
        Q(tr("Wibracja"), null, s.haptics) { c.updateSettings { it.copy(haptics = !it.haptics) } },
        Q(tr("Dźwięk"), null, s.sound) { c.updateSettings { it.copy(sound = !it.sound) } },
        Q(tr("Jedna ręka"), null, s.oneHanded != OneHanded.OFF) { c.toggleOneHanded() },
        Q(tr("Pływająca"), null, s.floating) { c.toggleFloating() },
        Q(tr("Incognito"), null, s.forceIncognito) { c.updateSettings { it.copy(forceIncognito = !it.forceIncognito) } },
        Q(tr("Rozmiar"), "${s.keyHeight}%", true) { c.openPanel(Panel.RESIZE) },
        Q(tr("Strzałki"), null, s.arrowRow) { c.updateSettings { it.copy(arrowRow = !it.arrowRow) } },
        Q(tr("Gest"), null, s.glideTyping) { c.updateSettings { it.copy(glideTyping = !it.glideTyping) } },
    )
    Column(Modifier.fillMaxWidth().height(height).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tiles) { q ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (q.on && q.value == null) colors.accent else colors.key)
                        .combinedClickable {
                            c.feedback.key()
                            q.action()
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val fg = if (q.on && q.value == null) colors.onAccent else colors.text
                    Text(q.label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (q.value != null) {
                        Text(q.value, color = colors.textDim, fontSize = 10.sp, maxLines = 1)
                    } else if (q.label == tr("Akcent")) {
                        Box(Modifier.padding(top = 3.dp).size(10.dp).clip(CircleShape).background(colors.accent))
                    } else {
                        Text(if (q.on) tr("Wł.") else tr("Wył."), color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelTextButton(tr("Wszystkie ustawienia"), icon = Icons.Rounded.Settings) { c.openApp() }
            Spacer(Modifier.weight(1f))
            PanelTextButton("ABC") { c.closePanel() }
        }
    }
}

// ───────────────────────── narzędzia tekstu ─────────────────────────

/**
 * Zmiana wielkości liter, ozdobne style (dotknięcie – pisz w tym stylu, przytrzymanie – zmień
 * zaznaczenie albo poprzednie słowo), wstawianie daty i godziny, licznik znaków i słów.
 */
@Composable
fun TextPanel(c: KeyboardController, height: Dp) {
    val colors = LocalKb.current
    val stats = remember { c.fieldStats() }
    Column(
        Modifier.fillMaxWidth().height(height).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(tr("Wielkość liter – zaznaczenia albo słowa przed kursorem"), color = colors.textDim, fontSize = 11.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelTextButton("Aa ⟳") { c.transformText(com.blacksingularities.openkeyboard.features.TextTools::cycleCase) }
            PanelTextButton("ABC") { c.transformText { it.uppercase() } }
            PanelTextButton("abc") { c.transformText { it.lowercase() } }
            PanelTextButton(tr("Zdanie")) { c.transformText(com.blacksingularities.openkeyboard.features.TextTools::sentence) }
            PanelTextButton(tr("Tytuł")) { c.transformText(com.blacksingularities.openkeyboard.features.TextTools::title) }
            PanelTextButton("aA ⇄") { c.transformText(com.blacksingularities.openkeyboard.features.TextTools::invert) }
        }
        Text(tr("Styl pisania – dotknij, aby pisać; przytrzymaj, aby zmienić zaznaczenie"), color = colors.textDim, fontSize = 11.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StyleChip(tr("Zwykły"), c.fancy == null, onLong = null) { c.fancy = null }
            for (st in com.blacksingularities.openkeyboard.features.FancyStyle.entries) {
                StyleChip(st.label, c.fancy == st, onLong = { c.transformText { com.blacksingularities.openkeyboard.features.TextTools.style(it, st) } }) {
                    c.fancy = if (c.fancy == st) null else st
                    if (c.fancy != null) c.closePanel()
                }
            }
        }
        Text(tr("Wstaw"), color = colors.textDim, fontSize = 11.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelTextButton(com.blacksingularities.openkeyboard.features.TextTools.date()) { c.commitRaw(com.blacksingularities.openkeyboard.features.TextTools.date()) }
            PanelTextButton(com.blacksingularities.openkeyboard.features.TextTools.time()) { c.commitRaw(com.blacksingularities.openkeyboard.features.TextTools.time()) }
            PanelTextButton(com.blacksingularities.openkeyboard.features.TextTools.iso()) { c.commitRaw(com.blacksingularities.openkeyboard.features.TextTools.iso()) }
            PanelTextButton("…") { c.commitRaw("…") }
            PanelTextButton("—") { c.commitRaw("—") }
            PanelTextButton("→") { c.commitRaw("→") }
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
            .combinedClickable(onLongClick = onLong, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
