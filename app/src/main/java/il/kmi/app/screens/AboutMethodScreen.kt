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
import il.kmi.app.localization.rememberIsEnglish

private fun amText(he: String, en: String, isEnglish: Boolean): String =
    if (isEnglish) en else he

private fun amAlign(isEnglish: Boolean): TextAlign =
    if (isEnglish) TextAlign.Start else TextAlign.Right

private fun amHorizontal(isEnglish: Boolean): Alignment.Horizontal =
    if (isEnglish) Alignment.Start else Alignment.End

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMethodScreen(
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    val isEnglish = rememberIsEnglish()

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = amText("אודות השיטה", "About the Method", isEnglish),
                centerTitle = true,
                showMenu = false,
                onHome = onHome,
                showTopHome = false,
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
                        text = amText(
                            "אודות שיטת ק.מ.י - קרב מגן ישראלי",
                            "About the K.M.I Method - Israeli Protective Combat",
                            isEnglish
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        textAlign = amAlign(isEnglish)
                    )

                    HorizontalDivider()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = amHorizontal(isEnglish)
                    ) {
                        Text(
                            text = amText(
                                """
קרב מגן ישראלי - ק.מ.י היא שיטת לחימה ישראלית.
מטרת ק.מ.י היא הגנה עצמית והתמודדות בקרב פנים אל פנים. השיטה פותחה מתוך קרב המגע ויוסדה על ידי אלי אביקזר בשנת 1989.

לאחר פטירת המייסד בשנת 2004 נבחר אבי אביסידון לראש השיטה וליו"ר עמותת ק.מ.י.

שיטת ק.מ.י מבוססת על התנועות הטבעיות של גוף האדם ומצטיינת בפשטותה, במהירותה וביעילותה.

משמעות השם קרב מגן ישראלי:
"קרב מגן" - מטרת התרגילים והקרב היא להגן על החיים.
"ישראלי" - המקצוע פותח בישראל, ולאלי אביקזר היה חשוב שהשיטה שפיתח תישא את שם מדינת ישראל בפי כל מתאמן או מתעניין.
השם "קרב מגן ישראלי" מסמל אפוא את השיטה.
                                """.trimIndent(),
                                """
Israeli Protective Combat - K.M.I is an Israeli self-defense method.
The goal of K.M.I is self-defense and effective response in close-range combat. The method evolved from Krav Maga and was founded by Eli Avikzar in 1989.

After the founder passed away in 2004, Avi Avisidon was appointed head of the method and chairman of the K.M.I association.

K.M.I is based on the natural movements of the human body and stands out for its simplicity, speed, and effectiveness.

Meaning of the name "Israeli Protective Combat":
"Protective Combat" - the purpose of the drills and combat is to protect life.
"Israeli" - the discipline was developed in Israel, and Eli Avikzar wanted the method he created to carry the name of the State of Israel in the mouth of every trainee or interested learner.
Thus, the name symbolizes the method itself.
                                """.trimIndent(),
                                isEnglish
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = amText(
                                "האידיאולוגיה של ק.מ.י מאופיינת בכללים:",
                                "The ideology of K.M.I is characterized by these principles:",
                                isEnglish
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))
                        AMBullet(
                            text = amText("לא להיפגע.", "Do not get hurt.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("פעל לפי יכולתך, אך פעל נכון.", "Act according to your ability, but act correctly.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("השתמש בידע לפי הצורך.", "Use your knowledge when needed.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("הדרך הפשוטה — הקצרה ביותר והמהירה ביותר.", "Choose the simplest path — the shortest and fastest one.", isEnglish),
                            isEnglish = isEnglish
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = amText(
                                """
שיטת ק.מ.י מאושרת על ידי המכללה האקדמית בוינגייט. מורים לחינוך גופני משתלמים במכללה האקדמית בוינגייט בקורסים להגנה עצמית מטעם ק.מ.י. השיטה הומלצה לנעמ"ת על ידי משטרת ישראל להעברת סדנאות להגנה עצמית לנערות ולנשים. שיטת ק.מ.י משמשת בזרועות הביטחון השונים ומבוקשת ברחבי העולם. שיטת ק.מ.י זכתה להכרה בין־לאומית.
                                """.trimIndent(),
                                """
The K.M.I method is approved by the Wingate Academic College. Physical education teachers receive training there in self-defense courses conducted by K.M.I. The method was recommended to Na'amat by the Israel Police for self-defense workshops for girls and women. K.M.I is used in various security branches and is sought after around the world. The method has received international recognition.
                                """.trimIndent(),
                                isEnglish
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = amText(
                                "קרב מגן ישראלי - ק.מ.י, מתמקד בשני תחומים:",
                                "Israeli Protective Combat - K.M.I focuses on two main areas:",
                                isEnglish
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        AMBulletTitle(
                            text = amText("הגנה עצמית הכוללת:", "Self-defense, including:", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBulletSub(
                            text = amText(
                                "הדרכה בזיהוי מוקדי אלימות והתרחקות מהם; חינוך לריסון עצמי; תרגילים להגנה עצמית ולהימום התוקף; התאמה לבנות ובנים, נשים וגברים, קשישים ומוגבלים.",
                                "Training in identifying violent situations and avoiding them; education for self-restraint; self-defense drills and attacker neutralization; suitable for girls and boys, women and men, seniors, and people with disabilities.",
                                isEnglish
                            ),
                            isEnglish = isEnglish
                        )

                        AMBulletTitle(
                            text = amText("קרב פנים אל פנים:", "Face-to-face combat:", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBulletSub(
                            text = amText(
                                "כאשר תרגיל ההגנה העצמית לא הושלם והתוקף ממשיך בתקיפה — עוברים לקרב קצר, ממוקד ויעיל עד ניטרול האיום.",
                                "When a self-defense technique does not fully stop the attacker and the assault continues, the trainee moves into short, focused, and effective combat until the threat is neutralized.",
                                isEnglish
                            ),
                            isEnglish = isEnglish
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = amText(
                                "התרגילים הנלמדים בשיטת ק.מ.י:",
                                "The techniques taught in the K.M.I method:",
                                isEnglish
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))
                        AMBullet(
                            text = amText("הגנות נגד מכות שונות.", "Defenses against various strikes.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("הגנות נגד בעיטות שונות; מכות ובעיטות להימום התוקף.", "Defenses against various kicks; strikes and kicks to neutralize the attacker.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("שחרורים מתפיסות ידיים/שיער/חולצה, מחביקות ומחניקות (גם בקרקע).", "Releases from hand, hair, and shirt grabs, from hugs and chokes, including on the ground.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("התמודדות והגנות מול תוקף חמוש — מקל, סכין, אקדח.", "Response and defenses against an armed attacker — stick, knife, or gun.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("תרגילים נוספים וקרבות מול תוקף אחד או יותר.", "Additional techniques and combat against one or more attackers.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("סדנאות להגנה עצמית לנשים.", "Self-defense workshops for women.", isEnglish),
                            isEnglish = isEnglish
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = amText("עקרון הפשטות", "The Principle of Simplicity", isEnglish),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = amText(
                                """
התרגילים בק.מ.י פותחו על עקרון הפשטות: תנועה טבעית ופשוטה היא תנועה מהירה. תנועה מהירה מפיקה עוצמה. שימוש בתנועה בסיסית ולא מסורבלת יוצר מינימום תנועת הגנה נגד מקסימום תנועת התקפה ומאפשר לכל אדם יכולת הגנה עצמית והימום התוקף.

מכאן המשפט שטבע אלי אביקזר: "מינימום הגנה נגד מקסימום התקפה".
                                """.trimIndent(),
                                """
K.M.I techniques were developed according to the principle of simplicity: a natural and simple movement is a fast movement. Fast movement generates power. Using a basic, non-complicated motion creates minimum defensive movement against maximum attacking movement and enables every person to defend themselves and neutralize an attacker.

From this came Eli Avikzar's phrase: "Minimum defense against maximum attack."
                                """.trimIndent(),
                                isEnglish
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = amText("פילוסופיה מעשית ומתעדכנת", "A Practical and Evolving Philosophy", isEnglish),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = amText(
                                """
בניגוד לשיטות לחימה מסורתיות, ק.מ.י מתאים את עצמו למציאות המשתנה ברחוב ולסכנות המידיות. ק.מ.י מחדש, משפר, מוסיף או גורע תרגילים ומציע פתרונות עדכניים. השלמות היא שאיפה — אך המטרה היא שמירת החיים, שליטה וריסון עצמי.
                                """.trimIndent(),
                                """
Unlike traditional martial arts, K.M.I adapts itself to the changing reality of the street and to immediate threats. K.M.I renews, improves, adds, or removes techniques and offers updated solutions. Perfection is an aspiration — but the true goal is preserving life, control, and self-restraint.
                                """.trimIndent(),
                                isEnglish
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = amText("דירוג חגורות", "Belt Ranking", isEnglish),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = amText(
                                "לבן, צהוב, כתום, ירוק, כחול, חום, שחור: דאן 1–2; פסים אדום־לבן: דאן 3–4; קטעים אדום־שחור: דאן 5; קטעים אדום־לבן: דאן 6–7; אדום: דאן 8–10.",
                                "White, Yellow, Orange, Green, Blue, Brown, Black: Dan 1–2; red-white stripes: Dan 3–4; red-black sections: Dan 5; red-white sections: Dan 6–7; red: Dan 8–10.",
                                isEnglish
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = amText(
                                "הסמכות והכשרות המדריכים ברשת נוקאאוט",
                                "Instructor Qualifications in the Knockout Network",
                                isEnglish
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        AMBullet(
                            text = amText("תואר ראשון/שני בחינוך גופני; תנועה לגיל הרך.", "Bachelor's/Master's degree in physical education; movement training for early childhood.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("קורסי מדריכי ירי; אבטחת אישים; לחימה ופיקוד בזרועות הביטחון.", "Firearms instruction courses; personal security; combat and command experience in security branches.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("ידע בשיטות לחימה שונות; סדנאות ופעילות ייעודית לנשים.", "Knowledge of various fighting systems; workshops and dedicated activity for women.", isEnglish),
                            isEnglish = isEnglish
                        )
                        AMBullet(
                            text = amText("מועדונים בארץ ובעולם; אימונים מגיל 4 ומעלה; התאמות למוגבלויות.", "Clubs in Israel and abroad; training from age 4 and up; adaptations for disabilities.", isEnglish),
                            isEnglish = isEnglish
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = amText("השפעה חינוכית וחברתית", "Educational and Social Impact", isEnglish),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = amText(
                                "מתאמני ק.מ.י מציינים שיפור בתנועה ובקואורדינציה, עלייה בביטחון האישי והעצמי, אומץ לב, קור רוח, משמעת ושליטה עצמית, והתרחקות ממוקדי אלימות.",
                                "K.M.I trainees report improvement in movement and coordination, increased personal confidence and self-confidence, courage, composure, discipline, self-control, and distancing themselves from violent environments.",
                                isEnglish
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

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
                    contentDescription = amText("סגור", "Close", isEnglish),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/* ───────────────── Bullets helpers ───────────────── */

@Composable
private fun AMBullet(
    text: String,
    isEnglish: Boolean
) {
    Text(
        text = if (isEnglish) "• $text" else "•  $text",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth(),
        textAlign = amAlign(isEnglish)
    )
}

@Composable
private fun AMBulletSub(
    text: String,
    isEnglish: Boolean
) {
    Text(
        text = if (isEnglish) "– $text" else "–  $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isEnglish) 12.dp else 0.dp,
                end = if (isEnglish) 0.dp else 12.dp
            ),
        textAlign = amAlign(isEnglish)
    )
}

@Composable
private fun AMBulletTitle(
    text: String,
    isEnglish: Boolean
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = amAlign(isEnglish)
    )
}
