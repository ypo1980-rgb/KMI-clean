package il.kmi.app.screens

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.AppSubTopicRegistry
import il.kmi.app.domain.ContentRepo
import il.kmi.shared.domain.content.HardSectionsCatalog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.activity.compose.BackHandler
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.ext.lightColor
import il.kmi.app.domain.ExerciseExplanationResolver
import il.kmi.app.domain.ExerciseCountProvider
import il.kmi.shared.domain.content.HardSectionsCatalog.itemsFor
import il.kmi.shared.domain.content.HardSectionsCatalog.totalItemsCount
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.shared.domain.content.ExerciseTitlesEn
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.shared.localization.AppLanguage
import androidx.compose.ui.platform.LocalContext
import il.kmi.app.ui.dialogs.ExerciseExplanationDialog
import il.kmi.app.ui.dialogs.ExerciseNoteEditorDialog

//===========================================================================

private fun topicLookupAliases(topicTitle: String): List<String> {
    val raw = topicTitle.trim()

    val splitParts = raw
        .replace("/", " / ")
        .replace("־", " ")
        .split("ו", "/", ",")
        .map { it.trim() }
        .filter { it.isNotBlank() && it != raw }

    return when (raw) {
        "בלימות וגלגולים" -> listOf(
            "בלימות וגלגולים",
            "גלגולים ובלימות",
            "בלימות",
            "גלגולים",
            "נפילות וגלגולים",
            "נפילות",
            "גלגול",
            "בלימה"
        )

        "גלגולים ובלימות" -> listOf(
            "גלגולים ובלימות",
            "בלימות וגלגולים",
            "בלימות",
            "גלגולים",
            "נפילות וגלגולים",
            "נפילות",
            "גלגול",
            "בלימה"
        )

        else -> listOf(raw) + splitParts
    }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun hardDisplayTitleFallback(raw: String): String {
    return when (raw.trim()) {
        "releases" -> "שחרורים"
        "releases_hands_hair_shirt" -> "שחרור מתפיסות ידיים / שיער / חולצה"
        "releases_chokes" -> "שחרור מחניקות"
        "releases_hugs" -> "שחרור מחביקות"
        "releases_hugs_body" -> "חביקות גוף"
        "releases_hugs_neck" -> "חביקות צואר"
        "releases_hugs_arm" -> "חביקות זרוע"
        "knife_defense" -> "הגנות מסכין"
        "knife_rifle_defense" -> "הגנות עם רובה נגד דקירות סכין"
        "knife_defense_rifle_against_knife_stabs" -> "הגנות עם רובה נגד דקירות סכין"
        "gun_threat_defense" -> "הגנות מאיום אקדח"
        "multiple_attackers_defense" -> "הגנות נגד מספר תוקפים"
        "multiple_attackers_main" -> "הגנות נגד מספר תוקפים"
        "stick_defense" -> "הגנות נגד מקל"
        "kicks_hard" -> "הגנות נגד בעיטות"
        "kicks_straight_groin" -> "הגנות נגד בעיטות ישרות / למפשעה"
        "kicks_roundhouse_back_roundhouse" -> "הגנות נגד מגל / מגל לאחור"
        "kicks_knee" -> "הגנות נגד ברך"
        else -> raw
    }
}

private fun exerciseTitleForUi(
    raw: String,
    isEnglish: Boolean
): String {
    val original = raw.trim()
    if (original.isBlank()) return original

    val display = ExerciseTitleFormatter
        .displayName(original)
        .ifBlank { original }
        .trim()

    if (!isEnglish) return display

    val translated = ExerciseTitlesEn
        .getOrSame(display)
        .trim()

    if (translated.isNotBlank() && translated != display) {
        return translated
    }

    return when (display) {
        "קוואלר - הליכה לאחור" ->
            "Cavalier with a Step Backwards"

        "קוואלר נגד התנגדות - הליכה לפנים" ->
            "Cavalier Against Resistance - Walking Forward"

        "קוואלר - אגודלים" ->
            "Cavalier with Thumbs"

        "קוואלר – מרפק",
        "קוואלר - מרפק" ->
            "Cavalier - Elbow"

        "גלגול לאחור - ימין",
        "גלגול לאחור צד ימין" ->
            "Backward Roll - Right"

        "גלגול לאחור - שמאל",
        "גלגול לאחור שמאל",
        "גלגול לאחור צד שמאל" ->
            "Backward Roll - Left"

        "גלגול לפנים - ימין",
        "גלגול לפנים – צד ימין",
        "גלגול לפנים צד ימין" ->
            "Forward Roll - Right"

        "גלגול לפנים - שמאל",
        "גלגול לפנים צד שמאל" ->
            "Forward Roll - Left"

        "בלימה לאחור" ->
            "Backward Breakfall"

        "בלימה רכה לפנים" ->
            "Soft Forward Breakfall"

        "בלימה לצד ימין" ->
            "Breakfall to the Right"

        "בלימה לצד שמאל" ->
            "Breakfall to the Left"

        "בלימה לאחור מגובה" ->
            "Backward Breakfall from Height"

        "בלימה לצד כהכנה לגזיזות" ->
            "Side Breakfall as Preparation for Scissors"

        "גלגול לפנים ובלימה לאחור - ימין" ->
            "Forward Roll and Backward Breakfall - Right"

        "גלגול לפנים ובלימה לאחור - שמאל" ->
            "Forward Roll and Backward Breakfall - Left"

        "גלגול לפנים ולאחור - ימין" ->
            "Forward and Backward Roll - Right"

        "גלגול לפנים ולאחור - שמאל" ->
            "Forward and Backward Roll - Left"

        "גלגול ביד אחת - ימין" ->
            "One-Handed Roll - Right"

        "גלגול ביד אחת - שמאל" ->
            "One-Handed Roll - Left"

        "גלגול לפנים קימה לפנים" ->
            "Forward Roll and Stand Up Forward"

        "מניעת נפילה מחביקת שוקיים מלפנים להפלה" ->
            "Preventing a Double-Leg Take-Down from the Front"

        "גלגול לצד — ימין",
        "גלגול לצד - ימין" ->
            "Side Roll - Right"

        "גלגול לצד — שמאל",
        "גלגול לצד - שמאל" ->
            "Side Roll - Left"

        "גלגול ברחיפה — ימין",
        "גלגול ברחיפה - ימין" ->
            "Diving Roll - Right"

        "גלגול ברחיפה — שמאל",
        "גלגול ברחיפה - שמאל" ->
            "Diving Roll - Left"

        "גלגול לגובה — ימין",
        "גלגול לגובה - ימין" ->
            "High Roll - Right"

        "גלגול לגובה — שמאל",
        "גלגול לגובה - שמאל" ->
            "High Roll - Left"

        "גלגול ללא ידיים — ימין",
        "גלגול ללא ידיים - ימין" ->
            "No-Hands Roll - Right"

        "גלגול ללא ידיים — שמאל",
        "גלגול ללא ידיים - שמאל" ->
            "No-Hands Roll - Left"

        "בעיטת לצד בסיבוב מלא בניתור",
        "בעיטה לצד בסיבוב מלא בניתור" ->
            "Leaping Side-Kick with Spin"

        "עמידת מוצא רגילה" ->
            "Regular Stance"

        "עמידת מוצא להגנות פנימיות" ->
            "Internal Defence Stance"

        "עמידת מוצא להגנות חיצוניות" ->
            "External Defence Stance"

        "עמידת מוצא צידית" ->
            "Side Stance"

        "עמידת מוצא כללית מספר 1" ->
            "General Ready Stance No. 1"

        "עמידת מוצא כללית מספר 2" ->
            "General Ready Stance No. 2"

        "מוצא לעבודת קרקע" ->
            "Starting Position for Ground-work"

        "הוצאת אגן" ->
            "Hip Escape"

        "הרמת אגן והפניית גוף לכיוון ההפלה" ->
            "Hip Lift and Body Turn Toward the Take-Down"

        "הגנה נגד אגרופים בשכיבה" ->
            "Defense Against Punches on the Ground"

        "מכת גב יד בהצלפה" ->
            "Whipping Backfist Strike"

        "מכת גב יד בהצלפה בסיבוב" ->
            "Spinning Whipping Backfist Strike"

        "מכת פטיש" ->
            "Hammer Punch"

        "מכת פטיש מהצד" ->
            "Side Hammer Punch"

        "אגרוף עליון" ->
            "Uppercut Punch"

        "מכת פטיש בסיבוב" ->
            "Spinning Hammer Punch"

        "מכת פטיש לאחור" ->
            "Backward Hammer Punch"

        "מכת פטיש מלמטה למעלה" ->
            "Upward Hammer Punch"

        "פיסת יד פנימית" ->
            "Inside Knifehand Strike"

        "פיסת יד חיצונית" ->
            "Outside Knifehand Strike"

        "גב יד" ->
            "Backfist"

        "מגל" ->
            "Hook Punch"

        "סנוקרת" ->
            "Uppercut"

        "מכות במקל קצר - מכת מקל לראש" ->
            "Short Stick Strike - Strike to the Head"

        "מכות במקל קצר - מכת מקל לרקה" ->
            "Short Stick Strike - Strike to the Temple"

        "מכות במקל קצר - מכת מקל ללסת / צואר" ->
            "Short Stick Strike - Strike to the Jaw / Neck"

        "מכות במקל קצר - מכת מקל לעצם הבריח" ->
            "Short Stick Strike - Strike to the Collarbone"

        "מכות במקל קצר - מכת מקל למרפק" ->
            "Short Stick Strike - Strike to the Elbow"

        "מכות במקל קצר - מכת מקל לשורש כף היד" ->
            "Short Stick Strike - Strike to the Wrist"

        "מכות במקל קצר - מכת מקל לפרקי האצבעות" ->
            "Short Stick Strike - Strike to the Knuckles"

        "מכות במקל קצר - מכת מקל לברך" ->
            "Short Stick Strike - Strike to the Knee"

        "מכות במקל קצר - מכת מקל למפסעה" ->
            "Short Stick Strike - Strike to the Groin"

        "מכות במקל קצר - הצלפת מקל לצלעות" ->
            "Short Stick Strike - Whipping Strike to the Ribs"

        "מכות במקל קצר - דקירת מקל חיצונית לצלעות" ->
            "Short Stick Strike - External Stick Stab to the Ribs"

        "מכות במקל קצר - דקירת מקל ישרה לבטן / לגרון" ->
            "Short Stick Strike - Straight Stick Stab to the Abdomen / Throat"

        "מכות במקל קצר - דקירת מקל הפוכה" ->
            "Short Stick Strike - Reverse Stick Stab"

        "מכות במקל / רובה - התקפה עם מקל לנקודות תורפה" ->
            "Stick / Rifle Strike - Attack to Vital Points"

        "מכות במקל / רובה - מכה אופקית לצואר" ->
            "Stick / Rifle Strike - Horizontal Strike to the Neck"

        "מכות במקל / רובה - דקירה" ->
            "Stick / Rifle Strike - Stab"

        "מכות במקל / רובה - מכת מגל" ->
            "Stick / Rifle Strike - Circular Strike"

        "מכות במקל / רובה - שיסוף" ->
            "Stick / Rifle Strike - Slashing Strike"

        "מכות במקל / רובה - מכה למפסעה" ->
            "Stick / Rifle Strike - Strike to the Groin"

        "מכות במקל / רובה - מכת סנוקרת" ->
            "Stick / Rifle Strike - Uppercut Strike"

        "מכות במקל / רובה - מכה לצד" ->
            "Stick / Rifle Strike - Side Strike"

        "מכות במקל / רובה - מכה לאחור" ->
            "Stick / Rifle Strike - Backward Strike"

        "מכות במקל / רובה - מכה אופקית לאחור" ->
            "Stick / Rifle Strike - Horizontal Backward Strike"

        "מכות במקל / רובה - מכה אופקית ובעיטה רגילה למפסעה" ->
            "Stick / Rifle Strike - Horizontal Strike and Regular Kick to the Groin"

        "מכות במקל / רובה - מכה אופקית ובעיטת הגנה לפנים" ->
            "Stick / Rifle Strike - Horizontal Strike and Defensive Kick Forward"

        "מכות במקל / רובה - מכה לצד ובעיטה לצד" ->
            "Stick / Rifle Strike - Side Strike and Side Kick"

        "הגנה נגד מקל ארוך – התקפה לצד ימין מגן",
        "הגנה נגד מקל ארוך - התקפה לצד ימין מגן" ->
            "Defense Against Long Stick - Attack to Defender's Right Side"

        "הגנה נגד מקל ארוך – התקפה לצד שמאל מגן",
        "הגנה נגד מקל ארוך - התקפה לצד שמאל מגן" ->
            "Defense Against Long Stick - Attack to Defender's Left Side"

        "הגנה נגד מקל ארוך מצד ימין" ->
            "Defense Against Long Stick from the Right Side"

        "הגנה נגד מקל ארוך מצד שמאל" ->
            "Defense Against Long Stick from the Left Side"

        "הגנה נגד דקירה במקל ארוך – הצד החי",
        "הגנה נגד דקירה במקל ארוך - הצד החי" ->
            "Defense Against Long Stick Stab - Live Side"

        "הגנה נגד דקירה במקל ארוך – הצד המת",
        "הגנה נגד דקירה במקל ארוך - הצד המת" ->
            "Defense Against Long Stick Stab - Dead Side"

        else -> translated.ifBlank { display }
    }
}

private fun normalizeHardNavTopic(raw: String): String {
    val t = raw.trim()
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")

    return when {
        t == "שחרורים" -> "releases"

        t == "מתפיסות" ||
                t.contains("תפיסות יד") ||
                t.contains("שיער") ||
                t.contains("חולצה") ||
                t.contains("שחרור מתפיסות") ->
            "releases_hands_hair_shirt"

        t.contains("חניקות") ||
                t.contains("שחרור מחניקות") ->
            "releases_chokes"

        t.contains("מחביקות") ||
                t.contains("חביקות גוף") ||
                t.contains("חביקות צואר") ||
                t.contains("חביקות צואר") ||
                t.contains("חביקות זרוע") ->
            "releases_hugs"

        t == "הגנות עם רובה נגד דקירות סכין" -> "knife_rifle_defense"
        t == "הגנות נגד מספר תוקפים" -> "multiple_attackers_defense"
        t == "הגנות נגד 2 תוקפים" -> "multiple_attackers_defense"

        else -> t
    }
}

private fun loadDirectTopicItems(
    belt: Belt,
    topicTitle: String
): List<String> {
    val topicCandidates = topicLookupAliases(topicTitle)

    val direct = topicCandidates.flatMap { candidate ->
        runCatching {
            ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = candidate,
                subTopicTitle = null
            )
        }.getOrDefault(emptyList())
    }

    val directSameName = topicCandidates.flatMap { candidate ->
        runCatching {
            ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = candidate,
                subTopicTitle = candidate
            )
        }.getOrDefault(emptyList())
    }

    val repoSubTitles = topicCandidates.flatMap { candidate ->
        runCatching {
            ContentRepo.listSubTopicTitles(belt, candidate)
        }.getOrDefault(emptyList())
    }

    val registrySubTitles = topicCandidates.flatMap { candidate ->
        runCatching {
            AppSubTopicRegistry.getSubTopicsFor(belt, candidate)
        }.getOrDefault(emptyList())
    }

    val allSubTitles = (repoSubTitles + registrySubTitles)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    val viaSubs = topicCandidates.flatMap { candidate ->
        allSubTitles.flatMap { st ->
            runCatching {
                ContentRepo.listItemTitles(
                    belt = belt,
                    topicTitle = candidate,
                    subTopicTitle = st
                )
            }.getOrDefault(emptyList())
        }
    }

    val viaRegistrySameName = topicCandidates.flatMap { candidate ->
        runCatching {
            AppSubTopicRegistry.getItemsFor(
                belt = belt,
                topicTitle = candidate,
                subTopicTitle = candidate
            )
        }.getOrDefault(emptyList())
    }

    val viaRegistrySubs = topicCandidates.flatMap { candidate ->
        allSubTitles.flatMap { st ->
            runCatching {
                AppSubTopicRegistry.getItemsFor(
                    belt = belt,
                    topicTitle = candidate,
                    subTopicTitle = st
                )
            }.getOrDefault(emptyList())
        }
    }

    return (
            direct +
                    directSameName +
                    viaSubs +
                    viaRegistrySameName +
                    viaRegistrySubs
            )
        .asSequence()
        .map { ExerciseTitleFormatter.displayName(it).ifBlank { it }.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}

