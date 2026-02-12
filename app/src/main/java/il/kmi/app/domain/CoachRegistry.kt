package il.kmi.app.domain

/**
 * עטיפה צד-אנדרואיד ל-CoachRegistry של המודול המשותף.
 *
 * כל הקוד ה"אמיתי" יושב ב:
 *   il.kmi.shared.domain.CoachRegistry
 *
 * פה אנחנו רק מפנים אליו, כדי שקוד ישן שמייבא il.kmi.app.domain.CoachRegistry
 * ימשיך להתקמפל בלי שינויים.
 */
object CoachRegistry {

    // לוקחים את האובייקט מה-shared
    private val shared get() = il.kmi.shared.domain.CoachRegistry

    /**
     * האם הקוד קיים ברשימה.
     */
    @JvmStatic
    fun isValid(code: String?): Boolean = shared.isValid(code)

    /**
     * מחזירה את שם המאמן לפי הקוד, או null אם לא קיים.
     */
    @JvmStatic
    fun coachName(code: String?): String? = shared.coachName(code)

    /**
     * שימושי למסכים/אדמין: לקבל את כל הקודים הקיימים.
     */
    @JvmStatic
    fun allCoaches(): Map<String, String> = shared.allCoaches()
}
