package il.kmi.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import il.kmi.app.data.training.TrainingSummaryLocalRepo

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

/**
 * אנדרואיד-נטו: נקודת כניסה דקה שמפעילה UI.
 * מטרת “חוצה-פלטפורמות” בקונטקסט של KMP:
 * 1) להשאיר את נקודת הכניסה הספציפית לפלטפורמה כאן.
 * 2) להזיז בהדרגה לוגיקה/מצב/מודלים ל־shared (commonMain).
 * 3) ליצור בעתיד נקודת כניסה מקבילה ל־iOS שקוראת לאותה לוגיקה משותפת.
 */
class MainActivity : androidx.fragment.app.FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            // 1) טוענים את המצב הראשוני מ־SharedPreferences
            var themeMode by remember {
                mutableStateOf(kmiPrefs.themeMode.ifBlank {
                    sp.getString("theme_mode", "system") ?: "system"
                })
            }

            // 2) פונקציה שמשנה גם את ה־State וגם נשמרת ב-SP
            fun onThemeChange(mode: String) {
                themeMode = mode
                sp.edit().putString("theme_mode", mode).apply()
            }

            // 3) מחשבים darkTheme אמיתי
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark"  -> true
                else    -> isSystemInDarkTheme()
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
    var currentScreen by remember { mutableStateOf(if (isRegistered) "main" else "intro") }

    // מסלול פתיחה חד-פעמי ל־MainApp (כמו שהיה)
    var startRoute by remember { mutableStateOf<String?>(null) }

    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
    ) {
        when (currentScreen) {
            "intro" -> {
                IntroScreen(
                    onContinue = {
                        currentScreen = if (isRegistered) "main" else "register"
                    }
                )
            }

            "register" -> {
                MainApp(
                    sp = sp,
                    vm = vm,
                    startRoute = "registration",
                    kmiPrefs = kmiPrefs
                )
            }

            "main" -> {
                MainApp(
                    sp = sp,
                    vm = vm,
                    startRoute = startRoute,
                    kmiPrefs = kmiPrefs
                )
            }
        }
    }
}