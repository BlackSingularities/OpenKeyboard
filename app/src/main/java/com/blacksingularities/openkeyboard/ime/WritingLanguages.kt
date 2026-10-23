package com.blacksingularities.openkeyboard.ime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Languages
import com.blacksingularities.openkeyboard.dict.Dictionaries
import com.blacksingularities.openkeyboard.dict.LanguageDetector
import com.blacksingularities.openkeyboard.dict.UserModel

/**
 * Język pisania: w trybie „wiele naraz” bez wybierania – pierwszeństwo ma język wykryty
 * w ostatnich słowach, w tekście, który już jest w polu, albo zapamiętany dla aplikacji.
 * Od niego zależą kolejność podpowiedzi, cudzysłowy, daty, dyktowanie i przelicznik jednostek.
 */
class WritingLanguages(
    rank: (lang: String, word: String) -> Int? = { l, w -> Dictionaries.get(l)?.indexOf(w)?.takeIf { it >= 0 }?.plus(1) },
    private val appMemory: AppLanguageMemory = UserModelMemory,
) {
    private val detector = LanguageDetector(rank)
    private var app: String? = null

    /** Język wykryty w tym polu (albo zapamiętany dla aplikacji) – null, gdy nie wiadomo. */
    var detected by mutableStateOf<String?>(null)
        private set

    /** Słowniki podpowiedzi: w trybie „wiele naraz” wszystkie włączone języki bieżącego alfabetu. */
    fun suggestLangs(s: KeyboardSettings): List<String> =
        if (s.multilingual) Languages.sameScript(s.activeLanguage, s.languages) else listOf(s.activeLanguage)

    /** Język z pierwszeństwem: wykryty albo ten, którego układ jest na klawiaturze. */
    fun primary(s: KeyboardSettings): String =
        detected?.takeIf { s.autoLanguage && s.multilingual && it in suggestLangs(s) } ?: s.activeLanguage

    /** Nowe pole: język z tekstu, który już w nim jest, a bez niego – ostatni język w tej aplikacji. */
    fun startField(pkg: String?, text: String, s: KeyboardSettings) {
        app = pkg
        detector.clear()
        detected = null
        if (!s.multilingual || !s.autoLanguage) return
        val langs = suggestLangs(s)
        if (langs.size < 2) return
        detector.addText(text)
        detected = detector.detect(langs) ?: pkg?.let { appMemory.get(it) }?.takeIf { it in langs }
    }

    /** Zatwierdzone słowo; [remember] – zapamiętaj język dla aplikacji (nie w incognito). */
    fun onWord(word: String, s: KeyboardSettings, remember: Boolean) {
        detector.add(word)
        if (!s.multilingual || !s.autoLanguage) return
        val d = detector.detect(suggestLangs(s)) ?: return
        detected = d
        if (remember) app?.let { appMemory.set(it, d) }
    }
}

/** Zapamiętany język pisania dla aplikacji (w testach – zwykła mapa). */
interface AppLanguageMemory {
    fun get(pkg: String): String?
    fun set(pkg: String, lang: String)
}

private object UserModelMemory : AppLanguageMemory {
    override fun get(pkg: String): String? = UserModel.appLanguage(pkg)
    override fun set(pkg: String, lang: String) = UserModel.setAppLanguage(pkg, lang)
}
