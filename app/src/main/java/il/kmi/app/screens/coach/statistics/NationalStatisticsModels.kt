package il.kmi.app.screens.coach.statistics

import kotlin.math.roundToInt

/**
 * המין כפי שנשמר לצורכי סטטיסטיקה.
 *
 * UNKNOWN חשוב כדי לא להציג מידע חסר כאילו הוא נתון אמיתי.
 */
enum class NationalStatsGender {
    MALE,
    FEMALE,
    OTHER,
    UNKNOWN
}

/**
 * קבוצות גיל קבועות לחיתוכים הארציים.
 */
enum class NationalStatsAgeGroup(
    val minAge: Int?,
    val maxAge: Int?
) {
    CHILDREN(
        minAge = 0,
        maxAge = 11
    ),
    TEENS(
        minAge = 12,
        maxAge = 17
    ),
    YOUNG_ADULTS(
        minAge = 18,
        maxAge = 25
    ),
    ADULTS(
        minAge = 26,
        maxAge = 40
    ),
    MATURE_ADULTS(
        minAge = 41,
        maxAge = 59
    ),
    SENIORS(
        minAge = 60,
        maxAge = Int.MAX_VALUE
    ),
    UNKNOWN(
        minAge = null,
        maxAge = null
    );

    fun contains(age: Int?): Boolean {
        if (this == UNKNOWN) {
            return age == null || age <= 0
        }

        val validAge = age ?: return false
        val minimum = minAge ?: return false
        val maximum = maxAge ?: return false

        return validAge in minimum..maximum
    }

    companion object {
        fun fromAge(age: Int?): NationalStatsAgeGroup {
            return entries.firstOrNull { group ->
                group != UNKNOWN && group.contains(age)
            } ?: UNKNOWN
        }
    }
}

/**
 * מתאמן יחיד בסטטיסטיקה הארצית.
 *
 * branches ו-groups הם Set משום שמשתמש יכול להיות
 * משויך ליותר מסניף או קבוצה אחת.
 */
data class NationalTraineeRecord(
    val id: String,
    val fullName: String,
    val branches: Set<String>,
    val groups: Set<String>,
    val age: Int?,
    val gender: NationalStatsGender,
    val belt: String,
    val seniorityYears: Double?,
    val attendancePercent: Int?,
    val isActive: Boolean
)

/**
 * מצב הפילטרים במסך.
 *
 * Set ריק משמעותו "הכול".
 */
data class NationalStatisticsFilters(
    val selectedBranches: Set<String> = emptySet(),
    val selectedGroups: Set<String> = emptySet(),
    val selectedBelts: Set<String> = emptySet(),
    val selectedGenders: Set<NationalStatsGender> = emptySet(),
    val selectedAgeGroups: Set<NationalStatsAgeGroup> = emptySet(),
    val activeOnly: Boolean = true,
    val searchQuery: String = ""
) {
    val hasActiveFilters: Boolean
        get() =
            selectedBranches.isNotEmpty() ||
                    selectedGroups.isNotEmpty() ||
                    selectedBelts.isNotEmpty() ||
                    selectedGenders.isNotEmpty() ||
                    selectedAgeGroups.isNotEmpty() ||
                    !activeOnly ||
                    searchQuery.isNotBlank()
}

/**
 * נתונים של סניף יחיד בתוך התמונה הארצית.
 */
data class NationalBranchStatistics(
    val branchName: String,
    val traineeCount: Int,
    val averageAge: Int?,
    val averageAttendance: Int?,
    val beltCounts: Map<String, Int>,
    val genderCounts: Map<NationalStatsGender, Int>,
    val ageGroupCounts: Map<NationalStatsAgeGroup, Int>
)

/**
 * התוצאה המלאה שמוצגת במסך הסטטיסטיקה.
 */
data class NationalStatisticsSnapshot(
    val totalUniqueTrainees: Int,
    val filteredUniqueTrainees: Int,
    val activeTrainees: Int,
    val branchCount: Int,
    val groupCount: Int,
    val averageAge: Int?,
    val minimumAge: Int?,
    val maximumAge: Int?,
    val averageAttendance: Int?,
    val traineesWithAttendanceData: Int,
    val averageSeniorityYears: Double?,
    val branchCounts: Map<String, Int>,
    val groupCounts: Map<String, Int>,
    val beltCounts: Map<String, Int>,
    val genderCounts: Map<NationalStatsGender, Int>,
    val ageGroupCounts: Map<NationalStatsAgeGroup, Int>,
    val branchStatistics: List<NationalBranchStatistics>,
    val filteredTrainees: List<NationalTraineeRecord>
) {
    companion object {
        val EMPTY = NationalStatisticsSnapshot(
            totalUniqueTrainees = 0,
            filteredUniqueTrainees = 0,
            activeTrainees = 0,
            branchCount = 0,
            groupCount = 0,
            averageAge = null,
            minimumAge = null,
            maximumAge = null,
            averageAttendance = null,
            traineesWithAttendanceData = 0,
            averageSeniorityYears = null,
            branchCounts = emptyMap(),
            groupCounts = emptyMap(),
            beltCounts = emptyMap(),
            genderCounts = emptyMap(),
            ageGroupCounts = emptyMap(),
            branchStatistics = emptyList(),
            filteredTrainees = emptyList()
        )
    }
}

