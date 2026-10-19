package com.blacksingularities.openkeyboard.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Język interfejsu aplikacji (nie mylić z językami pisania). */
enum class AppLanguage { SYSTEM, PL, EN }

/** Wygląd klawiszy: wypełnione, z obwódką albo same znaki na tle. */
enum class KeyStyle { FILLED, OUTLINE, FLAT }

/** Tryb jednej ręki: klawiatura zwężona i dosunięta do krawędzi. */
enum class OneHanded { OFF, LEFT, RIGHT }

/** Siła autokorekty: łagodna poprawia tylko oczywiste literówki. */
enum class Autocorrect { OFF, GENTLE, NORMAL, AGGRESSIVE }

/** Przyciski paska narzędzi (gdy nie ma podpowiedzi). */
enum class ToolbarItem { CLIPBOARD, EMOJI, EDIT, TEXT_TOOLS, VOICE, TRANSLATE, ONE_HANDED, FLOATING, NUMPAD, SETTINGS }

/** Czynność przypisana do gestu. */
enum class GestureAction { NONE, CURSOR, LANGUAGE, HIDE, DELETE_WORD, SHIFT, EMOJI, CLIPBOARD, VOICE, TEXT_TOOLS, LANGUAGES, IME_PICKER, NUMPAD, UNDO }

/** Czcionka napisów na klawiszach. */
enum class KeyFont { DEFAULT, SANS, SERIF, MONO, CONDENSED }

/** Dzielenie klawiatury na dwie połówki (wygodne kciukami na szerokim ekranie). */
enum class SplitMode { OFF, LANDSCAPE, ALWAYS }

@Serializable
data class KeyboardSettings(
    // Języki pisania: kolejność = kolejność przełączania; [activeLanguage] – bieżący układ.
    val languages: List<String> = listOf("pl", "en"),
    val activeLanguage: String = "pl",
    /** Podpowiedzi i autokorekta ze wszystkich włączonych języków naraz. */
    val multilingual: Boolean = true,
    /** Własny układ dla języka (np. „qwertz” dla polskiego). */
    val layoutOverride: Map<String, String> = emptyMap(),

    // Układ
    val numberRow: Boolean = false,
    val keyHints: Boolean = true,
    val keyHeight: Int = 100,
    val bottomPadding: Int = 0,
    val oneHanded: OneHanded = OneHanded.OFF,
    val oneHandedWidth: Int = 80,
    val commaKey: Boolean = true,
    val emojiKey: Boolean = false,
    val languageKey: Boolean = true,
    val longPressMs: Int = 300,
    val floating: Boolean = false,
    val floatingScale: Int = 85,
    val split: SplitMode = SplitMode.LANDSCAPE,
    val splitGap: Int = 22,

    // Pisanie i podpowiedzi
    val suggestions: Boolean = true,
    val autocorrect: Autocorrect = Autocorrect.NORMAL,
    val nextWord: Boolean = true,
    val learnWords: Boolean = true,
    val autoCapitalize: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    val smartPunctuation: Boolean = true,
    val emojiSuggestions: Boolean = true,
    val inlineCalc: Boolean = true,
    val shortcuts: Boolean = true,
    val reSuggest: Boolean = true,
    val clipboardSuggestion: Boolean = true,
    val incognitoAuto: Boolean = true,
    val forceIncognito: Boolean = false,
    val blockOffensive: Boolean = false,
    /** Pisanie gestem: przeciągnij palcem po literach słowa. */
    val glideTyping: Boolean = true,
    val glideTrail: Boolean = true,
    /** Wykrywanie języka, w którym właśnie piszesz – ten słownik dostaje pierwszeństwo. */
    val autoLanguage: Boolean = true,
    /** Cofnięta poprawka nie wraca, a słowo trafia do słownika. */
    val learnFromUndo: Boolean = true,
    val suggestionCount: Int = 3,
    val smartQuotes: Boolean = false,
    val autoSpaceAfterPunct: Boolean = true,

    // Wygląd
    val theme: String = "black",
    val accent: Int = 0,
    val keyStyle: KeyStyle = KeyStyle.FILLED,
    val keyRadius: Int = 9,
    val keyGap: Int = 5,
    val fontScale: Int = 100,
    val keyPreview: Boolean = true,
    val showSpaceLanguage: Boolean = true,
    val boldLabels: Boolean = false,
    val keyFont: KeyFont = KeyFont.DEFAULT,
    // Własny motyw (theme = "custom"): kolory ARGB i opcjonalny obraz tła.
    val customBg: Long = 0xFF000000,
    val customKey: Long = 0xFF1A1A1A,
    val customText: Long = 0xFFFFFFFF,
    val customAccent: Long = 0xFFFFFFFF,
    val bgImage: Boolean = false,
    val bgImageStamp: Long = 0,
    val bgDim: Int = 45,
    val keyOpacity: Int = 100,

    // Dotyk i dźwięk
    val haptics: Boolean = true,
    val hapticStrength: Int = 25,
    val sound: Boolean = false,
    val soundVolume: Int = 40,
    val deleteSwipe: Boolean = true,
    // Gesty – każdemu można przypisać czynność.
    val spaceSwipe: GestureAction = GestureAction.CURSOR,
    val spaceSwipeUp: GestureAction = GestureAction.TEXT_TOOLS,
    val spaceLongPress: GestureAction = GestureAction.LANGUAGES,
    val swipeDown: GestureAction = GestureAction.HIDE,
    val enterLongPress: GestureAction = GestureAction.EMOJI,
    val commaLongPress: GestureAction = GestureAction.NONE,

    // Narzędzia
    val toolbar: List<ToolbarItem> = listOf(
        ToolbarItem.CLIPBOARD, ToolbarItem.EMOJI, ToolbarItem.EDIT, ToolbarItem.TEXT_TOOLS, ToolbarItem.VOICE,
        ToolbarItem.TRANSLATE, ToolbarItem.ONE_HANDED, ToolbarItem.FLOATING, ToolbarItem.SETTINGS,
    ),
    val clipboardHistory: Boolean = true,
    val clipboardSize: Int = 60,
    val translateTarget: String = "en",
    val voiceAutoPunct: Boolean = true,

    // Ogólne
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val autoUpdate: Boolean = true,
    val onboardingDone: Boolean = false,
)

/** Ustawienia współdzielone przez aplikację i klawiaturę (ten sam proces). */
object Prefs {
    private const val FILE = "openkeyboard"
    private const val KEY = "settings"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private lateinit var appContext: Context
    private val state = MutableStateFlow(KeyboardSettings())
    val flow: StateFlow<KeyboardSettings> get() = state
    val current: KeyboardSettings get() = state.value

    fun init(context: Context) {
        appContext = context.applicationContext
        val raw = prefs().getString(KEY, null)
        state.value = raw?.let { runCatching { json.decodeFromString(KeyboardSettings.serializer(), it) }.getOrNull() } ?: KeyboardSettings()
    }

    private fun prefs() = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun update(transform: (KeyboardSettings) -> KeyboardSettings) {
        val next = transform(state.value)
        if (next == state.value) return
        state.value = next
        prefs().edit().putString(KEY, json.encodeToString(KeyboardSettings.serializer(), next)).apply()
    }

    fun exportJson(): String = json.encodeToString(KeyboardSettings.serializer(), state.value)

    fun importJson(raw: String) {
        val s = json.decodeFromString(KeyboardSettings.serializer(), raw)
        update { s }
    }

    fun reset() = update { KeyboardSettings(onboardingDone = it.onboardingDone) }
}
