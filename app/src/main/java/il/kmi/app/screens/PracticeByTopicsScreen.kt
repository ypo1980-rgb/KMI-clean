package il.kmi.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.domain.ContentRepo
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.domain.Belt
import java.net.URLDecoder
import il.kmi.app.ui.ext.color
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBrush
import il.yuval.ui.theme.kmiSectionHeaderContentColor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.practice.PracticeBottomControls
import il.kmi.app.ui.practice.PracticeStartConfigDialog

private const val TOPICS_PICK_TOKEN =
    "__TOPICS_PICK__"

private data class PracticeByTopicsItem(
    val belt: Belt,
    val topic: String,
    val subTopic: String?,
    val title: String
)

private fun findPracticeByTopicsExplanation(
    item: PracticeByTopicsItem
): String {
    val resolved =
        ExerciseExplanationResolver.get(
            belt = item.belt,
            topic = item.topic,
            item = item.title,
            isEnglish = false
        ).trim()

    return resolved.ifBlank {
        "אין עדיין הסבר זמין לתרגיל זה."
    }
}

private fun decodePracticeTokenPart(
    value: String
): String =
    runCatching {
        URLDecoder.decode(
            value,
            "UTF-8"
        )
    }.getOrDefault(value)

private fun parsePracticeByTopicsSelection(
    token: String
): Map<Belt, List<String>> {

    if (
        !token.startsWith(
            "$TOPICS_PICK_TOKEN:"
        )
    ) {
        return emptyMap()
    }

    val payload =
        token
            .removePrefix(
                "$TOPICS_PICK_TOKEN:"
            )
            .trim()

    if (payload.isBlank()) {
        return emptyMap()
    }

    return payload
        .split(";")
        .mapNotNull { segment ->

            val parts =
                segment.split(
                    "|",
                    limit = 2
                )

            if (parts.size != 2) {
                return@mapNotNull null
            }

            val belt =
                Belt.fromId(
                    parts[0].trim()
                )
                    ?: return@mapNotNull null

            val topics =
                parts[1]
                    .split(",")
                    .map {
                        decodePracticeTokenPart(
                            it.trim()
                        )
                    }
                    .filter {
                        it.isNotBlank()
                    }

            if (topics.isEmpty()) {
                return@mapNotNull null
            }

            belt to topics
        }
        .groupBy(
            keySelector = {
                it.first
            },
            valueTransform = {
                it.second
            }
        )
        .mapValues { (_, lists) ->
            lists
                .flatten()
                .distinct()
        }
}

private fun buildPracticeByTopicsItems(
    selectionToken: String
): List<PracticeByTopicsItem> {

    val selection =
        parsePracticeByTopicsSelection(
            selectionToken
        )

    return selection
        .flatMap { (belt, topicTokens) ->

            topicTokens.flatMap { topicToken ->

                val cleanToken =
                    topicToken.trim()

                val isSubTopic =
                    cleanToken.startsWith(
                        "__SUBTOPIC__:"
                    )

                val topic =
                    if (isSubTopic) {
                        cleanToken
                            .removePrefix(
                                "__SUBTOPIC__:"
                            )
                            .split(
                                "::",
                                limit = 2
                            )
                            .getOrNull(0)
                            ?.trim()
                            .orEmpty()
                    } else {
                        cleanToken
                    }

                val subTopic =
                    if (isSubTopic) {
                        cleanToken
                            .removePrefix(
                                "__SUBTOPIC__:"
                            )
                            .split(
                                "::",
                                limit = 2
                            )
                            .getOrNull(1)
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                    } else {
                        null
                    }

                if (topic.isBlank()) {
                    return@flatMap emptyList()
                }

                ContentRepo
                    .listItemTitles(
                        belt = belt,
                        topicTitle = topic,
                        subTopicTitle = subTopic
                    )
                    .map { rawItem ->
                        PracticeByTopicsItem(
                            belt = belt,
                            topic = topic,
                            subTopic = subTopic,
                            title =
                                rawItem.trim()
                        )
                    }
                    .filter {
                        it.title.isNotBlank()
                    }
            }
        }
        .distinctBy { item ->
            buildString {
                append(item.belt.id)
                append("|")
                append(item.topic)
                append("|")
                append(
                    item.subTopic.orEmpty()
                )
                append("|")
                append(item.title)
            }
        }
}

