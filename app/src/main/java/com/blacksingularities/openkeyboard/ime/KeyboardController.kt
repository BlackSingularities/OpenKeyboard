package com.blacksingularities.openkeyboard.ime

import android.content.Intent
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.blacksingularities.openkeyboard.MainActivity
import com.blacksingularities.openkeyboard.data.Autocorrect
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Languages
import com.blacksingularities.openkeyboard.data.OneHanded
import com.blacksingularities.openkeyboard.data.Prefs
import com.blacksingularities.openkeyboard.dict.Dictionaries
import com.blacksingularities.openkeyboard.dict.Proximity
import com.blacksingularities.openkeyboard.dict.Suggest
import com.blacksingularities.openkeyboard.dict.Suggestion
import com.blacksingularities.openkeyboard.dict.SuggestionKind
import com.blacksingularities.openkeyboard.dict.Suggestions
import com.blacksingularities.openkeyboard.dict.UserModel
import com.blacksingularities.openkeyboard.dict.normalizeKey
import com.blacksingularities.openkeyboard.emoji.EmojiData
import com.blacksingularities.openkeyboard.features.Calculator
import com.blacksingularities.openkeyboard.features.ClipboardHistory
import com.blacksingularities.openkeyboard.features.FancyStyle
import com.blacksingularities.openkeyboard.features.TextTools
import com.blacksingularities.openkeyboard.features.SmartCase
import com.blacksingularities.openkeyboard.features.UnitConverter
import com.blacksingularities.openkeyboard.data.GestureAction
import com.blacksingularities.openkeyboard.dict.Glide
import com.blacksingularities.openkeyboard.dict.Pt
import com.blacksingularities.openkeyboard.i18n.tr
import com.blacksingularities.openkeyboard.keyboard.FieldKind
import com.blacksingularities.openkeyboard.keyboard.Key
import com.blacksingularities.openkeyboard.keyboard.KeyType
import com.blacksingularities.openkeyboard.keyboard.KeyboardLayout
import com.blacksingularities.openkeyboard.keyboard.KeyboardMode
import com.blacksingularities.openkeyboard.keyboard.Layouts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.BreakIterator
import kotlin.math.hypot

enum class ShiftState { OFF, ONESHOT, LOCKED }

/** Co pokazuje klawiatura pod paskiem podpowiedzi. */
enum class Panel { KEYS, EMOJI, CLIPBOARD, EDIT, TEXT, VOICE, LANGUAGES, QUICK, RESIZE, SYMBOLS }

/** Pisanie do pola w samej klawiaturze (wyszukiwanie emoji) zamiast do aplikacji. */
enum class InternalTarget { EMOJI_SEARCH }

/**
 * Stan i logika klawiatury: tryby (litery / symbole / cyfry), Shift, wpisywane słowo
 * (tekst „composing”), autokorekta, podpowiedzi, interpunkcja, kursor, panele.
 */