private data class UiNestedSubTopicLeaf(
    val title: String,
    val items: List<String>
)

private data class UiNestedSubTopicGroup(
    val title: String,
    val leaves: List<UiNestedSubTopicLeaf>
)

private fun normalizeUiNestedTitle(value: String): String =
    value
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace("־", "-")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun greenDefenseNestedGroups(): List<UiNestedSubTopicGroup> {
    return listOf(
        UiNestedSubTopicGroup(
            title = "הגנות נגד מכות",
            leaves = listOf(
                UiNestedSubTopicLeaf(
                    title = "הגנות חיצוניות נגד מכות",
                    items = listOf(
                        "הגנה חיצונית נגד ימין באגרוף מהופך",
                        "הגנה חיצונית נגד שמאל",
                        "הגנה חיצונית נגד שמאל בהתקדמות"
                    )
                ),
                UiNestedSubTopicLeaf(
                    title = "הגנות פנימיות נגד מכות",
                    items = listOf(
                        "הגנה פנימית נגד ימין באמת שמאל",
                        "הגנה פנימית נגד שמאל באמת שמאל",
                        "הגנה פנימית נגד ימין - אגרוף שמאל בהחלקה"
                    )
                )
            )
        ),

        UiNestedSubTopicGroup(
            title = "הגנות נגד בעיטות",
            leaves = listOf(
                UiNestedSubTopicLeaf(
                    title = "הגנות נגד בעיטות רגילות",
                    items = listOf(
                        "הגנה נגד בעיטה רגילה - בעיטה לצד",
                        "הגנה נגד בעיטה רגילה - טיימינג לצד החי",
                        "הגנה חיצונית באמת שמאל נגד בעיטה רגילה"
                    )
                ),
                UiNestedSubTopicLeaf(
                    title = "הגנות נגד בעיטות מגל לפנים",
                    items = listOf(
                        "הגנה נגד בעיטת מגל לפנים - בעיטה לצד",
                        "הגנה נגד בעיטת מגל נמוכה"
                    )
                ),
                UiNestedSubTopicLeaf(
                    title = "הגנות נגד בעיטות מגל לאחור",
                    items = listOf(
                        "הגנה נגד בעיטת מגל לאחור - בעיטה בימין",
                        "הגנה נגד בעיטת מגל לאחור - בעיטה בשמאל",
                        "הגנה נגד בעיטת מגל לאחור - אגרוף שמאל",
                        "הגנה נגד בעיטת מגל לאחור בסבוב - בעיטה"
                    )
                ),
                UiNestedSubTopicLeaf(
                    title = "הגנות נגד בעיטות לצד",
                    items = listOf(
                        "הגנה חיצונית באמת ימין נגד בעיטה לצד",
                        "הגנה חיצונית באמת שמאל נגד בעיטה לצד",
                        "הגנה נגד בעיטה לצד - בעיטת סטירה חיצונית"
                    )
                )
            )
        ),
        UiNestedSubTopicGroup(
            title = "הגנות - סכין",
            leaves = listOf(
                UiNestedSubTopicLeaf(
                    title = "הגנות נגד מקל",
                    items = listOf(
                        "הגנה נגד מקל - צד חי",
                        "הגנה נגד מקל - צד מת"
                    )
                ),
                UiNestedSubTopicLeaf(
                    title = "הגנות מאיום סכין",
                    items = listOf(
                        "הגנה מאיום סכין לעורק שמאל",
                        "הגנה מאיום סכין לעורק ימין",
                        "הגנה מאיום סכין להב לגורגרת",
                        "הגנה מאיום סכין מלפנים - חוד הסכין לגורגרת",
                        "הגנה מאיום סכין מאחור - להב הסכין לגורגרת",
                        "הגנה מאיום סכין - חוד לבטן התחתונה",
                        "הגנה מאיום סכין מאחור - חוד לגב"
                    )
                ),
                UiNestedSubTopicLeaf(
                    title = "הגנות נגד סכין",
                    items = listOf(
                        "הגנה נגד דקירה רגילה - בעיטה",
                        "הגנה נגד דקירה מזרחית - בעיטה",
                        "הגנה נגד דקירה ישרה מלפנים - בעיטה",
                        "הגנה נגד דקירה ישרה נמוכה - בעיטה",
                        "הגנה נגד דקירה ישרה מלפנים - הגנת גוף ובעיטת מגל למפסעה",
                        "הגנה נגד דקירה רגילה מהצד - בעיטה",
                        "הגנה נגד דקירה ישרה - בעיטה",
                        "הגנה נגד דקירה מזרחית מהצד - בעיטה"
                    )
                ),
                UiNestedSubTopicLeaf(
                    title = "הגנות נגד סכין קרובה מהצד",
                    items = listOf(
                        "הגנה נגד דקירה רגילה מהצד - התוקף בצד ימין",
                        "הגנה נגד דקירה רגילה מהצד - התוקף בצד שמאל",
                        "הגנה נגד דקירה מזרחית מהצד לעורף - התוקף בצד שמאל",
                        "הגנה נגד דקירה מזרחית מהצד לגב - התוקף בצד שמאל",
                        "הגנה נגד דקירה מזרחית מהצד לגרון - התוקף בצד ימין",
                        "הגנה נגד דקירה מזרחית מהצד לבטן - התוקף בצד ימין"
                    )
                )
            )
        )
    )
}

