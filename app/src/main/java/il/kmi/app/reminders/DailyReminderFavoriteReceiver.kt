package il.kmi.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import il.kmi.app.favorites.FavoritesStore

class DailyReminderFavoriteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val item = intent.getStringExtra("daily_reminder_item") ?: return

        FavoritesStore.toggle(item)
    }
}