package il.kmi.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import il.kmi.app.MainActivity
import il.kmi.app.R
import il.kmi.app.training.TrainingStatusEngine
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TrainingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val branch = intent.getStringExtra(EXTRA_BRANCH).orEmpty()
        val group = intent.getStringExtra(EXTRA_GROUP).orEmpty()
        val place = intent.getStringExtra(EXTRA_PLACE).orEmpty()
        val coach =
            intent.getStringExtra(EXTRA_COACH).orEmpty()

        val startMillis =
            intent.getLongExtra(
                EXTRA_START_MILLIS,
                0L
            )

        val rawEndMillis =
            intent.getLongExtra(
                EXTRA_END_MILLIS,
                0L
            )

        /*
         * באזעקות ישנות ייתכן שהערך אינו קיים.
         * במקרה כזה משתמשים בברירת המחדל 60 דקות.
         */
        val leadMinutes =
            if (
                intent.hasExtra(
                    EXTRA_LEAD_MINUTES
                )
            ) {
                intent.getIntExtra(
                    EXTRA_LEAD_MINUTES,
                    60
                )
                    .coerceIn(0, 180)
            } else {
                60
            }

        /*
         * גרסאות קודמות לא שלחו זמן סיום.
         * במקרה כזה מעבירים null ושומרים תאימות.
         */
        val endMillis =
            rawEndMillis.takeIf { value ->
                value > startMillis
            }

        val trainingStatus =
            TrainingStatusEngine.evaluate(
                context = context,
                trainingStartMillis =
                    startMillis,
                trainingEndMillis =
                    endMillis
            )

        /*
         * האזעקה היא חד־פעמית. לכן לאחר הפעלתה חייבים
         * לתזמן מחדש את המופע השבועי הבא, גם כאשר
         * ההתראה הנוכחית חסומה על ידי המנוע.
         */
        try {
            val reminderIdentity =
                buildReminderIdentity(
                    branch = branch,
                    group = group,
                    place = place,
                    startMillis = startMillis
                )

            val maySendReminder =
                trainingStatus.shouldNotify &&
                        claimReminderDelivery(
                            context = context,
                            reminderIdentity =
                                reminderIdentity
                        )

            if (maySendReminder) {
                val isEnglish =
                    AppLanguageManager(context)
                        .getCurrentLanguage() ==
                            AppLanguage.ENGLISH

                showTrainingReminderNotification(
                    context = context,
                    branch = branch,
                    group = group,
                    place = place,
                    coach = coach,
                    startMillis = startMillis,
                    endMillis = endMillis,
                    isEnglish = isEnglish,
                    reminderIdentity =
                        reminderIdentity
                )
            }
        } finally {
            /*
             * המתזמן מבטל את תמונת האזעקות הישנה ובונה
             * תמונה חדשה. הוא גם מדלג על חגים באמצעות
             * TrainingStatusEngine.
             */
            TrainingReminderScheduler
                .scheduleWeeklyTrainingAlarms(
                    context =
                        context.applicationContext,
                    leadMinutes =
                        leadMinutes
                )
        }
    }

    private fun showTrainingReminderNotification(
        context: Context,
        branch: String,
        group: String,
        place: String,
        coach: String,
        startMillis: Long,
        endMillis: Long?,
        isEnglish: Boolean,
        reminderIdentity: String
    ) {
        createChannelIfNeeded(context)

        val locale =
            if (isEnglish) {
                Locale.US
            } else {
                Locale("he", "IL")
            }

        val timeFormatter =
            SimpleDateFormat(
                "HH:mm",
                locale
            ).apply {
                timeZone =
                    TimeZone.getTimeZone(
                        "Asia/Jerusalem"
                    )
            }

        val timeText =
            if (startMillis > 0L) {
                timeFormatter.format(
                    Date(startMillis)
                )
            } else {
                ""
            }

        val title =
            if (isEnglish) {
                "Training reminder"
            } else {
                "תזכורת אימון"
            }

        val body = buildString {
            if (timeText.isNotBlank()) {
                if (isEnglish) {
                    append("Training starts at ")
                } else {
                    append("האימון מתחיל בשעה ")
                }

                append(timeText)
            } else {
                append(
                    if (isEnglish) {
                        "You have a training soon"
                    } else {
                        "יש לך אימון בקרוב"
                    }
                )
            }

            val cleanPlace =
                place.ifBlank { branch }

            if (cleanPlace.isNotBlank()) {
                append(" · ")
                append(cleanPlace)
            }

            if (group.isNotBlank()) {
                append(" · ")
                append(group)
            }

            if (coach.isNotBlank()) {
                append(
                    if (isEnglish) {
                        " · Coach: "
                    } else {
                        " · מאמן: "
                    }
                )
                append(coach)
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra("open_from_training_reminder", true)
            putExtra(
                "training_reminder_branch",
                branch
            )
            putExtra(
                "training_reminder_group",
                group
            )
            putExtra(
                "training_reminder_start_millis",
                startMillis
            )
            putExtra(
                "training_reminder_end_millis",
                endMillis ?: 0L
            )
        }

        /*
         * אותו אימון מקבל תמיד אותו מזהה, גם אם שני
         * מקורות יצרו אותו עם הבדל בשניות/אלפיות שנייה
         * או בנוסח מעט שונה של שם הסניף.
         */
        val requestCode =
            reminderIdentity.hashCode() and Int.MAX_VALUE

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    private fun buildReminderIdentity(
        branch: String,
        group: String,
        place: String,
        startMillis: Long
    ): String {
        fun normalizePart(value: String): String {
            return value
                .trim()
                .lowercase()
                .replace("–", "-")
                .replace("—", "-")
                .replace(Regex("\\s+"), " ")
        }

        /*
         * עיגול לדקה מונע הבדל מלאכותי שנובע
         * משניות או מאלפיות שנייה שונות.
         */
        val startMinute =
            startMillis / 60_000L

        val placeIdentity =
            place.ifBlank { branch }

        return buildString {
            append(startMinute)
            append("|")
            append(normalizePart(placeIdentity))
            append("|")
            append(normalizePart(group))
        }
    }

    private fun claimReminderDelivery(
        context: Context,
        reminderIdentity: String
    ): Boolean {
        /*
         * שני BroadcastReceivers יכולים להגיע כמעט יחד.
         * הסנכרון מבטיח שרק הראשון יסמן את ההתראה כנשלחה.
         */
        synchronized(TrainingReminderReceiver::class.java) {
            val preferences =
                context.getSharedPreferences(
                    DELIVERY_PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val now = System.currentTimeMillis()

            val lastIdentity =
                preferences.getString(
                    KEY_LAST_REMINDER_IDENTITY,
                    null
                )

            val lastDeliveryTime =
                preferences.getLong(
                    KEY_LAST_REMINDER_DELIVERY_TIME,
                    0L
                )

            val isRecentDuplicate =
                lastIdentity == reminderIdentity &&
                        now - lastDeliveryTime in
                        0L until DUPLICATE_GUARD_WINDOW_MILLIS

            if (isRecentDuplicate) {
                return false
            }

            /*
             * commit סינכרוני נדרש כאן כדי שמקלט נוסף
             * שמגיע מיד אחריו יראה את הערך החדש.
             */
            return preferences
                .edit()
                .putString(
                    KEY_LAST_REMINDER_IDENTITY,
                    reminderIdentity
                )
                .putLong(
                    KEY_LAST_REMINDER_DELIVERY_TIME,
                    now
                )
                .commit()
        }
    }



    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Training Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "תזכורות לפני אימונים באפליקציית K.M.I"
            enableVibration(true)
            setShowBadge(true)
        }

        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_BRANCH = "training_reminder_branch"
        const val EXTRA_GROUP = "training_reminder_group"
        const val EXTRA_PLACE = "training_reminder_place"
        const val EXTRA_COACH =
            "training_reminder_coach"

        const val EXTRA_START_MILLIS =
            "training_reminder_start_millis"

        const val EXTRA_END_MILLIS =
            "training_reminder_end_millis"

        const val EXTRA_LEAD_MINUTES =
            "training_reminder_lead_minutes"

        private const val CHANNEL_ID =
            "kmi_training_reminders_channel"

        private const val DELIVERY_PREFS_NAME =
            "kmi_training_reminder_delivery"

        private const val KEY_LAST_REMINDER_IDENTITY =
            "last_training_reminder_identity"

        private const val KEY_LAST_REMINDER_DELIVERY_TIME =
            "last_training_reminder_delivery_time"

        private const val DUPLICATE_GUARD_WINDOW_MILLIS =
            15L * 60L * 1000L
    }
}