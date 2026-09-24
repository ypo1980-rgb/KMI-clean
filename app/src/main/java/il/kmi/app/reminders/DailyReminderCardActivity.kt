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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.StyledExplanationText
import il.yuval.ui.theme.kmiScreenBackgroundBrush

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

        val glowBrush = Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                Color.Transparent
            )
        )

        val primaryButtonBrush = Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                MaterialTheme.colorScheme.secondary
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(kmiScreenBackgroundBrush())
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .aspectRatio(1f)
                    .background(
                        glowBrush,
                        shape = CircleShape
                    )
            )

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(260)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(360)
                        ) +
                        scaleIn(
                            initialScale = 0.97f,
                            animationSpec = tween(360)
                        )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.94f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(26.dp)
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
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 38.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (localFavorite) {
                                    Color(0xFFFFC42D).copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (localFavorite) {
                                        Color(0xFFFFB300).copy(alpha = 0.46f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                    }
                                ),
                                shadowElevation = 0.dp
                            ) {
                                IconButton(
                                    onClick = {
                                        localFavorite = !localFavorite
                                        onToggleFavorite()
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (localFavorite) {
                                            Icons.Filled.Star
                                        } else {
                                            Icons.Outlined.StarBorder
                                        },
                                        contentDescription = reminderTr(
                                            isEnglish,
                                            "הוספה למועדפים",
                                            "Add to favorites"
                                        ),
                                        tint = if (localFavorite) {
                                            Color(0xFFFFB300)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(KmiIconSize.small)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Text(
                                    text = reminderTr(
                                        isEnglish,
                                        "התרגיל היומי שלך",
                                        "Your daily exercise"
                                    ),
                                    style = KmiTypography.cardTitle.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = reminderTr(
                                        isEnglish,
                                        "צעד קטן. התקדמות גדולה.",
                                        "One step. Real progress."
                                    ),
                                    style = KmiTypography.caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                ),
                                shadowElevation = 0.dp
                            ) {
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = reminderTr(
                                            isEnglish,
                                            "סגור",
                                            "Close"
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(KmiIconSize.small)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                                            )
                                        )
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        )
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        )
                                    ) {
                                        Text(
                                            text = "$uiTopic  •  $uiBeltName",
                                            style = KmiTypography.caption.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 4.dp
                                            )
                                        )
                                    }

                                    Text(
                                        text = uiItem,
                                        style = KmiTypography.sectionTitle.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Box(
                                        modifier = Modifier
                                            .width(38.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                                            )
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                            ),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .heightIn(min = 110.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = reminderTr(
                                            isEnglish,
                                            "הסבר",
                                            "Explanation"
                                        ),
                                        style = KmiTypography.cardTitle.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = uiTextAlign
                                    )

                                    Box(
                                        modifier = Modifier
                                            .width(34.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary
                                                    )
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                            .padding(
                                                start = 14.dp,
                                                end = 22.dp,
                                                bottom = 26.dp
                                            )
                                    ) {
                                        StyledExplanationText(
                                            raw = explanation.ifBlank {
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
                                            .height(46.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        MaterialTheme.colorScheme.surface.copy(
                                                            alpha = 0.96f
                                                        )
                                                    )
                                                )
                                            )
                                    )

                                    if (scrollState.maxValue > 0) {
                                        val scrollProgress =
                                            scrollState.value.toFloat() /
                                                    scrollState.maxValue.toFloat()

                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 7.dp)
                                                .width(4.dp)
                                                .fillMaxHeight(0.72f)
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(
                                                    MaterialTheme.colorScheme.outline.copy(
                                                        alpha = 0.12f
                                                    )
                                                )
                                                .padding(vertical = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Spacer(
                                                modifier = Modifier.weight(
                                                    scrollProgress.coerceAtLeast(0.01f)
                                                )
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(100.dp))
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.72f
                                                        )
                                                    )
                                            )

                                            Spacer(
                                                modifier = Modifier.weight(
                                                    (1f - scrollProgress).coerceAtLeast(0.01f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (extraCount < 3) {
                                GradientActionButton(
                                    text = reminderTr(
                                        isEnglish,
                                        "תרגיל נוסף להיום",
                                        "Another exercise today"
                                    ),
                                    brush = primaryButtonBrush,
                                    onClick = onAnotherExercise
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                PremiumOutlinedActionButton(
                                    text = reminderTr(
                                        isEnglish,
                                        "תזמון מדויק",
                                        "Exact scheduling"
                                    ),
                                    onClick = onOpenExactAlarmSettings,
                                    modifier = Modifier.weight(1f)
                                )

                                PremiumOutlinedActionButton(
                                    text = reminderTr(
                                        isEnglish,
                                        "חיסכון בסוללה",
                                        "Battery settings"
                                    ),
                                    icon = Icons.Filled.Settings,
                                    onClick = onOpenBatteryOptimizationSettings,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PremiumOutlinedActionButton(
                                    text = reminderTr(
                                        isEnglish,
                                        "סגור",
                                        "Close"
                                    ),
                                    onClick = onClose,
                                    modifier = Modifier.weight(0.82f)
                                )

                                GradientActionButton(
                                    text = reminderTr(
                                        isEnglish,
                                        "פתח באפליקציה",
                                        "Open in app"
                                    ),
                                    icon = Icons.Filled.OpenInNew,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    onClick = onOpenApp,
                                    modifier = Modifier.weight(1.18f)
                                )
                            }
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
    val buttonShape = RoundedCornerShape(12.dp)

    Surface(
        onClick = onClick,
        shape = buttonShape,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp)
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.24f),
                    shape = buttonShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.10f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.32f))
            )

            Row(
                modifier = Modifier.padding(
                    horizontal = 9.dp,
                    vertical = 4.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(KmiIconSize.small)
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = text,
                    style = KmiTypography.action.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
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
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.52f
            ),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(
            horizontal = 7.dp,
            vertical = 1.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(KmiIconSize.small)
                )

                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = text,
                style = KmiTypography.action.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