private fun nestedGreenDefenseGroupFor(
    belt: Belt,
    topicTitle: String
): UiNestedSubTopicGroup? {
    if (belt != Belt.GREEN) return null

    val wanted = normalizeUiNestedTitle(topicTitle)

    return greenDefenseNestedGroups()
        .firstOrNull { group ->
            normalizeUiNestedTitle(group.title) == wanted
        }
}

private fun nestedGreenDefenseLeafFor(
    belt: Belt,
    topicTitle: String
): UiNestedSubTopicLeaf? {
    if (belt != Belt.GREEN) return null

    val wanted = normalizeUiNestedTitle(topicTitle)

    return greenDefenseNestedGroups()
        .asSequence()
        .flatMap { group -> group.leaves.asSequence() }
        .firstOrNull { leaf ->
            normalizeUiNestedTitle(leaf.title) == wanted
        }
}

private fun isGreenDefenseNestedNavigationTopic(
    belt: Belt,
    topicTitle: String
): Boolean {
    if (belt != Belt.GREEN) return false

    val wanted = normalizeUiNestedTitle(topicTitle)

    if (wanted == "הגנות") return true

    val groupTitles = greenDefenseNestedGroups()
        .map { group -> normalizeUiNestedTitle(group.title) }

    val leafTitles = greenDefenseNestedGroups()
        .flatMap { group -> group.leaves }
        .map { leaf -> normalizeUiNestedTitle(leaf.title) }

    return wanted in groupTitles || wanted in leafTitles
}

private fun visibleGreenDefenseSubTopics(
    belt: Belt,
    topicTitle: String,
    rawSubs: List<String>
): List<String> {
    if (belt != Belt.GREEN || normalizeUiNestedTitle(topicTitle) != "הגנות") {
        return rawSubs
    }

    val nestedGroups = greenDefenseNestedGroups()

    val nestedGroupTitles = nestedGroups
        .map { normalizeUiNestedTitle(it.title) }
        .toSet()

    val nestedLeafTitles = nestedGroups
        .flatMap { it.leaves }
        .map { normalizeUiNestedTitle(it.title) }
        .toSet()

    val otherSubs = rawSubs
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filter { normalizeUiNestedTitle(it) !in nestedLeafTitles }
        .filter { normalizeUiNestedTitle(it) !in nestedGroupTitles }
        .distinctBy { normalizeUiNestedTitle(it) }

    return (
            nestedGroups.map { it.title } + otherSubs
            ).distinctBy { normalizeUiNestedTitle(it) }
}

private val catalogScreenGradientTop = Color(0xFFF3F1FB)
private val catalogScreenGradientMid = Color(0xFFF8F9FD)
private val catalogScreenGradientBottom = Color(0xFFFDFDFE)

