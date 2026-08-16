package il.kmi.app.privacy

import android.content.Context

/**
 * מנהל את מצב הפרטיות בזמן הצגת האפליקציה.
 *
 * המצב נשמר במכשיר ולכן נשאר פעיל גם לאחר
 * סגירה ופתיחה מחדש של האפליקציה.
 */
object DemoPrivacy {

    private const val PREFS_FILE =
        "kmi_demo_privacy"

    private const val KEY_ENABLED =
        "demo_privacy_enabled"

    @Volatile
    private var initialized = false

    @Volatile
    private var enabled = false

    /**
     * יש לקרוא לפונקציה פעם אחת בעת פתיחת
     * האפליקציה, לפני הצגת שמות מתאמנים.
     */
    fun initialize(context: Context) {
        if (initialized) {
            return
        }

        synchronized(this) {
            if (initialized) {
                return
            }

            val appContext =
                context.applicationContext

            enabled =
                appContext
                    .getSharedPreferences(
                        PREFS_FILE,
                        Context.MODE_PRIVATE
                    )
                    .getBoolean(
                        KEY_ENABLED,
                        false
                    )

            initialized = true
        }
    }

    /**
     * האם מצב ההדגמה פעיל כרגע.
     */
    fun isEnabled(): Boolean {
        return enabled
    }

    /**
     * מפעיל או מכבה את מצב ההדגמה ושומר
     * את הבחירה במכשיר.
     */
    fun setEnabled(
        context: Context,
        value: Boolean
    ) {
        val appContext =
            context.applicationContext

        enabled = value
        initialized = true

        appContext
            .getSharedPreferences(
                PREFS_FILE,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_ENABLED,
                value
            )
            .apply()
    }

    /**
     * מחליף בין מצב רגיל למצב הדגמה.
     *
     * מחזיר את המצב החדש כדי שאפשר יהיה
     * לעדכן מיד את המתג במסך.
     */
    fun toggle(context: Context): Boolean {
        val newValue =
            !isEnabled()

        setEnabled(
            context = context,
            value = newValue
        )

        return newValue
    }
}