package il.kmi.shared.prefs

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings

actual object SharedSettingsFactoryProvider {
    actual fun createFactory(): Settings.Factory = NSUserDefaultsSettings.Factory()
}