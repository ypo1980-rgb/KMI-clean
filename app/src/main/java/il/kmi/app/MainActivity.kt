package il.kmi.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.subscription.BillingRepository


// Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import il.kmi.app.notifications.CoachGate
import il.kmi.app.screens.CoachMessageGateScreen

// Screens / App
import il.kmi.app.screens.IntroScreen
import il.kmi.app.ui.KmiTtsManager
import il.yuval.ui.theme.AppTheme

// 👇 חדש: מיגרציית העדפות ל-KMP (חוצה-פלטפורמות)
import il.kmi.shared.prefs.LegacyPrefsMigration

// --------------------------------------------------------------

private const val SUPPRESS_NEXT_DRAWER_OPEN_KEY = "kmi_suppress_next_drawer_open"

/**
 * אנדרואיד-נטו: נקודת כניסה דקה שמפעילה UI.
 * מטרת “חוצה-פלטפורמות” בקונטקסט של KMP:
 * 1) להשאיר את נקודת הכניסה הספציפית לפלטפורמה כאן.
 * 2) להזיז בהדרגה לוגיקה/מצב/מודלים ל־shared (commonMain).
 * 3) ליצור בעתיד נקודת כניסה מקבילה ל־iOS שקוראת לאותה לוגיקה משותפת.
 */
class MainActivity : androidx.fragment.app.FragmentActivity() {

    private var billingRepository: BillingRepository? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)
        // 🌍 Language Manager
        val languageManager = AppLanguageManager(this)
        val localizedContext = languageManager.applySavedLanguage(this)

        ensureNotificationPermission()

        // 👇 חדש: קליטה מהתראה (גם כשהאפליקציה סגורה)
        handleCoachGateIntent(intent)

        // ✅ חובה: אתחול מנהל ה-TTS כדי ש-speak() לא ייעצר לפני init()
        KmiTtsManager.init(this)

        // ---- SharedPreferences ----
        val sp     = getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
        val userSp = getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

        // ---- KMP Settings (Multiplatform) ----
        val kmiPrefs = il.kmi.shared.prefs.KmiPrefsFactory.create(context = this)

        // 👇 מיגרציה חד־פעמית מ-SharedPreferences ל-KMP Settings
        LegacyPrefsMigration.run(context = this)

        // 👇 אתחול שכבת הפלטפורמה (expect/actual)
        il.kmi.shared.Platform.init(appContext = applicationContext)

        // ✅ בזמן בדיקות לא מפעילים Billing אוטומטית בכניסה לאפליקציה.
        // מנוי ייבדק רק בלחיצה מפורשת על רכישה / שחזור רכישות.
        billingRepository = null

        // ---- Side effects אנדרואידיים (אפשר יהיה לארוז לשכבת Platform בהמשך) ----

        // תזמון תזכורות אימון – KMP הוא מקור האמת
        val remindersOn = kmiPrefs.remindersOn
        val lead = kmiPrefs.leadMinutes.takeIf { it > 0 } ?: 60
        if (remindersOn) {
            il.kmi.shared.Platform.scheduleWeeklyTrainingAlarms(lead)
        } else {
            il.kmi.shared.Platform.cancelWeeklyTrainingAlarms()
        }

        // DataStore / ViewModel – בשלב הזה נשארים באנדרואיד
        val ds = DataStoreManager(this)
        // ✅ NEW: Local repo לסיכומי אימון (SharedPreferences)
        val spTrainingSummary = getSharedPreferences("kmi_training_summary", Context.MODE_PRIVATE)
        val trainingSummaryLocalRepo = il.kmi.app.data.training.TrainingSummaryLocalRepo(spTrainingSummary)

