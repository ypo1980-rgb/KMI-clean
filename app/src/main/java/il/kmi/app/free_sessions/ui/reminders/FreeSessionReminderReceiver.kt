package il.kmi.app.free_sessions.ui.reminders

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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class FreeSessionReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "אימון חופשי"
        val branch = intent.getStringExtra(EXTRA_BRANCH) ?: ""
        val group = intent.getStringExtra(EXTRA_GROUP) ?: ""
        val startsAt = intent.getLongExtra(EXTRA_STARTS_AT, 0L)
        val leadMin = intent.getIntExtra(EXTRA_LEAD_MIN, 0)

        ensureChannel(context)

        val timeText = if (startsAt > 0L) fmtTimeHeb(startsAt) else ""
        val content = buildString {
            append("עוד $leadMin דקות מתחיל אימון")
            if (timeText.isNotBlank()) append(" • $timeText")
            if (branch.isNotBlank() || group.isNotBlank()) append("\n$branch • $group")
        }

        val requestCode = makeId(intent) // ✅ ייחודי לכל session + lead

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // ✅ ייחודיות ל-Intent כדי למנוע PendingIntent collisions
            data = android.net.Uri.parse("kmi://free_session/$requestCode")
        }

        val pi = PendingIntent.getActivity(
            context,
            requestCode,
            openApp,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(makeId(intent), notif)
    }

    private fun makeId(intent: Intent): Int {
        val sid = intent.getStringExtra(EXTRA_SESSION_ID) ?: ""
        val lead = intent.getIntExtra(EXTRA_LEAD_MIN, 0)
        return sid.hashCode() * 31 + lead
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID,
            "תזכורות אימון חופשי",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "התראות לפני אימונים חופשיים שאישרת הגעה"
        }
        nm.createNotificationChannel(ch)
    }

    private fun fmtTimeHeb(millis: Long): String {
        val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val fmt = DateTimeFormatter.ofPattern("EEEE · d.M.yyyy · HH:mm", Locale("he", "IL"))
        return dt.format(fmt)
    }

    companion object {
        const val CHANNEL_ID = "free_sessions_reminders"

        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BRANCH = "branch"
        const val EXTRA_GROUP = "groupKey"
        const val EXTRA_STARTS_AT = "startsAt"
        const val EXTRA_LEAD_MIN = "leadMin"
    }
}
