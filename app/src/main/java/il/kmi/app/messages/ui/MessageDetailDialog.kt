package il.kmi.app.messages.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.messages.model.MessageCenterItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MessageDetailDialog(
    message: MessageCenterItem,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val textAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val title =
        if (isEnglish) {
            message.titleEn.ifBlank {
                message.senderNameEn.ifBlank {
                    "K.M.I Team"
                }
            }
        } else {
            message.titleHe.ifBlank {
                message.senderNameHe.ifBlank {
                    "צוות ק.מ.י"
                }
            }
        }

    val dateTimeText =
        if (message.createdAtMillis > 0L) {
            Instant
                .ofEpochMilli(message.createdAtMillis)
                .atZone(
                    ZoneId.of("Asia/Jerusalem")
                )
                .format(
                    DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy · HH:mm"
                    )
                )
        } else {
            ""
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = textAlign
                )
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                ) {
                    if (dateTimeText.isNotBlank()) {
                        Text(
                            text = dateTimeText,
                            modifier = Modifier.fillMaxWidth(),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium,
                            textAlign = textAlign
                        )

                        Text(
                            text = "",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall
                        )
                    }

                    Text(
                        text = message.message,
                        modifier = Modifier.fillMaxWidth(),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        textAlign = textAlign
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Close"
                            } else {
                                "סגירה"
                            }
                    )
                }
            },
            containerColor =
                MaterialTheme
                    .colorScheme
                    .surface
        )
    }
}