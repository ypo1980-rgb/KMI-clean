package il.kmi.shared.free_sessions.data

object FreeSessionsPaths {
    const val ROOT_BRANCHES = "branches"
    const val ROOT_GROUPS = "groups"
    const val COL_FREE_SESSIONS = "free_sessions"
    const val COL_PARTICIPANTS = "participants"

    // branches/{branch}/groups/{groupKey}/free_sessions
    fun freeSessionsCol(branch: String, groupKey: String): String =
        "$ROOT_BRANCHES/${branch.trim()}/$ROOT_GROUPS/${groupKey.trim()}/$COL_FREE_SESSIONS"
}
