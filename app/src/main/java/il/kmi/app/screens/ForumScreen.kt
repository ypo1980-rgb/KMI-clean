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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.sp

//==================================================================

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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val systemDark = isSystemInDarkTheme()

    // 🔵 דיאלוג AI פתוח/סגור
    var showAiDialog by rememberSaveable { mutableStateOf(false) }
    val userSp = remember {
        ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    // ✅ מצב כהה/בהיר לפי הגדרת האפליקציה, לא רק לפי מצב המכשיר
    val appThemeMode = remember {
        sp.getString("theme_mode", null)
            ?: userSp.getString("theme_mode", null)
            ?: ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE)
                .getString("theme_mode", "system")
            ?: "system"
    }

    val isDarkMode = when (appThemeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val isCoachProfile = remember {
        val role = userSp.getString("user_role", "trainee").orEmpty().lowercase()
        role.contains("coach") || role.contains("מאמן")
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

    // 🔒 גישת פורום:
    // רק מנוי חודשי / שנתי פעיל או מנהל.
    // Trial לא פותח את הפורום.
    val canUseExtras = isManagerOverride || hasFull

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

    val listState = rememberLazyListState()

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

                scope.launch {
                    listState.animateScrollToItem(0)
                }
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

            // ✅ אחרי שליחה סוגרים מקלדת ומחזירים שדה ראייה למסך
            focusManager.clearFocus(force = true)
            keyboardController?.hide()

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

    val gradientBackground = remember(isDarkMode, isCoachProfile) {
        if (isDarkMode) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF0B141A),
                    Color(0xFF0F1B22),
                    Color(0xFF111B21)
                )
            )
        } else {
            if (isCoachProfile) {
                // ✅ רקע בהיר בסגנון מאמן — סגול/כחול אלגנטי
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF8F5FF),
                        Color(0xFFF0E9FF),
                        Color(0xFFEAF6FF)
                    )
                )
            } else {
                // ✅ רקע בהיר בסגנון מתאמן — כחול/טורקיז עדין
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF6FBFF),
                        Color(0xFFEAF7FF),
                        Color(0xFFEAFBF6)
                    )
                )
            }
        }
    }

    val forumHeaderColor = if (isDarkMode) Color(0xFF182229) else Color.White.copy(alpha = 0.94f)
    val forumHeaderBorder = if (isDarkMode) Color(0xFF22303A) else Color(0xFFD6E4F4)
    val forumHeaderText = if (isDarkMode) Color(0xFFD1D7DB) else Color(0xFF1F2937)

    val participantsColor = if (isDarkMode) Color(0xFF111B21) else Color.White.copy(alpha = 0.92f)
    val participantsText = if (isDarkMode) Color(0xFFBFC8CD) else Color(0xFF334155)

    val myBubbleColor = if (isDarkMode) Color(0xFF144D37) else Color(0xFFDDFBEA)
    val otherBubbleColor = if (isDarkMode) Color(0xFF202C33) else Color.White.copy(alpha = 0.96f)
    val myBubbleText = if (isDarkMode) Color(0xFFF7F8F8) else Color(0xFF064E3B)
    val otherBubbleText = if (isDarkMode) Color(0xFFE9EDEF) else Color(0xFF111827)

    val inputSurfaceColor = if (isDarkMode) Color(0xFF202C33) else Color.White
    val inputTextColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val inputPlaceholderColor = if (isDarkMode) Color(0xFF8696A0) else Color(0xFF64748B)
    val inputIconTint = if (isDarkMode) Color(0xFF8696A0) else Color(0xFF64748B)

    val attachmentChipColor = if (isDarkMode) Color(0xFF1B2A33) else Color.White.copy(alpha = 0.94f)
    val attachmentChipText = if (isDarkMode) Color(0xFFD8E1E6) else Color(0xFF334155)

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
                        color = if (isDarkMode) {
                            Color(0xFF020617).copy(alpha = 0.92f)
                        } else {
                            Color.White.copy(alpha = 0.94f)
                        },
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        border = BorderStroke(
                            1.dp,
                            if (isDarkMode) Color.Transparent else Color(0xFFD7E3F4)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White.copy(alpha = if (isDarkMode) 0.08f else 0.85f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFD7E3F4)
                                ),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = if (isDarkMode) Color(0xFFBFDBFE) else Color(0xFF2563EB),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "גישה לפורום",
                                color = if (isDarkMode) Color(0xFFBFDBFE) else Color(0xFF1E3A8A),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = lockText,
                                color = if (isDarkMode) Color(0xFFE5E7EB) else Color(0xFF334155),
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
                                color = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
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
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = if (isDarkMode) 0.dp else 3.dp,
                    color = forumHeaderColor,
                    border = BorderStroke(1.dp, forumHeaderBorder)
                ) {
                    Text(
                        text = roomLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = forumHeaderText,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp)
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
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = if (isDarkMode) 0.dp else 3.dp,
                        color = participantsColor,
                        border = BorderStroke(1.dp, forumHeaderBorder)
                    ) {
                        Text(
                            text = "משתתפים בפורום (${participants.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = participantsText,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp, horizontal = 12.dp)
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
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        val bubbleColor =
                            if (msg.isMine) myBubbleColor else otherBubbleColor
                        val textColor =
                            if (msg.isMine) myBubbleText else otherBubbleText

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Box {
                                Surface(
                                    color = bubbleColor,
                                    shape = RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (msg.isMine) 18.dp else 6.dp,
                                        bottomEnd = if (msg.isMine) 6.dp else 18.dp
                                    ),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .widthIn(max = 240.dp)
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (msg.text.isNotBlank()) {
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = textColor,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        msg.mediaUrl?.let { url ->
                                            Spacer(Modifier.height(4.dp))
                                            when (msg.mediaType) {
                                                "image" -> {
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color.Black.copy(alpha = 0.16f)
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
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color.Black.copy(alpha = 0.28f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(108.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                                                    color = Color.White.copy(alpha = 0.78f),
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

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (msg.isMine) {
                                                IconButton(
                                                    onClick = {
                                                        editingMessage = msg
                                                        editText = msg.text
                                                        input = ""
                                                        attachedUri = null
                                                        attachedMediaType = null
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Edit,
                                                        contentDescription = "עריכת הודעה",
                                                        tint = textColor.copy(alpha = 0.72f)
                                                    )
                                                }
                                                Spacer(Modifier.width(2.dp))
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            db.collection("branches")
                                                                .document(msg.branch)
                                                                .collection("messages")
                                                                .document(msg.id)
                                                                .delete()
                                                                .await()
                                                        }
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = "מחיקת הודעה",
                                                        tint = textColor.copy(alpha = 0.72f)
                                                    )
                                                }
                                                Spacer(Modifier.width(4.dp))
                                            }

                                            Text(
                                                text = formatInstant(msg.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.62f),
                                                textAlign = TextAlign.Right
                                            )
                                        }
                                    }
                                }

                                Canvas(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(
                                            if (msg.isMine) Alignment.BottomEnd else Alignment.BottomStart
                                        )
                                ) {
                                    val path = Path()

                                    if (msg.isMine) {
                                        path.moveTo(size.width, size.height)
                                        path.lineTo(0f, size.height)
                                        path.lineTo(size.width, 0f)
                                    } else {
                                        path.moveTo(0f, size.height)
                                        path.lineTo(size.width, size.height)
                                        path.lineTo(0f, 0f)
                                    }

                                    drawPath(
                                        path = path,
                                        color = bubbleColor,
                                        style = Fill
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // צ'יפ למדיה מצורפת (אם יש)
                if (attachedUri != null && attachedMediaType != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = attachmentChipColor,
                        border = BorderStroke(1.dp, forumHeaderBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when (attachedMediaType) {
                                    "image" -> "תמונה מצורפת לשליחה"
                                    "video" -> "סרטון מצורף לשליחה"
                                    else -> "קובץ מצורף"
                                },
                                color = attachmentChipText,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(top = 4.dp, bottom = 4.dp)
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = inputSurfaceColor,
                        tonalElevation = 0.dp,
                        shadowElevation = if (isDarkMode) 1.dp else 5.dp,
                        border = BorderStroke(
                            1.dp,
                            if (isDarkMode) Color.Transparent else Color(0xFFD6E4F4)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = "צרף תמונה",
                                    tint = inputIconTint
                                )
                            }

                            BasicTextField(
                                value = if (editingMessage != null) editText else input,
                                onValueChange = {
                                    if (editingMessage != null) {
                                        editText = it
                                    } else {
                                        input = it
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = inputTextColor,
                                    textAlign = TextAlign.Right,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 22.sp
                                ),
                                cursorBrush = SolidColor(Color(0xFF25D366)),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        val currentText =
                                            if (editingMessage != null) editText else input

                                        if (currentText.isBlank()) {
                                            Text(
                                                text = if (editingMessage != null) "עריכת הודעה..." else "הודעה",
                                                color = inputPlaceholderColor,
                                                textAlign = TextAlign.Right,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    lineHeight = 22.sp
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                }
                            )

                            IconButton(
                                onClick = { videoPicker.launch("video/*") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.VideoLibrary,
                                    contentDescription = "צרף וידאו",
                                    tint = inputIconTint
                                )
                            }
                        }
                    }

                    val canSend = (if (editingMessage != null) editText else input).trim().isNotEmpty() || attachedUri != null

                    Surface(
                        onClick = {
                            if (canSend) {
                                scope.launch { sendMessageInternal() }
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = if (canSend) Color(0xFF25D366) else Color(0xFF1F3C33),
                        modifier = Modifier.size(48.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (canSend) Icons.Filled.Send else Icons.Outlined.Mic,
                                contentDescription = if (canSend) {
                                    if (editingMessage != null) "עדכן הודעה" else "שלח"
                                } else {
                                    "הקלטה"
                                },
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




