package il.kmi.app.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import androidx.compose.animation.core.Easing
import android.view.animation.OvershootInterpolator
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import il.kmi.app.subscription.KmiAccess
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.Belt
import android.content.SharedPreferences
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import il.kmi.app.auth.GoogleAuthManager
import il.kmi.app.auth.UserProfileCompletion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import il.kmi.app.FcmTokenManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun overshootEasing(tension: Float = 2f): Easing =
    Easing { t -> OvershootInterpolator(tension).getInterpolation(t) }

private data class IntroRankDisplay(
    val id: String,
    val he: String,
    val en: String,
    val baseBelt: Belt,
    val color: Color
)

/** ✅ NEW: צבע חגורה (כדי לצייר בלי "רקע לבן" מהתמונה) */
private fun beltColor(belt: Belt): Color = when (belt) {
    Belt.WHITE  -> Color(0xFFF5F5F5)
    Belt.YELLOW -> Color(0xFFFFEB3B)
    Belt.ORANGE -> Color(0xFFFF9800)
    Belt.GREEN  -> Color(0xFF4CAF50)
    Belt.BLUE   -> Color(0xFF2196F3)
    Belt.BROWN  -> Color(0xFF6D4C41)
    Belt.BLACK  -> Color(0xFF111111)
}

private fun introRankFromId(rawId: String?): IntroRankDisplay? {
    return when (rawId?.trim().orEmpty()) {
        "white" -> IntroRankDisplay("white", "לבנה", "White belt", Belt.WHITE, beltColor(Belt.WHITE))
        "yellow" -> IntroRankDisplay("yellow", "צהובה", "Yellow belt", Belt.YELLOW, beltColor(Belt.YELLOW))
        "orange" -> IntroRankDisplay("orange", "כתומה", "Orange belt", Belt.ORANGE, beltColor(Belt.ORANGE))
        "green" -> IntroRankDisplay("green", "ירוקה", "Green belt", Belt.GREEN, beltColor(Belt.GREEN))
        "blue" -> IntroRankDisplay("blue", "כחולה", "Blue belt", Belt.BLUE, beltColor(Belt.BLUE))
        "brown" -> IntroRankDisplay("brown", "חומה", "Brown belt", Belt.BROWN, beltColor(Belt.BROWN))

        "black",
        "שחורה",
        "שחורה דאן 1" -> IntroRankDisplay("black", "שחורה דאן 1", "Black belt Dan 1", Belt.BLACK, beltColor(Belt.BLACK))

        "black_dan_2" -> IntroRankDisplay("black_dan_2", "שחורה דאן 2", "Black belt Dan 2", Belt.BLACK, beltColor(Belt.BLACK))
        "black_dan_3" -> IntroRankDisplay("black_dan_3", "שחורה דאן 3", "Black belt Dan 3", Belt.BLACK, beltColor(Belt.BLACK))
        "black_dan_4" -> IntroRankDisplay("black_dan_4", "שחורה דאן 4", "Black belt Dan 4", Belt.BLACK, beltColor(Belt.BLACK))
        "black_dan_5" -> IntroRankDisplay("black_dan_5", "שחורה דאן 5", "Black belt Dan 5", Belt.BLACK, beltColor(Belt.BLACK))
        "black_dan_6" -> IntroRankDisplay("black_dan_6", "שחורה דאן 6", "Black belt Dan 6", Belt.BLACK, beltColor(Belt.BLACK))
        "black_dan_7" -> IntroRankDisplay("black_dan_7", "שחורה דאן 7", "Black belt Dan 7", Belt.BLACK, beltColor(Belt.BLACK))
        "black_dan_8" -> IntroRankDisplay("black_dan_8", "שחורה דאן 8", "Black belt Dan 8", Belt.BLACK, beltColor(Belt.BLACK))
        "black_dan_9" -> IntroRankDisplay("black_dan_9", "שחורה דאן 9", "Black belt Dan 9", Belt.BLACK, beltColor(Belt.BLACK))
        "black_dan_10" -> IntroRankDisplay("black_dan_10", "שחורה דאן 10", "Black belt Dan 10", Belt.BLACK, beltColor(Belt.BLACK))

        else -> null
    }
}

// -------------------- prefs -> greeting + belt --------------------

