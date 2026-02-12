package il.kmi.app.data.training

/**
 * מי כתב/שייך הסיכום (כל אחד בצד שלו)
 */
enum class SummaryAuthorRole {
    COACH,
    TRAINEE
}

data class TrainingSummaryExerciseEntity(
    val exerciseId: String = "",     // מזהה מתוך הקטלוג/ContentRepo
    val name: String = "",           // שם לתצוגה
    val topic: String = "",          // נושא/קטגוריה לתצוגה

    val difficulty: Int? = null,     // 1..5
    val highlight: String = "",
    val homePractice: Boolean = false
)

data class TrainingSummaryEntity(
    val id: String = "",

    // מי הבעלים של הסיכום (כל אחד בצד שלו)
    val ownerUid: String = "",
    val ownerRole: SummaryAuthorRole = SummaryAuthorRole.TRAINEE,

    // פרטי אימון
    val dateIso: String = "",        // "yyyy-MM-dd"
    val branchId: String = "",
    val branchName: String = "",

    val coachUid: String = "",
    val coachName: String = "",

    val groupKey: String = "",

    // תוכן
    val exercises: List<TrainingSummaryExerciseEntity> = emptyList(),
    val notes: String = "",

    // מטא
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
)
