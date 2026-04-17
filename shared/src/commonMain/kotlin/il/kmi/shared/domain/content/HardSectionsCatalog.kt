package il.kmi.shared.domain.content

import il.kmi.shared.domain.Belt
import kotlin.collections.LinkedHashSet

object HardSectionsCatalog {

    data class BeltGroup(
        val belt: Belt,
        val items: List<String>
    )

    data class Section(
        val id: String,
        val title: String,
        val beltGroups: List<BeltGroup> = emptyList(),
        val subSections: List<Section> = emptyList()
    )

    fun Section.totalItemsCount(): Int =
        if (subSections.isNotEmpty()) {
            subSections.sumOf { it.totalItemsCount() }
        } else {
            beltGroups.sumOf { it.items.size }
        }

    fun Section.itemsFor(belt: Belt): List<String> =
        if (subSections.isNotEmpty()) {
            subSections.flatMap { it.itemsFor(belt) }
        } else {
            beltGroups.firstOrNull { it.belt == belt }?.items.orEmpty()
        }

    fun Section.isLeaf(): Boolean = subSections.isEmpty()

    fun Section.hasItems(): Boolean =
        beltGroups.any { it.items.isNotEmpty() } || subSections.any { it.hasItems() }

    fun Section.directItemsCount(): Int =
        beltGroups.sumOf { it.items.size }

    fun findSectionById(subjectId: String, sectionId: String): Section? {
        val roots = sectionsForSubject(subjectId).orEmpty()
        return roots.firstNotNullOfOrNull { it.findDeep(sectionId) }
    }

    fun findSectionPath(subjectId: String, sectionId: String): List<Section> {
        val roots = sectionsForSubject(subjectId).orEmpty()
        return roots.firstNotNullOfOrNull { it.findPathDeep(sectionId) }.orEmpty()
    }

    private fun Section.findDeep(sectionId: String): Section? {
        if (id == sectionId) return this
        return subSections.firstNotNullOfOrNull { it.findDeep(sectionId) }
    }

    private fun Section.findPathDeep(sectionId: String): List<Section>? {
        if (id == sectionId) return listOf(this)

        for (child in subSections) {
            val childPath = child.findPathDeep(sectionId)
            if (childPath != null) return listOf(this) + childPath
        }

        return null
    }

    private fun canonicalSubjectId(raw: String): String {
        val t = raw.trim()
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")

        return when {
            t == "שחרורים" -> "releases"

            t == "שחרור מתפיסות" ||
                    t == "שחרורים מתפיסות" ||
                    t == "שחרור מתפיסות ידיים" ||
                    t == "שחרורים מתפיסות ידיים" ||
                    t == "שחרור מתפיסות ידיים / שיער / חולצה" ||
                    t == "שחרורים מתפיסות ידיים / שיער / חולצה" ||
                    t.contains("תפיסות יד") ||
                    t.contains("שיער") ||
                    t.contains("חולצה") ->
                "releases_hands_hair_shirt"

            t == "שחרור מחניקות" ||
                    t == "שחרורים מחניקות" ||
                    t.contains("חניקות") ->
                "releases_chokes"

            t == "שחרור מחביקות" ||
                    t == "שחרורים מחביקות" ||
                    t.contains("מחביקות") ||
                    t.contains("חביקות גוף") ||
                    t.contains("חביקות צוואר") ||
                    t.contains("חביקות צואר") ||
                    t.contains("חביקות זרוע") ->
                "releases_hugs"

            t == "הגנות" -> "defenses_root"
            t == "הגנות מסכין" -> "knife_defense"
            t == "הגנות עם רובה נגד דקירות סכין" -> "knife_rifle_defense"
            t == "הגנות מאיום אקדח" -> "gun_threat_defense"
            t == "הגנות נגד מקל" -> "stick_defense"
            t == "הגנות נגד מספר תוקפים" -> "multiple_attackers_defense"
            t == "הגנות נגד 2 תוקפים" -> "multiple_attackers_defense"
            t == "עבודת ידיים" -> "hands_all"
            t == "מכות יד" -> "hands_strikes"
            t == "מכות מרפק" -> "hands_elbows"
            t == "מכות במקל / רובה" -> "hands_stick_rifle"
            t == "מכות ידיים" -> "topic_hands"
            t == "הגנות נגד בעיטות" -> "kicks_hard"
            t == "הגנות פנימיות - אגרופים" -> "def_internal_punch"
            t == "הגנות פנימיות - בעיטות" -> "def_internal_kick"
            t == "הגנות חיצוניות - אגרופים" -> "def_external_punch"
            t == "הגנות חיצוניות - בעיטות" -> "def_external_kick"

            t == "כללי" -> "topic_general"
            t == "בעיטות" -> "topic_kicks"
            t == "בלימות וגלגולים" -> "topic_breakfalls_rolls"
            t == "עמידת מוצא" -> "topic_ready_stance"
            t == "הכנה לעבודת קרקע" -> "topic_ground_prep"
            t == "קוואלר" -> "topic_Kavaler"

            else -> t
        }
    }

    val supportedSubjectIds: Set<String> = setOf(
        "releases",
        "defenses_root",
        "knife_defense",
        "knife_rifle_defense",
        "gun_threat_defense",
        "stick_defense",
        "multiple_attackers_defense",
        "hands_all",
        "hands_strikes",
        "hands_elbows",
        "hands_stick_rifle",
        "topic_hands",
        "kicks_hard",
        "def_internal_punch",
        "def_internal_kick",
        "def_external_punch",
        "def_external_kick",
        "topic_general",
        "topic_kicks",
        "topic_breakfalls_rolls",
        "topic_ready_stance",
        "topic_ground_prep",
        "topic_kavaler",
    )

    fun supportsSubject(subjectId: String): Boolean {
        val id = canonicalSubjectId(subjectId)

        return when {
            id in supportedSubjectIds -> true
            id.startsWith("releases_") -> findSectionById("releases", id) != null
            else -> false
        }
    }

    // ✅ NEW
    fun sectionsForSubject(subjectId: String): List<Section>? {
        val id = canonicalSubjectId(subjectId)

        return when {
            id == "releases" -> releases

            id.startsWith("releases_") -> {
                val section = findSectionById("releases", id) ?: return null
                if (section.subSections.isNotEmpty()) {
                    section.subSections
                } else {
                    listOf(section)
                }
            }

            id == "defenses_root" -> defensesRoot
            id == "knife_defense" -> defensesKnife.filter { it.id == "knife_defense_main" }
            id == "knife_rifle_defense" -> defensesKnife.filter { it.id == "knife_defense_rifle_against_knife_stabs" }
            id == "gun_threat_defense" -> defensesGunThreat
            id == "stick_defense" -> defensesStick
            id == "multiple_attackers_defense" -> defensesMultipleAttackers
            id == "kicks_hard" -> defensesKicks
            id == "hands_all" -> handsAll
            id == "hands_strikes" -> handsAll.filter { it.id == "hands_strikes" }
            id == "hands_elbows" -> handsAll.filter { it.id == "hands_elbows" }
            id == "hands_stick_rifle" -> handsAll.filter { it.id == "hands_stick_rifle" }
            id == "topic_hands" -> handsAll
            id == "def_internal_punch" -> defensesInternalPunch
            id == "def_internal_kick" -> defensesInternalKick
            id == "def_external_punch" -> defensesExternalPunch
            id == "def_external_kick" -> defensesExternalKick
            id == "topic_general" -> topicGeneral
            id == "topic_kicks" -> topicKicks
            id == "topic_breakfalls_rolls" -> topicBreakfallsRolls
            id == "topic_ready_stance" -> topicReadyStance
            id == "topic_ground_prep" -> topicGroundPrep
            id == "topic_kavaler" -> topicKavaler

            else -> null
        }
    }

