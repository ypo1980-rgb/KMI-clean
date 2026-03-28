package il.kmi.shared.reminders

import il.kmi.shared.domain.Belt

data class DailyExerciseItem(
    val belt: Belt,
    val topic: String,
    val item: String
)