package il.kmi.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import il.kmi.app.screens.registration.CoachBranchAssignmentsCodec
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.training.TrainingStatusEngine
import org.json.JSONArray
import java.util.Calendar

object TrainingReminderScheduler {

    private const val PREFS_NAME = "kmi_training_reminders"
    private const val KEY_REQUEST_CODES = "training_reminder_request_codes"
    private const val ACTION_TRAINING_REMINDER = "il.kmi.app.ACTION_TRAINING_REMINDER"

    @Synchronized
    fun scheduleWeeklyTrainingAlarms(
        context: Context,
        leadMinutes: Int
    ) {
        val appContext = context.applicationContext

        /*
         * שומרים את המזהים הקיימים.
         * לא מבטלים אותם לפני בניית התזמון החדש.
         */
        val previousRequestCodes =
            loadRequestCodes(appContext).toSet()

        val userPreferences = appContext.getSharedPreferences(
            "kmi_user",
            Context.MODE_PRIVATE
        )

        val coachAssignments =
            CoachBranchAssignmentsCodec.decode(
                userPreferences.getString(
                    "coach_branch_assignments_json",
                    null
                )
            )

        /*
         * כאשר קיימים שיוכי מאמן, כל סניף מקבל
         * רק את הקבוצות המשויכות אליו.
         *
         * בהיעדר שיוכים שומרים על מסלול התאימות
         * הקיים למתאמנים ולפרופילים ישנים.
         */
        val branchGroups: Map<String, List<String>> =
            if (coachAssignments.isNotEmpty()) {
                coachAssignments
                    .map { it.sanitized() }
                    .filter { it.branch.isNotBlank() }
                    .groupBy(
                        keySelector = { it.branch },
                        valueTransform = { it.groups }
                    )
                    .mapValues { (_, groupLists) ->
                        groupLists.flatten().distinct()
                    }
            } else {
                val branches = resolveUserBranches(appContext)
                val groups = resolveUserGroups(appContext)

                branches.associateWith { groups }
            }

        if (branchGroups.values.none { it.isNotEmpty() }) {
            cancelWeeklyTrainingAlarms(appContext)
            return
        }

        val safeLeadMinutes =
            leadMinutes.takeIf {
                it in 0..180
            } ?: 60
        val scheduledRequestCodes = linkedSetOf<Int>()
        val scheduledTrainingKeys = linkedSetOf<String>()

        try {
            branchGroups.forEach { (branch, groupsForBranch) ->
                groupsForBranch.forEach { group ->
                    val trainings = TrainingCatalog.trainingsFor(
                        branch = branch,
                        group = group,
                        isEnglish = false
                    )

                trainings.forEach { training ->
                    val nowMillis =
                        System.currentTimeMillis()

                    val startCal =
                        (training.cal.clone() as Calendar).apply {
                            while (timeInMillis <= nowMillis) {
                                add(Calendar.DAY_OF_YEAR, 7)
                            }
                        }

                    /*
                     * משך האימון נשמר גם כאשר מזיזים את
                     * המופע לשבוע הבא.
                     */
                    val durationMillis =
                        training.endMillis
                            ?.minus(training.startMillis)
                            ?.takeIf { duration ->
                                duration > 0L
                            }

                    val triggerCal =
                        (startCal.clone() as Calendar).apply {
                            add(
                                Calendar.MINUTE,
                                -safeLeadMinutes
                            )
                        }

                    /*
                     * מאתרים את המופע החוקי הבא.
                     * אם השבוע הקרוב מבוטל בחג, ממשיכים
                     * אוטומטית לשבוע הבא.
                     */
                    var resolvedTriggerMillis: Long? = null
                    var resolvedStartMillis: Long? = null
                    var resolvedEndMillis: Long? = null

                    var remainingAttempts = 104

                    while (remainingAttempts > 0) {
                        remainingAttempts--

                        while (
                            triggerCal.timeInMillis <=
                            nowMillis + 60_000L
                        ) {
                            triggerCal.add(
                                Calendar.DAY_OF_YEAR,
                                7
                            )
                            startCal.add(
                                Calendar.DAY_OF_YEAR,
                                7
                            )
                        }

                        val candidateStartMillis =
                            startCal.timeInMillis

                        val candidateEndMillis =
                            durationMillis?.let { duration ->
                                candidateStartMillis +
                                        duration
                            }

                        val status =
                            TrainingStatusEngine.evaluate(
                                context = appContext,
                                trainingStartMillis =
                                    candidateStartMillis,
                                trainingEndMillis =
                                    candidateEndMillis,
                                nowMillis = nowMillis
                            )

                        if (status.shouldNotify) {
                            resolvedTriggerMillis =
                                triggerCal.timeInMillis
                            resolvedStartMillis =
                                candidateStartMillis
                            resolvedEndMillis =
                                candidateEndMillis
                            break
                        }

                        /*
                         * המופע אינו רשאי לשלוח התראה,
                         * לדוגמה עקב חג. עוברים שבוע קדימה.
                         */
                        triggerCal.add(
                            Calendar.DAY_OF_YEAR,
                            7
                        )
                        startCal.add(
                            Calendar.DAY_OF_YEAR,
                            7
                        )
                    }

                    val triggerAtMillis =
                        resolvedTriggerMillis
                            ?: return@forEach

                    val finalStartMillis =
                        resolvedStartMillis
                            ?: return@forEach

                    val realBranch =
                        training.branch.ifBlank {
                            branch
                        }

                    val realGroup =
                        group.trim()

                    val realPlace =
                        training.place.trim()

                    val realCoach =
                        training.coach.trim()

                    /*
                     * אותו אימון יכול להימצא באמצעות יותר מקבוצה אחת,
                     * למשל "בוגרים" וגם "נוער + בוגרים".
                     *
                     * הקבוצה אינה חלק מזהות האימון. הזהות נקבעת
                     * לפי מועד ההתחלה והמקום בלבד.
                     */
                    val trainingPlaceIdentity =
                        realPlace
                            .ifBlank { realBranch }
                            .trim()
                            .lowercase()
                            .replace("–", "-")
                            .replace("—", "-")
                            .replace(Regex("\\s+"), " ")

                    val trainingStartMinute =
                        finalStartMillis / 60_000L

                    val trainingKey =
                        "$trainingStartMinute|" +
                                trainingPlaceIdentity

                    if (
                        !scheduledTrainingKeys.add(
                            trainingKey
                        )
                    ) {
                        return@forEach
                    }

                    val requestCode =
                        stableRequestCode(
                            place = trainingPlaceIdentity,
                            startMinute =
                                trainingStartMinute
                        )

                    /*
                     * שומרים את המזהה לפני ניסיון התזמון,
                     * כדי שניתן יהיה לנקות אותו גם אם
                     * הפעולה תיכשל באמצע.
                     */
                    scheduledRequestCodes += requestCode

                    scheduleOneTrainingAlarm(
                        context = appContext,
                        requestCode = requestCode,
                        triggerAtMillis =
                            triggerAtMillis,
                        branch = realBranch,
                        group = realGroup,
                        place = realPlace,
                        coach = realCoach,
                        startMillis =
                            finalStartMillis,
                        endMillis =
                            resolvedEndMillis,
                        leadMinutes =
                            safeLeadMinutes
                    )
                }
                }
            }
        } catch (error: Exception) {
            /*
             * אם חלק מההתראות כבר נוצרו,
             * שומרים גם את המזהים החדשים וגם
             * את הקודמים, כדי שנוכל לבטל אותם
             * בפעולת התזמון הבאה.
             */
            saveRequestCodes(
                context = appContext,
                requestCodes =
                    previousRequestCodes + scheduledRequestCodes
            )

            throw error
        }

        /*
         * רק לאחר יצירת התזמונים החדשים
         * מבטלים מזהים שלא קיימים עוד.
         */
        val obsoleteRequestCodes =
            previousRequestCodes - scheduledRequestCodes

        cancelRequestCodes(
            context = appContext,
            requestCodes = obsoleteRequestCodes
        )

        saveRequestCodes(
            context = appContext,
            requestCodes = scheduledRequestCodes
        )
    }

