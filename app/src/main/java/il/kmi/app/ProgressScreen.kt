// File: app/src/main/java/il/kmi/app/screens/ProgressScreen.kt
package il.kmi.app.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import il.kmi.app.KmiViewModel
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.ext.lightColor
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    vm: KmiViewModel,
    onBack: () -> Unit
) {
    // נשאר לצורך שימושים אחרים (PDF/סטטוסים). לא חייבים להשתמש בו כאן.
    val progress by vm.progress

    val context = LocalContext.current

    // === SP של ההחרגות בדיוק כמו במסך ההגדרות ===
    val spSettings = remember {
        context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
    }

    // --- פונקציות עזר (נשארות מחוץ ל-produceState) ---
    fun beltTitle(b: Belt): String = when (b) {
        Belt.YELLOW -> "חגורה: צהובה"
        Belt.ORANGE -> "חגורה: כתומה"
        Belt.GREEN  -> "חגורה: ירוקה"
        Belt.BLUE   -> "חגורה: כחולה"
        Belt.BROWN  -> "חגורה: חומה"
        Belt.BLACK  -> "חגורה: שחורה"
        else        -> "חגורה"
    }

    // ✅ נרמול עברי (כדי לייצר מפתח עקבי, בדיוק כמו במסכי הסימון)
    fun String.normHeb(): String = this
        .replace("\u200F", "") // RLM
        .replace("\u200E", "") // LRM
        .replace("\u00A0", " ") // NBSP -> space
        .replace(Regex("[\u0591-\u05C7]"), "") // ניקוד
        .replace('\u05BE', '-') // מקאף עברי ־
        .replace('\u2010', '-') // Hyphen
        .replace('\u2011', '-') // Non-Breaking Hyphen
        .replace('\u2012', '-') // Figure Dash
        .replace('\u2013', '-') // En Dash
        .replace('\u2014', '-') // Em Dash
        .replace('\u2015', '-') // Horizontal Bar
        .replace('\u2212', '-') // Minus
        .replace(Regex("\\s*-\\s*"), "-")
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()

    fun canonicalKeyFor(rawItem: String): String =
        ExerciseTitleFormatter.displayName(rawItem).trim().normHeb()

    fun displayName(rawItem: String): String =
        ExerciseTitleFormatter.displayName(rawItem).trim().ifBlank { rawItem.trim() }

    data class BeltRow(val belt: Belt, val percent: Int, val done: Int, val total: Int)

    val beltsToShow = remember { Belt.values().filter { it != Belt.WHITE } }
    val sharedBelts = remember { il.kmi.shared.catalog.KmiCatalogFacade.listBelts() }

    // ⬇️ חישוב אמיתי של התקדמות מתוך shared ProgressFacade
    // בלי remember בתוך produceState, ובלי produceState מקונן.
    val beltsData: List<BeltRow> by produceState(
        initialValue = emptyList(),
        key1 = Unit
    ) {
        val rows = il.kmi.shared.progress.ProgressFacade.computeBeltsProgress(
            beltsToScan = sharedBelts,
            excludedProvider = il.kmi.shared.progress.ProgressFacade.ExcludedProvider { beltId, topicTitle, rawItem, disp ->
                val excluded =
                    spSettings.getStringSet("excluded_${beltId}_${topicTitle}", emptySet()) ?: emptySet()
                (rawItem in excluded) || (disp in excluded)
            },
            statusProvider = il.kmi.shared.progress.ProgressFacade.StatusProvider { beltId, topicTitle, canonicalItemKey ->
                val appBelt = Belt.fromId(beltId) ?: return@StatusProvider null

                // ❗️פתרון קומפילציה: getItemStatusNullable היא suspend אצלך
                // ולכן אנחנו קוראים לה דרך runBlocking כדי להתאים ל-StatusProvider (לא-suspend).
                // אם תרצה, נשדרג אחר כך ל-cache אסינכרוני כדי לא לחסום.
                runBlocking {
                    vm.getItemStatusNullable(appBelt, topicTitle, canonicalItemKey)
                }
            },
            canonicalKeyFor = { rawItem -> canonicalKeyFor(rawItem) },
            displayNameFor = { rawItem -> displayName(rawItem) }
        )

        value = rows.mapNotNull { r ->
            val appBelt = Belt.fromId(r.beltId) ?: return@mapNotNull null
            BeltRow(
                belt = appBelt,
                percent = r.percent,
                done = r.done,
                total = r.total
            )
        }
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "מד התקדמות",
                onBack = onBack,
                extraActions = {
                    IconButton(onClick = {
                        val items = beltsData.map { row ->
                            il.kmi.shared.report.BeltProgress(
                                title = beltTitle(row.belt),
                                percent = row.percent,
                                colorHex = String.format("#%06X", 0xFFFFFF and row.belt.color.toArgb()),
                                lightColorHex = String.format("#%06X", 0xFFFFFF and row.belt.lightColor.toArgb())
                            )
                        }

                        val html = il.kmi.shared.report.ProgressReport.buildHtml(items)
                        val pf = il.kmi.shared.Platform.saveTextAsFile(
                            filename = "progress_report.html",
                            mimeType = "text/html",
                            contents = html
                        )

                        val file = File(pf.path)
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            context.packageName + ".fileprovider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = pf.mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "שתף מד התקדמות (HTML)"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "שיתוף")
                    }
                },
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (beltsData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("אין עדיין נתוני התקדמות", fontWeight = FontWeight.SemiBold)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    items(beltsData) { row ->
                        ProgressCard(
                            belt = row.belt,
                            percent = row.percent,
                            done = row.done,
                            total = row.total
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    belt: Belt,
    percent: Int,
    done: Int,
    total: Int
) {
    val progressAnim by animateFloatAsState(
        targetValue = if (total == 0) 0f else done.toFloat() / total.toFloat(),
        label = "progressAnim"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(2.dp, belt.color.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = belt.lightColor.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = belt.color.copy(alpha = 0.9f),
                    contentColor = Color.White,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "$percent%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "חגורה: ${belt.heb}",
                    fontWeight = FontWeight.Bold,
                    color = belt.color
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(color = belt.color, shape = CircleShape)
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(belt.color)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${percent}% ($done מתוך $total)",
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// √ נשמר – שימוש בשדות האחוזים ליצוא PDF, מעוצב כמו הכרטיסים במסך
fun createProgressPdf(
    dir: File,
    progress: Map<Belt, Int>,
    context: Context
): File {
    val width = 595
    val height = 842
    val margin = 32f

    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 16f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }

    val smallText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.DKGRAY
        textSize = 12.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.RIGHT
    }

    canvas.drawText("דו״ח מד התקדמות", width - margin, margin + 8f, titlePaint)

    var y = margin + 36f

    val cardHeight = 92f
    val gap = 14f
    val pillH = 22f
    val radius = 16f
    val barH = 12f

    val beltsToShow = Belt.values().filter { it != Belt.WHITE }

    beltsToShow.forEach { belt ->
        val pct = progress[belt] ?: 0

        val left = margin
        val right = width - margin
        val top = y
        val bottom = y + cardHeight
        val rect = RectF(left, top, right, bottom)

        val light = belt.lightColor.toArgb()
        val lightWithAlpha = ColorUtils.setAlphaComponent(light, (0.55f * 255).toInt())
        fill.color = lightWithAlpha
        fill.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, radius, radius, fill)

        val pillPad = 10f
        val pillText = "$pct%"
        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val pillW = pillTextPaint.measureText(pillText) + 18f
        val pillRect = RectF(
            left + pillPad,
            top + pillPad,
            left + pillPad + pillW,
            top + pillPad + pillH
        )

        fill.color = ColorUtils.setAlphaComponent(belt.color.toArgb(), (0.90f * 255).toInt())
        canvas.drawRoundRect(pillRect, pillH, pillH, fill)

        val pillTextY = pillRect.centerY() - (pillTextPaint.descent() + pillTextPaint.ascent()) / 2
        canvas.drawText(pillText, pillRect.centerX(), pillTextY, pillTextPaint)

        val barLeft = left + 12f
        val barRight = right - 12f
        val barTop = top + 44f
        val barBottom = barTop + barH

        fill.color = ColorUtils.setAlphaComponent(android.graphics.Color.BLACK, (0.08f * 255).toInt())
        canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), barH, barH, fill)

        val fillW = (barRight - barLeft) * (pct / 100f)
        if (fillW > 0f) {
            fill.color = belt.color.toArgb()
            canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + fillW, barBottom), barH, barH, fill)
        }

        smallText.color = android.graphics.Color.DKGRAY
        canvas.drawText(
            "$pct% (חישוב לפי פריטים שסומנו באפליקציה)",
            barRight,
            barBottom + 18f,
            smallText
        )

        y = bottom + gap
    }

    doc.finishPage(page)

    val file = File(dir, "progress_report.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()

    return file
}
