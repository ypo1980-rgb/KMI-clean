package il.kmi.app.screens.BeltQuestions.ByTopic

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import il.kmi.app.R
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.ExerciseCountsRegistry
import il.kmi.app.domain.ExerciseCountProvider
import il.kmi.app.domain.SubjectTopic
import il.kmi.app.localization.rememberIsEnglish
import il.kmi.app.ui.FloatingQuickMenu
import il.kmi.app.ui.QuickMenuTriggerMode
import il.kmi.app.ui.KmiTypography
import il.kmi.app.screens.PracticeByTopicsSelection
import il.kmi.app.screens.PracticeMenuDialog
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.ExerciseTitlesEn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import il.kmi.app.subscription.KmiAccess
import il.kmi.app.subscription.AccessMode
import il.kmi.app.subscription.AccessModeResolver
import il.kmi.app.subscription.LockedContentPolicy
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import il.kmi.app.domain.DefenseKind
import il.kmi.app.domain.TopicsBySubjectRegistry
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.LocalAppIconScale
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBrush
import kotlinx.coroutines.yield
import kotlin.math.ceil


//==================================================================

// ✅ FIX: פונקציית נרמול אחת בלבד, ברמת קובץ (נראית לכולם, אין forward-ref ואין אמביגיוטי)
private fun normText(raw: String): String =
    raw
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase()

private fun releasesSectionIdFor(raw: String): String? {
    val t = normText(raw)

    return when (t) {
        "שחרור מתפיסות ידיים / שיער / חולצה" -> "releases_hands_hair_shirt"
        "שחרור מחניקות" -> "releases_chokes"

        "שחרור מחביקות",
        "חביקות גוף",
        "חביקות צואר",
        "חביקות זרוע" -> "releases_hugs"

        else -> null
    }
}

private fun handsSectionIdFor(raw: String): String? {
    val t = normText(raw)

    return when (t) {
        "מכות יד" -> "hands_strikes"
        "מכות מרפק" -> "hands_elbows"
        "מכות במקל / רובה" -> "hands_stick_rifle"
        else -> null
    }
}

private fun subTopicTitleForUi(title: String, isEnglish: Boolean): String {
    val clean = title
        .trim()
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")

    if (!isEnglish) return clean

    val translated = ExerciseTitlesEn.getOrSame(clean).trim()
    if (translated.isNotBlank() && translated != clean) {
        return translated
    }

    return when (clean) {
        "מכות יד" -> "Hand Strikes"
        "מכות מרפק" -> "Elbow Strikes"
        "מכות במקל / רובה" -> "Stick / Rifle Strikes"

        "שחרור מתפיסות ידיים / שיער / חולצה" ->
            "Releases from Hand / Hair / Shirt Grabs"

        "שחרור מחניקות" ->
            "Choke Releases"

        "שחרור מחביקות" ->
            "Hug Releases"

        "חביקות גוף" ->
            "Body Hugs"

        "חביקות צואר" ->
            "Neck Hugs"

        "חביקות זרוע" ->
            "Arm Hugs"

        "הגנות עם רובה נגד דקירות סכין" ->
            "Rifle Defenses Against Knife Stabs"

        "הגנות נגד מספר תוקפים" ->
            "Multiple Attackers Defense"

        else -> translated.ifBlank { clean }
    }
}

private fun subjectTitleForUi(
    subjectId: String,
    fallbackHeb: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return fallbackHeb

    return when (subjectId) {
        "general" -> "General"
        "hands_all" -> "Hand Techniques"
        "hands_root" -> "Hand Techniques"
        "kicks" -> "Kicks"
        "releases" -> "Releases"
        "releases_hugs" -> "Hugs"
        "defense_root" -> "Defenses"
        "knife_defense" -> "Knife Defense"
        "gun_threat_defense" -> "Gun Threat Defense"
        "stick_defense" -> "Stick Defense"
        "hands_strikes" -> "Hand Strikes"
        "hands_elbows" -> "Elbow Strikes"
        "hands_stick_rifle" -> "Stick / Rifle Strikes"

        "rolls_breakfalls" -> "Breakfalls and Rolls"
        "topic_ready_stance" -> "Ready Stance"
        "topic_ground_prep" -> "Groundwork Preparation"
        "topic_kavaler" -> "Kavaler"

        "def_internal_punches" -> "Internal Defenses - Punches"
        "def_internal_kicks" -> "Internal Defenses - Kicks"
        "def_external_punches" -> "External Defenses - Punches"
        "def_external_kicks" -> "External Defenses - Kicks"

        "releases_hands_hair_shirt" -> "Releases from Hand / Hair / Shirt Grabs"
        "releases_chokes" -> "Choke Releases"

        else -> fallbackHeb
    }
}

private fun isDefenseRootSubjectId(subjectId: String): Boolean {
    return when (subjectId.trim().lowercase()) {
        "defense_root",
        "defenses_root",
        "defenses",
        "הגנות" -> true

        else -> false
    }
}

private fun isPremiumRootSubject(subjectId: String): Boolean {
    return when {
        subjectId.trim().lowercase() == "releases" -> true
        isDefenseRootSubjectId(subjectId) -> true
        else -> false
    }
}

private fun defenseCombinedSectionIdFor(kind: DefenseKind): String? {
    val clean = kind.name.lowercase()

    return when {
        clean.contains("internal") || clean.contains("inside") -> "def_internal"
        clean.contains("external") || clean.contains("outside") -> "def_external"
        else -> null
    }
}

private fun withLockSuffix(text: String, locked: Boolean): String {
    return if (locked) "$text 🔒" else text
}

private fun stripLockSuffix(text: String): String {
    return text.removeSuffix(" 🔒").trim()
}

@Composable
private fun PremiumTopicLockIcon(
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "subjectTopicLockPulse")

    val scale by pulse.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subjectTopicLockScale"
    )

    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        tint = Color(0xFFF59E0B),
        modifier = modifier
            .size(
                20.dp * LocalAppIconScale.current
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = 1f
            }
    )
}

private fun hardSubjectLoadingTitle(
    subjectId: String,
    isEnglish: Boolean
): String {
    return if (isEnglish) {
        when (subjectId.trim()) {
            "def_internal" -> "Internal defenses"
            "def_external" -> "External defenses"
            "releases_hugs" -> "Hug releases"
            "kicks_hard" -> "Defenses against kicks"
            else -> "Loading exercises"
        }
    } else {
        when (subjectId.trim()) {
            "def_internal" -> "הגנות פנימיות"
            "def_external" -> "הגנות חיצוניות"
            "releases_hugs" -> "שחרור מחביקות"
            "kicks_hard" -> "הגנות נגד בעיטות"
            else -> "טוען תרגילים"
        }
    }
}

@Composable
private fun HardSubjectLoadingScreen(
    title: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = kmiScreenBackgroundBrush()
            ),
        contentAlignment = Alignment.Center
    ) {
        KmiLoadingRings(
            text = title
        )
    }
}

@Composable
private fun EnsureContentRepoInitialized() {
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            runCatching {
                ContentRepo.initIfNeeded()
            }
        }
    }
}

internal data class SubjectTopicsPdfItem(
    val id: String,
    val title: String,
    val countText: String
)

private fun shareSubjectTopicsPdf(
    context: Context,
    items: List<SubjectTopicsPdfItem>,
    isEnglish: Boolean
) {
    if (items.isEmpty()) return

    val pdfFile = createSubjectTopicsPdf(
        context = context,
        items = items,
        isEnglish = isEnglish
    )

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(
            Intent.EXTRA_SUBJECT,
            if (isEnglish) {
                "KAMI exercises by topic"
            } else {
                "תרגילים לפי נושא - KAMI"
            }
        )
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            sendIntent,
            if (isEnglish) "Share PDF" else "שיתוף PDF"
        )
    )
}

