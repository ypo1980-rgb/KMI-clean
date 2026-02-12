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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMethodScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "אודות השיטה",
                centerTitle = true,
                showMenu = false,
                onBack = null,
                showBottomActions = true,
                lockSearch = true,
                showRoleBadge = false,
                showCoachBroadcastFab = false
            )
        },
        containerColor = Color(0xFFF7F7FA),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    Text(
                        "אודות שיטת ק.מ.י - קרב מגן ישראלי",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(20.dp)
                    )
                    Divider()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            """
קרב מגן ישראלי - ק.מ.י שיטת לחימה ישראלית
מטרת ק.מ.י הגנה עצמית והתמודדות בקרב פנים אל פנים. השיטה פותחה מתוך קרב המגע ויוסדה ע"י אלי אביקזר בשנת 1989.

לאחר פטירת המייסד בשנת 2004 נבחר אבי אביסידון לראש השיטה וליו"ר עמותת ק.מ.י.

שיטת ק.מ.י. מבוססת על התנועות הטבעיות של גוף האדם ומצטיינת בפשטותה, במהירותה וביעילותה.

משמעות השם קרב מגן ישראלי:
"קרב מגן" - מטרת התרגילים והקרב להגן על החיים. "ישראלי" - המקצוע פותח בישראל ולאלי אביקזר היה חשוב שהשיטה שפיתח, תישא את שם מדינת ישראל בפי כל מתאמן, או מתעניין.
השם "קרב מגן ישראלי" מסמל אפוא את השיטה.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "האידיאולוגיה של ק.מ.י מאופיינת בכללים:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        AMBullet(text = "לא להיפגע.")
                        AMBullet(text = "פעל לפי יכולתך, אך פעל נכון.")
                        AMBullet(text = "השתמש בידע לפי הצורך.")
                        AMBullet(text = "הדרך הפשוטה — הקצרה ביותר והמהירה ביותר.")

                        Spacer(Modifier.height(12.dp))

                        Text(
                            """
שיטת ק.מ.י. מאושרת ע"י המכללת האקדמית בוינגייט. מורים לחינוך גופני משתלמים במכללת האקדמית בוינגייט בקורסים להגנה עצמית מטעם ק.מ.י. השיטה הומלצה לנעמ"ת ע"י משטרת ישראל להעברת סדנאות להגנה עצמית לנערות ולנשים. שיטת ק.מ.י. משמשת בזרועות הביטחון השונים ומבוקשת ברחבי העולם. שיטת ק.מ.י. זכתה להכרה בין לאומית.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "קרב מגן ישראלי - ק.מ.י., מתמקד בשני תחומים:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        AMBulletTitle(text = "הגנה עצמית הכוללת:")
                        AMBulletSub(text = "הדרכה בזיהוי מוקדי אלימות והתרחקות מהם; חינוך לריסון עצמי; תרגילים להגנה עצמית ולהימום התוקף; התאמה לבנות ובנים, נשים וגברים, קשישים ומוגבלים.")
                        AMBulletTitle(text = "קרב פנים אל פנים:")
                        AMBulletSub(text = "כאשר תרגיל ההגנה העצמית לא הושלם והתוקף ממשיך בתקיפה — עוברים לקרב קצר, ממוקד ויעיל עד ניטרול האיום.")

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "התרגילים הנלמדים בשיטת ק.מ.י:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        AMBullet(text = "הגנות נגד מכות שונות.")
                        AMBullet(text = "הגנות נגד בעיטות שונות; מכות ובעיטות להימום התוקף.")
                        AMBullet(text = "שחרורים מתפיסות ידיים/שיער/חולצה, מחביקות ומחניקות (גם בקרקע).")
                        AMBullet(text = "התמודדות והגנות מול תוקף חמוש — מקל, סכין, אקדח.")
                        AMBullet(text = "תרגילים נוספים וקרבות מול תוקף אחד או יותר.")
                        AMBullet(text = "סדנאות להגנה עצמית לנשים.")

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "עקרון הפשטות",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            """
התרגילים בק.מ.י פותחו על עקרון הפשטות: תנועה טבעית, פשוטה, הינה תנועה מהירה. תנועה מהירה מפיקה עוצמה. שימוש בתנועה בסיסית ולא מסורבלת מהווה מינימום תנועת הגנה נגד מקסימום תנועת התקפה ומאפשר לכל אדם יכולת הגנה עצמית והימום התוקף.

מכאן המשפט שטבע אלי אביקזר: "מינימום הגנה נגד מקסימום התקפה".
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "פילוסופיה מעשית ומתעדכנת",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            """
בניגוד לשיטות לחימה מסורתיות, ק.מ.י. מתאים עצמו למציאות המשתנה ברחוב ולסכנות המידיות. ק.מ.י. מחדש, משפר, מוסיף או גורע תרגילים ומציע פתרונות עדכניים. השלמות היא שאיפה — אך המטרה היא שמירת החיים, שליטה וריסון עצמי.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "דירוג חגורות",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "לבן, צהוב, כתום, ירוק, כחול, חום, שחור: דאן 1–2; פסים אדום־לבן: דאן 3–4; קטעים אדום־שחור: דאן 5; קטעים אדום־לבן: דאן 6–7; אדום: דאן 8–10.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "הסמכות והכשרות המדריכים ברשת נוקאאוט",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        AMBullet(text = "תואר ראשון/שני בחינוך גופני; תנועה לגיל הרך.")
                        AMBullet(text = "קורסי מדריכי ירי; אבטחת אישים; לחימה ופיקוד בזרועות הביטחון.")
                        AMBullet(text = "ידע בשיטות לחימה שונות; סדנאות ופעילות ייעודית לנשים.")
                        AMBullet(text = "מועדונים בארץ ובעולם; אימונים מגיל 4 ומעלה; התאמות למוגבלויות.")

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "השפעה חינוכית וחברתית",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "מתאמני ק.מ.י מציינים שיפור בתנועה ובקואורדינציה, עלייה בביטחון האישי והעצמי, אומץ לב, קור רוח, משמעת ושליטה עצמית, והתרחקות ממוקדי אלימות.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            // ❌ הכפתור העגול – אותו גודל בכל המסכים
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-24).dp, y = 20.dp)
                    .size(22.dp)
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

/* ───────────────── Bullets helpers ───────────────── */

@Composable
private fun AMBullet(text: String) {
    Text(
        text = "•  $text",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Right
    )
}

@Composable
private fun AMBulletSub(text: String) {
    Text(
        text = "–  $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 12.dp),
        textAlign = TextAlign.Right
    )
}

@Composable
private fun AMBulletTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Right
    )
}
