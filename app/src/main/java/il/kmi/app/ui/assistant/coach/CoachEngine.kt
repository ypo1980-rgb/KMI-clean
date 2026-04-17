package il.kmi.app.ui.assistant.coach

class CoachEngine {

    fun handle(request: CoachRequest): CoachResponse {
        val q = request.query.trim()

        return when {
            q.contains("איך לשפר") || q.contains("שיפור") -> CoachResponse(
                summary = "כדי לשפר ביצוע, כדאי לפרק את התרגיל לשלבים ולעבוד לאט לפני מהירות.",
                recommendedSteps = listOf(
                    "ביצוע איטי ומדויק",
                    "עבודה מול מראה או מאמן",
                    "3 חזרות טכניות לפני חזרות מהירות"
                )
            )

            q.contains("טעויות") -> CoachResponse(
                summary = "הטעויות הנפוצות הן ירידת ידיים, חוסר מרחק נכון, וביצוע מהיר מדי לפני שליטה.",
                recommendedSteps = listOf(
                    "לשמור כיסוי גבוה",
                    "לעבוד על מרחק",
                    "להאט קצב ולחדד טכניקה"
                ),
                warning = "לא למהר להעלות קצב לפני שליטה."
            )

            else -> CoachResponse(
                summary = "אני יכול לעזור כמאמן עם שיפור טכניקה, טעויות נפוצות ובניית דרך תרגול.",
                recommendedSteps = listOf(
                    "בקש תיקון טכניקה",
                    "בקש טעויות נפוצות",
                    "בקש תוכנית תרגול קצרה"
                )
            )
        }
    }
}