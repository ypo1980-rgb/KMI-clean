package il.kmi.app.reminders

import android.Manifest
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
import il.kmi.shared.domain.Explanations
import il.kmi.shared.domain.Belt
import il.kmi.shared.reminders.DailyExercisePicker
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val reminderPrefs = ReminderPrefs(prefs)

        val isEnglish =
            AppLanguageManager(context).getCurrentLanguage() ==
                    AppLanguage.ENGLISH

        fun tr(he: String, en: String): String {
            return if (isEnglish) en else he
        }

        val isCoach = isCoachUser(context, prefs)
        val isEnabled = reminderPrefs.isEnabledForRole(isCoach)

        if (!isEnabled) {
            return
        }

        val registeredBelt = getRegisteredBelt(prefs, context)

        if (registeredBelt == null) {
            DailyReminderScheduler.rescheduleNextDay(context)
            return
        }

        val picker = DailyExercisePicker()
        val picked = picker.pickNextExerciseForUser(
            registeredBelt = registeredBelt,
            lastItemKey = reminderPrefs.getLastItemKey()
        )

        if (picked == null) {
            DailyReminderScheduler.rescheduleNextDay(context)
            return
        }

        val explanation = Explanations.get(picked.belt, picked.item).trim()

        if (explanation.isBlank()) {
            DailyReminderScheduler.rescheduleNextDay(context)
            return
        }

        reminderPrefs.setLastItemKey(picker.candidateKey(picked))

        createNotificationChannel(
            context = context,
            isEnglish = isEnglish
        )

        val beltTitle = if (isEnglish) {
            when (picked.belt) {
                Belt.WHITE -> "White belt"
                Belt.YELLOW -> "Yellow belt"
                Belt.ORANGE -> "Orange belt"
                Belt.GREEN -> "Green belt"
                Belt.BLUE -> "Blue belt"
                Belt.BROWN -> "Brown belt"
                Belt.BLACK -> "Black belt"
            }
        } else {
            picked.belt.heb
        }

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

        val contentText = if (isEnglish) {
            "$beltTitle • ${picked.item} — Tap to view"
        } else {
            "$beltTitle • ${picked.item} — לחץ לצפייה"
        }

        val notificationTitle = tr(
            "התרגיל היומי שלך",
            "Your daily exercise"
        )

        val favoriteActionTitle = tr(
            "⭐ שמור",
            "⭐ Save"
        )

        val anotherActionTitle = tr(
            "➕ תרגיל נוסף",
            "➕ Another exercise"
        )

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
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "$notificationTitle\n" +
                                "$beltTitle • ${picked.item}\n\n" +
                                explanation
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.btn_star_big_on,
                favoriteActionTitle,
                favoritePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_rotate,
                anotherActionTitle,
                anotherPendingIntent
            )
            .build()
        val canPostNotifications =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        if (canPostNotifications) {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_DAILY_EXERCISE, notification)
        }

        DailyReminderScheduler.rescheduleNextDay(context)
    }

    private fun createNotificationChannel(
        context: Context,
        isEnglish: Boolean
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channelName = if (isEnglish) {
            "KAMI daily exercise"
        } else {
            "התרגיל היומי של ק.מ.י"
        }

        val channelDescription = if (isEnglish) {
            "A daily KAMI exercise based on the user's belt"
        } else {
            "תזכורת יומית עם תרגיל ק.מ.י לפי חגורת המשתמש"
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
        }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(channel)
    }

    private fun isCoachUser(
        context: Context,
        prefs: SharedPreferences
    ): Boolean {
        val userPrefs = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)

        /*
         * קוראים תחילה את תפקיד המשתמש המחובר.
         * כך משתמש חדש אינו יורש את תפקיד המשתמש הקודם.
         */
        val rawRole =
            userPrefs.getString(KEY_USER_ROLE, null)
                ?: userPrefs.getString("user_role", null)
                ?: userPrefs.getString("role", null)
                ?: prefs.getString(KEY_USER_ROLE, null)
                ?: prefs.getString("user_role", null)
                ?: prefs.getString("role", null)
                ?: "trainee"

        val clean = rawRole.trim().lowercase()

        return clean == "coach" ||
                clean.contains("coach") ||
                clean.contains("מאמן") ||
                clean.contains("מדריך")
    }

    private fun getRegisteredBelt(
        prefs: SharedPreferences,
        context: Context
    ): Belt? {
        val userSp = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE)

        fun normalizeBeltId(raw: String): String {
            val clean = raw
                .trim()
                .lowercase()
                .replace(" ", "_")
                .replace("-", "_")

            return when {
                clean.isBlank() -> ""

                // ✅ חגורה שחורה כולל דאן 1 עד דאן 10
                clean == "black" -> "black"
                clean == "black_dan_1" -> "black"
                clean == "black_dan_2" -> "black"
                clean == "black_dan_3" -> "black"
                clean == "black_dan_4" -> "black"
                clean == "black_dan_5" -> "black"
                clean == "black_dan_6" -> "black"
                clean == "black_dan_7" -> "black"
                clean == "black_dan_8" -> "black"
                clean == "black_dan_9" -> "black"
                clean == "black_dan_10" -> "black"

                // ✅ תמיכה גם אם נשמר בפורמט כללי יותר
                clean.startsWith("black_dan_") -> "black"
                clean.startsWith("blackdan") -> "black"
                clean.startsWith("dan_") -> "black"

                // ✅ תמיכה בעברית אם נשמר טקסט כזה
                clean == "שחורה" -> "black"
                clean == "חגורה_שחורה" -> "black"
                clean.startsWith("שחורה_דאן") -> "black"
                clean.startsWith("חגורה_שחורה_דאן") -> "black"
                clean.startsWith("דאן") -> "black"

                else -> clean
            }
        }

        fun readBeltFrom(sp: SharedPreferences): Belt? {
            val keys = listOf(
                KEY_REGISTERED_BELT,
                "current_belt",
                "belt_current",
                "belt",
                "registered_belt",
                "registeredRank",
                "rank",
                "rank_id"
            )

            for (key in keys) {
                val normalized = normalizeBeltId(
                    sp.getString(key, null).orEmpty()
                )

                if (normalized.isBlank()) continue

                Belt.fromId(normalized)?.let { return it }
            }

            return null
        }

        return readBeltFrom(userSp)
            ?: readBeltFrom(prefs)
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