package il.kmi.app.screens

import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import il.kmi.app.KmiViewModel
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.ui.color
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.LocalAppIconScale
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import java.io.File
import java.io.FileOutputStream
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.CancellationException
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.core.content.edit
import androidx.core.graphics.withTranslation
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.progress.CoachGroupProgressSummary
import il.kmi.app.progress.UserProgressComparison
import il.kmi.app.progress.UserProgressRepository

/* ------------------------------ MarkState (3 states) ------------------------------ */

private enum class MarkState {
    YES,
    NO,
    NONE
}

private enum class CoachSummaryStatus(
    val storageValue: String
) {
    NOT_TAUGHT("not_taught"),
    TAUGHT("taught"),
    PRACTICED("practiced"),
    NEEDS_REINFORCEMENT("needs_reinforcement");

    companion object {
        fun fromStorage(value: String?): CoachSummaryStatus {
            return entries.firstOrNull { status ->
                status.storageValue == value
            } ?: NOT_TAUGHT
        }
    }
}

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
    val direct: List<String> = SharedContentRepo.getAllItemsFor(
        belt = b,
        topicTitle = t,
        subTopicTitle = null
    )
        .map { raw: String -> raw.trim() }
        .filter { raw: String -> raw.isNotBlank() }

    for (raw: String in direct) {
        val formatted: String = ExerciseTitleFormatter.displayName(raw)
            .trim()

        val disp = formatted.ifBlank { raw }.trim()

        if (norm(disp) == wanted || norm(raw) == wanted) {
            return raw
        }
    }

    // 2) פריטים מתוך תתי-נושאים
    val subs: List<SharedContentRepo.SubTopic> = SharedContentRepo.getSubTopicsFor(b, t)

    for (st: SharedContentRepo.SubTopic in subs) {
        val subItems: List<String> = st.items
            .map { raw: String -> raw.trim() }
            .filter { raw: String -> raw.isNotBlank() }

        for (raw: String in subItems) {
            val formatted: String = ExerciseTitleFormatter.displayName(raw)
                .trim()

            val disp = formatted.ifBlank { raw }.trim()

            if (norm(disp) == wanted || norm(raw) == wanted) {
                return raw
            }
        }
    }

    return null
}

