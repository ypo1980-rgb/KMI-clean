package il.kmi.shared.tts

import kotlinx.coroutines.*
import platform.AVFoundation.*
import platform.Foundation.*
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class PlatformContext

actual object PlatformEnv {
    actual fun init(platform: PlatformContext) {
        // nothing
    }
}

actual object PlatformPrefs {
    actual fun getString(key: String, default: String): String {
        val v = NSUserDefaults.standardUserDefaults.stringForKey(key)
        return v ?: default
    }
}

actual object PlatformHttp {
    actual suspend fun postJson(url: String, jsonBody: String): ByteArray {
        return suspendCoroutine { cont ->
            val nsUrl = NSURL.URLWithString(url)!!
            val req = NSMutableURLRequest.requestWithURL(nsUrl).apply {
                setHTTPMethod("POST")
                setValue("application/json; charset=utf-8", forHTTPHeaderField = "Content-Type")
                setHTTPBody(jsonBody.encodeToByteArray().toNSData())
            }

            NSURLSession.sharedSession.dataTaskWithRequest(req) { data, response, error ->
                if (error != null) {
                    cont.resumeWithException(Exception(error.localizedDescription))
                    return@dataTaskWithRequest
                }
                val http = response as? NSHTTPURLResponse
                val code = http?.statusCode?.toInt() ?: -1
                if (code !in 200..299) {
                    cont.resumeWithException(IllegalStateException("HTTP $code"))
                    return@dataTaskWithRequest
                }
                val bytes = data?.toByteArray() ?: ByteArray(0)
                cont.resume(bytes)
            }.resume()
        }
    }
}

actual object PlatformCache {
    private fun dir(): String = NSTemporaryDirectory()

    actual fun fileIfExists(fileName: String): PlatformFile? {
        val path = dir() + fileName
        val fm = NSFileManager.defaultManager
        return if (fm.fileExistsAtPath(path)) PlatformFile(path) else null
    }

    actual fun writeFile(fileName: String, bytes: ByteArray): PlatformFile {
        val path = dir() + fileName
        val data = bytes.toNSData()
        data.writeToFile(path, atomically = true)
        return PlatformFile(path)
    }

    actual fun deleteByPrefix(prefix: String, suffix: String): Int {
        val fm = NSFileManager.defaultManager
        val dirUrl = NSURL.fileURLWithPath(dir())
        val files = fm.contentsOfDirectoryAtURL(dirUrl, includingPropertiesForKeys = null, options = 0u, error = null)
            ?: return 0

        var deleted = 0
        (files as List<*>).forEach { u ->
            val url = u as? NSURL ?: return@forEach
            val name = url.lastPathComponent ?: return@forEach
            if (name.startsWith(prefix) && name.endsWith(suffix)) {
                if (fm.removeItemAtURL(url, error = null)) deleted++
            }
        }
        return deleted
    }
}

actual class PlatformFile(private val path: String) {
    actual val absolutePath: String get() = path
    actual val sizeBytes: Long
        get() {
            val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
            val size = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
            return size
        }
}

actual class PlatformAudioPlayer {
    private var player: AVAudioPlayer? = null

    actual fun playFile(path: String, speed: Float) {
        stop()
        val url = NSURL.fileURLWithPath(path)
        val p = AVAudioPlayer(contentsOfURL = url, error = null) ?: return
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

    actual fun release() = stop()
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
    actual fun nowMs(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}

// ------- helpers -------
private fun ByteArray.toNSData(): NSData {
    val data = NSMutableData.dataWithLength(size.toULong()) as NSMutableData
    if (size > 0) {
        memcpy(data.mutableBytes, this.refTo(0), size.toULong())
    }
    return data
}

private fun NSData.toByteArray(): ByteArray {
    val out = ByteArray(length.toInt())
    if (out.isNotEmpty()) {
        memcpy(out.refTo(0), bytes, length)
    }
    return out
}
