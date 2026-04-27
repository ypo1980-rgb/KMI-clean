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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import il.kmi.app.ui.DrawerBridge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Dialog
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
import il.kmi.app.domain.Explanations
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.assistant.core.AssistantBrain
import il.kmi.app.ui.assistant.exercise.ExerciseAssistantEngine
import il.kmi.app.ui.assistant.material.MaterialAssistantEngine
import il.kmi.app.ui.assistant.trainings.TrainingsAssistantEngine
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.app.ui.assistant.core.AssistantMemory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//======================================================

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
    if (t.startsWith("יובל") || t.lowercase().startsWith("yuval")) {
        t = t
            .removePrefix("יובל")
            .removePrefix("Yuval")
            .removePrefix("yuval")
            .removePrefix(",")
            .trimStart()
    }

    return when {
        "חזור למסך הבית" in t || "חזור לבית" in t || "מסך הבית" in t ||
                "go home" in t.lowercase() || "open home" in t.lowercase() || "home screen" in t.lowercase() ->
            VoiceNavCommand.OpenHome

        "פתח אימון" in t || "פתח את האימון" in t ||
                "open training" in t.lowercase() || "open workout" in t.lowercase() ->
            VoiceNavCommand.OpenTraining

        "התרגיל הבא" in t || "פתח תרגיל הבא" in t ||
                "next exercise" in t.lowercase() || "open next exercise" in t.lowercase() ->
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

    val normalizedRaw = normalizeExerciseQuery(rawItem)
    val normalizedDisplay = normalizeExerciseQuery(display)

    val candidates = buildList {
        add(rawItem)
        add(display)
        add(display.clean())
        add(display.substringBefore("(").trim().clean())

        add(normalizedRaw)
        add(normalizedDisplay)
        add(normalizedRaw.substringBefore("(").trim())
        add(normalizedDisplay.substringBefore("(").trim())

        // וריאציות קצרות יותר
        if (" " in normalizedRaw) {
            add(normalizedRaw.substringAfterLast(" ").trim())
        }
        if (" " in normalizedDisplay) {
            add(normalizedDisplay.substringAfterLast(" ").trim())
        }
    }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    for (candidate in candidates) {
        val got = Explanations.get(belt, candidate).trim()
        if (
            got.isNotBlank() &&
            !got.startsWith("הסבר מפורט על") &&
            !got.startsWith("אין כרגע")
        ) {
            return if ("::" in got) got.substringAfter("::").trim() else got
        }
    }

    return "אין כרגע הסבר מפורט לתרגיל הזה במאגר."
}

private fun normalizeExerciseQuery(text: String): String {
    return text
        .lowercase()

        // זכר
        .replace("תסביר לי", "")
        .replace("תן הסבר", "")
        .replace("הסבר על", "")
        .replace("איך עושים", "")
        .replace("איך מבצעים", "")

        // נקבה
        .replace("תסבירי לי", "")
        .replace("תסבירי", "")
        .replace("תני הסבר", "")
        .replace("איך תבצעי", "")
        .replace("איך עושים", "")
        .replace("איך מבצעים", "")

        // כללי
        .replace("בבקשה", "")
        .replace("אפשר", "")
        .replace("תרגיל", "")
        .replace("בעברית", "")
        .replace("באנגלית", "")
        .replace("?", "")
        .replace("!", "")
        .replace(",", " ")
        .replace(".", " ")
        .replace("־", "-")
        .replace("–", "-")
        .replace("\\s+".toRegex(), " ")
        .trim()
}

