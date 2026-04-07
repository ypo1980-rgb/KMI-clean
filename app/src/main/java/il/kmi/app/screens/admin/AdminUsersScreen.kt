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
import il.kmi.app.privacy.DemoPrivacy
import il.kmi.app.privacy.DemoTrainees
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

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
            ?: "ללא שם (${id.take(6)})"

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
        currentBeltId = stringOrNull("currentBeltId", "beltId", "belt"),
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
// --- שאלות מסומנות UNLIKE מה-AI ---
    var aiFeedback by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val snap = Firebase.firestore
                .collection("aiFeedback")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            aiFeedback = snap.documents.map { it.data ?: emptyMap() }
        } catch (t: Throwable) {
            Log.e("KMI_ADMIN", "Failed to load AI feedback", t)
        }
    }

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
            val rawErr = t.message ?: "שגיאה בטעינת המשתמשים"
            errorMsg = if (rawErr.contains("PERMISSION_DENIED")) {
                "אין לך הרשאה לצפות ברשימת המשתמשים. בדוק את הגדרות ההרשאות או פנה למנהל המערכת."
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
    val allBelts = remember(users) {
        users.mapNotNull { it.belt?.heb }
            .distinct()
            .sorted()
    }
    val allAgeBuckets = remember(users) {
        users.map { it.ageBucket }.distinct().sortedBy { it }
    }

    val filteredUsers = remember(users, genderFilter, regionFilter, beltFilter, ageBucketFilter) {
        users.filter { u ->
            val gOk = genderFilter == null ||
                    (genderFilter == "male" && (u.gender ?: "").lowercase().startsWith("m")) ||
                    (genderFilter == "female" && (u.gender ?: "").lowercase().startsWith("f"))
            val rOk = regionFilter == null || u.region == regionFilter
            val bOk = beltFilter == null || u.belt?.heb == beltFilter
            val aOk = ageBucketFilter == null || u.ageBucket == ageBucketFilter
            gOk && rOk && bOk && aOk
        }
    }

    val coachUsers = remember(filteredUsers) { filteredUsers.filter { it.isCoach } }
    val traineeUsers = remember(filteredUsers) { filteredUsers.filter { !it.isCoach } }

    val traineeUiUsers = remember(traineeUsers) {
        if (!DemoPrivacy.ENABLED) {
            traineeUsers
        } else {
            traineeUsers.mapIndexed { index, user ->
                val demo = DemoTrainees.trainees.getOrNull(index)

                user.copy(
                    fullName = demo?.name ?: "מתאמן ${index + 1}",
                    birthYear = demo?.age?.let {
                        Calendar.getInstance().get(Calendar.YEAR) - it
                    } ?: user.birthYear,
                    currentBeltId = demo?.belt?.id ?: user.currentBeltId,
                    branch = demo?.branch ?: user.branch,
                    groups = listOf(
                        "וותק: ${demo?.yearsTraining ?: ((index % 6) + 1)} שנים"
                    ),
                    phone = null,
                    email = null
                )
            }
        }
    }

    val coachUiUsers = remember(coachUsers) {
        if (!DemoPrivacy.ENABLED) {
            coachUsers
        } else {
            coachUsers.mapIndexed { index, user ->
                user.copy(
                    fullName = "מאמן ${index + 1}",
                    birthYear = user.birthYear ?: (Calendar.getInstance().get(Calendar.YEAR) - (32 + index)),
                    currentBeltId = user.currentBeltId ?: Belt.BLACK.id,
                    branch = user.branch ?: listOf("אופק", "סוקולוב", "נתניה").getOrElse(index % 3) { "אופק" },
                    groups = listOf("קבוצת בוגרים"),
                    phone = null,
                    email = null
                )
            }
        }
    }

    // -------- סטטיסטיקות כלליות --------
    val totalUsers = users.size
    val genderCounts = users.groupBy { (it.gender ?: "unknown").lowercase() }
        .mapValues { it.value.size }

    val regionCounts = users.groupBy { it.region ?: "לא ידוע" }
        .mapValues { it.value.size }

    val beltCountsRaw = users.groupBy { it.belt?.heb ?: "ללא חגורה" }
        .mapValues { it.value.size }

    // רשימה מסודרת: קודם "ללא חגורה", אח"כ כל החגורות לפי Belt.order
    val beltCountsOrdered: List<Pair<String, Int>> = buildList {
        add("ללא חגורה" to (beltCountsRaw["ללא חגורה"] ?: 0))
        Belt.order.forEach { belt ->
            add(belt.heb to (beltCountsRaw[belt.heb] ?: 0))
        }
    }

    val avgAge = users.mapNotNull { it.age }.takeIf { it.isNotEmpty() }?.average()

    val outerScroll = rememberScrollState()

    Scaffold(
        topBar = {
            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            KmiTopBar(
                title = "ניהול משתמשים",
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
                        title = "סה\"כ משתמשים",
                        value = if (loading) "…" else totalUsers.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "מס' סניפים",
                        value = if (loading) "…" else regionCounts.keys.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "גיל ממוצע",
                        value = if (loading) "…"
                        else avgAge?.let { String.format("%.1f", it) } ?: "לא ידוע",
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
                    title = "חלוקה לפי מין",
                    data = listOf(
                        "זכר" to (genderCounts["male"] ?: genderCounts["m"] ?: 0),
                        "נקבה" to (genderCounts["female"] ?: genderCounts["f"] ?: 0),
                        "לא ידוע" to (genderCounts["unknown"] ?: 0)
                    ),
                    accent = Color(0xFF38BDF8)
                )

                // ---------- חלוקה לפי חגורה – פילולים צבעוניים עם גלילה אופקית ----------
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
                            text = "חלוקה לפי חגורה",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE5E7EB),
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            beltCountsOrdered.forEach { (label, value) ->
                                val belt = Belt.order.firstOrNull { it.heb == label }
                                val circleColor = belt?.color ?: Color(0xFF6B7280)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    // "פיל" בגובה ורוחב קבועים לכל החגורות
                                    Box(
                                        modifier = Modifier
                                            .width(66.dp)      // היה 44.dp
                                            .height(39.dp)     // היה 26.dp
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
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    }
                }

                // ---------- פילטרים ----------
                FilterRow(
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
                            text = "משתמשים – מתאמנים (${traineeUiUsers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        if (loading) {
                            Text(
                                text = "טוען משתמשים…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        } else if (traineeUiUsers.isEmpty()) {
                            Text(
                                text = "אין מתאמנים מתאימים לפילטרים.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                traineeUiUsers.forEach { user ->
                                    UserRowCard(user = user)
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
                            text = "משתמשים – מאמנים (${coachUiUsers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        if (loading) {
                            Text(
                                text = "טוען משתמשים…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        } else if (coachUiUsers.isEmpty()) {
                            Text(
                                text = "אין מאמנים מתאימים לפילטרים.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                coachUiUsers.forEach { user ->
                                    UserRowCard(user = user)
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
                                text = "שאלות לסקירה (UNLIKE)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE5E7EB),
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "רשימת שאלות שהעוזר לא ענה עליהן טוב – לסקירה ולשיפור מאגר התכנים.",
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
            text = "פילטרים",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFFE5E7EB),
            fontWeight = FontWeight.SemiBold
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
                label = { Text("הכל") },
                colors = chipColors
            )
            FilterChip(
                selected = genderFilter == "male",
                onClick = { onGenderChange("male") },
                label = { Text("זכר") },
                colors = chipColors
            )
            FilterChip(
                selected = genderFilter == "female",
                onClick = { onGenderChange("female") },
                label = { Text("נקבה") },
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
                label = { Text("כל האזורים") },
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
                label = { Text("כל החגורות") },
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
                label = { Text("כל הגילים") },
                colors = chipColors
            )
            ageBuckets.forEach { bucket ->
                FilterChip(
                    selected = ageBucketFilter == bucket,
                    onClick = { onAgeBucketChange(bucket) },
                    label = { Text(bucket) },
                    colors = chipColors
                )
            }
        }
    }
}

@Composable
private fun UserRowCard(
    user: AdminUserRecord
) {
    val beltColor = user.belt?.color ?: Color(0xFF6B7280)
    val roleLabel = if (user.isCoach) "מאמן" else "מתאמן"

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
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(beltColor)
            )

            Spacer(Modifier.width(10.dp))

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

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFE5E7EB),
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (user.isCoach) Color(0xFF7C3AED).copy(alpha = 0.18f)
                                else Color(0xFF0EA5E9).copy(alpha = 0.18f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = roleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.isCoach) Color(0xFFC4B5FD) else Color(0xFFBAE6FD),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val beltText = user.belt?.heb ?: "ללא חגורה"
                val ageText = user.age?.toString() ?: "לא ידוע"
                val regionBranch =
                    listOfNotNull(user.region, user.branch).joinToString(" · ").ifBlank { "—" }

                Text(
                    text = "$beltText  •  גיל: $ageText  •  $regionBranch",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )

                val groups = user.groups.joinToString(", ").ifBlank { "ללא קבוצות" }
                Text(
                    text = "קבוצות: $groups",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}
