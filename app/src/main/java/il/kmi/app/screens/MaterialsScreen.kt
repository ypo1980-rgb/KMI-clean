package il.kmi.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.material3.Divider
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import il.kmi.app.ui.ext.lightColor
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import il.kmi.app.domain.CanonicalIds
import il.kmi.app.highlightItem
import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import il.kmi.app.ui.color
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.domain.content.ExerciseTitlesEn

//=================================================================================

@Composable
private fun BeltPill(
    belt: Belt,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.surface
    val stroke = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    fun beltDrawableRes(b: Belt): Int = when (b) {
        Belt.WHITE  -> R.drawable.belt_white
        Belt.YELLOW -> R.drawable.belt_yellow
        Belt.ORANGE -> R.drawable.belt_orange
        Belt.GREEN  -> R.drawable.belt_green
        Belt.BLUE   -> R.drawable.belt_blue
        Belt.BROWN  -> R.drawable.belt_brown
        Belt.BLACK  -> R.drawable.belt_black
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, stroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = beltDrawableRes(belt)),
                contentDescription = "חגורה ${belt.heb}",
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun premiumSurfaceGradientForBelt(belt: Belt): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.98f),
            belt.color.copy(alpha = 0.10f),
            Color.White.copy(alpha = 0.94f)
        )
    )
}

@Composable
private fun topicTitleForUi(title: String, lang: AppLanguage): String {
    val clean = title.trim()
    return if (lang == AppLanguage.ENGLISH) {
        ExerciseTitlesEn.getOrSame(clean)
    } else {
        clean
    }
}

