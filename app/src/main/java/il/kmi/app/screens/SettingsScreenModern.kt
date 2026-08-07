package il.kmi.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.SharedPreferences
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import il.kmi.shared.domain.Belt
import android.content.Context
import android.app.TimePickerDialog
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import il.kmi.app.reminders.TrainingReminderScheduler
import il.kmi.app.KmiCalendarSync
import il.kmi.app.hasCalendarPermission
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
import android.widget.TextView
import android.util.TypedValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.pm.PackageInfoCompat
import il.kmi.app.reminders.ReminderPrefs
import il.kmi.app.reminders.DailyReminderScheduler
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.ui.AppFontSize
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.Dialog

//======================================================================================

typealias StatsVm = AppStatsVm

/* ===== Helpers לשיתוף/דירוג/משוב ===== */
private fun openEmailFeedback(
    ctx: android.content.Context,
    to: String,
    subject: String,
    body: String = "",
    isEnglish: Boolean = false
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    val chooserTitle = if (isEnglish) "Send feedback" else "שלח משוב"
    val errorText = if (isEnglish) {
        "No email app was found"
    } else {
        "לא נמצאה אפליקציית דוא״ל"
    }

    try {
        ctx.startActivity(Intent.createChooser(intent, chooserTitle))
    } catch (_: Exception) {
        android.widget.Toast.makeText(
            ctx,
            errorText,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

private fun openStorePage(ctx: android.content.Context) {
    val pkg = ctx.packageName
    val market = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$pkg")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val web = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        ctx.startActivity(web)
    }
}

private fun shareApp(
    ctx: android.content.Context,
    isEnglish: Boolean = false
) {
    val text = if (isEnglish) {
        "Download KAMI – Israeli Krav Magen"
    } else {
        "הורידו את KAMI – ק.מ.י"
    }

    val chooserTitle = if (isEnglish) "Share with" else "שתף באמצעות"

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    ctx.startActivity(
        Intent.createChooser(send, chooserTitle)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
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

private fun styleKmiNumberPicker(
    picker: NumberPicker,
    textColor: Int,
    textSizePx: Float
) {
    picker.descendantFocusability =
        NumberPicker.FOCUS_BLOCK_DESCENDANTS

    picker.setFormatter { value ->
        value.toString()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        picker.textColor = textColor
    }

    for (i in 0 until picker.childCount) {
        (picker.getChildAt(i) as? TextView)?.apply {
            setTextColor(textColor)
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                textSizePx
            )
        }
    }

    picker.invalidate()
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
    onOpenRegistration: () -> Unit = {},
    onFontSizeChange: (String) -> Unit = {},
    vm: StatsVm
) {

    val discardAndExit: () -> Unit = { onBack() }
    val saveAllAndApply: () -> Unit = { /* preferences already saved inline */ }

    val appCtxLang = LocalContext.current
    val languageManager = remember { AppLanguageManager(appCtxLang) }
    var currentLanguage by remember { mutableStateOf(languageManager.getCurrentLanguage()) }
    val isEnglish =
        currentLanguage == AppLanguage.ENGLISH

    val textAlignPrimary =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    val horizontalEnd =
        if (isEnglish) Alignment.Start else Alignment.End

    val isDarkMode =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    fun tr(he: String, en: String): String =
        if (isEnglish) en else he

    fun formatLeadTime(totalMinutes: Int): String {
        val safeMinutes = totalMinutes.takeIf { it > 0 } ?: 60
        val hours = safeMinutes / 60
        val minutes = safeMinutes % 60

        return if (isEnglish) {
            when {
                hours > 0 && minutes > 0 -> "$hours h $minutes min before training"
                hours > 0 -> "$hours h before training"
                else -> "$minutes min before training"
            }
        } else {
            when {
                hours > 0 && minutes > 0 -> "$hours שעה ו־$minutes דקות לפני האימון"
                hours > 0 -> "$hours שעה לפני האימון"
                else -> "$minutes דקות לפני האימון"
            }
        }
    }

    val fullName by remember {
        mutableStateOf(
            sp.getString("fullName", null)
                ?: tr("שם מלא לא מוגדר", "Full name not set")
        )
    }
    val phone by remember { mutableStateOf(sp.getString("phone", "") ?: "") }
    val email by remember { mutableStateOf(sp.getString("email", "") ?: "") }
    val region by remember { mutableStateOf(sp.getString("region", "") ?: "") }
    val branch by remember { mutableStateOf(sp.getString("branch", "") ?: "") }

    val rankDisplayName by remember(currentLanguage) {
        mutableStateOf(
            traineeRankDisplayName(
                rawId = registeredRankId(appCtxLang, sp),
                isEnglish = isEnglish
            )
        )
    }

    val isCoachInit = sp.getString("user_role", "trainee") == "coach"
    var isCoach by rememberSaveable { mutableStateOf(isCoachInit) }
// === כלים גלובליים למסך: Haptics + Toast + Overlay טעינה ===
    val haptic = rememberHaptics()
    val toast = rememberToaster()
    var isBusy by rememberSaveable { mutableStateOf(false) }

    // סטייטים להגדרות (נשמרים ב־SP כדי להשאיר עקביות)
    var remindersEnabled by rememberSaveable {
        mutableStateOf(
            sp.getBoolean(
                "training_reminders_enabled",
                true
            )
        )
    }
    var reminderMinutes by rememberSaveable {
        mutableStateOf(
            sp.getInt(
                "training_reminder_minutes",
                sp.getInt("lead_minutes", 60)
            ).takeIf { it > 0 } ?: 60
        )
    }

    var showTrainingReminderTimePicker by rememberSaveable {
        mutableStateOf(false)
    }
    var themeModeLocal by rememberSaveable(themeMode) {
        mutableStateOf(
            when {
                themeMode == "dark" -> "dark"
                themeMode == "light" -> "light"
                themeMode == "system" -> "system"

                kmiPrefs.themeMode == "dark" -> "dark"
                kmiPrefs.themeMode == "light" -> "light"
                kmiPrefs.themeMode == "system" -> "system"

                sp.getString("theme_mode", "") == "dark" -> "dark"
                sp.getString("theme_mode", "") == "system" -> "system"

                else -> "light"
            }
        )
    }

    var fontSizeModeLocal by rememberSaveable {
        mutableStateOf(
            AppFontSize.fromStorageValue(
                kmiPrefs.fontSize
                    .takeIf { it.isNotBlank() }
                    ?: sp.getString(
                        AppFontSize.PREFERENCE_KEY,
                        AppFontSize.MEDIUM.storageValue
                    )
            ).storageValue
        )
    }

    // --- תרגיל יומי ---
    val appCtx = LocalContext.current
    val reminderPrefsSp =
        remember { appCtx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE) }
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

    fun hasNotificationPermissionForDailyReminder(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    appCtx,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
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

        if (enabled) {
            if (!hasNotificationPermissionForDailyReminder()) {
                return
            }

            if (!DailyReminderScheduler.canScheduleExactDailyReminder(appCtx)) {
                android.widget.Toast.makeText(
                    appCtx,
                    tr(
                        "כדי לקבל תרגיל יומי בדיוק בשעה שבחרת, אשר הרשאת התראות ושעונים במסך הבא",
                        "To receive the daily exercise exactly at the selected time, approve notifications and alarms on the next screen"
                    ),
                    android.widget.Toast.LENGTH_LONG
                ).show()

                DailyReminderScheduler.openExactAlarmPermissionSettings(appCtx)

                // משאירים fallback כדי שלא תאבד לגמרי התראה,
                // אבל אחרי אישור ההרשאה כדאי להיכנס שוב להגדרות ולשמור שעה מחדש.
                DailyReminderScheduler.cancel(appCtx)
                DailyReminderScheduler.schedule(appCtx)
                return
            }

            DailyReminderScheduler.cancel(appCtx)
            DailyReminderScheduler.schedule(appCtx)

            android.widget.Toast.makeText(
                appCtx,
                tr(
                    "התראה יומית נקבעה לשעה %02d:%02d".format(safeHour, safeMinute),
                    "Daily reminder was set for %02d:%02d".format(safeHour, safeMinute)
                ),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            DailyReminderScheduler.cancel(appCtx)

            android.widget.Toast.makeText(
                appCtx,
                tr(
                    "התראת התרגיל היומי בוטלה",
                    "Daily exercise reminder was disabled"
                ),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ---- גרדיאנט כותרת אחיד לכל התפקידים ----
    val headerBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF062B4A).copy(alpha = 0.96f),
            Color(0xFF0F5E9C).copy(alpha = 0.92f),
            Color(0xFF062B4A).copy(alpha = 0.96f)
        )
    )

// צבע "פרימיום" אחיד לאייקונים בכותרות הכרטיסים
    val sectionIconTint = remember {
        Color(0xFF0F5E9C)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = tr("הגדרות", "Settings"),
                onHome = { onBack() },
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                lockSearch = true,
                centerTitle = true
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            if (isDarkMode) {
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surface,
                                    Color(0xFF10243A),
                                    Color(0xFF0A3657),
                                    Color(0xFF041E33)
                                )
                            } else {
                                listOf(
                                    Color(0xFFF8FBFF),
                                    Color(0xFFEAF4FF),
                                    Color(0xFFB7DDF7),
                                    Color(0xFF1F78B4),
                                    Color(0xFF062B4A)
                                )
                            }
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .padding(bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // --- פיילוט עיצוב פרימיום: כללי + תזכורות ---
                SettingsListSection(
                    title = tr("כללי ותזכורות", "General and reminders"),
                    subtitle = tr(
                        "שפה, תזכורות אימון והגדרות שימוש יומי",
                        "Language, training reminders and daily usage settings"
                    ),
                    icon = Icons.Filled.Tune,
                    iconTint = sectionIconTint
                ) {
                    SettingsListItem(
                        title = tr("שפה", "Language"),
                        value = if (currentLanguage == AppLanguage.ENGLISH) "English" else "עברית",
                        icon = Icons.Filled.Language,
                        iconTint = Color(0xFF2A78E4),
                        topRounded = true
                    ) {
                        val selectedIndex = if (currentLanguage == AppLanguage.HEBREW) 0 else 1

                        fun applyLanguage(newLanguage: AppLanguage) {
                            if (currentLanguage == newLanguage) return

                            languageManager.setLanguage(newLanguage)
                            currentLanguage = newLanguage

                            android.widget.Toast.makeText(
                                appCtxLang,
                                if (newLanguage == AppLanguage.ENGLISH) {
                                    "Language changed to English"
                                } else {
                                    "השפה שונתה לעברית"
                                },
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tr(
                                    "בחר שפת ממשק",
                                    "Choose interface language"
                                ),
                                style = KmiTypography.secondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = textAlignPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            TabRow(selectedTabIndex = selectedIndex) {
                                Tab(
                                    selected = selectedIndex == 0,
                                    onClick = {
                                        applyLanguage(AppLanguage.HEBREW)
                                    },
                                    text = {
                                        Text(
                                            text = "עברית",
                                            style = KmiTypography.action
                                        )
                                    }
                                )

                                Tab(
                                    selected = selectedIndex == 1,
                                    onClick = {
                                        applyLanguage(AppLanguage.ENGLISH)
                                    },
                                    text = {
                                        Text(
                                            text = "English",
                                            style = KmiTypography.action
                                        )
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    SettingsListItem(
                        title = tr("תזכורות אימון", "Training reminders"),
                        value = if (remindersEnabled) {
                            formatLeadTime(reminderMinutes)
                        } else {
                            tr("כבוי", "Off")
                        },
                        icon = Icons.Filled.AlarmOn,
                        iconTint = Color(0xFF7B61D9),
                        bottomRounded = true
                    ) {
                        val ctx = LocalContext.current

                        val notifPermissionLauncher =
                            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                                if (granted) {
                                    val lead = reminderMinutes.takeIf { it > 0 } ?: 60

                                    sp.edit()
                                        .putBoolean("training_reminders_enabled", true)
                                        .putInt("training_reminder_minutes", lead)
                                        .putInt("lead_minutes", lead)
                                        .apply()

                                    kmiPrefs.remindersOn = true
                                    kmiPrefs.leadMinutes = lead

                                    TrainingReminderScheduler.scheduleWeeklyTrainingAlarms(
                                        context = ctx.applicationContext,
                                        leadMinutes = lead
                                    )
                                } else {
                                    remindersEnabled = false

                                    sp.edit()
                                        .putBoolean("training_reminders_enabled", false)
                                        .apply()

                                    kmiPrefs.remindersOn = false

                                    TrainingReminderScheduler.cancelWeeklyTrainingAlarms(
                                        context = ctx.applicationContext
                                    )
                                }
                            }

                        fun scheduleTrainingReminders(leadMinutes: Int) {
                            val lead = leadMinutes.takeIf { it > 0 } ?: 60

                            reminderMinutes = lead

                            sp.edit()
                                .putBoolean("training_reminders_enabled", remindersEnabled)
                                .putInt("training_reminder_minutes", lead)
                                .putInt("lead_minutes", lead)
                                .apply()

                            kmiPrefs.remindersOn = remindersEnabled
                            kmiPrefs.leadMinutes = lead

                            if (remindersEnabled) {
                                TrainingReminderScheduler.scheduleWeeklyTrainingAlarms(
                                    context = ctx.applicationContext,
                                    leadMinutes = lead
                                )
                            } else {
                                TrainingReminderScheduler.cancelWeeklyTrainingAlarms(
                                    context = ctx.applicationContext
                                )
                            }
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
                                    text = if (remindersEnabled) {
                                        tr(
                                            "בחר כמה זמן לפני האימון לקבל התראה",
                                            "Choose exactly how long before training to receive a reminder"
                                        )
                                    } else {
                                        tr(
                                            "הפעל תזכורות לפני אימונים",
                                            "Enable reminders before training sessions"
                                        )
                                    },
                                    style = KmiTypography.secondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    textAlign = textAlignPrimary
                                )

                                Switch(
                                    checked = remindersEnabled,
                                    onCheckedChange = { enabled ->
                                        remindersEnabled = enabled

                                        val lead = reminderMinutes.takeIf { it > 0 } ?: 60

                                        sp.edit()
                                            .putBoolean("training_reminders_enabled", enabled)
                                            .putInt("training_reminder_minutes", lead)
                                            .putInt("lead_minutes", lead)
                                            .apply()

                                        kmiPrefs.remindersOn = enabled
                                        kmiPrefs.leadMinutes = lead

                                        if (enabled) {
                                            if (Build.VERSION.SDK_INT >= 33) {
                                                val alreadyGranted =
                                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                                        ctx,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    ) == PackageManager.PERMISSION_GRANTED

                                                if (alreadyGranted) {
                                                    TrainingReminderScheduler.scheduleWeeklyTrainingAlarms(
                                                        context = ctx.applicationContext,
                                                        leadMinutes = lead
                                                    )
                                                } else {
                                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            } else {
                                                TrainingReminderScheduler.scheduleWeeklyTrainingAlarms(
                                                    context = ctx.applicationContext,
                                                    leadMinutes = lead
                                                )
                                            }
                                        } else {
                                            TrainingReminderScheduler.cancelWeeklyTrainingAlarms(
                                                context = ctx.applicationContext
                                            )
                                        }
                                    }
                                )
                            }

                            if (remindersEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 13.dp, vertical = 11.dp),
                                        verticalArrangement = Arrangement.spacedBy(9.dp),
                                        horizontalAlignment = horizontalEnd
                                    ) {
                                        Text(
                                            text = formatLeadTime(reminderMinutes),
                                            style = KmiTypography.cardTitle.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = textAlignPrimary,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text = tr(
                                                "ברירת המחדל היא 60 דקות אם לא נבחר זמן אחר.",
                                                "Default is 60 minutes if no other time is selected."
                                            ),
                                            style = KmiTypography.caption,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = textAlignPrimary,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                showTrainingReminderTimePicker = true
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = tr("בחר זמן מדויק", "Choose exact time")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showTrainingReminderTimePicker) {
                            val initialLead = reminderMinutes.takeIf { it > 0 } ?: 60
                            var selectedHours by rememberSaveable {
                                mutableIntStateOf(initialLead / 60)
                            }
                            var selectedMinutes by rememberSaveable {
                                mutableIntStateOf(initialLead % 60)
                            }
                            val pickerTextColor =
                                MaterialTheme.colorScheme.onSurface.toArgb()

                            val pickerTextSizePx =
                                with(LocalDensity.current) {
                                    KmiTypography.metric.fontSize.toPx()
                                }

                            Dialog(
                                onDismissRequest = {
                                    showTrainingReminderTimePicker = false
                                }
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(30.dp),
                                    color = if (isDarkMode) {
                                        MaterialTheme.colorScheme.surface
                                    } else {
                                        Color(0xFFF6F1FB)
                                    },
                                    tonalElevation = 0.dp,
                                    shadowElevation = 16.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 18.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFF062B4A),
                                                            Color(0xFF0F5E9C),
                                                            Color(0xFF5B35D5)
                                                        )
                                                    )
                                                )
                                                .padding(horizontal = 18.dp, vertical = 16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = horizontalEnd
                                            ) {
                                                Text(
                                                    text = tr(
                                                        "בחירת זמן לפני האימון",
                                                        "Choose reminder time before training"
                                                    ),
                                                    style = KmiTypography.screenTitle.copy(
                                                        fontWeight = FontWeight.ExtraBold
                                                    ),
                                                    color = Color.White,
                                                    textAlign = textAlignPrimary,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = tr(
                                                        "בחר שעות ודקות. לדוגמה: שעה ו־18 דקות.",
                                                        "Choose hours and minutes. For example: 1 hour and 18 minutes."
                                                    ),
                                                    style = KmiTypography.body,
                                                    color = Color.White.copy(alpha = 0.92f),
                                                    textAlign = textAlignPrimary,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = Color.White.copy(alpha = 0.14f),
                                                    tonalElevation = 0.dp
                                                ) {
                                                    Text(
                                                        text = formatLeadTime(
                                                            selectedHours.coerceIn(0, 6) * 60 +
                                                                    selectedMinutes.coerceIn(0, 59)
                                                        ),
                                                        modifier = Modifier.padding(
                                                            horizontal = 14.dp,
                                                            vertical = 10.dp
                                                        ),
                                                        style = KmiTypography.cardTitle.copy(
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (isEnglish) {
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(22.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    tonalElevation = 0.dp,
                                                    shadowElevation = 4.dp
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                horizontal = 12.dp,
                                                                vertical = 12.dp
                                                            ),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = tr("Hours", "Hours"),
                                                            style = KmiTypography.action,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        AndroidView(
                                                            factory = { viewContext ->
                                                                NumberPicker(viewContext).apply {
                                                                    minValue = 0
                                                                    maxValue = 6
                                                                    value =
                                                                        selectedHours.coerceIn(0, 6)
                                                                    wrapSelectorWheel = false
                                                                    setOnValueChangedListener { _, _, newValue ->
                                                                        selectedHours = newValue
                                                                    }
                                                                    styleKmiNumberPicker(
                                                                        picker = this,
                                                                        textColor = pickerTextColor,
                                                                        textSizePx = pickerTextSizePx
                                                                    )
                                                                }
                                                            },
                                                            update = { picker ->
                                                                picker.value =
                                                                    selectedHours.coerceIn(0, 6)
                                                                styleKmiNumberPicker(
                                                                    picker = picker,
                                                                    textColor = pickerTextColor,
                                                                    textSizePx = pickerTextSizePx
                                                                )
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(170.dp)
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(22.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    tonalElevation = 0.dp,
                                                    shadowElevation = 4.dp
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                horizontal = 12.dp,
                                                                vertical = 12.dp
                                                            ),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = tr("Minutes", "Minutes"),
                                                            style = KmiTypography.action,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        AndroidView(
                                                            factory = { viewContext ->
                                                                NumberPicker(viewContext).apply {
                                                                    minValue = 0
                                                                    maxValue = 59
                                                                    value =
                                                                        selectedMinutes.coerceIn(
                                                                            0,
                                                                            59
                                                                        )
                                                                    wrapSelectorWheel = true
                                                                    setOnValueChangedListener { _, _, newValue ->
                                                                        selectedMinutes = newValue
                                                                    }
                                                                    styleKmiNumberPicker(
                                                                        picker = this,
                                                                        textColor = pickerTextColor,
                                                                        textSizePx = pickerTextSizePx
                                                                    )
                                                                }
                                                            },
                                                            update = { picker ->
                                                                picker.value =
                                                                    selectedMinutes.coerceIn(0, 59)
                                                                styleKmiNumberPicker(
                                                                    picker = picker,
                                                                    textColor = pickerTextColor,
                                                                    textSizePx = pickerTextSizePx
                                                                )
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(170.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(22.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    tonalElevation = 0.dp,
                                                    shadowElevation = 4.dp
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                horizontal = 12.dp,
                                                                vertical = 12.dp
                                                            ),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = tr("דקות", "Minutes"),
                                                            style = KmiTypography.action,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        AndroidView(
                                                            factory = { viewContext ->
                                                                NumberPicker(viewContext).apply {
                                                                    minValue = 0
                                                                    maxValue = 59
                                                                    value =
                                                                        selectedMinutes.coerceIn(
                                                                            0,
                                                                            59
                                                                        )
                                                                    wrapSelectorWheel = true
                                                                    setOnValueChangedListener { _, _, newValue ->
                                                                        selectedMinutes = newValue
                                                                    }
                                                                    styleKmiNumberPicker(
                                                                        picker = this,
                                                                        textColor = pickerTextColor,
                                                                        textSizePx = pickerTextSizePx
                                                                    )
                                                                }
                                                            },
                                                            update = { picker ->
                                                                picker.value =
                                                                    selectedMinutes.coerceIn(0, 59)
                                                                styleKmiNumberPicker(
                                                                    picker = picker,
                                                                    textColor = pickerTextColor,
                                                                    textSizePx = pickerTextSizePx
                                                                )
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(170.dp)
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(22.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    tonalElevation = 0.dp,
                                                    shadowElevation = 4.dp
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                horizontal = 12.dp,
                                                                vertical = 12.dp
                                                            ),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = tr("שעות", "Hours"),
                                                            style = KmiTypography.action,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        AndroidView(
                                                            factory = { viewContext ->
                                                                NumberPicker(viewContext).apply {
                                                                    minValue = 0
                                                                    maxValue = 6
                                                                    value =
                                                                        selectedHours.coerceIn(0, 6)
                                                                    wrapSelectorWheel = false
                                                                    setOnValueChangedListener { _, _, newValue ->
                                                                        selectedHours = newValue
                                                                    }
                                                                    styleKmiNumberPicker(
                                                                        picker = this,
                                                                        textColor = pickerTextColor,
                                                                        textSizePx = pickerTextSizePx
                                                                    )
                                                                }
                                                            },
                                                            update = { picker ->
                                                                picker.value =
                                                                    selectedHours.coerceIn(0, 6)
                                                                styleKmiNumberPicker(
                                                                    picker = picker,
                                                                    textColor = pickerTextColor,
                                                                    textSizePx = pickerTextSizePx
                                                                )
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(170.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    showTrainingReminderTimePicker = false
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(18.dp)
                                            ) {
                                                Text(
                                                    text = tr("ביטול", "Cancel"),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val totalMinutes =
                                                        selectedHours.coerceIn(0, 6) * 60 +
                                                                selectedMinutes.coerceIn(0, 59)

                                                    val lead = totalMinutes.takeIf { it > 0 } ?: 60

                                                    scheduleTrainingReminders(lead)
                                                    showTrainingReminderTimePicker = false
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(18.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF5B35D5),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text(
                                                    text = tr("שמירה", "Save"),
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsListSection(
                    title = tr("התראות וסנכרון", "Notifications and sync"),
                    subtitle = tr(
                        "תרגיל יומי, אימונים חופשיים וסנכרון ליומן",
                        "Daily exercise, free training reminders and calendar sync"
                    ),
                    icon = Icons.Filled.NotificationsActive,
                    iconTint = sectionIconTint
                ) {
                    SettingsListItem(
                        title = tr("תרגיל יומי", "Daily exercise"),
                        value = if (dailyReminderEnabled) {
                            String.format("%02d:%02d", dailyReminderHour, dailyReminderMinute)
                        } else {
                            tr("כבוי", "Off")
                        },
                        icon = Icons.Filled.NotificationsActive,
                        iconTint = Color(0xFF2A78E4),
                        topRounded = true
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

                                    android.widget.Toast.makeText(
                                        ctx,
                                        tr(
                                            "לא ניתן להפעיל תרגיל יומי בלי הרשאת התראות",
                                            "Daily exercise reminder requires notification permission"
                                        ),
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
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
                                    text = if (isCoach) {
                                        tr(
                                            "המאמן יכול לכבות או להפעיל תרגיל יומי לעצמו",
                                            "The coach can enable or disable a daily exercise for themselves"
                                        )
                                    } else {
                                        tr(
                                            "שלח לי בכל יום תרגיל מהחגורה הבאה",
                                            "Send me a daily exercise from the next belt"
                                        )
                                    },
                                    style = KmiTypography.secondary,
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
                                                dailyReminderHour = hourOfDay
                                                dailyReminderMinute = minute

                                                reminderPrefs.setHour(hourOfDay)
                                                reminderPrefs.setMinute(minute)
                                                reminderPrefs.setEnabledForRole(isCoach, true)
                                                dailyReminderEnabled = true

                                                if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermissionForDailyReminder()) {
                                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    applyDailyReminderSettings(
                                                        enabled = true,
                                                        hour = hourOfDay,
                                                        minute = minute
                                                    )
                                                }
                                            },
                                            dailyReminderHour,
                                            dailyReminderMinute,
                                            true
                                        ).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = tr(
                                            "שעת התזכורת: $formattedDailyTime",
                                            "Reminder time: $formattedDailyTime"
                                        ),
                                        style = KmiTypography.action,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Text(
                                    text = tr(
                                        "תקבל התראה יומית עם אפשרות לפתוח כרטיס תרגיל, לשמור למועדפים ולקבל תרגיל נוסף.",
                                        "You will receive a daily reminder with options to open the exercise card, save it to favorites, and get another exercise."
                                    ),
                                    style = KmiTypography.caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = textAlignPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    SettingsListItem(
                        title = tr("תזכורות אימונים חופשיים", "Free training reminders"),
                        value = if (sp.getBoolean("free_sessions_reminders_enabled", false)) {
                            tr("פעיל", "On")
                        } else {
                            tr("כבוי", "Off")
                        },
                        icon = Icons.Filled.NotificationsActive,
                        iconTint = Color(0xFF16A34A)
                    ) {
                        val ctx = LocalContext.current

                        var freeRemindersEnabled by rememberSaveable {
                            mutableStateOf(sp.getBoolean("free_sessions_reminders_enabled", false))
                        }

                        val notifPermissionLauncher =
                            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                                if (!granted) {
                                    freeRemindersEnabled = false
                                    sp.edit().putBoolean("free_sessions_reminders_enabled", false)
                                        .apply()
                                }
                            }

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
                                style = KmiTypography.secondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = textAlignPrimary
                            )

                            Switch(
                                checked = freeRemindersEnabled,
                                onCheckedChange = { enabled ->
                                    freeRemindersEnabled = enabled
                                    sp.edit()
                                        .putBoolean("free_sessions_reminders_enabled", enabled)
                                        .apply()

                                    if (enabled && Build.VERSION.SDK_INT >= 33) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    SettingsListItem(
                        title = tr("סנכרון ליומן במכשיר", "Device calendar sync"),
                        value = if (sp.getBoolean("calendar_sync_selected_enabled", false)) {
                            val selectedCalendarDisplayCollapsed =
                                sp.getString("calendar_sync_selected_calendar_display", "")
                                    .orEmpty()

                            if (selectedCalendarDisplayCollapsed.isNotBlank()) {
                                tr(
                                    "מסוכרן: $selectedCalendarDisplayCollapsed",
                                    "Synced: $selectedCalendarDisplayCollapsed"
                                )
                            } else {
                                tr("מסוכרן", "Synced")
                            }
                        } else {
                            tr("לא מסוכרן", "Not synced")
                        },
                        icon = Icons.Filled.Event,
                        iconTint = Color(0xFF0284C7),
                        bottomRounded = true
                    ) {
                        val appCtx = LocalContext.current
                        var selectedSyncEnabled by rememberSaveable {
                            mutableStateOf(sp.getBoolean("calendar_sync_selected_enabled", false))
                        }
                        var selectedCalendarId by rememberSaveable {
                            mutableLongStateOf(
                                sp.getLong(
                                    "calendar_sync_selected_calendar_id",
                                    -1L
                                )
                            )
                        }
                        var selectedCalendarDisplay by rememberSaveable {
                            mutableStateOf(
                                sp.getString("calendar_sync_selected_calendar_display", "") ?: ""
                            )
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
                                sp.edit().putBoolean("calendar_sync_selected_enabled", false)
                                    .apply()
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
                                        val ok = KmiCalendarSync.upsertAllToSelectedCalendar(
                                            appCtx,
                                            selectedCalendarId
                                        )
                                        if (ok) {
                                            selectedSyncEnabled = true
                                            sp.edit()
                                                .putBoolean("calendar_sync_selected_enabled", true)
                                                .apply()
                                            haptic(true)
                                            toast(
                                                tr(
                                                    "האימונים סונכרנו ליומן שבחרת",
                                                    "Trainings were synced to the selected calendar"
                                                )
                                            )
                                        } else {
                                            selectedSyncEnabled = false
                                            sp.edit()
                                                .putBoolean("calendar_sync_selected_enabled", false)
                                                .apply()
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
                                        sp.edit()
                                            .putBoolean("calendar_sync_selected_enabled", false)
                                            .apply()
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
                                .putString(
                                    "calendar_sync_selected_calendar_display",
                                    selectedCalendarDisplay
                                )
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
                                val ok = KmiCalendarSync.upsertAllToSelectedCalendar(
                                    appCtx,
                                    selectedCalendarId
                                )
                                if (ok) {
                                    selectedSyncEnabled = true
                                    sp.edit()
                                        .putBoolean("calendar_sync_selected_enabled", true)
                                        .apply()
                                    haptic(true)
                                    toast(
                                        tr(
                                            "האימונים סונכרנו ליומן שבחרת",
                                            "Trainings were synced to the selected calendar"
                                        )
                                    )
                                } else {
                                    selectedSyncEnabled = false
                                    sp.edit()
                                        .putBoolean("calendar_sync_selected_enabled", false)
                                        .apply()
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
                                sp.edit()
                                    .putBoolean("calendar_sync_selected_enabled", false)
                                    .apply()
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
                                text = tr(
                                    "סנכרן ליומן חיצוני",
                                    "Sync to external calendar"
                                ),
                                style = KmiTypography.body.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
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
                                        sp.edit()
                                            .putBoolean("calendar_sync_selected_enabled", false)
                                            .apply()
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

                        Text(
                            text = if (selectedCalendarId > 0L && selectedCalendarDisplay.isNotBlank()) {
                                tr(
                                    "יומן שנבחר: $selectedCalendarDisplay",
                                    "Selected calendar: $selectedCalendarDisplay"
                                )
                            } else {
                                tr("עדיין לא נבחר יומן יעד", "No target calendar selected yet")
                            },
                            style = KmiTypography.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = textAlignPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )

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
                                title = {
                                    Text(
                                        text = tr("בחר יומן לסנכרון", "Choose calendar for sync"),
                                        textAlign = textAlignPrimary,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
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
                                                text = tr(
                                                    "לא נמצאו יומנים זמינים לכתיבה במכשיר.",
                                                    "No writable calendars were found on this device."
                                                ),
                                                style = KmiTypography.body,
                                                textAlign = textAlignPrimary,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            writableCalendars.forEach { cal ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { tempSelectedId = cal.id }
                                                        .padding(
                                                            horizontal = 8.dp,
                                                            vertical = 6.dp
                                                        ),
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
                                                            cal.displayName.ifBlank {
                                                                tr(
                                                                    "יומן ללא שם",
                                                                    "Unnamed calendar"
                                                                )
                                                            },
                                                            textAlign = textAlignPrimary
                                                        )
                                                        Text(
                                                            cal.accountName.ifBlank { cal.accountType.ifBlank { "-" } },
                                                            style = KmiTypography.secondary,
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
                                            val selected =
                                                writableCalendars.firstOrNull { it.id == tempSelectedId }
                                            if (selected == null) {
                                                haptic(true)
                                                toast(
                                                    tr(
                                                        "יש לבחור יומן תקין",
                                                        "Please choose a valid calendar"
                                                    )
                                                )
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

                        LoadingOverlay(
                            show = isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                    }
                }

                SettingsListSection(
                    title = tr("ממשק, קול ואבטחה", "Interface, voice and security"),
                    subtitle = tr(
                        "חוויית משתמש, קול, נראות ונעילת אפליקציה",
                        "User experience, voice, appearance and app lock"
                    ),
                    icon = Icons.Filled.Palette,
                    iconTint = sectionIconTint
                ) {
                    SettingsListItem(
                        title = tr("חוויית משתמש", "User experience"),
                        value = tr("צלילים ורטט", "Sounds and haptics"),
                        icon = Icons.Filled.Tune,
                        iconTint = Color(0xFF7C3AED),
                        topRounded = true
                    ) {
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

                        fun setClickSoundsEnabledSafe(enabled: Boolean) {
                            runCatching { il.kmi.shared.Platform.setClickSoundsEnabled(enabled) }
                        }

                        fun setHapticsEnabledSafe(enabled: Boolean) {
                            runCatching { il.kmi.shared.Platform.setHapticsEnabled(enabled) }
                        }

                        val view = LocalView.current

                        fun playFeedbackIfEnabled() {
                            if (clickSounds) {
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                            }
                            if (hapticsOn) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr(
                                    "צליל הקשה בכפתורים",
                                    "Button tap sound"
                                ),
                                style = KmiTypography.body,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = textAlignPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = clickSounds,
                                onCheckedChange = { enabled ->
                                    clickSounds = enabled
                                    sp.edit()
                                        .putBoolean("click_sounds", enabled)
                                        .putBoolean("tap_sound", enabled)
                                        .apply()
                                    setClickSoundsEnabledSafe(enabled)

                                    if (clickSounds) {
                                        playFeedbackIfEnabled()
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr(
                                    "רטט קצר בעת סימון ✓/✗",
                                    "Short haptic on ✓/✗ marking"
                                ),
                                style = KmiTypography.body,
                                color = MaterialTheme.colorScheme.onSurface,
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

                                    if (hapticsOn) {
                                        playFeedbackIfEnabled()
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    SettingsListItem(
                        title = tr("הגדרות קול", "Voice settings"),
                        value = tr("קול גבר / קול אישה", "Male / female voice"),
                        icon = Icons.Filled.SupportAgent,
                        iconTint = Color(0xFF0284C7)
                    ) {
                        val ctx = LocalContext.current

                        val voicePrefs = remember {
                            ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
                        }

                        var cloudVoice by rememberSaveable {
                            mutableStateOf(
                                voicePrefs.getString("voice", "male") ?: "male"
                            )
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
                                style = KmiTypography.secondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = textAlignPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SegmentedButton(
                                    selected = selectedIndex == 0,
                                    onClick = {
                                        setCloudVoice("male")
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = 0,
                                        count = 2
                                    ),
                                    label = {
                                        Text(
                                            text = tr(
                                                "קול גבר",
                                                "Male voice"
                                            ),
                                            style = KmiTypography.action,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )

                                SegmentedButton(
                                    selected = selectedIndex == 1,
                                    onClick = {
                                        setCloudVoice("female")
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = 1,
                                        count = 2
                                    ),
                                    label = {
                                        Text(
                                            text = tr(
                                                "קול אישה",
                                                "Female voice"
                                            ),
                                            style = KmiTypography.action,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )
                            }

                            Text(
                                text = tr(
                                    "הבחירה נשמרת למכשיר ותשפיע על הדיבור בעוזר הקולי.",
                                    "The selection is saved on the device and affects speech in the voice assistant."
                                ),
                                style = KmiTypography.caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = textAlignPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        SettingsListItem(
                            title = tr("נראות אפליקציה", "App appearance"),
                            value = when (themeModeLocal) {
                                "dark" -> tr("מצב כהה", "Dark mode")
                                "system" -> tr("לפי המכשיר", "Device default")
                                else -> tr("מצב בהיר", "Light mode")
                            },
                            icon = Icons.Filled.Palette,
                            iconTint = Color(0xFFD97706)
                        ) {
                            val systemIsDark = isSystemInDarkTheme()

                            fun effectiveModeLabel(): String {
                                return when (themeModeLocal) {
                                    "light" -> tr("מצב פעיל: בהיר", "Active mode: Light")
                                    "dark" -> tr("מצב פעיל: כהה", "Active mode: Dark")
                                    else -> {
                                        if (systemIsDark) {
                                            tr(
                                                "מצב פעיל: לפי המכשיר — כהה",
                                                "Active mode: Device default — Dark"
                                            )
                                        } else {
                                            tr(
                                                "מצב פעיל: לפי המכשיר — בהיר",
                                                "Active mode: Device default — Light"
                                            )
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

                            val themeIndex = when (themeModeLocal) {
                                "system" -> 0
                                "light" -> 1
                                "dark" -> 2
                                else -> 1
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
                                            style = KmiTypography.action
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
                                            style = KmiTypography.action
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
                                            style = KmiTypography.action
                                        )
                                    }
                                )
                            }

                            Text(
                                text = effectiveModeLabel(),
                                style = KmiTypography.caption,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = textAlignPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        SettingsListItem(
                            title = tr("גודל כתב", "Font size"),
                            value = when (
                                AppFontSize.fromStorageValue(fontSizeModeLocal)
                            ) {
                                AppFontSize.SMALL ->
                                    tr("קטן", "Small")

                                AppFontSize.MEDIUM ->
                                    tr("בינוני", "Medium")

                                AppFontSize.LARGE ->
                                    tr("גדול", "Large")
                            },
                            icon = Icons.Filled.AccessibilityNew,
                            iconTint = Color(0xFF0F8B8D)
                        ) {
                            fun applyFontSize(newSize: AppFontSize) {
                                if (fontSizeModeLocal == newSize.storageValue) {
                                    return
                                }

                                fontSizeModeLocal =
                                    newSize.storageValue

                                kmiPrefs.fontSize =
                                    newSize.storageValue

                                sp.edit()
                                    .putString(
                                        AppFontSize.PREFERENCE_KEY,
                                        newSize.storageValue
                                    )
                                    .apply()

                                /*
                                 * עדכון ישיר של MainApp.
                                 * כך גם Dialog שכבר פתוח מקבל
                                 * מיד LocalDensity חדש.
                                 */
                                onFontSizeChange(
                                    newSize.storageValue
                                )
                            }

                            val selectedFontSize =
                                AppFontSize.fromStorageValue(fontSizeModeLocal)

                            val selectedIndex = when (selectedFontSize) {
                                AppFontSize.SMALL -> 0
                                AppFontSize.MEDIUM -> 1
                                AppFontSize.LARGE -> 2
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr(
                                        "בחר גודל כתב אחיד לכל מסכי האפליקציה",
                                        "Choose one font size for all app screens"
                                    ),
                                    style = KmiTypography.secondary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = textAlignPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                TabRow(
                                    selectedTabIndex = selectedIndex
                                ) {
                                    Tab(
                                        selected =
                                            selectedFontSize == AppFontSize.SMALL,
                                        onClick = {
                                            applyFontSize(AppFontSize.SMALL)
                                        },
                                        text = {
                                            Text(
                                                text = tr("קטן", "Small"),
                                                maxLines = 1,
                                                style =
                                                    MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    )

                                    Tab(
                                        selected =
                                            selectedFontSize == AppFontSize.MEDIUM,
                                        onClick = {
                                            applyFontSize(AppFontSize.MEDIUM)
                                        },
                                        text = {
                                            Text(
                                                text = tr("בינוני", "Medium"),
                                                maxLines = 1,
                                                style =
                                                    MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    )

                                    Tab(
                                        selected =
                                            selectedFontSize == AppFontSize.LARGE,
                                        onClick = {
                                            applyFontSize(AppFontSize.LARGE)
                                        },
                                        text = {
                                            Text(
                                                text = tr("גדול", "Large"),
                                                maxLines = 1,
                                                style =
                                                    MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    )
                                }

                                Text(
                                    text = tr(
                                        "השינוי חל מיד ונשמר גם לאחר סגירת האפליקציה.",
                                        "The change applies immediately and remains after closing the app."
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = textAlignPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        SettingsListItem(
                            title = tr("נעילת אפליקציה", "App lock"),
                            value = when (sp.getString("app_lock_mode", "none") ?: "none") {
                                "biometric" -> tr("נעילה באצבע", "Biometric lock")
                                else -> tr("ללא נעילה", "No lock")
                            },
                            icon = Icons.Filled.Lock,
                            iconTint = Color(0xFFE11D48),
                            bottomRounded = true
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
                                                tr(
                                                    "ביומטרי לא זמין במכשיר",
                                                    "Biometric authentication is not available on this device"
                                                ),
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                            lockMode =
                                                sp.getString("app_lock_mode", "none") ?: "none"
                                            return
                                        }

                                        il.kmi.app.security.AppLockStore.setMethod(
                                            ctx,
                                            il.kmi.app.security.AppLockMethod.BIOMETRIC
                                        )
                                        act?.let {
                                            il.kmi.app.security.AppLock.requireIfNeeded(
                                                it,
                                                true
                                            )
                                        }
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
                                            style = KmiTypography.action
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
                                            style = KmiTypography.action
                                        )
                                    }
                                )
                            }

                            val ctxBio = LocalContext.current
                            val bioAvailable = remember(ctxBio) {
                                androidx.biometric.BiometricManager.from(ctxBio)
                                    .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                                        androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
                            }

                            if (!bioAvailable) {
                                Text(
                                    tr(
                                        "ביומטרי לא זמין במכשיר או לא הוגדר למשתמש.",
                                        "Biometric authentication is not available or not configured for this user."
                                    ),
                                    style = KmiTypography.caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = textAlignPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    SettingsListSection(
                        title = tr("מידע וניהול", "Info and management"),
                        subtitle = tr(
                            "נתונים, מסמכים משפטיים, גרסה ותמיכה",
                            "Data, legal documents, version and support"
                        ),
                        icon = Icons.Filled.Storage,
                        iconTint = sectionIconTint
                    ) {
                        SettingsListItem(
                            title = tr("ניהול נתונים", "Data management"),
                            value = tr("מטמון והיסטוריית שידורים", "Cache and broadcast history"),
                            icon = Icons.Filled.Storage,
                            iconTint = Color(0xFF0F766E),
                            topRounded = true
                        ) {
                            val ctx = LocalContext.current
                            val spUser = remember {
                                ctx.getSharedPreferences(
                                    "kmi_user",
                                    Context.MODE_PRIVATE
                                )
                            }
                            val PREF_RECENTS_KEY = "coach_broadcast_recents_json"

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        spUser.edit().remove(PREF_RECENTS_KEY).apply()
                                        toast(
                                            tr(
                                                "היסטוריית השידורים נוקתה",
                                                "Broadcast history cleared"
                                            )
                                        )
                                        haptic(true)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                ) {
                                    Text(tr("נקה היסטוריית שידורים", "Clear broadcast history"))
                                }

                                OutlinedButton(
                                    onClick = {
                                        isBusy = true
                                        val ok = clearAppCache(ctx)
                                        isBusy = false
                                        if (ok) {
                                            toast(tr("נוקו קבצי המטמון", "Cache files cleared"))
                                            haptic(true)
                                        } else {
                                            toast(tr("ניקוי נכשל", "Clear failed"))
                                            haptic(false)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Text(tr("נקה מטמון אפליקציה", "Clear app cache"))
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        SettingsListItem(
                            title = tr("מידע משפטי", "Legal information"),
                            value = tr("פרטיות, תנאים ונגישות", "Privacy, terms and accessibility"),
                            icon = Icons.Filled.Gavel,
                            iconTint = Color(0xFF7C3AED)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenPrivacy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(tr("מדיניות פרטיות", "Privacy policy"))
                                }

                                OutlinedButton(
                                    onClick = onOpenTerms,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(tr("תנאי שימוש", "Terms of use"))
                                }

                                OutlinedButton(
                                    onClick = onOpenAccessibility,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(tr("הצהרת נגישות", "Accessibility statement"))
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        SettingsListItem(
                            title = tr("אודות ותמיכה", "About and support"),
                            value = tr("גרסה, משוב ושיתוף", "Version, feedback and sharing"),
                            icon = Icons.Filled.SupportAgent,
                            iconTint = Color(0xFF0284C7),
                            bottomRounded = true
                        ) {
                            val ctx = LocalContext.current
                            val h = rememberHaptics()

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
                                    tr(
                                        "גרסה ${pInfo.versionName} ($longCode)",
                                        "Version ${pInfo.versionName} ($longCode)"
                                    )
                                }.getOrDefault(tr("גרסה לא ידועה", "Unknown version"))
                            }

                            Text(
                                text = pkgVer,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = textAlignPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val body = buildString {
                                            appendLine("")
                                            appendLine("---")
                                            appendLine(
                                                tr(
                                                    "פרטי מערכת (לעזרה באיתור תקלות):",
                                                    "System details (for troubleshooting):"
                                                )
                                            )
                                            appendLine(
                                                tr(
                                                    "חבילה: ${ctx.packageName}",
                                                    "Package: ${ctx.packageName}"
                                                )
                                            )
                                            appendLine(pkgVer)
                                            appendLine(
                                                tr(
                                                    "מכשיר: ${Build.MANUFACTURER} ${Build.MODEL}",
                                                    "Device: ${Build.MANUFACTURER} ${Build.MODEL}"
                                                )
                                            )
                                            appendLine(
                                                tr(
                                                    "אנדרואיד: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                                                    "Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
                                                )
                                            )
                                        }
                                        openEmailFeedback(
                                            ctx = ctx,
                                            to = "ypo1980@gmail.com",
                                            subject = tr("משוב על האפליקציה", "App feedback"),
                                            body = body,
                                            isEnglish = isEnglish
                                        )
                                        h(true)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Text(tr("שלח משוב", "Send feedback"))
                                }

                                OutlinedButton(
                                    onClick = {
                                        openStorePage(ctx)
                                        h(true)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Text(tr("דרג בחנות", "Rate in store"))
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    shareApp(ctx, isEnglish = isEnglish)
                                    h(true)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text(tr("שתף את האפליקציה", "Share the app"))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp
                    ),
                    color = if (isDarkMode) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    } else {
                        Color(0xFFF4EFFB).copy(alpha = 0.97f)
                    },
                    tonalElevation = 10.dp,
                    shadowElevation = 18.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { discardAndExit() },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor =
                                    if (isDarkMode) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        Color.White.copy(alpha = 0.80f)
                                    },
                                contentColor =
                                    if (isDarkMode) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        Color(0xFF5B35D5)
                                    }
                            )
                        ) {
                            Text(
                                text = tr("ביטול", "Cancel"),
                                style = KmiTypography.action
                            )
                        }

                        Button(
                            onClick = {
                                saveAllAndApply()
                                onBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7B61D9),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            )
                        ) {
                            Text(
                                text = tr("אישור", "Confirm"),
                                style = KmiTypography.action.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }
                }
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
            style = KmiTypography.action,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 3,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BeltsProgressBars(
    vm: StatsVm,
    modifier: Modifier = Modifier
) {
    // סטטיסטיקות הוסרו ממסך ההגדרות.
    // המסך יעבור למסך ייעודי דרך אייקון בסרגל הגלובלי.
    vm
    modifier
    return
}

@Composable
fun SettingsListSection(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val languageManager = remember { AppLanguageManager(context) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val textAlignPrimary =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    val horizontalEnd =
        if (isEnglish) Alignment.Start else Alignment.End

    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color =
                if (isDarkMode) {
                    MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.50f
                    )
                } else {
                    Color.White.copy(alpha = 0.22f)
                }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors =
                            if (isDarkMode) {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            } else {
                                listOf(
                                    Color(0xFFF6F1FA).copy(
                                        alpha = 0.98f
                                    ),
                                    Color(0xFFEAF5FB).copy(
                                        alpha = 0.96f
                                    ),
                                    Color(0xFFF8F4EC).copy(
                                        alpha = 0.94f
                                    )
                                )
                            }
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null && isEnglish) {
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = (
                                iconTint
                                    ?: MaterialTheme.colorScheme.primary
                                ).copy(alpha = 0.12f),
                        modifier = Modifier.size(
                            KmiIconSize.extraLarge
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint =
                                    iconTint
                                        ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(
                                    KmiIconSize.small
                                )
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
                        style = KmiTypography.sectionTitle,
                        textAlign = textAlignPrimary,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = KmiTypography.secondary.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = textAlignPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (icon != null && !isEnglish) {
                    Spacer(Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = (iconTint ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
                        modifier = Modifier.size(KmiIconSize.extraLarge)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint
                                    ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(KmiIconSize.small)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsListItem(
    title: String,
    value: String,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    topRounded: Boolean = false,
    bottomRounded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val languageManager = remember { AppLanguageManager(context) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val textAlignPrimary = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalEnd = if (isEnglish) Alignment.Start else Alignment.End
    val accentColor =
        iconTint ?: MaterialTheme.colorScheme.primary

    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var expanded by rememberSaveable(title) {
        mutableStateOf(false)
    }

    val rowInteraction =
        remember { MutableInteractionSource() }

    val rowShape = RoundedCornerShape(
        topStart = if (topRounded) 18.dp else 0.dp,
        topEnd = if (topRounded) 18.dp else 0.dp,
        bottomStart = if (!expanded && bottomRounded) 18.dp else 0.dp,
        bottomEnd = if (!expanded && bottomRounded) 18.dp else 0.dp
    )

    val expandedContentShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = if (bottomRounded) 18.dp else 0.dp,
        bottomEnd = if (bottomRounded) 18.dp else 0.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(rowShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(
                                alpha = if (isDarkMode) {
                                    0.18f
                                } else {
                                    0.08f
                                }
                            ),
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = if (isDarkMode) {
                                    0.72f
                                } else {
                                    0.10f
                                }
                            ),
                            accentColor.copy(
                                alpha = if (isDarkMode) {
                                    0.10f
                                } else {
                                    0.04f
                                }
                            )
                        )
                    )
                )
                .clickable(
                    interactionSource = rowInteraction,
                    indication = null
                ) { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEnglish) {
                if (icon != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier.size(KmiIconSize.extraLarge)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(KmiIconSize.small)
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = title,
                        style = KmiTypography.cardTitle,
                        textAlign = TextAlign.Left,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = value,
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Left,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(KmiIconSize.small)
                )
            } else {
                if (icon != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier.size(KmiIconSize.large)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(KmiIconSize.tiny)
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
                        style = KmiTypography.cardTitle,
                        textAlign = textAlignPrimary,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = value,
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = textAlignPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.width(10.dp))

                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(KmiIconSize.small)
                )
            }
        }

        if (expanded) {
            HorizontalDivider(
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(expandedContentShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = if (isDarkMode) 0.72f else 0.22f
                                ),
                                accentColor.copy(
                                    alpha = if (isDarkMode) 0.12f else 0.045f
                                )
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                ProvideTextStyle(
                    value = KmiTypography.body
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String? = null,
    collapsedValue: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val languageManager = remember { AppLanguageManager(context) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val textAlignPrimary = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalEnd = if (isEnglish) Alignment.Start else Alignment.End
    val accentColor =
        iconTint ?: MaterialTheme.colorScheme.primary

    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var expanded by rememberSaveable(title) {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(
            color = accentColor.copy(alpha = 0.13f),
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors =
                            if (isDarkMode) {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            } else {
                                listOf(
                                    Color(0xFFF7F2FA).copy(alpha = 0.72f),
                                    Color(0xFFEAF6FB).copy(alpha = 0.64f),
                                    Color(0xFFF8F4EC).copy(alpha = 0.58f)
                                )
                            }
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(0.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEnglish) {
                    if (icon != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.11f),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier.size(KmiIconSize.large)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(KmiIconSize.tiny)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = title,
                            style = KmiTypography.cardTitle.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textAlign = TextAlign.Left,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val secondaryText = if (expanded) {
                            subtitle.orEmpty()
                        } else {
                            collapsedValue?.takeIf { it.isNotBlank() }
                                ?: subtitle.orEmpty()
                        }

                        if (secondaryText.isNotBlank()) {
                            Text(
                                text = secondaryText,
                                style = KmiTypography.secondary.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Left,
                                maxLines = if (expanded) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(KmiIconSize.small)
                    )
                } else {
                    if (icon != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.11f),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier.size(KmiIconSize.large)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(KmiIconSize.tiny)
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 12.0.sp,
                                lineHeight = 14.6.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textAlign = textAlignPrimary,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val secondaryText = if (expanded) {
                            subtitle.orEmpty()
                        } else {
                            collapsedValue?.takeIf { it.isNotBlank() }
                                ?: subtitle.orEmpty()
                        }

                        if (secondaryText.isNotBlank()) {
                            Text(
                                text = secondaryText,
                                style = KmiTypography.secondary.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = textAlignPrimary,
                                maxLines = if (expanded) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(KmiIconSize.small)
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(
                    color = accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(
                                        alpha = if (isDarkMode) 0.72f else 0.10f
                                    ),
                                    accentColor.copy(
                                        alpha = if (isDarkMode) 0.10f else 0.025f
                                    )
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    ProvideTextStyle(
                        value = KmiTypography.body
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

private fun traineeRankDisplayName(
    rawId: String?,
    isEnglish: Boolean
): String {
    return when (rawId?.trim().orEmpty()) {
        "white" -> if (isEnglish) "White belt" else "לבנה"
        "yellow" -> if (isEnglish) "Yellow belt" else "צהובה"
        "orange" -> if (isEnglish) "Orange belt" else "כתומה"
        "green" -> if (isEnglish) "Green belt" else "ירוקה"
        "blue" -> if (isEnglish) "Blue belt" else "כחולה"
        "brown" -> if (isEnglish) "Brown belt" else "חומה"

        "black",
        "שחורה",
        "שחורה דאן 1" -> if (isEnglish) "Black belt Dan 1" else "שחורה דאן 1"

        "black_dan_2" -> if (isEnglish) "Black belt Dan 2" else "שחורה דאן 2"
        "black_dan_3" -> if (isEnglish) "Black belt Dan 3" else "שחורה דאן 3"
        "black_dan_4" -> if (isEnglish) "Black belt Dan 4" else "שחורה דאן 4"
        "black_dan_5" -> if (isEnglish) "Black belt Dan 5" else "שחורה דאן 5"
        "black_dan_6" -> if (isEnglish) "Black belt Dan 6" else "שחורה דאן 6"
        "black_dan_7" -> if (isEnglish) "Black belt Dan 7" else "שחורה דאן 7"
        "black_dan_8" -> if (isEnglish) "Black belt Dan 8" else "שחורה דאן 8"
        "black_dan_9" -> if (isEnglish) "Black belt Dan 9" else "שחורה דאן 9"
        "black_dan_10" -> if (isEnglish) "Black belt Dan 10" else "שחורה דאן 10"

        else -> ""
    }
}

private fun registeredRankId(
    ctx: android.content.Context,
    spSettings: SharedPreferences
): String {
    val spUser = ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

    return listOf(
        spSettings.getString("current_belt", null),
        spSettings.getString("belt_current", null),
        spSettings.getString("belt", null),
        spSettings.getString("belt_id", null),
        spSettings.getString("beltColor", null),
        spSettings.getString("belt_color", null),
        spSettings.getString("rank", null),
        spSettings.getString("rank_id", null),
        spSettings.getString("current_rank", null),
        spUser.getString("current_belt", null),
        spUser.getString("belt_current", null),
        spUser.getString("belt", null),
        spUser.getString("belt_id", null),
        spUser.getString("beltColor", null),
        spUser.getString("belt_color", null),
        spUser.getString("rank", null),
        spUser.getString("rank_id", null),
        spUser.getString("current_rank", null)
    ).firstOrNull { !it.isNullOrBlank() }
        ?.trim()
        .orEmpty()
}

private fun readRegisteredBelt(ctx: android.content.Context, spSettings: SharedPreferences): Belt {
    val spUser = ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

    // 1) מזהה חגורה שנשמר בטופס הרישום (למתאמן): "current_belt" ב-sp או "belt_current" ב-kmi_user
    val idFromSettings = listOf(
        "current_belt",
        "belt_current",
        "belt",
        "belt_id",
        "beltColor",
        "belt_color",
        "rank",
        "rank_id",
        "current_rank"
    ).firstNotNullOfOrNull { key ->
        spSettings.getString(key, null)?.trim()?.takeIf { it.isNotBlank() }
    }.orEmpty()

    val idFromUser = listOf(
        "current_belt",
        "belt_current",
        "belt",
        "belt_id",
        "beltColor",
        "belt_color",
        "rank",
        "rank_id",
        "current_rank"
    ).firstNotNullOfOrNull { key ->
        spUser.getString(key, null)?.trim()?.takeIf { it.isNotBlank() }
    }.orEmpty()

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
            "white", "לבן", "לבנה" -> Belt.WHITE
            "yellow", "צהוב", "צהובה" -> Belt.YELLOW
            "orange", "כתום", "כתומה" -> Belt.ORANGE
            "green", "ירוק", "ירוקה" -> Belt.GREEN
            "blue", "כחול", "כחולה" -> Belt.BLUE
            "brown", "חום", "חומה" -> Belt.BROWN
            "black", "שחור", "שחורה" -> Belt.BLACK
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
                style = KmiTypography.cardTitle.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = color
            )

            Text(
                text = "חגורה: $title",
                style = KmiTypography.cardTitle.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = color,
                textAlign = TextAlign.End,
                maxLines = 2,
                softWrap = true
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
