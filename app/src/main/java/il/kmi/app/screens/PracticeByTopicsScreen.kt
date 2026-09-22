package il.kmi.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import il.kmi.app.ui.practice.PracticeExerciseCenterCard
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
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import androidx.compose.material3.Scaffold
import il.yuval.ui.theme.kmiSectionHeaderBackground
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

    var timeLeft by
    rememberSaveable {
        mutableIntStateOf(
            durationMinutes * 60
        )
    }

    var isRunning by
    rememberSaveable {
        mutableStateOf(false)
    }

    var isMuted by
    rememberSaveable {
        mutableStateOf(false)
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

            timeLeft =
                durationSeconds

            isRunning = true

            showStartDialog = false
        }
    )

    val currentItem =
        practiceItems
            .getOrNull(currentIndex)

    val totalItems =
        practiceItems.size

    LaunchedEffect(
        currentIndex,
        durationMinutes
    ) {
        timeLeft =
            durationMinutes * 60
    }

    LaunchedEffect(
        isRunning,
        currentIndex,
        showStartDialog
    ) {
        if (
            !showStartDialog &&
            isRunning &&
            currentItem != null
        ) {
            while (
                timeLeft > 0 &&
                isRunning
            ) {
                delay(1000)
                timeLeft--
            }

            if (
                isRunning &&
                timeLeft == 0
            ) {
                if (
                    currentIndex <
                    practiceItems.lastIndex
                ) {
                    currentIndex++
                } else {
                    isRunning = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
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
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .kmiSectionHeaderBackground()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text =
                        if (totalItems > 0) {
                            "תרגול מכל החגורות\n$totalItems תרגילים בתרגול"
                        } else {
                            "תרגול מכל החגורות\nתרגול לפי הנושא שנבחר"
                        },
                    modifier = Modifier.fillMaxWidth(),
                    color = kmiSectionHeaderContentColor(),
                    style =
                        KmiTypography.secondary.copy(
                            fontWeight = FontWeight.Black
                        ),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }

            val sectionHeaderPadding = 56.dp

            if (currentItem == null) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = sectionHeaderPadding
                        ),
                    contentAlignment = Alignment.Center
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
                            textAlign = TextAlign.Center
                        )
                    }
                }

            } else {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = sectionHeaderPadding
                        )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 14.dp,
                                bottom = 150.dp
                            ),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        PracticeExerciseCenterCard(
                            belt =
                                currentItem.belt,
                            exerciseTitle =
                                currentItem.title,
                            exerciseSubtitle =
                                null,
                            timeText =
                                String.format(
                                    "%02d:%02d",
                                    timeLeft / 60,
                                    timeLeft % 60
                                ),
                            currentIndex =
                                currentIndex,
                            totalCount =
                                totalItems,
                            centerLabel =
                                null,
                            isRunning =
                                isRunning,
                            isMuted =
                                isMuted,
                            onToggleRunning = {
                                isRunning =
                                    !isRunning
                            },
                            onToggleMute = {
                                isMuted =
                                    !isMuted
                            },
                            onCardClick = {
                                showHelp = true
                            }
                        )
                    }

                    PracticeBottomControls(
                        isEnglish = false,
                        showSkip =
                            currentIndex <
                                    practiceItems.lastIndex,
                        modifier = Modifier
                            .align(
                                Alignment.BottomCenter
                            )
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 8.dp
                            ),
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
                            isRunning = false
                            onBack()
                        }
                    )
                }
            }
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