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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import il.kmi.app.analytics.KmiDiagnostics
import il.kmi.app.screens.admin.AdminAccess
import il.kmi.app.ui.AppFontSize
import il.kmi.app.ui.DrawerBridge
import il.kmi.app.ui.LocalAppIconScale
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull


internal suspend fun verifyKmiCoachViaRest(
    uid: String,
    idToken: String,
    projectId: String
): Boolean {
    if (
        uid.isBlank() ||
        idToken.isBlank() ||
        projectId.isBlank()
    ) {
        return false
    }

    return try {
        kotlinx.coroutines.withContext(
            kotlinx.coroutines.Dispatchers.IO
        ) {
            val url = java.net.URL(
                "https://firestore.googleapis.com/v1/" +
                        "projects/$projectId/" +
                        "databases/(default)/documents/" +
                        "authorizedCoaches/$uid"
            )

            val connection =
                url.openConnection() as java.net.HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $idToken"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val status = connection.responseCode

                if (status != 200) {
                    android.util.Log.w(
                        "KMI_COACH_AUTH",
                        "Coach REST fallback: HTTP $status"
                    )
                    false
                } else {
                    val response =
                        connection.inputStream
                            .bufferedReader(Charsets.UTF_8)
                            .use { it.readText() }

                    val document =
                        org.json.JSONObject(response)

                    val expectedName =
                        "projects/$projectId/" +
                                "databases/(default)/documents/" +
                                "authorizedCoaches/$uid"

                    val fields =
                        document.optJSONObject("fields")

                    val active =
                        fields
                            ?.optJSONObject("active")
                            ?.optBoolean(
                                "booleanValue",
                                false
                            ) == true

                    val role =
                        fields
                            ?.optJSONObject("role")
                            ?.optString("stringValue")
                            .orEmpty()

                    val canOpenDrawer =
                        fields
                            ?.optJSONObject("canOpenCoachDrawer")
                            ?.optBoolean(
                                "booleanValue",
                                false
                            ) == true

                    val authorized =
                        document.optString("name") == expectedName &&
                                active &&
                                role.equals(
                                    "coach",
                                    ignoreCase = true
                                ) &&
                                canOpenDrawer

                    android.util.Log.d(
                        "KMI_COACH_AUTH",
                        "Coach REST verified: authorized=$authorized"
                    )

                    authorized
                }
            } finally {
                connection.disconnect()
            }
        }
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        android.util.Log.e(
            "KMI_COACH_AUTH",
            "Coach REST fallback failed",
            error
        )
        false
    }
}

