package il.kmi.app.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import il.kmi.app.KmiViewModel
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.ext.lightColor
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.shared.domain.Belt
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBrush
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import java.io.File
import java.io.FileOutputStream


//========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    vm: KmiViewModel,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    onOpenCarousel: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val languageManager =
        remember(context) {
            AppLanguageManager(context)
        }

    val isEnglish =
        languageManager.getCurrentLanguage() ==
                AppLanguage.ENGLISH

    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) {
            en
        } else {
            he
        }
    }

    // === SP של ההחרגות בדיוק כמו במסך ההגדרות ===
    val spSettings = remember {
        context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
    }

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
                            "status_${belt.id}_${row.statusTopicKey}_${row.indexInStatusGroup}_${
                                normalizeStatusPart(
                                    row.rawItem
                                )
                            }"

                        var summaryValue: Boolean? =
                            summaryTopicSnap[summaryStatusId]
                                ?: summaryTopicSnap[summaryLegacyStatusId]

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

    val screenBackgroundBrush =
        kmiScreenBackgroundBrush()

    val screenLayoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides
                screenLayoutDirection
    ) {
        Scaffold(
            topBar = {
                il.kmi.app.ui.KmiTopBar(
                    title = tr(
                        "מד התקדמות",
                        "Progress"
                    ),
                    onBack = null,
                    onHome = onHome,
                    showTopHome = false,
                    showTopShare = true,
                    currentLang =
                        if (isEnglish) {
                            "en"
                        } else {
                            "he"
                        },
                    onToggleLanguage = {
                        val newLanguage =
                            if (isEnglish) {
                                AppLanguage.HEBREW
                            } else {
                                AppLanguage.ENGLISH
                            }

                        languageManager.setLanguage(
                            newLanguage
                        )

                        (context as? Activity)
                            ?.recreate()
                    },
                    onShare = {
                        val pdfFile = createProgressPdf(
                            context = context,
                            dir = File(
                                context.cacheDir,
                                "pdfs"
                            ).apply {
                                mkdirs()
                            },
                            progress = beltsData.associate { row ->
                                row.belt to row.percent
                            },
                            isEnglish = isEnglish
                        )

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            pdfFile
                        )

                        val sendIntent =
                            Intent(
                                Intent.ACTION_SEND
                            ).apply {
                                type = "application/pdf"

                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    tr(
                                        "מד התקדמות - KAMI",
                                        "Progress Report - KAMI"
                                    )
                                )

                                putExtra(
                                    Intent.EXTRA_STREAM,
                                    uri
                                )

                                addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }

                        context.startActivity(
                            Intent.createChooser(
                                sendIntent,
                                tr(
                                    "שיתוף PDF",
                                    "Share PDF"
                                )
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
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    contentColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary
                                )
                        ) {
                            Text(
                                text =
                                    tr(
                                        "מעבר למסך התרגילים",
                                        "Go to Exercises"
                                    ),
                                style =
                                    KmiTypography.action.copy(
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    ),
                                textAlign =
                                    TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        screenBackgroundBrush
                    )
            ) {

                // =====================================================
                // תת־כותרת כחולה קבועה
                // =====================================================

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(
                            brush = kmiSectionHeaderBrush()
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tr(
                            "ההישגים שלי לפי חגורות",
                            "My Achievements by Belt"
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        style = KmiTypography.sectionTitle.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // =====================================================
                // תוכן המסך
                // רק האזור הזה משתנה / נגלל
                // =====================================================

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
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
                                KmiLoadingRings(
                                    text =
                                        tr(
                                            "טוען נתוני התקדמות...",
                                            "Loading progress..."
                                        )
                                )

                                Text(
                                    text =
                                        tr(
                                            "מסדר את נתוני החגורות שלך",
                                            "Preparing your belt data"
                                        ),
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
                            verticalArrangement =
                                Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            items(beltsData) { row ->
                                ProgressCard(
                                    belt = row.belt,
                                    percent = row.percent,
                                    done = row.done,
                                    total = row.total,
                                    isEnglish = isEnglish
                                )
                            }
                        }
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
    total: Int,
    isEnglish: Boolean
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
                        if (isEnglish) {
                            "Belt: ${
                                belt.en
                                    .removePrefix("Belt")
                                    .trim()
                            }"
                        } else {
                            "חגורה: ${
                                belt.heb
                                    .removePrefix("חגורה")
                                    .trim()
                            }"
                        },
                    style = KmiTypography.sectionTitle,
                    color = readableBeltColor,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
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
                text =
                    if (isEnglish) {
                        "($done of $total)"
                    } else {
                        "($done מתוך $total)"
                    },
                style =
                    KmiTypography.body.copy(
                        fontWeight =
                            FontWeight.SemiBold
                    ),
                textAlign =
                    if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    },
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }
    }
}

