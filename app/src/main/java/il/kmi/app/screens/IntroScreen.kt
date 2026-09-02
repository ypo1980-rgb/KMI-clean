package il.kmi.app.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import il.kmi.app.subscription.KmiAccess
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.Belt
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import il.kmi.app.auth.GoogleAuthManager
import il.kmi.app.auth.UserProfileCompletion
import il.kmi.app.ui.KmiTypography
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import il.kmi.app.FcmTokenManager
import il.kmi.app.ui.loading.KmiLoadingRings
import il.yuval.ui.theme.kmiBeltColor
import il.yuval.ui.theme.kmiGraniteActionBrush
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


//=======================================================================

private data class IntroRankDisplay(
    val id: String,
    val he: String,
    val en: String,
    val baseBelt: Belt,
    val color: Color
)

/**
 * מחזיר את תמונת החגורה המעוצבת למסך הפתיחה.
 */
private fun introBeltDrawableRes(
    rank: IntroRankDisplay
): Int {
    return when (
        rank.id
            .trim()
            .lowercase()
    ) {
        "black_dan_2" ->
            R.drawable.intro_belt_black_dan_2

        "black_dan_3" ->
            R.drawable.intro_belt_black_dan_3

        "black_dan_4" ->
            R.drawable.intro_belt_black_dan_4

        "black_dan_5" ->
            R.drawable.intro_belt_black_dan_5

        "black_dan_6",
        "black_dan_7",
        "black_dan_8" ->
            R.drawable.intro_belt_red_white_dan_6_7_8

        "black_dan_9",
        "black_dan_10" ->
            R.drawable.intro_belt_red_dan_9_10

        else -> {
            when (rank.baseBelt) {
                Belt.WHITE ->
                    R.drawable.intro_belt_white

                Belt.YELLOW ->
                    R.drawable.intro_belt_yellow

                Belt.ORANGE ->
                    R.drawable.intro_belt_orange

                Belt.GREEN ->
                    R.drawable.intro_belt_green

                Belt.BLUE ->
                    R.drawable.intro_belt_blue

                Belt.BROWN ->
                    R.drawable.intro_belt_brown

                Belt.BLACK ->
                    R.drawable.intro_belt_black
            }
        }
    }
}

