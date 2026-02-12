package il.kmi.app.attendance.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import il.kmi.app.attendance.data.AttendanceRecord
import il.kmi.app.attendance.data.AttendanceStatus
import il.kmi.app.attendance.data.GroupMember
import il.kmi.app.attendance.data.AttendanceRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AttendanceUiState(
    val date: LocalDate = LocalDate.now(),
    val branch: String = "",
    val groupKey: String = "",
    val sessionId: Long? = null,
    val members: List<GroupMember> = emptyList(),
    val records: List<AttendanceRecord> = emptyList()
)

class AttendanceViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: AttendanceRepository = AttendanceRepository.get(app)

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

    val uiState =
        combine(headerFlow, membersFlow) { h, members -> Pair(h, members) }
            .let { pairFlow ->
                combine(pairFlow, recordsFlow) { (h, members), records ->
                    AttendanceUiState(
                        date = h.first,
                        branch = h.second,
                        groupKey = h.third,
                        sessionId = h.fourth,
                        members = members,
                        records = records
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, AttendanceUiState())

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
            }
        }
    }
}
