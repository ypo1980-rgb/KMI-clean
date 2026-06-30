package il.kmi.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import il.kmi.app.training.TrainingCatalog
import org.json.JSONArray
import java.util.Calendar
import kotlin.math.abs

object TrainingReminderScheduler {

    private const val PREFS_NAME = "kmi_training_reminders"
    private const val KEY_REQUEST_CODES = "training_reminder_request_codes"
    private const val ACTION_TRAINING_REMINDER = "il.kmi.app.ACTION_TRAINING_REMINDER"

    fun scheduleWeeklyTrainingAlarms(
        context: Context,
        leadMinutes: Int
    ) {
        val appContext = context.applicationContext

        cancelWeeklyTrainingAlarms(appContext)

        val branches = resolveUserBranches(appContext)
        val groups = resolveUserGroups(appContext)

        if (branches.isEmpty()) {
            return
        }

        val safeLeadMinutes = leadMinutes.takeIf { it > 0 } ?: 60
        val scheduledRequestCodes = linkedSetOf<Int>()

        branches.forEach { branch ->
            val groupsForBranch = groups.ifEmpty { listOf("") }

            groupsForBranch.forEach { group ->
                val trainings = TrainingCatalog.trainingsFor(
                    branch = branch,
                    group = group.ifBlank { null },
                    isEnglish = false
                )

                trainings.forEach { training ->
                    val startCal = (training.cal.clone() as Calendar).apply {
                        while (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 7)
                        }
                    }

                    val triggerAtMillis = (startCal.clone() as Calendar).apply {
                        add(Calendar.MINUTE, -safeLeadMinutes)

                        while (timeInMillis <= System.currentTimeMillis() + 60_000L) {
                            add(Calendar.DAY_OF_YEAR, 7)
                            startCal.add(Calendar.DAY_OF_YEAR, 7)
                        }
                    }.timeInMillis

                    val requestCode = stableRequestCode(
                        branch = training.branch.ifBlank { branch },
                        group = group,
                        dayOfWeek = startCal.get(Calendar.DAY_OF_WEEK),
                        hour = startCal.get(Calendar.HOUR_OF_DAY),
                        minute = startCal.get(Calendar.MINUTE)
                    )

                    scheduleOneTrainingAlarm(
                        context = appContext,
                        requestCode = requestCode,
                        triggerAtMillis = triggerAtMillis,
                        branch = training.branch.ifBlank { branch },
                        group = group,
                        place = training.place,
                        coach = training.coach,
                        startMillis = startCal.timeInMillis,
                        leadMinutes = safeLeadMinutes
                    )

                    scheduledRequestCodes += requestCode
                }
            }
        }

        saveRequestCodes(appContext, scheduledRequestCodes)
    }

    fun cancelWeeklyTrainingAlarms(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCodes = loadRequestCodes(appContext)

        requestCodes.forEach { requestCode ->
            val pendingIntent = reminderPendingIntent(
                context = appContext,
                requestCode = requestCode,
                branch = "",
                group = "",
                place = "",
                coach = "",
                startMillis = 0L,
                leadMinutes = 0
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_REQUEST_CODES)
            .apply()
    }

    private fun scheduleOneTrainingAlarm(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        branch: String,
        group: String,
        place: String,
        coach: String,
        startMillis: Long,
        leadMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pendingIntent = reminderPendingIntent(
            context = context,
            requestCode = requestCode,
            branch = branch,
            group = group,
            place = place,
            coach = coach,
            startMillis = startMillis,
            leadMinutes = leadMinutes
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun reminderPendingIntent(
        context: Context,
        requestCode: Int,
        branch: String,
        group: String,
        place: String,
        coach: String,
        startMillis: Long,
        leadMinutes: Int
    ): PendingIntent {
        val intent = Intent(context, TrainingReminderReceiver::class.java).apply {
            action = ACTION_TRAINING_REMINDER
            putExtra(TrainingReminderReceiver.EXTRA_BRANCH, branch)
            putExtra(TrainingReminderReceiver.EXTRA_GROUP, group)
            putExtra(TrainingReminderReceiver.EXTRA_PLACE, place)
            putExtra(TrainingReminderReceiver.EXTRA_COACH, coach)
            putExtra(TrainingReminderReceiver.EXTRA_START_MILLIS, startMillis)
            putExtra(TrainingReminderReceiver.EXTRA_LEAD_MINUTES, leadMinutes)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun resolveUserBranches(context: Context): List<String> {
        val userSp = context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
        val settingsSp = context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)

        return splitPrefsValues(
            userSp.getString("branches_json", null),
            userSp.getString("selected_branches", null),
            userSp.getString("branches", null),
            userSp.getString("activeBranch", null),
            userSp.getString("active_branch", null),
            userSp.getString("branch", null),
            settingsSp.getString("branches_json", null),
            settingsSp.getString("selected_branches", null),
            settingsSp.getString("branches", null),
            settingsSp.getString("activeBranch", null),
            settingsSp.getString("active_branch", null),
            settingsSp.getString("branch", null)
        )
    }

    private fun resolveUserGroups(context: Context): List<String> {
        val userSp = context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
        val settingsSp = context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)

        return splitPrefsValues(
            userSp.getString("groups_json", null),
            userSp.getString("selected_groups", null),
            userSp.getString("groups", null),
            userSp.getString("age_groups", null),
            userSp.getString("activeGroup", null),
            userSp.getString("active_group", null),
            userSp.getString("groupKey", null),
            userSp.getString("group_key", null),
            userSp.getString("age_group", null),
            userSp.getString("group", null),
            settingsSp.getString("groups_json", null),
            settingsSp.getString("selected_groups", null),
            settingsSp.getString("groups", null),
            settingsSp.getString("age_groups", null),
            settingsSp.getString("activeGroup", null),
            settingsSp.getString("active_group", null),
            settingsSp.getString("groupKey", null),
            settingsSp.getString("group_key", null),
            settingsSp.getString("age_group", null),
            settingsSp.getString("group", null)
        ).map {
            TrainingCatalog.normalizeGroupName(it).ifBlank { it }
        }.distinct()
    }

    private fun splitPrefsValues(vararg values: String?): List<String> {
        val out = linkedSetOf<String>()

        values.forEach { value ->
            val raw = value.orEmpty().trim()

            if (raw.isBlank()) {
                return@forEach
            }

            if (raw.startsWith("[")) {
                runCatching {
                    val array = JSONArray(raw)

                    for (index in 0 until array.length()) {
                        array.optString(index)
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

        return out.toList()
    }

    private fun stableRequestCode(
        branch: String,
        group: String,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ): Int {
        val raw = "$branch|$group|$dayOfWeek|$hour|$minute"
        return 42000 + abs(raw.hashCode() % 50_000)
    }

    private fun saveRequestCodes(
        context: Context,
        requestCodes: Set<Int>
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(
                KEY_REQUEST_CODES,
                requestCodes.joinToString(",")
            )
            .apply()
    }

    private fun loadRequestCodes(context: Context): List<Int> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REQUEST_CODES, "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
    }
}