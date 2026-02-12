package il.kmi.app.attendance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AttendanceDao {

    // --- Members ---
    @Query(
        """
        SELECT *
        FROM group_members
        WHERE branch  = :branch
          AND groupKey = :groupKey
        ORDER BY displayName COLLATE NOCASE   -- היה display_name
        """
    )
    fun members(branch: String, groupKey: String): Flow<List<GroupMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(m: GroupMember): Long

    @Delete
    suspend fun deleteMember(m: GroupMember)

    // --- Sessions ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(s: TrainingSession): Long

    @Query(
        "SELECT * FROM training_sessions WHERE date = :date AND branch = :branch AND groupKey = :groupKey LIMIT 1"
    )
    suspend fun findSession(
        date: LocalDate,
        branch: String,
        groupKey: String
    ): TrainingSession?

    @Query("SELECT * FROM training_sessions WHERE id = :id")
    fun sessionById(id: Long): Flow<TrainingSession?>

    // --- Attendance (per day) ---
    @Query(
        """
        SELECT ar.*
        FROM attendance_records ar
        INNER JOIN training_sessions s ON s.id = ar.sessionId
        WHERE s.date   = :date
          AND s.branch  = :branch
          AND s.groupKey = :groupKey
        """
    )
    fun attendanceForDay(
        branch: String,
        groupKey: String,
        date: LocalDate
    ): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(r: AttendanceRecord): Long

    @Query(
        """
        UPDATE attendance_records
        SET status = :status,
            markedAtMillis = :ts
        WHERE sessionId = :sessionId
          AND memberId  = :memberId
        """
    )
    suspend fun updateStatus(
        sessionId: Long,
        memberId: Long,
        status: AttendanceStatus,
        ts: Long = System.currentTimeMillis()
    )

    // --- Presence stats (range) ---
    // שים לב: סינון התאריכים ב-JOIN על training_sessions כדי לשמור LEFT JOIN
    @Query(
        """
        SELECT gm.id AS memberId,
               SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END) * 1.0 /
               NULLIF(COUNT(ar.id), 0) AS presenceRatio
        FROM group_members gm
        LEFT JOIN attendance_records ar ON ar.memberId = gm.id
        LEFT JOIN training_sessions s   ON s.id = ar.sessionId
                                       AND s.date BETWEEN :from AND :to
        WHERE gm.branch  = :branch
          AND gm.groupKey = :groupKey
        GROUP BY gm.id
        ORDER BY presenceRatio DESC
        """
    )
    fun presenceStats(
        branch: String,
        groupKey: String,
        from: LocalDate,
        to: LocalDate
    ): Flow<List<MemberPresenceStat>>

    // --- Reports archive (דו"חות אימון אחרונים) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReport(report: AttendanceReport): Long

    @Query(
        """
        SELECT *
        FROM attendance_reports
        WHERE branch  = :branch
          AND groupKey = :groupKey
        ORDER BY createdAtMillis DESC
        LIMIT :limit
        """
    )
    fun lastReportsForGroup(
        branch: String,
        groupKey: String,
        limit: Int
    ): Flow<List<AttendanceReport>>
}

// תוצאת הסטטיסטיקה
data class MemberPresenceStat(
    val memberId: Long,
    val presenceRatio: Double?
)
