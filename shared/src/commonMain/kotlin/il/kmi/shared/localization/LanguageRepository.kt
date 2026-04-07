package il.kmi.shared.localization

class LanguageRepository(
    private val languageStore: LanguageStore
) {
    fun currentLanguage(): AppLanguage {
        return languageStore.getLanguage()
    }

    fun setLanguage(language: AppLanguage) {
        languageStore.setLanguage(language)
    }

    fun text(key: String): String {
        return LanguageStrings.get(currentLanguage(), key)
    }
}