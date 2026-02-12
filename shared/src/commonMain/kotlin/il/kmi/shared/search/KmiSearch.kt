package il.kmi.shared.search

import il.kmi.shared.model.*

data class SearchHit(
    val belt: KmiBelt,
    val topic: String,
    val item: String? = null // null => פגיעה בכותרת נושא; אחרת פריט ספציפי
)

object KmiSearch {
    private fun norm(s: String) = s.trim().lowercase()

    fun search(
        repo: Map<KmiBelt, KmiBeltContent>,
        query: String,
        belt: KmiBelt? = null
    ): List<SearchHit> {
        val q = norm(query)
        if (q.isBlank()) return emptyList()

        val belts: Iterable<KmiBelt> = belt?.let { listOf(it) } ?: repo.keys
        val out = mutableListOf<SearchHit>()

        for (b in belts) {
            val content = repo[b] ?: continue
            for (t in content.topics) {
                if (norm(t.title).contains(q)) {
                    out += SearchHit(b, t.title, null)
                }
                val items = if (t.subTopics.isNotEmpty())
                    t.subTopics.flatMap { it.items }
                else
                    t.items
                for (item in items) {
                    if (norm(item).contains(q)) out += SearchHit(b, t.title, item)
                }
            }
        }
        return out
    }
}
