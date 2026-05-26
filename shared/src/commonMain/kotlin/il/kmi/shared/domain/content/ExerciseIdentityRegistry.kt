package il.kmi.shared.domain.content

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo

/**
 * מקור מרכזי לזהות חד־ערכית של תרגילים.
 *
 * המטרה:
 * - לא לשמור סימונים לפי שם תרגיל בלבד.
 * - לא להיות תלויים בעברית/אנגלית.
 * - לאפשר ל-ContentRepo ול-HardSectionsCatalog לקבל אותו ID לאותו תרגיל.
 *
 * שלב ראשון:
 * - אם קיים ID ידני ב-knownExercises נחזיר אותו.
 * - אם עדיין לא מיפינו ידנית, נחזיר fallback יציב לפי belt + topicKey + title.
 *
 * שלב שני:
 * - נמלא את knownExercises עם ex_001 עד ex_391.
 */
object ExerciseIdentityRegistry {

    data class ExerciseIdentity(
        val id: String,
        val belt: Belt,
        val hebrewTitle: String,
        val topicKeys: Set<String> = emptySet(),
        val aliases: Set<String> = emptySet()
    )

    data class ResolvedExerciseIdentity(
        val id: String,
        val isKnown: Boolean,
        val belt: Belt,
        val hebrewTitle: String,
        val topicKey: String? = null
    )

    /**
     * כאן נכניס בהמשך את כל 391 התרגילים:
     *
     * ExerciseIdentity(
     *     id = "ex_001",
     *     belt = Belt.YELLOW,
     *     hebrewTitle = "עמידת מוצא רגילה",
     *     topicKeys = setOf("עמידת מוצא", "topic_ready_stance")
     * )
     *
     * חשוב:
     * id לעולם לא משתנה אחרי שנקבע.
     */
    val knownExercises: List<ExerciseIdentity> = listOf(
        // ===== חגורה צהובה — תרגילים ex_001 עד ex_071 =====

        ExerciseIdentity(
            id = "ex_001",
            belt = Belt.YELLOW,
            hebrewTitle = "עמידת מוצא רגילה",
            topicKeys = setOf(
                "עמידת מוצא",
                "topic_ready_stance"
            )
        ),

        ExerciseIdentity(
            id = "ex_002",
            belt = Belt.YELLOW,
            hebrewTitle = "עמידת מוצא להגנות פנימיות",
            topicKeys = setOf(
                "עמידת מוצא",
                "topic_ready_stance"
            )
        ),

        ExerciseIdentity(
            id = "ex_003",
            belt = Belt.YELLOW,
            hebrewTitle = "עמידת מוצא להגנות חיצוניות",
            topicKeys = setOf(
                "עמידת מוצא",
                "topic_ready_stance"
            )
        ),

        ExerciseIdentity(
            id = "ex_004",
            belt = Belt.YELLOW,
            hebrewTitle = "עמידת מוצא כללית מס' 1",
            topicKeys = setOf(
                "עמידת מוצא",
                "topic_ready_stance"
            ),
            aliases = setOf(
                "עמידת מוצא כללית מספר 1"
            )
        ),

        ExerciseIdentity(
            id = "ex_005",
            belt = Belt.YELLOW,
            hebrewTitle = "עמידת מוצא כללית מס' 2",
            topicKeys = setOf(
                "עמידת מוצא",
                "topic_ready_stance"
            ),
            aliases = setOf(
                "עמידת מוצא כללית מספר 2"
            )
        ),

        ExerciseIdentity(
            id = "ex_006",
            belt = Belt.YELLOW,
            hebrewTitle = "עמידת מוצא צידית",
            topicKeys = setOf(
                "עמידת מוצא",
                "topic_ready_stance"
            )
        ),

        // ===== חגורה צהובה — כללי =====

        ExerciseIdentity(
            id = "ex_007",
            belt = Belt.YELLOW,
            hebrewTitle = "בלימה רכה לפנים",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        ExerciseIdentity(
            id = "ex_008",
            belt = Belt.YELLOW,
            hebrewTitle = "בלימה לאחור",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        ExerciseIdentity(
            id = "ex_009",
            belt = Belt.YELLOW,
            hebrewTitle = "תזוזות",
            topicKeys = setOf(
                "כללי",
                "topic_general"
            )
        ),

        ExerciseIdentity(
            id = "ex_010",
            belt = Belt.YELLOW,
            hebrewTitle = "גלגול לפנים - ימין",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            ),
            aliases = setOf(
                "גלגול לפנים – צד ימין",
                "גלגול לפנים צד ימין",
                "גלגול לפנים - צד ימין"
            )
        ),

        ExerciseIdentity(
            id = "ex_011",
            belt = Belt.YELLOW,
            hebrewTitle = "צל בוקס",
            topicKeys = setOf(
                "כללי",
                "topic_general"
            )
        ),

        ExerciseIdentity(
            id = "ex_012",
            belt = Belt.YELLOW,
            hebrewTitle = "הוצאת אגן",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "עבודת קרקע",
                "topic_ground_prep"
            ),
            aliases = setOf(
                "הוצאות אגן"
            )
        ),

        ExerciseIdentity(
            id = "ex_013",
            belt = Belt.YELLOW,
            hebrewTitle = "הרמת אגן והפניית גוף לכיון ההפלה",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "עבודת קרקע",
                "topic_ground_prep"
            ),
            aliases = setOf(
                "הרמת אגן והפניית גוף לכיוון ההפלה"
            )
        ),

