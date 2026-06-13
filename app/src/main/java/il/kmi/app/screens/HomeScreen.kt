package il.kmi.app.screens

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.R
import il.kmi.app.training.TrainingData
import java.lang.reflect.AccessibleObject
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import il.kmi.shared.domain.Belt
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.material3.Switch
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.assistant.ui.AiAssistantDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.app.domain.color
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import il.kmi.app.favorites.FavoritesStore
import android.app.Activity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.database.KmiDatabaseProvider
import kotlinx.coroutines.delay
import il.kmi.app.domain.ExerciseExplanationResolver

//=================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoachInfoCard(
    coachName: String?,
    branchName: String?,
    city: String?,
    groupName: String?,
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "מאמן: ${coachName.orEmpty()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val branchLine = buildString {
                    if (!city.isNullOrBlank()) append(city).append(" · ")
                    append(branchName.orEmpty())
                }
                Text(
                    text = branchLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "קבוצה: ${groupName.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    clickSound()
                    haptic(true)
                    onOpenProfile()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "פרופיל",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TrainingsWeekHeader(
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val heb = java.util.Locale("he", "IL")
    val dateFmt = java.text.SimpleDateFormat("dd/MM", heb)
    val dayFmt  = java.text.SimpleDateFormat("EEEE", heb)   // שם היום בעברית
    val start = java.util.Calendar.getInstance()
    val end   = (start.clone() as java.util.Calendar).apply {
        add(java.util.Calendar.DAY_OF_YEAR, 6)
    }

    // היום + תאריך: "יום חמישי 11/12"
    val startLabel = "${dayFmt.format(start.time)} ${dateFmt.format(start.time)}"
    val endLabel   = "${dayFmt.format(end.time)} ${dateFmt.format(end.time)}"

    Box(
        modifier = modifier
            .heightIn(min = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 2.dp, bottom = 5.dp)
        ) {
            Text(
                text = if (isEnglish) "Trainings for the upcoming week" else "אימונים לשבוע הקרוב",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = "(תאריכים: $startLabel–$endLabel)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : AccessibleObject> T.makeAccessible(): T {
    try { isAccessible = true } catch (_: SecurityException) { /* ignore */ }
    return this
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSubscription: () -> Unit,
    trainings: List<TrainingData>,
    onOpenExercise: (String) -> Unit,
    onOpenFreeSessions: (String, String, String, String) -> Unit,
    onOpenMonthlyCalendar: () -> Unit,
    onOpenTrainingSummary: () -> Unit
) {
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    // 🔵 מצב לדיאלוג העוזר האישי (AI)
    var showAiDialog by rememberSaveable { mutableStateOf(false) }

    val ctxRole = LocalContext.current
    val contextLang = LocalContext.current
    val langManager = remember { AppLanguageManager(contextLang) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val userSpRole = remember { ctxRole.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
    var userRole by remember { mutableStateOf(userSpRole.getString("user_role", "trainee")) }

    val notePrefs = remember(ctxRole) {
        ctxRole.getSharedPreferences("kmi_exercise_notes", Context.MODE_PRIVATE)
    }

    fun normalizeFavoriteId(raw: String): String =
        raw.substringAfter("::", raw)
            .substringAfter(":", raw)
            .trim()

    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var notesRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    fun saveHomeExerciseNote(noteKey: String, text: String) {
        val clean = text.trim()

        notePrefs.edit().apply {
            if (clean.isBlank()) {
                remove(noteKey)
            } else {
                putString(noteKey, clean)
            }
        }.apply()

        notesRefreshKey++
    }

// ✅ Speed Dial FAB
    var fabExpanded by rememberSaveable { mutableStateOf(false) }

    // ✅ נשמור פרמטרים ל-FAB "אימונים חופשיים" מחוץ ל-Column (כדי שיהיו זמינים גם אחרי שה-Column נסגר)
    var freeBranchUi by rememberSaveable { mutableStateOf("") }
    var freeGroupKeyUi by rememberSaveable { mutableStateOf("") }
    var freeUidUi by rememberSaveable { mutableStateOf("") }
    var freeNameUi by rememberSaveable { mutableStateOf("") }

    DisposableEffect(userSpRole) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "user_role") userRole = userSpRole.getString("user_role", "trainee")
        }
        userSpRole.registerOnSharedPreferenceChangeListener(l)
        onDispose { userSpRole.unregisterOnSharedPreferenceChangeListener(l) }
    }

    val isCoach = remember(userRole) {
        when (userRole?.trim()?.lowercase()) {
            "coach", "trainer", "מאמן" -> true
            else -> false
        }
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FBFF),
                Color(0xFFEAF4FF),
                Color(0xFFB7DDF7),
                Color(0xFF1F78B4),
                Color(0xFF062B4A)
            )
        )
    }

    Scaffold(
        topBar = {

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            il.kmi.app.ui.KmiTopBar(
                title = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "Home" else "מסך הבית",
                onHome = { /* no-op במסך הבית */ },
                lockHome = true,
                homeDisabledToast = "אתה כבר במסך הבית 🙂",
                showTopHome = false,

                currentLang =
                    if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",

                onToggleLanguage = {

                    val newLang =
                        if (langManager.getCurrentLanguage() == AppLanguage.HEBREW)
                            AppLanguage.ENGLISH
                        else
                            AppLanguage.HEBREW

                    langManager.setLanguage(newLang)

                    (contextLang as Activity).recreate()
                },

                // חיפוש תרגיל מהסרגל התחתון
                onPickSearchResult = { key ->
                    clickSound()
                    haptic(true)
                    pickedKey = key
                },
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top    = padding.calculateTopPadding(),
                    start  = padding.calculateStartPadding(LocalLayoutDirection.current),
                    end    = padding.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = padding.calculateBottomPadding()
                )
                .background(backgroundBrush)
        ) {
            val listState = rememberLazyListState()

            val ctx = LocalContext.current
            val userSp = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
            val subsSp = remember { ctx.getSharedPreferences("kmi_subs", Context.MODE_PRIVATE) }
            val legacySp = remember { ctx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE) }

            var homeAccessRefreshTick by remember { mutableIntStateOf(0) }

            // מרענן את מצב הגישה גם בלי שינוי ב-SharedPreferences,
            // כדי שכשה-sub_access_until עובר — המנעולים יחזרו לבד.
            LaunchedEffect(Unit) {
                while (true) {
                    delay(30_000L)
                    homeAccessRefreshTick++
                }
            }

            DisposableEffect(userSp, subsSp, legacySp) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { changedSp, key ->
                    if (
                        key == "has_full_access" ||
                        key == "full_access" ||
                        key == "subscription_active" ||
                        key == "is_subscribed" ||
                        key == "google_subscription_verified" ||
                        key == "google_subscription_checked_at" ||
                        key == "sub_product" ||
                        key == "sub_access_until" ||
                        key == "access_changed_at"
                    ) {
                        homeAccessRefreshTick++
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

                val hasSubscriptionFlags =
                    getBoolean("google_subscription_verified", false) ||
                            getBoolean("has_full_access", false) ||
                            getBoolean("full_access", false) ||
                            getBoolean("subscription_active", false) ||
                            getBoolean("is_subscribed", false) ||
                            getString("sub_product", "").orEmpty().isNotBlank()

                // מנוי רגיל / בדיקות פותח רק אם יש זמן תקף.
                val active = hasSubscriptionFlags && until > now

                // אם הזמן עבר — מנקים את כל הדגלים הישנים כדי שהמנעולים יחזרו.
                if (!active && hasSubscriptionFlags && until > 0L && until <= now) {
                    edit()
                        .putBoolean("google_subscription_verified", false)
                        .putBoolean("has_full_access", false)
                        .putBoolean("full_access", false)
                        .putBoolean("subscription_active", false)
                        .putBoolean("is_subscribed", false)
                        .remove("sub_product")
                        .remove("sub_token")
                        .remove("sub_purchase_time")
                        .remove("sub_access_until")
                        .putLong("access_changed_at", System.currentTimeMillis())
                        .apply()

                }

                return active
            }

            val hasFullAccess = remember(homeAccessRefreshTick) {
                userSp.hasActiveSubscriptionAccess() ||
                        subsSp.hasActiveSubscriptionAccess() ||
                        legacySp.hasActiveSubscriptionAccess()
            }

            // הסרגל הצף מופיע רק אחרי גלילה קטנה למטה.
            val showFab by remember(listState) {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 ||
                            listState.firstVisibleItemScrollOffset > 24
                }
            }

            // =========================
            // ⭐ הודעות מהמאמן – state ברמת Box כדי שגם הכרטיס וגם הדיאלוג יכירו אותו
            // =========================
            val currentUid = remember {
                FirebaseAuth.getInstance().currentUser?.uid
            }

            data class CoachHomeMessage(
                val text: String,
                val coachName: String,
                val sentAt: java.util.Date?,
                val branch: String,
                val group: String
            )

            var recentCoachMessages by remember {
                mutableStateOf<List<CoachHomeMessage>>(emptyList())
            }

            var showCoachMessagesDialog by rememberSaveable {
                mutableStateOf(false)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(2.dp))

                // === KMI_MULTI_GROUPS (FIX) ===
                fun readSelectedGroups(sp: SharedPreferences): List<String> {
                    fun splitGroups(raw: String): List<String> {
                        return raw
                            .split(',', ';', '|', '\n')
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    }

                    fun readPrefValueAsList(key: String): List<String> {
                        val value = sp.all[key] ?: return emptyList()

                        return when (value) {
                            is String -> {
                                val raw = value.trim()
                                if (raw.isBlank()) {
                                    emptyList()
                                } else if (raw.startsWith("[")) {
                                    runCatching {
                                        val arr = org.json.JSONArray(raw)
                                        (0 until arr.length())
                                            .mapNotNull { index -> arr.optString(index, null) }
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                    }.getOrDefault(emptyList())
                                } else {
                                    splitGroups(raw)
                                }
                            }

                            is Set<*> -> {
                                value
                                    .mapNotNull { it?.toString()?.trim() }
                                    .filter { it.isNotBlank() }
                            }

                            else -> emptyList()
                        }
                    }

                    fun readListFromPrefs(vararg keys: String): List<String> {
                        keys.forEach { key ->
                            val list = readPrefValueAsList(key)
                            if (list.isNotEmpty()) return list
                        }
                        return emptyList()
                    }

                    return readListFromPrefs(
                        "groups_json",
                        "selected_groups",
                        "groups",
                        "age_groups",
                        "age_group",
                        "group"
                    )
                        .map {
                            il.kmi.app.training.TrainingCatalog
                                .normalizeGroupName(it)
                                .ifBlank { it }
                        }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(3)
                }

                var groupsRefreshTick by remember { mutableIntStateOf(0) }

                var coachFromPrefs by remember(userSp) {
                    mutableStateOf(userSp.getString("coach_name", "") ?: "")
                }

                DisposableEffect(userSp) {
                    val l = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        when (key) {
                            "groups_json",
                            "selected_groups",
                            "groups",
                            "age_groups",
                            "age_group",
                            "group" -> {
                                groupsRefreshTick++
                            }

                            "coach_name" -> coachFromPrefs =
                                userSp.getString("coach_name", "") ?: ""
                        }
                    }
                    userSp.registerOnSharedPreferenceChangeListener(l)
                    onDispose { userSp.unregisterOnSharedPreferenceChangeListener(l) }
                }

                val groupsEffective: List<String> = remember(userSp, groupsRefreshTick) {
                    val savedGroups = readSelectedGroups(userSp)
                    if (savedGroups.isEmpty()) listOf("בוגרים") else savedGroups
                }

                // === KMI_MULTI_GROUPS (FIX) ===

                // =========================
                // ⭐ הודעות מהמאמן – Firestore
                // מציגים הודעה אחרונה בכרטיס + 5 הודעות אחרונות בדיאלוג
                // =========================
                DisposableEffect(currentUid, userSp) {
                    val uid = currentUid.orEmpty().trim()

                    val currentEmail = FirebaseAuth.getInstance()
                        .currentUser
                        ?.email
                        ?.trim()
                        .orEmpty()

                    fun normalizePhone(raw: String): String =
                        raw.filter { it.isDigit() }

                    fun normalizeText(raw: String): String =
                        raw.trim()
                            .replace('־', '-')
                            .replace('–', '-')
                            .replace('—', '-')
                            .replace(Regex("\\s+"), " ")
                            .lowercase(java.util.Locale("he", "IL"))

                    fun prefsAsList(vararg keys: String): List<String> {
                        val out = mutableListOf<String>()

                        keys.forEach { key ->
                            when (val value = userSp.all[key]) {
                                is String -> {
                                    value
                                        .removePrefix("[")
                                        .removeSuffix("]")
                                        .split(',', ';', '|', '\n')
                                        .map { it.trim().trim('"') }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }

                                is Set<*> -> {
                                    value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }

                                is List<*> -> {
                                    value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }
                            }
                        }

                        return out.distinct()
                    }

                    val currentPhones = prefsAsList(
                        "phone",
                        "phoneNumber",
                        "phone_number",
                        "user_phone",
                        "mobile"
                    )
                        .map { normalizePhone(it) }
                        .filter { it.isNotBlank() }
                        .distinct()

                    val currentNames = prefsAsList(
                        "fullName",
                        "full_name",
                        "name",
                        "displayName",
                        "user_name"
                    )

                    val currentBranches = prefsAsList(
                        "active_branch",
                        "branch",
                        "branches",
                        "branches_json",
                        "selected_branches"
                    )
                        .map { normalizeText(it) }
                        .filter { it.isNotBlank() }
                        .distinct()

                    val currentGroups = prefsAsList(
                        "active_group",
                        "group",
                        "groups",
                        "groups_json",
                        "selected_groups",
                        "age_group",
                        "age_groups"
                    )
                        .map {
                            normalizeText(
                                il.kmi.app.training.TrainingCatalog
                                    .normalizeGroupName(it)
                                    .ifBlank { it }
                            )
                        }
                        .filter { it.isNotBlank() }
                        .distinct()

                    fun stringListFromDoc(
                        doc: com.google.firebase.firestore.DocumentSnapshot,
                        vararg keys: String
                    ): List<String> {
                        val out = mutableListOf<String>()

                        keys.forEach { key ->
                            when (val value = doc.get(key)) {
                                is String -> {
                                    value
                                        .removePrefix("[")
                                        .removeSuffix("]")
                                        .split(',', ';', '|', '\n')
                                        .map { it.trim().trim('"') }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }

                                is List<*> -> {
                                    value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }

                                is Set<*> -> {
                                    value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .filter { it.isNotBlank() }
                                        .forEach { out += it }
                                }
                            }
                        }

                        return out.distinct()
                    }

                    fun firstStringFromDoc(
                        doc: com.google.firebase.firestore.DocumentSnapshot,
                        vararg keys: String
                    ): String {
                        keys.forEach { key ->
                            when (val value = doc.get(key)) {
                                is String -> {
                                    val clean = value.trim()
                                    if (clean.isNotBlank()) return clean
                                }

                                is List<*> -> {
                                    val clean = value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .firstOrNull { it.isNotBlank() }

                                    if (!clean.isNullOrBlank()) return clean
                                }

                                is Set<*> -> {
                                    val clean = value
                                        .mapNotNull { it?.toString()?.trim() }
                                        .firstOrNull { it.isNotBlank() }

                                    if (!clean.isNullOrBlank()) return clean
                                }
                            }
                        }

                        return ""
                    }

                    fun docTargetsCurrentUser(
                        doc: com.google.firebase.firestore.DocumentSnapshot
                    ): Boolean {
                        if (
                            uid.isBlank() &&
                            currentEmail.isBlank() &&
                            currentPhones.isEmpty() &&
                            currentNames.isEmpty() &&
                            currentBranches.isEmpty() &&
                            currentGroups.isEmpty()
                        ) {
                            return false
                        }

                        val uidTargets = stringListFromDoc(
                            doc,
                            "targetUids",
                            "targetUid",
                            "recipientUids",
                            "recipientUid",
                            "uids",
                            "userIds",
                            "participantIds",
                            "selectedUids"
                        )

                        if (uid.isNotBlank() && uidTargets.any { it.trim() == uid }) {
                            return true
                        }

                        val emailTargets = stringListFromDoc(
                            doc,
                            "targetEmails",
                            "targetEmail",
                            "recipientEmails",
                            "recipientEmail",
                            "emails",
                            "selectedEmails"
                        )

                        if (
                            currentEmail.isNotBlank() &&
                            emailTargets.any { it.equals(currentEmail, ignoreCase = true) }
                        ) {
                            return true
                        }

                        val phoneTargets = stringListFromDoc(
                            doc,
                            "targetPhones",
                            "targetPhone",
                            "recipientPhones",
                            "recipientPhone",
                            "phones",
                            "selectedPhones"
                        ).map { normalizePhone(it) }

                        if (
                            currentPhones.isNotEmpty() &&
                            phoneTargets.any { target ->
                                currentPhones.any { current -> current == target }
                            }
                        ) {
                            return true
                        }

                        val nameTargets = stringListFromDoc(
                            doc,
                            "targetNames",
                            "targetName",
                            "recipientNames",
                            "recipientName",
                            "names",
                            "selectedNames"
                        )

                        if (
                            currentNames.isNotEmpty() &&
                            nameTargets.any { target ->
                                currentNames.any { current ->
                                    current.trim().equals(target.trim(), ignoreCase = true)
                                }
                            }
                        ) {
                            return true
                        }

                        val docBranches = stringListFromDoc(
                            doc,
                            "branch",
                            "branches",
                            "branchName",
                            "branch_name",
                            "targetBranch",
                            "targetBranches",
                            "selectedBranch",
                            "selectedBranches"
                        ).map { normalizeText(it) }

                        val docGroups = stringListFromDoc(
                            doc,
                            "group",
                            "groups",
                            "groupKey",
                            "group_key",
                            "targetGroup",
                            "targetGroups",
                            "selectedGroup",
                            "selectedGroups"
                        ).map {
                            normalizeText(
                                il.kmi.app.training.TrainingCatalog
                                    .normalizeGroupName(it)
                                    .ifBlank { it }
                            )
                        }

                        val branchMatches =
                            docBranches.isNotEmpty() &&
                                    currentBranches.any { current ->
                                        docBranches.any { it == current }
                                    }

                        val groupMatches =
                            docGroups.isNotEmpty() &&
                                    currentGroups.any { current ->
                                        docGroups.any { it == current }
                                    }

                        return branchMatches && groupMatches
                    }

                    if (
                        uid.isBlank() &&
                        currentEmail.isBlank() &&
                        currentPhones.isEmpty() &&
                        currentNames.isEmpty() &&
                        currentBranches.isEmpty() &&
                        currentGroups.isEmpty()
                    ) {
                        recentCoachMessages = emptyList()
                        onDispose { }
                    } else {
                        val db = FirebaseFirestore.getInstance()

                        val query = db.collection("coachBroadcasts")
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(40)

                        val reg = query.addSnapshotListener { snap, e ->
                            if (e != null) {
                                return@addSnapshotListener
                            }

                            recentCoachMessages = snap
                                ?.documents
                                .orEmpty()
                                .filter { docTargetsCurrentUser(it) }
                                .mapNotNull { doc ->
                                    val text = (
                                            doc.getString("text")
                                                ?: doc.getString("message")
                                                ?: doc.getString("body")
                                                ?: doc.getString("content")
                                            )
                                        ?.trim()
                                        .orEmpty()

                                    if (text.isBlank()) {
                                        null
                                    } else {
                                        CoachHomeMessage(
                                            text = text,
                                            coachName = (
                                                    doc.getString("coachName")
                                                        ?: doc.getString("coach_name")
                                                        ?: doc.getString("senderName")
                                                        ?: doc.getString("fromName")
                                                        ?: "המאמן"
                                                    ).trim(),
                                            sentAt = doc.getTimestamp("createdAt")?.toDate()
                                                ?: doc.getTimestamp("sentAt")?.toDate()
                                                ?: doc.getTimestamp("timestamp")?.toDate(),
                                            branch = firstStringFromDoc(
                                                doc,
                                                "branch",
                                                "branchName",
                                                "branch_name",
                                                "targetBranch",
                                                "selectedBranch"
                                            ),
                                            group = firstStringFromDoc(
                                                doc,
                                                "group",
                                                "groupKey",
                                                "group_key",
                                                "targetGroup",
                                                "selectedGroup"
                                            )
                                        )
                                    }
                                }
                                .take(5)
                        }

                        onDispose { reg.remove() }
                    }
                }

                // =========================

                fun readSelectedBranches(sp: SharedPreferences): List<String> {
                    fun splitBranches(raw: String): List<String> {
                        return raw
                            .split(',', ';', '|', '\n')
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    }

                    fun readPrefValueAsList(key: String): List<String> {
                        val value = sp.all[key] ?: return emptyList()

                        return when (value) {
                            is String -> {
                                val raw = value.trim()
                                if (raw.isBlank()) {
                                    emptyList()
                                } else if (raw.startsWith("[")) {
                                    runCatching {
                                        val arr = org.json.JSONArray(raw)
                                        (0 until arr.length())
                                            .mapNotNull { index -> arr.optString(index, null) }
                                            .map { it.trim() }
                                            .filter { it.isNotBlank() }
                                    }.getOrDefault(emptyList())
                                } else {
                                    splitBranches(raw)
                                }
                            }

                            is Set<*> -> {
                                value
                                    .mapNotNull { it?.toString()?.trim() }
                                    .filter { it.isNotBlank() }
                            }

                            else -> emptyList()
                        }
                    }

                    fun readListFromPrefs(vararg keys: String): List<String> {
                        keys.forEach { key ->
                            val list = readPrefValueAsList(key)
                            if (list.isNotEmpty()) return list
                        }
                        return emptyList()
                    }

                    val fromCanonical = readListFromPrefs(
                        "branches_json",
                        "selected_branches",
                        "branches",
                        "branch"
                    )

                    val b2 = readPrefValueAsList("branch2")
                    val b3 = readPrefValueAsList("branch3")

                    return (fromCanonical + b2 + b3)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(3)
                }

                var branchesRefreshTick by remember { mutableIntStateOf(0) }

                // ✅ שכבת ביטחון:
                // אם ה־SharedPreferences המקומי לא מכיל את כל הסניפים/קבוצות,
                // נטען את הפרופיל מ־Firestore ונעדכן את kmi_user.
                LaunchedEffect(currentUid) {
                    if (currentUid.isNullOrBlank()) return@LaunchedEffect

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUid)
                        .get()
                        .addOnSuccessListener { doc ->

                            fun listFromFirestoreListOrCsv(
                                listKey: String,
                                csvKey: String,
                                fallbackKey: String
                            ): List<String> {
                                val fromList = (doc.get(listKey) as? List<*>)
                                    ?.mapNotNull { it?.toString()?.trim() }
                                    ?.filter { it.isNotBlank() }
                                    ?.distinct()
                                    .orEmpty()

                                if (fromList.isNotEmpty()) return fromList.take(3)

                                val csv = doc.getString(csvKey)
                                    ?.takeIf { it.isNotBlank() }
                                    ?: doc.getString(fallbackKey)
                                    ?: ""

                                return csv
                                    .split(',', ';', '|', '\n')
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                    .take(3)
                            }

                            val remoteBranches = listFromFirestoreListOrCsv(
                                listKey = "branches",
                                csvKey = "branchesCsv",
                                fallbackKey = "branch"
                            )

                            val remoteGroups = listFromFirestoreListOrCsv(
                                listKey = "groups",
                                csvKey = "groupsCsv",
                                fallbackKey = "primaryGroup"
                            )

                            val remoteActiveBranch =
                                doc.getString("activeBranch")
                                    ?.takeIf { it.isNotBlank() && it in remoteBranches }
                                    ?: remoteBranches.firstOrNull()
                                    ?: ""

                            val remoteActiveGroup =
                                doc.getString("activeGroup")
                                    ?.takeIf { it.isNotBlank() && it in remoteGroups }
                                    ?: remoteGroups.firstOrNull()
                                    ?: ""

                            val branchesCsv = remoteBranches.joinToString(", ")
                            val groupsCsv = remoteGroups.joinToString(", ")
                            val branchesJson = org.json.JSONArray(remoteBranches).toString()
                            val groupsJson = org.json.JSONArray(remoteGroups).toString()

                            if (remoteBranches.isNotEmpty() || remoteGroups.isNotEmpty()) {
                                userSp.edit()
                                    // ✅ ניקוי טיפוסים ישנים שאולי נשמרו כ־StringSet
                                    .remove("branches")
                                    .remove("selected_branches")
                                    .remove("groups")
                                    .remove("selected_groups")

                                    // ✅ סניפים
                                    .putString("branch", branchesCsv)
                                    .putString("branches", branchesCsv)
                                    .putString("branches_json", branchesJson)
                                    .putString("selected_branches", branchesCsv)
                                    .putString("active_branch", remoteActiveBranch)

                                    // ✅ קבוצות
                                    .putString("age_groups", groupsCsv)
                                    .putString("groups", groupsCsv)
                                    .putString("groups_json", groupsJson)
                                    .putString("selected_groups", groupsCsv)
                                    .putString("age_group", remoteGroups.firstOrNull().orEmpty())
                                    .putString("group", remoteGroups.firstOrNull().orEmpty())
                                    .putString("active_group", remoteActiveGroup)
                                    .apply()

                                branchesRefreshTick++
                                groupsRefreshTick++
                            }
                        }
                        .addOnFailureListener {
                        }
                }

                DisposableEffect(userSp) {
                    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (
                            key == "branches_json" ||
                            key == "selected_branches" ||
                            key == "branches" ||
                            key == "branch" ||
                            key == "branch2" ||
                            key == "branch3" ||
                            key == "branch_type"
                        ) {
                            branchesRefreshTick++
                        }
                    }

                    userSp.registerOnSharedPreferenceChangeListener(listener)

                    onDispose {
                        userSp.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                val selectedBranches: List<String> = remember(userSp, branchesRefreshTick) {
                    readSelectedBranches(userSp)
                }

                val branchTypeHome = remember(userSp, branchesRefreshTick) {
                    userSp.getString("branch_type", "israel") ?: "israel"
                }

                val isAbroadBranch = branchTypeHome == "abroad"

                val branchesEffective = remember(selectedBranches, isAbroadBranch) {
                    if (selectedBranches.isEmpty() && !isAbroadBranch) {
                        listOf("נתניה – מרכז קהילתי אופק")
                    } else {
                        selectedBranches.take(3)
                    }
                }

                // ✅ name להצגה + פרמטרים לניווט אימונים חופשיים (נעדכן state כדי שה-FAB יוכל להשתמש גם מחוץ ל-Column)
                val freeName = remember(userSp) {
                    userSp.getString("full_name", null)
                        ?: userSp.getString("name", null)
                        ?: userSp.getString("user_name", null)
                        ?: ""
                }.orEmpty()

                LaunchedEffect(branchesEffective, groupsEffective, currentUid, freeName) {
                    freeBranchUi = branchesEffective.firstOrNull().orEmpty()
                    freeGroupKeyUi = groupsEffective.firstOrNull().orEmpty()
                    freeUidUi = currentUid.orEmpty()
                    freeNameUi = freeName
                }

                data class SlotLike(
                    val dayOfWeek: Int,
                    val startHour: Int,
                    val startMinute: Int,
                    val durationMinutes: Int
                )

                fun <T : java.lang.reflect.AccessibleObject> T.makeAccessibleSafe(): T {
                    try {
                        isAccessible = true
                    } catch (_: SecurityException) {
                        /* ignore */
                    }
                    return this
                }

                fun readSlot(slot: Any): SlotLike {
                    val cls = slot::class.java

                    val dayField = runCatching { cls.getDeclaredField("day").makeAccessibleSafe() }.getOrNull()
                    val startField =
                        runCatching { cls.getDeclaredField("start").makeAccessibleSafe() }.getOrNull()
                    val endField = runCatching { cls.getDeclaredField("end").makeAccessibleSafe() }.getOrNull()

                    if (dayField != null && startField != null && endField != null) {
                        val dayEnum = runCatching { dayField.get(slot) as? java.time.DayOfWeek }.getOrNull()
                        val startLt = runCatching { startField.get(slot) as? java.time.LocalTime }.getOrNull()
                        val endLt = runCatching { endField.get(slot) as? java.time.LocalTime }.getOrNull()

                        val calDay = when (dayEnum) {
                            java.time.DayOfWeek.SUNDAY -> java.util.Calendar.SUNDAY
                            java.time.DayOfWeek.MONDAY -> java.util.Calendar.MONDAY
                            java.time.DayOfWeek.TUESDAY -> java.util.Calendar.TUESDAY
                            java.time.DayOfWeek.WEDNESDAY -> java.util.Calendar.WEDNESDAY
                            java.time.DayOfWeek.THURSDAY -> java.util.Calendar.THURSDAY
                            java.time.DayOfWeek.FRIDAY -> java.util.Calendar.FRIDAY
                            java.time.DayOfWeek.SATURDAY -> java.util.Calendar.SATURDAY
                            else -> java.util.Calendar.MONDAY
                        }

                        val durMin = if (startLt != null && endLt != null)
                            java.time.Duration.between(startLt, endLt).toMinutes().toInt()
                        else 90

                        return SlotLike(
                            dayOfWeek = calDay,
                            startHour = startLt?.hour ?: 19,
                            startMinute = startLt?.minute ?: 0,
                            durationMinutes = durMin
                        )
                    }

                    fun intField(vararg names: String, fallback: Int): Int {
                        for (n in names) {
                            val v = runCatching {
                                val f = cls.getDeclaredField(n).makeAccessibleSafe()
                                (f.get(slot) as? Number)?.toInt()
                            }.getOrNull()
                            if (v != null) return v
                        }
                        return fallback
                    }

                    return SlotLike(
                        dayOfWeek = intField("dayOfWeek", "day", "dow", fallback = java.util.Calendar.MONDAY),
                        startHour = intField("startHour", "hour", "h", fallback = 19),
                        startMinute = intField("startMinute", "minute", "min", "startMin", fallback = 0),
                        durationMinutes = intField("durationMinutes", "duration", "dur", "length", fallback = 90)
                    )
                }

                // הכותרת "מסך הבית" כבר מוצגת ב־KmiTopBar.
                // לכן מסירים את הכותרת הפנימית כדי להרוויח עוד שטח תצוגה.

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF062B4A).copy(alpha = 0.92f),
                                    Color(0xFF0F5E9C).copy(alpha = 0.86f),
                                    Color(0xFF062B4A).copy(alpha = 0.92f)
                                )
                            )
                        )
                        .padding(vertical = 4.dp)
                ) {
                    TrainingsWeekHeader(
                        isEnglish = isEnglish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xCCFFFFFF),
                                    Color(0x66FFFFFF),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(Modifier.height(10.dp))

                fun rollForwardIfPast(src: java.util.Calendar, graceMinutes: Int = 60): java.util.Calendar {
                    val now = System.currentTimeMillis() - graceMinutes * 60_000L
                    val c = src.clone() as java.util.Calendar
                    while (c.timeInMillis <= now) c.add(java.util.Calendar.DAY_OF_YEAR, 7)
                    return c
                }

                fun datesRange(
                    from: java.time.LocalDate,
                    to: java.time.LocalDate
                ): Sequence<java.time.LocalDate> =
                    generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }

                fun java.util.Calendar.toLocalDate(): java.time.LocalDate =
                    this.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()

                val holidayDates: Set<java.time.LocalDate> = remember {
                    runCatching {
                        val input = ctx.assets.open("holidays_hebrew_2024_2026.json")
                        val text = input.bufferedReader().use { it.readText() }

                        val root = org.json.JSONObject(text)
                        val arr = root.getJSONArray("items")

                        val set = mutableSetOf<java.time.LocalDate>()

                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            if (obj.has("date_iso")) {
                                val date = java.time.LocalDate.parse(obj.getString("date_iso"))
                                set.add(date)
                            }
                        }

                        set
                    }.getOrElse { emptySet() }
                }

                fun isBlockedHolidayDate(cal: java.util.Calendar): Boolean {
                    val d = cal.toLocalDate()
                    return holidayDates.contains(d)
                }

                fun upcomingWindowStartMillis(): Long {
                    return java.util.Calendar.getInstance().timeInMillis
                }

                fun upcomingWindowEndMillis(): Long {
                    return java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.DAY_OF_YEAR, 6)
                        set(java.util.Calendar.HOUR_OF_DAY, 23)
                        set(java.util.Calendar.MINUTE, 59)
                        set(java.util.Calendar.SECOND, 59)
                        set(java.util.Calendar.MILLISECOND, 999)
                    }.timeInMillis
                }

                fun isWithinUpcomingSevenDays(cal: java.util.Calendar): Boolean {
                    val start = upcomingWindowStartMillis()
                    val end = upcomingWindowEndMillis()
                    return cal.timeInMillis in start..end
                }

                fun branchScheduleVariants(branch: String): List<String> {
                    val clean = branch
                        .trim()
                        .replace("־", "-")
                        .replace("–", "-")
                        .replace("—", "-")
                        .replace("  ", " ")

                    val pretty = branch
                        .trim()
                        .replace("־", "–")
                        .replace("-", "–")
                        .replace("—", "–")
                        .replace("  ", " ")

                    return listOf(
                        branch.trim(),
                        clean,
                        pretty,
                        clean.replace(" - ", " – "),
                        pretty.replace(" – ", " - ")
                    )
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }

                fun groupScheduleVariants(group: String): List<String> {
                    val clean = group
                        .trim()
                        .replace("־", "-")
                        .replace("–", "-")
                        .replace("—", "-")
                        .replace("  ", " ")

                    return listOf(
                        group.trim(),
                        clean,
                        clean.replace("+", " + "),
                        clean.replace(" + ", "+"),
                        il.kmi.app.training.TrainingCatalog.normalizeGroupName(group).ifBlank { group }
                    )
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }

                fun calendarDayFromDatabase(dayOfWeek: String): Int {
                    return when (dayOfWeek.trim().uppercase(java.util.Locale.US)) {
                        "SUNDAY" -> java.util.Calendar.SUNDAY
                        "MONDAY" -> java.util.Calendar.MONDAY
                        "TUESDAY" -> java.util.Calendar.TUESDAY
                        "WEDNESDAY" -> java.util.Calendar.WEDNESDAY
                        "THURSDAY" -> java.util.Calendar.THURSDAY
                        "FRIDAY" -> java.util.Calendar.FRIDAY
                        "SATURDAY" -> java.util.Calendar.SATURDAY
                        else -> java.util.Calendar.MONDAY
                    }
                }

                fun hourFromTimeText(time: String, fallback: Int): Int {
                    return time
                        .substringBefore(":")
                        .trim()
                        .toIntOrNull()
                        ?: fallback
                }

                fun minuteFromTimeText(time: String, fallback: Int): Int {
                    return time
                        .substringAfter(":", "")
                        .trim()
                        .toIntOrNull()
                        ?: fallback
                }

                fun databaseGroupMatches(
                    selectedGroup: String,
                    databaseGroupHe: String,
                    databaseGroupEn: String
                ): Boolean {
                    val wanted = il.kmi.app.training.TrainingCatalog
                        .normalizeGroupName(selectedGroup)
                        .ifBlank { selectedGroup }
                        .trim()

                    val dbHe = il.kmi.app.training.TrainingCatalog
                        .normalizeGroupName(databaseGroupHe)
                        .ifBlank { databaseGroupHe }
                        .trim()

                    val dbEn = databaseGroupEn.trim()

                    if (wanted.equals(dbHe, ignoreCase = true)) return true
                    if (selectedGroup.trim().equals(databaseGroupHe.trim(), ignoreCase = true)) return true
                    if (selectedGroup.trim().equals(dbEn, ignoreCase = true)) return true

                    // התאמות מרחיבות כמו ב-TrainingCatalog:
                    // מי שבחר נוער או בוגרים יכול לקבל גם "נוער + בוגרים".
                    if (wanted == "נוער" && dbHe == "נוער + בוגרים") return true
                    if (wanted == "בוגרים" && dbHe == "נוער + בוגרים") return true

                    return false
                }

                fun trainingsFromDatabaseForHome(
                    branchName: String,
                    groupName: String,
                    coachFallback: String
                ): List<TrainingData> {
                    val dbBranch = KmiDatabaseProvider.branchByName(ctx, branchName)
                        ?: return emptyList()

                    val matchingDays = dbBranch.trainingDays.filter { day ->
                        databaseGroupMatches(
                            selectedGroup = groupName,
                            databaseGroupHe = day.groupHe,
                            databaseGroupEn = day.groupEn
                        )
                    }

                    if (matchingDays.isEmpty()) return emptyList()

                    return matchingDays.map { day ->
                        TrainingData.nextWeekly(
                            dayOfWeek = calendarDayFromDatabase(day.dayOfWeek),
                            startHour = hourFromTimeText(day.startTime, 19),
                            startMinute = minuteFromTimeText(day.startTime, 0),
                            durationMinutes = day.durationMinutes.takeIf { it > 0 } ?: 90,
                            place = dbBranch.displayPlace(isEnglish),
                            address = dbBranch.displayAddress(isEnglish),
                            coach = day.displayCoachName(isEnglish)
                                .ifBlank { coachFallback }
                                .ifBlank { "איציק ביטון" }
                        )
                    }
                }

                val currentWeekCandidates: List<TrainingData> =
                    remember(branchesEffective, groupsEffective, coachFromPrefs, isEnglish) {

                        val all = mutableListOf<TrainingData>()

                        branchesEffective.forEach { branchName ->
                            val parts = branchName.split('–', '-').map { it.trim() }
                            val city = parts.getOrNull(0) ?: branchName
                            val venue = parts.getOrNull(1) ?: ""

                            groupsEffective.forEach { grp ->

                                // ✅ 1) ניסיון ראשון: branches.json דרך KmiDatabaseProvider
                                val dbItems = trainingsFromDatabaseForHome(
                                    branchName = branchName,
                                    groupName = grp,
                                    coachFallback = coachFromPrefs
                                )

                                if (dbItems.isNotEmpty()) {
                                    val validDbItems = dbItems
                                        .map { it.copy(cal = rollForwardIfPast(it.cal, 60)) }
                                        .filter { isWithinUpcomingSevenDays(it.cal) }

                                    all += validDbItems
                                    return@forEach
                                }

                                // ✅ 2) Fallback זמני: TrainingDirectory הישן
                                val addr =
                                    il.kmi.app.training.TrainingCatalog.addressFor(branchName)
                                        .ifBlank {
                                            if (city.isNotBlank() && venue.isNotBlank()) "$venue, $city" else branchName
                                        }

                                val place = il.kmi.app.training.TrainingCatalog.placeFor(branchName)

                                val branchVariants = branchScheduleVariants(branchName)
                                val groupVariants = groupScheduleVariants(grp)

                                var matchedBranch = ""
                                var matchedGroup = ""

                                val sched = branchVariants
                                    .asSequence()
                                    .flatMap { branchCandidate ->
                                        groupVariants.asSequence().map { groupCandidate ->
                                            branchCandidate to groupCandidate
                                        }
                                    }
                                    .mapNotNull { pair ->
                                        val found = il.kmi.app.training.TrainingDirectory.getSchedule(
                                            pair.first,
                                            pair.second
                                        )

                                        if (found != null) {
                                            matchedBranch = pair.first
                                            matchedGroup = pair.second
                                            found
                                        } else {
                                            null
                                        }
                                    }
                                    .firstOrNull()

                                val coach =
                                    sched?.coachName?.takeIf { it.isNotBlank() }
                                        ?: coachFromPrefs.takeIf { it.isNotBlank() }
                                        ?: "איציק ביטון"

                                val fallbackItems: List<TrainingData> =
                                    sched?.slots?.map { slotAny ->
                                        val s = readSlot(slotAny)
                                        TrainingData.nextWeekly(
                                            dayOfWeek = s.dayOfWeek,
                                            startHour = s.startHour,
                                            startMinute = s.startMinute,
                                            durationMinutes = s.durationMinutes,
                                            place = place,
                                            address = addr,
                                            coach = coach
                                        )
                                    } ?: emptyList()

                                val validFallbackItems = fallbackItems
                                    .map { it.copy(cal = rollForwardIfPast(it.cal, 60)) }
                                    .filter { isWithinUpcomingSevenDays(it.cal) }

                                all += validFallbackItems
                            }
                        }

                        val result = all.distinctBy {
                            buildString {
                                append(it.cal.timeInMillis)
                                append("|")
                                append(it.place.orEmpty())
                                append("|")
                                append(it.address.orEmpty())
                                append("|")
                                append(it.coach.orEmpty())
                            }
                        }
                            .sortedBy { it.cal.timeInMillis }

                        result
                    }

                data class HomeTrainingUi(
                    val training: TrainingData,
                    val isCancelledByHoliday: Boolean
                )

                val upcoming: List<HomeTrainingUi> = remember(currentWeekCandidates) {
                    currentWeekCandidates
                        .sortedBy { it.cal.timeInMillis }
                        .take(4)
                        .map { training ->
                            HomeTrainingUi(
                                training = training,
                                isCancelledByHoliday = isBlockedHolidayDate(training.cal)
                            )
                        }
                }

                val weekBlockedByHoliday = remember(upcoming) {
                    upcoming.isNotEmpty() && upcoming.all { it.isCancelledByHoliday }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp)
                ) {

                    if (upcoming.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (weekBlockedByHoliday) {
                                        if (isEnglish)
                                            "Passover holiday\nNo trainings this week"
                                        else
                                            "חג פסח / חול המועד פסח\nאין אימונים בשבוע זה"
                                    } else {
                                        if (isAbroadBranch) {
                                            if (isEnglish)
                                                "Training schedule is not available for international branches this week"
                                            else
                                                "אין מידע על אימונים לשבוע הקרוב בסניפי חו״ל"
                                        } else {
                                            if (isEnglish) "No upcoming trainings" else "אין אימונים קרובים"
                                        }

                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        items(
                            items = upcoming,
                            key = { item ->
                                buildString {
                                    append(item.training.cal.timeInMillis)
                                    append("|")
                                    append(item.training.place.orEmpty())
                                    append("|")
                                    append(item.training.address.orEmpty())
                                    append("|")
                                    append(item.training.coach.orEmpty())
                                    append("|holiday=")
                                    append(item.isCancelledByHoliday)
                                }
                            }
                        ) { item ->
                            TrainingCardCompact(
                                training = item.training,
                                isEnglish = isEnglish,
                                isCancelledByHoliday = item.isCancelledByHoliday
                            )
                        }
                        item { Spacer(Modifier.height(6.dp)) }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xCCFFFFFF),
                                            Color(0x66FFFFFF),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    // ===== כרטיס הודעות מהמאמן – הודעה אחרונה + דיאלוג הודעות אחרונות =====
                    item {
                        val latestMessage = recentCoachMessages.firstOrNull()
                        val msg = latestMessage?.text?.trim()
                        val extraCount = (recentCoachMessages.size - 1).coerceAtLeast(0)

                        Surface(
                            onClick = {
                                if (recentCoachMessages.isNotEmpty()) {
                                    showCoachMessagesDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.95f),
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFF7DD3FC)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE0F2FE),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF0369A1),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = latestMessage?.coachName
                                                ?.takeIf { it.isNotBlank() }
                                                ?: if (isEnglish) "Coach" else "המאמן",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0C4A6E),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (recentCoachMessages.isNotEmpty()) {
                                            Surface(
                                                onClick = {
                                                    showCoachMessagesDialog = true
                                                },
                                                shape = RoundedCornerShape(999.dp),
                                                color = Color(0xFFE0F2FE),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    Color(0xFF7DD3FC)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Email,
                                                        contentDescription = if (isEnglish) {
                                                            "Recent messages"
                                                        } else {
                                                            "הודעות אחרונות"
                                                        },
                                                        tint = Color(0xFF0369A1),
                                                        modifier = Modifier.size(15.dp)
                                                    )

                                                    Text(
                                                        text = if (isEnglish) {
                                                            "Messages"
                                                        } else {
                                                            "הודעות"
                                                        },
                                                        color = Color(0xFF0369A1),
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    if (msg.isNullOrEmpty()) {
                                        Text(
                                            text = if (isEnglish) {
                                                "No new messages right now"
                                            } else {
                                                "אין הודעות חדשות כרגע"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF64748B)
                                        )
                                    } else {
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1E293B),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val branchGroupLine = buildString {
                                            val b = latestMessage.branch.trim()
                                            val g = latestMessage.group.trim()

                                            if (b.isNotBlank()) {
                                                append(if (isEnglish) "Branch: " else "סניף: ")
                                                append(b)
                                            }

                                            if (g.isNotBlank()) {
                                                if (isNotBlank()) append(" · ")
                                                append(if (isEnglish) "Group: " else "קבוצה: ")
                                                append(g)
                                            }
                                        }

                                        if (branchGroupLine.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = branchGroupLine,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF475569),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    val timeText = latestMessage?.sentAt?.let {
                                        java.text.SimpleDateFormat(
                                            "dd/MM/yyyy · HH:mm",
                                            java.util.Locale("he", "IL")
                                        ).format(it)
                                    }.orEmpty()

                                    val openRecentText = if (extraCount > 0) {
                                        if (isEnglish) {
                                            "Open recent messages · +$extraCount more"
                                        } else {
                                            "פתח הודעות אחרונות"
                                        }
                                    } else {
                                        if (isEnglish) {
                                            "Open recent messages"
                                        } else {
                                            "פתח הודעות אחרונות"
                                        }
                                    }

                                    if (timeText.isNotBlank() || recentCoachMessages.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (timeText.isNotBlank()) {
                                                Text(
                                                    text = timeText,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.2.sp,
                                                        lineHeight = 13.sp
                                                    ),
                                                    color = Color(0xFF64748B),
                                                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            if (recentCoachMessages.isNotEmpty()) {
                                                Spacer(Modifier.height(3.dp))

                                                Text(
                                                    text = openRecentText,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.2.sp,
                                                        lineHeight = 13.sp
                                                    ),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0369A1),
                                                    textAlign = if (isEnglish) TextAlign.Right else TextAlign.Left,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showCoachMessagesDialog = true
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(1.dp))

                var bubbleOffset by remember { mutableStateOf(0f) }

                LaunchedEffect(Unit) {
                    while (true) {
                        animate(
                            initialValue = -120f,
                            targetValue = 320f,
                            animationSpec = tween(2600)
                        ) { value, _ ->
                            bubbleOffset = value
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 1.dp)
                ) {
                    Surface(
                        onClick = {
                            clickSound()
                            haptic(true)
                            onContinue()
                        },
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(12.dp, RoundedCornerShape(18.dp))
                            .graphicsLayer {
                                scaleX = 1.02f
                                scaleY = 1.02f
                            }
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.85f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF7F00FF),
                                            Color(0xFF3F51B5),
                                            Color(0xFF03A9F4)

                                        )
                                    )
                                )
                        ) {

                            Box(
                                modifier = Modifier
                                    .offset(x = bubbleOffset.dp)
                                    .size(140.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = if (isEnglish) "Go to Belt Selection" else "מעבר לבחירת חגורה",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
            }

            val lockSuffix = if (hasFullAccess) "" else " 🔒"

            val quickMenuItems = listOf(
                Triple(
                    (if (isEnglish) "Voice Assistant" else "עוזר קולי") + lockSuffix,
                    Icons.Filled.Mic
                ) {
                    clickSound()
                    haptic(true)
                    fabExpanded = false

                    if (hasFullAccess) {
                        showAiDialog = true
                    } else {
                        onOpenSubscription()
                    }
                },
                Triple(
                    (if (isEnglish) "Monthly Calendar" else "לוח אימונים חודשי") + lockSuffix,
                    Icons.Filled.DateRange
                ) {
                    clickSound()
                    haptic(true)
                    fabExpanded = false
                    if (hasFullAccess) {
                        onOpenMonthlyCalendar()
                    } else {
                        onOpenSubscription()
                    }
                },
                Triple(
                    (if (isEnglish) "Training Summary" else "סיכום אימון") + lockSuffix,
                    Icons.Filled.EditNote
                ) {
                    clickSound()
                    haptic(true)
                    fabExpanded = false
                    if (hasFullAccess) {
                        onOpenTrainingSummary()
                    } else {
                        onOpenSubscription()
                    }
                },
                Triple(
                    (if (isEnglish) "Free Trainings" else "אימונים חופשיים") + lockSuffix,
                    Icons.Filled.Add
                ) {
                    clickSound()
                    haptic(true)
                    fabExpanded = false
                    if (hasFullAccess) {
                        onOpenFreeSessions(
                            freeBranchUi,
                            freeGroupKeyUi,
                            freeUidUi,
                            freeNameUi
                        )
                    } else {
                        onOpenSubscription()
                    }
                }
            )

            if (fabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable { fabExpanded = false }
                )
            }

            AnimatedVisibility(
                visible = fabExpanded,
                modifier = Modifier
                    // ✅ כמו מסך החגורות: התפריט נפתח מהצד, ליד הטאב
                    .align(Alignment.CenterStart)
                    .offset(x = 46.dp, y = 88.dp),
                enter =
                    fadeIn(animationSpec = tween(180)) +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(220)
                            ),
                exit =
                    fadeOut(animationSpec = tween(140)) +
                            scaleOut(
                                targetScale = 0.96f,
                                animationSpec = tween(160)
                            )
            ) {
                HomePremiumQuickMenuPanel(
                    title = if (isEnglish) "Quick Menu" else "תפריט מהיר",
                    isEnglish = isEnglish,
                    items = quickMenuItems,
                    onClose = { fabExpanded = false }
                )
            }

            AnimatedVisibility(
                visible = showFab && !fabExpanded,
                modifier = Modifier
                    // ✅ בדיוק כמו במסך החגורות: צד שמאל פיזי של המסך
                    .align(Alignment.CenterStart)
                    .offset(y = 88.dp),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ModernHomeQuickFab(
                    isEnglish = isEnglish,
                    onClick = {
                        clickSound()
                        haptic(true)
                        fabExpanded = true
                    }
                )
            }

            if (showCoachMessagesDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showCoachMessagesDialog = false
                    },
                    shape = RoundedCornerShape(30.dp),
                    containerColor = Color(0xFFF4F1FB),
                    tonalElevation = 10.dp,
                    title = {
                        Text(
                            text = if (isEnglish) {
                                "Recent coach messages"
                            } else {
                                "הודעות אחרונות מהמאמן"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 430.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (recentCoachMessages.isEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.94f),
                                        shadowElevation = 6.dp,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Text(
                                            text = if (isEnglish) {
                                                "No messages right now."
                                            } else {
                                                "אין הודעות כרגע."
                                            },
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 18.dp)
                                        )
                                    }
                                }
                            } else {
                                items(
                                    items = recentCoachMessages,
                                    key = { message ->
                                        buildString {
                                            append(message.sentAt?.time ?: 0L)
                                            append("|")
                                            append(message.coachName)
                                            append("|")
                                            append(message.text.take(40))
                                        }
                                    }
                                ) { message ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(
                                                elevation = 10.dp,
                                                shape = RoundedCornerShape(22.dp),
                                                clip = false
                                            ),
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.Transparent,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color(0xFFD6E4F0)
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFFF8FCFF),
                                                            Color(0xFFF1F7FB)
                                                        )
                                                    )
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(5.dp)
                                                        .fillMaxHeight()
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    Color(0xFF38BDF8),
                                                                    Color(0xFF7C3AED)
                                                                )
                                                            )
                                                        )
                                                )

                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                                    horizontalAlignment = if (isEnglish) {
                                                        Alignment.Start
                                                    } else {
                                                        Alignment.End
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = if (isEnglish) {
                                                            Arrangement.Start
                                                        } else {
                                                            Arrangement.End
                                                        }
                                                    ) {
                                                        if (isEnglish) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = Color(0xFFE0F2FE),
                                                                border = androidx.compose.foundation.BorderStroke(
                                                                    1.dp,
                                                                    Color(0xFFBAE6FD)
                                                                )
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Person,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF0369A1),
                                                                    modifier = Modifier
                                                                        .size(30.dp)
                                                                        .padding(6.dp)
                                                                )
                                                            }

                                                            Spacer(Modifier.width(8.dp))
                                                        }

                                                        Text(
                                                            text = message.coachName.ifBlank {
                                                                if (isEnglish) "Coach" else "המאמן"
                                                            },
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF0B5E8E),
                                                            fontSize = 17.sp,
                                                            lineHeight = 20.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        if (!isEnglish) {
                                                            Spacer(Modifier.width(8.dp))

                                                            Surface(
                                                                shape = CircleShape,
                                                                color = Color(0xFFE0F2FE),
                                                                border = androidx.compose.foundation.BorderStroke(
                                                                    1.dp,
                                                                    Color(0xFFBAE6FD)
                                                                )
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Person,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF0369A1),
                                                                    modifier = Modifier
                                                                        .size(30.dp)
                                                                        .padding(6.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(Modifier.height(7.dp))

                                                    Text(
                                                        text = message.text,
                                                        color = Color(0xFF1E293B),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        lineHeight = 20.sp,
                                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    val branchGroupLine = buildString {
                                                        val b = message.branch.trim()
                                                        val g = message.group.trim()

                                                        if (b.isNotBlank()) {
                                                            append(if (isEnglish) "Branch: " else "סניף: ")
                                                            append(b)
                                                        }

                                                        if (g.isNotBlank()) {
                                                            if (isNotBlank()) append(" · ")
                                                            append(if (isEnglish) "Group: " else "קבוצה: ")
                                                            append(g)
                                                        }
                                                    }

                                                    if (branchGroupLine.isNotBlank()) {
                                                        Spacer(Modifier.height(8.dp))

                                                        Surface(
                                                            shape = RoundedCornerShape(999.dp),
                                                            color = Color(0xFFEFF6FF),
                                                            border = androidx.compose.foundation.BorderStroke(
                                                                1.dp,
                                                                Color(0xFFBFDBFE)
                                                            )
                                                        ) {
                                                            Text(
                                                                text = branchGroupLine,
                                                                color = Color(0xFF475569),
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 11.5.sp,
                                                                lineHeight = 14.sp,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                                            )
                                                        }
                                                    }

                                                    val timeText = message.sentAt?.let {
                                                        java.text.SimpleDateFormat(
                                                            "dd/MM/yyyy · HH:mm",
                                                            java.util.Locale("he", "IL")
                                                        ).format(it)
                                                    }.orEmpty()

                                                    if (timeText.isNotBlank()) {
                                                        Spacer(Modifier.height(9.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = if (isEnglish) {
                                                                Arrangement.Start
                                                            } else {
                                                                Arrangement.End
                                                            }
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(999.dp),
                                                                color = Color(0xFFF1F5F9)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(
                                                                        horizontal = 9.dp,
                                                                        vertical = 4.dp
                                                                    ),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Filled.DateRange,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFF64748B),
                                                                        modifier = Modifier.size(12.dp)
                                                                    )

                                                                    Text(
                                                                        text = timeText,
                                                                        color = Color(0xFF64748B),
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 11.5.sp,
                                                                        lineHeight = 13.sp,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCoachMessagesDialog = false
                            }
                        ) {
                            Text(
                                text = if (isEnglish) "Close" else "סגור",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF5B21B6)
                            )
                        }
                    }
                )
            }

