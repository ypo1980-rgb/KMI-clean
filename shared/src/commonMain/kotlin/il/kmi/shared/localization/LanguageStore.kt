package il.kmi.shared.localization

interface LanguageStore {
    fun getLanguage(): AppLanguage
    fun setLanguage(language: AppLanguage)
}