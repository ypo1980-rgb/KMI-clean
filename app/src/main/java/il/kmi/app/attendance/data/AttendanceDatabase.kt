package il.kmi.app.attendance.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        GroupMember::class,
        TrainingSession::class,
        AttendanceRecord::class,
        AttendanceReport::class   // 👈 הטבלה החדשה לדו"חות
    ],
    version = 3,                 // 👈 העלינו מ-2 ל-3
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun dao(): AttendanceDao
}
