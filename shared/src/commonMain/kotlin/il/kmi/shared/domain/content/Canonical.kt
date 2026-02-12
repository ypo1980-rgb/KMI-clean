package il.kmi.shared.domain.content

import il.kmi.shared.domain.Belt

/**
 * Canonical utilities for cross-platform stable behavior.
 * - One normalization (Hebrew-safe)
 * - One parsing of "def:*" tags
 * - One canonical ID generator
 */
object Canonical {

    // we reuse the same separators style you already use in ContentRepo
    private const val SEP = "::"
    private const val ESC = "∷"

    private fun encPart(s: String) = s.replace(SEP, ESC)

    fun String.normHeb(): String = this
        .replace("\u200F", "") // RLM
        .replace("\u200E", "") // LRM
        .replace("\u00A0", " ") // NBSP -> space
        .replace(Regex("[\u0591-\u05C7]"), "") // ניקוד
        // unify hyphens
        .replace('\u05BE', '-') // מקאף עברי ־
        .replace('\u2010', '-') // Hyphen
        .replace('\u2011', '-') // Non-Breaking Hyphen
        .replace('\u2012', '-') // Figure Dash
        .replace('\u2013', '-') // En Dash
        .replace('\u2014', '-') // Em Dash
        .replace('\u2015', '-') // Horizontal Bar
        .replace('\u2212', '-') // Minus
        .replace(Regex("\\s*-\\s*"), "-")
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()

    data class ParsedItem(
        val raw: String,
        val displayName: String,
        val tag: String // normalized: "def:external:kick" etc, or ""
    )

    // Supports:
    // 1) "def:external:punch::NAME"
    // 2) "NAME::def:external:punch"
    // 3) "def_external_punches::NAME"
    // 4) "NAME::def_external_punches"
    fun parseDefenseTagAndName(raw: String): ParsedItem {
        val s = raw.trim()
        if (!s.contains("::")) {
            // no explicit tag, infer from name
            val display = s
            return ParsedItem(raw = raw, displayName = display, tag = inferDefenseTagFromDisplay(display))
        }

        val left = s.substringBeforeLast("::").trim()
        val right = s.substringAfterLast("::").trim()

        fun isTag(x: String): Boolean =
            x.startsWith("def:", ignoreCase = true) || x.startsWith("def_", ignoreCase = true)

        val (tagRaw, display) = when {
            isTag(left) -> left to right
            isTag(right) -> right to left
            else -> null to s.substringAfterLast("::").ifBlank { s }
        }

        val normalized = normalizeDefenseTag(tagRaw)
        val finalTag = if (normalized.isNotBlank()) normalized else inferDefenseTagFromDisplay(display)

        return ParsedItem(raw = raw, displayName = display.trim(), tag = finalTag)
    }

    private fun normalizeDefenseTag(tagRaw: String?): String {
        val t = tagRaw?.trim().orEmpty().lowercase()
        if (t.isBlank()) return ""
        if (t.startsWith("def:")) return t

        return when (t) {
            "def_external_punches" -> "def:external:punch"
            "def_internal_punches" -> "def:internal:punch"
            "def_external_kicks" -> "def:external:kick"
            "def_internal_kicks" -> "def:internal:kick"
            else -> t
        }
    }

    private fun inferDefenseTagFromDisplay(displayName: String): String {
        val d = displayName.normHeb()

        val isExternal = d.contains("חיצונ")
        val isInternal = d.contains("פנימ")
        val isKick = d.contains("בעיט")
        val isPunch = d.contains("אגרו") || d.contains("אגרוף") || d.contains("מכות אגרוף")

        return when {
            isExternal && isKick -> "def:external:kick"
            isInternal && isKick -> "def:internal:kick"
            isExternal && isPunch -> "def:external:punch"
            isInternal && isPunch -> "def:internal:punch"
            else -> ""
        }
    }

    /**
     * Canonical item ID – stable across platforms and safe for persistence.
     * IMPORTANT: we base canonical on displayName (after tag removal) + belt + topic + subtopic.
     */
    fun canonicalItemId(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String?,
        rawItem: String
    ): String {
        val parsed = parseDefenseTagAndName(rawItem)
        val topicN = topicTitle.normHeb()
        val subN = (subTopicTitle ?: "").normHeb()
        val itemN = parsed.displayName.normHeb()

        // We keep it readable and stable:
        // beltId::topic::sub::item
        return listOf(
            belt.id,
            encPart(topicN),
            encPart(subN),
            encPart(itemN)
        ).joinToString(SEP)
    }
}
