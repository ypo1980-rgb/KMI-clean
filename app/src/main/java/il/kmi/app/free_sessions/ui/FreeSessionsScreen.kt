package il.kmi.app.free_sessions.ui

import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.ui.KmiTopBar
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.free_sessions.data.FreeSessionsRepository
import il.kmi.shared.free_sessions.data.freeSessionsRepository
import il.kmi.shared.free_sessions.model.FreeSession
import il.kmi.shared.free_sessions.model.FreeSessionPart
import il.kmi.shared.free_sessions.model.ParticipantState
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import il.kmi.app.R


//========================================================================

private const val FREE_SESSIONS_DEBUG = "KMI_FREE_SESSIONS"

private val KmiFreeBgTop = Color(0xFFF8FBFF)
private val KmiFreeBgMid = Color(0xFFEAF4FF)
private val KmiFreeBgBottom = Color(0xFF0EA5D7)
private val KmiFreeCardColor = Color(0xFFF7FBFF)
private val KmiFreeCardColorSoft = Color(0xFFFFFFFF)
private val KmiFreeBorderColor = Color(0xFFBFD7EF)
private val KmiFreeBorderColorStrong = Color(0xFF0EA5D7)
private val KmiFreeTitleColor = Color(0xFF0F172A)
private val KmiFreeTextColor = Color(0xFF111827)
private val KmiFreeSubTextColor = Color(0xFF64748B)

private data class FreeSessionPlaceSuggestion(
    val name: String,
    val address: String,
    val lat: Double?,
    val lng: Double?,
    val placeId: String? = null
)

private data class FreeSessionBranchUser(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val mergeKey: String
)

