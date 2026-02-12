package il.kmi.app.ui.training

import il.kmi.app.domain.ContentRepo
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

/**
 * מוציא רשימת תרגילים שטוחה מה-ContentRepo לכל החגורות/נושאים/תתי-נושאים.
 * - exerciseId יציב = ContentRepo.makeItemKey(...)
 * - name = שם תרגיל לתצוגה (ללא def:...::)
 * - topic = "חגורה • נושא" או "חגורה • נושא • תת-נושא"
 */
object ContentRepoExerciseAdapter {

    fun buildAllExercisePickItems(): List<ExercisePickItem> {
        val out = mutableListOf<ExercisePickItem>()

        for (belt in Belt.values()) {
            val topicTitles = runCatching { ContentRepo.listTopicTitles(belt) }
                .getOrDefault(emptyList())

            for (topicTitle in topicTitles) {
                // 1) פריטים ישירים של נושא (ללא תתי-נושאים)
                val directItems = runCatching {
                    ContentRepo.listItemTitles(
                        belt = belt,
                        topicTitle = topicTitle,
                        subTopicTitle = null
                    )
                }.getOrDefault(emptyList())

                directItems.forEach { raw ->
                    val display = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
                    if (display.isBlank()) return@forEach

                    val id = ContentRepo.makeItemKey(
                        belt = belt,
                        topicTitle = topicTitle,
                        subTopicTitle = null,
                        itemTitle = display
                    )

                    out += ExercisePickItem(
                        exerciseId = id,
                        name = display,
                        topic = "${belt.heb} • $topicTitle"
                    )
                }

                // 2) פריטים בתוך תתי-נושאים
                val subTitles = runCatching { ContentRepo.listSubTopicTitles(belt, topicTitle) }
                    .getOrDefault(emptyList())

                subTitles.forEach { stTitle ->
                    val subItems = runCatching {
                        ContentRepo.listItemTitles(
                            belt = belt,
                            topicTitle = topicTitle,
                            subTopicTitle = stTitle
                        )
                    }.getOrDefault(emptyList())

                    subItems.forEach { raw ->
                        val display = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
                        if (display.isBlank()) return@forEach

                        val id = ContentRepo.makeItemKey(
                            belt = belt,
                            topicTitle = topicTitle,
                            subTopicTitle = stTitle,
                            itemTitle = display
                        )

                        out += ExercisePickItem(
                            exerciseId = id,
                            name = display,
                            topic = "${belt.heb} • $topicTitle • $stTitle"
                        )
                    }
                }
            }
        }

        return out
            .distinctBy { it.exerciseId }
            .sortedBy { it.name }
    }
}
