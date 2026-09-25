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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBackground

@Composable
fun StretchingScreen(
    isEnglish: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenCategory: (StretchingCategory) -> Unit
) {
    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val categories =
        remember {
            StretchingCategory.entries
                .sortedBy { category ->
                    category.sortOrder
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
                        if (isEnglish) {
                            "Stretching Exercises"
                        } else {
                            "תרגילי מתיחות"
                        },
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
                    showRoleStatus = false,
                    showRoleBadge = false,
                    showModePill = false,
                    showCoachBroadcastFab = false,
                    titleMaxLines = 1,
                    titleScale = 0.92f
                )
            }
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                kmiScreenBackgroundBrush()
                        )
                        .padding(innerPadding)
            ) {
                StretchingIntroductionCard(
                    isEnglish = isEnglish
                )

                LazyVerticalGrid(
                    columns =
                        GridCells.Adaptive(
                            minSize = 154.dp
                        ),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 14.dp,
                            top = 12.dp,
                            end = 14.dp,
                            bottom = 28.dp
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = categories,
                        key = { category ->
                            category.id
                        }
                    ) { category ->
                        val exerciseCount =
                            StretchingCatalog
                                .exerciseCountFor(category)

                        StretchingCategoryCard(
                            category = category,
                            exerciseCount = exerciseCount,
                            isEnglish = isEnglish,
                            onClick = {
                                if (exerciseCount > 0) {
                                    onOpenCategory(category)
                                }
                            }
                        )
                    }

                    item(
                        span = {
                            androidx.compose.foundation.lazy.grid
                                .GridItemSpan(maxLineSpan)
                        }
                    ) {
                        StretchingSafetyCard(
                            isEnglish = isEnglish
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StretchingIntroductionCard(
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
                    "Choose a body area · Perform gently and at your own pace"
                } else {
                    "בחרו אזור בגוף · מבצעים בעדינות ובקצב אישי"
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
private fun StretchingCategoryCard(
    category: StretchingCategory,
    exerciseCount: Int,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val isAvailable = exerciseCount > 0

    val imageResource =
        stretchingCategoryImageResource(
            category = category
        )

    val accentColor =
        when (category) {
            StretchingCategory.NECK_AND_HEAD ->
                MaterialTheme.colorScheme.primary

            StretchingCategory.SHOULDERS_AND_ARMS ->
                MaterialTheme.colorScheme.secondary

            StretchingCategory.UPPER_BACK ->
                MaterialTheme.colorScheme.tertiary

            StretchingCategory.LOWER_BACK ->
                MaterialTheme.colorScheme.primary

            StretchingCategory.HIPS_AND_GROIN ->
                MaterialTheme.colorScheme.secondary

            StretchingCategory.LEGS ->
                MaterialTheme.colorScheme.tertiary

            StretchingCategory.KNEES_AND_ANKLES ->
                MaterialTheme.colorScheme.primary

            StretchingCategory.FULL_BODY ->
                MaterialTheme.colorScheme.secondary
        }

    val cardColor =
        if (isAvailable) {
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.96f)
        } else {
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(alpha = 0.82f)
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = isAvailable,
                    onClick = onClick
                ),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        border =
            BorderStroke(
                width =
                    if (isAvailable) {
                        1.5.dp
                    } else {
                        1.dp
                    },
                color =
                    if (isAvailable) {
                        accentColor.copy(alpha = 0.62f)
                    } else {
                        MaterialTheme
                            .colorScheme
                            .outlineVariant
                            .copy(alpha = 0.62f)
                    }
            ),
        shadowElevation = 0.dp,
        tonalElevation =
            if (isAvailable) {
                2.dp
            } else {
                0.dp
            }
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(118.dp),
                shape = RoundedCornerShape(16.dp),
                color =
                    if (isAvailable) {
                        accentColor.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    },
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            if (isAvailable) {
                                accentColor.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .outlineVariant
                            }
                    ),
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageResource != null) {
                        Image(
                            painter =
                                painterResource(
                                    id = imageResource
                                ),
                            contentDescription =
                                category.displayTitle(
                                    isEnglish = isEnglish
                                ),
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(3.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text =
                                categoryShortLabel(
                                    category = category,
                                    isEnglish = isEnglish
                                ),
                            style = KmiTypography.sectionTitle,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                                    .copy(alpha = 0.58f),
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    category.displayTitle(
                        isEnglish = isEnglish
                    ),
                modifier = Modifier.fillMaxWidth(),
                style = KmiTypography.body,
                color =
                    if (isAvailable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.66f)
                    },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Surface(
                shape = RoundedCornerShape(50),
                color =
                    if (isAvailable) {
                        accentColor.copy(alpha = 0.13f)
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    },
                shadowElevation = 0.dp
            ) {
                Text(
                    text =
                        if (isAvailable) {
                            if (isEnglish) {
                                "$exerciseCount exercises"
                            } else {
                                "$exerciseCount תרגילים"
                            }
                        } else {
                            if (isEnglish) {
                                "Coming soon"
                            } else {
                                "בקרוב"
                            }
                        },
                    modifier =
                        Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        ),
                    style = KmiTypography.caption,
                    color =
                        if (isAvailable) {
                            accentColor
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                                .copy(alpha = 0.7f)
                        },
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StretchingSafetyCard(
    isEnglish: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color =
            MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.9f),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                        .copy(alpha = 0.74f)
            ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .kmiSectionHeaderBackground()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 7.dp
                        )
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            "Safe practice"
                        } else {
                            "תרגול בטוח"
                        },
                    modifier = Modifier.fillMaxWidth(),
                    style = KmiTypography.action,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Start
                        } else {
                            TextAlign.End
                        }
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(9.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    shadowElevation = 0.dp
                ) {}

                Text(
                    text =
                        if (isEnglish) {
                            "Move gently and stop if you feel sharp pain, dizziness, numbness or unusual discomfort."
                        } else {
                            "יש לבצע את התנועות בעדינות ולעצור במקרה של כאב חד, סחרחורת, נימול או תחושה חריגה."
                        },
                    modifier = Modifier.weight(1f),
                    style = KmiTypography.caption,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Start
                        } else {
                            TextAlign.End
                        }
                )
            }
        }
    }
}