private fun itemTitleForUi(topic: String, rawItem: String, lang: AppLanguage): String {
    val display = CanonicalIds.uiDisplayName(topic, rawItem).trim()
    return if (lang == AppLanguage.ENGLISH) {
        ExerciseTitlesEn.getOrSame(display)
    } else {
        display
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MaterialsScreen(
    vm: KmiViewModel,
    belt: Belt,
    topic: String,
    onBack: () -> Unit,
    // היה: onSummary: (Belt) -> Unit,
    onSummary: (Belt, String, String?) -> Unit,
    onPractice: (Belt, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHome: () -> Unit,
    subTopicFilter: String? = null
) {

    val context = LocalContext.current
    val langManager = remember { AppLanguageManager(context) }
    val currentLang = langManager.getCurrentLanguage()
    val isEnglish = currentLang == AppLanguage.ENGLISH

    val sp = remember { context.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val itemStates = remember(belt.id, topic, subTopicFilter) { mutableStateMapOf<String, Boolean?>() }

    var explainTriple by remember { mutableStateOf<Triple<Belt, String, String>?>(null) }
    var noteEditorFor by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }

    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val explanationTextColor = if (isDarkSurface) Color.White else Color.Black

    // ✅ NEW: נושא לתצוגה/קאנוניקליזציה — כדי ש"" יתנהג בדיוק כמו "כללי"
    val topicUi = remember(topic) { if (topic.isBlank()) "כללי" else topic }
    // ✅ NEW: נושא ל-VM/DataStore (ה-VM ממילא יהפוך "כללי" ל-"")
    val vmTopic = topicUi

    // ===== canonical (✅ מקור אמת אחד לכל האפליקציה) =====
    fun canonicalFor(displayItem: String): String =
        CanonicalIds.canonicalFor(belt, topicUi, displayItem)

    fun canonicalFor(topicTitle: String, displayItem: String): String =
        CanonicalIds.canonicalFor(belt, topicTitle, displayItem)

    fun cleanItem(topicTitle: String, item: String): String =
        CanonicalIds.cleanItem(topicTitle, item)
    // ===== סוף canonical =====

    val handlePickFromTopBar: (String) -> Unit = { key ->
        fun dec(s: String) = try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

        val r = runCatching { il.kmi.app.domain.ContentRepo.resolveItemKey(key) }.getOrNull()
        if (r != null) {
            explainTriple = Triple(r.belt, r.topicTitle, r.itemTitle)
        } else {
            val parts = when {
                '|'  in key -> key.split('|',  limit = 3)
                "::" in key -> key.split("::", limit = 3)
                '/'  in key -> key.split('/',  limit = 3)
                else        -> listOf("", "", "")
            }.map(::dec)

            val beltFromKey = Belt.fromId(parts.getOrNull(0).orEmpty()) ?: belt
            val topicFromKey = parts.getOrNull(1).orEmpty().ifBlank { topicUi }
            val itemRaw = cleanItem(topicFromKey, parts.getOrNull(2).orEmpty())
            explainTriple = Triple(beltFromKey, topicFromKey, itemRaw)
        }
    }

    // === שליפת התרגילים (כולל subTopicFilter) ===
    // ✅ Cache בזיכרון כדי שמעבר בין נושאים שכבר נפתחו יהיה מיידי
    val itemsCache = rememberSaveable { mutableMapOf<String, List<String>>() }
    fun itemsCacheKey(): String = buildString {
        append(belt.id)
        append("||")
        append(topic.trim())
        append("||")
        append(subTopicFilter?.trim().orEmpty())
    }

    val itemList by produceState<List<String>>(
        initialValue = itemsCache[itemsCacheKey()] ?: emptyList(),
        key1 = belt.id,
        key2 = topic,
        key3 = subTopicFilter
    ) {
        fun dec(s: String) = try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

        val key = itemsCacheKey()

        // אם כבר בקאש — חוזרים מיד בלי חישוב
        itemsCache[key]?.let {
            value = it
            return@produceState
        }

        value = withContext(Dispatchers.Default) {
            val topicTrim = topic.trim()

            val subTrim = subTopicFilter
                ?.takeIf { it.isNotBlank() }
                ?.let { raw -> dec(raw).trim() }

            val list = if (subTrim != null) {
                SharedContentRepo.getAllItemsFor(
                    belt = belt,
                    topicTitle = topicTrim,
                    subTopicTitle = subTrim
                )
            } else {
                SharedContentRepo.getAllItemsFor(
                    belt = belt,
                    topicTitle = topicTrim,
                    subTopicTitle = null
                )
            }

            list.distinct()
        }

        itemsCache[key] = value
    }

    // הדגשת תרגיל (✅ בלי Reflection: זה top-level flow)
    val highlight by highlightItem.collectAsState(initial = null)

    // ✅ NEW: נרמול אחיד למפתחות SP (כדי שסיכום ותוכן יקראו את אותו מפתח)
    fun spKeyPart(s: String): String = s
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .trim()

    val excludedKeySuffix = remember(topicUi, subTopicFilter) {
        val t = spKeyPart(topicUi)
        val st = subTopicFilter?.let(::spKeyPart).orEmpty()
        if (st.isBlank()) t else "${t}__${st}"
    }

    val excludedItems = remember { mutableStateListOf<String>() }
    LaunchedEffect(belt, excludedKeySuffix) {
        excludedItems.clear()
        excludedItems.addAll(
            sp.getStringSet("excluded_${belt.id}_$excludedKeySuffix", emptySet()) ?: emptySet()
        )
    }
    fun toggleExclude(item: String) {
        // ✅ item כאן כבר canonicalId
        if (excludedItems.contains(item)) excludedItems.remove(item) else excludedItems.add(item)

        sp.edit()
            .putStringSet("excluded_${belt.id}_$excludedKeySuffix", excludedItems.toSet())
            .apply()
    }

    // ⬇️ מועדפים / הערות נשארים ב-SP (לא קשור לסימונים)
    val favKey = remember(belt.id, excludedKeySuffix) { "fav_${belt.id}_$excludedKeySuffix" }
    var favorites by remember(favKey) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(favKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }
    fun toggleFavorite(id: String) {
        val s = favorites.toMutableSet()
        if (!s.add(id)) s.remove(id)
        favorites = s
        sp.edit().putStringSet(favKey, s).apply()
    }

    // ✅ סימונים (✓/✗/—) — מקור אמת יחיד: ViewModel/DataStore

    val unknownKey = remember(belt.id, excludedKeySuffix) { "unknown_${belt.id}_$excludedKeySuffix" }
    var unknowns by remember(unknownKey) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(unknownKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }
    fun setUnknown(id: String, set: Boolean) {
        val s = unknowns.toMutableSet()
        if (set) s.add(id) else s.remove(id)
        unknowns = s
        sp.edit().putStringSet(unknownKey, s).apply()
    }
// ✅ NEW: נשמור גם mastered (וי ירוק) ב-SP כדי שהסיכום יראה אותו
    val masteredKey = remember(belt.id, excludedKeySuffix) { "mastered_${belt.id}_$excludedKeySuffix" }
    var masteredSet by remember(masteredKey) {
        mutableStateOf<MutableSet<String>>(
            sp.getStringSet(masteredKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        )
    }
    fun setMasteredLocal(id: String, set: Boolean) {
        val s = masteredSet.toMutableSet()
        if (set) s.add(id) else s.remove(id)
        masteredSet = s
        sp.edit().putStringSet(masteredKey, s).apply()
    }

    // (SharedPreferences) הערות חופשיות לכל תרגיל – בלי excludedKeySuffix גלובלי
    fun loadNote(itemId: String): String {
        val suffix = if (subTopicFilter.isNullOrBlank()) {
            topicUi
        } else {
            "${topicUi}__${subTopicFilter}"
        }
        val key = "note_${belt.id}_${suffix}_$itemId"
        return sp.getString(key, "") ?: ""
    }

    fun saveNote(itemId: String, value: String) {
        val suffix = if (subTopicFilter.isNullOrBlank()) {
            topicUi
        } else {
            "${topicUi}__${subTopicFilter}"
        }
        val key = "note_${belt.id}_${suffix}_$itemId"
        with(sp.edit()) {
            if (value.isBlank()) {
                remove(key)
            } else {
                putString(key, value)
            }
            apply()
        }
    }

    // טעינת מצבי שליטה — מקור אמת יחיד: VM/DataStore
    LaunchedEffect(belt, topicUi, subTopicFilter, itemList) {
        itemStates.clear()

        itemList.forEach { item ->
            val canonicalId = canonicalFor(item)

            val vFromVm: Boolean? =
                runCatching { vm.getItemStatusNullable(belt, vmTopic, canonicalId) }.getOrNull()
                    ?: runCatching { if (vm.isMastered(belt, vmTopic, canonicalId)) true else null }.getOrNull()

            itemStates[item] = vFromVm
        }
    }

    Scaffold(
        topBar = {
            val headerTitle =
                if (subTopicFilter.isNullOrBlank()) {
                    topicTitleForUi(topic, currentLang)
                } else {
                    "${topicTitleForUi(topic, currentLang)} - ${topicTitleForUi(subTopicFilter, currentLang)}"
                }

            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            il.kmi.app.ui.KmiTopBar(
                title = headerTitle,
                onHome = onOpenHome,
                // לא רוצים אייקון בית עליון כי הוא כבר קיים
                showTopHome = false,
                showRoleStatus = false,      // מבטל את תג "מאמן" בצד
                centerTitle = true,
                alignTitleEnd = false,
                showBottomActions = true,
                onPickSearchResult = { key -> handlePickFromTopBar(key) },
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
                color = Color.Transparent,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.96f),
                                    belt.color.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.94f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = belt.color.copy(alpha = 0.14f)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedButton(
                                text = if (isEnglish) "Practice" else "תרגול",
                                modifier = Modifier.weight(1f),
                                containerColor = belt.color.copy(alpha = 0.92f),
                                onClick = { onPractice(belt, topicUi) }
                            )

                            AnimatedButton(
                                text = if (isEnglish) "Reset" else "איפוס",
                                modifier = Modifier.weight(1f),
                                containerColor = Color(0xFFB3261E),
                                onClick = {
                                    scope.launch {
                                        vm.clearTopic(belt, vmTopic)
                                        itemList.forEach { item -> itemStates[item] = null }

                                        excludedItems.clear()
                                        sp.edit()
                                            .remove("excluded_${belt.id}_$excludedKeySuffix")
                                            .remove("fav_${belt.id}_$excludedKeySuffix")
                                            .apply()

                                        favorites = mutableSetOf()
                                    }
                                }
                            )
                        }

                        AnimatedButton(
                            text = if (isEnglish) "Summary Screen" else "מסך סיכום",
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color(0xFF1F2937),
                            onClick = { onSummary(belt, topicUi, subTopicFilter) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
    Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = belt.lightColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {

            // ===== דיאלוג הסבר בעקבות חיפוש =====
            explainTriple?.let { (b, t, iRaw) ->
                fun norm2(s: String) = s.replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
                    .replace(Regex("[\u0591-\u05C7]"), "")
                    .replace("[\\-–—:_]".toRegex(), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim().lowercase()

                fun cleanItem2(topic2: String, item2: String): String {
                    var s = item2.trim()

                    // ✅ מסירים רק prefix של topic2::
                    if (topic2.isNotBlank() && s.startsWith("$topic2::")) {
                        s = s.removePrefix("$topic2::").trim()
                    }

                    // ✅ חשוב: לא לחתוך substringAfterLast("::")

                    s = s.replace(Regex("\\s+"), " ").trim()
                    return s
                }

                fun findCanonicalItem2(belt2: Belt, topic2: String, displayItem2: String): String? {
                    val wanted = norm2(displayItem2)
                    val topicTrim2 = topic2.trim()

                    val all = SharedContentRepo.getAllItemsFor(
                        belt = belt2,
                        topicTitle = topicTrim2,
                        subTopicTitle = null
                    )

                    all.firstOrNull { raw ->
                        val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
                        norm2(disp) == wanted || norm2(raw) == wanted
                    }?.let { return it }

                    val subs = SharedContentRepo.getSubTopicsFor(belt2, topicTrim2)
                    subs.forEach { st ->
                        st.items.firstOrNull { raw ->
                            val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
                            norm2(disp) == wanted || norm2(raw) == wanted
                        }?.let { return it }
                    }

                    return null
                }

                val canonical = CanonicalIds.resolveCanonicalForExplanation(b, t, iRaw)
                val explanation = remember(b, canonical) {
                    il.kmi.app.domain.Explanations.get(b, canonical).ifBlank {
                        val alt = canonical.substringAfter(":", canonical).trim()
                        il.kmi.app.domain.Explanations.get(b, alt)
                    }
                }.ifBlank { "לא נמצא הסבר עבור \"$canonical\"." }

                ExerciseExplanationDialog(
                    title = canonical,
                    beltLabel = "(${b.heb})",
                    explanation = explanation,
                    noteText = loadNote(canonical),
                    isFavorite = favorites.contains(canonical),
                    accentColor = b.color,
                    onDismiss = { explainTriple = null },
                    onEditNote = {
                        noteEditorFor = canonical
                        noteDraft = loadNote(canonical)
                    },
                    onToggleFavorite = { toggleFavorite(canonical) }
                )
            }
        // ===== סוף הדיאלוג =====

        noteEditorFor?.let { itemId ->
            AlertDialog(
                onDismissRequest = { noteEditorFor = null },
                title = {
                    Text(
                        if (isEnglish) "Exercise Note" else "הערה על התרגיל",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (isEnglish) "Write a free note" else "הקלד הערה חופשית") },
                            minLines = 3,
                            maxLines = 5
                        )
                        if (noteDraft.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    noteDraft = ""
                                    saveNote(itemId, "")
                                    noteEditorFor = null
                                }
                            ) {
                                Text(if (isEnglish) "Delete note" else "מחק הערה")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            saveNote(itemId, noteDraft)
                            noteEditorFor = null
                        }
                    ) {
                        Text(if (isEnglish) "Save" else "שמור")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { noteEditorFor = null }
                    ) {
                        Text(if (isEnglish) "Cancel" else "בטל")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.End
        ) {
            val header = if (subTopicFilter.isNullOrBlank()) {
                if (isEnglish) {
                    "Material: ${topicTitleForUi(topicUi, currentLang)}"
                } else {
                    "חומר: $topicUi"
                }
            } else {
                if (isEnglish) {
                    "Material: ${topicTitleForUi(topicUi, currentLang)} – ${topicTitleForUi(subTopicFilter, currentLang)}"
                } else {
                    "חומר: $topicUi – $subTopicFilter"
                }
            }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        val beltRes: Int = when (belt) {
                            Belt.WHITE  -> R.drawable.belt_white
                            Belt.YELLOW -> R.drawable.belt_yellow
                            Belt.ORANGE -> R.drawable.belt_orange
                            Belt.GREEN  -> R.drawable.belt_green
                            Belt.BLUE   -> R.drawable.belt_blue
                            Belt.BROWN  -> R.drawable.belt_brown
                            Belt.BLACK  -> R.drawable.belt_black
                        }

                        Text(
                            text = header,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF334155),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.70f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = belt.color.copy(alpha = 0.18f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = beltRes),
                                contentDescription = "חגורה ${belt.heb}",
                                modifier = Modifier.size(32.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Divider(
                    color = belt.color.copy(alpha = 0.14f),
                    thickness = 1.dp
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(belt.lightColor)
                        .padding(top = 4.dp, start = 12.dp, end = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {

                        // ✅ חשוב: מה שמוצג למשתמש (UI) ומה שנשלח ל-VM (Persist)
                        // אין טיפול מיוחד ל"כללי" כאן — ה-VM מטפל בזה דרך canonicalTopicKey()
                        val topicUi = topic
                        val vmTopic = topic

                        val filtered = itemList
                        filtered.forEach { item ->
                            var showNoteDialog by remember { mutableStateOf(false) }

                            // ✅ מזהה אחיד לכל פעולה/שמירה
                            val canonicalId = remember(item, belt.id, topicUi) { canonicalFor(item) }

                            // ✅ טקסט לתצוגה בלבד
                            val displayName = remember(item, topicUi, currentLang) {
                                itemTitleForUi(topicUi, item, currentLang)
                            }

                            var noteText by remember(item, belt.id, excludedKeySuffix) {
                                mutableStateOf(loadNote(canonicalId))
                            }

                            val mastered = itemStates[item]
                            val isExcluded = excludedItems.contains(canonicalId)
                            val isHighlighted = highlight != null && canonicalId == highlight

                            val bringer = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
                            LaunchedEffect(isHighlighted) {
                                if (isHighlighted) {
                                    kotlinx.coroutines.delay(120)
                                    bringer.bringIntoView()
                                }
                            }

                            var pressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(
                                targetValue = if (pressed) 1.2f else 1f,
                                label = "scaleAnim"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .bringIntoViewRequester(bringer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ItemFloatingActions(
                                        excluded = isExcluded,
                                        isFav = favorites.contains(canonicalId),
                                        hasNote = noteText.isNotBlank(),
                                        onToggleExclude = { toggleExclude(canonicalId) },
                                        onInfo = {
                                            pressed = true
                                            explainTriple = Triple(belt, topicUi, canonicalId)
                                            scope.launch {
                                                kotlinx.coroutines.delay(150)
                                                pressed = false
                                            }
                                        },
                                        onToggleFavorite = { toggleFavorite(canonicalId) },
                                        onEditNote = { showNoteDialog = true }
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = displayName,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp),
                                        color = when {
                                            isExcluded -> Color.Gray
                                            isHighlighted -> belt.color.copy(alpha = 0.95f)
                                            else -> Color(0xFF111827)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    MasterToggle(
                                        mastered = mastered,
                                        onSelect = { newVal ->
                                            itemStates[item] = newVal
                                            vm.setItemStatusNullable(belt, vmTopic, canonicalId, newVal)
                                        }
                                    )
                                }

                                Divider(
                                    color = belt.color.copy(alpha = 0.30f),
                                    thickness = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp)
                                )
                            }

                            // דיאלוג הערה
                            if (showNoteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showNoteDialog = false },
                                    title = {
                                        Text(
                                            if (isEnglish) "Exercise Note" else "הערה על התרגיל",
                                            style = MaterialTheme.typography.titleSmall,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    text = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            OutlinedTextField(
                                                value = noteText,
                                                onValueChange = { noteText = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                label = { Text(if (isEnglish) "Write a free note" else "הקלד הערה חופשית") },
                                                minLines = 3,
                                                maxLines = 5
                                            )
                                            if (noteText.isNotBlank()) {
                                                TextButton(
                                                    onClick = {
                                                        noteText = ""
                                                        saveNote(canonicalId, "")
                                                        showNoteDialog = false
                                                    }
                                                ) {
                                                    Text(if (isEnglish) "Delete note" else "מחק הערה")
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            saveNote(canonicalId, noteText)
                                            showNoteDialog = false
                                        }) {
                                            Text(if (isEnglish) "Save" else "שמור")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showNoteDialog = false }) {
                                            Text(if (isEnglish) "Cancel" else "בטל")
                                        }
                                    }
                                )
                            }

                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}

// ===== כפתור מונפש =====
@Composable
fun AnimatedButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "buttonScaleAnim"
    )
    val scope = rememberCoroutineScope()

    val contentOnContainer =
        if (containerColor.luminance() < 0.5f) Color.White else Color.Black

    Button(
        onClick = {
            pressed = true
            onClick()
            scope.launch {
                kotlinx.coroutines.delay(140)
                pressed = false
            }
        },
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .defaultMinSize(minWidth = 90.dp),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.24f)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentOnContainer
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = text,
            color = contentOnContainer,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ItemFloatingActions(
    excluded: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    onToggleExclude: () -> Unit,
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNote: () -> Unit
) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }
    var expanded by remember { mutableStateOf(false) }
    var helpSeen by remember { mutableStateOf(sp.getBoolean("exclude_help_seen", false)) }

    val infoScale by animateFloatAsState(
        targetValue = if (expanded) 1.08f else 1f,
        animationSpec = tween(180),
        label = "materialsInfoScale"
    )

    val infoRotation by animateFloatAsState(
        targetValue = if (expanded) 12f else 0f,
        animationSpec = tween(180),
        label = "materialsInfoRotation"
    )

    LaunchedEffect(expanded) {
        if (expanded && !helpSeen) {
            helpSeen = true
            sp.edit().putBoolean("exclude_help_seen", true).apply()
        }
    }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            ),
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = infoScale
                    scaleY = infoScale
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.98f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.94f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "פעולות לתרגיל",
                    tint = Color(0xFF455A64),
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

            if (!helpSeen) {
                DropdownMenuItem(
                    enabled = false,
                    text = {
                        Text(
                            "מה זה “החרג”?\nמנטרל את התרגיל מהתרגול של הנושא הנבחר.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {}
                )
                Divider()
            }

            DropdownMenuItem(
                text = { Text("מידע", style = MaterialTheme.typography.labelLarge) },
                onClick = {
                    expanded = false
                    onInfo()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        if (isFav) "הסר ממועדפים" else "הוסף למועדפים",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                onClick = {
                    expanded = false
                    onToggleFavorite()
                    android.widget.Toast
                        .makeText(
                            context,
                            if (isFav) "הוסר מהמועדפים." else "נוסף למועדפים.",
                            android.widget.Toast.LENGTH_SHORT
                        )
                        .show()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        if (excluded) "בטל החרגה" else "החרג (מנטרל מהתרגול)",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                onClick = {
                    expanded = false
                    onToggleExclude()
                    android.widget.Toast
                        .makeText(
                            context,
                            if (excluded) "בוטלה ההחרגה – התרגיל יחזור לתרגול." else "התרגיל הוחרג – לא יופיע בתרגול הנושא.",
                            android.widget.Toast.LENGTH_SHORT
                        )
                        .show()
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        if (hasNote) "ערוך / מחק הערה" else "הוסף הערה לתרגיל",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                onClick = {
                    expanded = false
                    onEditNote()
                }
            )
        }
    }
}
