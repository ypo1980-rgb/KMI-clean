package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography

//================================================================

private fun weakTr(
    isEnglish: Boolean,
    he: String,
    en: String
): String = if (isEnglish) en else he

private fun weakTextAlign(isEnglish: Boolean): TextAlign =
    if (isEnglish) TextAlign.Start else TextAlign.Right

private fun weakHorizontalAlignment(isEnglish: Boolean): Alignment.Horizontal =
    if (isEnglish) Alignment.Start else Alignment.End

private fun weakRowArrangement(isEnglish: Boolean): Arrangement.Horizontal =
    if (isEnglish) Arrangement.Start else Arrangement.End

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeakPointsScreen(
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    fun tr(he: String, en: String): String =
        weakTr(isEnglish, he, en)

    val backgroundBrush =
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.background
            )
        )

    Scaffold(
        topBar = {
            KmiTopBar(
                title = tr("נקודות תורפה", "Weak Points"),
                onHome = onOpenHome,
                onSearch = onOpenSearch,
                onSettings = onOpenSettings,
                showBottomActions = true,
                showTopHome = false,
                showRoleStatus = false,
                showSettings = true,
                centerTitle = true,
                alignTitleEnd = false,

                showTopShare = true,
                onShare = {
                    shareWeakPointsPdf(
                        context = ctx,
                        isEnglish = isEnglish
                    )
                },
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
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ⚠️ אזהרה
            SafetyWarningCard(
                isEnglish = isEnglish,
                text = tr(
                    "לפגיעה בנקודות התורפה יש פוטנציאל נזק גבוה. אין לבצע אלא במצב חירום.\n" +
                            "חל איסור מוחלט לתרגל ללא פיקוח מאמן מוסמך וציוד בטיחות מתאים.\n" +
                            "תרגול שגוי עלול להסתיים בפציעה ואף במוות.",
                    "Striking weak points has a high potential for severe harm. Do not use except in a real emergency.\n" +
                            "Training without the supervision of a certified coach and proper safety equipment is strictly forbidden.\n" +
                            "Incorrect practice may result in serious injury or even death."
                )
            )

            // כותרת קטנה
            SectionTitle(
                text = tr("מפרקים / אצבעות (כללי)", "Joints / Fingers (General)"),
                isEnglish = isEnglish
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
                effect = tr(
                    "נקודת אחיזה – ניתן להוציא משיווי משקל.",
                    "A gripping point that can be used to break balance."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("מצח", "Forehead"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "אזור קשה – הפגיעה פחות אפקטיבית יחסית.",
                    "A hard area - generally less effective to strike."
                ),
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
                effect = tr(
                    "פגיעה בעין גורמת לנזק חמור/עיוורון אפשרי.",
                    "A strike to the eye may cause severe injury or possible blindness."
                ),
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
                place = tr("צואר", "Neck"),
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
                effect = tr(
                    "העצבים בגוף (אינסטינקט).",
                    "Controls body nerves and instinctive reactions."
                ),
                isEnglish = isEnglish
            )
            WeakPointRow(
                place = tr("מוח קטן", "Cerebellum"),
                bodyPart = tr("ראש", "Head"),
                effect = tr(
                    "שיווי משקל – פגיעה גורמת לאיבוד שיווי משקל.",
                    "Responsible for balance – a strike may cause loss of balance."
                ),
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
                effect = tr(
                    "בנוי מחוליות – קשה לגרום לשבר.",
                    "Built from vertebrae – difficult to break."
                ),
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
                effect = tr(
                    "פגיעה ואי אפשר להזיז את העקב למעלה/למטה.",
                    "Damage prevents moving the heel up or down."
                ),
                isEnglish = isEnglish
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class WeakPointPdfItem(
    val section: String,
    val place: String,
    val bodyPart: String,
    val effect: String
)

private fun shareWeakPointsPdf(
    context: Context,
    isEnglish: Boolean
) {
    val pdfFile = createWeakPointsPdf(
        context = context,
        isEnglish = isEnglish
    )

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(
            Intent.EXTRA_SUBJECT,
            if (isEnglish) "KAMI Weak Points" else "נקודות תורפה - KAMI"
        )
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            sendIntent,
            if (isEnglish) "Share PDF" else "שיתוף PDF"
        )
    )
}

private fun createWeakPointsPdf(
    context: Context,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val items = listOf(
        WeakPointPdfItem(tr("מפרקים / אצבעות", "Joints / Fingers"), tr("מפרקים", "Joints"), tr("כללי", "General"), tr("כל תנועה כנגד כיוון לתנועה הטבעית – שבר.", "Any movement against the natural direction may cause a fracture.")),
        WeakPointPdfItem(tr("מפרקים / אצבעות", "Joints / Fingers"), tr("שבר באצבע", "Finger Fracture"), tr("כללי", "General"), tr("אדם מתעלף במקום.", "A severe finger break may cause collapse from pain.")),

        WeakPointPdfItem(tr("חזית", "Front"), tr("שיער", "Hair"), tr("ראש", "Head"), tr("נקודת אחיזה – ניתן להוציא משיווי משקל.", "A gripping point that can be used to break balance.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("מצח", "Forehead"), tr("ראש", "Head"), tr("אזור קשה – הפגיעה פחות אפקטיבית יחסית.", "A hard area - generally less effective to strike.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("גבה", "Eyebrow"), tr("ראש", "Head"), tr("נקודה רגישה – דימום יכול לרדת לעיניים ולפגוע בראייה.", "A sensitive point – bleeding may affect vision.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("עין", "Eye"), tr("ראש", "Head"), tr("פגיעה בעין גורמת לנזק חמור/עיוורון אפשרי.", "A strike to the eye may cause severe injury or blindness.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("גשר האף / שורש האף", "Nasal Bridge / Nose Root"), tr("ראש", "Head"), tr("פגיעה באף יכולה לגרום לדמעות/דימום ועד שבר עצם האף וזעזוע מוח.", "A strike to the nose may cause tearing, bleeding, fracture or concussion.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("שורש האף / שפה תחתונה מתחת לאף", "Nose Root / Upper Lip"), tr("ראש", "Head"), tr("נקודה להוצאה משיווי משקל ע״י הרמת שורש האף.", "A control point that can break balance.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("לסת עליונה", "Upper Jaw"), tr("ראש", "Head"), tr("ניתן לשבור שיניים בקלות יחסית ע״י מכה.", "Teeth may break relatively easily.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("שפתיים", "Lips"), tr("ראש", "Head"), tr("השפה עלולה להיפצע ע״י השיניים.", "The lips may be cut by the teeth.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("לסת פתוחה", "Open Jaw"), tr("ראש", "Head"), tr("קל יותר לשבור ע״י מכה.", "Easier to break when open.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("לסת סגורה", "Closed Jaw"), tr("ראש", "Head"), tr("זעזוע – קשה יותר לשבור.", "May cause shock; harder to break.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("גרוגרת", "Throat"), tr("ראש", "Head"), tr("לחיצה/מכה קדימה – סכנת חיים, דורש טיפול רפואי מיידי.", "Life-threatening and requires immediate medical care.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("שקע הגרוגרת", "Throat Hollow"), tr("ראש", "Head"), tr("דימום קל/כאב למספר שניות.", "May cause brief pain or minor bleeding.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("עצם הבריח", "Clavicle"), tr("חלק עליון", "Upper Body"), tr("שבר יכול לשתק את הצד ולמנוע תנועת יד בצורה תקינה.", "A fracture can disable normal arm movement.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("בית החזה", "Chest"), tr("פנימי", "Internal"), tr("שבר בצלעות יכול לגרום לקרע בריאה.", "Broken ribs may puncture the lung.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("כבד", "Liver"), tr("פנימי", "Internal"), tr("שבר בצלעות יכול לגרום לקרע בכבד.", "Broken ribs may cause liver rupture.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("מפתח הלב", "Solar Plexus"), tr("פנימי", "Internal"), tr("פגיעה קשה מאוד – סכנת חיים.", "A very dangerous strike – potentially life-threatening.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("כליות", "Kidneys"), tr("פנימי", "Internal"), tr("פגיעה בכליה – נזק משמעותי אפשרי.", "A kidney strike may cause serious damage.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("בטן", "Abdomen"), tr("פנימי", "Internal"), tr("פגיעה יכולה לגרום לשטף דם פנימי.", "A strike may cause internal bleeding.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("אשכים", "Groin"), tr("חלק תחתון", "Lower Body"), tr("נקודה חלשה מאוד – תגובת כאב חריפה.", "A very vulnerable point causing intense pain.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("פיקה (ברך)", "Kneecap"), tr("חלק תחתון", "Lower Body"), tr("ניתן לרסק/לגרום לנזק – נכות אפשרית.", "Can be damaged – possible disability.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("שוק הרגל", "Shin"), tr("חלק תחתון", "Lower Body"), tr("עצם חשופה יחסית – כאב משמעותי מפגיעה.", "Exposed bone – significant pain from impact.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("גב כף הרגל", "Top of the Foot"), tr("חלק תחתון", "Lower Body"), tr("מבנה עדין – בדריכה הנזק יכול להיות גדול.", "Delicate structure – stepping may cause serious damage.")),
        WeakPointPdfItem(tr("חזית", "Front"), tr("שרירים", "Muscles"), tr("כללי", "General"), tr("פגיעה בשריר/כלי דם גורמת כאב ופגיעה בתפקוד.", "Muscle or vessel damage causes pain and reduced function.")),

        WeakPointPdfItem(tr("צד", "Side"), tr("פגיעה ברקה", "Temple"), tr("ראש", "Head"), tr("פגיעה מסוכנת מאוד.", "A highly dangerous impact point.")),
        WeakPointPdfItem(tr("צד", "Side"), tr("אוזן", "Ear"), tr("ראש", "Head"), tr("קריעת עור התוף – דימום.", "A ruptured eardrum may cause bleeding.")),
        WeakPointPdfItem(tr("צד", "Side"), tr("צואר", "Neck"), tr("ראש", "Head"), tr("פגיעה בכלי דם: עילפון; חניקה ממושכת – סכנת חיים.", "Blood-vessel strike may cause collapse; prolonged choking is life-threatening.")),
        WeakPointPdfItem(tr("צד", "Side"), tr("כתף", "Shoulder"), tr("חלק עליון", "Upper Body"), tr("ניתן להוציא מהמקום ע״י הוצאת העצם מהשקע.", "Can be dislocated from the socket.")),
        WeakPointPdfItem(tr("צד", "Side"), tr("בית השחי", "Armpit"), tr("חלק עליון", "Upper Body"), tr("שריר רגיש מאוד – פגיעה כואבת מאוד.", "Very sensitive muscle area.")),
        WeakPointPdfItem(tr("צד", "Side"), tr("צלעות", "Ribs"), tr("חלק עליון", "Upper Body"), tr("פגיעה בעצב גורמת כאב חזק.", "Nerve impact causes strong pain.")),
        WeakPointPdfItem(tr("צד", "Side"), tr("שבירת צלעות", "Broken Ribs"), tr("חלק עליון", "Upper Body"), tr("גרימת קרע בריאה – אפילו מוות.", "May cause lung rupture – potentially fatal.")),
        WeakPointPdfItem(tr("צד", "Side"), tr("שקע הברך מהצד", "Side of Knee Joint"), tr("חלק תחתון", "Lower Body"), tr("קל לגרום לקרע ברצועות.", "Easy to damage ligaments.")),
        WeakPointPdfItem(tr("צד", "Side"), tr("קרסול", "Ankle"), tr("חלק תחתון", "Lower Body"), tr("פגיעה נכונה יכולה לפגוע בקרסול ולהקשות על הליכה.", "Can damage the ankle and make walking difficult.")),

        WeakPointPdfItem(tr("מאחור", "Back"), tr("מוח גדול", "Cerebrum"), tr("ראש", "Head"), tr("העצבים בגוף (אינסטינקט).", "Controls body nerves and instinctive reactions.")),
        WeakPointPdfItem(tr("מאחור", "Back"), tr("מוח קטן", "Cerebellum"), tr("ראש", "Head"), tr("שיווי משקל – פגיעה גורמת לאיבוד שיווי משקל.", "Responsible for balance – impact may cause loss of balance.")),
        WeakPointPdfItem(tr("מאחור", "Back"), tr("שבירת מפרקת", "Neck Break"), tr("ראש / צוואר", "Head / Neck"), tr("מוות מיידי.", "Immediate death.")),
        WeakPointPdfItem(tr("מאחור", "Back"), tr("פגיעה בחוליות", "Vertebrae"), tr("חלק עליון", "Upper Body"), tr("נזק למפרקת.", "Damage to the cervical spine.")),
        WeakPointPdfItem(tr("מאחור", "Back"), tr("עמוד השדרה", "Spine"), tr("חלק עליון", "Upper Body"), tr("בנוי מחוליות – קשה לגרום לשבר.", "Built from vertebrae – difficult to break.")),
        WeakPointPdfItem(tr("מאחור", "Back"), tr("עצם הזנב", "Tailbone"), tr("חלק תחתון", "Lower Body"), tr("האדם לא יכול לשבת בצורה תקינה.", "Injury prevents normal sitting.")),
        WeakPointPdfItem(tr("מאחור", "Back"), tr("גיד אכילס", "Achilles Tendon"), tr("חלק תחתון", "Lower Body"), tr("פגיעה ואי אפשר להזיז את העקב למעלה/למטה.", "Damage prevents moving the heel up or down."))
    )

    val document = PdfDocument()

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val blue = android.graphics.Color.rgb(12, 78, 130)
    val lightBlue = android.graphics.Color.rgb(234, 246, 255)
    val softBlue = android.graphics.Color.rgb(244, 250, 255)
    val borderBlue = android.graphics.Color.rgb(191, 213, 232)
    val textDark = android.graphics.Color.rgb(15, 23, 42)
    val textMuted = android.graphics.Color.rgb(80, 100, 120)
    val warningBg = android.graphics.Color.rgb(255, 243, 224)
    val warningBorder = android.graphics.Color.rgb(255, 183, 77)

    val regular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun paint(size: Float, color: Int = textDark, typeface: Typeface = regular, align: Paint.Align = Paint.Align.RIGHT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            this.typeface = typeface
            textAlign = align
        }

    val titlePaint = paint(29f, android.graphics.Color.WHITE, bold)
    val subTitlePaint = paint(14f, android.graphics.Color.WHITE)
    val sectionPaint = paint(17f, blue, bold)
    val labelPaint = paint(10.5f, blue, bold)
    val valuePaint = paint(12.5f)
    val boldValuePaint = paint(13f, textDark, bold)
    val smallPaint = paint(9f, textMuted)

    fun drawLogo(canvas: android.graphics.Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy })
        canvas.drawCircle(cx, cy, r - 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
        canvas.drawText("KAMI", cx, cy + r * 0.22f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            typeface = bold
            textSize = r * 0.62f
            textAlign = Paint.Align.CENTER
        })
    }

    fun drawRound(canvas: android.graphics.Canvas, l: Float, t: Float, r: Float, b: Float, c: Int, stroke: Boolean = false) {
        canvas.drawRoundRect(l, t, r, b, 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = c
            style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
            strokeWidth = 1.2f
        })
    }

    fun drawHeader(canvas: android.graphics.Canvas) {
        canvas.drawColor(android.graphics.Color.WHITE)

        canvas.drawPath(android.graphics.Path().apply {
            moveTo(pageWidth.toFloat(), 0f)
            lineTo(pageWidth.toFloat(), 122f)
            lineTo(178f, 122f)
            lineTo(238f, 0f)
            close()
        }, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy })

        canvas.drawPath(android.graphics.Path().apply {
            moveTo(208f, 122f)
            lineTo(224f, 122f)
            lineTo(284f, 0f)
            lineTo(268f, 0f)
            close()
        }, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(36, 103, 158) })

        canvas.drawPath(android.graphics.Path().apply {
            moveTo(230f, 122f)
            lineTo(238f, 122f)
            lineTo(298f, 0f)
            lineTo(290f, 0f)
            close()
        }, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(128, 183, 220) })

        drawLogo(canvas, 78f, 58f, 42f)

        canvas.drawText(tr("נקודות תורפה", "Weak Points"), pageWidth - 34f, 52f, titlePaint)
        canvas.drawText(tr("כרטיס מידע מקצועי למתאמן", "Professional trainee reference card"), pageWidth - 34f, 78f, subTitlePaint)

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            tr("תאריך הפקה:", "Generated:") + " " +
                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
            pageWidth - 34f,
            142f,
            smallPaint
        )
    }

    fun drawFooter(canvas: android.graphics.Canvas, pageNumber: Int, totalPages: Int) {
        val footerY = 804f
        canvas.drawLine(0f, footerY, pageWidth.toFloat(), footerY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            strokeWidth = 2f
        })

        drawLogo(canvas, 38f, footerY + 22f, 13f)

        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Together We Protect", 62f, footerY + 25f, smallPaint)

        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            tr("עמוד $pageNumber מתוך $totalPages", "Page $pageNumber of $totalPages"),
            pageWidth / 2f,
            footerY + 25f,
            smallPaint
        )

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Krav Maga Israel", pageWidth - 66f, footerY + 18f, smallPaint)
        canvas.drawText("www.kmi.org.il", pageWidth - 66f, footerY + 31f, smallPaint)
    }

    val firstPageCapacity = 5
    val nextPageCapacity = 7
    val totalPages =
        1 + kotlin.math.ceil(
            (items.size - firstPageCapacity) / nextPageCapacity.toDouble()
        ).toInt()

    var pageNumber = 1
    var itemIndex = 0

    while (itemIndex < items.size) {
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        val canvas = page.canvas

        drawHeader(canvas)

        var y = 136f

        if (pageNumber == 1) {
            drawRound(canvas, margin, y, pageWidth - margin, y + 82f, warningBg)
            drawRound(canvas, margin, y, pageWidth - margin, y + 82f, warningBorder, stroke = true)

            sectionPaint.color = android.graphics.Color.rgb(194, 65, 12)
            canvas.drawText(tr("אזהרת בטיחות", "Safety Warning"), pageWidth - margin - 22f, y + 30f, sectionPaint)

            valuePaint.color = android.graphics.Color.rgb(78, 52, 46)
            canvas.drawText(
                tr("אין לתרגל ללא פיקוח מאמן מוסמך וציוד בטיחות מתאים.", "Do not practice without certified supervision and proper safety equipment."),
                pageWidth - margin - 22f,
                y + 56f,
                valuePaint
            )

            sectionPaint.color = blue
            valuePaint.color = textDark
            y += 106f
        }

        sectionPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(tr("פירוט נקודות מרכזיות", "Key Points Summary"), pageWidth / 2f, y, sectionPaint)
        y += 24f

        val capacity = if (pageNumber == 1) firstPageCapacity else nextPageCapacity
        repeat(capacity) {
            if (itemIndex >= items.size) return@repeat

            val item = items[itemIndex]
            val bottom = y + 82f
            val right = pageWidth - margin
            val mid = pageWidth / 2f

            drawRound(
                canvas,
                margin,
                y,
                right,
                bottom,
                if (itemIndex % 2 == 0) lightBlue else softBlue
            )
            drawRound(
                canvas,
                margin,
                y,
                right,
                bottom,
                borderBlue,
                stroke = true
            )

            canvas.drawLine(mid, y + 22f, mid, bottom - 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderBlue
                strokeWidth = 1f
            })

            sectionPaint.textAlign = Paint.Align.RIGHT
            sectionPaint.textSize = 13f
            canvas.drawText(item.section.take(28), right - 22f, y + 26f, sectionPaint)
            sectionPaint.textSize = 17f

            boldValuePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(item.place.take(28), right - 22f, y + 48f, boldValuePaint)

            labelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(item.bodyPart.take(24), right - 22f, y + 68f, labelPaint)

            valuePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(item.effect.take(42), mid - 22f, y + 40f, valuePaint)

            y = bottom + 8f
            itemIndex++
        }

        drawFooter(canvas, pageNumber, totalPages)

        document.finishPage(page)
        pageNumber++
    }

    val dir =
        File(
            context.cacheDir,
            "shared_pdfs"
        ).apply {
            mkdirs()
        }

    /*
     * שם קבוע לפי שפת האפליקציה.
     *
     * כל יצירה חדשה מחליפה את הקובץ הקודם
     * במקום ליצור עותק נוסף עם timestamp.
     */
    val fileName =
        if (isEnglish) {
            "Weak Points.pdf"
        } else {
            "נקודות תורפה.pdf"
        }

    val file =
        File(
            dir,
            fileName
        )

    FileOutputStream(
        file,
        false
    ).use { output ->
        document.writeTo(output)
    }

    document.close()

    return file
}

@Composable
private fun SectionTitle(
    text: String,
    isEnglish: Boolean
) {
    Text(
        text = text,
        style =
            KmiTypography.sectionTitle.copy(
                fontWeight = FontWeight.ExtraBold
            ),
        textAlign = weakTextAlign(isEnglish),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 2.dp,
                vertical = 8.dp
            )
    )
}

@Composable
private fun SafetyWarningCard(
    isEnglish: Boolean,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = weakRowArrangement(isEnglish)
        ) {
            if (isEnglish) {
                Icon(
                    imageVector = Icons.Filled.Report,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(KmiIconSize.medium)
                )

                Spacer(Modifier.width(10.dp))
            }

            Text(
                text = text,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = KmiTypography.body,
                textAlign = weakTextAlign(isEnglish),
                modifier = Modifier.weight(1f)
            )

            if (!isEnglish) {
                Spacer(Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.Filled.Report,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(KmiIconSize.medium)
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    isEnglish: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = weakHorizontalAlignment(isEnglish)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = KmiTypography.body.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = weakTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = KmiTypography.body.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = weakTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = weakHorizontalAlignment(isEnglish)
            ) {
                Text(
                    text = place,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = KmiTypography.body.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = weakTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = bodyPart,
                    color = MaterialTheme.colorScheme.primary,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = weakTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = effect,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = KmiTypography.body.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = weakTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}