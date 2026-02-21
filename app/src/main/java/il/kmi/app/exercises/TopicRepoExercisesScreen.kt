package il.kmi.app.exercises

import android.net.Uri
import android.util.Log
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
import il.kmi.app.domain.ContentRepo
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.Canonical.parseDefenseTagAndName
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

private fun isPunchSubTopic(sub: String?): Boolean {
    val s = sub?.trim().orEmpty()
    return s.contains("אגרוף") || s.contains("אגרופים")
}

private fun isKickSubTopic(sub: String?): Boolean {
    val s = sub?.trim().orEmpty()
    return s.contains("בעיטה") || s.contains("בעיטות")
}

private fun matchesDefenseFilter(topicTitle: String, subTopic: String?, raw: String): Boolean {
    val t = topicTitle.trim()
    val wantsPunch = isPunchSubTopic(subTopic)
    val wantsKick = isKickSubTopic(subTopic)

    if (!wantsPunch && !wantsKick) return true

    val isInternalTopic = t.contains("פנימ")
    val isExternalTopic = t.contains("חיצונ")

    val parsed = parseDefenseTagAndName(raw.trim())
    val tag = parsed.tag.lowercase()
    val name = parsed.displayName.trim()

    val isPunch = tag.contains("punch") || name.contains("אגרוף") || name.contains("אגרופים")
    val isKick  = tag.contains("kick")  || name.contains("בעיטה") || name.contains("בעיטות")

    val isInternal = tag.contains("internal") || name.contains("פנימ")
    val isExternal = tag.contains("external") || name.contains("חיצונ")

    val okKind = (wantsPunch && isPunch) || (wantsKick && isKick)
    if (!okKind) return false

    if (isInternalTopic) return isInternal || (!isInternal && !isExternal)
    if (isExternalTopic) return isExternal || (!isInternal && !isExternal)

    return true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicRepoExercisesScreen(
    belt: Belt,
    topicId: String,
    subTopicId: String,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit = {}
) {
    Log.e("DEF_DEBUG", "ENTER SCREEN belt=${belt.id} topicId='$topicId' subTopicId='$subTopicId'")

    LaunchedEffect(Unit) {
        Log.e("DEF_DEBUG", "SCREEN SHOWN TopicRepoExercisesScreen")
    }

    // ✅ FIX: Uri.decode לא מחזיר '+' לרווח. Route.make משתמש ב-URLEncoder שמחזיר '+'
    fun decArg(s: String): String {
        val d = runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)
        return d.replace("+", " ").trim()
    }

// בתוך TopicRepoExercisesScreen(...)

    val topic = remember(topicId) { Uri.decode(topicId).trim() }

    val subTopicOrNull = remember(subTopicId) {
        val decoded = Uri.decode(subTopicId).trim()
        decoded.takeIf { it.isNotBlank() && it != "__all__" }
    }

    val isDefenseFamily = remember(topic) { topic.contains("הגנות") }

// ✅ FIX: לבחור topic אמיתי ל-Repo לפי מה שנכנס מה-UI
    val repoTopicTitle = remember(topic, isDefenseFamily) {
        if (!isDefenseFamily) return@remember topic

        when {
            topic.contains("הגנות פנימיות") -> "הגנות פנימיות"
            topic.contains("הגנות חיצוניות") -> "הגנות חיצוניות"
            else -> "הגנות"
        }
    }

// ✅ FIX: בהגנות – כן להעביר subTopic אם קיים (אגרופים/בעיטות וכו')
    val repoSubTopicOrNull = remember(isDefenseFamily, subTopicOrNull) {
        if (!isDefenseFamily) subTopicOrNull else subTopicOrNull
    }

    var items by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(belt, topic, subTopicOrNull) {
        runCatching { ContentRepo.initIfNeeded() }
            .onFailure { Log.e("DEF_DEBUG", "ContentRepo.initIfNeeded failed", it) }

        val uiTopic = topic.trim()
        val uiSub = subTopicOrNull?.trim()

        Log.e(
            "DEF_DEBUG",
            "PARAMS belt=${belt.id} uiTopic='$uiTopic' uiSub='$uiSub' repoTopic='$repoTopicTitle' repoSub='$repoSubTopicOrNull'"
        )

        suspend fun load(topicTitle: String, subTitle: String?): List<String> {
            var last: List<String> = emptyList()

            repeat(6) { attempt ->
                last = runCatching {
                    ContentRepo.listItemTitles(
                        belt = belt,
                        topicTitle = topicTitle,
                        subTopicTitle = subTitle
                    )
                }.getOrDefault(emptyList())

                Log.e(
                    "DEF_DEBUG",
                    "LOAD attempt=$attempt topic='$topicTitle' sub='${subTitle ?: "null"}' count=${last.size}"
                )

                if (last.isNotEmpty()) return last
                kotlinx.coroutines.delay(150)
            }

            return last
        }

        // ✅ אם זה "הגנות..." לפעמים התוכן באמת יושב תחת "הגנות" או תחת "הגנות פנימיות/חיצוניות"
        val rawList: List<String> = if (isDefenseFamily) {
            val fromRoot = load("הגנות", null)

            // uiTopic יכול להיות "הגנות פנימיות" / "הגנות חיצוניות" / "הגנות ..." וכו'
            val fromUi = if (uiTopic != "הגנות") load(uiTopic, null) else emptyList()

            // לפעמים יש מצב שאתה נכנס מ"MaterialScreen" כשה-topic הוא "הגנות פנימיות - אגרופים"
            // אז ננסה גם לנקות לסלאש בסיסי אם צריך:
            val uiBase = uiTopic
                .replace("–", "-")
                .substringBefore(" - ")
                .trim()

            val fromUiBase = if (uiBase.isNotBlank() && uiBase != uiTopic && uiBase != "הגנות") {
                load(uiBase, null)
            } else emptyList()

            (fromRoot + fromUi + fromUiBase)
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
        } else {
            load(repoTopicTitle, repoSubTopicOrNull)
        }

        Log.e("DEF_DEBUG", "RAW COUNT = ${rawList.size}")
        rawList.take(20).forEachIndexed { i, it ->
            Log.e("DEF_DEBUG", "RAW[$i] = '$it'")
        }

        val filteredRaw = rawList
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { raw ->
                if (isDefenseFamily) {
                    matchesDefenseFilter(
                        topicTitle = uiTopic,
                        subTopic = uiSub,
                        raw = raw
                    )
                } else true
            }
            .toList()

        Log.e("DEF_DEBUG", "filteredCount=${filteredRaw.size}")
        filteredRaw.take(12).forEachIndexed { i, raw ->
            Log.d("DEF_DEBUG", "FILTERED[$i]='$raw'")
        }

        items = filteredRaw
            .asSequence()
            .map { raw -> ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
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
                        Text(
                            text = subTopicOrNull ?: "כללי",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("חזרה") } }
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
                    text = "לא נמצאו תרגילים עבור \"$topic\"",
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
