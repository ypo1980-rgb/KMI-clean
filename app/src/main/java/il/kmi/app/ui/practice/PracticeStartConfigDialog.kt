package il.kmi.app.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeStartConfigDialog(
    show: Boolean,
    isEnglish: Boolean,
    initialMinutes: Int,
    initialHalfAlert: Boolean,
    initialLast10Alert: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        durationSeconds: Int,
        playHalf: Boolean,
        playCountdown: Boolean
    ) -> Unit
) {
    if (!show) return

    var selectedMin by
    remember {
        mutableStateOf(
            initialMinutes.coerceIn(
                1,
                60
            )
        )
    }

    var playHalf by
    remember {
        mutableStateOf(
            initialHalfAlert
        )
    }

    var playCountdown by
    remember {
        mutableStateOf(
            initialLast10Alert
        )
    }

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    val headerBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.18f),
                    MaterialTheme
                        .colorScheme
                        .surface
                        .copy(alpha = 0.0f)
                )
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor =
            MaterialTheme
                .colorScheme
                .surface,
        contentColor =
            MaterialTheme
                .colorScheme
                .onSurface,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(
                            top = 8.dp,
                            bottom = 4.dp
                        )
                        .size(
                            width = 44.dp,
                            height = 4.dp
                        )
                        .clip(
                            RoundedCornerShape(100)
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(alpha = 0.35f)
                        )
            )
        }
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(
                        bottom = 8.dp
                    )
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        ),
                shape =
                    RoundedCornerShape(24.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(alpha = 0.55f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(alpha = 0.24f)
                    )
            ) {
                Box(
                    modifier =
                        Modifier
                            .background(
                                headerBrush
                            )
                            .padding(
                                horizontal = 18.dp,
                                vertical = 16.dp
                            )
                ) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Choose Practice Duration"
                                } else {
                                    "בחר זמן תרגול"
                                },
                            style =
                                KmiTypography
                                    .sectionTitle
                                    .copy(
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    ),
                            textAlign =
                                TextAlign.Center
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                String.format(
                                    "%02d:00",
                                    selectedMin
                                ),
                            style =
                                KmiTypography
                                    .metric
                                    .copy(
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                        fontWeight =
                                            FontWeight.Black
                                    )
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            SegmentedTimeChooser(
                values =
                    listOf(
                        1,
                        3,
                        5
                    ),
                selected =
                    selectedMin,
                isEnglish =
                    isEnglish,
                onSelect = {
                    selectedMin = it
                }
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        ),
                shape =
                    RoundedCornerShape(22.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(alpha = 0.38f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .outlineVariant
                                .copy(alpha = 0.75f)
                    )
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 4.dp
                            )
                ) {
                    SettingRow(
                        title =
                            if (isEnglish) {
                                "Mid-time alert"
                            } else {
                                "התראה באמצע הזמן"
                            },
                        subtitle =
                            if (isEnglish) {
                                "Beep + voice announcement at halfway point"
                            } else {
                                "צפצוף + הודעה קולית בחצי הזמן"
                            },
                        checked =
                            playHalf,
                        isEnglish =
                            isEnglish,
                        onCheckedChange = {
                            playHalf = it
                        }
                    )

                    HorizontalDivider(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .outlineVariant
                                .copy(alpha = 0.55f)
                    )

                    SettingRow(
                        title =
                            if (isEnglish) {
                                "Sound in the last 10 seconds"
                            } else {
                                "צליל ב־10 השניות האחרונות"
                            },
                        subtitle =
                            if (isEnglish) {
                                "Short beep every second until the end"
                            } else {
                                "צפצוף קצר כל שנייה עד לסיום"
                            },
                        checked =
                            playCountdown,
                        isEnglish =
                            isEnglish,
                        onCheckedChange = {
                            playCountdown = it
                        }
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        12.dp,
                        Alignment.End
                    )
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier.heightIn(
                            min = 52.dp
                        )
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Cancel"
                            } else {
                                "בטל"
                            },
                        style =
                            KmiTypography
                                .action
                                .copy(
                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                    )
                }

                Button(
                    onClick = {
                        onConfirm(
                            selectedMin * 60,
                            playHalf,
                            playCountdown
                        )
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(
                                min = 52.dp
                            ),
                    shape =
                        RoundedCornerShape(16.dp),
                    elevation =
                        ButtonDefaults
                            .buttonElevation(
                                defaultElevation =
                                    0.dp,
                                pressedElevation =
                                    1.dp,
                                disabledElevation =
                                    0.dp
                            )
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Start"
                            } else {
                                "התחל"
                            },
                        style =
                            KmiTypography
                                .action
                                .copy(
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )
        }
    }
}

@Composable
private fun SegmentedTimeChooser(
    values: List<Int>,
    selected: Int,
    isEnglish: Boolean,
    onSelect: (Int) -> Unit
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        shape =
            RoundedCornerShape(22.dp),
        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(alpha = 0.42f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                        .copy(alpha = 0.75f)
            )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            values
                .distinct()
                .sorted()
                .forEach { value ->

                    val isSelected =
                        selected == value

                    val itemBrush =
                        if (isSelected) {
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                        MaterialTheme
                                            .colorScheme
                                            .tertiary
                                    )
                            )
                        } else {
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        MaterialTheme
                                            .colorScheme
                                            .surface,
                                        MaterialTheme
                                            .colorScheme
                                            .surface
                                    )
                            )
                        }

                    val contentColor =
                        if (isSelected) {
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        }

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .heightIn(
                                    min = 58.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        17.dp
                                    )
                                )
                                .background(
                                    itemBrush
                                )
                                .border(
                                    width = 1.dp,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                                .copy(
                                                    alpha = 0.55f
                                                )
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .outlineVariant
                                        },
                                    shape =
                                        RoundedCornerShape(
                                            17.dp
                                        )
                                )
                                .clickable {
                                    onSelect(value)
                                }
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 8.dp
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.Center
                        ) {
                            Text(
                                text = "$value",
                                style =
                                    KmiTypography
                                        .metric
                                        .copy(
                                            color =
                                                contentColor,
                                            fontWeight =
                                                FontWeight.ExtraBold
                                        ),
                                maxLines = 1
                            )

                            Text(
                                text =
                                    if (isEnglish) {
                                        "min"
                                    } else {
                                        "דק׳"
                                    },
                                style =
                                    KmiTypography
                                        .caption
                                        .copy(
                                            color =
                                                contentColor
                                        ),
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    isEnglish: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val rowTextAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val rowHorizontalAlignment =
        if (isEnglish) {
            Alignment.Start
        } else {
            Alignment.End
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onCheckedChange(
                        !checked
                    )
                }
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier =
                Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(2.dp),
            horizontalAlignment =
                rowHorizontalAlignment
        ) {
            Text(
                text = title,
                style =
                    KmiTypography
                        .cardTitle
                        .copy(
                            fontWeight =
                                FontWeight.SemiBold
                        ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                textAlign =
                    rowTextAlign,
                maxLines = 2,
                overflow =
                    TextOverflow.Ellipsis,
                modifier =
                    Modifier.fillMaxWidth()
            )

            Text(
                text = subtitle,
                style =
                    KmiTypography.caption,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                textAlign =
                    rowTextAlign,
                maxLines = 3,
                overflow =
                    TextOverflow.Ellipsis,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        Switch(
            checked = checked,
            onCheckedChange =
                onCheckedChange,
            thumbContent = {
                if (checked) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary
                                )
                    )
                }
            }
        )
    }
}