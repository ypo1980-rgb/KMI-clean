package il.kmi.shared.prefs

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

class UserPrefsRepositoryIos(
    suiteName: String? = null
) : UserPrefsRepository {

    private val settings: Settings by lazy {
        val defaults: NSUserDefaults =
            suiteName?.let { NSUserDefaults(suiteName = it) }
                ?: NSUserDefaults.standardUserDefaults

        NSUserDefaultsSettings(defaults)
    }

    override fun getFontSize(): String = settings.getString("font_size", "medium")
    override fun setFontSize(value: String) {
        settings.putString("font_size", value)
    }

    override fun getThemeMode(): String = settings.getString("theme_mode", "system")
    override fun setThemeMode(value: String) {
        settings.putString("theme_mode", value)
    }

    override fun getFontScale(): Double = settings.getDouble("font_scale", 1.0)
    override fun setFontScale(value: Double) {
        settings.putDouble("font_scale", value)
    }
}