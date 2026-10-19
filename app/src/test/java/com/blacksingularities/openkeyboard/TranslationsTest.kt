package com.blacksingularities.openkeyboard

import com.blacksingularities.openkeyboard.i18n.En
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Każdy tekst przekazany do tr("…") ma odpowiednik angielski. */
class TranslationsTest {
    @Test
    fun everyStringIsTranslated() {
        val root = File("src/main/java")
        val pattern = Regex("""(?<![A-Za-z.])tr\(\s*"((?:[^"\\]|\\.)*)"""")
        val missing = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "En.kt" }
            .flatMap { f -> pattern.findAll(f.readText()).map { it.groupValues[1].replace("\\\"", "\"") } }
            .filter { it !in En }
            .toSortedSet()
        assertTrue("Brak tłumaczeń:\n" + missing.joinToString("\n"), missing.isEmpty())
    }
}
