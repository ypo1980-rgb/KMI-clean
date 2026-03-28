package il.kmi.app

import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import il.kmi.shared.domain.Belt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import il.kmi.app.search.AppSearchHit
import il.kmi.app.search.KmiSearchBridge
import kotlinx.coroutines.flow.asStateFlow
import il.kmi.app.data.training.TrainingSummaryLocalRepo
import il.kmi.shared.domain.catalog.CatalogRepo
import kotlinx.coroutines.launch

// ========= הדגשת תרגיל במסך הפריטים =========
private val _highlightItem = MutableStateFlow<String?>(null)
val highlightItem: StateFlow<String?> = _highlightItem.asStateFlow()

fun setHighlightItem(name: String?) {
    _highlightItem.value = name
}

fun clearHighlightItem() {
    _highlightItem.value = null
}

// חיפוש סינכרוני ופשוט (מספיק מהיר בשביל להתחיל)
fun search(query: String, belt: Belt? = null): List<AppSearchHit> {
    return KmiSearchBridge.search(query.trim(), belt)
}

/** ממשק מינימלי שמסך ההגדרות החדש משתמש בו לסטטיסטיקות */
interface StatsVm {
    fun getItemStatusNullable(belt: Belt, topic: String, item: String): Boolean?
    fun isMastered(belt: Belt, topic: String, item: String): Boolean
}

