package com.blacksingularities.openkeyboard.dict

import android.content.Context
import com.blacksingularities.openkeyboard.BuildConfig
import com.blacksingularities.openkeyboard.data.Languages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Stan słownika języka (dla ekranu języków). */
sealed interface DictState {
    data object Missing : DictState
    data object Unavailable : DictState
    data class Downloading(val progress: Float) : DictState
    data object Loading : DictState
    data class Ready(val words: Int, val bundled: Boolean) : DictState
    data class Failed(val message: String) : DictState
}

/**
 * Słowniki: wbudowane (polski, angielski) i pobierane na żądanie z FrequencyWords
 * (github.com/hermitdave/FrequencyWords – 50 tys. najczęstszych słów, CC-BY-SA 4.0).
 */
object Dictionaries {
    private const val SOURCE = "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/%s/%s_50k.txt"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loaded = ConcurrentHashMap<String, WordList>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private lateinit var appContext: Context

    private val _states = MutableStateFlow<Map<String, DictState>>(emptyMap())
    val states: StateFlow<Map<String, DictState>> get() = _states

    fun init(context: Context) {
        appContext = context.applicationContext
        refreshStates()
    }

    private fun file(code: String) = File(File(appContext.filesDir, "dict").apply { mkdirs() }, "$code.txt.gz")

    private fun setState(code: String, s: DictState) {
        _states.value = _states.value + (code to s)
    }

    fun refreshStates() {
        val m = HashMap<String, DictState>()
        for (l in Languages.all) {
            m[l.code] = when {
                loaded[l.code] != null -> DictState.Ready(loaded.getValue(l.code).size, l.bundled && !file(l.code).exists())
                file(l.code).exists() -> DictState.Ready(0, false)
                l.bundled -> DictState.Ready(0, true)
                l.dict.isEmpty() -> DictState.Unavailable
                else -> DictState.Missing
            }
        }
        _states.value = m
    }

    fun get(code: String): WordList? = loaded[code]

    fun isAvailable(code: String): Boolean {
        val l = Languages.get(code)
        return l.bundled || file(code).exists()
    }

    /** Wczytuje słowniki języków w tle (bez pobierania z sieci). */
    fun ensureLoaded(codes: Collection<String>) {
        for (c in codes) if (loaded[c] == null && isAvailable(c)) scope.launch { load(c) }
        // Nieużywane słowniki zwalniają pamięć klawiatury.
        loaded.keys.filter { it !in codes }.forEach { loaded.remove(it) }
    }

    suspend fun load(code: String): WordList? = locks.getOrPut(code) { Mutex() }.withLock {
        loaded[code]?.let { return@withLock it }
        val f = file(code)
        val lang = Languages.get(code)
        if (!f.exists() && !lang.bundled) return@withLock null
        setState(code, DictState.Loading)
        val list = runCatching {
            val stream = if (f.exists()) f.inputStream() else appContext.assets.open("dict/$code.dict")
            GZIPInputStream(stream.buffered()).bufferedReader().useLines { WordList.parse(code, it) }
        }.getOrElse {
            setState(code, DictState.Failed(it.message ?: "?"))
            return@withLock null
        }
        loaded[code] = list
        setState(code, DictState.Ready(list.size, lang.bundled && !f.exists()))
        list
    }

    /** Pobiera listę słów z sieci, filtruje śmieci (liczby, pojedyncze litery) i zapisuje lokalnie. */
    fun download(code: String) {
        val lang = Languages.get(code)
        if (lang.dict.isEmpty()) return
        scope.launch {
            setState(code, DictState.Downloading(0f))
            runCatching {
                val url = SOURCE.format(lang.dict, lang.dict)
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "OpenKeyboard/" + BuildConfig.VERSION_NAME)
                }
                try {
                    if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: 700_000L
                    val tmp = File(file(code).parentFile, "$code.part")
                    var read = 0L
                    var kept = 0
                    GZIPOutputStream(tmp.outputStream().buffered()).bufferedWriter().use { out ->
                        conn.inputStream.bufferedReader().useLines { lines ->
                            for (line in lines) {
                                ensureActive()
                                read += line.length + 1
                                if (kept % 500 == 0) setState(code, DictState.Downloading((read.toFloat() / total).coerceIn(0f, 0.99f)))
                                val sp = line.lastIndexOf(' ').takeIf { it > 0 } ?: continue
                                val word = line.substring(0, sp)
                                val count = line.substring(sp + 1).toIntOrNull() ?: continue
                                if (!isWord(word)) continue
                                out.write(word)
                                out.write("\t")
                                out.write(count.toString())
                                out.write("\n")
                                kept++
                            }
                        }
                    }
                    if (kept < 100) error("Za mało słów")
                    if (!tmp.renameTo(file(code))) error("Nie można zapisać słownika")
                } finally {
                    conn.disconnect()
                }
                loaded.remove(code)
                load(code)
            }.onFailure { setState(code, DictState.Failed(it.message ?: it.javaClass.simpleName)) }
        }
    }

    fun delete(code: String) {
        file(code).delete()
        loaded.remove(code)
        refreshStates()
    }

    private fun isWord(w: String): Boolean {
        if (w.isEmpty() || w.length > 40) return false
        if (w.length == 1 && w !in setOf("a", "i", "o", "u", "w", "y", "e", "à", "è", "é", "å", "ä", "ö", "и", "в", "с", "к", "у", "о", "а", "я")) return false
        return w.all { it.isLetter() || it == '\'' || it == '-' } && w.first().isLetter()
    }

    fun storageBytes(): Long = File(appContext.filesDir, "dict").listFiles()?.sumOf { it.length() } ?: 0L

    suspend fun awaitLoaded(codes: Collection<String>) = withContext(Dispatchers.IO) { codes.forEach { load(it) } }
}
