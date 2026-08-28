package il.kmi.app.screens.coach

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.app.search.KmiSearchBridge
import il.kmi.app.localization.rememberIsEnglish
import il.kmi.app.privacy.DemoPrivacy
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.SubTopicRegistry
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.size
import il.kmi.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import android.graphics.RectF
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.ui.focus.focusRequester
import android.graphics.Color as AColor
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.app.ui.pdf.KmiPdfFooter
import androidx.compose.ui.graphics.luminance


//============================================================================

// ======================
// מודלים ולוגיקה
// ======================

// ✅ ציון לתרגיל: 0..10
private fun clampScore10(v: Int): Int = v.coerceIn(0, 10)

data class ExamExerciseItem(
    val id: String,
    val belt: Belt,      // החגורה האמיתית של התרגיל
    val topic: String,   // נושא ראשי בלבד
    val subTopic: String? = null,
    val name: String
)

data class InternalExamSession(
    val traineeName: String,
    val belt: Belt,
    val date: LocalDate,
    val exercises: List<ExamExerciseItem>,
    val marks: List<Int?>,
) {

    // רק תרגילים שסומנו
    private val answeredMarks: List<Int> =
        marks.filterNotNull()

    val totalScore: Double
        get() = answeredMarks.sum().toDouble()

    val maxScore: Double
        get() = answeredMarks.size * 10.0

    val percent: Int
        get() = if (maxScore == 0.0) 0
        else ((totalScore / maxScore) * 100).toInt()

    val summaryText: String
        get() = when {
            percent >= 85 -> "עבר בהצטיינות"
            percent >= 70 -> "עבר"
            percent >= 50 -> "נדרש שיפור"
            else -> "לא עבר"
        }
}

// תוצאה לכל חגורה (לסיכומים לפי חגורה)
private data class BeltScore(
    val total: Double,
    val max: Double
) {
    val percent: Int
        get() = if (max == 0.0) 0 else ((total / max) * 100.0).toInt()

    // ✅ ציון מנורמל 0–10
    val score10: Double
        get() = if (max == 0.0) 0.0 else (total / max) * 10.0
}

private data class RecentInternalExamResultUi(
    val resultId: String,
    val traineeName: String,
    val beltName: String,
    val score10: Double,
    val percent: Int,
    val completedAtMillis: Long
)

// הדפסה יפה של ניקוד
private fun Double.toScoreString(): String {
    if (this == 0.0) return "0"

    val intPart = this.toInt()

    return if (abs(this - intPart) < 1e-6) {
        intPart.toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", this)
    }
}

private fun examTr(isEnglish: Boolean, he: String, en: String): String =
    if (isEnglish) en else he

private fun examBeltNameForUi(belt: Belt, isEnglish: Boolean): String =
    if (isEnglish) belt.en else belt.heb

private fun examBeltShortNameForUi(belt: Belt, isEnglish: Boolean): String {
    val full = examBeltNameForUi(belt, isEnglish).trim()

    return if (isEnglish) {
        full
            .removeSuffix(" Belt")
            .removeSuffix(" belt")
            .trim()
    } else {
        full
            .removePrefix("חגורה ")
            .removePrefix("חגורת ")
            .trim()
    }
}

private fun examBeltMainColor(belt: Belt): Color =
    when (belt) {
        Belt.YELLOW -> Color(0xFFFDE047)
        Belt.ORANGE -> Color(0xFFFF8A00)
        Belt.GREEN -> Color(0xFF16A34A)
        Belt.BLUE -> Color(0xFF2563EB)
        Belt.BROWN -> Color(0xFF7C3F1D)
        Belt.BLACK -> Color(0xFF111827)
        else -> Color(0xFF7C3AED)
    }

private fun examBeltDarkColor(belt: Belt): Color =
    when (belt) {
        Belt.YELLOW -> Color(0xFF854D0E)
        Belt.ORANGE -> Color(0xFF7C2D12)
        Belt.GREEN -> Color(0xFF064E3B)
        Belt.BLUE -> Color(0xFF1E3A8A)
        Belt.BROWN -> Color(0xFF3B1F12)
        Belt.BLACK -> Color(0xFF020617)
        else -> Color(0xFF312E81)
    }

private fun examBeltSoftColor(belt: Belt): Color =
    when (belt) {
        Belt.YELLOW -> Color(0xFFFEFCE8)
        Belt.ORANGE -> Color(0xFFFFEDD5)
        Belt.GREEN -> Color(0xFFDCFCE7)
        Belt.BLUE -> Color(0xFFDBEAFE)
        Belt.BROWN -> Color(0xFFF3E8D6)
        Belt.BLACK -> Color(0xFFE5E7EB)
        else -> Color(0xFFEDE9FE)
    }

private fun examBeltDrawableRes(belt: Belt): Int =
    when (belt) {
        Belt.YELLOW -> R.drawable.intro_belt_yellow
        Belt.ORANGE -> R.drawable.intro_belt_orange
        Belt.GREEN -> R.drawable.intro_belt_green
        Belt.BLUE -> R.drawable.intro_belt_blue
        Belt.BROWN -> R.drawable.intro_belt_brown
        Belt.BLACK -> R.drawable.intro_belt_black
        else -> R.drawable.intro_belt_black
    }

private fun internalExamEntryScreenBrush():
        androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FBFF),
            Color(0xFFEAF4FF),
            Color(0xFFB7DDF7),
            Color(0xFF1F78B4),
            Color(0xFF062B4A)
        )
    )

private fun examBeltScreenBrush():
        androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FBFF),
            Color(0xFFEAF4FF),
            Color(0xFFB7DDF7),
            Color(0xFF1F78B4),
            Color(0xFF062B4A)
        )
    )

private fun examBeltButtonBrush(belt: Belt): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.horizontalGradient(
        listOf(
            examBeltDarkColor(belt).copy(alpha = 0.82f),
            examBeltMainColor(belt).copy(alpha = if (belt == Belt.YELLOW) 0.62f else 0.74f),
            Color(0xFF7C3AED).copy(alpha = 0.74f)
        )
    )

private fun examTitleForUi(raw: String, isEnglish: Boolean): String =
    if (isEnglish) ExerciseTitlesEn.getOrSame(raw.trim()) else raw

private fun examStatusText(percent: Int, isEnglish: Boolean): String {
    return if (isEnglish) {
        when {
            percent >= 85 -> "Passed with excellence"
            percent >= 70 -> "Passed"
            percent >= 50 -> "Needs improvement"
            else -> "Did not pass"
        }
    } else {
        when {
            percent >= 85 -> "עבר בהצטיינות"
            percent >= 70 -> "עבר"
            percent >= 50 -> "נדרש שיפור"
            else -> "לא עבר"
        }
    }
}

private fun examSummaryText(percent: Int, isEnglish: Boolean): String {
    return if (isEnglish) {
        when {
            percent >= 85 -> "Passed very successfully"
            percent >= 70 -> "Passed successfully"
            percent >= 50 -> "Average - needs improvement"
            else -> "Did not pass the exam"
        }
    } else {
        when {
            percent >= 85 -> "עבר בהצלחה רבה"
            percent >= 70 -> "עבר בהצלחה"
            percent >= 50 -> "בינוני – נדרש שיפור"
            else -> "לא עבר את המבחן"
        }
    }
}

private fun buildCompletedExamShareSummary(
    session: InternalExamSession,
    isEnglish: Boolean,
    demoIndex: Int? = null
): String {
    val score10 =
        if (session.maxScore == 0.0) {
            0.0
        } else {
            (
                    session.totalScore /
                            session.maxScore
                    ) * 10.0
        }

    val dateText =
        session.date.format(
            DateTimeFormatter.ofPattern(
                "dd.MM.yyyy"
            )
        )

    val beltName =
        examBeltNameForUi(
            session.belt,
            isEnglish
        )

    val statusText =
        examStatusText(
            session.percent,
            isEnglish
        )

    val displayTraineeName =
        if (DemoPrivacy.isEnabled()) {
            TraineeDisplayNameMapper.displayName(
                realName =
                    session.traineeName,
                stableKey =
                    internalExamTraineeKey(
                        session.traineeName
                    ),
                demoIndex = demoIndex,
                isEnglish = isEnglish
            )
        } else {
            session.traineeName
        }

    return if (isEnglish) {
        """
        Internal Exam Summary
        Trainee: $displayTraineeName
        Belt: $beltName
        Date: $dateText
        Score: ${score10.coerceIn(0.0, 10.0).toScoreString()} / 10 (${session.percent}%)
        Status: $statusText
        """.trimIndent()
    } else {
        """
        סיכום מבחן פנימי
        נבחן: $displayTraineeName
        חגורה: $beltName
        תאריך: $dateText
        ציון: ${score10.coerceIn(0.0, 10.0).toScoreString()} / 10 (${session.percent}%)
        סטטוס: $statusText
        """.trimIndent()
    }
}

// כל החגורות מהצהובה ועד החגורה הנבחנת
private fun beltsUpTo(target: Belt): List<Belt> {
    val all = listOf(
        Belt.YELLOW,
        Belt.ORANGE,
        Belt.GREEN,
        Belt.BLUE,
        Belt.BROWN,
        Belt.BLACK
    )
    val idx = all.indexOf(target)
    return if (idx == -1) all else all.take(idx + 1)
}

private fun buildInternalExamSessionForUi(
    traineeName: String,
    belt: Belt,
    marksMap: Map<String, Int>
): InternalExamSession {
    val allExercises = beltsUpTo(belt)
        .flatMap { buildInternalExamExercisesFromContent(it) }
        .distinctBy { it.id }

    return InternalExamSession(
        traineeName = traineeName,
        belt = belt,
        date = LocalDate.now(),
        exercises = allExercises,
        marks = allExercises.map { ex -> marksMap[ex.id] }
    )
}

// ======================
// יצוא PDF
// ======================

object InternalExamPdf {