class KmiViewModel(
    private val ds: DataStoreManager,
    private val trainingSummaryLocalRepo: TrainingSummaryLocalRepo, // ✅ NEW
) : ViewModel() {

    private val _selectedBelt = MutableStateFlow<Belt?>(null)
    val selectedBelt: StateFlow<Belt?> = _selectedBelt.asStateFlow()

    // ✅ NEW: כל שינוי סימון מגדיל גרסה כדי לרענן ProgressMeter בלי IO
    private val _marksVersion = MutableStateFlow(0L)
    val marksVersion: StateFlow<Long> = _marksVersion.asStateFlow()

    fun setSelectedBelt(belt: Belt) {
        viewModelScope.launch {
            ds.saveSelectedBelt(belt)
            _selectedBelt.value = belt
        }
    }

    fun clearSelectedBelt() {
        viewModelScope.launch {
            ds.clearSelectedBelt()
            _selectedBelt.value = null
        }
    }

    fun loadPersistedBelt() {
        viewModelScope.launch {
            _selectedBelt.value = ds.readSelectedBelt()
        }
    }

    // === סטטוס פריטים (cache בזיכרון כדי למנוע קריאות מיותרות ל־DataStore) ===
    private val masteredItems =
        mutableStateMapOf<String, MutableMap<String, MutableMap<String, Boolean?>>>()

    // ✅ NEW: עוזר פנימי — כתיבה עקבית ל-cache
    private fun putCache(belt: Belt, topicKey: String, item: String, value: Boolean?) {
        val beltMap = masteredItems.getOrPut(belt.id) { mutableMapOf() }
        val topicMap = beltMap.getOrPut(topicKey) { mutableMapOf() }
        if (value == null) topicMap.remove(item) else topicMap[item] = value
    }

    /**
     * ✅ NEW: Warmup ל-cache אחרי ריסטארט.
     * המסך שולח רשימת פריטים (ה-canonicalId שהוא משתמש בו),
     * ואנחנו טוענים מה-DataStore לתוך masteredItems כדי שה-UI יראה סימונים.
     */
    fun warmUpTopicStatuses(
        belt: Belt,
        topic: String,
        items: Collection<String>
    ) {
        val t = canonicalTopicKey(topic)
        if (items.isEmpty()) return

        viewModelScope.launch {
            var changed = false

            for (item in items) {
                // אם כבר ב-cache — דילוג
                val cached = masteredItems[belt.id]?.get(t)?.get(item)
                if (cached != null || (masteredItems[belt.id]?.get(t)?.containsKey(item) == true)) {
                    continue
                }

                val v = ds.readItemStatus(belt, t, item) // ✅ מקור אמת
                if (v != null) {
                    putCache(belt, t, item, v)
                    changed = true
                }
                // אם v == null אנחנו פשוט משאירים לא מסומן — אין מה לשמור ב-cache
            }

            if (changed) {
                recalcProgress()
                _marksVersion.value = _marksVersion.value + 1L
            }
        }
    }

    /** ✅ NEW: Snapshot מהיר מה-cache עבור נושא מסוים (ללא IO) */
    fun getTopicStatusSnapshot(belt: Belt, topic: String): Map<String, Boolean?> {
        val t = canonicalTopicKey(topic)
        val topicMap = masteredItems[belt.id]?.get(t) ?: return emptyMap()
        return topicMap.toMap()
    }

    /** ✅ NEW: Snapshot מהיר של כל החגורה (ללא IO) */
    fun getBeltStatusSnapshot(belt: Belt): Map<String, Map<String, Boolean?>> {
        val beltMap = masteredItems[belt.id] ?: return emptyMap()
        return beltMap.mapValues { (_, topicMap) -> topicMap.toMap() }
    }

    /** קבלת מצב של פריט (Nullable: true/false/null) */
    suspend fun getItemStatusNullable(belt: Belt, topic: String, item: String): Boolean? {
        val t = canonicalTopicKey(topic)

        // cache
        masteredItems[belt.id]?.get(t)?.get(item)?.let { return it }

        // datastore
        val value = ds.readItemStatus(belt, t, item)

        // שמירה ב-cache רק אם יש ערך אמיתי (true/false)
        if (value != null) {
            putCache(belt, t, item, value)
        }

        return value
    }

    /** בדיקה אם פריט נלמד (ברירת מחדל = false) */
    suspend fun isMastered(belt: Belt, topic: String, item: String): Boolean {
        val t = canonicalTopicKey(topic)
        return ds.isItemMastered(belt, t, item)
    }

    /** ✅ NEW: קביעה/איפוס מצב פריט (true/false/null) — מקור אמת יחיד: DataStore */
    fun setItemStatusNullable(belt: Belt, topic: String, item: String, value: Boolean?) {
        val t = canonicalTopicKey(topic)

        // עדכון cache מיידי כדי ששני המסכים יראו אותו דבר בלי "הבהובים"
        putCache(belt, t, item, value)

        viewModelScope.launch {
            when (value) {
                null -> ds.clearItemStatus(belt, t, item)
                else -> ds.setItemMastered(belt, t, item, value)
            }
            recalcProgress()
            _marksVersion.value = _marksVersion.value + 1L
        }
    }

    /** ✅ NEW: איפוס פריטים ספציפיים לנושא (מנקה DataStore + cache) */
    fun clearTopicItems(
        belt: Belt,
        topic: String,
        canonicalIds: Collection<String>
    ) {
        val t = canonicalTopicKey(topic)
        if (canonicalIds.isEmpty()) return

        // ✅ ננקה cache מיידי
        masteredItems[belt.id]?.get(t)?.let { topicMap ->
            canonicalIds.forEach { id -> topicMap.remove(id) }
        }

        // ✅ ננקה datastore
        viewModelScope.launch {
            canonicalIds.forEach { id ->
                ds.clearItemStatus(belt, t, id)
            }
            recalcProgress()
            _marksVersion.value = _marksVersion.value + 1L
        }
    }

    /** איפוס נושא שלם */
    fun clearTopic(belt: Belt, topic: String) {
        val t = canonicalTopicKey(topic)

        val items = masteredItems[belt.id]?.get(t)?.keys?.toList().orEmpty()

        // ✅ ננקה cache נכון (remove במקום לשים null)
        masteredItems[belt.id]?.get(t)?.clear()

        // ננקה datastore
        viewModelScope.launch {
            items.forEach { ds.clearItemStatus(belt, t, it) }
            recalcProgress()
            _marksVersion.value = _marksVersion.value + 1L
        }
    }

    private fun normalizeTopicKey(topic: String): String {
        return topic
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .trim()
    }

    // ✅ מקור אמת ל-topicKey (כולל טיפול ב"כללי")
    private fun canonicalTopicKey(topic: String): String {
        val t = normalizeTopicKey(topic)

        // ✅ כללי = "" (וגם topic ריק מתנהג אותו דבר)
        if (t.isBlank()) return ""
        return if (t.equals("כללי", ignoreCase = true)) "" else t
    }

    // === אחוזי התקדמות לכל חגורה ===
    private val _progress = mutableStateOf<Map<Belt, Int>>(emptyMap())
    val progress: State<Map<Belt, Int>> = _progress

    private fun normalizeCatalogItem(raw: String): String {
        // אם יש tag כמו "def:...::שם תרגיל" או "שם::def:..."
        return if (raw.contains("::")) raw.substringAfterLast("::").trim() else raw.trim()
    }

    private data class CatalogEntry(
        val topicTitle: String,
        val subTopicTitle: String?,
        val rawItem: String,
        val displayItem: String
    )

    private fun getCatalogEntriesForBelt(belt: Belt): List<CatalogEntry> {
        val out = mutableListOf<CatalogEntry>()

        val topicTitles = CatalogRepo.listTopicTitles(belt)
        if (topicTitles.isEmpty()) return emptyList()

        for (topicTitle in topicTitles) {
            val topic = CatalogRepo.findTopic(belt, topicTitle) ?: continue
            val subs = topic.subTopics

            if (subs.isNotEmpty()) {
                for (st in subs) {
                    val subTitle = st.title
                    for (raw in st.items) {
                        out += CatalogEntry(
                            topicTitle = topicTitle,
                            subTopicTitle = subTitle,
                            rawItem = raw,
                            displayItem = normalizeCatalogItem(raw)
                        )
                    }
                }
            } else {
                for (raw in topic.items) {
                    out += CatalogEntry(
                        topicTitle = topicTitle,
                        subTopicTitle = null,
                        rawItem = raw,
                        displayItem = normalizeCatalogItem(raw)
                    )
                }
            }
        }

        return out
    }

    private fun recalcProgress() {
        val newProgress = mutableMapOf<Belt, Int>()

        // נחשב לפי סדר החגורות שלך
        for (belt in Belt.order) {
            val entries = getCatalogEntriesForBelt(belt)
            val total = entries.size
            if (total == 0) {
                newProgress[belt] = 0
                continue
            }

            val learned: Set<String> =
                masteredItems[belt.id]
                    ?.values
                    ?.flatMap { topicMap -> topicMap.filterValues { it == true }.keys }
                    ?.toSet()
                    ?: emptySet()

            val masteredCount = entries.count { it.displayItem in learned }
            val percent = (masteredCount * 100) / total
            newProgress[belt] = percent
        }

        _progress.value = newProgress
    }

    fun preloadTopicsBySubjectCounts() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val subjects = il.kmi.app.domain.TopicsBySubjectRegistry.allSubjects()
                val handsBase = subjects.firstOrNull { it.id == "hands_all" }

                il.kmi.app.screens.BeltQuestions.SubjectTopicsUiLogic
                    .ensureTopicsUiCountsPreloaded(
                        subjects = subjects,
                        handsBase = handsBase
                    )
            } catch (t: Throwable) {
                android.util.Log.e("KMI_PERF", "preloadTopicsBySubjectCounts failed", t)
            }
        }
    }

    init {
        recalcProgress()
        preloadTopicsBySubjectCounts()
    }
}

// ─────────────────────────────────────────────
// Factory עבור KmiViewModel (להישאר באותו קובץ)
// ─────────────────────────────────────────────
class KmiViewModelFactory(
    private val dataStoreManager: DataStoreManager,
    private val spTrainingSummary: SharedPreferences,
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KmiViewModel::class.java)) {
            val localRepo = TrainingSummaryLocalRepo(spTrainingSummary)
            return KmiViewModel(
                ds = dataStoreManager,
                trainingSummaryLocalRepo = localRepo
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
