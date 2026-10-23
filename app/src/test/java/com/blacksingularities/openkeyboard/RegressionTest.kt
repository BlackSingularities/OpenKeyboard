package com.blacksingularities.openkeyboard

import com.blacksingularities.openkeyboard.data.Autocorrect
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.dict.Dictionaries
import com.blacksingularities.openkeyboard.dict.Suggest
import com.blacksingularities.openkeyboard.dict.WordList
import com.blacksingularities.openkeyboard.ime.proximityOf
import com.blacksingularities.openkeyboard.keyboard.FieldKind
import com.blacksingularities.openkeyboard.keyboard.KeyboardMode
import com.blacksingularities.openkeyboard.keyboard.Layouts
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Regresja autokorekty na prawdziwych, wbudowanych słownikach (polski i angielski naraz):
 * wpisane → to, co trafi do pola po spacji. Każda zmiana heurystyk musi zachować te wyniki.
 */
class RegressionTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun loadDictionaries() {
            for (code in listOf("pl", "en")) {
                val f = File("src/main/assets/dict/$code.dict")
                Dictionaries.putForTest(code, GZIPInputStream(f.inputStream().buffered()).bufferedReader().useLines { WordList.parse(code, it) })
            }
        }

        private val proximity by lazy { proximityOf(Layouts.forSettings(KeyboardMode.ALPHA, KeyboardSettings(), FieldKind.TEXT)) }

        /** (wpisane, język z pierwszeństwem, oczekiwany wynik po spacji). */
        private val CASES = listOf(
            // bez polskich znaków
            Triple("zolw", "pl", "żółw"),
            Triple("prosze", "pl", "proszę"),
            Triple("dziekuje", "pl", "dziękuję"),
            Triple("wiekszosc", "pl", "większość"),
            // sklejone słowa i litera zamiast spacji
            Triple("jakdzien", "pl", "jak dzień"),
            Triple("wbtej", "pl", "w tej"),
            Triple("dzieńndobry", "pl", "dzień dobry"),
            // jednoliterowe polskie słowa zostają
            Triple("i", "pl", "i"),
            Triple("w", "pl", "w"),
            // poprawne słowa bez zmian
            Triple("kot", "pl", "kot"),
            Triple("dom", "pl", "dom"),
            Triple("house", "en", "house"),
            // prawdziwe słowa bez ogonków, choć forma z ogonkami bywa częstsza – zostają
            Triple("ze", "pl", "ze"),
            Triple("piec", "pl", "piec"),
            Triple("sad", "pl", "sad"),
            Triple("chwile", "pl", "chwile"),
            Triple("prace", "pl", "prace"),
            Triple("ziemie", "pl", "ziemie"),
            // znane błędy z korpusu napisów – poprawiane
            Triple("troche", "pl", "trochę"),
            Triple("miedzy", "pl", "między"),
            Triple("musze", "pl", "muszę"),
            // angielski
            Triple("teh", "en", "the"),
            Triple("dont", "en", "don't"),
        )
    }

    @Test
    fun autocorrectTable() {
        val failures = CASES.mapNotNull { (typed, primary, expected) ->
            val s = Suggest.suggest(typed, null, listOf("pl", "en"), primary, Autocorrect.NORMAL, proximity)
            val got = s.autoCorrect?.text ?: typed
            if (got == expected) null else "„$typed” → „$got” (oczekiwano „$expected”)"
        }
        assertEquals("Zmieniona autokorekta:\n" + failures.joinToString("\n"), emptyList<String>(), failures)
    }
}
