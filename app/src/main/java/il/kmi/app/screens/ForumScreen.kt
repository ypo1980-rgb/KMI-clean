@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.Explanations
import il.kmi.app.subscription.KmiAccess   // 👈 חדש – בדיקת גישת מנוי/ניסיון
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// הודעה למסך (כולל מידע אם זו הודעה שלי + מדיה)
private data class ForumUiMessage(
    val id: String,
    val branch: String,
    val groupKey: String,
    val authorName: String,
    val authorEmail: String,
    val authorUid: String?,
    val text: String,
    val createdAt: Instant,
    val mediaUrl: String?,
    val mediaType: String?,   // "image" / "video" / null
    val isMine: Boolean
)

// משתתף בפורום – לצורך רשימת המשתתפים
private data class ForumParticipantUi(
    val id: String,
    val name: String,
    val isMe: Boolean
)

@Composable
fun ForumScreen(
    sp: SharedPreferences,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit = { _ -> },
    onOpenSubscription: () -> Unit = {},   // 👈 חדש
    onGoHome: () -> Unit                   // 👈 נשתמש בו באמת
) {
    val ctx = LocalContext.current
    // 🔵 דיאלוג AI פתוח/סגור
    var showAiDialog by rememberSaveable { mutableStateOf(false) }
    val userSp = remember {
        ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    // --- זיהוי מנהל / override ---

    // דגל מנהל כפי שנשמר במסך המנוי (kmi_user.is_manager)
    var isManagerOverride by remember {
        mutableStateOf(userSp.getBoolean("is_manager", false))
    }

    // עדכון חי כשמשתנה SharedPreferences (כניסה/יציאה ממצב מנהל)
    DisposableEffect(userSp) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "is_manager") {
                isManagerOverride = userSp.getBoolean("is_manager", false)
            }
        }
        userSp.registerOnSharedPreferenceChangeListener(l)
        onDispose { userSp.unregisterOnSharedPreferenceChangeListener(l) }
    }

    // --- מצב מנוי ---

    val isTrial = KmiAccess.isTrialActive(userSp)
    val hasFull = KmiAccess.hasFullAccess(userSp)

    // 🔒 גישת אקסטרות לפורום:
    // רק מנוי מלא או מנהל (trial לא פותח פורום)
    val canUseExtras = hasFull || isManagerOverride

    // סניף + קבוצה של המשתמש (הקבוצה = "חדר" הפורום)
    val branch = remember { userSp.getString("branch", "") ?: "" }

    // groupKey – קודם מנסים key אמיתי, אם ריק ניפול ל-age_group / group
    val groupKey = remember {
        val direct = userSp.getString("groupKey", null)
        direct?.takeIf { it.isNotBlank() }
            ?: userSp.getString("age_group", null)?.takeIf { it.isNotBlank() }
            ?: userSp.getString("group", "")!!.ifBlank { "" }
    }

    // 👇 שם המשתמש – מנסה כמה מפתחות מהרישום
    val fullName = remember {
        userSp.getString("fullName", null)
            ?: userSp.getString("name", null)
            ?: userSp.getString("displayName", null)
            ?: ""
    }
    val email = remember { userSp.getString("email", "") ?: "" }

    val db = remember { Firebase.firestore }
    val storage = remember { Firebase.storage }    // 👈 storage זמין
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ForumUiMessage>()) }

    // רשימת משתתפים לפי משתמשים ב־Firestore (בסניף)
    var participantsByUsers by remember { mutableStateOf<List<ForumParticipantUi>>(emptyList()) }

    // הודעה בעריכה (אם יש) + טקסט לעריכה
    var editingMessage by remember { mutableStateOf<ForumUiMessage?>(null) }
    var editText by remember { mutableStateOf("") }

    // מדיה שמצורפת להודעה שנשלחת
    var attachedUri by remember { mutableStateOf<Uri?>(null) }
    var attachedMediaType by remember { mutableStateOf<String?>(null) } // "image"/"video"/null

    // תרגיל שנבחר מהחיפוש (הדיאלוג למטה)
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    // אנונימי אם צריך
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }

    // ================== האזנה בזמן אמת ==================
    LaunchedEffect(branch, groupKey) {
        // חייב לפחות סניף; קבוצה משמשת רק כתווית בחדר, לא לסינון
        if (branch.isBlank()) return@LaunchedEffect

        // 🔹 פורום לפי סניף בלבד – רואים את כל ההודעות של כל הקבוצות באותו סניף
        db.collection("branches")
            .document(branch)
            .collection("messages")
            .addSnapshotListener { snap, _ ->

                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                val uiList = snap?.documents
                    ?.mapNotNull { doc ->
                        val rawTs = doc.getTimestamp("createdAt")
                        val instant = rawTs
                            ?.toDate()
                            ?.toInstant()
                            ?.toKotlinInstant()
                            ?: return@mapNotNull null

                        // 👇 שם השולח – מנסה כמה שדות: authorName / fullName / name / displayName
                        val authorNameDoc =
                            doc.getString("authorName")
                                ?: doc.getString("fullName")
                                ?: doc.getString("name")
                                ?: doc.getString("displayName")
                                ?: ""

                        val authorEmailDoc = doc.getString("authorEmail") ?: ""
                        val authorUidDoc = doc.getString("authorUid")

                        ForumUiMessage(
                            id = doc.id,
                            branch = doc.getString("branch") ?: branch,
                            groupKey = doc.getString("groupKey") ?: groupKey,
                            authorName = authorNameDoc,
                            authorEmail = authorEmailDoc,
                            authorUid = authorUidDoc,
                            text = doc.getString("text") ?: "",
                            createdAt = instant,
                            mediaUrl = doc.getString("mediaUrl"),
                            mediaType = doc.getString("mediaType"),
                            isMine = (authorUidDoc != null && authorUidDoc == currentUid)
                        )
                    }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()

                messages = uiList
            }
    }

    // ================== משתתפים לפי users בסניף ==================
    DisposableEffect(branch) {
        if (branch.isBlank()) {
            participantsByUsers = emptyList()
            onDispose { }
        } else {
            val registration = db.collection("users")
                .whereArrayContains("branches", branch)   // כולם בסניף הזה
                .whereEqualTo("isActive", true)
                .addSnapshotListener { snap, _ ->
                    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                    val list = snap?.documents
                        ?.mapNotNull { doc ->
                            val uid = doc.getString("uid") ?: doc.id
                            val name =
                                doc.getString("fullName")?.takeIf { it.isNotBlank() }
                                    ?: doc.getString("displayName")?.takeIf { it.isNotBlank() }
                                    ?: doc.getString("name")?.takeIf { it.isNotBlank() }
                                    ?: doc.getString("phone")?.takeIf { it.isNotBlank() }
                                    ?: return@mapNotNull null

                            ForumParticipantUi(
                                id = uid,
                                name = name,
                                isMe = (uid == currentUid)
                            )
                        }
                        ?.distinctBy { it.id }
                        ?.sortedBy { it.name }
                        ?: emptyList()

                    participantsByUsers = list
                }

            onDispose {
                registration.remove()
            }
        }
    }

    // ---------- בוררי מדיה ----------
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedUri = uri
            attachedMediaType = "image"
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedUri = uri
            attachedMediaType = "video"
        }
    }

    // ---------- שליחת/עדכון הודעה ----------
    suspend fun sendMessageInternal() {
        try {
            val text = (if (editingMessage != null) editText else input).trim()
            val auth = FirebaseAuth.getInstance()
            val currentUid = auth.currentUser?.uid

            if (text.isEmpty() && attachedUri == null) return
            if (branch.isBlank() || groupKey.isBlank()) return

            // העלאת מדיה (אם יש)
            var mediaUrl: String? = null
            val mediaType = attachedMediaType

            if (attachedUri != null && mediaType != null) {
                val path =
                    "forum_media/$branch/$groupKey/${currentUid ?: "anon"}/${System.currentTimeMillis()}"
                val ref = storage.reference.child(path)
                ref.putFile(attachedUri!!).await()
                mediaUrl = ref.downloadUrl.await().toString()
            }

            // דאטה בסיסי להודעה
            val baseData = mutableMapOf<String, Any?>(
                "branch" to branch,
                "groupKey" to groupKey,
                "authorName" to fullName,
                "authorEmail" to email,
                "authorUid" to currentUid,
                "text" to text,
            )

            if (mediaUrl != null && mediaType != null) {
                baseData["mediaUrl"] = mediaUrl
                baseData["mediaType"] = mediaType
            }

            if (editingMessage == null) {
                // הודעה חדשה
                baseData["createdAt"] = FieldValue.serverTimestamp()
                db.collection("branches")
                    .document(branch)
                    .collection("messages")
                    .add(baseData.filterValues { it != null })
                    .await()
            } else {
                // עדכון הודעה קיימת
                baseData["updatedAt"] = FieldValue.serverTimestamp()
                db.collection("branches")
                    .document(branch)
                    .collection("messages")
                    .document(editingMessage!!.id)
                    .set(
                        baseData.filterValues { it != null },
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()
            }

            // ניקוי מצב אחרי שליחה / עדכון
            input = ""
            editText = ""
            editingMessage = null
            attachedUri = null
            attachedMediaType = null

        } catch (e: Exception) {
            android.util.Log.e("KMI_FORUM", "sendMessageInternal failed", e)
            Toast.makeText(
                ctx,
                "שגיאה בשמירת ההודעה: ${e.localizedMessage ?: "בדוק חיבור לאינטרנט"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun formatInstant(instant: Instant): String {
        val df = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val date = Date(instant.toEpochMilliseconds())
        return df.format(date)
    }

    val gradientBackground = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF020617),
                Color(0xFF0F172A),
                Color(0xFF0EA5E9)
            )
        )
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "פורום הסניף",
                onPickSearchResult = { key -> pickedKey = key },
                onHome = onGoHome,          // 👈 כאן התיקון – הבית באמת הולך הביתה
                onSearch = { },
                showTopHome = false,
                showTopSearch = false,
                lockSearch = false,
                showBottomActions = true,
                onOpenAi = { showAiDialog = true }
            )
        },
        containerColor = Color.Transparent,
        // ⬅️ רק סטטוס־בר וצדדים; בלי מרווח בתחתית מה-Scaffold
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Start + WindowInsetsSides.End
        )
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(padding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {

                // 🔒 קודם כל – נעילת מסך הפורום לפי מנוי / ניסיון
                if (!canUseExtras) {
                    val lockText = when {
                        isTrial && !hasFull ->
                            "במהלך תקופת הניסיון מסך הפורום נעול.\nאחרי רכישת מנוי המסך ייפתח עבורך."
                        !isTrial && !hasFull ->
                            "מסך הפורום זמין למנויים בלבד.\nכדי להמשיך יש לרכוש מנוי פעיל."
                        else ->
                            "מסך הפורום זמין למנויים בלבד."
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        color = Color(0xFF020617).copy(alpha = 0.92f),
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔒 גישה לפורום",
                                color = Color(0xFFBFDBFE),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = lockText,
                                color = Color(0xFFE5E7EB),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onOpenSubscription,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "עבור למסך המנוי",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "ניתן לחזור תמיד למסך זה לאחר רכישת מנוי.",
                                color = Color(0xFF9CA3AF),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // לא מציירים שורת כתיבה כשאין גישה
                    return@Column
                }

                // רק אם יש גישה – בודקים שהמשתמש משויך לסניף/קבוצה
                if (branch.isBlank() || groupKey.isBlank()) {
                    Text(
                        "לא אותרו סניף/קבוצה במשתמש.\nודאו ש־\"branch\" ו־\"groupKey\" מוגדרים בפרופיל.",
                        color = Color(0xFFFF6B6B),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    return@Column
                }

                // כותרת בולטת לסניף / קבוצה
                val roomLabel = "סניף: $branch  •  קבוצה: $groupKey"

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    color = Color(0xFF020617).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.75f))
                ) {
                    Text(
                        text = roomLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFECFEFF),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // ===== רשימת משתתפים בפורום =====
                // קודם כל – לפי users בסניף; אם אין, נופל לשמות מתוך ההודעות
                val participants = if (participantsByUsers.isNotEmpty()) {
                    participantsByUsers
                } else {
                    messages
                        .groupBy { it.authorUid ?: it.authorEmail.ifBlank { it.authorName } }
                        .mapNotNull { (_, msgs) ->
                            val sample = msgs.firstOrNull() ?: return@mapNotNull null
                            val displayName =
                                sample.authorName.ifBlank { sample.authorEmail }.ifBlank { "משתתף" }
                            val id = sample.authorUid
                                ?: sample.authorEmail.ifBlank { sample.authorName.ifBlank { displayName } }

                            ForumParticipantUi(
                                id = id,
                                name = displayName,
                                isMe = sample.isMine
                            )
                        }
                        .distinctBy { it.id }
                        .sortedBy { it.name }
                }

                var showParticipantsDialog by remember { mutableStateOf(false) }

                if (participants.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { showParticipantsDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        color = Color(0xFF020617).copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Text(
                            text = "משתתפים בפורום (${participants.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFECFEFF),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }
                }

                if (showParticipantsDialog) {
                    AlertDialog(
                        onDismissRequest = { showParticipantsDialog = false },
                        title = {
                            Text(
                                text = "משתתפים בפורום (${participants.size})",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp)
                            ) {
                                items(participants, key = { it.id }) { p ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (p.isMe) "${p.name} (אני)" else p.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showParticipantsDialog = false }) {
                                Text("סגור")
                            }
                        }
                    )
                }

                // ================= רשימת הודעות =================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        val bubbleColor =
                            if (msg.isMine) Color(0xFF0EA5E9) else Color(0xFF020617).copy(alpha = 0.92f)
                        val textColor =
                            if (msg.isMine) Color.White else Color(0xFFE5E7EB)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = bubbleColor,
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (msg.isMine) 18.dp else 4.dp,
                                    bottomEnd = if (msg.isMine) 4.dp else 18.dp
                                ),
                                tonalElevation = 3.dp,
                                shadowElevation = 3.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 290.dp)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = msg.authorName.ifBlank { msg.authorEmail },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = textColor.copy(alpha = 0.9f),
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Right
                                            )
                                            Text(
                                                text = formatInstant(msg.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Right
                                            )
                                        }

                                        if (msg.isMine) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        editingMessage = msg
                                                        editText = msg.text
                                                        input = ""
                                                        attachedUri = null
                                                        attachedMediaType = null
                                                    },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Edit,
                                                        contentDescription = "עריכת הודעה",
                                                        tint = textColor,
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        // מחיקה
                                                        scope.launch {
                                                            db.collection("branches")
                                                                .document(msg.branch)
                                                                .collection("messages")
                                                                .document(msg.id)
                                                                .delete()
                                                                .await()
                                                        }
                                                    },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = "מחיקת הודעה",
                                                        tint = textColor.copy(alpha = 0.9f),
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (msg.text.isNotBlank()) {
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textColor,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // מדיה – אם יש
                                    msg.mediaUrl?.let { url ->
                                        Spacer(Modifier.height(6.dp))
                                        when (msg.mediaType) {
                                            "image" -> {
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = Color.Black.copy(alpha = 0.15f)
                                                ) {
                                                    AsyncImage(
                                                        model = url,
                                                        contentDescription = "תמונה מצורפת",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(min = 120.dp, max = 220.dp)
                                                    )
                                                }
                                            }

                                            "video" -> {
                                                val context = LocalContext.current
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = Color.Black.copy(alpha = 0.3f),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(120.dp)
                                                        .padding(top = 2.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                            horizontalAlignment = Alignment.End
                                                        ) {
                                                            Text(
                                                                "סרטון מצורף",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            Text(
                                                                "לחיצה לפתיחה בנגן",
                                                                color = Color.White.copy(alpha = 0.8f),
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                        FilledTonalButton(
                                                            onClick = {
                                                                val intent = Intent(
                                                                    Intent.ACTION_VIEW,
                                                                    Uri.parse(url)
                                                                ).apply {
                                                                    setDataAndType(
                                                                        Uri.parse(url),
                                                                        "video/*"
                                                                    )
                                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                }
                                                                context.startActivity(intent)
                                                            }
                                                        ) {
                                                            Icon(
                                                                Icons.Filled.VideoLibrary,
                                                                contentDescription = null
                                                            )
                                                            Spacer(Modifier.width(6.dp))
                                                            Text("פתח")
                                                        }
                                                    }
                                                }
                                            }

                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // צ'יפ למדיה מצורפת (אם יש)
                if (attachedUri != null && attachedMediaType != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0EA5E9).copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when (attachedMediaType) {
                                    "image" -> "תמונה מצורפת לשליחה"
                                    "video" -> "סרטון מצורף לשליחה"
                                    else -> "קובץ מצורף"
                                },
                                color = Color(0xFFBAE6FD),
                                style = MaterialTheme.typography.labelMedium
                            )
                            TextButton(onClick = {
                                attachedUri = null
                                attachedMediaType = null
                            }) {
                                Text("הסר")
                            }
                        }
                    }
                }

                // ================= שורת שליחה =================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        // ⬅️ גם ניווט כשאין מקלדת, וגם IME כשיש מקלדת
                        .windowInsetsPadding(
                            WindowInsets.navigationBars
                                .only(WindowInsetsSides.Bottom)
                                .union(
                                    WindowInsets.ime.only(WindowInsetsSides.Bottom)
                                )
                        )
                ) {
                    OutlinedTextField(
                        value = if (editingMessage != null) editText else input,
                        onValueChange = {
                            if (editingMessage != null) editText = it else input = it
                        },
                        modifier = Modifier
                            .weight(1f),
                        placeholder = {
                            Text(
                                if (editingMessage != null) "עריכת הודעה..." else "כתוב הודעה...",
                                textAlign = TextAlign.Right
                            )
                        },
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF020617),
                            unfocusedContainerColor = Color(0xFF020617),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF1E293B)
                        )
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFF0369A1)
                                )
                            ) {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = "צרף תמונה",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { videoPicker.launch("video/*") },
                                modifier = Modifier.size(32.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFF0F766E)
                                )
                            ) {
                                Icon(
                                    Icons.Filled.VideoLibrary,
                                    contentDescription = "צרף וידאו",
                                    tint = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch { sendMessageInternal() }
                            },
                            enabled = (if (editingMessage != null) editText else input).trim()
                                .isNotEmpty() || attachedUri != null,
                            modifier = Modifier
                                .size(width = 72.dp, height = 40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = if (editingMessage != null) "עדכן הודעה" else "שלח",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // ===== דיאלוג תרגיל מהחיפוש =====
            pickedKey?.let { key ->
                val (belt, topic, item) = parseSearchKey(key)

                val displayName = ExerciseTitleFormatter
                    .displayName(item)
                    .ifBlank { item }

                val explanation = remember(belt, item) {
                    findExplanationForHit(
                        belt = belt,
                        rawItem = item,
                        topic = topic
                    )
                }

                var isFav by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { pickedKey = null },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "(${belt.heb})",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            IconButton(
                                onClick = { isFav = !isFav },
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                if (isFav) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "מועדף",
                                        tint = Color(0xFFFFC107)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.StarBorder,
                                        contentDescription = "הוסף למועדפים",
                                    )
                                }
                            }
                        }
                    },
                    text = {
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { pickedKey = null }) {
                            Text("סגור")
                        }
                    }
                )
            }
        }
    }
}

/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String
): String {
    val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

    fun String.clean(): String = this
        .replace('–', '-')    // en dash
        .replace('־', '-')    // maqaf
        .replace("  ", " ")
        .trim()

    val candidates = buildList {
        add(rawItem)
        add(display)
        add(display.clean())
        add(display.substringBefore("(").trim().clean())
    }.distinct()

    for (candidate in candidates) {
        val got = Explanations.get(belt, candidate).trim()
        if (got.isNotBlank()
            && !got.startsWith("הסבר מפורט על")
            && !got.startsWith("אין כרגע")
        ) {
            return if ("::" in got) got.substringAfter("::").trim() else got
        }
    }

    return "אין כרגע הסבר לתרגיל הזה."
}


/* ========= עזר: ראשי תיבות לשם ========= */
private fun String.toInitials(): String {
    return this
        .split(" ", "‏", "-", "–")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifBlank { "?" }
}




