package il.kmi.app.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.R
import il.kmi.app.ui.KmiTopBar

@Composable
fun OnboardingScreen(
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    steps: List<OnboardingStep> = OnboardingContent.steps,
    allowSkip: Boolean = true,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    if (steps.isEmpty()) {
        onFinish()
        return
    }

    var currentStepIndex by rememberSaveable {
        mutableIntStateOf(0)
    }

    val currentStep = steps[currentStepIndex]
    val isFirstStep = currentStepIndex == 0
    val isLastStep = currentStepIndex == steps.lastIndex

    val layoutDirection = if (isEnglish) {
        LayoutDirection.Ltr
    } else {
        LayoutDirection.Rtl
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0),
            containerColor = Color.Transparent,
            topBar = {
                KmiTopBar(
                    title = if (isEnglish) {
                        "App guide"
                    } else {
                        "הדרכת האפליקציה"
                    },
                    currentLang = if (isEnglish) "en" else "he",
                    showMenu = true,
                    showBottomActions = true,
                    showBottomHelp = true,
                    showBottomShare = true,
                    showSettings = true,
                    showRoleBadge = false,
                    showTopBeltIcon = false,
                    showLogoInBar = false,
                    showTopHome = false,
                    showTopSearch = false,
                    showTopShare = false,
                    showModePill = false,
                    showFontQuick = false,
                    showRoleStatus = false
                )
            },
            bottomBar = {
                OnboardingBottomBar(
                    isEnglish = isEnglish,
                    isFirstStep = isFirstStep,
                    isLastStep = isLastStep,
                    onPrevious = {
                        if (currentStepIndex > 0) {
                            currentStepIndex--
                        }
                    },
                    onNext = {
                        if (isLastStep) {
                            onFinish()
                        } else {
                            currentStepIndex++
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8FAFF),
                                currentStep.accentColor.copy(alpha = 0.12f),
                                Color(0xFFF4F0FF)
                            )
                        )
                    )
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 22.dp,
                            vertical = 8.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OnboardingNavigationHeader(
                        currentStepIndex = currentStepIndex,
                        stepsCount = steps.size,
                        accentColor = currentStep.accentColor,
                        isEnglish = isEnglish,
                        allowSkip = allowSkip,
                        onSkip = onSkip
                    )

                    Spacer(Modifier.height(4.dp))

                    AnimatedContent(
                        targetState = currentStepIndex,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                                ) + fadeIn() togetherWith
                                        slideOutOfContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.Start
                                        ) + fadeOut()
                            } else {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End
                                ) + fadeIn() togetherWith
                                        slideOutOfContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.End
                                        ) + fadeOut()
                            }
                        },
                        label = "onboardingStepTransition"
                    ) { stepIndex ->
                        val step = steps[stepIndex]

                        OnboardingStepCard(
                            step = step,
                            isEnglish = isEnglish
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingNavigationHeader(
    currentStepIndex: Int,
    stepsCount: Int,
    accentColor: Color,
    isEnglish: Boolean,
    allowSkip: Boolean,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (allowSkip) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .height(34.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 6.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    text = if (isEnglish) {
                        "Skip"
                    } else {
                        "דלג"
                    },
                    color = Color(0xFF6D4ED8),
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        OnboardingProgressIndicator(
            currentStepIndex = currentStepIndex,
            stepsCount = stepsCount,
            accentColor = accentColor,
            isEnglish = isEnglish,
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun OnboardingProgressIndicator(
    currentStepIndex: Int,
    stepsCount: Int,
    accentColor: Color,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(stepsCount) { index ->
                val isSelected = index == currentStepIndex
                val isCompleted = index < currentStepIndex

                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.5.dp)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .width(28.dp)
                                    .height(7.dp)
                            } else {
                                Modifier.size(7.dp)
                            }
                        )
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> accentColor
                                isCompleted -> accentColor.copy(alpha = 0.55f)
                                else -> Color(0xFFD8DEE9)
                            }
                        )
                )
            }
        }

        Text(
            text = if (isEnglish) {
                "Step ${currentStepIndex + 1} of $stepsCount"
            } else {
                "שלב ${currentStepIndex + 1} מתוך $stepsCount"
            },
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun onboardingImageForStep(
    step: OnboardingStep
): Int? {
    return when (step.id) {
        "welcome" ->
            R.drawable.onboarding_home

        "roles" ->
            R.drawable.onboarding_roles

        "belts" ->
            R.drawable.onboarding_belts

        "knowledge_status" ->
            R.drawable.onboarding_exercises

        "topics" ->
            R.drawable.onboarding_topics

        "internal_exam" ->
            R.drawable.onboarding_internal_exam

        "payments_report" ->
            R.drawable.onboarding_payments_report

        "pdf" ->
            R.drawable.onboarding_pdf

        "summary" ->
            R.drawable.onboarding_summary

        "ai" ->
            R.drawable.onboarding_personal_assistant

        "progress" ->
            R.drawable.onboarding_progress

        else ->
            step.imageRes
    }
}

@Composable
private fun OnboardingStepCard(
    step: OnboardingStep,
    isEnglish: Boolean
) {
    val imageRes = onboardingImageForStep(step)
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color.White.copy(alpha = 0.97f),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 10.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = step.title(isEnglish),
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF111827),
                fontSize = 14.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            if (imageRes != null) {
                Spacer(Modifier.height(5.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(316.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = step.title(isEnglish),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(164.dp)
                            .height(316.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                if (scrollState.canScrollForward) {
                    Spacer(Modifier.height(6.dp))

                    PremiumScrollHint(
                        step = step,
                        isEnglish = isEnglish
                    )

                    Spacer(Modifier.height(8.dp))
                } else {
                    Spacer(Modifier.height(6.dp))
                }
            }

            Text(
                text = step.description(isEnglish),
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF475569),
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                textAlign = if (isEnglish) {
                    TextAlign.Start
                } else {
                    TextAlign.Right
                }
            )
        }
    }
}

@Composable
private fun PremiumScrollHint(
    step: OnboardingStep,
    isEnglish: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(22.dp),
                clip = false
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.55f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            step.accentColor.copy(alpha = 0.96f),
                            Color(0xFF7C3AED),
                            Color(0xFF4F46E5)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 6.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.78f)
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = if (isEnglish) {
                        Alignment.End
                    } else {
                        Alignment.Start
                    },
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Continue to the full guide"
                        } else {
                            "המשך להסבר המלא"
                        },
                        color = Color.White,
                        fontSize = 12.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = if (isEnglish) {
                            TextAlign.Right
                        } else {
                            TextAlign.Left
                        }
                    )

                    Text(
                        text = if (isEnglish) {
                            "More information is waiting below"
                        } else {
                            "מידע נוסף מחכה לך למטה"
                        },
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 9.5.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = if (isEnglish) {
                            TextAlign.Right
                        } else {
                            TextAlign.Left
                        }
                    )
                }

                Spacer(Modifier.width(10.dp))

                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.28f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↓",
                            color = Color.White,
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    isEnglish: Boolean,
    isFirstStep: Boolean,
    isLastStep: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = 18.dp,
                    vertical = 7.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isFirstStep) {
                Button(
                    onClick = onPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(14.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFD7DDEA)
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF475569)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Previous"
                        } else {
                            "הקודם"
                        },
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1.2f)
                    .height(36.dp)
                    .shadow(
                        elevation = 7.dp,
                        shape = RoundedCornerShape(14.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.32f)
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6D4ED8),
                    contentColor = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    text = when {
                        isLastStep && isEnglish -> "Finish"
                        isLastStep -> "סיום"
                        isEnglish -> "Next"
                        else -> "הבא"
                    },
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}