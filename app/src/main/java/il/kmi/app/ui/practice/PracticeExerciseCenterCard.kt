package il.kmi.app.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.R
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.ext.color
import il.kmi.shared.domain.Belt

@Composable
fun PracticeExerciseCenterCard(
    belt: Belt,
    exerciseTitle: String,
    exerciseSubtitle: String?,
    timeText: String,
    currentIndex: Int,
    totalCount: Int,
    centerLabel: String? = null,
    isRunning: Boolean,
    isMuted: Boolean,
    onToggleRunning: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
    onCenterClick: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null
) {
    val accent = belt.color

    val statusColor =
        when (
            centerLabel
                ?.trim()
                ?.lowercase()
        ) {
            "יודע",
            "known" ->
                androidx.compose.ui.graphics.Color(
                    0xFF2E7D32
                )

            "לא יודע",
            "not known" ->
                MaterialTheme.colorScheme.error

            "חלקי",
            "partial" ->
                androidx.compose.ui.graphics.Color(
                    0xFFF9A825
                )

            else ->
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        }

    val statusBackgroundColor =
        statusColor.copy(alpha = 0.14f)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight(
                    align = Alignment.Top,
                    unbounded = true
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .then(
                        if (onCardClick != null) {
                            Modifier.clickable {
                                onCardClick()
                            }
                        } else {
                            Modifier
                        }
                    ),
            shape = RoundedCornerShape(28.dp),
            color =
                MaterialTheme
                    .colorScheme
                    .surface
                    .copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border =
                BorderStroke(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.25f)
                )
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter =
                        painterResource(
                            id = beltImageRes(belt)
                        ),
                    contentDescription = belt.heb,
                    modifier =
                        Modifier
                            .width(112.dp)
                            .height(36.dp),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = belt.heb,
                    style =
                        KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Black
                        ),
                    color = accent,
                    textAlign = TextAlign.Center
                )

                if (!exerciseSubtitle.isNullOrBlank()) {
                    Text(
                        text = exerciseSubtitle,
                        style =
                            KmiTypography.secondary.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text = exerciseTitle,
                        modifier =
                            Modifier.fillMaxWidth(),
                        style =
                            KmiTypography.sectionTitle.copy(
                                fontWeight = FontWeight.Black
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        textAlign =
                            TextAlign.Center,
                        maxLines = 3
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                            .copy(alpha = 0.45f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = "${currentIndex + 1} מתוך $totalCount",
                        modifier =
                            Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 5.dp
                            ),
                        style =
                            KmiTypography.caption.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color =
                        accent.copy(
                            alpha = 0.10f
                        ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color =
                                accent.copy(
                                    alpha = 0.22f
                                )
                        )
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 6.dp
                            ),
                        horizontalArrangement =
                            Arrangement.Center,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏳",
                            style =
                                KmiTypography.secondary.copy(
                                    fontWeight =
                                        FontWeight.Black
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text = timeText,
                            style =
                                KmiTypography.metric.copy(
                                    fontWeight =
                                        FontWeight.Black
                                ),
                            color = accent
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onToggleRunning,
                        modifier =
                            Modifier.size(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .secondaryContainer
                                .copy(alpha = 0.85f),
                        contentColor =
                            MaterialTheme
                                .colorScheme
                                .onSecondaryContainer,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = accent.copy(alpha = 0.18f)
                            )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    if (isRunning) {
                                        Icons.Filled.Pause
                                    } else {
                                        Icons.Filled.PlayArrow
                                    },
                                contentDescription =
                                    if (isRunning) {
                                        "Pause"
                                    } else {
                                        "Resume"
                                    }
                            )
                        }
                    }

                    if (!centerLabel.isNullOrBlank()) {
                        Surface(
                            onClick = {
                                onCenterClick?.invoke()
                            },
                            enabled = false,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(46.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = statusBackgroundColor,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border =
                                BorderStroke(
                                    width = 1.dp,
                                    color =
                                        statusColor.copy(
                                            alpha = 0.42f
                                        )
                                )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = centerLabel,
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 10.dp
                                        ),
                                    style =
                                        KmiTypography.secondary.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                    color = statusColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Spacer(
                            modifier =
                                Modifier.weight(1f)
                        )
                    }

                    Surface(
                        onClick = onToggleMute,
                        modifier =
                            Modifier.size(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .secondaryContainer
                                .copy(alpha = 0.85f),
                        contentColor =
                            MaterialTheme
                                .colorScheme
                                .onSecondaryContainer,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = accent.copy(alpha = 0.18f)
                            )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    if (isMuted) {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    },
                                contentDescription =
                                    if (isMuted) {
                                        "Unmute"
                                    } else {
                                        "Mute"
                                    }
                            )
                        }
                    }
                }

            }
        }

        val safeTotalCount =
            totalCount.coerceAtLeast(1)

        val progressRows =
            if (safeTotalCount <= 12) {
                listOf(
                    (0 until safeTotalCount).toList()
                )
            } else {
                val firstRowSize =
                    (safeTotalCount + 1) / 2

                listOf(
                    (0 until firstRowSize).toList(),
                    (firstRowSize until safeTotalCount).toList()
                )
            }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 0.dp,
                        bottom = 16.dp
                    ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .offset(
                            y =
                                if (safeTotalCount > 12) {
                                    (-12).dp
                                } else {
                                    0.dp
                                }
                        ),
                shape = RoundedCornerShape(14.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .surface
                        .copy(alpha = 0.72f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .surface
                                .copy(alpha = 0.90f)
                    )
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 8.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    progressRows.forEach { rowIndexes ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(4.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            rowIndexes.forEach { index ->
                                val isCurrent =
                                    index == currentIndex

                                Box(
                                    modifier =
                                        Modifier
                                            .weight(
                                                if (isCurrent) {
                                                    4f
                                                } else {
                                                    1f
                                                }
                                            )
                                            .height(
                                                if (isCurrent) {
                                                    12.dp
                                                } else {
                                                    8.dp
                                                }
                                            )
                                            .clip(
                                                RoundedCornerShape(
                                                    999.dp
                                                )
                                            )
                                            .background(
                                                if (isCurrent) {
                                                    accent
                                                } else {
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurface
                                                        .copy(
                                                            alpha = 0.42f
                                                        )
                                                }
                                            )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun beltImageRes(
    belt: Belt
): Int =
    when (belt) {
        Belt.WHITE -> R.drawable.intro_belt_white
        Belt.YELLOW -> R.drawable.intro_belt_yellow
        Belt.ORANGE -> R.drawable.intro_belt_orange
        Belt.GREEN -> R.drawable.intro_belt_green
        Belt.BLUE -> R.drawable.intro_belt_blue
        Belt.BROWN -> R.drawable.intro_belt_brown
        Belt.BLACK -> R.drawable.intro_belt_black
    }