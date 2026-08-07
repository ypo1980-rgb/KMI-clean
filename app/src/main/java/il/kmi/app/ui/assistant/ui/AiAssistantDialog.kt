package il.kmi.app.ui.assistant.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import il.kmi.app.ui.DrawerBridge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.shared.domain.Belt
import il.kmi.app.KmiViewModel
import il.kmi.app.R
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.StyledExplanationText
import il.kmi.app.ui.scaledIconSize
import il.kmi.app.ui.assistant.core.AssistantKnowledgeSource
import il.kmi.app.ui.assistant.core.AssistantMatchQuality
import il.kmi.app.ui.assistant.core.AssistantMemory
import il.kmi.app.ui.assistant.core.AssistantOrchestrator
import il.kmi.app.ui.assistant.core.AssistantResult
import il.kmi.app.ui.assistant.core.AssistantResultItem
import il.kmi.app.ui.assistant.core.RemoteAssistantEngine
import il.kmi.app.ui.assistant.core.RemoteAssistantMessage
import il.kmi.app.ui.assistant.core.RemoteAssistantResult
import il.kmi.app.ui.assistant.core.matchQuality
import il.kmi.app.ui.assistant.core.primaryText
import il.kmi.app.ui.assistant.exercise.ExerciseAssistantEngine
import il.kmi.app.ui.assistant.trainings.TrainingsAssistantEngine
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


//======================================================

private lateinit var assistantMemory: AssistantMemory

private enum class Feedback {
    NONE, LIKE, UNLIKE
}

internal enum class AssistantMode {
    EXERCISE,   // מידע / הסבר על תרגיל
    TRAININGS,  // אימונים קרובים / לוח אימונים
    KMI_MATERIAL // חומר ק.מ.י (חיפוש בחומר)
}

private data class AiMessage(
    val fromUser: Boolean,
    val text: String,
    val answerTitle: String? = null,
    val relatedQuestion: String? = null,

    /*
     * משמשים לבניית הקדמה קצרה להקראה,
     * בלי להקריא את כל הסבר התרגיל.
     */
    val exerciseName: String? = null,
    val isExerciseExplanation: Boolean = false,

    val feedback: Feedback = Feedback.NONE,
    val trainingItems:
    List<AssistantResultItem> = emptyList(),
    val materialItems:
    List<AssistantResultItem> = emptyList()
)

private data class SpeechAlternative(
    val text: String,
    val confidence: Float,
    val relevanceScore: Float
)

private data class AssistantSuggestion(
    val label: String,
    val query: String
)

private enum class AssistantResultQuality {
    EXACT,
    RELEVANT,
    NEEDS_CLARIFICATION,
    ERROR
}

private enum class AssistantLogStatus {
    SUCCESS,
    NOT_RECOGNIZED,
    ALTERNATIVES_SHOWN,
    SUGGESTION_SELECTED,
    NOT_EXECUTED,
    PROCESSING_ERROR
}

// ───────────────────────────────
// קומפוזיבל: דיאלוג העוזר החכם
// ───────────────────────────────
@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun AiAssistantDialog(
    onDismiss: () -> Unit,
    onVoiceCommand: ((VoiceNavCommand) -> Unit)? = null,
    onOpenDrawer: (() -> Unit)? = null,
    currentLang: String = "",
    vm: KmiViewModel? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    val languageManager = remember { AppLanguageManager(ctx) }
    val resolvedLang = remember(currentLang) {
        currentLang.takeIf { it.isNotBlank() } ?: languageManager.getCurrentLanguage().code
    }
    val isEnglish = resolvedLang.equals("en", ignoreCase = true)

    val textAlignPrimary = if (isEnglish) TextAlign.Left else TextAlign.Right

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val graniteBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.background
        )
    )

    val premiumCardBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF4F46E5),
            Color(0xFF7C3AED),
            Color(0xFF8B5CF6),
            Color(0xFF2563EB)
        )
    )

    // ✅ Focus Sink: גורם ל-TextField לאבד פוקוס באמת בתוך AlertDialog
    val focusSinkRequester = remember { FocusRequester() }
    var focusSinkTick by remember { mutableStateOf(0) }

    LaunchedEffect(focusSinkTick) {
        if (focusSinkTick > 0) {
            runCatching { focusSinkRequester.requestFocus() }
        }
    }

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hideKeyboardHard() {
        try {
            // 0) להעביר פוקוס ל"בור" כדי שה-TextField יאבד פוקוס באמת
            focusSinkTick++

            // 1) Compose focus
            focusManager.clearFocus(force = true)
            keyboardController?.hide()

            // 2) View focus (חשוב במיוחד בתוך AlertDialog)
            view.clearFocus()

            // 3) Android IME (Hard close) - עדיף rootView token
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.rootView.windowToken, 0)

            // 4) עוד hide קטן (יש מכשירים שצריכים פעמיים)
            keyboardController?.hide()
        } catch (_: Throwable) {
        }
    }

// ✅ FIX: אתחול בטוח של assistantMemory כדי שלא יקרוס ב-sendQuestion
    val spAssistantMemory = remember {
        ctx.getSharedPreferences("kmi_assistant_memory", Context.MODE_PRIVATE)
    }

