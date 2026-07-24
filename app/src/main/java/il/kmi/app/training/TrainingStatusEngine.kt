package il.kmi.app.training

import android.content.Context
import il.kmi.app.halacha.TrainingHolidayCancellationChecker

/**
 * מקור האמת המרכזי למצבו של אימון.
 *
 * כל מסך או שירות שצריך להחליט אם להציג, לשלוח התראה
 * או להוסיף אימון ליומן חייב להשתמש במנוע הזה.
 */
object TrainingStatusEngine {

    enum class State {
        SCHEDULED,
        ONGOING,
        CANCELLED_BY_HOLIDAY,
        COMPLETED,
        INVALID
    }

    data class Status(
        val state: State,
        val reasonHe: String?,
        val reasonEn: String?,
        val shouldNotify: Boolean,
        val shouldAddToCalendar: Boolean
    ) {
        val isCancelled: Boolean
            get() = state == State.CANCELLED_BY_HOLIDAY

        val isScheduled: Boolean
            get() = state == State.SCHEDULED

        val isOngoing: Boolean
            get() = state == State.ONGOING

        val isCompleted: Boolean
            get() = state == State.COMPLETED

        val isInvalid: Boolean
            get() = state == State.INVALID

        fun reason(isEnglish: Boolean): String? {
            return if (isEnglish) {
                reasonEn
            } else {
                reasonHe
            }
        }

        fun displayText(
            isEnglish: Boolean
        ): String {
            return when (state) {
                State.SCHEDULED ->
                    if (isEnglish) {
                        "Scheduled training"
                    } else {
                        "אימון מתוכנן"
                    }

                State.ONGOING ->
                    if (isEnglish) {
                        "Training in progress"
                    } else {
                        "האימון מתקיים כעת"
                    }

                State.CANCELLED_BY_HOLIDAY -> {
                    val cancellationReason =
                        reason(isEnglish).orEmpty()

                    if (isEnglish) {
                        if (cancellationReason.isBlank()) {
                            "Training cancelled"
                        } else {
                            "Training cancelled due to $cancellationReason"
                        }
                    } else {
                        if (cancellationReason.isBlank()) {
                            "האימון מבוטל"
                        } else {
                            "האימון מבוטל עקב $cancellationReason"
                        }
                    }
                }

                State.COMPLETED ->
                    if (isEnglish) {
                        "Training completed"
                    } else {
                        "האימון הסתיים"
                    }

                State.INVALID ->
                    reason(isEnglish)
                        ?: if (isEnglish) {
                            "Invalid training details"
                        } else {
                            "פרטי האימון אינם תקינים"
                        }
            }
        }
    }

    fun evaluate(
        context: Context,
        training: TrainingData,
        nowMillis: Long = System.currentTimeMillis()
    ): Status {
        return evaluate(
            context = context,
            trainingStartMillis = training.startMillis,
            trainingEndMillis = training.endMillis,
            nowMillis = nowMillis
        )
    }

    fun evaluate(
        context: Context,
        trainingStartMillis: Long,
        trainingEndMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Status {
        /*
         * אימון ללא זמן התחלה תקין אינו יכול להיחשב מתוכנן.
         */
        if (trainingStartMillis <= 0L) {
            return Status(
                state = State.INVALID,
                reasonHe = "מועד האימון אינו תקין",
                reasonEn = "The training time is invalid",
                shouldNotify = false,
                shouldAddToCalendar = false
            )
        }

        /*
         * אם נמסר זמן סיום, הוא חייב להיות מאוחר
         * מזמן ההתחלה.
         */
        if (
            trainingEndMillis != null &&
            trainingEndMillis <= trainingStartMillis
        ) {
            return Status(
                state = State.INVALID,
                reasonHe = "שעת סיום האימון אינה תקינה",
                reasonEn = "The training end time is invalid",
                shouldNotify = false,
                shouldAddToCalendar = false
            )
        }

        val holidayReason =
            TrainingHolidayCancellationChecker.cancellationReason(
                context = context.applicationContext,
                trainingStartMillis = trainingStartMillis
            )

        /*
         * ביטול עקב חג מקבל עדיפות על הסטטוס הזמני
         * של האימון.
         */
        if (holidayReason != null) {
            return Status(
                state = State.CANCELLED_BY_HOLIDAY,
                reasonHe = holidayReason.he,
                reasonEn = holidayReason.en,
                shouldNotify = false,
                shouldAddToCalendar = false
            )
        }

        /*
         * מועד ההתחלה עדיין לא הגיע.
         */
        if (nowMillis < trainingStartMillis) {
            return Status(
                state = State.SCHEDULED,
                reasonHe = null,
                reasonEn = null,
                shouldNotify = true,
                shouldAddToCalendar = true
            )
        }

        /*
         * כאשר ידוע זמן הסיום והאימון כבר התחיל
         * אך עדיין לא הסתיים, הוא מתקיים כעת.
         */
        if (
            trainingEndMillis != null &&
            nowMillis < trainingEndMillis
        ) {
            return Status(
                state = State.ONGOING,
                reasonHe = null,
                reasonEn = null,
                shouldNotify = false,
                shouldAddToCalendar = true
            )
        }

        /*
         * אם זמן הסיום אינו ידוע, נשמרת התאימות
         * הישנה: אימון שמועד תחילתו עבר נחשב שהושלם.
         */
        return Status(
            state = State.COMPLETED,
            reasonHe = null,
            reasonEn = null,
            shouldNotify = false,

            /*
             * אימון שהתקיים נשאר ביומן ההיסטורי.
             */
            shouldAddToCalendar = true
        )
    }
}