@Composable
fun PracticeByTopicsScreen(
    selectionToken: String,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    val practiceItems =
        remember(selectionToken) {
            buildPracticeByTopicsItems(
                selectionToken
            )
        }

    var currentIndex by
    remember(selectionToken) {
        mutableIntStateOf(0)
    }

    var showStartDialog by
    rememberSaveable {
        mutableStateOf(true)
    }

    var showHelp by
    rememberSaveable {
        mutableStateOf(false)
    }

    var durationMinutes by
    rememberSaveable {
        mutableStateOf(1)
    }

    var halfAlertEnabled by
    rememberSaveable {
        mutableStateOf(true)
    }

    var last10AlertEnabled by
    rememberSaveable {
        mutableStateOf(true)
    }

    PracticeStartConfigDialog(
        show = showStartDialog,
        isEnglish = false,
        initialMinutes = durationMinutes,
        initialHalfAlert = halfAlertEnabled,
        initialLast10Alert = last10AlertEnabled,
        onDismiss = {
            onBack()
        },
        onConfirm = {
                durationSeconds,
                playHalf,
                playCountdown ->

            durationMinutes =
                (durationSeconds / 60)
                    .coerceAtLeast(1)

            halfAlertEnabled =
                playHalf

            last10AlertEnabled =
                playCountdown

            currentIndex = 0
            showStartDialog = false
        }
    )

    val currentItem =
        practiceItems
            .getOrNull(currentIndex)

    val totalItems =
        practiceItems.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush =
                    kmiScreenBackgroundBrush()
            )
    ) {
        KmiTopBar(
            title = "תרגול לפי נושא",
            onBack = onBack,
            onHome = onHome,
            showMenu = true,
            showBottomActions = true,
            showBottomHelp = false,
            showBottomShare = false,
            showTopSearch = false,
            showTopShare = false,
            showSettings = false,
            showCoachBroadcastFab = false
        )

        /*
         * הפס הכחול הגלובלי נשאר קבוע
         * מתחת לכותרת.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush =
                        kmiSectionHeaderBrush()
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 7.dp
                ),
            contentAlignment =
                Alignment.Center
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {
                Text(
                    text =
                        "תרגול מכל החגורות",
                    modifier =
                        Modifier.fillMaxWidth(),
                    color =
                        kmiSectionHeaderContentColor(),
                    style =
                        KmiTypography.secondary.copy(
                            fontWeight =
                                FontWeight.Black
                        ),
                    textAlign =
                        TextAlign.Center,
                    maxLines = 1
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text =
                        if (totalItems > 0) {
                            "$totalItems תרגילים בתרגול"
                        } else {
                            "תרגול לפי הנושא שנבחר"
                        },
                    modifier =
                        Modifier.fillMaxWidth(),
                    color =
                        kmiSectionHeaderContentColor()
                            .copy(alpha = 0.92f),
                    style =
                        KmiTypography.caption.copy(
                            fontWeight =
                                FontWeight.SemiBold
                        ),
                    textAlign =
                        TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        if (currentItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment =
                    Alignment.Center
            ) {
                Surface(
                    shape =
                        RoundedCornerShape(22.dp),
                    color =
                        MaterialTheme
                            .colorScheme
                            .surface
                            .copy(alpha = 0.95f),
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .outlineVariant
                        ),
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text =
                            "לא נמצאו תרגילים לתרגול",
                        modifier =
                            Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 20.dp
                            ),
                        style =
                            KmiTypography.sectionTitle,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        textAlign =
                            TextAlign.Center
                    )
                }
            }

            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                ),
            verticalArrangement =
                Arrangement.Top,
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(26.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .surface
                        .copy(alpha = 0.96f),
                border =
                    BorderStroke(
                        width = 1.5.dp,
                        color =
                            currentItem
                                .belt
                                .color
                                .copy(alpha = 0.55f)
                    ),
                tonalElevation = 0.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 22.dp,
                            vertical = 18.dp
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    Image(
                        painter =
                            painterResource(
                                id =
                                    practiceBeltImageRes(
                                        currentItem.belt
                                    )
                            ),
                        contentDescription =
                            currentItem.belt.heb,
                        modifier =
                            Modifier
                                .width(130.dp)
                                .height(52.dp),
                        contentScale =
                            ContentScale.Fit
                    )

                    Text(
                        text =
                            currentItem.belt.heb,
                        style =
                            KmiTypography.secondary.copy(
                                fontWeight =
                                    FontWeight.Black
                            ),
                        color =
                            currentItem.belt.color,
                        textAlign =
                            TextAlign.Center
                    )

                    Text(
                        text =
                            currentItem.topic,
                        style =
                            KmiTypography.secondary.copy(
                                fontWeight =
                                    FontWeight.Bold
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        textAlign =
                            TextAlign.Center
                    )

                    currentItem.subTopic
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let { subTopic ->
                            Text(
                                text = subTopic,
                                style =
                                    KmiTypography.caption.copy(
                                        fontWeight =
                                            FontWeight.SemiBold
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                textAlign =
                                    TextAlign.Center
                            )
                        }

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            currentItem.title,
                        modifier =
                            Modifier.fillMaxWidth(),
                        style =
                            KmiTypography.screenTitle.copy(
                                fontWeight =
                                    FontWeight.Black
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        textAlign =
                            TextAlign.Center
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Surface(
                        shape =
                            RoundedCornerShape(999.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                                .copy(alpha = 0.60f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text =
                                "${currentIndex + 1} מתוך $totalItems",
                            modifier =
                                Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 4.dp
                                ),
                            style =
                                KmiTypography.caption.copy(
                                    fontWeight =
                                        FontWeight.Bold
                                ),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            PracticeBottomControls(
                isEnglish = false,
                showSkip =
                    currentIndex <
                            practiceItems.lastIndex,
                onHelp = {
                    showHelp = true
                },
                onSkip = {
                    if (
                        currentIndex <
                        practiceItems.lastIndex
                    ) {
                        currentIndex++
                    }
                },
                onFinish = {
                    onBack()
                }
            )
        }
    }

    if (
        showHelp &&
        currentItem != null
    ) {
        val explanation =
            remember(currentItem) {
                findPracticeByTopicsExplanation(
                    currentItem
                )
            }

        ExerciseExplanationDialog(
            title =
                currentItem.title,
            beltLabel =
                "(${currentItem.belt.heb})",
            explanation =
                explanation,
            noteText = "",
            isFavorite = false,
            accentColor =
                currentItem.belt.color,
            isEnglish = false,
            onDismiss = {
                showHelp = false
            },
            onEditNote = {},
            onDeleteNote = {},
            onToggleFavorite = {}
        )
    }
}

private fun practiceBeltImageRes(
    belt: Belt
): Int =
    when (belt) {
        Belt.YELLOW -> R.drawable.intro_belt_yellow
        Belt.ORANGE -> R.drawable.intro_belt_orange
        Belt.GREEN -> R.drawable.intro_belt_green
        Belt.BLUE -> R.drawable.intro_belt_blue
        Belt.BROWN -> R.drawable.intro_belt_brown
        Belt.BLACK -> R.drawable.intro_belt_black
        else -> R.drawable.intro_belt_yellow
    }