// ===== דיאלוג תרגיל מהחיפוש =====
            pickedKey?.let { key ->
                val (belt, topic, item) = parseSearchKey(key)

                val displayName = ExerciseTitleFormatter
                    .displayName(item)
                    .ifBlank { item }

                val favoriteId = remember(item) { normalizeFavoriteId(item) }

                val favorites: Set<String> by FavoritesStore
                    .favoritesFlow
                    .collectAsState(initial = emptySet())

                val isFavorite = favorites.contains(favoriteId)

                val noteKey = remember(belt, topic, favoriteId) {
                    "note_${belt.id}_${topic.trim()}_${favoriteId}"
                }

                var noteText by remember(noteKey, notesRefreshKey) {
                    mutableStateOf(notePrefs.getString(noteKey, "").orEmpty())
                }

                var showNoteEditor by rememberSaveable(noteKey) {
                    mutableStateOf(false)
                }

                val explanation = remember(belt, item, topic, isEnglish) {
                    findExplanationForHit(
                        belt = belt,
                        rawItem = item,
                        topic = topic,
                        isEnglish = isEnglish
                    )
                }

                ExerciseExplanationDialog(
                    title = if (isEnglish) {
                        il.kmi.shared.domain.content.ExerciseTitlesEn.getOrSame(displayName)
                    } else {
                        displayName
                    },
                    beltLabel = if (isEnglish) "(${belt.en})" else "(${belt.heb})",
                    explanation = explanation,
                    noteText = noteText,
                    isFavorite = isFavorite,
                    accentColor = belt.color,
                    isEnglish = isEnglish,
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            androidx.compose.ui.graphics.lerp(Color.White, belt.color, 0.12f),
                            androidx.compose.ui.graphics.lerp(Color.White, belt.color, 0.06f),
                            Color.White
                        )
                    ),
                    onDismiss = {
                        clickSound()
                        haptic(true)
                        pickedKey = null
                        showNoteEditor = false
                    },
                    onEditNote = {
                        clickSound()
                        haptic(true)
                        showNoteEditor = true
                    },
                    onDeleteNote = {
                        clickSound()
                        haptic(true)

                        noteText = ""
                        saveHomeExerciseNote(noteKey, "")
                    },
                    onToggleFavorite = {
                        clickSound()
                        haptic(true)
                        FavoritesStore.toggle(favoriteId)
                    }
                )

                if (showNoteEditor) {
                    ExerciseNoteEditorDialog(
                        noteText = noteText,
                        isEnglish = isEnglish,
                        accentColor = belt.color,
                        onNoteChange = { noteText = it },
                        onDismiss = {
                            showNoteEditor = false
                        },
                        onSave = {
                            clickSound()
                            haptic(true)

                            val cleanNote = noteText.trim()
                            noteText = cleanNote

                            saveHomeExerciseNote(noteKey, cleanNote)

                            showNoteEditor = false
                        }
                    )
                }
            }
        }
    }

    // 🔊 דיאלוג העוזר הקולי – מחוץ ל-Box כדי להיות מעל כל המסך
    if (showAiDialog) {
        AiAssistantDialog(
            onDismiss = { showAiDialog = false },
            onOpenDrawer = {
                onOpenDrawer()
            }
        )
    }
}

