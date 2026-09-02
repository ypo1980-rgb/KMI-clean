package il.kmi.app.voicecommands

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.scaledIconSize
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import kotlin.time.Duration.Companion.seconds

/**
 * האזנה לפקודה קולית ללא מסך מלא.
 *
 * בזמן ההאזנה מוצג רק עיגול מיקרופון מונפש
 * מעל המסך הפעיל.
 */
@Composable
fun VoiceCommandListener(
    onDismiss: () -> Unit,
    onCommand: (
        command: VoiceAppCommand,
        spokenText: String
    ) -> Unit
) {
    val context = LocalContext.current

    val currentOnCommand by rememberUpdatedState(onCommand)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val languageManager =
        remember(context) {
            AppLanguageManager(context)
        }

    val isEnglish =
        languageManager.getCurrentLanguage() ==
                AppLanguage.ENGLISH

    var state by remember {
        mutableStateOf(PushToTalkState.IDLE)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * מונע ממצב IDLE ההתחלתי לסגור את המאזין
     * לפני שהבקר התחיל לעבוד.
     */
    var hasListeningStarted by remember {
        mutableStateOf(false)
    }

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
                    newState == PushToTalkState.LISTENING ||
                    newState == PushToTalkState.PROCESSING
                ) {
                    hasListeningStarted = true
                }
            },
            onCommand = { command, spokenText ->
                /*
                 * סוגרים תחילה את שכבת ההאזנה כדי לאפשר
                 * למנוע ההקראה לקבל את מיקוד השמע.
                 */
                currentOnDismiss()

                /*
                 * לאחר תחילת סגירת המאזין מוסרים את הפקודה
                 * לניווט ולמערכת הפידבק.
                 */
                currentOnCommand(
                    command,
                    spokenText
                )
            },
            onError = { message ->
                errorMessage = message

                Toast.makeText(
                    context,
                    if (isEnglish) {
                        "Voice recognition could not be completed"
                    } else {
                        "לא ניתן היה להשלים את הזיהוי הקולי"
                    },
                    Toast.LENGTH_SHORT
                ).show()

                currentOnDismiss()
            },
            onPartialTranscript = {
                // אין צורך להציג תמלול על המסך.
            },
            currentScreenName = {
                "voice_command_listener"
            }
        )
    }

    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                errorMessage = null
                controller.startListening()
            } else {
                VoiceCommandDiagnosticsLogger.logFailure(
                    context = context,
                    source = "voice_command_listener",
                    reason = "microphone_permission_denied",
                    screenName = "voice_command_listener"
                )

                Toast.makeText(
                    context,
                    if (isEnglish) {
                        "Voice commands require microphone permission"
                    } else {
                        "נדרשת הרשאת מיקרופון להפעלת פקודות קוליות"
                    },
                    Toast.LENGTH_SHORT
                ).show()

                currentOnDismiss()
            }
        }

    LaunchedEffect(Unit) {
        if (hasMicrophonePermission()) {
            errorMessage = null

            /*
             * אותו controller קיים ממשיך להפעיל את ההאזנה,
             * ולכן גם הצפצוף הקיים נשמר.
             */
            controller.startListening()
        } else {
            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    /*
     * לאחר שהאזנה כבר התחילה, חזרה ל־IDLE פירושה
     * שהבקר סיים ואין להשאיר את שכבת המיקרופון פתוחה.
     */
    LaunchedEffect(state, hasListeningStarted) {
        if (
            hasListeningStarted &&
            state == PushToTalkState.IDLE
        ) {
            currentOnDismiss()
        }
    }

    /*
     * רשת ביטחון למקרה שמנוע זיהוי הדיבור אינו מחזיר
     * תוצאה, שגיאה או מצב סיום במכשיר מסוים.
     */
    LaunchedEffect(state) {
        if (
            state == PushToTalkState.STARTING ||
            state == PushToTalkState.LISTENING
        ) {
            delay(12.seconds)

            if (
                state == PushToTalkState.STARTING ||
                state == PushToTalkState.LISTENING
            ) {
                VoiceCommandDiagnosticsLogger.logFailure(
                    context = context,
                    source = "voice_command_listener",
                    reason = "recognition_timeout",
                    screenName = "voice_command_listener"
                )

                currentOnDismiss()
            }
        }
    }

    DisposableEffect(controller) {
        onDispose {
            controller.destroy()
        }
    }

    val listening =
        state == PushToTalkState.STARTING ||
                state == PushToTalkState.LISTENING

    val microphoneTint =
        when {
            errorMessage != null ->
                MaterialTheme.colorScheme.onError

            state == PushToTalkState.PROCESSING ->
                MaterialTheme.colorScheme.onPrimary

            else ->
                MaterialTheme.colorScheme.onSecondary
        }

    val microphoneDescription =
        when {
            errorMessage != null -> {
                if (isEnglish) {
                    "Voice recognition error"
                } else {
                    "שגיאה בזיהוי הקולי"
                }
            }

            state == PushToTalkState.PROCESSING -> {
                if (isEnglish) {
                    "Processing voice command"
                } else {
                    "מעבד פקודה קולית"
                }
            }

            else -> {
                if (isEnglish) {
                    "Listening"
                } else {
                    "מאזין"
                }
            }
        }

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "voice_listener_pulse"
        )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 700
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_listener_pulse_scale"
    )

    /*
     * שכבה שקופה לחלוטין:
     * המסך הפעיל נשאר גלוי ורק עיגול המיקרופון מופיע מעליו.
     */
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(
                    scaledIconSize(126.dp)
                )
                .scale(
                    if (listening) {
                        pulseScale
                    } else {
                        1f
                    }
                ),
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors =
                                when {
                                    errorMessage != null -> {
                                        listOf(
                                            MaterialTheme.colorScheme.error,
                                            MaterialTheme.colorScheme.errorContainer
                                        )
                                    }

                                    state ==
                                            PushToTalkState.PROCESSING -> {
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primaryContainer
                                        )
                                    }

                                    else -> {
                                        listOf(
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription =
                        microphoneDescription,
                    tint = microphoneTint,
                    modifier = Modifier.size(
                        KmiIconSize.hero
                    )
                )
            }
        }
    }
}