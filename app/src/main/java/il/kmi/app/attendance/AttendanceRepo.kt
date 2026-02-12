package il.kmi.app.attendance

import android.app.Application
import androidx.room.Room
import il.kmi.app.attendance.data.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AttendanceRepo(app: Application) {
    private val db = Room.databaseBuilder(
        app,
        AttendanceDatabase::class.java,
        "attendance.db"
    ).fallbackToDestructiveMigration() // החלף במיגרציות כשצריך
        .build()

    private val dao = db.dao()

    fun members(branch: String, groupKey: String) =
        dao.members(branch, groupKey)

    suspend fun upsertMember(m: GroupMember) = dao.upsertMember(m)
    suspend fun deleteMember(m: GroupMember) = dao.deleteMember(m)

    suspend fun findOrCreateSession(date: LocalDate, branch: String, groupKey: String): TrainingSession {
        dao.findSession(date, branch, groupKey)?.let { return it }
        val id = dao.insertSession(TrainingSession(date = date, branch = branch, groupKey = groupKey))
        return dao.findSession(date, branch, groupKey) ?: TrainingSession(id = id, date = date, branch = branch, groupKey = groupKey)
    }

    fun attendanceForDay(branch: String, groupKey: String, date: LocalDate): Flow<List<AttendanceRecord>> =
        dao.attendanceForDay(branch, groupKey, date)

    suspend fun mark(sessionId: Long, memberId: Long, status: AttendanceStatus) =
        dao.updateStatus(sessionId, memberId, status)

    fun stats(branch: String, groupKey: String, from: LocalDate, to: LocalDate) =
        dao.presenceStats(branch, groupKey, from, to)
}
