package il.kmi.app.attendance.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import il.kmi.app.attendance.data.AttendanceRecord
import il.kmi.app.attendance.data.AttendanceStatus
import il.kmi.app.attendance.data.GroupMember
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.training.TrainingCatalog
import kotlinx.coroutines.Job
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class AttendanceUiState(
    val date: LocalDate = LocalDate.now(),
    val branch: String = "",
    val groupKey: String = "",
    val availableBranches: List<String> = emptyList(),
    val availableGroups: List<String> = emptyList(),
    val hasScheduledTraining: Boolean = false,
    val sessionId: Long? = null,
    val members: List<GroupMember> = emptyList(),
    val records: List<AttendanceRecord> = emptyList(),
    val statusByMemberId: Map<Long, AttendanceStatus> = emptyMap()
)

sealed class UiEvent {
    data class ReportSaved(val branch: String, val groupKey: String) : UiEvent()
    data class ReportSaveFailed(val message: String) : UiEvent()
}

class AttendanceViewModel(app: Application) : AndroidViewModel(app) {

    private fun String.nameKey(): String = this
        .trim()
        .replace('־', '-')   // maqaf
        .replace('–', '-')   // en-dash
        .replace('—', '-')   // em-dash
        .replace(Regex("\\s+"), " ")
        .replace(Regex("""[."'\u05F3\u05F4,;:()\\[\\]{}]"""), "")
        .lowercase()

    private fun String.phoneKey(): String = filter { it.isDigit() }
        .removePrefix("972")
        .removePrefix("0")

    private fun GroupMember.attendanceUniqueKey(): String {
        val phoneKey = phone.orEmpty().phoneKey()
        val nameKey = displayName.nameKey()

        return when {
            phoneKey.isNotBlank() -> "phone:$phoneKey"
            nameKey.isNotBlank() -> "name:${nameKey.substringBefore(" ")}"
            else -> "member:$id"
        }
    }

