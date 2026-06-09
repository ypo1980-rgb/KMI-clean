package il.kmi.app.screens

import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import il.kmi.app.KmiViewModel
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.ui.color
import il.kmi.app.ui.lightColor
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import java.io.File
import java.io.FileOutputStream
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.app.Activity
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.progress.UserProgressComparison
import il.kmi.app.progress.UserProgressRepository
import kotlinx.coroutines.delay

/* ------------------------------ MarkState (3 states) ------------------------------ */

private enum class MarkState { YES, NO, NONE }

private data class SummaryExerciseRow(
    val displayTopicTitle: String,
    val sourceTopicTitle: String,
    val subTopicTitle: String?,
    val statusTopicKey: String,
    val itemRaw: String,
    val indexInStatusGroup: Int
)

// ===== canonical בדיוק כמו MaterialsScreen =====

private fun cleanItem(topic: String, item: String): String {
    var s = item.trim()

    // ✅ אם הפריט מגיע עם prefix של topic:: — מסירים רק את ה-prefix הזה
    //    אבל לא "חותכים" לקטע האחרון, כדי לא ליצור התנגשויות ("ימין"/"שמאל")
    if (topic.isNotBlank() && s.startsWith("$topic::")) {
        s = s.removePrefix("$topic::").trim()
    }

    // ✅ חשוב: לא לעשות substringAfterLast("::") !
    // אם יש :: זה חלק מהייחודיות של הפריט (subTopic::item)

    s = s.replace(Regex("\\s+"), " ").trim()
    return s
}