/**
 * מסך שמציג את כל תתי־הנושאים של נושא מסוים בחגורה מסוימת.
 * כל כפתור = תת־נושא. למטה כתוב כמה תרגילים יש בו.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubTopicsScreen(
    belt: Belt,
    topic: String,
    onBack: () -> Unit,                 // נשאר בפרמטרים אך לא מוצג בכותרת
    onHome: () -> Unit,
    onOpenSubTopic: (String) -> Unit,
    onOpenExercise: (String) -> Unit,
    vm: KmiViewModel
) {

    val context = LocalContext.current
    val langManager = remember(context) { AppLanguageManager(context) }
    val currentLang = langManager.getCurrentLanguage()
    val isEnglish = currentLang == AppLanguage.ENGLISH

// ✅ לפי מצב ה-Theme של האפליקציה בפועל, לא לפי מצב המכשיר בלבד
    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f

// ✅ מפענחים את שם הנושא (למקרה שעבר דרך ה-URL Encoded)
    val topicDecoded = remember(topic) { Uri.decode(topic).trim() }

    // ✅ FIX: alias-ים של שחרורים ("מתפיסות", "חניקות", "מחביקות"...)
    // צריכים להפוך ל-id קשיח כדי שהמסך לא ייפול ל-flow הרגיל והריק.
    val hardNavTopic = remember(topicDecoded) {
        normalizeHardNavTopic(topicDecoded)
    }

    // ✅ נושאי הגנות בחגורה ירוקה מטופלים מקומית כרמה 1 -> רמה 2 -> תרגילים.
    // לא נותנים ל-HardSectionsCatalog לבלוע אותם ולהכניס ישר לתרגילים.
    val isGreenDefenseNestedNavigation = remember(belt, topicDecoded) {
        isGreenDefenseNestedNavigationTopic(
            belt = belt,
            topicTitle = topicDecoded
        )
    }

    val hardRootSections = remember(hardNavTopic, isGreenDefenseNestedNavigation) {
        if (isGreenDefenseNestedNavigation) {
            null
        } else {
            HardSectionsCatalog.sectionsForSubject(hardNavTopic)
        }
    }

    val hardCurrentSection = remember(hardNavTopic, hardRootSections, isGreenDefenseNestedNavigation) {
        if (isGreenDefenseNestedNavigation) {
            null
        } else if (hardRootSections == null) {
            HardSectionsCatalog.findAnySectionById(hardNavTopic)
        } else {
            null
        }
    }

    val isHardFlow = remember(
        hardNavTopic,
        hardRootSections,
        hardCurrentSection,
        isGreenDefenseNestedNavigation
    ) {
        !isGreenDefenseNestedNavigation &&
                (
                        HardSectionsCatalog.supportsSubject(hardNavTopic) ||
                                hardRootSections != null ||
                                hardCurrentSection != null
                        )
    }

    val hardTitle = remember(hardNavTopic, hardRootSections, hardCurrentSection) {
        when {
            hardCurrentSection != null -> hardCurrentSection.title
            hardRootSections != null ->
                HardSectionsCatalog.subjectDisplayTitle(hardNavTopic)
                    ?: hardDisplayTitleFallback(hardNavTopic)
            else -> hardDisplayTitleFallback(hardNavTopic)
        }
    }

    val hardSubSections = remember(hardRootSections, hardCurrentSection) {
        when {
            hardCurrentSection != null && hardCurrentSection.subSections.isNotEmpty() -> {
                hardCurrentSection.subSections
            }

            hardRootSections != null && hardRootSections.size > 1 -> {
                hardRootSections
            }

            else -> emptyList()
        }
    }

    val hardLeafSection = remember(hardRootSections, hardCurrentSection) {
        when {
            hardCurrentSection != null && hardCurrentSection.subSections.isEmpty() -> {
                hardCurrentSection
            }

            hardRootSections != null &&
                    hardRootSections.size == 1 &&
                    hardRootSections.first().subSections.isEmpty() -> {
                hardRootSections.first()
            }

            else -> null
        }
    }

    val hardItems: List<Pair<String, String>> = remember(hardLeafSection, belt, isEnglish) {
        hardLeafSection
            ?.itemsFor(belt)
            .orEmpty()
            .asSequence()
            .map { original ->
                val cleanOriginal = original.trim()
                cleanOriginal to exerciseTitleForUi(
                    raw = cleanOriginal,
                    isEnglish = isEnglish
                )
            }
            .filter { (original, display) ->
                original.isNotBlank() && display.isNotBlank()
            }
            .distinctBy { it.first }
            .toList()
    }

    val hardBeltGroups = remember(hardLeafSection) {
        hardLeafSection?.beltGroups
            ?.filter { it.items.isNotEmpty() }
            .orEmpty()
    }

    // שולפים את תתי־הנושאים הרגילים רק אם זה לא hard flow
    val subs: List<String> = remember(belt, topicDecoded, isHardFlow) {
        if (isHardFlow) return@remember emptyList()

        val forcedDefenseSubs = if (belt == Belt.BLACK && topicDecoded.trim() == "הגנות") {
            listOf(
                "הגנות עם רובה נגד דקירות סכין",
                "הגנות נגד מספר תוקפים"
            )
        } else {
            emptyList()
        }

        val fromShared = runCatching {
            AppSubTopicRegistry.getSubTopicsFor(belt, topicDecoded)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }.getOrElse { emptyList() }

        if (fromShared.isNotEmpty() || forcedDefenseSubs.isNotEmpty()) {
            val merged = (fromShared + forcedDefenseSubs)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

            return@remember merged
        }

        val fromRepo = runCatching {
            ContentRepo.listSubTopicTitles(belt, topicDecoded)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }.getOrElse { emptyList() }

        val merged = (fromRepo + forcedDefenseSubs)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        merged
    }

    val realSubs: List<String> = remember(subs, topicDecoded) {
        val t = topicDecoded.trim()
        subs.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it != t }
            .distinct()
            .toList()
    }

    val hasRealSubs = remember(subs, topicDecoded) {
        when {
            subs.isEmpty() -> false
            subs.size > 1 -> true
            else -> subs.first().trim() != topicDecoded.trim()
        }
    }

    val directItems: List<String> = remember(belt, topicDecoded, isHardFlow) {
        if (isHardFlow) {
            emptyList()
        } else {
            loadDirectTopicItems(
                belt = belt,
                topicTitle = topicDecoded
            )
        }
    }

    val localNestedGroup = remember(belt, topicDecoded, isHardFlow) {
        if (isHardFlow) {
            null
        } else {
            nestedGreenDefenseGroupFor(
                belt = belt,
                topicTitle = topicDecoded
            )
        }
    }

    val localNestedLeaf = remember(belt, topicDecoded, isHardFlow) {
        if (isHardFlow) {
            null
        } else {
            nestedGreenDefenseLeafFor(
                belt = belt,
                topicTitle = topicDecoded
            )
        }
    }

    val visibleRealSubs = remember(belt, topicDecoded, realSubs) {
        visibleGreenDefenseSubTopics(
            belt = belt,
            topicTitle = topicDecoded,
            rawSubs = realSubs
        )
    }

    val buttonSpacing = 12.dp

// ===== QUICK MENU STATE =====
    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // ✅ פתיחת רמה שנייה בתוך אותו מסך:
    // הגנות -> הגנות נגד מכות / הגנות נגד בעיטות / הגנות - סכין
    var openedLocalNestedGroupTitle by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // ✅ פתיחת רמה שלישית בתוך אותו מסך:
    // הגנות נגד מכות -> הגנות חיצוניות / הגנות פנימיות -> תרגילים
    var openedLocalNestedLeafTitle by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val activeNestedGroup = remember(
        belt,
        topicDecoded,
        localNestedGroup,
        openedLocalNestedGroupTitle,
        isHardFlow
    ) {
        if (isHardFlow) {
            null
        } else {
            openedLocalNestedGroupTitle
                ?.let { selectedTitle ->
                    nestedGreenDefenseGroupFor(
                        belt = belt,
                        topicTitle = selectedTitle
                    )
                }
                ?: localNestedGroup
        }
    }

    val activeNestedLeaf = remember(
        belt,
        topicDecoded,
        localNestedLeaf,
        openedLocalNestedLeafTitle,
        isHardFlow
    ) {
        if (isHardFlow) {
            null
        } else {
            openedLocalNestedLeafTitle
                ?.let { selectedTitle ->
                    nestedGreenDefenseLeafFor(
                        belt = belt,
                        topicTitle = selectedTitle
                    )
                }
                ?: localNestedLeaf
        }
    }

    BackHandler(enabled = openedLocalNestedLeafTitle != null) {
        openedLocalNestedLeafTitle = null
    }

    BackHandler(enabled = openedLocalNestedLeafTitle == null && openedLocalNestedGroupTitle != null) {
        openedLocalNestedGroupTitle = null
    }

// ⛔ כרגע חוסמים גישה עד שהמנויים עובדים
    val hasAccess = false

    data class OpenedExerciseRequest(
        val belt: Belt,
        val item: String
    )

    // כמו במסך הבית: מפתח תרגיל שנבחר מהחיפוש → פותח דיאלוג הסבר יחיד
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    // ✅ שומר גם את החגורה של התרגיל, כדי שההסבר ייקרא מ-Explanations נכון
    var openedExerciseRequest by remember {
        mutableStateOf<OpenedExerciseRequest?>(null)
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = if (isEnglish) ExerciseTitlesEn.getOrSame(hardTitle) else hardTitle,
                onHome = onHome,
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                lockSearch = false,
                onSearch = null,
                onPickSearchResult = { key -> pickedKey = key },
                centerTitle = true
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            catalogScreenGradientTop,
                            catalogScreenGradientMid,
                            catalogScreenGradientBottom
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (isHardFlow && hardSubSections.isNotEmpty()) {
                    hardSubSections.forEach { section ->
                        val beltCount = section.itemsFor(belt).size
                        val displayCount = if (beltCount > 0) beltCount else section.totalItemsCount()

                        HardSubTopicCategoryCard(
                            belt = belt,
                            title = if (isEnglish) ExerciseTitlesEn.getOrSame(section.title) else section.title,
                            count = displayCount,
                            onClick = { onOpenSubTopic(section.id) }
                        )
                    }

                } else if (isHardFlow && hardBeltGroups.isNotEmpty()) {

                    hardBeltGroups.forEach { group ->
                        HardBeltGroupCard(
                            belt = group.belt,
                            items = group.items,
                            topicKey = hardTitle.ifBlank { topicDecoded },
                            isDarkMode = isDarkMode,
                            vm = vm,
                            onOpenExercise = { item ->
                                openedExerciseRequest = OpenedExerciseRequest(
                                    belt = group.belt,
                                    item = item
                                )
                            }
                        )

                        Spacer(Modifier.height(12.dp))
                    }

                } else if (isHardFlow && hardItems.isNotEmpty()) {

                    var explain by rememberSaveable { mutableStateOf<String?>(null) }

                    hardItems.forEach { (originalName, displayName) ->
                        ExerciseRowWithInfo(
                            belt = belt,
                            itemName = originalName,
                            displayName = displayName,
                            accent = MaterialTheme.colorScheme.primary,
                            onExplain = { _, item -> explain = item },
                            onOpenExercise = { item ->
                                openedExerciseRequest = OpenedExerciseRequest(
                                    belt = belt,
                                    item = item
                                )
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    explain?.let { item ->
                        val titleForDialog = exerciseTitleForUi(
                            raw = item,
                            isEnglish = isEnglish
                        )

                        val explanation = remember(belt, hardTitle, item, isEnglish) {
                            findExplanationForHitLocal(
                                belt = belt,
                                rawItem = item,
                                topic = hardTitle,
                                isEnglish = isEnglish
                            )
                        }

                        ModernExerciseInfoDialog(
                            title = titleForDialog,
                            subtitle = if (isEnglish) belt.en else belt.heb,
                            explanation = explanation,
                            accentColor = belt.color,
                            onDismiss = { explain = null }
                        )
                    }

                } else if (isHardFlow) {

                    Text(
                        text = "לא נמצאו תרגילים קשיחים עבור \"$hardTitle\"",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                } else if (activeNestedLeaf != null) {

                    var explain by rememberSaveable { mutableStateOf<String?>(null) }

                    activeNestedLeaf.items.forEach { itemName ->
                        ExerciseRowWithInfo(
                            belt = belt,
                            itemName = itemName,
                            accent = MaterialTheme.colorScheme.primary,
                            onExplain = { _, item -> explain = item },
                            onOpenExercise = { item ->
                                openedExerciseRequest = OpenedExerciseRequest(
                                    belt = belt,
                                    item = item
                                )
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    explain?.let { item ->
                        val explanation = remember(belt, topicDecoded, item) {
                            findExplanationForHitLocal(
                                belt = belt,
                                rawItem = item,
                                topic = topicDecoded,
                                isEnglish = isEnglish
                            )
                        }

                        ModernExerciseInfoDialog(
                            title = exerciseTitleForUi(
                                raw = item,
                                isEnglish = isEnglish
                            ),
                            subtitle = if (isEnglish) belt.en else belt.heb,
                            explanation = explanation,
                            accentColor = belt.color,
                            onDismiss = { explain = null }
                        )
                    }

                } else if (activeNestedGroup != null) {

                    activeNestedGroup.leaves.forEach { leaf ->
                        HardSubTopicCategoryCard(
                            belt = belt,
                            title = if (isEnglish) ExerciseTitlesEn.getOrSame(leaf.title) else leaf.title,
                            count = leaf.items.size,
                            onClick = {
                                openedLocalNestedLeafTitle = leaf.title
                            }
                        )
                    }

                } else if (!hasRealSubs) {

                    val items: List<String> = remember(belt, topicDecoded) {
                        loadDirectTopicItems(
                            belt = belt,
                            topicTitle = topicDecoded
                        )
                    }

                    var explain by rememberSaveable { mutableStateOf<String?>(null) }

                    if (items.isEmpty()) {
                        Text(
                            text = "לא נמצאו תתי־נושאים או תרגילים עבור \"$topicDecoded\"",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        items.forEach { itemName ->
                            ExerciseRowWithInfo(
                                belt = belt,
                                itemName = itemName,
                                accent = MaterialTheme.colorScheme.primary,
                                onExplain = { _, item -> explain = item },
                                onOpenExercise = { item ->
                                    openedExerciseRequest = OpenedExerciseRequest(
                                        belt = belt,
                                        item = item
                                    )
                                }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    explain?.let { item ->
                        val explanation = remember(belt, topicDecoded, item) {
                            findExplanationForHitLocal(
                                belt = belt,
                                rawItem = item,
                                topic = topicDecoded,
                                isEnglish = isEnglish
                            )
                        }

                        ModernExerciseInfoDialog(
                            title = item,
                            subtitle = if (isEnglish) belt.en else belt.heb,
                            explanation = explanation,
                            accentColor = belt.color,
                            onDismiss = { explain = null }
                        )
                    }

                } else {

                    visibleRealSubs.forEach { subTitleRaw ->
                        val subTitle = subTitleRaw.trim()

                        val itemCount by remember(belt, topicDecoded, subTitle) {
                            mutableStateOf(
                                when {
                                    nestedGreenDefenseGroupFor(
                                        belt = belt,
                                        topicTitle = subTitle
                                    ) != null -> {
                                        nestedGreenDefenseGroupFor(
                                            belt = belt,
                                            topicTitle = subTitle
                                        )
                                            ?.leaves
                                            .orEmpty()
                                            .sumOf { leaf -> leaf.items.size }
                                    }

                                    belt == Belt.BLACK &&
                                            topicDecoded.trim() == "הגנות" &&
                                            subTitle == "הגנות עם רובה נגד דקירות סכין" -> {
                                        HardSectionsCatalog.subjectSubSectionItemsFor(
                                            subjectId = "knife_defense",
                                            subSectionId = "knife_defense_rifle_against_knife_stabs",
                                            belt = belt
                                        )
                                            .map { ExerciseTitleFormatter.displayName(it).ifBlank { it }.trim() }
                                            .filter { it.isNotBlank() }
                                            .distinct()
                                            .size
                                    }

                                    belt == Belt.BLACK &&
                                            topicDecoded.trim() == "הגנות" &&
                                            subTitle == "הגנות נגד מספר תוקפים" -> {
                                        HardSectionsCatalog.subjectItemsFor(
                                            subjectId = "multiple_attackers_defense",
                                            belt = belt
                                        )
                                            .map { ExerciseTitleFormatter.displayName(it).ifBlank { it }.trim() }
                                            .filter { it.isNotBlank() }
                                            .distinct()
                                            .size
                                    }

                                    else -> {
                                        ExerciseCountProvider.subTopicStats(
                                            belt = belt,
                                            topicTitle = topicDecoded,
                                            subTopicTitle = subTitle
                                        ).exerciseCount
                                    }
                                }
                            )
                        }

                        HardSubTopicCategoryCard(
                            belt = belt,
                            title = if (isEnglish) ExerciseTitlesEn.getOrSame(subTitle) else subTitle,
                            count = itemCount,
                            onClick = {
                                val nestedGroup = nestedGreenDefenseGroupFor(
                                    belt = belt,
                                    topicTitle = subTitle
                                )

                                if (nestedGroup != null) {
                                    openedLocalNestedLeafTitle = null
                                    openedLocalNestedGroupTitle = subTitle
                                } else {
                                    onOpenSubTopic(subTitle)
                                }
                            }
                        )
                    }
                }
            }

            // ===== QUICK MENU (FLOATING אמיתי) =====
            il.kmi.app.ui.FloatingQuickMenu(
                belt = belt,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 84.dp),
                expanded = quickMenuExpanded,
                onExpandedChange = { quickMenuExpanded = it },
                triggerMode = il.kmi.app.ui.QuickMenuTriggerMode.BottomBar,
                includePractice = true,
                hasFullAccess = hasAccess,
                onLockedItemClick = {
                },
                onWeakPoints = { },
                onAllLists = { },
                onPractice = { },
                onSummary = { },
                onVoice = { },
                onPdf = { }
            )
        }
    }

// ===== דיאלוג הסבר לתרגיל שנלחץ מתוך הרשימה עצמה =====
    openedExerciseRequest?.let { req ->
        val explanation = remember(req.belt, hardTitle, topicDecoded, req.item) {
            findExplanationForHitLocal(
                belt = req.belt,
                rawItem = req.item,
                topic = hardTitle.ifBlank { topicDecoded },
                isEnglish = isEnglish
            )
        }

        ModernExerciseInfoDialog(
            title = exerciseTitleForUi(
                raw = req.item,
                isEnglish = isEnglish
            ),
            subtitle = if (isEnglish) req.belt.en else req.belt.heb,
            explanation = explanation,
            accentColor = req.belt.color,
            onDismiss = { openedExerciseRequest = null }
        )
    }

    pickedKey?.let { key ->
        val (beltHit, topicHit, itemHit) = parseSearchKeyLocal(key)
        val displayName = ExerciseTitleFormatter.displayName(itemHit).ifBlank { itemHit }.trim()

        val explanation = remember(beltHit, itemHit) {
            findExplanationForHitLocal(
                belt = beltHit,
                rawItem = itemHit,
                topic = topicHit,
                isEnglish = isEnglish
            )
        }

        var isFav by remember { mutableStateOf(false) }

        ModernExerciseInfoDialog(
            title = displayName,
            subtitle = if (isEnglish) beltHit.en else beltHit.heb,
            explanation = explanation,
            accentColor = beltHit.color,
            isFav = isFav,
            onToggleFav = { isFav = !isFav },
            onDismiss = { pickedKey = null }
        )
    }
}


/* ========= עזר: לפרק מפתח חיפוש "belt|topic|item" ========= */
private fun parseSearchKeyLocal(key: String): Triple<il.kmi.shared.domain.Belt, String, String> {
    val parts = when {
        "|" in key  -> key.split("|", limit = 3)
        "::" in key -> key.split("::", limit = 3)
        "/" in key  -> key.split("/", limit = 3)
        else        -> listOf("", "", "")
    }.let { (it + listOf("", "", "")).take(3) }

    val belt  = il.kmi.shared.domain.Belt.fromId(parts[0]) ?: il.kmi.shared.domain.Belt.WHITE
    val topic = parts[1]
    val item  = parts[2]
    return Triple(belt, topic, item)
}

