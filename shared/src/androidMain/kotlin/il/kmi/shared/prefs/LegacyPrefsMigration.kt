package il.kmi.shared.prefs

import android.content.Context
import android.content.Context.MODE_PRIVATE

object LegacyPrefsMigration {
    /**
     * מעתיק ערכים ישנים מ-SharedPreferences ל-KMP Settings (ללא מחיקה),
     * רץ פעם אחת בתחילת האפליקציה.
     */
    fun run(context: Context) {
        // kmi_settings – תצוגה
        val settingsSp = context.getSharedPreferences("kmi_settings", MODE_PRIVATE)

        // אל תבצע מיגרציה יותר מפעם אחת
        if (settingsSp.getBoolean("migrated_to_kmp_v1", false)) return

        val uirepo = UserPrefsRepositoryAndroid(context)
        settingsSp.getString("font_size", null)?.let { uirepo.setFontSize(it) }
        settingsSp.getString("theme_mode", null)?.let { uirepo.setThemeMode(it) }
        if (settingsSp.contains("font_scale")) {
            val fs = settingsSp.getFloat("font_scale", 1.0f)
            uirepo.setFontScale(fs.toDouble())
        }

        // kmi_user – פרטי משתמש
        val userSp = context.getSharedPreferences("kmi_user", MODE_PRIVATE)
        val kmi = KmiPrefsFactory.create(context)
        userSp.getString("fullName", null)?.let { kmi.fullName = it }
        userSp.getString("phone", null)?.let { kmi.phone = it }
        userSp.getString("email", null)?.let { kmi.email = it }
        userSp.getString("region", null)?.let { kmi.region = it }
        userSp.getString("branch", null)?.let { kmi.branch = it }
        userSp.getString("username", null)?.let { kmi.username = it }
        userSp.getString("password", null)?.let { kmi.password = it }
        userSp.getString("branchId", null)?.let { kmi.branchId = it }
        userSp.getString("age_group", null)?.let { kmi.ageGroup = it }  // 👈 חדש

        // 👇 הרחבות מיגרציה: גם מה־settingsSP ל־KMP (מקור אמת חוצה פלטפורמות)
        settingsSp.getString("theme_mode", null)?.let { kmi.themeMode = it }
        settingsSp.getString("font_size", null)?.let { kmi.fontSize = it }
        if (settingsSp.contains("font_scale")) {
            kmi.fontScaleString = settingsSp.getFloat("font_scale", 1.0f)
                .coerceIn(0.80f, 1.40f)
                .toString()
        }

        // צליל/רטט/יומן/תזכורות
        kmi.clickSounds  = settingsSp.getBoolean("click_sounds", true)
        kmi.hapticsOn    = settingsSp.getBoolean("haptics_on", true)
        kmi.syncCalendar = settingsSp.getBoolean("sync_calendar", false)
        kmi.remindersOn  = settingsSp.getBoolean("reminders_on", true)
        kmi.leadMinutes  = settingsSp.getInt("lead_minutes", 60)

        // תמיכה גם במפתח הישן branch_id (אם לא הוגדר branchId חדש)
        if (kmi.branchId == null) {
            userSp.getString("branch_id", null)?.let { kmi.branchId = it }
        }

        // סמן שסיימנו מיגרציה כדי לא לרוץ שוב
        settingsSp.edit().putBoolean("migrated_to_kmp_v1", true).apply()
    }
}
