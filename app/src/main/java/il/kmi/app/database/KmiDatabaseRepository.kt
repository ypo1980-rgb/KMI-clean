package il.kmi.app.database

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * KmiDatabaseRepository
 *
 * שכבת קריאה מקומית לקבצי הנתונים שנמצאים ב:
 * app/src/main/assets/kmi_database/
 *
 * בשלב הזה הקובץ רק קורא את branches.json.
 * לא מחליפים עדיין את TrainingCatalog כדי לא לשבור מסכים קיימים.
 */
object KmiDatabaseRepository {

    private const val BRANCHES_FILE_PATH = "kmi_database/branches.json"
    private const val NETWORK_COACHES_FILE_PATH = "kmi_database/network_coaches.json"

    // ------------------------------------------------------------------------
    // Models
    // ------------------------------------------------------------------------

    data class KmiRegion(
        val id: String,
        val active: Boolean,
        val nameHe: String,
        val nameEn: String,
        val country: String
    ) {
        fun displayName(isEnglish: Boolean): String {
            return if (isEnglish) nameEn.ifBlank { nameHe } else nameHe.ifBlank { nameEn }
        }
    }

    data class KmiBranch(
        val id: String,
        val active: Boolean,
        val regionId: String,
        val regionHe: String,
        val regionEn: String,
        val country: String,
        val countryHe: String,
        val countryEn: String,
        val cityHe: String,
        val cityEn: String,
        val nameHe: String,
        val nameEn: String,
        val placeHe: String,
        val placeEn: String,
        val addressHe: String,
        val addressEn: String,
        val coachIds: List<String>,
        val trainingDays: List<KmiTrainingDay>,
        val notesHe: String,
        val notesEn: String
    ) {
        fun displayName(isEnglish: Boolean): String {
            return if (isEnglish) nameEn.ifBlank { nameHe } else nameHe.ifBlank { nameEn }
        }

        fun displayPlace(isEnglish: Boolean): String {
            return if (isEnglish) placeEn.ifBlank { placeHe } else placeHe.ifBlank { placeEn }
        }

        fun displayAddress(isEnglish: Boolean): String {
            return if (isEnglish) addressEn.ifBlank { addressHe } else addressHe.ifBlank { addressEn }
        }

        fun displayRegion(isEnglish: Boolean): String {
            return if (isEnglish) regionEn.ifBlank { regionHe } else regionHe.ifBlank { regionEn }
        }

        fun displayCountry(isEnglish: Boolean): String {
            return if (isEnglish) countryEn.ifBlank { countryHe } else countryHe.ifBlank { countryEn }
        }

        fun displayNotes(isEnglish: Boolean): String {
            return if (isEnglish) notesEn.ifBlank { notesHe } else notesHe.ifBlank { notesEn }
        }
    }

    data class KmiTrainingDay(
        val dayOfWeek: String,
        val dayHe: String,
        val dayEn: String,
        val startTime: String,
        val endTime: String,
        val durationMinutes: Int,
        val groupHe: String,
        val groupEn: String,
        val coachNameHe: String,
        val coachNameEn: String
    ) {
        fun displayDay(isEnglish: Boolean): String {
            return if (isEnglish) dayEn.ifBlank { dayHe } else dayHe.ifBlank { dayEn }
        }

        fun displayGroup(isEnglish: Boolean): String {
            return if (isEnglish) groupEn.ifBlank { groupHe } else groupHe.ifBlank { groupEn }
        }

        fun displayCoachName(isEnglish: Boolean): String {
            return if (isEnglish) coachNameEn.ifBlank { coachNameHe } else coachNameHe.ifBlank { coachNameEn }
        }

        fun displayTimeRange(): String {
            return if (startTime.isNotBlank() && endTime.isNotBlank()) {
                "$startTime–$endTime"
            } else {
                startTime.ifBlank { endTime }
            }
        }
    }

    data class KmiBranchesDatabase(
        val version: Int,
        val updatedAt: String,
        val regions: List<KmiRegion>,
        val branches: List<KmiBranch>
    )

