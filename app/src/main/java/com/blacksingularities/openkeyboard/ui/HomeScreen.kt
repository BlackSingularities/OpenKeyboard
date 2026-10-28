package com.blacksingularities.openkeyboard.ui

import androidx.compose.material.icons.rounded.Functions
import com.blacksingularities.openkeyboard.data.EmojiKey
import com.blacksingularities.openkeyboard.data.NavSpace
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardCapslock
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwipeDown
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.openkeyboard.LocalSystem
import com.blacksingularities.openkeyboard.data.AppLanguage
import com.blacksingularities.openkeyboard.data.Autocorrect
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Languages
import com.blacksingularities.openkeyboard.data.OneHanded
import com.blacksingularities.openkeyboard.data.SplitMode
import com.blacksingularities.openkeyboard.data.KeyStyle
import com.blacksingularities.openkeyboard.data.KeySound
import com.blacksingularities.openkeyboard.ui.components.LocalSectionAnchors
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.automirrored.rounded.Undo
import com.blacksingularities.openkeyboard.data.Prefs
import com.blacksingularities.openkeyboard.dict.UserModel
import com.blacksingularities.openkeyboard.features.ClipboardHistory
import com.blacksingularities.openkeyboard.i18n.tr
import com.blacksingularities.openkeyboard.ui.components.ActionRow
import com.blacksingularities.openkeyboard.ui.components.CompactChoice
import com.blacksingularities.openkeyboard.ui.components.DarkDialog
import com.blacksingularities.openkeyboard.ui.components.DarkField
import com.blacksingularities.openkeyboard.ui.components.Group
import com.blacksingularities.openkeyboard.ui.components.PillButton
import com.blacksingularities.openkeyboard.ui.components.SectionHeader
import com.blacksingularities.openkeyboard.ui.components.SliderRow
import com.blacksingularities.openkeyboard.ui.components.StatTile
import com.blacksingularities.openkeyboard.ui.components.Tile
import com.blacksingularities.openkeyboard.ui.components.TileGrid
import com.blacksingularities.openkeyboard.ui.theme.Palette

