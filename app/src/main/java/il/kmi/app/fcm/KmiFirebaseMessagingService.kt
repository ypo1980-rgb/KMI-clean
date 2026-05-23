package il.kmi.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import il.kmi.app.FcmTokenManager
import il.kmi.app.MainActivity
import il.kmi.app.R
import il.kmi.app.notifications.CoachGate

/**
 * שירות שמקבל הודעות מ-FCM ומציג התראות למשתמש.
 */
class KmiFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val cleanToken = token.trim()

        if (cleanToken.isBlank()) {
            return
        }

        // חשוב: שומרים את הטוקן שקיבלנו מ-Firebase ישירות,
        // ולא מבקשים שוב token חדש מ-FirebaseMessaging.
        try {
            FcmTokenManager.saveProvidedTokenForCurrentUser(cleanToken)
        } catch (_: Exception) {
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data

        val rawType =
            data["type"]
                ?: data["pushType"]
                ?: data["notificationType"]
                ?: data["kind"]
                ?: ""

        val type = when (rawType.trim().lowercase()) {
            "coach_broadcast",
            "coachbroadcast",
            "coach_message",
            "coachmessage",
            "broadcast",
            "trainer_message",
            "trainer_broadcast" -> "coach_broadcast"

            "forum_message",
            "forummessage",
            "forum" -> "forum_message"

            else -> when {
                data["broadcastId"]?.isNotBlank() == true ||
                        data["broadcast_id"]?.isNotBlank() == true ||
                        data["coachBroadcastId"]?.isNotBlank() == true ||
                        data["coach_broadcast_id"]?.isNotBlank() == true -> {
                    "coach_broadcast"
                }

                data["roomId"]?.isNotBlank() == true ||
                        data["forumRoomId"]?.isNotBlank() == true ||
                        data["messageId"]?.isNotBlank() == true ||
                        data["forumMessageId"]?.isNotBlank() == true ||
                        data["forum_room_id"]?.isNotBlank() == true ||
                        data["forum_message_id"]?.isNotBlank() == true -> {
                    "forum_message"
                }

                else -> rawType.trim()
            }
        }

        // כותרת + טקסט – קודם מ-notification, אם אין אז מה-data, ואם אין אז ברירת מחדל
        val titleFromPayload = message.notification?.title
            ?: data["title"]

        val bodyFromPayload = message.notification?.body
            ?: data["body"]
            ?: data["text"]
            ?: data["message"]   // ✅ תמיכה נוספת

        val title = titleFromPayload ?: when (type) {
            "coach_broadcast" -> "הודעה חדשה מהמאמן"
            "forum_message" -> "הודעה חדשה בפורום"
            else -> "ק.מ.י"
        }

        val body = bodyFromPayload ?: when (type) {
            "coach_broadcast" -> "המאמן שלח הודעה חדשה."
            "forum_message" -> "נוספה הודעה חדשה בפורום."
            else -> ""
        }

        if (title.isBlank() && body.isBlank()) {
            return
        }

        showNotification(
            type = type,
            title = title,
            body = body,
            data = data
        )
    }

    private fun showNotification(
        type: String,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val context = applicationContext

        // יצירת ערוץ התראות (נדרש מ-Android 8+)
        createChannelIfNeeded()

        // Intent שייפתח בלחיצה על ההתראה
        val intent = Intent(context, MainActivity::class.java).apply {
            // ✅ פתיחה אמינה גם כשהאפליקציה סגורה, ברקע או כבר פתוחה.
            // NEW_TASK חשוב במיוחד כשההתראה נוצרת מתוך Service.
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            action = "il.kmi.app.OPEN_FROM_PUSH_${System.currentTimeMillis()}"
            setPackage(context.packageName)

            putExtra("open_from_push", true)
            putExtra("fcm_type", type)
            putExtra("type", type)
            putExtra("push_title", title)
            putExtra("push_body", body)

            // שומרים גם את כל שדות ה-data המקוריים, כדי ש-MainActivity תוכל לקלוט
            // גם אם Cloud Function שינתה שם שדה.
            data.forEach { (key, value) ->
                putExtra(key, value)
            }

            data["broadcastId"]?.let { putExtra("broadcastId", it) }
            data["branchId"]?.let { putExtra("branchId", it) }
            data["groupKey"]?.let { putExtra("groupKey", it) }

            // נתוני פורום — לשימוש עתידי בפתיחה ישירה לחדר/הודעה
            data["roomId"]?.let { putExtra("forumRoomId", it) }
            data["roomName"]?.let { putExtra("forumRoomName", it) }
            data["messageId"]?.let { putExtra("forumMessageId", it) }
            data["senderId"]?.let { putExtra("forumSenderId", it) }

            // ✅ אם זו הודעת מאמן – פותחים קודם "כרטיס הודעה" לפני מסך הכניסה
            val isCoachBroadcast =
                type == "coach_broadcast" || data["broadcastId"]?.isNotBlank() == true

            if (isCoachBroadcast) {
                putExtra(CoachGate.EXTRA_OPEN, true)

                // נעדיף ID אם קיים (להמשך עתידי)
                data["broadcastId"]?.let { putExtra(CoachGate.EXTRA_BROADCAST_ID, it) }

                // הטקסט שמוצג בכרטיס (ננסה כמה שדות נפוצים)
                val gateText =
                    data["text"]
                        ?: data["body"]
                        ?: data["message"]
                        ?: data["content"]
                        ?: body
                putExtra(CoachGate.EXTRA_TEXT, gateText)

                // מי שלח (אם נשלח)
                val gateFrom =
                    data["coachName"]
                        ?: data["coach_name"]
                        ?: data["from"]
                        ?: "המאמן"
                putExtra(CoachGate.EXTRA_FROM, gateFrom)

                // זמן (אם נשלח; אחרת נשים עכשיו)
                val sentAtMillis =
                    data["sentAt"]?.toLongOrNull()
                        ?: data["createdAt"]?.toLongOrNull()
                        ?: System.currentTimeMillis()

                putExtra(CoachGate.EXTRA_SENT_AT, sentAtMillis)
            }
        }

        val requestCode = (System.currentTimeMillis() and 0xFFFFFFF).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // אייקון ברירת מחדל של האפליקציה
            .setContentTitle(title.ifBlank { "ק.מ.י" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(requestCode, builder.build())
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "KAMI Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "התראות על הודעות מאמן והודעות פורום באפליקציית KAMI / ק.מ.י"
            }

            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "kmi_messages_channel"
    }
}
