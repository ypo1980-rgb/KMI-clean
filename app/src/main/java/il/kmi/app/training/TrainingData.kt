package il.kmi.app.training

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class TrainingData(
    val cal: Calendar,
    val start: String,
    val end: String,
    val place: String,
    val address: String,
    val coach: String            // ✅ מאמן
) {
    // ✅ עוזר: חותמות זמנים לשימוש פנימי/סינון
    val startMillis: Long get() = cal.timeInMillis

    /** האם האימון כבר חלף (עם בופר אופציונלי של דקות לאחור, ברירת מחדל 0) */
    fun isPast(now: Calendar = Calendar.getInstance(), graceMinutes: Int = 0): Boolean {
        val cutoff = now.clone() as Calendar
        if (graceMinutes != 0) cutoff.add(Calendar.MINUTE, -graceMinutes)
        return startMillis < cutoff.timeInMillis
    }

    companion object {
        private val heLocale = Locale("he", "IL")
        @Suppress("SimpleDateFormat")
        private val timeFmt = SimpleDateFormat("HH:mm", heLocale)
        @Suppress("SimpleDateFormat")
        private val dateFmt = SimpleDateFormat("dd/MM/yyyy", heLocale)

        /**
         * ✅ יוצר אימון “שבועי הבא” לפי יום בשבוע + שעה/דקה + משך.
         * אם מועד השבוע הנוכחי כבר עבר – ידלג לשבוע הבא.
         *
         * @param dayOfWeek  Calendar.SUNDAY .. Calendar.SATURDAY
         * @param startHour  0..23
         * @param startMinute 0..59
         * @param durationMinutes משך בדקות (למשל 90)
         * @param place / address / coach  – תיאור הלוקיישן והמאמן להצגה
         * @param now  זמן ייחוס (ברירת מחדל עכשיו)
         */
        fun nextWeekly(
            dayOfWeek: Int,
            startHour: Int,
            startMinute: Int,
            durationMinutes: Int,
            place: String,
            address: String,
            coach: String,
            now: Calendar = Calendar.getInstance()
        ): TrainingData {
            val startCal = (now.clone() as Calendar).apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // הצמד ליום הרצוי בשבוע
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)

                // אם כבר עבר – דלג לשבוע הבא
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val endCal = (startCal.clone() as Calendar).apply {
                add(Calendar.MINUTE, durationMinutes)
            }

            val startStr = "${dateFmt.format(startCal.time)} ${timeFmt.format(startCal.time)}"
            val endStr   = timeFmt.format(endCal.time)

            return TrainingData(
                cal = startCal,
                start = startStr,
                end = endStr,
                place = place,
                address = address,
                coach = coach
            )
        }
    }
}
