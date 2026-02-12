package il.kmi.app.free_sessions.ui.navigation

import android.net.Uri

object FreeSessionsRoute {
    const val route = "free_sessions/{branch}/{groupKey}/{uid}/{name}"

    fun build(
        branch: String,
        groupKey: String,
        uid: String,
        name: String
    ): String = "free_sessions/${Uri.encode(branch)}/${Uri.encode(groupKey)}/${Uri.encode(uid)}/${Uri.encode(name)}"
}
