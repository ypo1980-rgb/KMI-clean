package il.kmi.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent

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

        val tmpDir: String = NSTemporaryDirectory()
        val path: String = NSString.create(string = tmpDir).stringByAppendingPathComponent(safeName)

        contents.encodeToByteArray()
            .toNSData()
            .writeToURL(
                url = platform.Foundation.NSURL.fileURLWithPath(path),
                atomically = true
            )

        val mt = if (mimeType.isNotBlank()) mimeType else "text/html"
        return PlatformFile(path = path, mimeType = mt)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    val data = NSMutableData()
    if (isNotEmpty()) {
        usePinned {
            data.appendBytes(it.addressOf(0), length = size.toULong())
        }
    }
    return data
}