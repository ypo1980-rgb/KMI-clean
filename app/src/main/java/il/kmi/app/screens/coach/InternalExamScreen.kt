package il.kmi.app.screens.coach

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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

// ======================
// מודלים ולוגיקה
// ======================

enum class ExamMark(val display: String, val score: Double) {
    PASS("✓", 1.0),      // יודע = 1 נק'
    PARTIAL("✓̶", 0.5),  // יודע חלקית = 0.5 נק'
    FAIL("✗", 0.0);      // לא יודע = 0 נק'
}

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
    val marks: List<ExamMark?>,
) {
    val maxScore: Double get() = exercises.size * ExamMark.PASS.score

    val totalScore: Double get() =
        marks.sumOf { it?.score ?: 0.0 }

    val percent: Int
        get() = if (maxScore == 0.0) 0
        else ((totalScore / maxScore) * 100).toInt()

    val summaryText: String
        get() = when {
            percent >= 85 -> "עבר בהצלחה רבה"
            percent >= 70 -> "עבר בהצלחה"
            percent >= 50 -> "בינוני – נדרש שיפור"
            else          -> "לא עבר את המבחן"
        }
}

// תוצאה לכל חגורה (לסיכומים לפי חגורה)
private data class BeltScore(
    val total: Double,
    val max: Double
) {
    val percent: Int
        get() = if (max == 0.0) 0 else ((total / max) * 100.0).toInt()
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

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val paintTitle = Paint().apply {
                isAntiAlias = true
                textSize = 18f
                typeface = android.graphics.Typeface.create(
                    "sans-serif-medium",
                    android.graphics.Typeface.BOLD
                )
            }
            val paintText = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                typeface = android.graphics.Typeface.create(
                    "sans-serif",
                    android.graphics.Typeface.NORMAL
                )
            }
            val paintHeader = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                typeface = android.graphics.Typeface.create(
                    "sans-serif-medium",
                    android.graphics.Typeface.BOLD
                )
            }

            var y = 40f

            // כותרת
            canvas.drawText("דו\"ח מבחן פנימי – ${session.belt.heb}", 40f, y, paintTitle)
            y += 30f

            val dateStr = session.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            canvas.drawText("שם מתאמן: ${session.traineeName}", 40f, y, paintText)
            y += 18f
            canvas.drawText(
                "ציון: ${session.totalScore.toScoreString()} / " +
                        "${session.maxScore.toScoreString()}  (${session.percent}%)",
                40f,
                y,
                paintText
            )
            y += 18f
            canvas.drawText("תאריך מבחן: $dateStr", 40f, y, paintText)
            y += 18f
            canvas.drawText("סיכום: ${session.summaryText}", 40f, y, paintText)
            y += 30f

            canvas.drawText("תרגילים:", 40f, y, paintHeader)
            y += 22f

            var currentTopic: String? = null

            // ✅ מציירים רק תרגילים שסומנו (mark != null)
            session.exercises.forEachIndexed { index, ex ->
                val markObj = session.marks.getOrNull(index)
                if (markObj == null) return@forEachIndexed   // דילוג על תרגילים בלי סימון

                // מעבר דף
                if (y > 780f) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                }

                if (currentTopic != ex.topic) {
                    currentTopic = ex.topic
                    canvas.drawText("נושא: ${ex.topic}", 40f, y, paintHeader)
                    y += 20f
                }

                val line = "- ${markObj.display}  ${ex.name}"
                canvas.drawText(line, 60f, y, paintText)
                y += 16f
            }

            document.finishPage(page)

            val dir = File(context.cacheDir, "internal_exam")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "exam_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
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
            return null
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
    var pendingLoadedDraft by remember { mutableStateOf<Map<String, ExamMark>>(emptyMap()) }

    // ✅ כדי שלא נפתח דיאלוג על כל אות בזמן שמקלידים שם
    var resumeCheckedKey by remember { mutableStateOf<String?>(null) }

    // 🟢 מפה גלובלית – נשמרת גם כשעוברים חגורות
    val marksMap = remember { mutableStateMapOf<String, ExamMark>() }

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
                                val markForThis = marksMap[ex.id]

                                ExerciseRow(
                                    name = ex.name,
                                    mark = markForThis,
                                    onMarkChange = { newMark ->
                                        hasUnsavedChanges = true

                                        if (newMark == null) {
                                            marksMap.remove(ex.id)
                                        } else {
                                            marksMap[ex.id] = newMark
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
    marksMap: Map<String, ExamMark>
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val orderedBelts = beltsUpTo(currentBelt)

    // ניקוד לכל חגורה עד החגורה הנוכחית – רק מתוך תרגילים שסומנו בפועל
    val beltScores: Map<Belt, BeltScore> = orderedBelts.associateWith { belt ->
        val exercisesForBelt = buildInternalExamExercisesFromContent(belt)
        var total = 0.0
        var max = 0.0
        exercisesForBelt.forEach { ex ->
            val mark = marksMap[ex.id]
            if (mark != null) {
                max += ExamMark.PASS.score
                total += mark.score
            }
        }
        BeltScore(total = total, max = max)
    }

    val totalScore = beltScores.values.sumOf { it.total }
    val maxScore = beltScores.values.sumOf { it.max }
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
                text = "מצטבר: ${totalScore.toScoreString()} / ${maxScore.toScoreString()}  (${percent}%)",
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
                        text = "${belt.heb}: ${score.total.toScoreString()} / ${score.max.toScoreString()} (${score.percent}%)",
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
    mark: ExamMark?,
    onMarkChange: (ExamMark?) -> Unit
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✔ יודע (ימין)
                MarkChip(
                    label = "יודע",
                    selected = mark == ExamMark.PASS,
                    color = Color(0xFF81C784),
                    modifier = Modifier.weight(1f)
                ) {
                    onMarkChange(if (mark == ExamMark.PASS) null else ExamMark.PASS)
                }

                // יודע חלקי (אמצע)
                MarkChip(
                    label = "יודע חלקי",
                    selected = mark == ExamMark.PARTIAL,
                    color = Color(0xFFFFB74D),
                    modifier = Modifier.weight(1f)
                ) {
                    onMarkChange(if (mark == ExamMark.PARTIAL) null else ExamMark.PARTIAL)
                }

                // ❌ לא יודע (שמאל)
                MarkChip(
                    label = "לא יודע",
                    selected = mark == ExamMark.FAIL,
                    color = Color(0xFFE57373),
                    modifier = Modifier.weight(1f)
                ) {
                    onMarkChange(if (mark == ExamMark.FAIL) null else ExamMark.FAIL)
                }
            }
        }
    }
}

