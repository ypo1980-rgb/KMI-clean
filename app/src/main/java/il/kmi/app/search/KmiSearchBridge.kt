package il.kmi.app.search

import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.model.KmiBelt
import il.kmi.shared.domain.ContentRepo as SharedContentRepo

/* ===== Helpers (single copy, top-level) ================================== */

/**
 * נרמול אחיד לשאילתות ולשמות התרגילים במאגר.
 *
 * הנרמול מטפל ב:
 * 1. ניקוד וסימנים נסתרים.
 * 2. צואר / צוואר.
 * 3. כל סוגי המקפים.
 * 4. רווחים כפולים.
 */
private fun String.normHeb(): String {
    return this
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace(
            Regex("[\u0591-\u05C7]"),
            ""
        )
        .lowercase()
        .replace(
            oldValue = "צואר",
            newValue = "צוואר",
            ignoreCase = true
        )
        .replace(
            Regex("""\s*[-־–—]\s*"""),
            " "
        )
        .replace(
            Regex("""\s*/\s*"""),
            " "
        )
        .replace(
            Regex("\\s+"),
            " "
        )
        .trim()
}

/**
 * יוצר וריאציות חיפוש מתוך טקסט שהוקלד או נאמר בקול.
 *
 * לדוגמה:
 * "הסבר לי איך עושים בעיטת מגל"
 *
 * יהפוך בין היתר ל:
 * "בעיטת מגל"
 */
private fun voiceExerciseSearchVariants(
    rawQuery: String
): List<String> {
    val normalized = rawQuery.normHeb()

    if (normalized.isBlank()) {
        return emptyList()
    }

    val variants = linkedSetOf<String>()

    fun addVariant(value: String) {
        val clean = value
            .normHeb()
            .trim(' ', '.', ',', '?', '!', ':', ';', '"', '\'')

        if (clean.length >= 2) {
            variants += clean
        }
    }

    addVariant(normalized)

    val prefixes = listOf(
        "הסבר לי על",
        "הסבר לי את",
        "הסבר לי",
        "תסביר לי על",
        "תסביר לי את",
        "תסביר לי",
        "תסביר על",
        "תסביר את",
        "תסביר",
        "הסבר על",
        "הסבר את",
        "הסבר",
        "איך מבצעים את",
        "איך מבצעים",
        "איך עושים את",
        "איך עושים",
        "איך לבצע את",
        "איך לבצע",
        "איך לעשות את",
        "איך לעשות",
        "תראה לי איך עושים את",
        "תראה לי איך עושים",
        "תראה לי את",
        "תראה לי",
        "פתח לי את",
        "פתח לי",
        "פתח את",
        "פתח",
        "מידע על התרגיל",
        "מידע על",
        "תרגיל בשם",
        "תרגיל",
        "explain to me",
        "explain",
        "show me how to do",
        "show me",
        "how do i perform",
        "how do i do",
        "how to perform",
        "how to do",
        "open exercise",
        "open",
        "information about"
    )

    prefixes
        .sortedByDescending { it.length }
        .forEach { prefix ->
            val normalizedPrefix = prefix.normHeb()

            if (normalized.startsWith("$normalizedPrefix ")) {
                addVariant(
                    normalized.removePrefix(normalizedPrefix)
                )
            }
        }

    /*
     * ניקוי ביטויים שעלולים להישאר לאחר הסרת הפתיח הראשי.
     */
    variants
        .toList()
        .forEach { variant ->
            addVariant(
                variant
                    .removePrefix("על התרגיל ")
                    .removePrefix("את התרגיל ")
                    .removePrefix("התרגיל ")
                    .removePrefix("על ")
                    .removePrefix("את ")
            )
        }

    /*
     * התוצאה הקצרה והנקייה ביותר נבדקת ראשונה.
     */
    return variants
        .sortedWith(
            compareBy<String> { value ->
                val containsVoicePrefix = prefixes.any { prefix ->
                    value.startsWith(prefix.normHeb())
                }

                if (containsVoicePrefix) 1 else 0
            }.thenBy { it.length }
        )
}

