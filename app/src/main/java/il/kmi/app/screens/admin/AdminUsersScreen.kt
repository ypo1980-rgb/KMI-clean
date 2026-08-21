package il.kmi.app.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.shared.domain.Belt
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.ext.color
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.app.screens.registration.CoachBranchAssignment
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable

//=====================================================================

private fun adminTr(isEnglish: Boolean, he: String, en: String): String =
    if (isEnglish) en else he

private fun adminTextAlign(isEnglish: Boolean): androidx.compose.ui.text.style.TextAlign =
    if (isEnglish) androidx.compose.ui.text.style.TextAlign.Left else androidx.compose.ui.text.style.TextAlign.Right

private fun adminGenderLabel(raw: String?, isEnglish: Boolean): String {
    val clean = raw.orEmpty().trim().lowercase()

    return when {
        clean.startsWith("m") || clean == "male" || clean == "זכר" ->
            adminTr(isEnglish, "זכר", "Male")

        clean.startsWith("f") || clean == "female" || clean == "נקבה" ->
            adminTr(isEnglish, "נקבה", "Female")

        else -> adminTr(isEnglish, "לא ידוע", "Unknown")
    }
}

private fun adminAgeBucketLabel(bucket: String, isEnglish: Boolean): String {
    return when (bucket) {
        "לא ידוע" -> adminTr(isEnglish, "לא ידוע", "Unknown")
        else -> bucket
    }
}

private fun Int?.orEmptyCount(): Int = this ?: 0

private fun adminMillisFromFirestore(value: Any?): Long? {
    val rawMillis = when (value) {
        is Timestamp -> value.toDate().time
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    } ?: return null

    // אם בטעות נשמר timestamp בשניות ולא במילישניות
    val millis = if (rawMillis in 1_000_000_000L..9_999_999_999L) {
        rawMillis * 1000L
    } else {
        rawMillis
    }

    // לא מציגים תאריכים שבורים כמו 0 / 1970
    val minReasonableMillis = 1_577_836_800_000L // 01.01.2020
    val maxReasonableMillis =
        System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L

    return millis.takeIf {
        it in minReasonableMillis..maxReasonableMillis
    }
}

// ======================================================
//  מודל נתוני משתמש למנהל – ממולא מ-Firestore
// ======================================================
data class AdminUserRecord(
    val id: String,
    val uidField: String?,
    val fullName: String,
    val gender: String?,
    val birthDay: Int?,
    val birthMonth: Int?,
    val birthYear: Int?,
    val region: String?,
    val branch: String?,
    val branches: List<String> = emptyList(),
    val groups: List<String>,

    /*
     * המבנה החדש: הקבוצות השייכות לכל סניף.
     */
    val branchAssignments:
    List<CoachBranchAssignment> = emptyList(),

    val currentBeltId: String?,
    val phone: String?,
    val email: String?,

    // ✅ חדש: שדות לזיהוי מאמן/מתאמן
    val role: String? = null,
    val isCoachFlag: Boolean? = null,

    val createdAtMillis: Long?,

    // ✅ נתוני שימוש באפליקציה
    val appOpenCount: Int = 0,
    val lastSeenAtMillis: Long? = null
) {

    data class AssistantQuestionRecord(
        val id: String,
        val question: String,
        val answer: String? = null,
        val createdAtMillis: Long? = null,
        val userName: String? = null,
        val userUid: String? = null
    )

    // חישוב גיל מדויק לפי שנה + חודש + יום
    val age: Int?
        get() {
            val year = birthYear ?: return null

            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)

            var calculatedAge = currentYear - year

            val month = birthMonth
            val day = birthDay

            if (
                month != null &&
                day != null &&
                month in 1..12 &&
                day in 1..31
            ) {
                val currentMonth = now.get(Calendar.MONTH) + 1
                val currentDay = now.get(Calendar.DAY_OF_MONTH)

                if (
                    currentMonth < month ||
                    (currentMonth == month && currentDay < day)
                ) {
                    calculatedAge--
                }
            }

            return calculatedAge.takeIf { it in 0..120 }
        }

    val ageBucket: String
        get() {
            val a = age ?: return "לא ידוע"
            return when (a) {
                in 0..12 -> "0–12"
                in 13..17 -> "13–17"
                in 18..25 -> "18–25"
                in 26..40 -> "26–40"
                in 41..60 -> "41–60"
                else -> "60+"
            }
        }

    val belt: Belt?
        get() = currentBeltId?.let { Belt.fromId(it) }

    // ✅ חדש: חישוב מאמן (תומך בכמה שיטות שמירה ב-Firestore)
    val isCoach: Boolean
        get() {
            if (isCoachFlag == true) return true

            val r = role?.trim()?.lowercase()
            if (r != null) {
                if (r in listOf("coach", "trainer", "instructor", "admin_coach")) return true
                if ("coach" in r || "trainer" in r || "instructor" in r) return true
                if ("מאמן" in r) return true
            }

            // fallback לפי groups
            val g = groups.joinToString(" ").lowercase()
            return ("מאמן" in g) || ("מאמנים" in g) || ("coach" in g) || ("coaches" in g) || ("trainer" in g)
        }
} // ✅ חשוב: סגירת AdminUserRecord כאן

/**
 * מפתח דה-דופ – מאחד מסמכים של אותו משתמש:
 * קודם לפי uid, אם אין אז לפי מייל, אם אין אז לפי טלפון, ואם אין – לפי שם.
 */
// קודם מייל, אחר כך טלפון, ורק אם אין – uid / שם
private fun traineeRankDisplayName(
    rawId: String?,
    isEnglish: Boolean = false
): String {
    return when (rawId?.trim().orEmpty()) {
        "white" -> adminTr(isEnglish, "לבנה", "White")
        "yellow" -> adminTr(isEnglish, "צהובה", "Yellow")
        "orange" -> adminTr(isEnglish, "כתומה", "Orange")
        "green" -> adminTr(isEnglish, "ירוקה", "Green")
        "blue" -> adminTr(isEnglish, "כחולה", "Blue")
        "brown" -> adminTr(isEnglish, "חומה", "Brown")

        "black",
        "שחורה",
        "שחורה דאן 1" -> adminTr(isEnglish, "שחורה דאן 1", "Black Dan 1")

        "black_dan_2" -> adminTr(isEnglish, "שחורה דאן 2", "Black Dan 2")
        "black_dan_3" -> adminTr(isEnglish, "שחורה דאן 3", "Black Dan 3")
        "black_dan_4" -> adminTr(isEnglish, "שחורה דאן 4", "Black Dan 4")
        "black_dan_5" -> adminTr(isEnglish, "שחורה דאן 5", "Black Dan 5")
        "black_dan_6" -> adminTr(isEnglish, "שחורה דאן 6", "Black Dan 6")
        "black_dan_7" -> adminTr(isEnglish, "שחורה דאן 7", "Black Dan 7")
        "black_dan_8" -> adminTr(isEnglish, "שחורה דאן 8", "Black Dan 8")
        "black_dan_9" -> adminTr(isEnglish, "שחורה דאן 9", "Black Dan 9")
        "black_dan_10" -> adminTr(isEnglish, "שחורה דאן 10", "Black Dan 10")

        else -> ""
    }
}

private fun traineeRankSortIndex(rawId: String?): Int {
    return when (rawId?.trim().orEmpty()) {
        "white" -> 0
        "yellow" -> 1
        "orange" -> 2
        "green" -> 3
        "blue" -> 4
        "brown" -> 5

        "black",
        "שחורה",
        "שחורה דאן 1" -> 6

        "black_dan_2" -> 7
        "black_dan_3" -> 8
        "black_dan_4" -> 9
        "black_dan_5" -> 10
        "black_dan_6" -> 11
        "black_dan_7" -> 12
        "black_dan_8" -> 13
        "black_dan_9" -> 14
        "black_dan_10" -> 15

        else -> 99
    }
}

private fun traineeRankColor(rawId: String?): Color {
    return when {
        rawId?.startsWith("black_dan_") == true -> Belt.BLACK.color
        rawId == "black" || rawId == "שחורה" || rawId == "שחורה דאן 1" -> Belt.BLACK.color
        else -> Belt.fromId(rawId.orEmpty())?.color ?: Color(0xFF6B7280)
    }
}

private fun traineeRankOrderedIds(): List<String> {
    return listOf(
        "",
        "white",
        "yellow",
        "orange",
        "green",
        "blue",
        "brown",
        "black",
        "black_dan_2",
        "black_dan_3",
        "black_dan_4",
        "black_dan_5",
        "black_dan_6",
        "black_dan_7",
        "black_dan_8",
        "black_dan_9",
        "black_dan_10"
    )
}

private fun traineeRankLabelFromOrderedId(
    rawId: String,
    isEnglish: Boolean
): String {
    return if (rawId.isBlank()) {
        adminTr(isEnglish, "ללא חגורה", "No belt")
    } else {
        traineeRankDisplayName(rawId, isEnglish).ifBlank {
            adminTr(isEnglish, "ללא חגורה", "No belt")
        }
    }
}

/**
 * בודק האם שתי רשומות Firestore שייכות לאותו משתמש.
 *
 * התאמה באחד מהמזהים החזקים מספיקה:
 * UID / אימייל / טלפון.
 *
 * שם משמש רק כאשר לשתי הרשומות אין שום מזהה חזק,
 * כדי לא לאחד בטעות שני אנשים בעלי אותו שם.
 */
