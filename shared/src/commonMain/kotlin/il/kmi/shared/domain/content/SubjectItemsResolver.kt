package il.kmi.shared.domain.content

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo
import il.kmi.shared.domain.SubjectTopic
import il.kmi.shared.domain.content.Canonical.canonicalItemId
import il.kmi.shared.domain.content.Canonical.normHeb

object SubjectItemsResolver {

    data class UiItem(
        val displayName: String,
        val canonicalId: String,
        val itemKey: String,
        val rawItem: String,
        val topicTitle: String,
        val subTopicTitle: String?
    )

    data class UiSection(
        val title: String,
        val items: List<UiItem>
    )

    /**
     * One resolver for UI (topic/subtopic picker).
     * - Handles regular topics/subtopics
     * ✅ No virtual topics / no filtering. 1:1 from ContentRepo.
     */
    fun resolveByTopic(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String? = null
    ): List<UiSection> {

        val subs = ContentRepo.getSubTopicsFor(belt, topicTitle)
        if (subs.isEmpty()) return emptyList()

        // if subTopic requested -> single section (exact match only)
        if (!subTopicTitle.isNullOrBlank()) {
            val wantedN = subTopicTitle.normHeb()
            val st = subs.firstOrNull { it.title.normHeb() == wantedN } ?: return emptyList()

            val items = st.items.map { rawItem ->
                UiItem(
                    displayName = rawItem.trim(),
                    canonicalId = canonicalItemId(belt, topicTitle, st.title, rawItem),
                    itemKey = ContentRepo.makeItemKey(belt, topicTitle, st.title, rawItem),
                    rawItem = rawItem,
                    topicTitle = topicTitle,
                    subTopicTitle = st.title
                )
            }
            return listOf(UiSection(title = st.title, items = items))
        }

        // else -> all subtopics as sections (1:1)
        return subs.map { st ->
            val items = st.items.map { rawItem ->
                UiItem(
                    displayName = rawItem.trim(),
                    canonicalId = canonicalItemId(belt, topicTitle, st.title, rawItem),
                    itemKey = ContentRepo.makeItemKey(belt, topicTitle, st.title, rawItem),
                    rawItem = rawItem,
                    topicTitle = topicTitle,
                    subTopicTitle = st.title
                )
            }
            UiSection(title = st.title, items = items)
        }
    }

    /**
     * ✅ Subject resolver (cross-platform):
     * UI gives: belt + SubjectTopic
     * shared returns: ready-to-render sections + stable IDs.
     *
     * ✅ No virtual topics / no keyword filtering. 1:1 from ContentRepo.
     * If subject.subTopicHint matches a REAL subtopic title under the topic — we lock to it.
     */
    fun resolveBySubject(
        belt: Belt,
        subject: SubjectTopic
    ): List<UiSection> {

        val topicTitles = subject.topicsByBelt[belt].orEmpty()
        if (topicTitles.isEmpty()) return emptyList()

        fun buildUiItem(topicTitle: String, rawItem: String): UiItem {
            val sub = ContentRepo.findSubTopicTitleForItem(belt, topicTitle, rawItem)
            val canonical = canonicalItemId(belt, topicTitle, sub, rawItem)
            val key = ContentRepo.makeItemKey(belt, topicTitle, sub, rawItem)

            return UiItem(
                displayName = rawItem.trim(),
                canonicalId = canonical,
                itemKey = key,
                rawItem = rawItem,
                topicTitle = topicTitle,
                subTopicTitle = sub
            )
        }

        fun resolveTopicItems(topicTitle: String): List<UiItem> {

            // אם יש hint והוא תת־נושא אמיתי באותו topic — ננעל אליו (exact match)
            val subs = ContentRepo.getSubTopicsFor(belt, topicTitle)
            val exactSubTitle = subject.subTopicHint
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { hint ->
                    val hn = hint.normHeb()
                    subs.firstOrNull { st -> st.title.normHeb() == hn }?.title
                }

            val rawItems = ContentRepo.getAllItemsFor(
                belt = belt,
                topicTitle = topicTitle,
                subTopicTitle = exactSubTitle
            )

            return rawItems
                .filter { it.isNotBlank() }
                .map { raw -> buildUiItem(topicTitle, raw) }
        }

        // Build one section per topic (simple and predictable for UI)
        return topicTitles.mapNotNull { topicTitle ->
            val items = resolveTopicItems(topicTitle)
            if (items.isEmpty()) null
            else UiSection(
                title = topicTitle.trim(),
                items = items
            )
        }
    }
}