package il.kmi.shared

import com.russhwolf.settings.Settings

// מפעל חוצה־פלטפורמות שמחזיר אובייקט Settings לפי שם ה־store
expect object KmiSettingsFactory {
    fun of(name: String, context: Any? = null): Settings
}


@Suppress("MemberVisibilityCanBePrivate")
class KmiSettings(
    private val settings: Settings
) {
    var fullName: String?
        get() = settings.getStringOrNullCompat("fullName")
        set(value) { if (value == null) settings.remove("fullName") else settings.putStringCompat("fullName", value) }

    var phone: String?
        get() = settings.getStringOrNullCompat("phone")
        set(value) { if (value == null) settings.remove("phone") else settings.putStringCompat("phone", value) }

    var email: String?
        get() = settings.getStringOrNullCompat("email")
        set(value) { if (value == null) settings.remove("email") else settings.putStringCompat("email", value) }

    var region: String?
        get() = settings.getStringOrNullCompat("region")
        set(value) { if (value == null) settings.remove("region") else settings.putStringCompat("region", value) }

    var branch: String?
        get() = settings.getStringOrNullCompat("branch")
        set(value) { if (value == null) settings.remove("branch") else settings.putStringCompat("branch", value) }

    var launchCount: Int
        get() = settings.getIntOrNullCompat("launchCount") ?: 0
        set(value) { settings.putIntCompat("launchCount", value) }

    fun incLaunchCount(): Int {
        val next = launchCount + 1
        launchCount = next
        return next
    }

    fun clearAll() = settings.clear()
}

/* --- Helpers (compat) לשימוש ב-API הבסיסי של Settings --- */
private fun Settings.getStringOrNullCompat(key: String): String? =
    if (hasKey(key)) getString(key, "") else null

private fun Settings.putStringCompat(key: String, value: String) {
    putString(key, value)
}

private fun Settings.getIntOrNullCompat(key: String): Int? =
    if (hasKey(key)) getInt(key, 0) else null

private fun Settings.putIntCompat(key: String, value: Int) {
    putInt(key, value)
}
