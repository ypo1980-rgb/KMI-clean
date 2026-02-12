package il.kmi.app.ui.training

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import il.kmi.app.data.training.FirestoreTrainingSummaryRepo
import il.kmi.app.data.training.SummaryAuthorRole
import il.kmi.app.data.training.TrainingSummaryEntity
import il.kmi.app.data.training.TrainingSummaryExerciseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SelectedExerciseUi(
    val exerciseId: String,
    val name: String,
    val topic: String,
    val difficulty: Int? = null,
    val highlight: String = "",
    val homePractice: Boolean = false
)

data class TrainingSummaryUiState(
    val isCoach: Boolean,
    val ownerUid: String,
    val ownerRole: SummaryAuthorRole,

    val dateIso: String,
    val branchId: String = "",
    val branchName: String = "",
    val coachUid: String = "",
    val coachName: String = "",
    val groupKey: String = "",

    val searchQuery: String = "",
    val selected: Map<String, SelectedExerciseUi> = emptyMap(),

    val notes: String = "",
    val isSaving: Boolean = false,

    // ✅ חדש: חיווי שמירה למסך (Snackbar)
    val lastSaveMsg: String? = null,
    val lastSaveWasError: Boolean = false,
    val saveEventId: Long = 0L,

    // ✅ חדש: כל הימים שיש בהם סיכום (בחודש שנמצא ביומן כרגע)
    val summaryDaysInCalendarMonth: Set<String> = emptySet()
)

class TrainingSummaryViewModel(
    private val repo: FirestoreTrainingSummaryRepo,
    ownerUid: String,
    ownerRole: SummaryAuthorRole,
    initialBranchName: String = "",
    initialCoachName: String = ""
) : ViewModel() {

    private val TAG = "TRAINING_SUMMARY"

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun todayIso(): String = isoFmt.format(Date())

    private val _state = MutableStateFlow(
        TrainingSummaryUiState(
            isCoach = (ownerRole == SummaryAuthorRole.COACH),
            ownerUid = ownerUid,
            ownerRole = ownerRole,
            dateIso = todayIso(),
            branchName = initialBranchName,
            coachName = initialCoachName
        )
    )
    val state: StateFlow<TrainingSummaryUiState> = _state

    // -----------------------------
    // Setters
    // -----------------------------

    fun setDateIso(v: String) = _state.update { it.copy(dateIso = v.trim()) }
    fun setBranchName(v: String) = _state.update { it.copy(branchName = v.trim()) }
    fun setCoachName(v: String) = _state.update { it.copy(coachName = v.trim()) }
    fun setGroupKey(v: String) = _state.update { it.copy(groupKey = v.trim()) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }
    fun setSearchQuery(v: String) = _state.update { it.copy(searchQuery = v) }

    // ✅ חדש: טעינת ימים מסומנים לחודש ביומן
    fun loadSummaryDaysForMonth(year: Int, month1to12: Int) {
        val snap = _state.value
        viewModelScope.launch {
            runCatching {
                val start = java.time.LocalDate.of(year, month1to12, 1)
                val endExclusive = start.plusMonths(1)

                val dates = repo.listDatesForOwnerBetween(
                    ownerUid = snap.ownerUid,
                    ownerRole = snap.ownerRole,
                    startIso = start.toString(),
                    endIsoExclusive = endExclusive.toString()
                )

                _state.update { it.copy(summaryDaysInCalendarMonth = dates) }
            }.onFailure { t ->
                Log.e(TAG, "loadSummaryDaysForMonth failed y=$year m=$month1to12", t)
                // לא נכשיל UX – פשוט נשאיר ריק
                _state.update { it.copy(summaryDaysInCalendarMonth = emptySet()) }
            }
        }
    }

    // -----------------------------
    // Exercises
    // -----------------------------

    fun toggleExercise(item: ExercisePickItem) {
        _state.update { st ->
            val cur = st.selected.toMutableMap()
            if (cur.containsKey(item.exerciseId)) {
                cur.remove(item.exerciseId)
            } else {
                cur[item.exerciseId] = SelectedExerciseUi(
                    exerciseId = item.exerciseId,
                    name = item.name,
                    topic = item.topic
                )
            }
            st.copy(selected = cur)
        }
    }

    fun removeExercise(exerciseId: String) {
        _state.update { st ->
            val cur = st.selected.toMutableMap()
            cur.remove(exerciseId)
            st.copy(selected = cur)
        }
    }

    fun setDifficulty(exerciseId: String, difficulty: Int?) {
        _state.update { st ->
            val cur = st.selected.toMutableMap()
            val ex = cur[exerciseId] ?: return@update st
            cur[exerciseId] = ex.copy(difficulty = difficulty)
            st.copy(selected = cur)
        }
    }

    fun setHighlight(exerciseId: String, highlight: String) {
        _state.update { st ->
            val cur = st.selected.toMutableMap()
            val ex = cur[exerciseId] ?: return@update st
            cur[exerciseId] = ex.copy(highlight = highlight)
            st.copy(selected = cur)
        }
    }

    fun setHomePractice(exerciseId: String, homePractice: Boolean) {
        _state.update { st ->
            val cur = st.selected.toMutableMap()
            val ex = cur[exerciseId] ?: return@update st
            cur[exerciseId] = ex.copy(homePractice = homePractice)
            st.copy(selected = cur)
        }
    }

    // -----------------------------
    // Save
    // -----------------------------

    fun save(onSuccess: (() -> Unit)? = null, onError: ((Throwable) -> Unit)? = null) {
        val snap = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val model = TrainingSummaryEntity(
                    id = "",
                    ownerUid = snap.ownerUid,
                    ownerRole = snap.ownerRole,
                    dateIso = snap.dateIso,
                    branchId = snap.branchId,
                    branchName = snap.branchName,
                    coachUid = snap.coachUid,
                    coachName = snap.coachName,
                    groupKey = snap.groupKey,
                    notes = snap.notes,
                    exercises = snap.selected.values.map { ex ->
                        TrainingSummaryExerciseEntity(
                            exerciseId = ex.exerciseId,
                            name = ex.name,
                            topic = ex.topic,
                            difficulty = ex.difficulty,
                            highlight = ex.highlight,
                            homePractice = ex.homePractice
                        )
                    }
                )

                val id = repo.saveForOwner(
                    ownerUid = snap.ownerUid,
                    ownerRole = snap.ownerRole,
                    summary = model
                )

                Log.w(TAG, "Saved training summary id=$id exercises=${model.exercises.size}")

                _state.update {
                    it.copy(
                        lastSaveMsg = "✅ הסיכום נשמר",
                        lastSaveWasError = false,
                        saveEventId = System.currentTimeMillis()
                    )
                }

                onSuccess?.invoke()
            } catch (t: Throwable) {
                Log.e(TAG, "Save failed", t)

                _state.update {
                    it.copy(
                        lastSaveMsg = "❌ השמירה נכשלה",
                        lastSaveWasError = true,
                        saveEventId = System.currentTimeMillis()
                    )
                }

                onError?.invoke(t)
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
