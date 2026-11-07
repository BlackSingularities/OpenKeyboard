package com.blacksingularities.openkeyboard.features

import android.content.ClipboardManager
import android.content.ClipDescription
import android.content.Context
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import java.io.File

/**
 * Wklejanie obrazów (zdjęć, GIF-ów, naklejek) ze schowka do aplikacji, które je przyjmują
 * (commitContent). Obraz kopiujemy do własnego katalogu i udostępniamy przez FileProvider,
 * bo uprawnienie do adresu z cudzej aplikacji nie przechodzi dalej.
 */
object ImagePaste {
    private const val DIR = "shared_images"

    data class ClipImage(val uri: Uri, val mime: String)

    /** Obraz w schowku systemu albo null. */
    fun fromClipboard(context: Context): ClipImage? {
        val m = context.getSystemService(ClipboardManager::class.java) ?: return null
        val clip = runCatching { m.primaryClip }.getOrNull() ?: return null
        if (clip.itemCount == 0) return null
        val desc = clip.description
        val sensitive = desc.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true
        if (sensitive) return null
        val uri = clip.getItemAt(0).uri ?: return null
        val mime = (0 until desc.mimeTypeCount).map { desc.getMimeType(it) }.firstOrNull { it.startsWith("image/") }
            ?: runCatching { context.contentResolver.getType(uri) }.getOrNull()?.takeIf { it.startsWith("image/") }
            ?: return null
        return ClipImage(uri, mime)
    }

    /** Czy pole przyjmuje obraz danego typu. */
    fun accepts(info: EditorInfo?, mime: String): Boolean {
        val types = info?.let { EditorInfoCompat.getContentMimeTypes(it) }.orEmpty()
        return types.any { ClipDescription.compareMimeTypes(mime, it) }
    }

    /** Wkleja obraz do pola. Zwraca false, gdy aplikacja go nie przyjęła. */
    fun commit(context: Context, ic: InputConnection, info: EditorInfo, image: ClipImage): Boolean {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val ext = when (image.mime) {
            "image/gif" -> "gif"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val file = File(dir, "image-${System.currentTimeMillis()}.$ext")
        val copied = runCatching {
            context.contentResolver.openInputStream(image.uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
        }.getOrNull() != null
        if (!copied) return false
        val shared = FileProvider.getUriForFile(context, context.packageName + ".images", file)
        val content = InputContentInfoCompat(shared, ClipDescription("image", arrayOf(image.mime)), null)
        return InputConnectionCompat.commitContent(
            ic, info, content, InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null,
        )
    }
}