// ✅ פרטי המשתמש האמיתיים: סניף / קבוצה / אזור / תפקיד
// חשוב למצב "מידע על אימונים", כדי שלא ייפול לסניף ברירת מחדל כמו כפר סבא.
    val spUser = remember {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    /*
     * חגורת המשתמש לצורך שאלות כלליות על חומר ק.מ.י.
     * אין שינוי בפרופיל ואין כתיבה ל־SharedPreferences.
     */
    val registeredBeltText = remember(spUser) {
        listOf(
            "user_belt",
            "belt",
            "belt_id",
            "belt_name",
            "selected_belt",
            "current_belt",
            "training_belt"
        )
            .firstNotNullOfOrNull { key ->
                spUser.getString(key, null)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
    }

    val assistantMemoryLocal = remember(spAssistantMemory) {
        AssistantMemory(spAssistantMemory)
    }

    val assistantOrchestrator = remember {
        AssistantOrchestrator()
    }

    LaunchedEffect(
        assistantMemoryLocal,
        spAssistantMemory,
        spUser,
        ctx
    ) {
        assistantMemory = assistantMemoryLocal

        /*
         * מנוע האימונים מקבל:
         * - applicationContext עבור TrainingStatusEngine.
         * - SharedPreferences של המשתמש עבור הסניף והקבוצה.
         */
        TrainingsAssistantEngine.init(
            context = ctx.applicationContext,
            sp = spUser
        )
    }

    // ✅ מצב עוזר נבחר + שמירה לבחירה האחרונה
    val assistantModePrefs = remember {
        ctx.getSharedPreferences("kmi_ai_mode", Context.MODE_PRIVATE)
    }
    var assistantMode by remember {
        mutableStateOf<AssistantMode?>(null) // ✅ בכל פתיחה: אין בחירה מסומנת
    }

    val effectiveMode = assistantMode ?: AssistantMode.EXERCISE

    val emptyStateText = remember(assistantMode, isEnglish) {
        when (effectiveMode) {
            AssistantMode.EXERCISE -> {
                tr(
                    he =
                        "אני כאן כדי לעזור לך.\n" +
                                "אפשר לבקש הסבר לתרגיל ספציפי (למשל: \"תן הסבר לבעיטת מגל\").\n" +
                                "אפשר גם לבקש רשימת תרגילים לפי חגורה/נושא.",
                    en =
                        "I'm here to help you.\n" +
                                "You can ask for an explanation of a specific exercise (for example: \"Explain roundhouse kick\").\n" +
                                "You can also ask for a list of exercises by belt or topic."
                )
            }

            AssistantMode.TRAININGS -> {
                tr(
                    he =
                        "אני כאן כדי לעזור לך עם אימונים.\n" +
                                "אפשר לשאול:\n" +
                                "• \"מה האימון הקרוב שלי?\"\n" +
                                "• \"מתי האימון הבא?\"\n" +
                                "• \"תראה לי את האימונים הקרובים\"\n" +
                                "• \"באיזה יום יש לי אימון?\"",
                    en =
                        "I'm here to help you with trainings.\n" +
                                "You can ask:\n" +
                                "• \"What is my next training?\"\n" +
                                "• \"When is the next training?\"\n" +
                                "• \"Show me upcoming trainings\"\n" +
                                "• \"Which day do I have training?\""
                )
            }

            AssistantMode.KMI_MATERIAL -> {
                tr(
                    he =
                        "מצב חומר ק.מ.י פעיל.\n" +
                                "אפשר לבקש:\n" +
                                "• נושא (למשל \"הגנות חיצוניות\")\n" +
                                "• תת־נושא\n" +
                                "• תרגיל / חיפוש לפי חגורה\n" +
                                "• רשימת תרגילים לפי חגורה/נושא",
                    en =
                        "KAMI material mode is active.\n" +
                                "You can ask for:\n" +
                                "• A topic (for example: \"Outside defenses\")\n" +
                                "• A sub-topic\n" +
                                "• An exercise / search by belt\n" +
                                "• A list of exercises by belt or topic"
                )
            }
        }
    }

    var input by remember {
        mutableStateOf("")
    }

    var messages by remember {
        mutableStateOf(
            listOf<AiMessage>()
        )
    }

    /*
     * כל ההודעות נשמרות במסך כדי לאפשר גלילה אחורה.
     * למנוע המרוחק נשמרת היסטוריה נפרדת ומוגבלת,
     * המיועדת להבנת שאלות המשך.
     */
    var remoteConversationHistory by remember {
        mutableStateOf(
            listOf<RemoteAssistantMessage>()
        )
    }

    var isThinking by remember {
        mutableStateOf(false)
    }

    var lastAiAnswer by remember {
        mutableStateOf<String?>(null)
    }
    var speechStatusMessage by remember { mutableStateOf<String?>(null) }

    // הצעות המשך שנוצרות בהתאם למצב ולתשובה האחרונה.
    var followUpSuggestions by remember {
        mutableStateOf<List<AssistantSuggestion>>(emptyList())
    }

    var resultQuality by remember {
        mutableStateOf<AssistantResultQuality?>(null)
    }

    // חלופות מוצגות רק כאשר מנוע הדיבור אינו בטוח מספיק.
    var speechAlternatives by remember {
        mutableStateOf<List<SpeechAlternative>>(emptyList())
    }

    var speechNeedsConfirmation by remember {
        mutableStateOf(false)
    }

    var speechCanRetry by remember {
        mutableStateOf(false)
    }

    // ✅ חדש: בקשה לשליחה שמגיעה מה-STT (כדי לא לקרוא ל-sendQuestion מתוך onResults)
    var pendingSendFromStt by remember { mutableStateOf<String?>(null) }

    // ✅ חדש: דגל לסגירת מקלדת בצורה יציבה (אחרי רינדור)
    var requestHideKeyboard by remember { mutableStateOf(false) }

    // ✅ סוגר מקלדת + מנקה פוקוס "אחרי רינדור" (אמין בתוך AlertDialog)
    LaunchedEffect(requestHideKeyboard) {
        if (requestHideKeyboard) {
            hideKeyboardHard()
            requestHideKeyboard = false
        }
    }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    /*
     * גובה אזור השיחה משמש ליצירת שטח גלילה
     * מתחת להודעה האחרונה.
     *
     * בלי השטח הזה אי אפשר להציב שאלה חדשה
     * בראש המסך לפני שהתשובה הארוכה התקבלה.
     */
    var conversationViewportHeightPx by remember {
        mutableStateOf(0)
    }

    /*
     * המיקום בתוך תוכן השיחה שבו מתחילה
     * השאלה הנוכחית.
     */
    var latestQuestionScrollOffset by remember {
        mutableStateOf<Int?>(null)
    }

    /*
     * מספר שאלות המשתמש שעבורן כבר בוצעה
     * גלילה אוטומטית.
     *
     * כך כל שאלה חדשה נגללת פעם אחת בלבד,
     * ולא מתבצעת התערבות בגלילה ידנית.
     */
    var autoScrolledUserQuestionCount by remember {
        mutableStateOf(0)
    }

    val userQuestionCount =
        messages.count { message ->
            message.fromUser
        }

    val latestUserMessage = messages.lastOrNull { it.fromUser }
    val latestAssistantMessage = messages.lastOrNull { !it.fromUser }

    /*
     * האפקט נמצא ברמת המסך ולא בתוך ה־Column המתחלף.
     * לכן הוא אינו מתבטל במעבר מתצוגת הפרימיום
     * לתצוגת השיחה לאחר השאלה השנייה.
     */
    LaunchedEffect(
        userQuestionCount,
        latestQuestionScrollOffset
    ) {
        if (
            userQuestionCount <= 1 ||
            userQuestionCount <=
            autoScrolledUserQuestionCount
        ) {
            return@LaunchedEffect
        }

        val questionOffset =
            latestQuestionScrollOffset
                ?: return@LaunchedEffect

        /*
         * ממתינים לסיום המעבר מתצוגת השאלה הראשונה
         * לתצוגת השיחה המלאה ולמדידת ה־Spacer.
         */
        delay(180L)

        autoScrolledUserQuestionCount =
            userQuestionCount

        scrollState.scrollTo(
            value =
                questionOffset.coerceIn(
                    minimumValue = 0,
                    maximumValue =
                        scrollState.maxValue
                )
        )
    }

    val showExerciseAnswerLayout =
        assistantMode == AssistantMode.EXERCISE &&
                (latestUserMessage != null || latestAssistantMessage != null || isThinking)

    val showMaterialAnswerLayout =
        assistantMode == AssistantMode.KMI_MATERIAL &&
                (latestUserMessage != null || latestAssistantMessage != null || isThinking)

    /*
     * בתשובה הראשונה נשמר כרטיס הפרימיום הגדול.
     * כאשר השיחה מתארכת עוברים לרשימת בועות נגללת,
     * שבה ניתן לראות את כל השאלות והתשובות.
     */
    val showPremiumAnswerLayout =
        (
                showExerciseAnswerLayout ||
                        showMaterialAnswerLayout
                ) &&
                messages.size <= 2

    val explanationScrollState = rememberScrollState()

    val rawExerciseQuestion = latestUserMessage?.text?.trim().orEmpty()

    val cleanedExerciseName = remember(rawExerciseQuestion) {
        extractExerciseNameFromQuestion(rawExerciseQuestion)
            ?.replace("\"", "")
            ?.replace("'", "")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
    }

    // הכרטיס העליון: כל מה שהמשתמש אמר
    val displayTopRequestText = rawExerciseQuestion

    // הכרטיס התחתון / שימוש פנימי: שם תרגיל נקי בלבד
    val displayExerciseName = cleanedExerciseName.ifBlank { rawExerciseQuestion }

    var pendingNavAfterSpeak by remember { mutableStateOf<VoiceNavCommand?>(null) }

    // ✅ חובה: אתחול בטוח של ה-TTS Manager גם מתוך הדיאלוג (idempotent)
    // זה מונע מצב שבו לא קראו init() מוקדם מספיק / Activity אחר / תזמון.
    LaunchedEffect(Unit) {
        runCatching { KmiTtsManager.init(ctx.applicationContext) }
    }

    // ✅ =========================================================
    // ✅ STT/TTS — TTS רק דרך KmiTtsManager (אין MediaPlayer/HTTP/Cloud כאן)
    // ✅ =========================================================

    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var pendingStartStt by remember { mutableStateOf(false) }
    var currentListeningSessionId by remember { mutableStateOf(0L) }

    /*
     * המיקרופון מופעל רק בלחיצה מפורשת של המשתמש.
     * לאחר סיום הדיבור או הקראת התשובה הוא נשאר כבוי.
     */
    val autoVoiceConversation = false

    val dynamicInputPlaceholder = remember(assistantMode, isEnglish, isListening, isSpeaking) {
        when {
            isSpeaking -> tr("לחץ כדי לעצור את הדיבור", "Tap to stop speaking")
            isListening -> tr("אני מקשיב...", "I'm listening...")
            assistantMode == null -> tr("אנא בחר נושא להמשך", "Please choose a topic to continue")
            assistantMode == AssistantMode.EXERCISE -> tr(
                "אמור שם תרגיל",
                "Type or say an exercise name"
            )

            assistantMode == AssistantMode.TRAININGS -> tr(
                "שאל משהו על אימונים",
                "Ask or say something about trainings"
            )

            else -> tr("חפש או אמור נושא / תרגיל", "Search or say a topic / exercise")
        }
    }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingSendFromStt = null
            pendingStartStt = true
        } else {
            Toast.makeText(
                ctx,
                tr(
                    "צריך הרשאת מיקרופון כדי להשתמש בדיבור",
                    "Microphone permission is required to use voice input"
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // UI בלבד (אנימציית "מדבר…")
    var speakJob by remember { mutableStateOf<Job?>(null) }

    fun stopSpeaking() {
        runCatching { KmiTtsManager.setOnSpeechCompletedListener(null) }
        runCatching { KmiTtsManager.stop() }

        speakJob?.cancel()
        speakJob = null
        isSpeaking = false
        // לא מכבים כאן isListening — זה STT
    }

    fun speakBest(text: String) {
        val ttsText = normalizeForTts(text).trim()
        if (ttsText.isBlank()) return

        stopSpeaking()
        isSpeaking = true

        speakJob?.cancel()
        KmiTtsManager.setOnSpeechCompletedListener {
            scope.launch {
                speakJob?.cancel()
                speakJob = null
                isSpeaking = false

                pendingNavAfterSpeak?.let { cmd ->
                    pendingNavAfterSpeak = null
                    onVoiceCommand?.invoke(cmd)
                    return@launch
                }

                // שיחה קולית רציפה: אחרי שהעוזר סיים לדבר חוזרים להאזין
                if (autoVoiceConversation && assistantMode != null && !isListening) {
                    delay(100L)
                    pendingStartStt = true
                }
            }
        }

        speakJob = scope.launch {
            // fallback הגנתי בלבד.
            // ברשימות חומר ק.מ.י הטקסט ארוך, לכן אסור לכבות isSpeaking אחרי 16 שניות.
            // אם נכבה מוקדם מדי — האייקון יחזור למיקרופון למרות שה-TTS עדיין מדבר.
            val fallbackMs = when {
                ttsText.length <= 80 -> 10_000L
                ttsText.length <= 180 -> 18_000L
                ttsText.length <= 350 -> 30_000L
                ttsText.length <= 700 -> 55_000L
                else -> 90_000L
            }

            delay(fallbackMs)

            if (isSpeaking) {
                isSpeaking = false
                // בכוונה לא נוגעים ב-TTS ולא מפעילים STT מכאן
            }
        }

        runCatching { KmiTtsManager.speak(ttsText) }
            .onFailure {
                speakJob?.cancel()
                isSpeaking = false
                KmiTtsManager.setOnSpeechCompletedListener(null)
            }
    }

    val speak: (String) -> Unit = { text -> speakBest(text) }

    // STT (SpeechRecognizer)
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(ctx)) {
            SpeechRecognizer.createSpeechRecognizer(ctx)
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        // no-op, keeps composition key stable
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            runCatching { speechRecognizer?.cancel() }
            runCatching { speechRecognizer?.destroy() }
        }
    }

    fun stopListeningHard() {
        /*
         * מבטלים גם בקשת הפעלה שעדיין ממתינה, למשל לאחר
         * קבלת הרשאת מיקרופון או לאחר סיום הקראת תשובה.
         */
        pendingStartStt = false
        isListening = false

        /*
         * שינוי המזהה מבטל גם את משימת פסק הזמן
         * של סשן ההאזנה הקודם.
         */
        currentListeningSessionId += 1L

        runCatching { speechRecognizer?.stopListening() }
        runCatching { speechRecognizer?.cancel() }

        hideKeyboardHard()
    }

    fun saveAssistantCommandLog(
        rawCommand: String,
        status: AssistantLogStatus,
        alternatives: List<String> = emptyList(),
        answer: String? = null,
        errorCode: Int? = null
    ) {
        val command = rawCommand.trim()

        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

            val data = hashMapOf<String, Any?>(
                "rawCommand" to command,
                "normalizedCommand" to normalizeExerciseQuery(command),
                "status" to status.name,
                "assistantMode" to (assistantMode?.name ?: "UNKNOWN"),
                "language" to resolvedLang,
                "alternatives" to alternatives.take(5),
                "answer" to answer.orEmpty().take(2_000),
                "errorCode" to errorCode,
                "source" to "android_ai_assistant",
                "userUid" to uid,
                "createdAt" to Timestamp.now(),
                "createdAtMillis" to System.currentTimeMillis()
            )

            Firebase.firestore
                .collection("assistantCommandLogs")
                .add(data)
        } catch (_: Throwable) {
            // כשל בשמירת לוג אינו עוצר את פעולת העוזר.
        }
    }

    fun startSpeechToTextInternal() {

        if (isListening) return

        speechAlternatives = emptyList()
        speechNeedsConfirmation = false
        speechCanRetry = false

        stopSpeaking()
        hideKeyboardHard()

        if (!hasRecordAudioPermission()) {
            Toast.makeText(
                ctx,
                tr(
                    "אין הרשאת מיקרופון. אשר גישה ונסה שוב",
                    "No microphone permission. Please allow access and try again"
                ),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(ctx) || speechRecognizer == null) {
            Toast.makeText(
                ctx,
                tr(
                    "זיהוי דיבור לא זמין במכשיר הזה",
                    "Speech recognition is not available on this device"
                ),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        currentListeningSessionId = System.currentTimeMillis()
        isListening = true
        speechStatusMessage = null

        fun speechCandidateScore(
            candidate: String,
            confidence: Float
        ): Float {
            val text =
                normalizeRecognizedExerciseSpeech(
                    candidate
                )

            val normalized =
                text.lowercase()

            var score = when {
                confidence in 0f..1f -> confidence * 100f
                else -> 0f
            }

            // תוצאה מלאה וברורה עדיפה על מילה בודדת.
            score += minOf(text.length, 60) * 0.15f

            /*
             * כאשר המשתמש מזכיר חגורה, משפט שמכיל גם
             * צבע חוקי עדיף משמעותית על משפט שנקטע
             * לאחר המילה "חגורה".
             */
            val mentionsBelt =
                listOf(
                    "חגורה",
                    "belt"
                ).any { marker ->
                    marker in normalized
                }

            val detectedCandidateBelt =
                detectBeltEnum(text)

            when {
                mentionsBelt &&
                        detectedCandidateBelt != null ->
                    score += 110f

                mentionsBelt &&
                        detectedCandidateBelt == null ->
                    score -= 140f
            }

            val endsWithIncompleteBelt =
                normalized.endsWith("חגורה") ||
                        normalized.endsWith("belt")

            if (endsWithIncompleteBelt) {
                score -= 180f
            }

            when (assistantMode) {
                AssistantMode.EXERCISE -> {
                    if (
                        hasVerifiedExerciseMatch(
                            rawQuestion = text
                        )
                    ) {
                        score += 90f
                    }

                    val exerciseWords = listOf(
                        "בעיטה",
                        "הגנה",
                        "אגרוף",
                        "חניקה",
                        "דקירה",
                        "תרגיל",
                        "kick",
                        "defense",
                        "punch",
                        "exercise"
                    )

                    if (exerciseWords.any { it in normalized }) {
                        score += 35f
                    }
                }

                AssistantMode.TRAININGS -> {
                    val trainingWords = listOf(
                        "אימון",
                        "אימונים",
                        "קבוצה",
                        "סניף",
                        "מאמן",
                        "שעה",
                        "training",
                        "trainings",
                        "branch",
                        "coach",
                        "schedule"
                    )

                    if (trainingWords.any { it in normalized }) {
                        score += 60f
                    }
                }

                AssistantMode.KMI_MATERIAL -> {
                    val materialWords = listOf(
                        "חגורה",
                        "נושא",
                        "תרגילים",
                        "חומר",
                        "קמי",
                        "belt",
                        "topic",
                        "exercises",
                        "material",
                        "kami"
                    )

                    if (materialWords.any { it in normalized }) {
                        score += 60f
                    }
                }

                null -> {
                    if (parseVoiceNavCommand(text) != null) {
                        score += 70f
                    }
                }
            }

            return score
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (isEnglish) "en-US" else "he-IL"
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                if (isEnglish) "en-US" else "he-IL"
            )
            // מבקשים מספר חלופות ולא מסתמכים רק על התוצאה הראשונה.
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

// מאפשר משפט טבעי בלי לסיים את ההאזנה מוקדם מדי.
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                2200L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                1400L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1200L
            )
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                /*
                 * זהו האישור הרשמי של Android שמנוע הדיבור
                 * מוכן ומאזין. מפעילים כאן שוב את מצב האנימציה
                 * כדי שגם סשן ההאזנה הראשון יוצג מיד.
                 */
                isListening = true
                speechStatusMessage = null
            }

            override fun onBeginningOfSpeech() {
                /*
                 * שומרים את האנימציה פעילה גם מהרגע שבו
                 * המנוע זיהה שהמשתמש התחיל לדבר בפועל.
                 */
                isListening = true
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onPartialResults(partialResults: Bundle) {
                val partial = partialResults
                    .getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    ?.firstOrNull()
                    ?.trim()

                if (!partial.isNullOrBlank()) {
                    /*
                     * תוצאה חלקית מעידה שסשן ההאזנה עדיין פעיל.
                     */
                    isListening = true
                    input = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onEndOfSpeech() {
                /*
                 * אין לכבות כאן את isListening ואין לקרוא שוב
                 * ל-stopListening().
                 *
                 * Android כבר סיים לקבל את הקול וכעת מעבד אותו.
                 * מצב ההאזנה והאנימציה ייסגרו רק ב-onResults()
                 * או ב-onError(), כדי למנוע הבהוב או היעלמות
                 * של האנימציה בסשן הראשון.
                 */
            }

            override fun onError(error: Int) {
                pendingStartStt = false
                isListening = false
                currentListeningSessionId += 1L

                /*
                 * לאחר שגיאה אין סיבה להשאיר סשן הקלט פעיל.
                 * ERROR_CLIENT יכול להיווצר בעצמו מפעולת ביטול,
                 * ולכן לא מבטלים פעם נוספת במקרה הזה.
                 */
                if (error != SpeechRecognizer.ERROR_CLIENT) {
                    runCatching { speechRecognizer?.cancel() }
                }

                speechNeedsConfirmation = false
                speechAlternatives = emptyList()

                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO ->
                        tr(
                            "לא הצלחתי לשמוע היטב. בדוק שהמיקרופון אינו חסום ונסה שוב.",
                            "I couldn't hear clearly. Check that the microphone is not blocked and try again."
                        )

                    SpeechRecognizer.ERROR_CLIENT ->
                        ""

                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        tr(
                            "אין הרשאת מיקרופון. אפשר לאשר אותה בהגדרות האפליקציה.",
                            "Microphone permission is missing. You can enable it in the app settings."
                        )

                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        tr(
                            "החיבור לזיהוי הדיבור נכשל. בדוק את החיבור לאינטרנט ונסה שוב.",
                            "Speech recognition could not connect. Check your internet connection and try again."
                        )

                    SpeechRecognizer.ERROR_NO_MATCH ->
                        tr(
                            "לא הצלחתי להבין את הבקשה. נסה לומר שם תרגיל או נושא בצורה קצרה.",
                            "I couldn't understand the request. Try saying a short exercise or topic name."
                        )

                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        tr(
                            "לא שמעתי דיבור. לחץ על המיקרופון ונסה שוב.",
                            "I didn't hear any speech. Tap the microphone and try again."
                        )

                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        tr(
                            "מנוע הדיבור עדיין עסוק. המתן רגע ונסה שוב.",
                            "The speech recognizer is still busy. Wait a moment and try again."
                        )

                    SpeechRecognizer.ERROR_SERVER ->
                        tr(
                            "שירות זיהוי הדיבור אינו זמין כרגע. אפשר לכתוב את הבקשה בשדה למטה.",
                            "Speech recognition is currently unavailable. You can type your request below."
                        )

                    else ->
                        tr(
                            "זיהוי הדיבור נכשל. אפשר לנסות שוב או לכתוב את הבקשה.",
                            "Speech recognition failed. Try again or type your request."
                        )
                }

                speechCanRetry =
                    error != SpeechRecognizer.ERROR_CLIENT &&
                            error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS

                speechStatusMessage = message.takeIf { it.isNotBlank() }

                if (error != SpeechRecognizer.ERROR_CLIENT) {
                    saveAssistantCommandLog(
                        rawCommand = input,
                        status = AssistantLogStatus.NOT_RECOGNIZED,
                        errorCode = error
                    )
                }
            }

            override fun onResults(results: Bundle) {
                /*
                 * התוצאות התקבלו ולכן ניתן לסגור לחלוטין
                 * את סשן ההאזנה בלי לסכן את המלל שזוהה.
                 */
                pendingStartStt = false
                isListening = false
                currentListeningSessionId += 1L
                runCatching { speechRecognizer?.cancel() }

                speechStatusMessage = null
                speechCanRetry = false

                val rawAlternatives = results
                    .getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.length > 1 }
                    .distinctBy { normalizeExerciseQuery(it) }

                if (rawAlternatives.isEmpty()) {
                    speechNeedsConfirmation = false
                    speechAlternatives = emptyList()
                    speechCanRetry = true

                    speechStatusMessage = tr(
                        "לא הצלחתי לזהות בקשה ברורה. נסה לומר רק את שם התרגיל או הנושא.",
                        "I couldn't recognize a clear request. Try saying only the exercise or topic name."
                    )

                    saveAssistantCommandLog(
                        rawCommand = input,
                        status = AssistantLogStatus.NOT_RECOGNIZED
                    )
                    return
                }

                val confidenceScores = results.getFloatArray(
                    SpeechRecognizer.CONFIDENCE_SCORES
                )

                val rankedAlternatives = rawAlternatives
                    .mapIndexed { index, candidate ->
                        val confidence =
                            confidenceScores?.getOrNull(index) ?: -1f

                        SpeechAlternative(
                            text = candidate,
                            confidence = confidence,
                            relevanceScore = speechCandidateScore(
                                candidate = candidate,
                                confidence = confidence
                            )
                        )
                    }
                    .sortedByDescending { it.relevanceScore }

                val best = rankedAlternatives.first()
                val second = rankedAlternatives.getOrNull(1)

                val bestConfidenceIsLow =
                    best.confidence in 0f..0.54f

                val resultsAreClose =
                    second != null &&
                            best.relevanceScore -
                            second.relevanceScore < 18f

                val normalizedBestText =
                    best.text
                        .lowercase()
                        .replace(
                            Regex("\\s+"),
                            " "
                        )
                        .trim()

                val bestMentionsBelt =
                    "חגורה" in normalizedBestText ||
                            "belt" in normalizedBestText

                val bestDetectedBelt =
                    detectBeltEnum(best.text)

                /*
                 * משפט שמזכיר חגורה אך אינו כולל צבע
                 * הוא תוצאה חלקית ואסור לשלוח אותו.
                 */
                if (
                    bestMentionsBelt &&
                    bestDetectedBelt == null
                ) {
                    speechAlternatives =
                        rankedAlternatives
                            .filter { alternative ->
                                detectBeltEnum(
                                    alternative.text
                                ) != null
                            }
                            .take(3)

                    speechNeedsConfirmation =
                        speechAlternatives.isNotEmpty()

                    speechCanRetry =
                        speechAlternatives.isEmpty()

                    speechStatusMessage =
                        if (speechAlternatives.isNotEmpty()) {
                            tr(
                                "לא זיהיתי בוודאות את צבע החגורה. בחר את המשפט הנכון.",
                                "I could not recognize the belt color with confidence. Choose the correct sentence."
                            )
                        } else {
                            tr(
                                "שמעתי את המילה חגורה, אבל לא זיהיתי את הצבע. נסה לומר שוב את צבע החגורה.",
                                "I heard the word belt, but not its color. Please say the belt color again."
                            )
                        }

                    input = best.text.trim()

                    saveAssistantCommandLog(
                        rawCommand = best.text,
                        status =
                            AssistantLogStatus.NOT_RECOGNIZED,
                        alternatives =
                            speechAlternatives.map {
                                it.text
                            }
                    )

                    return
                }

                val bestHasRelevantMatch = when (assistantMode) {
                    AssistantMode.EXERCISE -> {
                        hasVerifiedExerciseMatch(
                            rawQuestion = best.text
                        )
                    }

                    AssistantMode.TRAININGS -> {
                        val normalized = best.text.lowercase()

                        listOf(
                            "אימון",
                            "אימונים",
                            "קבוצה",
                            "סניף",
                            "מאמן",
                            "training",
                            "workout",
                            "branch",
                            "coach"
                        ).any { it in normalized }
                    }

                    AssistantMode.KMI_MATERIAL -> {
                        val normalized = best.text.lowercase()

                        listOf(
                            "חגורה",
                            "נושא",
                            "תרגיל",
                            "חומר",
                            "קמי",
                            "belt",
                            "topic",
                            "exercise",
                            "material",
                            "kami"
                        ).any { it in normalized }
                    }

                    null ->
                        parseVoiceNavCommand(best.text) != null
                }

                /*
                 * בודקים אם חלופות הזיהוי מכילות צבעי
                 * חגורה שונים. במקרה כזה אי אפשר לדעת
                 * אוטומטית אם נאמר "כחולה" או "כתומה",
                 * ולכן מציגים למשתמש אפשרויות לבחירה.
                 */
                val alternativeBelts =
                    rankedAlternatives
                        .mapNotNull { alternative ->
                            detectBeltEnum(
                                alternative.text
                            )
                        }
                        .distinct()

                val beltRecognitionIsAmbiguous =
                    alternativeBelts.size > 1

                val shouldConfirmSpeech =
                    rankedAlternatives.size > 1 &&
                            (
                                    beltRecognitionIsAmbiguous ||
                                            bestConfidenceIsLow ||
                                            resultsAreClose
                                    )

                if (shouldConfirmSpeech) {
                    speechAlternatives =
                        rankedAlternatives
                            .take(3)

                    speechNeedsConfirmation = true
                    speechCanRetry = true

                    speechStatusMessage =
                        if (beltRecognitionIsAmbiguous) {
                            tr(
                                "זיהיתי יותר מצבע חגורה אחד. בחר את המשפט הנכון.",
                                "I recognized more than one belt color. Choose the correct sentence."
                            )
                        } else {
                            tr(
                                "לא הייתי בטוח מה נאמר. בחר את האפשרות הנכונה.",
                                "I was not certain what was said. Choose the correct option."
                            )
                        }

                    input = best.text.trim()

                    saveAssistantCommandLog(
                        rawCommand = best.text,
                        status =
                            AssistantLogStatus.ALTERNATIVES_SHOWN,
                        alternatives =
                            speechAlternatives.map {
                                it.text
                            }
                    )

                    return
                }

                /*
                 * קיימת חלופה ברורה ולכן אפשר לשלוח
                 * אותה אוטומטית.
                 */
                speechAlternatives = emptyList()
                speechNeedsConfirmation = false
                speechCanRetry = false
                speechStatusMessage = null

                val rawSpoken =
                    best.text.trim()

                val modeNormalizedSpoken =
                    if (
                        assistantMode ==
                        AssistantMode.EXERCISE
                    ) {
                        normalizeRecognizedExerciseSpeech(
                            rawSpoken
                        )
                    } else {
                        rawSpoken
                    }

                /*
                 * תיקון טעויות תמלול כלליות מתבצע לאחר
                 * התיקונים הייעודיים למצב הפעיל.
                 */
                val spoken =
                    normalizeRecognizedAssistantSpeech(
                        modeNormalizedSpoken
                    )

                if (spoken.isBlank()) {
                    speechCanRetry = true

                    speechStatusMessage = tr(
                        "לא הצלחתי לזהות בקשה ברורה. נסה שוב.",
                        "I couldn't recognize a clear request. Please try again."
                    )

                    return
                }

                val navCommand =
                    parseVoiceNavCommand(
                        spoken
                    )

                if (navCommand != null) {
                    pendingNavAfterSpeak = null
                    input = ""
                    onVoiceCommand?.invoke(
                        navCommand
                    )
                    return
                }

                /*
                 * מציגים ושולחים את הטקסט לאחר תיקון
                 * טעויות זיהוי הדיבור במונחים מקצועיים.
                 */
                input = spoken
                pendingSendFromStt = spoken
            }
        })

        runCatching {
            speechRecognizer?.startListening(intent)
        }.onFailure {
            pendingStartStt = false
            isListening = false
            currentListeningSessionId += 1L
            runCatching { speechRecognizer?.cancel() }

            Toast.makeText(
                ctx,
                tr(
                    "לא ניתן להפעיל את המיקרופון כרגע",
                    "Unable to start microphone right now"
                ),
                Toast.LENGTH_SHORT
            ).show()
        }

        val listeningSessionId = currentListeningSessionId

        mainHandler.postDelayed({
            if (
                isListening &&
                currentListeningSessionId == listeningSessionId
            ) {
                stopListeningHard()

                speechCanRetry = true
                speechStatusMessage = tr(
                    "ההאזנה הסתיימה אוטומטית. אפשר ללחוץ על המיקרופון ולנסות שוב.",
                    "Listening stopped automatically. Tap the microphone to try again."
                )
            }
        }, 12_000L)
    }

    LaunchedEffect(pendingStartStt) {
        if (pendingStartStt) {
            pendingStartStt = false
            startSpeechToTextInternal()
        }
    }

    fun scrollToTop() {
        scope.launch {
            scrollState.scrollTo(0)
        }
    }

    fun setFeedback(index: Int, fb: Feedback) {
        messages = messages.mapIndexed { i, m ->
            if (i == index) m.copy(feedback = fb) else m
        }
    }

    fun saveAiFeedback(question: String, answer: String?) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            val now = System.currentTimeMillis()
            val db = Firebase.firestore

            val cleanQuestion = question.trim()
            val cleanAnswer = answer.orEmpty().trim()

            val data = hashMapOf(
                "question" to cleanQuestion,
                "answer" to cleanAnswer,
                "like" to false,
                "feedbackType" to "unlike",
                "assistantMode" to (assistantMode?.name ?: "UNKNOWN"),
                "source" to "android_ai_assistant",
                "userUid" to uid,
                "createdAt" to Timestamp.now(),
                "createdAtMillis" to now
            )

            db.collection("aiFeedback").add(data)
            db.collection("assistantFeedback").add(
                data + mapOf(
                    "type" to "unlike",
                    "status" to "open"
                )
            )
        } catch (_: Throwable) {
            // Feedback failure should not interrupt the assistant flow.
        }
    }

    fun setAssistantMode(m: AssistantMode) {
        assistantMode = m
        assistantModePrefs.edit().putString("mode", m.name).apply()

        // reset שיחה
        messages = emptyList()
        remoteConversationHistory =
            emptyList()
        lastAiAnswer = null
        followUpSuggestions = emptyList()
        resultQuality = null
        isThinking = false
        pendingNavAfterSpeak = null
        pendingSendFromStt = null
        speechAlternatives = emptyList()
        speechNeedsConfirmation = false
        speechCanRetry = false
        speechStatusMessage = null
        input = ""

        assistantOrchestrator.reset()

        stopSpeaking()
        stopListeningHard()
        requestHideKeyboard = true
    }

    fun backToModePicker() {
        stopListeningHard()
        stopSpeaking()
        requestHideKeyboard = true
        assistantOrchestrator.reset()

        assistantMode = null
        messages = emptyList()
        remoteConversationHistory =
            emptyList()
        lastAiAnswer = null
        followUpSuggestions = emptyList()
        resultQuality = null
        isThinking = false
        pendingNavAfterSpeak = null
        pendingSendFromStt = null
        speechAlternatives = emptyList()
        speechNeedsConfirmation = false
        speechCanRetry = false
        speechStatusMessage = null
        input = ""
    }

    fun sendQuestion(q: String) {
        val question = q.trim()
        if (question.isBlank() || isThinking) return

        parseVoiceNavCommand(question)?.let { command ->
            stopListeningHard()
            stopSpeaking()
            pendingNavAfterSpeak = null
            onVoiceCommand?.invoke(command)
            return
        }

        stopListeningHard()
        requestHideKeyboard = true
        input = ""
        followUpSuggestions = emptyList()
        resultQuality = null
        speechStatusMessage = null
        isThinking = true

        /*
         * מצרפים את השאלה לשיחה הקיימת במקום למחוק
         * את הכרטיסים הקודמים.
         */
        /*
         * לפני הוספת השאלה מנקים את המיקום הקודם.
         * המיקום החדש יימדד לאחר שהשאלה תוצג במסך.
         */
        /*
         * מאפסים את המדידה הקודמת. לאחר שהשאלה
         * החדשה תוצג, onGloballyPositioned יעדכן
         * את המיקום והאפקט הקבוע יגלול אליו.
         */
        latestQuestionScrollOffset = null

        /*
         * שומרים את כל הודעות השיחה הנוכחית.
         * ההיסטוריה תתאפס רק בסגירת השיחה או בהחלפת מצב.
         */
        messages =
            messages +
                    AiMessage(
                        fromUser = true,
                        text = question
                    )

        /*
         * אין לגלול כעת. ממתינים עד שהתשובה תתווסף,
         * ואז מציבים פעם אחת את השאלה בתחילת המסך.
         */

        /*
         * העיבוד המקומי נשאר כפי שהוא, ולאחריו
         * מתבצעת קריאה אסינכרונית לשרת ה־AI.
         */
        scope.launch {
            assistantMemoryLocal
                .saveLastQuestion(
                    question
                )

            /*
             * שאלת המשך קצרה צריכה להגיע ל־Orchestrator
             * ללא הוספת חגורת הפרופיל.
             *
             * ה־Orchestrator משלים בעצמו את הנושא ואת
             * החגורה מהשאלה הקודמת.
             */
            val normalizedQuestionForContext =
                question
                    .lowercase()
                    .replace("־", " ")
                    .replace("–", " ")
                    .replace("—", " ")
                    .replace("-", " ")
                    .replace(
                        Regex("\\s+"),
                        " "
                    )
                    .trim()

            val isContextualExerciseFollowUp =
                (
                        assistantMode ==
                                AssistantMode.EXERCISE ||
                                assistantMode ==
                                AssistantMode.KMI_MATERIAL
                        ) &&
                        listOf(
                            "תן את הרשימה",
                            "תני את הרשימה",
                            "תן רשימה",
                            "תני רשימה",
                            "תציג את הרשימה",
                            "תציגי את הרשימה",
                            "הצג את הרשימה",
                            "הציגי את הרשימה",
                            "תראה את הרשימה",
                            "תראי את הרשימה",
                            "תראה את כולם",
                            "תראי את כולם",
                            "תציג את כולם",
                            "תציגי את כולם",
                            "תן את כולם",
                            "תני את כולם",
                            "מה השמות שלהם",
                            "מה השמות שלהן",
                            "מהם השמות",
                            "תסביר אותו",
                            "תסביר אותה",
                            "תן עליו הסבר",
                            "תן עליה הסבר",
                            "איך מבצעים אותו",
                            "איך מבצעים אותה",
                            "list them",
                            "show the list",
                            "show them all",
                            "give me the list",
                            "what are their names",
                            "explain it",
                            "how to perform it"
                        ).any { marker ->
                            normalizedQuestionForContext ==
                                    marker ||
                                    normalizedQuestionForContext
                                        .startsWith(
                                            "$marker "
                                        )
                        }

            /*
             * בשאלת המשך לא מוסיפים סימון מצב או חגורת
             * פרופיל. בשאלה חדשה ממשיכים בניתוב הרגיל.
             */
            val routedQuestion =
                if (isContextualExerciseFollowUp) {
                    question
                } else {
                    when (assistantMode) {
                        /*
                         * מוסיפים סימון ניתוב בלבד, בלי לשנות את
                         * הכמות או את טווח הזמן שביקש המשתמש.
                         *
                         * לדוגמה:
                         * "מהם 3 האימונים האחרונים"
                         * הופך ל:
                         * "מידע על אימונים. מהם 3 האימונים האחרונים"
                         */
                AssistantMode.TRAININGS -> {
                    /*
                     * הביטוי "רשימת אימונים" מזוהה במפורש
                     * על ידי ה־Orchestrator כמקור TRAININGS.
                     * השאלה המקורית נשמרת במלואה אחריו,
                     * כולל כמות וטווח הזמן שביקש המשתמש.
                     */
                    if (isEnglish) {
                        "Training list. $question"
                    } else {
                        "רשימת אימונים. $question"
                    }
                }

                AssistantMode.KMI_MATERIAL -> {
                    val normalizedQuestion = question
                        .lowercase()
                        .replace("־", " ")
                        .replace("–", " ")
                        .replace("-", " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()

                    /*
                     * אם המשתמש אמר חגורה כלשהי במפורש,
                     * אסור לצרף לשאלה את החגורה השמורה בפרופיל.
                     * החגורה המפורשת תמיד קודמת לברירת המחדל.
                     */
                    val questionContainsExplicitBelt = listOf(
                        "לבנה",
                        "לבן",
                        "white",
                        "צהובה",
                        "צהוב",
                        "yellow",
                        "כתומה",
                        "כתום",
                        "orange",
                        "ירוקה",
                        "ירוק",
                        "green",
                        "כחולה",
                        "כחול",
                        "blue",
                        "חומה",
                        "חום",
                        "brown",
                        "שחורה",
                        "שחור",
                        "black"
                    ).any { beltMarker ->
                        beltMarker in normalizedQuestion
                    }

                    val beltContext =
                        if (questionContainsExplicitBelt) {
                            ""
                        } else {
                            registeredBeltText.orEmpty()
                        }

                    if (isEnglish) {
                        buildString {
                            append("KAMI material. ")

                            if (beltContext.isNotBlank()) {
                                append("Belt: ")
                                append(beltContext)
                                append(". ")
                            }

                            append(question)
                        }
                    } else {
                        buildString {
                            append("חומר ק.מ.י. ")

                            if (beltContext.isNotBlank()) {
                                append("חגורה ")
                                append(beltContext)
                                append(". ")
                            }

                            append(question)
                        }
                    }
                }

                /*
                 * במצב הסבר על תרגיל, גם הזנת שם בלבד היא
                 * בקשת הסבר ברורה. התוספת משמשת לניתוב בלבד;
                 * בכרטיס העליון עדיין מוצג הטקסט המקורי.
                 */
                AssistantMode.EXERCISE -> {
                    if (isEnglish) {
                        "Explain exercise. $question"
                    } else {
                        "הסבר תרגיל. $question"
                    }
                }

                        null -> question
                    }
                }

            val response = try {
                assistantOrchestrator.process(
                    question = routedQuestion,
                    isEnglish = isEnglish
                )
            } catch (error: Throwable) {
                null
            }

            if (response == null) {
                val errorAnswer = tr(
                    "אירעה תקלה רגעית בעיבוד הבקשה. אפשר לנסות שוב.",
                    "A temporary issue occurred while processing the request. Please try again."
                )

                isThinking = false
                resultQuality = AssistantResultQuality.ERROR
                lastAiAnswer = errorAnswer

                messages = messages + AiMessage(
                    fromUser = false,
                    text = errorAnswer,
                    relatedQuestion = question
                )

                followUpSuggestions = listOf(
                    AssistantSuggestion(
                        label = tr("נסה שוב", "Try again"),
                        query = question
                    )
                )

                saveAssistantCommandLog(
                    rawCommand = question,
                    status = AssistantLogStatus.PROCESSING_ERROR,
                    answer = errorAnswer
                )

                speakBest(errorAnswer)
                return@launch
            }

            val assistantResult = response.result

            /*
             * ה־Orchestrator עשוי להשלים שאלת המשך קצרה
             * באמצעות הנושא והחגורה מהשאלה הקודמת.
             *
             * לדוגמה:
             * "תן את רשימת התרגילים"
             *
             * יכול להיפתר כ:
             * "תן את רשימת תרגילי הסכין בחגורה כחולה".
             */
            val resolvedAssistantQuestion =
                response.resolution
                    .resolvedQuestion
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?: question

            /*
             * סדר העדיפויות:
             *
             * 1. חגורה שנאמרה בשאלה הנוכחית.
             * 2. חגורה שנמצאה בשאלה שה־Orchestrator השלים.
             * 3. חגורה שנשמרה ב־resolution.
             * 4. חגורה שנשמרה בהקשר השיחה.
             * 5. חגורת הפרופיל כברירת מחדל בלבד.
             */
            val preferredBelt =
                detectBeltEnum(
                    question
                )
                    ?: detectBeltEnum(
                        resolvedAssistantQuestion
                    )
                    ?: response.resolution.belt
                    ?: response.context.belt
                    ?: detectBeltEnum(
                        registeredBeltText.orEmpty()
                    )

            /*
             * במצב הסבר תרגיל משתמשים במנוע התרגילים המלא.
             *
             * המנוע בודק קודם אם קיימות מספר התאמות:
             * - התאמה מדויקת אחת מחזירה הסבר.
             * - שם כללי מחזיר רשימת תרגילים לבחירה.
             *
             * אסור לקרוא כאן ישירות ל-findBest(), משום שהוא
             * מחזיר תמיד תוצאה אחת ועלול לבחור תרגיל שרירותי.
             */
            val exerciseAnswer =
                if (
                    assistantMode == AssistantMode.EXERCISE ||
                    assistantResult.source ==
                    AssistantKnowledgeSource.EXERCISES
                ) {
                    /*
                     * משתמשים בשאלה שהושלמה על ידי
                     * ה־Orchestrator ולא בשאלת ההמשך
                     * הקצרה שהמשתמש אמר.
                     *
                     * הטקסט המקורי עדיין נשאר מוצג
                     * בכרטיס השאלה של המשתמש.
                     */
                    ExerciseAssistantEngine.answer(
                        question =
                            resolvedAssistantQuestion,
                        preferredBelt =
                            preferredBelt,
                        isEnglish =
                            isEnglish
                    )
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                } else {
                    null
                }

            /*
             * אם מנוע התרגילים החזיר רשימת בחירה, היא מוצגת
             * בשלמותה ולא נדרסת על ידי התאמת findBest().
             */
            val localFinalAnswer =
                sanitizeAssistantMarkup(
                    exerciseAnswer
                        ?: assistantResult
                            .primaryText()
                )
                    .ifBlank {
                        tr(
                            "לא התקבלה תשובה מהמאגר. נסה לנסח את הבקשה בצורה אחרת.",
                            "No answer was returned. Try phrasing the request differently."
                        )
                    }

            /*
             * קוראים את ההתקדמות ממקור האמת רק עבור
             * החגורה הרלוונטית לשיחה.
             *
             * הכשל בקריאת ההתקדמות אינו מפיל את העוזר;
             * במקרה כזה השיחה ממשיכה ללא פרופיל התקדמות.
             */
            val progressSnapshot =
                if (
                    vm != null &&
                    preferredBelt != null
                ) {
                    runCatching {
                        vm.readBeltProgressForAssistant(
                            belt =
                                preferredBelt
                        )
                    }
                        .getOrNull()
                } else {
                    null
                }

            /*
             * המנוע המרוחק מקבל:
             * - את השאלה המקורית.
             * - את החגורה המועדפת.
             * - את היסטוריית השיחה.
             * - את התשובה המקומית כמידע מאומת.
             * - את סימוני יודע/לא יודע של המשתמש.
             *
             * אם אין מנוי, אין מכסה או קיימת תקלה,
             * התוצאה המקומית נשארת ללא שינוי.
             */
            val remoteResult =
                RemoteAssistantEngine.answer(
                    question = question,
                    preferredBelt =
                        preferredBelt,
                    isEnglish =
                        isEnglish,
                    conversationHistory =
                        remoteConversationHistory,
                    additionalUserProfile =
                        buildString {
                            registeredBeltText
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.let { beltText ->
                                    appendLine(
                                        "registeredBelt=$beltText"
                                    )
                                }

                            assistantMode
                                ?.let { mode ->
                                    appendLine(
                                        "selectedAssistantMode=${mode.name}"
                                    )
                                }

                            progressSnapshot
                                ?.let { snapshot ->
                                    appendLine(
                                        "progressBeltId=${snapshot.belt.id}"
                                    )
                                    appendLine(
                                        "progressBeltName=${snapshot.belt.name}"
                                    )
                                    appendLine(
                                        "totalExercises=${snapshot.totalExercises}"
                                    )
                                    appendLine(
                                        "knownExercisesCount=${snapshot.knownExercises.size}"
                                    )
                                    appendLine(
                                        "unknownExercisesCount=${snapshot.unknownExercises.size}"
                                    )
                                    appendLine(
                                        "unmarkedExercisesCount=${snapshot.unmarkedExercisesCount}"
                                    )

                                    appendLine(
                                        "knownExercises:"
                                    )

                                    if (
                                        snapshot.knownExercises
                                            .isEmpty()
                                    ) {
                                        appendLine(
                                            "- none"
                                        )
                                    } else {
                                        snapshot.knownExercises
                                            .take(20)
                                            .forEach { item ->
                                                appendLine(
                                                    "- ${item.title} | topic=${item.topicTitle}" +
                                                            item.subTopicTitle
                                                                ?.takeIf {
                                                                    it.isNotBlank()
                                                                }
                                                                ?.let {
                                                                    " | subTopic=$it"
                                                                }
                                                                .orEmpty()
                                                )
                                            }

                                        val omittedKnown =
                                            snapshot.knownExercises.size -
                                                    20

                                        if (omittedKnown > 0) {
                                            appendLine(
                                                "- $omittedKnown additional known exercises omitted"
                                            )
                                        }
                                    }

                                    appendLine(
                                        "unknownExercises:"
                                    )

                                    if (
                                        snapshot.unknownExercises
                                            .isEmpty()
                                    ) {
                                        appendLine(
                                            "- none"
                                        )
                                    } else {
                                        snapshot.unknownExercises
                                            .take(20)
                                            .forEach { item ->
                                                appendLine(
                                                    "- ${item.title} | topic=${item.topicTitle}" +
                                                            item.subTopicTitle
                                                                ?.takeIf {
                                                                    it.isNotBlank()
                                                                }
                                                                ?.let {
                                                                    " | subTopic=$it"
                                                                }
                                                                .orEmpty()
                                                )
                                            }

                                        val omittedUnknown =
                                            snapshot.unknownExercises.size -
                                                    20

                                        if (omittedUnknown > 0) {
                                            appendLine(
                                                "- $omittedUnknown additional unknown exercises omitted"
                                            )
                                        }
                                    }
                                }
                        }
                            .trim(),
                    verifiedLocalAnswer =
                        localFinalAnswer
                )

            val remoteAnswer =
                when (remoteResult) {
                    is RemoteAssistantResult.Success ->
                        remoteResult.answer

                    is RemoteAssistantResult.Fallback ->
                        null
                }

            val finalAnswer =
                remoteAnswer
                    ?.text
                    ?.let {
                        sanitizeAssistantMarkup(
                            it
                        )
                    }
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: localFinalAnswer

            /*
             * שומרים עד 12 הודעות אחרונות כדי לאפשר
             * הבנת שאלות המשך בלי לשלוח שיחה אינסופית.
             */
            remoteConversationHistory =
                (
                        remoteConversationHistory +
                                RemoteAssistantMessage(
                                    role = "user",
                                    text = question
                                ) +
                                RemoteAssistantMessage(
                                    role = "assistant",
                                    text = finalAnswer
                                )
                        )
                    .takeLast(12)

            assistantMode = when (assistantResult.source) {
                AssistantKnowledgeSource.EXERCISES ->
                    AssistantMode.EXERCISE

                AssistantKnowledgeSource.TRAININGS,
                AssistantKnowledgeSource.USER_PROFILE ->
                    AssistantMode.TRAININGS

                AssistantKnowledgeSource.MATERIAL ->
                    AssistantMode.KMI_MATERIAL

                AssistantKnowledgeSource.NAVIGATION,
                AssistantKnowledgeSource.UNKNOWN ->
                    assistantMode
            }

            response.context.exerciseName
                ?.takeIf { it.isNotBlank() }
                ?.let { exerciseName ->
                    assistantMemoryLocal.saveLastExercise(exerciseName)
                }

            resultQuality = when (assistantResult) {
                is AssistantResult.Error ->
                    AssistantResultQuality.ERROR

                is AssistantResult.NotFound,
                is AssistantResult.MissingInformation,
                is AssistantResult.Clarification ->
                    AssistantResultQuality.NEEDS_CLARIFICATION

                else -> {
                    when (assistantResult.matchQuality()) {
                        AssistantMatchQuality.EXACT,
                        AssistantMatchQuality.HIGH ->
                            AssistantResultQuality.EXACT

                        AssistantMatchQuality.MEDIUM ->
                            AssistantResultQuality.RELEVANT

                        AssistantMatchQuality.LOW,
                        AssistantMatchQuality.NONE ->
                            AssistantResultQuality.NEEDS_CLARIFICATION
                    }
                }
            }

            if (
                remoteAnswer
                    ?.needsClarification == true
            ) {
                resultQuality =
                    AssistantResultQuality
                        .NEEDS_CLARIFICATION
            }

            val resultSuggestions =
                assistantResult.suggestedActions.map { action ->
                    AssistantSuggestion(
                        label = action.label(isEnglish),
                        query = action.query(isEnglish)
                    )
                }

            val clarificationSuggestions =
                if (assistantResult is AssistantResult.Clarification) {
                    assistantResult.options.map { option ->
                        AssistantSuggestion(
                            label = option.title,
                            query = option.title
                        )
                    }
                } else {
                    emptyList()
                }

            followUpSuggestions =
                (clarificationSuggestions + resultSuggestions)
                    .filter {
                        it.label.isNotBlank() &&
                                it.query.isNotBlank()
                    }
                    .distinctBy {
                        normalizeExerciseQuery(it.query)
                    }
                    .take(5)

            val trainingItems =
                if (
                    assistantResult is
                            AssistantResult.ResultList &&
                    assistantResult.source ==
                    AssistantKnowledgeSource.TRAININGS
                ) {
                    assistantResult.items
                } else {
                    emptyList()
                }

            val materialItems =
                if (
                    assistantResult is
                            AssistantResult.ResultList &&
                    assistantResult.source ==
                    AssistantKnowledgeSource.MATERIAL
                ) {
                    assistantResult.items
                } else {
                    emptyList()
                }

            /*
      * שומרים גם את ההתאמה עצמה ולא רק את הכותרת,
      * כדי להשתמש בחגורה האמיתית שאליה התרגיל שייך.
      */
            val verifiedExerciseMatch =
                runCatching {
                    il.kmi.app.domain
                        .ExplanationSearchIndex
                        .findBest(
                            query =
                                question,
                            preferredBelt =
                                preferredBelt,
                            minScore =
                                180
                        )
                }
                    .getOrNull()

            val verifiedAnswerTitle =
                verifiedExerciseMatch
                    ?.title
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }

            val baseAnswerTitle =
                remoteAnswer
                    ?.title
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: verifiedAnswerTitle
                    ?: when (assistantMode) {
                        AssistantMode.EXERCISE ->
                            tr(
                                "תשובה על תרגילים",
                                "Exercise answer"
                            )

                        AssistantMode.KMI_MATERIAL ->
                            tr(
                                "חומר ק.מ.י",
                                "KAMI material"
                            )

                        AssistantMode.TRAININGS ->
                            tr(
                                "מידע על אימונים",
                                "Training information"
                            )

                        null ->
                            tr(
                                "תשובת העוזר",
                                "Assistant answer"
                            )
                    }

            /*
             * בתשובות על תרגילים או חומר מציגים תמיד
             * את החגורה הרלוונטית בכותרת הכרטיס.
             *
             * חגורת ההתאמה המדויקת קודמת לחגורת המשתמש.
             */
            val answerBelt =
                verifiedExerciseMatch?.belt
                    ?: preferredBelt

            val isBeltScopedAnswer =
                assistantMode ==
                        AssistantMode.EXERCISE ||
                        assistantMode ==
                        AssistantMode.KMI_MATERIAL ||
                        assistantResult.source ==
                        AssistantKnowledgeSource.EXERCISES ||
                        assistantResult.source ==
                        AssistantKnowledgeSource.MATERIAL

            val answerBeltLabel =
                answerBelt
                    ?.takeIf {
                        isBeltScopedAnswer
                    }
                    ?.let { belt ->
                        beltDisplayLabel(
                            belt = belt,
                            isEnglish = isEnglish
                        )
                    }

            val resolvedAnswerTitle =
                answerBeltLabel
                    ?.takeIf { beltLabel ->
                        /*
                         * מונעים הוספה כפולה אם השרת כבר
                         * כלל את צבע החגורה בכותרת.
                         */
                        !baseAnswerTitle.contains(
                            beltLabel,
                            ignoreCase = true
                        )
                    }
                    ?.let { beltLabel ->
                        "$baseAnswerTitle • $beltLabel"
                    }
                    ?: baseAnswerTitle

            /*
             * מקור EXERCISES כולל כמה סוגי תשובות:
             * הסבר, חיפוש, רשימה וספירה.
             *
             * רק כוונת הסבר מפורשת מסומנת כהסבר
             * לצורך יצירת משפט ההקראה המקוצר.
             */
            val isExerciseExplanationAnswer =
                (
                        response.resolution.intent ==
                                il.kmi.app.ui.assistant.core
                                    .AssistantIntent.EXPLAIN_EXERCISE ||
                                response.resolution.intent ==
                                il.kmi.app.ui.assistant.core
                                    .AssistantIntent.EXERCISE
                        ) &&
                        assistantResult.source ==
                        AssistantKnowledgeSource.EXERCISES

            val spokenExerciseName =
                verifiedAnswerTitle
                    ?: response.context
                        .exerciseName
                        ?.trim()
                        ?.takeIf { name ->
                            name.isNotBlank()
                        }

            val aiMessage =
                AiMessage(
                    fromUser =
                        false,
                    text =
                        finalAnswer,
                    answerTitle =
                        resolvedAnswerTitle,
                    relatedQuestion =
                        question,
                    exerciseName =
                        spokenExerciseName,
                    isExerciseExplanation =
                        isExerciseExplanationAnswer,
                    trainingItems =
                        trainingItems,
                    materialItems =
                        materialItems
                )

            /*
             * מצרפים את התשובה לשיחה ושומרים מספר
             * מוגבל של הודעות כדי למנוע עומס במסך.
             */
            /*
             * מוסיפים את התשובה בלי למחוק את תחילת
             * השיחה הנוכחית.
             */
            messages =
                messages +
                        aiMessage

            lastAiAnswer = finalAnswer
            isThinking = false

            val logStatus = when (assistantResult) {
                is AssistantResult.Error ->
                    AssistantLogStatus.PROCESSING_ERROR

                is AssistantResult.NotFound,
                is AssistantResult.MissingInformation ->
                    AssistantLogStatus.NOT_EXECUTED

                is AssistantResult.Clarification ->
                    AssistantLogStatus.ALTERNATIVES_SHOWN

                is AssistantResult.Answer,
                is AssistantResult.ResultList ->
                    AssistantLogStatus.SUCCESS
            }

            saveAssistantCommandLog(
                rawCommand = question,
                status = logStatus,
                alternatives = followUpSuggestions.map {
                    it.query
                },
                answer = finalAnswer
            )

            /*
             * סוג התוצאה לבדו אינו מספיק לזיהוי רשימה.
             *
             * גם תשובה שמסווגת כ־Answer יכולה להכיל בפועל
             * רשימה ממוספרת של תרגילים. לכן בכל תשובה שאינה
             * רשימת אימונים בודקים את מבנה הטקסט עצמו.
             */
            val spokenAnswer =
                if (
                    assistantResult is AssistantResult.ResultList &&
                    assistantResult.source ==
                    AssistantKnowledgeSource.TRAININGS
                ) {
                    shortTrainingSpeech(
                        question = question,
                        answer = finalAnswer,
                        isEnglish = isEnglish
                    )
                } else {
                    assistantAnswerTextForSpeech(
                        answer = finalAnswer,
                        isEnglish = isEnglish,
                        exerciseName =
                            aiMessage.exerciseName,
                        isExerciseExplanation =
                            aiMessage.isExerciseExplanation
                    )
                }

            speakBest(spokenAnswer)
        }
    }

    // ✅ STT -> Send
    LaunchedEffect(pendingSendFromStt) {
        val q = pendingSendFromStt ?: return@LaunchedEffect
        pendingSendFromStt = null

        // מנהל השיחה החדש מזהה בעצמו אם מדובר
        // בתרגיל, חומר ק.מ.י או מידע על אימונים.
        sendQuestion(q)
    }

    // ✅ גורם לשדה הקלט להופיע מעל המקלדת (ולא להיבלע)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        var didIntroSpeak by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (!didIntroSpeak) {
                didIntroSpeak = true
                speakBest(
                    tr(
                        "שלום. כאן יוּבַל, העוזר האישי שלך. אנא בחר נושא מתוך הרשימה שלפניך כדי שנוכל להתחיל.",
                        "Hello. This is You-val, your personal assistant. Please choose a topic from the list so we can begin."
                    )
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(graniteBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    KmiTopBar(
                        title = if (isEnglish) {
                            "Yuval – Personal Assistant"
                        } else {
                            "יובל – העוזר האישי"
                        },
                        currentLang = resolvedLang,
                        showMenu = true,
                        showFontQuick = false,
                        showRoleStatus = true,
                        showSettings = true,

                        /*
                         * עוצרים האזנה והקראה, סוגרים את
                         * העוזר ורק אז פותחים את ההגדרות.
                         * כך מסך ההגדרות לא נפתח מאחורי
                         * דיאלוג העוזר.
                         */
                        onSettings = {
                            /*
                             * עוצרים האזנה והקראה, אך לא
                             * סוגרים את דיאלוג העוזר.
                             *
                             * כך לאחר לחיצה על "אישור"
                             * בהגדרות חוזרים למסך העוזר
                             * ולמצב השיחה האחרון.
                             */
                            stopListeningHard()
                            stopSpeaking()
                            DrawerBridge.openSettings()
                        },

// מציג את סרגל האייקונים, ללא כפתורי המסך התחתונים.
                        showBottomActions = true,
                        showBottomHelp = true,
                        showBottomShare = true,

                        showModePill = true,
                        showRoleBadge = true,
                        showTopHome = false,
                        showTopSearch = false,
                        isInsideAssistant = true,
                        useCloseIcon = false,
                        onBack = null,
                        // ✅ בית במסך הזה = סגירת הדיאלוג וחזרה למסך שמאחוריו
                        onHome = {
                            stopListeningHard()
                            stopSpeaking()
                            DrawerBridge.openHome()
                            onDismiss()
                        },

                        onSearch = {
                            // Search is handled by KmiTopBar.
                        },
                        onPickSearchResult = { key ->
                            val parts = when {
                                "|" in key -> key.split("|", limit = 3)
                                "::" in key -> key.split("::", limit = 3)
                                "/" in key -> key.split("/", limit = 3)
                                else -> listOf("", "", key)
                            }

                            val rawItem = parts.getOrNull(2).orEmpty().trim()
                            val displayName = ExerciseTitleFormatter
                                .displayName(rawItem)
                                .ifBlank { rawItem }
                                .trim()

                            if (displayName.isNotBlank()) {
                                assistantMode = AssistantMode.EXERCISE
                                input = displayName
                                sendQuestion(displayName)
                            }
                        },
                        onOpenDrawer = {
                            stopListeningHard()
                            stopSpeaking()

                            if (onOpenDrawer != null) {
                                onOpenDrawer.invoke()
                            } else {
                                DrawerBridge.open()
                            }
                        },

                        // ✅ לא להציג הודעת חסימה כאן
                        homeDisabledToast = null
                    )

                    /*
                 * כרטיס מצב העוזר נמצא ברכיב נפרד,
                 * כדי לצמצם את AiAssistantDialog.
                 */
                    AssistantModeHeader(
                        assistantMode = assistantMode,
                        isEnglish = isEnglish,
                        premiumCardBrush = premiumCardBrush,
                        onBackToModePicker = {
                            backToModePicker()
                        }
                    )

                    if (assistantMode == null) {
                        AssistantModePicker(
                            assistantMode = assistantMode,
                            isEnglish = isEnglish,
                            premiumCardBrush = premiumCardBrush,
                            onModeSelected = { selectedMode ->
                                setAssistantMode(selectedMode)
                                stopSpeaking()
                                pendingNavAfterSpeak = null

                                val openingText =
                                    when (selectedMode) {
                                        AssistantMode.EXERCISE ->
                                            tr(
                                                "אוקיי. אני מוכן להסביר על תרגילים.",
                                                "Okay. I'm ready to explain exercises."
                                            )

                                        AssistantMode.TRAININGS ->
                                            tr(
                                                "אוקיי. אני מוכן לתת מידע על אימונים.",
                                                "Okay. I'm ready to provide training information."
                                            )

                                        AssistantMode.KMI_MATERIAL ->
                                            tr(
                                                "מעולה. מצב חומר ק.מ.י פעיל. תגיד נושא או שם תרגיל ואני אחפש לך במאגר.",
                                                "Great. KAMI material mode is active. Say a topic or exercise name and I will search it in the database."
                                            )
                                    }

                                speak(openingText)
                            }
                        )
                    }

                    val shouldShowMessagesCard =
                        assistantMode != null ||
                                messages.isNotEmpty() ||
                                isThinking

                    if (shouldShowMessagesCard) {
                        Surface(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(28.dp),
                            tonalElevation = 0.dp,
                            shadowElevation = 10.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            if (
                                messages.isEmpty() &&
                                !isThinking
                            ) {
                                AssistantEmptyState(
                                    assistantMode = assistantMode,
                                    isEnglish = isEnglish,
                                    emptyStateText = emptyStateText
                                )
                            } else if (showPremiumAnswerLayout) {
                                val answerText =
                                    latestAssistantMessage
                                        ?.text
                                        ?.trim()
                                        .orEmpty()

                                val materialItems =
                                    latestAssistantMessage
                                        ?.materialItems
                                        .orEmpty()

                                val answerIndex =
                                    latestAssistantMessage
                                        ?.let { messages.indexOf(it) }
                                        ?: -1
                                val answerFeedback =
                                    latestAssistantMessage?.feedback ?: Feedback.NONE

                                val feedbackQuestionText = if (showMaterialAnswerLayout) {
                                    displayTopRequestText
                                } else {
                                    displayExerciseName
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (displayTopRequestText.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(22.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            tonalElevation = 0.dp,
                                            shadowElevation = 5.dp
                                        ) {
                                            Text(
                                                text = displayTopRequestText,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = 14.dp,
                                                        vertical = 7.dp
                                                    ),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                textAlign = textAlignPrimary,
                                                style = KmiTypography.secondary.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = true),
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color(0xFFF1EDF7),
                                        tonalElevation = 0.dp,
                                        shadowElevation = 4.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(explanationScrollState)
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                                    .padding(bottom = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isThinking && answerText.isBlank()) {
                                                    val dotsTransition =
                                                        rememberInfiniteTransition(label = "thinkingDotsExercise")

                                                    val dotAlpha by dotsTransition.animateFloat(
                                                        initialValue = 0.25f,
                                                        targetValue = 1f,
                                                        animationSpec = infiniteRepeatable(
                                                            animation = tween(650),
                                                            repeatMode = RepeatMode.Reverse
                                                        ),
                                                        label = "dotAlphaExercise"
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = tr(
                                                                "יובל חושב",
                                                                "Yuval is thinking"
                                                            ),
                                                            style =
                                                                KmiTypography.caption,
                                                            color =
                                                                MaterialTheme.colorScheme
                                                                    .onSurfaceVariant,
                                                            textAlign =
                                                                textAlignPrimary
                                                        )

                                                        Spacer(Modifier.width(6.dp))

                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(
                                                                    MaterialTheme.colorScheme.primary.copy(
                                                                        alpha = dotAlpha
                                                                    ),
                                                                    shape = RoundedCornerShape(50)
                                                                )
                                                        )
                                                    }
                                                }

                                                if (answerText.isNotBlank()) {
                                                    val answerAccentColor =
                                                        when (assistantMode) {
                                                            AssistantMode.EXERCISE ->
                                                                Color(0xFF6D4AFF)

                                                            AssistantMode.TRAININGS ->
                                                                Color(0xFF0F88A8)

                                                            AssistantMode.KMI_MATERIAL ->
                                                                Color(0xFF2563EB)

                                                            null ->
                                                                Color(0xFF6D4AFF)
                                                        }

                                                    val answerBackgroundColor =
                                                        when (assistantMode) {
                                                            AssistantMode.EXERCISE ->
                                                                Color(0xFFFCFAFF)

                                                            AssistantMode.TRAININGS ->
                                                                Color(0xFFF6FCFE)

                                                            AssistantMode.KMI_MATERIAL ->
                                                                Color(0xFFF7FAFF)

                                                            null ->
                                                                Color.White
                                                        }

                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .border(
                                                                width = 1.dp,
                                                                color =
                                                                    answerAccentColor.copy(
                                                                        alpha = 0.28f
                                                                    ),
                                                                shape =
                                                                    RoundedCornerShape(22.dp)
                                                            ),
                                                        shape = RoundedCornerShape(22.dp),
                                                        color = answerBackgroundColor,
                                                        tonalElevation = 0.dp,
                                                        shadowElevation = 9.dp
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(
                                                                    horizontal = 15.dp,
                                                                    vertical = 14.dp
                                                                ),
                                                            verticalArrangement =
                                                                Arrangement.spacedBy(11.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment =
                                                                    Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                    if (isEnglish) {
                                                                        Arrangement.Start
                                                                    } else {
                                                                        Arrangement.End
                                                                    }
                                                            ) {
                                                                if (isEnglish) {
                                                                    Surface(
                                                                        modifier = Modifier.size(32.dp),
                                                                        shape = RoundedCornerShape(
                                                                            11.dp
                                                                        ),
                                                                        color = Color(0xFFEDE9FE)
                                                                    ) {
                                                                        Box(
                                                                            contentAlignment =
                                                                                Alignment.Center
                                                                        ) {
                                                                            Icon(
                                                                                imageVector =
                                                                                    Icons.Filled.AutoAwesome,
                                                                                contentDescription = null,
                                                                                tint = Color(
                                                                                    0xFF6D4AFF
                                                                                ),
                                                                                modifier =
                                                                                    Modifier.size(18.dp)
                                                                            )
                                                                        }
                                                                    }

                                                                    Spacer(Modifier.width(9.dp))
                                                                }

                                                                StyledExplanationText(
                                                                    raw =
                                                                        latestAssistantMessage
                                                                            ?.answerTitle
                                                                            ?.trim()
                                                                            ?.takeIf {
                                                                                it.isNotBlank()
                                                                            }
                                                                            ?: when (
                                                                                assistantMode
                                                                            ) {
                                                                                AssistantMode.EXERCISE ->
                                                                                    tr(
                                                                                        "תשובה על תרגילים",
                                                                                        "Exercise answer"
                                                                                    )

                                                                                AssistantMode.KMI_MATERIAL ->
                                                                                    tr(
                                                                                        "חומר ק.מ.י",
                                                                                        "KAMI material"
                                                                                    )

                                                                                else ->
                                                                                    tr(
                                                                                        "התשובה של יובל",
                                                                                        "Yuval's answer"
                                                                                    )
                                                                            },
                                                                    modifier = Modifier.weight(1f),
                                                                    style =
                                                                        KmiTypography.cardTitle.copy(
                                                                            fontWeight =
                                                                                FontWeight.ExtraBold
                                                                        ),
                                                                    color = Color(0xFF302553),
                                                                    textAlign = textAlignPrimary
                                                                )

                                                                if (!isEnglish) {
                                                                    Spacer(Modifier.width(9.dp))

                                                                    Surface(
                                                                        modifier = Modifier.size(32.dp),
                                                                        shape = RoundedCornerShape(
                                                                            11.dp
                                                                        ),
                                                                        color = Color(0xFFEDE9FE)
                                                                    ) {
                                                                        Box(
                                                                            contentAlignment =
                                                                                Alignment.Center
                                                                        ) {
                                                                            Icon(
                                                                                imageVector =
                                                                                    Icons.Filled.AutoAwesome,
                                                                                contentDescription = null,
                                                                                tint = Color(
                                                                                    0xFF6D4AFF
                                                                                ),
                                                                                modifier =
                                                                                    Modifier.size(18.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            if (
                                                                showMaterialAnswerLayout &&
                                                                materialItems.isNotEmpty()
                                                            ) {
                                                                Column(
                                                                    modifier =
                                                                        Modifier.fillMaxWidth(),
                                                                    verticalArrangement =
                                                                        Arrangement.spacedBy(
                                                                            9.dp
                                                                        )
                                                                ) {
                                                                    Text(
                                                                        text = tr(
                                                                            "נמצאו ${materialItems.size} תוצאות",
                                                                            "${materialItems.size} results found"
                                                                        ),
                                                                        modifier =
                                                                            Modifier.fillMaxWidth(),
                                                                        color =
                                                                            Color(0xFF1D4ED8),
                                                                        textAlign =
                                                                            textAlignPrimary,
                                                                        style =
                                                                            KmiTypography.action
                                                                    )

                                                                    materialItems
                                                                        .forEachIndexed { index,
                                                                                          item ->
                                                                            AssistantMaterialCard(
                                                                                item = item,
                                                                                index = index,
                                                                                isEnglish =
                                                                                    isEnglish
                                                                            )
                                                                        }
                                                                }
                                                            } else {
                                                                /*
                                                                 * אותו רכיב שמשמש בדיאלוג ההסבר:
                                                                 * RED_BOLD מוצג באדום,
                                                                 * BLUE_BOLD מוצג בכחול.
                                                                 */
                                                                StyledExplanationText(
                                                                    raw = answerText,
                                                                    modifier =
                                                                        Modifier.fillMaxWidth(),
                                                                    style =
                                                                        KmiTypography.body,
                                                                    color =
                                                                        Color(0xFF232333),
                                                                    textAlign =
                                                                        textAlignPrimary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                if (
                                                    answerText.isNotBlank() &&
                                                    latestUserMessage != null
                                                ) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement =
                                                            Arrangement.spacedBy(9.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement =
                                                                if (isEnglish) {
                                                                    Arrangement.Start
                                                                } else {
                                                                    Arrangement.End
                                                                },
                                                            verticalAlignment =
                                                                Alignment.CenterVertically
                                                        ) {
                                                            Surface(
                                                                onClick = {
                                                                    speakBest(
                                                                        assistantAnswerTextForSpeech(
                                                                            answer = answerText,
                                                                            isEnglish = isEnglish,
                                                                            exerciseName =
                                                                                latestAssistantMessage
                                                                                    ?.exerciseName,
                                                                            isExerciseExplanation =
                                                                                latestAssistantMessage
                                                                                    ?.isExerciseExplanation
                                                                                        == true
                                                                        )
                                                                    )
                                                                },
                                                                shape = RoundedCornerShape(16.dp),
                                                                color = Color(0xFFEDE9FE),
                                                                border =
                                                                    androidx.compose.foundation.BorderStroke(
                                                                        width = 1.dp,
                                                                        color = Color(0xFFD8CFFD)
                                                                    )
                                                            ) {
                                                                Text(
                                                                    text = tr(
                                                                        "הקרא שוב",
                                                                        "Read again"
                                                                    ),
                                                                    modifier = Modifier.padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 8.dp
                                                                    ),
                                                                    color = Color(0xFF5B43B4),
                                                                    style =
                                                                        KmiTypography.action
                                                                )
                                                            }

                                                            Spacer(Modifier.width(8.dp))

                                                            Surface(
                                                                onClick = {
                                                                    input = tr(
                                                                        "בהמשך לתשובה, ",
                                                                        "About this answer, "
                                                                    )

                                                                    scope.launch {
                                                                        bringIntoViewRequester
                                                                            .bringIntoView()
                                                                    }
                                                                },
                                                                shape = RoundedCornerShape(16.dp),
                                                                color = Color.White,
                                                                border =
                                                                    androidx.compose.foundation.BorderStroke(
                                                                        width = 1.dp,
                                                                        color = Color(0xFFD8CFFD)
                                                                    )
                                                            ) {
                                                                Text(
                                                                    text = tr(
                                                                        "שאלת המשך",
                                                                        "Follow-up question"
                                                                    ),
                                                                    modifier = Modifier.padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 8.dp
                                                                    ),
                                                                    color = Color(0xFF5B43B4),
                                                                    style =
                                                                        KmiTypography.action
                                                                )
                                                            }
                                                        }

                                                        if (followUpSuggestions.isNotEmpty()) {
                                                            Text(
                                                                text = tr(
                                                                    "אפשר להמשיך מכאן:",
                                                                    "You can continue from here:"
                                                                ),
                                                                modifier = Modifier.fillMaxWidth(),
                                                                color = Color(0xFF667085),
                                                                style =
                                                                    KmiTypography.caption.copy(
                                                                        fontWeight =
                                                                            FontWeight.Bold
                                                                    ),
                                                                textAlign = textAlignPrimary
                                                            )

                                                            FlowRow(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                    Arrangement.spacedBy(7.dp),
                                                                verticalArrangement =
                                                                    Arrangement.spacedBy(7.dp)
                                                            ) {
                                                                followUpSuggestions.forEach { suggestion ->
                                                                    Surface(
                                                                        onClick = {
                                                                            saveAssistantCommandLog(
                                                                                rawCommand =
                                                                                    suggestion.query,
                                                                                status =
                                                                                    AssistantLogStatus
                                                                                        .SUGGESTION_SELECTED,
                                                                                alternatives =
                                                                                    followUpSuggestions
                                                                                        .map {
                                                                                            it.query
                                                                                        },
                                                                                answer =
                                                                                    lastAiAnswer
                                                                            )

                                                                            input = ""
                                                                            sendQuestion(
                                                                                suggestion.query
                                                                            )
                                                                        },
                                                                        shape =
                                                                            RoundedCornerShape(
                                                                                17.dp
                                                                            ),
                                                                        color =
                                                                            Color(0xFFF7F5FF),
                                                                        border =
                                                                            androidx.compose.foundation.BorderStroke(
                                                                                width = 1.dp,
                                                                                color =
                                                                                    Color(
                                                                                        0xFFCFC4F5
                                                                                    )
                                                                            ),
                                                                        shadowElevation = 2.dp
                                                                    ) {
                                                                        Text(
                                                                            text =
                                                                                suggestion.label,
                                                                            modifier =
                                                                                Modifier.padding(
                                                                                    horizontal =
                                                                                        11.dp,
                                                                                    vertical =
                                                                                        8.dp
                                                                                ),
                                                                            color =
                                                                                Color(
                                                                                    0xFF4C3A80
                                                                                ),
                                                                            style =
                                                                                KmiTypography.secondary.copy(
                                                                                    fontWeight =
                                                                                        FontWeight.Bold
                                                                                )
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                if (answerIndex >= 0) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                setFeedback(
                                                                    answerIndex,
                                                                    Feedback.UNLIKE
                                                                )
                                                                saveAiFeedback(
                                                                    question = feedbackQuestionText,
                                                                    answer = answerText
                                                                )
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.ThumbDown,
                                                                contentDescription = tr(
                                                                    "לא אהבתי את התשובה",
                                                                    "Dislike answer"
                                                                ),
                                                                tint = when (answerFeedback) {
                                                                    Feedback.UNLIKE -> Color(
                                                                        0xFFEF4444
                                                                    )

                                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                                }
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                setFeedback(
                                                                    answerIndex,
                                                                    Feedback.LIKE
                                                                )
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.ThumbUp,
                                                                contentDescription = tr(
                                                                    "אהבתי את התשובה",
                                                                    "Like answer"
                                                                ),
                                                                tint = when (answerFeedback) {
                                                                    Feedback.LIKE -> Color(
                                                                        0xFF22C55E
                                                                    )

                                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                                }
                                                            )
                                                        }

                                                        // Feedback is saved directly from the Unlike button click.
                                                    }
                                                }
                                            }

                                        }

                                        LaunchedEffect(
                                            displayTopRequestText,
                                            answerText,
                                            isThinking
                                        ) {
                                            explanationScrollState.scrollTo(0)
                                        }
                                    }
                                }
                            } else {
                                /*
                                 * לאחר יותר משאלה ותשובה אחת מציגים
                                     * את כל השיחה הנוכחית בתוך אותו כרטיס.
                                     *
                                     * הרשימה נמצאת בתוך ה־Surface של השיחה,
                                     * ולכן ניתן לגלול עד להודעה הראשונה.
                                     */
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        /*
                                         * המדידה מתבצעת לפני verticalScroll,
                                         * ולכן מתקבל גובה החלון הגלוי ולא
                                         * גובה כל תוכן השיחה.
                                         */
                                        .onGloballyPositioned { coordinates ->
                                            conversationViewportHeightPx =
                                                coordinates.size.height
                                        }
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 12.dp
                                        )
                                        .verticalScroll(scrollState),
                                    verticalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {

                                    val latestUserMessageIndex =
                                        messages.indexOfLast { message ->
                                            message.fromUser
                                        }

                                    messages.forEachIndexed { index, msg ->
                                        val isLatestQuestion =
                                            msg.fromUser &&
                                                    index ==
                                                    latestUserMessageIndex

                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (isLatestQuestion) {
                                                            Modifier
                                                                .onGloballyPositioned { coordinates ->
                                                                    /*
                                                                     * השאלה היא ילדה ישירה של
                                                                     * תוכן ה־Column הנגלל.
                                                                     *
                                                                     * positionInParent מחזיר כבר
                                                                     * את מיקומה בתוך התוכן המלא.
                                                                     * אסור להוסיף שוב את ערך הגלילה,
                                                                     * משום שהדבר יוצר גלילת יתר לסוף.
                                                                     */
                                                                    latestQuestionScrollOffset =
                                                                        coordinates
                                                                            .positionInParent()
                                                                            .y
                                                                            .toInt()
                                                                }
                                                        } else {
                                                            Modifier
                                                        }
                                                    ),
                                            contentAlignment = when {
                                                msg.fromUser && !isEnglish -> Alignment.CenterEnd
                                                msg.fromUser && isEnglish -> Alignment.CenterStart
                                                !msg.fromUser && !isEnglish -> Alignment.CenterStart
                                                else -> Alignment.CenterEnd
                                            }
                                        ) {
                                            val bubbleColor =
                                                if (msg.fromUser) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color(0xFFF1EDF7)
                                                }

                                            val textColor =
                                                if (msg.fromUser) {
                                                    Color.White
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }

                                            Surface(
                                                color = bubbleColor,
                                                shape = RoundedCornerShape(
                                                    topStart = 18.dp,
                                                    topEnd = 18.dp,
                                                    bottomEnd = if (msg.fromUser) 2.dp else 18.dp,
                                                    bottomStart = if (msg.fromUser) 18.dp else 2.dp
                                                ),
                                                tonalElevation = 0.dp,
                                                shadowElevation = 2.dp
                                            ) {
                                                Column {
                                                    if (
                                                        !msg.fromUser &&
                                                        !msg.answerTitle
                                                            .isNullOrBlank()
                                                    ) {
                                                        StyledExplanationText(
                                                            raw =
                                                                msg.answerTitle,
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(
                                                                        start = 14.dp,
                                                                        end = 14.dp,
                                                                        top = 12.dp,
                                                                        bottom = 2.dp
                                                                    ),
                                                            style =
                                                                KmiTypography.cardTitle.copy(
                                                                    fontWeight =
                                                                        FontWeight.ExtraBold
                                                                ),
                                                            color =
                                                                Color(0xFF4C3A80),
                                                            textAlign =
                                                                textAlignPrimary
                                                        )
                                                    }

                                                    if (
                                                        !msg.fromUser &&
                                                        msg.trainingItems.isNotEmpty()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(
                                                                    horizontal = 10.dp,
                                                                    vertical = 10.dp
                                                                ),
                                                            verticalArrangement =
                                                                Arrangement.spacedBy(9.dp)
                                                        ) {
                                                            Text(
                                                                text = tr(
                                                                    "האימונים שמצאתי",
                                                                    "Trainings I found"
                                                                ),
                                                                color = Color(0xFF312E81),
                                                                style =
                                                                    KmiTypography.sectionTitle.copy(
                                                                        fontWeight =
                                                                            FontWeight.Black
                                                                    ),
                                                                textAlign =
                                                                    textAlignPrimary,
                                                                modifier =
                                                                    Modifier.fillMaxWidth()
                                                            )

                                                            msg.trainingItems.forEach { item ->
                                                                AssistantTrainingCard(
                                                                    item = item,
                                                                    isEnglish = isEnglish
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        if (msg.fromUser) {
                                                            Text(
                                                                text = msg.text,
                                                                color = textColor,
                                                                modifier =
                                                                    Modifier.padding(
                                                                        horizontal = 14.dp,
                                                                        vertical = 12.dp
                                                                    ),
                                                                textAlign =
                                                                    textAlignPrimary,
                                                                style =
                                                                    MaterialTheme
                                                                        .typography
                                                                        .bodyMedium
                                                            )
                                                        } else {
                                                            StyledExplanationText(
                                                                raw = msg.text,
                                                                modifier =
                                                                    Modifier.padding(
                                                                        horizontal = 14.dp,
                                                                        vertical = 12.dp
                                                                    ),
                                                                style =
                                                                    MaterialTheme
                                                                        .typography
                                                                        .bodyMedium,
                                                                color = textColor,
                                                                textAlign =
                                                                    textAlignPrimary
                                                            )
                                                        }
                                                    }

                                                    if (!msg.fromUser) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(
                                                                    start = if (isEnglish) 4.dp else 0.dp,
                                                                    end = if (isEnglish) 0.dp else 4.dp,
                                                                    bottom = 4.dp
                                                                ),
                                                            horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            IconButton(onClick = {
                                                                setFeedback(
                                                                    index,
                                                                    Feedback.LIKE
                                                                )
                                                            }) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.ThumbUp,
                                                                    contentDescription = tr(
                                                                        "אהבתי את התשובה",
                                                                        "Like answer"
                                                                    ),
                                                                    tint = when (msg.feedback) {
                                                                        Feedback.LIKE -> Color(
                                                                            0xFF22C55E
                                                                        )

                                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                                    }
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    setFeedback(
                                                                        index,
                                                                        Feedback.UNLIKE
                                                                    )

                                                                    val questionText = messages
                                                                        .take(index)
                                                                        .lastOrNull { it.fromUser }
                                                                        ?.text
                                                                        ?.trim()
                                                                        ?: ""

                                                                    if (questionText.isNotBlank()) {
                                                                        saveAiFeedback(
                                                                            questionText,
                                                                            msg.text
                                                                        )
                                                                    }
                                                                }
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.ThumbDown,
                                                                    contentDescription = tr(
                                                                        "לא אהבתי את התשובה",
                                                                        "Dislike answer"
                                                                    ),
                                                                    tint = when (msg.feedback) {
                                                                        Feedback.UNLIKE -> Color(
                                                                            0xFFEF4444
                                                                        )

                                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (isThinking) {
                                        val dotsTransition =
                                            rememberInfiniteTransition(label = "thinkingDots")

                                        val dotAlpha by dotsTransition.animateFloat(
                                            initialValue = 0.25f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(650),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "dotAlpha"
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp),
                                            horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = tr(
                                                    "יובל חושב",
                                                    "Yuval is thinking"
                                                ),
                                                style =
                                                    KmiTypography.caption,
                                                color =
                                                    MaterialTheme.colorScheme
                                                        .onSurfaceVariant,
                                                textAlign =
                                                    textAlignPrimary
                                            )

                                            Spacer(Modifier.width(6.dp))

                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha),
                                                        shape = RoundedCornerShape(50)
                                                    )
                                            )
                                        }
                                    }

                                    /*
                                     * משאירים מתחת להודעה האחרונה שטח בגובה
                                     * רוב אזור השיחה. כך ניתן להציב את השאלה
                                     * החדשה בראש עוד לפני שהתשובה התקבלה.
                                     */
                                    if (conversationViewportHeightPx > 0) {
                                        Spacer(
                                            modifier =
                                                Modifier.height(
                                                    with(density) {
                                                        (
                                                                conversationViewportHeightPx *
                                                                        0.82f
                                                                )
                                                            .toDp()
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .fillMaxWidth()
                        )
                    }

                    /*
                     * בזמן שמוצגת שיחה אין צורך בשורת "מדבר…"
                     * נפרדת: כפתור המיקרופון כבר מציג כפתור עצירה.
                     * הסתרתה משאירה את כל הגובה לכרטיס המידע.
                     */
                    if (isSpeaking && assistantMode == null) {
                        val eqTransition = rememberInfiniteTransition(label = "eq")

                        val bars = listOf(
                            eqTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(420, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar1"
                            ),
                            eqTransition.animateFloat(
                                initialValue = 0.6f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(520, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar2"
                            ),
                            eqTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 0.4f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(480, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar3"
                            ),
                            eqTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(560, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar4"
                            )
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(
                                    "מדבר…",
                                    "Speaking…"
                                ),
                                color =
                                    MaterialTheme.colorScheme.primary,
                                style =
                                    KmiTypography.caption,
                                textAlign =
                                    textAlignPrimary
                            )

                            Spacer(Modifier.width(10.dp))

                            bars.forEachIndexed { i, anim ->
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height((8 + anim.value * 16).dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                                if (i < bars.lastIndex) Spacer(Modifier.width(4.dp))
                            }
                        }
                    }

                    // ✅ Focus Sink (חייב להיות בתוך ה-Composition)
                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .focusRequester(focusSinkRequester)
                            .focusable()
                    )

                    val pulseTransition =
                        rememberInfiniteTransition(
                            label = "micPulse"
                        )

                    val pulseScale by pulseTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.18f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 650,
                                easing = FastOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    /*
                     * הפעימה של האייקון נשלטת ישירות ממצב ההאזנה.
                     * אנימציית הגל עצמה נוצרת בהמשך מחדש עבור כל סשן.
                     */
                    val micScale by animateFloatAsState(
                        targetValue = when {
                            isSpeaking -> pulseScale
                            isListening -> pulseScale
                            else -> 1f
                        },
                        animationSpec = tween(
                            durationMillis = 220,
                            easing = FastOutSlowInEasing
                        ),
                        label = "micScale"
                    )

                    val liveAssistantStatus = when {
                        isThinking && assistantMode == AssistantMode.EXERCISE ->
                            tr(
                                "מאתר את התרגיל ובודק את ההסבר המתאים…",
                                "Finding the exercise and checking the best explanation…"
                            )

                        isThinking && assistantMode == AssistantMode.KMI_MATERIAL ->
                            tr(
                                "מחפש בחומר ק.מ.י ומדרג את התוצאות…",
                                "Searching KAMI material and ranking the results…"
                            )

                        isThinking && assistantMode == AssistantMode.TRAININGS ->
                            tr(
                                "בודק את פרטי המשתמש והאימונים הקרובים…",
                                "Checking your profile and upcoming trainings…"
                            )

                        isThinking ->
                            tr(
                                "מבין את הבקשה ומכין תשובה…",
                                "Understanding your request and preparing an answer…"
                            )

                        isListening ->
                            tr(
                                "מקשיב — אפשר לדבר באופן טבעי…",
                                "Listening — you can speak naturally…"
                            )

                        else ->
                            speechStatusMessage
                    }

                    if (!liveAssistantStatus.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 3.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = when {
                                speechNeedsConfirmation ->
                                    Color(0xFFFFF8E7)

                                speechStatusMessage != null ->
                                    Color(0xFFFFF1F2)

                                isListening ->
                                    Color(0xFFF0EDFF)

                                else ->
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = when {
                                    speechNeedsConfirmation ->
                                        Color(0xFFF2C94C).copy(alpha = 0.65f)

                                    speechStatusMessage != null ->
                                        Color(0xFFFCA5A5).copy(alpha = 0.75f)

                                    else ->
                                        Color(0xFFDDD6FE)
                                }
                            ),
                            tonalElevation = 0.dp,
                            shadowElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement =
                                        if (isEnglish) {
                                            Arrangement.Start
                                        } else {
                                            Arrangement.End
                                        }
                                ) {
                                    if (isEnglish) {
                                        Surface(
                                            modifier = Modifier.size(30.dp),
                                            shape = CircleShape,
                                            color = Color(0xFFEDE9FE)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector =
                                                        Icons.Filled.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6D4AFF),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(9.dp))
                                    }

                                    Text(
                                        text = liveAssistantStatus,
                                        modifier = Modifier.weight(1f),
                                        color = when {
                                            speechNeedsConfirmation ->
                                                Color(0xFF8A5A00)

                                            speechStatusMessage != null ->
                                                Color(0xFFB42318)

                                            isListening ->
                                                Color(0xFF6246B5)

                                            else ->
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = textAlignPrimary
                                    )

                                    if (isThinking || isListening) {
                                        Spacer(Modifier.width(8.dp))

                                        val statusTransition =
                                            rememberInfiniteTransition(
                                                label = "assistantStatusDots"
                                            )

                                        val statusDotAlpha by
                                        statusTransition.animateFloat(
                                            initialValue = 0.30f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(550),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "assistantStatusDotAlpha"
                                        )

                                        Row(
                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {
                                            repeat(3) { index ->
                                                Box(
                                                    modifier = Modifier
                                                        .padding(horizontal = 2.dp)
                                                        .size((5 + index).dp)
                                                        .background(
                                                            color =
                                                                Color(0xFF6D4AFF).copy(
                                                                    alpha =
                                                                        if (index == 1) {
                                                                            statusDotAlpha
                                                                        } else {
                                                                            0.45f
                                                                        }
                                                                ),
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }

                                    if (!isEnglish) {
                                        Spacer(Modifier.width(9.dp))

                                        Surface(
                                            modifier = Modifier.size(30.dp),
                                            shape = CircleShape,
                                            color = Color(0xFFEDE9FE)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector =
                                                        Icons.Filled.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6D4AFF),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (
                                    speechNeedsConfirmation &&
                                    speechAlternatives.isNotEmpty()
                                ) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                                        verticalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        speechAlternatives.forEach { alternative ->
                                            Surface(
                                                modifier = Modifier.clickable {
                                                    val selectedText =
                                                        alternative.text.trim()

                                                    speechAlternatives = emptyList()
                                                    speechNeedsConfirmation = false
                                                    speechCanRetry = false
                                                    speechStatusMessage = null
                                                    input = selectedText
                                                    pendingSendFromStt = selectedText
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color.White,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = 1.dp,
                                                    color = Color(0xFFB8A9E8)
                                                ),
                                                shadowElevation = 2.dp
                                            ) {
                                                Text(
                                                    text = alternative.text,
                                                    modifier = Modifier.padding(
                                                        horizontal = 11.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    color = Color(0xFF4C3A80),
                                                    style =
                                                        KmiTypography.secondary.copy(
                                                            fontWeight =
                                                                FontWeight.Bold
                                                        ),
                                                    textAlign =
                                                        textAlignPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                if (speechCanRetry && !isListening) {
                                    Surface(
                                        onClick = {
                                            speechAlternatives = emptyList()
                                            speechNeedsConfirmation = false
                                            speechCanRetry = false
                                            speechStatusMessage = null
                                            pendingSendFromStt = null

                                            if (hasRecordAudioPermission()) {
                                                pendingStartStt = true
                                            } else {
                                                recordAudioPermissionLauncher.launch(
                                                    Manifest.permission.RECORD_AUDIO
                                                )
                                            }
                                        },
                                        modifier = Modifier.align(
                                            if (isEnglish) {
                                                Alignment.Start
                                            } else {
                                                Alignment.End
                                            }
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF6D4AFF)
                                    ) {
                                        Text(
                                            text = tr(
                                                "נסה שוב עם המיקרופון",
                                                "Try again with the microphone"
                                            ),
                                            modifier = Modifier.padding(
                                                horizontal = 13.dp,
                                                vertical = 8.dp
                                            ),
                                            color = Color.White,
                                            style =
                                                KmiTypography.action
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (assistantMode != null) {
                        val inputEnabled = !isThinking
                        val inputShape = RoundedCornerShape(26.dp)

                        Surface(
                            shape = inputShape,
                            tonalElevation = 0.dp,
                            shadowElevation = 12.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isListening ->
                                            MaterialTheme.colorScheme.primary

                                        isThinking ->
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.55f
                                            )

                                        else ->
                                            MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = inputShape
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier.size(
                                        scaledIconSize(44.dp)
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    /*
                                     * המפתח הוא מזהה סשן ההאזנה.
                                     * בכל לחיצה על המיקרופון נוצר Transition חדש
                                     * שמתחיל מהגל הראשון כשהוא גלוי לחלוטין.
                                     */
                                    if (isListening) {
                                        androidx.compose.runtime.key(
                                            currentListeningSessionId
                                        ) {
                                            val listeningWaveTransition =
                                                rememberInfiniteTransition(
                                                    label = "activeMicWave"
                                                )

                                            val listeningWaveScale by
                                            listeningWaveTransition.animateFloat(
                                                initialValue = 0.92f,
                                                targetValue = 1.55f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(
                                                        durationMillis = 1100,
                                                        easing = FastOutSlowInEasing
                                                    ),
                                                    repeatMode = RepeatMode.Restart
                                                ),
                                                label = "activeMicWaveScale"
                                            )

                                            val listeningWaveAlpha by
                                            listeningWaveTransition.animateFloat(
                                                initialValue = 0.32f,
                                                targetValue = 0f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(
                                                        durationMillis = 1100
                                                    ),
                                                    repeatMode = RepeatMode.Restart
                                                ),
                                                label = "activeMicWaveAlpha"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .size(
                                                        scaledIconSize(40.dp)
                                                    )
                                                    .scale(listeningWaveScale)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(
                                                            alpha = listeningWaveAlpha
                                                        ),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(
                                                scaledIconSize(38.dp)
                                            )
                                            .background(
                                                when {
                                                    isSpeaking -> Color(0x22E53935)

                                                    isListening ->
                                                        MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.14f
                                                        )

                                                    else -> Color.Transparent
                                                },
                                                shape = RoundedCornerShape(50)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        /*
                                         * micScale מוגדר פעם אחת מעל אזור שורת הקלט.
                                         * אין ליצור כאן Transition נוסף שמתחיל רק
                                         * לאחר שההאזנה כבר הופעלה.
                                         */
                                        IconButton(
                                            modifier = Modifier
                                                .size(
                                                    scaledIconSize(36.dp)
                                                )
                                                .scale(micScale),
                                            enabled = inputEnabled || isSpeaking || isListening,
                                            onClick = {
                                                if (isSpeaking) {
                                                    stopSpeaking()
                                                    return@IconButton
                                                }

                                                if (isListening) {
                                                    stopListeningHard()
                                                    return@IconButton
                                                }

                                                if (!inputEnabled) return@IconButton

                                                pendingSendFromStt = null

                                                if (hasRecordAudioPermission()) {
                                                    pendingStartStt = true
                                                } else {
                                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center
                                            ) {

                                                if (isListening) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(
                                                                scaledIconSize(42.dp)
                                                            )
                                                            .background(
                                                                MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.15f
                                                                ),
                                                                CircleShape
                                                            )
                                                    )
                                                }

                                                Icon(
                                                    imageVector = if (isSpeaking) {
                                                        Icons.Filled.Stop
                                                    } else {
                                                        Icons.Filled.Mic
                                                    },
                                                    contentDescription = when {
                                                        isSpeaking ->
                                                            tr("עצור דיבור", "Stop speaking")

                                                        isListening ->
                                                            tr("מקשיב", "Listening")

                                                        else ->
                                                            tr("הפעל מיקרופון", "Start microphone")
                                                    },
                                                    tint = when {
                                                        isSpeaking ->
                                                            MaterialTheme.colorScheme.error

                                                        isListening ->
                                                            Color(0xFF00C853)

                                                        inputEnabled ->
                                                            MaterialTheme.colorScheme.primary

                                                        else ->
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.55f
                                                            )
                                                    },
                                                    modifier = Modifier
                                                        .size(KmiIconSize.medium)
                                                        .scale(micScale)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.width(6.dp))

                                TextField(
                                    value = input,
                                    onValueChange = {
                                        if (!inputEnabled) return@TextField
                                        input = it
                                    },
                                    enabled = inputEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp, max = 118.dp)
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .onFocusEvent { focusState ->
                                            if (focusState.isFocused) {
                                                scope.launch {
                                                    bringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                        },
                                    minLines = 1,
                                    maxLines = 4,
                                    singleLine = false,
                                    placeholder = {
                                        Text(
                                            text = if (assistantMode == null) {
                                                tr(
                                                    "בחר נושא ואז כתוב כאן",
                                                    "Choose a mode and type here"
                                                )
                                            } else {
                                                dynamicInputPlaceholder
                                            },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.82f
                                            ),
                                            textAlign = textAlignPrimary,
                                            style = KmiTypography.caption,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },

                                    textStyle = KmiTypography.body.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = textAlignPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            val cleanInput = input.trim()
                                            if (!inputEnabled || cleanInput.isBlank()) return@KeyboardActions

                                            stopListeningHard()
                                            hideKeyboardHard()
                                            requestHideKeyboard = true
                                            sendQuestion(cleanInput)
                                        }
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor =
                                            MaterialTheme.colorScheme.surfaceVariant,

                                        unfocusedContainerColor =
                                            MaterialTheme.colorScheme.surfaceVariant,

                                        disabledContainerColor =
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.72f
                                            ),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.75f
                                        ),
                                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.82f
                                        ),
                                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.82f
                                        ),
                                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.82f
                                        )
                                    )
                                )

                                Spacer(Modifier.width(4.dp))

                                IconButton(
                                    onClick = {
                                        val cleanInput = input.trim()
                                        if (
                                            !inputEnabled ||
                                            cleanInput.isBlank()
                                        ) {
                                            return@IconButton
                                        }

                                        stopListeningHard()
                                        requestHideKeyboard = true
                                        sendQuestion(cleanInput)
                                    },
                                    enabled =
                                        inputEnabled &&
                                                input.trim().isNotBlank(),
                                    modifier = Modifier.size(
                                        scaledIconSize(44.dp)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = tr(
                                            "שלח שאלה",
                                            "Send question"
                                        ),
                                        tint =
                                            if (
                                                inputEnabled &&
                                                input.isNotBlank()
                                            ) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.55f
                                                )
                                            },
                                        modifier = Modifier.size(
                                            KmiIconSize.medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} // ✅ סוגר את AiAssistantDialog

private fun detectIntent(question: String): String {

    val q = question.lowercase()

    return when {

        "הסבר" in q || "explain" in q ->
            "EXPLAIN_EXERCISE"

        "רשימת תרגילים" in q ||
                "list exercises" in q ->
            "LIST_EXERCISES"

        "האימון הבא" in q ||
                "next training" in q ->
            "NEXT_TRAINING"

        else ->
            "UNKNOWN"
    }
}

// ───────────────────────────────
// זיהוי והצגת חגורה
// ───────────────────────────────
private fun detectBeltEnum(
    text: String
): Belt? {
    val normalized =
        text
            .lowercase()
            .replace("_", " ")
            .replace("-", " ")
            .replace("־", " ")
            .replace("–", " ")
            .replace("—", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    return when {
        "לבן" in normalized ||
                "לבנה" in normalized ||
                "white" in normalized ->
            Belt.WHITE

        "צהוב" in normalized ||
                "צהובה" in normalized ||
                "yellow" in normalized ->
            Belt.YELLOW

        "כתום" in normalized ||
                "כתומה" in normalized ||
                "orange" in normalized ->
            Belt.ORANGE

        "ירוק" in normalized ||
                "ירוקה" in normalized ||
                "green" in normalized ->
            Belt.GREEN

        "כחול" in normalized ||
                "כחולה" in normalized ||
                "blue" in normalized ->
            Belt.BLUE

        "חום" in normalized ||
                "חומה" in normalized ||
                "brown" in normalized ->
            Belt.BROWN

        "שחור" in normalized ||
                "שחורה" in normalized ||
                "black" in normalized ->
            Belt.BLACK

        else -> null
    }
}

private fun beltDisplayLabel(
    belt: Belt,
    isEnglish: Boolean
): String {
    return when (belt) {
        Belt.WHITE ->
            if (isEnglish) {
                "White belt"
            } else {
                "חגורה לבנה"
            }

        Belt.YELLOW ->
            if (isEnglish) {
                "Yellow belt"
            } else {
                "חגורה צהובה"
            }

        Belt.ORANGE ->
            if (isEnglish) {
                "Orange belt"
            } else {
                "חגורה כתומה"
            }

        Belt.GREEN ->
            if (isEnglish) {
                "Green belt"
            } else {
                "חגורה ירוקה"
            }

        Belt.BLUE ->
            if (isEnglish) {
                "Blue belt"
            } else {
                "חגורה כחולה"
            }

        Belt.BROWN ->
            if (isEnglish) {
                "Brown belt"
            } else {
                "חגורה חומה"
            }

        Belt.BLACK ->
            if (isEnglish) {
                "Black belt"
            } else {
                "חגורה שחורה"
            }

        else ->
            belt.name
    }
}
