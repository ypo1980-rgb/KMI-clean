@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.domain.Explanations
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.ui.KmiTtsManager
import il.kmi.shared.domain.Belt
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.color

@Composable
fun FavoritesScreen(
    onHome: () -> Unit = {},
) {
    val favorites: Set<String> by FavoritesStore.favoritesFlow.collectAsState(initial = emptySet())
    val scroll = rememberScrollState()

    var pickedId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "מועדפים",
                onHome = onHome,
                centerTitle = true,
                showTopHome = false,
                lockSearch = true,
                onPickSearchResult = { },
                extraActions = { }
            )
        }
    ) { padding ->

        // ✅ מזהים עבור כל favorite: belt + topic + rawItem (מה-ContentRepo)
        data class FavHit(
            val favId: String,
            val belt: Belt,
            val topic: String,
            val rawItem: String
        )

        fun normKey(s: String): String = s
            .replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
            .replace(Regex("[\u0591-\u05C7]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()

        val favHits: List<FavHit> = remember(favorites) {
            if (favorites.isEmpty()) return@remember emptyList()

            // 1) ניסיון מהיר: אם favId הוא itemKey מלא -> resolveItemKey מחזיר belt/topic/subtopic/item
            val resolved = favorites.mapNotNull { favId ->
                val r = ContentRepo.resolveItemKey(favId) ?: return@mapNotNull null
                val topic = r.topicTitle
                val raw = r.itemTitle // אצלך זה "itemTitle" (השם שנשמר בתוך המפתח)
                FavHit(
                    favId = favId,
                    belt = r.belt,
                    topic = topic,
                    rawItem = raw
                )
            }

            // אם הצלחנו לפתור את כולם/רובם — אין צורך באינדקס כבד
            if (resolved.isNotEmpty()) {
                return@remember resolved.sortedWith(
                    compareBy<FavHit>(
                        { Belt.order.indexOf(it.belt).let { idx -> if (idx >= 0) idx else 999 } },
                        { it.topic },
                        { normKey(ExerciseTitleFormatter.displayName(it.rawItem).ifBlank { it.rawItem }) }
                    )
                )
            }

            // 2) FALLBACK: אינדקס לפי "שם תצוגה מנורמל" -> (belt, topic, rawItem)
            val index = buildMap<String, FavHit> {
                ContentRepo.listBeltsInOrder().forEach { belt ->
                    ContentRepo.listTopicTitles(belt).forEach { topicTitle ->

                        // items ישירים בנושא (ללא תת-נושא)
                        ContentRepo.listItemTitles(belt, topicTitle, subTopicTitle = null).forEach { raw ->
                            val key = normKey(ExerciseTitleFormatter.displayName(raw).ifBlank { raw })
                            putIfAbsent(key, FavHit(favId = "", belt = belt, topic = topicTitle, rawItem = raw))
                        }

                        // items של תתי-נושאים
                        ContentRepo.listSubTopicTitles(belt, topicTitle).forEach { stTitle ->
                            ContentRepo.listItemTitles(belt, topicTitle, subTopicTitle = stTitle).forEach { raw ->
                                val key = normKey(ExerciseTitleFormatter.displayName(raw).ifBlank { raw })
                                putIfAbsent(key, FavHit(favId = "", belt = belt, topic = topicTitle, rawItem = raw))
                            }
                        }
                    }
                }
            }

            favorites.mapNotNull { favId ->
                val key = normKey(favId)
                val hit = index[key] ?: return@mapNotNull null
                hit.copy(favId = favId)
            }.sortedWith(
                compareBy<FavHit>(
                    { Belt.order.indexOf(it.belt).let { idx -> if (idx >= 0) idx else 999 } },
                    { it.topic },
                    { normKey(ExerciseTitleFormatter.displayName(it.rawItem).ifBlank { it.rawItem }) }
                )
            )
        }

        // קיבוץ חגורה -> topic -> hits
        val grouped = remember(favHits) {
            favHits
                .groupBy { it.belt }
                .mapValues { (_, hitsByBelt) -> hitsByBelt.groupBy { it.topic } }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (favorites.isEmpty()) {
                Text(
                    text = "אין עדיין תרגילים במועדפים.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                )
            } else if (favHits.isEmpty()) {
                // אם יש ids במועדפים אבל לא הצלחנו למפות ל-ContentRepo
                Text(
                    text = "יש מועדפים, אבל לא הצלחתי למפות אותם לחגורה/נושא. (כנראה IDs ישנים).",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                )
            } else {
                Belt.order
                    .filter { it in grouped.keys }
                    .forEach { belt ->
                        val byTopic = grouped[belt].orEmpty()

                        // Header חגורה
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = belt.color.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = belt.heb,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                        }

                        byTopic.keys.sorted().forEach { topic ->
                            // Header נושא
                            Text(
                                text = topic,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp)
                            )

                            byTopic[topic].orEmpty().forEach { hit ->
                                val display = ExerciseTitleFormatter.displayName(hit.rawItem).ifBlank { hit.favId }

                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    tonalElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { pickedId = hit.favId } // נשמור pickedId = favId (כמו קודם)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { FavoritesStore.toggle(hit.favId) }) {
                                            val isFav = favorites.contains(hit.favId)
                                            if (isFav) {
                                                Icon(Icons.Filled.Star, contentDescription = "הסר ממועדפים")
                                            } else {
                                                Icon(Icons.Outlined.StarBorder, contentDescription = "הוסף למועדפים")
                                            }
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        Text(
                                            text = display,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Right,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
            }
        }

        pickedId?.let { favId ->
            // ✅ מוצאים את ה-hit לפי favId כדי לדעת belt/topic/rawItem
            val hit = remember(favId, favHits) { favHits.firstOrNull { it.favId == favId } }

            val display = remember(hit, favId) {
                val raw = hit?.rawItem ?: favId
                ExerciseTitleFormatter.displayName(raw).ifBlank { raw }
            }

            // ✅ TTS גלובלי אחיד
            LaunchedEffect(favId) {
                // אם אין לך LocalAppContext:
                // KmiTtsManager.init(ctx)  <-- אם יש לך ctx
            }
            DisposableEffect(favId) { onDispose { KmiTtsManager.stop() } }

            val belt = hit?.belt
            val topic = hit?.topic.orEmpty()

            val explanation = remember(belt, display, hit, favId) {
                if (belt == null) {
                    "לא הצלחתי למפות את המועדף הזה לחגורה/נושא.\nID: $favId"
                } else {
                    // קודם לפי display (כי ככה לרוב שמור ב-Explanations), ואז fallback ל-raw
                    Explanations.get(belt, display)
                        .ifBlank { hit?.rawItem?.let { Explanations.get(belt, it) }.orEmpty() }
                        .ifBlank { "אין כרגע הסבר לתרגיל הזה." }
                }
            }

            AlertDialog(
                onDismissRequest = { KmiTtsManager.stop(); pickedId = null },
                title = {
                    Box(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = display,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (belt != null) {
                                Text(
                                    text = "(${belt.heb}${if (topic.isNotBlank()) " · $topic" else ""})",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        IconButton(
                            onClick = { FavoritesStore.toggle(favId) },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            if (favorites.contains(favId)) {
                                Icon(Icons.Filled.Star, contentDescription = "הסר ממועדפים")
                            } else {
                                Icon(Icons.Outlined.StarBorder, contentDescription = "הוסף למועדפים")
                            }
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(explanation, textAlign = TextAlign.Right)
                        IconButton(onClick = { KmiTtsManager.speak(explanation) }) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "השמע הסבר")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { KmiTtsManager.stop(); pickedId = null }) { Text("סגור") }
                }
            )
        }
    }
}
