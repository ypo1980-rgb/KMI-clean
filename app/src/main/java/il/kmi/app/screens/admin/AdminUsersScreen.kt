package il.kmi.app.screens.admin

import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.shared.domain.Belt
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.ext.color
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

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
    val groups: List<String>,
    val currentBeltId: String?,
    val phone: String?,
    val email: String?,

    // ✅ חדש: שדות לזיהוי מאמן/מתאמן
    val role: String? = null,
    val isCoachFlag: Boolean? = null,

    val createdAtMillis: Long?
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
    val createdMillis = when (val v = get("createdAtMillis") ?: get("createdAt")) {
        is Long -> v
        is Int -> v.toLong()
        is Double -> v.toLong()
        else -> null
    }

    val role = stringOrNull("role", "userType", "type")
    val isCoachFlag = boolOrNull("isCoach", "coach", "isTrainer", "trainer")

    return AdminUserRecord(
        id = id,
        uidField = uidField,
        fullName = name,
        gender = stringOrNull("gender", "sex"),
        birthDay = birthDay,
        birthMonth = birthMonth,
        birthYear = birthYear,
        region = stringOrNull("region", "area"),
        branch = stringOrNull("branch", "club", "dojo"),
        groups = groupsList,
        currentBeltId = stringOrNull("currentBeltId", "currentBelt", "belt_current", "beltId", "belt"),
        phone = stringOrNull("phone", "phoneNumber"),
        email = stringOrNull("email"),

        // ✅ חדש
        role = role,
        isCoachFlag = isCoachFlag,

        createdAtMillis = createdMillis
    )
}

