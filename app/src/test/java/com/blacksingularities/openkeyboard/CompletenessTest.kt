package com.blacksingularities.openkeyboard

import com.blacksingularities.openkeyboard.data.SplitMode
import com.blacksingularities.openkeyboard.data.shouldSplit
import com.blacksingularities.openkeyboard.keyboard.validCustomRows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Własny układ, dzielenie na szerokim ekranie. */
class CompletenessTest {
    @Test
    fun customRowsValidation() {
        assertTrue(validCustomRows(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")))
        assertFalse(validCustomRows(listOf("qwerty", "asdf")))
        assertFalse(validCustomRows(listOf("qwq", "asd", "zxc")))
        assertFalse(validCustomRows(listOf("q w", "asd", "zxc")))
        assertFalse(validCustomRows(listOf("", "asd", "zxc")))
        assertFalse(validCustomRows(listOf("abcdefghijklm", "nop", "rst")))
    }

    @Test
    fun wideSplit() {
        assertFalse(shouldSplit(SplitMode.WIDE, landscape = false, screenWidthDp = 411))
        assertTrue(shouldSplit(SplitMode.WIDE, landscape = false, screenWidthDp = 673))
        assertTrue(shouldSplit(SplitMode.WIDE, landscape = true, screenWidthDp = 411))
        assertFalse(shouldSplit(SplitMode.LANDSCAPE, landscape = false, screenWidthDp = 800))
        assertFalse(shouldSplit(SplitMode.OFF, landscape = true, screenWidthDp = 1200))
    }
}