// ✅ FIX: KmiViewModel דורש גם trainingSummaryLocalRepo
        val vm = KmiViewModel(
            ds = ds,
            trainingSummaryLocalRepo = trainingSummaryLocalRepo
        )


        // Firebase אנונימי + סנכרון branchId (אנדרואיד-נטו כרגע)
        ensureAnonymousAuthAndSyncBranch(userSp)

        // -------------------- UI --------------------
        setContent {

            // 1) טוענים את המצב הראשוני.
            // ברירת מחדל חדשה: מצב בהיר.
            // כל ערך ישן כמו "system" מומר ל-light כדי לא ללכת לפי מצב המכשיר.
            val rawThemeMode = kmiPrefs.themeMode
                .ifBlank { sp.getString("theme_mode", "") ?: "" }
                .ifBlank { "light" }

            val normalizedThemeMode = when (rawThemeMode) {
                "dark" -> "dark"
                "light" -> "light"
                else -> "light"
            }

            var themeMode by remember {
                mutableStateOf(normalizedThemeMode)
            }

            LaunchedEffect(Unit) {
                if (
                    kmiPrefs.themeMode != normalizedThemeMode ||
                    sp.getString("theme_mode", "") != normalizedThemeMode
                ) {
                    kmiPrefs.themeMode = normalizedThemeMode
                    sp.edit()
                        .putString("theme_mode", normalizedThemeMode)
                        .apply()
                }
            }

            // 2) פונקציה שמשנה גם את ה־State וגם נשמרת ב-SP וב-KMP
            fun onThemeChange(mode: String) {
                val normalized = when (mode) {
                    "dark" -> "dark"
                    "light" -> "light"
                    "system" -> "system"
                    else -> "light"
                }

                themeMode = normalized
                kmiPrefs.themeMode = normalized
                sp.edit().putString("theme_mode", normalized).apply()
            }

            // 3) מחשבים darkTheme אמיתי.
            // ברירת המחדל היא light; רק בחירה מפורשת dark מפעילה מצב כהה.
            val darkTheme = when (themeMode) {
                "dark" -> true
                "system" -> isSystemInDarkTheme()
                else -> false
            }

            // 4) נותנים לערכת הנושא את darkTheme
            AppTheme(darkTheme = darkTheme) {
                AndroidAppRoot(
                    sp = sp,
                    userSp = userSp,
                    vm = vm,
                    kmiPrefs = kmiPrefs,
                    themeMode = themeMode,
                    onThemeChange = ::onThemeChange
                )
            }
        }
    }

    // 👇 חדש: כשהאפליקציה כבר פתוחה/ברקע ולוחצים על ההתראה
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCoachGateIntent(intent)
    }

    // 👇 חדש: שומר “הודעה ממתינה להצגה” ב-SharedPreferences ייעודי
    private fun handleCoachGateIntent(i: Intent?) {
        val intent = i ?: return
        val open = intent.getBooleanExtra(CoachGate.EXTRA_OPEN, false)
        if (!open) return

        val gateSp = getSharedPreferences(CoachGate.SP_NAME, Context.MODE_PRIVATE)

        val text = intent.getStringExtra(CoachGate.EXTRA_TEXT).orEmpty()
        val from = intent.getStringExtra(CoachGate.EXTRA_FROM).orEmpty()
        val sentAt = intent.getLongExtra(CoachGate.EXTRA_SENT_AT, 0L)
        val broadcastId = intent.getStringExtra(CoachGate.EXTRA_BROADCAST_ID).orEmpty()

        gateSp.edit()
            .putBoolean(CoachGate.SP_HAS_PENDING, true)
            .putString(CoachGate.SP_TEXT, text)
            .putString(CoachGate.SP_FROM, from)
            .putLong(CoachGate.SP_SENT_AT, sentAt)
            .putString(CoachGate.SP_BROADCAST_ID, broadcastId)
            .apply()
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
    }

    /**
     * בכל פתיחה "קרה" של האפליקציה נכפה אימות (אם מוגדרת נעילה).
     * כך אחרי סגירה ופתיחה מחדש תמיד תתבקש סיסמה / ביומטרי.
     */
    override fun onStart() {
        super.onStart()
        enforceAppLockIfNeeded(force = true)
    }

    /**
     * בחזרה מרקע (lock-screen / app switcher) – מפעיל את מנגנון הנעילה
     * לפי הלוגיקה הפנימית של AppLock (טיימאאוט וכו'), בלי לכפות כל פעם.
     */
    override fun onResume() {
        super.onResume()

        // בזמן בדיקות לא מרעננים מנוי אוטומטית בחזרה לאפליקציה.
        // רענון יתבצע רק בלחיצה מפורשת על שחזור רכישות.
        enforceAppLockIfNeeded(force = false)
    }

    /**
     * בודק את מצב הנעילה ב-SharedPreferences ואם אינו "none"
     * מפעיל את AppLock.requireIfNeeded.
     */
    private fun enforceAppLockIfNeeded(force: Boolean) {
        val sp = getSharedPreferences("kmi_settings", MODE_PRIVATE)
        val mode = sp.getString("app_lock_mode", "none") ?: "none"
        if (mode != "none") {
            runCatching {
                il.kmi.app.security.AppLock.requireIfNeeded(this, force)
            }
        }
    }

    /**
     * התחברות אנונימית לפיירבייס, וסנכרון branchId אם קיים ב־SharedPreferences.
     * (יישאר אנדרואיד-ספציפי עד שנוסיף שכבת Platform ל־iOS)
     */
    private fun ensureAnonymousAuthAndSyncBranch(userSp: android.content.SharedPreferences) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    // תמיכה בשני מפתחות: branch_id (ישן) ו-branchId (חדש)
                    val branchId = userSp.getString("branch_id", null)
                        ?: userSp.getString("branchId", null)
                        ?: return@addOnSuccessListener
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(hashMapOf("branchId" to branchId), SetOptions.merge())
                }
        } else {
            val branchId = userSp.getString("branch_id", null)
                ?: userSp.getString("branchId", null)
                ?: return
            val uid = auth.currentUser?.uid ?: return
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(hashMapOf("branchId" to branchId), SetOptions.merge())
        }
    }

    companion object {

        private const val REQUEST_CODE_POST_NOTIFICATIONS = 41024

        /**
         * קריאה מכפתור "התנתק":
         * מבצע Restart "קר" לאפליקציה (ניקוי ה־Back Stack והפעלה מחדש של MainActivity),
         * כך ש־onStart() ירוץ שוב ויכפה הקלדת קוד / ביומטרי לפי ההגדרות.
         */
        fun restartAndForceLock(context: Context) {
            val intent = android.content.Intent(
                context,
                MainActivity::class.java
            ).apply {
                flags =
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }
}

/**
 * קומפוזבּל שורש ל־Android שמרכיב את הניווט/מסכים שלך.
 * את החלק הזה נוכל להעביר מאוחר יותר ל־shared (commonMain) כמעט 1:1.
 */
@Composable
private fun AndroidAppRoot(
    sp: SharedPreferences,
    userSp: SharedPreferences,
    vm: KmiViewModel,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs,
    themeMode: String,
    onThemeChange: (String) -> Unit
){
    // ✅ חדש: Gate SP (הודעת מאמן לפני כניסה)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val gateSp = remember { ctx.getSharedPreferences(CoachGate.SP_NAME, Context.MODE_PRIVATE) }

    var hasPendingGate by remember {
        mutableStateOf(gateSp.getBoolean(CoachGate.SP_HAS_PENDING, false))
    }

    DisposableEffect(gateSp) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == CoachGate.SP_HAS_PENDING) {
                hasPendingGate = gateSp.getBoolean(CoachGate.SP_HAS_PENDING, false)
            }
        }
        gateSp.registerOnSharedPreferenceChangeListener(l)
        onDispose { gateSp.unregisterOnSharedPreferenceChangeListener(l) }
    }

    // ✅ אם יש הודעה ממתינה — מציגים כרטיס לפני Intro/Login
    if (hasPendingGate) {
        CoachMessageGateScreen(
            onApprove = {
                gateSp.edit()
                    .putBoolean(CoachGate.SP_HAS_PENDING, false)
                    .remove(CoachGate.SP_TEXT)
                    .remove(CoachGate.SP_FROM)
                    .remove(CoachGate.SP_SENT_AT)
                    .remove(CoachGate.SP_BROADCAST_ID)
                    .apply()
            }
        )
        return
    }

    // 🌍 בדיקה אם נבחרה שפה בפעם הראשונה
    var initialLanguageSelected by remember {
        mutableStateOf(userSp.getBoolean("initial_language_selected", false))
    }

    // האם קיים “משתמש רשום” (קודם KMP, עם פולבאק ל-SP הישן)
    var isRegistered by remember {
        mutableStateOf(
            (
                    (kmiPrefs.fullName?.isNotBlank() == true) &&
                            (kmiPrefs.phone?.isNotBlank() == true) &&
                            (kmiPrefs.email?.isNotBlank() == true) &&
                            (kmiPrefs.region?.isNotBlank() == true) &&
                            (kmiPrefs.branch?.isNotBlank() == true) &&
                            (kmiPrefs.username?.isNotBlank() == true) &&
                            (kmiPrefs.password?.isNotBlank() == true)
                    ) || (
                    userSp.contains("fullName") &&
                            userSp.contains("phone") &&
                            userSp.contains("email") &&
                            userSp.contains("region") &&
                            userSp.contains("branch") &&
                            userSp.contains("username") &&
                            userSp.contains("password")
                    )
        )
    }

