package il.kmi.shared.model

// חגורות – ללא תלות באנדרואיד
enum class KmiBelt { WHITE, YELLOW, ORANGE, GREEN, BLUE, BROWN, BLACK }

// תוכן “דק” לחיפוש בלבד
data class KmiSubTopic(
    val title: String,
    val items: List<String>
)

data class KmiTopic(
    val title: String,
    val items: List<String> = emptyList(),         // אם אין תתי־נושאים
    val subTopics: List<KmiSubTopic> = emptyList() // ואם יש – משתמשים בזה
)

data class KmiBeltContent(
    val topics: List<KmiTopic>
)
