package il.kmi.app.ui.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography

@Composable
private fun KmiLoadingRing(
    size: Dp,
    width: Dp,
    rotation: Float,
    colors: List<Color>
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation
            }
            .border(
                width = width,
                brush = Brush.sweepGradient(colors),
                shape = CircleShape
            )
    )
}

@Composable
fun KmiLoadingRings(
    modifier: Modifier = Modifier,
    text: String? = null,
    size: Dp = 82.dp
) {
    val scale =
        size.value / 82f

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "kmiLoadingRings"
        )

    val outerRotation by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 1350,
                    easing = LinearEasing
                )
            ),
        label = "kmiLoadingOuterRotation"
    )

    val middleRotation by
    infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 1650,
                    easing = LinearEasing
                )
            ),
        label = "kmiLoadingMiddleRotation"
    )

    val innerRotation by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 2050,
                    easing = LinearEasing
                )
            ),
        label = "kmiLoadingInnerRotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(
                14.dp * scale
            )
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment =
                Alignment.Center
        ) {
            KmiLoadingRing(
                size = 76.dp * scale,
                width = 5.dp * scale,
                rotation = outerRotation,
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFA78BFA),
                    Color(0xFF38BDF8),
                    Color.Transparent
                )
            )

            KmiLoadingRing(
                size = 62.dp * scale,
                width = 4.dp * scale,
                rotation = middleRotation,
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF38BDF8),
                    Color(0xFFA78BFA),
                    Color.Transparent
                )
            )

            KmiLoadingRing(
                size = 48.dp * scale,
                width = 3.5.dp * scale,
                rotation = innerRotation,
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFF59E0B),
                    Color(0xFF22C55E),
                    Color.Transparent
                )
            )

            Surface(
                modifier =
                    Modifier.size(
                        25.dp * scale
                    ),
                shape = CircleShape,
                color =
                    MaterialTheme
                        .colorScheme
                        .surface,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                border = BorderStroke(
                    width =
                        (1.dp * scale)
                            .coerceAtLeast(0.5.dp),
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(alpha = 0.32f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme
                                            .colorScheme
                                            .surface,
                                        MaterialTheme
                                            .colorScheme
                                            .primaryContainer,
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                    )
                                ),
                            shape = CircleShape
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        style =
                            KmiTypography.caption,
                        fontWeight =
                            FontWeight.Black,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        textAlign =
                            TextAlign.Center
                    )
                }
            }
        }

        if (!text.isNullOrBlank()) {
            Text(
                text = text,
                style = KmiTypography.cardTitle,
                color =
                    MaterialTheme
                        .colorScheme
                        .onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}