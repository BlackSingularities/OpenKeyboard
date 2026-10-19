package com.blacksingularities.openkeyboard.ime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openkeyboard.data.Languages
import com.blacksingularities.openkeyboard.data.OneHanded
import com.blacksingularities.openkeyboard.data.ToolbarItem
import com.blacksingularities.openkeyboard.dict.Suggestion
import com.blacksingularities.openkeyboard.dict.SuggestionKind
import com.blacksingularities.openkeyboard.i18n.tr
import com.blacksingularities.openkeyboard.keyboard.KbColors
import com.blacksingularities.openkeyboard.keyboard.Key
import com.blacksingularities.openkeyboard.keyboard.KeyType
import com.blacksingularities.openkeyboard.keyboard.KeyboardMode
import com.blacksingularities.openkeyboard.keyboard.KeyboardThemes
import kotlin.math.roundToInt

val LocalKb = staticCompositionLocalOf<KbColors> { error("Brak kolorów klawiatury") }

val STRIP_HEIGHT = 44.dp

/** Całe okno klawiatury: pasek u góry, klawisze albo panel; zwykła, jednoręczna lub pływająca. */
@Composable
fun KeyboardRoot(c: KeyboardController) {
    val s = c.settings
    val colors = KeyboardThemes.colors(s)
    val config = LocalConfiguration.current
    val landscape = config.screenWidthDp > config.screenHeightDp
    CompositionLocalProvider(LocalKb provides colors, LocalContentColor provides colors.text) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val floating = s.floating
            val boundsModifier = Modifier.onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                val r = android.graphics.Rect(b.left.roundToInt(), b.top.roundToInt(), b.right.roundToInt(), b.bottom.roundToInt())
                if (r != c.service.keyboardBounds) {
                    c.service.keyboardBounds.set(r)
                    c.service.requestInsetsUpdate()
                }
            }
            if (floating) {
                val width = maxWidth * (s.floatingScale / 100f)
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset { IntOffset(c.floatingOffset.x.roundToInt(), c.floatingOffset.y.roundToInt()) }
                        .padding(bottom = navPadding(c) + 12.dp)
                        .width(width)
                        .then(boundsModifier)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.background),
                ) {
                    // Uchwyt: przeciągnij, aby przesunąć klawiaturę po ekranie.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(onDragEnd = { c.service.requestInsetsUpdate() }) { change, drag ->
                                    change.consume()
                                    c.floatingOffset += drag
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.DragHandle, tr("Przesuń"), tint = colors.textDim, modifier = Modifier.size(20.dp))
                        Icon(
                            Icons.Rounded.Fullscreen,
                            tr("Zadokuj"),
                            tint = colors.textDim,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .combinedClickable(onClick = { c.toggleFloating() }),
                        )
                    }
                    KeyboardBody(c, landscape)
                }
            } else {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .then(boundsModifier)
                        .background(colors.background)
                        .padding(bottom = navPadding(c) + s.bottomPadding.dp),
                ) {
                    KeyboardBody(c, landscape)
                }
            }
        }
    }
}

/** Odstęp od dołu: większy z wcięcia systemowego i wysokości paska nawigacji klawiatury. */
@Composable
private fun navPadding(c: KeyboardController): androidx.compose.ui.unit.Dp {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val insets = WindowInsets.navigationBars.getBottom(density)
    return with(density) { maxOf(insets, c.service.navBarPx).toDp() }
}

