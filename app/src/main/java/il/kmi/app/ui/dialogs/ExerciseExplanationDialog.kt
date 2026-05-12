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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.StyledExplanationText

@Composable
fun ExerciseExplanationDialog(
    title: String,
    beltLabel: String,
    explanation: String,
    noteText: String,
    isFavorite: Boolean,
    accentColor: Color,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onEditNote: () -> Unit,
    onToggleFavorite: () -> Unit,
    backgroundBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color.White,
            lerp(Color.White, accentColor, 0.10f),
            lerp(Color.White, accentColor, 0.05f),
            Color.White
        )
    )
) {
    AlertDialog(
        modifier = Modifier
            .background(
                brush = backgroundBrush,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(24.dp)
            ),
        onDismissRequest = { },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        ),
        // חשוב: השקיפות נמצאת רק ב־overlay של המסך, לא בכרטיס עצמו.
        // הרקע של הכרטיס מגיע מ־backgroundBrush האטום.
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        title = {
            Column(
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = beltLabel,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                start = if (isEnglish) 10.dp else 0.dp,
                                end = if (isEnglish) 0.dp else 10.dp
                            ),
                        color = if (isEnglish) Color(0xFFB08900) else accentColor,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = onEditNote,
                            shape = RoundedCornerShape(12.dp),
                            color = if (noteText.isNotBlank()) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.White.copy(alpha = 0.72f)
                            },
                            border = BorderStroke(
                                1.dp,
                                accentColor.copy(alpha = 0.12f)
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = if (isEnglish) "Edit note" else "עריכת הערה",
                                    tint = if (noteText.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = onToggleFavorite,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.72f),
                            border = BorderStroke(
                                1.dp,
                                accentColor.copy(alpha = 0.12f)
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isFavorite) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = if (isEnglish) "Remove from favorites" else "הסר ממועדפים",
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.StarBorder,
                                        contentDescription = if (isEnglish) "Add to favorites" else "הוסף למועדפים",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
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
                StyledExplanationText(
                    raw = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1B1B1B)
                )

                if (noteText.isNotBlank()) {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.18f))

                    Text(
                        text = if (isEnglish) "Trainee note:" else "הערה של המתאמן:",
                        fontWeight = FontWeight.Bold,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        color = if (isEnglish) Color(0xFFB08900) else accentColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1B1B1B)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isEnglish) "Close" else "סגור",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )

    BackHandler(enabled = true) {
        onDismiss()
    }
}