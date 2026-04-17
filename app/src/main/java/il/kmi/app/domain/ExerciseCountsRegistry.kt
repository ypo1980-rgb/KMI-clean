package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.HardSectionsCatalog
import il.kmi.shared.domain.content.HardSectionsCatalog.itemsFor
import il.kmi.shared.domain.content.HardSectionsCatalog.totalItemsCount

/**
 * Registry מרכזי לכל ספירות התרגילים באפליקציה.
 *
 * מטרה:
 * - source of truth אחד לספירות UI
 * - בלי לפזר פונקציות counts ב-NavGraphs
 * - בלי קבצים נפרדים לכל תחום
 *
 * הערה:
 * כרגע אנחנו מרכזים כאן קודם כל את כל הספירות של ההגנות / שחרורים / נושאים קשיחים,
 * ובהמשך אפשר להעביר לכאן גם ספירות נוספות של subjects רגילים.
 */
object ExerciseCountsRegistry {

    // ---------------------------------------------------------
    // Generic helpers
    // ---------------------------------------------------------

    fun hardSubjectCount(subjectId: String): Int {
        val sections = HardSectionsCatalog.sectionsForSubject(subjectId).orEmpty()
        return sections.sumOf { it.totalItemsCount() }
    }

    fun hardSectionCount(subjectId: String, sectionId: String): Int {
        return HardSectionsCatalog.findSectionById(subjectId, sectionId)
            ?.totalItemsCount()
            ?: 0
    }

    // ---------------------------------------------------------
    // Defenses / hard-catalog counts
    // ---------------------------------------------------------

    fun defenseCount(kindRaw: String, pickRaw: String): Int =
        HardSectionsCatalog.defenseCount(kindRaw, pickRaw)

    fun kicksHardSubCounts(): Map<String, Int> = linkedMapOf(
        "הגנות נגד בעיטות ישרות / למפשעה" to
                HardSectionsCatalog.defenseCount("kicks_hard", "straight_groin"),
        "הגנות נגד מגל / מגל לאחור" to
                HardSectionsCatalog.defenseCount("kicks_hard", "hook_back"),
        "הגנות נגד ברך" to
                HardSectionsCatalog.defenseCount("kicks_hard", "knee"),
    )

    fun defenseDialogCounts(): Map<String, Int> = linkedMapOf(
        "הגנות פנימיות" to (
                HardSectionsCatalog.defenseCount("internal", "punch") +
                        HardSectionsCatalog.defenseCount("internal", "kick")
                ),
        "הגנות חיצוניות" to (
                HardSectionsCatalog.defenseCount("external", "punch") +
                        HardSectionsCatalog.defenseCount("external", "kick")
                ),
        "הגנות נגד בעיטות" to (
                HardSectionsCatalog.defenseCount("kicks_hard", "straight_groin") +
                        HardSectionsCatalog.defenseCount("kicks_hard", "hook_back") +
                        HardSectionsCatalog.defenseCount("kicks_hard", "knee")
                ),
        "הגנות מסכין" to HardSectionsCatalog.defenseCount("knife_hard", "all"),

        "הגנות עם רובה נגד דקירות סכין" to
                HardSectionsCatalog.defenseCount("knife_rifle_hard", "all"),

        "הגנות מאיום אקדח" to HardSectionsCatalog.defenseCount("gun_hard", "all"),

        "הגנות נגד מספר תוקפים" to
                HardSectionsCatalog.defenseCount("multiple_attackers_hard", "all"),

        "הגנות נגד מקל" to HardSectionsCatalog.defenseCount("stick_hard", "all"),
    )

    fun defensePickCounts(): Map<String, Int> = linkedMapOf(
        "INTERNAL:אגרופים" to HardSectionsCatalog.defenseCount("internal", "punch"),
        "INTERNAL:בעיטות" to HardSectionsCatalog.defenseCount("internal", "kick"),
        "EXTERNAL:אגרופים" to HardSectionsCatalog.defenseCount("external", "punch"),
        "EXTERNAL:בעיטות" to HardSectionsCatalog.defenseCount("external", "kick"),
    )

    fun totalDefenseCount(): Int =
        defenseDialogCounts().values.sum()

    // ---------------------------------------------------------
    // Releases counts
    // ---------------------------------------------------------

    fun releasesRootCounts(): Map<String, Int> = linkedMapOf(
        "שחרור מתפיסות ידיים / שיער / חולצה" to hardSectionCount(
            subjectId = "releases",
            sectionId = "releases_hands_hair_shirt"
        ),
        "שחרור מחניקות" to hardSectionCount(
            subjectId = "releases",
            sectionId = "releases_chokes"
        ),
        "שחרור מחביקות" to hardSectionCount(
            subjectId = "releases",
            sectionId = "releases_hugs"
        ),
    )

    fun releasesHugsSubCounts(): Map<String, Int> = linkedMapOf(
        "חביקות גוף" to hardSectionCount(
            subjectId = "releases",
            sectionId = "releases_hugs_body"
        ),
        "חביקות צוואר" to hardSectionCount(
            subjectId = "releases",
            sectionId = "releases_hugs_neck"
        ),
        "חביקות זרוע" to hardSectionCount(
            subjectId = "releases",
            sectionId = "releases_hugs_arm"
        ),
    )

    fun totalReleasesCount(): Int =
        hardSubjectCount("releases")

    // ---------------------------------------------------------
    // Hands counts
    // ---------------------------------------------------------

    fun handsSubCounts(): Map<String, Int> = linkedMapOf(
        "מכות יד" to hardSectionCount(
            subjectId = "hands_all",
            sectionId = "hands_strikes"
        ),
        "מכות מרפק" to hardSectionCount(
            subjectId = "hands_all",
            sectionId = "hands_elbows"
        ),
    )

    fun totalHandsCount(): Int =
        hardSubjectCount("hands_all")

    // ---------------------------------------------------------
    // Weapon / hard-subject totals
    // ---------------------------------------------------------

    fun totalKnifeDefenseCount(): Int =
        hardSubjectCount("knife_defense")

    fun totalGunThreatDefenseCount(): Int =
        hardSubjectCount("gun_threat_defense")

    fun totalStickDefenseCount(): Int =
        hardSubjectCount("stick_defense")

    fun totalKicksHardCount(): Int =
        hardSubjectCount("kicks_hard")

    // ---------------------------------------------------------
    // Optional belt helpers
    // ---------------------------------------------------------

    fun hardSectionItemsByBelt(
        subjectId: String,
        sectionId: String
    ): List<Pair<Belt, List<String>>> {
        val section = HardSectionsCatalog.findSectionById(subjectId, sectionId) ?: return emptyList()

        return HardSectionsCatalog.beltOrder.mapNotNull { belt ->
            val items = section.itemsFor(belt)
            if (items.isNotEmpty()) belt to items else null
        }
    }
}