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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.app.search.KmiSearchBridge
import il.kmi.app.localization.rememberIsEnglish
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import android.graphics.RectF
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.ui.KmiTopBar

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

private fun examBeltImageScale(belt: Belt): Float =
    when (belt) {
        Belt.YELLOW -> 1.50f
        else -> 1.0f
    }

private fun internalExamEntryScreenBrush(belt: Belt): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FBFF),
            Color(0xFFEAF4FF),
            Color(0xFFB7DDF7),
            Color(0xFF1F78B4),
            Color(0xFF062B4A)
        )
    )

private fun examBeltScreenBrush(belt: Belt): androidx.compose.ui.graphics.Brush =
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

private fun examBeltCardBrush(belt: Belt): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.horizontalGradient(
        listOf(
            Color.White.copy(alpha = 0.98f),
            examBeltSoftColor(belt).copy(alpha = 0.92f),
            Color.White.copy(alpha = 0.96f)
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
    isEnglish: Boolean
): String {
    val score10 = if (session.maxScore == 0.0) {
        0.0
    } else {
        (session.totalScore / session.maxScore) * 10.0
    }

    val dateText = session.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val beltName = examBeltNameForUi(session.belt, isEnglish)
    val statusText = examStatusText(session.percent, isEnglish)

    return if (isEnglish) {
        """
        Internal Exam Summary
        Trainee: ${session.traineeName}
        Belt: $beltName
        Date: $dateText
        Score: ${score10.coerceIn(0.0, 10.0).toScoreString()} / 10 (${session.percent}%)
        Status: $statusText
        """.trimIndent()
    } else {
        """
        סיכום מבחן פנימי
        נבחן: ${session.traineeName}
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
         isEnglish: Boolean = false
     ): Uri? {
        return try {
            val document = PdfDocument()

            // A4 (pt)
            val pageW = 595
            val pageH = 842

            val leftMargin = 40f
            val rightMargin = (pageW - 40).toFloat()

            val headerH = 86f
            val footerH = 44f

            val contentTop = 40f + headerH
            val contentBottom = (pageH - 40).toFloat() - footerH

            fun pdfTr(he: String, en: String): String =
                if (isEnglish) en else he

            fun pdfBeltName(belt: Belt): String =
                if (isEnglish) belt.en else belt.heb

            fun pdfExerciseTitle(raw: String): String =
                if (isEnglish) ExerciseTitlesEn.getOrSame(raw.trim()) else raw

            fun pdfStatusText(percent: Int): String =
                examStatusText(percent, isEnglish)

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
                    p >= 85 -> AColor.parseColor("#16A34A") // green
                    p >= 70 -> AColor.parseColor("#84CC16") // lime
                    p >= 50 -> AColor.parseColor("#F59E0B") // amber
                    else    -> AColor.parseColor("#EF4444") // red
                }
            }

            val headerBg = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#0B1220")
            }

            val headerTitle = Paint().apply {
                isAntiAlias = true
                color = AColor.WHITE
                textSize = 20f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val headerSub = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#C7D2FE")
                textSize = 12f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }

            val cardBg = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#F8FAFC")
            }
            val cardStroke = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = AColor.parseColor("#E2E8F0")
            }

            val kpiLabel = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#64748B")
                textSize = 11f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }
            val kpiValue = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#0F172A")
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val sectionTitle = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#0F172A")
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val topicTitle = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#1E293B")
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val lineText = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#0F172A")
                textSize = 12.5f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }

            val scoreBoxBg = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#EEF2FF")
            }
            val scoreBoxStroke = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
                color = AColor.parseColor("#C7D2FE")
            }
            val scoreBoxText = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#0F172A")
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            }

            val divider = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#E2E8F0")
                strokeWidth = 1.2f
            }

            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = AColor.parseColor("#64748B")
                textSize = 10.5f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }

            fun drawRight(canvas: android.graphics.Canvas, text: String, y: Float, paint: Paint) {
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

            fun drawOpposite(canvas: android.graphics.Canvas, text: String, y: Float, paint: Paint) {
                if (isEnglish) {
                    drawRight(canvas, text, y, paint)
                } else {
                    canvas.drawText(text, leftMargin, y, paint)
                }
            }

            fun drawHeader(canvas: android.graphics.Canvas, pageNumber: Int) {
                // header bg
                canvas.drawRect(0f, 0f, pageW.toFloat(), headerH, headerBg)

                // “logo” קטן (KMI)
                val logoR = RectF(leftMargin, 22f, leftMargin + 44f, 22f + 44f)
                val logoPaint = Paint().apply { isAntiAlias = true; color = AColor.parseColor("#2563EB") }
                canvas.drawRoundRect(logoR, 12f, 12f, logoPaint)

                val logoText = Paint().apply {
                    isAntiAlias = true
                    color = AColor.WHITE
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                canvas.drawText("K.A.M.I", leftMargin + 10f, 22f + 28f, logoText)

                val title = pdfTr(
                    "דו\"ח מבחן פנימי",
                    "Internal Exam Report"
                )

                val sub = pdfTr(
                    "חגורה: ${pdfBeltName(session.belt)}",
                    "Belt: ${pdfBeltName(session.belt)}"
                )

                drawStart(canvas, title, 44f, headerTitle)
                drawStart(canvas, sub, 66f, headerSub)

                val pn = pdfTr(
                    "עמוד $pageNumber",
                    "Page $pageNumber"
                )

                drawOpposite(canvas, pn, 66f, headerSub)
            }

            fun drawFooter(canvas: android.graphics.Canvas, pageNumber: Int) {
                val y = (pageH - 18).toFloat()
                val left = pdfTr(
                    "נוצר ע\"י K.A.M.I",
                    "Generated by K.A.M.I"
                )

                val right = pdfTr(
                    "עמוד $pageNumber",
                    "Page $pageNumber"
                )

                canvas.drawText(left, leftMargin, y, footerPaint)
                drawRight(canvas, right, y, footerPaint)
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

                val x1 = leftMargin
                val x2 = leftMargin + cardW + gap
                val x3 = leftMargin + (cardW + gap) * 2

                card(
                    x1,
                    pdfTr("שם מתאמן", "Trainee name"),
                    session.traineeName.ifBlank { "—" }
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
                val badgeBg = Paint().apply { isAntiAlias = true; color = AColor.parseColor("#FFFFFF") }
                val badgeStroke = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1.6f; color = AColor.parseColor("#E2E8F0") }
                canvas.drawRoundRect(badgeR, 18f, 18f, badgeBg)
                canvas.drawRoundRect(badgeR, 18f, 18f, badgeStroke)

                val pillR = RectF(rightMargin - 150f, y + 18f, rightMargin - 18f, y + 60f)
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
                    color = AColor.parseColor("#0F172A")
                    textSize = 16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                val statusPaint = Paint().apply {
                    isAntiAlias = true
                    color = AColor.parseColor("#334155")
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

            fun drawScoreBox(canvas: android.graphics.Canvas, xRight: Float, yTop: Float, score: Int) {
                val w = 40f
                val h = 20f
                val r = RectF(xRight - w, yTop - 14f, xRight, yTop + (h - 14f))
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

            drawHeader(canvas, pageNumber)

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
                val topic: String,
                val name: String,
                val score: Int
            )

            val rows: List<PdfRow> =
                session.exercises.mapIndexedNotNull { index, ex ->
                    val score = session.marks.getOrNull(index) ?: return@mapIndexedNotNull null
                    PdfRow(
                        topic = pdfExerciseTitle(ex.topic),
                        name = pdfExerciseTitle(ex.name),
                        score = clampScore10(score)
                    )
                }

            var currentTopic: String? = null

            fun newPage() {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                drawHeader(canvas, pageNumber)
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
                currentTopic = null
            }

            // ✅ רצועה קבועה לימין עבור תיבת ציון
            val scoreBoxW = 40f
            val scoreBoxGap = 10f
            val nameRight = rightMargin - scoreBoxW - scoreBoxGap

            rows.forEach { r ->
                if (y > contentBottom - 24f) {
                    drawFooter(canvas, pageNumber)
                    newPage()
                }

                if (currentTopic != r.topic) {
                    currentTopic = r.topic

                    if (y > contentBottom - 40f) {
                        drawFooter(canvas, pageNumber)
                        newPage()
                    }

                    drawStart(
                        canvas,
                        pdfTr(
                            "נושא: ${currentTopic}",
                            "Topic: ${currentTopic}"
                        ),
                        y,
                        topicTitle
                    )
                    y += 18f
                }

                // ✅ שם התרגיל עד nameRight (לא נכנס לתיבת הציון)
                drawTextWithin(canvas, r.name, nameRight, y, lineText)

                // ✅ תיבת ציון תמיד בקצה ימין
                drawScoreBox(canvas, xRight = rightMargin, yTop = y, score = r.score)

                y += 16f
            }

            drawFooter(canvas, pageNumber)

            document.finishPage(page)

            val dir = File(context.cacheDir, "internal_exam")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "exam_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out -> document.writeTo(out) }
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
    examResults: Map<String, Boolean> = emptyMap(),
    currentScore: Float = 0f,
    onResultUpdate: (String, Boolean) -> Unit = { _, _ -> },
    onBeltChange: (Belt) -> Unit,
    onBack: () -> Unit,
    sharedMarksMap: MutableMap<String, Int>? = null,
    showSetupHeader: Boolean = true
) {
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val isEnglish = rememberIsEnglish()
    // ✅ דיאלוג "נבחנים אחרונים"
    var showPickTraineeDialog by remember { mutableStateOf(false) }
    var recentTrainees by remember { mutableStateOf<List<String>>(emptyList()) }

// ✅ טוען רשימה ראשונית
    LaunchedEffect(Unit) {
        recentTrainees = loadRecentTrainees(ctx, 20)
    }

    // ✅ האם להציג את בלוק שם הנבחן (נעלם אחרי Done/שמור)
    var showTraineeNameBox by rememberSaveable { mutableStateOf(traineeName.isBlank()) }

    fun commitTraineeNameAndCollapse(): Boolean {
        val name = traineeName.trim()
        if (name.isBlank()) return false

        pushRecentTrainee(ctx, name, 20)
        saveLastTrainee(ctx, name)

        scope.launch {
            recentTrainees = loadRecentTrainees(ctx, 20)
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

    // session מצטבר לכל החגורות עד החגורה הנוכחית
    val session by remember {
        derivedStateOf {
            buildInternalExamSessionForUi(
                traineeName = traineeName,
                belt = belt,
                marksMap = marksMap
            )
        }
    }

    // 🔽 פעולה אחת לשיתוף ה-PDF (משותפת לטופ-בר ולבאר התחתון)
    val onExportPdf: () -> Unit = {
        val uri = InternalExamPdf.createPdf(
            context = ctx,
            session = session,
            isEnglish = isEnglish
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
                examTr(isEnglish, "שגיאה ביצירת PDF", "Error creating PDF"),
                Toast.LENGTH_SHORT
            ).show()
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
                title = examTr(isEnglish, "מבחן פנימי", "Internal exam"),
                showMenu = true,
                showBottomActions = true,
                showRoleStatus = true,
                showModePill = true,
                showTopHome = false,
                showTopSearch = true,
                showSettings = true,
                showTopShare = false,
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
                .background(examBeltScreenBrush(belt))
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
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
                                    text = traineeName.trim(),
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Button(
                                    onClick = {
                                        scope.launch {
                                            recentTrainees = loadRecentTrainees(ctx, 20)
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
                                        Text(examTr(isEnglish, "אין נבחנים שמורים עדיין.", "No saved trainees yet."))
                                    } else {
                                        recentTrainees.forEach { name ->
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
                                                Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

                Divider(modifier = Modifier.padding(vertical = 4.dp))

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
                                belt = belt,
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
                                                start = if (isEnglish) 14.dp else 0.dp,
                                                end = if (isEnglish) 0.dp else 14.dp,
                                                top = 2.dp,
                                                bottom = 4.dp
                                            ),
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color(0xFFEAF6FF).copy(alpha = 0.86f),
                                        shadowElevation = 5.dp,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.72f)
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
                                                val subTopicExpanded = expandedSubTopicKey == subTopicKey

                                                SubTopicHeader(
                                                    title = examTitleForUi(subTopic, isEnglish),
                                                    expanded = subTopicExpanded,
                                                    exerciseCount = subTopicExercises.size,
                                                    isEnglish = isEnglish,
                                                    belt = belt,
                                                    onClick = {
                                                        expandedSubTopicKey =
                                                            if (subTopicExpanded) null else subTopicKey
                                                    }
                                                )

                                                if (index < subTopicGroups.size - 1 || directExercises.isNotEmpty()) {
                                                    Divider(
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

                                            if (directExercises.isNotEmpty()) {
                                                val generalKey = "$topic||__direct__"
                                                val generalExpanded = expandedSubTopicKey == generalKey

                                                SubTopicHeader(
                                                    title = examTr(isEnglish, "כללי", "General"),
                                                    expanded = generalExpanded,
                                                    exerciseCount = directExercises.size,
                                                    isEnglish = isEnglish,
                                                    belt = belt,
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
                    session = session,
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
                    },
                    onExportPdf = onExportPdf
                )

                // --- תחתית במסך התרגילים: מעבר חזרה למסך הראשי לבחירת חגורה אחרת ---
                ChangeBeltBottomBar(
                    isEnglish = isEnglish,
                    belt = belt,
                    onChangeBelt = {
                        val activeName = traineeName.trim()
                        if (activeName.isNotBlank()) {
                            saveExamDraft(ctx, activeName, belt, marksMap)
                            pushRecentTrainee(ctx, activeName, 20)
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
                                    resumeCheckedKey = "${belt.name}_${internalExamTraineeKey(activeName)}"

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
                    color = Color(0xFFF6F1FF),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
                    shadowElevation = 24.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFF8F5FF),
                                        Color(0xFFF2EBFF),
                                        Color(0xFFE9E2FF)
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEDE9FE),
                            shadowElevation = 10.dp,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "💾",
                                    fontSize = 28.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = examTr(isEnglish, "מבחן שמור נמצא", "Saved exam found"),
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = examTr(
                                isEnglish,
                                "נמצא מבחן שמור מהפעם האחרונה.\nלהמשיך ממנו או להתחיל מבחן חדש?",
                                "A saved exam was found from the last session.\nContinue from it or start a new exam?"
                            ),
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            pushRecentTrainee(ctx, name, 20)
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
    belt: Belt,
    onClick: () -> Unit
) {
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val cardShape = RoundedCornerShape(15.dp)

    Surface(
        shape = cardShape,
        color = Color(0xFFEAF2FF),
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (expanded) {
                Color(0xFFBFD0E8)
            } else {
                Color(0xFFD8E3F5)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
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
                color = if (expanded) {
                    Color(0xFF0F5E9C)
                } else {
                    Color(0xFF6B778B)
                },
                shadowElevation = 2.dp,
                modifier = Modifier.size(23.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (expanded) "▲" else "▼",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        lineHeight = 9.sp
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
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.4.sp,
                    lineHeight = 12.sp,
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
                    color = Color(0xFF5E6C80),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 7.2.sp,
                    lineHeight = 7.8.sp,
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
    belt: Belt,
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
            .height(40.dp)
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
                color = if (expanded) {
                    Color(0xFF0F5E9C)
                } else {
                    Color(0xFF6B778B)
                },
                shadowElevation = 2.dp,
                modifier = Modifier.size(22.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (expanded) "−" else "+",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
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
                    color = Color(0xFF1E2A3D),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.6.sp,
                    lineHeight = 12.sp,
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
                    color = Color(0xFF5E6C80),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.4.sp,
                    lineHeight = 8.sp,
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
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    val ctx = LocalContext.current
    val isEnglish = rememberIsEnglish()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var traineeName by rememberSaveable { mutableStateOf("") }
    var currentBelt by remember { mutableStateOf(Belt.YELLOW) }

    var recentTrainees by remember { mutableStateOf<List<String>>(emptyList()) }
    var recentCompletedResults by remember { mutableStateOf<List<RecentInternalExamResultUi>>(emptyList()) }
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

    var examStarted by rememberSaveable { mutableStateOf(false) }
    var traineeSessionKey by rememberSaveable { mutableStateOf(0) }

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
        recentTrainees = loadRecentTrainees(ctx, 20)
        recentCompletedResults = loadRecentCompletedExamResults(limit = 20)
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            recentTrainees = loadRecentTrainees(ctx, 20)
        }
    }

    val exercises = remember(currentBelt) {
        buildInternalExamExercisesFromContent(currentBelt)
    }

    val hasExamProgress = marksMap.isNotEmpty()

    val beltMainColor = when (currentBelt) {
        Belt.YELLOW -> Color(0xFFFDE047)
        Belt.ORANGE -> Color(0xFFFF8A00)
        Belt.GREEN -> Color(0xFF16A34A)
        Belt.BLUE -> Color(0xFF2563EB)
        Belt.BROWN -> Color(0xFF7C3F1D)
        Belt.BLACK -> Color(0xFF111827)
        else -> Color(0xFF7C3AED)
    }

    val beltDarkColor = when (currentBelt) {
        Belt.YELLOW -> Color(0xFF7A5A00)
        Belt.ORANGE -> Color(0xFF7C2D12)
        Belt.GREEN -> Color(0xFF064E3B)
        Belt.BLUE -> Color(0xFF1E3A8A)
        Belt.BROWN -> Color(0xFF3B1F12)
        Belt.BLACK -> Color(0xFF020617)
        else -> Color(0xFF312E81)
    }

    val beltSoftColor = when (currentBelt) {
        Belt.YELLOW -> Color(0xFFFEFCE8)
        Belt.ORANGE -> Color(0xFFFFEDD5)
        Belt.GREEN -> Color(0xFFDCFCE7)
        Belt.BLUE -> Color(0xFFDBEAFE)
        Belt.BROWN -> Color(0xFFF3E8D6)
        Belt.BLACK -> Color(0xFFE5E7EB)
        else -> Color(0xFFEDE9FE)
    }

    val entryCardColor = when (currentBelt) {
        Belt.YELLOW -> Color(0xFFFEF9C3).copy(alpha = 0.94f)
        Belt.ORANGE -> Color(0xFFFFEDD5).copy(alpha = 0.94f)
        Belt.GREEN -> Color(0xFFDCFCE7).copy(alpha = 0.94f)
        Belt.BLUE -> Color(0xFFDBEAFE).copy(alpha = 0.94f)

        // חום מורגש יותר, אבל עדיין בהיר מספיק לקריאות
        Belt.BROWN -> Color(0xFFE8C89A).copy(alpha = 0.94f)

        // שחור/אפור פרימיום, בלי להרוג את הקריאות של הטקסטים
        Belt.BLACK -> Color(0xFFC7CCD6).copy(alpha = 0.94f)

        else -> Color(0xFFEDE9FE).copy(alpha = 0.94f)
    }

    val entrySession by remember {
        derivedStateOf {
            buildInternalExamSessionForUi(
                traineeName = traineeName,
                belt = currentBelt,
                marksMap = marksMap
            )
        }
    }

    traineeToDelete?.let { nameToDelete ->
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
                        "האם למחוק את \"$nameToDelete\" ואת כל המבחנים/טיוטות שלו?",
                        "Delete \"$nameToDelete\" and all of this trainee's exams/drafts?"
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
                                if (traineeName.trim().equals(nameToDelete.trim(), ignoreCase = true)) {
                                    traineeName = ""
                                    marksMap.clear()
                                    traineeSessionKey++
                                }

                                recentTrainees = loadRecentTrainees(ctx, 20)
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
                        "האם למחוק את המבחן של \"${resultToDelete.traineeName}\" מהיסטוריית המבחנים?\nהמחיקה היא סופית ולא תשפיע על מבחנים אחרים.",
                        "Delete \"${resultToDelete.traineeName}\" from the exam history?\nThis action is final and will not affect other exams."
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
                    showTopShare = false,
                    centerTitle = true,
                    onHome = onHome
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(internalExamEntryScreenBrush(currentBelt))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.68f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = examTr(
                                isEnglish,
                                "בחר נבחן וחגורה לפני תחילת המבחן",
                                "Select a trainee and exam belt before starting"
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E2A3D),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.White.copy(alpha = 0.90f),
                        border = BorderStroke(
                            1.dp,
                            examBeltMainColor(currentBelt).copy(alpha = 0.14f)
                        ),
                        shadowElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.96f),
                                            Color(0xFFF7FAFF).copy(alpha = 0.94f),
                                            examBeltSoftColor(currentBelt).copy(alpha = 0.40f)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                            recentTrainees = loadRecentTrainees(ctx, 20)
                                            recentCompletedResults = loadRecentCompletedExamResults(limit = 20)
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
                                        .menuAnchor()
                                        .focusRequester(traineeFocusRequester)
                                        .fillMaxWidth()
                                        .heightIn(min = 78.dp)
                                        .clickable {
                                            allowTraineeKeyboard = false
                                            keyboard?.hide()
                                            focusManager.clearFocus(force = true)
                                            expanded = true

                                            scope.launch {
                                                recentTrainees = loadRecentTrainees(ctx, 20)
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
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
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
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            lineHeight = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                              },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = examBeltMainColor(currentBelt).copy(alpha = 0.78f),
                                        unfocusedBorderColor = examBeltDarkColor(currentBelt).copy(alpha = 0.26f),
                                        focusedTextColor = Color(0xFF111827),
                                        unfocusedTextColor = Color(0xFF111827),
                                        focusedLabelColor = examBeltDarkColor(currentBelt).copy(alpha = 0.82f),
                                        unfocusedLabelColor = examBeltDarkColor(currentBelt).copy(alpha = 0.72f),
                                        cursorColor = examBeltDarkColor(currentBelt),
                                        focusedContainerColor = Color.White.copy(alpha = 0.64f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.52f),
                                        disabledContainerColor = Color.Transparent,
                                        errorContainerColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF111827),
                                        fontSize = 18.sp,
                                        lineHeight = 20.sp,
                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
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
                                        .fillMaxWidth(0.55f)
                                        .heightIn(min = 240.dp, max = 300.dp)
                                        .background(Color(0xFFF8FAFC))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isEnglish) {
                                                    "➕ New trainee…"
                                                } else {
                                                    "➕ נבחן חדש…"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF111827),
                                                fontSize = 18.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
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
                                        Divider()
                                    }

                                    recentTrainees.take(20).forEach { name ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                                                fontSize = 16.sp
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF111827),
                                                        fontSize = 17.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f),
                                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
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
                                                                fontSize = 16.sp
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
                                                    val savedDraft = loadExamDraft(ctx, cleanName, currentBelt)

                                                    marksMap.clear()
                                                    if (savedDraft.isNotEmpty()) {
                                                        marksMap.putAll(savedDraft)
                                                    }

                                                    saveLastTrainee(ctx, cleanName)
                                                    recentTrainees = loadRecentTrainees(ctx, 20)

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
                                onBeltChange = { newBelt -> currentBelt = newBelt }
                            )

                            PremiumExamSetupButton(
                                text = if (hasExamProgress) {
                                    examTr(isEnglish, "המשך מבחן", "Continue exam")
                                } else {
                                    examTr(isEnglish, "התחל מבחן", "Start exam")
                                },
                                icon = if (hasExamProgress) "⏩" else "▶",
                                startColor = examBeltDarkColor(currentBelt),
                                centerColor = examBeltMainColor(currentBelt),
                                endColor = Color(0xFF7C3AED),
                                textFontSize = 22.sp,
                                textLineHeight = 24.sp,
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
                                            val savedDraft = loadExamDraft(ctx, cleanName, currentBelt)

                                            if (savedDraft.isNotEmpty()) {
                                                marksMap.clear()
                                                marksMap.putAll(savedDraft)
                                            }

                                            pushRecentTrainee(ctx, cleanName, 20)
                                            saveLastTrainee(ctx, cleanName)
                                            recentTrainees = loadRecentTrainees(ctx, 20)

                                            traineeSessionKey++
                                            examStarted = true
                                        }
                                    }
                                }
                            )

                            BottomActionBar(
                                session = entrySession,
                                isEnglish = isEnglish,
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
                                                recentTrainees = loadRecentTrainees(ctx, 20)

                                                Toast.makeText(
                                                    ctx,
                                                    examTr(isEnglish, "המבחן נשמר להמשך", "Exam saved for later"),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }.onFailure { error ->
                                                Toast.makeText(
                                                    ctx,
                                                    examTr(isEnglish, "שמירת המבחן נכשלה", "Saving the exam failed") +
                                                            ": ${error.localizedMessage ?: ""}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                onExportPdf = {
                                    val cleanName = traineeName.trim()

                                    if (cleanName.isBlank()) {
                                        Toast.makeText(
                                            ctx,
                                            examTr(
                                                isEnglish,
                                                "נא להזין שם נבחן לפני שיתוף",
                                                "Please enter a trainee name before sharing"
                                            ),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val pdfSession = buildInternalExamSessionForUi(
                                            traineeName = cleanName,
                                            belt = currentBelt,
                                            marksMap = marksMap
                                        )

                                        val uri = InternalExamPdf.createPdf(
                                            context = ctx,
                                            session = pdfSession,
                                            isEnglish = isEnglish
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
                                                examTr(isEnglish, "שגיאה ביצירת PDF", "Error creating PDF"),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    PremiumExamSetupButton(
                        text = examTr(isEnglish, "ארכיון מבחנים", "Exam archive"),
                        icon = "📚",
                        startColor = Color(0xFF0F172A),
                        centerColor = Color(0xFF334155),
                        endColor = Color(0xFF7C3AED),
                        textFontSize = 19.sp,
                        textLineHeight = 21.sp,
                        onClick = {
                            scope.launch {
                                recentCompletedResults = loadRecentCompletedExamResults(limit = 20)
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
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(
                        width = 1.dp,
                        color = examBeltMainColor(currentBelt).copy(alpha = 0.28f)
                    ),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(
                                        Color.White,
                                        examBeltSoftColor(currentBelt).copy(alpha = 0.78f),
                                        Color.White
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
                                text = examTr(isEnglish, "היסטוריית מבחנים", "Exam history"),
                                modifier = Modifier.weight(1f),
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                color = Color(0xFF111827),
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                onClick = { showExamHistoryDialog = false },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.80f),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "×",
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        lineHeight = 22.sp
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
                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )

                        Divider(color = Color(0xFFCBD5E1).copy(alpha = 0.70f))

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
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(recentCompletedResults.take(20)) { result ->
                                    CompletedExamHistoryRow(
                                        result = result,
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
            CompletedExamPreviewDialog(
                session = previewSession,
                isEnglish = isEnglish,
                currentBelt = currentBelt,
                onDismiss = {
                    completedPreviewSession = null
                },
                onSharePdf = {
                    val uri = InternalExamPdf.createPdf(
                        context = ctx,
                        session = previewSession,
                        isEnglish = isEnglish
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
                            examTr(isEnglish, "שגיאה ביצירת PDF", "Error creating PDF"),
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
                        recentTrainees = loadRecentTrainees(ctx, 20)
                        recentCompletedResults = loadRecentCompletedExamResults(limit = 20)
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
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit
) {
    val context = LocalContext.current

    val score10 = if (session.maxScore == 0.0) {
        0.0
    } else {
        (session.totalScore / session.maxScore) * 10.0
    }

    val answeredCount = session.marks.count { it != null }
    val dateText = session.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(
                width = 1.dp,
                color = examBeltMainColor(currentBelt).copy(alpha = 0.28f)
            ),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Color.White,
                                examBeltSoftColor(currentBelt).copy(alpha = 0.82f),
                                Color.White
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
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Black,
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = session.traineeName,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = textAlign,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            lineHeight = 19.sp,
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

                Divider(color = Color(0xFFCBD5E1).copy(alpha = 0.55f))

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
                    value = "${score10.coerceIn(0.0, 10.0).toScoreString()} / 10  (${session.percent}%)",
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

                Divider(color = Color(0xFFCBD5E1).copy(alpha = 0.55f))

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
                                text = examTr(isEnglish, "סגור", "Close"),
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
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
                                    val summaryText = buildCompletedExamShareSummary(
                                        session = session,
                                        isEnglish = isEnglish
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
                                text = examTr(isEnglish, "העתק", "Copy"),
                                color = examBeltDarkColor(currentBelt),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
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
                                text = examTr(isEnglish, "שתף PDF", "PDF"),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
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
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
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
            color = Color(0xFF64748B),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign,
            color = Color(0xFF111827),
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            lineHeight = 19.sp,
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
    isEnglish: Boolean,
    currentBelt: Belt,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    val dateText = internalExamResultDateText(result.completedAtMillis)
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = examBeltSoftColor(currentBelt).copy(alpha = 0.78f),
        border = BorderStroke(
            width = 1.dp,
            color = examBeltDarkColor(currentBelt).copy(alpha = 0.14f)
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
                                fontSize = 13.sp,
                                lineHeight = 13.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 5.dp)
                            )

                            Text(
                                text = result.traineeName,
                                textAlign = TextAlign.Left,
                                color = Color(0xFF111827),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "📄",
                                fontSize = 13.sp,
                                lineHeight = 13.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(end = 5.dp)
                            )

                            Text(
                                text = result.traineeName,
                                textAlign = TextAlign.Right,
                                color = Color(0xFF111827),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
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
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
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
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = examTr(
                        isEnglish,
                        "ציון: ${result.score10.coerceIn(0.0, 10.0).toScoreString()} / 10  (${result.percent}%)",
                        "Score: ${result.score10.coerceIn(0.0, 10.0).toScoreString()} / 10  (${result.percent}%)"
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.2.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                onClick = onDeleteClick,
                shape = CircleShape,
                color = Color(0xFFFFF1F2),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFFCA5A5)
                ),
                modifier = Modifier.size(26.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗑",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
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
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentCompletedExamResultsCard(
    results: List<RecentInternalExamResultUi>,
    isEnglish: Boolean,
    currentBelt: Belt,
    onOpenResult: (RecentInternalExamResultUi) -> Unit
) {
    if (results.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.86f),
        border = BorderStroke(
            width = 1.dp,
            color = examBeltMainColor(currentBelt).copy(alpha = 0.24f)
        ),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
        ) {
            Text(
                text = examTr(
                    isEnglish,
                    "מבחנים אחרונים שהושלמו",
                    "Recent completed exams"
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                color = Color(0xFF111827),
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = examTr(
                    isEnglish,
                    "לחיצה על מבחן תפתח שיתוף PDF מחדש",
                    "Tap an exam to share the PDF again"
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            results.take(5).forEach { result ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenResult(result) },
                    shape = RoundedCornerShape(18.dp),
                    color = examBeltSoftColor(currentBelt).copy(alpha = 0.72f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = examBeltDarkColor(currentBelt).copy(alpha = 0.14f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                    text = "${result.percent}%",
                                    color = examBeltDarkColor(currentBelt),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                        ) {
                            Text(
                                text = result.traineeName,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                color = Color(0xFF111827),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "${result.beltName} • ${result.score10.coerceIn(0.0, 10.0).toScoreString()} / 10",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "📤",
                            fontSize = 20.sp,
                            maxLines = 1
                        )
                    }
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
    var expanded by remember { mutableStateOf(false) }

    val belts = listOf(
        Belt.YELLOW,
        Belt.ORANGE,
        Belt.GREEN,
        Belt.BLUE,
        Belt.BROWN,
        Belt.BLACK
    )

    val mainColor = examBeltMainColor(currentBelt)
    val darkColor = examBeltDarkColor(currentBelt)
    val softColor = examBeltSoftColor(currentBelt)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = mainColor.copy(alpha = 0.18f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = softColor,
                    border = BorderStroke(1.dp, mainColor.copy(alpha = 0.24f)),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (expanded) "▲" else "▼",
                            color = darkColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                ) {
                    Text(
                        text = examTr(isEnglish, "חגורה במבחן", "Exam belt"),
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = examBeltShortNameForUi(currentBelt, isEnglish),
                        color = darkColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        lineHeight = 20.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(0.dp))

                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 52.dp)
                        .offset(x = if (isEnglish) 8.dp else (-2).dp),
                    contentAlignment = if (isEnglish) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Image(
                        painter = painterResource(id = examBeltDrawableRes(currentBelt)),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 5.dp)
                            .graphicsLayer {
                                rotationZ = if (isEnglish) 14f else -14f
                                scaleX = examBeltImageScale(currentBelt)
                                scaleY = examBeltImageScale(currentBelt)
                            },
                                contentScale = ContentScale.Fit
                    )
                }
            }
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color(0xFFF8FAFC))
        ) {
            belts.forEach { b ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = examBeltSoftColor(b),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = examBeltMainColor(b).copy(alpha = 0.28f)
                                ),
                                modifier = Modifier.size(width = 66.dp, height = 38.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = examBeltDrawableRes(b)),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = examBeltShortNameForUi(b, isEnglish),
                                fontWeight = if (b == currentBelt) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (b == currentBelt) examBeltDarkColor(b) else Color(0xFF111827),
                                fontSize = 17.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        if (b != currentBelt) {
                            onBeltChange(b)
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

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp), // ✅ פחות גובה
        colors = CardDefaults.cardColors(containerColor = examBeltSoftColor(currentBelt)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp) // ✅ פחות גובה
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
                                text = examTr(isEnglish, "סיכום מבחן", "Exam summary"),
                                style = MaterialTheme.typography.titleMedium.copy(
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
                            text = examTr(
                                isEnglish,
                                "מצטבר: ${totalScore10.coerceIn(0.0, 10.0).toScoreString()} / 10  (${percent}%)",
                                "Total: ${totalScore10.coerceIn(0.0, 10.0).toScoreString()} / 10  (${percent}%)"
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Right
                        )

                        Text(
                            text = summaryText,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Right
                        )
                    }

                    Image(
                        painter = painterResource(id = examBeltDrawableRes(currentBelt)),
                        contentDescription = examBeltNameForUi(currentBelt, isEnglish),
                        modifier = Modifier
                            .width(104.dp)
                            .height(28.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = painterResource(id = examBeltDrawableRes(currentBelt)),
                        contentDescription = examBeltNameForUi(currentBelt, isEnglish),
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
                                text = examTr(isEnglish, "סיכום מבחן", "Exam summary"),
                                style = MaterialTheme.typography.titleMedium.copy(
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
                            text = examTr(
                                isEnglish,
                                "מצטבר: ${totalScore10.coerceIn(0.0, 10.0).toScoreString()} / 10  (${percent}%)",
                                "Total: ${totalScore10.coerceIn(0.0, 10.0).toScoreString()} / 10  (${percent}%)"
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Left
                        )

                        Text(
                            text = summaryText,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Left
                        )
                    }
                }
            }

            // ✅ פירוט רק כשפותחים
            if (expanded && beltScores.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                beltScores.forEach { (belt, score) ->
                    Text(
                        // ✅ לכל חגורה: 0–10
                        text = "${examBeltNameForUi(belt, isEnglish)}: ${score.score10.coerceIn(0.0, 10.0).toScoreString()} / 10 (${score.percent}%)",
                        style = MaterialTheme.typography.bodySmall
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
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.985f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFD8E3F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.8.sp,
                    lineHeight = 15.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF172033),
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
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
    text: String,
    fontSizeSp: Int = 13,
    strokeWidthPx: Float = 5f
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // outline
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            fontWeight = FontWeight.Bold,
            fontSize = fontSizeSp.sp,
            color = Color.White,
            style = TextStyle(drawStyle = Stroke(width = strokeWidthPx))
        )
        // fill + tiny shadow
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            fontWeight = FontWeight.Bold,
            fontSize = fontSizeSp.sp,
            color = Color.Black,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.35f),
                    blurRadius = 3f
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
            text = value.toString(),
            fontSizeSp = 9,
            strokeWidthPx = 3f
        )
    }
}

@Composable
private fun PremiumExamSetupButton(
    text: String,
    icon: String,
    startColor: Color,
    centerColor: Color,
    endColor: Color,
    textFontSize: androidx.compose.ui.unit.TextUnit = 25.sp,
    textLineHeight: androidx.compose.ui.unit.TextUnit = 26.sp,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 10.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.24f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClick() }
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            startColor.copy(alpha = 0.96f),
                            centerColor.copy(alpha = 0.96f),
                            endColor.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = textFontSize,
                lineHeight = textLineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.22f),
                        blurRadius = 8f
                    )
                )
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
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 14.dp,
        modifier = modifier.height(58.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .clickable { onClick() }
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            startColor,
                            centerColor,
                            endColor
                        )
                    )
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
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
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 5.dp)
                .height(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onChangeBelt() }
                    .background(
                        brush = examBeltButtonBrush(belt)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🥋",
                        fontSize = 16.sp
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = examTr(isEnglish, "מעבר לחגורה אחרת", "Change belt"),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.2.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    session: InternalExamSession,
    isEnglish: Boolean,
    isSaving: Boolean = false,
    finishMode: Boolean = false,
    onSave: () -> Unit,
    onExportPdf: () -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 4.dp,
                    end = 4.dp,
                    top = 2.dp,
                    bottom = 4.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = !isSaving) { onSave() }
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF5B35D5),
                                        Color(0xFF7C3AED),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSaving) {
                                examTr(isEnglish, "שומר...", "Saving...")
                            } else if (finishMode) {
                                examTr(isEnglish, "סיום מבחן", "Finish exam")
                            } else {
                                examTr(isEnglish, "שמור", "Save")
                            },
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onExportPdf() }
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF0EA5E9),
                                        Color(0xFF2563EB),
                                        Color(0xFF3B82F6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = examTr(isEnglish, "שתף", "Share"),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
            val sharedBelt: il.kmi.shared.domain.Belt =
                il.kmi.shared.domain.Belt.fromId(belt.id)
                    ?: il.kmi.shared.domain.Belt.WHITE

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

    pushRecentTrainee(context, cleanName, 20)
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

    pushRecentTrainee(context, cleanName, 20)
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
            return@runCatching emptyMap<String, Int>()
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

private fun saveLastTrainee(context: Context, name: String) {
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

private fun loadLastTrainee(context: Context): String {
    // לא בוחרים נבחן אוטומטית במסך הכניסה.
    // הפונקציה נשארת כדי לא לשבור קריאות קיימות.
    return ""
}

private suspend fun loadRecentTrainees(context: Context, limit: Int = 20): List<String> {
    val coachUid = internalExamCoachUid() ?: return emptyList()

    return runCatching {
        FirebaseFirestore.getInstance()
            .collection("internalExamRecentTrainees")
            .document(coachUid)
            .collection("trainees")
            .orderBy("updatedAtMillis", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.getString("name")?.trim()?.takeIf { it.isNotBlank() }
            }
    }.getOrDefault(emptyList())
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

private fun pushRecentTrainee(context: Context, name: String, limit: Int = 20) {
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
