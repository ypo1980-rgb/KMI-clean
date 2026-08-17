package il.kmi.app.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import il.kmi.app.ui.KmiTypography
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
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(34.dp),
                clip = false
            )
            .background(
                brush = backgroundBrush,
                shape = RoundedCornerShape(34.dp)
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(34.dp)
            ),
        onDismissRequest = { },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        ),
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(34.dp),
        tonalElevation = 0.dp,
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.99f),
                                accentColor.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.98f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        1.dp,
                        accentColor.copy(alpha = 0.13f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = title,
                    style = KmiTypography.sectionTitle.copy(
                        fontWeight = FontWeight.Black
                    ),
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1F2937),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = beltLabel,
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Black
                        ),
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                start =
                                    if (isEnglish) {
                                        8.dp
                                    } else {
                                        0.dp
                                    },
                                end =
                                    if (isEnglish) {
                                        0.dp
                                    } else {
                                        8.dp
                                    }
                            ),
                        color =
                            if (isEnglish) {
                                Color(0xFFB08900)
                            } else {
                                accentColor
                            },
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = onEditNote,
                            shape = RoundedCornerShape(14.dp),
                            color = if (noteText.isNotBlank()) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.White.copy(alpha = 0.78f)
                            },
                            border = BorderStroke(
                                1.dp,
                                accentColor.copy(alpha = 0.13f)
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(32.dp)
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
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                localIsFavorite = !localIsFavorite
                                onToggleFavorite()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (localIsFavorite) {
                                Color(0xFFFFF8E1)
                            } else {
                                Color.White.copy(alpha = 0.78f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (localIsFavorite) {
                                    Color(0xFFFFC107).copy(alpha = 0.55f)
                                } else {
                                    accentColor.copy(alpha = 0.13f)
                                }
                            ),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(32.dp)
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
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.99f),
                                accentColor.copy(alpha = 0.035f),
                                Color.White.copy(alpha = 0.97f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        1.dp,
                        accentColor.copy(alpha = 0.11f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                StyledExplanationText(
                    raw = explanation,
                    style = KmiTypography.body,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
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
                                style = KmiTypography.cardTitle,
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
                                style = KmiTypography.cardTitle,
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
                        style = KmiTypography.body,
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1B1B1B)
                    )
                }
            }
        },
        confirmButton = {
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(18.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.18f)
                ),
                modifier = Modifier
                    .height(38.dp)
                    .widthIn(min = 96.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "Close" else "סגור",
                        style = KmiTypography.action.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = Color(0xFF6D5BA6)
                    )
                }
            }
        }
    )

    BackHandler(enabled = true) {
        onDismiss()
    }
}

@Composable
fun ExerciseNoteEditorDialog(
    exerciseTitle: String = "",
    noteText: String,
    isEnglish: Boolean = false,
    accentColor: Color,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        ),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(34.dp),
        title = null,
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(34.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(34.dp),
                color = Color.White.copy(alpha = 0.99f),
                border = BorderStroke(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.22f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    lerp(Color.White, accentColor, 0.08f),
                                    lerp(Color.White, accentColor, 0.16f)
                                )
                            )
                        )
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Exercise Note"
                            } else {
                                "הערה על התרגיל"
                            },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = KmiTypography.screenTitle.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = Color(0xFF1E2A3D)
                    )

                    if (exerciseTitle.isNotBlank()) {
                        Text(
                            text = exerciseTitle,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = KmiTypography.cardTitle.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = accentColor,
                            maxLines = 2
                        )
                    }

                    Text(
                        text =
                            if (isEnglish) {
                                "Write a personal note that will stay attached to this exercise"
                            } else {
                                "כתוב הערה אישית שתישמר לתרגיל הזה"
                            },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = KmiTypography.body.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color(0xFF64748B)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.94f),
                        shadowElevation = 8.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.20f)
                        )
                    ) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = onNoteChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 170.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            minLines = 5,
                            maxLines = 8,
                            textStyle = KmiTypography.body.copy(
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Left
                                    } else {
                                        TextAlign.Right
                                    },
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E2A3D)
                            ),
                            placeholder = {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Write a free note"
                                        } else {
                                            "הקלד הערה\nחופשית"
                                        },
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = KmiTypography.body.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = accentColor,
                                focusedTextColor = Color(0xFF1E2A3D),
                                unfocusedTextColor = Color(0xFF1E2A3D)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // בטל
                        Surface(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.80f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color(0xFF6D5BA6).copy(alpha = 0.22f)
                            ),
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription =
                                        if (isEnglish) "Cancel" else "בטל",
                                    tint = Color(0xFF6D5BA6),
                                    modifier = Modifier.size(17.dp)
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = if (isEnglish) "Cancel" else "בטל",
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Color(0xFF6D5BA6),
                                    maxLines = 1
                                )
                            }
                        }

                        // מחק
                        Surface(
                            onClick = {
                                if (noteText.isNotBlank()) {
                                    onNoteChange("")
                                    onDelete?.invoke()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            color =
                                if (noteText.isNotBlank()) {
                                    Color(0xFFFFEBEE)
                                } else {
                                    Color(0xFFF4F4F5)
                                },
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    if (noteText.isNotBlank()) {
                                        Color(0xFFE57373).copy(alpha = 0.42f)
                                    } else {
                                        Color(0xFFD4D4D8).copy(alpha = 0.55f)
                                    }
                            ),
                            shadowElevation =
                                if (noteText.isNotBlank()) {
                                    2.dp
                                } else {
                                    0.dp
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription =
                                        if (isEnglish) "Delete" else "מחק",
                                    tint =
                                        if (noteText.isNotBlank()) {
                                            Color(0xFFD32F2F)
                                        } else {
                                            Color(0xFFA1A1AA)
                                        },
                                    modifier = Modifier.size(17.dp)
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = if (isEnglish) "Delete" else "מחק",
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color =
                                        if (noteText.isNotBlank()) {
                                            Color(0xFFD32F2F)
                                        } else {
                                            Color(0xFFA1A1AA)
                                        },
                                    maxLines = 1
                                )
                            }
                        }

                        // שמור
                        Surface(
                            onClick = onSave,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = accentColor.copy(alpha = 0.86f),
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription =
                                        if (isEnglish) "Save" else "שמור",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = if (isEnglish) "Save" else "שמור",
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Color.White,
                                    maxLines = 1
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
