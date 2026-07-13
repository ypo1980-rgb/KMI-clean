package il.kmi.app.onboarding

import androidx.compose.ui.graphics.Color

object OnboardingContent {

    val steps: List<OnboardingStep> = listOf(
        OnboardingStep(
            id = "welcome",
            titleHe = "מסך הבית",
            titleEn = "Home screen",
            descriptionHe = """
        כל האימונים הקרובים, הודעות המאמן, סיכום ההתקדמות ולוח האימונים החודשי שלך — במקום אחד.
    """.trimIndent(),
            descriptionEn = """
        Your upcoming training sessions, coach messages, progress summary and monthly schedule — all in one place.
    """.trimIndent(),
            imageRes = il.kmi.app.R.drawable.onboarding_welcome,
            accentColor = Color(0xFF6D4ED8)
        ),
        OnboardingStep(
            id = "belts",
            titleHe = "תפריט צד",
            titleEn = "Side menu",
            descriptionHe = """
        תפריט הצד מרכז במקום אחד את כל האזורים החשובים באפליקציה.

        ממנו ניתן לפתוח את הפרופיל האישי, רשימת המתאמנים, מבחנים, תרגילים, תשלומים, יצירת קשר, הפורום, הגדרות השפה וכלי ניהול נוספים.
    """.trimIndent(),
            descriptionEn = """
        The side menu provides quick access to the main areas of the app.

        From here you can open your profile, trainee list, exams, exercises, payments, contact options, the forum, language settings and additional management tools.
    """.trimIndent(),
            accentColor = Color(0xFFF59E0B)
        ),
        OnboardingStep(
            id = "subjects",
            titleHe = "תרגילים לפי נושא",
            titleEn = "Exercises by subject",
            descriptionHe = """
                ניתן לפתוח נושא מסוים ולראות את כל התרגילים השייכים אליו.
                
                במסכים הכוללים מספר חגורות, התרגילים מסודרים בקבוצות ברורות לפי חגורה.
            """.trimIndent(),
            descriptionEn = """
                Open a subject to view all exercises associated with it.
                
                When several belts are included, exercises are arranged in clear belt groups.
            """.trimIndent(),
            accentColor = Color(0xFF0EA5E9)
        ),
        OnboardingStep(
            id = "knowledge_status",
            titleHe = "סיווג תרגילים לפי נושא נבחר",
            titleEn = "Exercise Classification by Selected Topic",
            descriptionHe = """
        במסך זה ניתן לסווג כל תרגיל לפי מצב הידע שלך:

        ירוק – יודע
        אדום – לא יודע
        ללא סימון – עדיין לא נבדק

        הסיווגים נשמרים ומשמשים לסיכומים, לתרגול ממוקד ולדוחות PDF.
    """.trimIndent(),
            descriptionEn = """
        On this screen, you can classify each exercise according to your knowledge status:

        Green – known
        Red – unknown
        Unmarked – not reviewed yet

        These classifications are saved and used for summaries, focused practice sessions and PDF reports.
    """.trimIndent(),
            accentColor = Color(0xFF16A34A)
        ),
        OnboardingStep(
            id = "exercise_cards",
            titleHe = "תרגילים לפי נושא",
            titleEn = "Exercises by Topic",
            descriptionHe = """
        במסך התרגילים לפי נושא יוצגו כל התרגילים השייכים לנושא שנבחר, מכל החגורות הרלוונטיות.

        ניתן לפתוח הסבר מפורט, לשמור הערה אישית ולהתחיל תרגול לפי הרשימה הפעילה.
    """.trimIndent(),
            descriptionEn = """
        The Exercises by Topic screen displays all exercises related to the selected topic across the relevant belts.

        You can open a detailed explanation, save a personal note and start a practice session from the active list.
    """.trimIndent(),
            accentColor = Color(0xFF7C3AED)
        ),
        OnboardingStep(
            id = "internal_exam",
            titleHe = "מבחן פנימי",
            titleEn = "Internal exam",
            descriptionHe = """
                המבחן הפנימי מאפשר לבחור נבחן, להזין ציונים ולשמור תוצאה מסודרת.
                
                בסיום ניתן ליצור דוח PDF הכולל את פרטי המבחן והציונים.
            """.trimIndent(),
            descriptionEn = """
                The internal exam lets you select a trainee, enter scores and save an organized result.
                
                At the end, you can create a PDF report containing the exam details and scores.
            """.trimIndent(),
            accentColor = Color(0xFFDB2777)
        ),
        OnboardingStep(
            id = "pdf",
            titleHe = "יצירת דוחות PDF",
            titleEn = "Creating PDF reports",
            descriptionHe = """
                אייקון השיתוף בסרגל האייקונים יוצר דוח PDF מהנתונים האמיתיים שמופיעים במסך.
                
                לאחר יצירת הקובץ ניתן לפתוח, לשמור או לשתף אותו באמצעות אפליקציות המכשיר.
            """.trimIndent(),
            descriptionEn = """
                The share action in the icon rail creates a PDF report from the real data displayed on the screen.
                
                Once generated, the file can be opened, saved or shared using apps installed on the device.
            """.trimIndent(),
            accentColor = Color(0xFFEC4899)
        ),
        OnboardingStep(
            id = "tools",
            titleHe = "סרגל האייקונים וכלי האפליקציה",
            titleEn = "Icon rail and app tools",
            descriptionHe = """
                סרגל האייקונים מרכז פעולות שימושיות כמו בית, חיפוש, הגדרות, סטטיסטיקה, העוזר החכם ושיתוף.
                
                תמיד ניתן לחזור לסיור הזה דרך אייקון ההסברים שנוסיף לסרגל.
            """.trimIndent(),
            descriptionEn = """
                The icon rail provides quick access to Home, Search, Settings, Statistics, the smart assistant and Share.
                
                You will always be able to reopen this tour using the help action in the rail.
            """.trimIndent(),
            accentColor = Color(0xFF2563EB)
        )
    )
}