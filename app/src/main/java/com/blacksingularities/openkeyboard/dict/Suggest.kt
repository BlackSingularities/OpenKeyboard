package com.blacksingularities.openkeyboard.dict

import com.blacksingularities.openkeyboard.data.Autocorrect
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

enum class SuggestionKind { TYPED, WORD, PREDICTION, EMOJI, SHORTCUT, CALC, CLIPBOARD }

data class Suggestion(
    val text: String,
    val kind: SuggestionKind,
    val display: String = text,
)

data class Suggestions(
    /** Kolejność wg trafności: [0] trafia na środek paska. */
    val items: List<Suggestion> = emptyList(),
    /** Słowo, które zastąpi wpisane po spacji (autokorekta) – albo null. */
    val autoCorrect: Suggestion? = null,
    /** Wpisane słowo istnieje w słowniku. */
    val typedValid: Boolean = false,
) {
    companion object {
        val Empty = Suggestions()
    }
}

/** Sąsiedztwo klawiszy aktualnego układu – literówka na sąsiednim klawiszu kosztuje mniej. */
fun interface Proximity {
    fun near(a: Char, b: Char): Boolean
}

private class Candidate(val word: String, var score: Float, var dist: Float)

/**
 * Silnik podpowiedzi: uzupełnianie słów, poprawianie literówek (ważona odległość Damerau–Levenshteina
 * z uwzględnieniem sąsiednich klawiszy, podwojonych liter i przestawień), znaki diakrytyczne bez
 * ich wpisywania („zolw” → „żółw”), wiele języków naraz, słowa i pary słów użytkownika.
 */
object Suggest {

    fun suggest(
        typed: String,
        prev: String?,
        langs: List<String>,
        active: String,
        mode: Autocorrect,
        proximity: Proximity,
        limit: Int = 3,
    ): Suggestions {
        val key = normalizeKey(typed)
        if (key.isEmpty() || typed.any { it.isDigit() }) return Suggestions.Empty
        val cands = HashMap<String, Candidate>()
        var typedValid = false

        fun offer(word: String, score: Float, dist: Float) {
            if (UserModel.isBlocked(word)) return
            val k = word.lowercase()
            val c = cands[k]
            if (c == null) {
                cands[k] = Candidate(word, score, dist)
            } else {
                if (score > c.score) c.score = score
                if (dist >= 0 && (c.dist < 0 || dist < c.dist)) c.dist = dist
            }
        }

        for (lang in langs) {
            val wl = Dictionaries.get(lang) ?: continue
            val weight = if (lang == active) 1f else 0.88f
            val exact = wl.indexOf(typed)
            if (exact >= 0) {
                typedValid = true
                offer(wl.words[exact], wl.score[exact] * weight + 8f, 0f)
            }
            // Ten sam klucz bez znaków diakrytycznych („zolw” → „żółw”, „dont” → „don't”).
            val same = wl.bestForKey(key)
            if (same >= 0 && same != exact) offer(wl.words[same], wl.score[same] * weight + 6.5f, 0.05f)

            // Uzupełnienia: im dłuższy brakujący fragment, tym mniejsza szansa.
            for (i in wl.withPrefix(key, 14)) {
                val extra = wl.keys[i].length - key.length
                if (extra == 0) continue
                offer(wl.words[i], wl.score[i] * weight - 0.55f * extra + 1.2f, -1f)
            }

            // Literówki.
            if (key.length >= 2) {
                val maxDist = when {
                    key.length <= 3 -> 1f
                    key.length <= 6 -> 1.6f
                    else -> 2.2f
                }
                val lens = (key.length - 1)..(key.length + if (key.length >= 6) 2 else 1)
                for (len in lens) {
                    val bucket = wl.byLength[len] ?: continue
                    for (i in bucket) {
                        val k = wl.keys[i]
                        // Pierwsza litera zwykle jest trafiona – pozostałe słowa pomijamy (szybkość).
                        if (k[0] != key[0] && !proximity.near(k[0], key[0]) && !(k.length > 1 && k[1] == key[0])) continue
                        val d = distance(key, k, maxDist, proximity)
                        if (d > maxDist) continue
                        offer(wl.words[i], wl.score[i] * weight - d * 5.2f, d)
                    }
                }
            }
        }

        // Słowa użytkownika – wyuczone z pisania.
        for ((w, count) in UserModel.withPrefix(key, 8)) {
            val extra = normalizeKey(w).length - key.length
            val s = 7f + ln(count + 1f) * 1.6f - 0.55f * extra
            if (extra == 0) typedValid = typedValid || (count >= 2 && w.equals(typed, ignoreCase = true))
            offer(w, if (extra == 0 && w.equals(typed, true)) s + 8f else s, if (extra == 0) 0.05f else -1f)
        }

        // Bonus za parę słów (kontekst poprzedniego słowa) i za częste wybieranie.
        for (c in cands.values) {
            val big = UserModel.bigramCount(prev, c.word)
            if (big > 0) c.score += 2.5f + ln(big.toFloat()) * 1.2f
            val learned = UserModel.count(c.word)
            if (learned > 0) c.score += min(3f, ln(learned + 1f))
        }

        val ranked = cands.values.sortedByDescending { it.score }
        val items = ranked.take(limit + 1).map { Suggestion(applyCase(typed, it.word), SuggestionKind.WORD) }
            .distinctBy { it.text }
            .filter { it.text != typed || typedValid }
            .toMutableList()

        // Autokorekta: tylko gdy wpisane słowo nie istnieje, a kandydat jest bliski i częsty.
        var auto: Suggestion? = null
        val top = ranked.firstOrNull()
        if (mode != Autocorrect.OFF && !typedValid && top != null && top.dist >= 0 && key.length >= 2 && !(typed.length > 1 && typed.all { it.isUpperCase() })) {
            val allowed = when (mode) {
                Autocorrect.GENTLE -> top.dist <= 0.1f || (top.dist <= 1f && key.length >= 4)
                Autocorrect.NORMAL -> top.dist <= 1.1f || (top.dist <= 1.7f && key.length >= 6)
                Autocorrect.AGGRESSIVE -> true
                Autocorrect.OFF -> false
            }
            if (allowed) auto = Suggestion(applyCase(typed, top.word), SuggestionKind.WORD)
        }
        if (auto != null) {
            items.removeAll { it.text == auto.text }
            items.add(0, auto)
            // Wpisane słowo zostaje do wyboru – żeby można było odrzucić poprawkę.
            items.add(minOf(1, items.size), Suggestion(typed, SuggestionKind.TYPED, "„$typed”"))
        } else if (!typedValid && items.none { it.text == typed }) {
            items.add(minOf(1, items.size), Suggestion(typed, SuggestionKind.TYPED, "„$typed”"))
        }
        return Suggestions(items.take(limit), auto, typedValid)
    }

