package il.kmi.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.Explanations
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.material3.Divider
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import il.kmi.app.ui.KmiTtsManager
import il.kmi.app.ui.ext.lightColor
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R

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
    val sp = remember { context.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val itemStates = remember(belt.id, topic, subTopicFilter) { mutableStateMapOf<String, Boolean?>() }

    var explainTriple by remember { mutableStateOf<Triple<Belt, String, String>?>(null) }
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val explanationTextColor = if (isDarkSurface) Color.White else Color.Black

    // ===== חיבור חיפוש =====
    fun cleanItem(topic: String, item: String): String {
        var s = item.trim()

        // מסירים רק topic:: אם קיים
        if (topic.isNotBlank() && s.startsWith("$topic::")) {
            s = s.removePrefix("$topic::").trim()
        }

        // ❌ לא לעשות substringAfterLast("::") !
        // אם יש :: זה חלק מהייחודיות של הפריט (subTopic::item)

        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    fun norm(s: String) = s
        .replace("\u200F","").replace("\u200E","").replace("\u00A0"," ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .replace("[\\-–—:_]".toRegex(), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase()

    fun findCanonicalItem(b: Belt, t: String, displayItem: String): String? {
        val wanted = norm(displayItem)

        // 1) פריטים ישירים של נושא
        val direct = SharedContentRepo.getAllItemsFor(b, t, subTopicTitle = null)
        direct.firstOrNull { raw ->
            val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
            norm(disp) == wanted || norm(raw) == wanted
        }?.let { return it }

        // 2) פריטים מתוך תתי-נושאים (מדויק)
        val subs = SharedContentRepo.getSubTopicsFor(b, t)
        subs.forEach { st ->
            st.items.firstOrNull { raw ->
                val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
                norm(disp) == wanted || norm(raw) == wanted
            }?.let { return it }
        }

        return null
    }

    // ✅ זה מה ש-MaterialsScreen צריך: canonical לפי ה-topic של המסך
    fun canonicalFor(displayItem: String): String {
        val cleaned = cleanItem(topic, displayItem)
        return findCanonicalItem(belt, topic, cleaned) ?: cleaned
    }

    // ✅ אופציונלי למסכים אחרים: canonical לפי topicTitle שמועבר מבחוץ
    fun canonicalFor(topicTitle: String, displayItem: String): String {
        val cleaned = cleanItem(topicTitle, displayItem)
        return findCanonicalItem(belt, topicTitle, cleaned) ?: cleaned
    }
    // ===== סוף חיבור חיפוש =====

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
            val topicFromKey = parts.getOrNull(1).orEmpty().ifBlank { topic }
            val itemRaw = cleanItem(topicFromKey, parts.getOrNull(2).orEmpty())
            explainTriple = Triple(beltFromKey, topicFromKey, itemRaw)
        }
    }

    // === שליפת התרגילים (כולל subTopicFilter) ===
    // ✅ Cache בזיכרון כדי שמעבר בין נושאים שכבר נפתחו יהיה מיידי
    val itemsCache = remember { mutableMapOf<String, List<String>>() }
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

    // הדגשת תרגיל
    val highlightState = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(vm) {
        runCatching {
            val f = vm::class.java.getDeclaredField("highlightItem").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val flow = f.get(vm) as? kotlinx.coroutines.flow.StateFlow<String?>
            flow?.collect { value -> highlightState.value = value }
        }
    }
    val highlight = highlightState.value

    // ✅ NEW: נרמול אחיד למפתחות SP (כדי שסיכום ותוכן יקראו את אותו מפתח)
    fun spKeyPart(s: String): String = s
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .trim()

    val excludedKeySuffix = remember(topic, subTopicFilter) {
        val t = spKeyPart(topic)
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

    // ⬇️ מועדפים / לא־ידוע
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
            topic
        } else {
            "${topic}__${subTopicFilter}"
        }
        val key = "note_${belt.id}_${suffix}_$itemId"
        return sp.getString(key, "") ?: ""
    }

    fun saveNote(itemId: String, value: String) {
        val suffix = if (subTopicFilter.isNullOrBlank()) {
            topic
        } else {
            "${topic}__${subTopicFilter}"
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

    // טעינת מצבי שליטה מה־VM (טעינה מלאה רק כשנושא/רשימה משתנים)
    LaunchedEffect(belt, topic, subTopicFilter, itemList) {
        itemStates.clear()

        itemList.forEach { item ->
            val canonicalId = canonicalFor(item)

            val vFromVm: Boolean? =
                runCatching { vm.getItemStatusNullable(belt, topic, canonicalId) }.getOrNull()
                    ?: runCatching { if (vm.isMastered(belt, topic, canonicalId)) true else null }.getOrNull()

            // אם אין ערך מה-VM, נשתמש ב-unknowns רק כ-fallback מקומי
            val finalV: Boolean? = when {
                vFromVm != null -> vFromVm
                unknowns.contains(canonicalId) -> false
                else -> null
            }

            itemStates[item] = finalV
        }
    }

    // ✅ Compat bridge: לא תלוי בשם המדויק ב-KmiViewModel (מונע Unresolved reference)
    suspend fun setItemStatusCompat(
        belt: Belt,
        topic: String,
        item: String,
        value: Boolean?
    ) {
        val cls = vm.javaClass

        fun findExact(name: String, nullable: Boolean): java.lang.reflect.Method? {
            return cls.methods.firstOrNull { m ->
                if (m.name != name) return@firstOrNull false
                if (m.parameterTypes.size != 4) return@firstOrNull false

                val p = m.parameterTypes
                val okBase =
                    p[0].isAssignableFrom(belt::class.java) &&
                            p[1] == String::class.java &&
                            p[2] == String::class.java

                if (!okBase) return@firstOrNull false

                val want = if (nullable) java.lang.Boolean::class.java else java.lang.Boolean.TYPE
                p[3] == want
            }
        }

        // 1) הכי טוב: setItemStatusNullable(Belt, String, String, Boolean?)
        findExact("setItemStatusNullable", nullable = true)?.let { m ->
            runCatching { m.invoke(vm, belt, topic, item, value) }
            return
        }

        // 2) fallback: setItemStatus(Belt, String, String, Boolean?) אם קיים
        findExact("setItemStatus", nullable = true)?.let { m ->
            runCatching { m.invoke(vm, belt, topic, item, value) }
            return
        }

        // 3) fallback למתודה עם boolean (לא-nullable) רק אם value != null
        if (value != null) {
            findExact("setItemStatusNullable", nullable = false)?.let { m ->
                runCatching { m.invoke(vm, belt, topic, item, value) }
                return
            }
            findExact("setItemStatus", nullable = false)?.let { m ->
                runCatching { m.invoke(vm, belt, topic, item, value) }
                return
            }
            findExact("setMastered", nullable = false)?.let { m ->
                runCatching { m.invoke(vm, belt, topic, item, value) }
                return
            }
        }

        android.util.Log.w("MaterialsScreen", "No compatible setItemStatus method found on KmiViewModel")
    }

    Scaffold(
        topBar = {
            val headerTitle =
                if (subTopicFilter.isNullOrBlank()) topic else "$topic – $subTopicFilter"

            il.kmi.app.ui.KmiTopBar(
                title = headerTitle,
                onHome = onOpenHome,
                // לא רוצים אייקונים מיותרים למעלה – רק טקסט
                showTopHome = false,          // מבטל אייקון הבית בכותרת העליונה
                showRoleStatus = false,       // מבטל את תג "מאמן" בצד
                centerTitle = true,
                alignTitleEnd = false,
                showBottomActions = true,     // שומר על שורת האייקונים התחתונה (שידור/שיתוף/בית/הגדרות/חיפוש)
                onPickSearchResult = { key -> handlePickFromTopBar(key) }
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFFE0E0E0),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedButton(
                            text = "תרגול",
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFF6F64FF),
                            onClick = { onPractice(belt, topic) }
                        )
                        AnimatedButton(
                            text = "איפוס",
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFFD32F2F),
                            onClick = {
                                scope.launch {
                                    vm.clearTopic(belt, topic)
                                    itemList.forEach { item -> itemStates[item] = null }
                                    excludedItems.clear()
                                    sp.edit()
                                        .remove("excluded_${belt.id}_$excludedKeySuffix")
                                        .remove("fav_${belt.id}_$excludedKeySuffix")
                                        .remove("unknown_${belt.id}_$excludedKeySuffix")
                                        .apply()
                                    favorites = mutableSetOf()
                                    unknowns  = mutableSetOf()
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedButton(
                            text = "מסך סיכום",
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFF6F64FF),
                            // היה: onClick = { onSummary(belt) }
                            onClick = { onSummary(belt, topic, subTopicFilter) }
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

                val item = cleanItem2(t, iRaw)
                val canonical = findCanonicalItem2(b, t, item) ?: item
                val explanation = remember(b, canonical) {
                    il.kmi.app.domain.Explanations.get(b, canonical).ifBlank {
                        val alt = canonical.substringAfter(":", canonical).trim()
                        il.kmi.app.domain.Explanations.get(b, alt)
                    }
                }.ifBlank { "לא נמצא הסבר עבור \"$canonical\"." }

            AlertDialog(
                    onDismissRequest = { explainTriple = null },
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = { toggleFavorite(canonical) },
                                modifier = Modifier.align(AbsoluteAlignment.CenterLeft)
                            ) {
                                if (favorites.contains(canonical)) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "הסר ממועדפים",
                                        tint = Color(0xFFFFC107)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.StarBorder,
                                        contentDescription = "הוסף למועדפים"
                                    )
                                }
                            }
                            Text(
                                "מידע על התרגיל",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Right,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Right,
                            color = explanationTextColor
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { explainTriple = null }) {
                            Text("סגור")
                        }
                    }
                )
            }
            // ===== סוף הדיאלוג =====

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.End
            ) {
                val header = if (subTopicFilter.isNullOrBlank())
                    "חומר: $topic"
                else
                    "חומר: $topic – $subTopicFilter"

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()       // מגיע עד הקצוות (ימין/שמאל)
                            .height(64.dp)        // גובה קבוע: לא מתנפח בגלל החגורה
                            .zIndex(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // RIGHT: כותרת (צד ימין)
                            Text(
                                text = header,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Start,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(Modifier.width(12.dp))

                            // LEFT: חגורה גדולה ב-50% וממורכזת אנכית בתוך הפס האפור
                            val beltRes: Int = when (belt) {
                                Belt.WHITE  -> R.drawable.belt_white
                                Belt.YELLOW -> R.drawable.belt_yellow
                                Belt.ORANGE -> R.drawable.belt_orange
                                Belt.GREEN  -> R.drawable.belt_green
                                Belt.BLUE   -> R.drawable.belt_blue
                                Belt.BROWN  -> R.drawable.belt_brown
                                Belt.BLACK  -> R.drawable.belt_black
                                else        -> R.drawable.belt_white
                            }

                            Box(
                                modifier = Modifier
                                    .height(64.dp)   // זהה לגובה ה-Surface
                                    .width(84.dp),   // “תא” קבוע לתמונה
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = beltRes),
                                    contentDescription = "חגורה ${belt.heb}",
                                    modifier = Modifier.size(84.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(belt.lightColor)
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {

                        val filtered = itemList
                        filtered.forEach { item ->
                            var showInfoDialog by remember { mutableStateOf(false) }
                            var showNoteDialog by remember { mutableStateOf(false) }

                            // ✅ NEW: מזהה אחיד לכל פעולה/שמירה (מנקה "def:" / "topic::" וכו')
                            val canonicalId = remember(item, belt.id, topic) { canonicalFor(item) }

                            // ✅ NEW: טקסט לתצוגה בלבד (בלי קוד)
                            val displayName = remember(item, topic) {
                                val cleaned = cleanItem(topic, item)
                                ExerciseTitleFormatter.displayName(cleaned)
                                    .ifBlank { cleaned }
                                    .trim()
                            }

                            var noteText by remember(item, belt.id, excludedKeySuffix) {
                                mutableStateOf(loadNote(canonicalId))
                            }

                            val mastered = itemStates[item]
                            val isExcluded = excludedItems.contains(canonicalId)
                            val isHighlighted = highlight != null && item == highlight

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

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale) // ✅ להפעיל את האנימציה שחישבת
                                    .then(
                                        if (isHighlighted)
                                            Modifier.background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        else Modifier
                                    )
                                    .padding(vertical = 4.dp)
                                    .bringIntoViewRequester(bringer),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ItemFloatingActions(
                                    excluded = isExcluded,
                                    isFav = favorites.contains(canonicalId),
                                    hasNote = noteText.isNotBlank(),
                                    onToggleExclude = { toggleExclude(canonicalId) },
                                    onInfo = {
                                        pressed = true
                                        showInfoDialog = true
                                        scope.launch {
                                            kotlinx.coroutines.delay(150)
                                            pressed = false
                                        }
                                    },
                                    onToggleFavorite = { toggleFavorite(canonicalId) },
                                    onEditNote = { showNoteDialog = true }
                                )

                                Text(
                                    text = displayName, // ✅ FIX: מציגים רק שם נקי
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    color = when {
                                        isExcluded    -> Color.Gray
                                        isHighlighted -> MaterialTheme.colorScheme.primary
                                        else          -> Color.Black
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                                )

                                MasterToggle(
                                    mastered = mastered,
                                    onSelect = { newVal ->
                                        itemStates[item] = newVal

                                        when (newVal) {
                                            true -> {
                                                // ✅ וי ירוק נשמר
                                                setMasteredLocal(canonicalId, true)
                                                setUnknown(canonicalId, false)
                                            }
                                            false -> {
                                                // ❌ אדום נשמר
                                                setMasteredLocal(canonicalId, false)
                                                setUnknown(canonicalId, true)
                                            }
                                            null -> {
                                                // — ניקוי
                                                setMasteredLocal(canonicalId, false)
                                                setUnknown(canonicalId, false)
                                            }
                                        }

                                        // נשאיר גם את הכתיבה ל-VM (אם קיימת אצלך), אבל כבר לא תלויים בה
                                        scope.launch { setItemStatusCompat(belt, topic, canonicalId, newVal) }
                                    }
                                )
                            }

                            if (showInfoDialog) {
                                // ✅ FIX: הסבר לפי canonicalId (ולא לפי טקסט תצוגה/קוד)
                                val explanation2 = Explanations.get(belt, canonicalId)
                                val ctx = LocalContext.current

                                // ✅ במקום TextToSpeech מקומי — נשתמש במנהל הגלובלי
                                LaunchedEffect(showInfoDialog) {
                                    if (showInfoDialog) {
                                        KmiTtsManager.init(ctx)
                                    }
                                }
                                DisposableEffect(showInfoDialog) {
                                    onDispose {
                                        // לא עושים shutdown כדי לא לשבור מסכים אחרים; רק עוצרים
                                        KmiTtsManager.stop()
                                    }
                                }

                                AlertDialog(
                                    onDismissRequest = {
                                        KmiTtsManager.stop()
                                        showInfoDialog = false
                                    },
                                    title = {
                                        Text(
                                            "מידע על התרגיל",
                                            style = MaterialTheme.typography.titleSmall,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    text = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                explanation2,
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Right,
                                                color = explanationTextColor
                                            )
                                            IconButton(
                                                onClick = {
                                                    KmiTtsManager.speak(explanation2)
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.VolumeUp,
                                                    contentDescription = "השמע הסבר"
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                KmiTtsManager.stop()
                                                showInfoDialog = false
                                            }
                                        ) {
                                            Text("סגור", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                )
                            }

                            // דיאלוג הערה
                            if (showNoteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showNoteDialog = false },
                                    title = {
                                        Text(
                                            "הערה על התרגיל",
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
                                                label = { Text("הקלד הערה חופשית") },
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
                                                    Text("מחק הערה")
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            saveNote(canonicalId, noteText)
                                            showNoteDialog = false
                                        }) {
                                            Text("שמור")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showNoteDialog = false }) {
                                            Text("בטל")
                                        }
                                    }
                                )
                            }

                            Divider()
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
    val scale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, label = "buttonScaleAnim")
    val scope = rememberCoroutineScope()

    val contentOnContainer = if (containerColor.luminance() < 0.5f) Color.White else Color.Black

    Button(
        onClick = {
            pressed = true
            onClick()
            scope.launch {
                kotlinx.coroutines.delay(150)
                pressed = false
            }
        },
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .scale(scale)
            .height(56.dp)
            .defaultMinSize(minWidth = 90.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentOnContainer
        )
    ) {
        Text(
            text,
            color = contentOnContainer,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
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

    LaunchedEffect(expanded) {
        if (expanded && !helpSeen) {
            helpSeen = true
            sp.edit().putBoolean("exclude_help_seen", true).apply()
        }
    }

    Box {
        SmallFloatingActionButton(
            onClick = { expanded = true },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = "פעולות לתרגיל")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

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
                onClick = { expanded = false; onInfo() }
            )

            DropdownMenuItem(
                text = { Text(if (isFav) "הסר ממועדפים" else "הוסף למועדפים", style = MaterialTheme.typography.labelLarge) },
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
                text = { Text(if (excluded) "בטל החרגה" else "החרג (מנטרל מהתרגול)", style = MaterialTheme.typography.labelLarge) },
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
