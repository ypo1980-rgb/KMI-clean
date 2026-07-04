package il.kmi.shared

actual object Platform {

    private var appObj: Any? = null

    actual fun init(appContext: Any?) {
        appObj = appContext
    }

    actual val appContextOrNull: Any?
        get() = appObj

    actual fun setClickSoundsEnabled(enabled: Boolean) {}
    actual fun setHapticsEnabled(enabled: Boolean) {}

    actual fun scheduleWeeklyTrainingAlarms(leadMinutes: Int) {}
    actual fun cancelWeeklyTrainingAlarms() {}

    actual fun saveTextAsFile(
        filename: String,
        mimeType: String,
        contents: String
    ): PlatformFile {
        val safeName = if (filename.endsWith(".html", ignoreCase = true)) filename else "$filename.html"
        val path = "/tmp/$safeName"

        val mt = if (mimeType.isNotBlank()) mimeType else "text/html"
        return PlatformFile(path = path, mimeType = mt)
    }
}