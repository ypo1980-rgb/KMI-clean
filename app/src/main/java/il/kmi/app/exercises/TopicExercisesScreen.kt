package il.kmi.app.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicExercisesScreen(
    belt: Belt,
    topicTitle: String,
    subTopicTitle: String?,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit
) {
    val topic = remember(topicTitle) { topicTitle.trim() }

    // ✅ מסנן "תת־נושא פייק" (שווה לנושא)
    val subClean = remember(subTopicTitle, topic) {
        subTopicTitle
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != topic }
    }

    // ✅ שולף תרגילים ישירות מה-Shared ContentRepo
    val items: List<String> = remember(belt, topic, subClean) {
        ContentRepo.getAllItemsFor(
            belt = belt,
            topicTitle = topic,
            subTopicTitle = subClean
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = topic,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!subClean.isNullOrBlank()) {
                            Text(
                                text = subClean,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("חזרה") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (items.isEmpty()) {
                Text(
                    text = buildString {
                        append("לא נמצאו תרגילים עבור \"")
                        append(topic)
                        append("\"")
                        if (!subClean.isNullOrBlank()) {
                            append(" / \"")
                            append(subClean)
                            append("\"")
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                items.forEach { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenExercise(item) },
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }
    }
}
