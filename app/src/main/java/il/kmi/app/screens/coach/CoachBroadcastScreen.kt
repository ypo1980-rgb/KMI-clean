package il.kmi.app.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import android.app.Activity
import android.content.SharedPreferences
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.SetOptions
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

//======================================================================

private fun coachBroadcastTr(isEnglish: Boolean, he: String, en: String): String {
    return if (isEnglish) en else he
}

private fun coachBroadcastTextAlign(isEnglish: Boolean): TextAlign {
    return if (isEnglish) TextAlign.Left else TextAlign.Right
}

fun persistCoachBroadcast(
    region: String,
    branch: String,
    message: String,
    targetUids: List<String>,
    targetRecipients: List<Map<String, String>> = emptyList(),
    onResult: (Boolean, Throwable?) -> Unit = { _, _ -> }
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val currentUser = auth.currentUser
    val currentUid = currentUser?.uid

    if (currentUid.isNullOrBlank()) {
        onResult(false, IllegalStateException("No logged-in user"))
        return
    }

    val cleanRegion = region.trim()
    val cleanBranch = branch.trim()
    val cleanMessage = message.trim()
    val cleanTargetUids = targetUids
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    val cleanTargetRecipients = targetRecipients
        .mapNotNull { raw ->
            val uid = raw["uid"].orEmpty().trim()
            val name = raw["name"].orEmpty().trim()
            val phone = raw["phone"].orEmpty().trim()
            val email = raw["email"].orEmpty().trim()

            if (uid.isBlank() && phone.isBlank() && email.isBlank()) {
                null
            } else {
                mapOf(
                    "uid" to uid,
                    "name" to name,
                    "phone" to phone,
                    "email" to email
                )
            }
        }
        .distinctBy { recipient ->
            recipient["uid"]?.takeIf { it.isNotBlank() }
                ?: recipient["phone"]?.takeIf { it.isNotBlank() }
                ?: recipient["email"].orEmpty()
        }

    val cleanTargetPhones = cleanTargetRecipients
        .mapNotNull { it["phone"]?.trim()?.takeIf { phone -> phone.isNotBlank() } }
        .distinct()

    val cleanTargetNames = cleanTargetRecipients
        .mapNotNull { it["name"]?.trim()?.takeIf { name -> name.isNotBlank() } }
        .distinct()

    val cleanTargetEmails = cleanTargetRecipients
        .mapNotNull { it["email"]?.trim()?.takeIf { email -> email.isNotBlank() } }
        .distinct()

    if (cleanRegion.isBlank() || cleanBranch.isBlank()) {
        onResult(false, IllegalStateException("Missing region/branch"))
        return
    }

    if (cleanMessage.isBlank()) {
        onResult(false, IllegalStateException("Missing message"))
        return
    }

    if (cleanTargetUids.isEmpty()) {
        onResult(false, IllegalStateException("No selected recipients"))
        return
    }

    val coachName = currentUser.displayName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: currentUser.email
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        ?: "מאמן"

    val nowMillis = System.currentTimeMillis()
    val expiresAtMillis = nowMillis + 30L * 24L * 60L * 60L * 1000L

    val docRef = db.collection("coachBroadcasts").document()
    val broadcastId = docRef.id

    val data = hashMapOf<String, Any?>(
        "broadcastId" to broadcastId,

        // סוג הודעה — חשוב ל־Cloud Function / Android Intent
        "type" to "coach_broadcast",

        "authorUid" to currentUid,
        "coachUid" to currentUid,
        "coachName" to coachName,

        "region" to cleanRegion,
        "branch" to cleanBranch,

        // תאימות למסכים קיימים
        "text" to cleanMessage,
        "message" to cleanMessage,
        "body" to cleanMessage,

        // נמענים אמיתיים
        "targetUids" to cleanTargetUids,
        "targetUidCount" to cleanTargetUids.size,
        "targetCount" to cleanTargetUids.size,

        // Snapshot קריא של הנמענים בזמן השליחה
        "targetRecipients" to cleanTargetRecipients,
        "targetPhones" to cleanTargetPhones,
        "targetNames" to cleanTargetNames,
        "targetEmails" to cleanTargetEmails,
        "targetRecipientSnapshotCount" to cleanTargetRecipients.size,

        // הכנה ל־Push
        "pushEnabled" to true,
        "pushTarget" to "targetUids",
        "pushStatus" to "pending",
        "pushCreatedBy" to "android_coach_broadcast",

        // זמנים
        "createdAt" to FieldValue.serverTimestamp(),
        "createdAtMillis" to nowMillis,
        "sentAtMillis" to nowMillis,

        // TTL: Firestore ימחק את ההודעה אוטומטית אחרי 30 יום
        "expiresAt" to Timestamp(Date(expiresAtMillis)),
        "expiresAtMillis" to expiresAtMillis,

        "source" to "android_coach_broadcast"
    )

    android.util.Log.e(
        "KMI_COACH_BROADCAST",
        "persist start broadcastId=$broadcastId authorUid=$currentUid region=$cleanRegion branch=$cleanBranch targetUids=$cleanTargetUids message=$cleanMessage"
    )

    docRef
        .set(data.filterValues { it != null }, SetOptions.merge())
        .addOnSuccessListener {
            android.util.Log.e(
                "KMI_COACH_BROADCAST",
                "persist SUCCESS broadcastId=$broadcastId targetUids=$cleanTargetUids"
            )
            onResult(true, null)
        }
        .addOnFailureListener { e ->
            android.util.Log.e(
                "KMI_COACH_BROADCAST",
                "persist FAILED broadcastId=$broadcastId targetUids=$cleanTargetUids",
                e
            )
            onResult(false, e)
        }
}

