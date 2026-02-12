package il.kmi.shared.tts

expect class PlatformContext

expect object PlatformEnv {
    fun init(platform: PlatformContext)
}

expect object PlatformPrefs {
    fun getString(key: String, default: String): String
}

expect object PlatformHttp {
    suspend fun postJson(url: String, jsonBody: String): ByteArray
}

expect object PlatformCache {
    fun fileIfExists(fileName: String): PlatformFile?
    fun writeFile(fileName: String, bytes: ByteArray): PlatformFile
    fun deleteByPrefix(prefix: String, suffix: String): Int
}

expect class PlatformFile {
    val absolutePath: String
    val sizeBytes: Long
}

expect class PlatformAudioPlayer() {
    fun playFile(path: String, speed: Float)
    fun stop()
    fun release()
}

expect object PlatformCoroutines {
    fun launchBackground(block: suspend () -> Unit)
    fun launchMain(block: () -> Unit)
}

expect object PlatformClock {
    fun nowMs(): Long
}

object PlatformFormat {
    fun f2(v: Double): String {
        // בלי Locale כדי להישאר common
        val x = (v * 100.0 + 0.5).toLong()
        val i = x / 100
        val d = x % 100
        return "$i.${d.toString().padStart(2, '0')}"
    }
}

object PlatformJson {
    fun obj(vararg pairs: Pair<String, Any?>): String {
        // JSON פשוט (מספיק ל-body הזה)
        fun esc(s: String) = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val parts = pairs.joinToString(",") { (k, v) ->
            val key = "\"${esc(k)}\""
            val value = when (v) {
                null -> "null"
                is Number, is Boolean -> v.toString()
                else -> "\"${esc(v.toString())}\""
            }
            "$key:$value"
        }
        return "{$parts}"
    }
}
