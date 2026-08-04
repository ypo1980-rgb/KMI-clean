package il.kmi.app.search

import il.kmi.app.domain.ContentRepo
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.Explanations

/**
 * תרגיל שנפתר מתוצאת החיפוש ומוכן להצגה בדיאלוג ההסבר.
 */
data class ResolvedExerciseSearchSelection(
    val stableKey: String,
    val title: String,
    val belt: Belt,
    val beltLabel: String,
    val topicTitle: String,
    val explanation: String
)

/**
 * ממיר תוצאה של מנוע החיפוש למידע המלא הדרוש למסך ההסבר.
 */
object ExerciseSearchSelectionResolver {

    fun resolve(
        result: GlobalExerciseSearchEngine.Result,
        isEnglish: Boolean
    ): ResolvedExerciseSearchSelection {
        val rawKey = result.id.trim()

        val resolved = runCatching {
            ContentRepo.resolveItemKey(rawKey)
        }.getOrNull()

        val belt = resolved?.belt
            ?: detectBeltFromSubtitle(
                subtitle = result.subtitle
            )
            ?: Belt.GREEN

        val title = resolved?.itemTitle
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: result.title.trim()
                .ifBlank { rawKey }

        val topicTitle = resolved?.topicTitle
            ?.trim()
            .orEmpty()

        val stableKey =
            if (resolved != null) {
                listOf(
                    resolved.belt.id,
                    resolved.topicTitle,
                    resolved.itemTitle
                ).joinToString("::")
            } else {
                rawKey.ifBlank { title }
            }

        val explanation = Explanations.get(
            belt = belt,
            item = title,
            exerciseId = rawKey.takeIf {
                it.isNotBlank()
            }
        ).trim()

        return ResolvedExerciseSearchSelection(
            stableKey = stableKey.ifBlank { title },
            title = title,
            belt = belt,
            beltLabel = beltLabel(
                belt = belt,
                isEnglish = isEnglish
            ),
            topicTitle = topicTitle,
            explanation = explanation
        )
    }

    private fun detectBeltFromSubtitle(
        subtitle: String?
    ): Belt? {
        val value = subtitle
            ?.lowercase()
            ?.trim()
            .orEmpty()

        return when {
            "צהובה" in value ||
                    "yellow" in value ->
                Belt.YELLOW

            "כתומה" in value ||
                    "orange" in value ->
                Belt.ORANGE

            "ירוקה" in value ||
                    "green" in value ->
                Belt.GREEN

            "כחולה" in value ||
                    "blue" in value ->
                Belt.BLUE

            "חומה" in value ||
                    "brown" in value ->
                Belt.BROWN

            "שחורה" in value ||
                    "black" in value ->
                Belt.BLACK

            else -> null
        }
    }

    private fun beltLabel(
        belt: Belt,
        isEnglish: Boolean
    ): String {
        return when (belt) {
            Belt.YELLOW ->
                if (isEnglish) {
                    "Yellow belt"
                } else {
                    "חגורה צהובה"
                }

            Belt.ORANGE ->
                if (isEnglish) {
                    "Orange belt"
                } else {
                    "חגורה כתומה"
                }

            Belt.GREEN ->
                if (isEnglish) {
                    "Green belt"
                } else {
                    "חגורה ירוקה"
                }

            Belt.BLUE ->
                if (isEnglish) {
                    "Blue belt"
                } else {
                    "חגורה כחולה"
                }

            Belt.BROWN ->
                if (isEnglish) {
                    "Brown belt"
                } else {
                    "חגורה חומה"
                }

            Belt.BLACK ->
                if (isEnglish) {
                    "Black belt"
                } else {
                    "חגורה שחורה"
                }

            else -> belt.name
        }
    }
}
