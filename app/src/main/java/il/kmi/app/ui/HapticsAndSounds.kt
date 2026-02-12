package il.kmi.app.ui

import android.content.Context
import android.content.SharedPreferences
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/* =========================================================================
   רטט גלובלי לפי ההגדרות (חזק / קצר)
   strong = true  → LONG_PRESS  (רטט חזק)
   strong = false → KEYBOARD_TAP (רטט קצר)
   ========================================================================= */
@Composable
fun rememberHapticsGlobal(): (Boolean) -> Unit {
    val ctx = LocalContext.current
    val view = LocalView.current
    val sp = remember { ctx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE) }

    // קורא את ההעדפה – גם מהמפתחות הישנים כדי לשמור תאימות
    fun readEnabled(): Boolean =
        sp.getBoolean("haptics_on", sp.getBoolean("short_haptic", false))

    var enabled by remember { mutableStateOf(readEnabled()) }

    // מאזין לשינויים ב-SharedPreferences ומתעדכן מיידית
    DisposableEffect(sp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "haptics_on" || key == "short_haptic") {
                enabled = readEnabled()
            }
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // פונקציה שמפעילה רטט לפי ההגדרה
    return { strong ->
        if (enabled) {
            val type = if (strong) {
                HapticFeedbackConstants.LONG_PRESS   // רטט חזק
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP // רטט קצר
            }
            view.performHapticFeedback(type)
        }
    }
}

/* =========================================================================
   צליל הקשה גלובלי לפי ההגדרות
   ========================================================================= */

/** השם הראשי שבו נשתמש במסכים */
@Composable
fun rememberClickSound(): () -> Unit {
    val ctx = LocalContext.current
    val view = LocalView.current
    val sp = remember { ctx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE) }

    fun readEnabled(): Boolean =
        sp.getBoolean("click_sounds", sp.getBoolean("tap_sound", false))

    var enabled by remember { mutableStateOf(readEnabled()) }

    // מאזין לשינויים ב-SharedPreferences ומתעדכן מיידית
    DisposableEffect(sp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "click_sounds" || key == "tap_sound") {
                enabled = readEnabled()
            }
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // פונקציה שמנגנת צליל רק אם ההגדרה פעילה
    return {
        if (enabled) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }
}

/** עטיפה לתאימות לאחור – אם יש עדיין מסכים שקוראים ל־rememberClickSoundGlobal */
@Deprecated("השתמש ב-rememberClickSound במקום", ReplaceWith("rememberClickSound()"))
@Composable
fun rememberClickSoundGlobal(): () -> Unit = rememberClickSound()
