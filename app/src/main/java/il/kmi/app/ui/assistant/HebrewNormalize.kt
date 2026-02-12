package il.kmi.app.ui.assistant

object HebrewNormalize {
    fun normalize(s: String): String {
        return s
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("[\u0591-\u05C7]"), "") // ניקוד וטעמי מקרא
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