        ExerciseIdentity(
            id = "ex_014",
            belt = Belt.YELLOW,
            hebrewTitle = "מוצא לעבודת קרקע",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "עבודת קרקע",
                "topic_ground_prep"
            )
        ),

        // ===== חגורה צהובה — מניעת התקרבות התוקף =====

        ExerciseIdentity(
            id = "ex_015",
            belt = Belt.YELLOW,
            hebrewTitle = "אצבעות לפנים",
            topicKeys = setOf(
                "מניעת התקרבות התוקף",
                "topic_general"
            )
        ),

        ExerciseIdentity(
            id = "ex_016",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת קשת האגודל והאצבע לקנה הנשימה",
            topicKeys = setOf(
                "מניעת התקרבות התוקף",
                "topic_general"
            )
        ),

        ExerciseIdentity(
            id = "ex_017",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת קשת האגודל והאצבע",
            topicKeys = setOf(
                "מניעת התקרבות התוקף",
                "topic_general"
            ),
            aliases = setOf(
                "מכת קשת האצבע והאגודל"
            )
        ),

        // ===== חגורה צהובה — עבודת ידיים / פיסת יד =====

        ExerciseIdentity(
            id = "ex_018",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת פיסת יד שמאל לפנים",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__פיסת יד",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_019",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת פיסת יד ימין לפנים",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__פיסת יד",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_020",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת פיסת יד שמאל-ימין לפנים",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__פיסת יד",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_021",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת פיסת יד שמאל-ימין-שמאל לפנים",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__פיסת יד",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_022",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת פיסת יד מהצד",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__פיסת יד",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        // ===== חגורה צהובה — עבודת ידיים / אגרופים ישרים =====

        ExerciseIdentity(
            id = "ex_023",
            belt = Belt.YELLOW,
            hebrewTitle = "סגירת אגרוף",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "כללי",
                "topic_general",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_024",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף שמאל לפנים",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_025",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף ימין לפנים",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_026",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף שמאל-ימין לפנים",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_027",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף שמאל בהתקדמות",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_028",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף ימין בהתקדמות",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_029",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף שמאל-ימין בהתקדמות",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_030",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף שמאל-ימין ושמאל בהתקדמות",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_031",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף שמאל בנסיגה",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_032",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף שמאל למטה בהתקפה",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_033",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף ימין למטה בהתקפה",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_034",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף שמאל למטה בהגנה",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_035",
            belt = Belt.YELLOW,
            hebrewTitle = "אגרוף ימין למטה בהגנה",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__אגרופים ישרים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        // ===== חגורה צהובה — עבודת ידיים / מגל + סנוקרת =====

        ExerciseIdentity(
            id = "ex_036",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מגל שמאל",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__עבודת ידיים - מגל + סנוקרת",
                "עבודת ידיים - מגל + סנוקרת",
                "מגל + סנוקרת",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_037",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מגל ימין",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__עבודת ידיים - מגל + סנוקרת",
                "עבודת ידיים - מגל + סנוקרת",
                "מגל + סנוקרת",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_038",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מגל למטה ולמעלה בהתחלפות",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__עבודת ידיים - מגל + סנוקרת",
                "עבודת ידיים - מגל + סנוקרת",
                "מגל + סנוקרת",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_039",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת סנוקרת שמאל",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__עבודת ידיים - מגל + סנוקרת",
                "עבודת ידיים - מגל + סנוקרת",
                "מגל + סנוקרת",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_040",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת סנוקרת ימין",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__עבודת ידיים - מגל + סנוקרת",
                "עבודת ידיים - מגל + סנוקרת",
                "מגל + סנוקרת",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        // ===== חגורה צהובה — עבודת ידיים / מרפק =====

        ExerciseIdentity(
            id = "ex_041",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מרפק אופקית לפנים",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__מרפק",
                "מרפק",
                "hands_all",
                "hands_elbows",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_042",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מרפק אנכי למטה",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__מרפק",
                "מרפק",
                "hands_all",
                "hands_elbows",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_043",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מרפק אנכי למעלה",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__מרפק",
                "מרפק",
                "hands_all",
                "hands_elbows",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_044",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מרפק לאחור",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__מרפק",
                "מרפק",
                "hands_all",
                "hands_elbows",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_045",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מרפק לאחור למעלה",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__מרפק",
                "מרפק",
                "hands_all",
                "hands_elbows",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_046",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מרפק אופקית לאחור",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__מרפק",
                "מרפק",
                "hands_all",
                "hands_elbows",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_047",
            belt = Belt.YELLOW,
            hebrewTitle = "מכת מרפק אופקית לצד",
            topicKeys = setOf(
                "עבודת ידיים",
                "עבודת ידיים__מרפק",
                "מרפק",
                "hands_all",
                "hands_elbows",
                "topic_hands"
            )
        ),

        // ===== חגורה צהובה — בעיטות =====

        ExerciseIdentity(
            id = "ex_048",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטה רגילה למפסעה",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_049",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטה רגילה לסנטר",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_050",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטת ברך נמוכה למפסעה",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_051",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטת ברך גבוהה",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_052",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטת ברך מהצד",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_053",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטת מגל אופקית",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_054",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטת מגל אלכסונית",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_055",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטת מגל בהטעיה",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_056",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטת מגל נמוכה",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_057",
            belt = Belt.YELLOW,
            hebrewTitle = "בעיטה לצד מעמידת פיסוק",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        // ===== חגורה צהובה — הגנות =====

        ExerciseIdentity(
            id = "ex_058",
            belt = Belt.YELLOW,
            hebrewTitle = "הגנה חיצונית רפלקסיבית 360 מעלות",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_059",
            belt = Belt.YELLOW,
            hebrewTitle = "הגנה פנימית רפלקסיבית",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_060",
            belt = Belt.YELLOW,
            hebrewTitle = "הגנה פנימית נגד ימין בכף יד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_061",
            belt = Belt.YELLOW,
            hebrewTitle = "הגנה פנימית נגד שמאל בכף יד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_062",
            belt = Belt.YELLOW,
            hebrewTitle = "הגנה פנימית נגד בעיטה רגילה למפסעה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות רגילות",
                "הגנות נגד בעיטות רגילות",
                "הגנות נגד בעיטות",
                "kicks_hard",
                "defenses_root",
                "def_internal_kick"
            )
        ),

        // ===== חגורה צהובה — שחרורים =====

        ExerciseIdentity(
            id = "ex_063",
            belt = Belt.YELLOW,
            hebrewTitle = "שחרור מתפיסת יד מול יד",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_064",
            belt = Belt.YELLOW,
            hebrewTitle = "שחרור מתפיסת יד נגדית",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_065",
            belt = Belt.YELLOW,
            hebrewTitle = "שחרור מתפיסת שתי ידיים למטה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_066",
            belt = Belt.YELLOW,
            hebrewTitle = "שחרור מתפיסת שתי ידיים למעלה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_067",
            belt = Belt.YELLOW,
            hebrewTitle = "שחרור מחביקת צואר מהצד",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות צואר",
                "שחרורים מחביקות צואר",
                "releases",
                "releases_hugs"
            )
        ),

        ExerciseIdentity(
            id = "ex_068",
            belt = Belt.YELLOW,
            hebrewTitle = "מניעת התקרבות תוקף",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_069",
            belt = Belt.YELLOW,
            hebrewTitle = "מניעת חניקה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_070",
            belt = Belt.YELLOW,
            hebrewTitle = "שחרור מחניקה מלפנים בכף היד",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_071",
            belt = Belt.YELLOW,
            hebrewTitle = "שחרור מחניקה מאחור במשיכה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        // ===== חגורה כתומה — תרגילים ex_072 עד ex_149 =====

        // ===== חגורה כתומה — כללי =====

        ExerciseIdentity(
            id = "ex_072",
            belt = Belt.ORANGE,
            hebrewTitle = "בלימה לצד - ימין/שמאל",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            ),
            aliases = setOf(
                "בלימה לצד ימין",
                "בלימה לצד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_073",
            belt = Belt.ORANGE,
            hebrewTitle = "גלגול לפנים - שמאל",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            ),
            aliases = setOf(
                "גלגול לפנים צד שמאל",
                "גלגול לפנים - צד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_074",
            belt = Belt.ORANGE,
            hebrewTitle = "גלגול לאחור - ימין/שמאל",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            ),
            aliases = setOf(
                "גלגול לאחור - ימין",
                "גלגול לאחור - שמאל",
                "גלגול לאחור צד ימין",
                "גלגול לאחור צד שמאל",
                "גלגול לאחור שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_075",
            belt = Belt.ORANGE,
            hebrewTitle = "שילובי ידיים ורגליים",
            topicKeys = setOf(
                "כללי",
                "topic_general",
                "בעיטות",
                "topic_kicks"
            ),
            aliases = setOf(
                "שילובי ידיים רגליים"
            )
        ),

        // ===== חגורה כתומה — עבודת ידיים =====

        ExerciseIdentity(
            id = "ex_076",
            belt = Belt.ORANGE,
            hebrewTitle = "מכת גב יד בהצלפה",
            topicKeys = setOf(
                "עבודת ידיים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_077",
            belt = Belt.ORANGE,
            hebrewTitle = "מכת גב יד בהצלפה בסיבוב",
            topicKeys = setOf(
                "עבודת ידיים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_078",
            belt = Belt.ORANGE,
            hebrewTitle = "מכת פטיש יד שמאל",
            topicKeys = setOf(
                "עבודת ידיים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_079",
            belt = Belt.ORANGE,
            hebrewTitle = "מכת פטיש מהצד",
            topicKeys = setOf(
                "עבודת ידיים",
                "hands_all",
                "hands_strikes",
                "topic_hands"
            )
        ),

        // ===== חגורה כתומה — בעיטות =====

        ExerciseIdentity(
            id = "ex_080",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטה רגילה בעקב לסנטר",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_081",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטת הגנה לפנים",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_082",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטת סנוקרת לאחור",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_083",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטה לצד בשיכול",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_084",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטה רגילה לאחור",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_085",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטה לצד בנסיגה",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_086",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטת הגנה לאחור",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_087",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטת סטירה פנימית",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_088",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטת עצירה בכף הרגל האחורית",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_089",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטת עצירה בכף הרגל הקדמית",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_090",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטה רגילה ובעיטת מגל ברגל השנייה",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_091",
            belt = Belt.ORANGE,
            hebrewTitle = "שילובי בעיטות",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_092",
            belt = Belt.ORANGE,
            hebrewTitle = "ניתור ברגל ימין ובעיטה רגילה ברגל ימין",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        // ===== חגורה כתומה — שחרורים =====

        ExerciseIdentity(
            id = "ex_093",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת יד מול יד - בריח על האגודל",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_094",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת יד נגדית - פרקי אצבעות",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_095",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת יד בשתי ידיים למעלה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_096",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת יד בשתי ידיים למטה - מרווח",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_097",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת יד בשתי ידיים למטה - צמוד",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_098",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת ידיים צמודה מאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_099",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת זרוע מהצד במשיכה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_100",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת זרוע מהצד בדחיפה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות ידיים",
                "שחרורים מתפיסות ידיים",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_101",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור חולצה - בריח על האגודל",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות חולצה",
                "שחרורים מתפיסות חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_102",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור חולצה - מכת פרקי אצבעות",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות חולצה",
                "שחרורים מתפיסות חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_103",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור חולצה - שתי ידיים",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות חולצה",
                "שחרורים מתפיסות חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_104",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת שיער מלפנים",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות שיער",
                "שחרורים מתפיסות שיער",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_105",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מתפיסת שיער מלפנים בשתי ידיים",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות שיער",
                "שחרורים מתפיסות שיער",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_106",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור חביקת צואר מלפנים",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות צואר וגוף",
                "שחרורים מתפיסות צואר וגוף",
                "releases",
                "releases_hugs"
            )
        ),

        ExerciseIdentity(
            id = "ex_107",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מחביקה פתוחה מלפנים",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות צואר וגוף",
                "שחרורים מתפיסות צואר וגוף",
                "releases",
                "releases_hugs"
            )
        ),

        ExerciseIdentity(
            id = "ex_108",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מחביקה פתוחה מאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות צואר וגוף",
                "שחרורים מתפיסות צואר וגוף",
                "releases",
                "releases_hugs"
            )
        ),

        ExerciseIdentity(
            id = "ex_109",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מחביקת צואר מהצד בשכיבה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות צואר וגוף",
                "שחרורים מתפיסות צואר וגוף",
                "releases",
                "releases_hugs"
            )
        ),

        ExerciseIdentity(
            id = "ex_110",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מחביקת צואר ויד מהצד בשכיבה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות צואר וגוף",
                "שחרורים מתפיסות צואר וגוף",
                "releases",
                "releases_hugs"
            )
        ),

        ExerciseIdentity(
            id = "ex_111",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מחניקה מלפנים בדחיפה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_112",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מחניקה מאחור בדחיפה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_113",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מחניקה מהצד - מרחוק",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_114",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מחניקה מהצד - מקרוב",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_115",
            belt = Belt.ORANGE,
            hebrewTitle = "שחרור מחניקה מהצד בשכיבה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        // ===== חגורה כתומה — הגנות =====

        ExerciseIdentity(
            id = "ex_116",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנות נגד מכות עם הטיות גוף",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות גוף",
                "הגנות גוף",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_117",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית מס' 1",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_118",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית מס' 2",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_119",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית מס' 3",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_120",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית מס' 4",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_121",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית מס' 5",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_122",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית מס' 6",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_123",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית נגד אגרופים למטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_124",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד מכה גבוהה מהצד - התוקף בצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות מהצד",
                "הגנות חיצוניות נגד מכות מהצד",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_125",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד מכה מהצד לעורף - התוקף בצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות מהצד",
                "הגנות חיצוניות נגד מכות מהצד",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_126",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד מכה מהצד לגב - התוקף בצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות מהצד",
                "הגנות חיצוניות נגד מכות מהצד",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_127",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד מכה גבוהה מהצד - התוקף בצד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות מהצד",
                "הגנות חיצוניות נגד מכות מהצד",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_128",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד מכה מהצד לגרון - התוקף בצד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות מהצד",
                "הגנות חיצוניות נגד מכות מהצד",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_129",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד מכה מהצד לבטן - התוקף בצד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות מהצד",
                "הגנות חיצוניות נגד מכות מהצד",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_130",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה פנימית נגד שמאל עם המרפק",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_131",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה פנימית נגד מכות ישרות למטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_132",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד שמאל-ימין - אגרוף מהופך",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד אגרוף שמאל-ימין",
                "הגנות נגד אגרוף שמאל-ימין",
                "defenses_root",
                "def_internal_punch",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_133",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד שמאל-ימין - הטייה לאחור",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד אגרוף שמאל-ימין",
                "הגנות נגד אגרוף שמאל-ימין",
                "defenses_root",
                "def_internal_punch",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_134",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד שמאל-ימין (כמו חיצוניות)",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד אגרוף שמאל-ימין",
                "הגנות נגד אגרוף שמאל-ימין",
                "defenses_root",
                "def_internal_punch",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_135",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד בעיטת ברך",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ברך",
                "הגנות נגד בעיטות ברך",
                "הגנות נגד ברך",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_136",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית נגד בעיטה רגילה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות רגילות",
                "הגנות נגד בעיטות רגילות",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "defenses_root",
                "kicks_hard",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_137",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד בעיטה רגילה - עצירה ברגל הקדמית",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות רגילות",
                "הגנות נגד בעיטות רגילות",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_138",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד בעיטה רגילה - עצירה ברגל האחורית",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות רגילות",
                "הגנות נגד בעיטות רגילות",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_139",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד מגל / מגל לאחור",
                "defenses_root",
                "kicks_hard",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_140",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בשמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד מגל / מגל לאחור",
                "defenses_root",
                "kicks_hard",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_141",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה חיצונית נגד בעיטת מגל לפנים - אגרוף בימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד מגל / מגל לאחור",
                "defenses_root",
                "kicks_hard",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_142",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד בעיטת מגל לפנים באמות הידיים",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד מגל / מגל לאחור",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_143",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל האחורית",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד מגל / מגל לאחור",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_144",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל הקדמית",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד בעיטות מגל לפנים",
                "הגנות נגד מגל / מגל לאחור",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_145",
            belt = Belt.ORANGE,
            hebrewTitle = "בעיטת עצירה נגד בעיטה לצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות לצד",
                "הגנות נגד בעיטות לצד",
                "הגנות נגד בעיטה לצד",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_146",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנות יד רפלקסיביות נגד דקירות רגילות",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד סכין",
                "הגנות נגד סכין",
                "הגנות מסכין",
                "defenses_root",
                "knife_defense"
            )
        ),

        ExerciseIdentity(
            id = "ex_147",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנות יד רפלקסיביות נגד דקירות מזרחיות",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד סכין",
                "הגנות נגד סכין",
                "הגנות מסכין",
                "defenses_root",
                "knife_defense"
            )
        ),

        ExerciseIdentity(
            id = "ex_148",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנות יד רפלקסיביות נגד דקירה ישרה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד סכין",
                "הגנות נגד סכין",
                "הגנות מסכין",
                "defenses_root",
                "knife_defense"
            )
        ),

        ExerciseIdentity(
            id = "ex_149",
            belt = Belt.ORANGE,
            hebrewTitle = "הגנה נגד אגרופים בשכיבה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__עבודת קרקע",
                "עבודת קרקע",
                "topic_ground_prep",
                "defenses_root"
            )
        ),

        // ===== חגורה ירוקה — תרגילים ex_150 עד ex_221 =====

        // ===== חגורה ירוקה — בלימות וגלגולים =====

        ExerciseIdentity(
            id = "ex_150",
            belt = Belt.GREEN,
            hebrewTitle = "בלימה לאחור מגובה",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        ExerciseIdentity(
            id = "ex_151",
            belt = Belt.GREEN,
            hebrewTitle = "בלימה לצד כהכנה לגזיזות",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        ExerciseIdentity(
            id = "ex_152",
            belt = Belt.GREEN,
            hebrewTitle = "גלגול לפנים ובלימה לאחור - ימין",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            ),
            aliases = setOf(
                "גלגול לפנים ובלימה לאחור - ימין/שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_153",
            belt = Belt.GREEN,
            hebrewTitle = "גלגול לפנים ובלימה לאחור - שמאל",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        ExerciseIdentity(
            id = "ex_154",
            belt = Belt.GREEN,
            hebrewTitle = "גלגול לפנים ולאחור - ימין",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            ),
            aliases = setOf(
                "גלגול לפנים ולאחור - ימין/שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_155",
            belt = Belt.GREEN,
            hebrewTitle = "גלגול לפנים ולאחור - שמאל",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        ExerciseIdentity(
            id = "ex_156",
            belt = Belt.GREEN,
            hebrewTitle = "גלגול ביד אחת - ימין",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            ),
            aliases = setOf(
                "גלגול ביד אחת - ימין/שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_157",
            belt = Belt.GREEN,
            hebrewTitle = "גלגול ביד אחת - שמאל",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        ExerciseIdentity(
            id = "ex_158",
            belt = Belt.GREEN,
            hebrewTitle = "גלגול לפנים קימה לפנים",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            ),
            aliases = setOf(
                "גלגול לפנים - קימה לפנים"
            )
        ),

        // ===== חגורה ירוקה — קוואלר =====

        ExerciseIdentity(
            id = "ex_159",
            belt = Belt.GREEN,
            hebrewTitle = "קוואלר - הליכה לאחור",
            topicKeys = setOf(
                "קוואלר",
                "קאוולר",
                "topic_kavaler",
                "topic_Kavaler"
            )
        ),

        ExerciseIdentity(
            id = "ex_160",
            belt = Belt.GREEN,
            hebrewTitle = "קוואלר נגד התנגדות - הליכה לפנים",
            topicKeys = setOf(
                "קוואלר",
                "קאוולר",
                "topic_kavaler",
                "topic_Kavaler"
            ),
            aliases = setOf(
                "קוואלר נגד ההתנגדות - הליכה לפנים"
            )
        ),

        ExerciseIdentity(
            id = "ex_161",
            belt = Belt.GREEN,
            hebrewTitle = "קוואלר - אגודלים",
            topicKeys = setOf(
                "קוואלר",
                "קאוולר",
                "topic_kavaler",
                "topic_Kavaler"
            )
        ),

        ExerciseIdentity(
            id = "ex_162",
            belt = Belt.GREEN,
            hebrewTitle = "קוואלר – מרפק",
            topicKeys = setOf(
                "קוואלר",
                "קאוולר",
                "topic_kavaler",
                "topic_Kavaler"
            ),
            aliases = setOf(
                "קוואלר - מרפק"
            )
        ),

        // ===== חגורה ירוקה — מכות מרפק / מכות במקל או רובה =====

        ExerciseIdentity(
            id = "ex_163",
            belt = Belt.GREEN,
            hebrewTitle = "מכת מרפק נגד קבוצה",
            topicKeys = setOf(
                "מכות מרפק",
                "עבודת ידיים",
                "hands_elbows",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_164",
            belt = Belt.GREEN,
            hebrewTitle = "התקפה עם מקל לנקודות תורפה",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "topic_hands"
            )
        ),

        // ===== חגורה ירוקה — בעיטות =====

        ExerciseIdentity(
            id = "ex_165",
            belt = Belt.GREEN,
            hebrewTitle = "בעיטה רגילה ובעיטת מגל באותה רגל",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_166",
            belt = Belt.GREEN,
            hebrewTitle = "בעיטת מגל לאחור בשיכול אחורי",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_167",
            belt = Belt.GREEN,
            hebrewTitle = "בעיטה לצד בסיבוב",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_168",
            belt = Belt.GREEN,
            hebrewTitle = "בעיטת סטירה חיצונית",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        // ===== חגורה ירוקה — הגנות פנימיות / חיצוניות נגד אגרופים =====

        ExerciseIdentity(
            id = "ex_169",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה פנימית נגד ימין באמת שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_170",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה פנימית נגד שמאל באמת שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_171",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה פנימית נגד ימין - אגרוף שמאל בהחלקה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_172",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה חיצונית נגד ימין באגרוף מהופך",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_173",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה חיצונית נגד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        ExerciseIdentity(
            id = "ex_174",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה חיצונית נגד שמאל בהתקדמות",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות חיצוניות נגד מכות",
                "הגנות חיצוניות נגד מכות",
                "defenses_root",
                "def_external_punch"
            )
        ),

        // ===== חגורה ירוקה — הגנות נגד בעיטות =====

        ExerciseIdentity(
            id = "ex_175",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד בעיטה רגילה - בעיטה לצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_176",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד בעיטה רגילה - טיימינג לצד החי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_177",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה חיצונית באמת שמאל נגד בעיטה רגילה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "kicks_hard",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_178",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד בעיטת מגל לפנים – בעיטה לצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה נגד בעיטת מגל לפנים - בעיטה לצד"
            )
        ),

        ExerciseIdentity(
            id = "ex_179",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד בעיטת מגל נמוכה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "kicks_hard"
            )
        ),

        ExerciseIdentity(
            id = "ex_180",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד בעיטת מגל לאחור - בעיטה בימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "kicks_hard"
            )
        ),

        ExerciseIdentity(
            id = "ex_181",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד בעיטת מגל לאחור - בעיטה שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "הגנות__הגנות נגד בעיטות מגל לאחור",
                "הגנות נגד בעיטות מגל לאחור",
                "kicks_hard"
            ),
            aliases = setOf(
                "הגנה נגד בעיטת מגל לאחור - בעיטה בשמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_182",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד בעיטת מגל לאחור - אגרוף שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "kicks_hard"
            )
        ),

        ExerciseIdentity(
            id = "ex_183",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד בעיטת מגל לאחור בסיבוב – בעיטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "הגנות__הגנות נגד בעיטות מגל לאחור",
                "הגנות נגד בעיטות מגל לאחור",
                "kicks_hard"
            ),
            aliases = setOf(
                "הגנה נגד בעיטת מגל לאחור בסיבוב - בעיטה",
                "הגנה נגד בעיטת מגל לאחור בסבוב - בעיטה"
            )
        ),

        ExerciseIdentity(
            id = "ex_184",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה חיצונית באמת ימין נגד בעיטה לצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטה לצד",
                "הגנות נגד בעיטה לצד",
                "הגנות נגד בעיטות",
                "kicks_hard",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_185",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה חיצונית באמת שמאל נגד בעיטה לצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטה לצד",
                "הגנות נגד בעיטה לצד",
                "הגנות נגד בעיטות",
                "kicks_hard",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_186",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד בעיטת לצד בעיטת סטירה חיצונית",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטה לצד",
                "הגנות נגד בעיטה לצד",
                "הגנות__הגנות נגד בעיטות לצד",
                "הגנות נגד בעיטות לצד",
                "הגנות נגד בעיטות",
                "kicks_hard"
            ),
            aliases = setOf(
                "הגנה נגד בעיטה לצד בעיטת סטירה חיצונית",
                "הגנה נגד בעיטה לצד - בעיטת סטירה חיצונית"
            )
        ),

        // ===== חגורה ירוקה — הגנות מסכין =====

        ExerciseIdentity(
            id = "ex_187",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה מאיום סכין לעורק שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_188",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה מאיום סכין לעורק ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_189",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה מאיום סכין מאחור – להב לגרוגרת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות__הגנות מאיום סכין",
                "הגנות מאיום סכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום סכין מאחור - להב לגרוגרת",
                "הגנה מאיום סכין להב לגורגרת",
                "הגנה מאיום סכין להב לגרוגרת"
            )
        ),

        ExerciseIdentity(
            id = "ex_190",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה מאיום סכין מלפנים - חוד הסכין לגורגרת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_191",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה מאיום סכין מאחור - להב הסכין לגורגרת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_192",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה מאיום סכין - חוד לבטן התחתונה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_193",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה מאיום סכין מאחור – חוד לגב",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום סכין מאחור - חוד לגב"
            )
        ),

        ExerciseIdentity(
            id = "ex_194",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה רגילה – בעיטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד דקירה רגילה - בעיטה"
            )
        ),

        ExerciseIdentity(
            id = "ex_195",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה מזרחית - בעיטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_196",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה ישרה מלפנים – בעיטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד דקירה ישרה מלפנים - בעיטה"
            )
        ),

        ExerciseIdentity(
            id = "ex_197",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה ישרה נמוכה – בעיטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד דקירה ישרה נמוכה - בעיטה"
            )
        ),

        ExerciseIdentity(
            id = "ex_198",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה ישרה מלפנים – הגנת גוף ובעיטת מגל למפסעה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד דקירה ישרה מלפנים - הגנת גוף ובעיטת מגל למפסעה"
            )
        ),

        ExerciseIdentity(
            id = "ex_199",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה רגילה מהצד - בעיטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_200",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה ישרה - בעיטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_201",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה מזרחית מהצד - בעיטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_202",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה רגילה מהצד - התוקף בצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_203",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה רגילה מהצד - התוקף בצד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_204",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה מזרחית מהצד לעורף - התוקף בצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_205",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה מזרחית מהצד לגב - התוקף בצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_206",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה מזרחית מהצד לגרון - התוקף בצד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_207",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד דקירה מזרחית מהצד לבטן - התוקף בצד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        // ===== חגורה ירוקה — הגנות נגד מקל =====

        ExerciseIdentity(
            id = "ex_208",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד מקל - צד חי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_209",
            belt = Belt.GREEN,
            hebrewTitle = "הגנה נגד מקל - צד מת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            )
        ),

        // ===== חגורה ירוקה — שחרורים =====

        ExerciseIdentity(
            id = "ex_210",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מתפיסת שיער מאחור - צד חי",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_211",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מתפיסת שיער מאחור - צד מת",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_212",
            belt = Belt.GREEN,
            hebrewTitle = "חביקת יד מהצד - ראש התוקף מאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hands_hair_shirt",
                "releases_hugs_arm"
            ),
            aliases = setOf(
                "חביקות יד מצד - ראש התוקף מאחור"
            )
        ),

        ExerciseIdentity(
            id = "ex_213",
            belt = Belt.GREEN,
            hebrewTitle = "חביקת יד מהצד - ראש התוקף מלפנים",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hands_hair_shirt",
                "releases_hugs_arm"
            ),
            aliases = setOf(
                "חביקות יד מצד - ראש התוקף מלפנים"
            )
        ),

        ExerciseIdentity(
            id = "ex_214",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מתפיסת ידיים מאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_215",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מתפיסת חולצה מאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_216",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מתפיסת שיער מהצד - צד ימין",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_217",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מתפיסת שיער מהצד - צד שמאל",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_218",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מחביקה פתוחה מהצד",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_219",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מחביקה פתוחה מלפנים בהרמה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_220",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מחביקה סגורה מהצד",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_221",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מחביקה סגורה מלפנים בהרמה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_222",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מחביקה פתוחה מאחור בהרמה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "שחרורים__שחרורים מחביקות גוף",
                "שחרורים מחביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            ),
            aliases = setOf(
                "שחרור מחביקה סגורה מאחור בהרמה"
            )
        ),

        ExerciseIdentity(
            id = "ex_223",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מחביקה פתוחה מאחור עם תפיסת אצבע",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_224",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור מחביקה פתוחה מאחור - בריח על האצבעות",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_225",
            belt = Belt.GREEN,
            hebrewTitle = "שחרור חביקת צואר מאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות צואר",
                "חביקות צואר",
                "שחרורים__שחרורים מחביקות צואר",
                "שחרורים מחביקות צואר",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            ),
            aliases = setOf(
                "שחרור מחביקת צואר מאחור"
            )
        ),

        ExerciseIdentity(
            id = "ex_226",
            belt = Belt.GREEN,
            hebrewTitle = "חביקות יד מצד - ראש התוקף מאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות יד",
                "חביקות יד",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hugs_arm",
                "releases_hands_hair_shirt"
            ),
            aliases = setOf(
                "חביקת יד מהצד - ראש התוקף מאחור"
            )
        ),

        ExerciseIdentity(
            id = "ex_227",
            belt = Belt.GREEN,
            hebrewTitle = "חביקות יד מצד - ראש התוקף מלפנים",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות יד",
                "חביקות יד",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hugs_arm",
                "releases_hands_hair_shirt"
            ),
            aliases = setOf(
                "חביקת יד מהצד - ראש התוקף מלפנים"
            )
        ),

        // ===== חגורה כחולה — תרגילים ex_228 עד ex_272 =====

        // ===== חגורה כחולה — כללי / עבודת קרקע / בלימות וגלגולים =====

        ExerciseIdentity(
            id = "ex_228",
            belt = Belt.BLUE,
            hebrewTitle = "מניעת נפילה מחביקת שוקיים מלפנים להפלה",
            topicKeys = setOf(
                "כללי",
                "עבודת קרקע",
                "topic_general",
                "topic_ground_prep"
            )
        ),

        ExerciseIdentity(
            id = "ex_229",
            belt = Belt.BLUE,
            hebrewTitle = "גלגול לצד - ימין/שמאל",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            ),
            aliases = setOf(
                "גלגול לצד – ימין/שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_230",
            belt = Belt.BLUE,
            hebrewTitle = "גלגול ברחיפה - ימין/שמאל",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        ExerciseIdentity(
            id = "ex_231",
            belt = Belt.BLUE,
            hebrewTitle = "גלגול לגובה - ימין/שמאל",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        ExerciseIdentity(
            id = "ex_232",
            belt = Belt.BLUE,
            hebrewTitle = "גלגול ללא ידיים - ימין/שמאל",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "topic_breakfalls_rolls"
            )
        ),

        // ===== חגורה כחולה — בעיטות =====

        ExerciseIdentity(
            id = "ex_233",
            belt = Belt.BLUE,
            hebrewTitle = "בעיטת פטיש",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_234",
            belt = Belt.BLUE,
            hebrewTitle = "בעיטת גזיזה אחורית",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_235",
            belt = Belt.BLUE,
            hebrewTitle = "בעיטת גזיזה קדמית",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_236",
            belt = Belt.BLUE,
            hebrewTitle = "בעיטת גזיזה קדמית ובעיטת גזיזה אחורית בסיבוב",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_237",
            belt = Belt.BLUE,
            hebrewTitle = "בעיטת מגל לאחור בסיבוב",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_238",
            belt = Belt.BLUE,
            hebrewTitle = "בעיטת סטירה חיצונית בסיבוב",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        // ===== חגורה כחולה — שחרורים =====

        ExerciseIdentity(
            id = "ex_239",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור תפיסת ידיים בשכיבה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרור מתפיסות ידיים / שיער / חולצה",
                "שחרור מתפיסות ידיים / שיער / חולצה",
                "releases",
                "releases_hands_hair_shirt"
            )
        ),

        ExerciseIdentity(
            id = "ex_240",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחביקת צואר מהצד והפלה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות צואר",
                "חביקות צואר",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_241",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחביקת צואר מאחור עם נעילה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות צואר",
                "חביקות צואר",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_242",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחביקת צואר בשכיבה ברכיבה צמודה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות צואר",
                "חביקות צואר",
                "שחרורים__עבודת קרקע",
                "עבודת קרקע",
                "releases",
                "releases_hugs",
                "topic_ground_prep"
            )
        ),

        ExerciseIdentity(
            id = "ex_243",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחניקה לקיר - מלפנים לא צמודה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_244",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחניקה לקיר - צמודה מלפנים",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_245",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחניקה לקיר - צמודה מאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_246",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחניקה לקיר - דחיפה מאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "releases",
                "releases_chokes"
            )
        ),

        ExerciseIdentity(
            id = "ex_247",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחניקה בשכיבה - ידיים כפופות",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "שחרורים__עבודת קרקע",
                "עבודת קרקע",
                "releases",
                "releases_chokes",
                "topic_ground_prep"
            ),
            aliases = setOf(
                "שחרור מחניקה בשכיבה – ידיים כפופות"
            )
        ),

        ExerciseIdentity(
            id = "ex_248",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחניקה בשכיבה - ידיים ישרות",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "שחרורים__עבודת קרקע",
                "עבודת קרקע",
                "releases",
                "releases_chokes",
                "topic_ground_prep"
            ),
            aliases = setOf(
                "שחרור מחניקה בשכיבה – ידיים ישרות"
            )
        ),

        ExerciseIdentity(
            id = "ex_249",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחניקה צמודה בשכיבה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "שחרורים__עבודת קרקע",
                "עבודת קרקע",
                "releases",
                "releases_chokes",
                "topic_ground_prep"
            )
        ),

        ExerciseIdentity(
            id = "ex_250",
            belt = Belt.BLUE,
            hebrewTitle = "שחרור מחניקה מהצד בשכיבה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחניקות",
                "שחרורים מחניקות",
                "שחרורים__עבודת קרקע",
                "עבודת קרקע",
                "releases",
                "releases_chokes",
                "topic_ground_prep"
            )
        ),

        // ===== חגורה כחולה — הגנות מסכין =====

        ExerciseIdentity(
            id = "ex_251",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה מאיום סכין לעורק שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_252",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה מאיום סכין לעורק ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_253",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה מאיום סכין - להב לגרגרת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות__הגנות מאיום סכין לקיר",
                "הגנות מאיום סכין לקיר",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום סכין – להב לגרגרת",
                "הגנה מאיום סכין להב לגורגרת",
                "הגנה מאיום סכין להב לגרוגרת"
            )
        ),

        ExerciseIdentity(
            id = "ex_254",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה מאיום סכין מלפנים - חוד הסכין לגרגרת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות__הגנות מאיום סכין לקיר",
                "הגנות מאיום סכין לקיר",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום סכין מלפנים – חוד הסכין לגרגרת",
                "הגנה מאיום סכין מלפנים - חוד הסכין לגורגרת"
            )
        ),

        ExerciseIdentity(
            id = "ex_255",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה מאיום סכין מאחור - להב הסכין לגרגרת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות__הגנות מאיום סכין לקיר",
                "הגנות מאיום סכין לקיר",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום סכין מאחור – להב הסכין לגרגרת",
                "הגנה מאיום סכין מאחור - להב הסכין לגורגרת"
            )
        ),

        ExerciseIdentity(
            id = "ex_256",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה מאיום סכין מאחור - חוד לגב",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום סכין מאחור – חוד לגב"
            )
        ),

        ExerciseIdentity(
            id = "ex_257",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה מאיום סכין מאחור - להב על העורף",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום סכין מאחור – להב על העורף"
            )
        ),

        ExerciseIdentity(
            id = "ex_258",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד דקירה מזרחית - יד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד דקירה מזרחית – יד"
            )
        ),

        ExerciseIdentity(
            id = "ex_259",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד דקירה ישרה נמוכה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_260",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד דקירה ישרה מהצד - צד מת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד דקירה ישרה מהצד – צד מת"
            )
        ),

        ExerciseIdentity(
            id = "ex_261",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד דקירה ישרה מהצד - צד חי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד דקירה ישרה מהצד – צד חי"
            )
        ),

        ExerciseIdentity(
            id = "ex_262",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה פנימית נגד דקירה ישרה - צד חי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה פנימית נגד דקירה ישרה – צד חי"
            )
        ),

        ExerciseIdentity(
            id = "ex_263",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה פנימית נגד דקירה ישרה - צד מת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה פנימית נגד דקירה ישרה – צד מת"
            )
        ),

        // ===== חגורה כחולה — הגנות נגד בעיטות =====

        ExerciseIdentity(
            id = "ex_264",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד בעיטת ברך מלפנים",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ברך",
                "הגנות נגד בעיטות ברך",
                "הגנות נגד ברך",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_265",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד בעיטת ברך מהצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ברך",
                "הגנות נגד בעיטות ברך",
                "הגנות נגד ברך",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_266",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד בעיטה רגילה - סייד-סטפ לצד המת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה נגד בעיטה רגילה – סייד-סטפ לצד המת"
            )
        ),

        ExerciseIdentity(
            id = "ex_267",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד בעיטה רגילה - סייד-סטפ לצד החי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה נגד בעיטה רגילה – סייד-סטפ לצד החי"
            )
        ),

        ExerciseIdentity(
            id = "ex_268",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד בעיטת מגל לפנים עם השוק",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_269",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה נגד בעיטת מגל לצלעות",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_270",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick"
            ),
            aliases = setOf(
                "הגנה פנימית נגד בעיטת מגל לפנים — בעיטה לצד"
            )
        ),

        ExerciseIdentity(
            id = "ex_271",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה פנימית נגד בעיטת מגל לפנים - בעיטה לאחור",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick"
            ),
            aliases = setOf(
                "הגנה פנימית נגד בעיטת מגל לפנים — בעיטה לאחור"
            )
        ),

        ExerciseIdentity(
            id = "ex_272",
            belt = Belt.BLUE,
            hebrewTitle = "הגנה פנימית באמת ימין נגד בעיטה לצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטה לצד",
                "הגנות נגד בעיטה לצד",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick"
            )
        ),

        // ===== חגורה חומה — תרגילים ex_273 עד ex_298 =====

        // ===== חגורה חומה — בעיטות בניתור =====

        ExerciseIdentity(
            id = "ex_273",
            belt = Belt.BROWN,
            hebrewTitle = "בעיטה רגילה ובעיטת מגל בניתור",
            topicKeys = setOf(
                "בעיטות",
                "בעיטות בניתור",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_274",
            belt = Belt.BROWN,
            hebrewTitle = "בעיטת מגל בניתור",
            topicKeys = setOf(
                "בעיטות",
                "בעיטות בניתור",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_275",
            belt = Belt.BROWN,
            hebrewTitle = "בעיטת מגל כפולה בניתור",
            topicKeys = setOf(
                "בעיטות",
                "בעיטות בניתור",
                "topic_kicks"
            )
        ),

        // ===== חגורה חומה — בלימות וגלגולים =====

        ExerciseIdentity(
            id = "ex_276",
            belt = Belt.BROWN,
            hebrewTitle = "גלגול עם רובה",
            topicKeys = setOf(
                "בלימות וגלגולים",
                "גלגולים",
                "topic_breakfalls_rolls",
                "rifle"
            )
        ),

        // ===== חגורה חומה — הגנות נגד בעיטות =====

        ExerciseIdentity(
            id = "ex_277",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה פנימית נגד בעיטה לסנטר",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick"
            )
        ),

        ExerciseIdentity(
            id = "ex_278",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה חיצונית נגד בעיטה רגילה - פריצה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה חיצונית נגד בעיטה רגילה – פריצה"
            )
        ),

        ExerciseIdentity(
            id = "ex_279",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה חיצונית נגד בעיטה רגילה - גזיזה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה חיצונית נגד בעיטה רגילה – גזיזה"
            )
        ),

        ExerciseIdentity(
            id = "ex_280",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה חיצונית נגד בעיטה רגילה - טאטוא",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה חיצונית נגד בעיטה רגילה – טאטוא"
            )
        ),

        ExerciseIdentity(
            id = "ex_281",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה פנימית נגד בעיטה רגילה - טאטוא",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות ישרות / למפשעה",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick"
            ),
            aliases = setOf(
                "הגנה פנימית נגד בעיטה רגילה – טאטוא"
            )
        ),

        ExerciseIdentity(
            id = "ex_282",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד בעיטת מגל - פריצה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה נגד בעיטת מגל – פריצה"
            )
        ),

        ExerciseIdentity(
            id = "ex_283",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד בעיטת מגל לפנים - גזיזה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "הגנות__הגנה – בעיטה",
                "הגנה – בעיטה",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה נגד בעיטת מגל לפנים – גזיזה",
                "הגנה חיצונית נגד מגל לפנים – גזיזה",
                "הגנה חיצונית נגד מגל לפנים - גזיזה"
            )
        ),

        ExerciseIdentity(
            id = "ex_284",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה חיצונית נגד מגל לפנים - טאטוא",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה חיצונית נגד מגל לפנים – טאטוא"
            )
        ),

        ExerciseIdentity(
            id = "ex_285",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד בעיטת מגל לאחור - פריצה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה נגד בעיטת מגל לאחור – פריצה"
            )
        ),

        // ===== חגורה חומה — שחרורים =====

        ExerciseIdentity(
            id = "ex_286",
            belt = Belt.BROWN,
            hebrewTitle = "חביקת צוואר מאחור - בריח על העורף, המגן כפוף לפנים",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__חביקות צואר",
                "חביקות צואר",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            ),
            aliases = setOf(
                "חביקת צוואר מאחור – בריח על העורף, המגן כפוף לפנים",
                "חביקת צואר מאחור – בריח על העורף, המגן כפוף לפנים",
                "חביקת צואר מאחור - בריח על העורף, המגן כפוף לפנים",
                "חביקת צואר מאחור - בריח על השורף, המגן כפוף לפנים"
            )
        ),

        // ===== חגורה חומה — הגנות נגד מקל =====

        ExerciseIdentity(
            id = "ex_287",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד מקל בסיבוב - צד חי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד מקל בסיבוב – צד חי"
            )
        ),

        ExerciseIdentity(
            id = "ex_288",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד מקל עם קוואלר - צד מת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד מקל עם קוואלר – צד מת"
            )
        ),

        ExerciseIdentity(
            id = "ex_289",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד מקל נקודת תורפה - לצד המת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד מקל נקודת תורפה – לצד המת"
            )
        ),

        // ===== חגורה חומה — הגנות נגד סכין בשיסוף =====

        ExerciseIdentity(
            id = "ex_290",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד סכין בשיסוף - הטיה והגנה לצד החי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות__הגנה – סכין",
                "הגנה – סכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד סכין בשיסוף – הטיה והגנה לצד החי",
                "הגנה נגד סכין בשיסוף - הטיה והגנה לצד החי",
                "הגנה נגד סכין בשיסוף – הטיה והגנה לצד חי",
                "הגנה נגד סכין בשיסוף - הטיה והגנה לצד חי"
            )
        ),

        ExerciseIdentity(
            id = "ex_291",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד סכין בשיסוף - הטיה והגנה לצד המת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות__הגנה – סכין",
                "הגנה – סכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד סכין בשיסוף – הטיה והגנה לצד המת",
                "הגנה נגד סכין בשיסוף - הטיה והגנה לצד המת",
                "הגנה נגד סכין בשיסוף – הטיה והגנה לצד מת",
                "הגנה נגד סכין בשיסוף - הטיה והגנה לצד מת"
            )
        ),

        ExerciseIdentity(
            id = "ex_292",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד סכין בשיסוף - פריצה והגנה לצד החי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות__הגנה – סכין",
                "הגנה – סכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד סכין בשיסוף – פריצה והגנה לצד החי",
                "הגנה נגד סכין בשיסוף - פריצה והגנה לצד החי",
                "הגנה נגד סכין בשיסוף – פריצה והגנה לצד חי",
                "הגנה נגד סכין בשיסוף - פריצה והגנה לצד חי"
            )
        ),

        ExerciseIdentity(
            id = "ex_293",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה נגד סכין בשיסוף - פריצה והגנה לצד המת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד סכין בשיסוף – פריצה והגנה לצד המת"
            )
        ),

        // ===== חגורה חומה — הגנות מאיום אקדח =====

        ExerciseIdentity(
            id = "ex_294",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה מאיום אקדח מלפנים ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "הגנות__הגנה – איום אקדח",
                "הגנה – איום אקדח",
                "gun_threat_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום אקדח מלפנים"
            )
        ),

        ExerciseIdentity(
            id = "ex_295",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה מאיום אקדח מהצד הפנימי - תוקף בצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "הגנות__הגנה – איום אקדח",
                "הגנה – איום אקדח",
                "gun_threat_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד",
                "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד ימין",
                "הגנה מאיום אקדח מהצד הפנימי - תוקף בצד ימין"
            )
        ),

        ExerciseIdentity(
            id = "ex_296",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה מאיום אקדח מהצד הפנימי - תוקף בצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_297",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה מאיום אקדח מהצד החיצוני",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_298",
            belt = Belt.BROWN,
            hebrewTitle = "הגנה מאיום אקדח מאחור",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            )
        ),

        // ===== חגורה שחורה — תרגילים ex_299 עד ex_391 =====

        // ===== חגורה שחורה — בעיטות =====

        ExerciseIdentity(
            id = "ex_299",
            belt = Belt.BLACK,
            hebrewTitle = "ניתור ברגל שמאל ובעיטה רגילה ברגל ימין",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_300",
            belt = Belt.BLACK,
            hebrewTitle = "ניתור ברגל שמאל ובעיטה לצד ברגל ימין",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_301",
            belt = Belt.BLACK,
            hebrewTitle = "ניתור ברגל שמאל ובעיטה לצד ברגל שמאל",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_302",
            belt = Belt.BLACK,
            hebrewTitle = "בעיטת לצד בסיבוב מלא בניתור",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            ),
            aliases = setOf(
                "בעיטה לצד בסיבוב מלא בניתור"
            )
        ),

        ExerciseIdentity(
            id = "ex_303",
            belt = Belt.BLACK,
            hebrewTitle = "בעיטת מגל לאחור בסיבוב בניתור",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        ExerciseIdentity(
            id = "ex_304",
            belt = Belt.BLACK,
            hebrewTitle = "בעיטת הגנה לאחור בניתור",
            topicKeys = setOf(
                "בעיטות",
                "topic_kicks"
            )
        ),

        // ===== חגורה שחורה — הגנות נגד בעיטות =====

        ExerciseIdentity(
            id = "ex_305",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד בעיטת מגל לפנים לראש – הדיפה באמת שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות__הגנות נגד בעיטות",
                "הגנות נגד בעיטות",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה נגד בעיטת מגל לפנים לראש - הדיפה באמת שמאל",
                "הגנה נגד בעיטה רגילה – התחמקות בסיבוב",
                "הגנה נגד בעיטה רגילה - התחמקות בסיבוב"
            )
        ),

        ExerciseIdentity(
            id = "ex_306",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד בעיטת מגל לפנים לראש – רגל עברה מעל הראש",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה נגד בעיטת מגל לפנים לראש - רגל עברה מעל הראש"
            )
        ),

        ExerciseIdentity(
            id = "ex_307",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד מגל לפנים לראש – התחמקות גוף בסיבוב וגזיזה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מגל / מגל לאחור",
                "הגנות נגד מגל / מגל לאחור",
                "הגנות__הגנות נגד בעיטות",
                "הגנות נגד בעיטות",
                "defenses_root",
                "kicks_hard",
                "def_internal_kick",
                "def_external_kick"
            ),
            aliases = setOf(
                "הגנה נגד מגל לפנים לראש - התחמקות גוף בסיבוב וגזיזה",
                "הגנה נגד בעיטת סטירה – גזיזה",
                "הגנה נגד בעיטת סטירה - גזיזה"
            )
        ),

        // ===== חגורה שחורה — הגנות מסכין / שיסוף =====

        ExerciseIdentity(
            id = "ex_308",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד סכין שיסוף מהצד החי – בצד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד סכין שיסוף מהצד החי - בצד ימין"
            )
        ),

        ExerciseIdentity(
            id = "ex_309",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד סכין שיסוף מהצד החי – בצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד סכין שיסוף מהצד החי - בצד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_310",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד סכין שיסוף מהצד המת – בצד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד סכין שיסוף מהצד המת - בצד ימין"
            )
        ),

        ExerciseIdentity(
            id = "ex_311",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד סכין שיסוף מהצד המת – בצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מסכין",
                "הגנות מסכין",
                "הגנות נגד סכין",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד סכין שיסוף מהצד המת - בצד שמאל"
            )
        ),

        // ===== חגורה שחורה — הגנות עם רובה נגד דקירות סכין =====

        ExerciseIdentity(
            id = "ex_312",
            belt = Belt.BLACK,
            hebrewTitle = "רובה נגד דקירה ישרה גבוהה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות עם רובה נגד דקירות סכין",
                "הגנות עם רובה נגד דקירות סכין",
                "הגנות__הגנה עם רובה נגד סכין",
                "הגנה עם רובה נגד סכין",
                "knife_defense",
                "knife_defense_rifle_against_knife_stabs",
                "defenses_root"
            ),
            aliases = setOf(
                "רובה נגד דקירה ישירה גבוהה"
            )
        ),

        ExerciseIdentity(
            id = "ex_313",
            belt = Belt.BLACK,
            hebrewTitle = "רובה נגד דקירה ישרה נמוכה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות עם רובה נגד דקירות סכין",
                "הגנות עם רובה נגד דקירות סכין",
                "הגנות__הגנה עם רובה נגד סכין",
                "הגנה עם רובה נגד סכין",
                "knife_defense",
                "knife_defense_rifle_against_knife_stabs",
                "defenses_root"
            ),
            aliases = setOf(
                "רובה נגד דקירה ישירה נמוכה"
            )
        ),

        ExerciseIdentity(
            id = "ex_314",
            belt = Belt.BLACK,
            hebrewTitle = "רובה נגד דקירה רגילה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות עם רובה נגד דקירות סכין",
                "הגנות עם רובה נגד דקירות סכין",
                "knife_defense",
                "knife_defense_rifle_against_knife_stabs",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_315",
            belt = Belt.BLACK,
            hebrewTitle = "רובה נגד דקירה מזרחית מימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות עם רובה נגד דקירות סכין",
                "הגנות עם רובה נגד דקירות סכין",
                "knife_defense",
                "knife_defense_rifle_against_knife_stabs",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_316",
            belt = Belt.BLACK,
            hebrewTitle = "רובה נגד דקירה מזרחית משמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות עם רובה נגד דקירות סכין",
                "הגנות עם רובה נגד דקירות סכין",
                "knife_defense",
                "knife_defense_rifle_against_knife_stabs",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_317",
            belt = Belt.BLACK,
            hebrewTitle = "רובה נגד דקירה מזרחית מלמטה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות עם רובה נגד דקירות סכין",
                "הגנות עם רובה נגד דקירות סכין",
                "knife_defense",
                "knife_defense_rifle_against_knife_stabs",
                "defenses_root"
            )
        ),

        // ===== חגורה שחורה — הגנות מאיום אקדח =====

        ExerciseIdentity(
            id = "ex_318",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום אקדח לראש מלפנים",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_319",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום אקדח צמוד לראש מלפנים",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_320",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום אקדח מלפנים – קנה קצר",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד איום אקדח מלפנים - קנה קצר"
            )
        ),

        ExerciseIdentity(
            id = "ex_321",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום אקדח לראש – צד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד איום אקדח לראש - צד ימין"
            )
        ),

        ExerciseIdentity(
            id = "ex_322",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום אקדח לראש – צד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד איום אקדח לראש - צד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_323",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום אקדח לראש מהצד מאחור – צד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד איום אקדח לראש מהצד מאחור - צד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_324",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה מאיום אקדח בהובלה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_325",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום אקדח לראש מאחור",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_326",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום אקדח מאחור בידיים מורמות",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            )
        ),

        // ===== חגורה שחורה — הגנות נגד מקל ארוך =====

        ExerciseIdentity(
            id = "ex_327",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד מקל ארוך – התקפה לצד ימין מגן",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד מקל ארוך - התקפה לצד ימין מגן"
            )
        ),

        ExerciseIdentity(
            id = "ex_328",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד מקל ארוך – התקפה לצד שמאל מגן",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד מקל ארוך - התקפה לצד שמאל מגן"
            )
        ),

        ExerciseIdentity(
            id = "ex_329",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד מקל ארוך מצד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_330",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד מקל ארוך מצד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_331",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד דקירה במקל ארוך – הצד החי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד דקירה במקל ארוך - הצד החי"
            )
        ),

        ExerciseIdentity(
            id = "ex_332",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד דקירה במקל ארוך – הצד המת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מקל",
                "הגנות נגד מקל",
                "stick_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד דקירה במקל ארוך - הצד המת"
            )
        ),

        // ===== חגורה שחורה — הגנות פנימיות נגד אגרוף שמאל =====

        ExerciseIdentity(
            id = "ex_333",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה פנימית נגד אגרוף שמאל – בעיטת הגנה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            ),
            aliases = setOf(
                "הגנה פנימית נגד אגרוף שמאל - בעיטת הגנה"
            )
        ),

        ExerciseIdentity(
            id = "ex_334",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה פנימית נגד אגרוף שמאל – בעיטה לצד",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            ),
            aliases = setOf(
                "הגנה פנימית נגד אגרוף שמאל - בעיטה לצד"
            )
        ),

        ExerciseIdentity(
            id = "ex_335",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה פנימית נגד אגרוף שמאל – בעיטה רגילה לאחור",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            ),
            aliases = setOf(
                "הגנה פנימית נגד אגרוף שמאל - בעיטה רגילה לאחור"
            )
        ),

        ExerciseIdentity(
            id = "ex_336",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לאחור",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            ),
            aliases = setOf(
                "הגנה פנימית נגד אגרוף שמאל - בעיטת מגל לאחור"
            )
        ),

        ExerciseIdentity(
            id = "ex_337",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה פנימית נגד אגרוף שמאל – בעיטת סטירה חיצונית",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            ),
            aliases = setOf(
                "הגנה פנימית נגד אגרוף שמאל - בעיטת סטירה חיצונית"
            )
        ),

        ExerciseIdentity(
            id = "ex_338",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לפנים",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            ),
            aliases = setOf(
                "הגנה פנימית נגד אגרוף שמאל - בעיטת מגל לפנים"
            )
        ),

        ExerciseIdentity(
            id = "ex_339",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה פנימית נגד אגרוף שמאל – גזיזה קדמית",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות פנימיות נגד מכות",
                "הגנות פנימיות נגד מכות",
                "defenses_root",
                "def_internal_punch"
            ),
            aliases = setOf(
                "הגנה פנימית נגד אגרוף שמאל - גזיזה קדמית"
            )
        ),

        // ===== חגורה שחורה — המשך תרגילים ex_340 עד ex_379 =====

        // ===== חגורה שחורה — השלמת הגנות מאיום אקדח =====

        ExerciseIdentity(
            id = "ex_340",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום אקדח מאחור בדחיפה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות מאיום אקדח",
                "הגנות מאיום אקדח",
                "gun_threat_defense",
                "defenses_root"
            )
        ),

        // ===== חגורה שחורה — שחרורים מחביקות צואר =====

        ExerciseIdentity(
            id = "ex_341",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מחביקת צואר מהצד - משיכה לאחור",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות צואר",
                "שחרורים מחביקות צואר",
                "שחרורים__חביקות צואר",
                "חביקות צואר",
                "releases",
                "releases_hugs",
                "releases_hugs_neck"
            )
        ),

        ExerciseIdentity(
            id = "ex_342",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מחביקת צואר מהצד - יד תפוסה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות צואר",
                "שחרורים מחביקות צואר",
                "שחרורים__חביקות צואר",
                "חביקות צואר",
                "releases",
                "releases_hugs",
                "releases_hugs_neck"
            )
        ),

        ExerciseIdentity(
            id = "ex_343",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מחביקת צואר מהצד - זריקת רגל",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות צואר",
                "שחרורים מחביקות צואר",
                "שחרורים__חביקות צואר",
                "חביקות צואר",
                "releases",
                "releases_hugs",
                "releases_hugs_neck"
            )
        ),

        ExerciseIdentity(
            id = "ex_344",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מחביקת צואר מהצד - מהברך",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות צואר",
                "שחרורים מחביקות צואר",
                "שחרורים__חביקות צואר",
                "חביקות צואר",
                "releases",
                "releases_hugs",
                "releases_hugs_neck"
            )
        ),

        // ===== חגורה שחורה — שחרור מתפיסת נלסון =====

        ExerciseIdentity(
            id = "ex_345",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מתפיסת נלסון",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מתפיסות נלסון",
                "שחרורים מתפיסות נלסון",
                "releases",
                "releases_hugs"
            )
        ),

        // ===== חגורה שחורה — שחרורים מחביקות גוף =====

        ExerciseIdentity(
            id = "ex_346",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מחביקה סגורה מהצד",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות גוף",
                "שחרורים מחביקות גוף",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_347",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מחביקה סגורה מהצד - היד הרחוקה משוחררת",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות גוף",
                "שחרורים מחביקות גוף",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_348",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מחביקה פתוחה מהצד",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות גוף",
                "שחרורים מחביקות גוף",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_349",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מחביקה פתוחה מאחור - הטלה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות גוף",
                "שחרורים מחביקות גוף",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        ExerciseIdentity(
            id = "ex_350",
            belt = Belt.BLACK,
            hebrewTitle = "שחרור מחביקה סגורה מאחור - הטלה",
            topicKeys = setOf(
                "שחרורים",
                "שחרורים__שחרורים מחביקות גוף",
                "שחרורים מחביקות גוף",
                "שחרורים__חביקות גוף",
                "חביקות גוף",
                "releases",
                "releases_hugs",
                "releases_hugs_body"
            )
        ),

        // ===== חגורה שחורה — הגנות נגד מספר תוקפים =====

        ExerciseIdentity(
            id = "ex_351",
            belt = Belt.BLACK,
            hebrewTitle = "1 מקל 1 סכין - מקל בצד חי",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מספר תוקפים",
                "הגנות נגד מספר תוקפים",
                "הגנות נגד 2 תוקפים",
                "multiple_attackers_defense",
                "multiple_attackers_main",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_352",
            belt = Belt.BLACK,
            hebrewTitle = "1 מקל 1 סכין - מקל בצד מת",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מספר תוקפים",
                "הגנות נגד מספר תוקפים",
                "הגנות נגד 2 תוקפים",
                "multiple_attackers_defense",
                "multiple_attackers_main",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_353",
            belt = Belt.BLACK,
            hebrewTitle = "1 מקל 1 סכין - במקרה והסכין קרוב",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מספר תוקפים",
                "הגנות נגד מספר תוקפים",
                "הגנות נגד 2 תוקפים",
                "multiple_attackers_defense",
                "multiple_attackers_main",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_354",
            belt = Belt.BLACK,
            hebrewTitle = "הדמיה כנגד 2 תוקפים",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות נגד מספר תוקפים",
                "הגנות נגד מספר תוקפים",
                "הגנות נגד 2 תוקפים",
                "multiple_attackers_defense",
                "multiple_attackers_main",
                "defenses_root"
            )
        ),

        // ===== חגורה שחורה — מכות במקל קצר =====

        ExerciseIdentity(
            id = "ex_355",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - מכת מקל לראש",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מקל לראש")
        ),

        ExerciseIdentity(
            id = "ex_356",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - מכת מקל לרקה",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מקל לרקה")
        ),

        ExerciseIdentity(
            id = "ex_357",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - מכת מקל ללסת / צואר",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מקל ללסת / צואר")
        ),

        ExerciseIdentity(
            id = "ex_358",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - מכת מקל לעצם הבריח",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מקל לעצם הבריח")
        ),

        ExerciseIdentity(
            id = "ex_359",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - מכת מקל למרפק",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מקל למרפק")
        ),

        ExerciseIdentity(
            id = "ex_360",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - מכת מקל לשורש כף היד",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מקל לשורש כף היד")
        ),

        ExerciseIdentity(
            id = "ex_361",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - מכת מקל לפרקי האצבעות",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מקל לפרקי האצבעות")
        ),

        ExerciseIdentity(
            id = "ex_362",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - מכת מקל לברך",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מקל לברך")
        ),

        ExerciseIdentity(
            id = "ex_363",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - מכת מקל למפסעה",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מקל למפסעה")
        ),

        ExerciseIdentity(
            id = "ex_364",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - הצלפת מקל לצלעות",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("הצלפת מקל לצלעות")
        ),

        ExerciseIdentity(
            id = "ex_365",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - דקירת מקל חיצונית לצלעות",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("דקירת מקל חיצונית לצלעות")
        ),

        ExerciseIdentity(
            id = "ex_366",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - דקירת מקל ישרה לבטן / לגרון",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("דקירת מקל ישרה לבטן / לגרון")
        ),

        ExerciseIdentity(
            id = "ex_367",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל קצר - דקירת מקל הפוכה",
            topicKeys = setOf(
                "מכות במקל קצר",
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("דקירת מקל הפוכה")
        ),

        // ===== חגורה שחורה — מכות במקל / רובה =====

        ExerciseIdentity(
            id = "ex_368",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - התקפה עם מקל לנקודות תורפה",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            )
        ),

        ExerciseIdentity(
            id = "ex_369",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכה אופקית לצואר",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכה אופקית לצואר")
        ),

        ExerciseIdentity(
            id = "ex_370",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - דקירה",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("דקירה")
        ),

        ExerciseIdentity(
            id = "ex_371",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכת מגל",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת מגל")
        ),

        ExerciseIdentity(
            id = "ex_372",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - שיסוף",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("שיסוף")
        ),

        ExerciseIdentity(
            id = "ex_373",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכה למפסעה",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכה למפסעה")
        ),

        ExerciseIdentity(
            id = "ex_374",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכת סנוקרת",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכת סנוקרת")
        ),

        ExerciseIdentity(
            id = "ex_375",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכה לצד",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכה לצד")
        ),

        ExerciseIdentity(
            id = "ex_376",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכה לאחור",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכה לאחור")
        ),

        ExerciseIdentity(
            id = "ex_377",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכה אופקית לאחור",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכה אופקית לאחור")
        ),

        ExerciseIdentity(
            id = "ex_378",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכה אופקית ובעיטה רגילה למפסעה",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכה אופקית ובעיטה רגילה למפסעה")
        ),

        ExerciseIdentity(
            id = "ex_379",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכה אופקית ובעיטת הגנה לפנים",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכה אופקית ובעיטת הגנה לפנים")
        ),

        ExerciseIdentity(
            id = "ex_380",
            belt = Belt.BLACK,
            hebrewTitle = "מכות במקל / רובה - מכה לצד ובעיטה לצד",
            topicKeys = setOf(
                "מכות במקל / רובה",
                "עבודת ידיים",
                "hands_stick_rifle",
                "hands_all",
                "topic_hands"
            ),
            aliases = setOf("מכה לצד ובעיטה לצד")
        ),

        // ===== חגורה שחורה — השלמות ex_381 עד ex_395 =====

        ExerciseIdentity(
            id = "ex_381",
            belt = Belt.BLACK,
            hebrewTitle = "הגנת מקל נגד סכין בתפיסה רגילה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_382",
            belt = Belt.BLACK,
            hebrewTitle = "הגנת מקל נגד סכין בתפיסה מזרחית",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_383",
            belt = Belt.BLACK,
            hebrewTitle = "הגנת מקל נגד סכין בתפיסה ישרה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_384",
            belt = Belt.BLACK,
            hebrewTitle = "הגנת מקל נגד סכין בתפיסה רגילה – צד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנת מקל נגד סכין בתפיסה רגילה - צד ימין"
            )
        ),

        ExerciseIdentity(
            id = "ex_385",
            belt = Belt.BLACK,
            hebrewTitle = "הגנת מקל נגד סכין בתפיסה רגילה – צד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנת מקל נגד סכין בתפיסה רגילה - צד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_386",
            belt = Belt.BLACK,
            hebrewTitle = "הגנת מקל נגד סכין בתפיסה מזרחית – צד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנת מקל נגד סכין בתפיסה מזרחית - צד ימין"
            )
        ),

        ExerciseIdentity(
            id = "ex_387",
            belt = Belt.BLACK,
            hebrewTitle = "הגנת מקל נגד סכין בתפיסה מזרחית – צד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנת מקל נגד סכין בתפיסה מזרחית - צד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_388",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה פנימית במקל נגד סכין בתפיסה ישרה – צד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה פנימית במקל נגד סכין בתפיסה ישרה - צד ימין"
            )
        ),

        ExerciseIdentity(
            id = "ex_389",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה חיצונית במקל נגד סכין בתפיסה ישרה – צד ימין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה חיצונית במקל נגד סכין בתפיסה ישרה - צד ימין"
            )
        ),

        ExerciseIdentity(
            id = "ex_390",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה פנימית במקל נגד סכין בתפיסה ישרה – צד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה פנימית במקל נגד סכין בתפיסה ישרה - צד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_391",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה חיצונית במקל נגד סכין בתפיסה ישרה – צד שמאל",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה חיצונית במקל נגד סכין בתפיסה ישרה - צד שמאל"
            )
        ),

        ExerciseIdentity(
            id = "ex_392",
            belt = Belt.BLACK,
            hebrewTitle = "מקל בתנועה",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_393",
            belt = Belt.BLACK,
            hebrewTitle = "שימוש בסכין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנות – מקל נגד סכין",
                "הגנות – מקל נגד סכין",
                "הגנות מקל נגד סכין",
                "stick_defense",
                "knife_defense",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_394",
            belt = Belt.BLACK,
            hebrewTitle = "הגנות עם רובה נגד דקירות סכין",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנה עם רובה נגד סכין",
                "הגנה עם רובה נגד סכין",
                "הגנות__הגנות עם רובה נגד דקירות סכין",
                "הגנות עם רובה נגד דקירות סכין",
                "knife_defense",
                "knife_defense_rifle_against_knife_stabs",
                "defenses_root"
            )
        ),

        ExerciseIdentity(
            id = "ex_395",
            belt = Belt.BLACK,
            hebrewTitle = "הגנה נגד איום תת־מקלע",
            topicKeys = setOf(
                "הגנות",
                "הגנות__הגנה מאיום תמ״ק",
                "הגנה מאיום תמ״ק",
                "הגנות__הגנה מאיום תמ\"ק",
                "הגנה מאיום תמ\"ק",
                "gun_threat_defense",
                "defenses_root"
            ),
            aliases = setOf(
                "הגנה נגד איום תת-מקלע"
            )
        )
    )


    private val byId: Map<String, ExerciseIdentity> by lazy {
        knownExercises.associateBy { it.id }
    }

    /**
     * הפונקציה המרכזית:
     * כל מסך שרוצה לשמור סימון/מועדפים/הערות צריך להגיע בסוף לפה.
     */
    fun resolve(
        belt: Belt,
        hebrewTitle: String,
        topicKey: String? = null
    ): ResolvedExerciseIdentity {
        val cleanTitle = normalize(hebrewTitle)
        val cleanTopic = topicKey?.let { normalize(it) }?.takeIf { it.isNotBlank() }

        val exactWithTopic = knownExercises.firstOrNull { item ->
            item.belt == belt &&
                    item.matchesTitle(cleanTitle) &&
                    item.matchesTopic(cleanTopic)
        }

        if (exactWithTopic != null) {
            return ResolvedExerciseIdentity(
                id = exactWithTopic.id,
                isKnown = true,
                belt = belt,
                hebrewTitle = hebrewTitle,
                topicKey = topicKey
            )
        }

        val exactWithoutTopic = knownExercises.firstOrNull { item ->
            item.belt == belt && item.matchesTitle(cleanTitle)
        }

        if (exactWithoutTopic != null) {
            return ResolvedExerciseIdentity(
                id = exactWithoutTopic.id,
                isKnown = true,
                belt = belt,
                hebrewTitle = hebrewTitle,
                topicKey = topicKey
            )
        }

        return ResolvedExerciseIdentity(
            id = fallbackId(
                belt = belt,
                hebrewTitle = hebrewTitle,
                topicKey = topicKey
            ),
            isKnown = false,
            belt = belt,
            hebrewTitle = hebrewTitle,
            topicKey = topicKey
        )
    }

    fun idFor(
        belt: Belt,
        hebrewTitle: String,
        topicKey: String? = null
    ): String {
        return resolve(
            belt = belt,
            hebrewTitle = hebrewTitle,
            topicKey = topicKey
        ).id
    }

    fun knownById(id: String): ExerciseIdentity? {
        return byId[id.trim()]
    }

    fun isKnownId(id: String): Boolean {
        return knownById(id) != null
    }

    fun allKnownIds(): Set<String> {
        return byId.keys
    }

    fun allKnown(): List<ExerciseIdentity> {
        return knownExercises
    }

    data class AuditRow(
        val belt: Belt,
        val topicTitle: String,
        val subTopicTitle: String?,
        val index: Int,
        val rawTitle: String,
        val resolvedId: String,
        val isKnown: Boolean
    )

    data class AuditReport(
        val totalRows: Int,
        val knownRows: Int,
        val legacyRows: List<AuditRow>,
        val duplicateIds: Map<String, Int>,
        val knownIdsCount: Int
    ) {
        fun toLogLines(limit: Int = 120): List<String> {
            val lines = mutableListOf<String>()

            lines += "========== KMI ExerciseIdentity audit =========="
            lines += "ContentRepo rows total: $totalRows"
            lines += "Resolved known ex_* rows: $knownRows"
            lines += "Resolved legacy rows: ${legacyRows.size}"
            lines += "Known IDs defined in registry: $knownIdsCount"
            lines += "Duplicate IDs count: ${duplicateIds.size}"

            if (duplicateIds.isNotEmpty()) {
                lines += "---------- Duplicate IDs ----------"
                duplicateIds.forEach { (id, count) ->
                    lines += "DUPLICATE id=$id count=$count"
                }
            }

            if (legacyRows.isNotEmpty()) {
                lines += "---------- Missing / legacy rows, first $limit ----------"

                legacyRows
                    .take(limit)
                    .forEach { row ->
                        val sub = row.subTopicTitle?.let { " / $it" }.orEmpty()
                        lines += buildString {
                            append("LEGACY ")
                            append(row.belt.id)
                            append(" | ")
                            append(row.topicTitle)
                            append(sub)
                            append(" | index=")
                            append(row.index)
                            append(" | id=")
                            append(row.resolvedId)
                            append(" | title=")
                            append(row.rawTitle)
                        }
                    }

                if (legacyRows.size > limit) {
                    lines += "---------- ${legacyRows.size - limit} more legacy rows not printed ----------"
                }
            } else {
                lines += "✅ No legacy rows found. All ContentRepo rows resolve to known ex_* IDs."
            }

            lines += "========== KMI ExerciseIdentity audit finished =========="
            return lines
        }
    }

    fun auditAgainstContentRepo(): AuditReport {
        val allRows = mutableListOf<AuditRow>()

        Belt.order.forEach { belt ->
            val beltContent = ContentRepo.data[belt] ?: return@forEach

            beltContent.topics.forEach { topic ->
                val topicTitle = topic.title.trim().ifBlank { "כללי" }

                topic.items.forEachIndexed { index, rawItem ->
                    allRows += auditRowFor(
                        belt = belt,
                        topicTitle = topicTitle,
                        subTopicTitle = null,
                        topicKey = topicTitle,
                        index = index,
                        rawItem = rawItem
                    )
                }

                topic.subTopics.forEach { subTopic ->
                    val subTopicTitle = subTopic.title.trim()
                    val topicKey = "${topicTitle}__${subTopicTitle}"

                    subTopic.items.forEachIndexed { index, rawItem ->
                        allRows += auditRowFor(
                            belt = belt,
                            topicTitle = topicTitle,
                            subTopicTitle = subTopicTitle,
                            topicKey = topicKey,
                            index = index,
                            rawItem = rawItem
                        )
                    }
                }
            }
        }

        val duplicateIds = knownExercises
            .groupingBy { it.id.trim() }
            .eachCount()
            .filterValues { it > 1 }

        return AuditReport(
            totalRows = allRows.size,
            knownRows = allRows.count { it.isKnown },
            legacyRows = allRows.filterNot { it.isKnown },
            duplicateIds = duplicateIds,
            knownIdsCount = knownExercises.map { it.id.trim() }.distinct().size
        )
    }

    private fun auditRowFor(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String?,
        topicKey: String,
        index: Int,
        rawItem: String
    ): AuditRow {
        val cleanTitle = cleanTitleForAudit(
            topicTitle = topicTitle,
            rawItem = rawItem
        )

        val resolved = resolve(
            belt = belt,
            hebrewTitle = cleanTitle,
            topicKey = topicKey
        )

        return AuditRow(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = subTopicTitle,
            index = index,
            rawTitle = cleanTitle,
            resolvedId = resolved.id,
            isKnown = resolved.isKnown
        )
    }

    private fun cleanTitleForAudit(
        topicTitle: String,
        rawItem: String
    ): String {
        var s = rawItem.trim()

        if (topicTitle.isNotBlank() && s.startsWith("$topicTitle::")) {
            s = s.removePrefix("$topicTitle::").trim()
        }

        return s
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * מפתח לשמירת סטטוס.
     * למשל:
     * exercise_status_ex_001
     */
    fun statusPrefsKey(exerciseId: String): String {
        return "exercise_status_${exerciseId.trim()}"
    }

    fun notePrefsKey(exerciseId: String): String {
        return "exercise_note_${exerciseId.trim()}"
    }

    fun favoritePrefsKey(exerciseId: String): String {
        return "exercise_favorite_${exerciseId.trim()}"
    }

    private fun ExerciseIdentity.matchesTitle(cleanTitle: String): Boolean {
        if (normalize(hebrewTitle) == cleanTitle) return true

        return aliases.any { alias ->
            normalize(alias) == cleanTitle
        }
    }

    private fun ExerciseIdentity.matchesTopic(cleanTopic: String?): Boolean {
        if (cleanTopic.isNullOrBlank()) return true
        if (topicKeys.isEmpty()) return true

        return topicKeys.any { topic ->
            normalize(topic) == cleanTopic
        }
    }

    /**
     * fallback יציב עד שכל 391 התרגילים ימופו ידנית.
     *
     * חשוב:
     * זה לא המזהה הסופי האידיאלי, אבל הוא כבר מונע מצב שבו שם באנגלית/עברית מייצר key אחר.
     */
    private fun fallbackId(
        belt: Belt,
        hebrewTitle: String,
        topicKey: String?
    ): String {
        val topicPart = normalizeForKey(topicKey.orEmpty())
        val titlePart = normalizeForKey(hebrewTitle)

        return buildString {
            append("legacy")
            append("_")
            append(belt.id)
            if (topicPart.isNotBlank()) {
                append("_")
                append(topicPart)
            }
            append("_")
            append(titlePart)
        }
    }

    private fun normalizeForKey(raw: String): String {
        return normalize(raw)
            .replace(Regex("[^א-תa-z0-9]+"), "_")
            .trim('_')
    }

    fun normalize(raw: String): String {
        return raw
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")
            .replace("'", "'")
            .replace("’", "'")
            .replace("״", "\"")
            .replace("”", "\"")
            .replace("“", "\"")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }
}