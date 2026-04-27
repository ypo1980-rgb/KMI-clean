package il.kmi.app.subscription

object LockedContentPolicy {

    fun isTopicRestricted(title: String): Boolean {
        val t = title.trim().lowercase()

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
        return isTopicRestricted(title)
    }

    fun canOpenTopic(
        accessMode: AccessMode,
        title: String
    ): Boolean {
        if (accessMode == AccessMode.OPEN) return true
        return !isTopicRestricted(title)
    }
}