    fun subjectDisplayTitle(subjectId: String): String? {
        return when (canonicalSubjectId(subjectId)) {
            "releases" -> "שחרורים"
            "defenses_root" -> "הגנות"
            "knife_defense" -> "הגנות מסכין"
            "knife_rifle_defense" -> "הגנות עם רובה נגד דקירות סכין"
            "gun_threat_defense" -> "הגנות מאיום אקדח"
            "stick_defense" -> "הגנות נגד מקל"
            "multiple_attackers_defense" -> "הגנות נגד מספר תוקפים"
            "kicks_hard" -> "הגנות נגד בעיטות"
            "hands_all" -> "עבודת ידיים"
            "hands_strikes" -> "מכות יד"
            "hands_elbows" -> "מכות מרפק"
            "hands_stick_rifle" -> "מכות במקל / רובה"
            "topic_hands" -> "מכות ידיים"
            "def_internal_punch" -> "הגנות פנימיות - אגרופים"
            "def_internal_kick" -> "הגנות פנימיות - בעיטות"
            "def_external_punch" -> "הגנות חיצוניות - אגרופים"
            "def_external_kick" -> "הגנות חיצוניות - בעיטות"
            "topic_general" -> "כללי"
            "topic_kicks" -> "בעיטות"
            "topic_breakfalls_rolls" -> "בלימות וגלגולים"
            "topic_ready_stance" -> "עמידת מוצא"
            "topic_ground_prep" -> "הכנה לעבודת קרקע"
            "topic_kavaler" -> "קוואלר"
            else -> null
        }
    }

    fun subjectItemsFor(subjectId: String, belt: Belt): List<String> {
        val roots = sectionsForSubject(subjectId).orEmpty()
        if (roots.isEmpty()) return emptyList()

        val seen = LinkedHashSet<String>()

        roots.forEach { section ->
            section.itemsFor(belt)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { seen.add(it) }
        }

        return seen.toList()
    }

    fun subjectSubSectionsFor(subjectId: String): List<Section> {
        return sectionsForSubject(subjectId)
            .orEmpty()
            .filter { it.hasItems() }
    }

