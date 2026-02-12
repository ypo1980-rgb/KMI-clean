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

@Composable
fun Bullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutNetworkScreen(
    onBack: () -> Unit
) {
    val pageBg = Color(0xFFF7F8FA)

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "אודות הרשת",
                centerTitle = true,
                showMenu = false,
                onBack = null,
                showRoleBadge = false,
                showBottomActions = true,
                lockSearch = true,
                showCoachBroadcastFab = false
            )
        },
        containerColor = pageBg,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->

        // כדי לשים את העיגול מעל הכרטיס
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
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "בית ספר לקרב מגע והגנה עצמית",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    Divider()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            """
רשת נוקאאוט הוקמה בשנת 2010 מתוך רצון שכל אדם ידע להגן על עצמו מפני הסכנות הנמצאות כיום ברחוב. המדריכים ברשת מעבירים אימונים במסגרות שונות ומגוונות כגון מכינות קדם צבאיות, מרכזים להכשרות מאבטחים, מרכזי גמילה מסמים ואלכוהול, פרויקטים לנוער בסיכון, בתי ספר לחינוך מיוחד, מרכזים קהילתיים ועוד. הרשת שלנו כוללת סניפים רבים ברחבי הארץ כגון בנתניה, פרדס חנה, צופים, כפר יעבץ, פורת ועוד.

הייחודיות שלנו היא לקחת תרגיל לחימה ולהקביל אותו למצב בחיים וללמוד איך להתמודד עם המצב בחיי היום־יום ולא רק בלחימה.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "קרב מגע מעניין אתכם? מוזמנים לקרוא עוד בנושא:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))

                        Bullet("הגנה תומכת לכל האוכלוסיות ובכל הגילאים")
                        Bullet("תורם לילדים עם ADHD וכיו\"ב – מיקוד ומשמעת עצמית")
                        Bullet("מגבש קבוצה ובונה ביטחון עצמי")

                        Spacer(Modifier.height(16.dp))

                        Text(
                            """
המדריכים ברשת נוקאאוט מתמחים בעבודה עם ילדים ומעבירים את החומר בדרכים חווייתיות כגון משחקי קרב, תחרויות וסימולציות.

המטרה העיקרית אצלנו באימונים היא קודם כל להפוך את החניך לאדם טוב שמכבד ועוזר לכל אדם. בתוך כך אנו מכניסים משמעת, ביטחון עצמי, שליטה, ריסון וקבלת השונה. במהלך השגת מטרה זו אנו כמובן מתייחסים לפן ההגנה העצמית והלחימה.

הדרך שלנו בתחום זה היא להביא את המציאות שברחוב לאימונים וללמד את התלמיד לצמצם את ההלם הראשוני עד כמה שאפשר ולפעול כפי יכולתו בהתאם למצב.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            // ❌ העיגול הקטן – אותו גובה בכל המסכים
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = 20.dp)
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
