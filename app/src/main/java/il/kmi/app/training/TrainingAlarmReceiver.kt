package il.kmi.app.training

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.net.toUri
import il.kmi.app.reminders.TrainingReminderScheduler
import org.json.JSONArray

class TrainingAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE = "il.kmi.app.training.FIRE"
        const val ACTION_BOOT = "il.kmi.app.training.BOOT"
        const val ACTION_SNOOZE = "il.kmi.app.training.SNOOZE"

        // מזהי תזמונים ישנים נשמרים רק לצורך ביטול.
        private const val SP_SCHEDULED_REQS = "scheduled_training_alarm_reqs"

        // קורא את מזהי ההתראות הישנות לצורך ביטול בלבד.
        private fun loadScheduledReqs(spUser: SharedPreferences): List<Int> {
            val raw = spUser.getString(SP_SCHEDULED_REQS, null) ?: return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.getInt(it) }
            }.getOrElse { emptyList() }
        }

        /**
         * ✅ מתזמן תזכורות שבועיות (leadMinutes דקות לפני האימון).
         * תומך Multi-branch + Multi-group.
         */
        fun scheduleWeeklyAlarms(
            ctx: Context,
            leadMinutes: Int = 60
        ) {
            val appContext =
                ctx.applicationContext

            /*
             * מנקים קודם את האזעקות מהמנגנון הישן,
             * כדי שלא יתקבלו התראות כפולות.
             */
            cancelWeeklyAlarms(appContext)

            /*
             * כל התזמון החדש מתבצע דרך המתזמן המרכזי,
             * שמשתמש ב־TrainingStatusEngine.
             */
            TrainingReminderScheduler
                .scheduleWeeklyTrainingAlarms(
                    context = appContext,
                    leadMinutes =
                        leadMinutes.coerceIn(0, 180)
                )
        }

        /** ✅ מבטל את כל התזמונים השבועיים לפי הרשימה ששמרנו. */
        fun cancelWeeklyAlarms(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val spUser = ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

            val reqs = loadScheduledReqs(spUser)

            if (reqs.isEmpty()) {
                // fallback ישן (שלא יישאר כלום אם היה בעבר)
                for (rc in 1001..1020) {
                    val intent = Intent(ctx, TrainingAlarmReceiver::class.java).apply {
                        action = ACTION_FIRE
                        data = "kmi://train_alarm/$rc".toUri()
                    }
                    val pi = PendingIntent.getBroadcast(
                        ctx,
                        rc,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    am.cancel(pi)
                }
                return
            }

            reqs.distinct().forEach { rc ->
                val intent = Intent(ctx, TrainingAlarmReceiver::class.java).apply {
                    action = ACTION_FIRE
                    data = "kmi://train_alarm/$rc".toUri()
                }
                val pi = PendingIntent.getBroadcast(
                    ctx,
                    rc,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                am.cancel(pi)
            }

            spUser.edit()
                .remove(SP_SCHEDULED_REQS)
                .apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            // ⬅️ חשוב: אירועים מערכתיים אמיתיים אחרי אתחול/עדכון אפליקציה
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_BOOT -> {
                val appContext =
                    context.applicationContext

                val sp =
                    appContext.getSharedPreferences(
                        "kmi_settings",
                        Context.MODE_PRIVATE
                    )

                val enabled =
                    sp.getBoolean(
                        "training_reminders_enabled",
                        false
                    )

                val leadMin =
                    sp.getInt(
                        "lead_minutes",
                        60
                    ).coerceIn(0, 180)

                if (enabled) {
                    scheduleWeeklyAlarms(
                        ctx = appContext,
                        leadMinutes = leadMin
                    )
                } else {
                    /*
                     * אם התזכורות כבויות, מנקים גם את
                     * המנגנון הישן וגם את החדש.
                     */
                    cancelWeeklyAlarms(appContext)

                    TrainingReminderScheduler
                        .cancelWeeklyTrainingAlarms(
                            appContext
                        )
                }
            }

            ACTION_SNOOZE -> {
                /*
                 * מנגנון הדחייה הישן אינו פעיל.
                 * אין לתזמן ACTION_FIRE שלא מציג התראה.
                 *
                 * דחיית התראות פעילות תיבדק מול
                 * TrainingReminderScheduler.
                 */
            }

            ACTION_FIRE -> {
                /*
                 * פעולה ישנה שאינה פעילה.
                 * אין להציג התראה ואין לשנות תזמונים.
                 *
                 * התראות פעילות מנוהלות דרך
                 * TrainingReminderScheduler.
                 */
            }
        }
    }
}
