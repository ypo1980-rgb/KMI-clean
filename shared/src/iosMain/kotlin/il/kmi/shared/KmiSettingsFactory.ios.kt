package il.kmi.shared

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual object KmiSettingsFactory {
    actual fun of(name: String, context: Any?): Settings {
        val defaults = NSUserDefaults(suiteName = name) ?: NSUserDefaults.standardUserDefaults
        return NSUserDefaultsSettings(defaults)
    }
}