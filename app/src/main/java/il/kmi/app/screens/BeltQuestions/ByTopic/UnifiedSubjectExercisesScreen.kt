@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Explanations
import il.kmi.app.domain.color
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.pdf.KmiPdfDirection
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.shared.domain.Belt
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.content.HardSectionsResolver
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.LocalizationRuntime


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

@Composable
fun UnifiedSubjectExercisesScreen(
    subjectId: String,
    sectionId: String? = null,
    onOpenSection: (subjectId: String, sectionId: String?) -> Unit,
    onBack: () -> Unit,
    vm: KmiViewModel = viewModel()
) {
    val isEnglish = LocalizationRuntime.currentLanguage == AppLanguage.ENGLISH
    val resolverSubjectId = remember(subjectId) {
        when (subjectId.trim()) {
            "kicks" -> "kicks_hard"
            else -> subjectId
        }
    }

    val result = remember(resolverSubjectId, sectionId) {
        HardSectionsResolver.resolve(resolverSubjectId, sectionId)
    }

    val combinedDefenseGroups = remember(resolverSubjectId) {
        combinedDefenseGroupsFor(resolverSubjectId)
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
            if (combinedDefenseGroups != null) {
                BeltGroupsContent(
                    title = subjectRootTitle(subjectId),
                    groups = combinedDefenseGroups,
                    isEnglish = isEnglish,
                    vm = vm,
                    modifier = Modifier.fillMaxSize()
                )
                return@Box
            }

            when (result) {
                is HardSectionsResolver.NodeResult.Sections -> {
                    if (shouldShowSectionCards) {
                        SectionsContent(
                            subjectId = subjectId,
                            title = result.title,
                            entries = result.entries,
                            isEnglish = isEnglish,
                            onOpen = onOpenSection,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BeltGroupsContent(
                            title = result.title ?: subjectRootTitle(subjectId),
                            groups = flattenedSectionGroups.orEmpty(),
                            isEnglish = isEnglish,
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
        "releases" -> "שחרורים"
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
        when (val resolved = HardSectionsResolver.resolve(sectionId, null)) {
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
        when (val resolved = HardSectionsResolver.resolve(subjectId, entry.id)) {
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

@Composable
private fun SectionsContent(
    subjectId: String,
    title: String?,
    entries: List<HardSectionsResolver.SectionEntry>,
    isEnglish: Boolean,
    onOpen: (subjectId: String, sectionId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text =
                    if (isEnglish) {
                        translateHardTopicTitle(title ?: "נושאים")
                    } else {
                        title ?: "נושאים"
                    },
                style = KmiTypography.sectionTitle,
                textAlign =
                    if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text =
                    if (isEnglish) {
                        "Choose a sub-topic"
                    } else {
                        "בחר תת־נושא"
                    },
                style = KmiTypography.body,
                color = Color(0xFF6C6880),
                textAlign =
                    if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(entries) { entry ->
            SubjectSectionCard(
                title = if (isEnglish) translateHardTopicTitle(entry.title) else entry.title,
                count = entry.totalItemsCount,
                isEnglish = isEnglish,
                onClick = { onOpen(subjectId, entry.id) }
            )
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
    contentColor: Color = Color.White
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            contentColor.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = KmiTypography.cardTitle,
                color = contentColor,
                maxLines = 1
            )

            Text(
                text = label,
                style = KmiTypography.caption,
                color = contentColor.copy(alpha = 0.92f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HardExerciseMetaBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            contentColor.copy(alpha = 0.14f)
        ),
        shadowElevation = 0.dp
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

@Composable
private fun BeltGroupsContent(
    title: String,
    groups: List<HardSectionsResolver.BeltItems>,
    isEnglish: Boolean,
    vm: KmiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
    }

    val marksVersion by vm.marksVersion.collectAsState()
    val hardItemStates = remember(title) { mutableStateMapOf<String, Boolean?>() }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

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
        value: Boolean?
    ) {
        hardStatusKeysFor(
            topic = topic
        ).forEach { key ->
            val masteredKey = "mastered_${belt.id}_${key}"
            val unknownKey = "unknown_${belt.id}_${key}"

            val masteredSet =
                (prefs.getStringSet(masteredKey, emptySet<String>()) ?: emptySet()).toMutableSet()

            val unknownSet =
                (prefs.getStringSet(unknownKey, emptySet<String>()) ?: emptySet()).toMutableSet()

            when (value) {
                true -> {
                    masteredSet.add(statusId)
                    unknownSet.remove(statusId)
                }

                false -> {
                    unknownSet.add(statusId)
                    masteredSet.remove(statusId)
                }

                null -> {
                    masteredSet.remove(statusId)
                    unknownSet.remove(statusId)
                }
            }

            prefs.edit {
                putStringSet(masteredKey, masteredSet)
                putStringSet(unknownKey, unknownSet)
            }
        }
    }

    LaunchedEffect(groups, marksVersion) {
        groups.forEach { group: HardSectionsResolver.BeltItems ->
            val rawItems: List<String> = hardItemsOf(group)
            rawItems.forEach { rawItem: String ->
                val statusId = hardStatusIdFor(
                    belt = group.belt,
                    topic = title,
                    rawItem = rawItem
                )

                var valueFromVm: Boolean? = null

                for (key in hardStatusKeysFor(title)) {
                    val directStatus: Boolean? = try {
                        vm.getItemStatusNullable(
                            belt = group.belt,
                            topic = key,
                            item = statusId
                        )
                    } catch (_: Exception) {
                        null
                    }

                    val fromKey: Boolean? = directStatus ?: try {
                        if (
                            vm.isMastered(
                                belt = group.belt,
                                topic = key,
                                item = statusId
                            )
                        ) true else null
                    } catch (_: Exception) {
                        null
                    }

                    if (fromKey != null) {
                        valueFromVm = fromKey
                        break
                    }
                }

                if (valueFromVm == null) {
                    for (key in hardStatusKeysFor(title)) {
                        val masteredKey = "mastered_${group.belt.id}_${key}"
                        val unknownKey = "unknown_${group.belt.id}_${key}"

                        val masteredSet =
                            prefs.getStringSet(masteredKey, emptySet<String>()) ?: emptySet()
                        val unknownSet =
                            prefs.getStringSet(unknownKey, emptySet<String>()) ?: emptySet()

                        val localValue: Boolean? = when {
                            masteredSet.contains(statusId) -> true
                            unknownSet.contains(statusId) -> false
                            else -> null
                        }

                        if (localValue != null) {
                            valueFromVm = localValue

                            vm.setItemStatusNullable(
                                belt = group.belt,
                                topic = key,
                                item = statusId,
                                value = localValue
                            )

                            break
                        }
                    }
                }

                hardItemStates[statusId] = valueFromVm
            }
        }
    }

    val favoriteIds: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    fun hardFavoriteIdFor(
        belt: Belt,
        topic: String,
        rawItem: String
    ): String {
        return hardStatusIdFor(
            belt = belt,
            topic = topic,
            rawItem = rawItem
        )
    }

    var selectedExercise by remember { mutableStateOf<SelectedHardExercise?>(null) }

    val isKickDefenseScreen =
        title.trim() == "הגנות נגד בעיטות" ||
                title.trim() == "Defenses against kicks" ||
                title.trim() == "Defenses Against Kicks"

    val flatRows: List<Triple<Belt, Int, String>> = remember(groups, title) {
        groups.flatMap { group: HardSectionsResolver.BeltItems ->
            hardItemsOf(group).mapIndexed { index: Int, rawItem: String ->
                Triple(group.belt, index, rawItem)
            }
        }
    }

    val listState = rememberLazyListState()

    val currentStickyBelt by remember(flatRows, listState) {
        derivedStateOf {
            flatRows
                .getOrNull(listState.firstVisibleItemIndex)
                ?.first
                ?: groups.firstOrNull()?.belt
                ?: Belt.YELLOW
        }
    }

    val currentStickyGroup = groups.firstOrNull { it.belt == currentStickyBelt }
    val currentStickyItems: List<String> =
        currentStickyGroup?.let { hardItemsOf(it) }.orEmpty()

    val currentGroupTotalCount = currentStickyItems.size

    val currentGroupKnownCount = currentStickyItems.count { rawItem ->
        hardItemStates[hardStatusIdFor(currentStickyBelt, title, rawItem)] == true
    }

    val currentGroupUnknownCount = currentStickyItems.count { rawItem ->
        hardItemStates[hardStatusIdFor(currentStickyBelt, title, rawItem)] == false
    }

    val currentGroupFavoriteCount = currentStickyItems.count { rawItem ->
        hardFavoriteIdFor(currentStickyBelt, title, rawItem) in favoriteIds
    }

    val currentGroupUnmarkedCount = currentStickyItems.count { rawItem ->
        hardItemStates[hardStatusIdFor(currentStickyBelt, title, rawItem)] == null
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        HardBeltStickyHeader(
            belt = currentStickyBelt,
            count = currentGroupTotalCount,
            knownCount = currentGroupKnownCount,
            unknownCount = currentGroupUnknownCount,
            favoriteCount = currentGroupFavoriteCount,
            unmarkedCount = currentGroupUnmarkedCount,
            isEnglish = isEnglish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                bottom = 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = flatRows,
                key = { index, row ->
                    val belt = row.first
                    val rowIndex = row.second
                    val rawItem = row.third
                    "hard_row_${belt.id}_${rowIndex}_${rawItem}_$index"
                }
            ) { index, row ->
                val belt = row.first
                val rowIndex = row.second
                val rawItem = row.third

                val statusId = hardStatusIdFor(
                    belt = belt,
                    topic = title,
                    rawItem = rawItem
                )

                val favoriteId = hardFavoriteIdFor(
                    belt = belt,
                    topic = title,
                    rawItem = rawItem
                )

                val mastered = hardItemStates[statusId]
                val displayItem = if (isEnglish) translateHardExerciseTitle(rawItem) else rawItem
                val isFavorite = favoriteId in favoriteIds

                val sectionTitle = if (isKickDefenseScreen) {
                    kickDefenseSectionTitleFor(rawItem, isEnglish)
                } else {
                    null
                }

                val previousRawItem = flatRows
                    .getOrNull(index - 1)
                    ?.takeIf { it.first == belt }
                    ?.third

                val previousSectionTitle = if (previousRawItem != null && isKickDefenseScreen) {
                    kickDefenseSectionTitleFor(previousRawItem, isEnglish)
                } else {
                    null
                }

                if (sectionTitle != null && sectionTitle != previousSectionTitle) {
                    HardExerciseSectionHeader(
                        text = sectionTitle,
                        isEnglish = isEnglish
                    )
                }

                HardExerciseRowCard(
                    exerciseNumber = rowIndex + 1,
                    belt = belt,
                    item = displayItem,
                    mastered = mastered,
                    isFavorite = isFavorite,
                    isEnglish = isEnglish,
                    onStatusClick = {
                        val nextValue = when (hardItemStates[statusId]) {
                            null -> true
                            true -> false
                            false -> null
                        }

                        hardItemStates[statusId] = nextValue

                        hardStatusKeysFor(
                            topic = title
                        ).forEach { key ->
                            vm.setItemStatusNullable(
                                belt = belt,
                                topic = key,
                                item = statusId,
                                value = nextValue
                            )
                        }

                        setHardLocalStatus(
                            belt = belt,
                            topic = title,
                            statusId = statusId,
                            value = nextValue
                        )
                    },
                    onToggleFavorite = {
                        FavoritesStore.toggle(favoriteId)
                    },
                    onInfoClick = {
                        selectedExercise = SelectedHardExercise(
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

        ExerciseExplanationDialog(
            title = selected.displayItem,
            beltLabel =
                beltTitle(
                    belt = selected.belt,
                    isEnglish = isEnglish
                ),
            explanation = explanation,
            noteText = "",
            isFavorite = favoriteId in favoriteIds,
            accentColor = selected.belt.color,
            isEnglish = isEnglish,
            onDismiss = {
                selectedExercise = null
            },
            onEditNote = {},
            onDeleteNote = {},
            onToggleFavorite = {
                FavoritesStore.toggle(favoriteId)
            }
        )
    }
}

private fun kickDefenseSectionTitleFor(
    rawItem: String,
    isEnglish: Boolean
): String? {
    val clean = rawItem
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")
        .trim()

    return when {
        clean.contains("ברך") -> {
            if (isEnglish) "Defenses Against Knee Strikes" else "הגנות נגד ברך"
        }

        clean.contains("מגל") -> {
            if (isEnglish) "Defenses Against Round Kicks" else "הגנות נגד בעיטות מגל"
        }

        clean.contains("לצד") ||
                clean.contains("בעיטת צד") ||
                clean.contains("בעיטה צד") -> {
            if (isEnglish) "Defenses Against Side Kicks" else "הגנות נגד בעיטות לצד"
        }

        clean.contains("בעיטה ישרה") ||
                clean.contains("בעיטה רגילה") ||
                clean.contains("רגילה") ||
                clean.contains("ישרה") -> {
            if (isEnglish) "Defenses Against Regular Kicks" else "הגנות נגד בעיטה רגילה"
        }

        else -> null
    }
}

@Composable
private fun HardExerciseSectionHeader(
    text: String,
    isEnglish: Boolean
) {
    Text(
        text = text,
        style = KmiTypography.cardTitle,
        color = Color(0xFF4CAF50),
        textAlign =
            if (isEnglish) {
                TextAlign.Left
            } else {
                TextAlign.Right
            },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 8.dp,
                bottom = 4.dp
            )
    )
}

@Composable
private fun HardBeltStickyHeader(
    belt: Belt,
    count: Int,
    knownCount: Int,
    unknownCount: Int,
    favoriteCount: Int,
    unmarkedCount: Int,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = belt.color.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                color = belt.color.copy(alpha = 0.22f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = beltTitle(belt, isEnglish),
                        style = KmiTypography.sectionTitle,
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        modifier = Modifier.weight(1f)
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
                                "$count תרגילים"
                            },
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = belt.color,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HardTopStatChip(
                    value = count.toString(),
                    label = if (isEnglish) "Exercises" else "תרגילים",
                    containerColor = Color(0xFF98A2B3)
                )

                HardTopStatChip(
                    value = knownCount.toString(),
                    label = if (isEnglish) "Known" else "יודע",
                    containerColor = Color(0xFF7ACB88)
                )

                HardTopStatChip(
                    value = unknownCount.toString(),
                    label = if (isEnglish) "Unknown" else "לא יודע",
                    containerColor = Color(0xFFF1A97A)
                )

                HardTopStatChip(
                    value = favoriteCount.toString(),
                    label = if (isEnglish) "Favorites" else "מועדפים",
                    containerColor = Color(0xFFE7A3B5)
                )

                HardTopStatChip(
                    value = unmarkedCount.toString(),
                    label = if (isEnglish) "Unmarked" else "לא סומן",
                    containerColor = Color(0xFF8596C9)
                )
            }
        }
    }
}

@Composable
private fun HardExerciseRowCard(
    exerciseNumber: Int,
    belt: Belt,
    item: String,
    mastered: Boolean?,
    isFavorite: Boolean,
    isEnglish: Boolean,
    onStatusClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = 0.98f
        ),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = belt.color.copy(alpha = 0.32f)
        )
    ) {
        /*
         * הפריסה החיצונית נשארת LTR באופן פיזי:
         * עיגול הסטטוס משמאל ופס החגורה מימין.
         *
         * כיוון הטקסט בתוך העמודה נקבע בנפרד לפי השפה.
         */
        CompositionLocalProvider(
            LocalLayoutDirection provides
                    LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 5.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HardMasterToggle(
                    mastered = mastered,
                    onClick = onStatusClick
                )

                Spacer(Modifier.width(8.dp))

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
                            .weight(1f)
                            .clickable {
                                onInfoClick()
                            },
                        horizontalAlignment =
                            if (isEnglish) {
                                Alignment.Start
                            } else {
                                Alignment.End
                            }
                    ) {
                        /*
                         * מספר התרגיל ואייקון המידע נמצאים
                         * עכשיו באותה שורה ובאותו צד.
                         */
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HardExerciseMetaBadge(
                                text =
                                    if (isEnglish) {
                                        "No. $exerciseNumber"
                                    } else {
                                        "מס׳ $exerciseNumber"
                                    },
                                containerColor =
                                    belt.color.copy(alpha = 0.14f),
                                contentColor =
                                    MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.width(3.dp))

                            IconButton(
                                onClick = onInfoClick,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription =
                                        if (isEnglish) {
                                            "Exercise information"
                                        } else {
                                            "מידע על התרגיל"
                                        },
                                    tint =
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(Modifier.width(2.dp))

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector =
                                        if (isFavorite) {
                                            Icons.Filled.Star
                                        } else {
                                            Icons.Outlined.StarBorder
                                        },
                                    contentDescription =
                                        if (isFavorite) {
                                            if (isEnglish) {
                                                "Remove from favorites"
                                            } else {
                                                "הסר ממועדפים"
                                            }
                                        } else {
                                            if (isEnglish) {
                                                "Add to favorites"
                                            } else {
                                                "הוסף למועדפים"
                                            }
                                        },
                                    tint =
                                        if (isFavorite) {
                                            Color(0xFFFFC107)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Spacer(Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(1.dp))

                        /*
                         * אין יותר אייקונים חיצוניים שתופסים מקום
                         * בצד של הטקסט, ולכן שם התרגיל מתחיל
                         * מתחילת השורה ומקבל את כל רוחב העמודה.
                         */
                        Text(
                            text = item,
                            style = KmiTypography.body.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Start
                                } else {
                                    TextAlign.Right
                                },
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            belt.color.copy(alpha = 1f)
                        )
                )
            }
        }
    }
}

@Composable
private fun HardMasterToggle(
    mastered: Boolean?,
    onClick: () -> Unit
) {
    val bg = when (mastered) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> Color.White
    }

    val border = when (mastered) {
        true -> Color(0xFF1B5E20)
        false -> Color(0xFF8E1B1B)
        null -> Color(0xFFCBD5E1)
    }

    Surface(
        modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.5.dp, border),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (mastered) {
                true -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "יודע",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )

                false -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "לא יודע",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )

                null -> Spacer(Modifier.size(1.dp))
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