private fun resolveCanonicalIdForExplanation(
    belt: Belt,
    topicTitle: String,
    rawItemFromRepo: String
): String {
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

private fun topicDisplayName(topicTitle: String, isEnglish: Boolean): String {
    val clean = topicTitle.trim()
    if (!isEnglish) return clean

    val translated: String = ExerciseTitlesEn.get(clean)
        .orEmpty()
        .trim()

    if (translated.isNotBlank()) {
        return translated
    }

    return when (clean) {
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
    isEnglish: Boolean
): String {
    val clean =
        subTopicTitle
            ?.trim()
            .orEmpty()

    // אם במסך התרגילים אין תת־נושא,
    // גם במסך הסיכום לא ממציאים תת־נושא מלאכותי.
    if (clean.isBlank()) {
        return ""
    }

    if (!isEnglish) {
        return clean
    }

    val translated =
        ExerciseTitlesEn.get(clean)
            .orEmpty()
            .trim()

    return translated.ifBlank {
        clean
    }
}

private fun exerciseDisplayNameForUi(
    topicTitle: String,
    rawItem: String,
    isEnglish: Boolean
): String {
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

    val formatted: String =ExerciseTitleFormatter.displayName(cleaned)
        .trim()

    val base = formatted.ifBlank { cleaned }.trim()

    if (!isEnglish) return base

    val candidates: List<String> = listOf(
        base,
        normalizeForLookup(base),
        cleaned,
        normalizeForLookup(cleaned),
        rawItem.trim(),
        normalizeForLookup(rawItem.trim()),
        rawItem.substringAfter("::", rawItem).trim(),
        normalizeForLookup(rawItem.substringAfter("::", rawItem).trim())
    )
        .map { candidate: String -> candidate.trim() }
        .filter { candidate: String -> candidate.isNotBlank() }
        .distinct()

    for (candidate: String in candidates) {
        val translated: String = ExerciseTitlesEn.get(candidate)
            .orEmpty()
            .trim()

        if (translated.isNotBlank()) {
            return translated
        }
    }

    return base
}

/* ------------------------------ ProgressMeter ------------------------------ */

@Composable
fun ProgressMeter(
    vm: KmiViewModel,
    belt: Belt,
    modifier: Modifier = Modifier,
    topic: String? = null,
    meterSize: Dp = 180.dp,
    stroke: Dp = 14.dp,
    doneOverride: Int? = null,
    totalOverride: Int? = null,
    knownOverride: Int? = null,
    partiallyKnownOverride: Int? = null,
    notKnownOverride: Int? = null
) {
    var knownCount by remember(belt, topic) { mutableIntStateOf(0) }
    var notKnownCount by remember(belt, topic) { mutableIntStateOf(0) }
    var unmarkedCount by remember(belt, topic) { mutableIntStateOf(0) }
    var total by remember(belt, topic) { mutableIntStateOf(0) }

    val marksVer by vm.marksVersion.collectAsState()

    LaunchedEffect(belt, topic, marksVer) {
        val beltContent = beltContentFor(belt)

        val beltTopics: List<SharedContentRepo.Topic> =
            beltContent?.topics.orEmpty()

        val titles: List<String> =
            if (topic.isNullOrBlank()) {
                beltTopics.map { topicObj: SharedContentRepo.Topic ->
                    topicObj.title
                }
            } else {
                listOf(topic)
            }

        var t = 0
        var yes = 0
        var no = 0
        var none = 0

        titles.forEach { tp: String ->
            val tpObj: SharedContentRepo.Topic? =
                beltTopics.firstOrNull { topicObj: SharedContentRepo.Topic ->
                    norm(topicObj.title) == norm(tp)
                }

            val items: List<String> =
                if (tpObj == null) {
                    emptyList()
                } else {
                    (
                            tpObj.items +
                                    tpObj.subTopics.flatMap { subTopic: SharedContentRepo.SubTopic ->
                                        subTopic.items
                                    }
                            )
                        .map { raw: String -> raw.trim() }
                        .filter { raw: String -> raw.isNotBlank() }
                        .distinct()
                }

            t += items.size

            val topicSnap = vm.getTopicStatusSnapshot(belt, tp)

            items.forEachIndexed { index: Int, raw: String ->
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

    val effectiveKnown =
        knownOverride
            ?: doneOverride
            ?: knownCount

    val effectivePartiallyKnown =
        partiallyKnownOverride
            ?: 0

    val effectiveNotKnown =
        notKnownOverride
            ?: notKnownCount

    val effectiveTotal =
        totalOverride
            ?: total

    /*
     * במצב מתאמן אין חפיפה בין הקטגוריות.
     *
     * במצב מאמן אותו תרגיל יכול להיות גם „נלמד”
     * וגם „תורגל”. לכן doneOverride מחזיק את מספר
     * התרגילים הייחודיים שסומנו, ואינו סכום
     * של שלוש קטגוריות המאמן.
     */
    val categorySelectionsTotal =
        (
                effectiveKnown +
                        effectivePartiallyKnown +
                        effectiveNotKnown
                )
            .coerceAtLeast(0)

    val effectiveCompleted =
        (
                doneOverride
                    ?: categorySelectionsTotal
                )
            .coerceIn(
                minimumValue = 0,
                maximumValue = effectiveTotal
            )

    /*
     * אם קיימים שני סימוני מאמן לאותו תרגיל,
     * מקטינים את שלושת המקטעים באותו יחס.
     *
     * כך כל הצבעים מוצגים, אבל אורך הקשת הכולל
     * נשאר שווה למספר התרגילים הייחודיים שסומנו.
     */
    val categorySweepScale =
        if (
            categorySelectionsTotal >
            effectiveCompleted &&
            categorySelectionsTotal > 0
        ) {
            effectiveCompleted.toFloat() /
                    categorySelectionsTotal.toFloat()
        } else {
            1f
        }

    val pct: Int =
        if (effectiveTotal == 0) {
            0
        } else {
            effectiveCompleted * 100 /
                    effectiveTotal
        }

    val animatedKnownSweep by
    animateFloatAsState(
        targetValue =
            if (effectiveTotal == 0) {
                0f
            } else {
                360f *
                        (
                                effectiveKnown.toFloat() *
                                        categorySweepScale /
                                        effectiveTotal.toFloat()
                                )
            },
        animationSpec =
            tween(durationMillis = 950),
        label = "premiumKnownSweep"
    )

    val animatedPartiallyKnownSweep by
    animateFloatAsState(
        targetValue =
            if (effectiveTotal == 0) {
                0f
            } else {
                360f *
                        (
                                effectivePartiallyKnown
                                    .toFloat() *
                                        categorySweepScale /
                                        effectiveTotal.toFloat()
                                )
            },
        animationSpec =
            tween(durationMillis = 950),
        label =
            "premiumPartiallyKnownSweep"
    )

    val animatedNotKnownSweep by
    animateFloatAsState(
        targetValue =
            if (effectiveTotal == 0) {
                0f
            } else {
                360f *
                        (
                                effectiveNotKnown
                                    .toFloat() *
                                        categorySweepScale /
                                        effectiveTotal.toFloat()
                                )
            },
        animationSpec =
            tween(durationMillis = 950),
        label =
            "premiumNotKnownSweep"
    )

    val context = LocalContext.current
    val languageManager = remember { AppLanguageManager(context) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val colorScheme =
        MaterialTheme.colorScheme

    val knownColor = Color(0xFF4CAF50)
    val partiallyKnownColor = Color(0xFFF28C28)
    val notKnownColor = Color(0xFFE53935)

    val unmarkedColor =
        colorScheme.outlineVariant.copy(
            alpha = 0.78f
        )

    val meterSurfaceColor =
        colorScheme.surface

    val innerMeterColor =
        belt.color
            .copy(alpha = 0.07f)
            .compositeOver(
                colorScheme.surfaceVariant
            )

    val centerTextColor =
        colorScheme.onSurface

    val subTextColor =
        colorScheme.onSurfaceVariant

    val remaining =
        (
                effectiveTotal -
                        effectiveCompleted
                )
            .coerceAtLeast(0)

    Box(
        modifier = modifier.size(meterSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = stroke.toPx()
            var startAngle = -90f

            drawCircle(
                color = meterSurfaceColor
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

            if (
                animatedPartiallyKnownSweep >
                0.1f
            ) {
                drawArc(
                    color = partiallyKnownColor,
                    startAngle = startAngle,
                    sweepAngle =
                        animatedPartiallyKnownSweep,
                    useCenter = false,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    )
                )

                startAngle +=
                    animatedPartiallyKnownSweep
            }


            if (animatedNotKnownSweep > 0.1f) {
                drawArc(
                    color = notKnownColor,
                    startAngle = startAngle,
                    sweepAngle = animatedNotKnownSweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            val innerWhiteRadius = size.minDimension * 0.34f
            val innerSoftRadius = size.minDimension * 0.28f

            drawCircle(
                color = meterSurfaceColor,
                radius = innerWhiteRadius
            )

            drawCircle(
                color = innerMeterColor,
                radius = innerSoftRadius
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$pct%",
                style = KmiTypography.metric,
                color = centerTextColor,
                maxLines = 1
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (isEnglish) "Marked" else "סומנו",
                style = KmiTypography.cardTitle,
                color = knownColor,
                maxLines = 1
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text =
                    if (isEnglish) {
                        "$effectiveCompleted out of $effectiveTotal"
                    } else {
                        "$effectiveCompleted מתוך $effectiveTotal"
                    },
                style = KmiTypography.body.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = subTextColor,
                maxLines = 1
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
                    text =
                        if (!topic.isNullOrBlank()) {
                            topic
                        } else {
                            if (isEnglish) {
                                "$remaining remaining"
                            } else {
                                "נותרו $remaining"
                            }
                        },
                    style = KmiTypography.secondary.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color =
                        colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 7.dp
                    )
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

    /*
    * המשתמש הנוכחי כבר הוסר מהחישוב ב־Repository.
    * לכן גם מתאמן אחר אחד הוא נתון השוואה אמיתי ותקין.
    */
    val hasValidComparisonData =
        comparison != null &&
                comparison.usersCount >= 1

    val textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val columnAlignment = if (isEnglish) Alignment.Start else Alignment.End

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = belt.color.copy(alpha = 0.28f)
        ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            belt.color
                                .copy(alpha = 0.10f)
                                .compositeOver(MaterialTheme.colorScheme.surface),
                            MaterialTheme.colorScheme.surface
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
                    .heightIn(min = 38.dp)
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
                    Text(
                        text = titleText,
                        style = KmiTypography.sectionTitle,
                        color = Color(0xFF172033),
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!isLoaded) {
                Text(
                    text = loadingText,
                    style = KmiTypography.body.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF475467),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!hasValidComparisonData) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.72f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = belt.color.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = notEnoughText,
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF667085),
                        textAlign = textAlign,
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 12.dp
                        )
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryMiniProgressChip(
                        title = if (isEnglish) "You know" else "אתה יודע",
                        value = "${comparison.userKnownPercent}%",
                        color = belt.color,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryMiniProgressChip(
                        title = if (isEnglish) "Average" else "ממוצע",
                        value = "${comparison.averageKnownPercent}%",
                        color = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )

                    SummaryMiniProgressChip(
                        title = if (isEnglish) "Trainees" else "מתאמנים",
                        value = comparison.usersCount.toString(),
                        color = Color(0xFF475467),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text =
                        if (isEnglish) {
                            "You are above ${comparison.percentileAbove}% of trainees in your belt."
                        } else {
                            "אתה מעל ${comparison.percentileAbove}% מהמתאמנים בחגורה שלך."
                        },
                    style = KmiTypography.body.copy(
                        fontWeight = FontWeight.Bold
                    ),
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
        modifier =
            modifier.heightIn(
                min = 76.dp
            ),
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
                style = KmiTypography.metric,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = title,
                style = KmiTypography.caption.copy(
                    fontWeight = FontWeight.Bold
                ),
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
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    /*
     * הכפתור כולו צבוע. אין Surface שקוף, רקע לבן,
     * מסגרת לבנה או שכבה נוספת מתחת לכפתור.
     */
    val buttonBrush =
        if (selected) {
            Brush.horizontalGradient(
                colors =
                    listOf(
                        Color(0xFF00B8D9),
                        Color(0xFF1677FF),
                        Color(0xFF6D28D9)
                    )
            )
        } else {
            Brush.horizontalGradient(
                colors =
                    listOf(
                        Color(0xFF0E7490),
                        Color(0xFF2563A8),
                        Color(0xFF5B4BDB)
                    )
            )
        }

    Surface(
        onClick = onClick,
        modifier =
            modifier.heightIn(
                min = 48.dp
            ),
        shape = RoundedCornerShape(19.dp),
        color = colorScheme.primary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border =
            BorderStroke(
                width = 0.75.dp,
                color =
                    Color.White.copy(
                        alpha =
                            if (selected) {
                                0.52f
                            } else {
                                0.28f
                            }
                    )
            )
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = buttonBrush
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.Center
            ) {
                Text(
                    text = text,
                    style = KmiTypography.action,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.width(7.dp))

                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color = Color.White.copy(
                        alpha = if (selected) 0.24f else 0.17f
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Insights,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumSummaryLoadingRing(
    size: Dp,
    width: Dp,
    rotation: Float,
    colors: List<Color>
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation
            }
            .border(
                width = width,
                brush = Brush.sweepGradient(colors),
                shape = CircleShape
            )
    )
}

@Composable
private fun PremiumSummaryLoading() {
    val infiniteTransition = rememberInfiniteTransition(
        label = "premiumSummaryLoading"
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
        label = "premiumSummaryOuterRotation"
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
        label = "premiumSummaryInnerRotation"
    )

    Box(
        modifier = Modifier.size(82.dp),
        contentAlignment = Alignment.Center
    ) {
        PremiumSummaryLoadingRing(
            size = 76.dp,
            width = 5.dp,
            rotation = outerRotation,
            colors = listOf(
                Color.Transparent,
                Color(0xFFA78BFA),
                Color(0xFF38BDF8),
                Color.Transparent
            )
        )

        PremiumSummaryLoadingRing(
            size = 52.dp,
            width = 4.dp,
            rotation = innerRotation,
            colors = listOf(
                Color.Transparent,
                Color(0xFFF59E0B),
                Color(0xFF22C55E),
                Color.Transparent
            )
        )

        Surface(
            modifier = Modifier.size(25.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.42f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFEDE9FE),
                                Color(0xFFE0F2FE)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    style = KmiTypography.caption,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F5E9C),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CoachGroupsProgressCard(
    summary: CoachGroupProgressSummary?,
    isLoaded: Boolean,
    belt: Belt,
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val surfaceColor =
        MaterialTheme.colorScheme.surface

    val primaryTextColor =
        MaterialTheme.colorScheme.onSurface

    val secondaryTextColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = surfaceColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = belt.color.copy(alpha = 0.38f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            surfaceColor,
                            belt.color
                                .copy(alpha = 0.12f)
                                .compositeOver(surfaceColor),
                            surfaceColor
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription =
                            if (isEnglish) {
                                "Close group data"
                            } else {
                                "סגור נתוני קבוצות"
                            },
                        tint = secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text =
                        if (isEnglish) {
                            "Your groups · ${belt.id}"
                        } else {
                            "נתוני הקבוצות · ${belt.heb}"
                        },
                    modifier = Modifier.weight(1f),
                    style =
                        KmiTypography.sectionTitle.copy(
                            fontWeight =
                                FontWeight.Black
                        ),
                    color = primaryTextColor,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Start
                        } else {
                            TextAlign.End
                        }
                )
            }

            if (!isLoaded) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 138.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {
                    PremiumSummaryLoading()

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            if (isEnglish) {
                                "Loading group data..."
                            } else {
                                "טוען את נתוני הקבוצות..."
                            },
                        style = KmiTypography.secondary,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (
                summary == null ||
                summary.totalTrainees <= 0
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            "No trainees were found in the groups assigned to you."
                        } else {
                            "לא נמצאו מתאמנים בקבוצות שאליהן אתה משויך."
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    style =
                        KmiTypography.body.copy(
                            fontWeight =
                                FontWeight.SemiBold
                        ),
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = belt.color.copy(alpha = 0.16f),
                    border = BorderStroke(
                        width = 2.dp,
                        color = belt.color.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.size(138.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text =
                                    "${summary.averageKnownPercent}%",
                                style = KmiTypography.metric,
                                fontWeight = FontWeight.Black,
                                color = primaryTextColor,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text =
                                    if (isEnglish) {
                                        "Average knowledge"
                                    } else {
                                        "ידיעת החומר"
                                    },
                                style = KmiTypography.caption,
                                fontWeight = FontWeight.Bold,
                                color = secondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Text(
                    text =
                        if (isEnglish) {
                            "Average knowledge of the ${belt.id} belt material"
                        } else {
                            "ממוצע ידיעת חומר ${belt.heb}"
                        },
                    modifier = Modifier.fillMaxWidth(),
                    style =
                        KmiTypography.body.copy(
                            fontWeight =
                                FontWeight.Bold
                        ),
                    color = primaryTextColor,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.72f)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 12.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = summary.groupsCount.toString(),
                                style = KmiTypography.metric,
                                fontWeight = FontWeight.Black,
                                color = belt.color
                            )

                            Text(
                                text =
                                    if (isEnglish) {
                                        "Groups"
                                    } else {
                                        "קבוצות"
                                    },
                                style = KmiTypography.caption,
                                fontWeight = FontWeight.Bold,
                                color = secondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.72f)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 12.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = summary.totalTrainees.toString(),
                                style = KmiTypography.metric,
                                fontWeight = FontWeight.Black,
                                color = belt.color
                            )

                            Text(
                                text =
                                    if (isEnglish) {
                                        "Trainees"
                                    } else {
                                        "מתאמנים"
                                    },
                                style = KmiTypography.caption,
                                fontWeight = FontWeight.Bold,
                                color = secondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.72f)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 12.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text =
                                    summary
                                        .traineesWithProgress
                                        .toString(),
                                style = KmiTypography.metric,
                                fontWeight = FontWeight.Black,
                                color = belt.color
                            )

                            Text(
                                text =
                                    if (isEnglish) {
                                        "With data"
                                    } else {
                                        "עם נתונים"
                                    },
                                style = KmiTypography.caption,
                                fontWeight = FontWeight.Bold,
                                color = secondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (!summary.hasProgressData) {
                    Text(
                        text =
                            if (isEnglish) {
                                "The trainees have not saved progress for this belt yet."
                            } else {
                                "המתאמנים עדיין לא שמרו נתוני התקדמות בחגורה זו."
                            },
                        modifier = Modifier.fillMaxWidth(),
                        style = KmiTypography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center
                    )
                } else if (
                    summary.traineesWithoutProgress > 0
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "${summary.traineesWithoutProgress} trainees do not yet have progress data for this belt."
                            } else {
                                "ל־${summary.traineesWithoutProgress} מתאמנים עדיין אין נתוני התקדמות בחגורה זו."
                            },
                        modifier = Modifier.fillMaxWidth(),
                        style = KmiTypography.caption,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center
                    )
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
    @Suppress("UNUSED_PARAMETER")
    onOpenProgress: () -> Unit,
    @Suppress("UNUSED_PARAMETER")
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val languageManager = remember { AppLanguageManager(ctx) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    /*
  * מסך הסיכום מקבל את כל צבעיו מ-AppTheme.
  * כך אין ערכת צבעים מקומית נוספת ואין צורך לבדוק
  * כאן בנפרד אם המצב כהה או בהיר.
  */
    val summaryColors = MaterialTheme.colorScheme

    // אותו רקע מדורג שבו משתמשים המסכים הראשיים באפליקציה.
    val summaryBackgroundColors = listOf(
        summaryColors.background,
        summaryColors.surfaceVariant,
        summaryColors.primaryContainer,
        summaryColors.background
    )

    val summaryCardColor =
        summaryColors.surface

    val summaryCardGradient = listOf(
        summaryColors.surface,
        belt.color.copy(alpha = 0.10f),
        summaryColors.surface
    )

    /*
   * צבעי רשימת הנושאים.
   *
   * רקע כל שורה משלב את צבע החגורה עם צבע המשטח
   * של ערכת הנושא, ולכן מתאים גם למצב בהיר וגם לכהה.
   */
    val summaryTopicTitleColor =
        summaryColors.onSurface

    val summarySubTopicTitleColor =
        summaryColors.tertiary

    val summaryPrimaryText =
        summaryColors.onSurface

    val summarySecondaryText =
        summaryColors.onSurfaceVariant

    val summaryTopicRowColor =
        belt.color
            .copy(alpha = 0.16f)
            .compositeOver(summaryColors.surface)

    val summaryTopicDividerColor =
        belt.color
            .copy(alpha = 0.42f)
            .compositeOver(summaryColors.outlineVariant)

    val scroll = rememberScrollState()
    val focusManager = LocalFocusManager.current

    val notesSp = remember {
        ctx.getSharedPreferences(
            "kmi_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    val currentRole = notesSp
        .getString("user_role", "")
        .orEmpty()
        .lowercase()

    val isCoach =
        currentRole == "coach" ||
                currentRole.contains("coach") ||
                currentRole.contains("מאמן") ||
                currentRole.contains("מדריך")

    val favorites: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    var showProgress by rememberSaveable { mutableStateOf(false) }
    var showComparison by rememberSaveable { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    /*
  * מצב פתוח או סגור נשמר בנפרד לכל נושא.
  * כל הנושאים מתחילים סגורים, והמשתמש פותח
  * רק את הנושא שהוא מעוניין לראות.
  */
    val topicExpandedState = remember(
        belt,
        topic,
        subTopicFilter
    ) {
        mutableStateMapOf<String, Boolean>()
    }

    /*
     * כל עוד רשימת התרגילים והסימונים לא מוכנה,
     * לא מרכיבים את תוכן מסך הסיכום.
     */
    var isSummaryLoading by remember(
        belt,
        topic,
        subTopicFilter
    ) {
        mutableStateOf(true)
    }

    var userProgressComparison by remember {
        mutableStateOf<UserProgressComparison?>(null)
    }

    var userProgressComparisonLoaded by remember {
        mutableStateOf(false)
    }

    var coachGroupProgress by remember {
        mutableStateOf<CoachGroupProgressSummary?>(null)
    }

    var coachGroupProgressLoaded by remember {
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

    fun noteKeyFor(topicTitle: String, itemId: String): String {
        return "note_${belt.id}_${noteSuffixFor(topicTitle)}_${cleanItem(topicTitle, itemId)}"
    }

    fun loadNote(topicTitle: String, itemId: String): String =
        notesSp.getString(noteKeyFor(topicTitle, itemId), "")?.trim().orEmpty()

    fun saveNote(topicTitle: String, itemId: String, value: String) {
        val key = noteKeyFor(topicTitle, itemId)

        notesSp.edit {
            if (value.isBlank()) {
                remove(key)
            } else {
                putString(key, value.trim())
            }
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
        mutableStateOf<Map<String, List<SummaryExerciseRow>>>(
            emptyMap()
        )
    }

    LaunchedEffect(belt, topic, subTopicFilter) {
        val beltContent = beltContentFor(belt)
        val topics: List<SharedContentRepo.Topic> = beltContent?.topics.orEmpty()

        val out = LinkedHashMap<String, List<SummaryExerciseRow>>()

        fun rowsForTopic(
            topicTitle: String,
            requestedSubTopicTitle: String? = null
        ): List<SummaryExerciseRow> {
            val topicObj: SharedContentRepo.Topic =
                topics.firstOrNull { candidate: SharedContentRepo.Topic ->
                    norm(candidate.title) == norm(topicTitle)
                } ?: return emptyList()

            val rows = mutableListOf<SummaryExerciseRow>()

            fun findSubTopicDeep(
                list: List<SharedContentRepo.SubTopic>,
                wantedTitle: String
            ): SharedContentRepo.SubTopic? {
                list.forEach { subTopic: SharedContentRepo.SubTopic ->
                    if (norm(subTopic.title) == norm(wantedTitle)) {
                        return subTopic
                    }

                    val nested: SharedContentRepo.SubTopic? = findSubTopicDeep(
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

                val subItems: List<String> = subTopic.items
                    .map { itemRaw: String -> itemRaw.trim() }
                    .filter { itemRaw: String -> itemRaw.isNotBlank() }
                    .distinct()

                subItems.forEachIndexed { index: Int, itemRaw: String ->
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
                subTopic.subTopics.forEach { nestedSubTopic: SharedContentRepo.SubTopic ->
                    addRowsFromSubTopicDeep(nestedSubTopic)
                }
            }

            if (requestedSubTopicTitle.isNullOrBlank()) {
                val directItems: List<String> = topicObj.items
                    .map { itemRaw: String -> itemRaw.trim() }
                    .filter { itemRaw: String -> itemRaw.isNotBlank() }
                    .distinct()

                directItems.forEachIndexed { index: Int, itemRaw: String ->
                    rows += SummaryExerciseRow(
                        displayTopicTitle = topicObj.title,
                        sourceTopicTitle = topicObj.title,
                        subTopicTitle = null,
                        statusTopicKey = topicObj.title.trim(),
                        itemRaw = itemRaw,
                        indexInStatusGroup = index
                    )
                }

                topicObj.subTopics.forEach { subTopic: SharedContentRepo.SubTopic ->
                    addRowsFromSubTopicDeep(subTopic)
                }
            } else {
                val requested = requestedSubTopicTitle.trim()

                val selectedSubTopic: SharedContentRepo.SubTopic? = findSubTopicDeep(
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

        val allTitles: List<String> = topics.map { topicObj: SharedContentRepo.Topic ->
            topicObj.title
        }

        val orderedTitles: List<String> =
            if (topic.isNotBlank()) {
                listOf(topic) + allTitles.filterNot { title: String ->
                    norm(title) == norm(topic)
                }
            } else {
                allTitles
            }

        orderedTitles.forEach { title: String ->
            val topicObj: SharedContentRepo.Topic? =
                topics.firstOrNull { candidate: SharedContentRepo.Topic ->
                    norm(candidate.title) == norm(title)
                }

            if (topicObj != null) {
                out[topicObj.title] = rowsForTopic(topicObj.title)
            }
        }

        itemsByTopic = out
    }


    /**
     * ✅ masteredMap נשמר לפי (topicTitle, statusId)
     * statusId חייב להיות זהה ל-MaterialsScreen:
     * ExerciseIdentityRegistry / fallback legacy_row_index.
     * לא משתמשים כאן ב-canonicalId כדי למנוע הדבקת סימונים בין תרגילים דומים.
     */
    var masteredMap by remember(
        belt,
        itemsByTopic
    ) {
        mutableStateOf<
                Map<Pair<String, String>, MarkState>
                >(
            emptyMap()
        )
    }

    /*
     * „יודע חלקית” נשמר ב-MaterialsScreen
     * במפתח SharedPreferences נפרד.
     *
     * ה-Set מכיל את אותו צמד שבו משתמש masteredMap:
     * שם הנושא + מזהה התרגיל.
     */
    var partiallyKnownIds by remember(
        belt,
        itemsByTopic
    ) {
        mutableStateOf<
                Set<Pair<String, String>>
                >(
            emptySet()
        )
    }

    /*
     * במסך המאמן ניתן לבחור עד 2 מתוך 3:
     * נלמד / תורגל / נדרש חיזוק.
     *
     * לכן גם מסך הסיכום שומר רשימה של סטטוסים
     * לכל תרגיל ולא סטטוס יחיד.
     */
    var coachStatusMap by remember(
        belt,
        itemsByTopic,
        isCoach
    ) {
        mutableStateOf<
                Map<
                        Pair<String, String>,
                        List<CoachSummaryStatus>
                        >
                >(
            emptyMap()
        )
    }

    fun coachProgressBaseKey(
        row: SummaryExerciseRow,
        statusId: String
    ): String {
        return buildString {
            append("coach_material_progress_")
            append(belt.id)
            append("_")
            append(row.statusTopicKey)
            append("_")
            append(statusId)
        }
    }

    fun loadCoachSummaryStatuses(
        row: SummaryExerciseRow,
        statusId: String
    ): List<CoachSummaryStatus> {

        val key =
            coachProgressBaseKey(
                row = row,
                statusId = statusId
            )

        val selectableStatuses =
            listOf(
                CoachSummaryStatus.TAUGHT,
                CoachSummaryStatus.PRACTICED,
                CoachSummaryStatus.NEEDS_REINFORCEMENT
            )

        val selectedStatuses =
            selectableStatuses
                .filter { status ->
                    notesSp.getBoolean(
                        "${key}_${status.storageValue}_selected",
                        false
                    )
                }
                .take(2)

        /*
         * המבנה החדש קיים — מחזירים את כל
         * הסימונים שנבחרו, עד שניים.
         */
        if (selectedStatuses.isNotEmpty()) {
            return selectedStatuses
        }

        /*
         * תאימות לסימונים הישנים שנשמרו
         * לפני המעבר ל־2 מתוך 3.
         */
        val legacyStatus =
            CoachSummaryStatus.fromStorage(
                notesSp.getString(
                    "${key}_status",
                    null
                )
            )

        return if (
            legacyStatus ==
            CoachSummaryStatus.NOT_TAUGHT
        ) {
            emptyList()
        } else {
            listOf(legacyStatus)
        }
    }

    LaunchedEffect(
        belt,
        itemsByTopic,
        isCoach
    ) {
        if (!isCoach || itemsByTopic.isEmpty()) {
            coachStatusMap = emptyMap()
            return@LaunchedEffect
        }

        val loadedStatuses =
            mutableMapOf<
                    Pair<String, String>,
                    List<CoachSummaryStatus>
                    >()

        itemsByTopic.forEach { (topicTitle, rows) ->
            rows.forEach { row ->

                val statusId =
                    summaryExerciseIdentityIdFor(
                        belt = belt,
                        topicKey = row.statusTopicKey,
                        topicTitle = row.sourceTopicTitle,
                        index = row.indexInStatusGroup,
                        item = row.itemRaw
                    )

                loadedStatuses[
                    topicTitle to statusId
                ] =
                    loadCoachSummaryStatuses(
                        row = row,
                        statusId = statusId
                    )
            }
        }

        coachStatusMap = loadedStatuses
    }

    LaunchedEffect(
        belt,
        itemsByTopic,
        subTopicFilter,
        topic
    ) {
        loadError = null
        isSummaryLoading = true

        if (itemsByTopic.isEmpty()) {
            /*
             * itemsByTopic מתחיל ריק ונבנה ב־LaunchedEffect אחר.
             * משאירים את הטוען פעיל עד שהרשימה תהיה מוכנה.
             */
            masteredMap = emptyMap()
            partiallyKnownIds = emptySet()
            return@LaunchedEffect
        }

        /*
         * מאפשרים ל־Compose לצייר קודם את אנימציית הטעינה,
         * ורק לאחר מכן מתחילים את העבודה הכבדה.
         */
        yield()

        try {
            val statusGroups: Map<String, List<String>> =
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

            /*
             * ממתינים עד שכל הסימונים נמצאים במטמון.
             * רק לאחר מכן מחשבים ומפרסמים את המפה למסך.
             */
            vm.warmUpStatusGroupsAndAwait(
                belt = belt,
                groups = statusGroups
            )

            val computed: Map<Pair<String, String>, MarkState> =
                withContext(Dispatchers.Default) {
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

            /*
             * קוראים את המצב „יודע חלקית” מאותו מפתח
             * שבו MaterialsScreen שומר אותו:
             *
             * partially_known_{beltId}_{topicKey}
             */
            val computedPartiallyKnown:
                    Set<Pair<String, String>> =
                withContext(Dispatchers.Default) {
                    buildSet {
                        itemsByTopic.forEach {
                                (topicTitle, rows) ->

                            rows.forEach { row ->
                                val statusId =
                                    summaryExerciseIdentityIdFor(
                                        belt = belt,
                                        topicKey =
                                            row.statusTopicKey,
                                        topicTitle =
                                            row.sourceTopicTitle,
                                        index =
                                            row.indexInStatusGroup,
                                        item =
                                            row.itemRaw
                                    )

                                val legacyStatusId =
                                    summaryLegacyStatusIdFor(
                                        belt = belt,
                                        topicKey =
                                            row.statusTopicKey,
                                        index =
                                            row.indexInStatusGroup,
                                        item =
                                            row.itemRaw
                                    )

                                val partiallyKnownKey =
                                    "partially_known_" +
                                            "${belt.id}_" +
                                            row.statusTopicKey

                                val savedPartialIds =
                                    notesSp.getStringSet(
                                        partiallyKnownKey,
                                        emptySet()
                                    )
                                        .orEmpty()

                                if (
                                    statusId in savedPartialIds ||
                                    legacyStatusId in
                                    savedPartialIds
                                ) {
                                    add(
                                        topicTitle to statusId
                                    )
                                }
                            }
                        }
                    }
                }

            /*
             * מפרסמים למסך את שתי המפות יחד.
             */
            masteredMap = computed
            partiallyKnownIds =
                computedPartiallyKnown

            isSummaryLoading = false

        } catch (cancelled: CancellationException) {
            /*
             * ביטול של LaunchedEffect אינו שגיאת נתונים.
             * משאירים את הטוען פעיל ומאפשרים ל־Effect החדש להמשיך.
             */
            throw cancelled

        } catch (e: Exception) {
            loadError = e.message
                ?: tr(
                    "שגיאה בקריאת הנתונים",
                    "Error reading data"
                )

            masteredMap = emptyMap()
            partiallyKnownIds = emptySet()
            isSummaryLoading = false
        }
    }

    // ✅ סטטיסטיקות לפי נושא (מבוסס canonicalFromRepo)
    val topicStats: Map<String, Pair<Int, Int>> = remember(
        masteredMap,
        coachStatusMap,
        itemsByTopic,
        topic,
        subTopicFilter,
        isCoach
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

                if (isCoach) {
                    coachStatusMap[
                        topicTitle to statusId
                    ]
                        .orEmpty()
                        .isNotEmpty()
                } else {
                    masteredMap[topicTitle to statusId] ==
                            MarkState.YES
                }
            }

            done to total
        }
    }

    val overallDone = topicStats.values.sumOf { it.first }
    val overallTotal = topicStats.values.sumOf { it.second }
    val overallPct = if (overallTotal <= 0) 0 else ((overallDone * 100f) / overallTotal).toInt()

    // ✅ שומרים את התקדמות המשתמש ברקע, אבל לא טוענים השוואה אוטומטית.
    // ההשוואה נטענת רק כשהמשתמש פותח את כרטיס "השוואה".
    LaunchedEffect(
        belt.id,
        overallDone,
        overallTotal,
        overallPct,
        isCoach
    ) {
        /*
         * סימוני המאמן מתארים חומר שנלמד או תורגל
         * ואינם התקדמות אישית של מתאמן.
         */
        if (
            !isCoach &&
            overallTotal > 0
        ) {
            runCatching {
                UserProgressRepository.saveUserProgress(
                    beltId = belt.id,
                    knownPercent = overallPct,
                    knownCount = overallDone,
                    totalCount = overallTotal
                )
            }
        }
    }

    LaunchedEffect(
        showComparison,
        belt.id,
        overallPct,
        overallTotal,
        isCoach
    ) {
        /*
         * כרטיס ההשוואה הרגיל שייך למתאמן בלבד.
         * במצב מאמן אותו כפתור פותח את נתוני הקבוצות.
         */
        if (
            !showComparison ||
            isCoach
        ) {
            return@LaunchedEffect
        }

        userProgressComparisonLoaded = false
        userProgressComparison = null

        if (overallTotal <= 0) {
            userProgressComparisonLoaded = true
            return@LaunchedEffect
        }

        runCatching {
            /*
             * לפני שקוראים את נתוני ההשוואה,
             * שומרים וממתינים לנתון המדויק שמסך הסיכום
             * עצמו כבר חישב.
             *
             * כך לא ייתכן שבמסך מוצג 5% אבל ב-Firestore
             * עדיין נשאר 0% מסנכרון רקע ישן.
             */
            UserProgressRepository.saveUserProgress(
                beltId = belt.id,
                knownPercent = overallPct,
                knownCount = overallDone,
                totalCount = overallTotal
            )

            /*
             * רק לאחר שהשמירה הסתיימה קוראים את
             * כל המתאמנים ומחשבים ממוצע גלובלי.
             */
            userProgressComparison =
                UserProgressRepository.loadBeltComparison(
                    beltId = belt.id,
                    userKnownPercent = overallPct
                )

            userProgressComparisonLoaded = true
        }.onFailure { error ->
            /*
             * שגיאת טעינה אינה נחשבת ל"אין מספיק נתונים".
             * רושמים אותה גם ב־loadError כדי שלא תיבלע בשקט.
             */
            userProgressComparison = null
            userProgressComparisonLoaded = true

            loadError =
                error.message
                    ?: tr(
                        "שגיאה בטעינת נתוני ההשוואה",
                        "Error loading comparison data"
                    )
        }
    }

    LaunchedEffect(
        showComparison,
        belt.id,
        isCoach
    ) {
        if (
            !showComparison ||
            !isCoach
        ) {
            return@LaunchedEffect
        }

        coachGroupProgressLoaded = false
        coachGroupProgress = null
        loadError = null

        runCatching {
            coachGroupProgress =
                UserProgressRepository
                    .loadCoachGroupsBeltProgress(
                        beltId = belt.id
                    )

            coachGroupProgressLoaded = true
        }.onFailure { error ->
            coachGroupProgress = null
            coachGroupProgressLoaded = true

            loadError =
                error.message
                    ?: tr(
                        "שגיאה בטעינת נתוני הקבוצות",
                        "Error loading group data"
                    )
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
                partiallyKnownIds =
                    partiallyKnownIds,
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
                    showBackNavigation = false,
                    alignTitleEnd = true,

                    currentLang =
                        if (isEnglish) {
                            "en"
                        } else {
                            "he"
                        },

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.1.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.4.dp),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .height(46.8.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.85f)
                                )
                            ),
                            shape = RoundedCornerShape(14.4.dp)
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
                                    .align(Alignment.CenterStart)
                                    .width(58.5.dp)
                                    .fillMaxHeight(0.70f)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.10f),
                                                Color.White.copy(alpha = 0.18f),
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
                                modifier = Modifier.size(14.4.dp)
                            )

                            Spacer(Modifier.width(6.3.dp))

                            Text(
                                text = tr(
                                    "חזרה למסך הנושאים",
                                    "Back to topics screen"
                                ),
                                style = KmiTypography.action.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        if (isSummaryLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = summaryBackgroundColors
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = summaryCardColor,
                    tonalElevation = 8.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 34.dp,
                            vertical = 28.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PremiumSummaryLoading()

                        Text(
                            text = tr(
                                "טוען את כל סימוני התרגילים...",
                                "Loading all exercise marks..."
                            ),
                            style = KmiTypography.sectionTitle,
                            color = summaryPrimaryText,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )

                        Text(
                            text = tr(
                                "מסך הסיכום יוצג מיד כשהנתונים יהיו מוכנים",
                                "The summary will appear when all data is ready"
                            ),
                            style = KmiTypography.secondary,
                            color = summarySecondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            return@Scaffold
        }

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
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = summaryBackgroundColors
                    )
                ),
            color = Color.Transparent,
            contentColor = summaryPrimaryText
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

                    Spacer(Modifier.height(0.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryToggleButton(
                            text = tr("התקדמות", "Progress"),
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
                            text =
                                if (isCoach) {
                                    tr(
                                        "נתוני הקבוצות",
                                        "Group data"
                                    )
                                } else {
                                    tr(
                                        "השוואה",
                                        "Compare"
                                    )
                                },
                            selected = showComparison,
                            onClick = {
                                showComparison =
                                    !showComparison

                                if (showComparison) {
                                    showProgress = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (
                    showComparison &&
                    isCoach
                ) {
                    CoachGroupsProgressCard(
                        summary = coachGroupProgress,
                        isLoaded = coachGroupProgressLoaded,
                        belt = belt,
                        isEnglish = isEnglish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 2.dp,
                                bottom = 10.dp
                            ),
                        onClose = {
                            showComparison = false
                        }
                    )
                } else if (showComparison) {
                    UserProgressComparisonCard(
                        comparison = userProgressComparison,
                        isLoaded = userProgressComparisonLoaded,
                        belt = belt,
                        isEnglish = isEnglish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 2.dp,
                                bottom = 10.dp
                            ),
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

                    val partiallyKnownCount =
                        partiallyKnownIds.size

                    val notKnownCount =
                        masteredMap.entries.count {
                                (key, state) ->
                            state == MarkState.NO &&
                                    key !in partiallyKnownIds
                        }

                    /*
                     * לצורך מד ההתקדמות בלבד, כל תרגיל
                     * משויך לקטגוריה אחת לפי עדיפות:
                     *
                     * חיזוק ← תורגל ← נלמד.
                     *
                     * השיוכים המקוריים נשארים ללא שינוי,
                     * גם כאשר לתרגיל נשמרו שני סימונים.
                     */
                    val coachReinforcementCount =
                        coachStatusMap.values.count { statuses ->
                            CoachSummaryStatus
                                .NEEDS_REINFORCEMENT in
                                    statuses
                        }

                    val coachPracticedCount =
                        coachStatusMap.values.count { statuses ->
                            CoachSummaryStatus
                                .NEEDS_REINFORCEMENT !in
                                    statuses &&
                                    CoachSummaryStatus
                                        .PRACTICED in
                                    statuses
                        }

                    val coachTaughtCount =
                        coachStatusMap.values.count { statuses ->
                            CoachSummaryStatus
                                .NEEDS_REINFORCEMENT !in
                                    statuses &&
                                    CoachSummaryStatus
                                        .PRACTICED !in
                                    statuses &&
                                    CoachSummaryStatus
                                        .TAUGHT in
                                    statuses
                        }

                    /*
                     * במצב מאמן אחוז ההתקדמות מבוסס על מספר
                     * התרגילים שקיבלו לפחות סימון מאמן אחד.
                     *
                     * מוני המתאמן אינם נכנסים למד במצב מאמן.
                     */
                    val effectivePartiallyKnownCount =
                        if (isCoach) {
                            coachPracticedCount
                        } else {
                            partiallyKnownCount
                        }

                    val effectiveNotKnownCount =
                        if (isCoach) {
                            coachReinforcementCount
                        } else {
                            notKnownCount
                        }

                    val unmarkedCount =
                        if (isCoach) {
                            (
                                    overallTotal -
                                            overallDone
                                    )
                                .coerceAtLeast(0)
                        } else {
                            (
                                    overallTotal -
                                            overallDone -
                                            partiallyKnownCount -
                                            notKnownCount
                                    )
                                .coerceAtLeast(0)
                        }

                    Card(
                        modifier = Modifier
                            .widthIn(max = 318.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = summaryCardColor
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = belt.color.copy(alpha = 0.28f)
                        ),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = summaryCardGradient
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 62.dp)
                            ) {
                                IconButton(
                                    onClick = { showProgress = false },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .offset(y = (-13).dp)
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
                                        contentDescription = tr("סגור מד התקדמות", "Close progress meter"),
                                        tint = summarySecondaryText
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.Center)
                                        .absolutePadding(
                                            left = if (isEnglish) 0.dp else 42.dp,
                                            right = if (isEnglish) 42.dp else 0.dp
                                        ),
                                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                ) {
                                    Text(
                                        text = tr(
                                            "מד התקדמות",
                                            "Progress meter"
                                        ),
                                        style = KmiTypography.sectionTitle,
                                        color = summaryPrimaryText,
                                        textAlign =
                                            if (isEnglish) {
                                                TextAlign.Left
                                            } else {
                                                TextAlign.Right
                                            },
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth()
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
                                doneOverride =
                                    if (isCoach) {
                                        overallDone
                                    } else {
                                        null
                                    },
                                totalOverride = overallTotal,
                                knownOverride =
                                    if (isCoach) {
                                        coachTaughtCount
                                    } else {
                                        overallDone
                                    },
                                partiallyKnownOverride =
                                    effectivePartiallyKnownCount,
                                notKnownOverride =
                                    effectiveNotKnownCount
                            )

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(
                                            IntrinsicSize.Min
                                        ),
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(
                                            min = 44.dp
                                        ),
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
                                            text =
                                                if (isCoach) {
                                                    tr(
                                                        "נלמד: $coachTaughtCount",
                                                        "Taught: $coachTaughtCount"
                                                    )
                                                } else {
                                                    tr(
                                                        "יודע: $overallDone",
                                                        "Known: $overallDone"
                                                    )
                                                },
                                            style = KmiTypography.caption.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = Color(0xFF2E7D32),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }

                                /*
                                 * יודע חלקית — קטגוריה כתומה נפרדת.
                                 */
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(
                                            min = 44.dp
                                        ),
                                    shape =
                                        RoundedCornerShape(
                                            16.dp
                                        ),
                                    color =
                                        Color(0xFFF28C28)
                                            .copy(
                                                alpha = 0.12f
                                            ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color =
                                            Color(0xFFF28C28)
                                                .copy(
                                                    alpha = 0.30f
                                                )
                                    )
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.fillMaxSize(),
                                        contentAlignment =
                                            Alignment.Center
                                    ) {
                                        Text(
                                            text =
                                                if (isCoach) {
                                                    tr(
                                                        "תורגל: $coachPracticedCount",
                                                        "Practiced: $coachPracticedCount"
                                                    )
                                                } else {
                                                    tr(
                                                        "חלקית: $partiallyKnownCount",
                                                        "Partial: $partiallyKnownCount"
                                                    )
                                                },
                                            style =
                                                KmiTypography
                                                    .caption
                                                    .copy(
                                                        fontWeight =
                                                            FontWeight
                                                                .ExtraBold
                                                    ),
                                            color =
                                                Color(0xFFB85C00),
                                            textAlign =
                                                TextAlign.Center,
                                            maxLines = 1,
                                            overflow =
                                                androidx.compose
                                                    .ui
                                                    .text
                                                    .style
                                                    .TextOverflow
                                                    .Ellipsis
                                        )
                                    }
                                }

                                /*
                                 * לא יודע — קטגוריה אדומה.
                                 */
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(
                                            min = 44.dp
                                        ),
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
                                            text =
                                                if (isCoach) {
                                                    tr(
                                                        "חיזוק: $coachReinforcementCount",
                                                        "Reinforce: $coachReinforcementCount"
                                                    )
                                                } else {
                                                    tr(
                                                        "לא יודע: $notKnownCount",
                                                        "No: $notKnownCount"
                                                    )
                                                },
                                            style = KmiTypography.caption.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = Color(0xFFC62828),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(
                                            min = 44.dp
                                        ),
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
                                            text = tr(
                                                "לא סומן: $unmarkedCount",
                                                "Open: $unmarkedCount"
                                            ),
                                            style = KmiTypography.caption.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = summarySecondaryText,
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
                        .clip(RoundedCornerShape(18.dp))
                        .verticalScroll(scroll)
                        .padding(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    if (itemsByTopic.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(18.dp),
                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation = 0.dp
                                )
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
                                            colors = summaryCardGradient
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .padding(16.dp),
                                color = summaryPrimaryText,
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Left
                                    } else {
                                        TextAlign.Right
                                    }
                            )
                        }
                    } else {
                        val topicEntries =
                            itemsByTopic.entries.toList()

                        topicEntries.forEachIndexed { index, entry ->
                            val topicTitle = entry.key
                            val items = entry.value

                            val isFirstTopic =
                                index == 0

                            val isLastTopic =
                                index == topicEntries.lastIndex

                            val topicCardShape =
                                when {
                                    isFirstTopic && isLastTopic ->
                                        RoundedCornerShape(18.dp)

                                    isFirstTopic ->
                                        RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = 0.dp,
                                            bottomEnd = 0.dp
                                        )

                                    isLastTopic ->
                                        RoundedCornerShape(
                                            topStart = 0.dp,
                                            topEnd = 0.dp,
                                            bottomStart = 18.dp,
                                            bottomEnd = 18.dp
                                        )

                                    else ->
                                        RoundedCornerShape(0.dp)
                                }

                            val (done, total) =
                                topicStats[topicTitle] ?: (0 to 0)

                            val pct =
                                if (total > 0) {
                                    done * 100 / total
                                } else {
                                    0
                                }

                            val isTopicExpanded =
                                topicExpandedState[topicTitle] ?: false

                            val rowsBySubTopic = items
                                .groupBy { row ->
                                    row.subTopicTitle?.trim().orEmpty()
                                }
                                .toSortedMap(compareBy { key ->
                                    key.ifBlank { "000" }
                                })

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = summaryTopicRowColor
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 0.dp
                                ),
                                shape = topicCardShape
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    topicExpandedState[
                                                        topicTitle
                                                    ] =
                                                        !isTopicExpanded
                                                }
                                                .padding(
                                                    vertical = 2.dp
                                                ),
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text =
                                                if (isEnglish) {
                                                    "${topicDisplayName(topicTitle, true)} - $pct%"
                                                } else {
                                                    "$topicTitle – $pct%"
                                                },
                                            style =
                                                KmiTypography.cardTitle.copy(
                                                    fontWeight =
                                                        FontWeight.Black
                                                ),
                                            textAlign =
                                                if (isEnglish) {
                                                    TextAlign.Left
                                                } else {
                                                    TextAlign.Right
                                                },
                                            maxLines = 2,
                                            modifier = Modifier.weight(1f),
                                            color =
                                                summaryTopicTitleColor
                                        )

                                        IconButton(
                                            onClick = {
                                                topicExpandedState[topicTitle] =
                                                    !isTopicExpanded
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector =
                                                    if (isTopicExpanded) {
                                                        Icons.Filled.ExpandLess
                                                    } else {
                                                        Icons.Filled.ExpandMore
                                                    },
                                                contentDescription =
                                                    if (isTopicExpanded) {
                                                        tr(
                                                            "סגירת כרטיס הנושא",
                                                            "Collapse topic card"
                                                        )
                                                    } else {
                                                        tr(
                                                            "פתיחת כרטיס הנושא",
                                                            "Expand topic card"
                                                        )
                                                    },
                                                tint = belt.color,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }

                                    if (!isTopicExpanded) {
                                        // במצב סגור מוצגת רק כותרת הנושא.
                                    } else if (items.isEmpty()) {
                                        Text(
                                            text = tr(
                                                "אין פריטים בנושא הזה.",
                                                "No items in this topic."
                                            ),
                                            style = KmiTypography.body,
                                            textAlign =
                                                if (isEnglish) {
                                                    TextAlign.Left
                                                } else {
                                                    TextAlign.Right
                                                },
                                            modifier = Modifier.fillMaxWidth(),
                                            color =
                                                MaterialTheme.colorScheme.onSurfaceVariant
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

                                                if (isCoach) {
                                                    coachStatusMap[
                                                        topicTitle to statusId
                                                    ]
                                                        .orEmpty()
                                                        .isNotEmpty()
                                                } else {
                                                    masteredMap[topicTitle to statusId] ==
                                                            MarkState.YES
                                                }
                                            }

                                            val subTotal = rowsInSubTopic.size
                                            val subPct = if (subTotal > 0) (subDone * 100 / subTotal) else 0

                                            val hasRealSubTopic =
                                                subTopicTitleRaw
                                                    .trim()
                                                    .isNotBlank()

                                            Surface(
                                                modifier =
                                                    Modifier.fillMaxWidth(),
                                                shape =
                                                    RoundedCornerShape(16.dp),
                                                color =
                                                    if (hasRealSubTopic) {
                                                        belt.color.copy(
                                                            alpha = 0.055f
                                                        )
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                border =
                                                    if (hasRealSubTopic) {
                                                        BorderStroke(
                                                            width = 1.dp,
                                                            color =
                                                                belt.color.copy(
                                                                    alpha = 0.10f
                                                                )
                                                        )
                                                    } else {
                                                        null
                                                    },
                                                tonalElevation = 0.dp,
                                                shadowElevation = 0.dp
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            horizontal =
                                                                if (
                                                                    hasRealSubTopic
                                                                ) {
                                                                    10.dp
                                                                } else {
                                                                    0.dp
                                                                },
                                                            vertical =
                                                                if (
                                                                    hasRealSubTopic
                                                                ) {
                                                                    8.dp
                                                                } else {
                                                                    0.dp
                                                                }
                                                        ),
                                                    verticalArrangement =
                                                        Arrangement.spacedBy(
                                                            6.dp
                                                        )
                                                ) {
                                                    if (hasRealSubTopic) {
                                                        Text(
                                                            text =
                                                                if (isEnglish) {
                                                                    "${
                                                                        subTopicDisplayName(
                                                                            subTopicTitleRaw,
                                                                            true
                                                                        )
                                                                    } - $subPct%"
                                                                } else {
                                                                    "${
                                                                        subTopicDisplayName(
                                                                            subTopicTitleRaw,
                                                                            false
                                                                        )
                                                                    } – $subPct%"
                                                                },
                                                            style =
                                                                KmiTypography.cardTitle.copy(
                                                                    fontWeight =
                                                                        FontWeight.ExtraBold
                                                                ),
                                                            color =
                                                                summarySubTopicTitleColor,
                                                            textAlign =
                                                                if (isEnglish) {
                                                                    TextAlign.Left
                                                                } else {
                                                                    TextAlign.Right
                                                                },
                                                            maxLines = 2,
                                                            modifier =
                                                                Modifier.fillMaxWidth()
                                                        )
                                                    }

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

                                                        val state =
                                                            masteredMap[
                                                                topicTitle to statusId
                                                            ] ?: MarkState.NONE

                                                        val isPartiallyKnown =
                                                            partiallyKnownIds
                                                                .contains(
                                                                    topicTitle to
                                                                            statusId
                                                                )

                                                        val coachStatuses =
                                                            coachStatusMap[
                                                                topicTitle to statusId
                                                            ]
                                                                .orEmpty()
                                                                .take(2)

                                                        val coachPrimaryStatus =
                                                            coachStatuses.firstOrNull()
                                                                ?: CoachSummaryStatus.NOT_TAUGHT

                                                        val statusBackgroundColor =
                                                            if (isCoach) {
                                                                when (coachPrimaryStatus) {
                                                                    CoachSummaryStatus.NOT_TAUGHT ->
                                                                        Color(0xFF8A939D)

                                                                    CoachSummaryStatus.TAUGHT ->
                                                                        Color(0xFFF3A062)

                                                                    CoachSummaryStatus.PRACTICED ->
                                                                        Color(0xFF2F9B4E)

                                                                    CoachSummaryStatus.NEEDS_REINFORCEMENT ->
                                                                        Color(0xFF3478D4)
                                                                }
                                                            } else {
                                                                if (
                                                                    isPartiallyKnown
                                                                ) {
                                                                    Color(
                                                                        0xFFF28C28
                                                                    )
                                                                } else {
                                                                    when (state) {
                                                                        MarkState.YES ->
                                                                            Color(
                                                                                0xFF4CAF50
                                                                            )

                                                                        MarkState.NO ->
                                                                            Color(
                                                                                0xFFE53935
                                                                            )

                                                                        MarkState.NONE ->
                                                                            Color(
                                                                                0xFFE0E0E0
                                                                            )
                                                                    }
                                                                }
                                                            }

                                                        val statusForegroundColor =
                                                            if (
                                                                !isCoach &&
                                                                state == MarkState.NONE
                                                            ) {
                                                                Color(0xFF616161)
                                                            } else {
                                                                Color.White
                                                            }

                                                        val cleanFavId =
                                                            cleanItem(
                                                                row.sourceTopicTitle,
                                                                canonicalId
                                                            )

                                                        val itemHasNote =
                                                            hasNote(
                                                                row.sourceTopicTitle,
                                                                cleanFavId
                                                            )

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    color =
                                                                        if (isCoach) {
                                                                            when (coachPrimaryStatus) {
                                                                                CoachSummaryStatus.PRACTICED ->
                                                                                    Color(0xFF2F9B4E).copy(alpha = 0.07f)

                                                                                CoachSummaryStatus.TAUGHT ->
                                                                                    Color(0xFFF3A062).copy(alpha = 0.07f)

                                                                                CoachSummaryStatus.NEEDS_REINFORCEMENT ->
                                                                                    Color(0xFF3478D4).copy(alpha = 0.07f)

                                                                                CoachSummaryStatus.NOT_TAUGHT ->
                                                                                    Color.Transparent
                                                                            }
                                                                        } else {
                                                                            when {
                                                                                isPartiallyKnown ->
                                                                                    Color(
                                                                                        0xFFF28C28
                                                                                    ).copy(
                                                                                        alpha =
                                                                                            0.075f
                                                                                    )

                                                                                state ==
                                                                                        MarkState.YES ->
                                                                                    belt.color.copy(
                                                                                        alpha =
                                                                                            0.075f
                                                                                    )

                                                                                else ->
                                                                                    Color.Transparent
                                                                            }
                                                                        },
                                                                    shape = RoundedCornerShape(14.dp)
                                                                )
                                                                .padding(
                                                                    horizontal = 8.dp,
                                                                    vertical = 6.dp
                                                                ),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            /*
                                                             * הטקסט מופיע ראשון בתוך Row:
                                                             *
                                                             * בעברית, בגלל RTL, הוא מוצמד לקצה הימני
                                                             * ואייקון הסטטוס עובר לקצה השמאלי.
                                                             *
                                                             * באנגלית, בגלל LTR, הטקסט מתחיל משמאל
                                                             * ואייקון הסטטוס עובר לקצה הימני.
                                                             */
                                                            Column(
                                                                modifier = Modifier.weight(1f),
                                                                horizontalAlignment =
                                                                    if (isEnglish) {
                                                                        Alignment.Start
                                                                    } else {
                                                                        Alignment.End
                                                                    }
                                                            ) {
                                                                Text(
                                                                    text =
                                                                        exerciseDisplayNameForUi(
                                                                            row.sourceTopicTitle,
                                                                            itemRaw,
                                                                            isEnglish
                                                                        ),
                                                                    style = KmiTypography.body,
                                                                    color = summaryPrimaryText,
                                                                    textAlign =
                                                                        if (isEnglish) {
                                                                            TextAlign.Left
                                                                        } else {
                                                                            TextAlign.Right
                                                                        },
                                                                    maxLines = 3,
                                                                    overflow =
                                                                        androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )

                                                                if (itemHasNote) {
                                                                    Text(
                                                                        text = tr(
                                                                            "יש הערה שמורה",
                                                                            "Saved note exists"
                                                                        ),
                                                                        style = KmiTypography.caption,
                                                                        color =
                                                                            MaterialTheme.colorScheme.primary,
                                                                        textAlign =
                                                                            if (isEnglish) {
                                                                                TextAlign.Left
                                                                            } else {
                                                                                TextAlign.Right
                                                                            },
                                                                        maxLines = 1,
                                                                        overflow =
                                                                            androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    )
                                                                }
                                                            }

                                                            Spacer(Modifier.width(6.dp))

                                                            if (isCoach) {

                                                                /*
                                                                 * אם לא נבחר שום סטטוס,
                                                                 * ממשיכים להציג אייקון אפור "לא נלמד".
                                                                 */
                                                                if (coachStatuses.isEmpty()) {

                                                                    Column(
                                                                        modifier = Modifier.widthIn(
                                                                            min = 58.dp
                                                                        ),
                                                                        horizontalAlignment =
                                                                            Alignment.CenterHorizontally
                                                                    ) {
                                                                        Surface(
                                                                            modifier = Modifier.size(36.dp),
                                                                            shape = CircleShape,
                                                                            color = Color(0xFF8A939D),
                                                                            shadowElevation = 0.dp,
                                                                            tonalElevation = 0.dp
                                                                        ) {
                                                                            Box(
                                                                                modifier = Modifier.fillMaxSize(),
                                                                                contentAlignment = Alignment.Center
                                                                            ) {
                                                                                Text(
                                                                                    text = "—",
                                                                                    color = Color.White,
                                                                                    fontWeight = FontWeight.ExtraBold,
                                                                                    textAlign = TextAlign.Center,
                                                                                    style = KmiTypography.action
                                                                                )
                                                                            }
                                                                        }

                                                                        Spacer(Modifier.height(2.dp))

                                                                        Text(
                                                                            text =
                                                                                tr(
                                                                                    "לא נלמד",
                                                                                    "Not taught"
                                                                                ),
                                                                            style =
                                                                                KmiTypography.caption.copy(
                                                                                    fontWeight =
                                                                                        FontWeight.ExtraBold
                                                                                ),
                                                                            color = Color(0xFF8A939D),
                                                                            textAlign = TextAlign.Center,
                                                                            maxLines = 2,
                                                                            overflow =
                                                                                androidx.compose.ui.text.style
                                                                                    .TextOverflow.Ellipsis
                                                                        )
                                                                    }

                                                                } else {

                                                                    /*
                                                                     * עד שני סטטוסים מוצגים זה לצד זה.
                                                                     */
                                                                    Row(
                                                                        horizontalArrangement =
                                                                            Arrangement.spacedBy(6.dp),
                                                                        verticalAlignment =
                                                                            Alignment.Top
                                                                    ) {
                                                                        coachStatuses.forEach { status ->

                                                                            val coachColor =
                                                                                when (status) {
                                                                                    CoachSummaryStatus.TAUGHT ->
                                                                                        Color(0xFFF3A062)

                                                                                    CoachSummaryStatus.PRACTICED ->
                                                                                        Color(0xFF2F9B4E)

                                                                                    CoachSummaryStatus.NEEDS_REINFORCEMENT ->
                                                                                        Color(0xFF3478D4)

                                                                                    CoachSummaryStatus.NOT_TAUGHT ->
                                                                                        Color(0xFF8A939D)
                                                                                }

                                                                            val coachMark =
                                                                                when (status) {
                                                                                    CoachSummaryStatus.TAUGHT ->
                                                                                        "✓"

                                                                                    CoachSummaryStatus.PRACTICED ->
                                                                                        "↻"

                                                                                    CoachSummaryStatus.NEEDS_REINFORCEMENT ->
                                                                                        "!"

                                                                                    CoachSummaryStatus.NOT_TAUGHT ->
                                                                                        "—"
                                                                                }

                                                                            val coachLabel =
                                                                                when (status) {
                                                                                    CoachSummaryStatus.TAUGHT ->
                                                                                        tr(
                                                                                            "נלמד",
                                                                                            "Taught"
                                                                                        )

                                                                                    CoachSummaryStatus.PRACTICED ->
                                                                                        tr(
                                                                                            "תורגל",
                                                                                            "Practiced"
                                                                                        )

                                                                                    CoachSummaryStatus.NEEDS_REINFORCEMENT ->
                                                                                        tr(
                                                                                            "חיזוק",
                                                                                            "Reinforce"
                                                                                        )

                                                                                    CoachSummaryStatus.NOT_TAUGHT ->
                                                                                        tr(
                                                                                            "לא נלמד",
                                                                                            "Not taught"
                                                                                        )
                                                                                }

                                                                            Column(
                                                                                modifier = Modifier.width(48.dp),
                                                                                horizontalAlignment =
                                                                                    Alignment.CenterHorizontally
                                                                            ) {
                                                                                Surface(
                                                                                    modifier = Modifier.size(36.dp),
                                                                                    shape = CircleShape,
                                                                                    color = coachColor,
                                                                                    shadowElevation = 0.dp,
                                                                                    tonalElevation = 0.dp
                                                                                ) {
                                                                                    Box(
                                                                                        modifier =
                                                                                            Modifier.fillMaxSize(),
                                                                                        contentAlignment =
                                                                                            Alignment.Center
                                                                                    ) {
                                                                                        Text(
                                                                                            text = coachMark,
                                                                                            color = Color.White,
                                                                                            fontWeight =
                                                                                                FontWeight.ExtraBold,
                                                                                            textAlign =
                                                                                                TextAlign.Center,
                                                                                            style = KmiTypography.action
                                                                                        )
                                                                                    }
                                                                                }

                                                                                Spacer(
                                                                                    Modifier.height(2.dp)
                                                                                )

                                                                                Text(
                                                                                    text = coachLabel,
                                                                                    style =
                                                                                        KmiTypography.caption.copy(
                                                                                            fontWeight =
                                                                                                FontWeight.ExtraBold
                                                                                        ),
                                                                                    color = coachColor,
                                                                                    textAlign =
                                                                                        TextAlign.Center,
                                                                                    maxLines = 1,
                                                                                    overflow =
                                                                                        androidx.compose.ui.text.style
                                                                                            .TextOverflow.Ellipsis
                                                                                )
                                                                            }
                                                                        }
                                                                    }

                                                                }

                                                            } else {

                                                                /*
                                                                 * משתמש רגיל נשאר בדיוק כפי שהיה:
                                                                 * יודע / לא יודע / לא סומן.
                                                                 */
                                                                Column(
                                                                    modifier =
                                                                        Modifier.width(
                                                                            78.dp
                                                                        ),
                                                                    horizontalAlignment =
                                                                        Alignment.CenterHorizontally
                                                                ) {
                                                                    Surface(
                                                                        modifier = Modifier.size(28.dp),
                                                                        shape = CircleShape,
                                                                        color = statusBackgroundColor,
                                                                        shadowElevation = 0.dp,
                                                                        tonalElevation = 0.dp
                                                                    ) {
                                                                        Box(
                                                                            modifier = Modifier.fillMaxSize(),
                                                                            contentAlignment = Alignment.Center
                                                                        ) {
                                                                            if (state == MarkState.NONE) {
                                                                                Box(
                                                                                    modifier = Modifier
                                                                                        .size(9.dp)
                                                                                        .border(
                                                                                            width = 1.7.dp,
                                                                                            color =
                                                                                                statusForegroundColor,
                                                                                            shape = CircleShape
                                                                                        )
                                                                                )
                                                                            } else {
                                                                                Text(
                                                                                    text =
                                                                                        if (
                                                                                            isPartiallyKnown
                                                                                        ) {
                                                                                            "◐"
                                                                                        } else if (
                                                                                            state == MarkState.YES
                                                                                        ) {
                                                                                            "✓"
                                                                                        } else {
                                                                                            "✗"
                                                                                        },
                                                                                    color =
                                                                                        statusForegroundColor,
                                                                                    style =
                                                                                        KmiTypography.action,
                                                                                    fontWeight =
                                                                                        FontWeight.ExtraBold,
                                                                                    textAlign =
                                                                                        TextAlign.Center
                                                                                )
                                                                            }
                                                                        }
                                                                    }

                                                                    Spacer(Modifier.height(2.dp))

                                                                    Text(
                                                                        text =
                                                                            if (
                                                                                isPartiallyKnown
                                                                            ) {
                                                                                tr(
                                                                                    "יודע חלקית",
                                                                                    "Partially known"
                                                                                )
                                                                            } else {
                                                                                when (
                                                                                    state
                                                                                ) {
                                                                                    MarkState.YES ->
                                                                                        tr(
                                                                                            "יודע",
                                                                                            "Known"
                                                                                        )

                                                                                    MarkState.NO ->
                                                                                        tr(
                                                                                            "לא יודע",
                                                                                            "Not known"
                                                                                        )

                                                                                    MarkState.NONE ->
                                                                                        tr(
                                                                                            "לא סומן",
                                                                                            "Unmarked"
                                                                                        )
                                                                                }
                                                                            },
                                                                        style =
                                                                            KmiTypography.caption.copy(
                                                                                fontWeight =
                                                                                    FontWeight.ExtraBold
                                                                            ),
                                                                        color = statusBackgroundColor,
                                                                        textAlign = TextAlign.Center,
                                                                        maxLines = 2,
                                                                        overflow =
                                                                            androidx.compose.ui.text.style
                                                                                .TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        thickness = 1.dp,
                                        color = summaryTopicDividerColor
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

/* ------------------------------ PDF: Summary ------------------------------ */

private fun createSummaryPdf(
    dir: File,
    belt: Belt,
    itemsByTopic:
    Map<String, List<SummaryExerciseRow>>,
    masteredMap:
    Map<Pair<String, String>, MarkState>,
    partiallyKnownIds:
    Set<Pair<String, String>>,
    isEnglish: Boolean = false,
    topic: String = "",
    subTopicFilter: String? = null
): File {
    val document = PdfDocument()

    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f
    val contentRight = pageWidth - margin
    val footerY = 804f
    val bodyBottom = footerY - 12f

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val blue = android.graphics.Color.rgb(12, 78, 130)
    val sky = android.graphics.Color.rgb(42, 132, 190)
    val paleBlue = android.graphics.Color.rgb(234, 246, 255)
    val softBlue = android.graphics.Color.rgb(246, 250, 254)
    val borderBlue = android.graphics.Color.rgb(191, 213, 232)
    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(80, 100, 120)
    val green =
        android.graphics.Color.rgb(
            22,
            163,
            74
        )

    val orange =
        android.graphics.Color.rgb(
            242,
            140,
            40
        )

    val red =
        android.graphics.Color.rgb(
            220,
            38,
            38
        )

    val gray =
        android.graphics.Color.rgb(
            107,
            114,
            128
        )
    val white = android.graphics.Color.WHITE

    val regular = android.graphics.Typeface.create(
        android.graphics.Typeface.SANS_SERIF,
        android.graphics.Typeface.NORMAL
    )
    val bold = android.graphics.Typeface.create(
        android.graphics.Typeface.SANS_SERIF,
        android.graphics.Typeface.BOLD
    )

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: android.graphics.Typeface = regular,
        align: android.graphics.Paint.Align = android.graphics.Paint.Align.LEFT
    ) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        this.typeface = typeface
        textAlign = align
    }

    fun drawRoundRect(
        canvas: android.graphics.Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 12f,
        stroke: Boolean = false,
        strokeWidth: Float = 1.2f
    ) {
        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radius,
            radius,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = if (stroke) {
                    android.graphics.Paint.Style.STROKE
                } else {
                    android.graphics.Paint.Style.FILL
                }
                this.strokeWidth = strokeWidth
            }
        )
    }

    fun buildTextLayout(
        text: String,
        width: Int,
        size: Float,
        color: Int,
        typeface: android.graphics.Typeface = regular,
        maxLines: Int = 2,
        rtl: Boolean = !isEnglish,
        alignment: android.text.Layout.Alignment? = null
    ): android.text.StaticLayout {
        val textPaint = android.text.TextPaint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            textSize = size
            this.color = color
            this.typeface = typeface
        }

        return android.text.StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, width)
            .setAlignment(
                alignment ?: android.text.Layout.Alignment.ALIGN_NORMAL
            )
            .setTextDirection(
                if (rtl) {
                    android.text.TextDirectionHeuristics.RTL
                } else {
                    android.text.TextDirectionHeuristics.LTR
                }
            )
            .setMaxLines(maxLines)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.05f)
            .build()
    }

    fun drawLayout(
        canvas: android.graphics.Canvas,
        layout: android.text.StaticLayout,
        left: Float,
        top: Float
    ) {
        canvas.withTranslation(left, top) {
            layout.draw(this)
        }
    }

    fun statusIdForPdf(
        row: SummaryExerciseRow
    ): String {
        return summaryExerciseIdentityIdFor(
            belt = belt,
            topicKey = row.statusTopicKey,
            topicTitle = row.sourceTopicTitle,
            index = row.indexInStatusGroup,
            item = row.itemRaw
        )
    }

    fun stateFor(
        topicTitle: String,
        row: SummaryExerciseRow
    ): MarkState {
        val statusId =
            statusIdForPdf(row)

        return masteredMap[
            topicTitle to statusId
        ] ?: MarkState.NONE
    }

    fun isPartiallyKnownFor(
        topicTitle: String,
        row: SummaryExerciseRow
    ): Boolean {
        val statusId =
            statusIdForPdf(row)

        return partiallyKnownIds.contains(
            topicTitle to statusId
        )
    }

    val allRows =
        itemsByTopic.values.flatten()

    val knownCount =
        itemsByTopic.entries.sumOf {
                (topicTitle, rows) ->
            rows.count { row ->
                stateFor(
                    topicTitle,
                    row
                ) == MarkState.YES
            }
        }

    val partiallyKnownCount =
        itemsByTopic.entries.sumOf {
                (topicTitle, rows) ->
            rows.count { row ->
                isPartiallyKnownFor(
                    topicTitle,
                    row
                )
            }
        }

    val notKnownCount =
        itemsByTopic.entries.sumOf {
                (topicTitle, rows) ->
            rows.count { row ->
                stateFor(
                    topicTitle,
                    row
                ) == MarkState.NO &&
                        !isPartiallyKnownFor(
                            topicTitle,
                            row
                        )
            }
        }

    val unmarkedCount =
        (
                allRows.size -
                        knownCount -
                        partiallyKnownCount -
                        notKnownCount
                )
            .coerceAtLeast(0)

    val markedCount =
        knownCount +
                partiallyKnownCount +
                notKnownCount

    val markedPercent =
        if (allRows.isEmpty()) {
            0
        } else {
            markedCount *
                    100 /
                    allRows.size
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
        val clean = belt.heb.trim()
        if (clean.startsWith("חגורה")) clean else "חגורה $clean"
    }

    val scopeLabel = when {
        topic.isNotBlank() && !subTopicFilter.isNullOrBlank() -> {
            val topicName = topicDisplayName(topic, isEnglish)
            val subName =
                subTopicDisplayName(
                    subTopicFilter,
                    isEnglish
                )
            "$topicName · $subName"
        }
        topic.isNotBlank() -> topicDisplayName(topic, isEnglish)
        else -> tr("כל נושאי החגורה", "All belt topics")
    }

    data class PdfRow(
        val number: Int,
        val topicTitle: String,
        val subTopicTitle: String?,
        val title: String,
        val state: MarkState,
        val isPartiallyKnown: Boolean,
        val height: Float
    )

    data class PdfBlock(
        val topicTitle: String,
        val topicPercent: Int,
        val rows: List<PdfRow>
    )

    val textAreaWidth = (pageWidth - margin * 2f - 92f).toInt()
    var runningNumber = 1

    val blocks = itemsByTopic.map { (topicTitle, rows) ->
        val knownInTopic = rows.count { stateFor(topicTitle, it) == MarkState.YES }
        val topicPercent = if (rows.isEmpty()) 0 else knownInTopic * 100 / rows.size

        val pdfRows = rows.map { row ->
            val title = exerciseDisplayNameForUi(
                topicTitle = row.sourceTopicTitle,
                rawItem = row.itemRaw,
                isEnglish = isEnglish
            )
            val layout = buildTextLayout(
                text = title,
                width = textAreaWidth,
                size = 11.5f,
                color = textDark,
                typeface = bold,
                maxLines = 3
            )
            PdfRow(
                number = runningNumber++,
                topicTitle = topicTitle,
                subTopicTitle = row.subTopicTitle,
                title = title,
                state =
                    stateFor(
                        topicTitle,
                        row
                    ),
                isPartiallyKnown =
                    isPartiallyKnownFor(
                        topicTitle,
                        row
                    ),
                height =
                    maxOf(
                        64f,
                        layout.height + 40f
                    )
            )
        }

        PdfBlock(
            topicTitle = topicTitle,
            topicPercent = topicPercent,
            rows = pdfRows
        )
    }

    data class PageElement(
        val type: Int,
        val title: String = "",
        val percent: Int = 0,
        val row: PdfRow? = null
    )

    val topicHeaderType = 1
    val subTopicHeaderType = 2
    val exerciseType = 3

    val firstPageBodyTop = 278f
    val nextPageBodyTop = 156f
    val firstCapacity = bodyBottom - firstPageBodyTop
    val nextCapacity = bodyBottom - nextPageBodyTop

    fun elementHeight(element: PageElement): Float = when (element.type) {
        topicHeaderType -> 44f
        subTopicHeaderType -> 30f
        else -> (element.row?.height ?: 64f) + 8f
    }

    val pages = mutableListOf<MutableList<PageElement>>()
    var current = mutableListOf<PageElement>()
    var used = 0f
    var capacity = firstCapacity

    fun pushPage() {
        pages += current
        current = mutableListOf()
        used = 0f
        capacity = nextCapacity
    }

    blocks.forEach { block ->
        val header = PageElement(
            type = topicHeaderType,
            title = topicDisplayName(block.topicTitle, isEnglish),
            percent = block.topicPercent
        )

        if (used + elementHeight(header) > capacity && current.isNotEmpty()) {
            pushPage()
        }
        current += header
        used += elementHeight(header)

        var previousSubTopic: String? = null

        block.rows.forEach { row ->
            val cleanSub = row.subTopicTitle?.trim().orEmpty()
            if (cleanSub.isNotBlank() && cleanSub != previousSubTopic) {
                val subHeader = PageElement(
                    type = subTopicHeaderType,
                    title = subTopicDisplayName(
                        subTopicTitle = cleanSub,
                        isEnglish = isEnglish
                    )
                )

                if (used + elementHeight(subHeader) + elementHeight(PageElement(type = exerciseType, row = row)) > capacity &&
                    current.isNotEmpty()
                ) {
                    pushPage()
                    current += header
                    used += elementHeight(header)
                }

                current += subHeader
                used += elementHeight(subHeader)
                previousSubTopic = cleanSub
            }

            val exercise = PageElement(type = exerciseType, row = row)
            if (used + elementHeight(exercise) > capacity && current.isNotEmpty()) {
                pushPage()
                current += header
                used += elementHeight(header)

                if (cleanSub.isNotBlank()) {
                    val repeatedSubHeader = PageElement(
                        type = subTopicHeaderType,
                        title = subTopicDisplayName(
                            subTopicTitle = cleanSub,
                            isEnglish = isEnglish
                        )
                    )
                    current += repeatedSubHeader
                    used += elementHeight(repeatedSubHeader)
                }
            }

            current += exercise
            used += elementHeight(exercise)
        }
    }

    if (current.isNotEmpty() || pages.isEmpty()) {
        pages += current
    }

    fun drawKmiLogo(
        canvas: android.graphics.Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {
        canvas.drawCircle(
            cx,
            cy,
            radius,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
            }
        )
        canvas.drawCircle(
            cx,
            cy,
            radius - 4f,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = white
            }
        )
        canvas.drawText(
            "KAMI",
            cx,
            cy + radius * 0.22f,
            paint(
                size = radius * 0.62f,
                color = navy,
                typeface = bold,
                align = android.graphics.Paint.Align.CENTER
            )
        )
    }

    fun drawHeader(canvas: android.graphics.Canvas, pageNumber: Int) {
        canvas.drawColor(white)

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(pageWidth.toFloat(), 0f)
                lineTo(pageWidth.toFloat(), 122f)
                lineTo(178f, 122f)
                lineTo(238f, 0f)
                close()
            },
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
            }
        )

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(208f, 122f)
                lineTo(224f, 122f)
                lineTo(284f, 0f)
                lineTo(268f, 0f)
                close()
            },
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = sky
            }
        )

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(230f, 122f)
                lineTo(238f, 122f)
                lineTo(298f, 0f)
                lineTo(290f, 0f)
                close()
            },
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(128, 183, 220)
            }
        )

        drawKmiLogo(canvas, 78f, 58f, 42f)

        val titleLayout = buildTextLayout(
            text = tr("דו״ח סיכום חגורה", "Belt summary report"),
            width = 330,
            size = 26f,
            color = white,
            typeface = bold,
            maxLines = 1
        )
        drawLayout(canvas, titleLayout, pageWidth - 34f - 330f, 28f)

        val subtitleLayout = buildTextLayout(
            text = "$beltLabel · $scopeLabel",
            width = 330,
            size = 12.5f,
            color = white,
            maxLines = 2
        )
        drawLayout(canvas, subtitleLayout, pageWidth - 34f - 330f, 67f)

        val generated = java.text.SimpleDateFormat(
            "dd/MM/yyyy",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val infoLayout = buildTextLayout(
            text = "${tr("תאריך הפקה", "Generated")}: $generated",
            width = 260,
            size = 9f,
            color = textMuted,
            maxLines = 1
        )
        drawLayout(canvas, infoLayout, pageWidth - margin - 260f, 132f)

        if (pageNumber > 1) {
            val continuation = buildTextLayout(
                text = tr("המשך רשימת התרגילים", "Exercises list continued"),
                width = 250,
                size = 14f,
                color = blue,
                typeface = bold,
                maxLines = 1,
                alignment = android.text.Layout.Alignment.ALIGN_CENTER
            )
            drawLayout(canvas, continuation, (pageWidth - 250f) / 2f, 132f)
        }
    }

    fun drawFooter(
        canvas: android.graphics.Canvas,
        pageNumber: Int,
        totalPages: Int
    ) {
        canvas.drawLine(
            0f,
            footerY,
            pageWidth.toFloat(),
            footerY,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                strokeWidth = 2f
            }
        )

        drawKmiLogo(canvas, 38f, footerY + 22f, 13f)

        canvas.drawText(
            "Together We Protect",
            62f,
            footerY + 25f,
            paint(
                size = 9f,
                color = textMuted,
                align = android.graphics.Paint.Align.LEFT
            )
        )

        canvas.drawText(
            tr(
                "עמוד $pageNumber מתוך $totalPages",
                "Page $pageNumber of $totalPages"
            ),
            pageWidth / 2f,
            footerY + 25f,
            paint(
                size = 9f,
                color = textMuted,
                align = android.graphics.Paint.Align.CENTER
            )
        )

        canvas.drawText(
            "Krav Maga Israel",
            pageWidth - 66f,
            footerY + 18f,
            paint(
                size = 9f,
                color = textMuted,
                align = android.graphics.Paint.Align.RIGHT
            )
        )
        canvas.drawText(
            "www.kmi.org.il",
            pageWidth - 66f,
            footerY + 31f,
            paint(
                size = 8f,
                color = textMuted,
                align = android.graphics.Paint.Align.RIGHT
            )
        )
    }

    fun drawSummaryCard(canvas: android.graphics.Canvas, top: Float) {
        drawRoundRect(
            canvas,
            margin,
            top,
            contentRight,
            top + 118f,
            paleBlue,
            14f
        )
        drawRoundRect(
            canvas,
            margin,
            top,
            contentRight,
            top + 118f,
            borderBlue,
            14f,
            stroke = true
        )

        val summaryTitle = buildTextLayout(
            text = tr("סיכום תרגילים", "Exercises summary"),
            width = (pageWidth - margin * 2f - 40f).toInt(),
            size = 17f,
            color = blue,
            typeface = bold,
            maxLines = 1
        )
        drawLayout(canvas, summaryTitle, margin + 20f, top + 16f)

        val stats = listOf(
            knownCount.toString() to
                    tr("יודע", "Known"),

            partiallyKnownCount.toString() to
                    tr("חלקית", "Partial"),

            notKnownCount.toString() to
                    tr("לא יודע", "Not known"),

            unmarkedCount.toString() to
                    tr("לא סומן", "Unmarked"),

            "$markedPercent%" to
                    tr("סומנו", "Marked")
        )

        val gap = 6f
        val innerLeft = margin + 16f

        val cardWidth =
            (
                    pageWidth -
                            margin * 2f -
                            32f -
                            gap * 4f
                    ) / 5f
        val cardTop = top + 48f

        stats.forEachIndexed { index, (value, label) ->
            val left = innerLeft + index * (cardWidth + gap)
            val right = left + cardWidth

            drawRoundRect(
                canvas,
                left,
                cardTop,
                right,
                cardTop + 54f,
                white,
                10f
            )
            drawRoundRect(
                canvas,
                left,
                cardTop,
                right,
                cardTop + 54f,
                borderBlue,
                10f,
                stroke = true
            )

            val valueColor =
                when (index) {
                    0 -> green
                    1 -> orange
                    2 -> red
                    3 -> gray
                    else -> blue
                }

            canvas.drawText(
                value,
                (left + right) / 2f,
                cardTop + 23f,
                paint(
                    size = 15f,
                    color = valueColor,
                    typeface = bold,
                    align = android.graphics.Paint.Align.CENTER
                )
            )
            canvas.drawText(
                label,
                (left + right) / 2f,
                cardTop + 42f,
                paint(
                    size = 8.5f,
                    color = textMuted,
                    align = android.graphics.Paint.Align.CENTER
                )
            )
        }
    }

    fun drawTopicHeader(
        canvas: android.graphics.Canvas,
        top: Float,
        title: String,
        percent: Int
    ): Float {
        drawRoundRect(
            canvas,
            margin,
            top,
            contentRight,
            top + 36f,
            navy,
            10f
        )

        val layout = buildTextLayout(
            text = "$title · $percent%",
            width = (pageWidth - margin * 2f - 28f).toInt(),
            size = 13f,
            color = white,
            typeface = bold,
            maxLines = 1
        )
        drawLayout(canvas, layout, margin + 14f, top + 9f)

        return top + 44f
    }

    fun drawSubTopicHeader(
        canvas: android.graphics.Canvas,
        top: Float,
        title: String
    ): Float {
        drawRoundRect(
            canvas,
            margin + 10f,
            top,
            contentRight - 10f,
            top + 24f,
            android.graphics.Color.rgb(225, 240, 251),
            8f
        )

        val layout = buildTextLayout(
            text = title,
            width = (pageWidth - margin * 2f - 48f).toInt(),
            size = 10.5f,
            color = blue,
            typeface = bold,
            maxLines = 1
        )
        drawLayout(canvas, layout, margin + 24f, top + 6f)

        return top + 30f
    }

    fun drawExerciseCard(
        canvas: android.graphics.Canvas,
        top: Float,
        row: PdfRow
    ): Float {
        val bottom = top + row.height
        val statusColor =
            if (row.isPartiallyKnown) {
                orange
            } else {
                when (row.state) {
                    MarkState.YES ->
                        green

                    MarkState.NO ->
                        red

                    MarkState.NONE ->
                        gray
                }
            }

        val statusLabel =
            if (row.isPartiallyKnown) {
                tr(
                    "יודע חלקית",
                    "Partially known"
                )
            } else {
                when (row.state) {
                    MarkState.YES ->
                        tr(
                            "יודע",
                            "Known"
                        )

                    MarkState.NO ->
                        tr(
                            "לא יודע",
                            "Not known"
                        )

                    MarkState.NONE ->
                        tr(
                            "לא סומן",
                            "Unmarked"
                        )
                }
            }

        drawRoundRect(
            canvas,
            margin,
            top,
            contentRight,
            bottom,
            softBlue,
            12f
        )
        drawRoundRect(
            canvas,
            margin,
            top,
            contentRight,
            bottom,
            borderBlue,
            12f,
            stroke = true
        )

        val circleCenterX = if (isEnglish) margin + 23f else contentRight - 23f
        canvas.drawCircle(
            circleCenterX,
            top + 27f,
            14f,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = blue
            }
        )
        canvas.drawText(
            row.number.toString(),
            circleCenterX,
            top + 31f,
            paint(
                size = 10f,
                color = white,
                typeface = bold,
                align = android.graphics.Paint.Align.CENTER
            )
        )

        val titleLeft = if (isEnglish) margin + 48f else margin + 22f
        val titleLayout = buildTextLayout(
            text = row.title,
            width = textAreaWidth,
            size = 11.5f,
            color = textDark,
            typeface = bold,
            maxLines = 3
        )
        drawLayout(canvas, titleLayout, titleLeft, top + 11f)

        val statusPaint = paint(
            size = 9f,
            color = statusColor,
            typeface = bold,
            align = if (isEnglish) {
                android.graphics.Paint.Align.RIGHT
            } else {
                android.graphics.Paint.Align.LEFT
            }
        )

        canvas.drawText(
            statusLabel,
            if (isEnglish) contentRight - 16f else margin + 16f,
            bottom - 12f,
            statusPaint
        )

        canvas.drawCircle(
            if (isEnglish) contentRight - 20f else margin + 20f,
            top + 20f,
            4f,
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = statusColor
            }
        )

        return bottom + 8f
    }

    pages.forEachIndexed { pageIndex, elements ->
        val pageNumber = pageIndex + 1
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()
        )
        val canvas = page.canvas

        drawHeader(canvas, pageNumber)

        var y = if (pageNumber == 1) {
            drawSummaryCard(canvas, 148f)
            firstPageBodyTop
        } else {
            nextPageBodyTop
        }

        if (elements.isEmpty()) {
            drawRoundRect(
                canvas,
                margin,
                y,
                contentRight,
                y + 92f,
                softBlue,
                12f
            )
            drawRoundRect(
                canvas,
                margin,
                y,
                contentRight,
                y + 92f,
                borderBlue,
                12f,
                stroke = true
            )

            val emptyLayout = buildTextLayout(
                text = tr(
                    "אין תרגילים להצגה",
                    "No exercises to display"
                ),
                width = (pageWidth - margin * 2f - 40f).toInt(),
                size = 16f,
                color = blue,
                typeface = bold,
                maxLines = 2,
                alignment = android.text.Layout.Alignment.ALIGN_CENTER
            )
            drawLayout(canvas, emptyLayout, margin + 20f, y + 30f)
        } else {
            elements.forEach { element ->
                y = when (element.type) {
                    topicHeaderType -> drawTopicHeader(
                        canvas = canvas,
                        top = y,
                        title = element.title,
                        percent = element.percent
                    )

                    subTopicHeaderType -> drawSubTopicHeader(
                        canvas = canvas,
                        top = y,
                        title = element.title
                    )

                    else -> drawExerciseCard(
                        canvas = canvas,
                        top = y,
                        row = requireNotNull(element.row)
                    )
                }
            }
        }

        drawFooter(
            canvas = canvas,
            pageNumber = pageNumber,
            totalPages = pages.size
        )
        document.finishPage(page)
    }

    val fileName =
        if (isEnglish) {
            "Belt Summary Report.pdf"
        } else {
            "דוח סיכום חגורה.pdf"
        }

    val file = File(
        dir,
        fileName
    )

    try {
        FileOutputStream(file).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }

    return file
}