package com.blacksingularities.openkeyboard.spell

import android.service.textservice.SpellCheckerService
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import com.blacksingularities.openkeyboard.data.Autocorrect
import com.blacksingularities.openkeyboard.data.Prefs
import com.blacksingularities.openkeyboard.dict.Dictionaries
import com.blacksingularities.openkeyboard.dict.Proximity
import com.blacksingularities.openkeyboard.dict.Suggest
import com.blacksingularities.openkeyboard.dict.SuggestionKind
import kotlinx.coroutines.runBlocking

/**
 * Sprawdzanie pisowni dla całego systemu (podkreślenia w polach tekstowych innych aplikacji),
 * na tych samych słownikach, co klawiatura – wszystkie włączone języki naraz.
 */
class OpenSpellChecker : SpellCheckerService() {
    override fun createSession(): Session = object : Session() {
        override fun onCreate() {
            runBlocking { Dictionaries.awaitLoaded(Prefs.current.languages) }
        }

        override fun onGetSuggestions(textInfo: TextInfo, suggestionsLimit: Int): SuggestionsInfo {
            val word = textInfo.text.trim()
            val langs = Prefs.current.languages
            if (word.isEmpty() || word.any { it.isDigit() } || word.length < 2 || Suggest.isValid(word, langs) || Suggest.isValid(word.lowercase(), langs)) {
                return SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, emptyArray(), textInfo.cookie, textInfo.sequence)
            }
            val s = Suggest.suggest(word, null, langs, Prefs.current.activeLanguage, Autocorrect.NORMAL, Proximity { _, _ -> false }, suggestionsLimit.coerceIn(1, 5))
            val list = s.items.filter { it.kind == SuggestionKind.WORD }.map { it.text }.toTypedArray()
            val attrs = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or (if (list.isNotEmpty()) SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS else 0)
            return SuggestionsInfo(attrs, list, textInfo.cookie, textInfo.sequence)
        }

        override fun onGetSentenceSuggestionsMultiple(textInfos: Array<out TextInfo>, suggestionsLimit: Int): Array<SentenceSuggestionsInfo> =
            textInfos.map { ti ->
                val text = ti.text
                val results = mutableListOf<SuggestionsInfo>()
                val offsets = mutableListOf<Int>()
                val lengths = mutableListOf<Int>()
                Regex("""[\p{L}'’]+""").findAll(text).forEach { m ->
                    val info = onGetSuggestions(TextInfo(m.value, ti.cookie, ti.sequence), suggestionsLimit)
                    results += info
                    offsets += m.range.first
                    lengths += m.value.length
                }
                SentenceSuggestionsInfo(results.toTypedArray(), offsets.toIntArray(), lengths.toIntArray())
            }.toTypedArray()
    }
}
