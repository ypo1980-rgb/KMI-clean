package il.kmi.app.ui.assistant.search

data class SearchRequest(
    val query: String,
    val belt: String? = null
)

data class SearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String? = null
)

data class SearchResponse(
    val query: String,
    val results: List<SearchResultItem>
)