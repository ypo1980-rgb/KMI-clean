package il.kmi.app.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ExerciseExplanationDialog(
    title: String,
    beltLabel: String,
    explanation: String,
    noteText: String,
    isFavorite: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onEditNote: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        ),
        containerColor = Color.White.copy(alpha = 0.98f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.99f),
                                accentColor.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.97f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .border(
                        1.dp,
                        accentColor.copy(alpha = 0.12f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.align(AbsoluteAlignment.CenterLeft),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEditNote) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "עריכת הערה",
                            tint = if (noteText.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    IconButton(onClick = onToggleFavorite) {
                        if (isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "הסר ממועדפים",
                                tint = Color(0xFFFFC107)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.StarBorder,
                                contentDescription = "הוסף למועדפים"
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(AbsoluteAlignment.CenterRight)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = beltLabel,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        color = accentColor
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.98f),
                                accentColor.copy(alpha = 0.04f),
                                Color.White.copy(alpha = 0.96f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .border(
                        1.dp,
                        accentColor.copy(alpha = 0.10f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1B1B1B)
                )

                if (noteText.isNotBlank()) {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.18f))

                    Text(
                        text = "הערה של המתאמן:",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        color = accentColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1B1B1B)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("סגור")
            }
        }
    )

    BackHandler(enabled = true) {
        onDismiss()
    }
}