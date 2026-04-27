package il.kmi.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.SharedPreferences
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.domain.Belt
import android.content.Context
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import il.kmi.app.KmiCalendarSync
import il.kmi.app.hasCalendarPermission
import androidx.appcompat.app.AppCompatDelegate
import il.kmi.app.StatsVm as AppStatsVm
import android.Manifest
import androidx.compose.foundation.clickable
import il.kmi.app.ui.rememberHaptics
import il.kmi.app.ui.rememberToaster
import il.kmi.app.ui.LoadingOverlay
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.pm.PackageInfoCompat
import il.kmi.app.ui.ext.color
import il.kmi.app.reminders.ReminderPrefs
import il.kmi.app.reminders.DailyReminderScheduler
import androidx.compose.material.icons.filled.Language
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.foundation.isSystemInDarkTheme

//======================================================================================

typealias StatsVm = AppStatsVm

/* ===== Helpers לשיתוף/דירוג/משוב ===== */
private fun openEmailFeedback(ctx: android.content.Context, to: String, subject: String, body: String = "") {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try { ctx.startActivity(Intent.createChooser(intent, "שלח משוב")) }
    catch (_: Exception) { android.widget.Toast.makeText(ctx, "לא נמצאה אפליקציית דוא״ל", android.widget.Toast.LENGTH_SHORT).show() }
}

private fun openStorePage(ctx: android.content.Context) {
    val pkg = ctx.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val web    = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { ctx.startActivity(market) } catch (_: ActivityNotFoundException) { ctx.startActivity(web) }
}

