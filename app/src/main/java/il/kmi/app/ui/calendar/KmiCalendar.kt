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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    markers: KmiCalendarMarkers = KmiCalendarMarkers(),
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme =
        colorScheme.background.luminance() < 0.5f

    val navigationButtonColor =
        if (isDarkTheme) {
            Color(0xFF0A234A)
        } else {
            colorScheme.primaryContainer
        }

    val navigationButtonContentColor =
        if (isDarkTheme) {
            Color.White
        } else {
            colorScheme.onPrimaryContainer
        }

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
        buildList<Int?> {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        space = 8.dp,
                        alignment =
                            Alignment.CenterHorizontally
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
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
                    color = navigationButtonColor,
                    tonalElevation = 0.dp,
                    shadowElevation =
                        if (isDarkTheme) 0.dp else 2.dp,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text = "‹",
                            color =
                                navigationButtonContentColor,
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight =
                                FontWeight.Black,
                            textAlign =
                                TextAlign.Center
                        )
                    }
                }

                Text(
                    text = monthTitle,
                    color = primaryTextColor,
                    fontWeight = FontWeight.Black,
                    style =
                        MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
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
                    color = navigationButtonColor,
                    tonalElevation = 0.dp,
                    shadowElevation =
                        if (isDarkTheme) 0.dp else 2.dp,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text = "›",
                            color =
                                navigationButtonContentColor,
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight =
                                FontWeight.Black,
                            textAlign =
                                TextAlign.Center
                        )
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
                if (isDarkTheme) 0.dp else 2.dp,
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
                        fontWeight = FontWeight.Black,
                        style =
                            MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
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
                if (isDarkTheme) 0.dp else 3.dp,
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
                                    .height(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (
                                    day != null &&
                                    cellDate != null
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(32.dp)
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
                                                fontWeight = FontWeight.Black,
                                                style =
                                                    MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center
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
            shadowElevation = 20.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF07152E),
                                Color(0xFF0B1E48),
                                Color(0xFF103C89),
                                Color(0xFF18BDEB)
                            )
                        )
                    )
                    .padding(1.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(29.dp),
                    color = Color(0xFF061832)
                        .copy(alpha = 0.96f)
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

                            Divider(
                                color = Color.White.copy(
                                    alpha = 0.14f
                                )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.09f),
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.18f)
            )
        ) {
            Text(
                text = "📅",
                fontSize = 20.sp,
                modifier = Modifier.padding(9.dp)
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
                color = Color(0xFFBFDBFE),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = if (isEnglish) {
                    TextAlign.Start
                } else {
                    TextAlign.End
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = selectedTitle,
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    lineHeight = 25.sp
                ),
                textAlign = if (isEnglish) {
                    TextAlign.Start
                } else {
                    TextAlign.End
                },
                modifier = Modifier.fillMaxWidth()
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.12f)
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
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = if (isEnglish) {
                        "Cancel"
                    } else {
                        "ביטול"
                    },
                    color = Color(0xFFBFDBFE),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                onClick = onToday,
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF22D3EE),
                shadowElevation = 5.dp
            ) {
                Box(
                    modifier = Modifier.padding(
                        horizontal = 22.dp,
                        vertical = 9.dp
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Today"
                        } else {
                            "היום"
                        },
                        color = Color(0xFF04101F),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
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