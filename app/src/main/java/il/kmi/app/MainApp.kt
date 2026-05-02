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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
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

    val ctxForLanguage = LocalContext.current

    // ✅ מקור אמת יחיד לשפת האפליקציה וה-Drawer.
    // לא מחזיקים state נוסף ל-Drawer כדי שלא יהיה פער בין עברית לאנגלית.
    var appLanguage by remember {
        mutableStateOf(
            il.kmi.shared.localization.AppLanguageManager(ctxForLanguage)
                .getCurrentLanguage()
        )
    }

    // ✅ מאזין לשינויי שפה גם אם הם בוצעו ממקום אחר באפליקציה:
    // KmiTopBar / Settings / מסך פתיחה / כל מקום שקורא ל-AppLanguageManager.setLanguage(...)
    DisposableEffect(ctxForLanguage) {
        val languagePrefs = ctxForLanguage.applicationContext.getSharedPreferences(
            "kmi_language_prefs",
            Context.MODE_PRIVATE
        )

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "app_language") {
                val newStoredLanguage =
                    il.kmi.shared.localization.AppLanguageManager(ctxForLanguage)
                        .getCurrentLanguage()

                android.util.Log.e(
                    "KMI_LANG",
                    "MAIN_PREF_LISTENER key=$key newStoredLanguage=$newStoredLanguage oldState=$appLanguage"
                )

                appLanguage = newStoredLanguage
            }
        }

        languagePrefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            languagePrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

