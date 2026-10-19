package com.blacksingularities.openkeyboard.keyboard

import androidx.compose.ui.graphics.Color
import com.blacksingularities.openkeyboard.data.KeyboardSettings

/** Kolory klawiatury. [accent] – kolor wyróżnień (Shift, Enter, aktywne elementy). */
data class KbColors(
    val background: Color,
    val key: Color,
    val keySpecial: Color,
    val keyPressed: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val onAccent: Color,
    val strip: Color,
    val popup: Color,
    val light: Boolean,
)

data class KbThemePreset(val id: String, val name: String, val build: (accent: Color) -> KbColors)

object KeyboardThemes {
    /** Akcenty: biel (domyślny, jak w aplikacji) i kilka stonowanych kolorów. */
    val accents: List<Color> = listOf(
        Color(0xFFFFFFFF), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFB74D),
        Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFF4DD0E1), Color(0xFFF06292),
        Color(0xFFFFD54F), Color(0xFFA1887F),
    )

    val presets: List<KbThemePreset> = listOf(
        KbThemePreset("black", "Czarny") { a ->
            KbColors(Color(0xFF000000), Color(0xFF161616), Color(0xFF0B0B0B), Color(0xFF2E2E2E), Color.White, Color(0x99FFFFFF), a, onAccent(a), Color(0xFF000000), Color(0xFF1F1F1F), false)
        },
        KbThemePreset("amoled", "AMOLED") { a ->
            KbColors(Color(0xFF000000), Color(0xFF000000), Color(0xFF000000), Color(0xFF1C1C1C), Color.White, Color(0x80FFFFFF), a, onAccent(a), Color(0xFF000000), Color(0xFF141414), false)
        },
        KbThemePreset("graphite", "Grafit") { a ->
            KbColors(Color(0xFF1B1C1E), Color(0xFF2B2C30), Color(0xFF222326), Color(0xFF45464B), Color.White, Color(0x99FFFFFF), a, onAccent(a), Color(0xFF1B1C1E), Color(0xFF34353A), false)
        },
        KbThemePreset("midnight", "Granat") { a ->
            KbColors(Color(0xFF0A0F1E), Color(0xFF161E33), Color(0xFF10172A), Color(0xFF26304D), Color(0xFFE8EEFF), Color(0x99E8EEFF), a, onAccent(a), Color(0xFF0A0F1E), Color(0xFF1D2640), false)
        },
        KbThemePreset("forest", "Las") { a ->
            KbColors(Color(0xFF0C1410), Color(0xFF17241D), Color(0xFF111B16), Color(0xFF2A3D32), Color(0xFFE6F2EA), Color(0x99E6F2EA), a, onAccent(a), Color(0xFF0C1410), Color(0xFF1E2E25), false)
        },
        KbThemePreset("light", "Jasny") { a ->
            val acc = if (a == Color(0xFFFFFFFF)) Color(0xFF111111) else a
            KbColors(Color(0xFFE9EAEE), Color(0xFFFFFFFF), Color(0xFFD5D7DD), Color(0xFFC3C6CE), Color(0xFF111111), Color(0x99111111), acc, onAccent(acc), Color(0xFFE9EAEE), Color(0xFFFFFFFF), true)
        },
        KbThemePreset("paper", "Papier") { a ->
            val acc = if (a == Color(0xFFFFFFFF)) Color(0xFF3E3A33) else a
            KbColors(Color(0xFFF3EEE3), Color(0xFFFBF8F1), Color(0xFFE6DFD0), Color(0xFFD8CFBC), Color(0xFF2B2722), Color(0x992B2722), acc, onAccent(acc), Color(0xFFF3EEE3), Color(0xFFFBF8F1), true)
        },
    )

    private fun onAccent(a: Color): Color {
        val l = 0.299f * a.red + 0.587f * a.green + 0.114f * a.blue
        return if (l > 0.6f) Color.Black else Color.White
    }

    /** Motyw złożony z kolorów wybranych przez użytkownika. */
    fun custom(s: KeyboardSettings): KbColors {
        val bg = Color(s.customBg.toInt())
        val key = Color(s.customKey.toInt())
        val text = Color(s.customText.toInt())
        val accent = Color(s.customAccent.toInt())
        val light = luminance(bg) > 0.55f
        fun shift(c: Color, d: Float) = if (light) lerp(c, Color.Black, d) else lerp(c, Color.White, d)
        return KbColors(
            background = bg,
            key = key,
            keySpecial = lerp(key, bg, 0.45f),
            keyPressed = shift(key, 0.18f),
            text = text,
            textDim = text.copy(alpha = 0.6f),
            accent = accent,
            onAccent = onAccent(accent),
            strip = bg,
            popup = shift(key, 0.08f),
            light = light,
        )
    }

    fun luminance(c: Color): Float = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue

    private fun lerp(a: Color, b: Color, t: Float) = Color(
        a.red + (b.red - a.red) * t,
        a.green + (b.green - a.green) * t,
        a.blue + (b.blue - a.blue) * t,
        a.alpha,
    )

    fun colors(s: KeyboardSettings): KbColors {
        if (s.theme == "custom") return custom(s)
        val preset = presets.firstOrNull { it.id == s.theme } ?: presets.first()
        return preset.build(accents.getOrElse(s.accent) { accents.first() })
    }
}
