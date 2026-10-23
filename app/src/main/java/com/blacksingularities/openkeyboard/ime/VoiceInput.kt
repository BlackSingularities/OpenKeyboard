package com.blacksingularities.openkeyboard.ime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_CLIENT -> tr("Usługa mowy jest zajęta – spróbuj za chwilę")
                    12, 13 -> tr("Język nieobsługiwany przez usługę mowy")
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
            // Język, w którym właśnie się pisze (wykryty), a nie wybrany układ klawiszy.
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechTag(c.writingLanguage))
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        // Android 14+: usługa mowy sama przełącza się między włączonymi językami (jeśli ma ich modele).
        val spoken = c.languages.suggestLangs(c.settings).map(::speechTag)
        if (spoken.size > 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
                .putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES, ArrayList(spoken))
        }
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

    /** Usługi mowy lepiej rozumieją „pl-PL” niż samo „pl”. */
    /** Pełny kod dla usługi mowy (pl → pl-PL). */
    private fun speechTag(code: String): String =
        java.util.Locale.forLanguageTag(code).let { l -> if (l.country.isEmpty()) java.util.Locale(l.language, defaultCountry(l.language)).toLanguageTag() else l.toLanguageTag() }

    private fun defaultCountry(lang: String): String = when (lang) {
        "en" -> "US"; "pl" -> "PL"; "de" -> "DE"; "fr" -> "FR"; "es" -> "ES"; "it" -> "IT"; "pt" -> "PT"
        "uk" -> "UA"; "cs" -> "CZ"; "sk" -> "SK"; "sv" -> "SE"; "da" -> "DK"; "nb", "no" -> "NO"; "el" -> "GR"
        "ru" -> "RU"; "nl" -> "NL"; "fi" -> "FI"; "hu" -> "HU"; "ro" -> "RO"; "tr" -> "TR"; "lt" -> "LT"; "hr" -> "HR"
        else -> ""
    }
}
