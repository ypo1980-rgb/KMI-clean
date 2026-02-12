package il.kmi.shared.prefs

import com.russhwolf.settings.AppleSettings
import platform.Foundation.NSUserDefaults

class UserPrefsRepositoryIos(
    suiteName: String? = null
) : UserPrefsRepository {

    private val settings by lazy {
        val defaults = suiteName?.let { NSUserDefaults(suiteName = it) }
            ?: NSUserDefaults.standardUserDefaults
        AppleSettings(defaults)
    }

    override fun getFontSize(): String = settings.getString("font_size", "medium")
    override fun setFontSize(value: String) { settings.putString("font_size", value) }

    override fun getThemeMode(): String = settings.getString("theme_mode", "system")
    override fun setThemeMode(value: String) { settings.putString("theme_mode", value) }

    override fun getFontScale(): Double = settings.getDouble("font_scale", 1.0)
    override fun setFontScale(value: Double) { settings.putDouble("font_scale", value) }
}
