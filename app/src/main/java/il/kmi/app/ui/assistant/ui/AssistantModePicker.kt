package il.kmi.app.ui.assistant.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.R
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.scaledIconSize

@Composable
internal fun ColumnScope.AssistantModePicker(
    assistantMode: AssistantMode?,
    isEnglish: Boolean,
    premiumCardBrush: Brush,
    onModeSelected: (AssistantMode) -> Unit
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

    val modePickerShape = RoundedCornerShape(30.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = modePickerShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.52f
                            )
                        )
                    ),
                    shape = modePickerShape
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = modePickerShape
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssistantModeButton(
                title =
                    tr(
                        "מידע על תרגיל",
                        "Exercise information"
                    ),
                selected =
                    assistantMode == AssistantMode.EXERCISE,
                icon = Icons.Filled.FitnessCenter,
                iconDescription =
                    tr(
                        "מידע על תרגיל",
                        "Exercise information"
                    ),
                isEnglish = isEnglish,
                textAlign = textAlignPrimary,
                premiumCardBrush = premiumCardBrush,
                onClick = {
                    onModeSelected(
                        AssistantMode.EXERCISE
                    )
                }
            )

            AssistantModeButton(
                title =
                    tr(
                        "מידע על אימונים",
                        "Training information"
                    ),
                selected =
                    assistantMode == AssistantMode.TRAININGS,
                icon = Icons.Filled.RecordVoiceOver,
                iconDescription =
                    tr(
                        "מידע על אימונים",
                        "Training information"
                    ),
                isEnglish = isEnglish,
                textAlign = textAlignPrimary,
                premiumCardBrush = premiumCardBrush,
                onClick = {
                    onModeSelected(
                        AssistantMode.TRAININGS
                    )
                }
            )

            AssistantModeButton(
                title =
                    tr(
                        "חומר ק.מ.י",
                        "KAMI material"
                    ),
                selected =
                    assistantMode == AssistantMode.KMI_MATERIAL,
                icon = Icons.Filled.MenuBook,
                iconDescription =
                    tr(
                        "חומר ק.מ.י",
                        "KAMI material"
                    ),
                isEnglish = isEnglish,
                textAlign = textAlignPrimary,
                premiumCardBrush = premiumCardBrush,
                onClick = {
                    onModeSelected(
                        AssistantMode.KMI_MATERIAL
                    )
                }
            )
        }
    }

    val logoTransition =
        rememberInfiniteTransition(
            label = "kamiLogoPulse"
        )

    val logoScale by logoTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kamiLogoScale"
    )

    Column(
        modifier = Modifier
            .weight(1f, fill = true)
            .fillMaxWidth()
            .padding(
                top = 44.dp,
                bottom = 2.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.requiredSize(
                scaledIconSize(102.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(
                        scaledIconSize(100.dp)
                    )
                    .scale(logoScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.34f
                                ),
                                MaterialTheme.colorScheme.secondary.copy(
                                    alpha = 0.14f
                                ),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Surface(
                modifier = Modifier
                    .requiredSize(
                        scaledIconSize(84.dp)
                    )
                    .scale(logoScale)
                    .border(
                        width = 2.dp,
                        color =
                            MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 14.dp
            ) {
                Image(
                    painter = painterResource(
                        R.drawable.kami_logo
                    ),
                    contentDescription =
                        tr(
                            "לוגו ק.מ.י",
                            "K.A.M.I logo"
                        ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text =
                tr(
                    "העוזר האישי של ק.מ.י",
                    "K.A.M.I Personal Assistant"
                ),
            color =
                MaterialTheme.colorScheme.onBackground,
            style = KmiTypography.cardTitle
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text =
                tr(
                    "המיקרופון פועל רק לאחר לחיצה",
                    "Microphone activates only when tapped"
                ),
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            style = KmiTypography.caption.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun AssistantModeButton(
    title: String,
    selected: Boolean,
    icon: ImageVector,
    iconDescription: String,
    isEnglish: Boolean,
    textAlign: TextAlign,
    premiumCardBrush: Brush,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    val outlineColor =
        if (selected) {
            Color(0xFF7C3AED)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    Surface(
        onClick = onClick,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation =
            if (selected) 14.dp else 7.dp,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = outlineColor,
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush =
                        if (selected) {
                            premiumCardBrush
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.42f
                                    )
                                )
                            )
                        },
                    shape = shape
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 15.dp
                )
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
                    AssistantModeIcon(
                        selected = selected,
                        icon = icon,
                        iconDescription =
                            iconDescription
                    )

                    Spacer(Modifier.width(10.dp))
                }

                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    textAlign = textAlign,
                    style = KmiTypography.sectionTitle,
                    color =
                        if (selected) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                )

                if (!isEnglish) {
                    Spacer(Modifier.width(10.dp))

                    AssistantModeIcon(
                        selected = selected,
                        icon = icon,
                        iconDescription =
                            iconDescription
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantModeIcon(
    selected: Boolean,
    icon: ImageVector,
    iconDescription: String
) {
    Surface(
        modifier = Modifier.size(
            scaledIconSize(46.dp)
        ),
        shape = RoundedCornerShape(16.dp),
        color =
            if (selected) {
                Color.White.copy(alpha = 0.20f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        tonalElevation = 0.dp,
        shadowElevation = 5.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                tint =
                    if (selected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                modifier = Modifier.size(
                    KmiIconSize.medium
                )
            )
        }
    }
}