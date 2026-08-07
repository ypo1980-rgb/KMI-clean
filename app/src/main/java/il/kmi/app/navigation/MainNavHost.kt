package il.kmi.app.navigation

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import il.kmi.app.screens.SmsVerifyScreen
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.TopicsEngine
import il.kmi.app.domain.ContentRepo
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
import androidx.compose.ui.platform.LocalContext
import il.kmi.app.screens.MyProfileScreen
import il.kmi.app.screens.PhoneAuthGateScreen
import il.kmi.app.screens.RateUsScreen
import il.kmi.app.ui.DrawerBridge
import il.kmi.app.ui.KmiTtsManager
import android.widget.Toast
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
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
import il.kmi.app.screens.AboutNetworkCoachesScreen
import il.kmi.app.screens.BeltQuestions.ByBelt.subTopicsByBeltNavGraph
import il.kmi.app.screens.BeltQuestions.ByTopic.subTopicsByTopicNavGraph
import il.kmi.app.screens.admin.PaymentsReportScreen
import il.kmi.app.screens.admin.AdminDiagnosticsScreen
import il.kmi.app.screens.admin.AdminAccess
import il.kmi.app.screens.payments.PaymentScreen
import il.kmi.app.ui.loading.KmiStartupLoadingScreen
import il.kmi.app.screens.InitialLanguageScreen
import il.kmi.app.screens.drawer.DrawerVoiceActionsBridge
import com.google.firebase.auth.FirebaseAuth
import il.kmi.app.screens.registration.RegistrationFormScreen
import il.kmi.app.analytics.KmiUsageTracker
import il.kmi.app.onboarding.OnboardingPreferences
import il.kmi.app.onboarding.onboardingNavGraph
import il.kmi.app.onboarding.OnboardingRoute
import il.kmi.app.ui.OnboardingBridge
import il.kmi.app.ui.VoiceExerciseExplanationBridge
import il.kmi.app.voicecommands.VoiceCommandListener
import il.kmi.app.voicecommands.VoiceAppCommand
import il.kmi.app.voicecommands.VoiceDrawerDestination
import il.kmi.app.voicecommands.VoiceCommandDiagnosticsLogger
import il.kmi.app.voicecommands.VoiceCommandsBridge
import il.kmi.app.subscription.AccessModeResolver
import il.kmi.app.subscription.KmiAccess
import il.kmi.app.subscription.LockedContentPolicy

private const val APP_ENTRY_ROUTE = "app_entry"
private const val GOOGLE_PROFILE_COMPLETION_ROUTE = "google_profile_completion"
private const val PROFILE_EDIT_ROUTE = "profile_edit"

private const val TAG_NAV = "KMI_NAV"

private fun authStateForLog(): String {
    val user = FirebaseAuth.getInstance().currentUser

    return if (user == null) {
        "uid=null, email=null, isAnonymous=null, providers=[]"
    } else {
        val providers = user.providerData
            .map { it.providerId }
            .joinToString("|")

        "uid=${user.uid}, email=${user.email.orEmpty()}, isAnonymous=${user.isAnonymous}, providers=[$providers]"
    }
}

