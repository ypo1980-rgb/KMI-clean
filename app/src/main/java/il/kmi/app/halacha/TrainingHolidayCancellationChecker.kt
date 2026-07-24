package il.kmi.app.halacha

import android.content.Context
import java.time.Instant
import java.time.ZoneId

/**
 * עטיפת תאימות לבדיקת ביטול אימון.
 *
 * מקור הנתונים והכללים נמצא ב־HolidayCalendarRepository.
 */
object TrainingHolidayCancellationChecker {

    private val israelZone: ZoneId =
        ZoneId.of("Asia/Jerusalem")

    data class CancellationReason(
        val he: String,
        val en: String
    )

    fun cancellationReason(
        context: Context,
        trainingStartMillis: Long
    ): CancellationReason? {
        if (trainingStartMillis <= 0L) {
            return null
        }

        val trainingDate = Instant
            .ofEpochMilli(trainingStartMillis)
            .atZone(israelZone)
            .toLocalDate()

        val reason =
            HolidayCalendarRepository.cancellationReason(
                context = context.applicationContext,
                date = trainingDate
            ) ?: return null

        return CancellationReason(
            he = reason.he,
            en = reason.en
        )
    }

    fun isTrainingCancelled(
        context: Context,
        trainingStartMillis: Long
    ): Boolean {
        return cancellationReason(
            context = context,
            trainingStartMillis = trainingStartMillis
        ) != null
    }
}