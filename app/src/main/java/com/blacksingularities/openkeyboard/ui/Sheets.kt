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
import androidx.compose.material.icons.rounded.Tune
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
        Group(tr("Włączone (kolejność przełączania)")) {
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
                if (!active) Chip(tr("Ustaw bieżący"), false) { vm.update { it.copy(activeLanguage = code) } }
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
    val colors = KeyboardThemes.colors(s)
    val layout = remember(s) { Layouts.build(KeyboardMode.ALPHA, Languages.get(s.activeLanguage), s, FieldKind.TEXT, s.languages.size) }
    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides colors.text) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.background).padding(bottom = 6.dp)) {
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
                            .clickable { set { it.copy(theme = p.id) } }
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
        Group(tr("Kolor akcentu")) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyboardThemes.accents.forEachIndexed { i, c ->
                    val sel = i == s.accent
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (sel) Color.White else Color.White.copy(alpha = 0.12f))
                            .clickable { set { it.copy(accent = i) } }
                            .padding(if (sel) 3.dp else 2.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(if (c == Color.White) Modifier.border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape) else Modifier),
                    )
                }
            }
        }
        Group(null) {
            CompactChoice(
                Icons.Rounded.Tune,
                tr("Klawisze"),
                KeyStyle.entries,
                s.keyStyle,
                { when (it) { KeyStyle.FILLED -> tr("Wypełnione"); KeyStyle.OUTLINE -> tr("Z obwódką"); KeyStyle.FLAT -> tr("Same znaki (minimalistyczne)") } },
                short = { when (it) { KeyStyle.FILLED -> tr("Pełne"); KeyStyle.OUTLINE -> tr("Obrys"); KeyStyle.FLAT -> tr("Płaskie") } },
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
    val words = remember(version, query) {
        UserModel.allWords().filter { query.isBlank() || it.first.contains(query, true) }.take(300)
    }
    val blocked = remember(version) { UserModel.blockedWords() }
    Panel(tr("Słownik użytkownika"), onClose = { vm.sheet = null }) {
        Text(
            tr("Słowa zapamiętane podczas pisania i dodane ręcznie. Słowo zablokowane (przytrzymaj podpowiedź w klawiaturze) nie będzie podpowiadane."),
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
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
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
    ToolbarItem.VOICE -> Icons.Rounded.Mic
    ToolbarItem.TRANSLATE -> Icons.Rounded.Translate
    ToolbarItem.ONE_HANDED -> Icons.Rounded.SwapHoriz
    ToolbarItem.FLOATING -> Icons.Rounded.PictureInPicture
    ToolbarItem.NUMPAD -> Icons.Rounded.Dialpad
    ToolbarItem.SETTINGS -> Icons.Rounded.Tune
}

private fun toolbarName(t: ToolbarItem): String = when (t) {
    ToolbarItem.CLIPBOARD -> tr("Schowek")
    ToolbarItem.EMOJI -> tr("Emoji")
    ToolbarItem.EDIT -> tr("Kursor i zaznaczanie")
    ToolbarItem.VOICE -> tr("Dyktowanie")
    ToolbarItem.TRANSLATE -> tr("Tłumacz")
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
            tr("Pasek pokazuje się nad klawiszami, gdy nie ma podpowiedzi (albo po dotknięciu ikony siatki). Włącz potrzebne przyciski i ustaw ich kolejność."),
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
            ActionRow(Icons.Rounded.Translate, "MyMemory", tr("Darmowe tłumaczenia w klawiaturze – tekst wysyłany tylko po użyciu tłumacza")) {
                sys.openUrl("https://mymemory.translated.net/")
            }
            ActionRow(Icons.Rounded.Mic, tr("Rozpoznawanie mowy systemu"), tr("Dyktowanie korzysta z usługi zainstalowanej w telefonie")) {}
        }
        Group(tr("Prywatność")) {
            Text(
                tr("Wszystko, czego klawiatura się uczy (słowa, pary słów, schowek, statystyki), zostaje na telefonie. Aplikacja łączy się z internetem tylko, by pobrać słownik, sprawdzić aktualizację na GitHubie albo przetłumaczyć tekst, gdy o to poprosisz."),
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
