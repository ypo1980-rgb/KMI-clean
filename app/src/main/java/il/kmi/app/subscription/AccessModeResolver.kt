package il.kmi.app.subscription

object AccessModeResolver {

    // זמני: פתיחת התכנים במהלך בדיקות סגורות.
    // חובה להחזיר ל-false לפני הפצה ציבורית.
    private const val TESTER_CONTENT_UNLOCK = true

    fun resolve(
        hasManagerAccess: Boolean
    ): AccessMode {

        if (TESTER_CONTENT_UNLOCK) {
            return AccessMode.OPEN
        }

        return if (hasManagerAccess) {
            AccessMode.OPEN
        } else {
            AccessMode.LOCKED
        }
    }
}