@Composable
fun AppRoot(vm: AppViewModel) {
    Box(Modifier.fillMaxSize().background(Palette.Ink)) {
        HomeScreen(vm)
        when (vm.sheet) {
            Sheet.LANGUAGES -> LanguagesSheet(vm)
            Sheet.THEME -> ThemeSheet(vm)
            Sheet.DICTIONARY -> DictionarySheet(vm)
            Sheet.SHORTCUTS -> ShortcutsSheet(vm)
            Sheet.TOOLBAR -> ToolbarSheet(vm)
            Sheet.ABOUT -> AboutSheet(vm)
            Sheet.GESTURES -> GesturesSheet(vm)
            Sheet.STATS -> StatsSheet(vm)
            null -> {}
        }
        val msg = vm.message
        AnimatedVisibility(
            msg != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 24.dp),
        ) {
            Text(
                msg.orEmpty(),
                color = Palette.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun HomeScreen(vm: AppViewModel) {
    val scroll = rememberScrollState()
    val anchors = remember { mutableMapOf<String, Int>() }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompositionLocalProvider(LocalSectionAnchors provides anchors) {
                Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Header(vm)
                    UpdateBanner(vm)
                    SetupCard(vm)
                    TryField()
                    Stats(vm)
                    SettingsContent(vm)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        // Skróty do sekcji – szybkie przejście bez przewijania całej listy.
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.Ink)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (title in listOf(tr("Języki"), tr("Pisanie i podpowiedzi"), tr("Wygląd"), tr("Układ"), tr("Dotyk i dźwięk"), tr("Narzędzia"), tr("Kopia zapasowa"), tr("Ogólne"))) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { anchors[title]?.let { y -> scope.launch { scroll.animateScrollTo(y) } } }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(vm: AppViewModel) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(com.blacksingularities.openkeyboard.R.drawable.brand_mark),
            contentDescription = null, modifier = Modifier.size(42.dp),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("OpenKeyboard", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(tr("Wersja {0} · {1} języków", vm.appVersion, Languages.all.size), fontSize = 12.sp, color = Palette.TextDim)
        }
    }
}

/** Dyktowanie: stan mikrofonu i usługi rozpoznawania mowy, opcje. */
@Composable
private fun VoiceGroup(vm: AppViewModel) {
    val sys = LocalSystem.current
    val s = vm.settings
    Group(tr("Dyktowanie")) {
        when {
            !vm.speechAvailable -> ActionRow(
                Icons.Rounded.MicOff,
                tr("Brak usługi rozpoznawania mowy"),
                tr("Zainstaluj lub włącz aplikację Google (rozpoznawanie mowy), a potem wróć tutaj"),
            ) { sys.openUrl("market://details?id=com.google.android.googlequicksearchbox") }
            !vm.micGranted -> ActionRow(
                Icons.Rounded.MicOff,
                tr("Mikrofon zablokowany"),
                if (vm.micAsked) tr("Dotknij, aby otworzyć ustawienia aplikacji i zezwolić na mikrofon") else tr("Dotknij, aby zezwolić klawiaturze na mikrofon"),
            ) { if (vm.micAsked) sys.openAppSettings() else sys.requestMic() }
            else -> ActionRow(Icons.Rounded.Mic, tr("Dyktowanie gotowe"), tr("Mikrofon w pasku narzędzi klawiatury; język = bieżący język pisania")) { sys.openVoiceSettings() }
        }
        ActionRow(Icons.Rounded.Settings, tr("Ustawienia rozpoznawania mowy"), tr("Języki do pobrania (dyktowanie bez internetu) i usługa mowy systemu")) { sys.openVoiceSettings() }
    }
    TileGrid(
        listOf(
            Tile(Icons.Rounded.Mic, tr("Wielka litera w dyktowaniu"), tr("Dyktowany tekst na początku zdania zaczyna się wielką literą"), s.voiceAutoPunct) { v -> vm.update { it.copy(voiceAutoPunct = v) } },
        ),
    )
}

/** Kroki konfiguracji: włączenie klawiatury, wybór domyślnej, mikrofon, powiadomienia. */
@Composable
private fun SetupCard(vm: AppViewModel) {
    val sys = LocalSystem.current
    val ctx = LocalContext.current
    val micOk = vm.micGranted
    val notifOk = android.os.Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val ready = vm.imeEnabled && vm.imeSelected
    Group(if (ready) tr("Klawiatura gotowa") else tr("Pierwsze kroki")) {
        StepRow(1, tr("Włącz OpenKeyboard"), tr("W ustawieniach systemu: Klawiatury → OpenKeyboard"), vm.imeEnabled, tr("Włącz")) { sys.openImeSettings() }
        StepRow(2, tr("Ustaw jako domyślną"), tr("Wybierz OpenKeyboard z listy klawiatur"), vm.imeSelected, tr("Wybierz"), enabled = vm.imeEnabled) { sys.showImePicker() }
        StepRow(3, tr("Dyktowanie (opcjonalnie)"), tr("Dostęp do mikrofonu dla przycisku dyktowania"), micOk, tr("Zezwól")) { sys.requestMic() }
        if (!notifOk) StepRow(4, tr("Powiadomienia (opcjonalnie)"), tr("Informacja o zainstalowanej aktualizacji"), false, tr("Zezwól")) { sys.requestNotifications() }
    }
}

@Composable
private fun StepRow(n: Int, title: String, subtitle: String, done: Boolean, action: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (done) Color.White else Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.Check, null, tint = Palette.Ink, modifier = Modifier.size(18.dp))
            else Text(n.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontSize = 14.sp, color = if (enabled || done) Color.White else Palette.TextFaint)
            Text(subtitle, fontSize = 11.sp, color = Palette.TextDim, lineHeight = 14.sp)
        }
        if (!done) PillButton(action, enabled = enabled, onClick = onClick)
    }
}

@Composable
private fun TryField() {
    var text by remember { mutableStateOf("") }
    Group(tr("Wypróbuj")) {
        Column(Modifier.padding(10.dp)) {
            DarkField(text, { text = it }, tr("Napisz coś – np. „zolw” albo „=12*7”"), singleLine = false, minLines = 2)
        }
    }
}

@Composable
private fun Stats(vm: AppViewModel) {
    val st by UserModel.stats.collectAsState()
    if (st.words == 0L) return
    Row(Modifier.fillMaxWidth().padding(top = 4.dp).clickable { vm.sheet = Sheet.STATS }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatTile(compact(st.words), tr("słów"), Modifier.weight(1f))
        StatTile(compact(st.corrections), tr("poprawek"), Modifier.weight(1f))
        StatTile(compact(st.charsSaved), tr("znaków zaoszczędzonych"), Modifier.weight(1f))
        StatTile(compact(st.emojis), tr("emoji"), Modifier.weight(1f))
    }
}

private fun compact(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 10_000 -> "${n / 1000}k"
    else -> n.toString()
}

