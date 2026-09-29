package il.kmi.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
    enabled: Boolean = true,
    dividerAfterIndex: Int? = null
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

    val menuTitle =
        if (isEnglish) {
            "Select $title"
        } else {
            "בחירת $title"
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
                    .menuAnchor(
                        type =
                            MenuAnchorType
                                .PrimaryNotEditable,
                        enabled = enabled
                    ),
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

                Spacer(
                    Modifier.width(8.dp)
                )

                /*
                 * אייקון פתיחה גלובלי אחיד.
                 * זהה למבנה של שדה התאריך במסך הנוכחות.
                 */
                Surface(
                    modifier = Modifier.size(
                        KmiIconSize.medium
                    ),
                    shape = CircleShape,
                    color =
                        if (isDarkMode) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            Color(0xFFEAF2FF)
                        },
                    border = BorderStroke(
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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▼",
                            color =
                                if (canExpand) {
                                    titleColor
                                } else {
                                    titleColor.copy(
                                        alpha = 0.45f
                                    )
                                },
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

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier =
                Modifier
                    .clip(
                        RectangleShape
                    )
                    .border(
                        width = 1.dp,
                        color =
                            Color(0xFF38BDF8)
                                .copy(alpha = 0.40f),
                        shape =
                            RectangleShape
                    ),
            containerColor = Color.Transparent,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color(0xFF1A426E),
                                            Color(0xFF12345F),
                                            Color(0xFF0D294B)
                                        )
                                )
                        )
            ) {

                /*
                 * כותרת קבועה.
                 */
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color(0xFF286A9B),
                                                Color(0xFF1A507D)
                                            )
                                    )
                            )
                            .padding(
                                horizontal = 16.dp
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text = menuTitle,
                        modifier =
                            Modifier.fillMaxWidth(),
                        color = Color.White,
                        style =
                            KmiTypography.action.copy(
                                fontWeight =
                                    FontWeight.Black
                            ),
                        textAlign =
                            TextAlign.Center,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color =
                        Color(0xFF67C7F4)
                            .copy(alpha = 0.40f)
                )

                /*
                 * הרשימה גדלה לפי מספר האפשרויות.
                 * רק כאשר יש הרבה שורות היא מגיעה לגובה המרבי ונגללת.
                 */
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(
                                rememberScrollState()
                            )
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
                                    overflow =
                                        TextOverflow.Ellipsis,
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

                            val isStrongDivider =
                                dividerAfterIndex == index

                            if (isStrongDivider) {
                                HorizontalDivider(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 12.dp
                                            ),
                                    thickness = 2.dp,
                                    color =
                                        Color.White.copy(
                                            alpha = 0.95f
                                        )
                                )
                            } else {
                                HorizontalDivider(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    thickness = 1.dp,
                                    color =
                                        Color.White.copy(
                                            alpha = 0.10f
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> KmiPremiumMultiSelectDropdown(
    title: String,
    options: List<T>,
    selectedValues: Set<T>,
    isEnglish: Boolean,
    labelForOption: (T) -> String,
    onSelectionChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true
) {
    var expanded by rememberSaveable {
        mutableStateOf(false)
    }

    val cleanOptions =
        remember(options) {
            options.distinct()
        }

    val canExpand =
        enabled &&
                cleanOptions.isNotEmpty()

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
        when {
            selectedValues.isEmpty() ->
                placeholder
                    .trim()
                    .ifBlank {
                        if (isEnglish) {
                            "All"
                        } else {
                            "הכול"
                        }
                    }

            selectedValues.size == 1 ->
                selectedValues
                    .firstOrNull()
                    ?.let(labelForOption)
                    .orEmpty()

            else ->
                if (isEnglish) {
                    "${selectedValues.size} selected"
                } else {
                    "${selectedValues.size} נבחרו"
                }
        }

    val menuTitle =
        if (isEnglish) {
            "Select $title"
        } else {
            "בחירת $title"
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
                    .menuAnchor(
                        type =
                            MenuAnchorType
                                .PrimaryNotEditable,
                        enabled = enabled
                    ),
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
                    modifier =
                        Modifier.weight(1f),
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
                        overflow =
                            TextOverflow.Ellipsis,
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
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                Spacer(
                    Modifier.width(8.dp)
                )

                Surface(
                    modifier =
                        Modifier.size(
                            KmiIconSize.medium
                        ),
                    shape = CircleShape,
                    color =
                        if (isDarkMode) {
                            MaterialTheme
                                .colorScheme
                                .surface
                        } else {
                            Color(0xFFEAF2FF)
                        },
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
                    Box(
                        modifier =
                            Modifier.fillMaxWidth(),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text = "▼",
                            color =
                                if (canExpand) {
                                    titleColor
                                } else {
                                    titleColor.copy(
                                        alpha = 0.45f
                                    )
                                },
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

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier =
                Modifier
                    .widthIn(
                        min = 280.dp,
                        max = 340.dp
                    )
                    .border(
                        width = 1.dp,
                        color =
                            Color(0xFF38BDF8)
                                .copy(alpha = 0.40f),
                        shape = RectangleShape
                    )
                    .background(
                        Color.Transparent
                    ),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color(0xFF1A426E),
                                            Color(0xFF12345F),
                                            Color(0xFF0D294B)
                                        )
                                )
                        )
            ) {

                /*
                 * כותרת קבועה של הרשימה.
                 * אינה משתתפת בגלילה.
                 */
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .background(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color(0xFF286A9B),
                                                Color(0xFF1A507D)
                                            )
                                    )
                            )
                            .padding(
                                horizontal = 16.dp
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.Center
                    ) {
                        Text(
                            text = menuTitle,
                            color = Color.White,
                            style =
                                KmiTypography.action.copy(
                                    fontWeight =
                                        FontWeight.Black
                                ),
                            textAlign =
                                TextAlign.Center,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        if (selectedValues.isNotEmpty()) {
                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Surface(
                                shape =
                                    RoundedCornerShape(10.dp),
                                color =
                                    Color(0xFF0EA5D7)
                                        .copy(alpha = 0.88f),
                                border =
                                    BorderStroke(
                                        width = 1.dp,
                                        color =
                                            Color(0xFF67E8F9)
                                                .copy(alpha = 0.90f)
                                    ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Box(
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Text(
                                        text =
                                            selectedValues.size.toString(),
                                        color = Color.White,
                                        style =
                                            KmiTypography.caption.copy(
                                                fontWeight =
                                                    FontWeight.Black
                                            ),
                                        textAlign =
                                            TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color =
                        Color(0xFF67C7F4)
                            .copy(alpha = 0.40f)
                )

                /*
                 * הרשימה מתאימה את הגובה לכמות האפשרויות.
                 * ברשימה ארוכה בלבד מופעלת גלילה.
                 */
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(
                                rememberScrollState()
                            )
                ) {

                    if (selectedValues.isNotEmpty()) {

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Clear selection"
                                        } else {
                                            "נקה בחירה"
                                        },
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    style =
                                        KmiTypography.secondary.copy(
                                            fontWeight =
                                                FontWeight.ExtraBold
                                        ),
                                    color =
                                        Color(0xFFFCA5A5),
                                    textAlign =
                                        textAlign
                                )
                            },
                            onClick = {
                                onSelectionChange(
                                    emptySet()
                                )
                            }
                        )

                        HorizontalDivider(
                            color =
                                Color.White.copy(
                                    alpha = 0.14f
                                )
                        )
                    }

                    cleanOptions.forEachIndexed {
                            index,
                            option ->

                        val isSelected =
                            option in selectedValues

                        DropdownMenuItem(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) {
                                            Color(0xFF164E79)
                                                .copy(
                                                    alpha = 0.72f
                                                )
                                        } else {
                                            Color.Transparent
                                        }
                                    ),
                            text = {
                                Text(
                                    text =
                                        labelForOption(option),
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    style =
                                        KmiTypography.secondary.copy(
                                            fontWeight =
                                                if (isSelected) {
                                                    FontWeight.Black
                                                } else {
                                                    FontWeight.Bold
                                                }
                                        ),
                                    color =
                                        if (isSelected) {
                                            Color(0xFF67E8F9)
                                        } else {
                                            Color.White
                                        },
                                    textAlign =
                                        textAlign,
                                    maxLines = 2,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Surface(
                                    modifier =
                                        Modifier.size(22.dp),
                                    shape =
                                        RoundedCornerShape(6.dp),
                                    color =
                                        if (isSelected) {
                                            Color(0xFF0EA5D7)
                                        } else {
                                            Color.Transparent
                                        },
                                    border =
                                        BorderStroke(
                                            width = 1.dp,
                                            color =
                                                if (isSelected) {
                                                    Color(0xFF67E8F9)
                                                } else {
                                                    Color.White.copy(
                                                        alpha = 0.45f
                                                    )
                                                }
                                        ),
                                    shadowElevation = 0.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth(),
                                            contentAlignment =
                                                Alignment.Center
                                        ) {
                                            Text(
                                                text = "✓",
                                                color =
                                                    Color.White,
                                                style =
                                                    KmiTypography
                                                        .caption
                                                        .copy(
                                                            fontWeight =
                                                                FontWeight.Black
                                                        )
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {

                                val newSelection =
                                    if (isSelected) {
                                        selectedValues -
                                                option
                                    } else {
                                        selectedValues +
                                                option
                                    }

                                onSelectionChange(
                                    newSelection
                                )
                            }
                        )

                        if (
                            index <
                            cleanOptions.lastIndex
                        ) {
                            HorizontalDivider(
                                color =
                                    Color.White.copy(
                                        alpha = 0.10f
                                    )
                            )
                        }
                    }
                }

                /*
      * החלק הזה אינו בתוך אזור הגלילה.
      * לכן "סיום בחירה" נשאר תמיד קבוע בתחתית.
      */
                Surface(
                    onClick = {
                        expanded = false
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    color = Color(0xFF1D5F8A),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Done"
                                } else {
                                    "סיום בחירה"
                                },
                            color = Color.White,
                            style =
                                KmiTypography.action.copy(
                                    fontWeight =
                                        FontWeight.Black
                                ),
                            textAlign =
                                TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}