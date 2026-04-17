package il.kmi.app.ui.assistant.search

class SearchEngine {

    fun handle(request: SearchRequest): SearchResponse {
        val q = request.query.trim()

        val all = listOf(
            SearchResultItem("stance_001", "עמידת מוצא", "יסודות"),
            SearchResultItem("kick_101", "הגנות נגד בעיטות", "בעיטות"),
            SearchResultItem("knife_201", "הגנות מסכין", "נשק קר"),
            SearchResultItem("release_301", "שחרורים", "אחיזות וחניקות")
        )

        val filtered = if (q.isBlank()) {
            all
        } else {
            all.filter {
                it.title.contains(q, ignoreCase = true) ||
                        (it.subtitle?.contains(q, ignoreCase = true) == true)
            }
        }

        return SearchResponse(
            query = q,
            results = filtered
        )
    }
}