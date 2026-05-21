package il.kmi.app.ui.assistant.utils

object HebrewNormalize {
    fun normalize(s: String): String {
        return s
            // סימוני כיוון נסתרים
            .replace("\u200F", "")
            .replace("\u200E", "")

            // רווחים מיוחדים
            .replace("\u00A0", " ")

            // ניקוד וטעמי מקרא
            .replace(Regex("[\u0591-\u05C7]"), "")

            // גרשיים וסימני ציטוט נפוצים
            .replace("״", "\"")
            .replace("׳", "'")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("’", "'")

            // מקפים שונים לאותו סימן
            .replace("־", "-")
            .replace("–", "-")
            .replace("—", "-")

            // שם המותג / קיצור — לנרמול חיפוש פנימי בלבד
            .replace("ק.מ.י", "קמי", ignoreCase = true)
            .replace("ק מ י", "קמי", ignoreCase = true)
            .replace("K.A.M.I", "KAMI", ignoreCase = true)
            .replace("K.M.I", "KAMI", ignoreCase = true)
            .replace("K A M I", "KAMI", ignoreCase = true)
            .replace("K M I", "KAMI", ignoreCase = true)

            // רווחים כפולים
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}