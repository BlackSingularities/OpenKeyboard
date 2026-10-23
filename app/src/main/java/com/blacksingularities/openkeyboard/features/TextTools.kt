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

/**
 * Inteligentna wielkość liter przy zatwierdzaniu słowa: wielka litera na początku zdania (także
 * w podpowiedziach), „HEllo” → „Hello” (przytrzymany Shift), angielskie dni, miesiące i „I'm”.
 */
object SmartCase {
    private val english = listOf(
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "january", "february", "march", "april", "june", "july", "august", "september", "october", "november", "december",
        "english", "polish", "american", "british", "european", "christmas", "easter",
    ).associateWith { it.replaceFirstChar { c -> c.uppercaseChar() } } + mapOf(
        "i" to "I", "i'm" to "I'm", "i’m" to "I’m", "i've" to "I've", "i'll" to "I'll", "i'd" to "I'd",
    )

    /** Jednoliterowe spójniki i przyimki pisane małą literą („i”, „w”, „z”…) – nie zamieniać na angielskie „I”. */
    private val lowercaseShort = mapOf(
        "pl" to setOf("i", "a", "o", "u", "w", "z"),
        "cs" to setOf("i", "a", "o", "u", "v", "k", "s", "z"),
        "sk" to setOf("i", "a", "o", "u", "v", "k", "s", "z"),
        "hr" to setOf("i", "a", "o", "u", "k", "s"),
        "sr" to setOf("i", "a", "o", "u", "k", "s"),
        "sl" to setOf("i", "a", "o", "u", "k", "s", "v", "z"),
        "it" to setOf("i", "e", "o", "a"),
    )

    /** [word] to małe słowo w którymś z [langs] poza angielskim (np. polskie „i”). */
    fun isLowercaseShort(word: String, langs: Collection<String>): Boolean =
        langs.any { it != "en" && word in lowercaseShort[it].orEmpty() }

    /**
     * Słowo po poprawce wielkości liter. [sentenceStart] – słowo zaczyna zdanie.
     * [primary] – język, w którym właśnie się pisze: angielskie „i” → „I” tylko, gdy to angielski
     * albo żaden z włączonych języków nie ma takiego słowa.
     */
    fun fix(word: String, sentenceStart: Boolean, langs: Collection<String>, primary: String? = null): String {
        var w = word
        // Dwie wielkie na początku, reszta małe – palec za długo na Shift.
        if (w.length >= 3 && w[0].isUpperCase() && w[1].isUpperCase() && w.drop(2).all { !it.isLetter() || it.isLowerCase() } && w.drop(2).any { it.isLetter() }) {
            w = w[0] + w.substring(1).lowercase()
        }
        if ("en" in langs && (primary == "en" || !isLowercaseShort(w, langs))) english[w]?.let { w = it }
        if (sentenceStart && w.firstOrNull()?.isLowerCase() == true) w = w.replaceFirstChar { it.uppercaseChar() }
        return w
    }

    /** Czy tekst przed słowem to początek zdania (puste pole, po . ! ? … albo nowej linii). */
    fun isSentenceStart(textBefore: String): Boolean {
        val t = textBefore.trimEnd(' ', '\t', '\u00A0')
        if (t.isEmpty() || t.endsWith("\n")) return true
        val trimmed = t.trimEnd('"', '”', '»', ')', '\'')
        val last = trimmed.lastOrNull() ?: return true
        if (last !in ".!?…") return false
        // „np.”, „itd.”, „e.g.” – skróty nie kończą zdania; tak samo liczby („3.”).
        val lastWord = Regex("""([\p{L}.]+)\.$""").find(trimmed)?.groupValues?.get(1)?.lowercase()
        return lastWord == null || lastWord !in ABBREVIATIONS
    }

    private val ABBREVIATIONS = setOf("np", "itd", "itp", "tzn", "tj", "m.in", "ok", "godz", "ul", "al", "nr", "dr", "prof", "mgr", "inż", "św", "tel", "e.g", "i.e", "mr", "mrs", "ms", "vs", "etc", "st")
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

