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
    val dates = remember(c.writingLanguage) { TextTools.dateInserts(c.writingLanguage) }
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
