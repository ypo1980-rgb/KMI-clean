package il.kmi.shared.prefs

import com.russhwolf.settings.Settings

actual object KmiPrefsFactory {

    actual fun create(context: Any): KmiPrefs {
        // ב־iOS ה־Factory משתמש ב־NSUserDefaults הסטנדרטי
        val settings: Settings = Settings.Factory().create(name = "kmi_settings")
        return KmiPrefs(settings)
    }
}
