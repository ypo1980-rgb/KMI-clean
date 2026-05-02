package il.kmi.shared.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.util.Locale

class AppLanguageManager(
    private val context: Context
) {
    private val appContext = context.applicationContext
    private val store = AndroidLanguageStore(appContext)
    private val repository = LanguageRepository(store)

    init {
        LocalizationRuntime.currentLanguage = repository.currentLanguage()
    }

    fun getCurrentLanguage(): AppLanguage {
        val language = repository.currentLanguage()

        Log.e(
            "KMI_LANG",
            "AppLanguageManager.getCurrentLanguage repository=$language runtimeBefore=${LocalizationRuntime.currentLanguage}"
        )

        LocalizationRuntime.currentLanguage = language

        Log.e(
            "KMI_LANG",
            "AppLanguageManager.getCurrentLanguage runtimeAfter=${LocalizationRuntime.currentLanguage}"
        )

        return language
    }

    fun setLanguage(language: AppLanguage) {
        Log.e(
            "KMI_LANG",
            "AppLanguageManager.setLanguage START requested=$language runtimeBefore=${LocalizationRuntime.currentLanguage}"
        )

        repository.setLanguage(language)

        val storedAfter = repository.currentLanguage()
        LocalizationRuntime.currentLanguage = language

        Log.e(
            "KMI_LANG",
            "AppLanguageManager.setLanguage END requested=$language storedAfter=$storedAfter runtimeAfter=${LocalizationRuntime.currentLanguage}"
        )
    }

    fun text(key: String): String {
        return repository.text(key)
    }

    fun applySavedLanguage(base: Context): Context {
        val language = repository.currentLanguage()
        LocalizationRuntime.currentLanguage = language
        return applyLanguage(base, language)
    }

    fun applyLanguage(base: Context, language: AppLanguage): Context {
        LocalizationRuntime.currentLanguage = language

        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            base.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            base.resources.updateConfiguration(config, base.resources.displayMetrics)
            base
        }
    }
}