object NationalStatisticsCalculator {

    fun calculate(
        allRecords: List<NationalTraineeRecord>,
        filters: NationalStatisticsFilters
    ): NationalStatisticsSnapshot {
        /*
         * אותו משתמש עשוי להופיע ביותר מסניף או קבוצה.
         * מאחדים לפי id לפני חישוב הנתונים הארציים.
         */
        val uniqueRecords = mergeDuplicateRecords(allRecords)

        val filteredRecords = uniqueRecords
            .asSequence()
            .filter { record ->
                matchesFilters(
                    record = record,
                    filters = filters
                )
            }
            .sortedWith(
                compareBy<NationalTraineeRecord> {
                    it.fullName.lowercase()
                }.thenBy {
                    it.id
                }
            )
            .toList()

        val validAges = filteredRecords
            .mapNotNull { record ->
                record.age?.takeIf { age -> age in 1..120 }
            }

        val attendanceValues = filteredRecords
            .mapNotNull { record ->
                record.attendancePercent
                    ?.coerceIn(0, 100)
            }

        val seniorityValues = filteredRecords
            .mapNotNull { record ->
                record.seniorityYears
                    ?.takeIf { years -> years >= 0.0 }
            }

        val branchCounts = countSetValues(
            records = filteredRecords,
            values = { it.branches }
        )

        val groupCounts = countSetValues(
            records = filteredRecords,
            values = { it.groups }
        )

        val beltCounts = filteredRecords
            .groupingBy { record ->
                record.belt
                    .trim()
                    .ifBlank { UNKNOWN_BELT }
            }
            .eachCount()
            .sortedByDescendingValue()

        val genderCounts = filteredRecords
            .groupingBy { it.gender }
            .eachCount()
            .sortedByDescendingValue()

        val ageGroupCounts = filteredRecords
            .groupingBy { record ->
                NationalStatsAgeGroup.fromAge(record.age)
            }
            .eachCount()
            .sortedByDescendingValue()

        val branchStatistics = branchCounts.keys
            .map { branchName ->
                buildBranchStatistics(
                    branchName = branchName,
                    records = filteredRecords.filter { record ->
                        branchName in record.branches
                    }
                )
            }
            .sortedWith(
                compareByDescending<NationalBranchStatistics> {
                    it.traineeCount
                }.thenBy {
                    it.branchName
                }
            )

        return NationalStatisticsSnapshot(
            totalUniqueTrainees = uniqueRecords.size,
            filteredUniqueTrainees = filteredRecords.size,
            activeTrainees = filteredRecords.count { it.isActive },
            branchCount = branchCounts.size,
            groupCount = groupCounts.size,
            averageAge =
                validAges.averageIntOrNull()?.roundToInt(),
            minimumAge = validAges.minOrNull(),
            maximumAge = validAges.maxOrNull(),
            averageAttendance =
                attendanceValues.averageIntOrNull()?.roundToInt(),
            traineesWithAttendanceData = attendanceValues.size,
            averageSeniorityYears =
                seniorityValues.averageDoubleOrNull(),
            branchCounts = branchCounts,
            groupCounts = groupCounts,
            beltCounts = beltCounts,
            genderCounts = genderCounts,
            ageGroupCounts = ageGroupCounts,
            branchStatistics = branchStatistics,
            filteredTrainees = filteredRecords
        )
    }

    private fun matchesFilters(
        record: NationalTraineeRecord,
        filters: NationalStatisticsFilters
    ): Boolean {
        if (filters.activeOnly && !record.isActive) {
            return false
        }

        if (
            filters.selectedBranches.isNotEmpty() &&
            record.branches.none { branch ->
                branch in filters.selectedBranches
            }
        ) {
            return false
        }

        if (
            filters.selectedGroups.isNotEmpty() &&
            record.groups.none { group ->
                group in filters.selectedGroups
            }
        ) {
            return false
        }

        val normalizedBelt = record.belt
            .trim()
            .ifBlank { UNKNOWN_BELT }

        if (
            filters.selectedBelts.isNotEmpty() &&
            normalizedBelt !in filters.selectedBelts
        ) {
            return false
        }

        if (
            filters.selectedGenders.isNotEmpty() &&
            record.gender !in filters.selectedGenders
        ) {
            return false
        }

        val ageGroup = NationalStatsAgeGroup.fromAge(record.age)

        if (
            filters.selectedAgeGroups.isNotEmpty() &&
            ageGroup !in filters.selectedAgeGroups
        ) {
            return false
        }

        val query = normalizeSearch(filters.searchQuery)

        if (query.isNotBlank()) {
            val searchableValues = buildList {
                add(record.fullName)
                add(record.belt)
                addAll(record.branches)
                addAll(record.groups)
            }

            val hasSearchMatch = searchableValues.any { value ->
                normalizeSearch(value).contains(query)
            }

            if (!hasSearchMatch) {
                return false
            }
        }

        return true
    }

