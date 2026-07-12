@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package il.kmi.app.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.KmiViewModel
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.domain.SubjectTopic as AppSubjectTopic
import il.kmi.app.domain.TopicsBySubjectRegistry
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.color
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.SubjectTopic as SharedSubjectTopic
import il.kmi.shared.domain.content.SubjectItemsResolver
import androidx.compose.ui.unit.LayoutDirection
import il.kmi.app.favorites.FavoritesStore
import il.kmi.shared.domain.content.HardSectionsCatalog
import il.kmi.shared.domain.content.HardSectionsCatalog.itemsFor
import il.kmi.shared.domain.content.ExerciseIdentityRegistry


//=======================================================================

private fun toSharedBeltOrNull(rawId: String?): Belt? {
    val s = rawId?.trim().orEmpty()
    if (s.isBlank()) return null

    // 1) אם fromId יודע להתמודד עם "yellow" וכו'
    Belt.fromId(s)?.let { return it }

    // 2) אם הגיע "YELLOW" / "Yellow" / "yellow"
    runCatching { return Belt.valueOf(s.uppercase()) }.getOrNull()

    // 3) אם הגיע "חגורה צהובה" / "צהובה" וכו' (fallback עדין)
    val heb = s.replace("חגורה", "").trim()
    return Belt.order.firstOrNull { it.heb.contains(heb) || heb.contains(it.heb.replace("חגורה", "").trim()) }
}

// ✅ עדיף להוציא enum החוצה כדי שלא "יתבלבל" קומפיילר/IDE בתוך scope
private enum class FilterMode { ALL, FAVORITES, RECENTS }

private data class SubjectExercisePdfItem(
    val beltId: String,
    val beltHeb: String,
    val beltEn: String,
    val topic: String,
    val title: String,
    val mastered: Boolean?,
    val isFavorite: Boolean
)

private fun shareSubjectExercisesPdf(
    context: Context,
    screenTitle: String,
    items: List<SubjectExercisePdfItem>,
    isEnglish: Boolean
) {
    if (items.isEmpty()) {
        android.widget.Toast.makeText(
            context,
            if (isEnglish) {
                "No exercises to export"
            } else {
                "אין תרגילים לייצוא"
            },
            android.widget.Toast.LENGTH_SHORT
        ).show()

        return
    }

    val pdfFile = createSubjectExercisesPdf(
        context = context,
        screenTitle = screenTitle,
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
                "KAMI - $screenTitle"
            } else {
                "KAMI - $screenTitle"
            }
        )
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(
        sendIntent,
        if (isEnglish) "Share PDF" else "שיתוף PDF"
    )

    if (context !is android.app.Activity) {
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(chooser)
}

