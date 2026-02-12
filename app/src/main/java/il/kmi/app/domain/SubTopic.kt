package il.kmi.app.domain

/**
 * DTO פשוט לשכבת ה-UI של האפליקציה.
 * זה לא ה-SubTopic של ה-shared catalog — רק מבנה נתונים לשימוש במסכים.
 */
data class SubTopic(
    val title: String,
    val items: List<String> = emptyList()
)
