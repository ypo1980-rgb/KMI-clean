package il.kmi.app.ui.assistant.core

object AssistantIntentDetector {

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace("ק.מ.י", "קמי")
            .replace("k.m.i", "kmi")
            .replace("  ", " ")
            .trim()
    }

    private fun scoreTrainings(q: String): Int {
        var score = 0

        if ("אימון" in q) score += 3
        if ("אימונים" in q) score += 3
        if ("האימון הבא" in q) score += 4
        if ("האימון הקרוב" in q) score += 4
        if ("מתי יש אימון" in q) score += 4
        if ("באיזה יום" in q) score += 2
        if ("יום האימון" in q) score += 3

        if ("training" in q) score += 3
        if ("trainings" in q) score += 3
        if ("workout" in q) score += 3
        if ("next training" in q) score += 4
        if ("upcoming training" in q) score += 4
        if ("training day" in q) score += 4
        if ("which day" in q) score += 2

        return score
    }

    private fun scoreMaterial(q: String): Int {
        var score = 0

        if ("חומר" in q) score += 4
        if ("נושא" in q) score += 3
        if ("נושאים" in q) score += 3
        if ("תת נושא" in q || "תת-נושא" in q) score += 4
        if ("קמי" in q) score += 2
        if ("הגנות חיצוניות" in q) score += 4
        if ("שחרורים" in q) score += 3
        if ("עבודת ידיים" in q) score += 3

        if ("material" in q) score += 4
        if ("topic" in q) score += 3
        if ("topics" in q) score += 3
        if ("sub topic" in q || "sub-topic" in q) score += 4
        if ("kmi" in q) score += 2
        if ("external defenses" in q) score += 4
        if ("releases" in q) score += 3
        if ("hand work" in q || "hands" in q) score += 2

        return score
    }

    private fun scoreExercise(q: String): Int {
        var score = 0

        if ("תרגיל" in q) score += 4
        if ("תרגילים" in q) score += 4
        if ("הסבר" in q) score += 3
        if ("הסבר לתרגיל" in q) score += 5
        if ("בעיטה" in q) score += 4
        if ("בעיטות" in q) score += 4
        if ("אגרוף" in q) score += 3
        if ("הגנה" in q) score += 2
        if ("ביצוע" in q) score += 2
        if ("איך עושים" in q) score += 4

        if ("exercise" in q) score += 4
        if ("exercises" in q) score += 4
        if ("explain" in q) score += 3
        if ("kick" in q) score += 4
        if ("kicks" in q) score += 4
        if ("punch" in q) score += 3
        if ("defense" in q) score += 2
        if ("how to do" in q) score += 4
        if ("how do i do" in q) score += 4

        return score
    }

    fun detect(question: String): AssistantIntent {
        val q = normalize(question)

        val scores = listOf(
            AssistantIntentScore(AssistantIntent.TRAININGS, scoreTrainings(q)),
            AssistantIntentScore(AssistantIntent.MATERIAL, scoreMaterial(q)),
            AssistantIntentScore(AssistantIntent.EXERCISE, scoreExercise(q))
        )

        val best = scores.maxByOrNull { it.score } ?: return AssistantIntent.UNKNOWN

        if (best.score <= 0) return AssistantIntent.UNKNOWN

        val topCount = scores.count { it.score == best.score && it.score > 0 }

        if (topCount > 1) {
            return when {
                "הסבר" in q || "explain" in q -> AssistantIntent.EXERCISE
                "חומר" in q || "topic" in q || "material" in q -> AssistantIntent.MATERIAL
                "אימון" in q || "training" in q || "workout" in q -> AssistantIntent.TRAININGS
                else -> best.intent
            }
        }

        return best.intent
    }
}