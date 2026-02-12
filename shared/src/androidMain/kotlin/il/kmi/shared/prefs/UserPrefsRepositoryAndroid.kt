package il.kmi.shared.prefs

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

class UserPrefsRepositoryAndroid(
    context: Context,
    prefsName: String = "kmi_prefs"
) : UserPrefsRepository {

    private val settings: Settings by lazy {
        val sp = context.getSharedPreferences(prefsName, MODE_PRIVATE)
        SharedPreferencesSettings(sp)
    }

    override fun getFontSize(): String = settings.getString("font_size", "medium")
    override fun setFontSize(value: String) { settings.putString("font_size", value) }

    override fun getThemeMode(): String = settings.getString("theme_mode", "system")
    override fun setThemeMode(value: String) { settings.putString("theme_mode", value) }

    // נשמר כ-Double כדי להיות אחיד חוצה-פלטפורמות
    override fun getFontScale(): Double = settings.getDouble("font_scale", 1.0)
    override fun setFontScale(value: Double) { settings.putDouble("font_scale", value) }
}
