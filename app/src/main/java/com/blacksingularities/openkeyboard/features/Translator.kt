package com.blacksingularities.openkeyboard.features

import com.blacksingularities.openkeyboard.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Tłumaczenie przez darmowe API MyMemory (bez klucza, ok. 5000 znaków dziennie).
 * Tekst wysyłany jest tylko po naciśnięciu „Tłumacz” w panelu klawiatury.
 */
object Translator {
    suspend fun translate(text: String, from: String, to: String): String = withContext(Dispatchers.IO) {
        val pair = URLEncoder.encode("${if (from == "auto") "autodetect" else from}|$to", "UTF-8")
        val q = URLEncoder.encode(text.take(500), "UTF-8")
        val conn = (URL("https://api.mymemory.translated.net/get?q=$q&langpair=$pair").openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "OpenKeyboard/" + BuildConfig.VERSION_NAME)
        }
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = Json.parseToJsonElement(body).jsonObject
            val data = root["responseData"]?.jsonObject ?: error("Brak odpowiedzi")
            val out = data["translatedText"]?.jsonPrimitive?.content.orEmpty()
            if (out.isBlank() || out.startsWith("MYMEMORY WARNING")) error(out.ifBlank { "Pusta odpowiedź" })
            out
        } finally {
            conn.disconnect()
        }
    }
}