// √ נשמר – שימוש בשדות האחוזים ליצוא PDF, מעוצב כמו הכרטיסים במסך
fun createProgressPdf(
    context: Context,
    dir: File,
    progress: Map<Belt, Int>,
    isEnglish: Boolean
): File {
    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) {
            en
        } else {
            he
        }
    }

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

    val sectionPaint = paint(17f, blue, bold)
    val labelPaint = paint(10.5f, blue, bold)
    val valuePaint = paint(12.5f, textDark, regular)
    val boldValuePaint = paint(13f, textDark, bold)
    val percentPaint =
        paint(
            16f,
            android.graphics.Color.WHITE,
            bold,
            Paint.Align.CENTER
        )

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
        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "מד התקדמות",
            titleEnglish = "Progress",
            subtitleHebrew =
                "דו״ח התקדמות אישי לפי חגורות",
            subtitleEnglish =
                "Personal Progress Report by Belt"
        )
    }

    fun drawFooter() {
        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = 1,
            totalPages = 1,
            isEnglish = isEnglish
        )
    }

    fun drawSummary(top: Float): Float {
        val beltsToShow =
            Belt.entries.filter { belt ->
                belt != Belt.WHITE
            }
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

        val summaryTextAlign =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }

        val summaryTextX =
            if (isEnglish) {
                margin + 22f
            } else {
                pageWidth - margin - 22f
            }

        val summaryValueAlign =
            if (isEnglish) {
                Paint.Align.RIGHT
            } else {
                Paint.Align.LEFT
            }

        val summaryValueX =
            if (isEnglish) {
                pageWidth - margin - 28f
            } else {
                margin + 28f
            }

        sectionPaint.textAlign =
            summaryTextAlign

        canvas.drawText(
            tr(
                "סיכום כללי",
                "Overall Summary"
            ),
            summaryTextX,
            top + 32f,
            sectionPaint
        )

        labelPaint.textAlign =
            summaryTextAlign

        canvas.drawText(
            tr(
                "אחוז התקדמות ממוצע:",
                "Average progress:"
            ),
            summaryTextX,
            top + 58f,
            labelPaint
        )

        boldValuePaint.textAlign =
            summaryValueAlign

        boldValuePaint.textSize = 24f
        boldValuePaint.color = navy

        canvas.drawText(
            "$avg%",
            summaryValueX,
            top + 56f,
            boldValuePaint
        )

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
        val cardRight = pageWidth - margin
        val cardBottom = top + cardHeight

        val beltColor = beltPdfColor(belt)
        val beltLight = beltPdfLightColor(belt)

        drawRoundRect(
            margin,
            top,
            cardRight,
            cardBottom,
            if (index % 2 == 0) alpha(beltLight, 0.72f) else softBlue,
            12f
        )
        drawRoundRect(
            margin,
            top,
            cardRight,
            cardBottom,
            alpha(beltColor, 0.78f),
            12f,
            stroke = true,
            strokeWidth = 1.6f
        )

        val title =
            if (isEnglish) {
                "Belt: ${
                    belt.en
                        .removePrefix("Belt")
                        .trim()
                }"
            } else {
                "חגורה: ${
                    belt.heb
                        .removePrefix("חגורה")
                        .trim()
                }"
            }

        val cardTextAlign =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }

        val cardTextX =
            if (isEnglish) {
                margin + 22f
            } else {
                cardRight - 22f
            }

        sectionPaint.textAlign =
            cardTextAlign

        sectionPaint.textSize = 15f
        sectionPaint.color = beltColor

        canvas.drawText(
            title,
            cardTextX,
            top + 28f,
            sectionPaint
        )

        sectionPaint.textSize = 17f
        sectionPaint.color = blue

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = beltColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cardRight - 22f, top + 48f, 5.5f, dotPaint)

        val percentCircleX = margin + 42f
        val percentCircleY = top + 31f

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = alpha(beltColor, 0.92f)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(percentCircleX, percentCircleY, 23f, circlePaint)
        drawCenteredText("$pct%", percentCircleX, percentCircleY, percentPaint)

        val barLeft = margin + 22f
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

        valuePaint.textAlign =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }

        valuePaint.color =
            textDark

        canvas.drawText(
            tr(
                "$pct% התקדמות לפי פריטים שסומנו באפליקציה",
                "$pct% progress based on marked items"
            ),
            if (isEnglish) {
                barLeft
            } else {
                barRight
            },
            cardBottom - 8f,
            valuePaint
        )

        return cardBottom + 9f
    }

    drawHeader()

    /*
     * בדוח זה יש עמוד יחיד ושש חגורות.
     * מתחילים 8 נקודות מתחת לכותרת כדי שכולן ייכנסו
     * בלי לחפוף לכותרת או לתחתית.
     */
    var y =
        KmiPdfHeader.HEADER_BOTTOM + 8f

    y = drawSummary(y)

    sectionPaint.textAlign =
        Paint.Align.CENTER

    canvas.drawText(
        tr(
            "פירוט לפי חגורות",
            "Progress by Belt"
        ),
        pageWidth / 2f,
        y,
        sectionPaint
    )

    y += 24f

    val beltsToShow =
        Belt.entries.filter { belt ->
            belt != Belt.WHITE
        }

    beltsToShow.forEachIndexed { index, belt ->
        val pct = (progress[belt] ?: 0).coerceIn(0, 100)

        if (
            y + 88f <
            pageHeight - KmiPdfFooter.CONTENT_BOTTOM_PADDING
        ) {
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

    val file =
        File(
            dir,
            if (isEnglish) {
                "Progress Report.pdf"
            } else {
                "דוח התקדמות.pdf"
            }
        )

    FileOutputStream(
        file,
        false
    ).use { output ->
        document.writeTo(output)
    }

    document.close()

    return file
}
