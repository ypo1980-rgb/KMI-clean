package il.kmi.app.attendance.data

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class AttendanceRepository private constructor(
    private val db: AttendanceDatabase
) {
    private val dao: AttendanceDao = db.dao()

    /**
     * רשימת מתאמנים לקבוצה – נתוני אמת מה-DB.
     * אין הגבלה ל-10 ואין דמו.
     */
    fun members(branch: String, groupKey: String): Flow<List<GroupMember>> =
        dao.members(branch, groupKey)

    suspend fun addMember(branch: String, groupKey: String, displayName: String): Long =
        dao.upsertMember(
            GroupMember(
                id = 0,
                branch = branch,
                groupKey = groupKey,
                displayName = displayName.trim()
            )
        )

    /**
     * מחיקת מתאמן לפי מזהה.
     */
    suspend fun removeMember(branch: String, groupKey: String, memberId: Long) {
        val membersInGroup = dao.members(branch, groupKey).first()
        val member = membersInGroup.firstOrNull { it.id == memberId } ?: return
        dao.deleteMember(member)
    }


    /**
     * מבטיח שתהיה ישיבת אימון ל־(תאריך+סניף+קבוצה) ומחזיר את ה-id שלה.
     */
    suspend fun ensureSession(
        date: LocalDate,
        branch: String,
        groupKey: String
    ): Long {
        val existing = dao.findSession(date, branch, groupKey)
        if (existing != null) return existing.id

        val insertedId = dao.insertSession(
            TrainingSession(
                date = date,
                branch = branch,
                groupKey = groupKey
            )
        )

        return if (insertedId > 0L) insertedId
        else requireNotNull(dao.findSession(date, branch, groupKey)).id
    }

    /** רשומות נוכחות ליום מסוים (לפי סניף+קבוצה+תאריך) */
    fun attendanceForDay(
        branch: String,
        groupKey: String,
        date: LocalDate
    ): Flow<List<AttendanceRecord>> =
        dao.attendanceForDay(branch, groupKey, date)

    /**
     * סימון נוכחות/היעדרות לחבר מסוים במפגש נתון.
     * אם אין עדיין רשומה (sessionId+memberId) – ניצור אחת.
     */
    suspend fun mark(
        sessionId: Long,
        memberId: Long,
        status: AttendanceStatus
    ) {
        val ts = System.currentTimeMillis()

        val updated = dao.updateStatus(
            sessionId = sessionId,
            memberId = memberId,
            status = status,
            ts = ts
        )

        if (updated == 0) {
            dao.upsertRecord(
                AttendanceRecord(
                    sessionId = sessionId,
                    memberId = memberId,
                    status = status,
                    markedAtMillis = ts
                )
            )
        }
    }

    /** סטטיסטיקות נוכחות לפי טווח תאריכים */
    fun stats(
        branch: String,
        groupKey: String,
        from: LocalDate,
        to: LocalDate
    ): Flow<List<MemberPresenceStat>> =
        dao.presenceStats(branch, groupKey, from, to)

    /**
     * מחשב דו"ח נוכחות ליום מסוים ושומר אותו בטבלת attendance_reports.
     */
    suspend fun saveReportForDate(
        branch: String,
        groupKey: String,
        date: LocalDate
    ) {
        val sessionId = ensureSession(date, branch, groupKey)

        val members = members(branch, groupKey).first()
        val records = attendanceForDay(branch, groupKey, date).first()

        val byMember = records.associateBy { it.memberId }

        var present = 0
        var excused = 0
        var absent = 0

        members.forEach { m ->
            val r = byMember[m.id]
            when (r?.status) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.EXCUSED -> excused++
                AttendanceStatus.ABSENT, null -> absent++
            }
        }

        val total = present + excused + absent
        val percent = if (total > 0) (present * 100.0 / total).toInt() else 0

        val report = AttendanceReport(
            branch = branch,
            groupKey = groupKey,
            date = date,
            sessionId = sessionId,
            totalMembers = total,
            presentCount = present,
            excusedCount = excused,
            absentCount = absent,
            percentPresent = percent,
            createdAtMillis = System.currentTimeMillis()
        )

        dao.upsertReport(report)

        // ✅ שומרים לפחות שנה: מוחקים מה שיותר משנה
        val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
        dao.deleteReportsOlderThan(branch, groupKey, oneYearAgo)
    }

    // ✅ מחיקה לפי IDs (מהמסך החדש עם צ'קבוקסים)
    suspend fun deleteReportsByIds(
        branch: String,
        groupKey: String,
        reportIds: List<Long>
    ): Int {
        if (reportIds.isEmpty()) return 0
        return dao.deleteReportsByIds(branch, groupKey, reportIds)
    }

    fun reportsLastYear(branch: String, groupKey: String): Flow<List<AttendanceReport>> {
        val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
        return dao.reportsSince(branch, groupKey, oneYearAgo)
    }

    /**
     * החזרה של N הדו"חות האחרונים לקבוצה.
     */
    fun lastReports(
        branch: String,
        groupKey: String,
        limit: Int = 5
    ): Flow<List<AttendanceReport>> =
        dao.lastReportsForGroup(branch, groupKey, limit)

    // ✅ NEW: מחיקת כל הדו"חות לקבוצה (לא נוגע בסימונים)
    suspend fun clearReports(branch: String, groupKey: String): Int {
        return dao.deleteReportsForGroup(branch, groupKey)
    }

    // ✅ NEW: איפוס נוכחות לקבוצה (מוחק סימונים+שיעורים+דו"חות, משאיר מתאמנים)
    suspend fun resetAttendanceForGroup(branch: String, groupKey: String) {
        dao.deleteAttendanceRecordsForGroup(branch, groupKey)
        dao.deleteSessionsForGroup(branch, groupKey)
        dao.deleteReportsForGroup(branch, groupKey)
    }

    companion object {
        @Volatile
        private var INSTANCE: AttendanceRepository? = null

        fun get(app: Application): AttendanceRepository =
            INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    app,
                    AttendanceDatabase::class.java,
                    "attendance.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                AttendanceRepository(db).also { INSTANCE = it }
            }
    }
}
