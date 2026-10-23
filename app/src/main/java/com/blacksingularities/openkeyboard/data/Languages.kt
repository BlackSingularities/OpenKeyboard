package com.blacksingularities.openkeyboard.data

/**
 * Język pisania: układ klawiszy, znaki pod przytrzymaniem i źródło słownika.
 * [accents] – znaki tego języka pokazywane jako pierwsze (i jako mała podpowiedź na klawiszu).
 * [dict] – kod listy słów w FrequencyWords (napisy filmowe, CC-BY-SA 4.0).
 */
data class Lang(
    val code: String,
    val name: String,
    val native: String,
    val layout: String,
    val accents: Map<Char, String> = emptyMap(),
    val currency: String = "€",
    val dict: String = code,
    val bundled: Boolean = false,
    val rtl: Boolean = false,
) {
    /** Alfabet – wg domyślnego układu (własny układ, np. QWERTY dla rosyjskiego, go nie zmienia). */
    val script: Script
        get() = when (layout) {
            "ru", "uk", "be" -> Script.CYRILLIC
            "el" -> Script.GREEK
            else -> Script.LATIN
        }
}

/** Alfabet języka – języki o tym samym alfabecie dzielą jeden układ klawiszy w trybie „wiele naraz”. */
enum class Script { LATIN, CYRILLIC, GREEK }

object Languages {
    val all: List<Lang> = listOf(
        Lang("pl", "Polski", "Polski", "qwerty", mapOf('a' to "ą", 'c' to "ć", 'e' to "ę", 'l' to "ł", 'n' to "ń", 'o' to "ó", 's' to "ś", 'z' to "żź"), currency = "zł", bundled = true),
        Lang("en", "Angielski", "English", "qwerty", currency = "$", bundled = true),
        Lang("de", "Niemiecki", "Deutsch", "qwertz_de", mapOf('a' to "ä", 'o' to "ö", 'u' to "ü", 's' to "ß")),
        Lang("fr", "Francuski", "Français", "azerty", mapOf('e' to "éèêë", 'a' to "àâæ", 'c' to "ç", 'i' to "îï", 'o' to "ôœ", 'u' to "ùûü", 'y' to "ÿ")),
        Lang("es", "Hiszpański", "Español", "qwerty_es", mapOf('a' to "á", 'e' to "é", 'i' to "í", 'n' to "ñ", 'o' to "ó", 'u' to "úü")),
        Lang("it", "Włoski", "Italiano", "qwerty", mapOf('a' to "à", 'e' to "èé", 'i' to "ìí", 'o' to "òó", 'u' to "ùú")),
        Lang("pt", "Portugalski", "Português", "qwerty", mapOf('a' to "ãáâà", 'c' to "ç", 'e' to "éê", 'i' to "í", 'o' to "õóô", 'u' to "ú"), dict = "pt_br"),
        Lang("cs", "Czeski", "Čeština", "qwertz", mapOf('a' to "á", 'c' to "č", 'd' to "ď", 'e' to "éě", 'i' to "í", 'n' to "ň", 'o' to "ó", 'r' to "ř", 's' to "š", 't' to "ť", 'u' to "úů", 'y' to "ý", 'z' to "ž"), currency = "Kč"),
        Lang("sk", "Słowacki", "Slovenčina", "qwertz", mapOf('a' to "áä", 'c' to "č", 'd' to "ď", 'e' to "é", 'i' to "í", 'l' to "ľĺ", 'n' to "ň", 'o' to "óô", 'r' to "ŕ", 's' to "š", 't' to "ť", 'u' to "ú", 'y' to "ý", 'z' to "ž")),
        Lang("uk", "Ukraiński", "Українська", "uk", mapOf('г' to "ґ", 'і' to "ї", 'е' to "є"), currency = "₴"),
        Lang("ru", "Rosyjski", "Русский", "ru", mapOf('е' to "ё", 'ь' to "ъ"), currency = "₽"),
        Lang("be", "Białoruski", "Беларуская", "be", mapOf('е' to "ё", 'у' to "ў"), currency = "Br", dict = ""),
        Lang("nl", "Niderlandzki", "Nederlands", "qwerty", mapOf('e' to "éë", 'i' to "ï", 'o' to "óö", 'u' to "ü")),
        Lang("sv", "Szwedzki", "Svenska", "nordic_sv", mapOf('a' to "åä", 'o' to "ö", 'e' to "é"), currency = "kr"),
        Lang("no", "Norweski", "Norsk", "nordic_da", mapOf('a' to "æå", 'o' to "ø", 'e' to "é"), currency = "kr"),
        Lang("da", "Duński", "Dansk", "nordic_da", mapOf('a' to "æå", 'o' to "ø", 'e' to "é"), currency = "kr"),
        Lang("fi", "Fiński", "Suomi", "nordic_sv", mapOf('a' to "äå", 'o' to "ö")),
        Lang("hu", "Węgierski", "Magyar", "qwertz", mapOf('a' to "á", 'e' to "é", 'i' to "í", 'o' to "óöő", 'u' to "úüű"), currency = "Ft"),
        Lang("ro", "Rumuński", "Română", "qwerty", mapOf('a' to "ăâ", 'i' to "î", 's' to "ș", 't' to "ț"), currency = "lei"),
        Lang("hr", "Chorwacki", "Hrvatski", "qwertz", mapOf('c' to "čć", 'd' to "đ", 's' to "š", 'z' to "ž")),
        Lang("sl", "Słoweński", "Slovenščina", "qwertz", mapOf('c' to "čć", 's' to "š", 'z' to "ž")),
        Lang("tr", "Turecki", "Türkçe", "tr", mapOf('c' to "ç", 'g' to "ğ", 'o' to "ö", 's' to "ş", 'u' to "ü"), currency = "₺"),
        Lang("el", "Grecki", "Ελληνικά", "el", mapOf('α' to "ά", 'ε' to "έ", 'η' to "ή", 'ι' to "ίϊΐ", 'ο' to "ό", 'υ' to "ύϋΰ", 'ω' to "ώ")),
        Lang("lt", "Litewski", "Lietuvių", "qwerty", mapOf('a' to "ą", 'c' to "č", 'e' to "ęė", 'i' to "į", 's' to "š", 'u' to "ųū", 'z' to "ž")),
        Lang("lv", "Łotewski", "Latviešu", "qwerty", mapOf('a' to "ā", 'c' to "č", 'e' to "ē", 'g' to "ģ", 'i' to "ī", 'k' to "ķ", 'l' to "ļ", 'n' to "ņ", 's' to "š", 'u' to "ū", 'z' to "ž")),
        Lang("et", "Estoński", "Eesti", "qwerty", mapOf('a' to "ä", 'o' to "öõ", 'u' to "ü", 's' to "š", 'z' to "ž")),
        Lang("ca", "Kataloński", "Català", "qwerty", mapOf('a' to "à", 'c' to "ç", 'e' to "èé", 'i' to "íï", 'o' to "òó", 'u' to "úü", 'l' to "·")),
        Lang("eo", "Esperanto", "Esperanto", "qwerty", mapOf('c' to "ĉ", 'g' to "ĝ", 'h' to "ĥ", 'j' to "ĵ", 's' to "ŝ", 'u' to "ŭ")),
        Lang("id", "Indonezyjski", "Indonesia", "qwerty", currency = "Rp"),
        Lang("vi", "Wietnamski", "Tiếng Việt", "qwerty", mapOf('a' to "àáảãạăằắẳẵặâầấẩẫậ", 'd' to "đ", 'e' to "èéẻẽẹêềếểễệ", 'i' to "ìíỉĩị", 'o' to "òóỏõọôồốổỗộơờớởỡợ", 'u' to "ùúủũụưừứửữự", 'y' to "ỳýỷỹỵ"), currency = "₫"),
    )

