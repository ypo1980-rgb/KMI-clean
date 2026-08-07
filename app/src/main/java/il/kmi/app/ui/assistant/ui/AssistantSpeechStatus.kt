package il.kmi.app.ui.assistant.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography

@OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
internal fun AssistantSpeechStatus(
    assistantMode: AssistantMode?,
    isEnglish: Boolean,
    isThinking: Boolean,
    isListening: Boolean,
    speechStatusMessage: String?,
    speechNeedsConfirmation: Boolean,
    speechAlternatives: List<String>,
    speechCanRetry: Boolean,
    onAlternativeSelected: (String) -> Unit,
    onRetry: () -> Unit
) {
    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    val textAlignPrimary =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val liveAssistantStatus =
        when {
            isThinking &&
                    assistantMode == AssistantMode.EXERCISE ->
                tr(
                    "מאתר את התרגיל ובודק את ההסבר המתאים…",
                    "Finding the exercise and checking the best explanation…"
                )

            isThinking &&
                    assistantMode == AssistantMode.KMI_MATERIAL ->
                tr(
                    "מחפש בחומר ק.מ.י ומדרג את התוצאות…",
                    "Searching KAMI material and ranking the results…"
                )

            isThinking &&
                    assistantMode == AssistantMode.TRAININGS ->
                tr(
                    "בודק את פרטי המשתמש והאימונים הקרובים…",
                    "Checking your profile and upcoming trainings…"
                )

            isThinking ->
                tr(
                    "מבין את הבקשה ומכין תשובה…",
                    "Understanding your request and preparing an answer…"
                )

            isListening ->
                tr(
                    "מקשיב — אפשר לדבר באופן טבעי…",
                    "Listening — you can speak naturally…"
                )

            else ->
                speechStatusMessage
        }

    if (liveAssistantStatus.isNullOrBlank()) {
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 3.dp
            ),
        shape = RoundedCornerShape(18.dp),
        color =
            when {
                speechNeedsConfirmation ->
                    Color(0xFFFFF8E7)

                speechStatusMessage != null ->
                    Color(0xFFFFF1F2)

                isListening ->
                    Color(0xFFF0EDFF)

                else ->
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.90f
                    )
            },
        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color =
                    when {
                        speechNeedsConfirmation ->
                            Color(0xFFF2C94C).copy(
                                alpha = 0.65f
                            )

                        speechStatusMessage != null ->
                            Color(0xFFFCA5A5).copy(
                                alpha = 0.75f
                            )

                        else ->
                            Color(0xFFDDD6FE)
                    }
            ),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    if (isEnglish) {
                        Arrangement.Start
                    } else {
                        Arrangement.End
                    }
            ) {
                if (isEnglish) {
                    AssistantStatusIcon()
                    Spacer(Modifier.width(9.dp))
                }

                Text(
                    text = liveAssistantStatus,
                    modifier = Modifier.weight(1f),
                    color =
                        when {
                            speechNeedsConfirmation ->
                                Color(0xFF8A5A00)

                            speechStatusMessage != null ->
                                Color(0xFFB42318)

                            isListening ->
                                Color(0xFF6246B5)

                            else ->
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        },
                    style =
                        MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = textAlignPrimary
                )

                if (isThinking || isListening) {
                    AssistantStatusDots()
                }

                if (!isEnglish) {
                    Spacer(Modifier.width(9.dp))
                    AssistantStatusIcon()
                }
            }

            if (
                speechNeedsConfirmation &&
                speechAlternatives.isNotEmpty()
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(7.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(7.dp)
                ) {
                    speechAlternatives.forEach { alternative ->
                        Surface(
                            modifier = Modifier.clickable {
                                onAlternativeSelected(
                                    alternative.trim()
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border =
                                androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = Color(0xFFB8A9E8)
                                ),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = alternative,
                                modifier = Modifier.padding(
                                    horizontal = 11.dp,
                                    vertical = 8.dp
                                ),
                                color = Color(0xFF4C3A80),
                                style =
                                    KmiTypography.secondary.copy(
                                        fontWeight =
                                            FontWeight.Bold
                                    ),
                                textAlign = textAlignPrimary
                            )
                        }
                    }
                }
            }

            if (speechCanRetry && !isListening) {
                Surface(
                    onClick = onRetry,
                    modifier = Modifier.align(
                        if (isEnglish) {
                            Alignment.Start
                        } else {
                            Alignment.End
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF6D4AFF)
                ) {
                    Text(
                        text =
                            tr(
                                "נסה שוב עם המיקרופון",
                                "Try again with the microphone"
                            ),
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 8.dp
                        ),
                        color = Color.White,
                        style = KmiTypography.action
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantStatusIcon() {
    Surface(
        modifier = Modifier.size(30.dp),
        shape = CircleShape,
        color = Color(0xFFEDE9FE)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF6D4AFF),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AssistantStatusDots() {
    val transition =
        rememberInfiniteTransition(
            label = "assistantStatusDots"
        )

    val middleDotAlpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "assistantStatusDotAlpha"
    )

    Spacer(Modifier.width(8.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size((5 + index).dp)
                    .background(
                        color = Color(0xFF6D4AFF).copy(
                            alpha =
                                if (index == 1) {
                                    middleDotAlpha
                                } else {
                                    0.45f
                                }
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}