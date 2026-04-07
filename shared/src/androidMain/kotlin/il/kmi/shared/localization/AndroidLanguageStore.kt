package il.kmi.shared.localization

import android.content.Context
import android.content.SharedPreferences

class AndroidLanguageStore(context: Context) : LanguageStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getLanguage(): AppLanguage {
        val code: String? = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.HEBREW.code)
        return AppLanguage.fromCode(code)
    }

    override fun setLanguage(language: AppLanguage) {
        prefs.edit()
            .putString(KEY_APP_LANGUAGE, language.code)
            .apply()
    }

    private companion object {
        const val PREFS_NAME: String = "kmi_language_prefs"
        const val KEY_APP_LANGUAGE: String = "app_language"
    }
}