@Composable
private fun ModernHomeQuickFab(
    isEnglish: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // ✅ הטאב יושב בצד שמאל כמו במסך החגורות:
    // צד שמאל ישר, צד ימין מעוגל.
    val tabShape = RoundedCornerShape(
        topStart = 0.dp,
        bottomStart = 0.dp,
        topEnd = 18.dp,
        bottomEnd = 18.dp
    )

    Box(
        modifier = modifier
            .width(38.dp)
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFE7A3),
                            Color(0xFFFFC247),
                            Color(0xFFFFA928)
                        )
                    ),
                    shape = tabShape
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.72f),
                    shape = tabShape
                )
                .shadow(
                    elevation = 7.dp,
                    shape = tabShape,
                    clip = false
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = if (isEnglish) "Quick menu" else "תפריט מהיר",
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}


/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
@Composable
private fun HomePremiumQuickMenuPanel(
    title: String,
    isEnglish: Boolean,
    items: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>>,
    onClose: () -> Unit
) {
    val panelHeight = 214.dp
    val panelShape = RoundedCornerShape(20.dp)

    Surface(
        shape = panelShape,
        color = Color.White.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFF16A34A).copy(alpha = 0.58f)
        ),
        modifier = Modifier
            .width(190.dp)
            .height(panelHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(panelShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color(0xFFF9FFFB),
                            Color(0xFF16A34A).copy(alpha = 0.12f),
                            Color(0xFFFBFFFC),
                            Color.White.copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF16A34A).copy(alpha = 0.34f),
                    shape = panelShape
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        Text(
                            text = title,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 15.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onClose() }
                        )
                    } else {
                        Text(
                            text = title,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 15.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "סגור",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onClose() }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                items.forEachIndexed { index, item ->
                    HomePremiumQuickMenuRow(
                        text = item.first,
                        icon = item.second,
                        isEnglish = isEnglish,
                        onClick = item.third
                    )

                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = Color(0xFF16A34A).copy(alpha = 0.18f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePremiumQuickMenuRow(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val isLocked = text.endsWith(" 🔒")
    val cleanText = if (isLocked) text.removeSuffix(" 🔒") else text

    val lockPulse = rememberInfiniteTransition(label = "homeQuickMenuLockPulse")

    val lockScale by lockPulse.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeQuickMenuLockScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEnglish) {
            HomePremiumQuickMenuIcon(icon)
            Spacer(Modifier.width(7.dp))

            Text(
                text = cleanText,
                color = Color(0xFF16A34A).copy(alpha = 0.94f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    letterSpacing = (-0.14).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isLocked) {
                Spacer(Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier
                        .size(13.dp)
                        .graphicsLayer {
                            scaleX = lockScale
                            scaleY = lockScale
                            alpha = 1f
                        }
                )
            }
        } else {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier
                        .size(13.dp)
                        .graphicsLayer {
                            scaleX = lockScale
                            scaleY = lockScale
                            alpha = 1f
                        }
                )
                Spacer(Modifier.width(5.dp))
            }

            Text(
                text = cleanText,
                color = Color(0xFF16A34A).copy(alpha = 0.94f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Right,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    letterSpacing = (-0.14).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(7.dp))
            HomePremiumQuickMenuIcon(icon)
        }
    }
}

@Composable
private fun HomePremiumQuickMenuIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color(0xFF16A34A).copy(alpha = 0.10f), CircleShape)
            .border(
                width = 1.dp,
                color = Color(0xFF16A34A).copy(alpha = 0.24f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF16A34A),
            modifier = Modifier.size(10.5.dp)
        )
    }
}