private fun createSubjectExercisesPdf(
    context: Context,
    screenTitle: String,
    items: List<SubjectExercisePdfItem>,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842

    val pageMargin = 28f
    val contentTop = 166f
    val footerTop = 804f
    val contentBottom = footerTop - 14f

    fun tr(he: String, en: String): String =
        if (isEnglish) en else he

    fun cleanText(raw: String): String =
        raw
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun beltTitle(item: SubjectExercisePdfItem): String {
        return if (isEnglish) {
            item.beltEn.ifBlank { item.beltId }
        } else {
            val clean = item.beltHeb.trim()

            if (clean.startsWith("חגורה")) {
                clean
            } else {
                "חגורה $clean"
            }
        }
    }

    fun statusTitle(value: Boolean?): String {
        return when (value) {
            true -> tr("יודע", "Known")
            false -> tr("לא יודע", "Unknown")
            null -> tr("לא סומן", "Unmarked")
        }
    }

    fun statusColor(value: Boolean?): Int {
        return when (value) {
            true -> android.graphics.Color.rgb(46, 125, 50)
            false -> android.graphics.Color.rgb(198, 40, 40)
            null -> android.graphics.Color.rgb(100, 116, 139)
        }
    }

    fun beltColor(beltId: String): Int {
        return when (beltId.trim().lowercase()) {
            "yellow" -> android.graphics.Color.rgb(245, 158, 11)
            "orange" -> android.graphics.Color.rgb(249, 115, 22)
            "green" -> android.graphics.Color.rgb(46, 125, 50)
            "blue" -> android.graphics.Color.rgb(30, 136, 229)
            "brown" -> android.graphics.Color.rgb(109, 76, 65)
            "black" -> android.graphics.Color.rgb(31, 41, 55)
            else -> android.graphics.Color.rgb(100, 116, 139)
        }
    }

    val document = PdfDocument()

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val mediumBlue = android.graphics.Color.rgb(36, 103, 158)
    val lightHeaderBlue = android.graphics.Color.rgb(128, 183, 220)

    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(100, 116, 139)

    val cardBackground = android.graphics.Color.rgb(246, 250, 253)
    val cardBorder = android.graphics.Color.rgb(203, 213, 225)
    val summaryBackground = android.graphics.Color.rgb(234, 246, 255)

    val regularTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    val boldTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun newPaint(
        size: Float,
        color: Int = textDark,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.RIGHT
    ): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) boldTypeface else regularTypeface
            textAlign = align
        }
    }

    val titlePaint = newPaint(
        size = 27f,
        color = android.graphics.Color.WHITE,
        bold = true
    )

    val subtitlePaint = newPaint(
        size = 13f,
        color = android.graphics.Color.WHITE
    )

    val smallPaint = newPaint(
        size = 9f,
        color = textMuted
    )

    val beltHeaderPaint = newPaint(
        size = 13f,
        color = android.graphics.Color.WHITE,
        bold = true
    )

    val exerciseTitlePaint = newPaint(
        size = 11.5f,
        color = textDark,
        bold = true
    )

    val exerciseMetaPaint = newPaint(
        size = 8.5f,
        color = textMuted
    )

    fun drawRoundRect(
        canvas: android.graphics.Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float,
        stroke: Boolean = false,
        strokeWidth: Float = 1f
    ) {
        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radius,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = if (stroke) {
                    Paint.Style.STROKE
                } else {
                    Paint.Style.FILL
                }
                this.strokeWidth = strokeWidth
            }
        )
    }

    fun fitText(
        raw: String,
        paint: Paint,
        maxWidth: Float
    ): String {
        val clean = cleanText(raw)

        if (paint.measureText(clean) <= maxWidth) {
            return clean
        }

        val suffix = "…"
        var value = clean

        while (
            value.isNotEmpty() &&
            paint.measureText(value + suffix) > maxWidth
        ) {
            value = value.dropLast(1)
        }

        return value.trimEnd() + suffix
    }

    fun drawKmiLogo(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {
        canvas.drawCircle(
            cx,
            cy,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
            }
        )

        canvas.drawCircle(
            cx,
            cy,
            radius - 4f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
            }
        )

        canvas.drawText(
            "KAMI",
            cx,
            cy + radius * 0.22f,
            newPaint(
                size = radius * 0.62f,
                color = navy,
                bold = true,
                align = Paint.Align.CENTER
            )
        )
    }

    fun drawHeader(canvas: android.graphics.Canvas) {
        canvas.drawColor(android.graphics.Color.WHITE)

        val headerBottom = 122f
        val headerTextRight = 435f

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(pageWidth.toFloat(), 0f)
                lineTo(pageWidth.toFloat(), headerBottom)
                lineTo(178f, headerBottom)
                lineTo(238f, 0f)
                close()
            },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
            }
        )

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(208f, headerBottom)
                lineTo(224f, headerBottom)
                lineTo(284f, 0f)
                lineTo(268f, 0f)
                close()
            },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mediumBlue
            }
        )

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(230f, headerBottom)
                lineTo(238f, headerBottom)
                lineTo(298f, 0f)
                lineTo(290f, 0f)
                close()
            },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = lightHeaderBlue
            }
        )

        drawKmiLogo(
            canvas = canvas,
            cx = 78f,
            cy = 58f,
            radius = 42f
        )

        titlePaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        subtitlePaint.textAlign =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        val headerTextX =
            if (isEnglish) 308f else headerTextRight

        canvas.drawText(
            tr(
                "תרגילים לפי נושא",
                "Exercises by Topic"
            ),
            headerTextX,
            50f,
            titlePaint
        )

        canvas.drawText(
            fitText(
                raw = screenTitle,
                paint = subtitlePaint,
                maxWidth = 260f
            ),
            headerTextX,
            77f,
            subtitlePaint
        )

        smallPaint.textAlign = Paint.Align.RIGHT

        canvas.drawText(
            tr("תאריך הפקה:", "Generated:") + " " +
                    SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    ).format(Date()),
            pageWidth - 34f,
            142f,
            smallPaint
        )
    }

    fun drawFooter(
        canvas: android.graphics.Canvas,
        pageNumber: Int,
        totalPages: Int
    ) {
        canvas.drawLine(
            0f,
            footerTop,
            pageWidth.toFloat(),
            footerTop,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                strokeWidth = 2f
            }
        )

        drawKmiLogo(
            canvas = canvas,
            cx = 38f,
            cy = footerTop + 22f,
            radius = 13f
        )

        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            "Together We Protect",
            62f,
            footerTop + 25f,
            smallPaint
        )

        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            tr(
                "עמוד $pageNumber מתוך $totalPages",
                "Page $pageNumber of $totalPages"
            ),
            pageWidth / 2f,
            footerTop + 25f,
            smallPaint
        )

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            "Krav Maga Israel",
            pageWidth - 32f,
            footerTop + 18f,
            smallPaint
        )

        canvas.drawText(
            "www.kmi.org.il",
            pageWidth - 32f,
            footerTop + 31f,
            smallPaint
        )
    }

    val groupedItems = items.groupBy { it.beltId }

    val orderedBeltIds = listOf(
        "yellow",
        "orange",
        "green",
        "blue",
        "brown",
        "black"
    )

    val beltsInPdf = (
            orderedBeltIds +
                    groupedItems.keys.filterNot { it in orderedBeltIds }
            )
        .distinct()
        .filter { groupedItems[it].orEmpty().isNotEmpty() }

    val knownCount = items.count { it.mastered == true }
    val unknownCount = items.count { it.mastered == false }
    val unmarkedCount = items.count { it.mastered == null }
    val favoritesCount = items.count { it.isFavorite }

    data class PdfBlock(
        val beltId: String,
        val item: SubjectExercisePdfItem?,
        val beltItemsCount: Int = 0
    )

    val blocks = buildList {
        beltsInPdf.forEach { beltId ->
            val beltItems = groupedItems[beltId].orEmpty()

            add(
                PdfBlock(
                    beltId = beltId,
                    item = null,
                    beltItemsCount = beltItems.size
                )
            )

            beltItems.forEach { item ->
                add(
                    PdfBlock(
                        beltId = beltId,
                        item = item
                    )
                )
            }
        }
    }

    val summaryHeight = 76f
    val beltHeaderHeight = 34f
    val exerciseRowHeight = 49f
    val blockSpacing = 7f

    fun blockHeight(block: PdfBlock): Float {
        return if (block.item == null) {
            beltHeaderHeight + blockSpacing
        } else {
            exerciseRowHeight + blockSpacing
        }
    }

    fun pageCount(): Int {
        var pages = 1
        var y = contentTop + summaryHeight + 16f

        blocks.forEach { block ->
            val height = blockHeight(block)

            if (y + height > contentBottom) {
                pages++
                y = contentTop
            }

            y += height
        }

        return pages
    }

    val totalPages = pageCount()

    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            pageNumber
        ).create()
    )

    var canvas = page.canvas
    drawHeader(canvas)

    var y = contentTop

    fun finishCurrentPage() {
        drawFooter(
            canvas = canvas,
            pageNumber = pageNumber,
            totalPages = totalPages
        )

        document.finishPage(page)
    }

    fun startNextPage() {
        pageNumber++

        page = document.startPage(
            PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()
        )

        canvas = page.canvas
        drawHeader(canvas)
        y = contentTop
    }

    drawRoundRect(
        canvas = canvas,
        left = pageMargin,
        top = y,
        right = pageWidth - pageMargin,
        bottom = y + summaryHeight,
        color = summaryBackground,
        radius = 14f
    )

    drawRoundRect(
        canvas = canvas,
        left = pageMargin,
        top = y,
        right = pageWidth - pageMargin,
        bottom = y + summaryHeight,
        color = cardBorder,
        radius = 14f,
        stroke = true
    )

    val summaryTitlePaint = newPaint(
        size = 15f,
        color = navy,
        bold = true,
        align = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    )

    val summaryTextPaint = newPaint(
        size = 10.5f,
        color = textDark,
        bold = true,
        align = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    )

    val summaryX =
        if (isEnglish) pageMargin + 18f else pageWidth - pageMargin - 18f

    canvas.drawText(
        tr("סיכום הנושא", "Subject Summary"),
        summaryX,
        y + 25f,
        summaryTitlePaint
    )

    canvas.drawText(
        tr(
            "${items.size} תרגילים · $knownCount יודע · $unknownCount לא יודע",
            "${items.size} exercises · $knownCount known · $unknownCount unknown"
        ),
        summaryX,
        y + 47f,
        summaryTextPaint
    )

    canvas.drawText(
        tr(
            "$unmarkedCount לא סומנו · $favoritesCount מועדפים",
            "$unmarkedCount unmarked · $favoritesCount favorites"
        ),
        summaryX,
        y + 64f,
        summaryTextPaint
    )

    y += summaryHeight + 16f

    blocks.forEachIndexed { blockIndex, block ->
        val requiredHeight = blockHeight(block)

        if (y + requiredHeight > contentBottom) {
            finishCurrentPage()
            startNextPage()
        }

        if (block.item == null) {
            val sampleItem =
                groupedItems[block.beltId].orEmpty().firstOrNull()

            val color = beltColor(block.beltId)

            drawRoundRect(
                canvas = canvas,
                left = pageMargin,
                top = y,
                right = pageWidth - pageMargin,
                bottom = y + beltHeaderHeight,
                color = color,
                radius = 12f
            )

            beltHeaderPaint.textAlign =
                if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

            val titleX =
                if (isEnglish) {
                    pageMargin + 16f
                } else {
                    pageWidth - pageMargin - 16f
                }

            canvas.drawText(
                sampleItem?.let(::beltTitle)
                    ?: block.beltId,
                titleX,
                y + 22f,
                beltHeaderPaint
            )

            val beltCountPaint = newPaint(
                size = 10f,
                color = android.graphics.Color.WHITE,
                bold = true,
                align = if (isEnglish) {
                    Paint.Align.RIGHT
                } else {
                    Paint.Align.LEFT
                }
            )

            val countX =
                if (isEnglish) {
                    pageWidth - pageMargin - 16f
                } else {
                    pageMargin + 16f
                }

            canvas.drawText(
                tr(
                    "${block.beltItemsCount} תרגילים",
                    "${block.beltItemsCount} exercises"
                ),
                countX,
                y + 22f,
                beltCountPaint
            )

            y += beltHeaderHeight + blockSpacing
        } else {
            val item = block.item

            drawRoundRect(
                canvas = canvas,
                left = pageMargin,
                top = y,
                right = pageWidth - pageMargin,
                bottom = y + exerciseRowHeight,
                color = cardBackground,
                radius = 11f
            )

            drawRoundRect(
                canvas = canvas,
                left = pageMargin,
                top = y,
                right = pageWidth - pageMargin,
                bottom = y + exerciseRowHeight,
                color = cardBorder,
                radius = 11f,
                stroke = true
            )

            val accent = beltColor(item.beltId)

            val accentLeft =
                if (isEnglish) {
                    pageMargin
                } else {
                    pageWidth - pageMargin - 4f
                }

            drawRoundRect(
                canvas = canvas,
                left = accentLeft,
                top = y,
                right = accentLeft + 4f,
                bottom = y + exerciseRowHeight,
                color = accent,
                radius = 4f
            )

            val textX =
                if (isEnglish) {
                    pageMargin + 16f
                } else {
                    pageWidth - pageMargin - 16f
                }

            exerciseTitlePaint.textAlign =
                if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

            exerciseMetaPaint.textAlign =
                if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

            canvas.drawText(
                fitText(
                    raw = item.title,
                    paint = exerciseTitlePaint,
                    maxWidth = 360f
                ),
                textX,
                y + 20f,
                exerciseTitlePaint
            )

            val topicText = cleanText(item.topic)
                .takeIf {
                    it.isNotBlank() &&
                            it != "כללי" &&
                            it != "שחרורים"
                }

            val metaParts = buildList {
                topicText?.let { add(it) }
                add(statusTitle(item.mastered))

                if (item.isFavorite) {
                    add(tr("מועדף", "Favorite"))
                }
            }

            canvas.drawText(
                fitText(
                    raw = metaParts.joinToString(" · "),
                    paint = exerciseMetaPaint,
                    maxWidth = 360f
                ),
                textX,
                y + 38f,
                exerciseMetaPaint
            )

            val statusPaint = newPaint(
                size = 9f,
                color = android.graphics.Color.WHITE,
                bold = true,
                align = Paint.Align.CENTER
            )

            val statusCenterX =
                if (isEnglish) {
                    pageWidth - pageMargin - 42f
                } else {
                    pageMargin + 42f
                }

            val statusCenterY = y + exerciseRowHeight / 2f

            canvas.drawCircle(
                statusCenterX,
                statusCenterY,
                13f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = statusColor(item.mastered)
                }
            )

            canvas.drawText(
                when (item.mastered) {
                    true -> "✓"
                    false -> "×"
                    null -> "—"
                },
                statusCenterX,
                statusCenterY + 3.5f,
                statusPaint
            )

            val rowNumberPaint = newPaint(
                size = 8.5f,
                color = textMuted,
                bold = true,
                align = Paint.Align.CENTER
            )

            val rowNumberX =
                if (isEnglish) {
                    pageWidth - pageMargin - 76f
                } else {
                    pageMargin + 76f
                }

            canvas.drawText(
                (blockIndex + 1).toString(),
                rowNumberX,
                statusCenterY + 3f,
                rowNumberPaint
            )

            y += exerciseRowHeight + blockSpacing
        }
    }

    finishCurrentPage()

    val directory = File(
        context.cacheDir,
        "pdfs"
    ).apply {
        mkdirs()
    }

    val safeTitle = cleanText(screenTitle)
        .replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
        .trim('_')
        .take(48)
        .ifBlank { "subject_exercises" }

    val outputFile = File(
        directory,
        "${safeTitle}_${System.currentTimeMillis()}.pdf"
    )

    try {
        FileOutputStream(outputFile).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }

    return outputFile
}

