package il.kmi.app.subscription

object LockedContentPolicy {

    private fun normalizeLockText(raw: String): String =
        raw.trim()
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .lowercase()

    private fun isAlwaysAllowedHardSubject(raw: String): Boolean {
        val t = normalizeLockText(raw)

        return t == "kicks_hard" ||
                t == "kicks" ||
                t == "הגנות נגד בעיטות" ||
                t == "defenses against kicks" ||
                t == "defences against kicks"
    }

    fun isTopicRestricted(title: String): Boolean {
        if (isAlwaysAllowedHardSubject(title)) return false

        val t = normalizeLockText(title)

        return t.contains("הגנות") ||
                t.contains("שחרור") ||
                t.contains("defense") ||
                t.contains("defence") ||
                t.contains("release")
    }

    fun shouldShowLock(
        accessMode: AccessMode,
        title: String
    ): Boolean {
        if (accessMode == AccessMode.OPEN) return false
        if (isAlwaysAllowedHardSubject(title)) return false

        return isTopicRestricted(title)
    }

    fun canOpenTopic(
        accessMode: AccessMode,
        title: String
    ): Boolean {
        if (accessMode == AccessMode.OPEN) return true
        if (isAlwaysAllowedHardSubject(title)) return true

        return !isTopicRestricted(title)
    }
}