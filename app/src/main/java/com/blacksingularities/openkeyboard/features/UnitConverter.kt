package com.blacksingularities.openkeyboard.features

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Przeliczanie jednostek w pasku podpowiedzi, bez internetu: „5 km” → „3,11 mi”, „20°C” → „68 °F”,
 * „3 kg” → „6,61 lb”. Liczba z jednostką musi stać tuż przed kursorem (spacja na końcu dozwolona).
 */
object UnitConverter {
    private class Unit(val names: List<String>, val target: String, val convert: (Double) -> Double)

    private val units = listOf(
        Unit(listOf("km"), "mi") { it / 1.609344 },
        Unit(listOf("mi", "mile", "miles"), "km") { it * 1.609344 },
        Unit(listOf("m"), "ft") { it / 0.3048 },
        Unit(listOf("ft", "feet"), "m") { it * 0.3048 },
        Unit(listOf("cm"), "in") { it / 2.54 },
        Unit(listOf("in", "inch", "cali"), "cm") { it * 2.54 },
        Unit(listOf("kg"), "lb") { it / 0.45359237 },
        Unit(listOf("lb", "lbs"), "kg") { it * 0.45359237 },
        Unit(listOf("g"), "oz") { it / 28.349523125 },
        Unit(listOf("oz"), "g") { it * 28.349523125 },
        Unit(listOf("l"), "gal") { it / 3.785411784 },
        Unit(listOf("gal"), "l") { it * 3.785411784 },
        Unit(listOf("°c", "c", "℃"), "°F") { it * 9 / 5 + 32 },
        Unit(listOf("°f", "f", "℉"), "°C") { (it - 32) * 5 / 9 },
        Unit(listOf("km/h", "kmh"), "mph") { it / 1.609344 },
        Unit(listOf("mph"), "km/h") { it * 1.609344 },
        Unit(listOf("kw"), "KM") { it * 1.35962 },
        Unit(listOf("hp", "ps"), "kW") { it / 1.35962 },
    )

    private val pattern = Regex("""(?<![\p{L}\d.,])(-?\d+(?:[.,]\d+)?)\s?(°?[\p{L}℃℉/]{1,6})\s?$""")

    /** Wynik przeliczenia (np. „3,11 mi”) albo null, gdy przed kursorem nie ma liczby z jednostką. */
    fun find(before: String, lang: String = "en"): String? {
        val m = pattern.find(before) ?: return null
        val number = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val unitName = m.groupValues[2].lowercase()
        // Same „c”/„f” tylko bez spacji po liczbie („20c”) albo ze znakiem stopnia („20 °C”).
        val unit = units.firstOrNull { unitName in it.names } ?: return null
        if (unitName.length == 1 && unitName in setOf("c", "f") && m.value.contains(' ')) return null
        return format(unit.convert(number), lang) + if (unit.target.startsWith("°")) unit.target else " " + unit.target
    }

    private fun format(v: Double, lang: String): String {
        val r = BigDecimal(v).round(MathContext(4, RoundingMode.HALF_UP))
        val decimals = when {
            kotlin.math.abs(v) >= 100 -> 0
            kotlin.math.abs(v) >= 10 -> 1
            else -> 2
        }
        val scaled = r.setScale(decimals, RoundingMode.HALF_UP).let { if (decimals > 0) it.stripTrailingZeros() else it }
        val s = scaled.toPlainString()
        return if (lang in setOf("en", "ja", "zh", "ko", "he")) s else s.replace('.', ',')
    }
}
