@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("PackageName", "SpellCheckingInspection")

package il.kmi.app.screens.BeltQuestions.ByTopic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Explanations
import il.kmi.app.domain.color
import il.kmi.app.notes.ExerciseNotesStore
import il.kmi.app.exercises.sync.ExerciseSyncStore
import il.kmi.app.screens.BeltQuestions.Materials.CoachMaterialProgress
import il.kmi.app.screens.BeltQuestions.Materials.CoachMaterialStatus
import il.kmi.app.screens.BeltQuestions.Materials.CoachMaterialStatusSelector
import il.kmi.app.screens.BeltQuestions.Materials.MaterialsExerciseStatusCard
import il.kmi.app.screens.BeltQuestions.Materials.TraineeMaterialStatus
import il.kmi.app.screens.BeltQuestions.Materials.TraineeMaterialStatusSelector
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.app.ui.pdf.KmiPdfDirection
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.shared.domain.Belt
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.content.HardSectionsCatalog
import il.kmi.shared.domain.content.HardSectionsResolver
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.LocalizationRuntime
import il.yuval.ui.theme.kmiSectionHeaderBrush
import il.yuval.ui.theme.kmiSectionHeaderContentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


//=========================================================================

private fun shareUnifiedSubjectExercisesPdf(
    context: Context,
    title: String,
    groups: List<HardSectionsResolver.BeltItems>,
    isEnglish: Boolean
) {
    val nonEmptyGroups = groups
        .map { group ->
            group.copy(
                items = group.items
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
            )
        }
        .filter { it.items.isNotEmpty() }

    if (nonEmptyGroups.isEmpty()) {
        Toast.makeText(
            context,
            if (isEnglish) {
                "No exercises to export"
            } else {
                "אין תרגילים לייצוא"
            },
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    val file = createUnifiedSubjectExercisesPdf(
        context = context,
        title = title,
        groups = nonEmptyGroups,
        isEnglish = isEnglish
    )

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(
        shareIntent,
        if (isEnglish) "Share PDF" else "שיתוף PDF"
    )

    if (context !is Activity) {
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(chooser)
}

private fun createUnifiedSubjectExercisesPdf(
    context: Context,
    title: String,
    groups: List<HardSectionsResolver.BeltItems>,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 34f
    val contentTop = KmiPdfHeader.CONTENT_TOP
    val contentBottom =
        pageHeight -
                KmiPdfFooter.CONTENT_BOTTOM_PADDING

    fun tr(he: String, en: String): String =
        if (isEnglish) en else he

    val document = PdfDocument()

    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(100, 116, 139)
    val rowBackground = android.graphics.Color.rgb(246, 250, 253)
    val rowBorder = android.graphics.Color.rgb(203, 213, 225)

    val regularTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    val boldTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun textPaint(
        size: Float,
        color: Int = textDark,
        bold: Boolean = false,
        align: Paint.Align =
            KmiPdfDirection.textAlign(isEnglish)
    ): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) boldTypeface else regularTypeface
            textAlign = align
        }
    }

    fun beltPdfColor(belt: Belt): Int {
        return when (belt) {
            Belt.YELLOW -> android.graphics.Color.rgb(245, 158, 11)
            Belt.ORANGE -> android.graphics.Color.rgb(249, 115, 22)
            Belt.GREEN -> android.graphics.Color.rgb(46, 125, 50)
            Belt.BLUE -> android.graphics.Color.rgb(30, 136, 229)
            Belt.BROWN -> android.graphics.Color.rgb(109, 76, 65)
            Belt.BLACK -> android.graphics.Color.rgb(31, 41, 55)
            else -> textMuted
        }
    }

    fun fitText(
        raw: String,
        paint: Paint,
        maxWidth: Float
    ): String {
        val clean = raw
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (paint.measureText(clean) <= maxWidth) {
            return clean
        }

        var shortened = clean
        while (
            shortened.isNotEmpty() &&
            paint.measureText("$shortened…") > maxWidth
        ) {
            shortened = shortened.dropLast(1)
        }

        return "${shortened.trimEnd()}…"
    }

    fun drawHeader(
        canvas: Canvas
    ) {
        val generatedDate =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(Date())

        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "תרגילים לפי נושא",
            titleEnglish = "Exercises by Topic",
            subtitleHebrew = title,
            subtitleEnglish = title,
            generatedDate = generatedDate
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

    data class PdfRow(
        val belt: Belt,
        val title: String?,
        val count: Int = 0
    )

    val rows = buildList {
        groups.forEach { group ->
            val cleanItems = group.items
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()

            add(
                PdfRow(
                    belt = group.belt,
                    title = null,
                    count = cleanItems.size
                )
            )

            cleanItems.forEach { item ->
                add(
                    PdfRow(
                        belt = group.belt,
                        title = item
                    )
                )
            }
        }
    }

    val headerHeight = 34f
    val rowHeight = 48f
    val spacing = 7f

    fun requiredHeight(row: PdfRow): Float =
        if (row.title == null) {
            headerHeight + spacing
        } else {
            rowHeight + spacing
        }

    fun calculatePages(): Int {
        var pages = 1
        var y = contentTop

        rows.forEach { row ->
            val needed = requiredHeight(row)

            if (y + needed > contentBottom) {
                pages++
                y = contentTop
            }

            y += needed
        }

        return pages
    }

    val totalPages = calculatePages()

    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            pageNumber
        ).create()
    )

    var canvas = page.canvas
    var y = contentTop

    drawHeader(canvas)

    fun finishPage() {
        drawFooter(
            canvas = canvas,
            pageNumber = pageNumber,
            totalPages = totalPages
        )

        document.finishPage(page)
    }

    fun nextPage() {
        pageNumber++

        page = document.startPage(
            PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()
        )

        canvas = page.canvas
        y = contentTop
        drawHeader(canvas)
    }

    rows.forEachIndexed { index, row ->
        val needed = requiredHeight(row)

        if (y + needed > contentBottom) {
            finishPage()
            nextPage()
        }

        val beltColor = beltPdfColor(row.belt)

        if (row.title == null) {
            canvas.drawRoundRect(
                margin,
                y,
                pageWidth - margin,
                y + headerHeight,
                12f,
                12f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = beltColor
                }
            )

            val titleAlign =
                KmiPdfDirection.textAlign(isEnglish)

            val titleX =
                KmiPdfDirection.startPaddingX(
                    isEnglish = isEnglish,
                    left = margin,
                    right = pageWidth - margin,
                    padding = 16f
                )

            canvas.drawText(
                beltTitle(row.belt, isEnglish),
                titleX,
                y + 22f,
                textPaint(
                    size = 13f,
                    color = android.graphics.Color.WHITE,
                    bold = true,
                    align = titleAlign
                )
            )

            canvas.drawText(
                tr(
                    "${row.count} תרגילים",
                    "${row.count} exercises"
                ),
                KmiPdfDirection.endPaddingX(
                    isEnglish = isEnglish,
                    left = margin,
                    right = pageWidth - margin,
                    padding = 16f
                ),
                y + 22f,
                textPaint(
                    size = 10f,
                    color = android.graphics.Color.WHITE,
                    bold = true,
                    align =
                        KmiPdfDirection.endTextAlign(isEnglish)
                )
            )

            y += headerHeight + spacing
        } else {
            canvas.drawRoundRect(
                margin,
                y,
                pageWidth - margin,
                y + rowHeight,
                11f,
                11f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = rowBackground
                }
            )

            canvas.drawRoundRect(
                margin,
                y,
                pageWidth - margin,
                y + rowHeight,
                11f,
                11f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = rowBorder
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
            )

            val accentX =
                KmiPdfDirection.startX(
                    isEnglish = isEnglish,
                    left = margin,
                    right = pageWidth - margin
                ) -
                        if (isEnglish) {
                            0f
                        } else {
                            4f
                        }

            canvas.drawRoundRect(
                accentX,
                y,
                accentX + 4f,
                y + rowHeight,
                4f,
                4f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = beltColor
                }
            )

            val itemAlign =
                KmiPdfDirection.textAlign(isEnglish)

            val itemX =
                KmiPdfDirection.startPaddingX(
                    isEnglish = isEnglish,
                    left = margin,
                    right = pageWidth - margin,
                    padding = 16f
                )

            val itemPaint = textPaint(
                size = 11.5f,
                color = textDark,
                bold = true,
                align = itemAlign
            )

            canvas.drawText(
                fitText(
                    raw = if (isEnglish) {
                        translateHardExerciseTitle(row.title)
                    } else {
                        row.title
                    },
                    paint = itemPaint,
                    maxWidth = 445f
                ),
                itemX,
                y + 29f,
                itemPaint
            )

            canvas.drawText(
                (index + 1).toString(),
                KmiPdfDirection.endPaddingX(
                    isEnglish = isEnglish,
                    left = margin,
                    right = pageWidth - margin,
                    padding = 22f
                ),
                y + 29f,
                textPaint(
                    size = 9f,
                    color = textMuted,
                    bold = true,
                    align = Paint.Align.CENTER
                )
            )

            y += rowHeight + spacing
        }
    }

    finishPage()

    val directory = File(
        context.cacheDir,
        "pdfs"
    ).apply {
        mkdirs()
    }

    val fileName =
        if (isEnglish) {
            "Exercises by Topic.pdf"
        } else {
            "תרגילים לפי נושא.pdf"
        }

    val file = File(
        directory,
        fileName
    )

    try {
        FileOutputStream(file).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }

    return file
}

internal object HardSubjectResolverMemoryCache {

    private val lock =
        Any()

    private val resolvedResults =
        mutableMapOf<
                String,
                HardSectionsResolver.NodeResult?
                >()

    private fun cacheKey(
        subjectId: String,
        sectionId: String?
    ): String {
        return buildString {
            append(subjectId.trim())
            append("||")
            append(sectionId?.trim().orEmpty())
        }
    }

    fun resolve(
        subjectId: String,
        sectionId: String? = null
    ): HardSectionsResolver.NodeResult? {
        val key =
            cacheKey(
                subjectId = subjectId,
                sectionId = sectionId
            )

        synchronized(lock) {
            if (resolvedResults.containsKey(key)) {
                return resolvedResults[key]
            }
        }

        val resolved =
            HardSectionsResolver.resolve(
                subjectId,
                sectionId
            )

        synchronized(lock) {
            resolvedResults[key] =
                resolved
        }

        return resolved
    }

