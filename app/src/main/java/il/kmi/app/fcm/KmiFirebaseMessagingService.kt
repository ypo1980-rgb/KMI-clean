package il.kmi.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
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
        Log.d(TAG, "onNewToken: $token")

        // שמירת ה-token החדש במסמך המשתמש (דרך ה־manager הקיים באפליקציה)
        try {
            FcmTokenManager.refreshTokenForCurrentUser(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refreshTokenForCurrentUser from onNewToken", e)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "onMessageReceived from=${message.from}, data=${message.data}")

        val data = message.data
        val type = data["type"] ?: ""

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
            Log.d(TAG, "Empty FCM message (no title/body), skipping notification")
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
            // ✅ חשוב: כדי ש-onNewIntent יתפוס כשהאפליקציה פתוחה/ברקע
            // וגם כדי לא ליצור Activity כפולה
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra("fcm_type", type)
            data["broadcastId"]?.let { putExtra("broadcastId", it) }
            data["branchId"]?.let { putExtra("branchId", it) }
            data["groupKey"]?.let { putExtra("groupKey", it) }

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

        with(NotificationManagerCompat.from(context)) {
            notify(requestCode, builder.build())
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
                "KMI Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "התראות על הודעות מאמן והודעות פורום באפליקציית ק.מ.י"
            }

            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "KmiFirebaseMsgService"
        const val CHANNEL_ID = "kmi_messages_channel"
    }
}
