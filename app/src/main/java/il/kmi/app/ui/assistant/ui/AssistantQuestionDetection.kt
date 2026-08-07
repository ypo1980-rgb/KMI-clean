package il.kmi.app.ui.assistant.ui

import il.kmi.shared.domain.Belt

internal fun detectIntent(
    question: String
): String {
    val q = question.lowercase()

    return when {
        "הסבר" in q ||
                "explain" in q ->
            "EXPLAIN_EXERCISE"

        "רשימת תרגילים" in q ||
                "list exercises" in q ->
            "LIST_EXERCISES"

        "האימון הבא" in q ||
                "next training" in q ->
            "NEXT_TRAINING"

        else ->
            "UNKNOWN"
    }
}

internal fun detectBeltEnum(
    text: String
): Belt? {
    val normalized =
        text
            .lowercase()
            .replace("_", " ")
            .replace("-", " ")
            .replace("־", " ")
            .replace("–", " ")
            .replace("—", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    return when {
        "לבן" in normalized ||
                "לבנה" in normalized ||
                "white" in normalized ->
            Belt.WHITE

        "צהוב" in normalized ||
                "צהובה" in normalized ||
                "yellow" in normalized ->
            Belt.YELLOW

        "כתום" in normalized ||
                "כתומה" in normalized ||
                "orange" in normalized ->
            Belt.ORANGE

        "ירוק" in normalized ||
                "ירוקה" in normalized ||
                "green" in normalized ->
            Belt.GREEN

        "כחול" in normalized ||
                "כחולה" in normalized ||
                "blue" in normalized ->
            Belt.BLUE

        "חום" in normalized ||
                "חומה" in normalized ||
                "brown" in normalized ->
            Belt.BROWN

        "שחור" in normalized ||
                "שחורה" in normalized ||
                "black" in normalized ->
            Belt.BLACK

        else ->
            null
    }
}

internal fun beltDisplayLabel(
    belt: Belt,
    isEnglish: Boolean
): String {
    return when (belt) {
        Belt.WHITE ->
            if (isEnglish) {
                "White belt"
            } else {
                "חגורה לבנה"
            }

        Belt.YELLOW ->
            if (isEnglish) {
                "Yellow belt"
            } else {
                "חגורה צהובה"
            }

        Belt.ORANGE ->
            if (isEnglish) {
                "Orange belt"
            } else {
                "חגורה כתומה"
            }

        Belt.GREEN ->
            if (isEnglish) {
                "Green belt"
            } else {
                "חגורה ירוקה"
            }

        Belt.BLUE ->
            if (isEnglish) {
                "Blue belt"
            } else {
                "חגורה כחולה"
            }

        Belt.BROWN ->
            if (isEnglish) {
                "Brown belt"
            } else {
                "חגורה חומה"
            }

        Belt.BLACK ->
            if (isEnglish) {
                "Black belt"
            } else {
                "חגורה שחורה"
            }

        else ->
            belt.name
    }
}