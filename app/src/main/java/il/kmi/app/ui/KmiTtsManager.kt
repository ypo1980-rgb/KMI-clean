package il.kmi.app.ui

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

object KmiTtsManager {

    @Volatile
    private var onSpeechCompleted: (() -> Unit)? = null

    // ✅ PREF שממנו מגיעה בחירת הקול (מהמסך הגדרות שלך)
    private const val PREF_CLOUD_FILE = "app_prefs"
    private const val PREF_CLOUD_VOICE = "kmi_tts_voice" // "male" | "female" | "human"
    private const val VOICE_MALE = "male"
    private const val VOICE_FEMALE = "female"
    private const val VOICE_HUMAN = "human"

    // Cloud Function כתובת Gen1 us-central1
    private const val CLOUD_TTS_URL =
        "https://us-central1-app-1c22cc8d.cloudfunctions.net/kmiTts"

    private fun requireValidCloudUrl(): String {
        val u = CLOUD_TTS_URL.trim()
        require(u.startsWith("https://") && u.contains("cloudfunctions.net/")) {
            "Bad CLOUD_TTS_URL: '$u'"
        }
        return u
    }

    // ✅ שליטה במהירות ניגון (ExoPlayer PlaybackParameters)
    private const val SPEED_MIN = 0.60f
    private const val SPEED_MAX = 1.60f

    // חשוב: לא למתוח את הקול שוב מקומית אם כבר קיבלנו אודיו משרת TTS
    private var defaultSpeed = 1.05f
    private var defaultSpeakingRate = 1.01
    // ✅ פיצול תשובות ארוכות למקטעים טבעיים
    private const val MAX_TTS_CHARS_PER_CHUNK = 220

    // מצב פיתוח בלבד: true יעקוף Cache ויוריד קובצי TTS מחדש
    private const val FORCE_FRESH_TTS = false

    // ✅ מניעת כפילויות (כפול קומפוז / לחיצות רצופות)
    private const val DUP_WINDOW_MS = 900L
    private var lastSpeakHash: Int = 0
    private var lastSpeakAtMs: Long = 0L

    // ✅ state פנימי
    @Volatile private var appCtx: Context? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var inFlightJob: kotlinx.coroutines.Job? = null

    // ✅ ExoPlayer
    private var exo: ExoPlayer? = null

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    fun init(context: Context) {
        appCtx = context.applicationContext

        if (FORCE_FRESH_TTS) {
            clearCloudTtsCache()
        }
    }

    fun setOnSpeechCompletedListener(listener: (() -> Unit)?) {
        onSpeechCompleted = listener
    }

    fun setSpeechProfile(rate: Float = 1.0f, pitch: Float = 1.0f) {
        val newSpeed = rate.coerceIn(SPEED_MIN, SPEED_MAX)
        defaultSpeed = newSpeed
    }

    fun setCloudSpeakingRate(rate: Double) {
        defaultSpeakingRate = rate.coerceIn(0.25, 2.0)
    }

    // מוחק את קבצי הקאש של Cloud TTS: kmi_cloud_tts_*.mp3
    fun clearCloudTtsCache(): Int {
        val ctx = appCtx ?: return 0

        val dir = ctx.cacheDir
        val files = dir.listFiles { f ->
            f.isFile &&
                    f.name.startsWith("kmi_cloud_tts_") &&
                    (f.name.endsWith(".mp3") || f.name.endsWith(".m4a") || f.name.endsWith(".wav"))
        } ?: return 0

        var deleted = 0
        files.forEach { f ->
            runCatching {
                if (f.delete()) deleted++
            }
        }

        return deleted
    }

    fun stop() {
        val job = inFlightJob
        inFlightJob = null
        if (job != null) {
            scope.launch { runCatching { job.cancelAndJoin() } }
        }

        runCatching { exo?.stop() }
        runCatching { exo?.release() }
        exo = null

        onSpeechCompleted = null
    }

    fun speak(text: String) {
        val ctx = appCtx ?: return

        val clean = normalizeForTts(text).trim()
        if (clean.isBlank()) return

        val now = android.os.SystemClock.elapsedRealtime()
        val h = clean.hashCode()
        if (h == lastSpeakHash && (now - lastSpeakAtMs) <= DUP_WINDOW_MS) {
            return
        }
        lastSpeakHash = h
        lastSpeakAtMs = now

        stop()

        val voice = currentVoiceKey(ctx)
        val speakingRate = defaultSpeakingRate
        val chunks = splitForNaturalSpeech(clean)

        inFlightJob = scope.launch {
            try {
                chunks.forEachIndexed { index, chunk ->
                    val mp3 = fetchOrGetCachedMp3(ctx, chunk, voice, speakingRate)

                    playMp3Await(mp3)

                    if (index < chunks.lastIndex) {
                        val pause = when {
                            chunk.endsWith("?") -> 220L
                            chunk.endsWith("!") -> 180L
                            chunk.endsWith(".") -> 140L
                            else -> 80L
                        }
                        delay(pause)
                    }
                }

                val callback = onSpeechCompleted
                onSpeechCompleted = null
                callback?.invoke()

            } catch (_: Throwable) {
                // TTS failure should not crash or block the app.
            }
        }
    }

