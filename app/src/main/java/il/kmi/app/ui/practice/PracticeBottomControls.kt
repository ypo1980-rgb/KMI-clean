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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
        modifier =
            modifier
                .fillMaxWidth(),
        shape =
            RoundedCornerShape(24.dp),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                        .copy(alpha = 0.70f)
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 8.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            PracticeBottomActionsRow(
                isEnglish = isEnglish,
                showSkip = showSkip,
                onHelp = onHelp,
                onSkip = onSkip
            )

            Surface(
                onClick = onFinish,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                shape =
                    RoundedCornerShape(17.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                        .copy(alpha = 0.72f),
                contentColor =
                    MaterialTheme
                        .colorScheme
                        .onPrimaryContainer,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(alpha = 0.22f)
                    )
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Finish and Return"
                            } else {
                                "סיום וחזרה"
                            },
                        style =
                            KmiTypography.action.copy(
                                fontWeight =
                                    FontWeight.ExtraBold
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onPrimaryContainer,
                        textAlign =
                            TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
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
            modifier.heightIn(min = 48.dp)
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

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.Center
            ) {
                if (leading != null) {
                    leading()

                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )
                }

                Text(
                    text = text,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis,
                    style =
                        KmiTypography.action.copy(
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                )
            }
        }
    }
}