package il.kmi.app.ui.assistant

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.Explanations
import il.kmi.app.search.asSharedRepo
import il.kmi.app.search.toShared
import il.kmi.app.ui.KmiTtsManager
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.search.KmiSearch
import il.kmi.shared.search.SearchHit
import kotlinx.coroutines.launch

private lateinit var assistantMemory: AssistantMemory

private enum class Feedback {
    NONE, LIKE, UNLIKE
}

private enum class AssistantMode {
    EXERCISE,   // מידע / הסבר על תרגיל
    TRAININGS,  // אימונים קרובים / לוח אימונים
    KMI_MATERIAL // חומר ק.מ.י (חיפוש בחומר)
}

private data class AiMessage(
    val fromUser: Boolean,
    val text: String,
    val relatedQuestion: String? = null,
    val feedback: Feedback = Feedback.NONE
)

// 🔹 פקודות ניווט בקול
sealed class VoiceNavCommand {
    object OpenHome : VoiceNavCommand()
    object OpenTraining : VoiceNavCommand()
    object OpenNextExercise : VoiceNavCommand()
    data class Custom(val raw: String) : VoiceNavCommand()
}

/**
 * ניתוח טקסט דיבור לפקודת ניווט + תמיכה ב-"יובל" כ-Wake word
 */
private fun parseVoiceNavCommand(raw: String): VoiceNavCommand? {
    var t = raw.trim()

    // Wake word – אם המשפט מתחיל ב"יובל", נוריד את זה מההתחלה
    if (t.startsWith("יובל")) {
        t = t.removePrefix("יובל")
            .removePrefix(",")
            .trimStart()
    }

    return when {
        "חזור למסך הבית" in t || "חזור לבית" in t || "מסך הבית" in t ->
            VoiceNavCommand.OpenHome

        "פתח אימון" in t || "פתח את האימון" in t ->
            VoiceNavCommand.OpenTraining

        "התרגיל הבא" in t || "פתח תרגיל הבא" in t ->
            VoiceNavCommand.OpenNextExercise

        else -> null
    }
}

// ───────────────────────────────
// הדגשת "עמידת מוצא" בתוך הסבר
// ───────────────────────────────
private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val stancePrefix = "עמידת מוצא"
    val idx = source.indexOf(stancePrefix)
    if (idx < 0) return AnnotatedString(source)

    val before = source.substring(0, idx)

    val endPunctIndex = listOf(',', '.')
        .map { ch -> source.indexOf(ch, idx + stancePrefix.length) }
        .filter { it >= 0 }
        .minOrNull()

    val stanceEndExclusive = if (endPunctIndex != null) {
        endPunctIndex + 1
    } else {
        source.indexOf('\n', idx + stancePrefix.length)
            .takeIf { it >= 0 } ?: source.length
    }

    val stanceText = source.substring(idx, stanceEndExclusive)
    val after = source.substring(stanceEndExclusive)

    return buildAnnotatedString {
        append(before)

        val start = length
        append(stanceText)
        val end = length

        addStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = stanceColor
            ),
            start,
            end
        )

        append(after)
    }
}

// ───────────────────────────────
// מציאת הסבר מתוך Explanations
// ───────────────────────────────
private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String
): String {
    val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

    fun String.clean(): String = this
        .replace('–', '-')
        .replace('־', '-')
        .replace("  ", " ")
        .trim()

    val candidates = buildList {
        add(rawItem)
        add(display)
        add(display.clean())
        add(display.substringBefore("(").trim().clean())
    }.distinct()

    for (candidate in candidates) {
        val got = Explanations.get(belt, candidate).trim()
        if (got.isNotBlank() &&
            !got.startsWith("הסבר מפורט על") &&
            !got.startsWith("אין כרגע")
        ) {
            return if ("::" in got) got.substringAfter("::").trim() else got
        }
    }

    return "אין כרגע הסבר מפורט לתרגיל הזה במאגר."
}

// ───────────────────────────────
// חיפוש תרגילים מתוך ה-Repo (KmiSearch)
// ───────────────────────────────
private fun searchExercisesForQuestion(
    question: String,
    beltEnum: Belt?
): List<SearchHit> {
    return try {
        val sharedRepo = ContentRepo.asSharedRepo()
        KmiSearch.search(
            repo = sharedRepo,
            query = question,
            belt = beltEnum?.toShared()
        )
    } catch (t: Throwable) {
        Log.e("KMI-AI", "KmiSearch failed", t)
        emptyList()
    }
}

