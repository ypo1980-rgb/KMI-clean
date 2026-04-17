package il.kmi.app.ui.assistant.coach

data class CoachRequest(
    val query: String,
    val belt: String? = null,
    val topic: String? = null,
    val traineeLevel: String? = null
)

data class CoachResponse(
    val summary: String,
    val recommendedSteps: List<String> = emptyList(),
    val warning: String? = null
)