package com.blacksingularities.openkeyboard.dict

import com.blacksingularities.openkeyboard.data.Autocorrect
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

enum class SuggestionKind { TYPED, WORD, PREDICTION, EMOJI, SHORTCUT, CALC, CLIPBOARD, UNDO, CONVERT }

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
/** Ile podpowiedzi liczyć do przewijanego paska – tyle, ile silnik uzna za sensowne. */
const val MAX_SUGGESTIONS = 50

object Suggest {

    fun suggest(
        typed: String,
        prev: String?,
        langs: List<String>,
        active: String,
        mode: Autocorrect,
        proximity: Proximity,
        limit: Int = MAX_SUGGESTIONS,
    ): Suggestions {
        val key = normalizeKey(typed)
        if (key.isEmpty() || typed.any { it.isDigit() }) return Suggestions.Empty
        val cands = HashMap<String, Candidate>()
        var typedValid = false
        // Wpisana forma istnieje dokładnie tak (np. polskie „i”) – nie zmieniaj jej na „I”.
        var typedExactCase = false

        fun offer(word: String, score: Float, dist: Float) {
            if (UserModel.isBlocked(word)) return
            val k = word.lowercase()
            val c = cands[k]
            if (c == null) {
                cands[k] = Candidate(word, score, dist)
            } else {
                if (dist == 0f && word == typed) {
                    cands[k] = Candidate(word, maxOf(score, c.score), 0f)
                    return
                }
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
                if (wl.words[exact] == typed) typedExactCase = true
                offer(wl.words[exact], wl.score[exact] * weight + 8f, 0f)
            }
            // Ten sam klucz bez znaków diakrytycznych („zolw” → „żółw”, „dont” → „don't”).
            val same = wl.bestForKey(key)
            if (same >= 0 && same != exact) offer(wl.words[same], wl.score[same] * weight + 6.5f, 0.05f)

            // Uzupełnienia: im dłuższy brakujący fragment, tym mniejsza szansa.
            for (i in wl.withPrefix(key, 40)) {
                val extra = wl.keys[i].length - key.length
                if (extra == 0) continue
                offer(wl.words[i], wl.score[i] * weight - 0.55f * extra + 1.2f, -1f)
            }

            // Literówki.
            if (key.length >= 2) {
                val maxDist = when {
                    key.length <= 3 -> 1f
                    key.length <= 5 -> 1.6f
                    key.length <= 8 -> 2.2f
                    else -> 2.8f
                }
                val lens = (key.length - if (key.length >= 6) 2 else 1)..(key.length + if (key.length >= 6) 2 else 1)
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
        for ((w, count) in UserModel.withPrefix(key, 20)) {
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
        // Forma ze słownika różni się tylko wielkością liter („i” → „I”, „warszawa” → „Warszawa”).
        if (mode != Autocorrect.OFF && typedValid && !typedExactCase && top != null && top.word != typed && top.word.equals(typed, ignoreCase = true) &&
            top.word.any { it.isUpperCase() } && typed.none { it.isUpperCase() }
        ) {
            auto = Suggestion(top.word, SuggestionKind.WORD)
        }
        if (mode != Autocorrect.OFF && !typedValid && top != null && top.dist >= 0 && key.length >= 2 && !(typed.length > 1 && typed.all { it.isUpperCase() })) {
            val allowed = when (mode) {
                Autocorrect.GENTLE -> top.dist <= 0.1f || (top.dist <= 1f && key.length >= 4)
                // Bełkot w dłuższych słowach też poprawiamy – byle kandydat był wyraźnie najlepszy.
                Autocorrect.NORMAL -> top.dist <= 1.2f || (top.dist <= 1.8f && key.length >= 5) || (top.dist <= 2.4f && key.length >= 8)
                Autocorrect.AGGRESSIVE -> true
                Autocorrect.OFF -> false
            }
            if (allowed && !UserModel.isRejected(typed, top.word)) auto = Suggestion(applyCase(typed, top.word), SuggestionKind.WORD)
        }
        // Dwa słowa: pominięta spacja („jakdzien” → „jak dzień”) albo litera zamiast spacji („jakndzień”).
        if (!typedValid && key.length >= 4) {
            val splits = splitWords(typed, langs, proximity)
            if (splits.isNotEmpty()) {
                val best = Suggestion(applyCase(typed, splits[0]), SuggestionKind.WORD)
                val singleClose = top != null && top.dist in 0f..0.6f
                if (mode != Autocorrect.OFF && !singleClose && !UserModel.isRejected(typed, splits[0])) auto = best
                items.removeAll { it.text.equals(best.text, ignoreCase = true) }
                items.add(if (auto == best) 0 else minOf(1, items.size), best)
                splits.drop(1).take(2).forEach { w -> items.add(minOf(2, items.size), Suggestion(applyCase(typed, w), SuggestionKind.WORD)) }
            }
        }
        if (auto != null && auto.text.equals(typed, ignoreCase = true)) {
            items.removeAll { it.text == auto.text }
            items.add(0, auto)
        } else if (auto != null) {
            items.removeAll { it.text == auto.text }
            items.add(0, auto)
            // Wpisane słowo zostaje do wyboru – żeby można było odrzucić poprawkę.
            items.add(minOf(1, items.size), Suggestion(typed, SuggestionKind.TYPED, "„$typed”"))
        } else if (!typedValid && items.none { it.text == typed }) {
            items.add(minOf(1, items.size), Suggestion(typed, SuggestionKind.TYPED, "„$typed”"))
        }
        return Suggestions(items.take(limit), auto, typedValid)
    }

    /** Litery dolnego rzędu – palec ześlizguje się z nich na spację (i odwrotnie). */
    private const val NEAR_SPACE = "zxcvbnm"
    private val ONE_LETTER = setOf("a", "i", "o", "u", "w", "z")

    /** Najlepsza forma części słowa w słownikach (także bez polskich znaków) i jej siła – albo null. */
    private fun lookup(part: String, langs: List<String>, proximity: Proximity? = null): Pair<String, Float>? {
        if (part.length == 1 && part.lowercase() !in ONE_LETTER) return null
        var best: Pair<String, Float>? = null
        for (lang in langs) {
            val wl = Dictionaries.get(lang) ?: continue
            val i = wl.indexOf(part).takeIf { it >= 0 } ?: wl.bestForKey(normalizeKey(part))
            if (i < 0) continue
            if (best == null || wl.score[i] > best.second) best = wl.words[i] to wl.score[i]
        }
        if (best != null || proximity == null || part.length < 3) return best
        // Literówka w części słowa („jakdzirn” → „jak dzień”): ta sama długość, jedna pomyłka.
        val key = normalizeKey(part)
        for (lang in langs) {
            val wl = Dictionaries.get(lang) ?: continue
            val bucket = wl.byLength[key.length] ?: continue
            for (i in bucket) {
                val k = wl.keys[i]
                if (k[0] != key[0]) continue
                val d = distance(key, k, 1f, proximity)
                if (d > 1f) continue
                val sc = wl.score[i] - d * 3f
                if (best == null || sc > best.second) best = wl.words[i] to sc
            }
        }
        return best
    }

    /**
     * Wpisane słowo jako dwa słowa: brakująca spacja albo litera z dolnego rzędu trafiona zamiast spacji.
     * Najlepsze propozycje pierwsze (wg słabszego z dwóch słów).
     */
    fun splitWords(typed: String, langs: List<String>, proximity: Proximity? = null): List<String> {
        if (typed.length < 4 || typed.any { !it.isLetter() && it != '\'' && it != '’' }) return emptyList()
        val out = ArrayList<Pair<String, Float>>()
        // Podziały: (pierwsza część, druga część, bonus). Pominięta spacja albo litera
        // z dolnego rzędu zamiast spacji – także po jednoliterowym słowie („wbtej” → „w tej”).
        val parts = ArrayList<Triple<String, String, Float>>()
        for (i in 1 until typed.length) parts += Triple(typed.substring(0, i), typed.substring(i), 0.5f)
        for (i in 1 until typed.length - 1) {
            if (typed[i].lowercaseChar() in NEAR_SPACE) parts += Triple(typed.substring(0, i), typed.substring(i + 1), 0f)
        }
        for ((pa, pb, bonus) in parts) {
            val a = lookup(pa, langs) ?: continue
            val b = lookup(pb, langs) ?: continue
            out += "${a.first} ${b.first}" to minOf(a.second, b.second) + bonus
        }
        // Brak dokładnego podziału – jedna z części z literówką (druga musi być poprawna).
        if (out.isEmpty() && proximity != null) {
            for ((pa, pb, bonus) in parts) {
                val ea = lookup(pa, langs)
                val eb = lookup(pb, langs)
                val a = ea ?: if (eb != null) lookup(pa, langs, proximity) else null
                val b = eb ?: if (ea != null) lookup(pb, langs, proximity) else null
                if (a == null || b == null) continue
                out += "${a.first} ${b.first}" to minOf(a.second, b.second) + bonus - 0.5f
            }
        }
        // Rzadkie słowa (np. pojedyncze litery z końca słownika) dają śmieciowe podziały.
        return out.filter { it.second >= 2.5f }.sortedByDescending { it.second }.map { it.first }.distinct()
    }

    /** Następne słowo po [prev] – z par wyuczonych podczas pisania. */
    fun predict(prev: String?, capitalize: Boolean, limit: Int = 3, langs: List<String> = emptyList(), active: String? = null): Suggestions {
        val out = LinkedHashSet<String>()
        if (!prev.isNullOrBlank()) UserModel.next(prev, limit).forEach { out += it.first }
        // Zawsze coś dalej: brak wyuczonych par → najczęstsze słowa języka (także na początku zdania).
        val wl = (listOfNotNull(active) + langs).firstNotNullOfOrNull { Dictionaries.get(it) }
        if (wl != null) {
            var i = 0
            while (out.size < limit && i < wl.size && i < 400) {
                val w = wl.words[i++]
                if (w.length < 2 && w.lowercase() !in ONE_LETTER) continue
                if (!w.all { it.isLetter() } || UserModel.isBlocked(w) || w.equals(prev, ignoreCase = true)) continue
                if (out.none { it.equals(w, ignoreCase = true) }) out += w
            }
        }
        if (out.isEmpty()) return Suggestions.Empty
        return Suggestions(out.take(limit).map { w -> Suggestion(if (capitalize) w.replaceFirstChar { it.uppercaseChar() } else w, SuggestionKind.PREDICTION) })
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
