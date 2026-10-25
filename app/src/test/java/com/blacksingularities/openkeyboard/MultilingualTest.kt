package com.blacksingularities.openkeyboard

import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Languages
import com.blacksingularities.openkeyboard.dict.FieldContext
import com.blacksingularities.openkeyboard.dict.LanguageDetector
import com.blacksingularities.openkeyboard.dict.Suggest
import com.blacksingularities.openkeyboard.dict.UserModel
import com.blacksingularities.openkeyboard.dict.wordsBefore
import com.blacksingularities.openkeyboard.ime.AppLanguageMemory
import com.blacksingularities.openkeyboard.ime.WritingLanguages
import com.blacksingularities.openkeyboard.ime.smartQuote
import com.blacksingularities.openkeyboard.keyboard.FieldKind
import com.blacksingularities.openkeyboard.keyboard.KeyType
import com.blacksingularities.openkeyboard.keyboard.KeyboardMode
import com.blacksingularities.openkeyboard.keyboard.Layouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultilingualTest {
    /** Mini-słowniki: pozycja w rankingu częstości (od 1). */
    private val ranks = mapOf(
        "pl" to listOf("i", "a", "to", "nie", "jest", "w", "na", "mój", "dom", "kot", "jak", "ma", "the").withIndex().associate { it.value to it.index + 1 },
        "en" to listOf("the", "a", "to", "i", "is", "on", "cat", "table", "my", "house", "nie").withIndex().associate { it.value to it.index + 1 },
    )
    private val detector get() = LanguageDetector { l, w -> ranks[l]?.get(w) }

    @Test
    fun detectsLanguageFromDistinctWords() {
        val d = detector
        "the cat is on the table".split(' ').forEach(d::add)
        assertEquals("en", d.detect(listOf("pl", "en")))
        d.clear()
        "to jest mój dom".split(' ').forEach(d::add)
        assertEquals("pl", d.detect(listOf("pl", "en")))
    }

    @Test
    fun sharedWordsDoNotDecide() {
        val d = detector
        listOf("a", "to", "i").forEach(d::add)
        assertNull(d.detect(listOf("pl", "en")))
        // Jeden język – nie ma czego wykrywać.
        d.add("kot")
        assertNull(d.detect(listOf("pl")))
    }

    @Test
    fun recentWordsWin() {
        val d = detector
        d.addText("To jest mój dom i kot. Now the cat is on my table")
        assertEquals("en", d.detect(listOf("pl", "en")))
    }

    @Test
    fun writingLanguageFromFieldTextAndAppMemory() {
        val memory = object : AppLanguageMemory {
            val m = HashMap<String, String>()
            override fun get(pkg: String) = m[pkg]
            override fun set(pkg: String, lang: String) { m[pkg] = lang }
        }
        val w = WritingLanguages({ l, word -> ranks[l]?.get(word) }, memory)
        val s = KeyboardSettings(languages = listOf("pl", "en"), activeLanguage = "pl")
        // Tekst w polu rozstrzyga od razu.
        w.startField("app.chat", "the cat is on the table", s)
        assertEquals("en", w.primary(s))
        // Pisanie po polsku – zmiana i zapamiętanie dla aplikacji.
        listOf("to", "jest", "mój", "dom", "nie", "kot").forEach { w.onWord(it, s, remember = true) }
        assertEquals("pl", w.primary(s))
        assertEquals("pl", memory.m["app.chat"])
        // Puste pole w tej samej aplikacji – ostatni język.
        memory.m["app.chat"] = "en"
        w.startField("app.chat", "", s)
        assertEquals("en", w.primary(s))
        // Bez trybu „wiele naraz” – zawsze wybrany język.
        val single = s.copy(multilingual = false)
        assertEquals("pl", w.primary(single))
        assertEquals(listOf("pl"), w.suggestLangs(single))
    }

    @Test
    fun switchingOnlyBetweenScripts() {
        assertEquals(listOf("pl"), Languages.switchTargets(listOf("pl", "en"), "pl", multilingual = true).take(1))
        assertEquals(1, Languages.switchTargets(listOf("pl", "en", "de"), "en", multilingual = true).size)
        assertEquals(listOf("en", "ru"), Languages.switchTargets(listOf("pl", "en", "ru", "uk"), "en", multilingual = true))
        assertEquals(listOf("pl", "en"), Languages.switchTargets(listOf("pl", "en"), "pl", multilingual = false))
        assertEquals(listOf("ru", "uk"), Languages.sameScript("ru", listOf("pl", "uk", "en")))
    }

    @Test
    fun sharedLayoutForSameScript() {
        val keys = { s: KeyboardSettings -> Layouts.forSettings(KeyboardMode.ALPHA, s, FieldKind.TEXT).rows.flatten() }
        // Polski + angielski naraz: bez klawisza języka, spacja „PL · EN”.
        val plEn = keys(KeyboardSettings(languages = listOf("pl", "en"), activeLanguage = "pl"))
        assertFalse(plEn.any { it.type == KeyType.LANGUAGE })
        assertEquals("PL · EN", plEn.first { it.type == KeyType.SPACE }.label)
        // Polski + rosyjski: inny alfabet – klawisz języka zostaje.
        val plRu = keys(KeyboardSettings(languages = listOf("pl", "ru"), activeLanguage = "pl"))
        assertTrue(plRu.any { it.type == KeyType.LANGUAGE })
        // Polski + niemiecki: na „a” oba znaki, polski pierwszy.
        val plDe = keys(KeyboardSettings(languages = listOf("pl", "de"), activeLanguage = "pl", numberRow = true))
        val a = plDe.first { it.type == KeyType.CHAR && it.text == "a" }.popup
        assertTrue(a.indexOf("ą") in 0 until a.indexOf("ä"))
    }

    @Test
    fun systemLanguagesOnFirstRun() {
        assertEquals(listOf("pl", "en"), Languages.fromSystem(listOf("pl")))
        assertEquals(listOf("de", "en"), Languages.fromSystem(listOf("de", "ja")))
        assertEquals(listOf("en"), Languages.fromSystem(listOf("en")))
        assertEquals(listOf("en"), Languages.fromSystem(listOf("ja")))
        assertEquals(listOf("no", "id", "en"), Languages.fromSystem(listOf("nb", "in")))
    }

    @Test
    fun wordsBeforeCursorInSentence() {
        assertEquals(listOf("ala", "ma"), wordsBefore("ala ma ", 2))
        assertEquals(listOf("ma"), wordsBefore("Tak, ala, ma ", 2))
        assertEquals(emptyList<String>(), wordsBefore("Koniec. ", 2))
        assertEquals(emptyList<String>(), wordsBefore("kot, ", 2))
        assertEquals(listOf("don't", "know"), wordsBefore("I don't know ", 2))
        assertEquals(listOf("kot"), wordsBefore("kot, ", 2, acrossPunctuation = true))
        assertEquals(listOf("to", "koniec"), wordsBefore("to koniec. ", 2, acrossPunctuation = true))
    }

    @Test
    fun fieldContextKnowsNamesAndOrder() {
        val f = FieldContext.of("Wczoraj spotkałem Bartosza. Bartosz ma nowego kota. Potem Bartosz ma czas")
        assertEquals("Bartosza", f.withPrefix("bartosz", 5).map { it.first }.first { it.endsWith("a") })
        assertTrue(f.withPrefix("bart", 5).any { it.first == "Bartosz" })
        // Po „Bartosz ma” padło „nowego” i „czas”; zdanie się kończy na kropce.
        assertEquals(setOf("nowego", "czas"), f.next("Bartosz", "ma").map { it.first }.toSet())
        assertEquals(0, f.followCount(null, "Bartosza", "Bartosz"))
        assertEquals(2, f.count("bartosz"))
    }

    @Test
    fun trigramsPredictNextWord() {
        repeat(3) { UserModel.learn("zxkota", "zxma", "zxala") }
        UserModel.learn("zxpsa", "zxma")
        assertEquals("zxkota", Suggest.nextWords("zxala", "zxma").first())
        // Kolejność słów z pola też przewiduje.
        val f = FieldContext.of("zxjan zxlubi zxjabłka")
        assertTrue("zxjabłka" in Suggest.nextWords("zxjan", "zxlubi", f))
        UserModel.removeWord("zxkota")
        UserModel.removeWord("zxpsa")
    }

    @Test
    fun quotesFollowWritingLanguage() {
        assertEquals("„", smartQuote("\"", ' ', "pl"))
        assertEquals("”", smartQuote("\"", 'a', "pl"))
        assertEquals("“", smartQuote("\"", 'a', "de"))
        assertEquals("“", smartQuote("\"", null, "en"))
        assertEquals("’", smartQuote("'", 't', "en"))
    }
}
