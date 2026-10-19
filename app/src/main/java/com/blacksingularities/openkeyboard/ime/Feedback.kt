package com.blacksingularities.openkeyboard.ime

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.blacksingularities.openkeyboard.data.KeySound
import com.blacksingularities.openkeyboard.data.Prefs
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

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
    private val synth = KeySynth()

    fun key(sound: Sound = Sound.KEY) {
        val s = Prefs.current
        if (s.haptics) vibrate(s.hapticStrength)
        if (s.sound && s.keySound != KeySound.SYSTEM) {
            synth.play(s.keySound, sound, s.soundVolume / 100f)
        } else if (s.sound) {
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

/**
 * Własne dźwięki klawiszy, generowane w pamięci (bez plików): krótkie próbki PCM odtwarzane
 * przez AudioTrack w trybie statycznym – opóźnienie praktycznie zerowe.
 */
private class KeySynth {
    private val rate = 44_100
    private val tracks = HashMap<Pair<KeySound, Boolean>, AudioTrack>()
    private val rnd = java.util.Random(7)

    fun play(kind: KeySound, sound: Sound, volume: Float) {
        val heavy = sound == Sound.SPACE || sound == Sound.ENTER || sound == Sound.DELETE
        val t = tracks.getOrPut(kind to heavy) { build(kind, heavy) }
        runCatching {
            t.setVolume(volume.coerceIn(0f, 1f))
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.playbackHeadPosition = 0
            t.play()
        }
    }

    private fun build(kind: KeySound, heavy: Boolean): AudioTrack {
        val pcm = samples(kind, heavy)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    private fun samples(kind: KeySound, heavy: Boolean): ShortArray {
        val ms = when (kind) {
            KeySound.CLICK -> 14
            KeySound.SOFT -> 40
            KeySound.TYPEWRITER -> 45
            KeySound.BUBBLE -> 55
            KeySound.SYSTEM -> 10
        } + if (heavy) 15 else 0
        val n = rate * ms / 1000
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i / rate.toFloat()
            val v = when (kind) {
                // Krótki trzask szumu z bardzo szybkim wygaszeniem.
                KeySound.CLICK -> (rnd.nextFloat() * 2 - 1) * exp(-t * 900f) * 0.9f
                // Miękkie „puk”: niska sinusoida z łagodnym wybrzmieniem.
                KeySound.SOFT -> sin(2 * PI * (if (heavy) 320 else 520) * t).toFloat() * exp(-t * 110f) * 0.8f
                // Maszyna do pisania: uderzenie metalu + głuchy rezonans.
                KeySound.TYPEWRITER -> ((rnd.nextFloat() * 2 - 1) * exp(-t * 500f) * 0.7f +
                    sin(2 * PI * (if (heavy) 110 else 180) * t).toFloat() * exp(-t * 70f) * 0.5f)
                // Bańka: ton przesuwający się w górę.
                KeySound.BUBBLE -> sin(2 * PI * (380 + 9000 * t) * t).toFloat() * exp(-t * 60f) * 0.6f
                KeySound.SYSTEM -> 0f
            }
            out[i] = (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
