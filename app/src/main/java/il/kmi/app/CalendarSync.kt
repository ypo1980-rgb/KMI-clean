package il.kmi.app

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import android.text.format.DateUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone
import il.kmi.app.database.KmiDatabaseProvider
import il.kmi.app.training.TrainingCatalog
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

// בדיקת הרשאות קריאה/כתיבה ליומן
fun hasCalendarPermission(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

/**
 * סנכרון אימונים קבועים ליומן המובנה (Google Calendar כשהוא ברירת המחדל).
 * אנחנו יוצרים/מעדכנים שלושה אירועים שבועיים (SU/MO/TU) עם RRULE, ומסמנים אותם בתיאור עם TAG ייחודי.
 */
object KmiCalendarSync {
    private const val TAG_MARK = "[KMI_SYNC]"   // מזהה פנימי לאירועי סנכרון. לא מוצג למשתמש.
    private const val TAG_MARK_SELECTED = "[KMI_SYNC_SELECTED]" // מזהה פנימי לאירועי יומן שנבחר.

    private fun tr(context: Context, he: String, en: String): String {
        return if (AppLanguageManager(context).getCurrentLanguage() == AppLanguage.ENGLISH) en else he
    }

    data class DeviceCalendar(
        val id: Long,
        val displayName: String,
        val accountName: String,
        val accountType: String
    )

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

    private data class CalendarTrainingCandidate(
        val title: String,
        val location: String,
        val dayOfWeek: Int,
        val hour: Int,
        val minute: Int,
        val durationMinutes: Int
    )

    private fun prefsList(
        sp: SharedPreferences,
        vararg keys: String
    ): List<String> {
        val out = mutableListOf<String>()

        keys.forEach { key ->
            when (val value = sp.all[key]) {
                is String -> {
                    val raw = value.trim()

                    if (raw.isBlank()) {
                        // no-op
                    } else if (raw.startsWith("[")) {
                        runCatching {
                            val arr = org.json.JSONArray(raw)
                            for (i in 0 until arr.length()) {
                                arr.optString(i)
                                    .trim()
                                    .takeIf { it.isNotBlank() }
                                    ?.let { out += it }
                            }
                        }
                    } else {
                        raw.split(',', ';', '|', '\n')
                            .map { it.trim().trim('"') }
                            .filter { it.isNotBlank() }
                            .forEach { out += it }
                    }
                }

                is Set<*> -> {
                    value
                        .mapNotNull { it?.toString()?.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { out += it }
                }

                is List<*> -> {
                    value
                        .mapNotNull { it?.toString()?.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { out += it }
                }
            }
        }

        return out
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun selectedBranchesForCalendarSync(context: Context): List<String> {
        val sp = context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

        return prefsList(
            sp,
            "branches_json",
            "selected_branches",
            "branches",
            "branch",
            "active_branch",
            "activeBranch",
            "branch2",
            "branch3"
        )
    }

    private fun selectedGroupsForCalendarSync(context: Context): List<String> {
        val sp = context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

        return prefsList(
            sp,
            "groups_json",
            "selected_groups",
            "groups",
            "age_groups",
            "age_group",
            "active_group",
            "activeGroup",
            "group"
        )
            .map {
                TrainingCatalog
                    .normalizeGroupName(it)
                    .ifBlank { it }
            }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun calendarDayFromDatabase(dayOfWeek: String): Int {
        return when (dayOfWeek.trim().uppercase(java.util.Locale.US)) {
            "SUNDAY" -> Calendar.SUNDAY
            "MONDAY" -> Calendar.MONDAY
            "TUESDAY" -> Calendar.TUESDAY
            "WEDNESDAY" -> Calendar.WEDNESDAY
            "THURSDAY" -> Calendar.THURSDAY
            "FRIDAY" -> Calendar.FRIDAY
            "SATURDAY" -> Calendar.SATURDAY
            else -> -1
        }
    }

    private fun hourFromTimeText(time: String): Int? {
        return time
            .substringBefore(":")
            .trim()
            .toIntOrNull()
            ?.takeIf { it in 0..23 }
    }

    private fun minuteFromTimeText(time: String): Int? {
        return time
            .substringAfter(":", "")
            .trim()
            .toIntOrNull()
            ?.takeIf { it in 0..59 }
    }

    private fun databaseGroupMatches(
        selectedGroup: String,
        databaseGroupHe: String,
        databaseGroupEn: String
    ): Boolean {
        val wanted = TrainingCatalog
            .normalizeGroupName(selectedGroup)
            .ifBlank { selectedGroup }
            .trim()

        val dbHe = TrainingCatalog
            .normalizeGroupName(databaseGroupHe)
            .ifBlank { databaseGroupHe }
            .trim()

        val dbEn = databaseGroupEn.trim()

        if (wanted.equals(dbHe, ignoreCase = true)) return true
        if (selectedGroup.trim().equals(databaseGroupHe.trim(), ignoreCase = true)) return true
        if (selectedGroup.trim().equals(dbEn, ignoreCase = true)) return true

        if (wanted == "נוער" && dbHe == "נוער + בוגרים") return true
        if (wanted == "בוגרים" && dbHe == "נוער + בוגרים") return true

        return false
    }

    private fun calendarTrainingCandidatesForSync(
        context: Context
    ): List<CalendarTrainingCandidate> {
        val isEnglish = AppLanguageManager(context).getCurrentLanguage() == AppLanguage.ENGLISH

        val branches = selectedBranchesForCalendarSync(context)
        val groups = selectedGroupsForCalendarSync(context)

        if (branches.isEmpty() || groups.isEmpty()) {
            return emptyList()
        }

        return branches.flatMap { branchName ->
            val dbBranch = KmiDatabaseProvider.branchByName(context, branchName)
                ?: return@flatMap emptyList()

            groups.flatMap { groupName ->
                dbBranch.trainingDays
                    .filter { day ->
                        databaseGroupMatches(
                            selectedGroup = groupName,
                            databaseGroupHe = day.groupHe,
                            databaseGroupEn = day.groupEn
                        )
                    }
                    .mapNotNull { day ->
                        val calendarDay = calendarDayFromDatabase(day.dayOfWeek)
                        if (calendarDay == -1) return@mapNotNull null

                        val hour = hourFromTimeText(day.startTime) ?: return@mapNotNull null
                        val minute = minuteFromTimeText(day.startTime) ?: return@mapNotNull null
                        val duration = day.durationMinutes.takeIf { it > 0 } ?: return@mapNotNull null

                        val place = dbBranch
                            .displayPlace(isEnglish)
                            .ifBlank { branchName }

                        val location = dbBranch
                            .displayAddress(isEnglish)
                            .ifBlank { place }

                        CalendarTrainingCandidate(
                            title = if (isEnglish) {
                                "KMI Training – $place"
                            } else {
                                "אימון ק.מ.י – $place"
                            },
                            location = location,
                            dayOfWeek = calendarDay,
                            hour = hour,
                            minute = minute,
                            durationMinutes = duration
                        )
                    }
            }
        }
            .distinctBy {
                "${it.title}|${it.location}|${it.dayOfWeek}|${it.hour}|${it.minute}|${it.durationMinutes}"
            }
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

    fun listWritableCalendars(context: Context): List<DeviceCalendar> {
        val out = mutableListOf<DeviceCalendar>()
        val cr = context.contentResolver
        val proj = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.VISIBLE,
            Calendars.SYNC_EVENTS,
            Calendars.CALENDAR_ACCESS_LEVEL
        )
        val sel = "${Calendars.VISIBLE}=1 AND ${Calendars.SYNC_EVENTS}=1 AND ${Calendars.CALENDAR_ACCESS_LEVEL}>=200"
        cr.query(Calendars.CONTENT_URI, proj, sel, null, "${Calendars.CALENDAR_DISPLAY_NAME} COLLATE NOCASE ASC")?.use { c ->
            while (c.moveToNext()) {
                out += DeviceCalendar(
                    id = c.getLong(0),
                    displayName = c.getString(1).orEmpty(),
                    accountName = c.getString(2).orEmpty(),
                    accountType = c.getString(3).orEmpty()
                )
            }
        }
        return out
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
        eventKeyPrefix: String,
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
        val eventKey = "$eventKeyPrefix:${byday(dayOfWeek)}_${String.format("%02d%02d", hour, minute)}:${title}"

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

    private fun upsertAllToCalendar(
        context: Context,
        calendarId: Long,
        descriptionTag: String,
        eventKeyPrefix: String
    ): Boolean {
        val sp = context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
        val leadMin = sp.getInt("lead_minutes", 60).coerceIn(0, 180)

        val candidates = calendarTrainingCandidatesForSync(context)

        if (candidates.isEmpty()) {
            Toast.makeText(
                context,
                tr(
                    context,
                    "לא נמצאו אימונים לסניפים ולקבוצות הרשומים בפרופיל",
                    "No trainings were found for the branches and groups in the profile"
                ),
                Toast.LENGTH_LONG
            ).show()

            return false
        }

        candidates.forEach { candidate ->
            upsertWeeklyEvent(
                context = context,
                calendarId = calendarId,
                title = candidate.title,
                location = candidate.location,
                descriptionTag = descriptionTag,
                eventKeyPrefix = eventKeyPrefix,
                dayOfWeek = candidate.dayOfWeek,
                hour = candidate.hour,
                minute = candidate.minute,
                durationMin = candidate.durationMinutes,
                leadMinutes = leadMin
            )
        }

        return true
    }

    /** יוצר/מעדכן את כל האימונים לפי הסניפים והקבוצות הרשומים בפרופיל. */
    fun upsertAll(context: Context) {
        // הרשאות?
        if (!hasCalendarPermission(context)) {
            Toast.makeText(
                context,
                tr(context, "אין הרשאה ליומן", "No calendar permission"),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val calId = pickWritableCalendarId(context)
        if (calId == null) {
            Toast.makeText(
                context,
                tr(context, "לא נמצא יומן לעריכה", "No writable calendar was found"),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        upsertAllToCalendar(
            context = context,
            calendarId = calId,
            descriptionTag = TAG_MARK,
            eventKeyPrefix = "KMI_WEEKLY"
        )

        // ❌ לא מסמנים שום דגל למסך הכניסה ולא מציגים Toast כאן.
        //    אם תרצה משוב, תציג אותו במסך ההגדרות בעת הלחיצה על "אישור".
    }   // ←← סוגר fun upsertAll(context: Context)

    fun upsertAllToSelectedCalendar(context: Context, selectedCalendarId: Long): Boolean {
        if (!hasCalendarPermission(context)) {
            Toast.makeText(
                context,
                tr(context, "אין הרשאה ליומן", "No calendar permission"),
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        val valid = listWritableCalendars(context).any { it.id == selectedCalendarId }
        if (!valid) {
            Toast.makeText(
                context,
                tr(context, "היומן שנבחר אינו זמין לכתיבה", "The selected calendar is not writable"),
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        return upsertAllToCalendar(
            context = context,
            calendarId = selectedCalendarId,
            descriptionTag = TAG_MARK_SELECTED,
            eventKeyPrefix = "KMI_SELECTED_WEEKLY"
        )
    }

    /** מוחק מהיומן את כל האירועים שסונכרנו ע"י האפליקציה (בטוח לפי CUSTOM_APP_*). */
    fun removeAll(context: Context) {
        if (!hasCalendarPermission(context)) {
            Toast.makeText(
                context,
                tr(context, "אין הרשאה למחיקה מהיומן", "No permission to delete calendar events"),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val cr = context.contentResolver
        val proj = arrayOf(Events._ID)
        val sel  = "${Events.CUSTOM_APP_PACKAGE}=? AND ${Events.CUSTOM_APP_URI} LIKE ?"
        val args = arrayOf(context.packageName, "KMI_WEEKLY:%")

        cr.query(Events.CONTENT_URI, proj, sel, args, null)?.use { cur ->
            while (cur.moveToNext()) {
                val id = cur.getLong(0)
                val uri = ContentUris.withAppendedId(Events.CONTENT_URI, id)
                cr.delete(uri, null, null)
            }
        }
        Toast.makeText(
            context,
            tr(context, "האימונים הוסרו מהיומן", "Training events were removed from the calendar"),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun removeSelectedCalendarEvents(context: Context) {
        if (!hasCalendarPermission(context)) {
            Toast.makeText(
                context,
                tr(context, "אין הרשאה למחיקה מהיומן", "No permission to delete calendar events"),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val cr = context.contentResolver
        val proj = arrayOf(Events._ID)
        val sel = "${Events.CUSTOM_APP_PACKAGE}=? AND ${Events.CUSTOM_APP_URI} LIKE ?"
        val args = arrayOf(context.packageName, "KMI_SELECTED_WEEKLY:%")

        cr.query(Events.CONTENT_URI, proj, sel, args, null)?.use { cur ->
            while (cur.moveToNext()) {
                val id = cur.getLong(0)
                val uri = ContentUris.withAppendedId(Events.CONTENT_URI, id)
                cr.delete(uri, null, null)
            }
        }
    }
}
