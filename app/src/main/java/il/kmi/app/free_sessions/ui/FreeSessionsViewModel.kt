package il.kmi.app.free_sessions.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import il.kmi.shared.free_sessions.data.FreeSessionsRepository
import il.kmi.shared.free_sessions.data.systemNowMillis
import il.kmi.shared.free_sessions.model.FreeSession
import il.kmi.shared.free_sessions.model.ParticipantState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class FreeSessionsViewModel(
    private val repo: FreeSessionsRepository
) : ViewModel() {

    private val _branch = MutableStateFlow("")
    private val _groupKey = MutableStateFlow("")
    private val _myUid = MutableStateFlow("")
    private val _myName = MutableStateFlow("")

    fun setContext(
        branch: String,
        groupKey: String,
        myUid: String,
        myName: String
    ) {
        _branch.value = branch.trim()
        _groupKey.value = groupKey.trim()
        _myUid.value = myUid.trim()
        _myName.value = myName.trim()
    }

    val upcoming: StateFlow<List<FreeSession>> =
        combine(_branch, _groupKey) { b, g -> b to g }
            .flatMapLatest { (b, g) ->
                if (b.isBlank() || g.isBlank()) {
                    flowOf(emptyList())
                } else {
                    repo.observeUpcoming(
                        branch = b,
                        groupKey = g,
                        nowMillis = repo.systemNowMillisCompat()
                    )
                }
            }
            .catch { emit(emptyList()) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun createSession(
        title: String,
        locationName: String?,
        lat: Double?,
        lng: Double?,
        startsAt: Long,
        onDone: (sessionId: String?) -> Unit = {}
    ) {
        val b = _branch.value
        val g = _groupKey.value
        val uid = _myUid.value
        val name = _myName.value

        if (b.isBlank() || g.isBlank() || uid.isBlank() || name.isBlank()) {
            onDone(null)
            return
        }

        viewModelScope.launch {
            runCatching {
                repo.createFreeSession(
                    branch = b,
                    groupKey = g,
                    title = title,
                    locationName = locationName,
                    lat = lat,
                    lng = lng,
                    startsAt = startsAt,
                    createdByUid = uid,
                    createdByName = name
                )
            }.onSuccess { id ->
                onDone(id)
            }.onFailure {
                onDone(null)
            }
        }
    }

    fun setMyState(sessionId: String, state: ParticipantState) {
        val b = _branch.value
        val g = _groupKey.value
        val uid = _myUid.value
        val name = _myName.value
        if (b.isBlank() || g.isBlank() || uid.isBlank() || name.isBlank()) return

        viewModelScope.launch {
            runCatching {
                repo.setParticipantState(
                    branch = b,
                    groupKey = g,
                    sessionId = sessionId,
                    uid = uid,
                    name = name,
                    state = state
                )
            }
        }
    }

    fun closeSession(sessionId: String) {
        val b = _branch.value
        val g = _groupKey.value
        if (b.isBlank() || g.isBlank() || sessionId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                repo.closeSession(
                    branch = b,
                    groupKey = g,
                    sessionId = sessionId
                )
            }
        }
    }
}

/**
 * מציגים "אימונים של היום + עתידיים":
 * במקום nowMillis=עכשיו, אנחנו שולחים startOfToday (00:00) — כך אימונים של היום נשארים עד סוף היום.
 */
private fun FreeSessionsRepository.systemNowMillisCompat(): Long {
    val z = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(systemNowMillis()).atZone(z).toLocalDate()
    val startOfToday = today.atStartOfDay(z).toInstant().toEpochMilli()
    return startOfToday
}
