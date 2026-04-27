package il.kmi.app.subscription

object AccessModeResolver {

    fun resolve(
        hasManagerAccess: Boolean
    ): AccessMode {
        return if (hasManagerAccess) {
            AccessMode.OPEN
        } else {
            AccessMode.LOCKED
        }
    }
}