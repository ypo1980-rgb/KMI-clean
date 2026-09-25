package il.kmi.app.stretching.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.stretching.StretchingCatalog
import il.kmi.shared.stretching.StretchingExercise
import il.kmi.shared.stretching.StretchingStepType
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBackground
import kotlinx.coroutines.delay

@Composable
fun StretchingExerciseScreen(
    exerciseId: String,
    isEnglish: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    val exercise =
        StretchingCatalog.exerciseById(exerciseId)

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                KmiTopBar(
                    title =
                        if (isEnglish) {
                            "Stretching"
                        } else {
                            "מתיחות"
                        },
                    onBack = onBack,
                    onHome = onHome,
                    currentLang =
                        if (isEnglish) {
                            "en"
                        } else {
                            "he"
                        },
                    showMenu = true,
                    showTopSearch = false,
                    showTopShare = false,
                    showBottomHelp = false,
                    showBottomShare = false,
                    showRoleStatus = false,
                    showRoleBadge = false,
                    showModePill = false,
                    showCoachBroadcastFab = false,
                    titleMaxLines = 1,
                    titleScale = 0.82f
                )
            }
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                kmiScreenBackgroundBrush()
                        )
                        .padding(innerPadding)
            ) {
                if (exercise == null) {
                    MissingStretchingExercise(
                        isEnglish = isEnglish
                    )
                } else {
                    StretchingExerciseContent(
                        exercise = exercise,
                        isEnglish = isEnglish
                    )
                }
            }
        }
    }
}

