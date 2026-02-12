package il.kmi.shared.questions.model.util

/**
 * עוזר חוצה-פלטפורמה לחיפוש:
 * - מנרמל עברית/אנגלית
 * - מפרק שאילתת חיפוש למילות מפתח
 * - מספק "contains-all" ו-"score" פשוטים
 *
 * שים לב: בלי java.text.Normalizer (כדי לעבוד גם ב-iOS/Native).
 */
object SearchKeyParser {

    /**
     * נרמול טקסט לחיפוש:
     * - lower-case (בצורה בטוחה KMP)
     * - מחליף ניקוד/תווים מיוחדים בסיסיים
     * - שומר רק אותיות/ספרות/רווחים
     * - מאחד רווחים
     */
    fun norm(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val s = text.trim()

        val sb = StringBuilder(s.length)
        for (ch in s) {
            val c = ch

            // הורדת ניקוד עברי (טווח ניקוד)
            if (c in '\u0591'..'\u05C7') continue

            val out = when (c) {
                // גרש/גרשיים/תווים דומים -> רווח
                '\u05F3', '\u05F4', '\'', '"', '׳', '״' -> ' '
                // מפרידים נפוצים -> רווח
                '-', '–', '—', '/', '\\', '|', ',', '.', ':', ';', '(', ')', '[', ']', '{', '}', '•', '·' -> ' '
                // טאבים/שורות -> רווח
                '\n', '\r', '\t' -> ' '
                else -> c
            }

            // lowercase בלי Locale
            val lowered = out.lowercaseChar()

            // שמירה רק על אותיות/ספרות/רווח
            if (lowered.isLetterOrDigit() || lowered == ' ') {
                sb.append(lowered)
            } else {
                // כל דבר אחר -> רווח (כדי לא לחבר מילים בטעות)
                sb.append(' ')
            }
        }

        return collapseSpaces(sb.toString())
    }

    /**
     * פירוק שאילתת חיפוש למילות מפתח:
     * - מסיר מילים קצרות מאוד
     * - מסיר כפילויות
     */
    fun parseKeywords(query: String?, minLen: Int = 2): List<String> {
        val n = norm(query)
        if (n.isBlank()) return emptyList()

        val parts = n.split(' ')
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= minLen }
            .toList()

        if (parts.isEmpty()) return emptyList()

        // uniq שומר סדר
        val seen = LinkedHashSet<String>(parts.size)
        for (p in parts) seen.add(p)
        return seen.toList()
    }

    /**
     * בדיקה אם כל מילות המפתח קיימות בטקסט יעד (AND).
     */
    fun containsAll(target: String?, keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return true
        val t = norm(target)
        if (t.isBlank()) return false
        return keywords.all { kw -> kw.isNotBlank() && t.contains(kw) }
    }

    /**
     * ניקוד חיפוש פשוט:
     * +2 לכל התאמת מילה
     * +3 אם התאמה בתחילת מילה
     * +5 אם כל השאילתה מופיעה כמחרוזת רציפה
     */
    fun score(target: String?, query: String?): Int {
        val t = norm(target)
        val q = norm(query)
        if (t.isBlank() || q.isBlank()) return 0

        val kws = parseKeywords(q)
        if (kws.isEmpty()) return 0

        var s = 0
        if (t.contains(q)) s += 5

        for (kw in kws) {
            if (!t.contains(kw)) continue
            s += 2
            if (startsWord(t, kw)) s += 3
        }
        return s
    }

    private fun startsWord(text: String, kw: String): Boolean {
        // מחפש " kw" או תחילת מחרוזת
        if (text.startsWith(kw)) return true
        val idx = text.indexOf(" $kw")
        return idx >= 0
    }

    private fun collapseSpaces(s: String): String {
        val sb = StringBuilder(s.length)
        var prevSpace = true
        for (ch in s) {
            val isSpace = ch == ' '
            if (isSpace) {
                if (!prevSpace) sb.append(' ')
            } else {
                sb.append(ch)
            }
            prevSpace = isSpace
        }
        // trim ידני
        var out = sb.toString()
        if (out.startsWith(' ')) out = out.trimStart()
        if (out.endsWith(' ')) out = out.trimEnd()
        return out
    }
}
