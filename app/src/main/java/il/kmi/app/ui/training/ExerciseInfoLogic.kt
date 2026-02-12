package il.kmi.app.ui.training

import il.kmi.shared.domain.Belt
import java.util.Locale

object ExerciseInfoLogic {

    // ניקוי כמו אצלך
    fun cleanItem(topic: String, item: String): String {
        var s = item.trim()
        if (topic.isNotBlank() && s.startsWith("$topic::")) s = s.removePrefix("$topic::")
        if ("::" in s) s = s.substringAfterLast("::")
        s = s.replace(Regex(":+"), ":").replace(Regex("\\s+"), " ").trim()
        return s
    }

    private fun norm(s: String) = s
        .replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .replace("[\\-–—:_]".toRegex(), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase(Locale.ROOT)

    // מחפש canonical אמיתי ב-ContentRepo (כולל תתי-נושאים)
    fun findCanonicalItem(b: Belt, topic: String, displayItem: String): String? {
        val wanted = norm(displayItem)

        // 1) פריטים ישירים של הנושא
        val direct: List<String> = runCatching {
            il.kmi.app.domain.ContentRepo.listItemTitles(
                belt = b,
                topicTitle = topic,
                subTopicTitle = null
            )
        }.getOrDefault(emptyList())

        direct.firstOrNull { raw ->
            norm(raw) == wanted || norm(cleanItem(topic, raw)) == wanted
        }?.let { return it }

        // 2) פריטים בתוך תתי-נושאים (לפי שמות תתי-נושאים)
        val subTitles: List<String> = runCatching {
            il.kmi.app.domain.ContentRepo.listSubTopicTitles(b, topic)
        }.getOrDefault(emptyList())

        for (st in subTitles) {
            val items: List<String> = runCatching {
                il.kmi.app.domain.ContentRepo.listItemTitles(
                    belt = b,
                    topicTitle = topic,
                    subTopicTitle = st
                )
            }.getOrDefault(emptyList())

            items.firstOrNull { raw ->
                norm(raw) == wanted || norm(cleanItem(topic, raw)) == wanted
            }?.let { return it }
        }

        return null
    }

    // ✅ המפתח האחיד שעליו עובדים להסבר/שמירות
    fun canonicalFor(belt: Belt, topic: String, displayItem: String): String {
        val cleaned = cleanItem(topic, displayItem)
        return findCanonicalItem(belt, topic, cleaned) ?: cleaned
    }
}
