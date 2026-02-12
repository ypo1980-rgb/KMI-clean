package il.kmi.app.ui.debug

import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import il.kmi.shared.catalog.KmiCatalogFacade

@Composable
fun CatalogDebugScreen() {

    var selectedBelt by remember { mutableStateOf<String?>(null) }
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    var selectedSubTopic by remember { mutableStateOf<String?>(null) }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }

    val belts = remember { KmiCatalogFacade.listBelts() }

    Row(Modifier.fillMaxSize()) {

        // ===== LEFT: navigation =====
        LazyColumn(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .padding(8.dp)
        ) {

            if (selectedBelt == null) {
                items(belts) { b ->
                    DebugItem(b.title) { selectedBelt = b.id }
                }
            } else if (selectedTopic == null) {
                val topics = KmiCatalogFacade.listTopics(selectedBelt!!)
                item { DebugHeader("נושאים") }
                items(topics) { t ->
                    DebugItem(t.title) { selectedTopic = t.id }
                }
                item { DebugBack { selectedBelt = null } }
            } else if (selectedSubTopic == null) {
                val subs = KmiCatalogFacade.listSubTopics(selectedBelt!!, selectedTopic!!)
                val hasSubs = subs.isNotEmpty()

                if (hasSubs) {
                    item { DebugHeader("תתי נושאים") }
                    items(subs) { st ->
                        DebugItem(st.title) { selectedSubTopic = st.id }
                    }
                }

                val exercises = KmiCatalogFacade.listExercises(
                    beltId = selectedBelt!!,
                    topicId = selectedTopic!!,
                    subTopicId = null
                )

                if (exercises.isNotEmpty()) {
                    item { DebugHeader("תרגילים") }
                    items(exercises) { ex ->
                        DebugItem(ex.title) { selectedExerciseId = ex.id }
                    }
                }

                item { DebugBack { selectedTopic = null } }
            } else {
                val exercises = KmiCatalogFacade.listExercises(
                    beltId = selectedBelt!!,
                    topicId = selectedTopic!!,
                    subTopicId = selectedSubTopic
                )
                item { DebugHeader("תרגילים") }
                items(exercises) { ex ->
                    DebugItem(ex.title) { selectedExerciseId = ex.id }
                }
                item { DebugBack { selectedSubTopic = null } }
            }
        }

        // ===== RIGHT: HTML preview =====
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(8.dp)
        ) {
            if (selectedExerciseId != null) {
                HtmlPreview(KmiCatalogFacade.getExerciseHtml(selectedExerciseId!!))
            } else {
                Text("בחר תרגיל כדי לראות HTML", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun DebugItem(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun DebugHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun DebugBack(onClick: () -> Unit) {
    Text(
        text = "⬅ חזור",
        modifier = Modifier
            .padding(vertical = 12.dp)
            .clickable(onClick = onClick),
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun HtmlPreview(html: String) {
    val ctx = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
            }
        },
        update = { it.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) }
    )
}
