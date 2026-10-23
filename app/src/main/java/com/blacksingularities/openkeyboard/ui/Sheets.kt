package com.blacksingularities.openkeyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.ContentPasteGo
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openkeyboard.LocalSystem
import com.blacksingularities.openkeyboard.data.KeyStyle
import com.blacksingularities.openkeyboard.data.GestureAction
import com.blacksingularities.openkeyboard.data.KeyFont
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Gradient
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Languages
import com.blacksingularities.openkeyboard.data.ToolbarItem
import com.blacksingularities.openkeyboard.dict.DictState
import com.blacksingularities.openkeyboard.dict.Dictionaries
import com.blacksingularities.openkeyboard.dict.Shortcut
import com.blacksingularities.openkeyboard.dict.UserModel
import com.blacksingularities.openkeyboard.i18n.tr
import com.blacksingularities.openkeyboard.ime.KeyGrid
import com.blacksingularities.openkeyboard.ime.ShiftState
import com.blacksingularities.openkeyboard.keyboard.FieldKind
import com.blacksingularities.openkeyboard.keyboard.KeyboardMode
import com.blacksingularities.openkeyboard.keyboard.KeyboardThemes
import com.blacksingularities.openkeyboard.keyboard.Layouts
import com.blacksingularities.openkeyboard.ui.components.ActionRow
import com.blacksingularities.openkeyboard.ui.components.CompactChoice
import com.blacksingularities.openkeyboard.ui.components.DISABLED_ALPHA
import com.blacksingularities.openkeyboard.ui.components.DarkField
import com.blacksingularities.openkeyboard.ui.components.Group
import com.blacksingularities.openkeyboard.ui.components.Panel
import com.blacksingularities.openkeyboard.ui.components.PillButton
import com.blacksingularities.openkeyboard.ui.components.RowIcon
import com.blacksingularities.openkeyboard.ui.components.SliderRow
import com.blacksingularities.openkeyboard.ui.theme.Palette

// ───────────────────────── języki ─────────────────────────

