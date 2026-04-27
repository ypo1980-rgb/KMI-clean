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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

enum class QuickMenuTriggerMode {
    Fab,
    BottomBar
}

private data class QuickMenuItemUi(
    val title: String,
    val icon: ImageVector,
    val action: () -> Unit,
    val isLocked: Boolean
)

@Composable
private fun ModernGlowFab(
    accentColor: Color,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    val pulse = rememberInfiniteTransition(label = "quickFabPulse")
    val haloAlpha by pulse.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )
    val haloScale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "iconRotation"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (expanded) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 520f),
        label = "buttonScale"
    )

    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    scaleX = haloScale
                    scaleY = haloScale
                }
                .clip(shape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = haloAlpha),
                            Color.Transparent
                        ),
                        radius = 240f
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.96f))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.96f),
                    shape = shape
                )
        )

        Surface(
            onClick = onClick,
            shape = shape,
            color = Color.Transparent,
            shadowElevation = 16.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.10f)),
            modifier = Modifier.graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.78f),
                                accentColor,
                                accentColor.copy(alpha = 0.90f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = if (expanded) "סגור" else "תפריט מהיר",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { rotationZ = iconRotation }
                )
            }
        }
    }
}

@Composable
private fun BottomQuickMenuButton(
    belt: Belt,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 8.dp,
        color = Color.White,
        border = BorderStroke(
            1.dp,
            belt.color.copy(alpha = 0.22f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            belt.color.copy(alpha = 0.10f),
                            Color.White,
                            belt.color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null,
                tint = belt.color,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = if (isEnglish) "More Actions" else "פעולות נוספות",
                fontWeight = FontWeight.Bold,
                color = belt.color,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun FloatingQuickMenu(
    belt: Belt,
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    triggerMode: QuickMenuTriggerMode = QuickMenuTriggerMode.Fab,
    includePractice: Boolean = true,
    includeAllLists: Boolean = true,
    includeSummary: Boolean = true,
    accentColorOverride: Color? = null,
    hasFullAccess: Boolean = true,
    onLockedItemClick: () -> Unit = {},
    onWeakPoints: () -> Unit,
    onAllLists: () -> Unit,
    onPractice: () -> Unit = {},
    onSummary: () -> Unit,
    onVoice: () -> Unit,
    onPdf: () -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val accentColor = accentColorOverride ?: belt.color
    val isMenuLocked = !hasFullAccess

    LaunchedEffect(hasFullAccess) {
        android.util.Log.e(
            "KMI_LOCK_DEBUG",
            "🔥 hasFullAccess=$hasFullAccess  →  LOCK SHOULD BE = ${!hasFullAccess}"
        )
    }

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    // ✅ עוזר לסגור את הרשימה לפני פעולה (כולל ניווט)
    fun closeThen(action: () -> Unit) {
        onExpandedChange(false)
        action()
    }

    val items = buildList {
        add(
            QuickMenuItemUi(
                title = tr("נקודות תורפה", "Weak Points"),
                icon = Icons.Filled.Warning,
                action = onWeakPoints,
                isLocked = !hasFullAccess
            )
        )

        if (includeAllLists) {
            add(
                QuickMenuItemUi(
                    title = tr("כל הרשימות", "All Lists"),
                    icon = Icons.Filled.FormatListBulleted,
                    action = onAllLists,
                    isLocked = !hasFullAccess
                )
            )
        }

        if (includePractice) {
            add(
                QuickMenuItemUi(
                    title = tr("תרגול", "Practice"),
                    icon = Icons.Filled.SportsMma,
                    action = onPractice,
                    isLocked = !hasFullAccess
                )
            )
        }

        if (includeSummary) {
            add(
                QuickMenuItemUi(
                    title = tr("מסך סיכום", "Summary"),
                    icon = Icons.Filled.ReceiptLong,
                    action = onSummary,
                    isLocked = !hasFullAccess
                )
            )
        }

        add(
            QuickMenuItemUi(
                title = tr("עוזר קולי", "Voice Assistant"),
                icon = Icons.Filled.Mic,
                action = onVoice,
                isLocked = !hasFullAccess
            )
        )
    }

    val menuVisibilityState = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) {
        menuVisibilityState.targetState = expanded
    }

    Box(
        modifier = modifier,
        contentAlignment = when (triggerMode) {
            QuickMenuTriggerMode.BottomBar -> Alignment.BottomCenter
            QuickMenuTriggerMode.Fab -> if (isEnglish) Alignment.BottomStart else Alignment.BottomEnd
        }
    ) {
        if (menuVisibilityState.currentState || menuVisibilityState.targetState) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.10f))
            )
        }

        AnimatedVisibility(
            visibleState = menuVisibilityState,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 6 }),
            modifier = when (triggerMode) {
                QuickMenuTriggerMode.BottomBar -> Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .wrapContentHeight()

                QuickMenuTriggerMode.Fab -> Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = (-6).dp)
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isEnglish) 16.dp else 0.dp,
                        end = if (isEnglish) 0.dp else 16.dp
                    ),
                contentAlignment = when (triggerMode) {
                    QuickMenuTriggerMode.BottomBar -> Alignment.BottomCenter
                    QuickMenuTriggerMode.Fab ->
                        if (isEnglish) BiasAbsoluteAlignment(-1f, 1f)
                        else BiasAbsoluteAlignment(1f, 1f)
                }
            ) {
                Column(
                    modifier = Modifier.wrapContentSize(),
                    horizontalAlignment = if (triggerMode == QuickMenuTriggerMode.BottomBar) {
                        Alignment.CenterHorizontally
                    } else {
                        if (isEnglish) Alignment.Start else Alignment.End
                    }
                ) {
                    Spacer(Modifier.height(6.dp))

                    PremiumQuickMenuPanel(
                        title = tr("תפריט מהיר", "Quick Menu"),
                        accentColor = accentColor,
                        isEnglish = isEnglish,
                        items = items,
                        onItemClick = { action -> closeThen(action) },
                        onLockedItemClick = onLockedItemClick,
                        onClose = { onExpandedChange(false) }
                    )

                    Spacer(
                        Modifier.height(
                            if (triggerMode == QuickMenuTriggerMode.BottomBar) 10.dp else 14.dp
                        )
                    )
                }
            }
        }

        val shouldShowTrigger =
            triggerMode == QuickMenuTriggerMode.Fab &&
                    !menuVisibilityState.currentState &&
                    !menuVisibilityState.targetState

        if (shouldShowTrigger) {
            Box(
                modifier = Modifier.align(
                    if (isEnglish) Alignment.BottomEnd else Alignment.BottomStart
                ),
                contentAlignment = Alignment.Center
            ) {
                ModernGlowFab(
                    accentColor = accentColor,
                    expanded = expanded,
                    onClick = { onExpandedChange(true) }
                )
            }
        }
    }
}

   /* ✅ FIX: הפונקציה הזאת חסרה אצלך בקובץ ולכן הכל נשבר */
   @Composable
   private fun PremiumQuickMenuPanel(
       title: String,
       accentColor: Color,
       isEnglish: Boolean,
       items: List<QuickMenuItemUi>,
       onItemClick: (() -> Unit) -> Unit,
       onLockedItemClick: () -> Unit,
       onClose: () -> Unit
   ) {
       val panelWidth = 214.dp
       val panelShape = RoundedCornerShape(22.dp)

       Surface(
           shape = panelShape,
           color = Color.White,
           tonalElevation = 0.dp,
           shadowElevation = 16.dp,
           modifier = Modifier
               .requiredWidth(panelWidth)
               .wrapContentWidth(
                   if (isEnglish) Alignment.Start else Alignment.End
               )
       ) {
           Box(
               modifier = Modifier
                   .clip(panelShape)
                   .background(
                       brush = Brush.verticalGradient(
                           colors = listOf(
                               accentColor.copy(alpha = 0.24f),
                               Color(0xFFFDFDFE),
                               Color(0xFFF6F8FB),
                               accentColor.copy(alpha = 0.14f)
                           )
                       )
                   )
                   .border(
                       width = 1.dp,
                       color = accentColor.copy(alpha = 0.42f),
                       shape = panelShape
                   )
                   .padding(horizontal = 10.dp, vertical = 10.dp)
           ) {
               Column(
                   modifier = Modifier
                       .fillMaxWidth()
                       .wrapContentWidth(if (isEnglish) Alignment.Start else Alignment.End),
                   horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
               ) {
                   Row(
                       modifier = Modifier.fillMaxWidth(),
                       verticalAlignment = Alignment.CenterVertically
                   ) {
                       if (isEnglish) {
                           Box(
                               modifier = Modifier.weight(1f),
                               contentAlignment = Alignment.CenterStart
                           ) {
                               Text(
                                   text = title,
                                   color = accentColor,
                                   fontWeight = FontWeight.ExtraBold,
                                   textAlign = TextAlign.Start,
                                   modifier = Modifier.fillMaxWidth()
                               )
                           }

                           Spacer(Modifier.width(8.dp))

                           Icon(
                               imageVector = Icons.Filled.Close,
                               contentDescription = "close",
                               tint = accentColor,
                               modifier = Modifier
                                   .size(18.dp)
                                   .clickable { onClose() }
                           )
                       } else {
                           Box(
                               modifier = Modifier.weight(1f),
                               contentAlignment = BiasAbsoluteAlignment(1f, 0f)
                           ) {
                               Text(
                                   text = title,
                                   color = accentColor,
                                   fontWeight = FontWeight.ExtraBold,
                                   textAlign = TextAlign.Right,
                                   modifier = Modifier.fillMaxWidth()
                               )
                           }

                           Spacer(Modifier.width(8.dp))

                           Icon(
                               imageVector = Icons.Filled.Close,
                               contentDescription = "close",
                               tint = accentColor,
                               modifier = Modifier
                                   .size(18.dp)
                                   .clickable { onClose() }
                           )
                       }
                   }
                   Spacer(Modifier.height(10.dp))

                   items.forEachIndexed { index, item ->
                       PremiumQuickMenuRow(
                           text = item.title,
                           icon = item.icon,
                           accentColor = accentColor,
                           isEnglish = isEnglish,
                           isLocked = item.isLocked,
                           onClick = {
                               onItemClick {
                                   if (item.isLocked) {
                                       onLockedItemClick()
                                   } else {
                                       item.action()
                                   }
                               }
                           }
                       )

                       if (index != items.lastIndex) {
                           HorizontalDivider(
                               thickness = 0.8.dp,
                               color = accentColor.copy(alpha = 0.20f)
                           )
                       }
                   }
               }
           }
       }
   }

