package il.kmi.app.attendance.data

import androidx.room.TypeConverter
import java.time.LocalDate

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    @JvmStatic
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    @JvmStatic
    fun fromStatus(name: String?): AttendanceStatus? = name?.let { runCatching { AttendanceStatus.valueOf(it) }.getOrNull() }

    @TypeConverter
    @JvmStatic
    fun statusToString(status: AttendanceStatus?): String? = status?.name
}
