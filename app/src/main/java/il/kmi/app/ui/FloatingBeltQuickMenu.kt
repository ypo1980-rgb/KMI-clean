package il.kmi.app.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.subscription.KmiAccess
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import il.yuval.ui.theme.kmiSuccessColor

//============================================================================


enum class QuickMenuTriggerMode {
    Fab,
    BottomBar,

    // ✅ מפעיל צדדי למסך "תרגילים לפי חגורה"
    SideRail
}

private data class QuickMenuItemUi(
    val title: String,
    val icon: ImageVector,
    val action: () -> Unit,
    val isLocked: Boolean
)

@Composable
private fun quickMenuLockTint(): Color {
    return MaterialTheme.colorScheme.primary
}

@Composable
private fun ModernGlowFab(
    accentColor: Color,
    expanded: Boolean,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val shape =
        RoundedCornerShape(22.dp)

    val accentContentColor =
        if (accentColor.luminance() < 0.55f) {
            Color.White
        } else {
            Color.Black
        }

    val pulse =
        rememberInfiniteTransition(
            label = "quickFabPulse"
        )
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
        modifier = Modifier.size(
            scaledIconSize(72.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    scaledIconSize(88.dp)
                )
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
                .size(
                    scaledIconSize(62.dp)
                )
                .clip(shape)
                .background(
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.92f
                    )
                )
                .border(
                    width = 1.dp,
                    color =
                        MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.45f
                        ),
                    shape = shape
                )
        )

        Surface(
            onClick = onClick,
            shape = shape,
            color = Color.Transparent,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(
                width = 0.75.dp,
                color =
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.38f
                    )
            ),
            modifier = Modifier.graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
        ) {
            Box(
                modifier = Modifier
                    .size(
                        scaledIconSize(56.dp)
                    )
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
                                    accentContentColor.copy(
                                        alpha = 0.18f
                                    ),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription =
                        if (expanded) {
                            if (isEnglish) {
                                "Close quick menu"
                            } else {
                                "סגור תפריט מהיר"
                            }
                        } else {
                            if (isEnglish) {
                                "Open quick menu"
                            } else {
                                "פתח תפריט מהיר"
                            }
                        },
                    tint = accentContentColor,
                    modifier = Modifier
                        .size(KmiIconSize.large)
                        .graphicsLayer { rotationZ = iconRotation }
                )
            }
        }
    }
}

