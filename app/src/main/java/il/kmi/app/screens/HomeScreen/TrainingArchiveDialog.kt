package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.LocalContext
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.app.ui.scaledIconSize
import il.yuval.ui.theme.kmiScreenBackgroundBrush
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import il.kmi.app.privacy.DemoPrivacy
import il.kmi.app.training.TrainingData
import il.kmi.app.training.TrainingOverride
import il.kmi.app.training.TrainingOverrideRepository
import il.kmi.app.training.TrainingStatusEngine
import il.kmi.app.ui.calendar.KmiCalendarMarkers
import il.kmi.app.ui.calendar.KmiCalendarPickerDialog
import il.kmi.app.ui.pdf.KmiPdfDirection
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.app.ui.pdf.KmiPdfHeader
import java.io.File
import java.io.FileOutputStream
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
import kotlin.math.ceil


//==============================================================================

private val archiveIsraelZone: ZoneId =
    ZoneId.of("Asia/Jerusalem")

data class TrainingArchiveSource(
    val training: TrainingData,
    val branch: String,
    val group: String
)

/**
 * מעביר למסך הארכיון את מקורות האימונים שכבר
 * חושבו במסך הבית.
 *
 * המאגר אינו נשמר במסד הנתונים ואינו משנה
 * את נתוני האימונים.
 */
object TrainingArchiveNavigationStore {

    private var sources:
            List<TrainingArchiveSource> =
        emptyList()

    fun update(
        value: List<TrainingArchiveSource>
    ) {
        sources =
            value
                .distinctBy { source ->
                    listOf(
                        source.training.startMillis,
                        source.branch,
                        source.group,
                        source.training.place,
                        source.training.address
                    ).joinToString("|")
                }
    }

    fun currentOrFallback(
        fallback: List<TrainingArchiveSource>
    ): List<TrainingArchiveSource> {
        return sources
            .ifEmpty {
                fallback
            }
    }
}

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
fun TrainingArchiveScreen(
    baseTrainings: List<TrainingArchiveSource>,
    isEnglish: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
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

    var activeOverridesByOccurrenceKey by
    remember {
        mutableStateOf<
                Map<String, TrainingOverride>
                >(
            emptyMap()
        )
    }

    var isArchiveLoading by
    remember {
        mutableStateOf(true)
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
        isArchiveLoading = true

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

                        isArchiveLoading = false
                    },
                    onError = {
                        activeOverridesByOccurrenceKey =
                            emptyMap()

                        isArchiveLoading = false
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
        Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                kmiScreenBackgroundBrush()
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
                            onHome = onHome,
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
                            showTopShare = true,
                            onShare = {
                                shareTrainingArchivePdf(
                                    context = context,
                                    items =
                                        filteredItems.map { item ->
                                            item.toPdfItem(
                                                isEnglish = isEnglish,
                                                locale = locale
                                            )
                                        },
                                    fromDate = fromDate,
                                    toDate = toDate,
                                    isEnglish = isEnglish
                                )
                            },
                            useCloseIcon = false,
                            onBack = onBack,
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

                        if (isArchiveLoading) {
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                contentAlignment =
                                    Alignment.Center
                            ) {
                                KmiLoadingRings(
                                    text =
                                        if (isEnglish) {
                                            "Loading training archive..."
                                        } else {
                                            "טוען את ארכיון האימונים..."
                                        }
                                )
                            }
                        } else if (filteredItems.isEmpty()) {
                            ArchiveEmptyState(
                                isEnglish =
                                    isEnglish,
                                modifier =
                                    Modifier
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

private data class TrainingArchivePdfItem(
    val place: String,
    val address: String,
    val coach: String,
    val branch: String,
    val group: String,
    val date: String,
    val time: String,
    val status: String,
    val isCancelled: Boolean
)

private fun archiveCoachDisplayName(
    realName: String,
    isEnglish: Boolean
): String {
    val cleanName = realName.trim()

    if (!DemoPrivacy.isEnabled()) {
        return cleanName
    }

    if (cleanName.isBlank()) {
        return ""
    }

    return if (isEnglish) {
        "Coach"
    } else {
        "מאמן"
    }
}

private fun TrainingArchiveItem.toPdfItem(
    isEnglish: Boolean,
    locale: Locale
): TrainingArchivePdfItem {
    val dateFormatter =
        SimpleDateFormat(
            "dd/MM/yyyy",
            locale
        ).apply {
            timeZone =
                TimeZone.getTimeZone(
                    "Asia/Jerusalem"
                )
        }

    val timeFormatter =
        SimpleDateFormat(
            "HH:mm",
            locale
        ).apply {
            timeZone =
                TimeZone.getTimeZone(
                    "Asia/Jerusalem"
                )
        }

    val startMillis =
        activeOverride
            ?.effectiveStartMillis
            ?: training.startMillis

    val endMillis =
        activeOverride
            ?.effectiveEndMillis
            ?: training.endMillis
            ?: startMillis

    val timeText =
        buildString {
            append(
                timeFormatter.format(
                    Date(startMillis)
                )
            )

            if (endMillis > startMillis) {
                append(" – ")

                append(
                    timeFormatter.format(
                        Date(endMillis)
                    )
                )
            }
        }

    return TrainingArchivePdfItem(
        place =
            training.place.ifBlank {
                if (isEnglish) {
                    "Training"
                } else {
                    "אימון"
                }
            },
        address = training.address.trim(),
        coach =
            archiveCoachDisplayName(
                realName = training.coach,
                isEnglish = isEnglish
            ),
        branch = branch.trim(),
        group = group.trim(),
        date =
            dateFormatter.format(
                Date(startMillis)
            ),
        time = timeText,
        status =
            if (isCancelled) {
                if (isEnglish) {
                    "Cancelled"
                } else {
                    "בוטל"
                }
            } else {
                if (isEnglish) {
                    "Completed"
                } else {
                    "הושלם"
                }
            },
        isCancelled = isCancelled
    )
}

private fun shareTrainingArchivePdf(
    context: Context,
    items: List<TrainingArchivePdfItem>,
    fromDate: LocalDate,
    toDate: LocalDate,
    isEnglish: Boolean
) {
    val pdfFile =
        createTrainingArchivePdf(
            context = context,
            items = items,
            fromDate = fromDate,
            toDate = toDate,
            isEnglish = isEnglish
        )

    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isEnglish) {
                    "Training Archive"
                } else {
                    "ארכיון אימונים"
                }
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
            shareIntent,
            if (isEnglish) {
                "Share PDF"
            } else {
                "שיתוף PDF"
            }
        )
    )
}

