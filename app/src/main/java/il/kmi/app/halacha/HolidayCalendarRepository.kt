package il.kmi.app.halacha

import android.content.Context
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * מקור האמת המרכזי לכל נתוני החגים באפליקציה.
 *
 * מספק:
 * - שמות חגים להצגה.
 * - תמיכה בתאריך יחיד ובטווח תאריכים.
 * - החלטה אם חג מבטל אימון.
 * - סיבת הביטול בעברית ובאנגלית.
 */
object HolidayCalendarRepository {

    private const val ASSET_FILE =
        "holidays_hebrew_2024_2026.json"

    private const val ASSET_FIRST_YEAR = 2024
    private const val ASSET_LAST_YEAR = 2026

    data class CancellationReason(
        val he: String,
        val en: String
    )

    data class HolidayInfo(
        val date: LocalDate,
        val nameHe: String,
        val nameEn: String,
        val cancellationReason: CancellationReason?
    ) {
        val cancelsTraining: Boolean
            get() = cancellationReason != null

        fun displayName(isEnglish: Boolean): String {
            return if (isEnglish) {
                nameEn.ifBlank { nameHe }
            } else {
                nameHe.ifBlank { nameEn }
            }
        }
    }

    @Volatile
    private var cachedByDate:
            Map<LocalDate, List<HolidayInfo>>? = null

    /*
     * חגים המחושבים עבור שנים שאינן נמצאות בקובץ ה־JSON.
     * המטמון מונע חישוב חוזר בכל פתיחת לוח השנה.
     */
    private val calculatedMonths =
        ConcurrentHashMap<
                YearMonth,
                Map<LocalDate, List<HolidayInfo>>
                >()

    fun holidaysOn(
        context: Context,
        date: LocalDate
    ): List<HolidayInfo> {
        return holidaysForMonth(
            context = context,
            yearMonth = YearMonth.from(date)
        )[date].orEmpty()
    }

    fun holidaysForMonth(
        context: Context,
        yearMonth: YearMonth
    ): Map<LocalDate, List<HolidayInfo>> {
        /*
         * בשנים המכוסות על ידי קובץ הנתונים,
         * ה־JSON נשאר מקור האמת.
         */
        if (
            yearMonth.year in
            ASSET_FIRST_YEAR..ASSET_LAST_YEAR
        ) {
            return allHolidays(context)
                .filterKeys { date ->
                    YearMonth.from(date) == yearMonth
                }
        }

        /*
         * בשנים אחרות החגים מחושבים אוטומטית.
         */
        return calculatedMonths.getOrPut(yearMonth) {
            calculateMonth(yearMonth)
        }
    }