private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String,
    isEnglish: Boolean
): String {
    val display = ExerciseTitleFormatter
        .displayName(rawItem)
        .ifBlank { rawItem }
        .trim()

    val resolved = ExerciseExplanationResolver.get(
        belt = belt,
        topic = topic,
        item = display,
        isEnglish = isEnglish
    ).trim()

    val cleaned = if ("::" in resolved) {
        resolved
            .split("::")
            .map { it.trim() }
            .lastOrNull { it.isNotBlank() }
            ?: resolved
    } else {
        resolved
    }.trim()

    val isFallback = if (isEnglish) {
        cleaned.isBlank() ||
                cleaned.startsWith("Detailed explanation for:") ||
                cleaned.startsWith("There is currently no explanation")
    } else {
        cleaned.isBlank() ||
                cleaned.startsWith("הסבר מפורט על") ||
                cleaned.startsWith("אין כרגע")
    }

    if (!isFallback) {
        return cleaned
    }

    return if (isEnglish) {
        "There is currently no explanation for this exercise."
    } else {
        "אין כרגע הסבר לתרגיל הזה."
    }
}

// ========= עזר: הדגשת "עמידת מוצא ..." עד פסיק/נקודה =========
private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val marker = "עמידת מוצא"

    val idx = source.indexOf(marker)
    if (idx < 0) return AnnotatedString(source)

    val sentenceEndExclusive = run {
        val endIdx = source.indexOfAny(charArrayOf('.', ','), startIndex = idx)
        if (endIdx == -1) source.length else endIdx + 1
    }

    val before = source.substring(0, idx)
    val stanceSentence = source.substring(idx, sentenceEndExclusive)
    val after = source.substring(sentenceEndExclusive)

    return buildAnnotatedString {
        append(before)

        val stanceStart = length
        append(stanceSentence)
        val stanceEnd = length

        addStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = stanceColor
            ),
            start = stanceStart,
            end = stanceEnd
        )

        append(after)
    }
}

