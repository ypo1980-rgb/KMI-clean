@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.coach

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import il.kmi.app.screens.coach.statistics.NationalStatisticsScreen
import il.kmi.app.ui.KmiTypography

@Composable
internal fun CoachGroupStatsPremiumScreen(
    stats: GroupStatsUi,
    profiles: List<TraineeProfile>,
    isEnglish: Boolean,
    onClose: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenHome: () -> Unit
) {
    var showNationalStatistics by rememberSaveable {
        mutableStateOf(false)
    }

    /*
     * כאשר הטאב הארצי פתוח, חזור מחזיר תחילה
     * לטאב הקבוצה ולא יוצא מהמסך כולו.
     */
    androidx.activity.compose.BackHandler(
        enabled = showNationalStatistics
    ) {
        showNationalStatistics = false
    }

    if (showNationalStatistics) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFEAF4FF),
                            Color(0xFFB7DDF7),
                            Color(0xFF1F78B4),
                            Color(0xFF062B4A)
                        )
                    )
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                )
        ) {
            StatisticsTabsSelector(
                isEnglish = isEnglish,
                nationalSelected = true,
                onGroupClick = {
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
                    showNationalStatistics = false
                },
                onOpenDrawer = onOpenDrawer,
                onOpenHome = onOpenHome
            )
        }

        return
    }

    val statsTextAlign =
        coachTextAlign(isEnglish)
    val statsHorizontalAlignment = coachHorizontalAlignment(isEnglish)

    val statsAccent = Color(0xFF4F46E5)
    val glassTop = Color.White.copy(alpha = 0.72f)
    val glassBottom = Color.White.copy(alpha = 0.38f)

    val statsBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FBFF),
            Color(0xFFEAF4FF),
            Color(0xFFB7DDF7),
            Color(0xFF1F78B4),
            Color(0xFF062B4A)
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

    val glowTransition = rememberInfiniteTransition(label = "glassGlow")
    val glowShift by glowTransition.animateFloat(
        initialValue = -160f,
        targetValue = 460f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glassGlowShift"
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
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
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
                        color = Color(0xFFFCFDFF),
                        shape = RoundedCornerShape(28.dp),
                        shadowElevation = 7.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFE7ECF7)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White,
                                            Color(0xFFFAFBFF),
                                            Color(0xFFF7FAFF)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 16.dp),
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
                                    color = Color(0xFFFCFDFF),
                                shape = RoundedCornerShape(24.dp),
                                shadowElevation = 5.dp,
                                tonalElevation = 0.dp,
                                border = BorderStroke(1.dp, Color(0xFFE7ECF7)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(132.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.White,
                                                    Color(0xFFFAFBFF),
                                                    Color(0xFF4F46E5).copy(alpha = 0.07f)
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
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontSize = 32.sp,
                                                lineHeight = 34.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF4F46E5)
                                            )
                                        )

                                        Text(
                                            text = "🏆",
                                            fontSize = 22.sp,
                                            textAlign = TextAlign.Left
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = coachTr(isEnglish, "מעל 80%", "Above 80%"),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = statsTextAlign,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF4F46E5)
                                        )
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = coachTr(isEnglish, "נוכחות", "Attendance"),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = statsTextAlign,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFFFCFDFF),
                                shape = RoundedCornerShape(24.dp),
                                shadowElevation = 5.dp,
                                tonalElevation = 0.dp,
                                border = BorderStroke(1.dp, Color(0xFFE7ECF7)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(132.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.White,
                                                    Color(0xFFFAFBFF),
                                                    Color(0xFF22C55E).copy(alpha = 0.07f)
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
                                        modifier =
                                            Modifier.fillMaxWidth(),
                                        textAlign =
                                            TextAlign.Center,
                                        maxLines = 1,
                                        style =
                                            MaterialTheme.typography
                                                .bodySmall.copy(
                                                    fontSize = 12.sp,
                                                    lineHeight = 14.sp,
                                                    fontWeight =
                                                        FontWeight.Bold,
                                                    color =
                                                        Color(0xFF64748B)
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
                    color = Color(0xFFFCFDFF),
                    shape = RoundedCornerShape(28.dp),
                shadowElevation = 7.dp,
                tonalElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    Color(0xFFE7ECF7)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFFAFBFF),
                                    Color(0xFFF7FAFF)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = coachTr(isEnglish, "התפלגות חגורות בסניף", "Belt distribution"),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = statsTextAlign,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    )

                    if (stats.beltCounts.isEmpty()) {
                        Text(
                            text = coachTr(isEnglish, "אין נתונים להצגה", "No data to display"),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = statsTextAlign,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        stats.beltCounts.forEach { (belt, count) ->
                            val progress =
                                if (profiles.isNotEmpty()) count.toFloat() / profiles.size.toFloat() else 0f

                            val beltColor = beltColorForStats(belt)
                            val labelColor =
                                if (beltColor == Color(0xFFE5E7EB)) Color(0xFF475569) else beltColor

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = beltColor.copy(alpha = 0.14f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$count",
                                                color = labelColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = coachBeltNameForUi(belt, isEnglish),
                                        modifier = Modifier.weight(1f),
                                        textAlign = statsTextAlign,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 18.sp,
                                            lineHeight = 21.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = labelColor
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(beltColor)
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = beltColor,
                                    trackColor = beltColor.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(999.dp))
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
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.65f)
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
                    Text(text = "📊", fontSize = 24.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish)
            ) {
                Text(
                    text = coachTr(
                        isEnglish,
                        "סטטיסטיקה ארצית",
                        "National statistics"
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
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
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = if (isEnglish) "›" else "‹",
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
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
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFCFDFF),
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE7ECF7)
        ),
        modifier = modifier
            .height(96.dp)
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
                            Color.White,
                            Color(0xFFFAFBFF),
                            accent.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(start = 6.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = accent.copy(alpha = 0.13f),
                shadowElevation = 2.dp,
                modifier = Modifier.size(34.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 17.sp,
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
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (valueNumber > 0f) animatedValue.toInt().toString() else value,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 28.sp,
                        lineHeight = 31.sp,
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
        color = Color(0xFFFCFDFF),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 7.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            Color(0xFFE7ECF7)
        ),
        modifier = modifier
            .height(78.dp)
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
                            Color.White,
                            Color(0xFFFAFBFF),
                            accent.copy(alpha = 0.06f)
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
                        fontSize = 19.sp,
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
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                )

                Text(
                    text = if (valueNumber > 0f) animatedValue.toInt().toString() else value,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        lineHeight = 24.sp,
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
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "attendanceRing"
    )

    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            drawCircle(
                color = Color(0xFFE5E7EB),
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
    }
}