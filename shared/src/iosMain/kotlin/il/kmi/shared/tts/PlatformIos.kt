package il.kmi.shared.tts

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSFileSize
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class PlatformContext

actual object PlatformEnv {
    actual fun init(platform: PlatformContext) {}
}

actual object PlatformPrefs {
    actual fun getString(key: String, default: String): String {
        return NSUserDefaults.standardUserDefaults.stringForKey(key) ?: default
    }
}

actual object PlatformHttp {
    actual suspend fun postJson(url: String, jsonBody: String): ByteArray {
        return suspendCoroutine { cont ->
            val nsUrl = NSURL.URLWithString(url)
                ?: return@suspendCoroutine cont.resumeWithException(
                    IllegalArgumentException("Invalid URL: $url")
                )

            val req = NSMutableURLRequest.requestWithURL(nsUrl)
            req.HTTPMethod = "POST"
            req.setValue("application/json; charset=utf-8", forHTTPHeaderField = "Content-Type")
            req.HTTPBody = jsonBody.encodeToByteArray().toNSData()

            NSURLSession.sharedSession.dataTaskWithRequest(req).resume()
            cont.resume(ByteArray(0))
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual object PlatformCache {
    private fun dir(): String = NSTemporaryDirectory()

    actual fun fileIfExists(fileName: String): PlatformFile? {
        val path = dir() + fileName
        return if (NSFileManager.defaultManager.fileExistsAtPath(path)) PlatformFile(path) else null
    }

    actual fun writeFile(fileName: String, bytes: ByteArray): PlatformFile {
        val path = dir() + fileName
        bytes.toNSData().writeToURL(NSURL.fileURLWithPath(path), atomically = true)
        return PlatformFile(path)
    }

    actual fun deleteByPrefix(prefix: String, suffix: String): Int {
        val fm = NSFileManager.defaultManager
        val dirUrl = NSURL.fileURLWithPath(dir())
        val files = fm.contentsOfDirectoryAtURL(
            url = dirUrl,
            includingPropertiesForKeys = null,
            options = 0u,
            error = null
        ) ?: return 0

        var deleted = 0
        (files as List<*>).forEach { value ->
            val url = value as? NSURL ?: return@forEach
            val name = url.lastPathComponent ?: return@forEach
            if (name.startsWith(prefix) && name.endsWith(suffix)) {
                if (fm.removeItemAtURL(url, error = null)) deleted++
            }
        }

        return deleted
    }
}

actual class PlatformFile(private val path: String) {
    actual val absolutePath: String
        get() = path

    actual val sizeBytes: Long
        get() {
            val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
            return (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
        }
}

actual class PlatformAudioPlayer {
    private var player: AVAudioPlayer? = null

    actual fun playFile(path: String, speed: Float) {
        stop()

        val p = AVAudioPlayer(
            contentsOfURL = NSURL.fileURLWithPath(path),
            error = null
        ) ?: return

        p.enableRate = true
        p.rate = speed
        p.prepareToPlay()
        p.play()
        player = p
    }

    actual fun stop() {
        player?.stop()
        player = null
    }

    actual fun release() {
        stop()
    }
}

actual object PlatformCoroutines {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    actual fun launchBackground(block: suspend () -> Unit) {
        scope.launch(Dispatchers.Default) { block() }
    }

    actual fun launchMain(block: () -> Unit) {
        scope.launch(Dispatchers.Main) { block() }
    }
}

actual object PlatformClock {
    actual fun nowMs(): Long = (NSDate().timeIntervalSinceReferenceDate * 1000.0).toLong()
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    val data = NSMutableData()
    usePinned {
        data.appendBytes(it.addressOf(0), size.toULong())
    }
    return data
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    return bytes?.readBytes(length.toInt()) ?: ByteArray(0)
}