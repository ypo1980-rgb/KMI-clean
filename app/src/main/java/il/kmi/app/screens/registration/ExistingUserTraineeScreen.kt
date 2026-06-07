@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package il.kmi.app.screens.registration

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.KmiViewModel
import il.kmi.shared.prefs.KmiPrefs
import kotlinx.coroutines.launch
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import il.kmi.app.FcmTokenManager
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

//======================================================================

// מנרמל קוד: מוריד רווחים/מקפים וממיר ל-UPPERCASE
private fun String?.normalizeCoachCode(): String =
    this?.trim()?.replace(" ", "")?.replace("-", "")?.uppercase() ?: ""

private fun firstNonBlank(vararg values: String?): String =
    values
        .asSequence()
        .map { it.orEmpty().trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()

private suspend fun resolveLoginUserUid(
    appCtx: Context,
    sp: SharedPreferences,
    username: String
): String {
    val userSp = appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    val cleanUsername = username.trim()

    val db = FirebaseFirestore.getInstance()

    if (cleanUsername.isNotBlank()) {
        val usernameFields = listOf(
            "username",
            "userName",
            "loginUsername",
            "login_name",
            "user_login",
            "email",
            "emailLower"
        )

        for (field in usernameFields) {
            val snap = db.collection("users")
                .whereEqualTo(
                    field,
                    if (field == "emailLower") cleanUsername.lowercase() else cleanUsername
                )
                .limit(1)
                .get()
                .await()

            val doc = snap.documents.firstOrNull()
            if (doc != null) {
                return doc.id
            }
        }
    }

    val localUid = firstNonBlank(
        sp.getString("uid", null),
        sp.getString("profile_completed_uid", null),
        sp.getString("user_uid", null),
        sp.getString("firebase_uid", null),
        sp.getString("auth_uid", null),
        userSp.getString("uid", null),
        userSp.getString("profile_completed_uid", null),
        userSp.getString("user_uid", null),
        userSp.getString("firebase_uid", null),
        userSp.getString("auth_uid", null)
    )

    if (localUid.isNotBlank()) {
        return localUid
    }

    val authUser = FirebaseAuth.getInstance().currentUser
    return authUser
        ?.takeIf { !it.isAnonymous }
        ?.uid
        .orEmpty()
        .trim()
}

private suspend fun verifyCoachInviteWithServer(
    phoneDigits: String,
    emailLower: String
): Result<Map<String, Any?>> {
    return runCatching {
        val data = hashMapOf(
            "phoneDigits" to phoneDigits.filter { it.isDigit() },
            "emailLower" to emailLower.trim().lowercase()
        )

        val result = FirebaseFunctions
            .getInstance("us-central1")
            .getHttpsCallable("verifyCoachInvite")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        result.data as? Map<String, Any?> ?: emptyMap()
    }
}

@Composable
private fun ExistingUserLockedTopBar(
    title: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(38.dp))

                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        color = Color(0xFF111827)
                    )
                )

                Spacer(Modifier.width(38.dp))
            }
        }
    }
}

