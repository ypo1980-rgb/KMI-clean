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
    val coach: String,
    val branch: String = "",
    val group: String = ""
) {
    // ✅ חותמת זמן תחילת האימון
    val startMillis: Long
        get() = cal.timeInMillis

    /*
 * מזהה פנימי יציב למופע אימון מסוים.
 *
 * branch + group + startMillis מבדילים בין קבוצות
 * וסניפים שמקיימים אימון באותה שעה.
 *
 * המפתח מיועד להשוואה ומטמון מקומי. במסמך Firestore
 * נשמור גם את שלושת השדות בנפרד.
 */
    val occurrenceKey: String
        get() {
            val resolvedBranch =
                branch
                    .ifBlank { place }
                    .trim()
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace(Regex("\\s+"), " ")
                    .lowercase(Locale("he", "IL"))

            val resolvedGroup =
                group
                    .trim()
                    .replace("–", "-")
                    .replace("—", "-")
                    .replace(Regex("\\s+"), " ")
                    .lowercase(Locale("he", "IL"))

            return listOf(
                resolvedBranch,
                resolvedGroup,
                startMillis.toString()
            ).joinToString("|")
        }

    /*
     * חותמת זמן סיום האימון המחושבת מהשדה end בפורמט HH:mm.
     *
     * אם שעת הסיום מוקדמת משעת ההתחלה או שווה לה,
     * האימון נחשב כאימון שמסתיים ביום הבא.
     *
     * במקרה של ערך לא תקין מוחזר null,
     * כדי שהמנוע יוכל להשתמש בהתנהגות תאימות בטוחה.
     */
    val endMillis: Long?
        get() {
            val parts = end
                .trim()
                .split(":")

            if (parts.size != 2) {
                return null
            }

            val hour =
                parts[0].trim().toIntOrNull()
                    ?: return null

            val minute =
                parts[1].trim().toIntOrNull()
                    ?: return null

            if (
                hour !in 0..23 ||
                minute !in 0..59
            ) {
                return null
            }

            val endCalendar =
                (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

            /*
             * תמיכה באימון שחוצה את חצות.
             */
            if (
                endCalendar.timeInMillis <=
                startMillis
            ) {
                endCalendar.add(
                    Calendar.DAY_OF_YEAR,
                    1
                )
            }

            return endCalendar.timeInMillis
        }

    /**
     * האם האימון כבר הסתיים.
     *
     * כאשר שעת הסיום אינה תקינה, נשמרת תאימות להתנהגות
     * הישנה והבדיקה מתבצעת לפי שעת ההתחלה.
     */
    fun isPast(
        now: Calendar = Calendar.getInstance(),
        graceMinutes: Int = 0
    ): Boolean {
        val cutoff =
            (now.clone() as Calendar).apply {
                if (graceMinutes != 0) {
                    add(
                        Calendar.MINUTE,
                        -graceMinutes
                    )
                }
            }

        val effectiveEndMillis =
            endMillis ?: startMillis

        return effectiveEndMillis <
                cutoff.timeInMillis
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
            branch: String = "",
            now: Calendar = Calendar.getInstance(),
            group: String = ""
        ): TrainingData {
            val startCal = (now.clone() as Calendar).apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
            }

            val endCal = (startCal.clone() as Calendar).apply {
                add(
                    Calendar.MINUTE,
                    durationMinutes.coerceAtLeast(1)
                )
            }

            /*
             * מדלגים לשבוע הבא רק לאחר שהאימון הסתיים.
             * אם האימון כבר התחיל אך עדיין מתקיים,
             * נשארים במופע של השבוע הנוכחי.
             */
            if (endCal.timeInMillis <= now.timeInMillis) {
                startCal.add(Calendar.WEEK_OF_YEAR, 1)
                endCal.add(Calendar.WEEK_OF_YEAR, 1)
            }

            val startStr = "${dateFmt.format(startCal.time)} ${timeFmt.format(startCal.time)}"
            val endStr   = timeFmt.format(endCal.time)

            return TrainingData(
                cal = startCal,
                start = startStr,
                end = endStr,
                place = place,
                address = address,
                coach = coach,
                branch = branch,
                group = group
            )
        }
    }
}
