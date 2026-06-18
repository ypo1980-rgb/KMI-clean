package il.kmi.app.analytics

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object KmiDiagnostics {

    private fun appVersion(context: Context): String {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName ?: ""} (${info.longVersionCode})"
        }.getOrDefault("")
    }

    private fun userRole(context: Context): String {
        val userSp = context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
        val defaultSp = context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )

        return userSp.getString("user_role", null)
            ?: defaultSp.getString("user_role", null)
            ?: "unknown"
    }

    private fun safeDocId(raw: String): String {
        return raw
            .trim()
            .ifBlank { "unknown_screen" }
            .replace("/", "_")
            .replace("\\", "_")
            .replace(" ", "_")
            .replace(Regex("[^A-Za-z0-9_\\-א-ת]"), "_")
            .take(120)
    }

    fun logEvent(
        context: Context,
        type: String,
        title: String,
        message: String = "",
        area: String = "",
        severity: String = "info",
        extra: Map<String, Any?> = emptyMap()
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val payload = mutableMapOf<String, Any>(
            "type" to type,
            "title" to title,
            "message" to message,
            "area" to area,
            "severity" to severity,
            "userRole" to userRole(context),
            "appVersion" to appVersion(context),
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "language" to java.util.Locale.getDefault().language,
            "createdAt" to FieldValue.serverTimestamp()
        )

        if (!uid.isNullOrBlank()) {
            payload["uid"] = uid
        }

        extra.forEach { (key, value) ->
            if (value != null) {
                payload[key] = value
            }
        }

        Firebase.firestore
            .collection("adminLogs")
            .add(payload)
    }

    fun trackScreen(
        context: Context,
        screenName: String,
        route: String = screenName
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val docId = safeDocId(screenName)

        val screenPayload = mutableMapOf<String, Any>(
            "screenName" to screenName,
            "route" to route,
            "count" to FieldValue.increment(1),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (!uid.isNullOrBlank()) {
            screenPayload["lastUid"] = uid
        }

        Firebase.firestore
            .collection("screen_views")
            .document(docId)
            .set(screenPayload, SetOptions.merge())

        logEvent(
            context = context,
            type = "screen_view",
            title = "צפייה במסך",
            message = screenName,
            area = "screen",
            severity = "info",
            extra = mapOf(
                "screenName" to screenName,
                "route" to route
            )
        )
    }

    fun logSearch(
        context: Context,
        query: String,
        resultCount: Int
    ) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return

        logEvent(
            context = context,
            type = if (resultCount == 0) "search_no_results" else "search_results",
            title = if (resultCount == 0) "חיפוש ללא תוצאה" else "חיפוש באפליקציה",
            message = "query=$cleanQuery, results=$resultCount",
            area = "search",
            severity = if (resultCount == 0) "warning" else "info",
            extra = mapOf(
                "query" to cleanQuery,
                "resultCount" to resultCount
            )
        )
    }
}