    private fun buildBranchStatistics(
        branchName: String,
        records: List<NationalTraineeRecord>
    ): NationalBranchStatistics {
        val validAges = records
            .mapNotNull { record ->
                record.age?.takeIf { age -> age in 1..120 }
            }

        val attendanceValues = records
            .mapNotNull { record ->
                record.attendancePercent
                    ?.coerceIn(0, 100)
            }

        val beltCounts = records
            .groupingBy { record ->
                record.belt
                    .trim()
                    .ifBlank { UNKNOWN_BELT }
            }
            .eachCount()
            .sortedByDescendingValue()

        val genderCounts = records
            .groupingBy { it.gender }
            .eachCount()
            .sortedByDescendingValue()

        val ageGroupCounts = records
            .groupingBy { record ->
                NationalStatsAgeGroup.fromAge(record.age)
            }
            .eachCount()
            .sortedByDescendingValue()

        return NationalBranchStatistics(
            branchName = branchName,
            traineeCount = records.size,
            averageAge =
                validAges.averageIntOrNull()?.roundToInt(),
            averageAttendance =
                attendanceValues.averageIntOrNull()?.roundToInt(),
            beltCounts = beltCounts,
            genderCounts = genderCounts,
            ageGroupCounts = ageGroupCounts
        )
    }

    private fun mergeDuplicateRecords(
        records: List<NationalTraineeRecord>
    ): List<NationalTraineeRecord> {
        return records
            .filter { record -> record.id.isNotBlank() }
            .groupBy { record -> record.id.trim() }
            .map { (id, duplicates) ->
                val primary = duplicates.first()

                NationalTraineeRecord(
                    id = id,
                    fullName = duplicates
                        .firstNotNullOfOrNull { record ->
                            record.fullName
                                .trim()
                                .takeIf { it.isNotBlank() }
                        }
                        .orEmpty(),
                    branches = duplicates
                        .flatMap { it.branches }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toSet(),
                    groups = duplicates
                        .flatMap { it.groups }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toSet(),
                    age = duplicates
                        .firstNotNullOfOrNull { record ->
                            record.age?.takeIf { it in 1..120 }
                        },
                    gender = duplicates
                        .map { it.gender }
                        .firstOrNull {
                            it != NationalStatsGender.UNKNOWN
                        }
                        ?: NationalStatsGender.UNKNOWN,
                    belt = duplicates
                        .firstNotNullOfOrNull { record ->
                            record.belt
                                .trim()
                                .takeIf { it.isNotBlank() }
                        }
                        .orEmpty(),
                    seniorityYears = duplicates
                        .mapNotNull { it.seniorityYears }
                        .maxOrNull(),
                    attendancePercent = duplicates
                        .mapNotNull { it.attendancePercent }
                        .takeIf { it.isNotEmpty() }
                        ?.average()
                        ?.roundToInt(),
                    isActive = duplicates.any { it.isActive }
                )
            }
    }

    private fun countSetValues(
        records: List<NationalTraineeRecord>,
        values: (NationalTraineeRecord) -> Set<String>
    ): Map<String, Int> {
        return records
            .flatMap { record ->
                values(record)
                    .map { value -> value.trim() }
                    .filter { value -> value.isNotBlank() }
                    .distinct()
            }
            .groupingBy { it }
            .eachCount()
            .sortedByDescendingValue()
    }

    private fun normalizeSearch(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")
    }

    /*
     * שמות שונים נדרשים משום ש-List<Int> ו-List<Double>
     * מקבלים אותה חתימת JVM לאחר type erasure.
     */
    private fun List<Int>.averageIntOrNull(): Double? {
        if (isEmpty()) return null
        return average()
    }

    private fun List<Double>.averageDoubleOrNull(): Double? {
        if (isEmpty()) return null
        return average()
    }

    private fun <K> Map<K, Int>.sortedByDescendingValue(): Map<K, Int> {
        return entries
            .sortedWith(
                compareByDescending<Map.Entry<K, Int>> {
                    it.value
                }.thenBy {
                    it.key.toString()
                }
            )
            .associate { entry ->
                entry.key to entry.value
            }
    }

    private const val UNKNOWN_BELT = "ללא דרגה"
}