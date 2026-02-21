package il.kmi.app.attendance.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        GroupMember::class,
        TrainingSession::class,
        AttendanceRecord::class,
        AttendanceReport::class
    ],
    version = 2, // <-- היה 1 (או מספר אחר). תעלה ב-1 בכל שינוי סכמה
    exportSchema = false
)
@TypeConverters(AttendanceConverters::class)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun dao(): AttendanceDao
}
