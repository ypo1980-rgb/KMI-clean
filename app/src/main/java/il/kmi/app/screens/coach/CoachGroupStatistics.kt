@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.coach

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.screens.coach.statistics.NationalStatisticsScreen
import il.kmi.app.ui.KmiTypography
import java.io.File
import java.io.FileOutputStream

@Composable
internal fun CoachGroupStatsPremiumScreen(
    stats: GroupStatsUi,
    profiles: List<TraineeProfile>,
    isEnglish: Boolean,
    onClose: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenHome: () -> Unit,
    shareTrigger: Int = 0,
    onShareGroupStatistics: () -> Unit = {}
) {
    var showNationalStatistics by rememberSaveable {
        mutableStateOf(false)
    }

    var nationalShareTrigger by rememberSaveable {
        mutableIntStateOf(0)
    }

    /*
     * השיתוף מופעל רק כאשר מתקבל shareTrigger חדש.
     *
     * שינוי בין הטאבים אינו מפעיל שיתוף.
     *
     * קבוצתי -> PDF סטטיסטיקת הקבוצה.
     * ארצי   -> מעבירים Trigger חדש למסך הארצי.
     */
    LaunchedEffect(
        shareTrigger
    ) {
        if (shareTrigger > 0) {
            if (showNationalStatistics) {
                nationalShareTrigger++
            } else {
                onShareGroupStatistics()
            }
        }
    }

    /*
     * כאשר הטאב הארצי פתוח, חזור מחזיר תחילה
     * לטאב הקבוצה ולא יוצא מהמסך כולו.
     */
    androidx.activity.compose.BackHandler(
        enabled = showNationalStatistics
    ) {
        nationalShareTrigger = 0
        showNationalStatistics = false
    }

    if (showNationalStatistics) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.34f
                            )
                        )
                    )
                )
                // גם התצוגה הארצית נמצאת מתחת
                // לאותו KmiTopBar חיצוני.
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 72.dp,
                    bottom = 12.dp
                )
        ) {
            StatisticsTabsSelector(
                isEnglish = isEnglish,
                nationalSelected = true,
                onGroupClick = {
                    nationalShareTrigger = 0
                    showNationalStatistics = false
                },
                onNationalClick = {}
            )

            Spacer(
                Modifier.height(10.dp)
            )

            NationalStatisticsScreen(
                isEnglish = isEnglish,
                embedded = true,
                modifier = Modifier.weight(1f),
                onBack = {
                    nationalShareTrigger = 0
                    showNationalStatistics = false
                },
                onOpenDrawer = onOpenDrawer,
                onOpenHome = onOpenHome,
                shareTrigger = nationalShareTrigger
            )
        }

        return
    }

    val statsTextAlign =
        coachTextAlign(isEnglish)

    val statsHorizontalAlignment =
        coachHorizontalAlignment(isEnglish)

    val isDarkMode =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    val statsCardColor =
        if (isDarkMode) {
            Color(0xFF111827)
        } else {
            MaterialTheme.colorScheme.surface
        }

    val statsCardVariantColor =
        if (isDarkMode) {
            Color(0xFF172036)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

    val statsCardBorderColor =
        if (isDarkMode) {
            Color(0xFF475569).copy(alpha = 0.58f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    val statsBgBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary.copy(
                alpha = 0.34f
            )
        )
    )

    val minAge = profiles.map { it.age }.filter { it > 0 }.minOrNull()?.toString() ?: "—"
    val maxAge = profiles.map { it.age }.filter { it > 0 }.maxOrNull()?.toString() ?: "—"
    val minSeniority = profiles.mapNotNull { parseYearsFromSeniority(it.seniority) }.minOrNull()?.toString() ?: "—"
    val maxSeniority = profiles.mapNotNull { parseYearsFromSeniority(it.seniority) }.maxOrNull()?.toString() ?: "—"

    val avgAttendanceValue = profiles
        .map { it.attendancePct }
        .filter { it > 0 }
        .average()
        .takeIf { !it.isNaN() }
        ?.toInt()
        ?: 0

    var startAvgAttendanceAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAvgAttendanceAnimation = true
    }

    val animatedAvgAttendance by animateFloatAsState(
        targetValue = if (startAvgAttendanceAnimation) avgAttendanceValue.toFloat() else 0f,
        animationSpec = tween(
            durationMillis = 1400,
            easing = FastOutSlowInEasing
        ),
        label = "avgAttendanceAnim"
    )

    val sparklineData = remember(profiles) {
        profiles
            .map { it.attendancePct.coerceIn(0, 100) }
            .filter { it > 0 }
            .sorted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(statsBgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // המסך מוצג מתחת ל־KmiTopBar חיצוני.
                // משאירים מקום לכותרת כדי שהטאבים
                // לא יצוירו מאחוריה.
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 72.dp,
                    bottom = 12.dp
                )
        ) {
            StatisticsTabsSelector(
                isEnglish = isEnglish,
                nationalSelected = false,
                onGroupClick = {},
                onNationalClick = {
                    showNationalStatistics = true
                }
            )

            Spacer(
                Modifier.height(10.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Surface(
                        color = statsCardColor.copy(
                            alpha = 0.98f
                        ),
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 2.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = statsCardBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            statsCardColor,
                                            statsCardVariantColor.copy(
                                                alpha = if (isDarkMode) 0.96f else 0.52f
                                            ),
                                            statsCardColor
                                        )
                                    )
                                )
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 14.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(10.dp),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = statsCardColor,
                                    shape = RoundedCornerShape(20.dp),
                                    shadowElevation = 0.dp,
                                    tonalElevation = 0.dp,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = Color(0xFF4F46E5).copy(
                                            alpha = 0.24f
                                        )
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 132.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        statsCardColor,
                                                        statsCardVariantColor.copy(
                                                            alpha =
                                                                if (isDarkMode) {
                                                                    0.94f
                                                                } else {
                                                                    0.50f
                                                                }
                                                        ),
                                                        Color(0xFF4F46E5).copy(
                                                            alpha =
                                                                if (isDarkMode) {
                                                                    0.22f
                                                                } else {
                                                                    0.10f
                                                                }
                                                        )
                                                    )
                                                )
                                            )
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalAlignment = statsHorizontalAlignment,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stats.highAttendanceCount.toString(),
                                            textAlign = statsTextAlign,
                                            maxLines = 1,
                                            style = KmiTypography.metric.copy(
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )

                                        Text(
                                            text = "🏆",
                                            style = KmiTypography.action,
                                            textAlign = TextAlign.Left
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = coachTr(
                                            isEnglish,
                                            "מעל 80%",
                                            "Above 80%"
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = statsTextAlign,
                                        maxLines = 1,
                                        style = KmiTypography.action.copy(
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = coachTr(
                                            isEnglish,
                                            "נוכחות",
                                            "Attendance"
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = statsTextAlign,
                                        maxLines = 1,
                                        style = KmiTypography.secondary.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                                Surface(
                                    color = statsCardColor,
                                    shape = RoundedCornerShape(20.dp),
                                    shadowElevation = 0.dp,
                                    tonalElevation = 0.dp,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = Color(0xFF22C55E).copy(
                                            alpha = 0.24f
                                        )
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 132.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        statsCardColor,
                                                        statsCardVariantColor.copy(
                                                            alpha =
                                                                if (isDarkMode) {
                                                                    0.94f
                                                                } else {
                                                                    0.50f
                                                                }
                                                        ),
                                                        Color(0xFF22C55E).copy(
                                                            alpha =
                                                                if (isDarkMode) {
                                                                    0.18f
                                                                } else {
                                                                    0.10f
                                                                }
                                                        )
                                                    )
                                                )
                                            )
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    AttendanceRing(
                                        percent = animatedAvgAttendance.toInt(),
                                        modifier = Modifier.size(82.dp)
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = coachTr(
                                            isEnglish,
                                            "נוכחות ממוצעת",
                                            "Average attendance"
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        style = KmiTypography.caption.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            }

                        }

                        AttendanceSparkline(
                            values = sparklineData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PremiumStatChip(
                        label = coachTr(isEnglish, "גיל מינ׳", "Min age"),
                        value = minAge,
                        icon = "📅",
                        gradient = listOf(
                            Color(0xFF1D4ED8),
                            Color(0xFF3B82F6),
                            Color(0xFF93C5FD)
                        ),
                        isEnglish = isEnglish,
                        modifier = Modifier.weight(1f)
                    )

                    PremiumStatChip(
                        label = coachTr(isEnglish, "גיל מקס׳", "Max age"),
                        value = maxAge,
                        icon = "🎂",
                        gradient = listOf(
                            Color(0xFF0F766E),
                            Color(0xFF14B8A6),
                            Color(0xFF5EEAD4)
                        ),
                        isEnglish = isEnglish,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PremiumStatChip(
                        label = coachTr(isEnglish, "ותק מינ׳", "Min seniority"),
                        value = minSeniority,
                        icon = "⏱",
                        gradient = listOf(
                            Color(0xFF7C3AED),
                            Color(0xFFA855F7),
                            Color(0xFFD8B4FE)
                        ),
                        isEnglish = isEnglish,
                        modifier = Modifier.weight(1f)
                    )

                    PremiumStatChip(
                        label = coachTr(isEnglish, "ותק מקס׳", "Max seniority"),
                        value = maxSeniority,
                        icon = "🔥",
                        gradient = listOf(
                            Color(0xFFBE185D),
                            Color(0xFFEC4899),
                            Color(0xFFF9A8D4)
                        ),
                        isEnglish = isEnglish,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PremiumMiniPill(
                        title = coachTr(isEnglish, "מתאמנים", "Trainees"),
                        value = stats.totalTrainees.toString(),
                        icon = "👥",
                        accent = Color(0xFF4F46E5),
                        isEnglish = isEnglish,
                        modifier = Modifier.weight(1f)
                    )

                    PremiumMiniPill(
                        title = coachTr(
                            isEnglish,
                            "חגורות שונות",
                            "Belt types"
                        ),
                        value =
                            stats.beltCounts.size.toString(),
                        icon = "🥋",
                        accent = Color(0xFF0891B2),
                        isEnglish = isEnglish,
                        modifier = Modifier.weight(1f)
                    )
                }

                /*
                 * גם התפלגות החגורות היא חלק
                 * מאותו תוכן גלול.
                 */
                Surface(
                    color = statsCardColor.copy(
                        alpha = 0.98f
                    ),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 2.dp,
                    tonalElevation = 0.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = statsCardBorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        statsCardColor,
                                        statsCardVariantColor.copy(
                                            alpha = if (isDarkMode) 0.96f else 0.52f
                                        ),
                                        statsCardColor
                                    )
                                )
                            )
                            .padding(
                                horizontal = 14.dp,
                                vertical = 14.dp
                            ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = coachTr(
                            isEnglish,
                            "התפלגות חגורות בסניף",
                            "Belt distribution"
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = statsTextAlign,
                        maxLines = 2,
                        style = KmiTypography.sectionTitle.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (stats.beltCounts.isEmpty()) {
                        Text(
                            text = coachTr(
                                isEnglish,
                                "אין נתונים להצגה",
                                "No data to display"
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = statsTextAlign,
                            style = KmiTypography.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        stats.beltCounts.forEach { (belt, count) ->
                            val progress =
                                if (profiles.isNotEmpty()) {
                                    count.toFloat() / profiles.size.toFloat()
                                } else {
                                    0f
                                }

                            val beltColor =
                                beltColorForStats(belt)

                            val isBlackBelt =
                                belt.trim() == "שחורה" ||
                                        belt.trim().equals(
                                            "Black",
                                            ignoreCase = true
                                        )

                            /*
                             * חגורה שחורה נבלעת ברקע הכהה.
                             * משאירים את צבע החגורה עצמו שחור,
                             * אבל מוסיפים לה מסגרת/הילה לבנה עדינה.
                             */
                            val labelColor =
                                when {
                                    isBlackBelt ->
                                        if (isDarkMode) {
                                            Color(0xFFF8FAFC)
                                        } else {
                                            Color(0xFF111827)
                                        }

                                    beltColor == Color(0xFFE5E7EB) ->
                                        Color(0xFF475569)

                                    else ->
                                        beltColor
                                }

                            val beltTrackColor =
                                if (
                                    isBlackBelt &&
                                    isDarkMode
                                ) {
                                    Color.White.copy(
                                        alpha = 0.18f
                                    )
                                } else {
                                    beltColor.copy(
                                        alpha = 0.18f
                                    )
                                }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement =
                                    Arrangement.spacedBy(7.dp)
                            ) {
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color =
                                            if (
                                                isBlackBelt &&
                                                isDarkMode
                                            ) {
                                                Color.White.copy(
                                                    alpha = 0.08f
                                                )
                                            } else {
                                                beltColor.copy(
                                                    alpha = 0.14f
                                                )
                                            },
                                        shape = CircleShape,
                                        border =
                                            if (
                                                isBlackBelt &&
                                                isDarkMode
                                            ) {
                                                BorderStroke(
                                                    width = 1.dp,
                                                    color = Color.White.copy(
                                                        alpha = 0.45f
                                                    )
                                                )
                                            } else {
                                                null
                                            },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier.fillMaxSize(),
                                            contentAlignment =
                                                Alignment.Center
                                        ) {
                                            Text(
                                                text = "$count",
                                                style =
                                                    KmiTypography.metric.copy(
                                                        fontWeight =
                                                            FontWeight.Black
                                                    ),
                                                color = labelColor,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(
                                        modifier =
                                            Modifier.width(10.dp)
                                    )

                                    Text(
                                        text =
                                            coachBeltNameForUi(
                                                belt,
                                                isEnglish
                                            ),
                                        modifier =
                                            Modifier.weight(1f),
                                        textAlign =
                                            statsTextAlign,
                                        maxLines = 2,
                                        style =
                                            KmiTypography.cardTitle.copy(
                                                fontWeight =
                                                    FontWeight.ExtraBold,
                                                color =
                                                    labelColor
                                            )
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.width(8.dp)
                                    )

                                    Surface(
                                        modifier =
                                            Modifier.size(11.dp),
                                        shape = CircleShape,
                                        color = beltColor,
                                        border =
                                            if (
                                                isBlackBelt &&
                                                isDarkMode
                                            ) {
                                                BorderStroke(
                                                    width = 1.dp,
                                                    color = Color.White.copy(
                                                        alpha = 0.65f
                                                    )
                                                )
                                            } else {
                                                null
                                            }
                                    ) {}
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                999.dp
                                            )
                                        )
                                        .background(
                                            beltTrackColor
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(
                                                progress.coerceIn(
                                                    0f,
                                                    1f
                                                )
                                            )
                                            .clip(
                                                RoundedCornerShape(
                                                    999.dp
                                                )
                                            )
                                            .background(
                                                if (
                                                    isBlackBelt &&
                                                    isDarkMode
                                                ) {
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFF111111),
                                                            Color(0xFF262626),
                                                            Color(0xFF111111)
                                                        )
                                                    )
                                                } else {
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            beltColor,
                                                            beltColor
                                                        )
                                                    )
                                                }
                                            )
                                    )

                                    if (
                                        isBlackBelt &&
                                        isDarkMode
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .border(
                                                    width = 1.dp,
                                                    color =
                                                        Color.White.copy(
                                                            alpha = 0.42f
                                                        ),
                                                    shape =
                                                        RoundedCornerShape(
                                                            999.dp
                                                        )
                                                )
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

@Composable
private fun StatisticsTabsSelector(
    isEnglish: Boolean,
    nationalSelected: Boolean,
    onGroupClick: () -> Unit,
    onNationalClick: () -> Unit
) {
    val isDarkMode =
        MaterialTheme.colorScheme.surface
            .luminance() < 0.5f

    val containerColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surface
        } else {
            Color(0xFFF8FAFF)
        }

    val borderColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.outline.copy(
                alpha = 0.45f
            )
        } else {
            Color(0xFFC7D7F2)
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                // שומרים רווח רק בצדדים.
                // כך קו הטאב הפעיל יישב על קו המסגרת התחתון.
                .padding(horizontal = 3.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            StatisticsTabButton(
                title = coachTr(
                    isEnglish,
                    "סטטיסטיקת\nהקבוצה",
                    "Group\nstatistics"
                ),
                selected = !nationalSelected,
                onClick = onGroupClick,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(
                        color = borderColor.copy(
                            alpha = 0.72f
                        )
                    )
            )

            StatisticsTabButton(
                title = coachTr(
                    isEnglish,
                    "סטטיסטיקה\nארצית",
                    "National\nstatistics"
                ),
                selected = nationalSelected,
                onClick = onNationalClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatisticsTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionProgress by animateFloatAsState(
        targetValue =
            if (selected) {
                1f
            } else {
                0f
            },
        animationSpec = tween(
            durationMillis = 240,
            easing = FastOutSlowInEasing
        ),
        label = "statistics_tab_selection"
    )

    val isDarkMode =
        MaterialTheme.colorScheme.surface
            .luminance() < 0.5f

    val selectedTextColor =
        if (isDarkMode) {
            Color(0xFFA5B4FC)
        } else {
            Color(0xFF4338CA)
        }

    val unselectedTextColor =
        MaterialTheme.colorScheme
            .onSurfaceVariant

    val selectedBackground =
        Brush.horizontalGradient(
            colors =
                if (isDarkMode) {
                    listOf(
                        Color(0xFF4338CA).copy(
                            alpha = 0.25f
                        ),
                        Color(0xFF0284C7).copy(
                            alpha = 0.18f
                        )
                    )
                } else {
                    listOf(
                        Color(0xFFEDE9FE),
                        Color(0xFFE0F2FE)
                    )
                }
        )

    val transparentBackground =
        Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent
            )
        )

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush =
                        if (selected) {
                            selectedBackground
                        } else {
                            transparentBackground
                        },
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style =
                    KmiTypography.secondary.copy(
                        fontWeight =
                            if (selected) {
                                FontWeight.ExtraBold
                            } else {
                                FontWeight.SemiBold
                            }
                    ),
                color =
                    if (selected) {
                        selectedTextColor
                    } else {
                        unselectedTextColor
                    },
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // קו הבחירה יושב על המסגרת התחתונה של הטאבים
            // ורחב משמעותית מהקו הקודם.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(
                        0.62f * selectionProgress
                    )
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF7C3AED),
                                Color(0xFF4F46E5),
                                Color(0xFF0284C7)
                            )
                        ),
                        shape = RoundedCornerShape(
                            topStart = 999.dp,
                            topEnd = 999.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
            )
        }
    }
}

