package il.kmi.shared

// החוזה המשותף לכל הפלטפורמות
expect object Platform {
    // ייתכן שאין לנו קונטקסט (למשל בתחילת חיי האפליקציה או בבדיקות) – לכן Nullable
    fun init(appContext: Any?)

    // מאפיין תואם: מחזיר את הקונטקסט אם קיים, אחרת null
    val appContextOrNull: Any?

    fun setClickSoundsEnabled(enabled: Boolean)
    fun setHapticsEnabled(enabled: Boolean)

    fun scheduleWeeklyTrainingAlarms(leadMinutes: Int)
    fun cancelWeeklyTrainingAlarms()

    fun saveTextAsFile(filename: String, mimeType: String, contents: String): PlatformFile
}

data class PlatformFile(
    val path: String,
    val mimeType: String
)
