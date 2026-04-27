package il.kmi.app.screens.BeltQuestions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.domain.SubjectTopic
import il.kmi.shared.domain.Belt
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.DialogProperties


// -------------------------------------------------------------
// UI MODELS (הועברו מ-BeltQuestionsUiModels.kt)
// -------------------------------------------------------------

internal enum class TopicsViewMode {
    BY_BELT,
    BY_TOPIC
}

internal data class TopicDetails(
    val itemCount: Int,
    val subTitles: List<String>
) {
    val hasSubs: Boolean get() = subTitles.isNotEmpty()
}

internal data class CountsPayload(
    val subjectCounts: Map<String, Int>,
    val internalDefenseRootCount: Int,
    val externalDefenseRootCount: Int,
    val handsRootCount: Int,
    val totalDefenseCount: Int
)

internal data class UiExercise(
    val raw: String,
    val title: String
)

internal typealias ItemsByBelt =
        Map<il.kmi.shared.domain.Belt, List<UiExercise>>

@Composable
internal fun DefensePickModeDialogModern(
    kind: il.kmi.app.domain.DefenseKind,
    counts: Map<String, Int> = emptyMap(),
    hasAccess: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val isEnglish = il.kmi.app.localization.rememberIsEnglish()
    fun tr(he: String, en: String) = if (isEnglish) en else he

    val title =
        if (kind == il.kmi.app.domain.DefenseKind.INTERNAL) {
            tr("הגנות פנימיות", "Internal Defenses")
        } else {
            tr("הגנות חיצוניות", "External Defenses")
        }

    val accent =
        if (kind == il.kmi.app.domain.DefenseKind.INTERNAL) Color(0xFF4CAF50)
        else Color(0xFF2196F3)

    val keyPunch = "${kind.name}:אגרופים"
    val keyKick = "${kind.name}:בעיטות"

    val punchCount = counts[keyPunch] ?: 0
    val kickCount = counts[keyKick] ?: 0
    fun countLabel(n: Int) = if (isEnglish) "exercises $n" else "$n תרגילים"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = Color(0xFFF7F4FB),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 8.dp,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tr("מה לבחור?", "Choose a category"),
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                DrawerStylePickItem(
                    title = if (hasAccess) tr("אגרופים", "Punches") else tr("אגרופים 🔒", "Punches 🔒"),
                    subtitle = countLabel(punchCount),
                    accent = accent,
                    isEnglish = isEnglish,
                    onClick = { onPick("אגרופים") }
                )

                DrawerStylePickItem(
                    title = if (hasAccess) tr("בעיטות", "Kicks") else tr("בעיטות 🔒", "Kicks 🔒"),
                    subtitle = countLabel(kickCount),
                    accent = accent,
                    isEnglish = isEnglish,
                    onClick = { onPick("בעיטות") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(tr("ביטול", "Cancel"))
                    }
                }
            }
        }
    )
}

