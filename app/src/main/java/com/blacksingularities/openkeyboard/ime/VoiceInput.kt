package com.blacksingularities.openkeyboard.ime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blacksingularities.openkeyboard.i18n.tr

enum class VoiceState { IDLE, LISTENING, NO_PERMISSION, UNAVAILABLE, ERROR }

/**
 * Dyktowanie: rozpoznawanie mowy systemu (Google / inne usługi) bezpośrednio w klawiaturze,
 * z podglądem na żywo. Język = bieżący język pisania.
 */
class VoiceInput(private val c: KeyboardController) {
    var state by mutableStateOf(VoiceState.IDLE)
        private set
    var partial by mutableStateOf("")
        private set
    var level by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var recognizer: SpeechRecognizer? = null

    fun start() {
        val ctx = c.service
        if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            state = VoiceState.NO_PERMISSION
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            state = VoiceState.UNAVAILABLE
            return
        }
        stop()
        partial = ""
        error = null
        val r = SpeechRecognizer.createSpeechRecognizer(ctx)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                state = VoiceState.LISTENING
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(code: Int) {
                error = when (code) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> tr("Nic nie usłyszałem – spróbuj jeszcze raz")
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> tr("Brak połączenia z usługą rozpoznawania mowy")
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> tr("Brak uprawnienia do mikrofonu")
                    else -> tr("Błąd rozpoznawania mowy ({0})", code)
                }
                state = VoiceState.ERROR
                level = 0f
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                partial = ""
                level = 0f
                c.commitVoice(text)
                state = VoiceState.IDLE
                c.closePanel()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, c.activeLanguage)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        state = VoiceState.LISTENING
        runCatching { r.startListening(intent) }.onFailure {
            state = VoiceState.ERROR
            error = it.message
        }
    }

    /** Zakończ słuchanie i wstaw to, co już rozpoznano. */
    fun finish() {
        recognizer?.stopListening()
    }

    fun stop() {
        recognizer?.let {
            runCatching { it.cancel() }
            runCatching { it.destroy() }
        }
        recognizer = null
        if (state == VoiceState.LISTENING) state = VoiceState.IDLE
        level = 0f
    }

    fun destroy() = stop()
}
