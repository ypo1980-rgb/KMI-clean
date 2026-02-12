@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.security

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * "שער" לנעילה בסיסמה:
 * אם app_lock_mode == "pin" ויש app_lock_pin – מציג דיאלוג סיסמה
 * לפני שמרנדרים את התוכן של האפליקציה.
 */
@Composable
fun PinLockGate(
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val sp = remember(ctx) {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    val mode = sp.getString("app_lock_mode", "none") ?: "none"
    val storedPin = sp.getString("app_lock_pin", null)

    // אם אין נעילה בסיסמה או שלא הוגדרה סיסמה – לא חוסמים כלום
    if (mode != "pin" || storedPin.isNullOrEmpty()) {
        content()
        return
    }

    var unlocked by rememberSaveable { mutableStateOf(false) }

    if (!unlocked) {
        PinUnlockDialog(
            storedPin = storedPin,
            onSuccess = { unlocked = true },
            onCancel = {
                // סגירת הפעילות אם המשתמש בוחר לצאת
                (ctx as? Activity)?.finish()
            }
        )
    } else {
        content()
    }
}

/**
 * דיאלוג הזנת סיסמה (בדיקה מול app_lock_pin)
 */
@Composable
private fun PinUnlockDialog(
    storedPin: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var inputPin by rememberSaveable { mutableStateOf("") }
    var pinVisible by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { onCancel() },
        title = {
            Text(
                text = "הזן סיסמה לשחרור האפליקציה",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                OutlinedTextField(
                    value = inputPin,
                    onValueChange = {
                        inputPin = it
                        error = null
                    },
                    label = { Text(text = "סיסמה") },
                    singleLine = true,
                    visualTransformation = if (pinVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password   // ⬅ מקלדת מלאה
                    ),
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible)
                                    Icons.Filled.VisibilityOff
                                else
                                    Icons.Filled.Visibility,
                                contentDescription = if (pinVisible)
                                    "הסתר סיסמה"
                                else
                                    "הצג סיסמה"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (inputPin.isBlank()) {
                        error = "יש להזין סיסמה"
                        return@TextButton
                    }
                    if (inputPin != storedPin) {
                        error = "סיסמה שגויה"
                        return@TextButton
                    }

                    onSuccess()
                }
            ) {
                Text("אשר")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("בטל")
            }
        }
    )
}
