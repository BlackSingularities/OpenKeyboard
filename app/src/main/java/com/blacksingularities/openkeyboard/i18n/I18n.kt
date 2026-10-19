package com.blacksingularities.openkeyboard.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blacksingularities.openkeyboard.data.AppLanguage

/**
 * Lokalizacja: teksty w kodzie są po polsku (klucze), a [En] zawiera ich angielskie odpowiedniki.
 * Szablony {0}, {1}… są zastępowane argumentami. Odczyt języka to stan Compose – zmiana języka
 * od razu odświeża cały interfejs (także klawiatury).
 */
object I18n {
    var english by mutableStateOf(false)
        private set

    fun apply(language: AppLanguage, systemLanguage: String) {
        english = when (language) {
            AppLanguage.PL -> false
            AppLanguage.EN -> true
            AppLanguage.SYSTEM -> systemLanguage != "pl"
        }
    }
}

fun tr(pl: String, vararg args: Any?): String {
    val text = if (I18n.english) En[pl] ?: pl else pl
    if (args.isEmpty()) return text
    var out = text
    args.forEachIndexed { i, a -> out = out.replace("{$i}", a?.toString() ?: "") }
    return out
}
