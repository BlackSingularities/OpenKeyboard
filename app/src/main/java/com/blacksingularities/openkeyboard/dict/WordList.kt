package com.blacksingularities.openkeyboard.dict

import java.text.Normalizer
import kotlin.math.ln

/**
 * Klucz porównań: małe litery, bez znaków diakrytycznych, apostrofów i łączników.
 * Dzięki temu „zolw” znajduje „żółw”, a „dont” – „don't”.
 */
fun normalizeKey(word: String): String {
    val sb = StringBuilder(word.length)
    for (ch in word.lowercase()) {
        when (ch) {
            'ł' -> sb.append('l')
            'ø' -> sb.append('o')
            'đ' -> sb.append('d')
            'ß' -> sb.append("ss")
            'ı' -> sb.append('i')
            'æ' -> sb.append("ae")
            'œ' -> sb.append("oe")
            '\'', '’', '-', '·' -> {}
            // Litery cyrylicy, których rozkład zmieniłby znaczenie, zostają bez zmian.
            'й', 'ї', 'ў' -> sb.append(ch)
            else -> {
                if (ch.code < 0x80) {
                    sb.append(ch)
                } else {
                    val d = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)
                    for (c in d) if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) sb.append(c)
                }
            }
        }
    }
    return sb.toString()
}

/**
 * Lista słów jednego języka z częstością. Słowa posortowane malejąco wg częstości,
 * do tego indeks kluczy (wyszukiwanie prefiksów) i grupy wg długości (poprawianie literówek).
 */
class WordList(val lang: String, val words: Array<String>, counts: IntArray) {
    val keys: Array<String> = Array(words.size) { normalizeKey(words[it]) }

    /** Siła słowa ~ log częstości (0 – najrzadsze). */
    val score: FloatArray = run {
        val minLog = ln(counts.minOrNull()?.coerceAtLeast(1)?.toDouble() ?: 1.0)
        FloatArray(words.size) { (ln(counts[it].coerceAtLeast(1).toDouble()) - minLog).toFloat() }
    }

    private val sorted: IntArray = words.indices.sortedWith(compareBy<Int> { keys[it] }.thenBy { it }).toIntArray()
    private val sortedKeys: Array<String> = Array(sorted.size) { keys[sorted[it]] }

    /** Dokładne słowo (małymi literami) → indeks. */
    private val exact = HashMap<String, Int>(words.size * 2).apply {
        for (i in words.indices) putIfAbsent(words[i].lowercase(), i)
    }

    /** Klucz → najczęstsze słowo o tym kluczu (np. „zolw” → „żółw”). */
    private val bestForKey = HashMap<String, Int>(words.size * 2).apply {
        for (i in words.indices) putIfAbsent(this@WordList.keys[i], i)
    }

    val byLength: Map<Int, IntArray> = words.indices.groupBy { keys[it].length }.mapValues { it.value.toIntArray() }

    val size: Int get() = words.size

    fun indexOf(word: String): Int = exact[word.lowercase()] ?: -1

    fun contains(word: String): Boolean = exact.containsKey(word.lowercase())

    fun bestForKey(key: String): Int = bestForKey[key] ?: -1

    /** Indeksy słów, których klucz zaczyna się od [prefix] – najczęstsze najpierw. */
    fun withPrefix(prefix: String, limit: Int, scanCap: Int = 4000): List<Int> {
        if (prefix.isEmpty()) return emptyList()
        var lo = 0
        var hi = sortedKeys.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (sortedKeys[mid] < prefix) lo = mid + 1 else hi = mid
        }
        val found = ArrayList<Int>()
        var i = lo
        while (i < sortedKeys.size && found.size < scanCap && sortedKeys[i].startsWith(prefix)) {
            found += sorted[i]
            i++
        }
        // Indeks w tablicy = pozycja w rankingu częstości, więc mniejszy = częstszy.
        found.sort()
        return if (found.size > limit) found.subList(0, limit) else found
    }

    companion object {
        /** Wczytuje plik „słowo<TAB>liczba” (posortowany lub nie). */
        fun parse(lang: String, lines: Sequence<String>, max: Int = 80_000): WordList {
            val w = ArrayList<String>()
            val c = ArrayList<Int>()
            val seen = HashSet<String>()
            for (line in lines) {
                if (w.size >= max) break
                val tab = line.indexOf('\t').takeIf { it > 0 } ?: line.indexOf(' ').takeIf { it > 0 } ?: continue
                val word = line.substring(0, tab).trim()
                val count = line.substring(tab + 1).trim().toIntOrNull() ?: continue
                if (word.isEmpty() || !seen.add(word)) continue
                w += word
                c += count
            }
            val order = w.indices.sortedByDescending { c[it] }
            return WordList(lang, Array(order.size) { w[order[it]] }, IntArray(order.size) { c[order[it]] })
        }
    }
}
