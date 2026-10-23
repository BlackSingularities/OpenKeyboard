package com.blacksingularities.openkeyboard.ime

import com.blacksingularities.openkeyboard.dict.Proximity
import com.blacksingularities.openkeyboard.dict.normalizeKey
import com.blacksingularities.openkeyboard.keyboard.KeyType
import com.blacksingularities.openkeyboard.keyboard.KeyboardLayout
import java.text.BreakIterator
import kotlin.math.hypot

/** Języki z cudzysłowem otwierającym na dole („…”). */
internal val LOW_QUOTE_LANGS = setOf("pl", "de", "cs", "sk", "hu", "ro", "hr", "lt")

/** Języki zamykające dolny cudzysłów górnym „…“ (polski, węgierski, rumuński – „…”). */
internal val GERMAN_QUOTE_LANGS = setOf("de", "cs", "sk", "lt")

/** Nawiasy i cudzysłowy domykane automatycznie (ustawienie „Domykaj nawiasy”). */
internal val PAIRS = mapOf('(' to ')', '[' to ']', '{' to '}', '„' to '”', '“' to '”', '«' to '»')

/** Typograficzny cudzysłów za prosty [typed] („"” albo „'”) po znaku [prev] w języku [lang]. */
internal fun smartQuote(typed: String, prev: Char?, lang: String): String {
    val opening = prev == null || prev.isWhitespace() || prev in "([{„“‘"
    return if (typed == "\"") {
        when {
            opening -> if (lang in LOW_QUOTE_LANGS) "„" else "“"
            lang in GERMAN_QUOTE_LANGS -> "“"
            else -> "”"
        }
    } else {
        if (opening) "‘" else "’"
    }
}

/** Para do domknięcia za [ch] – „„” zamyka się zależnie od języka. */
internal fun closingFor(ch: Char, lang: String): Char =
    if (ch == '„' && lang in GERMAN_QUOTE_LANGS) '“' else PAIRS.getValue(ch)

/** Sąsiedztwo klawiszy liter w układzie – literówka na sąsiednim klawiszu kosztuje mniej. */
internal fun proximityOf(l: KeyboardLayout): Proximity {
    val centers = HashMap<Char, MutableList<Pair<Float, Float>>>()
    l.rows.forEachIndexed { r, row ->
        var x = (l.units - row.sumOf { it.width.toDouble() }.toFloat()) / 2f
        for (k in row) {
            if (k.type == KeyType.CHAR && k.text.length == 1) {
                val c = normalizeKey(k.text).firstOrNull()
                if (c != null) centers.getOrPut(c) { mutableListOf() } += (x + k.width / 2f) to r.toFloat()
            }
            x += k.width
        }
    }
    val near = HashMap<Char, MutableSet<Char>>()
    val list = centers.entries.toList()
    for ((a, pa) in list) for ((b, pb) in list) {
        if (a == b) continue
        if (pa.any { p -> pb.any { q -> hypot(p.first - q.first, (p.second - q.second) * 1.1f) <= 1.3f } }) near.getOrPut(a) { HashSet() } += b
    }
    return Proximity { x, y -> near[x]?.contains(y) == true }
}

/** Długość ostatniego znaku (grafemu – emoji, litera z akcentem) w [text]. */
internal fun graphemeLengthBefore(text: String): Int {
    if (text.isEmpty()) return 0
    val it = BreakIterator.getCharacterInstance()
    it.setText(text)
    val end = it.last()
    val start = it.previous()
    return if (start == BreakIterator.DONE) 1 else end - start
}

/** Długość ostatniego słowa (z odstępami za nim) albo ciągu znaków interpunkcji w [text]. */
internal fun wordLengthBefore(text: String): Int {
    if (text.isEmpty()) return 0
    var i = text.length
    while (i > 0 && text[i - 1].isWhitespace()) i--
    if (i > 0 && !text[i - 1].isLetterOrDigit()) {
        while (i > 0 && !text[i - 1].isLetterOrDigit() && !text[i - 1].isWhitespace()) i--
    } else {
        while (i > 0 && (text[i - 1].isLetterOrDigit() || text[i - 1] == '\'' || text[i - 1] == '’')) i--
    }
    return (text.length - i).coerceAtLeast(1)
}

internal fun String.dropLastGrapheme(): String = dropLast(graphemeLengthBefore(this))
