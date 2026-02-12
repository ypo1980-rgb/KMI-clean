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
                id = 0,               // auto-id
                branch = branch,
                groupKey = groupKey,
                displayName = displayName
            )
        )

    /**
     * מחיקת מתאמן לפי מזהה.
     * ה-ViewModel יקרא לפונקציה הזו ישירות (suspend) בתוך viewModelScope.launch.
     */
    suspend fun removeMember(branch: String, groupKey: String, memberId: Long) {
        // נשלוף את רשימת המתאמנים בקבוצה ונמצא את המתאמן לפי id
        val membersInGroup = dao.members(branch, groupKey).first()
        val member = membersInGroup.firstOrNull { it.id == memberId } ?: return
        // מחיקה אמיתית מה-DB
        dao.deleteMember(member)
    }

    /**
     * מבטיח שתהיה ישיבת אימון ל־(תאריך+סניף+קבוצה) ומחזיר את ה-id שלה.
     * אם קיימת – מחזיר את הקיימת; אם לא – יוצר חדשה.
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
        // אם בגלל ON CONFLICT IGNORE לא הוחדר (id=0) – נשלוף שוב את הקיימת
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
     * משתמש בעדכון ישיר (קיים ב-DAO) כדי לא להיתקל בשדות שאינם במודל.
     */
    suspend fun mark(
        sessionId: Long,
        memberId: Long,
        status: AttendanceStatus,
        note: String? = null   // שמור לעתיד, כרגע לא נשמר ב-DB
    ) {
        dao.updateStatus(
            sessionId = sessionId,
            memberId = memberId,
            status = status,
            ts = System.currentTimeMillis()
        )
    }

    /** סטטיסטיקות נוכחות לפי טווח תאריכים */
    fun stats(
        branch: String,
        groupKey: String,
        from: LocalDate,
        to: LocalDate
    ): Flow<List<MemberPresenceStat>> =
        dao.presenceStats(branch, groupKey, from, to)

    // ---------- דו"חות נוכחות (ארכיון) ----------

    /**
     * מחשב דו"ח נוכחות ליום מסוים ושומר אותו בטבלת attendance_reports.
     * נועד לשימוש כשמאמן לוחץ "שמור דו"ח" / "אישור נוכחות".
     */
    suspend fun saveReportForDate(
        branch: String,
        groupKey: String,
        date: LocalDate
    ) {
        // לוודא שיש session ליום הזה
        val sessionId = ensureSession(date, branch, groupKey)

        // חברי הקבוצה
        val members = members(branch, groupKey).first()

        // רשומות נוכחות ליום הזה
        val records = attendanceForDay(branch, groupKey, date).first()

        // מפה memberId -> record
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
    }

    /**
     * החזרה של N הדו"חות האחרונים לקבוצה (למסך "ארכיון נוכחות").
     */
    fun lastReports(
        branch: String,
        groupKey: String,
        limit: Int = 5
    ): Flow<List<AttendanceReport>> =
        dao.lastReportsForGroup(branch, groupKey, limit)

    // ---------- Singleton ----------

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