// ───────────────────────────────
// המרת תוצאות חיפוש לרשימת תרגילים קריאה
// ───────────────────────────────
private fun formatHitsAsExerciseList(
    hits: List<SearchHit>,
    maxItems: Int = 6
): String {
    if (hits.isEmpty()) return ""

    return hits.take(maxItems).joinToString("\n") { hit ->
        val appBelt = runCatching { Belt.valueOf(hit.belt.name) }
            .getOrElse { Belt.WHITE }

        val topicTitle = hit.topic
        val rawItem = hit.item ?: ""

        // ✅ FIX: תמיד דרך הפורמטור (מטפל גם "שם::def:*" וגם "def_*::שם")
        val displayName = ExerciseTitleFormatter
            .displayName(rawItem)
            .ifBlank { rawItem }
            .ifBlank { topicTitle }
            .trim()

        "• $displayName (${topicTitle} – חגורה ${appBelt.heb})"
    }
}

private fun normalizeForTts(text: String): String {
    return text
        .replace("ק.מ.י", "קמי")
        .replace("ק מ י", "קמי")
        .replace("K.M.I", "KAMI", ignoreCase = true)
        .replace("K M I", "KAMI", ignoreCase = true)
        .replace("קמי", "קָמִי")
}

private fun buildBestHitExplanation(
    hits: List<SearchHit>,
    preferredBelt: Belt?
): String? {
    val first = hits.firstOrNull() ?: return null

    val appBelt = runCatching { Belt.valueOf(first.belt.name) }
        .getOrElse { preferredBelt ?: Belt.WHITE }

    val topic = first.topic
    val rawItem = first.item ?: return null
    val explanation = findExplanationForHit(appBelt, rawItem, topic)
    val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

    return "ההסבר לתרגיל \"$display\":\n\n$explanation"
}

