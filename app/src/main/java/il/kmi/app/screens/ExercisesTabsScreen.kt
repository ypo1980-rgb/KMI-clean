@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package il.kmi.app.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.collectAsState
import il.kmi.app.domain.CanonicalIds
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.domain.ContentRepo
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import il.kmi.app.ui.ext.color
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

//==============================================================================

private data class ExerciseCardsPdfItem(
    val title: String,
    val status: Boolean?,
    val isFavorite: Boolean
)

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
    val contentBottom = pageHeight - 58f

    val document = android.graphics.pdf.PdfDocument()

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val mediumBlue = android.graphics.Color.rgb(36, 103, 158)
    val lightHeaderBlue = android.graphics.Color.rgb(128, 183, 220)
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

    val titlePaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = android.graphics.Color.WHITE
        textSize = 26f
        typeface = boldTypeface
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
    }

    val subtitlePaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = android.graphics.Color.WHITE
        textSize = 12.5f
        typeface = regularTypeface
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
    }

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
        canvas.drawColor(android.graphics.Color.WHITE)

        val headerBottom = 122f

        val navyPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            style = android.graphics.Paint.Style.FILL
        }

        val mediumStripePaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = mediumBlue
            style = android.graphics.Paint.Style.FILL
        }

        val lightStripePaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = lightHeaderBlue
            style = android.graphics.Paint.Style.FILL
        }

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(pageWidth.toFloat(), 0f)
                lineTo(pageWidth.toFloat(), headerBottom)
                lineTo(178f, headerBottom)
                lineTo(238f, 0f)
                close()
            },
            navyPaint
        )

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(208f, headerBottom)
                lineTo(224f, headerBottom)
                lineTo(284f, 0f)
                lineTo(268f, 0f)
                close()
            },
            mediumStripePaint
        )

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(230f, headerBottom)
                lineTo(238f, headerBottom)
                lineTo(298f, 0f)
                lineTo(290f, 0f)
                close()
            },
            lightStripePaint
        )

        val logoCenterX = 78f
        val logoCenterY = 58f
        val logoRadius = 42f

        val logoOuterPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            style = android.graphics.Paint.Style.FILL
        }

        val logoInnerPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }

        val logoTextPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            textSize = logoRadius * 0.62f
            typeface = boldTypeface
            textAlign = android.graphics.Paint.Align.CENTER
        }

        canvas.drawCircle(
            logoCenterX,
            logoCenterY,
            logoRadius,
            logoOuterPaint
        )

        canvas.drawCircle(
            logoCenterX,
            logoCenterY,
            logoRadius - 4f,
            logoInnerPaint
        )

        canvas.drawText(
            "KAMI",
            logoCenterX,
            logoCenterY + logoRadius * 0.22f,
            logoTextPaint
        )

        val headerTextX = if (isEnglish) 308f else 435f

        canvas.drawText(
            if (isEnglish) {
                "Exercise Cards Report"
            } else {
                "דו״ח כרטיסיות תרגילים"
            },
            headerTextX,
            50f,
            titlePaint
        )

        canvas.drawText(
            if (isEnglish) {
                "${beltLabel()} · $topicTitle"
            } else {
                "${beltLabel()} · $topicTitle"
            },
            headerTextX,
            76f,
            subtitlePaint
        )

        if (!subTopicTitle.isNullOrBlank()) {
            canvas.drawText(
                if (isEnglish) {
                    "Sub-topic: $subTopicTitle"
                } else {
                    "תת־נושא: $subTopicTitle"
                },
                headerTextX,
                98f,
                subtitlePaint
            )
        }

        val generatedDate = java.text.SimpleDateFormat(
            "dd/MM/yyyy",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        smallPaint.textAlign = android.graphics.Paint.Align.RIGHT

        canvas.drawText(
            if (isEnglish) {
                "Generated: $generatedDate"
            } else {
                "תאריך הפקה: $generatedDate"
            },
            pageWidth - 34f,
            142f,
            smallPaint
        )

        y = 174f
    }

    fun drawFooter() {
        val dividerPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = rowBorder
            strokeWidth = 1f
        }

        canvas.drawLine(
            contentLeft,
            pageHeight - 42f,
            contentRight,
            pageHeight - 42f,
            dividerPaint
        )

        smallPaint.textAlign = android.graphics.Paint.Align.CENTER

        canvas.drawText(
            if (isEnglish) {
                "Page $pageNumber · KAMI"
            } else {
                "עמוד $pageNumber · KAMI"
            },
            pageWidth / 2f,
            pageHeight - 24f,
            smallPaint
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

    val safeBeltId = belt.id
        .lowercase()
        .replace(Regex("[^a-z0-9_-]"), "_")

    val outputFile = java.io.File(
        outputDirectory,
        "exercise_cards_${safeBeltId}_${System.currentTimeMillis()}.pdf"
    )

    try {
        java.io.FileOutputStream(outputFile).use { output ->
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
    onSearch: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val scroll = rememberScrollState()
    val sp = remember { ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }
    val notesSp = remember { ctx.getSharedPreferences("kmi_notes", android.content.Context.MODE_PRIVATE) }
// ⭐ Favorites גלובלי – source of truth אחד לכל האפליקציה
    val favorites: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    // ✅ רענון סימוני יודע/לא יודע שהגיעו ממסכים אחרים, כולל MaterialsScreen
    val marksVersion by vm.marksVersion.collectAsState()

    fun readSet(key: String): MutableSet<String> =
        sp.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()

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
        try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

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
                a.startsWith(wanted) || wanted.startsWith(a) || a.contains(wanted) || wanted.contains(a)
            }
            if (loose != null) {
                val items = ContentRepo.listItemTitles(belt, topic, subTopicTitle = loose)
                if (items.isNotEmpty()) return@remember items
            }

            // 2) fallback: KmiSearchBridge (רק אם עדיין קיים אצלך)
            val bySubBridge = runCatching { il.kmi.app.search.KmiSearchBridge.itemsFor(belt, subRaw) }
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
    // --- מצב טאבים (0=הכל, 1=לא יודע, 2=מועדפים) — חייב להיות לפני ה-Scaffold ---
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    fun String.norm() = this
        .replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .trim().lowercase()

    // אין יותר searchResults מקומי — החיפוש הגלובלי נמצא ב-KmiTopBar

    // מזהה תרגיל "אחיד" – בלי prefix של נושא וכו'
    fun normalizeItemId(raw: String): String =
        raw.substringAfter("::", raw)
            .substringAfter(":", raw)
            .trim()

    fun normalizeStatusPart(raw: String): String =
        raw.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun formattedExerciseTitle(raw: String): String {
        val formatted = ExerciseTitleFormatter
            .displayName(raw)
            .toString()
            .trim()

        return formatted
            .takeIf { value: String -> value.isNotBlank() && value != "null" }
            ?: raw.trim()
    }

    fun noteKeyFor(raw: String): String = "note_${belt.id}_${normalizeItemId(raw)}"

    fun loadNote(raw: String): String =
        notesSp.getString(noteKeyFor(raw), "")?.trim().orEmpty()

    fun saveNote(raw: String, value: String) {
        val clean = value.trim()

        notesSp.edit().apply {
            if (clean.isBlank()) {
                remove(noteKeyFor(raw))
            } else {
                putString(noteKeyFor(raw), clean)
            }
        }.apply()

        notesRefreshKey++
    }

    fun deleteNote(raw: String) {
        notesSp.edit()
            .remove(noteKeyFor(raw))
            .apply()

        notesRefreshKey++
    }

    fun hasNote(raw: String): Boolean {
        notesRefreshKey
        return loadNote(raw).isNotBlank()
    }

    // סטטוסים מה-VM
    val itemStates = remember(belt.id, topic, subTopicFilter) { mutableStateMapOf<String, Boolean?>() }
    // ✅ אם זה __ALL__ צריך לדעת לאיזה נושא שייך כל item כדי לקרוא סטטוס נכון מה-VM
    fun topicForRawItem(raw: String): String {
        if (topic != "__ALL__") return topic
        return allTopicItems.firstOrNull { it.items.contains(raw) }?.topic ?: topic
    }

    LaunchedEffect(belt, topic, subTopicFilter, itemList, allTopicItems) {
        itemStates.clear()

        itemList.forEach { raw ->
            val tp = topicForRawItem(raw)

            // ✅ קריטי: משתמשים באותו canonicalId שהשאר האפליקציה שומרת
            val canonicalId = CanonicalIds.canonicalFor(belt, tp, raw)

            val v = runCatching { vm.getItemStatusNullable(belt, tp, canonicalId) }.getOrNull()
                ?: runCatching { if (vm.isMastered(belt, tp, canonicalId)) true else null }.getOrNull()

            itemStates[raw] = v
        }
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

    fun unknownCandidateIdsFor(raw: String): Set<String> {
        val tp = topicForRawItem(raw)
        val cleanId = normalizeItemId(raw)
        val displayName = formattedExerciseTitle(raw)

        val canonicalFromRaw = CanonicalIds.canonicalFor(belt, tp, raw)
        val canonicalFromClean = CanonicalIds.canonicalFor(belt, tp, cleanId)
        val canonicalFromDisplay = CanonicalIds.canonicalFor(belt, tp, displayName)

        return buildSet {
            add(raw.trim())
            add(cleanId)
            add(displayName)
            add(canonicalFromRaw)
            add(canonicalFromClean)
            add(canonicalFromDisplay)
        }.filter { it.isNotBlank() }.toSet()
    }

    fun isUnknownRawItem(raw: String): Boolean {
        val candidates = unknownCandidateIdsFor(raw)
        val cleanRaw = normalizeStatusPart(raw)
        val cleanDisplay = normalizeStatusPart(
            formattedExerciseTitle(raw)
        )

        return unknowns.any { storedRaw ->
            val stored = storedRaw.trim()
            val storedNormalized = normalizeItemId(stored)

            stored in candidates ||
                    storedNormalized in candidates ||
                    candidates.contains(storedNormalized) ||
                    (
                            stored.startsWith("status_${belt.id}_") &&
                                    (
                                            stored.endsWith("_$cleanRaw") ||
                                                    stored.endsWith("_$cleanDisplay") ||
                                                    stored.contains(cleanRaw) ||
                                                    stored.contains(cleanDisplay)
                                            )
                            )
        }
    }

    fun toggleFavorite(rawId: String) {
        FavoritesStore.toggle(normalizeItemId(rawId))
    }

    /**
     * סימון/הסרה ממועדפים
     */
      /**
     * סימון/הסרה "לא יודע"
     */
      fun setUnknown(id: String, set: Boolean) {
          val cleanId = normalizeItemId(id)

          fun removeMatchingUnknowns(
              setToClean: MutableSet<String>,
              raw: String,
              canonicalId: String
          ) {
              val cleanRaw = normalizeStatusPart(raw)
              val cleanDisplay = normalizeStatusPart(
                  formattedExerciseTitle(raw)
              )

              setToClean.remove(cleanId)
              setToClean.remove(raw)
              setToClean.remove(canonicalId)

              setToClean.removeAll { stored ->
                  stored.trim() == cleanId ||
                          stored.trim() == raw.trim() ||
                          stored.trim() == canonicalId ||
                          (
                                  stored.startsWith("status_${belt.id}_") &&
                                          (
                                                  stored.endsWith("_$cleanRaw") ||
                                                          stored.endsWith("_$cleanDisplay") ||
                                                          stored.contains(cleanRaw) ||
                                                          stored.contains(cleanDisplay)
                                                  )
                                  )
              }
          }

          if (topic == "__ALL__") {
              val nextUnknowns = unknowns.toMutableSet()

              allTopicItems.forEach { ti ->
                  val matchedItems = ti.items.filter { raw ->
                      normalizeItemId(raw) == cleanId ||
                              raw.trim() == id.trim() ||
                              CanonicalIds.canonicalFor(belt, ti.topic, raw) == id.trim()
                  }

                  matchedItems.forEach { raw ->
                      val canonicalId = CanonicalIds.canonicalFor(belt, ti.topic, raw)
                      val key = "unknown_${belt.id}_${ti.topic}"

                      val s = readSet(key)

                      if (set) {
                          s.add(cleanId)
                          s.add(canonicalId)

                          nextUnknowns.add(cleanId)
                          nextUnknowns.add(canonicalId)

                          vm.setItemStatusNullable(
                              belt = belt,
                              topic = ti.topic,
                              item = canonicalId,
                              value = false
                          )
                      } else {
                          removeMatchingUnknowns(s, raw, canonicalId)
                          removeMatchingUnknowns(nextUnknowns, raw, canonicalId)

                          vm.setItemStatusNullable(
                              belt = belt,
                              topic = ti.topic,
                              item = canonicalId,
                              value = null
                          )
                      }

                      sp.edit()
                          .putStringSet(key, s)
                          .apply()
                  }
              }

              unknowns = nextUnknowns
          } else {
              val tp = topicForRawItem(id)
              val canonicalId = CanonicalIds.canonicalFor(belt, tp, id)
              val key = "unknown_${belt.id}_$suffix"

              val s = readSet(key)

              if (set) {
                  s.add(cleanId)
                  s.add(canonicalId)

                  vm.setItemStatusNullable(
                      belt = belt,
                      topic = tp,
                      item = canonicalId,
                      value = false
                  )
              } else {
                  removeMatchingUnknowns(s, id, canonicalId)

                  vm.setItemStatusNullable(
                      belt = belt,
                      topic = tp,
                      item = canonicalId,
                      value = null
                  )
              }

              unknowns = s.toMutableSet()

              sp.edit()
                  .putStringSet(key, s)
                  .apply()
          }
      }

    val pdfFilteredItems: List<String> = when (selectedTab) {
        1 -> itemList.filter { rawItem ->
            isUnknownRawItem(rawItem)
        }

        2 -> itemList.filter { rawItem ->
            normalizeItemId(rawItem) in favorites
        }

        else -> itemList
    }

    val pdfItems: List<ExerciseCardsPdfItem> = pdfFilteredItems
        .map { rawItem ->
            val status: Boolean? = when {
                isUnknownRawItem(rawItem) -> false
                itemStates[rawItem] == true -> true
                itemStates[rawItem] == false -> false
                else -> null
            }

            ExerciseCardsPdfItem(
                title = formattedExerciseTitle(rawItem),
                status = status,
                isFavorite = normalizeItemId(rawItem) in favorites
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

    val pdfTabTitle = when (selectedTab) {
        1 -> tr("לא יודע", "Unknown")
        2 -> tr("מועדפים", "Favorites")
        else -> tr("הכל", "All")
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

    Scaffold(
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
                showTopShare = false,
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
                color = Color(0xFFE0E0E0),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {                    // ← קובע את מקור התרגול לפי הטאב: הכל/לא-יודע/מועדפים
                    val practiceToken = when (selectedTab) {
                        1 -> "__UNKNOWN__"
                        2 -> "__FAVS_ALL__"
                        else -> {
                            // ✅ אם נכנסנו דרך נושא מסוים — מתרגלים את הנושא הזה
                            if (topic != "__ALL__") topic else "__ALL__"
                        }
                    }

                    ActionButton(
                        text = tr("תרגול", "Practice"),
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFF6F64FF),
                        onClick = { onPractice(belt, practiceToken) }
                    )
                    ActionButton(
                        text = tr("איפוס", "Reset"),
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFFD32F2F),
                        onClick = {
                            scope.launch {
                                // איפוס סטטוסים בזיכרון הקומפוז
                                itemList.forEach { item -> itemStates[item] = null }

                                // ⭐ איפוס מועדפים גלובלי
                                FavoritesStore.clearAll()

                                // ❓ איפוס unknown – נשאר מקומי לפי חגורה/נושא
                                unknowns = mutableSetOf()

                                val editor = sp.edit()

                                if (topic == "__ALL__") {
                                    // ✅ 1) מחיקת unknown keys מה-SP (כמו שהיה)
                                    sp.all.keys
                                        .filter { it.startsWith("unknown_${belt.id}_") }
                                        .forEach { key -> editor.remove(key) }

                                    // ✅ 2) איפוס אמיתי של הסימונים (DataStore) לכל נושא
                                    allTopicItems.forEach { ti ->
                                        val canonicalIds = ti.items
                                            .map { raw -> CanonicalIds.canonicalFor(belt, ti.topic, raw) }
                                            .distinct()

                                        vm.clearTopicItems(
                                            belt = belt,
                                            topic = ti.topic,
                                            canonicalIds = canonicalIds
                                        )
                                    }
                                } else {
                                    val singleUnknownKey = "unknown_${belt.id}_$suffix"
                                    editor.remove(singleUnknownKey)

                                    vm.clearTopic(belt, topic)
                                }

                                editor.apply()
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->

        // ===== טאבים "מקצה-לקצה" =====
        @Composable
        fun MetricFieldEdgeToEdge(
            title: String,
            number: Int,
            selected: Boolean,
            onClick: () -> Unit,
            modifier: Modifier = Modifier
        ) {
            val baseBg    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            val selBg     = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            val borderCol = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
            else MaterialTheme.colorScheme.outlineVariant

            Box(
                modifier = modifier
                    .height(64.dp)
                    .background(if (selected) selBg else baseBg, shape = RectangleShape)
                    .border(1.dp, borderCol, RectangleShape)
                    .clickable(onClick = onClick)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = number.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        val allCount     = itemList.size
        // ✅ תומך גם ב-cleanId, גם ב-canonicalId וגם ב-statusId שמגיע מ-MaterialsScreen
        val unknownCount = itemList.count { isUnknownRawItem(it) }
        val favCount     = itemList.count { normalizeItemId(it) in favorites }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricFieldEdgeToEdge(
                    title    = tr("הכל", "All"),
                    number   = allCount,
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                MetricFieldEdgeToEdge(
                    title    = tr("לא יודע", "Unknown"),
                    number   = unknownCount,
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
                MetricFieldEdgeToEdge(
                    title    = tr("מועדפים", "Favorites"),
                    number   = favCount,
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            val filtered = when (selectedTab) {
                1 -> itemList.filter { isUnknownRawItem(it) }
                2 -> itemList.filter { normalizeItemId(it) in favorites }
                else -> itemList
            }

// ✅ מפת “raw -> display” אחת, שמשמשת לכל ה-UI
            val displayByRaw: Map<String, String> = remember(filtered) {
                filtered.associateWith { raw: String ->
                    formattedExerciseTitle(raw)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filtered.forEach { item ->
                    val bringer = remember { BringIntoViewRequester() }
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (pressed) 1.15f else 1f, label = "scale")

                    val tpForUi = topicForRawItem(item)
                    val displayName = CanonicalIds.uiDisplayName(tpForUi, item)
                    val isFav = favorites.contains(normalizeItemId(item))
                    val itemHasNote = remember(item, notesRefreshKey) {
                        hasNote(item)
                    }

                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .bringIntoViewRequester(bringer),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExerciseRowActionsMenu(
                                isEnglish = isEnglish,
                                isFav = isFav,
                                hasNote = itemHasNote,
                                isUnknown = isUnknownRawItem(item),
                                onInfo = {
                                    pressed = true
                                    explainFromSearch = item
                                    scope.launch {
                                        kotlinx.coroutines.delay(150)
                                        pressed = false
                                    }
                                },
                                onToggleFavorite = {
                                    FavoritesStore.toggle(item)
                                },
                                onEditNote = {
                                    noteEditorFor = item
                                    noteDraft = loadNote(item)
                                },
                                onToggleUnknown = {
                                    setUnknown(item, !isUnknownRawItem(item))
                                },
                                modifier = Modifier.scale(scale)
                            )

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { explainFromSearch = item },
                                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                            ) {
                                Text(
                                    text = displayName,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                if (itemHasNote) {
                                    Text(
                                        text = tr("יש הערה שמורה", "Saved note exists"),
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
                                    )
                                }
                            }
                        }
                    }

                    Divider()
                }
            }
        }

        // אין כאן יותר דיאלוג מתוצאת חיפוש גלובלי.
        // החיפוש, ההסבר, המועדפים והערות המשתמש מטופלים דרך KmiTopBar.

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

            val isFav = favorites.contains(normalizeItemId(item))
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

} // ✅ סוגר את ExercisesTabsScreen(...)


@Composable
private fun ExerciseRowActionsMenu(
    isEnglish: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    isUnknown: Boolean,
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNote: () -> Unit,
    onToggleUnknown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

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

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            ),
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = infoScale
                    scaleY = infoScale
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = tr("פעולות לתרגיל", "Exercise actions"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            rotationZ = infoRotation
                        }
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.99f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.97f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(18.dp)
                )
        ) {
            DropdownMenuItem(
                text = { Text(tr("מידע", "Info"), style = MaterialTheme.typography.labelLarge) },
                onClick = {
                    expanded = false
                    onInfo()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        tr(
                            if (isFav) "הסר ממועדפים" else "הוסף למועדפים",
                            if (isFav) "Remove from favorites" else "Add to favorites"
                        ),
                        style = MaterialTheme.typography.labelLarge
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
                        tr(
                            if (hasNote) "ערוך / מחק הערה" else "הוסף הערה לתרגיל",
                            if (hasNote) "Edit / delete note" else "Add note"
                        ),
                        style = MaterialTheme.typography.labelLarge
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
                        tr(
                            if (isUnknown) "בטל לא יודע" else "סמן כלא יודע",
                            if (isUnknown) "Remove unknown mark" else "Mark as unknown"
                        ),
                        style = MaterialTheme.typography.labelLarge
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

// ========= כפתור מונפש לשימוש חוזר =========
@Composable
fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "btnScale")
    val scope = rememberCoroutineScope()
    val contentOnContainer = if (containerColor.luminance() < 0.5f) Color.White else Color.Black

    Button(
        onClick = {
            pressed = true; onClick()
            scope.launch { kotlinx.coroutines.delay(150); pressed = false }
        },
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.scale(scale).height(56.dp).defaultMinSize(minWidth = 90.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentOnContainer)
    ) {
        Text(text, color = contentOnContainer, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
