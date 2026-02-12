package il.kmi.shared.prefs

import com.russhwolf.settings.Settings
import com.russhwolf.settings.AppleSettings

actual object SharedSettingsFactoryProvider {
    actual fun createFactory(): Settings.Factory = AppleSettings.Factory()
}