private fun shareApp(ctx: android.content.Context, text: String = "הורידו את KMI – ק.מ.י") {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, "שתף באמצעות").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun clearAppCache(ctx: android.content.Context): Boolean {
    return runCatching {
        ctx.cacheDir?.let { dir ->
            dir.deleteRecursively()
            dir.mkdirs() // להשאיר ספרייה קיימת
        }
        true
    }.getOrElse { false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenModern(
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs,     // 👈 נוסף
    themeMode: String,                           // 👈 נוסף
    onThemeChange: (String) -> Unit,             // 👈 נוסף
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenProgress: () -> Unit = {},
    onOpenCoachBroadcast: () -> Unit = {},
    onOpenRegistration: () -> Unit = {},         // 👈 כמו שהיה
    vm: StatsVm = object : StatsVm {
        override fun getItemStatusNullable(
            belt: Belt,
            topic: String,
            item: String
        ): Boolean? = null

        override fun isMastered(
            belt: Belt,
            topic: String,
            item: String
        ): Boolean = false
    }
) {

    val discardAndExit: () -> Unit = { onBack() }
    val saveAllAndApply: () -> Unit = { /* preferences already saved inline */ }

    val appCtxLang = LocalContext.current
    val languageManager = remember { AppLanguageManager(appCtxLang) }
    var currentLanguage by remember { mutableStateOf(languageManager.getCurrentLanguage()) }
    val isEnglish = currentLanguage == AppLanguage.ENGLISH
    val textAlignPrimary = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalEnd = if (isEnglish) Alignment.Start else Alignment.End
    val rowSpaceBetween = Arrangement.SpaceBetween

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val fullName by remember {
        mutableStateOf(
            sp.getString("fullName", null)
                ?: tr("שם מלא לא מוגדר", "Full name not set")
        )
    }
    val phone    by remember { mutableStateOf(sp.getString("phone", "") ?: "") }
    val email    by remember { mutableStateOf(sp.getString("email", "") ?: "") }
    val region   by remember { mutableStateOf(sp.getString("region", "") ?: "") }
    val branch   by remember { mutableStateOf(sp.getString("branch", "") ?: "") }
    val isCoachInit = sp.getString("user_role", "trainee") == "coach"
    var isCoach by rememberSaveable { mutableStateOf(isCoachInit) }
// === כלים גלובליים למסך: Haptics + Toast + Overlay טעינה ===
    val haptic = rememberHaptics()
    val toast  = rememberToaster()
    var isBusy by rememberSaveable { mutableStateOf(false) }

    // סטייטים להגדרות (נשמרים ב־SP כדי להשאיר עקביות)
    var remindersEnabled by rememberSaveable { mutableStateOf(sp.getBoolean("training_reminders_enabled", true)) }
    var reminderMinutes by rememberSaveable {
        mutableStateOf(
            sp.getInt("training_reminder_minutes",
                sp.getInt("lead_minutes", 60)
            )
        )
    }
    var calendarSync by rememberSaveable { mutableStateOf(sp.getBoolean("calendar_sync_enabled", true)) }
    var themeModeLocal by rememberSaveable(themeMode) {
        mutableStateOf(
            when {
                themeMode.isNotBlank() -> themeMode
                kmiPrefs.themeMode.isNotBlank() -> kmiPrefs.themeMode
                else -> sp.getString("theme_mode", "system") ?: "system"
            }
        )
    }

    // (אופציונלי, נשאר לעתיד)
    var tapSound by rememberSaveable { mutableStateOf(sp.getBoolean("tap_sound", false)) }
    var shortHaptic by rememberSaveable { mutableStateOf(sp.getBoolean("short_haptic", false)) }

    // --- תרגיל יומי ---
    val appCtx = LocalContext.current
    val reminderPrefsSp = remember { appCtx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE) }
    val reminderPrefs = remember { ReminderPrefs(reminderPrefsSp) }

    var dailyReminderEnabled by rememberSaveable(isCoach) {
        mutableStateOf(reminderPrefs.isEnabledForRole(isCoach))
    }
    var dailyReminderHour by rememberSaveable {
        mutableStateOf(reminderPrefs.getHour().takeIf { it in 0..23 } ?: 20)
    }
    var dailyReminderMinute by rememberSaveable {
        mutableStateOf(reminderPrefs.getMinute().takeIf { it in 0..59 } ?: 0)
    }

    fun applyDailyReminderSettings(
        enabled: Boolean,
        hour: Int = dailyReminderHour,
        minute: Int = dailyReminderMinute
    ) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)

        reminderPrefs.setHour(safeHour)
        reminderPrefs.setMinute(safeMinute)
        reminderPrefs.setEnabledForRole(isCoach, enabled)

        dailyReminderEnabled = enabled
        dailyReminderHour = safeHour
        dailyReminderMinute = safeMinute

        android.util.Log.d(
            "KMI_REMINDER",
            "applyDailyReminderSettings enabled=$enabled hour=$safeHour minute=$safeMinute isCoach=$isCoach"
        )

        if (enabled) {
            DailyReminderScheduler.cancel(appCtx)
            DailyReminderScheduler.schedule(appCtx)
        } else {
            DailyReminderScheduler.cancel(appCtx)
        }
    }

    // ▼▼ דיבוג: הדפסת כל המפתחות שקיימים ב-SharedPreferences למסך ה-Logcat ▼▼
    LaunchedEffect(Unit) {
        android.util.Log.d("Stats", "SP keys: " + sp.all.keys.joinToString())
        // אופציונלי: הצצה לערכים נפוצים אם קיימים
        val probeKeys = listOf(
            "progress_yellow","yellow_progress","yellowPercent","yellow_percentage",
            "progress_orange","orange_progress","orangePercent","orange_percentage",
            "progress_green","green_progress","greenPercent","green_percentage",
            "progress_blue","blue_progress","bluePercent","blue_percentage",
            "progress_brown","brown_progress","brownPercent","brown_percentage",
            "progress_black","black_progress","blackPercent","black_percentage"
        )
        android.util.Log.d("Stats", probeKeys.joinToString { k -> "$k=${sp.all[k]}" })
    }
    // ▲▲ סוף דיבוג ▲▲

    // ---- גרדיאנט כותרת לפי תפקיד (נשאר כפי שהיה) ----
    val headerBrush = if (isCoach)
        Brush.linearGradient(listOf(Color(0xFF7B1FA2), Color(0xFF512DA8)))
    else
        Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF26A69A)))

    // צבע "פרימיום" לאייקונים בכותרות הכרטיסים לפי תפקיד
    val sectionIconTint = remember(isCoach) {
        if (isCoach) Color(0xFF6A1B9A) else Color(0xFF1565C0)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(headerBrush)    // ← קודם הצבע/גרדיאנט
                    .statusBarsPadding()        // ← ואז הפדינג של הסטטוס בר
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ⬇⬇ שורת כותרת — RTL: "הגדרות" בצד ימין, "ערוך פרטים" בשמאל
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = rowSpaceBetween
                    ) {
                        Text(
                            text = tr("הגדרות", "Settings"),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textAlign = textAlignPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        val ctx = LocalContext.current
                        val onOpenRegistrationState = rememberUpdatedState(newValue = onOpenRegistration)

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    try {
                                        onOpenRegistrationState.value.invoke()
                                    } catch (t: Throwable) {
                                        android.util.Log.e("Settings", "EditProfile click failed", t)
                                        android.widget.Toast.makeText(
                                            ctx,
                                            tr("לא הצלחתי לפתוח עריכת פרטים", "Could not open profile editing"),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                tr("ערוך פרטים", "Edit profile"),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ⬇⬇ כרטיס פרופיל — אייקון בשמאל, טקסטים מיושרים לימין
                    Surface(
                        color = Color.White.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCoach) Icons.Filled.Verified else Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (isCoach) Color(0xFF6A1B9A) else Color(0xFF1565C0),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = horizontalEnd
                            ) {
                                Text(
                                    text = fullName.ifBlank { tr("משתמש", "User") },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = textAlignPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (phone.isNotBlank()) {
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = textAlignPrimary,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                        }
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- שפה מועדפת ---
            SettingsCard(
                title = "שפה",
                subtitle = "בחר שפת ממשק האפליקציה",
                icon = Icons.Filled.Language,
                iconTint = sectionIconTint
            ) {

                val context = LocalContext.current
                val languageManager = remember { AppLanguageManager(context) }

                var currentLanguage by remember {
                    mutableStateOf(languageManager.getCurrentLanguage())
                }

                val selectedIndex = if (currentLanguage == AppLanguage.HEBREW) 0 else 1

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = tr("בחר שפת ממשק", "Choose interface language"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlignPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TabRow(selectedTabIndex = selectedIndex) {

                        Tab(
                            selected = selectedIndex == 0,
                            onClick = {
                                languageManager.setLanguage(AppLanguage.HEBREW)
                                currentLanguage = AppLanguage.HEBREW
                                (context as? android.app.Activity)?.recreate()
                            },
                            text = {
                                Text(
                                    text = "עברית",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )

                        Tab(
                            selected = selectedIndex == 1,
                            onClick = {
                                languageManager.setLanguage(AppLanguage.ENGLISH)
                                currentLanguage = AppLanguage.ENGLISH
                                (context as? android.app.Activity)?.recreate()
                            },
                            text = {
                                Text(
                                    text = "English",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }

// --- תזכורות אימון ---
            SettingsCard(
                title = tr("תזכורות אימון", "Training reminders"),
                subtitle = tr("קבל התראה לפני תחילת אימון", "Get a reminder before training starts"),
                icon = Icons.Filled.AlarmOn,
                iconTint = sectionIconTint
            ) {
                val ctx = LocalContext.current

                // הרשאת POST_NOTIFICATIONS (אנדרואיד 13+)
                val notifPermissionLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                        if (granted) {
                            val lead = reminderMinutes.coerceIn(0, 180)
                            il.kmi.app.training.TrainingAlarmReceiver.cancelWeeklyAlarms(ctx)
                            il.kmi.app.training.TrainingAlarmReceiver.scheduleWeeklyAlarms(ctx, lead)
                        } else {
                            remindersEnabled = false
                            sp.edit().putBoolean("training_reminders_enabled", false).apply()
                        }
                    }

                fun scheduleReminders(leadMinutes: Int) {
                    val lead = leadMinutes.coerceIn(0, 180)
                    il.kmi.app.training.TrainingAlarmReceiver.cancelWeeklyAlarms(ctx)
                    il.kmi.app.training.TrainingAlarmReceiver.scheduleWeeklyAlarms(ctx, lead)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // שורה עליונה: שאלה (כשדלוק) + מתג
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (remindersEnabled) {
                            Text(
                                text = tr(
                                    "כמה דקות לפני האימון לקבל תזכורת?",
                                    "How many minutes before training would you like a reminder?"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = textAlignPrimary
                            )
                        } else {
                            // כשכבוי – רק משאיר רווח כדי שהמתג יישב בצד
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { enabled ->
                                remindersEnabled = enabled
                                sp.edit()
                                    .putBoolean("training_reminders_enabled", enabled)
                                    // שמירת מפתחות תאימות
                                    .putInt("lead_minutes", reminderMinutes.coerceIn(0, 180))
                                    .putInt("training_reminder_minutes", reminderMinutes.coerceIn(0, 180))
                                    .apply()

                                if (enabled) {
                                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                                        notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        scheduleReminders(reminderMinutes)
                                    }
                                } else {
                                    il.kmi.app.training.TrainingAlarmReceiver.cancelWeeklyAlarms(ctx)
                                }
                            }
                        )
                    }

                    // כפתורי 30 / 60 / 90 – מופיעים רק כשהמתג דלוק
                    if (remindersEnabled) {
                        val options = listOf(30, 60, 90)

                        val selectedIndex = options.indexOf(reminderMinutes).let { if (it >= 0) it else 1 } // ברירת מחדל 60

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            tonalElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TabRow(
                                selectedTabIndex = selectedIndex,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                indicator = { tabPositions ->
                                    TabRowDefaults.Indicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                        height = 3.dp
                                    )
                                },
                                divider = { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                            ) {
                                options.forEachIndexed { idx, minutes ->
                                    val selected = idx == selectedIndex

                                    Tab(
                                        selected = selected,
                                        onClick = {
                                            reminderMinutes = minutes
                                            sp.edit()
                                                .putInt("training_reminder_minutes", minutes)
                                                .putInt("lead_minutes", minutes) // ✅ זה מה שה-Receiver קורא אחרי BOOT
                                                .apply()
                                            scheduleReminders(minutes)
                                        },
                                        text = {
                                            // שתי שורות כמו בתמונה
                                            Text(
                                                text = if (isEnglish) "$minutes min\nbefore" else "${minutes} דק׳\nלפני",
                                                minLines = 2,
                                                maxLines = 2,
                                                softWrap = true,
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 56.dp)
                                                    .wrapContentHeight(Alignment.CenterVertically)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } // ← סוף כרטיס תזכורות אימון

            // --- תרגיל יומי ---
            SettingsCard(
                title = tr("תרגיל יומי", "Daily exercise"),
                subtitle = tr(
                    "קבל כל יום תרגיל מהחגורה הבאה בשעה שתבחר",
                    "Get a daily exercise from the next belt at the time you choose"
                ),
                icon = Icons.Filled.NotificationsActive,
                iconTint = sectionIconTint
            ) {
                val ctx = LocalContext.current

                val notifPermissionLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                        if (granted) {
                            applyDailyReminderSettings(
                                enabled = true,
                                hour = dailyReminderHour,
                                minute = dailyReminderMinute
                            )
                        } else {
                            applyDailyReminderSettings(enabled = false)
                        }
                    }

                val formattedDailyTime = remember(dailyReminderHour, dailyReminderMinute) {
                    String.format("%02d:%02d", dailyReminderHour, dailyReminderMinute)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isCoach)
                                tr(
                                    "המאמן יכול לכבות או להפעיל תרגיל יומי לעצמו",
                                    "The coach can enable or disable a daily exercise for themselves"
                                )
                            else
                                tr(
                                    "שלח לי בכל יום תרגיל מהחגורה הבאה",
                                    "Send me a daily exercise from the next belt"
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = textAlignPrimary
                        )

                        Switch(
                            checked = dailyReminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        val alreadyGranted =
                                            androidx.core.content.ContextCompat.checkSelfPermission(
                                                ctx,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED

                                        if (alreadyGranted) {
                                            applyDailyReminderSettings(
                                                enabled = true,
                                                hour = dailyReminderHour,
                                                minute = dailyReminderMinute
                                            )
                                        } else {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        applyDailyReminderSettings(
                                            enabled = true,
                                            hour = dailyReminderHour,
                                            minute = dailyReminderMinute
                                        )
                                    }
                                } else {
                                    applyDailyReminderSettings(enabled = false)
                                }
                            }
                        )
                    }

                    if (dailyReminderEnabled) {
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    ctx,
                                    { _, hourOfDay, minute ->
                                        applyDailyReminderSettings(
                                            enabled = true,
                                            hour = hourOfDay,
                                            minute = minute
                                        )
                                    },
                                    dailyReminderHour,
                                    dailyReminderMinute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tr("שעת התזכורת: $formattedDailyTime", "Reminder time: $formattedDailyTime"))
                        }

                        Text(
                            text = tr(
                                "תקבל התראה יומית עם אפשרות לפתוח כרטיס תרגיל, לשמור למועדפים ולקבל תרגיל נוסף.",
                                "You will receive a daily reminder with options to open the exercise card, save it to favorites, and get another exercise."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = textAlignPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- תזכורות אימונים חופשיים ---
            SettingsCard(
                title = tr("תזכורות אימונים חופשיים", "Free training reminders"),
                subtitle = tr(
                    "קבל התראה לפני אימון חופשי שאישרת הגעה",
                    "Get a reminder before a free training session you confirmed"
                ),
                icon = Icons.Filled.NotificationsActive,
                iconTint = sectionIconTint
            ) {
                val ctx = LocalContext.current

                var freeRemindersEnabled by rememberSaveable {
                    mutableStateOf(sp.getBoolean("free_sessions_reminders_enabled", false))
                }

                // הרשאת POST_NOTIFICATIONS (אנדרואיד 13+)
                val notifPermissionLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                        if (!granted) {
                            freeRemindersEnabled = false
                            sp.edit().putBoolean("free_sessions_reminders_enabled", false).apply()
                        }
                    }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr(
                                "התראות 30 ו-10 דקות לפני אימון חופשי שסימנת \"אני מגיע\"",
                                "Notifications 30 and 10 minutes before a free training session marked as \"I'm coming\""
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = textAlignPrimary
                        )

                        Switch(
                            checked = freeRemindersEnabled,
                            onCheckedChange = { enabled ->
                                freeRemindersEnabled = enabled
                                sp.edit().putBoolean("free_sessions_reminders_enabled", enabled).apply()

                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        // אין מה לתזמן כאן: התזכורות נקבעות כשמאשרים "אני מגיע"
                                        // והערוץ נוצר אוטומטית בזמן שליחת ההתראה בתוך ה-Receiver
                                    }
                                } else {
                                    // אופציונלי בעתיד: cancelAll(ctx) אם תוסיף פונקציה כזאת
                                }
                            }
                        )
                    }
                }
            } // ← סוף כרטיס תזכורות אימונים חופשיים

            // --- סנכרון ליומן חיצוני / יומן במכשיר ---
            SettingsCard(
                title = tr("סנכרון ליומן במכשיר", "Device calendar sync"),
                subtitle = tr(
                    "בחר יומן חיצוני לסנכרון האימונים",
                    "Choose an external/device calendar for training sync"
                ),
                icon = Icons.Filled.Event,
                iconTint = sectionIconTint
            ) {
                val appCtx = LocalContext.current
                var selectedSyncEnabled by rememberSaveable {
                    mutableStateOf(sp.getBoolean("calendar_sync_selected_enabled", false))
                }
                var selectedCalendarId by rememberSaveable {
                    mutableLongStateOf(sp.getLong("calendar_sync_selected_calendar_id", -1L))
                }
                var selectedCalendarDisplay by rememberSaveable {
                    mutableStateOf(sp.getString("calendar_sync_selected_calendar_display", "") ?: "")
                }
                var showCalendarPicker by rememberSaveable { mutableStateOf(false) }
                var pendingEnableAfterPermission by rememberSaveable { mutableStateOf(false) }

                val writableCalendars = remember(showCalendarPicker) {
                    if (showCalendarPicker) KmiCalendarSync.listWritableCalendars(appCtx) else emptyList()
                }

                val calendarPermissionLauncherSelected = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val granted =
                        result[Manifest.permission.READ_CALENDAR] == true &&
                                result[Manifest.permission.WRITE_CALENDAR] == true

                    if (!granted) {
                        selectedSyncEnabled = false
                        sp.edit().putBoolean("calendar_sync_selected_enabled", false).apply()
                        pendingEnableAfterPermission = false
                        haptic(true)
                        toast(
                            tr(
                                "אין הרשאה ליומן – לא בוצע סנכרון",
                                "No calendar permission - sync was not performed"
                            )
                        )
                        return@rememberLauncherForActivityResult
                    }

                    if (pendingEnableAfterPermission) {
                        pendingEnableAfterPermission = false
                        if (selectedCalendarId <= 0L) {
                            selectedSyncEnabled = false
                            showCalendarPicker = true
                            haptic(true)
                            toast(
                                tr(
                                    "יש לבחור יומן לפני הפעלת הסנכרון",
                                    "Please choose a calendar before enabling sync"
                                )
                            )
                        } else {
                            try {
                                isBusy = true
                                val ok = KmiCalendarSync.upsertAllToSelectedCalendar(appCtx, selectedCalendarId)
                                if (ok) {
                                    selectedSyncEnabled = true
                                    sp.edit().putBoolean("calendar_sync_selected_enabled", true).apply()
                                    haptic(true)
                                    toast(
                                        tr(
                                            "האימונים סונכרנו ליומן שבחרת",
                                            "Trainings were synced to the selected calendar"
                                        )
                                    )
                                } else {
                                    selectedSyncEnabled = false
                                    sp.edit().putBoolean("calendar_sync_selected_enabled", false).apply()
                                    haptic(true)
                                    toast(
                                        tr(
                                            "שגיאה בסנכרון ליומן שנבחר",
                                            "Error syncing to selected calendar"
                                        )
                                    )
                                }
                            } catch (_: Throwable) {
                                selectedSyncEnabled = false
                                sp.edit().putBoolean("calendar_sync_selected_enabled", false).apply()
                                haptic(true)
                                toast(
                                    tr(
                                        "שגיאה בסנכרון ליומן שנבחר",
                                        "Error syncing to selected calendar"
                                    )
                                )
                            } finally {
                                isBusy = false
                            }
                        }
                    }
                }

                fun persistCalendarSelection(cal: KmiCalendarSync.DeviceCalendar) {
                    selectedCalendarId = cal.id
                    selectedCalendarDisplay = "${cal.displayName} (${cal.accountName})"
                    sp.edit()
                        .putLong("calendar_sync_selected_calendar_id", cal.id)
                        .putString("calendar_sync_selected_calendar_display", selectedCalendarDisplay)
                        .apply()
                }

                fun enableSelectedCalendarSync() {
                    if (!hasCalendarPermission(appCtx)) {
                        pendingEnableAfterPermission = true
                        calendarPermissionLauncherSelected.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                        return
                    }

                    if (selectedCalendarId <= 0L) {
                        selectedSyncEnabled = false
                        showCalendarPicker = true
                        haptic(true)
                        toast(
                            tr(
                                "יש לבחור יומן לפני הפעלת הסנכרון",
                                "Please choose a calendar before enabling sync"
                            )
                        )
                        return
                    }

                    try {
                        isBusy = true
                        val ok = KmiCalendarSync.upsertAllToSelectedCalendar(appCtx, selectedCalendarId)
                        if (ok) {
                            selectedSyncEnabled = true
                            sp.edit().putBoolean("calendar_sync_selected_enabled", true).apply()
                            haptic(true)
                            toast(
                                tr(
                                    "האימונים סונכרנו ליומן שבחרת",
                                    "Trainings were synced to the selected calendar"
                                )
                            )
                        } else {
                            selectedSyncEnabled = false
                            sp.edit().putBoolean("calendar_sync_selected_enabled", false).apply()
                            haptic(true)
                            toast(
                                tr(
                                    "שגיאה בסנכרון ליומן שנבחר",
                                    "Error syncing to selected calendar"
                                )
                            )
                        }
                    } catch (_: Throwable) {
                        selectedSyncEnabled = false
                        sp.edit().putBoolean("calendar_sync_selected_enabled", false).apply()
                        haptic(true)
                        toast(
                            tr(
                                "שגיאה בסנכרון ליומן שנבחר",
                                "Error syncing to selected calendar"
                            )
                        )
                    } finally {
                        isBusy = false
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("סנכרן ליומן חיצוני", "Sync to external calendar"),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlignPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(12.dp))

                    Switch(
                        checked = selectedSyncEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                enableSelectedCalendarSync()
                            } else {
                                selectedSyncEnabled = false
                                sp.edit().putBoolean("calendar_sync_selected_enabled", false).apply()
                                try {
                                    isBusy = true
                                    KmiCalendarSync.removeSelectedCalendarEvents(appCtx)
                                    haptic(true)
                                    toast(
                                        tr(
                                            "הסנכרון ליומן שבחרת בוטל",
                                            "Selected calendar sync was disabled"
                                        )
                                    )
                                } catch (_: Throwable) {
                                    haptic(true)
                                    toast(
                                        tr(
                                            "שגיאה בביטול הסנכרון",
                                            "Error disabling calendar sync"
                                        )
                                    )
                                } finally {
                                    isBusy = false
                                }
                            }
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (selectedCalendarId > 0L && selectedCalendarDisplay.isNotBlank()) {
                        tr("יומן שנבחר: $selectedCalendarDisplay", "Selected calendar: $selectedCalendarDisplay")
                    } else {
                        tr("עדיין לא נבחר יומן יעד", "No target calendar selected yet")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlignPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        if (!hasCalendarPermission(appCtx)) {
                            calendarPermissionLauncherSelected.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        } else {
                            showCalendarPicker = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(tr("בחר יומן יעד", "Choose target calendar"))
                }

                if (showCalendarPicker) {
                    var tempSelectedId by remember(selectedCalendarId, writableCalendars) {
                        mutableLongStateOf(
                            if (writableCalendars.any { it.id == selectedCalendarId }) selectedCalendarId
                            else writableCalendars.firstOrNull()?.id ?: -1L
                        )
                    }

                    AlertDialog(
                        onDismissRequest = { showCalendarPicker = false },
                        title = { Text(tr("בחר יומן לסנכרון", "Choose calendar for sync")) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (writableCalendars.isEmpty()) {
                                    Text(
                                        tr(
                                            "לא נמצאו יומנים זמינים לכתיבה במכשיר.",
                                            "No writable calendars were found on this device."
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    writableCalendars.forEach { cal ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { tempSelectedId = cal.id }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = tempSelectedId == cal.id,
                                                onClick = { tempSelectedId = cal.id }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = horizontalEnd
                                            ) {
                                                Text(
                                                    cal.displayName.ifBlank { tr("יומן ללא שם", "Unnamed calendar") },
                                                    textAlign = textAlignPrimary
                                                )
                                                Text(
                                                    cal.accountName.ifBlank { cal.accountType.ifBlank { "-" } },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = textAlignPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val selected = writableCalendars.firstOrNull { it.id == tempSelectedId }
                                    if (selected == null) {
                                        haptic(true)
                                        toast(tr("יש לבחור יומן תקין", "Please choose a valid calendar"))
                                        return@TextButton
                                    }
                                    persistCalendarSelection(selected)
                                    showCalendarPicker = false
                                    if (selectedSyncEnabled) {
                                        enableSelectedCalendarSync()
                                    }
                                }
                            ) {
                                Text(tr("שמירה", "Save"))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCalendarPicker = false }) {
                                Text(tr("ביטול", "Cancel"))
                            }
                        }
                    )
                }

                LoadingOverlay(show = isBusy, modifier = Modifier.fillMaxWidth().height(80.dp))
            }

            // --- חוויית משתמש ---
            SettingsCard(
                title = tr("חוויית משתמש", "User experience"),
                subtitle = tr("צלילים, רטט ושיפור חוויית האינטראקציה", "Sounds, haptics, and improved interaction experience"),
                icon = Icons.Filled.Tune,
                iconTint = sectionIconTint
            ) {
                // מצב נוכחי מתוך SP (תומך גם במפתחות הישנים tap_sound / short_haptic)
                var clickSounds by rememberSaveable {
                    mutableStateOf(
                        sp.getBoolean(
                            "click_sounds",
                            sp.getBoolean("tap_sound", false)
                        )
                    )
                }
                var hapticsOn by rememberSaveable {
                    mutableStateOf(
                        sp.getBoolean(
                            "haptics_on",
                            sp.getBoolean("short_haptic", false)
                        )
                    )
                }

                // קריאות בטוחות למודול המשותף (אם קיים)
                fun setClickSoundsEnabledSafe(enabled: Boolean) {
                    runCatching { il.kmi.shared.Platform.setClickSoundsEnabled(enabled) }
                }
                fun setHapticsEnabledSafe(enabled: Boolean) {
                    runCatching { il.kmi.shared.Platform.setHapticsEnabled(enabled) }
                }

                // כדי לבצע צליל/רטט מיידי במסך ההגדרות
                val view = LocalView.current

                fun playFeedbackIfEnabled() {
                    // צליל הקשה
                    if (clickSounds) {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                    }
                    // רטט קצר
                    if (hapticsOn) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                }

                // שורה 1: צליל הקשה בכפתורים
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        tr("צליל הקשה בכפתורים", "Button tap sound"),
                        textAlign = textAlignPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = clickSounds,
                        onCheckedChange = { enabled ->
                            clickSounds = enabled
                            // שומרים גם במפתחות הישנים וגם בחדשים
                            sp.edit()
                                .putBoolean("click_sounds", enabled)
                                .putBoolean("tap_sound", enabled)
                                .apply()
                            setClickSoundsEnabledSafe(enabled)

                            // משוב מיידי – רק כשהמצב לאחר הלחיצה הוא פעיל
                            if (clickSounds) {
                                playFeedbackIfEnabled()
                            }
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // שורה 2: רטט קצר בעת סימון ✓/✗
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        tr("רטט קצר בעת סימון ✓/✗", "Short haptic on ✓/✗ marking"),
                        textAlign = textAlignPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = hapticsOn,
                        onCheckedChange = { enabled ->
                            hapticsOn = enabled
                            sp.edit()
                                .putBoolean("haptics_on", enabled)
                                .putBoolean("short_haptic", enabled)
                                .apply()
                            setHapticsEnabledSafe(enabled)

                            // משוב מיידי – רק כשהמצב לאחר הלחיצה הוא פעיל
                            if (hapticsOn) {
                                playFeedbackIfEnabled()
                            }
                        }
                    )
                }
            }

            // --- הגדרות קול (ענן) ---
            SettingsCard(
                title = tr("הגדרות קול", "Voice settings"),
                subtitle = tr("בחירת קול גבר/אישה (אחיד לכל האפליקציה)", "Choose male/female voice for the entire app"),
                icon = Icons.Filled.SupportAgent,
                iconTint = sectionIconTint
            ) {
            val ctx = LocalContext.current

                val voicePrefs = remember {
                    ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
                }

                // ✅ זה ה-SP שה-TTS המקומי באמת קורא ממנו
                val userSp = remember {
                    ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
                }

                var cloudVoice by rememberSaveable {
                    mutableStateOf(voicePrefs.getString("voice", "male") ?: "male") // "male" / "female"
                }

                fun setCloudVoice(v: String) {
                    cloudVoice = v

                    voicePrefs.edit()
                        .putString("voice", v)
                        .apply()

                    ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("kmi_tts_voice", v)
                        .apply()
                }

                val selectedIndex = if (cloudVoice == "male") 0 else 1

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tr("בחר קול להשמעה:", "Choose voice playback:"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlignPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ✅ במקום TabRow (שנוטה “להעלים” טקסט) – SegmentedButton יציב
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = selectedIndex == 0,
                            onClick = { setCloudVoice("male") },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text(tr("קול גבר", "Male voice"), maxLines = 1) }
                        )
                        SegmentedButton(
                            selected = selectedIndex == 1,
                            onClick = { setCloudVoice("female") },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text(tr("קול אישה", "Female voice"), maxLines = 1) }
                        )
                    }

                    Text(
                        text = tr(
                            "הבחירה נשמרת למכשיר ותשפיע על הדיבור בעוזר הקולי.",
                            "The selection is saved on the device and affects speech in the voice assistant."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlignPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- נראות אפליקציה (לפי מכשיר / מצב בהיר / מצב כהה) ---
            SettingsCard(
                title = tr("נראות אפליקציה", "App appearance"),
                subtitle = tr(
                    "ברירת המחדל היא לפי מצב המכשיר",
                    "Default is based on the device appearance"
                ),
                icon = Icons.Filled.Palette,
                iconTint = sectionIconTint
            ) {
                val systemIsDark = isSystemInDarkTheme()

                fun effectiveModeLabel(): String {
                    return when (themeModeLocal) {
                        "light" -> tr("מצב פעיל: בהיר", "Active mode: Light")
                        "dark" -> tr("מצב פעיל: כהה", "Active mode: Dark")
                        else -> {
                            if (systemIsDark) {
                                tr("מצב פעיל: לפי המכשיר — כהה", "Active mode: Device default — Dark")
                            } else {
                                tr("מצב פעיל: לפי המכשיר — בהיר", "Active mode: Device default — Light")
                            }
                        }
                    }
                }

                fun applyAppearanceMode(mode: String) {
                    themeModeLocal = mode
                    onThemeChange(mode)
                    kmiPrefs.themeMode = mode
                    sp.edit().putString("theme_mode", mode).apply()
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tr("בחר מצב תצוגה:", "Choose display mode:"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlignPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val themeIndex = when (themeModeLocal) {
                        "system" -> 0
                        "light" -> 1
                        "dark" -> 2
                        else -> 0
                    }

                    TabRow(selectedTabIndex = themeIndex) {

                        Tab(
                            selected = themeModeLocal == "system",
                            onClick = { applyAppearanceMode("system") },
                            text = {
                                Text(
                                    text = tr("לפי\nהמכשיר", "Device\ndefault"),
                                    minLines = 2,
                                    maxLines = 2,
                                    softWrap = true,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (themeModeLocal == "system") {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.SemiBold
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .wrapContentHeight(Alignment.CenterVertically)
                                )
                            }
                        )

                        Tab(
                            selected = themeModeLocal == "light",
                            onClick = { applyAppearanceMode("light") },
                            text = {
                                Text(
                                    text = tr("מצב\nבהיר", "Light\nmode"),
                                    minLines = 2,
                                    maxLines = 2,
                                    softWrap = true,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (themeModeLocal == "light") {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.SemiBold
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .wrapContentHeight(Alignment.CenterVertically)
                                )
                            }
                        )

                        Tab(
                            selected = themeModeLocal == "dark",
                            onClick = { applyAppearanceMode("dark") },
                            text = {
                                Text(
                                    text = tr("מצב\nכהה", "Dark\nmode"),
                                    minLines = 2,
                                    maxLines = 2,
                                    softWrap = true,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (themeModeLocal == "dark") {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.SemiBold
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .wrapContentHeight(Alignment.CenterVertically)
                                )
                            }
                        )
                    }

                    Text(
                        text = effectiveModeLabel(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = textAlignPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = tr(
                            "במצב לפי המכשיר, האפליקציה תעבור אוטומטית בין מצב כהה ובהיר לפי ההגדרה של Android.",
                            "In device default mode, the app automatically follows Android light/dark appearance."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlignPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- נעילת אפליקציה (ללא נעילה / אצבע) ---
            SettingsCard(
                title = tr("נעילת אפליקציה", "App lock"),
                subtitle = tr("בחר שיטת נעילה להגנה על האפליקציה", "Choose a lock method to protect the app"),
                icon = Icons.Filled.Lock,
                iconTint = sectionIconTint
            ) {
                var lockMode by rememberSaveable {
                    mutableStateOf(
                        when (sp.getString("app_lock_mode", "none") ?: "none") {
                            "biometric" -> "biometric"
                            else -> "none"
                        }
                    )
                }

                val ctx = LocalContext.current
                val act = ctx as? androidx.fragment.app.FragmentActivity

                fun applyLock(mode: String) {
                    sp.edit().putString("app_lock_mode", mode).apply()
                    when (mode) {
                        "none" -> {
                            il.kmi.app.security.AppLockStore.setMethod(
                                ctx,
                                il.kmi.app.security.AppLockMethod.NONE
                            )
                            android.widget.Toast.makeText(
                                ctx,
                                tr("נעילת האפליקציה בוטלה", "App lock disabled"),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }

                        "biometric" -> {
                            val canBio = androidx.biometric.BiometricManager.from(ctx)
                                .canAuthenticate(
                                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
                                ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

                            if (!canBio) {
                                android.widget.Toast.makeText(
                                    ctx,
                                    tr("ביומטרי לא זמין במכשיר", "Biometric authentication is not available on this device"),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                lockMode = sp.getString("app_lock_mode", "none") ?: "none"
                                return
                            }

                            il.kmi.app.security.AppLockStore.setMethod(
                                ctx,
                                il.kmi.app.security.AppLockMethod.BIOMETRIC
                            )
                            act?.let { il.kmi.app.security.AppLock.requireIfNeeded(it, true) }
                            android.widget.Toast.makeText(
                                ctx,
                                tr("זיהוי ביומטרי הופעל", "Biometric lock enabled"),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                val lockIndex = when (lockMode) {
                    "none" -> 0
                    "biometric" -> 1
                    else -> 0
                }

                TabRow(selectedTabIndex = lockIndex) {
                    Tab(
                        selected = lockMode == "none",
                        onClick = {
                            lockMode = "none"
                            applyLock("none")
                        },
                        text = {
                            Text(
                                text = tr("ללא\nנעילה", "No\nlock"),
                                minLines = 2,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .wrapContentHeight(Alignment.CenterVertically)
                            )
                        }
                    )

                    Tab(
                        selected = lockMode == "biometric",
                        onClick = {
                            lockMode = "biometric"
                            applyLock("biometric")
                        },
                        text = {
                            Text(
                                text = tr("נעילה\nבאצבע", "Biometric\nlock"),
                                minLines = 2,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .wrapContentHeight(Alignment.CenterVertically)
                            )
                        }
                    )
                }
            }

// הודעת מידע אם ביומטרי לא זמין במכשיר
            val ctxBio = LocalContext.current
            val bioAvailable = remember(ctxBio) {
                androidx.biometric.BiometricManager.from(ctxBio)
                    .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                        androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
            }
            if (!bioAvailable) {
                Spacer(Modifier.height(6.dp))
                Text(
                    tr("ביומטרי לא זמין במכשיר או לא הוגדר למשתמש.", "Biometric authentication is not available or not configured for this user."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlignPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // =========================
            // סטטיסטיקות במסך הגדרות
            // =========================

            SettingsCard(
                title = tr("סטטיסטיקות", "Statistics"),
                subtitle = tr("התקדמות לפי חגורות ונושאים", "Progress by belts and topics"),
                icon = Icons.Filled.BarChart,
                iconTint = sectionIconTint
            ) {
                // ▼ הצגת הדרגה הנוכחית לפי החגורה מהרישום (קורא גם מ-sp וגם מ-kmi_user)
                val ctxForBelt = LocalContext.current
                val currentBelt = remember { readRegisteredBelt(ctxForBelt, sp) }

// ✅ חשוב: קוראים ל-MaterialTheme מחוץ ל-remember
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                val beltTextColor = remember(currentBelt, onSurfaceColor) {
                    when (currentBelt) {
                        Belt.WHITE -> onSurfaceColor
                        Belt.BLACK -> onSurfaceColor
                        else -> currentBelt.color
                    }
                }

                Text(
                    text = if (isEnglish)
                        "My current rank: ${currentBelt.id.replaceFirstChar { it.uppercase() }} belt"
                    else
                        "דרגתי הנוכחית: חגורה ${currentBelt.heb.removePrefix("חגורה").trim()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = beltTextColor,
                    textAlign = textAlignPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))
                BeltsProgressBars(vm = vm)
            }

            // =========================
            // ניהול נתונים
            // =========================

            // כרטיס: ניהול נתונים (היסטוריית שידורים + מטמון)
            SettingsCard(
                title = tr("ניהול נתונים", "Data management"),
                subtitle = tr("ניקוי נתונים מקומיים במכשיר", "Clear local data on the device"),
                icon = Icons.Filled.Storage,
                iconTint = sectionIconTint
            ) {
                val ctx = LocalContext.current
                val spUser = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
                val PREF_RECENTS_KEY = "coach_broadcast_recents_json"

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // ניקוי היסטוריית שידורים (10 אחרונות שהוספנו קודם)
                    Button(
                        onClick = {
                            spUser.edit().remove(PREF_RECENTS_KEY).apply()
                            toast(tr("היסטוריית השידורים נוקתה", "Broadcast history cleared")); haptic(true)
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) { Text(tr("נקה היסטוריית שידורים", "Clear broadcast history")) }

                    // ניקוי מטמון קבצים (תמונות/ייצוא זמני וכד׳)
                    OutlinedButton(
                        onClick = {
                            isBusy = true
                            val ok = clearAppCache(ctx)
                            isBusy = false
                            if (ok) { toast(tr("נוקו קבצי המטמון", "Cache files cleared")); haptic(true) } else { toast(tr("ניקוי נכשל", "Clear failed")); haptic(false) }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) { Text(tr("נקה מטמון אפליקציה", "Clear app cache")) }
                }
            }

            // כרטיס: משפטי (פרימיום) — 3 מסמכים
            SettingsCard(
                title = tr("מידע משפטי", "Legal information"),
                subtitle = tr("מסמכים רשמיים ומידע חשוב", "Official documents and important information"),
                icon = Icons.Filled.Gavel,
                iconTint = sectionIconTint
            ) {

                @Composable
                fun LegalTile(
                    title: String,
                    subtitle: String,
                    icon: ImageVector,
                    onClick: () -> Unit,
                    modifier: Modifier = Modifier
                ) {
                    Surface(
                        onClick = onClick,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = modifier
                            .heightIn(min = 92.dp) // ⬅️ היה 76.dp (קטן מדי לשתי שורות)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // אייקון קטן “יוקרתי”
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = horizontalEnd
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = textAlignPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = textAlignPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    LegalTile(
                        title = tr("מדיניות פרטיות", "Privacy policy"),
                        subtitle = tr("איך אנחנו שומרים על הנתונים שלך", "How we protect your data"),
                        icon = Icons.Filled.Lock,
                        onClick = onOpenPrivacy,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LegalTile(
                        title = tr("תנאי שימוש", "Terms of use"),
                        subtitle = tr("כללי שימוש והתחייבויות המשתמש", "Usage rules and user responsibilities"),
                        icon = Icons.Filled.Gavel,
                        onClick = onOpenTerms,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LegalTile(
                        title = tr("הצהרת נגישות", "Accessibility statement"),
                        subtitle = tr("מידע על התאמות ונגישות באפליקציה", "Information about accessibility and adaptations in the app"),
                        icon = Icons.Filled.AccessibilityNew,
                        onClick = onOpenAccessibility,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // כרטיס: אודות ותמיכה
            SettingsCard(
                title = tr("אודות ותמיכה", "About and support"),
                subtitle = tr("ספרו לנו איך אפשר לשפר", "Tell us how we can improve"),
                icon = Icons.Filled.SupportAgent,
                iconTint = sectionIconTint
            ) {
                val ctx = LocalContext.current
                val h   = rememberHaptics()

                val pkgVer = remember {
                    runCatching {
                        val pm = ctx.packageManager
                        val pInfo = if (Build.VERSION.SDK_INT >= 33) {
                            pm.getPackageInfo(
                                ctx.packageName,
                                PackageManager.PackageInfoFlags.of(0)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getPackageInfo(ctx.packageName, 0)
                        }
                        val longCode = PackageInfoCompat.getLongVersionCode(pInfo)
                        tr("גרסה ${pInfo.versionName} ($longCode)", "Version ${pInfo.versionName} ($longCode)")
                    }.getOrDefault(tr("גרסה לא ידועה", "Unknown version"))
                }

                Text(
                    text = pkgVer,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val body = buildString {
                                appendLine("")
                                appendLine("---")
                                appendLine(tr("פרטי מערכת (לעזרה באיתור תקלות):", "System details (for troubleshooting):"))
                                appendLine(tr("חבילה: ${ctx.packageName}", "Package: ${ctx.packageName}"))
                                appendLine(pkgVer)
                                appendLine(tr("מכשיר: ${Build.MANUFACTURER} ${Build.MODEL}", "Device: ${Build.MANUFACTURER} ${Build.MODEL}"))
                                appendLine(tr("אנדרואיד: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})", "Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"))
                            }
                            openEmailFeedback(
                                ctx,
                                to = "support@kmi.example",
                                subject = tr("משוב על האפליקציה", "App feedback"),
                                body = body
                            )
                            h(true)
                        },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) { Text(tr("שלח משוב", "Send feedback")) }

                    OutlinedButton(
                        onClick = { openStorePage(ctx); h(true) },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) { Text(tr("דרג בחנות", "Rate in store")) }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { shareApp(ctx); h(true) },
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text(tr("שתף את האפליקציה", "Share the app")) }
            }

            // --- מרווח לפני כפתורי פעולה ---
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { discardAndExit() },
                    modifier = Modifier.weight(1f)
                ) { Text(tr("ביטול", "Cancel")) }

                Button(
                    onClick = {
                        saveAllAndApply()
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(tr("אישור", "Confirm")) }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
// --- עזר לכפתורי מידע משפטי: גובה אחיד, ישור למרכז, משקל שווה ---
@Composable
private fun RowScope.LegalLink(
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)                // כל כפתור חצי רוחב
            .heightIn(min = 64.dp)     // גובה קבוע נעים ל־2 שורות
            .padding(horizontal = 4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            softWrap = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BeltsProgressBars(
    vm: StatsVm,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val sp  = remember { ctx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE) }
    val languageManager = remember { AppLanguageManager(ctx) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val belts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)

    data class Row(val title: String, val pct: Int, val color: Color)

    val rows by produceState(initialValue = emptyList<Row>()) {
        val out = mutableListOf<Row>()
        belts.forEach { belt ->
            val topics = runCatching { il.kmi.app.search.KmiSearchBridge.topicTitlesFor(belt) }
                .getOrDefault(emptyList())

            var done = 0
            var total = 0

            topics.forEach { topic ->
                val items = runCatching { il.kmi.app.search.KmiSearchBridge.itemsFor(belt, topic) }
                    .getOrDefault(emptyList())

                val excluded = sp.getStringSet("excluded_${belt.id}_${topic}", emptySet()) ?: emptySet()

                items.forEach { item ->
                    if (item !in excluded) {
                        total++
                        val mastered: Boolean? =
                            runCatching { vm.getItemStatusNullable(belt, topic, item) }.getOrNull()
                                ?: runCatching { if (vm.isMastered(belt, topic, item)) true else null }.getOrNull()
                        if (mastered == true) done++
                    }
                }
            }

            val pct = if (total > 0) ((done * 100f) / total).toInt().coerceIn(0, 100) else 0
            val title = when (belt) {
                Belt.YELLOW -> if (isEnglish) "Yellow belt" else "חגורה: צהובה"
                Belt.ORANGE -> if (isEnglish) "Orange belt" else "חגורה: כתומה"
                Belt.GREEN  -> if (isEnglish) "Green belt" else "חגורה: ירוקה"
                Belt.BLUE   -> if (isEnglish) "Blue belt" else "חגורה: כחולה"
                Belt.BROWN  -> if (isEnglish) "Brown belt" else "חגורה: חומה"
                Belt.BLACK  -> if (isEnglish) "Black belt" else "חגורה: שחורה"
                else        -> if (isEnglish) "Belt" else "חגורה"
            }
            out += Row(title, pct, belt.color)
        }
        value = out
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            // צבע טקסט מיוחד לחגורה שחורה – לפי Theme
            val textColor =
                if (row.title.contains("שחורה") || row.title.contains("Black"))
                    MaterialTheme.colorScheme.onSurface
                else
                    row.color

            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        row.title,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${row.pct}%",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(row.pct / 100f)
                            .background(row.color, RoundedCornerShape(999.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val languageManager = remember { AppLanguageManager(context) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val textAlignPrimary = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalEnd = if (isEnglish) Alignment.Start else Alignment.End

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ✅ כותרת עם אייקון (RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (iconTint ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = horizontalEnd
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = textAlignPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = textAlignPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            content()
        }
    }
}

private fun applyTheme(mode: String) {
    val night = when (mode) {
        "dark"  -> AppCompatDelegate.MODE_NIGHT_YES
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        else    -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(night)
}

private fun readRegisteredBelt(ctx: android.content.Context, spSettings: SharedPreferences): Belt {
    val spUser = ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

    // 1) מזהה חגורה שנשמר בטופס הרישום (למתאמן): "current_belt" ב-sp או "belt_current" ב-kmi_user
    val idFromSettings = spSettings.getString("current_belt", null)?.trim().orEmpty()
    val idFromUser     = spUser.getString("belt_current", null)?.trim().orEmpty()

    // 2) שדות טקסטואליים נפוצים (עברית/אנגלית) – אם מישהו שמר את שם החגורה ולא מזהה
    val rawText = listOf(
        "belt", "belt_id", "beltColor", "belt_color",
        "beltName", "belt_name", "beltHeb", "belt_heb"
    ).firstNotNullOfOrNull { k -> spUser.getString(k, null)?.trim() }.orEmpty()

    fun fromId(id: String): Belt? =
        id.takeIf { it.isNotBlank() }?.let { Belt.fromId(it.lowercase()) }

    fun fromText(s: String): Belt? {
        val v = s.trim().lowercase()
        return when (v) {
            "", "—" -> null
            "white",  "לבן",  "לבנה"   -> Belt.WHITE
            "yellow", "צהוב", "צהובה"  -> Belt.YELLOW
            "orange", "כתום", "כתומה"  -> Belt.ORANGE
            "green",  "ירוק", "ירוקה"  -> Belt.GREEN
            "blue",   "כחול", "כחולה"  -> Belt.BLUE
            "brown",  "חום",  "חומה"   -> Belt.BROWN
            "black",  "שחור", "שחורה"  -> Belt.BLACK
            else -> Belt.values().firstOrNull { it.id.equals(v, true) || it.heb.contains(s, true) }
        }
    }

    // סדר עדיפויות: מזהה מה-sp → מזהה מה-kmi_user → טקסט חופשי מה-kmi_user → ברירת מחדל לבן
    return fromId(idFromSettings)
        ?: fromId(idFromUser)
        ?: fromText(rawText)
        ?: Belt.WHITE
}

@Composable
fun StatRow(
    title: String,
    percent: Int,
    color: Color,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
) {
    val pct = percent.coerceIn(0, 100)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${pct}%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = color
            )
            Text(
                text = "חגורה: $title",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = color,
                textAlign = TextAlign.End
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct / 100f)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
