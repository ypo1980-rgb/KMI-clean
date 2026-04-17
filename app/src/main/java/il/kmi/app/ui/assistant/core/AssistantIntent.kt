package il.kmi.app.ui.assistant.core

enum class AssistantIntent {
    EXERCISE,
    MATERIAL,
    TRAININGS,
    UNKNOWN
}

data class AssistantIntentScore(
    val intent: AssistantIntent,
    val score: Int
)