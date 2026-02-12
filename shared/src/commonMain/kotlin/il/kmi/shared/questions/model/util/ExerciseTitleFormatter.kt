package il.kmi.shared.questions.model.util

object ExerciseTitleFormatter {

    /** מחזיר שם לתצוגה בעברית, גם אם raw הוא "=def:internal:punch" */
    fun displayName(raw: String): String {
        val s0 = raw.trim()
        if (s0.isBlank()) return ""

        // אם יש "::" ויש באמת שם — ניקח את ה"שם" וננקה
        if ("::" in s0) {
            val left = s0.substringBefore("::").trim()
            val right = s0.substringAfter("::").trim()

            val leftIsDef = looksLikeDefCode(left)
            val rightIsDef = looksLikeDefCode(right)

            val candidate = when {
                leftIsDef && !rightIsDef -> right.ifBlank { left }
                rightIsDef && !leftIsDef -> left.ifBlank { right }
                else -> right.ifBlank { left }
            }

            if (looksLikeDefCode(candidate)) {
                return defCodeToHebrew(candidate) ?: cleanupHuman(candidate)
            }

            return cleanupHuman(candidate)
        }

        if (looksLikeDefCode(s0) || s0.startsWith("=") || s0.contains('=')) {
            defCodeToHebrew(s0)?.let { return it }
            return cleanupHuman(s0)
        }

        return cleanupHuman(s0)
    }

    private fun looksLikeDefCode(x0: String): Boolean {
        val x = x0.trim().removePrefix("=").trim()
        return x.startsWith("def:", true) ||
                x.startsWith("def_", true) ||
                x.startsWith("def.", true) ||
                x.startsWith("exercise:", true) ||
                x.startsWith("exercise.", true)
    }

    /** ניקוי "אנושי" עדין (רווחים/תווים), לא ממיר לעברית */
    private fun cleanupHuman(t0: String): String {
        var t = t0.trim()
        t = t.substringAfter('=', t).trim()
        t = t.replace(Regex("\\s+"), " ").trim()
        return t
    }

    /**
     * ממיר def:* למחרוזת עברית כללית:
     * def:internal:punch -> "הגנה מאגרוף פנימי"
     * def:external:kick -> "הגנה מבעיטה חיצונית"
     * def:knife:stab    -> "הגנה מדקירה בסכין"
     */
    private fun defCodeToHebrew(code0: String): String? {
        val c = code0.trim()
            .removePrefix("=")
            .trim()
            .lowercase()   // ✅ בלי Locale — KMP-safe
            .replace("def_", "def:")
            .replace("def.", "def:")
            .replace("exercise_", "exercise:")
            .replace("exercise.", "exercise:")
            .replace("exercise:", "def:")
            .replace("punches", "punch")
            .replace("kicks", "kick")

        if (!c.startsWith("def:")) return null

        val tokens = c.removePrefix("def:")
            .split(':', '.', '_')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (tokens.isEmpty()) return null

        fun tokHeb(t: String): String? = when (t) {
            "internal" -> "פנימי"
            "external" -> "חיצוני"
            "punch" -> "אגרוף"
            "straight" -> "ישר"
            "hook" -> "וו"
            "uppercut" -> "אפרקאט"
            "kick" -> "בעיטה"
            "front" -> "קדמית"
            "round" -> "עגולה"
            "low" -> "נמוכה"
            "knee" -> "ברך"
            "elbow" -> "מרפק"
            "choke" -> "חניקה"
            "grab" -> "אחיזה"
            "hold" -> "אחיזה"
            "knife" -> "סכין"
            "stab" -> "דקירה"
            "slash" -> "שיסוף"
            "stick" -> "מקל"
            "baton" -> "אלה"
            else -> null
        }

        val hasKnife = tokens.contains("knife")
        val hasStick = tokens.contains("stick") || tokens.contains("baton")
        val hasPunch = tokens.contains("punch")
        val hasKick  = tokens.contains("kick")

        return when {
            hasKnife && tokens.contains("stab") -> "הגנה מדקירה בסכין"
            hasKnife && tokens.contains("slash") -> "הגנה משיסוף בסכין"
            hasKnife -> "הגנה מסכין"

            hasStick -> "הגנה ממקל"

            hasPunch -> {
                val side = when {
                    tokens.contains("internal") -> "פנימי"
                    tokens.contains("external") -> "חיצוני"
                    else -> ""
                }
                if (side.isNotBlank()) "הגנה מאגרוף $side" else "הגנה מאגרוף"
            }

            hasKick -> {
                val side = when {
                    tokens.contains("internal") -> "פנימית"
                    tokens.contains("external") -> "חיצונית"
                    else -> ""
                }
                if (side.isNotBlank()) "הגנה מבעיטה $side" else "הגנה מבעיטה"
            }

            else -> {
                val translated = tokens.mapNotNull(::tokHeb)
                if (translated.isEmpty()) null
                else "הגנה מ" + translated.joinToString(" ")
            }
        }
    }
}
