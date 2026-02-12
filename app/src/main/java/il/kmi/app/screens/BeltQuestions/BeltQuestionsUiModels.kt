package il.kmi.app.screens.BeltQuestions

import il.kmi.shared.domain.Belt

/* ----------------------------- מצב תצוגת נושאים ----------------------------- */
internal enum class TopicsViewMode {
    BY_BELT,
    BY_TOPIC
}

/* ---------------------- Details לנושא: תתי־נושאים + ספירת תרגילים ---------------------- */
internal data class TopicDetails(
    val itemCount: Int,
    val subTitles: List<String>
) {
    val hasSubs: Boolean get() = subTitles.isNotEmpty()
}

/* ----------------------------- payload חישובים “לפי נושא” ----------------------------- */
internal data class CountsPayload(
    val subjectCounts: Map<String, Int>,
    val internalDefenseRootCount: Int,
    val externalDefenseRootCount: Int,
    val handsRootCount: Int,
    val totalDefenseCount: Int
)

/* ----------------------------- תצוגת תרגיל בדיאלוגים ----------------------------- */
internal data class UiExercise(
    val raw: String,
    val title: String
)

/* ----------------------------- map “חגורה -> תרגילים” ----------------------------- */
internal typealias ItemsByBelt = Map<Belt, List<UiExercise>>
