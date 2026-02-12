package il.kmi.shared.prefs

import com.russhwolf.settings.Settings

// לפני היה: SettingsFactory / create()
// עכשיו: Settings.Factory / create(name)

expect object SharedSettingsFactoryProvider {
    fun createFactory(): Settings.Factory
}

object SharedSettings {
    private const val DEFAULT_NAME = "kmi_settings"

    fun create(name: String = DEFAULT_NAME): Settings {
        return SharedSettingsFactoryProvider.createFactory().create(name)
    }
}