@Composable
private fun MarkChip(
    label: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        label = {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.25f),
            selectedLabelColor = Color.Black,
            containerColor = Color(0xFFF5F5F5)
        )
    )
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
            Text(
                text = "ציון: ${session.totalScore.toScoreString()} / ${session.maxScore.toScoreString()} (${session.percent}%)",
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
    marksMap: Map<String, ExamMark>
) {
    val sp = context.getSharedPreferences("kmi_internal_exam_drafts", Context.MODE_PRIVATE)

    val obj = JSONObject()
    marksMap.forEach { (id, mark) ->
        obj.put(id, mark.name) // PASS / PARTIAL / FAIL
    }

    sp.edit()
        .putString(draftKey(traineeName, belt), obj.toString())
        .apply()
}

private fun loadExamDraft(
    context: Context,
    traineeName: String,
    belt: Belt
): Map<String, ExamMark> {
    val sp = context.getSharedPreferences("kmi_internal_exam_drafts", Context.MODE_PRIVATE)
    val raw = sp.getString(draftKey(traineeName, belt), null) ?: return emptyMap()

    return runCatching {
        val obj = JSONObject(raw)
        val out = mutableMapOf<String, ExamMark>()
        val it = obj.keys()
        while (it.hasNext()) {
            val id = it.next()
            val markName = obj.getString(id)
            out[id] = ExamMark.valueOf(markName)
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
