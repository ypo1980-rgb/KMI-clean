package il.kmi.shared.domain

/**
 * מאגר קודי מאמנים בסיסי (KMP, רץ ב-commonMain).
 * אפשר להחליף אחר כך במקור חיצוני (API / Firestore / Sheet).
 */
object CoachRegistry {

    private val codes: Map<String, String> = mapOf(
        // דוגמאות – תעדכן לקודים האמיתיים שלך
        "11111"       to "יוני מלסה — נתניה",
        "22222"       to "גל חג'ג' — נתניה",
        "KMI-TA-003"  to "רפי נחום — תל אביב",
        "KMI-NZ-004"  to "מאור חקק — נס ציונה",

        // 👇 קוד בדיקות למאמן (לבחינה באפליקציה)
        "12345"       to "יובל פולק - בדיקות"
    )

    /**
     * האם הקוד קיים ברשימה.
     */
    fun isValid(code: String?): Boolean {
        val c = code?.trim().orEmpty()
        if (c.isEmpty()) return false
        // אנחנו מאחידים ל-uppercase כדי ש"Kmi-ta-003" יעבוד
        return codes.containsKey(c.uppercase())
    }

    /**
     * מחזירה את שם המאמן לפי הקוד, או null אם לא קיים.
     */
    fun coachName(code: String?): String? =
        code
            ?.trim()
            ?.uppercase()
            ?.let { codes[it] }

    /**
     * שימושי למסכים/אדמין: לקבל את כל הקודים הקיימים.
     */
    fun allCoaches(): Map<String, String> = codes
}