    /** Następne słowo po [prev] – z par wyuczonych podczas pisania. */
    fun predict(prev: String?, capitalize: Boolean, limit: Int = 3): Suggestions {
        if (prev.isNullOrBlank()) return Suggestions.Empty
        val next = UserModel.next(prev, limit)
        if (next.isEmpty()) return Suggestions.Empty
        return Suggestions(next.map { (w, _) -> Suggestion(if (capitalize) w.replaceFirstChar { it.uppercaseChar() } else w, SuggestionKind.PREDICTION) })
    }

    fun isValid(word: String, langs: List<String>): Boolean =
        langs.any { Dictionaries.get(it)?.contains(word) == true } || UserModel.count(word) >= 2

    /** Wielkość liter jak we wpisanym słowie: „Kto” → „Który”, „NIE” → „NIGDY”. */
    fun applyCase(typed: String, word: String): String = when {
        typed.length > 1 && typed.all { !it.isLetter() || it.isUpperCase() } -> word.uppercase()
        typed.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
        else -> word
    }

    /**
     * Ważona odległość edycji (optymalne dopasowanie z przestawieniami). Zamiana na sąsiedni
     * klawisz – 0,55, przestawienie – 0,6, podwojona/pominięta podwójna litera – 0,5–0,6.
     * Przerywa liczenie, gdy cały wiersz przekroczy [max].
     */
    fun distance(a: String, b: String, max: Float, proximity: Proximity): Float {
        val n = a.length
        val m = b.length
        if (abs(n - m) > max) return Float.MAX_VALUE
        var prev2 = FloatArray(m + 1)
        var prev = FloatArray(m + 1) { it.toFloat() }
        var cur = FloatArray(m + 1)
        for (i in 1..n) {
            val ca = a[i - 1]
            cur[0] = i.toFloat()
            var rowMin = cur[0]
            for (j in 1..m) {
                val cb = b[j - 1]
                val sub = when {
                    ca == cb -> 0f
                    proximity.near(ca, cb) -> 0.55f
                    else -> 1f
                }
                val del = if (i > 1 && a[i - 2] == ca) 0.5f else 1f
                val ins = if (j > 1 && b[j - 2] == cb) 0.6f else 1f
                var v = min(min(prev[j] + del, cur[j - 1] + ins), prev[j - 1] + sub)
                if (i > 1 && j > 1 && ca == b[j - 2] && a[i - 2] == cb && ca != cb) v = min(v, prev2[j - 2] + 0.6f)
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > max) return Float.MAX_VALUE
            val t = prev2
            prev2 = prev
            prev = cur
            cur = t
        }
        return prev[m]
    }
}
