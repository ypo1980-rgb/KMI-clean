package il.kmi.app.subscription

import android.content.SharedPreferences

private const val KEY_HAS_FULL_ACCESS = "has_full_access"
private const val KEY_FULL_ACCESS = "full_access"
private const val KEY_SUBSCRIPTION_ACTIVE = "subscription_active"
private const val KEY_IS_SUBSCRIBED = "is_subscribed"
private const val KEY_GOOGLE_SUBSCRIPTION_VERIFIED = "google_subscription_verified"
private const val KEY_SUB_PRODUCT = "sub_product"
private const val KEY_SUB_TOKEN = "sub_token"
private const val KEY_SUB_PURCHASE_TIME = "sub_purchase_time"
private const val KEY_SUB_ACCESS_UNTIL = "sub_access_until"
private const val KEY_ACCESS_CHANGED_AT = "access_changed_at"

// ⬅️ מנהל אפליקציה
private const val KEY_IS_ADMIN = "is_admin"

// ⬅️ דגל פתיחה עם קוד סודי (למפתחים / נסיינים)
private const val KEY_DEV_UNLOCK = "dev_unlock"
private const val DEV_UNLOCK_CODE = "34567@"

// 🔒 אם רוצים שהקוד הסודי יפתח הכול — חייב להיות false
private const val FORCE_SUBSCRIPTION_LOCK = false

// בזמן בדיקות מנויים: אדמין לא עוקף מנוי.
// כך אפשר לבדוק שהמנעולים חוזרים אחרי שהמנוי החודשי/שנתי פג.
private const val ADMIN_BYPASSES_SUBSCRIPTION = false

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
        sp.edit()
            .putBoolean(KEY_HAS_FULL_ACCESS, value)
            .putBoolean(KEY_FULL_ACCESS, value)
            .putBoolean(KEY_SUBSCRIPTION_ACTIVE, value)
            .putBoolean(KEY_IS_SUBSCRIBED, value)
            .putLong(KEY_ACCESS_CHANGED_AT, System.currentTimeMillis())
            .apply()
    }

    fun clearAllAccessFlags(sp: SharedPreferences) {
        sp.edit()
            .putBoolean(KEY_IS_ADMIN, false)
            .putBoolean(KEY_DEV_UNLOCK, false)
            .putBoolean(KEY_HAS_FULL_ACCESS, false)
            .putBoolean(KEY_FULL_ACCESS, false)
            .putBoolean(KEY_SUBSCRIPTION_ACTIVE, false)
            .putBoolean(KEY_IS_SUBSCRIBED, false)
            .putBoolean(KEY_GOOGLE_SUBSCRIPTION_VERIFIED, false)
            .remove(KEY_SUB_PRODUCT)
            .remove(KEY_SUB_TOKEN)
            .remove(KEY_SUB_PURCHASE_TIME)
            .remove(KEY_SUB_ACCESS_UNTIL)
            .putLong(KEY_ACCESS_CHANGED_AT, System.currentTimeMillis())
            .apply()
    }

    private fun hasSubscriptionFlags(sp: SharedPreferences): Boolean {
        return sp.getBoolean(KEY_GOOGLE_SUBSCRIPTION_VERIFIED, false) ||
                sp.getBoolean(KEY_HAS_FULL_ACCESS, false) ||
                sp.getBoolean(KEY_FULL_ACCESS, false) ||
                sp.getBoolean(KEY_SUBSCRIPTION_ACTIVE, false) ||
                sp.getBoolean(KEY_IS_SUBSCRIBED, false) ||
                sp.getString(KEY_SUB_PRODUCT, "").orEmpty().isNotBlank()
    }

    private fun clearExpiredSubscriptionFlags(sp: SharedPreferences, until: Long, now: Long) {
        sp.edit()
            .putBoolean(KEY_GOOGLE_SUBSCRIPTION_VERIFIED, false)
            .putBoolean(KEY_HAS_FULL_ACCESS, false)
            .putBoolean(KEY_FULL_ACCESS, false)
            .putBoolean(KEY_SUBSCRIPTION_ACTIVE, false)
            .putBoolean(KEY_IS_SUBSCRIBED, false)
            .remove(KEY_SUB_PRODUCT)
            .remove(KEY_SUB_TOKEN)
            .remove(KEY_SUB_PURCHASE_TIME)
            .remove(KEY_SUB_ACCESS_UNTIL)
            .putLong(KEY_ACCESS_CHANGED_AT, now)
            .apply()

        android.util.Log.e(
            "KMI_ACCESS",
            "clearExpiredSubscriptionFlags until=$until now=$now"
        )
    }

    fun hasValidTimedSubscription(sp: SharedPreferences): Boolean {
        val now = System.currentTimeMillis()
        val until = sp.getLong(KEY_SUB_ACCESS_UNTIL, 0L)
        val hasFlags = hasSubscriptionFlags(sp)

        val active = hasFlags && until > now

        if (!active && hasFlags && until > 0L && until <= now) {
            clearExpiredSubscriptionFlags(sp, until, now)
        }

        android.util.Log.e(
            "KMI_ACCESS",
            "hasValidTimedSubscription hasFlags=$hasFlags until=$until now=$now active=$active"
        )

        return active
    }
    /**
     * גישה מלאה:
     * בזמן בדיקות — אדמין לא פותח אוטומטית.
     * מנוי רגיל / בדיקות נפתח רק אם sub_access_until עדיין בתוקף.
     */
    fun hasFullAccess(sp: SharedPreferences): Boolean {
        val admin = isAdmin(sp)
        val dev = hasDevUnlock(sp)
        val timedSubscription = hasValidTimedSubscription(sp)

        val adminAccess =
            ADMIN_BYPASSES_SUBSCRIPTION && admin

        if (FORCE_SUBSCRIPTION_LOCK && !adminAccess && !dev) {
            android.util.Log.e(
                "KMI_ACCESS",
                "hasFullAccess FORCE_SUBSCRIPTION_LOCK=true and no admin/dev unlock -> result=false"
            )
            return false
        }

        val result =
            adminAccess ||
                    dev ||
                    timedSubscription

        android.util.Log.e(
            "KMI_ACCESS",
            "hasFullAccess admin=$admin adminBypass=$ADMIN_BYPASSES_SUBSCRIPTION " +
                    "adminAccess=$adminAccess devUnlock=$dev " +
                    "timedSubscription=$timedSubscription result=$result"
        )

        return result
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
