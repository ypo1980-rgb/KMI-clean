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

    private val _date = MutableStateFlow(LocalDate.now())
    private val _branch = MutableStateFlow("")
    private val _groupKey = MutableStateFlow("")
    private val _sessionId = MutableStateFlow<Long?>(null)

    // 🔄 טיקט רענון – כשמעלים אותו, זורם חדש מתחבר למקורות (ועושה re-query)
    private val _refreshTick = MutableStateFlow(0)

    private val membersFlow: Flow<List<GroupMember>> =
        combine(_branch, _groupKey, _refreshTick) { b, g, _ -> b to g }
            .flatMapLatest { (b, g) ->
                if (b.isBlank() || g.isBlank()) flowOf(emptyList())
                else repo.members(branch = b, groupKey = g)
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

    fun setContext(date: LocalDate, branch: String, groupKey: String) {
        fun String.norm(): String = trim()
            .replace('־', '-')   // maqaf
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
            val id = repo.ensureSession(date = d, branch = b, groupKey = g)
            _sessionId.value = id
        }
    }

    /** הוספת מתאמן בשם בלבד (כולל נפילות לרפלקציה) + רענון UI */
    fun addMember(name: String) {
        val b = _branch.value
        val g = _groupKey.value
        if (b.isBlank() || g.isBlank() || name.isBlank()) return

        viewModelScope.launch {
            // ניסיון “טיפוסי”
            val okDirect = runCatching {
                repo.addMember(branch = b, groupKey = g, displayName = name)
            }.isSuccess

            if (!okDirect) {
                // רפלקציה: כל מתודה בשם נפוץ עם 3 מחרוזות
                runCatching {
                    val m = repo::class.java.methods.firstOrNull { mm ->
                        mm.name in listOf("addMember", "createMember", "insertMember", "add") &&
                                mm.parameterCount == 3 &&
                                mm.parameterTypes.all { it == String::class.java }
                    } ?: return@runCatching
                    m.invoke(repo, b, g, name)
                }
            }
            // 🔔 רענון כפוי
            _refreshTick.update { it + 1 }
        }
    }

    /** סימון נוכחות למתאמן + רענון קל */
    fun mark(memberId: Long, status: AttendanceStatus) {
        val sid = _sessionId.value ?: return
        viewModelScope.launch {
            val ok = runCatching {
                repo.mark(sessionId = sid, memberId = memberId, status = status)
            }.isSuccess

            if (!ok) {
                runCatching {
                    val m = repo::class.java.methods.firstOrNull { mm ->
                        mm.name in listOf("mark", "setMark", "markAttendance") &&
                                mm.parameterCount == 3
                    } ?: return@runCatching
                    m.invoke(repo, sid, memberId, status)
                }
            }
            // לרוב DB יפיץ שינוי לבד; אם לא – נכפה רענון קל
            _refreshTick.update { it + 1 }
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
            runCatching {
                repo.removeMember(b, g, memberId)
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
                _events.tryEmit(UiEvent.ReportSaved(branch = b, groupKey = g))
            }.onFailure { t ->
                _events.tryEmit(UiEvent.ReportSaveFailed(message = t.message ?: t.toString()))
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
        if (lastBootstrapKey == key) return
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
                        .split(',', ';', '；')
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .map { it.norm() }
                }

                val gList = (get("groups") as? List<*>)?.mapNotNull { it?.toString()?.trim() }?.map { it.norm() }.orEmpty()

                val primary = getString("primaryGroup")?.trim()?.norm()
                val groupKeyField = getString("groupKey")?.trim()?.norm()

                // ✅ תוספות נפוצות ב-DBים שונים:
                val groupField = getString("group")?.trim()?.norm()
                val groupName = getString("groupName")?.trim()?.norm()
                val groupsCsv = getString("groupsCsv")?.trim()
                val groupCsv = getString("groupCsv")?.trim()

                val tokenBag = buildList {
                    addAll(gList)
                    addAll(splitTokens(primary))
                    addAll(splitTokens(groupKeyField))
                    addAll(splitTokens(groupField))
                    addAll(splitTokens(groupName))
                    addAll(splitTokens(groupsCsv))
                    addAll(splitTokens(groupCsv))
                }.filter { it.isNotBlank() }.distinct()

                // ✅ אם אין בכלל מידע על קבוצה במסמך – לא נפיל אותו
                if (tokenBag.isEmpty()) return true

                val candNorm = groupCandidates.map { it.norm() }.toSet()

                // התאמה: == או contains דו-כיווני
                return tokenBag.any { tok ->
                    tok in candNorm ||
                            candNorm.any { cand ->
                                cand.length >= 2 && (tok.contains(cand) || cand.contains(tok))
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

            withContext(Dispatchers.Main) {
                toAdd.forEach { n -> addMember(n) }
            }

            // ✅ ניקוי כפילויות קיימות ב-DB (מהרצות קודמות / וריאציות שם)
            cleanupDuplicateMembersInDb()
        }
    }
}