@Composable
private fun ColumnScope.SettingsContent(vm: AppViewModel) {
    val s = vm.settings
    val sys = LocalSystem.current
    fun set(t: (KeyboardSettings) -> KeyboardSettings) = vm.update(t)
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    // Opcje, które teraz nic nie zmieniają, są przygaszone – z wyjaśnieniem, co je włącza.
    fun needs(name: String) = tr("Działa po włączeniu: {0}", name)
    val incognitoNote = tr("Nie działa przy „Zawsze incognito”")
    val manyLanguages = s.languages.size > 1
    val switchable = Languages.switchTargets(s.languages, s.activeLanguage, s.multilingual).size > 1
    val switchNote = if (!manyLanguages) tr("Działa przy co najmniej dwóch językach") else tr("W trybie „Wiele naraz” działa tylko z językiem o innym alfabecie (np. rosyjskim)")
    val suggestNote = needs(tr("Podpowiedzi"))
    val learns = s.learnWords && !s.forceIncognito

    SectionHeader(Icons.Rounded.Language, tr("Języki"), tr("W jakich językach piszesz. Podpowiedzi mogą działać we wszystkich naraz – bez przełączania klawiatury."))
    Group(null) {
        ActionRow(
            Icons.Rounded.Language,
            tr("Języki i słowniki"),
            s.languages.joinToString(" · ") { Languages.get(it).native } + "  ·  " + tr("dotknij, aby dodać lub pobrać"),
        ) { vm.sheet = Sheet.LANGUAGES }
    }
    TileGrid(
        listOf(
            Tile(Icons.Rounded.Translate, tr("Wiele naraz"), tr("Podpowiedzi i autokorekta ze wszystkich włączonych języków jednocześnie – bez przełączania"), s.multilingual, enabled = manyLanguages, requires = tr("Działa przy co najmniej dwóch językach")) { v -> set { it.copy(multilingual = v) } },
            Tile(Icons.Rounded.Language, tr("Klawisz języka"), tr("Przycisk globusa obok spacji (gdy jest kilka języków)"), s.languageKey, enabled = switchable, requires = switchNote) { v -> set { it.copy(languageKey = v) } },
            Tile(Icons.Rounded.SpaceBar, tr("Język na spacji"), tr("Nazwa bieżącego języka na klawiszu spacji"), s.showSpaceLanguage) { v -> set { it.copy(showSpaceLanguage = v) } },
            Tile(Icons.Rounded.Psychology, tr("Wykrywaj język"), tr("Klawiatura rozpoznaje, w jakim języku właśnie piszesz, i jego słowom daje pierwszeństwo"), s.autoLanguage, enabled = manyLanguages && s.multilingual, requires = if (manyLanguages) needs(tr("Wiele naraz")) else tr("Działa przy co najmniej dwóch językach")) { v -> set { it.copy(autoLanguage = v) } },
            Tile(Icons.Rounded.SwapHoriz, tr("Spacja zmienia język"), tr("Przytrzymaj spację i przesuń w bok – następny lub poprzedni język. Zwykłe przesunięcie spacji przesuwa kursor"), s.spaceSlideLanguage, enabled = switchable, requires = switchNote) { v -> set { it.copy(spaceSlideLanguage = v) } },
        ),
    )

    SectionHeader(Icons.Rounded.Psychology, tr("Pisanie i podpowiedzi"), tr("Jak klawiatura pomaga pisać: poprawia literówki, podpowiada słowa, uczy się Twojego stylu."))
    Group(null) {
        CompactChoice(
            Icons.Rounded.AutoFixHigh,
            tr("Autokorekta"),
            Autocorrect.entries,
            s.autocorrect,
            {
                when (it) {
                    Autocorrect.OFF -> tr("Wyłączona")
                    Autocorrect.GENTLE -> tr("Łagodna – tylko oczywiste literówki")
                    Autocorrect.NORMAL -> tr("Normalna")
                    Autocorrect.AGGRESSIVE -> tr("Mocna – poprawia odważniej")
                }
            },
            short = { when (it) { Autocorrect.OFF -> tr("Wył."); Autocorrect.GENTLE -> "1"; Autocorrect.NORMAL -> "2"; Autocorrect.AGGRESSIVE -> "3" } },
            enabled = s.suggestions,
            requires = suggestNote,
        ) { v -> set { it.copy(autocorrect = v) } }
    }
    TileGrid(
        listOf(
            Tile(Icons.Rounded.Spellcheck, tr("Podpowiedzi"), tr("Pasek z podpowiedziami słów nad klawiaturą"), s.suggestions) { v -> set { it.copy(suggestions = v) } },
            Tile(Icons.Rounded.Psychology, tr("Następne słowo"), tr("Przewidywanie kolejnego słowa na podstawie Twojego pisania"), s.nextWord, enabled = s.suggestions, requires = suggestNote) { v -> set { it.copy(nextWord = v) } },
            Tile(Icons.AutoMirrored.Rounded.MenuBook, tr("Ucz się"), tr("Zapamiętywanie nowych słów i par słów (lokalnie, na telefonie)"), s.learnWords, enabled = !s.forceIncognito, requires = incognitoNote) { v -> set { it.copy(learnWords = v) } },
            Tile(Icons.Rounded.Gesture, tr("Pisanie gestem"), tr("Przeciągnij palcem po literach słowa bez odrywania – klawiatura odgadnie słowo"), s.glideTyping) { v -> set { it.copy(glideTyping = v) } },
            Tile(Icons.Rounded.Draw, tr("Ślad gestu"), tr("Rysowanie śladu palca podczas pisania gestem"), s.glideTrail, enabled = s.glideTyping, requires = needs(tr("Pisanie gestem"))) { v -> set { it.copy(glideTrail = v) } },
            Tile(Icons.AutoMirrored.Rounded.Undo, tr("Ucz się z cofnięć"), tr("Poprawka cofnięta Backspace'em nie wraca, a wpisane słowo trafia do słownika"), s.learnFromUndo,
                enabled = learns && s.suggestions && s.autocorrect != Autocorrect.OFF,
                requires = when {
                    s.forceIncognito -> incognitoNote
                    !s.suggestions -> suggestNote
                    s.autocorrect == Autocorrect.OFF -> needs(tr("Autokorekta"))
                    else -> needs(tr("Ucz się"))
                },
            ) { v -> set { it.copy(learnFromUndo = v) } },
            Tile(Icons.Rounded.FormatQuote, tr("Cudzysłowy „”"), tr("Typograficzne cudzysłowy zależne od języka zamiast prostych"), s.smartQuotes) { v -> set { it.copy(smartQuotes = v) } },
            Tile(Icons.Rounded.SpaceBar, tr("Spacja po , ! ?"), tr("Spacja dodawana automatycznie, gdy po przecinku, wykrzykniku lub pytajniku od razu piszesz dalej"), s.autoSpaceAfterPunct) { v -> set { it.copy(autoSpaceAfterPunct = v) } },
            Tile(Icons.Rounded.KeyboardCapslock, tr("Wielka litera"), tr("Wielka litera na początku zdania – także w podpowiedziach i autokorekcie; poprawia „HEllo” na „Hello” i angielskie dni i miesiące"), s.autoCapitalize) { v -> set { it.copy(autoCapitalize = v) } },
            Tile(Icons.Rounded.KeyboardCapslock, tr("Shift w słowie"), tr("Shift w trakcie pisania słowa: pierwsze naciśnięcie – wielka pierwsza litera, drugie – całe słowo wielkimi. Przy zaznaczeniu zmienia jego wielkość liter"), s.shiftCycleWord) { v -> set { it.copy(shiftCycleWord = v) } },
            Tile(Icons.Rounded.Abc, tr("Znak pociągnięciem"), tr("Pociągnij palcem w górę po klawiszu, aby wstawić znak z jego rogu (cyfrę, symbol, polską literę)"), s.flickSymbols) { v -> set { it.copy(flickSymbols = v) } },
            Tile(Icons.Rounded.Functions, tr("Domykaj nawiasy"), tr("Nawias lub cudzysłów od razu z zamknięciem, kursor w środku"), s.autoPair) { v -> set { it.copy(autoPair = v) } },
            Tile(Icons.Rounded.Calculate, tr("Przelicz jednostki"), tr("Wynik w pasku po wpisaniu np. 5 km, 20°C, 3 kg, 60 mph"), s.unitConvert) { v -> set { it.copy(unitConvert = v) } },
            Tile(Icons.Rounded.SpaceBar, tr("Kropka 2× spacja"), tr("Dwukrotne naciśnięcie spacji wstawia kropkę i spację"), s.doubleSpacePeriod) { v -> set { it.copy(doubleSpacePeriod = v) } },
            Tile(Icons.Rounded.AutoFixHigh, tr("Interpunkcja"), tr("Kropka, przecinek, ! i ? po wybranym słowie trafiają za słowo, a spacja za znak"), s.smartPunctuation) { v -> set { it.copy(smartPunctuation = v) } },
            Tile(Icons.Rounded.EmojiEmotions, tr("Emoji w pasku"), tr("Podpowiadanie emoji do słów (np. „kot” → 🐱)"), s.emojiSuggestions, enabled = s.suggestions, requires = suggestNote) { v -> set { it.copy(emojiSuggestions = v) } },
            Tile(Icons.Rounded.Calculate, tr("Kalkulator"), tr("Wynik działania w pasku: wpisz np. 12*7+3"), s.inlineCalc) { v -> set { it.copy(inlineCalc = v) } },
            Tile(Icons.AutoMirrored.Rounded.Shortcut, tr("Skróty"), tr("Rozwijanie skrótów tekstowych (np. „pzdr” → „Pozdrawiam”); w treści działają {data}, {godzina}, {dzień}, {schowek} i {kursor}"), s.shortcuts) { v -> set { it.copy(shortcuts = v) } },
            Tile(Icons.Rounded.OpenWith, tr("Popraw słowo"), tr("Kursor postawiony na końcu słowa pokazuje dla niego podpowiedzi"), s.reSuggest, enabled = s.suggestions, requires = suggestNote) { v -> set { it.copy(reSuggest = v) } },
            Tile(Icons.Rounded.Block, tr("Bez podpowiedzi, gdy aplikacja prosi"), tr("Niektóre aplikacje (np. Duolingo) wyłączają słownik – domyślnie klawiatura podpowiada mimo to"), s.honorNoSuggestions, enabled = s.suggestions, requires = suggestNote) { v -> set { it.copy(honorNoSuggestions = v) } },
            Tile(Icons.Rounded.ContentPaste, tr("Wklej z paska"), tr("Świeżo skopiowany tekst pojawia się w pasku do wklejenia"), s.clipboardSuggestion,
                enabled = s.clipboardHistory && !s.forceIncognito,
                requires = if (s.forceIncognito) incognitoNote else needs(tr("Historia schowka")),
            ) { v -> set { it.copy(clipboardSuggestion = v) } },
        ),
    )
    Group(null) {
        ActionRow(Icons.AutoMirrored.Rounded.MenuBook, tr("Słownik użytkownika"), tr("Wyuczone i dodane słowa, słowa zablokowane")) { vm.sheet = Sheet.DICTIONARY }
        val shortcuts by UserModel.shortcuts.collectAsState()
        ActionRow(Icons.AutoMirrored.Rounded.Shortcut, tr("Skróty tekstowe"), tr("{0} skrótów", shortcuts.size)) { vm.sheet = Sheet.SHORTCUTS }
    }

    SectionHeader(Icons.Rounded.Palette, tr("Wygląd"), tr("Kolory, kształt klawiszy i to, co na nich widać. Zmiany widać od razu w podglądzie motywu."))
    Group(null) {
        ActionRow(Icons.Rounded.Palette, tr("Motywy i kolory"), tr("18 motywów (w tym płaski czarny bez obramowań), własne kolory i gradient, obraz w tle, motyw dzienny i nocny – z podglądem na żywo")) { vm.sheet = Sheet.THEME }
        CompactChoice(
            Icons.Rounded.Tune,
            tr("Kształt klawiszy"),
            KeyStyle.entries,
            s.keyStyle,
            {
                when (it) {
                    KeyStyle.FILLED -> tr("Wypełnione – każdy klawisz to osobne pole")
                    KeyStyle.OUTLINE -> tr("Z obwódką – cienka linia dookoła klawisza")
                    KeyStyle.FLAT -> tr("Płaskie – same znaki, bez pól i obramowań")
                    KeyStyle.RAISED -> tr("3D – klawisze z cieniem pod spodem")
                }
            },
            short = { when (it) { KeyStyle.FILLED -> tr("Pełne"); KeyStyle.OUTLINE -> tr("Obrys"); KeyStyle.FLAT -> tr("Płaskie"); KeyStyle.RAISED -> "3D" } },
        ) { v -> set { it.copy(keyStyle = v) } }
    }
    TileGrid(
        listOf(
            Tile(Icons.Rounded.FormatSize, tr("Podgląd znaku"), tr("Powiększony znak nad naciśniętym klawiszem"), s.keyPreview) { v -> set { it.copy(keyPreview = v) } },
            Tile(Icons.Rounded.Abc, tr("Znaki w rogu"), tr("Małe cyfry, symbole i polskie znaki w rogu klawiszy (dostępne po przytrzymaniu)"), s.keyHints) { v -> set { it.copy(keyHints = v) } },
            Tile(Icons.Rounded.Palette, tr("Litery w akcencie"), tr("Znaki na klawiszach liter w kolorze akcentu (dowolny kolor ustawisz w motywach)"), s.accentLabels) { v -> set { it.copy(accentLabels = v) } },
            Tile(Icons.Rounded.FormatSize, tr("Pogrubione"), tr("Pogrubione litery na klawiszach"), s.boldLabels) { v -> set { it.copy(boldLabels = v) } },
        ),
    )

    SectionHeader(Icons.Rounded.Keyboard, tr("Układ"), tr("Jakie klawisze są na klawiaturze, jej rozmiar i położenie na ekranie."))
    Group(null) {
        CompactChoice(
            Icons.Rounded.VerticalSplit,
            tr("Klawiatura dzielona"),
            SplitMode.entries,
            s.split,
            { when (it) { SplitMode.OFF -> tr("Nigdy"); SplitMode.LANDSCAPE -> tr("W poziomie"); SplitMode.ALWAYS -> tr("Zawsze") } },
            short = { when (it) { SplitMode.OFF -> tr("Wył."); SplitMode.LANDSCAPE -> tr("Poziom"); SplitMode.ALWAYS -> tr("Zawsze") } },
        ) { v -> set { it.copy(split = v) } }
        SliderRow(Icons.Rounded.VerticalSplit, tr("Przerwa w środku"), s.splitGap, 5..50, 1, { "${it / 10f}" }, enabled = s.split != SplitMode.OFF, requires = needs(tr("Klawiatura dzielona"))) { v -> set { it.copy(splitGap = v) } }
    }
    TileGrid(
        listOf(
            Tile(Icons.Rounded.Dialpad, tr("Rząd cyfr"), tr("Stały rząd cyfr nad literami"), s.numberRow) { v -> set { it.copy(numberRow = v) } },
            Tile(Icons.Rounded.OpenWith, tr("Rząd strzałek"), tr("Strzałki ← ↑ ↓ → pod klawiszami do precyzyjnego przesuwania kursora"), s.arrowRow) { v -> set { it.copy(arrowRow = v) } },
            Tile(Icons.Rounded.Abc, tr("Przecinek"), tr("Klawisz przecinka obok spacji"), s.commaKey) { v -> set { it.copy(commaKey = v) } },
            Tile(Icons.Rounded.Functions, tr("Symbole na literach"), tr("Jak w SwiftKey: przytrzymaj Q – %, A – @, S – #, K – ( … Symbol widać w rogu klawisza"), s.symbolKeys) { v -> set { it.copy(symbolKeys = v) } },
            Tile(Icons.Rounded.PictureInPicture, tr("Pływająca"), tr("Klawiatura jako przesuwana nakładka nad aplikacją"), s.floating) { v -> set { it.copy(floating = v) } },
        ),
    )
    Group(null) {
        CompactChoice(
            Icons.Rounded.SwapHoriz,
            tr("Tryb jednej ręki"),
            OneHanded.entries,
            s.oneHanded,
            { when (it) { OneHanded.OFF -> tr("Wyłączony"); OneHanded.LEFT -> tr("Do lewej"); OneHanded.RIGHT -> tr("Do prawej") } },
            short = { when (it) { OneHanded.OFF -> tr("Wył."); OneHanded.LEFT -> "L"; OneHanded.RIGHT -> "P" } },
        ) { v -> set { it.copy(oneHanded = v) } }
        CompactChoice(
            Icons.Rounded.EmojiEmotions,
            tr("Klawisz emoji"),
            EmojiKey.entries,
            s.emojiKeyMode,
            {
                when (it) {
                    EmojiKey.OFF -> tr("Brak – emoji z paska narzędzi albo przytrzymania Enter")
                    EmojiKey.SEPARATE -> tr("Osobny – między ?123 a przecinkiem")
                    EmojiKey.COMBINED -> tr("Połączony z ?123 – dotknij: symbole, przytrzymaj: emoji")
                }
            },
            short = { when (it) { EmojiKey.OFF -> tr("Brak"); EmojiKey.SEPARATE -> tr("Osobny"); EmojiKey.COMBINED -> "?123" } },
        ) { v -> set { it.copy(emojiKeyMode = v) } }
        if (s.symbolKeys) {
            CompactChoice(
                Icons.Rounded.Dialpad,
                tr("Cyfry przy symbolach"),
                listOf(false, true),
                s.numberRow,
                { if (it) tr("W osobnym rzędzie nad literami") else tr("Jako drugi znak – w lewym rogu i pod przytrzymaniem") },
                short = { if (it) tr("Rząd") else tr("Drugi znak") },
            ) { v -> set { it.copy(numberRow = v) } }
        }
        CompactChoice(
            Icons.Rounded.KeyboardHide,
            tr("Miejsce pod klawiaturą"),
            NavSpace.entries,
            s.navSpace,
            {
                when (it) {
                    NavSpace.AUTO -> tr("Automatycznie – klawiatura przylega do paska systemu")
                    NavSpace.SYSTEM -> tr("Pełny pasek nawigacji (jeśli przyciski systemu nachodzą na klawisze)")
                    NavSpace.NONE -> tr("Brak – klawisze do samej krawędzi ekranu")
                }
            },
            short = { when (it) { NavSpace.AUTO -> "Auto"; NavSpace.SYSTEM -> tr("Pełne"); NavSpace.NONE -> tr("Brak") } },
        ) { v -> set { it.copy(navSpace = v) } }
        SliderRow(Icons.Rounded.Height, tr("Wysokość klawiszy"), s.keyHeight, 70..140, 5, { "$it%" }) { v -> set { it.copy(keyHeight = v) } }
        SliderRow(Icons.Rounded.SwapHoriz, tr("Szerokość w trybie jednej ręki"), s.oneHandedWidth, 60..95, 1, { "$it%" }, enabled = s.oneHanded != OneHanded.OFF, requires = needs(tr("Tryb jednej ręki"))) { v -> set { it.copy(oneHandedWidth = v) } }
        SliderRow(Icons.Rounded.PictureInPicture, tr("Szerokość klawiatury pływającej"), s.floatingScale, 55..100, 1, { "$it%" }, enabled = s.floating, requires = needs(tr("Pływająca"))) { v -> set { it.copy(floatingScale = v) } }
        SliderRow(Icons.Rounded.KeyboardHide, tr("Odstęp od dołu ekranu"), s.bottomPadding, 0..40, 1, { "$it dp" }) { v -> set { it.copy(bottomPadding = v) } }
        SliderRow(Icons.Rounded.Timer, tr("Czas przytrzymania"), s.longPressMs, 100..1000, 10, { "$it ms" }) { v -> set { it.copy(longPressMs = v) } }
    }

    SectionHeader(Icons.Rounded.TouchApp, tr("Dotyk i dźwięk"), tr("Wibracja, dźwięk klawiszy oraz gesty palcem – i co mają robić."))
    TileGrid(
        listOf(
            Tile(Icons.Rounded.Vibration, tr("Wibracja"), tr("Delikatna wibracja przy każdym klawiszu"), s.haptics) { v -> set { it.copy(haptics = v) } },
            Tile(Icons.Rounded.VolumeUp, tr("Dźwięk"), tr("Dźwięk przy naciśnięciu klawisza – rodzaj i głośność wybierzesz niżej"), s.sound) { v -> set { it.copy(sound = v) } },
            Tile(Icons.AutoMirrored.Rounded.Backspace, tr("Usuwanie gestem"), tr("Przesuń palec w lewo z Backspace, aby zaznaczyć i usunąć całe słowa"), s.deleteSwipe) { v -> set { it.copy(deleteSwipe = v) } },
            Tile(Icons.AutoMirrored.Rounded.Backspace, tr("Backspace usuwa słowa"), tr("Przytrzymany Backspace kasuje całe słowa zamiast pojedynczych liter"), s.deleteWordsOnHold) { v -> set { it.copy(deleteWordsOnHold = v) } },

        ),
    )
    Group(null) {
        ActionRow(Icons.Rounded.Gesture, tr("Gesty"), tr("Spacja w bok i w górę, przytrzymania, przeciągnięcie w dół – przypisz własne czynności")) { vm.sheet = Sheet.GESTURES }
        SliderRow(Icons.Rounded.Vibration, tr("Siła wibracji"), s.hapticStrength, 1..100, 1, { "$it%" }, enabled = s.haptics, requires = needs(tr("Wibracja"))) { v -> set { it.copy(hapticStrength = v) } }
        SliderRow(Icons.Rounded.VolumeUp, tr("Głośność klawiszy"), s.soundVolume, 1..100, 1, { "$it%" }, enabled = s.sound, requires = needs(tr("Dźwięk"))) { v -> set { it.copy(soundVolume = v) } }
        CompactChoice(
            Icons.Rounded.VolumeUp,
            tr("Rodzaj dźwięku"),
            KeySound.entries,
            s.keySound,
            {
                when (it) {
                    KeySound.SYSTEM -> tr("Systemowy – dźwięk klawiatury telefonu")
                    KeySound.CLICK -> tr("Klik – krótki i suchy")
                    KeySound.SOFT -> tr("Miękki – ciche „puk”")
                    KeySound.TYPEWRITER -> tr("Maszyna do pisania")
                    KeySound.BUBBLE -> tr("Bańka – wesoły, rosnący ton")
                }
            },
            short = { when (it) { KeySound.SYSTEM -> "Sys"; KeySound.CLICK -> "Klik"; KeySound.SOFT -> tr("Miękki"); KeySound.TYPEWRITER -> "⌨"; KeySound.BUBBLE -> "○" } },
            enabled = s.sound,
            requires = needs(tr("Dźwięk")),
        ) { v -> set { it.copy(keySound = v) } }
    }

    SectionHeader(Icons.Rounded.Widgets, tr("Narzędzia"), tr("Pasek narzędzi nad klawiszami, schowek i prywatność."))
    Group(null) {
        ActionRow(Icons.Rounded.Tune, tr("Pasek narzędzi"), tr("Kolejność i widoczność przycisków nad klawiaturą")) { vm.sheet = Sheet.TOOLBAR }
    }
    TileGrid(
        listOf(
            Tile(Icons.Rounded.ContentPaste, tr("Historia schowka"), tr("Zapamiętywanie skopiowanych tekstów (dane poufne są pomijane)"), s.clipboardHistory) { v -> set { it.copy(clipboardHistory = v) } },
            Tile(Icons.Rounded.VisibilityOff, tr("Auto-incognito"), tr("Bez nauki i historii w polach haseł i w trybie prywatnym przeglądarek"), s.incognitoAuto, enabled = !s.forceIncognito, requires = tr("„Zawsze incognito” obejmuje już wszystkie pola")) { v -> set { it.copy(incognitoAuto = v) } },
            Tile(Icons.Rounded.VisibilityOff, tr("Zawsze incognito"), tr("Klawiatura niczego nie zapamiętuje"), s.forceIncognito) { v -> set { it.copy(forceIncognito = v) } },
        ),
    )
    VoiceGroup(vm)
    Group(null) {
        SliderRow(Icons.Rounded.ContentPaste, tr("Pojemność historii schowka"), s.clipboardSize, 10..200, 10, enabled = s.clipboardHistory, requires = needs(tr("Historia schowka"))) { v -> set { it.copy(clipboardSize = v) } }
        ActionRow(Icons.Rounded.DeleteSweep, tr("Wyczyść historię schowka"), tr("Przypięte wpisy zostaną"), danger = true) {
            confirm = tr("Wyczyścić historię schowka?") to { ClipboardHistory.clear() }
        }
    }

    SectionHeader(Icons.Rounded.Backup, tr("Kopia zapasowa"), tr("Przenieś ustawienia i wyuczone słowa na inny telefon albo zacznij od nowa."))
    Group(null) {
        ActionRow(Icons.Rounded.Backup, tr("Eksportuj"), tr("Ustawienia, wyuczone słowa, skróty i przypięte wpisy schowka do pliku JSON")) { sys.exportBackup() }
        ActionRow(Icons.Rounded.Restore, tr("Importuj"), tr("Przywróć z pliku kopii")) { sys.importBackup() }
        ActionRow(Icons.Rounded.BarChart, tr("Statystyki i mapa cieplna"), tr("Tempo pisania, najczęstsze słowa i które klawisze naciskasz najczęściej")) { vm.sheet = Sheet.STATS }
        ActionRow(Icons.Rounded.BarChart, tr("Zeruj statystyki"), null) {
            confirm = tr("Wyzerować statystyki pisania?") to { UserModel.resetStats() }
        }
        ActionRow(Icons.AutoMirrored.Rounded.MenuBook, tr("Zapomnij wyuczone słowa"), tr("Słowa i pary słów zapamiętane podczas pisania"), danger = true) {
            confirm = tr("Usunąć wszystkie wyuczone słowa?") to { UserModel.clearLearned() }
        }
        ActionRow(Icons.Rounded.Restore, tr("Przywróć ustawienia domyślne"), null, danger = true) {
            confirm = tr("Przywrócić domyślne ustawienia klawiatury?") to { Prefs.reset() }
        }
    }

    SectionHeader(Icons.Rounded.Settings, tr("Ogólne"), tr("Język aplikacji, aktualizacje z GitHuba i informacje o aplikacji."))
    Group(null) {
        CompactChoice(
            Icons.Rounded.Language,
            tr("Język aplikacji"),
            AppLanguage.entries,
            s.appLanguage,
            { when (it) { AppLanguage.SYSTEM -> tr("Systemowy"); AppLanguage.PL -> "Polski"; AppLanguage.EN -> "English" } },
            short = { when (it) { AppLanguage.SYSTEM -> "Auto"; AppLanguage.PL -> "PL"; AppLanguage.EN -> "EN" } },
        ) { v -> set { it.copy(appLanguage = v) } }
    }
    TileGrid(
        listOf(
            Tile(Icons.Rounded.SystemUpdate, tr("Aktualizacje"), tr("Sprawdzaj nowe wersje na GitHubie przy uruchomieniu"), s.autoUpdate) { v -> set { it.copy(autoUpdate = v) } },
            Tile(Icons.Rounded.Notifications, tr("Powiadomienia"), tr("Zezwól na powiadomienie po aktualizacji"), false) { sys.requestNotifications() },
        ),
    )
    Group(null) {
        UpdateRow(vm)
        ActionRow(Icons.AutoMirrored.Rounded.KeyboardReturn, tr("O aplikacji i źródłach danych"), tr("Licencje słowników i emoji")) { vm.sheet = Sheet.ABOUT }
    }
    About()

    val c = confirm
    if (c != null) {
        DarkDialog(
            title = c.first,
            onDismiss = { confirm = null },
            buttons = {
                PillButton(tr("Anuluj"), primary = false) { confirm = null }
                PillButton(tr("Tak"), danger = true) {
                    c.second()
                    confirm = null
                    vm.toast(tr("Gotowe"))
                }
            },
        ) {
            Text(tr("Tej operacji nie można cofnąć."), color = Palette.TextDim, fontSize = 13.sp)
        }
    }
}

@Composable
private fun About() {
    val sys = LocalSystem.current
    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(tr("OpenKeyboard – otwarta klawiatura dla Androida"), fontSize = 13.sp, color = Color.White)
        Text(tr("Licencja MIT · bez reklam i śledzenia"), fontSize = 11.sp, color = Palette.TextDim)
        Text(
            tr("Pomysł i wykonanie: Adam Rędzikowski · redzikowski.com"),
            fontSize = 12.sp,
            color = Palette.TextDim,
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { sys.openUrl("https://redzikowski.com") }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Text(
            "☕  " + tr("Postaw kawę"),
            fontSize = 13.sp,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable { sys.openUrl("https://buycoffee.to/adamredzikowski") }
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}
