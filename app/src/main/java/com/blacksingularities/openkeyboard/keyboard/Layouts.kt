package com.blacksingularities.openkeyboard.keyboard

import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Lang
import com.blacksingularities.openkeyboard.data.Languages

enum class KeyType { CHAR, TEXT, SHIFT, DELETE, ENTER, SPACE, SYMBOLS, SYMBOLS2, ALPHA, LANGUAGE, EMOJI, NUMPAD }

/**
 * Klawisz: [text] – wstawiany tekst (dla CHAR/TEXT), [label] – napis na klawiszu,
 * [popup] – znaki pod przytrzymaniem, [hint] – mały znak w rogu, [width] – szerokość w jednostkach.
 */
data class Key(
    val type: KeyType,
    val text: String = "",
    val label: String = text,
    val popup: List<String> = emptyList(),
    val hint: String? = null,
    val width: Float = 1f,
)

data class KeyboardLayout(val rows: List<List<Key>>, val units: Float, val mode: KeyboardMode)

enum class KeyboardMode { ALPHA, SYMBOLS, SYMBOLS2, NUMBER, PHONE }

/** Rodzaj pola tekstowego – zmienia kilka klawiszy (np. „@” w polu e-mail). */
enum class FieldKind { TEXT, EMAIL, URL, NUMBER, PHONE, PASSWORD, SEARCH }

private val letterRows: Map<String, List<String>> = mapOf(
    "qwerty" to listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
    "qwerty_es" to listOf("qwertyuiop", "asdfghjklñ", "zxcvbnm"),
    "qwertz" to listOf("qwertzuiop", "asdfghjkl", "yxcvbnm"),
    "qwertz_de" to listOf("qwertzuiopü", "asdfghjklöä", "yxcvbnm"),
    "azerty" to listOf("azertyuiop", "qsdfghjklm", "wxcvbn'"),
    "colemak" to listOf("qwfpgjluy'", "arstdhneio", "zxcvbkm"),
    "dvorak" to listOf("',.pyfgcrl", "aoeuidhtns", ";qjkxbmwvz"),
    "nordic_sv" to listOf("qwertyuiopå", "asdfghjklöä", "zxcvbnm"),
    "nordic_da" to listOf("qwertyuiopå", "asdfghjklæø", "zxcvbnm"),
    "ru" to listOf("йцукенгшщзх", "фывапролджэ", "ячсмитьбю"),
    "uk" to listOf("йцукенгшщзх", "фівапролджє", "ячсмитьбю"),
    "be" to listOf("йцукенгшўзх", "фывапролджэ", "ячсмітьбю"),
    "el" to listOf("ςερτυθιοπ", "ασδφγηξκλ", "ζχψωβνμ"),
    "tr" to listOf("qwertyuıopğü", "asdfghjklşi", "zxcvbnmöç"),
)

private val superscripts = mapOf(
    '1' to "¹½⅓¼⅛", '2' to "²⅔", '3' to "³¾⅜", '4' to "⁴", '5' to "⁵⅝", '6' to "⁶", '7' to "⁷⅞", '8' to "⁸", '9' to "⁹", '0' to "⁰ⁿ∅",
)

private val symbolPopups = mapOf(
    "-" to "–—_·", "+" to "±", "(" to "[{<", ")" to "]}>", "/" to "\\", "&" to "§", "#" to "№",
    "*" to "★†‡", "\"" to "„“”«»", "'" to "‚‘’‹›", "!" to "¡", "?" to "¿", "%" to "‰", "=" to "≠≈∞",
    "$" to "€£¥¢₽₹₩", "." to "…", "<" to "≤«", ">" to "≥»",
)

private fun symbolKey(s: String) = Key(KeyType.CHAR, s, popup = symbolPopups[s]?.map { it.toString() } ?: emptyList())

/** Okres wpisany kropką: znaki interpunkcji pod przytrzymaniem kropki. */
private val periodPopup = listOf(",", "?", "!", "'", "\"", ":", ";", "-", "(", ")", "/", "@", "&", "#", "%", "…")

object Layouts {

    fun build(
        mode: KeyboardMode,
        lang: Lang,
        settings: KeyboardSettings,
        field: FieldKind,
        languageCount: Int,
    ): KeyboardLayout = when (mode) {
        KeyboardMode.ALPHA -> alpha(lang, settings, field, languageCount)
        KeyboardMode.SYMBOLS -> symbols(lang, settings, field, second = false)
        KeyboardMode.SYMBOLS2 -> symbols(lang, settings, field, second = true)
        KeyboardMode.NUMBER -> numpad(phone = false)
        KeyboardMode.PHONE -> numpad(phone = true)
    }

    private fun alpha(lang: Lang, s: KeyboardSettings, field: FieldKind, languageCount: Int): KeyboardLayout {
        val layoutId = s.layoutOverride[lang.code] ?: lang.layout
        val rows = letterRows[layoutId] ?: letterRows.getValue("qwerty")
        val units = maxOf(rows[0].length, rows[1].length, rows[2].length + 3, 10).toFloat()
        val out = mutableListOf<List<Key>>()
        if (s.numberRow) out += "1234567890".map { c -> Key(KeyType.CHAR, c.toString(), popup = superscripts[c]?.map { it.toString() } ?: emptyList()) }
        rows.forEachIndexed { r, letters ->
            val row = letters.mapIndexed { i, c ->
                val digit = if (r == 0 && !s.numberRow && i < 10) "1234567890"[i].toString() else null
                letterKey(c, lang, digit, s.keyHints)
            }
            if (r == 2) {
                val side = ((units - row.size) / 2f).coerceAtLeast(1f)
                out += listOf(Key(KeyType.SHIFT, width = side)) + row + Key(KeyType.DELETE, width = side)
            } else {
                out += row
            }
        }
        out += bottomRow(s, field, languageCount, units, KeyType.SYMBOLS, "?123", lang)
        return KeyboardLayout(out, units, KeyboardMode.ALPHA)
    }

