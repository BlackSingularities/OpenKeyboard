package com.blacksingularities.openkeyboard.ime

import android.content.res.Configuration
import android.graphics.Rect
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.blacksingularities.openkeyboard.data.Prefs
import com.blacksingularities.openkeyboard.features.ClipboardHistory
import com.blacksingularities.openkeyboard.i18n.I18n

/**
 * Usługa klawiatury. Okno klawiatury zajmuje całą wysokość ekranu (przezroczyste), a aplikacji
 * oddajemy tylko obszar nad klawiaturą – dzięki temu działają okienka nad klawiszami, tryb
 * pływający (nakładka, którą można przesuwać) i tryb jednej ręki.
 */
class OpenKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    lateinit var controller: KeyboardController
        private set

    /** Obszar klawiatury w oknie (do wcięć i obszaru dotyku). Ustawiany przez interfejs. */
    val keyboardBounds = Rect()
    private var rootView: View? = null

    /**
     * Wysokość paska nawigacji rysowanego przez system w oknie klawiatury (przyciski „schowaj”
     * i „zmień klawiaturę”) – klawisze muszą kończyć się nad nim.
     */
    val navBarPx: Int by lazy {
        fun dimen(name: String): Int {
            val id = resources.getIdentifier(name, "dimen", "android")
            return if (id != 0) resources.getDimensionPixelSize(id) else 0
        }
        maxOf(dimen("navigation_bar_frame_height"), dimen("navigation_bar_height"))
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val s = Prefs.current
        I18n.apply(s.appLanguage, resources.configuration.locales[0].language)
        controller = KeyboardController(this)
        ClipboardHistory.attach(this)
    }

    override fun onCreateInputView(): View {
        val decor = window.window?.decorView
        decor?.setViewTreeLifecycleOwner(this)
        decor?.setViewTreeViewModelStoreOwner(this)
        decor?.setViewTreeSavedStateRegistryOwner(this)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OpenKeyboardService)
            setViewTreeViewModelStoreOwner(this@OpenKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@OpenKeyboardService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setContent { KeyboardRoot(controller) }
        }
        rootView = view
        return view
    }

    override fun setInputView(view: View) {
        super.setInputView(view)
        updateWindowLayout()
    }

    /** Okno i obszar wprowadzania na całą wysokość, treść przyklejona do dołu. */
    private fun updateWindowLayout() {
        val w = window.window ?: return
        WindowCompat.setDecorFitsSystemWindows(w, false)
        w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        val area = w.findViewById<View>(android.R.id.inputArea) ?: return
        fun matchParent(v: View) {
            val lp = v.layoutParams ?: return
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            if (lp is FrameLayout.LayoutParams) lp.gravity = Gravity.BOTTOM
            if (lp is android.widget.LinearLayout.LayoutParams) lp.gravity = Gravity.BOTTOM
            v.layoutParams = lp
        }
        matchParent(area)
        (area.parent as? View)?.let(::matchParent)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    /** Podłączona i rozłożona fizyczna klawiatura (Bluetooth, tablet z etui, Chromebook). */
    private val hardwareKeyboard: Boolean
        get() {
            val c = resources.configuration
            return c.keyboard != Configuration.KEYBOARD_NOKEYS && c.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
        }

    /**
     * Klawiatura ekranowa domyślnie także przy fizycznej (emulator, etui z klawiaturą) – z paskiem
     * podpowiedzi. Można ją wtedy chować w ustawieniach.
     */
    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return !(hardwareKeyboard && Prefs.current.hideWithHardware)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateInputViewShown()
    }

    /**
     * Fizyczna klawiatura. Ctrl+Spacja zmienia język pisania. Gdy klawisze
     * ekranowe są widoczne, litery, spacja, Backspace i Enter przechodzą przez silnik klawiatury –
     * działają podpowiedzi, autokorekta i nauka słów. Skróty z Ctrl/Alt/Meta i klawisze systemowe
     * idą prosto do aplikacji.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (destroyed || !fromHardware(event)) return super.onKeyDown(keyCode, event)
        if (isLanguageShortcut(keyCode, event)) {
            if (event.repeatCount == 0) controller.nextLanguage()
            return true
        }
        if (!routeToEngine(event)) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> controller.onDelete(repeat = event.repeatCount > 0)
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> controller.onEnter()
            KeyEvent.KEYCODE_SPACE -> controller.onSpace()
            else -> {
                val ch = event.getUnicodeChar(event.metaState)
                if (ch == 0 || ch and KeyCharacterMap.COMBINING_ACCENT != 0) return super.onKeyDown(keyCode, event)
                controller.onText(String(Character.toChars(ch)))
            }
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (destroyed || !fromHardware(event)) return super.onKeyUp(keyCode, event)
        if (isLanguageShortcut(keyCode, event)) return true
        if (routeToEngine(event) && handledByEngine(keyCode, event)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun isLanguageShortcut(keyCode: Int, event: KeyEvent): Boolean =
        keyCode == KeyEvent.KEYCODE_SPACE && event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed

    private fun fromHardware(event: KeyEvent): Boolean =
        event.deviceId != KeyCharacterMap.VIRTUAL_KEYBOARD && !event.isSystem && event.source and InputDevice.SOURCE_KEYBOARD != 0

    /** Czy klawisz ma przejść przez silnik (podpowiedzi). Tylko bez modyfikatorów i przy widocznym pasku. */
    private fun routeToEngine(event: KeyEvent): Boolean =
        Prefs.current.hardwareSuggestions && isInputViewShown && currentInputConnection != null &&
            !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed &&
            controller.wordSuggestions

    private fun handledByEngine(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_SPACE -> true
        else -> event.getUnicodeChar(event.metaState).let { it != 0 && it and KeyCharacterMap.COMBINING_ACCENT == 0 }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (destroyed) return
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        controller.onStartInput(info, restarting)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        if (!destroyed) {
            controller.onFinishInput()
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        controller.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val root = rootView ?: return
        val h = root.height
        if (h <= 0 || keyboardBounds.isEmpty || !isInputViewShown) {
            outInsets.contentTopInsets = h
            outInsets.visibleTopInsets = h
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
            outInsets.touchableRegion.setEmpty()
            return
        }
        // W trybie pływającym aplikacja zajmuje cały ekran – klawiatura wisi nad nią.
        val top = if (Prefs.current.floating) h else keyboardBounds.top
        outInsets.contentTopInsets = top
        outInsets.visibleTopInsets = top
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
        outInsets.touchableRegion.set(keyboardBounds)
    }

    /** Wywoływane po zmianie położenia/rozmiaru klawiatury – system przelicza obszary. */
    fun requestInsetsUpdate() {
        rootView?.requestLayout()
    }

    /** Po zniszczeniu system może jeszcze wywołać onFinishInputView – wtedy nic nie robimy. */
    private var destroyed = false

    override fun onDestroy() {
        // Najpierw system kończy pole (onFinishInputView), dopiero potem sprzątamy.
        super.onDestroy()
        destroyed = true
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        ClipboardHistory.detach()
        controller.destroy()
        store.clear()
        rootView = null
    }
}
