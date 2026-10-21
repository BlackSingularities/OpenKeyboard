package com.blacksingularities.openkeyboard.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blacksingularities.openkeyboard.data.KeyboardSettings
import com.blacksingularities.openkeyboard.data.Prefs
import com.blacksingularities.openkeyboard.dict.UserModel
import com.blacksingularities.openkeyboard.features.Clip
import com.blacksingularities.openkeyboard.features.ClipboardHistory
import com.blacksingularities.openkeyboard.i18n.I18n
import com.blacksingularities.openkeyboard.i18n.tr
import com.blacksingularities.openkeyboard.update.UPDATE_API
import com.blacksingularities.openkeyboard.update.UpdateManager
import com.blacksingularities.openkeyboard.update.UpdateState
import com.blacksingularities.openkeyboard.update.isNewer
import com.blacksingularities.openkeyboard.update.parseRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** Arkusze otwierane nad ekranem ustawień. */
enum class Sheet { LANGUAGES, THEME, DICTIONARY, SHORTCUTS, TOOLBAR, ABOUT, GESTURES, STATS }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()

    var settings by mutableStateOf(Prefs.current)
        private set
    var sheet by mutableStateOf<Sheet?>(null)
    var imeEnabled by mutableStateOf(false)
        private set
    var imeSelected by mutableStateOf(false)
        private set
    /** Uprawnienie do mikrofonu (dyktowanie). */
    var micGranted by mutableStateOf(false)
        private set
    /** W telefonie jest usługa rozpoznawania mowy (np. Google). */
    var speechAvailable by mutableStateOf(true)
        private set
    /** Prośba o mikrofon została już raz odrzucona – kolejna pójdzie do ustawień systemu. */
    var micAsked by mutableStateOf(false)
    /** Aplikację otwarto z panelu dyktowania – od razu poproś o mikrofon. */
    var requestMicOnStart by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
        private set

    fun update(t: (KeyboardSettings) -> KeyboardSettings) {
        val oldLanguage = Prefs.current.appLanguage
        Prefs.update(t)
        if (Prefs.current.appLanguage != oldLanguage) applyLanguage()
    }

    fun applyLanguage() {
        I18n.apply(Prefs.current.appLanguage, ctx.resources.configuration.locales[0].language)
    }

    fun toast(text: String) {
        message = text
        viewModelScope.launch {
            delay(2500)
            if (message == text) message = null
        }
    }

    // ───────────────────────── konfiguracja klawiatury ─────────────────────────

    fun refreshImeState() {
        val imm = ctx.getSystemService(InputMethodManager::class.java)
        imeEnabled = imm?.enabledInputMethodList?.any { it.packageName == ctx.packageName } == true
        val current = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
        imeSelected = current.startsWith(ctx.packageName + "/")
        micGranted = ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        speechAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(ctx)
    }

    /** Obraz tła klawiatury: zmniejszony do rozsądnego rozmiaru i zapisany w plikach aplikacji. */
    fun setBackground(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val bmp = ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: error("Brak obrazu")
                    val scale = minOf(1f, 1600f / maxOf(bmp.width, bmp.height))
                    val out = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
                    java.io.File(ctx.filesDir, com.blacksingularities.openkeyboard.ime.BACKGROUND_FILE).outputStream().use {
                        out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it)
                    }
                }
            }.onSuccess {
                Prefs.update { it.copy(bgImage = true, bgImageStamp = System.currentTimeMillis()) }
                toast(tr("Ustawiono obraz tła"))
            }.onFailure { toast(tr("Nie udało się wczytać: {0}", it.message ?: "")) }
        }
    }

    // ───────────────────────── kopia zapasowa ─────────────────────────

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val clips = Json.encodeToString(ListSerializer(Clip.serializer()), ClipboardHistory.items.value.filter { it.pinned })
                    val text = """{"format":"openkeyboard-backup","version":1,"settings":${Prefs.exportJson()},"user":${UserModel.exportJson()},"pinnedClips":$clips}"""
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("Brak dostępu")
                }
            }.onSuccess { toast(tr("Zapisano kopię zapasową")) }
                .onFailure { toast(tr("Nie udało się zapisać: {0}", it.message ?: "")) }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Brak dostępu")
                    val root = Json.parseToJsonElement(text).jsonObject
                    if (root["format"]?.toString()?.trim('"') != "openkeyboard-backup") error(tr("To nie jest kopia OpenKeyboard"))
                    root["settings"]?.let { Prefs.importJson(it.toString()) }
                    root["user"]?.let { UserModel.importJson(it.toString()) }
                    root["pinnedClips"]?.let { el ->
                        Json.decodeFromString(ListSerializer(Clip.serializer()), el.toString()).forEach { ClipboardHistory.add(it) }
                    }
                }
            }.onSuccess {
                applyLanguage()
                toast(tr("Przywrócono kopię zapasową"))
            }.onFailure { toast(tr("Nie udało się wczytać: {0}", it.message ?: "")) }
        }
    }

    // ───────────────────────── aktualizacje (GitHub Releases) ─────────────────────────

    var update by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set
    var updateBannerHidden by mutableStateOf(false)
    private var updateJob: Job? = null
    private var lastUpdateCheck = 0L

    val appVersion: String get() = UpdateManager.currentVersion

    fun checkUpdate(silent: Boolean = false) {
        if (updateJob?.isActive == true) return
        updateJob = viewModelScope.launch {
            update = UpdateState.Checking
            update = try {
                val info = withContext(Dispatchers.IO) { parseRelease(UpdateManager.httpGet(UPDATE_API), ".apk") }
                if (isNewer(info.version, appVersion)) UpdateState.Available(info) else UpdateState.UpToDate
            } catch (e: Throwable) {
                if (silent) UpdateState.Idle else UpdateState.Failed(e.message ?: tr("Brak połączenia"))
            }
        }
    }

    fun checkUpdateIfDue() {
        if (!Prefs.current.autoUpdate) return
        val now = System.currentTimeMillis()
        if (now - lastUpdateCheck < 6 * 3600_000L) return
        if (update is UpdateState.Downloading || update is UpdateState.Ready || update is UpdateState.Installing) return
        lastUpdateCheck = now
        checkUpdate(silent = true)
    }

    private fun downloadUpdate(installAfter: Boolean) {
        val info = (update as? UpdateState.Available)?.info ?: return
        updateJob = viewModelScope.launch {
            update = UpdateState.Downloading(info, 0f)
            update = try {
                val file = UpdateManager.download(ctx, info) { p ->
                    viewModelScope.launch { if (update is UpdateState.Downloading) update = UpdateState.Downloading(info, p) }
                }
                UpdateState.Ready(info, file)
            } catch (e: Throwable) {
                UpdateState.Failed(e.message ?: tr("Pobieranie nie powiodło się"))
            }
            if (installAfter && update is UpdateState.Ready) installUpdate()
        }
    }

    /** Jednym dotknięciem: pobranie nowej wersji z GitHuba i instalacja. */
    fun updateNow() {
        when (update) {
            is UpdateState.Available -> downloadUpdate(installAfter = true)
            is UpdateState.Ready -> installUpdate()
            is UpdateState.Failed, UpdateState.Idle, UpdateState.UpToDate -> checkUpdate()
            else -> {}
        }
    }

    fun installUpdate() {
        val ready = update as? UpdateState.Ready ?: return
        if (!UpdateManager.canInstall(ctx)) {
            UpdateManager.openInstallPermissionSettings(ctx)
            return
        }
        viewModelScope.launch {
            update = UpdateState.Installing(ready.info)
            try {
                UpdateManager.install(ctx, ready.file)
            } catch (e: Throwable) {
                update = UpdateState.Failed(e.message ?: tr("Instalacja nie powiodła się"))
            }
        }
    }

    val updatePending: Boolean
        get() = update is UpdateState.Available || update is UpdateState.Downloading || update is UpdateState.Ready || update is UpdateState.Installing

    // Na końcu klasy – wszystkie pola stanu są już utworzone.
    init {
        viewModelScope.launch { Prefs.flow.collect { settings = it } }
        applyLanguage()
        refreshImeState()
        checkUpdateIfDue()
    }
}