private fun resolveExerciseAlias(raw: String): String {
    val q = normalizeExerciseQuery(raw)

    val aliases = linkedMapOf(
        // עברית
        "מגל" to "בעיטת מגל",
        "בעיטת מגל" to "בעיטת מגל",
        "בעיטת מגל ימנית" to "בעיטת מגל",
        "בעיטת מגל שמאלית" to "בעיטת מגל",

        "סטירה" to "בעיטת סטירה",
        "בעיטת סטירה" to "בעיטת סטירה",
        "בעיטת סטירה חיצונית" to "בעיטת סטירה חיצונית",
        "בעיטת סטירה פנימית" to "בעיטת סטירה פנימית",
        "בעיטת סטירה חיצונית בסיבוב" to "בעיטת סטירה חיצונית בסיבוב",

        "דקירה" to "הגנה נגד דקירה",
        "הגנה נגד דקירה" to "הגנה נגד דקירה",
        "דקירה מזרחית" to "הגנה נגד דקירה מזרחית",
        "דקירה מערבית" to "הגנה נגד דקירה מערבית",
        "דקירת מלפנים" to "הגנה נגד דקירה מלפנים",
        "דקירת מלמטה" to "הגנה נגד דקירה מלמטה",
        "דקירה נמוכה" to "הגנה נגד דקירה נמוכה",

        "roundhouse" to "בעיטת מגל",
        "roundhouse kick" to "בעיטת מגל",

        "outside slap kick" to "בעיטת סטירה חיצונית",
        "slap kick" to "בעיטת סטירה",
        "inside slap kick" to "בעיטת סטירה פנימית",
        "spinning outside slap kick" to "בעיטת סטירה חיצונית בסיבוב",

        "knife defense" to "הגנה נגד דקירה",
        "knife stab defense" to "הגנה נגד דקירה",
        "eastern stab defense" to "הגנה נגד דקירה מזרחית",
        "western stab defense" to "הגנה נגד דקירה מערבית",

        "בעיטה קדמית" to "בעיטה קדמית",
        "קדמית" to "בעיטה קדמית",
        "בעיטת צד" to "בעיטת צד",
        "צד" to "בעיטת צד",
        "אגרוף ישר" to "אגרוף ישר",
        "ישר" to "אגרוף ישר",
        "אפרקאט" to "אפרקאט",
        "וו" to "וו",
        "הוק" to "וו",
        "מרפק" to "מכת מרפק",
        "ברך" to "מכת ברך",

        // אנגלית
        "roundhouse" to "בעיטת מגל",
        "roundhouse kick" to "בעיטת מגל",
        "front kick" to "בעיטה קדמית",
        "side kick" to "בעיטת צד",
        "straight punch" to "אגרוף ישר",
        "jab" to "אגרוף ישר",
        "cross" to "אגרוף ישר",
        "uppercut" to "אפרקאט",
        "hook" to "וו",
        "elbow" to "מכת מרפק",
        "knee" to "מכת ברך"
    )

    aliases[q]?.let { return it }

    val containsMatch = aliases.entries.firstOrNull { (alias, _) ->
        q == alias || q.contains(alias)
    }

    return containsMatch?.value ?: raw.trim()
}

private fun getExerciseAnswerWithFallback(
    question: String,
    preferredBelt: Belt?,
    isEnglish: Boolean
): String {
    val engineAnswer = ExerciseAssistantEngine.answer(
        question = question,
        preferredBelt = preferredBelt,
        isEnglish = isEnglish
    ).trim()

    val normalizedEngineAnswer = engineAnswer.lowercase()

    val looksLikeNotFound =
        "לא מצא" in engineAnswer ||
                "לא נמצא" in engineAnswer ||
                "אין כרגע" in engineAnswer ||
                "couldn't find" in normalizedEngineAnswer ||
                "not found" in normalizedEngineAnswer ||
                "no exercise" in normalizedEngineAnswer

    if (!looksLikeNotFound) return engineAnswer

    val rawExercise = extractExerciseNameFromQuestion(question)?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: question.trim()

    il.kmi.app.domain.ExplanationSearchIndex.findBest(
        query = rawExercise,
        preferredBelt = preferredBelt
    )?.let { match ->
        return match.explanation
    }

    val partialQuery = when (resolveExerciseAlias(rawExercise)) {
        "הגנה נגד דקירה" -> "דקירה"
        else -> rawExercise
    }

    val partialMatches = findPartialExerciseCandidates(
        query = partialQuery,
        isEnglish = isEnglish
    )

    if (partialMatches.size > 1) {
        val cleanExerciseLabel = resolveExerciseAlias(rawExercise)
            .replace("\"", "")
            .trim()

        val intro = if (isEnglish) {
            "I found several exercises for $cleanExerciseLabel:"
        } else {
            "מצאתי מספר תרגילים עבור $cleanExerciseLabel:"
        }

        val outro = if (isEnglish) {
            "Please write the exact exercise name you want and I will explain it."
        } else {
            "כתוב לי את השם המדויק של התרגיל שאתה רוצה ואני אסביר אותו."
        }

        return buildString {
            append(intro)
            append("\n\n")
            partialMatches.forEachIndexed { index, item ->
                append("${index + 1}. $item")
                append("\n")
            }
            append("\n")
            append(outro)
        }.trim()
    }

    val beltsToTry = listOfNotNull(
        preferredBelt,
        Belt.YELLOW,
        Belt.ORANGE,
        Belt.GREEN,
        Belt.BLUE,
        Belt.BROWN,
        Belt.BLACK
    ).distinct()

    for (belt in beltsToTry) {
        val local = findExplanationForHit(
            belt = belt,
            rawItem = rawExercise,
            topic = ""
        ).trim()

        if (
            local.isNotBlank() &&
            !local.startsWith("אין כרגע") &&
            !local.startsWith("הסבר מפורט על")
        ) {
            return local
        }
    }

    return engineAnswer
}