@Composable
private fun StretchingExerciseContent(
    exercise: StretchingExercise,
    isEnglish: Boolean
) {
    val context = LocalContext.current
    val visualSteps = exercise.visualSteps
    val hasVisualSteps = visualSteps.isNotEmpty()

    var isMuted by rememberSaveable {
        mutableStateOf(false)
    }

    DisposableEffect(context) {
        KmiTtsManager.init(context)

        isMuted =
            KmiTtsManager.isMuted()

        onDispose {
            KmiTtsManager.stop()
        }
    }

    var currentStepIndex by rememberSaveable(exercise.id) {
        mutableIntStateOf(0)
    }

    var currentRepetition by rememberSaveable(exercise.id) {
        mutableIntStateOf(1)
    }

    val totalRepetitions =
        exercise.repetitions ?: 1

    val currentStep =
        visualSteps.getOrNull(currentStepIndex)

    val currentDurationSeconds =
        currentStep?.durationSeconds
            ?: exercise.durationSeconds

    var remainingSeconds by rememberSaveable(exercise.id) {
        mutableIntStateOf(currentDurationSeconds)
    }

    var isRunning by rememberSaveable(exercise.id) {
        mutableStateOf(false)
    }

    var isVoiceCueCompleted by rememberSaveable(exercise.id) {
        mutableStateOf(false)
    }

    LaunchedEffect(exercise.id) {
        currentStepIndex = 0
        currentRepetition = 1
        remainingSeconds =
            visualSteps
                .firstOrNull()
                ?.durationSeconds
                ?: exercise.durationSeconds
        isVoiceCueCompleted = false
        isRunning = false
    }

    LaunchedEffect(
        isRunning,
        isVoiceCueCompleted,
        currentStepIndex,
        currentRepetition,
        exercise.id,
        isEnglish
    ) {
        if (!isRunning) return@LaunchedEffect

        if (
            hasVisualSteps &&
            !isVoiceCueCompleted
        ) {
            return@LaunchedEffect
        }

        while (
            isRunning &&
            remainingSeconds > 0
        ) {
            delay(1_000L)

            if (
                isRunning &&
                remainingSeconds > 0
            ) {
                remainingSeconds -= 1
            }
        }

        if (
            isRunning &&
            remainingSeconds <= 0
        ) {
            val nextStepIndex =
                currentStepIndex + 1

            if (
                hasVisualSteps &&
                nextStepIndex < visualSteps.size
            ) {
                isVoiceCueCompleted = false
                currentStepIndex = nextStepIndex
                remainingSeconds =
                    visualSteps[
                        nextStepIndex
                    ].durationSeconds
            } else if (
                hasVisualSteps &&
                currentRepetition < totalRepetitions
            ) {
                isVoiceCueCompleted = false
                currentRepetition += 1
                currentStepIndex = 0
                remainingSeconds =
                    visualSteps
                        .first()
                        .durationSeconds
            } else {
                isRunning = false
            }
        }
    }

    LaunchedEffect(
        isRunning,
        currentStepIndex,
        currentRepetition,
        exercise.id,
        isEnglish
    ) {
        if (!hasVisualSteps) {
            isVoiceCueCompleted = true
            KmiTtsManager.stop()
            return@LaunchedEffect
        }

        if (!isRunning) {
            KmiTtsManager.stop()
            isVoiceCueCompleted = false

            val exerciseCompleted =
                remainingSeconds <= 0 &&
                        currentStepIndex == visualSteps.lastIndex &&
                        currentRepetition >= totalRepetitions

            if (exerciseCompleted) {
                KmiTtsManager.speak(
                    if (isEnglish) {
                        "Exercise completed"
                    } else {
                        "התרגיל הסתיים."
                    }
                )
            }

            return@LaunchedEffect
        }

        val activeStep =
            visualSteps.getOrNull(currentStepIndex)
                ?: return@LaunchedEffect

        isVoiceCueCompleted = false

        val stepVoiceCue =
            activeStep.displayInstruction(
                isEnglish = isEnglish
            )

        val voiceCue =
            if (
                activeStep.type ==
                StretchingStepType.PREPARE &&
                currentRepetition > 1
            ) {
                if (isEnglish) {
                    "Starting again. $stepVoiceCue"
                } else {
                    "מתחילים שוב. $stepVoiceCue"
                }
            } else {
                stepVoiceCue
            }

        KmiTtsManager.setOnSpeechCompletedListener {
            isVoiceCueCompleted = true
        }

        KmiTtsManager.speak(voiceCue)
    }

    val displayedImageKey =
        currentStep?.imageKey
            ?: exercise.imageKey

    val stepInstruction =
        currentStep?.displayInstruction(
            isEnglish = isEnglish
        )

    val stepPositionText =
        if (hasVisualSteps) {
            if (isEnglish) {
                "Step ${currentStepIndex + 1} of ${visualSteps.size} · " +
                        "Repetition $currentRepetition of $totalRepetitions"
            } else {
                "שלב ${currentStepIndex + 1} מתוך ${visualSteps.size} · " +
                        "חזרה $currentRepetition מתוך $totalRepetitions"
            }
        } else {
            null
        }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        StretchingExerciseChapterHeader(
            exercise = exercise,
            isEnglish = isEnglish
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(
                        state = rememberScrollState()
                    )
                    .navigationBarsPadding()
                    .padding(
                        start = 14.dp,
                        top = 10.dp,
                        end = 14.dp,
                        bottom = 24.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            StretchingExerciseHeroCard(
            exercise = exercise,
            isEnglish = isEnglish,
            imageKey = displayedImageKey,
            remainingSeconds = remainingSeconds,
            stepInstruction = stepInstruction,
            stepPositionText = stepPositionText
        )

        StretchingTimerCard(
            remainingSeconds = remainingSeconds,
            totalSeconds = currentDurationSeconds,
            isRunning = isRunning,
            isMuted = isMuted,
            isEnglish = isEnglish,
            onToggleRunning = {
                val isExerciseCompleted =
                    remainingSeconds <= 0 &&
                            (
                                    !hasVisualSteps ||
                                            currentStepIndex ==
                                            visualSteps.lastIndex
                                    )

                if (isExerciseCompleted) {
                    currentStepIndex = 0
                    currentRepetition = 1
                    remainingSeconds =
                        visualSteps
                            .firstOrNull()
                            ?.durationSeconds
                            ?: exercise.durationSeconds
                }

                if (!isRunning) {
                    isVoiceCueCompleted = false
                }

                isRunning = !isRunning
            },
            onToggleMuted = {
                isMuted =
                    KmiTtsManager.toggleMuted()
            },
            onReset = {
                KmiTtsManager.stop()
                isVoiceCueCompleted = false
                isRunning = false
                currentStepIndex = 0
                currentRepetition = 1
                remainingSeconds =
                    visualSteps
                        .firstOrNull()
                        ?.durationSeconds
                        ?: exercise.durationSeconds
            }
        )

        StretchingInstructionsCard(
            exercise = exercise,
            isEnglish = isEnglish
        )

            StretchingSafetyCard(
                exercise = exercise,
                isEnglish = isEnglish
            )
        }
    }
}