// ===========================
//   מסך ניהול משתמשים
// ===========================
@Composable
fun AdminUsersScreen(
    onBack: () -> Unit
) {
    Log.d("KMI_ADMIN", "AdminUsersScreen composed ✅")

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

    // --- מצב נתונים מ-Firestore ---
    var users by remember { mutableStateOf<List<AdminUserRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
// 👇 שאלות שסומנו UNLIKE בעוזר הקולי
    var unlikeQuestions by remember { mutableStateOf<List<AdminUserRecord.AssistantQuestionRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        // לוג קטן כדי לראות איזה UID מחובר בפועל
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("KMI_ADMIN", "current uid = $currentUid")

        loading = true
        errorMsg = null
        try {
            Log.d("KMI_ADMIN", "Loading users collection...")

            val snap = Firebase.firestore
                .collection("users")
                .get()
                .await()

            Log.d("KMI_ADMIN", "users snap size = ${snap.size()}")

            val raw = snap.documents
                .mapNotNull { doc ->
                    val rec = doc.toAdminUserRecord()
                    if (rec == null) {
                        Log.w("KMI_ADMIN", "users doc skipped id=${doc.id} keys=${doc.data?.keys}")
                    }
                    rec
                }

            Log.d("KMI_ADMIN", "users parsed = ${raw.size}")

            users = raw
                .groupBy { it.dedupeKey() }
                .map { (_, list) ->
                    list.maxByOrNull { it.createdAtMillis ?: 0L } ?: list.first()
                }
                .sortedBy { it.fullName }

            Log.d("KMI_ADMIN", "users after dedupe = ${users.size}")

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
            } else rawErr

            Log.e("KMI_ADMIN", "loading users failed", t)
        } finally {
            loading = false
        }

        // --- טעינת שאלות UNLIKE מהעוזר הקולי (לא משפיע על errorMsg) ---
        try {
            val feedbackSnap = Firebase.firestore
                .collection("assistantFeedback")
                .whereEqualTo("liked", false)              // רק UNLIKE
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            unlikeQuestions = feedbackSnap.documents.mapNotNull { doc ->
                val qText = doc.getString("question") ?: return@mapNotNull null
                AdminUserRecord.AssistantQuestionRecord(
                    id = doc.id,
                    question = qText,
                    answer = doc.getString("answer"),
                    createdAtMillis = (doc.get("createdAt") as? Long)
                        ?: (doc.get("ts") as? Long),
                    userName = doc.getString("userName"),
                    userUid = doc.getString("userUid")
                )
            }
        } catch (t: Throwable) {
            Log.w("KMI_ADMIN", "loading assistantFeedback failed", t)
            // לא מציגים שגיאה למשתמש – פשוט לא יהיו שאלות ברשימה
            unlikeQuestions = emptyList()
        }
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
        users.map { it.ageBucket }.distinct().sortedBy { it }
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
    val genderCounts = users.groupBy { (it.gender ?: "unknown").lowercase() }
        .mapValues { it.value.size }

    val regionCounts = users.groupBy { it.region ?: "לא ידוע" }
        .mapValues { it.value.size }

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

    val outerScroll = rememberScrollState()

    Scaffold(
        topBar = {

            KmiTopBar(
                title = adminTr(isEnglish, "ניהול משתמשים", "User management"),
                onHome = onBack,        // NavGraph מההורה callback לפי הביתה
                showTopHome = false,    // למנוע אייקון בית
                lockSearch = true,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = adminTr(isEnglish, "סה\"כ משתמשים", "Total users"),
                        value = if (loading) "…" else totalUsers.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = adminTr(isEnglish, "מס' סניפים", "Branches"),
                        value = if (loading) "…" else regionCounts.keys.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = adminTr(isEnglish, "גיל ממוצע", "Avg. age"),
                        value = if (loading) {
                            "…"
                        } else {
                            avgAge?.let { String.format("%.1f", it) }
                                ?: adminTr(isEnglish, "לא ידוע", "Unknown")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // הודעת שגיאה (אם יש)
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color(0xFFF97373),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // ---------- גרף קטן – לפי מין ----------
                MiniBarChartCard(
                    title = adminTr(isEnglish, "חלוקה לפי מין", "Gender distribution"),
                    data = listOf(
                        adminTr(isEnglish, "זכר", "Male") to (genderCounts["male"] ?: genderCounts["m"] ?: 0),
                        adminTr(isEnglish, "נקבה", "Female") to (genderCounts["female"] ?: genderCounts["f"] ?: 0),
                        adminTr(isEnglish, "לא ידוע", "Unknown") to (genderCounts["unknown"] ?: 0)
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
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = adminTr(
                                    isEnglish,
                                    "רשימת שאלות שהעוזר לא ענה עליהן טוב – לסקירה ולשיפור מאגר התכנים.",
                                    "Questions where the assistant response was marked as not helpful — for review and content improvement."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
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
                                            color = Color(0xFFE5E7EB)
                                        )

                                        val meta = listOfNotNull(
                                            fb.userName,
                                            fb.userUid
                                        ).joinToString(" • ")

                                        if (meta.isNotBlank()) {
                                            Text(
                                                text = meta,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF9CA3AF)
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
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF020617).copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF9CA3AF),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFE5E7EB),
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = adminTr(isEnglish, "פילטרים", "Filters"),
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFE5E7EB),
            fontWeight = FontWeight.SemiBold,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )

        // מין
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                containerColor = Color(0xFF0B1220),              // ✅ רקע ברור לכפתור לא-נבחר
                labelColor = Color(0xFFE5E7EB),                  // ✅ טקסט בהיר
                selectedContainerColor = Color(0xFF0EA5E9),      // ✅ נבחר
                selectedLabelColor = Color(0xFF020617)           // ✅ טקסט כהה על נבחר
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

        // אזור
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

        // חגורה  🔹 תוקן – שורה נגללת אופקית
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

        // קבוצת גיל
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
                val regionBranch =
                    listOfNotNull(user.region, user.branch).joinToString(" · ").ifBlank { "—" }

                Text(
                    text = adminTr(
                        isEnglish,
                        "$beltText  •  גיל: $ageText  •  $regionBranch",
                        "$beltText  •  Age: $ageText  •  $regionBranch"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
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
