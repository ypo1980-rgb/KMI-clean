package il.kmi.shared.practice

import kotlin.math.max

object PracticeFacade {

    fun interface TopicTitlesProvider {
        fun topicTitlesFor(beltId: String): List<String>
    }

    fun interface ItemsProvider {
        fun itemsFor(beltId: String, topicTitle: String): List<String>
    }

    /** סטים שמגיעים מה-Preferences ב-App (unknown_ / fav_) */
    fun interface SetProvider {
        fun getSet(key: String): Set<String>
    }

    /** החרגות לפי חגורה+נושא (כמו ProgressFacade) */
    fun interface ExcludedProvider {
        fun isExcluded(beltId: String, topicTitle: String, rawItem: String, display: String): Boolean
    }

    /**
     * בונה את “הבסיס” לתרגול — items עם origin (belt/topic) + display + canonical,
     * כולל תמיכה בטוקנים מיוחדים:
     * - TOPICS_PICK_TOKEN
     * - __UNKNOWN__
     * - __FAVS_ALL__
     * - __ALL__ / null
     * - topicTitle רגיל
     */
    fun buildPracticeItems(
        request: PracticeRequest,
        topicTitlesProvider: TopicTitlesProvider,
        itemsProvider: ItemsProvider,
        setsProvider: SetProvider,
        excludedProvider: ExcludedProvider,
        canonicalKeyFor: (rawItem: String) -> String,
        displayNameFor: (rawItem: String) -> String
    ): List<PracticeItem> {

        val beltId = request.beltId.trim()
        val filter = request.topicFilter?.trim().orEmpty()

        // 0) TOPICS_PICK_TOKEN: __TOPICS_PICK__:<beltId>|<topicEnc>,<topicEnc>;<beltId>|...
        if (filter.startsWith("${PracticeFilters.TOPICS_PICK_TOKEN}:")) {
            val picked = parseTopicsPickToken(filter)
            val out = mutableListOf<PracticeItem>()
            picked.forEach { (bId, topics) ->
                topics.forEach { topicTitle ->
                    val rawItems = itemsProvider.itemsFor(bId, topicTitle)
                    rawItems.forEach { raw ->
                        val rawTrim = raw.trim()
                        if (rawTrim.isBlank()) return@forEach
                        val disp = displayNameFor(rawTrim).trim()
                        val canon = canonicalKeyFor(rawTrim).trim()
                        if (excludedProvider.isExcluded(bId, topicTitle, rawTrim, disp)) return@forEach

                        out += PracticeItem(
                            beltId = bId,
                            topicTitle = topicTitle,
                            rawTitle = rawTrim,
                            displayTitle = disp.ifBlank { rawTrim },
                            canonicalKey = canon
                        )
                    }
                }
            }
            return out.distinctBy { it.beltId + "|" + it.topicTitle + "|" + it.canonicalKey }
        }

        // 1) UNKNOWN: אוסף מכל הנושאים unknown_<beltId>_<topicTitle>
        if (filter == PracticeFilters.UNKNOWN) {
            val out = mutableListOf<PracticeItem>()
            val topics = topicTitlesProvider.topicTitlesFor(beltId)
            topics.forEach { topicTitle ->
                val key = "unknown_${beltId}_${topicTitle}"
                val set = setsProvider.getSet(key)
                set.forEach { raw ->
                    val rawTrim = raw.trim()
                    if (rawTrim.isBlank()) return@forEach
                    val disp = displayNameFor(rawTrim).trim()
                    val canon = canonicalKeyFor(rawTrim).trim()
                    if (excludedProvider.isExcluded(beltId, topicTitle, rawTrim, disp)) return@forEach

                    out += PracticeItem(
                        beltId = beltId,
                        topicTitle = topicTitle,
                        rawTitle = rawTrim,
                        displayTitle = disp.ifBlank { rawTrim },
                        canonicalKey = canon
                    )
                }
            }
            return out.distinctBy { it.beltId + "|" + it.topicTitle + "|" + it.canonicalKey }
        }

        // 2) FAVS_ALL: אוסף מכל הנושאים fav_<beltId>_<topicTitle>
        if (filter == PracticeFilters.FAVS_ALL) {
            val out = mutableListOf<PracticeItem>()
            val topics = topicTitlesProvider.topicTitlesFor(beltId)
            topics.forEach { topicTitle ->
                val key = "fav_${beltId}_${topicTitle}"
                val set = setsProvider.getSet(key)
                set.forEach { raw ->
                    val rawTrim = raw.trim()
                    if (rawTrim.isBlank()) return@forEach
                    val disp = displayNameFor(rawTrim).trim()
                    val canon = canonicalKeyFor(rawTrim).trim()
                    if (excludedProvider.isExcluded(beltId, topicTitle, rawTrim, disp)) return@forEach

                    out += PracticeItem(
                        beltId = beltId,
                        topicTitle = topicTitle,
                        rawTitle = rawTrim,
                        displayTitle = disp.ifBlank { rawTrim },
                        canonicalKey = canon
                    )
                }
            }
            return out.distinctBy { it.beltId + "|" + it.topicTitle + "|" + it.canonicalKey }
        }

        // 3) ALL (או null/blank): כל הנושאים דרך provider
        val isAll = filter.isBlank() || filter == PracticeFilters.ALL
        if (isAll) {
            val out = mutableListOf<PracticeItem>()
            val topics = topicTitlesProvider.topicTitlesFor(beltId)
            topics.forEach { topicTitle ->
                val rawItems = itemsProvider.itemsFor(beltId, topicTitle)
                rawItems.forEach { raw ->
                    val rawTrim = raw.trim()
                    if (rawTrim.isBlank()) return@forEach
                    val disp = displayNameFor(rawTrim).trim()
                    val canon = canonicalKeyFor(rawTrim).trim()
                    if (excludedProvider.isExcluded(beltId, topicTitle, rawTrim, disp)) return@forEach

                    out += PracticeItem(
                        beltId = beltId,
                        topicTitle = topicTitle,
                        rawTitle = rawTrim,
                        displayTitle = disp.ifBlank { rawTrim },
                        canonicalKey = canon
                    )
                }
            }
            return out.distinctBy { it.beltId + "|" + it.topicTitle + "|" + it.canonicalKey }
        }

        // 4) topicTitle רגיל
        val topicTitle = filter
        val out = mutableListOf<PracticeItem>()
        val rawItems = itemsProvider.itemsFor(beltId, topicTitle)
        rawItems.forEach { raw ->
            val rawTrim = raw.trim()
            if (rawTrim.isBlank()) return@forEach
            val disp = displayNameFor(rawTrim).trim()
            val canon = canonicalKeyFor(rawTrim).trim()
            if (excludedProvider.isExcluded(beltId, topicTitle, rawTrim, disp)) return@forEach

            out += PracticeItem(
                beltId = beltId,
                topicTitle = topicTitle,
                rawTitle = rawTrim,
                displayTitle = disp.ifBlank { rawTrim },
                canonicalKey = canon
            )
        }
        return out.distinctBy { it.beltId + "|" + it.topicTitle + "|" + it.canonicalKey }
    }

