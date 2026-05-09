package il.kmi.app.database

import android.content.Context

/**
 * KmiDatabaseProvider
 *
 * מחזיק Cache בזיכרון לקבצי ה־database המקומיים.
 *
 * במקום שכל מסך יקרא את קבצי ה־JSON מה-assets מחדש,
 * נטען פעם אחת ונחזיק את הנתונים בזיכרון.
 */
object KmiDatabaseProvider {

    private var cachedBranchesDatabase: KmiDatabaseRepository.KmiBranchesDatabase? = null
    private var cachedNetworkCoachesDatabase: KmiDatabaseRepository.KmiNetworkCoachesDatabase? = null

    // ------------------------------------------------------------------------
    // Branches Database
    // ------------------------------------------------------------------------

    fun getBranchesDatabase(
        context: Context,
        forceReload: Boolean = false
    ): KmiDatabaseRepository.KmiBranchesDatabase {
        if (!forceReload) {
            cachedBranchesDatabase?.let { return it }
        }

        val loaded = KmiDatabaseRepository.loadBranchesDatabase(
            context = context.applicationContext
        )

        cachedBranchesDatabase = loaded
        return loaded
    }

    fun regions(
        context: Context,
        onlyActive: Boolean = true
    ): List<KmiDatabaseRepository.KmiRegion> {
        val db = getBranchesDatabase(context)
        return if (onlyActive) {
            db.regions.filter { it.active }
        } else {
            db.regions
        }
    }

    fun branches(
        context: Context,
        onlyActive: Boolean = true
    ): List<KmiDatabaseRepository.KmiBranch> {
        val db = getBranchesDatabase(context)
        return if (onlyActive) {
            db.branches.filter { it.active }
        } else {
            db.branches
        }
    }

    fun branchesByRegion(
        context: Context,
        regionId: String,
        onlyActive: Boolean = true
    ): List<KmiDatabaseRepository.KmiBranch> {
        return branches(context, onlyActive)
            .filter { it.regionId == regionId }
    }

    fun branchById(
        context: Context,
        branchId: String
    ): KmiDatabaseRepository.KmiBranch? {
        return branches(context, onlyActive = false)
            .firstOrNull { it.id == branchId }
    }

    fun branchByName(
        context: Context,
        branchName: String
    ): KmiDatabaseRepository.KmiBranch? {
        val normalized = branchName.trim()

        return branches(context, onlyActive = false)
            .firstOrNull { branch ->
                branch.nameHe.trim() == normalized ||
                        branch.nameEn.trim() == normalized ||
                        branch.placeHe.trim() == normalized ||
                        branch.placeEn.trim() == normalized
            }
    }

    fun hasTrainingSchedule(
        context: Context,
        branchId: String
    ): Boolean {
        return branchById(context, branchId)
            ?.trainingDays
            ?.isNotEmpty() == true
    }

    fun databaseSummary(context: Context): String {
        val db = getBranchesDatabase(context)

        val activeRegions = db.regions.count { it.active }
        val activeBranches = db.branches.count { it.active }
        val branchesWithSchedule = db.branches.count { it.trainingDays.isNotEmpty() }

        return buildString {
            append("KMI Database")
            append("\nversion=").append(db.version)
            append("\nupdatedAt=").append(db.updatedAt)
            append("\nregions=").append(db.regions.size)
            append("\nactiveRegions=").append(activeRegions)
            append("\nbranches=").append(db.branches.size)
            append("\nactiveBranches=").append(activeBranches)
            append("\nbranchesWithSchedule=").append(branchesWithSchedule)
        }
    }

    fun validateBranchesDatabase(
        context: Context
    ): KmiDatabaseValidator.ValidationResult {
        val db = getBranchesDatabase(context)
        return KmiDatabaseValidator.validateBranchesDatabase(db)
    }

    fun validationSummary(context: Context): String {
        val result = validateBranchesDatabase(context)

        return buildString {
            append(result.summary)

            if (result.errors.isNotEmpty()) {
                append("\n\nErrors:")
                result.errors.forEach { error ->
                    append("\n- ").append(error)
                }
            }

            if (result.warnings.isNotEmpty()) {
                append("\n\nWarnings:")
                result.warnings.forEach { warning ->
                    append("\n- ").append(warning)
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Network Coaches Database
    // ------------------------------------------------------------------------

    fun getNetworkCoachesDatabase(
        context: Context,
        forceReload: Boolean = false
    ): KmiDatabaseRepository.KmiNetworkCoachesDatabase {
        if (!forceReload) {
            cachedNetworkCoachesDatabase?.let { return it }
        }

        val loaded = KmiDatabaseRepository.loadNetworkCoachesDatabase(
            context = context.applicationContext
        )

        cachedNetworkCoachesDatabase = loaded
        return loaded
    }

    fun networkCoaches(
        context: Context,
        onlyActive: Boolean = true
    ): List<KmiDatabaseRepository.KmiNetworkCoach> {
        val db = getNetworkCoachesDatabase(context)

        return if (onlyActive) {
            db.coaches.filter { it.active }
        } else {
            db.coaches
        }
    }

    fun networkCoachById(
        context: Context,
        coachId: String
    ): KmiDatabaseRepository.KmiNetworkCoach? {
        return networkCoaches(context, onlyActive = false)
            .firstOrNull { it.id == coachId }
    }

    // ------------------------------------------------------------------------
    // Cache
    // ------------------------------------------------------------------------

    fun clearCache() {
        cachedBranchesDatabase = null
        cachedNetworkCoachesDatabase = null
    }
}