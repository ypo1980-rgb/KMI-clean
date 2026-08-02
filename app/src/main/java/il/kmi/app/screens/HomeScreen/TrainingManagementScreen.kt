package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.training.TrainingData
import il.kmi.app.training.TrainingOverrideRepository
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class TrainingManagementUiData(
    val occurrenceKey: String,
    val place: String,
    val branch: String,
    val group: String,
    val dateText: String,
    val startTime: String,
    val endTime: String
)

data class TrainingManagementRequest(
    val uiData: TrainingManagementUiData,
    val training: TrainingData,
    val branch: String,
    val group: String,
    val changedByName: String
)

object TrainingManagementNavigationStore {

    var current by mutableStateOf<TrainingManagementRequest?>(null)
        private set

    fun open(request: TrainingManagementRequest) {
        current = request
    }

    fun clear() {
        current = null
    }
}

private fun extractManagementTime(
    rawValue: String
): String {
    return Regex(
        """(?:[01]\d|2[0-3]):[0-5]\d"""
    )
        .find(rawValue)
        ?.value
        ?: rawValue.trim()
}

private fun parseManagementTime(
    rawValue: String
): Pair<Int, Int>? {
    val cleanValue =
        extractManagementTime(rawValue)

    val parts = cleanValue.split(":")

    if (parts.size != 2) {
        return null
    }

    val hour =
        parts[0].toIntOrNull()
            ?: return null

    val minute =
        parts[1].toIntOrNull()
            ?: return null

    return if (
        hour in 0..23 &&
        minute in 0..59
    ) {
        hour to minute
    } else {
        null
    }
}

private suspend fun cancelManagedTraining(
    request: TrainingManagementRequest,
    reason: String,
    isEnglish: Boolean
): Result<Unit> {
    return suspendCoroutine { continuation ->
        TrainingOverrideRepository.cancelTraining(
            training = request.training,
            branch = request.branch,
            group = request.group,
            reason = reason,
            changedByName = request.changedByName,
            onResult = { success, error ->
                continuation.resume(
                    if (success) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            error ?: IllegalStateException(
                                if (isEnglish) {
                                    "The training could not be cancelled."
                                } else {
                                    "לא ניתן היה לבטל את האימון."
                                }
                            )
                        )
                    }
                )
            }
        )
    }
}

private suspend fun changeManagedTrainingTime(
    request: TrainingManagementRequest,
    startTime: String,
    endTime: String,
    reason: String,
    isEnglish: Boolean
): Result<Unit> {
    val parsedStart =
        parseManagementTime(startTime)
            ?: return Result.failure(
                IllegalArgumentException(
                    if (isEnglish) {
                        "Enter a valid start time."
                    } else {
                        "יש להזין שעת התחלה תקינה."
                    }
                )
            )

    val parsedEnd =
        parseManagementTime(endTime)
            ?: return Result.failure(
                IllegalArgumentException(
                    if (isEnglish) {
                        "Enter a valid end time."
                    } else {
                        "יש להזין שעת סיום תקינה."
                    }
                )
            )

    val newStartCalendar =
        (request.training.cal.clone() as Calendar).apply {
            set(
                Calendar.HOUR_OF_DAY,
                parsedStart.first
            )
            set(
                Calendar.MINUTE,
                parsedStart.second
            )
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    val newEndCalendar =
        (request.training.cal.clone() as Calendar).apply {
            set(
                Calendar.HOUR_OF_DAY,
                parsedEnd.first
            )
            set(
                Calendar.MINUTE,
                parsedEnd.second
            )
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    if (
        newEndCalendar.timeInMillis <=
        newStartCalendar.timeInMillis
    ) {
        newEndCalendar.add(
            Calendar.DAY_OF_YEAR,
            1
        )
    }

    return suspendCoroutine { continuation ->
        TrainingOverrideRepository.changeTrainingTime(
            training = request.training,
            branch = request.branch,
            group = request.group,
            newStartMillis =
                newStartCalendar.timeInMillis,
            newEndMillis =
                newEndCalendar.timeInMillis,
            reason = reason,
            changedByName = request.changedByName,
            onResult = { success, error ->
                continuation.resume(
                    if (success) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            error ?: IllegalStateException(
                                if (isEnglish) {
                                    "The new training time could not be saved."
                                } else {
                                    "לא ניתן היה לשמור את שעת האימון החדשה."
                                }
                            )
                        )
                    }
                )
            }
        )
    }
}

