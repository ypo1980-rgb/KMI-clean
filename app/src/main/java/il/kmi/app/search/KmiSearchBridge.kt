package il.kmi.app.search

import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.model.KmiBelt
import il.kmi.shared.domain.ContentRepo as SharedContentRepo

/* ===== Helpers (single copy, top-level) ================================== */

private fun String.normHeb(): String = this
    .replace("\u200F", "")
    .replace("\u200E", "")
    .replace("\u00A0", " ")
    .replace(Regex("[\u0591-\u05C7]"), "")
    .trim()
    .replace(Regex("\\s+"), " ")
    .lowercase()

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
    fun searchExercises(query: String): List<il.kmi.app.domain.ContentRepo.SearchHit> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        val repoObj: Any = il.kmi.app.domain.ContentRepo

        // שמות אפשריים לפונקציה קיימת אצלך ב-ContentRepo
        val candidates = listOf(
            "searchExercises",
            "search",
            "searchAll",
            "searchItems",
            "searchHits"
        )

        // 1) נסה להחזיר ישירות List<SearchHit> אם קיימת פונקציה כזאת
        for (name in candidates) {
            runCatching {
                val m = repoObj.javaClass.methods.firstOrNull {
                    it.name == name && it.parameterTypes.size == 1
                } ?: return@runCatching

                val res = m.invoke(repoObj, q)
                when (res) {
                    is List<*> -> {
                        val asHits = res.mapNotNull { it as? il.kmi.app.domain.ContentRepo.SearchHit }
                        if (asHits.isNotEmpty()) return asHits

                        // 2) אם חזר List<String> – נעטוף ל-SearchHit
                        val asStrings = res.mapNotNull { it as? String }
                        if (asStrings.isNotEmpty()) {
                            return asStrings.map { s ->
                                il.kmi.app.domain.ContentRepo.SearchHit(
                                    id = s,
                                    title = s,
                                    subtitle = null
                                )
                            }
                        }
                    }
                }
            }
        }

        // אם אין חיפוש מובנה — לא מפילים קומפילציה
        return emptyList()
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
    fun search(query: String, beltFilter: Belt? = null): List<AppSearchHit> {
        val qn = query.normHeb()
        if (qn.isBlank()) return emptyList()

        // 👇 כרגע אנחנו לא תלויים ב-KmiSearch בכלל כדי לא להיתקע על API/גרסאות.
        // אפשר להחזיר רק localSearch לפי Shared repo (source of truth).
        val local: List<AppSearchHit> = localSearchContentRepo(query = qn, beltFilter = beltFilter)

        return local
            .distinctBy { "${it.belt.name}|${it.topic}|${it.item}" }
            .sortedWith(compareBy<AppSearchHit> { it.topic }.thenBy { it.item })
            .let { list -> if (beltFilter != null) list.filter { it.belt == beltFilter } else list }
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
                    if (item.normHeb().contains(query)) {
                        val fixedBelt = resolveBeltByContent(topicTitle, item, belt)
                        results += AppSearchHit(
                            belt  = fixedBelt,
                            topic = topicTitle,
                            item  = item
                        )
                    }
                }
            }
        }

        return results
    }
}
