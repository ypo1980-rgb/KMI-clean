package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import il.kmi.app.screens.SmsVerifyScreen
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.IntroScreen
import il.kmi.app.screens.registration.RegistrationNavHost
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Context
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
import il.kmi.app.ui.assistant.AiAssistantDialog
import il.kmi.app.ui.WakeWordManager
import il.kmi.app.ui.assistant.VoiceNavCommand
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import il.kmi.app.screens.ContactUsScreen
import il.kmi.app.screens.SubTopics.subTopicsByBeltNavGraph
import il.kmi.app.screens.SubTopics.subTopicsByTopicNavGraph
import il.kmi.app.screens.admin.PaymentsReportScreen
import il.kmi.app.screens.payments.PaymentScreen

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
    startDestination: String = Route.Intro.route
) {
    val ctx = LocalContext.current

    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

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

    LaunchedEffect(nav) {
        DrawerBridge.register(
            onOpenDrawer = { /* handled elsewhere */ },
            onOpenSettings = {
                nav.navigate(Route.Settings.route) {
                    launchSingleTop = true
                }
            },
            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        )
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
            // תמיד מתחילים במסך הפתיחה; נחליט שם אם לדלג על האימות
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {

            // מסך פתיחה
            composable(Route.Intro.route) {
                val ctxInner = LocalContext.current
                val authSp = remember {
                    ctxInner.getSharedPreferences("kmi_auth", Context.MODE_PRIVATE)
                }
                val savedPhone = authSp.getString("phone_number", null)
                val hasLocalPhone = !savedPhone.isNullOrBlank()

                val isPhoneVerifiedInner = sp.getBoolean("phone_verified", false)
                val effectiveVerified = isPhoneVerifiedInner || hasLocalPhone

                IntroScreen(
                    onContinue = {
                        if (effectiveVerified) {
                            nav.navigate(Route.RegistrationLanding.route) {
                                popUpTo(Route.Intro.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            nav.navigate(Route.PhoneGate.route) {
                                popUpTo(Route.Intro.route) { inclusive = true }
                                launchSingleTop = true
                            }
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
                kmiPrefs = kmiPrefs
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
                    sp = sp,
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

            // ✅ Voice assistant route = טריגר לפתיחת הדיאלוג הגלובלי (showAssistant)
            composable(Route.VoiceAssistant.route) {
                LaunchedEffect(Unit) {
                    showAssistant = true
                    // לא נשארים במסך "ריק" — חוזרים אחורה מיד
                    nav.popBackStack()
                }
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

        // 🔊 כאן מציירים את העוזר הקולי מעל כל המסכים
        if (showAssistant) {
            AiAssistantDialog(
                onDismiss = {
                    showAssistant = false
                    if (enableWakeWord) {
                        WakeWordManager.start(ctx) { showAssistant = true }
                    }
                },
                contextLabel = "כל האפליקציה",

                // ✅ זה החיבור שמבצע את הניווט אחרי שהעוזר סיים לדבר
                onVoiceCommand = { cmd ->
                    when (cmd) {
                        VoiceNavCommand.OpenTraining -> {
                            showAssistant = false
                            nav.navigate(Route.MonthlyCalendar.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        VoiceNavCommand.OpenHome -> {
                            showAssistant = false
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
    }       // <-- PinLockGate
}           // <-- MainNavHost


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
