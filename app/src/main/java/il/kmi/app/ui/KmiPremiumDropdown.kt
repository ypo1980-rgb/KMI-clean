package il.kmi.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KmiPremiumDropdown(
    title: String,
    options: List<String>,
    selectedValue: String,
    isEnglish: Boolean,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true
) {
    val cleanOptions =
        remember(options) {
            options
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }

    var expanded by rememberSaveable {
        mutableStateOf(false)
    }

    val canExpand =
        enabled && cleanOptions.size > 1

    val isDarkMode =
        MaterialTheme
            .colorScheme
            .surface
            .luminance() < 0.5f

    val textAlign =
        if (isEnglish) {
            TextAlign.Start
        } else {
            TextAlign.Right
        }

    val horizontalAlignment =
        if (isEnglish) {
            Alignment.Start
        } else {
            Alignment.End
        }

    val fieldContainerColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color.White.copy(alpha = 0.88f)
        }

    val fieldBorderColor =
        if (isDarkMode) {
            MaterialTheme
                .colorScheme
                .outline
                .copy(alpha = 0.55f)
        } else {
            Color(0xFFBFD7EF)
        }

    val focusedBorderColor =
        if (isDarkMode) {
            Color(0xFF38BDF8)
        } else {
            Color(0xFF0EA5D7)
        }

    val titleColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color(0xFF64748B)
        }

    val valueColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color(0xFF111827)
        }

    val displayValue =
        selectedValue
            .trim()
            .ifBlank {
                placeholder
                    .trim()
                    .ifBlank { "—" }
            }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (canExpand) {
                expanded = !expanded
            }
        },
        modifier =
            modifier.fillMaxWidth()
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .menuAnchor(),
            shape = RoundedCornerShape(15.dp),
            color = fieldContainerColor,
            border =
                BorderStroke(
                    width = 1.dp,
                    color =
                        if (expanded) {
                            focusedBorderColor
                        } else {
                            fieldBorderColor
                        }
                ),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment =
                        horizontalAlignment,
                    verticalArrangement =
                        Arrangement.Center
                ) {
                    Text(
                        text = title,
                        color = titleColor,
                        style =
                            KmiTypography.caption.copy(
                                fontWeight =
                                    FontWeight.Bold
                            ),
                        textAlign = textAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Text(
                        text = displayValue,
                        color =
                            if (enabled) {
                                valueColor
                            } else {
                                valueColor.copy(
                                    alpha = 0.55f
                                )
                            },
                        style =
                            KmiTypography.secondary.copy(
                                fontWeight =
                                    FontWeight.ExtraBold
                            ),
                        textAlign = textAlign,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(6.dp))

                Text(
                    text =
                        when {
                            !canExpand -> "•"
                            expanded -> "▲"
                            else -> "▼"
                        },
                    color =
                        if (canExpand) {
                            titleColor
                        } else {
                            titleColor.copy(alpha = 0.45f)
                        },
                    style =
                        KmiTypography.caption.copy(
                            fontWeight =
                                FontWeight.Black
                        )
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier =
                Modifier
                    .heightIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color =
                            Color(0xFF38BDF8)
                                .copy(alpha = 0.40f),
                        shape =
                            RoundedCornerShape(16.dp)
                    ),
            containerColor = Color(0xFF0A234A),
            shadowElevation = 2.dp,
            tonalElevation = 0.dp
        ) {
            cleanOptions.forEachIndexed { index, option ->
                val isSelected =
                    option == selectedValue.trim()

                DropdownMenuItem(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color =
                                    if (isSelected) {
                                        Color(0xFF164E79)
                                            .copy(alpha = 0.72f)
                                    } else {
                                        Color.Transparent
                                    }
                            ),
                    text = {
                        Text(
                            text = option,
                            color =
                                if (isSelected) {
                                    Color(0xFF67E8F9)
                                } else {
                                    Color.White
                                },
                            style =
                                KmiTypography.secondary.copy(
                                    fontWeight =
                                        if (isSelected) {
                                            FontWeight.Black
                                        } else {
                                            FontWeight.Bold
                                        }
                                ),
                            textAlign = textAlign,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )

                if (index < cleanOptions.lastIndex) {
                    Spacer(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 1.dp)
                                .background(
                                    Color.White.copy(
                                        alpha = 0.10f
                                    )
                                )
                    )
                }
            }
        }
    }
}