@Composable
private fun NationalStatisticsEntryCard(
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4338CA),
                            Color(0xFF7C3AED),
                            Color(0xFF0284C7)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.17f),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.22f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📊",
                        style = KmiTypography.action
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment =
                    coachHorizontalAlignment(isEnglish)
            ) {
                Text(
                    text = coachTr(
                        isEnglish,
                        "סטטיסטיקה ארצית",
                        "National statistics"
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    style = KmiTypography.cardTitle.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White,
                    maxLines = 2
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = coachTr(
                        isEnglish,
                        "כל הסניפים, הגילאים, המינים והחגורות",
                        "All branches, ages, genders and belts"
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 3
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = if (isEnglish) "›" else "‹",
                style = KmiTypography.metric.copy(
                    fontWeight = FontWeight.Light
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun PremiumStatChip(
    label: String,
    value: String,
    icon: String,
    gradient: List<Color>,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val valueNumber = value.filter { it.isDigit() }.toFloatOrNull() ?: 0f

    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val animatedValue by animateFloatAsState(
        targetValue = if (startAnimation) valueNumber else 0f,
        animationSpec = tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "premiumChipValue"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val chipScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "statChipPressScale"
    )

    val accent = gradient.getOrNull(1) ?: Color(0xFF4F46E5)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = 0.24f)
        ),
        modifier = modifier
            .heightIn(min = 96.dp)
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.48f
                            ),
                            accent.copy(alpha = 0.10f)
                        )
                    )
                )
                .padding(start = 6.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.13f),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.size(34.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        style = KmiTypography.action,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 2,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text =
                        if (valueNumber > 0f) {
                            animatedValue.toInt().toString()
                        } else {
                            value
                        },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = KmiTypography.metric.copy(
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                )
            }
        }
    }
}

