package il.kmi.app.ui

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext

/**
 * פעולה מהירה לשינוי גודל גופנים.
 * כעת תומכת בסליידר רציף ושומרת:
 *  - font_scale (Float) בטווח 0.80..1.40
 *  - וגם font_size (small/medium/large) לתאימות לאחור
 */
@Composable
fun FontSizeQuickAction(
    sp: SharedPreferences,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }

    // KMP prefs (מקור אמת חוצה־פלטפורמות)
    val ctx = LocalContext.current
    val kmiPrefs = remember { il.kmi.shared.prefs.KmiPrefsFactory.create(ctx) }

    // קריאה מועדפת: KMP fontScaleString -> SP font_scale -> מיפוי לפי font_size (KMP/‏SP)
    fun initialScale(): Float {
        // 1) KMP
        kmiPrefs.fontScaleString.toFloatOrNull()?.let { return it.coerceIn(0.80f, 1.40f) }
        // 2) SP הישן
        val spScale = sp.getFloat("font_scale", Float.NaN)
        if (!spScale.isNaN()) return spScale.coerceIn(0.80f, 1.40f)
        // 3) מיפוי לפי small/medium/large (קודם KMP, ואז SP)
        val sizePref = kmiPrefs.fontSize.ifBlank { sp.getString("font_size", "medium") ?: "medium" }
        return when (sizePref) {
            "small" -> 0.92f
            "large" -> 1.12f
            else    -> 1.00f
        }
    }

    // טווחים: 0.80x..1.40x
    val minScale = 0.80f
    val maxScale = 1.40f

    var scale by remember { mutableStateOf(initialScale().coerceIn(minScale, maxScale)) }

    // כפתור בטופ־בר עם "אא"
    IconButton(onClick = { show = true }, modifier = modifier) {
        Text("אא", style = MaterialTheme.typography.titleMedium)
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("גודל גופן בכתיבה") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // אינדיקטורים קטנים/גדולים משני צידי הסקאלה
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("א")
                        Text("א")
                    }

                    // 🔸 סליידר רציף – אין steps
                    Slider(
                        value = scale,
                        onValueChange = { v ->
                            // שמירה רציפה כדי שה־MaterialTheme יתעדכן בלייב
                            val clamped = v.coerceIn(minScale, maxScale)
                            scale = clamped
                            sp.edit().putFloat("font_scale", clamped).apply()
                        },
                        valueRange = minScale..maxScale,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // תצוגה: אחוז מהגודל הרגיל
                    val pct = (scale * 100).roundToInt()
                    Text("נבחר: ~$pct%")
                }
            },
            confirmButton = {
                Button(onClick = {
                    // נשמור את הסקייל הרציף (למקרה שלא נשמר בגרירה)
                    sp.edit().putFloat("font_scale", scale).apply()

                    // תאימות לאחור: נמפה גם ל-small / medium / large
                    val legacy = when {
                        scale <= 0.95f -> "small"
                        scale >= 1.10f -> "large"
                        else -> "medium"
                    }
                    sp.edit().putString("font_size", legacy).apply()

                    // 👇 סנכרון ל-KMP (מקור אמת חוצה־פלטפורמות)
                    kmiPrefs.fontScaleString = scale.coerceIn(0.80f, 1.40f).toString()
                    kmiPrefs.fontSize = legacy

                    show = false
                }) { Text("שמור") }
            },
            dismissButton = {
                Button(onClick = { show = false }) { Text("בטל") }
            }
        )
    }
}
