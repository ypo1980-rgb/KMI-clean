package il.kmi.shared.domain

/**
 * TEMP – הגדרה מינימלית כדי ש-ContentRepo יתקמפל ב-shared.
 * כשתשלח את SubjectTopic האמיתי שלך – נחליף את הקובץ הזה.
 */
data class SubjectTopic(
    val id: String,
    val titleHeb: String,
    val topicsByBelt: Map<Belt, List<String>> = emptyMap(),
    val subTopicHint: String? = null,
    val includeItemKeywords: List<String> = emptyList(),
    val requireAllItemKeywords: List<String> = emptyList(),
    val excludeItemKeywords: List<String> = emptyList()
)
