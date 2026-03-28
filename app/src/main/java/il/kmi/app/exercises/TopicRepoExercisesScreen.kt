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
import il.kmi.shared.domain.content.HardSectionsCatalog
import il.kmi.shared.domain.content.HardSectionsCatalog.itemsFor
import il.kmi.shared.domain.content.HardSectionsCatalog.totalItemsCount

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
    onOpenExercise: (String) -> Unit = {},
    onOpenSubTopic: (topicId: String, subTopicId: String) -> Unit = { _, _ -> } // ✅ NEW
) {
    android.util.Log.e("DEF_DEBUG", "ENTER SCREEN belt=${belt.id} topicId='$topicId' subTopicId='$subTopicId'")
    println("DEF_DEBUG ENTER SCREEN belt=${belt.id} topicId='$topicId' subTopicId='$subTopicId'")

    LaunchedEffect(Unit) {
        Log.e(
            "DEF_DEBUG",
            "SCREEN SHOWN TopicRepoExercisesScreen belt=${belt.id} topicId='$topicId' subTopicId='$subTopicId'"
        )
    }

    // ✅ FIX: Uri.decode לא מחזיר '+' לרווח. Route.make משתמש ב-URLEncoder שמחזיר '+'
    fun decArg(s: String): String {
        val d = runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)
        return d.replace("+", " ").trim()
    }

    val topic = remember(topicId) { decArg(topicId) }

    val isDefenseFamily = remember(topic) { topic.contains("הגנות") }

    val subTopicOrNull = remember(subTopicId) {
        val decoded = decArg(subTopicId)
        decoded.takeIf { it.isNotBlank() && it != "__all__" }
    }

    LaunchedEffect(topicId, subTopicId, topic, subTopicOrNull) {
        Log.e(
            "DEF_DEBUG",
            "DECODED topicId='$topicId' -> topic='$topic' | subTopicId='$subTopicId' -> sub='${subTopicOrNull ?: "null"}'"
        )
    }

    // ✅ FIX: לבחור topic אמיתי ל-Repo לפי מה שנכנס מה-UI (כדי שלא יהיו unresolved)
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

    // ✅ NEW: שחרורים מגיעים מהקטלוג הקשיח (HardSectionsCatalog.releases)
    fun normHebLocal(s: String): String = s
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")
        .trim()

    // ✅ NEW: מנקה prefixים של UI כמו "שחרורים - ..."
    fun baseHardTitle(ui: String): String {
        val n = normHebLocal(ui)
        return n
            .removePrefix("שחרורים -").trim()
            .removePrefix("שחרורים-").trim()
            .removePrefix("שחרורים :").trim()
            .removePrefix("שחרורים:").trim()
            .trim()
    }

    // ✅ NEW: aliases שמגיעים מה-Registry / UI / hard ids אל titles אמיתיים ב-HardCatalog
    fun normalizeReleasesSubAlias(ui: String): String {
        val n0 = baseHardTitle(ui).trim()
        val n = n0
            .removePrefix("שחרור ").trim()
            .removePrefix("שחרורים ").trim()

        return when {
            // ✅ hard ids ישירים
            n0 == "releases" -> "שחרורים"
            n0 == "releases_hands_hair_shirt" -> "שחרור מתפיסות ידיים / שיער / חולצה"
            n0 == "releases_chokes" -> "שחרור מחניקות"
            n0 == "releases_hugs" -> "שחרור מחביקות"
            n0 == "releases_hugs_body" -> "חביקות גוף"
            n0 == "releases_hugs_neck" -> "חביקות צוואר"
            n0 == "releases_hugs_arm" -> "חביקות זרוע"

            // ✅ מתפיסות
            n0.contains("תפיס") || n0.contains("שיער") || n0.contains("חולצה") ||
                    n.contains("תפיס") || n.contains("שיער") || n.contains("חולצה") ->
                "שחרור מתפיסות ידיים / שיער / חולצה"

            // ✅ חניקות
            n0.contains("חניק") || n.contains("חניק") ->
                "שחרור מחניקות"

            // ✅ מחביקות הורה / תתי-נושאים
            n0.contains("מחביק") || n.contains("מחביק") ||
                    n0.startsWith("releases_hugs") -> when {
                n0.contains("body") || n0.contains("גוף") || n.contains("גוף") -> "חביקות גוף"
                n0.contains("neck") || n0.contains("צוואר") || n0.contains("צואר") || n.contains("צוואר") || n.contains("צואר") -> "חביקות צוואר"
                n0.contains("arm") || n0.contains("זרוע") || n.contains("זרוע") -> "חביקות זרוע"
                else -> "שחרור מחביקות"
            }

            // ✅ שורש
            n0 == "שחרורים" || n == "שחרורים" -> "שחרורים"

            else -> n0
        }
    }

    fun isReleasesHardTopic(uiTopic: String): Boolean {
        val t = normalizeReleasesSubAlias(uiTopic)
        if (t == "שחרורים") return true

        return HardSectionsCatalog.releases.any { sec ->
            normHebLocal(sec.title) == t ||
                    sec.subSections.any { sub -> normHebLocal(sub.title) == t }
        }
    }

    fun loadHardReleasesItems(
        belt: Belt,
        uiTopic: String,
        uiSub: String?
    ): List<String> {
        val t = normalizeReleasesSubAlias(uiTopic)
        val subWanted = uiSub?.let(::normalizeReleasesSubAlias)

        // 1) אם נכנסו עם topic = "שחרורים" + sub = "...": נחפש subsection לפי sub
        if (t == "שחרורים" && !subWanted.isNullOrBlank()) {
            HardSectionsCatalog.releases.forEach { sec ->
                // קודם: subSections
                sec.subSections.firstOrNull { sub -> normHebLocal(sub.title) == subWanted }
                    ?.let { return it.itemsFor(belt) }

                // ואם אין subSections וזו כותרת ישירה
                if (sec.subSections.isEmpty() && normHebLocal(sec.title) == subWanted) {
                    return sec.itemsFor(belt)
                }
            }
        }

        // 2) אם topic עצמו הוא "שחרור מחביקות" (הורה) והגיע sub = "חביקות גוף/צוואר/זרוע"
        HardSectionsCatalog.releases.firstOrNull { sec ->
            normHebLocal(sec.title) == t
        }?.let { parent ->
            if (!subWanted.isNullOrBlank()) {
                parent.subSections.firstOrNull { sub -> normHebLocal(sub.title) == subWanted }
                    ?.let { return it.itemsFor(belt) }
            }
            // אם אין subWanted – מחזירים את כל הפריטים תחת parent (שיטוח)
            return parent.itemsFor(belt)
        }

        // 3) אם topic עצמו הוא "חביקות גוף/צוואר/זרוע" – נחפש אותו בתוך subSections
        HardSectionsCatalog.releases.forEach { sec ->
            sec.subSections.firstOrNull { sub -> normHebLocal(sub.title) == t }
                ?.let { return it.itemsFor(belt) }
        }

        // 4) אם topic הוא Section רגיל בלי תתי סעיפים
        HardSectionsCatalog.releases.firstOrNull { sec ->
            sec.subSections.isEmpty() && normHebLocal(sec.title) == t
        }?.let { return it.itemsFor(belt) }

        return emptyList()
    }

    var items by remember { mutableStateOf<List<String>>(emptyList()) }
    // ✅ NEW: מחזיק רשימת תתי־נושאים קשיחים להצגה במקום רשימת תרגילים
    var hardSubSections by remember { mutableStateOf<List<HardSectionsCatalog.Section>>(emptyList()) }

    LaunchedEffect(belt, topic, subTopicOrNull) {
        runCatching { ContentRepo.initIfNeeded() }
            .onFailure { Log.e("DEF_DEBUG", "ContentRepo.initIfNeeded failed", it) }

        val uiTopic = topic.trim()
        val uiSub = subTopicOrNull?.trim()

        Log.e(
            "DEF_DEBUG",
            "PARAMS belt=${belt.id} uiTopic='$uiTopic' uiSub='$uiSub' repoTopic='$repoTopicTitle' repoSub='$repoSubTopicOrNull'"
        )

        // ✅ FIX: אם נכנסו עם topic="שחרורים" ו-sub="שחרור מחביקות" (או כל Section הורה עם subSections)
        // צריך להציג את תתי־הנושאים (חביקות גוף/צוואר/זרוע) במקום לנסות לטעון תרגילים ישר
        val tTopic = normalizeReleasesSubAlias(uiTopic)
        val tSub = uiSub?.let(::normalizeReleasesSubAlias).orEmpty()

        if (tTopic == "שחרורים" && tSub.isNotBlank()) {
            val parent = HardSectionsCatalog.releases.firstOrNull { sec ->
                normHebLocal(sec.title) == tSub && sec.subSections.isNotEmpty()
            }

            if (parent != null) {
                hardSubSections = parent.subSections
                items = emptyList()

                Log.e(
                    "DEF_DEBUG",
                    "HARD_RELEASES_SUBSECTIONS via topic='שחרורים' parent='${parent.title}' subSections=${parent.subSections.map { "${it.id}:${it.title}" }}"
                )

                return@LaunchedEffect
            }
        }

        // ✅ NEW: אם זה סקשן קשיח עם subSections (למשל "שחרורים - שחרור מחביקות") ועדיין לא בחרו תת־נושא
        if (isReleasesHardTopic(uiTopic) && uiSub.isNullOrBlank()) {
            val t = normalizeReleasesSubAlias(uiTopic)

            val parent = HardSectionsCatalog.releases.firstOrNull { sec ->
                normHebLocal(sec.title) == t && sec.subSections.isNotEmpty()
            }

            if (parent != null) {
                hardSubSections = parent.subSections
                items = emptyList()

                Log.e(
                    "DEF_DEBUG",
                    "HARD_RELEASES_SUBSECTIONS parent='${parent.title}' subSections=${parent.subSections.map { "${it.id}:${it.title}" }}"
                )

                return@LaunchedEffect
            } else {
                hardSubSections = emptyList()
            }
        } else {
            hardSubSections = emptyList()
        }

        // ✅ NEW: שחרורים (HardSectionsCatalog) לא נטענים מ-ContentRepo בכלל
        if (isReleasesHardTopic(uiTopic) || (uiSub != null && isReleasesHardTopic(uiSub))) {
            val hard = loadHardReleasesItems(
                belt = belt,
                uiTopic = uiTopic,
                uiSub = uiSub
            )
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()

            Log.e(
                "DEF_DEBUG",
                "HARD_RELEASES count=${hard.size} topic='$uiTopic' sub='${uiSub ?: "null"}' normalizedTopic='${normalizeReleasesSubAlias(uiTopic)}' normalizedSub='${uiSub?.let(::normalizeReleasesSubAlias) ?: "null"}'"
            )
            hard.take(20).forEachIndexed { i, it ->
                Log.e("DEF_DEBUG", "HARD[$i] = '$it'")
            }

            items = hard
                .asSequence()
                .map { raw -> ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()

            return@LaunchedEffect
        }

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

            // ✅ אם יש תתי־נושאים קשיחים (למשל "שחרור מחביקות") – מציגים אותם במקום items
            if (hardSubSections.isNotEmpty()) {

                android.util.Log.e(
                    "DEF_DEBUG",
                    "SHOW HARD_SUB_SECTIONS belt=${belt.id} topicId='$topicId' topic='$topic' subTopicId='$subTopicId' sections=${hardSubSections.map { "${it.id}:${it.title}" }}"
                )
                println("DEF_DEBUG SHOW HARD_SUB_SECTIONS belt=${belt.id} topicId='$topicId' topic='$topic' subTopicId='$subTopicId' sections=${hardSubSections.map { "${it.id}:${it.title}" }}")

                Text(
                    text = "בחר תת־נושא",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                hardSubSections.forEach { sec ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                android.util.Log.e(
                                    "DEF_DEBUG",
                                    "CLICK HARD_SUB topicId='$topicId' topic='$topic' subTopicId='$subTopicId' -> targetSectionId='${sec.id}' targetTitle='${sec.title}'"
                                )
                                println("DEF_DEBUG CLICK HARD_SUB topicId='$topicId' topic='$topic' subTopicId='$subTopicId' -> targetSectionId='${sec.id}' targetTitle='${sec.title}'")

                                onOpenSubTopic(
                                    topicId,
                                    sec.id
                                )
                            },
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = sec.title,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            textAlign = TextAlign.Right,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

            } else if (items.isEmpty()) {

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