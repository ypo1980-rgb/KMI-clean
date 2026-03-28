package il.kmi.app.halacha

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object ShabbatHolidayChecker {

    private const val TZ_ID = "Asia/Jerusalem"

    // אפשר בעתיד להחליף לעיר/סניף של המשתמש
    private val timeZone: TimeZone = TimeZone.getTimeZone(TZ_ID)

    private val jerusalemGeoLocation = GeoLocation(
        "Jerusalem, Israel",
        31.778,
        35.235,
        0.0,
        timeZone
    )

    private data class DayCache(
        val key: String,
        val jewishCalendar: JewishCalendar,
        val candleLighting: Date?,
        val tzais: Date?
    )

    @Volatile
    private var cachedDay: DayCache? = null

    data class BlockStatus(
        val blocked: Boolean,
        val reason: String?,
        val candleLighting: Date?,
        val tzais: Date?
    )

    /**
     * האם עכשיו אסור לשלוח התראה:
     * - משעת כניסת שבת/חג ועד צאת
     * - בכל שבת/חג עד צאת
     */
    fun getBlockStatus(nowMillis: Long = System.currentTimeMillis()): BlockStatus {
        val dayData = getOrBuildDayCache(nowMillis)

        val now = Date(nowMillis)
        val jewishCalendar = dayData.jewishCalendar
        val candleLighting = dayData.candleLighting
        val tzais = dayData.tzais

        // שבת/חג עצמם – חסום עד צאת
        if (jewishCalendar.isAssurBemelacha()) {
            val blocked = tzais?.let { now.before(it) } ?: true
            return BlockStatus(
                blocked = blocked,
                reason = if (blocked) "shabbat_or_yom_tov" else null,
                candleLighting = candleLighting,
                tzais = tzais
            )
        }

        // ערב שבת / ערב חג – חסום מרגע הדלקת נרות
        if (candleLighting != null && !now.before(candleLighting)) {
            return BlockStatus(
                blocked = true,
                reason = "erev_shabbat_or_erev_yom_tov_after_candle_lighting",
                candleLighting = candleLighting,
                tzais = tzais
            )
        }

        return BlockStatus(
            blocked = false,
            reason = null,
            candleLighting = candleLighting,
            tzais = tzais
        )
    }

    fun isBlockedNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return getBlockStatus(nowMillis).blocked
    }

    /**
     * מחזיר זמן תזכורת ליום נתון:
     * - יום רגיל -> השעה שנבחרה
     * - ערב שבת / ערב חג -> ברירת מחדל: 60 דקות לפני הדלקת נרות
     */
    fun adjustReminderTimeForDate(
        baseDateMillis: Long,
        preferredHour: Int,
        preferredMinute: Int,
        erevOffsetMinutes: Int = 60
    ): Long {
        val baseCalendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = baseDateMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayData = getOrBuildDayCache(baseCalendar.timeInMillis)
        val candleLighting = dayData.candleLighting

        return if (candleLighting != null) {
            Calendar.getInstance(timeZone).apply {
                time = candleLighting
                add(Calendar.MINUTE, -erevOffsetMinutes)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else {
            Calendar.getInstance(timeZone).apply {
                timeInMillis = baseCalendar.timeInMillis
                set(Calendar.HOUR_OF_DAY, preferredHour)
                set(Calendar.MINUTE, preferredMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    /**
     * מחפש את זמן ההתראה החוקי הבא:
     * - מדלג על שבת/חג
     * - בערב שבת/חג מזיז לזמן מוקדם לפני כניסה
     * - עובד לפי Asia/Jerusalem, כולל שעון קיץ/חורף
     */
    fun computeNextAllowedTriggerTimeMillis(
        preferredHour: Int,
        preferredMinute: Int,
        nowMillis: Long = System.currentTimeMillis(),
        erevOffsetMinutes: Int = 60
    ): Long {
        val now = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
        }

        val candidateDay = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, preferredHour)
            set(Calendar.MINUTE, preferredMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (candidateDay.timeInMillis <= now.timeInMillis) {
            candidateDay.add(Calendar.DAY_OF_YEAR, 1)
        }

        repeat(30) {
            val adjustedCandidate = adjustReminderTimeForDate(
                baseDateMillis = candidateDay.timeInMillis,
                preferredHour = preferredHour,
                preferredMinute = preferredMinute,
                erevOffsetMinutes = erevOffsetMinutes
            )

            val status = getBlockStatus(adjustedCandidate)

            if (!status.blocked && adjustedCandidate > nowMillis) {
                return adjustedCandidate
            }

            candidateDay.add(Calendar.DAY_OF_YEAR, 1)
            candidateDay.set(Calendar.HOUR_OF_DAY, preferredHour)
            candidateDay.set(Calendar.MINUTE, preferredMinute)
            candidateDay.set(Calendar.SECOND, 0)
            candidateDay.set(Calendar.MILLISECOND, 0)
        }

        return candidateDay.timeInMillis
    }

    private fun buildJewishCalendar(timeMillis: Long): JewishCalendar {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = timeMillis
        }

        return JewishCalendar().apply {
            setGregorianDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            setInIsrael(true)
            setUseModernHolidays(true)
        }
    }

    private fun buildZmanimCalendar(timeMillis: Long): ComplexZmanimCalendar {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = timeMillis
        }

        return ComplexZmanimCalendar(jerusalemGeoLocation).apply {
            calendar = cal
            candleLightingOffset = 18.0
        }
    }

    private fun getOrBuildDayCache(timeMillis: Long): DayCache {
        val key = dayKey(timeMillis)

        val existing = cachedDay
        if (existing != null && existing.key == key) {
            return existing
        }

        val jewishCalendar = buildJewishCalendar(timeMillis)
        val zmanimCalendar = buildZmanimCalendar(timeMillis)

        val fresh = DayCache(
            key = key,
            jewishCalendar = jewishCalendar,
            candleLighting = if (jewishCalendar.hasCandleLighting()) {
                zmanimCalendar.getCandleLighting()
            } else {
                null
            },
            tzais = zmanimCalendar.getTzais()
        )

        cachedDay = fresh
        return fresh
    }

    private fun dayKey(timeMillis: Long): String {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = timeMillis
        }
        return buildString {
            append(cal.get(Calendar.YEAR))
            append('-')
            append(cal.get(Calendar.MONTH))
            append('-')
            append(cal.get(Calendar.DAY_OF_MONTH))
        }
    }
}