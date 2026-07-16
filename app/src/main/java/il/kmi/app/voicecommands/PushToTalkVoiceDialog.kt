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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
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

    Dialog(
        onDismissRequest = {
            controller.cancelListening()
            onDismiss()
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color.White,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 20.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            controller.cancelListening()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = tr("סגירה", "Close")
                        )
                    }
                }

                Text(
                    text = tr(
                        "פקודות קוליות",
                        "Voice commands"
                    ),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF172033)
                )

                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .scale(
                            if (listening) pulseScale else 1f
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (listening) {
                                    listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFF6366F1),
                                        Color(0xFF7C3AED)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFE0F2FE),
                                        Color(0xFFEDE9FE)
                                    )
                                }
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (listening) {
                            Icons.Filled.Stop
                        } else {
                            Icons.Filled.Mic
                        },
                        contentDescription = null,
                        tint = if (listening) {
                            Color.White
                        } else {
                            Color(0xFF4F46E5)
                        },
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = statusText,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (errorMessage == null) {
                        Color(0xFF334155)
                    } else {
                        Color(0xFFDC2626)
                    }
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = tr(
                        "לדוגמה: „פתח חגורה ירוקה” או „הסבר על בעיטת צד”",
                        "For example: “Open green belt” or “Explain side kick”"
                    ),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = {
                        if (listening) {
                            controller.stopListening()
                        } else if (hasMicrophonePermission()) {
                            startListening()
                        } else {
                            permissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = when {
                            listening -> Icons.Filled.Stop
                            errorMessage != null -> Icons.Filled.Refresh
                            else -> Icons.Filled.Mic
                        },
                        contentDescription = null
                    )

                    Spacer(Modifier.size(8.dp))

                    Text(
                        text = when {
                            listening ->
                                tr("סיים פקודה", "Finish command")

                            errorMessage != null ->
                                tr("נסה שוב", "Try again")

                            else ->
                                tr("התחל להאזין", "Start listening")
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}