@Composable
private fun SideRailQuickMenuTrigger(
    accentColor: Color,
    expanded: Boolean,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val accentContentColor =
        if (accentColor.luminance() < 0.55f) {
            Color.White
        } else {
            Color.Black
        }

    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 500f
        ),
        label = "sideRailIconRotation"
    )

    val triggerShape = RoundedCornerShape(
        topEnd = 18.dp,
        bottomEnd = 18.dp,
        topStart = 0.dp,
        bottomStart = 0.dp
    )

    Surface(
        onClick = onClick,
        shape = triggerShape,
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 0.75.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = 0.55f
            )
        ),
        modifier = Modifier
            .width(38.dp)
            .height(72.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(triggerShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.84f),
                            accentColor,
                            accentColor.copy(alpha = 0.88f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentContentColor.copy(
                                    alpha = 0.22f
                                ),
                                Color.Transparent
                            )
                        )
                    )
            )

            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription =
                    if (expanded) {
                        if (isEnglish) {
                            "Close quick menu"
                        } else {
                            "סגור תפריט מהיר"
                        }
                    } else {
                        if (isEnglish) {
                            "Open quick menu"
                        } else {
                            "פתח תפריט מהיר"
                        }
                    },
                tint = accentContentColor,
                modifier = Modifier
                    .size(KmiIconSize.large)
                    .graphicsLayer {
                        rotationZ = iconRotation
                    }
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

    val userSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    val subsSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_subs", Context.MODE_PRIVATE)
    }

    // ✅ מקור ישן/כללי שחלק מהאפליקציה עדיין עשוי להשתמש בו
    val legacySp = remember(ctx) {
        ctx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE)
    }

    var accessRefreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(userSp, subsSp, legacySp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "has_full_access" ||
                key == "full_access" ||
                key == "subscription_active" ||
                key == "is_subscribed" ||
                key == "google_subscription_verified" ||
                key == "google_subscription_checked_at" ||
                key == "sub_product" ||
                key == "sub_access_until" ||
                key == "access_changed_at"
            ) {
                accessRefreshTick++
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)
        subsSp.registerOnSharedPreferenceChangeListener(listener)
        legacySp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
            subsSp.unregisterOnSharedPreferenceChangeListener(listener)
            legacySp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val effectiveHasFullAccess = remember(hasFullAccess, accessRefreshTick) {
        hasFullAccess ||
                KmiAccess.hasFullAccess(userSp) ||
                KmiAccess.hasFullAccess(subsSp) ||
                KmiAccess.hasFullAccess(legacySp)
    }

    val isMenuLocked = !effectiveHasFullAccess



    fun tr(he: String, en: String): String = if (isEnglish) en else he

    // ✅ עוזר לסגור את הרשימה לפני פעולה (כולל ניווט)
    fun closeThen(action: () -> Unit) {
        onExpandedChange(false)
        action()
    }

    val items = remember(
        isMenuLocked,
        isEnglish,
        includeAllLists,
        includePractice,
        includeSummary,
        onWeakPoints,
        onAllLists,
        onPractice,
        onSummary,
        onVoice,
        onPdf
    ) {
        buildList {
        add(
            QuickMenuItemUi(
                title = tr("נקודות תורפה", "Weak Points"),
                icon = Icons.Filled.Warning,
                action = onWeakPoints,
                isLocked = isMenuLocked
            )
        )

        if (includeAllLists) {
            add(
                QuickMenuItemUi(
                    title = tr("כל הרשימות", "All Lists"),
                    icon =
                        Icons.AutoMirrored.Filled
                            .FormatListBulleted,
                    action = onAllLists,
                    isLocked = isMenuLocked
                )
            )
        }

        if (includePractice) {
            add(
                QuickMenuItemUi(
                    title = tr("תרגול", "Practice"),
                    icon = Icons.Filled.SportsMma,
                    action = onPractice,
                    isLocked = isMenuLocked
                )
            )
        }

        if (includeSummary) {
            add(
                QuickMenuItemUi(
                    title = tr("מסך סיכום", "Summary"),
                    icon =
                        Icons.AutoMirrored.Filled
                            .ReceiptLong,
                    action = onSummary,
                    isLocked = isMenuLocked
                )
            )
        }

            add(
                QuickMenuItemUi(
                    title = tr("עוזר קולי", "Voice Assistant"),
                    icon = Icons.Filled.Mic,
                    action = onVoice,
                    isLocked = isMenuLocked
                )
            )

            add(
                QuickMenuItemUi(
                    title = tr("חומרי PDF", "PDF Materials"),
                    icon = Icons.Filled.PictureAsPdf,
                    action = onPdf,
                    isLocked = isMenuLocked
                )
            )
        }
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
            QuickMenuTriggerMode.SideRail -> Alignment.CenterStart
        }
    ) {
        if (menuVisibilityState.currentState || menuVisibilityState.targetState) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Transparent)
                    .clickable {
                        onExpandedChange(false)
                    }
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

                QuickMenuTriggerMode.SideRail -> Modifier
                    .align(Alignment.TopStart)
                    .wrapContentSize()
                    .offset(
                        x = 46.dp,
                        y = 88.dp
                    )
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

                    QuickMenuTriggerMode.SideRail -> Alignment.CenterStart
                }
            ) {
                Column(
                    modifier = Modifier.wrapContentSize(),
                    horizontalAlignment = when (triggerMode) {
                        QuickMenuTriggerMode.BottomBar -> Alignment.CenterHorizontally
                        QuickMenuTriggerMode.SideRail -> Alignment.Start
                        QuickMenuTriggerMode.Fab -> if (isEnglish) Alignment.Start else Alignment.End
                    }
                ) {
                    Spacer(Modifier.height(6.dp))

                    PremiumQuickMenuPanel(
                        title = tr("תפריט מהיר", "Quick Menu"),
                        accentColor = accentColor,
                        isEnglish = isEnglish,
                        menuLocked = isMenuLocked,
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
            (triggerMode == QuickMenuTriggerMode.Fab ||
                    triggerMode == QuickMenuTriggerMode.SideRail) &&
                    !menuVisibilityState.currentState &&
                    !menuVisibilityState.targetState

        if (shouldShowTrigger) {
            val isSideRail =
                triggerMode ==
                        QuickMenuTriggerMode.SideRail

            Box(
                modifier =
                    Modifier
                        .align(
                            if (isSideRail) {
                                Alignment.TopStart
                            } else if (isEnglish) {
                                Alignment.BottomEnd
                            } else {
                                Alignment.BottomStart
                            }
                        )
                        .offset(
                            y =
                                if (isSideRail) {
                                    88.dp
                                } else {
                                    0.dp
                                }
                        ),
                contentAlignment = Alignment.Center
            ) {
                if (isSideRail) {
                    SideRailQuickMenuTrigger(
                        accentColor = accentColor,
                        expanded = expanded,
                        isEnglish = isEnglish,
                        onClick = {
                            onExpandedChange(true)
                        }
                    )
                } else {
                    ModernGlowFab(
                        accentColor = accentColor,
                        expanded = expanded,
                        isEnglish = isEnglish,
                        onClick = {
                            onExpandedChange(true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumQuickMenuPanel(
    title: String,
    accentColor: Color,
    isEnglish: Boolean,
    menuLocked: Boolean,
    items: List<QuickMenuItemUi>,
    onItemClick: (() -> Unit) -> Unit,
    onLockedItemClick: () -> Unit,
    onClose: () -> Unit
) {
    /*
     * הכרטיס קומפקטי בכתב קטן ומתרחב לפי התוכן.
     * הרוחב המרבי משאיר שוליים בטוחים במסכים קטנים.
     */
    val panelShape = RoundedCornerShape(20.dp)

    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode =
        colorScheme.background.luminance() < 0.5f

    /*
     * צבעי הכרטיס נלקחים מערכת הנושא כדי לשמור
     * על ניגודיות עקבית במצב בהיר ובמצב כהה.
     */
    val panelColor =
        colorScheme.surface

    val panelSecondaryColor =
        colorScheme.surfaceContainerHigh

    /*
     * בחגורה כהה משתמשים בצבע הטקסט של ערכת הנושא,
     * כדי למנוע שחור על רקע שחור.
     */
    val readableAccent =
        when {
            accentColor == Belt.GREEN.color ->
                kmiSuccessColor()

            isDarkMode &&
                    accentColor.luminance() < 0.45f ->
                colorScheme.onSurface

            !isDarkMode &&
                    accentColor.luminance() > 0.78f ->
                colorScheme.onSurfaceVariant

            else ->
                accentColor
        }

    val borderAccent =
        if (
            isDarkMode &&
            accentColor.luminance() < 0.45f
        ) {
            colorScheme.outline.copy(alpha = 0.55f)
        } else {
            readableAccent.copy(
                alpha = if (isDarkMode) 0.48f else 0.34f
            )
        }

    val dividerColor =
        if (isDarkMode) {
            colorScheme.outline.copy(alpha = 0.38f)
        } else {
            readableAccent.copy(alpha = 0.18f)
        }

    /*
     * גרדיאנט עדין המבוסס על ערכת הנושא וצבע החגורה.
     */
    val panelGradientColors =
        listOf(
            panelColor,
            panelSecondaryColor.copy(
                alpha = if (isDarkMode) 0.92f else 0.72f
            ),
            readableAccent.copy(
                alpha = if (isDarkMode) 0.12f else 0.07f
            ),
            panelColor
        )

    Surface(
        shape = panelShape,
        color = panelColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 0.75.dp,
            color = borderAccent
        ),
        modifier = Modifier
            .widthIn(
                min = 190.dp,
                max = 230.dp
            )
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(panelShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = panelGradientColors
                    )
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                )
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
                                style = KmiTypography.cardTitle,
                                color = readableAccent,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription =
                                    if (isEnglish) {
                                        "Close quick menu"
                                    } else {
                                        "סגור תפריט מהיר"
                                    },
                                tint = readableAccent,
                                modifier = Modifier.size(
                                    KmiIconSize.small
                                )
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = BiasAbsoluteAlignment(1f, 0f)
                        ) {
                            Text(
                                text = title,
                                style = KmiTypography.cardTitle,
                                color = readableAccent,
                                textAlign = TextAlign.Right,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription =
                                    if (isEnglish) {
                                        "Close quick menu"
                                    } else {
                                        "סגור תפריט מהיר"
                                    },
                                tint = readableAccent,
                                modifier = Modifier.size(
                                    KmiIconSize.small
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                items.forEachIndexed { index, item ->
                    val lockedForUi = menuLocked && item.isLocked

                    PremiumQuickMenuRow(
                        text = item.title,
                        icon = item.icon,
                        accentColor = accentColor,
                        textColor = readableAccent,
                        lockColor = quickMenuLockTint(),
                        isEnglish = isEnglish,
                        isLocked = lockedForUi,
                        onClick = {
                            onItemClick {
                                if (lockedForUi) {
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
                            color = dividerColor
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
    textColor: Color,
    lockColor: Color,
    isEnglish: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val lockPulse = rememberInfiniteTransition(label = "quickMenuLockPulse")

    val lockScale by lockPulse.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "quickMenuLockScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = 4.dp,
                vertical = 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEnglish) {
            PremiumQuickMenuIcon(
                icon = icon,
                accentColor = accentColor
            )

            Spacer(Modifier.width(7.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = text,
                    style =
                        KmiTypography.action.copy(
                            fontWeight =
                                FontWeight.SemiBold
                        ),
                    color = textColor,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isLocked) {
                    Spacer(Modifier.width(5.dp))

                    PremiumAnimatedLockIcon(
                        accentColor = lockColor,
                        scale = lockScale
                    )
                }
            }
        } else {
            /*
             * בתצוגת RTL הרכיב הראשון מוצג בצד ימין.
             * לכן האייקון מופיע ראשון והמנעול אחרון.
             */
            PremiumQuickMenuIcon(
                icon = icon,
                accentColor = accentColor
            )

            Spacer(Modifier.width(7.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = text,
                    style =
                        KmiTypography.action.copy(
                            fontWeight =
                                FontWeight.SemiBold
                        ),
                    color = textColor,
                    textAlign = TextAlign.Right,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isLocked) {
                    Spacer(Modifier.width(5.dp))

                    PremiumAnimatedLockIcon(
                        accentColor = lockColor,
                        scale = lockScale
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumAnimatedLockIcon(
    accentColor: Color,
    scale: Float
) {
    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier
            .size(KmiIconSize.tiny)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = 1f
            }
    )
}

@Composable
private fun PremiumQuickMenuIcon(
    icon: ImageVector,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(
                scaledIconSize(20.dp)
            )
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.24f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(
                scaledIconSize(10.5.dp)
            )
        )
    }
}
