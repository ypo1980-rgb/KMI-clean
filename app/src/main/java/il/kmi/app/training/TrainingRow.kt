package il.kmi.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * שורה שמציגה פרטי אימון (דוגמה).
 * כאן נשתמש ב־SoundIconButton מתוך UiHelpers.kt
 */
@Composable
fun TrainingRow(
    title: String,
    isSoundOn: Boolean,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )

        // ✅ שימוש בפונקציה מתוך UiHelpers.kt
        SoundIconButton(
            enabled = isSoundOn,
            onToggle = onToggleSound
        )
    }
}
