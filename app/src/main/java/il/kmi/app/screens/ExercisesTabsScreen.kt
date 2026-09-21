@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package il.kmi.app.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ExerciseExplanationResolver
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.collectAsState
import il.kmi.app.domain.CanonicalIds
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.domain.ContentRepo
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import il.kmi.app.screens.BeltQuestions.Materials.CoachMaterialProgress
import il.kmi.app.screens.BeltQuestions.Materials.CoachMaterialStatus
import il.kmi.app.screens.BeltQuestions.Materials.ItemFloatingActions
import androidx.compose.ui.graphics.luminance
import androidx.core.content.edit
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.yuval.ui.theme.kmiSectionHeaderBackground
import il.yuval.ui.theme.kmiScreenBackgroundBrush

//==============================================================================

private data class ExerciseCardsPdfItem(
    val title: String,
    val status: Boolean?,
    val isFavorite: Boolean
)

private enum class ExerciseCoachStatus(
    val storageValue: String
) {
    NOT_TAUGHT("not_taught"),
    TAUGHT("taught"),
    PRACTICED("practiced"),
    NEEDS_REINFORCEMENT("needs_reinforcement");

    companion object {
        fun fromStorage(value: String?): ExerciseCoachStatus {
            return entries.firstOrNull { status ->
                status.storageValue == value
            } ?: NOT_TAUGHT
        }
    }
}

@Composable
private fun ExerciseCardsCoachStatusButton(
    selected: Boolean,
    label: String,
    dateText: String,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val dark =
        colors.surface.luminance() < 0.5f

    val accent =
        if (dark) {
            androidx.compose.ui.graphics.lerp(
                activeColor,
                Color.White,
                0.30f
            )
        } else {
            activeColor
        }

    val shape =
        RoundedCornerShape(7.dp)

    val background =
        activeColor.copy(
            alpha =
                if (selected) {
                    if (dark) 0.18f else 0.10f
                } else {
                    if (dark) 0.08f else 0.05f
                }
        )

    val borderColor =
        accent.copy(
            alpha = if (selected) 0.46f else 0.16f
        )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = background,
                shape = shape
            )
            .border(
                width = if (selected) 0.9.dp else 0.7.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 1.dp,
                    vertical = 1.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text = label,
                style =
                    KmiTypography.caption.copy(
                        fontWeight =
                            if (selected) {
                                FontWeight.ExtraBold
                            } else {
                                FontWeight.Bold
                            }
                    ),
                color =
                    if (selected) {
                        accent
                    } else {
                        colors.onSurfaceVariant
                    },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (
                selected &&
                dateText.isNotBlank()
            ) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides
                            LayoutDirection.Ltr
                ) {
                    Text(
                        text = dateText,
                        style =
                            KmiTypography.caption.copy(
                                fontSize =
                                    KmiTypography.caption.fontSize *
                                            0.82f,
                                fontWeight =
                                    FontWeight.Bold
                            ),
                        color =
                            accent.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCardsCoachStatusRow(
    progress: CoachMaterialProgress,
    isEnglish: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNote: () -> Unit,
    onSelect: (CoachMaterialStatus) -> Unit
) {
    val context = LocalContext.current

    CompositionLocalProvider(
        LocalLayoutDirection provides
                if (isEnglish) {
                    LayoutDirection.Ltr
                } else {
                    LayoutDirection.Rtl
                }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .padding(
                    horizontal = 1.dp,
                    vertical = 1.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(4.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            listOf(
                CoachMaterialStatus.TAUGHT,
                CoachMaterialStatus.PRACTICED,
                CoachMaterialStatus.NEEDS_REINFORCEMENT
            ).forEach { status ->

                val selected =
                    progress.isSelected(status)

                val updatedAt =
                    progress.updatedAtFor(status)

                val dateText =
                    if (
                        selected &&
                        updatedAt > 0L
                    ) {
                        java.text.SimpleDateFormat(
                            "dd/MM/yy",
                            java.util.Locale.getDefault()
                        ).format(
                            java.util.Date(updatedAt)
                        )
                    } else {
                        ""
                    }

                ExerciseCardsCoachStatusButton(
                    selected = selected,
                    label =
                        when (status) {
                            CoachMaterialStatus.TAUGHT ->
                                if (isEnglish) "Taught" else "נלמד"

                            CoachMaterialStatus.PRACTICED ->
                                if (isEnglish) "Practiced" else "תורגל"

                            else ->
                                if (isEnglish) "Reinforce" else "חיזוק"
                        },
                    dateText = dateText,
                    activeColor =
                        when (status) {
                            CoachMaterialStatus.TAUGHT ->
                                Color(0xFF2F9B4E)

                            CoachMaterialStatus.PRACTICED ->
                                Color(0xFF6D4BD8)

                            else ->
                                Color(0xFFB96B12)
                        },
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = 2.dp,
                            vertical = 1.dp
                        ),
                    onClick = {
                        if (
                            selected ||
                            progress.selectedStatuses.size < 2
                        ) {
                            onSelect(status)
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                if (isEnglish) {
                                    "You can select up to 2 statuses."
                                } else {
                                    "ניתן לבחור עד 2 סטטוסים."
                                },
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(
                        horizontal = 2.dp,
                        vertical = 1.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                ItemFloatingActions(
                    isEnglish = isEnglish,
                    excluded = false,
                    isFav = isFav,
                    hasNote = hasNote,
                    onToggleExclude = {},
                    onInfo = onInfo,
                    onToggleFavorite = onToggleFavorite,
                    onEditNote = onEditNote
                )
            }
        }
    }
}

@Composable
private fun ExerciseCardsMetaBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = contentColor.copy(alpha = 0.14f)
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = KmiTypography.caption.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = 7.dp,
                vertical = 2.dp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun createExerciseCardsPdf(
    context: android.content.Context,
    belt: Belt,
    topicTitle: String,
    subTopicTitle: String?,
    tabTitle: String,
    items: List<ExerciseCardsPdfItem>,
    isEnglish: Boolean
): java.io.File {
    val pageWidth = 595
    val pageHeight = 842

    val contentLeft = 36f
    val contentRight = pageWidth - 36f
    val contentBottom =
        pageHeight -
                KmiPdfFooter.CONTENT_BOTTOM_PADDING

    val document = android.graphics.pdf.PdfDocument()

    val mediumBlue = android.graphics.Color.rgb(36, 103, 158)
    val darkText = android.graphics.Color.rgb(15, 23, 42)
    val mutedText = android.graphics.Color.rgb(100, 116, 139)
    val rowBackground = android.graphics.Color.rgb(248, 250, 252)
    val rowBorder = android.graphics.Color.rgb(203, 213, 225)

    val regularTypeface = android.graphics.Typeface.create(
        android.graphics.Typeface.SANS_SERIF,
        android.graphics.Typeface.NORMAL
    )

    val boldTypeface = android.graphics.Typeface.create(
        android.graphics.Typeface.SANS_SERIF,
        android.graphics.Typeface.BOLD
    )

    val sectionPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = darkText
        textSize = 15f
        typeface = boldTypeface
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
    }

    val itemTitlePaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = darkText
        textSize = 11.5f
        typeface = boldTypeface
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
    }

    val smallPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = mutedText
        textSize = 9f
        typeface = regularTypeface
    }

    val rowFillPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = rowBackground
        style = android.graphics.Paint.Style.FILL
    }

    val rowStrokePaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = rowBorder
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1f
    }

    val numberFillPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = mediumBlue
        style = android.graphics.Paint.Style.FILL
    }

    val numberTextPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = android.graphics.Color.WHITE
        textSize = 10f
        typeface = boldTypeface
        textAlign = android.graphics.Paint.Align.CENTER
    }

    var pageNumber = 0
    lateinit var page: android.graphics.pdf.PdfDocument.Page
    lateinit var canvas: android.graphics.Canvas
    var y = 0f

    fun textX(): Float {
        return if (isEnglish) contentLeft else contentRight
    }

    fun beltLabel(): String {
        return if (isEnglish) belt.en else belt.heb
    }

    fun cleanPdfText(value: String): String {
        return value
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun ellipsize(
        value: String,
        paint: android.graphics.Paint,
        maxWidth: Float
    ): String {
        val clean = cleanPdfText(value)

        if (paint.measureText(clean) <= maxWidth) {
            return clean
        }

        var result = clean

        while (
            result.length > 4 &&
            paint.measureText("$result…") > maxWidth
        ) {
            result = result.dropLast(1)
        }

        return "$result…"
    }

    fun drawHeader() {
        val subtitleHebrew =
            buildString {
                append("${belt.heb} · $topicTitle")

                if (!subTopicTitle.isNullOrBlank()) {
                    append(" · תת־נושא: $subTopicTitle")
                }
            }

        val subtitleEnglish =
            buildString {
                append("${belt.en} · $topicTitle")

                if (!subTopicTitle.isNullOrBlank()) {
                    append(" · Sub-topic: $subTopicTitle")
                }
            }

        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "דו״ח כרטיסיות תרגילים",
            titleEnglish = "Exercise Cards Report",
            subtitleHebrew = subtitleHebrew,
            subtitleEnglish = subtitleEnglish
        )

        y = KmiPdfHeader.CONTENT_TOP
    }

    fun drawFooter() {
        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = pageNumber,
            totalPages = null,
            isEnglish = isEnglish
        )
    }

    fun startPage() {
        if (pageNumber > 0) {
            drawFooter()
            document.finishPage(page)
        }

        pageNumber++

        page = document.startPage(
            android.graphics.pdf.PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()
        )

        canvas = page.canvas
        drawHeader()
    }

    fun ensureSpace(requiredHeight: Float) {
        if (y + requiredHeight > contentBottom) {
            startPage()
        }
    }

    fun drawItem(
        index: Int,
        item: ExerciseCardsPdfItem
    ) {
        val rowHeight = 54f

        ensureSpace(rowHeight + 8f)

        val rowTop = y
        val rowBottom = rowTop + rowHeight

        canvas.drawRoundRect(
            contentLeft,
            rowTop,
            contentRight,
            rowBottom,
            12f,
            12f,
            rowFillPaint
        )

        canvas.drawRoundRect(
            contentLeft,
            rowTop,
            contentRight,
            rowBottom,
            12f,
            12f,
            rowStrokePaint
        )

        val numberCenterX = if (isEnglish) {
            contentLeft + 22f
        } else {
            contentRight - 22f
        }

        val numberCenterY = rowTop + rowHeight / 2f

        canvas.drawCircle(
            numberCenterX,
            numberCenterY,
            14f,
            numberFillPaint
        )

        canvas.drawText(
            (index + 1).toString(),
            numberCenterX,
            numberCenterY + 3.5f,
            numberTextPaint
        )

        val statusColor = when (item.status) {
            true -> android.graphics.Color.rgb(22, 163, 74)
            false -> android.graphics.Color.rgb(220, 38, 38)
            null -> android.graphics.Color.rgb(100, 116, 139)
        }

        val statusText = when (item.status) {
            true -> if (isEnglish) "Known" else "יודע"
            false -> if (isEnglish) "Unknown" else "לא יודע"
            null -> if (isEnglish) "Unmarked" else "לא סומן"
        }

        val statusPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = statusColor
            textSize = 9f
            typeface = boldTypeface
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val statusDotPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = statusColor
            style = android.graphics.Paint.Style.FILL
        }

        val statusCenterX = if (isEnglish) {
            contentRight - 45f
        } else {
            contentLeft + 45f
        }

        canvas.drawCircle(
            statusCenterX,
            rowTop + 17f,
            5.5f,
            statusDotPaint
        )

        canvas.drawText(
            statusText,
            statusCenterX,
            rowTop + 38f,
            statusPaint
        )

        val titleX = if (isEnglish) {
            contentLeft + 48f
        } else {
            contentRight - 48f
        }

        val maxTitleWidth =
            contentRight - contentLeft - 150f

        canvas.drawText(
            ellipsize(
                value = item.title,
                paint = itemTitlePaint,
                maxWidth = maxTitleWidth
            ),
            titleX,
            rowTop + 25f,
            itemTitlePaint
        )

        smallPaint.textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }

        canvas.drawText(
            if (item.isFavorite) {
                if (isEnglish) "Favorite" else "מועדף"
            } else {
                if (isEnglish) "Not favorite" else "לא מועדף"
            },
            titleX,
            rowTop + 42f,
            smallPaint
        )

        y = rowBottom + 8f
    }

    startPage()

    val knownCount = items.count { it.status == true }
    val unknownCount = items.count { it.status == false }
    val unmarkedCount = items.count { it.status == null }
    val favoriteCount = items.count { it.isFavorite }

    canvas.drawText(
        if (isEnglish) {
            "$tabTitle · ${items.size} exercises"
        } else {
            "$tabTitle · ${items.size} תרגילים"
        },
        textX(),
        y,
        sectionPaint
    )

    y += 24f

    smallPaint.textAlign = if (isEnglish) {
        android.graphics.Paint.Align.LEFT
    } else {
        android.graphics.Paint.Align.RIGHT
    }

    canvas.drawText(
        if (isEnglish) {
            "$knownCount known · $unknownCount unknown · " +
                    "$unmarkedCount unmarked · $favoriteCount favorites"
        } else {
            "$knownCount יודע · $unknownCount לא יודע · " +
                    "$unmarkedCount לא סומן · $favoriteCount מועדפים"
        },
        textX(),
        y,
        smallPaint
    )

    y += 22f

    items.forEachIndexed { index, item ->
        drawItem(
            index = index,
            item = item
        )
    }

    drawFooter()
    document.finishPage(page)

    val outputDirectory = java.io.File(
        context.cacheDir,
        "exercise_cards_pdf"
    ).apply {
        mkdirs()
    }

    val fileName =
        if (isEnglish) {
            "Exercise Cards Report.pdf"
        } else {
            "דוח כרטיסיות תרגילים.pdf"
        }

    val outputFile =
        java.io.File(
            outputDirectory,
            fileName
        )

    try {
        java.io.FileOutputStream(
            outputFile,
            false
        ).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }

    return outputFile
}

