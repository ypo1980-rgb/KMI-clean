package il.kmi.app.voicecommands

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

@Composable
fun PushToTalkVoiceDialog(
    onDismiss: () -> Unit,
    onCommand: (
        command: VoiceAppCommand,
        spokenText: String
    ) -> Unit
) {
    val context = LocalContext.current

    val isEnglish =
        AppLanguageManager(context).getCurrentLanguage() ==
                AppLanguage.ENGLISH

    fun tr(hebrew: String, english: String): String {
        return if (isEnglish) english else hebrew
    }

    var state by remember {
        mutableStateOf(PushToTalkState.IDLE)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val currentOnCommand by rememberUpdatedState(onCommand)

    val controller = remember(context) {
        PushToTalkVoiceController(
            context = context,
            isEnglish = { isEnglish },
            onStateChanged = { newState ->
                state = newState
            },
            onCommand = { command, spokenText ->
                /*
                 * MainNavHost אחראי לסגירת הדיאלוג ולביצוע הניווט.
                 * לא סוגרים כאן פעם נוספת, כדי לא לקטוע את הפעולה.
                 */
                currentOnCommand(command, spokenText)
            },
            onError = { message ->
                errorMessage = message
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
                    "לא ניתן להשתמש בפקודות קוליות ללא הרשאת מיקרופון",
                    "Voice commands require microphone permission"
                )
            }
        }

    LaunchedEffect(Unit) {
        if (hasMicrophonePermission()) {
            startListening()
        } else {
            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    DisposableEffect(controller) {
        onDispose {
            controller.destroy()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "voiceCommandPulse"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceCommandPulseScale"
    )

    val listening =
        state == PushToTalkState.STARTING ||
                state == PushToTalkState.LISTENING

    val statusText = when {
        errorMessage != null ->
            errorMessage.orEmpty()

        state == PushToTalkState.STARTING ->
            tr(
                "מפעיל את המיקרופון...",
                "Starting microphone..."
            )

        state == PushToTalkState.LISTENING ->
            tr(
                "מקשיב לפקודה...",
                "Listening for a command..."
            )

        state == PushToTalkState.PROCESSING ->
            tr(
                "מעבד את הפקודה...",
                "Processing command..."
            )

        else ->
            tr(
                "לחץ על המיקרופון ואמור פקודה",
                "Tap the microphone and say a command"
            )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(126.dp)
                    .scale(
                        if (listening) pulseScale else 1f
                    ),
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 18.dp,
                onClick = {
                    when {
                        listening -> {
                            controller.stopListening()
                        }

                        hasMicrophonePermission() -> {
                            startListening()
                        }

                        else -> {
                            permissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = when {
                                    errorMessage != null -> {
                                        listOf(
                                            Color(0xFFEF4444),
                                            Color(0xFFDC2626)
                                        )
                                    }

                                    listening -> {
                                        listOf(
                                            Color(0xFF38BDF8),
                                            Color(0xFF6366F1),
                                            Color(0xFF7C3AED)
                                        )
                                    }

                                    state == PushToTalkState.PROCESSING -> {
                                        listOf(
                                            Color(0xFF8B5CF6),
                                            Color(0xFF4F46E5)
                                        )
                                    }

                                    else -> {
                                        listOf(
                                            Color(0xFF6366F1),
                                            Color(0xFF4338CA)
                                        )
                                    }
                                }
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            errorMessage != null ->
                                Icons.Filled.Refresh

                            listening ->
                                Icons.Filled.Mic

                            state == PushToTalkState.PROCESSING ->
                                Icons.Filled.Mic

                            else ->
                                Icons.Filled.Mic
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xEFFFFFFF),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    ),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (errorMessage == null) {
                        Color(0xFF172033)
                    } else {
                        Color(0xFFDC2626)
                    }
                )
            }
        }
    }
}