@Composable
private fun KeyboardBody(c: KeyboardController, landscape: Boolean) {
    val s = c.settings
    val colors = LocalKb.current
    val rowH = rowHeight(s, KeyboardMode.ALPHA, landscape)
    val panelHeight = rowH * (if (s.numberRow) 5 else 4)
    Box {
        Column {
            TopBar(c)
            when (c.panel) {
                Panel.KEYS -> {
                    if (c.internal != null) InternalResults(c)
                    OneHandedWrapper(c) { Keys(c, landscape) }
                }
                Panel.EMOJI -> EmojiPanel(c, panelHeight)
                Panel.CLIPBOARD -> ClipboardPanel(c, panelHeight)
                Panel.EDIT -> EditPanel(c, panelHeight)
                Panel.VOICE -> VoicePanel(c, panelHeight)
                Panel.LANGUAGES -> LanguagesPanel(c, panelHeight)
                Panel.QUICK -> QuickPanel(c, panelHeight)
            }
        }
        val toast = c.toast
        AnimatedVisibility(toast != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)) {
            Text(
                toast.orEmpty(),
                color = colors.onAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.accent)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun Keys(c: KeyboardController, landscape: Boolean) {
    val colors = LocalKb.current
    val actions = remember(c) { ControllerActions(c) }
    KeyGrid(
        layout = c.layout,
        settings = c.settings,
        colors = colors,
        shift = c.shift,
        enterAction = c.enterAction,
        multiLine = c.multiLine,
        landscape = landscape,
        actions = actions,
        modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp),
    )
}

private class ControllerActions(val c: KeyboardController) : KeyActions {
    override fun press(key: Key) {
        c.feedback.key(
            when (key.type) {
                KeyType.DELETE -> Sound.DELETE
                KeyType.SPACE -> Sound.SPACE
                KeyType.ENTER -> Sound.ENTER
                else -> Sound.KEY
            },
        )
    }

    override fun commit(key: Key) = c.onKey(key)
    override fun text(t: String) = c.onText(t)
    override fun deleteDown() = c.onDelete()
    override fun deleteRepeat() = c.onDelete(repeat = true)
    override fun longPress(key: Key): Boolean = when (key.type) {
        KeyType.SPACE -> {
            if (c.settings.spaceLongPressPicker) {
                if (c.settings.languages.size > 1) c.openPanel(Panel.LANGUAGES) else c.showImePicker()
                true
            } else false
        }
        KeyType.SHIFT -> { c.lockShift(); true }
        KeyType.SYMBOLS -> { c.switchMode(KeyboardMode.NUMBER); true }
        KeyType.LANGUAGE -> { c.openPanel(Panel.LANGUAGES); true }
        KeyType.ENTER -> { c.openPanel(Panel.EMOJI); true }
        else -> false
    }

    override fun cursor(steps: Int) = c.moveCursor(steps)
    override fun selectBack(words: Int) = c.selectWordsBack(words)
    override fun selectDone(delete: Boolean) = if (delete) c.deleteSelection() else c.cancelSelectionBack()
    override fun hide() = c.hide()
    override fun tick() = c.feedback.tick()
    override fun long() = c.feedback.long()
}

/** Tryb jednej ręki: klawisze zwężone, obok kolumna z przełącznikami. */
@Composable
private fun OneHandedWrapper(c: KeyboardController, content: @Composable () -> Unit) {
    val s = c.settings
    if (s.oneHanded == OneHanded.OFF || s.floating) {
        content()
        return
    }
    val colors = LocalKb.current
    val side: @Composable () -> Unit = {
        Column(
            Modifier.fillMaxHeight().padding(4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StripIcon(Icons.Rounded.SwapHoriz, tr("Druga strona")) {
                c.updateSettings { it.copy(oneHanded = if (it.oneHanded == OneHanded.LEFT) OneHanded.RIGHT else OneHanded.LEFT) }
            }
            StripIcon(Icons.Rounded.Fullscreen, tr("Pełna szerokość")) { c.updateSettings { it.copy(oneHanded = OneHanded.OFF) } }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val w = maxWidth * (s.oneHandedWidth / 100f)
        Row(Modifier.fillMaxWidth().background(colors.background), verticalAlignment = Alignment.CenterVertically) {
            if (s.oneHanded == OneHanded.RIGHT) Box(Modifier.weight(1f).height(rowHeight(s, c.layout.mode, false) * c.layout.rows.size)) { side() }
            Box(Modifier.width(w)) { content() }
            if (s.oneHanded == OneHanded.LEFT) Box(Modifier.weight(1f).height(rowHeight(s, c.layout.mode, false) * c.layout.rows.size)) { side() }
        }
    }
}

// ───────────────────────── pasek u góry ─────────────────────────

@Composable
private fun TopBar(c: KeyboardController) {
    val colors = LocalKb.current
    Box(Modifier.fillMaxWidth().height(STRIP_HEIGHT).background(colors.strip)) {
        when {
            c.internal != null -> InternalBar(c)
            c.panel != Panel.KEYS -> PanelBar(c)
            c.toolbarOpen || (c.suggestions.items.isEmpty() && c.extras.isEmpty()) -> Toolbar(c)
            else -> SuggestionStrip(c)
        }
    }
}

@Composable
fun StripIcon(icon: ImageVector, label: String, active: Boolean = false, onLong: (() -> Unit)? = null, onClick: () -> Unit) {
    val colors = LocalKb.current
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(onClickLabel = label, onLongClick = onLong, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (active) colors.onAccent else colors.text, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun Toolbar(c: KeyboardController) {
    val colors = LocalKb.current
    Row(Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (c.incognito) {
            Icon(Icons.Rounded.VisibilityOff, tr("Tryb incognito"), tint = colors.textDim, modifier = Modifier.padding(horizontal = 8.dp).size(18.dp))
        }
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in c.settings.toolbar) ToolbarButton(c, item)
        }
        StripIcon(Icons.Rounded.KeyboardHide, tr("Schowaj klawiaturę"), onLong = { c.showImePicker() }) { c.hide() }
    }
}

@Composable
private fun ToolbarButton(c: KeyboardController, item: ToolbarItem) {
    val s = c.settings
    when (item) {
        ToolbarItem.CLIPBOARD -> StripIcon(Icons.Rounded.ContentPaste, tr("Schowek")) { c.openPanel(Panel.CLIPBOARD) }
        ToolbarItem.EMOJI -> StripIcon(Icons.Rounded.EmojiEmotions, tr("Emoji")) { c.openPanel(Panel.EMOJI) }
        ToolbarItem.EDIT -> StripIcon(Icons.Rounded.OpenWith, tr("Kursor i zaznaczanie")) { c.openPanel(Panel.EDIT) }
        ToolbarItem.VOICE -> StripIcon(Icons.Rounded.Mic, tr("Dyktowanie")) { c.openPanel(Panel.VOICE) }
        ToolbarItem.TRANSLATE -> StripIcon(Icons.Rounded.Translate, tr("Tłumacz")) { c.openInternal(InternalTarget.TRANSLATE) }
        ToolbarItem.ONE_HANDED -> StripIcon(Icons.Rounded.SwapHoriz, tr("Jedna ręka"), active = s.oneHanded != OneHanded.OFF) { c.toggleOneHanded() }
        ToolbarItem.FLOATING -> StripIcon(Icons.Rounded.PictureInPicture, tr("Klawiatura pływająca"), active = s.floating) { c.toggleFloating() }
        ToolbarItem.NUMPAD -> StripIcon(Icons.Rounded.Dialpad, tr("Klawiatura numeryczna"), active = c.layout.mode == KeyboardMode.NUMBER) {
            c.switchMode(if (c.layout.mode == KeyboardMode.NUMBER) KeyboardMode.ALPHA else KeyboardMode.NUMBER)
        }
        ToolbarItem.SETTINGS -> StripIcon(Icons.Rounded.Tune, tr("Szybkie ustawienia"), onLong = { c.openApp() }) { c.openPanel(Panel.QUICK) }
    }
}

@Composable
private fun SuggestionStrip(c: KeyboardController) {
    val colors = LocalKb.current
    val items = c.suggestions.items
    val auto = c.suggestions.autoCorrect
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        StripIcon(if (c.incognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Apps, tr("Narzędzia")) { c.toolbarOpen = true }
        // Kolejność na pasku: [drugi] [najlepszy] [trzeci] – najlepszy na środku.
        val slots = when (items.size) {
            0 -> emptyList()
            1 -> listOf<Suggestion?>(null, items[0], null)
            2 -> listOf(items[1], items[0], null)
            else -> listOf(items[1], items[0], items[2])
        }
        Row(Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            slots.forEachIndexed { i, sug ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (sug != null) Modifier.combinedClickable(
                                onLongClick = { c.blockSuggestion(sug) },
                                onClick = { c.pickSuggestion(sug) },
                            ) else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (sug != null) {
                        val strong = i == 1 && (auto != null && sug.text == auto.text || c.suggestions.typedValid || sug.kind == SuggestionKind.PREDICTION)
                        Text(
                            sug.display,
                            color = if (sug.kind == SuggestionKind.TYPED) colors.textDim else colors.text,
                            fontSize = 16.sp,
                            fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        if (i == 1 && auto != null && sug.text == auto.text) {
                            // Kropka pod słowem: to wstawi spacja (autokorekta).
                            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(4.dp).clip(CircleShape).background(colors.accent))
                        }
                    }
                }
            }
        }
        if (c.extras.isNotEmpty()) {
            Row(
                Modifier.padding(end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (e in c.extras.take(3)) ExtraChip(c, e)
            }
        }
    }
}

@Composable
private fun ExtraChip(c: KeyboardController, e: Suggestion) {
    val colors = LocalKb.current
    Text(
        e.display,
        color = colors.text,
        fontSize = if (e.kind == SuggestionKind.EMOJI) 20.sp else 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.key)
            .combinedClickable { c.pickSuggestion(e) }
            .padding(horizontal = if (e.kind == SuggestionKind.EMOJI) 8.dp else 12.dp, vertical = 5.dp),
    )
}

/** Pasek nad panelem: tytuł i powrót do liter. */
@Composable
private fun PanelBar(c: KeyboardController) {
    val colors = LocalKb.current
    val title = when (c.panel) {
        Panel.EMOJI -> tr("Emoji")
        Panel.CLIPBOARD -> tr("Schowek")
        Panel.EDIT -> tr("Kursor i zaznaczanie")
        Panel.VOICE -> tr("Dyktowanie")
        Panel.LANGUAGES -> tr("Języki")
        Panel.QUICK -> tr("Szybkie ustawienia")
        Panel.KEYS -> ""
    }
    Row(Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp).weight(1f))
        if (c.panel == Panel.EMOJI) StripIcon(Icons.Rounded.Search, tr("Szukaj emoji")) { c.openInternal(InternalTarget.EMOJI_SEARCH) }
        if (c.panel == Panel.CLIPBOARD) {
            StripIcon(Icons.Rounded.Tune, tr("Ustawienia schowka")) { c.openApp("clipboard") }
        }
        StripIcon(Icons.Rounded.Close, tr("Wróć do klawiatury")) { c.closePanel() }
    }
}

