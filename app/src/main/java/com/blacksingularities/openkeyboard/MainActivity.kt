package com.blacksingularities.openkeyboard

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.view.WindowCompat
import com.blacksingularities.openkeyboard.ui.AppRoot
import com.blacksingularities.openkeyboard.ui.AppViewModel
import com.blacksingularities.openkeyboard.ui.Sheet
import com.blacksingularities.openkeyboard.ui.theme.GlassScope
import com.blacksingularities.openkeyboard.ui.theme.OpenKeyboardTheme
import com.blacksingularities.openkeyboard.update.UpdateManager

/** Działania wymagające aktywności (okna systemu, wybór plików, uprawnienia). */
class SystemActions(
    val openImeSettings: () -> Unit,
    val showImePicker: () -> Unit,
    val requestMic: () -> Unit,
    val requestNotifications: () -> Unit,
    val exportBackup: () -> Unit,
    val importBackup: () -> Unit,
    val openUrl: (String) -> Unit,
    val pickBackground: () -> Unit,
)

val LocalSystem = staticCompositionLocalOf<SystemActions> { error("Brak SystemActions") }

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        UpdateManager.clearUpdatedNotification(this)
        handleSection(intent)

        setContent {
            val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                vm.toast(if (ok) com.blacksingularities.openkeyboard.i18n.tr("Mikrofon dostępny – dyktowanie działa") else com.blacksingularities.openkeyboard.i18n.tr("Bez mikrofonu dyktowanie nie zadziała"))
            }
            val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri != null) vm.exportBackup(uri)
            }
            val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) vm.importBackup(uri)
            }
            val background = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) vm.setBackground(uri)
            }
            val actions = SystemActions(
                openImeSettings = { runCatching { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } },
                showImePicker = { getSystemService(InputMethodManager::class.java)?.showInputMethodPicker() },
                requestMic = { mic.launch(Manifest.permission.RECORD_AUDIO) },
                requestNotifications = { if (android.os.Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS) },
                exportBackup = { export.launch("OpenKeyboard-kopia.json") },
                importBackup = { import.launch(arrayOf("application/json", "text/plain", "*/*")) },
                openUrl = { url -> runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) } },
                pickBackground = { background.launch("image/*") },
            )
            OpenKeyboardTheme {
                CompositionLocalProvider(LocalSystem provides actions) {
                    GlassScope(haptics = vm.settings.haptics) { AppRoot(vm) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshImeState()
        vm.checkUpdateIfDue()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Po zamknięciu okna wyboru klawiatury aktywność odzyskuje fokus – odśwież stan.
        if (hasFocus) vm.refreshImeState()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        vm.applyLanguage()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSection(intent)
    }

    private fun handleSection(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_SECTION)) {
            "languages" -> vm.sheet = Sheet.LANGUAGES
            "clipboard" -> vm.sheet = null
            "voice" -> requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7)
        }
    }

    companion object {
        const val EXTRA_SECTION = "section"
    }
}
