package il.kmi.app.reminders

import android.Manifest
import android.app.BroadcastOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import il.kmi.app.MainActivity
import il.kmi.app.domain.Explanations
import il.kmi.shared.domain.Belt
import il.kmi.shared.reminders.DailyExercisePicker

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        android.util.Log.d("KMI_REMINDER", "DailyReminderReceiver triggered")

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val reminderPrefs = ReminderPrefs(prefs)

        val isCoach = isCoachUser(prefs)
        val isEnabled = reminderPrefs.isEnabledForRole(isCoach)

        android.util.Log.d(
            "KMI_REMINDER",
            "Receiver state: isCoach=$isCoach isEnabled=$isEnabled"
        )

        if (!isEnabled) {
            android.util.Log.d("KMI_REMINDER", "Receiver exit: reminder disabled")
            return
        }

        val userSp = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)

        android.util.Log.d("KMI_REMINDER", "kmi_user keys=${userSp.all}")
        android.util.Log.d("KMI_REMINDER", "kmi_prefs keys=${prefs.all}")

        val registeredBelt = getRegisteredBelt(prefs, context)
        android.util.Log.d("KMI_REMINDER", "Receiver state: registeredBelt=$registeredBelt")

        if (registeredBelt == null) {
            android.util.Log.d("KMI_REMINDER", "Receiver exit: registeredBelt is null")
            return
        }

        val picker = DailyExercisePicker()
        val picked = picker.pickNextExerciseForUser(
            registeredBelt = registeredBelt,
            lastItemKey = reminderPrefs.getLastItemKey()
        )

        android.util.Log.d("KMI_REMINDER", "Receiver state: picked=$picked")

        if (picked == null) {
            android.util.Log.d("KMI_REMINDER", "Receiver exit: no exercise picked")
            return
        }

        val explanation = Explanations.get(picked.belt, picked.item).trim()
        android.util.Log.d(
            "KMI_REMINDER",
            "Receiver state: explanationBlank=${explanation.isBlank()} explanationLength=${explanation.length}"
        )

        if (explanation.isBlank()) {
            android.util.Log.d("KMI_REMINDER", "Receiver exit: explanation is blank")
            return
        }

        reminderPrefs.setLastItemKey(picker.candidateKey(picked))

        createNotificationChannel(context)

        val openCardIntent = Intent(context, DailyReminderCardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("daily_reminder_belt_id", picked.belt.id)
            putExtra("daily_reminder_topic", picked.topic)
            putExtra("daily_reminder_item", picked.item)
            putExtra("daily_reminder_explanation", explanation)
            putExtra("daily_reminder_extra_count", 0)
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            openCardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "${picked.belt.heb} • ${picked.item} — לחץ לצפייה"

        val favoriteIntent = Intent(context, DailyReminderFavoriteReceiver::class.java).apply {
            putExtra("daily_reminder_item", picked.item)
        }

        val favoritePendingIntent = PendingIntent.getBroadcast(
            context,
            9201,
            favoriteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val anotherIntent = Intent(context, DailyReminderAnotherReceiver::class.java)

        val anotherPendingIntent = PendingIntent.getBroadcast(
            context,
            9202,
            anotherIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("התרגיל היומי שלך")
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "התרגיל היומי שלך\n" +
                                "${picked.belt.heb} • ${picked.item}\n\n" +
                                explanation
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.btn_star_big_on,
                "⭐ שמור",
                favoritePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_rotate,
                "➕ תרגיל נוסף",
                anotherPendingIntent
            )
            .build()
        val canPostNotifications =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        android.util.Log.d(
            "KMI_REMINDER",
            "Receiver state: canPostNotifications=$canPostNotifications"
        )

        if (canPostNotifications) {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_DAILY_EXERCISE, notification)
            android.util.Log.d("KMI_REMINDER", "Notification posted")
        } else {
            android.util.Log.d("KMI_REMINDER", "Receiver exit: POST_NOTIFICATIONS denied")
        }

        DailyReminderScheduler.rescheduleNextDay(context)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Exercise Reminder",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "תזכורת יומית עם תרגיל מהחגורה הבאה"
        }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun isCoachUser(prefs: SharedPreferences): Boolean {
        val rawRole = prefs.getString(KEY_USER_ROLE, "trainee").orEmpty()
        return rawRole.trim()
            .lowercase()
            .contains("coach")
    }

    private fun getRegisteredBelt(prefs: SharedPreferences, context: Context): Belt? {
        val userSp = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)

        val fromUserPrefs = userSp.getString(KEY_REGISTERED_BELT, null)
            ?.trim()
            ?.lowercase()
            ?.let { Belt.fromId(it) }

        if (fromUserPrefs != null) return fromUserPrefs

        val fromLegacyUserPrefs = userSp.getString("belt", null)
            ?.trim()
            ?.lowercase()
            ?.let { Belt.fromId(it) }

        if (fromLegacyUserPrefs != null) return fromLegacyUserPrefs

        val fromCurrentPrefs = prefs.getString(KEY_REGISTERED_BELT, null)
            ?.trim()
            ?.lowercase()
            ?.let { Belt.fromId(it) }

        return fromCurrentPrefs
    }

    companion object {
        private const val PREFS_NAME = "kmi_prefs"
        private const val USER_PREFS_NAME = "kmi_user"
        private const val KEY_USER_ROLE = "kmi.user.role"
        private const val KEY_REGISTERED_BELT = "registered_belt"

        private const val CHANNEL_ID = "daily_exercise_reminder_channel"
        private const val NOTIFICATION_ID_DAILY_EXERCISE = 41022
        private const val REQUEST_CODE_OPEN_APP = 41023
    }
}