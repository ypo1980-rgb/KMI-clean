package il.kmi.app.onboarding

import android.content.Context

object OnboardingPreferences {

    private const val PREFS_NAME = "kmi_onboarding_preferences"
    private const val KEY_COMPLETED_VERSION = "completed_onboarding_version"

    /*
     * העלאת המספר בעתיד תאפשר להציג מחדש הדרכה מעודכנת
     * לאחר שינוי משמעותי באפליקציה.
     */
    private const val CURRENT_VERSION = 1

    fun hasCompleted(context: Context): Boolean {
        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val completedVersion = preferences.getInt(
            KEY_COMPLETED_VERSION,
            0
        )

        return completedVersion >= CURRENT_VERSION
    }

    fun markCompleted(context: Context) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putInt(
                KEY_COMPLETED_VERSION,
                CURRENT_VERSION
            )
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove(KEY_COMPLETED_VERSION)
            .apply()
    }
}