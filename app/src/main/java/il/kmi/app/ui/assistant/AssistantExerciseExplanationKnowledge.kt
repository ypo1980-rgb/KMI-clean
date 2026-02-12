package il.kmi.app.ui.assistant

import android.util.Log
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.Explanations
import il.kmi.app.search.asSharedRepo
import il.kmi.app.search.toShared
import il.kmi.shared.search.KmiSearch
import il.kmi.shared.search.SearchHit
import java.util.Locale

/**
 * מנוע ייעודי לשאלות "הסבר לתרגיל".
 * תופס ניסוחים שונים, מחלץ שם תרגיל, ומחזיר הסבר מתוך Explanations.
 */
object AssistantExerciseExplanationKnowledge {

    private const val TAG = "KMI_EXPLAIN"

    // ─────────────────────────────────────────────────────────────
    // 1) טריגרים לשאלות הסבר
    // ─────────────────────────────────────────────────────────────
    private val explainTriggers = listOf(
        // ✅ תוספות שביקשת
        "תסביר בבקשה על","איך עושים",
        "תן בבקשה הסבר על",
        "תתן בבקשה את ההסבר על",
        "תתן הסבר","תן לי בבקשה הסבר",

        // הקיים
        "הסבר", "תסביר", "תסבירי", "תסביר לי",
        "תן הסבר", "תני הסבר", "תן לי הסבר", "תני לי הסבר",
        "תן פירוט", "תני פירוט", "פירוט",

        "איך עושים", "איך לעשות", "איך לבצע", "איך מבצעים", "איך לבצע את",
        "שלב שלב", "צעד צעד", "הדרכה", "הדריך",

        "מה זה", "מהו", "מה היא", "מה פירוש", "מה המשמעות",
        "תן דוגמה", "תני דוגמה", "דוגמה", "דוגמא",
        "טיפים", "דגשים", "מה חשוב", "מה לשים לב"
    )

    // ✅ רמזים שמדובר בתרגיל גם בלי המילה "תרגיל"
    private val mentionsExerciseTokens = listOf(
        "תרגיל", "תרגילים",
        "טכניקה", "טכניקת",
        "בעיטה", "אגרוף",
        "הגנה", "חניקה",
        "הטלה", "בריח",
        "שחרור", "אחיזה"
    )

    // מילים מיותרות בסוף שם התרגיל
    private val tailNoise = listOf(
        "הזה", "הזאת",
        "בבקשה", "תודה",
        "תן", "תני",
        "הסבר", "פירוט",
        "שלב", "איך", "מה"
    )

    // ─────────────────────────────────────────────────────────────
    // 2) API חיצוני
    // ─────────────────────────────────────────────────────────────
    data class ExplainRequest(
        val rawQuestion: String,
        val exerciseName: String,
        val belt: Belt?
    )

    fun answer(question: String, preferredBelt: Belt? = null): String? {
        val req = tryParse(question, preferredBelt) ?: return null
        return answer(req)
    }

