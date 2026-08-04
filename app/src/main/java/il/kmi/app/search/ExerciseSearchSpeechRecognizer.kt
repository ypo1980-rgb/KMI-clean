package il.kmi.app.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * מצב זיהוי הדיבור של החיפוש הגלובלי.
 *
 * האובייקט שומר Application Context בלבד ומנקה את
 * SpeechRecognizer כאשר ה-Composable יוצא מהמסך.
 */
class ExerciseSearchSpeechState internal constructor(
    context: Context
) {
    private val applicationContext =
        context.applicationContext

    private var recognizer: SpeechRecognizer? = null

    private var permissionRequester:
            (() -> Unit)? = null

    private var resultHandler:
                (String) -> Unit = {}

    private var errorHandler:
                (String) -> Unit = {}

    private var isEnglish: Boolean = false

    var isListening by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(
            applicationContext
        )

    internal fun update(
        isEnglish: Boolean,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        requestPermission: () -> Unit
    ) {
        this.isEnglish = isEnglish
        resultHandler = onResult
        errorHandler = onError
        permissionRequester = requestPermission
    }

    fun toggleListening() {
        if (isListening) {
            stopListening()
        } else {
            startWithPermissionCheck()
        }
    }

    fun startListening() {
        if (isListening) return

        startWithPermissionCheck()
    }

    fun stopListening() {
        isListening = false
        statusMessage = null

        runCatching {
            recognizer?.stopListening()
        }
    }

    internal fun onPermissionResult(
        granted: Boolean
    ) {
        if (granted) {
            startRecognizer()
        } else {
            isListening = false

            val message =
                if (isEnglish) {
                    "Microphone permission is required for voice search."
                } else {
                    "נדרשת הרשאת מיקרופון כדי להשתמש בחיפוש הקולי."
                }

            statusMessage = message
            errorHandler(message)
        }
    }

    internal fun release() {
        isListening = false
        statusMessage = null

        runCatching {
            recognizer?.cancel()
        }

        runCatching {
            recognizer?.destroy()
        }

        recognizer = null
    }

    private fun startWithPermissionCheck() {
        if (!isRecognitionAvailable) {
            val message =
                if (isEnglish) {
                    "Voice recognition is not available on this device."
                } else {
                    "זיהוי קולי אינו זמין במכשיר הזה."
                }

            statusMessage = message
            errorHandler(message)
            return
        }

        val hasPermission =
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startRecognizer()
        } else {
            permissionRequester?.invoke()
        }
    }

    private fun startRecognizer() {
        if (isListening) return

        val activeRecognizer =
            recognizer ?: createRecognizer().also {
                recognizer = it
            }

        if (activeRecognizer == null) {
            val message =
                if (isEnglish) {
                    "Voice search could not be started."
                } else {
                    "לא ניתן להפעיל כרגע את החיפוש הקולי."
                }

            statusMessage = message
            errorHandler(message)
            return
        }

        /*
         * מפעילים את מצב ההאזנה לפני startListening,
         * כדי שהאנימציה תופיע כבר בלחיצה הראשונה.
         */
        isListening = true
        statusMessage =
            if (isEnglish) {
                "Listening..."
            } else {
                "מקשיב..."
            }

        val languageTag =
            if (isEnglish) {
                Locale.US.toLanguageTag()
            } else {
                Locale("he", "IL").toLanguageTag()
            }

        val recognitionIntent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                languageTag
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                languageTag
            )
            putExtra(
                RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,
                false
            )
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
        }

        runCatching {
            activeRecognizer.startListening(
                recognitionIntent
            )
        }.onFailure {
            isListening = false

            val message =
                if (isEnglish) {
                    "Voice search could not be started."
                } else {
                    "לא ניתן להפעיל כרגע את החיפוש הקולי."
                }

            statusMessage = message
            errorHandler(message)
        }
    }

    private fun createRecognizer(): SpeechRecognizer? {
        return runCatching {
            SpeechRecognizer.createSpeechRecognizer(
                applicationContext
            ).apply {
                setRecognitionListener(
                    object : RecognitionListener {

                        override fun onReadyForSpeech(
                            params: Bundle?
                        ) {
                            isListening = true
                            statusMessage =
                                if (isEnglish) {
                                    "Listening..."
                                } else {
                                    "מקשיב..."
                                }
                        }

                        override fun onBeginningOfSpeech() {
                            isListening = true
                        }

                        override fun onRmsChanged(
                            rmsdB: Float
                        ) = Unit

                        override fun onBufferReceived(
                            buffer: ByteArray?
                        ) = Unit

                        override fun onEndOfSpeech() {
                            /*
                             * לא מכבים כאן את החיווי. המנוע עדיין
                             * מעבד את המלל עד onResults או onError.
                             */
                            statusMessage =
                                if (isEnglish) {
                                    "Recognizing..."
                                } else {
                                    "מזהה..."
                                }
                        }

                        override fun onError(
                            error: Int
                        ) {
                            isListening = false

                            if (
                                error ==
                                SpeechRecognizer.ERROR_CLIENT
                            ) {
                                statusMessage = null
                                return
                            }

                            val message = errorMessage(error)
                            statusMessage = message
                            errorHandler(message)
                        }

                        override fun onResults(
                            results: Bundle?
                        ) {
                            isListening = false
                            statusMessage = null

                            val alternatives = results
                                ?.getStringArrayList(
                                    SpeechRecognizer
                                        .RESULTS_RECOGNITION
                                )
                                .orEmpty()
                                .map { candidate ->
                                    GlobalExerciseSearchEngine
                                        .normalizeSpokenQuery(
                                            candidate
                                        )
                                }
                                .filter { candidate ->
                                    candidate.length >= 2
                                }
                                .distinct()

                            val bestResult =
                                alternatives.firstOrNull()

                            if (bestResult == null) {
                                val message =
                                    if (isEnglish) {
                                        "I couldn't recognize an exercise name. Please try again."
                                    } else {
                                        "לא הצלחתי לזהות שם תרגיל. נסה שוב."
                                    }

                                statusMessage = message
                                errorHandler(message)
                            } else {
                                resultHandler(bestResult)
                            }
                        }

                        override fun onPartialResults(
                            partialResults: Bundle?
                        ) = Unit

                        override fun onEvent(
                            eventType: Int,
                            params: Bundle?
                        ) = Unit
                    }
                )
            }
        }.getOrNull()
    }

    private fun errorMessage(
        error: Int
    ): String {
        return if (isEnglish) {
            when (error) {
                SpeechRecognizer.ERROR_AUDIO ->
                    "There was a microphone audio problem."

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "Microphone permission is required for voice search."

                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "A network problem interrupted voice recognition."

                SpeechRecognizer.ERROR_NO_MATCH ->
                    "I couldn't recognize an exercise name. Please try again."

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    "Voice recognition is busy. Please try again."

                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    "No speech was detected. Please try again."

                else ->
                    "Voice recognition stopped unexpectedly."
            }
        } else {
            when (error) {
                SpeechRecognizer.ERROR_AUDIO ->
                    "אירעה בעיה בקליטת השמע מהמיקרופון."

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "נדרשת הרשאת מיקרופון כדי להשתמש בחיפוש הקולי."

                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "בעיית רשת הפסיקה את זיהוי הדיבור."

                SpeechRecognizer.ERROR_NO_MATCH ->
                    "לא הצלחתי לזהות שם תרגיל. נסה שוב."

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    "מנוע הזיהוי הקולי עסוק. נסה שוב."

                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    "לא זוהה דיבור. נסה שוב."

                else ->
                    "זיהוי הדיבור הופסק באופן לא צפוי."
            }
        }
    }
}

/**
 * יוצר ומנהל את מצב המיקרופון של חלון החיפוש.
 */
@Composable
fun rememberExerciseSearchSpeechState(
    isEnglish: Boolean,
    onResult: (String) -> Unit,
    onError: (String) -> Unit = {}
): ExerciseSearchSpeechState {
    val context = LocalContext.current

    val currentResultHandler by
    rememberUpdatedState(onResult)

    val currentErrorHandler by
    rememberUpdatedState(onError)

    val state = remember {
        ExerciseSearchSpeechState(context)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) { granted ->
            state.onPermissionResult(granted)
        }

    state.update(
        isEnglish = isEnglish,
        onResult = { value ->
            currentResultHandler(value)
        },
        onError = { message ->
            currentErrorHandler(message)
        },
        requestPermission = {
            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    )

    DisposableEffect(state) {
        onDispose {
            state.release()
        }
    }

    return state
}