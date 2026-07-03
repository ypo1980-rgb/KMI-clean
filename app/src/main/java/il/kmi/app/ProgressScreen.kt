package il.kmi.app.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    vm: KmiViewModel,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    onOpenCarousel: (() -> Unit)? = null
) {
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

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun cleanItemForSummary(topic: String, item: String): String {
        var s = item.trim()

        if (topic.isNotBlank() && s.startsWith("$topic::")) {
            s = s.removePrefix("$topic::").trim()
        }

        return s.replace(Regex("\\s+"), " ").trim()
    }

    fun identityStatusIdFor(
        belt: Belt,
        topicKey: String,
        topicTitle: String,
        index: Int,
        rawItem: String
    ): String {
        val cleanOriginal = cleanItemForSummary(topicTitle, rawItem).trim()

        val resolved = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = cleanOriginal,
            topicKey = topicKey
        )

        return if (resolved.isKnown) {
            resolved.id
        } else {
            "${resolved.id}_row_$index"
        }
    }

    fun displayName(rawItem: String): String {
        val formatted = ExerciseTitleFormatter
            .displayName(rawItem)
            .toString()
            .trim()

        return if (formatted.isNotBlank() && formatted != "null") {
            formatted
        } else {
            rawItem.trim()
        }
    }

    data class BeltRow(val belt: Belt, val percent: Int, val done: Int, val total: Int)

    data class ProgressExerciseRow(
        val sourceTopicTitle: String,
        val statusTopicKey: String,
        val rawItem: String,
        val indexInStatusGroup: Int
    )

    val beltsToShow: List<Belt> = remember {
        il.kmi.app.domain.ContentRepo
            .listBeltsInOrder()
            .filter { belt: Belt -> belt != Belt.WHITE }
    }

