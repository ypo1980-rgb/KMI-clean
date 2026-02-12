package il.kmi.app.debug

import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import il.kmi.shared.catalog.ExerciseDto
import il.kmi.shared.catalog.KmiCatalogFacade

@Composable
fun CatalogDebugScreen() {

    // נטען פעם אחת (כי InMemoryCatalog כבר בזיכרון)
    val belts = remember { KmiCatalogFacade.listBelts() }

    var htmlToShow by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // בונים "רשימה שטוחה" של items — בלי קינון של items() בתוך items()
            belts.forEach { belt ->
                item(key = "belt_${belt.id}") {
                    Text(
                        text = "🥋 ${belt.title}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                    )
                }

                val topics = KmiCatalogFacade.listTopics(belt.id)
                topics.forEach { topic ->
                    item(key = "topic_${belt.id}_${topic.id}") {
                        Text(
                            text = "• ${topic.title}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                        )
                    }

                    // תרגילים ברמת נושא
                    val direct = KmiCatalogFacade.listExercises(
                        beltId = belt.id,
                        topicId = topic.id,
                        subTopicId = null
                    )

                    items(
                        items = direct,
                        key = { ex -> "ex_${belt.id}_${topic.id}_direct_${ex.id}" }
                    ) { ex ->
                        ExerciseRow(
                            ex = ex,
                            indentDp = 24,
                            onHtml = { htmlToShow = KmiCatalogFacade.getExerciseHtml(ex.id) }
                        )
                    }

                    // תתי נושאים + תרגילים של תת נושא
                    val subs = KmiCatalogFacade.listSubTopics(belt.id, topic.id)
                    subs.forEach { st ->
                        item(key = "sub_${belt.id}_${topic.id}_${st.id}") {
                            Text(
                                text = "↳ ${st.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 2.dp)
                            )
                        }

                        val subEx = KmiCatalogFacade.listExercises(
                            beltId = belt.id,
                            topicId = topic.id,
                            subTopicId = st.id
                        )

                        items(
                            items = subEx,
                            key = { ex -> "ex_${belt.id}_${topic.id}_${st.id}_${ex.id}" }
                        ) { ex ->
                            ExerciseRow(
                                ex = ex,
                                indentDp = 36,
                                onHtml = { htmlToShow = KmiCatalogFacade.getExerciseHtml(ex.id) }
                            )
                        }
                    }
                }
            }
        }

        // ===== HTML DIALOG =====
        val html = htmlToShow
        if (html != null) {
            AlertDialog(
                onDismissRequest = { htmlToShow = null },
                confirmButton = {
                    TextButton(onClick = { htmlToShow = null }) { Text("סגור") }
                },
                text = {
                    AndroidHtml(html)
                }
            )
        }
    }
}

@Composable
private fun ExerciseRow(
    ex: ExerciseDto,
    indentDp: Int,
    onHtml: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentDp.dp, top = 2.dp, bottom = 2.dp)
            .clickable { onHtml() },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "• ${ex.title}")
        Text(text = "HTML", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AndroidHtml(html: String) {
    val ctx = LocalContext.current
    AndroidView(
        factory = {
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    )
}