@Composable
internal fun HandsPickModeDialogModern(
    picks: List<String>,
    counts: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val isEnglish = il.kmi.app.localization.rememberIsEnglish()
    val accent = Color(0xFF8E24AA)

    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dialogBg = if (isDarkMode) Color(0xFF111827) else Color(0xFFF7F4FB)
    val dialogBorder = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFE3DDF0)
    val primaryTextColor = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF111827)
    val secondaryTextColor = if (isDarkMode) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0xFFD8D2E6).copy(alpha = 0.82f)

    val orderedPicks = picks.ifEmpty {
        listOf(
            if (isEnglish) "Hand Strikes" else "מכות יד",
            if (isEnglish) "Elbow Strikes" else "מכות מרפק",
            if (isEnglish) "Stick / Rifle Strikes" else "מכות במקל / רובה"
        )
    }

    fun tr(he: String, en: String) = if (isEnglish) en else he
    fun countLabel(n: Int) = if (isEnglish) "exercises $n" else "$n תרגילים"

    fun countForDisplay(pick: String): Int {
        counts[pick]?.let { return it }

        if (!isEnglish) return 0

        return when (pick.trim()) {
            "Hand Strikes" -> counts["מכות יד"] ?: 0
            "Elbow Strikes" -> counts["מכות מרפק"] ?: 0
            "Stick / Rifle Strikes" -> counts["מכות במקל / רובה"] ?: 0
            else -> 0
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 205.dp, start = 8.dp, end = 8.dp),
                    contentAlignment = if (isEnglish) {
                        BiasAbsoluteAlignment(-1f, -1f) // TopLeft
                    } else {
                        BiasAbsoluteAlignment(1f, -1f)  // TopRight
                    }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .wrapContentHeight()
                            .heightIn(max = 420.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { },
                        shape = RoundedCornerShape(30.dp),
                        tonalElevation = 10.dp,
                        shadowElevation = 22.dp,
                        color = dialogBg,
                        border = BorderStroke(1.dp, dialogBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isEnglish) {
                                    IconButton(onClick = onDismiss) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = tr("סגור", "Close")
                                        )
                                    }
                                }

                                Text(
                                    text = tr("עבודת ידיים", "Hand Techniques"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryTextColor,
                                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isEnglish) {
                                    IconButton(onClick = onDismiss) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = tr("סגור", "Close")
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = tr("בחר תת־נושא:", "Choose a sub-topic:"),
                                color = secondaryTextColor,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(10.dp))

                            Column(
                                modifier = Modifier.wrapContentHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                orderedPicks.forEach { pick ->
                                    DrawerStylePickItem(
                                        title = pick,
                                        subtitle = countLabel(countForDisplay(pick)),
                                        accent = accent,
                                        isEnglish = isEnglish,
                                        titleColorOverride = primaryTextColor,
                                        subtitleColorOverride = accent,
                                        dividerColorOverride = dividerColor,
                                        onClick = { onPick(pick) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubTopicsPickModeDialogModern(
    title: String,
    picks: List<String>,
    counts: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val isEnglish = il.kmi.app.localization.rememberIsEnglish()
    val accent = Color(0xFF5E35B1)

    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dialogBg = if (isDarkMode) Color(0xFF111827) else Color(0xFFF7F4FB)
    val dialogBorder = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFE3DDF0)
    val primaryTextColor = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF111827)
    val secondaryTextColor = if (isDarkMode) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0xFFD8D2E6).copy(alpha = 0.82f)

    fun tr(he: String, en: String) = if (isEnglish) en else he
    fun countLabel(n: Int) = if (isEnglish) "exercises $n" else "$n תרגילים"

    fun countForDisplay(pick: String): Int {
        counts[pick]?.let { return it }

        if (!isEnglish) return 0

        return when (pick.trim()) {
            "Release from Hand / Hair / Shirt Grabs" ->
                counts["שחרור מתפיסות ידיים / שיער / חולצה"] ?: 0

            "Choke Releases" ->
                counts["שחרור מחניקות"] ?: 0

            "Bear Hug Releases" ->
                counts["שחרור מחביקות"] ?: 0

            "Body Bear Hugs" ->
                counts["חביקות גוף"] ?: 0

            "Neck Bear Hugs" ->
                counts["חביקות צוואר"] ?: 0

            "Arm Bear Hugs" ->
                counts["חביקות זרוע"] ?: 0

            else -> 0
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 205.dp, start = 8.dp, end = 8.dp),
                    contentAlignment = if (isEnglish) {
                        BiasAbsoluteAlignment(-1f, -1f)
                    } else {
                        BiasAbsoluteAlignment(1f, -1f)
                    }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .wrapContentHeight()
                            .heightIn(max = 420.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { },
                        shape = RoundedCornerShape(30.dp),
                        tonalElevation = 10.dp,
                        shadowElevation = 22.dp,
                        color = dialogBg,
                        border = BorderStroke(1.dp, dialogBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isEnglish) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = tr("סגור", "Close")
                                    )
                                }
                            }

                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryTextColor,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )

                            if (isEnglish) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = tr("סגור", "Close")
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                            Text(
                                text = tr("בחר תת־נושא:", "Choose a sub-topic:"),
                                color = secondaryTextColor,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                        Spacer(Modifier.height(10.dp))

                            Column(
                                modifier = Modifier.wrapContentHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                picks.forEach { pick ->
                                    DrawerStylePickItem(
                                        title = pick,
                                        subtitle = countLabel(countForDisplay(pick)),
                                        accent = accent,
                                        isEnglish = isEnglish,
                                        titleColorOverride = primaryTextColor,
                                        subtitleColorOverride = accent,
                                        dividerColorOverride = dividerColor,
                                        onClick = { onPick(pick) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerStylePickItem(
    title: String,
    subtitle: String? = null,
    accent: Color,
    isEnglish: Boolean,
    titleColorOverride: Color? = null,
    subtitleColorOverride: Color? = null,
    dividerColorOverride: Color? = null,
    onClick: () -> Unit
) {
    val isLocked = title.contains("🔒")
    val cleanTitle = title.replace(" 🔒", "").trim()

    val titleColor = titleColorOverride ?: MaterialTheme.colorScheme.onSurface
    val subtitleColor = subtitleColorOverride ?: accent
    val dividerColor = dividerColorOverride ?: Color(0xFFD8D2E6).copy(alpha = 0.82f)
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.86f),
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }

                            Text(
                                text = cleanTitle,
                                style = if (isEnglish) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.titleSmall
                                },
                                fontWeight = FontWeight.ExtraBold,
                                color = titleColor,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                maxLines = if (isEnglish) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!subtitle.isNullOrBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = subtitleColor,
                                fontWeight = FontWeight.Bold,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    thickness = 1.dp,
                    color = dividerColor
                )
            }
        }
    }
}