// ContentRepo הוא מקור האמת לרשימות התרגילים במסכים.
    val marksVersion by vm.marksVersion.collectAsState()

    val beltsData: List<BeltRow> by produceState(
        initialValue = emptyList(),
        key1 = marksVersion
    ) {
        value = withContext(Dispatchers.Default) {
            il.kmi.app.domain.ContentRepo.initIfNeeded()

            val rows = mutableListOf<BeltRow>()

        suspend fun countKnownLikeSummaryScreen(belt: Belt): Int {
            val knownIds = mutableSetOf<String>()

            suspend fun checkKnown(
                topicKey: String,
                topicTitle: String,
                index: Int,
                rawItem: String
            ) {
                val statusId = identityStatusIdFor(
                    belt = belt,
                    topicKey = topicKey,
                    topicTitle = topicTitle,
                    index = index,
                    rawItem = rawItem
                )

                val legacyStatusId =
                    "status_${belt.id}_${topicKey}_${index}_${normalizeStatusPart(rawItem)}"

                val topicSnap = vm.getTopicStatusSnapshot(
                    belt = belt,
                    topic = topicKey
                )

                val value =
                    topicSnap[statusId]
                        ?: topicSnap[legacyStatusId]
                        ?: vm.getItemStatusNullable(
                            belt = belt,
                            topic = topicKey,
                            item = statusId
                        )
                        ?: vm.getItemStatusNullable(
                            belt = belt,
                            topic = topicKey,
                            item = legacyStatusId
                        )

                if (value == true) {
                    knownIds += "$topicKey::$statusId"
                }
            }

            val topicTitles: List<String> = il.kmi.app.domain.ContentRepo
                .listTopicTitles(belt)
                .map { topicTitle: String -> topicTitle.trim() }
                .filter { topicTitle: String -> topicTitle.isNotBlank() }

            for (topicTitle: String in topicTitles) {
                val directItems: List<String> = SharedContentRepo.getAllItemsFor(
                    belt = belt,
                    topicTitle = topicTitle,
                    subTopicTitle = null
                )
                    .map { rawItem: String -> rawItem.trim() }
                    .filter { rawItem: String -> rawItem.isNotBlank() }
                    .distinct()

                directItems.forEachIndexed { index: Int, rawItem: String ->
                    checkKnown(
                        topicKey = topicTitle,
                        topicTitle = topicTitle,
                        index = index,
                        rawItem = rawItem
                    )
                }

                suspend fun addSubTopicLikeSummary(
                    subTopic: SharedContentRepo.SubTopic
                ) {
                    val cleanSubTopicTitle = subTopic.title.trim()
                    val statusTopicKey = "${topicTitle}__${cleanSubTopicTitle}"

                    val subItems: List<String> = subTopic.items
                        .map { rawItem: String -> rawItem.trim() }
                        .filter { rawItem: String -> rawItem.isNotBlank() }
                        .distinct()

                    subItems.forEachIndexed { index: Int, rawItem: String ->
                        checkKnown(
                            topicKey = statusTopicKey,
                            topicTitle = topicTitle,
                            index = index,
                            rawItem = rawItem
                        )
                    }

                    subTopic.subTopics.forEach { nestedSubTopic: SharedContentRepo.SubTopic ->
                        addSubTopicLikeSummary(nestedSubTopic)
                    }
                }

                val subTopics: List<SharedContentRepo.SubTopic> =
                    SharedContentRepo.getSubTopicsFor(
                        belt = belt,
                        topicTitle = topicTitle
                    )

                subTopics.forEach { subTopic: SharedContentRepo.SubTopic ->
                    addSubTopicLikeSummary(subTopic)
                }
            }

            return knownIds.size
        }

        for (belt in beltsToShow) {
            var done = 0
            var total = 0

            val countedStatusIds = mutableSetOf<String>()

            val topicTitles: List<String> = il.kmi.app.domain.ContentRepo
                .listTopicTitles(belt)
                .map { topicTitle: String -> topicTitle.trim() }
                .filter { topicTitle: String -> topicTitle.isNotBlank() }

            for (topicTitle in topicTitles) {
                val progressRows = mutableListOf<ProgressExerciseRow>()

                val directItems: List<String> = il.kmi.app.domain.ContentRepo.listItemTitles(
                    belt = belt,
                    topicTitle = topicTitle,
                    subTopicTitle = null
                )
                    .map { rawItem: String -> rawItem.trim() }
                    .filter { rawItem: String -> rawItem.isNotBlank() }
                    .distinct()

                directItems.forEachIndexed { index: Int, rawItem: String ->
                    progressRows += ProgressExerciseRow(
                        sourceTopicTitle = topicTitle,
                        statusTopicKey = topicTitle,
                        rawItem = rawItem,
                        indexInStatusGroup = index
                    )
                }

                val subTopicTitles: List<String> = il.kmi.app.domain.ContentRepo
                    .listSubTopicTitles(
                        belt = belt,
                        topicTitle = topicTitle
                    )
                    .map { subTopicTitle: String -> subTopicTitle.trim() }
                    .filter { subTopicTitle: String -> subTopicTitle.isNotBlank() }

                for (subTopicTitle in subTopicTitles) {
                    val subItems: List<String> = il.kmi.app.domain.ContentRepo.listItemTitles(
                        belt = belt,
                        topicTitle = topicTitle,
                        subTopicTitle = subTopicTitle
                    )
                        .map { rawItem: String -> rawItem.trim() }
                        .filter { rawItem: String -> rawItem.isNotBlank() }
                        .distinct()

                    subItems.forEachIndexed { index: Int, rawItem: String ->
                        progressRows += ProgressExerciseRow(
                            sourceTopicTitle = topicTitle,
                            statusTopicKey = "${topicTitle}__${subTopicTitle}",
                            rawItem = rawItem,
                            indexInStatusGroup = index
                        )
                    }
                }

                val uniqueProgressRows: List<ProgressExerciseRow> =
                    progressRows.distinctBy { row: ProgressExerciseRow ->
                        row.rawItem
                            .replace("\u200F", "")
                            .replace("\u200E", "")
                            .replace("\u00A0", " ")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                    }

                val snapshotsByStatusTopicKey: Map<String, Map<String, Boolean?>> =
                    uniqueProgressRows
                        .map { row: ProgressExerciseRow -> row.statusTopicKey }
                        .distinct()
                        .associateWith { statusTopicKey: String ->
                            vm.getTopicStatusSnapshot(belt, statusTopicKey)
                        }

                for (row in uniqueProgressRows) {
                    val rawItem = row.rawItem
                    val display = displayName(rawItem)

                    val excluded = spSettings.getStringSet(
                        "excluded_${belt.id}_${row.statusTopicKey}",
                        emptySet()
                    ) ?: emptySet()

                    if (rawItem in excluded || display in excluded) {
                        continue
                    }

                    val identityStatusId = identityStatusIdFor(
                        belt = belt,
                        topicKey = row.statusTopicKey,
                        topicTitle = row.sourceTopicTitle,
                        index = row.indexInStatusGroup,
                        rawItem = rawItem
                    )

                    if (!countedStatusIds.add("${row.statusTopicKey}::$identityStatusId")) {
                        continue
                    }

                    total++

                    val legacyStatusId =
                        "status_${belt.id}_${row.statusTopicKey}_${row.indexInStatusGroup}_${normalizeStatusPart(rawItem)}"

                    val canonicalId = il.kmi.app.domain.CanonicalIds.canonicalFor(
                        belt = belt,
                        topicTitle = row.sourceTopicTitle,
                        displayItem = rawItem
                    )

                    val canonicalDisplayKey = canonicalKeyFor(rawItem)

                    val statusKeys = listOf(
                        identityStatusId,
                        legacyStatusId,
                        canonicalId,
                        rawItem,
                        display,
                        canonicalDisplayKey
                    )
                        .map { key: String -> key.trim() }
                        .filter { key: String -> key.isNotBlank() }
                        .distinct()

                    val topicSnap: Map<String, Boolean?> =
                        snapshotsByStatusTopicKey[row.statusTopicKey].orEmpty()

                    val summaryTopicSnap: Map<String, Boolean?> =
                        vm.getTopicStatusSnapshot(
                            belt = belt,
                            topic = row.statusTopicKey
                        )

                    val summaryStatusId = identityStatusIdFor(
                        belt = belt,
                        topicKey = row.statusTopicKey,
                        topicTitle = row.sourceTopicTitle,
                        index = row.indexInStatusGroup,
                        rawItem = row.rawItem
                    )

                    val summaryLegacyStatusId =
                        "status_${belt.id}_${row.statusTopicKey}_${row.indexInStatusGroup}_${normalizeStatusPart(row.rawItem)}"

                    var summaryValue: Boolean? =
                        summaryTopicSnap[summaryStatusId] ?: summaryTopicSnap[summaryLegacyStatusId]

                    if (summaryValue == null) {
                        summaryValue = vm.getItemStatusNullable(
                            belt = belt,
                            topic = row.statusTopicKey,
                            item = summaryStatusId
                        ) ?: vm.getItemStatusNullable(
                            belt = belt,
                            topic = row.statusTopicKey,
                            item = summaryLegacyStatusId
                        )
                    }

                    if (summaryValue == true) {
                        done++
                    }
                }
            }

            // ✅ לא נוגעים ב-total.
            // את "יודע" מחשבים באותו מנגנון של מסך הסיכום.
            done = countKnownLikeSummaryScreen(belt).coerceIn(0, total)

            val percent = if (total > 0) {
                ((done * 100f) / total).toInt().coerceIn(0, 100)
            } else {
                0
            }

            rows.add(
                BeltRow(
                    belt = belt,
                    percent = percent,
                    done = done,
                    total = total
                )
            )
        }

            rows
        }
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "מד התקדמות",
                onBack = null,
                onHome = onHome,
                showTopHome = false,
            )
        },
        bottomBar = {
            if (onOpenCarousel != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent,
                    shadowElevation = 0.dp
                ) {
                    Button(
                        onClick = onOpenCarousel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2937),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "מעבר למסך התרגילים",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }
                }
            }
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PremiumProgressLoading()

                        Text(
                            text = "טוען נתוני התקדמות...",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3F3A4A),
                            textAlign = TextAlign.Center,
                            fontSize = 19.sp,
                            lineHeight = 23.sp
                        )

                        Text(
                            text = "מסדר את נתוני החגורות שלך",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7A7288),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        )
                    }
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
private fun PremiumProgressLoading() {
    val infiniteTransition = rememberInfiniteTransition(
        label = "premiumProgressLoading"
    )

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1350,
                easing = LinearEasing
            )
        ),
        label = "premiumProgressOuterRotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1850,
                easing = LinearEasing
            )
        ),
        label = "premiumProgressInnerRotation"
    )

    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    rotationZ = outerRotation
                }
                .border(
                    width = 5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF7C3AED),
                            Color(0xFF38BDF8),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(62.dp)
                .graphicsLayer {
                    rotationZ = innerRotation
                }
                .border(
                    width = 4.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFF59E0B),
                            Color(0xFF22C55E),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFFE9D5FF)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFF3E8FF),
                                Color(0xFFE0F2FE)
                            )
                        ),
                        shape = CircleShape
                    )
            )
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

    val readableBeltColor = when (belt) {
        Belt.WHITE -> MaterialTheme.colorScheme.onSurface
        Belt.BLACK -> MaterialTheme.colorScheme.onSurface
        else -> belt.color
    }

    val percentBubbleColor = when (belt) {
        Belt.WHITE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
        else -> belt.color.copy(alpha = 0.9f)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(2.dp, readableBeltColor.copy(alpha = 0.75f)),
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
                    color = percentBubbleColor,
                    contentColor = Color.White,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "$percent%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "חגורה: ${belt.heb.removePrefix("חגורה").trim()}",
                    fontWeight = FontWeight.ExtraBold,
                    color = readableBeltColor,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .background(color = readableBeltColor, shape = CircleShape)
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
                        .background(readableBeltColor)
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "($done מתוך $total)",
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