    // ------------------------------------------------------------
    // Cloud internals
    // ------------------------------------------------------------

    private fun currentVoiceKey(ctx: Context): String {
        val sp = ctx.getSharedPreferences(PREF_CLOUD_FILE, Context.MODE_PRIVATE)

        val raw = sp.getString(PREF_CLOUD_VOICE, VOICE_MALE)
            ?.trim()
            ?.lowercase(Locale.US)

        return when (raw) {
            VOICE_FEMALE -> VOICE_FEMALE
            VOICE_MALE -> VOICE_MALE
            VOICE_HUMAN -> VOICE_HUMAN
            else -> VOICE_MALE
        }
    }

    private suspend fun fetchOrGetCachedMp3(
        ctx: Context,
        text: String,
        voice: String,
        speakingRate: Double
    ): File {
        val srKey = String.format(Locale.US, "%.2f", speakingRate)
        val cacheKey = (text + "|" + voice + "|sr=" + srKey).hashCode().toString()
        val outFile = File(ctx.cacheDir, "kmi_cloud_tts_${cacheKey}.mp3")

        if (FORCE_FRESH_TTS) {
            runCatching { if (outFile.exists()) outFile.delete() }
        } else {
            if (outFile.exists() && outFile.length() > 256) {
                return outFile
            }
        }

        withContext(Dispatchers.IO) {
            val urlStr = requireValidCloudUrl()
            val url = URL(urlStr)

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val body = JSONObject()
                .put("text", text)
                .put("voice", voice)
                .put("speakingRate", speakingRate)
                .toString()

            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode

            if (code !in 200..299) {
                val err = runCatching {
                    conn.errorStream?.readBytes()?.toString(Charsets.UTF_8)
                }.getOrNull()
                throw IllegalStateException("Cloud TTS HTTP $code ${err ?: ""}".trim())
            }

            val bytes = conn.inputStream.use { it.readBytes() }

            if (bytes.size < 256) throw IllegalStateException("Cloud TTS returned too small payload")

            outFile.writeBytes(bytes)
        }

        return outFile
    }

    private suspend fun playMp3Await(mp3: File) = suspendCancellableCoroutine<Unit> { cont ->
        runCatching { exo?.stop() }
        runCatching { exo?.release() }
        exo = null

        val ctx = appCtx
        if (ctx == null) {
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }

        val speed = defaultSpeed.coerceIn(SPEED_MIN, SPEED_MAX)
        val p = ExoPlayer.Builder(ctx).build()
        exo = p

        cont.invokeOnCancellation {
            runCatching { p.stop() }
            runCatching { p.release() }
            if (exo === p) exo = null
        }

        p.addListener(object : Player.Listener {
            private var finished = false

            private fun finish() {
                if (finished) return
                finished = true
                runCatching { p.release() }
                if (exo === p) exo = null

                if (cont.isActive) cont.resume(Unit)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    finish()
                    return
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                if (!isPlayingNow && p.playbackState == Player.STATE_ENDED) {
                    finish()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                finish()
            }
        })

        p.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(mp3)))

        runCatching {
            p.playbackParameters = PlaybackParameters(speed)
        }

        p.prepare()

        runCatching {
            p.playbackParameters = PlaybackParameters(speed)
        }

        p.playWhenReady = true
    }

    // ------------------------------------------------------------
    // Text normalization
    // ------------------------------------------------------------

    private fun splitForNaturalSpeech(text: String): List<String> {
        val normalized = text
            .replace("\n•", ". ")
            .replace("\n-", ". ")
            .replace("\n", ". ")
            .replace("...", ". ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (normalized.length <= MAX_TTS_CHARS_PER_CHUNK) {
            return listOf(normalized)
        }

        val sentences = normalized
            .split(Regex("(?<=[.!?,])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (sentences.isEmpty()) return listOf(normalized)

        val chunks = mutableListOf<String>()
        val current = StringBuilder()

        for (sentence in sentences) {
            if (current.isEmpty()) {
                current.append(sentence)
                continue
            }

            val candidate = current.toString() + " " + sentence
            if (candidate.length <= MAX_TTS_CHARS_PER_CHUNK) {
                current.append(" ").append(sentence)
            } else {
                chunks.add(current.toString().trim())
                current.clear()
                current.append(sentence)
            }
        }

        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }

        return chunks.ifEmpty { listOf(normalized) }
    }

    private fun normalizeForTts(text: String): String {
        return text
            .replace(Regex("""(?m)^\s*\d+\.\s*"""), "")
            .replace("שלום,", "שַלוֹם,")
            .replace("יובל", "יוּבָל")
            .replace("•", ". ")
            .replace(" - ", ". ")
            .replace("\n", ". ")
            .replace(":", ". ")
            .replace(";", ". ")
            .replace("...", ". ")
            .replace("ק.מ.י", "קמי")
            .replace("ק מ י", "קמי")
            .replace("K.M.I", "KAMI", ignoreCase = true)
            .replace("K M I", "KAMI", ignoreCase = true)
            .replace("קמי", "קָמִי")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
