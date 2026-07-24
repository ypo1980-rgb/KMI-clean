@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.coach

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.ui.calendar.KmiCalendarPickerDialog
import java.time.LocalDate

@Composable
internal fun CoachTopStatsCard(
    stats: GroupStatsUi,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isEnglish: Boolean,
    showSearch: Boolean = true
) {

    Surface(
        color = Color(0xFFF4F8FF),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFD8E4F4)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = coachHorizontalAlignment(isEnglish)
        ) {
            Text(
                text = coachTr(isEnglish, "רשימת מתאמנים", "Trainees list"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 20.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF172036),
                textAlign = coachTextAlign(isEnglish),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = coachTr(
                    isEnglish,
                    "נתוני נוכחות וקבוצה בזמן אמת",
                    "attendance and group data"
                ),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                ),
                color = Color(0xFF64748B),
                textAlign = coachTextAlign(isEnglish),
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CoachTopStatTile(
                        value = stats.totalTrainees.toString(),
                        label = coachTr(isEnglish, "מתאמנים", "Trainees"),
                        modifier = Modifier.weight(1f)
                    )

                    CoachTopStatTile(
                        value = stats.beltCounts
                            .filterValues { it > 0 }
                            .size
                            .toString(),
                        label = coachTr(isEnglish, "דרגות", "Ranks"),
                        modifier = Modifier.weight(1f)
                    )

                    CoachTopStatTile(
                        value = if (stats.avgAttendance > 0) "${stats.avgAttendance}%" else "—",
                        label = coachTr(isEnglish, "נוכחות", "Attendance"),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CoachTopStatTile(
                        value = if (stats.avgAge > 0) stats.avgAge.toString() else "—",
                        label = coachTr(isEnglish, "גיל ממוצע", "Avg age"),
                        modifier = Modifier.weight(1f)
                    )

                    CoachTopStatTile(
                        value = stats.highAttendanceCount.toString(),
                        label = coachTr(isEnglish, "נוכחות גבוהה", "High attendance"),
                        modifier = Modifier.weight(1f)
                    )

                    CoachTopStatTile(
                        value = formatAvgSeniority(stats.avgSeniority, isEnglish),
                        label = coachTr(isEnglish, "וותק ממוצע", "Avg seniority"),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        textAlign = coachTextAlign(isEnglish),
                        color = Color.White
                    ),
                    placeholder = {
                        Text(
                            text = coachTr(
                                isEnglish,
                                "חיפוש מתאמן",
                                "Search trainee"
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.62f)
                            )
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "🔎",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = coachTr(isEnglish, "נקה חיפוש", "Clear search"),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        onSearchQueryChange("")
                                    },
                                tint = Color.White.copy(alpha = 0.82f)
                            )
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.White.copy(alpha = 0.60f),
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFFA78BFA),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.62f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.62f),
                        focusedLeadingIconColor = Color.White.copy(alpha = 0.82f),
                        unfocusedLeadingIconColor = Color.White.copy(alpha = 0.82f),
                        focusedTrailingIconColor = Color.White.copy(alpha = 0.82f),
                        unfocusedTrailingIconColor = Color.White.copy(alpha = 0.82f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CoachTopStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val iconText = when {
        label.contains("מתאמנים") || label.contains("Trainees") -> "👥"
        label.contains("דרגות") || label.contains("Ranks") -> "🎖️"
        label.contains("נוכחות גבוהה") || label.contains("High attendance") -> "✅"
        label.contains("נוכחות") || label.contains("Attendance") -> "📊"
        label.contains("גיל") || label.contains("Age") -> "🎂"
        label.contains("וותק") || label.contains("seniority", ignoreCase = true) -> "⏱️"
        else -> "⭐"
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFD6E0EE)
        ),
        modifier = modifier.heightIn(min = 72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = iconText,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 17.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF172036),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(1.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    lineHeight = 10.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun PremiumCoachDateField(
    label: String,
    value: String,
    placeholder: String,
    accent: Color,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF8FAFC),
        shadowElevation = 4.dp,
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.72f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isEnglish) {
                            listOf(
                                Color.White,
                                accent.copy(alpha = 0.05f),
                                Color.White
                            )
                        } else {
                            listOf(
                                Color.White,
                                accent.copy(alpha = 0.05f),
                                Color.White
                            )
                        }
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅",
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    ),
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = coachTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 20.sp
                    ),
                    color = if (value.isBlank()) {
                        Color(0xFF64748B)
                    } else {
                        Color(0xFF0F172A)
                    },
                    fontWeight = FontWeight.Black,
                    textAlign = coachTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun PremiumCoachDatePickerDialog(
    title: String,
    selectedDate: String,
    accent: Color,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val parsedSelectedDate = remember(selectedDate) {
        runCatching {
            LocalDate.parse(
                selectedDate.trim()
            )
        }.getOrNull()
    }

    KmiCalendarPickerDialog(
        title = title,
        selectedDate = parsedSelectedDate,
        isEnglish = isEnglish,
        onDismiss = onDismiss,
        onDateSelected = { date ->
            onDateSelected(
                date.toString()
            )
        }
    )
}

@Composable
internal fun PremiumCoachLoading() {
    val infiniteTransition = rememberInfiniteTransition(
        label = "premiumCoachLoading"
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
        label = "premiumCoachOuterRotation"
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
        label = "premiumCoachInnerRotation"
    )

    Box(
        modifier = Modifier.size(82.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    rotationZ = outerRotation
                }
                .border(
                    width = 5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFA78BFA),
                            Color(0xFF38BDF8),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(52.dp)
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
            modifier = Modifier.size(25.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
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
                    text = "👥",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