@Composable
private fun AttendanceSparkline(
    values: List<Int>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    val lineAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "sparklineReveal"
    )

    Canvas(modifier = modifier) {
        val maxValue = 100f
        val minValue = 0f
        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width

        val points = values.mapIndexed { index, value ->
            val normalized = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
            Offset(
                x = index * stepX * lineAnim,
                y = size.height - (normalized * size.height)
            )
        }

        for (i in 0..3) {
            val y = size.height * (i / 3f)
            drawLine(
                color = Color(0xFFCBD5E1).copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.2f
            )
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6366F1).copy(alpha = 0.24f),
                    Color(0xFF6366F1).copy(alpha = 0.03f)
                )
            )
        )

        val linePath = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }

        drawPath(
            path = linePath,
            color = Color(0xFF4F46E5),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        points.forEachIndexed { index, point ->
            if (index == points.lastIndex) {
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = point
                )
                drawCircle(
                    color = Color(0xFF4F46E5),
                    radius = 5f,
                    center = point
                )
            }
        }
    }
}

@Composable
private fun PremiumMiniPill(
    title: String,
    value: String,
    icon: String,
    accent: Color,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val valueNumber = value.filter { it.isDigit() }.toFloatOrNull() ?: 0f

    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val animatedValue by animateFloatAsState(
        targetValue = if (startAnimation) valueNumber else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "miniPillValue"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "pillScale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = 0.24f)
        ),
        modifier = modifier
            .heightIn(min = 78.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {}
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.48f
                            ),
                            accent.copy(alpha = 0.09f)
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        style = KmiTypography.action,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 2,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Text(
                    text =
                        if (valueNumber > 0f) {
                            animatedValue.toInt().toString()
                        } else {
                            value
                        },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = KmiTypography.metric.copy(
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                )
            }
        }
    }
}

