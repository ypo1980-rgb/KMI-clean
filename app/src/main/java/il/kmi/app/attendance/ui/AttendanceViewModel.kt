package il.kmi.app.attendance.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import il.kmi.app.attendance.data.AttendanceRecord
import il.kmi.app.attendance.data.AttendanceStatus
import il.kmi.app.attendance.data.GroupMember
import il.kmi.app.attendance.data.AttendanceRepository
import kotlinx.coroutines.Job
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AttendanceUiState(
    val date: LocalDate = LocalDate.now(),
    val branch: String = "",
    val groupKey: String = "",
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

    private fun cleanupDuplicateMembersInDb() {
        val members = uiState.value.members
        if (members.size <= 1) return

        val dups = members.groupBy { it.displayName.nameKey() }
            .values
            .filter { it.size > 1 }

        if (dups.isEmpty()) return

        viewModelScope.launch {
            // משאירים הראשון, מוחקים את השאר
            dups.forEach { group ->
                group.drop(1).forEach { m ->
                    runCatching { repo.removeMember(_branch.value, _groupKey.value, m.id) }
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

    private val _date = MutableStateFlow(LocalDate.now())
    private val _branch = MutableStateFlow("")
    private val _groupKey = MutableStateFlow("")
    private val _sessionId = MutableStateFlow<Long?>(null)

    // 🔄 טיקט רענון – כשמעלים אותו, זורם חדש מתחבר למקורות (ועושה re-query)
    private val _refreshTick = MutableStateFlow(0)

    private val membersFlow: Flow<List<GroupMember>> =
        combine(_branch, _groupKey, _refreshTick) { b, g, tick -> Triple(b, g, tick) }
            .flatMapLatest { (b, g, tick) ->
                Log.i(
                    "ATT_MEMBERS_FLOW",
                    "reading members branch=$b group=$g refreshTick=$tick"
                )

                if (b.isBlank() || g.isBlank()) {
                    flowOf(emptyList())
                } else {
                    repo.members(branch = b, groupKey = g)
                }
            }

    private val recordsFlow: Flow<List<AttendanceRecord>> =
        combine(_branch, _groupKey, _date, _refreshTick) { b, g, d, _ -> Triple(b, g, d) }
            .flatMapLatest { (b, g, d) ->
                if (b.isBlank() || g.isBlank()) flowOf(emptyList())
                else repo.attendanceForDay(branch = b, groupKey = g, date = d)
            }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private val headerFlow =
        combine(_date, _branch, _groupKey, _sessionId) { d, b, g, s -> Quad(d, b, g, s) }

    val uiState: StateFlow<AttendanceUiState> =
        combine(
            headerFlow,
            membersFlow,
            recordsFlow
        ) { h, members, records ->

            val map: Map<Long, AttendanceStatus> =
                records.associate { it.memberId to it.status }

            AttendanceUiState(
                date = h.first,
                branch = h.second,
                groupKey = h.third,
                sessionId = h.fourth,
                members = members,
                records = records,
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
                        state.members.map { it.displayName.trim().lowercase() }
                    )
                }
                .distinctUntilChanged()
                .collect { (branch, groupKey, names) ->
                    if (branch.isBlank() || groupKey.isBlank()) return@collect
                    if (names.size <= 1) return@collect

                    val key = "$branch|$groupKey"
                    if (lastCleanupKey == key) return@collect

                    val hasDuplicates = names
                        .filter { it.isNotBlank() }
                        .groupBy { it }
                        .any { (_, sameNames) -> sameNames.size > 1 }

                    if (hasDuplicates) {
                        lastCleanupKey = key
                        cleanupDuplicateMembersInDb()
                    }
                }
        }
    }

    fun setContext(date: LocalDate, branch: String, groupKey: String) {
        fun String.norm(): String = trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")

        // ✅ תמיד נשתמש בסניף "פעיל" אחד:
        // אם מגיע CSV (כמה סניפים) – ניקח את הראשון.
        // אם קיים active_branch (מהרישום החדש) – נשתמש בו תמיד
        val sp = getApplication<Application>()
            .getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

        val active = (sp.getString("active_branch", "") ?: "").trim()

        val branchOne = if (active.isNotBlank()) {
            active.norm()
        } else {
            branch.norm()
                .split(",")
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
                ?.norm()
                ?: ""
        }

        _date.value = date
        _branch.value = branchOne
        _groupKey.value = groupKey.norm()

        ensureSession()
    }

    fun ensureSession() {
        val d = _date.value
        val b = _branch.value
        val g = _groupKey.value
        if (b.isBlank() || g.isBlank()) return

        viewModelScope.launch {
            val result = runCatching {
                repo.ensureSession(date = d, branch = b, groupKey = g)
            }

            result.onSuccess { id ->
                _sessionId.value = id
            }.onFailure { t ->
                Log.e(
                    "ATT_SESSION",
                    "Failed ensuring attendance session date=$d branch=$b group=$g",
                    t
                )

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
            }.onSuccess { memberId ->
                Log.i(
                    "ATT_ADD_MEMBER",
                    "member added branch=$b group=$g memberId=$memberId name=$cleanName"
                )

                _refreshTick.update { it + 1 }
            }.onFailure { t ->
                Log.e(
                    "ATT_ADD_MEMBER",
                    "Failed adding member branch=$b group=$g name=$cleanName",
                    t
                )

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
            Log.e(
                "ATT_MARK",
                "Cannot mark attendance: sessionId is null memberId=$memberId status=$status"
            )

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
                Log.i(
                    "ATT_MARK",
                    "attendance marked sessionId=$sid memberId=$memberId status=$status"
                )

                _refreshTick.update { it + 1 }
            }.onFailure { t ->
                Log.e(
                    "ATT_MARK",
                    "Failed marking attendance sessionId=$sid memberId=$memberId status=$status",
                    t
                )

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
                Log.e(
                    "ATT_REMOVE_MEMBER",
                    "Failed removing member branch=$b group=$g memberId=$memberId",
                    result.exceptionOrNull()
                )

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
        if (b.isBlank() || g.isBlank()) return

        viewModelScope.launch {
            runCatching {
                repo.saveReportForDate(
                    branch = b,
                    groupKey = g,
                    date = d
                )
            }.onSuccess {
                Log.i(
                    "ATT_SAVE",
                    "Attendance report saved date=$d branch=$b group=$g"
                )

                _events.tryEmit(UiEvent.ReportSaved(branch = b, groupKey = g))
            }.onFailure { t ->
                Log.e(
                    "ATT_SAVE",
                    "Failed saving attendance report date=$d branch=$b group=$g",
                    t
                )

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
    fun bootstrapMembersFromUsers(branchBase: String, groupBase: String) {
        val b0 = branchBase.trim()
        val g0 = groupBase.trim()
        if (b0.isBlank()) return

        val key = "$b0|$g0"

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

            Log.i("ATT_BOOT", "VM branchCandidates=$branchCandidates groupCandidates=$groupCandidates")

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
                Log.w("ATT_BOOT", "VM docs empty -> fallback: fetch ALL users (paged) and filter client-side")

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

                Log.w("ATT_BOOT", "VM fallback matched docs=${docs.size} (from all=${all.size})")
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

            Log.i(
                "ATT_BOOT",
                "VM docs=${docs.size} -> notCoach=${stepNotCoach.size} -> trainee=${stepTrainee.size} -> group=${stepGroup.size} -> names=${names.size} -> $names"
            )

            // ✅ מניעת כפילויות: לא מוסיפים אם כבר קיים במודל/DB (לפי displayName מנורמל)
            val existingNamesNorm = uiState.value.members
                .asSequence()
                .map { it.displayName.trim() }
                .filter { it.isNotBlank() }
                .map { it.lowercase() }
                .toSet()

            val toAdd = names
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filter { it.lowercase() !in existingNamesNorm }
                .toList()

            Log.i("ATT_BOOT", "VM toAdd=${toAdd.size} skipped=${names.size - toAdd.size}")

            val repoBranch = _branch.value.ifBlank { b0 }.trim()
            val repoGroup = _groupKey.value.ifBlank { g0 }.trim()

            Log.i(
                "ATT_BOOT",
                "VM writing attendance members to repoBranch=$repoBranch repoGroup=$repoGroup"
            )

            toAdd.forEach { n ->
                runCatching {
                    repo.addMember(
                        branch = repoBranch,
                        groupKey = repoGroup,
                        displayName = n
                    )
                }.onSuccess { memberId ->
                    Log.i(
                        "ATT_BOOT",
                        "VM added member to attendanceGroups memberId=$memberId name=$n branch=$repoBranch group=$repoGroup"
                    )
                }.onFailure { t ->
                    Log.e(
                        "ATT_BOOT",
                        "VM failed adding member to attendanceGroups name=$n branch=$repoBranch group=$repoGroup",
                        t
                    )
                }
            }

            _refreshTick.update { it + 1 }

            // ✅ ניקוי כפילויות קיימות ב-DB / Firestore לפי המימוש הנוכחי של ה-Repository
            cleanupDuplicateMembersInDb()
        }
    }
}