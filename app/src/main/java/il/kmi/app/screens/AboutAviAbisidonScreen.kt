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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import android.app.Activity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.app.ui.rememberClickSoundGlobal

//=========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAviAbisidonScreen(
    onClose: () -> Unit
) {

    // ⭐ helpers גלובליים – לקרוא פעם אחת בתחילת המסך
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSoundGlobal()

    val contextLang = LocalContext.current
    val langManager = remember { AppLanguageManager(contextLang) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val screenTitle = if (isEnglish) {
        "Founder of the K.M.I Method"
    } else {
        "מייסד שיטת ק.מ.י - קרב מגן ישראלי"
    }

    val cardTitle = if (isEnglish) "Avi Abisidon" else "אבי אביסידון"
    val closeCd = if (isEnglish) "Close" else "סגור"

    Scaffold(
        // טופבר רגיל – בלי X, רק שם המסך
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = screenTitle,
                centerTitle = true,
                showMenu = false,
                onBack = null,
                showRoleBadge = false,
                showBottomActions = true,
                lockSearch = true,
                lockHome = false,
                currentLang = if (isEnglish) "en" else "he",
                onToggleLanguage = {
                    val newLang =
                        if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    (contextLang as? Activity)?.recreate()
                }
            )
        },
        containerColor = Color(0xFFFFF7FA)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // הכרטיס
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                ) {
                    // כותרת קבועה
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    Divider()

                    // גוף גולל
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = if (isEnglish) {
                                    "Avi Abisidon, 10th Dan, Head of the K.M.I method and Chairman of the K.M.I association."
                                } else {
                                    "אבי אביסידון דאן 10 ראש שיטת ק.מ.י ויו\"ר עמותת ק.מ.י."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                            )

                            Surface(
                                shape = CircleShape,
                                shadowElevation = 6.dp,
                                tonalElevation = 2.dp,
                                modifier = Modifier.size(120.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.avi_avisidon),
                                    contentDescription = if (isEnglish) "Avi Abisidon" else "אבי אביסידון",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Text(
                            text = if (isEnglish) {
                                "Avi Abisidon has been engaged in Krav Maga and Israeli Defensive Combat for over 40 years."
                            } else {
                                "אבי אביסידון עוסק במקצועות קרב מגע וקרב מגן ישראלי, למעלה מ-40 שנה."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "In 1979, Avi was awarded a 1st Dan black belt in Krav Maga. Up to 4th Dan, his ranks were signed by Krav Maga founder Imi Lichtenfeld and instructor Eli Avikzar."
                            } else {
                                "בשנת 1979 הוענקה לאבי חגורה שחורה דאן 1 בקרב מגע.\nעל חגורה זו ועד דרגת דאן 4 חתום מייסד קרב המגע אימי ליכטנפלד והמדריך אלי אביקזר."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "At that stage, Eli Avikzar began shaping the idea of formalizing the improvements and refinements he had developed in Krav Maga and uniting them into a new discipline called K.M.I. - Krav Magen Israeli. Eli turned to his senior student Avi, who became his deputy and right-hand man in establishing and implementing the method."
                            } else {
                                "בשלב זה, החל להרקם אצל אלי אביקזר רעיון מיסוד השינויים והשיפורים שפיתח בתרגילי קרב המגע ואיחודם למקצוע חדש בשם ק.מ.י. - קרב מגן ישראלי. אלי פנה לתלמידו הבכיר אבי, שהיה לסגנו ויד ימינו בהקמת והטמעת השיטה."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "For about 15 years, Avi Abisidon served as Deputy Head of the K.M.I. method and Deputy Chairman of the K.M.I. association. Ranks 5th Dan through 7th Dan were awarded to Avi by K.M.I founder Eli Avikzar."
                            } else {
                                "כ-15 שנה שימש אבי אביסידון בתפקיד סגן ראש שיטת ק.מ.י. וסגן יו\"ר עמותת ק.מ.י.\nדרגות דאן 5 ועד דאן 7 הוענקו לאבי ע\"י מייסד ק.מ.י. אלי אביקזר."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "After Eli Avikzar's passing, Avi Abisidon was elected in June 2004 as Head of the K.M.I. method and Chairman of the K.M.I. association. Avi Abisidon is certified by the Wingate Institute."
                            } else {
                                "לאחר פטירת אלי אביקזר, נבחר אבי אביסידון ביוני 2004 לראש שיטת ק.מ.י. וליו\"ר עמותת ק.מ.י.\nאבי אביסידון מוסמך מטעם מכון וינגייט."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider()

                        Text(
                            text = if (isEnglish) "He holds the following certifications:" else "בידיו התעודות הבאות:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Bulleted(if (isEnglish) "Krav Maga Instructor - Wingate Institute, School for Coaches." else "„מדריך קרב מגע\" מטעם מכון וינגייט - בי\"ס למאמנים.", isEnglish)
                        Bulleted(if (isEnglish) "Martial Arts Coach - Wingate Institute, School for Coaches." else "„מאמן באומנויות לחימה\" מטעם מכון וינגייט - בי\"ס למאמנים.", isEnglish)
                        Bulleted(if (isEnglish) "Senior Martial Arts Teacher - The Academic College at Wingate named after Zinman." else "„מורה בכיר באומנויות לחימה\" מטעם המכללה האקדמית בוינגייט ע\"ש זינמן.", isEnglish)

                        Text(
                            text = if (isEnglish) {
                                "Since 1991, Avi has taught Israeli Defensive Combat courses at the Academic College at Wingate."
                            } else {
                                "משנת 1991 מלמד אבי קורסים לקרב מגן ישראלי במכללה האקדמית בוינגייט."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider()

                        Text(
                            text = if (isEnglish) "Military and security experience:" else "ניסיון צבאי וביטחוני:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "During his military service between 1977 and 1979, Avi served as Deputy Head of the Krav Maga section in the IDF. Between 1979 and 1991, he trained Shayetet 13 in Krav Maga and physical fitness. Avi continued reserve service with the unit until 2004 while also training special units."
                            } else {
                                "במשך שרותו הצבאי בשנים 1977-1979 שימש אבי כסגן ראש מדור קרב מגע בצה\"ל.\nבין השנים 1979-1991 אימן את שייטת-13 בקרב מגע ובכושר גופני.\nאבי המשיך בשרות מילואים בשייטת עד 2004 ובמקביל אימן יחידות מיוחדות."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider()

                        Text(
                            text = if (isEnglish) {
                                "Since 1992, Avi Abisidon and his instructor team have managed training and security personnel qualification for government bodies and the business sector:"
                            } else {
                                "משנת 1992 עוסק אבי אבסידון וצוות מדריכים מטעמו בניהול אימון והכשרת מאבטחים לגופים ממשלתיים ולמגזר העסקי:"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Bulleted(if (isEnglish) "Ministry of Transport" else "משרד התחבורה,", isEnglish)
                        Bulleted(if (isEnglish) "Ministry of Education" else "משרד החינוך,", isEnglish)
                        Bulleted(if (isEnglish) "Ministry of Health - training hospital security staff and medical teams in psychiatric hospitals." else "משרד הבריאות - אימון מאבטחי בתי החולים ואימון הסגל הרפואי של בתי החולים הפסיכיאטרים.", isEnglish)
                        Bulleted(if (isEnglish) "Postal Authority" else "רשות הדואר,", isEnglish)
                        Bulleted(if (isEnglish) "Port Authorities in Haifa and Eilat" else "רשות הנמלים חיפה ואילת,", isEnglish)
                        Bulleted(if (isEnglish) "College for Security and Investigations" else "המכללה לבטחון וחקירות.", isEnglish)

                        Text(
                            text = if (isEnglish) {
                                "At the same time, Avi Abisidon conducted training programs for female Krav Maga instructors in the IDF, with emphasis on the civilian aspect, to prepare them as instructors in schools."
                            } else {
                                "במקביל העביר אבי אבסידון השתלמויות למדריכות קרב מגע בצה\"ל בדגש על ההיבט האזרחי לצורך הכשרתן כמדריכות בבתי הספר."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider()

                        Text(
                            text = if (isEnglish) "International activity:" else "פעילות בינלאומית:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "Avi is involved in preparing personal security personnel for dignitaries and politicians in Israel and abroad, and leads courses for delegations arriving on behalf of Jewish communities."
                            } else {
                                "אבי מעורב בהכנת מאבטחים אישיים עבור נכבדים ופוליטיקאים בארץ ובחו\"ל ומעביר קורסים למשלחות המגיעות מטעם הקהילות היהודיות."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider()

                        Text(
                            text = if (isEnglish) "Academia and professional training:" else "אקדמיה והכשרות:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "Avi Abisidon leads the martial arts field at the Academic College at Wingate, covering many martial arts disciplines. In this framework, annual courses and intensive training programs are held for instructors, coaches, and senior coaches."                            } else {
                                "אבי אבסידון מרכז תחום של אומנות לחימה במכללה האקדמית בוינגייט אשר כולל כל סוגי האומנות לחימה למיניהם.\nבמסגרת זו קיים קורסים שנתיים וקורסים מרוכזים עבור מדריכים, מאמנים ומאמנים בכירים."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider()

                        Text(
                            text = if (isEnglish) "Prison Service:" else "שב\"ס:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "Avi currently guides the training program of the Israel Prison Service and conducts professional seminars for them."
                            } else {
                                "אבי כיום מנחה את תוכנית ההדרכה של שירות בתי הסוהר ומעביר להם השתלמויות."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider()

                        Text(
                            text = if (isEnglish) {
                                "Avi Abisidon's vision: to strengthen and expand the K.M.I method in Israel and around the world."
                            } else {
                                "החזון של אבי אבסידון: העצמת שיטת ק.מ.י בארץ ובעולם."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // כפתור סגירה "X" – עם צליל ורטט חזק
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 12.dp)
                    .size(22.dp) // כמעט צמוד לאיקס
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .clickable {
                        clickSound()
                        haptic(true)   // רטט חזק
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = closeCd,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun Bulleted(
    text: String,
    isEnglish: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isEnglish) {
            Text("•", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text("•", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