private fun AdminUserRecord.isSameAdminUser(
    other: AdminUserRecord
): Boolean {

    fun normalizeName(value: String): String {
        return value
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    fun normalizeEmail(value: String?): String {
        return value
            .orEmpty()
            .trim()
            .lowercase()
    }

    fun normalizePhone(value: String?): String {
        val digits =
            value
                .orEmpty()
                .filter { it.isDigit() }

        return when {
            digits.startsWith("972") && digits.length >= 11 ->
                "0" + digits.removePrefix("972")

            digits.startsWith("00972") && digits.length >= 13 ->
                "0" + digits.removePrefix("00972")

            else ->
                digits
        }
    }

    // --------------------------------------------------
    // 1. UID
    //
    // בחלק מהמסמכים ה-UID נמצא בשדה uid/userId,
    // ובחלק מהמסמכים Document ID עצמו הוא ה-UID.
    // לכן משווים את כל הצירופים האפשריים.
    // --------------------------------------------------

    val thisUidField = uidField.orEmpty().trim()
    val otherUidField = other.uidField.orEmpty().trim()

    val thisDocumentId = id.trim()
    val otherDocumentId = other.id.trim()

    if (
        thisUidField.isNotBlank() &&
        otherUidField.isNotBlank() &&
        thisUidField == otherUidField
    ) {
        return true
    }

    if (
        thisUidField.isNotBlank() &&
        otherDocumentId.isNotBlank() &&
        thisUidField == otherDocumentId
    ) {
        return true
    }

    if (
        otherUidField.isNotBlank() &&
        thisDocumentId.isNotBlank() &&
        otherUidField == thisDocumentId
    ) {
        return true
    }

    if (
        thisDocumentId.isNotBlank() &&
        otherDocumentId.isNotBlank() &&
        thisDocumentId == otherDocumentId
    ) {
        return true
    }

    // --------------------------------------------------
    // 2. אימייל
    // --------------------------------------------------

    val thisEmail = normalizeEmail(email)
    val otherEmail = normalizeEmail(other.email)

    if (
        thisEmail.isNotBlank() &&
        otherEmail.isNotBlank() &&
        thisEmail == otherEmail
    ) {
        return true
    }

    // --------------------------------------------------
    // 3. טלפון
    // --------------------------------------------------

    val thisPhone = normalizePhone(phone)
    val otherPhone = normalizePhone(other.phone)

    if (
        thisPhone.isNotBlank() &&
        otherPhone.isNotBlank() &&
        thisPhone == otherPhone
    ) {
        return true
    }

    // --------------------------------------------------
    // 4. שם מלא + תאריך לידה
    //
    // זה מטפל במקרה שבו נוצרו שתי רשומות לאותו אדם
    // בלי UID/אימייל/טלפון משותף, אבל פרטי האדם זהים.
    // לא מאחדים לפי שם בלבד כאשר יש לנו מידע נוסף.
    // --------------------------------------------------

    val thisName = normalizeName(fullName)
    val otherName = normalizeName(other.fullName)

    val sameName =
        thisName.isNotBlank() &&
                otherName.isNotBlank() &&
                thisName == otherName

    val thisHasBirthDate =
        birthYear != null &&
                birthMonth != null &&
                birthDay != null

    val otherHasBirthDate =
        other.birthYear != null &&
                other.birthMonth != null &&
                other.birthDay != null

    // התאמה מלאה: שם + תאריך לידה מלא
    if (
        sameName &&
        thisHasBirthDate &&
        otherHasBirthDate &&
        birthYear == other.birthYear &&
        birthMonth == other.birthMonth &&
        birthDay == other.birthDay
    ) {
        return true
    }

    // התאמה לרשומות ישנות שבהן נשמרה רק שנת לידה.
    //
    // אם השם המלא זהה וגם שנת הלידה זהה,
    // מדובר בסבירות גבוהה מאוד באותו משתמש.
    if (
        sameName &&
        birthYear != null &&
        other.birthYear != null &&
        birthYear == other.birthYear
    ) {
        return true
    }

    // אם קיימים חודש ושנה בשתי הרשומות,
    // גם הם מספיקים יחד עם שם מלא זהה.
    if (
        sameName &&
        birthYear != null &&
        other.birthYear != null &&
        birthMonth != null &&
        other.birthMonth != null &&
        birthYear == other.birthYear &&
        birthMonth == other.birthMonth
    ) {
        return true
    }

    // --------------------------------------------------
    // 5. אם לשתי הרשומות אין בכלל מזהה חזק,
    // שם מלא זהה הוא fallback אחרון.
    // --------------------------------------------------

    val thisHasStrongIdentity =
        thisUidField.isNotBlank() ||
                thisEmail.isNotBlank() ||
                thisPhone.isNotBlank()

    val otherHasStrongIdentity =
        otherUidField.isNotBlank() ||
                otherEmail.isNotBlank() ||
                otherPhone.isNotBlank()

    if (
        sameName &&
        !thisHasStrongIdentity &&
        !otherHasStrongIdentity
    ) {
        return true
    }

    return false
}

/**
 * מאחד את כל המסמכים ששייכים לאותו משתמש.
 *
 * פרטי הפרופיל נלקחים מהרשומה העדכנית ביותר,
 * אבל סניפים וקבוצות מתאחדים מכל הרשומות.
 * שימוש אחרון נלקח לפי הזמן המאוחר ביותר,
 * וותק נלקח לפי תאריך היצירה המוקדם ביותר.
 */
private fun mergeAdminUserRecords(
    records: List<AdminUserRecord>
): AdminUserRecord {
    val newest =
        records.maxWithOrNull(
            compareBy<AdminUserRecord> {
                it.lastSeenAtMillis ?: 0L
            }.thenBy {
                it.createdAtMillis ?: 0L
            }
        ) ?: records.first()

    fun newestNonBlank(
        selector: (AdminUserRecord) -> String?
    ): String? {
        return records
            .sortedWith(
                compareByDescending<AdminUserRecord> { record ->
                    // רשומה עם פרטי פרופיל אמיתיים מקבלת עדיפות
                    val value = selector(record).orEmpty().trim()

                    if (
                        value.isNotBlank() &&
                        !value.startsWith(
                            "Unknown user",
                            ignoreCase = true
                        )
                    ) {
                        1
                    } else {
                        0
                    }
                }.thenByDescending {
                    it.lastSeenAtMillis ?: 0L
                }.thenByDescending {
                    it.createdAtMillis ?: 0L
                }
            )
            .firstNotNullOfOrNull { record ->
                selector(record)
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank() &&
                                !it.equals(
                                    "null",
                                    ignoreCase = true
                                ) &&
                                it != "—"
                    }
            }
    }

    val mergedBranches =
        records
            .flatMap { record ->
                buildList {
                    record.branch
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { add(it) }

                    addAll(
                        record.branches
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    )
                }
            }
            .distinctBy { it.lowercase() }

    val mergedGroups =
        records
            .flatMap { it.groups }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

    val mergedBranchAssignments =
        records
            .flatMap {
                it.branchAssignments
            }
            .groupBy { assignment ->
                assignment.branch
                    .trim()
                    .replace('־', '-')
                    .replace('–', '-')
                    .replace('—', '-')
                    .replace(Regex("\\s+"), " ")
                    .lowercase()
            }
            .mapNotNull { (_, assignments) ->
                val branchName =
                    assignments
                        .firstOrNull()
                        ?.branch
                        ?.trim()
                        .orEmpty()

                if (branchName.isBlank()) {
                    null
                } else {
                    CoachBranchAssignment(
                        branch = branchName,
                        groups =
                            assignments
                                .flatMap {
                                    it.groups
                                }
                                .map {
                                    it.trim()
                                }
                                .filter {
                                    it.isNotBlank()
                                }
                                .distinctBy {
                                    it.lowercase()
                                }
                    ).sanitized()
                }
            }

    return newest.copy(
        uidField = newestNonBlank { it.uidField },
        fullName =
            newestNonBlank { record ->
                record.fullName.takeUnless {
                    it.startsWith(
                        "Unknown user",
                        ignoreCase = true
                    )
                }
            } ?: newest.fullName,
        gender = newestNonBlank { it.gender },

        birthDay =
            records.firstNotNullOfOrNull { it.birthDay },
        birthMonth =
            records.firstNotNullOfOrNull { it.birthMonth },
        birthYear =
            records.firstNotNullOfOrNull { it.birthYear },

        region = newestNonBlank { it.region },

        branch =
            newestNonBlank { it.branch }
                ?: mergedBranches.firstOrNull(),

        branches =
            if (
                mergedBranchAssignments
                    .isNotEmpty()
            ) {
                mergedBranchAssignments
                    .map {
                        it.branch
                    }
            } else {
                mergedBranches
            },

        groups =
            if (
                mergedBranchAssignments
                    .isNotEmpty()
            ) {
                mergedBranchAssignments
                    .flatMap {
                        it.groups
                    }
                    .distinctBy {
                        it.lowercase()
                    }
            } else {
                mergedGroups
            },

        branchAssignments =
            mergedBranchAssignments,

        currentBeltId =
            newestNonBlank { it.currentBeltId },

        phone =
            newestNonBlank { it.phone },

        email =
            newestNonBlank { it.email },

        role =
            newestNonBlank { it.role },

        isCoachFlag =
            when {
                records.any { it.isCoachFlag == true } -> true
                records.any { it.isCoachFlag == false } -> false
                else -> null
            },

        createdAtMillis =
            records
                .mapNotNull { it.createdAtMillis }
                .minOrNull(),

        appOpenCount =
            records
                .maxOfOrNull { it.appOpenCount }
                ?: 0,

        lastSeenAtMillis =
            records
                .mapNotNull { it.lastSeenAtMillis }
                .maxOrNull()
    )
}

/**
 * מבצע איחוד טרנזיטיבי:
 * גם אם A תואם ל-B לפי מייל ו-B תואם ל-C לפי טלפון,
 * שלושתם ייחשבו משתמש אחד.
 */
private fun mergeDuplicateAdminUsers(
    users: List<AdminUserRecord>
): List<AdminUserRecord> {
    val groups =
        mutableListOf<MutableList<AdminUserRecord>>()

    users.forEach { user ->
        val matchingIndexes =
            groups.indices.filter { index ->
                groups[index].any { existing ->
                    existing.isSameAdminUser(user)
                }
            }

        if (matchingIndexes.isEmpty()) {
            groups += mutableListOf(user)
        } else {
            val targetIndex = matchingIndexes.first()

            groups[targetIndex].add(user)

            matchingIndexes
                .drop(1)
                .sortedDescending()
                .forEach { index ->
                    groups[targetIndex]
                        .addAll(groups[index])

                    groups.removeAt(index)
                }
        }
    }

    return groups
        .map { records ->
            mergeAdminUserRecords(records)
        }
}

private fun AdminUserRecord.hasRealAdminUserContent(): Boolean {
    val cleanName = fullName.trim()
    val cleanEmail = email.orEmpty().trim()
    val cleanPhoneDigits = phone.orEmpty().filter { it.isDigit() }
    val cleanUid = uidField.orEmpty().trim()
    val cleanBranch = branch.orEmpty().trim()
    val hasBranches = branches.any { it.trim().isNotBlank() }
    val cleanRegion = region.orEmpty().trim()
    val cleanBelt = currentBeltId.orEmpty().trim()

    // מסמכים שנוצרו בלי פרטי משתמש אמיתיים לא יוצגו במסך הניהול
    if (cleanName.startsWith("Unknown user", ignoreCase = true) &&
        cleanEmail.isBlank() &&
        cleanPhoneDigits.isBlank()
    ) {
        return false
    }

    // אם אין שום פרט מזהה/תצוגה משמעותי — לא מציגים
    return cleanName.isNotBlank() ||
            cleanEmail.isNotBlank() ||
            cleanPhoneDigits.isNotBlank() ||
            cleanUid.isNotBlank() ||
            cleanBranch.isNotBlank() ||
            hasBranches ||
            cleanRegion.isNotBlank() ||
            cleanBelt.isNotBlank() ||
            groups.isNotEmpty()
}

