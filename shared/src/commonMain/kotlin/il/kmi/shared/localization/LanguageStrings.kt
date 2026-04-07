package il.kmi.shared.localization

object LanguageStrings {

    private val he = mapOf(
        LanguageKeys.HOME to "בית",
        LanguageKeys.SETTINGS to "הגדרות",
        LanguageKeys.LOGIN to "התחברות",
        LanguageKeys.LOGOUT to "התנתקות",
        LanguageKeys.SEARCH to "חיפוש",
        LanguageKeys.SAVE to "שמור",
        LanguageKeys.CANCEL to "ביטול",
        LanguageKeys.LANGUAGE to "שפה",
        LanguageKeys.HEBREW to "עברית",
        LanguageKeys.ENGLISH to "English",
        LanguageKeys.SEND_MESSAGE to "שליחת הודעה"
    )

    private val en = mapOf(
        LanguageKeys.HOME to "Home",
        LanguageKeys.SETTINGS to "Settings",
        LanguageKeys.LOGIN to "Login",
        LanguageKeys.LOGOUT to "Logout",
        LanguageKeys.SEARCH to "Search",
        LanguageKeys.SAVE to "Save",
        LanguageKeys.CANCEL to "Cancel",
        LanguageKeys.LANGUAGE to "Language",
        LanguageKeys.HEBREW to "עברית",
        LanguageKeys.ENGLISH to "English",
        LanguageKeys.SEND_MESSAGE to "Send Message"
    )

    fun get(language: AppLanguage, key: String): String {
        val table = when (language) {
            AppLanguage.HEBREW -> he
            AppLanguage.ENGLISH -> en
        }

        return table[key]
            ?: he[key]
            ?: key
    }
}