/* ========= עזר: למצוא הסבר אמיתי מתוך Explanations ========= */
private fun findExplanationForHitLocal(
    belt: il.kmi.shared.domain.Belt,
    rawItem: String,
    topic: String,
    isEnglish: Boolean
): String {
    val resolved = ExerciseExplanationResolver.get(
        belt = belt,
        topic = topic,
        item = rawItem,
        isEnglish = isEnglish
    ).trim()

    val isFallback = if (isEnglish) {
        resolved.startsWith("Detailed explanation for:") ||
                resolved.startsWith("There is currently no explanation")
    } else {
        resolved.startsWith("הסבר מפורט על") ||
                resolved.startsWith("אין כרגע")
    }

    if (resolved.isNotBlank() && !isFallback) {
        return resolved.split("::")
            .map { it.trim() }
            .lastOrNull { it.isNotBlank() }
            ?: resolved
    }

    return if (isEnglish) {
        "There is currently no explanation for this exercise."
    } else {
        "אין כרגע הסבר לתרגיל זה."
    }
}

private fun saveSubTopicExerciseNote(
    prefs: android.content.SharedPreferences,
    noteKey: String,
    text: String
) {
    val clean = text.trim()

    prefs.edit().apply {
        if (clean.isBlank()) {
            remove(noteKey)
        } else {
            putString(noteKey, clean)
        }
    }.apply()
}

