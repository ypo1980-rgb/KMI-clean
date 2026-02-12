package il.kmi.app.domain

/**
 * עטיפה ל-UserRole מהמודול המשותף, כדי שקוד ישן שמייבא
 * il.kmi.app.domain.UserRole ימשיך לעבוד.
 */
enum class UserRole(val id: String, val heb: String) {
    COACH("coach", "מאמן"),
    TRAINEE("trainee", "מתאמן");

    companion object {
        fun fromId(id: String?): UserRole? =
            il.kmi.shared.domain.UserRole.fromId(id)?.toApp()
    }
}

/**
 * מיפוי מהטיפוס המשותף לטיפוס של האפליקציה.
 */
private fun il.kmi.shared.domain.UserRole.toApp(): UserRole =
    when (this) {
        il.kmi.shared.domain.UserRole.COACH   -> UserRole.COACH
        il.kmi.shared.domain.UserRole.TRAINEE -> UserRole.TRAINEE
    }
