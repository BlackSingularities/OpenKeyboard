package com.blacksingularities.openkeyboard.features

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Clip(val text: String, val time: Long, val pinned: Boolean = false)

/**
 * Historia schowka: klawiatura (domyślna metoda wprowadzania) widzi zmiany schowka systemu.
 * Przypięte wpisy zostają na stałe, reszta wypada po przekroczeniu limitu.
 */
object ClipboardHistory {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File
    private val _items = MutableStateFlow<List<Clip>>(emptyList())
    val items: StateFlow<List<Clip>> get() = _items

    /** Ostatnio skopiowany tekst i czas – do podpowiedzi „wklej” w pasku. */
    var lastCopy: Clip? = null
        private set

    private var manager: ClipboardManager? = null
    private val listener = ClipboardManager.OnPrimaryClipChangedListener { capture() }

    fun init(context: Context) {
        file = File(context.filesDir, "clipboard.json")
        _items.value = runCatching { json.decodeFromString(ListSerializer(Clip.serializer()), file.readText()) }.getOrDefault(emptyList())
    }

    fun attach(context: Context) {
        if (manager != null) return
        manager = context.getSystemService(ClipboardManager::class.java)?.also { it.addPrimaryClipChangedListener(listener) }
    }

    fun detach() {
        manager?.removePrimaryClipChangedListener(listener)
        manager = null
    }

    var enabled = true
    var limit = 60

    /** Wstrzymanie zapisu (pole hasła, tryb incognito). */
    var paused = false

    private fun capture() {
        val m = manager ?: return
        val clip = runCatching { m.primaryClip }.getOrNull() ?: return
        // Dane oznaczone przez aplikację jako poufne (hasła z menedżerów haseł) – pomijamy.
        val sensitive = runCatching {
            clip.description.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true
        }.getOrDefault(false)
        if (sensitive || clip.itemCount == 0) return
        val text = clip.getItemAt(0).text?.toString()?.takeIf { it.isNotBlank() } ?: return
        val c = Clip(text.take(20_000), System.currentTimeMillis())
        lastCopy = c
        if (!enabled || paused) return
        add(c)
    }

    fun add(c: Clip) {
        val list = _items.value.filter { it.text != c.text || it.pinned }
        val pinnedSame = _items.value.any { it.text == c.text && it.pinned }
        val next = if (pinnedSame) list else listOf(c) + list
        val pinned = next.filter { it.pinned }
        val rest = next.filter { !it.pinned }.take(limit)
        _items.value = (next.filter { it in pinned || it in rest })
        save()
    }

    fun togglePin(c: Clip) {
        _items.value = _items.value.map { if (it == c) it.copy(pinned = !it.pinned) else it }
        save()
    }

    fun remove(c: Clip) {
        _items.value = _items.value - c
        save()
    }

    fun clear(keepPinned: Boolean = true) {
        _items.value = if (keepPinned) _items.value.filter { it.pinned } else emptyList()
        lastCopy = null
        save()
    }

    fun consumeRecent(windowMs: Long = 90_000): Clip? {
        val c = lastCopy ?: return null
        return if (System.currentTimeMillis() - c.time < windowMs) c else null
    }

    fun dismissRecent() {
        lastCopy = null
    }

    private fun save() {
        val snapshot = _items.value
        scope.launch { runCatching { file.writeText(json.encodeToString(ListSerializer(Clip.serializer()), snapshot)) } }
    }
}
