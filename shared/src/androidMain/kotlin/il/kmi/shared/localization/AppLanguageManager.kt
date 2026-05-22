package il.kmi.shared.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
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
        LocalizationRuntime.currentLanguage = language
        return language
    }

    fun setLanguage(language: AppLanguage) {
        repository.setLanguage(language)
        LocalizationRuntime.currentLanguage = language
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