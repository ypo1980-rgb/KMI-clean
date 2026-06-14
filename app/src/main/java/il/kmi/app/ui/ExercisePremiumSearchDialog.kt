package il.kmi.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val FALLBACK_PREFIX_HE = "הסבר מפורט על:"
private const val FALLBACK_PREFIX_EN = "Detailed explanation for:"

private fun beltAccentColorFromName(beltName: String): Color {
    val clean = beltName.trim().lowercase()

    return when {
        clean.contains("צהובה") || clean.contains("yellow") -> Color(0xFFD6A600)
        clean.contains("כתומה") || clean.contains("orange") -> Color(0xFFF97316)
        clean.contains("ירוקה") || clean.contains("green") -> Color(0xFF16A34A)
        clean.contains("כחולה") || clean.contains("blue") -> Color(0xFF2563EB)
        clean.contains("חומה") || clean.contains("brown") -> Color(0xFF8B5A2B)
        clean.contains("שחורה") || clean.contains("black") -> Color(0xFF111827)
        clean.contains("לבנה") || clean.contains("white") -> Color(0xFF64748B)
        else -> Color(0xFF6D5BA8)
    }
}

private fun beltSoftColorFromName(beltName: String): Color {
    val clean = beltName.trim().lowercase()

    return when {
        clean.contains("צהובה") || clean.contains("yellow") -> Color(0xFFFFF9C4)
        clean.contains("כתומה") || clean.contains("orange") -> Color(0xFFFFEDD5)
        clean.contains("ירוקה") || clean.contains("green") -> Color(0xFFEAF8EF)
        clean.contains("כחולה") || clean.contains("blue") -> Color(0xFFEAF4FF)
        clean.contains("חומה") || clean.contains("brown") -> Color(0xFFF3E4D1)
        clean.contains("שחורה") || clean.contains("black") -> Color(0xFFE5E7EB)
        clean.contains("לבנה") || clean.contains("white") -> Color(0xFFF8FAFC)
        else -> Color(0xFFF4F0FF)
    }
}

private fun beltVerySoftColorFromName(beltName: String): Color {
    val clean = beltName.trim().lowercase()

    return when {
        clean.contains("צהובה") || clean.contains("yellow") -> Color(0xFFFFFDEB)
        clean.contains("כתומה") || clean.contains("orange") -> Color(0xFFFFF7ED)
        clean.contains("ירוקה") || clean.contains("green") -> Color(0xFFF1FFF6)
        clean.contains("כחולה") || clean.contains("blue") -> Color(0xFFF3F8FF)
        clean.contains("חומה") || clean.contains("brown") -> Color(0xFFFFF8EF)
        clean.contains("שחורה") || clean.contains("black") -> Color(0xFFF4F4F5)
        clean.contains("לבנה") || clean.contains("white") -> Color.White
        else -> Color(0xFFFAF7FF)
    }
}

fun buildMissingExerciseExplanationText(
    exerciseTitle: String,
    isEnglish: Boolean
): String {
    val cleanTitle = exerciseTitle
        .trim()
        .ifBlank {
            if (isEnglish) "Unknown exercise" else "תרגיל לא מזוהה"
        }

    return if (isEnglish) {
        "There is currently no explanation for this exercise.\n\nExercise name:\n$cleanTitle"
    } else {
        "אין כרגע הסבר לתרגיל הזה.\n\nשם התרגיל:\n$cleanTitle"
    }
}

fun normalizeExerciseExplanationForDisplay(
    rawExplanation: String,
    fallbackExerciseTitle: String,
    isEnglish: Boolean
): String {
    val clean = rawExplanation.trim()

    if (clean.isBlank()) {
        return buildMissingExerciseExplanationText(
            exerciseTitle = fallbackExerciseTitle,
            isEnglish = isEnglish
        )
    }

    val isFallback =
        clean.startsWith(FALLBACK_PREFIX_HE) ||
                clean.startsWith(FALLBACK_PREFIX_EN) ||
                clean.startsWith("אין כרגע") ||
                clean.startsWith("There is currently no explanation")

    if (!isFallback) {
        return clean
    }

    val extractedTitle = clean
        .removePrefix(FALLBACK_PREFIX_HE)
        .removePrefix(FALLBACK_PREFIX_EN)
        .replace("There is currently no explanation for this exercise.", "")
        .replace("אין כרגע הסבר לתרגיל הזה.", "")
        .replace("Exercise name:", "")
        .replace("שם התרגיל:", "")
        .trim()
        .ifBlank { fallbackExerciseTitle }

    return buildMissingExerciseExplanationText(
        exerciseTitle = extractedTitle,
        isEnglish = isEnglish
    )
}

