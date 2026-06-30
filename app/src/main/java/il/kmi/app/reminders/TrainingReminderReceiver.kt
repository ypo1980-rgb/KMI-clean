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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val branch = intent.getStringExtra(EXTRA_BRANCH).orEmpty()
        val group = intent.getStringExtra(EXTRA_GROUP).orEmpty()
        val place = intent.getStringExtra(EXTRA_PLACE).orEmpty()
        val coach = intent.getStringExtra(EXTRA_COACH).orEmpty()
        val startMillis = intent.getLongExtra(EXTRA_START_MILLIS, 0L)

        showTrainingReminderNotification(
            context = context,
            branch = branch,
            group = group,
            place = place,
            coach = coach,
            startMillis = startMillis
        )
    }

    private fun showTrainingReminderNotification(
        context: Context,
        branch: String,
        group: String,
        place: String,
        coach: String,
        startMillis: Long
    ) {
        createChannelIfNeeded(context)

        val timeText = if (startMillis > 0L) {
            SimpleDateFormat("HH:mm", Locale("he", "IL")).format(Date(startMillis))
        } else {
            ""
        }

        val title = "תזכורת לאימון"
        val body = buildString {
            if (timeText.isNotBlank()) {
                append("האימון מתחיל בשעה ")
                append(timeText)
            } else {
                append("יש לך אימון בקרוב")
            }

            val cleanPlace = place.ifBlank { branch }

            if (cleanPlace.isNotBlank()) {
                append(" · ")
                append(cleanPlace)
            }

            if (group.isNotBlank()) {
                append(" · ")
                append(group)
            }

            if (coach.isNotBlank()) {
                append(" · מאמן: ")
                append(coach)
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra("open_from_training_reminder", true)
            putExtra("training_reminder_branch", branch)
            putExtra("training_reminder_group", group)
            putExtra("training_reminder_start_millis", startMillis)
        }

        val requestCode = (System.currentTimeMillis() and 0xFFFFFFF).toInt()

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
        const val EXTRA_COACH = "training_reminder_coach"
        const val EXTRA_START_MILLIS = "training_reminder_start_millis"
        const val EXTRA_LEAD_MINUTES = "training_reminder_lead_minutes"

        private const val CHANNEL_ID = "kmi_training_reminders_channel"
    }
}