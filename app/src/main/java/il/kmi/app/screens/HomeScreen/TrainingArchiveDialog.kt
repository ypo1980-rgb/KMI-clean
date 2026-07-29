package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import android.app.Activity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.LocalContext
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import il.kmi.app.training.TrainingData
import il.kmi.app.training.TrainingOverride
import il.kmi.app.training.TrainingOverrideRepository
import il.kmi.app.training.TrainingStatusEngine
import il.kmi.app.ui.calendar.KmiCalendarMarkers
import il.kmi.app.ui.calendar.KmiCalendarPickerDialog
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


//==============================================================================

private val archiveIsraelZone: ZoneId =
    ZoneId.of("Asia/Jerusalem")

data class TrainingArchiveSource(
    val training: TrainingData,
    val branch: String,
    val group: String
)

private data class TrainingArchiveItem(
    val training: TrainingData,
    val branch: String,
    val group: String,
    val status: TrainingStatusEngine.Status,
    val activeOverride: TrainingOverride?,
    val localDate: LocalDate
) {
    val isCancelled: Boolean
        get() =
            activeOverride?.isCancelled == true ||
                    status.isCancelled

    val isCompleted: Boolean
        get() =
            !isCancelled &&
                    status.isCompleted

    val hasChangedTime: Boolean
        get() =
            activeOverride?.hasChangedTime == true
}

private enum class ArchiveStatusFilter {
    ALL,
    COMPLETED,
    CANCELLED
}

private data class ArchiveQuickRange(
    val days: Long,
    val titleHe: String,
    val titleEn: String
)

private val archiveQuickRanges =
    listOf(
        ArchiveQuickRange(
            days = 30L,
            titleHe = "30 ימים",
            titleEn = "30 days"
        ),
        ArchiveQuickRange(
            days = 90L,
            titleHe = "3 חודשים",
            titleEn = "3 months"
        ),
        ArchiveQuickRange(
            days = 180L,
            titleHe = "6 חודשים",
            titleEn = "6 months"
        ),
        ArchiveQuickRange(
            days = 365L,
            titleHe = "שנה",
            titleEn = "1 year"
        )
    )

