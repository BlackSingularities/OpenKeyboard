package com.blacksingularities.openkeyboard

import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Languages
import com.blacksingularities.openkeyboard.dict.Proximity
import com.blacksingularities.openkeyboard.dict.Suggest
import com.blacksingularities.openkeyboard.dict.WordList
import com.blacksingularities.openkeyboard.dict.normalizeKey
import com.blacksingularities.openkeyboard.features.Calculator
import com.blacksingularities.openkeyboard.keyboard.FieldKind
import com.blacksingularities.openkeyboard.keyboard.KeyType
import com.blacksingularities.openkeyboard.keyboard.KeyboardMode
import com.blacksingularities.openkeyboard.keyboard.Layouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    private val none = Proximity { _, _ -> false }
    private val qwertyNear = Proximity { a, b -> setOf(a, b) in listOf(setOf('q', 'w'), setOf('e', 'r'), setOf('o', 'p'), setOf('a', 's')) }

    @Test
    fun normalizesDiacriticsAndApostrophes() {
        assertEquals("zolw", normalizeKey("żółw"))
        assertEquals("dont", normalizeKey("don't"))
        assertEquals("strasse", normalizeKey("Straße"))
        assertEquals("lodz", normalizeKey("Łódź"))
        assertEquals("ежик", normalizeKey("ёжик"))
        assertEquals("йод", normalizeKey("йод"))
    }

    @Test
    fun distanceRewardsCommonTypos() {
        assertEquals(0f, Suggest.distance("kot", "kot", 2f, none))
        // Przestawienie liter jest tańsze niż dwie zamiany.
        assertTrue(Suggest.distance("teh", "the", 2f, none) < 1f)
        // Sąsiedni klawisz tańszy niż dowolny.
        assertTrue(Suggest.distance("wiele", "qiele", 2f, qwertyNear) < Suggest.distance("wiele", "ziele", 2f, qwertyNear))
        // Podwojona litera.
        assertTrue(Suggest.distance("helllo", "hello", 2f, none) <= 0.5f)
        // Za daleko – przerwane.
        assertEquals(Float.MAX_VALUE, Suggest.distance("abc", "xyzuvw", 1f, none))
    }

    @Test
    fun wordListFindsPrefixesAndKeys() {
        val wl = WordList.parse("pl", sequenceOf("żółw\t50", "żółty\t900", "żart\t300", "kot\t1000", "kotek\t200"))
        assertEquals("kot", wl.words[0])
        val prefix = wl.withPrefix("zol", 5).map { wl.words[it] }
        assertEquals(listOf("żółty", "żółw"), prefix)
        assertEquals("żółw", wl.words[wl.bestForKey("zolw")])
        assertTrue(wl.contains("Kot"))
    }

    @Test
    fun appliesTypedCase() {
        assertEquals("Który", Suggest.applyCase("Kto", "który"))
        assertEquals("NIGDY", Suggest.applyCase("NI", "nigdy"))
        assertEquals("dom", Suggest.applyCase("do", "dom"))
    }

    @Test
    fun calculator() {
        assertEquals("87", Calculator.evaluate("12*7+3"))
        assertEquals("25", Calculator.evaluate("(2+3)^2"))
        assertEquals("3.5", Calculator.evaluate("7/2"))
        assertEquals("1.5", Calculator.evaluate("1,5*1"))
        assertEquals("12*7", Calculator.findExpression("Razem 12*7"))
        assertEquals("12*7", Calculator.findExpression("12*7="))
        assertNull(Calculator.findExpression("dnia 12-05"))
        assertNull(Calculator.findExpression("tekst bez liczb"))
    }

    @Test
    fun layoutRowsFitTheGrid() {
        for (lang in Languages.all) {
            for (numberRow in listOf(false, true)) {
                val s = KeyboardSettings(numberRow = numberRow, languages = listOf(lang.code, "en"))
                val l = Layouts.build(KeyboardMode.ALPHA, lang, s, FieldKind.TEXT, 2)
                for (row in l.rows) {
                    val w = row.sumOf { it.width.toDouble() }
                    assertTrue("${lang.code}: rząd szerszy niż siatka ($w > ${l.units})", w <= l.units + 0.01)
                }
                assertTrue(l.rows.last().any { it.type == KeyType.SPACE })
                assertEquals(if (numberRow) 5 else 4, l.rows.size)
            }
        }
        for (mode in KeyboardMode.entries) {
            val l = Layouts.build(mode, Languages.get("pl"), KeyboardSettings(), FieldKind.EMAIL, 2)
            assertTrue(l.rows.isNotEmpty())
        }
    }

    @Test
    fun polishAccentsOnLongPress() {
        val l = Layouts.build(KeyboardMode.ALPHA, Languages.get("pl"), KeyboardSettings(), FieldKind.TEXT, 1)
        val a = l.rows.flatten().first { it.text == "a" }
        assertEquals("ą", a.popup.first { it != "1" && it.length == 1 && !it[0].isDigit() })
        val z = l.rows.flatten().first { it.text == "z" }
        assertEquals(listOf("ż", "ź"), z.popup.take(2))
    }
}
