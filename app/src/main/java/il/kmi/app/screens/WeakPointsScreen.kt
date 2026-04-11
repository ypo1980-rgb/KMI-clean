package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeakPointsScreen(
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he

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
                title = tr("נקודות תורפה", "Weak Points"),
                onHome = onOpenHome,
                onSearch = onOpenSearch?.let { { it() } },
                showBottomActions = true,
                showTopHome = false,
                showRoleStatus = false,
                centerTitle = true,
                alignTitleEnd = false,
                extraActions = { },
                currentLang = if (isEnglish) "en" else "he",
                onToggleLanguage = {
                    val newLang =
                        if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    (ctx as? Activity)?.recreate()
                }
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
                        text = if (isEnglish) {
                            "Striking weak points has a high potential for severe harm. Do not use except in a real emergency.\n" +
                                    "Training without the supervision of a certified coach and proper safety equipment is strictly forbidden.\n" +
                                    "Incorrect practice may result in serious injury or even death."
                        } else {
                            "לפגיעה בנקודות התורפה יש פוטנציאל נזק גבוה. אין לבצע אלא במצב חירום.\n" +
                                    "חל איסור מוחלט לתרגל ללא פיקוח מאמן מוסמך וציוד בטיחות מתאים.\n" +
                                    "תרגול שגוי עלול להסתיים בפציעה ואף במוות."
                        },
                        color = Color(0xFF4E342E),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // כותרת קטנה
            Text(
                text = tr("מפרקים / אצבעות (כללי)", "Joints / Fingers (General)"),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            InfoCard(
                title = tr("מפרקים", "Joints"),
                body = tr(
                    "כל תנועה כנגד כיוון לתנועה הטבעית – שבר.",
                    "Any movement against the joint's natural direction may cause a fracture."
                ),
                isEnglish = isEnglish
            )
            InfoCard(
                title = tr("שבר באצבע", "Finger Fracture"),
                body = tr(
                    "אדם מתעלף במקום.",
                    "A severe finger break may cause immediate collapse from pain."
                ),
                isEnglish = isEnglish
            )

            SectionTitle(
                text = tr("חזית", "Front"),
                isEnglish = isEnglish
            )

            // חזית – מקוצר וברור למסך (כמו הטבלה)
            WeakPointRow(
                place = tr("שיער", "Hair"),
                bodyPart = tr("ראש", "Head"),
                effect = tr("נקודת אחיזה – ניתן להוציא משיווי משקל.", "A gripping point that can be used to break balance."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("מצח", "Forehead"),
                bodyPart = tr("ראש", "Head"),
                effect = tr("אזור קשה – הפגיעה פחות אפקטיבית יחסית.", "A hard area - generally less effective to strike."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("גבה", "Eyebrow"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "נקודה רגישה – דימום יכול לרדת לעיניים ולפגוע בראייה.",
                    "A sensitive point – bleeding may flow into the eyes and affect vision."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("עין", "Eye"),
                bodyPart = tr("ראש", "Head"),
                effect = tr("פגיעה בעין גורמת לנזק חמור/עיוורון אפשרי.", "A strike to the eye may cause severe injury or possible blindness."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("גשר האף / שורש האף", "Nasal Bridge / Nose Root"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "פגיעה באף יכולה לגרום לדמעות/דימום ועד שבר עצם האף וזעזוע מוח.",
                    "A strike to the nose may cause tearing, bleeding, a broken nose, or even concussion."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("שורש האף / שפה תחתונה מתחת לאף", "Nose Root / Upper Lip"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "נקודה להוצאה משיווי משקל ע״י הרמת שורש האף.",
                    "A control point that can break balance by lifting the nose root."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("לסת עליונה", "Upper Jaw"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "ניתן לשבור שיניים בקלות יחסית ע״י מכה.",
                    "Teeth may break relatively easily with a strong strike."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("שפתיים", "Lips"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "השפה עלולה להיפצע ע״י השיניים.",
                    "The lips may be cut by the teeth during impact."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("לסת פתוחה", "Open Jaw"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "קל יותר לשבור ע״י מכה.",
                    "Easier to break when struck while the jaw is open."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("לסת סגורה", "Closed Jaw"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "זעזוע – קשה יותר לשבור.",
                    "May cause shock; harder to break when closed."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("גרוגרת", "Throat"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "לחיצה/מכה קדימה – סכנת חיים, דורש טיפול רפואי מיידי.",
                    "Forward pressure or strike – life-threatening and requires immediate medical treatment."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("שקע הגרוגרת", "Throat Hollow"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "דימום קל/כאב למספר שניות.",
                    "May cause brief pain or minor bleeding."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("עצם הבריח", "Clavicle"),
                bodyPart = tr("חלק עליון", "Upper Body"),
                effect = tr(
                    "שבר יכול לשתק את הצד ולמנוע תנועת יד בצורה תקינה.",
                    "A fracture can disable the side and prevent normal arm movement."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("בית החזה", "Chest"),
                bodyPart = tr("פנימי", "Internal"),
                effect = tr(
                    "שבר בצלעות יכול לגרום לקרע בריאה.",
                    "Broken ribs may puncture the lung."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("כבד", "Liver"),
                bodyPart = tr("פנימי", "Internal"),
                effect = tr(
                    "שבר בצלעות יכול לגרום לקרע בכבד.",
                    "Broken ribs may cause liver rupture."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("מפתח הלב", "Solar Plexus"),
                bodyPart = tr("פנימי", "Internal"),
                effect = tr(
                    "פגיעה קשה מאוד – סכנת חיים.",
                    "A very dangerous strike – potentially life-threatening."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("כליות", "Kidneys"),
                bodyPart = tr("פנימי", "Internal"),
                effect = tr(
                    "פגיעה בכליה – נזק משמעותי אפשרי.",
                    "A strike to the kidney may cause serious damage."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("בטן", "Abdomen"),
                bodyPart = tr("פנימי", "Internal"),
                effect = tr(
                    "פגיעה יכולה לגרום לשטף דם פנימי.",
                    "A strike may cause internal bleeding."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("אשכים", "Groin"),
                bodyPart = tr("חלק תחתון", "Lower Body"),
                effect = tr(
                    "נקודה חלשה מאוד – תגובת כאב חריפה.",
                    "A very vulnerable point causing intense pain."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("פיקה (ברך)", "Kneecap"),
                bodyPart = tr("חלק תחתון", "Lower Body"),
                effect = tr(
                    "ניתן לרסק/לגרום לנזק – נכות אפשרית.",
                    "Can be crushed or damaged – possible disability."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("שוק הרגל", "Shin"),
                bodyPart = tr("חלק תחתון", "Lower Body"),
                effect = tr(
                    "עצם חשופה יחסית – כאב משמעותי מפגיעה.",
                    "Relatively exposed bone – a strike causes significant pain."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("גב כף הרגל", "Top of the Foot"),
                bodyPart = tr("חלק תחתון", "Lower Body"),
                effect = tr(
                    "מבנה עדין – בדריכה הנזק יכול להיות גדול, רצועות עלולות להיקרע.",
                    "Delicate structure – stepping on it may cause serious damage and ligament tears."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("שרירים", "Muscles"),
                bodyPart = tr("כללי", "General"),
                effect = tr(
                    "פגיעה בשריר/כלי דם גורמת כאב ופגיעה בתפקוד.",
                    "Damage to muscle or blood vessels causes pain and reduced function."
                ),
                isEnglish = isEnglish
            )
            SectionTitle(
                text = tr("צד", "Side"),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("פגיעה ברקה", "Temple"),
                bodyPart = tr("ראש", "Head"),
                effect = tr("מוות.", "Potentially fatal."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("אוזן", "Ear"),
                bodyPart = tr("ראש", "Head"),
                effect = tr("קריעת עור התוף – דימום.", "A ruptured eardrum may cause bleeding."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("צוואר", "Neck"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "פגיעה בכלי דם: עד ~5 שניות עילפון; זמן נוסף/חניקה – סכנת חיים.",
                    "Strike to blood vessels: up to ~5 seconds unconsciousness; longer pressure/choking – life-threatening."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("כתף", "Shoulder"),
                bodyPart = tr("חלק עליון", "Upper Body"),
                effect = tr(
                    "ניתן להוציא מהמקום ע״י הוצאת העצם מהשקע.",
                    "Can be dislocated by forcing the joint out of its socket."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("בית השחי", "Armpit"),
                bodyPart = tr("חלק עליון", "Upper Body"),
                effect = tr(
                    "שריר רגיש מאוד – פגיעה כואבת מאוד.",
                    "Very sensitive muscle area – a strike causes intense pain."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("צלעות", "Ribs"),
                bodyPart = tr("חלק עליון", "Upper Body"),
                effect = tr(
                    "נכנסות פנימה בקלות; פגיעה בעצב גורמת כאב.",
                    "Relatively vulnerable; nerve impact causes strong pain."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("שבירת צלעות", "Broken Ribs"),
                bodyPart = tr("חלק עליון", "Upper Body"),
                effect = tr(
                    "גרימת קרע בריאה – אפילו מוות.",
                    "May cause lung rupture – potentially fatal."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("שקע הברך מהצד", "Side of Knee Joint"),
                bodyPart = tr("חלק תחתון", "Lower Body"),
                effect = tr(
                    "קל לשבור ולגרום לקרע – אין התנגדות.",
                    "Easy to damage or tear ligaments due to limited resistance."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("קרסול", "Ankle"),
                bodyPart = tr("חלק תחתון", "Lower Body"),
                effect = tr(
                    "בפגיעה נכונה (אלכסונית למעלה, ימינה/שמאלה) – נפגע הקרסול וקשה ללכת.",
                    "A correct diagonal strike (upward/right/left) can damage the ankle and make walking difficult."
                ),
                isEnglish = isEnglish
            )
            SectionTitle(
                text = tr("מאחור", "Back"),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("מוח גדול", "Cerebrum"),
                bodyPart = tr("ראש", "Head"),
                effect = tr("העצבים בגוף (אינסטינקט).", "Controls body nerves and instinctive reactions."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("מוח קטן", "Cerebellum"),
                bodyPart = tr("ראש", "Head"),
                effect = tr("שיווי משקל – פגיעה גורמת לאיבוד שיווי משקל.", "Responsible for balance – a strike may cause loss of balance."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("שבירת מפרקת", "Neck Break"),
                bodyPart = tr("ראש", "Head / Neck"),
                effect = tr("מוות מיידי.", "Immediate death."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("פגיעה בחוליות", "Vertebrae"),
                bodyPart = tr("חלק עליון", "Upper Body"),
                effect = tr("נזק למפרקת.", "Damage to the cervical spine."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("עמוד השדרה", "Spine"),
                bodyPart = tr("חלק עליון", "Upper Body"),
                effect = tr("בנוי מחוליות – קשה לגרום לשבר.", "Built from vertebrae – difficult to break."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("עצם הזנב", "Tailbone"),
                bodyPart = tr("חלק תחתון", "Lower Body"),
                effect = tr("האדם לא יכול לשבת (משם ומטה).", "Injury prevents sitting normally."),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("גיד אכילס", "Achilles Tendon"),
                bodyPart = tr("חלק תחתון", "Lower Body"),
                effect = tr("פגיעה ואי אפשר להזיז את העקב למעלה/למטה.", "Damage prevents moving the heel up or down."),
                isEnglish = isEnglish
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    isEnglish: Boolean
) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    isEnglish: Boolean
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
            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
        ) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = body,
                color = Color(0xFFE5E7EB),
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeakPointRow(
    place: String,
    bodyPart: String,
    effect: String,
    isEnglish: Boolean
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
            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
        ) {
            Text(
                text = place,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = bodyPart,
                color = Color(0xFFBFDBFE),
                fontWeight = FontWeight.SemiBold,
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = effect,
                color = Color(0xFFE5E7EB),
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
