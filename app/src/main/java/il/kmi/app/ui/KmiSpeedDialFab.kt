package il.kmi.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class KmiFabAction(
    val text: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun KmiSpeedDialFab(
    actions: List<KmiFabAction>,
    modifier: Modifier = Modifier,
    initiallyOpen: Boolean = false,
    onHaptic: (() -> Unit)? = null,
    onClickSound: (() -> Unit)? = null
) {
    val fabBottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    var open by rememberSaveable { mutableStateOf(initiallyOpen) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val mainIcon = if (open) Icons.Filled.Close else Icons.Filled.Add

        // שכבת לחיצה לסגירה כשפתוח
        if (open) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable { open = false }
            )
        }

        // כפתורי התפריט
        AnimatedVisibility(
            visible = open,
            modifier = modifier
                .fillMaxSize(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            val dialHeight = 52.dp
            val dialLabelWidth = 170.dp
            val dialIconButtonSize = 52.dp
            val dialIconSize = 22.dp
            val dialGap = 10.dp
            val dialTotalWidth = dialIconButtonSize + dialGap + dialLabelWidth + 8.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp + fabBottomInset + 92.dp)
                        .width(dialTotalWidth),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    actions.forEach { a ->
                        SpeedDialRow(
                            text = a.text,
                            icon = a.icon,
                            enabled = a.enabled,
                            height = dialHeight,
                            labelWidth = dialLabelWidth,
                            iconButtonSize = dialIconButtonSize,
                            iconSize = dialIconSize,
                            gap = dialGap,
                            onClick = {
                                onClickSound?.invoke()
                                onHaptic?.invoke()
                                open = false
                                a.onClick()
                            }
                        )
                    }
                }
            }
        }

        // הכפתור הראשי
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 10.dp,
            modifier = modifier
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp + fabBottomInset)
                        .size(58.dp)
                        .border(width = 3.dp, color = Color.White, shape = CircleShape)
                ) {
                    IconButton(
                        onClick = {
                            onClickSound?.invoke()
                            onHaptic?.invoke()
                            open = !open
                        }
                    ) {
                        Icon(
                            imageVector = mainIcon,
                            contentDescription = "פעולות מהירות",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedDialRow(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    height: Dp = 52.dp,
    labelWidth: Dp = 170.dp,
    iconButtonSize: Dp = 52.dp,
    iconSize: Dp = 22.dp,
    gap: Dp = 10.dp
) {
    val alphaDisabled = 0.45f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = if (enabled) 0.92f else 0.80f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
            modifier = Modifier
                .width(labelWidth)
                .height(height)
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color(0xFF0B1220).copy(alpha = if (enabled) 1f else alphaDisabled),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(gap))

        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = if (enabled) 0.92f else 0.80f),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
            modifier = Modifier.size(iconButtonSize)
        ) {
            IconButton(
                enabled = enabled,
                onClick = onClick,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(iconSize),
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}