private fun DocumentSnapshot
        .adminBranchAssignments():
        List<CoachBranchAssignment> {

    val rawAssignments =
        get("coachBranchAssignments")
                as? List<*>
            ?: return emptyList()

    val parsedAssignments =
        rawAssignments.mapNotNull {
                rawAssignment ->

            val assignmentMap =
                rawAssignment as? Map<*, *>
                    ?: return@mapNotNull null

            val branchName =
                assignmentMap["branch"]
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            if (branchName.isBlank()) {
                return@mapNotNull null
            }

            val groupNames =
                (
                        assignmentMap["groups"]
                                as? List<*>
                        )
                    ?.mapNotNull { rawGroup ->
                        rawGroup
                            ?.toString()
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                    }
                    ?.distinctBy {
                        it.lowercase()
                    }
                    .orEmpty()

            CoachBranchAssignment(
                branch = branchName,
                groups = groupNames
            ).sanitized()
        }

    /*
     * אם קיימות כמה רשומות לאותו סניף,
     * מאחדים את הקבוצות ולא מוחקים אף שיוך.
     */
    return parsedAssignments
        .groupBy { assignment ->
            assignment.branch
                .trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .lowercase()
        }
        .mapNotNull { (_, assignments) ->
            val branchName =
                assignments
                    .firstOrNull()
                    ?.branch
                    ?.trim()
                    .orEmpty()

            if (branchName.isBlank()) {
                null
            } else {
                CoachBranchAssignment(
                    branch = branchName,
                    groups =
                        assignments
                            .flatMap {
                                it.groups
                            }
                            .map {
                                it.trim()
                            }
                            .filter {
                                it.isNotBlank()
                            }
                            .distinctBy {
                                it.lowercase()
                            }
                ).sanitized()
            }
        }
}

/**
 * המרה של מסמך Firestore למודל AdminUserRecord
 * מנסה לתמוך במספר שמות אפשריים לשדות.
 */
