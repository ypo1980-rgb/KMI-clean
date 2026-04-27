package il.kmi.app.subscription

import android.content.SharedPreferences

private const val KEY_HAS_FULL_ACCESS = "has_full_access"

// ⬅️ מנהל אפליקציה
private const val KEY_IS_ADMIN = "is_admin"

// ⬅️ דגל פתיחה עם קוד סודי (למפתחים / נסיינים)
private const val KEY_DEV_UNLOCK = "dev_unlock"
private const val DEV_UNLOCK_CODE = "34567@"

// 🔒 אם רוצים שהקוד הסודי יפתח הכול — חייב להיות false
private const val FORCE_SUBSCRIPTION_LOCK = false

object KmiAccess {

    // ----------- אדמין / מנהל אפליקציה -----------

    /**
     * סימון המשתמש הנוכחי כאדמין (מנהל אפליקציה).
     * אם value=true → כל המסכים פתוחים בלי מנוי ובלי ניסיון.
     */
    fun setAdmin(sp: SharedPreferences, value: Boolean) {
        sp.edit().putBoolean(KEY_IS_ADMIN, value).apply()

        android.util.Log.e(
            "KMI_ACCESS",
            "setAdmin value=$value savedNow=${sp.getBoolean(KEY_IS_ADMIN, false)}"
        )
    }

    /**
     * האם המשתמש הנוכחי מסומן כאדמין.
     */
    fun isAdmin(sp: SharedPreferences): Boolean =
        sp.getBoolean(KEY_IS_ADMIN, false)

    // ----------- פתיחה עם קוד סודי (למפתחים/נסיינים) -----------

    /** האם במכשיר הזה כבר הוזן קוד סודי תקין בעבר */
    fun hasDevUnlock(sp: SharedPreferences): Boolean =
        sp.getBoolean(KEY_DEV_UNLOCK, false)

    /**
     * ניסיון לפתוח גישה באמצעות קוד סודי.
     * מחזיר true אם הקוד נכון ושומר PREF מקומי במכשיר.
     * משתמש כזה יקבל גישה מלאה, אבל לא יהיה "isAdmin".
     */
    fun tryDevUnlock(sp: SharedPreferences, code: String): Boolean {
        val ok = code.trim() == DEV_UNLOCK_CODE
        if (ok) {
            sp.edit()
                .putBoolean(KEY_DEV_UNLOCK, true)
                .apply()
        }

        android.util.Log.e(
            "KMI_ACCESS",
            "tryDevUnlock ok=$ok savedNow=${sp.getBoolean(KEY_DEV_UNLOCK, false)}"
        )

        return ok
    }

    fun clearDevUnlock(sp: SharedPreferences) {
        sp.edit()
            .putBoolean(KEY_DEV_UNLOCK, false)
            .apply()
    }

    /** כרגע לא מפעילים תקופת ניסיון */
    fun ensureTrialStarted(sp: SharedPreferences) {
        // מושבת זמנית – נשאיר פונקציה ריקה כדי לא לשבור קריאות קיימות
    }

    fun setFullAccess(sp: SharedPreferences, value: Boolean) {
        sp.edit().putBoolean(KEY_HAS_FULL_ACCESS, value).apply()
    }

    fun clearAllAccessFlags(sp: SharedPreferences) {
        sp.edit()
            .putBoolean(KEY_IS_ADMIN, false)
            .putBoolean(KEY_DEV_UNLOCK, false)
            .putBoolean(KEY_HAS_FULL_ACCESS, false)
            .apply()
    }

    /**
     * "גישה מלאה" = מנוי פעיל, אדמין, או פתיחה עם קוד סודי.
     * כרגע יש גם נעילה גלובלית זמנית לפיתוח.
     */
    fun hasFullAccess(sp: SharedPreferences): Boolean {
        val admin = isAdmin(sp)
        val dev = hasDevUnlock(sp)
        val full = sp.getBoolean(KEY_HAS_FULL_ACCESS, false)

        if (FORCE_SUBSCRIPTION_LOCK && !admin && !dev) {
            android.util.Log.e(
                "KMI_ACCESS",
                "hasFullAccess FORCE_SUBSCRIPTION_LOCK=true and no admin/dev unlock -> result=false"
            )
            return false
        }

        android.util.Log.e(
            "KMI_ACCESS",
            "hasFullAccess admin=$admin devUnlock=$dev fullAccess=$full result=${admin || dev || full}"
        )

        return admin || dev || full
    }

    /** כרגע תקופת הניסיון כבויה */
    fun isTrialActive(sp: SharedPreferences): Boolean = false

    /** כרגע אין ספירת ימי ניסיון */
    fun trialDaysLeft(sp: SharedPreferences): Int = 0

    // ===== הרשאות שימוש במסכים =====

    /** אימונים / תרגול רגיל – כרגע פתוח רק בגישה מלאה */
    fun canUseTraining(sp: SharedPreferences): Boolean =
        hasFullAccess(sp)

    /** כלים נוספים – פתוח רק לגישה מלאה (מנוי / קוד / אדמין) */
    fun canUseExtras(sp: SharedPreferences): Boolean =
        hasFullAccess(sp)

    /** פורום – פתוח רק לגישה מלאה (מנוי / קוד / אדמין) */
    fun canUseForum(sp: SharedPreferences): Boolean =
        hasFullAccess(sp)
}
