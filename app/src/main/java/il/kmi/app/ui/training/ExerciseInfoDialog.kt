package il.kmi.app.ui.training

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.Explanations
import il.kmi.app.ui.KmiTtsManager

@Composable
fun ExerciseInfoDialog(
    belt: Belt,
    canonicalId: String,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val explanation = Explanations.get(belt, canonicalId).ifBlank {
        "לא נמצא הסבר עבור \"$canonicalId\"."
    }

    LaunchedEffect(Unit) { KmiTtsManager.init(ctx) }

    DisposableEffect(Unit) {
        onDispose { KmiTtsManager.stop() }
    }

    AlertDialog(
        onDismissRequest = {
            KmiTtsManager.stop()
            onDismiss()
        },
        title = {
            Text(
                "מידע על התרגיל",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Right
            )
        },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Right
                )
                IconButton(onClick = { KmiTtsManager.speak(explanation) }) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "השמע הסבר")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                KmiTtsManager.stop()
                onDismiss()
            }) { Text("סגור") }
        }
    )
}