@Composable
private fun ModernExerciseInfoDialog(
    title: String,
    subtitle: String? = null,
    explanation: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isFav: Boolean? = null,
    onToggleFav: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val langManager = remember(context) { AppLanguageManager(context) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val notePrefs = remember(context) {
        context.getSharedPreferences("kmi_exercise_notes", android.content.Context.MODE_PRIVATE)
    }

    val favoriteId = remember(title) {
        title.trim()
    }

    val noteKey = remember(title, subtitle) {
        "note_${subtitle.orEmpty().trim()}_${favoriteId}"
    }

    var notesRefreshKey by rememberSaveable(noteKey) {
        mutableIntStateOf(0)
    }

    var noteText by remember(noteKey, notesRefreshKey) {
        mutableStateOf(notePrefs.getString(noteKey, "").orEmpty())
    }

    var showNoteEditor by rememberSaveable(noteKey) {
        mutableStateOf(false)
    }

    var localFavorite by remember(favoriteId) {
        mutableStateOf(il.kmi.app.favorites.FavoritesStore.isFavorite(favoriteId))
    }

    val effectiveFavorite = isFav ?: localFavorite

    ExerciseExplanationDialog(
        title = title,
        beltLabel = subtitle
            ?.takeIf { it.isNotBlank() }
            ?.let { "($it)" }
            ?: "",
        explanation = explanation,
        noteText = noteText,
        isFavorite = effectiveFavorite,
        accentColor = accentColor,
        isEnglish = isEnglish,
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White,
                lerp(Color.White, accentColor, 0.12f),
                lerp(Color.White, accentColor, 0.06f),
                Color.White
            )
        ),
        onDismiss = {
            showNoteEditor = false
            onDismiss()
        },
        onEditNote = {
            showNoteEditor = true
        },
        onDeleteNote = {
            noteText = ""

            saveSubTopicExerciseNote(
                prefs = notePrefs,
                noteKey = noteKey,
                text = ""
            )

            notesRefreshKey++
        },
        onToggleFavorite = {
            if (onToggleFav != null) {
                onToggleFav()
            } else {
                il.kmi.app.favorites.FavoritesStore.toggle(favoriteId)
                localFavorite = !localFavorite
            }
        }
    )

    if (showNoteEditor) {
        ExerciseNoteEditorDialog(
            noteText = noteText,
            isEnglish = isEnglish,
            accentColor = accentColor,
            onNoteChange = { noteText = it },
            onDismiss = {
                showNoteEditor = false
            },
            onSave = {
                val cleanNote = noteText.trim()
                noteText = cleanNote

                saveSubTopicExerciseNote(
                    prefs = notePrefs,
                    noteKey = noteKey,
                    text = cleanNote
                )

                notesRefreshKey++
                showNoteEditor = false
            }
        )
    }
}

@Composable
private fun SubTopicCategoryCard(
    belt: Belt,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    HardSubTopicCategoryCard(
        belt = belt,
        title = title,
        count = count,
        onClick = onClick
    )
}