/** Pole wpisywania w klawiaturze: wyszukiwanie emoji albo tekst do przetłumaczenia. */
@Composable
private fun InternalBar(c: KeyboardController) {
    val colors = LocalKb.current
    Row(Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (c.internal == InternalTarget.TRANSLATE) Icons.Rounded.Translate else Icons.Rounded.Search,
            null,
            tint = colors.textDim,
            modifier = Modifier.padding(horizontal = 8.dp).size(18.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.key)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val placeholder = if (c.internal == InternalTarget.TRANSLATE) {
                tr("Tekst do przetłumaczenia na: {0}", Languages.get(c.settings.translateTarget).native)
            } else {
                tr("Szukaj emoji (np. serce, kot, party)")
            }
            Text(
                c.internalText.ifEmpty { placeholder } + if (c.internalText.isNotEmpty()) "▏" else "",
                color = if (c.internalText.isEmpty()) colors.textDim else colors.text,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
        }
        StripIcon(Icons.Rounded.Close, tr("Zamknij")) {
            c.closeInternal(if (c.internal == InternalTarget.EMOJI_SEARCH) Panel.EMOJI else Panel.KEYS)
        }
    }
}

/** Wyniki pod paskiem wewnętrznego pola (emoji / tłumaczenie) – nad klawiszami. */
@Composable
fun InternalResults(c: KeyboardController) {
    val colors = LocalKb.current
    when (c.internal) {
        InternalTarget.EMOJI_SEARCH -> {
            val found = remember(c.internalText) { com.blacksingularities.openkeyboard.emoji.EmojiData.search(c.internalText) }
            Row(
                Modifier.fillMaxWidth().height(46.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (found.isEmpty()) {
                    Text(
                        if (c.internalText.isBlank()) tr("Wpisz nazwę – po polsku lub angielsku") else tr("Brak wyników"),
                        color = colors.textDim,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                for (e in found) {
                    Text(
                        e,
                        fontSize = 26.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .combinedClickable { c.commitEmoji(e) }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }
        InternalTarget.TRANSLATE -> {
            Row(
                Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LangChooser(c)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        c.translateBusy -> tr("Tłumaczenie…")
                        c.translateResult != null -> c.translateResult!!
                        else -> tr("Wynik pojawi się tutaj")
                    },
                    color = if (c.translateResult != null) colors.text else colors.textDim,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (c.translateResult != null && !c.translateBusy) {
                    Text(
                        tr("Wstaw"),
                        color = colors.onAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.accent)
                            .combinedClickable { c.insertTranslation() }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
        null -> {}
    }
}

/** Wybór języka docelowego tłumaczenia (dotknięcie – następny z listy). */
@Composable
private fun LangChooser(c: KeyboardController) {
    val colors = LocalKb.current
    val targets = (c.settings.languages + listOf("en", "pl", "de", "es", "fr", "it", "uk")).distinct()
    val cur = c.settings.translateTarget
    Text(
        "→ " + cur.uppercase(),
        color = colors.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.key)
            .combinedClickable {
                val next = targets[(targets.indexOf(cur) + 1).mod(targets.size)]
                c.updateSettings { it.copy(translateTarget = next) }
                c.runTranslate()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
