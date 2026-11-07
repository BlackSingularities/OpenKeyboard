package com.blacksingularities.openkeyboard.data

import android.content.Context
import android.os.Build
import com.blacksingularities.openkeyboard.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lokalny dziennik awarii – bez wysyłania czegokolwiek. Po awarii zapisuje raport do pliku
 * w pamięci aplikacji; użytkownik może go obejrzeć i sam udostępnić. Seria awarii w krótkim
 * czasie włącza tryb awaryjny: klawiatura startuje na ustawieniach domyślnych, aż użytkownik
 * go wyłączy (zapisane ustawienia zostają nietknięte).
 */
object CrashLog {
    private const val PREFS = "openkeyboard_crash"
    private const val KEY_TIMES = "times"
    private const val KEY_SAFE = "safe_mode"
    private const val DIR = "crashes"
    private const val MAX_REPORTS = 5
    private const val WINDOW_MS = 2 * 60_000L
    private const val THRESHOLD = 3

    private lateinit var appContext: Context
    private val safe = MutableStateFlow(false)
    private val latest = MutableStateFlow<String?>(null)

    /** Tryb awaryjny (seria awarii) – klawiatura na ustawieniach domyślnych. */
    val safeMode: StateFlow<Boolean> get() = safe

    /** Treść ostatniego raportu albo null. */
    val lastReport: StateFlow<String?> get() = latest

    fun install(context: Context) {
        appContext = context.applicationContext
        safe.value = prefs().getBoolean(KEY_SAFE, false)
        latest.value = reports().firstOrNull()?.let { runCatching { it.readText() }.getOrNull() }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { record(thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun dir() = File(appContext.filesDir, DIR).apply { mkdirs() }

    private fun reports(): List<File> =
        dir().listFiles { f -> f.name.endsWith(".txt") }?.sortedByDescending { it.name }.orEmpty()

    internal fun format(thread: String, error: Throwable, now: Long = System.currentTimeMillis()): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date(now))
        return buildString {
            appendLine("OpenKeyboard ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Czas: $time · wątek: $thread")
            appendLine()
            append(trace)
        }
    }

    private fun record(thread: Thread, error: Throwable) {
        val now = System.currentTimeMillis()
        val text = format(thread.name, error, now)
        File(dir(), "crash-$now.txt").writeText(text)
        reports().drop(MAX_REPORTS).forEach { it.delete() }
        // Kilka awarii w krótkim czasie = klawiatura się zapętla → tryb awaryjny.
        val times = prefs().getString(KEY_TIMES, "").orEmpty()
            .split(',').mapNotNull { it.toLongOrNull() }
            .filter { now - it < WINDOW_MS } + now
        val enterSafe = times.size >= THRESHOLD
        prefs().edit()
            .putString(KEY_TIMES, times.joinToString(","))
            .apply { if (enterSafe) putBoolean(KEY_SAFE, true) }
            .commit() // proces zaraz zginie – zapis musi być synchroniczny
    }

    /** Wyjście z trybu awaryjnego – wracają zapisane ustawienia. */
    fun exitSafeMode() {
        prefs().edit().putBoolean(KEY_SAFE, false).remove(KEY_TIMES).apply()
        safe.value = false
    }

    fun clearReports() {
        reports().forEach { it.delete() }
        latest.value = null
    }
}
