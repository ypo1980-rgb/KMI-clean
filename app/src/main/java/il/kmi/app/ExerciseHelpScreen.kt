package il.kmi.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.screens.ScreenWithSettings   // ✅ זה הפתרון
import il.kmi.shared.domain.Belt

@Composable
fun ExerciseHelpScreen(
    belt: Belt,
    topic: String,
    item: String,
    onBack: () -> Unit
) {
    val explanation = remember(belt, topic, item) {
        HelpRepo.helpTextFor(belt, topic, item)
    }

    ScreenWithSettings(
        title = "הסבר: $item",
        onOpenSettings = { /* אפשר להוסיף מעבר להגדרות */ },
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("חזרה")
            }
        }
    }
}
