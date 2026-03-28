package il.kmi.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.posix.memcpy

actual object Platform {

    private var appObj: Any? = null

    actual fun init(appContext: Any?) {
        appObj = appContext
    }

    actual val appContextOrNull: Any?
        get() = appObj

    actual fun setClickSoundsEnabled(enabled: Boolean) { /* no-op */ }
    actual fun setHapticsEnabled(enabled: Boolean) { /* no-op */ }

    actual fun scheduleWeeklyTrainingAlarms(leadMinutes: Int) { /* TODO */ }
    actual fun cancelWeeklyTrainingAlarms() { /* TODO */ }

    actual fun saveTextAsFile(
        filename: String,
        mimeType: String,
        contents: String
    ): PlatformFile {
        val safeName = if (filename.endsWith(".html", ignoreCase = true)) filename else "$filename.html"

        val tmpDir: String = NSTemporaryDirectory()
        val path: String = NSString.create(string = tmpDir).stringByAppendingPathComponent(safeName)

        val data: NSData = contents.encodeToByteArray().toNSData()
        data.writeToFile(path, true)

        val mt = if (mimeType.isNotBlank()) mimeType else "text/html"
        return PlatformFile(path = path, mimeType = mt)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    val data = NSMutableData.dataWithLength(size.toULong()) as NSMutableData
    if (isNotEmpty()) {
        memcpy(data.mutableBytes, refTo(0), size.toULong())
    }
    return data
}