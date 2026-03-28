package il.kmi.app.reminders

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

object DailyReminderDebug {

    private const val PREFS_NAME = "kmi_prefs"
    private const val KEY_USER_ROLE = "kmi.user.role"

    fun scheduleTestReminder(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reminderPrefs = ReminderPrefs(prefs)

        val isCoach = isCoachUser(prefs)

        val now = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
        }

        reminderPrefs.setHour(now.get(Calendar.HOUR_OF_DAY))
        reminderPrefs.setMinute(now.get(Calendar.MINUTE))
        reminderPrefs.setEnabledForRole(isCoach, true)

        // ===== DEBUG ONLY =====
        // קובע חגורה ידנית כדי לבדוק את כל הזרימה מקצה לקצה
        prefs.edit()
            .putString("registered_belt", "orange")
            .apply()

        android.util.Log.d("KMI_REMINDER", "Scheduling test reminder with debug belt=orange")
        DailyReminderScheduler.schedule(context)
    }

    fun triggerReceiverNow(context: Context) {
        android.util.Log.d("KMI_REMINDER", "Triggering receiver manually now")
        DailyReminderReceiver().onReceive(context, null)
    }

    private fun isCoachUser(prefs: SharedPreferences): Boolean {
        val rawRole = prefs.getString(KEY_USER_ROLE, "trainee").orEmpty()
        return rawRole.trim()
            .lowercase()
            .contains("coach")
    }
}