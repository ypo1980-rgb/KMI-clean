@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
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
import androidx.navigation.compose.currentBackStackEntryAsState
import il.kmi.app.analytics.KmiDiagnostics
import il.kmi.app.ui.DrawerBridge
import kotlinx.coroutines.launch

// ---------- טיפוגרפיה עם סקייל ----------
private fun Typography.scaled(scale: Float): Typography {
    fun ts(t: androidx.compose.ui.text.TextStyle) =
        t.copy(fontSize = t.fontSize * scale, lineHeight = t.lineHeight * scale)

    return Typography(
        displayLarge = ts(displayLarge),
        displayMedium = ts(displayMedium),
        displaySmall = ts(displaySmall),
        headlineLarge = ts(headlineLarge),
        headlineMedium = ts(headlineMedium),
        headlineSmall = ts(headlineSmall),
        titleLarge = ts(titleLarge),
        titleMedium = ts(titleMedium),
        titleSmall = ts(titleSmall),
        bodyLarge = ts(bodyLarge),
        bodyMedium = ts(bodyMedium),
        bodySmall = ts(bodySmall),
        labelLarge = ts(labelLarge),
        labelMedium = ts(labelMedium),
        labelSmall = ts(labelSmall),
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
    val rawThemePrefInitial = kmiPrefs.themeMode
        .ifBlank { sp.getString("theme_mode", "") ?: "" }
        .ifBlank { "light" }

    val themePrefInitial = when (rawThemePrefInitial) {
        "dark" -> "dark"
        "light" -> "light"
        else -> "light"
    }

    // מצב נוכחי של מצב הנושא – נשמר ב־state
    var themeMode by rememberSaveable { mutableStateOf(themePrefInitial) }

    // מנרמל ערכים ישנים כמו "system" לברירת המחדל החדשה: light
    LaunchedEffect(Unit) {
        if (
            kmiPrefs.themeMode != themePrefInitial ||
            sp.getString("theme_mode", "") != themePrefInitial
        ) {
            kmiPrefs.themeMode = themePrefInitial
            sp.edit()
                .putString("theme_mode", themePrefInitial)
                .apply()
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

    // משתמשים ב-themeMode הדינמי.
    // ברירת המחדל היא light; רק dark מפורש מפעיל מצב כהה.
    val darkTheme = when (themeMode) {
        "dark" -> true
        "system" -> isSystemInDarkTheme()
        else -> false
    }

    // ליתר ביטחון
    LaunchedEffect(Unit) { vm.clearSelectedBelt() }

    val nav = rememberNavController()

    val resolvedStartDestination = remember(startRoute) {
        startRoute ?: Route.Splash.route
    }

    val mainBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = mainBackStackEntry?.destination?.route
    val activeRouteForDrawer = currentRoute ?: resolvedStartDestination

    val ctxForDiagnostics = LocalContext.current.applicationContext

    LaunchedEffect(currentRoute) {
        val route = currentRoute.orEmpty()

        if (route.isNotBlank()) {
            KmiDiagnostics.trackScreen(
                context = ctxForDiagnostics,
                screenName = route,
                route = route
            )
        }
    }

    val ctxForForumPush = LocalContext.current.applicationContext
    val forumPushSp = remember(ctxForForumPush) {
        ctxForForumPush.getSharedPreferences("kmi_forum_push", Context.MODE_PRIVATE)
    }

    var forumPushTick by remember {
        mutableStateOf(0)
    }

    DisposableEffect(forumPushSp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "has_pending_forum_push") {
                forumPushTick++
            }
        }

        forumPushSp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            forumPushSp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun isRegistrationOrEntryRoute(route: String?): Boolean {
        return route == Route.RegistrationLanding.route ||
                route == Route.Registration.route ||
                route == Route.NewUserTrainee.route ||
                route == Route.NewUserCoach.route ||
                route == Route.ExistingUserTrainee.route ||
                route == Route.ExistingUserCoach.route ||
                route == "google_profile_completion"
    }

    val isRegistrationRouteForDrawer =
        isRegistrationOrEntryRoute(activeRouteForDrawer)

    LaunchedEffect(currentRoute, forumPushTick, isRegistrationRouteForDrawer) {
        val routeReady = currentRoute != null
        val hasPendingForumPush =
            forumPushSp.getBoolean("has_pending_forum_push", false)

        if (!routeReady || !hasPendingForumPush) {
            return@LaunchedEffect
        }

        // במסכי רישום/כניסה לא דוחפים את המשתמש בכוח לפורום.
        // אחרי כניסה לאפליקציה, ה-effect ירוץ שוב ויפתח את הפורום.
        if (isRegistrationRouteForDrawer) {
            return@LaunchedEffect
        }

        forumPushSp.edit()
            .putBoolean("has_pending_forum_push", false)
            .apply()

        nav.navigate(Route.Forum.route) {
            launchSingleTop = true
        }
    }

    // להדליק ניווט מפוצל כברירת מחדל
    LaunchedEffect(Unit) {
        if (!sp.contains("nav_split_enabled")) {
            sp.edit().putBoolean("nav_split_enabled", true).apply()
        }
    }

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

                appLanguage = newStoredLanguage
            }
        }

        languagePrefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            languagePrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val onLanguageChanged: (il.kmi.shared.localization.AppLanguage) -> Unit = { newLanguage ->
        val manager = il.kmi.shared.localization.AppLanguageManager(ctxForLanguage)

        appLanguage = newLanguage
        manager.setLanguage(newLanguage)
    }

    MaterialTheme(
        typography = typography,
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    ) {
        val isEnglish = appLanguage == il.kmi.shared.localization.AppLanguage.ENGLISH
        val layoutDirection = if (isEnglish) {
            androidx.compose.ui.unit.LayoutDirection.Ltr
        } else {
            androidx.compose.ui.unit.LayoutDirection.Rtl
        }

        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
        ) {
            if (isRegistrationRouteForDrawer) {
                // במסכי כניסה / רישום אין Drawer בכלל.
                // כך אין מצב שהסרגל "קופץ" לבית אחרי מעבר מהרישום.
                LaunchedEffect(Unit) {
                    DrawerBridge.register(
                        onOpenDrawer = {
                            // לא עושים כלום במסכי רישום
                        },
                        onOpenSettings = {
                            nav.navigate(Route.Settings.route) {
                                launchSingleTop = true
                            }
                        },
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

                CompositionLocalProvider(
                    il.kmi.app.ui.LocalAppDrawerState provides null
                ) {
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
                                // במסכי רישום לא פותחים Drawer
                            },
                            startDestination = resolvedStartDestination
                        )
                    }
                }
            } else {
                // במסכי האפליקציה הרגילים ה-Drawer עובד בצורה רגילה ונקייה.
                val drawerState = rememberDrawerState(DrawerValue.Closed)

                LaunchedEffect(drawerState) {
                    DrawerBridge.register(
                        onOpenDrawer = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onOpenSettings = {
                            nav.navigate(Route.Settings.route) {
                                launchSingleTop = true
                            }
                        },
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

                CompositionLocalProvider(
                    il.kmi.app.ui.LocalAppDrawerState provides drawerState
                ) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = true,
                        scrimColor = Color.Black.copy(alpha = 0.35f),
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = Color.Transparent,
                                modifier = Modifier.fillMaxWidth(0.86f)
                            ) {
                                val shouldRenderDrawerContent =
                                    drawerState.currentValue == DrawerValue.Open ||
                                            drawerState.targetValue == DrawerValue.Open

                                if (!shouldRenderDrawerContent) {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    return@ModalDrawerSheet
                                }

                                val ctxInner = LocalContext.current
                                val spUser = remember {
                                    ctxInner.getSharedPreferences(
                                        "kmi_user",
                                        Context.MODE_PRIVATE
                                    )
                                }
                                val roleState = remember {
                                    mutableStateOf(spUser.getString("user_role", "trainee"))
                                }

                                DisposableEffect(spUser) {
                                    val l =
                                        SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
                                            if (k == "user_role") {
                                                roleState.value =
                                                    spUser.getString("user_role", "trainee")
                                            }
                                        }
                                    spUser.registerOnSharedPreferenceChangeListener(l)
                                    onDispose {
                                        spUser.unregisterOnSharedPreferenceChangeListener(l)
                                    }
                                }

                                val isCoach = roleState.value.equals("coach", true)

                                il.kmi.app.screens.drawer.AppDrawerContent(
                                    isEnglish = isEnglish,
                                    onLanguageChanged = { newLang ->
                                        onLanguageChanged(newLang)
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    },

                                    onOpenAboutNetwork = {
                                        nav.navigate(Route.AboutNetwork.route)
                                    },
                                    onOpenAboutMethod = {
                                        nav.navigate(Route.AboutMethod.route)
                                    },
                                    onOpenSubscriptions = {
                                        nav.navigate(Route.Subscription.route)
                                    },
                                    onOpenMembershipPayment = {
                                        nav.navigate(Route.MembershipPayment.route)
                                    },
                                    onOpenForum = {
                                        nav.navigate(Route.Forum.route)
                                    },
                                    onOpenContactUs = {
                                        nav.navigate(Route.ContactUs.route)
                                    },
                                    onOpenAboutAvi = {
                                        nav.navigate(Route.AboutAvi.route)
                                    },
                                    onOpenAboutNetworkCoaches = {
                                        nav.navigate(Route.AboutNetworkCoaches.route)
                                    },
                                    onOpenMyProfile = {
                                        nav.navigate(Route.MyProfile.route)
                                    },

                                    // עריכת פרופיל — פותח את אותו טופס רישום,
                                    // אבל במצב עריכה: בסיום חוזרים למסך הקודם ולא למסך הטעינה הדינמי.
                                    onOpenEditProfile = {
                                        nav.navigate(Route.NewUserTrainee.route + "?step=profile&skipOtp=false") {
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    },

                                    onOpenMonthlyCalendar = {
                                        nav.navigate(Route.MonthlyCalendar.route)
                                    },
                                    onOpenAboutItzik = {
                                        nav.navigate(Route.AboutItzik.route)
                                    },
                                    onOpenRateUs = {
                                        nav.navigate(Route.RateUs.route)
                                    },
                                    onOpenTrainingSummary = {
                                        nav.navigate(Route.TrainingSummary.route) {
                                            launchSingleTop = true
                                        }
                                    },

                                    // מאמן
                                    isCoach = isCoach,
                                    onOpenCoachAttendance = {
                                        fun cleanAttendancePart(raw: String?): String =
                                            raw.orEmpty()
                                                .replace(" • ", ",")
                                                .replace("|", ",")
                                                .replace("\n", ",")
                                                .split(',', ';', '；')
                                                .map {
                                                    it.trim()
                                                        .replace('־', '-')
                                                        .replace('–', '-')
                                                        .replace('—', '-')
                                                        .replace(Regex("\\s+"), " ")
                                                }
                                                .firstOrNull { it.isNotBlank() }
                                                .orEmpty()

                                        val branch = cleanAttendancePart(
                                            spUser.getString("active_branch", null)
                                                ?: spUser.getString("branch", null)
                                                ?: spUser.getString("branches", null)
                                                ?: spUser.getString("coach_branch", null)
                                                ?: spUser.getString("coach_branches", null)
                                                ?: spUser.getString("coachBranches", null)
                                        )

                                        val groupKey = cleanAttendancePart(
                                            spUser.getString("active_group", null)
                                                ?: spUser.getString("groupKey", null)
                                                ?: spUser.getString("group", null)
                                                ?: spUser.getString("age_group", null)
                                                ?: spUser.getString("age_groups", null)
                                                ?: spUser.getString("groups", null)
                                                ?: spUser.getString("coach_group", null)
                                                ?: spUser.getString("coach_groups", null)
                                                ?: spUser.getString("coachGroups", null)
                                        )

                                        if (branch.isBlank() || groupKey.isBlank()) {
                                            Toast.makeText(
                                                ctxInner,
                                                if (isEnglish) {
                                                    "Missing branch or group for attendance."
                                                } else {
                                                    "חסר סניף או קבוצה לפתיחת סימון נוכחות."
                                                },
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            runCatching {
                                                nav.navigate(
                                                    "attendance/mark/${Uri.encode(branch)}/${
                                                        Uri.encode(
                                                            groupKey
                                                        )
                                                    }"
                                                ) {
                                                    launchSingleTop = true
                                                }
                                            }.onFailure {
                                                Toast.makeText(
                                                    ctxInner,
                                                    if (isEnglish) {
                                                        "Unable to open attendance."
                                                    } else {
                                                        "לא ניתן לפתוח סימון נוכחות."
                                                    },
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    },
                                    onOpenCoachPaymentsReport = {
                                        nav.navigate(Route.PaymentsReport.route)
                                    },
                                    onOpenCoachBroadcast = {
                                        nav.navigate(Route.CoachBroadcast.route)
                                    },
                                    onOpenCoachTrainees = {
                                        nav.navigate("coach/trainees")
                                    },
                                    onOpenCoachInternalExam = {
                                        nav.navigate(Route.InternalExam.route)
                                    },

                                    // אדמין – ייקבע רק לפי Firestore בהמשך
                                    isAdmin = false,
                                    onOpenAdminUsers = {
                                        nav.navigate(Route.AdminUsers.route)
                                    },
                                    onOpenAdminDiagnostics = {
                                        nav.navigate("admin_diagnostics")
                                    },

                                    onClose = {
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    },

                                    onLogout = {
                                        scope.launch {
                                            runCatching {
                                                com.google.firebase.auth.FirebaseAuth
                                                    .getInstance()
                                                    .signOut()
                                            }

                                            // ✅ לא עושים clear מלא.
                                            // clear() מוחק גם שפה, שם, חגורה ופרטי פרופיל,
                                            // ואז האפליקציה מתנהגת כאילו זו כניסה ראשונה.
                                            spUser.edit()
                                                .remove("uid")
                                                .remove("user_uid")
                                                .remove("firebase_uid")
                                                .remove("auth_uid")
                                                .remove("isLoggedIn")
                                                .remove("logged_in")
                                                .remove("login_token")
                                                .remove("session_token")
                                                .remove("google_id_token")
                                                .remove("id_token")
                                                .remove("access_token")
                                                .remove("refresh_token")
                                                .apply()

                                            ctxInner.getSharedPreferences(
                                                "kmi_settings",
                                                Context.MODE_PRIVATE
                                            )
                                                .edit()
                                                .remove("app_lock_last_success")
                                                .apply()

                                            drawerState.close()
                                            kotlinx.coroutines.delay(150)

                                            val act = (ctxInner as? Activity)
                                                ?: generateSequence(ctxInner) {
                                                    (it as? ContextWrapper)?.baseContext
                                                }
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
                        BackHandler(enabled = drawerState.isOpen) {
                            scope.launch {
                                drawerState.close()
                            }
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
                                    scope.launch {
                                        drawerState.open()
                                    }
                                },
                                startDestination = resolvedStartDestination
                            )
                        }
                    }
                }
            }
        }
    }
}