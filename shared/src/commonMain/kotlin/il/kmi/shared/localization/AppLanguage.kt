package il.kmi.shared.localization

enum class AppLanguage(val code: String) {
    HEBREW("he"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return when (code?.lowercase()) {
                "en" -> ENGLISH
                "he" -> HEBREW
                else -> HEBREW
            }
        }
    }
}