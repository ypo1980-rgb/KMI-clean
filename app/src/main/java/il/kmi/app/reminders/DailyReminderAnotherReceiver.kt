package il.kmi.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyReminderAnotherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        val openCardIntent = Intent(context, DailyReminderCardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(openCardIntent)
    }
}