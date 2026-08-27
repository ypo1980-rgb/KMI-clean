package il.kmi.app.reminders

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import il.kmi.app.MainActivity
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.shared.domain.Belt
import il.kmi.shared.reminders.DailyExercisePicker
import il.kmi.app.favorites.FavoritesStore
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.content.ExerciseTitlesEn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography

private fun reminderTr(
    isEnglish: Boolean, he: String, en: String
): String = if (isEnglish) en else he

private fun reminderTextAlign(isEnglish: Boolean): TextAlign =
    if (isEnglish) TextAlign.Left else TextAlign.Right

private fun reminderBeltNameForUi(
    beltId: String, isEnglish: Boolean
): String {
    val belt = Belt.fromId(beltId) ?: return beltId

    return if (isEnglish) {
        belt.en
    } else {
        belt.heb
    }
}

private fun reminderTitleForUi(
    raw: String, isEnglish: Boolean
): String {
    val clean = raw.trim()
    if (clean.isBlank()) return clean

    return if (isEnglish) {
        ExerciseTitlesEn.getOrSame(clean)
    } else {
        clean
    }
}

private fun reminderFallbackExplanation(
    isEnglish: Boolean
): String {
    return reminderTr(
        isEnglish,
        "אין כרגע הסבר לתרגיל הזה.",
        "No explanation is available for this exercise right now."
    )
}

class DailyReminderCardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val belt = intent.getStringExtra("daily_reminder_belt_id") ?: ""
        val topic = intent.getStringExtra("daily_reminder_topic") ?: ""
        val item = intent.getStringExtra("daily_reminder_item") ?: ""
        val explanationFromIntent = intent.getStringExtra("daily_reminder_explanation") ?: ""
        val extraCount = intent.getIntExtra("daily_reminder_extra_count", 0)

        val isEnglishForContent =
            AppLanguageManager(this).getCurrentLanguage() == AppLanguage.ENGLISH

        val explanation = resolveDailyReminderExplanation(
            beltId = belt,
            topic = topic,
            item = item,
            explanationFromIntent = explanationFromIntent,
            isEnglish = isEnglishForContent
        )

        setContent {

            val favorites by FavoritesStore.favoritesFlow.collectAsState(initial = emptySet())
            val isEnglish = remember(this) {
                AppLanguageManager(this).getCurrentLanguage() == AppLanguage.ENGLISH
            }

            ReminderCardUI(
                belt = belt,
                topic = topic,
                item = item,
                isEnglish = isEnglish,
                explanation = explanation,
                extraCount = extraCount,
                isFavorite = favorites.contains(item),
                onToggleFavorite = {
                    FavoritesStore.toggle(item)
                },
                onClose = { finish() },
                onOpenApp = {
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("open_from_daily_reminder", true)
                            putExtra("daily_reminder_belt_id", belt)
                            putExtra("daily_reminder_topic", topic)
                            putExtra("daily_reminder_item", item)
                        })
                    finish()
                },
                onOpenExactAlarmSettings = {
                    DailyReminderPowerHelper.openExactAlarmSettings(this)
                },
                onOpenBatteryOptimizationSettings = {
                    DailyReminderPowerHelper.openBatteryOptimizationSettings(this)
                },
                onAnotherExercise = {
                    val beltEnum = Belt.fromId(belt)
                    if (beltEnum != null) {
                        val picker = DailyExercisePicker()
                        val nextPicked = picker.pickNextExerciseForUser(
                            registeredBelt = previousBeltForTarget(beltEnum),
                            lastItemKey = "${beltEnum.name}|$topic|$item"
                        )

                        if (nextPicked != null) {
                            val nextExplanation = resolveDailyReminderExplanation(
                                beltId = nextPicked.belt.id,
                                topic = nextPicked.topic,
                                item = nextPicked.item,
                                explanationFromIntent = "",
                                isEnglish = isEnglish
                            )

                            startActivity(
                                Intent(this, DailyReminderCardActivity::class.java).apply {
                                    putExtra("daily_reminder_belt_id", nextPicked.belt.id)
                                    putExtra("daily_reminder_topic", nextPicked.topic)
                                    putExtra("daily_reminder_item", nextPicked.item)
                                    putExtra("daily_reminder_explanation", nextExplanation)
                                    putExtra("daily_reminder_extra_count", extraCount + 1)
                                })
                            finish()
                        }
                    }
                })
        }
    }
}