@Composable
private fun PremiumQuickMenuRow(
    text: String,
    icon: ImageVector,
    accentColor: Color,
    isEnglish: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit
){
    val lockPulse = rememberInfiniteTransition(label = "quickMenuLockPulse")

    val lockScale by lockPulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quickMenuLockScale"
    )

    val lockGlowAlpha by lockPulse.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.26f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quickMenuLockGlow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEnglish) {
            PremiumQuickMenuIcon(
                icon = icon,
                accentColor = accentColor
            )

            Spacer(Modifier.width(10.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = text,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                if (isLocked) {
                    Spacer(Modifier.width(8.dp))

                    PremiumAnimatedLockIcon(
                        accentColor = accentColor,
                        scale = lockScale,
                        glowAlpha = lockGlowAlpha
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (isLocked) {
                    PremiumAnimatedLockIcon(
                        accentColor = accentColor,
                        scale = lockScale,
                        glowAlpha = lockGlowAlpha
                    )

                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    text = text,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Right,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.width(10.dp))

            PremiumQuickMenuIcon(
                icon = icon,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun PremiumAnimatedLockIcon(
    accentColor: Color,
    scale: Float,
    glowAlpha: Float
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    radius = 44f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f))
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.38f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun PremiumQuickMenuIcon(
    icon: ImageVector,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.32f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(14.dp)
        )
    }
}
