@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package il.kmi.app.screens.registration

import android.content.Context
import android.content.SharedPreferences
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.KmiViewModel
import il.kmi.app.domain.CoachRegistry
import il.kmi.app.ui.KmiTopBar
import il.kmi.shared.prefs.KmiPrefs
import kotlinx.coroutines.launch
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import il.kmi.app.FcmTokenManager

// מנרמל קוד: מוריד רווחים/מקפים וממיר ל-UPPERCASE
private fun String?.normalizeCoachCode(): String =
    this?.trim()?.replace(" ", "")?.replace("-", "")?.uppercase() ?: ""

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
    var isCoach by rememberSaveable {
        mutableStateOf(sp.getString("user_role", null)?.equals("coach", ignoreCase = true) == true)
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
    var passwordVisible by remember { mutableStateOf(false) }

    // מאמן בלבד
    var coachCode by rememberSaveable { mutableStateOf(sp.getString("coach_code", "") ?: "") }
    var coachCodeError by remember { mutableStateOf(false) }

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

    // רקע ברירת מחדל (סגול-כחול)
    val traineeBg = remember {
        Brush.linearGradient(
            listOf(Color(0xFF7F00FF), Color(0xFF3F51B5), Color(0xFF03A9F4)),
            start = Offset(0f, 0f), end = Offset(1000f, 3000f)
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
            KmiTopBar(
                title = "התחברות",
                showRoleStatus = false,
                showBottomActions = true,
                onOpenDrawer = onOpenDrawer,
                extraActions = { Spacer(Modifier.width(48.dp)) },
                showRoleBadge = false,
                lockSearch = true,
                showCoachBroadcastFab = isCoach
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        isCoach = false
                                        sp.edit().putString("user_role", "trainee").apply()
                                        appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
                                            .edit().putString("user_role", "trainee").apply()
                                    }
                                },
                                text = { Text("מתאמן", fontWeight = FontWeight.Bold) },
                                selectedContentColor = Color.White,
                                unselectedContentColor = Color.White.copy(alpha = 0.7f)
                            )

                            Tab(
                                selected = isCoach,
                                onClick = {
                                    if (!isCoach) {
                                        playStrongFeedback()
                                        isCoach = true
                                        sp.edit().putString("user_role", "coach").apply()
                                        appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
                                            .edit().putString("user_role", "coach").apply()
                                    }
                                },
                                text = { Text("מאמן", fontWeight = FontWeight.Bold) },
                                selectedContentColor = Color.White,
                                unselectedContentColor = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // שדה קוד מאמן (רק אם נבחר "מאמן")
                if (isCoach) {
                    OutlinedTextField(
                        value = coachCode,
                        onValueChange = { coachCode = it; coachCodeError = false },
                        label = { Text("קוד מאמן", color = Color.Black) },
                        modifier = Modifier
                            .fillMaxWidth(fieldWidth)
                            .defaultMinSize(minHeight = fieldHeight)
                            .background(Color.White, shape = MaterialTheme.shapes.medium),
                        isError = coachCodeError,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )
                    if (coachCodeError) {
                        Text("קוד מאמן שגוי", color = MaterialTheme.colorScheme.error)
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("שם משתמש", color = Color.Black) },
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
                    label = { Text("סיסמה", color = Color.Black) },
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
                        val desc = if (passwordVisible) "הסתר סיסמה" else "הצג סיסמה"
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
                        .fillMaxWidth(fieldWidth)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Spacer(Modifier.width(10.dp))

                    Text(
                        "שמירה לכניסה הבאה",
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(end = 6.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (loginError) {
                    Text("פרטי ההתחברות שגויים", color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        playStrongFeedback()

                        val savedUsername = sp.getString("username", "") ?: ""
                        val savedPassword = sp.getString("password", "") ?: ""
                        val credsOk = username.isNotBlank() &&
                                username == savedUsername && password == savedPassword

                        val coachOk = if (isCoach) {
                            val cc = coachCode.normalizeCoachCode()

                            // קורא קוד שנשמר בטופס הרישום
                            val spCode = sp.getString("coach_code", "")?.normalizeCoachCode().orEmpty()
                            val userSp = appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
                            val userCode = userSp.getString("coach_code", "")?.normalizeCoachCode().orEmpty()

                            // מאפשר גם קוד מ- CoachRegistry אם יש לך כאלה קבועים
                            val registryOk = runCatching { CoachRegistry.isValid(cc) }.getOrDefault(false)

                            val valid = cc.isNotBlank() && (cc == spCode || cc == userCode || registryOk)
                            coachCodeError = !valid

                            if (valid) {
                                sp.edit().putString("coach_code", cc).apply()
                                userSp.edit().putString("coach_code", cc).apply()

                                val coachName = runCatching { CoachRegistry.coachName(cc) }.getOrNull()
                                if (!coachName.isNullOrBlank()) {
                                    sp.edit().putString("coach_name", coachName).apply()
                                    userSp.edit().putString("coach_name", coachName).apply()
                                }
                            }
                            valid
                        } else true

                        if (credsOk && coachOk) {
                            loginError = false

                            if (rememberMe) {
                                sp.edit()
                                    .putBoolean("remember_me_login", true)
                                    .putString("remember_username", username)
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
                            sp.edit()
                                .putString("user_role", role)
                                .putBoolean("is_logged_in", true)
                                .apply()

                            appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
                                .edit()
                                .putString("user_role", role)
                                .putBoolean("is_logged_in", true)
                                .apply()

                            kmiPrefs.username = username
                            kmiPrefs.password = password

                            loginSucceeded = true
                        } else {
                            loginError = true
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
                    Text("התחבר")
                }

                TextButton(
                    onClick = { showRecoveryDialog = true },   // ← פותח דיאלוג מקומי
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = ((1 - fieldWidth) / 2f * 100).dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                ) {
                    Text("שכחתי סיסמה / שם משתמש", textDecoration = TextDecoration.Underline)
                }

                Spacer(Modifier.weight(1f))

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(start = 16.dp, end = 16.dp, bottom = 4.dp))
                )

                Text(
                    text = "❤️ פותח באהבה ע\"י יובל פולק ❤️",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (LocalConfiguration.current.screenWidthDp <= 360) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .navigationBarsPadding(),
                    textAlign = TextAlign.Center
                )
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
    var emailOrUser by rememberSaveable { mutableStateOf("") }
    var extraInfo by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "שחזור סיסמה / שם משתמש",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "מלא את הפרטים הבאים כדי שנוכל לאתר את המשתמש שלך:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = emailOrUser,
                    onValueChange = { emailOrUser = it },
                    label = { Text("אימייל או שם משתמש") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = extraInfo,
                    onValueChange = { extraInfo = it },
                    label = { Text("טלפון") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "בהמשך תוכל לחבר את זה לשליחת מייל / וואטסאפ למאמן או למנהל המערכת.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // כאן בעתיד תוסיף לוגיקה אמיתית (שרת, Firebase וכו')
                    onDismiss()
                }
            ) {
                Text("שלח בקשה")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("סגור")
            }
        }
    )
}
