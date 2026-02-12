package il.kmi.app.training

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import il.kmi.app.MainActivity
import il.kmi.app.R
import org.json.JSONArray
import java.util.Calendar

class TrainingAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE = "il.kmi.app.training.FIRE"
        const val ACTION_BOOT = "il.kmi.app.training.BOOT"
        const val ACTION_SNOOZE = "il.kmi.app.training.SNOOZE"

        private const val CH_ID = "il.kmi.app.training.SNOOZE"
        private const val CH_NAME = "תזכורות אימון"

        // ✅ איפה נשמור את ה-reqCodes שתוזמנו בפועל (כדי לבטל בדיוק)
        private const val SP_SCHEDULED_REQS = "scheduled_training_alarm_reqs"

        private fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(CH_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(CH_ID, CH_NAME, NotificationManager.IMPORTANCE_HIGH)
                    )
                }
            }
        }

        // ============================
        // ✅ Multi-branch / Multi-group readers (כמו אצלך במסכי הבית)
        // ============================
        private fun readSelectedBranches(sp: SharedPreferences): List<String> {
            val fromJsonOrCsv = runCatching {
                val js = sp.getString("branches_json", null) ?: sp.getString("branches", null)
                if (!js.isNullOrBlank()) {
                    if (js.trim().startsWith("[")) {
                        val arr = JSONArray(js)
                        (0 until arr.length())
                            .mapNotNull { arr.optString(it, null) }
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    } else {
                        js.split(',', ';', '|', '\n')
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    }
                } else null
            }.getOrNull()

            if (!fromJsonOrCsv.isNullOrEmpty()) return fromJsonOrCsv.distinct()

            val b1Raw = sp.getString("branch", "")?.trim().orEmpty()
            val fromBranchCsv =
                if (b1Raw.contains(',') || b1Raw.contains(';') || b1Raw.contains('|') || b1Raw.contains('\n'))
                    b1Raw.split(',', ';', '|', '\n').map { it.trim() }.filter { it.isNotBlank() }
                else listOf(b1Raw).filter { it.isNotBlank() }

            val b2 = sp.getString("branch2", "")?.trim().orEmpty()
            val b3 = sp.getString("branch3", "")?.trim().orEmpty()

            return (fromBranchCsv + listOf(b2, b3))
                .filter { it.isNotBlank() }
                .distinct()
        }

        private fun readSelectedGroups(sp: SharedPreferences): List<String> {
            val groupsCsv =
                sp.getString("age_groups", null)?.takeIf { it.isNotBlank() }
                    ?: sp.getString("age_group", null)?.takeIf { it.isNotBlank() }
                    ?: sp.getString("group", null).orEmpty()

            val raw = groupsCsv
                .split(',', ';', '|', '\n')
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val normalized = raw.map { TrainingCatalog.normalizeGroupLabel(it).ifBlank { it } }
            return (if (normalized.isEmpty()) listOf("בוגרים") else normalized).distinct()
        }

        private fun reqCodeFor(
            branch: String,
            group: String,
            day: Int,
            hour: Int,
            minute: Int,
            leadMin: Int
        ): Int {
            val key = "kmi_train|$branch|$group|$day|$hour|$minute|$leadMin"
            return (key.hashCode() and 0x7FFFFFFF)
        }

        private fun saveScheduledReqs(spUser: SharedPreferences, reqs: List<Int>) {
            val arr = JSONArray()
            reqs.distinct().forEach { arr.put(it) }
            spUser.edit().putString(SP_SCHEDULED_REQS, arr.toString()).apply()
        }

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
        fun scheduleWeeklyAlarms(ctx: Context, leadMinutes: Int = 60) {
            ensureChannel(ctx)
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val spUser = ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

            val branches = readSelectedBranches(spUser)
            val groups = readSelectedGroups(spUser)

            if (branches.isEmpty() || groups.isEmpty()) return

            // ✅ קודם נבטל מה שהיה מתוזמן (כדי לא להשאיר "רוחות")
            cancelWeeklyAlarms(ctx)

            val scheduledReqs = mutableListOf<Int>()

            // לכל שילוב סניף×קבוצה
            for (branch in branches) {
                for (group in groups) {
                    val trainings = TrainingCatalog.trainingsFor(branch, group)
                    if (trainings.isEmpty()) continue

                    val place = TrainingCatalog.placeFor(branch)

                    trainings.forEach { td ->
                        val cal0 = td.cal
                        val day = cal0.get(Calendar.DAY_OF_WEEK)
                        val hour = cal0.get(Calendar.HOUR_OF_DAY)
                        val minute = cal0.get(Calendar.MINUTE)

                        val timeText = "%02d:%02d".format(hour, minute)
                        val title = "עוד $leadMinutes דק׳ אימון ב־$timeText ב$place\n$branch • $group"

                        val req = reqCodeFor(branch, group, day, hour, minute, leadMinutes)

                        val eventCal = Calendar.getInstance().apply {
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            set(Calendar.DAY_OF_WEEK, day)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                        }

                        val fireAt = (eventCal.clone() as Calendar).apply {
                            add(Calendar.MINUTE, -leadMinutes)
                        }
                        if (fireAt.timeInMillis <= System.currentTimeMillis()) {
                            fireAt.add(Calendar.WEEK_OF_YEAR, 1)
                        }

                        val intent = Intent(ctx, TrainingAlarmReceiver::class.java).apply {
                            action = ACTION_FIRE
                            putExtra("title", title)
                            putExtra("notif_id", req)

                            // ✅ קריטי: ייחודיות ל-Intent כדי שה-PendingIntent לא יידרס
                            data = Uri.parse("kmi://train_alarm/$req")
                        }

                        val pi = PendingIntent.getBroadcast(
                            ctx,
                            req,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        // inexact שבועי מספיק לתזכורות
                        am.setInexactRepeating(
                            AlarmManager.RTC_WAKEUP,
                            fireAt.timeInMillis,
                            AlarmManager.INTERVAL_DAY * 7,
                            pi
                        )

                        scheduledReqs += req
                    }
                }
            }

            // ✅ נשמור את מה שתוזמן כדי שנוכל לבטל בדיוק
            saveScheduledReqs(spUser, scheduledReqs)
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
                        data = Uri.parse("kmi://train_alarm/$rc")
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
                    data = Uri.parse("kmi://train_alarm/$rc")
                }
                val pi = PendingIntent.getBroadcast(
                    ctx,
                    rc,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                am.cancel(pi)
            }

            spUser.edit().remove(SP_SCHEDULED_REQS).apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            // ⬅️ חשוב: אירועים מערכתיים אמיתיים אחרי אתחול/עדכון אפליקציה
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val sp = context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
                val enabled = sp.getBoolean("training_reminders_enabled", false)
                val leadMin = sp.getInt("lead_minutes", 60).coerceIn(0, 180)
                if (enabled) scheduleWeeklyAlarms(context, leadMin)
            }

            ACTION_BOOT -> scheduleWeeklyAlarms(context) // ברירת מחדל 60 דק׳ (תמיכה לאחור/בדיקות)

            ACTION_SNOOZE -> {
                // דחייה לפי דקות (ברירת־מחדל 10)
                val title = intent.getStringExtra("title") ?: "אימון היום"
                val delayMin = intent.getIntExtra("delayMin", 10)
                val am = context.getSystemService(AlarmManager::class.java)

                val fireIntent = Intent(context, TrainingAlarmReceiver::class.java).apply {
                    action = ACTION_FIRE
                    putExtra("title", title)
                }
                val firePI = PendingIntent.getBroadcast(
                    context,
                    2000,
                    fireIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val triggerAt = System.currentTimeMillis() + delayMin * 60_000L

                // ⚠️ טיפול באזהרה: בדיקה אם מותר לנו exact alarm + טיפול ב-SecurityException
                if (am != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val canExact = am.canScheduleExactAlarms()
                        try {
                            if (canExact) {
                                am.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    triggerAt,
                                    firePI
                                )
                            } else {
                                // fallback – לא exact אבל עדיף מכלום
                                am.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    triggerAt,
                                    firePI
                                )
                            }
                        } catch (_: SecurityException) {
                            // במקרה של SecurityException – fallback רגיל
                            am.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerAt,
                                firePI
                            )
                        }
                    } else {
                        // גרסאות ישנות יותר – כמו קודם
                        am.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            firePI
                        )
                    }
                }

                android.widget.Toast
                    .makeText(context, "ההתראה נדחתה ב־$delayMin דק׳", android.widget.Toast.LENGTH_SHORT)
                    .show()
            }

            ACTION_FIRE -> {
                ensureChannel(context)
                val title = intent.getStringExtra("title") ?: "אימון היום"

                // Android 13+: בדיקת POST_NOTIFICATIONS
                if (Build.VERSION.SDK_INT >= 33) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) return
                }

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Back stack תקין ל־MainActivity
                val tapIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("from_notification", true)
                }

                // ✅ עדיף להשתמש ב-notifId כ-requestCode כדי למנוע התנגשויות בלחיצות
                val notifId = intent.getIntExtra("notif_id", 0)
                    .takeIf { it != 0 } ?: (System.currentTimeMillis() and 0x7FFFFFFF).toInt()

                val contentPI = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(tapIntent)
                    .getPendingIntent(
                        notifId,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                // פעולה: דחה ב־10 דק׳
                val snoozeIntent = Intent(context, TrainingAlarmReceiver::class.java).apply {
                    action = ACTION_SNOOZE
                    putExtra("title", title)
                    putExtra("delayMin", 10)
                }
                val snoozePI = PendingIntent.getBroadcast(
                    context,
                    2001,
                    snoozeIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notif = NotificationCompat.Builder(context, CH_ID)
                    .setSmallIcon(R.drawable.kami_logo)
                    .setContentTitle("תזכורת אימון")
                    .setContentText(title)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(title))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setColor(0xFF1565C0.toInt())
                    .setDefaults(NotificationCompat.DEFAULT_ALL) // צליל/רטט/לד
                    .setContentIntent(contentPI)
                    .addAction(0, "דחה ב־10 דק׳", snoozePI)
                    .build()

                nm.notify(notifId, notif)
            }
        }
    }
}