private fun DocumentSnapshot.toAdminUserRecord(): AdminUserRecord? {
    fun intOrNull(field: String): Int? =
        when (val v = get(field)) {
            is Long -> v.toInt()
            is Int -> v
            is Double -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }

    fun intFromAnyField(vararg keys: String): Int {
        return keys
            .mapNotNull { key ->
                intOrNull(key)
                    ?.takeIf { it >= 0 }
            }
            .maxOrNull()
            ?: 0
    }

    fun stringOrNull(vararg keys: String): String? {
        for (k in keys) {
            val v = get(k)
            if (v is String && v.isNotBlank()) return v
        }
        return null
    }

    fun boolOrNull(vararg keys: String): Boolean? {
        for (k in keys) {
            val v = get(k)
            when (v) {
                is Boolean -> return v
                is String -> v.trim().lowercase().let {
                    if (it == "true") return true
                    if (it == "false") return false
                }
            }
        }
        return null
    }

    fun stringListOrEmpty(vararg keys: String): List<String> {
        for (k in keys) {
            val v = get(k)

            when (v) {
                is List<*> -> {
                    val list = v
                        .mapNotNull { it?.toString()?.trim() }
                        .filter { it.isNotBlank() }

                    if (list.isNotEmpty()) return list
                }

                is String -> {
                    val list = v
                        .split(",", "•", "|", ";")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    if (list.isNotEmpty()) return list
                }
            }
        }

        return emptyList()
    }

    val name =
        stringOrNull("fullName", "name", "displayName", "userName", "username", "full_name")
            ?: stringOrNull("email")
            ?: stringOrNull("phone", "phoneNumber")
            ?: id.take(6).let { "Unknown user ($it)" }

    // --- תאריך לידה: קודם מנסים שדות נפרדים, ואם אין – מפענחים birthDate ---
    var birthYear  = intOrNull("birthYear")
    var birthMonth = intOrNull("birthMonth")
    var birthDay   = intOrNull("birthDay")

    val birthDateStr = get("birthDate") as? String
    if (birthDateStr != null && Regex("""\d{4}-\d{2}-\d{2}""").matches(birthDateStr)) {
        val parts = birthDateStr.split("-")
        if (birthYear  == null) birthYear  = parts.getOrNull(0)?.toIntOrNull()
        if (birthMonth == null) birthMonth = parts.getOrNull(1)?.toIntOrNull()
        if (birthDay   == null) birthDay   = parts.getOrNull(2)?.toIntOrNull()
    }

    // uid של המשתמש מתוך המסמך (אם קיים)
    // תומך גם בשמות ישנים/חלופיים של אותו שדה.
    val uidField = stringOrNull(
        "uid",
        "userId",
        "user_id",
        "firebaseUid",
        "firebase_uid",
        "authUid",
        "auth_uid"
    )

    val groupsList =
        stringListOrEmpty(
            "groups",
            "selectedGroups",
            "selected_groups",
            "ageGroups",
            "age_groups",
            "age_group",
            "group",
            "trainingGroups",
            "trainingGroup"
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

    // createdAt יכול להיות בשם ישן או חדש
    val createdMillis = adminMillisFromFirestore(
        get("createdAtMillis") ?: get("createdAt")
    )

    val role = stringOrNull("role", "userType", "type")
    val isCoachFlag = boolOrNull("isCoach", "coach", "isTrainer", "trainer")

    val branchValue = stringOrNull(
        "branch",
        "branchName",
        "selectedBranch",
        "selectedBranchName",
        "trainingBranch",
        "trainingBranchName",
        "club",
        "dojo"
    )

    val branchList = stringListOrEmpty(
        "branches",
        "branchNames",
        "selectedBranches",
        "selectedBranchNames",
        "trainingBranches",
        "trainingBranchNames",
        "clubs",
        "dojos"
    )

    val branchAssignments =
        adminBranchAssignments()

    return AdminUserRecord(
        id = id,
        uidField = uidField,
        fullName = name,
        gender = stringOrNull("gender", "sex"),
        birthDay = birthDay,
        birthMonth = birthMonth,
        birthYear = birthYear,
        region = stringOrNull("region", "area", "selectedRegion", "trainingRegion"),
        branch = branchValue,
        branches =
            if (branchAssignments.isNotEmpty()) {
                branchAssignments
                    .map { it.branch }
                    .distinctBy {
                        it.lowercase()
                    }
            } else {
                branchList
            },
        groups =
            if (branchAssignments.isNotEmpty()) {
                branchAssignments
                    .flatMap { it.groups }
                    .distinctBy {
                        it.lowercase()
                    }
            } else {
                groupsList
            },
        branchAssignments =
            branchAssignments,
        currentBeltId = stringOrNull(
            "currentBeltId",
            "currentBelt",
            "belt_current",
            "beltId",
            "belt"
        ),
        phone = stringOrNull("phone", "phoneNumber"),
        email = stringOrNull("email"),

        // ✅ חדש
        role = role,
        isCoachFlag = isCoachFlag,

        createdAtMillis = createdMillis,

        // ✅ מספר שימושים באפליקציה.
        //
        // קיימות גרסאות שונות של האפליקציה ששמרו
        // את המונה בשמות שונים, ולכן קוראים את כולם
        // ולוקחים את הערך הגבוה ביותר.
        //
        // screenViewCount לא נכלל בכוונה:
        // צפייה במסכים אינה שוות ערך לשימוש/פתיחת אפליקציה.
        appOpenCount = intFromAnyField(
            "appOpenCount",
            "app_open_count",
            "appOpens",
            "app_opens",
            "openCount",
            "open_count",
            "opensCount",
            "launchCount",
            "launch_count",
            "appLaunchCount",
            "app_launch_count",
            "usageCount",
            "usage_count",
            "sessionsCount",
            "sessions_count",
            "sessionCount",
            "session_count",
            "loginCount",
            "login_count"
        ),

        // ✅ שימוש אחרון אמיתי באפליקציה.
        // updatedAt אינו מתאים כאן כי גם שמירת פרופיל
        // או עדכון נתונים יכולים לשנות אותו.
        lastSeenAtMillis = adminMillisFromFirestore(
            get("lastSeenAtMillis")
                ?: get("lastSeenAt")
                ?: get("lastOpenAtMillis")
                ?: get("lastOpenAt")
                ?: get("lastUsedAtMillis")
                ?: get("lastUsedAt")
                ?: get("lastLoginAtMillis")
                ?: get("lastLoginAt")
        )
    )
}

data class AdminUsersPreloadResult(
    val users: List<AdminUserRecord>,
    val unlikeQuestions: List<AdminUserRecord.AssistantQuestionRecord>,
    val errorMessage: String?
)

object AdminUsersPreloadCache {

    private const val FRESH_WINDOW_MILLIS = 5 * 60 * 1000L

    private var loadedAtMillis: Long = 0L
    private var hasLoadedOnce: Boolean = false

    var usersSnapshot: List<AdminUserRecord> = emptyList()
        private set

    var unlikeQuestionsSnapshot: List<AdminUserRecord.AssistantQuestionRecord> = emptyList()
        private set

    var errorMessageSnapshot: String? = null
        private set

    val hasFreshData: Boolean
        get() = hasLoadedOnce &&
                loadedAtMillis > 0L &&
                System.currentTimeMillis() - loadedAtMillis <= FRESH_WINDOW_MILLIS

    suspend fun preload(isEnglish: Boolean): AdminUsersPreloadResult {
        if (hasFreshData) {
            return AdminUsersPreloadResult(
                users = usersSnapshot,
                unlikeQuestions = unlikeQuestionsSnapshot,
                errorMessage = errorMessageSnapshot
            )
        }

        return refresh(isEnglish)
    }

    suspend fun refresh(isEnglish: Boolean): AdminUsersPreloadResult {
        var loadedUsers: List<AdminUserRecord> = emptyList()
        var loadedUnlikeQuestions: List<AdminUserRecord.AssistantQuestionRecord> = emptyList()
        var errorMsg: String? = null

        try {
            val snap = Firebase.firestore
                .collection("users")
                .get()
                .await()

            val raw = snap.documents
                .mapNotNull { doc ->
                    doc.toAdminUserRecord()
                }
                .filter { user ->
                    user.hasRealAdminUserContent()
                }

            loadedUsers =
                mergeDuplicateAdminUsers(raw)
                    .sortedWith(
                        compareBy<AdminUserRecord> {
                            it.fullName.startsWith(
                                "Unknown user",
                                ignoreCase = true
                            )
                        }.thenBy {
                            it.fullName
                                .trim()
                                .lowercase()
                        }
                    )

        } catch (t: Throwable) {
            val rawErr = t.message ?: adminTr(
                isEnglish,
                "שגיאה בטעינת המשתמשים",
                "Error loading users"
            )

            errorMsg = if (rawErr.contains("PERMISSION_DENIED")) {
                adminTr(
                    isEnglish,
                    "אין לך הרשאה לצפות ברשימת המשתמשים. בדוק את הגדרות ההרשאות או פנה למנהל המערכת.",
                    "You do not have permission to view the users list. Check the permission settings or contact the system administrator."
                )
            } else {
                rawErr
            }
        }

        try {
            val feedbackSnap = Firebase.firestore
                .collection("assistantFeedback")
                .whereEqualTo("liked", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            loadedUnlikeQuestions = feedbackSnap.documents.mapNotNull { doc ->
                val qText = doc.getString("question") ?: return@mapNotNull null

                AdminUserRecord.AssistantQuestionRecord(
                    id = doc.id,
                    question = qText,
                    answer = doc.getString("answer"),
                    createdAtMillis = adminMillisFromFirestore(
                        doc.get("createdAt") ?: doc.get("ts")
                    ),
                    userName = doc.getString("userName"),
                    userUid = doc.getString("userUid")
                )
            }
        } catch (_: Throwable) {
            loadedUnlikeQuestions = emptyList()
        }

        usersSnapshot = loadedUsers
        unlikeQuestionsSnapshot = loadedUnlikeQuestions
        errorMessageSnapshot = errorMsg
        loadedAtMillis = System.currentTimeMillis()
        hasLoadedOnce = true

        return AdminUsersPreloadResult(
            users = usersSnapshot,
            unlikeQuestions = unlikeQuestionsSnapshot,
            errorMessage = errorMessageSnapshot
        )
    }
}

private fun createAdminUsersPdf(
    context: android.content.Context,
    isEnglish: Boolean,
    users: List<AdminUserRecord>,
    traineeUsers: List<AdminUserRecord>,
    coachUsers: List<AdminUserRecord>,
    beltCounts: List<Pair<String, Int>>,
    filtersSummary: String,
    totalUsers: Int,
    branchCount: Int,
    averageAge: Double?,
    totalAppOpens: Int,
    activeUsersWithUsage: Int,
    unlikeQuestions: List<AdminUserRecord.AssistantQuestionRecord>
): java.io.File {
    val pageWidth = 595
    val pageHeight = 842

    val contentLeft = 34f
    val contentRight = pageWidth - 34f
    val contentBottom = pageHeight - 58f

    val document = android.graphics.pdf.PdfDocument()

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val mediumBlue = android.graphics.Color.rgb(36, 103, 158)
    val lightBlue = android.graphics.Color.rgb(128, 183, 220)
    val darkText = android.graphics.Color.rgb(15, 23, 42)
    val mutedText = android.graphics.Color.rgb(100, 116, 139)
    val cardFill = android.graphics.Color.rgb(248, 250, 252)
    val cardBorder = android.graphics.Color.rgb(203, 213, 225)
    val successGreen = android.graphics.Color.rgb(22, 163, 74)
    val infoBlue = android.graphics.Color.rgb(2, 132, 199)
    val coachPurple = android.graphics.Color.rgb(124, 58, 237)
    val warningOrange = android.graphics.Color.rgb(217, 119, 6)

    val regularTypeface = android.graphics.Typeface.create(
        android.graphics.Typeface.SANS_SERIF,
        android.graphics.Typeface.NORMAL
    )

    val boldTypeface = android.graphics.Typeface.create(
        android.graphics.Typeface.SANS_SERIF,
        android.graphics.Typeface.BOLD
    )

    val headerTitlePaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = android.graphics.Color.WHITE
        textSize = 25f
        typeface = boldTypeface
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
    }

    val headerSubtitlePaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = android.graphics.Color.WHITE
        textSize = 12f
        typeface = regularTypeface
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
    }

    val sectionPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = darkText
        textSize = 15f
        typeface = boldTypeface
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
    }

    val bodyPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = darkText
        textSize = 10f
        typeface = regularTypeface
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
    }

    val bodyBoldPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = darkText
        textSize = 11f
        typeface = boldTypeface
        textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }
    }

    val smallPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = mutedText
        textSize = 8.5f
        typeface = regularTypeface
    }

    val cardFillPaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = cardFill
        style = android.graphics.Paint.Style.FILL
    }

    val cardStrokePaint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = cardBorder
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1f
    }

    var pageNumber = 0
    lateinit var page: android.graphics.pdf.PdfDocument.Page
    lateinit var canvas: android.graphics.Canvas
    var y = 0f

    fun tr(he: String, en: String): String {
        return if (isEnglish) en else he
    }

    fun textX(): Float {
        return if (isEnglish) contentLeft else contentRight
    }

    fun cleanText(value: String): String {
        return value
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun splitLines(
        value: String,
        paint: android.graphics.Paint,
        maxWidth: Float,
        maxLines: Int
    ): List<String> {
        val words = cleanText(value)
            .split(" ")
            .filter { it.isNotBlank() }

        if (words.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<String>()
        var current = ""

        words.forEach { word ->
            val candidate = if (current.isBlank()) {
                word
            } else {
                "$current $word"
            }

            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotBlank()) {
                    result += current
                }

                current = word
            }
        }

        if (current.isNotBlank()) {
            result += current
        }

        if (result.size <= maxLines) {
            return result
        }

        val limited = result.take(maxLines).toMutableList()
        var last = limited.last()

        while (
            last.length > 3 &&
            paint.measureText("$last…") > maxWidth
        ) {
            last = last.dropLast(1)
        }

        limited[limited.lastIndex] = "$last…"

        return limited
    }

    fun drawHeader() {
        canvas.drawColor(android.graphics.Color.WHITE)

        val headerBottom = 122f

        val navyPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            style = android.graphics.Paint.Style.FILL
        }

        val mediumStripePaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = mediumBlue
            style = android.graphics.Paint.Style.FILL
        }

        val lightStripePaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = lightBlue
            style = android.graphics.Paint.Style.FILL
        }

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(pageWidth.toFloat(), 0f)
                lineTo(pageWidth.toFloat(), headerBottom)
                lineTo(178f, headerBottom)
                lineTo(238f, 0f)
                close()
            },
            navyPaint
        )

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(208f, headerBottom)
                lineTo(224f, headerBottom)
                lineTo(284f, 0f)
                lineTo(268f, 0f)
                close()
            },
            mediumStripePaint
        )

        canvas.drawPath(
            android.graphics.Path().apply {
                moveTo(230f, headerBottom)
                lineTo(238f, headerBottom)
                lineTo(298f, 0f)
                lineTo(290f, 0f)
                close()
            },
            lightStripePaint
        )

        val logoCenterX = 78f
        val logoCenterY = 58f
        val logoRadius = 42f

        val logoOuterPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            style = android.graphics.Paint.Style.FILL
        }

        val logoInnerPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
        }

        val logoTextPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            textSize = logoRadius * 0.62f
            typeface = boldTypeface
            textAlign = android.graphics.Paint.Align.CENTER
        }

        canvas.drawCircle(
            logoCenterX,
            logoCenterY,
            logoRadius,
            logoOuterPaint
        )

        canvas.drawCircle(
            logoCenterX,
            logoCenterY,
            logoRadius - 4f,
            logoInnerPaint
        )

        canvas.drawText(
            "KAMI",
            logoCenterX,
            logoCenterY + logoRadius * 0.22f,
            logoTextPaint
        )

        headerTitlePaint.textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }

        headerSubtitlePaint.textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }

        val headerTextX = if (isEnglish) {
            308f
        } else {
            pageWidth - 34f
        }

        canvas.drawText(
            tr(
                "דו״ח ניהול משתמשים",
                "User Management Report"
            ),
            headerTextX,
            50f,
            headerTitlePaint
        )

        canvas.drawText(
            tr(
                "משתמשים, שימושים וחלוקה לפי חגורות",
                "Users, activity and belt distribution"
            ),
            headerTextX,
            77f,
            headerSubtitlePaint
        )

        val generatedDate = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        smallPaint.textAlign = android.graphics.Paint.Align.RIGHT

        canvas.drawText(
            tr(
                "תאריך הפקה: $generatedDate",
                "Generated: $generatedDate"
            ),
            pageWidth - 34f,
            142f,
            smallPaint
        )

        y = 174f
    }

    fun drawFooter() {
        val dividerPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = cardBorder
            strokeWidth = 1f
        }

        canvas.drawLine(
            contentLeft,
            pageHeight - 42f,
            contentRight,
            pageHeight - 42f,
            dividerPaint
        )

        smallPaint.textAlign = android.graphics.Paint.Align.CENTER

        canvas.drawText(
            tr(
                "עמוד $pageNumber · KAMI",
                "Page $pageNumber · KAMI"
            ),
            pageWidth / 2f,
            pageHeight - 24f,
            smallPaint
        )
    }

    fun startPage() {
        if (pageNumber > 0) {
            drawFooter()
            document.finishPage(page)
        }

        pageNumber++

        page = document.startPage(
            android.graphics.pdf.PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()
        )

        canvas = page.canvas
        drawHeader()
    }

    fun ensureSpace(requiredHeight: Float) {
        if (y + requiredHeight > contentBottom) {
            startPage()
        }
    }

    fun drawSectionTitle(title: String) {
        ensureSpace(28f)

        canvas.drawText(
            title,
            textX(),
            y,
            sectionPaint
        )

        y += 22f
    }

    fun drawSummaryCard(
        title: String,
        value: String,
        valueColor: Int,
        left: Float,
        top: Float,
        width: Float
    ) {
        val rect = android.graphics.RectF(
            left,
            top,
            left + width,
            top + 56f
        )

        canvas.drawRoundRect(
            rect,
            12f,
            12f,
            cardFillPaint
        )

        canvas.drawRoundRect(
            rect,
            12f,
            12f,
            cardStrokePaint
        )

        val valuePaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = valueColor
            textSize = 17f
            typeface = boldTypeface
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val labelPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = darkText
            textSize = 9.2f
            typeface = boldTypeface
            textAlign = android.graphics.Paint.Align.CENTER
        }

        canvas.drawText(
            value,
            rect.centerX(),
            top + 24f,
            valuePaint
        )

        canvas.drawText(
            title,
            rect.centerX(),
            top + 43f,
            labelPaint
        )
    }

    fun drawBeltRow(
        label: String,
        count: Int
    ) {
        ensureSpace(31f)

        val rowTop = y
        val rowBottom = rowTop + 25f

        canvas.drawRoundRect(
            contentLeft,
            rowTop,
            contentRight,
            rowBottom,
            8f,
            8f,
            cardFillPaint
        )

        bodyBoldPaint.textAlign = if (isEnglish) {
            android.graphics.Paint.Align.LEFT
        } else {
            android.graphics.Paint.Align.RIGHT
        }

        val labelX = if (isEnglish) {
            contentLeft + 12f
        } else {
            contentRight - 12f
        }

        canvas.drawText(
            label,
            labelX,
            rowTop + 17f,
            bodyBoldPaint
        )

        val countPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = infoBlue
            textSize = 11f
            typeface = boldTypeface
            textAlign = android.graphics.Paint.Align.CENTER
        }

        canvas.drawText(
            count.toString(),
            if (isEnglish) contentRight - 26f else contentLeft + 26f,
            rowTop + 17f,
            countPaint
        )

        y = rowBottom + 5f
    }

    fun drawUserCard(
        user: AdminUserRecord,
        roleTitle: String,
        roleColor: Int
    ) {
        val beltTitle = traineeRankDisplayName(
            rawId = user.currentBeltId,
            isEnglish = isEnglish
        ).ifBlank {
            tr(
                "ללא חגורה",
                "No belt"
            )
        }

        val ageTitle = user.age?.toString()
            ?: tr(
                "לא ידוע",
                "Unknown"
            )

        val regionTitle =
            user.displayRegionText(isEnglish)

        val branchesTitle =
            user.displayBranchAssignmentsText(
                isEnglish
            )

        val lastSeenTitle =
            user.displayLastSeenText(isEnglish)

        val tenureTitle =
            user.displayAppTenureText(isEnglish)

        val nameLines = splitLines(
            value = user.demoSafeName(isEnglish),
            paint = bodyBoldPaint,
            maxWidth = contentRight - contentLeft - 110f,
            maxLines = 2
        )

        val detailsText = tr(
            "$beltTitle • גיל: $ageTitle • אזור: $regionTitle",
            "$beltTitle • Age: $ageTitle • Region: $regionTitle"
        )

        val detailsLines = splitLines(
            value = detailsText,
            paint = bodyPaint,
            maxWidth = contentRight - contentLeft - 28f,
            maxLines = 2
        )

        val branchesLines = splitLines(
            value = tr(
                "סניפים וקבוצות: $branchesTitle",
                "Branches and groups: $branchesTitle"
            ),
            paint = bodyPaint,
            maxWidth = contentRight - contentLeft - 28f,
            maxLines = 2
        )

        val usageLines = splitLines(
            value = tr(
                "שימושים: ${user.appOpenCount} • שימוש אחרון: $lastSeenTitle • ותק: $tenureTitle",
                "App opens: ${user.appOpenCount} • Last use: $lastSeenTitle • Tenure: $tenureTitle"
            ),
            paint = bodyPaint,
            maxWidth = contentRight - contentLeft - 28f,
            maxLines = 2
        )

        val requiredHeight =
            22f +
                    nameLines.size * 14f +
                    detailsLines.size * 13f +
                    branchesLines.size * 13f +
                    usageLines.size * 13f +
                    16f

        ensureSpace(requiredHeight + 8f)

        val rowTop = y
        val rowBottom = rowTop + requiredHeight

        canvas.drawRoundRect(
            contentLeft,
            rowTop,
            contentRight,
            rowBottom,
            12f,
            12f,
            cardFillPaint
        )

        canvas.drawRoundRect(
            contentLeft,
            rowTop,
            contentRight,
            rowBottom,
            12f,
            12f,
            cardStrokePaint
        )

        val roleStripePaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = roleColor
            style = android.graphics.Paint.Style.FILL
        }

        val stripeRect = if (isEnglish) {
            android.graphics.RectF(
                contentLeft,
                rowTop,
                contentLeft + 5f,
                rowBottom
            )
        } else {
            android.graphics.RectF(
                contentRight - 5f,
                rowTop,
                contentRight,
                rowBottom
            )
        }

        canvas.drawRoundRect(
            stripeRect,
            4f,
            4f,
            roleStripePaint
        )

        val innerX = if (isEnglish) {
            contentLeft + 14f
        } else {
            contentRight - 14f
        }

        var textY = rowTop + 18f

        nameLines.forEach { line ->
            canvas.drawText(
                line,
                innerX,
                textY,
                bodyBoldPaint
            )

            textY += 14f
        }

        val rolePaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = roleColor
            textSize = 9f
            typeface = boldTypeface
            textAlign = if (isEnglish) {
                android.graphics.Paint.Align.RIGHT
            } else {
                android.graphics.Paint.Align.LEFT
            }
        }

        canvas.drawText(
            roleTitle,
            if (isEnglish) contentRight - 14f else contentLeft + 14f,
            rowTop + 18f,
            rolePaint
        )

        detailsLines.forEach { line ->
            canvas.drawText(
                line,
                innerX,
                textY,
                bodyPaint
            )

            textY += 13f
        }

        branchesLines.forEach { line ->
            canvas.drawText(
                line,
                innerX,
                textY,
                bodyPaint
            )

            textY += 13f
        }

        usageLines.forEach { line ->
            canvas.drawText(
                line,
                innerX,
                textY,
                bodyPaint
            )

            textY += 13f
        }

        y = rowBottom + 8f
    }

    fun drawUnlikeQuestion(
        index: Int,
        question: AdminUserRecord.AssistantQuestionRecord
    ) {
        val questionLines = splitLines(
            value = question.question,
            paint = bodyPaint,
            maxWidth = contentRight - contentLeft - 36f,
            maxLines = 5
        )

        val metadata = listOfNotNull(
            question.userName?.trim()?.takeIf { it.isNotBlank() },
            question.userUid?.trim()?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        val metadataLines = splitLines(
            value = metadata,
            paint = smallPaint,
            maxWidth = contentRight - contentLeft - 36f,
            maxLines = 2
        )

        val requiredHeight =
            18f +
                    questionLines.size * 13f +
                    metadataLines.size * 11f +
                    12f

        ensureSpace(requiredHeight + 7f)

        val rowTop = y
        val rowBottom = rowTop + requiredHeight

        canvas.drawRoundRect(
            contentLeft,
            rowTop,
            contentRight,
            rowBottom,
            10f,
            10f,
            cardFillPaint
        )

        canvas.drawRoundRect(
            contentLeft,
            rowTop,
            contentRight,
            rowBottom,
            10f,
            10f,
            cardStrokePaint
        )

        val numberPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = warningOrange
            textSize = 10f
            typeface = boldTypeface
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val numberX = if (isEnglish) {
            contentLeft + 16f
        } else {
            contentRight - 16f
        }

        canvas.drawText(
            (index + 1).toString(),
            numberX,
            rowTop + 18f,
            numberPaint
        )

        val innerX = if (isEnglish) {
            contentLeft + 34f
        } else {
            contentRight - 34f
        }

        var textY = rowTop + 18f

        questionLines.forEach { line ->
            canvas.drawText(
                line,
                innerX,
                textY,
                bodyPaint
            )

            textY += 13f
        }

        metadataLines.forEach { line ->
            canvas.drawText(
                line,
                innerX,
                textY,
                smallPaint
            )

            textY += 11f
        }

        y = rowBottom + 7f
    }

    startPage()

    drawSectionTitle(
        tr(
            "סיכום נתונים",
            "Summary"
        )
    )

    val gap = 8f
    val summaryWidth =
        (contentRight - contentLeft - gap * 2f) / 3f

    drawSummaryCard(
        title = tr("משתמשים", "Users"),
        value = totalUsers.toString(),
        valueColor = infoBlue,
        left = contentLeft,
        top = y,
        width = summaryWidth
    )

    drawSummaryCard(
        title = tr("סניפים", "Branches"),
        value = branchCount.toString(),
        valueColor = mediumBlue,
        left = contentLeft + summaryWidth + gap,
        top = y,
        width = summaryWidth
    )

    drawSummaryCard(
        title = tr("גיל ממוצע", "Avg. age"),
        value = averageAge?.let {
            String.format(
                java.util.Locale.US,
                "%.1f",
                it
            )
        } ?: "-",
        valueColor = successGreen,
        left = contentLeft + (summaryWidth + gap) * 2f,
        top = y,
        width = summaryWidth
    )

    y += 66f

    drawSummaryCard(
        title = tr("שימושים", "App opens"),
        value = totalAppOpens.toString(),
        valueColor = infoBlue,
        left = contentLeft,
        top = y,
        width = summaryWidth
    )

    drawSummaryCard(
        title = tr("פעילים", "Active users"),
        value = activeUsersWithUsage.toString(),
        valueColor = successGreen,
        left = contentLeft + summaryWidth + gap,
        top = y,
        width = summaryWidth
    )

    drawSummaryCard(
        title = tr("מסוננים", "Filtered"),
        value = users.size.toString(),
        valueColor = coachPurple,
        left = contentLeft + (summaryWidth + gap) * 2f,
        top = y,
        width = summaryWidth
    )

    y += 72f

    drawSectionTitle(
        tr(
            "פילטרים פעילים",
            "Active filters"
        )
    )

    val filterLines = splitLines(
        value = filtersSummary,
        paint = bodyPaint,
        maxWidth = contentRight - contentLeft,
        maxLines = 4
    )

    filterLines.forEach { line ->
        canvas.drawText(
            line,
            textX(),
            y,
            bodyPaint
        )

        y += 13f
    }

    y += 10f

    drawSectionTitle(
        tr(
            "חלוקה לפי חגורה",
            "Belt distribution"
        )
    )

    beltCounts.forEach { (label, count) ->
        drawBeltRow(
            label = label,
            count = count
        )
    }

    y += 8f

    drawSectionTitle(
        tr(
            "מתאמנים (${traineeUsers.size})",
            "Trainees (${traineeUsers.size})"
        )
    )

    traineeUsers.forEach { user ->
        drawUserCard(
            user = user,
            roleTitle = tr("מתאמן", "Trainee"),
            roleColor = infoBlue
        )
    }

    y += 8f

    drawSectionTitle(
        tr(
            "מאמנים (${coachUsers.size})",
            "Coaches (${coachUsers.size})"
        )
    )

    coachUsers.forEach { user ->
        drawUserCard(
            user = user,
            roleTitle = tr("מאמן", "Coach"),
            roleColor = coachPurple
        )
    }

    if (unlikeQuestions.isNotEmpty()) {
        y += 8f

        drawSectionTitle(
            tr(
                "שאלות שסומנו UNLIKE",
                "Questions marked UNLIKE"
            )
        )

        unlikeQuestions
            .take(20)
            .forEachIndexed { index, question ->
                drawUnlikeQuestion(
                    index = index,
                    question = question
                )
            }
    }

    drawFooter()
    document.finishPage(page)

    val outputDirectory = java.io.File(
        context.cacheDir,
        "admin_users_pdf"
    ).apply {
        mkdirs()
    }

    val outputFile = java.io.File(
        outputDirectory,
        "admin_users_${System.currentTimeMillis()}.pdf"
    )

    try {
        java.io.FileOutputStream(outputFile).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }

    return outputFile
}

