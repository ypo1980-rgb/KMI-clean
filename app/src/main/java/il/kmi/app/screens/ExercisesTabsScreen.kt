@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package il.kmi.app.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.Explanations
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.collectAsState
import il.kmi.app.domain.CanonicalIds
import il.kmi.app.favorites.FavoritesStore
import il.kmi.app.domain.ContentRepo
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import il.kmi.app.ui.ext.color
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

//==============================================================================

@Composable
fun ExercisesTabsScreen(
    vm: KmiViewModel,
    belt: Belt,
    topic: String,
    onPractice: (Belt, String) -> Unit,
    subTopicFilter: String? = null,
    onHome: () -> Unit = {},
    onSearch: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val scroll = rememberScrollState()
    val sp = remember { ctx.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }
    val notesSp = remember { ctx.getSharedPreferences("kmi_notes", android.content.Context.MODE_PRIVATE) }
// ⭐ Favorites גלובלי – source of truth אחד לכל האפליקציה
    val favorites: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    // ✅ רענון סימוני יודע/לא יודע שהגיעו ממסכים אחרים, כולל MaterialsScreen
    val marksVersion by vm.marksVersion.collectAsState()

    fun readSet(key: String): MutableSet<String> =
        sp.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()

    val allUnknownKeys = remember(belt.id, marksVersion) {
        sp.all.keys.filter { it.startsWith("unknown_${belt.id}_") }
    }

     // --- item list כמו ב-MaterialsScreen ---
    data class TopicItems(val topic: String, val items: Set<String>)

    // ✅ Source of truth דרך ContentRepo
    fun String.normTitle(): String = this
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .replace('־', '-')
        .replace('–', '-')
        .trim()
        .lowercase()

    fun dec(s: String) =
        try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

    fun contentItemsForTopicIncludingSubTopics(
        belt: Belt,
        topicTitle: String
    ): List<String> {
        val directItems = ContentRepo.listItemTitles(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = null
        )

        val subTopicItems = ContentRepo
            .listSubTopicTitles(belt, topicTitle)
            .flatMap { subTitle ->
                ContentRepo.listItemTitles(
                    belt = belt,
                    topicTitle = topicTitle,
                    subTopicTitle = subTitle
                )
            }

        return (directItems + subTopicItems)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val allTopicItems: List<TopicItems> = remember(belt, topic) {
        if (topic != "__ALL__") return@remember emptyList()

        val topicTitles = ContentRepo.listTopicTitles(belt)

        topicTitles.mapNotNull { tpTitle ->
            val title = tpTitle.trim()
            if (title.isBlank()) return@mapNotNull null

            val items = contentItemsForTopicIncludingSubTopics(
                belt = belt,
                topicTitle = title
            ).toSet()

            if (items.isEmpty()) return@mapNotNull null

            TopicItems(title, items)
        }
    }

    val itemList: List<String> = remember(belt, topic, subTopicFilter, allTopicItems) {

        if (topic == "__ALL__") {
            return@remember allTopicItems.flatMap { it.items }.distinct()
        }

        // אם יש סינון תת-נושא
        subTopicFilter?.takeIf { it.isNotBlank() }?.let { raw ->
            val subRaw = dec(raw)

            // 1) subTopic match (exact/loose) מתוך Bridge
            val subTitles = ContentRepo.listSubTopicTitles(belt, topic)
            val exact = subTitles.firstOrNull { it.normTitle() == subRaw.normTitle() }
            if (exact != null) {
                val items = ContentRepo.listItemTitles(belt, topic, subTopicTitle = exact)
                if (items.isNotEmpty()) return@remember items
            }

            val wanted = subRaw.normTitle()
            val loose = subTitles.firstOrNull { st ->
                val a = st.normTitle()
                a.startsWith(wanted) || wanted.startsWith(a) || a.contains(wanted) || wanted.contains(a)
            }
            if (loose != null) {
                val items = ContentRepo.listItemTitles(belt, topic, subTopicTitle = loose)
                if (items.isNotEmpty()) return@remember items
            }

            // 2) fallback: KmiSearchBridge (רק אם עדיין קיים אצלך)
            val bySubBridge = runCatching { il.kmi.app.search.KmiSearchBridge.itemsFor(belt, subRaw) }
                .getOrDefault(emptyList())
            if (bySubBridge.isNotEmpty()) return@remember bySubBridge

            return@remember emptyList()
        }

        // ללא סינון תת-נושא: כל הפריטים של הנושא, כולל תתי־נושאים
        val byTopic = contentItemsForTopicIncludingSubTopics(
            belt = belt,
            topicTitle = topic
        )

        if (byTopic.isNotEmpty()) return@remember byTopic

        // fallback: bridge לפי נושא
        val byTopicBridge = runCatching {
            il.kmi.app.search.KmiSearchBridge.itemsFor(belt, topic)
        }.getOrDefault(emptyList())

        if (byTopicBridge.isNotEmpty()) return@remember byTopicBridge

        emptyList()
    }

    // === KMI_SEARCH_INJECT: STATE — MUST BE ABOVE Scaffold ===
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var explainFromSearch by remember { mutableStateOf<String?>(null) }
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }   // ← תרגיל שנבחר מחיפוש כללי
    var noteEditorFor by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var notesRefreshKey by rememberSaveable { mutableIntStateOf(0) }
    // --- מצב טאבים (0=הכל, 1=לא יודע, 2=מועדפים) — חייב להיות לפני ה-Scaffold ---
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    fun String.norm() = this
        .replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .trim().lowercase()

    val searchResults by remember(searchQuery, itemList) {
        val q = searchQuery.norm()
        mutableStateOf(
            if (q.isBlank()) emptyList()
            else itemList.filter { it.norm().contains(q) }
        )
    }

    // מזהה תרגיל "אחיד" – בלי prefix של נושא וכו'
    fun normalizeItemId(raw: String): String =
        raw.substringAfter("::", raw)
            .substringAfter(":", raw)
            .trim()

    fun normalizeStatusPart(raw: String): String =
        raw.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun noteKeyFor(raw: String): String = "note_${belt.id}_${normalizeItemId(raw)}"

    fun loadNote(raw: String): String =
        notesSp.getString(noteKeyFor(raw), "")?.trim().orEmpty()

    fun saveNote(raw: String, value: String) {
        val clean = value.trim()

        notesSp.edit().apply {
            if (clean.isBlank()) {
                remove(noteKeyFor(raw))
            } else {
                putString(noteKeyFor(raw), clean)
            }
        }.apply()

        notesRefreshKey++
    }

    fun deleteNote(raw: String) {
        notesSp.edit()
            .remove(noteKeyFor(raw))
            .apply()

        notesRefreshKey++
    }

    fun hasNote(raw: String): Boolean {
        notesRefreshKey
        return loadNote(raw).isNotBlank()
    }

    // סטטוסים מה-VM
    val itemStates = remember(belt.id, topic, subTopicFilter) { mutableStateMapOf<String, Boolean?>() }
    // ✅ אם זה __ALL__ צריך לדעת לאיזה נושא שייך כל item כדי לקרוא סטטוס נכון מה-VM
    fun topicForRawItem(raw: String): String {
        if (topic != "__ALL__") return topic
        return allTopicItems.firstOrNull { it.items.contains(raw) }?.topic ?: topic
    }

    LaunchedEffect(belt, topic, subTopicFilter, itemList, allTopicItems) {
        itemStates.clear()

        itemList.forEach { raw ->
            val tp = topicForRawItem(raw)

            // ✅ קריטי: משתמשים באותו canonicalId שהשאר האפליקציה שומרת
            val canonicalId = CanonicalIds.canonicalFor(belt, tp, raw)

            val v = runCatching { vm.getItemStatusNullable(belt, tp, canonicalId) }.getOrNull()
                ?: runCatching { if (vm.isMastered(belt, tp, canonicalId)) true else null }.getOrNull()

            itemStates[raw] = v
        }
    }

// ========= ⭐ / X =========
    val suffix = remember(topic, subTopicFilter) {
        if (subTopicFilter.isNullOrBlank()) topic else "${topic}__${subTopicFilter}"
    }


    var unknowns by remember(belt.id, topic, suffix, allUnknownKeys, marksVersion) {
        mutableStateOf(
            if (topic == "__ALL__") {
                allUnknownKeys
                    .flatMap { key -> readSet(key) }
                    .toMutableSet()
            } else {
                readSet("unknown_${belt.id}_$suffix")
                    .toMutableSet()
            }
        )
    }

    fun unknownCandidateIdsFor(raw: String): Set<String> {
        val tp = topicForRawItem(raw)
        val cleanId = normalizeItemId(raw)
        val displayName = ExerciseTitleFormatter.displayName(raw)
            .ifBlank { raw.trim() }
            .trim()

        val canonicalFromRaw = CanonicalIds.canonicalFor(belt, tp, raw)
        val canonicalFromClean = CanonicalIds.canonicalFor(belt, tp, cleanId)
        val canonicalFromDisplay = CanonicalIds.canonicalFor(belt, tp, displayName)

        return buildSet {
            add(raw.trim())
            add(cleanId)
            add(displayName)
            add(canonicalFromRaw)
            add(canonicalFromClean)
            add(canonicalFromDisplay)
        }.filter { it.isNotBlank() }.toSet()
    }

    fun isUnknownRawItem(raw: String): Boolean {
        val candidates = unknownCandidateIdsFor(raw)
        val cleanRaw = normalizeStatusPart(raw)
        val cleanDisplay = normalizeStatusPart(
            ExerciseTitleFormatter.displayName(raw).ifBlank { raw.trim() }
        )

        return unknowns.any { storedRaw ->
            val stored = storedRaw.trim()
            val storedNormalized = normalizeItemId(stored)

            stored in candidates ||
                    storedNormalized in candidates ||
                    candidates.contains(storedNormalized) ||
                    (
                            stored.startsWith("status_${belt.id}_") &&
                                    (
                                            stored.endsWith("_$cleanRaw") ||
                                                    stored.endsWith("_$cleanDisplay") ||
                                                    stored.contains(cleanRaw) ||
                                                    stored.contains(cleanDisplay)
                                            )
                            )
        }
    }

    fun toggleFavorite(rawId: String) {
        FavoritesStore.toggle(normalizeItemId(rawId))
    }

    /**
     * סימון/הסרה ממועדפים
     */
      /**
     * סימון/הסרה "לא יודע"
     */
      fun setUnknown(id: String, set: Boolean) {
          val cleanId = normalizeItemId(id)

          fun removeMatchingUnknowns(
              setToClean: MutableSet<String>,
              raw: String,
              canonicalId: String
          ) {
              val cleanRaw = normalizeStatusPart(raw)
              val cleanDisplay = normalizeStatusPart(
                  ExerciseTitleFormatter.displayName(raw).ifBlank { raw.trim() }
              )

              setToClean.remove(cleanId)
              setToClean.remove(raw)
              setToClean.remove(canonicalId)

              setToClean.removeAll { stored ->
                  stored.trim() == cleanId ||
                          stored.trim() == raw.trim() ||
                          stored.trim() == canonicalId ||
                          (
                                  stored.startsWith("status_${belt.id}_") &&
                                          (
                                                  stored.endsWith("_$cleanRaw") ||
                                                          stored.endsWith("_$cleanDisplay") ||
                                                          stored.contains(cleanRaw) ||
                                                          stored.contains(cleanDisplay)
                                                  )
                                  )
              }
          }

          if (topic == "__ALL__") {
              val nextUnknowns = unknowns.toMutableSet()

              allTopicItems.forEach { ti ->
                  val matchedItems = ti.items.filter { raw ->
                      normalizeItemId(raw) == cleanId ||
                              raw.trim() == id.trim() ||
                              CanonicalIds.canonicalFor(belt, ti.topic, raw) == id.trim()
                  }

                  matchedItems.forEach { raw ->
                      val canonicalId = CanonicalIds.canonicalFor(belt, ti.topic, raw)
                      val key = "unknown_${belt.id}_${ti.topic}"

                      val s = readSet(key)

                      if (set) {
                          s.add(cleanId)
                          s.add(canonicalId)

                          nextUnknowns.add(cleanId)
                          nextUnknowns.add(canonicalId)

                          vm.setItemStatusNullable(
                              belt = belt,
                              topic = ti.topic,
                              item = canonicalId,
                              value = false
                          )
                      } else {
                          removeMatchingUnknowns(s, raw, canonicalId)
                          removeMatchingUnknowns(nextUnknowns, raw, canonicalId)

                          vm.setItemStatusNullable(
                              belt = belt,
                              topic = ti.topic,
                              item = canonicalId,
                              value = null
                          )
                      }

                      sp.edit()
                          .putStringSet(key, s)
                          .apply()
                  }
              }

              unknowns = nextUnknowns
          } else {
              val tp = topicForRawItem(id)
              val canonicalId = CanonicalIds.canonicalFor(belt, tp, id)
              val key = "unknown_${belt.id}_$suffix"

              val s = readSet(key)

              if (set) {
                  s.add(cleanId)
                  s.add(canonicalId)

                  vm.setItemStatusNullable(
                      belt = belt,
                      topic = tp,
                      item = canonicalId,
                      value = false
                  )
              } else {
                  removeMatchingUnknowns(s, id, canonicalId)

                  vm.setItemStatusNullable(
                      belt = belt,
                      topic = tp,
                      item = canonicalId,
                      value = null
                  )
              }

              unknowns = s.toMutableSet()

              sp.edit()
                  .putStringSet(key, s)
                  .apply()
          }
      }

    Scaffold(
        topBar = {
            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            il.kmi.app.ui.KmiTopBar(
                title = tr("כרטיסיות התרגילים", "Exercise Cards"),
                onHome = onHome,                      // חזרה לבית שעובד ✅
                centerTitle = true,
                showTopHome = false,
                lockSearch = false,                  // חיפוש פעיל ✅
                onPickSearchResult = { key -> pickedKey = key }, // חיבור חיפוש מדויק
                extraActions = { },                  // אם אין אקשנים נוספים
                currentLang = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",
                onToggleLanguage = {
                    val newLang =
                        if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    (contextLang as? Activity)?.recreate()
                }
            )
        },

        bottomBar = {
            Surface(
                color = Color(0xFFE0E0E0),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {                    // ← קובע את מקור התרגול לפי הטאב: הכל/לא-יודע/מועדפים
                    val practiceToken = when (selectedTab) {
                        1 -> "__UNKNOWN__"
                        2 -> "__FAVS_ALL__"
                        else -> {
                            // ✅ אם נכנסנו דרך נושא מסוים — מתרגלים את הנושא הזה
                            if (topic != "__ALL__") topic else "__ALL__"
                        }
                    }

                    ActionButton(
                        text = tr("תרגול", "Practice"),
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFF6F64FF),
                        onClick = { onPractice(belt, practiceToken) }
                    )
                    ActionButton(
                        text = tr("איפוס", "Reset"),
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFFD32F2F),
                        onClick = {
                            scope.launch {
                                // איפוס סטטוסים בזיכרון הקומפוז
                                itemList.forEach { item -> itemStates[item] = null }

                                // ⭐ איפוס מועדפים גלובלי
                                FavoritesStore.clearAll()

                                // ❓ איפוס unknown – נשאר מקומי לפי חגורה/נושא
                                unknowns = mutableSetOf()

                                val editor = sp.edit()

                                if (topic == "__ALL__") {
                                    // ✅ 1) מחיקת unknown keys מה-SP (כמו שהיה)
                                    sp.all.keys
                                        .filter { it.startsWith("unknown_${belt.id}_") }
                                        .forEach { key -> editor.remove(key) }

                                    // ✅ 2) איפוס אמיתי של הסימונים (DataStore) לכל נושא
                                    allTopicItems.forEach { ti ->
                                        val canonicalIds = ti.items
                                            .map { raw -> CanonicalIds.canonicalFor(belt, ti.topic, raw) }
                                            .distinct()

                                        vm.clearTopicItems(
                                            belt = belt,
                                            topic = ti.topic,
                                            canonicalIds = canonicalIds
                                        )
                                    }
                                } else {
                                    val singleUnknownKey = "unknown_${belt.id}_$suffix"
                                    editor.remove(singleUnknownKey)

                                    vm.clearTopic(belt, topic)
                                }

                                editor.apply()
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->

        // פונקציות עזר להסבר דרך החיפוש הכללי
        fun parseSearchKey(key: String): Triple<Belt, String, String> {
            fun dec(s: String): String =
                runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

            val parts0 = when {
                '|' in key  -> key.split('|', limit = 3)
                "::" in key -> key.split("::", limit = 3)
                '/' in key  -> key.split('/', limit = 3)
                else        -> listOf("", "", "")
            }
            val parts = (parts0 + listOf("", "", "")).take(3)
            val beltFromKey  = Belt.fromId(parts[0]) ?: belt
            val topicFromKey = dec(parts[1])
            val itemFromKey  = dec(parts[2])
            return Triple(beltFromKey, topicFromKey, itemFromKey)
        }

        fun findExplanationForHit(
            beltHit: Belt,
            rawItem: String,
            topicHit: String
        ): String {
            val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

            fun String.clean(): String = this
                .replace('–', '-')    // en dash
                .replace('־', '-')    // maqaf
                .replace("  ", " ")
                .trim()

            val candidates = buildList {
                add(rawItem)
                add(display)
                add(display.clean())
                add(display.substringBefore("(").trim().clean())
            }.distinct()

            for (candidate in candidates) {
                val got = Explanations.get(beltHit, candidate).trim()
                if (got.isNotBlank()
                    && !got.startsWith("הסבר מפורט על")
                    && !got.startsWith("אין כרגע")
                ) {
                    return if ("::" in got) got.substringAfter("::").trim() else got
                }
            }
            return tr("אין כרגע הסבר לתרגיל הזה.", "There is currently no explanation for this exercise.")
        }

        // ===== טאבים "מקצה-לקצה" =====
        @Composable
        fun MetricFieldEdgeToEdge(
            title: String,
            number: Int,
            selected: Boolean,
            onClick: () -> Unit,
            modifier: Modifier = Modifier
        ) {
            val baseBg    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            val selBg     = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            val borderCol = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
            else MaterialTheme.colorScheme.outlineVariant

            Box(
                modifier = modifier
                    .height(64.dp)
                    .background(if (selected) selBg else baseBg, shape = RectangleShape)
                    .border(1.dp, borderCol, RectangleShape)
                    .clickable(onClick = onClick)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = number.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        val allCount     = itemList.size
        // ✅ תומך גם ב-cleanId, גם ב-canonicalId וגם ב-statusId שמגיע מ-MaterialsScreen
        val unknownCount = itemList.count { isUnknownRawItem(it) }
        val favCount     = itemList.count { normalizeItemId(it) in favorites }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricFieldEdgeToEdge(
                    title    = tr("הכל", "All"),
                    number   = allCount,
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                MetricFieldEdgeToEdge(
                    title    = tr("לא יודע", "Unknown"),
                    number   = unknownCount,
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
                MetricFieldEdgeToEdge(
                    title    = tr("מועדפים", "Favorites"),
                    number   = favCount,
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            val filtered = when (selectedTab) {
                1 -> itemList.filter { isUnknownRawItem(it) }
                2 -> itemList.filter { normalizeItemId(it) in favorites }
                else -> itemList
            }

// ✅ מפת “raw -> display” אחת, שמשמשת לכל ה-UI
            val displayByRaw = remember(filtered) {
                filtered.associateWith { raw ->
                    ExerciseTitleFormatter.displayName(raw).ifBlank { raw.trim() }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filtered.forEach { item ->
                    val bringer = remember { BringIntoViewRequester() }
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (pressed) 1.15f else 1f, label = "scale")

                    val tpForUi = topicForRawItem(item)
                    val displayName = CanonicalIds.uiDisplayName(tpForUi, item)
                    val isFav = favorites.contains(normalizeItemId(item))
                    val itemHasNote = remember(item, notesRefreshKey) {
                        hasNote(item)
                    }

                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .bringIntoViewRequester(bringer),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExerciseRowActionsMenu(
                                isEnglish = isEnglish,
                                isFav = isFav,
                                hasNote = itemHasNote,
                                isUnknown = isUnknownRawItem(item),
                                onInfo = {
                                    pressed = true
                                    explainFromSearch = item
                                    scope.launch {
                                        kotlinx.coroutines.delay(150)
                                        pressed = false
                                    }
                                },
                                onToggleFavorite = {
                                    FavoritesStore.toggle(item)
                                },
                                onEditNote = {
                                    noteEditorFor = item
                                    noteDraft = loadNote(item)
                                },
                                onToggleUnknown = {
                                    setUnknown(item, !isUnknownRawItem(item))
                                },
                                modifier = Modifier.scale(scale)
                            )

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { explainFromSearch = item },
                                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                            ) {
                                Text(
                                    text = displayName,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                if (itemHasNote) {
                                    Text(
                                        text = tr("יש הערה שמורה", "Saved note exists"),
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
                                    )
                                }
                            }
                        }
                    }

                    Divider()
                }
            }
        }

        // ===== דיאלוג הסבר מתוצאת חיפוש כללית (אייקון זכוכית מגדלת) =====
        pickedKey?.let { key ->
            val (hitBelt, hitTopic, hitItem) = parseSearchKey(key)
            val displayName = ExerciseTitleFormatter.displayName(hitItem)
                .ifBlank { hitItem.trim() }

            val explanation = remember(hitBelt, hitItem, hitTopic, isEnglish) {
                val raw = findExplanationForHit(hitBelt, hitItem, hitTopic)
                if (raw == "NO_EXPLANATION") {
                    tr("אין כרגע הסבר לתרגיל הזה.", "There is currently no explanation for this exercise.")
                } else {
                    raw
                }
            }

            val isFav = favorites.contains(normalizeItemId(hitItem))
            val noteText = remember(hitItem, notesRefreshKey) {
                loadNote(hitItem)
            }

            ExerciseExplanationDialog(
                title = displayName,
                beltLabel = if (isEnglish) {
                    "$hitTopic • ${hitBelt.en}"
                } else {
                    "$hitTopic • ${hitBelt.heb}"
                },
                explanation = explanation,
                noteText = noteText,
                isFavorite = isFav,
                accentColor = hitBelt.color,
                isEnglish = isEnglish,
                onDismiss = { pickedKey = null },
                onEditNote = {
                    noteEditorFor = hitItem
                    noteDraft = loadNote(hitItem)
                },
                onDeleteNote = {
                    deleteNote(hitItem)
                },
                onToggleFavorite = {
                    toggleFavorite(hitItem)
                }
            )
        }

        // ===== דיאלוג הסבר (לחיצה על שורה או אייקון info ברשימה) =====
        explainFromSearch?.let { item ->

            val displayName = ExerciseTitleFormatter.displayName(item)
                .ifBlank { item.trim() }

            LaunchedEffect(item) {
                KmiTtsManager.init(ctx)
            }
            DisposableEffect(item) {
                onDispose { KmiTtsManager.stop() }
            }

            val explanation = Explanations.get(belt, displayName)
                .ifBlank { Explanations.get(belt, item) }
                .ifBlank { tr("לא נמצא הסבר עבור \"$displayName\".", "No explanation found for \"$displayName\".") }

            val isFav = favorites.contains(normalizeItemId(item))
            val noteText = remember(item, notesRefreshKey) {
                loadNote(item)
            }

            ExerciseExplanationDialog(
                title = displayName,
                beltLabel = if (isEnglish) "(${belt.en})" else "(${belt.heb})",
                explanation = explanation,
                noteText = noteText,
                isFavorite = isFav,
                accentColor = belt.color,
                isEnglish = isEnglish,
                onDismiss = {
                    KmiTtsManager.stop()
                    explainFromSearch = null
                },
                onEditNote = {
                    noteEditorFor = item
                    noteDraft = loadNote(item)
                },
                onDeleteNote = {
                    deleteNote(item)
                },
                onToggleFavorite = {
                    toggleFavorite(item)
                }
            )
        }

        noteEditorFor?.let { item ->
            ExerciseNoteEditorDialog(
                noteText = noteDraft,
                isEnglish = isEnglish,
                accentColor = belt.color,
                onNoteChange = { noteDraft = it },
                onDismiss = {
                    noteEditorFor = null
                },
                onSave = {
                    saveNote(item, noteDraft)
                    noteEditorFor = null
                }
            )
        }

    } // ✅ סוגר את Scaffold { padding -> ... }

} // ✅ סוגר את ExercisesTabsScreen(...)


@Composable
private fun ExerciseRowActionsMenu(
    isEnglish: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    isUnknown: Boolean,
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNote: () -> Unit,
    onToggleUnknown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val infoScale by animateFloatAsState(
        targetValue = if (expanded) 1.08f else 1f,
        animationSpec = tween(180),
        label = "exerciseInfoScale"
    )

    val infoRotation by animateFloatAsState(
        targetValue = if (expanded) 12f else 0f,
        animationSpec = tween(180),
        label = "exerciseInfoRotation"
    )

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            ),
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = infoScale
                    scaleY = infoScale
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = tr("פעולות לתרגיל", "Exercise actions"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            rotationZ = infoRotation
                        }
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.99f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.97f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(18.dp)
                )
        ) {
            DropdownMenuItem(
                text = { Text(tr("מידע", "Info"), style = MaterialTheme.typography.labelLarge) },
                onClick = {
                    expanded = false
                    onInfo()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        tr(
                            if (isFav) "הסר ממועדפים" else "הוסף למועדפים",
                            if (isFav) "Remove from favorites" else "Add to favorites"
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                onClick = {
                    expanded = false
                    onToggleFavorite()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        tr(
                            if (hasNote) "ערוך / מחק הערה" else "הוסף הערה לתרגיל",
                            if (hasNote) "Edit / delete note" else "Add note"
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                onClick = {
                    expanded = false
                    onEditNote()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        tr(
                            if (isUnknown) "בטל לא יודע" else "סמן כלא יודע",
                            if (isUnknown) "Remove unknown mark" else "Mark as unknown"
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                onClick = {
                    expanded = false
                    onToggleUnknown()
                }
            )
        }
    }
}

// ========= כפתור מונפש לשימוש חוזר =========
@Composable
fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "btnScale")
    val scope = rememberCoroutineScope()
    val contentOnContainer = if (containerColor.luminance() < 0.5f) Color.White else Color.Black

    Button(
        onClick = {
            pressed = true; onClick()
            scope.launch { kotlinx.coroutines.delay(150); pressed = false }
        },
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.scale(scale).height(56.dp).defaultMinSize(minWidth = 90.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentOnContainer)
    ) {
        Text(text, color = contentOnContainer, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

// ========= עזר: הדגשת "עמידת מוצא ..." עד פסיק/נקודה =========
private fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val marker = "עמידת מוצא"

    // אם אין בכלל "עמידת מוצא" – מחזירים טקסט רגיל
    val idx = source.indexOf(marker)
    if (idx < 0) return AnnotatedString(source)

    // מחפשים סוף משפט: פסיק או נקודה אחרי "עמידת מוצא"
    val sentenceEndExclusive = run {
        val endIdx = source.indexOfAny(charArrayOf('.', ','), startIndex = idx)
        if (endIdx == -1) source.length else endIdx + 1   // כולל הפסיק/נקודה
    }

    val before = source.substring(0, idx)
    val stanceSentence = source.substring(idx, sentenceEndExclusive)
    val after = source.substring(sentenceEndExclusive)

    return buildAnnotatedString {
        // מה שלפני
        append(before)

        // המשפט של "עמידת מוצא ..." מודגש וצבוע
        val stanceStart = length
        append(stanceSentence)
        val stanceEnd = length

        addStyle(
            style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = stanceColor
            ),
            start = stanceStart,
            end = stanceEnd
        )

        // שאר ההסבר
        append(after)
    }
}
