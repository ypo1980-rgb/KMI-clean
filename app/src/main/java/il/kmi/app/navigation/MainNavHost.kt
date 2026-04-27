package il.kmi.app.navigation

import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Composable
import il.kmi.app.screens.SmsVerifyScreen
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.IntroScreen
import il.kmi.app.screens.registration.RegistrationNavHost
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import il.kmi.app.screens.MyProfileScreen
import il.kmi.app.screens.PhoneAuthGateScreen
import il.kmi.app.screens.RateUsScreen
import il.kmi.app.ui.DrawerBridge
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import il.kmi.app.security.PinLockGate
import il.kmi.shared.prefs.KmiPrefs
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavType
import androidx.navigation.navArgument
import il.kmi.app.free_sessions.ui.FreeSessionsScreen
import il.kmi.app.free_sessions.ui.navigation.FreeSessionsRoute
import il.kmi.app.ui.assistant.ui.AiAssistantDialog
import il.kmi.app.ui.WakeWordManager
import il.kmi.app.ui.assistant.ui.VoiceNavCommand
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import il.kmi.app.screens.ContactUsScreen
import il.kmi.app.screens.SubTopics.subTopicsByBeltNavGraph
import il.kmi.app.screens.SubTopics.subTopicsByTopicNavGraph
import il.kmi.app.screens.admin.PaymentsReportScreen
import il.kmi.app.screens.payments.PaymentScreen
import il.kmi.app.ui.loading.KmiStartupLoadingScreen
import il.kmi.app.screens.InitialLanguageScreen
import com.google.firebase.auth.FirebaseAuth
import il.kmi.app.auth.UserProfileCompletion

private const val APP_ENTRY_ROUTE = "app_entry"
private const val GOOGLE_PROFILE_COMPLETION_ROUTE = "google_profile_completion"
private const val NAV_LOG = "KMI_ENTRY_NAV"

private fun NavHostController.openIntroCleanFrom(sourceRoute: String) {
    val currentRoute = currentBackStackEntry?.destination?.route

    Log.e(
        NAV_LOG,
        "openIntroCleanFrom(source=$sourceRoute) current=$currentRoute target=${Route.Intro.route}"
    )

    if (currentRoute == Route.Intro.route) {
        Log.e(NAV_LOG, "SKIP openIntroCleanFrom - already on intro")
        return
    }

    navigate(Route.Intro.route) {
        popUpTo(sourceRoute) { inclusive = true }
        launchSingleTop = true
        restoreState = false
    }
}

private fun markInitialLanguageSelected(sp: SharedPreferences) {
    // חשוב להשתמש ב-commit כאן:
    // Google Login יכול לפתוח Activity/flow חיצוני, ולכן אנחנו רוצים שהשמירה תהיה מיידית.
    val saved = sp.edit()
        .putBoolean("initial_language_selected", true)
        .putBoolean("initial_language_selected_v2", true)
        .putBoolean("initial_language_selected_v3", true)
        .putBoolean("initial_language_selected_v4", true)
        .commit()

    Log.e(
        NAV_LOG,
        "markInitialLanguageSelected saved=$saved v4=${sp.getBoolean("initial_language_selected_v4", false)}"
    )
}

/**
 * NavHost הראשי של האפליקציה.
 */