    private fun preloadTree(
        subjectId: String,
        sectionId: String? = null
    ) {
        when (
            val resolved =
                resolve(
                    subjectId = subjectId,
                    sectionId = sectionId
                )
        ) {
            is HardSectionsResolver.NodeResult.Sections -> {
                resolved.entries.forEach { entry ->
                    preloadTree(
                        subjectId = subjectId,
                        sectionId = entry.id
                    )
                }
            }

            is HardSectionsResolver.NodeResult.BeltGroups,
            null -> Unit
        }
    }

    fun preloadAll() {
        listOf(
            "def_internal",
            "def_external",
            "knife_defense",
            "gun_threat_defense",
            "stick_defense",
            "kicks_hard",
            "knife_rifle_defense",
            "multiple_attackers_defense",
            "releases",
            "releases_hugs",
            "hands_all"
        ).forEach { subjectId ->
            preloadTree(
                subjectId = subjectId
            )
        }
    }
}

@Composable
fun UnifiedSubjectExercisesScreen(
    subjectId: String,
    sectionId: String? = null,
    onOpenSection: (subjectId: String, sectionId: String?) -> Unit,
    onBack: () -> Unit,
    vm: KmiViewModel,
    isCoach: Boolean? = null
) {
    val isEnglish = LocalizationRuntime.currentLanguage == AppLanguage.ENGLISH
    val resolverSubjectId = remember(subjectId) {
        when (subjectId.trim()) {
            "kicks" -> "kicks_hard"

            // פתיחת כל הנושא כרשימת תרגילים מלאה לפי חגורות
            "releases_all" -> "releases"
            "defenses_root_all" -> "defenses_root"
            "hands_all_full" -> "hands_all"

            else -> subjectId
        }
    }

    val result = remember(
        subjectId,
        resolverSubjectId,
        sectionId
    ) {
        if (subjectId.trim() == "hands_all_full") {
            HardSectionsResolver.resolve(
                subjectId = "hands_all",
                sectionId = sectionId
            )
        } else {
            HardSubjectResolverMemoryCache.resolve(
                subjectId = resolverSubjectId,
                sectionId = sectionId
            )
        }
    }

    val combinedDefenseGroups = remember(resolverSubjectId) {
        combinedDefenseGroupsFor(resolverSubjectId)
    }

    val fullTopicGroups =
        remember(
            subjectId,
            resolverSubjectId,
            result
        ) {
            when (subjectId.trim()) {

                "hands_all_full" -> {
                    listOf(
                        Belt.YELLOW,
                        Belt.ORANGE,
                        Belt.GREEN,
                        Belt.BLUE,
                        Belt.BROWN,
                        Belt.BLACK
                    ).mapNotNull { belt ->

                        val items =
                            HardSectionsCatalog
                                .subjectItemsFor(
                                    subjectId = "hands_all",
                                    belt = belt
                                )
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()

                        if (items.isEmpty()) {
                            null
                        } else {
                            HardSectionsResolver.BeltItems(
                                belt = belt,
                                items = items
                            )
                        }
                    }
                }

                else ->
                    null
            }
        }

    val shouldShowSectionCards = sectionId == null && isRootSubjectId(subjectId)

    val flattenedSectionGroups = remember(subjectId, sectionId, result, shouldShowSectionCards) {
        if (!shouldShowSectionCards && result is HardSectionsResolver.NodeResult.Sections) {
            flattenNestedSectionsToBeltGroups(
                subjectId = resolverSubjectId,
                entries = result.entries
            )
        } else {
            null
        }
    }

    val pdfGroups = remember(
        combinedDefenseGroups,
        flattenedSectionGroups,
        result,
        shouldShowSectionCards
    ) {
        when {
            combinedDefenseGroups != null ->
                combinedDefenseGroups

            result is HardSectionsResolver.NodeResult.BeltGroups ->
                result.groups

            !shouldShowSectionCards && flattenedSectionGroups != null ->
                flattenedSectionGroups

            else ->
                emptyList()
        }
    }

    val context = LocalContext.current

    val rolePrefs = remember(context) {
        context.getSharedPreferences(
            "kmi_user",
            Context.MODE_PRIVATE
        )
    }

    fun roleIsCoach(
        role: String?
    ): Boolean {
        val normalizedRole =
            role
                ?.trim()
                ?.lowercase()
                .orEmpty()

        return normalizedRole == "coach" ||
                normalizedRole == "trainer" ||
                normalizedRole.contains("מאמן") ||
                normalizedRole.contains("מדריך")
    }

    val savedActiveCoachMode =
        remember(rolePrefs) {
            val activeRole =
                rolePrefs.getString(
                    "active_user_mode",
                    null
                )
                    ?: rolePrefs.getString(
                        "last_active_app_role",
                        null
                    )

            activeRole
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let(::roleIsCoach)
        }

    val fallbackIsCoach =
        remember(rolePrefs) {
            val profileRole =
                rolePrefs.getString(
                    "user_role",
                    null
                )
                    ?: rolePrefs.getString(
                        "role",
                        null
                    )

            roleIsCoach(profileRole)
        }

    val resolvedIsCoach =
        savedActiveCoachMode
            ?: isCoach
            ?: fallbackIsCoach

    val pdfTitle = resultTitle(
        subjectId = subjectId,
        result = result
    )

    Scaffold(
        topBar = {
            KmiTopBar(
                title = pdfTitle,
                onBack = onBack,
                onHome = null,
                showTopHome = false,
                centerTitle = true,
                lockSearch = false,
                showBottomActions = true,
                onShare = {
                    shareUnifiedSubjectExercisesPdf(
                        context = context,
                        title = pdfTitle,
                        groups = pdfGroups,
                        isEnglish = isEnglish
                    )
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
        ) {
            if (subjectId == "hands_all_full") {
                BeltGroupsContent(
                    title = subjectRootTitle(subjectId),
                    groups = fullTopicGroups.orEmpty(),
                    isEnglish = isEnglish,
                    isCoach = resolvedIsCoach,
                    vm = vm,
                    modifier = Modifier.fillMaxSize()
                )

                return@Box
            }
            if (
                combinedDefenseGroups != null &&
                result is HardSectionsResolver.NodeResult.Sections
            ) {
                SectionsContent(
                    subjectId = resolverSubjectId,
                    entries = result.entries,
                    isEnglish = isEnglish,
                    isCoach = resolvedIsCoach,
                    vm = vm,
                    onOpen = onOpenSection,
                    modifier = Modifier.fillMaxSize()
                )

                return@Box
            }

            when (result) {
                is HardSectionsResolver.NodeResult.Sections -> {
                    if (shouldShowSectionCards) {
                        SectionsContent(
                            subjectId = subjectId,
                            entries = result.entries,
                            isEnglish = isEnglish,
                            isCoach = resolvedIsCoach,
                            vm = vm,
                            onOpen = onOpenSection,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BeltGroupsContent(
                            title = result.title ?: subjectRootTitle(subjectId),
                            groups = flattenedSectionGroups.orEmpty(),
                            isEnglish = isEnglish,
                            isCoach = resolvedIsCoach,
                            vm = vm,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                is HardSectionsResolver.NodeResult.BeltGroups -> {
                    BeltGroupsContent(
                        title = result.title,
                        groups = result.groups,
                        isEnglish = isEnglish,
                        isCoach = resolvedIsCoach,
                        vm = vm,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "No data to display"
                                } else {
                                    "אין נתונים להצגה"
                                },
                            style = KmiTypography.body,
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Left
                                } else {
                                    TextAlign.Right
                                },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun resultTitle(
    subjectId: String,
    result: HardSectionsResolver.NodeResult?
): String {
    return when (result) {
        is HardSectionsResolver.NodeResult.Sections -> {
            result.title ?: subjectRootTitle(subjectId)
        }

        is HardSectionsResolver.NodeResult.BeltGroups -> result.title
        null -> subjectRootTitle(subjectId)
    }
}

private fun subjectRootTitle(subjectId: String): String =
    when (subjectId) {
        "releases",
        "releases_all" -> "שחרורים"

        "defenses_root_all" -> "הגנות"

        "hands_all_full" -> "עבודת ידיים"

        "releases_hugs" -> "שחרור מחביקות"
        "def_internal" -> "הגנות פנימיות"
        "def_external" -> "הגנות חיצוניות"
        "knife_defense" -> "הגנות מסכין"
        "gun_threat_defense" -> "הגנות מאיום אקדח"
        "stick_defense" -> "הגנות נגד מקל"
        "kicks" -> "הגנות נגד בעיטות"
        "kicks_hard" -> "הגנות נגד בעיטות"

        else -> "נושאים"
    }

private fun isRootSubjectId(subjectId: String): Boolean {
    return subjectId in setOf(
        "releases",
        "knife_defense",
        "gun_threat_defense",
        "stick_defense",
        "kicks"
    )
}

private fun combinedDefenseGroupsFor(
    subjectId: String
): List<HardSectionsResolver.BeltItems>? {
    val sectionIds = when (subjectId.trim().lowercase()) {
        "def_internal" -> listOf(
            "def_internal_punch",
            "def_internal_kick"
        )

        "def_external" -> listOf(
            "def_external_punch",
            "def_external_kick"
        )

        else -> return null
    }

    val mergedByBelt = linkedMapOf<Belt, MutableList<String>>()

    fun addGroups(groups: List<HardSectionsResolver.BeltItems>) {
        groups.forEach { group ->
            val items = mergedByBelt.getOrPut(group.belt) { mutableListOf() }
            items.addAll(group.items)
        }
    }

    sectionIds.forEach { sectionId ->
        when (
            val resolved =
                HardSubjectResolverMemoryCache.resolve(
                    subjectId = sectionId
                )
        ) {
            is HardSectionsResolver.NodeResult.BeltGroups -> {
                addGroups(resolved.groups)
            }

            is HardSectionsResolver.NodeResult.Sections -> {
                addGroups(
                    flattenNestedSectionsToBeltGroups(
                        subjectId = sectionId,
                        entries = resolved.entries
                    )
                )
            }

            null -> Unit
        }
    }

    return mergedByBelt.map { (belt, items) ->
        HardSectionsResolver.BeltItems(
            belt = belt,
            items = items.distinct()
        )
    }
}

private fun flattenNestedSectionsToBeltGroups(
    subjectId: String,
    entries: List<HardSectionsResolver.SectionEntry>
): List<HardSectionsResolver.BeltItems> {
    val mergedByBelt = linkedMapOf<Belt, MutableList<String>>()

    fun addGroups(groups: List<HardSectionsResolver.BeltItems>) {
        groups.forEach { group ->
            val items = mergedByBelt.getOrPut(group.belt) { mutableListOf() }
            items.addAll(group.items)
        }
    }

    fun collect(entry: HardSectionsResolver.SectionEntry) {
        when (
            val resolved =
                HardSubjectResolverMemoryCache.resolve(
                    subjectId = subjectId,
                    sectionId = entry.id
                )
        ) {
            is HardSectionsResolver.NodeResult.BeltGroups -> {
                addGroups(resolved.groups)
            }

            is HardSectionsResolver.NodeResult.Sections -> {
                val nestedEntries: List<HardSectionsResolver.SectionEntry> = resolved.entries
                nestedEntries.forEach { nestedEntry: HardSectionsResolver.SectionEntry ->
                    collect(nestedEntry)
                }
            }

            null -> Unit
        }
    }

    entries.forEach { entry ->
        collect(entry)
    }

    return mergedByBelt.map { (belt, items) ->
        HardSectionsResolver.BeltItems(
            belt = belt,
            items = items.distinct()
        )
    }
}

private data class SectionBeltRow(
    val belt: Belt,
    val entry: HardSectionsResolver.SectionEntry,
    val items: List<String>
)

private data class SectionExerciseRef(
    val belt: Belt,
    val sectionTitle: String,
    val rawItem: String
)

@Composable
private fun SectionsContent(
    subjectId: String,
    entries: List<HardSectionsResolver.SectionEntry>,
    isEnglish: Boolean,
    isCoach: Boolean,
    vm: KmiViewModel,
    onOpen: (subjectId: String, sectionId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val prefs = remember(context) {
        context.getSharedPreferences(
            "kmi_settings",
            Context.MODE_PRIVATE
        )
    }

    val favoriteIds: Set<String> by
    FavoritesStore
        .favoritesFlow
        .collectAsState(
            initial = emptySet()
        )

    val resolveSubjectId =
        remember(subjectId) {
            when (subjectId.trim()) {
                "kicks" -> "kicks_hard"
                else -> subjectId
            }
        }

    fun cleanItems(
        items: List<String>
    ): List<String> {
        return items
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /*
     * הופכים את המבנה:
     *
     * section -> belts
     *
     * למבנה:
     *
     * belt -> sections
     */
    val sectionRows =
        remember(
            resolveSubjectId,
            entries
        ) {
            val grouped =
                linkedMapOf<
                        Belt,
                        MutableList<SectionBeltRow>
                        >()

            entries.forEach { entry ->

                val groups =
                    when (
                        val resolved =
                            HardSubjectResolverMemoryCache.resolve(
                                subjectId = resolveSubjectId,
                                sectionId = entry.id
                            )
                    ) {
                        is HardSectionsResolver
                        .NodeResult
                        .BeltGroups -> {
                            resolved.groups
                        }

                        is HardSectionsResolver
                        .NodeResult
                        .Sections -> {
                            flattenNestedSectionsToBeltGroups(
                                subjectId =
                                    resolveSubjectId,
                                entries =
                                    resolved.entries
                            )
                        }

                        null -> {
                            emptyList()
                        }
                    }

                groups.forEach { group ->
                    val items =
                        cleanItems(
                            group.items
                        )

                    if (items.isNotEmpty()) {
                        grouped
                            .getOrPut(
                                group.belt
                            ) {
                                mutableListOf()
                            }
                            .add(
                                SectionBeltRow(
                                    belt =
                                        group.belt,
                                    entry =
                                        entry,
                                    items =
                                        items
                                )
                            )
                    }
                }
            }

            grouped
                .values
                .flatten()
        }

    if (sectionRows.isEmpty()) {
        Box(
            modifier =
                modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text =
                    if (isEnglish) {
                        "No exercises found"
                    } else {
                        "לא נמצאו תרגילים"
                    },
                style =
                    KmiTypography.body,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )
        }

        return
    }

    val allExerciseRefs =
        remember(sectionRows) {
            sectionRows
                .flatMap { row ->
                    row.items.map { rawItem ->
                        SectionExerciseRef(
                            belt = row.belt,
                            sectionTitle =
                                row.entry.title,
                            rawItem =
                                rawItem
                        )
                    }
                }
                .distinctBy { ref ->
                    ref.belt.id +
                            "::${ref.sectionTitle}" +
                            "::${ref.rawItem}"
                }
        }

    fun normalizeStatusPart(
        value: String
    ): String {
        return value
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    fun statusIdFor(
        ref: SectionExerciseRef
    ): String {
        return ExerciseIdentityRegistry
            .resolve(
                belt = ref.belt,
                hebrewTitle =
                    normalizeStatusPart(
                        ref.rawItem
                    ),
                topicKey =
                    normalizeStatusPart(
                        ref.sectionTitle
                    )
            )
            .id
    }

    fun stateMapKey(
        ref: SectionExerciseRef
    ): String {
        return buildString {
            append(ref.belt.id)
            append("|")
            append(
                normalizeStatusPart(
                    ref.sectionTitle
                )
            )
            append("|")
            append(
                statusIdFor(ref)
            )
        }
    }

    val traineeStates =
        remember(
            resolveSubjectId,
            entries
        ) {
            mutableStateMapOf<
                    String,
                    Boolean?
                    >()
        }

    val coachStates =
        remember(
            resolveSubjectId,
            entries
        ) {
            mutableStateMapOf<
                    String,
                    CoachMaterialProgress
                    >()
        }

    fun coachProgressKey(
        ref: SectionExerciseRef
    ): String {
        val statusId =
            statusIdFor(ref)
                .substringBefore("__")

        return buildString {
            append(
                "coach_material_progress_"
            )
            append(ref.belt.id)
            append("_")
            append(
                normalizeStatusPart(
                    ref.sectionTitle
                )
            )
            append("_")
            append(statusId)
        }
    }

    fun loadCoachProgress(
        ref: SectionExerciseRef
    ): CoachMaterialProgress {

        val key =
            coachProgressKey(ref)

        val selectable =
            listOf(
                CoachMaterialStatus.TAUGHT,
                CoachMaterialStatus.PRACTICED,
                CoachMaterialStatus
                    .NEEDS_REINFORCEMENT
            )

        val selected =
            selectable
                .filter { status ->
                    prefs.getBoolean(
                        "${key}_${status.storageValue}_selected",
                        false
                    )
                }
                .toSet()

        val dates =
            selectable
                .mapNotNull { status ->
                    val value =
                        prefs.getLong(
                            "${key}_${status.storageValue}_updated_at",
                            0L
                        )

                    if (value > 0L) {
                        status to value
                    } else {
                        null
                    }
                }
                .toMap()

        if (selected.isEmpty()) {
            val legacy =
                CoachMaterialStatus
                    .fromStorage(
                        prefs.getString(
                            "${key}_status",
                            null
                        )
                    )

            val legacyDate =
                prefs.getLong(
                    "${key}_updated_at",
                    0L
                )

            if (
                legacy !=
                CoachMaterialStatus
                    .NOT_TAUGHT
            ) {
                return CoachMaterialProgress(
                    selectedStatuses =
                        setOf(legacy),
                    updatedAtByStatus =
                        if (legacyDate > 0L) {
                            mapOf(
                                legacy to
                                        legacyDate
                            )
                        } else {
                            emptyMap()
                        }
                )
            }
        }

        return CoachMaterialProgress(
            selectedStatuses =
                selected,
            updatedAtByStatus =
                dates
        )
    }

    LaunchedEffect(
        allExerciseRefs
    ) {
        allExerciseRefs.forEach { ref ->

            val statusId =
                statusIdFor(ref)

            val topicKeys =
                listOf(
                    ref.sectionTitle,
                    "כללי"
                )
                    .map {
                        normalizeStatusPart(it)
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()

            var traineeValue:
                    Boolean? = null

            for (key in topicKeys) {
                val loaded =
                    runCatching {
                        vm.getItemStatusNullable(
                            belt = ref.belt,
                            topic = key,
                            item = statusId
                        )
                    }
                        .getOrNull()

                if (loaded != null) {
                    traineeValue =
                        loaded
                    break
                }
            }

            if (traineeValue == null) {
                for (key in topicKeys) {

                    val mastered =
                        prefs.getStringSet(
                            "mastered_${ref.belt.id}_$key",
                            emptySet<String>()
                        )
                            ?: emptySet()

                    val unknown =
                        prefs.getStringSet(
                            "unknown_${ref.belt.id}_$key",
                            emptySet<String>()
                        )
                            ?: emptySet()

                    traineeValue =
                        when (statusId) {
                            in mastered ->
                                true

                            in unknown ->
                                false

                            else ->
                                null
                        }

                    if (
                        traineeValue != null
                    ) {
                        break
                    }
                }
            }

            traineeStates[
                stateMapKey(ref)
            ] = traineeValue

            coachStates[
                stateMapKey(ref)
            ] =
                loadCoachProgress(ref)
        }
    }

    val listState =
        rememberLazyListState()

    val currentStickyBelt by
    remember(
        sectionRows,
        listState
    ) {
        derivedStateOf {
            sectionRows
                .getOrNull(
                    listState
                        .firstVisibleItemIndex
                )
                ?.belt
                ?: sectionRows
                    .first()
                    .belt
        }
    }

    val currentExercises =
        allExerciseRefs.filter {
            it.belt ==
                    currentStickyBelt
        }

    val knownCount =
        currentExercises.count { ref ->
            traineeStates[
                stateMapKey(ref)
            ] == true
        }

    val unknownCount =
        currentExercises.count { ref ->
            traineeStates[
                stateMapKey(ref)
            ] == false
        }

    val unmarkedCount =
        currentExercises.count { ref ->
            traineeStates[
                stateMapKey(ref)
            ] == null
        }

    val favoriteCount =
        currentExercises.count { ref ->
            statusIdFor(ref) in
                    favoriteIds
        }

    val taughtCount =
        currentExercises.count { ref ->
            coachStates[
                stateMapKey(ref)
            ]?.isSelected(
                CoachMaterialStatus.TAUGHT
            ) == true
        }

    val practicedCount =
        currentExercises.count { ref ->
            coachStates[
                stateMapKey(ref)
            ]?.isSelected(
                CoachMaterialStatus.PRACTICED
            ) == true
        }

    val reinforcementCount =
        currentExercises.count { ref ->
            coachStates[
                stateMapKey(ref)
            ]?.isSelected(
                CoachMaterialStatus
                    .NEEDS_REINFORCEMENT
            ) == true
        }

    val coachUnmarkedCount =
        currentExercises.count { ref ->
            coachStates[
                stateMapKey(ref)
            ]
                ?.selectedStatuses
                .orEmpty()
                .isEmpty()
        }

    Column(
        modifier =
            modifier.fillMaxSize()
    ) {

        HardBeltStickyHeader(
            belt = currentStickyBelt,
            count = currentExercises.size,
            isCoach = isCoach,
            taughtCount = taughtCount,
            practicedCount = practicedCount,
            reinforcementCount = reinforcementCount,
            coachUnmarkedCount = coachUnmarkedCount,
            knownCount = knownCount,
            unknownCount = unknownCount,
            favoriteCount = favoriteCount,
            unmarkedCount = unmarkedCount,
            isEnglish = isEnglish,
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding =
                PaddingValues(
                    bottom = 24.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {
            itemsIndexed(
                items =
                    sectionRows,
                key = { index, row ->
                    "section_belt_" +
                            "${row.belt.id}_" +
                            "${row.entry.id}_" +
                            index
                }
            ) { index, row ->

                val previousBelt =
                    sectionRows
                        .getOrNull(
                            index - 1
                        )
                        ?.belt

                val firstOfBelt =
                    previousBelt !=
                            row.belt

                if (
                    firstOfBelt &&
                    index != 0 &&
                    row.belt !=
                    currentStickyBelt
                ) {
                    Spacer(
                        modifier =
                            Modifier.height(
                                42.dp
                            )
                    )

                    val beltExercises =
                        allExerciseRefs
                            .filter {
                                it.belt ==
                                        row.belt
                            }

                    HardBeltStickyHeader(
                        belt = row.belt,
                        count = beltExercises.size,
                        isCoach = isCoach,
                        taughtCount = 0,
                        practicedCount = 0,
                        reinforcementCount = 0,
                        coachUnmarkedCount = 0,
                        knownCount = 0,
                        unknownCount = 0,
                        favoriteCount = 0,
                        unmarkedCount = 0,
                        isEnglish = isEnglish,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SubjectSectionCard(
                    title =
                        if (isEnglish) {
                            translateHardTopicTitle(
                                row.entry.title
                            )
                        } else {
                            row.entry.title
                        },
                    count =
                        row.items.size,
                    isEnglish =
                        isEnglish,
                    onClick = {
                        onOpen(
                            subjectId,
                            row.entry.id
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SubjectSectionCard(
    title: String,
    count: Int,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFD9D4E8)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.White.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = Color(0xFF7B7593)
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = title,
                    style = KmiTypography.cardTitle,
                    textAlign =
                        if (isEnglish) TextAlign.Left else TextAlign.Right,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F4F8)
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                if (count == 1) {
                                    "1 exercise"
                                } else {
                                    "$count exercises"
                                }
                            } else {
                                "$count תרגילים"
                            },
                        style = KmiTypography.caption,
                        color = Color(0xFF4E6D73),
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        )
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(21.dp),
                color = Color(0xFFF3F0FA)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(0xFF7A6FA3)
                    )
                }
            }
        }
    }
}

private data class SelectedHardExercise(
    val belt: Belt,
    val topic: String,
    val rawItem: String,
    val displayItem: String
)

private sealed interface HardBeltListRow {
    val belt: Belt

    data class BeltHeader(
        override val belt: Belt
    ) : HardBeltListRow

    data class Exercise(
        override val belt: Belt,
        val index: Int,
        val rawItem: String,
        val statusId: String
    ) : HardBeltListRow
}

private fun hardItemsForGroup(
    group: HardSectionsResolver.BeltItems
): List<String> {
    return group.items
        .map { rawItem: String -> rawItem.trim() }
        .filter { rawItem: String -> rawItem.isNotBlank() }
}

@Composable
private fun HardTopStatChip(
    value: String,
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    val colors =
        MaterialTheme.colorScheme

    val isDark =
        colors.surface.luminance() < 0.5f

    val gradientTop =
        if (isDark) {
            colors.surface.copy(
                alpha = 0.98f
            )
        } else {
            containerColor.copy(
                alpha = 0.58f
            )
        }

    val gradientBottom =
        if (isDark) {
            containerColor.copy(
                alpha = 0.18f
            )
        } else {
            containerColor.copy(
                alpha = 0.92f
            )
        }

    Surface(
        modifier =
            modifier.height(58.dp),
        shape =
            RoundedCornerShape(10.dp),
        color =
            colors.surface,
        tonalElevation =
            0.dp,
        shadowElevation =
            1.dp,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    contentColor.copy(
                        alpha =
                            if (isDark) {
                                0.55f
                            } else {
                                0.24f
                            }
                    )
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    gradientTop,
                                    gradientBottom
                                )
                        )
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            contentColor
                        )
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 3.dp,
                            vertical = 3.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {
                Spacer(
                    Modifier.height(2.dp)
                )

                Text(
                    text =
                        value,
                    style =
                        KmiTypography
                            .action
                            .copy(
                                fontWeight =
                                    FontWeight.ExtraBold
                            ),
                    color =
                        colors.onSurface,
                    textAlign =
                        TextAlign.Center,
                    maxLines =
                        1
                )

                Text(
                    text =
                        label,
                    style =
                        KmiTypography
                            .caption
                            .copy(
                                fontWeight =
                                    FontWeight.Bold
                            ),
                    color =
                        if (isDark) {
                            colors.onSurface
                        } else {
                            contentColor
                        },
                    textAlign =
                        TextAlign.Center,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BeltGroupsContent(
    title: String,
    groups: List<HardSectionsResolver.BeltItems>,
    isEnglish: Boolean,
    isCoach: Boolean,
    vm: KmiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val prefs = remember(context) {
        context.getSharedPreferences(
            "kmi_settings",
            Context.MODE_PRIVATE
        )
    }

    val persistenceScope =
        rememberCoroutineScope()

    val hardItemStates =
        remember(title) {
            mutableStateMapOf<String, Boolean?>()
        }

    val hardPartiallyKnownStates =
        remember(title) {
            mutableStateMapOf<String, Boolean>()
        }

    val coachProgressStates =
        remember(title) {
            mutableStateMapOf<String, CoachMaterialProgress>()
        }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun syncIdentityFor(
        belt: Belt,
        topic: String,
        rawItem: String
    ) =
        ExerciseSyncStore.resolveIdentity(
            belt = belt,
            topic = topic,
            subTopic = null,
            rawItem = rawItem
        )

    fun coachProgressKey(
        belt: Belt,
        statusId: String
    ): String {
        return buildString {
            append("coach_material_progress_")
            append(belt.id)
            append("_")
            append(normalizeStatusPart(title))
            append("_")
            append(statusId.substringBefore("__"))
        }
    }

    fun loadCoachProgress(
        belt: Belt,
        statusId: String
    ): CoachMaterialProgress {
        val key =
            coachProgressKey(
                belt = belt,
                statusId = statusId
            )

        val statuses =
            listOf(
                CoachMaterialStatus.TAUGHT,
                CoachMaterialStatus.PRACTICED,
                CoachMaterialStatus.NEEDS_REINFORCEMENT
            )

        val selectedStatuses =
            statuses
                .filter { status ->
                    prefs.getBoolean(
                        "${key}_${status.storageValue}_selected",
                        false
                    )
                }
                .toSet()

        val updatedAtByStatus =
            statuses
                .mapNotNull { status ->
                    val updatedAt =
                        prefs.getLong(
                            "${key}_${status.storageValue}_updated_at",
                            0L
                        )

                    if (updatedAt > 0L) {
                        status to updatedAt
                    } else {
                        null
                    }
                }
                .toMap()

        return CoachMaterialProgress(
            selectedStatuses = selectedStatuses,
            updatedAtByStatus = updatedAtByStatus
        )
    }

    fun saveCoachProgress(
        belt: Belt,
        statusId: String,
        status: CoachMaterialStatus
    ) {
        val key =
            coachProgressKey(
                belt = belt,
                statusId = statusId
            )

        val current =
            coachProgressStates[statusId]
                ?: loadCoachProgress(
                    belt = belt,
                    statusId = statusId
                )

        val selected =
            current.selectedStatuses.toMutableSet()

        val dates =
            current.updatedAtByStatus.toMutableMap()

        if (selected.contains(status)) {
            selected.remove(status)
            dates.remove(status)
        } else {
            if (selected.size >= 2) {
                return
            }

            selected.add(status)
            dates[status] =
                System.currentTimeMillis()
        }

        val next =
            CoachMaterialProgress(
                selectedStatuses = selected,
                updatedAtByStatus = dates
            )

        coachProgressStates[statusId] = next

        prefs.edit {
            listOf(
                CoachMaterialStatus.TAUGHT,
                CoachMaterialStatus.PRACTICED,
                CoachMaterialStatus.NEEDS_REINFORCEMENT
            ).forEach { itemStatus ->

                if (selected.contains(itemStatus)) {
                    putBoolean(
                        "${key}_${itemStatus.storageValue}_selected",
                        true
                    )

                    putLong(
                        "${key}_${itemStatus.storageValue}_updated_at",
                        dates[itemStatus] ?: 0L
                    )
                } else {
                    remove(
                        "${key}_${itemStatus.storageValue}_selected"
                    )

                    remove(
                        "${key}_${itemStatus.storageValue}_updated_at"
                    )
                }
            }
        }
    }

    fun hardStatusIdFor(
        belt: Belt,
        topic: String,
        rawItem: String
    ): String {
        val resolved = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = normalizeStatusPart(rawItem),
            topicKey = normalizeStatusPart(topic)
        )

        return resolved.id
    }

    fun hardStatusKeysFor(
        topic: String
    ): List<String> {
        return listOf(topic, "כללי")
            .map { value -> normalizeStatusPart(value) }
            .filter { value -> value.isNotBlank() }
            .distinct()
    }

    fun hardItemsOf(group: HardSectionsResolver.BeltItems): List<String> {
        return hardItemsForGroup(group)
    }

    fun setHardLocalStatus(
        belt: Belt,
        topic: String,
        statusId: String,
        value: Boolean?,
        partiallyKnown: Boolean = false
    ) {
        hardStatusKeysFor(
            topic = topic
        ).forEach { key ->
            val masteredKey =
                "mastered_${belt.id}_${key}"

            val unknownKey =
                "unknown_${belt.id}_${key}"

            val partiallyKnownKey =
                "partially_known_${belt.id}_${key}"

            val masteredSet =
                (
                        prefs.getStringSet(
                            masteredKey,
                            emptySet<String>()
                        ) ?: emptySet()
                        )
                    .toMutableSet()

            val unknownSet =
                (
                        prefs.getStringSet(
                            unknownKey,
                            emptySet<String>()
                        ) ?: emptySet()
                        )
                    .toMutableSet()

            val partiallyKnownSet =
                (
                        prefs.getStringSet(
                            partiallyKnownKey,
                            emptySet<String>()
                        ) ?: emptySet()
                        )
                    .toMutableSet()

            when {
                partiallyKnown -> {
                    masteredSet.remove(statusId)
                    unknownSet.add(statusId)
                    partiallyKnownSet.add(statusId)
                }

                value == true -> {
                    masteredSet.add(statusId)
                    unknownSet.remove(statusId)
                    partiallyKnownSet.remove(statusId)
                }

                value == false -> {
                    unknownSet.add(statusId)
                    masteredSet.remove(statusId)
                    partiallyKnownSet.remove(statusId)
                }

                else -> {
                    masteredSet.remove(statusId)
                    unknownSet.remove(statusId)
                    partiallyKnownSet.remove(statusId)
                }
            }

            prefs.edit {
                putStringSet(
                    masteredKey,
                    masteredSet
                )
                putStringSet(
                    unknownKey,
                    unknownSet
                )
                putStringSet(
                    partiallyKnownKey,
                    partiallyKnownSet
                )
            }
        }
    }

    LaunchedEffect(
        groups,
        title,
        isCoach
    ) {
        val cachedStatusesByBelt =
            groups
                .map { group ->
                    group.belt
                }
                .distinct()
                .associateWith { belt ->
                    vm.getBeltStatusSnapshot(
                        belt = belt
                    )
                }

        val (
            loadedItemStates,
            loadedPartiallyKnownStates,
            loadedCoachStates
        ) = withContext(Dispatchers.IO) {
            val itemStates =
                linkedMapOf<String, Boolean?>()

            val partiallyKnownStates =
                linkedMapOf<String, Boolean>()

            val coachStates =
                linkedMapOf<String, CoachMaterialProgress>()

            val statusKeys =
                hardStatusKeysFor(title)

            groups.forEach { group ->
                val rawItems =
                    hardItemsOf(group)

                rawItems.forEach { rawItem ->
                    val statusId =
                        hardStatusIdFor(
                            belt = group.belt,
                            topic = title,
                            rawItem = rawItem
                        )

                    var valueFromVm: Boolean? =
                        null

                    val beltStatuses =
                        cachedStatusesByBelt[
                            group.belt
                        ]
                            .orEmpty()

                    for (key in statusKeys) {
                        val canonicalKey =
                            if (
                                key.equals(
                                    "כללי",
                                    ignoreCase = true
                                )
                            ) {
                                ""
                            } else {
                                key
                            }

                        val topicStatuses =
                            beltStatuses[
                                canonicalKey
                            ]

                        if (
                            topicStatuses
                                ?.containsKey(statusId) ==
                            true
                        ) {
                            valueFromVm =
                                topicStatuses[
                                    statusId
                                ]

                            break
                        }
                    }

                    if (valueFromVm == null) {
                        for (key in statusKeys) {
                            val masteredKey =
                                "mastered_${group.belt.id}_$key"

                            val unknownKey =
                                "unknown_${group.belt.id}_$key"

                            val masteredSet =
                                prefs.getStringSet(
                                    masteredKey,
                                    emptySet<String>()
                                ) ?: emptySet()

                            val unknownSet =
                                prefs.getStringSet(
                                    unknownKey,
                                    emptySet<String>()
                                ) ?: emptySet()

                            val localValue: Boolean? =
                                when (statusId) {
                                    in masteredSet ->
                                        true

                                    in unknownSet ->
                                        false

                                    else ->
                                        null
                                }

                            if (localValue != null) {
                                valueFromVm = localValue
                                break
                            }
                        }
                    }

                    val isPartiallyKnown =
                        statusKeys.any { key ->
                            val partiallyKnownKey =
                                "partially_known_${group.belt.id}_$key"

                            val partiallyKnownSet =
                                prefs.getStringSet(
                                    partiallyKnownKey,
                                    emptySet<String>()
                                ) ?: emptySet()

                            statusId in partiallyKnownSet
                        }

                    partiallyKnownStates[statusId] =
                        isPartiallyKnown

                    itemStates[statusId] =
                        if (isPartiallyKnown) {
                            false
                        } else {
                            valueFromVm
                        }

                    if (isCoach) {
                        coachStates[statusId] =
                            loadCoachProgress(
                                belt = group.belt,
                                statusId = statusId
                            )
                    }
                }
            }

            Triple(
                itemStates,
                partiallyKnownStates,
                coachStates
            )
        }

        hardItemStates.clear()
        hardItemStates.putAll(
            loadedItemStates
        )

        hardPartiallyKnownStates.clear()
        hardPartiallyKnownStates.putAll(
            loadedPartiallyKnownStates
        )

        coachProgressStates.clear()
        coachProgressStates.putAll(
            loadedCoachStates
        )
    }


    val favoriteIds: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    fun hardFavoriteIdFor(
        belt: Belt,
        topic: String,
        rawItem: String
    ): String {
        return syncIdentityFor(
            belt = belt,
            topic = topic,
            rawItem = rawItem
        ).exerciseId
    }

    var selectedExercise by remember {
        mutableStateOf<SelectedHardExercise?>(null)
    }

    var noteEditorExercise by remember {
        mutableStateOf<SelectedHardExercise?>(null)
    }

    var noteDraft by remember {
        mutableStateOf("")
    }

    var notesRefreshKey by remember {
        mutableIntStateOf(0)
    }

    var exclusionsRefreshKey by remember {
        mutableIntStateOf(0)
    }

    fun hardNoteAliasesFor(
        belt: Belt,
        topic: String,
        rawItem: String
    ): Set<String> {
        return syncIdentityFor(
            belt = belt,
            topic = topic,
            rawItem = rawItem
        ).aliases
    }

    fun exclusionKeyFor(
        belt: Belt,
        topic: String
    ): String {
        return buildString {
            append("excluded_")
            append(belt.id)
            append("_")
            append(
                normalizeStatusPart(
                    topic
                )
            )
        }
    }

    fun hardExclusionAliasesFor(
        belt: Belt,
        topic: String,
        rawItem: String
    ): Set<String> {

        val registryResolved =
            ExerciseIdentityRegistry.resolve(
                belt = belt,
                hebrewTitle = rawItem,
                topicKey = null
            )

        val knownIdentity =
            if (registryResolved.isKnown) {
                ExerciseIdentityRegistry.knownById(
                    registryResolved.id
                )
            } else {
                null
            }

        val candidateTopics =
            buildSet {

                if (topic.isNotBlank()) {
                    add(
                        topic.trim()
                    )
                }

                knownIdentity
                    ?.topicKeys
                    ?.forEach { topicKey ->
                        if (topicKey.isNotBlank()) {
                            add(
                                topicKey.trim()
                            )
                        }
                    }
            }

        return buildSet {

            add(
                rawItem.trim()
            )

            add(
                registryResolved.id
            )

            knownIdentity?.let { identity ->

                add(
                    identity.hebrewTitle
                )

                addAll(
                    identity.aliases
                )
            }

            candidateTopics.forEach { candidateTopic ->

                val syncIdentity =
                    ExerciseSyncStore.resolveIdentity(
                        belt = belt,
                        topic = candidateTopic,
                        subTopic = null,
                        rawItem = rawItem
                    )

                add(
                    syncIdentity.exerciseId
                )

                addAll(
                    syncIdentity.aliases
                )
            }
        }
            .map { value ->
                normalizeStatusPart(
                    value
                )
            }
            .filter { value ->
                value.isNotBlank()
            }
            .toSet()
    }

    fun isHardExcluded(
        belt: Belt,
        topic: String,
        rawItem: String
    ): Boolean {
        val aliases =
            hardExclusionAliasesFor(
                belt = belt,
                topic = topic,
                rawItem = rawItem
            )

        val exclusionKeys =
            prefs.all.keys
                .filter { key ->
                    key.startsWith(
                        "excluded_${belt.id}_"
                    )
                }

        return exclusionKeys.any { key ->

            val stored =
                prefs.getStringSet(
                    key,
                    emptySet()
                )
                    ?: emptySet()

            stored.any { storedValue ->
                normalizeStatusPart(
                    storedValue
                ) in aliases
            }
        }
    }

    fun toggleHardExcluded(
        belt: Belt,
        topic: String,
        rawItem: String
    ) {
        val identity =
            syncIdentityFor(
                belt = belt,
                topic = topic,
                rawItem = rawItem
            )

        val exclusionKeys =
            prefs.all.keys
                .filter { key ->
                    key.startsWith(
                        "excluded_${belt.id}_"
                    )
                }

        val normalizedAliases =
            hardExclusionAliasesFor(
                belt = belt,
                topic = topic,
                rawItem = rawItem
            )

        val currentlyExcluded =
            exclusionKeys.any { key ->

                val stored =
                    prefs.getStringSet(
                        key,
                        emptySet()
                    )
                        ?: emptySet()

                val normalizedStored =
                    stored
                        .map { value ->
                            normalizeStatusPart(
                                value
                            )
                        }
                        .filter { value ->
                            value.isNotBlank()
                        }
                        .toSet()

                normalizedAliases.any { alias ->
                    alias in normalizedStored
                }
            }

        if (currentlyExcluded) {

            prefs.edit {

                exclusionKeys.forEach { key ->

                    val stored =
                        (
                                prefs.getStringSet(
                                    key,
                                    emptySet()
                                )
                                    ?: emptySet()
                                )
                            .toMutableSet()

                    stored.removeAll { storedValue ->
                        normalizeStatusPart(
                            storedValue
                        ) in normalizedAliases
                    }

                    putStringSet(
                        key,
                        stored
                    )
                }
            }

        } else {

            val key =
                exclusionKeyFor(
                    belt = belt,
                    topic = topic
                )

            val stored =
                (
                        prefs.getStringSet(
                            key,
                            emptySet()
                        )
                            ?: emptySet()
                        )
                    .toMutableSet()

            stored.addAll(
                identity.aliases
            )

            prefs.edit {
                putStringSet(
                    key,
                    stored
                )
            }
        }

        exclusionsRefreshKey++
    }

    fun loadHardNote(
        belt: Belt,
        topic: String,
        rawItem: String
    ): String {

        val aliases =
            hardNoteAliasesFor(
                belt = belt,
                topic = topic,
                rawItem = rawItem
            )

        val primaryId =
            aliases.firstOrNull()
                ?: return ""

        return ExerciseNotesStore.loadNote(
            context = context,
            belt = belt,
            exerciseId = primaryId,
            aliases = aliases,
            allowLegacyMigration = true
        )
    }

    fun saveHardNote(
        belt: Belt,
        topic: String,
        rawItem: String,
        note: String
    ) {

        val aliases =
            hardNoteAliasesFor(
                belt = belt,
                topic = topic,
                rawItem = rawItem
            )

        val primaryId =
            aliases.firstOrNull()
                ?: return

        ExerciseNotesStore.saveNote(
            context = context,
            belt = belt,
            exerciseId = primaryId,
            note = note,
            aliases = aliases
        )

        notesRefreshKey++
    }

    fun deleteHardNote(
        belt: Belt,
        topic: String,
        rawItem: String
    ) {

        val aliases =
            hardNoteAliasesFor(
                belt = belt,
                topic = topic,
                rawItem = rawItem
            )

        val primaryId =
            aliases.firstOrNull()
                ?: return

        ExerciseNotesStore.deleteNote(
            context = context,
            belt = belt,
            exerciseId = primaryId,
            aliases = aliases
        )

        notesRefreshKey++
    }

    val flatRows: List<HardBeltListRow> =
        remember(
            groups,
            title
        ) {
            buildList {
                groups.forEach { group ->
                    add(
                        HardBeltListRow.BeltHeader(
                            belt = group.belt
                        )
                    )

                    hardItemsOf(group).forEachIndexed { index, rawItem ->
                        add(
                            HardBeltListRow.Exercise(
                                belt = group.belt,
                                index = index,
                                rawItem = rawItem,
                                statusId = hardStatusIdFor(
                                    belt = group.belt,
                                    topic = title,
                                    rawItem = rawItem
                                )
                            )
                        )
                    }
                }
            }
        }

    val listState = rememberLazyListState()

    val currentStickyBelt by remember(flatRows, listState) {
        derivedStateOf {
            flatRows
                .getOrNull(listState.firstVisibleItemIndex)
                ?.belt
                ?: groups.firstOrNull()?.belt
                ?: Belt.YELLOW
        }
    }

    val currentStickyExercises =
        remember(
            flatRows,
            currentStickyBelt
        ) {
            flatRows
                .filterIsInstance<HardBeltListRow.Exercise>()
                .filter { row ->
                    row.belt == currentStickyBelt
                }
        }

    val currentGroupTotalCount =
        currentStickyExercises.size

    val currentGroupKnownCount =
        currentStickyExercises.count { row ->
            hardItemStates[row.statusId] == true
        }

    val currentGroupUnknownCount =
        currentStickyExercises.count { row ->
            hardItemStates[row.statusId] == false &&
                    hardPartiallyKnownStates[row.statusId] != true
        }

    val currentGroupFavoriteCount =
        currentStickyExercises.count { row ->
            hardFavoriteIdFor(
                belt = row.belt,
                topic = title,
                rawItem = row.rawItem
            ) in favoriteIds
        }

    val currentGroupUnmarkedCount =
        currentStickyExercises.count { row ->
            hardItemStates[row.statusId] == null
        }

    val currentGroupTaughtCount =
        currentStickyExercises.count { row ->
            coachProgressStates[
                row.statusId
            ]?.isSelected(
                CoachMaterialStatus.TAUGHT
            ) == true
        }

    val currentGroupPracticedCount =
        currentStickyExercises.count { row ->
            coachProgressStates[
                row.statusId
            ]?.isSelected(
                CoachMaterialStatus.PRACTICED
            ) == true
        }

    val currentGroupReinforcementCount =
        currentStickyExercises.count { row ->
            coachProgressStates[
                row.statusId
            ]?.isSelected(
                CoachMaterialStatus.NEEDS_REINFORCEMENT
            ) == true
        }

    val currentGroupCoachUnmarkedCount =
        currentStickyExercises.count { row ->
            coachProgressStates[
                row.statusId
            ]
                ?.selectedStatuses
                .orEmpty()
                .isEmpty()
        }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        HardBeltStickyHeader(
            belt = currentStickyBelt,
            count = currentGroupTotalCount,
            isCoach = isCoach,
            taughtCount = currentGroupTaughtCount,
            practicedCount = currentGroupPracticedCount,
            reinforcementCount =
                currentGroupReinforcementCount,
            coachUnmarkedCount =
                currentGroupCoachUnmarkedCount
                    .coerceAtLeast(0),
            knownCount = currentGroupKnownCount,
            unknownCount = currentGroupUnknownCount,
            favoriteCount = currentGroupFavoriteCount,
            unmarkedCount = currentGroupUnmarkedCount,
            isEnglish = isEnglish,
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding =
                PaddingValues(
                    bottom = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(
                items = flatRows,
                key = { _, row ->
                    when (row) {
                        is HardBeltListRow.BeltHeader ->
                            "hard_belt_header_${row.belt.id}"

                        is HardBeltListRow.Exercise ->
                            "hard_row_${row.belt.id}_${row.statusId}"
                    }
                },
                contentType = { _, row ->
                    when (row) {
                        is HardBeltListRow.BeltHeader ->
                            "belt_header"

                        is HardBeltListRow.Exercise ->
                            "exercise"
                    }
                }
            ) { index, row ->

                when (row) {
                    is HardBeltListRow.BeltHeader -> {
                        if (index != 0) {

                            val beltExercises =
                                flatRows
                                    .filterIsInstance<HardBeltListRow.Exercise>()
                                    .filter { exercise ->
                                        exercise.belt == row.belt
                                    }

                            val beltTaughtCount =
                                beltExercises.count { exercise ->
                                    coachProgressStates[
                                        exercise.statusId
                                    ]?.isSelected(
                                        CoachMaterialStatus.TAUGHT
                                    ) == true
                                }

                            val beltPracticedCount =
                                beltExercises.count { exercise ->
                                    coachProgressStates[
                                        exercise.statusId
                                    ]?.isSelected(
                                        CoachMaterialStatus.PRACTICED
                                    ) == true
                                }

                            val beltReinforcementCount =
                                beltExercises.count { exercise ->
                                    coachProgressStates[
                                        exercise.statusId
                                    ]?.isSelected(
                                        CoachMaterialStatus.NEEDS_REINFORCEMENT
                                    ) == true
                                }

                            val beltCoachUnmarkedCount =
                                beltExercises.count { exercise ->
                                    coachProgressStates[
                                        exercise.statusId
                                    ]
                                        ?.selectedStatuses
                                        .orEmpty()
                                        .isEmpty()
                                }

                            val beltKnownCount =
                                beltExercises.count { exercise ->
                                    hardItemStates[
                                        exercise.statusId
                                    ] == true
                                }

                            val beltUnknownCount =
                                beltExercises.count { exercise ->
                                    hardItemStates[
                                        exercise.statusId
                                    ] == false &&
                                            hardPartiallyKnownStates[
                                                exercise.statusId
                                            ] != true
                                }

                            val beltFavoriteCount =
                                beltExercises.count { exercise ->
                                    hardFavoriteIdFor(
                                        belt = exercise.belt,
                                        topic = title,
                                        rawItem = exercise.rawItem
                                    ) in favoriteIds
                                }

                            val beltUnmarkedCount =
                                beltExercises.count { exercise ->
                                    hardItemStates[
                                        exercise.statusId
                                    ] == null
                                }

                            Spacer(
                                modifier =
                                    Modifier.height(42.dp)
                            )

                            HardBeltStickyHeader(
                                belt = row.belt,
                                count = beltExercises.size,
                                isCoach = isCoach,
                                taughtCount = beltTaughtCount,
                                practicedCount = beltPracticedCount,
                                reinforcementCount = beltReinforcementCount,
                                coachUnmarkedCount = beltCoachUnmarkedCount,
                                knownCount = beltKnownCount,
                                unknownCount = beltUnknownCount,
                                favoriteCount = beltFavoriteCount,
                                unmarkedCount = beltUnmarkedCount,
                                isEnglish = isEnglish,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    is HardBeltListRow.Exercise -> {
                        val belt = row.belt
                        val rawItem = row.rawItem
                        val statusId = row.statusId
                        val favoriteId =
                            hardFavoriteIdFor(
                                belt = belt,
                                topic = title,
                                rawItem = rawItem
                            )

                        val mastered =
                            hardItemStates[statusId]

                        val displayItem =
                            if (isEnglish) {
                                translateHardExerciseTitle(rawItem)
                            } else {
                                rawItem
                            }

                        val isFavorite =
                            favoriteId in favoriteIds

                        val isExcluded =
                            exclusionsRefreshKey.let {
                                isHardExcluded(
                                    belt = belt,
                                    topic = title,
                                    rawItem = rawItem
                                )
                            }

                        val noteText =
                            remember(
                                belt,
                                rawItem,
                                title,
                                notesRefreshKey
                            ) {
                                loadHardNote(
                                    belt = belt,
                                    topic = title,
                                    rawItem = rawItem
                                )
                            }

                        HardExerciseRowCard(
                            belt = belt,
                            item = displayItem,
                            isExcluded = isExcluded,
                            mastered = mastered,
                            partiallyKnown =
                                hardPartiallyKnownStates[
                                    statusId
                                ] == true,
                            isFavorite = isFavorite,
                            hasNote = noteText.isNotBlank(),
                            isEnglish = isEnglish,
                            isCoach = isCoach,
                            coachProgress =
                                coachProgressStates[statusId]
                                    ?: CoachMaterialProgress(),
                            onCoachStatusSelect = { selectedStatus ->
                                saveCoachProgress(
                                    belt = belt,
                                    statusId = statusId,
                                    status = selectedStatus
                                )
                            },
                            onTraineeStatusSelect = { selectedStatus ->

                                val nextValue =
                                    when (selectedStatus) {
                                        TraineeMaterialStatus.KNOWN ->
                                            true

                                        TraineeMaterialStatus.PARTIALLY_KNOWN ->
                                            false

                                        TraineeMaterialStatus.UNKNOWN ->
                                            false

                                        null ->
                                            null
                                    }

                                val isPartiallyKnown =
                                    selectedStatus ==
                                            TraineeMaterialStatus
                                                .PARTIALLY_KNOWN

                                hardItemStates[statusId] =
                                    nextValue

                                hardPartiallyKnownStates[
                                    statusId
                                ] =
                                    isPartiallyKnown

                                val statusKeys =
                                    hardStatusKeysFor(
                                        topic = title
                                    )

                                statusKeys.forEach { key ->
                                    vm.setItemStatusNullable(
                                        belt = belt,
                                        topic = key,
                                        item = statusId,
                                        value = nextValue
                                    )
                                }

                                persistenceScope.launch(
                                    Dispatchers.IO
                                ) {
                                    setHardLocalStatus(
                                        belt = belt,
                                        topic = title,
                                        statusId = statusId,
                                        value = nextValue,
                                        partiallyKnown =
                                            isPartiallyKnown
                                    )
                                }
                            },
                            onToggleFavorite = {
                                FavoritesStore.toggle(
                                    favoriteId
                                )
                            },
                            onToggleExclude = {
                                toggleHardExcluded(
                                    belt = belt,
                                    topic = title,
                                    rawItem = rawItem
                                )
                            },
                            onInfoClick = {
                                selectedExercise =
                                    SelectedHardExercise(
                                        belt = belt,
                                        topic = title,
                                        rawItem = rawItem,
                                        displayItem = displayItem
                                    )
                            }
                        )
                    }
                }
            }
        }
    }

    selectedExercise?.let { selected ->

        val explanation =
            remember(
                selected.belt,
                selected.rawItem,
                isEnglish
            ) {
                val raw =
                    Explanations
                        .get(
                            selected.belt,
                            selected.rawItem
                        )
                        .trim()

                if (raw.isBlank()) {
                    if (isEnglish) {
                        "There is no explanation for this exercise yet."
                    } else {
                        "אין כרגע הסבר לתרגיל הזה."
                    }
                } else {
                    if ("::" in raw) {
                        raw.substringAfter("::").trim()
                    } else {
                        raw
                    }
                }
            }

        val favoriteId =
            hardFavoriteIdFor(
                belt = selected.belt,
                topic = selected.topic,
                rawItem = selected.rawItem
            )

        val selectedNoteText =
            remember(
                selected.belt,
                selected.topic,
                selected.rawItem,
                notesRefreshKey
            ) {
                loadHardNote(
                    belt = selected.belt,
                    topic = selected.topic,
                    rawItem = selected.rawItem
                )
            }

        ExerciseExplanationDialog(
            title = selected.displayItem,
            beltLabel =
                beltTitle(
                    belt = selected.belt,
                    isEnglish = isEnglish
                ),
            explanation = explanation,
            noteText = selectedNoteText,
            isFavorite = favoriteId in favoriteIds,
            accentColor = selected.belt.color,
            isEnglish = isEnglish,
            onDismiss = {
                selectedExercise = null
            },
            onEditNote = {
                noteEditorExercise =
                    selected

                noteDraft =
                    loadHardNote(
                        belt = selected.belt,
                        topic = selected.topic,
                        rawItem = selected.rawItem
                    )
            },
            onDeleteNote = {
                deleteHardNote(
                    belt = selected.belt,
                    topic = selected.topic,
                    rawItem = selected.rawItem
                )

                noteDraft = ""
            },
            onToggleFavorite = {
                FavoritesStore.toggle(
                    favoriteId
                )
            }
        )
    }

    noteEditorExercise?.let { exercise ->

        ExerciseNoteEditorDialog(
            exerciseTitle =
                exercise.displayItem,
            noteText =
                noteDraft,
            isEnglish =
                isEnglish,
            accentColor =
                exercise.belt.color,
            onNoteChange = { value ->
                noteDraft = value
            },
            onDismiss = {
                noteEditorExercise = null
            },
            onSave = {
                val cleanNote =
                    noteDraft.trim()

                saveHardNote(
                    belt = exercise.belt,
                    topic = exercise.topic,
                    rawItem = exercise.rawItem,
                    note = cleanNote
                )

                noteDraft =
                    cleanNote

                noteEditorExercise =
                    null
            },
            onDelete = {
                deleteHardNote(
                    belt = exercise.belt,
                    topic = exercise.topic,
                    rawItem = exercise.rawItem
                )

                noteDraft = ""

                noteEditorExercise =
                    null
            }
        )
    }
}

@Composable
private fun HardBeltStickyHeader(
    belt: Belt,
    count: Int,
    isCoach: Boolean,
    taughtCount: Int,
    practicedCount: Int,
    reinforcementCount: Int,
    coachUnmarkedCount: Int,
    knownCount: Int,
    unknownCount: Int,
    favoriteCount: Int,
    unmarkedCount: Int,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val beltContentColor =
        if (
            isDarkMode &&
            belt == Belt.BLACK
        ) {
            Color.White.copy(alpha = 0.94f)
        } else {
            belt.color
        }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush =
                            kmiSectionHeaderBrush()
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    ),
            contentAlignment =
                Alignment.Center
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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {
                    Text(
                        text =
                            beltTitle(
                                belt = belt,
                                isEnglish = isEnglish
                            ),
                        modifier =
                            Modifier.fillMaxWidth(),
                        style =
                            KmiTypography
                                .sectionTitle
                                .copy(
                                    fontWeight =
                                        FontWeight.Black
                                ),
                        color =
                            if (belt == Belt.WHITE) {
                                kmiSectionHeaderContentColor()
                            } else {
                                belt.color
                            },
                        textAlign =
                            TextAlign.Center,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Spacer(
                        Modifier.height(1.dp)
                    )

                    Text(
                        text =
                            if (isEnglish) {
                                if (count == 1) {
                                    "1 exercise"
                                } else {
                                    "$count exercises"
                                }
                            } else {
                                "\u200E$count\u200E תרגילים"
                            },
                        modifier =
                            Modifier.fillMaxWidth(),
                        style =
                            KmiTypography
                                .caption
                                .copy(
                                    fontWeight =
                                        FontWeight.Bold
                                ),
                        color =
                            kmiSectionHeaderContentColor()
                                .copy(alpha = 0.92f),
                        textAlign =
                            TextAlign.Center,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.55f
                    )
                )
                .padding(
                    start = 6.dp,
                    top = 5.dp,
                    end = 6.dp,
                    bottom = 5.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCoach) {
                HardTopStatChip(
                    value = coachUnmarkedCount.toString(),
                    label =
                        if (isEnglish) {
                            "Unmarked"
                        } else {
                            "לא סומן"
                        },
                    containerColor = Color(0xFFE7EDF5),
                    contentColor = Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                )

                HardTopStatChip(
                    value = reinforcementCount.toString(),
                    label =
                        if (isEnglish) {
                            "Reinforcement"
                        } else {
                            "לחיזוק"
                        },
                    containerColor = Color(0xFFFFF1D6),
                    contentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )

                HardTopStatChip(
                    value = practicedCount.toString(),
                    label =
                        if (isEnglish) {
                            "Practiced"
                        } else {
                            "תורגל"
                        },
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer,
                    contentColor =
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                HardTopStatChip(
                    value = taughtCount.toString(),
                    label =
                        if (isEnglish) {
                            "Taught"
                        } else {
                            "נלמד"
                        },
                    containerColor = Color(0xFFDFF7E9),
                    contentColor = Color(0xFF16A36A),
                    modifier = Modifier.weight(1f)
                )
            } else {
                HardTopStatChip(
                    value = unmarkedCount.toString(),
                    label =
                        if (isEnglish) {
                            "Unmarked"
                        } else {
                            "לא סומן"
                        },
                    containerColor = Color(0xFFE7EDF5),
                    contentColor = Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                )

                HardTopStatChip(
                    value = favoriteCount.toString(),
                    label =
                        if (isEnglish) {
                            "Favorites"
                        } else {
                            "מועדפים"
                        },
                    containerColor = Color(0xFFFFF4CC),
                    contentColor = Color(0xFFE0A000),
                    modifier = Modifier.weight(1f)
                )

                HardTopStatChip(
                    value = unknownCount.toString(),
                    label =
                        if (isEnglish) {
                            "Unknown"
                        } else {
                            "לא יודע"
                        },
                    containerColor = Color(0xFFFFE3E3),
                    contentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )

                HardTopStatChip(
                    value = knownCount.toString(),
                    label =
                        if (isEnglish) {
                            "Known"
                        } else {
                            "יודע"
                        },
                    containerColor = Color(0xFFDFF7E9),
                    contentColor = Color(0xFF16A36A),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    beltContentColor.copy(alpha = 0.75f)
                )
        )
    }
}

@Composable
private fun HardExerciseRowCard(
    belt: Belt,
    item: String,
    isExcluded: Boolean,
    mastered: Boolean?,
    partiallyKnown: Boolean,
    isFavorite: Boolean,
    hasNote: Boolean,
    isEnglish: Boolean,
    isCoach: Boolean,
    coachProgress: CoachMaterialProgress,
    onCoachStatusSelect: (CoachMaterialStatus) -> Unit,
    onTraineeStatusSelect: (TraineeMaterialStatus?) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleExclude: () -> Unit,
    onInfoClick: () -> Unit
) {
    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    if (isCoach) {
        CompositionLocalProvider(
            LocalLayoutDirection provides
                    if (isEnglish) {
                        LayoutDirection.Ltr
                    } else {
                        LayoutDirection.Rtl
                    }
        ) {
            val exerciseCardShape =
                RoundedCornerShape(18.dp)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp
                    ),
                shape = exerciseCardShape,
                color =
                    MaterialTheme.colorScheme.surface,
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            if (isDarkMode) {
                                belt.color.copy(
                                    alpha = 0.55f
                                )
                            } else {
                                MaterialTheme.colorScheme
                                    .outlineVariant
                                    .copy(alpha = 0.85f)
                            }
                    ),
                tonalElevation = 0.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 4.dp,
                            vertical = 6.dp
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 2.dp,
                                end = 2.dp
                            ),
                        horizontalAlignment =
                            if (isEnglish) {
                                Alignment.Start
                            } else {
                                Alignment.End
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 10.dp,
                                    end = 3.dp,
                                    top = 2.dp,
                                    bottom = 2.dp
                                ),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text =
                                    if (isEnglish) {
                                        item.trim()
                                    } else {
                                        "\u200F${item.trim()}\u200F"
                                    },
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(onClick = onInfoClick),
                                color =
                                    MaterialTheme.colorScheme.onSurface,
                                style =
                                    KmiTypography.body.copy(
                                        fontWeight =
                                            FontWeight.SemiBold
                                    ),
                                maxLines = 3,
                                overflow =
                                    TextOverflow.Ellipsis
                            )

                            if (
                                isExcluded ||
                                isFavorite ||
                                hasNote
                            ) {
                                Spacer(
                                    modifier =
                                        Modifier.width(6.dp)
                                )

                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally,
                                    verticalArrangement =
                                        Arrangement.spacedBy(3.dp)
                                ) {
                                    if (isExcluded) {
                                        Surface(
                                            shape =
                                                RoundedCornerShape(10.dp),
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .surfaceVariant,
                                            border =
                                                BorderStroke(
                                                    width = 1.dp,
                                                    color =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onSurfaceVariant
                                                            .copy(alpha = 0.14f)
                                                ),
                                            shadowElevation = 0.dp
                                        ) {
                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "Excluded"
                                                    } else {
                                                        "מוחרג"
                                                    },
                                                style =
                                                    KmiTypography
                                                        .caption
                                                        .copy(
                                                            fontWeight =
                                                                FontWeight.ExtraBold
                                                        ),
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant,
                                                modifier =
                                                    Modifier.padding(
                                                        horizontal = 7.dp,
                                                        vertical = 2.dp
                                                    ),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (isFavorite) {
                                        Surface(
                                            shape =
                                                RoundedCornerShape(10.dp),
                                            color =
                                                Color(0xFFF9D9B8),
                                            border =
                                                BorderStroke(
                                                    width = 1.dp,
                                                    color =
                                                        Color(0xFF9A5A00)
                                                            .copy(
                                                                alpha = 0.14f
                                                            )
                                                ),
                                            shadowElevation = 0.dp
                                        ) {
                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "Favorite"
                                                    } else {
                                                        "מועדף"
                                                    },
                                                style =
                                                    KmiTypography
                                                        .caption
                                                        .copy(
                                                            fontWeight =
                                                                FontWeight.ExtraBold
                                                        ),
                                                color =
                                                    Color(0xFF9A5A00),
                                                modifier =
                                                    Modifier.padding(
                                                        horizontal = 7.dp,
                                                        vertical = 2.dp
                                                    ),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (hasNote) {
                                        Surface(
                                            shape =
                                                RoundedCornerShape(10.dp),
                                            color =
                                                Color(0xFFFFE7B3),
                                            border =
                                                BorderStroke(
                                                    width = 1.dp,
                                                    color =
                                                        Color(0xFF8A5A00)
                                                            .copy(
                                                                alpha = 0.18f
                                                            )
                                                ),
                                            shadowElevation = 0.dp
                                        ) {
                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "Note"
                                                    } else {
                                                        "הערה"
                                                    },
                                                style =
                                                    KmiTypography
                                                        .caption
                                                        .copy(
                                                            fontWeight =
                                                                FontWeight.ExtraBold
                                                        ),
                                                color =
                                                    Color(0xFF8A5A00),
                                                modifier =
                                                    Modifier.padding(
                                                        horizontal = 7.dp,
                                                        vertical = 2.dp
                                                    ),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(3.dp)
                    )

                    Box(
                        modifier =
                            Modifier.fillMaxWidth(),
                        contentAlignment =
                            if (isEnglish) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            }
                    ) {
                        CoachMaterialStatusSelector(
                            progress = coachProgress,
                            isEnglish = isEnglish,
                            modifier = Modifier.fillMaxWidth(),
                            excluded = isExcluded,
                            isFav = isFavorite,
                            hasNote = hasNote,
                            onToggleExclude = onToggleExclude,
                            onInfo = onInfoClick,
                            onToggleFavorite = onToggleFavorite,
                            onEditNote = {},
                            onSelect = onCoachStatusSelect
                        )
                    }
                }
            }
        }

        return
    }

    val traineeStatus =
        when {
            mastered == true ->
                TraineeMaterialStatus.KNOWN

            partiallyKnown ->
                TraineeMaterialStatus.PARTIALLY_KNOWN

            mastered == false ->
                TraineeMaterialStatus.UNKNOWN

            else ->
                null
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp
                ),
        shape =
            RoundedCornerShape(18.dp),
        color =
            MaterialTheme
                .colorScheme
                .surface,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (isDarkMode) {
                        belt.color.copy(
                            alpha = 0.55f
                        )
                    } else {
                        MaterialTheme
                            .colorScheme
                            .outlineVariant
                            .copy(alpha = 0.85f)
                    }
            ),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 4.dp,
                        vertical = 6.dp
                    )
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 10.dp,
                            end = 6.dp,
                            top = 2.dp,
                            bottom = 3.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            item.trim()
                        } else {
                            "\u200F${item.trim()}\u200F"
                        },
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable(
                                onClick =
                                    onInfoClick
                            ),
                    style =
                        KmiTypography
                            .body
                            .copy(
                                fontWeight =
                                    FontWeight.SemiBold
                            ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                    maxLines = 3,
                    overflow =
                        TextOverflow.Ellipsis
                )

                if (
                    isExcluded ||
                    isFavorite ||
                    hasNote
                ) {
                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(3.dp)
                    ) {
                        if (isExcluded) {
                            Surface(
                                shape =
                                    RoundedCornerShape(10.dp),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant,
                                border =
                                    BorderStroke(
                                        width = 1.dp,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                                .copy(
                                                    alpha = 0.14f
                                                )
                                    ),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Excluded"
                                        } else {
                                            "מוחרג"
                                        },
                                    style =
                                        KmiTypography
                                            .caption
                                            .copy(
                                                fontWeight =
                                                    FontWeight.ExtraBold
                                            ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 7.dp,
                                            vertical = 2.dp
                                        ),
                                    maxLines = 1
                                )
                            }
                        }

                        if (isFavorite) {
                            Surface(
                                shape =
                                    RoundedCornerShape(10.dp),
                                color =
                                    Color(0xFFF9D9B8),
                                border =
                                    BorderStroke(
                                        width = 1.dp,
                                        color =
                                            Color(0xFF9A5A00)
                                                .copy(
                                                    alpha = 0.14f
                                                )
                                    ),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Favorite"
                                        } else {
                                            "מועדף"
                                        },
                                    style =
                                        KmiTypography
                                            .caption
                                            .copy(
                                                fontWeight =
                                                    FontWeight.ExtraBold
                                            ),
                                    color =
                                        Color(0xFF9A5A00),
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 7.dp,
                                            vertical = 2.dp
                                        ),
                                    maxLines = 1
                                )
                            }
                        }

                        if (hasNote) {
                            Surface(
                                shape =
                                    RoundedCornerShape(10.dp),
                                color =
                                    if (isDarkMode) {
                                        Color(0xFF5B4A22)
                                    } else {
                                        Color(0xFFFFE7B3)
                                    },
                                border =
                                    BorderStroke(
                                        width = 1.dp,
                                        color =
                                            if (isDarkMode) {
                                                Color(0xFFFFD978)
                                                    .copy(alpha = 0.18f)
                                            } else {
                                                Color(0xFF8A5A00)
                                                    .copy(alpha = 0.18f)
                                            }
                                    ),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Note"
                                        } else {
                                            "הערה"
                                        },
                                    style =
                                        KmiTypography
                                            .caption
                                            .copy(
                                                fontWeight =
                                                    FontWeight.ExtraBold
                                            ),
                                    color =
                                        if (isDarkMode) {
                                            Color(0xFFFFD978)
                                        } else {
                                            Color(0xFF8A5A00)
                                        },
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 7.dp,
                                            vertical = 2.dp
                                        ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            MaterialsExerciseStatusCard(
                isEnglish = isEnglish,
                infoWidth = 76.dp,
                info = {
                    Surface(
                        onClick = onInfoClick,
                        modifier =
                            Modifier.fillMaxSize(),
                        shape =
                            RoundedCornerShape(7.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier =
                                Modifier.fillMaxSize(),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Filled.Info,
                                contentDescription =
                                    if (isEnglish) {
                                        "Exercise information"
                                    } else {
                                        "מידע"
                                    },
                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary,
                                modifier =
                                    Modifier.size(12.dp)
                            )

                            Spacer(
                                Modifier.height(1.dp)
                            )

                            Text(
                                text =
                                    if (isEnglish) {
                                        "Info"
                                    } else {
                                        "מידע"
                                    },
                                style =
                                    KmiTypography
                                        .caption
                                        .copy(
                                            fontWeight =
                                                FontWeight.ExtraBold
                                        ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary,
                                textAlign =
                                    TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            ) {
                TraineeMaterialStatusSelector(
                    selectedStatus =
                        traineeStatus,
                    dateText = "",
                    isEnglish = isEnglish,
                    showSymbols = false,
                    onSelect =
                        onTraineeStatusSelect
                )
            }
        }
    }
}

private fun beltTitle(belt: Belt, isEnglish: Boolean): String =
    if (isEnglish) {
        when (belt) {
            Belt.YELLOW -> "Yellow Belt"
            Belt.ORANGE -> "Orange Belt"
            Belt.GREEN -> "Green Belt"
            Belt.BLUE -> "Blue Belt"
            Belt.BROWN -> "Brown Belt"
            Belt.BLACK -> "Black Belt"
            else -> belt.name
        }
    } else {
        when (belt) {
            Belt.YELLOW -> "חגורה צהובה"
            Belt.ORANGE -> "חגורה כתומה"
            Belt.GREEN -> "חגורה ירוקה"
            Belt.BLUE -> "חגורה כחולה"
            Belt.BROWN -> "חגורה חומה"
            Belt.BLACK -> "חגורה שחורה"
            else -> belt.name
        }
    }

private fun translateHardExerciseTitle(
    raw: String
): String {
    val clean = raw.trim()

    return ExerciseTitlesEn
        .getOrSame(clean)
        .trim()
        .ifBlank { clean }
}

private fun translateHardTopicTitle(
    raw: String
): String {
    val clean = raw.trim()

    return ExerciseTitlesEn
        .getOrSame(clean)
        .trim()
        .ifBlank { clean }
}


