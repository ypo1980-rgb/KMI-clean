package il.kmi.shared.tts

import kotlin.math.max
import kotlin.time.TimeSource

object KmiTtsManager {

    private const val PREF_CLOUD_VOICE = "voice" // "male" | "female"
    private const val VOICE_MALE = "male"
    private const val VOICE_FEMALE = "female"

    // Cloud TTS URL (השרת אחראי על הקול האחיד)
    // שים לב: זה נשאר אותו URL בדיוק
    private const val CLOUD_TTS_URL =
        "https://us-central1-app-1c22cc8d.cloudfunctions.net/kmiTts"

    // Playback speed מקומי (ניגון) — אחיד בין פלטפורמות (Android/iOS)
    private const val SPEED_MIN = 0.60f
    private const val SPEED_MAX = 1.60f
    private var defaultSpeed = 0.90f

    // speakingRate בצד שרת (Google TTS speakingRate)
    private var defaultSpeakingRate = 1.05

    // Cache / dedupe
    private const val DUP_WINDOW_MS = 900L
    private var lastSpeakHash: Int = 0
    private var lastSpeakAtMs: Long = 0L

    // state
    @Volatile private var isInited = false
    @Volatile private var isSpeaking = false

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

    private fun cacheKey(text: String, voice: String, speakingRate: Double): String {
        val srKey = PlatformFormat.f2(speakingRate)
        return (text + "|" + voice + "|sr=" + srKey).hashCode().toString()
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

    private fun normalizeForTts(text: String): String {
        return text
            .replace("ק.מ.י", "קמי")
            .replace("ק מ י", "קמי")
            .replace("K.M.I", "KAMI", ignoreCase = true)
            .replace("K M I", "KAMI", ignoreCase = true)
            .replace("קמי", "קָמִי")
    }
}