private fun loadFirstName(sp: SharedPreferences): String? {
    val raw = listOf(
        sp.getString("fullName", null),
        sp.getString("user_name", null),
        sp.getString("name", null),
        sp.getString("displayName", null),
        sp.getString("firstName", null),
        sp.getString("first_name", null)
    ).firstOrNull { !it.isNullOrBlank() }

    val fromPrefs = raw
        ?.trim()
        ?.split(' ', limit = 2)
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    if (!fromPrefs.isNullOrBlank()) return fromPrefs

    // fallback: FirebaseAuth displayName (אם יש משתמש מחובר)
    val fbName = try {
        FirebaseAuth.getInstance().currentUser?.displayName
    } catch (_: Throwable) {
        null
    }

    return fbName
        ?.trim()
        ?.split(' ', limit = 2)
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

private fun loadBeltId(sp: SharedPreferences): String? {
    return listOf(
        sp.getString("current_belt", null),
        sp.getString("belt_current", null),
        sp.getString("currentBelt", null),
        sp.getString("beltId", null),
        sp.getString("belt_id", null),
        sp.getString("belt", null),
        sp.getString("belt_id_str", null)
    ).firstOrNull { !it.isNullOrBlank() }
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

@Composable
private fun rememberGreetingAndRank(
    userSp: SharedPreferences,
    lang: AppLanguage
): Pair<String, IntroRankDisplay?> {
    val firstName = remember { loadFirstName(userSp) }
    val beltId = remember { loadBeltId(userSp) }

    val rank = remember(beltId) { introRankFromId(beltId) }

    val greeting = remember(firstName, lang) {
        if (lang == AppLanguage.ENGLISH) {
            if (firstName.isNullOrBlank()) "Hello" else "Hello, $firstName"
        } else {
            if (firstName.isNullOrBlank()) "שלום" else "שלום $firstName"
        }
    }

    return greeting to rank
}

private suspend fun fetchAndPersistFullNameIfMissing(
    userSp: SharedPreferences
): String? {
    val existing = userSp.getString("fullName", null)?.trim()
    if (!existing.isNullOrBlank()) return existing

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return null

    // fallback מה-auth (לפני פיירסטור)
    val authName = auth.currentUser?.displayName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    if (!authName.isNullOrBlank()) {
        userSp.edit().putString("fullName", authName).apply()
        return authName
    }

    return try {
        val doc = FirebaseFirestore.getInstance()
            .collection("users")   // ⬅️ אם אצלך זה "trainees" / "profiles" עדכן כאן
            .document(uid)
            .get()
            .await()

        val fullName = (doc.getString("fullName")
            ?: doc.getString("full_name")
            ?: doc.getString("name")
            ?: doc.getString("displayName")
            ?: doc.getString("display_name"))
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (!fullName.isNullOrBlank()) {
            userSp.edit().putString("fullName", fullName).apply()
            fullName
        } else {
            null
        }
    } catch (_: Throwable) {
        null
    }
}

private fun googleLoginErrorMessage(
    error: Throwable,
    isEnglish: Boolean
): String {
    val clean = listOfNotNull(
        error.localizedMessage,
        error.message,
        error.toString()
    ).joinToString(" ")

    val isRealUserCancel =
        error is androidx.credentials.exceptions.GetCredentialCancellationException

    val isNoCredential =
        clean.contains("NoCredential", ignoreCase = true) ||
                clean.contains("No credentials", ignoreCase = true) ||
                clean.contains("credentials available", ignoreCase = true) ||
                clean.contains("no available credentials", ignoreCase = true)

    val isAccountCollision =
        clean.contains("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", ignoreCase = true) ||
                clean.contains("account exists with different credential", ignoreCase = true) ||
                clean.contains("already exists", ignoreCase = true) ||
                clean.contains("different credential", ignoreCase = true)

    val isConfigProblem =
        clean.contains("DEVELOPER_ERROR", ignoreCase = true) ||
                clean.contains("ApiException: 10", ignoreCase = true) ||
                clean.contains("invalid_audience", ignoreCase = true) ||
                clean.contains("audience", ignoreCase = true)

    val isNetworkProblem =
        clean.contains("network", ignoreCase = true) ||
                clean.contains("timeout", ignoreCase = true) ||
                clean.contains("unavailable", ignoreCase = true)

    if (isNoCredential) {
        return if (isEnglish) {
            "No Google account was found on this device. Please make sure a Google account is added to the device, update Google Play services, and try again."
        } else {
            "לא נמצא חשבון Google זמין במכשיר. יש לוודא שמוגדר חשבון Google במכשיר, לעדכן את שירותי Google Play Services ולנסות שוב."
        }
    }

    if (isRealUserCancel) {
        return if (isEnglish) {
            "Google sign-in was cancelled."
        } else {
            "ההתחברות עם Google בוטלה."
        }
    }

    if (isAccountCollision) {
        return if (isEnglish) {
            "This email is already registered with another sign-in method. Please sign in using the regular login method."
        } else {
            "האימייל הזה כבר רשום במערכת בדרך התחברות אחרת. יש להיכנס בדרך הרגילה."
        }
    }

    if (isConfigProblem) {
        return if (isEnglish) {
            "Google sign-in is not configured correctly for this app version. Please update the app and try again."
        } else {
            "התחברות Google אינה מוגדרת נכון לגרסה הזו. יש לעדכן את האפליקציה ולנסות שוב."
        }
    }

    if (isNetworkProblem) {
        return if (isEnglish) {
            "Network problem while signing in with Google. Please check your connection and try again."
        } else {
            "יש בעיית רשת בזמן התחברות עם Google. בדוק חיבור לאינטרנט ונסה שוב."
        }
    }

    return if (isEnglish) {
        "Google sign-in failed. Please try again."
    } else {
        "ההתחברות עם Google נכשלה. נסה שוב."
    }
}

/** ✅ REPLACE: חגורה מצוירת על הרקע (בלי תמונה עם לבן) */
@Composable
private fun BeltBadge(
    rank: IntroRankDisplay,
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    val belt = rank.baseBelt

    val beltTextColor = when (belt) {
        Belt.WHITE -> Color(0xFFE0E0E0)
        else -> rank.color
    }

    fun beltDrawableResOrNull(b: Belt): Int? = when (b) {
        Belt.WHITE  -> R.drawable.belt_white
        Belt.YELLOW -> R.drawable.belt_yellow
        Belt.ORANGE -> R.drawable.belt_orange
        Belt.GREEN  -> R.drawable.belt_green
        Belt.BLUE   -> R.drawable.belt_blue
        Belt.BROWN  -> R.drawable.belt_brown
        Belt.BLACK  -> R.drawable.belt_black
    }

    val res = beltDrawableResOrNull(belt)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (lang == AppLanguage.ENGLISH) rank.en else rank.he,
            color = beltTextColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        if (res != null) {
            Image(
                painter = painterResource(id = res),
                contentDescription = if (lang == AppLanguage.ENGLISH) rank.en else rank.he,
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = 1.55f
                        scaleY = 1.55f

                        compositingStrategy =
                            androidx.compose.ui.graphics.CompositingStrategy.Offscreen

                        if (belt == Belt.WHITE) {
                            shadowElevation = 6f
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            clip = false
                        }
                    },
                contentScale = ContentScale.Fit,
                colorFilter = if (belt == Belt.WHITE) null
                else androidx.compose.ui.graphics.ColorFilter.tint(
                    Color.White,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Modulate
                )
            )
        } else {
            Canvas(modifier = Modifier.size(width = 92.dp, height = 26.dp)) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = rank.color,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.45f, h * 0.45f)
                )
            }
        }
    }
}