    private val byCode = all.associateBy { it.code }

    fun get(code: String): Lang = byCode[code] ?: all.first()

    fun isSupported(code: String): Boolean = code in byCode

    /** Włączone języki pisane tym samym alfabetem co [code] – [code] pierwszy, reszta w kolejności z [enabled]. */
    fun sameScript(code: String, enabled: List<String>): List<String> {
        val script = get(code).script
        return listOf(code) + enabled.filter { it != code && get(it).script == script }
    }

    /**
     * Między czym przełącza klawisz języka: w trybie „wiele naraz” tylko między alfabetami
     * (łacinka ↔ cyrylica) – po jednym języku z każdej grupy, [active] za swoją grupę.
     */
    fun switchTargets(enabled: List<String>, active: String, multilingual: Boolean): List<String> {
        if (!multilingual) return enabled
        val activeScript = get(active).script
        return enabled.groupBy { get(it).script }.map { (script, group) -> if (script == activeScript) active else group.first() }
    }

    /**
     * Języki pisania na pierwsze uruchomienie: języki systemu, które znamy (najwyżej trzy),
     * i angielski. Stare kody Androida („in”, „nb”) zamieniane na używane tutaj.
     */
    fun fromSystem(systemCodes: List<String>): List<String> {
        val mapped = systemCodes.map {
            when (it.lowercase()) {
                "in" -> "id"
                "nb", "nn" -> "no"
                else -> it.lowercase()
            }
        }.filter { isSupported(it) }.distinct().take(3)
        return (mapped + "en").distinct()
    }

    /** Dostępne układy dla języka (domyślny pierwszy) – do zmiany w ustawieniach. */
    fun layoutsFor(lang: Lang): List<String> {
        val latin = listOf("qwerty", "qwertz", "azerty", "colemak", "dvorak")
        return if (lang.layout in latin) listOf(lang.layout) + (latin - lang.layout) else listOf(lang.layout, "qwerty")
    }

    fun layoutName(id: String): String = when (id) {
        "qwerty", "qwerty_es" -> "QWERTY"
        "qwertz", "qwertz_de" -> "QWERTZ"
        "azerty" -> "AZERTY"
        "colemak" -> "Colemak"
        "dvorak" -> "Dvorak"
        "nordic_sv", "nordic_da" -> "QWERTY (Nordic)"
        "ru", "uk", "be" -> "ЙЦУКЕН"
        "el" -> "ΕΡΤΥ"
        "tr" -> "Türkçe Q"
        else -> id.uppercase()
    }

    /** Znaki ogólne pod przytrzymaniem (po znakach danego języka). */
    val genericAccents: Map<Char, String> = mapOf(
        'a' to "àáâäãåāąæ",
        'c' to "çćčĉ",
        'd' to "ďđ",
        'e' to "èéêëēęėě",
        'g' to "ğĝ",
        'i' to "ìíîïīįı",
        'l' to "łľĺ",
        'n' to "ñńň",
        'o' to "òóôöõøōőœ",
        'r' to "řŕ",
        's' to "śšşß",
        't' to "ťţþ",
        'u' to "ùúûüūůűų",
        'y' to "ýÿ",
        'z' to "żźž",
    )
}
