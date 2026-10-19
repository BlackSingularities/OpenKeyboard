package com.blacksingularities.openkeyboard.ime

import android.graphics.Rect
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

    /** Klawiatura ekranowa także przy podłączonej fizycznej (emulator, etui z klawiaturą). */
    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
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
