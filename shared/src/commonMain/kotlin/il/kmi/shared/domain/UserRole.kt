package il.kmi.shared.domain

enum class UserRole(val id: String, val heb: String) {
    COACH("coach", "מאמן"),
    TRAINEE("trainee", "מתאמן");

    companion object {
        fun fromId(id: String?): UserRole? =
            values().firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }
    }
}
