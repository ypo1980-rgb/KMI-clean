package il.kmi.shared.questions.model.util

/**
 * Extensions חוצי-פלטפורמה (KMP) לנרמול טקסט וחיפוש.
 *
 * המטרה:
 * - להפסיק כפילויות של normHeb() בקבצי app
 * - לתת API נוח כמו שהיה לך: "someText.normHeb()"
 *
 * בפועל זה עוטף את SearchKeyParser (שהוא KMP-safe).
 */
fun String.normHeb(): String = SearchKeyParser.norm(this)

/**
 * תאימות לאחור: היו לך מקומות עם normHebLocal().
 * אפשר להשאיר אותו זהה לנרמול הרגיל כדי לא לשבור קוד.
 */
fun String.normHebLocal(): String = SearchKeyParser.norm(this)

/** נוח לעבודה עם nullable */
fun String?.normHebOrEmpty(): String = SearchKeyParser.norm(this)

/** מילות מפתח לשאילתה */
fun String?.toSearchKeywords(minLen: Int = 2): List<String> =
    SearchKeyParser.parseKeywords(this, minLen)

/** containsAll חוצה-פלטפורמה */
fun String?.containsAllKeywords(keywords: List<String>): Boolean =
    SearchKeyParser.containsAll(this, keywords)

/** ניקוד חיפוש פשוט */
fun String?.searchScore(query: String?): Int =
    SearchKeyParser.score(this, query)
