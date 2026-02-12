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

// ⭐ רטט וצליל גלובליים
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.app.ui.rememberClickSoundGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAviAbisidonScreen(
    onClose: () -> Unit
) {

    // ⭐ helpers גלובליים – לקרוא פעם אחת בתחילת המסך
    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSoundGlobal()

    Scaffold(
        // טופ־בר רגיל – בלי X, רק שם המסך
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "אודות אבי אביסידון",
                centerTitle = true,
                showMenu = false,
                onBack = null,
                showRoleBadge = false,
                showBottomActions = true,
                lockSearch = true,
                lockHome = false
            )
        },
        containerColor = Color(0xFFF7F7FA)
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
                    horizontalAlignment = Alignment.Start
                ) {
                    // כותרת קבועה
                    Text(
                        "אבי אביסידון",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
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
                        Text(
                            "מייסד שיטת ק.מ.י קרב מגן ישראלי\nאבי אבסידון דאן 10 ראש שיטת ק.מ.י. ויו\"ר עמותת ק.מ.י.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "אבי אביסידון עוסק במקצועות קרב מגע וקרב מגן ישראלי, למעלה מ-40 שנה.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "בשנת 1979 הוענקה לאבי חגורה שחורה דאן 1 בקרב מגע.\nעל חגורה זו ועד דרגת דאן 4 חתום מייסד קרב המגע אימי ליכטנפלד והמדריך אלי אביקזר.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "בשלב זה, החל להרקם אצל אלי אביקזר רעיון מיסוד השינויים והשיפורים שפיתח בתרגילי קרב המגע ואיחודם למקצוע חדש בשם ק.מ.י. - קרב מגן ישראלי. אלי פנה לתלמידו הבכיר אבי, שהיה לסגנו ויד ימינו בהקמת והטמעת השיטה.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "כ-15 שנה שימש אבי אביסידון בתפקיד סגן ראש שיטת ק.מ.י. וסגן יו\"ר עמותת ק.מ.י.\nדרגות דאן 5 ועד דאן 7 הוענקו לאבי ע\"י מייסד ק.מ.י. אלי אביקזר.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "לאחר פטירת אלי אביקזר, נבחר אבי אביסידון ביוני 2004 לראש שיטת ק.מ.י. וליו\"ר עמותת ק.מ.י.\nאבי אביסידון מוסמך מטעם מכון וינגייט.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Divider()

                        Text(
                            "בידיו התעודות הבאות:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Bulleted("„מדריך קרב מגע\" מטעם מכון וינגייט - בי\"ס למאמנים.")
                        Bulleted("„מאמן באומנויות לחימה\" מטעם מכון וינגייט - בי\"ס למאמנים.")
                        Bulleted("„מורה בכיר באומנויות לחימה\" מטעם המכללה האקדמית בוינגייט ע\"ש זינמן.")
                        Text(
                            "משנת 1991 מלמד אבי קורסים לקרב מגן ישראלי במכללה האקדמית בוינגייט.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Divider()

                        Text(
                            "ניסיון צבאי וביטחוני:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "במשך שרותו הצבאי בשנים 1977-1979 שימש אבי כסגן ראש מדור קרב מגע בצה\"ל.\nבין השנים 1979-1991 אימן את שייטת-13 בקרב מגע ובכושר גופני.\nאבי המשיך בשרות מילואים בשייטת עד 2004 ובמקביל אימן יחידות מיוחדות.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Divider()

                        Text(
                            "משנת 1992 עוסק אבי אבסידון וצוות מדריכים מטעמו בניהול אימון והכשרת מאבטחים לגופים ממשלתיים ולמגזר העסקי:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Bulleted("משרד התחבורה,")
                        Bulleted("משרד החינוך,")
                        Bulleted("משרד הבריאות - אימון מאבטחי בתי החולים ואימון הסגל הרפואי של בתי החולים הפסיכיאטרים.")
                        Bulleted("רשות הדואר,")
                        Bulleted("רשות הנמלים חיפה ואילת,")
                        Bulleted("המכללה לבטחון וחקירות.")
                        Text(
                            "במקביל העביר אבי אבסידון השתלמויות למדריכות קרב מגע בצה\"ל בדגש על ההיבט האזרחי לצורך הכשרתן כמדריכות בבתי הספר.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Divider()

                        Text(
                            "פעילות בינלאומית:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "אבי מעורב בהכנת מאבטחים אישיים עבור נכבדים ופוליטיקאים בארץ ובחו\"ל ומעביר קורסים למשלחות המגיעות מטעם הקהילות היהודיות.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Divider()

                        Text(
                            "אקדמיה והכשרות:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "אבי אבסידון מרכז תחום של אומנות לחימה במכללה האקדמית בוינגייט אשר כולל כל סוגי האומנות לחימה למיניהם.\nבמסגרת זו קיים קורסים שנתיים וקורסים מרוכזים עבור מדריכים, מאמנים ומאמנים בכירים.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Divider()

                        Text(
                            "שב\"ס:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "אבי כיום מנחה את תוכנית ההדרכה של שירות בתי הסוהר ומעביר להם השתלמויות.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Divider()

                        Text(
                            "החזון של אבי אבסידון: העצמת שיטת ק.מ.י בארץ ובעולם.",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Start
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
                    contentDescription = "סגור",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp) // קצת קטן יותר שיהיה לו אוויר
                )
            }
        }
    }
}

@Composable
private fun Bulleted(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", style = MaterialTheme.typography.bodyLarge)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
