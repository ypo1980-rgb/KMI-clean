package il.kmi.app.subscription

import android.content.SharedPreferences

private const val KEY_TRIAL_START = "trial_start_millis"
private const val KEY_HAS_FULL_ACCESS = "has_full_access"

// ⬅️ מנהל אפליקציה
private const val KEY_IS_ADMIN = "is_admin"

// ⬅️ חדש: דגל פתיחה עם קוד סודי (ל־5 המפתחים וכו')
private const val KEY_DEV_UNLOCK = "dev_unlock"
// אפשר לשנות לקוד אחר, רק לא לשכוח לעדכן גם באפליקציה
private const val DEV_UNLOCK_CODE = "KMI-SECRET-2025"

// 3 ימים
private const val TRIAL_DURATION_MILLIS: Long = 3L * 24 * 60 * 60 * 1000

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

    /** לקרוא פעם אחרי רישום משתמש חדש / כניסה ראשונה */
    fun ensureTrialStarted(sp: SharedPreferences) {
        // לא צריך תקופת ניסיון לאדמין
        if (isAdmin(sp)) return

        if (!sp.contains(KEY_TRIAL_START)) {
            sp.edit()
                .putLong(KEY_TRIAL_START, System.currentTimeMillis())
                .apply()
        }
    }

    fun setFullAccess(sp: SharedPreferences, value: Boolean) {
        sp.edit().putBoolean(KEY_HAS_FULL_ACCESS, value).apply()
    }

    /**
     * "גישה מלאה" = או מנוי רגיל, או אדמין, או פתיחה עם קוד סודי.
     */
    fun hasFullAccess(sp: SharedPreferences): Boolean =
        isAdmin(sp) ||
                hasDevUnlock(sp) ||
                sp.getBoolean(KEY_HAS_FULL_ACCESS, false)

    fun isTrialActive(sp: SharedPreferences): Boolean {
        // ⬅️ לא צריך "ניסיון" לאדמין – תמיד פתוח
        if (isAdmin(sp)) return false

        val start = sp.getLong(KEY_TRIAL_START, 0L)
        if (start == 0L) return false
        val now = System.currentTimeMillis()
        return now - start < TRIAL_DURATION_MILLIS
    }

    /** כמה ימים נשארו לניסיון (0 אם נגמר / לא התחיל) */
    fun trialDaysLeft(sp: SharedPreferences): Int {
        // לאדמין זה לא רלוונטי – נחזיר 0
        if (isAdmin(sp)) return 0

        val start = sp.getLong(KEY_TRIAL_START, 0L)
        if (start == 0L) return 0
        val now = System.currentTimeMillis()
        val remaining = TRIAL_DURATION_MILLIS - (now - start)
        if (remaining <= 0L) return 0
        val dayMillis = 24L * 60 * 60 * 1000
        return (remaining / dayMillis).toInt().coerceAtLeast(0)
    }

    // ===== הרשאות שימוש במסכים =====

    /** אימונים / תרגול רגיל – פתוח בזמן ניסיון או גישה מלאה */
    fun canUseTraining(sp: SharedPreferences): Boolean =
        hasFullAccess(sp) || isTrialActive(sp)

    /** כלים נוספים – פתוח רק לגישה מלאה (מנוי / קוד / אדמין) */
    fun canUseExtras(sp: SharedPreferences): Boolean =
        hasFullAccess(sp)

    /** פורום – פתוח רק לגישה מלאה (מנוי / קוד / אדמין) */
    fun canUseForum(sp: SharedPreferences): Boolean =
        hasFullAccess(sp)
}
