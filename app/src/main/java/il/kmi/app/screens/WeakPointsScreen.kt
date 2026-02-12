package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeakPointsScreen(
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: (() -> Unit)? = null,
) {
    val backgroundBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF020617),
            Color(0xFF111827),
            Color(0xFF1D4ED8),
            Color(0xFF22D3EE)
        )
    )

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "נקודות תורפה",
                onHome = onOpenHome,                // ✅ אייקון בית בסרגל
                onSearch = onOpenSearch?.let { { it() } }, // ✅ אייקון חיפוש בסרגל (אם יש)
                showBottomActions = true,            // ✅ זה מה שמוסיף את סרגל האייקונים
                showTopHome = false,                 // אצלך לרוב לא רוצים אייקון בית בשורה העליונה
                showRoleStatus = false,              // כמו במסכים אחרים שמבטלים תג/מצב
                centerTitle = true,
                alignTitleEnd = false,
                extraActions = { }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(p)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

        // ⚠️ אזהרה
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFFF3E0),
                border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Report,
                        contentDescription = null,
                        tint = Color(0xFFF57C00)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "לפגיעה בנקודות התורפה יש פוטנציאל נזק גבוה. אין לבצע אלא במצב חירום.\n" +
                                "חל איסור מוחלט לתרגל ללא פיקוח מאמן מוסמך וציוד בטיחות מתאים.\n" +
                                "תרגול שגוי עלול להסתיים בפציעה ואף במוות.",
                        color = Color(0xFF4E342E),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // כותרת קטנה
            Text(
                text = "מפרקים / אצבעות (כללי)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            InfoCard(
                title = "מפרקים",
                body = "כל תנועה כנגד כיוון לתנועה הטבעית – שבר."
            )
            InfoCard(
                title = "שבר באצבע",
                body = "אדם מתעלף במקום."
            )

            SectionTitle("חזית")

            // חזית – מקוצר וברור למסך (כמו הטבלה)
            WeakPointRow("שיער", "ראש", "נקודת אחיזה – ניתן להוציא משיווי משקל.")
            WeakPointRow("מצח", "ראש", "אזור קשה – הפגיעה פחות אפקטיבית יחסית.")
            WeakPointRow("גבה", "ראש", "נקודה רגישה – דימום יכול לרדת לעיניים ולפגוע בראייה.")
            WeakPointRow("עין", "ראש", "פגיעה בעין גורמת לנזק חמור/עיוורון אפשרי.")
            WeakPointRow("גשר האף / שורש האף", "ראש", "פגיעה באף יכולה לגרום לדמעות/דימום ועד שבר עצם האף וזעזוע מוח.")
            WeakPointRow("שורש האף / שפה תחתונה מתחת לאף", "ראש", "נקודה להוצאה משיווי משקל ע״י הרמת שורש האף.")
            WeakPointRow("לסת עליונה", "ראש", "ניתן לשבור שיניים בקלות יחסית ע״י מכה.")
            WeakPointRow("שפתיים", "ראש", "השפה עלולה להיפצע ע״י השיניים.")
            WeakPointRow("לסת פתוחה", "ראש", "קל יותר לשבור ע״י מכה.")
            WeakPointRow("לסת סגורה", "ראש", "זעזוע – קשה יותר לשבור.")
            WeakPointRow("גרוגרת", "ראש", "לחיצה/מכה קדימה – סכנת חיים, דורש טיפול רפואי מיידי.")
            WeakPointRow("שקע הגרוגרת", "ראש", "דימום קל/כאב למספר שניות.")
            WeakPointRow("עצם הבריח", "חלק עליון", "שבר יכול לשתק את הצד ולמנוע תנועת יד בצורה תקינה.")
            WeakPointRow("בית החזה", "פנימי", "שבר בצלעות יכול לגרום לקרע בריאה.")
            WeakPointRow("כבד", "פנימי", "שבר בצלעות יכול לגרום לקרע בכבד.")
            WeakPointRow("מפתח הלב", "פנימי", "פגיעה קשה מאוד – סכנת חיים.")
            WeakPointRow("כליות", "פנימי", "פגיעה בכליה – נזק משמעותי אפשרי.")
            WeakPointRow("בטן", "פנימי", "פגיעה יכולה לגרום לשטף דם פנימי.")
            WeakPointRow("אשכים", "חלק תחתון", "נקודה חלשה מאוד – תגובת כאב חריפה.")
            WeakPointRow("פיקה (ברך)", "חלק תחתון", "ניתן לרסק/לגרום לנזק – נכות אפשרית.")
            WeakPointRow("שוק הרגל", "חלק תחתון", "עצם חשופה יחסית – כאב משמעותי מפגיעה.")
            WeakPointRow("גב כף הרגל", "חלק תחתון", "מבנה עדין – בדריכה הנזק יכול להיות גדול, רצועות עלולות להיקרע.")
            WeakPointRow("שרירים", "כללי", "פגיעה בשריר/כלי דם גורמת כאב ופגיעה בתפקוד.")

            SectionTitle("צד")
            WeakPointRow("פגיעה ברקה", "ראש", "מוות.")
            WeakPointRow("אוזן", "ראש", "קריעת עור התוף – דימום.")
            WeakPointRow("צוואר", "ראש", "פגיעה בכלי דם: עד ~5 שניות עילפון; זמן נוסף/חניקה – סכנת חיים.")
            WeakPointRow("כתף", "חלק עליון", "ניתן להוציא מהמקום ע״י הוצאת העצם מהשקע.")
            WeakPointRow("בית השחי", "חלק עליון", "שריר רגיש מאוד – פגיעה כואבת מאוד.")
            WeakPointRow("צלעות", "חלק עליון", "נכנסות פנימה בקלות; פגיעה בעצב גורמת כאב.")
            WeakPointRow("שבירת צלעות", "חלק עליון", "גרימת קרע בריאה – אפילו מוות.")
            WeakPointRow("שקע הברך מהצד", "חלק תחתון", "קל לשבור ולגרום לקרע – אין התנגדות.")
            WeakPointRow("קרסול", "חלק תחתון", "בפגיעה נכונה (אלכסונית למעלה, ימינה/שמאלה) – נפגע הקרסול וקשה ללכת.")

            SectionTitle("מאחור")
            WeakPointRow("מוח גדול", "ראש", "העצבים בגוף (אינסטינקט).")
            WeakPointRow("מוח קטן", "ראש", "שיווי משקל – פגיעה גורמת לאיבוד שיווי משקל.")
            WeakPointRow("שבירת מפרקת", "ראש", "מוות מיידי.")
            WeakPointRow("פגיעה בחוליות", "חלק עליון", "נזק למפרקת.")
            WeakPointRow("עמוד השדרה", "חלק עליון", "בנוי מחוליות – קשה לגרום לשבר.")
            WeakPointRow("עצם הזנב", "חלק תחתון", "האדם לא יכול לשבת (משם ומטה).")
            WeakPointRow("גיד אכילס", "חלק תחתון", "פגיעה ואי אפשר להזיז את העקב למעלה/למטה.")

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Right,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InfoCard(
    title: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color(0xFF1D4ED8)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                body,
                color = Color(0xFFE5E7EB),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeakPointRow(
    place: String,
    bodyPart: String,
    effect: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = place,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = bodyPart,
                color = Color(0xFFBFDBFE),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = effect,
                color = Color(0xFFE5E7EB),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
