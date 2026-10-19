package com.blacksingularities.openkeyboard.keyboard

import androidx.compose.ui.graphics.Color
import com.blacksingularities.openkeyboard.data.KeyboardSettings

/**
 * Kolory klawiatury. [accent] – kolor wyróżnień (Shift, Enter, aktywne elementy),
 * [background2] – drugi kolor gradientu tła (null = jednolite), [flat] – klawisze bez pól
 * i obramowań, same znaki (także Enter i Shift).
 */
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
    val background2: Color? = null,
    val flat: Boolean = false,
)

/** Motyw z galerii; [dark] – do automatycznego wyboru motywu nocnego/dziennego. */
data class KbThemePreset(val id: String, val name: String, val dark: Boolean = true, val build: (accent: Color) -> KbColors)

object KeyboardThemes {
    /** Akcenty: biel (domyślny, jak w aplikacji) i kilka stonowanych kolorów. */
    val accents: List<Color> = listOf(
        Color(0xFFFFFFFF), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFB74D),
        Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFF4DD0E1), Color(0xFFF06292),
        Color(0xFFFFD54F), Color(0xFFA1887F),
    )

    private fun dark(bg: Long, key: Long, special: Long, pressed: Long, text: Long, a: Color, popup: Long, bg2: Long? = null) = KbColors(
        Color(bg), Color(key), Color(special), Color(pressed), Color(text), Color(text).copy(alpha = 0.6f),
        a, onAccent(a), Color(bg), Color(popup), false, bg2?.let { Color(it) },
    )

    private fun light(bg: Long, key: Long, special: Long, pressed: Long, text: Long, a: Color, defAccent: Long, bg2: Long? = null): KbColors {
        val acc = if (a == Color(0xFFFFFFFF)) Color(defAccent) else a
        return KbColors(
            Color(bg), Color(key), Color(special), Color(pressed), Color(text), Color(text).copy(alpha = 0.6f),
            acc, onAccent(acc), Color(bg), Color(key), true, bg2?.let { Color(it) },
        )
    }

    val presets: List<KbThemePreset> = listOf(
        KbThemePreset("black", "Czarny") { a -> dark(0xFF000000, 0xFF161616, 0xFF0B0B0B, 0xFF2E2E2E, 0xFFFFFFFF, a, 0xFF1F1F1F) },
        KbThemePreset("flat", "Płaski czarny") { a ->
            dark(0xFF000000, 0xFF000000, 0xFF000000, 0xFF1A1A1A, 0xFFFFFFFF, a, 0xFF161616).copy(flat = true)
        },
        KbThemePreset("amoled", "AMOLED") { a -> dark(0xFF000000, 0xFF000000, 0xFF000000, 0xFF1C1C1C, 0xFFFFFFFF, a, 0xFF141414) },
        KbThemePreset("graphite", "Grafit") { a -> dark(0xFF1B1C1E, 0xFF2B2C30, 0xFF222326, 0xFF45464B, 0xFFFFFFFF, a, 0xFF34353A) },
        KbThemePreset("midnight", "Granat") { a -> dark(0xFF0A0F1E, 0xFF161E33, 0xFF10172A, 0xFF26304D, 0xFFE8EEFF, a, 0xFF1D2640) },
        KbThemePreset("forest", "Las") { a -> dark(0xFF0C1410, 0xFF17241D, 0xFF111B16, 0xFF2A3D32, 0xFFE6F2EA, a, 0xFF1E2E25) },
        KbThemePreset("wine", "Wino") { a -> dark(0xFF1A0A10, 0xFF2C1520, 0xFF221019, 0xFF45253A, 0xFFF6E7EE, a, 0xFF341A27) },
        KbThemePreset("ocean", "Ocean") { a -> dark(0xFF04263B, 0xFF0A3550, 0xFF072D44, 0xFF14506F, 0xFFE3F4FF, a, 0xFF0D3F5E, 0xFF011522) },
        KbThemePreset("sunset", "Zachód") { a -> dark(0xFF3A1036, 0xFF4C1A45, 0xFF43143D, 0xFF6B2E5F, 0xFFFFEFF5, a, 0xFF5A2152, 0xFF8A2A1D) },
        KbThemePreset("aurora", "Zorza") { a -> dark(0xFF06201F, 0xFF0D302D, 0xFF0A2826, 0xFF1B4A45, 0xFFE4FFF9, a, 0xFF12403B, 0xFF1A1340) },
        KbThemePreset("nebula", "Mgławica") { a -> dark(0xFF120B2A, 0xFF1F1640, 0xFF191136, 0xFF332A5E, 0xFFF0EBFF, a, 0xFF261C4F, 0xFF2A0B23) },
        KbThemePreset("ember", "Żar") { a -> dark(0xFF1A0E07, 0xFF2A180D, 0xFF22140A, 0xFF4A2C17, 0xFFFFEEDD, a, 0xFF3A2212, 0xFF3D0F05) },
        KbThemePreset("light", "Jasny", dark = false) { a -> light(0xFFE9EAEE, 0xFFFFFFFF, 0xFFD5D7DD, 0xFFC3C6CE, 0xFF111111, a, 0xFF111111) },
        KbThemePreset("paper", "Papier", dark = false) { a -> light(0xFFF3EEE3, 0xFFFBF8F1, 0xFFE6DFD0, 0xFFD8CFBC, 0xFF2B2722, a, 0xFF3E3A33) },
        KbThemePreset("mint", "Mięta", dark = false) { a -> light(0xFFE3F4EE, 0xFFF8FFFC, 0xFFCDE8DE, 0xFFB5DACB, 0xFF103026, a, 0xFF1F7A5C) },
        KbThemePreset("sky", "Niebo", dark = false) { a -> light(0xFFDCEBFA, 0xFFF7FBFF, 0xFFC6DCF2, 0xFFAECBE8, 0xFF0E2236, a, 0xFF2A6FB0, 0xFFF4E3F2) },
        KbThemePreset("peach", "Brzoskwinia", dark = false) { a -> light(0xFFFBE4D8, 0xFFFFF7F2, 0xFFF2D0BE, 0xFFE8BBA5, 0xFF3A1E12, a, 0xFFC5532B, 0xFFF6D6E6) },
        KbThemePreset("lightflat", "Płaski jasny", dark = false) { a ->
            light(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFE6E6E6, 0xFF111111, a, 0xFF111111).copy(flat = true)
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
            background2 = if (s.customGradient) Color(s.customBg2.toInt()) else null,
        )
    }

    fun luminance(c: Color): Float = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue

    private fun lerp(a: Color, b: Color, t: Float) = Color(
        a.red + (b.red - a.red) * t,
        a.green + (b.green - a.green) * t,
        a.blue + (b.blue - a.blue) * t,
        a.alpha,
    )

    /** Motyw do użycia teraz: przy automatycznym przełączaniu – dzienny albo nocny wg systemu. */
    fun themeId(s: KeyboardSettings, systemDark: Boolean): String =
        if (s.autoDayNight) (if (systemDark) s.nightTheme else s.dayTheme) else s.theme

    fun colors(s: KeyboardSettings, systemDark: Boolean = true): KbColors {
        val id = themeId(s, systemDark)
        if (id == "custom") return custom(s)
        val preset = presets.firstOrNull { it.id == id } ?: presets.first()
        return preset.build(accents.getOrElse(s.accent) { accents.first() })
    }
}
