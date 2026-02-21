@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import il.kmi.app.ui.DrawerBridge
import kotlinx.coroutines.launch

// ---------- טיפוגרפיה עם סקייל ----------
private fun Typography.scaled(scale: Float): Typography {
    fun ts(t: androidx.compose.ui.text.TextStyle) =
        t.copy(fontSize = t.fontSize * scale, lineHeight = t.lineHeight * scale)

    return Typography(
        displayLarge = ts(displayLarge), displayMedium = ts(displayMedium), displaySmall = ts(displaySmall),
        headlineLarge = ts(headlineLarge), headlineMedium = ts(headlineMedium), headlineSmall = ts(headlineSmall),
        titleLarge = ts(titleLarge), titleMedium = ts(titleMedium), titleSmall = ts(titleSmall),
        bodyLarge = ts(bodyLarge), bodyMedium = ts(bodyMedium), bodySmall = ts(bodySmall),
        labelLarge = ts(labelLarge), labelMedium = ts(labelMedium), labelSmall = ts(labelSmall),
    )
}

@Composable
fun MainApp(
    sp: SharedPreferences,
    vm: KmiViewModel,
    startRoute: String? = null,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // ----- theme / font scale -----
    val fontPref = kmiPrefs.fontSize.ifBlank {
        sp.getString("font_size", "medium") ?: "medium"
    }
    val themePrefInitial = kmiPrefs.themeMode.ifBlank {
        sp.getString("theme_mode", "system") ?: "system"
    }

    // מצב נוכחי של מצב הנושא (בהיר/כהה/לפי מערכת) – נשמר ב־state
    var themeMode by rememberSaveable { mutableStateOf(themePrefInitial) }

    // אם בקובץ המשותף לא נשמר כלום – נשמור את הערך הראשוני
    LaunchedEffect(Unit) {
        if (kmiPrefs.themeMode.isBlank()) {
            kmiPrefs.themeMode = themeMode
            sp.edit().putString("theme_mode", themeMode).apply()
        }
    }

    // callback אחד רשמי שמקבל מצב חדש מה-Settings
    val onThemeChange: (String) -> Unit = { mode ->
        themeMode = mode
        kmiPrefs.themeMode = mode
        sp.edit().putString("theme_mode", mode).apply()
    }

    val stepScale = when (fontPref) {
        "small" -> 0.90f
        "large" -> 1.15f
        else -> 1.00f
    }
    val sliderScaleRaw = kmiPrefs.fontScaleString.toFloatOrNull()
        ?: sp.getFloat("font_scale", 1.0f)
    val sliderScale = sliderScaleRaw.coerceIn(0.80f, 1.40f)

    LaunchedEffect(Unit) {
        if (kmiPrefs.fontScaleString.isBlank()) {
            kmiPrefs.fontScaleString = sp.getFloat("font_scale", 1.0f).toString()
        }
    }
    DisposableEffect(sp) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (k == "font_scale") {
                kmiPrefs.fontScaleString = sp.getFloat("font_scale", 1.0f).toString()
            }
        }
        sp.registerOnSharedPreferenceChangeListener(l)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(l) }
    }

    val totalScale = (stepScale * sliderScale).coerceIn(0.80f, 1.40f)
    val typography = Typography().scaled(totalScale)

    // משתמשים ב-themeMode הדינמי
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    // ליתר ביטחון
    LaunchedEffect(Unit) { vm.clearSelectedBelt() }

    val nav = rememberNavController()
    val useNewNav = true

    // להדליק ניווט מפוצל כברירת מחדל
    LaunchedEffect(Unit) {
        if (!sp.contains("nav_split_enabled")) {
            sp.edit().putBoolean("nav_split_enabled", true).apply()
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // רישום הגשר של המגירה
    LaunchedEffect(Unit) {
        DrawerBridge.register(
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onOpenSettings = { nav.navigate(Route.Settings.route) },
            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }

    // ✅ TTS אחיד לכל האפליקציה
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        // הכל ענן (אנושי) – לא מאתחלים TTS מקומי בכלל
        // KmiTtsManager.init(ctx)
    }

    MaterialTheme(
        typography = typography,
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    ) {
    // ❌ הוסר AccessibilityPanelHost (הוסר קוד נגישות + state)
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            scrimColor = Color.Black.copy(alpha = 0.35f),
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color.Transparent,
                    drawerContentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(0.82f)
                ) {
                    val ctxInner = LocalContext.current
                    val spUser = remember {
                        ctxInner.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
                    }

                    // נעקוב אחרי תפקיד המשתמש כדי להציג פריטי מאמן
                    val roleState = remember {
                        mutableStateOf(spUser.getString("user_role", "trainee"))
                    }
                    DisposableEffect(spUser) {
                        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
                            if (k == "user_role") {
                                roleState.value = spUser.getString("user_role", "trainee")
                            }
                        }
                        spUser.registerOnSharedPreferenceChangeListener(l)
                        onDispose { spUser.unregisterOnSharedPreferenceChangeListener(l) }
                    }
                    val isCoach = roleState.value.equals("coach", true)

                    il.kmi.app.screens.drawer.AppDrawerContent(
                        onOpenAboutNetwork = { nav.navigate(Route.AboutNetwork.route) },
                        onOpenAboutMethod = { nav.navigate(Route.AboutMethod.route) },
                        onOpenSubscriptions = { nav.navigate(Route.Subscription.route) },
                        onOpenForum = { nav.navigate(Route.Forum.route) },
                        onOpenAboutAvi = { nav.navigate(Route.AboutAvi.route) },
                        onOpenMyProfile = { nav.navigate(Route.MyProfile.route) },
                        onOpenMonthlyCalendar = { nav.navigate(Route.MonthlyCalendar.route) },
                        onOpenAboutItzik = { nav.navigate(Route.AboutItzik.route) },
                        onOpenRateUs = { nav.navigate(Route.RateUs.route) },
                        onOpenTrainingSummary = {
                            nav.navigate(Route.TrainingSummary.route) {
                                launchSingleTop = true
                            }
                        },

                        // מאמן
                        isCoach = isCoach,
                        onOpenCoachAttendance = { nav.navigate("attendance") },
                        onOpenCoachBroadcast = { nav.navigate(Route.CoachBroadcast.route) },
                        onOpenCoachTrainees = { nav.navigate("coach/trainees") },
                        onOpenCoachInternalExam = {
                            nav.navigate(Route.InternalExam.route)
                        },

                        // 🔐 אדמין – ייקבע *רק* לפי Firestore (admins/{uid}.enabled)
                        isAdmin = false,
                        onOpenAdminUsers = {
                            nav.navigate(Route.AdminUsers.route)
                        },

                        onClose = { scope.launch { drawerState.close() } },

                        onLogout = {
                            scope.launch {
                                runCatching {
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                }
                                spUser.edit().clear().apply()
                                ctxInner.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
                                    .edit()
                                    .remove("app_lock_last_success")
                                    .apply()

                                drawerState.close()
                                kotlinx.coroutines.delay(150)

                                val act = (ctxInner as? Activity)
                                    ?: generateSequence(ctxInner) { (it as? ContextWrapper)?.baseContext }
                                        .filterIsInstance<Activity>()
                                        .firstOrNull()

                                act?.let {
                                    it.moveTaskToBack(true)
                                    it.finishAffinity()
                                    it.finishAndRemoveTask()
                                }
                            }
                        }
                    )
                }
            }
        ) {
            // קודם סוגרים מגירה לפני Back
            BackHandler(enabled = drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                il.kmi.app.navigation.MainNavHost(
                    nav = nav,
                    vm = vm,
                    sp = sp,
                    kmiPrefs = kmiPrefs,
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    startDestination = Route.Intro.route
                )
            }        }
    }
}