@Composable
fun TrainingArchiveDialog(
    baseTrainings: List<TrainingArchiveSource>,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    onSettings: () -> Unit = {},
    onOpenExercise: (String) -> Unit = {},
    onOpenAi: () -> Unit = {}
) {
    val context =
        LocalContext.current

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val locale =
        remember(isEnglish) {
            if (isEnglish) {
                Locale.US
            } else {
                Locale("he", "IL")
            }
        }

    val nowMillis =
        remember {
            System.currentTimeMillis()
        }

    val today =
        remember(nowMillis) {
            Instant
                .ofEpochMilli(nowMillis)
                .atZone(archiveIsraelZone)
                .toLocalDate()
        }

    var fromDate by remember {
        mutableStateOf(
            today.minusDays(89L)
        )
    }

    var toDate by remember {
        mutableStateOf(today)
    }

    var showFromDatePicker by remember {
        mutableStateOf(false)
    }

    var showToDatePicker by remember {
        mutableStateOf(false)
    }

    var statusFilter by remember {
        mutableStateOf(
            ArchiveStatusFilter.ALL
        )
    }

    var activeOverridesByOccurrenceKey by remember {
        mutableStateOf<Map<String, TrainingOverride>>(
            emptyMap()
        )
    }

    val archiveRangeStartMillis =
        remember(fromDate) {
            fromDate
                .atStartOfDay(archiveIsraelZone)
                .toInstant()
                .toEpochMilli()
        }

    val archiveRangeEndMillis =
        remember(toDate) {
            toDate
                .plusDays(1L)
                .atStartOfDay(archiveIsraelZone)
                .toInstant()
                .toEpochMilli() - 1L
        }

    DisposableEffect(
        archiveRangeStartMillis,
        archiveRangeEndMillis
    ) {
        val listenerHandle =
            TrainingOverrideRepository
                .listenForOverridesInRange(
                    fromOriginalStartMillis =
                        archiveRangeStartMillis,
                    toOriginalStartMillis =
                        archiveRangeEndMillis,
                    onChanged = { overrides ->
                        activeOverridesByOccurrenceKey =
                            overrides
                    },
                    onError = {
                        activeOverridesByOccurrenceKey =
                            emptyMap()
                    }
                )

        onDispose {
            listenerHandle.remove()
        }
    }

    /*
     * מספר השבועות שנדרש ליצור נקבע לפי התאריך
     * המוקדם שבחר המשתמש. מגבילים לעשר שנים.
     */
    val weeksToGenerate =
        remember(fromDate, today) {
            val requestedWeeks =
                ChronoUnit.WEEKS
                    .between(
                        fromDate,
                        today
                    )
                    .toInt()
                    .coerceAtLeast(0) + 3

            requestedWeeks.coerceIn(
                4,
                520
            )
        }

    /*
     * כל מופעי העבר מחושבים מלוח האימונים השבועי,
     * ולא ממקור מידע נוסף.
     */
    val allArchiveItems =
        remember(
            baseTrainings,
            nowMillis,
            weeksToGenerate,
            activeOverridesByOccurrenceKey
        ) {
            baseTrainings
                .flatMap { source ->
                    (0..weeksToGenerate).map { weeksBack ->
                        val archiveCalendar =
                            (
                                    source.training.cal.clone()
                                            as Calendar
                                    ).apply {
                                    add(
                                        Calendar.WEEK_OF_YEAR,
                                        -weeksBack
                                    )
                                }

                        source.copy(
                            training =
                                source.training.copy(
                                    cal = archiveCalendar
                                )
                        )
                    }
                }
                .filter { source ->
                    source.training.startMillis <
                            nowMillis
                }
                .distinctBy { source ->
                    listOf(
                        source.training.startMillis,
                        source.branch,
                        source.group,
                        source.training.place,
                        source.training.address,
                        source.training.coach
                    ).joinToString("|")
                }
                .map { source ->
                    val training =
                        source.training

                    val occurrenceKey =
                        TrainingOverrideRepository
                            .buildOccurrenceKey(
                                training = training,
                                branch = source.branch,
                                group = source.group
                            )

                    val activeOverride =
                        activeOverridesByOccurrenceKey[
                            occurrenceKey
                        ]

                    val status =
                        TrainingStatusEngine.evaluate(
                            context = context,
                            training = training,
                            nowMillis = nowMillis
                        )

                    TrainingArchiveItem(
                        training = training,
                        branch = source.branch,
                        group = source.group,
                        status = status,
                        activeOverride = activeOverride,
                        localDate =
                            Instant
                                .ofEpochMilli(
                                    training.startMillis
                                )
                                .atZone(
                                    archiveIsraelZone
                                )
                                .toLocalDate()
                    )
                }
                .filter { item ->
                    item.isCompleted ||
                            item.isCancelled
                }
                .sortedByDescending { item ->
                    item.activeOverride
                        ?.effectiveStartMillis
                        ?: item.training.startMillis
                }
        }

    /*
    * תחילה מסננים לפי טווח התאריכים.
    * הספירות מחושבות מכל האימונים בטווח,
    * לפני הפעלת מסנן הסטטוס.
    */
    val dateFilteredItems =
        remember(
            allArchiveItems,
            fromDate,
            toDate
        ) {
            allArchiveItems.filter { item ->
                !item.localDate.isBefore(
                    fromDate
                ) &&
                        !item.localDate.isAfter(
                            toDate
                        )
            }
        }

    val completedCount =
        remember(dateFilteredItems) {
            dateFilteredItems.count { item ->
                item.isCompleted
            }
        }

    val cancelledCount =
        remember(dateFilteredItems) {
            dateFilteredItems.count { item ->
                item.isCancelled
            }
        }

    /*
     * לחיצה על כרטיס סיכום מסננת את הרשימה.
     */
    val filteredItems =
        remember(
            dateFilteredItems,
            statusFilter
        ) {
            when (statusFilter) {
                ArchiveStatusFilter.ALL ->
                    dateFilteredItems

                ArchiveStatusFilter.COMPLETED ->
                    dateFilteredItems.filter { item ->
                        item.isCompleted
                    }

                ArchiveStatusFilter.CANCELLED ->
                    dateFilteredItems.filter { item ->
                        item.isCancelled
                    }
            }
        }

    val calendarMarkers =
        remember(allArchiveItems) {
            KmiCalendarMarkers(
                trainingDates =
                    allArchiveItems
                        .filter { item ->
                            item.isCompleted
                        }
                        .map { item ->
                            item.localDate
                        }
                        .toSet(),
                holidayDates =
                    allArchiveItems
                        .filter { item ->
                            item.isCancelled
                        }
                        .map { item ->
                            item.localDate
                        }
                        .toSet()
            )
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides
                layoutDirection
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = true
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF06152E),
                                Color(0xFF0B2551),
                                Color(0xFF0F5E9C),
                                Color(0xFFEAF6FF)
                            )
                        )
                    )
            ) {
                Scaffold(
                    modifier =
                        Modifier.fillMaxSize(),
                    containerColor =
                        Color.Transparent,
                    contentWindowInsets =
                        androidx.compose.foundation.layout
                            .WindowInsets(0),
                    topBar = {
                        val languageManager =
                            remember(context) {
                                AppLanguageManager(
                                    context
                                )
                            }

                        KmiTopBar(
                            title =
                                if (isEnglish) {
                                    "Training Archive"
                                } else {
                                    "ארכיון אימונים"
                                },
                            currentLang =
                                if (isEnglish) {
                                    "en"
                                } else {
                                    "he"
                                },
                            onHome = onDismiss,
                            onSettings = onSettings,
                            onOpenDrawer =
                                onOpenDrawer,
                            onPickSearchResult =
                                onOpenExercise,
                            onOpenAi = onOpenAi,
                            modePillIsCoach = null,
                            showMenu = true,
                            showFontQuick = true,
                            showRoleStatus = true,
                            showSettings = true,
                            showBottomActions = true,
                            showModePill = true,
                            showRoleBadge = true,
                            showTopHome = true,
                            showTopSearch = true,
                            showTopShare = false,
                            useCloseIcon = false,
                            onBack = null,
                            onToggleLanguage = {
                                val newLanguage =
                                    if (
                                        languageManager
                                            .getCurrentLanguage() ==
                                        AppLanguage.HEBREW
                                    ) {
                                        AppLanguage.ENGLISH
                                    } else {
                                        AppLanguage.HEBREW
                                    }

                                languageManager
                                    .setLanguage(
                                        newLanguage
                                    )

                                (context as? Activity)
                                    ?.recreate()
                            }
                        )
                    }
                ) { topBarPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top =
                                    topBarPadding
                                        .calculateTopPadding() + 30.dp
                            )
                            .navigationBarsPadding()
                    ) {
                        ArchiveFilterPanel(
                            fromDate = fromDate,
                            toDate = toDate,
                            locale = locale,
                            isEnglish = isEnglish,
                            allCount =
                                dateFilteredItems.size,
                            completedCount =
                                completedCount,
                            cancelledCount =
                                cancelledCount,
                            selectedStatusFilter =
                                statusFilter,
                            onStatusFilterSelected = {
                                    selectedFilter ->

                                statusFilter =
                                    if (
                                        statusFilter ==
                                        selectedFilter
                                    ) {
                                        ArchiveStatusFilter.ALL
                                    } else {
                                        selectedFilter
                                    }
                            },
                            onFromDateClick = {
                                showFromDatePicker =
                                    true
                            },
                            onToDateClick = {
                                showToDatePicker =
                                    true
                            },
                            onQuickRangeSelected = {
                                    days ->

                                fromDate =
                                    today.minusDays(
                                        days - 1L
                                    )

                                toDate = today

                                /*
                                 * לאחר בחירת טווח חדש חוזרים
                                 * להצגת כל הסטטוסים.
                                 */
                                statusFilter =
                                    ArchiveStatusFilter.ALL
                            },
                            onReset = {
                                fromDate =
                                    today.minusDays(
                                        89L
                                    )

                                toDate = today

                                statusFilter =
                                    ArchiveStatusFilter.ALL
                            }
                        )

                        if (filteredItems.isEmpty()) {
                            ArchiveEmptyState(
                                isEnglish =
                                    isEnglish,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding =
                                    PaddingValues(
                                        start = 14.dp,
                                        end = 14.dp,
                                        top = 10.dp,
                                        bottom = 24.dp
                                    ),
                                verticalArrangement =
                                    Arrangement.spacedBy(
                                        8.dp
                                    )
                            ) {
                                itemsIndexed(
                                    items = filteredItems,
                                    key = { index, item ->
                                        listOf(
                                            item.training.startMillis,
                                            item.training.endMillis ?: 0L,
                                            item.branch,
                                            item.group,
                                            item.training.place,
                                            item.training.address,
                                            item.training.coach,
                                            index
                                        ).joinToString("|")
                                    }
                                ) { _, item ->
                                    TrainingArchiveCard(
                                        item = item,
                                        isEnglish = isEnglish,
                                        locale = locale
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFromDatePicker) {
            KmiCalendarPickerDialog(
                title =
                    if (isEnglish) {
                        "Select start date"
                    } else {
                        "בחירת תאריך התחלה"
                    },
                selectedDate = fromDate,
                isEnglish = isEnglish,
                markers = calendarMarkers,
                onDismiss = {
                    showFromDatePicker = false
                },
                onDateSelected = { selectedDate ->
                    fromDate = selectedDate

                    if (selectedDate.isAfter(toDate)) {
                        toDate = selectedDate
                    }

                    showFromDatePicker = false
                }
            )
        }

        if (showToDatePicker) {
            KmiCalendarPickerDialog(
                title =
                    if (isEnglish) {
                        "Select end date"
                    } else {
                        "בחירת תאריך סיום"
                    },
                selectedDate = toDate,
                isEnglish = isEnglish,
                markers = calendarMarkers,
                onDismiss = {
                    showToDatePicker = false
                },
                onDateSelected = { selectedDate ->
                    toDate = selectedDate

                    if (selectedDate.isBefore(fromDate)) {
                        fromDate = selectedDate
                    }

                    showToDatePicker = false
                }
            )
        }
    }
}

@Composable
private fun ArchiveFilterPanel(
    fromDate: LocalDate,
    toDate: LocalDate,
    locale: Locale,
    isEnglish: Boolean,
    allCount: Int,
    completedCount: Int,
    cancelledCount: Int,
    selectedStatusFilter: ArchiveStatusFilter,
    onStatusFilterSelected: (ArchiveStatusFilter) -> Unit,
    onFromDateClick: () -> Unit,
    onToDateClick: () -> Unit,
    onQuickRangeSelected: (Long) -> Unit,
    onReset: () -> Unit
) {
    val formatter =
        remember(locale) {
            DateTimeFormatter.ofPattern(
                "dd/MM/yyyy",
                locale
            )
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 7.dp,
        border =
            BorderStroke(
                1.dp,
                Color(0xFF7DD3FC)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 13.dp,
                    vertical = 8.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                ArchiveDateField(
                    title =
                        if (isEnglish) {
                            "From date"
                        } else {
                            "מתאריך"
                        },
                    value =
                        fromDate.format(formatter),
                    onClick = onFromDateClick,
                    modifier = Modifier.weight(1f)
                )

                ArchiveDateField(
                    title =
                        if (isEnglish) {
                            "To date"
                        } else {
                            "עד תאריך"
                        },
                    value =
                        toDate.format(formatter),
                    onClick = onToDateClick,
                    modifier = Modifier.weight(1f)
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp),
                contentPadding =
                    PaddingValues(horizontal = 1.dp)
            ) {
                items(
                    items = archiveQuickRanges,
                    key = { range ->
                        range.days
                    }
                ) { range ->
                    ArchiveQuickRangeChip(
                        title =
                            if (isEnglish) {
                                range.titleEn
                            } else {
                                range.titleHe
                            },
                        onClick = {
                            onQuickRangeSelected(
                                range.days
                            )
                        }
                    )
                }

                item {
                    ArchiveQuickRangeChip(
                        title =
                            if (isEnglish) {
                                "Reset"
                            } else {
                                "איפוס"
                            },
                        onClick = onReset,
                        isReset = true
                    )
                }
            }

            HorizontalDivider(
                color =
                    Color(0xFF0F5E9C).copy(
                        alpha = 0.12f
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                ArchiveSummaryChip(
                    value = allCount,
                    title =
                        if (isEnglish) {
                            "All"
                        } else {
                            "הכול"
                        },
                    color = Color(0xFF0F5E9C),
                    selected =
                        selectedStatusFilter ==
                                ArchiveStatusFilter.ALL,
                    onClick = {
                        onStatusFilterSelected(
                            ArchiveStatusFilter.ALL
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                ArchiveSummaryChip(
                    value = completedCount,
                    title =
                        if (isEnglish) {
                            "Completed"
                        } else {
                            "הסתיימו"
                        },
                    color = Color(0xFF2563EB),
                    selected =
                        selectedStatusFilter ==
                                ArchiveStatusFilter.COMPLETED,
                    onClick = {
                        onStatusFilterSelected(
                            ArchiveStatusFilter.COMPLETED
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                ArchiveSummaryChip(
                    value = cancelledCount,
                    title =
                        if (isEnglish) {
                            "Cancelled"
                        } else {
                            "בוטלו"
                        },
                    color = Color(0xFFB45309),
                    selected =
                        selectedStatusFilter ==
                                ArchiveStatusFilter.CANCELLED,
                    onClick = {
                        onStatusFilterSelected(
                            ArchiveStatusFilter.CANCELLED
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ArchiveDateField(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(17.dp),
        color = Color(0xFFEFF8FF),
        border = BorderStroke(
            width = 1.dp,
            color =
                Color(0xFF38BDF8)
                    .copy(alpha = 0.55f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 7.dp,
                    vertical = 5.dp
                )
        ) {
            Icon(
                imageVector =
                    Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = Color(0xFF0F5E9C),
                modifier = Modifier
                    .size(17.dp)
                    .align(Alignment.CenterEnd)
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(
                        end = 21.dp
                    ),
                verticalArrangement =
                    Arrangement.Center,
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.height(1.dp)
                )

                Text(
                    text = value,
                    style = KmiTypography.body.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = Color(0xFF172033),
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ArchiveQuickRangeChip(
    title: String,
    onClick: () -> Unit,
    isReset: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color =
            if (isReset) {
                Color(0xFFFFF1F2)
            } else {
                Color(0xFFE0F2FE)
            },
        border = BorderStroke(
            width = 1.dp,
            color =
                if (isReset) {
                    Color(0xFFFB7185)
                        .copy(alpha = 0.5f)
                } else {
                    Color(0xFF38BDF8)
                        .copy(alpha = 0.45f)
                }
        )
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
            color =
                if (isReset) {
                    Color(0xFFBE123C)
                } else {
                    Color(0xFF075985)
                },
            style = KmiTypography.caption.copy(
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArchiveSummaryChip(
    value: Int,
    title: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width =
                if (selected) {
                    2.dp
                } else {
                    1.dp
                },
            color =
                if (selected) {
                    color
                } else {
                    Color(0xFFE2E8F0)
                }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 5.dp,
                    vertical = 3.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text = value.toString(),
                style = KmiTypography.cardTitle.copy(
                    fontWeight =
                        if (selected) {
                            FontWeight.Black
                        } else {
                            FontWeight.Bold
                        }
                ),
                color =
                    if (selected) {
                        color
                    } else {
                        Color(0xFF64748B)
                    },
                maxLines = 1
            )

            Text(
                text = title,
                style = KmiTypography.caption.copy(
                    fontWeight =
                        if (selected) {
                            FontWeight.Black
                        } else {
                            FontWeight.Bold
                        }
                ),
                color =
                    if (selected) {
                        color
                    } else {
                        Color(0xFF64748B)
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArchiveEmptyState(
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
            border =
                BorderStroke(
                    1.dp,
                    Color(0xFF7DD3FC)
                )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 28.dp,
                    vertical = 30.dp
                ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(62.dp),
                    shape = CircleShape,
                    color =
                        Color(0xFF0F5E9C)
                            .copy(alpha = 0.11f)
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Filled.History,
                            contentDescription = null,
                            tint = Color(0xFF0F5E9C),
                            modifier =
                                Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text =
                        if (isEnglish) {
                            "No trainings in this range"
                        } else {
                            "אין אימונים בטווח שנבחר"
                        },
                    style = KmiTypography.sectionTitle,
                    color = Color(0xFF172033),
                    textAlign = TextAlign.Center
                )

                Text(
                    text =
                        if (isEnglish) {
                            "Choose another date range or use one of the quick filters."
                        } else {
                            "אפשר לבחור טווח תאריכים אחר או להשתמש באחד מהסינונים המהירים."
                        },
                    style = KmiTypography.body,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TrainingArchiveCard(
    item: TrainingArchiveItem,
    isEnglish: Boolean,
    locale: Locale
) {
    val training = item.training
    val status = item.status

    val dateFormatter =
        remember(locale) {
            SimpleDateFormat(
                "EEEE, dd/MM/yyyy",
                locale
            ).apply {
                timeZone =
                    TimeZone.getTimeZone(
                        "Asia/Jerusalem"
                    )
            }
        }

    val timeFormatter =
        remember(locale) {
            SimpleDateFormat(
                "HH:mm",
                locale
            ).apply {
                timeZone =
                    TimeZone.getTimeZone(
                        "Asia/Jerusalem"
                    )
            }
        }

    val effectiveEndMillis =
        training.endMillis
            ?: training.startMillis

    val dateText =
        dateFormatter.format(
            Date(training.startMillis)
        )

    val timeText =
        buildString {
            append(
                timeFormatter.format(
                    Date(training.startMillis)
                )
            )

            if (
                effectiveEndMillis >
                training.startMillis
            ) {
                append(" – ")

                append(
                    timeFormatter.format(
                        Date(effectiveEndMillis)
                    )
                )
            }
        }

    val statusColor =
        if (status.isCancelled) {
            Color(0xFFB45309)
        } else {
            Color(0xFF2563EB)
        }

    val statusBackground =
        if (status.isCancelled) {
            Color(0xFFFFF7ED)
        } else {
            Color(0xFFEFF6FF)
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.97f),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                statusColor.copy(
                    alpha = 0.24f
                )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color =
                        statusColor.copy(
                            alpha = 0.1f
                        )
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                if (status.isCancelled) {
                                    Icons.Filled.CalendarMonth
                                } else {
                                    Icons.Filled.History
                                },
                            contentDescription = null,
                            tint = statusColor,
                            modifier =
                                Modifier.size(14.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text =
                            training.place.ifBlank {
                                if (isEnglish) {
                                    "Training"
                                } else {
                                    "אימון"
                                }
                            },
                        style = KmiTypography.sectionTitle.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = Color(0xFF172033),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "$dateText · $timeText",
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF334155),
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (training.address.isNotBlank()) {
                ArchiveDetailLine(
                    title =
                        if (isEnglish) {
                            "Location"
                        } else {
                            "מיקום"
                        },
                    value = training.address
                )
            }

            if (training.coach.isNotBlank()) {
                ArchiveDetailLine(
                    title =
                        if (isEnglish) {
                            "Coach"
                        } else {
                            "מאמן"
                        },
                    value = training.coach
                )
            }

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        top = 1.dp
                    ),
                color =
                    statusColor.copy(
                        alpha = 0.12f
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center
            ) {
                Surface(
                    shape =
                        RoundedCornerShape(
                            999.dp
                        ),
                    color = statusBackground,
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            statusColor.copy(
                                alpha = 0.18f
                            )
                    )
                ) {
                    Text(
                        text =
                            status.displayText(
                                isEnglish
                            ),
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Black
                        ),
                        modifier =
                            Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 4.dp
                            ),
                        textAlign = TextAlign.Center,
                        color = statusColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveDetailLine(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(4.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = "$title:",
            style = KmiTypography.secondary.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF64748B),
            maxLines = 1
        )

        Text(
            text = value,
            style = KmiTypography.secondary.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.weight(1f),
            color = Color(0xFF334155),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}