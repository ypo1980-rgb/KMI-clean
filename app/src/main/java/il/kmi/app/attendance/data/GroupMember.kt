package il.kmi.app.attendance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_members",
    indices = [
        Index(value = ["branch", "groupKey"]),               // סינון מהיר לפי סניף/קבוצה
        Index(value = ["branch", "groupKey", "displayName"]) // להצגת רשימות ממוינות
    ]
)
data class GroupMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val branch: String,
    val groupKey: String,           // מפתח קבוצת גיל מנורמל (למשל "בוגרים" / "נוער+בוגרים")
    val displayName: String,        // שם לתצוגה
    val phone: String? = null,      // אופציונלי
    val notes: String? = null       // אופציונלי
)