private fun NavHostController.openIntroCleanFrom(sourceRoute: String) {
    val currentRoute = currentBackStackEntry?.destination?.route

    if (currentRoute == Route.Intro.route) {
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
    sp.edit()
        .putBoolean("initial_language_selected", true)
        .putBoolean("initial_language_selected_v2", true)
        .putBoolean("initial_language_selected_v3", true)
        .putBoolean("initial_language_selected_v4", true)
        .commit()
}

fun resolveVoiceBelt(query: String): Belt? {
    val normalized = query.trim().lowercase()

    return when {
        normalized.contains("לבנ") ||
                normalized.contains("white") ->
            Belt.WHITE

        normalized.contains("צהוב") ||
                normalized.contains("yellow") ->
            Belt.YELLOW

        normalized.contains("כתומ") ||
                normalized.contains("orange") ->
            Belt.ORANGE

        normalized.contains("ירוק") ||
                normalized.contains("green") ->
            Belt.GREEN

        normalized.contains("כחול") ||
                normalized.contains("blue") ->
            Belt.BLUE

        normalized.contains("חומ") ||
                normalized.contains("brown") ->
            Belt.BROWN

        normalized.contains("שחור") ||
                normalized.contains("black") ->
            Belt.BLACK

        else -> null
    }
}

fun resolveVoiceTopicId(query: String): String? {
    val normalized = query
        .trim()
        .lowercase()
        .replace("-", " ")
        .replace(Regex("\\s+"), " ")

    return when {
        normalized.contains("בעיט") ||
                normalized.contains("kick") ->
            "kicks"

        normalized.contains("עבודת יד") ||
                normalized.contains("עבודה יד") ||
                normalized.contains("אגרופ") ||
                normalized.contains("מכות יד") ||
                normalized.contains("hand work") ||
                normalized.contains("hand technique") ||
                normalized.contains("punch") ||
                normalized.contains("hand strike") ->
            "hands_strikes"

        normalized.contains("מרפק") ||
                normalized.contains("elbow") ->
            "hands_elbows"

        normalized.contains("שחרור") ||
                normalized.contains("release") ->
            "releases"

        normalized.contains("סכין") ||
                normalized.contains("knife") ->
            "knife_defense"

        normalized.contains("אקדח") ||
                normalized.contains("gun") ||
                normalized.contains("pistol") ->
            "gun_threat_defense"

        normalized.contains("מקל") ||
                normalized.contains("stick") ->
            "stick_defense"

        normalized.contains("מספר תוקפים") ||
                normalized.contains("תוקפים מרובים") ||
                normalized.contains("multiple attackers") ->
            "multiple_attackers_defense"

        /*
         * "נפילות וגלגולים" נשאר ביטוי קולי חוקי,
         * אבל הוא ממופה לנושא הקיים "בלימות וגלגולים".
         */
        normalized.contains("בלימות") ||
                normalized.contains("בלימה") ||
                normalized.contains("נפילות") ||
                normalized.contains("נפילה") ||
                normalized.contains("גלגולים") ||
                normalized.contains("גלגול") ||
                normalized.contains("breakfall") ||
                normalized.contains("breakfalls") ||
                normalized.contains("roll") ||
                normalized.contains("rolls") ->
            "topic_breakfalls_rolls"

        normalized.contains("עמידת מוצא") ||
                normalized.contains("עמידת קרב") ||
                normalized.contains("ready stance") ->
            "topic_ready_stance"

        normalized.contains("קרקע") ||
                normalized.contains("ground") ->
            "topic_ground_prep"

        else -> null
    }
}

/**
 * שם הנושא כפי שהוא שמור במאגר התרגילים.
 *
 * הערך אינו תלוי בשפת הממשק, משום שהוא משמש
 * כמפתח לאיתור התרגילים בתוך MaterialsScreen.
 */
private fun voiceTopicRouteValue(
    topicId: String
): String {
    return when (topicId) {
        "kicks" ->
            "בעיטות"

        "hands_strikes" ->
            "עבודת ידיים"

        "hands_elbows" ->
            "מרפקים"

        "releases" ->
            "שחרורים"

        "knife_defense" ->
            "הגנות מסכין"

        "gun_threat_defense" ->
            "הגנות מאקדח"

        "stick_defense" ->
            "הגנות ממקל"

        "multiple_attackers_defense" ->
            "הגנות ממספר תוקפים"

        "topic_breakfalls_rolls" ->
            "בלימות וגלגולים"

        "topic_ready_stance" ->
            "עמידת מוצא"

        "topic_ground_prep" ->
            "הכנה לקרקע"

        else ->
            topicId
    }
}

/**
 * שם ידידותי למשתמש עבור הפידבק הקולי.
 */
private fun voiceTopicDisplayName(
    topicId: String,
    isEnglish: Boolean
): String {
    return when (topicId) {
        "kicks" ->
            if (isEnglish) "kicks" else "בעיטות"

        "hands_strikes" ->
            if (isEnglish) {
                "hand work"
            } else {
                "עבודת ידיים"
            }

        "hands_elbows" ->
            if (isEnglish) "elbows" else "מרפקים"

        "releases" ->
            if (isEnglish) "releases" else "שחרורים"

        "knife_defense" ->
            if (isEnglish) "knife defense" else "הגנות מסכין"

        "gun_threat_defense" ->
            if (isEnglish) "gun defense" else "הגנות מאקדח"

        "stick_defense" ->
            if (isEnglish) "stick defense" else "הגנות ממקל"

        "multiple_attackers_defense" ->
            if (isEnglish) {
                "multiple attackers defense"
            } else {
                "הגנות ממספר תוקפים"
            }

        "topic_breakfalls_rolls" ->
            if (isEnglish) {
                "breakfalls and rolls"
            } else {
                "בלימות וגלגולים"
            }

        "topic_ready_stance" ->
            if (isEnglish) {
                "ready stance"
            } else {
                "עמידת מוצא"
            }

        "topic_ground_prep" ->
            if (isEnglish) {
                "ground preparation"
            } else {
                "הכנה לקרקע"
            }

        else ->
            topicId
                .replace('_', ' ')
                .trim()
    }
}

/**
 * מנרמל שמות נושאים לצורך השוואה בטוחה בין
 * פקודה קולית, TopicsEngine ו־ContentRepo.
 */
private fun normalizeVoiceTopicTitle(
    raw: String
): String {
    return raw
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace("־", "-")
        .replace(Regex("\\s*-\\s*"), "-")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase()
}

/**
 * מחזיר את שם הנושא האמיתי כפי שהוא שמור במאגר.
 *
 * אם הנושא אינו קיים בחגורה, מוחזר null ואסור
 * לפתוח עבורו מסך חומרים.
 */
private fun resolveExistingVoiceTopicForBelt(
    belt: Belt,
    requestedTopicTitle: String
): String? {
    val requestedNormalized =
        normalizeVoiceTopicTitle(
            requestedTopicTitle
        )

    /*
     * בדיקה ראשונה מול רשימת הנושאים האמיתית
     * שמוצגת במסך "תרגילים לפי חגורה".
     */
    val topicFromEngine =
        runCatching {
            TopicsEngine.topicTitlesFor(belt)
        }
            .getOrDefault(emptyList())
            .firstOrNull { existingTitle ->
                normalizeVoiceTopicTitle(existingTitle) ==
                        requestedNormalized
            }

    if (topicFromEngine != null) {
        return topicFromEngine
    }

    /*
     * חלק מהפקודות מפנות לנושא ישיר או לתת־נושא.
     * לכן מבצעים בדיקה נוספת מול מאגר התוכן.
     */
    val directItems =
        runCatching {
            ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = requestedTopicTitle,
                subTopicTitle = null
            )
        }.getOrDefault(emptyList())

    val subTopics =
        runCatching {
            ContentRepo.listSubTopicTitles(
                belt = belt,
                topicTitle = requestedTopicTitle
            )
        }.getOrDefault(emptyList())

    return if (
        directItems.isNotEmpty() ||
        subTopics.isNotEmpty()
    ) {
        requestedTopicTitle
    } else {
        null
    }
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
    onFontSizeChange: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    startDestination: String = Route.Splash.route
) {
    val ctx = LocalContext.current

    val forumPushSp = remember(ctx) {
        ctx.applicationContext.getSharedPreferences(
            "kmi_forum_push",
            Context.MODE_PRIVATE
        )
    }

    val dailyReminderSp = remember(ctx) {
        ctx.applicationContext.getSharedPreferences(
            "kmi_daily_reminder_nav",
            Context.MODE_PRIVATE
        )
    }

    fun consumePendingDailyReminderAndNavigate(source: String): Boolean {
        val hasPendingDailyReminder =
            dailyReminderSp.getBoolean("has_pending_daily_reminder", false)

        if (!hasPendingDailyReminder) {
            return false
        }

        val beltId = dailyReminderSp.getString("daily_reminder_belt_id", "").orEmpty()
        val topic = dailyReminderSp.getString("daily_reminder_topic", "").orEmpty()
        val item = dailyReminderSp.getString("daily_reminder_item", "").orEmpty()

        dailyReminderSp.edit()
            .putBoolean("has_pending_daily_reminder", false)
            .remove("daily_reminder_belt_id")
            .remove("daily_reminder_topic")
            .remove("daily_reminder_item")
            .remove("received_at")
            .apply()

        val targetRoute = if (beltId.isNotBlank() && topic.isNotBlank()) {
            Route.TopicExercises.makeId(
                beltId = beltId,
                topic = topic,
                sub = null
            )
        } else {
            Route.BeltQ.route
        }

        nav.navigate(targetRoute) {
            launchSingleTop = true
            restoreState = false
        }

        return true
    }

    fun consumePendingForumPushAndNavigate(source: String): Boolean {
        val hasPendingForumPush =
            forumPushSp.getBoolean("has_pending_forum_push", false) ||
                    sp.getBoolean("forum_open_from_push", false)

        if (!hasPendingForumPush) {
            return false
        }

        // מנקים רק את הדגל הישן.
        // את forum_open_from_push ב-kmi_settings לא מנקים כאן,
        // כי ForumScreen צריך לקרוא אותו כדי לגלול להודעה הנכונה.
        forumPushSp.edit()
            .putBoolean("has_pending_forum_push", false)
            .apply()

        nav.navigate(Route.Forum.route) {
            launchSingleTop = true
            restoreState = false
        }

        return true
    }

    val userPrefsForEntry = remember(ctx) {
        ctx.getSharedPreferences(
            "kmi_user",
            android.content.Context.MODE_PRIVATE
        )
    }

    /*
     * אותו SharedPreferences שבו משתמש מסך הנושאים
     * לצורך בדיקת מצב המנוי.
     */
    val voiceSubscriptionPrefs = remember(ctx) {
        ctx.getSharedPreferences(
            "kmi_subs",
            android.content.Context.MODE_PRIVATE
        )
    }

    /*
     * בדיקת הגישה מתבצעת בכל פקודה מחדש, כדי שגם רכישה
     * או ביטול מנוי בזמן שהאפליקציה פתוחה ייכנסו לתוקף.
     */
    fun hasPremiumAccessFromVoice(): Boolean {
        return KmiAccess.hasFullAccess(
            userPrefsForEntry
        ) ||
                KmiAccess.hasFullAccess(
                    voiceSubscriptionPrefs
                )
    }

    /*
     * בדיקת הרשאת נושא ספציפי.
     *
     * פקודה קולית אינה יכולה לעקוף נושא נעול.
     */
    fun canOpenTopicFromVoice(
        topicTitle: String
    ): Boolean {
        val accessMode =
            AccessModeResolver.resolve(
                hasManagerAccess =
                    hasPremiumAccessFromVoice()
            )

        return LockedContentPolicy.canOpenTopic(
            accessMode = accessMode,
            title = topicTitle
        )
    }

    fun isInitialLanguageAlreadySelected(): Boolean {
        return sp.getBoolean("initial_language_selected_v4", false) ||
                userPrefsForEntry.getBoolean("initial_language_selected_v4", false) ||
                sp.getBoolean("initial_language_selected", false) ||
                userPrefsForEntry.getBoolean("initial_language_selected", false)
    }

    val langManager = remember {
        il.kmi.shared.localization.AppLanguageManager(ctx)
    }

    val isEnglish =
        langManager.getCurrentLanguage() ==
                il.kmi.shared.localization.AppLanguage.ENGLISH

    /*
     * ה־Scope נשאר פעיל גם לאחר סגירת שכבת המיקרופון,
     * ולכן ההקראה אינה מתבטלת בזמן הניווט.
     */
    val voiceFeedbackScope =
        rememberCoroutineScope()

    LaunchedEffect(ctx) {
        runCatching {
            KmiTtsManager.init(
                ctx.applicationContext
            )
        }
    }

    fun speakVoiceCommandFeedback(
        hebrewText: String,
        englishText: String
    ) {
        val feedbackText =
            if (isEnglish) {
                englishText
            } else {
                hebrewText
            }

        /*
         * פידבק חזותי מיידי מבטיח שהמשתמש יקבל אישור
         * גם אם מנוע ההקראה אינו זמין במכשיר.
         */
        Toast.makeText(
            ctx,
            feedbackText,
            Toast.LENGTH_SHORT
        ).show()

        voiceFeedbackScope.launch {
            /*
             * השהיה קצרה בלבד מאפשרת ל־SpeechRecognizer
             * לשחרר את המיקרופון, בלי לעכב את החיווי הקולי.
             *
             * מנוע ההקראה כבר מאותחל ב־LaunchedEffect,
             * ולכן אין לאתחל אותו מחדש בכל פקודה.
             */
            delay(120L)

            runCatching {
                KmiTtsManager.stop()

                KmiTtsManager.speak(
                    feedbackText
                )
            }.onFailure { error ->
                VoiceCommandDiagnosticsLogger.logFailure(
                    context = ctx,
                    source = "voice_command_feedback",
                    reason =
                        "tts_feedback_failed: " +
                                error.message.orEmpty(),
                    screenName = nav.currentBackStackEntry
                        ?.destination
                        ?.route
                )
            }
        }
    }

    /*
     * חייב להיות מוגדר לפני חיבור VoiceCommandsBridge,
     * משום שה־Bridge משנה את הערך הזה.
     */
    var showVoiceCommands by remember {
        mutableStateOf(false)
    }

    DisposableEffect(nav) {
        OnboardingBridge.bind {
            val currentRoute =
                nav.currentBackStackEntry
                    ?.destination
                    ?.route
                    .orEmpty()

            val alreadyInsideOnboarding =
                currentRoute.startsWith("onboarding")

            if (!alreadyInsideOnboarding) {
                /*
                 * פתיחה ידנית של ההדרכה מסרגל הצד:
                 * בסיום חוזרים לבית ולא למסך טעינת הפתיחה.
                 */
                sp.edit()
                    .remove("onboarding_continue_to_splash")
                    .apply()

                nav.navigate(
                    OnboardingRoute.build(
                        manual = true
                    )
                ) {
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }

        onDispose {
            OnboardingBridge.bind(null)
        }
    }

    /*
  * כל KmiTopBar יכול להפעיל או לעצור פקודה קולית,
  * אך ההאזנה עצמה מנוהלת כאן פעם אחת בלבד.
  *
  * לחיצה ראשונה:
  * false הופך ל־true והמאזין נפתח.
  *
  * לחיצה שנייה:
  * true הופך ל־false, ה־VoiceCommandListener יוצא
  * מה־Composition וה־controller נהרס ב־onDispose.
  */
    DisposableEffect(nav) {
        VoiceCommandsBridge.bind {
            showVoiceCommands =
                !showVoiceCommands
        }

        onDispose {
            VoiceCommandsBridge.bind(null)
        }
    }

// המסך הראשוני וטעינת הנתונים מנוהלים לפני MainApp ב־AndroidAppRoot.
// לאחר סיום הטעינה מותר ל־MainNavHost להתחיל ישירות בבית.
    val actualStartDestination = remember(startDestination) {
        when (startDestination) {
            Route.Home.route,
            GOOGLE_PROFILE_COMPLETION_ROUTE,
            Route.RegistrationLanding.route,
            Route.Registration.route,
            Route.NewUserTrainee.route,
            Route.NewUserCoach.route,
            Route.ExistingUserTrainee.route,
            Route.ExistingUserCoach.route -> startDestination

            else -> APP_ENTRY_ROUTE
        }
    }

    // מונע שני ניווטים רצופים אל אותו מסך כניסה בגלל recomposition / Activity recreation
    var entryNavigationLocked by remember { mutableStateOf(false) }

    fun openOnboardingBeforeStartupLoading() {
        /*
         * נשמר מחוץ ל־Compose כדי שהערך לא יתאפס
         * כאשר מחסנית הניווט נבנית מחדש.
         */
        sp.edit()
            .putBoolean(
                "onboarding_continue_to_splash",
                true
            )
            .commit()

        nav.navigate(
            OnboardingRoute.build(
                manual = false
            )
        ) {
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    fun openStartupLoading() {
        sp.edit()
            .remove("onboarding_continue_to_splash")
            .apply()

        nav.navigate(Route.Splash.route) {
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    fun openFirstStartupDestination() {
        if (OnboardingPreferences.hasCompleted(ctx)) {
            openStartupLoading()
        } else {
            openOnboardingBeforeStartupLoading()
        }
    }

// ✅ לא מבצעים preload גלובלי בעליית האפליקציה.
// רשימת המתאמנים נטענת רק בכניסה למסך CoachTraineesScreen,
// כדי לא להעמיס על כל המסכים ועל תגובת הלחיצות.

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
        com.google.firebase.auth.FirebaseAuth.getInstance()
            .currentUser
            ?.uid
            .orEmpty()
    }

    /*
     * הרשאת המנהל נבדקת מול אותו מנגנון שבו משתמש
     * סרגל הצד. כך פקודה קולית לא יכולה לעקוף
     * את הרשאות מסכי המנהל.
     */
    var isAdmin by remember(ownerUid) {
        mutableStateOf(false)
    }

    LaunchedEffect(ownerUid) {
        isAdmin =
            if (ownerUid.isBlank()) {
                false
            } else {
                runCatching {
                    AdminAccess.isCurrentUserAdmin()
                }.getOrDefault(false)
            }
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


    // 🔊 שליטה בפתיחת עוזר ה־AI הקיים
    var showAssistant by remember {
        mutableStateOf(false)
    }

    // ⚙️ FEATURE FLAG – נשאר כבוי: אין האזנה רציפה
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
            startDestination = actualStartDestination,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {

            composable(APP_ENTRY_ROUTE) {
                LaunchedEffect(Unit) {
                    val selectedNow = isInitialLanguageAlreadySelected()
                    val currentRoute = nav.currentBackStackEntry?.destination?.route

                    Log.d(
                        TAG_NAV,
                        "stage=app_entry_started, selectedLanguage=$selectedNow, currentRoute=$currentRoute, ${authStateForLog()}"
                    )

                    if (entryNavigationLocked) {
                        return@LaunchedEffect
                    }

                    entryNavigationLocked = true

                    if (!selectedNow) {

                        nav.navigate("initial_language") {
                            popUpTo(APP_ENTRY_ROUTE) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                        return@LaunchedEffect
                    }

                    /*
                     * לאחר בחירת השפה תמיד מציגים תחילה את מסך הכניסה.
                     * המסך יוצג פעם אחת בכל הפעלה חדשה של האפליקציה.
                     */
                    nav.openIntroCleanFrom(APP_ENTRY_ROUTE)
                }
            }

            composable("initial_language") {

                InitialLanguageScreen(
                    entrySp = sp,
                    onLanguageSelected = {

                        markInitialLanguageSelected(sp)

                        // אחרי שהגענו למסך אמיתי, מאפשרים ניווט עתידי תקין
                        entryNavigationLocked = false

                        nav.openIntroCleanFrom("initial_language")
                    }
                )
            }

            composable(Route.Splash.route) {
                val splashScope = rememberCoroutineScope()
                var splashFinishedLocked by remember { mutableStateOf(false) }

                KmiStartupLoadingScreen(
                    isEnglish = isEnglish,
                    onFinished = {

                        if (splashFinishedLocked) {
                            return@KmiStartupLoadingScreen
                        }

                        splashFinishedLocked = true
                        entryNavigationLocked = false

                        val firebaseUser = FirebaseAuth.getInstance().currentUser

                        Log.d(
                            TAG_NAV,
                            "stage=splash_finished, route=${Route.Splash.route}, ${authStateForLog()}"
                        )

                        // ✅ חשוב:
                        // משתמש אנונימי לא נחשב משתמש מחובר לאפליקציה.
                        // אחרת Splash מתייחס ל־anonymous uid כאילו זה משתמש אמיתי,
                        // ומנווט להשלמת פרטים / בדיקות פרופיל לפני שהמשתמש באמת התחבר.
                        if (firebaseUser == null || firebaseUser.isAnonymous) {

                            Log.d(
                                TAG_NAV,
                                "stage=splash_auth_decision, decision=intro, reason=${if (firebaseUser == null) "firebase_user_null" else "firebase_user_anonymous"}, ${authStateForLog()}"
                            )

                            nav.openIntroCleanFrom(Route.Splash.route)
                            return@KmiStartupLoadingScreen
                        }

                        Log.d(
                            TAG_NAV,
                            "stage=splash_auth_decision, decision=continue, ${authStateForLog()}"
                        )

                        // ✅ רישום שימוש באפליקציה – רק אחרי שיש משתמש אמיתי ולא אנונימי
                        splashScope.launch {
                            runCatching {
                                KmiUsageTracker.markAppOpen()
                            }
                        }

                        val uid = firebaseUser.uid

// ✅ קודם בודקים דגל מקומי שהרישום כבר הושלם.
// זה מונע חזרה לטופס בגלל missing=[belt] או שדה בודד.
                        val localProfileCompleted = isProfileCompletedLocally(sp, userPrefsForEntry, uid)

                        Log.d(
                            TAG_NAV,
                            "stage=splash_local_profile_check_result, localProfileCompleted=$localProfileCompleted, uid=$uid, ${authStateForLog()}"
                        )

                        if (localProfileCompleted) {

                            if (consumePendingDailyReminderAndNavigate("splash_local_profile_completed")) {
                                Log.d(TAG_NAV, "stage=splash_navigate_daily_reminder_from_local_profile")
                                return@KmiStartupLoadingScreen
                            }

                            if (consumePendingForumPushAndNavigate("splash_local_profile_completed")) {
                                return@KmiStartupLoadingScreen
                            }

                            /*
                             * הדרכה אוטומטית בפעם הראשונה בלבד.
                             */
                            if (!OnboardingPreferences.hasCompleted(ctx)) {

                                nav.navigate(
                                    OnboardingRoute.build(
                                        manual = false
                                    )
                                ) {
                                    popUpTo(Route.Splash.route) {
                                        inclusive = true
                                    }

                                    launchSingleTop = true
                                    restoreState = false
                                }

                                return@KmiStartupLoadingScreen
                            }

                            Log.d(
                                TAG_NAV,
                                "stage=splash_navigation_decision, decision=home, source=local_profile_completed, ${authStateForLog()}"
                            )

                            nav.navigate(Route.Home.route) {
                                popUpTo(Route.Splash.route) {
                                    inclusive = true
                                }

                                launchSingleTop = true
                                restoreState = false
                            }

                            return@KmiStartupLoadingScreen
                        }

                        splashScope.launch {
                            Log.d(
                                TAG_NAV,
                                "stage=splash_remote_profile_check_start, uid=$uid, ${authStateForLog()}"
                            )

                            val remoteCompletedResult = runCatching {
                                isProfileCompletedRemotely(uid)
                            }

                            remoteCompletedResult.onFailure { error ->
                                Log.e(
                                    TAG_NAV,
                                    "stage=splash_remote_profile_check_failure, uid=$uid, errorClass=${error.javaClass.name}, errorMessage=${error.message.orEmpty()}, ${authStateForLog()}",
                                    error
                                )
                            }

                            val remoteCompleted = remoteCompletedResult.getOrDefault(false)

                            Log.d(
                                TAG_NAV,
                                "stage=splash_remote_profile_check_result, remoteCompleted=$remoteCompleted, uid=$uid, ${authStateForLog()}"
                            )

                            if (remoteCompleted) {

                                val hydrated = runCatching {
                                    hydrateProfileLocallyFromFirestore(
                                        mainSp = sp,
                                        userSp = userPrefsForEntry,
                                        kmiPrefs = kmiPrefs,
                                        uid = uid
                                    )
                                }.getOrDefault(false)

                                markProfileCompletedLocally(sp, userPrefsForEntry, uid)

                                if (consumePendingDailyReminderAndNavigate("splash_remote_profile_completed")) {
                                    return@launch
                                }

                                if (consumePendingForumPushAndNavigate("splash_remote_profile_completed")) {
                                    return@launch
                                }

                                if (!OnboardingPreferences.hasCompleted(ctx)) {

                                    nav.navigate(
                                        OnboardingRoute.build(
                                            manual = false
                                        )
                                    ) {
                                        popUpTo(Route.Splash.route) {
                                            inclusive = true
                                        }

                                        launchSingleTop = true
                                        restoreState = false
                                    }

                                    return@launch
                                }

                                nav.navigate(Route.Home.route) {
                                    popUpTo(Route.Splash.route) { inclusive = true }
                                    launchSingleTop = true
                                    restoreState = false
                                }

                                return@launch
                            }

                            Log.d(
                                TAG_NAV,
                                "stage=splash_navigation_decision, decision=profile_completion, source=remote_profile_not_completed, ${authStateForLog()}"
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
                    markInitialLanguageSelected(sp)
                    entryNavigationLocked = false
                }

                IntroScreen(
                    /*
                     * המסך הראשוני מוצג תמיד:
                     * משתמש מחובר ממשיך לטעינה;
                     * משתמש שאינו מחובר ממשיך למסך "לקוח חדש / קיים".
                     */
                    onContinue = {

                        markInitialLanguageSelected(sp)

                        val firebaseUser =
                            FirebaseAuth.getInstance().currentUser

                        if (
                            firebaseUser != null &&
                            !firebaseUser.isAnonymous
                        ) {
                            openFirstStartupDestination()
                        } else {
                            nav.navigate(Route.RegistrationLanding.route) {
                                popUpTo(Route.Intro.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    },

                    // Google Login הצליח + הפרופיל מלא
                    // בהפעלה הראשונה מציגים הדרכה, ולאחריה טעינת נתונים.
                    onProfileComplete = {

                        markInitialLanguageSelected(sp)

                        openFirstStartupDestination()
                    },

                    // Google Login הצליח אבל חסרים פרטי KMI
                    // מדלגים על "לקוח חדש / קיים" ונכנסים ישירות להשלמת פרטים.
                    onProfileMissing = {

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
                    onHome = {
                        nav.navigate(Route.Home.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(nav.graph.startDestinationId) {
                                inclusive = false
                            }
                        }
                    },
                    onSubmit = { fullName, phone, email, subject, message ->
                        // כאן תדבר בהמשך לשרת / Firebase / Firestore
                    }
                )
            }

            composable(Route.AboutNetworkCoaches.route) {
                AboutNetworkCoachesScreen(
                    isEnglish = isEnglish,
                    onClose = {
                        nav.popBackStack()
                    },
                    onHome = {
                        nav.navigate(Route.Home.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(nav.graph.startDestinationId) {
                                inclusive = false
                            }
                        }
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
                    onOpenDrawer = { il.kmi.app.ui.DrawerBridge.open() },
                    onOpenHome = {
                        nav.navigate(Route.Home.route) {
                            launchSingleTop = true
                            restoreState = false

                            popUpTo(nav.graph.startDestinationId) {
                                inclusive = false
                            }
                        }
                    }
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
                    onOpenDrawer = {},
                    onOpenLegal = { nav.navigate(Route.Legal.route) },
                    onOpenTerms = { nav.navigate(Route.Legal.route) },
                    onRegistrationDone = {
                        openFirstStartupDestination()
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

                    // במסכי רישום / השלמת פרופיל אין פתיחת Drawer.
                    // זה מונע קפיצה של הסרגל אחרי מעבר למסך הבית.
                    onOpenDrawer = {},

                    onOpenLegal = { nav.navigate(Route.Legal.route) },
                    onOpenTerms = { nav.navigate(Route.Legal.route) },
                    onRegistrationDone = {
                        openFirstStartupDestination()
                    },
                    startAfterGoogleLogin = true
                )
            }

            // --- עריכת פרופיל מתוך מסך "הפרופיל שלי" ---
            composable(PROFILE_EDIT_ROUTE) {
                RegistrationFormScreen(
                    initial = "trainee",
                    onBack = { nav.popBackStack() },
                    onRegistrationComplete = {
                        nav.popBackStack()
                    },
                    onOpenLegal = { nav.navigate(Route.Legal.route) },
                    onOpenTerms = { nav.navigate(Route.Legal.route) },
                    vm = vm,
                    onOpenDrawer = {
                        DrawerBridge.open()
                    },
                    sp = sp,
                    kmiPrefs = kmiPrefs,
                    startAtProfile = true
                )
            }

            // --- NEW: Legal graph ---
            legalNavGraph(nav = nav)

            // --- NEW: Settings graph ---
            settingsNavGraph(
                nav = nav,
                vm = vm,
                sp = sp,
                kmiPrefs = kmiPrefs,
                themeMode = themeMode,
                onThemeChange = onThemeChange,
                onFontSizeChange =
                    onFontSizeChange
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
                onBack = {
                    nav.popBackStack()
                }
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
                nav = nav,
                vm = vm,
                isCoach = isCoach
            )

            subTopicsByTopicNavGraph(
                nav = nav,
                vm = vm,
                isCoach = isCoach
            )

            // --- NEW: Materials graph ---
            materialsNavGraph(
                nav = nav,
                vm = vm,
                sp = sp,
                kmiPrefs = kmiPrefs,
                isCoach = isCoach
            )

            // ----- לוח אימונים חודשי -----
            composable(route = Route.MonthlyCalendar.route) {
                il.kmi.app.screens.MonthlyCalendarScreen(
                    kmiPrefs = kmiPrefs,

                    onBack = {
                        nav.popBackStack()
                    },

                    onHome = {
                        nav.navigate(Route.Home.route) {
                            popUpTo(
                                nav.graph.startDestinationId
                            ) {
                                inclusive = false
                            }

                            launchSingleTop = true
                            restoreState = true
                        }
                    },

                    onDateClick = { pickedDate ->
                        nav.navigate(
                            Route.TrainingSummary.make(
                                pickedDate.toString()
                            )
                        )
                    }
                )
            }

            // ----- הפרופיל שלי -----
            composable(route = Route.MyProfile.route) {
                MyProfileScreen(
                    sp = userPrefsForEntry,
                    kmiPrefs = kmiPrefs,
                    onClose = {
                        nav.popBackStack()
                    },
                    onHome = {
                        nav.navigate(Route.Home.route) {
                            launchSingleTop = true
                            restoreState = false

                            popUpTo(nav.graph.startDestinationId) {
                                inclusive = false
                            }
                        }
                    },
                    onEditProfile = {
                        nav.navigate(PROFILE_EDIT_ROUTE) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }

            // אזור מנהל - ניהול משתמשים 🔐
            composable(route = Route.AdminUsers.route) {
                il.kmi.app.screens.admin.AdminUsersScreen(
                    onBack = {
                        nav.popBackStack()
                    },
                    onHome = {
                        nav.navigate(Route.Home.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(nav.graph.startDestinationId) {
                                inclusive = false
                            }
                        }
                    }
                )
            }

            // אזור מנהל - מרכז בקרה ולוגים 🔐
            composable(route = "admin_diagnostics") {
                AdminDiagnosticsScreen(
                    isEnglish = isEnglish,
                    onBack = {
                        nav.popBackStack()
                    },
                    onHome = {
                        nav.navigate(Route.Home.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(nav.graph.startDestinationId) {
                                inclusive = false
                            }
                        }
                    }
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

            onboardingNavGraph(
                nav = nav,
                isEnglish = isEnglish,
                onFinished = {
                    OnboardingPreferences.markCompleted(ctx)

                    val continueToSplash = sp.getBoolean(
                        "onboarding_continue_to_splash",
                        false
                    )

                    if (continueToSplash) {
                        openStartupLoading()
                    } else {
                        nav.navigate(Route.Home.route) {
                            popUpTo(0) {
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
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
                        nav.navigate(Route.Home.route) {
                            launchSingleTop = true
                            restoreState = false

                            popUpTo(nav.graph.startDestinationId) {
                                inclusive = false
                            }
                        }
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
                    onClose = { nav.popBackStack() },
                    onHome = {
                        nav.navigate(Route.Home.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(nav.graph.startDestinationId) {
                                inclusive = false
                            }
                        }
                    }
                )
            }
        }   // <-- NavHost

        if (showVoiceCommands) {
            VoiceCommandListener(
                onDismiss = {
                    showVoiceCommands = false
                },
                onCommand = commandHandler@ { command, spokenText ->
                    showVoiceCommands = false

                    VoiceCommandDiagnosticsLogger.logTrace(
                        context = ctx,
                        stage = "command_received_by_navigation",
                        spokenText = spokenText,
                        resolvedCommand =
                            command::class.simpleName ?: command.toString(),
                        screenName = nav.currentBackStackEntry
                            ?.destination
                            ?.route
                    )

                    /*
 * טיפול מקדים בפקודה משולבת של נושא וחגורה.
 *
 * הבדיקה נעשית ישירות מול המשפט שנאמר, משום שמנוע
 * הפקודות עלול לסווג משפט כזה כ־OpenBelt בלבד.
 */
                    val combinedTopicId =
                        resolveVoiceTopicId(spokenText)

                    val combinedBelt =
                        resolveVoiceBelt(spokenText)

                    if (
                        combinedTopicId != null &&
                        combinedBelt != null
                    ) {
                        vm.setSelectedBelt(combinedBelt)

                        val requestedTopicRouteValue =
                            voiceTopicRouteValue(
                                topicId = combinedTopicId
                            )

                        val topicDisplayName =
                            voiceTopicDisplayName(
                                topicId = combinedTopicId,
                                isEnglish = isEnglish
                            )

                        /*
                         * אין לבצע ניווט לפני שאימתנו שהנושא
                         * קיים בפועל בחגורה שנאמרה בפקודה.
                         */
                        val topicRouteValue =
                            resolveExistingVoiceTopicForBelt(
                                belt = combinedBelt,
                                requestedTopicTitle =
                                    requestedTopicRouteValue
                            )

                        if (topicRouteValue == null) {
                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "לא מצאתי את הנושא $topicDisplayName בחגורה המבוקשת. פותח את רשימת הנושאים בחגורה",
                                englishText =
                                    "I could not find $topicDisplayName for the requested belt. Opening the belt topics list"
                            )

                            VoiceCommandDiagnosticsLogger.logFailure(
                                context = ctx,
                                source = "main_navigation",
                                reason =
                                    "voice_topic_not_found_for_belt",
                                spokenText = spokenText,
                                alternatives = listOf(
                                    "command=CombinedTopicAndBelt",
                                    "topicId=$combinedTopicId",
                                    "requestedTopic=$requestedTopicRouteValue",
                                    "beltId=${combinedBelt.id}"
                                ),
                                screenName =
                                    nav.currentBackStackEntry
                                        ?.destination
                                        ?.route
                            )

                            vm.setSelectedBelt(combinedBelt)

                            nav.navigate(Route.BeltQ.route) {
                                launchSingleTop = true
                                restoreState = false
                            }

                            return@commandHandler
                        }

                        val beltDisplayName =
                            if (isEnglish) {
                                combinedBelt.name
                                    .lowercase()
                                    .replace('_', ' ')
                            } else {
                                combinedBelt.heb
                            }

                        /*
                         * חובה לבדוק הרשאת מנוי לפני יצירת הניווט.
                         * topicRouteValue הוא אותו שם נושא שמועבר למסך
                         * ולכן מתאים גם ל־LockedContentPolicy.
                         */
                        val canOpenRequestedTopic =
                            canOpenTopicFromVoice(
                                topicTitle = topicRouteValue
                            )

                        if (!canOpenRequestedTopic) {
                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "הנושא $topicDisplayName נעול. יש לרכוש מנוי כדי לפתוח אותו",
                                englishText =
                                    "$topicDisplayName is locked. A subscription is required to open it"
                            )

                            VoiceCommandDiagnosticsLogger.logFailure(
                                context = ctx,
                                source = "main_navigation",
                                reason = "voice_topic_blocked_by_subscription",
                                spokenText = spokenText,
                                screenName = nav.currentBackStackEntry
                                    ?.destination
                                    ?.route
                            )

                            return@commandHandler
                        }

                        val targetRoute =
                            Route.Materials.makeId(
                                beltId = combinedBelt.id,
                                topic = topicRouteValue,
                                coach = isCoach
                            )

                        speakVoiceCommandFeedback(
                            hebrewText =
                                "פותח נושא $topicDisplayName ב$beltDisplayName",
                            englishText =
                                "Opening $topicDisplayName for the $beltDisplayName belt"
                        )

                        VoiceCommandDiagnosticsLogger.logTrace(
                            context = ctx,
                            stage = "combined_topic_navigation_requested",
                            spokenText = spokenText,
                            resolvedCommand =
                                "CombinedTopicAndBelt",
                            target = targetRoute,
                            screenName = nav.currentBackStackEntry
                                ?.destination
                                ?.route
                        )

                        runCatching {
                            nav.navigate(targetRoute) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        }.onFailure { error ->
                            VoiceCommandDiagnosticsLogger.logFailure(
                                context = ctx,
                                source = "main_navigation",
                                reason =
                                    "combined_topic_navigation_failed: " +
                                            error.message.orEmpty(),
                                spokenText = spokenText,
                                screenName = nav.currentBackStackEntry
                                    ?.destination
                                    ?.route
                            )

                            Toast.makeText(
                                ctx,
                                if (isEnglish) {
                                    "Unable to open the requested topic"
                                } else {
                                    "לא ניתן לפתוח את הנושא המבוקש"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        return@commandHandler
                    }

                    when (command) {

                        VoiceAppCommand.OpenHome -> {
                            speakVoiceCommandFeedback(
                                hebrewText = "מעביר למסך הבית",
                                englishText = "Opening the home screen"
                            )

                            nav.navigate(Route.Home.route) {
                                launchSingleTop = true
                                restoreState = true

                                popUpTo(nav.graph.startDestinationId) {
                                    inclusive = false
                                }
                            }
                        }

                        VoiceAppCommand.GoBack -> {
                            speakVoiceCommandFeedback(
                                hebrewText = "חוזר למסך הקודם",
                                englishText = "Going back"
                            )

                            nav.popBackStack()
                        }

                        VoiceAppCommand.OpenSettings -> {
                            speakVoiceCommandFeedback(
                                hebrewText = "מעביר למסך ההגדרות",
                                englishText = "Opening settings"
                            )

                            DrawerBridge.openSettings()
                        }

                        VoiceAppCommand.OpenProgress -> {
                            speakVoiceCommandFeedback(
                                hebrewText = "מעביר למסך ההתקדמות",
                                englishText = "Opening progress"
                            )

                            DrawerBridge.openProgress()
                        }

                        VoiceAppCommand.OpenTrainings -> {
                            /*
                             * לוח האימונים החודשי הוא תוכן פרימיום.
                             * הפקודה הקולית חייבת לעבור דרך אותה
                             * בדיקת מנוי כמו הלחיצה על האייקון.
                             */
                            if (!hasPremiumAccessFromVoice()) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "לוח האימונים החודשי זמין למנויים בלבד. יש לרכוש מנוי כדי לפתוח אותו",
                                    englishText =
                                        "The monthly training calendar is available to subscribers only. A subscription is required"
                                )

                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason =
                                        "voice_monthly_calendar_blocked_by_subscription",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "command=OpenTrainings",
                                        "target=${Route.MonthlyCalendar.route}"
                                    ),
                                    screenName =
                                        nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                )

                                nav.navigate(Route.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                return@commandHandler
                            }

                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "מעביר למסך לוח האימונים החודשי",
                                englishText =
                                    "Opening the monthly training calendar"
                            )

                            VoiceCommandDiagnosticsLogger.logTrace(
                                context = ctx,
                                stage = "navigation_requested",
                                spokenText = spokenText,
                                resolvedCommand = "OpenTrainings",
                                target = Route.MonthlyCalendar.route,
                                screenName =
                                    nav.currentBackStackEntry
                                        ?.destination
                                        ?.route
                            )

                            nav.navigate(Route.MonthlyCalendar.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        VoiceAppCommand.OpenTrainingArchive -> {
                            if (!hasPremiumAccessFromVoice()) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "ארכיון האימונים זמין למנויים בלבד. יש לרכוש מנוי כדי לפתוח אותו",
                                    englishText =
                                        "The training archive is available to subscribers only. A subscription is required"
                                )

                                nav.navigate(Route.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                return@commandHandler
                            }

                            ctx.getSharedPreferences(
                                "kmi_voice_home_actions",
                                Context.MODE_PRIVATE
                            )
                                .edit()
                                .putBoolean(
                                    "open_training_archive",
                                    true
                                )
                                .apply()

                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "פותח את ארכיון האימונים",
                                englishText =
                                    "Opening the training archive"
                            )

                            nav.navigate(Route.Home.route) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        }

                        VoiceAppCommand.OpenFreeTrainings -> {
                            if (!hasPremiumAccessFromVoice()) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "אימונים חופשיים זמינים למנויים בלבד. יש לרכוש מנוי כדי לפתוח אותם",
                                    englishText =
                                        "Free trainings are available to subscribers only. A subscription is required"
                                )

                                nav.navigate(Route.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                return@commandHandler
                            }

                            ctx.getSharedPreferences(
                                "kmi_voice_home_actions",
                                Context.MODE_PRIVATE
                            )
                                .edit()
                                .putBoolean(
                                    "open_free_trainings",
                                    true
                                )
                                .apply()

                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "פותח את מסך האימונים החופשיים",
                                englishText =
                                    "Opening the Free Trainings screen"
                            )

                            nav.navigate(Route.Home.route) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        }

                        VoiceAppCommand.OpenTrainingSummary -> {
                            if (!hasPremiumAccessFromVoice()) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "סיכום אימון זמין למנויים בלבד. יש לרכוש מנוי כדי לפתוח אותו",
                                    englishText =
                                        "Training Summary is available to subscribers only. A subscription is required"
                                )

                                nav.navigate(Route.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                return@commandHandler
                            }

                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "פותח את מסך סיכום האימון",
                                englishText =
                                    "Opening the Training Summary screen"
                            )

                            nav.navigate(
                                Route.TrainingSummary.make()
                            ) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        VoiceAppCommand.OpenTopics -> {
                            VoiceCommandDiagnosticsLogger.logTrace(
                                context = ctx,
                                stage = "navigation_requested",
                                spokenText = spokenText,
                                resolvedCommand = "OpenTopics",
                                target = Route.Topics.route,
                                screenName = nav.currentBackStackEntry
                                    ?.destination
                                    ?.route
                            )

                            runCatching {
                                nav.navigate(Route.Topics.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }.onFailure { throwable ->
                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason = "command_execution_failed",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "command=OpenTopics",
                                        "target=${Route.Topics.route}",
                                        "error=${throwable.message.orEmpty()}"
                                    ),
                                    screenName = nav.currentBackStackEntry
                                        ?.destination
                                        ?.route
                                )

                                Toast.makeText(
                                    ctx,
                                    if (isEnglish) {
                                        "Unable to open the topics screen"
                                    } else {
                                        "לא ניתן לפתוח את מסך הנושאים"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        is VoiceAppCommand.OpenBelt -> {
                            val belt = resolveVoiceBelt(
                                command.beltQuery
                            )

                            if (belt != null) {
                                vm.setSelectedBelt(belt)

                                val beltDisplayName =
                                    if (isEnglish) {
                                        belt.name
                                            .lowercase()
                                            .replace('_', ' ')
                                    } else {
                                        belt.heb
                                    }

                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "פותח את $beltDisplayName",
                                    englishText =
                                        "Opening the $beltDisplayName belt"
                                )

                                val targetRoute = Route.BeltQ.route

                                VoiceCommandDiagnosticsLogger.logTrace(
                                    context = ctx,
                                    stage = "navigation_requested",
                                    spokenText = spokenText,
                                    resolvedCommand = "OpenBelt",
                                    target = targetRoute,
                                    screenName = nav.currentBackStackEntry
                                        ?.destination
                                        ?.route
                                )

                                /*
                                 * אם מסך החגורות כבר נמצא במחסנית,
                                 * מסירים את המופע הקיים כדי שלא יישאר
                                 * עם החגורה שנבחרה לפני הפקודה הקולית.
                                 *
                                 * לאחר מכן יוצרים את המסך מחדש והוא
                                 * קורא את החגורה החדשה מה־ViewModel.
                                 */
                                nav.popBackStack(
                                    route = targetRoute,
                                    inclusive = true
                                )

                                nav.navigate(targetRoute) {
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            } else {
                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason = "belt_not_resolved",
                                    spokenText = spokenText,
                                    screenName = nav.currentBackStackEntry
                                        ?.destination
                                        ?.route
                                )

                                Toast.makeText(
                                    ctx,
                                    if (isEnglish) {
                                        "I couldn't identify the belt"
                                    } else {
                                        "לא הצלחתי לזהות את החגורה"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        is VoiceAppCommand.OpenTopic -> {
                            val topicQuery =
                                command.topicQuery.trim()

                            val topicId =
                                resolveVoiceTopicId(topicQuery)

                            val requestedBelt =
                                resolveVoiceBelt(topicQuery)

                            val selectedBelt =
                                requestedBelt
                                    ?: vm.selectedBelt.value
                                    ?: Belt.GREEN

                            if (topicId != null) {
                                vm.setSelectedBelt(selectedBelt)

                                /*
                                 * Route.Materials הוא המסלול שרשום בפועל
                                 * ב־materialsNavGraph ופותח את MaterialsScreen
                                 * עם החגורה והנושא המבוקשים.
                                 */
                                val requestedTopicRouteValue =
                                    voiceTopicRouteValue(
                                        topicId = topicId
                                    )

                                val topicDisplayName =
                                    voiceTopicDisplayName(
                                        topicId = topicId,
                                        isEnglish = isEnglish
                                    )

                                /*
                                 * מאמתים שהנושא קיים בחגורה לפני
                                 * בדיקת המנוי ולפני יצירת המסלול.
                                 */
                                val topicRouteValue =
                                    resolveExistingVoiceTopicForBelt(
                                        belt = selectedBelt,
                                        requestedTopicTitle =
                                            requestedTopicRouteValue
                                    )

                                if (topicRouteValue == null) {
                                    speakVoiceCommandFeedback(
                                        hebrewText =
                                            "לא מצאתי את הנושא $topicDisplayName בחגורה המבוקשת. פותח את רשימת הנושאים בחגורה",
                                        englishText =
                                            "I could not find $topicDisplayName for the requested belt. Opening the belt topics list"
                                    )

                                    VoiceCommandDiagnosticsLogger.logFailure(
                                        context = ctx,
                                        source = "main_navigation",
                                        reason =
                                            "voice_topic_not_found_for_belt",
                                        spokenText = spokenText,
                                        alternatives = listOf(
                                            "command=OpenTopic",
                                            "topicQuery=$topicQuery",
                                            "topicId=$topicId",
                                            "requestedTopic=$requestedTopicRouteValue",
                                            "beltId=${selectedBelt.id}"
                                        ),
                                        screenName =
                                            nav.currentBackStackEntry
                                                ?.destination
                                                ?.route
                                    )

                                    vm.setSelectedBelt(selectedBelt)

                                    nav.navigate(Route.BeltQ.route) {
                                        launchSingleTop = true
                                        restoreState = false
                                    }

                                    return@commandHandler
                                }

                                val beltDisplayName =
                                    if (isEnglish) {
                                        selectedBelt.name
                                            .lowercase()
                                            .replace('_', ' ')
                                    } else {
                                        selectedBelt.heb
                                    }

                                val canOpenRequestedTopic =
                                    canOpenTopicFromVoice(
                                        topicTitle = topicRouteValue
                                    )

                                if (!canOpenRequestedTopic) {
                                    /*
                                     * אין לבצע nav.navigate כאשר הנושא נעול.
                                     * הפידבק מוצג ונאמר בקול באמצעות אותה פונקציה.
                                     */
                                    speakVoiceCommandFeedback(
                                        hebrewText =
                                            "הנושא $topicDisplayName נעול. יש לרכוש מנוי כדי לפתוח אותו",
                                        englishText =
                                            "$topicDisplayName is locked. A subscription is required to open it"
                                    )

                                    VoiceCommandDiagnosticsLogger.logFailure(
                                        context = ctx,
                                        source = "main_navigation",
                                        reason =
                                            "voice_topic_blocked_by_subscription",
                                        spokenText = spokenText,
                                        alternatives = listOf(
                                            "command=OpenTopic",
                                            "topicQuery=$topicQuery",
                                            "topicId=$topicId",
                                            "beltId=${selectedBelt.id}",
                                            "topicTitle=$topicRouteValue"
                                        ),
                                        screenName = nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                    )
                                } else {
                                    val targetRoute =
                                        Route.Materials.makeId(
                                            beltId = selectedBelt.id,
                                            topic = topicRouteValue,
                                            coach = isCoach
                                        )

                                    speakVoiceCommandFeedback(
                                        hebrewText =
                                            "פותח נושא $topicDisplayName ב$beltDisplayName",
                                        englishText =
                                            "Opening $topicDisplayName for the $beltDisplayName belt"
                                    )

                                    VoiceCommandDiagnosticsLogger.logTrace(
                                        context = ctx,
                                        stage = "navigation_requested",
                                        spokenText = spokenText,
                                        resolvedCommand = "OpenTopic",
                                        target = targetRoute,
                                        screenName = nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                    )

                                    runCatching {
                                        nav.navigate(targetRoute) {
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    }.onSuccess {
                                        VoiceCommandDiagnosticsLogger.logTrace(
                                            context = ctx,
                                            stage = "topic_navigation_succeeded",
                                            spokenText = spokenText,
                                            resolvedCommand = "OpenTopic",
                                            target = targetRoute,
                                            screenName = nav.currentBackStackEntry
                                                ?.destination
                                                ?.route
                                        )
                                    }.onFailure { throwable ->
                                        VoiceCommandDiagnosticsLogger.logFailure(
                                            context = ctx,
                                            source = "main_navigation",
                                            reason = "topic_navigation_failed",
                                            spokenText = spokenText,
                                            alternatives = listOf(
                                                "command=OpenTopic",
                                                "topicQuery=$topicQuery",
                                                "topicId=$topicId",
                                                "beltId=${selectedBelt.id}",
                                                "target=$targetRoute",
                                                "error=${throwable.message.orEmpty()}"
                                            ),
                                            screenName = nav.currentBackStackEntry
                                                ?.destination
                                                ?.route
                                        )

                                        Toast.makeText(
                                            ctx,
                                            if (isEnglish) {
                                                "Unable to open the requested topic"
                                            } else {
                                                "לא ניתן לפתוח את הנושא המבוקש"
                                            },
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason = "topic_not_resolved",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "topicQuery=$topicQuery",
                                        "beltId=${selectedBelt.id}"
                                    ),
                                    screenName = nav.currentBackStackEntry
                                        ?.destination
                                        ?.route
                                )

                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "לא הצלחתי לזהות את הנושא המבוקש",
                                    englishText =
                                        "I could not identify the requested topic"
                                )

                                /*
 * גם כאשר שם הנושא כלל לא זוהה,
 * מציגים למשתמש את הנושאים הקיימים
 * בחגורה במקום להשאיר אותו במסך הנוכחי.
 */
                                vm.setSelectedBelt(selectedBelt)

                                nav.navigate(Route.BeltQ.route) {
                                    launchSingleTop = true
                                    restoreState = false
                                }

                                Toast.makeText(
                                    ctx,
                                    if (isEnglish) {
                                        "I couldn't identify the topic"
                                    } else {
                                        "לא הצלחתי לזהות את הנושא"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        is VoiceAppCommand.OpenDrawerItem -> {
                            val destination =
                                command.destination

                            val destinationName =
                                destination.name

                            VoiceCommandDiagnosticsLogger.logTrace(
                                context = ctx,
                                stage = "drawer_action_requested",
                                spokenText = spokenText,
                                resolvedCommand = "OpenDrawerItem",
                                target = destinationName,
                                screenName =
                                    nav.currentBackStackEntry
                                        ?.destination
                                        ?.route
                            )

                            /*
                             * מסכי נוכחות ודוח תשלומים מיועדים למאמן.
                             * מתאמן יקבל הודעה קולית ולא יישלח למסך
                             * שאין לו הרשאה לפתוח.
                             */
                            val isProtectedCoachDestination =
                                destination ==
                                        VoiceDrawerDestination.COACH_ATTENDANCE ||
                                        destination ==
                                        VoiceDrawerDestination.COACH_BROADCAST ||
                                        destination ==
                                        VoiceDrawerDestination.COACH_TRAINEES ||
                                        destination ==
                                        VoiceDrawerDestination.COACH_PAYMENTS_REPORT ||
                                        destination ==
                                        VoiceDrawerDestination.COACH_INTERNAL_EXAM

                            if (
                                isProtectedCoachDestination &&
                                !isCoach
                            ) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "המסך המבוקש זמין למאמנים מורשים בלבד",
                                    englishText =
                                        "The requested screen is available to authorized coaches only"
                                )

                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason =
                                        "coach_destination_blocked_for_trainee",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "destination=$destinationName",
                                        "isCoach=false"
                                    ),
                                    screenName =
                                        nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                )

                                return@commandHandler
                            }

                            val performed =
                                DrawerVoiceActionsBridge.perform(
                                    destination
                                )

                            if (performed) {
                                /*
                                 * החיווי נאמר לאחר שהפעולה התקבלה.
                                 * ה־Scope של ההקראה נשאר פעיל גם
                                 * לאחר המעבר למסך החדש.
                                 */
                                /*
      * לכל יעד בסרגל הצד מוגדר חיווי אחיד.
      * לאחר שהפעולה התקבלה מוצגת הודעה
      * ונאמר בקול איזה מסך נפתח.
      */
                                val destinationFeedback =
                                    when (destination) {
                                        VoiceDrawerDestination.MY_PROFILE ->
                                            "פותח את מסך הפרופיל שלי" to
                                                    "Opening My Profile"

                                        VoiceDrawerDestination.COACH_ATTENDANCE ->
                                            "פותח את מסך עדכון הנוכחות" to
                                                    "Opening the attendance update screen"

                                        VoiceDrawerDestination.COACH_BROADCAST ->
                                            "פותח את מסך שליחת ההודעה" to
                                                    "Opening the message broadcast screen"

                                        VoiceDrawerDestination.COACH_TRAINEES ->
                                            "פותח את מסך רשימת המתאמנים" to
                                                    "Opening the trainees list"

                                        VoiceDrawerDestination.COACH_PAYMENTS_REPORT ->
                                            "פותח את מסך דוח התשלומים" to
                                                    "Opening the payments report"

                                        VoiceDrawerDestination.COACH_INTERNAL_EXAM ->
                                            "פותח את מסך המבחן הפנימי לחגורה" to
                                                    "Opening the internal belt exam"

                                        VoiceDrawerDestination.ADMIN_USERS ->
                                            "פותח את מסך ניהול המשתמשים" to
                                                    "Opening user management"

                                        VoiceDrawerDestination.ADMIN_DIAGNOSTICS ->
                                            "פותח את מרכז הבקרה והלוגים" to
                                                    "Opening the control center and logs"

                                        VoiceDrawerDestination.ABOUT_AVI ->
                                            "פותח את מסך אודות אבי אביסידון, ראש השיטה" to
                                                    "Opening the About Avi Avisidon screen"

                                        VoiceDrawerDestination.NETWORK_COACHES ->
                                            "פותח את מסך המאמנים ברשת" to
                                                    "Opening the network coaches screen"

                                        VoiceDrawerDestination.ABOUT_METHOD ->
                                            "פותח את מסך אודות השיטה" to
                                                    "Opening the About the Method screen"

                                        VoiceDrawerDestination.EXERCISES_DEMO ->
                                            "פותח את תרגילי ההדגמה" to
                                                    "Opening the exercise demonstrations"

                                        VoiceDrawerDestination.FORMS_AND_PAYMENTS ->
                                            "פותח את מסך הטפסים והתשלומים" to
                                                    "Opening forms and payments"

                                        VoiceDrawerDestination.CONTACT_US ->
                                            "פותח את מסך צור קשר" to
                                                    "Opening Contact Us"

                                        VoiceDrawerDestination.BRANCH_FORUM ->
                                            "פותח את פורום הסניף" to
                                                    "Opening the branch forum"

                                        VoiceDrawerDestination.LANGUAGE_HEBREW ->
                                            "שפת האפליקציה הוחלפה לעברית" to
                                                    "The application language was changed to Hebrew"

                                        VoiceDrawerDestination.LANGUAGE_ENGLISH ->
                                            "שפת האפליקציה הוחלפה לאנגלית" to
                                                    "The application language was changed to English"

                                        VoiceDrawerDestination.LANGUAGE -> {
                                            if (isEnglish) {
                                                "שפת האפליקציה הוחלפה לעברית" to
                                                        "The application language was changed to Hebrew"
                                            } else {
                                                "שפת האפליקציה הוחלפה לאנגלית" to
                                                        "The application language was changed to English"
                                            }
                                        }

                                        VoiceDrawerDestination.MANAGE_SUBSCRIPTION ->
                                            "פותח את מסך ניהול המנוי" to
                                                    "Opening subscription management"

                                        VoiceDrawerDestination.RATE_US ->
                                            "פותח את מסך דירוג האפליקציה" to
                                                    "Opening the application rating screen"

                                        VoiceDrawerDestination.LOGOUT ->
                                            "פותח את אישור ההתנתקות" to
                                                    "Opening the logout confirmation"
                                    }

                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        destinationFeedback.first,
                                    englishText =
                                        destinationFeedback.second
                                )

                                VoiceCommandDiagnosticsLogger.logTrace(
                                    context = ctx,
                                    stage = "drawer_action_dispatched",
                                    spokenText = spokenText,
                                    resolvedCommand = "OpenDrawerItem",
                                    target = destinationName,
                                    screenName =
                                        nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                )
                            } else {
                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason =
                                        "drawer_action_not_connected",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "destination=$destinationName"
                                    ),
                                    screenName =
                                        nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                )

                                /*
                                 * גם כישלון הפעולה מקבל חיווי חזותי וקולי,
                                 * במקום Toast בלבד.
                                 */
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "הפעולה הקולית עדיין אינה זמינה",
                                    englishText =
                                        "This voice action is not available yet"
                                )
                            }
                        }

                        VoiceAppCommand.OpenWeakPoints -> {
                            if (!hasPremiumAccessFromVoice()) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "מסך נקודות תורפה זמין למנויים בלבד. יש לרכוש מנוי כדי לפתוח אותו",
                                    englishText =
                                        "The Weak Points screen is available to subscribers only. A subscription is required"
                                )

                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason =
                                        "voice_quick_menu_blocked_by_subscription",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "command=OpenWeakPoints"
                                    ),
                                    screenName =
                                        nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                )

                                nav.navigate(Route.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                return@commandHandler
                            }

                            val selectedBelt =
                                vm.selectedBelt.value
                                    ?: Belt.GREEN

                            vm.setSelectedBelt(selectedBelt)

                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "פותח את מסך נקודות התורפה",
                                englishText =
                                    "Opening the Weak Points screen"
                            )

                            nav.navigate(Route.WeakPoints.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        VoiceAppCommand.OpenAllLists -> {
                            if (!hasPremiumAccessFromVoice()) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "מסך כל הרשימות זמין למנויים בלבד. יש לרכוש מנוי כדי לפתוח אותו",
                                    englishText =
                                        "The All Lists screen is available to subscribers only. A subscription is required"
                                )

                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason =
                                        "voice_quick_menu_blocked_by_subscription",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "command=OpenAllLists"
                                    ),
                                    screenName =
                                        nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                )

                                nav.navigate(Route.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                return@commandHandler
                            }

                            val selectedBelt =
                                vm.selectedBelt.value
                                    ?: Belt.GREEN

                            vm.setSelectedBelt(selectedBelt)

                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "פותח את מסך כל הרשימות",
                                englishText =
                                    "Opening the All Lists screen"
                            )

                            nav.navigate(
                                "ex_tabs_all/${selectedBelt.id}"
                            ) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        VoiceAppCommand.OpenPractice -> {
                            if (!hasPremiumAccessFromVoice()) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "מסך התרגול זמין למנויים בלבד. יש לרכוש מנוי כדי לפתוח אותו",
                                    englishText =
                                        "The Practice screen is available to subscribers only. A subscription is required"
                                )

                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason =
                                        "voice_quick_menu_blocked_by_subscription",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "command=OpenPractice"
                                    ),
                                    screenName =
                                        nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                )

                                nav.navigate(Route.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                return@commandHandler
                            }

                            val selectedBelt =
                                vm.selectedBelt.value
                                    ?: Belt.GREEN

                            vm.setSelectedBelt(selectedBelt)

                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "פותח את מסך התרגול",
                                englishText =
                                    "Opening the Practice screen"
                            )

                            nav.navigate(
                                Route.Practice.make(selectedBelt)
                            ) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        VoiceAppCommand.OpenExerciseSummary -> {
                            if (!hasPremiumAccessFromVoice()) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "מסך הסיכום זמין למנויים בלבד. יש לרכוש מנוי כדי לפתוח אותו",
                                    englishText =
                                        "The Summary screen is available to subscribers only. A subscription is required"
                                )

                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason =
                                        "voice_quick_menu_blocked_by_subscription",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "command=OpenExerciseSummary"
                                    ),
                                    screenName =
                                        nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                )

                                nav.navigate(Route.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                return@commandHandler
                            }

                            val selectedBelt =
                                vm.selectedBelt.value
                                    ?: Belt.GREEN

                            vm.setSelectedBelt(selectedBelt)

                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "פותח את מסך הסיכום",
                                englishText =
                                    "Opening the Summary screen"
                            )

                            nav.navigate(
                                Route.Summary.make(selectedBelt)
                            ) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        VoiceAppCommand.OpenVoiceAssistant -> {
                            if (!hasPremiumAccessFromVoice()) {
                                speakVoiceCommandFeedback(
                                    hebrewText =
                                        "העוזר הקולי זמין למנויים בלבד. יש לרכוש מנוי כדי לפתוח אותו",
                                    englishText =
                                        "The Voice Assistant is available to subscribers only. A subscription is required"
                                )

                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason =
                                        "voice_quick_menu_blocked_by_subscription",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "command=OpenVoiceAssistant"
                                    ),
                                    screenName =
                                        nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                )

                                nav.navigate(Route.Subscription.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                return@commandHandler
                            }

                            speakVoiceCommandFeedback(
                                hebrewText =
                                    "פותח את העוזר הקולי",
                                englishText =
                                    "Opening the Voice Assistant"
                            )

                            nav.navigate(Route.VoiceAssistant.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        VoiceAppCommand.OpenBelts -> {
                            nav.navigate(Route.BeltQ.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        VoiceAppCommand.OpenSearch -> {
                            Toast.makeText(
                                ctx,
                                if (isEnglish) {
                                    "Voice search will be connected in the next step"
                                } else {
                                    "החיפוש הקולי יחובר בשלב הבא"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is VoiceAppCommand.ExplainExercise -> {
                            val exerciseQuery =
                                command.query.trim()

                            val explanationOpened =
                                runCatching {
                                    VoiceExerciseExplanationBridge.openExplanation(
                                        exerciseQuery
                                    )
                                }.getOrElse { throwable ->
                                    VoiceCommandDiagnosticsLogger.logFailure(
                                        context = ctx,
                                        source = "main_navigation",
                                        reason = "exercise_explanation_bridge_failed",
                                        spokenText = spokenText,
                                        alternatives = listOf(
                                            "command=ExplainExercise",
                                            "exerciseQuery=$exerciseQuery",
                                            "error=${throwable.message.orEmpty()}"
                                        ),
                                        screenName = nav.currentBackStackEntry
                                            ?.destination
                                            ?.route
                                    )

                                    false
                                }

                            if (explanationOpened) {
                                VoiceCommandDiagnosticsLogger.logTrace(
                                    context = ctx,
                                    stage = "exercise_explanation_opened",
                                    spokenText = spokenText,
                                    resolvedCommand = "ExplainExercise",
                                    target = exerciseQuery,
                                    screenName = nav.currentBackStackEntry
                                        ?.destination
                                        ?.route
                                )
                            } else {
                                VoiceCommandDiagnosticsLogger.logFailure(
                                    context = ctx,
                                    source = "main_navigation",
                                    reason = "exercise_explanation_not_opened",
                                    spokenText = spokenText,
                                    alternatives = listOf(
                                        "command=ExplainExercise",
                                        "exerciseQuery=$exerciseQuery",
                                        "bridgeHandlerAvailable=false_or_exercise_not_found"
                                    ),
                                    screenName = nav.currentBackStackEntry
                                        ?.destination
                                        ?.route
                                )

                                Toast.makeText(
                                    ctx,
                                    if (isEnglish) {
                                        "I couldn't find an explanation for that exercise"
                                    } else {
                                        "לא הצלחתי למצוא הסבר לתרגיל"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        is VoiceAppCommand.FindAndOpen,
                        is VoiceAppCommand.Search -> {
                            Toast.makeText(
                                ctx,
                                if (isEnglish) {
                                    "Recognized: $spokenText"
                                } else {
                                    "זוהתה הפקודה: $spokenText"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is VoiceAppCommand.Unknown -> {
                            Toast.makeText(
                                ctx,
                                if (isEnglish) {
                                    "I couldn't understand the command"
                                } else {
                                    "לא הצלחתי להבין את הפקודה"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }
}

private fun isProfileCompletedLocally(
    mainSp: SharedPreferences,
    userSp: SharedPreferences,
    uid: String
): Boolean {
    if (uid.isBlank()) return false

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

    val email = getStringAny("email")
        .ifBlank { getStringAny("user_email") }
        .trim()

    val phone = (
            getStringAny("phone")
                .ifBlank { getStringAny("phone_number") }
            ).filter { it.isDigit() }

    // ✅ תנאי כניסה בסיסי:
    // אחרי Firebase Auth מספיקים אימייל + טלפון.
    // שאר הפרטים לא אמורים לחסום כניסה לאפליקציה.
    val canEnterApp =
        email.isNotBlank() &&
                phone.length >= 9

    return uidMatches && canEnterApp
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

}

private suspend fun isProfileCompletedRemotely(uid: String): Boolean {
    if (uid.isBlank()) return false

    val doc = Firebase.firestore
        .collection("users")
        .document(uid)
        .get()
        .await()

    if (!doc.exists()) {
        return false
    }

    val email = doc.getString("email").orEmpty().trim()

    val phone = (
            doc.getString("phone")
                ?: doc.getString("phoneNumber")
                ?: doc.getString("phone_number")
                ?: ""
            ).filter { it.isDigit() }

    // ✅ תנאי כניסה בסיסי מהשרת:
    // Firebase Auth כבר אימת את המשתמש.
    // כדי להיכנס לאפליקציה מספיקים אימייל + טלפון במסמך users/{uid}.
    val canEnterApp =
        email.isNotBlank() &&
                phone.length >= 9

    return canEnterApp
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