@Composable
private fun AttendanceRing(
    percent: Int,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "attendanceRing"
    )

    val ringTrackColor =
        MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            drawCircle(
                color = ringTrackColor,
                style = Stroke(width = 12f)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF22C55E),
                        Color(0xFF4ADE80),
                        Color(0xFF16A34A)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(
                    width = 12f,
                    cap = StrokeCap.Round
                )
            )
        }

        Text(
            text = "$percent%",
            style = KmiTypography.metric.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

internal fun createCoachGroupStatsPdf(
    context: Context,
    stats: GroupStatsUi,
    profiles: List<TraineeProfile>,
    branch: String,
    groupKey: String,
    isEnglish: Boolean
): File {

    val pageWidth = 595
    val pageHeight = 842
    val margin = 36f

    fun tr(
        he: String,
        en: String
    ): String =
        if (isEnglish) {
            en
        } else {
            he
        }

    val document = PdfDocument()

    val page =
        document.startPage(
            PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                1
            ).create()
        )

    val canvas = page.canvas

    val navy =
        android.graphics.Color.rgb(
            2,
            43,
            74
        )

    val mediumBlue =
        android.graphics.Color.rgb(
            36,
            103,
            158
        )

    val lightBlue =
        android.graphics.Color.rgb(
            128,
            183,
            220
        )

    val textDark =
        android.graphics.Color.rgb(
            15,
            23,
            42
        )

    val muted =
        android.graphics.Color.rgb(
            100,
            116,
            139
        )

    val cardBg =
        android.graphics.Color.rgb(
            239,
            247,
            255
        )

    val border =
        android.graphics.Color.rgb(
            191,
            219,
            254
        )

    val regularTypeface =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.NORMAL
        )

    val boldTypeface =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.BOLD
        )

    fun textPaint(
        size: Float,
        color: Int = textDark,
        bold: Boolean = false,
        align: Paint.Align =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }
    ): Paint {
        return Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            textSize = size
            this.color = color
            typeface =
                if (bold) {
                    boldTypeface
                } else {
                    regularTypeface
                }
            textAlign = align
        }
    }

    /*
     * Header – זהה לשפה הגרפית
     * של קבצי ה-PDF האחרים באפליקציה.
     */
    canvas.drawColor(
        android.graphics.Color.WHITE
    )

    val headerBottom = 122f

    val navyPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            style = Paint.Style.FILL
        }

    val mediumPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = mediumBlue
            style = Paint.Style.FILL
        }

    val lightPaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = lightBlue
            style = Paint.Style.FILL
        }

    canvas.drawPath(
        android.graphics.Path().apply {
            moveTo(
                pageWidth.toFloat(),
                0f
            )
            lineTo(
                pageWidth.toFloat(),
                headerBottom
            )
            lineTo(
                178f,
                headerBottom
            )
            lineTo(
                238f,
                0f
            )
            close()
        },
        navyPaint
    )

    canvas.drawPath(
        android.graphics.Path().apply {
            moveTo(208f, headerBottom)
            lineTo(224f, headerBottom)
            lineTo(284f, 0f)
            lineTo(268f, 0f)
            close()
        },
        mediumPaint
    )

    canvas.drawPath(
        android.graphics.Path().apply {
            moveTo(230f, headerBottom)
            lineTo(238f, headerBottom)
            lineTo(298f, 0f)
            lineTo(290f, 0f)
            close()
        },
        lightPaint
    )

    /*
     * Logo KAMI.
     */
    val logoX = 78f
    val logoY = 58f
    val logoRadius = 42f

    canvas.drawCircle(
        logoX,
        logoY,
        logoRadius,
        navyPaint
    )

    canvas.drawCircle(
        logoX,
        logoY,
        logoRadius - 4f,
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                android.graphics.Color.WHITE
        }
    )

    canvas.drawText(
        "KAMI",
        logoX,
        logoY + 9f,
        textPaint(
            size = 26f,
            color = navy,
            bold = true,
            align = Paint.Align.CENTER
        )
    )

    val headerX =
        pageWidth - 34f

    /*
     * כותרת — בדיוק בהתאם ל-PDF מסך הבית.
     */
    canvas.drawText(
        tr(
            "סטטיסטיקת הקבוצה",
            "Group Statistics"
        ),
        if (isEnglish) {
            34f
        } else {
            headerX
        },
        52f,
        textPaint(
            size = 24f,
            color =
                android.graphics.Color.WHITE,
            bold = true
        )
    )

    /*
     * תת־כותרת.
     */
    canvas.drawText(
        "${tr("סניף", "Branch")}: " +
                "${branch.ifBlank { "—" }}  ·  " +
                "${tr("קבוצה", "Group")}: " +
                groupKey.ifBlank { "—" },
        if (isEnglish) {
            34f
        } else {
            headerX
        },
        78f,
        textPaint(
            size = 11f,
            color =
                android.graphics.Color.WHITE
        )
    )

    /*
     * התאריך עובר מתחת לאזור הכחול,
     * כמו ב-PDF של מסך הבית.
     */
    val generatedDate =
        java.text.SimpleDateFormat(
            "dd/MM/yyyy",
            java.util.Locale.getDefault()
        ).format(
            java.util.Date()
        )

    canvas.drawText(
        tr(
            "תאריך הפקה:",
            "Generated:"
        ) + " " + generatedDate,
        if (isEnglish) {
            34f
        } else {
            headerX
        },
        142f,
        textPaint(
            size = 8.5f,
            color = muted
        )
    )

    /*
     * נתוני קצה מתוך אותה קבוצה.
     */
    val validAges =
        profiles
            .map { it.age }
            .filter { it > 0 }

    val minAge =
        validAges.minOrNull()
            ?.toString()
            ?: "—"

    val maxAge =
        validAges.maxOrNull()
            ?.toString()
            ?: "—"

    val seniorities =
        profiles
            .mapNotNull {
                parseYearsFromSeniority(
                    it.seniority
                )
            }

    val minSeniority =
        seniorities.minOrNull()
            ?.toString()
            ?: "—"

    val maxSeniority =
        seniorities.maxOrNull()
            ?.toString()
            ?: "—"

    /*
     * כרטיס ראשי.
     */
    var y = 148f

    fun drawStatCard(
        left: Float,
        top: Float,
        width: Float,
        label: String,
        value: String
    ) {
        val bg =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = cardBg
            }

        val stroke =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = border
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

        canvas.drawRoundRect(
            left,
            top,
            left + width,
            top + 70f,
            14f,
            14f,
            bg
        )

        canvas.drawRoundRect(
            left,
            top,
            left + width,
            top + 70f,
            14f,
            14f,
            stroke
        )

        canvas.drawText(
            value,
            left + width / 2f,
            top + 30f,
            textPaint(
                size = 21f,
                color = navy,
                bold = true,
                align = Paint.Align.CENTER
            )
        )

        canvas.drawText(
            label,
            left + width / 2f,
            top + 52f,
            textPaint(
                size = 9.5f,
                color = muted,
                bold = true,
                align = Paint.Align.CENTER
            )
        )
    }

    val gap = 10f

    val cardWidth =
        (
                pageWidth -
                        margin * 2f -
                        gap * 2f
                ) / 3f

    drawStatCard(
        margin,
        y,
        cardWidth,
        tr(
            "מתאמנים",
            "Trainees"
        ),
        stats.totalTrainees.toString()
    )

    drawStatCard(
        margin + cardWidth + gap,
        y,
        cardWidth,
        tr(
            "נוכחות ממוצעת",
            "Avg attendance"
        ),
        if (stats.avgAttendance > 0) {
            "${stats.avgAttendance}%"
        } else {
            "—"
        }
    )

    drawStatCard(
        margin + (cardWidth + gap) * 2f,
        y,
        cardWidth,
        tr(
            "מעל 80% נוכחות",
            "Above 80%"
        ),
        stats.highAttendanceCount.toString()
    )

    y += 92f

    drawStatCard(
        margin,
        y,
        cardWidth,
        tr(
            "גיל מינ׳",
            "Min age"
        ),
        minAge
    )

    drawStatCard(
        margin + cardWidth + gap,
        y,
        cardWidth,
        tr(
            "גיל מקס׳",
            "Max age"
        ),
        maxAge
    )

    drawStatCard(
        margin + (cardWidth + gap) * 2f,
        y,
        cardWidth,
        tr(
            "וותק ממוצע",
            "Avg seniority"
        ),
        formatAvgSeniority(
            stats.avgSeniority,
            isEnglish
        )
    )

    y += 92f

    drawStatCard(
        margin,
        y,
        cardWidth,
        tr(
            "וותק מינ׳",
            "Min seniority"
        ),
        minSeniority
    )

    drawStatCard(
        margin + cardWidth + gap,
        y,
        cardWidth,
        tr(
            "וותק מקס׳",
            "Max seniority"
        ),
        maxSeniority
    )

    drawStatCard(
        margin + (cardWidth + gap) * 2f,
        y,
        cardWidth,
        tr(
            "חגורות שונות",
            "Belt types"
        ),
        stats.beltCounts.size.toString()
    )

    /*
     * התפלגות חגורות.
     */
    y += 104f

    canvas.drawText(
        tr(
            "התפלגות חגורות",
            "Belt Distribution"
        ),
        if (isEnglish) {
            margin
        } else {
            pageWidth - margin
        },
        y,
        textPaint(
            size = 16f,
            color = textDark,
            bold = true
        )
    )

    y += 28f

    stats.beltCounts.forEach {
            (belt, count) ->

        if (y > pageHeight - 70f) {
            return@forEach
        }

        canvas.drawText(
            "${coachBeltNameForStatsPdf(
                belt = belt,
                isEnglish = isEnglish
            )}: $count",
            if (isEnglish) {
                margin + 10f
            } else {
                pageWidth - margin - 10f
            },
            y,
            textPaint(
                size = 11f,
                color = textDark,
                bold = true
            )
        )

        y += 23f
    }

    /*
     * Footer.
     */
    canvas.drawLine(
        margin,
        pageHeight - 42f,
        pageWidth - margin,
        pageHeight - 42f,
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                android.graphics.Color.rgb(
                    226,
                    232,
                    240
                )
            strokeWidth = 1f
        }
    )

    canvas.drawText(
        tr(
            "עמוד 1 · KAMI",
            "Page 1 · KAMI"
        ),
        pageWidth / 2f,
        pageHeight - 24f,
        textPaint(
            size = 9f,
            color = muted,
            align = Paint.Align.CENTER
        )
    )

    document.finishPage(page)

    val dir =
        File(
            context.cacheDir,
            "shared_pdfs"
        ).apply {
            mkdirs()
        }

    val fileName =
        if (isEnglish) {
            "Group Statistics.pdf"
        } else {
            "סטטיסטיקת הקבוצה.pdf"
        }

    val file =
        File(
            dir,
            fileName
        )

    FileOutputStream(file).use {
            output ->
        document.writeTo(output)
    }

    document.close()

    return file
}

private fun coachBeltNameForStatsPdf(
    belt: String,
    isEnglish: Boolean
): String {

    if (!isEnglish) {
        return belt
    }

    return when (
        belt.trim()
    ) {
        "לבנה" -> "White"
        "צהובה" -> "Yellow"
        "כתומה" -> "Orange"
        "ירוקה" -> "Green"
        "כחולה" -> "Blue"
        "חומה" -> "Brown"
        "שחורה" -> "Black"
        "ללא דרגה" -> "No rank"
        else -> belt
    }
}