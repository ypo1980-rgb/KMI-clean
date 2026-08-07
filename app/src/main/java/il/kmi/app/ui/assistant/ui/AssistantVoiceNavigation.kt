package il.kmi.app.ui.assistant.ui

// 🔹 פקודות ניווט בקול
sealed class VoiceNavCommand {
    object OpenHome : VoiceNavCommand()
    object OpenTraining : VoiceNavCommand()
    object OpenNextExercise : VoiceNavCommand()
    data class Custom(
        val raw: String
    ) : VoiceNavCommand()
}

/**
 * ניתוח טקסט דיבור לפקודת ניווט ותמיכה
 * ב"יובל" כמילת הפעלה.
 */
internal fun parseVoiceNavCommand(
    raw: String
): VoiceNavCommand? {
    var t = raw.trim()

    if (
        t.startsWith("יובל") ||
        t.lowercase().startsWith("yuval")
    ) {
        t = t
            .removePrefix("יובל")
            .removePrefix("Yuval")
            .removePrefix("yuval")
            .removePrefix(",")
            .trimStart()
    }

    return when {
        "חזור למסך הבית" in t ||
                "חזור לבית" in t ||
                "מסך הבית" in t ||
                "go home" in t.lowercase() ||
                "open home" in t.lowercase() ||
                "home screen" in t.lowercase() ->
            VoiceNavCommand.OpenHome

        "פתח אימון" in t ||
                "פתח את האימון" in t ||
                "open training" in t.lowercase() ||
                "open workout" in t.lowercase() ->
            VoiceNavCommand.OpenTraining

        "התרגיל הבא" in t ||
                "פתח תרגיל הבא" in t ||
                "next exercise" in t.lowercase() ||
                "open next exercise" in t.lowercase() ->
            VoiceNavCommand.OpenNextExercise

        else ->
            null
    }
}