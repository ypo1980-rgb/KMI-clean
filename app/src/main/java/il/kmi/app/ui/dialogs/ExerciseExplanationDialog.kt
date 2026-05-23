package il.kmi.app.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onDeleteNote: () -> Unit = {},
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
    var localIsFavorite by remember(title, beltLabel) {
        mutableStateOf(isFavorite)
    }

    LaunchedEffect(isFavorite, title, beltLabel) {
        localIsFavorite = isFavorite
    }

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
                            onClick = {
                                localIsFavorite = !localIsFavorite
                                onToggleFavorite()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (localIsFavorite) {
                                Color(0xFFFFF8E1)
                            } else {
                                Color.White.copy(alpha = 0.72f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (localIsFavorite) {
                                    Color(0xFFFFC107).copy(alpha = 0.55f)
                                } else {
                                    accentColor.copy(alpha = 0.12f)
                                }
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (localIsFavorite) {
                                        Icons.Filled.Star
                                    } else {
                                        Icons.Outlined.StarBorder
                                    },
                                    contentDescription = if (localIsFavorite) {
                                        if (isEnglish) "Remove from favorites" else "הסר ממועדפים"
                                    } else {
                                        if (isEnglish) "Add to favorites" else "הוסף למועדפים"
                                    },
                                    tint = if (localIsFavorite) {
                                        Color(0xFFFFC107)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isEnglish) {
                            Arrangement.Start
                        } else {
                            Arrangement.End
                        }
                    ) {
                        if (isEnglish) {
                            Text(
                                text = "Trainee note:",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Left,
                                color = Color(0xFFB08900)
                            )

                            Spacer(Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = onEditNote,
                                    shape = RoundedCornerShape(10.dp),
                                    color = accentColor.copy(alpha = 0.10f),
                                    border = BorderStroke(
                                        1.dp,
                                        accentColor.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit note",
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onDeleteNote,
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFFEBEE),
                                    border = BorderStroke(
                                        1.dp,
                                        Color(0xFFE57373).copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete note",
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "הערה של המתאמן:",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                color = accentColor
                            )

                            Spacer(Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = onEditNote,
                                    shape = RoundedCornerShape(10.dp),
                                    color = accentColor.copy(alpha = 0.10f),
                                    border = BorderStroke(
                                        1.dp,
                                        accentColor.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "עריכת הערה",
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onDeleteNote,
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFFEBEE),
                                    border = BorderStroke(
                                        1.dp,
                                        Color(0xFFE57373).copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "מחיקת הערה",
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

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

@Composable
fun ExerciseNoteEditorDialog(
    noteText: String,
    isEnglish: Boolean = false,
    accentColor: Color,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        ),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(30.dp),
        title = null,
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(30.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(30.dp),
                color = Color.White.copy(alpha = 0.98f),
                border = BorderStroke(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.18f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.99f),
                                    accentColor.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.96f)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Exercise Note" else "הערה על התרגיל",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F2937)
                    )

                    Text(
                        text = if (isEnglish) {
                            "Write a personal note that will stay attached to this exercise."
                        } else {
                            "כתוב הערה אישית שתישמר לתרגיל הזה"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.96f),
                        shadowElevation = 7.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.22f)
                        )
                    ) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = onNoteChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            minLines = 4,
                            maxLines = 7,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            ),
                            placeholder = {
                                Text(
                                    text = if (isEnglish) "Write a free note" else "הקלד הערה חופשית",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = accentColor
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Cancel" else "בטל",
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6D5BA6),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Surface(
                            onClick = onSave,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = accentColor,
                            shadowElevation = 8.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEnglish) "Save" else "שמור",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )

    BackHandler(enabled = true) {
        onDismiss()
    }
}
