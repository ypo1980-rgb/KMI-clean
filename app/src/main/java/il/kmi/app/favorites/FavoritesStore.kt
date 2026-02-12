package il.kmi.app.favorites

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoritesStore {

    private const val PREFS_NAME = "kmi_favorites"
    private const val KEY_SET = "favorites_ids"

    private lateinit var sp: SharedPreferences

    private val _favoritesFlow = MutableStateFlow<Set<String>>(emptySet())
    val favoritesFlow: StateFlow<Set<String>> = _favoritesFlow.asStateFlow()

    // ✅ מזהה יציב: displayName -> ניקוי -> lowercase
    private fun normalizeFavId(raw: String): String =
        il.kmi.shared.questions.model.util.ExerciseTitleFormatter
            .displayName(raw)
            .trim()
            .replace("\u200F", "") // RLM
            .replace("\u200E", "") // LRM
            .replace("\u00A0", " ") // NBSP
            .replace(Regex("[\u0591-\u05C7]"), "") // ניקוד
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()

    fun init(context: Context) {
        if (::sp.isInitialized) return

        sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loaded = sp.getStringSet(KEY_SET, emptySet()) ?: emptySet()

        // ✅ MIGRATION: מנרמלים את מה שכבר נשמר כדי שיתאים לכל הרשימות החדשות
        val normalized = loaded.map { normalizeFavId(it) }.toSet()

        _favoritesFlow.value = normalized

        if (normalized != loaded) {
            sp.edit().putStringSet(KEY_SET, normalized).apply()
        }
    }

    fun isFavorite(id: String): Boolean =
        _favoritesFlow.value.contains(normalizeFavId(id))

    fun toggle(id: String) {
        val nid = normalizeFavId(id)
        val cur = _favoritesFlow.value.toMutableSet()
        if (!cur.add(nid)) cur.remove(nid)

        _favoritesFlow.value = cur
        sp.edit().putStringSet(KEY_SET, cur).apply()
    }

    fun clearAll() {
        _favoritesFlow.value = emptySet()
        sp.edit().remove(KEY_SET).apply()
    }
}
