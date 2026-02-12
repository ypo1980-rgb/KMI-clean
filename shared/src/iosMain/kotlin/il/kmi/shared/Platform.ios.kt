package il.kmi.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile

actual object Platform {

    // אפשר לשמור כל אובייקט שיועבר מ-iOS (אם בכלל)
    private var appObj: Any? = null

    actual fun init(appContext: Any?) {
        appObj = appContext
    }

    actual val appContextOrNull: Any?
        get() = appObj

    actual fun setClickSoundsEnabled(enabled: Boolean) { /* no-op */ }
    actual fun setHapticsEnabled(enabled: Boolean) { /* no-op */ }

    actual fun scheduleWeeklyTrainingAlarms(leadMinutes: Int) { /* TODO: UNUserNotificationCenter */ }
    actual fun cancelWeeklyTrainingAlarms() { /* TODO */ }

    actual fun saveTextAsFile(
        filename: String,
        mimeType: String,
        contents: String
    ): PlatformFile {
        val safeName = if (filename.endsWith(".html", ignoreCase = true)) filename else "$filename.html"

        // כתיבה ל-temp של iOS
        val tmpDir: String = NSTemporaryDirectory()
        val path: String = NSString.create(string = tmpDir).stringByAppendingPathComponent(safeName)

        val data: NSData = contents.encodeToByteArray().toNSData()
        data.writeToFile(path, true)

        val mt = if (mimeType.isNotBlank()) mimeType else "text/html"
        return PlatformFile(path = path, mimeType = mt)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    NSData.create(bytes = this.refTo(0), length = this.size.toULong())
