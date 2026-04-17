package il.kmi.app.ui.assistant.explain

class ExplainEngine {

    fun handle(request: ExplainRequest): ExplainResponse {
        val q = request.query.trim()

        return when {
            q.contains("עמידת מוצא") -> ExplainResponse(
                title = "עמידת מוצא",
                shortAnswer = "עמידת מוצא היא בסיס היציבות, ההגנה והיציאה לתגובה.",
                bullets = listOf(
                    "רגל שמאל קדימה",
                    "ברכיים מעט כפופות",
                    "ידיים למעלה במצב הגנה"
                ),
                followUp = "אפשר לשאול גם איך לתרגל את זה נכון."
            )

            q.contains("בעיטה") -> ExplainResponse(
                title = "הסבר על בעיטה",
                shortAnswer = "בתרגול בעיטה חשוב לשמור על שיווי משקל, כיסוי והחזרת הרגל מהר.",
                bullets = listOf(
                    "להרים ברך בצורה נקייה",
                    "לפגוע ולחזור מהר",
                    "לא להפיל ידיים בזמן הביצוע"
                ),
                followUp = "אפשר גם לבקש טעויות נפוצות."
            )

            else -> ExplainResponse(
                title = "הסבר כללי",
                shortAnswer = "מצאתי נושא קרוב לשאלה שלך, אבל צריך יותר הקשר כדי לדייק.",
                bullets = listOf(
                    "שם התרגיל",
                    "חגורה",
                    "נושא"
                ),
                followUp = "כתוב לי את שם התרגיל ואדייק."
            )
        }
    }
}