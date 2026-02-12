@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import il.kmi.app.domain.Explanations
import il.kmi.app.domain.SubjectTopic as AppSubjectTopic
import il.kmi.app.domain.TopicsBySubjectRegistry
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.color
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.SubjectTopic as SharedSubjectTopic
import il.kmi.shared.domain.content.SubjectItemsResolver

private fun toSharedBeltOrNull(rawId: String?): Belt? {
    val s = rawId?.trim().orEmpty()
    if (s.isBlank()) return null

    // 1) אם fromId יודע להתמודד עם "yellow" וכו'
    Belt.fromId(s)?.let { return it }

    // 2) אם הגיע "YELLOW" / "Yellow" / "yellow"
    runCatching { return Belt.valueOf(s.uppercase()) }.getOrNull()

    // 3) אם הגיע "חגורה צהובה" / "צהובה" וכו' (fallback עדין)
    val heb = s.replace("חגורה", "").trim()
    return Belt.order.firstOrNull { it.heb.contains(heb) || heb.contains(it.heb.replace("חגורה", "").trim()) }
}

// ✅ עדיף להוציא enum החוצה כדי שלא "יתבלבל" קומפיילר/IDE בתוך scope
private enum class FilterMode { ALL, FAVORITES, RECENTS }

/**
 * מסך: כל התרגילים של נושא חוצה־חגורות.
 *
 * @param subjectId  ה-id מתוך TopicsBySubjectRegistry
 * @param isCoach    מצב מאמן/מתאמן (לרקע)
 * @param onBack     חזרה אחורה
 * @param onExerciseClick  קריאה חיצונית בעת לחיצה (לוגיקת ניווט/סטטיסטיקות נוספת אם תרצה)
 */
