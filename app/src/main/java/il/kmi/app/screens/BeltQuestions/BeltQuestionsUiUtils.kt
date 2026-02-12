package il.kmi.app.screens.BeltQuestions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.SubTopic
import il.kmi.shared.domain.TopicsEngine
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.prefs.KmiPrefs

/* -------------------------- עוזר: padding חוצה-פלטפורמה -------------------------- */
@Suppress("unused")
fun Modifier.statusBarsPaddingCompat(): Modifier = this

@Suppress("unused")
fun Modifier.navigationBarsPaddingCompat(): Modifier = this

/* ----------------------------- פענוח %XX ללא java.net ----------------------------- */
internal fun percentDecode(s: String): String {
    val out = StringBuilder(s.length)
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '%' && i + 2 < s.length) {
            val hex = s.substring(i + 1, i + 3)
            val v = hex.toIntOrNull(16)
            if (v != null) {
                out.append(v.toChar())
                i += 3
                continue
            }
        }
        out.append(if (c == '+') ' ' else c)
        i++
    }
    return out.toString()
}

internal fun parseSearchKey(key: String): Triple<Belt, String, String> {
    val parts0 = when {
        '|' in key -> key.split('|', limit = 3)
        "::" in key -> key.split("::", limit = 3)
        '/' in key -> key.split('/', limit = 3)
        else -> listOf("", "", "")
    }
    val parts = (parts0 + listOf("", "", "")).take(3)
    val belt = Belt.fromId(parts[0]) ?: Belt.WHITE
    val topic = percentDecode(parts[1])
    val item = percentDecode(parts[2])
    return Triple(belt, topic, item)
}

/* --------------------------- API משותף לשאילת תכנים --------------------------- */
internal fun Belt.toSharedBelt(): il.kmi.shared.domain.Belt {
    val id0 = this.id.trim()
    il.kmi.shared.domain.Belt.fromId(id0)?.let { return it }
    il.kmi.shared.domain.Belt.fromId(id0.lowercase())?.let { return it }

    val candidates = listOf(
        id0.lowercase(),
        id0.lowercase().removePrefix("belt_"),
        id0.lowercase().removePrefix("belt-"),
        id0.lowercase().replace("-", "_"),
        id0.lowercase().replace("_", "-")
    ).distinct()

    for (c in candidates) {
        il.kmi.shared.domain.Belt.fromId(c)?.let { return it }
        il.kmi.shared.domain.Belt.values()
            .firstOrNull { it.name.equals(c, ignoreCase = true) }
            ?.let { return it }
    }

    il.kmi.shared.domain.Belt.values()
        .firstOrNull { it.name.equals(this.name, ignoreCase = true) }
        ?.let { return it }

    return il.kmi.shared.domain.Belt.WHITE
}

/* ---------------------- Details לנושא: תתי־נושאים + ספירת תרגילים ---------------------- */
internal fun topicDetailsFor(belt: Belt, topicTitle: String): TopicDetails {
    val details = TopicsEngine.topicDetailsFor(belt, topicTitle.trim())

    val topicTrim = topicTitle.trim()
    val cleanSubs = details.subTitles
        .map { it.trim() }
        .filter { it.isNotBlank() && it != topicTrim }
        .distinct()

    return TopicDetails(
        itemCount = details.itemCount,
        subTitles = cleanSubs
    )
}

