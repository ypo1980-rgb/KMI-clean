package il.kmi.app.attendance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "training_sessions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["branch", "groupKey", "date"], unique = true) // יחודיות: מפגש אחד ליום/סניף/קבוצה
    ]
)
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: LocalDate,
    val branch: String,
    val groupKey: String
)
