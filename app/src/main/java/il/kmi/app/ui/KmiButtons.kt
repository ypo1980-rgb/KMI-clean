package il.kmi.app.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape

// ✅ צליל קצר
private fun playClickSound() {
    val toneGen = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150) // 150ms
}

// ✅ רטט חזק ומובחן
private fun vibrate(context: Context, millis: Long = 80L) {
    val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        context.getSystemService(Vibrator::class.java)
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        vibrator?.vibrate(
            VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(millis)
    }
}

// ✅ פונקציה מרכזית לפידבק
private fun performFeedback(
    context: Context,
    haptic: HapticFeedback,
    clickSounds: Boolean,
    hapticsOn: Boolean
) {
    if (clickSounds) {
        playClickSound()
    }
    if (hapticsOn) {
        vibrate(context, 80)
    }
}

// 🔘 כפתור מלא עם פידבק
@Composable
fun KmiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE) }
    val clickSounds = sp.getBoolean("click_sounds", true)
    val hapticsOn = sp.getBoolean("haptics_on", true)
    val haptic = LocalHapticFeedback.current

    Button(
        onClick = {
            performFeedback(context, haptic, clickSounds, hapticsOn)
            onClick()
        },
        modifier = modifier,
        contentPadding = contentPadding,
        shape = shape,
        colors = colors
    ) {
        content()
    }
}

// 🔲 כפתור מתאר עם פידבק
@Composable
fun KmiOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    shape: Shape = RoundedCornerShape(20.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE) }
    val clickSounds = sp.getBoolean("click_sounds", true)
    val hapticsOn = sp.getBoolean("haptics_on", true)
    val haptic = LocalHapticFeedback.current

    OutlinedButton(
        onClick = {
            performFeedback(context, haptic, clickSounds, hapticsOn)
            onClick()
        },
        modifier = modifier,
        contentPadding = contentPadding,
        shape = shape,
        colors = colors
    ) {
        content()
    }
}
