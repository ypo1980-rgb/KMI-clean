package il.kmi.shared

import com.russhwolf.settings.Settings
import com.russhwolf.settings.AppleSettings
import platform.Foundation.NSUserDefaults

actual object KmiSettingsFactory {
    actual fun of(name: String, context: Any?): Settings {
        // suiteName מאפשר "שם חנות" בדומה ל־SharedPreferences בשם
        return AppleSettings(NSUserDefaults(suiteName = name))
    }
}
