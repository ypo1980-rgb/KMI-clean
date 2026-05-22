package il.kmi.app.reminders

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import il.kmi.app.MainActivity
import il.kmi.app.domain.Explanations
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
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

private fun reminderTr(
    isEnglish: Boolean,
    he: String,
    en: String
): String = if (isEnglish) en else he

private fun reminderTextAlign(isEnglish: Boolean): TextAlign =
    if (isEnglish) TextAlign.Left else TextAlign.Right

private fun reminderHorizontalAlignment(isEnglish: Boolean): Alignment.Horizontal =
    if (isEnglish) Alignment.Start else Alignment.End

private fun reminderBeltNameForUi(
    beltId: String,
    isEnglish: Boolean
): String {
    val belt = Belt.fromId(beltId) ?: return beltId

    return if (isEnglish) {
        belt.en
    } else {
        belt.heb
    }
}

private fun reminderTitleForUi(
    raw: String,
    isEnglish: Boolean
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
                        }
                    )
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
                            val nextRawExplanation =
                                Explanations.get(nextPicked.belt, nextPicked.item).trim()

                            val nextExplanation = resolveDailyReminderExplanation(
                                beltId = nextPicked.belt.id,
                                item = nextPicked.item,
                                explanationFromIntent = nextRawExplanation,
                                isEnglish = isEnglish
                            )

                            startActivity(
                                Intent(this, DailyReminderCardActivity::class.java).apply {
                                    putExtra("daily_reminder_belt_id", nextPicked.belt.id)
                                    putExtra("daily_reminder_topic", nextPicked.topic)
                                    putExtra("daily_reminder_item", nextPicked.item)
                                    putExtra("daily_reminder_explanation", nextExplanation)
                                    putExtra("daily_reminder_extra_count", extraCount + 1)
                                }
                            )
                            finish()
                        }
                    }
                }
            )
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

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF061018),
            Color(0xFF0B2030),
            Color(0xFF123348),
            Color(0xFF091722)
        )
    )

    val glowBrush = Brush.radialGradient(
        colors = listOf(
            Color(0x443BD8FF),
            Color(0x225B6CFF),
            Color.Transparent
        )
    )

    val explanationBoxBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.86f),
            Color(0xFFF7F3FB).copy(alpha = 0.82f)
        )
    )

    val primaryButtonBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF7B61FF),
            Color(0xFF5A49E8),
            Color(0xFF6E57D2)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(glowBrush, shape = CircleShape)
        )

        AnimatedVisibility(
            visible = visible,
            enter =
                fadeIn(animationSpec = tween(260)) +
                        slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = tween(360)
                        ) +
                        scaleIn(
                            initialScale = 0.96f,
                            animationSpec = tween(360)
                        )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .shadow(
                        elevation = 36.dp,
                        shape = RoundedCornerShape(32.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFF8F5FC).copy(alpha = 0.95f),
                                Color(0xFFF0EAF8).copy(alpha = 0.90f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                radius = 900f
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
                            color = Color.White.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        localFavorite = !localFavorite
                                        onToggleFavorite()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (localFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = reminderTr(isEnglish, "מועדף", "Favorite"),
                                    tint = if (localFavorite) Color(0xFFFFC42D) else Color(0xFF6B6174)
                                )
                                Text(
                                    text = reminderTr(isEnglish, "מועדף", "Favorite"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF463C50)
                                )
                            }
                        }

                        Text(
                            text = reminderTr(isEnglish, "התרגיל היומי שלך", "Your daily exercise"),
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF231B28),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.52f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.62f))
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = reminderTr(isEnglish, "סגור", "Close"),
                                    tint = Color(0xFF382F42)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.38f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
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
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6A6071),
                                textAlign = uiTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = uiItem,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 24.sp,
                                color = Color(0xFF1E1723),
                                textAlign = uiTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 170.dp, max = 220.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(explanationBoxBrush)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = reminderTr(isEnglish, "הסבר", "Explanation"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2A2230),
                                textAlign = uiTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF302735),
                                        lineHeight = 19.sp,
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
                                                    Color(0xFFF4EFFA).copy(alpha = 0.96f)
                                                )
                                            ),
                                            shape = RectangleShape
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
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(Color(0xFF7C67E7))
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .width(26.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(Color(0xFFD6CFDE))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (extraCount < 3) {
                        GradientActionButton(
                            text = reminderTr(isEnglish, "תרגיל נוסף להיום", "Another exercise today"),
                            brush = primaryButtonBrush,
                            onClick = onAnotherExercise
                        )
                    }

                    PremiumOutlinedActionButton(
                        text = reminderTr(isEnglish, "אפשר תזמון מדויק", "Allow exact scheduling"),
                        onClick = onOpenExactAlarmSettings
                    )

                    PremiumOutlinedActionButton(
                        text = reminderTr(isEnglish, "הגדרות חיסכון סוללה", "Battery optimization settings"),
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
                                    Color(0xFF8A6BFF),
                                    Color(0xFF6B54F6)
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
        shadowElevation = 9.dp,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
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
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFB8ABC7)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.46f),
            contentColor = Color(0xFF5A4D69)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6A5B7A),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun resolveDailyReminderExplanation(
    beltId: String,
    item: String,
    explanationFromIntent: String,
    isEnglish: Boolean = false
): String {
    val cleanedIntentExplanation = cleanupDailyReminderExplanation(explanationFromIntent)

    if (
        cleanedIntentExplanation.isNotBlank() &&
        !isDailyReminderFallbackExplanation(cleanedIntentExplanation)
    ) {
        return cleanedIntentExplanation
    }

    val belt = Belt.fromId(beltId) ?: return reminderFallbackExplanation(isEnglish)

    fun String.cleanExerciseName(): String {
        return this
            .replace('–', '-')
            .replace('—', '-')
            .replace('־', '-')
            .replace("  ", " ")
            .trim()
    }

    val baseItem = item.trim()

    val candidates = buildList {
        add(baseItem)
        add(baseItem.cleanExerciseName())
        add(baseItem.substringBefore("(").trim())
        add(baseItem.substringBefore("(").trim().cleanExerciseName())
        add(baseItem.replace("-", "–").trim())
        add(baseItem.replace("–", "-").trim())
        add(baseItem.replace("'", "׳").trim())
        add(baseItem.replace("׳", "'").trim())
    }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    candidates.forEach { candidate ->
        val raw = Explanations.get(belt, candidate).trim()
        val cleaned = cleanupDailyReminderExplanation(raw)

        if (
            cleaned.isNotBlank() &&
            !isDailyReminderFallbackExplanation(cleaned)
        ) {
            return cleaned
        }
    }

    return reminderFallbackExplanation(isEnglish)
}

private fun cleanupDailyReminderExplanation(raw: String): String {
    val cleaned = raw.trim()

    if (cleaned.isBlank()) return ""

    return if ("::" in cleaned) {
        cleaned
            .split("::")
            .map { it.trim() }
            .lastOrNull { it.isNotBlank() }
            ?: cleaned
    } else {
        cleaned
    }
}

private fun isDailyReminderFallbackExplanation(text: String): Boolean {
    val clean = text.trim()

    return clean.isBlank() ||
            clean.startsWith("הסבר מפורט על") ||
            clean.startsWith("אין כרגע") ||
            clean.startsWith("Detailed explanation for:")
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

