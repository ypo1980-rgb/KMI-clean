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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import com.google.firebase.auth.FirebaseAuth
import il.yuval.ui.theme.kmiSectionHeaderBrush

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
    onClick: () -> Unit,
    isPrimary: Boolean
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

    val shape = RoundedCornerShape(22.dp)

    val backgroundBrush =
        if (isPrimary) {
            Brush.linearGradient(
                listOf(
                    Color(0xFF6673E8),
                    Color(0xFF4B57C8),
                    Color(0xFF3946A8)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF5F7FA)
                )
            )
        }

    val contentColor =
        if (isPrimary) {
            Color.White
        } else {
            Color(0xFF171717)
        }

    val innerOverlayColor =
        if (isPrimary) {
            Color.White.copy(alpha = 0.07f)
        } else {
            Color.White.copy(alpha = 0.14f)
        }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(shape)
                .background(backgroundBrush),
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
                                Color.White.copy(alpha = if (isPrimary) 0.16f else 0.28f),
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
                    .background(innerOverlayColor)
            )

            Text(
                text = text,
                style =
                    KmiTypography.action.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                color = contentColor,
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
    KmiTopBar(
        title =
            if (isEnglish) {
                "Sign In / Register"
            } else {
                "מסך כניסה / רישום"
            },
        showTopHome = false,
        showTopSearch = false,
        showTopShare = false,
        showBottomActions = true,
        lockHome = true,
        lockSearch = true,
        centerTitle = true
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun RegistrationLandingScreen(
    onNewUserTrainee: () -> Unit,
    onExistingUserTrainee: () -> Unit,
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

    // דילוג אוטומטי כאשר המשתמש בחר
// "שמירה לכניסה הבאה" ויש Firebase session תקף.
    LaunchedEffect(
        autoSkipIfLoggedIn,
        sp
    ) {
        if (!autoSkipIfLoggedIn) {
            return@LaunchedEffect
        }

        val userPrefs =
            ctx.getSharedPreferences(
                "kmi_user",
                Context.MODE_PRIVATE
            )

        val rememberMe =
            sp.getBoolean(
                "remember_me_login",
                userPrefs.getBoolean(
                    "remember_me_login",
                    false
                )
            )

        val auth =
            FirebaseAuth.getInstance()

        val firebaseUser =
            auth.currentUser

        val hasValidSession =
            firebaseUser != null &&
                    !firebaseUser.isAnonymous

        if (rememberMe && hasValidSession) {

            sp.edit()
                .putBoolean(
                    "is_logged_in",
                    true
                )
                .apply()

            userPrefs.edit()
                .putBoolean(
                    "is_logged_in",
                    true
                )
                .apply()

            onGoHome()

        } else if (!rememberMe) {

            /*
             * המשתמש לא ביקש לשמור את הכניסה.
             * Firebase שומר session אוטומטית באנדרואיד,
             * ולכן מנתקים אותו כאשר חוזרים למסך הכניסה.
             */
            if (hasValidSession) {
                auth.signOut()
            }

            sp.edit()
                .putBoolean(
                    "is_logged_in",
                    false
                )
                .apply()

            userPrefs.edit()
                .putBoolean(
                    "is_logged_in",
                    false
                )
                .apply()
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
                    .background(
                        brush = kmiScreenBackgroundBrush()
                    )
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
                            .padding(
                                top = 6.dp,
                                end = 16.dp
                            )
                            .fillMaxWidth(0.34f)
                            .height(54.dp)
                            .rotate(-14f)
                            .alpha(0.94f),
                        contentScale = ContentScale.Fit
                    )
                }

                // תוכן ראשי
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        color = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = kmiSectionHeaderBrush()
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Welcome to K.A.M.I"
                                        } else {
                                            "ברוכים הבאים ל־K.A.M.I"
                                        },
                                    style =
                                        KmiTypography.action.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Choose how you want to continue"
                                        } else {
                                            "בחרו איך תרצו להמשיך"
                                        },
                                    style =
                                        KmiTypography.caption.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                    color = Color.White.copy(alpha = 0.92f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Spacer(Modifier.height(34.dp))

                        AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.fillMaxWidth(),
                        enter =
                            fadeIn(
                                animationSpec = tween(450)
                            ) +
                                    scaleIn(
                                        initialScale = 0.94f,
                                        animationSpec = tween(450)
                                    )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant
                                    .copy(alpha = 0.96f),
                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border =
                                BorderStroke(
                                    width = 1.dp,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .outlineVariant
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 18.dp
                                    ),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                Spacer(
                                    Modifier.height(4.dp)
                                )

                                PremiumShineButton(
                                    text =
                                        if (isEnglish) {
                                            "New User"
                                        } else {
                                            "משתמש חדש"
                                        },
                                    onClick = {
                                        playStrongFeedback()
                                        onNewUserTrainee()
                                    },
                                    isPrimary = false
                                )

                                Spacer(
                                    Modifier.height(12.dp)
                                )

                                PremiumShineButton(
                                    text =
                                        if (isEnglish) {
                                            "Existing User"
                                        } else {
                                            "משתמש קיים"
                                        },
                                    onClick = {
                                        playStrongFeedback()

                                        // תמיד נכנסים למסך התחברות רגיל.
                                        // מצב מאמן מאושר רק אחרי בדיקת הרשאה מול Firestore.
                                        onExistingUserTrainee()
                                    },
                                    isPrimary = true
                                )
                            }
                        }
                    }

                        Spacer(Modifier.height(20.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 8.dp
                            ),
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
                                    76.dp
                                } else {
                                    86.dp
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

                            Spacer(Modifier.height(8.dp))

                            HorizontalDivider(
                                color =
                                    if (isDarkMode) {
                                        Color.White.copy(alpha = 0.32f)
                                    } else {
                                        Color(0xFF0B1020).copy(alpha = 0.24f)
                                    }
                            )

                            Spacer(Modifier.height(8.dp))

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