@Composable
fun MainApp(
    sp: SharedPreferences,
    vm: KmiViewModel,
    startRoute: String? = null,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    fun cleanPrefString(value: Any?): String {
        val text = value
            ?.toString()
            ?.trim()
            .orEmpty()

        return text.takeIf { it.isNotBlank() && it != "null" }.orEmpty()
    }

    // ----- theme / global font size -----

    val initialFontSize = AppFontSize.fromStorageValue(
        cleanPrefString(kmiPrefs.fontSize)
            .ifBlank {
                sp.getString(
                    AppFontSize.PREFERENCE_KEY,
                    AppFontSize.MEDIUM.storageValue
                ) ?: AppFontSize.MEDIUM.storageValue
            }
    )

    /*
     * נשמר כ־String כדי ש־rememberSaveable יוכל לשחזר אותו
     * גם לאחר יצירה מחדש של ה־Activity.
     */
    var fontSizeValue by rememberSaveable {
        mutableStateOf(initialFontSize.storageValue)
    }

    /*
     * מנרמל ערכים ישנים או לא תקינים ומבטיח ששני מנגנוני
     * השמירה מחזיקים תמיד באותו ערך.
     */
    LaunchedEffect(Unit) {
        val normalizedFontSize =
            AppFontSize.fromStorageValue(fontSizeValue)

        fontSizeValue = normalizedFontSize.storageValue
        kmiPrefs.fontSize = normalizedFontSize.storageValue

        sp.edit()
            .putString(
                AppFontSize.PREFERENCE_KEY,
                normalizedFontSize.storageValue
            )
            .apply()
    }

    /*
     * מאפשר למסך ההגדרות לשנות את גודל הכתב בזמן אמת.
     * כאשר font_size משתנה, כל MainApp עובר recomposition.
     */
    DisposableEffect(sp) {
        val fontSizeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
                if (key == AppFontSize.PREFERENCE_KEY) {
                    val newFontSize = AppFontSize.fromStorageValue(
                        preferences.getString(
                            AppFontSize.PREFERENCE_KEY,
                            AppFontSize.MEDIUM.storageValue
                        )
                    )

                    fontSizeValue = newFontSize.storageValue
                    kmiPrefs.fontSize = newFontSize.storageValue
                }
            }

        sp.registerOnSharedPreferenceChangeListener(fontSizeListener)

        onDispose {
            sp.unregisterOnSharedPreferenceChangeListener(fontSizeListener)
        }
    }

    val selectedFontSize =
        AppFontSize.fromStorageValue(
            fontSizeValue
        )

    /*
     * עדכון ישיר שמגיע ממסך ההגדרות.
     *
     * SharedPreferences עדיין נשמר כמקור קבוע,
     * אך ה־state מתעדכן מיד כדי שכל Composition,
     * כולל דיאלוג העוזר, יקבל Density חדש.
     */
    val handleFontSizeChange: (String) -> Unit =
        { storageValue ->
            val normalizedSize =
                AppFontSize.fromStorageValue(
                    storageValue
                )

            fontSizeValue =
                normalizedSize.storageValue

            kmiPrefs.fontSize =
                normalizedSize.storageValue

            sp.edit()
                .putString(
                    AppFontSize.PREFERENCE_KEY,
                    normalizedSize.storageValue
                )
                .apply()
        }

    /*
     * שומרים על גודל הכתב שנבחר בהגדרות הנגישות של המכשיר,
     * ומכפילים אותו בבחירה הפנימית של האפליקציה.
     *
     * שימוש ב־LocalDensity גורם גם ל־fontSize קשיח המוגדר ב־sp
     * לקבל את הבחירה Small / Medium / Large.
     */
    val systemDensity = LocalDensity.current

    val appDensity = remember(
        systemDensity.density,
        systemDensity.fontScale,
        selectedFontSize
    ) {
        Density(
            density = systemDensity.density,
            fontScale = systemDensity.fontScale *
                    selectedFontSize.scaleFactor
        )
    }

    val rawThemePrefInitial = cleanPrefString(kmiPrefs.themeMode)
        .ifBlank {
            sp.getString("theme_mode", "") ?: ""
        }
        .ifBlank {
            "light"
        }

    val themePrefInitial = when (rawThemePrefInitial) {
        "dark" -> "dark"
        "light" -> "light"
        else -> "light"
    }

    // מצב נוכחי של מצב הנושא – נשמר ב־state
    var themeMode by rememberSaveable {
        mutableStateOf(themePrefInitial)
    }

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

    // callback אחד רשמי שמקבל מצב חדש מה־Settings
    val onThemeChange: (String) -> Unit = { mode ->
        themeMode = mode
        kmiPrefs.themeMode = mode

        sp.edit()
            .putString("theme_mode", mode)
            .apply()
    }

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

    val firebaseAuthForRoles = remember {
        com.google.firebase.auth.FirebaseAuth.getInstance()
    }

    var authenticatedUid by remember {
        mutableStateOf(firebaseAuthForRoles.currentUser?.uid)
    }

    var isAdmin by remember {
        mutableStateOf(false)
    }

    var isCoachAuthorized by remember {
        mutableStateOf(false)
    }

    /*
     * מעקב אחר זהות המשתמש המחובר.
     * אין להסתמך על currentRoute לזיהוי שינוי משתמש.
     */
    DisposableEffect(firebaseAuthForRoles) {
        val listener =
            com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
                val newUid = auth.currentUser?.uid

                if (authenticatedUid != newUid) {
                    isAdmin = false
                    isCoachAuthorized = false
                    authenticatedUid = newUid
                }
            }

        firebaseAuthForRoles.addAuthStateListener(listener)

        onDispose {
            firebaseAuthForRoles.removeAuthStateListener(listener)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    /*
     * הרשאות נבדקות:
     * 1. בעת התחברות או החלפת משתמש.
     * 2. בעת חזרה של האפליקציה למצב STARTED.
     *
     * מעבר בין מסכים אינו מפעיל בדיקה נוספת.
     * כשהאפליקציה יוצאת מ-STARTED, הבדיקה מתבטלת.
     */
    LaunchedEffect(authenticatedUid, lifecycleOwner) {
        val uid = authenticatedUid.orEmpty()

        isAdmin = false
        isCoachAuthorized = false

        if (uid.isBlank()) {
            return@LaunchedEffect
        }

        lifecycleOwner.lifecycle.repeatOnLifecycle(
            Lifecycle.State.STARTED
        ) {
            // אין להציג הרשאות ישנות בזמן בדיקה מחדש.
            isAdmin = false
            isCoachAuthorized = false

            if (firebaseAuthForRoles.currentUser?.uid != uid) {
                return@repeatOnLifecycle
            }

            isAdmin =
                AdminAccess.isCurrentUserAdmin() &&
                        firebaseAuthForRoles.currentUser?.uid == uid

            val firebaseApp =
                com.google.firebase.FirebaseApp.getInstance()

        val firebaseAuth = firebaseAuthForRoles

        val firestore =
            com.google.firebase.firestore.FirebaseFirestore.getInstance()

        val authTokenResult = try {
            firebaseAuth.currentUser
                ?.getIdToken(false)
                ?.await()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e(
                "KMI_COACH_AUTH",
                "Firebase Auth token inspection failed",
                error
            )
            null
        }

        val coachResult = try {
            Result.success(
                com.google.firebase.firestore.FirebaseFirestore
                    .getInstance()
                    .collection("authorizedCoaches")
                    .document(uid)
                    .get(
                        com.google.firebase.firestore.Source.SERVER
                    )
                    .await()
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e(
                "KMI_COACH_AUTH",
                "MainApp coach authorization read failed",
                error
            )
            Result.failure(error)
        }

        val coachDoc = coachResult.getOrNull()

        // אימות REST משותף:
        // מופעל רק כאשר SDK אינו מחזיר מסמך תקין.
        val restCoachAuthorized =
            if (coachDoc?.exists() == true) {
                false
            } else {
                val idToken =
                    authTokenResult?.token.orEmpty()

                val projectId =
                    firebaseApp.options.projectId.orEmpty()

                verifyKmiCoachViaRest(
                    uid = uid,
                    idToken = idToken,
                    projectId = projectId
                )
            }

        val sdkCoachAuthorized =
            coachDoc?.exists() == true &&
                    coachDoc.getBoolean("active") == true &&
                    coachDoc.getString("role")
                        .orEmpty()
                        .equals(
                            "coach",
                            ignoreCase = true
                        ) &&
                    coachDoc.getBoolean(
                        "canOpenCoachDrawer"
                    ) == true

        // מונע שימוש בתשובת בדיקה שהסתיימה לאחר החלפת משתמש.
        val sameAuthenticatedUser =
            firebaseAuth.currentUser?.uid == uid

        isCoachAuthorized =
            sameAuthenticatedUser &&
                    (sdkCoachAuthorized || restCoachAuthorized)

            android.util.Log.d(
                "KMI_COACH_AUTH",
                "Drawer authorization: " +
                        "sdk=$sdkCoachAuthorized " +
                        "rest=$restCoachAuthorized " +
                        "authorized=$isCoachAuthorized"
            )
        }
    }

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
        ctxForForumPush.getSharedPreferences(
            "kmi_forum_push",
            Context.MODE_PRIVATE
        )
    }

    val coachPushSp = remember(ctxForForumPush) {
        ctxForForumPush.getSharedPreferences(
            "kmi_settings",
            Context.MODE_PRIVATE
        )
    }

    var forumPushTick by remember {
        mutableStateOf(0)
    }

    var coachPushTick by remember {
        mutableStateOf(0)
    }

    DisposableEffect(forumPushSp, coachPushSp) {
        val forumListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "has_pending_forum_push") {
                    forumPushTick++
                }
            }

        val coachListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "coach_broadcast_open_on_tap") {
                    coachPushTick++
                }
            }

        forumPushSp.registerOnSharedPreferenceChangeListener(
            forumListener
        )

        coachPushSp.registerOnSharedPreferenceChangeListener(
            coachListener
        )

        onDispose {
            forumPushSp.unregisterOnSharedPreferenceChangeListener(
                forumListener
            )

            coachPushSp.unregisterOnSharedPreferenceChangeListener(
                coachListener
            )
        }
    }

    fun isRegistrationOrEntryRoute(
        route: String?
    ): Boolean {
        val cleanRoute =
            route.orEmpty()

        return cleanRoute == Route.Splash.route ||
                cleanRoute == Route.RegistrationLanding.route ||
                cleanRoute == Route.Registration.route ||
                cleanRoute.startsWith(
                    Route.NewUserTrainee.route
                ) ||
                cleanRoute == Route.ExistingUserTrainee.route ||
                cleanRoute == Route.ExistingUserCoach.route ||
                cleanRoute == "google_profile_completion"
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

    // פתיחת מרכז ההודעות רק בעקבות לחיצה על Push.
    LaunchedEffect(
        currentRoute,
        coachPushTick,
        isRegistrationRouteForDrawer,
        forumPushTick
    ) {
        if (currentRoute == null) {
            return@LaunchedEffect
        }

        val shouldOpenMessages =
            coachPushSp.getBoolean(
                "coach_broadcast_open_on_tap",
                false
            )

        if (!shouldOpenMessages) {
            return@LaunchedEffect
        }

        // אין לעקוף מסכי התחברות או רישום.
        if (isRegistrationRouteForDrawer) {
            return@LaunchedEffect
        }

        val currentUser =
            com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser

        if (
            currentUser == null ||
            currentUser.isAnonymous ||
            currentUser.uid.isBlank()
        ) {
            return@LaunchedEffect
        }

        // משאירים עדיפות לניווט פורום שכבר קיים.
        if (
            forumPushSp.getBoolean(
                "has_pending_forum_push",
                false
            )
        ) {
            return@LaunchedEffect
        }

        // מנקים את הבקשה לפני הניווט למניעת פתיחה חוזרת.
        coachPushSp.edit()
            .putBoolean(
                "coach_broadcast_open_on_tap",
                false
            )
            .apply()

        if (currentRoute != Route.MessageCenter.route) {
            nav.navigate(Route.MessageCenter.route) {
                launchSingleTop = true
                restoreState = false
            }
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

    CompositionLocalProvider(
        LocalDensity provides appDensity,
        LocalAppIconScale provides selectedFontSize.scaleFactor
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }
        ) {
            val isEnglish =
                appLanguage == il.kmi.shared.localization.AppLanguage.ENGLISH
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
                            },
                            onOpenProgress = {
                                nav.navigate(Route.Progress.route) {
                                    launchSingleTop = true
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
                                onFontSizeChange =
                                    handleFontSizeChange,
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

                    /*
                     * ModalNavigationDrawer צריך להשלים מדידה של רוחב המגירה
                     * לפני שמציגים את התוכן שלה. אחרת התוכן עלול להופיע
                     * בפריים הראשון במיקום הפתוח.
                     */
                    var drawerContentReady by remember {
                        mutableStateOf(false)
                    }

                    LaunchedEffect(drawerState) {
                        drawerContentReady = false
                        drawerState.snapTo(DrawerValue.Closed)

                        // פריים ראשון: בניית ModalNavigationDrawer.
                        withFrameNanos { }

                        // פריים שני: חישוב העוגן הסגור לפי רוחב המגירה.
                        withFrameNanos { }

                        drawerState.snapTo(DrawerValue.Closed)
                        drawerContentReady = true

                        DrawerBridge.register(
                            onOpenDrawer = {
                                if (!drawerContentReady) {
                                    return@register
                                }

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
                            },
                            onOpenProgress = {
                                nav.navigate(Route.Progress.route) {
                                    launchSingleTop = true
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
                                    modifier = Modifier
                                        .fillMaxWidth(0.81f)
                                        .alpha(
                                            if (drawerContentReady) {
                                                1f
                                            } else {
                                                0f
                                            }
                                        )
                                ) {
                                    /*
                                     * משאירים את תוכן המגירה מחובר ל-Composition גם
                                     * כשהמגירה סגורה. כך DrawerVoiceActionsBridge
                                     * נשאר זמין תמיד לפקודות קוליות.
                                     *
                                     * המגירה עצמה עדיין מוסתרת ומוצגת רק באמצעות
                                     * drawerState; אין שינוי בהתנהגות החזותית שלה.
                                     */
                                    val ctxInner = LocalContext.current
                                    val spUser = remember {
                                        ctxInner.getSharedPreferences(
                                            "kmi_user",
                                            Context.MODE_PRIVATE
                                        )
                                    }
                                    val isCoach =
                                        isCoachAuthorized

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
                                            nav.navigate(
                                                Route.NewUserTrainee.route +
                                                        "?step=profile"
                                            ) {
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

                                        // הרשאת אדמין נקבעת לפי admins/{uid}.enabled ב-Firestore
                                        isAdmin = isAdmin,
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
                                                val tokenRemoved =
                                                    runCatching {
                                                        withTimeoutOrNull(5000L) {
                                                            FcmTokenManager
                                                                .removeCurrentDeviceTokenBeforeSignOut()
                                                        } == true
                                                    }.getOrDefault(false)

                                                if (!tokenRemoved) {
                                                    android.util.Log.w(
                                                        "KMI_AUTH",
                                                        "FCM token cleanup was not confirmed before sign-out"
                                                    )
                                                }

                                                com.google.firebase.auth.FirebaseAuth
                                                    .getInstance()
                                                    .signOut()

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
                                    onFontSizeChange =
                                        handleFontSizeChange,
                                    onOpenDrawer = {
                                        if (drawerContentReady) {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        }
                                    },
                                    startDestination =
                                        resolvedStartDestination
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}