    /**
     * אופציונלי (שלב קטן אחרי שזה עובד):
     * בונה רשימה לתרגול עם “שקלול טעויות” בצורה דטרמיניסטית אם נותנים seed.
     */
    fun buildWeightedOrder(
        items: List<PracticeItem>,
        wrongCanonicalKeys: Set<String>,
        wrongWeight: Int = 3,
        seed: Int? = null
    ): List<PracticeItem> {
        if (items.isEmpty()) return emptyList()
        val w = max(1, wrongWeight)

        val expanded = buildList<PracticeItem> {
            for (item in items) {
                val isWrong = wrongCanonicalKeys.contains(item.canonicalKey)
                if (isWrong) repeat(w) { add(item) } else add(item)
            }
        }

        val shuffled: List<PracticeItem> =
            if (seed == null) expanded.shuffled()
            else expanded.shuffled(kotlin.random.Random(seed))

        // “Wrong first”
        return shuffled.sortedBy { item ->
            if (wrongCanonicalKeys.contains(item.canonicalKey)) 0 else 1
        }
    }

    // ------------------------ token parsing ------------------------

    /**
     * מחזיר: Map<beltId, List<topicTitle>>
     * שים לב: decoding של topicEnc נעשה ב-App (כי שם עשית URLDecoder).
     * פה אנחנו משאירים כפי שמגיע — כדי לא לסחוב תלות JVM.
     * בפועל אצלך הטוקן כבר מגיע מפוענח לרוב, ואם לא — נעשה decode בצד App לפני הקריאה.
     */
    private fun parseTopicsPickToken(token: String): Map<String, List<String>> {
        if (!token.startsWith("${PracticeFilters.TOPICS_PICK_TOKEN}:")) return emptyMap()
        val payload = token.removePrefix("${PracticeFilters.TOPICS_PICK_TOKEN}:").trim()
        if (payload.isBlank()) return emptyMap()

        val pairs = payload.split(';').mapNotNull { seg ->
            val parts = seg.split('|', limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val beltId = parts[0].trim()
            val topics = parts[1]
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (beltId.isBlank() || topics.isEmpty()) return@mapNotNull null
            beltId to topics
        }

        return pairs.groupBy({ it.first }, { it.second })
            .mapValues { (_, lists) -> lists.flatten().distinct() }
    }
}