    fun displayNamesForMonth(
        context: Context,
        yearMonth: YearMonth,
        isEnglish: Boolean
    ): Map<LocalDate, String> {
        return holidaysForMonth(
            context = context,
            yearMonth = yearMonth
        ).mapValues { (_, holidays) ->
            holidays
                .map { it.displayName(isEnglish) }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" · ")
        }
    }

    fun cancellationReason(
        context: Context,
        date: LocalDate
    ): CancellationReason? {
        val cancellationReasons = holidaysOn(
            context = context,
            date = date
        )
            .mapNotNull { it.cancellationReason }

        /*
         * אם יש כמה אירועים באותו יום,
         * תשעה באב מקבל עדיפות על שם כללי.
         */
        return cancellationReasons.firstOrNull {
            it.he == "צום תשעה באב"
        } ?: cancellationReasons.firstOrNull()
    }

    fun isTrainingCancelled(
        context: Context,
        date: LocalDate
    ): Boolean {
        return cancellationReason(
            context = context,
            date = date
        ) != null
    }

    private fun calculateMonth(
        yearMonth: YearMonth
    ): Map<LocalDate, List<HolidayInfo>> {
        val hebrewFormatter =
            HebrewDateFormatter().apply {
                setHebrewFormat(true)
            }

        val englishFormatter =
            HebrewDateFormatter().apply {
                setHebrewFormat(false)
            }

        val result =
            linkedMapOf<
                    LocalDate,
                    MutableList<HolidayInfo>
                    >()

        for (
        dayOfMonth in
        1..yearMonth.lengthOfMonth()
        ) {
            val date =
                yearMonth.atDay(dayOfMonth)

            val jewishCalendar =
                JewishCalendar().apply {
                    setGregorianDate(
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    )
                    setInIsrael(true)
                    setUseModernHolidays(true)
                }

            val holidayIndex =
                jewishCalendar.yomTovIndex

            if (holidayIndex < 0) {
                continue
            }

            val formattedNameHe =
                hebrewFormatter
                    .formatYomTov(jewishCalendar)
                    .trim()

            val formattedNameEn =
                englishFormatter
                    .formatYomTov(jewishCalendar)
                    .trim()

            if (
                formattedNameHe.isBlank() &&
                formattedNameEn.isBlank()
            ) {
                continue
            }

            val cancellationReason =
                calculatedCancellationReason(
                    holidayIndex = holidayIndex
                )

            /*
             * בישראל שמיני עצרת ושמחת תורה
             * חלים באותו היום.
             */
            val nameHe =
                if (
                    holidayIndex ==
                    JewishCalendar.SHEMINI_ATZERES
                ) {
                    "שמחת תורה / שמיני עצרת"
                } else {
                    formattedNameHe
                }

            val nameEn =
                if (
                    holidayIndex ==
                    JewishCalendar.SHEMINI_ATZERES
                ) {
                    "Simchat Torah / Shemini Atzeret"
                } else {
                    formattedNameEn
                }

            result.getOrPut(date) {
                mutableListOf()
            }.add(
                HolidayInfo(
                    date = date,
                    nameHe = nameHe,
                    nameEn = nameEn,
                    cancellationReason =
                        cancellationReason
                )
            )
        }

        return result.mapValues { (_, holidays) ->
            holidays.distinctBy { holiday ->
                listOf(
                    holiday.nameHe,
                    holiday.nameEn,
                    holiday.cancellationReason?.he,
                    holiday.cancellationReason?.en
                )
            }
        }
    }

    private fun calculatedCancellationReason(
        holidayIndex: Int
    ): CancellationReason? {
        return when (holidayIndex) {
            JewishCalendar.TISHA_BEAV ->
                CancellationReason(
                    he = "צום תשעה באב",
                    en = "Tisha B’Av fast"
                )

            JewishCalendar.EREV_ROSH_HASHANA ->
                CancellationReason(
                    he = "ערב ראש השנה",
                    en = "Rosh Hashanah Eve"
                )

            JewishCalendar.ROSH_HASHANA ->
                CancellationReason(
                    he = "ראש השנה",
                    en = "Rosh Hashanah"
                )

            JewishCalendar.EREV_YOM_KIPPUR ->
                CancellationReason(
                    he = "ערב יום כיפור",
                    en = "Yom Kippur Eve"
                )

            JewishCalendar.YOM_KIPPUR ->
                CancellationReason(
                    he = "יום כיפור",
                    en = "Yom Kippur"
                )

            JewishCalendar.EREV_SUCCOS ->
                CancellationReason(
                    he = "ערב סוכות",
                    en = "Sukkot Eve"
                )

            JewishCalendar.SUCCOS ->
                CancellationReason(
                    he = "סוכות",
                    en = "Sukkot"
                )

            JewishCalendar.CHOL_HAMOED_SUCCOS,
            JewishCalendar.HOSHANA_RABBA ->
                CancellationReason(
                    he = "חול המועד סוכות",
                    en = "Sukkot Intermediate Days"
                )

            JewishCalendar.SHEMINI_ATZERES,
            JewishCalendar.SIMCHAS_TORAH ->
                CancellationReason(
                    he = "שמחת תורה",
                    en = "Simchat Torah"
                )

            JewishCalendar.EREV_PESACH ->
                CancellationReason(
                    he = "ערב פסח",
                    en = "Passover Eve"
                )

            JewishCalendar.PESACH ->
                CancellationReason(
                    he = "פסח",
                    en = "Passover"
                )

            JewishCalendar.CHOL_HAMOED_PESACH ->
                CancellationReason(
                    he = "חול המועד פסח",
                    en = "Passover Intermediate Days"
                )

            JewishCalendar.EREV_SHAVUOS ->
                CancellationReason(
                    he = "ערב שבועות",
                    en = "Shavuot Eve"
                )

            JewishCalendar.SHAVUOS ->
                CancellationReason(
                    he = "שבועות",
                    en = "Shavuot"
                )

            else -> null
        }
    }

    private fun allHolidays(
        context: Context
    ): Map<LocalDate, List<HolidayInfo>> {
        cachedByDate?.let { return it }

        return synchronized(this) {
            cachedByDate?.let {
                return@synchronized it
            }

            val loaded = runCatching {
                loadFromAssets(
                    context.applicationContext
                )
            }.getOrElse {
                emptyMap()
            }

            cachedByDate = loaded
            loaded
        }
    }

    private fun loadFromAssets(
        context: Context
    ): Map<LocalDate, List<HolidayInfo>> {
        val json = context.assets
            .open(ASSET_FILE)
            .bufferedReader()
            .use { it.readText() }

        val root = JSONTokener(json).nextValue()

        val items: JSONArray = when (root) {
            is JSONObject -> when {
                root.has("items") ->
                    root.getJSONArray("items")

                root.has("data") ->
                    root.getJSONArray("data")

                else ->
                    JSONArray()
            }

            is JSONArray -> root
            else -> JSONArray()
        }

        val result =
            linkedMapOf<LocalDate, MutableList<HolidayInfo>>()

        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index)
                ?: continue

            val dates = datesFor(item)

            if (dates.isEmpty()) {
                continue
            }

            val nameHe = firstNonBlank(
                item.optString("title_he", ""),
                item.optString("hebrew", ""),
                item.optString("name_he", ""),
                item.optString("name", ""),
                item.optString("title", "")
            )

            val nameEn = firstNonBlank(
                item.optString("title_en", ""),
                item.optString("english", ""),
                item.optString("name_en", ""),
                item.optString("title", ""),
                item.optString("name", "")
            )

            val cancellationReason =
                cancellationReasonFor(item)

            dates.forEach { date ->
                result.getOrPut(date) {
                    mutableListOf()
                }.add(
                    HolidayInfo(
                        date = date,
                        nameHe = nameHe,
                        nameEn = nameEn,
                        cancellationReason =
                            cancellationReason
                    )
                )
            }
        }

        return result.mapValues { (_, holidays) ->
            holidays.distinctBy {
                listOf(
                    it.nameHe,
                    it.nameEn,
                    it.cancellationReason?.he,
                    it.cancellationReason?.en
                )
            }
        }
    }

    private fun datesFor(
        item: JSONObject
    ): List<LocalDate> {
        val startText =
            item.optString("start_iso", "").trim()

        val endText =
            item.optString("end_iso", "").trim()

        if (
            startText.isNotBlank() &&
            endText.isNotBlank()
        ) {
            val start = runCatching {
                LocalDate.parse(startText.take(10))
            }.getOrNull()

            val end = runCatching {
                LocalDate.parse(endText.take(10))
            }.getOrNull()

            if (
                start != null &&
                end != null &&
                !end.isBefore(start)
            ) {
                return generateSequence(start) {
                        current ->
                    current
                        .plusDays(1)
                        .takeIf { !it.isAfter(end) }
                }.toList()
            }
        }

        val dateText =
            item.optString("date_iso", "").trim()

        val singleDate = runCatching {
            LocalDate.parse(dateText.take(10))
        }.getOrNull()

        return listOfNotNull(singleDate)
    }

    private fun cancellationReasonFor(
        item: JSONObject
    ): CancellationReason? {
        val title = listOf(
            item.optString("title", ""),
            item.optString("title_he", ""),
            item.optString("title_en", ""),
            item.optString("hebrew", ""),
            item.optString("english", ""),
            item.optString("name", ""),
            item.optString("name_he", ""),
            item.optString("name_en", ""),
            item.optString("category", ""),
            item.optString("subcat", "")
        )
            .joinToString(" ")
            .trim()

        if (title.isBlank()) {
            return null
        }

        val clean = title
            .lowercase(Locale("he", "IL"))
            .replace("׳", "'")
            .replace("״", "\"")

        val isEve =
            clean.contains("ערב") ||
                    clean.contains("erev")

        if (
            clean.contains("תשעה באב") ||
            clean.contains("ט' באב") ||
            clean.contains("ט באב") ||
            clean.contains("tisha b'av") ||
            clean.contains("tisha bav")
        ) {
            return CancellationReason(
                he = "צום תשעה באב",
                en = "Tisha B’Av fast"
            )
        }

        if (clean.contains("ראש השנה")) {
            return CancellationReason(
                he = if (isEve) {
                    "ערב ראש השנה"
                } else {
                    "ראש השנה"
                },
                en = if (isEve) {
                    "Rosh Hashanah Eve"
                } else {
                    "Rosh Hashanah"
                }
            )
        }

        if (
            clean.contains("יום כיפור") ||
            clean.contains("יום הכיפורים") ||
            clean.contains("yom kippur")
        ) {
            return CancellationReason(
                he = if (isEve) {
                    "ערב יום כיפור"
                } else {
                    "יום כיפור"
                },
                en = if (isEve) {
                    "Yom Kippur Eve"
                } else {
                    "Yom Kippur"
                }
            )
        }

        if (clean.contains("שמחת תורה")) {
            return CancellationReason(
                he = if (isEve) {
                    "ערב שמחת תורה"
                } else {
                    "שמחת תורה"
                },
                en = if (isEve) {
                    "Simchat Torah Eve"
                } else {
                    "Simchat Torah"
                }
            )
        }

        if (clean.contains("חול המועד סוכות")) {
            return CancellationReason(
                he = "חול המועד סוכות",
                en = "Sukkot Intermediate Days"
            )
        }

        if (clean.contains("סוכות")) {
            return CancellationReason(
                he = if (isEve) {
                    "ערב סוכות"
                } else {
                    "סוכות"
                },
                en = if (isEve) {
                    "Sukkot Eve"
                } else {
                    "Sukkot"
                }
            )
        }

        if (clean.contains("חול המועד פסח")) {
            return CancellationReason(
                he = "חול המועד פסח",
                en = "Passover Intermediate Days"
            )
        }

        if (clean.contains("פסח")) {
            return CancellationReason(
                he = if (isEve) {
                    "ערב פסח"
                } else {
                    "פסח"
                },
                en = if (isEve) {
                    "Passover Eve"
                } else {
                    "Passover"
                }
            )
        }

        if (clean.contains("שבועות")) {
            return CancellationReason(
                he = if (isEve) {
                    "ערב שבועות"
                } else {
                    "שבועות"
                },
                en = if (isEve) {
                    "Shavuot Eve"
                } else {
                    "Shavuot"
                }
            )
        }

        return null
    }

    private fun firstNonBlank(
        vararg values: String
    ): String {
        return values
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()
    }
}