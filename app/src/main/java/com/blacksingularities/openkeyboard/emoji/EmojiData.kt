package com.blacksingularities.openkeyboard.emoji

import android.content.Context
import com.blacksingularities.openkeyboard.dict.normalizeKey

data class Emoji(val char: String, val name: String, val tones: List<String>)

data class EmojiGroup(val id: String, val emoji: List<Emoji>)

/**
 * Emoji z oficjalnej listy Unicode (emoji-test.txt): grupy, nazwy angielskie i warianty
 * odcienia skóry. Do tego polskie słowa kluczowe – podpowiedzi emoji przy pisaniu po polsku.
 */
object EmojiData {
    @Volatile
    var groups: List<EmojiGroup> = emptyList()
        private set
    private val index = HashMap<String, MutableList<String>>()

    fun init(context: Context) {
        if (groups.isNotEmpty()) return
        val map = LinkedHashMap<String, MutableList<Emoji>>()
        val paint = android.graphics.Paint()
        runCatching {
            context.assets.open("emoji.tsv").bufferedReader().useLines { lines ->
                for (line in lines) {
                    val p = line.split('\t')
                    if (p.size < 3) continue
                    // Emoji nowsze niż czcionka systemu wyglądałyby jak puste prostokąty – pomijamy.
                    if (!paint.hasGlyph(p[1])) continue
                    val e = Emoji(p[1], p[2], p.getOrNull(3)?.split(' ')?.filter { it.isNotBlank() && paint.hasGlyph(it) }.orEmpty())
                    map.getOrPut(p[0]) { mutableListOf() } += e
                    // Indeks słów z nazwy („red heart” → „red”, „heart”).
                    if (p[0] != "Flags") {
                        for (w in e.name.lowercase().split(' ', '-', ':', ',')) {
                            if (w.length > 2 && w !in stopWords) index.getOrPut(w) { mutableListOf() } += e.char
                        }
                    }
                    index.getOrPut(e.name.lowercase()) { mutableListOf() }.add(0, e.char)
                }
            }
        }
        for ((word, list) in polish) for (e in list.split(' ')) index.getOrPut(normalizeKey(word)) { mutableListOf() } += e
        groups = map.map { EmojiGroup(it.key, it.value) }
    }

    /** Emoji pasujące do słowa (do paska podpowiedzi). */
    fun forWord(word: String, limit: Int = 2): List<String> {
        if (word.length < 3) return emptyList()
        val key = word.lowercase()
        val list = index[key] ?: index[normalizeKey(word)] ?: index[key.removeSuffix("s")] ?: return emptyList()
        return list.distinct().take(limit)
    }