private fun parseRedBoldTags(text: String): AnnotatedString {
    val startTag = "[[RED_BOLD]]"
    val endTag = "[[/RED_BOLD]]"

    val cleanText = text
        .replace("[[RED_BOLD/]]", "[[/RED_BOLD]]")
        .replace("[[/RED_BOLD/]]", "[[/RED_BOLD]]")
        .replace("[[ /RED_BOLD]]", "[[/RED_BOLD]]")

    return buildAnnotatedString {
        var index = 0

        while (index < cleanText.length) {
            val start = cleanText.indexOf(startTag, index)

            if (start == -1) {
                append(cleanText.substring(index))
                break
            }

            append(cleanText.substring(index, start))

            val contentStart = start + startTag.length
            val end = cleanText.indexOf(endTag, contentStart)

            if (end == -1) {
                append(text.substring(start).replace(startTag, ""))
                break
            }

            val highlighted = cleanText.substring(contentStart, end)

            pushStyle(
                SpanStyle(
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.ExtraBold
                )
            )
            append(highlighted)
            pop()

            index = end + endTag.length
        }
    }
}

@Composable
fun ExercisePremiumSearchDialog(
    title: String,
    beltName: String,
    explanation: String,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteIcon: Boolean = true,
    showEditIcon: Boolean = true,
    isFavorite: Boolean = false,
    userNoteTitle: String = "",
    userNote: String = "",
    onFavoriteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onUserNoteEditClick: (() -> Unit)? = null,
    onUserNoteDeleteClick: (() -> Unit)? = null
) {
    val normalizedExplanation = normalizeExerciseExplanationForDisplay(
        rawExplanation = explanation,
        fallbackExerciseTitle = title,
        isEnglish = isEnglish
    )

    val textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val beltAccentColor = beltAccentColorFromName(beltName)
    val beltSoftColor = beltSoftColorFromName(beltName)
    val beltVerySoftColor = beltVerySoftColorFromName(beltName)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 18.dp,
        border = BorderStroke(
            width = 1.dp,
            color = beltAccentColor.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            beltVerySoftColor,
                            beltSoftColor
                        )
                    )
                )
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val shouldShowFavoriteIcon = showFavoriteIcon && onFavoriteClick != null
            val shouldShowEditIcon = showEditIcon && onEditClick != null

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = horizontalAlignment
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = textAlign,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E2A3D),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (beltName.isNotBlank() || shouldShowFavoriteIcon || shouldShowEditIcon) {
                        Spacer(Modifier.height(2.dp))

                        androidx.compose.runtime.CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Ltr
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (shouldShowEditIcon) {
                                        DialogSmallActionIcon(
                                            icon = Icons.Filled.Edit,
                                            contentDescription = if (isEnglish) "Edit" else "עריכה",
                                            onClick = onEditClick
                                        )
                                    }

                                    if (shouldShowFavoriteIcon) {
                                        DialogSmallActionIcon(
                                            icon = if (isFavorite) {
                                                Icons.Filled.Star
                                            } else {
                                                Icons.Filled.StarBorder
                                            },
                                            contentDescription = if (isFavorite) {
                                                if (isEnglish) "Remove from favorites" else "הסר מהמועדפים"
                                            } else {
                                                if (isEnglish) "Add to favorites" else "הוסף למועדפים"
                                            },
                                            tint = if (isFavorite) {
                                                Color(0xFFFFB300)
                                            } else {
                                                Color(0xFF4F46E5)
                                            },
                                            onClick = onFavoriteClick
                                        )
                                    }
                                }

                                if (beltName.isNotBlank()) {
                                    Text(
                                        text = beltName,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 66.dp),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = beltAccentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // האייקונים נמצאים בצד שמאל פיזי באמצעות LayoutDirection.Ltr

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.96f),
                border = BorderStroke(
                    width = 1.dp,
                    color = beltAccentColor.copy(alpha = 0.18f)
                ),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = parseRedBoldTags(normalizedExplanation),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    textAlign = textAlign,
                    style = TextStyle(
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
            }

            if (userNote.trim().isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = beltSoftColor.copy(alpha = 0.72f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = beltAccentColor.copy(alpha = 0.22f)
                    ),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalAlignment = horizontalAlignment,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = userNoteTitle,
                                modifier = Modifier.weight(1f),
                                textAlign = textAlign,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = beltAccentColor
                            )

                            if (onUserNoteEditClick != null) {
                                DialogSmallActionIcon(
                                    icon = Icons.Filled.Edit,
                                    contentDescription = if (isEnglish) "Edit note" else "ערוך הערה",
                                    tint = Color(0xFF4F46E5),
                                    onClick = onUserNoteEditClick
                                )
                            }

                            if (onUserNoteDeleteClick != null) {
                                DialogSmallActionIcon(
                                    icon = Icons.Filled.Delete,
                                    contentDescription = if (isEnglish) "Delete note" else "מחק הערה",
                                    tint = Color(0xFFDC2626),
                                    onClick = onUserNoteDeleteClick
                                )
                            }
                        }

                        Text(
                            text = userNote.trim(),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = textAlign,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        )
                    }
                }
            }

            Surface(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                color = beltSoftColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = beltAccentColor.copy(alpha = 0.22f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) "Close" else "סגור",
                        color = beltAccentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun ExercisePremiumNoteEditorDialog(
    noteTitle: String,
    noteText: String,
    beltName: String,
    isEnglish: Boolean,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val beltAccentColor = beltAccentColorFromName(beltName)
    val beltSoftColor = beltSoftColorFromName(beltName)
    val beltVerySoftColor = beltVerySoftColorFromName(beltName)

    val textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(34.dp),
        title = null,
        text = {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                shape = RoundedCornerShape(34.dp),
                color = Color.White,
                shadowElevation = 22.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = beltAccentColor.copy(alpha = 0.24f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    beltVerySoftColor,
                                    beltSoftColor
                                )
                            )
                        )
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = horizontalAlignment,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = noteTitle.ifBlank {
                            if (isEnglish) "Trainee notes" else "הערות המתאמן"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 15.sp,
                            lineHeight = 19.sp
                        ),
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E2A3D)
                    )

                    Text(
                        text = if (isEnglish) {
                            "Write a personal note for this exercise"
                        } else {
                            "כתוב הערה אישית שתישמר לתרגיל הזה"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF64748B)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.96f),
                        shadowElevation = 8.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = beltAccentColor.copy(alpha = 0.18f)
                        )
                    ) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = onNoteChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 178.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            minLines = 5,
                            maxLines = 9,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                textAlign = textAlign,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color(0xFF1E2A3D)
                            ),
                            placeholder = {
                                Text(
                                    text = if (isEnglish) {
                                        "Write a free note..."
                                    } else {
                                        "הקלד הערה\nחופשית"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color(0xFF1E2A3D),
                                unfocusedTextColor = Color(0xFF1E2A3D),
                                cursorColor = beltAccentColor
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.82f),
                            shadowElevation = 3.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color(0xFF6D5BA6).copy(alpha = 0.18f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEnglish) "Cancel" else "ביטול",
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF6D5BA6),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        lineHeight = 17.sp
                                    )
                                )
                            }
                        }

                        Surface(
                            onClick = onSave,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = beltAccentColor.copy(alpha = 0.82f),
                            shadowElevation = 3.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color = beltAccentColor.copy(alpha = 0.22f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEnglish) "Save" else "שמור",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        lineHeight = 17.sp
                                    )
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
}

    @Composable
    private fun DialogSmallActionIcon(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        contentDescription: String,
        tint: Color = Color(0xFF4F46E5),
        onClick: () -> Unit
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 3.dp,
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFFE5EAF3)
            )
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
