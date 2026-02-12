package il.kmi.shared.free_sessions.model

data class FreeSessionPart(
    val uid: String,
    val name: String,
    val state: ParticipantState,
    val updatedAt: Long
)
