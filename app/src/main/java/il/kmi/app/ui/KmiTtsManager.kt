package il.kmi.app.ui

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object KmiTtsManager {

    private const val TAG = "KMI_TTS"

    // ✅ DEBUG: נוודא שהאובייקט בכלל נטען בזמן ריצה
    init {
        Log.e(TAG, "KmiTtsManager OBJECT LOADED (static init)")
    }

    // ✅ PREF שממנו מגיעה בחירת הקול (מהמסך הגדרות שלך)
    private const val PREF_CLOUD_FILE = "kmi_ai_voice"
    private const val PREF_CLOUD_VOICE = "voice" // "male" | "female"
    private const val VOICE_MALE = "male"
    private const val VOICE_FEMALE = "female"

    // ✅ Cloud Function שלך
    // ✅ Cloud Function שלך
    // ✅ Cloud Function כתובת (Gen1 us-central1)
    private const val CLOUD_TTS_URL =
        "https://us-central1-app-1c22cc8d.cloudfunctions.net/kmiTts"

    // ✅ מהירות ניגון מקומית (MediaPlayer) — משפיע רק על ההשמעה במכשיר
    private const val CLOUD_PLAY_SPEED = 1.25f   // נסה 1.15–1.40

    private fun requireValidCloudUrl(): String {
        val u = CLOUD_TTS_URL.trim()
        Log.e(TAG, "TTS_HTTP url=$u")
        require(u.startsWith("https://") && u.contains("cloudfunctions.net/")) {
            "Bad CLOUD_TTS_URL: '$u'"
        }
        return u
    }

    // ✅ שליטה במהירות ניגון (ExoPlayer PlaybackParameters)
    private const val SPEED_MIN = 0.60f
    private const val SPEED_MAX = 1.60f
    private var defaultSpeed = 0.90f   // ✅ "בן אדם" (אם חזק מדי תרד ל-1.10)

    // ✅ מהירות דיבור בצד השרת (Google TTS speakingRate)
    // בעברית he-IL, 1.0 עדיין נשמע איטי → ברירת מחדל אנושית: ~1.30–1.40
    private var defaultSpeakingRate = 1.05

    // ✅ DEBUG: לבטל קאש זמנית כדי לראות שינוי מהירות בוודאות
    private const val FORCE_FRESH_TTS = false

    // ✅ מניעת כפילויות (כפול קומפוז / לחיצות רצופות)
    private const val DUP_WINDOW_MS = 900L
    private var lastSpeakHash: Int = 0
    private var lastSpeakAtMs: Long = 0L

    // ✅ DEBUG: מזהה ריצה כדי לראות אם speak מתבטל/נדרס
    @Volatile private var speakSeq: Long = 0L

    // ✅ state פנימי
    @Volatile private var appCtx: Context? = null
    @Volatile private var isSpeaking: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var inFlightJob: kotlinx.coroutines.Job? = null

    // ✅ ExoPlayer
    private var exo: ExoPlayer? = null

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    fun init(context: Context) {
        appCtx = context.applicationContext
        Log.e(TAG, "init() OK appCtx=${appCtx?.packageName} cacheDir=${appCtx?.cacheDir?.absolutePath}")
    }

    fun setSpeechProfile(rate: Float = 1.0f, pitch: Float = 1.0f) {
        val newSpeed = rate.coerceIn(SPEED_MIN, SPEED_MAX)
        defaultSpeed = newSpeed
        Log.w(TAG, "setSpeechProfile(speed=$defaultSpeed, pitchIgnored=$pitch)")
    }

    fun setCloudSpeakingRate(rate: Double) {
        defaultSpeakingRate = rate.coerceIn(0.25, 2.0)
        Log.w(TAG, "setCloudSpeakingRate(speakingRate=$defaultSpeakingRate)")
    }

    // מוחק את קבצי הקאש של Cloud TTS: kmi_cloud_tts_*.mp3
    fun clearCloudTtsCache(): Int {
        val ctx = appCtx
        if (ctx == null) {
            Log.w(TAG, "clearCloudTtsCache() called before init()")
            return 0
        }

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
            }.onFailure {
                Log.w(TAG, "Failed to delete cache file: ${f.name}", it)
            }
        }

        Log.w(TAG, "clearCloudTtsCache() deleted=$deleted of ${files.size} files")
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

        isSpeaking = false
        Log.d(TAG, "stop()")
    }

    fun speak(text: String) {
        Log.e(TAG, ">>> KmiTtsManager.speak() CALLED <<< rawLen=${text.length}")

        val ctx = appCtx
        if (ctx == null) {
            Log.w(TAG, "speak() called before init()")
            return
        }

        val clean = normalizeForTts(text).trim()
        Log.e(
            TAG,
            ">>> speak cleanLen=${clean.length} speed=${String.format(Locale.US, "%.2f", defaultSpeed)} sr=${String.format(Locale.US, "%.2f", defaultSpeakingRate)} <<<"
        )
        if (clean.isBlank()) return

        val now = android.os.SystemClock.elapsedRealtime()
        val h = clean.hashCode()
        if (h == lastSpeakHash && (now - lastSpeakAtMs) <= DUP_WINDOW_MS) {
            Log.w(TAG, "Skip duplicate speak within ${DUP_WINDOW_MS}ms (hash=$h)")
            return
        }
        lastSpeakHash = h
        lastSpeakAtMs = now

        val seq = ++speakSeq
        Log.e(TAG, "SPEAK(seq=$seq) len=${clean.length} preview='${clean.take(60)}'")

        // ✅ עוצרים את הקודם לפני שמתחילים חדש
        stop()

        val voice = currentVoiceKey(ctx)
        val speakingRate = defaultSpeakingRate
        Log.e(
            TAG,
            "SPEAK(seq=$seq) voice=$voice speakingRate=${String.format(Locale.US, "%.2f", speakingRate)}"
        )

        inFlightJob = scope.launch {
            try {
                isSpeaking = true
                Log.e(TAG, "SPEAK(seq=$seq) -> IO start")
                val mp3 = fetchOrGetCachedMp3(ctx, clean, voice, speakingRate)
                Log.e(TAG, "SPEAK(seq=$seq) -> play file=${mp3.name} bytes=${mp3.length()}")
                playMp3(mp3)
            } catch (t: Throwable) {
                Log.e(TAG, "SPEAK(seq=$seq) failed", t)
                isSpeaking = false
            }
        }
    }

    // ------------------------------------------------------------
    // Cloud internals
    // ------------------------------------------------------------

    private fun currentVoiceKey(ctx: Context): String {
        val sp = ctx.getSharedPreferences(PREF_CLOUD_FILE, Context.MODE_PRIVATE)
        val raw = sp.getString(PREF_CLOUD_VOICE, VOICE_MALE)?.trim()?.lowercase(Locale.US)
        return when (raw) {
            VOICE_FEMALE -> VOICE_FEMALE
            VOICE_MALE -> VOICE_MALE
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
            Log.w(TAG, "DEBUG FORCE_FRESH_TTS -> bypass cache sr=$srKey voice=$voice file=${outFile.name}")
        } else {
            if (outFile.exists() && outFile.length() > 256) {
                Log.d(TAG, "Cloud TTS cache HIT bytes=${outFile.length()} file=${outFile.name} sr=$srKey")
                return outFile
            }
        }

        withContext(Dispatchers.IO) {
            Log.e(TAG, "TTS_HTTP -> POST url=$CLOUD_TTS_URL voice=$voice sr=$srKey textLen=${text.length}")

            val urlStr = requireValidCloudUrl()
            val url = URL(urlStr)

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12_000
                readTimeout = 25_000
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
            val ct = conn.contentType
            val cl = conn.getHeaderField("Content-Length")
            val ce = conn.getHeaderField("Content-Encoding")
            Log.w(TAG, "Cloud headers: code=$code contentType=$ct contentLen=$cl contentEnc=$ce")

            val hdrRate = conn.getHeaderField("X-KMI-Rate")
            val hdrSsml = conn.getHeaderField("X-KMI-SSML-Rate")
            Log.w(TAG, "DEBUG server used -> X-KMI-Rate=$hdrRate X-KMI-SSML-Rate=$hdrSsml")

            if (code !in 200..299) {
                val err = runCatching {
                    conn.errorStream?.readBytes()?.toString(Charsets.UTF_8)
                }.getOrNull()
                throw IllegalStateException("Cloud TTS HTTP $code ${err ?: ""}".trim())
            }

            val bytes = conn.inputStream.use { it.readBytes() }

            val headHex = bytes.take(24).joinToString(" ") { b -> "%02X".format(b) }
            val headAscii = bytes.take(24).map { b ->
                val c = b.toInt() and 0xFF
                if (c in 32..126) c.toChar() else '.'
            }.joinToString("")
            Log.w(TAG, "Cloud payload: bytes=${bytes.size} headHex=$headHex headAscii='$headAscii' sr=$srKey")

            if (bytes.size < 256) throw IllegalStateException("Cloud TTS returned too small payload")

            outFile.writeBytes(bytes)
            Log.d(TAG, "Cloud TTS OK bytes=${bytes.size} file=${outFile.name} sr=$srKey")
        }

        return outFile
    }

    private fun playMp3(mp3: File) {
        runCatching { exo?.stop() }
        runCatching { exo?.release() }
        exo = null

        val ctx = appCtx ?: return
        val speed = defaultSpeed.coerceIn(SPEED_MIN, SPEED_MAX)

        val p = ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(mp3)))

            // ✅ מחילים לפני prepare
            try {
                playbackParameters = PlaybackParameters(speed)
            } catch (t: Throwable) {
                Log.e(TAG, "PLAY failed to set playbackParameters BEFORE prepare", t)
            }

            prepare()

            // ✅ מחילים שוב אחרי prepare (כדי לשלול מקרה שהפרמטר נדרס)
            try {
                playbackParameters = PlaybackParameters(speed)
            } catch (t: Throwable) {
                Log.e(TAG, "PLAY failed to set playbackParameters AFTER prepare", t)
            }

            playWhenReady = true

            Log.e(
                TAG,
                "PLAY dbg -> requestedSpeed=$speed actual=${playbackParameters.speed} file=${mp3.name} bytes=${mp3.length()}"
            )

            addListener(object : Player.Listener {

                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                    Log.w(
                        TAG,
                        "PLAY onPlaybackParametersChanged -> speed=${playbackParameters.speed}"
                    )
                }

                override fun onPlaybackStateChanged(state: Int) {
                    Log.w(TAG, "PLAY state=$state isPlaying=$isPlaying speedNow=${playbackParameters.speed}")
                    if (state == Player.STATE_ENDED) {
                        Log.w(TAG, "PLAY ended file=${mp3.name}")
                        isSpeaking = false
                        runCatching { release() }
                        if (exo === this@apply) exo = null
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "PLAY error file=${mp3.name}", error)
                    isSpeaking = false
                    runCatching { release() }
                    if (exo === this@apply) exo = null
                }
            })
        }

        exo = p
        Log.e(TAG, "PLAY -> ExoPlayer started speed=$speed file=${mp3.name} bytes=${mp3.length()}")
    }

    // ------------------------------------------------------------
    // Text normalization
    // ------------------------------------------------------------

    private fun normalizeForTts(text: String): String {
        return text
            .replace("ק.מ.י", "קמי")
            .replace("ק מ י", "קמי")
            .replace("K.M.I", "KAMI", ignoreCase = true)
            .replace("K M I", "KAMI", ignoreCase = true)
            .replace("קמי", "קָמִי")
    }
}