// ───────────────────────────────
// קומפוזיבל: דיאלוג העוזר החכם
// ───────────────────────────────
@Composable
fun AiAssistantDialog(
    onDismiss: () -> Unit,
    contextLabel: String? = null,
    getExternalDefenses: ((Belt) -> List<String>)? = null,
    getExerciseExplanation: ((String) -> String?)? = null,
    onVoiceCommand: ((VoiceNavCommand) -> Unit)? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current

    // ✅ Focus Sink: גורם ל-TextField לאבד פוקוס באמת בתוך AlertDialog
    val focusSinkRequester = remember { FocusRequester() }
    var focusSinkTick by remember { mutableStateOf(0) }

    LaunchedEffect(focusSinkTick) {
        if (focusSinkTick > 0) {
            runCatching { focusSinkRequester.requestFocus() }
        }
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
        } catch (_: Throwable) { }
    }

    // ✅ FIX: אתחול בטוח של assistantMemory כדי שלא יקרוס ב-sendQuestion
    val spAssistantMemory = remember {
        ctx.getSharedPreferences("kmi_assistant_memory", Context.MODE_PRIVATE)
    }
    val assistantMemoryLocal = remember(spAssistantMemory) {
        AssistantMemory(spAssistantMemory)
    }
    LaunchedEffect(assistantMemoryLocal) {
        assistantMemory = assistantMemoryLocal
    }

    val unansweredPrefs = remember {
        ctx.getSharedPreferences("kmi_ai_unanswered", Context.MODE_PRIVATE)
    }

    // ✅ מצב עוזר נבחר + שמירה לבחירה האחרונה
    val assistantModePrefs = remember {
        ctx.getSharedPreferences("kmi_ai_mode", Context.MODE_PRIVATE)
    }
    var assistantMode by remember {
        mutableStateOf<AssistantMode?>(null) // ✅ בכל פתיחה: אין בחירה מסומנת
    }

    val effectiveMode = assistantMode ?: AssistantMode.EXERCISE

    val emptyStateText = remember(assistantMode) {
        when (effectiveMode) {
            AssistantMode.EXERCISE -> {
                "אני כאן כדי לעזור לך.\n" +
                        "אפשר לבקש הסבר לתרגיל ספציפי (למשל: \"תן הסבר לבעיטת מגל\").\n" +
                        "אפשר גם לבקש רשימת תרגילים לפי חגורה/נושא."
            }

            AssistantMode.TRAININGS -> {
                "אני כאן כדי לעזור לך עם אימונים.\n" +
                        "אפשר לשאול:\n" +
                        "• \"מה האימון הקרוב שלי?\"\n" +
                        "• \"מתי האימון הבא?\"\n" +
                        "• \"תראה לי את האימונים הקרובים\"\n" +
                        "• \"באיזה יום יש לי אימון?\""
            }

            AssistantMode.KMI_MATERIAL -> {
                "מצב חומר ק.מ.י פעיל.\n" +
                        "אפשר לבקש:\n" +
                        "• נושא (למשל \"הגנות חיצוניות\")\n" +
                        "• תת־נושא\n" +
                        "• תרגיל / חיפוש לפי חגורה\n" +
                        "• רשימת תרגילים לפי חגורה/נושא"
            }
        }
    }

    val inputPlaceholder = remember(assistantMode) {
        when (assistantMode) {
            null -> "אנא בחר נושא להמשך"
            AssistantMode.EXERCISE -> "כתוב שם תרגיל או נושא"
            AssistantMode.TRAININGS -> "שאל על אימונים בסניפים השונים"
            AssistantMode.KMI_MATERIAL -> "חפש בחומר (נושא / תרגיל)"
        }
    }

    fun logUnlikeQuestion(question: String, answer: String) {
        val key = "q_${System.currentTimeMillis()}"
        val value = "Q: ${question.trim()}\nA: ${answer.trim()}"
        unansweredPrefs.edit().putString(key, value).apply()
    }

    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<AiMessage>()) }
    var isThinking by remember { mutableStateOf(false) }
    var lastAiAnswer by remember { mutableStateOf<String?>(null) }

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
    var pendingNavAfterSpeak by remember { mutableStateOf<VoiceNavCommand?>(null) }

    // ✅ חובה: אתחול בטוח של ה-TTS Manager גם מתוך הדיאלוג (idempotent)
    // זה מונע מצב שבו לא קראו init() מוקדם מספיק / Activity אחר / תזמון.
    LaunchedEffect(Unit) {
        runCatching { KmiTtsManager.init(ctx.applicationContext) }
            .onFailure { Log.e("KMI_TTS", "KmiTtsManager.init() failed", it) }
    }

    // ✅ =========================================================
    // ✅ STT/TTS — TTS רק דרך KmiTtsManager (אין MediaPlayer/HTTP/Cloud כאן)
    // ✅ =========================================================

    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var pendingStartStt by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // UI בלבד (אנימציית "מדבר…")
    var speakJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun stopSpeaking() {
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
        speakJob = scope.launch {
            val estMs = maxOf(1200L, (ttsText.length * 55L))
            kotlinx.coroutines.delay(estMs)
            isSpeaking = false
            pendingNavAfterSpeak?.let { cmd ->
                pendingNavAfterSpeak = null
                onVoiceCommand?.invoke(cmd)
            }
        }

        Log.d("KMI_TTS", "AiAssistantDialog speakBest() len=${ttsText.length}")

        runCatching { KmiTtsManager.speak(ttsText) }
            .onFailure { Log.e("KMI_TTS", "KmiTtsManager.speak() failed", it) }
    }

    val speak: (String) -> Unit = { text -> speakBest(text) }

    // STT (SpeechRecognizer)
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(ctx) }

    fun stopListeningHard() {
        isListening = false
        runCatching { speechRecognizer.cancel() }
        hideKeyboardHard()
    }

    fun startSpeechToTextInternal() {
        stopSpeaking()
        stopListeningHard()
        hideKeyboardHard()

        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            Toast.makeText(ctx, "זיהוי דיבור לא זמין במכשיר הזה", Toast.LENGTH_SHORT).show()
            return
        }

        isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 250L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 180L)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onEndOfSpeech() { runCatching { speechRecognizer.stopListening() } }

            override fun onError(error: Int) {
                isListening = false
                Log.e("KMI_STT", "SpeechRecognizer error=$error")
            }

            override fun onResults(results: Bundle) {
                isListening = false
                val spoken = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (spoken.isNotBlank()) {
                    input = spoken
                    pendingSendFromStt = spoken
                }
            }
        })

        speechRecognizer.startListening(intent)

        mainHandler.postDelayed({
            if (isListening) {
                isListening = false
                runCatching { speechRecognizer.cancel() }
            }
        }, 7_000L)
    }

    LaunchedEffect(pendingStartStt) {
        if (pendingStartStt) {
            pendingStartStt = false
            startSpeechToTextInternal()
        }
    }

    fun scrollToBottom() {
        scope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun setFeedback(index: Int, fb: Feedback) {
        messages = messages.mapIndexed { i, m ->
            if (i == index) m.copy(feedback = fb) else m
        }
    }

    fun saveAiFeedback(question: String, answer: String?) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val db = Firebase.firestore
            val data = hashMapOf(
                "question" to question,
                "answer" to (answer ?: ""),
                "like" to false,
                "userUid" to uid,
                "createdAt" to Timestamp.now()
            )
            db.collection("aiFeedback").add(data)
        } catch (t: Throwable) {
            Log.e("KMI_AI_FEEDBACK", "Failed to save feedback", t)
        }
    }

    fun setAssistantMode(m: AssistantMode) {
        assistantMode = m
        assistantModePrefs.edit().putString("mode", m.name).apply()

        // reset שיחה
        messages = emptyList()
        lastAiAnswer = null
        isThinking = false
        pendingNavAfterSpeak = null
        input = ""

        stopSpeaking()
        stopListeningHard()
        requestHideKeyboard = true
    }

    fun backToModePicker() {
        stopListeningHard()
        stopSpeaking()
        requestHideKeyboard = true

        assistantMode = null
        messages = emptyList()
        lastAiAnswer = null
        isThinking = false
        pendingNavAfterSpeak = null
        input = ""
    }

    fun sendQuestion(q: String) {
        val question = q.trim()
        if (question.isEmpty()) return

        // ✅ תמיד לסגור STT + מקלדת לפני עיבוד תשובה
        stopListeningHard()
        requestHideKeyboard = true

        // UI: הודעת משתמש
        messages = messages + AiMessage(fromUser = true, text = question)
        input = ""
        isThinking = true
        scrollToBottom()

        val preferredBelt = detectBeltEnum(question)
        val answer: String = try {
            when (assistantMode ?: AssistantMode.EXERCISE) {

                AssistantMode.TRAININGS -> {
                    "רוצה שאבדוק אימונים קרובים? תגיד לי סניף + קבוצה + יום."
                }

                AssistantMode.EXERCISE,
                AssistantMode.KMI_MATERIAL -> {
                    val exp = AssistantExerciseExplanationKnowledge
                        .answer(question, preferredBelt = preferredBelt)
                        ?.trim()

                    if (!exp.isNullOrBlank()) {
                        exp
                    } else {
                        val hits = searchExercisesForQuestion(question, preferredBelt)
                        val best = buildBestHitExplanation(hits, preferredBelt)
                        best ?: run {
                            val list = formatHitsAsExerciseList(hits, maxItems = 6)
                            if (list.isNotBlank()) {
                                "לא מצאתי הסבר מדויק, אבל הנה תרגילים קשורים:\n$list"
                            } else {
                                "לא מצאתי תרגיל מתאים. נסה לכתוב שם תרגיל מדויק יותר או לציין חגורה."
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("KMI-AI", "sendQuestion failed", t)
            "יש תקלה רגעית בעיבוד הבקשה. נסה שוב בעוד רגע."
        }

        val finalAnswer = when (assistantMode ?: AssistantMode.EXERCISE) {
            AssistantMode.TRAININGS -> answer
            else -> answer.trimEnd() + "\n\nאני יכול לעזור לך בעוד משהו?"
        }

        isThinking = false
        messages = messages + AiMessage(
            fromUser = false,
            text = finalAnswer,
            relatedQuestion = question
        )
        lastAiAnswer = finalAnswer
        scrollToBottom()

        // ✅ חשוב: להקריא את ההסבר (finalAnswer), לא את השאלה
        speakBest(finalAnswer)
    }

    // ✅ STT -> Send
    LaunchedEffect(pendingSendFromStt) {
        val q = pendingSendFromStt ?: return@LaunchedEffect
        pendingSendFromStt = null
        sendQuestion(q)
    }

    // ✅ גורם לשדה הקלט להופיע מעל המקלדת (ולא להיבלע)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    AlertDialog(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = {
            stopListeningHard()
            stopSpeaking()
            onDismiss()
        },
        title = null,
        text = {
            var didIntroSpeak by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (!didIntroSpeak) {
                    didIntroSpeak = true
                    speakBest("שלום, כאן יובל העוזר האישי שלך. אנא בחר נושא מתוך הרשימה שלפניך כדי שנוכל להתחיל")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .imePadding()
                    .heightIn(min = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

            Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 3.dp,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.primary
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(900f, 260f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "יובל – העוזר האישי",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = when (assistantMode) {
                                        null -> "בחר מצב כדי להתחיל"
                                        AssistantMode.EXERCISE -> "מצב: מידע / הסבר על תרגיל"
                                        AssistantMode.TRAININGS -> "מצב: מידע על אימונים"
                                        AssistantMode.KMI_MATERIAL -> "מצב: חומר ק.מ.י"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.92f),
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            IconButton(onClick = { backToModePicker() }) {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = "החלף נושא",
                                    tint = Color.White
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.White.copy(alpha = 0.95f), androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                }

                if (assistantMode == null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            @Composable
                            fun ModeButton(
                                title: String,
                                selected: Boolean,
                                onClick: () -> Unit
                            ) {
                                val shape = RoundedCornerShape(14.dp)
                                val outlineColor =
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)

                                Surface(
                                    onClick = onClick,
                                    shape = shape,
                                    tonalElevation = if (selected) 2.dp else 0.dp,
                                    color = if (selected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, outlineColor, shape)
                                ) {
                                    Text(
                                        text = title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            ModeButton(
                                title = "מידע על תרגיל",
                                selected = assistantMode == AssistantMode.EXERCISE,
                                onClick = {
                                    setAssistantMode(AssistantMode.EXERCISE)
                                    stopSpeaking()
                                    pendingNavAfterSpeak = null
                                    speak("אוקיי. אני מוכן להסביר על תרגילים. תשאל אותי שם של תרגיל ואני אגיד את ההסבר שלו.")
                                }
                            )

                            ModeButton(
                                title = "מידע על אימונים",
                                selected = assistantMode == AssistantMode.TRAININGS,
                                onClick = {
                                    setAssistantMode(AssistantMode.TRAININGS)
                                    stopSpeaking()
                                    pendingNavAfterSpeak = null
                                    speak("אוקיי. עכשיו אני מוכן לתת מידע על אימונים. נעבור למסך האימונים.")
                                }
                            )

                            ModeButton(
                                title = "חומר ק.מ.י",
                                selected = assistantMode == AssistantMode.KMI_MATERIAL,
                                onClick = {
                                    setAssistantMode(AssistantMode.KMI_MATERIAL)
                                    stopSpeaking()
                                    pendingNavAfterSpeak = null
                                    speak("מעולה. מצב חומר ק.מ.י פעיל. תגיד נושא או שם תרגיל ואני אחפש לך במאגר.")
                                }
                            )
                        }
                    }
                } else {
                    // (השארת ה-else כמו שהיה אצלך)
                }

                Surface(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp
                ) {
                    if (messages.isEmpty() && !isThinking) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f))
                                .padding(12.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = emptyStateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Right
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f))
                                .padding(10.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            messages.forEachIndexed { index, msg ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (msg.fromUser) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    val bubbleColor =
                                        if (msg.fromUser) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface

                                    val textColor =
                                        if (msg.fromUser) Color.White
                                        else MaterialTheme.colorScheme.onSurface

                                    Surface(
                                        color = bubbleColor,
                                        shape = RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomEnd = if (msg.fromUser) 2.dp else 18.dp,
                                            bottomStart = if (msg.fromUser) 18.dp else 2.dp
                                        ),
                                        tonalElevation = 3.dp,
                                        shadowElevation = 2.dp
                                    ) {
                                        Column {
                                            Text(
                                                text = msg.text,
                                                color = textColor,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                textAlign = TextAlign.Right,
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            if (!msg.fromUser) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(end = 4.dp, bottom = 4.dp),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(onClick = { setFeedback(index, Feedback.LIKE) }) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ThumbUp,
                                                            contentDescription = "Like",
                                                            tint = when (msg.feedback) {
                                                                Feedback.LIKE -> Color(0xFF22C55E)
                                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                            }
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            setFeedback(index, Feedback.UNLIKE)

                                                            val questionText = messages
                                                                .take(index)
                                                                .lastOrNull { it.fromUser }
                                                                ?.text
                                                                ?.trim()
                                                                ?: ""

                                                            if (questionText.isNotBlank()) {
                                                                logUnlikeQuestion(question = questionText, answer = msg.text)
                                                                saveAiFeedback(questionText, msg.text)
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ThumbDown,
                                                            contentDescription = "Unlike",
                                                            tint = when (msg.feedback) {
                                                                Feedback.UNLIKE -> Color(0xFFEF4444)
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
                                Text(
                                    text = "חושב…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    fontSize = 12.sp
                                )
                            }

                            LaunchedEffect(messages.size) {
                                scrollToBottom()
                            }
                        }
                    }
                }

                if (isSpeaking) {
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
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "מדבר…",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.End
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

                val micScale by animateFloatAsState(
                    targetValue = if (isListening) 1.35f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(650, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "micScale"
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                modifier = Modifier.scale(micScale),
                                onClick = {
                                    if (assistantMode == null) {
                                        Toast.makeText(ctx, "בחר מצב כדי להשתמש במיקרופון", Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }

                                    // אם כרגע מאזין/מדבר -> עצור
                                    if (isListening || isSpeaking) {
                                        stopListeningHard()
                                        stopSpeaking()
                                    } else {
                                        // ✅ החזרה של STT בלחיצה
                                        stopSpeaking()
                                        pendingSendFromStt = null
                                        pendingStartStt = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Filled.VolumeOff else Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = if (isListening) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        val inputScrollState = rememberScrollState()

                        TextField(
                            value = input,
                            onValueChange = {
                                input = it
                                scope.launch { inputScrollState.scrollTo(inputScrollState.maxValue) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp, max = 120.dp)
                                .verticalScroll(inputScrollState)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .onFocusEvent { f ->
                                    if (f.isFocused) {
                                        scope.launch { bringIntoViewRequester.bringIntoView() }
                                    }
                                },
                            maxLines = 4,
                            singleLine = false,
                            placeholder = { Text(inputPlaceholder) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    stopListeningHard()
                                    hideKeyboardHard()
                                    requestHideKeyboard = true
                                    sendQuestion(input)
                                }
                            ),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )

                        IconButton(
                            onClick = {
                                stopListeningHard()
                                requestHideKeyboard = true
                                sendQuestion(input)
                            },
                            enabled = assistantMode != null && input.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = null,
                                tint = if (assistantMode != null && input.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { }
    )
} // ✅ סוגר את AiAssistantDialog

// ───────────────────────────────
// זיהוי חגורה מהטקסט
// ───────────────────────────────
private fun detectBeltEnum(text: String): Belt? = when {
    "לבן" in text || "לבנה" in text -> Belt.WHITE
    "צהוב" in text || "צהובה" in text -> Belt.YELLOW
    "כתום" in text || "כתומה" in text -> Belt.ORANGE
    "ירוק" in text || "ירוקה" in text -> Belt.GREEN
    "כחול" in text || "כחולה" in text -> Belt.BLUE
    "חום" in text || "חומה" in text -> Belt.BROWN
    "שחור" in text || "שחורה" in text -> Belt.BLACK
    else -> null
}

// ───────────────────────────────
// השמעת ביפ קצר
// ───────────────────────────────
private fun playBeep(context: Context, resId: Int) {
    try {
        val mp = MediaPlayer.create(context, resId)
        mp.setOnCompletionListener { it.release() }
        mp.start()
    } catch (_: Throwable) { }
}

// ───────────────────────────────
// מנוע לוגיקה — אימונים / לוח אימונים + תרגילים
// ───────────────────────────────