    fun subjectSubSectionItemsFor(
        subjectId: String,
        subSectionId: String,
        belt: Belt
    ): List<String> {
        val roots = sectionsForSubject(subjectId).orEmpty()
        val target = roots.firstOrNull { it.id == subSectionId } ?: return emptyList()

        return target.itemsFor(belt)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun findAnySectionById(sectionId: String): Section? {
        val id = sectionId.trim()
        supportedSubjectIds.forEach { subjectId ->
            val roots = sectionsForSubject(subjectId).orEmpty()
            roots.firstNotNullOfOrNull { it.findDeep(id) }?.let { return it }
        }
        return null
    }

    // ----------------------------
    // כללי – לפי חגורה
    // ----------------------------
    val topicGeneral: List<Section> = listOf(
        Section(
            id = "topic_general_main",
            title = "כללי",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "תזוזות",
                        "גלגול לפנים – צד ימין",
                        "הוצאות אגן, הרמת אגן והפניית גוף למעלה ",
                        "צל בוקס",
                        "סגירת אגרוף",
                        "אצבעות לפנים",
                        "מכת קשת האצבע והאגודל"
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "גלגול לאחור צד ימין",
                        "גלגול לאחור צד שמאל",
                        "גלגול לפנים צד שמאל",
                        "שילובי ידיים רגליים",
                        "בלימה לצד ימין",
                        "בלימה לצד שמאל"
                    )
                ),
                BeltGroup(belt = Belt.GREEN, items = emptyList()),
                BeltGroup(belt = Belt.BLUE, items = emptyList()),
                BeltGroup(belt = Belt.BROWN, items = emptyList()),
                BeltGroup(belt = Belt.BLACK, items = emptyList())
            )
        )
    )

    // ----------------------------
    // בעיטות – לפי חגורה
    // ----------------------------
    val topicKicks: List<Section> = listOf(
        Section(
            id = "topic_kicks_main",
            title = "בעיטות",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "בעיטה רגילה למפסעה",
                        "בעיטה רגילה לסנטר",
                        "בעיטת מגל נמוכה",
                        "בעיטת מגל אופקית",
                        "בעיטת מגל אלכסונית",
                        "בעיטת מגל בהטעיה",
                        "בעיטת ברך גבוהה",
                        "בעיטת ברך מהצד",
                        "בעיטת ברך נמוכה למפסעה",
                        "בעיטה לצד מעמידת פיסוק"
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "בעיטה רגילה בעקב לסנטר",
                        "בעיטת הגנה לפנים",
                        "בעיטת סנוקרת לאחור",
                        "בעיטה לצד בשיכול",
                        "בעיטה רגילה לאחור",
                        "בעיטה לצד בנסיגה",
                        "בעיטת הגנה לאחור",
                        "בעיטת סטירה פנימית",
                        "בעיטת עצירה בכף הרגל האחורית",
                        "בעיטת עצירה בכף הרגל הקדמית",
                        "בעיטה רגילה ובעיטת מגל ברגל השנייה",
                        "שילובי בעיטות",
                        "ניתור ברגל ימין ובעיטה רגילה ברגל ימין"
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "בעיטה רגילה ובעיטת מגל באותה רגל",
                        "בעיטת מגל לאחור בשיכול אחורי",
                        "בעיטה לצד בסיבוב",
                        "בעיטת סטירה חיצונית"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLUE,
                    items = listOf(
                        "בעיטת פטיש",
                        "בעיטת גזיזה אחורית",
                        "בעיטת גזיזה קדמית",
                        "בעיטת גזיזה קדמית ובעיטת גזיזה אחורית בסיבוב",
                        "בעיטת מגל לאחור בסיבוב",
                        "בעיטת סטירה חיצונית בסיבוב"
                    )
                ),
                BeltGroup(
                    belt = Belt.BROWN,
                    items = listOf(
                        "בעיטת מגל בניתור",
                        "בעיטה רגילה ובעיטת מגל בניתור",
                        "בעיטת מגל כפולה בניתור"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLACK,
                    items = listOf(
                        "ניתור ברגל שמאל ובעיטה רגילה ברגל ימין",
                        "ניתור ברגל שמאל ובעיטה לצד ברגל ימין",
                        "ניתור ברגל שמאל ובעיטה לצד ברגל שמאל",
                        "בעיטת לצד בסיבוב מלא בניתור",
                        "בעיטת מגל לאחור בסיבוב בניתור",
                        "בעיטת הגנה לאחור בניתור"
                    )
                )
            )
        )
    )

    // ----------------------------
    // בלימות וגלגולים – לפי חגורה
    // ----------------------------
    val topicBreakfallsRolls: List<Section> = listOf(
        Section(
            id = "topic_breakfalls_rolls_main",
            title = "בלימות וגלגולים",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "בלימה רכה לפנים",
                        "בלימה לאחור",
                        "גלגול לפנים - ימין"
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "בלימה לצד ימין",
                        "בלימה לצד שמאל",
                        "גלגול לפנים - שמאל",
                        "גלגול לאחור - ימין",
                        "גלגול לאחור שמאל"
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "בלימה לאחור מגובה",
                        "בלימה לצד כהכנה לגזיזות",
                        "גלגול לפנים ובלימה לאחור - ימין",
                        "גלגול לפנים ובלימה לאחור - שמאל",
                        "גלגול לפנים ולאחור - ימין",
                        "גלגול לפנים ולאחור - שמאל",
                        "גלגול ביד אחת - ימין",
                        "גלגול ביד אחת - שמאל",
                        "גלגול לפנים קימה לפנים"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLUE,
                    items = listOf(
                        "מניעת נפילה מחביקת שוקיים מלפנים להפלה",
                        "גלגול לצד — ימין",
                        "גלגול לצד — שמאל",
                        "גלגול ברחיפה — ימין",
                        "גלגול ברחיפה — שמאל",
                        "גלגול לגובה — ימין",
                        "גלגול לגובה — שמאל",
                        "גלגול ללא ידיים — ימין",
                        "גלגול ללא ידיים — שמאל"
                    )
                ),
                BeltGroup(
                    belt = Belt.BROWN,
                    items = listOf(
                        "גלגול עם רובה"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLACK,
                    items = emptyList()
                )
            )
        )
    )

    // ----------------------------
    // נושאים רגילים נוספים – לפי חגורה
    // ----------------------------
    val topicReadyStance: List<Section> = listOf(
        Section(
            id = "topic_ready_stance_main",
            title = "עמידת מוצא",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "עמידת מוצא רגילה",
                        "עמידת מוצא להגנות פנימיות",
                        "עמידת מוצא להגנות חיצוניות",
                        "עמידת מוצא צידית",
                        "עמידת מוצא כללית מספר 1",
                        "עמידת מוצא כללית מספר 2"
                    )
                ),
                BeltGroup(belt = Belt.ORANGE, items = emptyList()),
                BeltGroup(belt = Belt.GREEN, items = emptyList()),
                BeltGroup(belt = Belt.BLUE, items = emptyList()),
                BeltGroup(belt = Belt.BROWN, items = emptyList()),
                BeltGroup(belt = Belt.BLACK, items = emptyList())
            )
        )
    )

    val topicGroundPrep: List<Section> = listOf(
        Section(
            id = "topic_ground_prep_main",
            title = "הכנה לעבודת קרקע",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "מוצא לעבודת קרקע",
                        "הוצאת אגן",
                        "הרמת אגן והפניית גוף לכיוון ההפלה"
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "הגנה נגד אגרופים בשכיבה"
                    )
                )
            )
        )
    )

    val topicKavaler: List<Section> = listOf(
        Section(
            id = "topic_Kavaler_main",
            title = "קוואלר",
            beltGroups = listOf(
                BeltGroup(belt = Belt.YELLOW, items = emptyList()),
                BeltGroup(belt = Belt.ORANGE, items = emptyList()),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "קוואלר - הליכה לאחור",
                        "קוואלר נגד התנגדות - הליכה לפנים",
                        "קוואלר - אגודלים",
                        "קוואלר – מרפק"
                    )
                ),
                BeltGroup(belt = Belt.BLUE, items = emptyList()),
                BeltGroup(belt = Belt.BROWN, items = emptyList()),
                BeltGroup(belt = Belt.BLACK, items = emptyList())
            )
        )
    )

    // ----------------------------
    // הגנות נגד בעיטות – תתי נושאים קשיחים (מוצג לפי חגורה)
    // ----------------------------
    val defensesKicks: List<Section> = listOf(
        Section(
            id = "kicks_straight_groin",
            title = "הגנות נגד בעיטות ישרות / למפשעה",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "הגנה פנימית נגד בעיטה רגילה למפסעה"
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "הגנה חיצונית נגד בעיטה רגילה",
                        "הגנה נגד בעיטה רגילה - עצירה ברגל הקדמית",
                        "הגנה נגד בעיטה רגילה - עצירה ברגל האחורית"
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "הגנה נגד בעיטה רגילה - בעיטה לצד",
                        "הגנה נגד בעיטה רגילה - טיימינג לצד החי",
                        "הגנה חיצונית באמת שמאל נגד בעיטה רגילה"
                    )
                )
            )
        ),

        Section(
            id = "kicks_roundhouse_back_roundhouse",
            title = "הגנות נגד מגל / מגל לאחור",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בימין",
                        "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בשמאל",
                        "הגנה נגד בעיטת מגל לפנים באמות הידיים",
                        "הגנה חיצונית נגד בעיטת מגל לפנים - אגרוף בימין",
                        "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל האחורית",
                        "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל הקדמית"
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "הגנה נגד בעיטת מגל לפנים – בעיטה לצד",
                        "הגנה נגד בעיטת מגל נמוכה",
                        "הגנה נגד בעיטת מגל לאחור - בעיטה בימין",
                        "הגנה נגד בעיטת מגל לאחור - בעיטה שמאל",
                        "הגנה נגד בעיטת מגל לאחור - אגרוף שמאל",
                        "הגנה נגד בעיטת מגל לאחור בסיבוב – בעיטה"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLUE,
                    items = listOf(
                        "הגנה נגד בעיטת מגל לפנים עם השוק",
                        "הגנה נגד בעיטת מגל לצלעות",
                        "הגנה נגד בעיטת מגל לפנים - בעיטה לצד",
                        "הגנה נגד בעיטת מגל לפנים - בעיטה לאחור"
                    )
                ),
                BeltGroup(
                    belt = Belt.BROWN,
                    items = listOf(
                        "הגנה נגד בעיטת מגל – פריצה",
                        "הגנה חיצונית נגד מגל לפנים – גזיזה",
                        "הגנה חיצונית נגד מגל לפנים – טאטוא",
                        "הגנה נגד בעיטת מגל לאחור – פריצה"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLACK,
                    items = listOf(
                        "הגנה נגד בעיטת מגל לפנים לראש – הדיפה באמת שמאל",
                        "הגנה נגד בעיטת מגל לפנים לראש – רגל עברה מעל הראש",
                        "הגנה נגד מגל לפנים לראש – התחמקות גוף בסיבוב וגזיזה"
                    )
                )
            )
        ),

        Section(
            id = "kicks_side_kick",
            title = "הגנות נגד בעיטה לצד",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "בעיטת עצירה נגד בעיטה לצד"
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "הגנה חיצונית באמת ימין נגד בעיטה לצד",
                        "הגנה חיצונית באמת שמאל נגד בעיטה לצד",
                        "הגנה נגד בעיטת לצד בעיטת סטירה חיצונית"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLUE,
                    items = listOf(
                        "הגנה פנימית באמת ימין נגד בעיטה לצד",
                    )
                )
            )
        ),


                        Section(
            id = "kicks_knee",
            title = "הגנות נגד ברך",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "הגנה נגד בעיטת ברך"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLUE,
                    items = listOf(
                        "הגנות נגד בעיטת ברך מלפנים",
                        "הגנות נגד בעיטת ברך מהצד"
                    )
                )
            )
        )
    )

    // ----------------------------