    data class KmiNetworkCoach(
        val id: String,
        val active: Boolean,
        val sortOrder: Int,
        val type: String,
        val nameHe: String,
        val nameEn: String,
        val roleHe: String,
        val roleEn: String,
        val rankHe: String,
        val rankEn: String,
        val experienceHe: String,
        val experienceEn: String,
        val trainingHe: String,
        val trainingEn: String,
        val certificationsHe: List<String>,
        val certificationsEn: List<String>,
        val branchIds: List<String>,
        val branchesHe: List<String>,
        val branchesEn: List<String>,
        val descriptionHe: String,
        val descriptionEn: String
    )

    data class KmiNetworkCoachesDatabase(
        val version: Int,
        val updatedAt: String,
        val coaches: List<KmiNetworkCoach>
    )

    // ------------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------------

    fun loadBranchesDatabase(context: Context): KmiBranchesDatabase {
        val jsonText = context.assets
            .open(BRANCHES_FILE_PATH)
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(jsonText)

        return KmiBranchesDatabase(
            version = root.optInt("version", 1),
            updatedAt = root.optString("updatedAt", ""),
            regions = root.optJSONArray("regions").toRegionList(),
            branches = root.optJSONArray("branches").toBranchList()
        )
    }

    fun loadNetworkCoachesDatabase(context: Context): KmiNetworkCoachesDatabase {
        val jsonText = context.assets
            .open(NETWORK_COACHES_FILE_PATH)
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(jsonText)

        return KmiNetworkCoachesDatabase(
            version = root.optInt("version", 1),
            updatedAt = root.optString("updatedAt", ""),
            coaches = root.optJSONArray("coaches").toNetworkCoachList()
        )
    }

    fun loadNetworkCoaches(
        context: Context,
        onlyActive: Boolean = true
    ): List<KmiNetworkCoach> {
        val coaches = loadNetworkCoachesDatabase(context).coaches
        return if (onlyActive) coaches.filter { it.active } else coaches
    }

    fun loadRegions(context: Context, onlyActive: Boolean = true): List<KmiRegion> {
        val regions = loadBranchesDatabase(context).regions
        return if (onlyActive) regions.filter { it.active } else regions
    }

    fun loadBranches(context: Context, onlyActive: Boolean = true): List<KmiBranch> {
        val branches = loadBranchesDatabase(context).branches
        return if (onlyActive) branches.filter { it.active } else branches
    }

    fun branchesByRegion(
        context: Context,
        regionId: String,
        onlyActive: Boolean = true
    ): List<KmiBranch> {
        return loadBranches(context, onlyActive)
            .filter { it.regionId == regionId }
    }

    fun branchById(
        context: Context,
        branchId: String
    ): KmiBranch? {
        return loadBranches(context, onlyActive = false)
            .firstOrNull { it.id == branchId }
    }

    fun branchByName(
        context: Context,
        branchName: String
    ): KmiBranch? {
        val normalized = branchName.trim()

        return loadBranches(context, onlyActive = false)
            .firstOrNull { branch ->
                branch.nameHe.trim() == normalized ||
                        branch.nameEn.trim() == normalized ||
                        branch.placeHe.trim() == normalized ||
                        branch.placeEn.trim() == normalized
            }
    }

    fun trainingDaysForBranch(
        context: Context,
        branchId: String
    ): List<KmiTrainingDay> {
        return branchById(context, branchId)
            ?.trainingDays
            .orEmpty()
    }

    fun hasTrainingSchedule(
        context: Context,
        branchId: String
    ): Boolean {
        return trainingDaysForBranch(context, branchId).isNotEmpty()
    }

    // ------------------------------------------------------------------------
    // JSON parsing helpers
    // ------------------------------------------------------------------------

    private fun JSONArray?.toRegionList(): List<KmiRegion> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue

