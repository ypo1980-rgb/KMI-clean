package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.domain.ContentRepo as SharedContentRepo

object CanonicalIds {

    fun cleanItem(topic: String, item: String): String {
        var s = item.trim()

        // מסירים רק topic:: אם קיים
        if (topic.isNotBlank() && s.startsWith("$topic::")) {
            s = s.removePrefix("$topic::").trim()
        }

        // חשוב: לא לעשות substringAfterLast("::")
        // אם יש :: זה חלק מהייחודיות (subTopic::item)

        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    fun norm(s: String): String = s
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .replace("[\\-–—:_]".toRegex(), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase()

    fun findCanonicalItem(belt: Belt, topicTitle: String, displayItem: String): String? {
        val wanted = norm(displayItem)

        // 1) פריטים ישירים של נושא
        val direct = SharedContentRepo.getAllItemsFor(belt, topicTitle, subTopicTitle = null)
        direct.firstOrNull { raw ->
            val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
            norm(disp) == wanted || norm(raw) == wanted
        }?.let { return it }

        // 2) פריטים מתוך תתי-נושאים
        val subs = SharedContentRepo.getSubTopicsFor(belt, topicTitle)
        subs.forEach { st ->
            st.items.firstOrNull { raw ->
                val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
                norm(disp) == wanted || norm(raw) == wanted
            }?.let { return it }
        }

        return null
    }

    fun canonicalFor(belt: Belt, topicTitle: String, displayItem: String): String {
        val cleaned = cleanItem(topicTitle, displayItem)
        return findCanonicalItem(belt, topicTitle, cleaned) ?: cleaned
    }

    fun resolveCanonicalForExplanation(belt: Belt, topicTitle: String, rawItemFromRepo: String): String {
        val displayKey = cleanItem(topicTitle, rawItemFromRepo)
        return findCanonicalItem(belt, topicTitle, displayKey) ?: displayKey
    }

    fun uiDisplayName(topicTitle: String, rawItem: String): String {
        val cleaned = cleanItem(topicTitle, rawItem)
        return ExerciseTitleFormatter.displayName(cleaned).ifBlank { cleaned }.trim()
    }
}