private const val SUPPRESS_NEXT_DRAWER_OPEN_KEY = "kmi_suppress_next_drawer_open"

private suspend fun completeGoogleLoginAfterFirebaseAuth(
    ctx: Context,
    userSp: SharedPreferences,
    legacySp: SharedPreferences,
    onProfileComplete: () -> Unit,
    onProfileMissing: () -> Unit
) {
    GoogleAuthManager.logUiStage(
        context = ctx,
        stage = "intro_after_firebase_start_profile_check"
    )

    val profileStatus = runCatching {
        UserProfileCompletion.checkAndPersistProfileStatus(ctx)
    }.onFailure { error ->
        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_profile_check_failed_force_profile_completion",
            error = error
        )
    }.getOrElse {
        UserProfileCompletion.ProfileStatus(
            isComplete = false,
            missingFields = listOf("profile_check_failed")
        )
    }

    GoogleAuthManager.logUiStage(
        context = ctx,
        stage = "intro_profile_check_finished",
        message = "isComplete=${profileStatus.isComplete}, missingFields=${profileStatus.missingFields.joinToString("|")}"
    )

    userSp.edit()
        .putBoolean(SUPPRESS_NEXT_DRAWER_OPEN_KEY, true)
        .apply()

    legacySp.edit()
        .putBoolean(SUPPRESS_NEXT_DRAWER_OPEN_KEY, true)
        .apply()

    GoogleAuthManager.logUiStage(
        context = ctx,
        stage = "intro_navigation_decision",
        message = "profileComplete=${profileStatus.isComplete}"
    )

    if (profileStatus.isComplete) {
        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_call_on_profile_complete"
        )

        onProfileComplete()
    } else {
        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_call_on_profile_missing"
        )

        onProfileMissing()
    }

    runCatching {
        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_fcm_refresh_start_after_navigation"
        )

        FcmTokenManager.refreshTokenForCurrentUser(ctx)

        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_fcm_refresh_finished_after_navigation"
        )
    }.onFailure { error ->
        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_fcm_refresh_failed_non_blocking",
            error = error
        )
    }
}

