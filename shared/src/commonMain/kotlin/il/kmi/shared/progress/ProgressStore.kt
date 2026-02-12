package il.kmi.shared.progress

import com.russhwolf.settings.Settings

class ProgressStore(
    private val settings: Settings,
    private val keyPrefix: String = "mastered"
) {

    /** מחשב אחוז התקדמות לפי מפתח חגורה ומספר פריטים כולל */
    fun percent(beltKey: String, total: Int): Int {
        if (total <= 0) return 0
        val done = doneCount(beltKey)
        return ((done * 100f) / total).toInt()
    }

    /** יוצר תמונת מצב (אחוזים) לכל חגורה על בסיס המפה של ה-Totals */
    fun snapshot(totals: Map<String, Int>): Map<String, Int> =
        totals.mapValues { (belt, total) -> percent(beltKey = belt, total = total) }

    /** ספירת פריטים שסומנו כ"מסיימים" לחגורה.
     *  הערה: כרגע נדרש מפתח מונה ייעודי: `${keyPrefix}|${beltKey}|__count` */
    private fun doneCount(beltKey: String): Int {
        val counterKey = "$keyPrefix|$beltKey|__count"
        return settings.getInt(counterKey, 0)
    }

    companion object {
        /** Factory נוח שמקבל שם Store והקשר פלטפורמה */
        fun fromContext(
            name: String,
            context: Any,
            keyPrefix: String = "mastered"
        ): ProgressStore {
            val s: Settings = il.kmi.shared.KmiSettingsFactory.of(name, context)
            return ProgressStore(settings = s, keyPrefix = keyPrefix)
        }
    }
}