class KeyboardController(val service: OpenKeyboardService) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val feedback = Feedback(service)

    var settings by mutableStateOf(Prefs.current)
        private set
    var mode by mutableStateOf(KeyboardMode.ALPHA)
        private set
    var shift by mutableStateOf(ShiftState.OFF)
        private set
    var fieldKind by mutableStateOf(FieldKind.TEXT)
        private set
    var layout by mutableStateOf(buildLayout())
        private set
    var suggestions by mutableStateOf(Suggestions.Empty)
        private set
    /** Dodatkowe podpowiedzi po prawej (emoji, wynik rachunku, schowek, skrót). */
    var extras by mutableStateOf<List<Suggestion>>(emptyList())
        private set
    var panel by mutableStateOf(Panel.KEYS)
    var toolbarOpen by mutableStateOf(false)
    var enterAction by mutableStateOf(EditorInfo.IME_ACTION_UNSPECIFIED)
        private set
    var multiLine by mutableStateOf(false)
        private set
    var incognito by mutableStateOf(false)
        private set
    var internal by mutableStateOf<InternalTarget?>(null)
        private set
    var internalText by mutableStateOf("")
    var toast by mutableStateOf<String?>(null)
    var floatingOffset by mutableStateOf(Offset.Zero)
    var selecting by mutableStateOf(false)
    var voice = VoiceInput(this)
    /** Ozdobny styl wpisywanych liter (𝐁𝐨𝐥𝐝, 𝓢𝓬𝓻𝓲𝓹𝓽…) – null = zwykły tekst. */
    var fancy by mutableStateOf<FancyStyle?>(null)
    /** Język wykryty w ostatnio pisanych słowach (pierwszeństwo w podpowiedziach). */
    var detectedLanguage by mutableStateOf<String?>(null)
        private set
    private val recentWords = ArrayDeque<String>()
    /** Słowo wpisane gestem czeka w polu – następny gest albo litera doda przed sobą spację. */
    private var glidePending = false
    private var lastTypeTime = 0L

    private var editorInfo: EditorInfo? = null
    private val composing = StringBuilder()
    private var suggestJob: Job? = null
    private var suggestionsFor = ""
    private var lastSpaceTime = 0L
    private var autoSpace = false
    private var lastShiftTap = 0L
    private var lastSelfEdit = 0L
    /** Ostatnia autokorekta: (wpisane, poprawione, separator) – Backspace ją cofa. */
    private var lastCorrection: Triple<String, String, String>? = null
    /** Słowo przywrócone Backspace'em po autokorekcie – nie poprawiaj go ponownie. */
    private var keepTyped = false
    private var deleteRepeats = 0
    private var proximity: Proximity = buildProximity(layout)

    val ic: InputConnection? get() = service.currentInputConnection

    val activeLanguage: String get() = settings.activeLanguage
    val composingText: String get() = composing.toString()


    fun destroy() {
        voice.destroy()
        scope.cancel()
    }

    // ───────────────────────── cykl życia pola ─────────────────────────

    fun onStartInput(info: EditorInfo, restarting: Boolean) {
        editorInfo = info
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        fieldKind = when (cls) {
            InputType.TYPE_CLASS_NUMBER -> FieldKind.NUMBER
            InputType.TYPE_CLASS_PHONE -> FieldKind.PHONE
            InputType.TYPE_CLASS_DATETIME -> FieldKind.NUMBER
            InputType.TYPE_CLASS_TEXT -> when (variation) {
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> FieldKind.EMAIL
                InputType.TYPE_TEXT_VARIATION_URI -> FieldKind.URL
                InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> FieldKind.PASSWORD
                else -> if ((info.imeOptions and EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_SEARCH) FieldKind.SEARCH else FieldKind.TEXT
            }
            else -> FieldKind.TEXT
        }
        val noLearning = (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
        incognito = settings.forceIncognito || fieldKind == FieldKind.PASSWORD || (settings.incognitoAuto && noLearning)
        ClipboardHistory.paused = incognito
        enterAction = if ((info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) EditorInfo.IME_ACTION_NONE else info.imeOptions and EditorInfo.IME_MASK_ACTION
        multiLine = cls == InputType.TYPE_CLASS_TEXT && (info.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        if (!restarting) {
            mode = when (fieldKind) {
                FieldKind.NUMBER -> KeyboardMode.NUMBER
                FieldKind.PHONE -> KeyboardMode.PHONE
                else -> KeyboardMode.ALPHA
            }
            panel = Panel.KEYS
            toolbarOpen = false
            internal = null
            shift = ShiftState.OFF
        }
        composing.clear()
        lastCorrection = null
        keepTyped = false
        autoSpace = false
        rebuild()
        updateAutoShift()
        refreshSuggestions()
    }

    fun onFinishInput() {
        commitComposing(autocorrect = false)
        voice.stop()
        internal = null
        UserModel.saveNow()
    }

    private val suggestionsEnabledField: Boolean
        get() {
            val info = editorInfo ?: return false
            if (info.inputType == InputType.TYPE_NULL) return false
            if (fieldKind != FieldKind.TEXT && fieldKind != FieldKind.SEARCH) return false
            return (info.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0
        }

    /** Podpowiedzi słów w tym polu (hasła, adresy, liczby – bez). */
    val wordSuggestions: Boolean get() = settings.suggestions && suggestionsEnabledField && fieldKind != FieldKind.PASSWORD

    // ───────────────────────── układ ─────────────────────────

    private fun buildLayout(): KeyboardLayout {
        val s = Prefs.current
        return Layouts.build(mode, Languages.get(s.activeLanguage), s, fieldKind, s.languages.size)
    }

    private fun rebuild() {
        layout = buildLayout()
        proximity = buildProximity(layout)
    }

    private fun buildProximity(l: KeyboardLayout): Proximity {
        val centers = HashMap<Char, MutableList<Pair<Float, Float>>>()
        l.rows.forEachIndexed { r, row ->
            var x = (l.units - row.sumOf { it.width.toDouble() }.toFloat()) / 2f
            for (k in row) {
                if (k.type == KeyType.CHAR && k.text.length == 1) {
                    val c = normalizeKey(k.text).firstOrNull()
                    if (c != null) centers.getOrPut(c) { mutableListOf() } += (x + k.width / 2f) to r.toFloat()
                }
                x += k.width
            }
        }
        val near = HashMap<Char, MutableSet<Char>>()
        val list = centers.entries.toList()
        for ((a, pa) in list) for ((b, pb) in list) {
            if (a == b) continue
            if (pa.any { p -> pb.any { q -> hypot(p.first - q.first, (p.second - q.second) * 1.1f) <= 1.3f } }) near.getOrPut(a) { HashSet() } += b
        }
        return Proximity { x, y -> near[x]?.contains(y) == true }
    }

    fun switchMode(m: KeyboardMode) {
        mode = m
        rebuild()
    }

    // ───────────────────────── Shift ─────────────────────────

    fun onShift() {
        val now = SystemClock.uptimeMillis()
        // Shift w trakcie słowa: abc → Abc → ABC → abc (zamiast przełączać następną literę).
        if (settings.shiftCycleWord && internal == null && mode == KeyboardMode.ALPHA && composing.isNotEmpty() && fancy == null) {
            cycleComposingCase()
            lastShiftTap = 0L
            return
        }
        // Słowo tuż przed kursorem (pole bez podpowiedzi, słowo po geście) – też zmienia wielkość.
        if (settings.shiftCycleWord && internal == null && mode == KeyboardMode.ALPHA && composing.isEmpty() && fancy == null) {
            val ic = ic
            val before = ic?.getTextBeforeCursor(48, 0)?.toString().orEmpty()
            val word = Regex("""[\p{L}'’-]+$""").find(before)?.value
            if (ic != null && word != null && word.any { it.isLetter() } && ic.getSelectedText(0).isNullOrEmpty()) {
                val next = TextTools.cycleWordCase(word)
                ic.beginBatchEdit()
                ic.deleteSurroundingText(word.length, 0)
                ic.commitText(next, 1)
                ic.endBatchEdit()
                capsWord = next.length > 1 && next == next.uppercase()
                shift = ShiftState.OFF
                lastShiftTap = 0L
                feedback.tick()
                selfEdit()
                refreshSuggestions()
                return
            }
        }
        // Shift przy zaznaczeniu: zmienia wielkość liter zaznaczonego tekstu.
        if (settings.shiftCycleWord && internal == null && composing.isEmpty()) {
            val sel = ic?.getSelectedText(0)?.toString()
            if (!sel.isNullOrEmpty() && sel.any { it.isLetter() }) {
                transformText(TextTools::cycleCase)
                lastShiftTap = 0L
                return
            }
        }
        capsWord = false
        shift = when {
            shift == ShiftState.LOCKED -> ShiftState.OFF
            now - lastShiftTap < 320 -> ShiftState.LOCKED
            shift == ShiftState.ONESHOT -> ShiftState.OFF
            else -> ShiftState.ONESHOT
        }
        lastShiftTap = now
    }

    fun lockShift() {
        capsWord = false
        shift = ShiftState.LOCKED
    }

    /** Wielkie litery do końca bieżącego słowa (po drugim Shift w trakcie pisania). */
    var capsWord by mutableStateOf(false)
        private set

    /** Shift przytrzymany jednym palcem, gdy drugi pisze – wielkie litery tylko do puszczenia. */
    var shiftChord by mutableStateOf(false)
        private set

    fun startShiftChord() {
        shiftChord = true
    }

    fun endShiftChord() {
        shiftChord = false
    }

    /** Kolejny Shift w trakcie słowa: abc → Abc → ABC → abc. */
    private fun cycleComposingCase() {
        val ic = ic ?: return
        val word = composing.toString()
        val next = TextTools.cycleWordCase(word)
        composing.clear()
        composing.append(next)
        ic.setComposingText(composing, 1)
        keepTyped = next != next.lowercase()
        capsWord = next.length > 1 && next == next.uppercase() && next != next.lowercase()
        shift = ShiftState.OFF
        feedback.tick()
        selfEdit()
        refreshSuggestions()
    }

    private fun updateAutoShift() {
        if (shift == ShiftState.LOCKED) return
        if (mode != KeyboardMode.ALPHA || !settings.autoCapitalize || internal != null) {
            if (shift == ShiftState.ONESHOT && internal != null) shift = ShiftState.OFF
            return
        }
        val info = editorInfo ?: return
        val caps = if (composing.isEmpty()) ic?.getCursorCapsMode(info.inputType) ?: 0 else 0
        shift = if (caps != 0) ShiftState.ONESHOT else ShiftState.OFF
    }

    val upper: Boolean get() = shift != ShiftState.OFF || capsWord || shiftChord

    // ───────────────────────── klawisze ─────────────────────────

    fun onKey(key: Key) {
        when (key.type) {
            KeyType.CHAR -> onText(if (upper) key.text.uppercase() else key.text)
            KeyType.TEXT -> onText(key.text)
            KeyType.SPACE -> onSpace()
            KeyType.DELETE -> onDelete()
            KeyType.ENTER -> onEnter()
            KeyType.SHIFT -> onShift()
            KeyType.SYMBOLS -> switchMode(KeyboardMode.SYMBOLS)
            KeyType.SYMBOLS2 -> switchMode(KeyboardMode.SYMBOLS2)
            KeyType.ALPHA -> {
                switchMode(KeyboardMode.ALPHA)
                updateAutoShift()
            }
            KeyType.LANGUAGE -> nextLanguage()
            KeyType.EMOJI -> openPanel(Panel.EMOJI)
            KeyType.NUMPAD -> switchMode(KeyboardMode.NUMBER)
        }
        if (key.type != KeyType.DELETE) deleteRepeats = 0
    }

    /** Tekst z klawisza lub z okienka przytrzymania. */
    fun onText(raw: String) {
        if (internal != null) {
            internalText += raw
            afterInternalChange()
            if (shift == ShiftState.ONESHOT) shift = ShiftState.OFF
            return
        }
        val ic = ic ?: return
        var text = raw
        val ch0 = text.firstOrNull() ?: return
        if (glidePending && composing.isNotEmpty() && ch0.isLetter()) {
            commitComposing(autocorrect = false, separator = " ")
        }
        glidePending = false
        // Ozdobny styl: litery i cyfry zamieniane od razu, bez podpowiedzi.
        val style = fancy
        if (style != null && text.length == 1 && (ch0.isLetterOrDigit() || ch0 == ' ')) {
            if (composing.isNotEmpty()) commitComposing(autocorrect = false)
            ic.commitText(TextTools.style(text, style), 1)
            if (shift == ShiftState.ONESHOT) shift = ShiftState.OFF
            selfEdit()
            return
        }
        // Typograficzne cudzysłowy.
        // Także zaraz po wpisywanym słowie – wtedy to cudzysłów zamykający („słowo” zamiast „słowo").
        if (settings.smartQuotes && (text == "\"" || text == "'")) {
            val prevCh = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
            val opening = prevCh.isEmpty() || prevCh[0].isWhitespace() || prevCh[0] in "([{„“‘"
            text = if (text == "\"") {
                val low = activeLanguage in LOW_QUOTE_LANGS
                when {
                    opening -> if (low) "„" else "“"
                    // Niemiecki, czeski, słowacki, litewski zamykają „…“; polski, węgierski, rumuński – „…”.
                    activeLanguage in setOf("de", "cs", "sk", "lt") -> "“"
                    else -> "”"
                }
            } else {
                if (opening) "‘" else "’"
            }
        }
        val ch = text.first()
        // Spacja po przecinku, wykrzykniku itp., gdy od razu pisze się dalej.
        if (settings.autoSpaceAfterPunct && ch.isLetter() && composing.isEmpty() && fieldKind == FieldKind.TEXT) {
            val before2 = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            if (before2.length == 2 && before2[1] in ",;!?" && before2[0].isLetter()) ic.commitText(" ", 1)
        }
        val wordChar = text.length == 1 && (ch.isLetter() || (composing.isNotEmpty() && (ch == '\'' || ch == '’' || ch == '-')))
        // Pisanie chowa ręcznie otwarty pasek narzędzi – wracają podpowiedzi.
        if (wordChar) toolbarOpen = false
        if (wordChar && wordSuggestions) {
            if (composing.isEmpty()) {
                keepTyped = false
                lastCorrection = null
            }
            composing.append(text)
            ic.setComposingText(composing, 1)
            selfEdit()
        } else {
            val punct = text.length == 1 && ch in ".,!?:;)"
            if (composing.isNotEmpty()) commitComposing(autocorrect = punct || ch == '"' || ch == '”' || ch == '“' || ch == '…', separator = "")
            val before2 = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            // Inteligentny przecinek: „słowo ,” → „słowo, ” – znak wraca na koniec słowa.
            val afterWordSpace = before2.length == 2 && before2[1] == ' ' && (before2[0].isLetterOrDigit() || before2[0] in ")\"”’")
            if (punct && settings.smartPunctuation && fieldKind == FieldKind.TEXT && (autoSpace || afterWordSpace)) {
                if (before2.endsWith(" ")) ic.deleteSurroundingText(1, 0)
                ic.commitText(text + if (ch in ".,!?:;") " " else "", 1)
                autoSpace = ch in ".,!?:;"
            } else if (settings.smartPunctuation && fieldKind == FieldKind.TEXT && text.length == 1 && ch in ".," &&
                before2.lastOrNull()?.isLetter() == true
            ) {
                // Przecinek i kropka po słowie same dodają spację (po cyfrze nie – „3,5”, „1.2”).
                ic.commitText("$text ", 1)
                autoSpace = true
            } else if (settings.autoPair && text.length == 1 && ch in PAIRS && fieldKind == FieldKind.TEXT) {
                ic.commitText(text + (if (ch == '„' && activeLanguage in setOf("de", "cs", "sk", "lt")) '“' else PAIRS.getValue(ch)), 1)
                selfEdit()
                moveCursor(-1)
                autoSpace = false
            } else if (settings.autoPair && text.length == 1 && ch in PAIRS.values && ic.getTextAfterCursor(1, 0)?.toString() == text) {
                // Zamykający znak już stoi za kursorem – tylko go przeskocz.
                moveCursor(1)
                autoSpace = false
            } else {
                ic.commitText(text, 1)
                autoSpace = false
            }
            lastCorrection = null
            selfEdit()
        }
        if (!incognito) {
            val now = SystemClock.uptimeMillis()
            val dt = now - lastTypeTime
            lastTypeTime = now
            val key = text.lowercase()
            UserModel.bumpStats {
                it.copy(
                    chars = it.chars + text.length,
                    typingMs = it.typingMs + if (dt in 1..4000) dt else 0,
                    keyTaps = if (text.length == 1 && text[0].isLetter()) it.keyTaps + (key to ((it.keyTaps[key] ?: 0L) + 1)) else it.keyTaps,
                )
            }
        }
        if (shift == ShiftState.ONESHOT) shift = ShiftState.OFF
        if (!wordChar) {
            capsWord = false
            updateAutoShift()
        }
        // Po symbolu typu apostrof w trybie symboli – powrót do liter jak w Gboard.
        if (mode == KeyboardMode.SYMBOLS && ch == '\'' ) switchMode(KeyboardMode.ALPHA)
        refreshSuggestions()
    }

    fun onSpace() {
        if (internal != null) {
            internalText += " "
            afterInternalChange()
            return
        }
        val ic = ic ?: return
        val now = SystemClock.uptimeMillis()
        if (composing.isNotEmpty()) {
            commitComposing(autocorrect = true, separator = " ")
            autoSpace = false
        } else if (settings.doubleSpacePeriod && now - lastSpaceTime < 600 && fieldKind == FieldKind.TEXT) {
            val before = ic.getTextBeforeCursor(3, 0)?.toString().orEmpty()
            if (before.length >= 2 && before.endsWith(" ") && before[before.length - 2].isLetterOrDigit()) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                lastSpaceTime = 0
                selfEdit()
                updateAutoShift()
                refreshSuggestions()
                return
            } else {
                ic.commitText(" ", 1)
            }
        } else if (autoSpace && ic.getTextBeforeCursor(1, 0)?.toString() == " ") {
            // Spacja już dodana po przecinku/kropce lub podpowiedzi – nie dubluj.
            autoSpace = false
        } else {
            ic.commitText(" ", 1)
            autoSpace = false
        }
        lastSpaceTime = now
        selfEdit()
        if (mode == KeyboardMode.SYMBOLS || mode == KeyboardMode.SYMBOLS2) switchMode(KeyboardMode.ALPHA)
        updateAutoShift()
        refreshSuggestions()
    }

    fun onEnter() {
        if (internal != null) {
            closeInternal(Panel.EMOJI)
            return
        }
        val ic = ic ?: return
        if (composing.isNotEmpty()) commitComposing(autocorrect = true, separator = "")
        val action = enterAction
        val hasAction = action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED
        if (hasAction && !multiLine) {
            ic.performEditorAction(action)
        } else if (editorInfo?.inputType == InputType.TYPE_NULL) {
            sendKey(KeyEvent.KEYCODE_ENTER)
        } else {
            ic.commitText("\n", 1)
        }
        autoSpace = false
        selfEdit()
        updateAutoShift()
        refreshSuggestions()
    }

    /** Backspace; [repeat] – kolejne powtórzenie przy przytrzymaniu (po kilkunastu – całe słowa). */
    fun onDelete(repeat: Boolean = false) {
        if (internal != null) {
            if (internalText.isNotEmpty()) internalText = internalText.dropLastGrapheme()
            afterInternalChange()
            return
        }
        val ic = ic ?: return
        deleteRepeats = if (repeat) deleteRepeats + 1 else 0
        val correction = lastCorrection
        if (!repeat && correction != null && composing.isEmpty()) {
            // Cofnięcie autokorekty: „poprawione ” → „wpisane” do dalszej edycji.
            val (typed, corrected, sep) = correction
            val before = ic.getTextBeforeCursor(corrected.length + sep.length, 0)?.toString()
            if (before == corrected + sep) {
                ic.deleteSurroundingText(corrected.length + sep.length, 0)
                composing.clear()
                composing.append(typed)
                ic.setComposingText(composing, 1)
                keepTyped = true
                if (settings.learnFromUndo && !incognito && typed.equals(corrected, ignoreCase = true).not()) {
                    UserModel.rejectCorrection(typed, corrected)
                }
                lastCorrection = null
                selfEdit()
                refreshSuggestions()
                return
            }
        }
        lastCorrection = null
        autoSpace = false
        val wordMode = settings.deleteWordsOnHold && deleteRepeats > DELETE_WORDS_AFTER
        if (composing.isNotEmpty()) {
            if (wordMode) {
                composing.clear()
                ic.commitText("", 1)
            } else {
                val next = composing.toString().dropLastGrapheme()
                composing.clear()
                composing.append(next)
                if (next.isEmpty()) ic.commitText("", 1) else ic.setComposingText(composing, 1)
            }
            selfEdit()
            refreshSuggestions()
            if (composing.isEmpty()) updateAutoShift()
            return
        }
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else if (editorInfo?.inputType == InputType.TYPE_NULL) {
            sendKey(KeyEvent.KEYCODE_DEL)
        } else {
            val before = ic.getTextBeforeCursor(64, 0)?.toString()
            if (before == null) {
                sendKey(KeyEvent.KEYCODE_DEL)
            } else if (before.isNotEmpty()) {
                val n = if (wordMode) wordLengthBefore(before) else graphemeLengthBefore(before)
                ic.deleteSurroundingText(n.coerceAtLeast(1), 0)
                // Backspace doszedł do słowa – znów jest edytowane: podpowiedzi, a dopisane litery trafiają do niego.
                if (!wordMode && wordSuggestions) resumeWordBeforeCursor(before.dropLast(n.coerceAtLeast(1)))
            } else {
                // Puste pole – niektóre aplikacje (np. czaty) reagują na samo zdarzenie klawisza.
                sendKey(KeyEvent.KEYCODE_DEL)
            }
        }
        selfEdit()
        updateAutoShift()
        refreshSuggestions()
    }

    /** Usuwa całe słowo przed kursorem (gest w lewo na Backspace, przycisk w panelu edycji). */
    fun deleteWord() {
        val ic = ic ?: return
        if (composing.isNotEmpty()) {
            composing.clear()
            ic.commitText("", 1)
        } else {
            val before = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
            if (before.isNotEmpty()) ic.deleteSurroundingText(wordLengthBefore(before), 0)
        }
        selfEdit()
        updateAutoShift()
        refreshSuggestions()
    }

    /** Zaznacza [words] słów przed kursorem (gest na Backspace) – po puszczeniu są usuwane. */
    fun selectWordsBack(words: Int) {
        val ic = ic ?: return
        commitComposing(autocorrect = false)
        val before = ic.getTextBeforeCursor(400, 0)?.toString().orEmpty()
        val et = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
        val cursor = et?.let { it.startOffset + it.selectionEnd } ?: return
        var n = 0
        var text = before
        repeat(words) {
            val l = wordLengthBefore(text)
            n += l
            text = text.dropLast(l)
        }
        ic.setSelection(cursor - n.coerceAtMost(before.length), cursor)
    }

    fun deleteSelection() {
        val ic = ic ?: return
        if (!ic.getSelectedText(0).isNullOrEmpty()) ic.commitText("", 1)
        selfEdit()
        updateAutoShift()
        refreshSuggestions()
    }

    fun cancelSelectionBack() {
        val ic = ic ?: return
        val et = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0) ?: return
        val end = et.startOffset + maxOf(et.selectionStart, et.selectionEnd)
        ic.setSelection(end, end)
    }

    // ───────────────────────── wpisywane słowo ─────────────────────────

    /** Zatwierdza wpisywane słowo (z autokorektą, gdy [autocorrect]) i dodaje [separator]. */
    fun commitComposing(autocorrect: Boolean, separator: String = "") {
        val ic = ic ?: return
        if (composing.isEmpty()) {
            if (separator.isNotEmpty()) ic.commitText(separator, 1)
            return
        }
        val typed = composing.toString()
        val prev = previousWord()
        var final = typed
        if (autocorrect && !keepTyped && settings.autocorrect != Autocorrect.OFF && wordSuggestions) {
            val s = if (suggestionsFor == typed) suggestions else computeSuggestions(typed, composingAtSentenceStart())
            val auto = s.autoCorrect
            if (auto != null && auto.text != typed) {
                final = auto.text
                lastCorrection = Triple(typed, final, separator)
                if (!incognito) UserModel.bumpStats { it.copy(corrections = it.corrections + 1) }
            } else {
                lastCorrection = null
            }
        } else {
            lastCorrection = null
        }
        // Wielkość liter: początek zdania, „HEllo”, angielskie dni i miesiące.
        if (autocorrect && settings.autoCapitalize && !capsWord && wordSuggestions && final == typed && !keepTyped) {
            val fixed = SmartCase.fix(final, composingAtSentenceStart(), suggestLangs, primaryLanguage)
            if (fixed != final) {
                final = fixed
                lastCorrection = Triple(typed, final, separator)
            }
        }
        // Skrót tekstowy rozwijany spacją (np. „pzdr ” → „Pozdrawiam ”).
        if (settings.shortcuts && separator == " ") {
            UserModel.shortcutFor(typed)?.let { final = it.expansion; lastCorrection = Triple(typed, final, separator) }
        }
        capsWord = false
        val expanded = settings.shortcuts && separator == " " && final.contains('{') && UserModel.shortcutFor(typed)?.expansion == final
        if (expanded) commitExpanded(final, separator) else ic.commitText(final + separator, 1)
        composing.clear()
        keepTyped = false
        learn(final, prev)
        selfEdit()
    }

    /** Rozwinięcie skrótu ze zmiennymi: {data}, {godzina}, {dzień}, {schowek}, {kursor}. */
    private fun commitExpanded(template: String, separator: String) {
        val ic = ic ?: return
        val clip = ClipboardHistory.items.value.firstOrNull()?.text.orEmpty()
        val (text, cursorBack) = TextTools.expandTemplate(template, activeLanguage, clip)
        if (cursorBack > 0) {
            ic.commitText(text, 1)
            selfEdit()
            moveCursor(-cursorBack)
        } else {
            ic.commitText(text + separator, 1)
        }
    }

    private fun learn(word: String, prev: String?) {
        if (incognito || !settings.learnWords) return
        if (' ' in word) {
            // Poprawka na dwa słowa – każde osobno, drugie w parze z pierwszym.
            var p = prev
            for (w in word.split(' ').filter { it.isNotEmpty() }) {
                learn(w, p)
                p = w
            }
            return
        }
        val atSentenceStart = prev == null
        val stored = if (atSentenceStart && word.length > 1 && word[0].isUpperCase() && word.drop(1).all { it.isLowerCase() }) {
            // Wielka litera tylko z początku zdania – zapamiętaj małą, chyba że słownik zna formę wielką.
            val lower = word.lowercase()
            if (settings.languages.any { Dictionaries.get(it)?.contains(lower) == true }) lower else word
        } else word
        UserModel.learn(stored, prev)
        if (settings.autoLanguage) detectLanguage(word)
        UserModel.bumpStats { it.copy(words = it.words + 1) }
    }

    /** Poprzednie słowo przed wpisywanym (dla przewidywania i par słów). */
    private fun previousWord(): String? {
        val ic = ic ?: return null
        val before = ic.getTextBeforeCursor(80, 0)?.toString() ?: return null
        val text = if (composing.isNotEmpty() && before.endsWith(composing)) before.dropLast(composing.length) else before
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty() || trimmed.last() in ".!?\n") return null
        val m = Regex("""[\p{L}'’-]+$""").find(trimmed) ?: return null
        return m.value
    }

    fun pickSuggestion(s: Suggestion) {
        val ic = ic ?: return
        feedback.key()
        val typed = composing.toString()
        when (s.kind) {
            SuggestionKind.WORD, SuggestionKind.TYPED, SuggestionKind.PREDICTION, SuggestionKind.SHORTCUT -> {
                val prev = previousWord()
                val after = ic.getTextAfterCursor(1, 0)?.toString().orEmpty()
                val sep = if (after.startsWith(" ")) "" else " "
                if (s.kind == SuggestionKind.SHORTCUT && s.text.contains('{')) {
                    composing.clear()
                    ic.commitText("", 1)
                    commitExpanded(s.text, sep)
                } else {
                    ic.commitText(s.text + sep, 1)
                }
                if (after.startsWith(" ")) {
                    // Spacja już jest – przeskocz ją.
                    ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)?.let {
                        val c = it.startOffset + it.selectionEnd
                        ic.setSelection(c + 1, c + 1)
                    }
                }
                composing.clear()
                if (s.kind == SuggestionKind.TYPED && !incognito && settings.learnWords) UserModel.addWord(s.text)
                else learn(s.text, prev)
                if (!incognito) UserModel.bumpStats {
                    it.copy(suggestionsPicked = it.suggestionsPicked + 1, charsSaved = it.charsSaved + (s.text.length - typed.length).coerceAtLeast(0))
                }
                autoSpace = true
                lastCorrection = null
                keepTyped = false
            }
            SuggestionKind.EMOJI -> {
                if (composing.isNotEmpty()) commitComposing(autocorrect = true, separator = " ")
                else {
                    val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
                    if (before.isNotEmpty() && before != " ") ic.commitText(" ", 1)
                }
                ic.commitText(s.text + " ", 1)
                UserModel.addEmojiRecent(s.text)
                autoSpace = false
            }
            SuggestionKind.CALC -> {
                if (composing.isNotEmpty()) commitComposing(autocorrect = false)
                val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
                ic.commitText(if (before == "=") s.text else " = ${s.text}", 1)
            }
            SuggestionKind.UNDO -> {
                // Cofnięcie autokorekty z paska – jak Backspace, ale bez kasowania.
                val c = lastCorrection
                if (c != null) {
                    val n = c.second.length + c.third.length
                    if (ic.getTextBeforeCursor(n, 0)?.toString() == c.second + c.third) {
                        ic.deleteSurroundingText(n, 0)
                        ic.commitText(c.first + c.third, 1)
                        if (settings.learnFromUndo && !incognito) UserModel.rejectCorrection(c.first, c.second)
                    }
                }
                lastCorrection = null
            }
            SuggestionKind.CONVERT -> {
                if (composing.isNotEmpty()) commitComposing(autocorrect = false)
                val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
                ic.commitText((if (before.isEmpty() || before == " ") "" else " ") + "(${s.text})", 1)
            }
            SuggestionKind.CLIPBOARD -> {
                if (composing.isNotEmpty()) commitComposing(autocorrect = false, separator = " ")
                ic.commitText(s.text, 1)
                ClipboardHistory.dismissRecent()
            }
        }
        selfEdit()
        updateAutoShift()
        refreshSuggestions()
    }

    /** Przytrzymanie podpowiedzi: słowo przestaje być podpowiadane. */
    fun blockSuggestion(s: Suggestion) {
        if (s.kind != SuggestionKind.WORD && s.kind != SuggestionKind.PREDICTION) return
        UserModel.block(s.text)
        feedback.long()
        showToast(tr("„{0}” nie będzie podpowiadane", s.text))
        refreshSuggestions()
    }

    fun showToast(msg: String) {
        toast = msg
        scope.launch {
            delay(2200)
            if (toast == msg) toast = null
        }
    }

    // ───────────────────────── podpowiedzi ─────────────────────────

    private val suggestLangs: List<String>
        get() = if (settings.multilingual) settings.languages else listOf(settings.activeLanguage)

    /** Język z pierwszeństwem: wykryty w ostatnich słowach albo wybrany. */
    private val primaryLanguage: String
        get() = detectedLanguage?.takeIf { settings.autoLanguage && settings.multilingual && it in settings.languages } ?: settings.activeLanguage

    private fun computeSuggestions(typed: String, sentenceStart: Boolean = false): Suggestions {
        var s = Suggest.suggest(typed, previousWordSafe, suggestLangs, primaryLanguage, settings.autocorrect, proximity)
        // Przewidziane następne słowo nie znika po pierwszej literze – pasujące zostają na początku paska.
        if (settings.nextWord && !incognito) {
            val key = normalizeKey(typed)
            // Wyuczone pary słów; najczęstsze słowa języka i tak wygrywają w zwykłym rankingu.
            val preds = previousWordSafe?.let { UserModel.next(it, 30) }.orEmpty()
                .map { it.first }
                .filter { normalizeKey(it).startsWith(key) && normalizeKey(it) != key && !UserModel.isBlocked(it) }
            if (preds.isNotEmpty()) {
                val boosted = preds.take(3).map { Suggestion(Suggest.applyCase(typed, it), SuggestionKind.WORD) }
                val rest = s.items.filter { i -> boosted.none { it.text.equals(i.text, ignoreCase = true) } }
                val head = if (s.autoCorrect != null) rest.take(1) else emptyList()
                s = s.copy(items = head + boosted + rest.drop(head.size))
            }
        }
        if (!settings.autoCapitalize || capsWord) return s
        // Początek zdania: podpowiedzi i autokorekta z wielkiej litery, nawet gdy pisano małą.
        fun fix(x: Suggestion): Suggestion {
            if (x.kind != SuggestionKind.WORD) return x
            val t = SmartCase.fix(x.text, sentenceStart, suggestLangs, primaryLanguage)
            return if (t == x.text) x else x.copy(text = t, display = t)
        }
        val auto = s.autoCorrect?.let(::fix)
        return s.copy(items = s.items.map(::fix).distinctBy { it.text }, autoCorrect = auto)
    }

    /** Wpisywane słowo zaczyna zdanie (pole tekstowe z autokapitalizacją). */
    private fun composingAtSentenceStart(): Boolean {
        if (!settings.autoCapitalize || fieldKind != FieldKind.TEXT) return false
        val before = ic?.getTextBeforeCursor(80, 0)?.toString() ?: return false
        val text = if (composing.isNotEmpty() && before.endsWith(composing)) before.dropLast(composing.length) else before
        return SmartCase.isSentenceStart(text)
    }

    /** Który z włączonych języków zna najwięcej z ostatnich słów. */
    private fun detectLanguage(word: String) {
        recentWords.addLast(word)
        while (recentWords.size > 6) recentWords.removeFirst()
        if (settings.languages.size < 2) return
        var best: String? = null
        var bestScore = 0
        for (l in settings.languages) {
            val wl = Dictionaries.get(l) ?: continue
            val score = recentWords.count { wl.contains(it) && settings.languages.none { o -> o != l && Dictionaries.get(o)?.contains(it) == true } }
            if (score > bestScore) {
                bestScore = score
                best = l
            }
        }
        if (best != null && bestScore >= 2) detectedLanguage = best
    }

    // ───────────────────────── pisanie gestem ─────────────────────────

    fun onGlide(path: List<Pt>, centers: Map<Char, Pt>, keyW: Float) {
        val ic = ic ?: return
        if (!wordSuggestions) return
        if (composing.isNotEmpty()) commitComposing(autocorrect = !glidePending, separator = " ")
        else {
            val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
            if (before.isNotEmpty() && (before[0].isLetterOrDigit() || before[0] in ".,!?:;")) ic.commitText(" ", 1)
        }
        val prev = previousWord()
        val shifted = shift != ShiftState.OFF
        val lock = shift == ShiftState.LOCKED
        scope.launch {
            val words = withContext(Dispatchers.Default) {
                Glide.decode(path, centers, keyW, suggestLangs, primaryLanguage, prev, 8)
            }
            if (words.isEmpty()) {
                showToast(tr("Nie rozpoznano słowa"))
                return@launch
            }
            fun cased(w: String) = when {
                lock -> w.uppercase()
                shifted -> w.replaceFirstChar { it.uppercaseChar() }
                else -> w
            }
            val best = cased(words.first())
            composing.clear()
            composing.append(best)
            keepTyped = true
            glidePending = true
            this@KeyboardController.ic?.setComposingText(composing, 1)
            selfEdit()
            if (shift == ShiftState.ONESHOT) shift = ShiftState.OFF
            suggestions = Suggestions(words.map { Suggestion(cased(it), SuggestionKind.WORD) }, null, true)
            suggestionsFor = best
            if (!incognito) UserModel.bumpStats { it.copy(charsSaved = it.charsSaved + best.length) }
        }
    }

    // ───────────────────────── gesty ─────────────────────────

    fun runGesture(a: GestureAction) {
        when (a) {
            GestureAction.NONE, GestureAction.CURSOR -> {}
            GestureAction.LANGUAGE -> nextLanguage()
            GestureAction.HIDE -> hide()
            GestureAction.DELETE_WORD -> deleteWord()
            GestureAction.SHIFT -> lockShift()
            GestureAction.EMOJI -> openPanel(Panel.EMOJI)
            GestureAction.CLIPBOARD -> openPanel(Panel.CLIPBOARD)
            GestureAction.VOICE -> openPanel(Panel.VOICE)
            GestureAction.TEXT_TOOLS -> openPanel(Panel.TEXT)
            GestureAction.LANGUAGES -> if (settings.languages.size > 1) openPanel(Panel.LANGUAGES) else showImePicker()
            GestureAction.IME_PICKER -> showImePicker()
            GestureAction.NUMPAD -> switchMode(if (mode == KeyboardMode.NUMBER) KeyboardMode.ALPHA else KeyboardMode.NUMBER)
            GestureAction.UNDO -> undoRedo(false)
        }
    }

    // ───────────────────────── narzędzia tekstu ─────────────────────────

    /** Zmienia zaznaczony tekst – albo słowo przed kursorem, gdy nic nie zaznaczono. */
    fun transformText(fn: (String) -> String) {
        val ic = ic ?: return
        commitComposing(autocorrect = false)
        val sel = ic.getSelectedText(0)?.toString()
        if (!sel.isNullOrEmpty()) {
            val out = fn(sel)
            ic.commitText(out, 1)
            ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)?.let {
                val end = it.startOffset + it.selectionEnd
                ic.setSelection(end - out.length, end)
            }
        } else {
            val before = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
            val word = Regex("[\\p{L}\\p{N}'’-]+\\s*$").find(before)?.value?.trimEnd() ?: return
            val trailing = before.length - before.trimEnd().length
            ic.beginBatchEdit()
            ic.deleteSurroundingText(word.length + trailing, 0)
            ic.commitText(fn(word) + before.takeLast(trailing), 1)
            ic.endBatchEdit()
        }
        selfEdit()
        refreshSuggestions()
    }

    /** Liczba znaków i słów w polu (do panelu narzędzi tekstu). */
    fun fieldStats(): Pair<Int, Int> {
        val et = ic?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
        val t = et?.text?.toString() ?: ((ic?.getTextBeforeCursor(5000, 0) ?: "").toString() + (ic?.getTextAfterCursor(5000, 0) ?: ""))
        return t.length to TextTools.countWords(t)
    }

    fun clearField() {
        val ic = ic ?: return
        commitComposing(autocorrect = false)
        ic.performContextMenuAction(android.R.id.selectAll)
        ic.commitText("", 1)
        selfEdit()
        refreshSuggestions()
    }

    private var previousWordSafe: String? = null

    fun refreshSuggestions() {
        suggestJob?.cancel()
        if (internal != null) return
        val typed = composing.toString()
        previousWordSafe = previousWord()
        val sentenceStart = typed.isNotEmpty() && composingAtSentenceStart()
        val prevForPredict = previousWordSafe
        val before = ic?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        extras = buildExtras(typed, before)
        if (!wordSuggestions) {
            suggestions = Suggestions.Empty
            suggestionsFor = ""
            return
        }
        if (typed.isEmpty()) {
            suggestions = if (settings.nextWord && !incognito) Suggest.predict(prevForPredict, shift != ShiftState.OFF, 30, suggestLangs, primaryLanguage) else Suggestions.Empty
            suggestionsFor = ""
            return
        }
        suggestJob = scope.launch {
            val s = withContext(Dispatchers.Default) { computeSuggestions(typed, sentenceStart) }
            if (composing.toString() == typed) {
                suggestions = s
                suggestionsFor = typed
            }
        }
    }

    private fun buildExtras(typed: String, before: String): List<Suggestion> {
        val out = mutableListOf<Suggestion>()
        val corr = lastCorrection
        if (typed.isEmpty() && corr != null && !corr.first.equals(corr.second, ignoreCase = true) && UserModel.shortcutFor(corr.first) == null) {
            out += Suggestion(corr.first, SuggestionKind.UNDO, "↶ " + corr.first)
        }
        if (settings.unitConvert && typed.isEmpty()) {
            UnitConverter.find(before, activeLanguage)?.let { out += Suggestion(it, SuggestionKind.CONVERT, "≈ $it") }
        }
        if (settings.shortcuts && typed.isNotEmpty()) {
            UserModel.shortcutFor(typed)?.let { out += Suggestion(it.expansion, SuggestionKind.SHORTCUT, "↳ " + it.expansion.take(24)) }
        }
        if (settings.inlineCalc && typed.isEmpty()) {
            Calculator.findExpression(before)?.let { e -> Calculator.evaluate(e)?.let { out += Suggestion(it, SuggestionKind.CALC, "= $it") } }
        }
        if (settings.emojiSuggestions && wordSuggestions) {
            val word = typed.ifEmpty { Regex("""([\p{L}]+)\s$""").find(before)?.groupValues?.get(1).orEmpty() }
            EmojiData.forWord(word).forEach { out += Suggestion(it, SuggestionKind.EMOJI) }
        }
        if (settings.clipboardSuggestion && !incognito && typed.isEmpty()) {
            ClipboardHistory.consumeRecent()?.let { c ->
                out.add(0, Suggestion(c.text, SuggestionKind.CLIPBOARD, "📋 " + c.text.replace('\n', ' ').take(22)))
            }
        }
        return out
    }

    // ───────────────────────── kursor i zaznaczenie ─────────────────────────

    private fun selfEdit() {
        lastSelfEdit = SystemClock.uptimeMillis()
    }

    fun onUpdateSelection(oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int, candStart: Int, candEnd: Int) {
        val self = SystemClock.uptimeMillis() - lastSelfEdit < 250
        if (composing.isNotEmpty() && (newStart != newEnd || (candEnd != -1 && newEnd != candEnd) || candStart == -1)) {
            if (!self) {
                // Kursor przeniesiony przez użytkownika – słowo zostaje takie, jakie jest.
                composing.clear()
                ic?.finishComposingText()
                refreshSuggestions()
            }
        } else if (composing.isEmpty() && !self && newStart == newEnd) {
            lastCorrection = null
            autoSpace = false
            if (settings.reSuggest && wordSuggestions && internal == null) reSuggestAtCursor()
            updateAutoShift()
            refreshSuggestions()
        }
    }

    /** Kursor wewnątrz/na końcu słowa – słowo staje się edytowalne, z podpowiedziami jego poprawek. */
    private fun reSuggestAtCursor() {
        val ic = ic ?: return
        val before = ic.getTextBeforeCursor(48, 0)?.toString() ?: return
        val after = ic.getTextAfterCursor(48, 0)?.toString() ?: return
        val left = Regex("""[\p{L}'’-]+$""").find(before)?.value.orEmpty()
        val right = Regex("""^[\p{L}'’-]+""").find(after)?.value.orEmpty()
        if (left.isEmpty() || right.isNotEmpty()) return
        val et = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0) ?: return
        val cursor = et.startOffset + et.selectionEnd
        ic.setComposingRegion(cursor - left.length, cursor)
        composing.clear()
        composing.append(left)
        keepTyped = true
        selfEdit()
    }

    /** Słowo tuż przed kursorem (tekst [before]) staje się znów wpisywanym słowem. */
    private fun resumeWordBeforeCursor(before: String) {
        val ic = ic ?: return
        val left = Regex("""[\p{L}'’-]+$""").find(before)?.value.orEmpty()
        if (left.isEmpty() || !left.any { it.isLetter() }) return
        val after = ic.getTextAfterCursor(1, 0)?.toString().orEmpty()
        if (after.isNotEmpty() && after[0].isLetter()) return
        val et = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0) ?: return
        val cursor = et.startOffset + et.selectionEnd
        ic.setComposingRegion(cursor - left.length, cursor)
        composing.clear()
        composing.append(left)
        keepTyped = true
        glidePending = false
    }

    fun moveCursor(dx: Int) {
        if (dx == 0) return
        commitComposing(autocorrect = false)
        val ic = ic ?: return
        val et = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
        if (et == null || et.text == null) {
            repeat(kotlin.math.abs(dx)) { sendKey(if (dx > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT) }
            return
        }
        val len = et.startOffset + et.text.length
        if (selecting) {
            val start = et.startOffset + et.selectionStart
            val end = (et.startOffset + et.selectionEnd + dx).coerceIn(0, len)
            ic.setSelection(start, end)
        } else {
            val c = (et.startOffset + et.selectionEnd + dx).coerceIn(0, len)
            ic.setSelection(c, c)
        }
        selfEdit()
    }

    fun moveCursorVertical(down: Boolean) {
        commitComposing(autocorrect = false)
        if (selecting) sendKey(if (down) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP, KeyEvent.META_SHIFT_ON)
        else sendKey(if (down) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP)
    }

    fun cursorHomeEnd(end: Boolean) {
        commitComposing(autocorrect = false)
        val meta = if (selecting) KeyEvent.META_SHIFT_ON else 0
        sendKey(if (end) KeyEvent.KEYCODE_MOVE_END else KeyEvent.KEYCODE_MOVE_HOME, meta)
    }

    fun contextAction(id: Int) {
        commitComposing(autocorrect = false)
        ic?.performContextMenuAction(id)
        if (id == android.R.id.copy || id == android.R.id.cut) showToast(tr("Skopiowano"))
        refreshSuggestions()
    }

    fun undoRedo(redo: Boolean) {
        commitComposing(autocorrect = false)
        sendKey(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON or (if (redo) KeyEvent.META_SHIFT_ON else 0))
    }

    fun sendKey(code: Int, meta: Int = 0) {
        val ic = ic ?: return
        val t = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0, meta))
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0, meta))
    }

    // ───────────────────────── języki ─────────────────────────

    fun nextLanguage() {
        val langs = settings.languages
        if (langs.size < 2) return
        val i = langs.indexOf(settings.activeLanguage)
        setLanguage(langs[(i + 1) % langs.size])
    }

    /** Przesuwanie przytrzymanej spacji: kolejny język w lewo albo w prawo. */
    fun languageStep(dir: Int): Boolean {
        val langs = settings.languages
        if (langs.size < 2) return false
        val i = langs.indexOf(settings.activeLanguage)
        setLanguage(langs[(i + dir).mod(langs.size)])
        return true
    }

    fun setLanguage(code: String) {
        commitComposing(autocorrect = false)
        Prefs.update { it.copy(activeLanguage = code) }
        showToast(Languages.get(code).native)
    }

    // ───────────────────────── panele ─────────────────────────

    fun openPanel(p: Panel) {
        commitComposing(autocorrect = false)
        if (p == Panel.VOICE) {
            panel = p
            voice.start()
            return
        }
        panel = if (panel == p) Panel.KEYS else p
        toolbarOpen = false
        if (panel == Panel.KEYS) {
            internal = null
            refreshSuggestions()
        }
    }

    fun closePanel() {
        if (panel == Panel.VOICE) voice.stop()
        panel = Panel.KEYS
        internal = null
        refreshSuggestions()
    }

    fun openInternal(t: InternalTarget) {
        commitComposing(autocorrect = false)
        internal = t
        internalText = ""
        panel = Panel.KEYS
        toolbarOpen = false
        if (mode != KeyboardMode.ALPHA) switchMode(KeyboardMode.ALPHA)
        shift = ShiftState.OFF
    }

    fun closeInternal(back: Panel = Panel.KEYS) {
        internal = null
        internalText = ""
        panel = back
        updateAutoShift()
        refreshSuggestions()
    }

    /** Wyniki wyszukiwania emoji odświeżają się same (stan [internalText]). */
    private fun afterInternalChange() {}

    fun toggleIncognito() {
        Prefs.update { it.copy(forceIncognito = !it.forceIncognito) }
        val s = Prefs.current
        val noLearning = editorInfo?.let { (it.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0 } ?: false
        incognito = s.forceIncognito || fieldKind == FieldKind.PASSWORD || (s.incognitoAuto && noLearning)
        ClipboardHistory.paused = incognito
        showToast(if (incognito) tr("Tryb incognito") else tr("Incognito wyłączone"))
        refreshSuggestions()
    }


    fun commitEmoji(e: String) {
        if (internal == InternalTarget.EMOJI_SEARCH) {
            ic?.commitText(e, 1)
            UserModel.addEmojiRecent(e)
            return
        }
        commitComposing(autocorrect = false)
        ic?.commitText(e, 1)
        UserModel.addEmojiRecent(e)
        selfEdit()
    }

    fun commitRaw(text: String) {
        commitComposing(autocorrect = false)
        ic?.commitText(text, 1)
        selfEdit()
        refreshSuggestions()
    }

    fun commitVoice(text: String) {
        if (text.isBlank()) return
        val ic = ic ?: return
        commitComposing(autocorrect = false)
        val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        val capsMode = editorInfo?.let { ic.getCursorCapsMode(it.inputType) } ?: 0
        var t = text.trim()
        if (settings.voiceAutoPunct && capsMode != 0) t = t.replaceFirstChar { it.uppercaseChar() }
        val lead = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
        ic.commitText(lead + t + " ", 1)
        selfEdit()
        updateAutoShift()
        refreshSuggestions()
    }

    fun openApp(section: String? = null) {
        val i = Intent(service, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (section != null) i.putExtra(MainActivity.EXTRA_SECTION, section)
        runCatching { service.startActivity(i) }
    }

    fun hide() {
        commitComposing(autocorrect = false)
        service.requestHideSelf(0)
    }

    fun switchIme() {
        service.switchToNextInputMethod(false)
    }

    fun showImePicker() {
        val imm = service.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
        imm?.showInputMethodPicker()
    }

    fun updateSettings(transform: (KeyboardSettings) -> KeyboardSettings) = Prefs.update(transform)

    fun toggleOneHanded() = Prefs.update {
        it.copy(oneHanded = when (it.oneHanded) {
            OneHanded.OFF -> OneHanded.RIGHT
            OneHanded.RIGHT -> OneHanded.LEFT
            OneHanded.LEFT -> OneHanded.OFF
        })
    }

    fun toggleFloating() {
        floatingOffset = Offset.Zero
        Prefs.update { it.copy(floating = !it.floating) }
    }

    // Na końcu klasy – wszystkie pola są już utworzone.
    init {
        scope.launch {
            Prefs.flow.collectLatest { s ->
                val old = settings
                settings = s
                if (old.languages != s.languages || old.activeLanguage != s.activeLanguage) {
                    Dictionaries.ensureLoaded(s.languages)
                }
                ClipboardHistory.enabled = s.clipboardHistory
                ClipboardHistory.limit = s.clipboardSize
                rebuild()
            }
        }
        Dictionaries.ensureLoaded(settings.languages)
    }
}

/** Języki z cudzysłowem otwierającym na dole („…”). */
private val LOW_QUOTE_LANGS = setOf("pl", "de", "cs", "sk", "hu", "ro", "hr", "lt")

/** Nawiasy i cudzysłowy domykane automatycznie (ustawienie „Domykaj nawiasy”). */
private val PAIRS = mapOf('(' to ')', '[' to ']', '{' to '}', '„' to '”', '“' to '”', '«' to '»')

private fun graphemeLengthBefore(text: String): Int {
    if (text.isEmpty()) return 0
    val it = BreakIterator.getCharacterInstance()
    it.setText(text)
    val end = it.last()
    val start = it.previous()
    return if (start == BreakIterator.DONE) 1 else end - start
}

private fun wordLengthBefore(text: String): Int {
    if (text.isEmpty()) return 0
    var i = text.length
    while (i > 0 && text[i - 1].isWhitespace()) i--
    if (i > 0 && !text[i - 1].isLetterOrDigit()) {
        while (i > 0 && !text[i - 1].isLetterOrDigit() && !text[i - 1].isWhitespace()) i--
    } else {
        while (i > 0 && (text[i - 1].isLetterOrDigit() || text[i - 1] == '\'' || text[i - 1] == '’')) i--
    }
    return (text.length - i).coerceAtLeast(1)
}

private fun String.dropLastGrapheme(): String = dropLast(graphemeLengthBefore(this))