// ✅ פונקציה מרכזית לעדכון שפה - מקור אמת יחיד
    val onLanguageChanged: (il.kmi.shared.localization.AppLanguage) -> Unit = { newLanguage ->
        val beforeState = appLanguage
        val manager = il.kmi.shared.localization.AppLanguageManager(ctxForLanguage)

        // קודם מעדכנים State כדי שה-Drawer יקבל מיד isEnglish נכון.
        appLanguage = newLanguage

        // אחר כך שומרים בפועל.
        manager.setLanguage(newLanguage)

        val storedAfter = manager.getCurrentLanguage()

        android.util.Log.e(
            "KMI_LANG",
            "MAIN_onLanguageChanged requested=$newLanguage beforeState=$beforeState storedAfter=$storedAfter finalState=$appLanguage"
        )
    }

    // ✅ פתיחת Drawer עם לוגים — בלי לדרוס את appLanguage מה-Manager.
    // חשוב:
    // אם AppLanguageManager מחזיר ערך ישן, אסור לתת לו להחזיר את ה-Drawer לשפה הלא נכונה.
    fun openDrawerAfterLanguageSync(reason: String) {
        val storedLanguage =
            il.kmi.shared.localization.AppLanguageManager(ctxForLanguage)
                .getCurrentLanguage()

        val beforeState = appLanguage
        val beforeIsEnglish =
            beforeState == il.kmi.shared.localization.AppLanguage.ENGLISH
        val storedIsEnglish =
            storedLanguage == il.kmi.shared.localization.AppLanguage.ENGLISH

        android.util.Log.e(
            "KMI_LANG",
            "open[$reason] BEFORE state=$beforeState stateIsEnglish=$beforeIsEnglish stored=$storedLanguage storedIsEnglish=$storedIsEnglish drawerIsOpen=${drawerState.isOpen}"
        )

        if (appLanguage != storedLanguage) {
            android.util.Log.e(
                "KMI_LANG",
                "open[$reason] WARNING_STATE_AND_STORE_DIFFER state=$appLanguage stored=$storedLanguage - keeping state"
            )
        } else {
            android.util.Log.e(
                "KMI_LANG",
                "open[$reason] STATE_AND_STORE_MATCH state=$appLanguage"
            )
        }

        scope.launch {
            kotlinx.coroutines.delay(80)

            val latestStored =
                il.kmi.shared.localization.AppLanguageManager(ctxForLanguage)
                    .getCurrentLanguage()

            val latestState = appLanguage
            val latestIsEnglish =
                latestState == il.kmi.shared.localization.AppLanguage.ENGLISH

            android.util.Log.e(
                "KMI_LANG",
                "open[$reason] JUST_BEFORE_OPEN state=$latestState stateIsEnglish=$latestIsEnglish stored=$latestStored"
            )

            drawerState.open()

            android.util.Log.e(
                "KMI_LANG",
                "open[$reason] AFTER_OPEN drawerIsOpen=${drawerState.isOpen}"
            )
        }
    }

    LaunchedEffect(appLanguage) {
        android.util.Log.e(
            "KMI_LANG",
            "RECOMPOSE_LANGUAGE appLanguage=$appLanguage isEnglish=${appLanguage == il.kmi.shared.localization.AppLanguage.ENGLISH}"
        )
    }

    LaunchedEffect(Unit) {
        DrawerBridge.register(
            onOpenDrawer = {
                openDrawerAfterLanguageSync("DrawerBridge.onOpenDrawer")
            },
            onOpenSettings = {
                nav.navigate(Route.Settings.route) { launchSingleTop = true }
            },
            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }

    MaterialTheme(
        typography = typography,
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    ) {
        val isEnglish = appLanguage == il.kmi.shared.localization.AppLanguage.ENGLISH
        val layoutDirection = if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
        else androidx.compose.ui.unit.LayoutDirection.Rtl

        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
            CompositionLocalProvider(il.kmi.app.ui.LocalAppDrawerState provides drawerState) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    scrimColor = Color.Black.copy(alpha = 0.35f),
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color.Transparent,
                            modifier = Modifier.fillMaxWidth(0.86f)
                        ) {
                            val ctxInner = LocalContext.current
                            val spUser = remember { ctxInner.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
                            val roleState = remember { mutableStateOf(spUser.getString("user_role", "trainee")) }

                            DisposableEffect(spUser) {
                                val l = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
                                    if (k == "user_role") roleState.value = spUser.getString("user_role", "trainee")
                                }
                                spUser.registerOnSharedPreferenceChangeListener(l)
                                onDispose { spUser.unregisterOnSharedPreferenceChangeListener(l) }
                            }

                            val isCoach = roleState.value.equals("coach", true)

                            il.kmi.app.screens.drawer.AppDrawerContent(
                                isEnglish = isEnglish,
                                onLanguageChanged = { newLang ->
                                    onLanguageChanged(newLang)
                                    scope.launch { drawerState.close() }
                                },

                                onOpenAboutNetwork = { nav.navigate(Route.AboutNetwork.route) },
                                onOpenAboutMethod = { nav.navigate(Route.AboutMethod.route) },
                                onOpenSubscriptions = { nav.navigate(Route.Subscription.route) },
                                onOpenMembershipPayment = { nav.navigate(Route.MembershipPayment.route) },
                                onOpenForum = { nav.navigate(Route.Forum.route) },
                                onOpenContactUs = { nav.navigate(Route.ContactUs.route) },
                                onOpenAboutAvi = { nav.navigate(Route.AboutAvi.route) },
                                onOpenMyProfile = { nav.navigate(Route.MyProfile.route) },

                                // ✅ עריכת פרופיל — אותו מסך שהיה נפתח מכפתור "ערוך פרטים" בהגדרות
                                onOpenEditProfile = {
                                    nav.navigate("google_profile_completion") {
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                },

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
                        onOpenCoachPaymentsReport = { nav.navigate(Route.PaymentsReport.route) },
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
                            onOpenDrawer = {
                                // ✅ פתיחת Drawer דרך פונקציית האבחון המרכזית.
                                // היא מסנכרנת שפה, כותבת לוגים, ממתינה קצר,
                                // ואז פותחת את הסרגל.
                                openDrawerAfterLanguageSync("MainNavHost.onOpenDrawer")
                            },

                            startDestination = Route.Intro.route
                        )
                    }
                }
            }
        }
    }
}