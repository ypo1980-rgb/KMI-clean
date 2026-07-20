package il.kmi.app.screens.coach.statistics

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale

sealed interface NationalStatisticsLoadResult {

    data class Success(
        val records: List<NationalTraineeRecord>
    ) : NationalStatisticsLoadResult

    data class Error(
        val messageHe: String,
        val messageEn: String,
        val technicalMessage: String?
    ) : NationalStatisticsLoadResult
}

/**
 * טוען נתונים ארציים מ-users ב-Firestore.
 *
 * הרשאת הגישה לכל הסניפים חייבת להיות מוגנת גם
 * ב-Firestore Security Rules ולא רק בממשק.
 */
class NationalStatisticsRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun loadAllTrainees(): NationalStatisticsLoadResult {
        if (auth.currentUser == null) {
            return NationalStatisticsLoadResult.Error(
                messageHe = "יש להתחבר מחדש כדי לצפות בסטטיסטיקה הארצית.",
                messageEn = "Please sign in again to view national statistics.",
                technicalMessage = "No authenticated Firebase user"
            )
        }

        return try {
            val documents = loadTraineeDocuments()

            val records = documents
                .mapNotNull(::documentToRecord)
                .distinctBy { it.id }

            NationalStatisticsLoadResult.Success(records)
        } catch (error: Throwable) {
            NationalStatisticsLoadResult.Error(
                messageHe =
                    "לא ניתן לטעון כרגע את נתוני כל הסניפים. " +
                            "ודא שלחשבון יש הרשאה לצפייה ארצית.",
                messageEn =
                    "Unable to load all branch data. " +
                            "Make sure this account has national access.",
                technicalMessage = error.message
            )
        }
    }

    /**
     * תומך בשמות התפקידים הקיימים והישנים.
     * המסמכים מאוחדים לפי document id.
     */
    private suspend fun loadTraineeDocuments(): List<DocumentSnapshot> {
        val roleValues = listOf(
            "trainee",
            "student",
            "מתאמן"
        )

        val documentsById =
            linkedMapOf<String, DocumentSnapshot>()

        var successfulQueries = 0
        var firstError: Throwable? = null

        roleValues.forEach { role ->
            try {
                val snapshot = firestore
                    .collection(USERS_COLLECTION)
                    .whereEqualTo("role", role)
                    .get()
                    .await()

                successfulQueries += 1

                snapshot.documents.forEach { document ->
                    documentsById[document.id] = document
                }
            } catch (error: Throwable) {
                if (firstError == null) {
                    firstError = error
                }
            }
        }

        if (successfulQueries == 0 && firstError != null) {
            throw firstError as Throwable
        }

        return documentsById.values.toList()
    }

    private fun documentToRecord(
        document: DocumentSnapshot
    ): NationalTraineeRecord? {
        val role = document
            .getString("role")
            .orEmpty()
            .trim()
            .lowercase(Locale.ROOT)

        if (
            role !in setOf(
                "trainee",
                "student",
                "מתאמן"
            )
        ) {
            return null
        }

        val fullName = firstNonBlankString(
            document = document,
            keys = listOf(
                "fullName",
                "name",
                "displayName",
                "full_name"
            )
        )

        val branches = readStringSet(
            document = document,
            arrayKeys = listOf(
                "branches",
                "branchNames"
            ),
            singleKeys = listOf(
                "activeBranch",
                "active_branch",
                "branch",
                "coachBranch"
            ),
            csvKeys = listOf(
                "branchesCsv",
                "branches_csv"
            )
        )

        val groups = readStringSet(
            document = document,
            arrayKeys = listOf(
                "groups",
                "groupNames"
            ),
            singleKeys = listOf(
                "activeGroup",
                "active_group",
                "primaryGroup",
                "groupKey",
                "group_key",
                "age_group",
                "group"
            ),
            csvKeys = listOf(
                "groupsCsv",
                "groups_csv"
            )
        )

        /*
         * מסמך ללא שיוך לסניף אינו יכול להשתתף
         * בהשוואת סניפים אמינה.
         */
        if (branches.isEmpty()) {
            return null
        }

        return NationalTraineeRecord(
            id = document.id,
            fullName = fullName,
            branches = branches,
            groups = groups,
            age = readAge(document),
            gender = readGender(document),
            belt = readBelt(document),
            seniorityYears = readSeniorityYears(document),
            attendancePercent = readAttendancePercent(document),
            isActive = readIsActive(document)
        )
    }

    private fun readAge(
        document: DocumentSnapshot
    ): Int? {
        val directAge = firstNumber(
            document = document,
            keys = listOf(
                "age",
                "currentAge",
                "current_age"
            )
        )
            ?.toInt()
            ?.takeIf { it in 1..120 }

        if (directAge != null) {
            return directAge
        }

        val birthValue = firstValue(
            document = document,
            keys = listOf(
                "birthDate",
                "birth_date",
                "dateOfBirth",
                "date_of_birth",
                "birthday"
            )
        )

        val birthDate = parseDateValue(birthValue)
            ?: return null

        val today = LocalDate.now()

        if (birthDate.isAfter(today)) {
            return null
        }

        return java.time.Period
            .between(birthDate, today)
            .years
            .takeIf { it in 1..120 }
    }

    private fun readGender(
        document: DocumentSnapshot
    ): NationalStatsGender {
        val raw = firstNonBlankString(
            document = document,
            keys = listOf(
                "gender",
                "sex",
                "genderName",
                "gender_name",
                "מין"
            )
        )
            .lowercase(Locale.ROOT)
            .replace("_", " ")
            .replace("-", " ")
            .trim()

        return when {
            raw.isBlank() ->
                NationalStatsGender.UNKNOWN

            raw in setOf(
                "male",
                "man",
                "boy",
                "m",
                "זכר",
                "גבר",
                "נער",
                "ילד"
            ) ->
                NationalStatsGender.MALE

            raw in setOf(
                "female",
                "woman",
                "girl",
                "f",
                "נקבה",
                "אישה",
                "נערה",
                "ילדה"
            ) ->
                NationalStatsGender.FEMALE

            raw in setOf(
                "other",
                "non binary",
                "nonbinary",
                "אחר",
                "אחרת"
            ) ->
                NationalStatsGender.OTHER

            else ->
                NationalStatsGender.UNKNOWN
        }
    }

    private fun readBelt(
        document: DocumentSnapshot
    ): String {
        val raw = firstNonBlankString(
            document = document,
            keys = listOf(
                "belt",
                "currentBelt",
                "current_belt",
                "beltName",
                "belt_name",
                "currentBeltName",
                "currentBeltId",
                "beltId",
                "belt_id"
            )
        )
            .lowercase(Locale.ROOT)
            .replace("_", " ")
            .replace("-", " ")
            .trim()

        return when {
            raw.contains("white") || raw.contains("לבנ") ->
                "לבנה"

            raw.contains("yellow") || raw.contains("צהוב") ->
                "צהובה"

            raw.contains("orange") || raw.contains("כתומ") ->
                "כתומה"

            raw.contains("green") || raw.contains("ירוק") ->
                "ירוקה"

            raw.contains("blue") || raw.contains("כחול") ->
                "כחולה"

            raw.contains("brown") || raw.contains("חומ") ->
                "חומה"

            raw.contains("black") || raw.contains("שחור") ->
                "שחורה"

            else ->
                "ללא דרגה"
        }
    }

    private fun readSeniorityYears(
        document: DocumentSnapshot
    ): Double? {
        val directNumeric = firstNumber(
            document = document,
            keys = listOf(
                "seniorityYears",
                "trainingYears",
                "yearsTraining",
                "years_training",
                "experienceYears",
                "experience_years"
            )
        )
            ?.toDouble()
            ?.takeIf { it in 0.0..100.0 }

        if (directNumeric != null) {
            return directNumeric
        }

        val textValue = firstNonBlankString(
            document = document,
            keys = listOf(
                "seniority",
                "trainingSeniority",
                "training_seniority",
                "experience",
                "trainingExperience"
            )
        )

        val yearsFromText = Regex("""\d+(?:[.,]\d+)?""")
            .find(textValue)
            ?.value
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?.takeIf { it in 0.0..100.0 }

        if (yearsFromText != null) {
            return yearsFromText
        }

        val startValue = firstValue(
            document = document,
            keys = listOf(
                "trainingStartDate",
                "training_start_date",
                "startTrainingDate",
                "startedTrainingAt"
            )
        )

        val startDate = parseDateValue(startValue)
            ?: return null

        val today = LocalDate.now()

        if (startDate.isAfter(today)) {
            return null
        }

        val months = java.time.Period
            .between(startDate, today)
            .let { period ->
                period.years * 12 + period.months
            }

        return months
            .toDouble()
            .div(12.0)
            .takeIf { it in 0.0..100.0 }
    }

    /**
     * אם הנוכחות אינה נשמרת ב-users, מחזירים null.
     * אסור לפרש מידע חסר כ־0%.
     */
    private fun readAttendancePercent(
        document: DocumentSnapshot
    ): Int? {
        return firstNumber(
            document = document,
            keys = listOf(
                "attendancePercent",
                "attendancePct",
                "attendance_percentage",
                "attendanceRate",
                "attendance_rate"
            )
        )
            ?.toInt()
            ?.coerceIn(0, 100)
    }

    private fun readIsActive(
        document: DocumentSnapshot
    ): Boolean {
        val booleanValue = listOf(
            "isActive",
            "active",
            "is_active"
        )
            .firstNotNullOfOrNull { key ->
                document.getBoolean(key)
            }

        if (booleanValue != null) {
            return booleanValue
        }

        val status = firstNonBlankString(
            document = document,
            keys = listOf(
                "status",
                "accountStatus",
                "account_status",
                "membershipStatus"
            )
        )
            .lowercase(Locale.ROOT)
            .trim()

        return status !in setOf(
            "inactive",
            "disabled",
            "deleted",
            "suspended",
            "לא פעיל",
            "מושבת"
        )
    }

    private fun readStringSet(
        document: DocumentSnapshot,
        arrayKeys: List<String>,
        singleKeys: List<String>,
        csvKeys: List<String>
    ): Set<String> {
        val values = mutableSetOf<String>()

        arrayKeys.forEach { key ->
            val rawList = document.get(key) as? List<*>

            rawList
                ?.mapNotNull { it?.toString() }
                ?.flatMap(::splitMultiValue)
                ?.forEach(values::add)
        }

        singleKeys.forEach { key ->
            document.getString(key)
                ?.let(::splitMultiValue)
                ?.forEach(values::add)
        }

        csvKeys.forEach { key ->
            document.getString(key)
                ?.let(::splitMultiValue)
                ?.forEach(values::add)
        }

        return values
            .map(::normalizeLabel)
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun splitMultiValue(
        value: String
    ): List<String> {
        return value
            .split(",", ";", "|")
            .map(::normalizeLabel)
            .filter { it.isNotBlank() }
    }

    private fun normalizeLabel(
        value: String
    ): String {
        return value
            .trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")
    }

    private fun firstNonBlankString(
        document: DocumentSnapshot,
        keys: List<String>
    ): String {
        return keys
            .firstNotNullOfOrNull { key ->
                document.getString(key)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
            .orEmpty()
    }

    private fun firstValue(
        document: DocumentSnapshot,
        keys: List<String>
    ): Any? {
        return keys
            .firstNotNullOfOrNull { key ->
                document.get(key)
            }
    }

    private fun firstNumber(
        document: DocumentSnapshot,
        keys: List<String>
    ): Number? {
        return keys
            .firstNotNullOfOrNull { key ->
                document.get(key) as? Number
            }
    }

    private fun parseDateValue(
        value: Any?
    ): LocalDate? {
        return when (value) {
            is Timestamp ->
                value.toDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

            is Date ->
                value.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

            is Number ->
                parseEpochMillis(value.toLong())

            is String ->
                parseDateString(value)

            else ->
                null
        }
    }

    private fun parseEpochMillis(
        rawValue: Long
    ): LocalDate? {
        if (rawValue <= 0L) return null

        /*
         * ערך קטן יחסית כנראה נשמר בשניות Unix.
         */
        val epochMillis = if (rawValue < 10_000_000_000L) {
            rawValue * 1_000L
        } else {
            rawValue
        }

        return runCatching {
            Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.getOrNull()
    }

    private fun parseDateString(
        value: String
    ): LocalDate? {
        val clean = value.trim()
        if (clean.isBlank()) return null

        val formatters = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy")
        )

        formatters.forEach { formatter ->
            try {
                return LocalDate.parse(clean, formatter)
            } catch (_: DateTimeParseException) {
                // מנסים את הפורמט הבא.
            }
        }

        return null
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}