// ===========================
//   מסך ניהול משתמשים
// ===========================
@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    onHome: () -> Unit = onBack
) {
    val contextLang = LocalContext.current
    val langManager = remember { AppLanguageManager(contextLang) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val screenTextAlign = adminTextAlign(isEnglish)
    val gradient = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0EA5E9)
            )
        )
    }

    // --- מצב נתונים מ-Firestore / Cache מוקדם ממסך הטעינה ---
    var users by remember {
        mutableStateOf(AdminUsersPreloadCache.usersSnapshot)
    }

    var loading by remember {
        mutableStateOf(!AdminUsersPreloadCache.hasFreshData)
    }

    var errorMsg by remember {
        mutableStateOf(AdminUsersPreloadCache.errorMessageSnapshot)
    }

    // 👇 שאלות שסומנו UNLIKE בעוזר הקולי
    var unlikeQuestions by remember {
        mutableStateOf(AdminUsersPreloadCache.unlikeQuestionsSnapshot)
    }

    LaunchedEffect(isEnglish) {
        if (AdminUsersPreloadCache.hasFreshData) {
            users = AdminUsersPreloadCache.usersSnapshot
            unlikeQuestions = AdminUsersPreloadCache.unlikeQuestionsSnapshot
            errorMsg = AdminUsersPreloadCache.errorMessageSnapshot
            loading = false
            return@LaunchedEffect
        }

        loading = true

        val result = AdminUsersPreloadCache.refresh(isEnglish)

        users = result.users
        unlikeQuestions = result.unlikeQuestions
        errorMsg = result.errorMessage
        loading = false
    }

    // -------- פילטרים --------
    var genderFilter by remember { mutableStateOf<String?>(null) }   // null = הכל
    var regionFilter by remember { mutableStateOf<String?>(null) }
    var beltFilter by remember { mutableStateOf<String?>(null) }
    var ageBucketFilter by remember { mutableStateOf<String?>(null) }

    val allRegions = remember(users) {
        users.mapNotNull { it.region?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }
    val allBelts = remember(users, isEnglish) {
        users
            .mapNotNull { user ->
                traineeRankDisplayName(user.currentBeltId, isEnglish).ifBlank { null }
            }
            .distinct()
            .sortedBy { label ->
                val id = users.firstOrNull {
                    traineeRankDisplayName(it.currentBeltId, isEnglish) == label
                }?.currentBeltId

                traineeRankSortIndex(id)
            }
    }
    val allAgeBuckets = remember(users) {
        users
            .map { it.ageBucket }
            .filter { it != "לא ידוע" }
            .distinct()
            .sortedBy { it }
    }

    val filteredUsers = remember(users, genderFilter, regionFilter, beltFilter, ageBucketFilter, isEnglish) {
        users.filter { u ->
            val genderClean = (u.gender ?: "").trim().lowercase()

            val gOk = genderFilter == null ||
                    (genderFilter == "male" &&
                            (genderClean.startsWith("m") || genderClean == "זכר")) ||
                    (genderFilter == "female" &&
                            (genderClean.startsWith("f") || genderClean == "נקבה"))

            val rOk = regionFilter == null || u.region == regionFilter

            val bOk = beltFilter == null ||
                    traineeRankDisplayName(u.currentBeltId, isEnglish) == beltFilter

            val aOk = ageBucketFilter == null || u.ageBucket == ageBucketFilter

            gOk && rOk && bOk && aOk
        }
    }

    val coachUsers = remember(filteredUsers) { filteredUsers.filter { it.isCoach } }
    val traineeUsers = remember(filteredUsers) { filteredUsers.filter { !it.isCoach } }

    val traineeUiUsers = remember(traineeUsers) {
        traineeUsers
    }

    val coachUiUsers = remember(coachUsers) {
        coachUsers
    }

    // -------- סטטיסטיקות כלליות --------
    val totalUsers = users.size
    val genderCounts = users.groupBy { user ->
        when ((user.gender ?: "unknown").trim().lowercase()) {
            "m", "male", "זכר" -> "male"
            "f", "female", "נקבה" -> "female"
            else -> "unknown"
        }
    }.mapValues { it.value.size }

    val regionCounts = users.groupBy { it.region ?: "לא ידוע" }
        .mapValues { it.value.size }

    val branchCount = users
        .flatMap { user ->
            buildList {
                user.branch?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
                addAll(user.branches.map { it.trim() }.filter { it.isNotBlank() })
            }
        }
        .filter { branch ->
            branch.isNotBlank() &&
                    !branch.equals("לא ידוע", ignoreCase = true) &&
                    !branch.equals("unknown", ignoreCase = true) &&
                    !branch.equals("null", ignoreCase = true) &&
                    branch != "—"
        }
        .map { it.lowercase() }
        .distinct()
        .size

    val beltCountsRaw = users.groupBy { user ->
        user.currentBeltId?.trim().orEmpty()
    }.mapValues { it.value.size }

    val beltCountsOrdered: List<Triple<String, Int, Color>> = traineeRankOrderedIds().map { rawId ->
        val label = traineeRankLabelFromOrderedId(rawId, isEnglish)
        val count = if (rawId.isBlank()) {
            beltCountsRaw[""].orEmptyCount() +
                    users.count { it.currentBeltId.isNullOrBlank() }
        } else {
            beltCountsRaw[rawId] ?: 0
        }

        Triple(label, count, traineeRankColor(rawId))
    }


    val avgAge = users.mapNotNull { it.age }.takeIf { it.isNotEmpty() }?.average()

    // ✅ נתוני שימוש כלליים
    val totalAppOpens = users.sumOf { it.appOpenCount }
    val activeUsersWithUsage = users.count { it.appOpenCount > 0 }

    val outerScroll = rememberScrollState()

    val activeFiltersSummary = buildList {
        genderFilter?.let { gender ->
            add(
                when (gender) {
                    "male" -> adminTr(isEnglish, "מין: זכר", "Gender: Male")
                    "female" -> adminTr(isEnglish, "מין: נקבה", "Gender: Female")
                    else -> adminTr(
                        isEnglish,
                        "מין: $gender",
                        "Gender: $gender"
                    )
                }
            )
        }

        regionFilter?.let { region ->
            add(
                adminTr(
                    isEnglish,
                    "אזור: $region",
                    "Region: $region"
                )
            )
        }

        beltFilter?.let { belt ->
            add(
                adminTr(
                    isEnglish,
                    "חגורה: $belt",
                    "Belt: $belt"
                )
            )
        }

        ageBucketFilter?.let { ageBucket ->
            add(
                adminTr(
                    isEnglish,
                    "קבוצת גיל: ${adminAgeBucketLabel(ageBucket, isEnglish)}",
                    "Age group: ${adminAgeBucketLabel(ageBucket, isEnglish)}"
                )
            )
        }
    }.joinToString(" • ").ifBlank {
        adminTr(
            isEnglish,
            "אין פילטרים פעילים",
            "No active filters"
        )
    }

    val pdfBeltCounts = beltCountsOrdered.map { (label, count, _) ->
        label to count
    }

    val onExportUsersPdf: () -> Unit = {
        if (loading) {
            android.widget.Toast.makeText(
                contextLang,
                adminTr(
                    isEnglish,
                    "נתוני המשתמשים עדיין נטענים",
                    "The users data is still loading"
                ),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            runCatching {
                val pdfFile = createAdminUsersPdf(
                    context = contextLang,
                    isEnglish = isEnglish,
                    users = filteredUsers,
                    traineeUsers = traineeUiUsers,
                    coachUsers = coachUiUsers,
                    beltCounts = pdfBeltCounts,
                    filtersSummary = activeFiltersSummary,
                    totalUsers = totalUsers,
                    branchCount = branchCount,
                    averageAge = avgAge,
                    totalAppOpens = totalAppOpens,
                    activeUsersWithUsage = activeUsersWithUsage,
                    unlikeQuestions = unlikeQuestions
                )

                val pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    contextLang,
                    "${contextLang.packageName}.fileprovider",
                    pdfFile
                )

                val shareIntent = android.content.Intent(
                    android.content.Intent.ACTION_SEND
                ).apply {
                    type = "application/pdf"

                    putExtra(
                        android.content.Intent.EXTRA_SUBJECT,
                        adminTr(
                            isEnglish,
                            "דו״ח ניהול משתמשים",
                            "User Management Report"
                        )
                    )

                    putExtra(
                        android.content.Intent.EXTRA_STREAM,
                        pdfUri
                    )

                    addFlags(
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    if (contextLang !is android.app.Activity) {
                        addFlags(
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }
                }

                val chooserIntent = android.content.Intent.createChooser(
                    shareIntent,
                    adminTr(
                        isEnglish,
                        "שיתוף דו״ח PDF",
                        "Share PDF report"
                    )
                )

                if (contextLang !is android.app.Activity) {
                    chooserIntent.addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

                contextLang.startActivity(chooserIntent)
            }.onFailure {
                android.widget.Toast.makeText(
                    contextLang,
                    adminTr(
                        isEnglish,
                        "יצירת קובץ ה־PDF נכשלה",
                        "Failed to create the PDF file"
                    ),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {

            KmiTopBar(
                title = adminTr(
                    isEnglish,
                    "ניהול משתמשים",
                    "User management"
                ),
                onHome = onHome,
                showTopHome = false,
                showTopSearch = false,
                showTopShare = false,
                lockSearch = false,
                lockHome = false,
                showBottomActions = true,
                onShare = onExportUsersPdf,
                currentLang = if (
                    langManager.getCurrentLanguage() == AppLanguage.ENGLISH
                ) {
                    "en"
                } else {
                    "he"
                },
                onToggleLanguage = {
                    val newLang =
                        if (
                            langManager.getCurrentLanguage() ==
                            AppLanguage.HEBREW
                        ) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    (contextLang as? Activity)?.recreate()
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(outerScroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ---------- כרטיסי סטטוס עליונים ----------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatCard(
                        title = adminTr(isEnglish, "משתמשים", "Users"),
                        value = if (loading) "…" else totalUsers.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = adminTr(isEnglish, "סניפים", "Branches"),
                        value = if (loading) "…" else branchCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = adminTr(isEnglish, "גיל ממוצע", "Avg. age"),
                        value = if (loading) {
                            "…"
                        } else {
                            avgAge?.let { String.format("%.1f", it) }
                                ?: "-"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatCard(
                        title = adminTr(isEnglish, "שימושים", "App opens"),
                        value = if (loading) "…" else totalAppOpens.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = adminTr(isEnglish, "משתמשים פעילים", "Active users"),
                        value = if (loading) "…" else activeUsersWithUsage.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = adminTr(isEnglish, "ממוצע שימוש", "Avg. opens"),
                        value = if (loading || totalUsers == 0) {
                            "…"
                        } else {
                            String.format(Locale.US, "%.1f", totalAppOpens.toDouble() / totalUsers.toDouble())
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // הודעת שגיאה (אם יש)
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFF97373),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---------- גרף קטן – לפי מין ----------
                MiniBarChartCard(
                    title = adminTr(isEnglish, "חלוקה לפי מין", "Gender distribution"),
                    data = listOf(
                        adminTr(isEnglish, "זכר", "Male") to (genderCounts["male"] ?: genderCounts["m"] ?: 0),
                        adminTr(isEnglish, "נקבה", "Female") to (genderCounts["female"] ?: genderCounts["f"] ?: 0)
                    ),
                    accent = Color(0xFF38BDF8)
                )

                // ---------- Belt distribution ----------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF020617).copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = adminTr(isEnglish, "חלוקה לפי חגורה", "Belt distribution"),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE5E7EB),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = screenTextAlign,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            beltCountsOrdered.forEach { (label, value, circleColor) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(66.dp)
                                            .height(39.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(circleColor)
                                    )

                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE5E7EB)
                                    )

                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9CA3AF),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // ---------- פילטרים ----------
                FilterRow(
                    isEnglish = isEnglish,
                    textAlign = screenTextAlign,
                    genderFilter = genderFilter,
                    onGenderChange = { genderFilter = it },
                    regionFilter = regionFilter,
                    onRegionChange = { regionFilter = it },
                    beltFilter = beltFilter,
                    onBeltChange = { beltFilter = it },
                    ageBucketFilter = ageBucketFilter,
                    onAgeBucketChange = { ageBucketFilter = it },
                    regions = allRegions,
                    belts = allBelts,
                    ageBuckets = allAgeBuckets
                )

// ---------- משתמשים – מתאמנים ----------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0B1220).copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = adminTr(
                                isEnglish,
                                "משתמשים – מתאמנים (${traineeUiUsers.size})",
                                "Users – trainees (${traineeUiUsers.size})"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Bold,
                            textAlign = screenTextAlign,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        if (loading) {
                            Text(
                                text = adminTr(isEnglish, "טוען משתמשים…", "Loading users…"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (traineeUiUsers.isEmpty()) {
                            Text(
                                text = adminTr(
                                    isEnglish,
                                    "אין מתאמנים מתאימים לפילטרים.",
                                    "No trainees match the selected filters."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                traineeUiUsers.forEach { user ->
                                    UserRowCard(
                                        user = user,
                                        isEnglish = isEnglish
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

// ---------- משתמשים – מאמנים ----------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0B1220).copy(alpha = 0.92f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = adminTr(
                                isEnglish,
                                "משתמשים – מאמנים (${coachUiUsers.size})",
                                "Users – coaches (${coachUiUsers.size})"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Bold,
                            textAlign = screenTextAlign,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        if (loading) {
                            Text(
                                text = adminTr(isEnglish, "טוען משתמשים…", "Loading users…"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (coachUiUsers.isEmpty()) {
                            Text(
                                text = adminTr(
                                    isEnglish,
                                    "אין מאמנים מתאימים לפילטרים.",
                                    "No coaches match the selected filters."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                coachUiUsers.forEach { user ->
                                    UserRowCard(
                                        user = user,
                                        isEnglish = isEnglish
                                    )
                                }
                            }
                        }
                    }
                } // ✅ סגירת ה-Card

                Spacer(Modifier.height(12.dp))

                // ---------- שאלות שסומנו UNLIKE מהעוזר הקולי ----------
                if (unlikeQuestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF020617).copy(alpha = 0.95f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = adminTr(
                                    isEnglish,
                                    "שאלות לסקירה (UNLIKE)",
                                    "Questions for review (UNLIKE)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE5E7EB),
                                fontWeight = FontWeight.Bold,
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = adminTr(
                                    isEnglish,
                                    "רשימת שאלות שהעוזר לא ענה עליהן טוב – לסקירה ולשיפור מאגר התכנים.",
                                    "Questions where the assistant response was marked as not helpful — for review and content improvement."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            unlikeQuestions
                                .take(20) // לא להציף – 20 אחרונות
                                .forEach { fb ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF020617))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "• ${fb.question}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFE5E7EB),
                                            textAlign = screenTextAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        val meta = listOfNotNull(
                                            fb.userName,
                                            fb.userUid
                                        ).joinToString(" • ")

                                        if (meta.isNotBlank()) {
                                            Text(
                                                text = meta,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF9CA3AF),
                                                textAlign = screenTextAlign,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ===================== כרטיסי עזר =====================

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 108.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF020617).copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ✅ אזור כותרת גבוה יותר כדי שלא ייחתכו כותרות של 2 שורות
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(2.dp))

            // ✅ אזור מספר קבוע — כל המספרים באותו קו
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE5E7EB),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun MiniBarChartCard(
    title: String,
    data: List<Pair<String, Int>>,
    accent: Color,
    colorForLabel: ((String) -> Color)? = null   // 👈 צבע לפי תווית (למשל חגורה)
) {
    val max = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF020617).copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE5E7EB),
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { (label, value) ->
                    val ratio = value.toFloat() / max.toFloat()
                    val barColor = colorForLabel?.invoke(label) ?: accent

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .height(60.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((60f * ratio).dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                barColor.copy(alpha = 0.25f),
                                                barColor
                                            )
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE5E7EB)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    isEnglish: Boolean,
    textAlign: androidx.compose.ui.text.style.TextAlign,
    genderFilter: String?,
    onGenderChange: (String?) -> Unit,
    regionFilter: String?,
    onRegionChange: (String?) -> Unit,
    beltFilter: String?,
    onBeltChange: (String?) -> Unit,
    ageBucketFilter: String?,
    onAgeBucketChange: (String?) -> Unit,
    regions: List<String>,
    belts: List<String>,
    ageBuckets: List<String>
) {
    var filtersExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val activeFiltersCount = listOfNotNull(
        genderFilter,
        regionFilter,
        beltFilter,
        ageBucketFilter
    ).size

    val selectedFiltersSummary = buildList {
        genderFilter?.let { gender ->
            add(
                when (gender) {
                    "male" -> adminTr(isEnglish, "זכר", "Male")
                    "female" -> adminTr(isEnglish, "נקבה", "Female")
                    else -> gender
                }
            )
        }

        regionFilter?.let {
            add(it)
        }

        beltFilter?.let {
            add(it)
        }

        ageBucketFilter?.let {
            add(adminAgeBucketLabel(it, isEnglish))
        }
    }.joinToString(" • ")

    val collapsedSummary = if (activeFiltersCount == 0) {
        adminTr(
            isEnglish,
            "אין פילטרים פעילים",
            "No active filters"
        )
    } else {
        selectedFiltersSummary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B1220).copy(alpha = 0.92f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (filtersExpanded) {
                Color(0xFF38BDF8).copy(alpha = 0.75f)
            } else {
                Color(0xFF38BDF8).copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF020617).copy(alpha = 0.75f))
                    .clickable {
                        filtersExpanded = !filtersExpanded
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isEnglish) {
                    Arrangement.Start
                } else {
                    Arrangement.End
                }
            ) {
                if (isEnglish) {
                    Text(
                        text = if (filtersExpanded) "▴" else "▾",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Black
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = if (filtersExpanded) {
                                adminTr(isEnglish, "הסתר פילטרים", "Hide filters")
                            } else {
                                adminTr(isEnglish, "פתח פילטרים", "Show filters")
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFE5E7EB),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = collapsedSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activeFiltersCount == 0) {
                                Color(0xFF94A3B8)
                            } else {
                                Color(0xFF7DD3FC)
                            },
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (filtersExpanded) {
                                adminTr(isEnglish, "הסתר פילטרים", "Hide filters")
                            } else {
                                adminTr(isEnglish, "פתח פילטרים", "Show filters")
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFE5E7EB),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = collapsedSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activeFiltersCount == 0) {
                                Color(0xFF94A3B8)
                            } else {
                                Color(0xFF7DD3FC)
                            },
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = if (filtersExpanded) "▴" else "▾",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            AnimatedVisibility(
                visible = filtersExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeFiltersCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isEnglish) {
                                Arrangement.Start
                            } else {
                                Arrangement.End
                            }
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onGenderChange(null)
                                    onRegionChange(null)
                                    onBeltChange(null)
                                    onAgeBucketChange(null)
                                },
                                label = {
                                    Text(
                                        adminTr(
                                            isEnglish,
                                            "נקה את כל הפילטרים",
                                            "Clear all filters"
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color(0xFFE5E7EB),
                                    selectedContainerColor = Color(0xFF0EA5E9),
                                    selectedLabelColor = Color(0xFF020617)
                                )
                            )
                        }
                    }

                    Text(
                        text = adminTr(isEnglish, "מין", "Gender"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFBAE6FD),
                        fontWeight = FontWeight.Bold,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chipColors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF0B1220),
                            labelColor = Color(0xFFE5E7EB),
                            selectedContainerColor = Color(0xFF0EA5E9),
                            selectedLabelColor = Color(0xFF020617)
                        )

                        FilterChip(
                            selected = genderFilter == null,
                            onClick = { onGenderChange(null) },
                            label = { Text(adminTr(isEnglish, "הכל", "All")) },
                            colors = chipColors
                        )

                        FilterChip(
                            selected = genderFilter == "male",
                            onClick = { onGenderChange("male") },
                            label = { Text(adminTr(isEnglish, "זכר", "Male")) },
                            colors = chipColors
                        )

                        FilterChip(
                            selected = genderFilter == "female",
                            onClick = { onGenderChange("female") },
                            label = { Text(adminTr(isEnglish, "נקבה", "Female")) },
                            colors = chipColors
                        )
                    }

                    Text(
                        text = adminTr(isEnglish, "אזור", "Region"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFBAE6FD),
                        fontWeight = FontWeight.Bold,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chipColors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF0B1220),
                            labelColor = Color(0xFFE5E7EB),
                            selectedContainerColor = Color(0xFF0EA5E9),
                            selectedLabelColor = Color(0xFF020617)
                        )

                        FilterChip(
                            selected = regionFilter == null,
                            onClick = { onRegionChange(null) },
                            label = { Text(adminTr(isEnglish, "כל האזורים", "All regions")) },
                            colors = chipColors
                        )

                        regions.forEach { region ->
                            FilterChip(
                                selected = regionFilter == region,
                                onClick = { onRegionChange(region) },
                                label = { Text(region) },
                                colors = chipColors
                            )
                        }
                    }

                    Text(
                        text = adminTr(isEnglish, "חגורה", "Belt"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFBAE6FD),
                        fontWeight = FontWeight.Bold,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chipColors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF0B1220),
                            labelColor = Color(0xFFE5E7EB),
                            selectedContainerColor = Color(0xFF0EA5E9),
                            selectedLabelColor = Color(0xFF020617)
                        )

                        FilterChip(
                            selected = beltFilter == null,
                            onClick = { onBeltChange(null) },
                            label = { Text(adminTr(isEnglish, "כל החגורות", "All belts")) },
                            colors = chipColors
                        )

                        belts.forEach { belt ->
                            FilterChip(
                                selected = beltFilter == belt,
                                onClick = { onBeltChange(belt) },
                                label = { Text(belt) },
                                colors = chipColors
                            )
                        }
                    }

                    Text(
                        text = adminTr(isEnglish, "קבוצת גיל", "Age group"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFBAE6FD),
                        fontWeight = FontWeight.Bold,
                        textAlign = textAlign,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chipColors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF0B1220),
                            labelColor = Color(0xFFE5E7EB),
                            selectedContainerColor = Color(0xFF0EA5E9),
                            selectedLabelColor = Color(0xFF020617)
                        )

                        FilterChip(
                            selected = ageBucketFilter == null,
                            onClick = { onAgeBucketChange(null) },
                            label = { Text(adminTr(isEnglish, "כל הגילאים", "All ages")) },
                            colors = chipColors
                        )

                        ageBuckets.forEach { bucket ->
                            FilterChip(
                                selected = ageBucketFilter == bucket,
                                onClick = { onAgeBucketChange(bucket) },
                                label = { Text(adminAgeBucketLabel(bucket, isEnglish)) },
                                colors = chipColors
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun AdminUserRecord.displayBranchesText(isEnglish: Boolean): String {
    val allBranches = buildList {
        branch?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
        addAll(branches.map { it.trim() }.filter { it.isNotBlank() })
    }
        .filterNot {
            it.equals("לא ידוע", ignoreCase = true) ||
                    it.equals("unknown", ignoreCase = true) ||
                    it.equals("null", ignoreCase = true) ||
                    it == "—"
        }
        .distinctBy { it.lowercase() }

    return if (allBranches.isEmpty()) {
        adminTr(isEnglish, "לא נבחר סניף", "No branch selected")
    } else {
        allBranches.joinToString(" + ")
    }
}

private fun AdminUserRecord.demoSafeName(
    isEnglish: Boolean
): String {
    return TraineeDisplayNameMapper.displayName(
        realName = fullName,
        stableKey =
            uidField
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: id,
        isEnglish = isEnglish
    ).ifBlank {
        adminTr(
            isEnglish,
            "משתמש ללא שם",
            "Unnamed user"
        )
    }
}

private fun AdminUserRecord
        .displayBranchAssignmentsText(
    isEnglish: Boolean
): String {

    if (branchAssignments.isEmpty()) {
        val branchesText =
            displayBranchesText(isEnglish)

        val groupsText =
            groups
                .joinToString(", ")
                .ifBlank {
                    adminTr(
                        isEnglish,
                        "ללא קבוצות",
                        "No groups"
                    )
                }

        return adminTr(
            isEnglish,
            "$branchesText — $groupsText",
            "$branchesText — $groupsText"
        )
    }

    return branchAssignments
        .joinToString("\n") { assignment ->
            val branchName =
                assignment.branch
                    .trim()
                    .ifBlank {
                        adminTr(
                            isEnglish,
                            "סניף לא ידוע",
                            "Unknown branch"
                        )
                    }

            val groupNames =
                assignment.groups
                    .map {
                        it.trim()
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinctBy {
                        it.lowercase()
                    }
                    .joinToString(", ")
                    .ifBlank {
                        adminTr(
                            isEnglish,
                            "ללא קבוצות",
                            "No groups"
                        )
                    }

            "$branchName: $groupNames"
        }
}

private fun AdminUserRecord.displayRegionText(isEnglish: Boolean): String {
    return region
        ?.trim()
        ?.takeIf {
            it.isNotBlank() &&
                    !it.equals("לא ידוע", ignoreCase = true) &&
                    !it.equals("unknown", ignoreCase = true) &&
                    !it.equals("null", ignoreCase = true)
        }
        ?: adminTr(isEnglish, "לא נבחר אזור", "No region selected")
}

private fun AdminUserRecord.displayLastSeenText(isEnglish: Boolean): String {
    val lastSeen = lastSeenAtMillis ?: return adminTr(
        isEnglish,
        "לא ידוע",
        "Unknown"
    )

    val now = System.currentTimeMillis()

    // הגנה מערכים שבורים כמו 0 / 1970 או תאריך עתידי לא הגיוני
    if (lastSeen < 1_577_836_800_000L ||
        lastSeen > now + 7L * 24L * 60L * 60L * 1000L
    ) {
        return adminTr(
            isEnglish,
            "לא ידוע",
            "Unknown"
        )
    }

    val diffMillis = (now - lastSeen).coerceAtLeast(0L)

    val minutes = diffMillis / (1000L * 60L)
    val hours = diffMillis / (1000L * 60L * 60L)
    val days = diffMillis / (1000L * 60L * 60L * 24L)

    return when {
        minutes < 1L -> adminTr(
            isEnglish,
            "עכשיו",
            "Now"
        )

        minutes < 60L -> adminTr(
            isEnglish,
            "לפני $minutes דקות",
            "$minutes min ago"
        )

        hours < 24L -> adminTr(
            isEnglish,
            "לפני $hours שעות",
            "$hours hours ago"
        )

        days == 1L -> adminTr(
            isEnglish,
            "אתמול",
            "Yesterday"
        )

        days < 30L -> adminTr(
            isEnglish,
            "לפני $days ימים",
            "$days days ago"
        )

        days < 365L -> {
            val months = (days / 30L).coerceAtLeast(1L)
            adminTr(
                isEnglish,
                "לפני $months חודשים",
                "$months months ago"
            )
        }

        else -> {
            val years = (days / 365L).coerceAtLeast(1L)
            adminTr(
                isEnglish,
                "לפני $years שנים",
                "$years years ago"
            )
        }
    }
}

private fun AdminUserRecord.displayAppTenureText(isEnglish: Boolean): String {
    val created = createdAtMillis ?: return adminTr(
        isEnglish,
        "לא ידוע",
        "Unknown"
    )

    val now = System.currentTimeMillis()
    val diffMillis = (now - created).coerceAtLeast(0L)

    val days = diffMillis / (1000L * 60L * 60L * 24L)

    return when {
        days <= 0L -> adminTr(
            isEnglish,
            "היום",
            "Today"
        )

        days == 1L -> adminTr(
            isEnglish,
            "יום אחד",
            "1 day"
        )

        days < 30L -> adminTr(
            isEnglish,
            "$days ימים",
            "$days days"
        )

        days < 365L -> {
            val months = (days / 30L).coerceAtLeast(1L)
            if (months == 1L) {
                adminTr(isEnglish, "חודש אחד", "1 month")
            } else {
                adminTr(isEnglish, "$months חודשים", "$months months")
            }
        }

        else -> {
            val years = days / 365L
            val remainingMonths = (days % 365L) / 30L

            when {
                years == 1L && remainingMonths == 0L ->
                    adminTr(isEnglish, "שנה אחת", "1 year")

                years == 1L ->
                    adminTr(
                        isEnglish,
                        String.format(Locale.US, "שנה ו-%d חודשים", remainingMonths),
                        String.format(Locale.US, "1 year and %d months", remainingMonths)
                    )

                remainingMonths == 0L ->
                    adminTr(
                        isEnglish,
                        String.format(Locale.US, "%d שנים", years),
                        String.format(Locale.US, "%d years", years)
                    )

                else ->
                    adminTr(
                        isEnglish,
                        String.format(Locale.US, "%d שנים ו-%d חודשים", years, remainingMonths),
                        String.format(Locale.US, "%d years and %d months", years, remainingMonths)
                    )
            }
        }
    }
}

@Composable
private fun UserRowCard(
    user: AdminUserRecord,
    isEnglish: Boolean
) {
    val beltText = traineeRankDisplayName(user.currentBeltId, isEnglish).ifBlank {
        adminTr(isEnglish, "ללא חגורה", "No belt")
    }

    val beltColor = traineeRankColor(user.currentBeltId)

    val roleLabel = if (user.isCoach) {
        adminTr(isEnglish, "מאמן", "Coach")
    } else {
        adminTr(isEnglish, "מתאמן", "Trainee")
    }

    val displayName =
        user.demoSafeName(isEnglish)

    val nowMillis = System.currentTimeMillis()
    val activeWindowMillis =
        30L * 24L * 60L * 60L * 1000L

    val isActiveUser =
        user.lastSeenAtMillis
            ?.let { lastSeen ->
                lastSeen > 0L &&
                        lastSeen <= nowMillis &&
                        nowMillis - lastSeen <= activeWindowMillis
            }
            ?: false

    val activityStatusLabel =
        if (isActiveUser) {
            adminTr(isEnglish, "פעיל", "Active")
        } else {
            adminTr(isEnglish, "לא פעיל", "Inactive")
        }

    val activityStatusColor =
        if (isActiveUser) {
            Color(0xFF22C55E)
        } else {
            Color(0xFFEF4444)
        }

    val textAlign = adminTextAlign(isEnglish)
    val contentAlignment = if (isEnglish) Alignment.Start else Alignment.End
    val rowArrangement = if (isEnglish) Arrangement.Start else Arrangement.End

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF020617)
        ),
        border = BorderStroke(1.dp, beltColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEnglish) {
                UserBeltAccent(beltColor)
                Spacer(Modifier.width(10.dp))
                UserAvatar(
                    displayName = displayName,
                    beltColor = beltColor
                )
                Spacer(Modifier.width(10.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = contentAlignment
            ) {
                // שם המתאמן – שורה נפרדת כדי שלא ייחתך בגלל התגים
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE5E7EB),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = if (isEnglish) {
                        TextAlign.Left
                    } else {
                        TextAlign.Right
                    },
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

// תפקיד + סטטוס פעילות – בתחילת השורה
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        UserRoleBadge(
                            label = roleLabel,
                            isCoach = user.isCoach
                        )

                        Spacer(Modifier.width(6.dp))

                        UserActivityBadge(
                            label = activityStatusLabel,
                            color = activityStatusColor
                        )
                    } else {
                        UserActivityBadge(
                            label = activityStatusLabel,
                            color = activityStatusColor
                        )

                        Spacer(Modifier.width(6.dp))

                        UserRoleBadge(
                            label = roleLabel,
                            isCoach = user.isCoach
                        )
                    }
                }

                val ageText = user.age?.toString() ?: adminTr(isEnglish, "לא ידוע", "Unknown")
                val regionText =
                    user.displayRegionText(isEnglish)

                val assignmentsText =
                    user.displayBranchAssignmentsText(
                        isEnglish
                    )

                Text(
                    text = adminTr(
                        isEnglish,
                        "$beltText  •  גיל: $ageText  •  אזור: $regionText",
                        "$beltText  •  Age: $ageText  •  Region: $regionText"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = adminTr(
                        isEnglish,
                        "סניפים וקבוצות:\n$assignmentsText",
                        "Branches and groups:\n$assignmentsText"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )

                val appTenureText = user.displayAppTenureText(isEnglish)
                val lastSeenText = user.displayLastSeenText(isEnglish)

                Text(
                    text = adminTr(
                        isEnglish,
                        "וותק באפליקציה: $appTenureText",
                        "App tenure: $appTenureText"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7DD3FC),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )


                Text(
                    text = adminTr(
                        isEnglish,
                        "שימושים באפליקציה: ${user.appOpenCount} • שימוש אחרון: $lastSeenText",
                        "App opens: ${user.appOpenCount} • Last use: $lastSeenText"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF67E8F9),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isEnglish) {
                Spacer(Modifier.width(10.dp))
                UserAvatar(
                    displayName = displayName,
                    beltColor = beltColor
                )
                Spacer(Modifier.width(10.dp))
                UserBeltAccent(beltColor)
            }
        }
    }
}

@Composable
private fun UserActivityBadge(
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun UserBeltAccent(
    beltColor: Color
) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(beltColor)
    )
}

@Composable
private fun UserAvatar(
    displayName: String,
    beltColor: Color
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        beltColor.copy(alpha = 0.95f),
                        beltColor.copy(alpha = 0.75f),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName
                .split(" ")
                .take(2)
                .joinToString("") {
                    it.firstOrNull()
                        ?.toString()
                        .orEmpty()
                },
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UserRoleBadge(
    label: String,
    isCoach: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isCoach) Color(0xFF7C3AED).copy(alpha = 0.18f)
                else Color(0xFF0EA5E9).copy(alpha = 0.18f)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCoach) Color(0xFFC4B5FD) else Color(0xFFBAE6FD),
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