@Composable
private fun ModernPickCard(
    title: String,
    accent: Color,
    icon: String? = null,
    countText: String? = null,
    onClick: () -> Unit
) {
    val dir = if (icon != null) LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides dir) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 1.dp,
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, Color(0xFFD8D2E6)),
            color = Color.White.copy(alpha = 0.94f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (icon != null) {
                            Text(
                                text = icon,
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = accent.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    countText?.let {
                        Spacer(Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F4F8)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun DefenseCategoryPickDialogModern(
    counts: Map<String, Int> = emptyMap(),
    hasAccess: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val isEnglish = il.kmi.app.localization.rememberIsEnglish()

    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dialogBg = if (isDarkMode) Color(0xFF111827) else Color(0xFFF7F4FB)
    val dialogBorder = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFE3DDF0)
    val primaryTextColor = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF111827)
    val secondaryTextColor = if (isDarkMode) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0xFFD8D2E6).copy(alpha = 0.82f)

    fun tr(he: String, en: String) = if (isEnglish) en else he
    fun countLabel(n: Int) = if (isEnglish) "exercises $n" else "$n תרגילים"

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismiss() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 92.dp, bottom = 20.dp, start = 8.dp, end = 8.dp),
                    contentAlignment = if (isEnglish) {
                        BiasAbsoluteAlignment(-1f, -1f)
                    } else {
                        BiasAbsoluteAlignment(1f, -1f)
                    }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .fillMaxHeight()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { },
                        shape = RoundedCornerShape(30.dp),
                        tonalElevation = 10.dp,
                        shadowElevation = 22.dp,
                        color = dialogBg,
                        border = BorderStroke(1.dp, dialogBorder)
                    ) {
                        val scrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isEnglish) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = tr("סגור", "Close")
                                    )
                                }
                            }

                            Text(
                                text = tr("הגנות", "Defenses"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryTextColor,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )

                            if (isEnglish) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = tr("סגור", "Close")
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(scrollState)
                                        .padding(bottom = 34.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    DrawerStylePickItem(
                                        title = if (hasAccess) tr("הגנות פנימיות", "Internal Defenses") else tr("הגנות פנימיות 🔒", "Internal Defenses 🔒"),
                                        subtitle = countLabel(counts["הגנות פנימיות"] ?: 0),
                                        accent = Color(0xFF2E7D32),
                                        isEnglish = isEnglish,
                                        titleColorOverride = primaryTextColor,
                                        subtitleColorOverride = Color(0xFF2E7D32),
                                        dividerColorOverride = dividerColor,
                                        onClick = { onPick("הגנות פנימיות") }
                                    )

                                    DrawerStylePickItem(
                                        title = if (hasAccess) tr("הגנות חיצוניות", "External Defenses") else tr("הגנות חיצוניות 🔒", "External Defenses 🔒"),
                                        subtitle = countLabel(counts["הגנות חיצוניות"] ?: 0),
                                        accent = Color(0xFF1565C0),
                                        isEnglish = isEnglish,
                                        onClick = { onPick("הגנות חיצוניות") }
                                    )

                                    DrawerStylePickItem(
                                        title = if (hasAccess) tr("הגנות נגד בעיטות", "Defenses Against Kicks") else tr("הגנות נגד בעיטות 🔒", "Defenses Against Kicks 🔒"),
                                        subtitle = countLabel(counts["הגנות נגד בעיטות"] ?: 0),
                                        accent = Color(0xFFFF9800),
                                        isEnglish = isEnglish,
                                        onClick = { onPick("kicks") }
                                    )

                                    DrawerStylePickItem(
                                        title = if (hasAccess) tr("הגנות מסכין", "Knife Defenses") else tr("הגנות מסכין 🔒", "Knife Defenses 🔒"),
                                        subtitle = countLabel(counts["הגנות מסכין"] ?: 0),
                                        accent = Color(0xFFE53935),
                                        isEnglish = isEnglish,
                                        onClick = { onPick("knife") }
                                    )

                                    DrawerStylePickItem(
                                        title = if (hasAccess) tr("הגנות עם רובה נגד דקירות סכין", "Rifle Defenses Against Knife Stabs") else tr("הגנות עם רובה נגד דקירות סכין 🔒", "Rifle Defenses Against Knife Stabs 🔒"),
                                        subtitle = countLabel(counts["הגנות עם רובה נגד דקירות סכין"] ?: 0),
                                        accent = Color(0xFFEF6C00),
                                        isEnglish = isEnglish,
                                        onClick = { onPick("knife_rifle") }
                                    )

                                    DrawerStylePickItem(
                                        title = if (hasAccess) tr("הגנות מאיום אקדח", "Gun Threat Defenses") else tr("הגנות מאיום אקדח 🔒", "Gun Threat Defenses 🔒"),
                                        subtitle = countLabel(counts["הגנות מאיום אקדח"] ?: 0),
                                        accent = Color(0xFF5E35B1),
                                        isEnglish = isEnglish,
                                        onClick = { onPick("gun") }
                                    )

                                    DrawerStylePickItem(
                                        title = if (hasAccess) tr("הגנות נגד מספר תוקפים", "Defenses Against Multiple Attackers") else tr("הגנות נגד מספר תוקפים 🔒", "Defenses Against Multiple Attackers 🔒"),
                                        subtitle = countLabel(counts["הגנות נגד מספר תוקפים"] ?: 0),
                                        accent = Color(0xFFD81B60),
                                        isEnglish = isEnglish,
                                        onClick = { onPick("multiple_attackers") }
                                    )

                                    DrawerStylePickItem(
                                        title = if (hasAccess) tr("הגנות נגד מקל", "Stick Defenses") else tr("הגנות נגד מקל 🔒", "Stick Defenses 🔒"),
                                        subtitle = countLabel(counts["הגנות נגד מקל"] ?: 0),
                                        accent = Color(0xFF00897B),
                                        isEnglish = isEnglish,
                                        onClick = { onPick("stick") }
                                    )
                                }

                                if (scrollState.canScrollForward) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 4.dp),
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isDarkMode) Color(0xFF1E293B) else Color.White.copy(alpha = 0.95f),
                                        shadowElevation = if (isDarkMode) 0.dp else 6.dp,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFE3DDF0)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "⌄",
                                                color = Color(0xFF7C4DFF),
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.titleMedium
                                            )

                                            Spacer(Modifier.width(6.dp))

                                            Text(
                                                text = tr("גלול למטה", "Scroll down"),
                                                color = Color(0xFF7C4DFF),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BaseTopicCard(
    title: String,
    subtitle: String,
    accent: Color,
    countText: String? = null,
    onClick: () -> Unit
) {
    val parts = countText
        ?.split("\n")
        ?.map { it.trim() }
        .orEmpty()

    val badgeTop = when {
        parts.size >= 2 -> parts[0]
        parts.size == 1 -> "תרגילים"
        else -> null
    }

    val badgeBottom = when {
        parts.size >= 2 -> parts[1].replace("תרגילים", "").trim()
        parts.size == 1 -> parts[0].replace("תרגילים", "").trim()
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color(0x10000000)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        val isEnglish = il.kmi.app.localization.rememberIsEnglish()

        CompositionLocalProvider(
            LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.92f))
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Right,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!badgeTop.isNullOrBlank() && !badgeBottom.isNullOrBlank()) {
                        CountBadge(
                            textTop = badgeTop,
                            textBottom = badgeBottom,
                            accent = accent
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun CountBadge(
    textTop: String,
    textBottom: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = textTop,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = textBottom,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun CountTextBadge(
    text: String,
    color: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 2,
        lineHeight = 14.sp
    )
}

@Composable
private fun subjectAccentFor(subjectId: String?): Color {
    return when (subjectId?.trim()) {

        // ROOT
        "defense_root" -> Color(0xFF7C4DFF)
        "hands_root" -> Color(0xFFAB47BC)

        // SUBJECTS
        "releases" -> Color(0xFF42A5F5)
        "releases_hugs" -> Color(0xFF5C6BC0)

        "rolls_and_falls" -> Color(0xFFBA68C8)

        "starting_position" -> Color(0xFFEF5350)

        "katerol" -> Color(0xFF43A047)

        "kicks" -> Color(0xFFFFA726)
        "kicks_hard" -> Color(0xFFFF9800)

        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
internal fun SubjectRootCard(
    title: String,
    subtitle: String,
    subjectId: String? = null,
    countText: String? = null,
    showLeftBadge: Boolean = true,
    onClick: () -> Unit
) {
    val accent = subjectAccentFor(subjectId)
    val isEnglish = LocalLayoutDirection.current == LayoutDirection.Ltr

    val lines = remember(countText, isEnglish) {
        val raw = countText.orEmpty()
        val translated = if (!isEnglish) {
            raw
        } else {
            raw
                .replace(Regex("""(\d+)\s+תתי\s*נושאים"""), "sub-topics $1")
                .replace(Regex("""(\d+)\s+תרגילים"""), "exercises $1")
        }

        translated
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color(0x12000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isEnglish && showLeftBadge) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (lines.isNotEmpty()) {
                    if (lines.size >= 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lines[0],
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                            )
                            Text(
                                text = lines[1],
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                textAlign = if (isEnglish) TextAlign.Right else TextAlign.Left
                            )
                        }
                    } else {
                        Text(
                            text = lines.first(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.size(17.dp)
            )

            if (isEnglish && showLeftBadge) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                )
            }
        }
    }
}

@Composable
internal fun SubjectLeafCard(
    title: String,
    countText: String,
    onClick: () -> Unit
) {
    val accent = when (title.trim()) {
        "הגנות" -> Color(0xFF6A5ACD)
        "עבודת ידיים" -> Color(0xFF9C27B0)
        "שחרורים" -> Color(0xFF2196F3)
        "בלימות וגלגולים" -> Color(0xFF9C27B0)
        "עמידת מוצא" -> Color(0xFFE53935)
        "קאטרול" -> Color(0xFF4CAF50)
        "בעיטות" -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, Color(0x0F000000)),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = 0.35f))
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Right,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

internal object SubjectTopicsUiLogic {

    data class TopicsUiCountsPayload(
        val subjectCounts: Map<String, Int>,
        val handsRootCount: Int,
        val handsPickCounts: Map<String, Int>,
        val uiSectionCounts: Map<String, Int>,
        val subTopicsPickCountsBySubjectId: Map<String, Map<String, Int>>
    )

    // ✅ PERF CACHE: שומר את ה-payload בזיכרון כדי לא לחשב מחדש בכל פתיחת מסך
    private object TopicsUiCountsMemoryCache {
        @Volatile
        private var payload: TopicsUiCountsPayload? = null

        fun get(): TopicsUiCountsPayload? = payload

        fun put(value: TopicsUiCountsPayload) {
            payload = value
        }

        fun clear() {
            payload = null
        }
    }

    fun getCachedTopicsUiCountsPayload(): TopicsUiCountsPayload? =
        TopicsUiCountsMemoryCache.get()

    fun cacheTopicsUiCountsPayload(value: TopicsUiCountsPayload) {
        TopicsUiCountsMemoryCache.put(value)
    }

    fun clearCachedTopicsUiCountsPayload() {
        TopicsUiCountsMemoryCache.clear()
    }

    fun ensureTopicsUiCountsPreloaded(
        subjects: List<SubjectTopic>,
        handsBase: SubjectTopic?
    ) {
        if (getCachedTopicsUiCountsPayload() != null) return

        val payload = buildTopicsUiCountsPayload(
            subjects = subjects,
            handsBase = handsBase
        )

        cacheTopicsUiCountsPayload(payload)
    }

    private fun normReleaseTitle(raw: String): String =
        raw.trim()
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .lowercase()

    private fun releasesSectionIdForTitle(raw: String): String? {
        return when (normReleaseTitle(raw)) {
            "שחרור מתפיסות ידיים / שיער / חולצה" -> "releases_hands_hair_shirt"
            "שחרור מחניקות" -> "releases_chokes"
            "שחרור מחביקות" -> "releases_hugs"
            "חביקות גוף" -> "releases_hugs_body"
            "חביקות צוואר" -> "releases_hugs_neck"
            "חביקות זרוע" -> "releases_hugs_arm"
            else -> null
        }
    }

    private fun hardSectionTotalItems(sectionId: String): Int {
        val section = il.kmi.shared.domain.content.HardSectionsCatalog.findSectionById(
            subjectId = "releases",
            sectionId = sectionId
        ) ?: return 0

        fun countDeep(
            s: il.kmi.shared.domain.content.HardSectionsCatalog.Section
        ): Int {
            return if (s.subSections.isNotEmpty()) {
                s.subSections.sumOf { child -> countDeep(child) }
            } else {
                s.beltGroups.sumOf { group -> group.items.size }
            }
        }

        return countDeep(section)
    }

    private fun releasesCountForPick(raw: String): Int {
        val sectionId = releasesSectionIdForTitle(raw) ?: return 0
        return hardSectionTotalItems(sectionId)
    }

    fun buildTopicsUiCountsPayload(
        subjects: List<SubjectTopic>,
        handsBase: SubjectTopic?
    ): TopicsUiCountsPayload {
        val subjectCounts = subjects.associate { subject ->
            subject.id to SubjectTopicsEngine.countUiTitlesForSubject(subject)
        }

        val handsPicksOrder: List<String> =
            handsPicks(handsBase)

        val handsPickCounts: Map<String, Int> =
            handsPicksOrder.associateWith { pick ->
                val base = handsBase
                if (base == null) {
                    0
                } else {
                    val tmp = SubjectTopicsEngine.handsSubjectForPick(base, pick)
                    SubjectTopicsEngine.countUiTitlesForSubject(tmp)
                }
            }

        val handsRootCount: Int = run {
            val base = handsBase ?: return@run 0
            val all = linkedSetOf<String>()

            handsPicksOrder.forEach { pick ->
                val tmp = SubjectTopicsEngine.handsSubjectForPick(base, pick)

                tmp.topicsByBelt.keys.forEach { belt ->
                    SubjectTopicsEngine.resolveSectionsForSubject(belt, tmp)
                        .asSequence()
                        .flatMap { it.items.asSequence() }
                        .map { it.canonicalId }
                        .forEach { all += it }
                }
            }

            all.size
        }

        val uiSectionCounts: Map<String, Int> =
            subjects.associate { subject ->
                subject.id to subject.subTopics.size
            }

        val subTopicsPickCountsBySubjectId: Map<String, Map<String, Int>> =
            subjects
                .asSequence()
                .filter { it.subTopics.isNotEmpty() }
                .associate { base ->
                    val countsForBase =
                        base.subTopics.associateWith { pick ->
                            when (base.id) {
                                "hands_all" -> {
                                    val tmp = SubjectTopicsEngine.handsSubjectForPick(base, pick)
                                    SubjectTopicsEngine.countUiTitlesForSubject(tmp)
                                }

                                "releases",
                                "releases_hugs" -> {
                                    releasesCountForPick(pick)
                                }

                                else -> {
                                    val tmp = SubjectTopicsEngine.subjectForPick(base, pick)
                                    SubjectTopicsEngine.countUiTitlesForSubject(tmp)
                                }
                            }
                        }

                    base.id to countsForBase
                }

        return TopicsUiCountsPayload(
            subjectCounts = subjectCounts,
            handsRootCount = handsRootCount,
            handsPickCounts = handsPickCounts,
            uiSectionCounts = uiSectionCounts,
            subTopicsPickCountsBySubjectId = subTopicsPickCountsBySubjectId
        )
    }

    data class OpenSubjectDecision(
        val chosenBelt: Belt,
        val nonEmptyBelts: List<Belt>,
        val resolverSections: Int,
        val resolverItems: Int,
        val sample: List<String>
    )

    data class SubTopicsDialogData(
        val base: SubjectTopic?,
        val bodyHugsChild: SubjectTopic?,
        val picks: List<String>
    )

    sealed interface SubTopicPickDecision {
        data class OpenTopicWithSub(
            val topic: String,
            val subTopic: String
        ) : SubTopicPickDecision

        data class OpenSubject(
            val subject: SubjectTopic
        ) : SubTopicPickDecision

        data object None : SubTopicPickDecision
    }

    fun isDefenseChild(subject: SubjectTopic): Boolean {
        val t = subject.titleHeb.trim()

        val isWeaponOrKicks =
            t.contains("בעיטות") || t.contains("סכין") || t.contains("אקדח") || t.contains("מקל")

        val isOldInternalExternal =
            (t.contains("הגנות פנימיות") || t.contains("הגנות חיצוניות")) &&
                    (t.contains("אגרופים") || t.contains("בעיטות"))

        return t.contains("הגנות") && (isWeaponOrKicks || isOldInternalExternal)
    }

    fun isHandsChild(subject: SubjectTopic): Boolean {
        val t = subject.titleHeb.trim()
        if (t == "עבודת ידיים") return true
        return t.startsWith("עבודת ידיים") && (t.contains("-") || t.contains("–"))
    }

    fun visibleSubjects(subjects: List<SubjectTopic>): List<SubjectTopic> {
        return subjects.filter { !isDefenseChild(it) && !isHandsChild(it) }
    }

    data class VisibleSubjectsSplit(
        val withSubTopics: List<SubjectTopic>,
        val withoutSubTopics: List<SubjectTopic>
    )

    data class SubjectCardModel(
        val id: String,
        val title: String,
        val subtitle: String,
        val countText: String,
        val hasSubTopics: Boolean
    )

    data class RootCardModel(
        val title: String,
        val countText: String
    )

    fun splitVisibleSubjects(
        subjects: List<SubjectTopic>,
        sectionCounts: Map<String, Int>
    ): VisibleSubjectsSplit {

        val visible = visibleSubjects(subjects)

        val (withSubTopics, withoutSubTopics) = visible.partition { subject ->
            val subCount = sectionCounts[subject.id] ?: subject.subTopics.size
            subCount > 0
        }

        return VisibleSubjectsSplit(
            withSubTopics = withSubTopics,
            withoutSubTopics = withoutSubTopics
        )
    }

    fun buildSubjectCardModels(
        subjects: List<SubjectTopic>,
        sectionCounts: Map<String, Int>,
        subjectCounts: Map<String, Int>,
        formatCount: (Int) -> String
    ): List<SubjectCardModel> {
        return subjects.map { subject ->
            val subCount = sectionCounts[subject.id] ?: subject.subTopics.size
            val exCount = subjectCounts[subject.id] ?: 0
            val hasSubTopics = subCount > 0

            SubjectCardModel(
                id = subject.id,
                title = subject.titleHeb,
                subtitle = if (hasSubTopics) "בחר תת־נושא" else "כניסה לתרגילים",
                countText = if (hasSubTopics) {
                    "$subCount תתי נושאים\n${formatCount(exCount)}"
                } else {
                    formatCount(exCount)
                },
                hasSubTopics = hasSubTopics
            )
        }
    }

    fun buildDefenseRootCard(
        totalDefense: Int,
        formatCount: (Int) -> String,
        subTopicsCount: Int
    ): RootCardModel {
        return RootCardModel(
            title = "הגנות",
            countText = "$subTopicsCount תתי נושאים\n${formatCount(totalDefense)}"
        )
    }

    fun buildHandsRootCard(
        handsRootCount: Int,
        formatCount: (Int) -> String,
        subTopicsCount: Int
    ): RootCardModel {
        return RootCardModel(
            title = "עבודת ידיים",
            countText = "$subTopicsCount תתי נושאים\n${formatCount(handsRootCount)}"
        )
    }

    fun choosePreferredBelt(
        subject: SubjectTopic,
        currentBelt: Belt,
        nonEmptyBelts: List<Belt>
    ): Belt {

        val preferredOrder: List<Belt> = when {
            subject.id == "hands_all" && subject.subTopicHint == "מכות יד" ->
                listOf(Belt.YELLOW, Belt.ORANGE, currentBelt, Belt.GREEN)

            subject.id == "hands_all" && subject.subTopicHint == "מכות מרפק" ->
                listOf(Belt.GREEN, currentBelt, Belt.YELLOW, Belt.ORANGE)

            else -> emptyList()
        }

        return preferredOrder.firstOrNull { it in nonEmptyBelts }
            ?: nonEmptyBelts.firstOrNull()
            ?: preferredOrder.firstOrNull { it in subject.topicsByBelt.keys }
            ?: subject.topicsByBelt.keys.firstOrNull()
            ?: currentBelt
    }

    fun buildOpenSubjectDecision(
        subject: SubjectTopic,
        currentBelt: Belt
    ): OpenSubjectDecision {
        val nonEmptyBelts = SubjectTopicsEngine.beltsWithItemsForSubject(subject)

        val chosenBelt = choosePreferredBelt(
            subject = subject,
            currentBelt = currentBelt,
            nonEmptyBelts = nonEmptyBelts
        )

        val secs = SubjectTopicsEngine.resolveSectionsForSubject(chosenBelt, subject)
        val itemsCount = secs.sumOf { it.items.size }
        val sample = secs
            .asSequence()
            .flatMap { it.items.asSequence() }
            .take(8)
            .map { it.canonicalId }
            .toList()

        return OpenSubjectDecision(
            chosenBelt = chosenBelt,
            nonEmptyBelts = nonEmptyBelts,
            resolverSections = secs.size,
            resolverItems = itemsCount,
            sample = sample
        )
    }

    fun buildOpenSubjectLog(
        subject: SubjectTopic,
        decision: OpenSubjectDecision
    ): String {
        return "openSubjectSmart: " +
                "title='${subject.titleHeb}' " +
                "id='${subject.id}' " +
                "hint='${subject.subTopicHint}' " +
                "chosenBelt=${decision.chosenBelt} " +
                "nonEmptyBelts=${decision.nonEmptyBelts} " +
                "topicsByBelt=${subject.topicsByBelt} " +
                "resolverSections=${decision.resolverSections} " +
                "resolverItems=${decision.resolverItems} " +
                "sample=${decision.sample}"
    }

    data class OpenSubjectUiAction(
        val chosenBelt: Belt,
        val logMessage: String
    )

    fun buildOpenSubjectUiAction(
        subject: SubjectTopic,
        currentBelt: Belt
    ): OpenSubjectUiAction {
        val decision = buildOpenSubjectDecision(
            subject = subject,
            currentBelt = currentBelt
        )

        return OpenSubjectUiAction(
            chosenBelt = decision.chosenBelt,
            logMessage = buildOpenSubjectLog(
                subject = subject,
                decision = decision
            )
        )
    }

    fun buildSubTopicsDialogData(
        subjects: List<SubjectTopic>,
        id: String
    ): SubTopicsDialogData {
        val base = subjects.firstOrNull { it.id == id }

        val bodyHugsChild =
            if (base?.id == "releases") {
                subjects.firstOrNull { it.id == "releases_hugs" }
            } else {
                null
            }

        val picks = when {
            base == null -> emptyList()
            else -> base.subTopics
        }

        return SubTopicsDialogData(
            base = base,
            bodyHugsChild = bodyHugsChild,
            picks = picks
        )
    }

    fun handsPicks(base: SubjectTopic?): List<String> {
        return base?.subTopics?.takeIf { it.isNotEmpty() }
            ?: listOf(
                "מכות יד",
                "מכות מרפק",
                "מכות במקל / רובה"
            )
    }

    fun resolveHandsPick(
        base: SubjectTopic?,
        picked: String
    ): SubjectTopic? {
        if (base == null) return null
        return SubjectTopicsEngine.handsSubjectForPick(base, picked)
    }

    fun unifiedSubjectIdOrNull(
        subject: SubjectTopic
    ): String? {
        return when (subject.id) {
            "hands_all" -> "hands_all"
            else -> null
        }
    }

    sealed interface DefenseDialogDecision {
        data class AskKind(
            val kind: il.kmi.app.domain.DefenseKind
        ) : DefenseDialogDecision

        data class OpenHardSubject(
            val subjectId: String
        ) : DefenseDialogDecision

        data object None : DefenseDialogDecision
    }

    fun resolveDefenseDialogPick(
        picked: String
    ): DefenseDialogDecision {
        val p = picked.trim()

        return when {

            p.contains("פנימ") || p == "internal" -> {
                DefenseDialogDecision.AskKind(il.kmi.app.domain.DefenseKind.INTERNAL)
            }

            p.contains("חיצונ") || p == "external" -> {
                DefenseDialogDecision.AskKind(il.kmi.app.domain.DefenseKind.EXTERNAL)
            }

            p.contains("בעיט") || p == "kicks" -> {
                DefenseDialogDecision.OpenHardSubject("kicks_hard")
            }

            p == "knife_rifle" || p.contains("רובה") -> {
                DefenseDialogDecision.OpenHardSubject("knife_rifle_defense")
            }

            p == "multiple_attackers" || p.contains("מספר תוקפים") || p.contains("2 תוקפים") -> {
                DefenseDialogDecision.OpenHardSubject("multiple_attackers_defense")
            }

            p.contains("סכין") || p == "knife" -> {
                DefenseDialogDecision.OpenHardSubject("knife_defense")
            }

            p.contains("אקדח") || p == "gun" -> {
                DefenseDialogDecision.OpenHardSubject("gun_threat_defense")
            }

            p.contains("מקל") || p == "stick" -> {
                DefenseDialogDecision.OpenHardSubject("stick_defense")
            }

            else -> DefenseDialogDecision.None
        }
    }

    sealed interface DefenseKindPickDecision {
        data class OpenLegacyDefenses(
            val kind: String,
            val pick: String
        ) : DefenseKindPickDecision

        data class OpenHardSubject(
            val subjectId: String
        ) : DefenseKindPickDecision

        data object None : DefenseKindPickDecision
    }

    fun resolveDefenseKindPick(
        kind: il.kmi.app.domain.DefenseKind,
        picked: String
    ): DefenseKindPickDecision {

        val kindKey = when (kind) {
            il.kmi.app.domain.DefenseKind.INTERNAL -> "internal"
            il.kmi.app.domain.DefenseKind.EXTERNAL -> "external"
            else -> "all"
        }

        val pickKey = when {
            picked.contains("אגרופ") -> "punch"
            picked.contains("בעיט") -> "kick"
            else -> "punch"
        }

        return when {
            kindKey == "all" && pickKey == "kick" -> {
                DefenseKindPickDecision.OpenHardSubject(
                    subjectId = "kicks_hard"
                )
            }

            else -> {
                DefenseKindPickDecision.OpenLegacyDefenses(
                    kind = kindKey,
                    pick = pickKey
                )
            }
        }
    }

    fun resolveSubTopicPick(
        base: SubjectTopic?,
        bodyHugsChild: SubjectTopic?,
        picked: String,
        norm: (String) -> String
    ): SubTopicPickDecision {
        if (base == null) return SubTopicPickDecision.None

        val pickedNorm = norm(picked)

        if (bodyHugsChild != null && pickedNorm == norm(bodyHugsChild.titleHeb)) {
            return SubTopicPickDecision.OpenSubject(bodyHugsChild)
        }

        return SubTopicPickDecision.OpenSubject(
            SubjectTopicsEngine.subjectForPick(base, picked)
        )
    }
}