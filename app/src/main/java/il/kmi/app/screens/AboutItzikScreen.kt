package il.kmi.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutItzikScreen(
    onBack: () -> Unit
) {
    val pageBg = Color(0xFFF7F7FA)

    Scaffold(
        topBar = {
            // בלי X כאן – רק כותרת וסרגל תחתון
            il.kmi.app.ui.KmiTopBar(
                title = "אודות איציק ביטון",
                centerTitle = true,
                showMenu = false,
                onBack = null,
                showRoleBadge = false,
                showBottomActions = true,
                lockSearch = true,
                lockHome = false
            )
        },
        containerColor = pageBg,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->

        // כדי למקם את העיגול מעל הכרטיס נעטוף ב-Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                // כותרת קבועה; רק הגוף גולל
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "איציק ביטון – מייסד נוקאאוט, דאן 5",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    Divider()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            """
איציק ביטון הינו מאמן קרב מגן ישראלי ומאמן אומנויות לחימה בנתניה, מוסמך מטעם המכללה האקדמית בוינגייט. עוסק בתחום מאז 1997.

את דרכו החל אצל המאמן רפי אלגריסי, מתלמידיו הבכירים של אימי ליכטנפלד. אצל רפי התמחה בקרב מגע וכמקצוע משני באייקידו.

ב־2004 הצטרף לעמותת ק.מ.י; ב־2005 השלים קורס מדריכים בוינגייט וקיבל חגורה שחורה מאבי אביסידון ורפי אלגריסי. ב־2006 פתח את סניף נוקאאוט הראשון בנתניה.

לאורך השנים הדריך מסגרות ביטחוניות שונות (שב״ס, המכללה לפיקוד טקטי בצה״ל, מוסדות לבריאות הנפש) ומכשיר מאבטחים. לוקח חלק קבוע בקורסי מדריכים בוינגייט, ומאז 2009 גם מאמן אומנויות לחימה מוסמך.

ב־2005 החל להתאמן בנינג׳יטסו אצל שיהאן משה קסטיאל (דאן 10) ולמד רפואה משלימה – אוסטיאופתיה. כיום מנהל את רשת נוקאאוט עם מאות מתאמנים, עשרות חגורות שחורות ומדריכים מוסמכים.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "תחומי עשייה נוספים",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        ItzBullet("עבודה חינוכית עם נוער, ילדים בסיכון ואוכלוסיות מיוחדות.")
                        ItzBullet("בניית תכניות מותאמות לארגונים, לבתי־ספר ולחברות.")
                        ItzBullet("רקע נוסף: BJJ, איגרוף ו־MMA.")

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "מאמנים שהשפיעו עליו",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        ItzBullet("רן סודאי, ארז שרעבי, ג׳ון אסקודרו, רון רותם.")

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "סניפי רשת נוקאאוט (מבחר)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        ItzBullet("מרכז קהילתי אופק, שיכון מזרחי, נורדאו, נאות שקד, קריית השרון (נתניה).")
                        ItzBullet("כפר יעבץ (עזריאל), בני ברק, פתח תקווה, הרצליה – נווה עמל.")
                        ItzBullet("סניף צופים, סוקולוב ואחרים.")

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            // ❌ הכפתור המעוגל הקטן בפינה של הכרטיס
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-24).dp, y = 20.dp)
                    .size(22.dp) // עיגול קטן, מרווח מינימלי מהאיקס
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "סגור",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/* ───────── עזרים ───────── */

@Composable
private fun ItzBullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text("•", modifier = Modifier.padding(end = 8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
