package il.kmi.app.free_sessions.ui.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import il.kmi.shared.free_sessions.model.FreeSession

object FreeSessionReminderScheduler {

    private const val SP = "kmi_settings"
    private const val KEY_ENABLED = "free_sessions_reminders_enabled"

    fun isEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(SP, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    /**
     * מתזמן 2 התראות: 30 ו-10 דקות לפני startsAt
     * רק אם feature enabled ורק אם הזמנים עדיין בעתיד.
     */
    fun scheduleForGoing(
        ctx: Context,
        branch: String,
        groupKey: String,
        session: FreeSession
    ) {
        if (!isEnabled(ctx)) return

        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val startsAt = session.startsAt

        scheduleOne(am, ctx, branch, groupKey, session, leadMin = 30)
        scheduleOne(am, ctx, branch, groupKey, session, leadMin = 10)
    }

    /**
     * מבטל 2 התראות (30/10) עבור אימון מסוים.
     * לקרוא לזה כשמשתמש משנה סטטוס מ-GOING לסטטוס אחר.
     */
    fun cancelForSession(ctx: Context, sessionId: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(30, 10).forEach { lead ->
            val pi = pendingIntent(ctx, sessionId, lead, create = false)
            if (pi != null) am.cancel(pi)
        }
    }

    /**
     * ביטול גורף (למשל אם המשתמש כיבה את ההגדרה).
     * כאן אנחנו לא יודעים sessionIds, אז עושים טריק: שומרים רשימת sessionIds שתוזמנו.
     */
    fun cancelAll(ctx: Context) {
        val sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val ids = sp.getStringSet("free_sessions_reminders_scheduled_ids", emptySet()) ?: emptySet()
        ids.forEach { id -> cancelForSession(ctx, id) }
        sp.edit().remove("free_sessions_reminders_scheduled_ids").apply()
    }

    // -------------------- internal --------------------

    private fun scheduleOne(
        am: AlarmManager,
        ctx: Context,
        branch: String,
        groupKey: String,
        session: FreeSession,
        leadMin: Int
    ) {
        val triggerAt = session.startsAt - (leadMin * 60_000L)
        if (triggerAt <= System.currentTimeMillis() + 3_000L) return // אם כבר עבר/עוד רגע — לא מתזמנים

        val pi = pendingIntent(ctx, session.id, leadMin, create = true) ?: return

        // שומרים sessionId כדי שנוכל cancelAll
        val sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val ids = (sp.getStringSet("free_sessions_reminders_scheduled_ids", emptySet()) ?: emptySet()).toMutableSet()
        ids.add(session.id)
        sp.edit().putStringSet("free_sessions_reminders_scheduled_ids", ids).apply()

        val i = Intent(ctx, FreeSessionReminderReceiver::class.java).apply {
            putExtra(FreeSessionReminderReceiver.EXTRA_SESSION_ID, session.id)
            putExtra(FreeSessionReminderReceiver.EXTRA_TITLE, session.title)
            putExtra(FreeSessionReminderReceiver.EXTRA_BRANCH, branch)
            putExtra(FreeSessionReminderReceiver.EXTRA_GROUP, groupKey)
            putExtra(FreeSessionReminderReceiver.EXTRA_STARTS_AT, session.startsAt)
            putExtra(FreeSessionReminderReceiver.EXTRA_LEAD_MIN, leadMin)
        }

        // re-create PI with the intent that contains extras
        val fullPi = PendingIntent.getBroadcast(
            ctx,
            requestCode(session.id, leadMin),
            i,
            pendingFlags(create = true)
        )

        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, fullPi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, fullPi)
        }
    }

    private fun pendingIntent(ctx: Context, sessionId: String, leadMin: Int, create: Boolean): PendingIntent? {
        val i = Intent(ctx, FreeSessionReminderReceiver::class.java)
        val flags = pendingFlags(create)
        return try {
            PendingIntent.getBroadcast(ctx, requestCode(sessionId, leadMin), i, flags)
        } catch (_: Exception) {
            null
        }
    }

    private fun requestCode(sessionId: String, leadMin: Int): Int {
        // יציב וקצר: hash של sessionId + lead
        return (sessionId.hashCode() * 31 + leadMin).toInt()
    }

    private fun pendingFlags(create: Boolean): Int {
        val base = if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) base or PendingIntent.FLAG_IMMUTABLE else base
    }
}