// הגנות מסכין – לפי חגורה
// ----------------------------
    val defensesKnife: List<Section> = listOf(
        Section(
            id = "knife_defense_main",
            title = "הגנות מסכין",
            beltGroups = listOf(
                BeltGroup(belt = Belt.YELLOW, items = emptyList()),

                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "הגנות יד רפלקסיביות נגד דקירות רגילות",
                        "הגנות יד רפלקסיביות נגד דקירות מזרחיות",
                        "הגנות יד רפלקסיביות נגד דקירה ישרה"
                    )
                ),

                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "הגנה מאיום סכין לעורק שמאל",
                        "הגנה מאיום סכין לעורק ימין",
                        "הגנה מאיום סכין מאחור – להב לגרוגרת",
                        "הגנה מאיום סכין מלפנים - חוד הסכין לגורגרת",
                        "הגנה מאיום סכין מאחור - להב הסכין לגורגרת",
                        "הגנה מאיום סכין - חוד לבטן התחתונה",
                        "הגנה מאיום סכין מאחור – חוד לגב",
                        "הגנה נגד דקירה רגילה – בעיטה",
                        "הגנה נגד דקירה מזרחית - בעיטה",
                        "הגנה נגד דקירה ישרה מלפנים – בעיטה",
                        "הגנה נגד דקירה ישרה נמוכה – בעיטה",
                        "הגנה נגד דקירה ישרה מלפנים – הגנת גוף ובעיטת מגל למפסעה",
                        "הגנה נגד דקירה רגילה מהצד - בעיטה",
                        "הגנה נגד דקירה ישרה - בעיטה",
                        "הגנה נגד דקירה מזרחית מהצד - בעיטה",
                        "הגנה נגד דקירה רגילה מהצד - התוקף בצד שמאל",
                        "הגנה נגד דקירה רגילה מהצד - התוקף בצד ימין",
                        "הגנה נגד דקירה מזרחית מהצד לעורף - התוקף בצד שמאל",
                        "הגנה נגד דקירה מזרחית מהצד לגב - התוקף בצד שמאל",
                        "הגנה נגד דקירה מזרחית מהצד לגרון - התוקף בצד ימין",
                        "הגנה נגד דקירה מזרחית מהצד לבטן - התוקף בצד ימין"
                    )
                ),

                BeltGroup(
                    belt = Belt.BLUE,
                    items = listOf(
                        "הגנה מאיום סכין לעורק שמאל",
                        "הגנה מאיום סכין לעורק ימין",
                        "הגנה מאיום סכין – להב לגרוגרת",
                        "הגנה מאיום סכין מלפנים - חוד הסכין לגורגרת",
                        "הגנה מאיום סכין מאחור - להב הסכין לגורגרת",
                        "הגנה מאיום סכין מאחור - חוד לגב",
                        "הגנה מאיום סכין מאחור – להב על העורף",
                        "הגנה נגד דקירה מזרחית - יד",
                        "הגנה נגד דקירה ישרה נמוכה",
                        "הגנה נגד דקירה ישרה מהצד - צד מת",
                        "הגנה נגד דקירה ישרה מהצד - צד חי",
                        "הגנה נגד דקירה ישרה - צד חי",
                        "הגנה נגד דקירה ישרה - צד מת"
                    )
                ),

                BeltGroup(
                    belt = Belt.BROWN,
                    items = listOf(
                        "הגנה נגד סכין בשיסוף – הטיה והגנה לצד החי",
                        "הגנה נגד סכין בשיסוף – הטיה והגנה לצד המת",
                        "הגנה נגד סכין בשיסוף – פריצה והגנה לצד החי",
                        "הגנה נגד סכין בשיסוף – פריצה והגנה לצד המת"
                    )
                ),

                BeltGroup(
                    belt = Belt.BLACK,
                    items = listOf(
                        "הגנה נגד סכין שיסוף מהצד החי – בצד ימין",
                        "הגנה נגד סכין שיסוף מהצד החי – בצד שמאל",
                        "הגנה נגד סכין שיסוף מהצד המת – בצד ימין",
                        "הגנה נגד סכין שיסוף מהצד המת – בצד שמאל"
                    )
                )
            )
        ),
                    Section(
                    id = "knife_defense_rifle_against_knife_stabs",
            title = "הגנות עם רובה נגד דקירות סכין",
            beltGroups = listOf(
                BeltGroup(belt = Belt.YELLOW, items = emptyList()),
                BeltGroup(belt = Belt.ORANGE, items = emptyList()),
                BeltGroup(belt = Belt.GREEN, items = emptyList()),
                BeltGroup(belt = Belt.BLUE, items = emptyList()),
                BeltGroup(belt = Belt.BROWN, items = emptyList()),
                BeltGroup(
                    belt = Belt.BLACK,
                    items = listOf(
                        "רובה נגד דקירה ישרה גבוהה",
                        "רובה נגד דקירה ישרה נמוכה",
                        "רובה נגד דקירה רגילה",
                        "רובה נגד דקירה מזרחית מימין",
                        "רובה נגד דקירה מזרחית משמאל",
                        "רובה נגד דקירה מזרחית מלמטה"
                    )
                )
            )
        )
    )


    // ----------------------------
    // הגנות מאיום אקדח – לפי חגורה
    // ----------------------------
    val defensesGunThreat: List<Section> = listOf(
        Section(
            id = "gun_threat_defense_main",
            title = "הגנות מאיום אקדח",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.BROWN,
                    items = listOf(
                        "הגנה מאיום אקדח מלפנים",
                        "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד ימין",
                        "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד שמאל",
                        "הגנה מאיום אקדח מהצד החיצוני",
                        "הגנה מאיום אקדח מאחור"
                    )
                ),

                BeltGroup(
                    belt = Belt.BLACK,
                    items = listOf(
                        "הגנה נגד איום אקדח לראש מלפנים",
                        "הגנה נגד איום אקדח צמוד לראש מלפנים",
                        "הגנה נגד איום אקדח מלפנים – קנה קצר",
                        "הגנה נגד איום אקדח לראש – צד ימין",
                        "הגנה נגד איום אקדח לראש – צד שמאל",
                        "הגנה נגד איום אקדח מאחור בדחיפה",
                        "הגנה נגד איום אקדח לראש מהצד מאחור – צד שמאל",
                        "הגנה מאיום אקדח בהובלה",
                        "הגנה נגד איום אקדח לראש מאחור",
                        "הגנה נגד איום אקדח מאחור בידיים מורמות"
                    )
                )
            )
        )
    )

    // ----------------------------
    // הגנות נגד  מספר תוקפים – חגורה שחורה (בקטלוג הקשיח)
    // ----------------------------
    val defensesMultipleAttackers: List<Section> = listOf(

        Section(
            id = "multiple_attackers_main",
            title = "הגנות נגד 2 תוקפים",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.BLACK,
                    items = listOf(
                        "1 מקל 1 סכין - מקל בצד חי",
                        "1 מקל 1 סכין - מקל בצד מת",
                        "1 מקל 1 סכין - במקרה והסכין קרוב"
                    )
                )
            )
        )
    )


    // ----------------------------
    // הגנות נגד מקל – לפי חגורה (בקטלוג הקשיח)
    // ----------------------------
    val defensesStick: List<Section> = listOf(

        Section(
            id = "stick_defense_main",
            title = "הגנות נגד מקל",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "הגנה נגד מקל - צד חי",
                        "הגנה נגד מקל - צד מת"
                    )
                ),

                BeltGroup(
                    belt = Belt.BROWN,
                    items = listOf(
                "הגנה נגד מקל בסיבוב – צד חי",
                "הגנה נגד מקל עם קוואלר – צד מת",
                "הגנה נגד מקל נקודת תורפה – לצד המת"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLACK,
                    items = listOf(
                "הגנה נגד מקל ארוך – התקפה לצד ימין מגן",
                "הגנה נגד מקל ארוך – התקפה לצד שמאל מגן",
                "הגנה נגד מקל ארוך מצד ימין",
                "הגנה נגד מקל ארוך מצד שמאל",
                "הגנה נגד דקירה במקל ארוך – הצד החי",
                "הגנה נגד דקירה במקל ארוך – הצד המת"
                )
            )
         )
    )
)

    // ----------------------------
    // עבודת ידיים – לפי חגורה
    // ----------------------------
    val handsAll: List<Section> = listOf(
        Section(
            id = "hands_strikes",
            title = "מכות יד",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "מכת פיסת יד שמאל לפנים",
                        "מכת פיסת יד ימין לפנים",
                        "מכת פיסת יד שמאל-ימין לפנים",
                        "מכת פיסת יד שמאל-ימין-שמאל לפנים",
                        "מכת פיסת יד מהצד",
                        "אגרוף ימין לפנים",
                        "אגרוף שמאל-ימין לפנים",
                        "אגרוף שמאל בהתקדמות",
                        "אגרוף ימין בהתקדמות",
                        "אגרוף שמאל-ימין בהתקדמות",
                        "אגרוף שמאל-ימין ושמאל בהתקדמות",
                        "אגרוף שמאל בנסיגה",
                        "אגרוף שמאל למטה בהתקפה",
                        "אגרוף ימין למטה בהתקפה",
                        "אגרוף שמאל למטה בהגנה",
                        "אגרוף ימין למטה בהגנה",
                        "מכת מגל שמאל",
                        "מכת מגל ימין",
                        "מכת מגל למטה ולמעלה בהתחלפות",
                        "מכת סנוקרת שמאל",
                        "מכת סנוקרת ימין"
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "מכת גב יד בהצלפה",
                        "מכת גב יד בהצלפה בסיבוב",
                        "מכת פטיש",
                        "מכת פטיש מהצד",
                        "אגרוף עליון",
                        "מכת פטיש בסיבוב",
                        "מכת פטיש לאחור",
                        "מכת פטיש מלמטה למעלה",
                        "פיסת יד פנימית",
                        "פיסת יד חיצונית",
                        "גב יד",
                        "מגל",
                        "סנוקרת"
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = emptyList()
                ),
                BeltGroup(
                    belt = Belt.BLUE,
                    items = emptyList()
                ),
                BeltGroup(
                    belt = Belt.BROWN,
                    items = emptyList()
                ),
                BeltGroup(
                    belt = Belt.BLACK,
                    items = emptyList()
                )
            )
        ),

        Section(
            id = "hands_elbows",
            title = "מכות מרפק",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "מכת מרפק אופקית לאחור",
                        "מכת מרפק אופקית לצד",
                        "מכת מרפק אופקית לפנים",
                        "מכת מרפק לאחור",
                        "מכת מרפק לאחור למעלה",
                        "מכת מרפק אנכי למטה",
                        "מכת מרפק אנכי למעלה"
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = emptyList()
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "מכת מרפק נגד קבוצה"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLUE,
                    items = emptyList()
                ),
                BeltGroup(
                    belt = Belt.BROWN,
                    items = emptyList()
                ),
                BeltGroup(
                    belt = Belt.BLACK,
                    items = emptyList()
                )
            )
        ),

        Section(
            id = "hands_stick_rifle",
            title = "מכות במקל / רובה",
            beltGroups = listOf(

                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "התקפה עם מקל לנקודות תורפה"
                    )
                ),

                BeltGroup(
                    belt = Belt.BLACK,
                    items = listOf(
                        "מכות במקל קצר - מכת מקל לראש",
                        "מכות במקל קצר - מכת מקל לרקה",
                        "מכות במקל קצר - מכת מקל ללסת / צואר",
                        "מכות במקל קצר - מכת מקל לעצם הבריח",
                        "מכות במקל קצר - מכת מקל למרפק",
                        "מכות במקל קצר - מכת מקל לשורש כף היד",
                        "מכות במקל קצר - מכת מקל לפרקי האצבעות",
                        "מכות במקל קצר - מכת מקל לברך",
                        "מכות במקל קצר - מכת מקל למפסעה",
                        "מכות במקל קצר - הצלפת מקל לצלעות",
                        "מכות במקל קצר - דקירת מקל חיצונית לצלעות",
                        "מכות במקל קצר - דקירת מקל ישרה לבטן / לגרון",
                        "מכות במקל קצר - דקירת מקל הפוכה",

                        "מכות במקל / רובה - התקפה עם מקל לנקודות תורפה",
                        "מכות במקל / רובה - מכה אופקית לצוואר",
                        "מכות במקל / רובה - דקירה",
                        "מכות במקל / רובה - מכת מגל",
                        "מכות במקל / רובה - שיסוף",
                        "מכות במקל / רובה - מכה למפסעה",
                        "מכות במקל / רובה - מכת סנוקרת",
                        "מכות במקל / רובה - מכה לצד",
                        "מכות במקל / רובה - מכה לאחור",
                        "מכות במקל / רובה - מכה אופקית לאחור",
                        "מכות במקל / רובה - מכה אופקית ובעיטה רגילה למפסעה",
                        "מכות במקל / רובה - מכה אופקית ובעיטת הגנה לפנים",
                        "מכות במקל / רובה - מכה לצד ובעיטה לצד"
                    )
                )
            )
        )
    )

    // ----------------------------
    // הגנות פנימיות / חיצוניות – לפי חגורה
    // ----------------------------
    val defensesInternalPunch: List<Section> = listOf(
        Section(
            id = "def_internal_punch",
            title = "הגנות פנימיות - אגרופים",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "הגנה פנימית רפלקסיבית",
                        "הגנה פנימית נגד ימין בכף יד שמאל",
                        "הגנה פנימית נגד שמאל בכף יד ימין",
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "הגנה פנימית נגד שמאל עם המרפק",
                        "הגנה פנימית נגד מכות ישרות למטה",
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "הגנה פנימית נגד ימין באמת שמאל",
                        "הגנה פנימית נגד שמאל באמת שמאל",
                        "הגנה פנימית נגד ימין - אגרוף שמאל בהחלקה"
                    )
                ),
                BeltGroup(
                    belt = Belt.BLACK,
                    items = listOf(
                        "הגנה פנימית נגד אגרוף שמאל – בעיטת הגנה",
                        "הגנה פנימית נגד אגרוף שמאל – בעיטה לצד",
                        "הגנה פנימית נגד אגרוף שמאל – בעיטה רגילה לאחור",
                        "הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לאחור",
                        "הגנה פנימית נגד אגרוף שמאל – בעיטת סטירה חיצונית",
                        "הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לפנים",
                        "הגנה פנימית נגד אגרוף שמאל – גזיזה קדמית",
                    )
                )
            )
        )
    )

    val defensesInternalKick: List<Section> = listOf(
        Section(
            id = "def_internal_kick",
            title = "הגנות פנימיות - בעיטות",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "הגנה פנימית נגד בעיטה רגילה למפסעה",
                    )
                ),
                BeltGroup(
                    belt = Belt.BLUE,
                    items = listOf(
                        "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לצד",
                        "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לאחור",
                        "הגנה פנימית באמת ימין נגד בעיטה לצד"
                    )
                ),
                BeltGroup(
                    belt = Belt.BROWN,
                    items = listOf(
                        "הגנה פנימית נגד בעיטה לסנטר",
                        "הגנה פנימית נגד בעיטה רגילה – טאטוא",
                    )
                )
            )
        )
    )

    val defensesExternalPunch: List<Section> = listOf(
        Section(
            id = "def_external_punch",
            title = "הגנות חיצוניות - אגרופים",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "הגנה חיצונית רפלקסיבית 360 מעלות",
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "הגנה חיצונית מס' 1",
                        "הגנה חיצונית מס' 2",
                        "הגנה חיצונית מס' 3",
                        "הגנה חיצונית מס' 4",
                        "הגנה חיצונית מס' 5",
                        "הגנה חיצונית מס' 6",
                        "הגנה חיצונית נגד אגרופים למטה",
                        "הגנה נגד מכה גבוהה מהצד - התוקף בצד שמאל",
                        "הגנה נגד מכה מהצד לעורף - התוקף בצד שמאל",
                        "הגנה נגד מכה מהצד לגב - התוקף בצד שמאל",
                        "הגנה נגד מכה גבוהה מהצד - התוקף בצד ימין",
                        "הגנה נגד מכה מהצד לגרון - התוקף בצד ימין",
                        "הגנה נגד מכה מהצד לבטן - התוקף בצד ימין",
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "הגנה חיצונית נגד ימין באגרוף מהופך",
                        "הגנה חיצונית נגד שמאל",
                        "הגנה חיצונית נגד שמאל בהתקדמות",
                    )
                )
            )
        )
    )

    val defensesExternalKick: List<Section> = listOf(
        Section(
            id = "def_external_kick",
            title = "הגנות חיצוניות - בעיטות",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "הגנה חיצונית נגד בעיטה רגילה",
                        "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בימין",
                        "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בשמאל",
                        "הגנה חיצונית נגד בעיטת מגל לפנים - אגרוף בימין",
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "הגנה חיצונית באמת שמאל נגד בעיטה רגילה",
                        "הגנה חיצונית באמת ימין נגד בעיטה לצד",
                        "הגנה חיצונית באמת שמאל נגד בעיטה לצד",
                    )
                ),
                BeltGroup(
                    belt = Belt.BROWN,
                    items = listOf(
                        "הגנה חיצונית נגד בעיטה רגילה – פריצה",
                        "הגנה חיצונית נגד בעיטה רגילה – גזיזה",
                        "הגנה חיצונית נגד בעיטה רגילה – טאטוא",
                        "הגנה חיצונית נגד מגל לפנים – גזיזה",
                        "הגנה חיצונית נגד מגל לפנים – טאטוא",
                    )
                )
            )
        )
    )

    val defensesRoot: List<Section> = listOf(
        Section(
            id = "def_internal_punch",
            title = "הגנות פנימיות - אגרופים",
            beltGroups = defensesInternalPunch.firstOrNull()?.beltGroups.orEmpty()
        ),
        Section(
            id = "def_internal_kick",
            title = "הגנות פנימיות - בעיטות",
            beltGroups = defensesInternalKick.firstOrNull()?.beltGroups.orEmpty()
        ),
        Section(
            id = "def_external_punch",
            title = "הגנות חיצוניות - אגרופים",
            beltGroups = defensesExternalPunch.firstOrNull()?.beltGroups.orEmpty()
        ),
        Section(
            id = "def_external_kick",
            title = "הגנות חיצוניות - בעיטות",
            beltGroups = defensesExternalKick.firstOrNull()?.beltGroups.orEmpty()
        ),
        Section(
            id = "kicks_hard",
            title = "הגנות נגד בעיטות",
            subSections = defensesKicks
        ),
        Section(
            id = "knife_defense",
            title = "הגנות מסכין",
            beltGroups = defensesKnife
                .firstOrNull { it.id == "knife_defense_main" }
                ?.beltGroups
                .orEmpty()
        ),
        Section(
            id = "knife_rifle_defense",
            title = "הגנות עם רובה נגד דקירות סכין",
            beltGroups = defensesKnife
                .firstOrNull { it.id == "knife_defense_rifle_against_knife_stabs" }
                ?.beltGroups
                .orEmpty()
        ),
        Section(
            id = "gun_threat_defense",
            title = "הגנות מאיום אקדח",
            beltGroups = defensesGunThreat.firstOrNull()?.beltGroups.orEmpty()
        ),
        Section(
            id = "multiple_attackers_defense",
            title = "הגנות נגד מספר תוקפים",
            beltGroups = defensesMultipleAttackers.firstOrNull()?.beltGroups.orEmpty()
        ),
        Section(
            id = "stick_defense",
            title = "הגנות נגד מקל",
            beltGroups = defensesStick.firstOrNull()?.beltGroups.orEmpty()
        )
    )

    // ----------------------------
    // שחרורים – תתי-נושאים קשיחים (לפי חגורה)
    // ----------------------------
    val releases: List<Section> = listOf(

        Section(
            id = "releases_hands_hair_shirt",
            title = "שחרור מתפיסות ידיים / שיער / חולצה",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "שחרור מתפיסת יד מול יד",
                        "שחרור מתפיסת יד נגדית",
                        "שחרור מתפיסת שתי ידיים למטה",
                        "שחרור מתפיסת שתי ידיים למעלה"
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "שחרור חולצה - בריח על האגודל",
                        "שחרור חולצה - מכת פרקי אצבעות",
                        "שחרור חולצה - שתי ידיים",
                        "שחרור מתפיסת שיער מלפנים",
                        "שחרור מתפיסת שיער מלפנים בשתי ידיים",
                        "שחרור מתפיסת יד מול יד - בריח על האגודל",
                        "שחרור מתפיסת יד נגדית - פרקי אצבעות",
                        "שחרור מתפיסת יד בשתי ידיים למעלה",
                        "שחרור מתפיסת יד בשתי ידיים למטה - מרווח",
                        "שחרור מתפיסת יד בשתי ידיים למטה - צמוד",
                        "שחרור מתפיסת ידיים צמודה מאחור",
                        "שחרור מתפיסת זרוע מהצד במשיכה",
                        "שחרור מתפיסת זרוע מהצד בדחיפה"
                    )
                ),
                BeltGroup(
                    belt = Belt.GREEN,
                    items = listOf(
                        "שחרור מתפיסת שיער מאחור - צד חי",
                        "שחרור מתפיסת שיער מאחור - צד מת",
                        "חביקת יד מהצד - ראש התוקף מאחור",
                        "חביקת יד מהצד - ראש התוקף מלפנים",
                        "שחרור מתפיסת ידיים מאחור",
                        "שחרור מתפיסת חולצה מאחור",
                        "שחרור מתפיסת שיער מהצד - צד ימין",
                        "שחרור מתפיסת שיער מהצד - צד שמאל"
                    )
                ),
                BeltGroup(belt = Belt.BLUE, items = emptyList()),
                BeltGroup(belt = Belt.BROWN, items = emptyList()),
                BeltGroup(belt = Belt.BLACK, items = emptyList())
            )
        ),

        Section(
            id = "releases_chokes",
            title = "שחרור מחניקות",
            beltGroups = listOf(
                BeltGroup(
                    belt = Belt.YELLOW,
                    items = listOf(
                        "מניעת התקרבות תוקף",
                        "מניעת חניקה",
                        "שחרור מחניקה מלפנים בכף היד",
                        "שחרור מחניקה מאחור במשיכה"
                    )
                ),
                BeltGroup(
                    belt = Belt.ORANGE,
                    items = listOf(
                        "שחרור מחניקה מלפנים בדחיפה",
                        "שחרור מחניקה מאחור בדחיפה",
                        "שחרור מחניקה מהצד - מרחוק",
                        "שחרור מחניקה מהצד - מקרוב",
                        "שחרור מחנקה מהצד בשכיבה"
                    )
                ),
                BeltGroup(belt = Belt.GREEN, items = emptyList()),
                BeltGroup(
                    belt = Belt.BLUE,
                    items = listOf(
                        "שחרור מחניקה לקיר — מלפנים לא צמודה",
                        "שחרור מחניקה לקיר — צמודה מלפנים",
                        "שחרור מחניקה לקיר — דחיפה מאחור",
                        "שחרור מחניקה לקיר — צמודה מאחור",
                        "שחרור מחניקה בשכיבה — ידיים כפופות",
                        "שחרור מחניקה בשכיבה — ידיים ישרות",
                        "שחרור מחניקה צמודה בשכיבה",
                        "שחרור מחניקה מהצד בשכיבה"
                    )
                )
            )
        ),

        Section(
            id = "releases_hugs",
            title = "שחרור מחביקות",
            subSections = listOf(

                Section(
                    id = "releases_hugs_body",
                    title = "חביקות גוף",
                    beltGroups = listOf(
                        BeltGroup(
                            belt = Belt.ORANGE,
                            items = listOf(
                                "שחרור מחביקה פתוחה מלפנים",
                                "שחרור מחביקה פתוחה מאחור",
                                "שחרור מחביקה סגורה מלפנים",
                                "שחרור מחביקה סגורה מאחור"
                            )
                        ),
                        BeltGroup(
                            belt = Belt.GREEN,
                            items = listOf(
                                "שחרור מחביקה פתוחה מהצד",
                                "שחרור מחביקה פתוחה מלפנים בהרמה",
                                "שחרור מחביקה סגורה מהצד",
                                "שחרור מחביקה סגורה מלפנים בהרמה",
                                "שחרור מחביקה פתוחה מאחור בהרמה",
                                "שחרור מחביקה פתוחה מאחור עם תפיסת אצבע",
                                "שחרור מחביקה פתוחה מאחור - בריח על האצבעות"
                            )
                        ),
                        BeltGroup(belt = Belt.BLUE, items = emptyList()),
                        BeltGroup(belt = Belt.BROWN, items = emptyList()),
                        BeltGroup(
                            belt = Belt.BLACK,
                            items = listOf(
                                "שחרור מחביקה סגורה מהצד",
                                "שחרור מחביקה סגורה מהצד - היד הרחוקה משוחררת",
                                "שחרור מחביקה פתוחה מהצד",
                                "שחרור מחביקה פתוחה מאחור - הטלה",
                                "שחרור מחביקה סגורה מאחור - הטלה"
                            )
                        )
                    )
                ),

                Section(
                    id = "releases_hugs_neck",
                    title = "חביקות צוואר",
                    beltGroups = listOf(
                        BeltGroup(
                            belt = Belt.YELLOW,
                            items = listOf(
                                "שחרור מחביקת צואר מהצד"
                            )
                        ),
                        BeltGroup(
                            belt = Belt.ORANGE,
                            items = listOf(
                                "שחרור חביקת צואר מלפנים",
                                "שחרור מחביקת צואר מהצד בשכיבה",
                                "שחרור מחביקת צואר ויד מהצד בשכיבה"
                            )
                        ),
                        BeltGroup(
                            belt = Belt.GREEN,
                            items = listOf(
                                "שחרור חביקת צואר מאחור"
                            )
                        ),
                        BeltGroup(
                            belt = Belt.BLUE,
                            items = listOf(
                                "שחרור מחביקת צוואר מהצד והפלה",
                                "שחרור מחביקת צוואר מאחור עם נעילה",
                                "שחרור מחביקת צואר בשכיבה ברכיבה צמודה"
                            )
                        ),
                        BeltGroup(
                            belt = Belt.BROWN,
                            items = listOf(
                                "חביקת צוואר מאחור – בריח על העורף, המגן כפוף לפנים"
                            )
                        ),
                        BeltGroup(
                            belt = Belt.BLACK,
                            items = listOf(
                                "שחרור מחביקת צואר מהצד - משיכה לאחור",
                                "שחרור מחביקת צואר מהצד - יד תפוסה",
                                "שחרור מחביקת צואר מהצד - זריקת רגל",
                                "שחרור מחביקת צואר מהצד - מהברך"
                            )
                        )
                    )
                ),

                Section(
                    id = "releases_hugs_arm",
                    title = "חביקות זרוע",
                    beltGroups = listOf(
                        BeltGroup(
                            belt = Belt.ORANGE,
                            items = emptyList()
                        ),
                        BeltGroup(
                            belt = Belt.GREEN,
                            items = listOf(
                                "חביקות יד מצד - ראש התוקף מאחור",
                                "חביקות יד מצד - ראש התוקף מלפנים"
                            )
                        ),
                        BeltGroup(
                            belt = Belt.BLUE,
                            items = emptyList()
                        ),
                        BeltGroup(
                            belt = Belt.BROWN,
                            items = emptyList()
                        ),
                        BeltGroup(
                            belt = Belt.BLACK,
                            items = emptyList()
                        )
                    )
                )
            )
        )
    )

        // ----------------------------
    // Defense helpers (source of truth)
    // ----------------------------

    private fun normDefenseKey(s: String): String = s
        .trim()
        .lowercase()
        .replace("%3a", ":")
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")

    private fun normItemKey(s: String): String =
        s.trim()
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun canonicalDefenseKind(raw: String): String {
        val t = normDefenseKey(raw)
        return when {
            t == "all" -> "all"
            t == "internal" || t.contains("פנימ") -> "internal"
            t == "external" || t.contains("חיצונ") -> "external"
            t.startsWith("def_internal") -> "internal"
            t.startsWith("def_external") -> "external"
            t.startsWith("def_all") -> "all"
            t == "kicks_hard" -> "kicks_hard"
            t == "releases_hard" -> "releases_hard"
            t == "knife_hard" -> "knife_hard"
            t == "knife_rifle_hard" -> "knife_rifle_hard"
            t == "gun_hard" -> "gun_hard"
            t == "multiple_attackers_hard" -> "multiple_attackers_hard"
            t == "stick_hard" -> "stick_hard"
            else -> t
        }
    }

    fun canonicalDefensePick(raw: String): String {
        val t = normDefenseKey(raw)

        if (t in setOf("straight_groin", "hook_back", "knee", "side_kick")) return t
        if (t in setOf("hands_hair_shirt", "chokes", "hugs")) return t

        return when (t) {
            "punches" -> "punch"
            "kicks" -> "kick"
            else -> t
        }
    }

    fun defenseItemsFor(kindRaw: String, pickRaw: String): List<Pair<Belt, List<String>>> {
        val kind = canonicalDefenseKind(kindRaw)
        val pick = canonicalDefensePick(pickRaw)

        fun fromSection(section: Section?): List<Pair<Belt, List<String>>> {
            if (section == null) return emptyList()

            val byBelt = section.beltGroups.associate { bg ->
                bg.belt to bg.items
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .toList()
            }

            return beltOrder.mapNotNull { belt ->
                val items = byBelt[belt].orEmpty()
                if (items.isNotEmpty()) belt to items else null
            }
        }

        if (kind == "kicks_hard") {
            val sectionTitle = when (pick) {
                "straight_groin" -> "הגנות נגד בעיטות ישרות / למפשעה"
                "hook_back" -> "הגנות נגד מגל / מגל לאחור"
                "knee" -> "הגנות נגד ברך"
                "side_kick" -> "הגנות נגד בעיטה לצד"
                else -> ""
            }

            return fromSection(
                defensesKicks.firstOrNull { it.title.trim() == sectionTitle.trim() }
            )
        }

        if (kind == "releases_hard") {
            val sectionTitle = when (pick) {
                "hands_hair_shirt" -> "שחרור מתפיסות ידיים / שיער / חולצה"
                "chokes" -> "שחרור מחניקות"
                "hugs" -> "שחרור מחביקות"
                else -> ""
            }

            return fromSection(
                releases.firstOrNull { it.title.trim() == sectionTitle.trim() }
            )
        }

        if (kind == "internal" && pick == "punch") {
            return fromSection(defensesInternalPunch.firstOrNull())
        }

        if (kind == "internal" && pick == "kick") {
            return fromSection(defensesInternalKick.firstOrNull())
        }

        if (kind == "external" && pick == "punch") {
            return fromSection(defensesExternalPunch.firstOrNull())
        }

        if (kind == "external" && pick == "kick") {
            return fromSection(defensesExternalKick.firstOrNull())
        }

        if (kind == "knife_hard") {
            return fromSection(
                defensesKnife.firstOrNull { it.id == "knife_defense_main" }
            )
        }

        if (kind == "knife_rifle_hard") {
            return fromSection(
                defensesKnife.firstOrNull { it.id == "knife_defense_rifle_against_knife_stabs" }
            )
        }

        if (kind == "gun_hard") return fromSection(defensesGunThreat.firstOrNull())
        if (kind == "multiple_attackers_hard") return fromSection(defensesMultipleAttackers.firstOrNull())
        if (kind == "stick_hard") return fromSection(defensesStick.firstOrNull())

        return emptyList()
    }

    fun defenseScreenTitle(kindRaw: String, pickRaw: String): String {
        val kind = canonicalDefenseKind(kindRaw)
        val pick = canonicalDefensePick(pickRaw)

        return when (kind to pick) {
            "internal" to "punch" -> "הגנות פנימיות - אגרופים"
            "internal" to "kick" -> "הגנות פנימיות - בעיטות"
            "external" to "punch" -> "הגנות חיצוניות - אגרופים"
            "external" to "kick" -> "הגנות חיצוניות - בעיטות"
            "all" to "kick" -> "הגנות נגד בעיטות"
            "all" to "punch" -> "הגנות נגד אגרופים"
            "releases_hard" to "hands_hair_shirt" -> "שחרור מתפיסות ידיים / שיער / חולצה"
            "releases_hard" to "chokes" -> "שחרור מחניקות"
            "releases_hard" to "hugs" -> "שחרור מחביקות גוף"
            "kicks_hard" to "straight_groin" -> "הגנות נגד בעיטות ישרות / למפשעה"
            "kicks_hard" to "hook_back" -> "הגנות נגד מגל / מגל לאחור"
            "kicks_hard" to "knee" -> "הגנות נגד ברך"
            "kicks_hard" to "side_kick" -> "הגנות נגד בעיטה לצד"
            "knife_hard" to "all" -> "הגנות מסכין"
            "knife_rifle_hard" to "all" -> "הגנות עם רובה נגד דקירות סכין"
            "gun_hard" to "all" -> "הגנות מאיום אקדח"
            "multiple_attackers_hard" to "all" -> "הגנות נגד מספר תוקפים"
            "stick_hard" to "all" -> "הגנות נגד מקל"
            else -> "הגנות"
        }
    }

    fun stripDefenseItemPrefix(
        kindRaw: String,
        pickRaw: String,
        full: String
    ): String {
        val kind = canonicalDefenseKind(kindRaw)
        val pick = canonicalDefensePick(pickRaw)
        val t = full.trim()

        if (kind == "releases_hard") {
            val prefixes = listOf(
                "שחרור מתפיסות ידיים / שיער / חולצה - ",
                "שחרור מתפיסות ידיים / שיער / חולצה – ",
                "שחרור מתפיסות ידיים / שיער / חולצה: ",
                "שחרור מחניקות - ",
                "שחרור מחניקות – ",
                "שחרור מחניקות: ",
                "שחרור מחביקות - ",
                "שחרור מחביקות – ",
                "שחרור מחביקות: ",
                "שחרור מחביקות גוף - ",
                "שחרור מחביקות גוף – ",
                "שחרור מחביקות גוף: "
            )

            val hit = prefixes.firstOrNull { t.startsWith(it) }
            return if (hit != null) t.removePrefix(hit).trim() else t
        }

        if (kind == "kicks_hard") {
            val prefixes = listOf(
                "הגנות נגד בעיטות ישרות / למפשעה - ",
                "הגנות נגד בעיטות ישרות / למפשעה – ",
                "הגנות נגד בעיטות ישרות / למפשעה: ",
                "הגנות נגד מגל / מגל לאחור - ",
                "הגנות נגד מגל / מגל לאחור – ",
                "הגנות נגד מגל / מגל לאחור: ",
                "הגנות נגד ברך - ",
                "הגנות נגד ברך – ",
                "הגנות נגד ברך: ",
                "הגנות נגד בעיטה לצד - ",
                "הגנות נגד בעיטה לצד – ",
                "הגנות נגד בעיטה לצד: "
            )

            val hit = prefixes.firstOrNull { t.startsWith(it) }
            return if (hit != null) t.removePrefix(hit).trim() else t
        }

        return t
    }

    fun defenseCount(kindRaw: String, pickRaw: String): Int =
        defenseItemsFor(kindRaw, pickRaw)
            .asSequence()
            .flatMap { it.second.asSequence() }
            .map { normItemKey(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .count()

    fun kicksHardSubCounts(): Map<String, Int> = linkedMapOf(
        "הגנות נגד בעיטות ישרות / למפשעה" to defenseCount("kicks_hard", "straight_groin"),
        "הגנות נגד מגל / מגל לאחור" to defenseCount("kicks_hard", "hook_back"),
        "הגנות נגד ברך" to defenseCount("kicks_hard", "knee"),
        "הגנות נגד בעיטה לצד" to defenseCount("kicks_hard", "side_kick"),
    )

    fun defenseDialogCounts(): Map<String, Int> = linkedMapOf(
        "הגנות פנימיות" to (
                defenseCount("internal", "punch") +
                        defenseCount("internal", "kick")
                ),
        "הגנות חיצוניות" to (
                defenseCount("external", "punch") +
                        defenseCount("external", "kick")
                ),
        "הגנות נגד בעיטות" to (
                defenseCount("kicks_hard", "straight_groin") +
                        defenseCount("kicks_hard", "hook_back") +
                        defenseCount("kicks_hard", "knee") +
                        defenseCount("kicks_hard", "side_kick")
                ),
        "הגנות מסכין" to defenseCount("knife_hard", "all"),
        "הגנות עם רובה נגד דקירות סכין" to defenseCount("knife_rifle_hard", "all"),
        "הגנות מאיום אקדח" to defenseCount("gun_hard", "all"),
        "הגנות נגד מספר תוקפים" to defenseCount("multiple_attackers_hard", "all"),
        "הגנות נגד מקל" to defenseCount("stick_hard", "all"),
    )

    fun totalDefenseCount(): Int =
        defenseDialogCounts().values.sum()

    // ----------------------------
    // סדר תצוגה מומלץ (אם תרצה להשתמש בזה במקום listOf בכל מקום)
    // ----------------------------
    val beltOrder: List<Belt> = listOf(
        Belt.YELLOW,
        Belt.ORANGE,
        Belt.GREEN,
        Belt.BLUE,
        Belt.BROWN,
        Belt.BLACK
    )
}