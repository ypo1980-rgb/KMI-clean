package il.kmi.shared

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual object KmiSettingsFactory {
    actual fun of(name: String, context: Any?): Settings {
        val appCtx = (context as Context).applicationContext
        val sp = appCtx.getSharedPreferences(name, Context.MODE_PRIVATE)
        return SharedPreferencesSettings(delegate = sp)
    }
}
