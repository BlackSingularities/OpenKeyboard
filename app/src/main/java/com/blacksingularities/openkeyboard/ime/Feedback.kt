package com.blacksingularities.openkeyboard.ime

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.blacksingularities.openkeyboard.data.Prefs

enum class Sound { KEY, DELETE, SPACE, ENTER }

/** Wibracja i dźwięk klawiszy wg ustawień (siła wibracji 1–100, głośność 1–100). */
class Feedback(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    private val audio = context.getSystemService(AudioManager::class.java)

    fun key(sound: Sound = Sound.KEY) {
        val s = Prefs.current
        if (s.haptics) vibrate(s.hapticStrength)
        if (s.sound) {
            val fx = when (sound) {
                Sound.KEY -> AudioManager.FX_KEYPRESS_STANDARD
                Sound.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                Sound.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                Sound.ENTER -> AudioManager.FX_KEYPRESS_RETURN
            }
            runCatching { audio?.playSoundEffect(fx, s.soundVolume / 100f) }
        }
    }

    /** Mocniejszy impuls – przytrzymanie, wejście w tryb kursora itp. */
    fun long() {
        val s = Prefs.current
        if (s.haptics) vibrate((s.hapticStrength * 1.6f).toInt().coerceAtMost(100))
    }

    fun tick() {
        val s = Prefs.current
        if (s.haptics) vibrate((s.hapticStrength * 0.6f).toInt().coerceAtLeast(1))
    }

    private fun vibrate(strength: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (v.hasAmplitudeControl()) {
                val amp = (strength * 2.55f).toInt().coerceIn(1, 255)
                v.vibrate(VibrationEffect.createOneShot(14, amp))
            } else {
                v.vibrate(VibrationEffect.createOneShot((4 + strength / 4).toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