private fun createSubjectTopicsPdf(
    context: Context,
    items: List<SubjectTopicsPdfItem>,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f

    fun tr(he: String, en: String): String =
        if (isEnglish) en else he

    fun exerciseCountFromText(raw: String): Int {
        return Regex("""\d+""")
            .findAll(raw)
            .mapNotNull { it.value.toIntOrNull() }
            .lastOrNull()
            ?: 0
    }

    val document = PdfDocument()

    val lightBlue = android.graphics.Color.rgb(234, 246, 255)
    val softBlue = android.graphics.Color.rgb(244, 250, 255)
    val borderBlue = android.graphics.Color.rgb(191, 213, 232)
    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(80, 100, 120)

    val regularTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    val boldTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regularTypeface,
        align: Paint.Align = Paint.Align.RIGHT
    ): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            this.typeface = typeface
            textAlign = align
        }
    }

    val sectionPaint = paint(
        size = 17f,
        color = android.graphics.Color.rgb(12, 78, 130),
        typeface = boldTypeface
    )

    val itemTitlePaint = paint(
        size = 13f,
        color = textDark,
        typeface = boldTypeface
    )

    val itemCountPaint = paint(
        size = 10f,
        color = android.graphics.Color.rgb(12, 78, 130)
    )

    fun drawRoundRect(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 12f,
        stroke: Boolean = false,
        strokeWidth: Float = 1.2f
    ) {
        val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
            this.strokeWidth = strokeWidth
        }

        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radius,
            radius,
            rectPaint
        )
    }

    fun drawHeader(
        canvas: Canvas
    ) {
        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "תרגילים לפי נושא",
            titleEnglish = "Exercises by Topic",
            subtitleHebrew = "כל נושאי התרגול והספירות",
            subtitleEnglish = "All practice topics and counts"
        )
    }

    fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        totalPages: Int
    ) {
        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = pageNumber,
            totalPages = totalPages,
            isEnglish = isEnglish
        )
    }

    fun drawSummary(
        canvas: Canvas,
        top: Float
    ): Float {
        val totalExercises =
            items.sumOf { exerciseCountFromText(it.countText) }

        drawRoundRect(
            canvas,
            margin,
            top,
            pageWidth - margin,
            top + 74f,
            lightBlue
        )

        drawRoundRect(
            canvas,
            margin,
            top,
            pageWidth - margin,
            top + 74f,
            borderBlue,
            stroke = true
        )

        sectionPaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        canvas.drawText(
            tr("סיכום נושאים", "Topics Summary"),
            if (isEnglish) margin + 20f else pageWidth - margin - 20f,
            top + 27f,
            sectionPaint
        )

        val summaryPaint = paint(
            size = 12f,
            color = textDark,
            typeface = boldTypeface,
            align = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
        )

        canvas.drawText(
            tr(
                "${items.size} נושאים · $totalExercises תרגילים",
                "${items.size} topics · $totalExercises exercises"
            ),
            if (isEnglish) margin + 20f else pageWidth - margin - 20f,
            top + 53f,
            summaryPaint
        )

        return top + 94f
    }

    fun drawTopicCard(
        canvas: Canvas,
        item: SubjectTopicsPdfItem,
        top: Float,
        index: Int
    ): Float {
        val right = pageWidth - margin
        val bottom = top + 58f

        drawRoundRect(
            canvas,
            margin,
            top,
            right,
            bottom,
            if (index % 2 == 0) lightBlue else softBlue
        )

        drawRoundRect(
            canvas,
            margin,
            top,
            right,
            bottom,
            borderBlue,
            stroke = true
        )

        val numberCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(12, 78, 130)
        }

        val circleX =
            if (isEnglish) right - 22f else margin + 22f

        canvas.drawCircle(
            circleX,
            top + 29f,
            11f,
            numberCirclePaint
        )

        val numberPaint = paint(
            size = 10f,
            color = android.graphics.Color.WHITE,
            typeface = boldTypeface,
            align = Paint.Align.CENTER
        )

        canvas.drawText(
            (index + 1).toString(),
            circleX,
            top + 33f,
            numberPaint
        )

        itemTitlePaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        itemCountPaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        val textX =
            if (isEnglish) margin + 20f else right - 20f

        canvas.drawText(
            item.title.take(44),
            textX,
            top + 25f,
            itemTitlePaint
        )

        canvas.drawText(
            item.countText,
            textX,
            top + 44f,
            itemCountPaint
        )

        return bottom + 8f
    }

    val firstPageCapacity = 8
    val nextPageCapacity = 9

    val totalPages =
        if (items.size <= firstPageCapacity) {
            1
        } else {
            1 + ceil(
                (items.size - firstPageCapacity) /
                        nextPageCapacity.toDouble()
            ).toInt()
        }

    var pageNumber = 1
    var itemIndex = 0

    while (pageNumber <= totalPages) {
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()
        )

        val canvas = page.canvas

        drawHeader(canvas)

        var y = KmiPdfHeader.CONTENT_TOP

        if (pageNumber == 1) {
            y = drawSummary(
                canvas = canvas,
                top = y
            )
        } else {
            sectionPaint.textAlign = Paint.Align.CENTER

            canvas.drawText(
                tr("נושאי תרגול", "Practice Topics"),
                pageWidth / 2f,
                y,
                sectionPaint
            )

            y += 28f
        }

        val capacity =
            if (pageNumber == 1) {
                firstPageCapacity
            } else {
                nextPageCapacity
            }

        repeat(capacity) {
            if (itemIndex >= items.size) {
                return@repeat
            }

            y = drawTopicCard(
                canvas = canvas,
                item = items[itemIndex],
                top = y,
                index = itemIndex
            )

            itemIndex++
        }

        drawFooter(
            canvas = canvas,
            pageNumber = pageNumber,
            totalPages = totalPages
        )

        document.finishPage(page)
        pageNumber++
    }

    val directory =
        File(
            context.cacheDir,
            "shared_pdfs"
        )

    check(
        directory.exists() ||
                directory.mkdirs()
    ) {
        "Unable to create PDF sharing directory"
    }

    val fileName =
        if (isEnglish) {
            "Exercises by Topic.pdf"
        } else {
            "תרגילים לפי נושא.pdf"
        }

    val file =
        File(
            directory,
            fileName
        )

    /*
     * שם הקובץ קבוע.
     * FileOutputStream עם append = false
     * מחליף את תוכן הקובץ הקודם.
     */
    FileOutputStream(
        file,
        false
    ).use { output ->
        document.writeTo(output)
    }

    document.close()

    return file
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeltQuestionsByTopicScreen(
    onOpenByBelt: () -> Unit,
    onOpenSubject: (Belt, SubjectTopic) -> Unit,
    @Suppress("UNUSED_PARAMETER")
    onOpenTopic: (Belt, String) -> Unit = { _, _ -> },
    onOpenTopicWithSub: (belt: Belt, topic: String, subTopic: String) -> Unit = { _, _, _ -> },
    onOpenDefenseList: (belt: Belt, kind: String, pick: String) -> Unit = { _, _, _ -> },
    onOpenHardSubjectRoute: (belt: Belt, subjectId: String) -> Unit = { _, _ -> },
    onOpenSubscription: () -> Unit,
    onOpenWeakPoints: (Belt) -> Unit = {},
    onOpenAllLists: (Belt) -> Unit = {},
    onOpenRandomPractice: (Belt) -> Unit = {},
    onOpenRandomPracticeByTopic: (Belt, String) -> Unit = { _, _ -> },
    onOpenFinalExam: (Belt) -> Unit = {},
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit = {},
    onOpenSummaryScreen: (Belt) -> Unit = {},
    onOpenVoiceAssistant: (Belt) -> Unit = {},
    onOpenPdfMaterials: (Belt) -> Unit = {}
) {
    EnsureContentRepoInitialized()
    val isEnglish = rememberIsEnglish()
    val ctx = LocalContext.current

    var subjectTopicsPdfItems by remember {
        mutableStateOf<List<SubjectTopicsPdfItem>>(emptyList())
    }

    // הערות תרגילים מנוהלות עכשיו בדיאלוג הגלובלי החדש דרך KmiTopBar

    val userSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    val subsSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_subs", Context.MODE_PRIVATE)
    }

    val legacySp = remember(ctx) {
        ctx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE)
    }

    var accessRefreshTick by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ מרענן גישה מיד כשחוזרים ממסך רכישה / מנוי
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessRefreshTick++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // מצב הגישה מתרענן דרך SharedPreferences listener
    // וגם דרך ON_RESUME כשחוזרים ממסך רכישה / מנוי.

    DisposableEffect(userSp, subsSp, legacySp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "has_full_access" ||
                key == "full_access" ||
                key == "subscription_active" ||
                key == "is_subscribed" ||
                key == "google_subscription_verified" ||
                key == "google_subscription_checked_at" ||
                key == "sub_product" ||
                key == "sub_access_until" ||
                key == "access_changed_at"
            ) {
                accessRefreshTick++
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)
        subsSp.registerOnSharedPreferenceChangeListener(listener)
        legacySp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
            subsSp.unregisterOnSharedPreferenceChangeListener(listener)
            legacySp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val hasManagerAccess = remember(accessRefreshTick) {
        KmiAccess.hasFullAccess(userSp) ||
                KmiAccess.hasFullAccess(subsSp) ||
                KmiAccess.hasFullAccess(legacySp)
    }

    val accessMode = AccessModeResolver.resolve(
        hasManagerAccess = hasManagerAccess
    )

    val hasAccess = accessMode == AccessMode.OPEN

    var pendingHardSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var localHardSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var localHardSectionId by rememberSaveable { mutableStateOf<String?>(null) }

    pendingHardSubjectId?.let { pendingSubjectId ->
        HardSubjectLoadingScreen(
            title = hardSubjectLoadingTitle(pendingSubjectId, isEnglish)
        )

        LaunchedEffect(pendingSubjectId) {
            yield()
            localHardSectionId = null
            localHardSubjectId = pendingSubjectId
            pendingHardSubjectId = null
        }

        return
    }

    localHardSubjectId?.let { subjectId ->
        UnifiedSubjectExercisesScreen(
            subjectId = subjectId,
            sectionId = localHardSectionId,
            onOpenSection = { nextSubjectId, nextSectionId ->
                localHardSubjectId = nextSubjectId
                localHardSectionId = nextSectionId
            },
            onBack = {
                if (localHardSectionId != null) {
                    localHardSectionId = null
                } else {
                    localHardSubjectId = null
                }
            }
        )
        return
    }

    // State לניהול התפריט המהיר
    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showPracticeMenu by rememberSaveable { mutableStateOf(false) }
    var effectiveBelt by rememberSaveable { mutableStateOf(Belt.GREEN) }
    // החיפוש והסברי התרגילים עוברים דרך KmiTopBar + ExercisePremiumSearchDialog

    if (showPracticeMenu) {
        PracticeMenuDialog(
            canUseExtras = hasAccess,
            defaultBelt = effectiveBelt,
            onDismiss = { showPracticeMenu = false },
            onRandomPractice = { beltArg ->
                showPracticeMenu = false
                onOpenRandomPractice(beltArg)
            },
            onFinalExam = { beltArg ->
                showPracticeMenu = false
                onOpenFinalExam(beltArg)
            },
            onPracticeByTopics = { selection ->
                showPracticeMenu = false
                onPracticeByTopics(selection)
            },
            onPracticeByTopicSelected = { beltArg, topicArg ->
                showPracticeMenu = false
                onOpenRandomPracticeByTopic(beltArg, topicArg)
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            KmiTopBar(
                title = if (isEnglish) "Exercises by Topic" else "תרגילים לפי נושא",
                onHome = { },
                lockHome = false,
                showTopHome = false,
                showTopBeltIcon = false,
                topBeltIconRes = null,
                // החיפוש הגלובלי נפתח ומטופל פנימית בתוך KmiTopBar
                lockSearch = false,
                showTopShare = true,
                showBottomActions = true,
                centerTitle = true,
                onShare = {
                    shareSubjectTopicsPdf(
                        context = ctx,
                        items = subjectTopicsPdfItems,
                        isEnglish = isEnglish
                    )
                }
            )
        },
        bottomBar = {}
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
        ) {

            // 1. תוכן המסך (נגלל)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopicQuestionsModeSwitcher(
                    onOpenByBelt = onOpenByBelt
                )

                Spacer(Modifier.height(4.dp))

                TopicsBySubjectCard(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    currentBelt = effectiveBelt,
                    accessMode = accessMode,
                    hasAccess = hasAccess,
                    onOpenSubscription = {
                        onOpenSubscription()
                    },
                    onSubjectClick = { belt, subject ->
                        effectiveBelt = belt
                        onOpenSubject(belt, subject)
                    },
                    onOpenTopicWithSub = onOpenTopicWithSub,
                    onOpenDefenseList = onOpenDefenseList,
                    onOpenHardSubjectRoute = { belt, subjectId ->
                        effectiveBelt = belt

                        when (val cleanSubjectId = subjectId.trim()) {
                            "def_internal",
                            "def_external",
                            "releases_hugs",
                            "kicks_hard" -> {
                                pendingHardSubjectId = cleanSubjectId
                                localHardSubjectId = null
                                localHardSectionId = null
                            }

                            else -> {
                                onOpenHardSubjectRoute(belt, cleanSubjectId)
                            }
                        }
                    },
                    onOpenKicksHardLocal = {
                        effectiveBelt = Belt.GREEN
                        pendingHardSubjectId = "kicks_hard"
                        localHardSubjectId = null
                        localHardSectionId = null
                    },
                    onPdfItemsChanged = { items ->
                        subjectTopicsPdfItems = items
                    }
                )
            }

            FloatingQuickMenu(
                belt = effectiveBelt,
                modifier = Modifier
                    // ✅ אותו מלבן צדדי כמו במסך תרגילים לפי חגורה
                    .align(Alignment.CenterStart)
                    .zIndex(999f),
                expanded = quickMenuExpanded,
                onExpandedChange = { quickMenuExpanded = it },
                triggerMode = QuickMenuTriggerMode.SideRail,
                includePractice = true,
                hasFullAccess = hasAccess,
                onLockedItemClick = { onOpenSubscription() },
                onWeakPoints = { onOpenWeakPoints(effectiveBelt) },
                onAllLists = { onOpenAllLists(effectiveBelt) },
                onPractice = { showPracticeMenu = true },
                onSummary = { onOpenSummaryScreen(effectiveBelt) },
                onVoice = { onOpenVoiceAssistant(effectiveBelt) },
                onPdf = { onOpenPdfMaterials(effectiveBelt) }
            )

            // החץ הוסר: כרטיס הנושאים נפרס לגובה מלא כדי להציג את כל הנושאים
        }

        // אין כאן יותר דיאלוג חיפוש/הסבר מקומי.
        // כל החיפוש, ההסבר, המועדפים והערות המשתמש מטופלים דרך KmiTopBar.
    }
}

