package il.kmi.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * מנהל "מילת התעוררות" – מאזין ל:
 *
 *   "יובל שומע"
 *
 * כל עוד האפליקציה פתוחה בפרונט.
 */
object WakeWordManager : RecognitionListener {

    private const val TAG = "WakeWordManager"
    private const val WAKE_PHRASE = "יובל שומע"

    private var speechRecognizer: SpeechRecognizer? = null
    private var isActive: Boolean = false
    private var appContext: Context? = null
    private var onWakeCallback: (() -> Unit)? = null

    /**
     * להתחיל האזנה למילת התעוררות.
     *
     * @param context – חשוב להעביר context של האפליקציה / Activity חי
     * @param onWake – מה לעשות כשמזהים "יובל שומע"
     */
    fun start(context: Context, onWake: () -> Unit) {
        if (isActive) {
            Log.d(TAG, "WakeWord already active")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer is not available on this device")
            return
        }

        isActive = true
        appContext = context.applicationContext
        onWakeCallback = onWake

        createRecognizer()
        startListeningInternal()
    }

    /**
     * לעצור האזנה (לקרוא למשל ב־onPause / onStop).
     */
    fun stop() {
        isActive = false
        onWakeCallback = null

        try {
            speechRecognizer?.setRecognitionListener(null)
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Throwable) {
        }
        speechRecognizer = null
    }

    private fun createRecognizer() {
        val ctx = appContext ?: return
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(this@WakeWordManager)
        }
    }

    private fun createIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "il.kmi.app")
            // טקסט שיופיע בממשק של גוגל אם יוצג
            putExtra(RecognizerIntent.EXTRA_PROMPT, "תגיד: \"יובל שומע\" כדי להפעיל את העוזר")
        }
    }

    private fun startListeningInternal() {
        if (!isActive) return

        val recognizer = speechRecognizer ?: run {
            createRecognizer()
            speechRecognizer ?: return
        }

        try {
            recognizer.stopListening()
        } catch (_: Throwable) { }

        try {
            val intent = createIntent()
            recognizer.startListening(intent)
            Log.d(TAG, "WakeWord listening started")
        } catch (t: Throwable) {
            Log.e(TAG, "startListeningInternal failed", t)
        }
    }

    // ─────────────────────
    //  RecognitionListener
    // ─────────────────────

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // אפשר בעתיד להשתמש ב־rmsdB בשביל אנימציות
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
    }

    override fun onError(error: Int) {
        Log.w(TAG, "onError: $error")
        // במקרים רבים כדאי לנסות שוב, אם עדיין פעיל
        if (isActive) {
            // delay קטן כדי לא להיכנס ללופ שגיאות מטורף
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                { startListeningInternal() },
                800
            )
        }
    }

    override fun onResults(results: Bundle?) {
        handleResults(results)
        // אחרי סשן – אם עדיין פעיל, חוזרים להאזין
        if (isActive) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                { startListeningInternal() },
                500
            )
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // אפשר לזהות גם מתוצאות חלקיות (יותר “חי”)
        handleResults(partialResults, partial = true)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // ─────────────────────
    //  עיבוד תוצאות
    // ─────────────────────

    private fun handleResults(bundle: Bundle?, partial: Boolean = false) {
        if (bundle == null) return

        val matches = bundle.getStringArrayList(
            RecognizerIntent.EXTRA_RESULTS
        ) ?: return

        val normalizedList = matches
            .mapNotNull { it?.trim()?.lowercase(Locale("he", "IL")) }
            .filter { it.isNotEmpty() }

        if (normalizedList.isEmpty()) return

        // לדוגמה: "יובל שומע", "יובל, שומע", "יובל שומע אותי" וכו'
        val found = normalizedList.any { text ->
            text.contains(WAKE_PHRASE) ||
                    text.contains("יובל, שומע") ||
                    text.startsWith("יובל שומע") ||
                    text.startsWith("יובל, שומע")
        }

        if (found) {
            Log.d(TAG, "Wake phrase detected! ($normalizedList)")
            // כדיי לא לירות פעמיים:
            if (isActive) {
                // עוצרים רגע את ההאזנה כדי שלא תתנגש עם ה-STT של העוזר עצמו
                stop()
                onWakeCallback?.invoke()
            }
        } else {
            Log.d(TAG, "Speech (no wake): $normalizedList")
        }
    }
}