    fun answer(req: ExplainRequest): String? {
        // אם לא הועברה חגורה, ננסה "הכי הגיוני" ואז נרחיב לכל החגורות
        val primaryBelt = req.belt ?: Belt.YELLOW

        // 1) ניסיון ישיר (וגם עם תיקוני מינימליים למחרוזת)
        val direct = findExplanationAcrossBelts(
            primaryBelt = primaryBelt,
            exerciseName = req.exerciseName,
            allowAllBelts = (req.belt == null)
        )

        if (direct != null) {
            val (beltFound, expRaw) = direct
            val cleaned = expRaw.substringAfter("::", expRaw).trim()
            return "ההסבר לתרגיל \"${req.exerciseName}\":\n\n$cleaned\n\nהאם אני יכול לעזור לך בעוד משהו?"
        }

        // 2) fallback – חיפוש חכם ומיפוי ה-hit לשם מפתח שמתאים ל-Explanations
        val best = searchBestHit(req.rawQuestion, primaryBelt)
        if (best != null) {
            val hitBelt = runCatching { Belt.valueOf(best.belt.name) }.getOrElse { primaryBelt }
            val rawItem = best.item ?: return null

            val displayKey = canonToExplanationKey(rawItem)
            Log.d(TAG, "Explain fallback hit: belt=$hitBelt rawItem='$rawItem' -> key='$displayKey'")

            val exp = findExplanationAcrossBelts(
                primaryBelt = hitBelt,
                exerciseName = displayKey,
                allowAllBelts = false
            )?.second

            if (!exp.isNullOrBlank() && !looksLikeNoData(exp)) {
                val cleaned = exp.substringAfter("::", exp).trim()
                return "ההסבר לתרגיל \"$displayKey\":\n\n$cleaned\n\nהאם אני יכול לעזור לך בעוד משהו?"
            }
        }

        Log.d(TAG, "No explanation found. belt=${req.belt} name='${req.exerciseName}' q='${req.rawQuestion}'")
        return null
    }

    /**
     * מנסה להביא הסבר מה-Explanations:
     * - מנסה את השם כמו שהוא
     * - מנסה גרסה מנורמלת מינימלית (דאשים/רווחים)
     * - ואם לא הועברה חגורה: מנסה גם בכל החגורות עד שנמצא
     */
    private fun findExplanationAcrossBelts(
        primaryBelt: Belt,
        exerciseName: String,
        allowAllBelts: Boolean
    ): Pair<Belt, String>? {

        fun tryGet(belt: Belt, name: String): String? {
            val v = runCatching { Explanations.get(belt, name).trim() }.getOrNull()
            if (v.isNullOrBlank() || looksLikeNoData(v)) return null
            return v
        }

        val candidates = listOf(
            exerciseName.trim(),
            exerciseName.trim()
                .replace('–', '-')
                .replace('—', '-')
                .replace("  ", " ")
                .trim()
        ).distinct()

        // קודם בחגורה הראשית
        for (c in candidates) {
            val v = tryGet(primaryBelt, c)
            if (v != null) {
                Log.d(TAG, "Explain direct: belt=$primaryBelt key='$c' ✔")
                return primaryBelt to v
            }
            Log.d(TAG, "Explain direct miss: belt=$primaryBelt key='$c'")
        }

        if (!allowAllBelts) return null

        // אם לא הוגדרה חגורה — ננסה גם בכל החגורות (חוסך מצב שהמשתמש אומר תרגיל ירוק ואתה מחפש בצהוב)
        val beltsToTry = listOf(
            Belt.GREEN, Belt.ORANGE, Belt.YELLOW, Belt.BLUE, Belt.BROWN, Belt.BLACK
        ).distinct()

        for (b in beltsToTry) {
            if (b == primaryBelt) continue
            for (c in candidates) {
                val v = tryGet(b, c)
                if (v != null) {
                    Log.d(TAG, "Explain found in other belt: belt=$b key='$c' ✔")
                    return b to v
                }
            }
        }

        return null
    }

    /**
     * ממיר "item" קנוני שמגיע מה-ContentRepo (לפעמים יש ::tag)
     * לשם שמופיע כמפתח ב-Explanations (בדרך כלל ללא טאג).
     */
    private fun canonToExplanationKey(rawItem: String): String {
        val s = rawItem.trim()
        if (!s.contains("::")) return s

        val parts = s.split("::")
        if (parts.size != 2) return s.substringAfterLast("::").trim()

        val a = parts[0].trim()
        val b = parts[1].trim()

        // אם צד אחד נראה כמו טאג לטיני/underscore – הצד השני הוא שם התרגיל
        val latinTag = Regex("^[a-z0-9_\\-]+$", RegexOption.IGNORE_CASE)
        return when {
            latinTag.matches(a) && !latinTag.matches(b) -> b
            latinTag.matches(b) && !latinTag.matches(a) -> a
            else -> b // ברירת מחדל: כמו שהיה אצלך
        }
    }

