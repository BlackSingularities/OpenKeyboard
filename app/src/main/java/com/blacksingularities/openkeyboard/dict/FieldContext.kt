package com.blacksingularities.openkeyboard.dict

/**
 * Kontekst bieżącego pola: słowa, które już w nim są (imiona, nazwy, terminy – podpowiadane
 * nawet spoza słownika), i to, co w nim padło po tych samych słowach (przewidywanie następnego).
 * Tylko w pamięci, dla jednego pola – nic nie trafia do modelu użytkownika.
 */
class FieldContext private constructor(sentences: List<List<String>>) {
    /** Klucz (małe litery) → (forma z tekstu, ile razy). */
    private val vocab = HashMap<String, Pair<String, Int>>()
    /** „poprzednie” → następne słowo → ile razy. */
    private val after1 = HashMap<String, HashMap<String, Int>>()
    /** „przedostatnie poprzednie” → następne słowo → ile razy. */
    private val after2 = HashMap<String, HashMap<String, Int>>()

    init {
        for (sentence in sentences) {
            sentence.forEachIndexed { i, w ->
                val k = w.lowercase()
                // Forma z wielkiej litery tylko w środku zdania (imię), na początku zdania – mała.
                val seen = vocab[k]
                val form = if (i == 0) seen?.first ?: k else w
                vocab[k] = form to ((seen?.second ?: 0) + 1)
                if (i >= 1) after1.getOrPut(sentence[i - 1].lowercase()) { HashMap() }.merge(w, 1, Int::plus)
                if (i >= 2) after2.getOrPut(sentence[i - 2].lowercase() + " " + sentence[i - 1].lowercase()) { HashMap() }.merge(w, 1, Int::plus)
            }
        }
    }

    /** Słowa z kluczami (bez ogonków) policzonymi raz – wyszukiwanie przy każdej literze. */
    private val keyed: List<Pair<String, Pair<String, Int>>> = vocab.values.filter { it.first.length >= 2 }.map { normalizeKey(it.first) to it }

    val isEmpty: Boolean get() = vocab.isEmpty()

    fun count(word: String): Int = vocab[word.lowercase()]?.second ?: 0

    /** Słowa z pola, których klucz zaczyna się od [keyPrefix] (znormalizowanego) – częstsze pierwsze. */
    fun withPrefix(keyPrefix: String, limit: Int): List<Pair<String, Int>> =
        keyed.asSequence()
            .filter { it.first.startsWith(keyPrefix) }
            .map { it.second }
            .sortedByDescending { it.second }
            .take(limit)
            .toList()

    /** Ile razy [word] padło w polu po [prev] (i po [prev2] [prev], gdy [prev2] podane). */
    fun followCount(prev2: String?, prev: String?, word: String): Int {
        if (prev == null) return 0
        val map = if (prev2 != null) after2[prev2.lowercase() + " " + prev.lowercase()] else after1[prev.lowercase()]
        return map?.entries?.firstOrNull { it.key.equals(word, ignoreCase = true) }?.value ?: 0
    }

    /** Co padło w polu po [prev2] [prev] i po samym [prev] – z liczbą wystąpień. */
    fun next(prev2: String?, prev: String?): List<Pair<String, Int>> {
        if (prev == null) return emptyList()
        val out = LinkedHashMap<String, Int>()
        if (prev2 != null) after2[prev2.lowercase() + " " + prev.lowercase()]?.forEach { (w, n) -> out[w] = n * 2 }
        after1[prev.lowercase()]?.forEach { (w, n) -> out[w] = maxOf(out[w] ?: 0, n) }
        return out.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    companion object {
        val Empty = FieldContext(emptyList())
        private const val MAX_WORDS = 600
        private val WORD = Regex("""\p{L}[\p{L}'’-]*""")
        private val SENTENCE_END = Regex("""[.!?…\n]+""")

        /** Kontekst z tekstu pola (ostatnie [MAX_WORDS] słów). */
        fun of(text: String): FieldContext {
            if (text.isBlank()) return Empty
            var budget = MAX_WORDS
            val sentences = text.split(SENTENCE_END).asReversed().mapNotNull { s ->
                if (budget <= 0) return@mapNotNull null
                val words = WORD.findAll(s).map { it.value }.filter { w -> w.none { it.isDigit() } && w.length <= 40 }.toList().takeLast(budget)
                budget -= words.size
                words.takeIf { it.isNotEmpty() }
            }.asReversed()
            return FieldContext(sentences)
        }
    }
}

/**
 * Ostatnie słowa bieżącego zdania w [text] (tekst przed kursorem, bez wpisywanego słowa) –
 * od najdalszego. Pusta lista na początku zdania.
 */
fun wordsBefore(text: String, n: Int): List<String> {
    val trimmed = text.trimEnd()
    if (trimmed.isEmpty() || trimmed.last() in ".!?\n") return emptyList()
    val out = ArrayList<String>()
    var rest = trimmed
    while (out.size < n) {
        val m = Regex("""[\p{L}'’-]+$""").find(rest) ?: break
        out.add(0, m.value)
        rest = rest.substring(0, m.range.first)
        val gap = rest.length - rest.trimEnd().length
        val core = rest.trimEnd()
        // Słowa rozdziela tylko odstęp – przecinek czy kropka kończą kontekst.
        if (gap == 0 || core.isEmpty() || !core.last().isLetter()) break
        rest = core
    }
    return out
}
