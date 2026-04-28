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
import androidx.compose.ui.BiasAbsoluteAlignment
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import il.kmi.app.favorites.FavoritesStore
import android.app.Activity
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.subscription.KmiAccess

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

    val backgroundBrush = remember(isCoach) {
        if (isCoach) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF141E30),
                    Color(0xFF243B55),
                    Color(0xFF0EA5E9)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFF7F00FF), Color(0xFF3F51B5), Color(0xFF03A9F4)),
                start = Offset(0f, 0f),
                end = Offset(1000f, 3000f)
            )
        }
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

                        android.util.Log.e(
                            "KMI_ACCESS_MODE",
                            "HOME access pref changed source=${if (changedSp === userSp) "kmi_user" else if (changedSp === subsSp) "kmi_subs" else "kmi_prefs"} " +
                                    "key=$key tick=$homeAccessRefreshTick"
                        )
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
                        getBoolean("is_subscribed", false) ||
                        !getString("sub_product", "").isNullOrBlank()
            }

            val hasFullAccess = remember(homeAccessRefreshTick) {
                KmiAccess.isAdmin(userSp) ||
                        userSp.hasActiveSubscriptionAccess() ||
                        subsSp.hasActiveSubscriptionAccess() ||
                        legacySp.hasActiveSubscriptionAccess()
            }

            LaunchedEffect(hasFullAccess, homeAccessRefreshTick) {
                android.util.Log.e(
                    "KMI_ACCESS_MODE",
                    "HOME hasFullAccess=$hasFullAccess tick=$homeAccessRefreshTick " +
                            "isAdmin=${KmiAccess.isAdmin(userSp)} " +
                            "user_full=${userSp.getBoolean("has_full_access", false)} " +
                            "subs_full=${subsSp.getBoolean("has_full_access", false)} " +
                            "legacy_full=${legacySp.getBoolean("has_full_access", false)} " +
                            "user_active=${userSp.getBoolean("subscription_active", false)} " +
                            "subs_active=${subsSp.getBoolean("subscription_active", false)} " +
                            "legacy_active=${legacySp.getBoolean("subscription_active", false)} " +
                            "user_verified=${userSp.getBoolean("google_subscription_verified", false)} " +
                            "subs_verified=${subsSp.getBoolean("google_subscription_verified", false)} " +
                            "legacy_verified=${legacySp.getBoolean("google_subscription_verified", false)} " +
                            "user_product=${userSp.getString("sub_product", "")} " +
                            "subs_product=${subsSp.getString("sub_product", "")} " +
                            "legacy_product=${legacySp.getString("sub_product", "")}"
                )
            }

            val showFab by remember(listState) {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 ||
                            listState.firstVisibleItemScrollOffset > 120
                }
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
                                android.util.Log.e(
                                    "KMI_HOME_GROUPS",
                                    "groups pref changed key=$key tick=$groupsRefreshTick " +
                                            "groups_json=${userSp.getString("groups_json", "")} " +
                                            "selected_groups=${userSp.getString("selected_groups", "")} " +
                                            "groups=${userSp.getString("groups", "")} " +
                                            "age_groups=${userSp.getString("age_groups", "")} " +
                                            "age_group=${userSp.getString("age_group", "")} " +
                                            "group=${userSp.getString("group", "")}"
                                )
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

                LaunchedEffect(groupsEffective, groupsRefreshTick) {
                    android.util.Log.e(
                        "KMI_HOME_GROUPS",
                        "resolved groupsEffective=$groupsEffective tick=$groupsRefreshTick"
                    )
                }
                // === KMI_MULTI_GROUPS (FIX) ===

                // =========================
                // ⭐ הודעות מהמאמן – Firestore (לפי UID)
                // =========================
                val currentUid = remember {
                    FirebaseAuth.getInstance().currentUser?.uid
                }

                var lastCoachMessage by remember { mutableStateOf<String?>(null) }
                var lastCoachFrom by remember { mutableStateOf<String?>(null) }
                var lastCoachSentAt by remember { mutableStateOf<java.util.Date?>(null) }

                DisposableEffect(currentUid) {
                    if (currentUid == null) {
                        lastCoachMessage = null
                        lastCoachFrom = null
                        lastCoachSentAt = null
                        onDispose { }
                    } else {
                        val db = FirebaseFirestore.getInstance()
                        val query = db.collection("coachBroadcasts")
                            .whereArrayContains("targetUids", currentUid)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(1)

                        val reg = query.addSnapshotListener { snap, e ->
                            if (e != null) {
                                // אם יש שגיאה – לא מוחקים את ההודעה האחרונה שכבר מוצגת
                                return@addSnapshotListener
                            }
                            if (snap != null && !snap.isEmpty) {
                                val doc = snap.documents.first()
                                lastCoachMessage = doc.getString("text")
                                lastCoachFrom = doc.getString("coachName")
                                    ?: doc.getString("coach_name")
                                lastCoachSentAt = doc.getTimestamp("createdAt")?.toDate()
                            } else {
                                lastCoachMessage = null
                                lastCoachFrom = null
                                lastCoachSentAt = null
                            }
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

                            android.util.Log.e(
                                "KMI_HOME_HYDRATE",
                                "remoteBranches=$remoteBranches remoteGroups=$remoteGroups " +
                                        "activeBranch=$remoteActiveBranch activeGroup=$remoteActiveGroup"
                            )

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
                        .addOnFailureListener { e ->
                            android.util.Log.e(
                                "KMI_HOME_HYDRATE",
                                "failed to hydrate home profile from Firestore",
                                e
                            )
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

                            android.util.Log.e(
                                "KMI_HOME_BRANCHES",
                                "branches pref changed key=$key tick=$branchesRefreshTick " +
                                        "branches_json=${userSp.getString("branches_json", "")} " +
                                        "selected_branches=${userSp.getString("selected_branches", "")} " +
                                        "branches=${userSp.getString("branches", "")} " +
                                        "branch=${userSp.getString("branch", "")} " +
                                        "branch2=${userSp.getString("branch2", "")} " +
                                        "branch3=${userSp.getString("branch3", "")} " +
                                        "branch_type=${userSp.getString("branch_type", "")}"
                            )
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

                LaunchedEffect(branchesEffective, branchTypeHome, branchesRefreshTick) {
                    android.util.Log.e(
                        "KMI_HOME_BRANCHES",
                        "resolved branchesEffective=$branchesEffective " +
                                "selectedBranches=$selectedBranches " +
                                "branchTypeHome=$branchTypeHome tick=$branchesRefreshTick " +
                                "branches_json=${userSp.getString("branches_json", "")} " +
                                "selected_branches=${userSp.getString("selected_branches", "")} " +
                                "branches=${userSp.getString("branches", "")} " +
                                "branch=${userSp.getString("branch", "")}"
                    )
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

                if (isCoach) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        TrainingsWeekHeader(
                            isEnglish = isEnglish,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF7F00FF), Color(0xFF3F51B5))
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

                val nowCal = remember {
                    java.util.Calendar.getInstance()
                }

                val weekEndCal = remember {
                    (java.util.Calendar.getInstance()).apply {
                        add(java.util.Calendar.DAY_OF_YEAR, 6)
                        set(java.util.Calendar.HOUR_OF_DAY, 23)
                        set(java.util.Calendar.MINUTE, 59)
                        set(java.util.Calendar.SECOND, 59)
                        set(java.util.Calendar.MILLISECOND, 999)
                    }
                }

                fun isWithinCurrentWeek(cal: java.util.Calendar): Boolean {
                    return cal.timeInMillis in nowCal.timeInMillis..weekEndCal.timeInMillis
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

                val currentWeekCandidates: List<TrainingData> =
                    remember(branchesEffective, groupsEffective, coachFromPrefs) {
                        android.util.Log.e(
                            "KMI_HOME_SCHEDULE",
                            "START branchesEffective=$branchesEffective groupsEffective=$groupsEffective"
                        )

                        val all = mutableListOf<TrainingData>()
                        branchesEffective.forEach { branchName ->
                            val parts = branchName.split('–', '-').map { it.trim() }
                            val city = parts.getOrNull(0) ?: branchName
                            val venue = parts.getOrNull(1) ?: ""

                            val addr =
                                il.kmi.app.training.TrainingCatalog.addressFor(branchName) ?: ""
                                    .ifBlank {
                                        if (city.isNotBlank() && venue.isNotBlank()) "$venue, $city" else branchName
                                    }
                            val place = il.kmi.app.training.TrainingCatalog.placeFor(branchName)

                            groupsEffective.forEach { grp ->

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

                                android.util.Log.e(
                                    "KMI_HOME_SCHEDULE",
                                    "schedule lookup originalBranch='$branchName' originalGroup='$grp' " +
                                            "matchedBranch='$matchedBranch' matchedGroup='$matchedGroup' " +
                                            "found=${sched != null}"
                                )

                                val coach =
                                    sched?.coachName?.takeIf { it.isNotBlank() }
                                        ?: coachFromPrefs.takeIf { it.isNotBlank() }
                                        ?: "איציק ביטון"

                                val branchItems: List<TrainingData> =
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

                                val validItems = branchItems
                                    .map { it.copy(cal = rollForwardIfPast(it.cal, 60)) }
                                    .filter { isWithinCurrentWeek(it.cal) }

                                android.util.Log.e(
                                    "KMI_HOME_SCHEDULE",
                                    "items branch='$branchName' group='$grp' count=${validItems.size}"
                                )

                                all += validItems
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

                        android.util.Log.e(
                            "KMI_HOME_SCHEDULE",
                            "FINAL candidates=${result.size} " +
                                    result.joinToString(" || ") {
                                        "${it.place} | ${it.address} | ${it.cal.time}"
                                    }
                        )

                        result
                    }

                val blockedWeekTrainings = remember(currentWeekCandidates) {
                    currentWeekCandidates.filter { isBlockedHolidayDate(it.cal) }
                }

                val upcoming: List<TrainingData> = remember(currentWeekCandidates) {
                    currentWeekCandidates
                        .filterNot { isBlockedHolidayDate(it.cal) }
                        .take(4)
                }

                val weekBlockedByHoliday = remember(currentWeekCandidates, blockedWeekTrainings) {
                    currentWeekCandidates.isNotEmpty() &&
                            blockedWeekTrainings.isNotEmpty() &&
                            blockedWeekTrainings.size == currentWeekCandidates.size
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp)
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
                            key = { training ->
                                buildString {
                                    append(training.cal.timeInMillis)
                                    append("|")
                                    append(training.place.orEmpty())
                                    append("|")
                                    append(training.address.orEmpty())
                                    append("|")
                                    append(training.coach.orEmpty())
                                }
                            }
                        ) { training ->
                            TrainingCardCompact(
                                training = training,
                                isEnglish = isEnglish
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

                    // ===== כרטיס הודעות מהמאמן – מעודכן להציג הודעה אחרונה =====
                    item {

                        val msg = lastCoachMessage?.trim()

                        Surface(
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
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // 👤 אייקון מאמן
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE0F2FE),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF0369A1),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {

                                    // שם המאמן
                                    Text(
                                        text = lastCoachFrom?.takeIf { it.isNotBlank() } ?: "המאמן",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0C4A6E),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    if (msg.isNullOrEmpty()) {

                                        Text(
                                            text = if (isEnglish)
                                                "No new messages right now"
                                            else
                                                "אין הודעות חדשות כרגע",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF64748B)
                                        )

                                    } else {

                                        val shortMsg = if (msg.length > 140)
                                            msg.take(140) + "..."
                                        else msg

                                        Text(
                                            text = shortMsg,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1E293B)
                                        )
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    val timeText = lastCoachSentAt?.let {
                                        java.text.SimpleDateFormat(
                                            "dd/MM/yyyy HH:mm",
                                            java.util.Locale("he", "IL")
                                        ).format(it)
                                    } ?: ""

                                    if (timeText.isNotBlank()) {
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

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
                        .padding(horizontal = 12.dp, vertical = 4.dp)
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
                        .background(Color.Black.copy(alpha = 0.24f))
                        .clickable { fabExpanded = false }
                )
            }

            AnimatedVisibility(
                visible = fabExpanded,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 138.dp),
                enter =
                    fadeIn(animationSpec = tween(180)) +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = tween(220)
                            ) +
                            androidx.compose.animation.slideInVertically(
                                initialOffsetY = { it / 8 },
                                animationSpec = tween(220)
                            ),
                exit =
                    fadeOut(animationSpec = tween(140)) +
                            scaleOut(
                                targetScale = 0.96f,
                                animationSpec = tween(160)
                            ) +
                            androidx.compose.animation.slideOutVertically(
                                targetOffsetY = { it / 10 },
                                animationSpec = tween(160)
                            )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (isEnglish) 16.dp else 0.dp,
                            end = if (isEnglish) 0.dp else 16.dp
                        ),
                    contentAlignment =
                        if (isEnglish) BiasAbsoluteAlignment(-1f, 1f)
                        else BiasAbsoluteAlignment(1f, 1f)
                ) {
                    HomePremiumQuickMenuPanel(
                        title = if (isEnglish) "Quick Menu" else "תפריט מהיר",
                        isEnglish = isEnglish,
                        items = quickMenuItems,
                        onClose = { fabExpanded = false }
                    )
                }
            }

            AnimatedVisibility(
                visible = showFab && !fabExpanded,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 72.dp),
                    horizontalArrangement =
                        if (isEnglish) Arrangement.Absolute.Right
                        else Arrangement.Absolute.Left
                ) {
                    ModernHomeQuickFab(
                        onClick = {
                            clickSound()
                            haptic(true)
                            fabExpanded = true
                        }
                    )
                }
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

                var noteText by remember(noteKey) {
                    mutableStateOf(notePrefs.getString(noteKey, "").orEmpty())
                }
                var showNoteEditor by remember { mutableStateOf(false) }

                val explanation = remember(belt, item) {
                    findExplanationForHit(
                        belt = belt,
                        rawItem = item,
                        topic = topic
                    )
                }

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
                                    text = "${topic} • ${belt.heb}",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            IconButton(
                                onClick = {
                                    clickSound()
                                    haptic(true)
                                    showNoteEditor = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "הערה",
                                    tint = Color(0xFF42A5F5)
                                )
                            }

                            IconButton(
                                onClick = {
                                    clickSound()
                                    haptic(true)
                                    FavoritesStore.toggle(favoriteId)
                                },
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "מועדפים",
                                    tint = if (isFavorite) Color(0xFFFFC107) else Color.Gray
                                )
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            val annotated = buildExplanationWithStanceHighlight(
                                source = explanation,
                                stanceColor = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = annotated,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Black
                            )

                            if (noteText.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))

                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.LightGray.copy(alpha = 0.65f)
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = "הערה של המתאמן:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = noteText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.Black
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                clickSound()
                                haptic(true)
                                pickedKey = null
                            }
                        ) {
                            Text("סגור")
                        }
                    }
                )

                if (showNoteEditor) {
                    AlertDialog(
                        onDismissRequest = { showNoteEditor = false },
                        title = {
                            Text(
                                text = "הערה לתרגיל",
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                label = {
                                    Text("כתוב הערה")
                                }
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    notePrefs.edit()
                                        .putString(noteKey, noteText.trim())
                                        .apply()
                                    showNoteEditor = false
                                }
                            ) {
                                Text("שמור")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showNoteEditor = false }
                            ) {
                                Text("ביטול")
                            }
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF16A34A).copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    ),
                    shape = shape
                )
        )

        Surface(
            onClick = onClick,
            shape = shape,
            color = Color(0xFF16A34A),
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                Color.White.copy(alpha = 0.92f)
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Quick Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
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
    val panelShape = RoundedCornerShape(22.dp)

    Surface(
        shape = panelShape,
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 16.dp,
        modifier = Modifier.width(270.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF16A34A).copy(alpha = 0.24f),
                            Color(0xFFFDFDFE),
                            Color(0xFFF7FAF8),
                            Color(0xFF16A34A).copy(alpha = 0.14f)
                        )
                    ),
                    shape = panelShape
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF16A34A).copy(alpha = 0.34f),
                    shape = panelShape
                )
                .padding(horizontal = 10.dp, vertical = 10.dp)
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
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(15.dp)
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
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "סגור",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onClose() }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEnglish) {
            HomePremiumQuickMenuIcon(icon)
            Spacer(Modifier.width(10.dp))

            Text(
                text = cleanText,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isLocked) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Text(
                text = cleanText,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Right,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(10.dp))
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
            .size(24.dp)
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
            modifier = Modifier.size(14.dp)
        )
    }
}

private fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String
): String {
    val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

    fun String.clean(): String = this
        .replace('–', '-')
        .replace('־', '-')
        .replace("  ", " ")
        .trim()

    val candidates = buildList {
        add(rawItem)
        add(display)
        add(display.clean())
        add(display.substringBefore("(").trim().clean())
    }.distinct()

    for (candidate in candidates) {
        val got = il.kmi.app.domain.Explanations.get(belt, candidate).trim()
        if (got.isNotBlank()
            && !got.startsWith("הסבר מפורט על")
            && !got.startsWith("אין כרגע")
        ) {
            return if ("::" in got) got.substringAfter("::").trim() else got
        }
    }

    return "אין כרגע הסבר לתרגיל הזה."
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
    isEnglish: Boolean
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
        java.text.SimpleDateFormat("dd/MM/yyyy", locale).format(training.cal.time)
    }
    val timeText = remember(training.cal.timeInMillis, durationMin) {
        val fmt = java.text.SimpleDateFormat("HH:mm", locale)
        val start = fmt.format(training.cal.time)
        val end   = fmt.format(java.util.Date(training.cal.timeInMillis + durationMin * 60_000L))
        "$start – $end"
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
            .heightIn(min = 68.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

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
            .heightIn(min = 54.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.Person, // אם אתה רוצה חץ במקום, תגיד ואחליף
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
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
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
