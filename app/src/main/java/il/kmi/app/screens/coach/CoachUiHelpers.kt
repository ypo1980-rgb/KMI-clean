package il.kmi.app.screens.coach

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

internal fun beltColorForStats(
    belt: String
): Color {
    val normalized = belt.trim()

    return when {
        normalized.contains("לבנ") ||
                normalized.contains(
                    "white",
                    ignoreCase = true
                ) ->
            Color(0xFFE5E7EB)

        normalized.contains("צהוב") ||
                normalized.contains(
                    "yellow",
                    ignoreCase = true
                ) ->
            Color(0xFFFACC15)

        normalized.contains("כתומ") ||
                normalized.contains(
                    "orange",
                    ignoreCase = true
                ) ->
            Color(0xFFF97316)

        normalized.contains("ירוק") ||
                normalized.contains(
                    "green",
                    ignoreCase = true
                ) ->
            Color(0xFF22C55E)

        normalized.contains("כחול") ||
                normalized.contains(
                    "blue",
                    ignoreCase = true
                ) ->
            Color(0xFF3B82F6)

        normalized.contains("חומ") ||
                normalized.contains(
                    "brown",
                    ignoreCase = true
                ) ->
            Color(0xFF8B5A2B)

        normalized.contains("שחור") ||
                normalized.contains(
                    "black",
                    ignoreCase = true
                ) ->
            Color(0xFF111111)

        else ->
            Color(0xFF7C3AED)
    }
}

internal fun coachTr(
    isEnglish: Boolean,
    he: String,
    en: String
): String {
    return if (isEnglish) en else he
}

internal fun coachTextAlign(
    isEnglish: Boolean
): TextAlign {
    return if (isEnglish) {
        TextAlign.Left
    } else {
        TextAlign.Right
    }
}

internal fun coachHorizontalAlignment(
    isEnglish: Boolean
): Alignment.Horizontal {
    return if (isEnglish) {
        Alignment.Start
    } else {
        Alignment.End
    }
}

internal fun coachBeltNameForUi(
    beltName: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return beltName

    return when (beltName.trim()) {
        "לבנה",
        "חגורה לבנה" ->
            "White"

        "צהובה",
        "חגורה צהובה" ->
            "Yellow"

        "כתומה",
        "חגורה כתומה" ->
            "Orange"

        "ירוקה",
        "חגורה ירוקה" ->
            "Green"

        "כחולה",
        "חגורה כחולה" ->
            "Blue"

        "חומה",
        "חגורה חומה" ->
            "Brown"

        "שחורה",
        "חגורה שחורה" ->
            "Black"

        "ללא דרגה" ->
            "No rank"

        else ->
            beltName
    }
}

internal fun coachSectionTitleForUi(
    title: String,
    isEnglish: Boolean
): String {
    return when (title) {
        "השתלמויות" ->
            coachTr(
                isEnglish,
                "השתלמויות",
                "Seminars"
            )

        "מחנות אימונים" ->
            coachTr(
                isEnglish,
                "מחנות אימונים",
                "Training camps"
            )

        "הסמכות" ->
            coachTr(
                isEnglish,
                "הסמכות",
                "Certifications"
            )

        else ->
            title
    }
}

internal fun coachDateItemNameForUi(
    itemName: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return itemName

    val number = Regex("""\d+""")
        .find(itemName)
        ?.value
        .orEmpty()

    return when {
        itemName.startsWith("השתלמות") ->
            "Seminar $number"

        itemName.startsWith("מחנה אימונים") ->
            "Training camp $number"

        itemName.startsWith("הסמכה") ->
            "Certification $number"

        else ->
            itemName
    }
}

internal fun coachDateSectionIcon(
    title: String
): String {
    return when (title) {
        "השתלמויות" -> "🎓"
        "מחנות אימונים" -> "👥"
        "הסמכות" -> "🏅"
        else -> "⌄"
    }
}

internal fun coachDateSectionAccent(
    title: String
): Color {
    return when (title) {
        "השתלמויות" ->
            Color(0xFF7C3AED)

        "מחנות אימונים" ->
            Color(0xFF2563EB)

        "הסמכות" ->
            Color(0xFF0891B2)

        else ->
            Color(0xFF6D56B8)
    }
}