/**
 * מסך: כל התרגילים של נושא חוצה־חגורות.
 *
 * @param subjectId  ה-id מתוך TopicsBySubjectRegistry
 * @param isCoach    מצב מאמן/מתאמן (לרקע)
 * @param onBack     חזרה אחורה
 * @param onExerciseClick  קריאה חיצונית בעת לחיצה (לוגיקת ניווט/סטטיסטיקות נוספת אם תרצה)
 */
@Composable
fun SubjectExercisesScreen(
    subjectId: String,
    isCoach: Boolean,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onExerciseClick: (belt: Belt, topic: String, rawItem: String) -> Unit,
    screenTitle: String = "", // ✅ NEW
    vm: KmiViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // ✅ normalize: לפעמים מגיע subjectId בעברית (תת־נושא) ולא id אמיתי -> לא לקרוס
    val normalizedSubjectId = remember(subjectId, screenTitle) {
        val raw = subjectId.trim()
        val title = screenTitle.trim()

        // אם זה כבר id אמיתי שקיים ברג'יסטרי — נשאיר כמו שהוא
        val exists = runCatching { TopicsBySubjectRegistry.subjectById(raw) != null }.getOrDefault(false)
        if (exists) return@remember raw

        // ✅ fallback ממוקד: "שחרורים" (הבעיה אצלך)
        val combined = "$raw $title"
        val looksLikeReleases =
            combined.contains("שחרור") ||
                    combined.contains("שחרורים") ||
                    combined.contains("תפיס") ||
                    combined.contains("חניק") ||
                    combined.contains("חביק") ||
                    combined.contains("חולצ") ||
                    combined.contains("שיער")

        if (looksLikeReleases) "releases" else raw
    }

    // (subjectId + belts …) נושא מה־app (הישן) ✅
    val appSubjectOrNull: AppSubjectTopic? = remember(normalizedSubjectId) {
        TopicsBySubjectRegistry.subjectById(normalizedSubjectId)
    }

    // ✅ אם עדיין לא נמצא — לא לקרוס, אלא להציג מסך ברור + חזרה
    if (appSubjectOrNull == null) {
        Scaffold(
            topBar = {
                il.kmi.app.ui.KmiTopBar(
                    title = "נושא לא נמצא",
                    onHome = onOpenHome,
                    showTopHome = false,
                    centerTitle = true,
                    lockSearch = true,
                    showBottomActions = false
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "לא נמצא subject עבור:\n${screenTitle.ifBlank { subjectId }}",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onBack) { Text("חזרה") }
                }
            }
        }
        return
    }

    val appSubject: AppSubjectTopic = appSubjectOrNull

    // ✅ NEW: כותרת למסך
    val screenTitleResolved = remember(screenTitle, appSubject.titleHeb) {
        val base = appSubject.titleHeb.trim()
        val picked = screenTitle.trim()

        when {
            picked.isBlank() -> base

            // כבר כותרת מלאה (כוללת את הבסיס)
            picked.startsWith(base) -> picked

            // רק תת־נושא => מוסיפים בסיס
            else -> "$base - $picked"
        }
    }

    // ✅ NEW: מפיקים "תת־בחירה" מתוך הכותרת (כי כרגע לא מעבירים פילטרים בניווט)
    fun detectReleasesPickFromTitle(title: String): String? {
        return when {
            title.contains("חניק") -> "שחרור מחניקות"
            title.contains("תפיס") || title.contains("אחיז") -> "שחרור מתפיסות ידיים"
            title.contains("חביק") || title.contains("חיבוק") -> "שחרור מחביקות גוף"
            title.contains("חולצ") || title.contains("שיער") -> "שחרור חולצה / שיער"
            else -> null
        }
    }

    // ✅ NEW: תת־בחירה ל"עבודת ידיים" (2 קבוצות בלבד)
    fun detectHandsPickFromTitle(title: String): String? {
        return when {
            title.contains("מרפק") -> "מכות מרפק"
            title.contains("מכות יד") || title.contains("ידיים") -> "מכות יד"
            else -> null
        }
    }

    data class ReleasesPickFilter(
        val includeAny: List<String>,
        val requireAll: List<String>,
        val excludeAny: List<String>
    )

    fun releasesFilterForPick(pick: String): ReleasesPickFilter {
        return when (pick) {
            "שחרור מתפיסות ידיים" -> ReleasesPickFilter(
                includeAny = listOf("תפיס", "אחיז", "אוחז"),
                requireAll = listOf("יד"),
                excludeAny = listOf("חניק", "חביק", "חולצ", "שיער", "אקדח", "סכין", "מקל")
            )

            "שחרור מחניקות" -> ReleasesPickFilter(
                includeAny = listOf("חניק", "חניקה", "חניקות", "צואר"),
                requireAll = emptyList(),
                excludeAny = listOf("תפיס", "אחיז", "חביק", "חולצ", "שיער")
            )

            "שחרור מחביקות גוף" -> ReleasesPickFilter(
                includeAny = listOf("חביק", "חיבוק", "חיבוקים", "חביקות"),
                requireAll = emptyList(),
                excludeAny = listOf("חניק", "תפיס", "אחיז", "חולצ", "שיער")
            )

            "שחרור חולצה / שיער" -> ReleasesPickFilter(
                includeAny = listOf("חולצ", "חולצה", "שיער"),
                requireAll = emptyList(),
                excludeAny = listOf("חניק", "חביק", "תפיס", "אחיז")
            )

            else -> ReleasesPickFilter(
                includeAny = listOf(pick),
                requireAll = emptyList(),
                excludeAny = emptyList()
            )
        }
    }

    // ✅ פילטר ל"עבודת ידיים" לפי הבחירה (מכות יד / מכות מרפק)
    fun handsFilterForPick(pick: String): ReleasesPickFilter {
        return when (pick) {
            "מכות מרפק" -> ReleasesPickFilter(
                includeAny = listOf("מרפק"),
                requireAll = emptyList(),
                excludeAny = emptyList()
            )

            "מכות יד" -> ReleasesPickFilter(
                // ✅ תופס גם אגרופים/פיסת יד/מגל/סנוקרת/פטיש/גב יד וכו'
                includeAny = listOf("אגרוף", "פיסת", "מגל", "סנוקרת", "פטיש", "גב יד", "מכת"),
                requireAll = emptyList(),
                // ✅ שלא יתערבבו מרפקים
                excludeAny = listOf("מרפק")
            )

            else -> ReleasesPickFilter(
                includeAny = listOf(pick),
                requireAll = emptyList(),
                excludeAny = emptyList()
            )
        }
    }

    // ✅ NEW: נושא אפקטיבי לתצוגה – עבור releases + hands_all
    val effectiveAppSubject: AppSubjectTopic = remember(appSubject, screenTitleResolved) {

        // ----- releases -----
        if (appSubject.id == "releases") {
            val pick = detectReleasesPickFromTitle(screenTitleResolved) ?: return@remember appSubject
            val f = releasesFilterForPick(pick)

            return@remember appSubject.copy(
                // אפשר להשאיר subTopicHint ריק – אנחנו מסננים ב-include/require/exclude
                subTopicHint = null, // ✅ חשוב: אחרת זה מפיל הכל כי "שחרור מחניקות" לא מופיע בשם התרגיל
                includeItemKeywords = f.includeAny,
                requireAllItemKeywords = f.requireAll,
                excludeItemKeywords = f.excludeAny
            )
        }

        // ----- hands_all -----
        if (appSubject.id == "hands_all") {
            val pick = detectHandsPickFromTitle(screenTitleResolved) ?: return@remember appSubject
            val f = handsFilterForPick(pick)

            return@remember appSubject.copy(
                subTopicHint = null, // ✅ לא לסמוך על subTopicHint כי זה שובר כתומה/ירוקה
                includeItemKeywords = f.includeAny,
                requireAllItemKeywords = f.requireAll,
                excludeItemKeywords = f.excludeAny
            )
        }

        appSubject
    }

    // (belts) המרה מרשימת החגורות ב־app לרשימת חגורות ב־shared ✅
    val ctx = LocalContext.current
    val langManager = remember(ctx) { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == il.kmi.shared.localization.AppLanguage.ENGLISH

// ✅ בודקים את מצב ה-Theme של האפליקציה בפועל
    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val beltsForUi: List<Belt> = remember(effectiveAppSubject) {
        effectiveAppSubject.belts.mapNotNull { appBelt ->
            toSharedBeltOrNull(appBelt.id)
        }.distinct()
    }

    val sharedTopicsByBelt: Map<Belt, List<String>> = remember(effectiveAppSubject) {
        effectiveAppSubject.topicsByBelt.mapNotNull { (appBelt, topics) ->
            val b = toSharedBeltOrNull(appBelt.id)
            if (b == null) {
                null
            } else {
                b to topics
            }
        }.toMap()
    }

    // (shared subject) זה מה שה־SubjectItemsResolver מצפה לקבל ✅
    val sharedSubject: SharedSubjectTopic = remember(effectiveAppSubject, sharedTopicsByBelt) {
        SharedSubjectTopic(
            id = effectiveAppSubject.id,
            titleHeb = effectiveAppSubject.titleHeb,
            topicsByBelt = sharedTopicsByBelt,
            subTopicHint = effectiveAppSubject.subTopicHint,
            includeItemKeywords = effectiveAppSubject.includeItemKeywords.orEmpty(),
            requireAllItemKeywords = effectiveAppSubject.requireAllItemKeywords.orEmpty(),
            excludeItemKeywords = effectiveAppSubject.excludeItemKeywords.orEmpty()
        )
    }

    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    LaunchedEffect(subjectId) {
        KmiTtsManager.init(ctx)
        KmiTtsManager.setSpeechProfile(rate = 0.95f, pitch = 1.0f)
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FBFF),
                Color(0xFFEAF4FF),
                Color(0xFFB7DDF7),
                Color(0xFF1F78B4),
                Color(0xFF062B4A)
            )
        )
    }

            // נתוני שורה: belt + topic + rawItem (לניווט) + displayName + canonicalId
            data class RowData(
        val belt: Belt,
        val topic: String,
        val rawItem: String,
        val displayItem: String,
        val canonicalId: String
    )

    // shared resolver: מקור אמת ✅
    val rows: List<RowData> = remember(subjectId, beltsForUi, sharedSubject, effectiveAppSubject, screenTitleResolved) {

        // ✅ SPECIAL-CASE: releases מגיע *רק* מ-HardSectionsCatalog (לא מ-ContentRepo)
        if (effectiveAppSubject.id == "releases") {

            // בוחרים איזה Section להציג לפי הכותרת (כמו במסך הבחירה)
            val wantedSectionTitle: String? = run {
                val t = screenTitleResolved

                when {
                    t.contains("חניק") -> "שחרור מחניקות"
                    t.contains("חביק") || t.contains("חיבוק") -> "שחרור מחביקות גוף"

                    // ✅ זה הסקשן הראשון, והוא כולל גם ידיים וגם שיער/חולצה
                    t.contains("תפיס") || t.contains("אחיז") || t.contains("שיער") || t.contains("חולצ") ->
                        "שחרור מתפיסות ידיים / שיער / חולצה"

                    else -> null
                }
            }

            val sectionsToUse = HardSectionsCatalog.releases
                .filter { sec -> wantedSectionTitle == null || sec.title.trim() == wantedSectionTitle }

            val order = HardSectionsCatalog.beltOrder

            order.flatMap { belt ->
                val itemsForBelt = sectionsToUse
                    .flatMap { sec -> sec.itemsFor(belt) }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                itemsForBelt.map { raw ->
                    RowData(
                        belt = belt,
                        topic = "שחרורים",
                        rawItem = raw,
                        displayItem = stripSubjectPrefix(
                            subjectTitle = effectiveAppSubject.titleHeb,
                            itemTitle = raw
                        ),
                        canonicalId = "releases::${belt.id}::${wantedSectionTitle ?: "ALL"}::${raw.trim()}"
                    )
                }
            }
        } else {

            // ✅ כל שאר הנושאים נשארים דרך resolver (ContentRepo)
            beltsForUi
                .flatMap { belt ->
                    val sections = SubjectItemsResolver.resolveBySubject(
                        belt = belt,
                        subject = sharedSubject
                    )

                    sections.flatMap { section ->
                        val sectionTitle = section.title // יכול להיות null

                        // ✅ הסרה מוחלטת של section "כללי"
                        if (sectionTitle?.trim() == "כללי") return@flatMap emptyList()

                        // ✅ NEW: אכיפה של include/require/exclude/subTopicHint על כל פריט
                        val filteredItems = section.items.filter { ui ->
                            val rawTitle = buildString {
                                append(ui.canonicalId)
                                append("::")
                                append(ui.rawItem)
                                append("::")
                                append(ui.displayName)
                            }

                            TopicsBySubjectRegistry.run {
                                effectiveAppSubject.matchesItem(
                                    itemTitle = rawTitle,
                                    subTopicTitle = sectionTitle
                                )
                            }
                        }

                        filteredItems.map { ui ->
                            val cleanedDisplay = stripSubjectPrefix(
                                subjectTitle = effectiveAppSubject.titleHeb,
                                itemTitle = ui.displayName
                            )

                            RowData(
                                belt = belt,
                                topic = ui.topicTitle,
                                rawItem = ui.rawItem,
                                displayItem = cleanedDisplay,
                                canonicalId = ui.canonicalId
                            )
                        }
                    }
                }
                .filterNot { it.topic.trim() == "כללי" }
        }
    }

    // ----------------- ⭐ Favorites + 🕒 Recents -----------------
    val prefs = remember(ctx) {
        ctx.getSharedPreferences("kmi_subject_exercises", android.content.Context.MODE_PRIVATE)
    }

    val SEP = "\u001F" // unit-separator (נדיר בטקסט)

    val KEY_RECENTS = remember(subjectId) { "recent_ids_str__subject__$subjectId" }

    fun loadRecents(): List<String> {
        val raw = prefs.getString(KEY_RECENTS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP).asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    fun saveRecents(list: List<String>) {
        prefs.edit().putString(KEY_RECENTS, list.joinToString(SEP)).apply()
    }

    val marksVersion by vm.marksVersion.collectAsState()
    val subjectItemStates = remember(subjectId) { mutableStateMapOf<String, Boolean?>() }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun subjectStatusIdFor(row: RowData): String {
        val cleanTitle = normalizeStatusPart(row.rawItem)

        val resolved = ExerciseIdentityRegistry.resolve(
            belt = row.belt,
            hebrewTitle = cleanTitle,
            topicKey = row.topic
        )

        if (resolved.isKnown) {
            return resolved.id
        }

        // fallback בטוח בלבד — אחרי ה-audit אמור כמעט לא לקרות
        return resolved.id
    }

    fun subjectLegacyStatusIdFor(index: Int, row: RowData): String {
        val cleanItem = normalizeStatusPart(row.rawItem)
        val cleanTopic = normalizeStatusPart(row.topic).ifBlank { "כללי" }

        return "status_${row.belt.id}_${cleanTopic}_${index}_${cleanItem}"
    }

    fun subjectStatusKeysFor(row: RowData): List<String> {
        val statusId = subjectStatusIdFor(row)

        val identityKeys = ExerciseIdentityRegistry
            .allKnown()
            .firstOrNull { it.id == statusId && it.belt == row.belt }
            ?.topicKeys
            .orEmpty()

        return (
                identityKeys +
                        row.topic +
                        "כללי"
                )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun setSubjectLocalStatus(
        row: RowData,
        statusId: String,
        value: Boolean?
    ) {
        subjectStatusKeysFor(row).forEach { key ->
            val masteredKey = "mastered_${row.belt.id}_${key}"
            val unknownKey = "unknown_${row.belt.id}_${key}"

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

            prefs.edit()
                .putStringSet(masteredKey, masteredSet)
                .putStringSet(unknownKey, unknownSet)
                .apply()
        }
    }

    LaunchedEffect(rows, marksVersion) {
        rows.forEachIndexed { index, row ->
            val statusId = subjectStatusIdFor(row)
            val legacyStatusId = subjectLegacyStatusIdFor(index, row)

            var valueFromVm: Boolean? = null

            for (key in subjectStatusKeysFor(row)) {
                val valueFromKey: Boolean? =
                    runCatching {
                        vm.getItemStatusNullable(
                            belt = row.belt,
                            topic = key,
                            item = statusId
                        )
                    }.getOrNull()
                        ?: runCatching {
                            if (
                                vm.isMastered(
                                    belt = row.belt,
                                    topic = key,
                                    item = statusId
                                )
                            ) true else null
                        }.getOrNull()
                        ?: runCatching {
                            vm.getItemStatusNullable(
                                belt = row.belt,
                                topic = key,
                                item = legacyStatusId
                            )
                        }.getOrNull()
                        ?: runCatching {
                            if (
                                vm.isMastered(
                                    belt = row.belt,
                                    topic = key,
                                    item = legacyStatusId
                                )
                            ) true else null
                        }.getOrNull()

                if (valueFromKey != null) {
                    valueFromVm = valueFromKey
                    break
                }
            }

            if (valueFromVm == null) {
                for (key in subjectStatusKeysFor(row)) {
                    val masteredKey = "mastered_${row.belt.id}_${key}"
                    val unknownKey = "unknown_${row.belt.id}_${key}"

                    val masteredSet = prefs.getStringSet(masteredKey, emptySet<String>()) ?: emptySet()
                    val unknownSet = prefs.getStringSet(unknownKey, emptySet<String>()) ?: emptySet()

                    val localValue: Boolean? = when {
                        masteredSet.contains(statusId) || masteredSet.contains(legacyStatusId) -> true
                        unknownSet.contains(statusId) || unknownSet.contains(legacyStatusId) -> false
                        else -> null
                    }

                    if (localValue != null) {
                        valueFromVm = localValue

                        // ריפוי VM לפי statusId החדש
                        vm.setItemStatusNullable(
                            belt = row.belt,
                            topic = key,
                            item = statusId,
                            value = localValue
                        )

                        break
                    }
                }
            }

            subjectItemStates[statusId] = valueFromVm
        }
    }

    val favIds: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    var recentIds by remember(subjectId) { mutableStateOf(loadRecents()) }
    var filterMode by remember { mutableStateOf(FilterMode.ALL) }

    // ✅ ספירה נכונה לצ’יפ "מועדפים": רק מועדפים של הנושא הנוכחי
    val favoritesCountForThisSubject = remember(rows, favIds) {
        rows.count { it.canonicalId in favIds }
    }

    // ✅ מפה מהירה לפי canonicalId (עוזר לנו לסדר Recents לפי זמן)
    val rowById: Map<String, RowData> = remember(rows) {
        rows.associateBy { it.canonicalId }
    }

    // ✅ filteredRows *מחוץ* ל-LazyColumn
    val filteredRows: List<RowData> = remember(rows, favIds, recentIds, filterMode, rowById) {
        when (filterMode) {
            FilterMode.ALL -> rows
            FilterMode.FAVORITES -> rows.filter { it.canonicalId in favIds }
            FilterMode.RECENTS -> recentIds.mapNotNull { rowById[it] } // לפי זמן (אחרון ראשון)
        }
    }

    val subjectTotalCount = rows.size

    val subjectKnownCount = rows.count { row ->
        subjectItemStates[subjectStatusIdFor(row)] == true
    }

    val subjectUnknownCount = rows.count { row ->
        subjectItemStates[subjectStatusIdFor(row)] == false
    }

    val subjectUnmarkedCount = rows.count { row ->
        subjectItemStates[subjectStatusIdFor(row)] == null
    }

    val subjectFavoriteCount = rows.count { row ->
        row.canonicalId in favIds
    }

    // מצב: איזה תרגיל נבחר להצגת דיאלוג הסבר
    var selectedRow by remember { mutableStateOf<RowData?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundBrush) // שומר על אותו רקע מאחורי הכל
            ) {
                il.kmi.app.ui.KmiTopBar(
                    title = screenTitleResolved,
                    onHome = onOpenHome,
                    showTopHome = false,
                    centerTitle = true,
                    lockSearch = false,
                    showBottomActions = true,
                    onShare = {
                        val pdfItems = rows.map { row ->
                            SubjectExercisePdfItem(
                                beltId = row.belt.id,
                                beltHeb = row.belt.heb,
                                beltEn = row.belt.en,
                                topic = row.topic,
                                title = row.displayItem,
                                mastered = subjectItemStates[
                                    subjectStatusIdFor(row)
                                ],
                                isFavorite = row.canonicalId in favIds
                            )
                        }

                        shareSubjectExercisesPdf(
                            context = ctx,
                            screenTitle = screenTitleResolved,
                            items = pdfItems,
                            isEnglish = isEnglish
                        )
                    }
                )

                // ✅ הפילטרים + סטטיסטיקה בתוך ה-topBar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundBrush)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    TopFiltersBarModern(
                        filterMode = filterMode,
                        favCount = favoritesCountForThisSubject,
                        recentCount = recentIds.size,
                        onPick = { filterMode = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (isEnglish) {
                            "← Swipe sideways to see more stats →"
                        } else {
                            "→→ הזז לצד כדי לראות עוד נתונים →→"
                        },
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubjectTopStatChip(
                            value = subjectTotalCount.toString(),
                            label = if (isEnglish) "Exercises" else "תרגילים",
                            containerColor = Color(0xFF98A2B3)
                        )

                        SubjectTopStatChip(
                            value = subjectKnownCount.toString(),
                            label = if (isEnglish) "Known" else "יודע",
                            containerColor = Color(0xFF7ACB88)
                        )

                        SubjectTopStatChip(
                            value = subjectUnknownCount.toString(),
                            label = if (isEnglish) "Unknown" else "לא יודע",
                            containerColor = Color(0xFFF1A97A)
                        )

                        SubjectTopStatChip(
                            value = subjectFavoriteCount.toString(),
                            label = if (isEnglish) "Favorites" else "מועדפים",
                            containerColor = Color(0xFFE7A3B5)
                        )

                        SubjectTopStatChip(
                            value = subjectUnmarkedCount.toString(),
                            label = if (isEnglish) "Unmarked" else "לא סומן",
                            containerColor = Color(0xFF8596C9)
                        )

                        SubjectTopStatChip(
                            value = recentIds.size.toString(),
                            label = if (isEnglish) "Recent" else "אחרונים",
                            containerColor = Color(0xFF95D69A)
                        )
                    }
                }
            }
        }
    ) { padding ->

    // ✅ התוכן מתחיל אחרי כל ה-topBar (כולל הפילטרים)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundBrush)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = if (isDarkMode) {
                    Color(0xFF0F172A).copy(alpha = 0.96f)
                } else {
                    Color.White.copy(alpha = 0.97f)
                },
                tonalElevation = if (isDarkMode) 0.dp else 4.dp,
                shadowElevation = if (isDarkMode) 0.dp else 10.dp,
                border = if (isDarkMode) {
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                } else {
                    null
                }
            ) {
                // ... כל הקוד שלך של rows.isEmpty / LazyColumn נשאר אותו דבר
                if (rows.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "לא נמצאו תרגילים לנושא זה.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.86f) else Color(0xFF546E7A),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    if (filterMode == FilterMode.RECENTS) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = filteredRows,
                                key = { index, row -> "recent_${index}_${row.canonicalId}" }
                            ) { index, row ->
                                val statusId = subjectStatusIdFor(row)
                                val mastered = subjectItemStates[statusId]

                                ExerciseRowCardModern(
                                    exerciseNumber = index + 1,
                                    belt = row.belt,
                                    topic = row.topic,
                                    item = row.displayItem,
                                    mastered = mastered,
                                    isFavorite = row.canonicalId in favIds,
                                    isEnglish = isEnglish,
                                    isDarkMode = isDarkMode,
                                    showMeta = (effectiveAppSubject.id != "releases"),
                                    onStatusClick = {
                                        val nextValue = when (subjectItemStates[statusId]) {
                                            null -> true
                                            true -> false
                                            false -> null
                                        }

                                        subjectItemStates[statusId] = nextValue

                                        subjectStatusKeysFor(row).forEach { key ->
                                            vm.setItemStatusNullable(
                                                belt = row.belt,
                                                topic = key,
                                                item = statusId,
                                                value = nextValue
                                            )
                                        }

                                        setSubjectLocalStatus(
                                            row = row,
                                            statusId = statusId,
                                            value = nextValue
                                        )
                                    },
                                    onToggleFavorite = {
                                        FavoritesStore.toggle(row.canonicalId)
                                    },
                                    onInfoClick = {
                                        clickSound()
                                        haptic(true)

                                        val nextRecents = buildList {
                                            add(row.canonicalId)
                                            addAll(recentIds.filterNot { it == row.canonicalId })
                                        }.take(50)

                                        recentIds = nextRecents
                                        saveRecents(nextRecents)

                                        selectedRow = row
                                    }
                                )
                            }
                        }
                    } else {
                        val grouped = filteredRows.groupBy { it.belt }
                        val beltsToShow = beltsForUi.filter { grouped[it].orEmpty().isNotEmpty() }

                        val flatRows = beltsToShow.flatMap { belt ->
                            grouped[belt].orEmpty().mapIndexed { rowIndex, row ->
                                Triple(belt, rowIndex, row)
                            }
                        }

                        val listState = rememberLazyListState()

                        val currentStickyBelt by remember(flatRows, listState) {
                            derivedStateOf {
                                flatRows
                                    .getOrNull(listState.firstVisibleItemIndex)
                                    ?.first
                                    ?: beltsToShow.firstOrNull()
                                    ?: Belt.YELLOW
                            }
                        }

                        val currentStickyRows = grouped[currentStickyBelt].orEmpty()

                        val currentKnownCount = currentStickyRows.count { row ->
                            subjectItemStates[subjectStatusIdFor(row)] == true
                        }

                        val currentUnknownCount = currentStickyRows.count { row ->
                            subjectItemStates[subjectStatusIdFor(row)] == false
                        }

                        val currentFavoriteCount = currentStickyRows.count { row ->
                            row.canonicalId in favIds
                        }

                        val currentUnmarkedCount = currentStickyRows.count { row ->
                            subjectItemStates[subjectStatusIdFor(row)] == null
                        }

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            BeltStickyHeaderModern(
                                belt = currentStickyBelt,
                                count = currentStickyRows.size,
                                knownCount = currentKnownCount,
                                unknownCount = currentUnknownCount,
                                favoriteCount = currentFavoriteCount,
                                unmarkedCount = currentUnmarkedCount,
                                isEnglish = isEnglish,
                                isDarkMode = isDarkMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            )

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(
                                    start = 10.dp,
                                    end = 10.dp,
                                    bottom = 10.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(
                                    items = flatRows,
                                    key = { index, triple ->
                                        val belt = triple.first
                                        val rowIndex = triple.second
                                        val row = triple.third
                                        "fixed_belt_${belt.id}_${rowIndex}_${row.canonicalId}_$index"
                                    }
                                ) { _, triple ->
                                    val belt = triple.first
                                    val rowIndex = triple.second
                                    val row = triple.third

                                    val statusId = subjectStatusIdFor(row)
                                    val mastered = subjectItemStates[statusId]

                                    ExerciseRowCardModern(
                                        exerciseNumber = rowIndex + 1,
                                        belt = belt,
                                        topic = row.topic,
                                        item = row.displayItem,
                                        mastered = mastered,
                                        isFavorite = row.canonicalId in favIds,
                                        isEnglish = isEnglish,
                                        isDarkMode = isDarkMode,
                                        showMeta = (effectiveAppSubject.id != "releases"),
                                        onStatusClick = {
                                            val nextValue = when (subjectItemStates[statusId]) {
                                                null -> true
                                                true -> false
                                                false -> null
                                            }

                                            subjectItemStates[statusId] = nextValue

                                            subjectStatusKeysFor(row).forEach { key ->
                                                vm.setItemStatusNullable(
                                                    belt = row.belt,
                                                    topic = key,
                                                    item = statusId,
                                                    value = nextValue
                                                )
                                            }

                                            setSubjectLocalStatus(
                                                row = row,
                                                statusId = statusId,
                                                value = nextValue
                                            )
                                        },
                                        onToggleFavorite = {
                                            FavoritesStore.toggle(row.canonicalId)
                                        },
                                        onInfoClick = {
                                            clickSound()
                                            haptic(true)

                                            val nextRecents = buildList {
                                                add(row.canonicalId)
                                                addAll(recentIds.filterNot { it == row.canonicalId })
                                            }.take(50)

                                            recentIds = nextRecents
                                            saveRecents(nextRecents)

                                            selectedRow = row
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

            // ✅ דיאלוג נשאר אותו דבר (הוא מתחת ל-Column, אין שינוי)
            selectedRow?.let { row ->
    val isFavorite = row.canonicalId in favIds

                val explanation = remember(row.canonicalId, row.belt, row.topic, row.rawItem, isEnglish) {
                    val resolved = ExerciseExplanationResolver.get(
                        belt = row.belt,
                        topic = row.topic,
                        item = row.rawItem,
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
                        if (isEnglish) {
                            "There is currently no explanation for this exercise."
                        } else {
                            "אין כרגע הסבר לתרגיל הזה."
                        }
                    }
                }

                AlertDialog(
                    onDismissRequest = { selectedRow = null },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = row.displayItem,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start
                                )
                            }

                            Spacer(Modifier.width(4.dp))

                            val context = LocalContext.current

                            IconButton(
                                onClick = {
                                    KmiTtsManager.init(context)
                                    KmiTtsManager.stop()
                                    KmiTtsManager.speak(explanation)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "השמע הסבר קולי",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    val wasFavorite = row.canonicalId in favIds
                                    FavoritesStore.toggle(row.canonicalId)

                                    // אם אנחנו בפילטר "מועדפים" והסרנו מועדף — נסגור דיאלוג
                                    if (filterMode == FilterMode.FAVORITES && wasFavorite) {
                                        selectedRow = null
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) {
                                        Color(0xFFFFC107)
                                    } else {
                                        if (isDarkMode) Color.White.copy(alpha = 0.72f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    }
                                )
                            }
                        }
                    },
                    text = {
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedRow = null }) {
                            Text("סגור")
                            }
                        }
                     )
                 }
            }
        }
    }
}

