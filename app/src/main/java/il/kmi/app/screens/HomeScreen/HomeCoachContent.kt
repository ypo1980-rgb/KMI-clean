package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.app.ui.scaledIconSize
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun HomeCoachNoticesCard(
    homeNotices: List<HomeNotice>,
    hasRecentCoachMessages: Boolean,
    isEnglish: Boolean,
    onOpen: () -> Unit
) {
    val clickSound =
        rememberClickSound()

    val haptic =
        rememberHapticsGlobal()

    val latestNotice =
        homeNotices.firstOrNull()

    val message =
        latestNotice
            ?.text
            ?.trim()

    val extraCount =
        (homeNotices.size - 1)
            .coerceAtLeast(0)

    Surface(
        onClick = {
            clickSound()
            haptic(true)
            onOpen()
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        shape =
            RoundedCornerShape(20.dp),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.96f),
        tonalElevation =
            0.dp,
        shadowElevation =
            0.dp,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.45f)
            )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Surface(
                shape =
                    CircleShape,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.14f),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(alpha = 0.35f)
                    ),
                modifier =
                    Modifier.size(
                        scaledIconSize(38.dp)
                    )
            ) {
                Icon(
                    imageVector =
                        Icons.Filled.Person,
                    contentDescription =
                        null,
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    modifier =
                        Modifier.padding(
                            scaledIconSize(8.dp)
                        )
                )
            }

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Messages & Events"
                            } else {
                                "הודעות ואירועים"
                            },
                        style =
                            KmiTypography.cardTitle,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier.weight(1f)
                    )

                    if (homeNotices.isNotEmpty()) {
                        Surface(
                            onClick = {
                                onOpen()
                            },
                            shape =
                                CircleShape,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                                    .copy(alpha = 0.14f),
                            border =
                                BorderStroke(
                                    width = 1.dp,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                            .copy(alpha = 0.40f)
                                ),
                            modifier =
                                Modifier.size(
                                    scaledIconSize(32.dp)
                                )
                        ) {
                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Filled.Email,
                                    contentDescription =
                                        if (isEnglish) {
                                            "Messages and events"
                                        } else {
                                            "הודעות ואירועים"
                                        },
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    modifier =
                                        Modifier.size(
                                            scaledIconSize(
                                                17.dp
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(
                    Modifier.size(4.dp)
                )

                if (message.isNullOrEmpty()) {
                    Text(
                        text =
                            if (isEnglish) {
                                "No new messages right now"
                            } else {
                                "אין הודעות חדשות כרגע"
                            },
                        style =
                            KmiTypography.body,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                } else {
                    Text(
                        text =
                            message,
                        style =
                            KmiTypography.body,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        maxLines =
                            2,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    val branchGroupLine =
                        buildString {
                            val branch =
                                latestNotice.branch
                                    .trim()

                            val group =
                                latestNotice.group
                                    .trim()

                            if (branch.isNotBlank()) {
                                append(
                                    if (isEnglish) {
                                        "Branch: "
                                    } else {
                                        "סניף: "
                                    }
                                )

                                append(branch)
                            }

                            if (group.isNotBlank()) {
                                if (isNotBlank()) {
                                    append(" · ")
                                }

                                append(
                                    if (isEnglish) {
                                        "Group: "
                                    } else {
                                        "קבוצה: "
                                    }
                                )

                                append(group)
                            }
                        }

                    if (branchGroupLine.isNotBlank()) {
                        Spacer(
                            Modifier.size(4.dp)
                        )

                        Text(
                            text =
                                branchGroupLine,
                            style =
                                KmiTypography.secondary,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            maxLines =
                                2,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(
                    Modifier.size(6.dp)
                )

                val timeText =
                    latestNotice
                        ?.sentAt
                        ?.let { sentAt ->
                            SimpleDateFormat(
                                "dd/MM/yyyy · HH:mm",
                                Locale("he", "IL")
                            ).format(sentAt)
                        }
                        .orEmpty()

                val openRecentText =
                    if (
                        isEnglish &&
                        extraCount > 0
                    ) {
                        "Open recent updates · +$extraCount more"
                    } else if (isEnglish) {
                        "Open recent updates"
                    } else {
                        "פתח הודעות ואירועים אחרונים"
                    }

                if (
                    timeText.isNotBlank() ||
                    hasRecentCoachMessages
                ) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        if (timeText.isNotBlank()) {
                            Text(
                                text =
                                    timeText,
                                style =
                                    KmiTypography.caption,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Left
                                    } else {
                                        TextAlign.Right
                                    },
                                maxLines =
                                    1,
                                overflow =
                                    TextOverflow.Ellipsis,
                                modifier =
                                    Modifier.fillMaxWidth()
                            )
                        }

                        if (hasRecentCoachMessages) {
                            Spacer(
                                Modifier.size(3.dp)
                            )

                            Text(
                                text =
                                    openRecentText,
                                style =
                                    KmiTypography
                                        .caption
                                        .copy(
                                            fontWeight =
                                                FontWeight.Bold
                                        ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary,
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Right
                                    } else {
                                        TextAlign.Left
                                    },
                                maxLines =
                                    1,
                                overflow =
                                    TextOverflow.Ellipsis,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clickSound()
                                            haptic(true)
                                            onOpen()
                                        }
                            )
                        }
                    }
                }
            }
        }
    }
}