package com.blacksingularities.openkeyboard.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.blacksingularities.openkeyboard.dict.UserModel
import com.blacksingularities.openkeyboard.i18n.tr
import java.util.UUID

/**
 * Synchronizacja słownika bez chmury OpenKeyboard: każde urządzenie zapisuje swój plik
 * w folderze wybranym przez użytkownika (synchronizowanym np. przez Syncthing albo Nextcloud)
 * i scala pliki pozostałych urządzeń. Przenoszone są wyuczone słowa, pary słów, zablokowane
 * słowa i skróty – bez ustawień, schowka i statystyk.
 */
object FolderSync {
    private const val PREFIX = "OpenKeyboard-sync-"
    private const val PREFS = "openkeyboard_sync"

    private fun deviceId(context: Context): String {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getString("device", null) ?: UUID.randomUUID().toString().take(8).also { p.edit().putString("device", it).apply() }
    }

    /** Scala pliki innych urządzeń i zapisuje własny; zwraca liczbę scalonych urządzeń. Poza wątkiem głównym. */
    fun sync(context: Context, folder: String): Int {
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(folder))
        if (tree == null || !tree.canWrite()) error(tr("Brak dostępu do folderu"))
        val own = "$PREFIX${deviceId(context)}.json"
        var merged = 0
        for (f in tree.listFiles()) {
            val name = f.name ?: continue
            if (!name.startsWith(PREFIX) || !name.endsWith(".json") || name == own) continue
            val text = context.contentResolver.openInputStream(f.uri)?.use { it.readBytes().decodeToString() } ?: continue
            if (runCatching { UserModel.mergeJson(text) }.isSuccess) merged++
        }
        val target = tree.findFile(own) ?: tree.createFile("application/json", own) ?: error(tr("Brak dostępu do folderu"))
        context.contentResolver.openOutputStream(target.uri, "wt")?.use { it.write(UserModel.exportSyncJson().toByteArray()) }
        return merged
    }
}
