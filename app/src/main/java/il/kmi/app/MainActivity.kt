package il.kmi.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
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
        ensureCoachBroadcastNotificationChannel()

        // 👇 חדש: קליטה מהתראה (גם כשהאפליקציה סגורה)
        handleCoachGateIntent(intent)
        handleForumPushIntent(intent)
        handleDailyReminderIntent(intent)

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

        // ✅ שמירת FCM Token למשתמש האמיתי של האפליקציה כדי ש-Cloud Function תוכל לשלוח Push
        setupFcmTokenSync(userSp)

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
        handleForumPushIntent(intent)
        handleDailyReminderIntent(intent)
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

    private fun handleForumPushIntent(i: Intent?) {
        val intent = i ?: return

        val type = intent.getStringExtra("fcm_type").orEmpty()
        if (type != "forum_message") return

        val roomId = intent.getStringExtra("forumRoomId")
            ?: intent.getStringExtra("roomId")
            ?: ""

        val roomName = intent.getStringExtra("forumRoomName")
            ?: intent.getStringExtra("roomName")
            ?: ""

        val messageId = intent.getStringExtra("forumMessageId")
            ?: intent.getStringExtra("messageId")
            ?: ""

        val branchId = intent.getStringExtra("branchId").orEmpty()
        val groupKey = intent.getStringExtra("groupKey").orEmpty()
        val senderId = intent.getStringExtra("forumSenderId")
            ?: intent.getStringExtra("senderId")
            ?: ""

        if (roomId.isBlank() && messageId.isBlank()) {
            return
        }

        val forumSp = getSharedPreferences("kmi_forum_push", Context.MODE_PRIVATE)

        forumSp.edit()
            .putBoolean("has_pending_forum_push", true)
            .putString("forum_room_id", roomId)
            .putString("forum_room_name", roomName)
            .putString("forum_message_id", messageId)
            .putString("forum_branch_id", branchId)
            .putString("forum_group_key", groupKey)
            .putString("forum_sender_id", senderId)
            .putLong("received_at", System.currentTimeMillis())
            .apply()
    }

    private fun handleDailyReminderIntent(i: Intent?) {
        val intent = i ?: return

        val openFromDailyReminder =
            intent.getBooleanExtra("open_from_daily_reminder", false)

        if (!openFromDailyReminder) {
            return
        }

        val beltId = intent.getStringExtra("daily_reminder_belt_id").orEmpty()
        val topic = intent.getStringExtra("daily_reminder_topic").orEmpty()
        val item = intent.getStringExtra("daily_reminder_item").orEmpty()

        if (beltId.isBlank() && topic.isBlank() && item.isBlank()) {
            return
        }

        val dailyReminderSp = getSharedPreferences(
            "kmi_daily_reminder_nav",
            Context.MODE_PRIVATE
        )

        dailyReminderSp.edit()
            .putBoolean("has_pending_daily_reminder", true)
            .putString("daily_reminder_belt_id", beltId)
            .putString("daily_reminder_topic", topic)
            .putString("daily_reminder_item", item)
            .putLong("received_at", System.currentTimeMillis())
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

    private fun ensureCoachBroadcastNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val isEnglish =
            AppLanguageManager(this).getCurrentLanguage() ==
                    il.kmi.shared.localization.AppLanguage.ENGLISH

        val channelId = "coach_broadcasts"
        val channelName = if (isEnglish) "Coach Messages" else "הודעות מאמן"
        val channelDescription = if (isEnglish) {
            "Notifications for new messages from the coach"
        } else {
            "התראות על הודעות חדשות מהמאמן"
        }

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
            enableVibration(true)
            setShowBadge(true)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun setupFcmTokenSync(userSp: SharedPreferences) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        fun cleanPhone(raw: String?): String {
            return raw
                .orEmpty()
                .trim()
                .replace("-", "")
                .replace(" ", "")
        }

        fun localProfileUid(): String {
            return listOf(
                userSp.getString("uid", null),
                userSp.getString("user_uid", null),
                userSp.getString("firebase_uid", null),
                userSp.getString("auth_uid", null)
            )
                .map { it.orEmpty().trim() }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        }

        fun localProfileEmail(): String {
            return listOf(
                userSp.getString("email", null),
                userSp.getString("user_email", null)
            )
                .map { it.orEmpty().trim() }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        }

        fun localProfilePhone(): String {
            return listOf(
                userSp.getString("phone", null),
                userSp.getString("phoneNumber", null),
                userSp.getString("phoneRaw", null),
                userSp.getString("user_phone", null)
            )
                .map { cleanPhone(it) }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        }

        fun saveTokenToUserDoc(
            userDocId: String,
            token: String,
            reason: String
        ) {
            val cleanUserDocId = userDocId.trim()
            val cleanToken = token.trim()

            if (cleanUserDocId.isBlank()) {
                return
            }

            if (cleanToken.isBlank()) {
                return
            }

            val now = Timestamp.now()

            db.collection("users")
                .document(cleanUserDocId)
                .set(
                    mapOf(
                        "uid" to cleanUserDocId,

                        // תאימות אחורה / טוקן אחרון
                        "fcmToken" to cleanToken,
                        "fcmTokenUpdatedAt" to now,

                        // מבנה מרובה טוקנים עבור Cloud Function
                        "fcmTokens.$cleanToken" to mapOf(
                            "token" to cleanToken,
                            "platform" to "android",
                            "updatedAt" to now,
                            "reason" to reason
                        )
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener {
                }
                .addOnFailureListener {
                }
        }

        fun syncForCurrentProfile() {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (token.isNullOrBlank()) {
                        return@addOnSuccessListener
                    }

                    val profileUid = localProfileUid()
                    val profileEmail = localProfileEmail()
                    val profilePhone = localProfilePhone()
                    val authUid = auth.currentUser?.uid.orEmpty().trim()

                    // ✅ קודם מחפשים לפי אימייל/טלפון בפרופיל העסקי.
                    // הסיבה: לפעמים FirebaseAuth uid / uid מקומי הוא ישן או שייך למסמך אחר,
                    // אבל email/phone מצביעים למסמך המשתמש האמיתי ב-users.
                    if (profileEmail.isNotBlank()) {
                        db.collection("users")
                            .whereEqualTo("email", profileEmail)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { snap ->
                                val doc = snap.documents.firstOrNull()

                                if (doc != null) {
                                    saveTokenToUserDoc(
                                        userDocId = doc.id,
                                        token = token,
                                        reason = "email_match"
                                    )
                                } else if (profilePhone.isNotBlank()) {
                                    db.collection("users")
                                        .whereEqualTo("phone", profilePhone)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener { phoneSnap ->
                                            val phoneDoc = phoneSnap.documents.firstOrNull()

                                            if (phoneDoc != null) {
                                                saveTokenToUserDoc(
                                                    userDocId = phoneDoc.id,
                                                    token = token,
                                                    reason = "phone_match"
                                                )
                                            } else if (authUid.isNotBlank()) {
                                                saveTokenToUserDoc(
                                                    userDocId = authUid,
                                                    token = token,
                                                    reason = "fallback_auth_uid_after_email_phone"
                                                )
                                            } else {
                                            }
                                        }
                                        .addOnFailureListener {
                                        }
                                } else if (authUid.isNotBlank()) {
                                    saveTokenToUserDoc(
                                        userDocId = authUid,
                                        token = token,
                                        reason = "fallback_auth_uid_after_email"
                                    )
                                } else {
                                }
                            }
                            .addOnFailureListener {
                            }

                        return@addOnSuccessListener
                    }

                    // 3) אם אין email — חיפוש לפי טלפון
                    if (profilePhone.isNotBlank()) {
                        db.collection("users")
                            .whereEqualTo("phone", profilePhone)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { snap ->
                                val doc = snap.documents.firstOrNull()

                                if (doc != null) {
                                    saveTokenToUserDoc(
                                        userDocId = doc.id,
                                        token = token,
                                        reason = "phone_match"
                                    )
                                } else if (authUid.isNotBlank()) {
                                    saveTokenToUserDoc(
                                        userDocId = authUid,
                                        token = token,
                                        reason = "fallback_auth_uid_after_phone"
                                    )
                                } else {
                                }
                            }
                            .addOnFailureListener {
                            }

                        return@addOnSuccessListener
                    }

                    // 4) אם אין email/phone — נשתמש ב-UID המקומי אם קיים.
                    if (profileUid.isNotBlank()) {
                        saveTokenToUserDoc(
                            userDocId = profileUid,
                            token = token,
                            reason = "fallback_local_profile_uid"
                        )
                    } else if (authUid.isNotBlank()) {
                        saveTokenToUserDoc(
                            userDocId = authUid,
                            token = token,
                            reason = "fallback_auth_uid_only"
                        )
                    } else {
                    }
                }
                .addOnFailureListener {
                }
        }

        syncForCurrentProfile()

        auth.addAuthStateListener {
            syncForCurrentProfile()
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

    val isEnglish = remember(ctx) {
        AppLanguageManager(ctx).getCurrentLanguage() ==
                il.kmi.shared.localization.AppLanguage.ENGLISH
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
                    // פעם ראשונה בלבד:
                    // בחירת שפה → IntroScreen
                    currentScreen = "intro"
                }
            )
        }

        "intro" -> {
            IntroScreen(
                // כניסה רגילה / רישום בדרך הרגילה
                onContinue = {
                    // ✅ כניסה רגילה תמיד מובילה למסך הבחירה:
                    // משתמש חדש / משתמש קיים.
                    // גם אם יש נתוני משתמש שמורים, לא מדלגים לבית.
                    startRoute = null
                    currentScreen = "register"
                },

                // Google Login הצליח והפרופיל מלא
                // מדלגים על מסך משתמש חדש / משתמש קיים ונכנסים לבית
                onProfileComplete = {
                    startRoute = Route.Home.route
                    currentScreen = "main"
                },

                // Google Login הצליח אבל חסרים פרטי KMI
                // מדלגים על מסך משתמש חדש / משתמש קיים ונכנסים ישר להשלמת פרטים
                onProfileMissing = {
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
            MainApp(
                sp = sp,
                vm = vm,
                startRoute = startRoute,
                kmiPrefs = kmiPrefs
            )
        }
    }
}
