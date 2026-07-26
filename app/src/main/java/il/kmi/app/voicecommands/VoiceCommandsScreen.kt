package il.kmi.app.voicecommands

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//===================================================================

@Composable
fun VoiceCommandsScreen(
    onDismiss: () -> Unit,
    onCommand: (
        command: VoiceAppCommand,
        spokenText: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isEnglish =
        AppLanguageManager(context).getCurrentLanguage() ==
                AppLanguage.ENGLISH

    fun tr(
        hebrew: String,
        english: String
    ): String {
        return if (isEnglish) english else hebrew
    }

    var state by remember {
        mutableStateOf(PushToTalkState.IDLE)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var lastRecognizedText by remember {
        mutableStateOf<String?>(null)
    }

    var partialTranscript by remember {
        mutableStateOf("")
    }

    var commandWasSent by remember {
        mutableStateOf(false)
    }

    val currentOnCommand by rememberUpdatedState(onCommand)

    val scope = rememberCoroutineScope()

    val controller = remember(context) {
        PushToTalkVoiceController(
            context = context,
            isEnglish = {
                isEnglish
            },
            onStateChanged = { newState ->
                state = newState

                if (
                    newState == PushToTalkState.STARTING ||
                    newState == PushToTalkState.LISTENING
                ) {
                    commandWasSent = false
                }
            },
            onCommand = { command, spokenText ->
                lastRecognizedText =
                    spokenText
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }

                partialTranscript = ""

                commandWasSent =
                    command !is VoiceAppCommand.Unknown

                scope.launch {
                    delay(
                        if (command is VoiceAppCommand.Unknown) {
                            250L
                        } else {
                            700L
                        }
                    )

                    currentOnCommand(
                        command,
                        spokenText
                    )
                }
            },
            onError = { message ->
                errorMessage = message
                commandWasSent = false
            },
            onPartialTranscript = { transcript ->
                partialTranscript = transcript
            },
            currentScreenName = {
                "voice_commands_screen"
            }
        )
    }

    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening() {
        errorMessage = null
        lastRecognizedText = null
        partialTranscript = ""
        commandWasSent = false
        controller.startListening()
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startListening()
            } else {
                errorMessage = tr(
                    "לא ניתן להשתמש בפקודות קוליות ללא הרשאת מיקרופון.",
                    "Voice commands require microphone permission."
                )
            }
        }

    fun requestListening() {
        if (hasMicrophonePermission()) {
            startListening()
        } else {
            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    LaunchedEffect(Unit) {
        requestListening()
    }

    DisposableEffect(controller) {
        onDispose {
            controller.destroy()
        }
    }

    val isStarting =
        state == PushToTalkState.STARTING

    val isListening =
        state == PushToTalkState.LISTENING

    val isProcessing =
        state == PushToTalkState.PROCESSING

    val isActive =
        isStarting ||
                isListening ||
                isProcessing

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "voiceCommandsScreenPulse"
        )

    val pulseScale by
    infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 760,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceCommandsMicPulse"
    )

    val outerPulseScale by
    infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1050,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceCommandsOuterPulse"
    )

    val microphoneScale by
    animateFloatAsState(
        targetValue =
            if (isStarting || isListening) {
                pulseScale
            } else {
                1f
            },
        animationSpec = tween(
            durationMillis = 220
        ),
        label = "voiceCommandsMicrophoneScale"
    )

    val accentColor by
    animateColorAsState(
        targetValue = when {
            errorMessage != null ->
                Color(0xFFDC2626)

            isListening ->
                Color(0xFF16A34A)

            isProcessing ->
                Color(0xFF7C3AED)

            isStarting ->
                Color(0xFF2563EB)

            commandWasSent ->
                Color(0xFF059669)

            else ->
                Color(0xFF6D4ED8)
        },
        animationSpec = tween(
            durationMillis = 300
        ),
        label = "voiceCommandsAccentColor"
    )

    val statusTitle =
        when {
            errorMessage != null ->
                tr(
                    "לא הצלחתי לבצע את הפקודה",
                    "The command could not be completed"
                )

            isStarting ->
                tr(
                    "מפעיל את המיקרופון",
                    "Starting the microphone"
                )

            isListening ->
                tr(
                    "מקשיב לפקודה שלך",
                    "Listening for your command"
                )

            isProcessing ->
                tr(
                    "מזהה ומבצע את הפקודה",
                    "Recognizing and running the command"
                )

            commandWasSent ->
                tr(
                    "הפקודה זוהתה",
                    "Command recognized"
                )

            else ->
                tr(
                    "מוכן לפקודה קולית",
                    "Ready for a voice command"
                )
        }

    val statusDescription =
        when {
            errorMessage != null ->
                errorMessage.orEmpty()

            isStarting ->
                tr(
                    "המתן רגע עד שהמיקרופון יהיה מוכן.",
                    "Wait a moment while the microphone gets ready."
                )

            isListening ->
                tr(
                    "אמור פקודה קצרה וברורה. לדוגמה: פתח הגדרות.",
                    "Say a short, clear command. For example: Open settings."
                )

            isProcessing ->
                tr(
                    "הדיבור הסתיים. האפליקציה בודקת איזו פעולה לבצע.",
                    "Speech ended. The app is deciding which action to run."
                )

            commandWasSent ->
                lastRecognizedText?.let { recognized ->
                    tr(
                        "זוהה: „$recognized”",
                        "Recognized: “$recognized”"
                    )
                } ?: tr(
                    "הפעולה נשלחה לביצוע.",
                    "The action was sent for execution."
                )

            else ->
                tr(
                    "לחץ על המיקרופון ואמור מה ברצונך לפתוח או לבצע.",
                    "Tap the microphone and say what you want to open or do."
                )
        }

    val backgroundBrush =
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FAFF),
                Color(0xFFF1EDFF),
                Color(0xFFE8E2F7),
                Color(0xFFF3F7FF)
            )
        )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        VoiceCommandsHeader(
            isEnglish = isEnglish,
            onDismiss = {
                controller.cancelListening()
                onDismiss()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 14.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            VoiceCommandsHeroCard(
                isEnglish = isEnglish,
                accentColor = accentColor,
                statusTitle = statusTitle,
                statusDescription = statusDescription,
                state = state,
                errorMessage = errorMessage,
                microphoneScale = microphoneScale,
                outerPulseScale = outerPulseScale,
                isActive = isActive,
                onMicrophoneClick = {
                    when {
                        isStarting ||
                                isListening -> {
                            controller.stopListening()
                        }

                        isProcessing -> {
                            Unit
                        }

                        else -> {
                            requestListening()
                        }
                    }
                }
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            AnimatedVisibility(
                visible = partialTranscript.isNotBlank()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = tr(
                                "🎤 תמלול בזמן אמת",
                                "🎤 Live transcript"
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = partialTranscript,
                            fontSize = 18.sp,
                            color = Color(0xFF172033),
                            lineHeight = 25.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            VoiceCommandExamplesCard(
                isEnglish = isEnglish
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            VoiceCommandBottomAction(
                isEnglish = isEnglish,
                state = state,
                hasError = errorMessage != null,
                onClick = {
                    when {
                        isStarting ||
                                isListening -> {
                            controller.stopListening()
                        }

                        isProcessing -> {
                            Unit
                        }

                        else -> {
                            requestListening()
                        }
                    }
                }
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}

@Composable
private fun VoiceCommandsHeader(
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(
            alpha = 0.96f
        ),
        shadowElevation = 7.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(
                    horizontal = 12.dp
                )
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(
                    if (isEnglish) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    }
                )
            ) {
                Icon(
                    imageVector =
                        Icons.Filled.Close,
                    contentDescription =
                        if (isEnglish) {
                            "Close"
                        } else {
                            "סגור"
                        },
                    tint = Color(0xFF475569)
                )
            }

            Column(
                modifier = Modifier.align(
                    Alignment.Center
                ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            "Voice Commands"
                        } else {
                            "פקודות קוליות"
                        },
                    color = Color(0xFF111827),
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )

                Text(
                    text =
                        if (isEnglish) {
                            "Control the app by voice"
                        } else {
                            "שליטה באפליקציה באמצעות הקול"
                        },
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun VoiceCommandsHeroCard(
    isEnglish: Boolean,
    accentColor: Color,
    statusTitle: String,
    statusDescription: String,
    state: PushToTalkState,
    errorMessage: String?,
    microphoneScale: Float,
    outerPulseScale: Float,
    isActive: Boolean,
    onMicrophoneClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(
                max = 620.dp
            ),
        shape = RoundedCornerShape(30.dp),
        color = Color.White.copy(
            alpha = 0.98f
        ),
        shadowElevation = 14.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(
                alpha = 0.18f
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 22.dp,
                    vertical = 24.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(
                    999.dp
                ),
                color = accentColor.copy(
                    alpha = 0.10f
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = accentColor.copy(
                        alpha = 0.20f
                    )
                )
            ) {
                Text(
                    text = when {
                        errorMessage != null ->
                            if (isEnglish) {
                                "ACTION REQUIRED"
                            } else {
                                "נדרש ניסיון נוסף"
                            }

                        state ==
                                PushToTalkState.LISTENING ->
                            if (isEnglish) {
                                "MICROPHONE ACTIVE"
                            } else {
                                "המיקרופון פעיל"
                            }

                        state ==
                                PushToTalkState.PROCESSING ->
                            if (isEnglish) {
                                "PROCESSING"
                            } else {
                                "מעבד פקודה"
                            }

                        else ->
                            if (isEnglish) {
                                "VOICE CONTROL"
                            } else {
                                "שליטה קולית"
                            }
                    },
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    ),
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight =
                        FontWeight.Black
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Box(
                modifier = Modifier.size(
                    164.dp
                ),
                contentAlignment =
                    Alignment.Center
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .scale(
                                outerPulseScale
                            )
                            .background(
                                color =
                                    accentColor.copy(
                                        alpha = 0.10f
                                    ),
                                shape =
                                    CircleShape
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .scale(
                            microphoneScale
                        )
                        .background(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(
                                            alpha = 0.78f
                                        ),
                                        accentColor
                                    )
                                ),
                            shape = CircleShape
                        )
                        .border(
                            width = 4.dp,
                            color =
                                Color.White.copy(
                                    alpha = 0.80f
                                ),
                            shape = CircleShape
                        )
                        .clickable(
                            enabled =
                                state !=
                                        PushToTalkState.PROCESSING,
                            indication = null,
                            interactionSource =
                                remember {
                                    MutableInteractionSource()
                                },
                            onClick =
                                onMicrophoneClick
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Crossfade(
                        targetState = state,
                        label =
                            "voiceCommandMainIcon"
                    ) { currentState ->
                        Icon(
                            imageVector =
                                when (currentState) {
                                    PushToTalkState.STARTING,
                                    PushToTalkState.LISTENING ->
                                        Icons.Filled.Stop

                                    PushToTalkState.PROCESSING ->
                                        Icons.Filled.TrendingUp

                                    PushToTalkState.IDLE ->
                                        if (
                                            errorMessage != null
                                        ) {
                                            Icons.Filled.Refresh
                                        } else {
                                            Icons.Filled.Mic
                                        }
                                },
                            contentDescription =
                                null,
                            tint = Color.White,
                            modifier =
                                Modifier.size(
                                    54.dp
                                )
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            AnimatedContent(
                targetState = statusTitle,
                transitionSpec = {
                    fadeIn(
                        animationSpec =
                            tween(220)
                    ) togetherWith
                            fadeOut(
                                animationSpec =
                                    tween(160)
                            )
                },
                label =
                    "voiceCommandStatusTitle"
            ) { title ->
                Text(
                    text = title,
                    modifier =
                        Modifier.fillMaxWidth(),
                    color = Color(0xFF172033),
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    textAlign =
                        TextAlign.Center
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = statusDescription,
                modifier = Modifier.fillMaxWidth(),
                color =
                    if (errorMessage != null) {
                        Color(0xFFDC2626)
                    } else {
                        Color(0xFF526079)
                    },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VoiceCommandExamplesCard(
    isEnglish: Boolean
) {
    val examples =
        if (isEnglish) {
            listOf(
                VoiceExample(
                    icon = Icons.Filled.Home,
                    title = "Navigation",
                    commands = listOf(
                        "Open home",
                        "Go back",
                        "Open trainings"
                    ),
                    accent = Color(0xFF2563EB)
                ),
                VoiceExample(
                    icon = Icons.Filled.Search,
                    title = "Exercises",
                    commands = listOf(
                        "Open green belt",
                        "Search for roundhouse kick",
                        "Explain side kick"
                    ),
                    accent = Color(0xFF7C3AED)
                ),
                VoiceExample(
                    icon = Icons.Filled.Settings,
                    title = "App tools",
                    commands = listOf(
                        "Open settings",
                        "Open statistics",
                        "Open my profile"
                    ),
                    accent = Color(0xFFF59E0B)
                )
            )
        } else {
            listOf(
                VoiceExample(
                    icon = Icons.Filled.Home,
                    title = "ניווט",
                    commands = listOf(
                        "פתח מסך הבית",
                        "חזור אחורה",
                        "פתח אימונים"
                    ),
                    accent = Color(0xFF2563EB)
                ),
                VoiceExample(
                    icon = Icons.Filled.Search,
                    title = "תרגילים",
                    commands = listOf(
                        "פתח חגורה ירוקה",
                        "חפש בעיטת מגל",
                        "הסבר על בעיטת צד"
                    ),
                    accent = Color(0xFF7C3AED)
                ),
                VoiceExample(
                    icon = Icons.Filled.Settings,
                    title = "כלי האפליקציה",
                    commands = listOf(
                        "פתח הגדרות",
                        "פתח סטטיסטיקה",
                        "פתח את הפרופיל שלי"
                    ),
                    accent = Color(0xFFF59E0B)
                )
            )
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(
                max = 620.dp
            ),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(
            alpha = 0.97f
        ),
        shadowElevation = 9.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 18.dp
                )
        ) {
            Text(
                text =
                    if (isEnglish) {
                        "Example commands"
                    } else {
                        "דוגמאות לפקודות"
                    },
                modifier =
                    Modifier.fillMaxWidth(),
                color = Color(0xFF172033),
                fontSize = 17.sp,
                fontWeight =
                    FontWeight.ExtraBold,
                textAlign =
                    if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            examples.forEachIndexed { index, item ->
                VoiceExampleRow(
                    item = item,
                    isEnglish = isEnglish
                )

                if (index != examples.lastIndex) {
                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceExampleRow(
    item: VoiceExample,
    isEnglish: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = item.accent.copy(
            alpha = 0.07f
        ),
        border = BorderStroke(
            width = 1.dp,
            color = item.accent.copy(
                alpha = 0.16f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 13.dp,
                    vertical = 12.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp),
            verticalAlignment =
                Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(
                    14.dp
                ),
                color = item.accent.copy(
                    alpha = 0.14f
                )
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.accent,
                        modifier =
                            Modifier.size(21.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    modifier =
                        Modifier.fillMaxWidth(),
                    color = Color(0xFF24304D),
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        }
                )

                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )

                FlowRow(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    item.commands.forEach {
                            command ->
                        Surface(
                            shape =
                                RoundedCornerShape(
                                    999.dp
                                ),
                            color = Color.White,
                            border =
                                BorderStroke(
                                    width = 1.dp,
                                    color =
                                        item.accent.copy(
                                            alpha =
                                                0.20f
                                        )
                                )
                        ) {
                            Text(
                                text = command,
                                modifier =
                                    Modifier.padding(
                                        horizontal =
                                            10.dp,
                                        vertical =
                                            6.dp
                                    ),
                                color =
                                    Color(0xFF475569),
                                fontSize = 11.5.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceCommandBottomAction(
    isEnglish: Boolean,
    state: PushToTalkState,
    hasError: Boolean,
    onClick: () -> Unit
) {
    val isListening =
        state == PushToTalkState.STARTING ||
                state == PushToTalkState.LISTENING

    val isProcessing =
        state == PushToTalkState.PROCESSING

    Button(
        onClick = onClick,
        enabled = !isProcessing,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(
                max = 620.dp
            )
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor =
                when {
                    isListening ->
                        Color(0xFFDC2626)

                    hasError ->
                        Color(0xFF7C3AED)

                    else ->
                        Color(0xFF4F46E5)
                },
            contentColor = Color.White,
            disabledContainerColor =
                Color(0xFF7C3AED).copy(
                    alpha = 0.55f
                ),
            disabledContentColor =
                Color.White.copy(
                    alpha = 0.85f
                )
        )
    ) {
        Icon(
            imageVector =
                when {
                    isListening ->
                        Icons.Filled.Stop

                    isProcessing ->
                        Icons.Filled.TrendingUp

                    hasError ->
                        Icons.Filled.Refresh

                    else ->
                        Icons.Filled.Mic
                },
            contentDescription = null
        )

        Spacer(
            modifier = Modifier.height(1.dp)
        )

        Text(
            text =
                when {
                    isListening && isEnglish ->
                        "Finish command"

                    isListening ->
                        "סיים פקודה"

                    isProcessing && isEnglish ->
                        "Processing command..."

                    isProcessing ->
                        "מעבד פקודה..."

                    hasError && isEnglish ->
                        "Try again"

                    hasError ->
                        "נסה שוב"

                    isEnglish ->
                        "Start listening"

                    else ->
                        "התחל להאזין"
                },
            modifier = Modifier.padding(
                horizontal = 10.dp
            ),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private data class VoiceExample(
    val icon: ImageVector,
    val title: String,
    val commands: List<String>,
    val accent: Color
)