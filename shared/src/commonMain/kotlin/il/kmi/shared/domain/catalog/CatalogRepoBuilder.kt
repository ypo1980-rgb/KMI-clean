package il.kmi.shared.domain.catalog

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.HardSectionsCatalog

object CatalogRepoBuilder {

    fun buildTopicsForBelt(belt: Belt): List<CatalogTopic> {
        return when (belt) {
            Belt.YELLOW -> yellowTopics()
            Belt.ORANGE -> orangeTopics()
            Belt.GREEN -> greenTopics()
            Belt.BLUE -> blueTopics()
            Belt.BROWN -> brownTopics()
            Belt.BLACK -> blackTopics()
            Belt.WHITE -> emptyList()
        }
    }

    private fun yellowTopics(): List<CatalogTopic> = listOf(
        CatalogTopic(
            title = "כללי",
            items = listOf(
                "בלימת רכה לפנים",
                "בלימה לאחור",
                "תזוזות",
                "גלגול לפנים – צד ימין",
                "הוצאות אגן, הרמת אגן והפניית גוף למעלה ",
                "צל בוקס",
                "סגירת אגרוף",
                "אצבעות לפנים",
                "מכת קשת האצבע והאגודל"
            )
        ),
        CatalogTopic(
            title = "עמידת מוצא",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_ready_stance",
                belt = Belt.YELLOW
            )
        ),
        CatalogTopic(
            title = "מכות ידיים",
            items = emptyList(),
            subTopics = HardSectionsCatalog
                .subjectSubSectionsFor("topic_hands")
                .map { section ->
                    CatalogSubTopic(
                        title = section.title,
                        items = HardSectionsCatalog.subjectSubSectionItemsFor(
                            subjectId = "topic_hands",
                            subSectionId = section.id,
                            belt = Belt.YELLOW
                        )
                    )
                }
        ),
        CatalogTopic(
            title = "הכנה לעבודת קרקע",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_ground_prep",
                belt = Belt.YELLOW
            )
        ),
        CatalogTopic(
            title = "בעיטות",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_kicks",
                belt = Belt.YELLOW
            )
        )
    )

    private fun orangeTopics(): List<CatalogTopic> = listOf(
        CatalogTopic(
            title = "כללי",
            items = listOf(
                "גלגול לאחור צד ימין",
                "גלגול לאחור צד שמאל",
                "גלגול לפנים צד שמאל",
                "שילובי ידיים רגליים",
                "בלימה לצד ימין",
                "בלימה לצד שמאל"
            )
        ),
        CatalogTopic(
            title = "מכות יד",
            items = listOf(
                "מכת גב יד בהצלפה",
                "מכת גב יד בהצלפה בסיבוב",
                "מכת פטיש",
                "מכת פטיש מהצד"
            )
        ),
        CatalogTopic(
            title = "בעיטות",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_kicks",
                belt = Belt.ORANGE
            )
        )
    )

    private fun greenTopics(): List<CatalogTopic> = listOf(
        CatalogTopic(
            title = "בלימות וגלגולים",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_breakfalls_rolls",
                belt = Belt.GREEN
            )
        ),
        CatalogTopic(
            title = "קאוולר",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_kawalr",
                belt = Belt.GREEN
            )
        ),
        CatalogTopic(
            title = "מכות מרפק",
            items = listOf("מכת מרפק נגד קבוצה")
        ),
        CatalogTopic(
            title = "מכות במקל / רובה",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_stick_rifle_strikes",
                belt = Belt.GREEN
            )
        ),
        CatalogTopic(
            title = "בעיטות",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_kicks",
                belt = Belt.GREEN
            )
        )
    )

    private fun blueTopics(): List<CatalogTopic> = listOf(
        CatalogTopic(
            title = "בלימות וגלגולים",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_breakfalls_rolls",
                belt = Belt.BLUE
            )
        ),
        CatalogTopic(
            title = "בעיטות",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_kicks",
                belt = Belt.BLUE
            )
        )
    )

    private fun brownTopics(): List<CatalogTopic> = listOf(
        CatalogTopic(
            title = "בלימות וגלגולים",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_breakfalls_rolls",
                belt = Belt.BROWN
            )
        ),
        CatalogTopic(
            title = "בעיטות",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_kicks",
                belt = Belt.BROWN
            )
        )
    )

    private fun blackTopics(): List<CatalogTopic> = listOf(
        CatalogTopic(
            title = "בעיטות",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_kicks",
                belt = Belt.BLACK
            )
        ),
        CatalogTopic(
            title = "מכות במקל קצר",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_short_stick_strikes",
                belt = Belt.BLACK
            )
        ),
        CatalogTopic(
            title = "מכות במקל / רובה",
            items = HardSectionsCatalog.subjectItemsFor(
                subjectId = "topic_stick_rifle_strikes",
                belt = Belt.BLACK
            )
        )
    )
}