package il.kmi.shared.prefs

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual object KmiPrefsFactory {

    actual fun create(context: Any): KmiPrefs {
        val appCtx = (context as? Context)?.applicationContext
            ?: throw IllegalArgumentException("KmiPrefsFactory.create(context): Android Context is required")

        // משתמשים ישירות ב־SharedPreferencesSettings
        val sp = appCtx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
        val settings: Settings = SharedPreferencesSettings(sp)

        return KmiPrefs(settings)
    }
}
