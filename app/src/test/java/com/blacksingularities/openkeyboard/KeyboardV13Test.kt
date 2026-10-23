package com.blacksingularities.openkeyboard

import com.blacksingularities.openkeyboard.data.EmojiKey
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Languages
import com.blacksingularities.openkeyboard.data.Prefs
import com.blacksingularities.openkeyboard.data.ToolbarItem
import com.blacksingularities.openkeyboard.dict.WordList
import com.blacksingularities.openkeyboard.features.TextTools
import com.blacksingularities.openkeyboard.keyboard.FieldKind
import com.blacksingularities.openkeyboard.keyboard.KeyType
import com.blacksingularities.openkeyboard.keyboard.KeyboardMode
import com.blacksingularities.openkeyboard.keyboard.Layouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

class KeyboardV13Test {
    private val pl = Languages.get("pl")

    @Test
    fun oldSettingsWithTranslatorStillLoad() {
        val raw = """{"toolbar":["TRANSLATE","CLIPBOARD","EMOJI","TRANSLATE","SETTINGS"],"translateTarget":"de","keyHeight":120}"""
        val s = Prefs.decode(raw)
        assertEquals(listOf(ToolbarItem.CLIPBOARD, ToolbarItem.EMOJI, ToolbarItem.SETTINGS), s.toolbar)
        assertEquals(120, s.keyHeight)
        assertEquals(listOf(ToolbarItem.SETTINGS), Prefs.decode("""{"toolbar":["SETTINGS","TRANSLATE"]}""").toolbar)
    }

    @Test
    fun symbolsOnLettersLikeSwiftKey() {
        val l = Layouts.build(KeyboardMode.ALPHA, pl, KeyboardSettings(symbolKeys = true), FieldKind.TEXT, 1)
        val keys = l.rows.flatten().filter { it.type == KeyType.CHAR }.associateBy { it.text }
        // Pod palcem najpierw cyfra, potem znak języka, dopiero dalej symbol.
        assertEquals(listOf("1", "%"), keys.getValue("q").popup.take(2))
        assertEquals("%", keys.getValue("q").hint)
        assertEquals("1", keys.getValue("q").hint2)
        assertEquals("ą", keys.getValue("a").popup.first())
        assertTrue("@" in keys.getValue("a").popup)
        assertEquals("ł", keys.getValue("l").popup.first())
        assertTrue("ą" in keys.getValue("a").popup)
        assertEquals("ą", keys.getValue("a").hint2)
        assertEquals("/", keys.getValue("m").popup.first())
        // Z rzędem cyfr cyfr nie ma pod literami.
        val withRow = Layouts.build(KeyboardMode.ALPHA, pl, KeyboardSettings(symbolKeys = true, numberRow = true), FieldKind.TEXT, 1)
        assertFalse(withRow.rows[1].first { it.text == "q" }.popup.contains("1"))
    }

    @Test
    fun emojiKeyFollowsSettingAndField() {
        fun bottom(mode: EmojiKey, field: FieldKind) =
            Layouts.build(KeyboardMode.ALPHA, pl, KeyboardSettings(emojiKeyMode = mode, languages = listOf("pl", "en")), field, 2).rows.last()
        val sep = bottom(EmojiKey.SEPARATE, FieldKind.TEXT)
        assertEquals(KeyType.EMOJI, sep[1].type)
        assertEquals(",", sep[2].text)
        val combo = bottom(EmojiKey.COMBINED, FieldKind.TEXT)
        assertTrue(combo[0].combo)
        assertTrue(combo.none { it.type == KeyType.EMOJI })
        // W adresie e-mail emoji nie ma sensu.
        assertTrue(bottom(EmojiKey.SEPARATE, FieldKind.EMAIL).none { it.type == KeyType.EMOJI })
        assertFalse(bottom(EmojiKey.COMBINED, FieldKind.URL)[0].combo)
        for (row in listOf(sep, combo)) assertTrue(row.sumOf { it.width.toDouble() } <= 10.01)
    }

    @Test
    fun passwordFieldGetsDigits() {
        val l = Layouts.build(KeyboardMode.ALPHA, pl, KeyboardSettings(), FieldKind.PASSWORD, 1)
        assertEquals(5, l.rows.size)
        assertEquals("1", l.rows[0][0].text)
    }

    @Test
    fun dateInsertsUseWritingLanguage() {
        val d = java.util.GregorianCalendar(2026, 9, 20, 14, 5).time
        val values = TextTools.dateInserts("pl", d).map { it.second }
        assertTrue(values.toString(), "20.10.2026" in values)
        assertTrue(values.toString(), "20 października 2026" in values)
        assertTrue(values.toString(), "14:05" in values)
        assertTrue(values.toString(), "21.10.2026" in values)
    }

    @Test
    fun bundledDictionariesAreRich() {
        for ((code, min, words) in listOf(
            Triple("pl", 150_000, listOf("przyjdźcie", "zrobiłabym", "najważniejszych", "żółw", "się")),
            Triple("en", 100_000, listOf("I", "I'm", "Monday", "remortgage", "unbelievable")),
        )) {
            val f = File("src/main/assets/dict/$code.dict")
            val wl = GZIPInputStream(f.inputStream().buffered()).bufferedReader().useLines { WordList.parse(code, it) }
            assertTrue("$code: ${wl.size}", wl.size >= min)
            for (w in words) assertTrue("$code: brak „$w”", wl.contains(w))
        }
    }
}