@Composable
fun IntroScreen(
    onContinue: () -> Unit,
    onProfileComplete: () -> Unit = onContinue,
    onProfileMissing: () -> Unit = onContinue
) {
    var startAnim by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    // מונע הפעלה כפולה של Google Login בגלל לחיצה כפולה / recomposition
    var googleFlowLocked by remember { mutableStateOf(false) }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val currentLang = langManager.getCurrentLanguage()
    val isEnglish = currentLang == AppLanguage.ENGLISH

    val userSp = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }

    // הקובץ השני שבו נשמרים חלק מהדגלים הישנים של האפליקציה.
    val legacySp = remember { ctx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE) }

    val classicGoogleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            GoogleAuthManager.logUiStage(
                context = ctx,
                stage = "intro_classic_launcher_result_received",
                message = "resultCode=${result.resultCode}, dataNull=${result.data == null}"
            )

            val classicResult = GoogleAuthManager.handleClassicGoogleSignInResult(
                context = ctx,
                data = result.data
            )

            classicResult
                .onSuccess {
                    GoogleAuthManager.logUiStage(
                        context = ctx,
                        stage = "intro_classic_login_success_before_profile_check"
                    )

                    isGoogleLoading = false
                    googleFlowLocked = false

                    completeGoogleLoginAfterFirebaseAuth(
                        ctx = ctx,
                        userSp = userSp,
                        legacySp = legacySp,
                        onProfileComplete = onProfileComplete,
                        onProfileMissing = onProfileMissing
                    )
                }
                .onFailure { error ->
                    GoogleAuthManager.logUiStage(
                        context = ctx,
                        stage = "intro_classic_login_failure",
                        error = error
                    )

                    isGoogleLoading = false
                    googleFlowLocked = false

                    googleError = googleLoginErrorMessage(
                        error = error,
                        isEnglish = isEnglish
                    )

                    Toast.makeText(
                        ctx,
                        googleError.orEmpty(),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    var fetchedName by remember { mutableStateOf<String?>(null) }
    var didFetchName by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!didFetchName) {
            didFetchName = true
            fetchedName = fetchAndPersistFullNameIfMissing(userSp)
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "fadeIn"
    )

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.7f,
        animationSpec = tween(durationMillis = 2000, easing = overshootEasing(2f)),
        label = "scaleIn"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "gradientAnim")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        KmiAccess.ensureTrialStarted(userSp)
    }

    // ✅ FIX: משתמשים באותו userSp שממנו אתה מתחיל Trial ושבו נשמר המשתמש
    val (dynamicGreeting0, traineeRankOrNull) = rememberGreetingAndRank(
        userSp = userSp,
        lang = currentLang
    )

    val dynamicGreeting = remember(dynamicGreeting0, fetchedName, currentLang) {
        if (!fetchedName.isNullOrBlank()) {
            val first = fetchedName!!.trim().split(' ', limit = 2).first()
            if (currentLang == AppLanguage.ENGLISH) "Hello, $first" else "שלום $first"
        } else {
            dynamicGreeting0
        }
    }

    val accent = Color(0xFF16C47F)
    val bgTop = Color(0xFF071019)
    val bgBottom = Color(0xFF0E1A26)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bgTop,
                        bgBottom,
                        Color(0xFF101F2E)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            radius = 1200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEnglish) "KAMI" else "ק.מ.י",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.alpha(alpha).scale(scale)
                )
                Text(
                    text = if (isEnglish) "Israeli Krav Magen" else "קרב מגן ישראלי",
                    fontSize = if (isEnglish) 22.sp else 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    modifier = Modifier.alpha(alpha).scale(scale)
                )

                Text(
                    text = dynamicGreeting,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,      // מודגש
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .alpha(alpha)
                        .scale(scale)
                )

                if (traineeRankOrNull != null) {
                    Spacer(Modifier.height(2.dp))
                    BeltBadge(
                        rank = traineeRankOrNull,
                        lang = currentLang,
                        modifier = Modifier
                            .offset(y = 30.dp)
                            .alpha(alpha)
                            .scale(scale)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(232.dp)
                        .padding(top = 0.dp, bottom = 0.dp)
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    val imageScale = (scale * 1.84f).coerceAtMost(2.02f)

                    Image(
                        painter = painterResource(id = R.drawable.fighters_blackbelt),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .offset(y = 0.dp)
                            .graphicsLayer {
                                scaleX = imageScale
                                scaleY = imageScale
                                clip = false
                            },
                        contentScale = ContentScale.Fit
                    )
                }

                val buttonTransition = rememberInfiniteTransition(label = "introButtonAnim")
                val bubbleOffset by buttonTransition.animateFloat(
                    initialValue = -70f,
                    targetValue = 70f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "introBubbleOffset"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            GoogleAuthManager.logUiStage(
                                context = ctx,
                                stage = "intro_google_button_clicked",
                                message = "isGoogleLoading=$isGoogleLoading, googleFlowLocked=$googleFlowLocked"
                            )

                            if (isGoogleLoading || googleFlowLocked) {
                                GoogleAuthManager.logUiStage(
                                    context = ctx,
                                    stage = "intro_google_button_ignored_locked"
                                )
                                return@Button
                            }

                            googleError = null
                            isGoogleLoading = true
                            googleFlowLocked = true

                            scope.launch {
                                GoogleAuthManager.logUiStage(
                                    context = ctx,
                                    stage = "intro_credential_manager_flow_start"
                                )

                                val loginResult = GoogleAuthManager.signInWithGoogle(ctx)

                                loginResult
                                    .onSuccess {
                                        GoogleAuthManager.logUiStage(
                                            context = ctx,
                                            stage = "intro_credential_manager_login_success_before_profile_check"
                                        )

                                        isGoogleLoading = false
                                        googleFlowLocked = false

                                        completeGoogleLoginAfterFirebaseAuth(
                                            ctx = ctx,
                                            userSp = userSp,
                                            legacySp = legacySp,
                                            onProfileComplete = onProfileComplete,
                                            onProfileMissing = onProfileMissing
                                        )
                                    }
                                    .onFailure { error ->
                                        GoogleAuthManager.logUiStage(
                                            context = ctx,
                                            stage = "intro_credential_manager_login_failure",
                                            error = error
                                        )

                                        if (GoogleAuthManager.shouldUseClassicGoogleFallback(error)) {
                                            GoogleAuthManager.logUiStage(
                                                context = ctx,
                                                stage = "intro_classic_fallback_launch_start"
                                            )

                                            runCatching {
                                                classicGoogleLauncher.launch(
                                                    GoogleAuthManager.classicGoogleSignInIntent(ctx)
                                                )
                                            }.onFailure { launchError ->
                                                GoogleAuthManager.logUiStage(
                                                    context = ctx,
                                                    stage = "intro_classic_fallback_launch_failure",
                                                    error = launchError
                                                )

                                                isGoogleLoading = false
                                                googleFlowLocked = false

                                                googleError = googleLoginErrorMessage(
                                                    error = launchError,
                                                    isEnglish = isEnglish
                                                )

                                                Toast.makeText(
                                                    ctx,
                                                    googleError.orEmpty(),
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } else {
                                            GoogleAuthManager.logUiStage(
                                                context = ctx,
                                                stage = "intro_no_classic_fallback_show_error",
                                                error = error
                                            )

                                            isGoogleLoading = false
                                            googleFlowLocked = false

                                            googleError = googleLoginErrorMessage(
                                                error = error,
                                                isEnglish = isEnglish
                                            )

                                            Toast.makeText(
                                                ctx,
                                                googleError.orEmpty(),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            }
                        },
                        enabled = !isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 10.dp,
                            pressedElevation = 14.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF1E88E5),
                                            Color(0xFF5E35B1)
                                        )
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = bubbleOffset.dp)
                                    .size(140.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.42f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isGoogleLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(Modifier.width(8.dp))

                                        Text(
                                            text = if (isEnglish) {
                                                "Continue with Google"
                                            } else {
                                                "התחברות עם Google"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!googleError.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = googleError.orEmpty(),
                            color = Color(0xFFFFCDD2),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(0.dp))

                    TextButton(
                        onClick = {
                            // מונע פתיחת סרגל צד בטעות בזמן המעבר למסך כניסה / רישום
                            userSp.edit()
                                .putBoolean(SUPPRESS_NEXT_DRAWER_OPEN_KEY, true)
                                .apply()

                            legacySp.edit()
                                .putBoolean(SUPPRESS_NEXT_DRAWER_OPEN_KEY, true)
                                .apply()

                            onContinue()
                        },
                        enabled = !isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 34.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "Use existing login / sign up screen"
                            } else {
                                "כניסה / רישום בדרך הרגילה"
                            },
                            color = Color.White.copy(alpha = 0.88f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isEnglish) 13.sp else 15.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/** איור וקטורי עם הגבלה דינמית של סקייל כדי שלא ייחתך מהמסך */
@Composable
private fun FightersIllustration() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // זה הסקייל שתרצה "באידיאל" (היה 1.30 ואז הקטנו 2×5% ≈ 1.235)
        val desiredScale = 1.235f

        // שוליים מינימליים מסביב כדי שלא ייגע בגבולות (אפשר לכוונן)
        val pad = 24.dp.toPx()

        // סקייל מקסימלי שמבטיח שהאיור נשאר בתוך הקנבס + שוליים
        val maxScaleW = (w - 2 * pad) / w
        val maxScaleH = (h - 2 * pad) / h
        val safeScale = minOf(desiredScale, maxScaleW.coerceAtMost(1f), maxScaleH.coerceAtMost(1f))

        withTransform({
            // ממרכזים ומגדילים עד הגבול הבטוח
            scale(scaleX = safeScale, scaleY = safeScale, pivot = Offset(w / 2f, h / 2f))
        }) {
            // “שמש” מאחור
            drawOval(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(w * 0.15f, h * 0.05f),
                size = Size(w * 0.7f, h * 0.7f)
            )

            // סילואטות מינימליות
            val pathLeft = Path().apply {
                moveTo(w * 0.30f, h * 0.65f)
                cubicTo(w * 0.28f, h * 0.58f, w * 0.36f, h * 0.45f, w * 0.42f, h * 0.40f)
                cubicTo(w * 0.46f, h * 0.37f, w * 0.48f, h * 0.32f, w * 0.46f, h * 0.28f)
                cubicTo(w * 0.44f, h * 0.23f, w * 0.38f, h * 0.22f, w * 0.36f, h * 0.26f)
                cubicTo(w * 0.34f, h * 0.30f, w * 0.36f, h * 0.35f, w * 0.34f, h * 0.40f)
                cubicTo(w * 0.31f, h * 0.48f, w * 0.28f, h * 0.55f, w * 0.30f, h * 0.65f)
                close()
            }
            drawPath(pathLeft, color = Color.White.copy(alpha = 0.85f))

            val pathRight = Path().apply {
                moveTo(w * 0.70f, h * 0.70f)
                cubicTo(w * 0.72f, h * 0.60f, w * 0.68f, h * 0.50f, w * 0.62f, h * 0.45f)
                cubicTo(w * 0.58f, h * 0.42f, w * 0.56f, h * 0.36f, w * 0.58f, h * 0.32f)
                cubicTo(w * 0.60f, h * 0.28f, w * 0.66f, h * 0.28f, w * 0.68f, h * 0.32f)
                cubicTo(w * 0.70f, h * 0.36f, w * 0.68f, h * 0.40f, w * 0.70f, h * 0.46f)
                cubicTo(w * 0.73f, h * 0.54f, w * 0.76f, h * 0.60f, w * 0.70f, h * 0.70f)
                close()
            }
            drawPath(pathRight, color = Color.White.copy(alpha = 0.85f))
        }
    }
}
