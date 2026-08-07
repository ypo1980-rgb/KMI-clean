package il.kmi.app.ui.assistant.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.StyledExplanationText

@Composable
internal fun AssistantConversationHistory(
    messages: List<AiMessage>,
    isThinking: Boolean,
    isEnglish: Boolean,
    scrollState: ScrollState,
    conversationViewportHeightPx: Int,
    onConversationViewportHeightChanged: (Int) -> Unit,
    onLatestQuestionOffsetChanged: (Int) -> Unit,
    onLike: (Int) -> Unit,
    onUnlike: (
        index: Int,
        question: String,
        answer: String
    ) -> Unit
) {
    val density = LocalDensity.current

    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    val textAlignPrimary =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    Column(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                onConversationViewportHeightChanged(
                    coordinates.size.height
                )
            }
            .padding(
                horizontal = 12.dp,
                vertical = 12.dp
            )
            .verticalScroll(scrollState),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        val latestUserMessageIndex =
            messages.indexOfLast { message ->
                message.fromUser
            }

        messages.forEachIndexed { index, message ->
            val isLatestQuestion =
                message.fromUser &&
                        index == latestUserMessageIndex

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isLatestQuestion) {
                            Modifier.onGloballyPositioned {
                                    coordinates ->
                                onLatestQuestionOffsetChanged(
                                    coordinates
                                        .positionInParent()
                                        .y
                                        .toInt()
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment =
                    when {
                        message.fromUser && !isEnglish ->
                            Alignment.CenterEnd

                        message.fromUser && isEnglish ->
                            Alignment.CenterStart

                        !message.fromUser && !isEnglish ->
                            Alignment.CenterStart

                        else ->
                            Alignment.CenterEnd
                    }
            ) {
                val bubbleColor =
                    if (message.fromUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        androidx.compose.ui.graphics.Color(
                            0xFFF1EDF7
                        )
                    }

                val textColor =
                    if (message.fromUser) {
                        androidx.compose.ui.graphics.Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                Surface(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomEnd =
                            if (message.fromUser) {
                                2.dp
                            } else {
                                18.dp
                            },
                        bottomStart =
                            if (message.fromUser) {
                                18.dp
                            } else {
                                2.dp
                            }
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp
                ) {
                    Column {
                        if (
                            !message.fromUser &&
                            !message.answerTitle.isNullOrBlank()
                        ) {
                            StyledExplanationText(
                                raw = message.answerTitle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 14.dp,
                                        end = 14.dp,
                                        top = 12.dp,
                                        bottom = 2.dp
                                    ),
                                style =
                                    KmiTypography.cardTitle.copy(
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    ),
                                color =
                                    androidx.compose.ui.graphics.Color(
                                        0xFF4C3A80
                                    ),
                                textAlign = textAlignPrimary
                            )
                        }

                        if (
                            !message.fromUser &&
                            message.trainingItems.isNotEmpty()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 10.dp
                                    ),
                                verticalArrangement =
                                    Arrangement.spacedBy(9.dp)
                            ) {
                                Text(
                                    text =
                                        tr(
                                            "האימונים שמצאתי",
                                            "Trainings I found"
                                        ),
                                    color =
                                        androidx.compose.ui.graphics.Color(
                                            0xFF312E81
                                        ),
                                    style =
                                        KmiTypography.sectionTitle.copy(
                                            fontWeight =
                                                FontWeight.Black
                                        ),
                                    textAlign = textAlignPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                message.trainingItems.forEach { item ->
                                    AssistantTrainingCard(
                                        item = item,
                                        isEnglish = isEnglish
                                    )
                                }
                            }
                        } else if (message.fromUser) {
                            Text(
                                text = message.text,
                                color = textColor,
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 12.dp
                                ),
                                textAlign = textAlignPrimary,
                                style =
                                    MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            StyledExplanationText(
                                raw = message.text,
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 12.dp
                                ),
                                style =
                                    MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                textAlign = textAlignPrimary
                            )
                        }

                        if (!message.fromUser) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start =
                                            if (isEnglish) 4.dp else 0.dp,
                                        end =
                                            if (isEnglish) 0.dp else 4.dp,
                                        bottom = 4.dp
                                    ),
                                horizontalArrangement =
                                    if (isEnglish) {
                                        Arrangement.Start
                                    } else {
                                        Arrangement.End
                                    },
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        onLike(index)
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Filled.ThumbUp,
                                        contentDescription =
                                            tr(
                                                "אהבתי את התשובה",
                                                "Like answer"
                                            ),
                                        tint =
                                            when (message.feedback) {
                                                Feedback.LIKE ->
                                                    androidx.compose.ui.graphics.Color(
                                                        0xFF22C55E
                                                    )

                                                else ->
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant
                                            }
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val question =
                                            messages
                                                .take(index)
                                                .lastOrNull {
                                                    it.fromUser
                                                }
                                                ?.text
                                                ?.trim()
                                                .orEmpty()

                                        onUnlike(
                                            index,
                                            question,
                                            message.text
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Filled.ThumbDown,
                                        contentDescription =
                                            tr(
                                                "לא אהבתי את התשובה",
                                                "Dislike answer"
                                            ),
                                        tint =
                                            when (message.feedback) {
                                                Feedback.UNLIKE ->
                                                    androidx.compose.ui.graphics.Color(
                                                        0xFFEF4444
                                                    )

                                                else ->
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isThinking) {
            val dotsTransition =
                rememberInfiniteTransition(
                    label = "thinkingDots"
                )

            val dotAlpha by dotsTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(650),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotAlpha"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement =
                    if (isEnglish) {
                        Arrangement.Start
                    } else {
                        Arrangement.End
                    },
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        tr(
                            "יובל חושב",
                            "Yuval is thinking"
                        ),
                    style = KmiTypography.caption,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    textAlign = textAlignPrimary
                )

                Spacer(Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = dotAlpha
                            ),
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }

        if (conversationViewportHeightPx > 0) {
            Spacer(
                modifier = Modifier.height(
                    with(density) {
                        (
                                conversationViewportHeightPx *
                                        0.82f
                                ).toDp()
                    }
                )
            )
        }
    }
}

@Composable
internal fun AssistantSpeakingIndicator(
    isSpeaking: Boolean,
    assistantMode: AssistantMode?,
    isEnglish: Boolean
) {
    if (!isSpeaking || assistantMode != null) {
        return
    }

    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    val textAlignPrimary =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val eqTransition =
        rememberInfiniteTransition(label = "eq")

    val bars = listOf(
        eqTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    420,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar1"
        ),
        eqTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    520,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar2"
        ),
        eqTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    480,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar3"
        ),
        eqTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    560,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar4"
        )
    )

    Spacer(Modifier.height(4.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement =
            if (isEnglish) {
                Arrangement.Start
            } else {
                Arrangement.End
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tr("מדבר…", "Speaking…"),
            color = MaterialTheme.colorScheme.primary,
            style = KmiTypography.caption,
            textAlign = textAlignPrimary
        )

        Spacer(Modifier.width(10.dp))

        bars.forEachIndexed { index, animation ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(
                        (8 + animation.value * 16).dp
                    )
                    .background(
                        color =
                            MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )

            if (index < bars.lastIndex) {
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}