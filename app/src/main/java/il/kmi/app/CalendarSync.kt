package il.kmi.app

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import android.text.format.DateUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone
import il.kmi.app.training.TrainingCatalog.addressFor

// בדיקת הרשאות קריאה/כתיבה ליומן
fun hasCalendarPermission(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

/**
 * סנכרון אימונים קבועים ליומן המובנה (Google Calendar כשהוא ברירת המחדל).
 * אנחנו יוצרים/מעדכנים שלושה אירועים שבועיים (SU/MO/TU) עם RRULE, ומסמנים אותם בתיאור עם TAG ייחודי.
 */
object KmiCalendarSync {
    private const val TAG_MARK = "[KMI_SYNC]"   // מזהה הייחוס שלנו בתיאור האירוע
    // ↓↓↓ חדש: אותו שם כמו במסך הכניסה
    private const val PREFS_NAME = "kmi_prefs"
    // כותרות ואורכים
    private const val TITLE_SOKOLOV = "אימון ק.מ.י – מתנ\"ס סוקולוב"
    private const val TITLE_OFEK    = "אימון ק.מ.י – מרכז אופק"
    private const val DURATION_MIN  = 90

    // BYDAY ל-RRULE
    private fun byday(day: Int) = when (day) {
        Calendar.SUNDAY    -> "SU"
        Calendar.MONDAY    -> "MO"
        Calendar.TUESDAY   -> "TU"
        Calendar.WEDNESDAY -> "WE"
        Calendar.THURSDAY  -> "TH"
        Calendar.FRIDAY    -> "FR"
        Calendar.SATURDAY  -> "SA"
        else -> "SU"
    }

    /** בוחר יומן בר־כתיבה. קודם כל מועדף שמור, אחריו Google, ואז נפילה ליומן הראשון שבר־כתיבה. */
    private fun pickWritableCalendarId(context: Context): Long? {
        val sp = context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
        val preferred = sp.getLong("calendar_sync_calendar_id", -1L).takeIf { it > 0L }
        if (preferred != null) return preferred

        val cr = context.contentResolver
        val proj = arrayOf(
            Calendars._ID,
            Calendars.ACCOUNT_TYPE,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.VISIBLE,
            Calendars.SYNC_EVENTS,
            Calendars.CALENDAR_ACCESS_LEVEL
        )
        val sel = "${Calendars.VISIBLE}=1 AND ${Calendars.SYNC_EVENTS}=1 AND ${Calendars.CALENDAR_ACCESS_LEVEL}>=200"

        var googleId: Long? = null
        var fallbackId: Long? = null
        cr.query(Calendars.CONTENT_URI, proj, sel, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val type = c.getString(1) ?: ""
                if (fallbackId == null) fallbackId = id
                if (type == "com.google") { googleId = id; break }
            }
        }
        val chosen = googleId ?: fallbackId
        if (chosen != null) {
            // נשמור להבא — כך לא “נקפוץ” ליומן שונה אם הסדר משתנה
            sp.edit().putLong("calendar_sync_calendar_id", chosen).apply()
        }
        return chosen
    }

    /** מחשב את המועד הבא ליום/שעה/דקה נתונים (כ־DTSTART של אירוע חוזר). */
    private fun nextStartUtcMillis(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val tz = TimeZone.getDefault()
        val cal = Calendar.getInstance(tz).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val now = Calendar.getInstance(tz).timeInMillis
        if (cal.timeInMillis <= now) cal.add(Calendar.WEEK_OF_YEAR, 1)
        return cal.timeInMillis
    }

      /** יוצר/מעדכן אירוע שבועי חוזר; מחזיר eventId + האם נוצר חדש. */
    private data class UpsertResult(val id: Long, val created: Boolean)

    /** יוצר/מעדכן אירוע שבועי חוזר; מחזיר UpsertResult או null. */
    private fun upsertWeeklyEvent(
        context: Context,
        calendarId: Long,
        title: String,
        location: String,
        descriptionTag: String,
        dayOfWeek: Int,
        hour: Int,
        minute: Int,
        durationMin: Int,
        leadMinutes: Int
    ): UpsertResult? {
        val cr   = context.contentResolver
        val start = nextStartUtcMillis(dayOfWeek, hour, minute)
        val rule  = "FREQ=WEEKLY;BYDAY=${byday(dayOfWeek)};WKST=SU"
        val dur   = "PT${durationMin}M"
        val tzId  = TimeZone.getDefault().id

        // 🔐 מפתח יציב לכל אירוע – ימנע התנגשות בין שני סוקולוב
        val eventKey = "KMI_WEEKLY:${byday(dayOfWeek)}_${String.format("%02d%02d", hour, minute)}:${title}"

        // חיפוש אירוע קיים לפי CUSTOM_APP_PACKAGE + CUSTOM_APP_URI (יציב ובטוח)
        val proj = arrayOf(Events._ID)
        val sel  = "${Events.CUSTOM_APP_PACKAGE}=? AND ${Events.CUSTOM_APP_URI}=?"
        val args = arrayOf(context.packageName, eventKey)

        var existingId: Long? = null
        cr.query(Events.CONTENT_URI, proj, sel, args, null)?.use { cur ->
            if (cur.moveToFirst()) existingId = cur.getLong(0)
        }

        val values = ContentValues().apply {
            put(Events.CALENDAR_ID, calendarId)
            put(Events.TITLE, title)
            put(Events.DESCRIPTION, "$descriptionTag ${DateUtils.formatDateTime(context, start, DateUtils.FORMAT_SHOW_DATE)}")
            put(Events.EVENT_TIMEZONE, tzId)
            put(Events.DTSTART, start)
            put(Events.DURATION, dur)
            put(Events.RRULE, rule)
            put(Events.EVENT_LOCATION, location)
            // ✅ סימון האירוע כשייך לאפליקציה
            put(Events.CUSTOM_APP_PACKAGE, context.packageName)
            put(Events.CUSTOM_APP_URI, eventKey)
        }

        val (eventId, created) = if (existingId == null) {
            val uri = cr.insert(Events.CONTENT_URI, values) ?: return null
            ContentUris.parseId(uri) to true
        } else {
            val uri = ContentUris.withAppendedId(Events.CONTENT_URI, existingId!!)
            cr.update(uri, values, null, null)
            existingId!! to false
        }

        // Reminder: מוחקים קודמים ויוצרים חדש לפי leadMinutes
        val remSel = "${Reminders.EVENT_ID}=?"
        val remArg = arrayOf(eventId.toString())
        cr.delete(Reminders.CONTENT_URI, remSel, remArg)

        val safeLead = leadMinutes.coerceIn(0, 180)
        val rem = ContentValues().apply {
            put(Reminders.EVENT_ID, eventId)
            put(Reminders.MINUTES, safeLead)
            put(Reminders.METHOD, Reminders.METHOD_ALERT)
        }
        cr.insert(Reminders.CONTENT_URI, rem)

        return UpsertResult(eventId, created)
    }

    /** יוצר/מעדכן את שלושת האימונים הקבועים. */
    fun upsertAll(context: Context) {
        // הרשאות?
        if (!hasCalendarPermission(context)) {
            Toast.makeText(context, "אין הרשאה ליומן", Toast.LENGTH_LONG).show()
            return
        }

        val calId = pickWritableCalendarId(context)
        if (calId == null) {
            Toast.makeText(context, "לא נמצא יומן לעריכה", Toast.LENGTH_LONG).show()
            return
        }

        // זמן התראה מההגדרות (ברירת מחדל 60)
        val sp = context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
        val leadMin = sp.getInt("lead_minutes", 60).coerceIn(0, 180)

        // נתונים קבועים – משתמש בקבועי הכתובות שלך
        // א' 20:30–22:00 סוקולוב
        val r1 = upsertWeeklyEvent(
            context, calId,
            title = TITLE_SOKOLOV,
            location = addressFor("נתניה – מרכז קהילתי סוקולוב"),
            descriptionTag = TAG_MARK,
            dayOfWeek = Calendar.SUNDAY,
            hour = 20, minute = 30,
            durationMin = DURATION_MIN,
            leadMinutes = leadMin
        )
        // ב' 19:00–20:30 אופק
        val r2 = upsertWeeklyEvent(
            context, calId,
            title = TITLE_OFEK,
            location = addressFor("נתניה – מרכז קהילתי אופק"),
            descriptionTag = TAG_MARK,
            dayOfWeek = Calendar.MONDAY,
            hour = 19, minute = 0,
            durationMin = DURATION_MIN,
            leadMinutes = leadMin
        )
        // ג' 20:30–22:00 סוקולוב
        val r3 = upsertWeeklyEvent(
            context, calId,
            title = TITLE_SOKOLOV,
            location = addressFor("נתניה – מרכז קהילתי סוקולוב"),
            descriptionTag = TAG_MARK,
            dayOfWeek = Calendar.TUESDAY,
            hour = 20, minute = 30,
            durationMin = DURATION_MIN,
            leadMinutes = leadMin
        )

        // ❌ לא מסמנים שום דגל למסך הכניסה ולא מציגים Toast כאן.
        //    אם תרצה משוב, תציג אותו במסך ההגדרות בעת הלחיצה על "אישור".
    }   // ←← סוגר fun upsertAll(context: Context)

    private fun logAvailableCalendars(context: Context) {
        val cr = context.contentResolver
        val proj = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.VISIBLE,
            Calendars.SYNC_EVENTS
        )
        cr.query(Calendars.CONTENT_URI, proj, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id   = c.getLong(0)
                val name = c.getString(1)
                val acc  = c.getString(2)
                val type = c.getString(3)
                val vis  = c.getInt(4)
                val sync = c.getInt(5)
                android.util.Log.d(
                    "KMI_CAL",
                    "cal id=$id, name=$name, account=$acc, type=$type, visible=$vis, sync=$sync"
                )
            }
        }
    }

    /** מוחק מהיומן את כל האירועים שסונכרנו ע"י האפליקציה (בטוח לפי CUSTOM_APP_*). */
    fun removeAll(context: Context) {
        if (!hasCalendarPermission(context)) {
            Toast.makeText(context, "אין הרשאה למחיקה מהיומן", Toast.LENGTH_LONG).show()
            return
        }
        val cr = context.contentResolver
        val proj = arrayOf(Events._ID)
        val sel  = "${Events.CUSTOM_APP_PACKAGE}=?"
        val args = arrayOf(context.packageName)

        cr.query(Events.CONTENT_URI, proj, sel, args, null)?.use { cur ->
            while (cur.moveToNext()) {
                val id = cur.getLong(0)
                val uri = ContentUris.withAppendedId(Events.CONTENT_URI, id)
                cr.delete(uri, null, null)
            }
        }
        Toast.makeText(context, "האימונים הוסרו מהיומן", Toast.LENGTH_SHORT).show()
    }
}
