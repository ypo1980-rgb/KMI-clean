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
import il.kmi.shared.domain.catalog.CatalogData
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


    /** קבלת מצב של פריט (Nullable: true/false/null) */
    suspend fun getItemStatusNullable(belt: Belt, topic: String, item: String): Boolean? {
        val t = canonicalTopicKey(topic)

        // cache
        masteredItems[belt.id]?.get(t)?.get(item)?.let { return it }

        // datastore
        val value = ds.readItemStatus(belt, t, item)

        // שמירה ב-cache רק אם יש ערך אמיתי (true/false)
        if (value != null) {
            val beltMap = masteredItems.getOrPut(belt.id) { mutableMapOf() }
            val topicMap = beltMap.getOrPut(t) { mutableMapOf() }
            topicMap[item] = value
        }

        return value
    }

    /** בדיקה אם פריט נלמד (ברירת מחדל = false) */
    suspend fun isMastered(belt: Belt, topic: String, item: String): Boolean {
        val t = canonicalTopicKey(topic)
        return ds.isItemMastered(belt, t, item)
    }

    /** איפוס נושא שלם */
    fun clearTopic(belt: Belt, topic: String) {
        val t = canonicalTopicKey(topic)

        val items = masteredItems[belt.id]?.get(t)?.keys?.toList().orEmpty()

        // ננקה cache
        masteredItems[belt.id]?.get(t)?.keys?.forEach { key ->
            masteredItems[belt.id]?.get(t)?.set(key, null)
        }

        // ננקה datastore
        viewModelScope.launch {
            items.forEach { ds.clearItemStatus(belt, t, it) }
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
        return if (t == "כללי") "" else t
    }

    // === אחוזי התקדמות לכל חגורה ===
    private val _progress = mutableStateOf<Map<Belt, Int>>(emptyMap())
    val progress: State<Map<Belt, Int>> = _progress

    /** קבלת מצב של פריט (Nullable: true/false/null) */

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
        val beltContent = CatalogData.data[belt] ?: return emptyList()
        val out = mutableListOf<CatalogEntry>()

        for (topic in beltContent.topics) {
            val topicTitle = topic.title
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

    fun getExternalDefensesForBelt(belt: Belt): List<String> {
        val entries = getCatalogEntriesForBelt(belt)
        return entries
            .asSequence()
            .filter { it.topicTitle.contains("הגנות") }
            .filter { e ->
                val raw = e.rawItem.lowercase()
                raw.contains("def:external") || raw.contains("def_external") || e.displayItem.contains("חיצונ")
            }
            .map { it.displayItem }
            .distinct()
            .toList()
    }

    fun getExerciseExplanationText(exerciseName: String): String? {
        val query = exerciseName.trim()
        if (query.isEmpty()) return null

        var foundBelt: Belt? = null
        var foundTopicTitle: String? = null
        var foundItemName: String? = null

        for (belt in Belt.order) {
            val entries = getCatalogEntriesForBelt(belt)
            val hit = entries.firstOrNull { it.displayItem.contains(query, ignoreCase = true) }
            if (hit != null) {
                foundBelt = belt
                foundTopicTitle = hit.topicTitle
                foundItemName = hit.displayItem
                break
            }
        }

        val belt = foundBelt ?: return null
        val topicTitle = foundTopicTitle ?: return null
        val itemName = foundItemName ?: query

        val beltHe = when (belt) {
            Belt.WHITE  -> "לבנה"
            Belt.YELLOW -> "צהובה"
            Belt.ORANGE -> "כתומה"
            Belt.GREEN  -> "ירוקה"
            Belt.BLUE   -> "כחולה"
            Belt.BROWN  -> "חומה"
            Belt.BLACK  -> "שחורה"
        }

        return buildString {
            appendLine("התרגיל \"$itemName\" מופיע בחגורה $beltHe בנושא \"$topicTitle\".")
            appendLine()
            appendLine("הנה הסבר כללי לביצוע נכון של התרגיל:")
            appendLine("1. התחל מעמידת מוצא יציבה, ברכיים מעט כפופות ומבט קדימה.")
            appendLine("2. בצע את התנועה לאט מספר פעמים כדי להבין את המסלול ואת הכיוון.")
            appendLine("3. שמור שהיד השנייה נשארת בהגנה בזמן הביצוע.")
            appendLine("4. נשימה רגועה וקצב אחיד — בלי לעצור נשימה באמצע.")
            appendLine("5. אחרי שהטכניקה נקייה, העלה בהדרגה מהירות ועוצמה.")
            appendLine("6. חזור תמיד לעמידת מוצא מוכנה.")
        }
    }

    init {
        recalcProgress()
    }
} // ✅ חשוב! לסיים את ה־class לפני ה־Factory

// ─────────────────────────────────────────────
// Factory עבור KmiViewModel (להישאר באותו קובץ)
// ─────────────────────────────────────────────
class KmiViewModelFactory(
    private val dataStoreManager: DataStoreManager,
    private val spTrainingSummary: SharedPreferences, // ✅ NEW
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KmiViewModel::class.java)) {
            val localRepo = TrainingSummaryLocalRepo(spTrainingSummary) // ✅ NEW
            return KmiViewModel(
                ds = dataStoreManager,
                trainingSummaryLocalRepo = localRepo
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}