/* ----------------------------- topicTitlesFor (מקור אמת) ----------------------------- */
internal fun topicTitlesForCompat(belt: Belt): List<String> {
    return TopicsEngine.topicTitlesFor(belt)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

/* ----------------------------- תתי־נושאים ----------------------------- */
internal fun subTopicsForCompat(belt: Belt, topicTitle: String): List<SubTopic> {
    return TopicsEngine.subTopicTitlesFor(belt, topicTitle.trim())
        .map { it.trim() }
        .filter { it.isNotBlank() && it != topicTitle.trim() }
        .distinct()
        .map { t -> SubTopic(title = t, items = emptyList()) }
}

/* ----------------------------- prefs compat ----------------------------- */
internal fun KmiPrefs.getStringCompat(key: String): String? = try {
    val c = this::class.java
    val m1 = c.methods.firstOrNull { it.name == "getString" && it.parameterTypes.size == 1 }
    val m2 = c.methods.firstOrNull { it.name == "getString" && it.parameterTypes.size == 2 }
    when {
        m1 != null -> m1.invoke(this, key) as? String
        m2 != null -> m2.invoke(this, key, null) as? String
        else -> null
    }
} catch (_: Exception) {
    null
}

internal fun KmiPrefs.putStringCompat(key: String, value: String?) = try {
    val c = this::class.java
    val mPut = c.methods.firstOrNull { it.name == "putString" && it.parameterTypes.size == 2 }
    val mSet = c.methods.firstOrNull { it.name == "setString" && it.parameterTypes.size == 2 }
    when {
        mPut != null -> mPut.invoke(this, key, value)
        mSet != null -> mSet.invoke(this, key, value)
        else -> Unit
    }
} catch (_: Exception) {
    Unit
}

internal const val PREF_FAV_SUBJECTS: String = "fav_subject_ids_v1"

internal fun loadFavoriteSubjectIds(kmiPrefs: KmiPrefs): Set<String> {
    val raw = kmiPrefs.getStringCompat(PREF_FAV_SUBJECTS).orEmpty().trim()
    if (raw.isBlank()) return emptySet()
    return raw.split('|').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}

internal fun saveFavoriteSubjectIds(kmiPrefs: KmiPrefs, ids: Set<String>) {
    val raw = ids.joinToString("|")
    kmiPrefs.putStringCompat(PREF_FAV_SUBJECTS, raw)
}

/* ----------------------------- עזר: למצוא הסבר אמיתי ----------------------------- */
internal fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String
): String {
    val display = ExerciseTitleFormatter.displayName(rawItem).ifBlank { rawItem }.trim()

    fun String.clean(): String = this
        .replace('–', '-')
        .replace('־', '-')
        .replace("  ", " ")
        .trim()

    val candidates = buildList {
        add(rawItem)
        add(display)
        add(display.clean())
        add(display.substringBefore("(").trim().clean())
    }.distinct()

    for (candidate in candidates) {
        val got = il.kmi.app.domain.Explanations.get(belt, candidate).trim()
        if (got.isNotBlank()
            && !got.startsWith("הסבר מפורט על")
            && !got.startsWith("אין כרגע")
        ) {
            return if ("::" in got) got.substringAfter("::").trim() else got
        }
    }
    return "אין כרגע הסבר לתרגיל הזה."
}

/* -------------------------- Modifiers עיצוביים -------------------------- */
internal fun Modifier.circleGlow(
    color: Color,
    radius: Dp,
    intensity: Float = 0.55f
) = this.drawBehind {
    val rPx = radius.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = intensity), Color.Transparent),
            center = this.center,
            radius = rPx
        ),
        radius = rPx,
        center = this.center
    )
}

/* קליק ללא ריפל – לא חוסם מחוות גרירה של ההורה */
internal fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    clickable(
        interactionSource = interaction,
        indication = null
    ) { onClick() }
}

/* ========= עזר: הדגשת "עמידת מוצא ..." עד פסיק/נקודה ========= */
internal fun buildExplanationWithStanceHighlight(
    source: String,
    stanceColor: Color
): AnnotatedString {
    val marker = "עמידת מוצא"
    val idx = source.indexOf(marker)
    if (idx < 0) return AnnotatedString(source)

    val sentenceEndExclusive = run {
        val endIdx = source.indexOfAny(charArrayOf('.', ','), startIndex = idx)
        if (endIdx == -1) source.length else endIdx + 1
    }

    val before = source.substring(0, idx)
    val stanceSentence = source.substring(idx, sentenceEndExclusive)
    val after = source.substring(sentenceEndExclusive)

    return buildAnnotatedString {
        append(before)
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
        append(after)
    }
}

/* ----------------------------- טקסט “N תרגילים” ----------------------------- */
internal fun formatCount(n: Int): String = when {
    n <= 0 -> "0 תרגילים"
    n == 1 -> "תרגיל 1"
    else -> "$n תרגילים"
}
