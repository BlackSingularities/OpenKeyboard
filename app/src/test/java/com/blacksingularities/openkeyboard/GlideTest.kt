package com.blacksingularities.openkeyboard

import com.blacksingularities.openkeyboard.dict.Dictionaries
import com.blacksingularities.openkeyboard.dict.Glide
import com.blacksingularities.openkeyboard.dict.Pt
import com.blacksingularities.openkeyboard.dict.WordList
import com.blacksingularities.openkeyboard.features.FancyStyle
import com.blacksingularities.openkeyboard.features.TextTools
import org.junit.Assert.assertEquals
import org.junit.Test

class GlideTest {
    /** Środki klawiszy QWERTY w jednostkach klawisza. */
    private val centers: Map<Char, Pt> = buildMap {
        "qwertyuiop".forEachIndexed { i, c -> put(c, Pt(i + 0.5f, 0.5f)) }
        "asdfghjkl".forEachIndexed { i, c -> put(c, Pt(i + 1f, 1.5f)) }
        "zxcvbnm".forEachIndexed { i, c -> put(c, Pt(i + 2f, 2.5f)) }
    }

    private fun swipe(word: String): List<Pt> {
        // Ścieżka z lekkim drżeniem palca, gęsto próbkowana.
        val pts = word.map { centers.getValue(it) }
        val out = ArrayList<Pt>()
        for (i in 1 until pts.size) for (k in 0..10) {
            val t = k / 10f
            out += Pt(pts[i - 1].x + (pts[i].x - pts[i - 1].x) * t + 0.08f * (k % 2), pts[i - 1].y + (pts[i].y - pts[i - 1].y) * t)
        }
        return out
    }

    @Test
    fun decodesSwipedWords() {
        Dictionaries.putForTest(
            "pl",
            WordList.parse("pl", sequenceOf("kot\t900", "kit\t200", "kat\t300", "lot\t400", "pies\t800", "piwo\t600", "dom\t1000", "dym\t100", "żółw\t150")),
        )
        assertEquals("kot", Glide.decode(swipe("kot"), centers, 1f, listOf("pl"), "pl", null).first())
        assertEquals("pies", Glide.decode(swipe("pies"), centers, 1f, listOf("pl"), "pl", null).first())
        assertEquals("dom", Glide.decode(swipe("dom"), centers, 1f, listOf("pl"), "pl", null).first())
        // Polskie znaki bez przytrzymania: ścieżka z-o-l-w daje „żółw”.
        assertEquals("żółw", Glide.decode(swipe("zolw"), centers, 1f, listOf("pl"), "pl", null).first())
    }

    @Test
    fun fancyStylesAndCase() {
        assertEquals("𝐀𝐛𝟏", TextTools.style("Ab1", FancyStyle.BOLD))
        assertEquals("ⓐⒷ①", TextTools.style("aB1", FancyStyle.CIRCLED))
        assertEquals("ℂ𝕠", TextTools.style("Co", FancyStyle.DOUBLE))
        assertEquals("ᴋᴏᴛ", TextTools.style("kot", FancyStyle.SMALL_CAPS))
        assertEquals("ALA", TextTools.cycleCase("Ala"))
        assertEquals("ala", TextTools.cycleCase("ALA"))
        assertEquals("Ala ma kota. Kot ma", TextTools.sentence("ala MA kota. kot ma"))
        assertEquals("Ala Ma Kota", TextTools.title("ala ma kota"))
        assertEquals(3, TextTools.countWords("Ala ma kota."))
    }
}
