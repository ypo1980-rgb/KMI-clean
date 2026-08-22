package il.kmi.app.screens.registration

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.R
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.graphics.createBitmap
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

//======================================================================

private fun whiteToTransparent(src: Bitmap, tolerance: Int = 245): Bitmap {
    val w = src.width
    val h = src.height
    val pixels = IntArray(w * h)

    src.getPixels(
        pixels,
        0,
        w,
        0,
        0,
        w,
        h
    )

    for (i in pixels.indices) {
        val c = pixels[i]
        val r = android.graphics.Color.red(c)
        val g = android.graphics.Color.green(c)
        val b = android.graphics.Color.blue(c)
        if (r >= tolerance && g >= tolerance && b >= tolerance) {
            pixels[i] = android.graphics.Color.TRANSPARENT
        }
    }
    return createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
        it.setPixels(pixels, 0, w, 0, 0, w, h)
    }
}

@Composable
private fun PremiumShineButton(
    text: String,
    onClick: () -> Unit
) {
    val shine = rememberInfiniteTransition(label = "shine")
    val shineOffset by shine.animateFloat(
        initialValue = -220f,
        targetValue = 420f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineOffset"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFF171717)
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF5F7FA)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.00f),
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.00f),
                                Color.Transparent
                            ),
                            start = Offset(shineOffset, 0f),
                            end = Offset(shineOffset + 180f, 220f)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(1.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            )

            Text(
                text = text,
                style =
                    KmiTypography.action.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                color = Color(0xFF171717),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RegistrationLandingLockedTopBar(
    isEnglish: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            if (isDarkMode) {
                colorScheme.surface
            } else {
                Color.White.copy(alpha = 0.96f)
            },
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(38.dp))

                Text(
                    text = if (isEnglish) "Sign In / Register" else "מסך כניסה / רישום",
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        KmiTypography.screenTitle.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color =
                                if (isDarkMode) {
                                    colorScheme.onSurface
                                } else {
                                    Color(0xFF111827)
                                }
                        )
                )

                Spacer(Modifier.width(38.dp))
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun RegistrationLandingScreen(
    onNewUserTrainee: () -> Unit,
    onExistingUserTrainee: () -> Unit,
    onNewUserCoach: () -> Unit,
    onExistingUserCoach: () -> Unit,
    onOpenDrawer: () -> Unit,
    showTopBar: Boolean,
    sp: SharedPreferences,
    onGoHome: () -> Unit,
    autoSkipIfLoggedIn: Boolean,
    onOpenLegal: () -> Unit = {},
    onOpenTerms: () -> Unit = onOpenLegal
) {
    val ctx = LocalContext.current
    val resources = LocalResources.current
    val currentView by rememberUpdatedState(LocalView.current)

    val contextLang = LocalContext.current
    val langManager =
        remember(contextLang) {
            AppLanguageManager(contextLang)
        }

    val isEnglish =
        langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    // 🔊+📳 קריאת ההעדפות מההגדרות (kmi_settings)
    val settingsSp =
        ctx.getSharedPreferences(
            "kmi_settings",
            Context.MODE_PRIVATE
        )

    val clickEnabled by remember {
        mutableStateOf(
            settingsSp.getBoolean(
                "click_sounds",
                settingsSp.getBoolean("tap_sound", false)
            )
        )
    }
    val hapticEnabled by remember {
        mutableStateOf(
            settingsSp.getBoolean(
                "haptics_on",
                settingsSp.getBoolean("short_haptic", false)
            )
        )
    }

    fun playStrongFeedback() {
        if (clickEnabled) {
            currentView.playSoundEffect(
                SoundEffectConstants.CLICK
            )
        }

        if (hapticEnabled) {
            currentView.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS
            )
        }
    }

    // דילוג אוטומטי אם כבר מחוברים
    LaunchedEffect(
        autoSkipIfLoggedIn,
        sp
    ) {
        if (!autoSkipIfLoggedIn) {
            return@LaunchedEffect
        }

        val logged =
            sp.getBoolean(
                "is_logged_in",
                false
            )

        val hasUser =
            !sp.getString(
                "username",
                ""
            ).isNullOrBlank()

        val hasPass =
            !sp.getString(
                "password",
                ""
            ).isNullOrBlank()

        if (logged && hasUser && hasPass) {
            onGoHome()
        }
    }

    // משתמש קיים נכנס תמיד למסך התחברות רגיל.
    // מצב מאמן מאושר רק לאחר אימות במסך ההתחברות.
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = colorScheme.background.luminance() < 0.5f

    val screenLayoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val bgBrush =
        remember(isDarkMode) {
            Brush.verticalGradient(
                colors =
                    if (isDarkMode) {
                        listOf(
                            Color(0xFF030B14),
                            Color(0xFF061827),
                            Color(0xFF0A2940),
                            Color(0xFF0B3654),
                            Color(0xFF041522)
                        )
                    } else {
                        listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFEAF4FF),
                            Color(0xFFB7DDF7),
                            Color(0xFF1F78B4),
                            Color(0xFF062B4A)
                        )
                    }
            )
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides screenLayoutDirection
    ) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    RegistrationLandingLockedTopBar(
                        isEnglish = isEnglish
                    )
                }
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0)
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush)
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                // חגורה דקורטיבית באלכסון – למעלה בצד
                val blackBeltBitmap =
                    remember(resources) {
                        runCatching {
                            android.graphics.BitmapFactory.decodeResource(
                                resources,
                                R.drawable.intro_belt_black
                            )
                                ?.copy(Bitmap.Config.ARGB_8888, true)
                                ?.let { bitmap ->
                                    whiteToTransparent(
                                        bitmap,
                                        tolerance = 238
                                    )
                                }
                        }.getOrNull()
                    }

                if (blackBeltBitmap != null) {
                    Image(
                        bitmap = blackBeltBitmap.asImageBitmap(),
                        contentDescription =
                            if (isEnglish) {
                                "Black belt"
                            } else {
                                "חגורה שחורה"
                            },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp)
                            .fillMaxWidth(0.42f)
                            .height(64.dp)
                            .rotate(-18f)
                            .alpha(0.96f),
                        contentScale = ContentScale.Fit
                    )
                }

                // תוכן ראשי
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Spacer(Modifier.height(44.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF38D39F).copy(alpha = 0.30f),
                                            Color(0xFF1FAF85).copy(alpha = 0.16f),
                                            Color.Transparent
                                        ),
                                        radius = 420f
                                    ),
                                    shape = RoundedCornerShape(30.dp)
                                )
                                .alpha(0.95f)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(450)) +
                                        scaleIn(
                                            initialScale = 0.92f,
                                            animationSpec = tween(450)
                                        )
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(30.dp),
                                    color = Color(0xFF102A44).copy(alpha = 0.72f),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isEnglish) "Welcome to K.A.M.I" else "ברוכים הבאים ל־K.A.M.I",
                                            style =
                                                KmiTypography.sectionTitle.copy(
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        Text(
                                            text = if (isEnglish) "Choose how you want to continue" else "בחרו איך תרצו להמשיך",
                                            style = KmiTypography.secondary,
                                            color = Color.White.copy(alpha = 0.78f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(Modifier.height(16.dp))

                                        PremiumShineButton(
                                            text = if (isEnglish) "New User" else "משתמש חדש",
                                            onClick = {
                                                playStrongFeedback()
                                                onNewUserTrainee()
                                            }
                                        )

                                        Spacer(Modifier.height(14.dp))

                                        Button(
                                            onClick = {
                                                playStrongFeedback()

                                                // תמיד נכנסים למסך התחברות רגיל.
                                                // מצב מאמן מאושר רק אחרי בדיקת הרשאה מול Firestore.
                                                onExistingUserTrainee()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 56.dp),
                                            shape = RoundedCornerShape(22.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                                0.dp
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(
                                                defaultElevation = 0.dp,
                                                pressedElevation = 0.dp
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                Color(0xFF6673E8),
                                                                Color(0xFF4B57C8),
                                                                Color(0xFF3946A8)
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .background(
                                                            Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color.White.copy(alpha = 0.16f),
                                                                    Color.Transparent,
                                                                    Color.Transparent
                                                                ),
                                                                start = Offset(0f, 0f),
                                                                end = Offset(0f, 220f)
                                                            )
                                                        )
                                                )

                                                Text(
                                                    text = if (isEnglish) "Existing User" else "משתמש קיים",
                                                    style =
                                                        KmiTypography.action.copy(
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                    color = Color.White,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // תחתית
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val kamiBitmap =
                            remember(resources) {
                                runCatching {
                                    android.graphics.BitmapFactory.decodeResource(
                                        resources,
                                        R.drawable.kami_logo
                                    )
                                        ?.copy(Bitmap.Config.ARGB_8888, true)
                                        ?.let { bitmap ->
                                            whiteToTransparent(
                                                bitmap,
                                                tolerance = 245
                                            )
                                        }
                                }.getOrNull()
                            }

                        if (kamiBitmap != null) {
                            val windowWidthPx =
                                LocalWindowInfo.current.containerSize.width

                            val compactWidthPx =
                                with(LocalDensity.current) {
                                    360.dp.roundToPx()
                                }

                            val logoSize =
                                if (windowWidthPx <= compactWidthPx) {
                                    88.dp
                                } else {
                                    100.dp
                                }

                            Box(modifier = Modifier.size(logoSize + 10.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .size(logoSize + 10.dp)
                                        .align(Alignment.Center)
                                ) {}

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.97f),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .size(logoSize)
                                        .align(Alignment.Center)
                                ) {}

                                Image(
                                    bitmap = kamiBitmap.asImageBitmap(),
                                    contentDescription =
                                        if (isEnglish) {
                                            "K.A.M.I logo"
                                        } else {
                                            "לוגו ק.מ.י"
                                        },
                                    modifier = Modifier
                                        .size(logoSize)
                                        .align(Alignment.Center)
                                        .padding(10.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(
                                color =
                                    if (isDarkMode) {
                                        Color.White.copy(alpha = 0.32f)
                                    } else {
                                        Color(0xFF0B1020).copy(alpha = 0.24f)
                                    }
                            )
                            Spacer(Modifier.height(10.dp))

                            Text(
                                text =
                                    if (isEnglish) {
                                        "❤️ Developed with love by Yuval Polak ❤️"
                                    } else {
                                        "❤️ פותח באהבה ע\"י יובל פולק ❤️"
                                    },
                                style =
                                    KmiTypography.caption.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                color =
                                    if (isDarkMode) {
                                        Color.White.copy(alpha = 0.88f)
                                    } else {
                                        Color(0xFF0B1020)
                                    },
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}