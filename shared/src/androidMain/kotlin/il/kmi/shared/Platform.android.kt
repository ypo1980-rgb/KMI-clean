package il.kmi.shared

import android.app.Application
import android.content.Context
import java.io.File

actual object Platform {

    // נשמור Application Context אם סופק
    @Volatile
    private var appCtx: Context? = null

    actual fun init(appContext: Any?) {
        // Accept Context / Application / Activity etc.
        val ctx = when (appContext) {
            is Context -> appContext
            is Application -> appContext.applicationContext
            else -> null
        }
        appCtx = ctx?.applicationContext
    }

    // תואם expect: מחזיר Context או null
    actual val appContextOrNull: Any?
        get() = appCtx

    actual fun setClickSoundsEnabled(enabled: Boolean) {
        // no-op / hook ל-Android (אם תרצה בעתיד)
    }

    actual fun setHapticsEnabled(enabled: Boolean) {
        // no-op / hook ל-Android (אם תרצה בעתיד)
    }

    actual fun scheduleWeeklyTrainingAlarms(leadMinutes: Int) {
        // TODO: חיבור עתידי ל-AlarmManager / WorkManager
    }

    actual fun cancelWeeklyTrainingAlarms() {
        // TODO
    }

    actual fun saveTextAsFile(
        filename: String,
        mimeType: String,
        contents: String
    ): PlatformFile {
        val safeName =
            if (filename.endsWith(".html", ignoreCase = true)) filename
            else "$filename.html"

        val dir: File = appCtx?.cacheDir
            ?: run {
                val tmp = System.getProperty("java.io.tmpdir")
                val fallback = File(tmp ?: "/data/local/tmp")
                if (!fallback.exists()) fallback.mkdirs()
                fallback
            }

        // אם משום מה אין הרשאות/תיקייה לא קיימת, נוודא יצירה
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, safeName)
        file.writeText(contents, Charsets.UTF_8)

        val mt = if (mimeType.isNotBlank()) mimeType else "text/html"
        return PlatformFile(path = file.absolutePath, mimeType = mt)
    }
}