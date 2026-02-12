package il.kmi.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ===== הגדרת המספרים המורשים =====
// תחליף כאן ל־5 מספרי הטלפון שאתה רוצה לאפשר.
// אפשר לכתוב עם או בלי מקפים – אנחנו מנרמלים רק ספרות.
private val AllowedPhones: Set<String> = setOf(
    "0546919790", //רונן פולק
    "0526664660", //יובל פולק
    "0526969287", // איציק ביטון
    "0524291463", //ישראל פולק
    "0543475347", //ליאם פולק
    "0523466051", //אופק פולק
    "0546277116", //יצחק לייזרוביץ'
    "0546321555",  //רועי פולק
    "0543922090",  //יניב פולק
    "0586597044",    //דוד מאבטח יס"ב
    "0529462832",    //מעיין ק.מ.י אופק
    "0587938991"    //ליאם מלכה ק.מ.י אופק
)

// נרמול טלפון: משאיר ספרות בלבד, ולוקח עד 10 ספרות אחרונות
private fun normalizePhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthGateScreen(
    onPhoneSubmitted: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    var phone by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "כניסה לפי טלפון",
                onBack = onBack,
                lockSearch = true,
                showRoleBadge = false,
                showCoachBroadcastFab = false,
                showBottomActions = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "כדי להשתמש באפליקציה, יש להכניס מספר טלפון מאומת מתוך הרשימה שסוכמה מראש.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    // ברגע שמשנים – ננקה הודעת שגיאה
                    if (errorText != null) errorText = null
                },
                label = { Text("מספר טלפון") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                isError = errorText != null
            )

            // הודעת שגיאה (אם יש)
            if (!errorText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorText.orEmpty(),
                    color = Color(0xFFD32F2F),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val normalized = normalizePhone(phone)
                    if (normalized in AllowedPhones) {
                        // טלפון מורשה – מעבירים החוצה (MainNavHost כבר שומר ב-SharedPreferences)
                        onPhoneSubmitted(normalized)
                    } else {
                        // טלפון לא מורשה
                        errorText = "מספר הטלפון אינו מורשה לשימוש באפליקציה. פנה למדריך לקבלת גישה."
                    }
                },
                enabled = phone.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("המשך", fontWeight = FontWeight.Bold)
            }
        }
    }
}
