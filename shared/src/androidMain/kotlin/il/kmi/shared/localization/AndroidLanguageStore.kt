package il.kmi.shared.localization

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class AndroidLanguageStore(context: Context) : LanguageStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getLanguage(): AppLanguage {
        val rawCode: String? = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.HEBREW.code)
        val resolvedLanguage = AppLanguage.fromCode(rawCode)

        Log.e(
            "KMI_LANG",
            "AndroidLanguageStore.getLanguage rawCode=$rawCode resolved=$resolvedLanguage prefs=$PREFS_NAME key=$KEY_APP_LANGUAGE"
        )

        return resolvedLanguage
    }

    override fun setLanguage(language: AppLanguage) {
        val success = prefs.edit()
            .putString(KEY_APP_LANGUAGE, language.code)
            .commit()

        val rawAfter: String? = prefs.getString(KEY_APP_LANGUAGE, null)
        val resolvedAfter = AppLanguage.fromCode(rawAfter)

        Log.e(
            "KMI_LANG",
            "AndroidLanguageStore.setLanguage requested=$language code=${language.code} commitSuccess=$success rawAfter=$rawAfter resolvedAfter=$resolvedAfter prefs=$PREFS_NAME key=$KEY_APP_LANGUAGE"
        )
    }

    private companion object {
        const val PREFS_NAME: String = "kmi_language_prefs"
        const val KEY_APP_LANGUAGE: String = "app_language"
    }
}