    fun tryParse(question: String, preferredBelt: Belt? = null): ExplainRequest? {
        val norm = normalize(question)

        val hasTrigger = explainTriggers.any { trig ->
            norm.startsWith(trig) || (" $trig " in " $norm ")
        }
        if (!hasTrigger) return null

        val name = extractExerciseName(question, norm)
            ?.takeIf { it.length >= 2 }
            ?: return null

        return ExplainRequest(
            rawQuestion = question,
            exerciseName = name,
            belt = preferredBelt
        )
    }

    // ─────────────────────────────────────────────────────────────
    // 3) חילוץ שם תרגיל
    // ─────────────────────────────────────────────────────────────
    private fun extractExerciseName(original: String, norm: String): String? {

        extractQuoted(original)?.let {
            val cleaned = cleanName(it)
            if (cleaned.isNotBlank()) return cleaned
        }

        explainTriggers
            .sortedByDescending { it.length }
            .firstOrNull { norm.startsWith(it) }
            ?.let { trig ->
                val candidate = cleanName(norm.removePrefix(trig).trim())
                if (candidate.isNotBlank()) return candidate
            }

        val candidates = listOfNotNull(
            afterToken(norm, "תרגיל"),
            afterToken(norm, "טכניקה"),
            afterToken(norm, "טכניקת"),
            afterToken(norm, "הסבר על"),
            afterToken(norm, "הסבר ל"),
            afterColon(original)
        ).map { cleanName(it) }
            .filter { it.isNotBlank() }

        val best = candidates.maxByOrNull { it.length }

        if (best.isNullOrBlank()) {
            if (mentionsExerciseTokens.any { it in norm }) {
                val cleaned = cleanName(norm)
                if (cleaned.length >= 2) return cleaned
            }
        }

        return best
    }

    private fun afterToken(norm: String, token: String): String? {
        val idx = norm.indexOf(token)
        if (idx < 0) return null
        return norm.substring(idx + token.length).trim()
    }

    private fun afterColon(original: String): String? {
        val idx = original.indexOf(':')
        if (idx < 0) return null
        return original.substring(idx + 1).trim()
    }

    private fun cleanName(s: String): String {
        var t = s.trim()

        t = t.replace("?", " ")
            .replace("!", " ")
            .replace(".", " ")
            .trim()

        t = t.removePrefix("את").removePrefix("על").removePrefix("של").trim()

        tailNoise.forEach { tail ->
            if (t.endsWith(" $tail")) t = t.removeSuffix(" $tail").trim()
            if (t == tail) t = ""
        }

        return t
    }

    private fun extractQuoted(original: String): String? {
        val i1 = original.indexOf('"')
        if (i1 >= 0) {
            val i2 = original.indexOf('"', i1 + 1)
            if (i2 > i1) return original.substring(i1 + 1, i2)
        }

        val h1 = original.indexOf('״')
        if (h1 >= 0) {
            val h2 = original.indexOf('״', h1 + 1)
            if (h2 > h1) return original.substring(h1 + 1, h2)
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────
    // 4) חיפוש חכם
    // ─────────────────────────────────────────────────────────────
    private fun searchBestHit(question: String, belt: Belt?): SearchHit? {
        return try {
            val repo = ContentRepo.asSharedRepo()
            KmiSearch.search(repo, question, belt?.toShared()).firstOrNull()
        } catch (_: Throwable) {
            null
        }
    }

    private fun looksLikeNoData(text: String): Boolean {
        val t = normalize(text)
        return t.startsWith("אין כרגע") || t.startsWith("הסבר מפורט על")
    }

    private fun normalize(s: String): String {
        return HebrewNormalize.normalize(s)
            .lowercase(Locale("he", "IL"))
            .replace('–', '-')
            .replace('־', '-')
            .replace("  ", " ")
            .trim()
    }
}
