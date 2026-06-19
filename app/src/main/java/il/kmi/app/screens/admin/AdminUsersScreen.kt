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

    // חישוב גיל ללא java.time – עובד על כל מכשיר
    val age: Int?
        get() {
            val year = birthYear ?: return null
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val rough = currentYear - year
            // פילטר בסיסי לערכים לא הגיוניים
            return if (rough in 0..120) rough else null
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
 * מפתח דה-דופ – מאחד מסמכים של אותו משתמש:
 * קודם לפי מייל, אחר כך טלפון, אחר כך uid, ואם אין – לפי שם.
 */
private fun AdminUserRecord.dedupeKey(): String {
    // 1) מייל – הכי יציב לזיהוי אותו אדם
    email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { mail ->
        return "email:$mail"
    }

    // 2) טלפון – מורידים כל מה שלא ספרות
    phone?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }?.let { digits ->
        return "phone:$digits"
    }

    // 3) uid – רק אם אין מייל/טלפון
    uidField?.trim()?.takeIf { it.isNotEmpty() }?.let { uid ->
        return "uid:$uid"
    }

    // 4) נפילה אחרונה – שם מלא
    return "name:${fullName.trim()}"
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
        for (key in keys) {
            val value = intOrNull(key)
            if (value != null && value > 0) return value
        }
        return 0
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
    val uidField = stringOrNull("uid", "userId")

    @Suppress("UNCHECKED_CAST")
    val groupsList = (get("groups") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

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
        branches = branchList,
        groups = groupsList,
        currentBeltId = stringOrNull("currentBeltId", "currentBelt", "belt_current", "beltId", "belt"),
        phone = stringOrNull("phone", "phoneNumber"),
        email = stringOrNull("email"),

        // ✅ חדש
        role = role,
        isCoachFlag = isCoachFlag,

        createdAtMillis = createdMillis,

        // ✅ נתוני שימוש באפליקציה
        // קורא כמה שמות אפשריים כדי לא להציג 0 אם השדה נשמר בשם אחר
        appOpenCount = intFromAnyField(
            "appOpenCount",
            "app_open_count",
            "appOpens",
            "openCount",
            "opensCount",
            "launchCount",
            "loginCount",
            "usageCount",
            "sessionsCount",
            "screenViewCount"
        ),
        lastSeenAtMillis = adminMillisFromFirestore(
            get("lastSeenAtMillis")
                ?: get("lastSeenAt")
                ?: get("lastLoginAtMillis")
                ?: get("lastLoginAt")
                ?: get("lastOpenAtMillis")
                ?: get("lastOpenAt")
                ?: get("lastUsedAtMillis")
                ?: get("lastUsedAt")
                ?: get("updatedAt")
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

            loadedUsers = raw
                .groupBy { it.dedupeKey() }
                .map { (_, list) ->
                    list.maxWithOrNull(
                        compareBy<AdminUserRecord> {
                            // קודם מעדיפים רשומה שיש בה שימושים בפועל
                            it.appOpenCount
                        }.thenBy {
                            // אחר כך שימוש אחרון
                            it.lastSeenAtMillis ?: 0L
                        }.thenBy {
                            // אחר כך תאריך יצירה
                            it.createdAtMillis ?: 0L
                        }.thenBy {
                            // אחר כך רשומה עם שם אמיתי
                            if (it.fullName.startsWith("Unknown user", ignoreCase = true)) 0 else 1
                        }
                    ) ?: list.first()
                }
                .sortedWith(
                    compareBy<AdminUserRecord> {
                        it.fullName.startsWith("Unknown user", ignoreCase = true)
                    }.thenBy {
                        it.fullName.trim().lowercase()
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
        } else {
            loading = true
        }

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

    Scaffold(
        topBar = {

            KmiTopBar(
                title = adminTr(isEnglish, "ניהול משתמשים", "User management"),

                // ✅ מפעיל את אייקון הבית בסרגל האייקונים הצדדי
                onHome = onHome,

                // ✅ לא מציגים בית/חיפוש בכותרת העליונה עצמה,
                // אלא רק בסרגל האייקונים הצדדי כמו בשאר המסכים
                showTopHome = false,
                showTopSearch = false,

                // ✅ חובה false כדי שאייקון החיפוש בסרגל הצדדי יעבוד
                lockSearch = false,

                // ✅ הבית לא נעול
                lockHome = false,

                showBottomActions = true,
                currentLang = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",
                onToggleLanguage = {
                    val newLang =
                        if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    (contextLang as? Activity)?.recreate()
                }
                // אין צורך ב-onBack כי זה לא דיאלוג נוסף
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
                UserAvatar(user = user, beltColor = beltColor)
                Spacer(Modifier.width(10.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = contentAlignment
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = rowArrangement,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFE5E7EB),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Left,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(8.dp))

                        UserRoleBadge(
                            label = roleLabel,
                            isCoach = user.isCoach
                        )
                    } else {
                        UserRoleBadge(
                            label = roleLabel,
                            isCoach = user.isCoach
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFE5E7EB),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val ageText = user.age?.toString() ?: adminTr(isEnglish, "לא ידוע", "Unknown")
                val regionText = user.displayRegionText(isEnglish)
                val branchesText = user.displayBranchesText(isEnglish)

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
                        "סניפים: $branchesText",
                        "Branches: $branchesText"
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

                val groups = user.groups.joinToString(", ").ifBlank {
                    adminTr(isEnglish, "ללא קבוצות", "No groups")
                }

                Text(
                    text = adminTr(
                        isEnglish,
                        "קבוצות: $groups",
                        "Groups: $groups"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isEnglish) {
                Spacer(Modifier.width(10.dp))
                UserAvatar(user = user, beltColor = beltColor)
                Spacer(Modifier.width(10.dp))
                UserBeltAccent(beltColor)
            }
        }
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
    user: AdminUserRecord,
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
            text = user.fullName
                .split(" ")
                .take(2)
                .joinToString("") { it.firstOrNull()?.toString() ?: "" },
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
