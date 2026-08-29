package il.kmi.app.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import il.kmi.app.ui.KmiTypography
import il.yuval.ui.theme.kmiSectionHeaderBrush
import il.yuval.ui.theme.kmiSectionHeaderContentColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class KmiCalendarMarkers(
    val trainingDates: Set<LocalDate> = emptySet(),
    val holidayDates: Set<LocalDate> = emptySet(),
    val summaryDates: Set<LocalDate> = emptySet()
)

@Composable
fun KmiCalendarMonth(
    visibleMonth: YearMonth,
    selectedDate: LocalDate?,
    isEnglish: Boolean,
    onVisibleMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    markers: KmiCalendarMarkers = KmiCalendarMarkers()
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme =
        colorScheme.background.luminance() < 0.5f

    val calendarSectionColor =
        if (isDarkTheme) {
            Color.White.copy(alpha = 0.08f)
        } else {
            colorScheme.surface
        }

    val calendarSectionBorderColor =
        if (isDarkTheme) {
            Color.White.copy(alpha = 0.10f)
        } else {
            colorScheme.outline.copy(alpha = 0.20f)
        }

    val primaryTextColor =
        if (isDarkTheme) {
            Color.White
        } else {
            colorScheme.onSurface
        }

    val weekDayTextColor =
        if (isDarkTheme) {
            Color(0xFF67E8F9)
        } else {
            colorScheme.primary
        }

    val selectedDayColor =
        if (isDarkTheme) {
            Color(0xFF22D3EE)
        } else {
            colorScheme.primary
        }

    val selectedDayTextColor =
        if (isDarkTheme) {
            Color(0xFF031226)
        } else {
            colorScheme.onPrimary
        }

    val todayBackgroundColor =
        if (isDarkTheme) {
            Color.White.copy(alpha = 0.14f)
        } else {
            colorScheme.primaryContainer.copy(alpha = 0.55f)
        }

    val firstDayOfMonth = remember(visibleMonth) {
        visibleMonth.atDay(1)
    }

    // Sunday = 0 ... Saturday = 6
    val leadingEmptyDays = remember(firstDayOfMonth) {
        firstDayOfMonth.dayOfWeek.value % 7
    }

    val daysInMonth = remember(visibleMonth) {
        visibleMonth.lengthOfMonth()
    }

    val monthLocale = remember(isEnglish) {
        if (isEnglish) {
            Locale.ENGLISH
        } else {
            Locale("he", "IL")
        }
    }

    val monthTitle = remember(visibleMonth, monthLocale) {
        visibleMonth
            .atDay(1)
            .format(
                DateTimeFormatter.ofPattern(
                    "MMMM yyyy",
                    monthLocale
                )
            )
    }

    val weekDays = remember(isEnglish) {
        if (isEnglish) {
            listOf(
                "Sun", "Mon", "Tue", "Wed",
                "Thu", "Fri", "Sat"
            )
        } else {
            listOf(
                "א׳", "ב׳", "ג׳", "ד׳",
                "ה׳", "ו׳", "ש׳"
            )
        }
    }

    val cells = remember(
        visibleMonth,
        leadingEmptyDays,
        daysInMonth
    ) {
        buildList {
            repeat(leadingEmptyDays) {
                add(null)
            }

            for (day in 1..daysInMonth) {
                add(day)
            }

            while (size % 7 != 0) {
                add(null)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        /*
      * כותרת החודש מוצגת תמיד בסידור פיזי קבוע:
      *
      * חץ שמאלה בצד שמאל — חודש קודם.
      * חץ ימינה בצד ימין — חודש הבא.
      *
      * הכפייה ל־LTR משפיעה רק על מיקום החצים,
      * ולא על שפת הכותרת.
      */
        CompositionLocalProvider(
            LocalLayoutDirection provides
                    LayoutDirection.Ltr
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = kmiSectionHeaderBrush()
                    )
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Surface(
                        onClick = {
                            onVisibleMonthChange(
                                if (isEnglish) {
                                    visibleMonth.minusMonths(1)
                                } else {
                                    visibleMonth.plusMonths(1)
                                }
                            )
                        },
                        shape = CircleShape,
                        color = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "‹",
                                color = kmiSectionHeaderContentColor(),
                                style =
                                    KmiTypography.sectionTitle.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = monthTitle,
                        color = kmiSectionHeaderContentColor(),
                        style =
                            KmiTypography.sectionTitle.copy(
                                fontWeight = FontWeight.Black
                            ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        onClick = {
                            onVisibleMonthChange(
                                if (isEnglish) {
                                    visibleMonth.plusMonths(1)
                                } else {
                                    visibleMonth.minusMonths(1)
                                }
                            )
                        },
                        shape = CircleShape,
                        color = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "›",
                                color = kmiSectionHeaderContentColor(),
                                style =
                                    KmiTypography.sectionTitle.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = calendarSectionColor,
            tonalElevation = 0.dp,
            shadowElevation =
                if (isDarkTheme) {
                    0.dp
                } else {
                    1.dp
                },
            border = BorderStroke(
                width = 1.dp,
                color = calendarSectionBorderColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
            ) {
                weekDays.forEach { dayName ->
                    Text(
                        text = dayName,
                        color = weekDayTextColor,
                        style =
                            KmiTypography.caption.copy(
                                fontWeight = FontWeight.Black
                            ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = calendarSectionColor,
            tonalElevation = 0.dp,
            shadowElevation =
                if (isDarkTheme) {
                    0.dp
                } else {
                    1.dp
                },
            border = BorderStroke(
                width = 1.dp,
                color = calendarSectionBorderColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 7.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                cells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        week.forEach { day ->
                            val cellDate = day?.let {
                                visibleMonth.atDay(it)
                            }

                            val isSelected =
                                cellDate != null &&
                                        cellDate == selectedDate

                            val isToday =
                                cellDate != null &&
                                        cellDate == LocalDate.now()

                            val hasTraining =
                                cellDate != null &&
                                        cellDate in markers.trainingDates

                            val hasHoliday =
                                cellDate != null &&
                                        cellDate in markers.holidayDates

                            val hasSummary =
                                cellDate != null &&
                                        cellDate in markers.summaryDates

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (
                                    day != null &&
                                    cellDate != null
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .sizeIn(
                                                minWidth = 34.dp,
                                                minHeight = 34.dp
                                            )
                                            .clickable {
                                                onDateSelected(cellDate)
                                            },
                                        shape = CircleShape,
                                        color = when {
                                            isSelected ->
                                                selectedDayColor

                                            isToday ->
                                                todayBackgroundColor

                                            else ->
                                                Color.Transparent
                                        },
                                        border = when {
                                            isSelected -> null

                                            isToday -> BorderStroke(
                                                width = 1.dp,
                                                color = selectedDayColor
                                            )

                                            else -> null
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.toString(),
                                                color =
                                                    if (isSelected) {
                                                        selectedDayTextColor
                                                    } else {
                                                        primaryTextColor
                                                    },
                                                style =
                                                    KmiTypography.body.copy(
                                                        fontWeight = FontWeight.Black
                                                    ),
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )

                                            if (
                                                hasTraining ||
                                                hasHoliday ||
                                                hasSummary
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .align(
                                                            Alignment.BottomCenter
                                                        )
                                                        .padding(bottom = 2.dp),
                                                    horizontalArrangement =
                                                        Arrangement.spacedBy(2.dp),
                                                    verticalAlignment =
                                                        Alignment.CenterVertically
                                                ) {
                                                    if (hasTraining) {
                                                        CalendarMarkerDot(
                                                            color = Color(
                                                                0xFF3FA7FF
                                                            )
                                                        )
                                                    }

                                                    if (hasHoliday) {
                                                        CalendarMarkerDot(
                                                            color = Color(
                                                                0xFFFF4D6D
                                                            )
                                                        )
                                                    }

                                                    if (hasSummary) {
                                                        CalendarMarkerDot(
                                                            color = Color(
                                                                0xFFA78BFA
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
        }
    }
}

@Composable
fun KmiCalendarPickerDialog(
    title: String,
    selectedDate: LocalDate?,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    markers: KmiCalendarMarkers = KmiCalendarMarkers()
) {
    val initialDate = selectedDate ?: LocalDate.now()

    var visibleMonth by remember(initialDate) {
        mutableStateOf(
            YearMonth.from(initialDate)
        )
    }

    val locale = remember(isEnglish) {
        if (isEnglish) {
            Locale.ENGLISH
        } else {
            Locale("he", "IL")
        }
    }

    val selectedTitle = remember(initialDate, locale) {
        initialDate.format(
            DateTimeFormatter.ofPattern(
                "EEEE • d MMMM yyyy",
                locale
            )
        )
    }

    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme =
        colorScheme.background.luminance() < 0.5f

    val dialogContainerColor =
        if (isDarkTheme) {
            Color(0xFF061832).copy(alpha = 0.96f)
        } else {
            colorScheme.surface.copy(alpha = 0.98f)
        }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .heightIn(max = 690.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.verticalGradient(
                            colors =
                                if (isDarkTheme) {
                                    listOf(
                                        Color(0xFF07152E),
                                        Color(0xFF0B1E48),
                                        Color(0xFF103C89),
                                        Color(0xFF18BDEB)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFEFFBFF),
                                        Color(0xFFDBF4FF),
                                        Color(0xFFBAE6FD),
                                        Color(0xFF38BDF8)
                                    )
                                }
                        )
                    )
                    .padding(1.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(29.dp),
                    color = dialogContainerColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides
                                if (isEnglish) {
                                    LayoutDirection.Ltr
                                } else {
                                    LayoutDirection.Rtl
                                }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(
                                    rememberScrollState()
                                )
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 10.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            CalendarDialogHeader(
                                title = title,
                                selectedTitle = selectedTitle,
                                isEnglish = isEnglish
                            )

                            HorizontalDivider(
                                color =
                                    colorScheme
                                        .outlineVariant
                                        .copy(alpha = 0.65f)
                            )

                            KmiCalendarMonth(
                                visibleMonth = visibleMonth,
                                selectedDate = selectedDate,
                                isEnglish = isEnglish,
                                onVisibleMonthChange = {
                                    visibleMonth = it
                                },
                                onDateSelected = { date ->
                                    onDateSelected(date)
                                    onDismiss()
                                },
                                markers = markers
                            )

                            CalendarDialogActions(
                                isEnglish = isEnglish,
                                onDismiss = onDismiss,
                                onToday = {
                                    onDateSelected(
                                        LocalDate.now()
                                    )
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDialogHeader(
    title: String,
    selectedTitle: String,
    isEnglish: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme =
        colorScheme.background.luminance() < 0.5f

    val headerAccentColor =
        if (isDarkTheme) {
            Color(0xFFBFDBFE)
        } else {
            colorScheme.primary
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color =
                if (isDarkTheme) {
                    Color.White.copy(alpha = 0.09f)
                } else {
                    colorScheme.primaryContainer.copy(alpha = 0.55f)
                },
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                colorScheme.outline.copy(alpha = 0.35f)
            )
        ) {
            Text(
                text = "📅",
                style = KmiTypography.metric,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(9.dp),
                maxLines = 1
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (isEnglish) {
                Alignment.Start
            } else {
                Alignment.End
            }
        ) {
            Text(
                text = title,
                color = headerAccentColor,
                style =
                    KmiTypography.cardTitle.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                textAlign =
                    if (isEnglish) {
                        TextAlign.Start
                    } else {
                        TextAlign.End
                    },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = selectedTitle,
                color = colorScheme.onSurface,
                style =
                    KmiTypography.screenTitle.copy(
                        fontWeight = FontWeight.Black
                    ),
                textAlign =
                    if (isEnglish) {
                        TextAlign.Start
                    } else {
                        TextAlign.End
                    },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CalendarDialogActions(
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onToday: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme =
        colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color =
            if (isDarkTheme) {
                Color.White.copy(alpha = 0.07f)
            } else {
                colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            colorScheme.outline.copy(alpha = 0.30f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                ),
            horizontalArrangement = if (isEnglish) {
                Arrangement.End
            } else {
                Arrangement.Start
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            "Cancel"
                        } else {
                            "ביטול"
                        },
                    color =
                        if (isDarkTheme) {
                            Color(0xFFBFDBFE)
                        } else {
                            colorScheme.primary
                        },
                    style =
                        KmiTypography.action.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                onClick = onToday,
                shape = RoundedCornerShape(999.dp),
                color =
                    if (isDarkTheme) {
                        Color(0xFF22D3EE)
                    } else {
                        colorScheme.primary
                    },
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(
                        horizontal = 22.dp,
                        vertical = 9.dp
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Today"
                            } else {
                                "היום"
                            },
                        color =
                            if (isDarkTheme) {
                                Color(0xFF04101F)
                            } else {
                                colorScheme.onPrimary
                            },
                        style =
                            KmiTypography.action.copy(
                                fontWeight = FontWeight.Black
                            ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMarkerDot(
    color: Color
) {
    Box(
        modifier = Modifier
            .size(3.dp)
            .background(
                color = color,
                shape = CircleShape
            )
    )
}