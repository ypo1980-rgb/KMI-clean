package il.kmi.shared.tts

object KmiTtsManager {

    private const val PREF_CLOUD_VOICE = "voice" // "male" | "female"
    private const val VOICE_MALE = "male"
    private const val VOICE_FEMALE = "female"

    /*
     * יש להחליף ערך בכל שינוי מהותי בקול,
     * במהירות או בכללי ההגייה.
     */
    private const val TTS_CACHE_VERSION = "chirp3_he_v5"

    // Cloud TTS URL (השרת אחראי על הקול האחיד)
    // שים לב: זה נשאר אותו URL בדיוק
    private const val CLOUD_TTS_URL =
        "https://us-central1-app-1c22cc8d.cloudfunctions.net/kmiTts"

    /*
     * קובץ הקול כבר מופק במהירות המתאימה בשרת.
     * ניגון מקומי במהירות שונה מ־1 עלול לפגוע
     * בטבעיות הקול ובהגיית מילים בעברית.
     */
    private const val SPEED_MIN = 0.85f
    private const val SPEED_MAX = 1.15f
    private var defaultSpeed = 1.0f

    /*
     * Chirp 3 HD נשמע טבעי ומדויק יותר בעברית
     * בקצב מעט איטי מברירת המחדל הקודמת.
     */
    private var defaultSpeakingRate = 0.98

    // Cache / dedupe
    private const val DUP_WINDOW_MS = 900L
    private var lastSpeakHash: Int = 0
    private var lastSpeakAtMs: Long = 0L

    // state
    private var isInited = false
    private var isSpeaking = false

    private var audio: PlatformAudioPlayer? = null

    fun init(platform: PlatformContext) {
        if (isInited) return
        PlatformEnv.init(platform)
        audio = PlatformAudioPlayer()
        isInited = true
    }

    fun setSpeechProfile(rate: Float = 1.0f, pitch: Float = 1.0f) {
        // pitch לא בשימוש כרגע (אפשר להוסיף בעתיד אם השרת תומך)
        defaultSpeed = rate.coerceIn(SPEED_MIN, SPEED_MAX)
    }

    fun setCloudSpeakingRate(rate: Double) {
        defaultSpeakingRate = rate.coerceIn(0.25, 2.0)
    }

    fun stop() {
        audio?.stop()
        isSpeaking = false
    }

    fun clearCloudTtsCache(): Int {
        return PlatformCache.deleteByPrefix(prefix = "kmi_cloud_tts_", suffix = ".mp3")
    }

    fun speak(text: String) {
        if (!isInited) return

        val clean = normalizeForTts(text).trim()
        if (clean.isBlank()) return

        val nowMs = PlatformClock.nowMs()
        val h = clean.hashCode()
        if (h == lastSpeakHash && (nowMs - lastSpeakAtMs) <= DUP_WINDOW_MS) {
            return
        }
        lastSpeakHash = h
        lastSpeakAtMs = nowMs

        // stop current
        stop()

        val voice = currentVoiceKey()
        val sr = defaultSpeakingRate
        val speed = defaultSpeed.coerceIn(SPEED_MIN, SPEED_MAX)

        isSpeaking = true

        PlatformCoroutines.launchBackground {
            try {
                val mp3File = fetchOrGetCachedMp3(
                    text = clean,
                    voice = voice,
                    speakingRate = sr
                )

                PlatformCoroutines.launchMain {
                    audio?.playFile(mp3File.absolutePath, speed)
                }
            } catch (_: Throwable) {
                isSpeaking = false
            }
        }
    }

    // ------------------------------------------------------------
    // internals
    // ------------------------------------------------------------

    private fun currentVoiceKey(): String {
        val raw = PlatformPrefs.getString(PREF_CLOUD_VOICE, VOICE_MALE)
            .trim()
            .lowercase()
        return when (raw) {
            VOICE_FEMALE -> VOICE_FEMALE
            VOICE_MALE -> VOICE_MALE
            else -> VOICE_MALE
        }
    }

    private fun cacheKey(
        text: String,
        voice: String,
        speakingRate: Double
    ): String {
        val srKey =
            PlatformFormat.f2(speakingRate)

        return (
                TTS_CACHE_VERSION +
                        "|" +
                        text +
                        "|" +
                        voice +
                        "|sr=" +
                        srKey
                )
            .hashCode()
            .toString()
    }

    private suspend fun fetchOrGetCachedMp3(
        text: String,
        voice: String,
        speakingRate: Double
    ): PlatformFile {
        val key = cacheKey(text, voice, speakingRate)
        val fileName = "kmi_cloud_tts_${key}.mp3"

        val existing = PlatformCache.fileIfExists(fileName)
        if (existing != null && existing.sizeBytes > 256) return existing

        val bodyJson = PlatformJson.obj(
            "text" to text,
            "voice" to voice,
            "speakingRate" to speakingRate
        )

        val bytes = PlatformHttp.postJson(
            url = CLOUD_TTS_URL,
            jsonBody = bodyJson
        )

        if (bytes.size < 256) error("Cloud TTS payload too small")

        return PlatformCache.writeFile(fileName, bytes)
    }

    private fun normalizeForTts(
        text: String
    ): String {
        return text
            /*
             * סימוני עיצוב שאינם מיועדים להקראה.
             */
            .replace(
                Regex(
                    pattern = """\[\[\s*/?\s*(?:RED_BOLD|BLUE_BOLD)\s*]]""",
                    option = RegexOption.IGNORE_CASE
                ),
                ""
            )

            /*
             * הגייה אחידה של ק.מ.י.
             */
            .replace(
                Regex(
                    pattern = """ק\s*[.\-־]?\s*מ\s*[.\-־]?\s*י""",
                    option = RegexOption.IGNORE_CASE
                ),
                "קָמִי"
            )
            .replace(
                Regex(
                    pattern = """K\s*[.\-]?\s*M\s*[.\-]?\s*I""",
                    option = RegexOption.IGNORE_CASE
                ),
                "KAMI"
            )
            .replace(
                "קמי",
                "קָמִי"
            )

            /*
             * הפיכת מבנה רשימה למשפטים עם הפסקה טבעית.
             */
            .replace(
                Regex("""(?m)^\s*\d+[.)]\s*"""),
                ""
            )
            .replace(
                Regex("""(?m)^\s*[•●▪◦]\s*"""),
                ""
            )
            .replace(
                Regex("""\s+[-–—]\s+"""),
                ". "
            )
            .replace(
                Regex("""\n{2,}"""),
                ". "
            )
            .replace(
                Regex("""\n"""),
                ". "
            )

            /*
             * האפליקציה פונה למשתמש בלשון זכר.
             *
             * המילה "שלך" ללא ניקוד עלולה להיקרא
             * בטעות בצורת נקבה. הניקוד משפיע רק
             * על ההקראה ולא על הטקסט המוצג במסך.
             */
            .replace(
                Regex("""(?<![א-ת])שלך(?![א-ת])"""),
                "שֶׁלְּךָ"
            )

            /*
             * ניקוי רווחים ונקודות כפולות.
             */
            .replace(
                Regex("""\s*\.\s*\.+"""),
                ". "
            )
            .replace(
                Regex("""\s+([,.:;!?])"""),
                "$1"
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
    }
}
