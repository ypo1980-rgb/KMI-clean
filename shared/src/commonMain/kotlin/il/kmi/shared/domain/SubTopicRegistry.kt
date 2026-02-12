package il.kmi.shared.domain

/**
 * רישום תתי־נושאים ותרגילים פר חגורה.
 * אפשר להרחיב אותו בהמשך או להחליף במקור חיצוני.
 */
object SubTopicRegistry {

    // ─────────────────────────────────────────────────────────────────────────────
    // עזר: פענוח URL-encoding בסיסי (ללא תלות ב-android.net.Uri ב-commonMain)
    // תומך ב-+ => space, וב-%XX הקסדצימלי. אם הפענוח נכשל – מחזיר את הקלט כמו שהוא.
    private fun decodePercent(input: String): String {
        if (input.isEmpty()) return input
        val s = buildString(input.length) {
            var i = 0
            while (i < input.length) {
                val c = input[i]
                when {
                    c == '+' -> {
                        append(' ')
                        i++
                    }
                    c == '%' && i + 2 < input.length -> {
                        val hi = input[i + 1]
                        val lo = input[i + 2]
                        val hex = (hi.digitToIntOrNull(16) ?: -1) shl 4 or
                                (lo.digitToIntOrNull(16) ?: -1)
                        if (hex >= 0) {
                            append(hex.toChar())
                            i += 3
                        } else {
                            // לא חוקי – משאיר כמו שהוא ומתקדם תו אחד כדי לא ליפול ללולאה אינסופית
                            append('%')
                            i++
                        }
                    }
                    else -> {
                        append(c)
                        i++
                    }
                }
            }
        }
        return s
    }
    // ─────────────────────────────────────────────────────────────────────────────

    // נורמליזציה קלה לשמות (מורחבת כדי לשבור הבדלי תווים/מקפים/ניקוד + פענוח %)
    private fun norm(s: String): String =
        decodePercent(s)
            .trim()
            .replace("\u200F", "")        // RLM
            .replace("\u200E", "")        // LRM
            .replace("\u00A0", " ")       // NBSP -> space
            .replace(Regex("[\u0591-\u05C7]"), "") // ניקוד
            // אחידות מקפים: מקף עברי (maqaf) וכל גרסאות ה-hyphen/dash ל'-'
            .replace('\u05BE', '-')       // maqaf
            .replace('\u2010', '-')       // hyphen
            .replace('\u2011', '-')       // non-breaking hyphen
            .replace('\u2012', '-')       // figure dash
            .replace('\u2013', '-')       // en dash
            .replace('\u2014', '-')       // em dash
            .replace('\u2015', '-')       // horizontal bar
            .replace('\u2212', '-')       // minus sign
            .replace(Regex("\\s*-\\s*"), "-")
            .replace(Regex("\\s+"), " ")
            .lowercase()

    /**
     * dataSubTopics – מי הם תתי־הנושאים של כל נושא.
     */
    private val dataSubTopics: Map<Belt, Map<String, List<String>>> = mapOf(
        Belt.WHITE to mapOf(
            "עמידות" to listOf("עמידה בסיסית", "עמידת קרב"),
            "בעיטות" to listOf("בעיטות קדמיות", "בעיטות מעגליות")
        ),
        Belt.YELLOW to mapOf(
            "שחרורים" to listOf(
                "שחרור מאחיזת צוואר צד",
                "שחרור מאחיזת צוואר קדמית"
            )
        ),
        Belt.GREEN to mapOf(
            "שחרורים" to listOf(
                "שחרור מאחיזת צוואר צד",
                "שחרור מאחיזת צוואר אחורית"
            )
        )
        // תרחיב כאן לפי הצורך (BLUE/BROWN/BLACK וכו')
    )

    /**
     * dataItems – מה התרגילים של כל תת־נושא.
     * המפתח: חגורה -> נושא -> תת־נושא -> רשימת תרגילים
     */
    private val dataItems: Map<Belt, Map<String, Map<String, List<String>>>> = mapOf(
        Belt.YELLOW to mapOf(
            "שחרורים" to mapOf(
                "שחרור מאחיזת צוואר צד" to listOf(
                    "שחרור מאחיזת צוואר צד – שלב 1",
                    "שחרור מאחיזת צוואר צד – שלב 2",
                    "שחרור מאחיזת צוואר צד – סיום"
                ),
                "שחרור מאחיזת צוואר קדמית" to listOf(
                    "שחרור מאחיזת צוואר קדמית – דחיפה",
                    "שחרור מאחיזת צוואר קדמית – יציאה הצידה"
                )
            )
        ),
        Belt.GREEN to mapOf(
            "שחרורים" to mapOf(
                "שחרור מאחיזת צוואר צד" to listOf(
                    "אחיזה בצד – חסימה יד קרובה",
                    "אחיזה בצד – מכה נגדית",
                    "אחיזה בצד – שבירת אחיזה וסיום"
                )
                // דוגמה: אפשר להרחיב גם ל"אחורית" כשיהיה קאנון רשמי
                // "שחרור מאחיזת צוואר אחורית" to listOf(...)
            )
        )
        // תוסיף פה BLUE/BROWN וכו' כשיהיה
    )

    /** מחזיר תתי־נושאים של נושא */
    fun getSubTopicsFor(belt: Belt, topicTitle: String): List<String> {
        val beltMap = dataSubTopics[belt] ?: return emptyList()
        val wanted = norm(topicTitle)
        val entry = beltMap.entries.firstOrNull { (key, _) -> norm(key) == wanted }
        return entry?.value ?: emptyList()
    }

    /** מחזיר את כל המפה של נושא→תתי־נושאים לחגורה */
    fun allForBelt(belt: Belt): Map<String, List<String>> =
        dataSubTopics[belt] ?: emptyMap()

    /**
     * מחזיר את התרגילים של תת־נושא מסוים.
     * אם לא מצא – מחזיר רשימה ריקה.
     */
    fun getItemsFor(belt: Belt, topicTitle: String, subTopicTitle: String): List<String> {
        val beltMap = dataItems[belt] ?: return emptyList()
        val topicMap = beltMap.entries.firstOrNull { (k, _) -> norm(k) == norm(topicTitle) }?.value
            ?: return emptyList()
        val items = topicMap.entries.firstOrNull { (k, _) -> norm(k) == norm(subTopicTitle) }?.value
            ?: return emptyList()
        return items
    }
}
