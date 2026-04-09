package il.kmi.app.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicExercisesScreen(
    belt: Belt,
    topicTitle: String,
    subTopicTitle: String?,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val topic = remember(topicTitle) { topicTitle.trim() }

    // ✅ מסנן "תת־נושא פייק" (שווה לנושא)
    val subClean = remember(subTopicTitle, topic) {
        subTopicTitle
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != topic }
    }

    val topicDisplay = remember(topic, isEnglish) {
        if (isEnglish) ExerciseTitlesEn.getOrSame(topic) else topic
    }

    val subDisplay = remember(subClean, isEnglish) {
        subClean?.let { if (isEnglish) ExerciseTitlesEn.getOrSame(it) else it }
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
                            text = topicDisplay,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!subDisplay.isNullOrBlank()) {
                            Text(
                                text = subDisplay,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(if (isEnglish) "Back" else "חזרה")
                    }
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
                        if (isEnglish) {
                            append("No exercises found for \"")
                            append(topicDisplay)
                            append("\"")
                            if (!subDisplay.isNullOrBlank()) {
                                append(" / \"")
                                append(subDisplay)
                                append("\"")
                            }
                        } else {
                            append("לא נמצאו תרגילים עבור \"")
                            append(topic)
                            append("\"")
                            if (!subClean.isNullOrBlank()) {
                                append(" / \"")
                                append(subClean)
                                append("\"")
                            }
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                items.forEach { item ->
                    val itemDisplay = if (isEnglish) {
                        ExerciseTitlesEn.getOrSame(item)
                    } else {
                        item
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenExercise(item) },
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = itemDisplay,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }
    }
}
