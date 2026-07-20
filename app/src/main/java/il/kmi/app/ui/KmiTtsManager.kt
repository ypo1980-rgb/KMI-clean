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

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var inFlightJob: kotlinx.coroutines.Job? = null

    /*
     * מזהה חד־ערכי של ההקראה הפעילה.
     * הוא מונע מהקראה ישנה שהופסקה לסגור בטעות
     * את אנימציית הדיבור של הקראה חדשה.
     */
    @Volatile
    private var speechGeneration: Long = 0L

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
        /*
         * מבטלים את תוקף ההקראה הקודמת לפני עצירת הנגן.
         * כך callback מאוחר שלה לא ישפיע על הקראה חדשה.
         */
        speechGeneration += 1L

        val job = inFlightJob
        inFlightJob = null

        if (job != null) {
            scope.launch {
                runCatching { job.cancelAndJoin() }
            }
        }

        runCatching { exo?.stop() }
        runCatching { exo?.release() }
        exo = null

        onSpeechCompleted = null
    }

    fun speak(text: String) {
        /*
         * שומרים את ה-listener לפני stop().
         * stop() מנקה את ה-listener הגלובלי ולכן בלי השמירה
         * הזאת המסך לא מקבל הודעת סיום.
         */
        val completionCallback = onSpeechCompleted
        onSpeechCompleted = null

        val ctx = appCtx
        if (ctx == null) {
            completionCallback?.invoke()
            return
        }

        val clean = normalizeForTts(text).trim()
        if (clean.isBlank()) {
            completionCallback?.invoke()
            return
        }

        val now = android.os.SystemClock.elapsedRealtime()
        val hash = clean.hashCode()

        if (
            hash == lastSpeakHash &&
            now - lastSpeakAtMs <= DUP_WINDOW_MS
        ) {
            /*
             * גם במקרה של מניעת דיבור כפול חייבים להודיע
             * למסך שהפעולה הסתיימה.
             */
            completionCallback?.invoke()
            return
        }

        lastSpeakHash = hash
        lastSpeakAtMs = now

        stop()

        /*
         * לאחר stop() זהו המזהה של ההקראה החדשה.
         */
        val requestGeneration = speechGeneration

        val voice = currentVoiceKey(ctx)
        val speakingRate = defaultSpeakingRate
        val chunks = splitForNaturalSpeech(clean)

        inFlightJob = scope.launch {
            try {
                chunks.forEachIndexed { index, chunk ->
                    val mp3 = fetchOrGetCachedMp3(
                        ctx = ctx,
                        text = chunk,
                        voice = voice,
                        speakingRate = speakingRate
                    )

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
            } catch (_: Throwable) {
                /*
                 * גם כשל בהורדה או בניגון צריך לסיים
                 * מיד את מצב "מדבר..." במסך.
                 */
            } finally {
                val isCurrentRequest =
                    requestGeneration == speechGeneration

                if (isCurrentRequest) {
                    inFlightJob = null

                    runCatching { exo?.release() }
                    exo = null

                    completionCallback?.invoke()
                }
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
        if (text.isBlank()) return ""

        val containsHebrew =
            Regex("[\\u0590-\\u05FF]").containsMatchIn(text)

        var result = removeTechnicalContent(text)

        if (containsHebrew) {
            /*
             * ממירים קודם תאריכים ורק לאחר מכן שעות,
             * לפני שמחליפים נקודתיים וסימני פיסוק.
             */
            result = replaceHebrewDates(result)
            result = replaceHebrewTimes(result)
        }

        return result
            .replace(Regex("""(?m)^\s*\d+[.)]\s*"""), "")
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
            .replace(Regex("\\.{2,}"), ". ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /*
     * מסיר מידע טכני שאינו מיועד להקראה:
     * Markdown, קוד, JSON, כתובות אינטרנט ותגיות HTML.
     */
    private fun removeTechnicalContent(text: String): String {
        var cleaned = text
            // בלוקי קוד Markdown מלאים.
            .replace(
                Regex(
                    pattern = "```[\\s\\S]*?```",
                    option = RegexOption.IGNORE_CASE
                ),
                " "
            )
            // קוד קצר שמופיע בין backticks.
            .replace(Regex("`[^`\\n]+`"), " ")
            // קישורי Markdown: שומרים רק את הכותרת.
            .replace(
                Regex("\\[([^\\]]+)]\\([^)]*\\)")
            ) { match ->
                match.groupValues.getOrNull(1).orEmpty()
            }
            // כתובות אינטרנט אינן שימושיות בהקראה קולית.
            .replace(
                Regex(
                    "https?://\\S+|www\\.\\S+",
                    RegexOption.IGNORE_CASE
                ),
                " "
            )
            // תגיות HTML.
            .replace(Regex("<[^>]+>"), " ")

        cleaned = cleaned
            .lineSequence()
            .filterNot { line -> looksLikeCodeLine(line) }
            .joinToString("\n")

        return cleaned.trim()
    }

    private fun looksLikeCodeLine(rawLine: String): Boolean {
        val line = rawLine.trim()

        if (line.isBlank()) return false

        val lowercaseLine = line.lowercase(Locale.ROOT)

        val codePrefixes = listOf(
            "package ",
            "import ",
            "class ",
            "object ",
            "interface ",
            "enum class ",
            "fun ",
            "private fun ",
            "public fun ",
            "internal fun ",
            "protected fun ",
            "override fun ",
            "val ",
            "var ",
            "const val ",
            "private val ",
            "private var ",
            "return ",
            "when (",
            "if (",
            "else {",
            "try {",
            "catch (",
            "@composable",
            "@override"
        )

        if (codePrefixes.any { lowercaseLine.startsWith(it) }) {
            return true
        }

        if (
            line == "{" ||
            line == "}" ||
            line == "}," ||
            line == "]," ||
            line == ");" ||
            line == ") {"
        ) {
            return true
        }

        val looksLikeJsonProperty =
            Regex("""^["'][^"']+["']\s*:\s*.+[,}]?$""")
                .matches(line)

        if (looksLikeJsonProperty) return true

        val hasCodeAssignment =
            Regex("""\b(val|var|const)\s+\w+\s*=""")
                .containsMatchIn(line)

        if (hasCodeAssignment) return true

        val hasFunctionCallSyntax =
            line.contains("(") &&
                    line.contains(")") &&
                    (
                            line.endsWith("{") ||
                                    line.endsWith(";") ||
                                    line.contains("?.") ||
                                    line.contains("::")
                            )

        return hasFunctionCallSyntax
    }

    // ------------------------------------------------------------
    // Natural Hebrew dates
    // ------------------------------------------------------------

    private fun replaceHebrewDates(text: String): String {
        var result = text

        /*
         * תאריך ישראלי:
         * 20/07/2026
         * 20-07-2026
         * 20.07.2026
         */
        result = result.replace(
            Regex(
                """(?<!\d)(\d{1,2})[./-](\d{1,2})[./-](\d{4})(?!\d)"""
            )
        ) { match ->
            val day = match.groupValues[1].toIntOrNull()
            val month = match.groupValues[2].toIntOrNull()
            val year = match.groupValues[3].toIntOrNull()

            buildHebrewDate(
                day = day,
                month = month,
                year = year,
                original = match.value
            )
        }

        /*
         * תאריך ISO:
         * 2026-07-20
         */
        result = result.replace(
            Regex(
                """(?<!\d)(\d{4})-(\d{1,2})-(\d{1,2})(?!\d)"""
            )
        ) { match ->
            val year = match.groupValues[1].toIntOrNull()
            val month = match.groupValues[2].toIntOrNull()
            val day = match.groupValues[3].toIntOrNull()

            buildHebrewDate(
                day = day,
                month = month,
                year = year,
                original = match.value
            )
        }

        return result
    }

    private fun buildHebrewDate(
        day: Int?,
        month: Int?,
        year: Int?,
        original: String
    ): String {
        if (
            day == null ||
            month == null ||
            year == null ||
            day !in 1..31 ||
            month !in 1..12
        ) {
            return original
        }

        val monthName = hebrewGregorianMonth(month)
            ?: return original

        val spokenDay = hebrewCardinalNumber(day)
        val spokenYear = hebrewCardinalNumber(year)

        return "$spokenDay ב$monthName, $spokenYear"
    }

    private fun hebrewGregorianMonth(month: Int): String? {
        return when (month) {
            1 -> "ינואר"
            2 -> "פברואר"
            3 -> "מרץ"
            4 -> "אפריל"
            5 -> "מאי"
            6 -> "יוני"
            7 -> "יולי"
            8 -> "אוגוסט"
            9 -> "ספטמבר"
            10 -> "אוקטובר"
            11 -> "נובמבר"
            12 -> "דצמבר"
            else -> null
        }
    }

    // ------------------------------------------------------------
    // Natural Hebrew times
    // ------------------------------------------------------------

    private fun replaceHebrewTimes(text: String): String {
        return text.replace(
            Regex(
                """(?<!\d)([01]?\d|2[0-3]):([0-5]\d)(?!\d)"""
            )
        ) { match ->
            val hour = match.groupValues[1].toIntOrNull()
            val minute = match.groupValues[2].toIntOrNull()

            if (hour == null || minute == null) {
                match.value
            } else {
                buildNaturalHebrewTime(
                    hour24 = hour,
                    minute = minute
                )
            }
        }
    }

    private fun buildNaturalHebrewTime(
        hour24: Int,
        minute: Int
    ): String {
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }

        val spokenHour = hebrewHour(hour12)

        val spokenMinute = when (minute) {
            0 -> ""
            15 -> " ורבע"
            30 -> " וחצי"
            else -> {
                val minuteText = hebrewCardinalNumber(minute)
                " ו$minuteText דקות"
            }
        }

        val period = when (hour24) {
            in 0..4 -> "בלילה"
            in 5..11 -> "בבוקר"
            in 12..16 -> "בצהריים"
            in 17..20 -> "בערב"
            else -> "בלילה"
        }

        return "$spokenHour$spokenMinute $period"
    }

    /*
     * שעות בעברית נאמרות בלשון נקבה:
     * אחת, שתיים, שלוש וכן הלאה.
     */
    private fun hebrewHour(hour: Int): String {
        return when (hour) {
            1 -> "אחת"
            2 -> "שתיים"
            3 -> "שלוש"
            4 -> "ארבע"
            5 -> "חמש"
            6 -> "שש"
            7 -> "שבע"
            8 -> "שמונה"
            9 -> "תשע"
            10 -> "עשר"
            11 -> "אחת עשרה"
            12 -> "שתים עשרה"
            else -> hour.toString()
        }
    }

    // ------------------------------------------------------------
    // Hebrew numbers
    // ------------------------------------------------------------

    private fun hebrewCardinalNumber(number: Int): String {
        if (number < 0) {
            return "מינוס ${hebrewCardinalNumber(-number)}"
        }

        if (number == 0) return "אפס"

        if (number < 100) {
            return hebrewNumberBelowOneHundred(number)
        }

        if (number < 1_000) {
            val hundreds = number / 100
            val remainder = number % 100

            val hundredsText = when (hundreds) {
                1 -> "מאה"
                2 -> "מאתיים"
                3 -> "שלוש מאות"
                4 -> "ארבע מאות"
                5 -> "חמש מאות"
                6 -> "שש מאות"
                7 -> "שבע מאות"
                8 -> "שמונה מאות"
                9 -> "תשע מאות"
                else -> ""
            }

            return if (remainder == 0) {
                hundredsText
            } else {
                "$hundredsText ו${hebrewNumberBelowOneHundred(remainder)}"
            }
        }

        if (number < 10_000) {
            val thousands = number / 1_000
            val remainder = number % 1_000

            val thousandsText = when (thousands) {
                1 -> "אלף"
                2 -> "אלפיים"
                3 -> "שלושת אלפים"
                4 -> "ארבעת אלפים"
                5 -> "חמשת אלפים"
                6 -> "ששת אלפים"
                7 -> "שבעת אלפים"
                8 -> "שמונת אלפים"
                9 -> "תשעת אלפים"
                else -> ""
            }

            return if (remainder == 0) {
                thousandsText
            } else {
                "$thousandsText ו${hebrewCardinalNumber(remainder)}"
            }
        }

        return number.toString()
    }

    private fun hebrewNumberBelowOneHundred(number: Int): String {
        if (number !in 0..99) return number.toString()

        val directNumbers = mapOf(
            0 to "אפס",
            1 to "אחד",
            2 to "שניים",
            3 to "שלושה",
            4 to "ארבעה",
            5 to "חמישה",
            6 to "שישה",
            7 to "שבעה",
            8 to "שמונה",
            9 to "תשעה",
            10 to "עשרה",
            11 to "אחד עשר",
            12 to "שנים עשר",
            13 to "שלושה עשר",
            14 to "ארבעה עשר",
            15 to "חמישה עשר",
            16 to "שישה עשר",
            17 to "שבעה עשר",
            18 to "שמונה עשר",
            19 to "תשעה עשר",
            20 to "עשרים",
            30 to "שלושים",
            40 to "ארבעים",
            50 to "חמישים",
            60 to "שישים",
            70 to "שבעים",
            80 to "שמונים",
            90 to "תשעים"
        )

        directNumbers[number]?.let { return it }

        val tens = number / 10 * 10
        val units = number % 10

        val tensText = directNumbers[tens].orEmpty()
        val unitsText = directNumbers[units].orEmpty()

        return "$tensText ו$unitsText"
    }
}
