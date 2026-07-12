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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
                OnboardingTopBar(
                    isEnglish = isEnglish,
                    allowSkip = allowSkip,
                    onSkip = onSkip
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
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OnboardingProgressIndicator(
                        currentStepIndex = currentStepIndex,
                        stepsCount = steps.size,
                        accentColor = currentStep.accentColor,
                        isEnglish = isEnglish
                    )

                    Spacer(Modifier.height(18.dp))

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
                            stepNumber = stepIndex + 1,
                            isEnglish = isEnglish
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgressIndicator(
    currentStepIndex: Int,
    stepsCount: Int,
    accentColor: Color,
    isEnglish: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(stepsCount) { index ->
                val isSelected = index == currentStepIndex
                val isCompleted = index < currentStepIndex

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(8.dp)
                        .then(
                            if (isSelected) {
                                Modifier.fillMaxWidth(0.10f)
                            } else {
                                Modifier.size(8.dp)
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
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OnboardingTopBar(
    isEnglish: Boolean,
    allowSkip: Boolean,
    onSkip: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEnglish) {
                    "App guide"
                } else {
                    "הדרכה על האפליקציה"
                },
                modifier = Modifier.weight(1f),
                color = Color(0xFF111827),
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = if (isEnglish) {
                    TextAlign.Start
                } else {
                    TextAlign.Right
                }
            )

            if (allowSkip) {
                TextButton(
                    onClick = onSkip
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Skip"
                        } else {
                            "דלג"
                        },
                        color = Color(0xFF6D4ED8),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun onboardingImageForStep(
    step: OnboardingStep
): Int? {
    return when (step.id) {
        "welcome" ->
            R.drawable.onboarding_welcome

        "belts" ->
            R.drawable.onboarding_subjects

        "drawer" ->
            R.drawable.onboarding_drawer

        "subjects" ->
            R.drawable.onboarding_subjects

        "categories" ->
            R.drawable.onboarding_categories

        "knowledge_status" ->
            R.drawable.onboarding_progress

        "exercise_cards" ->
            R.drawable.onboarding_categories

        "pdf" ->
            R.drawable.onboarding_pdf

        "tools" ->
            R.drawable.onboarding_toolbar

        "ai" ->
            R.drawable.onboarding_ai

        /*
         * עדיין אין drawable בשם onboarding_exam.
         * לאחר הוספת התמונה, החלף את null ב:
         *
         * R.drawable.onboarding_exam
         */
        "internal_exam" ->
            null

        else ->
            null
    }
}

@Composable
private fun OnboardingStepCard(
    step: OnboardingStep,
    stepNumber: Int,
    isEnglish: Boolean
) {
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
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 20.dp,
                    vertical = 22.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val imageRes = onboardingImageForStep(step)

            if (imageRes != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .width(214.dp)
                            .height(382.dp)
                            .shadow(
                                elevation = 22.dp,
                                shape = RoundedCornerShape(34.dp),
                                clip = false
                            ),
                        shape = RoundedCornerShape(34.dp),
                        color = Color(0xFF090D18),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = step.accentColor.copy(alpha = 0.38f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(7.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.Black)
                        ) {
                            Image(
                                painter = painterResource(imageRes),
                                contentDescription = step.title(isEnglish),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 6.dp)
                                    .width(68.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFF070A12))
                            )

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                                shape = CircleShape,
                                color = step.accentColor,
                                shadowElevation = 8.dp
                            ) {
                                Text(
                                    text = stepNumber.toString(),
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(126.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        step.accentColor.copy(alpha = 0.32f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Spacer(Modifier.height(20.dp))
            }

            Text(
                text = step.title(isEnglish),
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF111827),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

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
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isFirstStep) {
                TextButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Previous"
                        } else {
                            "הקודם"
                        },
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Bold
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
                    .weight(1.35f)
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6D4ED8),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = when {
                        isLastStep && isEnglish -> "Finish"
                        isLastStep -> "סיום"
                        isEnglish -> "Next"
                        else -> "הבא"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}