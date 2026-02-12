package il.kmi.app.attendance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

// ───────── רשומת נוכחות ליחיד ─────────
@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["memberId"]),
        Index(value = ["sessionId", "memberId"], unique = true)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val memberId: Long,
    val status: AttendanceStatus = AttendanceStatus.ABSENT,
    val markedAtMillis: Long = System.currentTimeMillis()
)

// ───────── דו"ח נוכחות מסוכם לקבוצה ─────────
@Entity(
    tableName = "attendance_reports",
    indices = [
        Index(value = ["branch"]),
        Index(value = ["groupKey"]),
        Index(value = ["date"]),
        Index(value = ["sessionId"])
    ]
)
data class AttendanceReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    val branch: String,
    val groupKey: String,

    // נשמר כ-TEXT בעזרת Converters (LocalDate <-> String)
    val date: LocalDate,
    val sessionId: Long,

    val totalMembers: Int,
    val presentCount: Int,
    val excusedCount: Int,
    val absentCount: Int,

    // אחוז (0–100)
    val percentPresent: Int,

    // מילישניות, לשמירה לפי זמן יצירת הדו"ח
    val createdAtMillis: Long
)