@Composable
private fun TopicQuestionsModeSwitcher(
    onOpenByBelt: () -> Unit
) {
    val isEnglish = rememberIsEnglish()

    val tabs = listOf(
        TopicQuestionsMode.BY_TOPIC,
        TopicQuestionsMode.BY_BELT
    )

    val selectedIndex = tabs.indexOf(
        TopicQuestionsMode.BY_TOPIC
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.34f)
        ),
        shape = RectangleShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(
                    brush = kmiSectionHeaderBrush()
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-4).dp)
                    .width(1.dp)
                    .height(24.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.65f)
                    )
            )

            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = {},
                    indicator = { positions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(
                                    positions[selectedIndex]
                                )
                                .padding(horizontal = 58.dp)
                                .offset(
                                    x =
                                        if (selectedIndex == 0) {
                                            19.dp
                                        } else {
                                            (-19).dp
                                        },
                                    y = (-4).dp
                                ),
                            height = 3.dp,
                            color = Color.White
                        )
                    },
                    modifier = Modifier.matchParentSize()
                ) {
                    tabs.forEach { mode ->
                        val label =
                            when (mode) {
                                TopicQuestionsMode.BY_TOPIC ->
                                    if (isEnglish) {
                                        "By Topic"
                                    } else {
                                        "לפי נושא"
                                    }

                                TopicQuestionsMode.BY_BELT ->
                                    if (isEnglish) {
                                        "By Belt"
                                    } else {
                                        "לפי חגורה"
                                    }
                            }

                        Tab(
                            selected =
                                mode == TopicQuestionsMode.BY_TOPIC,
                            onClick = {
                                if (
                                    mode ==
                                    TopicQuestionsMode.BY_BELT
                                ) {
                                    onOpenByBelt()
                                }
                            },
                            text = {
                                Text(
                                    text = label,
                                    style = KmiTypography.action,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = if (mode == tabs.first()) 38.dp else 0.dp,
                                            end = if (mode == tabs.last()) 38.dp else 0.dp
                                        )
                                )
                            },
                            selectedContentColor = Color.White,
                            unselectedContentColor =
                                Color.White.copy(alpha = 0.82f)
                        )
                    }
                }
            }
        }
    }
}

private enum class TopicQuestionsMode {
    BY_TOPIC,
    BY_BELT
}

private fun subjectAccentColor(subjectId: String): Color =
    when (subjectId.trim().lowercase()) {
        "defense_root",
        "defenses_root",
        "defenses" -> Color(0xFF7C5CFF)          // violet premium

        "releases" -> Color(0xFF19A7E0)          // aqua premium

        "hands_root",
        "hands_all" -> Color(0xFF16B39A)         // emerald premium

        "rolls_breakfalls",
        "topic_breakfalls_rolls" -> Color(0xFFB88746) // bronze premium

        "topic_ready_stance" -> Color(0xFF59B96F) // green premium

        "topic_ground_prep" -> Color(0xFFD05AA0) // pink premium

        "topic_kavaler",
        "kavaler" -> Color(0xFFF09A2A)           // amber premium

        "kicks",
        "topic_kicks" -> Color(0xFF3B82F6)       // blue premium

        "releases_hugs" -> Color(0xFF5B6CFF)

        else -> Color(0xFF7C5CFF)
    }

