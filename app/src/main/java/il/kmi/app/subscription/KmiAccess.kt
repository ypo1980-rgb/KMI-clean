package il.kmi.app.subscription

import android.content.SharedPreferences

private const val KEY_HAS_FULL_ACCESS = "has_full_access"

// ⬅️ מנהל אפליקציה
private const val KEY_IS_ADMIN = "is_admin"

// ⬅️ דגל פתיחה עם קוד סודי (למפתחים / נסיינים)
private const val KEY_DEV_UNLOCK = "dev_unlock"
private const val DEV_UNLOCK_CODE = "34567@"

object KmiAccess {

    // ----------- אדמין / מנהל אפליקציה -----------

    /**
     * סימון המשתמש הנוכחי כאדמין (מנהל אפליקציה).
     * אם value=true → כל המסכים פתוחים בלי מנוי ובלי ניסיון.
     */
    fun setAdmin(sp: SharedPreferences, value: Boolean) {
        sp.edit().putBoolean(KEY_IS_ADMIN, value).apply()
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

    /**
     * "גישה מלאה" = מנוי פעיל, אדמין, או פתיחה עם קוד סודי.
     */
    fun hasFullAccess(sp: SharedPreferences): Boolean =
        isAdmin(sp) ||
                hasDevUnlock(sp) ||
                sp.getBoolean(KEY_HAS_FULL_ACCESS, false)

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