@Composable
private fun ReminderCardUI(
    belt: String,
    topic: String,
    item: String,
    isEnglish: Boolean,
    explanation: String,
    extraCount: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onAnotherExercise: () -> Unit
) {
    var localFavorite by remember(belt, topic, item, isFavorite) {
        mutableStateOf(isFavorite)
    }
    val scrollState = rememberScrollState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val uiBeltName = reminderBeltNameForUi(belt, isEnglish)
    val uiTopic = reminderTitleForUi(topic, isEnglish)
    val uiItem = reminderTitleForUi(item, isEnglish)
    val uiTextAlign = reminderTextAlign(isEnglish)
    val uiLayoutDirection = if (isEnglish) {
        LayoutDirection.Ltr
    } else {
        LayoutDirection.Rtl
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides uiLayoutDirection
    ) {

        val backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.background
            )
        )

        val glowBrush = Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                Color.Transparent
            )
        )

        val primaryButtonBrush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF7B61FF), Color(0xFF5A49E8), Color(0xFF6E57D2)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .navigationBarsPadding()
                .padding(
                    horizontal = 20.dp, vertical = 28.dp
                ), contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .aspectRatio(1f)
                    .background(
                        glowBrush, shape = CircleShape
                    )
            )

            AnimatedVisibility(
                visible = visible, enter = fadeIn(animationSpec = tween(260)) + slideInVertically(
                    initialOffsetY = { it / 3 }, animationSpec = tween(360)
                ) + scaleIn(
                    initialScale = 0.96f, animationSpec = tween(360)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    ), radius = 900f
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border = BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                                ),
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            }, indication = null
                                        ) {
                                            localFavorite = !localFavorite
                                            onToggleFavorite()
                                        }
                                        .padding(
                                            horizontal = 10.dp, vertical = 8.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = if (localFavorite) {
                                            Icons.Filled.Star
                                        } else {
                                            Icons.Outlined.StarBorder
                                        }, contentDescription = reminderTr(
                                            isEnglish, "מועדף", "Favorite"
                                        ), tint = if (localFavorite) {
                                            Color(0xFFFFC42D)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }, modifier = Modifier.size(
                                            KmiIconSize.small
                                        )
                                    )

                                    Text(
                                        text = reminderTr(
                                            isEnglish, "מועדף", "Favorite"
                                        ),
                                        style = KmiTypography.caption.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Text(
                                text = reminderTr(
                                    isEnglish, "התרגיל היומי שלך", "Your daily exercise"
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = KmiTypography.cardTitle.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                border = BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
                                )
                            ) {
                                IconButton(
                                    onClick = onClose, modifier = Modifier.size(
                                        KmiIconSize.medium
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = reminderTr(
                                            isEnglish, "סגור", "Close"
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$uiTopic • $uiBeltName",
                                    style = KmiTypography.caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = uiTextAlign,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = uiItem,
                                    style = KmiTypography.sectionTitle.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = uiTextAlign,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 170.dp, max = 220.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = reminderTr(
                                        isEnglish, "הסבר", "Explanation"
                                    ),
                                    style = KmiTypography.cardTitle.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = uiTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                            .padding(bottom = 20.dp)
                                    ) {
                                        Text(
                                            text = explanation.ifBlank {
                                                reminderTr(
                                                    isEnglish,
                                                    "אין הסבר זמין כרגע.",
                                                    "No explanation is available right now."
                                                )
                                            },
                                            style = KmiTypography.body,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = uiTextAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(
                                                            alpha = 0.96f
                                                        )
                                                    )
                                                ), shape = RectangleShape
                                            )
                                    )

                                    if (scrollState.canScrollForward) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(46.dp)
                                                    .height(4.dp)
                                                    .clip(
                                                        RoundedCornerShape(
                                                            100.dp
                                                        )
                                                    )
                                                    .background(
                                                        MaterialTheme.colorScheme.primary
                                                    )
                                            )

                                            Spacer(
                                                modifier = Modifier.width(8.dp)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .width(26.dp)
                                                    .height(4.dp)
                                                    .clip(
                                                        RoundedCornerShape(
                                                            100.dp
                                                        )
                                                    )
                                                    .background(
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (extraCount < 3) {
                            GradientActionButton(
                                text = reminderTr(
                                    isEnglish, "תרגיל נוסף להיום", "Another exercise today"
                                ), brush = primaryButtonBrush, onClick = onAnotherExercise
                            )
                        }

                        PremiumOutlinedActionButton(
                            text = reminderTr(
                                isEnglish, "אפשר תזמון מדויק", "Allow exact scheduling"
                            ), onClick = onOpenExactAlarmSettings
                        )

                        PremiumOutlinedActionButton(
                            text = reminderTr(
                                isEnglish, "הגדרות חיסכון סוללה", "Battery optimization settings"
                            ),
                            icon = Icons.Filled.Settings,
                            onClick = onOpenBatteryOptimizationSettings
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PremiumOutlinedActionButton(
                                text = reminderTr(isEnglish, "סגור", "Close"),
                                onClick = onClose,
                                modifier = Modifier.weight(1f)
                            )

                            GradientActionButton(
                                text = reminderTr(isEnglish, "פתח באפליקציה", "Open in app"),
                                icon = Icons.Filled.OpenInNew,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF8A6BFF), Color(0xFF6B54F6)
                                    )
                                ),
                                onClick = onOpenApp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun GradientActionButton(
    text: String,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp), contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.06f)
                            )
                        )
                    )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(
                            KmiIconSize.small
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = text,
                    style = KmiTypography.action.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PremiumOutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick, shape = RoundedCornerShape(20.dp), border = BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
        ), colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ), contentPadding = PaddingValues(
            horizontal = 12.dp, vertical = 0.dp
        ), modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(
                        KmiIconSize.small
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = text, style = KmiTypography.action.copy(
                    fontWeight = FontWeight.SemiBold
                ), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun resolveDailyReminderExplanation(
    beltId: String,
    topic: String,
    item: String,
    explanationFromIntent: String,
    isEnglish: Boolean = false
): String {
    val cleanedIntentExplanation = cleanupDailyReminderExplanation(explanationFromIntent)

    if (cleanedIntentExplanation.isNotBlank() && !isDailyReminderFallbackExplanation(
            cleanedIntentExplanation
        )
    ) {
        return cleanedIntentExplanation
    }

    val belt = Belt.fromId(beltId) ?: return reminderFallbackExplanation(isEnglish)

    val cleanTopic = topic.trim()
    val cleanItem = item.trim()

    val resolved = ExerciseExplanationResolver.get(
        belt = belt, topic = cleanTopic, item = cleanItem, isEnglish = isEnglish
    ).trim()

    val cleanedResolved = cleanupDailyReminderExplanation(resolved)

    if (cleanedResolved.isNotBlank() && !isDailyReminderFallbackExplanation(cleanedResolved)) {
        return cleanedResolved
    }

    return reminderFallbackExplanation(isEnglish)
}

private fun cleanupDailyReminderExplanation(raw: String): String {
    val cleaned = raw.trim()

    if (cleaned.isBlank()) return ""

    return if ("::" in cleaned) {
        cleaned.split("::").map { it.trim() }.lastOrNull { it.isNotBlank() } ?: cleaned
    } else {
        cleaned
    }
}

private fun isDailyReminderFallbackExplanation(text: String): Boolean {
    val clean = text.trim()

    return clean.isBlank() || clean.startsWith("הסבר מפורט על") || clean.startsWith("אין כרגע") || clean.startsWith(
        "Detailed explanation for:"
    ) || clean.startsWith("There is currently no explanation")
}

private fun previousBeltForTarget(targetBelt: Belt): Belt {
    return when (targetBelt) {
        Belt.YELLOW -> Belt.WHITE
        Belt.ORANGE -> Belt.YELLOW
        Belt.GREEN -> Belt.ORANGE
        Belt.BLUE -> Belt.GREEN
        Belt.BROWN -> Belt.BLUE
        Belt.BLACK -> Belt.BROWN
        Belt.WHITE -> Belt.WHITE
    }
}

