package il.kmi.shared.domain.content

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo
import il.kmi.shared.domain.SubjectTopic
import il.kmi.shared.domain.content.Canonical.canonicalItemId
import il.kmi.shared.domain.content.Canonical.normHeb
import il.kmi.shared.domain.content.Canonical.parseDefenseTagAndName

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
     * - Handles "הגנות פנימיות/חיצוניות" (virtual topic names)
     */
    fun resolveByTopic(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String? = null
    ): List<UiSection> {
        val reqN = topicTitle.normHeb()
        val isVirtualDefense = reqN.contains("הגנות".normHeb()) && (reqN.contains("פנימ") || reqN.contains("חיצונ"))

        // --- Case 1: virtual defense topics from UI ---
        if (isVirtualDefense) {
            val hint = if (reqN.contains("פנימ")) "הגנות פנימיות" else "הגנות חיצוניות"

            // pull ALL raw items from the real topic "הגנות"
            val raw = ContentRepo.getAllItemsFor(belt = belt, topicTitle = "הגנות", subTopicTitle = null)

            val filteredRaw = raw.filter { r ->
                val parsed = parseDefenseTagAndName(r)
                when {
                    hint.contains("חיצונ") -> parsed.tag.contains("external")
                    hint.contains("פנימ") -> parsed.tag.contains("internal")
                    else -> true
                }
            }

            val items = filteredRaw.map { rawItem ->
                val parsed = parseDefenseTagAndName(rawItem)
                UiItem(
                    displayName = parsed.displayName,
                    canonicalId = canonicalItemId(belt, "הגנות", hint, rawItem),
                    itemKey = ContentRepo.makeItemKey(belt, "הגנות", hint, rawItem),
                    rawItem = rawItem,
                    topicTitle = "הגנות",
                    subTopicTitle = hint
                )
            }

            return listOf(UiSection(title = hint, items = items))
        }

        // --- Case 2: normal topics ---
        val subs = ContentRepo.getSubTopicsFor(belt, topicTitle)
        if (subs.isEmpty()) return emptyList()

// if subTopic requested -> single section
        if (!subTopicTitle.isNullOrBlank()) {
            val wantedN = subTopicTitle.normHeb()
            val st = subs.firstOrNull { it.title.normHeb() == wantedN } ?: return emptyList()

            val items = st.items.map { rawItem ->
                val parsed = parseDefenseTagAndName(rawItem)
                UiItem(
                    displayName = parsed.displayName,
                    canonicalId = canonicalItemId(belt, topicTitle, st.title, rawItem),
                    itemKey = ContentRepo.makeItemKey(belt, topicTitle, st.title, rawItem),
                    rawItem = rawItem,
                    topicTitle = topicTitle,
                    subTopicTitle = st.title
                )
            }
            return listOf(UiSection(title = st.title, items = items))
        }

        // else -> all subtopics as sections
        return subs.map { st ->
            val items = st.items.map { rawItem ->
                val parsed = parseDefenseTagAndName(rawItem)
                UiItem(
                    displayName = parsed.displayName,
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
     * Rules supported from SubjectTopic:
     * - topicsByBelt[belt] => list of topic titles to pull from
     * - subTopicHint (optional) => exact subtopic title OR "הגנות פנימיות/חיצוניות"
     * - includeItemKeywords (OR)
     * - requireAllItemKeywords (AND)
     * - excludeItemKeywords
     */
    fun resolveBySubject(
        belt: Belt,
        subject: SubjectTopic
    ): List<UiSection> {

        val topicTitles = subject.topicsByBelt[belt].orEmpty()
        if (topicTitles.isEmpty()) return emptyList()

        val hintN = subject.subTopicHint?.trim()?.takeIf { it.isNotBlank() }?.normHeb()

        fun passKeywords(rawItem: String): Boolean {
            val parsed = parseDefenseTagAndName(rawItem)
            val displayN = parsed.displayName.normHeb()
            val tagN = parsed.tag.normHeb()
            fun hit(kw: String): Boolean = displayN.contains(kw) || tagN.contains(kw)

            val inc = subject.includeItemKeywords.map { it.normHeb() }.filter { it.isNotBlank() }
            val req = subject.requireAllItemKeywords.map { it.normHeb() }.filter { it.isNotBlank() }
            val exc = subject.excludeItemKeywords.map { it.normHeb() }.filter { it.isNotBlank() }

            val okInc = inc.isEmpty() || inc.any { hit(it) }
            val okReq = req.isEmpty() || req.all { hit(it) }
            val okExc = exc.isEmpty() || exc.none { hit(it) }

            return okInc && okReq && okExc
        }

        fun buildUiItem(topicTitle: String, rawItem: String): UiItem {
            val parsed = parseDefenseTagAndName(rawItem)
            val sub = ContentRepo.findSubTopicTitleForItem(belt, topicTitle, rawItem)
                ?: subject.subTopicHint // fallback only if it’s meaningful
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

            val canonical = canonicalItemId(belt, topicTitle, sub, rawItem)
            val key = ContentRepo.makeItemKey(belt, topicTitle, sub, rawItem)

            return UiItem(
                displayName = parsed.displayName,
                canonicalId = canonical,
                itemKey = key,
                rawItem = rawItem,
                topicTitle = topicTitle,
                subTopicTitle = sub
            )
        }

        fun resolveTopicItems(topicTitle: String): List<UiItem> {
            val isDefense = topicTitle.normHeb() == "הגנות".normHeb()

            // Defense topic: use ContentRepo specialized filtering + hint + keywords lists
            val rawItems: List<String> = if (isDefense) {
                ContentRepo.getDefenseItemsFiltered(
                    belt = belt,
                    topicTitle = "הגנות",
                    subTopicHint = subject.subTopicHint,
                    includeItemKeywords = subject.includeItemKeywords,
                    requireAllItemKeywords = subject.requireAllItemKeywords,
                    excludeItemKeywords = subject.excludeItemKeywords
                )
            } else {
                // Non-defense topics: pull items, optionally lock to exact subTopicHint if it matches
                val subs = ContentRepo.getSubTopicsFor(belt, topicTitle)
                val exactSubTitle = subject.subTopicHint
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { hint ->
                        val hn = hint.normHeb()
                        subs.firstOrNull { st -> st.title.normHeb() == hn }?.title
                    }

                val raw = ContentRepo.getAllItemsFor(
                    belt = belt,
                    topicTitle = topicTitle,
                    subTopicTitle = exactSubTitle
                )

                // Apply keyword filters here (defense already applied inside getDefenseItemsFiltered)
                raw.filter { passKeywords(it) }
                    .map { parseDefenseTagAndName(it).displayName.trim() } // keep display clean
            }

            // Map to UiItems (we still keep rawItem as given by list)
            return rawItems
                .filter { it.isNotBlank() }
                .map { raw -> buildUiItem(topicTitle, raw) }
        }

        // Build one section per topic (simple and predictable for UI)
        return topicTitles.mapNotNull { topicTitle ->
            val items = resolveTopicItems(topicTitle)
            if (items.isEmpty()) null
            else {
                val sectionTitle = when {
                    // nice display when hint is used globally
                    hintN != null && hintN.contains("הגנות".normHeb()) -> "${topicTitle.trim()} • ${subject.subTopicHint}"
                    else -> topicTitle.trim()
                }
                UiSection(title = sectionTitle, items = items)
            }
        }
    }
}
