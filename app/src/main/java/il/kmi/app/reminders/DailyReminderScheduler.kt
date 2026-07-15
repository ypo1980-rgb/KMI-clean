package il.kmi.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import il.kmi.app.halacha.ShabbatHolidayChecker

object DailyReminderScheduler {

    private const val REQUEST_CODE_DAILY_REMINDER = 41021
    private const val PREFS_NAME = "kmi_prefs"
    private const val KEY_USER_ROLE = "kmi.user.role"

    fun canScheduleExactDailyReminder(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun openExactAlarmPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            context.startActivity(intent)
        }
    }

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reminderPrefs = ReminderPrefs(prefs)

        val isCoach = isCoachUser(context, prefs)
        val isEnabled = reminderPrefs.isEnabledForRole(isCoach)

        if (!isEnabled) {
            cancel(context)
            return
        }

        val hour = reminderPrefs.getHour()
        val minute = reminderPrefs.getMinute()

        var triggerAtMillis =
            ShabbatHolidayChecker.computeNextAllowedTriggerTimeMillis(
                preferredHour = hour,
                preferredMinute = minute
            )

        val now = System.currentTimeMillis()

        if (triggerAtMillis <= now + 60_000L) {
            /*
             * מחשבים שוב מנקודת זמן שנמצאת אחרי חלון הביטחון.
             * כך השעה של היום לא נבחרת שוב, והחיפוש מתקדם
             * באמת ליום החוקי הבא.
             */
            triggerAtMillis =
                ShabbatHolidayChecker.computeNextAllowedTriggerTimeMillis(
                    preferredHour = hour,
                    preferredMinute = minute,
                    nowMillis = now + 60_000L
                )
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context)

        scheduleExactAlarm(
            alarmManager = alarmManager,
            triggerAtMillis = triggerAtMillis,
            pendingIntent = pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context)

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleNextDay(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reminderPrefs = ReminderPrefs(prefs)
        val isCoach = isCoachUser(context, prefs)
        val isEnabled = reminderPrefs.isEnabledForRole(isCoach)

        if (!isEnabled) {
            cancel(context)
            return
        }

        // מקור אמת יחיד: תמיד מתזמנים מחדש דרך schedule()
        // כדי להשתמש בשעה/דקה המעודכנות וגם בלוגיקת שבת/חג
        schedule(context)
    }

    private fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleExactAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val canExact = alarmManager.canScheduleExactAlarms()

            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun isCoachUser(
        context: Context,
        prefs: SharedPreferences
    ): Boolean {
        val userPrefs = context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

        /*
         * kmi_user שייך לפרופיל המחובר כרגע ולכן נקרא ראשון.
         * kmi_prefs נשאר רק כתאימות לגרסאות קודמות.
         */
        val rawRole =
            userPrefs.getString(KEY_USER_ROLE, null)
                ?: userPrefs.getString("user_role", null)
                ?: userPrefs.getString("role", null)
                ?: prefs.getString(KEY_USER_ROLE, null)
                ?: prefs.getString("user_role", null)
                ?: prefs.getString("role", null)
                ?: "trainee"

        val clean = rawRole.trim().lowercase()

        return clean == "coach" ||
                clean.contains("coach") ||
                clean.contains("מאמן") ||
                clean.contains("מדריך")
    }
}