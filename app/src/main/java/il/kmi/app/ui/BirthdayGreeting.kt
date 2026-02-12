package il.kmi.app.ui

import android.content.SharedPreferences
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun BirthdayGate(
    sp: SharedPreferences,
    content: @Composable () -> Unit
) {
    val fullName = sp.getString("fullName", "") ?: ""
    val day = sp.getString("birth_day", null)?.toIntOrNull()
    val month = sp.getString("birth_month", null)?.toIntOrNull()

    val today = remember { LocalDate.now() }
    val todayKey = today.toString() // yyyy-MM-dd

    var showBirthday by remember {
        mutableStateOf(false)
    }

    // לבדוק פעם אחת כשנטען
    LaunchedEffect(Unit) {
        val lastShown = sp.getString("last_birthday_shown", null)

        if (day != null && month != null) {
            val isTodayBirthday = (day == today.dayOfMonth && month == today.monthValue)
            if (isTodayBirthday && lastShown != todayKey) {
                showBirthday = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        content()

        if (showBirthday) {
            AlertDialog(
                onDismissRequest = { /* לא נסגור בלחיצה בחוץ */ },
                confirmButton = {
                    TextButton(
                        onClick = {
                            sp.edit().putString("last_birthday_shown", todayKey).apply()
                            showBirthday = false
                        }
                    ) {
                        Text("להתחיל באימון 🎯")
                    }
                },
                title = {
                    Text(
                        text = "מזל טוב ${if (fullName.isNotBlank()) fullName else ""} 🎉",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("יום הולדת שמח! מאחלים לך המון בריאות, הצלחה ואימונים טובים בק.מ.י 💪")
                        Spacer(Modifier.height(12.dp))
                        Text("🎂 🎈 🎆", fontSize = 26.sp)
                    }
                }
            )
        }
    }
}
