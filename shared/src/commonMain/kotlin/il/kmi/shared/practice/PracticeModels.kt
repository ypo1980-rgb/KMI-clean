package il.kmi.shared.practice

/**
 * פילטרים “מיוחדים” שהגיעו מה-UI הקיים.
 * שמרתי אותם זהים למה שיש אצלך ב-App כדי שתוכל להעביר מחר ל-iOS בלי שינוי.
 */
object PracticeFilters {
    const val ALL = "__ALL__"
    const val UNKNOWN = "__UNKNOWN__"
    const val FAVS_ALL = "__FAVS_ALL__"

    // טוקן לתרגול לפי נושאים (חגורות+נושאים) – זהה ל-App
    const val TOPICS_PICK_TOKEN = "__TOPICS_PICK__"
}

data class PracticeRequest(
    val beltId: String,
    val topicFilter: String? // null/blank => ALL
)

data class PracticeItem(
    val beltId: String,
    val topicTitle: String,
    val rawTitle: String,
    val displayTitle: String,
    val canonicalKey: String
)
