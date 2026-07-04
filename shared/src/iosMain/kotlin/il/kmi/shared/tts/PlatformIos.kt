package il.kmi.shared.tts

actual class PlatformContext

actual object PlatformEnv {
    actual fun init(platform: PlatformContext) {}
}

actual object PlatformPrefs {
    actual fun getString(key: String, default: String): String = default
}

actual object PlatformHttp {
    actual suspend fun postJson(url: String, jsonBody: String): ByteArray = ByteArray(0)
}

actual object PlatformCache {
    actual fun fileIfExists(fileName: String): PlatformFile? = null

    actual fun writeFile(fileName: String, bytes: ByteArray): PlatformFile {
        return PlatformFile("/tmp/$fileName")
    }

    actual fun deleteByPrefix(prefix: String, suffix: String): Int = 0
}

actual class PlatformFile(private val path: String) {
    actual val absolutePath: String
        get() = path

    actual val sizeBytes: Long
        get() = 0L
}

actual class PlatformAudioPlayer {
    actual fun playFile(path: String, speed: Float) {}
    actual fun stop() {}
    actual fun release() {}
}

actual object PlatformCoroutines {
    actual fun launchBackground(block: suspend () -> Unit) {}
    actual fun launchMain(block: () -> Unit) {
        block()
    }
}

actual object PlatformClock {
    actual fun nowMs(): Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
}