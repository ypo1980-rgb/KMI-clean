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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.Explanations
import il.kmi.app.subscription.KmiAccess   // 👈 חדש – בדיקת גישת מנוי/ניסיון
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.app.localization.rememberIsEnglish
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
    val messageId: String,
    val branch: String,
    val groupKey: String,
    val authorName: String,
    val authorEmail: String,
    val authorUid: String?,
    val text: String,
    val createdAt: Instant,
    val createdAtMillis: Long,
    val updatedAtMillis: Long?,
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

private fun forumTr(isEnglish: Boolean, he: String, en: String): String =
    if (isEnglish) en else he

private fun forumTextAlign(isEnglish: Boolean): TextAlign =
    if (isEnglish) TextAlign.Left else TextAlign.Right

private const val FORUM_MESSAGE_RETENTION_DAYS = 90L
private const val FORUM_MESSAGE_RETENTION_MILLIS =
    FORUM_MESSAGE_RETENTION_DAYS * 24L * 60L * 60L * 1000L

private fun forumLastReadKey(branch: String, groupKey: String): String =
    "forum_last_read_at_${branch.trim()}_${groupKey.trim()}"

private fun forumSafeDocId(raw: String): String {
    return raw
        .trim()
        .lowercase()
        .replace('־', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replace(Regex("\\s+"), "_")
        .replace(Regex("[^a-z0-9א-ת_\\-]+"), "_")
        .trim('_')
        .ifBlank { "default" }
}

private fun forumRoomDocId(branch: String, groupKey: String): String {
    return "room_${forumSafeDocId(branch)}_${forumSafeDocId(groupKey)}"
}

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

    val isEnglish = rememberIsEnglish()
    val screenTextAlign = forumTextAlign(isEnglish)

    val systemDark = isSystemInDarkTheme()

    // 🔵 דיאלוג AI פתוח/סגור
    var showAiDialog by rememberSaveable { mutableStateOf(false) }
    val userSp = remember {
        ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    val settingsSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE)
    }

    var themeRefreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(sp, userSp, settingsSp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "theme_mode" ||
                key == "themeMode" ||
                key == "app_theme" ||
                key == "appTheme" ||
                key == "dark_mode" ||
                key == "darkMode" ||
                key == "is_dark_mode" ||
                key == "isDarkMode"
            ) {
                themeRefreshTick++
            }
        }

        sp.registerOnSharedPreferenceChangeListener(listener)
        userSp.registerOnSharedPreferenceChangeListener(listener)
        settingsSp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            sp.unregisterOnSharedPreferenceChangeListener(listener)
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
            settingsSp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val appThemeMode = remember(themeRefreshTick) {
        sp.getString("theme_mode", null)
            ?: sp.getString("themeMode", null)
            ?: sp.getString("app_theme", null)
            ?: sp.getString("appTheme", null)
            ?: userSp.getString("theme_mode", null)
            ?: userSp.getString("themeMode", null)
            ?: settingsSp.getString("theme_mode", null)
            ?: settingsSp.getString("themeMode", null)
            ?: settingsSp.getString("app_theme", null)
            ?: settingsSp.getString("appTheme", null)
            ?: "light"
    }

    val isDarkMode = remember(appThemeMode, themeRefreshTick, systemDark) {
        when (appThemeMode.lowercase()) {
            "dark", "night", "כהה" -> true
            "light", "day", "בהיר" -> false
            "system" -> systemDark
            else -> {
                settingsSp.getBoolean("isDarkMode", false) ||
                        settingsSp.getBoolean("darkMode", false) ||
                        sp.getBoolean("isDarkMode", false) ||
                        sp.getBoolean("darkMode", false)
            }
        }
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

    val subsSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_subs", android.content.Context.MODE_PRIVATE)
    }

    val legacySp = remember(ctx) {
        ctx.getSharedPreferences("kmi_prefs", android.content.Context.MODE_PRIVATE)
    }

    var forumAccessRefreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(userSp, subsSp, legacySp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "has_full_access" ||
                key == "full_access" ||
                key == "subscription_active" ||
                key == "is_subscribed" ||
                key == "google_subscription_verified" ||
                key == "google_subscription_checked_at" ||
                key == "sub_access_until" ||
                key == "access_changed_at" ||
                key == "sub_product"
            ) {
                forumAccessRefreshTick++
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)
        subsSp.registerOnSharedPreferenceChangeListener(listener)
        legacySp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
            subsSp.unregisterOnSharedPreferenceChangeListener(listener)
            legacySp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun SharedPreferences.hasActiveSubscriptionAccess(): Boolean {
        val now = System.currentTimeMillis()
        val until = getLong("sub_access_until", 0L)

        val verifiedAndValid =
            getBoolean("google_subscription_verified", false) && until > now

        return KmiAccess.hasFullAccess(this) ||
                verifiedAndValid ||
                getBoolean("has_full_access", false) ||
                getBoolean("full_access", false) ||
                getBoolean("subscription_active", false) ||
                getBoolean("is_subscribed", false)
    }

    val isTrial = remember(forumAccessRefreshTick, isManagerOverride) {
        KmiAccess.isTrialActive(userSp) &&
                !userSp.hasActiveSubscriptionAccess() &&
                !subsSp.hasActiveSubscriptionAccess() &&
                !legacySp.hasActiveSubscriptionAccess()
    }

    val hasFull = remember(forumAccessRefreshTick, isManagerOverride) {
        isManagerOverride ||
                userSp.hasActiveSubscriptionAccess() ||
                subsSp.hasActiveSubscriptionAccess() ||
                legacySp.hasActiveSubscriptionAccess()
    }

    // 🔒 גישת פורום:
    // פתוח רק למנהל או למנוי חודשי/שנתי פעיל.
    // Trial לא פותח פורום.
    val canUseExtras = hasFull

    // סניף + קבוצה של המשתמש — מקור אמת מחדר הפורום
    val branch = remember {
        val active = userSp.getString("active_branch", null)
            ?: userSp.getString("activeBranch", null)

        val raw = active?.takeIf { it.isNotBlank() }
            ?: userSp.getString("branch", null)
            ?: userSp.getString("branchesCsv", null)
            ?: ""

        raw
            .split(",", "•", "|")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: ""
    }

    // groupKey – קודם key אמיתי, אחרת age_group/group
    val groupKey = remember {
        val direct = userSp.getString("groupKey", null)
            ?: userSp.getString("group_key", null)

        direct?.takeIf { it.isNotBlank() }
            ?: userSp.getString("age_group", null)?.takeIf { it.isNotBlank() }
            ?: userSp.getString("group", "")!!.ifBlank { "" }
    }

    val forumRoomId = remember(branch, groupKey) {
        forumRoomDocId(branch, groupKey)
    }

    val openFromPushInitial = remember {
        sp.getBoolean("forum_open_from_push", false)
    }

    var pendingPushRoomId by rememberSaveable {
        mutableStateOf(
            if (openFromPushInitial) {
                sp.getString("forum_push_room_id", "").orEmpty()
            } else {
                ""
            }
        )
    }

    var pendingPushMessageId by rememberSaveable {
        mutableStateOf(
            if (openFromPushInitial) {
                sp.getString("forum_push_message_id", "").orEmpty()
            } else {
                ""
            }
        )
    }

    var pendingPushHandled by rememberSaveable {
        mutableStateOf(!openFromPushInitial)
    }

    LaunchedEffect(openFromPushInitial, pendingPushMessageId) {
        if (openFromPushInitial && pendingPushMessageId.isBlank()) {
            pendingPushHandled = true

            sp.edit()
                .putBoolean("forum_open_from_push", false)
                .remove("forum_push_message_id")
                .remove("forum_push_room_id")
                .remove("forum_push_room_name")
                .remove("forum_push_branch_id")
                .remove("forum_push_group_key")
                .remove("forum_push_sender_id")
                .remove("forum_push_received_at")
                .apply()
        }
    }

    // 👇 שם המשתמש – מנסה כמה מפתחות מהרישום
    val fullName = remember {
        userSp.getString("fullName", null)
            ?: userSp.getString("name", null)
            ?: userSp.getString("displayName", null)
            ?: ""
    }
    val email = remember { userSp.getString("email", "") ?: "" }

    LaunchedEffect(branch, groupKey) {
        if (branch.isNotBlank() && groupKey.isNotBlank()) {
            userSp.edit()
                .putLong(forumLastReadKey(branch, groupKey), System.currentTimeMillis())
                .apply()
        }
    }

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

    // במסך אמת לא מתחברים אנונימית.
    // הפורום צריך לעבוד עם משתמש Firebase אמיתי מהכניסה לאפליקציה.
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance()
    }

    // ================== האזנה בזמן אמת ==================
    DisposableEffect(branch, groupKey) {
        // חייב לפחות סניף; קבוצה משמשת רק כתווית בחדר, לא לסינון
        if (branch.isBlank()) {
            messages = emptyList()
            onDispose { }
        } else {
            val registration = db.collection("branches")
                .document(branch)
                .collection("forumRooms")
                .document(forumRoomId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener { snap, error ->

                    if (error != null) {
                        return@addSnapshotListener
                    }

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
                                messageId = doc.getString("messageId") ?: doc.id,
                                branch = doc.getString("branch") ?: branch,
                                groupKey = doc.getString("groupKey") ?: groupKey,
                                authorName = authorNameDoc,
                                authorEmail = authorEmailDoc,
                                authorUid = authorUidDoc,
                                text = doc.getString("text") ?: "",
                                createdAt = instant,
                                createdAtMillis = doc.getLong("createdAtMillis")
                                    ?: instant.toEpochMilliseconds(),
                                updatedAtMillis = doc.getLong("updatedAtMillis"),
                                mediaUrl = doc.getString("mediaUrl"),
                                mediaType = doc.getString("mediaType"),
                                isMine = (authorUidDoc != null && authorUidDoc == currentUid)
                            )
                        }
                        ?: emptyList()

                    messages = uiList

                    scope.launch {
                        if (
                            !pendingPushHandled &&
                            pendingPushMessageId.isNotBlank() &&
                            uiList.isNotEmpty()
                        ) {
                            val targetIndex = uiList.indexOfFirst { msg ->
                                msg.id == pendingPushMessageId ||
                                        msg.messageId == pendingPushMessageId
                            }

                            if (targetIndex >= 0) {
                                listState.animateScrollToItem(targetIndex)
                                pendingPushHandled = true

                                sp.edit()
                                    .putBoolean("forum_open_from_push", false)
                                    .remove("forum_push_message_id")
                                    .remove("forum_push_room_id")
                                    .remove("forum_push_room_name")
                                    .remove("forum_push_branch_id")
                                    .remove("forum_push_group_key")
                                    .remove("forum_push_sender_id")
                                    .remove("forum_push_received_at")
                                    .apply()

                                pendingPushMessageId = ""
                                pendingPushRoomId = ""
                            } else {
                                listState.animateScrollToItem(0)
                            }
                        } else if (uiList.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                }

            onDispose {
                registration.remove()
                messages = emptyList()
            }
        }
    }

    // ================== משתתפים בפורום — משתמשים אמיתיים מ-Firestore ==================
    // מסך אמת: אין שימוש ב-DemoTrainees.
    // חשוב: משתמשים יכולים לשמור סניף ב-branch / branches / branchesCsv,
    // וגם עם סוגי מקפים שונים. לכן לא מספיק whereEqualTo("branch", branch).
    LaunchedEffect(branch, groupKey, fullName, email) {
        if (branch.isBlank() || groupKey.isBlank()) {
            participantsByUsers = emptyList()
            return@LaunchedEffect
        }

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val currentEmail = email.trim()
        val currentName = fullName.trim()

        fun String.normForum(): String {
            val t = trim()
            val sb = StringBuilder(t.length)
            var lastWasWs = false

            for (ch0 in t) {
                val ch = when (ch0) {
                    '-', '–', '—', '־' -> '-'
                    else -> ch0
                }

                val ws = ch.isWhitespace()
                if (ws) {
                    if (!lastWasWs) sb.append(' ')
                } else {
                    sb.append(ch)
                }
                lastWasWs = ws
            }

            return sb.toString().trim()
        }

        fun String.swapDash(to: Char): String = buildString(length) {
            for (ch in this@swapDash) {
                append(
                    when (ch) {
                        '-', '–', '—', '־' -> to
                        else -> ch
                    }
                )
            }
        }

        fun splitTokensNorm(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()

            return raw
                .replace(" • ", ",")
                .replace("|", ",")
                .replace("\n", ",")
                .split(',', ';', '；')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { it.normForum() }
        }

        fun DocumentSnapshot.userNameOrNull(): String? {
            val full =
                getString("fullName")
                    ?: getString("name")
                    ?: getString("displayName")
                    ?: getString("email")

            return full?.trim()?.takeIf { it.isNotBlank() }
        }

        fun DocumentSnapshot.roleText(): String {
            return (getString("role")
                ?: getString("userType")
                ?: getString("type")
                ?: "")
                .trim()
                .lowercase()
        }

        fun DocumentSnapshot.isAllowedForumRole(): Boolean {
            val role = roleText()

            return role.isBlank() ||
                    role.contains("trainee") ||
                    role.contains("coach") ||
                    role.contains("trainer") ||
                    role.contains("instructor") ||
                    role.contains("מתאמן") ||
                    role.contains("מאמן")
        }

        fun DocumentSnapshot.branchTokensNorm(): List<String> {
            val out = mutableListOf<String>()

            val branchesList = (get("branches") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                .orEmpty()

            out.addAll(branchesList.map { it.normForum() })
            out.addAll(splitTokensNorm(getString("branchesCsv")))
            out.addAll(splitTokensNorm(getString("branch")))
            out.addAll(splitTokensNorm(getString("activeBranch")))
            out.addAll(splitTokensNorm(getString("active_branch")))

            return out
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun expandForumGroupAliases(raw: String): List<String> {
            val n = raw.normForum()

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
                .map { it.normForum() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun DocumentSnapshot.groupTokensNorm(): List<String> {
            val groupsList = (get("groups") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                ?.flatMap { expandForumGroupAliases(it) }
                .orEmpty()

            return buildList {
                addAll(groupsList)
                addAll(splitTokensNorm(getString("primaryGroup")).flatMap { expandForumGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupKey")).flatMap { expandForumGroupAliases(it) })
                addAll(splitTokensNorm(getString("group_key")).flatMap { expandForumGroupAliases(it) })
                addAll(splitTokensNorm(getString("group")).flatMap { expandForumGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupName")).flatMap { expandForumGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupsCsv")).flatMap { expandForumGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupCsv")).flatMap { expandForumGroupAliases(it) })
                addAll(splitTokensNorm(getString("age_group")).flatMap { expandForumGroupAliases(it) })
            }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun matchesForumGroup(tokens: List<String>, candidates: Set<String>): Boolean {
            if (candidates.isEmpty()) return true

            // אם למשתמש אין שדה קבוצה בכלל, לא נכניס אותו לחדר קבוצה ספציפי.
            if (tokens.isEmpty()) return false

            return tokens.any { tok ->
                tok in candidates ||
                        candidates.any { cand ->
                            cand.length >= 2 &&
                                    tok.length >= 2 &&
                                    (tok.contains(cand) || cand.contains(tok))
                        }
            }
        }

        fun matchesBranch(tokens: List<String>, candidates: Set<String>): Boolean {
            if (tokens.isEmpty() || candidates.isEmpty()) return false

            return tokens.any { tok ->
                tok in candidates ||
                        candidates.any { cand ->
                            cand.length >= 4 &&
                                    tok.length >= 4 &&
                                    (tok.contains(cand) || cand.contains(tok))
                        }
            }
        }

        suspend fun fetchUsersFor(branchValue: String): List<DocumentSnapshot> {
            val col = db.collection("users")
            val out = mutableListOf<DocumentSnapshot>()

            runCatching {
                out.addAll(
                    col.whereArrayContains("branches", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            runCatching {
                out.addAll(
                    col.whereEqualTo("branchesCsv", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            runCatching {
                out.addAll(
                    col.whereEqualTo("branch", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            runCatching {
                out.addAll(
                    col.whereEqualTo("activeBranch", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            runCatching {
                out.addAll(
                    col.whereEqualTo("active_branch", branchValue)
                        .get()
                        .await()
                        .documents
                )
            }

            return out
        }

        val branchCandidates = listOf(
            branch,
            branch.swapDash('-'),
            branch.swapDash('–'),
            branch.swapDash('—'),
            branch.swapDash('־'),
            branch.replace("  ", " ")
        ).map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val groupCandidates = expandForumGroupAliases(groupKey)
            .flatMap { g ->
                listOf(
                    g,
                    g.replace("-", "–"),
                    g.replace("–", "-"),
                    g.replace(" + ", " ו"),
                    g.replace(" ו", " + ")
                )
            }
            .map { it.normForum() }
            .filter { it.isNotBlank() }
            .distinct()

        scope.launch {
            try {
                var docs = branchCandidates
                    .flatMap { cand -> fetchUsersFor(cand) }
                    .distinctBy { it.id }

                if (docs.isEmpty()) {
                    val all = mutableListOf<DocumentSnapshot>()
                    val col = db.collection("users")

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

                        if (all.size >= 5000) break
                    }

                    val candNorm = branchCandidates.map { it.normForum() }.toSet()

                    val groupNorm = groupCandidates.toSet()

                    docs = all.filter { doc ->
                        matchesBranch(doc.branchTokensNorm(), candNorm) &&
                                matchesForumGroup(doc.groupTokensNorm(), groupNorm)
                    }.distinctBy { it.id }
                }

                fun normalizeParticipantName(value: String): String {
                    return value
                        .trim()
                        .lowercase()
                        .replace("‏", "")
                        .replace("יובל פולק", "יובל פולק")
                        .replace(Regex("\\s+"), " ")
                }

                fun DocumentSnapshot.participantUniqueKey(): String {
                    val docUid = getString("uid")
                        ?: getString("authUid")
                        ?: ""

                    val docEmail = getString("email").orEmpty().trim().lowercase()
                    val docPhone = getString("phone").orEmpty().trim()
                    val docName = userNameOrNull().orEmpty()

                    return when {
                        docUid.isNotBlank() -> "uid:${docUid.trim()}"
                        docEmail.isNotBlank() -> "email:$docEmail"
                        docPhone.isNotBlank() -> "phone:$docPhone"
                        docName.isNotBlank() -> "name:${normalizeParticipantName(docName)}"
                        else -> "doc:${id}"
                    }
                }

                val groupNorm = groupCandidates.toSet()

                val realParticipants = docs
                    .asSequence()
                    .filter { it.isAllowedForumRole() }
                    .filter { it.userNameOrNull()?.isNotBlank() == true }
                    .filter { matchesForumGroup(it.groupTokensNorm(), groupNorm) }
                    .groupBy { it.participantUniqueKey() }
                    .values
                    .mapNotNull { samePersonDocs ->
                        val doc = samePersonDocs.firstOrNull() ?: return@mapNotNull null
                        val cleanName = doc.userNameOrNull() ?: return@mapNotNull null

                        val docEmail = doc.getString("email").orEmpty().trim()
                        val docUid =
                            doc.getString("uid")
                                ?: doc.getString("authUid")
                                ?: doc.id

                        ForumParticipantUi(
                            id = docUid.ifBlank { doc.id },
                            name = cleanName,
                            isMe = (
                                    (currentUid != null && docUid == currentUid) ||
                                            (currentEmail.isNotBlank() && docEmail == currentEmail) ||
                                            (currentName.isNotBlank() && cleanName.trim() == currentName)
                                    )
                        )
                    }
                    .distinctBy {
                        normalizeParticipantName(it.name)
                    }
                    .sortedBy { it.name }
                    .toList()

                participantsByUsers = realParticipants
            } catch (_: Exception) {
                participantsByUsers = emptyList()
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

    val currentFirebaseUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    val isCurrentUserForumParticipant = remember(
        participantsByUsers,
        currentFirebaseUid,
        email,
        fullName,
        isManagerOverride
    ) {
        if (isManagerOverride) {
            true
        } else {
            participantsByUsers.any { p ->
                p.isMe ||
                        (currentFirebaseUid.isNotBlank() && p.id == currentFirebaseUid) ||
                        (email.isNotBlank() && p.id.equals(email, ignoreCase = true)) ||
                        (fullName.isNotBlank() && p.name.trim() == fullName.trim())
            }
        }
    }

    // ---------- שליחת/עדכון הודעה ----------
    suspend fun sendMessageInternal() {
        try {
            val text = (if (editingMessage != null) editText else input).trim()
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            val currentUid = currentUser?.uid

            if (text.isEmpty() && attachedUri == null) return
            if (branch.isBlank() || groupKey.isBlank()) return

            if (currentUser == null || currentUid.isNullOrBlank()) {
                Toast.makeText(
                    ctx,
                    forumTr(
                        isEnglish,
                        "לא ניתן לשלוח הודעה לפני התחברות משתמש.",
                        "You must be signed in before sending a message."
                    ),
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            if (!isCurrentUserForumParticipant) {
                Toast.makeText(
                    ctx,
                    forumTr(
                        isEnglish,
                        "אין הרשאה לשלוח הודעות בחדר הקבוצה הזה.",
                        "You do not have permission to send messages in this group room."
                    ),
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // העלאת מדיה (אם יש)
            var mediaUrl: String? = null
            val mediaType = attachedMediaType

            if (attachedUri != null && mediaType != null) {
                val path =
                    "forum_media/${forumSafeDocId(branch)}/${forumSafeDocId(groupKey)}/$currentUid/${System.currentTimeMillis()}"
                val ref = storage.reference.child(path)
                ref.putFile(attachedUri!!).await()
                mediaUrl = ref.downloadUrl.await().toString()
            }

            val messagePreview = when {
                text.isNotBlank() -> text.take(120)
                mediaType == "image" -> forumTr(isEnglish, "תמונה חדשה", "New image")
                mediaType == "video" -> forumTr(isEnglish, "סרטון חדש", "New video")
                else -> forumTr(isEnglish, "הודעה חדשה", "New message")
            }

            // דאטה בסיסי להודעה
            val safeAuthorName = fullName
                .ifBlank { userSp.getString("displayName", "").orEmpty() }
                .ifBlank { userSp.getString("name", "").orEmpty() }
                .ifBlank { email }
                .ifBlank { forumTr(isEnglish, "משתתף", "Participant") }

            // מבטיח שחדר הפורום קיים כמסמך אמת בשרת
            val roomRef = db.collection("branches")
                .document(branch)
                .collection("forumRooms")
                .document(forumRoomId)

            roomRef.set(
                mapOf(
                    "roomId" to forumRoomId,
                    "branch" to branch,
                    "groupKey" to groupKey,
                    "participantCount" to participantsByUsers.size,
                    "participantIds" to participantsByUsers.map { it.id }.take(200),
                    "participantNames" to participantsByUsers.map { it.name }.take(200),
                    "participantSource" to "users_by_branch_and_group",
                    "pushEnabled" to true,
                    "pushTarget" to "forum_room_participants",
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "updatedAtMillis" to System.currentTimeMillis(),
                    "lastMessagePreview" to messagePreview,
                    "lastMessageAuthorUid" to currentUid,
                    "lastMessageAuthorName" to safeAuthorName,
                    "source" to "android_forum"
                ),
                SetOptions.merge()
            ).await()

            val expiresAtDate = Date(
                System.currentTimeMillis() + FORUM_MESSAGE_RETENTION_MILLIS
            )

            val baseData = mutableMapOf<String, Any?>(
                "roomId" to forumRoomId,
                "branch" to branch,
                "groupKey" to groupKey,
                "authorName" to safeAuthorName,
                "authorEmail" to email,
                "authorUid" to currentUid,
                "authorIsManager" to isManagerOverride,
                "text" to text,
                "messagePreview" to messagePreview,
                "hasMedia" to (mediaUrl != null && mediaType != null),
                "mediaType" to mediaType,
                "expiresAt" to com.google.firebase.Timestamp(expiresAtDate),
                "retentionDays" to FORUM_MESSAGE_RETENTION_DAYS,
                "isPinned" to false,
                "pushStatus" to "pending",
                "pushCreatedBy" to "android_forum",
                "source" to "android_forum"
            )

            if (mediaUrl != null && mediaType != null) {
                baseData["mediaUrl"] = mediaUrl
                baseData["mediaType"] = mediaType
            }

            if (editingMessage == null) {
                // הודעה חדשה — מוגדרת למחיקה אוטומטית אחרי 90 יום דרך Firestore TTL
                val nowMillis = System.currentTimeMillis()
                val messageRef = db.collection("branches")
                    .document(branch)
                    .collection("forumRooms")
                    .document(forumRoomId)
                    .collection("messages")
                    .document()

                baseData["messageId"] = messageRef.id
                baseData["createdAt"] = FieldValue.serverTimestamp()
                baseData["createdAtMillis"] = nowMillis
                baseData["updatedAtMillis"] = nowMillis

                messageRef
                    .set(baseData.filterValues { it != null }, SetOptions.merge())
                    .await()

                roomRef.set(
                    mapOf(
                        "lastMessageId" to messageRef.id,
                        "lastMessagePreview" to messagePreview,
                        "lastMessageAuthorUid" to currentUid,
                        "lastMessageAuthorName" to safeAuthorName,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "lastMessageAtMillis" to nowMillis,
                        "lastMessageHasMedia" to (mediaUrl != null && mediaType != null),
                        "lastMessageMediaType" to mediaType,
                        "pendingPushMessageId" to messageRef.id,
                        "pendingPushAuthorUid" to currentUid,
                        "pendingPushPreview" to messagePreview,
                        "pendingPushAt" to FieldValue.serverTimestamp(),
                        "pendingPushAtMillis" to nowMillis,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "updatedAtMillis" to nowMillis
                    ),
                    SetOptions.merge()
                ).await()
            } else {
                val msg = editingMessage ?: return
                val canEditThisMessage = msg.isMine || isManagerOverride

                if (!canEditThisMessage) {
                    Toast.makeText(
                        ctx,
                        forumTr(
                            isEnglish,
                            "אין הרשאה לערוך הודעה זו.",
                            "You do not have permission to edit this message."
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val nowMillis = System.currentTimeMillis()

                // עדכון הודעה קיימת — לא מאריכים את expiresAt בעריכה
                baseData.remove("expiresAt")
                baseData.remove("retentionDays")
                baseData.remove("isPinned")
                baseData["messageId"] = msg.messageId
                baseData["updatedAt"] = FieldValue.serverTimestamp()
                baseData["updatedAtMillis"] = nowMillis
                baseData["edited"] = true

                db.collection("branches")
                    .document(branch)
                    .collection("forumRooms")
                    .document(forumRoomId)
                    .collection("messages")
                    .document(msg.id)
                    .set(
                        baseData.filterValues { it != null },
                        SetOptions.merge()
                    )
                    .await()

                roomRef.set(
                    mapOf(
                        "lastMessagePreview" to messagePreview,
                        "lastMessageAuthorUid" to currentUid,
                        "lastMessageAuthorName" to safeAuthorName,
                        "lastMessageEditedAt" to FieldValue.serverTimestamp(),
                        "lastMessageEditedAtMillis" to nowMillis,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "updatedAtMillis" to nowMillis
                    ),
                    SetOptions.merge()
                ).await()
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
            Toast.makeText(
                ctx,
                forumTr(
                    isEnglish,
                    "שגיאה בשמירת ההודעה: ${e.localizedMessage ?: "בדוק חיבור לאינטרנט"}",
                    "Error saving message: ${e.localizedMessage ?: "Check your internet connection"}"
                ),
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
                title = forumTr(isEnglish, "פורום הסניף", "Branch Forum"),
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
                        isTrial && !hasFull -> forumTr(
                            isEnglish,
                            "במהלך תקופת הניסיון מסך הפורום נעול.\nאחרי רכישת מנוי המסך ייפתח עבורך.",
                            "During the trial period, the forum is locked.\nAfter purchasing a subscription, this screen will be available."
                        )

                        !isTrial && !hasFull -> forumTr(
                            isEnglish,
                            "מסך הפורום זמין למנויים בלבד.\nכדי להמשיך יש לרכוש מנוי פעיל.",
                            "The forum is available to subscribers only.\nTo continue, please purchase an active subscription."
                        )

                        else -> forumTr(
                            isEnglish,
                            "מסך הפורום זמין למנויים בלבד.",
                            "The forum is available to subscribers only."
                        )
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
                                text = forumTr(isEnglish, "גישה לפורום", "Forum Access"),
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
                                    text = forumTr(isEnglish, "עבור למסך המנוי", "Go to Subscription"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = forumTr(
                                    isEnglish,
                                    "ניתן לחזור תמיד למסך זה לאחר רכישת מנוי.",
                                    "You can always return to this screen after purchasing a subscription."
                                ),
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
                        text = forumTr(
                            isEnglish,
                            "לא אותרו סניף/קבוצה במשתמש.\nודאו ש־\"branch\" ו־\"groupKey\" מוגדרים בפרופיל.",
                            "No branch/group was found for this user.\nPlease make sure \"branch\" and \"groupKey\" are set in the profile."
                        ),
                        color = Color(0xFFFF6B6B),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                    return@Column
                }

                // כותרת בולטת לסניף / קבוצה
                val roomLabel = forumTr(
                    isEnglish,
                    "סניף: $branch  •  קבוצה: $groupKey",
                    "Branch: $branch  •  Group: $groupKey"
                )

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
                val participants = participantsByUsers

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
                            text = forumTr(
                                isEnglish,
                                "משתתפים בפורום (${participants.size})",
                                "Forum participants (${participants.size})"
                            ),
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

                if (participants.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = if (isDarkMode) 0.dp else 3.dp,
                        color = participantsColor,
                        border = BorderStroke(1.dp, forumHeaderBorder)
                    ) {
                        Text(
                            text = forumTr(
                                isEnglish,
                                "אין משתתפים רשומים בקבוצה הזו עדיין",
                                "No registered participants in this group yet"
                            ),
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
                                text = forumTr(
                                    isEnglish,
                                    "משתתפים בפורום (${participants.size})",
                                    "Forum participants (${participants.size})"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = screenTextAlign,
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
                                            text = if (p.isMe) {
                                                forumTr(isEnglish, "${p.name} (אני)", "${p.name} (me)")
                                            } else {
                                                p.name
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = screenTextAlign,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showParticipantsDialog = false }) {
                                Text(forumTr(isEnglish, "סגור", "Close"))
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
                    if (messages.isEmpty()) {
                        item(key = "empty_forum_room") {
                            EmptyForumRoomCard(
                                branch = branch,
                                groupKey = groupKey,
                                isDarkMode = isDarkMode,
                                isEnglish = isEnglish
                            )
                        }
                    }

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
                                            .widthIn(max = 260.dp)
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val participantNameByUid = msg.authorUid
                                            ?.let { uid ->
                                                participantsByUsers.firstOrNull { it.id == uid }?.name
                                            }
                                            .orEmpty()

                                        val messageAuthorName = msg.authorName
                                            .ifBlank { participantNameByUid }
                                            .ifBlank { msg.authorEmail }
                                            .ifBlank { forumTr(isEnglish, "משתתף", "Participant") }

                                        Text(
                                            text = if (msg.isMine) {
                                                forumTr(
                                                    isEnglish,
                                                    "$messageAuthorName • אני",
                                                    "$messageAuthorName • me"
                                                )
                                            } else {
                                                messageAuthorName
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = textColor.copy(alpha = 0.78f),
                                            fontWeight = FontWeight.Black,
                                            textAlign = screenTextAlign,
                                            maxLines = 1,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (msg.text.isNotBlank()) {
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = textColor,
                                                textAlign = screenTextAlign,
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
                                                            contentDescription = forumTr(isEnglish, "תמונה מצורפת", "Attached image"),
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
                                                                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                                            ) {
                                                                Text(
                                                                    text = forumTr(isEnglish, "סרטון מצורף", "Attached video"),
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    textAlign = screenTextAlign,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )
                                                                Text(
                                                                    text = forumTr(isEnglish, "לחיצה לפתיחה בנגן", "Tap to open in player"),
                                                                    color = Color.White.copy(alpha = 0.78f),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    textAlign = screenTextAlign,
                                                                    modifier = Modifier.fillMaxWidth()
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
                                                                Text(forumTr(isEnglish, "פתח", "Open"))
                                                            }
                                                        }
                                                    }
                                                }

                                                else -> {}
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isEnglish) {
                                                Arrangement.End
                                            } else {
                                                Arrangement.Start
                                            },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val canModifyMessage = msg.isMine || isManagerOverride

                                            if (canModifyMessage) {
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
                                                        contentDescription = forumTr(isEnglish, "עריכת הודעה", "Edit message"),
                                                        tint = textColor.copy(alpha = 0.72f)
                                                    )
                                                }
                                                Spacer(Modifier.width(2.dp))
                                                IconButton(
                                                    onClick = {
                                                        if (!canModifyMessage) return@IconButton

                                                        scope.launch {
                                                            val roomIdForMessage = msg.groupKey
                                                                .takeIf { it.isNotBlank() }
                                                                ?.let { forumRoomDocId(msg.branch, it) }
                                                                ?: forumRoomId

                                                            db.collection("branches")
                                                                .document(msg.branch)
                                                                .collection("forumRooms")
                                                                .document(roomIdForMessage)
                                                                .collection("messages")
                                                                .document(msg.id)
                                                                .delete()
                                                                .await()

                                                            val deleteAtMillis = System.currentTimeMillis()

                                                            db.collection("branches")
                                                                .document(msg.branch)
                                                                .collection("forumRooms")
                                                                .document(roomIdForMessage)
                                                                .set(
                                                                    mapOf(
                                                                        "updatedAt" to FieldValue.serverTimestamp(),
                                                                        "updatedAtMillis" to deleteAtMillis,
                                                                        "lastModerationAction" to "message_deleted",
                                                                        "lastModerationByUid" to FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                                                                        "lastDeletedMessageId" to msg.messageId,
                                                                        "lastDeletedAt" to FieldValue.serverTimestamp(),
                                                                        "lastDeletedAtMillis" to deleteAtMillis
                                                                    ),
                                                                    SetOptions.merge()
                                                                )
                                                                .await()
                                                        }
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = forumTr(isEnglish, "מחיקת הודעה", "Delete message"),
                                                        tint = textColor.copy(alpha = 0.72f)
                                                    )
                                                }
                                                Spacer(Modifier.width(4.dp))
                                            }

                                            Text(
                                                text = formatInstant(msg.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.62f),
                                                textAlign = screenTextAlign
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
                                    "image" -> forumTr(isEnglish, "תמונה מצורפת לשליחה", "Image attached")
                                    "video" -> forumTr(isEnglish, "סרטון מצורף לשליחה", "Video attached")
                                    else -> forumTr(isEnglish, "קובץ מצורף", "Attachment")
                                },
                                color = attachmentChipText,
                                style = MaterialTheme.typography.labelMedium
                            )
                            TextButton(onClick = {
                                attachedUri = null
                                attachedMediaType = null
                            }) {
                                Text(forumTr(isEnglish, "הסר", "Remove"))
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
                                    contentDescription = forumTr(isEnglish, "צרף תמונה", "Attach image"),
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
                                    textAlign = screenTextAlign,
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
                                        contentAlignment = if (isEnglish) Alignment.CenterStart else Alignment.CenterEnd
                                    ) {
                                        val currentText =
                                            if (editingMessage != null) editText else input

                                        if (currentText.isBlank()) {
                                            Text(
                                                text = if (!isCurrentUserForumParticipant) {
                                                    forumTr(
                                                        isEnglish,
                                                        "אין הרשאה לשלוח בחדר זה",
                                                        "No permission to send in this room"
                                                    )
                                                } else if (editingMessage != null) {
                                                    forumTr(isEnglish, "עריכת הודעה...", "Editing message...")
                                                } else {
                                                    forumTr(isEnglish, "הודעה", "Message")
                                                },
                                                color = inputPlaceholderColor,
                                                textAlign = screenTextAlign,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    lineHeight = 22.sp
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = if (isEnglish) Alignment.CenterStart else Alignment.CenterEnd
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
                                    contentDescription = forumTr(isEnglish, "צרף וידאו", "Attach video"),
                                    tint = inputIconTint
                                )
                            }
                        }
                    }

                    val canSendContent =
                        (if (editingMessage != null) editText else input).trim().isNotEmpty() || attachedUri != null

                    val canSend = canSendContent && isCurrentUserForumParticipant

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
                                    if (editingMessage != null) {
                                        forumTr(isEnglish, "עדכן הודעה", "Update message")
                                    } else {
                                        forumTr(isEnglish, "שלח", "Send")
                                    }
                                } else {
                                    forumTr(isEnglish, "הקלטה", "Voice recording")
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

                val explanation = remember(belt, item, isEnglish) {
                    findExplanationForHit(
                        belt = belt,
                        rawItem = item,
                        topic = topic,
                        isEnglish = isEnglish
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
                                    textAlign = screenTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = if (isEnglish) "(${belt.en})" else "(${belt.heb})",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = screenTextAlign,
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
                                        contentDescription = forumTr(isEnglish, "מועדף", "Favorite"),
                                        tint = Color(0xFFFFC107)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.StarBorder,
                                        contentDescription = forumTr(isEnglish, "הוסף למועדפים", "Add to favorites"),
                                    )
                                }
                            }
                        }
                    },
                    text = {
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = screenTextAlign,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { pickedKey = null }) {
                            Text(forumTr(isEnglish, "סגור", "Close"))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyForumRoomCard(
    branch: String,
    groupKey: String,
    isDarkMode: Boolean,
    isEnglish: Boolean
) {
    val align = forumTextAlign(isEnglish)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (isDarkMode) {
            Color(0xFF202C33).copy(alpha = 0.92f)
        } else {
            Color.White.copy(alpha = 0.96f)
        },
        border = BorderStroke(
            1.dp,
            if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0xFFD6E4F4)
        ),
        tonalElevation = 0.dp,
        shadowElevation = if (isDarkMode) 0.dp else 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (isDarkMode) Color.White.copy(alpha = 0.04f) else Color(0xFFEAF7FF),
                            if (isDarkMode) Color.Transparent else Color.White
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE0F2FE),
                border = BorderStroke(
                    1.dp,
                    if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0xFFBAE6FD)
                )
            ) {
                Box(
                    modifier = Modifier.size(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (isDarkMode) Color(0xFFBFDBFE) else Color(0xFF0284C7),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Text(
                text = forumTr(
                    isEnglish,
                    "אין עדיין הודעות בחדר הזה",
                    "No messages in this room yet"
                ),
                color = if (isDarkMode) Color(0xFFE9EDEF) else Color(0xFF0F172A),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = forumTr(
                    isEnglish,
                    "הפורום מחובר לשרת. הודעות שתשלחו כאן יישמרו בחדר הקבוצה ויופיעו לכל המשתתפים המורשים.",
                    "This forum is connected to the server. Messages sent here will be saved in the group room and shown to authorized participants."
                ),
                color = if (isDarkMode) Color(0xFFBFC8CD) else Color(0xFF475569),
                style = MaterialTheme.typography.bodySmall,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = forumTr(
                    isEnglish,
                    "סניף: ${branch.ifBlank { "—" }}  •  קבוצה: ${groupKey.ifBlank { "—" }}",
                    "Branch: ${branch.ifBlank { "—" }}  •  Group: ${groupKey.ifBlank { "—" }}"
                ),
                color = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF2563EB),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String,
    isEnglish: Boolean
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

    return if (isEnglish) {
        "There is currently no explanation for this exercise."
    } else {
        "אין כרגע הסבר לתרגיל הזה."
    }
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