@Composable
fun SubjectExercisesScreen(
    subjectId: String,
    isCoach: Boolean,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onExerciseClick: (belt: Belt, topic: String, rawItem: String) -> Unit,
    screenTitle: String = "" // ✅ NEW
) {
    // (subjectId + belts …) נושא מה־app (הישן) ✅
    val appSubject: AppSubjectTopic = remember(subjectId) {
        TopicsBySubjectRegistry.subjectById(subjectId)
            ?: error("Unknown subjectId=$subjectId")
    }

    // ✅ NEW: כותרת למסך
     val screenTitleResolved = remember(screenTitle, appSubject.titleHeb) {
        val base = appSubject.titleHeb.trim()
        val picked = screenTitle.trim()

        when {
            picked.isBlank() -> base

            // כבר כותרת מלאה (כוללת את הבסיס)
            picked.startsWith(base) -> picked

            // רק תת־נושא => מוסיפים בסיס
            else -> "$base - $picked"
        }
    }

    // ✅ NEW: מפיקים "תת־בחירה" מתוך הכותרת (כי כרגע לא מעבירים פילטרים בניווט)
    fun detectReleasesPickFromTitle(title: String): String? {
        return when {
            title.contains("חניק") -> "שחרור מחניקות"
            title.contains("תפיס") || title.contains("אחיז") -> "שחרור מתפיסות ידיים"
            title.contains("חביק") || title.contains("חיבוק") -> "שחרור מחביקות גוף"
            title.contains("חולצ") || title.contains("שיער") -> "שחרור חולצה / שיער"
            else -> null
        }
    }

    data class ReleasesPickFilter(
        val includeAny: List<String>,
        val requireAll: List<String>,
        val excludeAny: List<String>
    )

    fun releasesFilterForPick(pick: String): ReleasesPickFilter {
        return when (pick) {
            "שחרור מתפיסות ידיים" -> ReleasesPickFilter(
                includeAny = listOf("תפיס", "אחיז", "אוחז"),
                requireAll = listOf("יד"),
                excludeAny = listOf("חניק", "חביק", "חולצ", "שיער", "אקדח", "סכין", "מקל")
            )

            "שחרור מחניקות" -> ReleasesPickFilter(
                includeAny = listOf("חניק", "חניקה", "חניקות", "צוואר"),
                requireAll = emptyList(),
                excludeAny = listOf("תפיס", "אחיז", "חביק", "חולצ", "שיער")
            )

            "שחרור מחביקות גוף" -> ReleasesPickFilter(
                includeAny = listOf("חביק", "חיבוק", "חיבוקים", "חביקות"),
                requireAll = emptyList(),
                excludeAny = listOf("חניק", "תפיס", "אחיז", "חולצ", "שיער")
            )

            "שחרור חולצה / שיער" -> ReleasesPickFilter(
                includeAny = listOf("חולצ", "חולצה", "שיער"),
                requireAll = emptyList(),
                excludeAny = listOf("חניק", "חביק", "תפיס", "אחיז")
            )

            else -> ReleasesPickFilter(
                includeAny = listOf(pick),
                requireAll = emptyList(),
                excludeAny = emptyList()
            )
        }
    }

    // ✅ NEW: נושא אפקטיבי לתצוגה – עבור releases בלבד
    val effectiveAppSubject: AppSubjectTopic = remember(appSubject, screenTitleResolved) {
        if (appSubject.id != "releases") return@remember appSubject

        val pick = detectReleasesPickFromTitle(screenTitleResolved) ?: return@remember appSubject
        val f = releasesFilterForPick(pick)

        appSubject.copy(
            // אפשר להשאיר subTopicHint ריק – אנחנו מסננים ב-include/require/exclude
            subTopicHint = null, // ✅ חשוב: אחרת זה מפיל הכל כי "שחרור מחניקות" לא מופיע בשם התרגיל
            includeItemKeywords = f.includeAny,
            requireAllItemKeywords = f.requireAll,
            excludeItemKeywords = f.excludeAny
        )
    }

    // (belts) המרה מרשימת החגורות ב־app לרשימת חגורות ב־shared ✅
    val beltsForUi: List<Belt> = remember(effectiveAppSubject) {
        effectiveAppSubject.belts.mapNotNull { appBelt ->
            // ✅ חשוב: לא ליפול ל-WHITE בשקט
            val b = toSharedBeltOrNull(appBelt.id)
            if (b == null) {
                android.util.Log.e("KMI_DBG", "SubjectExercisesScreen: cannot map belt id='${appBelt.id}'")
            }
            b
        }.distinct()
    }

    val sharedTopicsByBelt: Map<Belt, List<String>> = remember(effectiveAppSubject) {
        effectiveAppSubject.topicsByBelt.mapNotNull { (appBelt, topics) ->
            val b = toSharedBeltOrNull(appBelt.id)
            if (b == null) {
                android.util.Log.e("KMI_DBG", "SubjectExercisesScreen: cannot map topicsByBelt key id='${appBelt.id}'")
                null
            } else {
                b to topics
            }
        }.toMap()
    }

    // (shared subject) זה מה שה־SubjectItemsResolver מצפה לקבל ✅
    val sharedSubject: SharedSubjectTopic = remember(effectiveAppSubject, sharedTopicsByBelt) {
        SharedSubjectTopic(
            id = effectiveAppSubject.id,
            titleHeb = effectiveAppSubject.titleHeb,
            topicsByBelt = sharedTopicsByBelt,
            subTopicHint = effectiveAppSubject.subTopicHint,
            includeItemKeywords = effectiveAppSubject.includeItemKeywords.orEmpty(),
            requireAllItemKeywords = effectiveAppSubject.requireAllItemKeywords.orEmpty(),
            excludeItemKeywords = effectiveAppSubject.excludeItemKeywords.orEmpty()
        )
    }

    LaunchedEffect(subjectId, beltsForUi, sharedTopicsByBelt) {
        android.util.Log.e("KMI_DBG", "SubjectExercisesScreen subjectId=$subjectId title='${screenTitleResolved}'")
        android.util.Log.e("KMI_DBG", "beltsForUi=${beltsForUi.joinToString { it.name }}")
        android.util.Log.e("KMI_DBG", "sharedTopicsByBelt.keys=${sharedTopicsByBelt.keys.joinToString { it.name }}")
    }

    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()

    val ctx = LocalContext.current
    LaunchedEffect(subjectId) {
        KmiTtsManager.init(ctx)
        KmiTtsManager.setSpeechProfile(rate = 0.95f, pitch = 1.0f)
    }

    val backgroundBrush = remember(isCoach) {
        if (isCoach) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF141E30),
                    Color(0xFF243B55),
                    Color(0xFF0EA5E9)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF7F00FF),
                    Color(0xFF3F51B5),
                    Color(0xFF03A9F4)
                )
            )
        }
    }

    // נתוני שורה: belt + topic + rawItem (לניווט) + displayName + canonicalId
    data class RowData(
        val belt: Belt,
        val topic: String,
        val rawItem: String,
        val displayItem: String,
        val canonicalId: String
    )

    // shared resolver: מקור אמת ✅
    val rows: List<RowData> = remember(subjectId, beltsForUi, sharedSubject, effectiveAppSubject) {
        beltsForUi.flatMap { belt ->
            val sections = SubjectItemsResolver.resolveBySubject(
                belt = belt,
                subject = sharedSubject
            )

            sections.flatMap { section ->
                val sectionTitle = section.title // יכול להיות null

                // ✅ NEW: אכיפה של include/require/exclude/subTopicHint על כל פריט
                val filteredItems = section.items.filter { ui ->
                    val rawTitle = buildString {
                        // נותן למנוע גם את ה-canonicalId (לעיתים מכיל def:...) וגם את ה-rawItem וגם את השם לתצוגה
                        append(ui.canonicalId)
                        append("::")
                        append(ui.rawItem)
                        append("::")
                        append(ui.displayName)
                    }

                    TopicsBySubjectRegistry.run {
                        effectiveAppSubject.matchesItem(
                            itemTitle = rawTitle,
                            subTopicTitle = sectionTitle
                        )
                    }
                }

                filteredItems.map { ui ->
                    RowData(
                        belt = belt,
                        topic = ui.topicTitle,
                        rawItem = ui.rawItem,
                        displayItem = ui.displayName,
                        canonicalId = ui.canonicalId
                    )
                }
            }
        }
    }

    // ----------------- ⭐ Favorites + 🕒 Recents (Persisted) -----------------
    val prefs = remember(ctx) {
        ctx.getSharedPreferences("kmi_subject_exercises", android.content.Context.MODE_PRIVATE)
    }

    val SEP = "\u001F" // unit-separator (נדיר בטקסט)

    // ✅ לכל נושא KEY נפרד, כדי שלא יתערבבו מועדפים בין קטגוריות שונות
    // ⚠️ חשוב: אל תשתמש ב-getStringSet / putStringSet (עלול לקרוס)
    val KEY_FAVS = remember(subjectId) { "fav_ids_str__subject__$subjectId" }
    val KEY_RECENTS = remember(subjectId) { "recent_ids_str__subject__$subjectId" }

    fun loadFavs(): Set<String> {
        val raw = prefs.getString(KEY_FAVS, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(SEP).asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun saveFavs(set: Set<String>) {
        prefs.edit().putString(KEY_FAVS, set.joinToString(SEP)).apply()
    }

    fun loadRecents(): List<String> {
        val raw = prefs.getString(KEY_RECENTS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP).asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    fun saveRecents(list: List<String>) {
        prefs.edit().putString(KEY_RECENTS, list.joinToString(SEP)).apply()
    }

    var favIds by remember(subjectId) { mutableStateOf(loadFavs()) }
    var recentIds by remember(subjectId) { mutableStateOf(loadRecents()) }
    var filterMode by remember { mutableStateOf(FilterMode.ALL) }

    // ✅ ספירה נכונה לצ’יפ "מועדפים": רק מועדפים של הנושא הנוכחי
    val favoritesCountForThisSubject = remember(rows, favIds) {
        rows.count { it.canonicalId in favIds }
    }

    // ✅ מפה מהירה לפי canonicalId (עוזר לנו לסדר Recents לפי זמן)
    val rowById: Map<String, RowData> = remember(rows) {
        rows.associateBy { it.canonicalId }
    }

    // ✅ filteredRows *מחוץ* ל-LazyColumn
    val filteredRows: List<RowData> = remember(rows, favIds, recentIds, filterMode, rowById) {
        when (filterMode) {
            FilterMode.ALL -> rows
            FilterMode.FAVORITES -> rows.filter { it.canonicalId in favIds }
            FilterMode.RECENTS -> recentIds.mapNotNull { rowById[it] } // לפי זמן (אחרון ראשון)
        }
    }

    // מצב: איזה תרגיל נבחר להצגת דיאלוג הסבר
    var selectedRow by remember { mutableStateOf<RowData?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundBrush) // שומר על אותו רקע מאחורי הכל
            ) {
                il.kmi.app.ui.KmiTopBar(
                    title = screenTitleResolved, // ✅ היה: appSubject.titleHeb
                    onHome = onOpenHome,
                    showTopHome = false,
                    centerTitle = true,
                    lockSearch = true,
                    showBottomActions = true
                )

                // ✅ הפילטרים בתוך ה-topBar => ה-Scaffold מפנה להם מקום, לא נחתכים
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundBrush) // ✅ אותו רקע כמו המסך מאחורי 3 הכפתורים
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    TopFiltersBarModern(
                        filterMode = filterMode,
                        favCount = favoritesCountForThisSubject, // ✅ היה: favIds.size
                        recentCount = recentIds.size,
                        onPick = { filterMode = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->

    // ✅ התוכן מתחיל אחרי כל ה-topBar (כולל הפילטרים)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundBrush)
        ) {
    Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.97f),
                tonalElevation = 4.dp,
                shadowElevation = 10.dp
            ) {
    // ... כל הקוד שלך של rows.isEmpty / LazyColumn נשאר אותו דבר
                if (rows.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "לא נמצאו תרגילים לנושא זה.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF546E7A),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        if (filterMode == FilterMode.RECENTS) {
                            itemsIndexed(
                                items = filteredRows,
                                key = { index, row -> "recent_${index}_${row.canonicalId}" }
                            ) { _, row ->
                                ExerciseRowCardModern(
                                    belt = row.belt,
                                    topic = row.topic,
                                    item = row.displayItem,
                                    isFavorite = row.canonicalId in favIds,
                                    showMeta = true,
                                    onToggleFavorite = {
                                        val next = favIds.toMutableSet()
                                        if (row.canonicalId in next) next.remove(row.canonicalId) else next.add(row.canonicalId)
                                        favIds = next
                                        saveFavs(next)
                                    },
                                    onClick = {
                                        clickSound()
                                        haptic(true)

                                        val nextRecents = buildList {
                                            add(row.canonicalId)
                                            addAll(recentIds.filterNot { it == row.canonicalId })
                                        }.take(50)

                                        recentIds = nextRecents
                                        saveRecents(nextRecents)

                                        selectedRow = row
                                    }
                                )
                            }
                        } else {
                            val grouped = filteredRows.groupBy { it.belt }
                            val beltsToShow = beltsForUi.filter { grouped[it].orEmpty().isNotEmpty() }

                            beltsToShow.forEach { belt ->
                                val beltRows = grouped[belt].orEmpty()

                                item(key = "belt_card_${belt.id}") {
                                    BeltSectionCardModern(
                                        belt = belt,
                                        count = beltRows.size
                                    ) {
                                        beltRows.forEach { row ->
                                            ExerciseRowCardModern(
                                                belt = belt,
                                                topic = row.topic,
                                                item = row.displayItem,
                                                isFavorite = row.canonicalId in favIds,
                                                showMeta = true,
                                                onToggleFavorite = {
                                                    val next = favIds.toMutableSet()
                                                    if (row.canonicalId in next) next.remove(row.canonicalId) else next.add(row.canonicalId)
                                                    favIds = next
                                                    saveFavs(next)
                                                },
                                                onClick = {
                                                    clickSound()
                                                    haptic(true)

                                                    val nextRecents = buildList {
                                                        add(row.canonicalId)
                                                        addAll(recentIds.filterNot { it == row.canonicalId })
                                                    }.take(50)

                                                    recentIds = nextRecents
                                                    saveRecents(nextRecents)

                                                    selectedRow = row
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ✅ דיאלוג נשאר אותו דבר (הוא מתחת ל-Column, אין שינוי)
            selectedRow?.let { row ->
    val isFavorite = row.canonicalId in favIds

                val explanation = remember(row.canonicalId) {
                    val raw = Explanations.get(row.belt, row.rawItem).trim()
                    if (raw.isBlank()) {
                        "אין כרגע הסבר לתרגיל הזה."
                    } else {
                        if ("::" in raw) raw.substringAfter("::").trim() else raw
                    }
                }

                AlertDialog(
                    onDismissRequest = { selectedRow = null },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = row.displayItem,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start
                                )
                            }

                            Spacer(Modifier.width(4.dp))

                            val context = LocalContext.current

                            IconButton(
                                onClick = {
                                    KmiTtsManager.init(context)
                                    KmiTtsManager.stop()
                                    KmiTtsManager.speak(explanation)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "השמע הסבר קולי",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    val next = favIds.toMutableSet()

                                    if (row.canonicalId in next) {
                                        next.remove(row.canonicalId)
                                    } else {
                                        next.add(row.canonicalId)
                                    }

                                    favIds = next.toSet()
                                    saveFavs(favIds)

                                    // אם אנחנו בפילטר "מועדפים" והסרנו מועדף — נסגור דיאלוג כדי למנוע מצב UI מוזר
                                    if (filterMode == FilterMode.FAVORITES && row.canonicalId !in favIds) {
                                        selectedRow = null
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (isFavorite) "הסר מהמועדפים" else "הוסף למועדפים",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    text = {
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedRow = null }) {
                            Text("סגור")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TopFiltersBarModern(
    filterMode: FilterMode,
    favCount: Int,
    recentCount: Int,
    onPick: (FilterMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0x14000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            FilterChipModern(
                text = "הכל",
                selected = filterMode == FilterMode.ALL,
                onClick = { onPick(FilterMode.ALL) },
                modifier = Modifier.weight(1f)
            )

            FilterChipModern(
                text = "מועדפים ($favCount)\n⭐",
                selected = filterMode == FilterMode.FAVORITES,
                onClick = { onPick(FilterMode.FAVORITES) },
                modifier = Modifier.weight(1f)
            )

            FilterChipModern(
                text = "אחרונים ($recentCount)\n🕒",
                selected = filterMode == FilterMode.RECENTS,
                onClick = { onPick(FilterMode.RECENTS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** ✅ זה מה שחסר לך ולכן יש Unresolved reference */
@Composable
private fun FilterChipModern(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant

    val border =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else Color(0x16000000)

    Surface(
        modifier = modifier
            .height(56.dp), // ✅ גובה קבוע – מונע "התמתחות" מטורפת ב-topBar
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth() // ✅ במקום fillMaxSize() שגרם לכפתור למלא גובה ענק
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ───────── כותרת חגורה בתוך המסך ───────── */

@Composable
private fun BeltHeaderRow(
    belt: Belt,
    count: Int
) {
    val onBelt = if (belt.color.luminance() < 0.5f) Color.White else Color.Black

    val cleanName = remember(belt.heb) {
        val s = belt.heb.trim()
        if (s.startsWith("חגורה")) s.removePrefix("חגורה").trim() else s
    }

    val titleText = "חגורה $cleanName - $count תרגילים"

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = belt.color,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = titleText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 14.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = onBelt
        )
    }
}

@Composable
private fun BeltSectionCardModern(
    belt: Belt,
    count: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    val onBelt = if (belt.color.luminance() < 0.5f) Color.White else Color.Black

    val cleanName = remember(belt.heb) {
        val s = belt.heb.trim()
        if (s.startsWith("חגורה")) s.removePrefix("חגורה").trim() else s
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0x12000000))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header צבעוני של החגורה
            Surface(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = belt.color
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "חגורה $cleanName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onBelt,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "$count תרגילים",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = onBelt.copy(alpha = 0.95f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

/* ───────── שורת תרגיל ───────── */

@Composable
private fun ExerciseRowCardModern( // ✅ RENAME (was ExerciseRowCard)
    belt: Belt,
    topic: String,
    item: String,
    isFavorite: Boolean,
    showMeta: Boolean = false,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, belt.color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(belt.color.copy(alpha = 0.9f))
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showMeta) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = topic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "הסר ממועדפים" else "הוסף למועדפים",
                    tint = if (isFavorite) Color(0xFFFFC107)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }
    }
}
