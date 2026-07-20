package il.kmi.app.screens.coach

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

internal fun createCoachTraineesPdf(
    context: Context,
    profiles: List<TraineeProfile>,
    stats: GroupStatsUi,
    branch: String,
    groupKey: String,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36f

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val document = PdfDocument()

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textSize = 11.5f
        color = android.graphics.Color.rgb(15, 23, 42)
        textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    }

    val titlePaint = Paint(textPaint).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 22f
        color = android.graphics.Color.WHITE
    }

    val subtitlePaint = Paint(textPaint).apply {
        textSize = 10.5f
        color = android.graphics.Color.rgb(226, 232, 240)
    }

    val sectionPaint = Paint(textPaint).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 15f
        color = android.graphics.Color.rgb(15, 23, 42)
    }

    val headerPaint = Paint(textPaint).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 10.5f
        color = android.graphics.Color.WHITE
    }

    val smallPaint = Paint(textPaint).apply {
        textSize = 9.5f
        color = android.graphics.Color.rgb(100, 116, 139)
    }

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(226, 232, 240)
        strokeWidth = 1f
    }

    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    )
    var canvas = page.canvas
    var y = margin

    fun xStart(): Float = margin
    fun xEnd(): Float = pageWidth - margin
    fun textXStart(): Float = if (isEnglish) xStart() else xEnd()
    fun textXEnd(): Float = if (isEnglish) xEnd() else xStart()

    fun beltPdfColor(belt: String): Int {
        val normalized = belt.trim()
        return when {
            normalized.contains("לבנ") || normalized.contains("White", true) ->
                android.graphics.Color.rgb(229, 231, 235)
            normalized.contains("צהוב") || normalized.contains("Yellow", true) ->
                android.graphics.Color.rgb(250, 204, 21)
            normalized.contains("כתומ") || normalized.contains("Orange", true) ->
                android.graphics.Color.rgb(249, 115, 22)
            normalized.contains("ירוק") || normalized.contains("Green", true) ->
                android.graphics.Color.rgb(34, 197, 94)
            normalized.contains("כחול") || normalized.contains("Blue", true) ->
                android.graphics.Color.rgb(59, 130, 246)
            normalized.contains("חומ") || normalized.contains("Brown", true) ->
                android.graphics.Color.rgb(139, 90, 43)
            normalized.contains("שחור") || normalized.contains("Black", true) ->
                android.graphics.Color.rgb(17, 17, 17)
            else ->
                android.graphics.Color.rgb(124, 58, 237)
        }
    }

    fun drawHeader() {
        canvas.drawColor(android.graphics.Color.WHITE)

        val navy = android.graphics.Color.rgb(2, 43, 74)
        val mediumBlue = android.graphics.Color.rgb(36, 103, 158)
        val lightBlue = android.graphics.Color.rgb(128, 183, 220)
        val mutedText = android.graphics.Color.rgb(100, 116, 139)

        val headerBottom = 122f

        val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            style = Paint.Style.FILL
        }

        val mediumStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mediumBlue
            style = Paint.Style.FILL
        }

        val lightStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lightBlue
            style = Paint.Style.FILL
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

        val logoOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            style = Paint.Style.FILL
        }

        val logoInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }

        val logoTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            typeface = Typeface.create(
                Typeface.SANS_SERIF,
                Typeface.BOLD
            )
            textSize = logoRadius * 0.62f
            textAlign = Paint.Align.CENTER
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

        titlePaint.apply {
            typeface = Typeface.create(
                Typeface.SANS_SERIF,
                Typeface.BOLD
            )
            textSize = 25f
            color = android.graphics.Color.WHITE
            textAlign = if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }
        }

        subtitlePaint.apply {
            typeface = Typeface.create(
                Typeface.SANS_SERIF,
                Typeface.NORMAL
            )
            textSize = 12f
            color = android.graphics.Color.WHITE
            textAlign = if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }
        }

        val headerTextX = if (isEnglish) {
            308f
        } else {
            pageWidth - 34f
        }

        canvas.drawText(
            tr(
                "דו״ח רשימת מתאמנים",
                "Trainees List Report"
            ),
            headerTextX,
            50f,
            titlePaint
        )

        canvas.drawText(
            "${tr("סניף", "Branch")}: ${branch.ifBlank { "—" }} · " +
                    "${tr("קבוצה", "Group")}: ${groupKey.ifBlank { "—" }}",
            headerTextX,
            77f,
            subtitlePaint
        )

        val generatedDate = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        smallPaint.apply {
            color = mutedText
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText(
            tr(
                "תאריך הפקה: $generatedDate",
                "Generated: $generatedDate"
            ),
            pageWidth - 34f,
            142f,
            smallPaint
        )

        y = 174f
    }

    fun drawFooter() {
        smallPaint.color = android.graphics.Color.rgb(100, 116, 139)
        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawLine(margin, pageHeight - 42f, pageWidth - margin, pageHeight - 42f, linePaint)
        canvas.drawText(
            tr("עמוד $pageNumber · KAMI", "Page $pageNumber · KAMI"),
            pageWidth / 2f,
            pageHeight - 24f,
            smallPaint
        )
    }

    fun newPage() {
        drawFooter()
        document.finishPage(page)
        pageNumber++
        page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        canvas = page.canvas
        y = margin
        drawHeader()
    }

    fun ensureSpace(height: Float) {
        if (y + height > pageHeight - 58f) {
            newPage()
        }
    }

    fun drawSummaryTile(
        index: Int,
        label: String,
        value: String
    ) {
        val gap = 8f
        val tileWidth = ((pageWidth - margin * 2f) - gap * 2f) / 3f
        val left = if (isEnglish) {
            margin + index * (tileWidth + gap)
        } else {
            pageWidth - margin - tileWidth - index * (tileWidth + gap)
        }

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(248, 251, 255)
        }

        canvas.drawRoundRect(
            left,
            y,
            left + tileWidth,
            y + 58f,
            14f,
            14f,
            bg
        )

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(214, 226, 241)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawRoundRect(
            left,
            y,
            left + tileWidth,
            y + 58f,
            14f,
            14f,
            border
        )

        val valuePaint = Paint(textPaint).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 18f
            color = android.graphics.Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }

        val labelPaint = Paint(textPaint).apply {
            textSize = 9.5f
            color = android.graphics.Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(value, left + tileWidth / 2f, y + 26f, valuePaint)
        canvas.drawText(label, left + tileWidth / 2f, y + 44f, labelPaint)
    }

    fun drawTableHeader() {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(2, 43, 74)
        }

        canvas.drawRoundRect(
            margin,
            y,
            pageWidth - margin,
            y + 30f,
            10f,
            10f,
            bg
        )

        headerPaint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT

        val cols = if (isEnglish) {
            listOf(
                margin + 14f,
                margin + 190f,
                margin + 270f,
                margin + 330f,
                margin + 395f,
                margin + 455f
            )
        } else {
            listOf(
                pageWidth - margin - 14f,
                pageWidth - margin - 190f,
                pageWidth - margin - 270f,
                pageWidth - margin - 330f,
                pageWidth - margin - 395f,
                pageWidth - margin - 455f
            )
        }

        listOf(
            tr("שם", "Name"),
            tr("דרגה", "Rank"),
            tr("גיל", "Age"),
            tr("ותק", "Seniority"),
            tr("נוכחות", "Attendance"),
            tr("טלפון", "Phone")
        ).forEachIndexed { index, title ->
            canvas.drawText(title, cols[index], y + 20f, headerPaint)
        }

        y += 42f
    }

    fun drawTraineeRow(index: Int, trainee: TraineeProfile) {
        ensureSpace(38f)

        val rowBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (index % 2 == 0) {
                android.graphics.Color.rgb(248, 251, 255)
            } else {
                android.graphics.Color.rgb(234, 244, 255)
            }
        }

        canvas.drawRoundRect(
            margin,
            y - 18f,
            pageWidth - margin,
            y + 12f,
            8f,
            8f,
            rowBg
        )

        val beltColor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = beltPdfColor(trainee.belt)
        }

        val dotX = if (isEnglish) margin + 8f else pageWidth - margin - 8f
        canvas.drawCircle(dotX, y - 3f, 4f, beltColor)

        textPaint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
        textPaint.color = android.graphics.Color.rgb(15, 23, 42)
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.textSize = 10.5f

        val cols = if (isEnglish) {
            listOf(
                margin + 18f,
                margin + 190f,
                margin + 270f,
                margin + 330f,
                margin + 395f,
                margin + 455f
            )
        } else {
            listOf(
                pageWidth - margin - 18f,
                pageWidth - margin - 190f,
                pageWidth - margin - 270f,
                pageWidth - margin - 330f,
                pageWidth - margin - 395f,
                pageWidth - margin - 455f
            )
        }

        val values = listOf(
            trainee.fullName.ifBlank { "—" }.take(24),
            coachBeltNameForPdf(trainee.belt.ifBlank { "—" }, isEnglish).take(12),
            trainee.age.takeIf { it > 0 }?.toString() ?: "—",
            trainee.seniority.ifBlank { "—" }.take(10),
            trainee.attendancePct.takeIf { it > 0 }?.let { "$it%" } ?: "—",
            trainee.phone.ifBlank { "—" }.take(14)
        )

        values.forEachIndexed { colIndex, value ->
            canvas.drawText(value, cols[colIndex], y, textPaint)
        }

        y += 34f
    }

    drawHeader()

    sectionPaint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    canvas.drawText(
        tr("סיכום קבוצה", "Group Summary"),
        textXStart(),
        y,
        sectionPaint
    )

    y += 14f

    drawSummaryTile(0, tr("מתאמנים", "Trainees"), stats.totalTrainees.toString())
    drawSummaryTile(1, tr("נוכחות ממוצעת", "Avg attendance"), if (stats.avgAttendance > 0) "${stats.avgAttendance}%" else "—")
    drawSummaryTile(2, tr("וותק ממוצע", "Avg seniority"), formatAvgSeniority(stats.avgSeniority, isEnglish))

    y += 82f

    sectionPaint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    canvas.drawText(
        tr("רשימת מתאמנים", "Trainees"),
        textXStart(),
        y,
        sectionPaint
    )

    y += 16f

    drawTableHeader()

    profiles.forEachIndexed { index, trainee ->
        drawTraineeRow(index, trainee)
    }

    y += 18f
    ensureSpace(90f)

    sectionPaint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    canvas.drawText(
        tr("התפלגות חגורות", "Belt Distribution"),
        textXStart(),
        y,
        sectionPaint
    )

    y += 22f

    stats.beltCounts.forEach { (belt, count) ->
        ensureSpace(24f)

        val color = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = beltPdfColor(belt)
        }

        val dotX = if (isEnglish) margin + 6f else pageWidth - margin - 6f
        canvas.drawCircle(dotX, y - 4f, 5f, color)

        textPaint.textAlign = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textPaint.color = android.graphics.Color.rgb(15, 23, 42)
        textPaint.textSize = 11.5f

        canvas.drawText(
            "${coachBeltNameForPdf(belt, isEnglish)}: $count",
            if (isEnglish) margin + 20f else pageWidth - margin - 20f,
            y,
            textPaint
        )

        y += 22f
    }

    drawFooter()
    document.finishPage(page)

    val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
    val file = File(dir, "coach_trainees_${System.currentTimeMillis()}.pdf")

    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }

    document.close()
    return file
}

private fun coachBeltNameForPdf(
    beltName: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return beltName

    return when (beltName.trim()) {
        "לבנה" -> "White"
        "צהובה" -> "Yellow"
        "כתומה" -> "Orange"
        "ירוקה" -> "Green"
        "כחולה" -> "Blue"
        "חומה" -> "Brown"
        "שחורה" -> "Black"
        "ללא דרגה" -> "No rank"
        else -> beltName
    }
}