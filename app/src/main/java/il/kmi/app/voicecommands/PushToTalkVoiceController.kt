package il.kmi.app.voicecommands

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

enum class PushToTalkState {
    IDLE,
    STARTING,
    LISTENING,
    PROCESSING
}

class PushToTalkVoiceController(
    context: Context,
    private val isEnglish: () -> Boolean,
    private val onStateChanged: (PushToTalkState) -> Unit,
    private val onCommand: (
        command: VoiceAppCommand,
        spokenText: String
    ) -> Unit,
    private val onError: (String) -> Unit,
    private val onPartialTranscript: (String) -> Unit = {},
    private val currentScreenName: () -> String = { "unknown" }
) {
    private val appContext = context.applicationContext

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentState = PushToTalkState.IDLE
    private var destroyed = false

    private val silenceHandler =
        Handler(Looper.getMainLooper())

    private val silenceRunnable = Runnable {
        if (currentState == PushToTalkState.LISTENING) {
            stopListening()
        }
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(appContext)
    }

    fun startListening() {
        if (destroyed) return
        if (currentState != PushToTalkState.IDLE) return

        onPartialTranscript("")

        if (!isRecognitionAvailable()) {
            VoiceCommandDiagnosticsLogger.logFailure(
                context = appContext,
                source = "push_to_talk",
                reason = "recognition_unavailable",
                screenName = currentScreenName()
            )

            onError(
                if (isEnglish()) {
                    "Speech recognition is not available on this device"
                } else {
                    "זיהוי דיבור אינו זמין במכשיר הזה"
                }
            )
            return
        }

        ensureRecognizer()

        updateState(PushToTalkState.STARTING)

        val languageTag = if (isEnglish()) {
            "en-US"
        } else {
            "he-IL"
        }

        val intent = Intent(
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

            /*
             * מבקשים כמה תוצאות כדי שמנוע הפקודות יוכל
             * להשתמש גם בחלופה כאשר התמלול הראשון שגוי.
             */
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
            putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                appContext.packageName
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                700L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                400L
            )

            /*
             * לא מגבילים את משך ההקלטה המינימלי.
             * אחרת חלק מהמכשירים ממשיכים להאזין
             * גם לאחר שהמשתמש כבר סיים לדבר.
             */
        }

        runCatching {
            speechRecognizer?.startListening(intent)
        }.onFailure { throwable ->
            updateState(PushToTalkState.IDLE)

            VoiceCommandDiagnosticsLogger.logFailure(
                context = appContext,
                source = "push_to_talk",
                reason = "recognition_start_failed",
                spokenText = throwable.message,
                screenName = currentScreenName()
            )

            onError(
                if (isEnglish()) {
                    "Unable to start voice commands"
                } else {
                    "לא ניתן להפעיל כרגע את הפקודות הקוליות"
                }
            )
        }
    }

    fun stopListening() {
        if (destroyed) return

        runCatching {
            speechRecognizer?.stopListening()
        }

        if (currentState != PushToTalkState.IDLE) {
            updateState(PushToTalkState.PROCESSING)
        }
    }

    fun cancelListening() {
        if (destroyed) return

        runCatching {
            speechRecognizer?.cancel()
        }

        updateState(PushToTalkState.IDLE)
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true

        runCatching {
            speechRecognizer?.cancel()
        }
        runCatching {
            speechRecognizer?.destroy()
        }

        cancelSilenceTimer()
        speechRecognizer = null
        currentState = PushToTalkState.IDLE
    }

    private fun restartSilenceTimer() {
        silenceHandler.removeCallbacks(silenceRunnable)

        silenceHandler.postDelayed(
            silenceRunnable,
            1_500L
        )
    }

    private fun cancelSilenceTimer() {
        silenceHandler.removeCallbacks(silenceRunnable)
    }

    private fun ensureRecognizer() {
        if (speechRecognizer != null) return

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(
                appContext
            ).apply {
                setRecognitionListener(
                    createRecognitionListener()
                )
            }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                updateState(PushToTalkState.LISTENING)
            }

            override fun onBeginningOfSpeech() {
                updateState(PushToTalkState.LISTENING)
                cancelSilenceTimer()
            }

            override fun onEndOfSpeech() {
                updateState(PushToTalkState.PROCESSING)
            }

            override fun onResults(results: Bundle?) {
                cancelSilenceTimer()
                onPartialTranscript("")

                val alternatives = results
                    ?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                VoiceCommandDiagnosticsLogger.logTrace(
                    context = appContext,
                    stage = "recognition_results",
                    spokenText = alternatives.firstOrNull(),
                    alternatives = alternatives,
                    screenName = currentScreenName()
                )

                updateState(PushToTalkState.IDLE)

                if (alternatives.isEmpty()) {
                    VoiceCommandDiagnosticsLogger.logFailure(
                        context = appContext,
                        source = "push_to_talk",
                        reason = "empty_recognition_results",
                        screenName = currentScreenName()
                    )

                    showNoMatchError()
                    return
                }

                /*
                 * בוחרים את החלופה הראשונה שהמנוע הצליח
                 * להפוך לפקודה מוכרת.
                 */
                val resolved = alternatives
                    .asSequence()
                    .map { spoken ->
                        spoken to VoiceAppCommandParser.parse(spoken)
                    }
                    .firstOrNull { (_, command) ->
                        command !is VoiceAppCommand.Unknown
                    }

                if (resolved != null) {
                    VoiceCommandDiagnosticsLogger.logTrace(
                        context = appContext,
                        stage = "parser_resolved",
                        spokenText = resolved.first,
                        alternatives = alternatives,
                        resolvedCommand =
                            resolved.second::class.simpleName
                                ?: resolved.second.toString(),
                        screenName = currentScreenName()
                    )

                    onCommand(
                        resolved.second,
                        resolved.first
                    )
                } else {
                    VoiceCommandDiagnosticsLogger.logFailure(
                        context = appContext,
                        source = "push_to_talk",
                        reason = "command_not_understood",
                        spokenText = alternatives.first(),
                        alternatives = alternatives,
                        screenName = currentScreenName()
                    )

                    onCommand(
                        VoiceAppCommand.Unknown(
                            alternatives.first()
                        ),
                        alternatives.first()
                    )
                }
            }

            override fun onError(error: Int) {
                updateState(PushToTalkState.IDLE)

                /*
                 * ERROR_CLIENT מתקבל לעיתים לאחר cancel()
                 * יזום ולכן אין צורך להציג אותו למשתמש.
                 */
                if (error == SpeechRecognizer.ERROR_CLIENT) {
                    return
                }

                VoiceCommandDiagnosticsLogger.logFailure(
                    context = appContext,
                    source = "push_to_talk",
                    reason = "recognition_error",
                    errorCode = error,
                    screenName = currentScreenName()
                )

                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO ->
                        bilingual(
                            "יש בעיית שמע במיקרופון",
                            "There is an audio problem with the microphone"
                        )

                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        bilingual(
                            "חסרה הרשאת מיקרופון",
                            "Microphone permission is missing"
                        )

                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        bilingual(
                            "לא ניתן להתחבר לשירות זיהוי הדיבור",
                            "Unable to connect to speech recognition"
                        )

                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        bilingual(
                            "מנוע זיהוי הדיבור עסוק. נסה שוב",
                            "Speech recognition is busy. Please try again"
                        )

                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        bilingual(
                            "לא הצלחתי לזהות את הפקודה",
                            "The voice command was not recognized"
                        )

                    else ->
                        bilingual(
                            "זיהוי הפקודה נכשל. נסה שוב",
                            "Voice command recognition failed. Please try again"
                        )
                }

                onError(message)
            }

            override fun onPartialResults(
                partialResults: Bundle?
            ) {

                val partial = partialResults
                    ?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (partial.isNotBlank()) {
                    restartSilenceTimer()
                    onPartialTranscript(partial)
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (
                    currentState == PushToTalkState.LISTENING &&
                    rmsdB > 1.5f
                ) {
                    restartSilenceTimer()
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEvent(
                eventType: Int,
                params: Bundle?
            ) = Unit
        }
    }

    private fun showNoMatchError() {
        onError(
            bilingual(
                "לא הצלחתי לזהות את הפקודה",
                "The voice command was not recognized"
            )
        )
    }

    private fun bilingual(
        hebrew: String,
        english: String
    ): String {
        return if (isEnglish()) english else hebrew
    }

    private fun updateState(
        newState: PushToTalkState
    ) {
        currentState = newState
        onStateChanged(newState)
    }
}