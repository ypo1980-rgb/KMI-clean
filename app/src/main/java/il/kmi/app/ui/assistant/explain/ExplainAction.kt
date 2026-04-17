package il.kmi.app.ui.assistant.explain

data class ExplainRequest(
    val query: String,
    val belt: String? = null,
    val topic: String? = null,
    val exerciseId: String? = null
)

data class ExplainResponse(
    val title: String,
    val shortAnswer: String,
    val bullets: List<String> = emptyList(),
    val followUp: String? = null
)