    @Synchronized
    fun cancelWeeklyTrainingAlarms(context: Context) {
        val appContext = context.applicationContext

        cancelRequestCodes(
            context = appContext,
            requestCodes = loadRequestCodes(appContext)
        )

        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove(KEY_REQUEST_CODES)
            .apply()
    }

    private fun cancelRequestCodes(
        context: Context,
        requestCodes: Collection<Int>
    ) {
        if (requestCodes.isEmpty()) return

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        requestCodes.forEach { requestCode ->
            val intent = Intent(
                context,
                TrainingReminderReceiver::class.java
            ).apply {
                action = ACTION_TRAINING_REMINDER
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
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
        endMillis: Long?,
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
            endMillis = endMillis,
            leadMinutes = leadMinutes
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canScheduleExact =
                alarmManager.canScheduleExactAlarms()

            if (canScheduleExact) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } catch (_: SecurityException) {
                    // ההרשאה עשויה להשתנות לאחר הבדיקה.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
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
        endMillis: Long?,
        leadMinutes: Int
    ): PendingIntent {
        val intent = Intent(context, TrainingReminderReceiver::class.java).apply {
            action = ACTION_TRAINING_REMINDER
            putExtra(TrainingReminderReceiver.EXTRA_BRANCH, branch)
            putExtra(TrainingReminderReceiver.EXTRA_GROUP, group)
            putExtra(TrainingReminderReceiver.EXTRA_PLACE, place)
            putExtra(
                TrainingReminderReceiver.EXTRA_COACH,
                coach
            )
            putExtra(
                TrainingReminderReceiver.EXTRA_START_MILLIS,
                startMillis
            )
            putExtra(
                TrainingReminderReceiver.EXTRA_END_MILLIS,
                endMillis ?: 0L
            )
            putExtra(
                TrainingReminderReceiver.EXTRA_LEAD_MINUTES,
                leadMinutes
            )
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun resolveUserBranches(context: Context): List<String> {
        val userSp = context.getSharedPreferences(
            "kmi_user",
            Context.MODE_PRIVATE
        )
        val settingsSp = context.getSharedPreferences(
            "kmi_settings",
            Context.MODE_PRIVATE
        )

        /*
         * "branch" הוא הערך הקנוני שנכתב בעת רישום או רענון
         * הפרופיל. אם הוא קיים, לא מאחדים איתו מפתחות ישנים.
         */
        val currentUserBranches = splitPrefsValues(
            userSp.getString("branch", null)
        )

        if (currentUserBranches.isNotEmpty()) {
            return currentUserBranches
        }

        val currentSettingsBranches = splitPrefsValues(
            settingsSp.getString("branch", null)
        )

        if (currentSettingsBranches.isNotEmpty()) {
            return currentSettingsBranches
        }

        /*
         * תאימות למשתמשים מגרסאות ישנות שבהן "branch"
         * עדיין לא נשמר.
         */
        return splitPrefsValues(
            userSp.getString("branches_json", null),
            userSp.getString("selected_branches", null),
            userSp.getString("branches", null),
            userSp.getString("active_branch", null),
            userSp.getString("activeBranch", null),
            settingsSp.getString("branches_json", null),
            settingsSp.getString("selected_branches", null),
            settingsSp.getString("branches", null),
            settingsSp.getString("active_branch", null),
            settingsSp.getString("activeBranch", null)
        )
    }

    private fun resolveUserGroups(context: Context): List<String> {
        val userSp = context.getSharedPreferences(
            "kmi_user",
            Context.MODE_PRIVATE
        )
        val settingsSp = context.getSharedPreferences(
            "kmi_settings",
            Context.MODE_PRIVATE
        )

        fun normalize(values: List<String>): List<String> {
            return values.map {
                TrainingCatalog.normalizeGroupName(it).ifBlank { it }
            }.filter {
                it.isNotBlank()
            }.distinct()
        }

        /*
         * age_groups הוא המקור העדכני שיכול להכיל קבוצה אחת
         * או כמה קבוצות של המשתמש הנוכחי.
         */
        val currentUserGroups = normalize(
            splitPrefsValues(
                userSp.getString("age_groups", null)
            )
        )

        if (currentUserGroups.isNotEmpty()) {
            return currentUserGroups
        }

        val currentSettingsGroups = normalize(
            splitPrefsValues(
                settingsSp.getString("age_groups", null)
            )
        )

        if (currentSettingsGroups.isNotEmpty()) {
            return currentSettingsGroups
        }

        val primaryUserGroup = normalize(
            splitPrefsValues(
                userSp.getString("group", null),
                userSp.getString("age_group", null)
            )
        )

        if (primaryUserGroup.isNotEmpty()) {
            return primaryUserGroup
        }

        return normalize(
            splitPrefsValues(
                userSp.getString("groups_json", null),
                userSp.getString("selected_groups", null),
                userSp.getString("groups", null),
                userSp.getString("active_group", null),
                userSp.getString("activeGroup", null),
                userSp.getString("groupKey", null),
                userSp.getString("group_key", null),
                settingsSp.getString("groups_json", null),
                settingsSp.getString("selected_groups", null),
                settingsSp.getString("groups", null),
                settingsSp.getString("active_group", null),
                settingsSp.getString("activeGroup", null),
                settingsSp.getString("group", null)
            )
        )
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
        place: String,
        startMinute: Long
    ): Int {
        /*
         * הקבוצה אינה חלק מהמזהה, משום שאותו אימון
         * עשוי להתאים לכמה קבוצות של אותו משתמש.
         *
         * שומרים על טווח Hash מלא כדי לצמצם
         * התנגשויות בין אימונים שונים.
         */
        val raw =
            "$startMinute|$place"

        return raw.hashCode() and Int.MAX_VALUE
    }

    private fun saveRequestCodes(
        context: Context,
        requestCodes: Set<Int>
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                KEY_REQUEST_CODES,
                requestCodes.joinToString(",")
            )
            .commit()
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