package com.blacksingularities.openkeyboard.features

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.pow

/**
 * Kalkulator w pasku podpowiedzi: „12*3,5+4” albo „(2+3)^2=” → wynik do wstawienia jednym dotknięciem.
 * Obsługuje + − * / × ÷ ^ %, nawiasy, przecinek i kropkę dziesiętną.
 */
object Calculator {
    private val tail = Regex("""[0-9(][0-9.,()+\-*/×÷^%\s]*=?$""")

    /** Wyrażenie na końcu tekstu przed kursorem – albo null, gdy to nie rachunek. */
    fun findExpression(before: String): String? {
        val m = tail.find(before) ?: return null
        val expr = m.value.trimEnd().removeSuffix("=").trim()
        if (expr.length < 3) return null
        // Musi być działanie między liczbami, a nie np. sama data „12-05”.
        if (!Regex("""\d\s*[+\-*/×÷^%]\s*[\d(]""").containsMatchIn(expr)) return null
        if (Regex("""^\d{1,2}[-./]\d{1,2}([-./]\d{2,4})?$""").matches(expr)) return null
        return expr
    }

    fun evaluate(expr: String): String? = runCatching {
        val v = Parser(expr.replace(',', '.').replace('×', '*').replace('÷', '/').replace(" ", "")).parse()
        if (v.isNaN() || v.isInfinite()) return null
        format(v)
    }.getOrNull()

    fun format(v: Double): String {
        if (v == Math.rint(v) && kotlin.math.abs(v) < 1e15) return v.toLong().toString()
        val bd = BigDecimal(v).round(MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros()
        return bd.toPlainString()
    }

    private class Parser(val s: String) {
        var pos = 0

        fun parse(): Double {
            val v = expr()
            if (pos != s.length) error("Nieoczekiwany znak")
            return v
        }

        fun expr(): Double {
            var v = term()
            while (pos < s.length) {
                when (s[pos]) {
                    '+' -> { pos++; v += term() }
                    '-' -> { pos++; v -= term() }
                    else -> return v
                }
            }
            return v
        }

        fun term(): Double {
            var v = factor()
            while (pos < s.length) {
                when (s[pos]) {
                    '*' -> { pos++; v *= factor() }
                    '/' -> { pos++; v /= factor() }
                    else -> return v
                }
            }
            return v
        }

        fun factor(): Double {
            var v = unary()
            if (pos < s.length && s[pos] == '^') {
                pos++
                v = v.pow(factor())
            }
            return v
        }

        fun unary(): Double {
            if (pos < s.length && s[pos] == '-') {
                pos++
                return -unary()
            }
            var v = atom()
            while (pos < s.length && s[pos] == '%') {
                pos++
                v /= 100.0
            }
            return v
        }

        fun atom(): Double {
            if (pos < s.length && s[pos] == '(') {
                pos++
                val v = expr()
                if (pos < s.length && s[pos] == ')') pos++
                return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (start == pos) error("Brak liczby")
            return s.substring(start, pos).toDouble()
        }
    }
}
