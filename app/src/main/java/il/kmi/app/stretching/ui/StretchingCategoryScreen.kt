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

        "shoulders_pendulum_start" ->
            R.drawable.stretching_shoulders_pendulum_start

        "shoulders_pendulum_forward" ->
            R.drawable.stretching_shoulders_pendulum_forward

        "shoulders_pendulum_backward" ->
            R.drawable.stretching_shoulders_pendulum_backward

        "shoulders_wall_walk_start" ->
            R.drawable.stretching_shoulders_wall_walk_start

        "shoulders_wall_walk_middle" ->
            R.drawable.stretching_shoulders_wall_walk_middle

        "shoulders_wall_walk_top" ->
            R.drawable.stretching_shoulders_wall_walk_top

        "shoulders_arm_circles_center" ->
            R.drawable.stretching_shoulders_arm_circles_center

        "shoulders_arm_circles_up" ->
            R.drawable.stretching_shoulders_arm_circles_up

        "shoulders_arm_circles_down" ->
            R.drawable.stretching_shoulders_arm_circles_down

        "arms_biceps_wall_left" ->
            R.drawable.stretching_arms_biceps_wall_left

        "arms_biceps_wall_right" ->
            R.drawable.stretching_arms_biceps_wall_right

        "shoulders_doorway_chest_start" ->
            R.drawable.stretching_shoulders_doorway_chest_start

        "shoulders_doorway_chest_stretch" ->
            R.drawable.stretching_shoulders_doorway_chest_stretch

        "arms_wrist_flexor_right" ->
            R.drawable.stretching_arms_wrist_flexor_right

        "arms_wrist_flexor_left" ->
            R.drawable.stretching_arms_wrist_flexor_left

        "arms_wrist_extensor_right" ->
            R.drawable.stretching_arms_wrist_extensor_right

        "arms_wrist_extensor_left" ->
            R.drawable.stretching_arms_wrist_extensor_left

        "arms_forearm_rotation_up" ->
            R.drawable.stretching_arms_forearm_rotation_up

        "arms_forearm_rotation_neutral" ->
            R.drawable.stretching_arms_forearm_rotation_neutral

        "arms_forearm_rotation_down" ->
            R.drawable.stretching_arms_forearm_rotation_down

        "upper_back_forward_reach_start" ->
            R.drawable.stretching_upper_back_forward_reach_start

        "upper_back_forward_reach_stretch" ->
            R.drawable.stretching_upper_back_forward_reach_stretch

        "upper_back_self_hug_start" ->
            R.drawable.stretching_upper_back_self_hug_start

        "upper_back_self_hug_hold" ->
            R.drawable.stretching_upper_back_self_hug_hold

        "upper_back_chair_extension_start" ->
            R.drawable.stretching_upper_back_chair_extension_start

        "upper_back_chair_extension_stretch" ->
            R.drawable.stretching_upper_back_chair_extension_stretch

        // גב עליון — מתיחת גב עליון בהושטת ידיים
        "upper_back_forward_reach_start" ->
            R.drawable.stretching_upper_back_forward_reach_start

        "upper_back_forward_reach_stretch" ->
            R.drawable.stretching_upper_back_forward_reach_stretch

        // גב עליון — חיבוק עצמי לגב העליון
        "upper_back_self_hug_start" ->
            R.drawable.stretching_upper_back_self_hug_start

        "upper_back_self_hug_hold" ->
            R.drawable.stretching_upper_back_self_hug_hold

        // גב עליון — פשיטת גב עליון על כיסא
        "upper_back_chair_extension_start" ->
            R.drawable.stretching_upper_back_chair_extension_start

        "upper_back_chair_extension_stretch" ->
            R.drawable.stretching_upper_back_chair_extension_stretch

        // גב עליון — סיבוב גב עליון בישיבה
        "upper_back_seated_rotation_start" ->
            R.drawable.stretching_upper_back_seated_rotation_start

        "upper_back_seated_rotation_left" ->
            R.drawable.stretching_upper_back_seated_rotation_left

        "upper_back_seated_rotation_right" ->
            R.drawable.stretching_upper_back_seated_rotation_right

        // גב עליון — קימור ויישור הגב
        "upper_back_cat_cow_start" ->
            R.drawable.stretching_upper_back_cat_cow_start

        "upper_back_cat_cow_round" ->
            R.drawable.stretching_upper_back_cat_cow_round

        "upper_back_cat_cow_open" ->
            R.drawable.stretching_upper_back_cat_cow_open

        // גב עליון — השחלת יד מתחת לגוף
        "upper_back_thread_needle_start" ->
            R.drawable.stretching_upper_back_thread_needle_start

        "upper_back_thread_needle_left" ->
            R.drawable.stretching_upper_back_thread_needle_left

        "upper_back_thread_needle_right" ->
            R.drawable.stretching_upper_back_thread_needle_right

        // גב עליון — ישיבת עקבים עם ידיים לפנים
        "upper_back_child_pose_start" ->
            R.drawable.stretching_upper_back_child_pose_start

        "upper_back_child_pose_stretch" ->
            R.drawable.stretching_upper_back_child_pose_stretch

        // גב עליון — מתיחת גב עליון מול קיר
        "upper_back_wall_lat_start" ->
            R.drawable.stretching_upper_back_wall_lat_start

        "upper_back_wall_lat_stretch" ->
            R.drawable.stretching_upper_back_wall_lat_stretch

        // גב עליון — הושטת יד אלכסונית בישיבה
        "upper_back_side_reach_start" ->
            R.drawable.stretching_upper_back_side_reach_start

        "upper_back_side_reach_left" ->
            R.drawable.stretching_upper_back_side_reach_left

        "upper_back_side_reach_right" ->
            R.drawable.stretching_upper_back_side_reach_right

        // גב עליון — החלקת שכמות לפנים ולאחור
        "upper_back_scapular_glide_start" ->
            R.drawable.stretching_upper_back_scapular_glide_start

        "upper_back_scapular_glide_forward" ->
            R.drawable.stretching_upper_back_scapular_glide_forward

        "upper_back_scapular_glide_back" ->
            R.drawable.stretching_upper_back_scapular_glide_back

        "lower_back_pelvic_tilt_start" ->
            R.drawable.stretching_lower_back_pelvic_tilt_start

        "lower_back_pelvic_tilt_tilt" ->
            R.drawable.stretching_lower_back_pelvic_tilt_tilt

        "lower_back_single_knee_chest_start" ->
            R.drawable.stretching_lower_back_single_knee_chest_start

        "lower_back_single_knee_chest_left" ->
            R.drawable.stretching_lower_back_single_knee_chest_left

        "lower_back_single_knee_chest_right" ->
            R.drawable.stretching_lower_back_single_knee_chest_right

        "lower_back_double_knee_chest_start" ->
            R.drawable.stretching_lower_back_double_knee_chest_start

        "lower_back_double_knee_chest_hold" ->
            R.drawable.stretching_lower_back_double_knee_chest_hold

        "lower_back_knee_rolls_start" ->
            R.drawable.stretching_lower_back_knee_rolls_start

        "lower_back_knee_rolls_left" ->
            R.drawable.stretching_lower_back_knee_rolls_left

        "lower_back_knee_rolls_right" ->
            R.drawable.stretching_lower_back_knee_rolls_right

        "lower_back_cat_cow_start" ->
            R.drawable.stretching_lower_back_cat_cow_start

        "lower_back_cat_cow_round" ->
            R.drawable.stretching_lower_back_cat_cow_round

        "lower_back_cat_cow_open" ->
            R.drawable.stretching_lower_back_cat_cow_open

        "lower_back_child_pose_start" ->
            R.drawable.stretching_lower_back_child_pose_start

        "lower_back_child_pose_stretch" ->
            R.drawable.stretching_lower_back_child_pose_stretch

        "lower_back_sphinx_start" ->
            R.drawable.stretching_lower_back_sphinx_start

        "lower_back_sphinx_hold" ->
            R.drawable.stretching_lower_back_sphinx_hold

        "lower_back_seated_pelvic_rock_start" ->
            R.drawable.stretching_lower_back_seated_pelvic_rock_start

        "lower_back_seated_pelvic_rock_back" ->
            R.drawable.stretching_lower_back_seated_pelvic_rock_back

        "lower_back_seated_pelvic_rock_forward" ->
            R.drawable.stretching_lower_back_seated_pelvic_rock_forward

        "lower_back_supported_forward_start" ->
            R.drawable.stretching_lower_back_supported_forward_start

        "lower_back_supported_forward_stretch" ->
            R.drawable.stretching_lower_back_supported_forward_stretch

        "lower_back_standing_extension_start" ->
            R.drawable.stretching_lower_back_standing_extension_start

        "lower_back_standing_extension_back" ->
            R.drawable.stretching_lower_back_standing_extension_back

        "hips_groin_butterfly_start" ->
            R.drawable.stretching_hips_groin_butterfly_start

        "hips_groin_butterfly_stretch" ->
            R.drawable.stretching_hips_groin_butterfly_stretch

        "hips_groin_supine_figure_four_start" ->
            R.drawable.stretching_hips_groin_supine_figure_four_start

        "hips_groin_supine_figure_four_left" ->
            R.drawable.stretching_hips_groin_supine_figure_four_left

        "hips_groin_supine_figure_four_right" ->
            R.drawable.stretching_hips_groin_supine_figure_four_right

        "hips_groin_kneeling_hip_flexor_start" ->
            R.drawable.stretching_hips_groin_kneeling_hip_flexor_start

        "hips_groin_kneeling_hip_flexor_left" ->
            R.drawable.stretching_hips_groin_kneeling_hip_flexor_left

        "hips_groin_kneeling_hip_flexor_right" ->
            R.drawable.stretching_hips_groin_kneeling_hip_flexor_right

        "hips_groin_adductor_side_lunge_start" ->
            R.drawable.stretching_hips_groin_adductor_side_lunge_start

        "hips_groin_adductor_side_lunge_left" ->
            R.drawable.stretching_hips_groin_adductor_side_lunge_left

        "hips_groin_adductor_side_lunge_right" ->
            R.drawable.stretching_hips_groin_adductor_side_lunge_right

        "hips_groin_seated_wide_fold_start" ->
            R.drawable.stretching_hips_groin_seated_wide_fold_start

        "hips_groin_seated_wide_fold_stretch" ->
            R.drawable.stretching_hips_groin_seated_wide_fold_stretch

        "hips_groin_frog_rock_back_start" ->
            R.drawable.stretching_hips_groin_frog_rock_back_start

        "hips_groin_frog_rock_back_stretch" ->
            R.drawable.stretching_hips_groin_frog_rock_back_stretch

        "hips_groin_seated_figure_four_start" ->
            R.drawable.stretching_hips_groin_seated_figure_four_start

        "hips_groin_seated_figure_four_left" ->
            R.drawable.stretching_hips_groin_seated_figure_four_left

        "hips_groin_seated_figure_four_right" ->
            R.drawable.stretching_hips_groin_seated_figure_four_right

        "hips_groin_hip_rotations_start" ->
            R.drawable.stretching_hips_groin_hip_rotations_start

        "hips_groin_hip_rotations_left" ->
            R.drawable.stretching_hips_groin_hip_rotations_left

        "hips_groin_hip_rotations_right" ->
            R.drawable.stretching_hips_groin_hip_rotations_right

        "hips_groin_supine_adductor_start" ->
            R.drawable.stretching_hips_groin_supine_adductor_start

        "hips_groin_supine_adductor_stretch" ->
            R.drawable.stretching_hips_groin_supine_adductor_stretch

        "hips_groin_standing_hip_circles_start" ->
            R.drawable.stretching_hips_groin_standing_hip_circles_start

        "hips_groin_standing_hip_circles_left" ->
            R.drawable.stretching_hips_groin_standing_hip_circles_left

        "hips_groin_standing_hip_circles_right" ->
            R.drawable.stretching_hips_groin_standing_hip_circles_right

        "legs_standing_quadriceps_start" ->
            R.drawable.stretching_legs_standing_quadriceps_start

        "legs_standing_quadriceps_left" ->
            R.drawable.stretching_legs_standing_quadriceps_left

        "legs_standing_quadriceps_right" ->
            R.drawable.stretching_legs_standing_quadriceps_right

        "legs_seated_hamstring_start" ->
            R.drawable.stretching_legs_seated_hamstring_start

        "legs_seated_hamstring_left" ->
            R.drawable.stretching_legs_seated_hamstring_left

        "legs_seated_hamstring_right" ->
            R.drawable.stretching_legs_seated_hamstring_right

        "legs_standing_hamstring_start" ->
            R.drawable.stretching_legs_standing_hamstring_start

        "legs_standing_hamstring_left" ->
            R.drawable.stretching_legs_standing_hamstring_left

        "legs_standing_hamstring_right" ->
            R.drawable.stretching_legs_standing_hamstring_right

        "legs_wall_calf_start" ->
            R.drawable.stretching_legs_wall_calf_start

        "legs_wall_calf_left" ->
            R.drawable.stretching_legs_wall_calf_left

        "legs_wall_calf_right" ->
            R.drawable.stretching_legs_wall_calf_right

        "legs_wall_soleus_start" ->
            R.drawable.stretching_legs_wall_soleus_start

        "legs_wall_soleus_left" ->
            R.drawable.stretching_legs_wall_soleus_left

        "legs_wall_soleus_right" ->
            R.drawable.stretching_legs_wall_soleus_right

        "legs_standing_inner_thigh_start" ->
            R.drawable.stretching_legs_standing_inner_thigh_start

        "legs_standing_inner_thigh_left" ->
            R.drawable.stretching_legs_standing_inner_thigh_left

        "legs_standing_inner_thigh_right" ->
            R.drawable.stretching_legs_standing_inner_thigh_right


        "legs_crossed_outer_thigh_start" ->
            R.drawable.stretching_legs_crossed_outer_thigh_start

        "legs_crossed_outer_thigh_left" ->
            R.drawable.stretching_legs_crossed_outer_thigh_left

        "legs_crossed_outer_thigh_right" ->
            R.drawable.stretching_legs_crossed_outer_thigh_right

        "legs_supine_hamstring_start" ->
            R.drawable.stretching_legs_supine_hamstring_start

        "legs_supine_hamstring_left" ->
            R.drawable.stretching_legs_supine_hamstring_left

        "legs_supine_hamstring_right" ->
            R.drawable.stretching_legs_supine_hamstring_right

        "legs_prone_quadriceps_start" ->
            R.drawable.stretching_legs_prone_quadriceps_start

        "legs_prone_quadriceps_left" ->
            R.drawable.stretching_legs_prone_quadriceps_left

        "legs_prone_quadriceps_right" ->
            R.drawable.stretching_legs_prone_quadriceps_right

        "legs_controlled_leg_swings_start" ->
            R.drawable.stretching_legs_controlled_leg_swings_start

        "legs_controlled_leg_swings_left" ->
            R.drawable.stretching_legs_controlled_leg_swings_left

        "legs_controlled_leg_swings_right" ->
            R.drawable.stretching_legs_controlled_leg_swings_right

        "knees_ankles_knee_extension_start" ->
            R.drawable.stretching_knees_ankles_knee_extension_start

        "knees_ankles_knee_extension_left" ->
            R.drawable.stretching_knees_ankles_knee_extension_left

        "knees_ankles_knee_extension_right" ->
            R.drawable.stretching_knees_ankles_knee_extension_right

        "knees_ankles_knee_flexion_start" ->
            R.drawable.stretching_knees_ankles_knee_flexion_start

        "knees_ankles_knee_flexion_left" ->
            R.drawable.stretching_knees_ankles_knee_flexion_left

        "knees_ankles_knee_flexion_right" ->
            R.drawable.stretching_knees_ankles_knee_flexion_right

        "knees_ankles_heel_slide_start" ->
            R.drawable.stretching_knees_ankles_heel_slide_start

        "knees_ankles_heel_slide_left" ->
            R.drawable.stretching_knees_ankles_heel_slide_left

        "knees_ankles_heel_slide_right" ->
            R.drawable.stretching_knees_ankles_heel_slide_right

        "knees_ankles_standing_knee_flexion_start" ->
            R.drawable.stretching_knees_ankles_standing_knee_flexion_start

        "knees_ankles_standing_knee_flexion_left" ->
            R.drawable.stretching_knees_ankles_standing_knee_flexion_left

        "knees_ankles_standing_knee_flexion_right" ->
            R.drawable.stretching_knees_ankles_standing_knee_flexion_right

        "knees_ankles_mini_squat_start" ->
            R.drawable.stretching_knees_ankles_mini_squat_start

        "knees_ankles_mini_squat_down" ->
            R.drawable.stretching_knees_ankles_mini_squat_down

        "knees_ankles_mini_squat_up" ->
            R.drawable.stretching_knees_ankles_mini_squat_up

        "knees_ankles_ankle_circles_start" ->
            R.drawable.stretching_knees_ankles_ankle_circles_start

        "knees_ankles_ankle_circles_left" ->
            R.drawable.stretching_knees_ankles_ankle_circles_left

        "knees_ankles_ankle_circles_right" ->
            R.drawable.stretching_knees_ankles_ankle_circles_right

        "knees_ankles_ankle_pumps_start" ->
            R.drawable.stretching_knees_ankles_ankle_pumps_start

        "knees_ankles_ankle_pumps_flex" ->
            R.drawable.stretching_knees_ankles_ankle_pumps_flex

        "knees_ankles_ankle_pumps_point" ->
            R.drawable.stretching_knees_ankles_ankle_pumps_point

        "knees_ankles_wall_dorsiflexion_start" ->
            R.drawable.stretching_knees_ankles_wall_dorsiflexion_start

        "knees_ankles_wall_dorsiflexion_left" ->
            R.drawable.stretching_knees_ankles_wall_dorsiflexion_left

        "knees_ankles_wall_dorsiflexion_right" ->
            R.drawable.stretching_knees_ankles_wall_dorsiflexion_right

        "knees_ankles_heel_raises_start" ->
            R.drawable.stretching_knees_ankles_heel_raises_start

        "knees_ankles_heel_raises_up" ->
            R.drawable.stretching_knees_ankles_heel_raises_up

        "knees_ankles_heel_raises_down" ->
            R.drawable.stretching_knees_ankles_heel_raises_down

        "knees_ankles_toe_raises_start" ->
            R.drawable.stretching_knees_ankles_toe_raises_start

        "knees_ankles_toe_raises_up" ->
            R.drawable.stretching_knees_ankles_toe_raises_up

        "knees_ankles_toe_raises_down" ->
            R.drawable.stretching_knees_ankles_toe_raises_down

        "full_body_standing_reach_start" ->
            R.drawable.stretching_full_body_standing_reach_start

        "full_body_standing_reach_up" ->
            R.drawable.stretching_full_body_standing_reach_up

        "full_body_standing_reach_release" ->
            R.drawable.stretching_full_body_standing_reach_release

        "full_body_side_reach_start" ->
            R.drawable.stretching_full_body_side_reach_start

        "full_body_side_reach_left" ->
            R.drawable.stretching_full_body_side_reach_left

        "full_body_side_reach_right" ->
            R.drawable.stretching_full_body_side_reach_right

        "full_body_standing_rotation_start" ->
            R.drawable.stretching_full_body_standing_rotation_start

        "full_body_standing_rotation_left" ->
            R.drawable.stretching_full_body_standing_rotation_left

        "full_body_standing_rotation_right" ->
            R.drawable.stretching_full_body_standing_rotation_right

        "full_body_squat_reach_start" ->
            R.drawable.stretching_full_body_squat_reach_start

        "full_body_squat_reach_down" ->
            R.drawable.stretching_full_body_squat_reach_down

        "full_body_squat_reach_up" ->
            R.drawable.stretching_full_body_squat_reach_up

        "full_body_reverse_lunge_reach_start" ->
            R.drawable.stretching_full_body_reverse_lunge_reach_start

        "full_body_reverse_lunge_reach_left" ->
            R.drawable.stretching_full_body_reverse_lunge_reach_left

        "full_body_reverse_lunge_reach_right" ->
            R.drawable.stretching_full_body_reverse_lunge_reach_right

        "full_body_cat_cow_start" ->
            R.drawable.stretching_full_body_cat_cow_start

        "full_body_cat_cow_round" ->
            R.drawable.stretching_full_body_cat_cow_round

        "full_body_cat_cow_extend" ->
            R.drawable.stretching_full_body_cat_cow_extend

        "full_body_child_pose_start" ->
            R.drawable.stretching_full_body_child_pose_start

        "full_body_child_pose_reach" ->
            R.drawable.stretching_full_body_child_pose_reach

        "full_body_child_pose_return" ->
            R.drawable.stretching_full_body_child_pose_return

        "full_body_thread_needle_start" ->
            R.drawable.stretching_full_body_thread_needle_start

        "full_body_thread_needle_left" ->
            R.drawable.stretching_full_body_thread_needle_left

        "full_body_thread_needle_right" ->
            R.drawable.stretching_full_body_thread_needle_right

        "full_body_supine_lengthening_start" ->
            R.drawable.stretching_full_body_supine_lengthening_start

        "full_body_supine_lengthening_reach" ->
            R.drawable.stretching_full_body_supine_lengthening_reach

        "full_body_supine_lengthening_release" ->
            R.drawable.stretching_full_body_supine_lengthening_release

        "full_body_marching_reach_start" ->
            R.drawable.stretching_full_body_marching_reach_start

        "full_body_marching_reach_left" ->
            R.drawable.stretching_full_body_marching_reach_left

        "full_body_marching_reach_right" ->
            R.drawable.stretching_full_body_marching_reach_right

        else -> null
    }