@Composable
fun TrainingManagementRoute(
    request: TrainingManagementRequest,
    isEnglish: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenVoiceCommands: () -> Unit,
    onOpenAi: () -> Unit
) {
    TrainingManagementScreen(
        training = request.uiData,
        isEnglish = isEnglish,
        onBack = onBack,
        onHome = onHome,
        onOpenDrawer = onOpenDrawer,
        onOpenVoiceCommands = onOpenVoiceCommands,
        onOpenAi = onOpenAi,
        onCancelTraining = { reason ->
            cancelManagedTraining(
                request = request,
                reason = reason,
                isEnglish = isEnglish
            )
        },
        onChangeTrainingTime = {
                startTime,
                endTime,
                reason ->

            changeManagedTrainingTime(
                request = request,
                startTime = startTime,
                endTime = endTime,
                reason = reason,
                isEnglish = isEnglish
            )
        }
    )
}

enum class TrainingManagementScreenMode {
    MENU,
    CHANGE_TIME,
    CANCEL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingManagementScreen(
    training: TrainingManagementUiData,
    isEnglish: Boolean,
    initialMode: TrainingManagementScreenMode =
        TrainingManagementScreenMode.MENU,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenVoiceCommands: () -> Unit,
    onOpenAi: () -> Unit,
    onCancelTraining: suspend (
        reason: String
    ) -> Result<Unit>,
    onChangeTrainingTime: suspend (
        startTime: String,
        endTime: String,
        reason: String
    ) -> Result<Unit>
) {
    var mode by rememberSaveable {
        mutableStateOf(initialMode)
    }

    var reason by rememberSaveable {
        mutableStateOf("")
    }

    var changedStartTime by rememberSaveable(
        training.startTime
    ) {
        mutableStateOf(
            extractManagementTime(
                training.startTime
            )
        )
    }

    var changedEndTime by rememberSaveable(
        training.endTime
    ) {
        mutableStateOf(
            extractManagementTime(
                training.endTime
            )
        )
    }

    LaunchedEffect(
        training.startTime,
        training.endTime
    ) {
        val start =
            parseManagementTime(
                training.startTime
            )

        val end =
            parseManagementTime(
                training.endTime
            )

        if (
            start != null &&
            end != null
        ) {
            val startMinutes =
                start.first * 60 +
                        start.second

            val endMinutes =
                end.first * 60 +
                        end.second

            /*
             * מתקנים רק נתונים שנשלחו הפוך למסך.
             *
             * לא הופכים אימון אמיתי שחוצה חצות,
             * למשל 23:00–01:00.
             */
            val probablyReversed =
                startMinutes > endMinutes &&
                        startMinutes - endMinutes < 12 * 60

            if (probablyReversed) {
                changedStartTime =
                    extractManagementTime(
                        training.endTime
                    )

                changedEndTime =
                    extractManagementTime(
                        training.startTime
                    )
            } else {
                changedStartTime =
                    extractManagementTime(
                        training.startTime
                    )

                changedEndTime =
                    extractManagementTime(
                        training.endTime
                    )
            }
        }
    }

    var showStartTimePicker by rememberSaveable {
        mutableStateOf(false)
    }

    var showEndTimePicker by rememberSaveable {
        mutableStateOf(false)
    }

    var isSaving by rememberSaveable {
        mutableStateOf(false)
    }

    var errorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    val accent = when (mode) {
        TrainingManagementScreenMode.MENU ->
            Color(0xFF075985)

        TrainingManagementScreenMode.CHANGE_TIME ->
            Color(0xFF6D4BB6)

        TrainingManagementScreenMode.CANCEL ->
            Color(0xFFB91C1C)
    }

    val title = when (mode) {
        TrainingManagementScreenMode.MENU ->
            if (isEnglish) "Manage training" else "ניהול אימון"

        TrainingManagementScreenMode.CHANGE_TIME ->
            if (isEnglish) "Change training time" else "שינוי שעת האימון"

        TrainingManagementScreenMode.CANCEL ->
            if (isEnglish) "Cancel training" else "ביטול אימון"
    }

    val timeRegex = remember {
        Regex("""^(?:[01]\d|2[0-3]):[0-5]\d$""")
    }

    val timesAreValid =
        changedStartTime.matches(timeRegex) &&
                changedEndTime.matches(timeRegex)

    val canSubmit = when (mode) {
        TrainingManagementScreenMode.MENU ->
            false

        TrainingManagementScreenMode.CHANGE_TIME ->
            timesAreValid && reason.trim().length >= 3

        TrainingManagementScreenMode.CANCEL ->
            reason.trim().length >= 3
    }

    fun returnToMenu() {
        mode = TrainingManagementScreenMode.MENU
        reason = ""

        changedStartTime =
            extractManagementTime(
                training.startTime
            )

        changedEndTime =
            extractManagementTime(
                training.endTime
            )

        errorMessage = null
    }

    if (showStartTimePicker) {
        TrainingManagementTimePickerDialog(
            initialTime = changedStartTime,
            title =
                if (isEnglish) {
                    "Select start time"
                } else {
                    "בחירת שעת התחלה"
                },
            isEnglish = isEnglish,
            onDismiss = {
                showStartTimePicker = false
            },
            onTimeSelected = {
                changedStartTime = it
                errorMessage = null
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TrainingManagementTimePickerDialog(
            initialTime = changedEndTime,
            title =
                if (isEnglish) {
                    "Select end time"
                } else {
                    "בחירת שעת סיום"
                },
            isEnglish = isEnglish,
            onDismiss = {
                showEndTimePicker = false
            },
            onTimeSelected = {
                changedEndTime = it
                errorMessage = null
                showEndTimePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            KmiTopBar(
                title = title,
                onHome = {
                    if (!isSaving) {
                        onHome()
                    }
                },
                onOpenDrawer = {
                    if (!isSaving) {
                        onOpenDrawer()
                    }
                },
                onOpenVoiceCommands = {
                    if (!isSaving) {
                        onOpenVoiceCommands()
                    }
                },
                onOpenAi = {
                    if (!isSaving) {
                        onOpenAi()
                    }
                },
                currentLang = if (isEnglish) "en" else "he",
                showTopHome = false,
                showTopShare = false,
                showBottomActions = true,
                centerTitle = true
            )
        },
        containerColor = colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.background,
                            colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            colorScheme.background
                        )
                    )
                )
                .imePadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = 20.dp,
                    vertical = 14.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TrainingDetailsCard(
                    training = training,
                    isEnglish = isEnglish,
                    accent = accent
                )

                Text(
                    text =
                        if (isEnglish) {
                            "Choose the action you want to perform"
                        } else {
                            "בחר את הפעולה שברצונך לבצע"
                        },
                    style = KmiTypography.sectionTitle,
                    color = colorScheme.onBackground,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                    modifier = Modifier.fillMaxWidth()
                )

                when (mode) {
                    TrainingManagementScreenMode.MENU -> {
                        TrainingManagementActionCard(
                            title =
                                if (isEnglish) {
                                    "Change training time"
                                } else {
                                    "שינוי שעת אימון"
                                },
                            subtitle =
                                if (isEnglish) {
                                    "Choose new start and end times"
                                } else {
                                    "בחירת שעת התחלה וסיום חדשות"
                                },
                            iconTint = Color(0xFF6D4BB6),
                            containerColor =
                                Color(0xFFF0EBFF),
                            borderColor =
                                Color(0xFFB69AF5),
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = {
                                errorMessage = null
                                mode =
                                    TrainingManagementScreenMode.CHANGE_TIME
                            }
                        )

                        TrainingManagementActionCard(
                            title =
                                if (isEnglish) {
                                    "Cancel training"
                                } else {
                                    "ביטול אימון"
                                },
                            subtitle =
                                if (isEnglish) {
                                    "Cancel and notify the trainees"
                                } else {
                                    "ביטול האימון ושליחת עדכון למתאמנים"
                                },
                            iconTint = Color(0xFFC81E1E),
                            containerColor =
                                Color(0xFFFFEEEE),
                            borderColor =
                                Color(0xFFFF9A9A),
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = {
                                errorMessage = null
                                mode =
                                    TrainingManagementScreenMode.CANCEL
                            }
                        )
                    }

                    TrainingManagementScreenMode.CHANGE_TIME -> {
                        TrainingTimeEditor(
                            startTime = changedStartTime,
                            endTime = changedEndTime,
                            isEnglish = isEnglish,
                            accent = accent,
                            onStartClick = {
                                showStartTimePicker = true
                            },
                            onEndClick = {
                                showEndTimePicker = true
                            }
                        )

                        TrainingReasonField(
                            value = reason,
                            isEnglish = isEnglish,
                            isCancellation = false,
                            accent = accent,
                            onValueChange = {
                                reason = it.take(250)
                                errorMessage = null
                            }
                        )
                    }

                    TrainingManagementScreenMode.CANCEL -> {
                        TrainingReasonField(
                            value = reason,
                            isEnglish = isEnglish,
                            isCancellation = true,
                            accent = accent,
                            onValueChange = {
                                reason = it.take(250)
                                errorMessage = null
                            }
                        )
                    }
                }

                errorMessage?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.errorContainer
                    ) {
                        Text(
                            text = it,
                            color = colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            TrainingManagementBottomActions(
                mode = mode,
                isEnglish = isEnglish,
                accent = accent,
                isSaving = isSaving,
                canSubmit = canSubmit,
                onClose = onBack,
                onReturnToMenu = {
                    if (!isSaving) {
                        returnToMenu()
                    }
                },
                onSubmit = {
                    if (!canSubmit || isSaving) {
                        return@TrainingManagementBottomActions
                    }

                    isSaving = true
                    errorMessage = null

                    scope.launch {
                        val result =
                            when (mode) {
                                TrainingManagementScreenMode.CANCEL ->
                                    onCancelTraining(reason.trim())

                                TrainingManagementScreenMode.CHANGE_TIME ->
                                    onChangeTrainingTime(
                                        changedStartTime,
                                        changedEndTime,
                                        reason.trim()
                                    )

                                TrainingManagementScreenMode.MENU ->
                                    Result.success(Unit)
                            }

                        isSaving = false

                        result.fold(
                            onSuccess = {
                                onBack()
                            },
                            onFailure = {
                                errorMessage =
                                    it.localizedMessage
                                        ?.takeIf(String::isNotBlank)
                                        ?: if (isEnglish) {
                                            "The change could not be saved."
                                        } else {
                                            "לא ניתן היה לשמור את השינוי."
                                        }
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun TrainingDetailsCard(
    training: TrainingManagementUiData,
    isEnglish: Boolean,
    accent: Color
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colorScheme.surface,
        border = BorderStroke(
            1.dp,
            colorScheme.outline.copy(alpha = 0.30f)
        ),
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 13.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = training.place.ifBlank { training.branch },
                style = KmiTypography.cardTitle,
                color = colorScheme.onSurface,
                textAlign =
                    if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text =
                    if (isEnglish) {
                        "Branch: ${training.branch}"
                    } else {
                        "סניף: ${training.branch}"
                    },
                style = KmiTypography.body,
                color = colorScheme.onSurfaceVariant,
                textAlign =
                    if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text =
                    if (isEnglish) {
                        "Group: ${training.group}"
                    } else {
                        "קבוצה: ${training.group}"
                    },
                style = KmiTypography.body,
                color = colorScheme.onSurfaceVariant,
                textAlign =
                    if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 3.dp),
                color = colorScheme.outline.copy(alpha = 0.18f)
            )

            val displayedDates =
                Regex("""\d{2}/\d{2}/\d{4}""")
                    .findAll(training.dateText)
                    .map { match ->
                        match.value
                    }
                    .distinct()
                    .toList()

            val cleanDate =
                when {
                    displayedDates.isNotEmpty() ->
                        displayedDates.joinToString(" • ")

                    else ->
                        training.dateText
                            .trim()
                            .trim('•', '·', '-', '–')
                            .trim()
                }

            val rawStartTime =
                extractManagementTime(
                    training.startTime
                )

            val rawEndTime =
                extractManagementTime(
                    training.endTime
                )

            val parsedStartTime =
                parseManagementTime(
                    rawStartTime
                )

            val parsedEndTime =
                parseManagementTime(
                    rawEndTime
                )

            val shouldSwapTimes =
                if (
                    parsedStartTime != null &&
                    parsedEndTime != null
                ) {
                    val startMinutes =
                        parsedStartTime.first * 60 +
                                parsedStartTime.second

                    val endMinutes =
                        parsedEndTime.first * 60 +
                                parsedEndTime.second

                    startMinutes > endMinutes &&
                            startMinutes - endMinutes < 12 * 60
                } else {
                    false
                }

            val displayedStartTime =
                if (shouldSwapTimes) {
                    rawEndTime
                } else {
                    rawStartTime
                }

            val displayedEndTime =
                if (shouldSwapTimes) {
                    rawStartTime
                } else {
                    rawEndTime
                }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment =
                    if (isEnglish) {
                        Alignment.Start
                    } else {
                        Alignment.End
                    }
            ) {

                Text(
                    text = cleanDate,
                    style = KmiTypography.cardTitle,
                    color = accent,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text =
                        "$displayedStartTime–$displayedEndTime",
                    style = KmiTypography.cardTitle,
                    color = accent,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TrainingManagementActionCard(
    title: String,
    subtitle: String,
    iconTint: Color,
    containerColor: Color,
    borderColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = iconTint
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 21.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = iconTint,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    style = KmiTypography.body,
                    color = iconTint.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TrainingTimeEditor(
    startTime: String,
    endTime: String,
    isEnglish: Boolean,
    accent: Color,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TrainingTimeButton(
            label = if (isEnglish) "Start" else "התחלה",
            time = startTime,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = onStartClick
        )

        TrainingTimeButton(
            label = if (isEnglish) "End" else "סיום",
            time = endTime,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = onEndClick
        )
    }
}

@Composable
private fun TrainingTimeButton(
    label: String,
    time: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.38f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = KmiTypography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = time,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                color = accent
            )
        }
    }
}

@Composable
private fun TrainingReasonField(
    value: String,
    isEnglish: Boolean,
    isCancellation: Boolean,
    accent: Color,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                if (isCancellation) {
                    if (isEnglish) {
                        "Cancellation reason"
                    } else {
                        "סיבת הביטול"
                    }
                } else {
                    if (isEnglish) {
                        "Reason for the change"
                    } else {
                        "סיבת השינוי"
                    }
                }
            )
        },
        placeholder = {
            Text(
                if (isEnglish) {
                    "Enter at least 3 characters"
                } else {
                    "יש להזין לפחות 3 תווים"
                }
            )
        },
        minLines = 3,
        maxLines = 5,
        supportingText = {
            Text("${value.length}/250")
        },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            focusedLabelColor = accent,
            cursorColor = accent,
            focusedContainerColor =
                MaterialTheme.colorScheme.surface,
            unfocusedContainerColor =
                MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun TrainingManagementBottomActions(
    mode: TrainingManagementScreenMode,
    isEnglish: Boolean,
    accent: Color,
    isSaving: Boolean,
    canSubmit: Boolean,
    onClose: () -> Unit,
    onReturnToMenu: () -> Unit,
    onSubmit: () -> Unit
) {
    if (mode == TrainingManagementScreenMode.MENU) {
        Surface(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(999.dp),
            color = accent,
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.30f)
            ),
            shadowElevation = 7.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEnglish) "Close" else "סגור",
                    style = KmiTypography.action,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            onClick = onReturnToMenu,
            modifier = Modifier
                .weight(0.36f)
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = accent.copy(alpha = 0.10f),
            border = BorderStroke(
                1.dp,
                accent.copy(alpha = 0.30f)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = if (isEnglish) "Back" else "חזרה",
                        color = accent,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Button(
            onClick = onSubmit,
            enabled = canSubmit && !isSaving,
            modifier = Modifier
                .weight(0.64f)
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(
                horizontal = 10.dp,
                vertical = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                disabledContainerColor =
                    accent.copy(alpha = 0.35f)
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )

                Spacer(Modifier.width(6.dp))
            }

            Text(
                text =
                    when (mode) {
                        TrainingManagementScreenMode.CANCEL ->
                            if (isEnglish) {
                                "Confirm cancellation"
                            } else {
                                "אישור ביטול"
                            }

                        TrainingManagementScreenMode.CHANGE_TIME ->
                            if (isEnglish) {
                                "Save new time"
                            } else {
                                "שמירת שעה"
                            }

                        TrainingManagementScreenMode.MENU ->
                            ""
                    },
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingManagementTimePickerDialog(
    initialTime: String,
    title: String,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val initialParts = remember(initialTime) {
        initialTime.split(":")
    }

    val initialHour =
        initialParts.getOrNull(0)
            ?.toIntOrNull()
            ?.coerceIn(0, 23)
            ?: 19

    val initialMinute =
        initialParts.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceIn(0, 59)
            ?: 0

    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = KmiTypography.sectionTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(
                        String.format(
                            Locale.US,
                            "%02d:%02d",
                            state.hour,
                            state.minute
                        )
                    )
                }
            ) {
                Text(
                    text = if (isEnglish) "Select" else "בחירה",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isEnglish) "Cancel" else "ביטול"
                )
            }
        },
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}