@Composable
fun LanguagesSheet(vm: AppViewModel) {
    val s = vm.settings
    val states by Dictionaries.states.collectAsState()
    var query by remember { mutableStateOf("") }
    Panel(tr("Języki i słowniki"), onClose = { vm.sheet = null }) {
        Text(
            tr("Włącz języki, w których piszesz – podpowiedzi działają we wszystkich naraz. Polski i angielski mają słowniki wbudowane, pozostałe pobierzesz jednym dotknięciem (ok. 0,3 MB, 50 tys. najczęstszych słów)."),
            color = Palette.TextDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        DarkField(query, { query = it }, tr("Szukaj języka"))
        val enabled = s.languages.map { Languages.get(it) }
        val rest = Languages.all.filter { it.code !in s.languages }
            .filter { query.isBlank() || it.name.contains(query, true) || it.native.contains(query, true) || it.code.equals(query, true) }
        Group(if (s.multilingual) tr("Włączone – działają naraz") else tr("Włączone (kolejność przełączania)")) {
            enabled.forEachIndexed { i, l ->
                LanguageRow(vm, l.code, states[l.code], enabled = true, first = i == 0, last = i == enabled.lastIndex)
            }
        }
        Group(tr("Dostępne")) {
            rest.forEach { l -> LanguageRow(vm, l.code, states[l.code], enabled = false, first = false, last = false) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LanguageRow(vm: AppViewModel, code: String, state: DictState?, enabled: Boolean, first: Boolean, last: Boolean) {
    val s = vm.settings
    val l = Languages.get(code)
    val active = s.activeLanguage == code
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(if (active) Color.White else Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(code.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Palette.Ink else Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(l.native + if (l.native != tr(l.name)) " · " + tr(l.name) else "", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(dictLabel(state, l.bundled), fontSize = 11.sp, color = Palette.TextDim, maxLines = 1)
            }
            when (state) {
                is DictState.Downloading -> CircularProgressIndicator(progress = { state.progress }, color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                DictState.Missing, is DictState.Failed -> IconBtn(Icons.Rounded.Download, tr("Pobierz słownik")) { Dictionaries.download(code) }
                is DictState.Ready -> if (!state.bundled && !l.bundled) IconBtn(Icons.Rounded.Delete, tr("Usuń słownik")) { Dictionaries.delete(code) }
                else -> {}
            }
            if (enabled) {
                IconBtn(Icons.Rounded.KeyboardArrowUp, tr("Wyżej"), enabled = !first) { move(vm, code, -1) }
                IconBtn(Icons.Rounded.KeyboardArrowDown, tr("Niżej"), enabled = !last) { move(vm, code, 1) }
            }
            Switch(
                checked = enabled,
                onCheckedChange = { on ->
                    vm.update {
                        val list = if (on) it.languages + code else it.languages - code
                        if (list.isEmpty()) it else it.copy(languages = list, activeLanguage = if (it.activeLanguage in list) it.activeLanguage else list.first())
                    }
                    if (on && state == DictState.Missing) Dictionaries.download(code)
                },
                modifier = Modifier.scale(0.82f),
                colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.Ink),
            )
        }
        if (enabled) {
            val layouts = Languages.layoutsFor(l)
            Row(
                Modifier.padding(start = 44.dp, top = 4.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val cur = s.layoutOverride[code] ?: l.layout
                for (id in layouts) {
                    Chip(Languages.layoutName(id), id == cur) {
                        vm.update { st -> st.copy(layoutOverride = if (id == l.layout) st.layoutOverride - code else st.layoutOverride + (code to id)) }
                    }
                }
                // Wiele naraz: wybór decyduje tylko o układzie liter wspólnym dla języków tego alfabetu.
                if (!active) Chip(if (s.multilingual) tr("Użyj tego układu") else tr("Ustaw bieżący"), false) { vm.update { it.copy(activeLanguage = code) } }
            }
        }
    }
}

private fun move(vm: AppViewModel, code: String, d: Int) = vm.update {
    val list = it.languages.toMutableList()
    val i = list.indexOf(code)
    val j = i + d
    if (i < 0 || j !in list.indices) return@update it
    list[i] = list[j]
    list[j] = code
    it.copy(languages = list)
}

private fun dictLabel(state: DictState?, bundled: Boolean): String = when (state) {
    null -> ""
    DictState.Missing -> tr("Słownik do pobrania")
    DictState.Unavailable -> tr("Bez słownika – tylko układ klawiszy")
    is DictState.Downloading -> tr("Pobieranie… {0}%", (state.progress * 100).toInt())
    DictState.Loading -> tr("Wczytywanie…")
    is DictState.Ready -> when {
        state.words > 0 -> tr("{0} słów", state.words) + if (state.bundled || bundled) " · " + tr("wbudowany") else ""
        bundled -> tr("Słownik wbudowany")
        else -> tr("Słownik pobrany")
    }
    is DictState.Failed -> tr("Błąd: {0} – dotknij, aby ponowić", state.message)
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Palette.Ink else Color.White,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@Composable
private fun IconBtn(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (enabled) Color.White else Palette.TextFaint, modifier = Modifier.size(20.dp))
    }
}

// ───────────────────────── motyw ─────────────────────────

/** Podgląd klawiatury z bieżącymi ustawieniami (bez dotyku). */
@Composable
fun KeyboardPreview(s: KeyboardSettings) {
    val colors = KeyboardThemes.colors(s, androidx.compose.foundation.isSystemInDarkTheme())
    val layout = remember(s) { Layouts.forSettings(KeyboardMode.ALPHA, s, FieldKind.TEXT) }
    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides colors.text) {
        val bgMod = colors.background2?.let { Modifier.background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(colors.background, it))) } ?: Modifier.background(colors.background)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).then(bgMod).padding(bottom = 6.dp)) {
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(tr("cześć"), tr("dzień"), tr("dobry")).forEachIndexed { i, w ->
                    Text(
                        w,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = if (i == 1) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            KeyGrid(layout, s, colors, ShiftState.ONESHOT, android.view.inputmethod.EditorInfo.IME_ACTION_SEND, false, false, null, Modifier.padding(horizontal = 3.dp))
        }
    }
}

@Composable
fun ThemeSheet(vm: AppViewModel) {
    val s = vm.settings
    fun set(t: (KeyboardSettings) -> KeyboardSettings) = vm.update(t)
    Panel(tr("Motyw i klawisze"), onClose = { vm.sheet = null }) {
        KeyboardPreview(s)
        Group(tr("Motyw")) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (p in KeyboardThemes.presets) {
                    val c = p.build(KeyboardThemes.accents.getOrElse(s.accent) { Color.White })
                    val sel = p.id == s.theme
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) Color.White else Color.White.copy(alpha = 0.06f))
                            .clickable { set { it.copy(theme = p.id, autoDayNight = false) } }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Miniatura: tło, dwa klawisze, akcent.
                        Box(Modifier.size(64.dp, 44.dp).clip(RoundedCornerShape(8.dp)).background(c.background).padding(5.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(Modifier.size(15.dp, 14.dp).clip(RoundedCornerShape(3.dp)).background(c.key))
                                Box(Modifier.size(15.dp, 14.dp).clip(RoundedCornerShape(3.dp)).background(c.key))
                                Box(Modifier.size(15.dp, 14.dp).clip(RoundedCornerShape(3.dp)).background(c.key))
                            }
                            Row(Modifier.align(Alignment.BottomStart), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(Modifier.size(33.dp, 14.dp).clip(RoundedCornerShape(3.dp)).background(c.keySpecial))
                                Box(Modifier.size(15.dp, 14.dp).clip(RoundedCornerShape(3.dp)).background(c.accent))
                            }
                        }
                        Text(tr(p.name), fontSize = 11.sp, color = if (sel) Palette.Ink else Color.White, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        DayNightSection(vm)
        CustomThemeSection(vm)
        Group(tr("Kolor akcentu")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyboardThemes.accents.forEachIndexed { i, c ->
                    val sel = i == s.accent && s.accentColor == 0L
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (sel) Color.White else Color.White.copy(alpha = 0.12f))
                            .clickable { set { it.copy(accent = i, accentColor = 0) } }
                            .padding(if (sel) 3.dp else 2.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(if (c == Color.White) Modifier.border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape) else Modifier),
                    )
                }
            }
            AccentPicker(vm)
        }

        Group(null) {
            CompactChoice(
                Icons.Rounded.Tune,
                tr("Klawisze"),
                KeyStyle.entries,
                s.keyStyle,
                { when (it) { KeyStyle.FILLED -> tr("Wypełnione"); KeyStyle.OUTLINE -> tr("Z obwódką"); KeyStyle.FLAT -> tr("Same znaki (minimalistyczne)"); KeyStyle.RAISED -> tr("3D – klawisze z cieniem pod spodem") } },
                short = { when (it) { KeyStyle.FILLED -> tr("Pełne"); KeyStyle.OUTLINE -> tr("Obrys"); KeyStyle.FLAT -> tr("Płaskie"); KeyStyle.RAISED -> "3D" } },
            ) { v -> set { it.copy(keyStyle = v) } }
            SliderRow(Icons.Rounded.RoundedCorner, tr("Zaokrąglenie"), s.keyRadius, 0..24) { v -> set { it.copy(keyRadius = v) } }
            SliderRow(Icons.Rounded.SpaceBar, tr("Odstęp między klawiszami"), s.keyGap, 0..12) { v -> set { it.copy(keyGap = v) } }
            SliderRow(Icons.Rounded.Height, tr("Wysokość klawiszy"), s.keyHeight, 70..140, 5, { "$it%" }) { v -> set { it.copy(keyHeight = v) } }
            SliderRow(Icons.Rounded.FormatSize, tr("Wielkość znaków"), s.fontScale, 70..150, 5, { "$it%" }) { v -> set { it.copy(fontScale = v) } }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ───────────────────────── słownik użytkownika ─────────────────────────

@Composable
fun DictionarySheet(vm: AppViewModel) {
    val version by UserModel.version.collectAsState()
    var query by remember { mutableStateOf("") }
    var newWord by remember { mutableStateOf("") }
    var showBlocked by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    val words = remember(version, query) {
        UserModel.allWords().filter { query.isBlank() || it.first.contains(query, true) }.take(300)
    }
    val blocked = remember(version) { UserModel.blockedWords() }
    Panel(tr("Słownik użytkownika"), onClose = { vm.sheet = null }) {
        Text(
            tr("Słowa zapamiętane podczas pisania i dodane ręcznie – dotknij słowa, aby poprawić pisownię. Przytrzymaj podpowiedź w klawiaturze, aby słowo zapomnieć; słowo ze słownika ogólnego trafia wtedy do zablokowanych."),
            color = Palette.TextDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            DarkField(newWord, { newWord = it.trim() }, tr("Nowe słowo"), Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            PillButton(tr("Dodaj"), enabled = newWord.length >= 2) {
                UserModel.addWord(newWord)
                vm.toast(tr("Dodano „{0}”", newWord))
                newWord = ""
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(tr("Słowa ({0})", UserModel.allWords().size), !showBlocked) { showBlocked = false }
            Chip(tr("Zablokowane ({0})", blocked.size), showBlocked) { showBlocked = true }
        }
        if (!showBlocked) {
            DarkField(query, { query = it }, tr("Szukaj"))
            Group(null) {
                if (words.isEmpty()) Text(tr("Brak słów – pisz, a klawiatura sama się nauczy."), color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
                for ((w, n) in words) {
                    if (editing == w) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            DarkField(editText, { editText = it.trim() }, w, Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            PillButton(tr("Zapisz"), enabled = editText.length >= 2 && editText != w) {
                                UserModel.renameWord(w, editText)
                                editing = null
                            }
                        }
                        continue
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { editing = w; editText = w }.padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(w, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("×$n", fontSize = 11.sp, color = Palette.TextDim)
                        IconBtn(Icons.Rounded.Delete, tr("Usuń")) { UserModel.removeWord(w) }
                    }
                }
            }
        } else {
            Group(null) {
                if (blocked.isEmpty()) Text(tr("Brak zablokowanych słów."), color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
                for (w in blocked) {
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(w, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Chip(tr("Odblokuj"), false) { UserModel.unblock(w) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ───────────────────────── skróty ─────────────────────────

@Composable
fun ShortcutsSheet(vm: AppViewModel) {
    val list by UserModel.shortcuts.collectAsState()
    var trigger by remember { mutableStateOf("") }
    var expansion by remember { mutableStateOf("") }
    Panel(tr("Skróty tekstowe"), onClose = { vm.sheet = null }) {
        Text(
            tr("Wpisz skrót i naciśnij spację – zamieni się na pełny tekst. Pojawia się też w pasku podpowiedzi. Backspace cofa zamianę."),
            color = Palette.TextDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Group(tr("Nowy skrót")) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkField(trigger, { trigger = it.filter { c -> !c.isWhitespace() }.take(20) }, tr("Skrót (np. adr)"))
                DarkField(expansion, { expansion = it.take(2000) }, tr("Tekst (np. ul. Długa 5, 00-001 Warszawa)"), singleLine = false, minLines = 2)
                PillButton(tr("Dodaj skrót"), enabled = trigger.isNotBlank() && expansion.isNotBlank()) {
                    UserModel.setShortcuts(list.filter { !it.trigger.equals(trigger, true) } + Shortcut(trigger, expansion))
                    trigger = ""
                    expansion = ""
                }
            }
        }
        Group(tr("Twoje skróty")) {
            if (list.isEmpty()) Text(tr("Brak skrótów."), color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
            for (sc in list) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sc.trigger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.Ink,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    Text(sc.expansion, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                    IconBtn(Icons.Rounded.Delete, tr("Usuń")) { UserModel.setShortcuts(list - sc) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ───────────────────────── pasek narzędzi ─────────────────────────

private fun toolbarIcon(t: ToolbarItem): ImageVector = when (t) {
    ToolbarItem.CLIPBOARD -> Icons.Rounded.ContentPaste
    ToolbarItem.EMOJI -> Icons.Rounded.EmojiEmotions
    ToolbarItem.EDIT -> Icons.Rounded.OpenWith
    ToolbarItem.TEXT_TOOLS -> Icons.Rounded.TextFields
    ToolbarItem.VOICE -> Icons.Rounded.Mic
    ToolbarItem.PASTE -> Icons.Rounded.ContentPasteGo
    ToolbarItem.UNDO -> Icons.AutoMirrored.Rounded.Undo
    ToolbarItem.REDO -> Icons.AutoMirrored.Rounded.Redo
    ToolbarItem.SELECT_ALL -> Icons.Rounded.SelectAll
    ToolbarItem.LANGUAGES -> Icons.Rounded.Language
    ToolbarItem.RESIZE -> Icons.Rounded.Height
    ToolbarItem.INCOGNITO -> Icons.Rounded.VisibilityOff
    ToolbarItem.SYMBOLS -> Icons.Rounded.Functions
    ToolbarItem.ONE_HANDED -> Icons.Rounded.SwapHoriz
    ToolbarItem.FLOATING -> Icons.Rounded.PictureInPicture
    ToolbarItem.NUMPAD -> Icons.Rounded.Dialpad
    ToolbarItem.SETTINGS -> Icons.Rounded.Tune
}

private fun toolbarName(t: ToolbarItem): String = when (t) {
    ToolbarItem.CLIPBOARD -> tr("Schowek")
    ToolbarItem.EMOJI -> tr("Emoji")
    ToolbarItem.EDIT -> tr("Kursor i zaznaczanie")
    ToolbarItem.TEXT_TOOLS -> tr("Narzędzia tekstu")
    ToolbarItem.VOICE -> tr("Dyktowanie")
    ToolbarItem.PASTE -> tr("Wklej")
    ToolbarItem.UNDO -> tr("Cofnij")
    ToolbarItem.REDO -> tr("Ponów")
    ToolbarItem.SELECT_ALL -> tr("Zaznacz wszystko")
    ToolbarItem.LANGUAGES -> tr("Języki")
    ToolbarItem.RESIZE -> tr("Rozmiar klawiatury")
    ToolbarItem.INCOGNITO -> tr("Incognito")
    ToolbarItem.SYMBOLS -> tr("Symbole")
    ToolbarItem.ONE_HANDED -> tr("Jedna ręka")
    ToolbarItem.FLOATING -> tr("Klawiatura pływająca")
    ToolbarItem.NUMPAD -> tr("Klawiatura numeryczna")
    ToolbarItem.SETTINGS -> tr("Szybkie ustawienia")
}

@Composable
fun ToolbarSheet(vm: AppViewModel) {
    val s = vm.settings
    Panel(tr("Pasek narzędzi"), onClose = { vm.sheet = null }) {
        Text(
            tr("Pasek widać nad klawiszami od razu po otwarciu klawiatury – zanim zaczniesz pisać (i po dotknięciu ikony siatki obok podpowiedzi). Włącz potrzebne przyciski i ustaw ich kolejność; pasek przewija się w bok, gdy jest ich dużo."),
            color = Palette.TextDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        val ordered = s.toolbar + ToolbarItem.entries.filter { it !in s.toolbar }
        Group(null) {
            ordered.forEach { t ->
                val on = t in s.toolbar
                val i = s.toolbar.indexOf(t)
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RowIcon(toolbarIcon(t))
                    Text(toolbarName(t), fontSize = 14.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    if (on) {
                        IconBtn(Icons.Rounded.KeyboardArrowUp, tr("Wyżej"), enabled = i > 0) {
                            vm.update { st -> st.copy(toolbar = st.toolbar.toMutableList().also { l -> l[i] = l[i - 1]; l[i - 1] = t }) }
                        }
                        IconBtn(Icons.Rounded.KeyboardArrowDown, tr("Niżej"), enabled = i < s.toolbar.lastIndex) {
                            vm.update { st -> st.copy(toolbar = st.toolbar.toMutableList().also { l -> l[i] = l[i + 1]; l[i + 1] = t }) }
                        }
                    }
                    Switch(
                        checked = on,
                        onCheckedChange = { v -> vm.update { st -> st.copy(toolbar = if (v) st.toolbar + t else st.toolbar - t) } },
                        modifier = Modifier.scale(0.82f),
                        colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.Ink),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ───────────────────────── o aplikacji ─────────────────────────

@Composable
fun AboutSheet(vm: AppViewModel) {
    val sys = LocalSystem.current
    Panel(tr("O aplikacji"), onClose = { vm.sheet = null }) {
        Group(tr("Dane i usługi")) {
            ActionRow(Icons.Rounded.Translate, "FrequencyWords (Hermit Dave)", tr("Listy częstości słów z napisów OpenSubtitles · CC-BY-SA 4.0")) {
                sys.openUrl("https://github.com/hermitdave/FrequencyWords")
            }
            ActionRow(Icons.Rounded.EmojiEmotions, "Unicode Emoji", tr("Lista emoji, nazwy i odcienie skóry (emoji-test.txt) · licencja Unicode")) {
                sys.openUrl("https://unicode.org/emoji/")
            }

            ActionRow(Icons.Rounded.Mic, tr("Rozpoznawanie mowy systemu"), tr("Dyktowanie korzysta z usługi zainstalowanej w telefonie")) {}
        }
        Group(tr("Prywatność")) {
            Text(
                tr("Wszystko, czego klawiatura się uczy (słowa, pary słów, schowek, statystyki), zostaje na telefonie. Aplikacja łączy się z internetem tylko, by pobrać słownik albo sprawdzić aktualizację na GitHubie."),
                color = Palette.TextDim,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(14.dp),
            )
        }
        Text(
            tr("Słowniki zajmują {0} KB", Dictionaries.storageBytes() / 1024),
            color = Palette.TextDim,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(12.dp))
    }
}

// ───────────────────────── gesty ─────────────────────────

fun gestureName(a: GestureAction): String = when (a) {
    GestureAction.NONE -> tr("Nic")
    GestureAction.CURSOR -> tr("Przesuwanie kursora")
    GestureAction.LANGUAGE -> tr("Następny język")
    GestureAction.HIDE -> tr("Schowaj klawiaturę")
    GestureAction.DELETE_WORD -> tr("Usuń słowo")
    GestureAction.SHIFT -> tr("Caps Lock")
    GestureAction.EMOJI -> tr("Emoji")
    GestureAction.CLIPBOARD -> tr("Schowek")
    GestureAction.VOICE -> tr("Dyktowanie")
    GestureAction.TEXT_TOOLS -> tr("Narzędzia tekstu")
    GestureAction.LANGUAGES -> tr("Lista języków")
    GestureAction.IME_PICKER -> tr("Wybór klawiatury")
    GestureAction.NUMPAD -> tr("Klawiatura numeryczna")
    GestureAction.UNDO -> tr("Cofnij")
}

@Composable
private fun GestureRow(
    icon: ImageVector,
    title: String,
    value: GestureAction,
    options: List<GestureAction>,
    enabled: Boolean = true,
    requires: String? = null,
    onPick: (GestureAction) -> Unit,
) {
    Column(Modifier.fillMaxWidth().then(if (enabled) Modifier else Modifier.alpha(DISABLED_ALPHA)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Column(Modifier.padding(horizontal = 12.dp)) {
                Text(title, fontSize = 14.sp)
                Text((if (enabled) null else requires) ?: gestureName(value), fontSize = 11.sp, color = if (enabled) Palette.TextDim else Palette.Amber)
            }
        }
        Row(
            Modifier.padding(start = 44.dp, top = 6.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            for (o in options) Chip(gestureName(o), o == value) { if (enabled) onPick(o) }
        }
    }
}

@Composable
fun GesturesSheet(vm: AppViewModel) {
    val s = vm.settings
    val all = GestureAction.entries.filter { it != GestureAction.CURSOR }
    Panel(tr("Gesty"), onClose = { vm.sheet = null }) {
        Text(
            tr("Każdemu gestowi możesz przypisać własną czynność. Pisanie gestem (przeciąganie po literach) działa niezależnie – włączysz je w sekcji pisania."),
            color = Palette.TextDim,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Group(null) {
            GestureRow(Icons.Rounded.SwapHoriz, tr("Spacja – przesunięcie w bok"), s.spaceSwipe, listOf(GestureAction.CURSOR, GestureAction.LANGUAGE, GestureAction.NONE)) { v -> vm.update { it.copy(spaceSwipe = v) } }
            GestureRow(Icons.Rounded.KeyboardArrowUp, tr("Spacja – przesunięcie w górę"), s.spaceSwipeUp, all) { v -> vm.update { it.copy(spaceSwipeUp = v) } }
            GestureRow(Icons.Rounded.Tune, tr("Spacja – przytrzymanie"), s.spaceLongPress, all) { v -> vm.update { it.copy(spaceLongPress = v) } }
            GestureRow(Icons.Rounded.KeyboardArrowDown, tr("Przeciągnięcie w dół po klawiszach"), s.swipeDown, all) { v -> vm.update { it.copy(swipeDown = v) } }
            GestureRow(Icons.Rounded.EmojiEmotions, tr("Enter – przytrzymanie"), s.enterLongPress, all) { v -> vm.update { it.copy(enterLongPress = v) } }
            GestureRow(Icons.Rounded.Add, tr("Przecinek – przytrzymanie"), s.commaLongPress, all, enabled = s.commaKey, requires = tr("Działa po włączeniu: {0}", tr("Przecinek"))) { v -> vm.update { it.copy(commaLongPress = v) } }
        }
        Text(
            tr("Przytrzymanie przecinka ustawione na „Nic” pokazuje znaki ; : „ ”. Przytrzymaną spację możesz też przesunąć w bok, aby zmienić język (opcja w sekcji Języki)."),
            color = Palette.TextDim,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(12.dp))
    }
}

// ───────────────────────── własny motyw ─────────────────────────

private enum class ColorSlot { BG, KEY, TEXT, ACCENT }

@Composable
fun CustomThemeSection(vm: AppViewModel) {
    val s = vm.settings
    val sys = LocalSystem.current
    val custom = s.theme == "custom"
    var slot by remember { mutableStateOf(ColorSlot.BG) }
    Group(tr("Własny motyw")) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (custom) tr("Aktywny – zmieniaj kolory poniżej") else tr("Ułóż własne kolory klawiatury i obraz tła"),
                fontSize = 12.sp,
                color = Palette.TextDim,
                modifier = Modifier.weight(1f),
            )
            if (!custom) PillButton(tr("Użyj")) { vm.update { it.copy(theme = "custom") } }
        }
        if (custom) {
            Row(Modifier.padding(horizontal = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (sl in ColorSlot.entries) {
                    val argb = slotColor(s, sl)
                    val name = when (sl) {
                        ColorSlot.BG -> tr("Tło")
                        ColorSlot.KEY -> tr("Klawisze")
                        ColorSlot.TEXT -> tr("Znaki")
                        ColorSlot.ACCENT -> tr("Akcent")
                    }
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sl == slot) Color.White else Color.White.copy(alpha = 0.08f))
                            .clickable { slot = sl }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(argb.toInt())).border(1.dp, Color.Gray, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(name, fontSize = 12.sp, color = if (sl == slot) Palette.Ink else Color.White)
                    }
                }
            }
            val current = slotColor(s, slot)
            fun setColor(v: Long) = vm.update {
                when (slot) {
                    ColorSlot.BG -> it.copy(customBg = v)
                    ColorSlot.KEY -> it.copy(customKey = v)
                    ColorSlot.TEXT -> it.copy(customText = v)
                    ColorSlot.ACCENT -> it.copy(customAccent = v)
                }
            }
            val swatches = listOf(
                0xFF000000, 0xFF111111, 0xFF1E1E1E, 0xFF2B2B2B, 0xFF3A3A3A, 0xFFFFFFFF, 0xFFECEFF1, 0xFFF5EEDC,
                0xFF0D1B2A, 0xFF1B263B, 0xFF14213D, 0xFF003049, 0xFF2D0A31, 0xFF3C1642, 0xFF1F2F16, 0xFF0B3D2E,
                0xFFE63946, 0xFFF4A261, 0xFFE9C46A, 0xFF2A9D8F, 0xFF64B5F6, 0xFFBA68C8, 0xFFF06292, 0xFF81C784,
            )
            Row(Modifier.padding(10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (sw in swatches) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (sw == current) Color.White else Color.White.copy(alpha = 0.15f))
                            .clickable { setColor(sw) }
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Color(sw.toInt())),
                    )
                }
            }
            val hsv = remember(current) { FloatArray(3).also { android.graphics.Color.colorToHSV(current.toInt(), it) } }
            fun fromHsv(h: Float, sat: Float, v: Float): Long = android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v)).toLong() and 0xFFFFFFFFL
            SliderRow(Icons.Rounded.Palette, tr("Barwa"), hsv[0].toInt(), 0..360, 1, { "$it°" }) { v -> setColor(fromHsv(v.toFloat(), hsv[1], hsv[2])) }
            SliderRow(Icons.Rounded.Palette, tr("Nasycenie"), (hsv[1] * 100).toInt(), 0..100, 1, { "$it%" }) { v -> setColor(fromHsv(hsv[0], v / 100f, hsv[2])) }
            SliderRow(Icons.Rounded.Palette, tr("Jasność"), (hsv[2] * 100).toInt(), 0..100, 1, { "$it%" }) { v -> setColor(fromHsv(hsv[0], hsv[1], v / 100f)) }
        }
        ActionRow(
            Icons.Rounded.Image,
            if (s.bgImage) tr("Zmień obraz tła") else tr("Obraz w tle klawiatury"),
            tr("Twoje zdjęcie pod klawiszami (przyciemnione dla czytelności)"),
        ) { sys.pickBackground() }
        if (s.bgImage) {
            SliderRow(Icons.Rounded.Contrast, tr("Przyciemnienie obrazu"), s.bgDim, 0..90, 1, { "$it%" }) { v -> vm.update { it.copy(bgDim = v) } }
            ActionRow(Icons.Rounded.Delete, tr("Usuń obraz tła"), null, danger = true) { vm.update { it.copy(bgImage = false) } }
        }
        if (s.theme == "custom") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RowIcon(Icons.Rounded.Gradient)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(tr("Gradient tła"), fontSize = 14.sp)
                    Text(tr("Tło przechodzi od koloru „Tło” do drugiego koloru u dołu"), fontSize = 11.sp, color = Palette.TextDim)
                }
                Switch(
                    checked = s.customGradient,
                    onCheckedChange = { v -> vm.update { it.copy(customGradient = v) } },
                    modifier = Modifier.scale(0.82f),
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.Ink),
                )
            }
            if (s.customGradient) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (sw in listOf(0xFF1A1033, 0xFF2A0B23, 0xFF011522, 0xFF3D0F05, 0xFF0B3D2E, 0xFF000000, 0xFF4A1942, 0xFF14213D, 0xFF8A2A1D, 0xFFE9C46A, 0xFF64B5F6, 0xFFFFFFFF)) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (sw == s.customBg2) Color.White else Color.White.copy(alpha = 0.15f))
                                .clickable { vm.update { it.copy(customBg2 = sw) } }
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(Color(sw.toInt())),
                        )
                    }
                }
            }
        }
        SliderRow(Icons.Rounded.Opacity, tr("Krycie klawiszy"), s.keyOpacity, 0..100, 5, { "$it%" }) { v -> vm.update { it.copy(keyOpacity = v) } }
    }
    Group(null) {
        CompactChoice(
            Icons.Rounded.FontDownload,
            tr("Czcionka klawiszy"),
            KeyFont.entries,
            s.keyFont,
            {
                when (it) {
                    KeyFont.DEFAULT -> tr("Systemowa")
                    KeyFont.SANS -> tr("Bezszeryfowa")
                    KeyFont.SERIF -> tr("Szeryfowa")
                    KeyFont.MONO -> tr("Maszynowa")
                    KeyFont.CONDENSED -> tr("Odręczna")
                }
            },
            short = {
                when (it) {
                    KeyFont.DEFAULT -> "Aa"
                    KeyFont.SANS -> "Sans"
                    KeyFont.SERIF -> "Serif"
                    KeyFont.MONO -> "Mono"
                    KeyFont.CONDENSED -> "✎"
                }
            },
        ) { v -> vm.update { it.copy(keyFont = v) } }
    }
}

private fun slotColor(s: KeyboardSettings, sl: ColorSlot): Long = when (sl) {
    ColorSlot.BG -> s.customBg
    ColorSlot.KEY -> s.customKey
    ColorSlot.TEXT -> s.customText
    ColorSlot.ACCENT -> s.customAccent
}

// ───────────────────────── statystyki ─────────────────────────

/**
 * Statystyki pisania: tempo (słowa na minutę aktywnego pisania), liczby, najczęstsze słowa
 * i mapa cieplna – im jaśniejszy klawisz, tym częściej go naciskasz.
 */
@Composable
fun StatsSheet(vm: AppViewModel) {
    val st by UserModel.stats.collectAsState()
    val version by UserModel.version.collectAsState()
    val top = remember(version) { UserModel.allWords().take(12) }
    val s = vm.settings
    Panel(tr("Statystyki"), onClose = { vm.sheet = null }) {
        val minutes = st.typingMs / 60_000.0
        val wpm = if (minutes > 0.2) (st.words / minutes).toInt() else 0
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            com.blacksingularities.openkeyboard.ui.components.StatTile(if (wpm > 0) wpm.toString() else "—", tr("słów na minutę"), Modifier.weight(1f))
            com.blacksingularities.openkeyboard.ui.components.StatTile(st.words.toString(), tr("słów"), Modifier.weight(1f))
            com.blacksingularities.openkeyboard.ui.components.StatTile(st.corrections.toString(), tr("poprawek"), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            com.blacksingularities.openkeyboard.ui.components.StatTile(st.charsSaved.toString(), tr("znaków zaoszczędzonych"), Modifier.weight(1f))
            com.blacksingularities.openkeyboard.ui.components.StatTile(st.suggestionsPicked.toString(), tr("wybranych podpowiedzi"), Modifier.weight(1f))
            com.blacksingularities.openkeyboard.ui.components.StatTile(st.emojis.toString(), tr("emoji"), Modifier.weight(1f))
        }
        Group(tr("Mapa cieplna klawiszy")) {
            val layout = remember(s.activeLanguage, s.layoutOverride) {
                Layouts.build(KeyboardMode.ALPHA, Languages.get(s.activeLanguage), s.copy(numberRow = false), FieldKind.TEXT, 1)
            }
            val max = (st.keyTaps.values.maxOrNull() ?: 0L).coerceAtLeast(1L)
            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in layout.rows.take(3)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                        for (k in row) {
                            if (k.type != com.blacksingularities.openkeyboard.keyboard.KeyType.CHAR) continue
                            val n = st.keyTaps[k.text.lowercase()] ?: 0L
                            val heat = (n.toFloat() / max).coerceIn(0f, 1f)
                            Column(
                                Modifier
                                    .weight(1f, fill = false)
                                    .size(width = 30.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(heatColor(heat)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(k.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (heat > 0.55f) Palette.Ink else Color.White)
                                if (n > 0) Text(compactCount(n), fontSize = 8.sp, color = if (heat > 0.55f) Palette.Ink else Palette.TextDim)
                            }
                        }
                    }
                }
            }
            Text(
                tr("Liczone są litery wpisane na klawiaturze (nie w trybie incognito)."),
                color = Palette.TextDim,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Group(tr("Twoje najczęstsze słowa")) {
            if (top.isEmpty()) Text(tr("Brak słów – pisz, a klawiatura sama się nauczy."), color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
            val maxW = (top.firstOrNull()?.second ?: 1).coerceAtLeast(1)
            for ((w, n) in top) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(w, fontSize = 13.sp, modifier = Modifier.width(110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))) {
                        Box(Modifier.fillMaxWidth(n.toFloat() / maxW).height(8.dp).clip(RoundedCornerShape(50)).background(Color.White))
                    }
                    Text("×$n", fontSize = 11.sp, color = Palette.TextDim, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun heatColor(t: Float): Color {
    if (t <= 0f) return Color.White.copy(alpha = 0.06f)
    // Od ciemnego granatu przez pomarańcz do bieli.
    val stops = listOf(Color(0xFF1B2A4A), Color(0xFF7B2D8E), Color(0xFFE8613C), Color(0xFFFFD166), Color(0xFFFFFFFF))
    val x = t * (stops.size - 1)
    val i = x.toInt().coerceAtMost(stops.size - 2)
    val f = x - i
    val a = stops[i]
    val b = stops[i + 1]
    return Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f)
}

private fun compactCount(n: Long): String = if (n >= 1000) "${n / 1000}k" else n.toString()

// ───────────────────────── motyw dzienny i nocny ─────────────────────────

@Composable
fun DayNightSection(vm: AppViewModel) {
    val s = vm.settings
    Group(tr("Dzień i noc")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RowIcon(Icons.Rounded.Contrast)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(tr("Przełączaj motyw z systemem"), fontSize = 14.sp)
                Text(tr("Gdy telefon jest w trybie ciemnym – motyw nocny, w jasnym – dzienny."), fontSize = 11.sp, color = Palette.TextDim, lineHeight = 14.sp)
            }
            Switch(
                checked = s.autoDayNight,
                onCheckedChange = { v -> vm.update { it.copy(autoDayNight = v) } },
                modifier = Modifier.scale(0.82f),
                colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.Ink),
            )
        }
        if (s.autoDayNight) {
            val ids = KeyboardThemes.presets.map { it.id } + "custom"
            fun name(id: String) = if (id == "custom") tr("Własny motyw") else tr(KeyboardThemes.presets.first { it.id == id }.name)
            Text(tr("Motyw dzienny"), fontSize = 12.sp, color = Palette.TextDim, modifier = Modifier.padding(start = 14.dp, top = 4.dp))
            Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (id in ids) Chip(name(id), id == s.dayTheme) { vm.update { it.copy(dayTheme = id) } }
            }
            Text(tr("Motyw nocny"), fontSize = 12.sp, color = Palette.TextDim, modifier = Modifier.padding(start = 14.dp, top = 4.dp))
            Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (id in ids) Chip(name(id), id == s.nightTheme) { vm.update { it.copy(nightTheme = id) } }
            }
        }
    }
}

// ───────────────────────── dowolny akcent ─────────────────────────

/** Dowolny kolor akcentu (barwa, nasycenie, jasność) i litery klawiszy w tym kolorze. */
@Composable
private fun AccentPicker(vm: AppViewModel) {
    val s = vm.settings
    val current = if (s.accentColor != 0L) s.accentColor else (KeyboardThemes.accentOf(s).toArgb().toLong() and 0xFFFFFFFFL)
    val hsv = remember(current) { FloatArray(3).also { android.graphics.Color.colorToHSV(current.toInt(), it) } }
    fun fromHsv(h: Float, sat: Float, v: Float): Long = android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v)).toLong() and 0xFFFFFFFFL
    fun setColor(v: Long) = vm.update { it.copy(accentColor = v) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(Color(current.toInt())).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(tr("Dowolny kolor"), fontSize = 14.sp)
            Text(
                if (s.accentColor != 0L) "#" + "%06X".format(current and 0xFFFFFF) else tr("Przesuń suwaki, aby ustawić własny"),
                fontSize = 11.sp,
                color = Palette.TextDim,
            )
        }
        if (s.accentColor != 0L) Chip(tr("Z palety"), false) { vm.update { it.copy(accentColor = 0) } }
    }
    SliderRow(Icons.Rounded.Palette, tr("Barwa"), hsv[0].toInt(), 0..360, 1, { "$it°" }) { v -> setColor(fromHsv(v.toFloat(), hsv[1].coerceAtLeast(0.05f), hsv[2].coerceAtLeast(0.3f))) }
    SliderRow(Icons.Rounded.Palette, tr("Nasycenie"), (hsv[1] * 100).toInt(), 0..100, 1, { "$it%" }) { v -> setColor(fromHsv(hsv[0], v / 100f, hsv[2])) }
    SliderRow(Icons.Rounded.Palette, tr("Jasność"), (hsv[2] * 100).toInt(), 0..100, 1, { "$it%" }) { v -> setColor(fromHsv(hsv[0], hsv[1], v / 100f)) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RowIcon(Icons.Rounded.TextFields)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(tr("Litery w kolorze akcentu"), fontSize = 14.sp)
            Text(tr("Znaki na klawiszach liter mają kolor akcentu"), fontSize = 11.sp, color = Palette.TextDim)
        }
        Switch(
            checked = s.accentLabels,
            onCheckedChange = { v -> vm.update { it.copy(accentLabels = v) } },
            modifier = Modifier.scale(0.82f),
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.Ink),
        )
    }
}
