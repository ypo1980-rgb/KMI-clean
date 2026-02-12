package il.kmi.shared.prefs

interface UserPrefsRepository {
    fun getFontSize(): String          // "small" | "medium" | "large"
    fun setFontSize(value: String)

    fun getThemeMode(): String         // "system" | "light" | "dark"
    fun setThemeMode(value: String)

    // נוספה תמיכה ב"סקייל רציף" של הפונט (נשמר כ-Double כדי לעבוד טוב גם ב-iOS)
    fun getFontScale(): Double         // 0.80 .. 1.40 (ברירת מחדל 1.0)
    fun setFontScale(value: Double)
}
