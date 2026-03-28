package il.kmi.shared.prefs

import com.russhwolf.settings.Settings

actual object KmiPrefsFactory {

    actual fun create(context: Any): KmiPrefs {
        val factory: Settings.Factory = SharedSettingsFactoryProvider.createFactory()
        val settings: Settings = factory.create("kmi_settings")
        return KmiPrefs(settings)
    }
}