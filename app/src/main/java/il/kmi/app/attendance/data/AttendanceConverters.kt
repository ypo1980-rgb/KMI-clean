package il.kmi.app.attendance.data

import androidx.room.TypeConverter
import java.time.LocalDate

class AttendanceConverters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
}