    /** Wyszukiwanie po nazwie (angielskiej lub polskim słowie kluczowym). */
    fun search(query: String, limit: Int = 60): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val nq = normalizeKey(q)
        val out = LinkedHashSet<String>()
        index[q]?.let(out::addAll)
        index[nq]?.let(out::addAll)
        for ((k, v) in index) {
            if (out.size >= limit) break
            if (k.startsWith(q) || k.startsWith(nq)) out.addAll(v)
        }
        if (out.size < limit) {
            for (g in groups) for (e in g.emoji) {
                if (out.size >= limit) break
                if (e.name.contains(q)) out += e.char
            }
        }
        return out.take(limit)
    }

    fun tonesOf(char: String): List<String> {
        for (g in groups) for (e in g.emoji) if (e.char == char) return e.tones
        return emptyList()
    }

    /** Słowa z nazw emoji, które nie niosą treści – bez nich pasek nie podpowiada emoji do „with”. */
    private val stopWords = setOf(
        "with", "and", "the", "for", "face", "button", "sign", "symbol", "mark", "skin", "tone", "medium",
        "light", "dark", "from", "over", "without", "open", "closed", "squared", "large", "small", "white",
        "black", "old", "new", "two", "one", "three", "left", "right", "down", "up!", "type", "person",
    )

    /** Polskie słowa kluczowe (w tym odmiany) → emoji. */
    private val polish: Map<String, String> = mapOf(
        "serce" to "❤️ 💕", "serduszko" to "❤️", "kocham" to "❤️ 😍", "miłość" to "❤️ 💑", "kochanie" to "😘 ❤️",
        "buziak" to "😘", "buziaki" to "😘", "całuję" to "😘", "przytulas" to "🤗", "przytulam" to "🤗",
        "śmiech" to "😂", "haha" to "😂 🤣", "hahaha" to "🤣", "lol" to "😂", "xd" to "😆",
        "uśmiech" to "😊", "radość" to "😄", "szczęśliwy" to "😊", "szczęśliwa" to "😊", "super" to "👍 🔥",
        "smutny" to "😢", "smutna" to "😢", "smutek" to "😢", "płaczę" to "😭", "płacz" to "😭",
        "zły" to "😠", "zła" to "😠", "wściekły" to "😡", "złość" to "😡",
        "zmęczony" to "😴", "zmęczona" to "😴", "śpię" to "😴", "spać" to "😴", "dobranoc" to "🌙 😴",
        "dzieńdobry" to "☀️", "rano" to "☀️ ☕", "kawa" to "☕", "kawka" to "☕", "herbata" to "🍵",
        "piwo" to "🍺", "piwko" to "🍺", "wino" to "🍷", "pizza" to "🍕", "burger" to "🍔", "tort" to "🎂",
        "ciasto" to "🍰", "lody" to "🍦", "jedzenie" to "🍽️", "obiad" to "🍽️", "śniadanie" to "🥐",
        "urodziny" to "🎂 🎉", "impreza" to "🎉 🥳", "gratulacje" to "🎉 👏", "brawo" to "👏", "dzięki" to "🙏",
        "dziękuję" to "🙏", "proszę" to "🙏", "ok" to "👌", "okej" to "👌", "kciuk" to "👍",
        "kot" to "🐱 🐈", "kotek" to "🐱", "pies" to "🐶 🐕", "piesek" to "🐶", "koń" to "🐴", "ptak" to "🐦",
        "ryba" to "🐟", "mysz" to "🐭", "królik" to "🐰", "miś" to "🧸", "żółw" to "🐢", "świnia" to "🐷",
        "słońce" to "☀️", "deszcz" to "🌧️", "śnieg" to "❄️", "zimno" to "🥶", "gorąco" to "🥵", "burza" to "⛈️",
        "kwiat" to "🌸", "kwiaty" to "💐", "róża" to "🌹", "drzewo" to "🌳", "las" to "🌲", "morze" to "🌊",
        "góry" to "⛰️", "plaża" to "🏖️", "wakacje" to "🏖️ ✈️", "samolot" to "✈️", "samochód" to "🚗",
        "auto" to "🚗", "rower" to "🚲", "pociąg" to "🚆", "autobus" to "🚌", "dom" to "🏠", "praca" to "💼",
        "szkoła" to "🏫", "książka" to "📖", "muzyka" to "🎵", "film" to "🎬", "telefon" to "📱", "komputer" to "💻",
        "pieniądze" to "💰", "kasa" to "💰", "prezent" to "🎁", "święta" to "🎄", "choinka" to "🎄",
        "wielkanoc" to "🐣", "sylwester" to "🎆", "mecz" to "⚽", "piłka" to "⚽", "sport" to "🏃",
        "ogień" to "🔥", "czad" to "🔥", "gwiazda" to "⭐", "księżyc" to "🌙", "tęcza" to "🌈",
        "chory" to "🤒", "chora" to "🤒", "lekarz" to "👨‍⚕️", "szpital" to "🏥", "wow" to "😮", "szok" to "😱",
        "myślę" to "🤔", "hmm" to "🤔", "cicho" to "🤫", "sekret" to "🤫", "cool" to "😎", "fajnie" to "😎",
        "pa" to "👋", "cześć" to "👋", "hej" to "👋", "siema" to "✌️", "spoko" to "👌", "czekam" to "⏳",
        "zegar" to "⏰", "czas" to "⏰", "polska" to "🇵🇱", "polski" to "🇵🇱",
    )
}