// מסך פתיחה
    var currentScreen by remember {
        mutableStateOf(
            when {
                !initialLanguageSelected -> "language"
                isRegistered -> "main"
                else -> "intro"
            }
        )
    }

// מסלול פתיחה חד-פעמי ל־MainApp
    var startRoute by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {

        "language" -> {
            il.kmi.app.screens.InitialLanguageScreen(
                onLanguageSelected = {
                    currentScreen = if (isRegistered) "main" else "intro"
                }
            )
        }

        "intro" -> {
            IntroScreen(
                // כניסה רגילה / רישום בדרך הרגילה
                onContinue = {
                    android.util.Log.e(
                        "KMI_INTRO_FLOW",
                        "regular login / register button clicked"
                    )

                    currentScreen = if (isRegistered) "main" else "register"
                },

                // Google Login הצליח והפרופיל מלא
                // מדלגים על מסך משתמש חדש / משתמש קיים ונכנסים לבית
                onProfileComplete = {
                    android.util.Log.e(
                        "KMI_INTRO_FLOW",
                        "google profile complete -> main/home"
                    )

                    startRoute = Route.Home.route
                    currentScreen = "main"
                },

                // Google Login הצליח אבל חסרים פרטי KMI
                // מדלגים על מסך משתמש חדש / משתמש קיים ונכנסים ישר להשלמת פרטים
                onProfileMissing = {
                    android.util.Log.e(
                        "KMI_INTRO_FLOW",
                        "google profile missing -> google_profile_completion"
                    )

                    startRoute = "google_profile_completion"
                    currentScreen = "main"
                }
            )
        }

        "register" -> {
            MainApp(
                sp = sp,
                vm = vm,
                startRoute = Route.RegistrationLanding.route,
                kmiPrefs = kmiPrefs
            )
        }

        "main" -> {
            LaunchedEffect(Unit) {
                android.util.Log.e(
                    "KMI_INTRO_FLOW",
                    "entered main from intro - drawer open should be suppressed"
                )
            }

            MainApp(
                sp = sp,
                vm = vm,
                startRoute = startRoute,
                kmiPrefs = kmiPrefs
            )
        }
    }
}
