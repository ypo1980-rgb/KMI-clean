@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.registration

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.R
import il.kmi.app.ui.KmiTopBar
import android.app.Activity
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

//======================================================================

private fun whiteToTransparent(src: Bitmap, tolerance: Int = 245): Bitmap {
    val w = src.width; val h = src.height
    val pixels = IntArray(w * h); src.getPixels(pixels, 0, w, 0, 0, w, h)
    for (i in pixels.indices) {
        val c = pixels[i]
        val r = android.graphics.Color.red(c)
        val g = android.graphics.Color.green(c)
        val b = android.graphics.Color.blue(c)
        if (r >= tolerance && g >= tolerance && b >= tolerance) {
            pixels[i] = android.graphics.Color.TRANSPARENT
        }
    }
    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
        it.setPixels(pixels, 0, w, 0, 0, w, h)
    }
}

@Composable
private fun PremiumShineButton(
    text: String,
    isPrimaryLight: Boolean,
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
            .height(60.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false
            ),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimaryLight) Color.White else Color(0xFF4F56A8).copy(alpha = 0.92f),
            contentColor = if (isPrimaryLight) Color(0xFF171717) else Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp)),
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
                                Color.White.copy(alpha = 0.32f),
                                Color.White.copy(alpha = 0.00f),
                                Color.Transparent
                            ),
                            start = Offset(shineOffset, 0f),
                            end = Offset(shineOffset + 160f, 220f)
                        )
                    )
            )

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp,
                color = if (isPrimaryLight) Color(0xFF171717) else Color.White
            )
        }
    }
}

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
    onOpenLegal: () -> Unit = {},          // ← להוסיף
    onOpenTerms: () -> Unit = onOpenLegal  // ← להוסיף
) {
    val ctx = LocalContext.current
    val view = LocalView.current

    val contextLang = LocalContext.current
    val langManager = remember { AppLanguageManager(contextLang) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    // 🔊+📳 קריאת ההעדפות מההגדרות (kmi_settings)
    val settingsSp = remember {
        ctx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
    }
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
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
        if (hapticEnabled) {
            // רטט חזק יותר
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    // דילוג אוטומטי אם כבר מחוברים
    LaunchedEffect(autoSkipIfLoggedIn) {
        if (!autoSkipIfLoggedIn) return@LaunchedEffect
        val logged   = sp.getBoolean("is_logged_in", false)
        val hasUser  = !sp.getString("username", "").isNullOrBlank()
        val hasPass  = !sp.getString("password", "").isNullOrBlank()
        if (logged && hasUser && hasPass) onGoHome()
    }

    // תפקיד אחרון
    val isCoach by remember {
        mutableStateOf((sp.getString("user_role", null) ?: "trainee") == "coach")
    }

    val bgBrush = remember {
        Brush.linearGradient(
            listOf(Color(0xFF7F00FF), Color(0xFF3F51B5), Color(0xFF03A9F4)),
            start = Offset(0f, 0f), end = Offset(1000f, 3000f)
        )
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                val contextLang = LocalContext.current
                val langManager = remember { AppLanguageManager(contextLang) }

                KmiTopBar(
                    title = if (isEnglish) "Sign In / Register" else "מסך כניסה / רישום",
                    showRoleStatus = false,
                    showBottomActions = true,
                    onOpenDrawer = { onOpenDrawer.invoke() },
                    extraActions = { androidx.compose.foundation.layout.Spacer(Modifier.width(48.dp)) },
                    showRoleBadge = false,
                    lockSearch = true,
                    showCoachBroadcastFab = false,
                    currentLang = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",
                    onToggleLanguage = {
                        val newLang =
                            if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                                AppLanguage.ENGLISH
                            } else {
                                AppLanguage.HEBREW
                            }

                        langManager.setLanguage(newLang)
                        (contextLang as? Activity)?.recreate()
                    }
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
            val beltAlpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900),
                label = "beltFade"
            )

            // חגורה דקורטיבית באלכסון – למעלה בצד
            Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.belt_black),
                contentDescription = "חגורה שחורה",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp)
                    .fillMaxWidth(0.42f)
                    .height(64.dp)
                    .rotate(-18f)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(8.dp),
                        clip = false
                    )
                    .alpha(0.9f),
                contentScale = ContentScale.Fit
            )

            // תוכן ראשי
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(56.dp))

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
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 18.dp,
                                    shape = RoundedCornerShape(30.dp),
                                    clip = false
                                ),
                            shape = RoundedCornerShape(30.dp),
                            color = Color(0xFF1A1440).copy(alpha = 0.42f),
                            tonalElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isEnglish) "Welcome to KMI" else "ברוכים הבאים ל־KMI",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = if (isEnglish) "Choose how you want to continue" else "בחרו איך תרצו להמשיך",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.78f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(24.dp))

                                PremiumShineButton(
                                    text = if (isEnglish) "New User" else "משתמש חדש",
                                    isPrimaryLight = true,
                                    onClick = {
                                        playStrongFeedback()
                                        onNewUserTrainee()
                                    }
                                )

                                Spacer(Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        playStrongFeedback()
                                        if (isCoach) onExistingUserCoach() else onExistingUserTrainee()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .shadow(
                                            elevation = 12.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            clip = false
                                        ),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4F56A8).copy(alpha = 0.92f),
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp
                                    )
                                ) {
                                    Text(
                                        text = if (isEnglish) "Existing User" else "משתמש קיים",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.2.sp
                                    )
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
                    val kamiBitmap = runCatching {
                        android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.kami_logo)
                            ?.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                            ?.let { whiteToTransparent(it, tolerance = 245) }
                    }.getOrNull()

                    if (kamiBitmap != null) {
                        val cfg = LocalConfiguration.current
                        val logoSize = if (cfg.screenWidthDp <= 360) 88.dp else 100.dp

                        Box(modifier = Modifier.size(logoSize + 10.dp)) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .size(logoSize + 10.dp)
                                    .align(Alignment.Center)
                            ) {}

                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color.White.copy(alpha = 0.97f),
                                tonalElevation = 4.dp,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .size(logoSize)
                                    .align(Alignment.Center)
                            ) {}

                            Image(
                                bitmap = kamiBitmap.asImageBitmap(),
                                contentDescription = "KAMI",
                                modifier = Modifier
                                    .size(logoSize)
                                    .align(Alignment.Center)
                                    .padding(10.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Divider(color = Color.White.copy(alpha = 0.45f))
                        Spacer(Modifier.height(10.dp))

                        val creditSize =
                            if (LocalConfiguration.current.screenWidthDp <= 360) 14.sp else 16.sp

                        Text(
                            text = if (isEnglish)
                                "❤️ Developed with love by Yuval Polak ❤️"
                            else
                                "❤️ פותח באהבה ע\"י יובל פולק ❤️",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = creditSize),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B1020),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}