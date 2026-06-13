package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
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
                showMenu = true,
                onHome = onHome,
                showTopHome = false,
                onBack = null,
                showBottomActions = true,
                lockSearch = true,
                showRoleBadge = true,
                showModePill = true,
                showCoachBroadcastFab = false
            )
                 },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFEAF4FF),
                            Color(0xFFB7DDF7),
                            Color(0xFF1F78B4),
                            Color(0xFF062B4A)
                        )
                    )
                )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFEAF2FF),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFD8E3F5)
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    Text(
                        text = amText(
                            "אודות שיטת ק.מ.י - קרב מגן ישראלי",
                            "About the K.A.M.I Method - Israeli Protective Combat",
                            isEnglish
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        textAlign = amAlign(isEnglish)
                    )

                    HorizontalDivider(
                        color = Color(0xFFBFD0E8),
                        thickness = 1.dp
                    )

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
Israeli Protective Combat - K.A.M.I is an Israeli self-defense method.
The goal of K.A.M.I is self-defense and effective response in close-range combat. The method evolved from Krav Maga and was founded by Eli Avikzar in 1989.

After the founder passed away in 2004, Avi Avisidon was appointed head of the method and chairman of the K.A.M.I association.

K.A.M.I is based on the natural movements of the human body and stands out for its simplicity, speed, and effectiveness.

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

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = amText(
                                "ההבדל בין ק.מ.י לקרב מגע",
                                "The Difference Between K.A.M.I and Krav Maga",
                                isEnglish
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = amAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = amText(
                                """
ק.מ.י – קרב מגן ישראלי היא שיטה שצמחה מתוך משפחת "קרב מגע" ומהווה חלק בלתי נפרד מהשם הכללי והגנרי "קרב מגע".

מייסד שיטת ק.מ.י, אליהו (אלי) אביקזר ז"ל, היה תלמידו של מייסד קרב המגע אמריך אימי ליכטנפלד שדה-אור ז"ל, והראשון שקיבל מאימי חגורה.

במשך שנים רבות אלי עבד בצמוד לאימי. לימים, כאשר אימי פרש מהצבא, נבחר אלי במקומו לתפקיד ראש מדור קרב מגע בצה"ל.

עם פרישתו של אלי מהצבא הוא הציע לאימי להוסיף את המילה "ישראל" לשם קרב מגע, כדי להבדיל בין השם הכללי והגנרי "קרב מגע" לבין שם ייחודי וספציפי הכולל את ארץ המקור – ישראל. אימי דחה את הרעיון בטענה שהמילה "ישראל" עלולה להרתיע אנשים מחו"ל מללמוד קרב מגע ישראלי.

בשנת 1987, ובברכתו של אימי, פרש אלי מהאגודה לקרב מגע במטרה להקים שיטה בשם "קרב מגע ישראלי" או בקיצור "ק.מ.י". מאחר שאימי התנגד לשם קרב מגע ישראלי, ומכיוון שאלי כיבד את אימי, החליף אלי את המילה "מגע" ל"מגן", כך שיישארו ראשי התיבות "ק.מ.י". בדרך זו לשם "קרב מגן" נוספה המילה "ישראלי", והשם ק.מ.י הפך לשם ספציפי ולא גנרי.

שינוי המילה ל"מגן" התאים למטרת השיטה, שנוסדה כדי ללמד את האוכלוסייה הגנה עצמית, אך עדיין השאיר את השיטה כחלק ממשפחת קרב המגע. בנוסף, קבע אלי כי בגד האימון בק.מ.י יהיה בצבעי כחול־לבן, כצבעי דגל ישראל.

השיטה מתעדכנת, משתנה ומתאימה את התרגילים למצבים ולסכנות המודרניות והעכשוויות.

השם "קרב מגע" משמש עדיין שם כללי וגנרי, וכל אחד יכול להשתמש בו וללמד קרב מגע ללא פיקוח מקצועי. לעומתו, עמותת ק.מ.י רשומה בישראל ובעולם; השם וסמלי ק.מ.י רשומים ומוגנים בישראל ובעולם; ותוכנית הלימודים מסודרת וזהה, כך שבכל העולם נלמדים התרגילים באופן אחיד ותחת פיקוח מקצועי.

ק.מ.י כיום הוא הארגון הגדול בישראל בתחום קרב המגע, ונמצא תחת פיקוח מקצועי מתמיד של העמותה.

המדריכים, מרמת עוזר מדריך ועד לרמת מומחה בענף, עוברים הכשרות המפוקחות ומאושרות על ידי המרכז האקדמי לוינסקי־וינגייט ומשרד הספורט הישראלי.

שיטת ק.מ.י מאושרת על ידי המרכז האקדמי לוינסקי־וינגייט. מורים לחינוך גופני משתלמים במרכז האקדמי בקורסים להגנה עצמית מטעם ק.מ.י. השיטה הומלצה לנעמ"ת על ידי משטרת ישראל להעברת סדנאות להגנה עצמית לנערות ולנשים. שיטת ק.מ.י משמשת בזרועות הביטחון השונות, מבוקשת ברחבי העולם וזכתה להכרה בין־לאומית.
                                """.trimIndent(),
                                """
K.A.M.I — Israeli Protective Combat — is a method that grew out of the Krav Maga family and remains an integral part of the general, generic name "Krav Maga".

The founder of K.A.M.I, Eliyahu (Eli) Avikzar, was a student of Krav Maga founder Imi Lichtenfeld Sde-Or, and was the first person to receive a belt from Imi.

For many years, Eli worked closely with Imi. Later, when Imi retired from the army, Eli was chosen to replace him as head of the Krav Maga section in the IDF.

When Eli retired from the army, he suggested adding the word "Israel" to the name Krav Maga, in order to distinguish between the general, generic name "Krav Maga" and a unique, specific name that included the country of origin — Israel. Imi rejected the idea, arguing that the word "Israel" might discourage people abroad from learning Israeli Krav Maga.

In 1987, with Imi's blessing, Eli left the Krav Maga association in order to establish a method named "Israeli Krav Maga", abbreviated in Hebrew as K.A.M.I. Since Imi objected to the name Israeli Krav Maga, and because Eli respected Imi, Eli replaced the Hebrew word "contact" with "protective", while keeping the same Hebrew initials K.A.M.I. In this way, the word "Israeli" was added to "Protective Combat", making K.A.M.I a specific name rather than a generic one.

Changing the word to "protective" matched the purpose of the method, which was founded to teach self-defense to the public, while still keeping the method within the Krav Maga family. Eli also decided that the K.A.M.I training uniform would be blue and white, like the colors of the Israeli flag.

The method continues to evolve, change, and adapt its techniques to modern and current situations and threats.

The name "Krav Maga" is still used as a general, generic name, and anyone may use it and teach Krav Maga without professional supervision. In contrast, the K.A.M.I association is registered in Israel and worldwide; the name and symbols of K.A.M.I are registered and protected in Israel and worldwide; and the curriculum is structured and consistent, so the techniques are taught uniformly around the world under professional supervision.

Today, K.A.M.I is the largest organization in Israel in the field of Krav Maga and is under constant professional supervision by the association.

Instructors, from assistant instructor level to expert level, undergo training supervised and approved by the Levinsky-Wingate Academic Center and the Israeli Ministry of Sports.

The K.A.M.I method is approved by the Levinsky-Wingate Academic Center. Physical education teachers train at the academic center in self-defense courses conducted by K.A.M.I. The method was recommended to Na'amat by the Israel Police for self-defense workshops for girls and women. K.A.M.I is used by various security branches, is sought after around the world, and has received international recognition.
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
                                "The ideology of K.A.M.I is characterized by these principles:",
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
The K.A.M.I method is approved by the Wingate Academic College. Physical education teachers receive training there in self-defense courses conducted by K.A.M.I. The method was recommended to Na'amat by the Israel Police for self-defense workshops for girls and women. K.A.M.I is used in various security branches and is sought after around the world. The method has received international recognition.
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
                                "Israeli Protective Combat - K.A.M.I focuses on two main areas:",
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
                                "The techniques taught in the K.A.M.I method:",
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
K.A.M.I techniques were developed according to the principle of simplicity: a natural and simple movement is a fast movement. Fast movement generates power. Using a basic, non-complicated motion creates minimum defensive movement against maximum attacking movement and enables every person to defend themselves and neutralize an attacker.

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
Unlike traditional martial arts, K.A.M.I adapts itself to the changing reality of the street and to immediate threats. K.A.M.I renews, improves, adds, or removes techniques and offers updated solutions. Perfection is an aspiration — but the true goal is preserving life, control, and self-restraint.
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
                                "K.A.M.I trainees report improvement in movement and coordination, increased personal confidence and self-confidence, courage, composure, discipline, self-control, and distancing themselves from violent environments.",
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