// המרת KmiBelt -> Belt (מוגדרת כרמה-עליונה, לא בתוך פונקציה)
private fun KmiBelt.toAppBelt(): Belt = when (this) {
    KmiBelt.WHITE  -> Belt.WHITE
    KmiBelt.YELLOW -> Belt.YELLOW
    KmiBelt.ORANGE -> Belt.ORANGE
    KmiBelt.GREEN  -> Belt.GREEN
    KmiBelt.BLUE   -> Belt.BLUE
    KmiBelt.BROWN  -> Belt.BROWN
    KmiBelt.BLACK  -> Belt.BLACK
}

// ✅ ADD: Belt -> KmiBelt (זה המפתח של asSharedRepo)
private fun Belt.toKmiBelt(): KmiBelt = when (this) {
    Belt.WHITE  -> KmiBelt.WHITE
    Belt.YELLOW -> KmiBelt.YELLOW
    Belt.ORANGE -> KmiBelt.ORANGE
    Belt.GREEN  -> KmiBelt.GREEN
    Belt.BLUE   -> KmiBelt.BLUE
    Belt.BROWN  -> KmiBelt.BROWN
    Belt.BLACK  -> KmiBelt.BLACK
}

/** מאתר את החגורה בפועל עבור (topic,item). אם יש hint – נבדוק אותו קודם. */
fun resolveBeltByTopicItem(
    topicTitle: String,
    itemTitle: String,
    hint: Belt? = null
): Belt = resolveBeltByContent(
    topicTitle = topicTitle,
    itemTitle  = itemTitle,
    hint       = hint
)

/* ======================================================================== */

object KmiSearchBridge {

    // --- Public wrapper used by MainApp ---
    @JvmStatic
    fun resolveBeltByTopicItem(
        topicTitle: String,
        itemTitle: String,
        hint: Belt? = null
    ): Belt = resolveBeltByContent(
        topicTitle = topicTitle,
        itemTitle = itemTitle,
        hint = hint
    )