// ייצוג נמען אחד ברשימת הקבוצה
private data class CoachRecipient(
    val uid: String,
    val name: String,
    val phone: String,
    val email: String,
    val selected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachBroadcastScreen(
    branchesByRegion: Map<String, List<String>>,
    defaultRegion: String?,
    defaultBranch: String?,
    onBack: () -> Unit,
    onHome: () -> Unit,

    // ✅ פעולות פלטפורמה
    onOpenSms: (numbers: List<String>, message: String) -> Unit = { _, _ -> },
    onShareText: (message: String) -> Unit = {}
) {
    val contextLang = LocalContext.current
    val langManager = remember { AppLanguageManager(contextLang) }

    var currentLanguage by remember {
        mutableStateOf(langManager.getCurrentLanguage())
    }

    val isEnglish = currentLanguage == AppLanguage.ENGLISH
    val screenTextAlign = coachBroadcastTextAlign(isEnglish)
    val layoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    val userSp = remember(contextLang) {
        contextLang.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    fun readCoachGroupKey(): String {
        return userSp.getString("active_group", null)
            ?: userSp.getString("activeGroup", null)
            ?: userSp.getString("primaryGroup", null)
            ?: userSp.getString("groupKey", null)
            ?: userSp.getString("group_key", null)
            ?: userSp.getString("age_group", null)
            ?: userSp.getString("group", null)
            ?: ""
    }

    var coachGroupKey by remember {
        mutableStateOf(readCoachGroupKey())
    }

    androidx.compose.runtime.DisposableEffect(userSp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "active_group" ||
                key == "activeGroup" ||
                key == "primaryGroup" ||
                key == "groupKey" ||
                key == "group_key" ||
                key == "age_group" ||
                key == "group"
            ) {
                coachGroupKey = readCoachGroupKey()
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var sendScope by remember { mutableStateOf("group") }

    val effectiveGroupKey = remember(sendScope, coachGroupKey) {
        if (sendScope == "branch") "" else coachGroupKey.trim()
    }

    var region by remember { mutableStateOf(defaultRegion.orEmpty()) }
    var branch by remember { mutableStateOf(defaultBranch.orEmpty()) }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    var expandedRegion by remember { mutableStateOf(false) }
    var expandedBranch by remember { mutableStateOf(false) }

    // רשימת הנמענים מהקבוצה (נשלפת מפיירסטור)
    var recipients by remember { mutableStateOf<List<CoachRecipient>>(emptyList()) }

    val db = remember { FirebaseFirestore.getInstance() }

    // Snackbar לפידבק
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // listener כדי שנוכל להסיר כשמשנים region/branch
    var usersListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    // ===== טעינת חברי הקבוצה מה- Firestore לפי אזור + סניף + קבוצה =====
    LaunchedEffect(region, branch, effectiveGroupKey) {
        usersListener?.remove()
        usersListener = null

        fun String.norm(): String {
            return trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun String.pickPrimaryBranch(): String {
            return split(",", "•", "|")
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
                ?: trim()
        }

        fun splitTokensNorm(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()

            return raw
                .replace(" • ", ",")
                .replace("|", ",")
                .replace("\n", ",")
                .split(',', ';', '；')
                .map { it.norm() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun expandGroupAliases(raw: String): List<String> {
            val n = raw.norm()

            return buildList {
                add(n)
                addAll(splitTokensNorm(n))

                if (n.contains("נוער") && n.contains("בוגרים")) {
                    add("נוער")
                    add("בוגרים")
                    add("נוער ובוגרים")
                    add("נוער + בוגרים")
                }

                if (n.contains("children", ignoreCase = true)) add("ילדים")
                if (n.contains("kids", ignoreCase = true)) add("ילדים")
                if (n.contains("youth", ignoreCase = true)) add("נוער")
                if (n.contains("adults", ignoreCase = true)) add("בוגרים")
                if (n.contains("adult", ignoreCase = true)) add("בוגרים")
            }
                .map { it.norm() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun DocumentSnapshot.roleText(): String {
            return (
                    getString("role")
                        ?: getString("userType")
                        ?: getString("type")
                        ?: ""
                    )
                .trim()
                .lowercase()
        }

        fun DocumentSnapshot.isActiveUser(): Boolean {
            val activeBoolean = getBoolean("isActive")
            val activeText = (
                    getString("status")
                        ?: getString("active")
                        ?: ""
                    ).trim().lowercase()

            return activeBoolean != false &&
                    activeText != "inactive" &&
                    activeText != "disabled" &&
                    activeText != "blocked" &&
                    activeText != "לא פעיל"
        }

        fun DocumentSnapshot.isTraineeRole(): Boolean {
            val role = roleText()

            return role.isBlank() ||
                    role == "trainee" ||
                    role.contains("trainee") ||
                    role.contains("student") ||
                    role.contains("מתאמן") ||
                    role.contains("חניך")
        }

        fun DocumentSnapshot.branchTokensNorm(): List<String> {
            val branchesList = (get("branches") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                .orEmpty()

            return buildList {
                addAll(branchesList.map { it.norm() })
                addAll(splitTokensNorm(getString("branchesCsv")))
                addAll(splitTokensNorm(getString("branch")))
                addAll(splitTokensNorm(getString("activeBranch")))
                addAll(splitTokensNorm(getString("active_branch")))
            }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun DocumentSnapshot.groupTokensNorm(): List<String> {
            val groupsList = (get("groups") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                ?.flatMap { expandGroupAliases(it) }
                .orEmpty()

            return buildList {
                addAll(groupsList)
                addAll(splitTokensNorm(getString("primaryGroup")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("activeGroup")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("active_group")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupKey")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("group_key")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("group")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupName")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupsCsv")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupCsv")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("age_group")).flatMap { expandGroupAliases(it) })
            }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun matchesAnyToken(tokens: List<String>, candidates: Set<String>): Boolean {
            if (tokens.isEmpty() || candidates.isEmpty()) return false

            return tokens.any { token ->
                token in candidates ||
                        candidates.any { candidate ->
                            candidate.length >= 2 &&
                                    token.length >= 2 &&
                                    (token.contains(candidate) || candidate.contains(token))
                        }
            }
        }

        fun DocumentSnapshot.displayNameOrPhone(phone: String): String {
            return getString("fullName")?.takeIf { it.isNotBlank() }
                ?: getString("name")?.takeIf { it.isNotBlank() }
                ?: getString("displayName")?.takeIf { it.isNotBlank() }
                ?: getString("email")?.takeIf { it.isNotBlank() }
                ?: phone
        }

        val regionNorm = region.norm()
        val branchPrimary = branch.norm().pickPrimaryBranch()
        val groupNorm = effectiveGroupKey.norm()

        if (regionNorm.isBlank() || branchPrimary.isBlank()) {
            recipients = emptyList()
            return@LaunchedEffect
        }

        val branchCandidates = listOf(
            branchPrimary,
            branchPrimary.replace("-", "–"),
            branchPrimary.replace("-", "—"),
            branchPrimary.replace("-", "־"),
            branchPrimary.replace("–", "-"),
            branchPrimary.replace("—", "-"),
            branchPrimary.replace("־", "-")
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val branchCandidateSet = branchCandidates
            .map { it.norm() }
            .toSet()

        val groupCandidateSet = expandGroupAliases(groupNorm)
            .toSet()

        android.util.Log.e(
            "KMI_COACH_BROADCAST",
            "loading recipients region=$regionNorm branch=$branchPrimary group=$groupNorm branchCandidates=$branchCandidateSet groupCandidates=$groupCandidateSet"
        )

        fun docsToRecipients(docs: List<DocumentSnapshot>): List<CoachRecipient> {
            val currentSelectionByUid = recipients.associate { it.uid to it.selected }
            val currentSelectionByPhone = recipients.associate { it.phone to it.selected }

            return docs
                .asSequence()
                .filter { it.isActiveUser() }
                .filter { it.isTraineeRole() }
                .filter { matchesAnyToken(it.branchTokensNorm(), branchCandidateSet) }
                .filter {
                    // אם אין לקואץ׳ קבוצה מוגדרת — לא מסננים לפי קבוצה.
                    // אם יש קבוצה — שולחים רק למי ששייך לקבוצה הזו.
                    groupCandidateSet.isEmpty() ||
                            matchesAnyToken(it.groupTokensNorm(), groupCandidateSet)
                }
                .mapNotNull { doc ->
                    val phone = (
                            doc.getString("phone")
                                ?: doc.getString("phoneNumber")
                                ?: doc.getString("phone_number")
                                ?: ""
                            ).trim()

                    if (phone.isBlank()) return@mapNotNull null

                    val uid = (
                            doc.getString("uid")
                                ?: doc.getString("authUid")
                                ?: doc.id
                            ).trim()

                    val name = doc.displayNameOrPhone(phone).trim()
                    val email = doc.getString("email")
                        ?.trim()
                        .orEmpty()

                    CoachRecipient(
                        uid = uid.ifBlank { doc.id },
                        name = name,
                        phone = phone,
                        email = email,
                        selected = currentSelectionByUid[uid]
                            ?: currentSelectionByPhone[phone]
                            ?: true
                    )
                }
                .groupBy { it.uid.ifBlank { it.phone } }
                .values
                .mapNotNull { it.firstOrNull() }
                .distinctBy { it.phone }
                .sortedBy { it.name }
                .toList()
        }

        suspend fun fetchCandidateDocs(): List<DocumentSnapshot> {
            val col = db.collection("users")
            val out = mutableListOf<DocumentSnapshot>()

            for (candidate in branchCandidates) {
                runCatching {
                    out.addAll(
                        col.whereArrayContains("branches", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }

                runCatching {
                    out.addAll(
                        col.whereEqualTo("branchesCsv", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }

                runCatching {
                    out.addAll(
                        col.whereEqualTo("branch", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }

                runCatching {
                    out.addAll(
                        col.whereEqualTo("activeBranch", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }

                runCatching {
                    out.addAll(
                        col.whereEqualTo("active_branch", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }
            }

            val distinct = out.distinctBy { it.id }

            if (distinct.isNotEmpty()) {
                return distinct
            }

            // fallback זהיר: רק אם אין שום תוצאה ישירה.
            // מוגבל ל־3000 כדי לא להעמיס בפרויקטים גדולים.
            val all = mutableListOf<DocumentSnapshot>()
            var last: DocumentSnapshot? = null

            while (true) {
                var q = col
                    .orderBy(FieldPath.documentId())
                    .limit(1000)

                if (last != null) {
                    q = q.startAfter(last!!)
                }

                val snap = q.get().await()
                val page = snap.documents

                if (page.isEmpty()) break

                all.addAll(page)
                last = page.last()

                if (all.size >= 3000) break
            }

            android.util.Log.w(
                "KMI_COACH_BROADCAST",
                "recipient direct queries returned 0 -> fallback all users size=${all.size}"
            )

            return all.distinctBy { it.id }
        }

        try {
            val docs = fetchCandidateDocs()
            val finalRecipients = docsToRecipients(docs)

            recipients = finalRecipients

            android.util.Log.e(
                "KMI_COACH_BROADCAST",
                "recipients loaded count=${finalRecipients.size} region=$regionNorm branch=$branchPrimary group=$groupNorm names=${finalRecipients.map { it.name }}"
            )
        } catch (e: Exception) {
            android.util.Log.e(
                "KMI_COACH_BROADCAST",
                "failed loading recipients region=$regionNorm branch=$branchPrimary group=$groupNorm",
                e
            )

            recipients = emptyList()
        }
    }

    // ✅ מסך אמת: אין DemoPrivacy ואין DemoTrainees.
    // הרשימה המוצגת היא הרשימה האמיתית שנשלפה מ-Firestore.
    val uiRecipients = recipients

    // נמענים שנבחרו (גם טלפונים וגם UIDs)
    val selectedRecipients = recipients.filter { it.selected }
    val selectedNumbers = selectedRecipients.map { it.phone }
    val selectedUids = selectedRecipients.map { it.uid }

    val selectedRecipientSnapshots = selectedRecipients.map { recipient ->
        mapOf(
            "uid" to recipient.uid,
            "name" to recipient.name,
            "phone" to recipient.phone,
            "email" to recipient.email
        )
    }

    val allSelected = recipients.isNotEmpty() && recipients.all { it.selected }

    val sendButtonText = when {
        selectedNumbers.isEmpty() -> coachBroadcastTr(
            isEnglish,
            "בחר מתאמנים לשליחה",
            "Select trainees to send"
        )

        allSelected -> coachBroadcastTr(
            isEnglish,
            "שליחת הודעה לכל המתאמנים",
            "Send message to all trainees"
        )

        selectedNumbers.size == 1 -> coachBroadcastTr(
            isEnglish,
            "שליחת הודעה למתאמן שנבחר",
            "Send message to selected trainee"
        )

        else -> coachBroadcastTr(
            isEnglish,
            "שליחת הודעה ל-${selectedNumbers.size} מתאמנים",
            "Send message to ${selectedNumbers.size} trainees"
        )
    }

    // שמירת השידור עם ה־UIDs שנבחרו ל-Firestore
    fun saveBroadcast() {
        val cleanRegion = region.trim()
        val cleanBranch = branch.trim()
        val cleanMessage = message.trim()

        android.util.Log.e(
            "KMI_COACH_BROADCAST",
            "saveBroadcast clicked selected=${selectedRecipients.size} uids=$selectedUids names=${selectedRecipients.map { it.name }}"
        )

        // ✅ שמירה ודאית מתוך המסך עצמו.
        // כך גם אם הניווט מעביר onPersistBroadcast ריק/ישן, עדיין יווצר מסמך ב-coachBroadcasts.
        persistCoachBroadcast(
            region = cleanRegion,
            branch = cleanBranch,
            message = cleanMessage,
            targetUids = selectedUids,
            targetRecipients = selectedRecipientSnapshots,
            onResult = { ok, error ->
                if (ok) {
                    isSending = false

                    android.util.Log.e(
                        "KMI_COACH_BROADCAST",
                        "saveBroadcast callback SUCCESS selected=${selectedRecipients.size} uids=$selectedUids"
                    )

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            coachBroadcastTr(
                                isEnglish,
                                "ההודעה נשמרה ונשלחה לעיבוד Push עבור ${selectedRecipients.size} נמענים",
                                "The message was saved and sent for Push processing to ${selectedRecipients.size} recipients"
                            )
                        )
                    }
                } else {
                    isSending = false

                    android.util.Log.e(
                        "KMI_COACH_BROADCAST",
                        "saveBroadcast callback FAILED selected=${selectedRecipients.size} uids=$selectedUids",
                        error
                    )

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            coachBroadcastTr(
                                isEnglish,
                                "שמירת ההודעה נכשלה: ${error?.localizedMessage ?: "שגיאה לא ידועה"}",
                                "Saving the message failed: ${error?.localizedMessage ?: "Unknown error"}"
                            )
                        )
                    }
                }
            }
        )
    }

    // 🔵 גרעין – רקע גרדיאנט בסגנון המסכים החדשים
    val gradientBackground = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF020617),
                Color(0xFF0F172A),
                Color(0xFF0EA5E9)
            )
        )
    }

    // 🎨 צבעים אחידים לכל ה־TextField: טקסט לבן, רקע כהה, מסגרת כחולה
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF0F172A),
        unfocusedContainerColor = Color(0xFF0B1220),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color(0xFFF8FAFC),
        focusedLabelColor = Color(0xFFE0F2FE),
        unfocusedLabelColor = Color(0xFFCBD5E1),
        focusedBorderColor = Color(0xFF67E8F9),
        unfocusedBorderColor = Color(0xFF38BDF8),
        cursorColor = Color(0xFF67E8F9)
    )

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = coachBroadcastTr(
                    isEnglish,
                    "שידור הודעה לקבוצה",
                    "Broadcast Message"
                ),
                onHome = onHome,
                onOpenDrawer = { il.kmi.app.ui.DrawerBridge.open() },
                showRoleStatus = false,
                lockSearch = true,
                lockHome = false,
                showBottomActions = true,
                currentLang = if (isEnglish) "en" else "he",
                onToggleLanguage = {
                    val newLang =
                        if (currentLanguage == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    currentLanguage = newLang
                    (contextLang as? Activity)?.recreate()
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0)
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .background(gradientBackground)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                    color = Color.Black.copy(alpha = 0.18f),
                    tonalElevation = 0.dp,
                    border = BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.10f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ===== אזור =====
                        ExposedDropdownMenuBox(
                            expanded = expandedRegion,
                            onExpandedChange = { expandedRegion = !expandedRegion }
                        ) {
                            OutlinedTextField(
                                value = region,
                                onValueChange = {},
                                readOnly = true,
                                label = {
                                    Text(
                                        coachBroadcastTr(isEnglish, "אזור", "Region"),
                                        textAlign = screenTextAlign
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                colors = fieldColors
                            )
                            DropdownMenu(
                                expanded = expandedRegion,
                                onDismissRequest = { expandedRegion = false }
                            ) {
                                branchesByRegion.keys.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r) },
                                        onClick = {
                                            region = r
                                            branch = ""
                                            recipients = emptyList()
                                            expandedRegion = false
                                        }
                                    )
                                }
                            }
                        }

                        // ===== סניף =====
                        if (region.isNotBlank()) {
                            ExposedDropdownMenuBox(
                                expanded = expandedBranch,
                                onExpandedChange = { expandedBranch = !expandedBranch }
                            ) {
                                OutlinedTextField(
                                    value = branch,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = {
                                        Text(
                                            coachBroadcastTr(isEnglish, "סניף", "Branch"),
                                            textAlign = screenTextAlign
                                        )
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    colors = fieldColors
                                )
                                DropdownMenu(
                                    expanded = expandedBranch,
                                    onDismissRequest = { expandedBranch = false }
                                ) {
                                    (branchesByRegion[region] ?: emptyList()).forEach { b ->
                                        DropdownMenuItem(
                                            text = { Text(b) },
                                            onClick = {
                                                branch = b
                                                expandedBranch = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ===== טקסט ההודעה =====
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = {
                                Text(
                                    coachBroadcastTr(isEnglish, "טקסט ההודעה", "Message text"),
                                    textAlign = screenTextAlign
                                )
                            },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors = fieldColors
                        )
                    }
                }

                if (coachGroupKey.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.16f),
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.10f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = coachBroadcastTr(
                                    isEnglish,
                                    "בחירת קהל יעד",
                                    "Target audience"
                                ),
                                color = Color.White,
                                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { sendScope = "group" },
                                    modifier = Modifier.weight(1f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (sendScope == "group") {
                                            Color(0xFF67E8F9)
                                        } else {
                                            Color.White.copy(alpha = 0.18f)
                                        }
                                    ),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (sendScope == "group") {
                                            Color(0xFF0EA5E9).copy(alpha = 0.32f)
                                        } else {
                                            Color(0xFF0B1220).copy(alpha = 0.82f)
                                        },
                                        contentColor = Color(0xFFE0F2FE)
                                    )
                                ) {
                                    Text(
                                        text = coachBroadcastTr(
                                            isEnglish,
                                            "הקבוצה שלי",
                                            "My group"
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                OutlinedButton(
                                    onClick = { sendScope = "branch" },
                                    modifier = Modifier.weight(1f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (sendScope == "branch") {
                                            Color(0xFF67E8F9)
                                        } else {
                                            Color.White.copy(alpha = 0.18f)
                                        }
                                    ),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (sendScope == "branch") {
                                            Color(0xFF0EA5E9).copy(alpha = 0.32f)
                                        } else {
                                            Color(0xFF0B1220).copy(alpha = 0.82f)
                                        },
                                        contentColor = Color(0xFFE0F2FE)
                                    )
                                ) {
                                    Text(
                                        text = coachBroadcastTr(
                                            isEnglish,
                                            "כל הסניף",
                                            "Entire branch"
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Text(
                                text = coachBroadcastTr(
                                    isEnglish,
                                    if (sendScope == "group") {
                                        "ההודעה תישלח רק למתאמני הקבוצה: $coachGroupKey"
                                    } else {
                                        "ההודעה תישלח לכל המתאמנים הפעילים בסניף שנבחר."
                                    },
                                    if (sendScope == "group") {
                                        "The message will be sent only to trainees in: $coachGroupKey"
                                    } else {
                                        "The message will be sent to all active trainees in the selected branch."
                                    }
                                ),
                                color = Color(0xFFE0F2FE),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ===== רשימת נמענים מהקבוצה =====
                if (recipients.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        OutlinedButton(
                            onClick = {
                                val newValue = !allSelected
                                recipients = recipients.map { it.copy(selected = newValue) }
                            },
                            modifier = Modifier.width(200.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF67E8F9)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF0B1220),
                                contentColor = Color(0xFFE0F2FE)
                            )
                        ) {
                            Text(
                                text = if (allSelected) {
                                    coachBroadcastTr(isEnglish, "בטל סימון לכולם", "Unselect all")
                                } else {
                                    coachBroadcastTr(isEnglish, "סמן את כל חברי הקבוצה", "Select all group members")
                                },
                                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.95f))
                            .padding(8.dp)
                    ) {
                        uiRecipients.forEach { r ->
                            val isSelected = r.selected

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFE0F2FE) else Color.Transparent,
                                tonalElevation = 0.dp,
                                border = if (isSelected) {
                                    BorderStroke(
                                        1.dp,
                                        Color(0xFF7DD3FC)
                                    )
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = r.name,
                                            color = if (isSelected) Color(0xFF0C4A6E) else Color.Black
                                        )
                                        Text(
                                            text = r.phone,
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) Color(0xFF0369A1) else Color.Gray
                                        )
                                    }
                                    Checkbox(
                                        checked = r.selected,
                                        onCheckedChange = { checked ->
                                            recipients = recipients.map {
                                                if (it.uid == r.uid) it.copy(selected = checked) else it
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (region.isNotBlank() && branch.isNotBlank()) {
                    Text(
                        text = coachBroadcastTr(
                            isEnglish,
                            if (effectiveGroupKey.isNotBlank()) {
                                "לא נמצאו מתאמנים פעילים לסניף ולקבוצה שנבחרו."
                            } else {
                                "לא נמצאו מתאמנים פעילים לסניף שנבחר."
                            },
                            if (effectiveGroupKey.isNotBlank()) {
                                "No active trainees were found for the selected branch and group."
                            } else {
                                "No active trainees were found for the selected branch."
                            }
                        ),
                        color = Color.White,
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = coachBroadcastTr(
                            isEnglish,
                            if (effectiveGroupKey.isNotBlank()) {
                                "מתאמנים בקבוצה $effectiveGroupKey: ${recipients.size}"
                            } else {
                                "מתאמנים בסניף: ${recipients.size}"
                            },
                            if (effectiveGroupKey.isNotBlank()) {
                                "Trainees in $effectiveGroupKey: ${recipients.size}"
                            } else {
                                "Trainees in branch: ${recipients.size}"
                            }
                        ),
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = coachBroadcastTr(
                            isEnglish,
                            "מתאמנים נבחרים: ${selectedNumbers.size}",
                            "Selected trainees: ${selectedNumbers.size}"
                        ),
                        color = Color(0xFFE0F2FE),
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // שליחה דרך SMS + שמירה + פידבק
                androidx.compose.material3.Button(
                    onClick = {
                        scope.launch {
                            if (isSending) {
                                return@launch
                            }

                            when {
                                message.isBlank() -> {
                                    snackbarHostState.showSnackbar(
                                        coachBroadcastTr(
                                            isEnglish,
                                            "נא לכתוב טקסט להודעה",
                                            "Please write a message"
                                        )
                                    )
                                }

                                selectedNumbers.isEmpty() -> {
                                    snackbarHostState.showSnackbar(
                                        coachBroadcastTr(
                                            isEnglish,
                                            "לא נבחרו נמענים – סמן לפחות מתאמן אחד",
                                            "No recipients selected — select at least one trainee"
                                        )
                                    )
                                }
                                else -> {
                                    isSending = true

                                    android.util.Log.e(
                                        "KMI_COACH_BROADCAST",
                                        "send button confirmed message='${message.trim()}' selectedNumbers=$selectedNumbers selectedUids=$selectedUids recipients=$selectedRecipientSnapshots"
                                    )

                                    val messageToSend = message

                                    saveBroadcast()

                                    runCatching {
                                        onOpenSms(selectedNumbers, messageToSend)
                                    }.onSuccess {
                                        message = ""

                                        snackbarHostState.showSnackbar(
                                            coachBroadcastTr(
                                                isEnglish,
                                                "נפתחה אפליקציית ההודעות עם ${selectedNumbers.size} מתאמנים",
                                                "The messaging app opened with ${selectedNumbers.size} trainees"
                                            )
                                        )
                                    }.onFailure { e ->
                                        isSending = false

                                        android.util.Log.e(
                                            "KMI_COACH_BROADCAST",
                                            "failed opening SMS app",
                                            e
                                        )

                                        snackbarHostState.showSnackbar(
                                            coachBroadcastTr(
                                                isEnglish,
                                                "שמירת ההודעה בוצעה, אבל פתיחת אפליקציית ההודעות נכשלה.",
                                                "The message was saved, but opening the messaging app failed."
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSending && message.isNotBlank() && selectedNumbers.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0EA5E9),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    ),
                    border = BorderStroke(1.dp, Color(0xFF67E8F9))
                ) {
                    if (isSending) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = coachBroadcastTr(
                                    isEnglish,
                                    "שולח הודעה...",
                                    "Sending message..."
                                ),
                                color = Color.White,
                                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = sendButtonText,
                            color = Color(0xFFE0F2FE),
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(84.dp))
                }
            }
        }
    }
}