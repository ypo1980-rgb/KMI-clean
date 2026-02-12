package il.kmi.shared.progress

import il.kmi.shared.catalog.BeltDto
import il.kmi.shared.catalog.KmiCatalogFacade
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo

/**
 * חישוב התקדמות ב-commonMain (ללא Android).
 *
 * הרעיון:
 * - shared עושה את כל הסריקה (חגורות/נושאים/פריטים).
 * - הלקוח (Android/iOS) מספק 2 דברים:
 *    1) excluded: האם פריט מוחרג
 *    2) statusProvider: האם פריט סומן/נלמד
 *
 * ככה iOS יקבל מינימום עבודה: רק לספק provider.
 */
object ProgressFacade {

    data class BeltProgressRow(
        val beltId: String,
        val beltTitle: String,
        val done: Int,
        val total: Int,
        val percent: Int
    )

    /**
     * provider שמחזיר סטטוס:
     *  - true  => mastered
     *  - false => not mastered
     *  - null  => אין מידע/לא זמין (לא סופרים כ-done)
     */
    fun interface StatusProvider {
        fun getStatus(
            beltId: String,
            topicTitle: String,
            canonicalItemKey: String
        ): Boolean?
    }

    /**
     * פונקציה שמחליטה האם פריט מוחרג (למשל לפי SP באנדרואיד).
     */
    fun interface ExcludedProvider {
        fun isExcluded(
            beltId: String,
            topicTitle: String,
            rawItem: String,
            displayName: String
        ): Boolean
    }

    /**
     * מחשב התקדמות לכל החגורות לפי ContentRepo.
     *
     * canonicalKeyFor: פונקציה שמייצרת מפתח עקבי (כמו שאתה עושה עם displayName+normHeb).
     * displayNameFor: פונקציה שמחזירה “שם לתצוגה” (למשל בלי def:*::).
     */
    fun computeBeltsProgress(
        beltsToScan: List<BeltDto> = KmiCatalogFacade.listBelts(),
        excludedProvider: ExcludedProvider,
        statusProvider: StatusProvider,
        canonicalKeyFor: (rawItem: String) -> String,
        displayNameFor: (rawItem: String) -> String
    ): List<BeltProgressRow> {

        val out = mutableListOf<BeltProgressRow>()

        for (beltDto in beltsToScan) {
            // ממירים id -> Belt של shared repo
            val sharedBelt = Belt.fromId(beltDto.id) ?: continue
            val beltContent = ContentRepo.data[sharedBelt] ?: continue

            var done = 0
            var total = 0

            for (topic in beltContent.topics) {
                val topicTitle = topic.title

                val items: List<String> = when {
                    topic.subTopics.isNotEmpty() -> topic.subTopics.flatMap { st -> st.items }
                    topic.items.isNotEmpty() -> topic.items
                    else -> emptyList()
                }

                for (rawItem in items) {
                    val disp = displayNameFor(rawItem)

                    if (excludedProvider.isExcluded(
                            beltId = beltDto.id,
                            topicTitle = topicTitle,
                            rawItem = rawItem,
                            displayName = disp
                        )
                    ) {
                        continue
                    }

                    total++

                    val key = canonicalKeyFor(rawItem)
                    val st = statusProvider.getStatus(beltDto.id, topicTitle, key)

                    if (st == true) done++
                }
            }

            val percent =
                if (total > 0) ((done.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
                else 0

            out += BeltProgressRow(
                beltId = beltDto.id,
                beltTitle = beltDto.title,
                done = done,
                total = total,
                percent = percent
            )
        }

        return out
    }
}