    // ✅ NEW: search provider for top bar / global search
    // חייב להחזיר List<SearchHit> (כמו ש-KmiTopBar/BottomActionsBar מצפים)
    @JvmStatic
    fun searchExercises(
        query: String
    ): List<il.kmi.app.domain.ContentRepo.SearchHit> {
        val cleanQuery =
            query
                .trim()
                .takeIf { it.isNotBlank() }
                ?: return emptyList()

        val normalizedQuery =
            cleanQuery.normHeb()

        val queryWords =
            normalizedQuery
                .split(" ")
                .map { word ->
                    word.trim()
                }
                .filter { word ->
                    word.length >= 2
                }
                .distinct()

        val repoObject: Any =
            il.kmi.app.domain.ContentRepo

        val candidateMethods =
            listOf(
                "searchExercises",
                "search",
                "searchAll",
                "searchItems",
                "searchHits"
            )
                .mapNotNull { methodName ->
                    repoObject.javaClass.methods
                        .firstOrNull { method ->
                            method.name == methodName &&
                                    method.parameterTypes.size == 1
                        }
                }
                .distinctBy { method ->
                    method.name
                }

        fun convertToHits(
            result: Any?
        ): List<il.kmi.app.domain.ContentRepo.SearchHit> {
            val list =
                result as? List<*>
                    ?: return emptyList()

            val directHits =
                list.mapNotNull { value ->
                    value as?
                            il.kmi.app.domain.ContentRepo.SearchHit
                }

            if (directHits.isNotEmpty()) {
                return directHits
            }

            return list
                .mapNotNull { value ->
                    value as? String
                }
                .map { value ->
                    il.kmi.app.domain.ContentRepo.SearchHit(
                        id = value,
                        title = value,
                        subtitle = null
                    )
                }
        }

        /*
         * ניסיון ראשון: החיפוש הרגיל לפי המשפט המלא.
         */
        candidateMethods.forEach { method ->
            val directHits =
                runCatching {
                    convertToHits(
                        method.invoke(
                            repoObject,
                            cleanQuery
                        )
                    )
                }
                    .getOrElse {
                        emptyList()
                    }

            if (directHits.isNotEmpty()) {
                return directHits
            }
        }

        /*
         * ניסיון שני: חיפוש כל מילה בנפרד.
         *
         * לאחר איסוף התוצאות משאירים רק תרגילים
         * שכותרתם המנורמלת מכילה את כל מילות השאילתה.
         * כך המקף אינו משפיע על ההתאמה.
         */
        val wordHits =
            candidateMethods
                .flatMap { method ->
                    queryWords.flatMap { word ->
                        runCatching {
                            convertToHits(
                                method.invoke(
                                    repoObject,
                                    word
                                )
                            )
                        }
                            .getOrElse {
                                emptyList()
                            }
                    }
                }
                .distinctBy { hit ->
                    hit.id ?: hit.title
                }
                .filter { hit ->
                    val normalizedTitle =
                        hit.title.normHeb()

                    queryWords.isNotEmpty() &&
                            queryWords.all { word ->
                                normalizedTitle.contains(word)
                            }
                }

        if (wordHits.isNotEmpty()) {
            return wordHits
        }

        /*
         * ניסיון שלישי: חיפוש ישיר במקור התוכן המשותף.
         */
        val localResults =
            search(
                query = cleanQuery,
                beltFilter = null
            )

        return localResults
            .mapNotNull { hit ->
                val itemTitle =
                    hit.item
                        ?.trim()
                        .orEmpty()

                if (itemTitle.isBlank()) {
                    null
                } else {
                    il.kmi.app.domain.ContentRepo.SearchHit(
                        id =
                            "${hit.belt.id}::" +
                                    "${hit.topic}::" +
                                    itemTitle,
                        title = itemTitle,
                        subtitle =
                            "${hit.belt.heb} • ${hit.topic}"
                    )
                }
            }
            .distinctBy { hit ->
                hit.id ?: hit.title
            }
    }

    /* ====================== read from SHARED ContentRepo (source of truth) ====================== */

    private fun beltNodeOrNull(belt: Belt): Any? {
        // נשאר כדי לא לשבור קריאות קיימות, אבל בפועל אין צורך באובייקט רפלקציה
        return SharedContentRepo.data[belt]
    }

    /** דיבוג מהיר לראות מה נטען (מה-SharedContentRepo בפועל) */
    fun debugDumpRepo(): String = runCatching {
        val keys = SharedContentRepo.data.keys.joinToString(",") { it.name }

        val sample = Belt.order
            .filter { it != Belt.WHITE }
            .associateWith { b -> topicTitlesFor(b).size }
            .entries.joinToString(" | ") { (b, c) -> "${b.name}:$c" }

        "sharedRepoKeys=[$keys] | topicCounts={$sample}"
    }.getOrElse { e -> "sharedRepo: error: ${e.message}" }

