package il.kmi.app.screens.coach

import java.util.Locale
import kotlin.math.roundToInt

data class CoachDateEntry(
    val date: String = "",
    val description: String = ""
)

data class TraineeProfile(
    val id: String,
    val fullName: String,
    val belt: String,
    val seniority: String,
    val age: Int,
    val attendancePct: Int = 0,
    val branch: String = "",
    val groupKey: String = "",

    /*
     * משמשים לאיחוד רשומות כפולות של אותו מתאמן.
     */
    val email: String = "",
    val phone: String = "",

    /*
     * מזהה המסמך האמיתי ב-Firestore:
     * users/{userDocId}
     */
    val userDocId: String = "",

    val beltAwardDates: Map<String, String> =
        emptyMap(),

    val beltAwardDescriptions: Map<String, String> =
        emptyMap(),

    val coachNotes: String = "",

    val seminarDates: Map<String, CoachDateEntry> =
        emptyMap(),

    val campDates: Map<String, CoachDateEntry> =
        emptyMap(),

    val certificationDates: Map<String, CoachDateEntry> =
        emptyMap()
)

data class GroupStatsUi(
    val totalTrainees: Int,
    val filteredTrainees: Int,
    val avgAge: Int,
    val avgAttendance: Int,
    val beltCounts: Map<String, Int>,
    val highAttendanceCount: Int,
    val avgSeniority: Double
)

internal fun nextCoachDateItemName(
    sectionTitle: String,
    index: Int
): String {
    return when (sectionTitle) {
        "השתלמויות" ->
            "השתלמות $index"

        "מחנות אימונים" ->
            "מחנה אימונים $index"

        "הסמכות" ->
            "הסמכה $index"

        else ->
            "פריט $index"
    }
}

internal fun parseYearsFromSeniority(
    value: String
): Int? {
    val digits = Regex("""\d+""")
        .find(value)
        ?.value
        ?: return null

    return digits.toIntOrNull()
}

internal fun formatAvgSeniority(
    value: Double,
    isEnglish: Boolean
): String {
    if (value <= 0.0) return "—"

    val formatted = String.format(
        Locale.US,
        "%.1f",
        value
    )

    return if (isEnglish) {
        "$formatted yrs"
    } else {
        "$formatted שנים"
    }
}

internal fun buildGroupStats(
    profiles: List<TraineeProfile>,
    filtered: List<TraineeProfile>
): GroupStatsUi {
    val validAges = filtered
        .map { it.age }
        .filter { it > 0 }

    val avgAge = validAges
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.roundToInt()
        ?: 0

    /*
     * אחוז נוכחות 0 אינו נכלל בממוצע הקיים,
     * כדי לשמור על ההתנהגות הנוכחית של המסך.
     */
    val attendanceValues = filtered
        .map { it.attendancePct }
        .filter { it > 0 }

    val avgAttendance = attendanceValues
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.roundToInt()
        ?: 0

    val seniorityValues = filtered
        .mapNotNull { profile ->
            parseYearsFromSeniority(
                profile.seniority
            )?.toDouble()
        }
        .filter { it > 0.0 }

    val avgSeniority = seniorityValues
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.let { average ->
            (average * 10.0).roundToInt() / 10.0
        }
        ?: 0.0

    val beltCounts = filtered
        .groupingBy { profile ->
            profile.belt.ifBlank {
                "ללא דרגה"
            }
        }
        .eachCount()
        .toList()
        .sortedByDescending { (_, count) ->
            count
        }
        .toMap()

    val highAttendanceCount = filtered.count {
        it.attendancePct >= 80
    }

    return GroupStatsUi(
        totalTrainees = profiles.size,
        filteredTrainees = filtered.size,
        avgAge = avgAge,
        avgAttendance = avgAttendance,
        beltCounts = beltCounts,
        highAttendanceCount =
            highAttendanceCount,
        avgSeniority = avgSeniority
    )
}