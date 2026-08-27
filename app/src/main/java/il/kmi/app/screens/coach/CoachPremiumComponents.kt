@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.coach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.scaledIconSize
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
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
                .copy(alpha = 0.28f)
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
                text = coachTr(
                    isEnglish,
                    "רשימת מתאמנים",
                    "Trainees list"
                ),
                style = KmiTypography.sectionTitle.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = coachTextAlign(isEnglish),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = coachTr(
                    isEnglish,
                    "נתוני נוכחות וקבוצה בזמן אמת",
                    "attendance and group data"
                ),
                style = KmiTypography.secondary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = coachTextAlign(isEnglish),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
                    textStyle = KmiTypography.body.copy(
                        textAlign = coachTextAlign(isEnglish),
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    placeholder = {
                        Text(
                            text = coachTr(
                                isEnglish,
                                "חיפוש מתאמן",
                                "Search trainee"
                            ),
                            style = KmiTypography.secondary,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.72f)
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "🔎",
                            style = KmiTypography.body,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = coachTr(
                                    isEnglish,
                                    "נקה חיפוש",
                                    "Clear search"
                                ),
                                modifier = Modifier
                                    .size(
                                        scaledIconSize(18.dp)
                                    )
                                    .clickable {
                                        onSearchQueryChange("")
                                    },
                                tint =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor =
                            MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor =
                            MaterialTheme.colorScheme.onSurface,
                        disabledTextColor =
                            MaterialTheme.colorScheme.onSurface
                                .copy(alpha = 0.60f),
                        cursorColor =
                            MaterialTheme.colorScheme.primary,
                        focusedBorderColor =
                            MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor =
                            MaterialTheme.colorScheme.outline
                                .copy(alpha = 0.35f),
                        focusedContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant,
                        focusedPlaceholderColor =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLeadingIconColor =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLeadingIconColor =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTrailingIconColor =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTrailingIconColor =
                            MaterialTheme.colorScheme.onSurfaceVariant
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
                .copy(alpha = 0.24f)
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
                style = KmiTypography.body,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = value,
                style = KmiTypography.cardTitle.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(1.dp))

            Text(
                text = label,
                style = KmiTypography.caption.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            accent.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅",
                style = KmiTypography.sectionTitle,
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
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = accent,
                    textAlign = coachTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = value.ifBlank { placeholder },
                    style = KmiTypography.sectionTitle.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color =
                        if (value.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun PremiumCoachDatePickerDialog(
    title: String,
    selectedDate: String,
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
