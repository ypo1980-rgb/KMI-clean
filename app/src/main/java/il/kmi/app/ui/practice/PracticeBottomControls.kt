package il.kmi.app.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography

@Composable
fun PracticeBottomControls(
    isEnglish: Boolean,
    showSkip: Boolean,
    modifier: Modifier = Modifier,
    onHelp: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PracticeBottomPillButton(
                text = if (isEnglish) "Help" else "עזרה",
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null
                    )
                },
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                overlayGradient = null,
                modifier = Modifier.weight(1f),
                onClick = onHelp
            )

            if (showSkip) {
                PracticeBottomPillButton(
                    text = if (isEnglish) "Skip" else "דלג",
                    leading = {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                    },
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onSkip
                )
            }

            PracticeBottomPillButton(
                text = if (isEnglish) "Finish" else "סיום",
                leading = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null
                    )
                },
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                overlayGradient = null,
                modifier = Modifier.weight(1f),
                onClick = onFinish
            )
        }
    }
}

@Composable
private fun PracticeBottomActionsRow(
    isEnglish: Boolean,
    showSkip: Boolean,
    onHelp: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        PracticeBottomPillButton(
            text =
                if (isEnglish) {
                    "Help"
                } else {
                    "עזרה"
                },
            leading = {
                Icon(
                    imageVector =
                        Icons.Outlined.Info,
                    contentDescription =
                        if (isEnglish) {
                            "Help"
                        } else {
                            "עזרה"
                        }
                )
            },
            container =
                MaterialTheme
                    .colorScheme
                    .secondaryContainer,
            content =
                MaterialTheme
                    .colorScheme
                    .onSecondaryContainer,
            overlayGradient = null,
            onClick = onHelp,
            modifier =
                Modifier.weight(1f)
        )

        if (showSkip) {
            PracticeBottomPillButton(
                text =
                    if (isEnglish) {
                        "Skip"
                    } else {
                        "דלג"
                    },
                leading = {
                    Icon(
                        imageVector =
                            Icons.Filled.PlayArrow,
                        contentDescription =
                            if (isEnglish) {
                                "Skip"
                            } else {
                                "דלג"
                            }
                    )
                },
                container =
                    MaterialTheme
                        .colorScheme
                        .primary,
                content =
                    MaterialTheme
                        .colorScheme
                        .onPrimary,
                overlayGradient =
                    Brush.horizontalGradient(
                        colors =
                            listOf(
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary
                                    .copy(alpha = 0.08f),
                                Color.Transparent
                            )
                    ),
                onClick = onSkip,
                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PracticeBottomPillButton(
    text: String,
    leading: @Composable (() -> Unit)? = null,
    container: Color =
        MaterialTheme.colorScheme.primary,
    content: Color =
        MaterialTheme.colorScheme.onPrimary,
    overlayGradient: Brush? =
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.10f),
                Color.Transparent
            )
        ),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape =
        RoundedCornerShape(28.dp)

    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        contentColor = content,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    content.copy(alpha = 0.20f)
            ),
        modifier =
            modifier.height(58.dp)
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            if (overlayGradient != null) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(
                                overlayGradient
                            )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 4.dp,
                        vertical = 4.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                if (leading != null) {
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        leading()
                    }
                }
            }
        }
    }
}