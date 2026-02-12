@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsVerifyScreen(
    phone: String,
    onVerified: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    var code by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "אימות SMS",
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
                text = "קוד נשלח למספר:\n$phone",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    // ננקה הודעת שגיאה ברגע שמקלידים מחדש
                    errorText = null
                },
                label = { Text("קוד SMS") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
            )

            if (errorText != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val trimmed = code.trim()
                    if (trimmed == "123456") {
                        // רק הקוד שסיכמנו → הצלחה
                        onVerified(phone)
                    } else {
                        // כל קוד אחר → שגיאה
                        errorText = "קוד אימות שגוי. יש להזין את הקוד 123456."
                    }
                },
                enabled = code.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("אשר קוד", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/* ===== פונקציות עזר ===== */

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** ממיר מספר ישראלי 05… לפורמט E.164 (+972…) עבור Firebase */
private fun toE164Il(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.startsWith("972") -> "+$digits"
        digits.startsWith("0")   -> "+972" + digits.drop(1)
        else                     -> "+972$digits"
    }
}

/** פורמט יפה להצגה בלבד */
private fun formatLocalPhone(phone: String): String {
    val d = phone.filter { it.isDigit() }
    return when {
        d.length == 10 && d.startsWith("0") ->
            "${d.substring(0,3)}-${d.substring(3,6)}-${d.substring(6)}"
        else -> d
    }
}
