package il.kmi.app.reminders

import android.content.SharedPreferences

class ReminderPrefs(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_ENABLED_TRAINEE = "daily_exercise_reminder_enabled_trainee"
        private const val KEY_ENABLED_COACH = "daily_exercise_reminder_enabled_coach"
        private const val KEY_HOUR = "daily_exercise_reminder_hour"
        private const val KEY_MINUTE = "daily_exercise_reminder_minute"
        private const val KEY_LAST_ITEM_KEY = "daily_exercise_reminder_last_item_key"
    }

    fun isEnabledForTrainee(): Boolean {
        return prefs.getBoolean(KEY_ENABLED_TRAINEE, false)
    }

    fun setEnabledForTrainee(value: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ENABLED_TRAINEE, value)
            .apply()
    }

    fun isEnabledForCoach(): Boolean {
        return prefs.getBoolean(KEY_ENABLED_COACH, false)
    }

    fun setEnabledForCoach(value: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ENABLED_COACH, value)
            .apply()
    }

    fun getHour(): Int {
        return prefs.getInt(KEY_HOUR, 20)
    }

    fun setHour(value: Int) {
        prefs.edit()
            .putInt(KEY_HOUR, value)
            .apply()
    }

    fun getMinute(): Int {
        return prefs.getInt(KEY_MINUTE, 0)
    }

    fun setMinute(value: Int) {
        prefs.edit()
            .putInt(KEY_MINUTE, value)
            .apply()
    }

    fun getLastItemKey(): String? {
        return prefs.getString(KEY_LAST_ITEM_KEY, null)
    }

    fun setLastItemKey(value: String?) {
        prefs.edit()
            .putString(KEY_LAST_ITEM_KEY, value)
            .apply()
    }

    fun isEnabledForRole(isCoach: Boolean): Boolean {
        return if (isCoach) {
            isEnabledForCoach()
        } else {
            isEnabledForTrainee()
        }
    }

    fun setEnabledForRole(isCoach: Boolean, value: Boolean) {
        if (isCoach) {
            setEnabledForCoach(value)
        } else {
            setEnabledForTrainee(value)
        }
    }
}