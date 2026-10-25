package com.blacksingularities.openkeyboard

import com.blacksingularities.openkeyboard.dict.Dictionaries
import com.blacksingularities.openkeyboard.dict.Proximity
import com.blacksingularities.openkeyboard.dict.Suggest
import com.blacksingularities.openkeyboard.dict.WordList
import com.blacksingularities.openkeyboard.features.SmartCase
import com.blacksingularities.openkeyboard.features.TextTools
import com.blacksingularities.openkeyboard.features.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class KeyboardV14Test {
    @Test
    fun shiftCyclesWordCase() {
        assertEquals("Kot", TextTools.cycleWordCase("kot"))
        assertEquals("KOT", TextTools.cycleWordCase("Kot"))
        assertEquals("kot", TextTools.cycleWordCase("KOT"))
        assertEquals("Kot", TextTools.cycleWordCase("kOt"))
        assertEquals("K", TextTools.cycleWordCase("k"))
        assertEquals("k", TextTools.cycleWordCase("K"))
    }

    @Test
    fun sentenceStartAndSmartCase() {
        assertTrue(SmartCase.isSentenceStart(""))
        assertTrue(SmartCase.isSentenceStart("To jest kot. "))
        assertTrue(SmartCase.isSentenceStart("Serio?! "))
        assertTrue(SmartCase.isSentenceStart("linia\n"))
        assertFalse(SmartCase.isSentenceStart("To jest "))
        assertFalse(SmartCase.isSentenceStart("np. "))
        assertFalse(SmartCase.isSentenceStart("Kupię m.in. "))
        assertEquals("Ala", SmartCase.fix("ala", true, listOf("pl")))
        assertEquals("ala", SmartCase.fix("ala", false, listOf("pl")))
        assertEquals("Hello", SmartCase.fix("HEllo", false, listOf("en")))
        assertEquals("OK", SmartCase.fix("OK", false, listOf("en")))
        assertEquals("Monday", SmartCase.fix("monday", false, listOf("pl", "en")))
        assertEquals("monday", SmartCase.fix("monday", false, listOf("pl")))
        assertEquals("I'm", SmartCase.fix("i'm", false, listOf("en")))
        // Polskie jednoliterowe spójniki zostają małe, także przy włączonym angielskim.
        assertEquals("i", SmartCase.fix("i", false, listOf("pl", "en"), "pl"))
        assertEquals("w", SmartCase.fix("w", false, listOf("pl", "en"), "pl"))
        assertEquals("I", SmartCase.fix("i", false, listOf("pl", "en"), "en"))
        assertEquals("I", SmartCase.fix("i", false, listOf("en")))
    }

    @Test
    fun textTransforms() {
        assertEquals("Zazolc gesla jazn", TextTools.stripDiacritics("Zażółć gęślą jaźń"))
        assertEquals("tok 🐱", TextTools.reverse("🐱 kot"))
        assertEquals("ollǝɥ", TextTools.upsideDown("hello"))
        assertEquals("Ala, ma kota.\nI psa", TextTools.tidySpaces("  Ala  ,  ma   kota .  \n I psa "))
    }

    @Test
    fun shortcutTemplates() {
        val d = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 21, 9, 5) }.time
        assertEquals("Dziś 21.10.2026, 09:05" to 0, TextTools.expandTemplate("Dziś {data}, {godzina}", "pl", "", d))
        assertEquals("Link: abc" to 0, TextTools.expandTemplate("Link: {schowek}", "pl", "abc", d))
        val (t, back) = TextTools.expandTemplate("<b>{kursor}</b>", "en", "", d)
        assertEquals("<b></b>", t)
        assertEquals(4, back)
    }

    @Test
    fun unitConversion() {
        assertEquals("3,11 mi", UnitConverter.find("Przebiegłem 5 km", "pl"))
        assertEquals("3.11 mi", UnitConverter.find("I ran 5 km ", "en"))
        assertEquals("68°F", UnitConverter.find("Jest 20°C", "pl"))
        assertEquals("6,61 lb", UnitConverter.find("waży 3 kg", "pl"))
        assertEquals("96.6 km/h", UnitConverter.find("60 mph", "en"))
        assertNull(UnitConverter.find("Mam 5 kotów", "pl"))
        assertNull(UnitConverter.find("o 5 c", "pl"))
        assertNull(UnitConverter.find("A4km", "pl"))
    }

    @Test
    fun missedSpaceGivesTwoWords() {
        Dictionaries.putForTest("xx", WordList.parse("xx", sequenceOf("rzadkie	1", "jak	5000", "dzień	3000", "dobry	2500", "kot	900", "i	9000", "w	8000", "tej	4000", "rozumieć	1500")))
        val langs = listOf("xx")
        assertEquals("jak dzień", Suggest.splitWords("jakdzien", langs).first())
        // Litera z dolnego rzędu zamiast spacji.
        assertEquals("dzień dobry", Suggest.splitWords("dzieńndobry", langs).first())
        assertTrue(Suggest.splitWords("xyzkot", langs).isEmpty())
        // Jednoliterowe słowo i litera zamiast spacji, bez polskich znaków.
        assertEquals("w tej", Suggest.splitWords("wbtej", langs).first())
        assertEquals("i rozumieć", Suggest.splitWords("ibrozumiec", langs).first())
    }

    @Test
    fun missedSpaceWithExtraLetter() {
        Dictionaries.putForTest("yy", WordList.parse("yy", sequenceOf("rzadkie	1", "kim	4000", "jesteś	3500", "jestem	3000")))
        val langs = listOf("yy")
        val prox = Proximity { _, _ -> false }
        assertEquals("kim jesteś", Suggest.splitWords("kikmjestes", langs, prox).first())
    }
}
