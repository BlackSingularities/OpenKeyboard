package com.blacksingularities.openkeyboard.dict

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Shortcut(val trigger: String, val expansion: String)

@Serializable
data class Stats(
    val words: Long = 0,
    val chars: Long = 0,
    val corrections: Long = 0,
    val suggestionsPicked: Long = 0,
    val charsSaved: Long = 0,
    val emojis: Long = 0,
    /** Czas aktywnego pisania (przerwy powyżej kilku sekund się nie liczą). */
    val typingMs: Long = 0,
    /** Naciśnięcia poszczególnych liter – mapa cieplna klawiatury. */
    val keyTaps: Map<String, Long> = emptyMap(),
)

@Serializable
private data class UserData(
    val words: Map<String, Int> = emptyMap(),
    val bigrams: Map<String, Map<String, Int>> = emptyMap(),
    val blocked: Set<String> = emptySet(),
    val shortcuts: List<Shortcut> = emptyList(),
    val stats: Stats = Stats(),
    val emojiRecent: List<String> = emptyList(),
    val rejected: Set<String> = emptySet(),
)

/**
 * Model użytkownika: wyuczone słowa, pary słów (przewidywanie następnego słowa), słowa
 * zablokowane, skróty tekstowe, ostatnie emoji i statystyki. Zapis do pliku z opóźnieniem.
 */
object UserModel {
    private const val MAX_WORDS = 20_000
    private const val MAX_BIGRAM_HEADS = 8_000
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    private lateinit var file: File

    private val words = HashMap<String, Int>()
    private val bigrams = HashMap<String, HashMap<String, Int>>()
    private val blocked = HashSet<String>()
    private val normCache = HashMap<String, String>()
    /** Poprawki cofnięte przez użytkownika: „wpisane\u0001poprawione”. */
    private val rejected = HashSet<String>()
    private val _shortcuts = MutableStateFlow<List<Shortcut>>(emptyList())
    val shortcuts: StateFlow<List<Shortcut>> get() = _shortcuts
    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> get() = _stats
    private val _emojiRecent = MutableStateFlow<List<String>>(emptyList())
    val emojiRecent: StateFlow<List<String>> get() = _emojiRecent

    /** Zmienia się przy każdej zmianie listy słów – odświeża ekran słownika. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> get() = _version

    private val defaultShortcuts = listOf(
        Shortcut("omw", "On my way!"),
        Shortcut("pzdr", "Pozdrawiam"),
        Shortcut("dzk", "Dziękuję!"),
        Shortcut("btw", "by the way"),
    )

    fun init(context: Context) {
        file = File(context.filesDir, "user_model.json")
        val data = runCatching { json.decodeFromString(UserData.serializer(), file.readText()) }.getOrNull()
        synchronized(this) {
            words.clear()
            bigrams.clear()
            blocked.clear()
            if (data != null) {
                words.putAll(data.words)
                data.bigrams.forEach { (k, v) -> bigrams[k] = HashMap(v) }
                blocked.addAll(data.blocked)
                rejected.addAll(data.rejected)
                _shortcuts.value = data.shortcuts
                _stats.value = data.stats
                _emojiRecent.value = data.emojiRecent
            } else {
                _shortcuts.value = defaultShortcuts
            }
        }
    }

    private fun snapshot(): UserData = synchronized(this) {
        UserData(HashMap(words), bigrams.mapValues { HashMap(it.value) }, HashSet(blocked), _shortcuts.value, _stats.value, _emojiRecent.value, HashSet(rejected))
    }

    private fun scheduleSave() {
        _version.value++
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1500)
            saveNow()
        }
    }

    fun saveNow() {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(UserData.serializer(), snapshot()))
            tmp.renameTo(file)
        }
    }

    fun count(word: String): Int = synchronized(this) { words[word] ?: words[word.lowercase()] ?: 0 }

    /** Użytkownik cofnął poprawkę – następnym razem zostaw wpisane słowo. */
    fun rejectCorrection(typed: String, corrected: String) {
        synchronized(this) {
            rejected.add(typed.lowercase() + "\u0001" + corrected.lowercase())
            words[typed] = maxOf(words[typed] ?: 0, 2)
        }
        scheduleSave()
    }

    fun isRejected(typed: String, corrected: String): Boolean =
        synchronized(this) { (typed.lowercase() + "\u0001" + corrected.lowercase()) in rejected }

