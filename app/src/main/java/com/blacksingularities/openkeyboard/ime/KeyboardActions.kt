package com.blacksingularities.openkeyboard.ime

import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import com.blacksingularities.openkeyboard.MainActivity
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.OneHanded
import com.blacksingularities.openkeyboard.data.Prefs
import com.blacksingularities.openkeyboard.features.TextTools
import com.blacksingularities.openkeyboard.i18n.tr

// Czynności klawiatury, które tylko korzystają z pola tekstowego i usługi – bez stanu wpisywania.
// Wydzielone z [KeyboardController], żeby ten zajmował się samym pisaniem i podpowiedziami.

// ───────────────────────── narzędzia tekstu ─────────────────────────

/** Zmienia zaznaczony tekst – albo słowo przed kursorem, gdy nic nie zaznaczono. */
fun KeyboardController.transformText(fn: (String) -> String) {
    val ic = ic ?: return
    commitComposing(autocorrect = false)
    val sel = ic.getSelectedText(0)?.toString()
    if (!sel.isNullOrEmpty()) {
        val out = fn(sel)
        ic.commitText(out, 1)
        ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)?.let {
            val end = it.startOffset + it.selectionEnd
            ic.setSelection(end - out.length, end)
        }
    } else {
        val before = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        val word = Regex("[\\p{L}\\p{N}'’-]+\\s*$").find(before)?.value?.trimEnd() ?: return
        val trailing = before.length - before.trimEnd().length
        ic.beginBatchEdit()
        ic.deleteSurroundingText(word.length + trailing, 0)
        ic.commitText(fn(word) + before.takeLast(trailing), 1)
        ic.endBatchEdit()
    }
    selfEdit()
    refreshSuggestions()
}

/** Liczba znaków i słów w polu (do panelu narzędzi tekstu). */
fun KeyboardController.fieldStats(): Pair<Int, Int> {
    val et = ic?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
    val t = et?.text?.toString() ?: ((ic?.getTextBeforeCursor(5000, 0) ?: "").toString() + (ic?.getTextAfterCursor(5000, 0) ?: ""))
    return t.length to TextTools.countWords(t)
}

fun KeyboardController.clearField() {
    val ic = ic ?: return
    commitComposing(autocorrect = false)
    ic.performContextMenuAction(android.R.id.selectAll)
    ic.commitText("", 1)
    selfEdit()
    refreshSuggestions()
}

fun KeyboardController.moveCursor(dx: Int) {
    if (dx == 0) return
    commitComposing(autocorrect = false)
    val ic = ic ?: return
    val et = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
    if (et == null || et.text == null) {
        repeat(kotlin.math.abs(dx)) { sendKey(if (dx > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT) }
        return
    }
    val len = et.startOffset + et.text.length
    if (selecting) {
        val start = et.startOffset + et.selectionStart
        val end = (et.startOffset + et.selectionEnd + dx).coerceIn(0, len)
        ic.setSelection(start, end)
    } else {
        val c = (et.startOffset + et.selectionEnd + dx).coerceIn(0, len)
        ic.setSelection(c, c)
    }
    selfEdit()
}

fun KeyboardController.moveCursorVertical(down: Boolean) {
    commitComposing(autocorrect = false)
    if (selecting) sendKey(if (down) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP, KeyEvent.META_SHIFT_ON)
    else sendKey(if (down) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP)
}

fun KeyboardController.cursorHomeEnd(end: Boolean) {
    commitComposing(autocorrect = false)
    val meta = if (selecting) KeyEvent.META_SHIFT_ON else 0
    sendKey(if (end) KeyEvent.KEYCODE_MOVE_END else KeyEvent.KEYCODE_MOVE_HOME, meta)
}

fun KeyboardController.contextAction(id: Int) {
    commitComposing(autocorrect = false)
    ic?.performContextMenuAction(id)
    if (id == android.R.id.copy || id == android.R.id.cut) showToast(tr("Skopiowano"))
    refreshSuggestions()
}

fun KeyboardController.undoRedo(redo: Boolean) {
    commitComposing(autocorrect = false)
    sendKey(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON or (if (redo) KeyEvent.META_SHIFT_ON else 0))
}

fun KeyboardController.sendKey(code: Int, meta: Int = 0) {
    val ic = ic ?: return
    val t = SystemClock.uptimeMillis()
    ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0, meta))
    ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0, meta))
}

fun KeyboardController.openApp(section: String? = null) {
    val i = Intent(service, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    if (section != null) i.putExtra(MainActivity.EXTRA_SECTION, section)
    runCatching { service.startActivity(i) }
}

fun KeyboardController.hide() {
    commitComposing(autocorrect = false)
    service.requestHideSelf(0)
}

fun KeyboardController.switchIme() {
    service.switchToNextInputMethod(false)
}

fun KeyboardController.showImePicker() {
    val imm = service.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
    imm?.showInputMethodPicker()
}

fun KeyboardController.updateSettings(transform: (KeyboardSettings) -> KeyboardSettings) = Prefs.update(transform)

fun KeyboardController.toggleOneHanded() = Prefs.update {
    it.copy(oneHanded = when (it.oneHanded) {
        OneHanded.OFF -> OneHanded.RIGHT
        OneHanded.RIGHT -> OneHanded.LEFT
        OneHanded.LEFT -> OneHanded.OFF
    })
}

fun KeyboardController.toggleFloating() {
    floatingOffset = Offset.Zero
    Prefs.update { it.copy(floating = !it.floating) }
}
