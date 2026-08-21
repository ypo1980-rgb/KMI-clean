package il.kmi.app.privacy

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * מנהל את מצב הפרטיות בזמן הצגת האפליקציה.
 *
 * המצב נשמר במכשיר ולכן נשאר פעיל גם לאחר
 * סגירה ופתיחה מחדש של האפליקציה.
 *
 * הערך נשמר גם כ־Compose State, ולכן כל מסך
 * שקורא ל־isEnabled() בזמן Composition מתעדכן
 * מיד כאשר מצב ההדגמה משתנה.
 */
object DemoPrivacy {

    private const val PREFS_FILE =
        "kmi_demo_privacy"

    private const val KEY_ENABLED =
        "demo_privacy_enabled"

    @Volatile
    private var initialized = false

    private val enabledState =
        mutableStateOf(false)

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

            enabledState.value =
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
        return enabledState.value
    }

    /**
     * מפעיל או מכבה את מצב ההדגמה ושומר
     * את הבחירה במכשיר.
     *
     * שינוי enabledState גורם גם למסכי Compose
     * הפתוחים להתעדכן מיד.
     */
    fun setEnabled(
        context: Context,
        value: Boolean
    ) {
        val appContext =
            context.applicationContext

        enabledState.value = value
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