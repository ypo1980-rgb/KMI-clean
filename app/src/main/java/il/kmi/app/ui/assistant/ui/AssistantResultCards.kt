package il.kmi.app.ui.assistant.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.assistant.core.AssistantResultItem

@Composable
internal fun AssistantTrainingCard(
    item: AssistantResultItem,
    isEnglish: Boolean
) {
    val statusCode =
        item.trainingStatusCode
            ?.trim()
            ?.uppercase()
            .orEmpty()

    val statusColor =
        when (statusCode) {
            "ONGOING" ->
                Color(0xFF047857)

            "CANCELLED_BY_HOLIDAY" ->
                Color(0xFFEA580C)

            "COMPLETED" ->
                Color(0xFF64748B)

            "INVALID" ->
                Color(0xFFDC2626)

            else ->
                Color(0xFF2563EB)
        }

    val statusBackground =
        statusColor.copy(alpha = 0.14f)

    val statusText =
        item.trainingStatusText(
            isEnglish
        ).orEmpty()

    val timeText =
        buildString {
            item.startTime
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append(it)
                }

            item.endTime
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    if (isNotBlank()) {
                        append("–")
                    }

                    append(it)
                }
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = statusColor.copy(
                    alpha = 0.28f
                ),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = KmiTypography.cardTitle,
                textAlign = if (isEnglish) {
                    TextAlign.Left
                } else {
                    TextAlign.Right
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (timeText.isNotBlank()) {
                Text(
                    text = if (isEnglish) {
                        "Time: $timeText"
                    } else {
                        "שעה: $timeText"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = KmiTypography.secondary.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item.branchName
                ?.takeIf { it.isNotBlank() }
                ?.let { branch ->
                    Text(
                        text = if (isEnglish) {
                            "Branch: $branch"
                        } else {
                            "סניף: $branch"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = KmiTypography.secondary,
                        textAlign = if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

            item.groupName
                ?.takeIf { it.isNotBlank() }
                ?.let { group ->
                    Text(
                        text = if (isEnglish) {
                            "Group: $group"
                        } else {
                            "קבוצה: $group"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = KmiTypography.secondary,
                        textAlign = if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

            item.location
                ?.takeIf { it.isNotBlank() }
                ?.let { location ->
                    Text(
                        text = if (isEnglish) {
                            "Location: $location"
                        } else {
                            "מיקום: $location"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = KmiTypography.secondary,
                        textAlign = if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

            item.coachName
                ?.takeIf { it.isNotBlank() }
                ?.let { coach ->
                    Text(
                        text = if (isEnglish) {
                            "Coach: $coach"
                        } else {
                            "מאמן: $coach"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = KmiTypography.secondary,
                        textAlign = if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

            if (statusText.isNotBlank()) {
                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(999.dp),
                    color = statusBackground
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Black
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun AssistantMaterialCard(
    item: AssistantResultItem,
    index: Int,
    isEnglish: Boolean
) {
    val secondaryText =
        item.subtitle
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: item.topicName
                ?.trim()
                ?.takeIf { it.isNotBlank() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF2563EB).copy(
                    alpha = 0.25f
                ),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 11.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp),
            verticalAlignment =
                Alignment.Top
        ) {
            if (isEnglish) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = Color(0xFF2563EB),
                            style =
                                KmiTypography.caption.copy(
                                    fontWeight =
                                        FontWeight.Black
                                )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF172033),
                    style =
                        KmiTypography.cardTitle.copy(
                            fontWeight =
                                FontWeight.ExtraBold
                        ),
                    textAlign = if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    }
                )

                secondaryText?.let { details ->
                    Text(
                        text = details,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF526079),
                        style =
                            KmiTypography.secondary.copy(
                                fontWeight =
                                    FontWeight.Medium
                            ),
                        textAlign = if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        }
                    )
                }

                item.description
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF64748B),
                            style =
                                KmiTypography.secondary,
                            textAlign = if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            }
                        )
                    }
            }

            if (!isEnglish) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = Color(0xFF2563EB),
                            style =
                                KmiTypography.caption.copy(
                                    fontWeight =
                                        FontWeight.Black
                                )
                        )
                    }
                }
            }
        }
    }
}