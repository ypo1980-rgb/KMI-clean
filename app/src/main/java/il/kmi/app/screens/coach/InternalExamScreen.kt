package il.kmi.app.screens.coach

import android.content.Context
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
import androidx.compose.foundation.horizontalScroll
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
import il.kmi.shared.domain.SubTopicRegistry
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import android.graphics.RectF
import android.graphics.Color as AColor

// ======================
// מודלים ולוגיקה
// ======================

// ✅ ציון לתרגיל: 0..10
private fun clampScore10(v: Int): Int = v.coerceIn(0, 10)

data class ExamExerciseItem(
    val id: String,
    val belt: Belt,      // החגורה האמיתית של התרגיל
    val topic: String,
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

// הדפסה יפה של ניקוד
private fun Double.toScoreString(): String {
    if (this == 0.0) return "0"
    val intPart = this.toInt()
    return if (abs(this - intPart) < 1e-6) {
        intPart.toString()
    } else {
        String.format(java.util.Locale("he", "IL"), "%.1f", this)
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

// ======================
// יצוא PDF
// ======================

object InternalExamPdf {

    fun createPdf(context: Context, session: InternalExamSession): Uri? {
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
                canvas.drawText("KMI", leftMargin + 10f, 22f + 28f, logoText)

                // title + subtitle (ימין)
                val title = "דו\"ח מבחן פנימי"
                val sub = "חגורה: ${session.belt.heb}"

                drawRight(canvas, title, 44f, headerTitle)
                drawRight(canvas, sub, 66f, headerSub)

                // page number קטן בצד שמאל למעלה
                val pn = "עמוד $pageNumber"
                canvas.drawText(pn, leftMargin + 56f, 66f, headerSub)
            }

            fun drawFooter(canvas: android.graphics.Canvas, pageNumber: Int) {
                val y = (pageH - 18).toFloat()
                val left = "נוצר ע\"י KMI"
                val right = "עמוד $pageNumber"
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

                card(x1, "שם מתאמן", session.traineeName.ifBlank { "—" })
                card(x2, "חגורה במבחן", session.belt.heb)
                card(x3, "תאריך", dateStr)

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

                val scoreLine = "ציון: ${session.totalScore.toInt()} / ${session.maxScore.toInt()}  (${p}%)"
                val statusLine = "סטטוס: ${session.summaryText}"

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

                // ימין (השורות)
                drawRight(canvas, scoreLine, y + 40f, scorePaint)
                drawRight(canvas, statusLine, y + 62f, statusPaint)

                // טקסט בתוך ה-pill
                val pillLabel = when {
                    p >= 85 -> "מצוין"
                    p >= 70 -> "טוב"
                    p >= 50 -> "בינוני"
                    else -> "חלש"
                }
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

            fun drawRightWithin(canvas: android.graphics.Canvas, text: String, xRight: Float, y: Float, paint: Paint) {
                val w = paint.measureText(text)
                canvas.drawText(text, xRight - w, y, paint)
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
            drawRight(canvas, "פירוט תרגילים", y, sectionTitle)
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
                        topic = ex.topic,
                        name = ex.name,
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

                drawRight(canvas, "פירוט תרגילים (המשך)", y, sectionTitle)
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

                    drawRight(canvas, "נושא: ${currentTopic}", y, topicTitle)
                    y += 18f
                }

                // ✅ שם התרגיל עד nameRight (לא נכנס לתיבת הציון)
                drawRightWithin(canvas, r.name, nameRight, y, lineText)

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

    fun sharePdf(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "שיתוף דו\"ח מבחן פנימי")
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
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // ✅ דיאלוג "נבחנים אחרונים"
    var showPickTraineeDialog by remember { mutableStateOf(false) }
    var recentTrainees by remember { mutableStateOf<List<String>>(emptyList()) }

// ✅ טוען רשימה ראשונית
    LaunchedEffect(Unit) {
        recentTrainees = loadRecentTrainees(ctx, 10)
    }

    // ✅ האם להציג את בלוק שם הנבחן (נעלם אחרי Done/שמור)
    var showTraineeNameBox by rememberSaveable { mutableStateOf(traineeName.isBlank()) }

    fun commitTraineeNameAndCollapse(): Boolean {
        val name = traineeName.trim()
        if (name.isBlank()) return false

        pushRecentTrainee(ctx, name, 10)
        saveLastTrainee(ctx, name)

        focusManager.clearFocus()
        keyboard?.hide()
        showTraineeNameBox = false
        return true
    }

    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var pendingLoadedDraft by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // ✅ כדי שלא נפתח דיאלוג על כל אות בזמן שמקלידים שם
    var resumeCheckedKey by remember { mutableStateOf<String?>(null) }

    // ✅ ציון לכל תרגיל: 0..10
    val marksMap = remember { mutableStateMapOf<String, Int>() }

    // ✅ דיאלוג יציאה / חזרה
    BackHandler {
        if (hasUnsavedChanges) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    // session – רק לחגורה הנוכחית (ל-PDF ולשורת הציון התחתונה)
    val session by remember {
        derivedStateOf {
            val marksList = exercises.map { ex -> marksMap[ex.id] }
            InternalExamSession(
                traineeName = traineeName,
                belt = belt,
                date = LocalDate.now(),
                exercises = exercises,
                marks = marksList
            )
        }
    }

    // 🔽 פעולה אחת לשיתוף ה-PDF (משותפת לטופ-בר ולבאר התחתון)
    val onExportPdf: () -> Unit = {
        val uri = InternalExamPdf.createPdf(ctx, session)
        if (uri != null) {
            InternalExamPdf.sharePdf(ctx, uri)
        } else {
            Toast.makeText(ctx, "שגיאה ביצירת PDF", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ טעינת Draft (אם קיים) בכניסה למסך
    LaunchedEffect(traineeName, belt) {
        val name = traineeName.trim()
        if (name.isBlank()) return@LaunchedEffect

        val key = draftKey(name, belt)
        if (resumeCheckedKey == key) return@LaunchedEffect
        resumeCheckedKey = key

        val loaded = loadExamDraft(ctx, name, belt)
        if (loaded.isNotEmpty()) {
            pendingLoadedDraft = loaded
            showResumeDialog = true
        }
    }

    Scaffold(
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color(0xFF020617),
                            Color(0xFF0F172A),
                            Color(0xFF1E3A8A),
                            Color(0xFF38BDF8)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                // ✅ מצב עבודה: אם יש שם נבחן והוא כבר "ננעל" – מציגים פס קומפקטי
                val hasActiveTrainee = traineeName.trim().isNotBlank() && !showTraineeNameBox

                if (showTraineeNameBox) {
                    // 🟦 מצב בחירת/הזנת נבחן (מופיע רק כשצריך)
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
                            // ✅ שם נבחן – צד ימין
                            OutlinedTextField(
                                value = traineeName,
                                onValueChange = { onTraineeNameChange(it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("שם הנבחן") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { commitTraineeNameAndCollapse() }
                                )
                            )

                            Spacer(Modifier.width(10.dp))

                            // ✅ כפתור אישור – צד שמאל
                            Button(
                                onClick = { commitTraineeNameAndCollapse() },
                                enabled = traineeName.trim().isNotBlank()
                            ) {
                                Text("אישור")
                            }
                        }
                    }

                } else if (hasActiveTrainee) {
                    // 🟩 מצב עבודה — פס קומפקטי מאוד במקום הבלוק הגדול
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
                                    recentTrainees = loadRecentTrainees(ctx, 10)
                                    showPickTraineeDialog = true
                                }
                            ) { Text("החלף") }

                            Button(
                                onClick = {
                                    // ✅ מבחן חדש לגמרי
                                    marksMap.clear()
                                    onTraineeNameChange("")
                                    showTraineeNameBox = true
                                    resumeCheckedKey = null
                                }
                            ) { Text("חדש") }
                        }
                    }
                }

                if (showPickTraineeDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showPickTraineeDialog = false },
                        title = { Text("בחר נבחן") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                                if (recentTrainees.isEmpty()) {
                                    Text("אין נבחנים שמורים עדיין.")
                                } else {
                                    recentTrainees.forEach { name ->
                                        Button(
                                            onClick = {
                                                // ✅ מעבר לנבחן שנבחר
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
                                        // ✅ נבחן חדש
                                        marksMap.clear()
                                        onTraineeNameChange("")
                                        showTraineeNameBox = true
                                        resumeCheckedKey = null
                                        showPickTraineeDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("נבחן חדש") }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {}
                    )
                }

                // --- בחירת חגורה ---
                BeltSelector(
                    currentBelt = belt,
                    onBeltChange = onBeltChange
                )

                // --- סיכום ---
                SummaryCard(
                    currentBelt = belt,
                    marksMap = marksMap
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                val exercisesByTopic = remember(exercises) {
                    exercises.groupBy { it.topic }
                }

                var expandedTopic by remember { mutableStateOf<String?>(null) }

                // --- תרגילים ---
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    exercisesByTopic.forEach { (topic, topicExercises) ->

                        item {
                            TopicHeader(
                                title = topic,
                                expanded = expandedTopic == topic,
                                onClick = {
                                    expandedTopic = if (expandedTopic == topic) null else topic
                                }
                            )
                        }

                        if (expandedTopic == topic) {
                            items(topicExercises) { ex ->
                                val scoreForThis = marksMap[ex.id]

                                ExerciseRow(
                                    name = ex.name,
                                    score = scoreForThis,
                                    onScoreChange = { newScore ->
                                        hasUnsavedChanges = true

                                        if (newScore == null) {
                                            marksMap.remove(ex.id)
                                        } else {
                                            marksMap[ex.id] = clampScore10(newScore)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }

                // --- תחתית ---
                BottomActionBar(
                    session = session,
                    onSave = {
                        val nameOk = commitTraineeNameAndCollapse()
                        if (!nameOk) {
                            Toast.makeText(ctx, "נא להזין שם נבחן לפני שמירה", Toast.LENGTH_SHORT).show()
                            return@BottomActionBar
                        }

                        saveExamDraft(ctx, traineeName.trim(), belt, marksMap)
                        pushRecentTrainee(ctx, traineeName.trim(), 10)
                        saveLastTrainee(ctx, traineeName.trim())
                        hasUnsavedChanges = false
                        Toast.makeText(ctx, "המבחן נשמר", Toast.LENGTH_SHORT).show()
                    },
                    onExportPdf = onExportPdf
                )
            }
        }

        if (showResumeDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showResumeDialog = false },
                title = { Text("מבחן שמור נמצא") },
                text = { Text("נמצא מבחן שמור מהפעם האחרונה. להמשיך ממנו או להתחיל מבחן חדש?") },
                confirmButton = {
                    Button(onClick = {
                        // ✅ המשך מבחן אחרון
                        marksMap.clear()
                        marksMap.putAll(pendingLoadedDraft)
                        hasUnsavedChanges = false
                        showResumeDialog = false
                    }) { Text("המשך") }
                },
                dismissButton = {
                    Button(onClick = {
                        // ✅ מבחן חדש
                        marksMap.clear()

                        val sp = ctx.getSharedPreferences("kmi_internal_exam_drafts", Context.MODE_PRIVATE)
                        sp.edit().remove(draftKey(traineeName.trim(), belt)).apply()

                        onTraineeNameChange("")
                        showTraineeNameBox = true
                        resumeCheckedKey = null

                        hasUnsavedChanges = false
                        showResumeDialog = false
                    }) { Text("מבחן חדש") }
                }
            )
        }

        if (showExitDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("שמירת מבחן") },
                text = { Text("האם ברצונך לשמור את המבחן לפני היציאה?") },
                confirmButton = {
                    Button(onClick = {
                        val name = traineeName.trim()
                        if (name.isNotBlank()) {
                            saveExamDraft(ctx, name, belt, marksMap)
                            pushRecentTrainee(ctx, name, 10)
                            saveLastTrainee(ctx, name)
                        }
                        hasUnsavedChanges = false
                        showExitDialog = false
                        onBack()
                    }) { Text("שמור") }
                },
                dismissButton = {
                    Button(onClick = {
                        showExitDialog = false
                        onBack()
                    }) { Text("צא בלי לשמור") }
                }
            )
        }
    }
}

@Composable
private fun TopicHeader(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE0F2FE)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (expanded) "▲" else "▼",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalExamEntryScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current

    var traineeName by rememberSaveable { mutableStateOf("") }
    var currentBelt by remember { mutableStateOf(Belt.YELLOW) }

    // ✅ רשימת 10 נבחנים אחרונים
    var recentTrainees by remember { mutableStateOf(loadRecentTrainees(ctx)) }
    var expanded by remember { mutableStateOf(false) }

    // ✅ מפתח סשן: מאתחל את InternalExamScreen רק כשבוחרים "נבחן חדש" / נבחן מהרשימה
    var traineeSessionKey by rememberSaveable { mutableStateOf(0) }

    // ✅ ברירת מחדל: אם אין שם — נטען את האחרון
    LaunchedEffect(Unit) {
        val last = loadLastTrainee(ctx).trim()
        if (last.isNotBlank() && traineeName.isBlank()) {
            traineeName = last
        }
    }

    val exercises = remember(currentBelt) {
        buildInternalExamExercisesFromContent(currentBelt)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ✅ Dropdown נבחנים אחרונים + "נבחן חדש"
        // ✅ מופיע רק כשאין שם נבחן (אחרי בחירה נעלם כדי לפנות מקום לתרגילים)
        if (traineeName.isBlank()) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = traineeName,
                    onValueChange = { traineeName = it },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    singleLine = true,
                    label = { Text("בחר נבחן") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // ✅ נבחן חדש
                    DropdownMenuItem(
                        text = { Text("➕ נבחן חדש…") },
                        onClick = {
                            expanded = false
                            traineeName = ""
                            traineeSessionKey++ // ✅ אתחול מסך מבחן חדש
                        }
                    )

                    if (recentTrainees.isNotEmpty()) {
                        Divider()
                    }

                    recentTrainees.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                expanded = false
                                traineeName = name
                                traineeSessionKey++ // ✅ אתחול מסך מבחן לנבחן שנבחר
                            }
                        )
                    }
                }
            }
        }

        // ✅ המסך עצמו (מאותחל רק כשבוחרים נבחן מהרשימה/חדש)
        key(traineeSessionKey) {
            InternalExamScreen(
                traineeName = traineeName,
                onTraineeNameChange = { traineeName = it },
                belt = currentBelt,
                exercises = exercises,
                onBeltChange = { newBelt -> currentBelt = newBelt },
                onBack = onBack
            )
        }
    }

    // ✅ רענון הרשימה כששומרים/משנים שם
    LaunchedEffect(traineeName) {
        recentTrainees = loadRecentTrainees(ctx)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeltSelector(
    currentBelt: Belt,
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

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = currentBelt.heb,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    text = "חגורה במבחן",
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            },
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF60A5FA),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            belts.forEach { b ->
                DropdownMenuItem(
                    text = { Text(b.heb) },
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
    marksMap: Map<String, Int>
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

    val summaryText = when {
        percent >= 85 -> "עבר בהצלחה רבה"
        percent >= 70 -> "עבר בהצלחה"
        percent >= 50 -> "בינוני – נדרש שיפור"
        else          -> "לא עבר את המבחן"
    }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp), // ✅ פחות גובה
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp) // ✅ פחות גובה
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "סיכום מבחן",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            // ✅ תמיד מוצג (קומפקטי)
            Text(
                // ✅ מצטבר 0–10
                text = "מצטבר: ${totalScore10.coerceIn(0.0, 10.0).toScoreString()} / 10  (${percent}%)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall
            )

            // ✅ פירוט רק כשפותחים
            if (expanded && beltScores.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                beltScores.forEach { (belt, score) ->
                    Text(
                        // ✅ לכל חגורה: 0–10
                        text = "${belt.heb}: ${score.score10.coerceIn(0.0, 10.0).toScoreString()} / 10 (${score.percent}%)",
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
    onScoreChange: (Int?) -> Unit // null = לא סומן
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // ✅ ציון 0..10
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val scroll = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scroll),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (v in 0..10) {
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
    val bg = if (selected) base.copy(alpha = 0.95f) else base.copy(alpha = 0.40f)

    Surface(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        color = bg,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.5.dp,
            color = if (selected) base else base.copy(alpha = 0.85f)
        ),
        shadowElevation = 0.dp
    ) {
        OutlinedNumberText(
            text = value.toString(),
            fontSizeSp = 13,
            strokeWidthPx = 5f
        )
    }
}

@Composable
private fun BottomActionBar(
    session: InternalExamSession,
    onSave: () -> Unit,
    onExportPdf: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val score10 = if (session.maxScore == 0.0) 0.0 else (session.totalScore / session.maxScore) * 10.0

            Text(
                text = "ציון: ${score10.coerceIn(0.0, 10.0).toScoreString()} / 10  (${session.percent}%)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(12.dp))

            Row(
                modifier = Modifier.width(220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) { Text("שמור", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }

                Button(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f)
                ) { Text("שתף", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
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

    topicTitles.forEach { topicTitle ->
        val rawItems = itemsForTopicFlattenInternal(belt, topicTitle)
        if (rawItems.isEmpty()) return@forEach

        rawItems.forEach { rawItem ->
            val cleanName = rawItem
                .substringAfter("::")
                .substringAfter(":")
                .trim()
                .ifBlank { rawItem.trim() }

            val subTopicTitle = findSubTopicTitleForItemInternal(belt, topicTitle, cleanName)

            val topicLabel =
                if (!subTopicTitle.isNullOrBlank() && subTopicTitle != topicTitle)
                    "${topicTitle} – ${subTopicTitle}"
                else
                    topicTitle

            val stableId = ContentRepo.makeItemKey(
                belt = belt,
                topicTitle = topicTitle,
                subTopicTitle = subTopicTitle,
                itemTitle = cleanName
            )

            result += ExamExerciseItem(
                id = stableId,
                belt = belt,
                topic = topicLabel,
                name = cleanName
            )
        }
    }

    return result
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

private fun draftKey(traineeName: String, belt: Belt): String =
    "draft_${traineeName.trim()}_${belt.name}"

private fun saveExamDraft(
    context: Context,
    traineeName: String,
    belt: Belt,
    marksMap: Map<String, Int>
) {
    val sp = context.getSharedPreferences("kmi_internal_exam_drafts", Context.MODE_PRIVATE)

    val obj = JSONObject()
    marksMap.forEach { (id, score) ->
        obj.put(id, clampScore10(score))
    }

    sp.edit()
        .putString(draftKey(traineeName, belt), obj.toString())
        .apply()
}

private fun loadExamDraft(
    context: Context,
    traineeName: String,
    belt: Belt
): Map<String, Int> {
    val sp = context.getSharedPreferences("kmi_internal_exam_drafts", Context.MODE_PRIVATE)
    val raw = sp.getString(draftKey(traineeName, belt), null) ?: return emptyMap()

    return runCatching {
        val obj = JSONObject(raw)
        val out = mutableMapOf<String, Int>()
        val it = obj.keys()
        while (it.hasNext()) {
            val id = it.next()
            val v = obj.optInt(id, -1)
            if (v >= 0) out[id] = clampScore10(v)
        }
        out
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

private const val PREFS_EXAM_RECENTS = "kmi_internal_exam_recents"
private const val KEY_RECENT_TRAINEES = "recent_trainees"
private const val KEY_LAST_TRAINEE = "last_trainee"

private fun saveLastTrainee(context: Context, name: String) {
    context.getSharedPreferences(PREFS_EXAM_RECENTS, Context.MODE_PRIVATE)
        .edit().putString(KEY_LAST_TRAINEE, name.trim()).apply()
}

private fun loadLastTrainee(context: Context): String {
    return context.getSharedPreferences(PREFS_EXAM_RECENTS, Context.MODE_PRIVATE)
        .getString(KEY_LAST_TRAINEE, "") ?: ""
}

private fun loadRecentTrainees(context: Context, limit: Int = 10): List<String> {
    val raw = context.getSharedPreferences(PREFS_EXAM_RECENTS, Context.MODE_PRIVATE)
        .getString(KEY_RECENT_TRAINEES, null) ?: return emptyList()

    return runCatching {
        val arr = JSONObject(raw).optJSONArray("list")
        if (arr == null) emptyList() else buildList {
            for (i in 0 until minOf(arr.length(), limit)) {
                val s = arr.optString(i).trim()
                if (s.isNotBlank()) add(s)
            }
        }
    }.getOrDefault(emptyList())
}

private fun pushRecentTrainee(context: Context, name: String, limit: Int = 10) {
    val clean = name.trim()
    if (clean.isBlank()) return

    val current = loadRecentTrainees(context, limit = 50).toMutableList()
    current.removeAll { it.equals(clean, ignoreCase = true) }
    current.add(0, clean)

    val trimmed = current.take(limit)
    val obj = JSONObject()
    val arr = org.json.JSONArray()
    trimmed.forEach { arr.put(it) }
    obj.put("list", arr)

    context.getSharedPreferences(PREFS_EXAM_RECENTS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_RECENT_TRAINEES, obj.toString())
        .apply()
}