    /** Shift w trakcie pisania słowa: abc → Abc → ABC → abc. */
    fun cycleWordCase(word: String): String {
        val lower = word.lowercase()
        val cap = lower.replaceFirstChar { it.uppercaseChar() }
        val up = word.uppercase()
        return when {
            word == up && word != lower -> lower
            word == cap && word.length > 1 -> up
            else -> cap
        }
    }

    /** Bez znaków diakrytycznych: „Zażółć gęślą” → „Zazolc gesla”. */
    fun stripDiacritics(text: String): String {
        val special = mapOf('ł' to "l", 'Ł' to "L", 'ø' to "o", 'Ø' to "O", 'đ' to "d", 'Đ' to "D", 'ß' to "ss", 'æ' to "ae", 'Æ' to "AE", 'œ' to "oe")
        val sb = StringBuilder()
        for (c in text) sb.append(special[c] ?: c.toString())
        return java.text.Normalizer.normalize(sb, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    }

    /** Tekst od tyłu (z zachowaniem emoji i znaków złożonych). */
    fun reverse(text: String): String {
        val it = java.text.BreakIterator.getCharacterInstance()
        it.setText(text)
        val parts = ArrayList<String>()
        var start = it.first()
        var end = it.next()
        while (end != java.text.BreakIterator.DONE) {
            parts += text.substring(start, end)
            start = end
            end = it.next()
        }
        return parts.asReversed().joinToString("")
    }

    private const val FLIP_FROM = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,!?'\"()[]{}<>_&"
    private const val FLIP_TO = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀ꓭƆꓷƎℲ⅁HIſꓘ⅂WNOԀΌꓤSꓕՈΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86˙'¡¿,„)(][}{><‾⅋"

    /** Do góry nogami: „hello” → „ollǝɥ”. */
    fun upsideDown(text: String): String = buildString {
        for (c in stripDiacritics(text).reversed()) {
            val i = FLIP_FROM.indexOf(c)
            append(if (i >= 0 && i < FLIP_TO.length) FLIP_TO[i] else c)
        }
    }

    /** Porządek w odstępach: pojedyncze spacje, bez spacji przed , . ! ? i na końcach linii. */
    fun tidySpaces(text: String): String = text
        .replace(Regex("[ \\t\\u00A0]+"), " ")
        .replace(Regex(" ([,.!?;:])"), "$1")
        .lines().joinToString("\n") { it.trim() }

    /**
     * Zmienne w skrótach tekstowych: {data}/{date}, {godzina}/{time}, {dzień}/{day}, {schowek}/{clipboard},
     * {kursor}/{cursor} – miejsce kursora po rozwinięciu. Zwraca tekst i liczbę znaków, o ile cofnąć kursor
     * (0 – kursor na końcu).
     */
    fun expandTemplate(template: String, lang: String, clipboard: String, now: Date = Date()): Pair<String, Int> {
        val loc = Locale.forLanguageTag(lang)
        var out = template
        fun sub(names: List<String>, value: () -> String) {
            for (n in names) if (out.contains("{$n}", ignoreCase = true)) out = out.replace("{$n}", value(), ignoreCase = true)
        }
        sub(listOf("data", "date")) { SimpleDateFormat("d.MM.yyyy", loc).format(now) }
        sub(listOf("godzina", "time")) { SimpleDateFormat("HH:mm", loc).format(now) }
        sub(listOf("dzień", "dzien", "day")) { SimpleDateFormat("EEEE", loc).format(now) }
        sub(listOf("schowek", "clipboard")) { clipboard }
        val marker = Regex("\\{(kursor|cursor)\\}", RegexOption.IGNORE_CASE)
        val m = marker.find(out) ?: return out to 0
        val result = out.removeRange(m.range).replace(marker, "")
        return result to (result.length - m.range.first)
    }

    fun invert(text: String): String =buildString { for (c in text) append(if (c.isUpperCase()) c.lowercaseChar() else c.uppercaseChar()) }

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