private fun readFreeSessionPrefsList(
    sp: SharedPreferences,
    vararg keys: String
): List<String> {
    val out = mutableListOf<String>()

    keys.forEach { key ->
        when (val value = sp.all[key]) {
            is String -> {
                val raw = value.trim()

                if (raw.isBlank()) {
                    // no-op
                } else if (raw.startsWith("[")) {
                    runCatching {
                        val arr = org.json.JSONArray(raw)
                        for (i in 0 until arr.length()) {
                            arr.optString(i)
                                .trim()
                                .takeIf { it.isNotBlank() }
                                ?.let { out += it }
                        }
                    }
                } else {
                    raw.split(',', ';', '|', '\n')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { out += it }
                }
            }

            is Set<*> -> {
                value
                    .mapNotNull { it?.toString()?.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { out += it }
            }
        }
    }

    return out
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun normalizeFreeSessionText(raw: String): String {
    return raw.trim()
        .replace('־', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale("he", "IL"))
}

private fun fallbackGroupsForFreeSessionBranch(
    allGroups: List<String>
): List<String> {
    return allGroups
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun splitFreeSessionCsv(raw: String): List<String> {
    return raw
        .split(',', ';', '|', '\n')
        .map { it.trim().trim('"') }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun sanitizeFreeSessionGroupsForBranch(
    branch: String,
    groups: List<String>
): List<String> {
    // לא מבצעים כאן שום תיקון קשיח לפי שם סניף.
    // הקבוצות חייבות להגיע ממיפוי אמיתי מהשרת:
    // branchGroups / groupsByBranch / branchToGroups
    // או ממסמך מתאמן שבו יש שיוך חד-משמעי של סניף וקבוצה.
    return groups
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun readFreeSessionCoachUid(sp: SharedPreferences): String {
    return listOf(
        sp.getString("coachUid", null),
        sp.getString("coach_uid", null),
        sp.getString("trainerUid", null),
        sp.getString("trainer_uid", null),
        sp.getString("instructorUid", null),
        sp.getString("instructor_uid", null)
    )
        .map { it.orEmpty().trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeSessionsScreen(
    branch: String,
    groupKey: String,
    currentUid: String,
    currentName: String,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repo: FreeSessionsRepository = remember { freeSessionsRepository() }
    val vm = remember { FreeSessionsViewModel(repo) }
    val ctx = LocalContext.current   // ✅ חובה לטוסטים

    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val screenDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    val screenTextAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val screenHorizontalEnd = if (isEnglish) Alignment.Start else Alignment.End

    val userSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    val availableBranches = remember(userSp, branch) {
        (
                readFreeSessionPrefsList(
                    userSp,
                    "active_branch",
                    "branch",
                    "branches",
                    "branches_json",
                    "selected_branches",
                    "branch2",
                    "branch3"
                ) + listOf(branch)
                )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val availableGroups = remember(userSp, groupKey) {
        (
                readFreeSessionPrefsList(
                    userSp,
                    "active_group",
                    "group",
                    "groups",
                    "groups_json",
                    "selected_groups",
                    "age_group",
                    "age_groups"
                ) + listOf(groupKey)
                )
            .map {
                il.kmi.app.training.TrainingCatalog
                    .normalizeGroupName(it)
                    .ifBlank { it }
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    var selectedBranch by rememberSaveable(availableBranches.joinToString("|")) {
        mutableStateOf(
            branch.takeIf { it.isNotBlank() }
                ?: availableBranches.firstOrNull()
                ?: ""
        )
    }

    var serverGroupsByBranch by remember {
        mutableStateOf<Map<String, List<String>>>(emptyMap())
    }

    var branchGroupsLoading by remember {
        mutableStateOf(false)
    }

    var branchGroupsLoadedOnce by remember {
        mutableStateOf(false)
    }

    var resolvedCoachUid by remember {
        mutableStateOf(readFreeSessionCoachUid(userSp))
    }

    fun normalizedBranchKey(raw: String): String {
        return normalizeFreeSessionText(raw)
    }

    LaunchedEffect(currentUid, availableBranches.joinToString("|")) {
        if (currentUid.isBlank()) {
            serverGroupsByBranch = emptyMap()
            branchGroupsLoading = false
            branchGroupsLoadedOnce = true

            Log.d(
                FREE_SESSIONS_DEBUG,
                "groups_load_skip | reason=currentUid_blank"
            )

            return@LaunchedEffect
        }

        branchGroupsLoading = true
        branchGroupsLoadedOnce = false

        Log.d(
            FREE_SESSIONS_DEBUG,
            "groups_load_start | currentUid=$currentUid | availableBranches=${
                availableBranches.joinToString(
                    " | "
                )
            }"
        )

        val db = FirebaseFirestore.getInstance()

        fun cleanGroups(rawGroups: List<String>): List<String> {
            return rawGroups
                .map {
                    il.kmi.app.training.TrainingCatalog
                        .normalizeGroupName(it)
                        .ifBlank { it }
                        .trim()
                }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun groupsFromAny(value: Any?): List<String> {
            return when (value) {
                is String -> splitFreeSessionCsv(value)

                is List<*> -> value
                    .mapNotNull { it?.toString()?.trim() }
                    .filter { it.isNotBlank() }

                is Set<*> -> value
                    .mapNotNull { it?.toString()?.trim() }
                    .filter { it.isNotBlank() }

                else -> emptyList()
            }
        }

        fun branchesFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): List<String> {
            val out = mutableListOf<String>()

            listOf(
                "active_branch",
                "activeBranch",
                "branch",
                "branchName",
                "branch_name",
                "branches",
                "selected_branches",
                "branches_json"
            ).forEach { key ->
                when (val value = doc.get(key)) {
                    is String -> {
                        if (value.trim().startsWith("[")) {
                            runCatching {
                                val arr = org.json.JSONArray(value)
                                for (i in 0 until arr.length()) {
                                    arr.optString(i)
                                        .trim()
                                        .takeIf { it.isNotBlank() }
                                        ?.let { out += it }
                                }
                            }
                        } else {
                            out += splitFreeSessionCsv(value)
                        }
                    }

                    is List<*> -> {
                        value.mapNotNull { it?.toString()?.trim() }
                            .filter { it.isNotBlank() }
                            .forEach { out += it }
                    }

                    is Set<*> -> {
                        value.mapNotNull { it?.toString()?.trim() }
                            .filter { it.isNotBlank() }
                            .forEach { out += it }
                    }
                }
            }

            return out
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun groupsFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): List<String> {
            val out = mutableListOf<String>()

            listOf(
                "active_group",
                "activeGroup",
                "group",
                "groupKey",
                "group_key",
                "primaryGroup",
                "groups",
                "groups_json",
                "selected_groups",
                "age_group",
                "age_groups"
            ).forEach { key ->
                when (val value = doc.get(key)) {
                    is String -> {
                        if (value.trim().startsWith("[")) {
                            runCatching {
                                val arr = org.json.JSONArray(value)
                                for (i in 0 until arr.length()) {
                                    arr.optString(i)
                                        .trim()
                                        .takeIf { it.isNotBlank() }
                                        ?.let { out += it }
                                }
                            }
                        } else {
                            out += splitFreeSessionCsv(value)
                        }
                    }

                    is List<*> -> {
                        value.mapNotNull { it?.toString()?.trim() }
                            .filter { it.isNotBlank() }
                            .forEach { out += it }
                    }

                    is Set<*> -> {
                        value.mapNotNull { it?.toString()?.trim() }
                            .filter { it.isNotBlank() }
                            .forEach { out += it }
                    }
                }
            }

            return cleanGroups(out)
        }

        fun mergeInto(
            target: MutableMap<String, MutableSet<String>>,
            branchName: String,
            groups: List<String>
        ) {
            val cleanBranch = branchName.trim()

            val cleanGroupsList = sanitizeFreeSessionGroupsForBranch(
                branch = cleanBranch,
                groups = cleanGroups(groups)
            )

            if (cleanBranch.isBlank() || cleanGroupsList.isEmpty()) return

            val key = normalizedBranchKey(cleanBranch)
            val set = target.getOrPut(key) { linkedSetOf() }
            cleanGroupsList.forEach { set += it }
        }

        db.collection("users")
            .document(currentUid)
            .get()
            .addOnSuccessListener { currentUserDoc ->

                val docCoachUid = listOf(
                    currentUserDoc.getString("coachUid"),
                    currentUserDoc.getString("coach_uid"),
                    currentUserDoc.getString("trainerUid"),
                    currentUserDoc.getString("trainer_uid"),
                    currentUserDoc.getString("instructorUid"),
                    currentUserDoc.getString("instructor_uid")
                )
                    .map { it.orEmpty().trim() }
                    .firstOrNull { it.isNotBlank() }
                    .orEmpty()

                val effectiveCoachUid = resolvedCoachUid
                    .ifBlank { docCoachUid }
                    .ifBlank { currentUid }

                resolvedCoachUid = effectiveCoachUid

                val directResult = linkedMapOf<String, MutableSet<String>>()
                val coachDoc = currentUserDoc

                fun readCoachMapField(vararg keys: String) {
                    keys.forEach { key ->
                        val value = coachDoc.get(key)
                        val map = value as? Map<*, *> ?: return@forEach

                        map.forEach { entry ->
                            val branchName = entry.key?.toString()?.trim().orEmpty()
                            val groups = groupsFromAny(entry.value)
                            mergeInto(directResult, branchName, groups)
                        }
                    }
                }

                readCoachMapField(
                    "branchGroups",
                    "groupsByBranch",
                    "branchToGroups",
                    "branchesToGroups",
                    "branch_groups"
                )

                val coachBranchesValue = coachDoc.get("branches")
                if (coachBranchesValue is List<*>) {
                    coachBranchesValue.forEach { item ->
                        val map = item as? Map<*, *> ?: return@forEach

                        val branchName = listOf(
                            "name",
                            "branch",
                            "branchName",
                            "branch_name",
                            "title"
                        )
                            .mapNotNull { key -> map[key]?.toString()?.trim() }
                            .firstOrNull { it.isNotBlank() }
                            .orEmpty()

                        val groups = listOf(
                            "groups",
                            "ageGroups",
                            "age_groups",
                            "groupKeys",
                            "group_keys"
                        )
                            .flatMap { key -> groupsFromAny(map[key]) }

                        mergeInto(directResult, branchName, groups)
                    }
                }

                if (directResult.isNotEmpty()) {
                    serverGroupsByBranch = directResult.mapValues { it.value.toList() }
                    branchGroupsLoading = false
                    branchGroupsLoadedOnce = true

                    Log.d(
                        FREE_SESSIONS_DEBUG,
                        "groups_load_finish | source=coach_direct_fields | map=$serverGroupsByBranch"
                    )

                    return@addOnSuccessListener
                }

                fun loadTraineesByBranchesFallback() {
                    db.collection("users")
                        .limit(800)
                        .get()
                        .addOnSuccessListener { allUsersSnap ->

                            Log.d(
                                FREE_SESSIONS_DEBUG,
                                "branches_fallback_start | usersCount=${allUsersSnap.size()} | availableBranches=${
                                    availableBranches.joinToString(
                                        " | "
                                    )
                                }"
                            )

                            val wantedBranches = availableBranches
                                .map { normalizedBranchKey(it) }
                                .filter { it.isNotBlank() }
                                .toSet()

                            val fromBranches = linkedMapOf<String, MutableSet<String>>()

                            allUsersSnap.documents.forEach { userDoc ->
                                val userBranches = branchesFromDoc(userDoc)
                                val userGroups = groupsFromDoc(userDoc)

                                val matchedBranches = userBranches.filter { userBranch ->
                                    val cleanUserBranch = normalizedBranchKey(userBranch)

                                    wantedBranches.any { wanted ->
                                        cleanUserBranch == wanted ||
                                                cleanUserBranch.contains(wanted) ||
                                                wanted.contains(cleanUserBranch)
                                    }
                                }

                                // חשוב:
                                // אם למשתמש יש כמה סניפים וכמה קבוצות,
                                // אין דרך לדעת איזו קבוצה שייכת לאיזה סניף.
                                // לכן לא משייכים כדי לא לזהם את כל הסניפים.
                                val isUnambiguous =
                                    matchedBranches.size == 1 && userGroups.isNotEmpty()

                                if (isUnambiguous) {
                                    Log.d(
                                        FREE_SESSIONS_DEBUG,
                                        "branches_fallback_match | id=${userDoc.id} | branch=${matchedBranches.first()} | groups=${
                                            userGroups.joinToString(
                                                " | "
                                            )
                                        }"
                                    )

                                    mergeInto(
                                        target = fromBranches,
                                        branchName = matchedBranches.first(),
                                        groups = userGroups
                                    )
                                } else if (matchedBranches.isNotEmpty() && userGroups.isNotEmpty()) {
                                    Log.d(
                                        FREE_SESSIONS_DEBUG,
                                        "branches_fallback_skip_ambiguous | id=${userDoc.id} | branches=${
                                            userBranches.joinToString(
                                                " | "
                                            )
                                        } | matched=${matchedBranches.joinToString(" | ")} | groups=${
                                            userGroups.joinToString(
                                                " | "
                                            )
                                        }"
                                    )
                                }
                            }

                            serverGroupsByBranch = fromBranches.mapValues { it.value.toList() }
                            branchGroupsLoading = false
                            branchGroupsLoadedOnce = true

                            Log.d(
                                FREE_SESSIONS_DEBUG,
                                "groups_load_finish | source=branches_fallback | map=$serverGroupsByBranch"
                            )
                        }
                        .addOnFailureListener { error ->
                            serverGroupsByBranch = emptyMap()
                            branchGroupsLoading = false
                            branchGroupsLoadedOnce = true

                            Log.e(
                                FREE_SESSIONS_DEBUG,
                                "groups_load_failed | source=branches_fallback | message=${error.message.orEmpty()}",
                                error
                            )
                        }
                }

                // ✅ fallback אמיתי מהשרת:
                // אם במסמך המאמן אין מיפוי branch -> groups,
                // בונים אותו מהמתאמנים שמשויכים למאמן.
                fun loadTraineesByCoachField(
                    fieldName: String,
                    onEmpty: () -> Unit
                ) {
                    db.collection("users")
                        .whereEqualTo(fieldName, effectiveCoachUid)
                        .limit(400)
                        .get()
                        .addOnSuccessListener { traineesSnap ->

                            Log.d(
                                FREE_SESSIONS_DEBUG,
                                "trainees_query_success | field=$fieldName | coachUid=$effectiveCoachUid | count=${traineesSnap.size()}"
                            )

                            if (traineesSnap.isEmpty) {
                                onEmpty()
                                return@addOnSuccessListener
                            }

                            val fromTrainees = linkedMapOf<String, MutableSet<String>>()

                            traineesSnap.documents.forEach { traineeDoc ->
                                val traineeBranches = branchesFromDoc(traineeDoc)
                                val traineeGroups = groupsFromDoc(traineeDoc)

                                Log.d(
                                    FREE_SESSIONS_DEBUG,
                                    "trainee_doc | id=${traineeDoc.id} | branches=${
                                        traineeBranches.joinToString(
                                            " | "
                                        )
                                    } | groups=${traineeGroups.joinToString(" | ")}"
                                )

                                traineeBranches.forEach { traineeBranch ->
                                    mergeInto(
                                        target = fromTrainees,
                                        branchName = traineeBranch,
                                        groups = traineeGroups
                                    )
                                }
                            }

                            serverGroupsByBranch = fromTrainees.mapValues { it.value.toList() }
                            branchGroupsLoading = false
                            branchGroupsLoadedOnce = true

                            Log.d(
                                FREE_SESSIONS_DEBUG,
                                "groups_load_finish | source=trainees_$fieldName | map=$serverGroupsByBranch"
                            )
                        }
                        .addOnFailureListener { error ->
                            serverGroupsByBranch = emptyMap()
                            branchGroupsLoading = false
                            branchGroupsLoadedOnce = true

                            Log.e(
                                FREE_SESSIONS_DEBUG,
                                "groups_load_failed | source=trainees_$fieldName | message=${error.message.orEmpty()}",
                                error
                            )
                        }
                }

                loadTraineesByCoachField(
                    fieldName = "coachUid",
                    onEmpty = {
                        loadTraineesByCoachField(
                            fieldName = "coach_uid",
                            onEmpty = {
                                loadTraineesByBranchesFallback()
                            }
                        )
                    }
                )
            }
            .addOnFailureListener { error ->
                serverGroupsByBranch = emptyMap()
                branchGroupsLoading = false
                branchGroupsLoadedOnce = true

                Log.e(
                    FREE_SESSIONS_DEBUG,
                    "groups_load_failed | source=current_user_doc | message=${error.message.orEmpty()}",
                    error
                )
            }
    }

    val groupsForSelectedBranch = remember(
        selectedBranch,
        serverGroupsByBranch,
        branchGroupsLoading,
        branchGroupsLoadedOnce
    ) {
        val rawGroups =
            serverGroupsByBranch[normalizedBranchKey(selectedBranch)]
                ?.takeIf { it.isNotEmpty() }
                ?: emptyList()

        sanitizeFreeSessionGroupsForBranch(
            branch = selectedBranch,
            groups = rawGroups
        )
    }

    var selectedGroupKey by rememberSaveable(
        availableGroups.joinToString("|"),
        selectedBranch
    ) {
        mutableStateOf(
            groupKey
                .takeIf { it.isNotBlank() && it in groupsForSelectedBranch }
                ?: groupsForSelectedBranch.firstOrNull()
                ?: ""
        )
    }

    LaunchedEffect(selectedBranch, groupsForSelectedBranch.joinToString("|")) {
        if (selectedGroupKey !in groupsForSelectedBranch) {
            selectedGroupKey = groupsForSelectedBranch.firstOrNull().orEmpty()
        }
    }

    LaunchedEffect(selectedBranch, selectedGroupKey, currentUid, currentName) {
        vm.setContext(
            branch = selectedBranch,
            groupKey = selectedGroupKey,
            myUid = currentUid,
            myName = currentName
        )
    }

    val sessions by vm.upcoming.collectAsStateCompat()

    suspend fun createFreeSessionNotificationRequests(
        sessionTitle: String,
        locationName: String?,
        startsAtMillis: Long
    ) {
        withContext(Dispatchers.IO) {
            runCatching {
                val db = FirebaseFirestore.getInstance()

                data class FreeSessionNotifyTarget(
                    val uid: String,
                    val name: String,
                    val email: String,
                    val phone: String,
                    val mergeKey: String
                )

                fun splitDocValue(value: Any?): List<String> {
                    return when (value) {
                        is String -> {
                            val raw = value.trim()

                            if (raw.startsWith("[")) {
                                runCatching {
                                    val arr = org.json.JSONArray(raw)
                                    (0 until arr.length())
                                        .mapNotNull { index -> arr.optString(index, null) }
                                }.getOrDefault(emptyList())
                            } else {
                                raw.split(',', ';', '|', '\n')
                            }
                        }

                        is List<*> -> value.mapNotNull { it?.toString() }
                        is Set<*> -> value.mapNotNull { it?.toString() }
                        else -> emptyList()
                    }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                }

                fun docBranches(doc: com.google.firebase.firestore.DocumentSnapshot): List<String> {
                    return listOf(
                        "active_branch",
                        "activeBranch",
                        "branch",
                        "branchName",
                        "branch_name",
                        "branches",
                        "branches_json",
                        "selected_branches"
                    )
                        .flatMap { key -> splitDocValue(doc.get(key)) }
                        .distinct()
                }

                fun docGroups(doc: com.google.firebase.firestore.DocumentSnapshot): List<String> {
                    return listOf(
                        "active_group",
                        "activeGroup",
                        "group",
                        "groupKey",
                        "group_key",
                        "primaryGroup",
                        "groups",
                        "groups_json",
                        "selected_groups",
                        "age_group",
                        "age_groups"
                    )
                        .flatMap { key -> splitDocValue(doc.get(key)) }
                        .map {
                            il.kmi.app.training.TrainingCatalog
                                .normalizeGroupName(it)
                                .ifBlank { it }
                                .trim()
                        }
                        .filter { it.isNotBlank() }
                        .distinct()
                }

                fun docName(doc: com.google.firebase.firestore.DocumentSnapshot): String {
                    return listOf(
                        doc.getString("fullName"),
                        doc.getString("full_name"),
                        doc.getString("name"),
                        doc.getString("displayName"),
                        doc.getString("display_name"),
                        doc.getString("user_name"),
                        doc.getString("email")
                    )
                        .map { it.orEmpty().trim() }
                        .firstOrNull { it.isNotBlank() }
                        .orEmpty()
                }

                fun docEmail(doc: com.google.firebase.firestore.DocumentSnapshot): String {
                    return listOf(
                        doc.getString("email"),
                        doc.getString("userEmail"),
                        doc.getString("user_email")
                    )
                        .map { it.orEmpty().trim().lowercase(Locale.US) }
                        .firstOrNull { it.isNotBlank() }
                        .orEmpty()
                }

                fun docPhone(doc: com.google.firebase.firestore.DocumentSnapshot): String {
                    return listOf(
                        doc.getString("phone"),
                        doc.getString("phoneNumber"),
                        doc.getString("phone_number"),
                        doc.getString("mobile"),
                        doc.getString("userPhone"),
                        doc.getString("user_phone")
                    )
                        .map { it.orEmpty().filter { ch -> ch.isDigit() } }
                        .firstOrNull { it.isNotBlank() }
                        .orEmpty()
                }

                fun mergeKey(
                    uid: String,
                    name: String,
                    email: String,
                    phone: String
                ): String {
                    return when {
                        email.isNotBlank() -> "email:$email"
                        phone.isNotBlank() -> "phone:$phone"
                        name.isNotBlank() -> "name:${normalizeFreeSessionText(name)}"
                        else -> "uid:$uid"
                    }
                }

                val wantedBranch = normalizeFreeSessionText(selectedBranch)
                val wantedGroup = normalizeFreeSessionText(
                    il.kmi.app.training.TrainingCatalog
                        .normalizeGroupName(selectedGroupKey)
                        .ifBlank { selectedGroupKey }
                )

                val targets = db.collection("users")
                    .limit(1000)
                    .get()
                    .await()
                    .documents
                    .filter { doc ->
                        val branchMatch = docBranches(doc).any { branchName ->
                            val clean = normalizeFreeSessionText(branchName)
                            clean == wantedBranch ||
                                    clean.contains(wantedBranch) ||
                                    wantedBranch.contains(clean)
                        }

                        val groupMatch = docGroups(doc).any { groupName ->
                            normalizeFreeSessionText(groupName) == wantedGroup
                        }

                        branchMatch && groupMatch
                    }
                    .mapNotNull { doc ->
                        val uid = listOf(
                            doc.getString("uid"),
                            doc.getString("authUid"),
                            doc.id
                        )
                            .map { it.orEmpty().trim() }
                            .firstOrNull { it.isNotBlank() }
                            .orEmpty()

                        val name = docName(doc)
                        val email = docEmail(doc)
                        val phone = docPhone(doc)

                        if (uid.isBlank()) {
                            null
                        } else {
                            FreeSessionNotifyTarget(
                                uid = uid,
                                name = name,
                                email = email,
                                phone = phone,
                                mergeKey = mergeKey(uid, name, email, phone)
                            )
                        }
                    }
                    .filter { it.uid != currentUid }
                    .distinctBy { it.mergeKey }

                if (targets.isEmpty()) {
                    Log.d(
                        FREE_SESSIONS_DEBUG,
                        "free_session_push_skip | reason=no_targets | branch=$selectedBranch | group=$selectedGroupKey"
                    )
                    return@runCatching
                }

                val timeText = fmtTime(startsAtMillis, isEnglish)
                val locationText = locationName.orEmpty().trim()

                val titleText = if (isEnglish) {
                    "New free session"
                } else {
                    "נפתח אימון חופשי"
                }

                val bodyText = if (isEnglish) {
                    buildString {
                        append("A free session was opened")
                        if (locationText.isNotBlank()) append(" at ").append(locationText)
                        append(" · ").append(timeText)
                    }
                } else {
                    buildString {
                        append("נפתח אימון חופשי")
                        if (locationText.isNotBlank()) append(" במקום ").append(locationText)
                        append(" בשעה ").append(timeText)
                    }
                }

                val broadcastRef = db.collection("coachBroadcasts").document()
                val broadcastId = broadcastRef.id

                broadcastRef.set(
                    mapOf(
                        "type" to "free_session_created",
                        "broadcastId" to broadcastId,
                        "broadcast_id" to broadcastId,

                        // שדות קיימים של הודעות מאמן
                        "text" to bodyText,
                        "message" to bodyText,
                        "body" to bodyText,
                        "title" to titleText,
                        "coachName" to currentName,
                        "senderName" to currentName,
                        "fromName" to currentName,
                        "authorUid" to currentUid,
                        "coachUid" to currentUid,
                        "senderUid" to currentUid,

                        // יעדים — כדי שה־Push הקיים של הודעות מאמן יידע למי לשלוח
                        "targetUids" to targets.map { it.uid },
                        "recipientUids" to targets.map { it.uid },
                        "targetEmails" to targets.map { it.email }.filter { it.isNotBlank() },
                        "targetPhones" to targets.map { it.phone }.filter { it.isNotBlank() },
                        "targetNames" to targets.map { it.name }.filter { it.isNotBlank() },
                        "targetRecipients" to targets.map { target ->
                            mapOf(
                                "uid" to target.uid,
                                "name" to target.name,
                                "email" to target.email,
                                "phone" to target.phone
                            )
                        },

                        // סינון למסך הבית ולכרטיס הודעות מאמן
                        "branch" to selectedBranch,
                        "branchName" to selectedBranch,
                        "selectedBranch" to selectedBranch,
                        "group" to selectedGroupKey,
                        "groupKey" to selectedGroupKey,
                        "selectedGroup" to selectedGroupKey,

                        // מידע על האימון החופשי
                        "source" to "free_sessions",
                        "sessionTitle" to sessionTitle,
                        "locationName" to locationText,
                        "startsAt" to startsAtMillis,

                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "sentAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()

                Log.d(
                    FREE_SESSIONS_DEBUG,
                    "free_session_coach_broadcast_created | broadcastId=$broadcastId | targets=${targets.size} | branch=$selectedBranch | group=$selectedGroupKey"
                )
            }.onFailure { error ->
                Log.e(
                    FREE_SESSIONS_DEBUG,
                    "free_session_coach_broadcast_failed | message=${error.message.orEmpty()}",
                    error
                )
            }
        }
    }

// ===== Create dialog =====
    var showCreate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var locationQuery by remember { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<FreeSessionPlaceSuggestion?>(null) }

    // ברירת מחדל: עוד שעה
    var startsAt by remember { mutableLongStateOf(System.currentTimeMillis() + 60 * 60 * 1000L) }

    // ===== Details sheet =====
    var selected by remember { mutableStateOf<FreeSession?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            KmiTopBar(
                title = tr("אימונים חופשיים", "Free Sessions"),
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                lockSearch = false,
                lockHome = false,
                centerTitle = true,
                onHome = onBack
            )
        },
        floatingActionButton = {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0EA5E9),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                IconButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "יצירת אימון", tint = Color.White)
                }
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { p ->

        CompositionLocalProvider(LocalLayoutDirection provides screenDirection) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                KmiFreeBgTop,
                                KmiFreeBgMid,
                                KmiFreeBgBottom
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(p)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    HeaderCard(
                        branch = selectedBranch,
                        groupKey = selectedGroupKey,
                        count = sessions.size,
                        isEnglish = isEnglish
                    )

                    if (sessions.isEmpty()) {
                        EmptyStateCard(
                            title = tr(
                                "אין עדיין אימונים מתוכננים",
                                "No free sessions planned yet"
                            ),
                            subtitle = tr(
                                "אפשר ליצור אימון חדש ולשלוח הזמנה לכל המתאמנים בקבוצה.",
                                "Create a new free session and invite the group."
                            ),
                            isEnglish = isEnglish
                        )
                    } else {
                        // ✅ NEW: Delete confirm dialog state
                        var pendingDelete by remember { mutableStateOf<FreeSession?>(null) }

                        sessions.forEach { s ->
                            FreeSessionCard(
                                session = s,
                                isEnglish = isEnglish,
                                onClick = { selected = s },

                                // ✅ רק יוצר האימון רואה עריכה/מחיקה
                                canManage = (s.createdByUid == currentUid),

                                // כרגע "עריכה" פשוט פותחת את ה-Details sheet
                                onEdit = { selected = s },

                                // דיאלוג אישור לפני מחיקה
                                onDelete = { pendingDelete = s }
                            )
                        }

                        // ✅ NEW: Confirm delete dialog
                        pendingDelete?.let { s ->
                            AlertDialog(
                                onDismissRequest = { pendingDelete = null },
                                title = {
                                    Text(
                                        text = "מחיקת אימון",
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                text = {
                                    Text(
                                        text = "למחוק את האימון \"${s.title}\"?",
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val sid = s.id
                                        scope.launch {
                                            val res = runCatching {
                                                repo.deleteFreeSession(
                                                    branch = selectedBranch,
                                                    groupKey = selectedGroupKey,
                                                    sessionId = sid
                                                )
                                            }

                                            if (res.isSuccess) {
                                                Toast.makeText(
                                                    ctx,
                                                    "האימון נמחק ✅",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                pendingDelete = null
                                                selected = null // אם במקרה פתוח שיט של אותו אימון
                                            } else {
                                                val e = res.exceptionOrNull()
                                                val msg = e?.message?.takeIf { it.isNotBlank() }
                                                    ?: "מחיקת אימון נכשלה"
                                                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                                                pendingDelete = null
                                            }
                                        }
                                    }) { Text("מחק") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { pendingDelete = null }) { Text("ביטול") }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(90.dp))
                }
            }
        }
    }

    // ===== Create dialog UI =====
    if (showCreate) {
        Dialog(
            onDismissRequest = { showCreate = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(34.dp),
                color = Color.Transparent,
                shadowElevation = 24.dp,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(34.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    KmiFreeBorderColorStrong,
                                    KmiFreeBorderColor,
                                    KmiFreeBgTop
                                )
                            )
                        )
                        .padding(1.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(33.dp),
                        color = KmiFreeBgMid.copy(alpha = 0.98f),
                        tonalElevation = 0.dp
                    ) {
                        val scroll = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = screenHorizontalEnd
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = screenHorizontalEnd
                                ) {
                                    Text(
                                        text = tr("יצירת אימון חדש", "Create New Session"),
                                        color = KmiFreeTitleColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        lineHeight = 25.sp,
                                        textAlign = screenTextAlign,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = tr(
                                            "בחר כותרת, מקום, תאריך ושעה לאימון החופשי",
                                            "Choose a title, location, date and time for the free session"
                                        ),
                                        color = KmiFreeSubTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        textAlign = screenTextAlign,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.10f),
                                    border = BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.18f)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF67E8F9),
                                        modifier = Modifier
                                            .padding(11.dp)
                                            .size(24.dp)
                                    )
                                }
                            }

                            Divider(color = KmiFreeBorderColor)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 560.dp)
                                    .verticalScroll(scroll)
                                    .padding(
                                        bottom = WindowInsets.ime
                                            .asPaddingValues()
                                            .calculateBottomPadding() + 190.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = screenHorizontalEnd
                            ) {

                                PremiumBranchGroupSelector(
                                    branches = availableBranches,
                                    groups = groupsForSelectedBranch,
                                    selectedBranch = selectedBranch,
                                    selectedGroup = selectedGroupKey,
                                    isEnglish = isEnglish,
                                    isLoadingGroups = branchGroupsLoading && !branchGroupsLoadedOnce,
                                    onBranchSelected = { selectedBranch = it },
                                    onGroupSelected = { selectedGroupKey = it }
                                )

                                PremiumCreateInputField(
                                    label = tr("כותרת", "Title"),
                                    isEnglish = isEnglish,
                                    value = title,
                                    onValueChange = { title = it },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { })
                                )

                                WazeStyleLocationSearchField(
                                    query = locationQuery,
                                    selectedPlace = selectedPlace,
                                    isEnglish = isEnglish,
                                    branchHint = selectedBranch,
                                    existingPlaces = sessions
                                        .mapNotNull { it.locationName?.trim() }
                                        .filter { it.isNotBlank() }
                                        .distinct(),
                                    onQueryChange = {
                                        locationQuery = it
                                        selectedPlace = null
                                    },
                                    onPlaceSelected = { place ->
                                        selectedPlace = place
                                        locationQuery = place.name.ifBlank { place.address }
                                    }
                                )

                                PremiumDateTimeCard(
                                    startsAt = startsAt,
                                    isEnglish = isEnglish,
                                    onPick = { startsAt = it }
                                )

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    color = KmiFreeCardColor.copy(alpha = 0.94f),
                                    border = BorderStroke(
                                        1.dp,
                                        KmiFreeBorderColor
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { showCreate = false }
                                        ) {
                                            Text(
                                                text = tr("ביטול", "Cancel"),
                                                color = KmiFreeSubTextColor,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            )
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Surface(
                                            onClick = {
                                                val cleanTitle = title.trim()

                                                if (cleanTitle.isBlank()) {
                                                    Toast.makeText(
                                                        ctx,
                                                        tr(
                                                            "נא להזין כותרת",
                                                            "Please enter a title"
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    return@Surface
                                                }

                                                scope.launch {
                                                    val result = runCatching {
                                                        repo.createFreeSession(
                                                            branch = selectedBranch,
                                                            groupKey = selectedGroupKey,
                                                            title = cleanTitle,
                                                            locationName = (
                                                                    selectedPlace?.name
                                                                        ?: locationQuery
                                                                    )
                                                                .trim()
                                                                .ifBlank { null },
                                                            lat = selectedPlace?.lat,
                                                            lng = selectedPlace?.lng,
                                                            startsAt = startsAt,
                                                            createdByUid = currentUid,
                                                            createdByName = currentName
                                                        )
                                                    }

                                                    if (result.isSuccess) {
                                                        val createdTitle = cleanTitle
                                                        val createdLocation = (
                                                                selectedPlace?.name
                                                                    ?: locationQuery
                                                                )
                                                            .trim()
                                                            .ifBlank { null }

                                                        val createdStartsAt = startsAt

                                                        createFreeSessionNotificationRequests(
                                                            sessionTitle = createdTitle,
                                                            locationName = createdLocation,
                                                            startsAtMillis = createdStartsAt
                                                        )

                                                        title = ""
                                                        locationQuery = ""
                                                        selectedPlace = null
                                                        startsAt =
                                                            System.currentTimeMillis() + 60 * 60 * 1000L
                                                        showCreate = false

                                                        Toast
                                                            .makeText(
                                                                ctx,
                                                                tr(
                                                                    "האימון נוצר בהצלחה ✅",
                                                                    "Session created successfully ✅"
                                                                ),
                                                                Toast.LENGTH_SHORT
                                                            )
                                                            .show()
                                                    } else {
                                                        val e = result.exceptionOrNull()
                                                        val msg =
                                                            e?.message?.takeIf { it.isNotBlank() }
                                                                ?: tr(
                                                                    "יצירת אימון נכשלה",
                                                                    "Failed to create session"
                                                                )

                                                        Toast
                                                            .makeText(ctx, msg, Toast.LENGTH_LONG)
                                                            .show()
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color(0xFF22D3EE),
                                            shadowElevation = 6.dp
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(
                                                    horizontal = 30.dp,
                                                    vertical = 11.dp
                                                ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = tr("צור", "Create"),
                                                    color = Color(0xFF04101F),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp
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

    // ===== Details bottom sheet =====
    selected?.let { session ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheetState,
            containerColor = KmiFreeBgMid
        ) {
            FreeSessionDetailsSheet(
                repo = repo,
                isEnglish = isEnglish,
                session = session,
                branch = selectedBranch,
                groupKey = selectedGroupKey,
                currentUid = currentUid,
                currentName = currentName,
                onClose = { selected = null }
            )
        }
    }
}

/* ---------------- UI blocks ---------------- */

@Composable
private fun HeaderCard(
    branch: String,
    groupKey: String,
    count: Int,
    isEnglish: Boolean
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = KmiFreeCardColor,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, KmiFreeBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = horizontal
        ) {

            Text(
                text = "${tr("סניף", "Branch")}: ${branch.trim()}",
                color = KmiFreeTextColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                textAlign = align,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${tr("קבוצה", "Group")}: ${groupKey.trim()}",
                color = KmiFreeSubTextColor,
                fontWeight = FontWeight.ExtraBold,
                textAlign = align,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Divider(color = KmiFreeBorderColor)

            Text(
                text = "${tr("אימונים עתידיים", "Upcoming sessions")}: $count",
                color = KmiFreeTextColor,
                fontWeight = FontWeight.Black,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    isEnglish: Boolean
) {
    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = KmiFreeCardColor,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, KmiFreeBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = horizontal
        ) {
            Text(
                text = title,
                color = KmiFreeTextColor,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = subtitle,
                color = KmiFreeSubTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PremiumCreateInputField(
    label: String,
    isEnglish: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions
) {
    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = horizontal
    ) {
        Text(
            text = label,
            color = KmiFreeTitleColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = KmiFreeCardColorSoft.copy(alpha = 0.82f),
            border = BorderStroke(
                1.dp,
                KmiFreeBorderColor
            ),
            tonalElevation = 0.dp
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = KmiFreeTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = align
                ),
                placeholder = {
                    Text(
                        text = label,
                        color = KmiFreeSubTextColor.copy(alpha = 0.78f),
                        textAlign = align,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = KmiFreeTextColor,
                    unfocusedTextColor = KmiFreeTextColor,
                    cursorColor = KmiFreeBorderColorStrong,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = KmiFreeTitleColor,
                    unfocusedLabelColor = KmiFreeTitleColor
                )
            )
        }
    }
}

@Composable
private fun PremiumBranchGroupSelector(
    branches: List<String>,
    groups: List<String>,
    selectedBranch: String,
    selectedGroup: String,
    isEnglish: Boolean,
    isLoadingGroups: Boolean,
    onBranchSelected: (String) -> Unit,
    onGroupSelected: (String) -> Unit
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    if (branches.size <= 1 && groups.size <= 1 && groups.isNotEmpty()) {
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = KmiFreeCardColor.copy(alpha = 0.94f),
        border = BorderStroke(
            1.dp,
            KmiFreeBorderColor
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = horizontal
        ) {
            if (branches.size > 1) {
                PremiumComboPicker(
                    title = tr("בחר סניף", "Choose branch"),
                    values = branches,
                    selected = selectedBranch,
                    isEnglish = isEnglish,
                    onSelected = onBranchSelected
                )
            }

            if (groups.isEmpty()) {
                Text(
                    text = if (isLoadingGroups) {
                        tr(
                            "טוען קבוצות לפי הסניף שנבחר...",
                            "Loading groups for the selected branch..."
                        )
                    } else {
                        tr(
                            "לא נמצא שיוך קבוצות מדויק לסניף הזה",
                            "No exact group mapping was found for this branch"
                        )
                    },
                    color = KmiFreeSubTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (groups.size > 1) {
                PremiumComboPicker(
                    title = tr("בחר קבוצה", "Choose group"),
                    values = groups,
                    selected = selectedGroup,
                    isEnglish = isEnglish,
                    onSelected = onGroupSelected
                )
            } else if (groups.size == 1) {
                Text(
                    text = "${tr("קבוצה", "Group")}: ${groups.first()}",
                    color = KmiFreeTitleColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumComboPicker(
    title: String,
    values: List<String>,
    selected: String,
    isEnglish: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = horizontal
    ) {
        Text(
            text = title,
            color = KmiFreeTitleColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            textAlign = align,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                singleLine = false,
                minLines = 2,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .menuAnchor(),
                textStyle = MaterialTheme.typography.titleSmall.copy(
                    color = KmiFreeTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    textAlign = align
                ),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = KmiFreeCardColorSoft.copy(alpha = 0.86f),
                    unfocusedContainerColor = KmiFreeCardColorSoft.copy(alpha = 0.82f),
                    focusedTextColor = KmiFreeTextColor,
                    unfocusedTextColor = KmiFreeTextColor,
                    cursorColor = KmiFreeTitleColor,
                    focusedBorderColor = KmiFreeBorderColorStrong,
                    unfocusedBorderColor = KmiFreeBorderColor
                ),
                shape = RoundedCornerShape(20.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = Color(0xFF0A234A)
            ) {
                values.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item,
                                color = Color.White,
                                fontWeight = if (item == selected) {
                                    FontWeight.Black
                                } else {
                                    FontWeight.Bold
                                },
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                textAlign = align,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelected(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WazeStyleLocationSearchField(
    query: String,
    selectedPlace: FreeSessionPlaceSuggestion?,
    isEnglish: Boolean,
    branchHint: String,
    existingPlaces: List<String>,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (FreeSessionPlaceSuggestion) -> Unit
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val bringLocationIntoViewRequester = remember { BringIntoViewRequester() }

    val placesClient = remember(ctx, isEnglish) {
        if (!Places.isInitialized()) {
            Places.initialize(
                ctx.applicationContext,
                ctx.getString(R.string.google_maps_key),
                if (isEnglish) Locale.ENGLISH else Locale("he", "IL")
            )
        }

        Places.createClient(ctx)
    }

    var suggestions by remember {
        mutableStateOf<List<FreeSessionPlaceSuggestion>>(emptyList())
    }

    var searching by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(query, branchHint, selectedPlace) {
        val clean = query.trim()
        val cleanLower = normalizeFreeSessionText(clean)
        val branchClean = branchHint.trim()

        val queryTokens = cleanLower
            .split(" ")
            .map { it.trim() }
            .filter { it.length >= 2 }

        fun matchesUserLocationQuery(name: String, address: String): Boolean {
            val haystack = normalizeFreeSessionText("$name $address")

            if (queryTokens.isEmpty()) {
                return cleanLower.isNotBlank() && haystack.contains(cleanLower)
            }

            return queryTokens.any { token ->
                haystack.contains(token)
            }
        }

        if (clean.length < 2 || selectedPlace != null) {
            suggestions = emptyList()
            searching = false
            return@LaunchedEffect
        }

        searching = true
        delay(280)

        val localSuggestions = buildList {
            existingPlaces
                .filter { place ->
                    normalizeFreeSessionText(place).contains(cleanLower)
                }
                .take(5)
                .forEach { place ->
                    add(
                        FreeSessionPlaceSuggestion(
                            name = place,
                            address = place,
                            lat = null,
                            lng = null,
                            placeId = null
                        )
                    )
                }

            if (
                branchClean.isNotBlank() &&
                normalizeFreeSessionText(branchClean).contains(cleanLower)
            ) {
                add(
                    FreeSessionPlaceSuggestion(
                        name = branchClean,
                        address = branchClean,
                        lat = null,
                        lng = null,
                        placeId = null
                    )
                )
            }
        }

        val placesSuggestions = runCatching {
            val token = AutocompleteSessionToken.newInstance()

            val searchText = buildString {
                append(clean)
                if (branchClean.isNotBlank()) {
                    append(" ")
                    append(branchClean)
                }
            }.trim()

            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(searchText)
                .setCountries(listOf("IL"))
                .build()

            placesClient
                .findAutocompletePredictions(request)
                .await()
                .autocompletePredictions
                .mapNotNull { prediction ->
                    val name = prediction.getPrimaryText(null)
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                    val address = prediction.getSecondaryText(null)
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                    val fallbackName = prediction.getFullText(null)
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                    val finalName = name.ifBlank { fallbackName }

                    if (
                        (finalName.isBlank() && address.isBlank()) ||
                        !matchesUserLocationQuery(finalName, address)
                    ) {
                        null
                    } else {
                        FreeSessionPlaceSuggestion(
                            name = finalName.ifBlank { address },
                            address = address,
                            lat = null,
                            lng = null,
                            placeId = prediction.placeId
                        )
                    }
                }
                .distinctBy {
                    "${normalizeFreeSessionText(it.name)}|${normalizeFreeSessionText(it.address)}|${it.placeId.orEmpty()}"
                }
                .take(6)
        }.getOrElse { error ->
            Log.e(
                FREE_SESSIONS_DEBUG,
                "places_autocomplete_failed | query=$clean | branch=$branchClean | message=${error.message.orEmpty()}",
                error
            )

            emptyList()
        }

        val geoSuggestions = if (placesSuggestions.isEmpty()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val geocoder = Geocoder(
                        ctx,
                        if (isEnglish) Locale.ENGLISH else Locale("he", "IL")
                    )

                    val searchQueries = buildList {
                        add(clean)

                        if (branchClean.isNotBlank()) {
                            add("$clean $branchClean")
                        }

                        add("$clean ישראל")
                        add("$clean Israel")
                    }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()

                    searchQueries
                        .flatMap { searchText ->
                            @Suppress("DEPRECATION")
                            geocoder.getFromLocationName(searchText, 6).orEmpty()
                        }
                        .mapNotNull { address ->
                            val title = listOfNotNull(
                                address.featureName,
                                address.thoroughfare,
                                address.locality,
                                address.subAdminArea
                            )
                                .firstOrNull { !it.isNullOrBlank() }
                                ?.trim()
                                .orEmpty()

                            val fullAddress = (0..address.maxAddressLineIndex)
                                .mapNotNull { index ->
                                    address.getAddressLine(index)
                                        ?.trim()
                                        ?.takeIf { it.isNotBlank() }
                                }
                                .joinToString(" · ")

                            val name = title.ifBlank { fullAddress }

                            if (
                                (name.isBlank() && fullAddress.isBlank()) ||
                                !matchesUserLocationQuery(name, fullAddress)
                            ) {
                                null
                            } else {
                                FreeSessionPlaceSuggestion(
                                    name = name.ifBlank { fullAddress },
                                    address = fullAddress,
                                    lat = address.latitude,
                                    lng = address.longitude,
                                    placeId = null
                                )
                            }
                        }
                        .distinctBy {
                            "${normalizeFreeSessionText(it.name)}|${normalizeFreeSessionText(it.address)}"
                        }
                        .take(6)
                }.getOrDefault(emptyList())
            }
        } else {
            emptyList()
        }

        suggestions = (localSuggestions + placesSuggestions + geoSuggestions)
            .filter { place ->
                matchesUserLocationQuery(place.name, place.address)
            }
            .distinctBy {
                "${normalizeFreeSessionText(it.name)}|${normalizeFreeSessionText(it.address)}|${it.placeId.orEmpty()}"
            }
            .take(6)

        searching = false
    }

    LaunchedEffect(suggestions.size, searching) {
        if (suggestions.isNotEmpty() || searching) {
            delay(120)
            bringLocationIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringLocationIntoViewRequester),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = tr("מקום (אופציונלי)", "Location (optional)"),
            color = KmiFreeTitleColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = KmiFreeCardColorSoft.copy(alpha = 0.86f),
            border = BorderStroke(
                1.dp,
                if (suggestions.isNotEmpty()) {
                    KmiFreeBorderColorStrong
                } else {
                    KmiFreeBorderColor
                }
            ),
            tonalElevation = 0.dp,
            shadowElevation = if (suggestions.isNotEmpty()) 10.dp else 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { }),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = Color(0xFF22D3EE)
                        )
                    },
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = KmiFreeTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Right
                    ),
                    placeholder = {
                        Text(
                            text = tr(
                                "הקלד מקום, כתובת או עיר",
                                "typing a place, address or city"
                            ),
                            color = KmiFreeSubTextColor.copy(alpha = 0.78f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = KmiFreeTextColor,
                        unfocusedTextColor = KmiFreeTextColor,
                        cursorColor = Color(0xFF22D3EE),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedLabelColor = Color(0xFF67E8F9),
                        unfocusedLabelColor = Color(0xFF67E8F9)
                    )
                )

                if (searching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = Color(0xFF22D3EE),
                        trackColor = Color.Transparent
                    )
                }

                if (selectedPlace != null) {
                    Divider(color = Color.White.copy(alpha = 0.10f))

                    Text(
                        text = "${tr("נבחר", "Selected")}: ${selectedPlace.name}",
                        color = KmiFreeTitleColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }

                if (suggestions.isNotEmpty()) {
                    Divider(color = Color.White.copy(alpha = 0.10f))

                    suggestions.forEachIndexed { index, place ->
                        WazePlaceSuggestionRow(
                            place = place,
                            isEnglish = isEnglish,
                            onClick = {
                                scope.launch {
                                    val resolvedPlace = if (place.placeId.isNullOrBlank()) {
                                        place
                                    } else {
                                        runCatching {
                                            val request = FetchPlaceRequest.builder(
                                                place.placeId,
                                                listOf(
                                                    Place.Field.ID,
                                                    Place.Field.NAME,
                                                    Place.Field.ADDRESS,
                                                    Place.Field.LAT_LNG
                                                )
                                            ).build()

                                            val fetchedPlace = placesClient
                                                .fetchPlace(request)
                                                .await()
                                                .place

                                            val latLng = fetchedPlace.latLng

                                            FreeSessionPlaceSuggestion(
                                                name = fetchedPlace.name?.trim().orEmpty()
                                                    .ifBlank { place.name },
                                                address = fetchedPlace.address?.trim().orEmpty()
                                                    .ifBlank { place.address },
                                                lat = latLng?.latitude,
                                                lng = latLng?.longitude,
                                                placeId = fetchedPlace.id ?: place.placeId
                                            )
                                        }.getOrElse { error ->
                                            Log.e(
                                                FREE_SESSIONS_DEBUG,
                                                "places_fetch_place_failed | placeId=${place.placeId} | name=${place.name} | message=${error.message.orEmpty()}",
                                                error
                                            )

                                            place
                                        }
                                    }

                                    onPlaceSelected(resolvedPlace)
                                    suggestions = emptyList()
                                }
                            }
                        )

                        if (index < suggestions.lastIndex) {
                            Divider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WazePlaceSuggestionRow(
    place: FreeSessionPlaceSuggestion,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF22D3EE).copy(alpha = 0.14f),
                border = BorderStroke(
                    1.dp,
                    Color(0xFF22D3EE).copy(alpha = 0.30f)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = Color(0xFF22D3EE),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = horizontal
            ) {
                Text(
                    text = place.name,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    textAlign = align,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                if (place.address.isNotBlank() && place.address != place.name) {
                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = place.address,
                        color = KmiFreeSubTextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        textAlign = align,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumDateTimeCard(
    startsAt: Long,
    isEnglish: Boolean,
    onPick: (Long) -> Unit
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = KmiFreeCardColor.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            KmiFreeBorderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("בחירת יום ושעה", "Choose day and time"),
                    color = KmiFreeTitleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right
                )

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF22D3EE)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = KmiFreeBgBottom.copy(alpha = 0.92f),
                tonalElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    KmiFreeBorderColorStrong
                )
            ) {
                Text(
                    text = fmtTime(startsAt, isEnglish),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                )
            }

            TimeQuickPicker(
                startsAt = startsAt,
                isEnglish = isEnglish,
                onPick = onPick
            )
        }
    }
}

@Composable
private fun FreeSessionCard(
    session: FreeSession,
    isEnglish: Boolean,
    onClick: () -> Unit,
    canManage: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, KmiFreeBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            /* ---------- שורה עליונה: טקסט + פעולות ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                // ---- טקסטים (ימין) ----
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = session.title,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = KmiFreeTextColor,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "נוצר ע״י ${session.createdByName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KmiFreeSubTextColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---- פעולות (שמאל, גובה קבוע) ----
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .widthIn(min = 84.dp)        // ✅ לא ימחץ את הטקסט (מונע "שורה אנכית")
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF7DD3FC),
                        modifier = Modifier.size(20.dp)
                    )

                    if (canManage) {
                        IconButton(
                            onClick = { onEdit?.invoke() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "עריכה",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { onDelete?.invoke() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "מחיקה",
                                tint = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            }

            /* ---------- זמן + משתתפים (בלי שבירה אנכית) ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fmtTime(session.startsAt, isEnglish),
                    color = KmiFreeSubTextColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.width(14.dp))

                Icon(Icons.Filled.Group, contentDescription = null, tint = Color(0xFF22D3EE))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${session.goingCount + session.onWayCount + session.arrivedCount + session.cantCount} משתתפים",
                    color = KmiFreeTextColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            /* ---------- מיקום (שמאל) ---------- */
            session.locationName?.takeIf { it.isNotBlank() }?.let { loc ->
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = Color(0xFFF97316)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = loc,
                            color = KmiFreeTextColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = "לחץ כדי לבחור סטטוס (מגיע / לא יכול / וכו׳)",
                style = MaterialTheme.typography.labelSmall,
                color = KmiFreeSubTextColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,   // ✅ ב-RTL זה ימין
                modifier = Modifier.fillMaxWidth()
            )

            val total =
                (session.goingCount + session.onWayCount + session.arrivedCount + session.cantCount)
                    .coerceAtLeast(1)
            val progress by animateFloatAsState(
                (session.goingCount + session.onWayCount + session.arrivedCount).toFloat() / total
            )

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Color(0xFF22C55E),
                trackColor = KmiFreeBorderColor.copy(alpha = 0.55f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeSessionDetailsSheet(
    repo: FreeSessionsRepository,
    isEnglish: Boolean,
    session: FreeSession,
    branch: String,
    groupKey: String,
    currentUid: String,
    currentName: String,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val detailAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val detailHorizontal = if (isEnglish) Alignment.Start else Alignment.End
    val detailArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
    val detailChipAlignment = if (isEnglish) Alignment.Start else Alignment.End

    var parts by remember { mutableStateOf<List<FreeSessionPart>>(emptyList()) }
    var myState by remember { mutableStateOf<ParticipantState?>(null) }

    var branchUsers by remember {
        mutableStateOf<List<FreeSessionBranchUser>>(emptyList())
    }

    var branchUsersLoading by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(session.id) {
        repo.observeParticipants(branch, groupKey, session.id).collectLatest { list ->
            parts = list
            myState = list.firstOrNull { it.uid == currentUid }?.state
        }
    }

    LaunchedEffect(branch) {
        val cleanBranch = branch.trim()

        if (cleanBranch.isBlank()) {
            branchUsers = emptyList()
            branchUsersLoading = false
            return@LaunchedEffect
        }

        branchUsersLoading = true

        branchUsers = withContext(Dispatchers.IO) {
            runCatching {
                val db = FirebaseFirestore.getInstance()

                fun normalizeBranch(raw: String): String {
                    return normalizeFreeSessionText(raw)
                        .replace(" - ", " – ")
                        .replace("-", "–")
                        .replace("—", "–")
                        .replace("־", "–")
                        .trim()
                }

                fun splitValue(value: Any?): List<String> {
                    return when (value) {
                        is String -> {
                            val raw = value.trim()

                            if (raw.startsWith("[")) {
                                runCatching {
                                    val arr = org.json.JSONArray(raw)
                                    (0 until arr.length())
                                        .mapNotNull { index -> arr.optString(index, null) }
                                }.getOrDefault(emptyList())
                            } else {
                                raw.split(',', ';', '|', '\n')
                            }
                        }

                        is List<*> -> value.mapNotNull { it?.toString() }
                        is Set<*> -> value.mapNotNull { it?.toString() }
                        else -> emptyList()
                    }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                }

                fun userBranches(
                    doc: com.google.firebase.firestore.DocumentSnapshot
                ): List<String> {
                    return listOf(
                        "active_branch",
                        "activeBranch",
                        "branch",
                        "branchName",
                        "branch_name",
                        "branches",
                        "branches_json",
                        "selected_branches"
                    )
                        .flatMap { key -> splitValue(doc.get(key)) }
                        .distinct()
                }

                fun userName(
                    doc: com.google.firebase.firestore.DocumentSnapshot
                ): String {
                    return listOf(
                        doc.getString("fullName"),
                        doc.getString("full_name"),
                        doc.getString("name"),
                        doc.getString("displayName"),
                        doc.getString("display_name"),
                        doc.getString("user_name"),
                        doc.getString("email")
                    )
                        .map { it.orEmpty().trim() }
                        .firstOrNull { it.isNotBlank() }
                        .orEmpty()
                }

                fun userEmail(
                    doc: com.google.firebase.firestore.DocumentSnapshot
                ): String {
                    return listOf(
                        doc.getString("email"),
                        doc.getString("userEmail"),
                        doc.getString("user_email")
                    )
                        .map { it.orEmpty().trim().lowercase(Locale.US) }
                        .firstOrNull { it.isNotBlank() }
                        .orEmpty()
                }

                fun userPhone(
                    doc: com.google.firebase.firestore.DocumentSnapshot
                ): String {
                    return listOf(
                        doc.getString("phone"),
                        doc.getString("phoneNumber"),
                        doc.getString("phone_number"),
                        doc.getString("mobile"),
                        doc.getString("userPhone"),
                        doc.getString("user_phone")
                    )
                        .map { it.orEmpty().filter { ch -> ch.isDigit() } }
                        .firstOrNull { it.isNotBlank() }
                        .orEmpty()
                }

                fun userMergeKey(
                    uid: String,
                    name: String,
                    email: String,
                    phone: String
                ): String {
                    return when {
                        email.isNotBlank() -> "email:$email"
                        phone.isNotBlank() -> "phone:$phone"
                        name.isNotBlank() -> "name:${normalizeFreeSessionText(name)}"
                        else -> "uid:$uid"
                    }
                }

                val wanted = normalizeBranch(cleanBranch)

                db.collection("users")
                    .limit(1000)
                    .get()
                    .await()
                    .documents
                    .filter { doc ->
                        userBranches(doc).any { userBranch ->
                            val cleanUserBranch = normalizeBranch(userBranch)

                            cleanUserBranch == wanted ||
                                    cleanUserBranch.contains(wanted) ||
                                    wanted.contains(cleanUserBranch)
                        }
                    }
                    .mapNotNull { doc ->
                        val uid = listOf(
                            doc.getString("uid"),
                            doc.getString("authUid"),
                            doc.id
                        )
                            .map { it.orEmpty().trim() }
                            .firstOrNull { it.isNotBlank() }
                            .orEmpty()

                        val name = userName(doc)
                        val email = userEmail(doc)
                        val phone = userPhone(doc)
                        val mergeKey = userMergeKey(
                            uid = uid,
                            name = name,
                            email = email,
                            phone = phone
                        )

                        if (uid.isBlank() || name.isBlank()) {
                            null
                        } else {
                            FreeSessionBranchUser(
                                uid = uid,
                                name = name,
                                email = email,
                                phone = phone,
                                mergeKey = mergeKey
                            )
                        }
                    }
                    .distinctBy { it.mergeKey }
                    .sortedBy { it.name }
            }.getOrDefault(emptyList())
        }

        branchUsersLoading = false
    }

    val allParticipants = remember(parts, branchUsers) {
        val byMergeKey = linkedMapOf<String, FreeSessionPart>()

        branchUsers.forEach { user ->
            byMergeKey[user.mergeKey] = FreeSessionPart(
                uid = user.uid,
                name = user.name,
                state = ParticipantState.INVITED,
                updatedAt = 0L
            )
        }

        parts.forEach { part ->
            val existingUser = branchUsers.firstOrNull { user ->
                user.uid == part.uid ||
                        normalizeFreeSessionText(user.name) == normalizeFreeSessionText(part.name)
            }

            val key = existingUser?.mergeKey
                ?: "name:${normalizeFreeSessionText(part.name)}"

            byMergeKey[key] = part
        }

        byMergeKey.values.toList()
    }

    // ✅ חדש: יצירת לינק וויז (ניווט/שיתוף) לפי lat/lng אם קיימים, אחרת לפי שם מקום
    fun buildWazeUrl(navigate: Boolean): String {
        val nav = if (navigate) "true" else "false"
        val lat = runCatching { session.lat }.getOrNull()
        val lng = runCatching { session.lng }.getOrNull()
        val name = session.locationName?.trim().orEmpty()

        return when {
            lat != null && lng != null -> "https://waze.com/ul?ll=$lat,$lng&navigate=$nav"
            name.isNotBlank() -> "https://waze.com/ul?q=${Uri.encode(name)}&navigate=$nav"
            else -> "https://waze.com/ul"
        }
    }

    fun openWaze() {
        val lat = runCatching { session.lat }.getOrNull()
        val lng = runCatching { session.lng }.getOrNull()
        val name = session.locationName?.trim().orEmpty()

        val wazeDeepLink = when {
            lat != null && lng != null -> "waze://?ll=$lat,$lng&navigate=yes"
            name.isNotBlank() -> "waze://?q=${Uri.encode(name)}&navigate=yes"
            else -> "waze://"
        }

        val ok = runCatching {
            ctx.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(wazeDeepLink)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess

        if (!ok) {
            // fallback לדפדפן (יעבוד גם בלי האפליקציה)
            val url = buildWazeUrl(navigate = true)
            runCatching {
                ctx.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure {
                Toast.makeText(ctx, "לא הצלחתי לפתוח את וויז", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareWazeToGroup() {
        val url = buildWazeUrl(navigate = true)
        val title = session.title.trim()
        val place = session.locationName?.trim().orEmpty()

        val text = buildString {
            append("מיקום לאימון: ")
            if (place.isNotBlank()) append(place).append("\n")
            if (title.isNotBlank()) append("אימון: ").append(title).append("\n")
            append("וויז: ").append(url)
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            ctx.startActivity(
                Intent.createChooser(send, "שיתוף מיקום לקבוצה")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            Toast.makeText(ctx, "שיתוף נכשל", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 760.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = detailHorizontal
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = detailHorizontal) {
                Text(
                    text = session.title,
                    color = KmiFreeTextColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    textAlign = detailAlign,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${tr("זמן", "Time")}: ${fmtTime(session.startsAt, isEnglish)}",
                    color = KmiFreeSubTextColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = detailAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(onClick = onClose) {
                Text(
                    text = tr("סגור", "Close"),
                    color = KmiFreeSubTextColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        session.locationName?.takeIf { it.isNotBlank() }?.let { loc ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = detailHorizontal
            ) {
                Text(
                    text = "${tr("מקום", "Location")}: $loc",
                    color = KmiFreeTextColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = detailAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ✅ חדש: כפתורי Waze (ניווט + שיתוף לקבוצה)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = detailHorizontal
            ) {
                OutlinedButton(
                    onClick = { shareWazeToGroup() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Group, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr("שתף לקבוצה", "Share with group"),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = { openWaze() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr("פתח בוויז", "Open in Waze"),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Divider(color = KmiFreeBorderColor)

        Text(
            text = tr("מה הסטטוס שלך?", "What is your status?"),
            color = KmiFreeTextColor,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            textAlign = detailAlign,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp, detailChipAlignment)
        ) {
            StateChip(
                text = tr("אני מגיע", "I'm coming"),
                selected = myState == ParticipantState.GOING,
                selectedColor = Color(0xFF22C55E),
                onClick = {
                    scope.launch {
                        repo.setParticipantState(
                            branch,
                            groupKey,
                            session.id,
                            currentUid,
                            currentName,
                            ParticipantState.GOING
                        )
                    }
                }
            )

            StateChip(
                text = tr("בדרך", "On my way"),
                selected = myState == ParticipantState.ON_WAY,
                selectedColor = Color(0xFF0EA5E9),
                onClick = {
                    scope.launch {
                        repo.setParticipantState(
                            branch,
                            groupKey,
                            session.id,
                            currentUid,
                            currentName,
                            ParticipantState.ON_WAY
                        )
                    }
                }
            )

            StateChip(
                text = tr("הגעתי", "Arrived"),
                selected = myState == ParticipantState.ARRIVED,
                selectedColor = Color(0xFF8B5CF6),
                onClick = {
                    scope.launch {
                        repo.setParticipantState(
                            branch,
                            groupKey,
                            session.id,
                            currentUid,
                            currentName,
                            ParticipantState.ARRIVED
                        )
                    }
                }
            )

            StateChip(
                text = tr("לא יכול", "Can't come"),
                selected = myState == ParticipantState.CANT,
                selectedColor = Color(0xFFEF4444),
                onClick = {
                    scope.launch {
                        repo.setParticipantState(
                            branch,
                            groupKey,
                            session.id,
                            currentUid,
                            currentName,
                            ParticipantState.CANT
                        )
                    }
                }
            )
        } // ✅ סגירה נכונה של ה-Row של הצ'יפים

        Divider(color = KmiFreeBorderColor)

        Text(
            text = tr("מי מגיע?", "Who's coming?"),
            color = KmiFreeTextColor,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            textAlign = detailAlign,
            modifier = Modifier.fillMaxWidth()
        )

        if (branchUsersLoading) {
            Text(
                text = tr("טוען מתאמנים מהסניף...", "Loading branch students..."),
                color = KmiFreeSubTextColor,
                fontWeight = FontWeight.Bold,
                textAlign = detailAlign,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (allParticipants.isEmpty()) {
            Text(
                text = tr("לא נמצאו מתאמנים בסניף הזה.", "No students were found in this branch."),
                color = KmiFreeSubTextColor,
                fontWeight = FontWeight.Bold,
                textAlign = detailAlign,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            allParticipants
                .sortedWith(
                    compareByDescending<FreeSessionPart> { it.updatedAt }
                        .thenBy { it.name }
                )
                .take(100)
                .forEach { p ->
                    ParticipantRow(
                        p = p,
                        isEnglish = isEnglish
                    )
                    Divider(color = KmiFreeBorderColor)
                }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StateChip(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor.copy(alpha = 0.22f),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF0B1220),
            labelColor = Color(0xFFE5E7EB)
        ),
        border = BorderStroke(1.dp, if (selected) selectedColor else Color(0xFF334155))
    )
}

@Composable
private fun ParticipantRow(
    p: FreeSessionPart,
    isEnglish: Boolean
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val rowAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
    val rowHorizontal = if (isEnglish) Alignment.Start else Alignment.End
    val rowArrangement = if (isEnglish) Arrangement.Start else Arrangement.End

    val stateLabel = when (p.state) {
        ParticipantState.GOING -> tr("מגיע", "Coming")
        ParticipantState.ON_WAY -> tr("בדרך", "On the way")
        ParticipantState.ARRIVED -> tr("הגיע", "Arrived")
        ParticipantState.CANT -> tr("לא יכול", "Can't come")
        ParticipantState.INVITED -> tr("הוזמן", "Invited")
    }

    val stateColor = when (p.state) {
        ParticipantState.GOING -> Color(0xFF22C55E)
        ParticipantState.ON_WAY -> Color(0xFF0EA5E9)
        ParticipantState.ARRIVED -> Color(0xFF8B5CF6)
        ParticipantState.CANT -> Color(0xFFEF4444)
        ParticipantState.INVITED -> Color(0xFF64748B)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = rowArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEnglish) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(stateColor, CircleShape)
            )

            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = rowHorizontal
        ) {
            Text(
                text = p.name,
                color = KmiFreeTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                textAlign = rowAlign,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stateLabel,
                color = stateColor,
                style = MaterialTheme.typography.labelSmall,
                textAlign = rowAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!isEnglish) {
            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(stateColor, CircleShape)
            )
        }
    }
}

@Composable
private fun PremiumFreeSessionDatePickerDialog(
    selectedMillis: Long,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val zone = ZoneId.systemDefault()
    val locale = if (isEnglish) Locale.ENGLISH else Locale("he", "IL")
    val direction = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    val align = if (isEnglish) TextAlign.Start else TextAlign.Right
    val horizontal = if (isEnglish) Alignment.Start else Alignment.End

    val selectedDate = remember(selectedMillis) {
        Instant.ofEpochMilli(selectedMillis)
            .atZone(zone)
            .toLocalDate()
    }

    var visibleMonth by remember(selectedDate) {
        mutableStateOf(YearMonth.from(selectedDate))
    }

    val today = remember {
        LocalDate.now()
    }

    val firstDayOfMonth = remember(visibleMonth) {
        visibleMonth.atDay(1)
    }

    val leadingEmptyDays = remember(firstDayOfMonth) {
        firstDayOfMonth.dayOfWeek.value % 7
    }

    val daysInMonth = remember(visibleMonth) {
        visibleMonth.lengthOfMonth()
    }

    val monthTitle = remember(visibleMonth, isEnglish) {
        visibleMonth.atDay(1)
            .format(
                if (isEnglish) {
                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
                } else {
                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale("he", "IL"))
                }
            )
    }

    val selectedTitle = remember(selectedDate, isEnglish) {
        selectedDate.format(
            if (isEnglish) {
                DateTimeFormatter.ofPattern("EEEE • MMMM d, yyyy", Locale.ENGLISH)
            } else {
                DateTimeFormatter.ofPattern("EEEE • d MMMM yyyy", Locale("he", "IL"))
            }
        )
    }

    fun dateToMillis(date: LocalDate): Long {
        return date
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color.Transparent,
            shadowElevation = 22.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                KmiFreeBorderColorStrong,
                                KmiFreeBorderColor,
                                KmiFreeBgTop
                            )
                        )
                    )
                    .padding(1.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(29.dp),
                    color = KmiFreeBgMid.copy(alpha = 0.98f),
                    tonalElevation = 0.dp
                ) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides direction
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = horizontal
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = horizontal
                                ) {
                                    Text(
                                        text = tr("בחר תאריך לאימון", "Choose session date"),
                                        color = KmiFreeSubTextColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = align,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = selectedTitle,
                                        color = KmiFreeTextColor,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 23.sp,
                                            lineHeight = 27.sp
                                        ),
                                        textAlign = align,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.09f),
                                    border = BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.18f)
                                    )
                                ) {
                                    Text(
                                        text = "📅",
                                        fontSize = 22.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Divider(color = KmiFreeBorderColor)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = {
                                        visibleMonth = visibleMonth.minusMonths(1)
                                    },
                                    shape = CircleShape,
                                    color = KmiFreeBorderColorStrong,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isEnglish) "‹" else "›",
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Text(
                                    text = monthTitle,
                                    color = KmiFreeTextColor,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 22.sp,
                                        lineHeight = 25.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )

                                Surface(
                                    onClick = {
                                        visibleMonth = visibleMonth.plusMonths(1)
                                    },
                                    shape = CircleShape,
                                    color = KmiFreeBorderColorStrong,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isEnglish) "›" else "‹",
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            val weekDays = if (isEnglish) {
                                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            } else {
                                listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(KmiFreeCardColorSoft.copy(alpha = 0.74f))
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                weekDays.forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        color = KmiFreeTitleColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = if (isEnglish) 12.sp else 15.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            val cells = buildList<Int?> {
                                repeat(leadingEmptyDays) { add(null) }

                                for (day in 1..daysInMonth) {
                                    add(day)
                                }

                                while (size % 7 != 0) {
                                    add(null)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(KmiFreeCardColorSoft.copy(alpha = 0.74f))
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                cells.chunked(7).forEach { week ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        week.forEach { day ->
                                            val cellDate = day?.let {
                                                visibleMonth.atDay(it)
                                            }

                                            val isSelected = cellDate == selectedDate
                                            val isToday = cellDate == today

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (day != null && cellDate != null) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clickable {
                                                                onDateSelected(
                                                                    dateToMillis(cellDate)
                                                                )
                                                            },
                                                        shape = CircleShape,
                                                        color = when {
                                                            isSelected -> Color(0xFF22D3EE)
                                                            isToday -> Color.White.copy(alpha = 0.14f)
                                                            else -> Color.Transparent
                                                        },
                                                        border = when {
                                                            isSelected -> null
                                                            isToday -> BorderStroke(
                                                                1.dp,
                                                                Color(0xFF22D3EE)
                                                            )

                                                            else -> null
                                                        }
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = day.toString(),
                                                                color = if (isSelected) {
                                                                    Color(0xFF020617)
                                                                } else {
                                                                    KmiFreeTextColor
                                                                },
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 16.sp,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.07f),
                                border = BorderStroke(
                                    1.dp,
                                    Color.White.copy(alpha = 0.12f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = if (isEnglish) {
                                        Arrangement.End
                                    } else {
                                        Arrangement.Start
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = onDismiss
                                    ) {
                                        Text(
                                            text = tr("ביטול", "Cancel"),
                                            color = KmiFreeSubTextColor,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Surface(
                                        onClick = {
                                            onDateSelected(dateToMillis(today))
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color(0xFF22D3EE),
                                        shadowElevation = 5.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(
                                                horizontal = 24.dp,
                                                vertical = 10.dp
                                            ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tr("היום", "Today"),
                                                color = Color(0xFF04101F),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeQuickPicker(
    startsAt: Long,
    isEnglish: Boolean,
    onPick: (Long) -> Unit
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var customDateMillis by remember { mutableStateOf<Long?>(null) }
    var customHour by remember { mutableStateOf(19) }
    var customMinute by remember { mutableStateOf(0) }

    fun buildMillisFromDateAndTime(dateMillis: Long, hour: Int, minute: Int): Long {
        val z = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(dateMillis).atZone(z).toLocalDate()

        return date
            .atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            .atZone(z)
            .toInstant()
            .toEpochMilli()
    }

    Surface(
        onClick = {
            if (customDateMillis == null) {
                customDateMillis = startsAt
            }

            showDatePicker = true
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF22D3EE),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tr("בחר יום ושעה", "Choose day and time"),
                color = Color(0xFF04101F),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showDatePicker) {
        PremiumFreeSessionDatePickerDialog(
            selectedMillis = customDateMillis ?: startsAt,
            isEnglish = isEnglish,
            onDismiss = {
                showDatePicker = false
            },
            onDateSelected = { selectedMillis ->
                customDateMillis = selectedMillis
                showDatePicker = false
                showTimePicker = true
            }
        )
    }

    if (showTimePicker) {
        val selectedTime = remember(startsAt) {
            Instant.ofEpochMilli(startsAt)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }

        val tp = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Color(0xFF061832),
            title = {
                Text(
                    text = tr("בחר שעה לאימון", "Choose session time"),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    customHour = tp.hour
                    customMinute = tp.minute
                    showTimePicker = false

                    val d = customDateMillis ?: startsAt
                    onPick(buildMillisFromDateAndTime(d, customHour, customMinute))
                }) {
                    Text(
                        text = tr("אישור", "OK"),
                        color = Color(0xFF22D3EE),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(
                        text = tr("ביטול", "Cancel"),
                        color = KmiFreeSubTextColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    TimePicker(state = tp)
                }
            }
        )
    }
}

/* ---------------- helpers ---------------- */

private fun fmtTime(millis: Long, isEnglish: Boolean): String {
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())

    val fmt = if (isEnglish) {
        DateTimeFormatter.ofPattern("EEEE · MMM d, yyyy · HH:mm", Locale.ENGLISH)
    } else {
        DateTimeFormatter.ofPattern("EEEE · d.M.yyyy · HH:mm", Locale("he", "IL"))
    }

    return dt.format(fmt)
}

private fun forceTodayAtHour(hour24: Int): Long {
    val z = ZoneId.systemDefault()
    val now = Instant.now().atZone(z)
    val today = now.toLocalDate()
    val dt = today.atTime(hour24.coerceIn(0, 23), 0).atZone(z)
    return dt.toInstant().toEpochMilli()
}

private fun forceTomorrowAtHour(hour24: Int): Long {
    val z = ZoneId.systemDefault()
    val now = Instant.now().atZone(z)
    val tomorrow = now.toLocalDate().plusDays(1)
    val dt = tomorrow.atTime(hour24.coerceIn(0, 23), 0).atZone(z)
    return dt.toInstant().toEpochMilli()
}

/**
 * תחליף קטן כדי לא להוסיף פה import של collectAsStateWithLifecycle.
 * עובד טוב למסכים שלך.
 */
@Composable
private fun <T> StateFlow<T>.collectAsStateCompat() =
    collectAsState()