private fun subjectPremiumBackgroundColors(subjectId: String): List<Color> =
    when (subjectId.trim().lowercase()) {
        "defense_root",
        "defenses_root",
        "defenses" -> listOf(
            Color(0xFFF3EEFF),
            Color(0xFFE9E0FF)
        )

        "releases" -> listOf(
            Color(0xFFEAF7FF),
            Color(0xFFDCEFFF)
        )

        "hands_root",
        "hands_all" -> listOf(
            Color(0xFFEAF9F5),
            Color(0xFFDDF4ED)
        )

        "rolls_breakfalls",
        "topic_breakfalls_rolls" -> listOf(
            Color(0xFFF8F1E7),
            Color(0xFFF0E4D2)
        )

        "topic_ready_stance" -> listOf(
            Color(0xFFEEF9EE),
            Color(0xFFE1F3E2)
        )

        "topic_ground_prep" -> listOf(
            Color(0xFFF9EDF4),
            Color(0xFFF2DFEA)
        )

        "topic_kavaler",
        "kavaler" -> listOf(
            Color(0xFFFFF3E5),
            Color(0xFFFFE6C8)
        )

        "kicks",
        "topic_kicks" -> listOf(
            Color(0xFFEEF5FF),
            Color(0xFFDDEBFF)
        )

        "releases_hugs" -> listOf(
            Color(0xFFEEF0FF),
            Color(0xFFE2E6FF)
        )

        else -> listOf(
            Color(0xFFF4F0FF),
            Color(0xFFE9E3FF)
        )
    }

private fun subjectIconFor(subjectId: String): ImageVector =
    when (subjectId) {
        "defense_root" -> Icons.Filled.Security
        "hands_root" -> Icons.Filled.PanTool
        "releases" -> Icons.Filled.PanTool
        "releases_hugs" -> Icons.Filled.PanTool
        "rolls_breakfalls" -> Icons.Filled.Build
        "topic_ready_stance" -> Icons.Filled.CheckCircle
        "topic_ground_prep" -> Icons.Filled.Info
        "topic_kavaler" -> Icons.Filled.Build
        else -> Icons.Filled.Info
    }

private fun subjectImageFor(subjectId: String): Int? =
    when (subjectId.trim().lowercase()) {
        // הגנות
        "defense_root",
        "defenses_root",
        "defenses" -> R.drawable.topic_defenses

        // שחרורים / חביקות גוף
        "releases",
        "releases_hugs",
        "releases_hugs_body" -> R.drawable.topic_body_hug_releases

        // עבודות ידיים
        "hands_root",
        "hands_all",
        "hands_strikes" -> R.drawable.topic_hand_strikes

        // בלימות וגלגולים
        "rolls_breakfalls",
        "topic_breakfalls_rolls" -> R.drawable.topic_forward_roll

        // עמידת מוצא
        "topic_ready_stance" -> R.drawable.topic_ready_stance

        // עבודת קרקע
        "topic_ground_prep" -> R.drawable.topic_ground_fighting

        // קוואלר
        "topic_kavaler",
        "kavaler" -> R.drawable.topic_kavaler

        // בעיטות
        "kicks",
        "topic_kicks" -> R.drawable.topic_kicks

        else -> null
    }

