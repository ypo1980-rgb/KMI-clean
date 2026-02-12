@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.registration

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
                KmiTopBar(
                    title = "רישום משתמש",
                    showRoleStatus = false,
                    showBottomActions = true,
                    onOpenDrawer = { onOpenDrawer.invoke() },
                    extraActions = { androidx.compose.foundation.layout.Spacer(Modifier.width(48.dp)) },
                    showRoleBadge = false,
                    lockSearch = true,
                    showCoachBroadcastFab = false,
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
            // —— תוכן ראשי ——
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // באנר
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.nok_out_banner),
                        contentDescription = "לוגו נוק אאוט",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp),
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.Center
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                }

                // כפתורים
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val buttonsWidth = 0.94f

                    Button(
                        onClick = {
                            playStrongFeedback()
                            onNewUserTrainee()
                        }, // הטאבים לטיפול במסך הרישום
                        modifier = Modifier
                            .fillMaxWidth(buttonsWidth)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White, contentColor = Color.Black
                        )
                    ) { Text("משתמש חדש", style = MaterialTheme.typography.titleMedium) }

                    androidx.compose.foundation.layout.Spacer(Modifier.height(22.dp))

                    Button(
                        onClick = {
                            playStrongFeedback()
                            if (isCoach) onExistingUserCoach() else onExistingUserTrainee()
                        },
                        modifier = Modifier
                            .fillMaxWidth(buttonsWidth)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White, contentColor = Color.Black
                        )
                    ) { Text("משתמש קיים", style = MaterialTheme.typography.titleMedium) }
                }

                // —— תחתית —— (נשארת בתוך ה־Box/Scaffold)
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
                        // מרווח דינמי בין הכפתורים ללוגו
                        val cfg = LocalConfiguration.current
                        val topGap = when {
                            cfg.screenHeightDp <= 640 -> 8.dp
                            cfg.screenHeightDp <= 720 -> 14.dp
                            cfg.screenHeightDp <= 800 -> 20.dp
                            else -> 28.dp
                        }
                        val logoSize = if (cfg.screenWidthDp <= 360) 108.dp else 124.dp

                        androidx.compose.foundation.layout.Spacer(Modifier.height(topGap))

                        Box(modifier = Modifier.size(logoSize)) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = Color.White.copy(alpha = 0.96f),
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp,
                                modifier = Modifier.matchParentSize()
                            ) {}
                            Image(
                                bitmap = kamiBitmap.asImageBitmap(),
                                contentDescription = "KAMI",
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(12.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                        Divider()
                        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

                        val creditSize =
                            if (LocalConfiguration.current.screenWidthDp <= 360) 14.sp else 16.sp
                        Text(
                            text = "❤️ פותח באהבה ע\"י יובל פולק ❤️",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = creditSize),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
