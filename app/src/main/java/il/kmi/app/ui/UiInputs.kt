@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope

/* ===========================
   RTL GLOBAL POLICY (Hebrew)
   =========================== */
/** עוטף מסך/עץ קומפוזיציה שלם ומגדיר RTL רק כש- currentLang מתחיל ב-"he". */
@Composable
fun AppDirectionProvider(
    currentLang: String,
    content: @Composable () -> Unit
) {
    val isHebrew = remember(currentLang) { currentLang.lowercase().startsWith("he") }
    val dir = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides dir) {
        content()
    }
}

/* =========================================
   ProgressButton – כפתור עם מצב טעינה/הצלחה
   ========================================= */
@Composable
fun ProgressButton(
    onClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    successText: String? = null,       // אם לא null – מוצג לכמה מאות מילי־שניות אחרי הצלחה
    content: @Composable RowScope.() -> Unit
) {
    var inProgress by remember { mutableStateOf(false) }
    var justSucceeded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val buttonEnabled = enabled && !inProgress

    Button(
        onClick = {
            if (!buttonEnabled) return@Button
            scope.launch {
                inProgress = true
                justSucceeded = false
                runCatching { onClick() }
                    .onSuccess {
                        if (!successText.isNullOrBlank()) {
                            justSucceeded = true
                            kotlinx.coroutines.delay(550)
                            justSucceeded = false
                        }
                    }
                inProgress = false
            }
        },
        enabled = buttonEnabled,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        when {
            inProgress -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 8.dp)
                )
                Text("מבצע…")
            }
            justSucceeded && !successText.isNullOrBlank() -> {
                Text(successText!!)
            }
            else -> {
                content()
            }
        }
    }
}

/* =========================================
   LoadingOverlay – שכבת טעינה למסכים/Sheets
   ========================================= */
@Composable
fun LoadingOverlay(
    show: Boolean,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.35f,
) {
    if (!show) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = scrimAlpha))
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .size(36.dp),
            strokeWidth = 3.dp
        )
    }
}

/* =========================================
   shakeIfError – ניעור עדין בשגיאה לשדות קלט
   ========================================= */
@Composable
private fun rememberShakeOffset(active: Boolean): State<Dp> {
    val transition = rememberInfiniteTransition(label = "shake")
    val offset by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeValue"
    )
    return remember(active) {
        derivedStateOf { if (active) offset.dp else 0.dp }
    }
}

fun Modifier.shakeIfError(active: Boolean): Modifier {
    // אם לא פעיל – החזר as-is כדי לא להקצות אנימציה
    return if (!active) this else composed {
        val dx = rememberShakeOffset(active).value
        this.offset(x = dx)
    }
}

/* =========================================
   Haptics & Toast helpers
   ========================================= */
@Composable
fun rememberHaptics(): (Boolean) -> Unit {
    val h = LocalHapticFeedback.current
    val ctx = LocalContext.current

    // Vibrator (לשדרוג תחושה במכשירים שתומכים)
    val vibrator by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                ctx.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
        )
    }

    return remember {
        { success: Boolean ->
            // 1) Compose Haptics (קיים בכל הגרסאות)
            val composeType = if (success) {
                // הצלחה → תחושה ברורה
                HapticFeedbackType.LongPress
            } else {
                // כישלון → תנועה עדינה אך מורגשת
                HapticFeedbackType.TextHandleMove
            }
            h.performHapticFeedback(composeType)

            // 2) שדרוג עם Vibrator (אם קיים) — לא חובה, רק מחזק
            try {
                val v = vibrator ?: return@remember
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    val effect = if (success) {
                        // “Confirm”-like
                        android.os.VibrationEffect.createPredefined(
                            android.os.VibrationEffect.EFFECT_HEAVY_CLICK
                        )
                    } else {
                        // “Reject”-like
                        android.os.VibrationEffect.createPredefined(
                            android.os.VibrationEffect.EFFECT_DOUBLE_CLICK
                        )
                    }
                    v.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(25) // פולבק קצר למכשירים ישנים
                }
            } catch (_: Throwable) {
                // מתעלמים בשקט אם אין הרשאה/תמיכה
            }
        }
    }
}

@Composable
fun rememberToaster(): (String) -> Unit {
    val ctx = LocalContext.current
    return remember {
        { msg -> android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show() }
    }
}

/* =========================================
   Password TextField – טוגל עקבי (48.dp)
   ========================================= */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "סיסמה",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIconWhenVisible: ImageVector = Icons.Filled.VisibilityOff,
    trailingIconWhenHidden: ImageVector = Icons.Filled.Visibility
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            isError = isError,
            singleLine = true,
            label = { Text(label) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction?.invoke() },
                onGo = { onImeAction?.invoke() },
                onNext = { onImeAction?.invoke() },
                onSearch = { onImeAction?.invoke() },
                onSend = { onImeAction?.invoke() }
            ),
            leadingIcon = leadingIcon,
            trailingIcon = {
                val icon = if (showPassword) trailingIconWhenVisible else trailingIconWhenHidden
                val cd = if (showPassword) "הסתר סיסמה" else "הצג סיסמה"
                IconButton(
                    onClick = { showPassword = !showPassword },
                    enabled = enabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = cd)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (isError && !errorText.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorText.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
