package com.blacksingularities.openkeyboard

import android.app.Application
import com.blacksingularities.openkeyboard.data.Prefs
import com.blacksingularities.openkeyboard.dict.Dictionaries
import com.blacksingularities.openkeyboard.dict.UserModel
import com.blacksingularities.openkeyboard.emoji.EmojiData
import com.blacksingularities.openkeyboard.features.ClipboardHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Wspólny start aplikacji, klawiatury i sprawdzania pisowni (jeden proces). */
class OpenKeyboardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        UserModel.init(this)
        ClipboardHistory.init(this)
        Dictionaries.init(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            EmojiData.init(this@OpenKeyboardApp)
            Dictionaries.awaitLoaded(Prefs.current.languages.filter { Dictionaries.isAvailable(it) })
        }
    }
}
