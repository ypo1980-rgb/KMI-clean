package il.kmi.shared.free_sessions.model

enum class ParticipantState(val order: Int) {
    INVITED(0),
    GOING(1),
    ON_WAY(2),
    ARRIVED(3),
    CANT(4);

    companion object {
        fun fromId(raw: String?): ParticipantState {
            val v = raw?.trim()?.uppercase() ?: return INVITED
            return entries.firstOrNull { it.name == v } ?: INVITED
        }
    }
}
