package il.kmi.app.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import il.kmi.app.KmiViewModel
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.ext.lightColor
import il.kmi.app.ui.KmiTypography
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

    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val screenBackgroundBrush =
        Brush.verticalGradient(
            colors =
                if (isDarkMode) {
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        Color(0xFF10243A),
                        Color(0xFF0A3657),
                        Color(0xFF041E33)
                    )
                } else {
                    listOf(
                        Color(0xFFF8FBFF),
                        Color(0xFFEAF4FF),
                        Color(0xFFB7DDF7),
                        Color(0xFF1F78B4),
                        Color(0xFF062B4A)
                    )
                }
        )

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "מד התקדמות",
                onBack = null,
                onHome = onHome,
                showTopHome = false,
                showTopShare = false,
                onShare = {
                    val pdfFile = createProgressPdf(
                        dir = File(context.cacheDir, "pdfs").apply { mkdirs() },
                        progress = beltsData.associate { row ->
                            row.belt to row.percent
                        },
                        context = context
                    )

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        pdfFile
                    )

                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_SUBJECT, "מד התקדמות - KAMI")
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    context.startActivity(
                        Intent.createChooser(
                            sendIntent,
                            "שיתוף PDF"
                        )
                    )
                }
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
                            .padding(
                                horizontal = 18.dp,
                                vertical = 10.dp
                            )
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2937),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "מעבר למסך התרגילים",
                            style = KmiTypography.action.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    screenBackgroundBrush
                )
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
                            style = KmiTypography.sectionTitle,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onBackground,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )

                        Text(
                            text = "מסדר את נתוני החגורות שלך",
                            style = KmiTypography.secondary.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2
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

        val isDarkMode =
            MaterialTheme.colorScheme.background
                .luminance() < 0.5f

        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color =
                MaterialTheme
                    .colorScheme
                    .surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color =
                    Color(0xFF7C3AED)
                        .copy(
                            alpha =
                                if (isDarkMode) {
                                    0.65f
                                } else {
                                    0.30f
                                }
                        )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    if (isDarkMode) {
                                        listOf(
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant,
                                            Color(0xFF312E81),
                                            MaterialTheme
                                                .colorScheme
                                                .surface
                                        )
                                    } else {
                                        listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFF3E8FF),
                                            Color(0xFFE0F2FE)
                                        )
                                    }
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
        Belt.WHITE ->
            MaterialTheme.colorScheme
                .onSurface
                .copy(alpha = 0.82f)

        else ->
            belt.color.copy(alpha = 0.9f)
    }

    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val cardBackgroundColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            belt.lightColor.copy(alpha = 0.55f)
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
            containerColor = cardBackgroundColor
        ),
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 0.dp
            )
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
                        style = KmiTypography.metric,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text =
                        "חגורה: ${
                            belt.heb
                                .removePrefix("חגורה")
                                .trim()
                        }",
                    style = KmiTypography.sectionTitle,
                    color = readableBeltColor,
                    textAlign = TextAlign.Right,
                    maxLines = 1,
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
                style = KmiTypography.body.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Right,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
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
    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f

    val document = PdfDocument()
    val page = document.startPage(
        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    )
    val canvas = page.canvas

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val blue = android.graphics.Color.rgb(12, 78, 130)
    val lightBlue = android.graphics.Color.rgb(234, 246, 255)
    val softBlue = android.graphics.Color.rgb(244, 250, 255)
    val borderBlue = android.graphics.Color.rgb(191, 213, 232)
    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(80, 100, 120)

    val regular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun alpha(color: Int, value: Float): Int =
        ColorUtils.setAlphaComponent(color, (value.coerceIn(0f, 1f) * 255).toInt())

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regular,
        align: Paint.Align = Paint.Align.RIGHT
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        this.typeface = typeface
        textAlign = align
    }

    val titlePaint = paint(29f, android.graphics.Color.WHITE, bold)
    val subTitlePaint = paint(14f, android.graphics.Color.WHITE, regular)
    val sectionPaint = paint(17f, blue, bold)
    val labelPaint = paint(10.5f, blue, bold)
    val valuePaint = paint(12.5f, textDark, regular)
    val boldValuePaint = paint(13f, textDark, bold)
    val percentPaint = paint(16f, android.graphics.Color.WHITE, bold, Paint.Align.CENTER)
    val smallPaint = paint(9f, textMuted, regular)

    fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 12f,
        stroke: Boolean = false,
        strokeWidth: Float = 1.2f
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
            this.strokeWidth = strokeWidth
        }
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, p)
    }

    fun drawCenteredText(
        text: String,
        x: Float,
        centerY: Float,
        paint: Paint
    ) {
        val y = centerY - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, x, y, paint)
    }

    fun drawKmiLogo(cx: Float, cy: Float, radius: Float) {
        val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy }
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            typeface = bold
            textSize = radius * 0.62f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawCircle(cx, cy, radius, outer)
        canvas.drawCircle(cx, cy, radius - 4f, inner)
        canvas.drawText("KAMI", cx, cy + radius * 0.22f, text)
    }

    fun beltPdfColor(belt: Belt): Int {
        return when (belt) {
            Belt.BLACK -> android.graphics.Color.rgb(35, 35, 35)
            Belt.BROWN -> android.graphics.Color.rgb(121, 85, 72)
            else -> belt.color.toArgb()
        }
    }

    fun beltPdfLightColor(belt: Belt): Int {
        return when (belt) {
            Belt.BLACK -> android.graphics.Color.rgb(229, 231, 235)
            Belt.BROWN -> android.graphics.Color.rgb(239, 224, 214)
            else -> belt.lightColor.toArgb()
        }
    }

    fun drawHeader() {
        canvas.drawColor(android.graphics.Color.WHITE)

        val diagonal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy }
        val accent1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(36, 103, 158)
        }
        val accent2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(128, 183, 220)
        }

        val path = android.graphics.Path().apply {
            moveTo(pageWidth.toFloat(), 0f)
            lineTo(pageWidth.toFloat(), 122f)
            lineTo(178f, 122f)
            lineTo(238f, 0f)
            close()
        }
        canvas.drawPath(path, diagonal)

        canvas.drawPath(android.graphics.Path().apply {
            moveTo(208f, 122f)
            lineTo(224f, 122f)
            lineTo(284f, 0f)
            lineTo(268f, 0f)
            close()
        }, accent1)

        canvas.drawPath(android.graphics.Path().apply {
            moveTo(230f, 122f)
            lineTo(238f, 122f)
            lineTo(298f, 0f)
            lineTo(290f, 0f)
            close()
        }, accent2)

        drawKmiLogo(78f, 58f, 42f)

        titlePaint.textAlign = Paint.Align.RIGHT
        subTitlePaint.textAlign = Paint.Align.RIGHT

        canvas.drawText("מד התקדמות", pageWidth - 34f, 52f, titlePaint)
        canvas.drawText("דו״ח התקדמות אישי לפי חגורות", pageWidth - 34f, 78f, subTitlePaint)

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            "תאריך הפקה: " +
                    java.text.SimpleDateFormat(
                        "dd/MM/yyyy",
                        java.util.Locale("he", "IL")
                    ).format(java.util.Date()),
            pageWidth - 34f,
            142f,
            smallPaint
        )
    }

    fun drawFooter() {
        val footerY = 804f

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            strokeWidth = 2f
        }

        canvas.drawLine(0f, footerY, pageWidth.toFloat(), footerY, line)

        drawKmiLogo(38f, footerY + 22f, 13f)

        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Together We Protect", 62f, footerY + 25f, smallPaint)

        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("עמוד 1 מתוך 1", pageWidth / 2f, footerY + 25f, smallPaint)

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Krav Maga Israel", pageWidth - 66f, footerY + 18f, smallPaint)
        canvas.drawText("www.kmi.org.il", pageWidth - 66f, footerY + 31f, smallPaint)

        val flag = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(20, 85, 200)
        }

        canvas.drawRect(pageWidth - 48f, footerY + 14f, pageWidth - 20f, footerY + 18f, flag)
        canvas.drawRect(pageWidth - 48f, footerY + 28f, pageWidth - 20f, footerY + 32f, flag)
    }

    fun drawSummary(top: Float): Float {
        val beltsToShow = Belt.values().filter { it != Belt.WHITE }
        val avg = if (beltsToShow.isNotEmpty()) {
            beltsToShow.map { belt -> (progress[belt] ?: 0).coerceIn(0, 100) }.average().toInt()
        } else {
            0
        }

        drawRoundRect(
            margin,
            top,
            pageWidth - margin,
            top + 82f,
            lightBlue,
            12f
        )
        drawRoundRect(
            margin,
            top,
            pageWidth - margin,
            top + 82f,
            borderBlue,
            12f,
            stroke = true
        )

        sectionPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("סיכום כללי", pageWidth - margin - 22f, top + 32f, sectionPaint)

        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("אחוז התקדמות ממוצע:", pageWidth - margin - 22f, top + 58f, labelPaint)

        boldValuePaint.textAlign = Paint.Align.LEFT
        boldValuePaint.textSize = 24f
        boldValuePaint.color = navy
        canvas.drawText("$avg%", margin + 28f, top + 56f, boldValuePaint)

        boldValuePaint.textSize = 13f
        boldValuePaint.color = textDark

        return top + 104f
    }

    fun drawProgressCard(
        belt: Belt,
        pct: Int,
        top: Float,
        index: Int
    ): Float {
        val cardHeight = 78f
        val cardLeft = margin
        val cardRight = pageWidth - margin
        val cardBottom = top + cardHeight

        val beltColor = beltPdfColor(belt)
        val beltLight = beltPdfLightColor(belt)

        drawRoundRect(
            cardLeft,
            top,
            cardRight,
            cardBottom,
            if (index % 2 == 0) alpha(beltLight, 0.72f) else softBlue,
            12f
        )
        drawRoundRect(
            cardLeft,
            top,
            cardRight,
            cardBottom,
            alpha(beltColor, 0.78f),
            12f,
            stroke = true,
            strokeWidth = 1.6f
        )

        val title = "חגורה: ${belt.heb.removePrefix("חגורה").trim()}"

        sectionPaint.textAlign = Paint.Align.RIGHT
        sectionPaint.textSize = 15f
        sectionPaint.color = beltColor
        canvas.drawText(title, cardRight - 22f, top + 28f, sectionPaint)

        sectionPaint.textSize = 17f
        sectionPaint.color = blue

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = beltColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cardRight - 22f, top + 48f, 5.5f, dotPaint)

        val percentCircleX = cardLeft + 42f
        val percentCircleY = top + 31f

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = alpha(beltColor, 0.92f)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(percentCircleX, percentCircleY, 23f, circlePaint)
        drawCenteredText("$pct%", percentCircleX, percentCircleY, percentPaint)

        val barLeft = cardLeft + 22f
        val barRight = cardRight - 22f
        val barTop = top + 52f
        val barBottom = barTop + 10f

        drawRoundRect(
            barLeft,
            barTop,
            barRight,
            barBottom,
            alpha(navy, 0.10f),
            999f
        )

        val fillWidth = (barRight - barLeft) * (pct / 100f)
        if (fillWidth > 0f) {
            drawRoundRect(
                barLeft,
                barTop,
                barLeft + fillWidth,
                barBottom,
                beltColor,
                999f
            )
        }

        valuePaint.textAlign = Paint.Align.RIGHT
        valuePaint.color = textDark
        canvas.drawText(
            "$pct% התקדמות לפי פריטים שסומנו באפליקציה",
            barRight,
            cardBottom - 8f,
            valuePaint
        )

        return cardBottom + 9f
    }

    drawHeader()

    var y = 136f
    y = drawSummary(y)

    sectionPaint.textAlign = Paint.Align.CENTER
    canvas.drawText("פירוט לפי חגורות", pageWidth / 2f, y, sectionPaint)

    y += 24f

    val beltsToShow = Belt.values().filter { it != Belt.WHITE }

    beltsToShow.forEachIndexed { index, belt ->
        val pct = (progress[belt] ?: 0).coerceIn(0, 100)

        if (y + 88f < 792f) {
            y = drawProgressCard(
                belt = belt,
                pct = pct,
                top = y,
                index = index
            )
        }
    }

    drawFooter()

    document.finishPage(page)

    val file = File(dir, "progress_report_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }

    document.close()

    return file
}