/** כרטיס אימון קומפקטי – כמו לפני השינוי, עם סדר אייקונים ישן */
@Composable
private fun TrainingCardCompact(
    training: TrainingData,
    isEnglish: Boolean,
    isCancelledByHoliday: Boolean = false
) {
    val ctx = LocalContext.current
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    var showNavPicker by rememberSaveable(training.cal.timeInMillis) { mutableStateOf(false) }

    val navPref: NavAppPref = remember(training.cal.timeInMillis) {
        readNavPref(ctx)
    }

    fun onNavigateClick() {
        clickSound()
        haptic(true)

        val safeAddress = training.address?.trim().orEmpty()
        if (safeAddress.isBlank()) return

        when (navPref) {
            NavAppPref.ASK -> showNavPicker = true
            NavAppPref.GOOGLE_MAPS -> openGoogleMaps(ctx, safeAddress)
            NavAppPref.WAZE -> openWaze(ctx, safeAddress)
        }
    }

    if (showNavPicker) {
        AlertDialog(
            onDismissRequest = { showNavPicker = false },
            title = {
                Text(
                    text = "פתיחה באמצעות",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "בחר אפליקציה לניווט. אפשר גם לשמור כברירת מחדל.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    var rememberChoice by rememberSaveable(training.cal.timeInMillis) { mutableStateOf(true) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("זכור בחירה", modifier = Modifier.padding(end = 8.dp))
                        Switch(
                            checked = rememberChoice,
                            onCheckedChange = { rememberChoice = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val safeAddress = training.address?.trim().orEmpty()
                                if (safeAddress.isNotBlank()) {
                                    if (rememberChoice) writeNavPref(ctx, NavAppPref.WAZE)
                                    openWaze(ctx, safeAddress)
                                }
                                showNavPicker = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("וייז", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val safeAddress = training.address?.trim().orEmpty()
                                if (safeAddress.isNotBlank()) {
                                    if (rememberChoice) writeNavPref(ctx, NavAppPref.GOOGLE_MAPS)
                                    openGoogleMaps(ctx, safeAddress)
                                }
                                showNavPicker = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("גוגל מפות", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = {
                            writeNavPref(ctx, NavAppPref.ASK)
                            showNavPicker = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("כל פעם לשאול")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNavPicker = false }) { Text("סגור") }
            }
        )
    }

    val durationMin: Int = remember(training) {
        fun readIntField(vararg names: String, fallback: Int): Int {
            val cls = training::class.java
            for (n in names) {
                val v = runCatching {
                    val f = cls.getDeclaredField(n).apply { isAccessible = true }
                    (f.get(training) as? Number)?.toInt()
                }.getOrNull()
                if (v != null) return v
            }
            return fallback
        }
        readIntField("durationMinutes", "durationMinuets", "duration", "dur", fallback = 90)
    }

    val locale = if (isEnglish) {
        java.util.Locale.ENGLISH
    } else {
        java.util.Locale("he", "IL")
    }

    val dayText = remember(training.cal.timeInMillis, isEnglish) {
        java.text.SimpleDateFormat("EEEE", locale).format(training.cal.time)
    }
    val dateText = remember(training.cal.timeInMillis, isEnglish) {
        java.text.SimpleDateFormat("dd/MM", locale).format(training.cal.time)
    }
    val timeText = remember(training.cal.timeInMillis, durationMin) {
        val fmt = java.text.SimpleDateFormat("HH:mm", locale)
        val start = fmt.format(training.cal.time)
        val end   = fmt.format(java.util.Date(training.cal.timeInMillis + durationMin * 60_000L))
        "$start – $end"
    }
    val dateTimeText = remember(dayText, dateText, timeText, isEnglish) {
        if (isEnglish) {
            "$dayText $dateText · $timeText"
        } else {
            "$dayText $dateText · $timeText"
        }
    }

    @Composable
    fun MapChip(
        label: String,
        icon: @Composable () -> Unit,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            onClick = { onClick() },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = modifier.height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                icon()
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 78.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val branchLine = remember(training.place, training.address, isEnglish) {
                val displaySource = training.place
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: training.address.orEmpty()

                il.kmi.app.training.TrainingCatalog.placeDisplayName(
                    displaySource,
                    isEnglish
                )
            }

            Text(
                text = branchLine,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,    // גודל הכותרת של שם המקום
                    lineHeight = 19.sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = dateTimeText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.4.sp,
                    lineHeight = 15.sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (isCancelledByHoliday) {
                Spacer(Modifier.height(4.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF7ED),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFF97316).copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Training cancelled due to holiday"
                        } else {
                            "האימון מבוטל עקב חג"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationChip(
                    address = il.kmi.app.training.TrainingCatalog.addressDisplayName(
                        training.address.orEmpty(),
                        isEnglish
                    ),
                    isEnglish = isEnglish,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
// ===============================
// ✅ ניווט: כפתור אחד + דיאלוג בחירה יפה
// ===============================

private enum class NavChoice { GOOGLE_MAPS, WAZE }

@Composable
private fun NavigationChip(
    address: String?,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    var showPicker by rememberSaveable(address) { mutableStateOf(false) }
    var rememberChoice by rememberSaveable(address) { mutableStateOf(true) }

    val safeAddress = address?.trim().orEmpty()

    fun open(choice: NavChoice) {
        if (safeAddress.isBlank()) return
        when (choice) {
            NavChoice.GOOGLE_MAPS -> openGoogleMaps(ctx, safeAddress)
            NavChoice.WAZE -> openWaze(ctx, safeAddress)
        }
    }

    Surface(
        onClick = {
            clickSound()
            haptic(true)
            if (safeAddress.isNotBlank()) showPicker = true
        },
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Black.copy(alpha = 0.06f)
        ),
        modifier = modifier
            .heightIn(min = 62.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // אייקון
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_maps),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isEnglish) "Navigate" else "ניווט",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    // ✅ הכרטיס לבן גם במצב כהה, לכן צבע קבוע וכהה
                    color = Color(0xFF0B1220),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (safeAddress.isBlank()) {
                        if (isEnglish) "No address" else "אין כתובת"
                    } else {
                        safeAddress
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.4.sp,
                        lineHeight = 13.8.sp
                    ),
                    // ✅ לא להשתמש כאן ב-onSurfaceVariant,
                    // כי במצב כהה הוא יוצא בהיר מדי על כרטיס לבן.
                    color = Color(0xFF475569),
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.Person, // אם אתה רוצה חץ במקום, תגיד ואחליף
                contentDescription = null,
                tint = Color(0xFF2563EB).copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showPicker) {
        NavPickerDialog(
            address = safeAddress,
            rememberChoice = rememberChoice,
            onRememberChoiceChange = { rememberChoice = it },
            onPick = { picked ->
                // אם בעתיד תרצה לשמור העדפה ב-SP — אפשר כאן.
                open(picked)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun NavPickerDialog(
    address: String,
    rememberChoice: Boolean,
    onRememberChoiceChange: (Boolean) -> Unit,
    onPick: (NavChoice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = "ניווט באמצעות",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "זכור בחירה",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        androidx.compose.material3.Switch(
                            checked = rememberChoice,
                            onCheckedChange = onRememberChoiceChange
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    val btnHeight = 52.dp

                    // Waze
                    Surface(
                        onClick = { onPick(NavChoice.WAZE) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.Black.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(btnHeight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_waze),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Waze",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // Google Maps
                    Surface(
                        onClick = { onPick(NavChoice.GOOGLE_MAPS) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.Black.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(btnHeight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_maps),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "גוגל מפות",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = "אפשר לשמור בחירה כברירת מחדל (בהמשך נחבר את זה להגדרות).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("סגור") }
        }
    )
}

// ===== העדפת ניווט (Google Maps / Waze / Ask) =====

private enum class NavAppPref { ASK, GOOGLE_MAPS, WAZE }

private const val NAV_PREFS_FILE = "kmi_user"
private const val NAV_PREF_KEY = "nav_app_pref"

private fun readNavPref(ctx: Context): NavAppPref {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)
    return when (sp.getString(NAV_PREF_KEY, "ask")?.lowercase()) {
        "gmaps", "google", "google_maps" -> NavAppPref.GOOGLE_MAPS
        "waze" -> NavAppPref.WAZE
        else -> NavAppPref.ASK
    }
}

private fun writeNavPref(ctx: Context, pref: NavAppPref) {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)
    val v = when (pref) {
        NavAppPref.ASK -> "ask"
        NavAppPref.GOOGLE_MAPS -> "gmaps"
        NavAppPref.WAZE -> "waze"
    }
    sp.edit().putString(NAV_PREF_KEY, v).apply()
}

// ===== עזרי ניווט מפות =====
private fun openGoogleMaps(ctx: Context, address: String?) {
    val safeAddress = address?.trim().orEmpty()
    if (safeAddress.isEmpty()) return

    val mapsPkg = "com.google.android.apps.maps"

    // ✅ אם Maps מותקן – לפתוח ישר
    if (isPackageInstalled(ctx, mapsPkg)) {
        val navUri = Uri.parse("google.navigation:q=" + Uri.encode(safeAddress))
        val i = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage(mapsPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(i) }
        return
    }

    // ❗ Maps לא מותקן → לפתוח Play Store בלי chooser של חנויות
    openPlayStoreDirect(ctx, mapsPkg)
}

private fun isPackageInstalled(ctx: Context, pkg: String): Boolean {
    return runCatching {
        ctx.packageManager.getPackageInfo(pkg, 0)
        true
    }.getOrElse { false }
}

private fun openPlayStoreDirect(ctx: Context, pkg: String) {
    val playPkg = "com.android.vending" // Google Play

    // ניסיון 1: לפתוח Play Store ישירות (בלי לשאול Galaxy Store)
    val marketUri = Uri.parse("market://details?id=$pkg")
    val playIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
        setPackage(playPkg)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (playIntent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(playIntent)
        return
    }

    // ניסיון 2: fallback לדפדפן (אם אין Play Store)
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
    ctx.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun clearNavPref(ctx: Context) {
    val sp = ctx.getSharedPreferences(NAV_PREFS_FILE, Context.MODE_PRIVATE)
    sp.edit().remove(NAV_PREF_KEY).apply()   // או putString(NAV_PREF_KEY, "ask")
}

private fun openWaze(ctx: Context, address: String?) {
    val safeAddress = address?.trim().orEmpty()
    if (safeAddress.isEmpty()) return

    val wazeUri = Uri.parse("https://waze.com/ul?q=" + Uri.encode(safeAddress))
    val intent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
        setPackage("com.waze")
    }

    if (intent.resolveActivity(ctx.packageManager) != null) {
        ctx.startActivity(intent)
    } else {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
    }
}
