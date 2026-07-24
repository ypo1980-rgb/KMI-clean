package il.kmi.app.training

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import il.kmi.app.halacha.HolidayCalendarRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class TrainingStatusEngineInstrumentedTest {

    private val context: Context
        get() =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .applicationContext

    private val israelZone: ZoneId =
        ZoneId.of("Asia/Jerusalem")

    private fun millisAtNoon(
        date: LocalDate
    ): Long {
        return date
            .atTime(12, 0)
            .atZone(israelZone)
            .toInstant()
            .toEpochMilli()
    }

    private fun allKnownHolidays():
            List<HolidayCalendarRepository.HolidayInfo> {
        return buildList {
            for (year in 2024..2026) {
                for (month in 1..12) {
                    addAll(
                        HolidayCalendarRepository
                            .holidaysForMonth(
                                context = context,
                                yearMonth =
                                    YearMonth.of(year, month)
                            )
                            .values
                            .flatten()
                    )
                }
            }
        }
    }

    @Test
    fun holidayAsset_isLoaded() {
        val holidays = allKnownHolidays()

        assertTrue(
            "Holiday asset should contain holidays",
            holidays.isNotEmpty()
        )
    }

    @Test
    fun futureYear_isCalculatedWithoutAsset() {
        val futureHolidays =
            buildList {
                for (month in 1..12) {
                    addAll(
                        HolidayCalendarRepository
                            .holidaysForMonth(
                                context = context,
                                yearMonth =
                                    YearMonth.of(2027, month)
                            )
                            .values
                            .flatten()
                    )
                }
            }

        assertTrue(
            "2027 holidays should be calculated dynamically",
            futureHolidays.isNotEmpty()
        )

        val cancellingHoliday =
            futureHolidays.firstOrNull { holiday ->
                holiday.cancelsTraining
            }

        assertNotNull(
            "2027 should contain a holiday that cancels training",
            cancellingHoliday
        )

        val holiday = requireNotNull(cancellingHoliday)
        val startMillis = millisAtNoon(holiday.date)

        val status = TrainingStatusEngine.evaluate(
            context = context,
            trainingStartMillis = startMillis,
            nowMillis = startMillis - 60_000L
        )

        assertEquals(
            TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY,
            status.state
        )

        assertTrue(status.isCancelled)
        assertFalse(status.shouldNotify)
        assertFalse(status.shouldAddToCalendar)
        assertTrue(!status.reasonHe.isNullOrBlank())
        assertTrue(!status.reasonEn.isNullOrBlank())
    }

    @Test
    fun cancellingHoliday_returnsCancelledStatus() {
        val cancellingHoliday =
            allKnownHolidays()
                .firstOrNull { holiday ->
                    holiday.cancelsTraining
                }

        assertNotNull(
            "At least one holiday must cancel training",
            cancellingHoliday
        )

        val holiday = requireNotNull(cancellingHoliday)
        val startMillis = millisAtNoon(holiday.date)

        val status = TrainingStatusEngine.evaluate(
            context = context,
            trainingStartMillis = startMillis,
            nowMillis = startMillis - 60_000L
        )

        assertEquals(
            TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY,
            status.state
        )

        assertTrue(status.isCancelled)
        assertFalse(status.shouldNotify)
        assertFalse(status.shouldAddToCalendar)
        assertTrue(!status.reasonHe.isNullOrBlank())
        assertTrue(!status.reasonEn.isNullOrBlank())
    }

    @Test
    fun tishaBav_cancelsTraining() {
        val tishaBav =
            allKnownHolidays()
                .firstOrNull { holiday ->
                    holiday.cancellationReason?.he ==
                            "צום תשעה באב"
                }

        assertNotNull(
            "Tisha B’Av must exist in the holiday asset",
            tishaBav
        )

        val holiday = requireNotNull(tishaBav)
        val startMillis = millisAtNoon(holiday.date)

        val status = TrainingStatusEngine.evaluate(
            context = context,
            trainingStartMillis = startMillis,
            nowMillis = startMillis - 60_000L
        )

        assertEquals(
            TrainingStatusEngine.State.CANCELLED_BY_HOLIDAY,
            status.state
        )

        assertEquals(
            "צום תשעה באב",
            status.reasonHe
        )

        assertEquals(
            "Tisha B’Av fast",
            status.reasonEn
        )
    }

    @Test
    fun nonBlockingHoliday_doesNotCancelTraining() {
        val nonBlockingHoliday =
            allKnownHolidays()
                .firstOrNull { holiday ->
                    val combinedName = buildString {
                        append(holiday.nameHe)
                        append(" ")
                        append(holiday.nameEn)
                    }.lowercase()

                    !holiday.cancelsTraining &&
                            listOf(
                                "חנוכה",
                                "hanukkah",
                                "פורים",
                                "purim",
                                "ראש חודש"
                            ).any { keyword ->
                                combinedName.contains(keyword)
                            }
                }

        assertNotNull(
            "A known non-blocking holiday must exist",
            nonBlockingHoliday
        )

        val holiday = requireNotNull(nonBlockingHoliday)
        val startMillis = millisAtNoon(holiday.date)

        val status = TrainingStatusEngine.evaluate(
            context = context,
            trainingStartMillis = startMillis,
            nowMillis = startMillis - 60_000L
        )

        assertEquals(
            TrainingStatusEngine.State.SCHEDULED,
            status.state
        )

        assertFalse(status.isCancelled)
        assertTrue(status.shouldNotify)
        assertTrue(status.shouldAddToCalendar)
    }

    @Test
    fun regularDate_returnsScheduledStatus() {
        val yearMonth = YearMonth.of(2026, 1)

        val holidays =
            HolidayCalendarRepository.holidaysForMonth(
                context = context,
                yearMonth = yearMonth
            )

        val regularDate =
            (1..yearMonth.lengthOfMonth())
                .map { day ->
                    yearMonth.atDay(day)
                }
                .first { date ->
                    date !in holidays
                }

        val startMillis = millisAtNoon(regularDate)

        val status = TrainingStatusEngine.evaluate(
            context = context,
            trainingStartMillis = startMillis,
            nowMillis = startMillis - 60_000L
        )

        assertEquals(
            TrainingStatusEngine.State.SCHEDULED,
            status.state
        )

        assertTrue(status.isScheduled)
        assertFalse(status.isCancelled)
        assertTrue(status.shouldNotify)
        assertTrue(status.shouldAddToCalendar)
    }

    @Test
    fun pastRegularTraining_returnsCompletedStatus() {
        val yearMonth = YearMonth.of(2024, 1)

        val holidays =
            HolidayCalendarRepository.holidaysForMonth(
                context = context,
                yearMonth = yearMonth
            )

        val regularDate =
            (1..yearMonth.lengthOfMonth())
                .map { day ->
                    yearMonth.atDay(day)
                }
                .first { date ->
                    date !in holidays
                }

        val startMillis = millisAtNoon(regularDate)

        val status = TrainingStatusEngine.evaluate(
            context = context,
            trainingStartMillis = startMillis,
            nowMillis = startMillis + 60_000L
        )

        assertEquals(
            TrainingStatusEngine.State.COMPLETED,
            status.state
        )

        assertTrue(status.isCompleted)
        assertFalse(status.shouldNotify)

        /*
         * אימון שהתקיים נשאר ביומן ההיסטורי.
         */
        assertTrue(status.shouldAddToCalendar)
    }
}