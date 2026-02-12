package il.kmi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.graphicsLayer

@Composable
private fun ModernGlowFab(
    expanded: Boolean,
    onClick: () -> Unit
) {
    // ✅ Squircle (שונה מעיגול) כדי לא להיבלע בין העיגולים במסך
    val shape = RoundedCornerShape(22.dp)

    val base = Color(0xFF2563EB)
    val glow = Color(0xFF60A5FA)

    // ✅ פולס עדין כדי למשוך עין
    val pulse = rememberInfiniteTransition(label = "fabPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by pulse.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier.size(66.dp),
        contentAlignment = Alignment.Center
    ) {
        // ✅ הילה “נושמת” (מבדיל מהעיגולים)
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(shape)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = pulseAlpha), Color.Transparent),
                        radius = 260f
                    )
                )
        )

        // ✅ מסגרת לבנה חיצונית
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(shape)
                .border(BorderStroke(3.dp, Color.White.copy(alpha = 0.92f)), shape)
        )

        // ✅ הכפתור עצמו
        Surface(
            onClick = onClick,
            shape = shape,
            color = base,
            shadowElevation = 14.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // ✅ אייקון ברור (לא +/-)
                Icon(
                    imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Menu,
                    contentDescription = if (expanded) "סגור" else "תפריט מהיר",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


@Composable
fun FloatingBeltQuickMenu(
    belt: Belt,
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onWeakPoints: () -> Unit,
    onAllLists: () -> Unit,
    onPractice: () -> Unit,
    onSummary: () -> Unit,
    onVoice: () -> Unit,
    onPdf: () -> Unit
) {
    // ✅ עוזר לסגור את הרשימה לפני פעולה (כולל ניווט)
    fun closeThen(action: () -> Unit) {
        onExpandedChange(false)
        action()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (expanded) {
            Spacer(Modifier.height(6.dp))

            FloatingMenuRow(
                text = "נקודות תורפה",
                icon = { Icon(Icons.Filled.Warning, null) },
                onClick = { closeThen(onWeakPoints) }
            )
            Spacer(Modifier.height(10.dp))

            FloatingMenuRow(
                text = "כל הרשימות",
                icon = { Icon(Icons.Filled.FormatListBulleted, null) },
                onClick = { closeThen(onAllLists) }
            )
            Spacer(Modifier.height(10.dp))

            FloatingMenuRow(
                text = "תרגול",
                icon = { Icon(Icons.Filled.SportsMma, null) },
                onClick = { closeThen(onPractice) }
            )
            Spacer(Modifier.height(10.dp))

            FloatingMenuRow(
                text = "מסך סיכום",
                icon = { Icon(Icons.Filled.ReceiptLong, null) },
                onClick = { closeThen(onSummary) }
            )
            Spacer(Modifier.height(10.dp))

            FloatingMenuRow(
                text = "עוזר קולי",
                icon = { Icon(Icons.Filled.Mic, null) },
                onClick = { closeThen(onVoice) }
            )
            Spacer(Modifier.height(10.dp))

            FloatingMenuRow(
                text = "חומר סיכום (PDF)",
                icon = { Icon(Icons.Filled.PictureAsPdf, null) },
                onClick = { closeThen(onPdf) }
            )
            Spacer(Modifier.height(14.dp))
        }

        ModernGlowFab(
            expanded = expanded,
            onClick = { onExpandedChange(!expanded) }
        )
    }
}

   /* ✅ FIX: הפונקציה הזאת חסרה אצלך בקובץ ולכן הכל נשבר */
@Composable
private fun FloatingMenuRow(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val rowHeight = 44.dp
    val labelWidth = 170.dp
    val iconSize = 44.dp
    val gap = 10.dp
    val totalWidth = labelWidth + gap + iconSize

    Row(
        modifier = Modifier
            .requiredWidth(totalWidth)
            .height(rowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .requiredWidth(labelWidth)
                .height(rowHeight)
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(gap))

        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .size(iconSize)
                .clickable { onClick() }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                icon()
            }
        }
    }
}
