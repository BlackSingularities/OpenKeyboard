package com.blacksingularities.openkeyboard.keyboard

import com.blacksingularities.openkeyboard.data.EmojiKey
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Lang
import com.blacksingularities.openkeyboard.data.Languages

enum class KeyType { CHAR, TEXT, SHIFT, DELETE, ENTER, SPACE, SYMBOLS, SYMBOLS2, ALPHA, LANGUAGE, EMOJI, NUMPAD }

/**
 * Klawisz: [text] – wstawiany tekst (dla CHAR/TEXT), [label] – napis na klawiszu,
 * [popup] – znaki pod przytrzymaniem, [hint] – mały znak w prawym rogu, [hint2] – drugi znak
 * w lewym rogu (cyfra przy symbolach), [width] – szerokość w jednostkach, [combo] – klawisz
 * „?123” połączony z emoji (przytrzymanie otwiera emoji).
 */
data class Key(
    val type: KeyType,
    val text: String = "",
    val label: String = text,
    val popup: List<String> = emptyList(),
    val hint: String? = null,
    val width: Float = 1f,
    val hint2: String? = null,
    val combo: Boolean = false,
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

/**
 * Symbole przypisane do liter (jak w SwiftKey) – wg pozycji w rzędzie, więc działają w każdym
 * układzie: Q %, W ^, E ~, R |, T [, Y ], U <, I >, O {, P }, A @, S #, D &, F *, G -, H +, J =,
 * K (, L ), Z _, X $, C ", V ', B :, N ;, M /.
 */
val letterSymbols: List<String> = listOf("%^~|[]<>{}€£", "@#&*-+=()\\'", "_$\"':;/!?")

private val superscripts = mapOf(
    '1' to "¹½⅓¼⅛", '2' to "²⅔", '3' to "³¾⅜", '4' to "⁴", '5' to "⁵⅝", '6' to "⁶", '7' to "⁷⅞", '8' to "⁸", '9' to "⁹", '0' to "⁰ⁿ∅",
)

private val symbolPopups = mapOf(
    "-" to "–—_·", "+" to "±", "(" to "[{<", ")" to "]}>", "/" to "\\", "&" to "§", "#" to "№",
    "*" to "★†‡", "\"" to "„“”«»", "'" to "‚‘’‹›", "!" to "¡", "?" to "¿", "%" to "‰", "=" to "≠≈∞",
    "$" to "€£¥¢₽₹₩", "." to "…", "<" to "≤«", ">" to "≥»", "@" to "©®", "~" to "≈", "^" to "°",
)

private fun symbolKey(s: String) = Key(KeyType.CHAR, s, popup = symbolPopups[s]?.map { it.toString() } ?: emptyList())

/** Okres wpisany kropką: znaki interpunkcji pod przytrzymaniem kropki. */
private val periodPopup = listOf("?", "!", ",", "'", "\"", ":", ";", "-", "(", ")", "/", "@", "&", "#", "%", "…")

object Layouts {

    /**
     * Układ jak w klawiaturze dla bieżących ustawień: litery wg [KeyboardSettings.activeLanguage],
     * w trybie „wiele naraz” ze znakami wszystkich języków tego samego alfabetu.
     */
    fun forSettings(mode: KeyboardMode, s: KeyboardSettings, field: FieldKind): KeyboardLayout {
        val lang = Languages.get(s.activeLanguage)
        val accentLangs = if (s.multilingual) Languages.sameScript(lang.code, s.languages).map { Languages.get(it) } else listOf(lang)
        val switchable = Languages.switchTargets(s.languages, s.activeLanguage, s.multilingual).size
        return build(mode, lang, s, field, switchable, accentLangs)
    }

    /**
     * [lang] – układ liter i waluta, [accentLangs] – języki, których znaki są pod przytrzymaniem
     * (pierwszy ma pierwszeństwo), [languageCount] – między iloma językami przełącza klawisz języka.
     */
    fun build(
        mode: KeyboardMode,
        lang: Lang,
        settings: KeyboardSettings,
        field: FieldKind,
        languageCount: Int,
        accentLangs: List<Lang> = listOf(lang),
    ): KeyboardLayout = when (mode) {
        KeyboardMode.ALPHA -> alpha(lang, settings, field, languageCount, accentLangs)
        KeyboardMode.SYMBOLS -> symbols(lang, settings, field, second = false)
        KeyboardMode.SYMBOLS2 -> symbols(lang, settings, field, second = true)
        KeyboardMode.NUMBER -> numpad(phone = false)
        KeyboardMode.PHONE -> numpad(phone = true)
    }

    private fun alpha(lang: Lang, s: KeyboardSettings, field: FieldKind, languageCount: Int, accentLangs: List<Lang>): KeyboardLayout {
        val accents = mergedAccents(accentLangs)
        val layoutId = s.layoutOverride[lang.code] ?: lang.layout
        val rows = letterRows[layoutId] ?: letterRows.getValue("qwerty")
        val units = maxOf(rows[0].length, rows[1].length, rows[2].length + 3, 10).toFloat()
        val out = mutableListOf<List<Key>>()
        // W polu hasła cyfry zawsze pod ręką.
        val numberRow = s.numberRow || field == FieldKind.PASSWORD
        if (numberRow) out += "1234567890".map { c -> Key(KeyType.CHAR, c.toString(), popup = superscripts[c]?.map { it.toString() } ?: emptyList()) }
        rows.forEachIndexed { r, letters ->
            val row = letters.mapIndexed { i, c ->
                val digit = if (r == 0 && !numberRow && i < 10) "1234567890"[i].toString() else null
                val symbol = if (s.symbolKeys && field != FieldKind.PASSWORD) letterSymbols[r].getOrNull(i)?.toString() else null
                letterKey(c, accents, digit, symbol, s.keyHints)
            }
            if (r == 2) {
                val side = ((units - row.size) / 2f).coerceAtLeast(1f)
                out += listOf(Key(KeyType.SHIFT, width = side)) + row + Key(KeyType.DELETE, width = side)
            } else {
                out += row
            }
        }
        out += bottomRow(s, field, languageCount, units, KeyType.SYMBOLS, "?123", lang, accentLangs)
        return KeyboardLayout(out, units, KeyboardMode.ALPHA)
    }

    /** Znaki pod przytrzymaniem z kilku języków naraz – w kolejności języków, bez powtórzeń. */
    internal fun mergedAccents(langs: List<Lang>): Map<Char, String> {
        val out = LinkedHashMap<Char, String>()
        for (l in langs) for ((c, chars) in l.accents) {
            val cur = out[c].orEmpty()
            out[c] = cur + chars.filter { it !in cur }
        }
        return out
    }

    /** Klawisz litery: pierwsza opcja okienka (pod palcem) to cyfra, a bez niej znak języka (np. Ł). */
    private fun letterKey(c: Char, accents: Map<Char, String>, digit: String?, symbol: String?, hints: Boolean): Key {
        val primary = accents[c].orEmpty()
        val generic = Languages.genericAccents[c].orEmpty().filter { it !in primary }
        val popup = buildList {
            // Pod palcem cyfra, a bez niej znak języka (L→ł); symbol dopiero dalej.
            if (digit != null) add(digit)
            primary.forEach { add(it.toString()) }
            if (symbol != null) add(symbol)
            // Ogólne znaki tylko dla alfabetu łacińskiego – w cyrylicy i greckim byłyby szumem.
            if (c in 'a'..'z') generic.forEach { add(it.toString()) }
        }.distinct()
        val hint = when {
            !hints -> null
            digit != null -> digit
            symbol != null -> symbol
            primary.isNotEmpty() -> primary.first().toString()
            else -> null
        }
        // Drugi znak w lewym rogu: symbol przy cyfrze, albo polski znak przy symbolu.
        val hint2 = if (hints && symbol != null) (if (digit != null) symbol else primary.firstOrNull()?.toString()) else null
        return Key(KeyType.CHAR, c.toString(), popup = popup, hint = hint, hint2 = hint2)
    }

    private fun bottomRow(
        s: KeyboardSettings,
        field: FieldKind,
        languageCount: Int,
        units: Float,
        switchType: KeyType,
        switchLabel: String,
        lang: Lang,
        accentLangs: List<Lang> = listOf(lang),
    ): List<Key> {
        val row = mutableListOf<Key>()
        // Emoji tylko tam, gdzie mają sens – nie w adresach, hasłach i liczbach.
        val emojiField = field == FieldKind.TEXT || field == FieldKind.SEARCH
        val combo = emojiField && s.emojiKeyMode == EmojiKey.COMBINED && switchType == KeyType.SYMBOLS
        row += Key(switchType, label = switchLabel, width = 1.5f, combo = combo)
        if (emojiField && s.emojiKeyMode == EmojiKey.SEPARATE) row += Key(KeyType.EMOJI)
        when (field) {
            FieldKind.EMAIL -> row += Key(KeyType.CHAR, "@", popup = listOf("_", "-", "+"))
            FieldKind.URL -> row += Key(KeyType.CHAR, "/", popup = listOf(":", "?", "=", "&", "#", "_", "-", "~"))
            else -> if (s.commaKey) row += Key(KeyType.CHAR, ",", popup = listOf(";", ":", "„", "”", "'", "\"", "-"))
        }
        if (s.languageKey && languageCount > 1) row += Key(KeyType.LANGUAGE)
        val tail = mutableListOf<Key>()
        when (field) {
            FieldKind.EMAIL, FieldKind.URL -> tail += Key(KeyType.TEXT, ".com", label = ".com", popup = listOf(".pl", ".eu", ".org", ".net", ".io", ".de", ".uk"))
            else -> {}
        }
        tail += Key(KeyType.CHAR, ".", popup = periodPopup)
        tail += Key(KeyType.ENTER, width = 1.5f)
        var used = row.sumOf { it.width.toDouble() } + tail.sumOf { it.width.toDouble() }
        // Ciasno (emoji + język + przecinek) – węższe ?123 i Enter, żeby spacja została wygodna.
        if (units - used < 3.5) {
            row[0] = row[0].copy(width = 1.25f)
            tail[tail.lastIndex] = tail.last().copy(width = 1.25f)
            used -= 0.5
        }
        val space = (units - used).toFloat().coerceAtLeast(2f)
        return row + Key(KeyType.SPACE, " ", label = spaceLabel(s, lang, accentLangs), width = space) + tail
    }

    /** Napis na spacji: bieżący język albo wszystkie działające naraz z tym układem („PL · EN”). */
    private fun spaceLabel(s: KeyboardSettings, lang: Lang, together: List<Lang>): String =
        if (s.multilingual && together.size > 1) {
            together.take(3).joinToString(" · ") { it.code.uppercase() }
        } else {
            lang.native
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
        val emoji = if (s.emojiKeyMode == EmojiKey.OFF) EmojiKey.OFF else EmojiKey.SEPARATE
        val bottom = bottomRow(s.copy(emojiKeyMode = emoji, languageKey = false), field, 1, units, KeyType.ALPHA, "ABC", lang)
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
