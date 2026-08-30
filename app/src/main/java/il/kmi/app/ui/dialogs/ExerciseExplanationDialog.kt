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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.LocalAppIconScale
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
    backgroundBrush: Brush? = null
) {
    /*
     * מקור אמת גלובלי לצבע החגורה.
     *
     * כל מסך שפותח ExerciseExplanationDialog מקבל
     * אוטומטית את צבע החגורה והרקע המתאים,
     * בלי לבצע מיפוי מקומי בכל מסך.
     */
    val resolvedAccentColor =
        remember(beltLabel, accentColor) {
            when {
                beltLabel.contains("לבנה") ||
                        beltLabel.contains(
                            "White",
                            ignoreCase = true
                        ) ->
                    Color(0xFFD1D5DB)

                beltLabel.contains("צהובה") ||
                        beltLabel.contains(
                            "Yellow",
                            ignoreCase = true
                        ) ->
                    Color(0xFFFACC15)

                beltLabel.contains("כתומה") ||
                        beltLabel.contains(
                            "Orange",
                            ignoreCase = true
                        ) ->
                    Color(0xFFF97316)

                beltLabel.contains("ירוקה") ||
                        beltLabel.contains(
                            "Green",
                            ignoreCase = true
                        ) ->
                    Color(0xFF22C55E)

                beltLabel.contains("כחולה") ||
                        beltLabel.contains(
                            "Blue",
                            ignoreCase = true
                        ) ->
                    Color(0xFF3B82F6)

                beltLabel.contains("חומה") ||
                        beltLabel.contains(
                            "Brown",
                            ignoreCase = true
                        ) ->
                    Color(0xFF8B5A2B)

                beltLabel.contains("שחורה") ||
                        beltLabel.contains(
                            "Black",
                            ignoreCase = true
                        ) ->
                    Color(0xFF111111)

                else ->
                    accentColor
            }
        }

    val resolvedBackgroundBrush =
        backgroundBrush
            ?: Brush.verticalGradient(
                colors = listOf(
                    Color.White,
                    lerp(
                        Color.White,
                        resolvedAccentColor,
                        0.10f
                    ),
                    lerp(
                        Color.White,
                        resolvedAccentColor,
                        0.05f
                    ),
                    Color.White
                )
            )

    var localIsFavorite by remember(title, beltLabel) {
        mutableStateOf(isFavorite)
    }

    LaunchedEffect(isFavorite, title, beltLabel) {
        localIsFavorite = isFavorite
    }

    val isDarkTheme =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val dialogBackground =
        if (isDarkTheme) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.White
        }

    val cardBackground =
        if (isDarkTheme) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        } else {
            Color.White.copy(alpha = 0.96f)
        }

    val primaryTextColor =
        MaterialTheme.colorScheme.onSurface

    /*
     * בחירת גודל הכתב של המשתמש:
     * SMALL  = 0.80
     * MEDIUM = 1.00
     * LARGE  = 1.15
     *
     * LocalAppIconScale מגיע מאותו AppFontSize גלובלי,
     * ולכן אין כאן מקור אמת נוסף.
     */
    val userFontScale =
        LocalAppIconScale.current

    fun scaledTextStyle(
        base: androidx.compose.ui.text.TextStyle
    ): androidx.compose.ui.text.TextStyle {
        return base.copy(
            fontSize = base.fontSize * userFontScale,
            lineHeight = base.lineHeight * userFontScale
        )
    }

    val softBorderColor =
        resolvedAccentColor.copy(
            alpha = if (isDarkTheme) 0.34f else 0.16f
        )

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        AlertDialog(
            modifier = Modifier
                .shadow(
                    elevation = if (isDarkTheme) 10.dp else 22.dp,
                    shape = RoundedCornerShape(34.dp),
                    clip = false
                )
                .then(
                    if (isDarkTheme) {
                        Modifier.background(
                            color = dialogBackground,
                            shape = RoundedCornerShape(34.dp)
                        )
                    } else {
                        Modifier.background(
                            brush = resolvedBackgroundBrush,
                            shape = RoundedCornerShape(34.dp)
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = softBorderColor,
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
                        color = cardBackground,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = softBorderColor,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = title,
                    style = scaledTextStyle(
                        KmiTypography.sectionTitle
                    ).copy(
                        fontWeight = FontWeight.Black
                    ),
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryTextColor,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = beltLabel,
                        style = scaledTextStyle(
                            KmiTypography.body
                        ).copy(
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
                            if (isDarkTheme) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                resolvedAccentColor
                            },
                        maxLines = 2
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = onEditNote,
                            shape = RoundedCornerShape(14.dp),
                            color =
                                if (noteText.isNotBlank()) {
                                    if (isDarkTheme) {
                                        resolvedAccentColor.copy(alpha = 0.24f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    }
                                } else {
                                    if (isDarkTheme) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        Color.White.copy(alpha = 0.78f)
                                    }
                                },
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    if (isDarkTheme) {
                                        MaterialTheme.colorScheme.outlineVariant
                                    } else {
                                        resolvedAccentColor.copy(alpha = 0.13f)
                                    }
                            ),
                            shadowElevation = if (isDarkTheme) 0.dp else 2.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription =
                                        if (isEnglish) {
                                            "Edit note"
                                        } else {
                                            "עריכת הערה"
                                        },
                                    tint =
                                        if (noteText.isNotBlank()) {
                                            if (isDarkTheme) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                localIsFavorite = !localIsFavorite
                                onToggleFavorite()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color =
                                if (localIsFavorite) {
                                    if (isDarkTheme) {
                                        Color(0xFFFFC107).copy(alpha = 0.18f)
                                    } else {
                                        Color(0xFFFFF8E1)
                                    }
                                } else {
                                    if (isDarkTheme) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        Color.White.copy(alpha = 0.78f)
                                    }
                                },
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    if (localIsFavorite) {
                                        Color(0xFFFFC107).copy(
                                            alpha = if (isDarkTheme) 0.72f else 0.55f
                                        )
                                    } else {
                                        if (isDarkTheme) {
                                            MaterialTheme.colorScheme.outlineVariant
                                        } else {
                                            resolvedAccentColor.copy(alpha = 0.13f)
                                        }
                                    }
                            ),
                            shadowElevation = if (isDarkTheme) 0.dp else 2.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector =
                                        if (localIsFavorite) {
                                            Icons.Filled.Star
                                        } else {
                                            Icons.Outlined.StarBorder
                                        },
                                    contentDescription =
                                        if (localIsFavorite) {
                                            if (isEnglish) {
                                                "Remove from favorites"
                                            } else {
                                                "הסר ממועדפים"
                                            }
                                        } else {
                                            if (isEnglish) {
                                                "Add to favorites"
                                            } else {
                                                "הוסף למועדפים"
                                            }
                                        },
                                    tint =
                                        if (localIsFavorite) {
                                            Color(0xFFFFC107)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    modifier = Modifier.size(21.dp)
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
                        color = cardBackground,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = softBorderColor,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                StyledExplanationText(
                    raw = explanation,
                    style = scaledTextStyle(
                        KmiTypography.body
                    ),
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryTextColor
                )

                if (noteText.isNotBlank()) {
                    HorizontalDivider(
                        color = resolvedAccentColor.copy(alpha = 0.18f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            if (isEnglish) {
                                Arrangement.Start
                            } else {
                                Arrangement.End
                            }
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Trainee note:"
                                } else {
                                    "הערה של המתאמן:"
                                },
                            style = scaledTextStyle(
                                KmiTypography.cardTitle
                            ).copy(
                                fontWeight = FontWeight.Black
                            ),
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Left
                                } else {
                                    TextAlign.Right
                                },
                            color =
                                if (isDarkTheme) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    resolvedAccentColor
                                }
                        )

                        Spacer(Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // עריכת הערה
                            Surface(
                                onClick = onEditNote,
                                shape = RoundedCornerShape(10.dp),
                                color =
                                    if (isDarkTheme) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        resolvedAccentColor.copy(alpha = 0.10f)
                                    },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        if (isDarkTheme) {
                                            MaterialTheme.colorScheme.outlineVariant
                                        } else {
                                            resolvedAccentColor.copy(alpha = 0.16f)
                                        }
                                ),
                                shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription =
                                            if (isEnglish) {
                                                "Edit note"
                                            } else {
                                                "עריכת הערה"
                                            },
                                        tint =
                                            if (isDarkTheme) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                resolvedAccentColor
                                            },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // מחיקת הערה
                            Surface(
                                onClick = onDeleteNote,
                                shape = RoundedCornerShape(10.dp),
                                color =
                                    if (isDarkTheme) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        Color(0xFFFFEBEE)
                                    },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        if (isDarkTheme) {
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                                        } else {
                                            Color(0xFFE57373).copy(alpha = 0.35f)
                                        }
                                ),
                                shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription =
                                            if (isEnglish) {
                                                "Delete note"
                                            } else {
                                                "מחיקת הערה"
                                            },
                                        tint =
                                            if (isDarkTheme) {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            } else {
                                                Color(0xFFD32F2F)
                                            },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = noteText,
                        style = scaledTextStyle(
                            KmiTypography.body
                        ),
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        modifier = Modifier.fillMaxWidth(),
                        color = primaryTextColor
                    )
                }
            }
        },
            confirmButton = {
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(18.dp),
                    color =
                        if (isDarkTheme) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            resolvedAccentColor.copy(alpha = 0.12f)
                        },
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            if (isDarkTheme) {
                                MaterialTheme.colorScheme.outlineVariant
                            } else {
                                resolvedAccentColor.copy(alpha = 0.18f)
                            }
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .widthIn(min = 104.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 18.dp,
                                vertical = 8.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Close"
                                } else {
                                    "סגור"
                                },
                            style = scaledTextStyle(
                                KmiTypography.action
                            ).copy(
                                fontWeight = FontWeight.Black
                            ),
                            color =
                                if (isDarkTheme) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    resolvedAccentColor
                                },
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
    )

        BackHandler(enabled = true) {
            onDismiss()
        }
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
    val isDarkTheme =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val dialogBackground =
        if (isDarkTheme) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.White
        }

    val cardBackground =
        if (isDarkTheme) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
        } else {
            Color.White.copy(alpha = 0.96f)
        }

    val primaryTextColor =
        MaterialTheme.colorScheme.onSurface

    val secondaryTextColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    /*
     * בחירת גודל הכתב של המשתמש:
     * SMALL  = 0.80
     * MEDIUM = 1.00
     * LARGE  = 1.15
     */
    val userFontScale =
        LocalAppIconScale.current

    fun scaledTextStyle(
        base: androidx.compose.ui.text.TextStyle
    ): androidx.compose.ui.text.TextStyle {
        return base.copy(
            fontSize = base.fontSize * userFontScale,
            lineHeight = base.lineHeight * userFontScale
        )
    }

    val softBorderColor =
        accentColor.copy(
            alpha = if (isDarkTheme) 0.34f else 0.20f
        )

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
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
                        elevation = if (isDarkTheme) 10.dp else 22.dp,
                        shape = RoundedCornerShape(34.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(34.dp),
                color = dialogBackground,
                border = BorderStroke(
                    width = 1.dp,
                    color = softBorderColor
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .background(
                            color =
                                if (isDarkTheme) {
                                    dialogBackground
                                } else {
                                    lerp(
                                        Color.White,
                                        accentColor,
                                        0.06f
                                    )
                                }
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
                        style = scaledTextStyle(
                            KmiTypography.screenTitle
                        ).copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = primaryTextColor
                    )

                    if (exerciseTitle.isNotBlank()) {
                        Text(
                            text = exerciseTitle,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = scaledTextStyle(
                                KmiTypography.cardTitle
                            ).copy(
                                fontWeight = FontWeight.Black
                            ),
                            color =
                                if (isDarkTheme) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    accentColor
                                },
                            maxLines = 3
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
                        style = scaledTextStyle(
                            KmiTypography.body
                        ).copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = secondaryTextColor
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = cardBackground,
                        shadowElevation = if (isDarkTheme) 2.dp else 8.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = softBorderColor
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
                            textStyle = scaledTextStyle(
                                KmiTypography.body
                            ).copy(
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Left
                                    } else {
                                        TextAlign.Right
                                    },
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryTextColor
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
                                    style = scaledTextStyle(
                                        KmiTypography.body
                                    ).copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = secondaryTextColor
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = accentColor,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor
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
                                .heightIn(min = 50.dp),
                            shape = RoundedCornerShape(16.dp),
                            color =
                                if (isDarkTheme) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    Color.White.copy(alpha = 0.80f)
                                },
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    if (isDarkTheme) {
                                        MaterialTheme.colorScheme.outlineVariant
                                    } else {
                                        Color(0xFF6D5BA6).copy(alpha = 0.22f)
                                    }
                            ),
                            shadowElevation = if (isDarkTheme) 1.dp else 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 6.dp,
                                        vertical = 7.dp
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription =
                                        if (isEnglish) "Cancel" else "בטל",
                                    tint =
                                        if (isDarkTheme) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            Color(0xFF6D5BA6)
                                        },
                                    modifier = Modifier.size(17.dp)
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = if (isEnglish) "Cancel" else "בטל",
                                    style = scaledTextStyle(
                                        KmiTypography.action
                                    ).copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color =
                                        if (isDarkTheme) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            Color(0xFF6D5BA6)
                                        },
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
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
                                .heightIn(min = 50.dp),
                            shape = RoundedCornerShape(16.dp),
                            color =
                                when {
                                    noteText.isNotBlank() && isDarkTheme ->
                                        MaterialTheme.colorScheme.errorContainer

                                    noteText.isNotBlank() ->
                                        Color(0xFFFFEBEE)

                                    isDarkTheme ->
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

                                    else ->
                                        Color(0xFFF4F4F5)
                                },
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    when {
                                        noteText.isNotBlank() && isDarkTheme ->
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.38f)

                                        noteText.isNotBlank() ->
                                            Color(0xFFE57373).copy(alpha = 0.42f)

                                        isDarkTheme ->
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

                                        else ->
                                            Color(0xFFD4D4D8).copy(alpha = 0.55f)
                                    }
                            ),
                            shadowElevation =
                                if (noteText.isNotBlank() && !isDarkTheme) {
                                    2.dp
                                } else {
                                    0.dp
                                }
                        ) {
                            val deleteContentColor =
                                when {
                                    noteText.isNotBlank() && isDarkTheme ->
                                        MaterialTheme.colorScheme.onErrorContainer

                                    noteText.isNotBlank() ->
                                        Color(0xFFD32F2F)

                                    isDarkTheme ->
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)

                                    else ->
                                        Color(0xFFA1A1AA)
                                }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 6.dp,
                                        vertical = 7.dp
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription =
                                        if (isEnglish) "Delete" else "מחק",
                                    tint = deleteContentColor,
                                    modifier = Modifier.size(17.dp)
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = if (isEnglish) "Delete" else "מחק",
                                    style = scaledTextStyle(
                                        KmiTypography.action
                                    ).copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = deleteContentColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }

                        // שמור
                        val saveButtonColor =
                            if (isDarkTheme) {
                                lerp(
                                    MaterialTheme.colorScheme.primary,
                                    accentColor,
                                    0.22f
                                )
                            } else {
                                accentColor.copy(alpha = 0.86f)
                            }

                        val saveContentColor =
                            if (isDarkTheme) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                Color.White
                            }

                        Surface(
                            onClick = onSave,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = saveButtonColor,
                            border =
                                if (isDarkTheme) {
                                    BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(
                                            alpha = 0.18f
                                        )
                                    )
                                } else {
                                    null
                                },
                            shadowElevation = if (isDarkTheme) 2.dp else 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 6.dp,
                                        vertical = 7.dp
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription =
                                        if (isEnglish) {
                                            "Save"
                                        } else {
                                            "שמור"
                                        },
                                    tint = saveContentColor,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Save"
                                        } else {
                                            "שמור"
                                        },
                                    style = scaledTextStyle(
                                        KmiTypography.action
                                    ).copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = saveContentColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
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
}