private fun sanitizeTrainingTextForSpeech(text: String, isEnglish: Boolean): String {
    val cleaned = text
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { line ->
            line
                .replace("•", "")
                .replace("(", ". ")
                .replace(")", "")
                .replace(" - ", ". ")
                .replace(" – ", ". ")
                .replace(":", " ")
                .replace("נוער + בוגרים", "נוער ובוגרים")
                .replace("Youth + Adults", "Youth and Adults")
        }
        .joinToString(". ")

    return if (isEnglish) {
        cleaned
            .replace(Regex("\\s+"), " ")
            .trim()
    } else {
        cleaned
            .replace(Regex("[A-Za-z_]{2,}[A-Za-z0-9_().-]*"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

private fun sanitizeAssistantTextForSpeech(text: String, isEnglish: Boolean): String {
    fun isCodeLikeLine(line: String): Boolean {
        val trimmed = line.trim()

        if (trimmed.isBlank()) return true

        if (
            trimmed.startsWith("```") ||
            trimmed.startsWith("import ") ||
            trimmed.startsWith("package ") ||
            trimmed.startsWith("class ") ||
            trimmed.startsWith("fun ") ||
            trimmed.startsWith("val ") ||
            trimmed.startsWith("var ") ||
            trimmed.startsWith("const ") ||
            trimmed.startsWith("@Composable") ||
            trimmed.startsWith("private fun ") ||
            trimmed.startsWith("override fun ")
        ) return true

        if (
            trimmed.contains("->") ||
            trimmed.contains("==") ||
            trimmed.contains(" = ") ||
            trimmed.contains("Icons.") ||
            trimmed.contains("MaterialTheme.") ||
            trimmed.contains("TextField") ||
            trimmed.contains("IconButton") ||
            trimmed.contains("Modifier.") ||
            trimmed.contains("mutableStateOf") ||
            trimmed.contains("remember {") ||
            trimmed.contains("LaunchedEffect(")
        ) return true

        val codeSymbolCount = trimmed.count { it in setOf('{', '}', '(', ')', '=', '<', '>', '@') }
        if (codeSymbolCount >= 4) return true

        return false
    }

    val cleaned = text
        .lineSequence()
        .map { line ->
            line.trim()
                .replace(Regex("""^\d+\.\s*"""), "")
        }
        .filter { it.isNotBlank() }
        .filterNot { line -> isCodeLikeLine(line) }
        .filterNot { line ->
            line.equals("Can I help you with anything else?", ignoreCase = true) ||
                    line.equals("אני יכול לעזור לך בעוד משהו?", ignoreCase = true)
        }
        .joinToString(". ")

    return if (isEnglish) {
        cleaned
            .replace(Regex("""[`"']"""), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    } else {
        cleaned
            .replace(Regex("""[`"']"""), "")
            .replace(Regex("""[A-Za-z_]{2,}[A-Za-z0-9_().]*"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
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
    currentLang: String = ""
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
    val graniteBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF6F2F8),
            Color(0xFFECE4F1),
            Color(0xFFE4DCEB),
            Color(0xFFF5F1F8)
        )
    )

    val premiumCardBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF7B61FF),
            Color(0xFF8B5CF6),
            Color(0xFF5B7CFA)
        )
    )    // ✅ Focus Sink: גורם ל-TextField לאבד פוקוס באמת בתוך AlertDialog
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
        } catch (_: Throwable) { }
    }

    // ✅ FIX: אתחול בטוח של assistantMemory כדי שלא יקרוס ב-sendQuestion
    val spAssistantMemory = remember {
        ctx.getSharedPreferences("kmi_assistant_memory", Context.MODE_PRIVATE)
    }
    val assistantMemoryLocal = remember(spAssistantMemory) {
        AssistantMemory(spAssistantMemory)
    }
    LaunchedEffect(assistantMemoryLocal, spAssistantMemory) {
        assistantMemory = assistantMemoryLocal
        TrainingsAssistantEngine.init(spAssistantMemory)
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
                        "K.M.I material mode is active.\n" +
                                "You can ask for:\n" +
                                "• A topic (for example: \"Outside defenses\")\n" +
                                "• A sub-topic\n" +
                                "• An exercise / search by belt\n" +
                                "• A list of exercises by belt or topic"
                )
            }
        }
    }

    val inputPlaceholder = remember(assistantMode, isEnglish) {
        when (assistantMode) {
            null -> tr("אנא בחר נושא להמשך", "Please choose a topic to continue")
            AssistantMode.EXERCISE -> tr("כתוב או אמור שם תרגיל", "Type or say an exercise name")
            AssistantMode.TRAININGS -> tr("שאל או אמור משהו על אימונים", "Ask or say something about trainings")
            AssistantMode.KMI_MATERIAL -> tr("חפש או אמור נושא / תרגיל", "Search or say a topic / exercise")
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
    var speechStatusMessage by remember { mutableStateOf<String?>(null) }

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
    var currentListeningSessionId by remember { mutableStateOf(0L) }
    var continuousListening by remember { mutableStateOf(false) }

// מאפשר שיחה קולית רציפה כמו ChatGPT
    var autoVoiceConversation by remember { mutableStateOf(true) }

    val dynamicInputPlaceholder = remember(assistantMode, isEnglish, isListening, isSpeaking) {
        when {
            isSpeaking -> tr("לחץ כדי לעצור את הדיבור", "Tap to stop speaking")
            isListening -> tr("אני מקשיב...", "I'm listening...")
            assistantMode == null -> tr("אנא בחר נושא להמשך", "Please choose a topic to continue")
            assistantMode == AssistantMode.EXERCISE -> tr("כתוב או אמור שם תרגיל", "Type or say an exercise name")
            assistantMode == AssistantMode.TRAININGS -> tr("שאל או אמור משהו על אימונים", "Ask or say something about trainings")
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
            // fallback הגנתי בלבד - רק לנקות UI אם משהו נתקע,
            // בלי להפסיק דיבור ובלי להפעיל מיקרופון מחדש
            val fallbackMs = when {
                ttsText.length <= 80 -> 7000L
                ttsText.length <= 180 -> 11000L
                else -> 16000L
            }

            delay(fallbackMs)

            if (isSpeaking) {
                Log.w("KMI_TTS", "speakBest fallback timeout -> clearing speaking UI only")
                isSpeaking = false
                // בכוונה לא נוגעים ב-TTS ולא מפעילים STT מכאן
            }
        }

        Log.d("KMI_TTS", "AiAssistantDialog speakBest() len=${ttsText.length}")

        runCatching { KmiTtsManager.speak(ttsText) }
            .onFailure {
                speakJob?.cancel()
                isSpeaking = false
                KmiTtsManager.setOnSpeechCompletedListener(null)
                Log.e("KMI_TTS", "KmiTtsManager.speak() failed", it)
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
        isListening = false
        runCatching { speechRecognizer?.cancel() }
        hideKeyboardHard()
    }

    fun startSpeechToTextInternal() {

        if (isListening) return

        stopSpeaking()
        hideKeyboardHard()

        if (!hasRecordAudioPermission()) {
            Toast.makeText(
                ctx,
                tr("אין הרשאת מיקרופון. אשר גישה ונסה שוב", "No microphone permission. Please allow access and try again"),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(ctx) || speechRecognizer == null) {
            Toast.makeText(
                ctx,
                tr("זיהוי דיבור לא זמין במכשיר הזה", "Speech recognition is not available on this device"),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        currentListeningSessionId = System.currentTimeMillis()
        isListening = true
        speechStatusMessage = null

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (isEnglish) "en-US" else "he-IL"
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                if (isEnglish) "en-US" else "he-IL"
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

// יותר סבלני להפסקות טבעיות בדיבור
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)

// אפשר להוסיף גם:
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onPartialResults(partialResults: Bundle) {
                val partial = partialResults
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()

                if (!partial.isNullOrBlank()) {
                    input = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onEndOfSpeech() {
                isListening = false
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Throwable) {}
            }

            override fun onError(error: Int) {
                isListening = false
                Log.e("KMI_STT", "SpeechRecognizer error=$error")

                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO ->
                        tr("יש בעיית שמע במיקרופון", "There is an audio issue with the microphone")

                    SpeechRecognizer.ERROR_CLIENT ->
                        ""

                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        tr("חסרה הרשאת מיקרופון", "Microphone permission is missing")

                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        tr("יש בעיית רשת בזיהוי הדיבור", "There is a network issue with speech recognition")

                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        tr("לא זוהה דיבור", "No speech detected")

                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        tr("מנוע הדיבור עסוק כרגע", "Speech recognizer is busy right now")

                    SpeechRecognizer.ERROR_SERVER ->
                        tr("שרת זיהוי הדיבור לא זמין כרגע", "Speech recognition server is currently unavailable")

                    else ->
                        tr("זיהוי הדיבור נכשל", "Speech recognition failed")
                }

                Log.e("KMI_STT", "SpeechRecognizer error=$error message=$message")

                speechStatusMessage = message.takeIf { it.isNotBlank() }

                if (speechStatusMessage != null) {
                    mainHandler.postDelayed({
                        if (speechStatusMessage == message) {
                            speechStatusMessage = null
                        }
                    }, 2200L)
                }
            }

            override fun onResults(results: Bundle) {
                isListening = false
                speechStatusMessage = null

                val spoken = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: return

                val navCommand = parseVoiceNavCommand(spoken)

                if (navCommand != null) {
                    pendingNavAfterSpeak = null
                    onVoiceCommand?.invoke(navCommand)
                    return
                }

                input = spoken
                pendingSendFromStt = spoken
            }
        })

        runCatching {
            speechRecognizer?.startListening(intent)
        }.onFailure {
            Log.e("KMI_STT", "startListening failed", it)
            isListening = false
            Toast.makeText(
                ctx,
                tr("לא ניתן להפעיל את המיקרופון כרגע", "Unable to start microphone right now"),
                Toast.LENGTH_SHORT
            ).show()
        }

        val listeningSessionId = currentListeningSessionId

        mainHandler.postDelayed({
            if (isListening && currentListeningSessionId == listeningSessionId) {
                isListening = false
                runCatching { speechRecognizer?.cancel() }
            }
        }, 12_000L)
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

        val memoryExercise = assistantMemory.getLastExercise()
        val lastQuestion = assistantMemory.getLastQuestion()

        val followUpWords = listOf(
            "אותו",
            "אותה",
            "אותם",
            "אותן",
            "again",
            "it",
            "this exercise"
        )

        val isFollowUp = followUpWords.any {
            question.contains(it, ignoreCase = true)
        }

        val resolvedQuestion =
            when {
                isFollowUp && memoryExercise != null ->
                    "$memoryExercise $question"

                isFollowUp && lastQuestion != null ->
                    "$lastQuestion $question"

                else ->
                    question
            }

        // ✅ תמיד לסגור STT + מקלדת לפני עיבוד תשובה
        stopListeningHard()
        requestHideKeyboard = true

        // UI: הודעת משתמש
        messages = messages + AiMessage(fromUser = true, text = question)

        // שמירת התרגיל האחרון בזיכרון העוזר
        assistantMemory.saveLastQuestion(question)

        extractExerciseNameFromQuestion(question)?.let {
            assistantMemory.saveLastExercise(it)
        }

        input = ""
        isThinking = true
        scrollToBottom()

        val preferredBelt = detectBeltEnum(resolvedQuestion)
        val answer: String = try {
            when (assistantMode) {

                null -> {
                    AssistantBrain.answer(
                        question = resolvedQuestion,
                        preferredBelt = preferredBelt,
                        isEnglish = isEnglish
                    )
                }

                AssistantMode.TRAININGS -> {
                    TrainingsAssistantEngine.answer(
                        question = resolvedQuestion,
                        isEnglish = isEnglish
                    )
                }

                AssistantMode.EXERCISE -> {
                    getExerciseAnswerWithFallback(
                        question = resolvedQuestion,
                        preferredBelt = preferredBelt,
                        isEnglish = isEnglish
                    )
                }

                AssistantMode.KMI_MATERIAL -> {
                    MaterialAssistantEngine.answer(
                        question = resolvedQuestion,
                        preferredBelt = preferredBelt,
                        isEnglish = isEnglish
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e("KMI-AI", "sendQuestion failed", t)
            tr(
                "יש תקלה רגעית בעיבוד הבקשה. נסה שוב בעוד רגע.",
                "There is a temporary issue processing the request. Please try again in a moment."
            )
        }

        val finalAnswer = when (assistantMode) {
            AssistantMode.TRAININGS -> answer
            else -> answer.trimEnd() + "\n\n" + tr("אני יכול לעזור לך בעוד משהו?", "Can I help you with anything else?")
        }

        isThinking = false
        messages = messages + AiMessage(
            fromUser = false,
            text = finalAnswer,
            relatedQuestion = question
        )
        lastAiAnswer = finalAnswer
        scrollToBottom()

        // ✅ חשוב: להקריא רק טקסט נקי שמתאים לדיבור
        val spokenAnswer = if (assistantMode == AssistantMode.TRAININGS) {
            sanitizeTrainingTextForSpeech(
                text = finalAnswer,
                isEnglish = isEnglish
            )
        } else {
            sanitizeAssistantTextForSpeech(
                text = finalAnswer,
                isEnglish = isEnglish
            )
        }

        speakBest(spokenAnswer)
    }

    // ✅ STT -> Send
    LaunchedEffect(pendingSendFromStt) {
        val q = pendingSendFromStt ?: return@LaunchedEffect
        pendingSendFromStt = null
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
                        "שַלוֹם, כאן יובל העוזר האישי שלך. אנא בחר נושא מתוך הרשימה שלפניך כדי שנוכל להתחיל",
                        "Hello, this is Yuval, your personal assistant. Please choose a topic from the list so we can begin."
                    )
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(graniteBrush)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    KmiTopBar(
                        title = if (isEnglish) "Yuval – Personal Assistant" else "יובל – העוזר האישי",
                        currentLang = resolvedLang,
                        showMenu = true,
                        showFontQuick = false,
                        showRoleStatus = true,
                        showSettings = true,
                        showBottomActions = true,
                        showModePill = true,
                        showRoleBadge = true,
                        showTopHome = false,
                        showTopSearch = false,
                        isInsideAssistant = true,
                        useCloseIcon = false,
                        onBack = null,
                    // ✅ בית במסך הזה = סגירת הדיאלוג וחזרה למסך שמאחוריו
                        onHome = {
                            Log.e("KMI_DRAWER", "AI_HOME clicked")
                            stopListeningHard()
                            stopSpeaking()
                            onDismiss()
                            DrawerBridge.openHome()
                        },

                        onSearch = {
                            Log.e("KMI_AI_SEARCH", "AI search opened")
                        },
                        onPickSearchResult = { key ->
                            Log.e("KMI_AI_SEARCH", "AI picked search result key=$key")

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
                            Log.e("KMI_DRAWER", "AI_TOPBAR menu clicked")

                            stopListeningHard()
                            stopSpeaking()

                            if (onOpenDrawer != null) {
                                Log.e("KMI_DRAWER", "AI_TOPBAR using local onOpenDrawer")
                                onOpenDrawer.invoke()
                            } else {
                                Log.e("KMI_DRAWER", "AI_TOPBAR using DrawerBridge fallback")
                                DrawerBridge.open()
                            }
                        },

                        // ✅ לא להציג הודעת חסימה כאן
                        homeDisabledToast = null
                    )

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(22.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(premiumCardBrush, RoundedCornerShape(22.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (assistantMode) {
                                            AssistantMode.EXERCISE -> Icons.Filled.FitnessCenter
                                            AssistantMode.TRAININGS -> Icons.Filled.RecordVoiceOver
                                            AssistantMode.KMI_MATERIAL -> Icons.Filled.MenuBook
                                            null -> Icons.Filled.AutoAwesome
                                        },
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = when (assistantMode) {
                                    null -> tr("בחר מצב כדי להתחיל", "Choose a mode to begin")
                                    AssistantMode.EXERCISE -> tr("מצב: מידע / הסבר על תרגיל", "Mode: Exercise info / explanation")
                                    AssistantMode.TRAININGS -> tr("מצב: מידע על אימונים", "Mode: Training information")
                                    AssistantMode.KMI_MATERIAL -> tr("מצב: חומר ק.מ.י", "Mode: K.M.I material")
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = textAlignPrimary,
                                color = Color.White
                            )

                            IconButton(onClick = { backToModePicker() }) {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = tr("החלף נושא", "Switch topic"),
                                    tint = Color.White
                                )
                            }
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
                                icon: ImageVector,
                                onClick: () -> Unit
                            ) {
                                val shape = RoundedCornerShape(18.dp)
                                val outlineColor =
                                    if (selected) Color(0xFF7B61FF)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

                                Surface(
                                    onClick = onClick,
                                    shape = shape,
                                    tonalElevation = 0.dp,
                                    shadowElevation = if (selected) 8.dp else 3.dp,
                                    color = if (selected) Color.Transparent else Color.White.copy(alpha = 0.68f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, outlineColor, shape)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (selected) premiumCardBrush else Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.82f),
                                                        Color(0xFFF3EDF7).copy(alpha = 0.82f)
                                                    )
                                                ),
                                                shape
                                            )
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                                        ) {
                                            if (isEnglish) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (selected) Color.White.copy(alpha = 0.18f) else Color(0xFFEEE7FB)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = icon,
                                                            contentDescription = null,
                                                            tint = if (selected) Color.White else Color(0xFF7B61FF)
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.width(10.dp))
                                            }

                                            Text(
                                                text = title,
                                                modifier = Modifier.weight(1f),
                                                textAlign = textAlignPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )

                                            if (!isEnglish) {
                                                Spacer(Modifier.width(10.dp))

                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (selected) Color.White.copy(alpha = 0.18f) else Color(0xFFEEE7FB)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = icon,
                                                            contentDescription = null,
                                                            tint = if (selected) Color.White else Color(0xFF7B61FF)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            ModeButton(
                                title = tr("מידע על תרגיל", "Exercise information"),
                                icon = Icons.Filled.FitnessCenter,
                                selected = assistantMode == AssistantMode.EXERCISE,
                                onClick = {
                                    setAssistantMode(AssistantMode.EXERCISE)
                                    stopSpeaking()
                                    pendingNavAfterSpeak = null
                                    speak(
                                        tr(
                                            "אוקיי. אני מוכן להסביר על תרגילים. תשאל אותי שם של תרגיל ואני אגיד את ההסבר שלו.",
                                            "Okay. I'm ready to explain exercises. Ask me for an exercise name and I'll explain it."
                                        )
                                    )
                                }
                            )

                            ModeButton(
                                title = tr("מידע על אימונים", "Training information"),
                                icon = Icons.Filled.RecordVoiceOver,
                                selected = assistantMode == AssistantMode.TRAININGS,
                                onClick = {
                                    setAssistantMode(AssistantMode.TRAININGS)
                                    stopSpeaking()
                                    pendingNavAfterSpeak = null
                                    speak(
                                        tr(
                                            "אוקיי. עכשיו אני מוכן לתת מידע על אימונים. נעבור למסך האימונים.",
                                            "Okay. I'm now ready to provide training information. We will move to the training mode."
                                        )
                                    )
                                }
                            )

                            ModeButton(
                                title = tr("חומר ק.מ.י", "K.M.I material"),
                                icon = Icons.Filled.MenuBook,
                                selected = assistantMode == AssistantMode.KMI_MATERIAL,
                                onClick = {
                                    setAssistantMode(AssistantMode.KMI_MATERIAL)
                                    stopSpeaking()
                                    pendingNavAfterSpeak = null
                                    speak(
                                        tr(
                                            "מעולה. מצב חומר ק.מ.י פעיל. תגיד נושא או שם תרגיל ואני אחפש לך במאגר.",
                                            "Great. K.M.I material mode is active. Say a topic or exercise name and I will search it in the database."
                                        )
                                    )
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
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    color = Color.White.copy(alpha = 0.62f)
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
                                textAlign = textAlignPrimary
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
                                    contentAlignment = when {
                                        msg.fromUser && !isEnglish -> Alignment.CenterEnd
                                        msg.fromUser && isEnglish -> Alignment.CenterStart
                                        !msg.fromUser && !isEnglish -> Alignment.CenterStart
                                        else -> Alignment.CenterEnd
                                    }
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
                                                textAlign = textAlignPrimary,
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
                                val dotsTransition = rememberInfiniteTransition(label = "thinkingDots")

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
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr("יובל חושב", "Yuval is thinking"),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = tr("מדבר…", "Speaking…"),
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

                    val pulseTransition = rememberInfiniteTransition(label = "micPulse")

                    val pulseScale by pulseTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.18f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(650, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    val waveTransition = rememberInfiniteTransition(label = "micWave")

                    val waveScale by waveTransition.animateFloat(
                        initialValue = 0.92f,
                        targetValue = 1.55f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1100, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "waveScale"
                    )

                    val waveAlpha by waveTransition.animateFloat(
                        initialValue = 0.24f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1100),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "waveAlpha"
                    )

                    val micScale by animateFloatAsState(
                        targetValue = when {
                            isSpeaking -> pulseScale
                            isListening -> 1.12f
                            else -> 1f
                        },
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        label = "micScale"
                    )

                    val liveAssistantStatus = when {
                        isSpeaking -> tr("מדבר…", "Speaking…")
                        isThinking -> tr("מעבד…", "Processing…")
                        isListening -> tr("מקשיב…", "Listening…")
                        else -> speechStatusMessage
                    }

                    if (!liveAssistantStatus.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            tonalElevation = 0.dp,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = liveAssistantStatus,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = when {
                                    speechStatusMessage != null -> MaterialTheme.colorScheme.error
                                    isListening -> MaterialTheme.colorScheme.primary
                                    isSpeaking -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = textAlignPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 10.dp,
                        color = Color.White.copy(alpha = 0.86f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                    Row(
                        modifier = Modifier
                            .background(Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier.size(52.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isListening) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .scale(waveScale)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = waveAlpha),
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        when {
                                            isSpeaking -> Color(0x22E53935)
                                            isListening -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(50)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .scale(micScale),
                                    onClick = {
                                        if (isSpeaking) {
                                            stopSpeaking()
                                            return@IconButton
                                        }

                                        if (isListening) {
                                            stopListeningHard()
                                            return@IconButton
                                        }

                                        pendingSendFromStt = null

                                        if (hasRecordAudioPermission()) {
                                            pendingStartStt = true
                                        } else {
                                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isSpeaking -> Icons.Filled.Stop
                                            isListening -> Icons.Filled.Mic
                                            else -> Icons.Filled.Mic
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            isSpeaking -> Color(0xFFE53935)
                                            isListening -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
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
                            placeholder = {
                                Text(
                                    text = dynamicInputPlaceholder,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
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
                                focusedContainerColor = Color.White.copy(alpha = 0.64f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.38f),
                                disabledContainerColor = Color.White.copy(alpha = 0.24f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
            }
        }
    }
} // ✅ סוגר את AiAssistantDialog

private fun extractExerciseNameFromQuestion(question: String): String? {
    val q = question.lowercase().trim()

    val prefixes = listOf(
        "תן הסבר על",
        "תן הסבר ל",
        "תני הסבר על",
        "תני הסבר ל",
        "מה זה",
        "תסבירי לי את",
        "תסבירי לי",
        "תסבירי את",
        "תסבירי",
        "תסביר לי את",
        "תסביר לי",
        "תסביר את",
        "תסביר",
        "איך עושים את",
        "איך עושים",
        "איך מבצעים את",
        "איך מבצעים",
        "איך תבצעי את",
        "איך תבצעי",
        "הסבר על",
        "explain the",
        "explain",
        "how to do the",
        "how to do"
    )

    var cleaned = question.trim()

    prefixes.forEach { prefix ->
        if (q.startsWith(prefix)) {
            cleaned = question.trim().substring(prefix.length).trim()
            return@forEach
        }
    }

    return cleaned
        .removePrefix("את ")
        .removeSuffix("?")
        .trim()
        .takeIf { it.length > 1 }
}

private fun findPartialExerciseCandidates(query: String, isEnglish: Boolean): List<String> {
    val q = query.trim()
        .replace("?", "")
        .replace("הסבר על", "")
        .replace("תן הסבר על", "")
        .replace("תסביר לי", "")
        .replace("תסביר", "")
        .trim()

    if (q.isBlank()) return emptyList()

    val knownExercises = listOf(
        "בעיטת מגל אופקית",
        "בעיטת מגל אלכסונית",
        "בעיטת מגל בהטעיה",
        "בעיטת מגל נמוכה",
        "בעיטת מגל לאחור בשיכול אחורי",
        "בעיטת מגל לאחור בסיבוב",
        "בעיטת מגל בניתור",
        "בעיטת מגל כפולה בניתור",
        "הגנה נגד בעיטת מגל לפנים – בעיטה לצד",
        "הגנה נגד בעיטת מגל נמוכה",
        "הגנה נגד בעיטת מגל לאחור - בעיטה בימין",
        "הגנה נגד בעיטת מגל לאחור - בעיטה שמאל",
        "הגנה נגד בעיטת מגל לאחור - אגרוף שמאל",
        "הגנה נגד בעיטת מגל לאחור בסיבוב – בעיטה",
        "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לצד",
        "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לאחור",
        "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בימין",
        "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בשמאל",
        "הגנה חיצונית נגד בעיטת מגל לפנים - אגרוף בימין",
        "בעיטת סטירה חיצונית",
        "בעיטת סטירה פנימית",
        "בעיטת סטירה חיצונית בסיבוב",
        "הגנה נגד בעיטת סטירה חיצונית",
        "הגנה נגד בעיטת סטירה פנימית",
        "Outside Slap Kick",
        "Inside Slap Kick",
        "Spinning Outside Slap Kick",
        "הגנה נגד דקירה",
        "הגנה נגד דקירה מזרחית",
        "הגנה נגד דקירה מערבית",
        "הגנה נגד דקירה מלפנים",
        "הגנה נגד דקירה מלמטה",
        "הגנה נגד דקירה נמוכה"
    )

    return knownExercises
        .filter { title ->
            title.contains(q, ignoreCase = true) ||
                    q.contains(title, ignoreCase = true)
        }
        .distinct()
        .sorted()
}

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
// מנוע לוגיקה — אימונים / לוח אימונים + תרגילים
// ───────────────────────────────

