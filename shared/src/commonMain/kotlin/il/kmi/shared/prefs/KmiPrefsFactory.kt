package il.kmi.shared.prefs

/**
 * מפעל חוצה־פלטפורמות ליצירת KmiPrefs.
 * ב־Android מקבלים Context, ב־iOS מתעלמים ממנו.
 */
expect object KmiPrefsFactory {
    fun create(context: Any): KmiPrefs
}