@Composable
fun ExercisesTabsScreen(
    vm: KmiViewModel,
    belt: Belt,
    topic: String,
    onPractice: (Belt, String) -> Unit,
    subTopicFilter: String? = null,
    onHome: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val sp = remember {
        ctx.getSharedPreferences(
            "kmi_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    val notesSp = remember {
        ctx.getSharedPreferences(
            "kmi_notes",
            android.content.Context.MODE_PRIVATE
        )
    }

    val roleSp = remember(ctx) {
        ctx.getSharedPreferences(
            "kmi_user",
            android.content.Context.MODE_PRIVATE
        )
    }

    fun readActiveExercisesRole(): String {
        return roleSp.getString(
            "active_user_mode",
            null
        )
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
            ?: roleSp.getString(
                "last_active_app_role",
                null
            )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
            ?: roleSp.getString(
                "user_role",
                null
            )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
            ?: roleSp.getString(
                "role",
                ""
            )
                ?.trim()
                .orEmpty()
    }

    var currentRole by remember(roleSp) {
        mutableStateOf(
            readActiveExercisesRole()
        )
    }

    DisposableEffect(roleSp) {
        val roleListener =
            android.content.SharedPreferences
                .OnSharedPreferenceChangeListener { _,
                                                    key ->

                    if (
                        key == "active_user_mode" ||
                        key == "last_active_app_role" ||
                        key == "user_role" ||
                        key == "role"
                    ) {
                        currentRole =
                            readActiveExercisesRole()
                    }
                }

        roleSp.registerOnSharedPreferenceChangeListener(
            roleListener
        )

        onDispose {
            roleSp.unregisterOnSharedPreferenceChangeListener(
                roleListener
            )
        }
    }

    val normalizedRole =
        currentRole
            .trim()
            .lowercase()

    val isCoach =
        normalizedRole == "coach" ||
                normalizedRole == "trainer" ||
                normalizedRole.contains("coach") ||
                normalizedRole.contains("מאמן") ||
                normalizedRole.contains("מדריך")

// ⭐ Favorites גלובלי – source of truth אחד לכל האפליקציה
    val favorites: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

// ✅ רענון סימוני יודע/לא יודע שהגיעו ממסכים אחרים, כולל MaterialsScreen
    val marksVersion by vm.marksVersion.collectAsState()

    fun readSet(key: String): MutableSet<String> =
        sp.getStringSet(
            key,
            emptySet()
        )?.toMutableSet() ?: mutableSetOf()

    val allUnknownKeys = remember(belt.id, marksVersion) {
        sp.all.keys.filter { it.startsWith("unknown_${belt.id}_") }
    }

    // --- item list כמו ב-MaterialsScreen ---
    data class TopicItems(val topic: String, val items: Set<String>)

    // ✅ Source of truth דרך ContentRepo
    fun String.normTitle(): String = this
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .replace('־', '-')
        .replace('–', '-')
        .trim()
        .lowercase()

    fun dec(s: String) =
        try {
            java.net.URLDecoder.decode(s, "UTF-8")
        } catch (_: Exception) {
            s
        }

    fun contentItemsForTopicIncludingSubTopics(
        belt: Belt,
        topicTitle: String
    ): List<String> {
        val directItems = ContentRepo.listItemTitles(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = null
        )

        val subTopicItems = ContentRepo
            .listSubTopicTitles(belt, topicTitle)
            .flatMap { subTitle ->
                ContentRepo.listItemTitles(
                    belt = belt,
                    topicTitle = topicTitle,
                    subTopicTitle = subTitle
                )
            }

        return (directItems + subTopicItems)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val allTopicItems: List<TopicItems> = remember(belt, topic) {
        if (topic != "__ALL__") return@remember emptyList()

        val topicTitles = ContentRepo.listTopicTitles(belt)

        topicTitles.mapNotNull { tpTitle ->
            val title = tpTitle.trim()
            if (title.isBlank()) return@mapNotNull null

            val items = contentItemsForTopicIncludingSubTopics(
                belt = belt,
                topicTitle = title
            ).toSet()

            if (items.isEmpty()) return@mapNotNull null

            TopicItems(title, items)
        }
    }

    val itemList: List<String> = remember(belt, topic, subTopicFilter, allTopicItems) {

        if (topic == "__ALL__") {
            return@remember allTopicItems.flatMap { it.items }.distinct()
        }

        // אם יש סינון תת-נושא
        subTopicFilter?.takeIf { it.isNotBlank() }?.let { raw ->
            val subRaw = dec(raw)

            // 1) subTopic match (exact/loose) מתוך Bridge
            val subTitles = ContentRepo.listSubTopicTitles(belt, topic)
            val exact = subTitles.firstOrNull { it.normTitle() == subRaw.normTitle() }
            if (exact != null) {
                val items = ContentRepo.listItemTitles(belt, topic, subTopicTitle = exact)
                if (items.isNotEmpty()) return@remember items
            }

            val wanted = subRaw.normTitle()
            val loose = subTitles.firstOrNull { st ->
                val a = st.normTitle()
                a.startsWith(wanted) || wanted.startsWith(a) || a.contains(wanted) || wanted.contains(
                    a
                )
            }
            if (loose != null) {
                val items = ContentRepo.listItemTitles(belt, topic, subTopicTitle = loose)
                if (items.isNotEmpty()) return@remember items
            }

            // 2) fallback: KmiSearchBridge (רק אם עדיין קיים אצלך)
            val bySubBridge =
                runCatching { il.kmi.app.search.KmiSearchBridge.itemsFor(belt, subRaw) }
                    .getOrDefault(emptyList())
            if (bySubBridge.isNotEmpty()) return@remember bySubBridge

            return@remember emptyList()
        }

        // ללא סינון תת-נושא: כל הפריטים של הנושא, כולל תתי־נושאים
        val byTopic = contentItemsForTopicIncludingSubTopics(
            belt = belt,
            topicTitle = topic
        )

        if (byTopic.isNotEmpty()) return@remember byTopic

        // fallback: bridge לפי נושא
        val byTopicBridge = runCatching {
            il.kmi.app.search.KmiSearchBridge.itemsFor(belt, topic)
        }.getOrDefault(emptyList())

        if (byTopicBridge.isNotEmpty()) return@remember byTopicBridge

        emptyList()
    }

    // הסברי תרגילים מתוך הרשימה נשארים מקומיים.
    // החיפוש הגלובלי עצמו מטופל עכשיו פנימית דרך KmiTopBar + ExercisePremiumSearchDialog.
    var explainFromSearch by remember { mutableStateOf<String?>(null) }
    var noteEditorFor by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var notesRefreshKey by rememberSaveable { mutableIntStateOf(0) }
    /*
   * צד מתאמן:
   * 0 = הכול
   * 1 = לא יודע
   * 2 = מועדפים
   *
   * צד מאמן:
   * 0 = הכול
   * 1 = נלמד
   * 2 = לתרגול
   * 3 = לשיפור
   */
    var selectedTab by rememberSaveable(isCoach) {
        mutableStateOf(0)
    }

    LaunchedEffect(isCoach) {
        selectedTab = 0
    }

// אין יותר searchResults מקומי — החיפוש הגלובלי נמצא ב-KmiTopBar

    fun formattedExerciseTitle(raw: String): String {
        val formatted = ExerciseTitleFormatter
            .displayName(raw)
            .toString()
            .trim()

        return formatted
            .takeIf { value: String ->
                value.isNotBlank() && value != "null"
            }
            ?: raw.trim()
    }

    // ✅ אם זה __ALL__ צריך לדעת לאיזה נושא שייך כל item
    fun topicForRawItem(raw: String): String {
        if (topic != "__ALL__") {
            return topic
        }

        return allTopicItems
            .firstOrNull { topicItems ->
                raw in topicItems.items
            }
            ?.topic
            ?: topic
    }

    /*
     * מקור האמת היחיד לזהות התרגיל.
     *
     * כל תרגיל במסך מומר ל־ex_XXX מתוך ExerciseIdentityRegistry.
     */
    fun exerciseIdentityIdFor(raw: String): String {
        val itemTopic = topicForRawItem(raw)

        return ExerciseIdentityRegistry.idFor(
            belt = belt,
            hebrewTitle = formattedExerciseTitle(raw),
            topicKey = itemTopic
        )
    }

    /*
     * תמיכה גם במועדפים החדשים שנשמרים כ־ex_XXX
     * וגם במועדפים ישנים שנשמרו לפי שם התרגיל.
     */
    val favoriteExerciseIds: Set<String> = remember(
        favorites,
        belt
    ) {
        favorites.mapTo(linkedSetOf()) { storedValue ->
            val cleanValue = storedValue.trim()

            if (cleanValue.matches(Regex("ex_\\d+"))) {
                cleanValue
            } else {
                ExerciseIdentityRegistry.idFor(
                    belt = belt,
                    hebrewTitle = cleanValue,
                    topicKey = null
                )
            }
        }
    }

    val exerciseIdByRaw: Map<String, String> =
        remember(
            itemList,
            belt
        ) {
            itemList.associateWith { raw ->
                exerciseIdentityIdFor(raw)
            }
        }

    val favoriteRawItems: Set<String> =
        remember(
            exerciseIdByRaw,
            favoriteExerciseIds
        ) {
            exerciseIdByRaw
                .filterValues { exerciseId ->
                    exerciseId in favoriteExerciseIds
                }
                .keys
        }

    fun isFavoriteRawItem(
        raw: String
    ): Boolean {
        return raw in favoriteRawItems
    }

    fun noteKeyFor(raw: String): String {
        val exerciseId =
            exerciseIdByRaw[raw]
                ?: exerciseIdentityIdFor(raw)

        return "note_${belt.id}_$exerciseId"
    }

    fun loadNote(raw: String): String =
        notesSp.getString(
            noteKeyFor(raw),
            ""
        )?.trim().orEmpty()

    fun saveNote(
        raw: String,
        value: String
    ) {
        val clean = value.trim()

        notesSp.edit {
            if (clean.isBlank()) {
                remove(
                    noteKeyFor(raw)
                )
            } else {
                putString(
                    noteKeyFor(raw),
                    clean
                )
            }
        }

        notesRefreshKey++
    }

    fun deleteNote(raw: String) {
        notesSp.edit {
            remove(
                noteKeyFor(raw)
            )
        }

        notesRefreshKey++
    }

    val notePresenceByRaw: Map<String, Boolean> =
        remember(
            itemList,
            notesRefreshKey,
            exerciseIdByRaw
        ) {
            itemList.associateWith { raw ->
                loadNote(raw).isNotBlank()
            }
        }

    fun hasNote(
        raw: String
    ): Boolean {
        return notePresenceByRaw[raw] == true
    }

// סטטוסים מה-VM
    val itemStates = remember(
        belt.id,
        topic,
        subTopicFilter
    ) {
        mutableStateMapOf<String, Boolean?>()
    }

    LaunchedEffect(
        belt,
        topic,
        subTopicFilter,
        itemList,
        allTopicItems,
        marksVersion
    ) {
        /*
         * אוספים תחילה את כל המצבים למפה רגילה.
         * כך הרשימה אינה עוברת recomposition אחרי
         * כל תרגיל בזמן הטעינה.
         */
        val loadedStates =
            linkedMapOf<String, Boolean?>()

        itemList.forEach { raw ->
            val itemTopic =
                topicForRawItem(raw)

            val canonicalId =
                CanonicalIds.canonicalFor(
                    belt = belt,
                    topicTitle = itemTopic,
                    displayItem = raw
                )

            loadedStates[raw] =
                runCatching {
                    vm.getItemStatusNullable(
                        belt = belt,
                        topic = itemTopic,
                        item = canonicalId
                    )
                }.getOrNull()
        }

        /*
         * עדכון מרוכז אחד בלבד לאחר סיום הקריאה.
         *
         * אין צורך ב-isMastered:
         * getItemStatusNullable כבר מחזיר
         * true / false / null ממקור האמת.
         */
        itemStates.clear()
        itemStates.putAll(loadedStates)
    }

// ========= ⭐ / X =========
    val suffix = remember(topic, subTopicFilter) {
        if (subTopicFilter.isNullOrBlank()) topic else "${topic}__${subTopicFilter}"
    }


    var unknowns by remember(belt.id, topic, suffix, allUnknownKeys, marksVersion) {
        mutableStateOf(
            if (topic == "__ALL__") {
                allUnknownKeys
                    .flatMap { key -> readSet(key) }
                    .toMutableSet()
            } else {
                readSet("unknown_${belt.id}_$suffix")
                    .toMutableSet()
            }
        )
    }

    fun isUnknownRawItem(raw: String): Boolean {
        val exerciseId = exerciseIdentityIdFor(raw)

        return exerciseId in unknowns
    }

    fun isUnknownForCards(raw: String): Boolean {
        return itemStates[raw] == false ||
                isUnknownRawItem(raw)
    }

    /*
     * מקור אמת יחיד לכל שימושי "לא יודע":
     * המונה, הטאב, הרשימה וקובץ ה־PDF.
     */
    val unknownItems: Set<String> by remember(
        itemList,
        belt,
        topic,
        subTopicFilter,
        marksVersion
    ) {
        derivedStateOf {
            itemList.filterTo(linkedSetOf()) { rawItem ->
                itemStates[rawItem] == false ||
                        isUnknownRawItem(rawItem)
            }
        }
    }

    fun coachStatusBaseKey(raw: String): String {
        val itemTopic = topicForRawItem(raw)
        val exerciseId = exerciseIdentityIdFor(raw)

        val statusTopicKey =
            if (
                !subTopicFilter.isNullOrBlank() &&
                topic != "__ALL__"
            ) {
                "${itemTopic.trim()}__${dec(subTopicFilter).trim()}"
            } else {
                itemTopic.trim()
            }

        return buildString {
            append("coach_material_progress_")
            append(belt.id)
            append("_")
            append(statusTopicKey)
            append("_")
            append(exerciseId)
        }
    }

    fun loadCoachStatuses(
        raw: String
    ): Set<ExerciseCoachStatus> {

        val key =
            coachStatusBaseKey(raw)

        val selectableStatuses =
            listOf(
                ExerciseCoachStatus.TAUGHT,
                ExerciseCoachStatus.PRACTICED,
                ExerciseCoachStatus.NEEDS_REINFORCEMENT
            )

        val selected =
            selectableStatuses
                .filterTo(linkedSetOf()) { status ->
                    sp.getBoolean(
                        "${key}_${status.storageValue}_selected",
                        false
                    )
                }

        /*
         * תאימות לנתונים הישנים:
         * אם אין עדיין מבנה multi-select,
         * מנסים לקרוא את _status הישן.
         */
        if (selected.isEmpty()) {
            val legacy =
                ExerciseCoachStatus.fromStorage(
                    sp.getString(
                        "${key}_status",
                        null
                    )
                )

            if (legacy != ExerciseCoachStatus.NOT_TAUGHT) {
                selected.add(legacy)
            }
        }

        return selected
    }

    fun saveCoachStatuses(
        raw: String,
        statuses: Set<ExerciseCoachStatus>
    ) {
        val key =
            coachStatusBaseKey(raw)

        val selectableStatuses =
            listOf(
                ExerciseCoachStatus.TAUGHT,
                ExerciseCoachStatus.PRACTICED,
                ExerciseCoachStatus.NEEDS_REINFORCEMENT
            )

        sp.edit {
            selectableStatuses.forEach { status ->

                val selected =
                    status in statuses

                putBoolean(
                    "${key}_${status.storageValue}_selected",
                    selected
                )

                if (selected) {
                    putLong(
                        "${key}_${status.storageValue}_updated_at",
                        System.currentTimeMillis()
                    )
                } else {
                    remove(
                        "${key}_${status.storageValue}_updated_at"
                    )
                }
            }

            // מנקים את המבנה הישן אחרי השמירה החדשה.
            remove("${key}_status")
            remove("${key}_updated_at")
        }
    }

    var coachStatusesVersion by rememberSaveable {
        mutableIntStateOf(0)
    }

    val coachStatuses:
            Map<String, Set<ExerciseCoachStatus>> =
        remember(
            itemList,
            isCoach,
            coachStatusesVersion
        ) {
            if (!isCoach) {
                emptyMap()
            } else {
                itemList.associateWith { raw ->
                    loadCoachStatuses(raw)
                }
            }
        }

    fun updateCoachStatus(
        raw: String,
        status: ExerciseCoachStatus
    ) {
        if (status == ExerciseCoachStatus.NOT_TAUGHT) {
            saveCoachStatuses(
                raw = raw,
                statuses = emptySet()
            )

            coachStatusesVersion++
            return
        }

        val current =
            loadCoachStatuses(raw)
                .toMutableSet()

        if (status in current) {
            current.remove(status)
        } else {
            /*
             * במסך המאמן מותר לבחור עד 2 מתוך 3.
             */
            if (current.size >= 2) {
                return
            }

            current.add(status)
        }

        saveCoachStatuses(
            raw = raw,
            statuses = current
        )

        coachStatusesVersion++
    }

    fun toggleFavorite(rawItem: String) {
        FavoritesStore.toggle(
            exerciseIdentityIdFor(rawItem)
        )
    }

    /**
     * סימון/הסרה ממועדפים
     */
    /**
     * סימון/הסרה "לא יודע"
     */
    fun setUnknown(
        rawItem: String,
        set: Boolean
    ) {
        val itemTopic = topicForRawItem(rawItem)
        val exerciseId = exerciseIdentityIdFor(rawItem)
        val canonicalId = CanonicalIds.canonicalFor(
            belt,
            itemTopic,
            rawItem
        )

        val storageKey =
            if (topic == "__ALL__") {
                "unknown_${belt.id}_$itemTopic"
            } else {
                "unknown_${belt.id}_$suffix"
            }

        val storedUnknowns = readSet(storageKey)

        if (set) {
            storedUnknowns.add(exerciseId)

            vm.setItemStatusNullable(
                belt = belt,
                topic = itemTopic,
                item = canonicalId,
                value = false
            )
        } else {
            storedUnknowns.remove(exerciseId)
            storedUnknowns.remove(rawItem.trim())
            storedUnknowns.remove(canonicalId)

            vm.setItemStatusNullable(
                belt = belt,
                topic = itemTopic,
                item = canonicalId,
                value = null
            )
        }

        sp.edit {
            putStringSet(
                storageKey,
                storedUnknowns
            )
        }

        unknowns =
            if (topic == "__ALL__") {
                allUnknownKeys
                    .plus(storageKey)
                    .distinct()
                    .flatMap { key ->
                        readSet(key)
                    }
                    .toMutableSet()
            } else {
                storedUnknowns.toMutableSet()
            }
    }

    val pdfFilteredItems: List<String> =
        if (isCoach) {
            when (selectedTab) {
                1 -> itemList.filter { rawItem ->
                    ExerciseCoachStatus.TAUGHT in
                            coachStatuses[rawItem].orEmpty()
                }

                2 -> itemList.filter { rawItem ->
                    ExerciseCoachStatus.PRACTICED in
                            coachStatuses[rawItem].orEmpty()
                }

                3 -> itemList.filter { rawItem ->
                    ExerciseCoachStatus.NEEDS_REINFORCEMENT in
                            coachStatuses[rawItem].orEmpty()
                }

                else -> itemList
            }
        } else {
            when (selectedTab) {
                1 -> itemList.filter { rawItem ->
                    rawItem in unknownItems
                }

                2 -> itemList.filter { rawItem ->
                    isFavoriteRawItem(rawItem)
                }

                else -> itemList
            }
        }

    val pdfItems: List<ExerciseCardsPdfItem> = pdfFilteredItems
        .map { rawItem ->
            val status: Boolean? = when {
                rawItem in unknownItems -> false
                itemStates[rawItem] == true -> true
                else -> null
            }

            ExerciseCardsPdfItem(
                title = formattedExerciseTitle(rawItem),
                status = status,
                isFavorite = isFavoriteRawItem(rawItem)
            )
        }
        .filter { pdfItem ->
            pdfItem.title.isNotBlank()
        }
        .distinctBy { pdfItem ->
            pdfItem.title
                .replace("\u200F", "")
                .replace("\u200E", "")
                .replace("\u00A0", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .lowercase()
        }

    val pdfTopicTitle = when {
        topic == "__ALL__" -> {
            tr(
                "כל הנושאים",
                "All subjects"
            )
        }

        else -> topic
    }

    val pdfSubTopicTitle = subTopicFilter
        ?.takeIf { it.isNotBlank() }
        ?.let { rawSubTopic ->
            dec(rawSubTopic)
        }

    val pdfTabTitle =
        if (isCoach) {
            when (selectedTab) {
                1 -> tr("נלמד", "Taught")
                2 -> tr("לתרגול", "Practice")
                3 -> tr("לשיפור", "Needs improvement")
                else -> tr("הכול", "All")
            }
        } else {
            when (selectedTab) {
                1 -> tr("לא יודע", "Unknown")
                2 -> tr("מועדפים", "Favorites")
                else -> tr("הכול", "All")
            }
        }

    val onExportPdf: () -> Unit = {
        if (pdfItems.isEmpty()) {
            android.widget.Toast.makeText(
                ctx,
                tr(
                    "אין תרגילים ליצירת קובץ PDF בטאב הנוכחי",
                    "There are no exercises to export in the current tab"
                ),
                android.widget.Toast.LENGTH_LONG
            ).show()
        } else {
            runCatching {
                val pdfFile = createExerciseCardsPdf(
                    context = ctx,
                    belt = belt,
                    topicTitle = pdfTopicTitle,
                    subTopicTitle = pdfSubTopicTitle,
                    tabTitle = pdfTabTitle,
                    items = pdfItems,
                    isEnglish = isEnglish
                )

                val pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    ctx,
                    "${ctx.packageName}.fileprovider",
                    pdfFile
                )

                val shareIntent = android.content.Intent(
                    android.content.Intent.ACTION_SEND
                ).apply {
                    type = "application/pdf"

                    putExtra(
                        android.content.Intent.EXTRA_SUBJECT,
                        tr(
                            "כרטיסיות תרגילים - $pdfTopicTitle",
                            "Exercise Cards - $pdfTopicTitle"
                        )
                    )

                    putExtra(
                        android.content.Intent.EXTRA_STREAM,
                        pdfUri
                    )

                    addFlags(
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                ctx.startActivity(
                    android.content.Intent.createChooser(
                        shareIntent,
                        tr(
                            "שיתוף קובץ PDF",
                            "Share PDF"
                        )
                    )
                )
            }.onFailure {
                android.widget.Toast.makeText(
                    ctx,
                    tr(
                        "יצירת קובץ ה־PDF נכשלה",
                        "Failed to create the PDF file"
                    ),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val practiceToken =
        if (isCoach) {
            when (selectedTab) {
                1 -> "__COACH_TAUGHT__"
                2 -> "__COACH_PRACTICE__"
                3 -> "__COACH_IMPROVEMENT__"

                else -> {
                    if (topic != "__ALL__") {
                        topic
                    } else {
                        "__ALL__"
                    }
                }
            }
        } else {
            when (selectedTab) {
                1 -> "__UNKNOWN__"
                2 -> "__FAVS_ALL__"

                else -> {
                    if (topic != "__ALL__") {
                        topic
                    } else {
                        "__ALL__"
                    }
                }
            }
        }

    val onPracticeClick: () -> Unit = {
        onPractice(
            belt,
            practiceToken
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val contextLang = LocalContext.current
                val langManager = remember { AppLanguageManager(contextLang) }

                il.kmi.app.ui.KmiTopBar(
                    title = tr(
                        "כרטיסיות התרגילים",
                        "Exercise Cards"
                    ),
                    onHome = onHome,
                    centerTitle = true,
                    showTopHome = false,
                    showTopShare = true,
                    showBottomActions = true,
                    lockSearch = false,
                    onShare = onExportPdf,
                    extraActions = {},
                    currentLang = if (
                        langManager.getCurrentLanguage() == AppLanguage.ENGLISH
                    ) {
                        "en"
                    } else {
                        "he"
                    },
                    onToggleLanguage = {
                        val newLang =
                            if (
                                langManager.getCurrentLanguage() ==
                                AppLanguage.HEBREW
                            ) {
                                AppLanguage.ENGLISH
                            } else {
                                AppLanguage.HEBREW
                            }

                        langManager.setLanguage(newLang)
                        (contextLang as? Activity)?.recreate()
                    }
                )
            },

            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.96f
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Button(
                        onClick = onPracticeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = belt.color,
                            contentColor =
                                if (belt.color.luminance() < 0.55f) {
                                    Color.White
                                } else {
                                    Color.Black
                                }
                        )
                    ) {
                        Text(
                            text = tr(
                                "תרגול",
                                "Practice"
                            ),
                            style = KmiTypography.action.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }
        ) { padding ->

            // ===== טאבים על פס הגראניט =====
            @Composable
            fun MetricFieldEdgeToEdge(
                title: String,
                number: Int,
                selected: Boolean,
                onClick: () -> Unit,
                modifier: Modifier = Modifier
            ) {
                Box(
                    modifier = modifier
                        .height(70.dp)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 4.dp,
                                vertical = 7.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = KmiTypography.caption.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = number.toString(),
                            style = KmiTypography.action.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .width(30.dp)
                                .height(3.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )
                    }
                }
            }

            val allCount = itemList.size

            val unknownCount by remember {
                derivedStateOf {
                    unknownItems.count()
                }
            }

            val favCount = remember(
                itemList,
                favoriteExerciseIds,
                belt,
                topic,
                allTopicItems
            ) {
                itemList.count { item ->
                    isFavoriteRawItem(item)
                }
            }

            val taughtCount by remember(
                itemList,
                coachStatuses
            ) {
                derivedStateOf {
                    itemList.count { item ->
                        ExerciseCoachStatus.TAUGHT in
                                coachStatuses[item].orEmpty()
                    }
                }
            }

            val practiceCount by remember(
                itemList,
                coachStatuses
            ) {
                derivedStateOf {
                    itemList.count { item ->
                        ExerciseCoachStatus.PRACTICED in
                                coachStatuses[item].orEmpty()
                    }
                }
            }

            val improvementCount by remember(
                itemList,
                coachStatuses
            ) {
                derivedStateOf {
                    itemList.count { item ->
                        ExerciseCoachStatus.NEEDS_REINFORCEMENT in
                                coachStatuses[item].orEmpty()
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        brush = kmiScreenBackgroundBrush()
                    )
            ) {

                // =========================================================
                // טאבים על מלבן הגראניט העליון
                // =========================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .kmiSectionHeaderBackground()
                ) {
                    if (isCoach) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .padding(horizontal = 40.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            MetricFieldEdgeToEdge(
                                title = tr("הכול", "All"),
                                number = allCount,
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(54.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.72f)
                                    )
                            )

                            MetricFieldEdgeToEdge(
                                title = tr("נלמד", "Taught"),
                                number = taughtCount,
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(54.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.72f)
                                    )
                            )

                            MetricFieldEdgeToEdge(
                                title = tr("לתרגול", "Practice"),
                                number = practiceCount,
                                selected = selectedTab == 2,
                                onClick = {
                                    selectedTab = 2
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(54.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.72f)
                                    )
                            )

                            MetricFieldEdgeToEdge(
                                title = tr("לשיפור", "Improve"),
                                number = improvementCount,
                                selected = selectedTab == 3,
                                onClick = {
                                    selectedTab = 3
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .padding(horizontal = 40.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            MetricFieldEdgeToEdge(
                                title = tr("הכול", "All"),
                                number = allCount,
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(54.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.72f)
                                    )
                            )

                            MetricFieldEdgeToEdge(
                                title = tr("לא יודע", "Unknown"),
                                number = unknownCount,
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(54.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.72f)
                                    )
                            )

                            MetricFieldEdgeToEdge(
                                title = tr("מועדפים", "Favorites"),
                                number = favCount,
                                selected = selectedTab == 2,
                                onClick = {
                                    selectedTab = 2
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                val filtered: List<String> by remember(
                    selectedTab,
                    isCoach,
                    itemList,
                    coachStatuses,
                    unknownItems,
                    favoriteExerciseIds,
                    belt,
                    topic
                ) {
                    derivedStateOf {
                        if (isCoach) {
                            when (selectedTab) {
                                1 -> itemList.filter { item ->
                                    ExerciseCoachStatus.TAUGHT in
                                            coachStatuses[item].orEmpty()
                                }

                                2 -> itemList.filter { item ->
                                    ExerciseCoachStatus.PRACTICED in
                                            coachStatuses[item].orEmpty()
                                }

                                3 -> itemList.filter { item ->
                                    ExerciseCoachStatus.NEEDS_REINFORCEMENT in
                                            coachStatuses[item].orEmpty()
                                }

                                else -> itemList
                            }
                        } else {
                            when (selectedTab) {
                                1 -> itemList.filter { item ->
                                    item in unknownItems
                                }

                                2 -> itemList.filter { item ->
                                    isFavoriteRawItem(item)
                                }

                                else -> itemList
                            }
                        }
                    }
                }

                // שמות התצוגה מחושבים פעם אחת לכל הרשימה.
// מעבר בין טאבים וגלילה אינם מחשבים אותם מחדש.
                val displayByRaw: Map<String, String> =
                    remember(
                        itemList,
                        belt,
                        topic,
                        isEnglish
                    ) {
                        itemList.associateWith { raw ->
                            formattedExerciseTitle(raw)
                        }
                    }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 6.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = filtered,
                        key = { index, item ->
                            exerciseIdByRaw[item]
                                ?.let { exerciseId ->
                                    "$exerciseId::$index"
                                }
                                ?: "$item::$index"
                        },
                        contentType = { _, _ ->
                            "exercise_row"
                        }
                    ) { index, item ->

                        var pressed by remember(item) {
                            mutableStateOf(false)
                        }

                        val scale by animateFloatAsState(
                            targetValue = if (pressed) 0.985f else 1f,
                            animationSpec = tween(120),
                            label = "exerciseRowScale"
                        )

                        val displayName = displayByRaw[item]
                            ?: formattedExerciseTitle(item)

                        val isFav = isFavoriteRawItem(item)

                        val itemHasNote =
                            notePresenceByRaw[item] == true

                        val itemIsUnknown = item in unknownItems

                        val itemCoachStatuses =
                            coachStatuses[item]
                                .orEmpty()

                        val materialCoachStatuses =
                            itemCoachStatuses
                                .mapTo(linkedSetOf()) { status ->
                                    when (status) {
                                        ExerciseCoachStatus.TAUGHT ->
                                            CoachMaterialStatus.TAUGHT

                                        ExerciseCoachStatus.PRACTICED ->
                                            CoachMaterialStatus.PRACTICED

                                        ExerciseCoachStatus.NEEDS_REINFORCEMENT ->
                                            CoachMaterialStatus.NEEDS_REINFORCEMENT

                                        ExerciseCoachStatus.NOT_TAUGHT ->
                                            CoachMaterialStatus.NOT_TAUGHT
                                    }
                                }
                                .filterTo(linkedSetOf()) { status ->
                                    status != CoachMaterialStatus.NOT_TAUGHT
                                }

                        val coachStatusKey =
                            coachStatusBaseKey(item)

                        val materialCoachProgress =
                            CoachMaterialProgress(
                                selectedStatuses =
                                    materialCoachStatuses,
                                updatedAtByStatus =
                                    materialCoachStatuses.associateWith { status ->
                                        sp.getLong(
                                            "${coachStatusKey}_${status.storageValue}_updated_at",
                                            0L
                                        )
                                    }
                            )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clickable {
                                    pressed = true
                                    explainFromSearch = item

                                    scope.launch {
                                        kotlinx.coroutines.delay(120)
                                        pressed = false
                                    }
                                },
                            shape = RoundedCornerShape(18.dp),
                            color =
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.96f
                                ),
                            tonalElevation = 0.dp,
                            shadowElevation = 1.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    MaterialTheme.colorScheme.outlineVariant.copy(
                                        alpha = 0.45f
                                    )
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush =
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surface,
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                        .copy(alpha = 0.26f),
                                                    MaterialTheme.colorScheme.surface
                                                )
                                            )
                                    )
                                    .padding(
                                        horizontal = 2.dp,
                                        vertical = 10.dp
                                    )
                            ) {
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides
                                            if (isEnglish) {
                                                LayoutDirection.Ltr
                                            } else {
                                                LayoutDirection.Rtl
                                            }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CompositionLocalProvider(
                                            LocalLayoutDirection provides
                                                    if (isEnglish) {
                                                        LayoutDirection.Ltr
                                                    } else {
                                                        LayoutDirection.Rtl
                                                    }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp),
                                                horizontalAlignment =
                                                    if (isEnglish) {
                                                        Alignment.Start
                                                    } else {
                                                        Alignment.End
                                                    }
                                            ) {
                                                if (isCoach) {
                                                    Row(
                                                        modifier =
                                                            Modifier.fillMaxWidth(),
                                                        verticalAlignment =
                                                            Alignment.Top
                                                    ) {
                                                        Text(
                                                            text = displayName,
                                                            modifier =
                                                                Modifier.weight(1f),
                                                            textAlign =
                                                                if (isEnglish) {
                                                                    TextAlign.Left
                                                                } else {
                                                                    TextAlign.Right
                                                                },
                                                            color =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .onSurface,
                                                            style =
                                                                KmiTypography
                                                                    .cardTitle
                                                                    .copy(
                                                                        fontWeight =
                                                                            FontWeight.Bold
                                                                    ),
                                                            maxLines = 2,
                                                            overflow =
                                                                TextOverflow.Ellipsis
                                                        )

                                                        if (
                                                            isFav ||
                                                            itemHasNote
                                                        ) {
                                                            Spacer(
                                                                Modifier.width(6.dp)
                                                            )

                                                            Column(
                                                                horizontalAlignment =
                                                                    Alignment.CenterHorizontally,
                                                                verticalArrangement =
                                                                    Arrangement.spacedBy(3.dp)
                                                            ) {
                                                                if (isFav) {
                                                                    ExerciseCardsMetaBadge(
                                                                        text =
                                                                            tr(
                                                                                "מועדף",
                                                                                "Favorite"
                                                                            ),
                                                                        containerColor =
                                                                            Color(0xFFF9D9B8),
                                                                        contentColor =
                                                                            Color(0xFF9A5A00)
                                                                    )
                                                                }

                                                                if (itemHasNote) {
                                                                    val isDark =
                                                                        MaterialTheme
                                                                            .colorScheme
                                                                            .surface
                                                                            .luminance() < 0.5f

                                                                    ExerciseCardsMetaBadge(
                                                                        text =
                                                                            tr(
                                                                                "הערה",
                                                                                "Note"
                                                                            ),
                                                                        containerColor =
                                                                            if (isDark) {
                                                                                Color(0xFF5B4A22)
                                                                            } else {
                                                                                Color(0xFFFFE7B3)
                                                                            },
                                                                        contentColor =
                                                                            if (isDark) {
                                                                                Color(0xFFFFD978)
                                                                            } else {
                                                                                Color(0xFF8A5A00)
                                                                            }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Text(
                                                        text = displayName,
                                                        modifier =
                                                            Modifier.fillMaxWidth(),
                                                        textAlign =
                                                            if (isEnglish) {
                                                                TextAlign.Left
                                                            } else {
                                                                TextAlign.Right
                                                            },
                                                        color =
                                                            MaterialTheme
                                                                .colorScheme
                                                                .onSurface,
                                                        style =
                                                            KmiTypography
                                                                .cardTitle
                                                                .copy(
                                                                    fontWeight =
                                                                        FontWeight.Bold
                                                                ),
                                                        maxLines = 2,
                                                        overflow =
                                                            TextOverflow.Ellipsis
                                                    )

                                                    if (
                                                        itemHasNote ||
                                                        isFav ||
                                                        itemIsUnknown
                                                    ) {
                                                        Spacer(
                                                            Modifier.height(9.dp)
                                                        )

                                                        Row(
                                                            modifier =
                                                                Modifier.fillMaxWidth(),
                                                            horizontalArrangement =
                                                                if (isEnglish) {
                                                                    Arrangement.Start
                                                                } else {
                                                                    Arrangement.End
                                                                },
                                                            verticalAlignment =
                                                                Alignment.CenterVertically
                                                        ) {
                                                            if (itemIsUnknown) {
                                                                Surface(
                                                                    shape =
                                                                        RoundedCornerShape(
                                                                            999.dp
                                                                        ),
                                                                    color =
                                                                        MaterialTheme
                                                                            .colorScheme
                                                                            .errorContainer,
                                                                    tonalElevation = 0.dp,
                                                                    shadowElevation = 0.dp
                                                                ) {
                                                                    Text(
                                                                        text =
                                                                            tr(
                                                                                "●  לא יודע",
                                                                                "●  Unknown"
                                                                            ),
                                                                        modifier =
                                                                            Modifier.padding(
                                                                                horizontal =
                                                                                    10.dp,
                                                                                vertical =
                                                                                    4.dp
                                                                            ),
                                                                        color =
                                                                            MaterialTheme
                                                                                .colorScheme
                                                                                .error,
                                                                        style =
                                                                            KmiTypography
                                                                                .caption
                                                                                .copy(
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
                                        }

                                        if (isCoach) {
                                            Spacer(
                                                modifier = Modifier.height(8.dp)
                                            )

                                            ExerciseCardsCoachStatusRow(
                                                progress = materialCoachProgress,
                                                isEnglish = isEnglish,
                                                isFav = isFav,
                                                hasNote = itemHasNote,
                                                onInfo = {
                                                    pressed = true
                                                    explainFromSearch = item

                                                    scope.launch {
                                                        kotlinx.coroutines.delay(120)
                                                        pressed = false
                                                    }
                                                },
                                                onToggleFavorite = {
                                                    toggleFavorite(item)
                                                },
                                                onEditNote = {
                                                    noteEditorFor = item
                                                    noteDraft = loadNote(item)
                                                },
                                                onSelect = { status ->
                                                    val exerciseStatus =
                                                        when (status) {
                                                            CoachMaterialStatus.TAUGHT ->
                                                                ExerciseCoachStatus.TAUGHT

                                                            CoachMaterialStatus.PRACTICED ->
                                                                ExerciseCoachStatus.PRACTICED

                                                            CoachMaterialStatus.NEEDS_REINFORCEMENT ->
                                                                ExerciseCoachStatus.NEEDS_REINFORCEMENT

                                                            CoachMaterialStatus.NOT_TAUGHT ->
                                                                ExerciseCoachStatus.NOT_TAUGHT
                                                        }

                                                    updateCoachStatus(
                                                        raw = item,
                                                        status = exerciseStatus
                                                    )
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

            // ===== דיאלוג הסבר (לחיצה על שורה או אייקון info ברשימה) =====
            explainFromSearch?.let { item ->

                val displayName = formattedExerciseTitle(item)

                LaunchedEffect(item) {
                    KmiTtsManager.init(ctx)
                }
                DisposableEffect(item) {
                    onDispose { KmiTtsManager.stop() }
                }

                val explanation = remember(belt, topic, item, displayName, isEnglish) {
                    val itemTopic = topicForRawItem(item)

                    val resolved = ExerciseExplanationResolver.get(
                        belt = belt,
                        topic = itemTopic,
                        item = displayName,
                        isEnglish = isEnglish
                    ).trim()

                    val cleaned = if ("::" in resolved) {
                        resolved
                            .split("::")
                            .map { it.trim() }
                            .lastOrNull { it.isNotBlank() }
                            ?: resolved
                    } else {
                        resolved
                    }.trim()

                    val isFallback = if (isEnglish) {
                        cleaned.isBlank() ||
                                cleaned.startsWith("Detailed explanation for:") ||
                                cleaned.startsWith("There is currently no explanation")
                    } else {
                        cleaned.isBlank() ||
                                cleaned.startsWith("הסבר מפורט על") ||
                                cleaned.startsWith("אין כרגע")
                    }

                    if (!isFallback) {
                        cleaned
                    } else {
                        tr(
                            "לא נמצא הסבר עבור \"$displayName\".",
                            "No explanation found for \"$displayName\"."
                        )
                    }
                }

                val isFav = isFavoriteRawItem(item)
                val noteText = remember(item, notesRefreshKey) {
                    loadNote(item)
                }

                ExerciseExplanationDialog(
                    title = displayName,
                    beltLabel = if (isEnglish) "(${belt.en})" else "(${belt.heb})",
                    explanation = explanation,
                    noteText = noteText,
                    isFavorite = isFav,
                    accentColor = belt.color,
                    isEnglish = isEnglish,
                    onDismiss = {
                        KmiTtsManager.stop()
                        explainFromSearch = null
                    },
                    onEditNote = {
                        noteEditorFor = item
                        noteDraft = loadNote(item)
                    },
                    onDeleteNote = {
                        deleteNote(item)
                    },
                    onToggleFavorite = {
                        toggleFavorite(item)
                    }
                )
            }

            noteEditorFor?.let { item ->
                ExerciseNoteEditorDialog(
                    exerciseTitle = formattedExerciseTitle(item),
                    noteText = noteDraft,
                    isEnglish = isEnglish,
                    accentColor = belt.color,
                    onNoteChange = { noteDraft = it },
                    onDismiss = {
                        noteEditorFor = null
                    },
                    onSave = {
                        saveNote(item, noteDraft)
                        noteEditorFor = null
                    }
                )
            }
        } // ✅ סוגר את Scaffold { padding -> ... }
    }
}

@Composable
private fun ExerciseRowActionsMenu(
    isEnglish: Boolean,
    isCoach: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    isUnknown: Boolean,
    coachStatuses: Set<ExerciseCoachStatus>,
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNote: () -> Unit,
    onToggleUnknown: () -> Unit,
    onCoachStatusChange: (ExerciseCoachStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val infoScale by animateFloatAsState(
        targetValue = if (expanded) 1.08f else 1f,
        animationSpec = tween(180),
        label = "exerciseInfoScale"
    )

    val infoRotation by animateFloatAsState(
        targetValue = if (expanded) 12f else 0f,
        animationSpec = tween(180),
        label = "exerciseInfoRotation"
    )

    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    Box(
        modifier = modifier
    ) {
        Surface(
            onClick = {
                expanded = true
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.28f)
            ),
            modifier = Modifier
                .size(KmiIconSize.medium)
                .graphicsLayer {
                    scaleX = infoScale
                    scaleY = infoScale
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    color =
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    style = KmiTypography.action.copy(
                        fontWeight = FontWeight.Black
                    ),
                    modifier = Modifier.graphicsLayer {
                        rotationZ = infoRotation
                    }
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                                .copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.dp,
                    color =
                        MaterialTheme.colorScheme.outline
                            .copy(alpha = 0.28f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = tr("מידע", "Info"),
                        style = KmiTypography.action
                    )
                },
                onClick = {
                    expanded = false
                    onInfo()
                }
            )

            if (isCoach) {
                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme.outline
                            .copy(alpha = 0.28f)
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = tr(
                                if (coachStatuses.isEmpty()) {
                                    "✓ לא נלמד"
                                } else {
                                    "לא נלמד"
                                },
                                if (coachStatuses.isEmpty()) {
                                    "✓ Not taught"
                                } else {
                                    "Not taught"
                                }
                            ),
                            style = KmiTypography.action,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    },
                    onClick = {
                        expanded = false

                        onCoachStatusChange(
                            ExerciseCoachStatus.NOT_TAUGHT
                        )
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = tr(
                                if (
                                    ExerciseCoachStatus.TAUGHT in
                                    coachStatuses
                                ) {
                                    "✓ נלמד"
                                } else {
                                    "נלמד"
                                },
                                if (
                                    ExerciseCoachStatus.TAUGHT in
                                    coachStatuses
                                ) {
                                    "✓ Taught"
                                } else {
                                    "Taught"
                                }
                            ),
                            style = KmiTypography.action,
                            color = Color(0xFF2E7D32)
                        )
                    },
                    onClick = {
                        expanded = false

                        onCoachStatusChange(
                            ExerciseCoachStatus.TAUGHT
                        )
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = tr(
                                if (
                                    ExerciseCoachStatus.PRACTICED in
                                    coachStatuses
                                ) {
                                    "✓ לתרגול"
                                } else {
                                    "לתרגול"
                                },
                                if (
                                    ExerciseCoachStatus.PRACTICED in
                                    coachStatuses
                                ) {
                                    "✓ Practice"
                                } else {
                                    "Practice"
                                }
                            ),
                            style = KmiTypography.action,
                            color = Color(0xFFF57C00)
                        )
                    },
                    onClick = {
                        expanded = false

                        onCoachStatusChange(
                            ExerciseCoachStatus.PRACTICED
                        )
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = tr(
                                if (
                                    ExerciseCoachStatus.NEEDS_REINFORCEMENT in
                                    coachStatuses
                                ) {
                                    "✓ לשיפור"
                                } else {
                                    "לשיפור"
                                },
                                if (
                                    ExerciseCoachStatus.NEEDS_REINFORCEMENT in
                                    coachStatuses
                                ) {
                                    "✓ Needs improvement"
                                } else {
                                    "Needs improvement"
                                }
                            ),
                            style = KmiTypography.action,
                            color = Color(0xFFC62828)
                        )
                    },
                    onClick = {
                        expanded = false

                        onCoachStatusChange(
                            ExerciseCoachStatus.NEEDS_REINFORCEMENT
                        )
                    }
                )
            } else {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = tr(
                                if (isFav) {
                                    "הסר ממועדפים"
                                } else {
                                    "הוסף למועדפים"
                                },
                                if (isFav) {
                                    "Remove from favorites"
                                } else {
                                    "Add to favorites"
                                }
                            ),
                            style = KmiTypography.action
                        )
                    },
                    onClick = {
                        expanded = false
                        onToggleFavorite()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = tr(
                                if (hasNote) {
                                    "ערוך / מחק הערה"
                                } else {
                                    "הוסף הערה לתרגיל"
                                },
                                if (hasNote) {
                                    "Edit / delete note"
                                } else {
                                    "Add note"
                                }
                            ),
                            style = KmiTypography.action
                        )
                    },
                    onClick = {
                        expanded = false
                        onEditNote()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = tr(
                                if (isUnknown) {
                                    "בטל לא יודע"
                                } else {
                                    "סמן כלא יודע"
                                },
                                if (isUnknown) {
                                    "Remove unknown mark"
                                } else {
                                    "Mark as unknown"
                                }
                            ),
                            style = KmiTypography.action
                        )
                    },
                    onClick = {
                        expanded = false
                        onToggleUnknown()
                    }
                )
            }
        }
    }
}

// ========= כפתור מונפש לשימוש חוזר =========
@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    brush: Brush,
    contentColor: Color,
    onClick: () -> Unit
) {
    var pressed by remember {
        mutableStateOf(false)
    }

    val scale by animateFloatAsState(
        targetValue =
            if (pressed) {
                0.96f
            } else {
                1f
            },
        animationSpec = tween(120),
        label = "btnScale"
    )

    val scope = rememberCoroutineScope()

    Surface(
        onClick = {
            pressed = true
            onClick()

            scope.launch {
                kotlinx.coroutines.delay(140)
                pressed = false
            }
        },
        modifier = modifier
            .scale(scale)
            .heightIn(min = 60.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = contentColor.copy(alpha = 0.20f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = brush,
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    text = text,
                    style = KmiTypography.action.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ========= עזר: הדגשת "עמידת מוצא ..." עד פסיק/נקודה =========
private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val marker = "עמידת מוצא"

    // אם אין בכלל "עמידת מוצא" – מחזירים טקסט רגיל
    val idx = source.indexOf(marker)
    if (idx < 0) return AnnotatedString(source)

    // מחפשים סוף משפט: פסיק או נקודה אחרי "עמידת מוצא"
    val sentenceEndExclusive = run {
        val endIdx = source.indexOfAny(charArrayOf('.', ','), startIndex = idx)
        if (endIdx == -1) source.length else endIdx + 1   // כולל הפסיק/נקודה
    }

    val before = source.substring(0, idx)
    val stanceSentence = source.substring(idx, sentenceEndExclusive)
    val after = source.substring(sentenceEndExclusive)

    return buildAnnotatedString {
        // מה שלפני
        append(before)

        // המשפט של "עמידת מוצא ..." מודגש וצבוע
        val stanceStart = length
        append(stanceSentence)
        val stanceEnd = length

        addStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = stanceColor
            ),
            start = stanceStart,
            end = stanceEnd
        )

        // שאר ההסבר
        append(after)
    }
}