                add(
                    KmiRegion(
                        id = item.optString("id", ""),
                        active = item.optBoolean("active", true),
                        nameHe = item.optString("nameHe", ""),
                        nameEn = item.optString("nameEn", ""),
                        country = item.optString("country", "")
                    )
                )
            }
        }
    }

    private fun JSONArray?.toBranchList(): List<KmiBranch> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue

                add(
                    KmiBranch(
                        id = item.optString("id", ""),
                        active = item.optBoolean("active", true),
                        regionId = item.optString("regionId", ""),
                        regionHe = item.optString("regionHe", ""),
                        regionEn = item.optString("regionEn", ""),
                        country = item.optString("country", ""),
                        countryHe = item.optString("countryHe", ""),
                        countryEn = item.optString("countryEn", ""),
                        cityHe = item.optString("cityHe", ""),
                        cityEn = item.optString("cityEn", ""),
                        nameHe = item.optString("nameHe", ""),
                        nameEn = item.optString("nameEn", ""),
                        placeHe = item.optString("placeHe", ""),
                        placeEn = item.optString("placeEn", ""),
                        addressHe = item.optString("addressHe", ""),
                        addressEn = item.optString("addressEn", ""),
                        coachIds = item.optJSONArray("coachIds").toStringList(),
                        trainingDays = item.optJSONArray("trainingDays").toTrainingDayList(),
                        notesHe = item.optString("notesHe", ""),
                        notesEn = item.optString("notesEn", "")
                    )
                )
            }
        }
    }

    private fun JSONArray?.toTrainingDayList(): List<KmiTrainingDay> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue

                add(
                    KmiTrainingDay(
                        dayOfWeek = item.optString("dayOfWeek", ""),
                        dayHe = item.optString("dayHe", ""),
                        dayEn = item.optString("dayEn", ""),
                        startTime = item.optString("startTime", ""),
                        endTime = item.optString("endTime", ""),
                        durationMinutes = item.optInt("durationMinutes", 0),
                        groupHe = item.optString("groupHe", ""),
                        groupEn = item.optString("groupEn", ""),
                        coachNameHe = item.optString("coachNameHe", ""),
                        coachNameEn = item.optString("coachNameEn", "")
                    )
                )
            }
        }
    }

    private fun JSONArray?.toNetworkCoachList(): List<KmiNetworkCoach> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue

                val nameHe = item.optString("nameHe", "")
                val nameEn = item.optString("nameEn", nameHe)

                if (nameHe.isBlank() && nameEn.isBlank()) continue

                add(
                    KmiNetworkCoach(
                        id = item.optString("id", "network_coach_$index"),
                        active = item.optBoolean("active", true),
                        sortOrder = item.optInt("sortOrder", 999),
                        type = item.optString("type", "coach"),
                        nameHe = nameHe.ifBlank { nameEn },
                        nameEn = nameEn.ifBlank { nameHe },
                        roleHe = item.optString("roleHe", "מאמן ברשת ק.מ.י"),
                        roleEn = item.optString("roleEn", "K.M.I Network Coach"),
                        rankHe = item.optString("rankHe", "דרגה תעודכן בהמשך"),
                        rankEn = item.optString("rankEn", "Rank will be updated"),
                        experienceHe = item.optString("experienceHe", "ותק מקצועי יעודכן בהמשך"),
                        experienceEn = item.optString("experienceEn", "Professional experience will be updated"),
                        trainingHe = item.optString("trainingHe", "הכשרות מקצועיות יעודכנו בהמשך"),
                        trainingEn = item.optString("trainingEn", "Professional training will be updated"),
                        certificationsHe = item.optJSONArray("certificationsHe").toStringList(),
                        certificationsEn = item.optJSONArray("certificationsEn").toStringList(),
                        branchIds = item.optJSONArray("branchIds").toStringList(),
                        branchesHe = item.optJSONArray("branchesHe").toStringList(),
                        branchesEn = item.optJSONArray("branchesEn").toStringList(),
                        descriptionHe = item.optString("descriptionHe", "מידע מקצועי על המאמן יעודכן בהמשך."),
                        descriptionEn = item.optString("descriptionEn", "Professional coach information will be updated later.")
                    )
                )
            }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val value = optString(index, "").trim()
                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }
    }
}