private fun categoryShortLabel(
    category: StretchingCategory,
    isEnglish: Boolean
): String =
    when (category) {
        StretchingCategory.NECK_AND_HEAD ->
            if (isEnglish) "NH" else "צ"

        StretchingCategory.SHOULDERS_AND_ARMS ->
            if (isEnglish) "SA" else "כ"

        StretchingCategory.UPPER_BACK ->
            if (isEnglish) "UB" else "ע"

        StretchingCategory.LOWER_BACK ->
            if (isEnglish) "LB" else "ת"

        StretchingCategory.HIPS_AND_GROIN ->
            if (isEnglish) "HG" else "א"

        StretchingCategory.LEGS ->
            if (isEnglish) "L" else "ר"

        StretchingCategory.KNEES_AND_ANKLES ->
            if (isEnglish) "KA" else "ב"

        StretchingCategory.FULL_BODY ->
            if (isEnglish) "FB" else "ג"
    }

private fun stretchingCategoryImageResource(
    category: StretchingCategory
): Int? =
    when (category) {
        StretchingCategory.NECK_AND_HEAD ->
            R.drawable.stretching_category_neck_head

        StretchingCategory.SHOULDERS_AND_ARMS ->
            R.drawable.stretching_category_shoulders_arms

        StretchingCategory.UPPER_BACK ->
            R.drawable.stretching_category_upper_back

        StretchingCategory.LOWER_BACK ->
            R.drawable.stretching_category_lower_back

        StretchingCategory.HIPS_AND_GROIN,
        StretchingCategory.LEGS,
        StretchingCategory.KNEES_AND_ANKLES,
        StretchingCategory.FULL_BODY ->
            null
    }