@Composable
private fun HardSubTopicCategoryCard(
    belt: Belt,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val langManager = remember(context) { AppLanguageManager(context) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val textAlignByLang = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalByLang = if (isEnglish) Alignment.Start else Alignment.End
    val layoutByLang =
        if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
        else androidx.compose.ui.unit.LayoutDirection.Rtl

    val iconTint = belt.color
    val borderColor = belt.color.copy(alpha = 0.42f)
    val chevronColor = belt.color
    val subtitleColor = belt.color.copy(alpha = 0.95f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = belt.lightColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides layoutByLang
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = horizontalByLang
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = textAlignByLang,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = if (isEnglish) {
                                if (count == 1) "1 exercise" else "$count exercises"
                            } else {
                                "$count תרגילים"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = subtitleColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = textAlignByLang,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "‹",
                    style = MaterialTheme.typography.headlineSmall,
                    color = chevronColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HardGroupStatChip(
    value: String,
    label: String,
    containerColor: Color,
    contentColor: Color = Color.White
) {
    Surface(
        modifier = Modifier.widthIn(min = 64.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            contentColor.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = contentColor,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Text(
                text = label,
                color = contentColor.copy(alpha = 0.92f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HardLegacyMetaBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            contentColor.copy(alpha = 0.14f)
        ),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 9.sp,
            lineHeight = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun HardBeltGroupCard(
    belt: Belt,
    items: List<String>,
    topicKey: String,
    isDarkMode: Boolean = false,
    vm: KmiViewModel,
    onOpenExercise: (String) -> Unit
) {
    val context = LocalContext.current
    val langManager = remember(context) { AppLanguageManager(context) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val prefs = remember(context) {
        context.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE)
    }

    val actionKeyPart = remember(belt.id, topicKey) {
        topicKey
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    val excludedKey = remember(belt.id, actionKeyPart) {
        "excluded_${belt.id}_$actionKeyPart"
    }

    val favKey = remember(belt.id, actionKeyPart) {
        "fav_${belt.id}_$actionKeyPart"
    }

    var excludedItems by remember(excludedKey) {
        mutableStateOf<Set<String>>(
            prefs.getStringSet(excludedKey, emptySet<String>())
                ?.toSet()
                ?: emptySet()
        )
    }

    var favorites by remember(favKey) {
        mutableStateOf<Set<String>>(
            prefs.getStringSet(favKey, emptySet<String>())
                ?.toSet()
                ?: emptySet()
        )
    }

    var noteEditorFor by rememberSaveable { mutableStateOf<String?>(null) }
    var noteDraft by rememberSaveable { mutableStateOf("") }
    var notesRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    fun noteKeyFor(itemId: String): String {
        return "note_${belt.id}_${actionKeyPart}_$itemId"
    }

    fun loadNoteFor(itemId: String): String {
        return prefs.getString(noteKeyFor(itemId), "").orEmpty()
    }

    fun saveNoteFor(itemId: String, value: String) {
        val clean = value.trim()

        prefs.edit().apply {
            if (clean.isBlank()) {
                remove(noteKeyFor(itemId))
            } else {
                putString(noteKeyFor(itemId), clean)
            }
        }.apply()

        notesRefreshKey++
    }

    fun toggleFavorite(itemId: String) {
        val next = favorites.toMutableSet()

        if (next.contains(itemId)) {
            next.remove(itemId)
        } else {
            next.add(itemId)
        }

        favorites = next.toSet()
        prefs.edit().putStringSet(favKey, next).apply()
    }

    fun toggleExclude(itemId: String) {
        val next = excludedItems.toMutableSet()

        if (next.contains(itemId)) {
            next.remove(itemId)
        } else {
            next.add(itemId)
        }

        excludedItems = next.toSet()
        prefs.edit().putStringSet(excludedKey, next).apply()
    }

    val marksVersion by vm.marksVersion.collectAsState()
    val itemStates = remember(belt.id, topicKey, items) {
        mutableStateMapOf<String, Boolean?>()
    }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun statusIdFor(rawItem: String): String {
        val cleanItem = normalizeStatusPart(rawItem)
        val cleanTopic = normalizeStatusPart(topicKey)

        val resolved = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = cleanItem,
            topicKey = cleanTopic
        )

        val itemStablePart = cleanItem
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), "_")
            .trim('_')
            .ifBlank { "item" }

        return "${resolved.id}__$itemStablePart"
    }

    fun statusKeysFor(rawItem: String): List<String> {
        val statusId = statusIdFor(rawItem)
        val baseStatusId = statusId.substringBefore("__")

        val identityKeys = ExerciseIdentityRegistry
            .allKnown()
            .firstOrNull { it.id == baseStatusId && it.belt == belt }
            ?.topicKeys
            .orEmpty()

        return (
                identityKeys +
                        topicKey +
                        "כללי"
                )
            .map { normalizeStatusPart(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun setLocalStatus(
        rawItem: String,
        statusId: String,
        value: Boolean?
    ) {
        statusKeysFor(rawItem).forEach { key ->
            val masteredKey = "mastered_${belt.id}_${key}"
            val unknownKey = "unknown_${belt.id}_${key}"

            val masteredSet =
                (prefs.getStringSet(masteredKey, emptySet<String>()) ?: emptySet()).toMutableSet()

            val unknownSet =
                (prefs.getStringSet(unknownKey, emptySet<String>()) ?: emptySet()).toMutableSet()

            when (value) {
                true -> {
                    masteredSet.add(statusId)
                    unknownSet.remove(statusId)
                }

                false -> {
                    unknownSet.add(statusId)
                    masteredSet.remove(statusId)
                }

                null -> {
                    masteredSet.remove(statusId)
                    unknownSet.remove(statusId)
                }
            }

            prefs.edit()
                .putStringSet(masteredKey, masteredSet)
                .putStringSet(unknownKey, unknownSet)
                .apply()
        }
    }

    LaunchedEffect(items, marksVersion, topicKey) {
        items.forEach { rawItem ->
            val originalName = rawItem.trim()
            val statusId = statusIdFor(originalName)

            var valueFromVm: Boolean? = null

            for (key in statusKeysFor(originalName)) {
                val fromKey: Boolean? =
                    runCatching {
                        vm.getItemStatusNullable(
                            belt = belt,
                            topic = key,
                            item = statusId
                        )
                    }.getOrNull()
                        ?: runCatching {
                            if (
                                vm.isMastered(
                                    belt = belt,
                                    topic = key,
                                    item = statusId
                                )
                            ) true else null
                        }.getOrNull()

                if (fromKey != null) {
                    valueFromVm = fromKey
                    break
                }
            }

            if (valueFromVm == null) {
                for (key in statusKeysFor(originalName)) {
                    val masteredKey = "mastered_${belt.id}_${key}"
                    val unknownKey = "unknown_${belt.id}_${key}"

                    val masteredSet =
                        prefs.getStringSet(masteredKey, emptySet<String>()) ?: emptySet()

                    val unknownSet =
                        prefs.getStringSet(unknownKey, emptySet<String>()) ?: emptySet()

                    val localValue: Boolean? = when {
                        masteredSet.contains(statusId) -> true
                        unknownSet.contains(statusId) -> false
                        else -> null
                    }

                    if (localValue != null) {
                        valueFromVm = localValue

                        vm.setItemStatusNullable(
                            belt = belt,
                            topic = key,
                            item = statusId,
                            value = localValue
                        )

                        break
                    }
                }
            }

            itemStates[statusId] = valueFromVm
        }
    }

    fun toggleStatus(rawItem: String) {
        val originalName = rawItem.trim()
        val statusId = statusIdFor(originalName)

        val nextValue = when (itemStates[statusId]) {
            null -> true
            true -> false
            false -> null
        }

        itemStates[statusId] = nextValue

        statusKeysFor(originalName).forEach { key ->
            vm.setItemStatusNullable(
                belt = belt,
                topic = key,
                item = statusId,
                value = nextValue
            )
        }

        setLocalStatus(
            rawItem = originalName,
            statusId = statusId,
            value = nextValue
        )
    }

    val title = if (isEnglish) {
        when (belt) {
            Belt.YELLOW -> "Yellow Belt"
            Belt.ORANGE -> "Orange Belt"
            Belt.GREEN -> "Green Belt"
            Belt.BLUE -> "Blue Belt"
            Belt.BROWN -> "Brown Belt"
            Belt.BLACK -> "Black Belt"
            else -> belt.en
        }
    } else {
        when (belt) {
            Belt.YELLOW -> "חגורה צהובה"
            Belt.ORANGE -> "חגורה כתומה"
            Belt.GREEN -> "חגורה ירוקה"
            Belt.BLUE -> "חגורה כחולה"
            Belt.BROWN -> "חגורה חומה"
            Belt.BLACK -> "חגורה שחורה"
            else -> belt.heb
        }
    }

    val displayItems = remember(items, isEnglish) {
        items.map { raw ->
            val original = raw.trim()
            original to exerciseTitleForUi(
                raw = original,
                isEnglish = isEnglish
            )
        }.filter { (original, display) ->
            original.isNotBlank() && display.isNotBlank()
        }
    }

    val knownCount = displayItems.count { pair ->
        val statusId = statusIdFor(pair.first)
        itemStates[statusId] == true
    }

    val unknownCount = displayItems.count { pair ->
        val statusId = statusIdFor(pair.first)
        itemStates[statusId] == false
    }

    val unmarkedCount = displayItems.count { pair ->
        val statusId = statusIdFor(pair.first)
        itemStates[statusId] == null
    }

    val favoriteCount = displayItems.count { pair ->
        val statusId = statusIdFor(pair.first)
        favorites.contains(statusId)
    }

    val excludedCount = displayItems.count { pair ->
        val statusId = statusIdFor(pair.first)
        excludedItems.contains(statusId)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (isDarkMode) {
            Color(0xFF111827)
        } else {
            belt.lightColor
        },
        tonalElevation = if (isDarkMode) 0.dp else 2.dp,
        shadowElevation = if (isDarkMode) 0.dp else 3.dp,
        border = if (isDarkMode) {
            BorderStroke(1.dp, belt.color.copy(alpha = 0.45f))
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
        ) {
            val countText = if (isEnglish) {
                if (displayItems.size == 1) {
                    "1 exercise"
                } else {
                    "${displayItems.size} exercises"
                }
            } else {
                "\u200E${displayItems.size}\u200E תרגילים"
            }

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Ltr
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            lineHeight = 17.sp
                        ),
                        color = belt.color,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 21.sp
                        ),
                        color = belt.color,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isEnglish) {
                    "← Swipe sideways to see more stats →"
                } else {
                    "→→ הזז לצד כדי לראות עוד נתונים →→"
                },
                color = Color(0xFF5B6472),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HardGroupStatChip(
                    value = knownCount.toString(),
                    label = if (isEnglish) "Known" else "יודע",
                    containerColor = Color(0xFF7ACB88)
                )

                HardGroupStatChip(
                    value = unknownCount.toString(),
                    label = if (isEnglish) "Unknown" else "לא יודע",
                    containerColor = Color(0xFFF1A97A)
                )

                HardGroupStatChip(
                    value = favoriteCount.toString(),
                    label = if (isEnglish) "Favorites" else "מועדפים",
                    containerColor = Color(0xFFE7A3B5)
                )

                HardGroupStatChip(
                    value = excludedCount.toString(),
                    label = if (isEnglish) "Excluded" else "מוחרגים",
                    containerColor = Color(0xFFE5A3A3)
                )

                HardGroupStatChip(
                    value = unmarkedCount.toString(),
                    label = if (isEnglish) "Unmarked" else "לא סומן",
                    containerColor = Color(0xFF8596C9)
                )
            }

            Spacer(Modifier.height(10.dp))

            displayItems.forEachIndexed { index, pair ->
                val originalName = pair.first
                val displayName = pair.second
                val statusId = statusIdFor(originalName)
                val mastered = itemStates[statusId]

                val actionId = statusId
                val noteText = remember(actionId, notesRefreshKey) {
                    loadNoteFor(actionId)
                }

                HardExerciseLegacyRow(
                    exerciseNumber = index + 1,
                    belt = belt,
                    itemName = originalName,
                    displayName = displayName,
                    mastered = mastered,
                    isDarkMode = isDarkMode,
                    excluded = excludedItems.contains(actionId),
                    isFav = favorites.contains(actionId),
                    hasNote = noteText.isNotBlank(),
                    onStatusClick = {
                        toggleStatus(originalName)
                    },
                    onInfoClick = {
                        onOpenExercise(originalName)
                    },
                    onToggleExclude = {
                        toggleExclude(actionId)
                    },
                    onToggleFavorite = {
                        toggleFavorite(actionId)
                    },
                    onEditNote = {
                        noteEditorFor = actionId
                        noteDraft = loadNoteFor(actionId)
                    }
                )

                if (index != displayItems.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    noteEditorFor?.let { itemId ->
        ExerciseNoteEditorDialog(
            noteText = noteDraft,
            isEnglish = isEnglish,
            accentColor = belt.color,
            onNoteChange = { noteDraft = it },
            onDismiss = {
                noteEditorFor = null
            },
            onSave = {
                val cleanNote = noteDraft.trim()
                noteDraft = cleanNote
                saveNoteFor(itemId, cleanNote)
                noteEditorFor = null
            }
        )
    }
}

@Composable
private fun HardExerciseLegacyRow(
    exerciseNumber: Int,
    belt: Belt,
    itemName: String,
    displayName: String = itemName,
    mastered: Boolean?,
    isDarkMode: Boolean = false,
    excluded: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    onStatusClick: () -> Unit,
    onInfoClick: () -> Unit,
    onToggleExclude: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNote: () -> Unit
) {
    val context = LocalContext.current
    val langManager = remember(context) { AppLanguageManager(context) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val rowBgColor = if (isDarkMode) {
        Color(0xFF1E293B)
    } else {
        Color.White
    }

    val rowTextColor = when {
        excluded && isDarkMode -> Color.White.copy(alpha = 0.45f)
        excluded -> Color.Gray
        isDarkMode -> Color(0xFFF8FAFC)
        else -> Color(0xFF263238)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = rowBgColor,
        tonalElevation = if (isDarkMode) 0.dp else 1.dp,
        shadowElevation = if (isDarkMode) 0.dp else 1.dp,
        border = BorderStroke(
            1.dp,
            if (isDarkMode) belt.color.copy(alpha = 0.55f) else Color.Transparent
        )
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .padding(
                        start = 8.dp,
                        top = 5.dp,
                        end = 0.dp,
                        bottom = 5.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEnglish) {
                    SubTopicItemFloatingActions(
                        isEnglish = true,
                        excluded = excluded,
                        isFav = isFav,
                        hasNote = hasNote,
                        onInfo = onInfoClick,
                        onToggleFavorite = onToggleFavorite,
                        onToggleExclude = onToggleExclude,
                        onEditNote = onEditNote
                    )

                    Spacer(Modifier.width(6.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onInfoClick() }
                            .padding(start = 2.dp, end = 4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HardLegacyMetaBadge(
                                text = "No. $exerciseNumber",
                                containerColor = belt.color.copy(alpha = 0.14f),
                                contentColor = Color(0xFF1F2937)
                            )

                            if (isFav) {
                                Spacer(Modifier.width(5.dp))
                                HardLegacyMetaBadge(
                                    text = "Favorite",
                                    containerColor = Color(0xFFF9D9B8),
                                    contentColor = Color(0xFF9A5A00)
                                )
                            }

                            if (excluded) {
                                Spacer(Modifier.width(5.dp))
                                HardLegacyMetaBadge(
                                    text = "Excluded",
                                    containerColor = Color(0xFFE5E7EB),
                                    contentColor = Color(0xFF6B7280)
                                )
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = displayName.trim(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = rowTextColor,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    HardMasterToggle(
                        mastered = mastered,
                        onClick = onStatusClick
                    )

                    Spacer(Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            .background(belt.color)
                    )
                } else {
                    HardMasterToggle(
                        mastered = mastered,
                        onClick = onStatusClick
                    )

                    Spacer(Modifier.width(6.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onInfoClick() }
                            .padding(start = 2.dp, end = 2.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.weight(1f))

                            if (excluded) {
                                HardLegacyMetaBadge(
                                    text = "מוחרג",
                                    containerColor = Color(0xFFE5E7EB),
                                    contentColor = Color(0xFF6B7280)
                                )
                                Spacer(Modifier.width(5.dp))
                            }

                            if (isFav) {
                                HardLegacyMetaBadge(
                                    text = "מועדף",
                                    containerColor = Color(0xFFF9D9B8),
                                    contentColor = Color(0xFF9A5A00)
                                )
                                Spacer(Modifier.width(5.dp))
                            }

                            HardLegacyMetaBadge(
                                text = "מס׳ $exerciseNumber",
                                containerColor = belt.color.copy(alpha = 0.14f),
                                contentColor = Color(0xFF1F2937)
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "\u200F${displayName.trim()}\u200F",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = rowTextColor,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(2.dp))

                    SubTopicItemFloatingActions(
                        isEnglish = false,
                        excluded = excluded,
                        isFav = isFav,
                        hasNote = hasNote,
                        onInfo = onInfoClick,
                        onToggleFavorite = onToggleFavorite,
                        onToggleExclude = onToggleExclude,
                        onEditNote = onEditNote
                    )

                    Spacer(Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            .background(belt.color)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubTopicItemFloatingActions(
    isEnglish: Boolean,
    excluded: Boolean,
    isFav: Boolean,
    hasNote: Boolean,
    onInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleExclude: () -> Unit,
    onEditNote: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val infoScale by animateFloatAsState(
        targetValue = if (expanded) 1.08f else 1f,
        animationSpec = tween(180),
        label = "subTopicInfoScale"
    )

    val infoRotation by animateFloatAsState(
        targetValue = if (expanded) 12f else 0f,
        animationSpec = tween(180),
        label = "subTopicInfoRotation"
    )

    Box {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = Color(0xFF60717A),
            shadowElevation = 3.dp,
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.22f)
            ),
            modifier = Modifier
                .size(27.dp)
                .graphicsLayer {
                    scaleX = infoScale
                    scaleY = infoScale
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = infoRotation
                    }
                )
            }
        }

        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                    if (isEnglish) {
                        androidx.compose.ui.unit.LayoutDirection.Ltr
                    } else {
                        androidx.compose.ui.unit.LayoutDirection.Rtl
                    }
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.99f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.97f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isEnglish) "Info" else "מידע",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        expanded = false
                        onInfo()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = when {
                                isEnglish && isFav -> "Remove from favorites"
                                isEnglish -> "Add to favorites"
                                isFav -> "הסר ממועדפים"
                                else -> "הוסף למועדפים"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        expanded = false
                        onToggleFavorite()

                        android.widget.Toast
                            .makeText(
                                context,
                                when {
                                    isEnglish && isFav -> "Removed from favorites."
                                    isEnglish -> "Added to favorites."
                                    isFav -> "הוסר מהמועדפים."
                                    else -> "נוסף למועדפים."
                                },
                                android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = when {
                                isEnglish && excluded -> "Cancel exclusion"
                                isEnglish -> "Exclude from practice"
                                excluded -> "בטל החרגה"
                                else -> "החרג מהתרגול"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        expanded = false
                        onToggleExclude()

                        android.widget.Toast
                            .makeText(
                                context,
                                when {
                                    isEnglish && excluded -> "Exclusion canceled."
                                    isEnglish -> "Exercise excluded from practice."
                                    excluded -> "בוטלה ההחרגה."
                                    else -> "התרגיל הוחרג מהתרגול."
                                },
                                android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = when {
                                isEnglish && hasNote -> "Edit / delete note"
                                isEnglish -> "Add exercise note"
                                hasNote -> "ערוך / מחק הערה"
                                else -> "הוסף הערה לתרגיל"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
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
}

@Composable
private fun HardMasterToggle(
    mastered: Boolean?,
    onClick: () -> Unit
) {
    val bg = when (mastered) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> Color.White
    }

    val border = when (mastered) {
        true -> Color(0xFF1B5E20)
        false -> Color(0xFF8E1B1B)
        null -> Color(0xFFCBD5E1)
    }

    Surface(
        modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.5.dp, border),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (mastered) {
                true -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "יודע",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )

                false -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "לא יודע",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )

                null -> Spacer(Modifier.size(1.dp))
            }
        }
    }
}

/* ---------------------- ✅ NEW: שורת תרגיל עם אייקון הסבר ---------------------- */
@Composable
private fun ExerciseRowWithInfo(
    belt: Belt,
    itemName: String,
    displayName: String = itemName,
    accent: Color,
    onExplain: (Belt, String) -> Unit,
    onOpenExercise: (String) -> Unit
) {
    val context = LocalContext.current
    val langManager = remember(context) { AppLanguageManager(context) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val isDarkMode = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val rowBgColor = if (isDarkMode) {
        Color(0xFF1E293B)
    } else {
        Color.White.copy(alpha = 0.94f)
    }

    val rowTextColor = if (isDarkMode) {
        Color(0xFFF8FAFC)
    } else {
        Color(0xFF1F2933)
    }

    val infoTint = if (isDarkMode) {
        belt.color.copy(alpha = 0.95f)
    } else {
        Color(0xFF6F55C8)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = rowBgColor,
        tonalElevation = if (isDarkMode) 0.dp else 1.dp,
        shadowElevation = if (isDarkMode) 0.dp else 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDarkMode) {
                belt.color.copy(alpha = 0.32f)
            } else {
                Color.White.copy(alpha = 0.70f)
            }
        )
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenExercise(itemName) }
                    .padding(start = 14.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onExplain(belt, itemName) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = if (isEnglish) "Explanation" else "הסבר",
                        tint = infoTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                            if (isEnglish) {
                                androidx.compose.ui.unit.LayoutDirection.Ltr
                            } else {
                                androidx.compose.ui.unit.LayoutDirection.Rtl
                            }
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = rowTextColor,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )
                }
            }
        }
    }
}