private fun norm(s: String) = s
    .replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
    .replace(Regex("[\u0591-\u05C7]"), "")
    .replace("[\\-–—:_]".toRegex(), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
    .lowercase()

// ✅ ADD
private fun topicKey(s: String): String = s
    .replace("\u200F","")
    .replace("\u200E","")
    .replace("\u00A0"," ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun normalizeStatusPart(s: String): String =
    s.replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun summaryLegacyStatusIdFor(
    belt: Belt,
    topicKey: String,
    index: Int,
    item: String
): String {
    val cleanItem = normalizeStatusPart(item)

    // מפתח ישן — נשאר רק כ-fallback זמני לסימונים ישנים
    return "status_${belt.id}_${topicKey}_${index}_${cleanItem}"
}

private fun summaryExerciseIdentityIdFor(
    belt: Belt,
    topicKey: String,
    topicTitle: String,
    index: Int,
    item: String
): String {
    val cleanOriginal = cleanItem(topicTitle, item).trim()

    val resolved = ExerciseIdentityRegistry.resolve(
        belt = belt,
        hebrewTitle = cleanOriginal,
        topicKey = topicKey
    )

    if (resolved.isKnown) {
        return resolved.id
    }

    // חייב להיות זהה ל-fallback שהגדרנו ב-MaterialsScreen
    return "${resolved.id}_row_$index"
}

private fun findCanonicalItem(b: Belt, t: String, displayItem: String): String? {
    val wanted = norm(displayItem)

    // 1) פריטים ישירים של נושא
    val direct = SharedContentRepo.getAllItemsFor(b, t, subTopicTitle = null)
    direct.firstOrNull { raw ->
        val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
        norm(disp) == wanted || norm(raw) == wanted
    }?.let { return it }

    // 2) פריטים מתוך תתי-נושאים
    val subs = SharedContentRepo.getSubTopicsFor(b, t)
    subs.forEach { st ->
        st.items.firstOrNull { raw ->
            val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
            norm(disp) == wanted || norm(raw) == wanted
        }?.let { return it }
    }

    return null
}

private fun canonicalFor(belt: Belt, topicTitle: String, displayItem: String): String {
    val cleaned = cleanItem(topicTitle, displayItem)
    return findCanonicalItem(belt, topicTitle, cleaned) ?: cleaned
}

private fun resolveCanonicalIdForExplanation(belt: Belt, topicTitle: String, rawItemFromRepo: String): String {
    val displayKey = cleanItem(topicTitle, rawItemFromRepo)
    return findCanonicalItem(belt, topicTitle, displayKey) ?: displayKey
}

private fun beltContentFor(belt: Belt): SharedContentRepo.BeltContent? {
    // ✅ מקור אמת: shared ContentRepo.data
    return SharedContentRepo.data[belt]
}

/**
 * ✅ טקסט לתצוגה בלבד (כמו MaterialsScreen):
 * מנקה prefixים ומחזיר displayName מה-formatter.
 */

private fun canonicalFromRepo(topicTitle: String, rawItemFromRepo: String): String {
    return cleanItem(topicTitle, rawItemFromRepo).trim()
}

private fun uiDisplayName(topicTitle: String, rawItem: String): String {
    val cleaned = cleanItem(topicTitle, rawItem)
    return ExerciseTitleFormatter.displayName(cleaned)
        .ifBlank { cleaned }
        .trim()
}

private fun topicDisplayName(topicTitle: String, isEnglish: Boolean): String {
    val clean = topicTitle.trim()
    if (!isEnglish) return clean

    return ExerciseTitlesEn.get(clean)?.takeIf { it.isNotBlank() } ?: when (clean) {
        "כללי" -> "General"
        "עבודת ידיים" -> "Hand Strikes"
        "בעיטות" -> "Kicks"
        "שחרורים" -> "Releases"
        "הגנות" -> "Defences"
        "נפילות" -> "Break-Falls and Rolls"
        "קרקע" -> "Ground-Work"
        "כושר" -> "Fitness"
        "קוואלר" -> "Kavaler"
        else -> clean
    }
}

private fun subTopicDisplayName(
    subTopicTitle: String?,
    topicTitle: String,
    isEnglish: Boolean
): String {
    val clean = subTopicTitle?.trim().orEmpty()

    if (clean.isBlank()) {
        return if (isEnglish) "General exercises" else "תרגילים כלליים"
    }

    if (!isEnglish) return clean

    return ExerciseTitlesEn.get(clean)?.takeIf { it.isNotBlank() } ?: clean
}

private fun exerciseDisplayNameForUi(topicTitle: String, rawItem: String, isEnglish: Boolean): String {
    val topicTrim = topicTitle.trim()

    fun normalizeForLookup(s: String): String =
        s.trim()
            .replace("–", "-")
            .replace("—", "-")
            .replace(" - ", " - ")
            .replace("- ", "-")
            .replace(" -", "-")
            .replace(Regex("\\s*/\\s*"), "/")
            .replace(Regex("\\s+"), " ")
            .trim()

    val cleaned = buildString {
        var s = rawItem.trim()

        if (topicTrim.isNotBlank() && s.startsWith("$topicTrim::")) {
            s = s.removePrefix("$topicTrim::").trim()
        }

        if (topicTrim.isNotBlank() && s.startsWith(topicTrim)) {
            s = s.removePrefix(topicTrim).trim()
            s = s.trimStart('-', '–', '—', ':').trim()
        }

        append(s)
    }

    val base = ExerciseTitleFormatter.displayName(cleaned)
        .ifBlank { cleaned }
        .trim()

    if (!isEnglish) return base

    val candidates = listOf(
        base,
        normalizeForLookup(base),
        cleaned,
        normalizeForLookup(cleaned),
        rawItem.trim(),
        normalizeForLookup(rawItem.trim()),
        rawItem.substringAfter("::", rawItem).trim(),
        normalizeForLookup(rawItem.substringAfter("::", rawItem).trim())
    ).distinct()

    val translated = candidates.firstNotNullOfOrNull { candidate ->
        ExerciseTitlesEn.get(candidate)?.takeIf { it.isNotBlank() }
    }

    return translated ?: base
}

/* ------------------------------ ProgressMeter ------------------------------ */

@Composable
fun ProgressMeter(
    vm: KmiViewModel,
    belt: Belt,
    topic: String? = null,
    modifier: Modifier = Modifier,
    meterSize: Dp = 180.dp,
    stroke: Dp = 14.dp,
    doneOverride: Int? = null,
    totalOverride: Int? = null,
    knownOverride: Int? = null,
    notKnownOverride: Int? = null,
    unmarkedOverride: Int? = null
) {
    var knownCount by remember(belt, topic) { mutableStateOf(0) }
    var notKnownCount by remember(belt, topic) { mutableStateOf(0) }
    var unmarkedCount by remember(belt, topic) { mutableStateOf(0) }
    var total by remember(belt, topic) { mutableStateOf(0) }

    val marksVer by vm.marksVersion.collectAsState()

    LaunchedEffect(belt, topic, marksVer) {
        val beltContent = beltContentFor(belt)

        val titles: List<String> =
            if (topic.isNullOrBlank()) beltContent?.topics?.map { it.title }.orEmpty()
            else listOf(topic)

        var t = 0
        var yes = 0
        var no = 0
        var none = 0

        titles.forEach { tp ->
            val tpObj = beltContent?.topics?.firstOrNull { norm(it.title) == norm(tp) }
            val items = if (tpObj == null) emptyList()
            else (tpObj.items + tpObj.subTopics.flatMap { it.items }).distinct()

            t += items.size

            val topicSnap = vm.getTopicStatusSnapshot(belt, tp)

            items.forEachIndexed { index, raw ->
                val statusId = summaryExerciseIdentityIdFor(
                    belt = belt,
                    topicKey = tp.trim(),
                    topicTitle = tp,
                    index = index,
                    item = raw
                )

                val legacyStatusId = summaryLegacyStatusIdFor(
                    belt = belt,
                    topicKey = tp.trim(),
                    index = index,
                    item = raw
                )

                when (topicSnap[statusId] ?: topicSnap[legacyStatusId]) {
                    true -> yes++
                    false -> no++
                    null -> none++
                }
            }
        }

        total = t
        knownCount = yes
        notKnownCount = no
        unmarkedCount = none
    }

    val effectiveKnown = knownOverride ?: doneOverride ?: knownCount
    val effectiveNotKnown = notKnownOverride ?: notKnownCount
    val effectiveUnmarked = unmarkedOverride ?: unmarkedCount
    val effectiveTotal = totalOverride ?: total

    // ✅ התקדמות = כל מה שסומן: יודע + לא יודע.
    // ירוק = יודע, אדום = לא יודע, אפור = לא סומן.
    val effectiveCompleted = (effectiveKnown + effectiveNotKnown)
        .coerceAtMost(effectiveTotal)
        .coerceAtLeast(0)

    val pct: Int =
        if (effectiveTotal == 0) 0
        else (effectiveCompleted * 100 / effectiveTotal)

    val animatedKnownSweep by animateFloatAsState(
        targetValue = if (effectiveTotal == 0) 0f else 360f * (effectiveKnown.toFloat() / effectiveTotal.toFloat()),
        animationSpec = tween(durationMillis = 950),
        label = "premiumKnownSweep"
    )

    val animatedNotKnownSweep by animateFloatAsState(
        targetValue = if (effectiveTotal == 0) 0f else 360f * (effectiveNotKnown.toFloat() / effectiveTotal.toFloat()),
        animationSpec = tween(durationMillis = 950),
        label = "premiumNotKnownSweep"
    )

    val animatedUnmarkedSweep by animateFloatAsState(
        targetValue = if (effectiveTotal == 0) 0f else 360f * (effectiveUnmarked.toFloat() / effectiveTotal.toFloat()),
        animationSpec = tween(durationMillis = 950),
        label = "premiumUnmarkedSweep"
    )

    val context = LocalContext.current
    val languageManager = remember { AppLanguageManager(context) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val knownColor = Color(0xFF4CAF50)
    val notKnownColor = Color(0xFFE53935)
    val unmarkedColor = Color(0xFFD9D9E3)

    val centerTextColor = Color(0xFF1E293B)
    val subTextColor = Color(0xFF667085)
    val remaining = (effectiveTotal - effectiveCompleted).coerceAtLeast(0)

    Box(
        modifier = modifier.size(meterSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = stroke.toPx()
            var startAngle = -90f

            drawCircle(
                color = Color.White.copy(alpha = 0.96f)
            )

            drawArc(
                color = unmarkedColor,
                startAngle = startAngle,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (animatedKnownSweep > 0.1f) {
                drawArc(
                    color = knownColor,
                    startAngle = startAngle,
                    sweepAngle = animatedKnownSweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                startAngle += animatedKnownSweep
            }

            if (animatedNotKnownSweep > 0.1f) {
                drawArc(
                    color = notKnownColor,
                    startAngle = startAngle,
                    sweepAngle = animatedNotKnownSweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                startAngle += animatedNotKnownSweep
            }

            val innerWhiteRadius = size.minDimension * 0.34f
            val innerSoftRadius = size.minDimension * 0.28f

            drawCircle(
                color = Color.White.copy(alpha = 0.98f),
                radius = innerWhiteRadius
            )

            drawCircle(
                color = belt.color.copy(alpha = 0.07f),
                radius = innerSoftRadius
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = centerTextColor
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (isEnglish) "Marked" else "סומנו",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = knownColor
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = if (isEnglish) {
                    "$effectiveCompleted out of $effectiveTotal"
                } else {
                    "$effectiveCompleted מתוך $effectiveTotal"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = subTextColor
            )

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = knownColor.copy(alpha = 0.10f),
                border = BorderStroke(
                    width = 1.dp,
                    color = knownColor.copy(alpha = 0.18f)
                )
            ) {
                Text(
                    text = if (!topic.isNullOrBlank()) {
                        topic
                    } else {
                        if (isEnglish) {
                            "$remaining remaining"
                        } else {
                            "נותרו $remaining"
                        }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF344054),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun UserProgressComparisonCard(
    comparison: UserProgressComparison?,
    isLoaded: Boolean,
    belt: Belt,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    val titleText = if (isEnglish) {
        "Your belt progress"
    } else {
        "המצב שלך בחגורה"
    }

    val loadingText = if (isEnglish) {
        "Loading comparison data..."
    } else {
        "טוען נתוני השוואה..."
    }

    val notEnoughText = if (isEnglish) {
        "Not enough trainees yet for a reliable comparison."
    } else {
        "אין עדיין מספיק נתונים להשוואה מול מתאמנים אחרים."
    }

    val c = comparison

    // ✅ אם אין מתאמנים בכלל / אין נתונים מספיקים — לא מציגים 0% ו-0 מתאמנים.
    // זה מונע מצב לא הגיוני כמו "ממוצע 0%" + "מתאמנים 0".
    val hasValidComparisonData =
        c != null &&
                c.usersCount > 0 &&
                c.hasEnoughData

    val textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val columnAlignment = if (isEnglish) Alignment.Start else Alignment.End

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.92f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = belt.color.copy(alpha = 0.28f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            belt.color.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = columnAlignment
        ) {

            // ✅ כותרת + X לסגירת כרטיס ההשוואה
            // בעברית: הכותרת והאייקון בצד ימין, X בסוף השורה בצד שמאל.
            // באנגלית: הכותרת והאייקון בצד שמאל, X בסוף השורה בצד ימין.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(34.dp)
                        .align(
                            if (isEnglish) {
                                androidx.compose.ui.AbsoluteAlignment.CenterRight
                            } else {
                                androidx.compose.ui.AbsoluteAlignment.CenterLeft
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = if (isEnglish) "Close comparison" else "סגור השוואה",
                        tint = Color(0xFF475467)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .absolutePadding(
                            left = if (isEnglish) 0.dp else 42.dp,
                            right = if (isEnglish) 42.dp else 0.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isEnglish) {
                        Arrangement.Absolute.Left
                    } else {
                        Arrangement.Absolute.Right
                    }
                ) {
                    if (isEnglish) {
                        Icon(
                            imageVector = Icons.Filled.Insights,
                            contentDescription = null,
                            tint = belt.color,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF172033),
                            textAlign = TextAlign.Left,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF172033),
                            textAlign = TextAlign.Right,
                            maxLines = 1
                        )

                        Spacer(Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Filled.Insights,
                            contentDescription = null,
                            tint = belt.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (!isLoaded) {
                Text(
                    text = loadingText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475467),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!hasValidComparisonData) {
                Text(
                    text = notEnoughText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF667085),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryMiniProgressChip(
                        title = if (isEnglish) "You know" else "אתה יודע",
                        value = "${c.userKnownPercent}%",
                        color = belt.color,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryMiniProgressChip(
                        title = if (isEnglish) "Average" else "ממוצע",
                        value = "${c.averageKnownPercent}%",
                        color = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )

                    SummaryMiniProgressChip(
                        title = if (isEnglish) "Trainees" else "מתאמנים",
                        value = c.usersCount.toString(),
                        color = Color(0xFF475467),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = if (isEnglish) {
                        "You are above ${c.percentileAbove}% of trainees in your belt."
                    } else {
                        "אתה מעל ${c.percentileAbove}% מהמתאמנים בחגורה שלך."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = belt.color,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SummaryMiniProgressChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF344054),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SummaryToggleButton(
    text: String,
    iconColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedBorder = iconColor.copy(alpha = 0.46f)
    val idleBorder = Color.White.copy(alpha = 0.78f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(19.dp),
        color = Color.Transparent,
        shadowElevation = if (selected) 8.dp else 5.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) selectedBorder else idleBorder
        ),
        modifier = modifier.height(48.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (selected) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.98f),
                                iconColor.copy(alpha = 0.24f),
                                iconColor.copy(alpha = 0.15f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.98f),
                                Color.White.copy(alpha = 0.90f),
                                iconColor.copy(alpha = 0.055f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(19.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = if (selected) {
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                iconColor.copy(alpha = 0.36f),
                                Color.White.copy(alpha = 0.82f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.55f),
                                iconColor.copy(alpha = 0.12f)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(19.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) Color(0xFF172033) else Color(0xFF263238),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.width(6.dp))

                Surface(
                    shape = CircleShape,
                    color = if (selected) {
                        iconColor.copy(alpha = 0.18f)
                    } else {
                        iconColor.copy(alpha = 0.09f)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = iconColor.copy(alpha = if (selected) 0.24f else 0.12f)
                    ),
                    modifier = Modifier.size(25.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Insights,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------ SummaryScreen ------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    vm: KmiViewModel,
    belt: Belt,
    topic: String = "",
    subTopicFilter: String? = null,
    onBack: () -> Unit,
    onBackHome: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val languageManager = remember { AppLanguageManager(ctx) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val scroll = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val notesSp = remember { ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }

    val favorites: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    val marksVer by vm.marksVersion.collectAsState()

    var showProgress by rememberSaveable { mutableStateOf(false) }
    var showComparison by rememberSaveable { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var userProgressComparison by remember {
        mutableStateOf<UserProgressComparison?>(null)
    }

    var userProgressComparisonLoaded by remember {
        mutableStateOf(false)
    }

    fun noteSuffixFor(topicTitle: String): String {
        val cleanTopic = topicTitle.trim().ifBlank { "כללי" }
        return if (
            topic.isNotBlank() &&
            !subTopicFilter.isNullOrBlank() &&
            norm(cleanTopic) == norm(topic)
        ) {
            "${topic.trim()}__${subTopicFilter.trim()}"
        } else {
            cleanTopic
        }
    }

    fun statusTopicKeyFor(topicTitle: String): String {
        val cleanTopic = topicTitle.trim().ifBlank { "כללי" }

        return if (
            topic.isNotBlank() &&
            !subTopicFilter.isNullOrBlank() &&
            norm(cleanTopic) == norm(topic)
        ) {
            "${topic.trim()}__${subTopicFilter.trim()}"
        } else {
            cleanTopic
        }
    }

    fun noteKeyFor(topicTitle: String, itemId: String): String {
        return "note_${belt.id}_${noteSuffixFor(topicTitle)}_${cleanItem(topicTitle, itemId)}"
    }

    fun loadNote(topicTitle: String, itemId: String): String =
        notesSp.getString(noteKeyFor(topicTitle, itemId), "")?.trim().orEmpty()

    fun saveNote(topicTitle: String, itemId: String, value: String) {
        val key = noteKeyFor(topicTitle, itemId)
        with(notesSp.edit()) {
            if (value.isBlank()) {
                remove(key)
            } else {
                putString(key, value.trim())
            }
            apply()
        }
    }

    fun hasNote(topicTitle: String, itemId: String): Boolean =
        loadNote(topicTitle, itemId).isNotBlank()

    // ✅ כשפותחים מד התקדמות / השוואה — עולים לראש המסך
    LaunchedEffect(showProgress, showComparison) {
        if (showProgress || showComparison) {
            scroll.animateScrollTo(0)
        }
    }

    // === רשימת פריטים לפי נושא (ישירות מה-shared ContentRepo) ===
    var itemsByTopic by remember(belt, topic, subTopicFilter) {
        mutableStateOf(LinkedHashMap<String, List<SummaryExerciseRow>>())
    }

    LaunchedEffect(belt, topic, subTopicFilter) {
        val beltContent = beltContentFor(belt)
        val topics = beltContent?.topics.orEmpty()

        val out = LinkedHashMap<String, List<SummaryExerciseRow>>()

        fun rowsForTopic(
            topicTitle: String,
            requestedSubTopicTitle: String? = null
        ): List<SummaryExerciseRow> {
            val topicObj = topics.firstOrNull { norm(it.title) == norm(topicTitle) }
                ?: return emptyList()

            val rows = mutableListOf<SummaryExerciseRow>()

            fun findSubTopicDeep(
                list: List<SharedContentRepo.SubTopic>,
                wantedTitle: String
            ): SharedContentRepo.SubTopic? {
                list.forEach { subTopic ->
                    if (norm(subTopic.title) == norm(wantedTitle)) {
                        return subTopic
                    }

                    val nested = findSubTopicDeep(
                        list = subTopic.subTopics,
                        wantedTitle = wantedTitle
                    )

                    if (nested != null) {
                        return nested
                    }
                }

                return null
            }

            fun addRowsFromSubTopicDeep(
                subTopic: SharedContentRepo.SubTopic
            ) {
                val cleanSubTopicTitle = subTopic.title.trim()

                subTopic.items
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .forEachIndexed { index, itemRaw ->
                        rows += SummaryExerciseRow(
                            displayTopicTitle = topicObj.title,
                            sourceTopicTitle = topicObj.title,
                            subTopicTitle = cleanSubTopicTitle,
                            statusTopicKey = "${topicObj.title.trim()}__${cleanSubTopicTitle}",
                            itemRaw = itemRaw,
                            indexInStatusGroup = index
                        )
                    }

                // ✅ חשוב: אוסף גם תתי־נושאים פנימיים,
                // למשל הגנות -> הגנות נגד בעיטות -> בעיטות צד / בעיטות מעגליות וכו׳
                subTopic.subTopics.forEach { nestedSubTopic ->
                    addRowsFromSubTopicDeep(nestedSubTopic)
                }
            }

            if (requestedSubTopicTitle.isNullOrBlank()) {
                topicObj.items
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .forEachIndexed { index, itemRaw ->
                        rows += SummaryExerciseRow(
                            displayTopicTitle = topicObj.title,
                            sourceTopicTitle = topicObj.title,
                            subTopicTitle = null,
                            statusTopicKey = topicObj.title.trim(),
                            itemRaw = itemRaw,
                            indexInStatusGroup = index
                        )
                    }

                topicObj.subTopics.forEach { subTopic ->
                    addRowsFromSubTopicDeep(subTopic)
                }
            } else {
                val requested = requestedSubTopicTitle.trim()

                val selectedSubTopic = findSubTopicDeep(
                    list = topicObj.subTopics,
                    wantedTitle = requested
                )

                if (selectedSubTopic != null) {
                    addRowsFromSubTopicDeep(selectedSubTopic)
                }
            }

            return rows
        }

        if (topic.isNotBlank() && !subTopicFilter.isNullOrBlank()) {
            out[topic.trim()] = withContext(Dispatchers.Default) {
                rowsForTopic(
                    topicTitle = topic.trim(),
                    requestedSubTopicTitle = subTopicFilter.trim()
                )
            }

            itemsByTopic = out
            return@LaunchedEffect
        }

        val allTitles = topics.map { it.title }
        val orderedTitles: List<String> =
            if (topic.isNotBlank()) {
                listOf(topic) + allTitles.filterNot { norm(it) == norm(topic) }
            } else {
                allTitles
            }

        orderedTitles.forEach { title ->
            val topicObj = topics.firstOrNull { norm(it.title) == norm(title) }
            if (topicObj != null) {
                out[topicObj.title] = rowsForTopic(topicObj.title)
            }
        }

        itemsByTopic = out
    }

    // ✅ Warmup לכל הסימונים של כל הנושאים ותתי-הנושאים במסך הסיכום.
    // בלי זה, getTopicStatusSnapshot קורא רק cache,
    // ולכן סימונים נטענים רק אחרי שנכנסים ידנית לנושא/תת-נושא.
    LaunchedEffect(belt, itemsByTopic) {
        val warmupByStatusTopicKey: Map<String, List<String>> =
            itemsByTopic
                .values
                .flatten()
                .groupBy { row -> row.statusTopicKey }
                .mapValues { (_, rows) ->
                    rows.map { row ->
                        summaryExerciseIdentityIdFor(
                            belt = belt,
                            topicKey = row.statusTopicKey,
                            topicTitle = row.sourceTopicTitle,
                            index = row.indexInStatusGroup,
                            item = row.itemRaw
                        )
                    }.distinct()
                }

        warmupByStatusTopicKey.forEach { (statusTopicKey, statusIds) ->
            vm.warmUpTopicStatuses(
                belt = belt,
                topic = statusTopicKey,
                items = statusIds
            )
        }
    }

    /**
     * ✅ masteredMap נשמר לפי (topicTitle, statusId)
     * statusId חייב להיות זהה ל-MaterialsScreen:
     * ExerciseIdentityRegistry / fallback legacy_row_index.
     * לא משתמשים כאן ב-canonicalId כדי למנוע הדבקת סימונים בין תרגילים דומים.
     */
    var masteredMap by remember(belt, itemsByTopic) {
        mutableStateOf<Map<Pair<String, String>, MarkState>>(emptyMap())
    }

    LaunchedEffect(belt, itemsByTopic, subTopicFilter, topic, marksVer) {
        loadError = null
        try {
            // ✅ מחשבים ברקע כדי למנוע תקיעה של ה-UI
            val computed: Map<Pair<String, String>, MarkState> = withContext(Dispatchers.Default) {
                val map = mutableMapOf<Pair<String, String>, MarkState>()

                itemsByTopic.forEach { (topicTitle, rows) ->

                    val snapshotsByStatusTopicKey = rows
                        .map { it.statusTopicKey }
                        .distinct()
                        .associateWith { key ->
                            vm.getTopicStatusSnapshot(belt, key)
                        }

                    rows.forEach { row ->
                        val topicSnap = snapshotsByStatusTopicKey[row.statusTopicKey].orEmpty()

                        val statusId = summaryExerciseIdentityIdFor(
                            belt = belt,
                            topicKey = row.statusTopicKey,
                            topicTitle = row.sourceTopicTitle,
                            index = row.indexInStatusGroup,
                            item = row.itemRaw
                        )

                        val legacyStatusId = summaryLegacyStatusIdFor(
                            belt = belt,
                            topicKey = row.statusTopicKey,
                            index = row.indexInStatusGroup,
                            item = row.itemRaw
                        )

                        val v: Boolean? = topicSnap[statusId] ?: topicSnap[legacyStatusId]

                        val state = when (v) {
                            true  -> MarkState.YES
                            false -> MarkState.NO
                            null  -> MarkState.NONE
                        }

                        map[topicTitle to statusId] = state
                    }
                }

                map
            }

            masteredMap = computed
        } catch (e: Exception) {
            loadError = e.message ?: tr("שגיאה בקריאת הנתונים", "Error reading data")
            masteredMap = emptyMap()
        }
    }

    // ✅ סטטיסטיקות לפי נושא (מבוסס canonicalFromRepo)
    val topicStats: Map<String, Pair<Int, Int>> = remember(
        masteredMap,
        itemsByTopic,
        topic,
        subTopicFilter
    ) {
        itemsByTopic.mapValues { (topicTitle, rows) ->
            val total = rows.size

            val done = rows.count { row ->
                val statusId = summaryExerciseIdentityIdFor(
                    belt = belt,
                    topicKey = row.statusTopicKey,
                    topicTitle = row.sourceTopicTitle,
                    index = row.indexInStatusGroup,
                    item = row.itemRaw
                )

                masteredMap[topicTitle to statusId] == MarkState.YES
            }

            done to total
        }
    }

    val overallDone = topicStats.values.sumOf { it.first }
    val overallTotal = topicStats.values.sumOf { it.second }
    val overallPct = if (overallTotal <= 0) 0 else ((overallDone * 100f) / overallTotal).toInt()

    // ✅ שלב א׳ בפיצ׳ר ההשוואה:
    // שמירת סיכום אישי של המשתמש לפי חגורה.
    // עדיין לא מחשבים דירוג מול משתמשים אחרים ולא מציגים UI חדש.
    LaunchedEffect(
        belt.id,
        overallDone,
        overallTotal,
        overallPct
    ) {
        if (overallTotal > 0) {
            runCatching {
                UserProgressRepository.saveUserProgress(
                    beltId = belt.id,
                    knownPercent = overallPct,
                    knownCount = overallDone,
                    totalCount = overallTotal
                )

                // נותן ל-Cloud Function זמן קצר לעדכן beltStats.
                delay(900)

                userProgressComparison =
                    UserProgressRepository.loadBeltComparison(
                        beltId = belt.id,
                        userKnownPercent = overallPct
                    )

                userProgressComparisonLoaded = true
            }.onFailure {
                userProgressComparison = null
                userProgressComparisonLoaded = true
            }
        }
    }

    // === חיפוש/הסבר ===
    var explainFromSearch: Triple<Belt, String, String>? by rememberSaveable { mutableStateOf(null) }
    var noteEditorFor by rememberSaveable { mutableStateOf<String?>(null) }
    var noteEditorTopic by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var notesRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    val handlePickFromTopBar: (String) -> Unit = { key ->
        fun dec(s: String) = try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

        val resolved = runCatching { il.kmi.app.domain.ContentRepo.resolveItemKey(key) }.getOrNull()
        if (resolved != null) {
            explainFromSearch = Triple(resolved.belt, resolved.topicTitle, resolved.itemTitle)
        } else {
            val parts = when {
                '|' in key -> key.split('|', limit = 3)
                "::" in key -> key.split("::", limit = 3)
                '/' in key -> key.split('/', limit = 3)
                else -> listOf("", "", "")
            }.map(::dec)

            val beltFromKey = Belt.fromId(parts.getOrNull(0).orEmpty()) ?: belt
            val topicTitle = parts.getOrNull(1).orEmpty().trim()
            val itemTitleRaw = parts.getOrNull(2).orEmpty().trim()
            val itemTitle = cleanItem(topicTitle, itemTitleRaw)

            explainFromSearch = Triple(beltFromKey, topicTitle, itemTitle)
        }
    }


    // ✳️ שיתוף PDF
    val sharePdf: (String?) -> Unit = { targetPackage ->
        runCatching {
            val dir = File(ctx.cacheDir, "pdfs").apply { mkdirs() }
            val pdf = createSummaryPdf(
                dir = dir,
                belt = belt,
                itemsByTopic = itemsByTopic,
                masteredMap = masteredMap,
                isEnglish = isEnglish,
                topic = topic,
                subTopicFilter = subTopicFilter
            )
            val uri = androidx.core.content.FileProvider.getUriForFile(
                ctx, "${ctx.packageName}.fileprovider", pdf
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                targetPackage?.let { setPackage(it) }
            }
            ctx.startActivity(android.content.Intent.createChooser(intent, tr("שיתוף דו\"ח סיכום", "Share summary report")))
        }.onFailure {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    if (isEnglish) "Summary report ${belt.id}" else "דו\"ח סיכום ${belt.heb}"
                )
                targetPackage?.let { setPackage(it) }
            }
            runCatching {
                ctx.startActivity(android.content.Intent.createChooser(intent, tr("שיתוף", "Share")))
            }
        }
    }

    Scaffold(
        topBar = {
            val beltLabel = remember(belt, isEnglish) {
                if (isEnglish) {
                    when (belt) {
                        Belt.WHITE -> "White Belt"
                        Belt.YELLOW -> "Yellow Belt"
                        Belt.ORANGE -> "Orange Belt"
                        Belt.GREEN -> "Green Belt"
                        Belt.BLUE -> "Blue Belt"
                        Belt.BROWN -> "Brown Belt"
                        Belt.BLACK -> "Black Belt"
                    }
                } else {
                    val h = belt.heb.trim()
                    if (h.startsWith("חגורה")) h else "חגורה $h"
                }
            }

            val beltRes = remember(belt) {
                when (belt) {
                    Belt.WHITE  -> il.kmi.app.R.drawable.belt_white
                    Belt.YELLOW -> il.kmi.app.R.drawable.belt_yellow
                    Belt.ORANGE -> il.kmi.app.R.drawable.belt_orange
                    Belt.GREEN  -> il.kmi.app.R.drawable.belt_green
                    Belt.BLUE   -> il.kmi.app.R.drawable.belt_blue
                    Belt.BROWN  -> il.kmi.app.R.drawable.belt_brown
                    Belt.BLACK  -> il.kmi.app.R.drawable.belt_black
                }
            }

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            // ✅ במסך הסיכום שומרים את מבנה הכותרת קבוע כמו בעברית:
            // חגורה/תג בצד שמאל, תפריט בצד ימין.
            // הטקסט עדיין משתנה לעברית/אנגלית לפי isEnglish.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                il.kmi.app.ui.KmiTopBar(
                    title = if (isEnglish) "Summary $beltLabel - ${overallPct}%" else "סיכום $beltLabel - ${overallPct}%",
                    onShare = { sharePdf(null) },
                    onPickSearchResult = { key -> handlePickFromTopBar(key) },
                    onShareWhatsApp = { sharePdf("com.whatsapp") },
                    onHome = { onBackHome() },
                    showBottomActions = true,
                    extraActions = { },
                    centerTitle = false,
                    showTopHome = false,

                    // ✅ בכוונה "he" רק לסידור הכותרת, כדי לא להזיז את התפריט לשמאל באנגלית.
                    // הכותרת עצמה נשארת באנגלית לפי isEnglish.
                    currentLang = "he",

                    onToggleLanguage = {
                        val newLang =
                            if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                                AppLanguage.ENGLISH
                            } else {
                                AppLanguage.HEBREW
                            }

                        langManager.setLanguage(newLang)
                        (contextLang as? Activity)?.recreate()
                    }
                )
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            val shineAnim = rememberInfiniteTransition(label = "summaryButtonShine")
            val shineOffset by shineAnim.animateFloat(
                initialValue = -140f,
                targetValue = 900f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3200)
                ),
                label = "summaryButtonShineOffset"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.85f)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF7F00FF),
                                        Color(0xFF3F51B5),
                                        Color(0xFF03A9F4)
                                    )
                                )
                            )
                            .graphicsLayer { clip = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer { clip = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = shineOffset.dp)
                                    .width(52.dp)
                                    .fillMaxHeight(0.70f)
                                    .align(Alignment.CenterStart)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.14f),
                                                Color.White.copy(alpha = 0.30f),
                                                Color.White.copy(alpha = 0.14f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = tr("חזרה למסך הנושאים", "Back to topics screen"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        // ===== דיאלוג הסבר + מועדפים + הערה =====
        explainFromSearch?.let { (b, t, iRaw) ->
            val canonical = resolveCanonicalIdForExplanation(b, t, iRaw)

            val explanation = remember(b, t, iRaw, canonical, isEnglish) {
                val resolved = ExerciseExplanationResolver.get(
                    belt = b,
                    topic = t,
                    item = iRaw,
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
                    tr(
                        "לא נמצא הסבר עבור \"$canonical\".",
                        "No explanation was found for \"$canonical\"."
                    )
                }
            }

            val cleanFavId = cleanItem(t, canonical)
            val isFav = favorites.contains(cleanFavId)
            val noteText = remember(t, cleanFavId, notesRefreshKey) {
                loadNote(t, cleanFavId)
            }

            val dialogTitle = exerciseDisplayNameForUi(
                topicTitle = t,
                rawItem = canonical,
                isEnglish = isEnglish
            )

            val dialogBeltLabel = if (isEnglish) {
                when (b) {
                    Belt.WHITE -> "(White belt)"
                    Belt.YELLOW -> "(Yellow belt)"
                    Belt.ORANGE -> "(Orange belt)"
                    Belt.GREEN -> "(Green belt)"
                    Belt.BLUE -> "(Blue belt)"
                    Belt.BROWN -> "(Brown belt)"
                    Belt.BLACK -> "(Black belt)"
                }
            } else {
                "(${b.heb})"
            }

            ExerciseExplanationDialog(
                title = dialogTitle,
                beltLabel = dialogBeltLabel,
                explanation = explanation,
                noteText = noteText,
                isFavorite = isFav,
                accentColor = b.color,
                isEnglish = isEnglish,
                onDismiss = {
                    explainFromSearch = null
                    focusManager.clearFocus()
                },
                onEditNote = {
                    noteEditorTopic = t
                    noteEditorFor = cleanFavId
                    noteDraft = loadNote(t, cleanFavId)
                },
                onDeleteNote = {
                    saveNote(t, cleanFavId, "")
                    noteDraft = ""
                    notesRefreshKey++
                },
                onToggleFavorite = {
                    FavoritesStore.toggle(cleanFavId)
                }
            )
        }

        noteEditorFor?.let { item ->
            val noteTopic = noteEditorTopic ?: topic.ifBlank { "כללי" }

            ExerciseNoteEditorDialog(
                noteText = noteDraft,
                isEnglish = isEnglish,
                accentColor = belt.color,
                onNoteChange = { noteDraft = it },
                onDismiss = {
                    noteEditorFor = null
                    noteEditorTopic = null
                },
                onSave = {
                    val cleanNote = noteDraft.trim()
                    noteDraft = cleanNote

                    saveNote(noteTopic, item, cleanNote)

                    notesRefreshKey++
                    noteEditorFor = null
                    noteEditorTopic = null
                }
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = belt.lightColor,
            contentColor = Color(0xFF1B1B1B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 2.dp)
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.End
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val beltRes = when (belt) {
                        Belt.WHITE  -> il.kmi.app.R.drawable.belt_white
                        Belt.YELLOW -> il.kmi.app.R.drawable.belt_yellow
                        Belt.ORANGE -> il.kmi.app.R.drawable.belt_orange
                        Belt.GREEN  -> il.kmi.app.R.drawable.belt_green
                        Belt.BLUE   -> il.kmi.app.R.drawable.belt_blue
                        Belt.BROWN  -> il.kmi.app.R.drawable.belt_brown
                        Belt.BLACK  -> il.kmi.app.R.drawable.belt_black
                    }

                    val beltLabel = if (isEnglish) {
                        when (belt) {
                            Belt.WHITE -> "White Belt"
                            Belt.YELLOW -> "Yellow Belt"
                            Belt.ORANGE -> "Orange Belt"
                            Belt.GREEN -> "Green Belt"
                            Belt.BLUE -> "Blue Belt"
                            Belt.BROWN -> "Brown Belt"
                            Belt.BLACK -> "Black Belt"
                        }
                    } else {
                        belt.heb
                    }

                    Spacer(Modifier.height(0.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryToggleButton(
                            text = tr("התקדמות", "Progress"),
                            iconColor = belt.color,
                            selected = showProgress,
                            onClick = {
                                showProgress = !showProgress
                                if (showProgress) {
                                    showComparison = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        SummaryToggleButton(
                            text = tr("השוואה", "Compare"),
                            iconColor = belt.color,
                            selected = showComparison,
                            onClick = {
                                showComparison = !showComparison
                                if (showComparison) {
                                    showProgress = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (showComparison) {
                    UserProgressComparisonCard(
                        comparison = userProgressComparison,
                        isLoaded = userProgressComparisonLoaded,
                        belt = belt,
                        isEnglish = isEnglish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 10.dp),
                        onClose = {
                            showComparison = false
                        }
                    )
                }

                loadError?.let { err ->
                    Text(
                        text = tr("שגיאה: $err", "Error: $err"),
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (showProgress) {
                    Spacer(Modifier.height(8.dp))

                    val beltLabel = remember(belt, isEnglish) {
                        if (isEnglish) {
                            when (belt) {
                                Belt.WHITE -> "White Belt"
                                Belt.YELLOW -> "Yellow Belt"
                                Belt.ORANGE -> "Orange Belt"
                                Belt.GREEN -> "Green Belt"
                                Belt.BLUE -> "Blue Belt"
                                Belt.BROWN -> "Brown Belt"
                                Belt.BLACK -> "Black Belt"
                            }
                        } else {
                            val h = belt.heb.trim()
                            if (h.startsWith("חגורה")) h else "חגורה $h"
                        }
                    }

                    val notKnownCount = masteredMap.values.count { it == MarkState.NO }
                    val unmarkedCount = (overallTotal - overallDone - notKnownCount).coerceAtLeast(0)

                    Surface(
                        modifier = Modifier
                            .widthIn(max = 318.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.98f),
                                            belt.color.copy(alpha = 0.045f),
                                            Color.White.copy(alpha = 0.96f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = if (isEnglish) 0.dp else 42.dp,
                                            end = if (isEnglish) 42.dp else 0.dp
                                        ),
                                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                ) {
                                    Text(
                                        text = tr("מד התקדמות", "progress meter"),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1F2937),
                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = if (isEnglish) {
                                            "$beltLabel progress overview"
                                        } else {
                                            "סקירת התקדמות עבור $beltLabel"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF667085),
                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                IconButton(
                                    onClick = { showProgress = false },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .offset(y = (-5).dp)
                                        .align(
                                            if (isEnglish) {
                                                androidx.compose.ui.AbsoluteAlignment.TopRight
                                            } else {
                                                androidx.compose.ui.AbsoluteAlignment.TopLeft
                                            }
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = tr("סגור מד התקדמות", "Close progress meter"),
                                        tint = Color(0xFF475467)
                                    )
                                }
                            }

                            ProgressMeter(
                                vm = vm,
                                belt = belt,
                                topic = null,
                                modifier = Modifier.size(194.dp),
                                meterSize = 194.dp,
                                stroke = 16.dp,
                                doneOverride = overallDone,
                                totalOverride = overallTotal,
                                knownOverride = overallDone,
                                notKnownOverride = notKnownCount,
                                unmarkedOverride = unmarkedCount
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = Color(0xFF4CAF50).copy(alpha = 0.26f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tr("יודע: $overallDone", "Known: $overallDone"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF2E7D32),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFE53935).copy(alpha = 0.12f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = Color(0xFFE53935).copy(alpha = 0.26f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tr("לא יודע: $notKnownCount", "No: $notKnownCount"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFC62828),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF98A2B3).copy(alpha = 0.12f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = Color(0xFF98A2B3).copy(alpha = 0.26f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tr("לא סומן: $unmarkedCount", "Open: $unmarkedCount"),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF667085),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .padding(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (itemsByTopic.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = tr(
                                    "לא נמצאו פריטים להצגה עבור החגורה או הנושא שנבחרו.",
                                    "No items were found for the selected belt or topic."
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.98f),
                                                belt.color.copy(alpha = 0.06f),
                                                Color.White.copy(alpha = 0.95f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .padding(16.dp),
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                            )
                        }
                    } else {
                        itemsByTopic.forEach { (topicTitle, items) ->
                            val (done, total) = topicStats[topicTitle] ?: (0 to 0)
                            val pct = if (total > 0) (done * 100 / total) else 0

                            val rowsBySubTopic = items
                                .groupBy { row ->
                                    row.subTopicTitle?.trim().orEmpty()
                                }
                                .toSortedMap(compareBy<String> { key ->
                                    if (key.isBlank()) "000" else key
                                })

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.94f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.98f),
                                                    belt.color.copy(alpha = 0.055f),
                                                    Color.White.copy(alpha = 0.98f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isEnglish)
                                                "${topicDisplayName(topicTitle, true)} - $pct%"
                                            else
                                                "$topicTitle – $pct%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFF263238)
                                        )
                                    }

                                    if (items.isEmpty()) {
                                        Text(
                                            text = tr("אין פריטים בנושא הזה.", "No items in this topic."),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        rowsBySubTopic.forEach { (subTopicTitleRaw, rowsInSubTopic) ->
                                            val subDone = rowsInSubTopic.count { row ->
                                                val statusId = summaryExerciseIdentityIdFor(
                                                    belt = belt,
                                                    topicKey = row.statusTopicKey,
                                                    topicTitle = row.sourceTopicTitle,
                                                    index = row.indexInStatusGroup,
                                                    item = row.itemRaw
                                                )

                                                masteredMap[topicTitle to statusId] == MarkState.YES
                                            }

                                            val subTotal = rowsInSubTopic.size
                                            val subPct = if (subTotal > 0) (subDone * 100 / subTotal) else 0

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                color = belt.color.copy(alpha = 0.055f),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = belt.color.copy(alpha = 0.10f)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (isEnglish) {
                                                            "${subTopicDisplayName(subTopicTitleRaw, topicTitle, true)} - $subPct%"
                                                        } else {
                                                            "${subTopicDisplayName(subTopicTitleRaw, topicTitle, false)} – $subPct%"
                                                        },
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF455A64),
                                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    rowsInSubTopic.forEach { row ->
                                                        val itemRaw = row.itemRaw
                                                        val canonicalId = canonicalFromRepo(row.sourceTopicTitle, itemRaw)

                                                        val statusId = summaryExerciseIdentityIdFor(
                                                            belt = belt,
                                                            topicKey = row.statusTopicKey,
                                                            topicTitle = row.sourceTopicTitle,
                                                            index = row.indexInStatusGroup,
                                                            item = itemRaw
                                                        )

                                                        val state = masteredMap[topicTitle to statusId] ?: MarkState.NONE

                                                        val (bg, fg, mark) = when (state) {
                                                            MarkState.YES  -> Triple(Color(0xFF4CAF50), Color.White, "✓")
                                                            MarkState.NO   -> Triple(Color(0xFFE53935), Color.White, "✗")
                                                            MarkState.NONE -> Triple(Color(0xFFE0E0E0), Color(0xFF616161), "○")
                                                        }

                                                        val cleanFavId = cleanItem(row.sourceTopicTitle, canonicalId)
                                                        val isFav = favorites.contains(cleanFavId)
                                                        val itemHasNote = hasNote(row.sourceTopicTitle, cleanFavId)

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    color = if (state == MarkState.YES) {
                                                                        belt.color.copy(alpha = 0.075f)
                                                                    } else {
                                                                        Color.Transparent
                                                                    },
                                                                    shape = RoundedCornerShape(14.dp)
                                                                )
                                                                .padding(vertical = 6.dp, horizontal = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.End
                                                        ) {
                                                            Surface(
                                                                modifier = Modifier.size(24.dp),
                                                                shape = CircleShape,
                                                                color = bg,
                                                                shadowElevation = 2.dp,
                                                                tonalElevation = 0.dp
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    if (state == MarkState.NONE) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .size(8.dp)
                                                                                .border(
                                                                                    width = 1.7.dp,
                                                                                    color = fg,
                                                                                    shape = CircleShape
                                                                                )
                                                                        )
                                                                    } else {
                                                                        Text(
                                                                            text = mark,
                                                                            color = fg,
                                                                            fontWeight = FontWeight.ExtraBold,
                                                                            textAlign = TextAlign.Center,
                                                                            style = MaterialTheme.typography.labelMedium
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Spacer(Modifier.width(8.dp))

                                                            Column(
                                                                modifier = Modifier.weight(1f),
                                                                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                                            ) {
                                                                Text(
                                                                    text = exerciseDisplayNameForUi(row.sourceTopicTitle, itemRaw, isEnglish),
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    color = Color(0xFF1B1B1B)
                                                                )

                                                                if (itemHasNote) {
                                                                    Text(
                                                                        text = tr("יש הערה שמורה", "Saved note exists"),
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    )
                                                                }
                                                            }

                                                            Spacer(Modifier.width(8.dp))

                                                            var menuExpanded by remember { mutableStateOf(false) }
                                                            val infoScale by animateFloatAsState(
                                                                targetValue = if (menuExpanded) 1.08f else 1f,
                                                                animationSpec = tween(180),
                                                                label = "summaryInfoScale"
                                                            )
                                                            val infoRotation by animateFloatAsState(
                                                                targetValue = if (menuExpanded) 12f else 0f,
                                                                animationSpec = tween(180),
                                                                label = "summaryInfoRotation"
                                                            )

                                                            Box {
                                                                Surface(
                                                                    onClick = { menuExpanded = true },
                                                                    shape = CircleShape,
                                                                    color = Color.White.copy(alpha = 0.94f),
                                                                    shadowElevation = 5.dp,
                                                                    border = BorderStroke(
                                                                        1.dp,
                                                                        belt.color.copy(alpha = 0.14f)
                                                                    ),
                                                                    modifier = Modifier
                                                                        .size(38.dp)
                                                                        .graphicsLayer {
                                                                            scaleX = infoScale
                                                                            scaleY = infoScale
                                                                        }
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .fillMaxSize()
                                                                            .background(
                                                                                brush = Brush.verticalGradient(
                                                                                    colors = listOf(
                                                                                        Color.White.copy(alpha = 0.98f),
                                                                                        belt.color.copy(alpha = 0.05f),
                                                                                        Color.White.copy(alpha = 0.94f)
                                                                                    )
                                                                                ),
                                                                                shape = CircleShape
                                                                            ),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Filled.Info,
                                                                            contentDescription = tr("אפשרויות", "Options"),
                                                                            tint = Color(0xFF455A64),
                                                                            modifier = Modifier
                                                                                .size(18.dp)
                                                                                .graphicsLayer {
                                                                                    rotationZ = infoRotation
                                                                                }
                                                                        )
                                                                    }
                                                                }

                                                                DropdownMenu(
                                                                    expanded = menuExpanded,
                                                                    onDismissRequest = { menuExpanded = false },
                                                                    modifier = Modifier
                                                                        .background(
                                                                            brush = Brush.verticalGradient(
                                                                                colors = listOf(
                                                                                    Color.White.copy(alpha = 0.99f),
                                                                                    belt.color.copy(alpha = 0.05f),
                                                                                    Color.White.copy(alpha = 0.97f)
                                                                                )
                                                                            ),
                                                                            shape = RoundedCornerShape(18.dp)
                                                                        )
                                                                        .border(
                                                                            1.dp,
                                                                            belt.color.copy(alpha = 0.12f),
                                                                            RoundedCornerShape(18.dp)
                                                                        )
                                                                ) {
                                                                    DropdownMenuItem(
                                                                        text = {
                                                                            Text(
                                                                                tr("מידע", "Info"),
                                                                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                                                modifier = Modifier.fillMaxWidth()
                                                                            )
                                                                        },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            explainFromSearch = Triple(belt, row.sourceTopicTitle, itemRaw)
                                                                        }
                                                                    )

                                                                    DropdownMenuItem(
                                                                        text = {
                                                                            Text(
                                                                                if (isFav)
                                                                                    tr("הסר ממועדפים", "Remove from favorites")
                                                                                else
                                                                                    tr("הוסף למועדפים", "Add to favorites"),
                                                                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                                                modifier = Modifier.fillMaxWidth()
                                                                            )
                                                                        },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            FavoritesStore.toggle(cleanFavId)
                                                                        }
                                                                    )

                                                                    DropdownMenuItem(
                                                                        text = {
                                                                            Text(
                                                                                tr("הוסף הערה למתרגל", "Add note for trainee"),
                                                                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                                                modifier = Modifier.fillMaxWidth()
                                                                            )
                                                                        },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            noteEditorTopic = row.sourceTopicTitle
                                                                            noteEditorFor = cleanFavId
                                                                            noteDraft = loadNote(row.sourceTopicTitle, cleanFavId)
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------ PDF: Summary ------------------------------ */

private fun createSummaryPdf(
    dir: File,
    belt: Belt,
    itemsByTopic: Map<String, List<SummaryExerciseRow>>,
    masteredMap: Map<Pair<String, String>, MarkState>,
    isEnglish: Boolean = false,
    topic: String = "",
    subTopicFilter: String? = null
): File {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas

    val pageWidth = 595f
    val leftX = 45f
    val rightX = 550f
    val textX = if (isEnglish) leftX else rightX

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
        color = android.graphics.Color.BLACK
    }

    var y = 50f
    canvas.drawText(
        if (isEnglish) "Summary report - ${belt.id.replaceFirstChar { it.uppercase() }}" else "דו״ח סיכום – ${belt.heb}",
        textX,
        y,
        paint
    )
    y += 30f

    itemsByTopic.forEach { (topicTitle, items) ->
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 16f
        val topicLabel = if (isEnglish) topicDisplayName(topicTitle, true) else topicTitle
        canvas.drawText(if (isEnglish) "Topic: $topicLabel" else "נושא: $topicTitle", textX, y, paint)
        y += 22f

        paint.textSize = 13.5f
        items.forEach { row ->
            val itemRaw = row.itemRaw

            val statusId = summaryExerciseIdentityIdFor(
                belt = belt,
                topicKey = row.statusTopicKey,
                topicTitle = row.sourceTopicTitle,
                index = row.indexInStatusGroup,
                item = itemRaw
            )

            // ✅ אותו מפתח בדיוק כמו ב-SummaryScreen / MaterialsScreen
            val state = masteredMap[topicTitle to statusId] ?: MarkState.NONE

            val circleX = if (isEnglish) pageWidth - 60f else 60f
            val circleY = y - 7f

            val circleColor = when (state) {
                MarkState.YES  -> android.graphics.Color.rgb(76, 175, 80)
                MarkState.NO   -> android.graphics.Color.rgb(229, 57, 53)
                MarkState.NONE -> android.graphics.Color.rgb(189, 189, 189)
            }

            val markText = when (state) {
                MarkState.YES  -> "✓"
                MarkState.NO   -> "✘"
                MarkState.NONE -> "○"
            }

            val markTextColor = when (state) {
                MarkState.NONE -> android.graphics.Color.rgb(97, 97, 97)
                else -> android.graphics.Color.WHITE
            }

            val circlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = circleColor
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(circleX, circleY, 8f, circlePaint)

            paint.color = android.graphics.Color.BLACK
            paint.textAlign = if (isEnglish) {
                android.graphics.Paint.Align.LEFT
            } else {
                android.graphics.Paint.Align.RIGHT
            }
            val display = exerciseDisplayNameForUi(row.sourceTopicTitle, itemRaw, isEnglish)
            canvas.drawText(display, textX, y, paint)

            val markPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = markTextColor
                textSize = 10f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.SANS_SERIF,
                    android.graphics.Typeface.BOLD
                )
            }
            canvas.drawText(markText, circleX, circleY + 4f, markPaint)

            y += 22f
            if (y > 790f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
        }

        y += 10f
        if (y > 790f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
        }
    }

    document.finishPage(page)

    val file = File(dir, "summary_${belt.id}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return file
}