package il.kmi.shared.tts

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

actual class PlatformContext(val context: Context)

actual object PlatformEnv {
    private var appCtx: Context? = null
    actual fun init(platform: PlatformContext) {
        appCtx = platform.context.applicationContext
        PlatformAndroidState.sp = appCtx!!.getSharedPreferences("kmi_ai_voice", Context.MODE_PRIVATE)
    }
    fun ctx(): Context = requireNotNull(appCtx) { "PlatformEnv not inited" }
}

private object PlatformAndroidState {
    lateinit var sp: SharedPreferences
}

actual object PlatformPrefs {
    actual fun getString(key: String, default: String): String {
        return PlatformAndroidState.sp.getString(key, default) ?: default
    }
}

actual object PlatformHttp {
    actual suspend fun postJson(url: String, jsonBody: String): ByteArray = withContext(Dispatchers.IO) {
        val u = URL(url)
        val conn = (u.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 25_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = runCatching { conn.errorStream?.readBytes()?.toString(Charsets.UTF_8) }.getOrNull()
            throw IllegalStateException("HTTP $code ${err ?: ""}".trim())
        }
        conn.inputStream.use { it.readBytes() }
    }
}

actual object PlatformCache {
    private fun dir(): File = PlatformEnv.ctx().cacheDir

    actual fun fileIfExists(fileName: String): PlatformFile? {
        val f = File(dir(), fileName)
        return if (f.exists() && f.isFile) PlatformFile(f) else null
    }

    actual fun writeFile(fileName: String, bytes: ByteArray): PlatformFile {
        val f = File(dir(), fileName)
        f.writeBytes(bytes)
        return PlatformFile(f)
    }

    actual fun deleteByPrefix(prefix: String, suffix: String): Int {
        val files = dir().listFiles { f ->
            f.isFile && f.name.startsWith(prefix) && f.name.endsWith(suffix)
        } ?: return 0
        var deleted = 0
        files.forEach { if (runCatching { it.delete() }.getOrDefault(false)) deleted++ }
        return deleted
    }
}

actual class PlatformFile(private val f: File) {
    actual val absolutePath: String get() = f.absolutePath
    actual val sizeBytes: Long get() = f.length()
}

actual class PlatformAudioPlayer {

    private var mp: MediaPlayer? = null

    actual fun playFile(path: String, speed: Float) {
        stop()

        val safeSpeed = speed.coerceIn(0.60f, 1.60f)

        try {
            val player = MediaPlayer().apply {
                setDataSource(path)

                setOnPreparedListener { p ->
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            p.playbackParams = p.playbackParams.setSpeed(safeSpeed)
                        }
                    } catch (t: Throwable) {
                        Log.w("KMI_TTS", "MediaPlayer failed to set speed=$safeSpeed", t)
                    }
                    p.start()
                }

                setOnCompletionListener { p ->
                    runCatching { p.stop() }
                    runCatching { p.release() }
                    if (mp === p) mp = null
                }

                setOnErrorListener { p, what, extra ->
                    Log.e("KMI_TTS", "MediaPlayer error what=$what extra=$extra")
                    runCatching { p.release() }
                    if (mp === p) mp = null
                    true
                }
            }

            mp = player
            player.prepareAsync()
        } catch (t: Throwable) {
            Log.e("KMI_TTS", "MediaPlayer playFile failed path=$path", t)
            stop()
        }
    }

    actual fun stop() {
        val p = mp
        mp = null
        if (p != null) {
            runCatching { p.stop() }
            runCatching { p.release() }
        }
    }

    actual fun release() = stop()
}

actual object PlatformCoroutines {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    actual fun launchBackground(block: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) { block() }
    }
    actual fun launchMain(block: () -> Unit) {
        scope.launch(Dispatchers.Main.immediate) { block() }
    }
}

actual object PlatformClock {
    actual fun nowMs(): Long = android.os.SystemClock.elapsedRealtime()
}
