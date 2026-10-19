package com.blacksingularities.openkeyboard.dict

import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sqrt

/** Punkt ścieżki palca (w pikselach siatki klawiszy). */
data class Pt(val x: Float, val y: Float)

/**
 * Pisanie gestem: palec przeciąga się po literach słowa. Dla każdego kandydata ze słownika
 * budujemy idealną ścieżkę przez środki jego liter i porównujemy kształt z ruchem palca
 * (ścieżki próbkowane do tej samej liczby punktów). Wynik łączy dopasowanie kształtu,
 * położenie początku i końca oraz częstość słowa (i to, czego nauczył się użytkownik).
 */
object Glide {
    private const val N = 40

    fun decode(
        path: List<Pt>,
        centers: Map<Char, Pt>,
        keyW: Float,
        langs: List<String>,
        active: String,
        prev: String?,
        limit: Int = 5,
    ): List<String> {
        if (path.size < 2 || centers.isEmpty()) return emptyList()
        val user = resample(path, N)
        val first = path.first()
        val last = path.last()
        // Litery, przy których palec mógł zacząć i skończyć.
        val startSet = nearKeys(first, centers, keyW * 1.25f)
        val endSet = nearKeys(last, centers, keyW * 1.25f)
        if (startSet.isEmpty() || endSet.isEmpty()) return emptyList()
        val pathLen = length(path)

        val scores = HashMap<String, Float>()
        fun consider(word: String, key: String, freqScore: Float) {
            if (key.length < 2 || key[0] !in startSet || key.last() !in endSet) return
            // Każda litera słowa musi leżeć blisko ścieżki palca.
            for (ch in key) {
                val c = centers[ch] ?: return
                if (minDist(c, user) > keyW * 1.05f) return
            }
            val ideal = idealPath(key, centers) ?: return
            val idealLen = length(ideal)
            // Słowo o zupełnie innej długości ścieżki odpada od razu.
            if (idealLen > 0f && (pathLen / idealLen > 2.2f || idealLen / pathLen > 2.2f)) return
            val shape = shapeDistance(user, resample(ideal, N)) / keyW
            val ends = (dist(first, ideal.first()) + dist(last, ideal.last())) / keyW
            val s = freqScore * 0.55f - shape * 9f - ends * 2.5f
            val k = word.lowercase()
            if (s > (scores[k] ?: Float.NEGATIVE_INFINITY)) scores[k] = s
        }

        for (lang in langs) {
            val wl = Dictionaries.get(lang) ?: continue
            val bonus = if (lang == active) 0.6f else 0f
            for (i in wl.words.indices) consider(wl.words[i], wl.keys[i], wl.score[i] + bonus)
        }
        for ((w, n) in UserModel.allWords()) consider(w, normalizeKey(w), 6f + ln(n + 1f) * 1.5f)

        // Kontekst: para z poprzednim słowem.
        val ranked = scores.entries.map { (w, s) ->
            val big = UserModel.bigramCount(prev, w)
            w to (s + if (big > 0) 1.5f + ln(big.toFloat()) else 0f)
        }.sortedByDescending { it.second }

        // Zachowaj formę ze słownika (np. „I”, nazwy własne).
        val forms = HashMap<String, String>()
        for (lang in langs) {
            val wl = Dictionaries.get(lang) ?: continue
            for ((w, _) in ranked.take(limit * 2)) if (w !in forms) wl.indexOf(w).takeIf { it >= 0 }?.let { forms[w] = wl.words[it] }
        }
        return ranked.take(limit).map { forms[it.first] ?: it.first }.filter { !UserModel.isBlocked(it) }
    }

    private fun nearKeys(p: Pt, centers: Map<Char, Pt>, r: Float): Set<Char> =
        centers.filter { dist(it.value, p) <= r }.keys

    /** Ścieżka przez środki liter; podwojone litery liczą się raz. */
    private fun idealPath(key: String, centers: Map<Char, Pt>): List<Pt>? {
        val out = ArrayList<Pt>(key.length)
        var prevCh: Char? = null
        for (ch in key) {
            if (ch == prevCh) continue
            out += centers[ch] ?: return null
            prevCh = ch
        }
        if (out.size == 1) out += out[0]
        return out
    }

    private fun dist(a: Pt, b: Pt) = hypot(a.x - b.x, a.y - b.y)

    private fun length(p: List<Pt>): Float {
        var l = 0f
        for (i in 1 until p.size) l += dist(p[i - 1], p[i])
        return l
    }

    private fun minDist(c: Pt, path: List<Pt>): Float {
        var m = Float.MAX_VALUE
        for (p in path) {
            val d = dist(c, p)
            if (d < m) m = d
        }
        return m
    }

    private fun shapeDistance(a: List<Pt>, b: List<Pt>): Float {
        var s = 0f
        for (i in a.indices) s += dist(a[i], b[i])
        return s / a.size
    }

    /** Równomierne próbkowanie łamanej do [n] punktów. */
    fun resample(path: List<Pt>, n: Int): List<Pt> {
        val total = length(path)
        if (total == 0f) return List(n) { path.first() }
        val step = total / (n - 1)
        val out = ArrayList<Pt>(n)
        out += path.first()
        var acc = 0f
        var i = 1
        var prev = path.first()
        while (i < path.size && out.size < n) {
            val cur = path[i]
            val d = dist(prev, cur)
            if (acc + d >= step && d > 0f) {
                val t = (step - acc) / d
                val q = Pt(prev.x + t * (cur.x - prev.x), prev.y + t * (cur.y - prev.y))
                out += q
                prev = q
                acc = 0f
            } else {
                acc += d
                prev = cur
                i++
            }
        }
        while (out.size < n) out += path.last()
        return out
    }

    @Suppress("unused")
    private fun norm(x: Float, y: Float) = sqrt(x * x + y * y)
}