@Composable
fun MainNavHost(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    themeMode: String,
    onThemeChange: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    startDestination: String = Route.Splash.route
) {
    val ctx = LocalContext.current

    val userPrefsForEntry = remember(ctx) {
        ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    fun isInitialLanguageAlreadySelected(): Boolean {
        return sp.getBoolean("initial_language_selected_v4", false) ||
                userPrefsForEntry.getBoolean("initial_language_selected_v4", false) ||
                sp.getBoolean("initial_language_selected", false) ||
                userPrefsForEntry.getBoolean("initial_language_selected", false)
    }

    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    val backStackEntry by nav.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry?.destination?.route) {
        Log.e(
            NAV_LOG,
            "BACK_STACK current=${backStackEntry?.destination?.route} " +
                    "navHash=${nav.hashCode()} " +
                    "mainV4=${sp.getBoolean("initial_language_selected_v4", false)} " +
                    "userV4=${userPrefsForEntry.getBoolean("initial_language_selected_v4", false)} " +
                    "selectedAny=${isInitialLanguageAlreadySelected()} " +
                    "firebaseUid=${FirebaseAuth.getInstance().currentUser?.uid}"
        )
    }

    // מונע שני ניווטים רצופים אל אותו מסך כניסה בגלל recomposition / Activity recreation
    var entryNavigationLocked by remember { mutableStateOf(false) }

// ✅ PRELOAD רשימת מתאמנים כבר בהפעלת האפליקציה
    LaunchedEffect(Unit) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            val uid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .currentUser?.uid ?: return@LaunchedEffect

            // טוען מראש את המתאמנים של המאמן
            db.collection("users")
                .whereEqualTo("coachUid", uid)
                .limit(200)
                .get()
        } catch (_: Exception) {
            // preload בלבד – לא מפריע אם נכשל
        }
    }

    // ✅ Training Summary VM + exercises list (מחשבים Role מ־SharedPreferences כדי לא להיות תלויים ב־Flow)
    val isCoach = remember {
        val role = (sp.getString("user_role", "") ?: "").lowercase()
        role == "coach" || role.contains("coach") || role.contains("מאמן") || role.contains("מדריך")
    }

    val ownerRole = remember(isCoach) {
        if (isCoach) {
            il.kmi.app.data.training.SummaryAuthorRole.COACH
        } else {
            il.kmi.app.data.training.SummaryAuthorRole.TRAINEE
        }
    }

    val ownerUid = remember {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    }

    val trainingSummaryVm = remember(ownerUid, ownerRole) {
        il.kmi.app.ui.training.TrainingSummaryViewModel(
            repo = il.kmi.app.data.training.FirestoreTrainingSummaryRepo(),
            ownerUid = ownerUid,
            ownerRole = ownerRole
        )
    }

    // ✅ כרגע ריק כדי שיקמפל (אחרי זה נחבר ל-ContentRepo)
    val allExercises = remember { emptyList<il.kmi.app.ui.training.ExercisePickItem>() }


    // 🔊 שליטה בפתיחת העוזר הקולי מכל מסך
    var showAssistant by remember { mutableStateOf(false) }

    // ⚙️ FEATURE FLAG – כרגע מכובה כדי לא להכביד על המכשיר
    val enableWakeWord = false

    // מפעילים / מכבים האזנה ל-"יובל שומע" לפי הדגל
    LaunchedEffect(enableWakeWord) {
        if (enableWakeWord) {
            WakeWordManager.start(ctx) {
                // זה יופעל כשמזוהים המילים "יובל שומע"
                showAssistant = true
            }
        } else {
            // לוודא שמפסיקים כל האזנה רציפה
            WakeWordManager.stop()
        }
    }

    // כשיוצאים מהקומפוזבל – מפסיקים האזנה
    DisposableEffect(Unit) {
        onDispose {
            WakeWordManager.stop()
        }
    }

    // אם המספר כבר אומת בעבר – נשמור את המידע כאן
    val isPhoneVerified = sp.getBoolean("phone_verified", false)

    // ⭐ עטיפה בשער נעילה בסיסמה / ללא נעילה / ביומטרי
    PinLockGate {

        NavHost(
            navController = nav,
            startDestination = APP_ENTRY_ROUTE,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {

            composable(APP_ENTRY_ROUTE) {
                LaunchedEffect(Unit) {
                    val selectedNow = isInitialLanguageAlreadySelected()
                    val currentRoute = nav.currentBackStackEntry?.destination?.route

                    Log.e(
                        NAV_LOG,
                        "APP_ENTRY effect START current=$currentRoute selectedNow=$selectedNow " +
                                "locked=$entryNavigationLocked " +
                                "mainV4=${sp.getBoolean("initial_language_selected_v4", false)} " +
                                "userV4=${userPrefsForEntry.getBoolean("initial_language_selected_v4", false)} " +
                                "firebaseUid=${FirebaseAuth.getInstance().currentUser?.uid}"
                    )

                    if (entryNavigationLocked) {
                        Log.e(NAV_LOG, "APP_ENTRY SKIP - entryNavigationLocked=true")
                        return@LaunchedEffect
                    }

                    entryNavigationLocked = true

                    if (!selectedNow) {
                        Log.e(NAV_LOG, "APP_ENTRY -> initial_language")

                        nav.navigate("initial_language") {
                            popUpTo(APP_ENTRY_ROUTE) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                        return@LaunchedEffect
                    }

                    // חשוב:
                    // APP_ENTRY לא בודק כאן פרופיל ולא מדלג ל-Home / השלמת פרטים.
                    // הוא תמיד מעביר קודם למסך הטעינה.
                    // מסך הטעינה הוא זה שמחליט בסיום לאן ממשיכים.
                    Log.e(
                        NAV_LOG,
                        "APP_ENTRY -> splash/loading selectedNow=true firebaseUid=${FirebaseAuth.getInstance().currentUser?.uid}"
                    )

                    nav.navigate(Route.Splash.route) {
                        popUpTo(APP_ENTRY_ROUTE) { inclusive = true }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            }

            composable("initial_language") {
                LaunchedEffect(Unit) {
                    Log.e(
                        NAV_LOG,
                        "SCREEN initial_language ENTER " +
                                "mainV4=${sp.getBoolean("initial_language_selected_v4", false)} " +
                                "userV4=${userPrefsForEntry.getBoolean("initial_language_selected_v4", false)} " +
                                "selectedAny=${isInitialLanguageAlreadySelected()} " +
                                "firebaseUid=${FirebaseAuth.getInstance().currentUser?.uid}"
                    )
                }

                InitialLanguageScreen(
                    entrySp = sp,
                    onLanguageSelected = {
                        Log.e(NAV_LOG, "initial_language onLanguageSelected CLICK")

                        markInitialLanguageSelected(sp)

                        // אחרי שהגענו למסך אמיתי, מאפשרים ניווט עתידי תקין
                        entryNavigationLocked = false

                        Log.e(NAV_LOG, "initial_language -> splash/loading")

                        nav.navigate(Route.Splash.route) {
                            popUpTo("initial_language") { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }

            composable(Route.Splash.route) {
                val splashScope = rememberCoroutineScope()
                var splashFinishedLocked by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    Log.e(
                        NAV_LOG,
                        "SCREEN splash ENTER selectedV4=${sp.getBoolean("initial_language_selected_v4", false)} " +
                                "firebaseUid=${FirebaseAuth.getInstance().currentUser?.uid}"
                    )
                }

                KmiStartupLoadingScreen(
                    isEnglish = isEnglish,
                    onFinished = {
                        Log.e(
                            NAV_LOG,
                            "splash onFinished CLICK/AUTO locked=$splashFinishedLocked " +
                                    "firebaseUid=${FirebaseAuth.getInstance().currentUser?.uid}"
                        )

                        if (splashFinishedLocked) {
                            Log.e(NAV_LOG, "splash onFinished SKIP duplicate")
                            return@KmiStartupLoadingScreen
                        }

                        splashFinishedLocked = true
                        entryNavigationLocked = false

                        val firebaseUser = FirebaseAuth.getInstance().currentUser

                        if (firebaseUser == null) {
                            Log.e(NAV_LOG, "splash -> intro, no Firebase user")

                            nav.openIntroCleanFrom(Route.Splash.route)
                            return@KmiStartupLoadingScreen
                        }

                        val uid = firebaseUser.uid

// ✅ קודם בודקים דגל מקומי שהרישום כבר הושלם.
// זה מונע חזרה לטופס בגלל missing=[belt] או שדה בודד.
                        if (isProfileCompletedLocally(sp, userPrefsForEntry, uid)) {
                            Log.e(NAV_LOG, "splash -> home because LOCAL profile_completed=true uid=$uid")

                            nav.navigate(Route.Home.route) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                                launchSingleTop = true
                                restoreState = false
                            }

                            return@KmiStartupLoadingScreen
                        }

                        Log.e(
                            NAV_LOG,
                            "splash detected Firebase user uid=$uid -> checking remote completion before field validation"
                        )

                        splashScope.launch {
                            val remoteCompleted = runCatching {
                                isProfileCompletedRemotely(uid)
                            }.onFailure {
                                Log.e(NAV_LOG, "splash remote completion check failed", it)
                            }.getOrDefault(false)

                            if (remoteCompleted) {
                                Log.e(
                                    NAV_LOG,
                                    "splash remote completed=true -> hydrate local profile before home uid=$uid"
                                )

                                val hydrated = runCatching {
                                    hydrateProfileLocallyFromFirestore(
                                        mainSp = sp,
                                        userSp = userPrefsForEntry,
                                        kmiPrefs = kmiPrefs,
                                        uid = uid
                                    )
                                }.onFailure {
                                    Log.e(NAV_LOG, "splash hydrate local profile failed", it)
                                }.getOrDefault(false)

                                Log.e(NAV_LOG, "splash hydrate result=$hydrated uid=$uid")

                                markProfileCompletedLocally(sp, userPrefsForEntry, uid)

                                nav.navigate(Route.Home.route) {
                                    popUpTo(Route.Splash.route) { inclusive = true }
                                    launchSingleTop = true
                                    restoreState = false
                                }

                                return@launch
                            }

                            Log.e(
                                NAV_LOG,
                                "splash remote not completed -> google_profile_completion. " +
                                        "No bypass without registrationFormCompleted=true/schema>=2"
                            )

                            nav.navigate(GOOGLE_PROFILE_COMPLETION_ROUTE) {
                                popUpTo(Route.Splash.route) { inclusive = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                )
            }

            // מסך כניסה
            composable(Route.Intro.route) {
                LaunchedEffect(Unit) {
                    Log.e(
                        NAV_LOG,
                        "SCREEN intro ENTER selectedV4=${sp.getBoolean("initial_language_selected_v4", false)}"
                    )

                    markInitialLanguageSelected(sp)
                    entryNavigationLocked = false
                }

                IntroScreen(
                    // כניסה רגילה — כן מגיעה למסך "לקוח חדש / קיים"
                    onContinue = {
                        Log.e(NAV_LOG, "intro onContinue -> registration_landing")

                        markInitialLanguageSelected(sp)

                        nav.navigate(Route.RegistrationLanding.route) {
                            popUpTo(Route.Intro.route) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },

                    // Google Login הצליח + הפרופיל מלא
                    onProfileComplete = {
                        Log.e(NAV_LOG, "intro onProfileComplete -> home")

                        markInitialLanguageSelected(sp)

                        nav.navigate(Route.Home.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },

                    // Google Login הצליח אבל חסרים פרטי KMI
                    // מדלגים על "לקוח חדש / קיים" ונכנסים ישירות להשלמת פרטים.
                    onProfileMissing = {
                        Log.e(NAV_LOG, "intro onProfileMissing -> google_profile_completion")

                        markInitialLanguageSelected(sp)

                        nav.navigate(GOOGLE_PROFILE_COMPLETION_ROUTE) {
                            // מנקים את כל ה-stack כדי שלא נחזור שוב למסך שפה / כניסה
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }

            composable(Route.WeakPoints.route) {
                il.kmi.app.screens.WeakPointsScreen(
                    onOpenHome = { nav.navigate(Route.Home.route) },
                    onOpenSettings = { DrawerBridge.openSettings() },
                    onOpenSearch = null
                )
            }

            // מסך הזנת מספר טלפון
            composable(Route.PhoneGate.route) {
            val ctxInner = LocalContext.current
                val scope = rememberCoroutineScope()

                PhoneAuthGateScreen(
                    onPhoneSubmitted = { phone ->
                        val cleaned = phone.filter { it.isDigit() }

                        scope.launch {
                            val ok = try {
                                checkAndConsumePhone(cleaned)
                            } catch (t: Throwable) {
                                Toast.makeText(
                                    ctxInner,
                                    "שגיאת חיבור לשרת. נסה שוב בעוד רגע.",
                                    Toast.LENGTH_LONG
                                ).show()
                                false
                            }

                            if (ok) {
                                sp.edit()
                                    .putString("phone_number", cleaned)
                                    .putBoolean("phone_verified", true)
                                    .apply()

                                nav.navigate(Route.RegistrationLanding.route) {
                                    popUpTo(Route.Intro.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                Toast.makeText(
                                    ctxInner,
                                    "מספר הטלפון אינו מורשה לשימוש באפליקציה.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Route.MembershipPayment.route) {
                il.kmi.app.screens.forms.payment.MembershipPaymentScreen(
                    isEnglish = isEnglish,
                    onClose = {
                        nav.popBackStack()
                    },
                    onContinueToPayment = { _ ->
                        nav.navigate(Route.Payment.route)
                    }
                )
            }

            composable(Route.ContactUs.route) {
                ContactUsScreen(
                    isEnglish = isEnglish,
                    onClose = {
                        nav.popBackStack()
                    },
                    onSubmit = { fullName, phone, email, subject, message ->
                        // כאן תדבר בהמשך לשרת / Firebase / Firestore
                    }
                )
            }

            composable(Route.Payment.route) {
                PaymentScreen(
                    isEnglish = isEnglish,
                    amountToPay = "150 ₪",
                    onClose = {
                        nav.popBackStack()
                    },
                    onPayClicked = { _, _, _, _, _, _, _, _ ->
                        // כאן תחבר סליקה / שמירה / הצלחה
                    }
                )
            }

            composable(
                route = FreeSessionsRoute.route,
                arguments = listOf(
                    navArgument("branch") { type = NavType.StringType },
                    navArgument("groupKey") { type = NavType.StringType },
                    navArgument("uid") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val branch   = backStackEntry.arguments?.getString("branch").orEmpty()
                val groupKey = backStackEntry.arguments?.getString("groupKey").orEmpty()
                val uid      = backStackEntry.arguments?.getString("uid").orEmpty()
                val name     = backStackEntry.arguments?.getString("name").orEmpty()

                FreeSessionsScreen(
                    branch = branch,
                    groupKey = groupKey,
                    currentUid = uid,
                    currentName = name,
                    onBack = { nav.popBackStack() }
                )
            }

            // מסך קוד ה-SMS
            composable("phone_verify/{phone}") { backStackEntry ->
                val ctxInner = LocalContext.current
                val scope = rememberCoroutineScope()
                val phone = backStackEntry.arguments?.getString("phone") ?: ""

                SmsVerifyScreen(
                    phone = phone,
                    onVerified = { verifiedPhone ->
                        val cleaned = verifiedPhone.filter { it.isDigit() }

                        scope.launch {
                            val ok = try {
                                checkAndConsumePhone(cleaned)
                            } catch (t: Throwable) {
                                Toast.makeText(
                                    ctxInner,
                                    "שגיאת חיבור לשרת. נסה שוב בעוד רגע.",
                                    Toast.LENGTH_LONG
                                ).show()
                                false
                            }

                            if (ok) {
                                sp.edit()
                                    .putString("phone_number", cleaned)
                                    .putBoolean("phone_verified", true)
                                    .apply()

                                nav.navigate(Route.RegistrationLanding.route) {
                                    popUpTo(Route.Intro.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                Toast.makeText(
                                    ctxInner,
                                    "מספר הטלפון אינו מורשה לשימוש באפליקציה.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable("coach/trainees") {
                il.kmi.app.screens.coach.CoachTraineesScreen(
                    onBack = { nav.popBackStack() },
                    onOpenDrawer = { il.kmi.app.ui.DrawerBridge.open() }
                )
            }

            // --- Registration Landing ---
            composable(Route.RegistrationLanding.route) {
                val regNav = rememberNavController()

                RegistrationNavHost(
                    nav = regNav,
                    vm = vm,
                    sp = sp,
                    kmiPrefs = kmiPrefs,
                    onOpenDrawer = { DrawerBridge.open() },
                    onOpenLegal = { nav.navigate(Route.Legal.route) },
                    onOpenTerms = { nav.navigate(Route.Legal.route) },
                    onRegistrationDone = {
                        nav.navigate(Route.Home.route) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                )
            }

            // --- Google Login: השלמת פרטים ישירה בלי מסך לקוח חדש / קיים ---
            composable(GOOGLE_PROFILE_COMPLETION_ROUTE) {
                val regNav = rememberNavController()

                RegistrationNavHost(
                    nav = regNav,
                    vm = vm,
                    sp = sp,
                    kmiPrefs = kmiPrefs,
                    onOpenDrawer = { DrawerBridge.open() },
                    onOpenLegal = { nav.navigate(Route.Legal.route) },
                    onOpenTerms = { nav.navigate(Route.Legal.route) },
                    onRegistrationDone = {
                        nav.navigate(Route.Home.route) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    },
                    startAfterGoogleLogin = true
                )
            }

            // --- NEW: Legal graph ---
            legalNavGraph(nav = nav)

            // --- NEW: Settings graph ---
            settingsNavGraph(
                nav = nav,
                sp = sp,
                kmiPrefs = kmiPrefs,
                themeMode = themeMode,
                onThemeChange = onThemeChange
            )

            // --- NEW: Home graph (מינימלי) ---
            homeNavGraph(
                nav = nav,
                vm = vm,
                sp = sp,
                kmiPrefs = kmiPrefs,
                onOpenDrawer = onOpenDrawer
            )

            // --- NEW: Training graph ---
            trainingNavGraph(
                nav = nav,
                vm = vm,
                sp = sp,
                kmiPrefs = kmiPrefs
            )

            // ✅ NEW: Training Summary graph (סיכום אימון)
            trainingSummaryNavGraph(
                nav = nav,
                kmiVm = vm,
                summaryVm = trainingSummaryVm,
                sp = sp,
                kmiPrefs = kmiPrefs,
                onBack = null
            )

            // --- NEW: Topics graph ---
            topicsNavGraph(
                nav = nav,
                vm  = vm,
                sp  = sp,
                kmiPrefs = kmiPrefs
            )

            // --- NEW: SubTopics graphs ---
            subTopicsByBeltNavGraph(
                nav = nav
            )
            subTopicsByTopicNavGraph(
                nav = nav
            )

            // --- NEW: Materials graph ---
            materialsNavGraph(
                nav = nav,
                vm  = vm,
                sp  = sp,
                kmiPrefs = kmiPrefs
            )

            // ----- לוח אימונים חודשי -----
            composable(route = Route.MonthlyCalendar.route) {
                il.kmi.app.screens.MonthlyCalendarScreen(
                    kmiPrefs = kmiPrefs,
                    onBack = { nav.popBackStack() },
                    onDateClick = { pickedDate ->
                        nav.navigate(
                            Route.TrainingSummary.make(pickedDate.toString())
                        )
                    }
                )
            }

            // ----- הפרופיל שלי -----
            composable(route = Route.MyProfile.route) {
                MyProfileScreen(
                    sp = userPrefsForEntry,
                    kmiPrefs = kmiPrefs,
                    onClose = { nav.popBackStack() }
                )
            }

            // 🔐 אזור מנהל – ניהול משתמשים
            composable(route = Route.AdminUsers.route) {
                il.kmi.app.screens.admin.AdminUsersScreen(
                    onBack = { nav.popBackStack() }
                )
            }

            // --- NEW: Attendance graph ---
            attendanceNavGraph(nav = nav)

            // --- NEW: Subscription graph ---
            subscriptionNavGraph(
                nav = nav,
                vm  = vm,
                sp  = sp,
                kmiPrefs = kmiPrefs
            )

            // --- NEW: Summary graph ---
            summaryNavGraph(
                nav = nav,
                vm  = vm,
                sp  = sp,
                kmiPrefs = kmiPrefs
            )

            // --- NEW: Practice graph ---
            practiceNavGraph(
                nav = nav,
                vm  = vm,
                sp  = sp,
                kmiPrefs = kmiPrefs
            )

            // --- NEW: Exam graph ---
            examNavGraph(
                nav = nav,
                vm  = vm,
                sp  = sp,
                kmiPrefs = kmiPrefs
            )

            // --- NEW: Progress graph ---
            progressNavGraph(
                nav = nav,
                vm  = vm,
                sp  = sp,
                kmiPrefs = kmiPrefs
            )

            // --- NEW: About / Forum / Legal graph ---
            aboutNavGraph(
                nav = nav,
                vm  = vm,
                sp  = sp,
                kmiPrefs = kmiPrefs
            )

            // --- NEW: Registration graph ---
            registrationNavGraph(
                nav = nav,
                vm = vm,
                sp = sp,
                kmiPrefs = kmiPrefs
            )


            // --- NEW: Coach graph ---
            coachNavGraph(
                nav = nav,
                vm = vm,
                sp = sp,
                kmiPrefs = kmiPrefs
            )

// ✅ Voice assistant route = מסך רגיל מלא, לא Dialog
            composable(Route.VoiceAssistant.route) {
                AiAssistantDialog(
                    onDismiss = {
                        nav.popBackStack()
                    },

                    onOpenDrawer = {
                        DrawerBridge.open()
                    },

                    onVoiceCommand = { cmd ->
                        when (cmd) {
                            VoiceNavCommand.OpenTraining -> {
                                nav.navigate(Route.MonthlyCalendar.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }

                            VoiceNavCommand.OpenHome -> {
                                nav.navigate(Route.Home.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(nav.graph.startDestinationId) { inclusive = false }
                                }
                            }

                            else -> {}
                        }
                    }
                )
            }

            composable(Route.PaymentsReport.route) {
                PaymentsReportScreen(
                    isEnglish = isEnglish,
                    onClose = {
                        nav.popBackStack()
                    },
                    onSaveManualPayment = { traineeId, amount, method, notes ->
                        // כאן נחבר בהמשך ל-Firebase / Firestore
                    }
                )
            }

            // ✅ NEW: Voice settings (קול אחיד לכל האפליקציה)
            composable("voice_settings") {
                il.kmi.app.screens.VoiceSettingsScreen(
                    sp = sp,
                    onBack = { nav.popBackStack() }
                )
            }

            // --- NEW: Rate us ---
            composable(Route.RateUs.route) {
                RateUsScreen(
                    onClose = { nav.popBackStack() }
                )
            }
        }   // <-- NavHost
    }       // <-- PinLockGate
}           // <-- MainNavHost

private fun isProfileCompletedLocally(
    mainSp: SharedPreferences,
    userSp: SharedPreferences,
    uid: String
): Boolean {
    if (uid.isBlank()) return false

    val mainCompleted =
        mainSp.getBoolean("profile_completed", false) ||
                mainSp.getBoolean("registration_complete", false)

    val userCompleted =
        userSp.getBoolean("profile_completed", false) ||
                userSp.getBoolean("registration_complete", false)

    val savedUid =
        mainSp.getString("profile_completed_uid", "")?.takeIf { it.isNotBlank() }
            ?: userSp.getString("profile_completed_uid", "")?.takeIf { it.isNotBlank() }
            ?: ""

    val uidMatches = savedUid.isBlank() || savedUid == uid

    fun getStringAny(key: String): String {
        return mainSp.getString(key, "")?.takeIf { it.isNotBlank() }
            ?: userSp.getString(key, "")?.takeIf { it.isNotBlank() }
            ?: ""
    }

    val role = getStringAny("user_role").lowercase()
    val isCoach = role == "coach" || role.contains("coach") || role.contains("מאמן")

    val fullName = getStringAny("fullName").trim()
    val email = getStringAny("email").trim()
    val phone = (
            getStringAny("phone").ifBlank { getStringAny("phone_number") }
            ).filter { it.isDigit() }

    val region = getStringAny("region").trim()
    val branch = getStringAny("branch").trim()
    val group = getStringAny("age_group").ifBlank { getStringAny("group") }.trim()
    val gender = getStringAny("gender").trim()
    val belt = getStringAny("current_belt").ifBlank { getStringAny("belt_current") }.trim()

    val hasCoreProfile =
        fullName.isNotBlank() &&
                email.isNotBlank() &&
                phone.length >= 9 &&
                region.isNotBlank() &&
                branch.isNotBlank() &&
                (group.isNotBlank() || isCoach) &&
                gender.isNotBlank() &&
                (isCoach || belt.isNotBlank())

    val mainFormCompleted =
        mainSp.getBoolean("registration_form_completed", false) &&
                mainSp.getInt("registration_schema_version", 0) >= 2

    val userFormCompleted =
        userSp.getBoolean("registration_form_completed", false) &&
                userSp.getInt("registration_schema_version", 0) >= 2

    val completedFlag = (mainCompleted || userCompleted) && (mainFormCompleted || userFormCompleted)
    val finalResult = uidMatches && completedFlag && hasCoreProfile

    Log.e(
        NAV_LOG,
        "isProfileCompletedLocally uid=$uid mainCompleted=$mainCompleted userCompleted=$userCompleted " +
                "savedUid=$savedUid uidMatches=$uidMatches role=$role " +
                "mainFormCompleted=$mainFormCompleted userFormCompleted=$userFormCompleted " +
                "fullName=${fullName.isNotBlank()} email=${email.isNotBlank()} phoneLen=${phone.length} " +
                "region=${region.isNotBlank()} branch=${branch.isNotBlank()} group=${group.isNotBlank()} " +
                "gender=${gender.isNotBlank()} belt=$belt finalResult=$finalResult"
    )

    return finalResult
}

private suspend fun hydrateProfileLocallyFromFirestore(
    mainSp: SharedPreferences,
    userSp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    uid: String
): Boolean {
    if (uid.isBlank()) return false

    val doc = Firebase.firestore
        .collection("users")
        .document(uid)
        .get()
        .await()

    if (!doc.exists()) {
        Log.e(NAV_LOG, "hydrateProfileLocallyFromFirestore uid=$uid exists=false")
        return false
    }

    val role = doc.getString("role").orEmpty()
    val fullName = doc.getString("fullName").orEmpty()
    val email = doc.getString("email").orEmpty()
    val phone = (
            doc.getString("phone")
                ?: doc.getString("phoneNumber")
                ?: ""
            ).filter { it.isDigit() }

    val region = doc.getString("region").orEmpty()

    val branchesList = doc.get("branches") as? List<*>
    val branchesCsvFromList = branchesList
        ?.mapNotNull { it?.toString()?.trim() }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        ?.joinToString(", ")
        .orEmpty()

    val branchesFinal = doc.getString("branchesCsv")
        ?.takeIf { it.isNotBlank() }
        ?: doc.getString("branch")?.takeIf { it.isNotBlank() }
        ?: branchesCsvFromList

    val activeBranchFinal = doc.getString("activeBranch")
        ?.takeIf { it.isNotBlank() }
        ?: branchesFinal.split(",").firstOrNull()?.trim().orEmpty()

    val groupsList = doc.get("groups") as? List<*>
    val groupsCsv = groupsList
        ?.mapNotNull { it?.toString()?.trim() }
        ?.filter { it.isNotBlank() }
        ?.distinct()
        ?.joinToString(", ")
        .orEmpty()

    val primaryGroup = doc.getString("primaryGroup")
        ?.takeIf { it.isNotBlank() }
        ?: doc.getString("activeGroup")?.takeIf { it.isNotBlank() }
        ?: groupsCsv.split(",").firstOrNull()?.trim().orEmpty()

    val activeGroupFinal = doc.getString("activeGroup")
        ?.takeIf { it.isNotBlank() }
        ?: primaryGroup

    val gender = doc.getString("gender").orEmpty()

    val beltFinal = (
            doc.getString("belt")
                ?: doc.getString("currentBelt")
                ?: ""
            ).trim()

    val birthDate = doc.getString("birthDate").orEmpty()
    val birthParts = birthDate.split("-")
    val birthYear = birthParts.getOrNull(0)?.toIntOrNull()?.toString() ?: "2000"
    val birthMonth = birthParts.getOrNull(1)?.toIntOrNull()?.toString() ?: "1"
    val birthDay = birthParts.getOrNull(2)?.toIntOrNull()?.toString() ?: "1"

    val completedAt = System.currentTimeMillis()

    fun SharedPreferences.Editor.putProfileCore(): SharedPreferences.Editor {
        putString("uid", uid)
        putString("firebase_uid", uid)

        putString("fullName", fullName)
        putString("name", fullName)
        putString("user_name", fullName)
        putString("displayName", fullName)

        putString("phone", phone)
        putString("phone_number", phone)

        putString("email", email)
        putString("user_email", email)

        putString("user_role", role)
        putString("region", region)
        putString("branch", branchesFinal)
        putString("active_branch", activeBranchFinal)

        putString("age_groups", groupsCsv)
        putString("age_group", primaryGroup)
        putString("group", primaryGroup)
        putString("active_group", activeGroupFinal)

        putString("gender", gender)
        putString("current_belt", beltFinal)
        putString("belt_current", beltFinal)

        putString("birth_year", birthYear)
        putString("birth_month", birthMonth)
        putString("birth_day", birthDay)

        putString("authProvider", "google")
        putBoolean("google_login", true)
        putBoolean("skip_otp", true)

        putBoolean("profile_completed", true)
        putBoolean("registration_complete", true)

        // ✅ דגל חדש: רק משתמש שעבר את טופס הרישום החדש נחשב מושלם מקומית
        putBoolean("registration_form_completed", true)
        putInt("registration_schema_version", 2)

        putString("profile_completed_uid", uid)
        putLong("profile_completed_at", completedAt)

        return this
    }

    mainSp.edit()
        .putProfileCore()
        .commit()

    userSp.edit()
        .putProfileCore()
        .commit()

    kmiPrefs.fullName = fullName
    kmiPrefs.phone = phone
    kmiPrefs.email = email
    kmiPrefs.region = region
    kmiPrefs.branch = branchesFinal
    kmiPrefs.ageGroup = primaryGroup
    kmiPrefs.username = email

    Log.e(
        NAV_LOG,
        "hydrateProfileLocallyFromFirestore uid=$uid " +
                "fullName=${fullName.isNotBlank()} email=${email.isNotBlank()} phoneLen=${phone.length} " +
                "region=${region.isNotBlank()} branch=${branchesFinal.isNotBlank()} " +
                "group=${primaryGroup.isNotBlank()} gender=${gender.isNotBlank()} belt=$beltFinal"
    )

    return true
}

private fun markProfileCompletedLocally(
    mainSp: SharedPreferences,
    userSp: SharedPreferences,
    uid: String
) {
    val completedAt = System.currentTimeMillis()

    mainSp.edit()
        .putBoolean("profile_completed", true)
        .putBoolean("registration_complete", true)

        // ✅ דגל חדש: חשוב כדי שבכניסה הבאה isProfileCompletedLocally יחזיר true
        .putBoolean("registration_form_completed", true)
        .putInt("registration_schema_version", 2)

        .putString("profile_completed_uid", uid)
        .putLong("profile_completed_at", completedAt)
        .commit()

    userSp.edit()
        .putBoolean("profile_completed", true)
        .putBoolean("registration_complete", true)

        // ✅ דגל חדש: חשוב כדי שבכניסה הבאה isProfileCompletedLocally יחזיר true
        .putBoolean("registration_form_completed", true)
        .putInt("registration_schema_version", 2)

        .putString("profile_completed_uid", uid)
        .putLong("profile_completed_at", completedAt)
        .commit()

    Log.e(
        NAV_LOG,
        "markProfileCompletedLocally uid=$uid completedAt=$completedAt registration_form_completed=true schema=2"
    )
}

private suspend fun isProfileCompletedRemotely(uid: String): Boolean {
    if (uid.isBlank()) return false

    val doc = Firebase.firestore
        .collection("users")
        .document(uid)
        .get()
        .await()

    if (!doc.exists()) {
        Log.e(NAV_LOG, "isProfileCompletedRemotely uid=$uid exists=false")
        return false
    }

    val profileCompleted = doc.getBoolean("profileCompleted") == true
    val registrationComplete = doc.getBoolean("registrationComplete") == true

// ✅ דגל חדש: לא מספיק profileCompleted ישן.
// רק משתמש שעבר את טופס הרישום החדש מקבל אישור לדלג על הטופס.
    val registrationFormCompleted = doc.getBoolean("registrationFormCompleted") == true
    val registrationSchemaVersion = (doc.getLong("registrationSchemaVersion") ?: 0L).toInt()
    val hasNewRegistrationCompletion =
        registrationFormCompleted && registrationSchemaVersion >= 2

    val role = doc.getString("role").orEmpty().lowercase()
    val fullName = doc.getString("fullName").orEmpty().trim()
    val email = doc.getString("email").orEmpty().trim()
    val phone = (
            doc.getString("phone")
                ?: doc.getString("phoneNumber")
                ?: ""
            ).filter { it.isDigit() }

    val region = doc.getString("region").orEmpty().trim()

    val branches = doc.get("branches") as? List<*>
    val hasBranch =
        !doc.getString("branch").orEmpty().trim().isBlank() ||
                !doc.getString("branchesCsv").orEmpty().trim().isBlank() ||
                branches?.any { !it?.toString().orEmpty().trim().isBlank() } == true

    val groups = doc.get("groups") as? List<*>
    val hasGroup =
        !doc.getString("primaryGroup").orEmpty().trim().isBlank() ||
                !doc.getString("activeGroup").orEmpty().trim().isBlank() ||
                groups?.any { !it?.toString().orEmpty().trim().isBlank() } == true ||
                role == "coach"

    val gender = doc.getString("gender").orEmpty().trim()

    val belt = (
            doc.getString("belt")
                ?: doc.getString("currentBelt")
                ?: ""
            ).trim()

    val isCoach = role == "coach" || role.contains("coach") || role.contains("מאמן")
    val hasBeltIfNeeded = isCoach || belt.isNotBlank()

    val hasCoreProfile =
        fullName.isNotBlank() &&
                email.isNotBlank() &&
                phone.length >= 9 &&
                region.isNotBlank() &&
                hasBranch &&
                hasGroup &&
                gender.isNotBlank() &&
                hasBeltIfNeeded

    val completedFlag = (profileCompleted || registrationComplete) && hasNewRegistrationCompletion
    val finalResult = completedFlag && hasCoreProfile

    Log.e(
        NAV_LOG,
        "isProfileCompletedRemotely uid=$uid exists=true " +
                "profileCompleted=$profileCompleted registrationComplete=$registrationComplete " +
                "registrationFormCompleted=$registrationFormCompleted registrationSchemaVersion=$registrationSchemaVersion " +
                "role=$role fullName=${fullName.isNotBlank()} email=${email.isNotBlank()} " +
                "phoneLen=${phone.length} region=${region.isNotBlank()} " +
                "hasBranch=$hasBranch hasGroup=$hasGroup gender=${gender.isNotBlank()} " +
                "belt=$belt hasBeltIfNeeded=$hasBeltIfNeeded finalResult=$finalResult"
    )

    return finalResult
}

/**
 * בדיקת מספר טלפון מול Firestore.
 */
private suspend fun checkAndConsumePhone(phoneDigits: String): Boolean {
    val db = Firebase.firestore

    val doc = db.collection("allowed_numbers")
        .document("numbers")
        .get()
        .await()

    val rawList = doc.get("list") as? List<*> ?: emptyList<Any>()
    val allowedNumbers = rawList
        .mapNotNull { it?.toString() }
        .map { it.filter { ch -> ch.isDigit() } }

    if (phoneDigits !in allowedNumbers) {
        return false
    }

    val existingUserSnap = db.collection("users")
        .whereEqualTo("phone", phoneDigits)
        .limit(1)
        .get()
        .await()

    if (!existingUserSnap.isEmpty) {
        return true
    }

    runCatching {
        db.collection("used_numbers")
            .document(phoneDigits)
            .set(
                mapOf(
                    "usedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    return true
}