@Composable
fun ExistingUserTraineeScreen(
    onBack: () -> Unit,
    onLoginComplete: () -> Unit,
    onOpenRecovery: () -> Unit = {},
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    onOpenDrawer: () -> Unit = {}   // ← חדש
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val appCtx = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    // כשהמקלדת פתוחה, מסתירים את הקרדיט הקבוע בתחתית
    // כדי שלא יעלה מעל המקלדת ויכסה את שדות ההתחברות.
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    val langManager = remember { AppLanguageManager(appCtx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    // 🔊+📳 טעינת ההעדפות ממסך ההגדרות (kmi_settings)
    val settingsSp = remember {
        appCtx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
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
            // רטט חזק
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

// מצב נבחר: מתאמן/מאמן
// משתמש קיים ייפתח כמאמן רק אם כבר אומת בעבר מול authorizedCoaches
// ונשמרו user_role=coach + coach_authorized=true.
    val userSpForInitialRole = remember(appCtx) {
        appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    var isCoach by rememberSaveable {
        mutableStateOf(
            (
                    sp.getString("user_role", "")
                        .equals("coach", ignoreCase = true) ||
                            userSpForInitialRole.getString("user_role", "")
                                .equals("coach", ignoreCase = true)
                    ) &&
                    (
                            sp.getBoolean("coach_authorized", false) ||
                                    userSpForInitialRole.getBoolean("coach_authorized", false)
                            )
        )
    }

    // מאמן בלבד
    var coachCode by rememberSaveable { mutableStateOf("") }
    var coachCodeError by remember { mutableStateOf(false) }
    var serverCoachCode by remember { mutableStateOf<String?>(null) }
    var serverCoachRole by remember { mutableStateOf("") }
    var serverCoachActive by remember { mutableStateOf(false) }
    var serverCoachName by remember { mutableStateOf("") }

    var canOpenCoachDrawer by remember { mutableStateOf(false) }
    var canViewTrainees by remember { mutableStateOf(false) }
    var canManageTrainees by remember { mutableStateOf(false) }
    var canManageAttendance by remember { mutableStateOf(false) }
    var canManageInternalExams by remember { mutableStateOf(false) }
    var canViewPaymentReports by remember { mutableStateOf(false) }
    var canManagePayments by remember { mutableStateOf(false) }
    var canSendBroadcasts by remember { mutableStateOf(false) }

    var coachCodeResetError by rememberSaveable { mutableStateOf<String?>(null) }
    var coachCodeResetSuccess by rememberSaveable { mutableStateOf<String?>(null) }
    var resettingCoachCode by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isCoach) {
        if (isCoach) {
            coachCodeError = false
            coachCodeResetError = null
            coachCodeResetSuccess = null
            return@LaunchedEffect
        }

        serverCoachCode = null
        serverCoachRole = ""
        serverCoachActive = false
        serverCoachName = ""

        canOpenCoachDrawer = false
        canViewTrainees = false
        canManageTrainees = false
        canManageAttendance = false
        canManageInternalExams = false
        canViewPaymentReports = false
        canManagePayments = false
        canSendBroadcasts = false
    }

    // שדות
    var username by rememberSaveable {
        mutableStateOf(sp.getString("remember_username", sp.getString("username", "") ?: "") ?: "")
    }
    var password by rememberSaveable {
        mutableStateOf(sp.getString("remember_password", sp.getString("password", "") ?: "") ?: "")
    }

    var rememberMe by rememberSaveable { mutableStateOf(sp.getBoolean("remember_me_login", false)) }
    var loginError by remember { mutableStateOf(false) }
    var loginDebugText by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    // אם rememberMe דלוק – טוענים קרדנציאלס מראש
    LaunchedEffect(Unit) {
        if (rememberMe) {
            username = sp.getString("remember_username", "") ?: ""
            password = sp.getString("remember_password", "") ?: ""
        }
    }

    // BringIntoView לשדות
    val usernameBring = remember { BringIntoViewRequester() }
    val passwordBring = remember { BringIntoViewRequester() }

    val fieldWidth = 0.88f
    val fieldHeight = 52.dp

    // רקע ברירת מחדל (ירוק-כחול כמו מסך כניסה / רישום)
    val traineeBg = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0F172A),   // כחול כהה
                Color(0xFF1D4ED8),   // כחול ראשי של מתאמן
                Color(0xFF0EA5E9)    // כחול טורקיז תחתון
            )
        )
    }
    // רקע למצב מאמן – גרדיאנט מודרני בכחול/טורקיז
    val coachBg = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF141E30), // כחול-לילה עמוק
                Color(0xFF243B55), // כחול פלדה מודרני
                Color(0xFF0EA5E9)  // טורקיז/כחול אנרגטי
            )
        )
    }

    // —— ניווט חד־פעמי לאחר התחברות מוצלחת ——
    var loginSucceeded by rememberSaveable { mutableStateOf(false) }
    var navigated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(loginSucceeded) {
        if (loginSucceeded && !navigated) {
            navigated = true

            // 👇 שמירת FCM token למשתמש שנכנס
            FcmTokenManager.refreshTokenForCurrentUser()

            onLoginComplete()
        }
    }

    // דיאלוג שחזור פרטים
    var showRecoveryDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ExistingUserLockedTopBar(
                title = tr("התחברות", "Login"),
            )
                 },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(if (isCoach) coachBg else traineeBg) // ← רקע לפי role
                .padding(innerPadding)
        ) {
            // ---------- מרווח עליון דינמי ----------
            val cfg = LocalConfiguration.current
            val topPad = if (cfg.screenHeightDp <= 700) 40.dp else 52.dp
            Spacer(Modifier.height(topPad))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .imePadding()
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp) // רווח בין כפתור התחבר לכפתור שכחתי סיסמה / שם משתמש
            ) {

                Spacer(Modifier.height(8.dp))

                // טאבים: מתאמן / מאמן
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(fieldWidth)
                        .padding(bottom = 6.dp),
                    color = Color.White.copy(alpha = 0.10f)
                ) {
                    val selectedIndex = if (isCoach) 1 else 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-4).dp)
                                .width(1.dp)
                                .height(24.dp)
                                .background(Color.White.copy(alpha = 0.95f))
                        )

                        TabRow(
                            selectedTabIndex = selectedIndex,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            divider = {},
                            indicator = { positions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(positions[selectedIndex]),
                                    height = 3.dp,
                                    color = Color.White
                                )
                            },
                            modifier = Modifier.matchParentSize()
                        ) {
                            Tab(
                                selected = !isCoach,
                                onClick = {
                                    if (isCoach) {
                                        playStrongFeedback()

                                        // מעבר טאב בלבד.
                                        // לא משנים user_role ולא מוחקים הרשאת מאמן שמורה.
                                        // התפקיד נשמר רק אחרי לחיצה על התחבר והתחברות מוצלחת.
                                        isCoach = false
                                        loginError = false
                                    }
                                },
                                text = { Text(tr("מתאמן", "Trainee"), fontWeight = FontWeight.Bold) },
                                selectedContentColor = Color.White,
                                unselectedContentColor = Color.White.copy(alpha = 0.7f)
                            )

                            Tab(
                                selected = isCoach,
                                onClick = {
                                    if (!isCoach) {
                                        playStrongFeedback()

                                        // בחירת מאמן היא רק ניסיון כניסה.
                                        // לא שומרים user_role=coach לפני אימות הרשאה מהשרת.
                                        isCoach = true
                                        coachCodeError = false
                                    }
                                },
                                text = { Text(tr("מאמן", "Coach"), fontWeight = FontWeight.Bold) },
                                selectedContentColor = Color.White,
                                unselectedContentColor = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // מצב מאמן:
                // אין יותר שדה קוד מאמן במסך.
                // האימות יתבצע מול Cloud Function לפי Firebase Auth + אימייל + טלפון + coachInvites.
                if (isCoach) {
                    Text(
                        text = tr(
                            "מצב מאמן יאומת מול השרת בעת ההתחברות",
                            "Coach mode will be verified by the server during login"
                        ),
                        color = Color.White.copy(alpha = 0.90f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(fieldWidth)
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(tr("שם משתמש", "Username"), color = Color.Black) },
                    modifier = Modifier
                        .fillMaxWidth(fieldWidth)
                        .defaultMinSize(minHeight = fieldHeight)
                        .background(Color.White, shape = MaterialTheme.shapes.medium)
                        .bringIntoViewRequester(usernameBring)
                        .onFocusChanged {
                            if (it.isFocused) scope.launch { usernameBring.bringIntoView() }
                        },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(tr("סיסמה", "Password"), color = Color.Black) },
                    modifier = Modifier
                        .fillMaxWidth(fieldWidth)
                        .defaultMinSize(minHeight = fieldHeight)
                        .background(Color.White, shape = MaterialTheme.shapes.medium)
                        .bringIntoViewRequester(passwordBring)
                        .onFocusChanged {
                            if (it.isFocused) scope.launch { passwordBring.bringIntoView() }
                        },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val desc = if (passwordVisible) {
                            tr("הסתר סיסמה", "Hide password")
                        } else {
                            tr("הצג סיסמה", "Show password")
                        }
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = desc, tint = Color.Black)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )

                // שמירה לכניסה הבאה
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        modifier = Modifier.padding(end = 2.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.White,
                            uncheckedColor = Color.White,
                            checkmarkColor = Color.Black
                        )
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        tr("שמירה לכניסה הבאה", "Remember me"),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(end = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (loginError) {
                    Text(
                        text = tr("פרטי ההתחברות שגויים", "Invalid login details"),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(fieldWidth)
                    )
                }

                Button(
                    onClick = {
                        playStrongFeedback()
                        loginDebugText = null

                        scope.launch {
                            val userSpForLogin = appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

                            val savedUsername = sp.getString("username", "") ?: ""
                            val savedPassword = sp.getString("password", "") ?: ""
                            val savedEmail =
                                sp.getString("email", null)
                                    ?: userSpForLogin.getString("email", null)
                                    ?: ""

                            val credsOk =
                                username.isNotBlank() &&
                                        password.isNotBlank() &&
                                        savedUsername.isNotBlank() &&
                                        savedPassword.isNotBlank() &&
                                        username.trim().equals(savedUsername.trim(), ignoreCase = true) &&
                                        password == savedPassword

                            if (!credsOk) {
                                loginError = true
                                return@launch
                            }

                            var resolvedCoachCode = ""
                            var resolvedCoachRole = ""
                            var resolvedCoachActive = false
                            var resolvedCoachName = ""

                            var resolvedCanOpenCoachDrawer = false
                            var resolvedCanViewTrainees = false
                            var resolvedCanManageTrainees = false
                            var resolvedCanManageAttendance = false
                            var resolvedCanManageInternalExams = false
                            var resolvedCanViewPaymentReports = false
                            var resolvedCanManagePayments = false
                            var resolvedCanSendBroadcasts = false

                            var resolvedLoginUid = ""

                            val coachOk = if (isCoach) {
                                val loginEmail =
                                    savedEmail.trim().takeIf { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
                                        ?: username.trim().takeIf { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
                                        ?: runCatching {
                                            FirebaseFirestore.getInstance()
                                                .collection("users")
                                                .whereEqualTo("username", username.trim())
                                                .limit(1)
                                                .get()
                                                .await()
                                                .documents
                                                .firstOrNull()
                                                ?.getString("email")
                                                .orEmpty()
                                        }.getOrDefault("")
                                            .takeIf { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
                                        ?: runCatching {
                                            FirebaseFirestore.getInstance()
                                                .collection("users")
                                                .whereEqualTo("userName", username.trim())
                                                .limit(1)
                                                .get()
                                                .await()
                                                .documents
                                                .firstOrNull()
                                                ?.getString("email")
                                                .orEmpty()
                                        }.getOrDefault("")
                                            .takeIf { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
                                        ?: ""

                                val cleanPhoneDigits =
                                    (
                                            sp.getString("phone", null)
                                                ?: sp.getString("phoneDigits", null)
                                                ?: userSpForLogin.getString("phone", null)
                                                ?: userSpForLogin.getString("phoneDigits", null)
                                                ?: ""
                                            ).filter { it.isDigit() }

                                if (loginEmail.isBlank()) {
                                    false
                                } else if (cleanPhoneDigits.isBlank()) {
                                    false
                                } else {
                                    val firebaseUidFromLogin = runCatching {
                                        FirebaseAuth.getInstance()
                                            .signInWithEmailAndPassword(loginEmail, password)
                                            .await()
                                            .user
                                            ?.uid
                                            .orEmpty()
                                    }.getOrDefault("")

                                    val uid = firebaseUidFromLogin.ifBlank {
                                        resolveLoginUserUid(
                                            appCtx = appCtx,
                                            sp = sp,
                                            username = username
                                        )
                                    }

                                    resolvedLoginUid = uid

                                    if (uid.isBlank()) {
                                        false
                                    } else {
                                        val existingDoc = runCatching {
                                            FirebaseFirestore.getInstance()
                                                .collection("authorizedCoaches")
                                                .document(uid)
                                                .get()
                                                .await()
                                        }.getOrNull()

                                        if (existingDoc?.exists() == true) {
                                            resolvedCoachActive = existingDoc.getBoolean("active") == true
                                            resolvedCoachRole = existingDoc.getString("role").orEmpty()
                                            resolvedCoachCode = existingDoc.getString("coachCode").orEmpty()
                                            resolvedCoachName = existingDoc.getString("fullName").orEmpty()

                                            resolvedCanOpenCoachDrawer =
                                                existingDoc.getBoolean("canOpenCoachDrawer") == true
                                            resolvedCanViewTrainees =
                                                existingDoc.getBoolean("canViewTrainees") == true
                                            resolvedCanManageTrainees =
                                                existingDoc.getBoolean("canManageTrainees") == true
                                            resolvedCanManageAttendance =
                                                existingDoc.getBoolean("canManageAttendance") == true
                                            resolvedCanManageInternalExams =
                                                existingDoc.getBoolean("canManageInternalExams") == true ||
                                                        existingDoc.getBoolean("canManageExams") == true
                                            resolvedCanViewPaymentReports =
                                                existingDoc.getBoolean("canViewPaymentReports") == true
                                            resolvedCanManagePayments =
                                                existingDoc.getBoolean("canManagePayments") == true
                                            resolvedCanSendBroadcasts =
                                                existingDoc.getBoolean("canSendBroadcasts") == true

                                            val valid =
                                                resolvedCoachActive &&
                                                        resolvedCoachRole.equals("coach", ignoreCase = true)

                                            valid
                                        } else {
                                            val verifyResult = verifyCoachInviteWithServer(
                                                phoneDigits = cleanPhoneDigits,
                                                emailLower = loginEmail
                                            )

                                            if (verifyResult.isFailure) {
                                                false
                                            } else {
                                                val freshDoc = runCatching {
                                                        FirebaseFirestore.getInstance()
                                                            .collection("authorizedCoaches")
                                                            .document(uid)
                                                            .get()
                                                            .await()
                                                    }.getOrNull()

                                                if (freshDoc?.exists() != true) {
                                                    false
                                                } else {
                                                        resolvedCoachActive = freshDoc.getBoolean("active") == true
                                                        resolvedCoachRole = freshDoc.getString("role").orEmpty()
                                                        resolvedCoachCode = freshDoc.getString("coachCode").orEmpty()
                                                        resolvedCoachName = freshDoc.getString("fullName").orEmpty()

                                                        resolvedCanOpenCoachDrawer =
                                                            freshDoc.getBoolean("canOpenCoachDrawer") == true
                                                        resolvedCanViewTrainees =
                                                            freshDoc.getBoolean("canViewTrainees") == true
                                                        resolvedCanManageTrainees =
                                                            freshDoc.getBoolean("canManageTrainees") == true
                                                        resolvedCanManageAttendance =
                                                            freshDoc.getBoolean("canManageAttendance") == true
                                                        resolvedCanManageInternalExams =
                                                            freshDoc.getBoolean("canManageInternalExams") == true ||
                                                                    freshDoc.getBoolean("canManageExams") == true
                                                        resolvedCanViewPaymentReports =
                                                            freshDoc.getBoolean("canViewPaymentReports") == true
                                                        resolvedCanManagePayments =
                                                            freshDoc.getBoolean("canManagePayments") == true
                                                        resolvedCanSendBroadcasts =
                                                            freshDoc.getBoolean("canSendBroadcasts") == true

                                                        val valid =
                                                            resolvedCoachActive &&
                                                                    resolvedCoachRole.equals("coach", ignoreCase = true)

                                                    valid
                                                    }
                                            }
                                        }
                                    }
                                }
                            } else {
                                resolvedLoginUid = resolveLoginUserUid(
                                    appCtx = appCtx,
                                    sp = sp,
                                    username = username
                                )

                                true
                            }

                            if (!coachOk) {
                                loginError = true

                                // ניסיון כניסה כמאמן נכשל.
                                // לא מוחקים הרשאת מאמן קיימת ולא שומרים trainee,
                                // כדי לא לפגוע במאמן מורשה בגלל כשל זמני בזיהוי UID / רשת / Firebase.
                                return@launch
                            }

                            loginError = false

                            if (rememberMe && password.isNotBlank()) {
                                sp.edit()
                                    .putBoolean("remember_me_login", true)
                                    .putString("remember_username", username.trim())
                                    .putString("remember_password", password)
                                    .apply()
                            } else {
                                sp.edit()
                                    .putBoolean("remember_me_login", false)
                                    .remove("remember_username")
                                    .remove("remember_password")
                                    .apply()
                            }

                            val role = if (isCoach) "coach" else "trainee"

                            if (resolvedLoginUid.isBlank()) {
                                resolvedLoginUid = resolveLoginUserUid(
                                    appCtx = appCtx,
                                    sp = sp,
                                    username = username
                                )
                            }

                            sp.edit()
                                .putString("uid", resolvedLoginUid)
                                .putString("profile_completed_uid", resolvedLoginUid)
                                .putString("user_role", role)
                                .putString("coach_code", if (role == "coach") resolvedCoachCode else "")
                                .putString("coach_name", if (role == "coach") resolvedCoachName else "")
                                .putBoolean("coach_authorized", role == "coach")
                                .putBoolean("can_open_coach_drawer", role == "coach" && resolvedCanOpenCoachDrawer)
                                .putBoolean("can_view_trainees", role == "coach" && resolvedCanViewTrainees)
                                .putBoolean("can_manage_trainees", role == "coach" && resolvedCanManageTrainees)
                                .putBoolean("can_manage_attendance", role == "coach" && resolvedCanManageAttendance)
                                .putBoolean("can_manage_internal_exams", role == "coach" && resolvedCanManageInternalExams)
                                .putBoolean("can_view_payment_reports", role == "coach" && resolvedCanViewPaymentReports)
                                .putBoolean("can_manage_payments", role == "coach" && resolvedCanManagePayments)
                                .putBoolean("can_send_broadcasts", role == "coach" && resolvedCanSendBroadcasts)
                                .putBoolean("is_logged_in", true)
                                .apply()

                            appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
                                .edit()
                                .putString("uid", resolvedLoginUid)
                                .putString("profile_completed_uid", resolvedLoginUid)
                                .putString("user_role", role)
                                .putString("coach_code", if (role == "coach") resolvedCoachCode else "")
                                .putString("coach_name", if (role == "coach") resolvedCoachName else "")
                                .putBoolean("coach_authorized", role == "coach")
                                .putBoolean("can_open_coach_drawer", role == "coach" && resolvedCanOpenCoachDrawer)
                                .putBoolean("can_view_trainees", role == "coach" && resolvedCanViewTrainees)
                                .putBoolean("can_manage_trainees", role == "coach" && resolvedCanManageTrainees)
                                .putBoolean("can_manage_attendance", role == "coach" && resolvedCanManageAttendance)
                                .putBoolean("can_manage_internal_exams", role == "coach" && resolvedCanManageInternalExams)
                                .putBoolean("can_view_payment_reports", role == "coach" && resolvedCanViewPaymentReports)
                                .putBoolean("can_manage_payments", role == "coach" && resolvedCanManagePayments)
                                .putBoolean("can_send_broadcasts", role == "coach" && resolvedCanSendBroadcasts)
                                .putBoolean("is_logged_in", true)
                                .apply()

                            kmiPrefs.username = username
                            kmiPrefs.password = password

                            loginSucceeded = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(fieldWidth)
                        .defaultMinSize(minHeight = fieldHeight)
                        .background(Color.White, shape = MaterialTheme.shapes.medium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(tr("התחבר", "Login"))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth(fieldWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(-15.dp)
                ) {
                    TextButton(
                        onClick = { showRecoveryDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 0.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            tr("שכחתי סיסמה / שם משתמש", "Forgot password / username"),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }

            // ===== קרדיט קבוע בתחתית המסך =====
            // מוצג רק כשהמקלדת סגורה.
            // כשהמקלדת פתוחה הוא מוסתר כדי לא לכסות את שדות ההתחברות.
            if (!isKeyboardVisible) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = tr(
                            "❤️ פותח באהבה ע\"י יובל פולק ❤️",
                            "❤️ Developed with love by Yuval Polak ❤️"
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = if (LocalConfiguration.current.screenWidthDp <= 360) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // דיאלוג שחזור סיסמה / שם משתמש
            if (showRecoveryDialog) {
                RecoveryDialog(
                    onDismiss = { showRecoveryDialog = false }
                )
            }
        }
    }
}

@Composable
private fun RecoveryDialog(
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    var email by rememberSaveable { mutableStateOf("") }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var successText by rememberSaveable { mutableStateOf<String?>(null) }
    var isSending by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isSending) onDismiss()
        },
        title = {
            Text(
                text = tr("שחזור סיסמה", "Password Recovery"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = tr(
                        "הזן את כתובת האימייל שאיתה נרשמת לאפליקציה. נשלח אליך מייל לאיפוס הסיסמה.",
                        "Enter the email address you used to register. We will send you a password reset email."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorText = null
                        successText = null
                    },
                    label = { Text(tr("אימייל", "Email")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    singleLine = true,
                    enabled = !isSending,
                    isError = errorText != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    )
                )

                if (!errorText.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = errorText.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!successText.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = successText.orEmpty(),
                        color = Color(0xFF16A34A),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSending,
                onClick = {
                    val cleanEmail = email.trim()

                    when {
                        cleanEmail.isBlank() -> {
                            errorText = tr(
                                "יש להזין כתובת אימייל.",
                                "Please enter an email address."
                            )
                        }

                        !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                            errorText = tr(
                                "כתובת האימייל אינה תקינה.",
                                "Invalid email address."
                            )
                        }

                        else -> {
                            isSending = true
                            errorText = null
                            successText = null

                            scope.launch {
                                runCatching {
                                    val resetUrl = "https://app-1c22cc8d.web.app/reset-password.html"

                                    val actionCodeSettings = ActionCodeSettings.newBuilder()
                                        .setUrl(resetUrl)
                                        .setHandleCodeInApp(false)
                                        .build()

                                    FirebaseAuth.getInstance()
                                        .sendPasswordResetEmail(cleanEmail, actionCodeSettings)
                                        .await()
                                }.onSuccess {
                                    isSending = false
                                    successText = tr(
                                        "נשלח מייל לשחזור הסיסמה. בדוק את תיבת הדואר שלך וגם את תיקיית הספאם / דואר זבל.",
                                        "A password reset email was sent. Please check your inbox and also your spam or junk folder."
                                    )
                                }.onFailure { error ->
                                    isSending = false

                                    errorText = when {
                                        error.message?.contains("badly formatted", ignoreCase = true) == true -> {
                                            tr(
                                                "כתובת האימייל אינה תקינה.",
                                                "Invalid email address."
                                            )
                                        }

                                        error.message?.contains("network", ignoreCase = true) == true -> {
                                            tr(
                                                "אין חיבור תקין לרשת. נסה שוב.",
                                                "Network error. Please try again."
                                            )
                                        }

                                        else -> {
                                            tr(
                                                "לא הצלחנו לשלוח מייל שחזור. ודא שהאימייל קיים במערכת ונסה שוב.",
                                                "Failed to send a reset email. Make sure the email exists and try again."
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) {
                Text(
                    text = if (isSending) {
                        tr("שולח...", "Sending...")
                    } else {
                        tr("שלח מייל שחזור", "Send reset email")
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSending,
                onClick = onDismiss
            ) {
                Text(tr("סגור", "Close"))
            }
        }
    )
}
