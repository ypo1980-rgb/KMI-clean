package il.kmi.app.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

fun persistCoachBroadcast(
    region: String,
    branch: String,
    message: String,
    targetUids: List<String>,
    onResult: (Boolean, Throwable?) -> Unit = { _, _ -> }
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val currentUid = auth.currentUser?.uid
    if (currentUid == null) {
        onResult(false, IllegalStateException("No logged-in user"))
        return
    }

    // 👇 נסה להביא שם מאמן אם שמור בפרופיל (אם אין — נשאיר null)
    val coachName = auth.currentUser?.displayName

    val data = hashMapOf(
        "authorUid" to currentUid,
        "region" to region,
        "branch" to branch,

        // ✅ תאימות: HomeScreen קורא "text"
        "text" to message,

        // ✅ נשאיר גם "message" אם כבר יש לוגיקה אחרת שמסתמכת עליו
        "message" to message,

        // ✅ כדי שיוצג "ממי"
        "coachName" to coachName,

        "targetUids" to targetUids,
        "createdAt" to FieldValue.serverTimestamp()
    )

    db.collection("coachBroadcasts")
        .add(data)
        .addOnSuccessListener { onResult(true, null) }
        .addOnFailureListener { e -> onResult(false, e) }
}

// ייצוג נמען אחד ברשימת הקבוצה
private data class CoachRecipient(
    val uid: String,
    val name: String,
    val phone: String,
    val selected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachBroadcastScreen(
    branchesByRegion: Map<String, List<String>>,
    defaultRegion: String?,
    defaultBranch: String?,
    onBack: () -> Unit,

    // ✅ פעולות פלטפורמה (Android/iOS/desktop)
    onOpenSms: (numbers: List<String>, message: String) -> Unit = { _, _ -> },
    onShareText: (message: String) -> Unit = {},
    // 👈 עכשיו גם מקבל רשימת UIDs של נמענים שנבחרו
    onPersistBroadcast: (region: String, branch: String, message: String, targetUids: List<String>) -> Unit =
        { _, _, _, _ -> }
) {
    var region by remember { mutableStateOf(defaultRegion.orEmpty()) }
    var branch by remember { mutableStateOf(defaultBranch.orEmpty()) }
    var message by remember { mutableStateOf("") }

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

    // ===== טעינת חברי הקבוצה מה- Firestore לפי אזור+סניף (עם תמיכה ב-CSV + dash/en-dash) =====
    LaunchedEffect(region, branch) {
        // לנקות Listener קודם
        usersListener?.remove()
        usersListener = null

        fun String.norm(): String = this
            .trim()
            .replace('־', '-')   // maqaf
            .replace(Regex("\\s+"), " ")

        fun String.pickPrimaryBranch(): String = this
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: this.trim()

        val regionNorm = region.norm()
        val branchPrimary = branch.norm().pickPrimaryBranch()

        if (regionNorm.isBlank() || branchPrimary.isBlank()) {
            recipients = emptyList()
            return@LaunchedEffect
        }

        // וריאציות סניף: '-' / '–'
        val branchCandidates = listOf(
            branchPrimary,
            branchPrimary.replace("-", "–"),
            branchPrimary.replace("–", "-")
        ).map { it.trim() }.distinct()

        // כדי לשמור checkbox גם אחרי Snapshot updates
        val prevSelectionByPhone = recipients.associate { it.phone to it.selected }

        // פונקציה שממירה snapshot לרשימת recipients ושומרת selected לפי הבחירה *הנוכחית* של המשתמש
        fun applySnap(snap: com.google.firebase.firestore.QuerySnapshot?) {

            // ✅ לוקחים את הבחירות האחרונות בזמן אמת (לא פעם אחת בתחילת LaunchedEffect)
            val currentSelectionByPhone = recipients.associate { it.phone to it.selected }

            val list = snap?.documents
                ?.mapNotNull { doc ->
                    val phone = doc.getString("phone")?.trim().orEmpty()
                    if (phone.isEmpty()) return@mapNotNull null

                    val name = doc.getString("fullName")?.takeIf { it.isNotBlank() }
                        ?: doc.getString("name")?.takeIf { it.isNotBlank() }
                        ?: doc.getString("displayName")?.takeIf { it.isNotBlank() }
                        ?: phone

                    val uid = doc.getString("uid") ?: doc.id

                    CoachRecipient(
                        uid = uid,
                        name = name,
                        phone = phone,
                        // ✅ שומר את הבחירה הנוכחית אם קיימת, אחרת true
                        selected = currentSelectionByPhone[phone] ?: true
                    )
                }
                ?.distinctBy { it.phone }
                ?.sortedBy { it.name }
                ?: emptyList()

            recipients = list
        }

        // בניית Query לפי ניסיון עד שמוצאים התאמה
        suspend fun findBestQuery(): com.google.firebase.firestore.Query? {
            // A) region + branches[] + isActive
            for (cand in branchCandidates) {
                val q = db.collection("users")
                    .whereEqualTo("region", regionNorm)
                    .whereArrayContains("branches", cand)
                    .whereEqualTo("isActive", true)

                val test = runCatching { q.limit(1).get().await() }.getOrNull()
                if (test != null && !test.isEmpty) return q
            }

            // B) region + branchesCsv + isActive
            for (cand in branchCandidates) {
                val q = db.collection("users")
                    .whereEqualTo("region", regionNorm)
                    .whereEqualTo("branchesCsv", cand)
                    .whereEqualTo("isActive", true)

                val test = runCatching { q.limit(1).get().await() }.getOrNull()
                if (test != null && !test.isEmpty) return q
            }

            // C) region + branch (שדה יחיד) + isActive
            for (cand in branchCandidates) {
                val q = db.collection("users")
                    .whereEqualTo("region", regionNorm)
                    .whereEqualTo("branch", cand)
                    .whereEqualTo("isActive", true)

                val test = runCatching { q.limit(1).get().await() }.getOrNull()
                if (test != null && !test.isEmpty) return q
            }

            // אם לא מצאנו כלום – נחזיר query בסיסי (כדי להראות "אין מתאמנים")
            return db.collection("users")
                .whereEqualTo("region", regionNorm)
                .whereArrayContains("branches", branchCandidates.first())
                .whereEqualTo("isActive", true)
        }

        val q = findBestQuery()
        usersListener = q?.addSnapshotListener { snap, _ ->
            applySnap(snap)
        }
    }

    // נמענים שנבחרו (גם טלפונים וגם UIDs)
    val selectedRecipients = recipients.filter { it.selected }
    val selectedNumbers = selectedRecipients.map { it.phone }
    val selectedUids = selectedRecipients.map { it.uid }

    val allSelected = recipients.isNotEmpty() && recipients.all { it.selected }

    // שמירת השידור עם ה־UIDs שנבחרו (ל־Firestore / Cloud Function)
    fun saveBroadcast() {
        onPersistBroadcast(
            region.trim(),
            branch.trim(),
            message.trim(),
            selectedUids
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
        focusedContainerColor = Color(0xFF020617),
        unfocusedContainerColor = Color(0xFF020617),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedLabelColor = Color(0xFFBAE6FD),
        unfocusedLabelColor = Color(0xFF94A3B8),
        focusedBorderColor = Color(0xFF38BDF8),
        unfocusedBorderColor = Color(0xFF1E293B),
        cursorColor = Color(0xFF38BDF8)
    )

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "שליחת הודעה לקבוצה",
                onOpenDrawer = { il.kmi.app.ui.DrawerBridge.open() },
                showRoleStatus = false,
                lockSearch = true,
                showBottomActions = true
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
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

                // ===== אזור =====
                ExposedDropdownMenuBox(
                    expanded = expandedRegion,
                    onExpandedChange = { expandedRegion = !expandedRegion }
                ) {
                    OutlinedTextField(
                        value = region,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("אזור") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
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
                            label = { Text("סניף") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
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
                    label = { Text("טקסט ההודעה") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )

                // ===== רשימת נמענים מהקבוצה =====
                if (recipients.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("נמענים בקבוצה: ${recipients.size}")
                        OutlinedButton(
                            onClick = {
                                val newValue = !allSelected
                                recipients = recipients.map { it.copy(selected = newValue) }
                            }
                        ) {
                            Text(if (allSelected) "בטל סימון לכולם" else "סמן את כל חברי הקבוצה")
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.95f))
                            .padding(8.dp)
                    ) {
                        recipients.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(r.name)
                                    Text(
                                        text = r.phone,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Checkbox(
                                    checked = r.selected,
                                    onCheckedChange = { checked ->
                                        recipients = recipients.map {
                                            if (it.phone == r.phone) it.copy(selected = checked) else it
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else if (region.isNotBlank() && branch.isNotBlank()) {
                    Text("לא נמצאו מתאמנים פעילים לסניף שנבחר.")
                }

                Spacer(Modifier.height(4.dp))

                Text("נמענים נבחרים: ${selectedNumbers.size}")

                // שליחה דרך SMS + שמירה + פידבק
                androidx.compose.material3.Button(
                    onClick = {
                        scope.launch {
                            when {
                                message.isBlank() -> {
                                    snackbarHostState.showSnackbar("נא לכתוב טקסט להודעה")
                                }
                                selectedNumbers.isEmpty() -> {
                                    snackbarHostState.showSnackbar("לא נבחרו נמענים – סמן לפחות נמען אחד")
                                }
                                else -> {
                                    // נשמור לשימוש Cloud Function (כולל ה-UIDs שנבחרו)
                                    saveBroadcast()
                                    // פתיחת אפליקציית SMS עם הנמענים המסומנים
                                    onOpenSms(selectedNumbers, message)
                                    snackbarHostState.showSnackbar(
                                        "נפתחה אפליקציית ההודעות עם ${selectedNumbers.size} נמענים"
                                    )
                                }
                            }
                        }
                    },
                    enabled = message.isNotBlank() && selectedNumbers.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0EA5E9),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0xFF64748B)
                    )
                ) {
                    Text("שליחת הודעה לכל הנמענים המסומנים")
                }

                // שיתוף כללי + שמירה
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        if (message.isBlank()) return@OutlinedButton
                        saveBroadcast()
                        onShareText(message)
                        scope.launch {
                            snackbarHostState.showSnackbar("נוסח ההודעה שותף לאפליקציה שבחרת")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFBAE6FD)
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFFECFEFF),
                        disabledContentColor = Color(0xFF64748B)
                    )
                ) {
                    Text("שיתוף נוסח ההודעה (אפליקציות אחרות)")
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
