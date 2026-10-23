package com.blacksingularities.openkeyboard.dict

import kotlin.math.ln

/**
 * Wykrywanie języka, w którym właśnie się pisze. Każde z ostatnich słów głosuje na język,
 * w którym jest wyraźnie częstsze (stosunek pozycji w rankingach częstości) – słowa wspólne
 * („a”, „to”, „no”) prawie nie głosują, a słowa spoza słownika innego języka – mocno.
 * Nowsze słowa ważą więcej. [rank] – pozycja słowa w słowniku języka (od 1) albo null.
 */
class LanguageDetector(private val rank: (lang: String, word: String) -> Int?) {
    private val recent = ArrayDeque<String>()

    fun clear() = recent.clear()

    fun add(word: String) {
        if (word.none { it.isLetter() }) return
        recent.addLast(word.lowercase())
        while (recent.size > WINDOW) recent.removeFirst()
    }

    /** Słowa z tekstu (np. tego, co już jest w polu) – liczą się ostatnie. */
    fun addText(text: String) {
        WORD.findAll(text).map { it.value }.toList().takeLast(WINDOW).forEach(::add)
    }

    /** Język z wyraźną przewagą wśród [langs] – albo null, gdy nie da się rozstrzygnąć. */
    fun detect(langs: List<String>): String? {
        if (langs.size < 2 || recent.isEmpty()) return null
        val total = HashMap<String, Float>()
        recent.forEachIndexed { i, w ->
            val weight = 0.6f + 0.4f * (i + 1) / recent.size
            val ranks = langs.associateWith { rank(it, w)?.toFloat() }
            if (ranks.values.all { it == null }) return@forEachIndexed
            for (l in langs) {
                val r = ranks[l] ?: continue
                val other = langs.filter { it != l }.minOf { ranks[it] ?: MISSING }
                val vote = (ln(other / r) / LN10).coerceIn(0f, MAX_VOTE)
                if (vote > 0f) total[l] = (total[l] ?: 0f) + vote * weight
            }
        }
        val sorted = total.entries.sortedByDescending { it.value }
        val best = sorted.firstOrNull() ?: return null
        val second = sorted.getOrNull(1)?.value ?: 0f
        return if (best.value >= MIN_SCORE && best.value - second >= MIN_LEAD) best.key else null
    }

    private companion object {
        const val WINDOW = 8
        /** Pozycja słowa, którego nie ma w słowniku (dalej niż koniec każdej listy). */
        const val MISSING = 400_000f
        const val MAX_VOTE = 1.5f
        const val MIN_SCORE = 2f
        const val MIN_LEAD = 1f
        val LN10 = ln(10f)
        val WORD = Regex("""\p{L}[\p{L}'’-]*""")
    }
}
