package il.kmi.app.database

import android.content.Context
import org.json.JSONObject
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import il.kmi.app.training.TrainingCatalog


/**
 * KmiDatabaseProvider
 *
 * מחזיק Cache בזיכרון לקבצי ה־database המקומיים.
 *
 * במקום שכל מסך יקרא את קבצי ה־JSON מה-assets מחדש,
 * נטען פעם אחת ונחזיק את הנתונים בזיכרון.
 */
object KmiDatabaseProvider {

    @Volatile
    private var cachedBranchesDatabase:
            KmiDatabaseRepository.KmiBranchesDatabase? = null

    private val refreshLock = Any()
    private val _catalogRevision =
        MutableStateFlow(0L)

    val catalogRevision =
        _catalogRevision.asStateFlow()

    private fun publishCatalogUpdated() {
        _catalogRevision.value =
            _catalogRevision.value + 1L
    }

    private var refreshInProgress = false

    private val refreshCallbacks =
        mutableListOf<(Boolean, String) -> Unit>()

    private fun finishBranchesRefresh(
        success: Boolean,
        status: String
    ) {
        val callbacks = synchronized(refreshLock) {
            refreshInProgress = false

            refreshCallbacks.toList().also {
                refreshCallbacks.clear()
            }
        }

        callbacks.forEach { callback ->
            runCatching {
                callback(success, status)
            }.onFailure { error ->
                android.util.Log.e(
                    "KMI_CATALOG_CALLBACK",
                    "Catalog refresh callback failed",
                    error
                )
            }
        }
    }

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

    fun refreshBranchesDatabase(
        context: Context,
        onResult: (Boolean, String) -> Unit
    ) {
        val appContext = context.applicationContext

        synchronized(refreshLock) {
            refreshCallbacks.add(onResult)

            if (refreshInProgress) {
                return
            }

            refreshInProgress = true
        }

        FirebaseFirestore.getInstance()
            .collection("kmiCatalog")
            .document("current")
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->

                val result = runCatching {

                    if (!snapshot.exists()) {
                        return@runCatching Pair(
                            false,
                            "catalog_missing"
                        )
                    }

                    val data = snapshot.data
                        ?: return@runCatching Pair(
                            false,
                            "catalog_empty"
                        )

                    val jsonText =
                        JSONObject(data).toString()

                    val updatedDatabase =
                        KmiDatabaseRepository
                            .parseBranchesDatabase(jsonText)

                    val validation =
                        KmiDatabaseValidator
                            .validateBranchesDatabase(
                                updatedDatabase
                            )

                    if (validation.errors.isNotEmpty()) {
                        return@runCatching Pair(
                            false,
                            "catalog_validation_failed"
                        )
                    }

                    /*
                     * אין שומרים קטלוג חדש לפני
                     * בדיקת זהות האימונים.
                     */
                    val installed =
                        TrainingCatalog.installCentralSchedule(
                            updatedDatabase
                        )

                    if (!installed) {
                        return@runCatching Pair(
                            false,
                            "catalog_identity_rejected"
                        )
                    }

                    /*
                     * הקטלוג עבר את שתי הבדיקות:
                     * מבנה הנתונים וזהות האימונים.
                     */
                    /*
  * התקנת הקטלוג בזיכרון כבר הצליחה.
  * כשל בכתיבה לדיסק לא מבטל אותה.
  */
                    cachedBranchesDatabase =
                        updatedDatabase

                    val cacheSaved = runCatching {
                        KmiDatabaseRepository
                            .saveBranchesDatabaseCache(
                                appContext,
                                jsonText
                            )
                    }.onFailure { error ->
                        android.util.Log.e(
                            "KMI_CATALOG_CACHE",
                            "Catalog installed, disk cache failed",
                            error
                        )
                    }.isSuccess

                    publishCatalogUpdated()

                    Pair(
                        true,
                        if (cacheSaved) {
                            "catalog_updated"
                        } else {
                            "catalog_updated_cache_failed"
                        }
                    )
                }.getOrElse { error ->

                    android.util.Log.e(
                        "KMI_CATALOG_SYNC_ERROR",
                        "Catalog processing failed",
                        error
                    )

                    Pair(
                        false,
                        "catalog_processing_failed"
                    )
                }

                finishBranchesRefresh(
                    result.first,
                    result.second
                )
            }
            .addOnFailureListener { error ->

                val firestoreCode =
                    (
                            error as?
                                    com.google.firebase.firestore
                                    .FirebaseFirestoreException
                            )?.code?.name ?: "UNKNOWN"

                android.util.Log.e(
                    "KMI_CATALOG_SYNC_ERROR",
                    "Firestore read failed | " +
                            "code=$firestoreCode",
                    error
                )

                finishBranchesRefresh(
                    false,
                    "catalog_read_failed_$firestoreCode"
                )
            }
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