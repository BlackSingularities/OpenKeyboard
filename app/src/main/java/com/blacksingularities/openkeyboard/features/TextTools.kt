package com.blacksingularities.openkeyboard.features

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * Ozdobne style z bloków Unicode (działają w każdej aplikacji, bez czcionek):
 * 𝐁𝐨𝐥𝐝, 𝘐𝘵𝘢𝘭𝘪𝘤, 𝓢𝓬𝓻𝓲𝓹𝓽, 𝕯𝖗𝖆𝖐, 𝔻𝕠𝕦𝕓𝕝𝕖, 𝙼𝚘𝚗𝚘, Ⓒⓘⓡⓒⓛⓔ, Ｗｉｄｅ, ꜱᴍᴀʟʟ, s̶t̶r̶i̶k̶e̶, u̲n̲d̲e̲r̲.
 */
enum class FancyStyle(val label: String) {
    BOLD("𝐁𝐨𝐥𝐝"),
    ITALIC("𝘐𝘵𝘢𝘭𝘪𝘤"),
    BOLD_ITALIC("𝑩𝒐𝒍𝒅"),
    SANS_BOLD("𝗦𝗮𝗻𝘀"),
    SCRIPT("𝓢𝓬𝓻𝓲𝓹𝓽"),
    FRAKTUR("𝕱𝖗𝖆𝖐"),
    DOUBLE("𝔻𝕠𝕦𝕓𝕝𝕖"),
    MONO("𝙼𝚘𝚗𝚘"),
    CIRCLED("Ⓒⓘⓡⓒⓛⓔ"),
    WIDE("Ｗｉｄｅ"),
    SMALL_CAPS("ꜱᴍᴀʟʟ"),
    STRIKE("s̶t̶r̶i̶k̶e̶"),
    UNDERLINE("u̲n̲d̲e̲r̲"),
}

object TextTools {
    private const val SMALL = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ"
    private val doubleUpper = mapOf('C' to "ℂ", 'H' to "ℍ", 'N' to "ℕ", 'P' to "ℙ", 'Q' to "ℚ", 'R' to "ℝ", 'Z' to "ℤ")

    private fun math(c: Char, upper: Int, lower: Int, digit: Int?): String = when (c) {
        in 'A'..'Z' -> String(Character.toChars(upper + (c - 'A')))
        in 'a'..'z' -> String(Character.toChars(lower + (c - 'a')))
        in '0'..'9' -> if (digit != null) String(Character.toChars(digit + (c - '0'))) else c.toString()
        else -> c.toString()
    }

    fun styleChar(c: Char, style: FancyStyle): String = when (style) {
        FancyStyle.BOLD -> math(c, 0x1D400, 0x1D41A, 0x1D7CE)
        FancyStyle.ITALIC -> if (c == 'h') "ℎ" else math(c, 0x1D434, 0x1D44E, null)
        FancyStyle.BOLD_ITALIC -> math(c, 0x1D468, 0x1D482, 0x1D7CE)
        FancyStyle.SANS_BOLD -> math(c, 0x1D5D4, 0x1D5EE, 0x1D7EC)
        FancyStyle.SCRIPT -> math(c, 0x1D4D0, 0x1D4EA, null)
        FancyStyle.FRAKTUR -> math(c, 0x1D56C, 0x1D586, null)
        FancyStyle.DOUBLE -> doubleUpper[c] ?: math(c, 0x1D538, 0x1D552, 0x1D7D8)
        FancyStyle.MONO -> math(c, 0x1D670, 0x1D68A, 0x1D7F6)
        FancyStyle.CIRCLED -> when (c) {
            in 'A'..'Z' -> (0x24B6 + (c - 'A')).toChar().toString()
            in 'a'..'z' -> (0x24D0 + (c - 'a')).toChar().toString()
            '0' -> "⓪"
            in '1'..'9' -> (0x2460 + (c - '1')).toChar().toString()
            else -> c.toString()
        }
        FancyStyle.WIDE -> when (c) {
            in '!'..'~' -> (0xFF01 + (c - '!')).toChar().toString()
            ' ' -> "　"
            else -> c.toString()
        }
        FancyStyle.SMALL_CAPS -> if (c.lowercaseChar() in 'a'..'z') SMALL[c.lowercaseChar() - 'a'].toString() else c.toString()
        FancyStyle.STRIKE -> if (c.isWhitespace()) c.toString() else "$c̶"
        FancyStyle.UNDERLINE -> if (c.isWhitespace()) c.toString() else "$c̲"
    }

    fun style(text: String, style: FancyStyle): String = buildString { for (c in text) append(styleChar(c, style)) }

    /** Wielkość liter: UPPER → lower → Zdanie → Każde Słowo → UPPER… */
    fun cycleCase(text: String): String = when {
        text.any { it.isLetter() } && text == text.uppercase() -> text.lowercase()
        text == text.lowercase() -> sentence(text)
        text == sentence(text) && text.split(' ').size > 1 -> title(text)
        else -> text.uppercase()
    }

    fun sentence(text: String): String {
        val sb = StringBuilder(text.lowercase())
        var cap = true
        for (i in sb.indices) {
            val ch = sb[i]
            if (cap && ch.isLetter()) {
                sb.setCharAt(i, ch.uppercaseChar())
                cap = false
            } else if (ch in ".!?\n") {
                cap = true
            }
        }
        return sb.toString()
    }

    fun title(text: String): String = text.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } }

    fun invert(text: String): String = buildString { for (c in text) append(if (c.isUpperCase()) c.lowercaseChar() else c.uppercaseChar()) }

    fun date(): String = SimpleDateFormat("d.MM.yyyy", Locale.getDefault()).format(Date())
    fun time(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    fun iso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(Date())

    /**
     * Gotowe wstawki daty i czasu w języku pisania: (etykieta, tekst). Etykieta null – pokazuje
     * sam tekst. Nazwy miesięcy i dni w odpowiednim języku („20 października 2026”).
     */
    fun dateInserts(lang: String, now: Date = Date()): List<Pair<String?, String>> {
        val loc = Locale.forLanguageTag(lang)
        fun f(p: String, d: Date = now, l: Locale = loc) = SimpleDateFormat(p, l).format(d)
        val cal = Calendar.getInstance(loc).apply { time = now }
        val tomorrow = Calendar.getInstance(loc).apply { time = now; add(Calendar.DAY_OF_MONTH, 1) }.time
        val yesterday = Calendar.getInstance(loc).apply { time = now; add(Calendar.DAY_OF_MONTH, -1) }.time
        return listOf(
            null to f("d.MM.yyyy"),
            null to f("d MMMM yyyy"),
            null to f("EEEE, d MMMM"),
            null to f("HH:mm"),
            null to f("d.MM.yyyy, HH:mm"),
            null to f("yyyy-MM-dd", l = Locale.US),
            null to f("MMMM yyyy").replaceFirstChar { it.uppercase(loc) },
            "+1" to f("d.MM.yyyy", tomorrow),
            "−1" to f("d.MM.yyyy", yesterday),
            "#" to cal.get(Calendar.WEEK_OF_YEAR).toString(),
            "ISO" to iso(),
            "Unix" to (now.time / 1000).toString(),
        )
    }

    fun uuid(): String = UUID.randomUUID().toString()

    fun randomNumber(): String = Random.nextInt(1, 101).toString()


    fun countWords(text: String): Int = Regex("""[\p{L}\p{N}'’-]+""").findAll(text).count()
}