@Composable
private fun InlineSubTopicsExpansionCard(
    picks: List<String>,
    counts: Map<String, Int>,
    isEnglish: Boolean,
    accent: Color,
    onPick: (String) -> Unit
) {
    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val itemTitleColor =
        MaterialTheme.colorScheme.onSurface

    val itemSecondaryColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            accent
        }

    val itemArrowColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            accent
        }

    fun countLabel(n: Int): String =
        if (isEnglish) "exercises $n" else "$n תרגילים"

    fun countForDisplay(pick: String): Int {
        counts[pick]?.let { return it }

        val clean = stripLockSuffix(pick).trim()

        return counts[clean]
            ?: when (clean) {
                // שחרורים
                "Release from Hand / Hair / Shirt Grabs" ->
                    counts["שחרור מתפיסות ידיים / שיער / חולצה"] ?: 0

                "Choke Releases" ->
                    counts["שחרור מחניקות"] ?: 0

                "Bear Hug Releases",
                "Hug Releases" ->
                    counts["שחרור מחביקות"] ?: 0

                // עבודת ידיים
                "Hand Strikes" ->
                    counts["מכות יד"] ?: 0

                "Elbow Strikes" ->
                    counts["מכות מרפק"] ?: 0

                "Stick / Rifle Strikes" ->
                    counts["מכות במקל / רובה"] ?: 0

                else -> 0
            }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.20f),
                            accent.copy(alpha = 0.10f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            picks.forEachIndexed { index, pick ->
                val cleanTitle = stripLockSuffix(pick)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPick(pick) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = itemArrowColor,
                        modifier =
                            Modifier.size(
                                15.dp * LocalAppIconScale.current
                            )
                    )

                    Spacer(Modifier.width(6.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                    ) {
                        Text(
                            text = cleanTitle,
                            style = KmiTypography.cardTitle,
                            color = itemTitleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Start
                                } else {
                                    TextAlign.Right
                                },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(1.dp))

                        Text(
                            text = countLabel(
                                countForDisplay(pick)
                            ),
                            style = KmiTypography.caption.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = itemSecondaryColor,
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Start
                                } else {
                                    TextAlign.Right
                                },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
                    }
                }

                if (index != picks.lastIndex) {
                    HorizontalDivider(
                        color = accent.copy(alpha = 0.22f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectRootCardPremium(
    title: String,
    subtitle: String = "",
    subjectId: String,
    countText: String,
    showLeftBadge: Boolean = false,
    isDarkMode: Boolean = false,
    showExpandArrow: Boolean = false,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    val isEnglish = rememberIsEnglish()

    val accent =
        remember(subjectId) {
            subjectAccentColor(subjectId)
        }

    val icon =
        remember(subjectId) {
            subjectIconFor(subjectId)
        }

    val imageRes =
        remember(subjectId) {
            subjectImageFor(subjectId)
        }

    val lightBackgroundColors =
        remember(subjectId) {
            subjectPremiumBackgroundColors(subjectId)
        }

    /*
     * במצב כהה הכרטיס נטמע בתוך המשטח הכהה של רשימת
     * הנושאים. לכן כל הטקסטים מקבלים צבעים בהירים.
     *
     * במצב בהיר נשמרים צבעי הכרטיסים המקוריים.
     */
    val titleColor =
        MaterialTheme.colorScheme.onSurface

    val subtitleColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    val countColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    /*
     * החצים והפסים הצבעוניים חייבים להישאר גלויים גם
     * כאשר צבע הנושא המקורי כהה.
     */
    val visualAccent =
        if (isDarkMode) {
            Color.White.copy(alpha = 0.82f)
        } else {
            accent
        }

    val bgColors =
        if (isDarkMode) {
            listOf(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainer
            )
        } else {
            lightBackgroundColors
        }

    val isTitleLocked = title.endsWith(" 🔒")
    val displayTitle = stripLockSuffix(title)

    /*
     * כל כרטיסי הנושאים משתמשים כעת באותם סגנונות.
     * כותרות ארוכות מקבלות עד שתי שורות במקום גודל כתב מוקטן.
     */
    val titleMaxLines = 2

    @Composable
    fun SubjectVisual() {
        if (imageRes != null) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(31.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Box(
                modifier =
                    Modifier.size(
                        24.dp * LocalAppIconScale.current
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier =
                        Modifier.size(
                            16.dp * LocalAppIconScale.current
                        )
                )
            }
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(bgColors),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(
                                    when {
                                        imageRes != null -> 44.dp
                                        showLeftBadge -> 34.dp
                                        else -> 26.dp
                                    }
                                )
                                .background(
                                    color = accent,
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(10.dp))
                        SubjectVisual()
                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = displayTitle,
                                    style = KmiTypography.cardTitle,
                                    textAlign = TextAlign.Start,
                                    color = titleColor,
                                    maxLines = titleMaxLines,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isTitleLocked) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    PremiumTopicLockIcon(
                                        modifier =
                                            Modifier.size(
                                                15.dp * LocalAppIconScale.current
                                            )
                                    )
                                }

                                if (showExpandArrow) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text =
                                            if (isExpanded) {
                                                "⌃"
                                            } else {
                                                "⌄"
                                            },
                                        style = KmiTypography.caption.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = visualAccent
                                    )
                                }
                            }

                            if (subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = KmiTypography.secondary.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    textAlign = TextAlign.Start,
                                    color = subtitleColor,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = countText,
                                style = KmiTypography.caption.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                textAlign = TextAlign.Start,
                                color = countColor,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.width(34.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier.width(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showExpandArrow) {
                                    Text(
                                        text = if (isExpanded) "⌃" else "⌄",
                                        style = KmiTypography.caption.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = accent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier.width(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTitleLocked) {
                                    PremiumTopicLockIcon(
                                        modifier =
                                            Modifier.size(
                                                15.dp * LocalAppIconScale.current
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = displayTitle,
                                style = KmiTypography.cardTitle,
                                textAlign = TextAlign.Right,
                                color = titleColor,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = titleMaxLines,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = KmiTypography.secondary.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    textAlign = TextAlign.Right,
                                    color = subtitleColor,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = countText,
                                style = KmiTypography.caption.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                textAlign = TextAlign.Right,
                                color = countColor,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(5.dp))
                        SubjectVisual()
                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(
                                    when {
                                        imageRes != null -> 44.dp
                                        showLeftBadge -> 34.dp
                                        else -> 26.dp
                                    }
                                )
                                .background(
                                    color = accent,
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TopicsBySubjectCard(
    modifier: Modifier = Modifier,
    currentBelt: Belt,
    accessMode: AccessMode,
    hasAccess: Boolean = true,
    onOpenSubscription: () -> Unit,
    onSubjectClick: (Belt, SubjectTopic) -> Unit = { _, _ -> },
    onOpenTopicWithSub: (belt: Belt, topic: String, subTopic: String) -> Unit = { _, _, _ -> },
    onOpenDefenseList: (belt: Belt, kind: String, pick: String) -> Unit = { _, _, _ -> },
    onOpenHardSubjectRoute: (belt: Belt, subjectId: String) -> Unit,
    onOpenKicksHardLocal: () -> Unit,
    onPdfItemsChanged: (List<SubjectTopicsPdfItem>) -> Unit = {}
) {

    val isEnglish = rememberIsEnglish()

    // בודקים את ה-Theme של האפליקציה בפועל, לא את מצב המכשיר.
    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val subjects = TopicsBySubjectRegistry.allSubjects()

// ✅ DEFENSE counts – source of truth מ-ExerciseCountsRegistry
    val defenseDialogCountsMap = remember {
        ExerciseCountsRegistry.defenseDialogCounts()
    }

    val defensePickCountsMap = remember {
        ExerciseCountsRegistry.defensePickCounts()
    }

    val totalDefense = remember {
        ExerciseCountsRegistry.totalDefenseCount()
    }

    val handsBase: SubjectTopic? = remember(subjects) {
        subjects.firstOrNull { it.id == "hands_all" }
    }

    var subjectCounts by remember(subjects) {
        mutableStateOf<Map<String, Int>>(emptyMap())
    }
    var handsRootCount by remember(subjects) {
        mutableIntStateOf(0)
    }
    var countsReady by remember(subjects) {
        mutableStateOf(false)
    }
    var handsPickCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var uiSectionCounts by remember(subjects) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var expandedSubTopicsForId by rememberSaveable { mutableStateOf<String?>(null) }

    var subTopicsPickCountsBySubjectId by remember(subjects) {
        mutableStateOf<Map<String, Map<String, Int>>>(emptyMap())
    }

    var askDefense by rememberSaveable { mutableStateOf(false) }
    var askKind by rememberSaveable { mutableStateOf<DefenseKind?>(null) }

    fun applyPayload(payload: SubjectTopicsUiLogic.TopicsUiCountsPayload) {
        subjectCounts = payload.subjectCounts
        handsRootCount = payload.handsRootCount
        handsPickCounts = payload.handsPickCounts
        uiSectionCounts = payload.uiSectionCounts
        subTopicsPickCountsBySubjectId = payload.subTopicsPickCountsBySubjectId
        countsReady = true
    }

    LaunchedEffect(subjects, handsBase, currentBelt) {
        // ✅ אם כבר יש cache בזיכרון לחגורה הנוכחית — משתמשים בו מיידית
        SubjectTopicsUiLogic.getCachedTopicsUiCountsPayload(currentBelt)?.let { cached ->
            applyPayload(cached)
            return@LaunchedEffect
        }

        val payload = withContext(Dispatchers.Default) {
            SubjectTopicsUiLogic.buildTopicsUiCountsPayload(
                subjects = subjects,
                handsBase = handsBase,
                currentBelt = currentBelt
            )
        }

        SubjectTopicsUiLogic.cacheTopicsUiCountsPayload(
            currentBelt = currentBelt,
            value = payload
        )

        applyPayload(payload)
    }

    // ----------------- UI -----------------

    fun formatCount(n: Int): String =
        if (isEnglish) "exercises $n" else "$n תרגילים"

    fun uniqueExerciseCountForTopics(
        vararg topicTitles: String
    ): Int {
        return topicTitles
            .asSequence()
            .map { topicTitle ->
                ExerciseCountProvider.topicStats(
                    belt = currentBelt,
                    topicTitle = topicTitle
                ).exerciseCount
            }
            .firstOrNull { count -> count > 0 }
            ?: 0
    }

    fun formatSubTopicsAndExercises(subTopics: Int, exercises: Int): String =
        if (isEnglish) {
            "$subTopics sub-topics · $exercises exercises"
        } else {
            "\u200F$subTopics\u00A0תתי נושאים · $exercises\u00A0תרגילים\u200F"
        }

    fun translateCardCountText(raw: String): String {
        if (!isEnglish) return raw

        return raw
            .replace(Regex("""(\d+)\s+תתי\s+נושאים"""), "sub-topics $1")
            .replace(Regex("""(\d+)\s+תרגילים"""), "exercises $1")
    }

    fun countTextFromSubTopicTotalsOrFallback(
        counts: Map<String, Int>,
        fallback: String,
        subTopicsCount: Int
    ): String {
        val totalExercises = counts
            .filterKeys { it.trim().isNotBlank() }
            .filterValues { it > 0 }
            .values
            .sum()

        if (totalExercises > 0) {
            return if (subTopicsCount > 0) {
                formatSubTopicsAndExercises(subTopicsCount, totalExercises)
            } else {
                formatCount(totalExercises)
            }
        }

        val fallbackExerciseCount = Regex("""\d+""")
            .findAll(fallback)
            .mapNotNull { it.value.toIntOrNull() }
            .lastOrNull()
            ?: 0

        return if (subTopicsCount > 0 && fallbackExerciseCount > 0) {
            formatSubTopicsAndExercises(subTopicsCount, fallbackExerciseCount)
        } else {
            formatCount(fallbackExerciseCount)
        }
    }

    fun countTextForSubjectCard(
        card: SubjectTopicsUiLogic.SubjectCardModel
    ): String {
        val uniqueCount = when (card.id.trim().lowercase()) {
            "rolls_breakfalls",
            "topic_breakfalls_rolls" -> {
                uniqueExerciseCountForTopics(
                    "בלימות וגלגולים",
                    "גלגולים ובלימות"
                )
            }

            "topic_ready_stance" -> {
                uniqueExerciseCountForTopics(
                    "עמידת מוצא"
                )
            }

            "topic_ground_prep" -> {
                uniqueExerciseCountForTopics(
                    "עבודת קרקע"
                )
            }

            "topic_kavaler",
            "kavaler" -> {
                uniqueExerciseCountForTopics(
                    "קוואלר"
                )
            }

            "kicks",
            "topic_kicks" -> {
                uniqueExerciseCountForTopics(
                    "בעיטות",
                    "topic_kicks"
                )
            }

            else -> 0
        }

        if (uniqueCount > 0) {
            return formatCount(uniqueCount)
        }

        return translateCardCountText(
            card.countText
        )
    }

    fun openSubjectSmart(subject: SubjectTopic) {
        val action = SubjectTopicsUiLogic.buildOpenSubjectUiAction(
            subject = subject,
            currentBelt = currentBelt
        )

        onSubjectClick(action.chosenBelt, subject)
    }

    fun openPickedSubTopic(
        id: String,
        pickedDisplay: String
    ) {
        val dialogData = SubjectTopicsUiLogic.buildSubTopicsDialogData(
            subjects = subjects,
            id = id
        )

        val pickedDisplayClean = stripLockSuffix(pickedDisplay)

        val picked = dialogData.picks.firstOrNull {
            subTopicTitleForUi(it, isEnglish) == pickedDisplayClean
        } ?: pickedDisplayClean

        val isLockedPick =
            accessMode != AccessMode.OPEN &&
                    LockedContentPolicy.shouldShowLock(
                        accessMode,
                        dialogData.base?.titleHeb.orEmpty()
                    )

        if (isLockedPick) {
            onOpenSubscription()
            return
        }

        val decision = SubjectTopicsUiLogic.resolveSubTopicPick(
            base = dialogData.base,
            bodyHugsChild = dialogData.bodyHugsChild,
            picked = picked,
            norm = ::normText
        )

        when (decision) {
            is SubjectTopicsUiLogic.SubTopicPickDecision.OpenTopicWithSub -> {
                onOpenTopicWithSub(
                    currentBelt,
                    decision.topic,
                    decision.subTopic
                )
            }

            is SubjectTopicsUiLogic.SubTopicPickDecision.OpenSubject -> {
                val hardSubjectId = when (decision.subject.id) {
                    "releases",
                    "releases_hugs" -> releasesSectionIdFor(picked) ?: "releases_hugs"

                    else -> null
                }

                if (hardSubjectId != null) {
                    onOpenHardSubjectRoute(currentBelt, hardSubjectId)
                } else {
                    openSubjectSmart(decision.subject)
                }
            }

            SubjectTopicsUiLogic.SubTopicPickDecision.None -> Unit
        }
    }

    fun isReleasesRootCard(card: SubjectTopicsUiLogic.SubjectCardModel): Boolean {
        val cleanId = card.id.trim().lowercase()
        val cleanTitle = normText(stripLockSuffix(card.title))

        return cleanId == "releases" ||
                cleanTitle == "שחרורים" ||
                cleanTitle == "releases"
    }

    fun isDefenseRootCard(card: SubjectTopicsUiLogic.SubjectCardModel): Boolean {
        val cleanId = card.id.trim().lowercase()
        val cleanTitle = normText(stripLockSuffix(card.title))

        return isDefenseRootSubjectId(cleanId) ||
                cleanTitle == "הגנות" ||
                cleanTitle == "defenses"
    }

    val visibleSubjectsSplit = remember(subjects, uiSectionCounts) {
        SubjectTopicsUiLogic.splitVisibleSubjects(
            subjects = subjects,
            sectionCounts = uiSectionCounts
        )
    }

    val subjectsWithSubTopicsCards = remember(
        visibleSubjectsSplit,
        uiSectionCounts,
        subjectCounts,
        isEnglish,
        hasAccess
    ) {
        SubjectTopicsUiLogic.buildSubjectCardModels(
            subjects = visibleSubjectsSplit.withSubTopics,
            sectionCounts = uiSectionCounts,
            subjectCounts = subjectCounts,
            formatCount = ::formatCount
        ).map { card ->

            val baseTitle = subjectTitleForUi(
                subjectId = card.id,
                fallbackHeb = card.title,
                isEnglish = isEnglish
            )

            val titleWithLock =
                if (
                    card.id == "releases" &&
                    accessMode != AccessMode.OPEN
                ) {
                    "$baseTitle 🔒"
                } else if (LockedContentPolicy.shouldShowLock(accessMode, baseTitle)) {
                    "$baseTitle 🔒"
                } else {
                    baseTitle
                }

            card.copy(
                title = titleWithLock,
                countText = translateCardCountText(card.countText)
            )
        }.sortedWith(
            compareByDescending<SubjectTopicsUiLogic.SubjectCardModel> {
                isPremiumRootSubject(it.id)
            }.thenBy {
                when (it.id.trim().lowercase()) {
                    "defense_root", "defenses_root" -> 0
                    "releases" -> 1
                    else -> 2
                }
            }
        )
    }

    val subjectsWithoutSubTopicsCards = remember(
        visibleSubjectsSplit,
        uiSectionCounts,
        subjectCounts,
        isEnglish,
        hasAccess
    ) {
        SubjectTopicsUiLogic.buildSubjectCardModels(
            subjects = visibleSubjectsSplit.withoutSubTopics,
            sectionCounts = uiSectionCounts,
            subjectCounts = subjectCounts,
            formatCount = ::formatCount
        ).map { card ->

            val baseTitle = subjectTitleForUi(
                subjectId = card.id,
                fallbackHeb = card.title,
                isEnglish = isEnglish
            )

            val titleWithLock =
                if (
                    card.id == "releases" &&
                    accessMode != AccessMode.OPEN
                ) {
                    "$baseTitle 🔒"
                } else if (LockedContentPolicy.shouldShowLock(accessMode, baseTitle)) {
                    "$baseTitle 🔒"
                } else {
                    baseTitle
                }

            card.copy(
                title = titleWithLock,
                countText = translateCardCountText(card.countText)
            )
        }
    }

    // ✅ Releases יכול להגיע בשתי צורות:
    // 1. כנושא עם תתי־נושאים — ברוב החגורות
    // 2. כנושא ישיר בלי תתי־נושאים — למשל חגורה חומה
    // לכן מחפשים אותו גם ב-withSubTopics וגם ב-withoutSubTopics.
    val releasesRootCard = remember(
        subjectsWithSubTopicsCards,
        subjectsWithoutSubTopicsCards,
        subTopicsPickCountsBySubjectId,
        currentBelt,
        isEnglish,
        hasAccess
    ) {
        val card = subjectsWithSubTopicsCards.firstOrNull { isReleasesRootCard(it) }
            ?: subjectsWithoutSubTopicsCards.firstOrNull { isReleasesRootCard(it) }

        card?.let {
            val baseTitle = subjectTitleForUi(
                subjectId = "releases",
                fallbackHeb = stripLockSuffix(it.title),
                isEnglish = isEnglish
            )

            val releaseCounts = subTopicsPickCountsBySubjectId["releases"].orEmpty()

            val displayedReleasesCount =
                releaseCounts
                    .values
                    .sum()

            it.copy(
                id = "releases",
                title = if (hasAccess) baseTitle else "$baseTitle 🔒",
                countText = if (displayedReleasesCount > 0) {
                    formatSubTopicsAndExercises(
                        subTopics = releaseCounts.size,
                        exercises = displayedReleasesCount
                    )
                } else {
                    countTextFromSubTopicTotalsOrFallback(
                        counts = releaseCounts,
                        fallback = it.countText,
                        subTopicsCount = releaseCounts.size
                    )
                }
            )
        }
    }

    val otherSubjectsWithSubTopicsCards = remember(subjectsWithSubTopicsCards) {
        subjectsWithSubTopicsCards.filterNot {
            isReleasesRootCard(it) || isDefenseRootCard(it)
        }
    }

    val otherSubjectsWithoutSubTopicsCards = remember(subjectsWithoutSubTopicsCards) {
        subjectsWithoutSubTopicsCards.filterNot {
            isReleasesRootCard(it) || isDefenseRootCard(it)
        }
    }

    val defenseRootCard = remember(totalDefense, defenseDialogCountsMap, isEnglish, hasAccess) {
        val base = SubjectTopicsUiLogic.buildDefenseRootCard(
            totalDefense = totalDefense,
            formatCount = ::formatCount,
            subTopicsCount = defenseDialogCountsMap.size
        )

        val baseTitle = subjectTitleForUi(
            subjectId = "defense_root",
            fallbackHeb = base.title,
            isEnglish = isEnglish
        )

        base.copy(
            title = if (hasAccess) baseTitle else "$baseTitle 🔒",
            countText = countTextFromSubTopicTotalsOrFallback(
                counts = defenseDialogCountsMap,
                fallback = base.countText,
                subTopicsCount = defenseDialogCountsMap.size
            )
        )
    }

    val handsRootCard = remember(
        handsRootCount,
        handsPickCounts,
        currentBelt,
        isEnglish
    ) {
        val uniqueHandsCount = uniqueExerciseCountForTopics(
            "עבודות ידיים",
            "עבודת ידיים"
        )

        val fallbackHandsCount =
            handsPickCounts.values.sum().takeIf { it > 0 }
                ?: handsRootCount

        val displayedHandsCount =
            uniqueHandsCount.takeIf { it > 0 }
                ?: fallbackHandsCount

        val base = SubjectTopicsUiLogic.buildHandsRootCard(
            handsRootCount = displayedHandsCount,
            formatCount = ::formatCount,
            subTopicsCount = handsPickCounts.size
        )

        base.copy(
            title = subjectTitleForUi(
                subjectId = "hands_root",
                fallbackHeb = base.title,
                isEnglish = isEnglish
            ),
            countText = if (displayedHandsCount > 0) {
                formatSubTopicsAndExercises(
                    subTopics = handsPickCounts.size,
                    exercises = displayedHandsCount
                )
            } else {
                countTextFromSubTopicTotalsOrFallback(
                    counts = handsPickCounts,
                    fallback = base.countText,
                    subTopicsCount = handsPickCounts.size
                )
            }
        )
    }

    LaunchedEffect(
        defenseRootCard,
        releasesRootCard,
        handsRootCard,
        otherSubjectsWithSubTopicsCards,
        otherSubjectsWithoutSubTopicsCards,
        countsReady,
        isEnglish
    ) {
        if (!countsReady) return@LaunchedEffect

        val pdfItems = buildList {
            add(
                SubjectTopicsPdfItem(
                    id = "defense_root",
                    title = stripLockSuffix(defenseRootCard.title),
                    countText = defenseRootCard.countText
                )
            )

            releasesRootCard?.let { card ->
                add(
                    SubjectTopicsPdfItem(
                        id = "releases",
                        title = stripLockSuffix(card.title),
                        countText = card.countText
                    )
                )
            }

            add(
                SubjectTopicsPdfItem(
                    id = "hands_root",
                    title = stripLockSuffix(handsRootCard.title),
                    countText = handsRootCard.countText
                )
            )

            otherSubjectsWithSubTopicsCards.forEach { card ->
                add(
                    SubjectTopicsPdfItem(
                        id = card.id,
                        title = stripLockSuffix(card.title),
                        countText = countTextForSubjectCard(card)
                    )
                )
            }

            otherSubjectsWithoutSubTopicsCards.forEach { card ->
                add(
                    SubjectTopicsPdfItem(
                        id = card.id,
                        title = stripLockSuffix(card.title),
                        countText = countTextForSubjectCard(card)
                    )
                )
            }
        }
            .distinctBy { it.id.trim().lowercase() }

        onPdfItemsChanged(pdfItems)
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                color =
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.97f
                    ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Subjects (Categories)"
                            } else {
                                "נושאים (קטגוריות)"
                            },
                        style = KmiTypography.sectionTitle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 8.dp,
                                bottom = 6.dp
                            ),
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )

                    if (!countsReady) {
                        KmiLoadingRings(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            text =
                                if (isEnglish) {
                                    "Loading data..."
                                } else {
                                    "טוען נתונים…"
                                }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 8.dp)
                                .padding(top = 0.dp, bottom = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            val pinnedLockCards = buildList {
                                add(
                                    Triple(
                                        "defense_root",
                                        defenseRootCard.title,
                                        defenseRootCard.countText
                                    )
                                )

                                releasesRootCard?.let { releasesCard ->
                                    add(
                                        Triple(
                                            "releases",
                                            releasesCard.title,
                                            releasesCard.countText
                                        )
                                    )
                                }
                            }

                            pinnedLockCards.forEach { card ->
                                SubjectRootCardPremium(
                                    title = card.second,
                                    subtitle = "",
                                    subjectId = card.first,
                                    countText = card.third,
                                    showLeftBadge = true,
                                    isDarkMode = isDarkMode,
                                    showExpandArrow = true,
                                    isExpanded = expandedSubTopicsForId == card.first,
                                    onClick = {
                                        when (card.first) {
                                            "defense_root",
                                            "defenses_root",
                                            "defenses" -> {
                                                expandedSubTopicsForId =
                                                    if (expandedSubTopicsForId == "defense_root") null else "defense_root"
                                            }

                                            "releases" -> {
                                                if (!hasAccess) {
                                                    onOpenSubscription()
                                                } else {
                                                    expandedSubTopicsForId =
                                                        if (expandedSubTopicsForId == "releases") null else "releases"
                                                }
                                            }
                                        }
                                    }
                                )

                                if (card.first == "defense_root" && expandedSubTopicsForId == "defense_root") {
                                    val defensePicks = remember(isEnglish, hasAccess) {
                                        listOf(
                                            if (isEnglish) "Internal Defenses" else "הגנות פנימיות",
                                            if (isEnglish) "External Defenses" else "הגנות חיצוניות",
                                            if (isEnglish) "Defenses Against Kicks" else "הגנות נגד בעיטות",
                                            if (isEnglish) "Knife Defenses" else "הגנות מסכין",
                                            if (isEnglish) "Rifle Defenses Against Knife Stabs" else "הגנות עם רובה נגד דקירות סכין",
                                            if (isEnglish) "Gun Threat Defenses" else "הגנות מאיום אקדח",
                                            if (isEnglish) "Defenses Against Multiple Attackers" else "הגנות נגד מספר תוקפים",
                                            if (isEnglish) "Stick Defenses" else "הגנות נגד מקל"
                                        ).map { title ->
                                            val cleanTitle = stripLockSuffix(title).trim()

                                            val isFreeKickDefense =
                                                cleanTitle == "הגנות נגד בעיטות" ||
                                                        cleanTitle == "Defenses Against Kicks"

                                            withLockSuffix(title, !hasAccess && !isFreeKickDefense)
                                        }
                                    }

                                    InlineSubTopicsExpansionCard(
                                        picks = defensePicks,
                                        counts = defenseDialogCountsMap,
                                        isEnglish = isEnglish,
                                        accent = subjectAccentColor("defense_root"),
                                        onPick = { pickedDisplay ->
                                            val pickedClean = stripLockSuffix(pickedDisplay)

                                            val pickedForLogic = when (pickedClean) {
                                                "Internal Defenses" -> "הגנות פנימיות"
                                                "External Defenses" -> "הגנות חיצוניות"
                                                "Defenses Against Kicks" -> "הגנות נגד בעיטות"
                                                "Knife Defenses" -> "הגנות מסכין"
                                                "Rifle Defenses Against Knife Stabs" -> "הגנות עם רובה נגד דקירות סכין"
                                                "Gun Threat Defenses" -> "הגנות מאיום אקדח"
                                                "Defenses Against Multiple Attackers" -> "הגנות נגד מספר תוקפים"
                                                "Stick Defenses" -> "הגנות נגד מקל"
                                                else -> pickedClean
                                            }

                                            if (pickedForLogic == "הגנות נגד בעיטות") {
                                                onOpenKicksHardLocal()
                                            } else {
                                                when (val decision =
                                                    SubjectTopicsUiLogic.resolveDefenseDialogPick(
                                                        pickedForLogic
                                                    )) {

                                                    is SubjectTopicsUiLogic.DefenseDialogDecision.AskKind -> {
                                                        val combinedId =
                                                            defenseCombinedSectionIdFor(decision.kind)

                                                        if (combinedId != null) {
                                                            onOpenHardSubjectRoute(
                                                                currentBelt,
                                                                combinedId
                                                            )
                                                        } else {
                                                            askKind = decision.kind
                                                        }
                                                    }

                                                    is SubjectTopicsUiLogic.DefenseDialogDecision.OpenHardSubject -> {
                                                        when (decision.subjectId) {
                                                            "kicks",
                                                            "kicks_hard" -> {
                                                                onOpenKicksHardLocal()
                                                            }

                                                            else -> {
                                                                onOpenHardSubjectRoute(
                                                                    currentBelt,
                                                                    decision.subjectId
                                                                )
                                                            }
                                                        }
                                                    }

                                                    SubjectTopicsUiLogic.DefenseDialogDecision.None -> Unit
                                                }
                                            }
                                        }
                                    )
                                }

                                if (card.first == "releases" && expandedSubTopicsForId == "releases") {
                                    val dialogData = remember(subjects) {
                                        SubjectTopicsUiLogic.buildSubTopicsDialogData(
                                            subjects = subjects,
                                            id = "releases"
                                        )
                                    }

                                    val counts =
                                        subTopicsPickCountsBySubjectId["releases"].orEmpty()

                                    val displayPicks = remember(
                                        dialogData.picks,
                                        dialogData.base,
                                        isEnglish,
                                        hasAccess,
                                        accessMode
                                    ) {
                                        dialogData.picks.map { rawPick ->
                                            val uiTitle =
                                                subTopicTitleForUi(rawPick.trim(), isEnglish)

                                            withLockSuffix(
                                                uiTitle,
                                                accessMode != AccessMode.OPEN &&
                                                        LockedContentPolicy.shouldShowLock(
                                                            accessMode,
                                                            dialogData.base?.titleHeb.orEmpty()
                                                        )
                                            )
                                        }
                                    }

                                    InlineSubTopicsExpansionCard(
                                        picks = displayPicks,
                                        counts = counts,
                                        isEnglish = isEnglish,
                                        accent = subjectAccentColor("releases"),
                                        onPick = { pickedDisplay ->
                                            openPickedSubTopic("releases", pickedDisplay)
                                        }
                                    )
                                }

                                HorizontalDivider(
                                    thickness = 0.8.dp,
                                    color =
                                        MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.65f
                                        ),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }

                            SubjectRootCardPremium(
                                title = handsRootCard.title,
                                subtitle = "",
                                subjectId = "hands_root",
                                countText = handsRootCard.countText,
                                showLeftBadge = true,
                                isDarkMode = isDarkMode,
                                showExpandArrow = true,
                                isExpanded = expandedSubTopicsForId == "hands_root",
                                onClick = {
                                    expandedSubTopicsForId =
                                        if (expandedSubTopicsForId == "hands_root") null else "hands_root"
                                }
                            )

                            if (expandedSubTopicsForId == "hands_root") {
                                val handsPicks = remember(handsBase) {
                                    SubjectTopicsUiLogic.handsPicks(handsBase)
                                }

                                val handsDisplayPicks = remember(handsPicks, isEnglish) {
                                    handsPicks.map { subTopicTitleForUi(it, isEnglish) }
                                }

                                InlineSubTopicsExpansionCard(
                                    picks = handsDisplayPicks,
                                    counts = handsPickCounts,
                                    isEnglish = isEnglish,
                                    accent = subjectAccentColor("hands_root"),
                                    onPick = { pickedDisplay ->
                                        val picked = handsPicks.firstOrNull {
                                            subTopicTitleForUi(it, isEnglish) == pickedDisplay
                                        } ?: pickedDisplay

                                        val hardSubjectId = handsSectionIdFor(picked)

                                        if (hardSubjectId != null) {
                                            onOpenHardSubjectRoute(currentBelt, hardSubjectId)
                                        } else {
                                            val subject = SubjectTopicsUiLogic.resolveHandsPick(
                                                base = handsBase,
                                                picked = picked
                                            )

                                            if (subject != null) {
                                                openSubjectSmart(subject)
                                            }
                                        }
                                    }
                                )
                            }


                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = if (isDarkMode) {
                                    Color.White.copy(alpha = 0.08f)
                                } else {
                                    Color(0x14000000)
                                },
                                modifier = Modifier.padding(horizontal = if (isDarkMode) 8.dp else 6.dp)
                            )

                            // ✅ שאר הנושאים עם תתי־נושאים (בלי releases ובלי defenses שכבר הוצגו למעלה)
                            otherSubjectsWithSubTopicsCards
                                .filter { it.id != "defense_root" && it.id != "defenses_root" }
                                .forEach { card ->
                                    SubjectRootCardPremium(
                                        title = card.title,
                                        subtitle = "",
                                        subjectId = card.id,
                                        countText = countTextForSubjectCard(card),
                                        isDarkMode = isDarkMode,
                                        showExpandArrow = card.id != "releases_hugs",
                                        isExpanded = expandedSubTopicsForId == card.id,
                                        onClick = {
                                            if (card.id == "releases_hugs") {
                                                onOpenHardSubjectRoute(currentBelt, "releases_hugs")
                                            } else {
                                                expandedSubTopicsForId =
                                                    if (expandedSubTopicsForId == card.id) null else card.id
                                            }
                                        }
                                    )

                                    if (expandedSubTopicsForId == card.id) {
                                        val dialogData = remember(subjects, card.id) {
                                            SubjectTopicsUiLogic.buildSubTopicsDialogData(
                                                subjects = subjects,
                                                id = card.id
                                            )
                                        }

                                        val counts =
                                            subTopicsPickCountsBySubjectId[card.id].orEmpty()

                                        val displayPicks = remember(
                                            dialogData.picks,
                                            dialogData.base,
                                            isEnglish,
                                            hasAccess,
                                            accessMode
                                        ) {
                                            dialogData.picks.map { rawPick ->
                                                val uiTitle =
                                                    subTopicTitleForUi(rawPick.trim(), isEnglish)

                                                withLockSuffix(
                                                    uiTitle,
                                                    accessMode != AccessMode.OPEN &&
                                                            LockedContentPolicy.shouldShowLock(
                                                                accessMode,
                                                                dialogData.base?.titleHeb.orEmpty()
                                                            )
                                                )
                                            }
                                        }

                                        InlineSubTopicsExpansionCard(
                                            picks = displayPicks,
                                            counts = counts,
                                            isEnglish = isEnglish,
                                            accent = subjectAccentColor(card.id),
                                            onPick = { pickedDisplay ->
                                                openPickedSubTopic(card.id, pickedDisplay)
                                            }
                                        )
                                    }

                                    HorizontalDivider(
                                        thickness = 0.8.dp,
                                        color = if (isDarkMode) {
                                            Color.White.copy(alpha = 0.08f)
                                        } else {
                                            Color(0x14000000)
                                        },
                                        modifier = Modifier.padding(horizontal = if (isDarkMode) 8.dp else 6.dp)
                                    )
                                }

                            // ✅ מציגים נושאים בלי תתי־נושאים
                            // releases כבר מוצג למעלה יחד עם Defenses כדי לקבל מנעול כמו נושא פרימיום.
                            otherSubjectsWithoutSubTopicsCards
                                .filter { it.id != "defenses" }
                                .forEachIndexed { index, card ->

                                    val subject = subjects.firstOrNull { it.id == card.id }
                                        ?: return@forEachIndexed
                                    SubjectRootCardPremium(
                                        title = card.title,
                                        subtitle = "",
                                        subjectId = card.id,
                                        countText = countTextForSubjectCard(card),
                                        isDarkMode = isDarkMode,
                                        onClick = {
                                            when (card.id) {
                                                "topic_kavaler" -> {
                                                    val action =
                                                        SubjectTopicsUiLogic.buildOpenSubjectUiAction(
                                                            subject = subject,
                                                            currentBelt = currentBelt
                                                        )

                                                    val chosenBelt = action.chosenBelt

                                                    onOpenHardSubjectRoute(
                                                        chosenBelt,
                                                        "topic_kavaler"
                                                    )
                                                }

                                                else -> {
                                                    openSubjectSmart(subject)
                                                }
                                            }
                                        }
                                    )

                                    if (index != otherSubjectsWithoutSubTopicsCards.lastIndex) {
                                        HorizontalDivider(
                                            thickness = 0.8.dp,
                                            color = if (isDarkMode) {
                                                Color.White.copy(alpha = 0.08f)
                                            } else {
                                                Color(0x14000000)
                                            },
                                            modifier = Modifier.padding(horizontal = if (isDarkMode) 8.dp else 6.dp)
                                        )
                                    }
                                }

                            if (askDefense) {
                                DefenseCategoryPickDialogModern(
                                    counts = defenseDialogCountsMap,
                                    hasAccess = hasAccess,
                                    onDismiss = { askDefense = false },
                                    onPick = { picked ->
                                        askDefense = false

                                        val pickedClean = picked
                                            .replace("🔒", "")
                                            .trim()

                                        if (
                                            pickedClean == "הגנות נגד בעיטות" ||
                                            pickedClean == "Defenses Against Kicks"
                                        ) {
                                            onOpenHardSubjectRoute(
                                                currentBelt,
                                                "kicks_hard"
                                            )
                                        } else if (!hasAccess) {
                                            onOpenSubscription()
                                        } else {
                                            when (val decision =
                                                SubjectTopicsUiLogic.resolveDefenseDialogPick(
                                                    pickedClean
                                                )
                                            ) {
                                                is SubjectTopicsUiLogic.DefenseDialogDecision.AskKind -> {
                                                    askKind = decision.kind
                                                }

                                                is SubjectTopicsUiLogic.DefenseDialogDecision.OpenHardSubject -> {
                                                    when (decision.subjectId) {
                                                        "kicks",
                                                        "kicks_hard" -> {
                                                            onOpenHardSubjectRoute(
                                                                currentBelt,
                                                                "kicks_hard"
                                                            )
                                                        }

                                                        else -> {
                                                            onOpenHardSubjectRoute(
                                                                currentBelt,
                                                                decision.subjectId
                                                            )
                                                        }
                                                    }
                                                }

                                                SubjectTopicsUiLogic.DefenseDialogDecision.None -> Unit
                                            }
                                        }
                                    }
                                )
                            }

                            askKind?.let { kind ->
                                DefensePickModeDialogModern(
                                    kind = kind,
                                    counts = defensePickCountsMap,
                                    hasAccess = hasAccess,
                                    onDismiss = { askKind = null },
                                    onPick = { picked ->
                                        askKind = null

                                        val pickedClean = stripLockSuffix(picked)

                                        when (val decision =
                                            SubjectTopicsUiLogic.resolveDefenseKindPick(
                                                kind,
                                                pickedClean
                                            )) {
                                            is SubjectTopicsUiLogic.DefenseKindPickDecision.OpenLegacyDefenses -> {
                                                if (!hasAccess) {
                                                    onOpenSubscription()
                                                } else {
                                                    onOpenDefenseList(
                                                        currentBelt,
                                                        decision.kind,
                                                        decision.pick
                                                    )
                                                }
                                            }

                                            is SubjectTopicsUiLogic.DefenseKindPickDecision.OpenHardSubject -> {
                                                when (decision.subjectId) {
                                                    "kicks",
                                                    "kicks_hard" -> {
                                                        onOpenKicksHardLocal()
                                                    }

                                                    else -> {
                                                        onOpenHardSubjectRoute(
                                                            currentBelt,
                                                            decision.subjectId
                                                        )
                                                    }
                                                }
                                            }

                                            SubjectTopicsUiLogic.DefenseKindPickDecision.None -> Unit
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
