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

            branch.trainingDays.forEachIndexed { trainingIndex, training ->
                val prefix = "Branch '${branch.id}' trainingDays[$trainingIndex]"

                if (training.dayOfWeek.isBlank()) {
                    errors += "$prefix has empty dayOfWeek"
                }

                if (training.startTime.isBlank()) {
                    errors += "$prefix has empty startTime"
                }

                if (training.endTime.isBlank()) {
                    warnings += "$prefix has empty endTime"
                }

                if (training.durationMinutes <= 0) {
                    warnings += "$prefix has invalid durationMinutes=${training.durationMinutes}"
                }

                if (training.groupHe.isBlank() && training.groupEn.isBlank()) {
                    errors += "$prefix has no group name"
                }

                if (training.coachNameHe.isBlank() && training.coachNameEn.isBlank()) {
                    warnings += "$prefix has no coach name"
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