    private fun cleanupDuplicateMembersInDb() {
        val members = uiState.value.members
        if (members.size <= 1) return

        val dups = members
            .groupBy { it.attendanceUniqueKey() }
            .values
            .filter { it.size > 1 }

        if (dups.isEmpty()) return

        viewModelScope.launch {
            dups.forEach { group ->
                group
                    .sortedBy { it.id }
                    .drop(1)
                    .forEach { m ->
                        runCatching {
                            repo.removeMember(_branch.value, _groupKey.value, m.id)
                        }
                    }
            }

            _refreshTick.update { it + 1 }
        }
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val repo: AttendanceRepository = AttendanceRepository.get(app)

    // ✅ NEW: bootstrap state
    private var bootstrapJob: Job? = null
    private var lastBootstrapKey: String? = null
    private var lastCleanupKey: String? = null

    private val _date =
        MutableStateFlow(LocalDate.now())

    private val _branch =
        MutableStateFlow("")

    private val _groupKey =
        MutableStateFlow("")

    /*
     * כל הסניפים המשויכים למאמן, ללא סינון תאריך.
     * הרשימה נשמרת כדי שאפשר יהיה לסנן אותה מחדש
     * בכל החלפת תאריך.
     */
    private val _allAvailableBranches =
        MutableStateFlow<List<String>>(emptyList())

    /*
     * הרשימות שמוצגות בפועל במסך לאחר סינון
     * לפי התאריך הנבחר.
     */
    private val _availableBranches =
        MutableStateFlow<List<String>>(emptyList())

    private val _availableGroups =
        MutableStateFlow<List<String>>(emptyList())

    private val _sessionId =
        MutableStateFlow<Long?>(null)

    // 🔄 טיקט רענון – כשמעלים אותו, זורם חדש מתחבר למקורות (ועושה re-query)
    private val _refreshTick = MutableStateFlow(0)

    private data class AttendanceContext(
        val branch: String,
        val groupKey: String,
        val date: LocalDate
    )

    private val membersFlow: Flow<List<GroupMember>> =
        combine(
            _branch,
            _groupKey,
            _date,
            _refreshTick
        ) { branch, groupKey, date, _ ->
            AttendanceContext(
                branch = branch,
                groupKey = groupKey,
                date = date
            )
        }.flatMapLatest { context ->
            val hasTraining =
                TrainingCatalog.hasTrainingOn(
                    date = context.date,
                    branch = context.branch,
                    group = context.groupKey
                )

            if (
                context.branch.isBlank() ||
                context.groupKey.isBlank() ||
                !hasTraining
            ) {
                flowOf(emptyList())
            } else {
                repo.members(
                    branch = context.branch,
                    groupKey = context.groupKey
                )
            }
        }

    private val recordsFlow: Flow<List<AttendanceRecord>> =
        combine(
            _branch,
            _groupKey,
            _date,
            _refreshTick
        ) { branch, groupKey, date, _ ->
            AttendanceContext(
                branch = branch,
                groupKey = groupKey,
                date = date
            )
        }.flatMapLatest { context ->
            val hasTraining =
                TrainingCatalog.hasTrainingOn(
                    date = context.date,
                    branch = context.branch,
                    group = context.groupKey
                )

            if (
                context.branch.isBlank() ||
                context.groupKey.isBlank() ||
                !hasTraining
            ) {
                flowOf(emptyList())
            } else {
                repo.attendanceForDay(
                    branch = context.branch,
                    groupKey = context.groupKey,
                    date = context.date
                )
            }
        }

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
    private val headerFlow =
        combine(_date, _branch, _groupKey, _sessionId) { d, b, g, s -> Quad(d, b, g, s) }

    val uiState: StateFlow<AttendanceUiState> =
        combine(
            headerFlow,
            membersFlow,
            recordsFlow,
            _availableBranches,
            _availableGroups
        ) { h, members, records, availableBranches, availableGroups ->

            val hasScheduledTraining =
                TrainingCatalog.hasTrainingOn(
                    date = h.first,
                    branch = h.second,
                    group = h.third
                )

            val visibleMembers =
                if (hasScheduledTraining) {
                    members
                } else {
                    emptyList()
                }

            val visibleRecords =
                if (hasScheduledTraining) {
                    records
                } else {
                    emptyList()
                }

            val map: Map<Long, AttendanceStatus> =
                visibleRecords.associate { record ->
                    record.memberId to record.status
                }

            AttendanceUiState(
                date = h.first,
                branch = h.second,
                groupKey = h.third,
                availableBranches = availableBranches,
                availableGroups = availableGroups,
                hasScheduledTraining = hasScheduledTraining,
                sessionId =
                    if (hasScheduledTraining) {
                        h.fourth
                    } else {
                        null
                    },
                members = visibleMembers,
                records = visibleRecords,
                statusByMemberId = map
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AttendanceUiState()
        )

    init {
        viewModelScope.launch {
            uiState
                .map { state ->
                    Triple(
                        state.branch.trim(),
                        state.groupKey.trim(),
                        state.members.map { it.attendanceUniqueKey() }
                    )
                }
                .distinctUntilChanged()
                .collect { (branch, groupKey, names) ->
                    if (branch.isBlank() || groupKey.isBlank()) return@collect
                    if (names.size <= 1) return@collect

                    val key = "$branch|$groupKey|${names.sorted().joinToString("|")}"
                    if (lastCleanupKey == key) return@collect

                    val hasDuplicates = names
                        .filter { it.isNotBlank() }
                        .groupBy { it }
                        .any { (_, sameMembers) -> sameMembers.size > 1 }

                    if (hasDuplicates) {
                        lastCleanupKey = key
                        cleanupDuplicateMembersInDb()
                    }
                }
        }
    }

    fun setContext(
        date: LocalDate,
        branch: String,
        groupKey: String
    ) {
        val allBranches =
            collectAttendanceOptions(
                incoming = branch,
                prefKeys = listOf(
                    "active_branch",
                    "branch",
                    "branches",
                    "coach_branch",
                    "coach_branches",
                    "coachBranches"
                )
            )
                .map { branchName ->
                    branchName.normAttendanceOption()
                }
                .filter { branchName ->
                    branchName.isNotBlank()
                }
                .distinct()

        val sp =
            getApplication<Application>()
                .getSharedPreferences(
                    "kmi_user",
                    Context.MODE_PRIVATE
                )

        val activeBranch =
            sp.getString(
                "active_branch",
                ""
            )
                ?.normAttendanceOption()
                .orEmpty()

        val activeGroup =
            sp.getString(
                "active_group",
                ""
            )
                ?.normAttendanceOption()
                .orEmpty()

        _allAvailableBranches.value =
            allBranches

        val preferredBranch =
            when {
                _branch.value.isNotBlank() ->
                    _branch.value

                activeBranch.isNotBlank() ->
                    activeBranch

                else ->
                    branch.normAttendanceOption()
            }

        val preferredGroup =
            when {
                _groupKey.value.isNotBlank() ->
                    _groupKey.value

                activeGroup.isNotBlank() ->
                    activeGroup

                else ->
                    groupKey.normAttendanceOption()
            }

        refreshScheduleOptionsForDate(
            date = date,
            preferredBranch = preferredBranch,
            preferredGroup = preferredGroup
        )

        ensureSession()
        _refreshTick.update { it + 1 }
    }

    private fun String.normAttendanceOption(): String = trim()
        .replace('־', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replace(Regex("\\s+"), " ")

    private fun splitAttendanceOptions(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()

        return raw
            .replace(" • ", ",")
            .replace("|", ",")
            .replace("\n", ",")
            .split(',', ';', '；')
            .map { it.normAttendanceOption() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun collectAttendanceOptions(
        incoming: String,
        prefKeys: List<String>
    ): List<String> {
        val sp = getApplication<Application>()
            .getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

        val fromIncoming = splitAttendanceOptions(incoming)

        fun readPrefOptionValues(key: String): List<String> {
            val raw: Any? = sp.all[key]

            return when (raw) {
                is String -> {
                    splitAttendanceOptions(raw)
                }

                is Set<*> -> {
                    raw
                        .mapNotNull { it?.toString() }
                        .flatMap { splitAttendanceOptions(it) }
                }

                is List<*> -> {
                    raw
                        .mapNotNull { it?.toString() }
                        .flatMap { splitAttendanceOptions(it) }
                }

                else -> {
                    emptyList()
                }
            }
        }

        val fromPrefs = prefKeys.flatMap { key ->
            readPrefOptionValues(key)
        }

        return (fromIncoming + fromPrefs)
            .map { it.normAttendanceOption() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * מסנן את הסניפים והקבוצות לפי התאריך.
     *
     * availableBranches יכיל רק סניפים שיש בהם
     * לפחות אימון אחד בתאריך המבוקש.
     *
     * availableGroups יכיל רק קבוצות שיש להן
     * אימון בסניף ובתאריך שנבחרו.
     */
    private fun refreshScheduleOptionsForDate(
        date: LocalDate,
        preferredBranch: String = _branch.value,
        preferredGroup: String = _groupKey.value
    ) {
        val activeBranches =
            _allAvailableBranches.value
                .asSequence()
                .map { branch ->
                    branch.normAttendanceOption()
                }
                .filter { branch ->
                    branch.isNotBlank()
                }
                .filter { branch ->
                    TrainingCatalog.hasTrainingOn(
                        date = date,
                        branch = branch,
                        group = null
                    )
                }
                .distinct()
                .toList()

        val cleanPreferredBranch =
            preferredBranch.normAttendanceOption()

        val selectedBranch =
            activeBranches.firstOrNull { branch ->
                branch == cleanPreferredBranch
            }
                ?: activeBranches.firstOrNull()
                    .orEmpty()

        val activeGroups =
            if (selectedBranch.isBlank()) {
                emptyList()
            } else {
                TrainingCatalog.groupsForBranch(
                    branch = selectedBranch,
                    isEnglish = false
                )
                    .asSequence()
                    .map { group ->
                        group.normAttendanceOption()
                    }
                    .filter { group ->
                        group.isNotBlank()
                    }
                    .filter { group ->
                        TrainingCatalog.hasTrainingOn(
                            date = date,
                            branch = selectedBranch,
                            group = group
                        )
                    }
                    .distinct()
                    .toList()
            }

        val cleanPreferredGroup =
            preferredGroup.normAttendanceOption()

        val preferredGroupNormalized =
            TrainingCatalog.normalizeGroupName(
                cleanPreferredGroup
            )

        val selectedGroup =
            activeGroups.firstOrNull { group ->
                group == cleanPreferredGroup ||
                        TrainingCatalog.normalizeGroupName(group) ==
                        preferredGroupNormalized
            }
                ?: activeGroups.firstOrNull()
                    .orEmpty()

        _date.value = date
        _availableBranches.value = activeBranches
        _branch.value = selectedBranch
        _availableGroups.value = activeGroups
        _groupKey.value = selectedGroup
        _sessionId.value = null
    }

    fun selectAttendanceDate(date: LocalDate) {
        val today =
            LocalDate.now()

        val safeDate =
            if (date.isAfter(today)) {
                today
            } else {
                date
            }

        refreshScheduleOptionsForDate(
            date = safeDate,
            preferredBranch = _branch.value,
            preferredGroup = _groupKey.value
        )

        ensureSession()
        _refreshTick.update { it + 1 }
    }

    fun selectBranch(branch: String) {
        val cleanBranch =
            branch.normAttendanceOption()

        /*
         * ניתן לבחור רק סניף שנמצא פעיל
         * בתאריך הנבחר.
         */
        val selectedBranch =
            _availableBranches.value
                .firstOrNull { activeBranch ->
                    activeBranch == cleanBranch
                }
                ?: return

        val activeGroups =
            TrainingCatalog.groupsForBranch(
                branch = selectedBranch,
                isEnglish = false
            )
                .asSequence()
                .map { group ->
                    group.normAttendanceOption()
                }
                .filter { group ->
                    group.isNotBlank()
                }
                .filter { group ->
                    TrainingCatalog.hasTrainingOn(
                        date = _date.value,
                        branch = selectedBranch,
                        group = group
                    )
                }
                .distinct()
                .toList()

        val currentGroupNormalized =
            TrainingCatalog.normalizeGroupName(
                _groupKey.value
            )

        val selectedGroup =
            activeGroups.firstOrNull { group ->
                group == _groupKey.value ||
                        TrainingCatalog.normalizeGroupName(group) ==
                        currentGroupNormalized
            }
                ?: activeGroups.firstOrNull()
                    .orEmpty()

        _branch.value = selectedBranch
        _availableGroups.value = activeGroups
        _groupKey.value = selectedGroup
        _sessionId.value = null

        getApplication<Application>()
            .getSharedPreferences(
                "kmi_user",
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                "active_branch",
                selectedBranch
            )
            .putString(
                "active_group",
                selectedGroup
            )
            .apply()

        ensureSession()
        _refreshTick.update { it + 1 }
    }

    fun selectGroup(groupKey: String) {
        val clean =
            groupKey.normAttendanceOption()

        val relevantGroup =
            _availableGroups.value.firstOrNull { group ->
                group == clean ||
                        TrainingCatalog.normalizeGroupName(group) ==
                        TrainingCatalog.normalizeGroupName(clean)
            }
                ?: return

        if (_groupKey.value == relevantGroup) {
            return
        }

        _groupKey.value = relevantGroup
        _sessionId.value = null

        getApplication<Application>()
            .getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
            .edit()
            .putString("active_group", clean)
            .apply()

        ensureSession()
        _refreshTick.update { it + 1 }
    }

    fun ensureSession() {
        val d = _date.value
        val b = _branch.value
        val g = _groupKey.value

        val hasTraining =
            TrainingCatalog.hasTrainingOn(
                date = d,
                branch = b,
                group = g
            )

        if (
            b.isBlank() ||
            g.isBlank() ||
            !hasTraining
        ) {
            _sessionId.value = null
            return
        }

        viewModelScope.launch {
            val result = runCatching {
                repo.ensureSession(date = d, branch = b, groupKey = g)
            }

            result.onSuccess { id ->
                _sessionId.value = id
            }.onFailure {
                _events.tryEmit(
                    UiEvent.ReportSaveFailed(
                        message = "Failed opening attendance session"
                    )
                )
            }
        }
    }

    /** הוספת מתאמן בשם בלבד + רענון UI */
    fun addMember(name: String) {
        val b = _branch.value.trim()
        val g = _groupKey.value.trim()
        val cleanName = name.trim()

        if (b.isBlank() || g.isBlank() || cleanName.isBlank()) return

        viewModelScope.launch {
            runCatching {
                repo.addMember(
                    branch = b,
                    groupKey = g,
                    displayName = cleanName
                )
            }.onSuccess {
                _refreshTick.update { it + 1 }
            }.onFailure {
                _events.tryEmit(
                    UiEvent.ReportSaveFailed(
                        message = "Failed adding trainee: $cleanName"
                    )
                )
            }
        }
    }

    /** סימון נוכחות למתאמן + רענון קל */
    fun mark(memberId: Long, status: AttendanceStatus) {
        val sid = _sessionId.value

        if (sid == null) {
            _events.tryEmit(
                UiEvent.ReportSaveFailed(
                    message = "Attendance session is not ready yet"
                )
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                repo.mark(
                    sessionId = sid,
                    memberId = memberId,
                    status = status
                )
            }.onSuccess {
                _refreshTick.update { it + 1 }
            }.onFailure {
                _events.tryEmit(
                    UiEvent.ReportSaveFailed(
                        message = "Failed marking attendance"
                    )
                )
            }
        }
    }

    fun mark(memberId: String, status: AttendanceStatus) {
        memberId.toLongOrNull()?.let { mark(it, status) }
    }

    /** מחיקת מתאמן לפי memberId (Long) + רענון UI */
    fun removeMember(memberId: Long) {
        val b = _branch.value
        val g = _groupKey.value
        if (b.isBlank() || g.isBlank()) return

        viewModelScope.launch {
            val result = runCatching {
                repo.removeMember(b, g, memberId)
            }

            if (result.isFailure) {
                _events.tryEmit(
                    UiEvent.ReportSaveFailed(
                        message = "Failed removing trainee"
                    )
                )
                return@launch
            }

            _refreshTick.update { it + 1 }
        }
    }

    fun removeMember(memberId: String) {
        memberId.toLongOrNull()?.let { removeMember(it) }
    }

    /** 🔹 שמירת דו"ח נוכחות של האימון הנוכחי לארכיון */
    fun saveTodayReport() {
        val d = _date.value
        val b = _branch.value
        val g = _groupKey.value

        val hasTraining =
            TrainingCatalog.hasTrainingOn(
                date = d,
                branch = b,
                group = g
            )

        if (
            b.isBlank() ||
            g.isBlank() ||
            !hasTraining
        ) {
            _events.tryEmit(
                UiEvent.ReportSaveFailed(
                    message =
                        "No scheduled training for the selected date"
                )
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                repo.saveReportForDate(
                    branch = b,
                    groupKey = g,
                    date = d
                )
            }.onSuccess {
                _events.tryEmit(UiEvent.ReportSaved(branch = b, groupKey = g))
            }.onFailure { t ->
                _events.tryEmit(
                    UiEvent.ReportSaveFailed(
                        message = t.message ?: t.toString()
                    )
                )
            }
        }
    }

    /**
     * ✅ טעינת מתאמנים אוטומטית מתוך collection "users" לפי סניף + קבוצה.
     * רץ ב-viewModelScope כדי שלא יתבטל בגלל יציאה מהקומפוזיציה.
     */
    fun bootstrapMembersFromUsers(
        branchBase: String,
        groupBase: String
    ) {
        val b0 = branchBase.trim()
        val g0 = groupBase.trim()
        val selectedDate = _date.value

        val hasTraining =
            TrainingCatalog.hasTrainingOn(
                date = selectedDate,
                branch = b0,
                group = g0
            )

        if (
            b0.isBlank() ||
            g0.isBlank() ||
            !hasTraining
        ) {
            bootstrapJob?.cancel()
            bootstrapJob = null
            return
        }

        val key = "$selectedDate|$b0|$g0"

        if (lastBootstrapKey == key && bootstrapJob?.isActive == true) {
            return
        }

        lastBootstrapKey = key

        bootstrapJob?.cancel()
        bootstrapJob = viewModelScope.launch(Dispatchers.IO) {

            fun String.norm(): String {
                val t = this.trim()
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

            val branchCandidates = listOf(
                b0,
                b0.swapDash('-'),
                b0.swapDash('–'),
                b0.swapDash('—'),
                b0.swapDash('־'),
                b0.replace("  ", " "),
            ).map { it.trim() }.distinct()

            val groupCandidates = listOf(
                g0,
                g0.replace("-", "–"),
                g0.replace("–", "-"),
            ).map { it.trim() }.distinct()

            fun DocumentSnapshot.userNameOrNull(): String? {
                val full = getString("fullName")
                    ?: getString("name")
                    ?: getString("displayName")

                if (!full.isNullOrBlank()) return full

                val first = getString("firstName")?.trim().orEmpty()
                val last  = getString("lastName")?.trim().orEmpty()
                val joined = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ").trim()
                return joined.ifBlank { null }
            }

            fun DocumentSnapshot.isCoachDoc(): Boolean {
                val isCoachFlag = (get("isCoach") as? Boolean) == true
                if (isCoachFlag) return true

                val role = (getString("role") ?: getString("userType") ?: getString("type"))
                    ?.trim()
                    ?.lowercase()
                    .orEmpty()

                if ("coach" in role || "trainer" in role || "instructor" in role || "admin" in role) return true
                if ("מאמן" in role) return true
                return false
            }

            fun DocumentSnapshot.isTraineeDoc(): Boolean {
                val role = (getString("role") ?: getString("userType") ?: getString("type"))
                    ?.trim()
                    ?.lowercase()
                    .orEmpty()

                // אם אין role – נניח מתאמן
                if (role.isBlank()) return true

                // מתאמן מפורש
                if ("trainee" in role) return true
                if ("מתאמן" in role) return true

                // אם זה נראה כמו מאמן/אדמין – לא מתאמן
                if ("coach" in role || "trainer" in role || "instructor" in role) return false
                if ("admin" in role || "manager" in role) return false
                if ("מאמן" in role) return false

                // אחרת – עדיף לא להפיל (רוב הסיכויים שזה משתמש רגיל)
                return true
            }

            fun DocumentSnapshot.hasGroupMatch(): Boolean {
                if (groupCandidates.firstOrNull().isNullOrBlank()) return true

                fun splitTokens(raw: String?): List<String> {
                    if (raw.isNullOrBlank()) return emptyList()

                    return raw
                        .replace(" • ", ",")
                        .replace("|", ",")
                        .replace("\n", ",")
                        .replace("+", ",")
                        .replace("/", ",")
                        .split(',', ';', '；')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .map { it.norm() }
                }

                fun expandGroupAliases(raw: String): List<String> {
                    val n = raw.norm()

                    return buildList {
                        add(n)
                        addAll(splitTokens(n))

                        if (n.contains("נוער") && n.contains("בוגרים")) {
                            add("נוער")
                            add("בוגרים")
                            add("נוער ובוגרים")
                            add("נוער + בוגרים")
                        }

                        if (n.contains("children", ignoreCase = true)) add("ילדים")
                        if (n.contains("youth", ignoreCase = true)) add("נוער")
                        if (n.contains("adults", ignoreCase = true)) add("בוגרים")
                    }
                        .map { it.norm() }
                        .filter { it.isNotBlank() }
                        .distinct()
                }

                val gList = (get("groups") as? List<*>)
                    ?.mapNotNull { it?.toString()?.trim() }
                    ?.flatMap { expandGroupAliases(it) }
                    .orEmpty()

                val primary = getString("primaryGroup")?.trim()
                val groupKeyField = getString("groupKey")?.trim()

                val groupField = getString("group")?.trim()
                val groupName = getString("groupName")?.trim()
                val groupsCsv = getString("groupsCsv")?.trim()
                val groupCsv = getString("groupCsv")?.trim()

                val tokenBag = buildList {
                    addAll(gList)
                    addAll(splitTokens(primary).flatMap { expandGroupAliases(it) })
                    addAll(splitTokens(groupKeyField).flatMap { expandGroupAliases(it) })
                    addAll(splitTokens(groupField).flatMap { expandGroupAliases(it) })
                    addAll(splitTokens(groupName).flatMap { expandGroupAliases(it) })
                    addAll(splitTokens(groupsCsv).flatMap { expandGroupAliases(it) })
                    addAll(splitTokens(groupCsv).flatMap { expandGroupAliases(it) })
                }.filter { it.isNotBlank() }.distinct()

                if (tokenBag.isEmpty()) return true

                val candNorm = groupCandidates
                    .flatMap { expandGroupAliases(it) }
                    .map { it.norm() }
                    .toSet()

                return tokenBag.any { tok ->
                    tok in candNorm ||
                            candNorm.any { cand ->
                                cand.length >= 2 &&
                                        tok.length >= 2 &&
                                        (tok.contains(cand) || cand.contains(tok))
                            }
                }
            }

            suspend fun fetchUsersFor(branchValue: String): List<DocumentSnapshot> {
                val col = Firebase.firestore.collection("users")
                val out = mutableListOf<DocumentSnapshot>()

                runCatching { out.addAll(col.whereArrayContains("branches", branchValue).get().await().documents) }
                runCatching { out.addAll(col.whereEqualTo("branchesCsv", branchValue).get().await().documents) }
                runCatching { out.addAll(col.whereEqualTo("branch", branchValue).get().await().documents) }

                return out
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
                    .map { it.norm() }
            }

            fun DocumentSnapshot.branchesListNorm(): List<String> =
                (get("branches") as? List<*>)?.mapNotNull { it?.toString() }?.map { it.norm() } ?: emptyList()

            fun DocumentSnapshot.branchTokensNorm(): List<String> {
                val out = mutableListOf<String>()
                out.addAll(branchesListNorm())
                out.addAll(splitTokensNorm(getString("branchesCsv")))
                out.addAll(splitTokensNorm(getString("branch")))
                return out.filter { it.isNotBlank() }.distinct()
            }

            fun matchesBranch(tokens: List<String>, candSet: Set<String>): Boolean {
                if (tokens.isEmpty() || candSet.isEmpty()) return false
                return tokens.any { tok ->
                    tok in candSet ||
                            candSet.any { cand ->
                                cand.length >= 4 && (tok.contains(cand) || cand.contains(tok))
                            }
                }
            }

            var docs = branchCandidates
                .flatMap { cand -> fetchUsersFor(cand) }
                .distinctBy { it.id }

            if (docs.isEmpty()) {
                val col = Firebase.firestore.collection("users")
                val all = mutableListOf<DocumentSnapshot>()

                var last: DocumentSnapshot? = null
                while (true) {
                    var q = col
                        .orderBy(FieldPath.documentId())
                        .limit(1000)

                    if (last != null) q = q.startAfter(last!!)

                    val snap = q.get().await()
                    val page = snap.documents
                    if (page.isEmpty()) break

                    all.addAll(page)
                    last = page.last()

                    if (all.size >= 5000) break
                }

                val candNorm = branchCandidates.map { it.norm() }.toSet()

                docs = all.filter { d ->
                    if (d.isCoachDoc()) return@filter false
                    matchesBranch(d.branchTokensNorm(), candNorm)
                }.distinctBy { it.id }
            }

            val stepNotCoach = docs.filter { !it.isCoachDoc() }
            val stepTrainee  = stepNotCoach.filter { it.isTraineeDoc() }
            val stepGroup    = stepTrainee.filter { it.hasGroupMatch() }

            val names = stepGroup
                .asSequence()
                .mapNotNull { it.userNameOrNull() }
                .map { it.norm() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .toList()

            val existingNameKeys = uiState.value.members
                .asSequence()
                .map { it.attendanceUniqueKey() }
                .toSet()

            val toAdd = names
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filter { name ->
                    val nameKey = name.nameKey()
                    val uniqueKey = if (nameKey.isNotBlank()) {
                        "name:${nameKey.substringBefore(" ")}"
                    } else {
                        ""
                    }

                    uniqueKey.isNotBlank() && uniqueKey !in existingNameKeys
                }
                .toList()

            val repoBranch = _branch.value.ifBlank { b0 }.trim()
            val repoGroup = _groupKey.value.ifBlank { g0 }.trim()

            toAdd.forEach { n ->
                runCatching {
                    repo.addMember(
                        branch = repoBranch,
                        groupKey = repoGroup,
                        displayName = n
                    )
                }
            }

            _refreshTick.update { it + 1 }

            // ✅ ניקוי כפילויות קיימות ב-DB / Firestore לפי המימוש הנוכחי של ה-Repository
            cleanupDuplicateMembersInDb()
        }
    }
}