package il.kmi.app.onboarding

import androidx.compose.ui.graphics.Color

object OnboardingContent {

    val steps: List<OnboardingStep> = listOf(
        OnboardingStep(
            id = "welcome",
            titleHe = "ברוכים הבאים ל־KAMI",
            titleEn = "Welcome to KAMI",
            descriptionHe = """
        כל חומרי הלימוד, ההתקדמות והמבחנים שלך במקום אחד.

        בסיור קצר נכיר את הכלים המרכזיים באפליקציה.
    """.trimIndent(),
            descriptionEn = """
        Your learning materials, progress and exams in one place.

        This short tour introduces the app's main tools.
    """.trimIndent(),
            imageRes = il.kmi.app.R.drawable.onboarding_welcome,
            accentColor = Color(0xFF6D4ED8)
        ),
        OnboardingStep(
            id = "belts",
            titleHe = "חגורות וחומרי לימוד",
            titleEn = "Belts and learning materials",
            descriptionHe = """
                בחר חגורה כדי לצפות בחומר הרלוונטי לרמה שלך.
                
                בכל חגורה תמצא נושאים, תתי־נושאים ותרגילים המבוססים על תוכן האפליקציה.
            """.trimIndent(),
            descriptionEn = """
                Select a belt to view the material relevant to your level.
                
                Each belt contains subjects, subtopics and exercises from the app's content.
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
            titleHe = "יודע, לא יודע ולא סומן",
            titleEn = "Known, unknown and unmarked",
            descriptionHe = """
                סמן את מצב הידע שלך עבור כל תרגיל:
                
                ירוק – יודע
                אדום – לא יודע
                ללא סימון – עדיין לא נבדק
                
                הסימונים נשמרים ומשמשים לסיכומים, לתרגול ולדוחות PDF.
            """.trimIndent(),
            descriptionEn = """
                Mark your knowledge status for every exercise:
                
                Green – known
                Red – unknown
                Unmarked – not reviewed yet
                
                These marks are saved and used in summaries, practice sessions and PDF reports.
            """.trimIndent(),
            accentColor = Color(0xFF16A34A)
        ),
        OnboardingStep(
            id = "exercise_cards",
            titleHe = "כרטיסיות ותרגול",
            titleEn = "Exercise cards and practice",
            descriptionHe = """
                במסך הכרטיסיות ניתן לעבור בין כל התרגילים, תרגילים שלא ידועים ומועדפים.
                
                ניתן לפתוח הסבר, לשמור הערה אישית ולהתחיל תרגול לפי הרשימה הפעילה.
            """.trimIndent(),
            descriptionEn = """
                The exercise cards screen lets you switch between all exercises, unknown exercises and favorites.
                
                You can open explanations, save personal notes and start a practice session from the active list.
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