    fun isBlocked(word: String): Boolean = synchronized(this) { word.lowercase() in blocked }

    fun learn(word: String, prev: String?) {
        if (word.length < 2 || word.length > 40 || word.any { it.isDigit() }) return
        synchronized(this) {
            words[word] = (words[word] ?: 0) + 1
            if (prev != null) {
                val head = prev.lowercase()
                val map = bigrams.getOrPut(head) { HashMap() }
                map[word] = (map[word] ?: 0) + 1
                if (map.size > 40) map.entries.minByOrNull { it.value }?.let { map.remove(it.key) }
            }
            if (words.size > MAX_WORDS) words.entries.sortedBy { it.value }.take(words.size - MAX_WORDS).forEach { words.remove(it.key) }
            if (bigrams.size > MAX_BIGRAM_HEADS) bigrams.entries.sortedBy { it.value.values.sum() }.take(bigrams.size - MAX_BIGRAM_HEADS).forEach { bigrams.remove(it.key) }
        }
        scheduleSave()
    }

    /** Słowa użytkownika zaczynające się od klucza (znormalizowanego). */
    fun withPrefix(keyPrefix: String, limit: Int): List<Pair<String, Int>> = synchronized(this) {
        words.entries.asSequence()
            .filter { normCache.getOrPut(it.key) { normalizeKey(it.key) }.startsWith(keyPrefix) && it.key.lowercase() !in blocked }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
            .toList()
    }

    fun allWords(): List<Pair<String, Int>> = synchronized(this) { words.entries.map { it.key to it.value }.sortedByDescending { it.second } }

    fun blockedWords(): List<String> = synchronized(this) { blocked.sorted() }

    fun next(prev: String, limit: Int): List<Pair<String, Int>> = synchronized(this) {
        bigrams[prev.lowercase()]?.entries
            ?.filter { it.key.lowercase() !in blocked }
            ?.sortedByDescending { it.value }
            ?.take(limit)
            ?.map { it.key to it.value }
            .orEmpty()
    }

    fun bigramCount(prev: String?, word: String): Int = if (prev == null) 0 else synchronized(this) { bigrams[prev.lowercase()]?.get(word) ?: 0 }

    fun addWord(word: String) {
        synchronized(this) {
            blocked.remove(word.lowercase())
            words[word] = maxOf(words[word] ?: 0, 5)
        }
        scheduleSave()
    }

    fun removeWord(word: String) {
        synchronized(this) {
            words.remove(word)
            bigrams.values.forEach { it.remove(word) }
        }
        scheduleSave()
    }

    /** Słowo nie będzie już podpowiadane (także ze słownika ogólnego). */
    fun block(word: String) {
        synchronized(this) {
            blocked.add(word.lowercase())
            words.remove(word)
        }
        scheduleSave()
    }

    fun unblock(word: String) {
        synchronized(this) { blocked.remove(word.lowercase()) }
        scheduleSave()
    }

    fun clearLearned() {
        synchronized(this) {
            words.clear()
            bigrams.clear()
        }
        scheduleSave()
    }

    fun setShortcuts(list: List<Shortcut>) {
        _shortcuts.value = list
        scheduleSave()
    }

    fun shortcutFor(trigger: String): Shortcut? = _shortcuts.value.firstOrNull { it.trigger.equals(trigger, ignoreCase = true) }

    fun addEmojiRecent(e: String) {
        _emojiRecent.value = (listOf(e) + _emojiRecent.value.filter { it != e }).take(48)
        bumpStats { it.copy(emojis = it.emojis + 1) }
    }

    fun bumpStats(transform: (Stats) -> Stats) {
        _stats.value = transform(_stats.value)
        scheduleSave()
    }

    fun resetStats() {
        _stats.value = Stats()
        scheduleSave()
    }

    fun exportJson(): String = json.encodeToString(UserData.serializer(), snapshot())

    fun importJson(raw: String) {
        val data = json.decodeFromString(UserData.serializer(), raw)
        synchronized(this) {
            words.clear(); words.putAll(data.words)
            bigrams.clear(); data.bigrams.forEach { (k, v) -> bigrams[k] = HashMap(v) }
            blocked.clear(); blocked.addAll(data.blocked)
            rejected.clear(); rejected.addAll(data.rejected)
        }
        _shortcuts.value = data.shortcuts
        _stats.value = data.stats
        _emojiRecent.value = data.emojiRecent
        scheduleSave()
    }
}
