package il.kmi.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.util.Calendar
import il.kmi.app.halacha.ShabbatHolidayChecker

object DailyReminderScheduler {

    private const val REQUEST_CODE_DAILY_REMINDER = 41021
    private const val PREFS_NAME = "kmi_prefs"
    private const val KEY_USER_ROLE = "kmi.user.role"

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reminderPrefs = ReminderPrefs(prefs)

        val isCoach = isCoachUser(prefs)
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
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            triggerAtMillis =
                ShabbatHolidayChecker.computeNextAllowedTriggerTimeMillis(
                    preferredHour = tomorrow.get(Calendar.HOUR_OF_DAY),
                    preferredMinute = tomorrow.get(Calendar.MINUTE)
                )

            android.util.Log.w(
                "KMI_REMINDER",
                "schedule adjusted triggerAtMillis because computed time was too close/past. adjusted=$triggerAtMillis now=$now"
            )
        }

        android.util.Log.d(
            "KMI_REMINDER",
            "schedule triggerAtMillis=$triggerAtMillis hour=$hour minute=$minute now=$now"
        )

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

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context)

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, reminderPrefs.getHour())
            set(Calendar.MINUTE, reminderPrefs.getMinute())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val triggerAtMillis =
            ShabbatHolidayChecker.computeNextAllowedTriggerTimeMillis(
                preferredHour = calendar.get(Calendar.HOUR_OF_DAY),
                preferredMinute = calendar.get(Calendar.MINUTE)
            )

        android.util.Log.d(
            "KMI_REMINDER",
            "rescheduleNextDay triggerAtMillis=$triggerAtMillis hour=${calendar.get(Calendar.HOUR_OF_DAY)} minute=${calendar.get(Calendar.MINUTE)}"
        )

        scheduleExactAlarm(
            alarmManager = alarmManager,
            triggerAtMillis = triggerAtMillis,
            pendingIntent = pendingIntent
        )
    }

    private fun computeNextTriggerTimeMillis(hour: Int, minute: Int): Long {

        val now = Calendar.getInstance()

        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)

        val targetHour =
            if (dayOfWeek == Calendar.FRIDAY) 13 else hour

        val targetMinute =
            if (dayOfWeek == Calendar.FRIDAY) 0 else minute

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.timeInMillis <= now.timeInMillis) {

            target.add(Calendar.DAY_OF_YEAR, 1)

            val nextDay = target.get(Calendar.DAY_OF_WEEK)

            val nextHour =
                if (nextDay == Calendar.FRIDAY) 13 else hour

            val nextMinute =
                if (nextDay == Calendar.FRIDAY) 0 else minute

            target.set(Calendar.HOUR_OF_DAY, nextHour)
            target.set(Calendar.MINUTE, nextMinute)
        }

        return target.timeInMillis
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
        android.util.Log.d(
            "KMI_REMINDER",
            "scheduleExactAlarm triggerAtMillis=$triggerAtMillis sdk=${android.os.Build.VERSION.SDK_INT}"
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val canExact = alarmManager.canScheduleExactAlarms()

            android.util.Log.d(
                "KMI_REMINDER",
                "canScheduleExactAlarms=$canExact"
            )

            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                android.util.Log.d("KMI_REMINDER", "Scheduled with setExactAndAllowWhileIdle")
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                android.util.Log.d("KMI_REMINDER", "Scheduled with setAndAllowWhileIdle fallback")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            android.util.Log.d("KMI_REMINDER", "Scheduled on pre-Android 12 exact path")
        }
    }

    private fun isCoachUser(prefs: SharedPreferences): Boolean {
        val rawRole = prefs.getString(KEY_USER_ROLE, "trainee").orEmpty()
        return rawRole.trim()
            .lowercase()
            .contains("coach")
    }
}