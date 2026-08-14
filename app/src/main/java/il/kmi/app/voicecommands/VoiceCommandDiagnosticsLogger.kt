package il.kmi.app.voicecommands

import android.content.Context
import android.os.Build
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

object VoiceCommandDiagnosticsLogger {

    private const val MAX_TEXT_LENGTH = 300
    private const val MAX_ALTERNATIVES = 5

    fun logFailure(
        context: Context,
        source: String,
        reason: String,
        spokenText: String? = null,
        alternatives: List<String> = emptyList(),
        errorCode: Int? = null,
        screenName: String? = null
    ) {
        val appContext = context.applicationContext

        val role = readUserRole(appContext)
        val language = appContext.resources.configuration
            .locales
            .get(0)
            .language

        val safeSpokenText = sanitize(spokenText.orEmpty())
        val safeAlternatives = alternatives
            .take(MAX_ALTERNATIVES)
            .map(::sanitize)
            .filter { it.isNotBlank() }

        val message = buildString {
            append("source=").append(source)
            append("\nreason=").append(reason)

            if (safeSpokenText.isNotBlank()) {
                append("\nspokenText=").append(safeSpokenText)
            }

            if (safeAlternatives.isNotEmpty()) {
                append("\nalternatives=")
                append(safeAlternatives.joinToString(" | "))
            }

            errorCode?.let {
                append("\nerrorCode=").append(it)
            }

            if (!screenName.isNullOrBlank()) {
                append("\nscreen=").append(sanitize(screenName))
            }
        }

        val data = hashMapOf<String, Any>(
            "type" to "voice_command_failed",
            "title" to failureTitle(reason),
            "message" to message,
            "area" to "voice_commands",
            "severity" to "warning",
            "source" to source,
            "reason" to reason,
            "spokenText" to safeSpokenText,
            "alternatives" to safeAlternatives,
            "userRole" to role,
            "language" to language,
            "appVersion" to readAppVersion(appContext),
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            "attemptId" to UUID.randomUUID().toString(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        if (!screenName.isNullOrBlank()) {
            data["screenName"] = sanitize(screenName)
        }

        errorCode?.let {
            data["errorCode"] = it
        }

        // כשל בכתיבת לוג לא צריך להפיל את מנגנון הפקודות.
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("adminLogs")
                .add(data)
        }
    }

    fun logTrace(
        context: Context,
        stage: String,
        spokenText: String? = null,
        alternatives: List<String> = emptyList(),
        resolvedCommand: String? = null,
        target: String? = null,
        screenName: String? = null
    ) {
        val appContext = context.applicationContext

        val safeSpokenText = sanitize(spokenText.orEmpty())
        val safeAlternatives = alternatives
            .take(MAX_ALTERNATIVES)
            .map(::sanitize)
            .filter { it.isNotBlank() }

        val message = buildString {
            append("stage=").append(stage)

            if (safeSpokenText.isNotBlank()) {
                append("\nspokenText=").append(safeSpokenText)
            }

            if (safeAlternatives.isNotEmpty()) {
                append("\nalternatives=")
                append(safeAlternatives.joinToString(" | "))
            }

            if (!resolvedCommand.isNullOrBlank()) {
                append("\nresolvedCommand=").append(resolvedCommand)
            }

            if (!target.isNullOrBlank()) {
                append("\ntarget=").append(target)
            }

            if (!screenName.isNullOrBlank()) {
                append("\nscreen=").append(screenName)
            }
        }

        val data = hashMapOf<String, Any>(
            "type" to "voice_command_trace",
            "title" to "Voice command: $stage",
            "message" to message,
            "area" to "voice_commands",
            "severity" to "info",
            "stage" to stage,
            "spokenText" to safeSpokenText,
            "alternatives" to safeAlternatives,
            "userRole" to readUserRole(appContext),
            "language" to appContext.resources.configuration
                .locales
                .get(0)
                .language,
            "appVersion" to readAppVersion(appContext),
            "deviceModel" to
                    "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            "attemptId" to UUID.randomUUID().toString(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        resolvedCommand?.let {
            data["resolvedCommand"] = sanitize(it)
        }

        target?.let {
            data["target"] = sanitize(it)
        }

        screenName?.let {
            data["screenName"] = sanitize(it)
        }

        runCatching {
            FirebaseFirestore.getInstance()
                .collection("adminLogs")
                .add(data)
        }
    }

    private fun failureTitle(reason: String): String {
        return when (reason) {
            "recognition_unavailable" ->
                "Speech recognition unavailable"

            "recognition_start_failed" ->
                "Unable to start voice recognition"

            "empty_recognition_results" ->
                "No speech recognition results"

            "command_not_understood" ->
                "Voice command not understood"

            "recognition_error" ->
                "Speech recognition failed"

            "recognition_timeout" ->
                "Voice recognition timed out"

            "microphone_permission_denied" ->
                "Microphone permission denied"

            "command_execution_failed" ->
                "Voice command execution failed"

            "voice_feature_not_implemented" ->
                "Recognized voice feature is not implemented"

            "belt_has_no_exercises" ->
                "Requested belt has no exercises"

            else ->
                "Voice command failure"
        }
    }

    private fun readUserRole(context: Context): String {
        val userPrefs = context.getSharedPreferences(
            "kmi_user",
            Context.MODE_PRIVATE
        )

        val appPrefs = context.getSharedPreferences(
            "kmi_prefs",
            Context.MODE_PRIVATE
        )

        return userPrefs.getString("kmi.user.role", null)
            ?: userPrefs.getString("user_role", null)
            ?: userPrefs.getString("role", null)
            ?: appPrefs.getString("kmi.user.role", null)
            ?: appPrefs.getString("user_role", null)
            ?: appPrefs.getString("role", null)
            ?: "unknown"
    }

    private fun readAppVersion(context: Context): String {
        return runCatching {
            val packageInfo = context.packageManager
                .getPackageInfo(context.packageName, 0)

            packageInfo.versionName.orEmpty()
        }.getOrDefault("")
    }

    private fun sanitize(value: String): String {
        return value
            .replace(Regex("[\\r\\n]+"), " ")
            .replace(
                Regex(
                    "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
                ),
                "[email]"
            )
            .replace(
                Regex("(?<!\\d)(?:\\+972|0)?5\\d[- ]?\\d{3}[- ]?\\d{4}(?!\\d)"),
                "[phone]"
            )
            .trim()
            .take(MAX_TEXT_LENGTH)
    }
}