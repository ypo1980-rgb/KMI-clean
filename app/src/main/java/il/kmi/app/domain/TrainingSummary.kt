package il.kmi.app.domain

import java.time.LocalDate

data class TrainingSummary(
    val date: String,                    // yyyy-MM-dd
    val branch: String,
    val coachName: String,
    val exercises: List<TrainingExercise>,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * תרגיל שנבחר לסיכום אימון
 */
data class TrainingExercise(
    val itemKey: String,                 // ContentRepo.makeItemKey(...)
    val title: String,                   // שם תרגיל לתצוגה
    val beltId: String,
    val topicTitle: String,
    val subTopicTitle: String? = null
)
