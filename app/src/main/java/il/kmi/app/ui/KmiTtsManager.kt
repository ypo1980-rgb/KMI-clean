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

    private const val TAG = "KMI_TTS"

    @Volatile
    private var onSpeechCompleted: (() -> Unit)? = null

    // ✅ DEBUG: נוודא שהאובייקט בכלל נטען בזמן ריצה
    init {
        Log.e(TAG, "KmiTtsManager OBJECT LOADED (static init)")
    }

    // ✅ PREF שממנו מגיעה בחירת הקול (מהמסך הגדרות שלך)
    private const val PREF_CLOUD_FILE = "app_prefs"
    private const val PREF_CLOUD_VOICE = "kmi_tts_voice" // "male" | "female" | "human"
    private const val VOICE_MALE = "male"
    private const val VOICE_FEMALE = "female"
    private const val VOICE_HUMAN = "human"
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

    // חשוב: לא למתוח את הקול שוב מקומית אם כבר קיבלנו אודיו משרת TTS
    private var defaultSpeed = 1.05f
    private var defaultSpeakingRate = 1.01
    // ✅ פיצול תשובות ארוכות למקטעים טבעיים
    private const val MAX_TTS_CHARS_PER_CHUNK = 220
    private const val CHUNK_PAUSE_MS = 60L

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

        if (FORCE_FRESH_TTS) {
            val deleted = clearCloudTtsCache()
            Log.w(TAG, "init() FORCE_FRESH_TTS deletedCacheFiles=$deleted")
        }
    }

    fun setOnSpeechCompletedListener(listener: (() -> Unit)?) {
        onSpeechCompleted = listener
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
        onSpeechCompleted = null
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

        stop()

        val voice = currentVoiceKey(ctx)
        val speakingRate = defaultSpeakingRate
        val chunks = splitForNaturalSpeech(clean)

        Log.e(
            TAG,
            "SPEAK(seq=$seq) voice=$voice speakingRate=${String.format(Locale.US, "%.2f", speakingRate)} forceFresh=$FORCE_FRESH_TTS chunks=${chunks.size}"
        )

        inFlightJob = scope.launch {
            try {
                isSpeaking = true

                chunks.forEachIndexed { index, chunk ->
                    Log.e(TAG, "SPEAK(seq=$seq) -> chunk ${index + 1}/${chunks.size} len=${chunk.length}")

                    val mp3 = fetchOrGetCachedMp3(ctx, chunk, voice, speakingRate)
                    Log.e(TAG, "SPEAK(seq=$seq) -> play file=${mp3.name} bytes=${mp3.length()}")

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

            } catch (t: Throwable) {
                Log.e(TAG, "SPEAK(seq=$seq) failed", t)
            } finally {
                isSpeaking = false
            }
        }
    }

    // ------------------------------------------------------------
    // Cloud internals
    // ------------------------------------------------------------

    private fun currentVoiceKey(ctx: Context): String {
        val sp = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val raw = sp.getString("kmi_tts_voice", "male")
            ?.trim()
            ?.lowercase(Locale.US)

        val normalized = when (raw) {
            "female" -> VOICE_FEMALE
            "male" -> VOICE_MALE
            "human" -> VOICE_HUMAN
            else -> VOICE_MALE
        }

        Log.w(TAG, "currentVoiceKey() rawPref=$raw normalized=$normalized prefFile=app_prefs prefKey=kmi_tts_voice")

        return normalized
    }

    private fun cleanForSpeech(text: String): String {

        return text
            .replace("(", "")
            .replace(")", "")
            .replace("–", " ")
            .replace("-", " ")
            .replace(",", ", ")
            .replace("  ", " ")
            .trim()
    }

    private fun hourToHebrewSpeech(time: String): String {

        val parts = time.split(":")
        if (parts.size < 2) return time

        val hour = parts[0].toIntOrNull() ?: return time
        val minute = parts[1].toIntOrNull() ?: 0

        val hourWords = mapOf(
            1 to "אחת",
            2 to "שתיים",
            3 to "שלוש",
            4 to "ארבע",
            5 to "חמש",
            6 to "שש",
            7 to "שבע",
            8 to "שמונה",
            9 to "תשע",
            10 to "עשר",
            11 to "אחת עשרה",
            12 to "שתים עשרה"
        )

        val period = when (hour) {
            in 5..11 -> "בבוקר"
            in 12..16 -> "בצהריים"
            in 17..21 -> "בערב"
            else -> "בלילה"
        }

        val h = if (hour > 12) hour - 12 else hour

        val hourText = hourWords[h] ?: return time

        return if (minute == 0) {
            "$hourText $period"
        } else {
            "$hourText ו-$minute $period"
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
            val ct = conn.contentType
            val cl = conn.getHeaderField("Content-Length")
            val ce = conn.getHeaderField("Content-Encoding")
            Log.w(TAG, "Cloud headers: code=$code contentType=$ct contentLen=$cl contentEnc=$ce")

            val hdrVersion = conn.getHeaderField("X-KMI-Version")
            val hdrRate = conn.getHeaderField("X-KMI-Rate")
            val hdrStyle = conn.getHeaderField("X-KMI-Style")
            val hdrVoice = conn.getHeaderField("X-KMI-Voice")
            val hdrEngine = conn.getHeaderField("X-KMI-Engine")

            Log.w(
                TAG,
                "DEBUG server used -> X-KMI-Version=$hdrVersion X-KMI-Voice=$hdrVoice X-KMI-Engine=$hdrEngine X-KMI-Style=$hdrStyle X-KMI-Rate=$hdrRate"
            )

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

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                Log.w(TAG, "PLAY onPlaybackParametersChanged -> speed=${playbackParameters.speed}")
            }

            override fun onPlaybackStateChanged(state: Int) {
                Log.w(TAG, "PLAY state=$state isPlaying=${p.isPlaying} speedNow=${p.playbackParameters.speed}")

                if (state == Player.STATE_ENDED) {
                    Log.w(TAG, "PLAY ended file=${mp3.name}")
                    finish()
                    return
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                Log.w(TAG, "PLAY onIsPlayingChanged=$isPlayingNow state=${p.playbackState}")

                if (!isPlayingNow && p.playbackState == Player.STATE_ENDED) {
                    finish()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "PLAY error file=${mp3.name}", error)
                finish()
            }
        })

        p.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(mp3)))

        try {
            p.playbackParameters = PlaybackParameters(speed)
        } catch (t: Throwable) {
            Log.e(TAG, "PLAY failed to set playbackParameters BEFORE prepare", t)
        }

        p.prepare()

        try {
            p.playbackParameters = PlaybackParameters(speed)
        } catch (t: Throwable) {
            Log.e(TAG, "PLAY failed to set playbackParameters AFTER prepare", t)
        }

        p.playWhenReady = true

        Log.e(
            TAG,
            "PLAY -> ExoPlayer started speed=$speed file=${mp3.name} bytes=${mp3.length()}"
        )
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