@Composable
private fun StretchingExerciseChapterHeader(
    exercise: StretchingExercise,
    isEnglish: Boolean
) {
    val categoryTitle =
        exercise.category.displayTitle(
            isEnglish = isEnglish
        )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .kmiSectionHeaderBackground()
                .padding(
                    horizontal = 14.dp,
                    vertical = 7.dp
                ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text =
                    if (isEnglish) {
                        "$categoryTitle stretches"
                    } else {
                        "מתיחות $categoryTitle"
                    },
                modifier = Modifier.fillMaxWidth(),
                style = KmiTypography.action,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text =
                    exerciseTitle(
                        exercise = exercise,
                        isEnglish = isEnglish
                    ),
                modifier = Modifier.fillMaxWidth(),
                style = KmiTypography.caption,
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StretchingExerciseHeroCard(
    exercise: StretchingExercise,
    isEnglish: Boolean,
    imageKey: String,
    remainingSeconds: Int,
    stepInstruction: String?,
    stepPositionText: String?
) {
    val imageResource =
        stretchingImageResource(
            imageKey = imageKey
        )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.96f),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.42f)
            ),
        shadowElevation = 0.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 15.dp
                    ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                shape = RoundedCornerShape(19.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                        .copy(alpha = 0.42f),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(alpha = 0.28f)
                    ),
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = imageResource,
                        animationSpec =
                            tween(
                                durationMillis = 140
                            ),
                        label = "stretchingStepImage"
                    ) { targetImageResource ->
                        if (targetImageResource != null) {
                            Image(
                                painter =
                                    painterResource(
                                        id = targetImageResource
                                    ),
                                contentDescription =
                                    exerciseTitle(
                                        exercise = exercise,
                                        isEnglish = isEnglish
                                    ),
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(5.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.Center
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Filled.AccessibilityNew,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(
                                            KmiIconSize.large
                                        ),
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )

                                Spacer(
                                    modifier = Modifier.height(7.dp)
                                )

                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Illustration will be added"
                                        } else {
                                            "תמונת המחשה תתווסף"
                                        },
                                    style = KmiTypography.caption,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    CompositionLocalProvider(
                        LocalLayoutDirection provides
                                LayoutDirection.Ltr
                    ) {
                        Surface(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(9.dp)
                                    .size(56.dp),
                            shape = CircleShape,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surface
                                    .copy(alpha = 0.94f),
                            border =
                                BorderStroke(
                                    width = 2.dp,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                            .copy(alpha = 0.75f)
                                ),
                            shadowElevation = 0.dp,
                            tonalElevation = 2.dp
                        ) {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize(),
                                contentAlignment =
                                    Alignment.Center
                            ) {
                                Text(
                                    text =
                                        timerText(
                                            seconds =
                                                remainingSeconds
                                        ),
                                    style =
                                        KmiTypography.body,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    fontWeight =
                                        FontWeight.ExtraBold,
                                    textAlign =
                                        TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (
                stepPositionText != null &&
                stepInstruction != null
            ) {
                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Text(
                    text = stepPositionText,
                    modifier = Modifier.fillMaxWidth(),
                    style = KmiTypography.caption,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = stepInstruction,
                    modifier = Modifier.fillMaxWidth(),
                    style = KmiTypography.body,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(
                modifier = Modifier.height(9.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExerciseInformationBadge(
                    text =
                        durationLabel(
                            seconds =
                                exercise.durationSeconds,
                            isEnglish = isEnglish
                        )
                )

                exercise.repetitions?.let { repetitions ->
                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    ExerciseInformationBadge(
                        text =
                            if (isEnglish) {
                                "$repetitions reps"
                            } else {
                                "$repetitions חזרות"
                            }
                    )
                }

                if (exercise.performBothSides) {
                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    ExerciseInformationBadge(
                        text =
                            if (isEnglish) {
                                "Both sides"
                            } else {
                                "שני הצדדים"
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseInformationBadge(
    text: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(alpha = 0.9f),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
            ),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier =
                Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 4.dp
                ),
            style = KmiTypography.caption,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun StretchingTimerCard(
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    isMuted: Boolean,
    isEnglish: Boolean,
    onToggleRunning: () -> Unit,
    onToggleMuted: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.96f),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                        .copy(alpha = 0.8f)
            ),
        shadowElevation = 0.dp,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 9.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text =
                    when {
                        remainingSeconds <= 0 -> {
                            if (isEnglish) {
                                "Completed"
                            } else {
                                "התרגיל הושלם"
                            }
                        }

                        isRunning -> {
                            if (isEnglish) {
                                "Timer running"
                            } else {
                                "הטיימר פועל"
                            }
                        }

                        remainingSeconds < totalSeconds -> {
                            if (isEnglish) {
                                "Timer paused"
                            } else {
                                "הטיימר מושהה"
                            }
                        }

                        else -> {
                            if (isEnglish) {
                                "Ready to begin"
                            } else {
                                "מוכנים להתחיל"
                            }
                        }
                    },
                modifier = Modifier.fillMaxWidth(),
                style = KmiTypography.caption,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleRunning,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding =
                        PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .primary,
                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary
                        )
                ) {
                    Icon(
                        imageVector =
                            if (isRunning) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                        contentDescription = null,
                        modifier =
                            Modifier.size(
                                KmiIconSize.small
                            )
                    )

                    Spacer(
                        modifier = Modifier.width(5.dp)
                    )

                    Text(
                        text =
                            if (isRunning) {
                                if (isEnglish) {
                                    "Pause"
                                } else {
                                    "השהה"
                                }
                            } else {
                                if (isEnglish) {
                                    "Start"
                                } else {
                                    "התחל"
                                }
                            },
                        style = KmiTypography.caption,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                OutlinedButton(
                    onClick = onToggleMuted,
                    modifier =
                        Modifier.size(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding =
                        PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector =
                            if (isMuted) {
                                Icons.Filled.VolumeOff
                            } else {
                                Icons.Filled.VolumeUp
                            },
                        contentDescription =
                            if (isMuted) {
                                if (isEnglish) {
                                    "Enable voice guidance"
                                } else {
                                    "הפעלת ההנחיה הקולית"
                                }
                            } else {
                                if (isEnglish) {
                                    "Mute voice guidance"
                                } else {
                                    "השתקת ההנחיה הקולית"
                                }
                            },
                        modifier =
                            Modifier.size(
                                KmiIconSize.small
                            ),
                        tint =
                            if (isMuted) {
                                MaterialTheme
                                    .colorScheme
                                    .error
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .primary
                            }
                    )
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier =
                        Modifier.size(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding =
                        PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.Replay,
                        contentDescription =
                            if (isEnglish) {
                                "Reset timer"
                            } else {
                                "איפוס הטיימר"
                            },
                        modifier =
                            Modifier.size(
                                KmiIconSize.small
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun StretchingInstructionsCard(
    exercise: StretchingExercise,
    isEnglish: Boolean
) {
    val instructions =
        if (isEnglish) {
            exercise.instructionsEn
        } else {
            exercise.instructionsHe
        }

    InformationSectionCard(
        title =
            if (isEnglish) {
                "How to perform"
            } else {
                "אופן הביצוע"
            },
        text = instructions,
        emphasize = false,
        isEnglish = isEnglish
    )
}

@Composable
private fun StretchingSafetyCard(
    exercise: StretchingExercise,
    isEnglish: Boolean
) {
    val safetyText =
        if (isEnglish) {
            exercise.safetyNoteEn
        } else {
            exercise.safetyNoteHe
        }

    InformationSectionCard(
        title =
            if (isEnglish) {
                "Safety note"
            } else {
                "דגש בטיחות"
            },
        text = safetyText,
        emphasize = true,
        isEnglish = isEnglish
    )
}

@Composable
private fun InformationSectionCard(
    title: String,
    text: String,
    emphasize: Boolean,
    isEnglish: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color =
            if (emphasize) {
                MaterialTheme
                    .colorScheme
                    .errorContainer
                    .copy(alpha = 0.74f)
            } else {
                MaterialTheme
                    .colorScheme
                    .surface
                    .copy(alpha = 0.96f)
            },
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (emphasize) {
                        MaterialTheme
                            .colorScheme
                            .error
                            .copy(alpha = 0.38f)
                    } else {
                        MaterialTheme
                            .colorScheme
                            .outlineVariant
                            .copy(alpha = 0.8f)
                    }
            ),
        shadowElevation = 0.dp,
        tonalElevation =
            if (emphasize) {
                0.dp
            } else {
                1.dp
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .kmiSectionHeaderBackground()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 7.dp
                        )
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = KmiTypography.action,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        }
                )
            }

            Text(
                text = text,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 15.dp,
                            vertical = 13.dp
                        ),
                style = KmiTypography.body,
                color =
                    if (emphasize) {
                        MaterialTheme
                            .colorScheme
                            .onErrorContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurface
                    },
                textAlign =
                    if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    }
            )
        }
    }
}

@Composable
private fun MissingStretchingExercise(
    isEnglish: Boolean
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color =
                MaterialTheme
                    .colorScheme
                    .surface
                    .copy(alpha = 0.95f),
            border =
                BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .outlineVariant
                ),
            shadowElevation = 0.dp
        ) {
            Text(
                text =
                    if (isEnglish) {
                        "The requested exercise was not found."
                    } else {
                        "התרגיל המבוקש לא נמצא."
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                style = KmiTypography.body,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun exerciseTitle(
    exercise: StretchingExercise,
    isEnglish: Boolean
): String =
    if (isEnglish) {
        exercise.titleEn
    } else {
        exercise.titleHe
    }

private fun timerText(
    seconds: Int
): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60

    return buildString {
        append(minutes.toString().padStart(2, '0'))
        append(':')
        append(
            remainingSeconds
                .toString()
                .padStart(2, '0')
        )
    }
}

private fun durationLabel(
    seconds: Int,
    isEnglish: Boolean
): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    return when {
        minutes > 0 && remainingSeconds > 0 -> {
            if (isEnglish) {
                "${minutes}m ${remainingSeconds}s"
            } else {
                "$minutes דק׳ $remainingSeconds שנ׳"
            }
        }

        minutes > 0 -> {
            if (isEnglish) {
                "${minutes}m"
            } else {
                "$minutes דק׳"
            }
        }

        else -> {
            if (isEnglish) {
                "${remainingSeconds}s"
            } else {
                "$remainingSeconds שנ׳"
            }
        }
    }
}