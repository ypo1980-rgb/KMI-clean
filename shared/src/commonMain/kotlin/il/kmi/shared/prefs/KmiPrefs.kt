package il.kmi.shared.prefs

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * עטיפה חוצה־פלטפורמות מעל Multiplatform Settings.
 * הקובץ יושב ב-commonMain ולכן עובד גם באנדרואיד וגם ב-iOS.
 *
 * שים לב: הודות לתלות no-arg, ניתן ליצור ברירת מחדל Settings()
 * גם ב-commonMain ללא קוד פלטפורמה.
 */
class KmiPrefs(
    private val settings: Settings
) {
    // ----- Keys -----
    private object Keys {
        // User
        const val FULL_NAME  = "user.fullName"
        const val PHONE      = "user.phone"
        const val EMAIL      = "user.email"
        const val REGION     = "user.region"
        const val BRANCH     = "user.branch"
        const val AGE_GROUP  = "user.ageGroup"
        const val USERNAME   = "user.username"
        const val PASSWORD   = "user.password"
        const val BRANCH_ID  = "user.branchId"

        // App prefs (UX/Theme)
        const val THEME_MODE = "ui.themeMode"        // "system" | "light" | "dark"
        const val FONT_SIZE  = "ui.fontSize"         // "small" | "medium" | "large"
        const val FONT_SCALE = "ui.fontScale"        // Float (e.g. 1.0)

        const val CLICK_SOUNDS = "ui.clickSounds"    // Boolean
        const val HAPTICS_ON   = "ui.hapticsOn"      // Boolean

        // Reminders / Calendar
        const val REMINDERS_ON = "reminders.on"      // Boolean
        const val LEAD_MIN     = "reminders.leadMinutes" // Int (e.g. 60)
        const val SYNC_CAL     = "calendar.sync"     // Boolean

        // Misc
        const val OPEN_COUNT   = "meta.openCount"    // Int
    }

    // ----- User -----
    var fullName: String?
        get() = settings.getStringOrNull(Keys.FULL_NAME)
        set(v) { if (v == null) settings.remove(Keys.FULL_NAME) else settings[Keys.FULL_NAME] = v }

    var phone: String?
        get() = settings.getStringOrNull(Keys.PHONE)
        set(v) { if (v == null) settings.remove(Keys.PHONE) else settings[Keys.PHONE] = v }

    var email: String?
        get() = settings.getStringOrNull(Keys.EMAIL)
        set(v) { if (v == null) settings.remove(Keys.EMAIL) else settings[Keys.EMAIL] = v }

    var region: String?
        get() = settings.getStringOrNull(Keys.REGION)
        set(v) { if (v == null) settings.remove(Keys.REGION) else settings[Keys.REGION] = v }

    var branch: String?
        get() = settings.getStringOrNull(Keys.BRANCH)
        set(v) { if (v == null) settings.remove(Keys.BRANCH) else settings[Keys.BRANCH] = v }

    var ageGroup: String?
        get() = settings.getStringOrNull(Keys.AGE_GROUP)
        set(v) { if (v == null) settings.remove(Keys.AGE_GROUP) else settings[Keys.AGE_GROUP] = v }

    var username: String?
        get() = settings.getStringOrNull(Keys.USERNAME)
        set(v) { if (v == null) settings.remove(Keys.USERNAME) else settings[Keys.USERNAME] = v }

    var password: String?
        get() = settings.getStringOrNull(Keys.PASSWORD)
        set(v) { if (v == null) settings.remove(Keys.PASSWORD) else settings[Keys.PASSWORD] = v }

    var branchId: String?
        get() = settings.getStringOrNull(Keys.BRANCH_ID)
        set(v) { if (v == null) settings.remove(Keys.BRANCH_ID) else settings[Keys.BRANCH_ID] = v }

    // ----- UI / Theme -----
    var themeMode: String
        get() = settings.getString(Keys.THEME_MODE, "system")
        set(v) { settings[Keys.THEME_MODE] = v }

    var fontSize: String
        get() = settings.getString(Keys.FONT_SIZE, "medium")
        set(v) { settings[Keys.FONT_SIZE] = v }

    /**
     * נשמר כמחרוזת כדי להימנע מהבדלי טיפוס בין פלטפורמות.
     * מומלץ להמיר ל-Float בקריאה: .toFloatOrNull() ?: 1.0f
     *
     * ⚠️ לשמירת תאימות – המימוש כאן עוטף את fontScale (Float) כך שלא משנה איך קראת בעבר,
     * תמיד תקבל/תעדכן את אותו הערך.
     */
    var fontScaleString: String
        get() = fontScale.toString()
        set(v) { fontScale = v.toFloatOrNull() ?: 1.0f }

    // 👇 חדש: ערך רציף של קנה־מידה לגופנים כ-Float (חוצה־פלטפורמות)
    var fontScale: Float
        get() = settings.getFloat(Keys.FONT_SCALE, 1.0f)
        set(v) { settings[Keys.FONT_SCALE] = v.coerceIn(0.80f, 1.40f) }

    var clickSounds: Boolean
        get() = settings.getBoolean(Keys.CLICK_SOUNDS, true)
        set(v) { settings[Keys.CLICK_SOUNDS] = v }

    var hapticsOn: Boolean
        get() = settings.getBoolean(Keys.HAPTICS_ON, true)
        set(v) { settings[Keys.HAPTICS_ON] = v }

    // ----- Reminders / Calendar -----
    var remindersOn: Boolean
        get() = settings.getBoolean(Keys.REMINDERS_ON, true)
        set(v) { settings[Keys.REMINDERS_ON] = v }

    var leadMinutes: Int
        get() = settings.getInt(Keys.LEAD_MIN, 60)
        set(v) { settings[Keys.LEAD_MIN] = v }

    var syncCalendar: Boolean
        get() = settings.getBoolean(Keys.SYNC_CAL, false)
        set(v) { settings[Keys.SYNC_CAL] = v }

    // ----- Misc -----
    var openCount: Int
        get() = settings.getInt(Keys.OPEN_COUNT, 0)
        set(v) { settings[Keys.OPEN_COUNT] = v }

    fun incrementOpenCount(): Int {
        val next = openCount + 1
        openCount = next
        return next
    }

    fun clearAll() = settings.clear()
}