private fun introRankFromId(
    rawId: String?
): IntroRankDisplay? {
    fun rank(
        id: String,
        he: String,
        en: String,
        belt: Belt
    ): IntroRankDisplay {
        return IntroRankDisplay(
            id = id,
            he = he,
            en = en,
            baseBelt = belt,
            color = kmiBeltColor(belt)
        )
    }

    return when (rawId?.trim().orEmpty()) {
        "white" ->
            rank("white", "לבנה", "White belt", Belt.WHITE)

        "yellow" ->
            rank("yellow", "צהובה", "Yellow belt", Belt.YELLOW)

        "orange" ->
            rank("orange", "כתומה", "Orange belt", Belt.ORANGE)

        "green" ->
            rank("green", "ירוקה", "Green belt", Belt.GREEN)

        "blue" ->
            rank("blue", "כחולה", "Blue belt", Belt.BLUE)

        "brown" ->
            rank("brown", "חומה", "Brown belt", Belt.BROWN)

        "black",
        "שחורה",
        "שחורה דאן 1" ->
            rank(
                "black",
                "שחורה דאן 1",
                "Black belt Dan 1",
                Belt.BLACK
            )

        "black_dan_2" ->
            rank("black_dan_2", "שחורה דאן 2", "Black belt Dan 2", Belt.BLACK)

        "black_dan_3" ->
            rank("black_dan_3", "שחורה דאן 3", "Black belt Dan 3", Belt.BLACK)

        "black_dan_4" ->
            rank("black_dan_4", "שחורה דאן 4", "Black belt Dan 4", Belt.BLACK)

        "black_dan_5" ->
            rank("black_dan_5", "שחורה דאן 5", "Black belt Dan 5", Belt.BLACK)

        "black_dan_6" ->
            rank("black_dan_6", "שחורה דאן 6", "Black belt Dan 6", Belt.BLACK)

        "black_dan_7" ->
            rank("black_dan_7", "שחורה דאן 7", "Black belt Dan 7", Belt.BLACK)

        "black_dan_8" ->
            rank("black_dan_8", "שחורה דאן 8", "Black belt Dan 8", Belt.BLACK)

        "black_dan_9" ->
            rank("black_dan_9", "שחורה דאן 9", "Black belt Dan 9", Belt.BLACK)

        "black_dan_10" ->
            rank("black_dan_10", "שחורה דאן 10", "Black belt Dan 10", Belt.BLACK)

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
        userSp.edit {
            putString("fullName", authName)
        }
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
            userSp.edit {
                putString("fullName", fullName)
            }
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

    val isReauthFailed =
        clean.contains("Account reauth failed", ignoreCase = true) ||
                clean.contains("reauth failed", ignoreCase = true) ||
                clean.contains("[16]", ignoreCase = true)

    val isRealUserCancel =
        error is androidx.credentials.exceptions.GetCredentialCancellationException &&
                !isReauthFailed

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

    if (isReauthFailed) {
        return if (isEnglish) {
            "Google sign-in could not be completed on this device. Please try again, choose another Google account, or update Google Play services."
        } else {
            "לא ניתן היה להשלים את ההתחברות עם Google במכשיר הזה. נסה שוב, בחר חשבון Google אחר, או עדכן את Google Play Services."
        }
    }

    if (isNoCredential) {
        return if (isEnglish) {
            "No Google account was found on this device. Please make sure a Google account is added to the device, update Google Play services, and try again."
        } else {
            "לא נמצא חשבון Google זמין במכשיר. יש לוודא שמוגדר חשבון Google במכשיר, לעדכן את שירותי Google Play Services ולנסות שוב."
        }
    }

    if (isRealUserCancel) {
        return ""
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

@Composable
private fun IntroWelcomeImageScreen(
    isEnglish: Boolean,
    greeting: String,
    rank: IntroRankDisplay?,
    isGoogleLoading: Boolean,
    isProfileStatusLoading: Boolean,
    canContinueWithoutLogin: Boolean,
    googleError: String?,
    onGoogleClick: () -> Unit,
    onContinueClick: () -> Unit,
    onRegularClick: () -> Unit
) {
    /*
     * MaterialTheme כבר מכיל את הבחירה האחרונה של המשתמש:
     * מצב בהיר, מצב כהה או בהתאם למערכת.
     */
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme =
        colorScheme.background.luminance() < 0.5f

    val cardBackground =
        colorScheme.surface.copy(
            alpha = if (isDarkTheme) 0.94f else 0.88f
        )

    val primaryTextColor =
        colorScheme.onSurface

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        val isCompactHeight = maxHeight < 760.dp

        val horizontalPadding =
            if (isCompactHeight) 24.dp else 30.dp

        // מיקום יחסי של הברכה לפי גובה המסך, כדי שיהיה יציב בין מכשירים
        val greetingTopSpace = maxHeight * 0.185f

        val greetingHeight =
            if (isCompactHeight) 38.dp else 42.dp

        // מיקום יחסי של שורת החגורה לפי גובה המסך, כדי שיהיה יציב בין מכשירים
        val beltTopSpace = maxHeight * 0.455f

        val beltRowHeight =
            if (isCompactHeight) {
                84.dp
            } else {
                98.dp
            }

        val beltImageHeight =
            if (isCompactHeight) {
                45.dp
            } else {
                54.dp
            }

        val beltVerticalOffset =
            -(maxHeight * 0.015f)

        /*
         * מצב התצוגה באפליקציה נקבע מתוך MaterialTheme
         * ולכן בוחרים כאן רק את קובץ התמונה המתאים.
         *
         * אין ColorMatrix, אין מסנן צבע ואין שכבת כהות:
         * כל מצב מציג תמונה שעוצבה במיוחד עבורו.
         */
        val introBackgroundRes =
            if (isDarkTheme) {
                R.drawable.intro_welcome_screen_v2_dark
            } else {
                R.drawable.intro_welcome_screen_v2
            }

        Image(
            painter = painterResource(
                id = introBackgroundRes
            ),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(greetingTopSpace))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .heightIn(min = greetingHeight)
                    .shadow(
                        elevation = 1.dp,
                        shape = RoundedCornerShape(10.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBackground)
                    .padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = greeting,
                    style =
                        KmiTypography.screenTitle.copy(
                            fontWeight = FontWeight.ExtraBold,
                            textDirection =
                                if (isEnglish) {
                                    TextDirection.Ltr
                                } else {
                                    TextDirection.Rtl
                                }
                        ),
                    color = primaryTextColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(beltTopSpace))

            if (rank != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .heightIn(min = beltRowHeight)
                        .offset(
                            y = beltVerticalOffset
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isEnglish) rank.en else rank.he,
                        style =
                            KmiTypography.sectionTitle.copy(
                                fontWeight = FontWeight.ExtraBold,
                                textDirection =
                                    if (isEnglish) {
                                        TextDirection.Ltr
                                    } else {
                                        TextDirection.Rtl
                                    }
                            ),
                        color =
                            when (rank.baseBelt) {
                                Belt.WHITE ->
                                    colorScheme.onSurfaceVariant

                                Belt.BLACK ->
                                    colorScheme.onSurface

                                else ->
                                    rank.color
                            },
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(
                            if (isCompactHeight) 3.dp else 5.dp
                        )
                    )

                    Image(
                        painter = painterResource(
                            id = introBeltDrawableRes(rank)
                        ),
                        contentDescription =
                            if (isEnglish) rank.en else rank.he,
                        modifier = Modifier
                            .width(
                                if (isCompactHeight) {
                                    195.dp
                                } else {
                                    225.dp
                                }
                            )
                            .height(beltImageHeight),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .heightIn(min = beltRowHeight)
                        .shadow(
                            elevation = 1.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = false
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBackground)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Belt has not been updated yet"
                        } else {
                            "עדיין לא עודכנה חגורה"
                        },
                        style =
                            KmiTypography.secondary.copy(
                                fontWeight = FontWeight.ExtraBold,
                                textDirection =
                                    if (isEnglish) {
                                        TextDirection.Ltr
                                    } else {
                                        TextDirection.Rtl
                                    }
                            ),
                        color = primaryTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.weight(1.25f))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .heightIn(
                        min =
                            if (isCompactHeight) {
                                40.dp
                            } else {
                                44.dp
                            }
                    )
                    .shadow(
                        elevation = 1.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = kmiGraniteActionBrush()
                    )
                    .clickable(
                        enabled =
                            !isGoogleLoading &&
                                    !isProfileStatusLoading
                    ) {
                        if (canContinueWithoutLogin) {
                            onContinueClick()
                        } else {
                            onGoogleClick()
                        }
                    }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 7.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isGoogleLoading || isProfileStatusLoading) {
                    KmiLoadingRings(
                        size = 34.dp,
                        text = null
                    )
                } else {
                    Text(
                        text = when {
                            canContinueWithoutLogin && isEnglish ->
                                "Continue"

                            canContinueWithoutLogin ->
                                "המשך"

                            isEnglish ->
                                "★ Continue with Google"

                            else ->
                                "התחברות עם Google ★"
                        },
                        color = Color.White,
                        style =
                            KmiTypography.action.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            /*
             * כניסה או רישום מוצגים רק לאחר שבדיקת המשתמש הסתיימה
             * ונמצא שאין פרופיל מלא שניתן להמשיך באמצעותו.
             */
            if (
                !isProfileStatusLoading &&
                !canContinueWithoutLogin
            ) {
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .heightIn(
                            min =
                                if (isCompactHeight) {
                                    36.dp
                                } else {
                                    40.dp
                                }
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBackground)
                        .clickable(
                            enabled = !isGoogleLoading
                        ) {
                            onRegularClick()
                        }
                        .padding(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Existing login / regular registration"
                        } else {
                            "כניסה / רישום בדרך הרגילה"
                        },
                        color = primaryTextColor,
                        style =
                            KmiTypography.action.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            if (!googleError.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))

                Text(
                    text = googleError,
                    color = MaterialTheme.colorScheme.error,
                    style = KmiTypography.caption,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(if (isCompactHeight) 2.dp else 4.dp))
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
            canEnterApp = false,
            missingFields = listOf("profile_check_failed")
        )
    }

    GoogleAuthManager.logUiStage(
        context = ctx,
        stage = "intro_profile_check_finished",
        message = "isComplete=${profileStatus.isComplete}, canEnterApp=${profileStatus.canEnterApp}, missingFields=${
            profileStatus.missingFields.joinToString(
                "|"
            )
        }"
    )

    userSp.edit {
        putBoolean(SUPPRESS_NEXT_DRAWER_OPEN_KEY, true)
    }

    legacySp.edit {
        putBoolean(SUPPRESS_NEXT_DRAWER_OPEN_KEY, true)
    }

    GoogleAuthManager.logUiStage(
        context = ctx,
        stage = "intro_navigation_decision",
        message = "profileComplete=${profileStatus.isComplete}, canEnterApp=${profileStatus.canEnterApp}"
    )

    if (profileStatus.canEnterApp) {
        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_call_on_app_enter_allowed"
        )

        onProfileComplete()
    } else {
        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_call_on_profile_missing_basic_details"
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
    var isGoogleLoading by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }

    /*
     * משתמש חוזר יקבל כפתור "המשך" רק לאחר שבדיקת
     * הפרופיל אישרה שכל פרטי החובה קיימים.
     */
    var canContinueWithoutLogin by remember {
        mutableStateOf(false)
    }

    /*
     * מתחילים תמיד במצב בדיקה.
     * FirebaseAuth עשוי להחזיר null בפריים הראשון בזמן שחזור המשתמש,
     * ולכן אסור להציג לפני סיום הבדיקה את כפתור Google.
     */
    var isProfileStatusLoading by remember {
        mutableStateOf(true)
    }

    // מונע הפעלה כפולה של Google Login בגלל לחיצה כפולה / recomposition
    var googleFlowLocked by remember { mutableStateOf(false) }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val currentLang = langManager.getCurrentLanguage()
    val isEnglish = currentLang == AppLanguage.ENGLISH

    val userSp = remember {
        ctx.getSharedPreferences(
            "kmi_user",
            Context.MODE_PRIVATE
        )
    }

    // הקובץ השני שבו נשמרים חלק מהדגלים הישנים של האפליקציה.
    val legacySp = remember {
        ctx.getSharedPreferences(
            "kmi_prefs",
            Context.MODE_PRIVATE
        )
    }

    /*
     * בדיקה אוטומטית של משתמש שכבר התחבר בעבר.
     * משתמש לא מחובר או אנונימי נשאר בזרימת הכניסה הקיימת.
     */
    LaunchedEffect(Unit) {
        val existingFirebaseUser =
            FirebaseAuth.getInstance().currentUser
                ?.takeIf { !it.isAnonymous }

        if (existingFirebaseUser == null) {
            canContinueWithoutLogin = false
            isProfileStatusLoading = false
        } else {
            isProfileStatusLoading = true

            val profileStatus = runCatching {
                UserProfileCompletion
                    .checkAndPersistProfileStatus(ctx)
            }.getOrNull()

            canContinueWithoutLogin =
                profileStatus?.canEnterApp == true

            isProfileStatusLoading = false

            GoogleAuthManager.logUiStage(
                context = ctx,
                stage = "intro_existing_user_profile_checked",
                message =
                    "uid=${existingFirebaseUser.uid}, " +
                            "canContinue=$canContinueWithoutLogin, " +
                            "isComplete=${profileStatus?.isComplete}, " +
                            "missingFields=" +
                            profileStatus
                                ?.missingFields
                                .orEmpty()
                                .joinToString("|")
            )
        }
    }

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

                    if (!googleError.isNullOrBlank()) {
                        Toast.makeText(
                            ctx,
                            googleError.orEmpty(),
                            Toast.LENGTH_LONG
                        ).show()
                    }
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

    LaunchedEffect(Unit) {
        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_screen_opened",
            message = "currentUserUid=${FirebaseAuth.getInstance().currentUser?.uid.orEmpty()}, currentUserEmail=${FirebaseAuth.getInstance().currentUser?.email.orEmpty()}, isAnonymous=${FirebaseAuth.getInstance().currentUser?.isAnonymous}"
        )

        KmiAccess.ensureTrialStarted(userSp)

        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_trial_started_or_verified",
            message = "currentUserUid=${FirebaseAuth.getInstance().currentUser?.uid.orEmpty()}, isAnonymous=${FirebaseAuth.getInstance().currentUser?.isAnonymous}"
        )
    }

    // ✅ FIX: משתמשים באותו userSp שממנו אתה מתחיל Trial ושבו נשמר המשתמש
    val (dynamicGreeting0, traineeRankOrNull) = rememberGreetingAndRank(
        userSp = userSp,
        lang = currentLang
    )

    val dynamicGreeting =
        remember(
            dynamicGreeting0,
            fetchedName,
            currentLang
        ) {
            fetchedName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.split(' ', limit = 2)
                ?.firstOrNull()
                ?.let { first ->
                    if (currentLang == AppLanguage.ENGLISH) {
                        "Hello, $first"
                    } else {
                        "שלום $first"
                    }
                }
                ?: dynamicGreeting0
        }

    val startGoogleLogin: () -> Unit = {
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
        } else {
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

                                if (!googleError.isNullOrBlank()) {
                                    Toast.makeText(
                                        ctx,
                                        googleError.orEmpty(),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
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

                            if (!googleError.isNullOrBlank()) {
                                Toast.makeText(
                                    ctx,
                                    googleError.orEmpty(),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
            }
        }
    }

    val openRegularLogin: () -> Unit = {
        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_regular_login_clicked",
            message = "isGoogleLoading=$isGoogleLoading, googleFlowLocked=$googleFlowLocked, currentUserUid=${FirebaseAuth.getInstance().currentUser?.uid.orEmpty()}, isAnonymous=${FirebaseAuth.getInstance().currentUser?.isAnonymous}"
        )

        userSp.edit {
            putBoolean(SUPPRESS_NEXT_DRAWER_OPEN_KEY, true)
        }

        legacySp.edit {
            putBoolean(SUPPRESS_NEXT_DRAWER_OPEN_KEY, true)
        }

        GoogleAuthManager.logUiStage(
            context = ctx,
            stage = "intro_regular_login_call_on_continue"
        )

        onContinue()
    }

    val continueExistingUser: () -> Unit = {
        if (canContinueWithoutLogin) {
            userSp.edit {
                putBoolean(
                    SUPPRESS_NEXT_DRAWER_OPEN_KEY,
                    true
                )
            }

            legacySp.edit {
                putBoolean(
                    SUPPRESS_NEXT_DRAWER_OPEN_KEY,
                    true
                )
            }

            GoogleAuthManager.logUiStage(
                context = ctx,
                stage = "intro_existing_user_continue_clicked",
                message =
                    "uid=" +
                            FirebaseAuth.getInstance()
                                .currentUser
                                ?.uid
                                .orEmpty()
            )

            onProfileComplete()
        }
    }

    IntroWelcomeImageScreen(
        isEnglish = isEnglish,
        greeting = dynamicGreeting,
        rank = traineeRankOrNull,
        isGoogleLoading = isGoogleLoading,
        isProfileStatusLoading = isProfileStatusLoading,
        canContinueWithoutLogin = canContinueWithoutLogin,
        googleError = googleError,
        onGoogleClick = startGoogleLogin,
        onContinueClick = continueExistingUser,
        onRegularClick = openRegularLogin
    )
}