    private fun letterKey(c: Char, lang: Lang, digit: String?, hints: Boolean): Key {
        val primary = lang.accents[c].orEmpty()
        val generic = Languages.genericAccents[c].orEmpty().filter { it !in primary }
        val popup = buildList {
            if (digit != null) add(digit)
            primary.forEach { add(it.toString()) }
            // Ogólne znaki tylko dla alfabetu łacińskiego – w cyrylicy i greckim byłyby szumem.
            if (c in 'a'..'z') generic.forEach { add(it.toString()) }
        }
        val hint = when {
            !hints -> null
            digit != null -> digit
            primary.isNotEmpty() -> primary.first().toString()
            else -> null
        }
        return Key(KeyType.CHAR, c.toString(), popup = popup, hint = hint)
    }

    private fun bottomRow(
        s: KeyboardSettings,
        field: FieldKind,
        languageCount: Int,
        units: Float,
        switchType: KeyType,
        switchLabel: String,
        lang: Lang,
    ): List<Key> {
        val row = mutableListOf<Key>()
        row += Key(switchType, label = switchLabel, width = 1.5f)
        when (field) {
            FieldKind.EMAIL -> row += Key(KeyType.CHAR, "@")
            FieldKind.URL -> row += Key(KeyType.CHAR, "/")
            else -> if (s.commaKey) row += Key(KeyType.CHAR, ",", popup = listOf(";", ":", "„", "”"))
        }
        if (s.languageKey && languageCount > 1) row += Key(KeyType.LANGUAGE)
        if (s.emojiKey) row += Key(KeyType.EMOJI)
        val tail = mutableListOf<Key>()
        when (field) {
            FieldKind.EMAIL, FieldKind.URL -> tail += Key(KeyType.TEXT, ".com", label = ".com", popup = listOf(".pl", ".eu", ".org", ".net", ".io", ".de", ".uk"))
            else -> {}
        }
        tail += Key(KeyType.CHAR, ".", popup = periodPopup)
        tail += Key(KeyType.ENTER, width = 1.5f)
        val used = row.sumOf { it.width.toDouble() } + tail.sumOf { it.width.toDouble() }
        val space = (units - used).toFloat().coerceAtLeast(2f)
        return row + Key(KeyType.SPACE, " ", label = lang.native, width = space) + tail
    }

    private fun symbols(lang: Lang, s: KeyboardSettings, field: FieldKind, second: Boolean): KeyboardLayout {
        val units = 10f
        val rows = mutableListOf<List<Key>>()
        if (!second) {
            rows += "1234567890".map { c -> Key(KeyType.CHAR, c.toString(), popup = superscripts[c]?.map { it.toString() } ?: emptyList()) }
            rows += listOf("@", "#", lang.currency, "_", "&", "-", "+", "(", ")", "/").map { k ->
                if (k == lang.currency) Key(KeyType.TEXT, k, popup = listOf("€", "$", "£", "¥", "zł", "₽", "₴", "¢").filter { it != k }) else symbolKey(k)
            }
            rows += listOf(Key(KeyType.SYMBOLS2, label = "=\\<", width = 1.5f)) +
                listOf("*", "\"", "'", ":", ";", "!", "?").map(::symbolKey) +
                Key(KeyType.DELETE, width = 1.5f)
        } else {
            rows += listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆").map(::symbolKey)
            rows += listOf("£", "¥", "$", "¢", "^", "°", "=", "{", "}", "\\").map(::symbolKey)
            rows += listOf(Key(KeyType.SYMBOLS, label = "?123", width = 1.5f)) +
                listOf("%", "©", "®", "™", "✓", "[", "]").map(::symbolKey) +
                Key(KeyType.DELETE, width = 1.5f)
        }
        val bottom = bottomRow(s.copy(emojiKey = true, languageKey = false), field, 1, units, KeyType.ALPHA, "ABC", lang)
            .map { if (it.type == KeyType.SPACE) it.copy(label = "") else it }
        rows += bottom
        return KeyboardLayout(rows, units, if (second) KeyboardMode.SYMBOLS2 else KeyboardMode.SYMBOLS)
    }

    private fun numpad(phone: Boolean): KeyboardLayout {
        fun d(c: String, sub: String = "") = Key(KeyType.CHAR, c, popup = emptyList(), hint = sub.ifEmpty { null })
        val side: List<String> = if (phone) listOf("+", "*", "#", "(") else listOf("-", "+", ".", ",")
        val rows = listOf(
            listOf(d("1"), d("2", if (phone) "ABC" else ""), d("3", if (phone) "DEF" else ""), symbolKey(side[0])),
            listOf(d("4", if (phone) "GHI" else ""), d("5", if (phone) "JKL" else ""), d("6", if (phone) "MNO" else ""), symbolKey(side[1])),
            listOf(d("7", if (phone) "PQRS" else ""), d("8", if (phone) "TUV" else ""), d("9", if (phone) "WXYZ" else ""), Key(KeyType.DELETE)),
            listOf(
                if (phone) symbolKey(side[2]) else Key(KeyType.ALPHA, label = "ABC"),
                d("0", if (phone) "+" else ""),
                if (phone) symbolKey(side[3]) else Key(KeyType.SPACE, " ", label = "␣"),
                Key(KeyType.ENTER),
            ),
        ).map { r -> r.map { it.copy(width = 2.5f) } }
        return KeyboardLayout(rows, 10f, if (phone) KeyboardMode.PHONE else KeyboardMode.NUMBER)
    }
}
