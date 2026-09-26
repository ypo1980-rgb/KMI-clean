package il.kmi.app.stretching.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.R
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.stretching.StretchingCatalog
import il.kmi.shared.stretching.StretchingCategory
import il.kmi.shared.stretching.StretchingExercise
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBackground

@Composable
fun StretchingCategoryScreen(
    category: StretchingCategory,
    isEnglish: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenExercise: (String) -> Unit
) {
    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val exercises =
        remember(category) {
            StretchingCatalog
                .exercisesFor(category)
                .sortedBy { exercise ->
                    exercise.sortOrder
                }
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                KmiTopBar(
                    title =
                        category.displayTitle(
                            isEnglish = isEnglish
                        ),
                    onBack = onBack,
                    onHome = onHome,
                    currentLang =
                        if (isEnglish) {
                            "en"
                        } else {
                            "he"
                        },
                    showMenu = true,
                    showTopSearch = false,
                    showTopShare = false,
                    showBottomHelp = false,
                    showBottomShare = false,
                    showRoleStatus = true,
                    showRoleBadge = true,
                    showModePill = true,
                    showCoachBroadcastFab = false,
                    titleMaxLines = 1,
                    titleScale = 0.9f
                )
            }
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                kmiScreenBackgroundBrush()
                        )
                        .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    StretchingCategoryHeader(
                        exerciseCount = exercises.size,
                        isEnglish = isEnglish
                    )

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                        contentPadding =
                            PaddingValues(
                                start = 14.dp,
                                top = 10.dp,
                                end = 14.dp,
                                bottom = 24.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        if (exercises.isEmpty()) {
                            item {
                                EmptyStretchingCategoryCard(
                                    isEnglish = isEnglish
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = exercises,
                                key = { _, exercise ->
                                    exercise.id
                                }
                            ) { index, exercise ->
                                StretchingExerciseListCard(
                                    exercise = exercise,
                                    exerciseNumber = index + 1,
                                    isEnglish = isEnglish,
                                    onClick = {
                                        onOpenExercise(
                                            exercise.id
                                        )
                                    }
                                )
                            }
                        }

                        item {
                            StretchingCategorySafetyNotice(
                                isEnglish = isEnglish
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StretchingCategoryHeader(
    exerciseCount: Int,
    isEnglish: Boolean
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .kmiSectionHeaderBackground()
                .padding(
                    horizontal = 14.dp,
                    vertical = 9.dp
                ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text =
                if (isEnglish) {
                    "$exerciseCount exercises\nPerform gently and at your own pace"
                } else {
                    "$exerciseCount תרגילים\nמבצעים בעדינות ובקצב אישי"
                },
            modifier = Modifier.fillMaxWidth(),
            style = KmiTypography.action,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun StretchingExerciseListCard(
    exercise: StretchingExercise,
    exerciseNumber: Int,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val title =
        if (isEnglish) {
            exercise.titleEn
        } else {
            exercise.titleHe
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.96f),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                        .copy(alpha = 0.78f)
            ),
        shadowElevation = 0.dp,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 11.dp
                    ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageResource =
                stretchingImageResource(
                    imageKey = exercise.imageKey
                )

            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(16.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                        .copy(alpha = 0.42f),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(alpha = 0.3f)
                    ),
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (imageResource != null) {
                        Image(
                            painter =
                                painterResource(
                                    id = imageResource
                                ),
                            contentDescription = title,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(3.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Surface(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(25.dp),
                        shape = CircleShape,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                        .copy(alpha = 0.42f)
                            ),
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = exerciseNumber.toString(),
                                style = KmiTypography.caption,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimaryContainer,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(11.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = KmiTypography.body,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Left
                        } else {
                            TextAlign.Right
                        },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExerciseDetailBadge(
                        text =
                            durationLabel(
                                seconds = exercise.durationSeconds,
                                isEnglish = isEnglish
                            )
                    )

                    exercise.repetitions?.let { repetitions ->
                        ExerciseDetailBadge(
                            text =
                                if (isEnglish) {
                                    "$repetitions reps"
                                } else {
                                    "$repetitions חזרות"
                                }
                        )
                    }

                    if (exercise.performBothSides) {
                        ExerciseDetailBadge(
                            text =
                                if (isEnglish) {
                                    "Both sides"
                                } else {
                                    "שני הצדדים"
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailBadge(
    text: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(alpha = 0.84f),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                        .copy(alpha = 0.7f)
            ),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier =
                Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 3.dp
                ),
            style = KmiTypography.caption,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyStretchingCategoryCard(
    isEnglish: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.92f),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
            ),
        shadowElevation = 0.dp
    ) {
        Text(
            text =
                if (isEnglish) {
                    "Exercises for this category will be added soon."
                } else {
                    "תרגילים לקטגוריה זו יתווספו בקרוב."
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
            style = KmiTypography.body,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StretchingCategorySafetyNotice(
    isEnglish: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(alpha = 0.88f),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                        .copy(alpha = 0.78f)
            ),
        shadowElevation = 0.dp
    ) {
        Text(
            text =
                if (isEnglish) {
                    "Stop if you feel sharp pain, dizziness, numbness or unusual discomfort."
                } else {
                    "יש לעצור במקרה של כאב חד, סחרחורת, נימול או תחושה חריגה."
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    ),
            style = KmiTypography.caption,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun durationLabel(
    seconds: Int,
    isEnglish: Boolean
): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    return when {
        minutes > 0 && remainingSeconds > 0 -> {
            if (isEnglish) {
                "${minutes}m ${remainingSeconds}s"
            } else {
                "$minutes דק׳ $remainingSeconds שנ׳"
            }
        }

        minutes > 0 -> {
            if (isEnglish) {
                "${minutes}m"
            } else {
                "$minutes דק׳"
            }
        }

        else -> {
            if (isEnglish) {
                "${remainingSeconds}s"
            } else {
                "$remainingSeconds שנ׳"
            }
        }
    }
}

internal fun stretchingImageResource(
    imageKey: String
): Int? =
    when (imageKey) {
        "neck_chin_tuck" ->
            R.drawable.stretching_neck_chin_tuck_start

        "neck_chin_tuck_start" ->
            R.drawable.stretching_neck_chin_tuck_start

        "neck_chin_tuck_move" ->
            R.drawable.stretching_neck_chin_tuck_move

        "neck_chin_tuck_hold" ->
            R.drawable.stretching_neck_chin_tuck_hold

        "neck_forward_flexion_start" ->
            R.drawable.stretching_neck_forward_flexion_start

        "neck_forward_flexion" ->
            R.drawable.stretching_neck_forward_flexion

        "neck_rotation" ->
            R.drawable.stretching_neck_rotation_start

        "neck_rotation_start" ->
            R.drawable.stretching_neck_rotation_start

        "neck_rotation_right" ->
            R.drawable.stretching_neck_rotation_right

        "neck_rotation_left" ->
            R.drawable.stretching_neck_rotation_left

        "neck_side_flexion" ->
            R.drawable.stretching_neck_side_flexion_start

        "neck_side_flexion_start" ->
            R.drawable.stretching_neck_side_flexion_start

        "neck_side_flexion_left" ->
            R.drawable.stretching_neck_side_flexion_left

        "neck_side_flexion_right" ->
            R.drawable.stretching_neck_side_flexion_right

        "upper_trapezius_stretch" ->
            R.drawable.stretching_neck_upper_trapezius

        "levator_scapulae_stretch" ->
            R.drawable.stretching_neck_levator_scapulae_start_left

        "neck_levator_scapulae_start_left" ->
            R.drawable.stretching_neck_levator_scapulae_start_left

        "neck_levator_scapulae_left" ->
            R.drawable.stretching_neck_levator_scapulae_left

        "neck_levator_scapulae_start_right" ->
            R.drawable.stretching_neck_levator_scapulae_start_left

        "neck_levator_scapulae_right" ->
            R.drawable.stretching_neck_levator_scapulae_right

        "shoulder_rolls" ->
            R.drawable.stretching_shoulders_rolls_start

        "shoulder_rolls_start" ->
            R.drawable.stretching_shoulders_rolls_start

        "shoulder_rolls_up" ->
            R.drawable.stretching_shoulders_rolls_up

        "shoulder_rolls_back" ->
            R.drawable.stretching_shoulders_rolls_back

        "scapular_retraction" ->
            R.drawable.stretching_shoulders_scapular_retraction_start

        "scapular_retraction_start" ->
            R.drawable.stretching_shoulders_scapular_retraction_start

        "scapular_retraction_squeeze" ->
            R.drawable.stretching_shoulders_scapular_retraction_squeeze

        "seated_row_without_equipment" ->
            R.drawable.stretching_upper_back_seated_row_start

        "upper_back_seated_row_start" ->
            R.drawable.stretching_upper_back_seated_row_start

        "upper_back_seated_row_pull" ->
            R.drawable.stretching_upper_back_seated_row_pull

        "seated_upper_body_rotation" ->
            R.drawable.stretching_upper_body_seated_rotation_start

        "upper_body_seated_rotation_start" ->
            R.drawable.stretching_upper_body_seated_rotation_start

        "upper_body_seated_rotation_left" ->
            R.drawable.stretching_upper_body_seated_rotation_left

        "upper_body_seated_rotation_right" ->
            R.drawable.stretching_upper_body_seated_rotation_right

        "stretch_shoulders_cross_body" ->
            R.drawable.stretching_shoulders_cross_body_start

        "shoulders_cross_body_start" ->
            R.drawable.stretching_shoulders_cross_body_start

        "shoulders_cross_body_right" ->
            R.drawable.stretching_shoulders_cross_body_right

        "shoulders_cross_body_left" ->
            R.drawable.stretching_shoulders_cross_body_left

        "stretch_shoulders_overhead_triceps" ->
            R.drawable.stretching_shoulders_cross_body_start

        "shoulders_overhead_triceps_right" ->
            R.drawable.stretching_shoulders_overhead_triceps_right

        "shoulders_overhead_triceps_left" ->
            R.drawable.stretching_shoulders_overhead_triceps_left

        "stretch_shoulders_doorway_chest" ->
            R.drawable.stretch_shoulders_doorway_chest

        "stretch_shoulders_pendulum" ->
            R.drawable.stretch_shoulders_pendulum

        "stretch_shoulders_wall_walk" ->
            R.drawable.stretch_shoulders_wall_walk

        "stretch_shoulders_arm_circles" ->
            R.drawable.stretch_shoulders_arm_circles

        "stretch_arms_biceps_wall" ->
            R.drawable.stretching_shoulders_doorway_chest_start

        "shoulders_doorway_chest_start" ->
            R.drawable.stretching_shoulders_doorway_chest_start

        "shoulders_doorway_chest_stretch" ->
            R.drawable.stretching_shoulders_doorway_chest_stretch

        "stretch_arms_wrist_flexor" ->
            R.drawable.stretch_arms_wrist_flexor

        "stretch_arms_wrist_extensor" ->
            R.drawable.stretch_arms_wrist_extensor

        "stretch_arms_forearm_rotation" ->
            R.drawable.stretch_arms_forearm_rotation

        else ->
            null
    }