package il.kmi.app.database

/**
 * KmiDatabaseValidator
 *
 * כלי בדיקה פנימי לקבצי ה־database המקומיים.
 *
 * המטרה:
 * אחרי החלפת branches.json בעתיד, אפשר לבדוק שהקובץ תקין:
 * - אין IDs כפולים
 * - כל סניף מחובר לאזור קיים
 * - אין שדות בסיסיים חסרים
 * - אין אימונים בלי יום/שעה/קבוצה
 *
 * הקובץ לא משנה נתונים ולא מחליף את TrainingCatalog.
 */
object KmiDatabaseValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
        val summary: String
    )

    fun validateBranchesDatabase(
        db: KmiDatabaseRepository.KmiBranchesDatabase
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val regionIds = db.regions.map { it.id }.filter { it.isNotBlank() }.toSet()

        // --------------------------------------------------------------------
        // Regions validation
        // --------------------------------------------------------------------

        val duplicateRegionIds = db.regions
            .groupBy { it.id.trim() }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size > 1 }
            .keys

        duplicateRegionIds.forEach { id ->
            errors += "Duplicate region id: $id"
        }

        db.regions.forEachIndexed { index, region ->
            if (region.id.isBlank()) {
                errors += "Region at index $index has empty id"
            }

            if (region.nameHe.isBlank() && region.nameEn.isBlank()) {
                errors += "Region '${region.id}' has no Hebrew or English name"
            }

            if (region.country.isBlank()) {
                warnings += "Region '${region.id}' has empty country"
            }
        }

        // --------------------------------------------------------------------
        // Branches validation
        // --------------------------------------------------------------------

        val duplicateBranchIds = db.branches
            .groupBy { it.id.trim() }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size > 1 }
            .keys

        duplicateBranchIds.forEach { id ->
            errors += "Duplicate branch id: $id"
        }

        db.branches.forEachIndexed { index, branch ->
            if (branch.id.isBlank()) {
                errors += "Branch at index $index has empty id"
            }

            if (branch.regionId.isBlank()) {
                errors += "Branch '${branch.id}' has empty regionId"
            } else if (branch.regionId !in regionIds) {
                errors += "Branch '${branch.id}' references missing regionId='${branch.regionId}'"
            }

            if (branch.nameHe.isBlank() && branch.nameEn.isBlank()) {
                errors += "Branch '${branch.id}' has no Hebrew or English name"
            }

            if (branch.country.isBlank()) {
                warnings += "Branch '${branch.id}' has empty country"
            }

            if (branch.cityHe.isBlank() && branch.cityEn.isBlank()) {
                warnings += "Branch '${branch.id}' has no city"
            }

            if (branch.addressHe.isBlank() && branch.addressEn.isBlank()) {
                warnings += "Branch '${branch.id}' has no address"
            }

            if (branch.trainingDays.isEmpty()) {
                warnings += "Branch '${branch.id}' has no trainingDays"
            }

            /*
             * סניף פעיל עם אימונים חייב להכיל
             * את שדות הזהות הנדרשים למערכת המרכזית.
             */
            if (branch.active && branch.trainingDays.isNotEmpty()) {

                if (branch.nameHe.isBlank()) {
                    errors +=
                        "Active branch '${branch.id}' has empty nameHe"
                }

                if (branch.placeHe.isBlank()) {
                    errors +=
                        "Active branch '${branch.id}' has empty placeHe"
                }

                if (branch.addressHe.isBlank()) {
                    errors +=
                        "Active branch '${branch.id}' has empty addressHe"
                }

                val region =
                    db.regions.firstOrNull {
                        it.id == branch.regionId
                    }

                if (region != null && !region.active) {
                    errors +=
                        "Active branch '${branch.id}' belongs to inactive region"
                }
            }

            val allowedDays = setOf(
                "SUNDAY",
                "MONDAY",
                "TUESDAY",
                "WEDNESDAY",
                "THURSDAY",
                "FRIDAY",
                "SATURDAY"
            )

            val timePattern =
                Regex("""(?:[01]\d|2[0-3]):[0-5]\d""")

            val scheduleKeys = mutableSetOf<String>()

            branch.trainingDays.forEachIndexed { trainingIndex, training ->

                val prefix =
                    "Branch '${branch.id}' trainingDays[$trainingIndex]"

                val day =
                    training.dayOfWeek.trim().uppercase()

                val start =
                    training.startTime.trim()

                val end =
                    training.endTime.trim()

                val group =
                    training.groupHe.trim()

                if (day !in allowedDays) {
                    errors += "$prefix invalid dayOfWeek='$day'"
                }

                if (!timePattern.matches(start)) {
                    errors += "$prefix invalid startTime='$start'"
                }

                if (end.isBlank()) {
                    warnings += "$prefix has empty endTime"
                } else if (!timePattern.matches(end)) {
                    errors += "$prefix invalid endTime='$end'"
                }

                if (training.durationMinutes <= 0) {
                    errors +=
                        "$prefix invalid durationMinutes=" +
                                training.durationMinutes
                }

                if (
                    training.groupHe.isBlank() &&
                    training.groupEn.isBlank()
                ) {
                    errors += "$prefix has no group name"
                }

                if (
                    training.coachNameHe.isBlank() &&
                    training.coachNameEn.isBlank()
                ) {
                    warnings += "$prefix has no coach name"
                }

                /*
                 * בדיקת כפילות של אימון שבועי:
                 * יום + שעה + קבוצה.
                 */
                val scheduleKey = listOf(
                    day,
                    start,
                    group.ifBlank {
                        training.groupEn.trim()
                    }
                ).joinToString("|")

                if (!scheduleKeys.add(scheduleKey)) {
                    errors +=
                        "$prefix duplicate weekly training: $scheduleKey"
                }

                /*
                 * בדיקת התאמה בין משך האימון
                 * לבין שעת הסיום המוצהרת.
                 */
                if (
                    timePattern.matches(start) &&
                    timePattern.matches(end) &&
                    training.durationMinutes > 0
                ) {
                    val startMinutes =
                        start.substringBefore(":").toInt() * 60 +
                                start.substringAfter(":").toInt()

                    val endMinutes =
                        end.substringBefore(":").toInt() * 60 +
                                end.substringAfter(":").toInt()

                    val calculatedDuration =
                        (endMinutes - startMinutes + 1440) % 1440

                    if (
                        calculatedDuration != training.durationMinutes
                    ) {
                        warnings +=
                            "$prefix duration mismatch: " +
                                    "expected=${training.durationMinutes} " +
                                    "actual=$calculatedDuration"
                    }
                }
            }
        }

        val activeRegions = db.regions.count { it.active }
        val activeBranches = db.branches.count { it.active }
        val branchesWithSchedule = db.branches.count { it.trainingDays.isNotEmpty() }
        val totalTrainingDays = db.branches.sumOf { it.trainingDays.size }

        val summary = buildString {
            append("KMI Database Validation")
            append("\nversion=").append(db.version)
            append("\nupdatedAt=").append(db.updatedAt)
            append("\nregions=").append(db.regions.size)
            append("\nactiveRegions=").append(activeRegions)
            append("\nbranches=").append(db.branches.size)
            append("\nactiveBranches=").append(activeBranches)
            append("\nbranchesWithSchedule=").append(branchesWithSchedule)
            append("\ntotalTrainingDays=").append(totalTrainingDays)
            append("\nerrors=").append(errors.size)
            append("\nwarnings=").append(warnings.size)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            summary = summary
        )
    }
}