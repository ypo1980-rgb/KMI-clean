package il.kmi.shared.domain.catalog

data class CatalogSubTopic(
    val title: String,
    val items: List<String>
)

data class CatalogTopic(
    val title: String,
    val items: List<String> = emptyList(),
    val subTopics: List<CatalogSubTopic> = emptyList()
)