    /** ✅ כותרות הנושאים לחגורה (מקור אמת: SharedContentRepo) */
    fun topicTitlesFor(belt: Belt): List<String> {
        return SharedContentRepo.data[belt]
            ?.topics
            ?.map { it.title.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }

    // ✅ תתי-נושאים לנושא
    @JvmStatic
    fun subTopicTitlesFor(belt: Belt, topicTitle: String): List<String> {
        val topic = SharedContentRepo.data[belt]
            ?.topics
            ?.firstOrNull { it.title.trim() == topicTitle.trim() }
            ?: return emptyList()

        return topic.subTopics
            ?.map { it.title.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            .orEmpty()
    }

    // ✅ ספירת פריטים לנושא (אם יש תתי-נושאים נסכום מהם, אחרת items ישירות)
    @JvmStatic
    fun itemCountForTopic(belt: Belt, topicTitle: String): Int {
        val topic = SharedContentRepo.data[belt]
            ?.topics
            ?.firstOrNull { it.title.trim() == topicTitle.trim() }
            ?: return 0

        val subs = topic.subTopics.orEmpty()
        return if (subs.isNotEmpty()) {
            subs.sumOf { it.items.size }
        } else {
            topic.items.size
        }
    }

    /** ✅ כל ה-items של נושא (משטח גם subTopics אם קיימים) */
    fun itemsFor(belt: Belt, topicTitle: String): List<String> {
        val topic = SharedContentRepo.data[belt]
            ?.topics
            ?.firstOrNull { it.title.trim() == topicTitle.trim() }
            ?: return emptyList()

        val subs = topic.subTopics.orEmpty()

        val raw: List<String> = if (subs.isNotEmpty()) {
            subs.flatMap { it.items }
        } else {
            topic.items
        }

        return raw.map { ExerciseTitleFormatter.displayName(it) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /* ====================== end SHARED ====================== */

    /** חיפוש: (אם אתה משתמש בזה עדיין) */
    fun search(
        query: String,
        beltFilter: Belt? = null
    ): List<AppSearchHit> {
        val variants = voiceExerciseSearchVariants(query)

        if (variants.isEmpty()) {
            return emptyList()
        }

        val results = variants.flatMap { variant ->
            localSearchContentRepo(
                query = variant,
                beltFilter = beltFilter
            )
        }

        return results
            .distinctBy { hit ->
                "${hit.belt.name}|${hit.topic}|${hit.item}"
            }
            .sortedWith(
                compareBy<AppSearchHit> { hit ->
                    val normalizedItem =
                        hit.item
                            ?.normHeb()
                            .orEmpty()

                    when {
                        variants.any { variant ->
                            normalizedItem == variant
                        } -> 0

                        variants.any { variant ->
                            normalizedItem.startsWith(variant)
                        } -> 1

                        variants.any { variant ->
                            normalizedItem.contains(variant)
                        } -> 2

                        else -> 3
                    }
                }
                    .thenBy { hit ->
                        hit.item?.length ?: Int.MAX_VALUE
                    }
                    .thenBy { hit ->
                        hit.topic
                    }
            )
            .let { list ->
                if (beltFilter != null) {
                    list.filter { hit ->
                        hit.belt == beltFilter
                    }
                } else {
                    list
                }
            }
    }

    /* ---------- חיפוש מקומי ישיר על ה־ContentRepo ---------- */
    private fun localSearchContentRepo(query: String, beltFilter: Belt?): List<AppSearchHit> {
        val belts: List<Belt> = beltFilter?.let { listOf(it) } ?: Belt.values().toList()
        val results = mutableListOf<AppSearchHit>()

        belts.forEach { belt ->
            val titles: List<String> = topicTitlesFor(belt)

            titles.forEach { topicTitle ->
                val items: List<String> = itemsFor(belt, topicTitle)

                items.forEach { item ->
                    val normalizedItem = item.normHeb()
                    val normalizedQuery = query.normHeb()

                    val queryWords = normalizedQuery
                        .split(" ")
                        .filter { word ->
                            word.length >= 2
                        }

                    val matches =
                        normalizedItem == normalizedQuery ||
                                normalizedItem.contains(normalizedQuery) ||
                                (
                                        queryWords.isNotEmpty() &&
                                                queryWords.all { word ->
                                                    normalizedItem.contains(word)
                                                }
                                        )

                    if (matches) {
                        val fixedBelt = resolveBeltByContent(
                            topicTitle = topicTitle,
                            itemTitle = item,
                            hint = belt
                        )

                        results += AppSearchHit(
                            belt = fixedBelt,
                            topic = topicTitle,
                            item = item
                        )
                    }
                }
            }
        }

        return results
    }
}
