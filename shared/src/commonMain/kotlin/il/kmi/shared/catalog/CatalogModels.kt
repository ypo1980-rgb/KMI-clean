package il.kmi.shared.catalog

/**
 * DTOs "שטוחים" שמתאימים מאוד ל-Swift.
 * בלי Enum מורכב ובלי טיפוסים של Android.
 */
data class BeltDto(
    val id: String,        // "yellow", "orange"...
    val title: String,     // "חגורה צהובה"
    val order: Int
)

data class TopicDto(
    val id: String,        // stable id (string)
    val title: String      // "הגנות פנימיות", "עבודת ידיים"...
)

data class SubTopicDto(
    val id: String,
    val title: String
)

data class ExerciseDto(
    val id: String,        // stable id (string)
    val title: String,     // כותרת תרגיל
    val subtitle: String? = null
)

data class ExerciseContentDto(
    val id: String,
    val title: String,
    val mimeType: String = "text/html",
    val contents: String  // HTML/Text
)