@Composable
private fun SubjectTopStatChip(
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
                color = contentColor,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Text(
                text = label,
                color = contentColor.copy(alpha = 0.92f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SubjectExerciseMetaBadge(
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
            color = contentColor,
            fontSize = 9.sp,
            lineHeight = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun TopFiltersBarModern(
    filterMode: FilterMode,
    favCount: Int,
    recentCount: Int,
    onPick: (FilterMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0x14000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            FilterChipModern(
                text = "הכל",
                selected = filterMode == FilterMode.ALL,
                onClick = { onPick(FilterMode.ALL) },
                modifier = Modifier.weight(1f)
            )

            FilterChipModern(
                text = "מועדפים ($favCount)\n⭐",
                selected = filterMode == FilterMode.FAVORITES,
                onClick = { onPick(FilterMode.FAVORITES) },
                modifier = Modifier.weight(1f)
            )

            FilterChipModern(
                text = "אחרונים ($recentCount)\n🕒",
                selected = filterMode == FilterMode.RECENTS,
                onClick = { onPick(FilterMode.RECENTS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun stripSubjectPrefix(subjectTitle: String, itemTitle: String): String {
    val subj = subjectTitle.trim()
    var s = itemTitle.trim()

    // 1) אם באמת מתחיל בשם הנושא (למשל "בעיטות - ...")
    if (subj.isNotBlank() && s.startsWith(subj)) {
        s = s.removePrefix(subj).trim()
        s = s.trimStart('-', '–', '—', ':').trim()
        return s
    }

    // ✅ לשחרורים: אם מופיע "שחרורים" (רבים) בתחילת טקסט – ננקה רק אותו.
    // ⚠️ לא מנקים "שחרור" (יחיד) כי זה חלק מהשם של התרגיל.
    if (s.startsWith("שחרורים")) {
        s = s.removePrefix("שחרורים").trim()
        s = s.trimStart('-', '–', '—', ':').trim()
        return s
    }

    return s
}

/** ✅ זה מה שחסר לך ולכן יש Unresolved reference */
@Composable
private fun FilterChipModern(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant

    val border =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else Color(0x16000000)

    Surface(
        modifier = modifier
            .height(56.dp), // ✅ גובה קבוע – מונע "התמתחות" מטורפת ב-topBar
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth() // ✅ במקום fillMaxSize() שגרם לכפתור למלא גובה ענק
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ───────── כותרת חגורה בתוך המסך ───────── */

@Composable
private fun BeltHeaderRow(
    belt: Belt,
    count: Int
) {
    val onBelt = if (belt.color.luminance() < 0.5f) Color.White else Color.Black

    val cleanName = remember(belt.heb) {
        val s = belt.heb.trim()
        if (s.startsWith("חגורה")) s.removePrefix("חגורה").trim() else s
    }

    val titleText = "חגורה $cleanName - $count תרגילים"

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = belt.color,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = titleText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 14.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = onBelt
        )
    }
}

@Composable
private fun BeltStickyHeaderModern(
    belt: Belt,
    count: Int,
    knownCount: Int,
    unknownCount: Int,
    favoriteCount: Int,
    unmarkedCount: Int,
    isEnglish: Boolean,
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val onBelt = if (belt.color.luminance() < 0.5f) Color.White else Color.Black

    val cleanName = remember(belt.heb) {
        val s = belt.heb.trim()
        if (s.startsWith("חגורה")) s.removePrefix("חגורה").trim() else s
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 0.dp),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = if (isDarkMode) 0.dp else 2.dp,
        shadowElevation = if (isDarkMode) 0.dp else 8.dp,
        color = if (isDarkMode) Color(0xFF111827) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0x12000000)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = belt.color
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) beltTitleEnglishForSticky(belt) else "חגורה $cleanName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onBelt,
                        modifier = Modifier.weight(1f),
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
                    )

                    Text(
                        text = if (isEnglish) {
                            if (count == 1) "1 exercise" else "$count exercises"
                        } else {
                            "$count תרגילים"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = onBelt.copy(alpha = 0.95f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (isEnglish) {
                        "← Swipe sideways to see more stats →"
                    } else {
                        "→→ הזז לצד כדי לראות עוד נתונים →→"
                    },
                    color = if (isDarkMode) {
                        Color.White.copy(alpha = 0.78f)
                    } else {
                        Color(0xFF5B6472)
                    },
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubjectTopStatChip(
                        value = count.toString(),
                        label = if (isEnglish) "Exercises" else "תרגילים",
                        containerColor = Color(0xFF98A2B3)
                    )

                    SubjectTopStatChip(
                        value = knownCount.toString(),
                        label = if (isEnglish) "Known" else "יודע",
                        containerColor = Color(0xFF7ACB88)
                    )

                    SubjectTopStatChip(
                        value = unknownCount.toString(),
                        label = if (isEnglish) "Unknown" else "לא יודע",
                        containerColor = Color(0xFFF1A97A)
                    )

                    SubjectTopStatChip(
                        value = favoriteCount.toString(),
                        label = if (isEnglish) "Favorites" else "מועדפים",
                        containerColor = Color(0xFFE7A3B5)
                    )

                    SubjectTopStatChip(
                        value = unmarkedCount.toString(),
                        label = if (isEnglish) "Unmarked" else "לא סומן",
                        containerColor = Color(0xFF8596C9)
                    )
                }
            }
        }
    }
}

private fun beltTitleEnglishForSticky(belt: Belt): String {
    return when (belt) {
        Belt.YELLOW -> "Yellow Belt"
        Belt.ORANGE -> "Orange Belt"
        Belt.GREEN -> "Green Belt"
        Belt.BLUE -> "Blue Belt"
        Belt.BROWN -> "Brown Belt"
        Belt.BLACK -> "Black Belt"
        else -> belt.name
    }
}

@Composable
private fun BeltSectionCardModern(
    belt: Belt,
    count: Int,
    isDarkMode: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {    val onBelt = if (belt.color.luminance() < 0.5f) Color.White else Color.Black

    val cleanName = remember(belt.heb) {
        val s = belt.heb.trim()
        if (s.startsWith("חגורה")) s.removePrefix("חגורה").trim() else s
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = if (isDarkMode) 0.dp else 2.dp,
        shadowElevation = if (isDarkMode) 0.dp else 6.dp,
        color = if (isDarkMode) Color(0xFF111827) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0x12000000)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header צבעוני של החגורה
            Surface(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = belt.color
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "חגורה $cleanName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onBelt,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "$count תרגילים",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = onBelt.copy(alpha = 0.95f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

/* ───────── שורת תרגיל ───────── */

@Composable
private fun ExerciseRowCardModern(
    exerciseNumber: Int,
    belt: Belt,
    topic: String,
    item: String,
    mastered: Boolean?,
    isFavorite: Boolean,
    isEnglish: Boolean,
    isDarkMode: Boolean = false,
    showMeta: Boolean = false,
    onStatusClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInfoClick: () -> Unit
) {
    val rowBgColor = if (isDarkMode) {
        Color(0xFF1E293B)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val rowTextColor = if (isDarkMode) {
        Color(0xFFF8FAFC)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val rowMetaColor = if (isDarkMode) {
        Color(0xFFCBD5E1)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = rowBgColor,
        tonalElevation = if (isDarkMode) 0.dp else 1.dp,
        border = BorderStroke(
            1.dp,
            if (isDarkMode) belt.color.copy(alpha = 0.55f) else belt.color.copy(alpha = 0.35f)
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.scale(0.82f),
                    contentAlignment = Alignment.Center
                ) {
                    SubjectMasterToggle(
                        mastered = mastered,
                        onClick = onStatusClick
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onInfoClick() }
                        .padding(vertical = 1.dp),
                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                ) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEnglish) {
                                SubjectExerciseMetaBadge(
                                    text = "No. $exerciseNumber",
                                    containerColor = belt.color.copy(alpha = 0.14f),
                                    contentColor = Color(0xFF1F2937)
                                )

                                if (isFavorite) {
                                    Spacer(Modifier.width(5.dp))
                                    SubjectExerciseMetaBadge(
                                        text = "Favorite",
                                        containerColor = Color(0xFFF9D9B8),
                                        contentColor = Color(0xFF9A5A00)
                                    )
                                }

                                Spacer(Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))

                                if (isFavorite) {
                                    SubjectExerciseMetaBadge(
                                        text = "מועדף",
                                        containerColor = Color(0xFFF9D9B8),
                                        contentColor = Color(0xFF9A5A00)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                }

                                SubjectExerciseMetaBadge(
                                    text = "מס׳ $exerciseNumber",
                                    containerColor = belt.color.copy(alpha = 0.14f),
                                    contentColor = Color(0xFF1F2937)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 13.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = rowTextColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showMeta) {
                        val meta = topic.trim()
                        if (meta.isNotBlank() && meta != "כללי" && meta != "שחרורים") {
                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.5.sp,
                                    lineHeight = 11.sp
                                ),
                                color = rowMetaColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = if (isEnglish) "Exercise information" else "מידע על התרגיל",
                        tint = if (isDarkMode) {
                            Color.White.copy(alpha = 0.78f)
                        } else {
                            Color(0xFF607D8B)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(3.dp))

                val scale by animateFloatAsState(
                    targetValue = if (isFavorite) 1.12f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "favoriteScale"
                )

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "הסר ממועדפים" else "הוסף למועדפים",
                        tint = if (isFavorite) {
                            Color(0xFFFFC107)
                        } else {
                            if (isDarkMode) Color.White.copy(alpha = 0.62f) else Color(0xFF9CA3AF)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(3.dp))

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(belt.color.copy(alpha = 0.9f))
                )
            }
        }
    }
}


@Composable
private fun SubjectMasterToggle(
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

    val iconTint = when (mastered) {
        true, false -> Color.White
        null -> Color.Transparent
    }

    Surface(
        modifier = Modifier
            .size(38.dp)
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
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )

                false -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "לא יודע",
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )

                null -> Spacer(Modifier.size(1.dp))
            }
        }
    }
}