    fun createPdf(
        context: Context,
        session: InternalExamSession,
        isEnglish: Boolean = false,
        demoIndex: Int? = null
    ): Uri? {
        return try {
            val document = PdfDocument()

            // A4 (pt)
            val pageW = 595
            val pageH = 842

            val leftMargin = 40f
            val rightMargin = (pageW - 40).toFloat()

            val contentTop =
                KmiPdfHeader.CONTENT_TOP

            val contentBottom =
                pageH -
                        KmiPdfFooter.CONTENT_BOTTOM_PADDING

            fun pdfTr(he: String, en: String): String =
                if (isEnglish) en else he

            fun pdfBeltName(belt: Belt): String =
                if (isEnglish) belt.en else belt.heb

            fun pdfExerciseTitle(raw: String): String =
                if (isEnglish) ExerciseTitlesEn.getOrSame(raw.trim()) else raw

            fun pdfStatusText(percent: Int): String =
                examStatusText(percent, isEnglish)

            val pdfTraineeName =
                if (DemoPrivacy.isEnabled()) {
                    TraineeDisplayNameMapper.displayName(
                        realName = session.traineeName,
                        stableKey =
                            internalExamTraineeKey(
                                session.traineeName
                            ),
                        demoIndex = demoIndex,
                        isEnglish = isEnglish
                    )
                } else {
                    session.traineeName
                }

            fun pdfPillLabel(percent: Int): String {
                return if (isEnglish) {
                    when {
                        percent >= 85 -> "Excellent"
                        percent >= 70 -> "Good"
                        percent >= 50 -> "Average"
                        else -> "Weak"
                    }
                } else {
                    when {
                        percent >= 85 -> "מצוין"
                        percent >= 70 -> "טוב"
                        percent >= 50 -> "בינוני"
                        else -> "חלש"
                    }
                }
            }

            fun percentColor(p: Int): Int {
                // אדום -> צהוב -> ירוק
                return when {
                    p >= 85 -> "#16A34A".toColorInt() // green
                    p >= 70 -> "#84CC16".toColorInt() // lime
                    p >= 50 -> "#F59E0B".toColorInt() // amber
                    else -> "#EF4444".toColorInt() // red
                }
            }

            val cardBg = Paint().apply {
                isAntiAlias = true
                color = "#F8FAFC".toColorInt()
            }
            val cardStroke = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = "#E2E8F0".toColorInt()
            }

            val kpiLabel = Paint().apply {
                isAntiAlias = true
                color = "#64748B".toColorInt()
                textSize = 11f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }
            val kpiValue = Paint().apply {
                isAntiAlias = true
                color = "#0F172A".toColorInt()
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val sectionTitle = Paint().apply {
                isAntiAlias = true
                color = "#0F172A".toColorInt()
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val topicTitle = Paint().apply {
                isAntiAlias = true
                color = "#1E293B".toColorInt()
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val lineText = Paint().apply {
                isAntiAlias = true
                color = "#0F172A".toColorInt()
                textSize = 12.5f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }

            val scoreBoxBg = Paint().apply {
                isAntiAlias = true
                color = "#EEF2FF".toColorInt()
            }
            val scoreBoxStroke = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
                color = "#C7D2FE".toColorInt()
            }
            val scoreBoxText = Paint().apply {
                isAntiAlias = true
                color = "#0F172A".toColorInt()
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val divider = Paint().apply {
                isAntiAlias = true
                color = "#E2E8F0".toColorInt()
                strokeWidth = 1.2f
            }

            fun drawRight(
                canvas: android.graphics.Canvas,
                text: String,
                y: Float,
                paint: Paint
            ) {
                val w = paint.measureText(text)
                canvas.drawText(text, rightMargin - w, y, paint)
            }

            fun drawStart(canvas: android.graphics.Canvas, text: String, y: Float, paint: Paint) {
                if (isEnglish) {
                    canvas.drawText(text, leftMargin, y, paint)
                } else {
                    drawRight(canvas, text, y, paint)
                }
            }

            fun drawHeader(
                canvas: android.graphics.Canvas
            ) {
                val generatedDate =
                    session.date.format(
                        DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy"
                        )
                    )

                KmiPdfHeader.draw(
                    context = context,
                    canvas = canvas,
                    pageWidth = pageW,
                    isEnglish = isEnglish,
                    titleHebrew = "דו״ח מבחן פנימי",
                    titleEnglish = "Internal Exam Report",
                    subtitleHebrew =
                        "חגורה: ${pdfBeltName(session.belt)}",
                    subtitleEnglish =
                        "Belt: ${pdfBeltName(session.belt)}",
                    generatedDate = generatedDate
                )
            }

            fun drawFooter(
                canvas: android.graphics.Canvas,
                pageNumber: Int
            ) {
                KmiPdfFooter.draw(
                    canvas = canvas,
                    pageWidth = pageW,
                    pageHeight = pageH,
                    pageNumber = pageNumber,
                    totalPages = null,
                    isEnglish = isEnglish
                )
            }

            fun drawKpiCards(canvas: android.graphics.Canvas, startY: Float): Float {
                var y = startY

                val cardH = 64f
                val gap = 10f
                val totalW = rightMargin - leftMargin
                val cardW = (totalW - gap * 2) / 3f

                val dateStr = session.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

                fun card(x: Float, label: String, value: String) {
                    val r = RectF(x, y, x + cardW, y + cardH)
                    canvas.drawRoundRect(r, 14f, 14f, cardBg)
                    canvas.drawRoundRect(r, 14f, 14f, cardStroke)

                    // label
                    canvas.drawText(label, x + 14f, y + 24f, kpiLabel)
                    // value (ימין בתוך הכרטיס)
                    val vw = kpiValue.measureText(value)
                    canvas.drawText(value, x + cardW - 14f - vw, y + 46f, kpiValue)
                }

                val x2 = leftMargin + cardW + gap
                val x3 = leftMargin + (cardW + gap) * 2

                card(
                    leftMargin,
                    pdfTr(
                        "שם מתאמן",
                        "Trainee name"
                    ),
                    pdfTraineeName.ifBlank {
                        "—"
                    }
                )

                card(
                    x2,
                    pdfTr("חגורה במבחן", "Exam belt"),
                    pdfBeltName(session.belt)
                )

                card(
                    x3,
                    pdfTr("תאריך", "Date"),
                    dateStr
                )

                y += cardH + 16f
                return y
            }

            fun drawScoreBadge(canvas: android.graphics.Canvas, startY: Float): Float {
                var y = startY

                val p = session.percent
                val c = percentColor(p)

                val badgeR = RectF(leftMargin, y, rightMargin, y + 78f)
                val badgeBg = Paint().apply { isAntiAlias = true; color = "#FFFFFF".toColorInt() }
                val badgeStroke = Paint().apply {
                    isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1.6f; color =
                    "#E2E8F0".toColorInt()
                }
                canvas.drawRoundRect(badgeR, 18f, 18f, badgeBg)
                canvas.drawRoundRect(badgeR, 18f, 18f, badgeStroke)

                val pillR = if (isEnglish) {
                    RectF(rightMargin - 150f, y + 18f, rightMargin - 18f, y + 60f)
                } else {
                    RectF(leftMargin + 18f, y + 18f, leftMargin + 150f, y + 60f)
                }

                val pillPaint = Paint().apply { isAntiAlias = true; color = c }
                canvas.drawRoundRect(pillR, 20f, 20f, pillPaint)

                val pillText = Paint().apply {
                    isAntiAlias = true
                    color = AColor.WHITE
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }

                val scoreLine = pdfTr(
                    "ציון: ${session.totalScore.toInt()} / ${session.maxScore.toInt()}  (${p}%)",
                    "Score: ${session.totalScore.toInt()} / ${session.maxScore.toInt()}  (${p}%)"
                )

                val statusLine = pdfTr(
                    "סטטוס: ${pdfStatusText(p)}",
                    "Status: ${pdfStatusText(p)}"
                )

                val scorePaint = Paint().apply {
                    isAntiAlias = true
                    color = "#0F172A".toColorInt()
                    textSize = 16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                val statusPaint = Paint().apply {
                    isAntiAlias = true
                    color = "#334155".toColorInt()
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                }

                drawStart(canvas, scoreLine, y + 40f, scorePaint)
                drawStart(canvas, statusLine, y + 62f, statusPaint)

                // טקסט בתוך ה-pill
                val pillLabel = pdfPillLabel(p)
                val tw = pillText.measureText(pillLabel)
                canvas.drawText(
                    pillLabel,
                    pillR.centerX() - tw / 2f,
                    pillR.centerY() + 5f,
                    pillText
                )

                y += 78f + 16f
                return y
            }

            fun drawScoreBox(
                canvas: android.graphics.Canvas,
                xRight: Float,
                yTop: Float,
                score: Int
            ) {
                val w = 40f
                val r = RectF(
                    xRight - w,
                    yTop - 16f,
                    xRight,
                    yTop + 6f
                )
                canvas.drawRoundRect(r, 7f, 7f, scoreBoxBg)
                canvas.drawRoundRect(r, 7f, 7f, scoreBoxStroke)

                val s = score.toString()
                val tw = scoreBoxText.measureText(s)
                canvas.drawText(s, r.centerX() - tw / 2f, r.centerY() + 5f, scoreBoxText)
            }

            fun drawTextWithin(
                canvas: android.graphics.Canvas,
                text: String,
                xRight: Float,
                y: Float,
                paint: Paint
            ) {
                if (isEnglish) {
                    val maxWidth = xRight - leftMargin - 8f
                    var clean = text.trim()

                    while (clean.isNotBlank() && paint.measureText(clean) > maxWidth) {
                        clean = clean.dropLast(1)
                    }

                    if (clean.length < text.trim().length && clean.length > 3) {
                        clean = clean.dropLast(3) + "..."
                    }

                    canvas.drawText(clean, leftMargin, y, paint)
                } else {
                    val w = paint.measureText(text)
                    canvas.drawText(text, xRight - w, y, paint)
                }
            }

            // ====== רינדור עם ריבוי עמודים ======
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            drawHeader(canvas)

            var y = contentTop
            y = drawKpiCards(canvas, y)
            y = drawScoreBadge(canvas, y)

            // Section Title
            drawStart(
                canvas,
                pdfTr("פירוט תרגילים", "Exercise details"),
                y,
                sectionTitle
            )
            y += 16f
            canvas.drawLine(leftMargin, y, rightMargin, y, divider)
            y += 16f

            // ✅ הופכים את התרגילים שסומנו לרשימה “שטוחה” לרינדור
            data class PdfRow(
                val belt: Belt,
                val topic: String,
                val subTopic: String,
                val name: String,
                val score: Int
            )

            val rows: List<PdfRow> =
                session.exercises.mapIndexedNotNull { index, ex ->
                    val score = session.marks.getOrNull(index) ?: return@mapIndexedNotNull null
                    PdfRow(
                        belt = ex.belt,
                        topic = pdfExerciseTitle(ex.topic),
                        subTopic = ex.subTopic?.let { pdfExerciseTitle(it) }.orEmpty(),
                        name = pdfExerciseTitle(ex.name),
                        score = clampScore10(score)
                    )
                }

            var currentBelt: Belt? = null
            var currentTopic: String? = null
            var currentSubTopic: String? = null

            fun newPage() {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                drawHeader(canvas)
                y = contentTop

                drawStart(
                    canvas,
                    pdfTr(
                        "פירוט תרגילים (המשך)",
                        "Exercise details — continued"
                    ),
                    y,
                    sectionTitle
                )
                y += 16f
                canvas.drawLine(leftMargin, y, rightMargin, y, divider)
                y += 16f
                currentBelt = null
                currentTopic = null
                currentSubTopic = null
            }

            fun beltPdfColor(belt: Belt): Int {
                return when (belt) {
                    Belt.YELLOW -> "#CA8A04".toColorInt()
                    Belt.ORANGE -> "#EA580C".toColorInt()
                    Belt.GREEN -> "#16A34A".toColorInt()
                    Belt.BLUE -> "#2563EB".toColorInt()
                    Belt.BROWN -> "#7C3F1D".toColorInt()
                    Belt.BLACK -> "#111827".toColorInt()
                    else -> "#7C3AED".toColorInt()
                }
            }

            rows.forEach { r ->
                if (y > contentBottom - 24f) {
                    drawFooter(canvas, pageNumber)
                    newPage()
                }

                if (currentBelt != r.belt) {
                    currentBelt = r.belt
                    currentTopic = null
                    currentSubTopic = null

                    if (y > contentBottom - 44f) {
                        drawFooter(canvas, pageNumber)
                        newPage()
                    }

                    topicTitle.color = beltPdfColor(r.belt)

                    drawStart(
                        canvas,
                        pdfTr(
                            "חגורה: ${pdfBeltName(r.belt)}",
                            "Belt: ${pdfBeltName(r.belt)}"
                        ),
                        y,
                        topicTitle
                    )

                    topicTitle.color = "#1E293B".toColorInt()
                    y += 20f
                }

                if (currentTopic != r.topic) {
                    currentTopic = r.topic
                    currentSubTopic = null

                    if (y > contentBottom - 40f) {
                        drawFooter(canvas, pageNumber)
                        newPage()
                    }

                    drawStart(
                        canvas,
                        pdfTr(
                            "נושא: ${r.topic}",
                            "Topic: ${r.topic}"
                        ),
                        y,
                        topicTitle
                    )
                    y += 24f
                }

                if (r.subTopic.isNotBlank() && currentSubTopic != r.subTopic) {
                    currentSubTopic = r.subTopic

                    if (y > contentBottom - 34f) {
                        drawFooter(canvas, pageNumber)
                        newPage()
                    }

                    drawStart(
                        canvas,
                        pdfTr(
                            "תת־נושא: ${r.subTopic}",
                            "Sub-topic: ${r.subTopic}"
                        ),
                        y,
                        lineText
                    )
                    y += 16f
                }

                val rowTop = y - 13f
                val rowBottom = y + 10f

                val rowBg = Paint().apply {
                    isAntiAlias = true
                    color = "#F8FAFC".toColorInt()
                }

                val rowStroke = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                    color = "#E2E8F0".toColorInt()
                }

                canvas.drawRoundRect(
                    RectF(leftMargin, rowTop, rightMargin, rowBottom),
                    7f,
                    7f,
                    rowBg
                )
                canvas.drawRoundRect(
                    RectF(leftMargin, rowTop, rightMargin, rowBottom),
                    7f,
                    7f,
                    rowStroke
                )

                if (isEnglish) {
                    drawScoreBox(canvas, xRight = rightMargin - 8f, yTop = y, score = r.score)
                    drawTextWithin(canvas, r.name, rightMargin - 58f, y, lineText)
                } else {
                    drawScoreBox(canvas, xRight = leftMargin + 48f, yTop = y, score = r.score)

                    val textRight = rightMargin - 10f
                    val maxTextWidth = rightMargin - leftMargin - 70f
                    val cleanName = ellipsizePdfText(
                        text = r.name,
                        paint = lineText,
                        maxWidth = maxTextWidth
                    )
                    val textWidth = lineText.measureText(cleanName)

                    canvas.drawText(
                        cleanName,
                        textRight - textWidth,
                        y,
                        lineText
                    )
                }

                y += 26f
            }

            drawFooter(canvas, pageNumber)

            document.finishPage(page)

            val dir =
                File(
                    context.cacheDir,
                    "internal_exam"
                ).apply {
                    mkdirs()
                }

            /*
             * שם קבוע לפי שפת האפליקציה.
             *
             * מאחר שהשם קבוע, יצירת PDF חדש
             * מחליפה את הקובץ הקודם ולא משאירה
             * עותקים ישנים בתיקיית המטמון.
             */
            val fileName =
                if (isEnglish) {
                    "Internal Exam Report.pdf"
                } else {
                    "דוח מבחן פנימי.pdf"
                }

            val file =
                File(
                    dir,
                    fileName
                )

            FileOutputStream(
                file,
                false
            ).use { out ->
                document.writeTo(out)
            }

            document.close()

            FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun ellipsizePdfText(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): String {
        var clean = text.trim()

        if (paint.measureText(clean) <= maxWidth) {
            return clean
        }

        while (clean.length > 4 && paint.measureText("$clean...") > maxWidth) {
            clean = clean.dropLast(1)
        }

        return "$clean..."
    }

    fun sharePdf(
        context: Context,
        uri: Uri,
        isEnglish: Boolean = false
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserTitle = examTr(
            isEnglish,
            "שיתוף דו\"ח מבחן פנימי",
            "Share internal exam report"
        )

        context.startActivity(
            Intent.createChooser(intent, chooserTitle)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

// ======================
// מסך קומפוז – מבחן פנימי
// ======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalExamScreen(
    traineeName: String,
    onTraineeNameChange: (String) -> Unit,
    belt: Belt,
    exercises: List<ExamExerciseItem>,
    @Suppress("UNUSED_PARAMETER")
    examResults: Map<String, Boolean> = emptyMap(),
    @Suppress("UNUSED_PARAMETER")
    currentScore: Float = 0f,
    @Suppress("UNUSED_PARAMETER")
    onResultUpdate: (String, Boolean) -> Unit = { _, _ -> },
    onBeltChange: (Belt) -> Unit,
    onBack: () -> Unit,
    onExportPdf: (
        InternalExamSession
    ) -> Unit = {},
    sharedMarksMap: MutableMap<String, Int>? = null,
    showSetupHeader: Boolean = true
) {
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val isEnglish = rememberIsEnglish()
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val traineeCardColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme
                .surfaceVariant
        } else {
            Color(0xFFE0F2FE)
        }
    // ✅ דיאלוג "נבחנים אחרונים"
    var showPickTraineeDialog by remember { mutableStateOf(false) }
    var recentTrainees by remember { mutableStateOf<List<String>>(emptyList()) }

// ✅ טוען רשימה ראשונית
    LaunchedEffect(Unit) {
        recentTrainees = loadRecentTrainees(ctx)
    }

    // ✅ האם להציג את בלוק שם הנבחן (נעלם אחרי Done/שמור)
    var showTraineeNameBox by rememberSaveable { mutableStateOf(traineeName.isBlank()) }

    fun commitTraineeNameAndCollapse(): Boolean {
        val name = traineeName.trim()
        if (name.isBlank()) return false

        pushRecentTrainee(ctx, name)
        saveLastTrainee(ctx, name)

        scope.launch {
            recentTrainees = loadRecentTrainees(ctx)
        }

        focusManager.clearFocus()
        keyboard?.hide()
        showTraineeNameBox = false
        return true
    }

    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var showFinishExamConfirmDialog by remember { mutableStateOf(false) }
    var pendingLoadedDraft by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    var isSavingFinalResult by remember { mutableStateOf(false) }

    // ✅ כדי שלא נפתח דיאלוג על כל אות בזמן שמקלידים שם
    var resumeCheckedKey by remember { mutableStateOf<String?>(null) }

    // ✅ ציון לכל תרגיל: 0..10
    // אם נשלחת מפה משותפת ממסך ההכנה — משתמשים בה כדי לשמור ציונים בין חגורות.
    val localMarksMap = remember { mutableStateMapOf<String, Int>() }
    val marksMap: MutableMap<String, Int> = sharedMarksMap ?: localMarksMap

    // ✅ דיאלוג יציאה / חזרה
    BackHandler {
        if (hasUnsavedChanges) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    // ✅ טעינת Draft מהשרת אם קיים
    LaunchedEffect(traineeName, belt) {
        val name = traineeName.trim()
        if (name.isBlank()) return@LaunchedEffect

        val key = "${belt.name}_${internalExamTraineeKey(name)}"
        if (resumeCheckedKey == key) return@LaunchedEffect
        resumeCheckedKey = key

        val loaded = loadExamDraft(ctx, name, belt)
        if (loaded.isNotEmpty()) {
            pendingLoadedDraft = loaded
            showResumeDialog = true
        }
    }

    Scaffold(
        topBar = {
            KmiTopBar(
                title = examTr(
                    isEnglish,
                    "מבחן פנימי",
                    "Internal exam"
                ),
                showMenu = true,
                showBottomActions = true,
                showRoleStatus = true,
                showModePill = true,
                showTopHome = false,
                showTopSearch = true,
                showSettings = true,
                showTopShare = true,
                onShare = {
                    val activeName =
                        traineeName.trim()

                    if (activeName.isBlank()) {
                        Toast.makeText(
                            ctx,
                            examTr(
                                isEnglish,
                                "נא לבחור או להזין שם נבחן לפני שיתוף",
                                "Please select or enter a trainee name before sharing"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (marksMap.isEmpty()) {
                        Toast.makeText(
                            ctx,
                            examTr(
                                isEnglish,
                                "אין ציונים ליצירת קובץ PDF",
                                "There are no scores for creating a PDF"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val pdfSession =
                            buildInternalExamSessionForUi(
                                traineeName = activeName,
                                belt = belt,
                                marksMap = marksMap
                            )

                        onExportPdf(
                            pdfSession
                        )
                    }
                },
                centerTitle = true,
                onHome = onBack,
                onPickSearchResult = {}
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    examBeltScreenBrush()
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                if (showSetupHeader) {
                    // ✅ מצב עבודה: אם יש שם נבחן והוא כבר "ננעל" – מציגים פס קומפקטי
                    val hasActiveTrainee = traineeName.trim().isNotBlank() && !showTraineeNameBox

                    if (showTraineeNameBox) {
                        // 🟦 מצב בחירת/הזנת נבחן
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        traineeCardColor,
                                    contentColor =
                                        MaterialTheme.colorScheme
                                            .onSurface
                                ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = traineeName,
                                    onValueChange = { onTraineeNameChange(it) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text(examTr(isEnglish, "שם הנבחן", "Trainee name")) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = { commitTraineeNameAndCollapse() }
                                    )
                                )

                                Spacer(Modifier.width(10.dp))

                                Button(
                                    onClick = { commitTraineeNameAndCollapse() },
                                    enabled = traineeName.trim().isNotBlank()
                                ) {
                                    Text(examTr(isEnglish, "אישור", "OK"))
                                }
                            }
                        }

                    } else if (hasActiveTrainee) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        traineeCardColor,
                                    contentColor =
                                        MaterialTheme.colorScheme
                                            .onSurface
                                ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text =
                                        TraineeDisplayNameMapper.displayName(
                                            realName =
                                                traineeName.trim(),
                                            stableKey =
                                                internalExamTraineeKey(
                                                    traineeName.trim()
                                                ),
                                            demoIndex =
                                                recentTrainees
                                                    .indexOfFirst {
                                                        it.trim().equals(
                                                            traineeName.trim(),
                                                            ignoreCase = true
                                                        )
                                                    }
                                                    .takeIf {
                                                        it >= 0
                                                    }
                                                    ?.plus(1),
                                            isEnglish = isEnglish
                                        ),
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Button(
                                    onClick = {
                                        scope.launch {
                                            recentTrainees = loadRecentTrainees(ctx)
                                            showPickTraineeDialog = true
                                        }
                                    }
                                ) { Text(examTr(isEnglish, "החלף", "Change")) }

                                Button(
                                    onClick = {
                                        marksMap.clear()
                                        onTraineeNameChange("")
                                        showTraineeNameBox = true
                                        resumeCheckedKey = null
                                    }
                                ) { Text(examTr(isEnglish, "חדש", "New")) }
                            }
                        }
                    }

                    if (showPickTraineeDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showPickTraineeDialog = false },
                            title = { Text(examTr(isEnglish, "בחר נבחן", "Select trainee")) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                                    if (recentTrainees.isEmpty()) {
                                        Text(
                                            examTr(
                                                isEnglish,
                                                "אין נבחנים שמורים עדיין.",
                                                "No saved trainees yet."
                                            )
                                        )
                                    } else {
                                        recentTrainees.forEachIndexed { index,
                                                                        name ->
                                            Button(
                                                onClick = {
                                                    marksMap.clear()
                                                    onTraineeNameChange(name)
                                                    showTraineeNameBox = false
                                                    resumeCheckedKey = null
                                                    showPickTraineeDialog = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text =
                                                        TraineeDisplayNameMapper
                                                            .displayName(
                                                                realName =
                                                                    name,
                                                                stableKey =
                                                                    internalExamTraineeKey(
                                                                        name
                                                                    ),
                                                                demoIndex =
                                                                    index + 1,
                                                                isEnglish =
                                                                    isEnglish
                                                            ),
                                                    style =
                                                        KmiTypography.body,
                                                    maxLines = 1,
                                                    overflow =
                                                        TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            marksMap.clear()
                                            onTraineeNameChange("")
                                            showTraineeNameBox = true
                                            resumeCheckedKey = null
                                            showPickTraineeDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(examTr(isEnglish, "נבחן חדש", "New trainee")) }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {}
                        )
                    }

                    // --- בחירת חגורה ---
                    BeltSelector(
                        currentBelt = belt,
                        isEnglish = isEnglish,
                        onBeltChange = onBeltChange
                    )
                }

                // --- סיכום ---
                SummaryCard(
                    currentBelt = belt,
                    marksMap = marksMap,
                    isEnglish = isEnglish
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                val exercisesByTopic = remember(exercises) {
                    exercises.groupBy { it.topic }
                }

                val orderedExercisesByTopic = remember(exercisesByTopic) {
                    exercisesByTopic
                        .toList()
                        .sortedWith(
                            compareByDescending<Pair<String, List<ExamExerciseItem>>> { (_, topicExercises) ->
                                topicExercises.any { !it.subTopic.isNullOrBlank() }
                            }.thenBy { (topic, _) ->
                                topic
                            }
                        )
                }

                var expandedTopic by remember { mutableStateOf<String?>(null) }
                var expandedSubTopicKey by remember { mutableStateOf<String?>(null) }

// --- תרגילים ---
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    orderedExercisesByTopic.forEach { (topic, topicExercises) ->

                        val subTopicGroups = topicExercises
                            .filter { !it.subTopic.isNullOrBlank() }
                            .groupBy { it.subTopic.orEmpty() }

                        val directExercises = topicExercises
                            .filter { it.subTopic.isNullOrBlank() }

                        val hasSubTopics = subTopicGroups.isNotEmpty()
                        val topicIsExpanded = expandedTopic == topic

                        item {
                            TopicHeader(
                                title = examTitleForUi(topic, isEnglish),
                                expanded = topicIsExpanded,
                                hasSubTopics = hasSubTopics,
                                exerciseCount = topicExercises.size,
                                subTopicCount = subTopicGroups.size,
                                isEnglish = isEnglish,
                                onClick = {
                                    if (topicIsExpanded) {
                                        expandedTopic = null
                                        expandedSubTopicKey = null
                                    } else {
                                        expandedTopic = topic
                                        expandedSubTopicKey = null
                                    }
                                }
                            )
                        }

                        if (topicIsExpanded) {
                            if (hasSubTopics) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start =
                                                    if (isEnglish) {
                                                        14.dp
                                                    } else {
                                                        0.dp
                                                    },
                                                end =
                                                    if (isEnglish) {
                                                        0.dp
                                                    } else {
                                                        14.dp
                                                    },
                                                top = 2.dp,
                                                bottom = 4.dp
                                            ),
                                        shape = RoundedCornerShape(22.dp),
                                        color =
                                            MaterialTheme.colorScheme
                                                .surfaceVariant.copy(
                                                    alpha = 0.88f
                                                ),
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp,
                                        border = BorderStroke(
                                            width = 0.75.dp,
                                            color =
                                                MaterialTheme.colorScheme
                                                    .outlineVariant.copy(
                                                        alpha = 0.55f
                                                    )
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            subTopicGroups.entries.forEachIndexed { index, entry ->
                                                val subTopic = entry.key
                                                val subTopicExercises = entry.value
                                                val subTopicKey = "$topic||$subTopic"
                                                val subTopicExpanded =
                                                    expandedSubTopicKey == subTopicKey

                                                SubTopicHeader(
                                                    title = examTitleForUi(subTopic, isEnglish),
                                                    expanded = subTopicExpanded,
                                                    exerciseCount = subTopicExercises.size,
                                                    isEnglish = isEnglish,
                                                    onClick = {
                                                        expandedSubTopicKey =
                                                            if (subTopicExpanded) null else subTopicKey
                                                    }
                                                )

                                                if (index < subTopicGroups.size - 1 || directExercises.isNotEmpty()) {
                                                    HorizontalDivider(
                                                        color = Color(0xFF7FAED6).copy(alpha = 0.46f),
                                                        thickness = 1.dp,
                                                        modifier = Modifier.padding(horizontal = 10.dp)
                                                    )
                                                }

                                                if (subTopicExpanded) {
                                                    subTopicExercises.forEach { ex ->
                                                        key(ex.id) {
                                                            val scoreForThis = marksMap[ex.id]

                                                            ExerciseRow(
                                                                name = examTitleForUi(
                                                                    ex.name,
                                                                    isEnglish
                                                                ),
                                                                score = scoreForThis,
                                                                isEnglish = isEnglish,
                                                                onScoreChange = { newScore ->
                                                                    hasUnsavedChanges = true

                                                                    if (newScore == null) {
                                                                        marksMap.remove(ex.id)
                                                                    } else {
                                                                        marksMap[ex.id] =
                                                                            clampScore10(newScore)
                                                                    }

                                                                    val activeName =
                                                                        traineeName.trim()
                                                                    if (activeName.isNotBlank()) {
                                                                        scope.launch {
                                                                            saveExamDraft(
                                                                                ctx,
                                                                                activeName,
                                                                                belt,
                                                                                marksMap
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (directExercises.isNotEmpty()) {
                                                val generalKey = "$topic||__direct__"
                                                val generalExpanded =
                                                    expandedSubTopicKey == generalKey

                                                SubTopicHeader(
                                                    title = examTr(
                                                        isEnglish,
                                                        "כללי",
                                                        "General"
                                                    ),
                                                    expanded = generalExpanded,
                                                    exerciseCount =
                                                        directExercises.size,
                                                    isEnglish = isEnglish,
                                                    onClick = {
                                                        expandedSubTopicKey =
                                                            if (generalExpanded) null else generalKey
                                                    }
                                                )

                                                if (generalExpanded) {
                                                    directExercises.forEach { ex ->
                                                        key(ex.id) {
                                                            val scoreForThis = marksMap[ex.id]

                                                            ExerciseRow(
                                                                name = examTitleForUi(
                                                                    ex.name,
                                                                    isEnglish
                                                                ),
                                                                score = scoreForThis,
                                                                isEnglish = isEnglish,
                                                                onScoreChange = { newScore ->
                                                                    hasUnsavedChanges = true

                                                                    if (newScore == null) {
                                                                        marksMap.remove(ex.id)
                                                                    } else {
                                                                        marksMap[ex.id] =
                                                                            clampScore10(newScore)
                                                                    }

                                                                    val activeName =
                                                                        traineeName.trim()
                                                                    if (activeName.isNotBlank()) {
                                                                        scope.launch {
                                                                            saveExamDraft(
                                                                                ctx,
                                                                                activeName,
                                                                                belt,
                                                                                marksMap
                                                                            )
                                                                        }
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
                            } else {
                                items(directExercises) { ex ->
                                    val scoreForThis = marksMap[ex.id]

                                    ExerciseRow(
                                        name = examTitleForUi(ex.name, isEnglish),
                                        score = scoreForThis,
                                        isEnglish = isEnglish,
                                        onScoreChange = { newScore ->
                                            hasUnsavedChanges = true

                                            if (newScore == null) {
                                                marksMap.remove(ex.id)
                                            } else {
                                                marksMap[ex.id] = clampScore10(newScore)
                                            }

                                            val activeName = traineeName.trim()
                                            if (activeName.isNotBlank()) {
                                                scope.launch {
                                                    saveExamDraft(ctx, activeName, belt, marksMap)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

                BottomActionBar(
                    isEnglish = isEnglish,
                    isSaving = isSavingFinalResult,
                    finishMode = true,
                    onSave = {
                        val activeName = traineeName.trim()

                        if (activeName.isBlank()) {
                            Toast.makeText(
                                ctx,
                                examTr(
                                    isEnglish,
                                    "נא להזין שם נבחן לפני שמירה",
                                    "Please enter a trainee name before saving"
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@BottomActionBar
                        }

                        if (marksMap.isEmpty()) {
                            Toast.makeText(
                                ctx,
                                examTr(
                                    isEnglish,
                                    "אין ציונים לשמירה",
                                    "There are no scores to save"
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@BottomActionBar
                        }

                        if (isSavingFinalResult) {
                            return@BottomActionBar
                        }

                        showFinishExamConfirmDialog = true
                    }
                )

                // --- תחתית במסך התרגילים: מעבר חזרה למסך הראשי לבחירת חגורה אחרת ---
                ChangeBeltBottomBar(
                    isEnglish = isEnglish,
                    belt = belt,
                    onChangeBelt = {
                        val activeName = traineeName.trim()
                        if (activeName.isNotBlank()) {
                            saveExamDraft(ctx, activeName, belt, marksMap)
                            pushRecentTrainee(ctx, activeName)
                            saveLastTrainee(ctx, activeName)
                        }

                        hasUnsavedChanges = false
                        onBack()
                    }
                )
            }
        }

        if (showFinishExamConfirmDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    if (!isSavingFinalResult) {
                        showFinishExamConfirmDialog = false
                    }
                },
                title = {
                    Text(
                        text = examTr(
                            isEnglish,
                            "סיום מבחן",
                            "Finish exam"
                        )
                    )
                },
                text = {
                    Text(
                        text = examTr(
                            isEnglish,
                            "לאחר אישור סיום המבחן, לא ניתן יהיה להמשיך לערוך את המבחן.\nהתוצאה תישמר כתוצאה סופית בהיסטוריית המבחנים.",
                            "After confirming, this exam can no longer be continued or edited.\nThe result will be saved as a final result in the exam history."
                        )
                    )
                },
                confirmButton = {
                    Button(
                        enabled = !isSavingFinalResult,
                        onClick = {
                            val activeName = traineeName.trim()

                            if (activeName.isBlank() || marksMap.isEmpty()) {
                                showFinishExamConfirmDialog = false
                                return@Button
                            }

                            scope.launch {
                                isSavingFinalResult = true

                                runCatching {
                                    saveExamDraftAwait(
                                        context = ctx,
                                        traineeName = activeName,
                                        belt = belt,
                                        marksMap = marksMap
                                    )

                                    val resultId = saveCompletedInternalExamResult(
                                        traineeName = activeName,
                                        belt = belt,
                                        marksMap = marksMap
                                    )

                                    deleteExamDraftAfterCompletion(
                                        traineeName = activeName,
                                        belt = belt
                                    )

                                    removeRecentTraineeAfterCompletion(
                                        traineeName = activeName
                                    )

                                    resultId
                                }.onSuccess {
                                    marksMap.clear()

                                    hasUnsavedChanges = false
                                    showFinishExamConfirmDialog = false
                                    resumeCheckedKey =
                                        "${belt.name}_${internalExamTraineeKey(activeName)}"

                                    Toast.makeText(
                                        ctx,
                                        examTr(
                                            isEnglish,
                                            "המבחן הסתיים ונשמר בהיסטוריית המבחנים",
                                            "The exam was finished and saved to exam history"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    onBack()
                                }.onFailure { error ->
                                    Toast.makeText(
                                        ctx,
                                        examTr(
                                            isEnglish,
                                            "סיום המבחן נכשל",
                                            "Finishing the exam failed"
                                        ) + ": ${error.localizedMessage ?: ""}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                isSavingFinalResult = false
                            }
                        }
                    ) {
                        Text(
                            text = if (isSavingFinalResult) {
                                examTr(isEnglish, "שומר...", "Saving...")
                            } else {
                                examTr(isEnglish, "אישור", "Confirm")
                            }
                        )
                    }
                },
                dismissButton = {
                    Button(
                        enabled = !isSavingFinalResult,
                        onClick = { showFinishExamConfirmDialog = false }
                    ) {
                        Text(examTr(isEnglish, "ביטול", "Cancel"))
                    }
                }
            )
        }

        if (showResumeDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showResumeDialog = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 0.75.dp,
                        color =
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.34f
                            )
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush =
                                    androidx.compose.ui.graphics.Brush
                                        .verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme
                                                    .surface,
                                                MaterialTheme.colorScheme
                                                    .surfaceVariant.copy(
                                                        alpha = 0.76f
                                                    ),
                                                MaterialTheme.colorScheme
                                                    .primary.copy(
                                                        alpha = 0.10f
                                                    ),
                                                MaterialTheme.colorScheme
                                                    .surface
                                            )
                                        )
                            )
                            .padding(
                                horizontal = 18.dp,
                                vertical = 18.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color =
                                MaterialTheme.colorScheme
                                    .primaryContainer,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = BorderStroke(
                                width = 0.75.dp,
                                color =
                                    MaterialTheme.colorScheme.primary
                                        .copy(alpha = 0.34f)
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "💾",
                                    style = KmiTypography.cardTitle
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = examTr(
                                isEnglish,
                                "מבחן שמור נמצא",
                                "Saved exam found"
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = KmiTypography.screenTitle.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = examTr(
                                isEnglish,
                                "נמצא מבחן שמור מהפעם האחרונה.\nלהמשיך ממנו או להתחיל מבחן חדש?",
                                "A saved exam was found from the last session.\nContinue from it or start a new exam?"
                            ),
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            style = KmiTypography.body.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PremiumDialogActionButton(
                                modifier = Modifier.weight(1f),
                                text = examTr(isEnglish, "המשך", "Continue"),
                                icon = "▶",
                                startColor = Color(0xFF4F46E5),
                                centerColor = Color(0xFF7C3AED),
                                endColor = Color(0xFF9333EA),
                                onClick = {
                                    // ✅ המשך מבחן אחרון
                                    marksMap.clear()
                                    marksMap.putAll(pendingLoadedDraft)
                                    hasUnsavedChanges = false
                                    showResumeDialog = false
                                }
                            )

                            PremiumDialogActionButton(
                                modifier = Modifier.weight(1f),
                                text = examTr(isEnglish, "מבחן חדש", "New exam"),
                                icon = "✨",
                                startColor = Color(0xFF0EA5E9),
                                centerColor = Color(0xFF2563EB),
                                endColor = Color(0xFF7C3AED),
                                onClick = {
                                    // ✅ מבחן חדש מקומי במסך — לא מוחקים מהשרת כאן
                                    marksMap.clear()

                                    onTraineeNameChange("")
                                    showTraineeNameBox = true
                                    resumeCheckedKey = null

                                    hasUnsavedChanges = false
                                    showResumeDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showExitDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text(examTr(isEnglish, "שמירת מבחן", "Save exam")) },
                text = {
                    Text(
                        examTr(
                            isEnglish,
                            "האם ברצונך לשמור את המבחן לפני היציאה?",
                            "Would you like to save the exam before leaving?"
                        )
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val name = traineeName.trim()
                        if (name.isNotBlank()) {
                            saveExamDraft(ctx, name, belt, marksMap)
                            pushRecentTrainee(ctx, name)
                            saveLastTrainee(ctx, name)
                        }
                        hasUnsavedChanges = false
                        showExitDialog = false
                        onBack()
                    }) { Text(examTr(isEnglish, "שמור", "Save")) }
                },
                dismissButton = {
                    Button(onClick = {
                        showExitDialog = false
                        onBack()
                    }) { Text(examTr(isEnglish, "צא בלי לשמור", "Exit without saving")) }
                }
            )
        }
    }
}

@Composable
private fun TopicHeader(
    title: String,
    expanded: Boolean,
    hasSubTopics: Boolean,
    exerciseCount: Int,
    subTopicCount: Int,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val cardShape = RoundedCornerShape(15.dp)

    Surface(
        shape = cardShape,
        color =
            MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (expanded) 0.96f else 0.82f
            ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 0.75.dp,
            color =
                if (expanded) {
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.42f
                    )
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.52f
                    )
                }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color =
                    if (expanded) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.size(23.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (expanded) "▲" else "▼",
                        color = Color.White,
                        style = KmiTypography.caption.copy(
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color =
                        MaterialTheme.colorScheme.onSurface,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.Black
                    ),
                    textAlign = textAlign,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(1.dp))

                Text(
                    text = if (hasSubTopics) {
                        examTr(
                            isEnglish,
                            "$subTopicCount תתי נושאים • $exerciseCount תרגילים",
                            "$subTopicCount sub-topics • $exerciseCount exercises"
                        )
                    } else {
                        examTr(
                            isEnglish,
                            "$exerciseCount תרגילים",
                            "$exerciseCount exercises"
                        )
                    },
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SubTopicHeader(
    title: String,
    expanded: Boolean,
    exerciseCount: Int,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val cardShape = RoundedCornerShape(20.dp)

    Surface(
        shape = cardShape,
        color = Color.Transparent,
        shadowElevation = 0.dp,
        border = null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (expanded) {
                    Color(0xFF0F5E9C)
                } else {
                    Color(0xFFBFD0E8)
                },
                modifier = Modifier
                    .width(4.dp)
                    .height(26.dp)
            ) {}

            Spacer(Modifier.width(9.dp))

            Surface(
                shape = CircleShape,
                color =
                    if (expanded) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.size(22.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (expanded) "−" else "+",
                        color = Color.White,
                        style = KmiTypography.caption.copy(
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color =
                        MaterialTheme.colorScheme.onSurface,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = textAlign,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = examTr(
                        isEnglish,
                        "תת נושא • $exerciseCount תרגילים",
                        "Sub-topic • $exerciseCount exercises"
                    ),
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalExamEntryScreen(
    @Suppress("UNUSED_PARAMETER")
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    val ctx = LocalContext.current
    val isEnglish = rememberIsEnglish()
    val demoPrivacyEnabled =
        DemoPrivacy.isEnabled()

    fun demoSafeTraineeName(
        realName: String,
        demoIndex: Int? = null
    ): String {
        val cleanName = realName.trim()

        if (!demoPrivacyEnabled) {
            return cleanName
        }

        return TraineeDisplayNameMapper.displayName(
            realName = cleanName,
            stableKey =
                internalExamTraineeKey(cleanName),
            demoIndex = demoIndex,
            isEnglish = isEnglish
        )
    }

    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var traineeName by rememberSaveable { mutableStateOf("") }
    var currentBelt by remember { mutableStateOf(Belt.YELLOW) }

    var recentTrainees by remember { mutableStateOf<List<String>>(emptyList()) }
    var recentCompletedResults by remember {
        mutableStateOf<List<RecentInternalExamResultUi>>(
            emptyList()
        )
    }
    var showExamHistoryDialog by remember { mutableStateOf(false) }
    var examHistoryResultToDelete by remember { mutableStateOf<RecentInternalExamResultUi?>(null) }
    var isDeletingExamHistoryResult by remember { mutableStateOf(false) }
    var isLoadingCompletedPreview by remember { mutableStateOf(false) }
    var completedPreviewSession by remember { mutableStateOf<InternalExamSession?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var traineeToDelete by remember { mutableStateOf<String?>(null) }
    var isDeletingTrainee by remember { mutableStateOf(false) }

    val traineeFocusRequester = remember { FocusRequester() }
    var allowTraineeKeyboard by rememberSaveable { mutableStateOf(false) }

    var examStarted by rememberSaveable {
        mutableStateOf(false)
    }

    var traineeSessionKey by rememberSaveable {
        mutableIntStateOf(0)
    }

    LaunchedEffect(allowTraineeKeyboard, expanded) {
        if (allowTraineeKeyboard && !expanded) {
            traineeFocusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // ✅ ציונים משותפים לכל החגורות באותו מבחן
    val marksMap = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        // לא בוחרים נבחן אוטומטית.
        // המשתמש צריך לבחור נבחן מהרשימה או ללחוץ על "נבחן חדש".
        recentTrainees = loadRecentTrainees(ctx)
        recentCompletedResults = loadRecentCompletedExamResults(limit = 20)
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            recentTrainees = loadRecentTrainees(ctx)
        }
    }

    val exercises = remember(currentBelt) {
        buildInternalExamExercisesFromContent(currentBelt)
    }

    val hasExamProgress = marksMap.isNotEmpty()

    val onExportEntryPdf: () -> Unit = {
        val cleanName = traineeName.trim()

        if (cleanName.isBlank()) {
            Toast.makeText(
                ctx,
                examTr(
                    isEnglish,
                    "נא לבחור או להזין שם נבחן לפני שיתוף",
                    "Please select or enter a trainee name before sharing"
                ),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val pdfSession =
                buildInternalExamSessionForUi(
                    traineeName = cleanName,
                    belt = currentBelt,
                    marksMap = marksMap
                )

            val uri =
                InternalExamPdf.createPdf(
                    context = ctx,
                    session = pdfSession,
                    isEnglish = isEnglish,
                    demoIndex =
                        recentTrainees
                            .indexOfFirst {
                                it.trim().equals(
                                    pdfSession.traineeName.trim(),
                                    ignoreCase = true
                                )
                            }
                            .takeIf {
                                it >= 0
                            }
                            ?.plus(1)
                )

            if (uri != null) {
                InternalExamPdf.sharePdf(
                    context = ctx,
                    uri = uri,
                    isEnglish = isEnglish
                )
            } else {
                Toast.makeText(
                    ctx,
                    examTr(
                        isEnglish,
                        "שגיאה ביצירת קובץ PDF",
                        "Error creating PDF"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    traineeToDelete?.let { nameToDelete ->

        val displayNameToDelete =
            demoSafeTraineeName(
                realName = nameToDelete,
                demoIndex =
                    recentTrainees
                        .indexOfFirst {
                            it.trim().equals(
                                nameToDelete.trim(),
                                ignoreCase = true
                            )
                        }
                        .takeIf {
                            it >= 0
                        }
                        ?.plus(1)
            )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                if (!isDeletingTrainee) {
                    traineeToDelete = null
                }
            },
            title = {
                Text(
                    text = examTr(
                        isEnglish,
                        "מחיקת נבחן",
                        "Delete trainee"
                    )
                )
            },
            text = {
                Text(
                    text = examTr(
                        isEnglish,
                        "האם למחוק את \"$displayNameToDelete\" ואת כל המבחנים/טיוטות שלו?",
                        "Delete \"$displayNameToDelete\" and all of this trainee's exams/drafts?"
                    )
                )
            },
            confirmButton = {
                Button(
                    enabled = !isDeletingTrainee,
                    onClick = {
                        scope.launch {
                            isDeletingTrainee = true

                            runCatching {
                                deleteTraineeAndExamHistory(nameToDelete)
                            }.onSuccess {
                                if (traineeName.trim()
                                        .equals(nameToDelete.trim(), ignoreCase = true)
                                ) {
                                    traineeName = ""
                                    marksMap.clear()
                                    traineeSessionKey++
                                }

                                recentTrainees = loadRecentTrainees(ctx)
                                recentCompletedResults = loadRecentCompletedExamResults(limit = 20)
                                completedPreviewSession = null
                                expanded = false
                                allowTraineeKeyboard = false
                                traineeToDelete = null

                                Toast.makeText(
                                    ctx,
                                    examTr(
                                        isEnglish,
                                        "הנבחן והמבחנים שלו נמחקו",
                                        "The trainee and exam history were deleted"
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    ctx,
                                    examTr(
                                        isEnglish,
                                        "מחיקת הנבחן נכשלה",
                                        "Deleting the trainee failed"
                                    ) + ": ${error.localizedMessage ?: ""}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            isDeletingTrainee = false
                        }
                    }
                ) {
                    Text(
                        text = if (isDeletingTrainee) {
                            examTr(isEnglish, "מוחק...", "Deleting...")
                        } else {
                            examTr(isEnglish, "מחק", "Delete")
                        }
                    )
                }
            },
            dismissButton = {
                Button(
                    enabled = !isDeletingTrainee,
                    onClick = { traineeToDelete = null }
                ) {
                    Text(
                        text = examTr(
                            isEnglish,
                            "ביטול",
                            "Cancel"
                        )
                    )
                }
            }
        )
    }

    examHistoryResultToDelete?.let { resultToDelete ->

        val displayResultTraineeName =
            if (DemoPrivacy.isEnabled()) {
                TraineeDisplayNameMapper.displayName(
                    realName =
                        resultToDelete.traineeName,
                    stableKey =
                        resultToDelete.resultId,
                    demoIndex =
                        recentCompletedResults
                            .indexOfFirst {
                                it.resultId ==
                                        resultToDelete.resultId
                            }
                            .takeIf {
                                it >= 0
                            }
                            ?.plus(1),
                    isEnglish = isEnglish
                )
            } else {
                resultToDelete.traineeName
            }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                if (!isDeletingExamHistoryResult) {
                    examHistoryResultToDelete = null
                }
            },
            title = {
                Text(
                    text = examTr(
                        isEnglish,
                        "מחיקת מבחן מההיסטוריה",
                        "Delete exam from history"
                    )
                )
            },
            text = {
                Text(
                    text = examTr(
                        isEnglish,
                        "האם למחוק את המבחן של \"$displayResultTraineeName\" מהיסטוריית המבחנים?\nהמחיקה היא סופית ולא תשפיע על מבחנים אחרים.",
                        "Delete \"$displayResultTraineeName\" from the exam history?\nThis action is final and will not affect other exams."
                    )
                )
            },
            confirmButton = {
                Button(
                    enabled = !isDeletingExamHistoryResult,
                    onClick = {
                        scope.launch {
                            isDeletingExamHistoryResult = true

                            runCatching {
                                deleteCompletedInternalExamResult(
                                    resultId = resultToDelete.resultId
                                )
                            }.onSuccess {
                                recentCompletedResults = loadRecentCompletedExamResults(limit = 20)
                                completedPreviewSession = null
                                examHistoryResultToDelete = null

                                Toast.makeText(
                                    ctx,
                                    examTr(
                                        isEnglish,
                                        "המבחן נמחק מהיסטוריית המבחנים",
                                        "The exam was deleted from history"
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    ctx,
                                    examTr(
                                        isEnglish,
                                        "מחיקת המבחן נכשלה",
                                        "Deleting the exam failed"
                                    ) + ": ${error.localizedMessage ?: ""}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            isDeletingExamHistoryResult = false
                        }
                    }
                ) {
                    Text(
                        text = if (isDeletingExamHistoryResult) {
                            examTr(isEnglish, "מוחק...", "Deleting...")
                        } else {
                            examTr(isEnglish, "מחק", "Delete")
                        }
                    )
                }
            },
            dismissButton = {
                Button(
                    enabled = !isDeletingExamHistoryResult,
                    onClick = { examHistoryResultToDelete = null }
                ) {
                    Text(examTr(isEnglish, "ביטול", "Cancel"))
                }
            }
        )
    }

    if (!examStarted) {
        Scaffold(
            topBar = {
                KmiTopBar(
                    title = examTr(isEnglish, "מבחן פנימי", "Internal exam"),
                    showMenu = true,
                    showBottomActions = true,
                    showRoleStatus = true,
                    showModePill = true,
                    showTopHome = false,
                    showTopSearch = true,
                    showSettings = true,
                    showTopShare = true,
                    onShare = onExportEntryPdf,
                    centerTitle = true,
                    onHome = onHome
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(
                        brush =
                            if (isDarkMode) {
                                androidx.compose.ui.graphics.Brush
                                    .verticalGradient(
                                        colors = listOf(
                                            MaterialTheme
                                                .colorScheme
                                                .background,
                                            MaterialTheme
                                                .colorScheme
                                                .surface,
                                            Color(0xFF10243A),
                                            Color(0xFF07365B),
                                            Color(0xFF031B31)
                                        )
                                    )
                            } else {
                                internalExamEntryScreenBrush()
                            }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            start = 18.dp,
                            top = 10.dp,
                            end = 18.dp,
                            bottom = 28.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    InternalExamEntryHeroCard(
                        isEnglish = isEnglish,
                        belt = currentBelt
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color =
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = 0.96f
                            ),
                        border = BorderStroke(
                            width = 0.75.dp,
                            color =
                                MaterialTheme.colorScheme
                                    .outlineVariant.copy(
                                        alpha =
                                            if (isDarkMode) {
                                                0.58f
                                            } else {
                                                0.44f
                                            }
                                    )
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush =
                                        androidx.compose.ui.graphics.Brush
                                            .verticalGradient(
                                                colors =
                                                    if (isDarkMode) {
                                                        listOf(
                                                            MaterialTheme
                                                                .colorScheme
                                                                .surface,
                                                            MaterialTheme
                                                                .colorScheme
                                                                .surfaceVariant
                                                                .copy(
                                                                    alpha =
                                                                        0.92f
                                                                ),
                                                            examBeltDarkColor(
                                                                currentBelt
                                                            ).copy(
                                                                alpha =
                                                                    0.22f
                                                            )
                                                        )
                                                    } else {
                                                        listOf(
                                                            MaterialTheme
                                                                .colorScheme
                                                                .surface,
                                                            MaterialTheme
                                                                .colorScheme
                                                                .surfaceVariant
                                                                .copy(
                                                                    alpha =
                                                                        0.58f
                                                                ),
                                                            examBeltSoftColor(
                                                                currentBelt
                                                            ).copy(
                                                                alpha =
                                                                    0.34f
                                                            )
                                                        )
                                                    }
                                            )
                                )
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 18.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(14.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { shouldExpand ->
                                    expanded = shouldExpand

                                    if (shouldExpand) {
                                        allowTraineeKeyboard = false
                                        keyboard?.hide()
                                        focusManager.clearFocus(force = true)

                                        scope.launch {
                                            recentTrainees = loadRecentTrainees(ctx)
                                            recentCompletedResults =
                                                loadRecentCompletedExamResults(limit = 20)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = traineeName,
                                    onValueChange = { value ->
                                        if (allowTraineeKeyboard) {
                                            traineeName = value
                                        }
                                    },
                                    readOnly = !allowTraineeKeyboard,
                                    modifier = Modifier
                                        .menuAnchor(
                                            type =
                                                MenuAnchorType
                                                    .PrimaryNotEditable,
                                            enabled = true
                                        )
                                        .focusRequester(
                                            traineeFocusRequester
                                        )
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .clickable {
                                            allowTraineeKeyboard = false
                                            keyboard?.hide()
                                            focusManager.clearFocus(force = true)
                                            expanded = true

                                            scope.launch {
                                                recentTrainees = loadRecentTrainees(ctx)
                                            }
                                        },
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    label = {
                                        Text(
                                            text = examTr(
                                                isEnglish,
                                                "שם הנבחן",
                                                "Trainee name"
                                            ),
                                            style = KmiTypography.caption.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            text = examTr(
                                                isEnglish,
                                                "בחר נבחן מהרשימה",
                                                "Select a trainee from the list"
                                            ),
                                            color =
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant,
                                            style =
                                                KmiTypography.secondary.copy(
                                                    fontWeight =
                                                        FontWeight.SemiBold
                                                ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                    },
                                    colors =
                                        OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor =
                                                MaterialTheme.colorScheme
                                                    .primary.copy(
                                                        alpha = 0.82f
                                                    ),
                                            unfocusedBorderColor =
                                                MaterialTheme.colorScheme
                                                    .outlineVariant.copy(
                                                        alpha = 0.62f
                                                    ),
                                            focusedTextColor =
                                                MaterialTheme.colorScheme
                                                    .onSurface,
                                            unfocusedTextColor =
                                                MaterialTheme.colorScheme
                                                    .onSurface,
                                            focusedLabelColor =
                                                MaterialTheme.colorScheme
                                                    .primary,
                                            unfocusedLabelColor =
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant,
                                            cursorColor =
                                                MaterialTheme.colorScheme
                                                    .primary,
                                            focusedContainerColor =
                                                MaterialTheme.colorScheme
                                                    .surface,
                                            unfocusedContainerColor =
                                                MaterialTheme.colorScheme
                                                    .surface,
                                            disabledContainerColor =
                                                MaterialTheme.colorScheme
                                                    .surfaceVariant,
                                            errorContainerColor =
                                                MaterialTheme.colorScheme
                                                    .surface
                                        ),
                                    textStyle =
                                        KmiTypography.cardTitle.copy(
                                            fontWeight =
                                                FontWeight.ExtraBold,
                                            color =
                                                MaterialTheme.colorScheme
                                                    .onSurface,
                                            textAlign =
                                                if (isEnglish) {
                                                    TextAlign.Left
                                                } else {
                                                    TextAlign.Right
                                                }
                                        )
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = {
                                        expanded = false
                                        keyboard?.hide()
                                        focusManager.clearFocus(force = true)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 320.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface
                                        )
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isEnglish) {
                                                    "➕ New trainee…"
                                                } else {
                                                    "➕ נבחן חדש…"
                                                },
                                                style =
                                                    KmiTypography.action.copy(
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    ),
                                                color =
                                                    MaterialTheme.colorScheme
                                                        .onSurface,
                                                maxLines = 1,
                                                overflow =
                                                    TextOverflow.Ellipsis,
                                                modifier =
                                                    Modifier.fillMaxWidth(),
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Left
                                                    } else {
                                                        TextAlign.Right
                                                    }
                                            )
                                        },
                                        onClick = {
                                            expanded = false
                                            allowTraineeKeyboard = true
                                            traineeName = ""
                                            marksMap.clear()
                                            traineeSessionKey++
                                        }
                                    )

                                    if (recentTrainees.isNotEmpty()) {
                                        HorizontalDivider()
                                    }

                                    recentTrainees
                                        .take(20)
                                        .forEachIndexed { index,
                                                          name ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            10.dp
                                                        )
                                                    ) {
                                                        if (isEnglish) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(30.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFFFEE2E2))
                                                                    .clickable {
                                                                        traineeToDelete = name
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "🗑",
                                                                    style =
                                                                        KmiTypography.action
                                                                )
                                                            }
                                                        }

                                                        Text(
                                                            text =
                                                                demoSafeTraineeName(
                                                                    realName = name,
                                                                    demoIndex =
                                                                        index + 1
                                                                ),
                                                            style =
                                                                KmiTypography.body.copy(
                                                                    fontWeight =
                                                                        FontWeight.SemiBold
                                                                ),
                                                            color =
                                                                MaterialTheme.colorScheme
                                                                    .onSurface,
                                                            maxLines = 1,
                                                            overflow =
                                                                TextOverflow.Ellipsis,
                                                            modifier =
                                                                Modifier.weight(1f),
                                                            textAlign =
                                                                if (isEnglish) {
                                                                    TextAlign.Left
                                                                } else {
                                                                    TextAlign.Right
                                                                }
                                                        )

                                                        if (!isEnglish) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(30.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFFFEE2E2))
                                                                    .clickable {
                                                                        traineeToDelete = name
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "🗑",
                                                                    style =
                                                                        KmiTypography.action
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    expanded = false
                                                    allowTraineeKeyboard = false
                                                    keyboard?.hide()
                                                    focusManager.clearFocus(force = true)

                                                    val cleanName = name.trim()
                                                    traineeName = cleanName

                                                    scope.launch {
                                                        val savedDraft = loadExamDraft(
                                                            ctx,
                                                            cleanName,
                                                            currentBelt
                                                        )

                                                        marksMap.clear()
                                                        if (savedDraft.isNotEmpty()) {
                                                            marksMap.putAll(savedDraft)
                                                        }

                                                        saveLastTrainee(ctx, cleanName)
                                                        recentTrainees = loadRecentTrainees(ctx)

                                                        traineeSessionKey++
                                                    }
                                                }
                                            )
                                        }
                                }
                            }

                            BeltSelector(
                                currentBelt = currentBelt,
                                isEnglish = isEnglish,
                                onBeltChange = { newBelt ->
                                    currentBelt = newBelt
                                }
                            )

                            InternalExamEntryMetaRow(
                                exercisesCount =
                                    exercises.size,
                                currentBelt =
                                    currentBelt,
                                isEnglish =
                                    isEnglish
                            )

                            PremiumExamSetupButton(
                                text = if (hasExamProgress) {
                                    examTr(isEnglish, "המשך מבחן", "Continue exam")
                                } else {
                                    examTr(isEnglish, "התחל מבחן", "Start exam")
                                },
                                icon = if (hasExamProgress) {
                                    "⏩"
                                } else {
                                    "▶"
                                },
                                centerColor =
                                    examBeltMainColor(currentBelt),
                                endColor = Color(0xFF7C3AED),
                                onClick = {
                                    val cleanName = traineeName.trim()

                                    if (cleanName.isBlank()) {
                                        Toast.makeText(
                                            ctx,
                                            examTr(
                                                isEnglish,
                                                "בחר נבחן מהרשימה או לחץ על נבחן חדש",
                                                "Select a trainee from the list or tap New trainee"
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        traineeName = cleanName

                                        scope.launch {
                                            val savedDraft =
                                                loadExamDraft(ctx, cleanName, currentBelt)

                                            if (savedDraft.isNotEmpty()) {
                                                marksMap.clear()
                                                marksMap.putAll(savedDraft)
                                            }

                                            pushRecentTrainee(ctx, cleanName)
                                            saveLastTrainee(ctx, cleanName)
                                            recentTrainees = loadRecentTrainees(ctx)

                                            traineeSessionKey++
                                            examStarted = true
                                        }
                                    }
                                }
                            )

                            BottomActionBar(
                                isEnglish = isEnglish,
                                entryScreenStyle = true,
                                onSave = {
                                    val cleanName = traineeName.trim()

                                    if (cleanName.isBlank()) {
                                        Toast.makeText(
                                            ctx,
                                            examTr(
                                                isEnglish,
                                                "נא להזין שם נבחן לפני שמירה",
                                                "Please enter a trainee name before saving"
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        traineeName = cleanName

                                        scope.launch {
                                            runCatching {
                                                saveExamDraftAwait(
                                                    context = ctx,
                                                    traineeName = cleanName,
                                                    belt = currentBelt,
                                                    marksMap = marksMap
                                                )
                                            }.onSuccess {
                                                recentTrainees = loadRecentTrainees(ctx)

                                                Toast.makeText(
                                                    ctx,
                                                    examTr(
                                                        isEnglish,
                                                        "המבחן נשמר להמשך",
                                                        "Exam saved for later"
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }.onFailure { error ->
                                                Toast.makeText(
                                                    ctx,
                                                    examTr(
                                                        isEnglish,
                                                        "שמירת המבחן נכשלה",
                                                        "Saving the exam failed"
                                                    ) +
                                                            ": ${error.localizedMessage ?: ""}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    PremiumExamArchiveRow(
                        isEnglish = isEnglish,
                        onClick = {
                            scope.launch {
                                recentCompletedResults =
                                    loadRecentCompletedExamResults(
                                        limit = 20
                                    )

                                showExamHistoryDialog = true
                            }
                        }
                    )
                }
            }
        }

        if (showExamHistoryDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showExamHistoryDialog = false }
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 0.75.dp,
                        color =
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.32f
                            )
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush =
                                    androidx.compose.ui.graphics.Brush
                                        .verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme
                                                    .surface,
                                                MaterialTheme.colorScheme
                                                    .surfaceVariant.copy(
                                                        alpha = 0.72f
                                                    ),
                                                examBeltMainColor(
                                                    currentBelt
                                                ).copy(
                                                    alpha = 0.10f
                                                ),
                                                MaterialTheme.colorScheme
                                                    .surface
                                            )
                                        )
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = examTr(
                                    isEnglish,
                                    "היסטוריית מבחנים",
                                    "Exam history"
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Left
                                    } else {
                                        TextAlign.Right
                                    },
                                color =
                                    MaterialTheme.colorScheme.onSurface,
                                style = KmiTypography.sectionTitle.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                onClick = {
                                    showExamHistoryDialog = false
                                },
                                shape = CircleShape,
                                color =
                                    MaterialTheme.colorScheme
                                        .surfaceVariant,
                                border = BorderStroke(
                                    width = 0.75.dp,
                                    color =
                                        MaterialTheme.colorScheme
                                            .outlineVariant
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "×",
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant,
                                        style =
                                            KmiTypography.sectionTitle.copy(
                                                fontWeight =
                                                    FontWeight.Black
                                            )
                                    )
                                }
                            }
                        }

                        Text(
                            text = examTr(
                                isEnglish,
                                "כאן מופיעים מבחנים שהסתיימו ונשמרו כתוצאה סופית.",
                                "Completed exams saved as final results appear here."
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Left
                                } else {
                                    TextAlign.Right
                                },
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            style = KmiTypography.secondary.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        HorizontalDivider(
                            color =
                                MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.70f
                                )
                        )

                        if (recentCompletedResults.isEmpty()) {
                            Text(
                                text = examTr(
                                    isEnglish,
                                    "אין עדיין מבחנים שהושלמו.",
                                    "No completed exams yet."
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                textAlign = TextAlign.Center,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                                style = KmiTypography.body.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(
                                    items = recentCompletedResults.take(20),
                                    key = { _, result ->
                                        result.resultId
                                    }
                                ) { index, result ->
                                    CompletedExamHistoryRow(
                                        result = result,
                                        displayTraineeName =
                                            TraineeDisplayNameMapper.displayName(
                                                realName = result.traineeName,
                                                stableKey = result.resultId,
                                                demoIndex = index + 1,
                                                isEnglish = isEnglish
                                            ),
                                        isEnglish = isEnglish,
                                        currentBelt = currentBelt,
                                        onDeleteClick = {
                                            examHistoryResultToDelete = result
                                        },
                                        onClick = {
                                            if (isLoadingCompletedPreview) {
                                                return@CompletedExamHistoryRow
                                            }

                                            scope.launch {
                                                isLoadingCompletedPreview = true

                                                runCatching {
                                                    loadCompletedInternalExamSessionForPdf(
                                                        resultId = result.resultId
                                                    ) ?: error("Missing completed exam data")
                                                }.onSuccess { completedSession ->
                                                    showExamHistoryDialog = false
                                                    completedPreviewSession = completedSession
                                                }.onFailure { error ->
                                                    Toast.makeText(
                                                        ctx,
                                                        examTr(
                                                            isEnglish,
                                                            "פתיחת המבחן מההיסטוריה נכשלה",
                                                            "Opening the exam from history failed"
                                                        ) + ": ${error.localizedMessage ?: ""}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }

                                                isLoadingCompletedPreview = false
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

        completedPreviewSession?.let { previewSession ->

            val previewDemoIndex =
                recentTrainees
                    .indexOfFirst {
                        it.trim().equals(
                            previewSession.traineeName.trim(),
                            ignoreCase = true
                        )
                    }
                    .takeIf {
                        it >= 0
                    }
                    ?.plus(1)
                    ?: recentCompletedResults
                        .indexOfFirst {
                            it.traineeName.trim().equals(
                                previewSession.traineeName.trim(),
                                ignoreCase = true
                            )
                        }
                        .takeIf {
                            it >= 0
                        }
                        ?.plus(1)

            CompletedExamPreviewDialog(
                session = previewSession,
                isEnglish = isEnglish,
                currentBelt = currentBelt,
                demoIndex = previewDemoIndex,
                onDismiss = {
                    completedPreviewSession = null
                },
                onSharePdf = {
                    val uri =
                        InternalExamPdf.createPdf(
                            context = ctx,
                            session = previewSession,
                            isEnglish = isEnglish,
                            demoIndex = previewDemoIndex
                        )

                    if (uri != null) {
                        InternalExamPdf.sharePdf(
                            context = ctx,
                            uri = uri,
                            isEnglish = isEnglish
                        )
                    } else {
                        Toast.makeText(
                            ctx,
                            examTr(
                                isEnglish,
                                "שגיאה ביצירת PDF",
                                "Error creating PDF"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    } else {
        key(traineeSessionKey) {
            InternalExamScreen(
                traineeName = traineeName,
                onTraineeNameChange = { traineeName = it },
                belt = currentBelt,
                exercises = exercises,
                onBeltChange = { newBelt -> currentBelt = newBelt },
                onBack = {
                    examStarted = false

                    scope.launch {
                        recentTrainees = loadRecentTrainees(ctx)
                        recentCompletedResults =
                            loadRecentCompletedExamResults(
                                limit = 20
                            )
                    }
                },
                onExportPdf = { pdfSession ->
                    val demoIndex =
                        recentTrainees
                            .indexOfFirst {
                                it.trim().equals(
                                    pdfSession
                                        .traineeName
                                        .trim(),
                                    ignoreCase = true
                                )
                            }
                            .takeIf {
                                it >= 0
                            }
                            ?.plus(1)

                    val uri =
                        InternalExamPdf.createPdf(
                            context = ctx,
                            session = pdfSession,
                            isEnglish = isEnglish,
                            demoIndex = demoIndex
                        )

                    if (uri != null) {
                        InternalExamPdf.sharePdf(
                            context = ctx,
                            uri = uri,
                            isEnglish = isEnglish
                        )
                    } else {
                        Toast.makeText(
                            ctx,
                            examTr(
                                isEnglish,
                                "שגיאה ביצירת קובץ PDF",
                                "Error creating PDF"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                sharedMarksMap = marksMap,
                showSetupHeader = false
            )
        }
    }
}

@Composable
private fun CompletedExamPreviewDialog(
    session: InternalExamSession,
    isEnglish: Boolean,
    currentBelt: Belt,
    demoIndex: Int?,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit
) {
    val context = LocalContext.current

    val score10 = if (session.maxScore == 0.0) {
        0.0
    } else {
        (session.totalScore / session.maxScore) * 10.0
    }

    val answeredCount =
        session.marks.count {
            it != null
        }

    val dateText =
        session.date.format(
            DateTimeFormatter.ofPattern(
                "dd.MM.yyyy"
            )
        )

    val displayTraineeName =
        if (DemoPrivacy.isEnabled()) {
            TraineeDisplayNameMapper.displayName(
                realName =
                    session.traineeName,
                stableKey =
                    internalExamTraineeKey(
                        session.traineeName
                    ),
                demoIndex = demoIndex,
                isEnglish = isEnglish
            )
        } else {
            session.traineeName
        }

    val textAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val horizontalAlignment =
        if (isEnglish) {
            Alignment.Start
        } else {
            Alignment.End
        }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 0.75.dp,
                color =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.32f
                    )
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush =
                            androidx.compose.ui.graphics.Brush
                                .verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme
                                            .surfaceVariant.copy(
                                                alpha = 0.74f
                                            ),
                                        examBeltMainColor(
                                            currentBelt
                                        ).copy(
                                            alpha = 0.10f
                                        ),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = horizontalAlignment
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isEnglish) {
                        CompletedExamPreviewPercentBadge(
                            percent = session.percent,
                            currentBelt = currentBelt
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = horizontalAlignment,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = examTr(
                                isEnglish,
                                "תצוגת מבחן שהושלם",
                                "Completed exam preview"
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = textAlign,
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            style = KmiTypography.sectionTitle.copy(
                                fontWeight = FontWeight.Black
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = displayTraineeName,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = textAlign,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            style = KmiTypography.cardTitle.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isEnglish) {
                        CompletedExamPreviewPercentBadge(
                            percent = session.percent,
                            currentBelt = currentBelt
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.55f))

                CompletedExamPreviewInfoLine(
                    label = examTr(isEnglish, "חגורה", "Belt"),
                    value = examBeltNameForUi(session.belt, isEnglish),
                    isEnglish = isEnglish
                )

                CompletedExamPreviewInfoLine(
                    label = examTr(isEnglish, "תאריך", "Date"),
                    value = dateText,
                    isEnglish = isEnglish
                )

                CompletedExamPreviewInfoLine(
                    label = examTr(isEnglish, "ציון", "Score"),
                    value = "${
                        score10.coerceIn(0.0, 10.0).toScoreString()
                    } / 10  (${session.percent}%)",
                    isEnglish = isEnglish
                )

                CompletedExamPreviewInfoLine(
                    label = examTr(isEnglish, "תרגילים שנוקדו", "Scored exercises"),
                    value = answeredCount.toString(),
                    isEnglish = isEnglish
                )

                CompletedExamPreviewInfoLine(
                    label = examTr(isEnglish, "סטטוס", "Status"),
                    value = examStatusText(session.percent, isEnglish),
                    isEnglish = isEnglish
                )

                HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.55f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.78f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFCBD5E1)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = examTr(
                                    isEnglish,
                                    "סגור",
                                    "Close"
                                ),
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                                style = KmiTypography.action.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.86f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = examBeltMainColor(currentBelt).copy(alpha = 0.32f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    val summaryText =
                                        buildCompletedExamShareSummary(
                                            session = session,
                                            isEnglish = isEnglish,
                                            demoIndex = demoIndex
                                        )

                                    val clipboard = context.getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                    ) as ClipboardManager

                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText(
                                            examTr(
                                                isEnglish,
                                                "סיכום מבחן פנימי",
                                                "Internal exam summary"
                                            ),
                                            summaryText
                                        )
                                    )

                                    Toast.makeText(
                                        context,
                                        examTr(
                                            isEnglish,
                                            "הסיכום הועתק",
                                            "Summary copied"
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = examTr(
                                    isEnglish,
                                    "העתק",
                                    "Copy"
                                ),
                                color =
                                    examBeltDarkColor(currentBelt),
                                style = KmiTypography.action.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        shadowElevation = 12.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onSharePdf() }
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(
                                            examBeltDarkColor(currentBelt),
                                            examBeltMainColor(currentBelt),
                                            Color(0xFF7C3AED)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = examTr(
                                    isEnglish,
                                    "שתף PDF",
                                    "PDF"
                                ),
                                color = Color.White,
                                style = KmiTypography.action.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedExamPreviewPercentBadge(
    percent: Int,
    currentBelt: Belt
) {
    Surface(
        shape = CircleShape,
        color = examBeltMainColor(currentBelt).copy(alpha = 0.18f),
        border = BorderStroke(
            width = 1.dp,
            color = examBeltDarkColor(currentBelt).copy(alpha = 0.18f)
        ),
        modifier = Modifier.size(58.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$percent%",
                color = examBeltDarkColor(currentBelt),
                style = KmiTypography.cardTitle.copy(
                    fontWeight = FontWeight.Black
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompletedExamPreviewInfoLine(
    label: String,
    value: String,
    isEnglish: Boolean
) {
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            style = KmiTypography.secondary.copy(
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign,
            color = MaterialTheme.colorScheme.onSurface,
            style = KmiTypography.cardTitle.copy(
                fontWeight = FontWeight.Black
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun internalExamResultDateText(completedAtMillis: Long): String {
    if (completedAtMillis <= 0L) return "—"

    return runCatching {
        java.time.Instant.ofEpochMilli(completedAtMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }.getOrDefault("—")
}

@Composable
private fun CompletedExamHistoryRow(
    result: RecentInternalExamResultUi,
    displayTraineeName: String,
    isEnglish: Boolean,
    currentBelt: Belt,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    val dateText = internalExamResultDateText(result.completedAtMillis)
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val historyCardColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            examBeltSoftColor(currentBelt)
                .copy(alpha = 0.78f)
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = historyCardColor,
        contentColor =
            MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 0.75.dp,
            color =
                examBeltMainColor(currentBelt)
                    .copy(
                        alpha =
                            if (isDarkMode) {
                                0.38f
                            } else {
                                0.22f
                            }
                    )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (isEnglish) {
                CompletedExamScoreBubble(
                    percent = result.percent,
                    currentBelt = currentBelt
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                    ) {
                        if (isEnglish) {
                            Text(
                                text = "📄",
                                style = KmiTypography.caption,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 5.dp)
                            )

                            Text(
                                text = displayTraineeName,
                                textAlign = TextAlign.Left,
                                color =
                                    MaterialTheme.colorScheme.onSurface,
                                style = KmiTypography.cardTitle.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "📄",
                                style = KmiTypography.caption,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 5.dp)
                            )

                            Text(
                                text = displayTraineeName,
                                textAlign = TextAlign.Right,
                                color =
                                    MaterialTheme.colorScheme.onSurface,
                                style = KmiTypography.cardTitle.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = result.beltName,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = KmiTypography.secondary.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = examTr(
                        isEnglish,
                        "תאריך: $dateText",
                        "Date: $dateText"
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = KmiTypography.secondary.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = examTr(
                        isEnglish,
                        "ציון: ${
                            result.score10.coerceIn(0.0, 10.0).toScoreString()
                        } / 10  (${result.percent}%)",
                        "Score: ${
                            result.score10.coerceIn(0.0, 10.0).toScoreString()
                        } / 10  (${result.percent}%)"
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = onDeleteClick,
                shape = CircleShape,
                color =
                    MaterialTheme.colorScheme.errorContainer,
                contentColor =
                    MaterialTheme.colorScheme.onErrorContainer,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 0.75.dp,
                    color =
                        MaterialTheme.colorScheme.error.copy(
                            alpha = 0.42f
                        )
                ),
                modifier = Modifier.size(26.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗑",
                        style = KmiTypography.caption,
                        maxLines = 1
                    )
                }
            }

            if (!isEnglish) {
                CompletedExamScoreBubble(
                    percent = result.percent,
                    currentBelt = currentBelt
                )
            }
        }
    }
}

@Composable
private fun CompletedExamScoreBubble(
    percent: Int,
    currentBelt: Belt
) {
    Surface(
        shape = CircleShape,
        color = examBeltMainColor(currentBelt).copy(alpha = 0.22f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$percent%",
                color = examBeltDarkColor(currentBelt),
                style = KmiTypography.caption.copy(
                    fontWeight = FontWeight.Black
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InternalExamEntryHeroCard(
    isEnglish: Boolean,
    belt: Belt
) {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val secondaryColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme
                .onSurfaceVariant
        } else {
            Color(0xFF53627A)
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color =
            MaterialTheme.colorScheme.surface.copy(
                alpha = 0.96f
            ),
        border = BorderStroke(
            width = 0.75.dp,
            color =
                MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha =
                        if (isDarkMode) {
                            0.58f
                        } else {
                            0.46f
                        }
                )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush =
                        androidx.compose.ui.graphics.Brush
                            .verticalGradient(
                                colors =
                                    if (isDarkMode) {
                                        listOf(
                                            MaterialTheme
                                                .colorScheme
                                                .surface,
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant,
                                            Color(0xFF172554)
                                                .copy(alpha = 0.52f)
                                        )
                                    } else {
                                        listOf(
                                            MaterialTheme.colorScheme
                                                .surface,
                                            MaterialTheme.colorScheme
                                                .surfaceVariant.copy(
                                                    alpha = 0.64f
                                                ),
                                            examBeltSoftColor(belt)
                                                .copy(alpha = 0.24f)
                                        )
                                    }
                            )
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 18.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            /*
             * סמל הישג הבנוי משתי שכבות,
             * כדי ליצור מראה הדומה למגן.
             */
            Box(
                modifier = Modifier.size(70.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            rotationZ = 45f
                        },
                    shape = RoundedCornerShape(18.dp),
                    color =
                        Color(0xFF7057DC)
                            .copy(alpha = 0.14f),
                    border = BorderStroke(
                        width = 2.dp,
                        color = Color(0xFF7057DC)
                            .copy(alpha = 0.44f)
                    )
                ) {}

                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color =
                        if (isDarkMode) {
                            Color(0xFF312E81)
                        } else {
                            Color(0xFFF1EFFF)
                        },
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFF8B5CF6)
                            .copy(alpha = 0.48f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "★",
                            color = Color(0xFF7057DC),
                            style =
                                KmiTypography.sectionTitle,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Text(
                text = examTr(
                    isEnglish,
                    "בחר נבחן וחגורה לפני תחילת המבחן",
                    "Select a trainee and belt before starting"
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = secondaryColor,
                style = KmiTypography.secondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InternalExamEntryMetaRow(
    exercisesCount: Int,
    currentBelt: Belt,
    isEnglish: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(
                horizontal = 4.dp
            ),
            color = Color(0xFFBCD2ED)
                .copy(alpha = 0.76f)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceEvenly,
            verticalAlignment =
                Alignment.Top
        ) {
            InternalExamEntryMetaItem(
                icon = "▣",
                value =
                    if (isEnglish) {
                        "$exercisesCount questions"
                    } else {
                        "$exercisesCount שאלות"
                    },
                modifier = Modifier.weight(1f)
            )

            InternalExamEntryMetaHorizontalDivider()

            InternalExamEntryMetaItem(
                icon = "◷",
                value =
                    examTr(
                        isEnglish,
                        "ללא הגבלת זמן",
                        "No time limit"
                    ),
                modifier = Modifier.weight(1f)
            )

            InternalExamEntryMetaHorizontalDivider()

            InternalExamEntryMetaItem(
                icon = "🥋",
                value =
                    examBeltNameForUi(
                        currentBelt,
                        isEnglish
                    ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InternalExamEntryMetaItem(
    icon: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(
            horizontal = 4.dp
        ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = Color(0xFFF3F1FF),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFF8B5CF6)
                    .copy(alpha = 0.22f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    color = Color(0xFF6551D9),
                    style = KmiTypography.action.copy(
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }

        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = KmiTypography.caption.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InternalExamEntryMetaHorizontalDivider() {
    Box(
        modifier = Modifier
            .padding(top = 5.dp)
            .width(1.dp)
            .height(56.dp)
            .background(
                Color(0xFFBDD1EA)
                    .copy(alpha = 0.72f)
            )
    )
}

@Composable
private fun PremiumExamArchiveRow(
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(23.dp),
        color = Color(0xFF0F2947)
            .copy(alpha = 0.92f),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFF9A7CFF)
                .copy(alpha = 0.68f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 13.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text =
                    if (isEnglish) {
                        "›"
                    } else {
                        "‹"
                    },
                color = Color(0xFFB8C5D9),
                style = KmiTypography.metric.copy(
                    fontWeight = FontWeight.Light
                )
            )

            Text(
                text = examTr(
                    isEnglish,
                    "ארכיון מבחנים",
                    "Exam archive"
                ),
                modifier = Modifier.weight(1f),
                color = Color.White,
                style = KmiTypography.sectionTitle.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign =
                    if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(
                        alpha = 0.16f
                    )
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📚",
                        style = KmiTypography.sectionTitle
                    )
                }
            }
        }
    }
}

@Composable
private fun BeltSelector(
    currentBelt: Belt,
    isEnglish: Boolean,
    onBeltChange: (Belt) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val belts = listOf(
        Belt.YELLOW,
        Belt.ORANGE,
        Belt.GREEN,
        Belt.BLUE,
        Belt.BROWN,
        Belt.BLACK
    )

    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val mainColor =
        examBeltMainColor(currentBelt)

    val darkColor =
        if (isDarkMode) {
            when (currentBelt) {
                Belt.BLACK ->
                    MaterialTheme.colorScheme.onSurface

                else ->
                    mainColor
            }
        } else {
            examBeltDarkColor(currentBelt)
        }

    val cardColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme
                .surfaceVariant
                .copy(alpha = 0.76f)
        } else {
            Color.White.copy(alpha = 0.76f)
        }

    val imageFrameColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surface
        } else {
            Color(0xFFF8FAFF)
        }

    val secondaryTextColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme
                .onSurfaceVariant
        } else {
            Color(0xFF64748B)
        }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            onClick = {
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp),
            shape = RoundedCornerShape(20.dp),
            color = cardColor,
            border = BorderStroke(
                width = 1.dp,
                color =
                    if (isDarkMode) {
                        MaterialTheme.colorScheme
                            .outline
                            .copy(alpha = 0.48f)
                    } else {
                        mainColor.copy(alpha = 0.24f)
                    }
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                /*
    * החץ בצד של המלל.
    */
                Text(
                    text =
                        if (expanded) {
                            "▲"
                        } else {
                            "▼"
                        },
                    color =
                        if (isDarkMode) {
                            Color(0xFFA78BFA)
                        } else {
                            Color(0xFF7C3AED)
                        },
                    style = KmiTypography.caption,
                    fontWeight = FontWeight.Black
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.Center,
                    horizontalAlignment =
                        if (isEnglish) {
                            Alignment.Start
                        } else {
                            Alignment.End
                        }
                ) {
                    Text(
                        text = examTr(
                            isEnglish,
                            "חגורה נבחרת",
                            "Selected belt"
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        color = secondaryTextColor,
                        style = KmiTypography.caption,
                        fontWeight = FontWeight.SemiBold,
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text =
                            examBeltShortNameForUi(
                                currentBelt,
                                isEnglish
                            ),
                        modifier = Modifier.fillMaxWidth(),
                        color = darkColor,
                        style =
                            KmiTypography.sectionTitle,
                        fontWeight =
                            FontWeight.ExtraBold,
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                /*
                 * תמונת החגורה נמצאת בצד הנגדי
                 * למלל ובתוך מסגרת נפרדת.
                 */
                Surface(
                    modifier = Modifier.size(
                        width = 100.dp,
                        height = 62.dp
                    ),
                    shape = RoundedCornerShape(16.dp),
                    color = imageFrameColor,
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            if (isDarkMode) {
                                MaterialTheme.colorScheme
                                    .outline
                                    .copy(alpha = 0.42f)
                            } else {
                                Color(0xFFC8D8EE)
                            }
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                id =
                                    examBeltDrawableRes(
                                        currentBelt
                                    )
                            ),
                            contentDescription =
                                examBeltNameForUi(
                                    currentBelt,
                                    isEnglish
                                ),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = 13.dp,
                                    vertical = 12.dp
                                ),
                            contentScale =
                                ContentScale.Fit
                        )
                    }
                }
            }
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .background(
                    if (isDarkMode) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color(0xFFF8FAFC)
                    }
                )
        ) {
            belts.forEach { belt ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(
                                    width = 72.dp,
                                    height = 42.dp
                                ),
                                shape =
                                    RoundedCornerShape(12.dp),
                                color =
                                    if (isDarkMode) {
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                    } else {
                                        Color(0xFFF8FAFF)
                                    },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        examBeltMainColor(
                                            belt
                                        ).copy(
                                            alpha = 0.30f
                                        )
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Image(
                                    painter =
                                        painterResource(
                                            id =
                                                examBeltDrawableRes(
                                                    belt
                                                )
                                        ),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            horizontal = 9.dp,
                                            vertical = 8.dp
                                        ),
                                    contentScale =
                                        ContentScale.Fit
                                )
                            }

                            Text(
                                text =
                                    examBeltShortNameForUi(
                                        belt,
                                        isEnglish
                                    ),
                                modifier =
                                    Modifier.weight(1f),
                                color =
                                    if (isDarkMode) {
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                    } else if (
                                        belt == currentBelt
                                    ) {
                                        examBeltDarkColor(
                                            belt
                                        )
                                    } else {
                                        Color(0xFF111827)
                                    },
                                style =
                                    KmiTypography.body,
                                fontWeight =
                                    if (
                                        belt == currentBelt
                                    ) {
                                        FontWeight.ExtraBold
                                    } else {
                                        FontWeight.SemiBold
                                    },
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Left
                                    } else {
                                        TextAlign.Right
                                    },
                                maxLines = 2,
                                overflow =
                                    TextOverflow.Ellipsis
                            )

                            if (belt == currentBelt) {
                                Text(
                                    text = "✓",
                                    color =
                                        examBeltMainColor(
                                            belt
                                        ),
                                    style =
                                        KmiTypography.action,
                                    fontWeight =
                                        FontWeight.Black
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false

                        if (belt != currentBelt) {
                            onBeltChange(belt)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    currentBelt: Belt,
    marksMap: Map<String, Int>,
    isEnglish: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val orderedBelts = beltsUpTo(currentBelt)

    // ניקוד לכל חגורה עד החגורה הנוכחית – רק מתוך תרגילים שסומנו בפועל
    val beltScores: Map<Belt, BeltScore> = orderedBelts.associateWith { belt ->
        val exercisesForBelt = buildInternalExamExercisesFromContent(belt)
        var total = 0.0
        var max = 0.0
        exercisesForBelt.forEach { ex ->
            val score = marksMap[ex.id]
            if (score != null) {
                max += 10.0
                total += clampScore10(score).toDouble()
            }
        }
        BeltScore(total = total, max = max)
    }

    val totalScore = beltScores.values.sumOf { it.total }
    val maxScore = beltScores.values.sumOf { it.max }

    // ✅ מצטבר מנורמל ל-0..10
    val totalScore10: Double = if (maxScore == 0.0) 0.0 else (totalScore / maxScore) * 10.0
    val percent = if (maxScore == 0.0) 0 else ((totalScore / maxScore) * 100.0).toInt()

    val summaryText = examSummaryText(percent, isEnglish)

    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val summaryContainerColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            examBeltSoftColor(currentBelt)
        }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor = summaryContainerColor,
                contentColor =
                    MaterialTheme.colorScheme.onSurface
            ),
        border = BorderStroke(
            width = 0.75.dp,
            color =
                examBeltMainColor(currentBelt)
                    .copy(
                        alpha =
                            if (isDarkMode) {
                                0.42f
                            } else {
                                0.24f
                            }
                    )
        ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                )
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isEnglish) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "סיכום מבחן",
                                style = KmiTypography.cardTitle.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Right
                            )

                            Text(
                                text = if (expanded) "▲" else "▼",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text =
                                "מצטבר: " +
                                        "${totalScore10.coerceIn(0.0, 10.0).toScoreString()} " +
                                        "/ 10  (${percent}%)",
                            modifier = Modifier.fillMaxWidth(),
                            style = KmiTypography.body.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Right
                        )

                        Text(
                            text = summaryText,
                            modifier = Modifier.fillMaxWidth(),
                            style = KmiTypography.secondary,
                            textAlign = TextAlign.Right
                        )
                    }

                    Image(
                        painter = painterResource(id = examBeltDrawableRes(currentBelt)),
                        contentDescription =
                            examBeltNameForUi(
                                currentBelt,
                                false
                            ),
                        modifier = Modifier
                            .width(104.dp)
                            .height(28.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = painterResource(id = examBeltDrawableRes(currentBelt)),
                        contentDescription =
                            examBeltNameForUi(
                                currentBelt,
                                true
                            ),
                        modifier = Modifier
                            .width(104.dp)
                            .height(28.dp),
                        contentScale = ContentScale.Fit
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Exam summary",
                                style = KmiTypography.cardTitle.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Left
                            )

                            Text(
                                text = if (expanded) "▲" else "▼",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text =
                                "Total: " +
                                        "${totalScore10.coerceIn(0.0, 10.0).toScoreString()} " +
                                        "/ 10  (${percent}%)",
                            modifier = Modifier.fillMaxWidth(),
                            style = KmiTypography.body.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Left
                        )

                        Text(
                            text = summaryText,
                            modifier = Modifier.fillMaxWidth(),
                            style = KmiTypography.secondary,
                            textAlign = TextAlign.Left
                        )
                    }
                }
            }

            // ✅ פירוט רק כשפותחים
            if (expanded && beltScores.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                beltScores.forEach { (belt, score) ->
                    Text(
                        text =
                            "${examBeltNameForUi(belt, isEnglish)}: " +
                                    "${score.score10.coerceIn(0.0, 10.0).toScoreString()} " +
                                    "/ 10 (${score.percent}%)",
                        style = KmiTypography.secondary,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseRow(
    name: String,
    score: Int?,                 // ✅ 0..10
    isEnglish: Boolean,
    onScoreChange: (Int?) -> Unit // null = לא סומן
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.96f
                )
        ),
        border = BorderStroke(
            width = 0.75.dp,
            color =
                MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = 0.55f
                )
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            Text(
                text = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                style = KmiTypography.secondary.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign =
                    if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // ✅ ציון 1..10 בשתי שורות קבועות, ממורכז, ללא גלילה לצד
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 6.dp,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                maxItemsInEachRow = 5
            ) {
                for (v in 1..10) {
                    ScoreChip(
                        value = v,
                        selected = score == v
                    ) {
                        onScoreChange(if (score == v) null else v)
                    }
                }
            }
        }
    }
}

/**
 * צבע מדורג לפי score עם עקומה "חזקה":
 * נמוכים אדום חזק, אמצע צהוב ברור, גבוהים ירוק חזק.
 */
private fun scoreColor(value: Int): Color {
    val v = value.coerceIn(0, 10)

    // t 0..1
    val tLinear = v / 10f

    // ✅ curve: מדגיש קצוות (יותר אדום בתחלה, יותר ירוק בסוף)
    // אפשר לשחק עם האקספוננט: 1.0 ליניארי, 1.2 יותר "קפיצה" לירוק בסוף
    val t = tLinear * tLinear  // (t^2)

    // Hue: 0 (אדום) → 120 (ירוק)
    val hue = 120f * t

    // רוויה וערך: נמוכים טיפה כהים יותר כדי להרגיש "אזהרה"
    val sat = 0.90f
    val valBase = 0.92f
    val valueV = (valBase - (0.08f * (1f - tLinear))).coerceIn(0.78f, 0.95f)

    return Color.hsv(
        hue = hue,
        saturation = sat,
        value = valueV
    )
}

@Composable
private fun OutlinedNumberText(
    text: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            color = Color.White,
            style = KmiTypography.caption.copy(
                fontWeight = FontWeight.Bold,
                drawStyle = Stroke(
                    width = 3f
                )
            )
        )

        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            color = Color.Black,
            style = KmiTypography.caption.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black.copy(
                        alpha = 0.24f
                    ),
                    blurRadius = 2f
                )
            )
        )
    }
}

@Composable
private fun ScoreChip(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val base = scoreColor(value)
    val bg = if (selected) base.copy(alpha = 0.95f) else base.copy(alpha = 0.34f)

    Surface(
        modifier = Modifier
            .size(25.dp)
            .clip(RoundedCornerShape(7.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        color = bg,
        border = BorderStroke(
            width = if (selected) 1.4.dp else 0.9.dp,
            color = if (selected) base else base.copy(alpha = 0.82f)
        ),
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        OutlinedNumberText(
            text = value.toString()
        )
    }
}

@Composable
private fun PremiumExamSetupButton(
    text: String,
    icon: String,
    centerColor: Color,
    endColor: Color,
    onClick: () -> Unit
) {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                if (isDarkMode) {
                    Color(0xFFB89CFF)
                        .copy(alpha = 0.52f)
                } else {
                    Color.White.copy(alpha = 0.34f)
                }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable {
                    onClick()
                }
                .background(
                    brush =
                        androidx.compose.ui.graphics.Brush
                            .horizontalGradient(
                                colors = listOf(
                                    Color(0xFF6437C8),
                                    centerColor.copy(
                                        alpha = 0.94f
                                    ),
                                    if (
                                        centerColor ==
                                        Color(0xFFFDE047)
                                    ) {
                                        Color(0xFFFFC642)
                                    } else {
                                        endColor.copy(
                                            alpha = 0.94f
                                        )
                                    }
                                )
                            )
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.White.copy(
                    alpha = 0.17f
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(
                        alpha = 0.30f
                    )
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        color = Color.White,
                        style = KmiTypography.action,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = text,
                color = Color.White,
                style = KmiTypography.sectionTitle,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PremiumDialogActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: String,
    startColor: Color,
    centerColor: Color,
    endColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.heightIn(min = 52.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onClick() }
                .background(
                    brush =
                        androidx.compose.ui.graphics.Brush
                            .horizontalGradient(
                                listOf(
                                    startColor,
                                    centerColor,
                                    endColor
                                )
                            )
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 12.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    style = KmiTypography.action.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = text,
                    color = Color.White,
                    style = KmiTypography.action.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChangeBeltBottomBar(
    isEnglish: Boolean,
    belt: Belt,
    onChangeBelt: () -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = examBeltSoftColor(belt).copy(alpha = 0.58f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 22.dp,
                    vertical = 5.dp
                )
                .heightIn(min = 44.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        onChangeBelt()
                    }
                    .background(
                        brush = examBeltButtonBrush(belt)
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🥋",
                        style = KmiTypography.action
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = examTr(
                            isEnglish,
                            "מעבר לחגורה אחרת",
                            "Change belt"
                        ),
                        color = Color.White,
                        style = KmiTypography.action.copy(
                            fontWeight = FontWeight.Black
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    isEnglish: Boolean,
    isSaving: Boolean = false,
    finishMode: Boolean = false,
    entryScreenStyle: Boolean = false,
    onSave: () -> Unit
) {
    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 2.dp
                )
        ) {
            if (entryScreenStyle) {
                /*
                 * כפתור שמירה משני במסך הכניסה:
                 * רקע כהה, מסגרת סגולה דקה וללא צל.
                 */
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(
                            enabled = !isSaving
                        ) {
                            onSave()
                        },
                    shape = RoundedCornerShape(20.dp),
                    color =
                        if (isDarkMode) {
                            Color(0xFF10182D)
                        } else {
                            Color(0xFF102848)
                                .copy(alpha = 0.96f)
                        },
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            if (isDarkMode) {
                                Color(0xFFA78BFA)
                                    .copy(alpha = 0.82f)
                            } else {
                                Color(0xFF8B5CF6)
                                    .copy(alpha = 0.74f)
                            }
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 18.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(21.dp)
                        )

                        Spacer(Modifier.width(9.dp))

                        Text(
                            text =
                                if (isSaving) {
                                    examTr(
                                        isEnglish,
                                        "שומר...",
                                        "Saving..."
                                    )
                                } else {
                                    examTr(
                                        isEnglish,
                                        "שמור מבחן",
                                        "Save exam"
                                    )
                                },
                            color = Color(0xFFA78BFA),
                            style = KmiTypography.action,
                            fontWeight =
                                FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                /*
                 * כפתור השמירה/סיום בתוך המבחן.
                 * נשאר מלא, אך ללא צל.
                 */
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(
                            RoundedCornerShape(18.dp)
                        )
                        .clickable(
                            enabled = !isSaving
                        ) {
                            onSave()
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFF8B5CF6)
                            .copy(alpha = 0.46f)
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush =
                                    androidx.compose.ui
                                        .graphics.Brush
                                        .horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF5B35D5),
                                                Color(0xFF7C3AED),
                                                Color(0xFF8B5CF6)
                                            )
                                        )
                            )
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                if (isSaving) {
                                    examTr(
                                        isEnglish,
                                        "שומר...",
                                        "Saving..."
                                    )
                                } else if (finishMode) {
                                    examTr(
                                        isEnglish,
                                        "סיום מבחן",
                                        "Finish exam"
                                    )
                                } else {
                                    examTr(
                                        isEnglish,
                                        "שמור",
                                        "Save"
                                    )
                                },
                            color = Color.White,
                            style = KmiTypography.action,
                            fontWeight =
                                FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
//  בניית רשימת תרגילים למבחן פנימי לפי ContentRepo / TopicsScreen
// ============================================================
private fun buildInternalExamExercisesFromContent(belt: Belt): List<ExamExerciseItem> {
    val result = mutableListOf<ExamExerciseItem>()

    // שמות הנושאים לחגורה הזו – כמו ב-TopicsScreen
    val topicTitles: List<String> = runCatching {
        KmiSearchBridge.topicTitlesFor(belt)
    }.getOrDefault(emptyList()).ifEmpty {
        runCatching {
            val sharedBelt =
                Belt.fromId(belt.id)
                    ?: Belt.WHITE

            SubTopicRegistry
                .allForBelt(sharedBelt)
                .keys
                .toList()
        }.getOrDefault(emptyList())
    }

    topicTitles.distinct().forEach { topicTitle ->
        val rawItems = itemsForTopicFlattenInternal(belt, topicTitle)
        if (rawItems.isEmpty()) return@forEach

        val seenNamesInTopic = mutableSetOf<String>()

        rawItems.forEach { rawItem ->
            val cleanName = rawItem
                .substringAfter("::")
                .substringAfter(":")
                .trim()
                .ifBlank { rawItem.trim() }

            val dedupeKey = cleanName
                .replace("־", "-")
                .replace("–", "-")
                .replace("—", "-")
                .replace(Regex("\\s+"), " ")
                .trim()
                .lowercase()

            if (!seenNamesInTopic.add(dedupeKey)) {
                return@forEach
            }

            val subTopicTitle = findSubTopicTitleForItemInternal(belt, topicTitle, cleanName)
                ?.takeIf { it.isNotBlank() && it != topicTitle }

            val stableId = ContentRepo.makeItemKey(
                belt = belt,
                topicTitle = topicTitle,
                subTopicTitle = subTopicTitle,
                itemTitle = cleanName
            )

            result += ExamExerciseItem(
                id = stableId,
                belt = belt,
                topic = topicTitle,
                subTopic = subTopicTitle,
                name = cleanName
            )
        }
    }

    return result.distinctBy { item ->
        item.topic.trim().lowercase() + "|" +
                item.name
                    .replace("־", "-")
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .lowercase()
    }
}

// עזר: שליפה שטוחה של תרגילים לנושא
private fun itemsForTopicFlattenInternal(belt: Belt, topicTitle: String): List<String> {

    // 1) Repo/Bridge החדש (הרשמי)
    val fromRepo: List<String> = runCatching {
        val direct = ContentRepo.listItemTitles(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = null
        )

        val subs = ContentRepo.listSubTopicTitles(belt, topicTitle)
        val viaSubs = subs.flatMap { stTitle ->
            ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = topicTitle,
                subTopicTitle = stTitle
            )
        }

        (direct + viaSubs)
    }.getOrDefault(emptyList())

    if (fromRepo.isNotEmpty()) return fromRepo

    // 2) גשר חיפוש ישן (אם עדיין קיים אצלך)
    val viaSearchBridge = runCatching {
        KmiSearchBridge.itemsFor(belt, topicTitle)
    }.getOrDefault(emptyList())

    if (viaSearchBridge.isNotEmpty()) return viaSearchBridge

    return emptyList()
}

private fun internalExamCoachUid(): String? {
    return FirebaseAuth.getInstance().currentUser?.uid
}

private fun internalExamTraineeKey(name: String): String {
    return name
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9א-ת]+"), "_")
        .trim('_')
        .ifBlank { "unknown_trainee" }
}

private fun internalExamDraftId(
    coachUid: String,
    traineeName: String,
    belt: Belt
): String {
    return "${coachUid}_${belt.name}_${internalExamTraineeKey(traineeName)}"
}

private fun saveExamDraft(
    context: Context,
    traineeName: String,
    belt: Belt,
    marksMap: Map<String, Int>
) {
    val cleanName = traineeName.trim()
    if (cleanName.isBlank()) return

    val coachUid = internalExamCoachUid() ?: return

    val session = buildInternalExamSessionForUi(
        traineeName = cleanName,
        belt = belt,
        marksMap = marksMap
    )

    val safeMarks = marksMap
        .filterKeys { it.isNotBlank() }
        .mapValues { (_, score) -> clampScore10(score) }

    val docId = internalExamDraftId(coachUid, cleanName, belt)

    val data = hashMapOf(
        "examId" to docId,
        "coachUid" to coachUid,
        "traineeName" to cleanName,
        "traineeKey" to internalExamTraineeKey(cleanName),
        "belt" to belt.name,
        "beltHeb" to belt.heb,
        "beltEn" to belt.en,
        "status" to "draft",
        "marks" to safeMarks,
        "totalScore" to session.totalScore,
        "maxScore" to session.maxScore,
        "percent" to session.percent,
        "summaryText" to session.summaryText,
        "summaryTextHe" to examStatusText(session.percent, false),
        "summaryTextEn" to examStatusText(session.percent, true),
        "updatedAtMillis" to System.currentTimeMillis(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    FirebaseFirestore.getInstance()
        .collection("internalExamDrafts")
        .document(docId)
        .set(data, SetOptions.merge())

    pushRecentTrainee(context, cleanName)
    saveLastTrainee(context, cleanName)
}

private suspend fun saveExamDraftAwait(
    context: Context,
    traineeName: String,
    belt: Belt,
    marksMap: Map<String, Int>
) {
    val cleanName = traineeName.trim()
    if (cleanName.isBlank()) return

    val coachUid = internalExamCoachUid()
        ?: error("Missing coach uid")

    val session = buildInternalExamSessionForUi(
        traineeName = cleanName,
        belt = belt,
        marksMap = marksMap
    )

    val safeMarks = marksMap
        .filterKeys { it.isNotBlank() }
        .mapValues { (_, score) -> clampScore10(score) }

    val docId = internalExamDraftId(coachUid, cleanName, belt)

    val data = hashMapOf(
        "examId" to docId,
        "coachUid" to coachUid,
        "traineeName" to cleanName,
        "traineeKey" to internalExamTraineeKey(cleanName),
        "belt" to belt.name,
        "beltHeb" to belt.heb,
        "beltEn" to belt.en,
        "status" to "draft",
        "marks" to safeMarks,
        "totalScore" to session.totalScore,
        "maxScore" to session.maxScore,
        "percent" to session.percent,
        "summaryText" to session.summaryText,
        "summaryTextHe" to examStatusText(session.percent, false),
        "summaryTextEn" to examStatusText(session.percent, true),
        "updatedAtMillis" to System.currentTimeMillis(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    FirebaseFirestore.getInstance()
        .collection("internalExamDrafts")
        .document(docId)
        .set(data, SetOptions.merge())
        .await()

    pushRecentTrainee(context, cleanName)
    saveLastTrainee(context, cleanName)
}

private suspend fun saveCompletedInternalExamResult(
    traineeName: String,
    belt: Belt,
    marksMap: Map<String, Int>
): String {
    val cleanName = traineeName.trim()
    if (cleanName.isBlank()) {
        error("Missing trainee name")
    }

    val coachUid = internalExamCoachUid()
        ?: error("Missing coach uid")

    val session = buildInternalExamSessionForUi(
        traineeName = cleanName,
        belt = belt,
        marksMap = marksMap
    )

    val safeMarks = marksMap
        .filterKeys { it.isNotBlank() }
        .mapValues { (_, score) -> clampScore10(score) }

    val answeredExercisesSnapshot = session.exercises
        .mapNotNull { exercise ->
            val score = safeMarks[exercise.id] ?: return@mapNotNull null

            mapOf(
                "exerciseId" to exercise.id,
                "belt" to exercise.belt.name,
                "beltHeb" to exercise.belt.heb,
                "beltEn" to exercise.belt.en,
                "topic" to exercise.topic,
                "subTopic" to exercise.subTopic.orEmpty(),
                "name" to exercise.name,
                "score" to score
            )
        }

    val score10 = if (session.maxScore == 0.0) {
        0.0
    } else {
        (session.totalScore / session.maxScore) * 10.0
    }

    val shareSummaryHe = buildCompletedExamShareSummary(
        session = session,
        isEnglish = false
    )

    val shareSummaryEn = buildCompletedExamShareSummary(
        session = session,
        isEnglish = true
    )

    val db = FirebaseFirestore.getInstance()
    val docRef = db.collection("internalExamResults").document()
    val resultId = docRef.id

    val data = hashMapOf(
        "resultId" to resultId,
        "coachUid" to coachUid,

        "traineeName" to cleanName,
        "traineeKey" to internalExamTraineeKey(cleanName),

        "belt" to belt.name,
        "beltHeb" to belt.heb,
        "beltEn" to belt.en,

        "status" to "completed",

        "marks" to safeMarks,
        "answeredExercises" to answeredExercisesSnapshot,
        "answeredCount" to answeredExercisesSnapshot.size,
        "totalExerciseCount" to session.exercises.size,

        "totalScore" to session.totalScore,
        "maxScore" to session.maxScore,
        "score10" to score10,
        "percent" to session.percent,

        "summaryTextHe" to examStatusText(session.percent, false),
        "summaryTextEn" to examStatusText(session.percent, true),
        "shareSummaryHe" to shareSummaryHe,
        "shareSummaryEn" to shareSummaryEn,

        "completedAtMillis" to System.currentTimeMillis(),
        "completedAt" to FieldValue.serverTimestamp(),

        "source" to "android_internal_exam"
    )

    docRef.set(data, SetOptions.merge()).await()

    return resultId
}

private suspend fun deleteExamDraftAfterCompletion(
    traineeName: String,
    belt: Belt
) {
    val cleanName = traineeName.trim()
    if (cleanName.isBlank()) return

    val coachUid = internalExamCoachUid() ?: return
    val docId = internalExamDraftId(coachUid, cleanName, belt)

    FirebaseFirestore.getInstance()
        .collection("internalExamDrafts")
        .document(docId)
        .delete()
        .await()
}

private suspend fun loadExamDraft(
    @Suppress("UNUSED_PARAMETER")
    context: Context,
    traineeName: String,
    belt: Belt
): Map<String, Int> {
    val cleanName = traineeName.trim()
    if (cleanName.isBlank()) return emptyMap()

    val coachUid = internalExamCoachUid() ?: return emptyMap()
    val docId = internalExamDraftId(coachUid, cleanName, belt)

    return runCatching {
        val snap = FirebaseFirestore.getInstance()
            .collection("internalExamDrafts")
            .document(docId)
            .get()
            .await()

        if (!snap.exists()) {
            return@runCatching emptyMap()
        }

        val rawMarks = snap.get("marks") as? Map<*, *> ?: return@runCatching emptyMap<String, Int>()

        rawMarks.mapNotNull { (key, value) ->
            val id = key?.toString()?.trim().orEmpty()
            val score = when (value) {
                is Long -> value.toInt()
                is Int -> value
                is Double -> value.toInt()
                is Number -> value.toInt()
                else -> null
            }

            if (id.isNotBlank() && score != null) {
                id to clampScore10(score)
            } else {
                null
            }
        }.toMap()
    }.getOrDefault(emptyMap())
}

// עזר: למצוא כותרת תת־נושא עבור תרגיל
private fun findSubTopicTitleForItemInternal(belt: Belt, topic: String, item: String): String? {

    fun norm(s: String): String = s
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .replace('\u05BE', '-').replace('\u2010', '-').replace('\u2011', '-')
        .replace('\u2012', '-').replace('\u2013', '-').replace('\u2014', '-')
        .replace('\u2015', '-').replace('\u2212', '-')
        .replace(Regex("\\s*-\\s*"), "-")
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()

    val wanted = norm(item)

    val subTitles = runCatching { ContentRepo.listSubTopicTitles(belt, topic) }
        .getOrDefault(emptyList())

    if (subTitles.isEmpty()) return null

    // ניסיון 1: התאמה ישירה
    for (stTitle in subTitles) {
        val items = runCatching {
            ContentRepo.listItemTitles(belt, topic, subTopicTitle = stTitle)
        }.getOrDefault(emptyList())

        if (items.any { it == item }) return stTitle
    }

    // ניסיון 2: התאמה מנורמלת
    for (stTitle in subTitles) {
        val items = runCatching {
            ContentRepo.listItemTitles(belt, topic, subTopicTitle = stTitle)
        }.getOrDefault(emptyList())

        if (items.any { norm(it) == wanted }) return stTitle
    }

    return null
}

private fun saveLastTrainee(
    @Suppress("UNUSED_PARAMETER")
    context: Context,
    name: String
) {
    val clean = name.trim()
    if (clean.isBlank()) return

    val coachUid = internalExamCoachUid() ?: return

    FirebaseFirestore.getInstance()
        .collection("internalExamCoachState")
        .document(coachUid)
        .set(
            mapOf(
                "lastTraineeName" to clean,
                "lastTraineeKey" to internalExamTraineeKey(clean),
                "updatedAtMillis" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
}

private suspend fun loadRecentTrainees(
    @Suppress("UNUSED_PARAMETER")
    context: Context
): List<String> {
    val coachUid =
        internalExamCoachUid()
            ?: return emptyList()

    return runCatching {
        FirebaseFirestore.getInstance()
            .collection(
                "internalExamRecentTrainees"
            )
            .document(coachUid)
            .collection("trainees")
            .orderBy(
                "updatedAtMillis",
                Query.Direction.DESCENDING
            )
            .limit(20L)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.getString("name")
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
            }
    }.getOrDefault(
        emptyList()
    )
}

private suspend fun loadRecentCompletedExamResults(
    limit: Int = 8
): List<RecentInternalExamResultUi> {
    val coachUid = internalExamCoachUid() ?: return emptyList()

    return runCatching {
        FirebaseFirestore.getInstance()
            .collection("internalExamResults")
            .whereEqualTo("coachUid", coachUid)
            .limit(80)
            .get()
            .await()
            .documents
            .asSequence()
            .filter { doc ->
                val status = doc.getString("status") ?: "completed"
                status == "completed"
            }
            .mapNotNull { doc ->
                val traineeName = doc.getString("traineeName")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                val completedAtMillis =
                    doc.getLong("completedAtMillis")
                        ?: doc.getLong("updatedAtMillis")
                        ?: 0L

                val beltName =
                    doc.getString("beltHeb")
                        ?: doc.getString("beltEn")
                        ?: doc.getString("belt")
                        ?: "—"

                RecentInternalExamResultUi(
                    resultId = doc.getString("resultId") ?: doc.id,
                    traineeName = traineeName,
                    beltName = beltName,
                    score10 = doc.getDouble("score10") ?: 0.0,
                    percent = (doc.getLong("percent") ?: 0L).toInt(),
                    completedAtMillis = completedAtMillis
                )
            }
            .sortedByDescending { it.completedAtMillis }
            .take(limit)
            .toList()
    }.onFailure {
        it.printStackTrace()
    }.getOrDefault(emptyList())
}

private suspend fun loadCompletedInternalExamSessionForPdf(
    resultId: String
): InternalExamSession? {
    val cleanResultId = resultId.trim()
    if (cleanResultId.isBlank()) return null

    val snap = FirebaseFirestore.getInstance()
        .collection("internalExamResults")
        .document(cleanResultId)
        .get()
        .await()

    if (!snap.exists()) return null

    val traineeName = snap.getString("traineeName")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    val belt = runCatching {
        Belt.valueOf(snap.getString("belt").orEmpty())
    }.getOrNull() ?: Belt.YELLOW

    val completedAtMillis = snap.getLong("completedAtMillis") ?: 0L
    val examDate = if (completedAtMillis > 0L) {
        java.time.Instant.ofEpochMilli(completedAtMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    } else {
        LocalDate.now()
    }

    val rawAnsweredExercises = snap.get("answeredExercises") as? List<*> ?: emptyList<Any>()

    val exercises = mutableListOf<ExamExerciseItem>()
    val marks = mutableListOf<Int?>()

    rawAnsweredExercises.forEachIndexed { index, rawItem ->
        val map = rawItem as? Map<*, *> ?: return@forEachIndexed

        val exerciseId = map["exerciseId"]
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "completed_${cleanResultId}_$index"

        val exerciseBelt = runCatching {
            Belt.valueOf(map["belt"]?.toString().orEmpty())
        }.getOrNull() ?: belt

        val topic = map["topic"]
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "—"

        val subTopic = map["subTopic"]
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val name = map["name"]
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "—"

        val score = when (val rawScore = map["score"]) {
            is Long -> rawScore.toInt()
            is Int -> rawScore
            is Double -> rawScore.toInt()
            is Number -> rawScore.toInt()
            is String -> rawScore.toIntOrNull()
            else -> null
        }?.let { clampScore10(it) }

        exercises += ExamExerciseItem(
            id = exerciseId,
            belt = exerciseBelt,
            topic = topic,
            subTopic = subTopic,
            name = name
        )

        marks += score
    }

    if (exercises.isEmpty()) return null

    return InternalExamSession(
        traineeName = traineeName,
        belt = belt,
        date = examDate,
        exercises = exercises,
        marks = marks
    )
}

private fun pushRecentTrainee(
    @Suppress("UNUSED_PARAMETER")
    context: Context,
    name: String
) {
    val clean = name.trim()
    if (clean.isBlank()) return

    val coachUid = internalExamCoachUid() ?: return
    val traineeKey = internalExamTraineeKey(clean)

    FirebaseFirestore.getInstance()
        .collection("internalExamRecentTrainees")
        .document(coachUid)
        .collection("trainees")
        .document(traineeKey)
        .set(
            mapOf(
                "name" to clean,
                "traineeKey" to traineeKey,
                "coachUid" to coachUid,
                "updatedAtMillis" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
}

private suspend fun removeRecentTraineeAfterCompletion(
    traineeName: String
) {
    val cleanName = traineeName.trim()
    if (cleanName.isBlank()) return

    val coachUid = internalExamCoachUid() ?: return
    val traineeKey = internalExamTraineeKey(cleanName)

    FirebaseFirestore.getInstance()
        .collection("internalExamRecentTrainees")
        .document(coachUid)
        .collection("trainees")
        .document(traineeKey)
        .delete()
        .await()
}

private suspend fun deleteCompletedInternalExamResult(
    resultId: String
) {
    val cleanResultId = resultId.trim()
    if (cleanResultId.isBlank()) return

    val coachUid = internalExamCoachUid() ?: return
    val db = FirebaseFirestore.getInstance()

    val directRef = db.collection("internalExamResults")
        .document(cleanResultId)

    val directSnap = directRef.get().await()

    if (directSnap.exists()) {
        val docCoachUid = directSnap.getString("coachUid")

        if (docCoachUid == coachUid) {
            directRef.delete().await()
            return
        }
    }

    val querySnap = db.collection("internalExamResults")
        .whereEqualTo("coachUid", coachUid)
        .whereEqualTo("resultId", cleanResultId)
        .get()
        .await()

    val batch = db.batch()

    querySnap.documents.forEach { doc ->
        batch.delete(doc.reference)
    }

    batch.commit().await()
}

private suspend fun deleteTraineeAndExamHistory(
    traineeName: String
) {
    val cleanName = traineeName.trim()
    if (cleanName.isBlank()) return

    val coachUid = internalExamCoachUid() ?: return
    val traineeKey = internalExamTraineeKey(cleanName)
    val db = FirebaseFirestore.getInstance()

    val draftsSnap = db.collection("internalExamDrafts")
        .whereEqualTo("coachUid", coachUid)
        .whereEqualTo("traineeKey", traineeKey)
        .get()
        .await()

    val resultsSnap = db.collection("internalExamResults")
        .whereEqualTo("coachUid", coachUid)
        .whereEqualTo("traineeKey", traineeKey)
        .get()
        .await()

    val batch = db.batch()

    batch.delete(
        db.collection("internalExamRecentTrainees")
            .document(coachUid)
            .collection("trainees")
            .document(traineeKey)
    )

    draftsSnap.documents.forEach { doc ->
        batch.delete(doc.reference)
    }

    resultsSnap.documents.forEach { doc ->
        batch.delete(doc.reference)
    }

    batch.commit().await()
}
