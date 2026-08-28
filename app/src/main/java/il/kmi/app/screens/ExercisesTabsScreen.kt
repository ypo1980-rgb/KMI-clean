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
import androidx.compose.ui.graphics.luminance
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
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.KmiIconSize
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
import androidx.core.content.edit
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

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
    PRACTICE("practice"),
    NEEDS_IMPROVEMENT("needs_improvement");

    companion object {
        fun fromStorage(value: String?): ExerciseCoachStatus {
            return entries.firstOrNull { status ->
                status.storageValue == value
            } ?: NOT_TAUGHT
        }
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

    val currentRole = sp
        .getString("user_role", "")
        .orEmpty()
        .trim()
        .lowercase()

    val isCoach =
        currentRole == "coach" ||
                currentRole.contains("coach") ||
                currentRole.contains("מאמן") ||
                currentRole.contains("מדריך")

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

    fun isFavoriteRawItem(raw: String): Boolean {
        return exerciseIdentityIdFor(raw) in favoriteExerciseIds
    }

    fun noteKeyFor(raw: String): String =
        "note_${belt.id}_${exerciseIdentityIdFor(raw)}"

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

    fun hasNote(raw: String): Boolean {
        notesRefreshKey
        return loadNote(raw).isNotBlank()
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
        itemStates.clear()

        itemList.forEach { raw ->
            val itemTopic = topicForRawItem(raw)
            val canonicalId = CanonicalIds.canonicalFor(
                belt,
                itemTopic,
                raw
            )

            val status = runCatching {
                vm.getItemStatusNullable(
                    belt = belt,
                    topic = itemTopic,
                    item = canonicalId
                )
            }.getOrNull()
                ?: runCatching {
                    if (
                        vm.isMastered(
                            belt = belt,
                            topic = itemTopic,
                            item = canonicalId
                        )
                    ) {
                        true
                    } else {
                        null
                    }
                }.getOrNull()

            itemStates[raw] = status
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
        unknowns,
        itemStates.toMap(),
        belt,
        topic,
        subTopicFilter,
        marksVersion
    ) {
        derivedStateOf {
            itemList.filterTo(linkedSetOf()) { rawItem ->
                isUnknownForCards(rawItem)
            }
        }
    }

    fun coachStatusKey(raw: String): String {
        val itemTopic = topicForRawItem(raw)

        val canonicalId = CanonicalIds.canonicalFor(
            belt,
            itemTopic,
            raw
        )

        val statusTopicKey =
            if (!subTopicFilter.isNullOrBlank() && topic != "__ALL__") {
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
            append(canonicalId)
            append("_status")
        }
    }

    fun loadCoachStatus(raw: String): ExerciseCoachStatus {
        return ExerciseCoachStatus.fromStorage(
            sp.getString(
                coachStatusKey(raw),
                null
            )
        )
    }

    fun saveCoachStatus(
        raw: String,
        status: ExerciseCoachStatus
    ) {
        sp.edit {
            putString(
                coachStatusKey(raw),
                status.storageValue
            )
        }
    }

    var coachStatusesVersion by rememberSaveable {
        mutableIntStateOf(0)
    }

    val coachStatuses: Map<String, ExerciseCoachStatus> = remember(
        itemList,
        isCoach,
        coachStatusesVersion
    ) {
        if (!isCoach) {
            emptyMap()
        } else {
            itemList.associateWith { raw ->
                loadCoachStatus(raw)
            }
        }
    }

    fun updateCoachStatus(
        raw: String,
        status: ExerciseCoachStatus
    ) {
        saveCoachStatus(
            raw = raw,
            status = status
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
                    coachStatuses[rawItem] ==
                            ExerciseCoachStatus.TAUGHT
                }

                2 -> itemList.filter { rawItem ->
                    coachStatuses[rawItem] ==
                            ExerciseCoachStatus.PRACTICE
                }

                3 -> itemList.filter { rawItem ->
                    coachStatuses[rawItem] ==
                            ExerciseCoachStatus.NEEDS_IMPROVEMENT
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.24f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {                    // ← קובע את מקור התרגול לפי הטאב: הכל/לא-יודע/מועדפים
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

                    ActionButton(
                        text = tr("תרגול", "Practice"),
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onPractice(
                                belt,
                                practiceToken
                            )
                        }
                    )

                    ActionButton(
                        text = tr("איפוס", "Reset"),
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            scope.launch {
                                // איפוס סטטוסים בזיכרון הקומפוז
                                itemList.forEach { item -> itemStates[item] = null }

                                // ⭐ איפוס מועדפים גלובלי
                                FavoritesStore.clearAll()

                                // ❓ איפוס unknown – נשאר מקומי לפי חגורה/נושא
                                unknowns = mutableSetOf()

                                sp.edit {
                                    if (topic == "__ALL__") {
                                        // ✅ 1) מחיקת unknown keys מה-SP
                                        sp.all.keys
                                            .filter {
                                                it.startsWith("unknown_${belt.id}_")
                                            }
                                            .forEach { key ->
                                                remove(key)
                                            }
                                    } else {
                                        val singleUnknownKey =
                                            "unknown_${belt.id}_$suffix"

                                        remove(singleUnknownKey)
                                    }
                                }

                                // ✅ 2) איפוס אמיתי של הסימונים ב-DataStore
                                if (topic == "__ALL__") {
                                    allTopicItems.forEach { ti ->
                                        val canonicalIds = ti.items
                                            .map { raw ->
                                                CanonicalIds.canonicalFor(
                                                    belt,
                                                    ti.topic,
                                                    raw
                                                )
                                            }
                                            .distinct()

                                        vm.clearTopicItems(
                                            belt = belt,
                                            topic = ti.topic,
                                            canonicalIds = canonicalIds
                                        )
                                    }
                                } else {
                                    vm.clearTopic(
                                        belt,
                                        topic
                                    )
                                }
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
            val shape = RoundedCornerShape(18.dp)

            val selectedGradient =
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.90f),
                        MaterialTheme.colorScheme.secondary
                            .copy(alpha = 0.82f)
                    )
                )

            val idleGradient =
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.62f),
                        MaterialTheme.colorScheme.surface
                    )
                )

            Surface(
                onClick = onClick,
                modifier = modifier
                    .padding(
                        horizontal = 3.dp,
                        vertical = 6.dp
                    )
                    .height(104.dp),
                shape = shape,
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width =
                        if (selected) {
                            2.dp
                        } else {
                            1.dp
                        },
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.outline
                                .copy(alpha = 0.28f)
                        }
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                if (selected) {
                                    selectedGradient
                                } else {
                                    idleGradient
                                },
                            shape = shape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 4.dp,
                                vertical = 7.dp
                            ),
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = KmiTypography.caption.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.surface
                                        .copy(alpha = 0.96f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    MaterialTheme.colorScheme.outline
                                        .copy(alpha = 0.24f)
                            )
                        ) {
                            Text(
                                text = number.toString(),
                                style = KmiTypography.action.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                modifier = Modifier.padding(
                                    horizontal = 13.dp,
                                    vertical = 2.dp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 3.dp)
                                .width(28.dp)
                                .height(3.dp)
                                .background(
                                    color =
                                        MaterialTheme.colorScheme.onPrimary,
                                    shape =
                                        RoundedCornerShape(999.dp)
                                )
                        )
                    }
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

        val taughtCount = itemList.count { item ->
            coachStatuses[item] ==
                    ExerciseCoachStatus.TAUGHT
        }

        val practiceCount = itemList.count { item ->
            coachStatuses[item] ==
                    ExerciseCoachStatus.PRACTICE
        }

        val improvementCount = itemList.count { item ->
            coachStatuses[item] ==
                    ExerciseCoachStatus.NEEDS_IMPROVEMENT
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isCoach) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricFieldEdgeToEdge(
                        title = tr("הכול", "All"),
                        number = allCount,
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )

                    MetricFieldEdgeToEdge(
                        title = tr("נלמד", "Taught"),
                        number = taughtCount,
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )

                    MetricFieldEdgeToEdge(
                        title = tr("לתרגול", "Practice"),
                        number = practiceCount,
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f)
                    )

                    MetricFieldEdgeToEdge(
                        title = tr("לשיפור", "Improve"),
                        number = improvementCount,
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricFieldEdgeToEdge(
                        title = tr("הכול", "All"),
                        number = allCount,
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )

                    MetricFieldEdgeToEdge(
                        title = tr("לא יודע", "Unknown"),
                        number = unknownCount,
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )

                    MetricFieldEdgeToEdge(
                        title = tr("מועדפים", "Favorites"),
                        number = favCount,
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val filtered: List<String> =
                if (isCoach) {
                    when (selectedTab) {
                        1 -> itemList.filter { item ->
                            coachStatuses[item] ==
                                    ExerciseCoachStatus.TAUGHT
                        }

                        2 -> itemList.filter { item ->
                            coachStatuses[item] ==
                                    ExerciseCoachStatus.PRACTICE
                        }

                        3 -> itemList.filter { item ->
                            coachStatuses[item] ==
                                    ExerciseCoachStatus.NEEDS_IMPROVEMENT
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
// ✅ מפת “raw -> display” אחת, שמשמשת לכל ה-UI
            val displayByRaw: Map<String, String> = remember(filtered) {
                filtered.associateWith { raw: String ->
                    formattedExerciseTitle(raw)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(
                    items = filtered,
                    key = { _, item ->
                        buildString {
                            append(topicForRawItem(item))
                            append("::")
                            append(exerciseIdentityIdFor(item))
                        }
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

                    val itemHasNote = remember(item, notesRefreshKey) {
                        hasNote(item)
                    }

                    val itemIsUnknown = item in unknownItems

                    val itemCoachStatus =
                        coachStatuses[item]
                            ?: ExerciseCoachStatus.NOT_TAUGHT

                    CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Row(
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
                                }
                                .padding(
                                    start = 4.dp,
                                    end = 4.dp,
                                    top = 9.dp,
                                    bottom = 9.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExerciseRowActionsMenu(
                                isEnglish = isEnglish,
                                isCoach = isCoach,
                                isFav = isFav,
                                hasNote = itemHasNote,
                                isUnknown = itemIsUnknown,
                                coachStatus = itemCoachStatus,
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
                                onToggleUnknown = {
                                    setUnknown(
                                        item,
                                        item !in unknownItems
                                    )
                                },
                                onCoachStatusChange = { newStatus ->
                                    updateCoachStatus(
                                        raw = item,
                                        status = newStatus
                                    )
                                }
                            )

                            Spacer(Modifier.width(10.dp))

                            CompositionLocalProvider(
                                LocalLayoutDirection provides if (isEnglish) {
                                    LayoutDirection.Ltr
                                } else {
                                    LayoutDirection.Rtl
                                }
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = if (isEnglish) {
                                        Alignment.Start
                                    } else {
                                        Alignment.End
                                    }
                                ) {
                                    Text(
                                        text = tr(
                                            "תרגיל ${index + 1}",
                                            "Exercise ${index + 1}"
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        style = KmiTypography.caption.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = when {
                                            itemIsUnknown ->
                                                MaterialTheme.colorScheme.error

                                            isFav ->
                                                MaterialTheme.colorScheme.primary

                                            else ->
                                                MaterialTheme.colorScheme.primary
                                        },
                                        textAlign =
                                            if (isEnglish) {
                                                TextAlign.Left
                                            } else {
                                                TextAlign.Right
                                            },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(3.dp))

                                    Text(
                                        text = displayName,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign =
                                            if (isEnglish) {
                                                TextAlign.Left
                                            } else {
                                                TextAlign.Right
                                            },
                                        color =
                                            MaterialTheme.colorScheme.onSurface,
                                        style = KmiTypography.body.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (itemHasNote || isFav || itemIsUnknown) {
                                        Spacer(Modifier.height(3.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isEnglish) {
                                                Arrangement.Start
                                            } else {
                                                Arrangement.End
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (itemIsUnknown) {
                                                Text(
                                                    text = tr(
                                                        "לא יודע",
                                                        "Unknown"
                                                    ),
                                                    color = MaterialTheme.colorScheme.error,
                                                    style =
                                                        KmiTypography.caption.copy(
                                                            fontWeight =
                                                                FontWeight.Bold
                                                        )
                                                )
                                            }

                                            if (
                                                itemIsUnknown &&
                                                (isFav || itemHasNote)
                                            ) {
                                                Spacer(
                                                    Modifier.width(7.dp)
                                                )
                                            }

                                            if (isFav) {
                                                Text(
                                                    text = tr(
                                                        "★ מועדף",
                                                        "★ Favorite"
                                                    ),
                                                    color =
                                                        MaterialTheme.colorScheme.primary,
                                                    style =
                                                        KmiTypography.caption.copy(
                                                            fontWeight =
                                                                FontWeight.Bold
                                                        )
                                                )
                                            }

                                            if (isFav && itemHasNote) {
                                                Spacer(
                                                    Modifier.width(7.dp)
                                                )
                                            }

                                            if (itemHasNote) {
                                                Text(
                                                    text = tr(
                                                        "הערה שמורה",
                                                        "Saved note"
                                                    ),
                                                    color =
                                                        MaterialTheme.colorScheme.primary,
                                                    style =
                                                        KmiTypography.caption.copy(
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

                    if (index < filtered.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = 48.dp,
                                end = 4.dp
                            ),
                            thickness = 1.dp,
                            color =
                                MaterialTheme.colorScheme.outlineVariant
                                    .copy(alpha = 0.72f)
                        )
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
} // ✅ סוגר את ExercisesTabsScreen(...)


@Composable
private fun ExerciseRowActionsMenu(
    isEnglish: Boolean,
    isCoach: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    isUnknown: Boolean,
    coachStatus: ExerciseCoachStatus,
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
                                if (
                                    coachStatus ==
                                    ExerciseCoachStatus.NOT_TAUGHT
                                ) {
                                    "✓ לא נלמד"
                                } else {
                                    "לא נלמד"
                                },
                                if (
                                    coachStatus ==
                                    ExerciseCoachStatus.NOT_TAUGHT
                                ) {
                                    "✓ Not taught"
                                } else {
                                    "Not taught"
                                }
                            ),
                            style = KmiTypography.action,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    coachStatus ==
                                    ExerciseCoachStatus.TAUGHT
                                ) {
                                    "✓ נלמד"
                                } else {
                                    "נלמד"
                                },
                                if (
                                    coachStatus ==
                                    ExerciseCoachStatus.TAUGHT
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
                                    coachStatus ==
                                    ExerciseCoachStatus.PRACTICE
                                ) {
                                    "✓ לתרגול"
                                } else {
                                    "לתרגול"
                                },
                                if (
                                    coachStatus ==
                                    ExerciseCoachStatus.PRACTICE
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
                            ExerciseCoachStatus.PRACTICE
                        )
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = tr(
                                if (
                                    coachStatus ==
                                    ExerciseCoachStatus.NEEDS_IMPROVEMENT
                                ) {
                                    "✓ לשיפור"
                                } else {
                                    "לשיפור"
                                },
                                if (
                                    coachStatus ==
                                    ExerciseCoachStatus.NEEDS_IMPROVEMENT
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
                            ExerciseCoachStatus.NEEDS_IMPROVEMENT
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
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    var pressed by remember {
        mutableStateOf(false)
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed) {
            0.95f
        } else {
            1f
        },
        label = "btnScale"
    )

    val scope = rememberCoroutineScope()

    val contentOnContainer =
        if (containerColor.luminance() < 0.5f) {
            Color.White
        } else {
            Color.Black
        }

    Button(
        onClick = {
            pressed = true
            onClick()

            scope.launch {
                kotlinx.coroutines.delay(150)
                pressed = false
            }
        },
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .scale(scale)
            .heightIn(min = 56.dp)
            .defaultMinSize(minWidth = 90.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentOnContainer
        )
    ) {
        Text(
            text = text,
            style = KmiTypography.action.copy(
                fontWeight = FontWeight.Bold
            ),
            color = contentOnContainer,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