private fun createTrainingArchivePdf(
    context: Context,
    items: List<TrainingArchivePdfItem>,
    fromDate: LocalDate,
    toDate: LocalDate,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 26f
    val rowsPerPage = 6

    val totalPages =
        maxOf(
            1,
            ceil(
                items.size.toDouble() /
                        rowsPerPage.toDouble()
            ).toInt()
        )

    val document = PdfDocument()

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

    val textColor =
        android.graphics.Color.rgb(
            23,
            32,
            51
        )

    val secondaryTextColor =
        android.graphics.Color.rgb(
            71,
            84,
            103
        )

    val completedColor =
        android.graphics.Color.rgb(
            103,
            80,
            164
        )

    val cancelledColor =
        android.graphics.Color.rgb(
            186,
            26,
            26
        )

    val cardBackground =
        android.graphics.Color.rgb(
            248,
            251,
            255
        )

    val cardBorder =
        android.graphics.Color.rgb(
            213,
            222,
            229
        )

    fun textPaint(
        size: Float,
        color: Int = textColor,
        bold: Boolean = false
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

            textAlign =
                KmiPdfDirection.textAlign(
                    isEnglish = isEnglish
                )
        }
    }

    val dateFormatter =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy",
            if (isEnglish) {
                Locale.US
            } else {
                Locale("he", "IL")
            }
        )

    val rangeText =
        "${fromDate.format(dateFormatter)} – " +
                toDate.format(dateFormatter)

    for (pageIndex in 0 until totalPages) {
        val pageNumber = pageIndex + 1

        val page =
            document.startPage(
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                ).create()
            )

        val canvas = page.canvas

        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "ארכיון אימונים",
            titleEnglish = "Training Archive",
            subtitleHebrew = "טווח תאריכים: $rangeText",
            subtitleEnglish = "Date range: $rangeText"
        )

        val contentLeft = margin
        val contentRight =
            pageWidth - margin

        val startX =
            KmiPdfDirection.startPaddingX(
                isEnglish = isEnglish,
                left = contentLeft,
                right = contentRight,
                padding = 14f
            )

        var currentTop = 138f

        val pageItems =
            items.drop(
                pageIndex * rowsPerPage
            ).take(
                rowsPerPage
            )

        if (items.isEmpty()) {
            val emptyPaint =
                textPaint(
                    size = 16f,
                    bold = true
                ).apply {
                    textAlign =
                        Paint.Align.CENTER
                }

            canvas.drawText(
                if (isEnglish) {
                    "No trainings were found in the selected range."
                } else {
                    "לא נמצאו אימונים בטווח שנבחר."
                },
                pageWidth / 2f,
                currentTop + 70f,
                emptyPaint
            )
        } else {
            pageItems.forEach { item ->
                val cardBottom =
                    currentTop + 94f

                val backgroundPaint =
                    Paint(
                        Paint.ANTI_ALIAS_FLAG
                    ).apply {
                        color = cardBackground
                        style = Paint.Style.FILL
                    }

                val borderPaint =
                    Paint(
                        Paint.ANTI_ALIAS_FLAG
                    ).apply {
                        color = cardBorder
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    }

                canvas.drawRoundRect(
                    contentLeft,
                    currentTop,
                    contentRight,
                    cardBottom,
                    10f,
                    10f,
                    backgroundPaint
                )

                canvas.drawRoundRect(
                    contentLeft,
                    currentTop,
                    contentRight,
                    cardBottom,
                    10f,
                    10f,
                    borderPaint
                )

                val titlePaint =
                    textPaint(
                        size = 14f,
                        bold = true
                    )

                val detailPaint =
                    textPaint(
                        size = 10.5f,
                        color = secondaryTextColor
                    )

                val statusPaint =
                    textPaint(
                        size = 11f,
                        color =
                            if (item.isCancelled) {
                                cancelledColor
                            } else {
                                completedColor
                            },
                        bold = true
                    )

                canvas.drawText(
                    item.place.take(46),
                    startX,
                    currentTop + 23f,
                    titlePaint
                )

                canvas.drawText(
                    "${item.date} · ${item.time}",
                    startX,
                    currentTop + 42f,
                    detailPaint
                )

                val locationText =
                    listOf(
                        item.branch,
                        item.group,
                        item.address
                    )
                        .filter {
                            it.isNotBlank()
                        }
                        .joinToString(" · ")

                if (locationText.isNotBlank()) {
                    canvas.drawText(
                        locationText.take(72),
                        startX,
                        currentTop + 61f,
                        detailPaint
                    )
                }

                val coachText =
                    if (item.coach.isBlank()) {
                        item.status
                    } else {
                        if (isEnglish) {
                            "Coach: ${item.coach} · ${item.status}"
                        } else {
                            "מאמן: ${item.coach} · ${item.status}"
                        }
                    }

                canvas.drawText(
                    coachText.take(72),
                    startX,
                    currentTop + 80f,
                    statusPaint
                )

                currentTop =
                    cardBottom + 8f
            }
        }

        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = pageNumber,
            totalPages = totalPages,
            isEnglish = isEnglish
        )

        document.finishPage(page)
    }

    val pdfDirectory =
        File(
            context.cacheDir,
            "shared_pdfs"
        ).apply {
            mkdirs()
        }

    val pdfFile =
        File(
            pdfDirectory,
            if (isEnglish) {
                "Training Archive.pdf"
            } else {
                "ארכיון אימונים.pdf"
            }
        )

    FileOutputStream(
        pdfFile,
        false
    ).use { output ->
        document.writeTo(output)
    }

    document.close()

    return pdfFile
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.30f)
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
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.18f)
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
                    color = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.error,
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.35f)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(
                        scaledIconSize(17.dp)
                    )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurface,
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
    val colorScheme =
        MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color =
            if (isReset) {
                colorScheme.errorContainer
                    .copy(alpha = 0.55f)
            } else {
                colorScheme.primaryContainer
                    .copy(alpha = 0.55f)
            },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color =
                if (isReset) {
                    colorScheme.error
                        .copy(alpha = 0.40f)
                } else {
                    colorScheme.primary
                        .copy(alpha = 0.35f)
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
                    colorScheme.onErrorContainer
                } else {
                    colorScheme.onPrimaryContainer
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
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
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
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.28f)
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
                        MaterialTheme.colorScheme.onSurfaceVariant
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
                        MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border =
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.30f)
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
                    modifier = Modifier.size(
                        scaledIconSize(62.dp)
                    ),
                    shape = CircleShape,
                    color =
                        MaterialTheme.colorScheme.primaryContainer
                            .copy(alpha = 0.55f)
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier.size(
                                    scaledIconSize(32.dp)
                                )
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }

    val statusBackground =
        if (status.isCancelled) {
            MaterialTheme.colorScheme.errorContainer
                .copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.primaryContainer
                .copy(alpha = 0.55f)
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
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
                    modifier = Modifier.size(
                        scaledIconSize(26.dp)
                    ),
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
                                Modifier.size(
                                    scaledIconSize(14.dp)
                                )
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "$dateText · $timeText",
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            val displayCoach =
                archiveCoachDisplayName(
                    realName = training.coach,
                    isEnglish = isEnglish
                )

            if (displayCoach.isNotBlank()) {
                ArchiveDetailLine(
                    title =
                        if (isEnglish) {
                            "Coach"
                        } else {
                            "מאמן"
                        },
                    value = displayCoach
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
                        color =
                            if (status.isCancelled) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Text(
            text = value,
            style = KmiTypography.secondary.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}