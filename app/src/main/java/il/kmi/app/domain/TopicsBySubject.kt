package il.kmi.app.domain

import il.kmi.shared.domain.Belt

/**
 * נושא חוצה־חגורות (למשל "הגנות פנימיות", "בעיטות", "הגנות סכין" וכו').
 *
 * @param id          מזהה טכני קצר באנגלית (לוגיקה / ניווט)
 * @param titleHeb    שם בעברית להצגה באפליקציה
 * @param description תיאור קצר (אופציונלי)
 * @param belts       באילו חגורות הנושא הזה קיים
 * @param topicsByBelt מיפוי חגורה -> רשימת נושאים (strings) כפי שהם מופיעים כבר ב־ContentRepo / SubTopicRegistry
 */
enum class DefenseKind { INTERNAL, EXTERNAL, NONE }

data class SubjectTopic(
    val id: String,
    val titleHeb: String,
    val description: String = "",
    val belts: List<Belt>,
    val topicsByBelt: Map<Belt, List<String>>,
    val subTopicHint: String? = null,
    val parentId: String? = null,
    val subTopics: List<String> = emptyList(),
    val includeItemKeywords: List<String> = emptyList(),
    val requireAllItemKeywords: List<String> = emptyList(),
    val excludeItemKeywords: List<String> = emptyList()
) {
    // ✅ לשימוש עיצובי במסך הנושאים: פנימיות/חיצוניות (אגרופים/בעיטות יקבלו אותו צבע)
    val defenseKind: DefenseKind
        get() = when {
            id.startsWith("def_internal") -> DefenseKind.INTERNAL
            id.startsWith("def_external") -> DefenseKind.EXTERNAL
            else -> DefenseKind.NONE
        }
}

/**
 * רישום מרכזי של נושאים חוצי־חגורות.
 *
 * חשוב: השמות ב-topicsByBelt חייבים להיות זהים ל-topicTitle
 * שאיתם אתה עובד היום ב־ContentRepo / SubTopicRegistry,
 * כדי שנוכל בהמשך לפתוח מהם את התרגילים.
 */
object TopicsBySubjectRegistry {

    // 🔹 כאן שמים את כל הנושאים החוצי־חגורות
    val all: List<SubjectTopic> = listOf(
        // ================== עבודת ידיים – (כללי) ==================
        SubjectTopic(
            id = "hands_all",
            titleHeb = "עבודת ידיים",
            description = "עבודת אגרופים ומכות יד – ישרים, מגל, פיסת יד ועוד.",
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("עבודת ידיים"),
                Belt.ORANGE to listOf("עבודת ידיים")
            )
        ),

        // ================== עבודת ידיים – מרפק ==================
        SubjectTopic(
            id = "hands_elbow",
            titleHeb = "עבודת ידיים - מרפק",
            description = "תרגילי מרפק לפי תת־נושא.",
            belts = listOf(Belt.YELLOW),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("עבודת ידיים")
            ),
            subTopicHint = "מרפק"
        ),

        // ================== עבודת ידיים – פיסת יד ==================
        SubjectTopic(
            id = "hands_palm",
            titleHeb = "עבודת ידיים - פיסת יד",
            description = "תרגילי פיסת יד לפי תת־נושא.",
            belts = listOf(Belt.YELLOW),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("עבודת ידיים")
            ),
            subTopicHint = "פיסת יד"
        ),

        // ================== עבודת ידיים – אגרופים ישרים ==================
        SubjectTopic(
            id = "hands_straight_punches",
            titleHeb = "עבודת ידיים - אגרופים ישרים",
            description = "אגרופים ישרים לפי תת־נושא.",
            belts = listOf(Belt.YELLOW),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("עבודת ידיים")
            ),
            subTopicHint = "אגרופים ישרים"
        ),

        // ================== עבודת ידיים – מגל + סנוקרת ==================
        SubjectTopic(
            id = "hands_hook_uppercut",
            titleHeb = "עבודת ידיים - מגל וסנוקרת",
            description = "מגל וסנוקרת לפי תת־נושא.",
            belts = listOf(Belt.YELLOW),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("עבודת ידיים")
            ),
            subTopicHint = "מגל + סנוקרת" // ✅ בדיוק כמו ב־ContentRepo
        ),

        // ================== בלימות וגלגולים ==================
        SubjectTopic(
            id = "rolls_breakfalls",
            titleHeb = "בלימות וגלגולים",
            description = "בסיסיים ומתקדמים",
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE,
                Belt.GREEN,
                Belt.BLUE
            ),
            topicsByBelt = mapOf(
                // ✅ בצהובה/כתומה זה יושב תחת "כללי"
                Belt.YELLOW to listOf("כללי"),
                Belt.ORANGE to listOf("כללי"),

                // ✅ בירוקה/כחולה זה באמת topic נפרד
                Belt.GREEN  to listOf("בלימות וגלגולים"),
                Belt.BLUE   to listOf("בלימות וגלגולים")
            ),
            // ✅ מסנן רק בלימות/גלגולים מתוך "כללי"
            includeItemKeywords = listOf("בלימ", "גלגול")
        ),

        // ================== הגנות פנימיות – אגרופים ==================
        SubjectTopic(
            id = "def_internal_punches",
            titleHeb = "הגנות פנימיות – אגרופים",
            description = "הגנות פנימיות נגד אגרופים.",
            belts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("הגנות"),
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN  to listOf("הגנות"),
                Belt.BLUE   to listOf("הגנות"),
                Belt.BROWN  to listOf("הגנות"),
                Belt.BLACK  to listOf("הגנות")
            ),
            // ✅ AND: חובה לתפוס את התגית (עובד גם עם def_internal_punches וגם def:internal:punch)
            requireAllItemKeywords = listOf("def:internal:punch")
        ),

        // ================== הגנות פנימיות – בעיטות ==================
        SubjectTopic(
            id = "def_internal_kicks",
            titleHeb = "הגנות פנימיות – בעיטות",
            description = "הגנות פנימיות נגד בעיטות.",
            belts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("הגנות"),
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN  to listOf("הגנות"),
                Belt.BLUE   to listOf("הגנות"),
                Belt.BROWN  to listOf("הגנות"),
                Belt.BLACK  to listOf("הגנות")
            ),
            requireAllItemKeywords = listOf("def:internal:kick")
        ),

        // ================== הגנות חיצוניות – אגרופים ==================
        SubjectTopic(
            id = "def_external_punches",
            titleHeb = "הגנות חיצוניות – אגרופים",
            description = "הגנות חיצוניות נגד אגרופים.",
            belts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("הגנות"),
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN  to listOf("הגנות"),
                Belt.BLUE   to listOf("הגנות"),
                Belt.BROWN  to listOf("הגנות"),
                Belt.BLACK  to listOf("הגנות")
            ),
            requireAllItemKeywords = listOf("def:external:punch")
        ),

        // ================== הגנות חיצוניות – בעיטות ==================
        SubjectTopic(
            id = "def_external_kicks",
            titleHeb = "הגנות חיצוניות – בעיטות",
            description = "הגנות חיצוניות נגד בעיטות.",
            belts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("הגנות"),
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN  to listOf("הגנות"),
                Belt.BLUE   to listOf("הגנות"),
                Belt.BROWN  to listOf("הגנות"),
                Belt.BLACK  to listOf("הגנות")
            ),
            requireAllItemKeywords = listOf("def:external:kick")
        ),

        // ================== בעיטות ==================
        SubjectTopic(
            id = "kicks",
            titleHeb = "בעיטות",
            description = " מגל, הגנה, בניתור, צד",
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE,
                Belt.GREEN,
                Belt.BLUE,
                Belt.BROWN,
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf(
                    "בעיטות"
                ),
                Belt.ORANGE to listOf(
                    "בעיטות"
                ),
                Belt.GREEN to listOf(
                    "בעיטות"
                ),
                Belt.BLUE to listOf(
                    "בעיטות"
                ),
                Belt.BROWN to listOf(
                    "בעיטות"
                ),
                Belt.BLACK to listOf(
                    "בעיטות"
                )
            )
        ),

        // ================== חביקות גוף ==================
                // ❌ הוסר: "חביקות גוף" כקטגוריה ראשית
                // ✅ עבר לתת־נושא תחת "שחרורים" (ראה בהמשך)

        // ================== שחרורים ==================
        SubjectTopic(
            id = "releases",
            titleHeb = "שחרורים",
            description = "מתפיסות ידיים, מחניקות ומחביקות",
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE,
                Belt.GREEN,
                Belt.BLUE,
                Belt.BROWN,
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("שחרורים"),
                Belt.ORANGE to listOf("שחרורים"),
                Belt.GREEN  to listOf("שחרורים"),
                Belt.BLUE   to listOf("שחרורים"),
                Belt.BROWN  to listOf("שחרורים"),
                Belt.BLACK  to listOf("שחרורים")
            ),

            // ✅ נשארים כל תתי־הנושאים הקיימים (בלי "שחרור מחביקות גוף" כי זה נושא ילד)
            subTopics = listOf(
                "שחרור מתפיסות ידיים",
                "שחרור מחניקות",
                "שחרור מחביקות גוף",
                "שחרור חולצה / שיער"
            )
        ),

// ✅ NEW: תת־נושא "חביקות גוף" כילד של "שחרורים"
        SubjectTopic(
            id = "releases_body_hugs",
            parentId = "releases",
            titleHeb = "שחרור מחביקות גוף",
            description = "מלפנים/מאחור, ידיים חופשיות/נעולות",
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE,
                Belt.GREEN,
                Belt.BLUE,
                Belt.BROWN,
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("שחרורים"),
                Belt.ORANGE to listOf("שחרורים"),
                Belt.GREEN  to listOf("שחרורים"),
                Belt.BLUE   to listOf("שחרורים"),
                Belt.BROWN  to listOf("שחרורים"),
                Belt.BLACK  to listOf("שחרורים")
            ),
            includeItemKeywords = listOf("חביק", "חיבוק", "חיבוקים", "חביקות")
        ),

        // ================== אגרופים ==================
        SubjectTopic(
            id = "punches",
            titleHeb = "עבודת ידיים",
            description = "עבודת אגרופים ומכות יד – ישרים, מגל, פיסת יד ועוד.",
            // כרגע יש לנו \"עבודת ידיים\" בחגורות לבן/צהוב וכתום – שם יושבים כל האגרופים
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE
            ),
            topicsByBelt = mapOf(
                // חייב להיות 1:1 כמו הכותרת ב-ContentRepo
                Belt.YELLOW to listOf("עבודת ידיים"),
                Belt.ORANGE to listOf("עבודת ידיים")
            )
        ),

        // ================== הגנות סכין ==================
        SubjectTopic(
            id = "knife_defense",
            titleHeb = "הגנות סכין",
            description = "עקרונות עבודה והגנות מול איום ודקירות בסכין.",
            belts = listOf(
                Belt.GREEN,
                Belt.BLUE,
                Belt.BROWN,
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                // ✅ הכל יושב תחת "הגנות" (לא קיים topic בשם "הגנות סכין")
                Belt.GREEN to listOf("הגנות"),
                Belt.BLUE  to listOf("הגנות"),
                Belt.BROWN to listOf("הגנות"),
                Belt.BLACK to listOf("הגנות")
            ),
            // ✅ מסנן תתי־נושאים/שמות פריטים שקשורים לסכין
            subTopicHint = "סכין",
            // ✅ מונע זליגה למקל/אקדח (ובשחור גם תמ"ק)
            excludeItemKeywords = listOf("מקל", "אקדח", "תמ\"ק")
        ),

        // ================== הגנות מאיום אקדח ==================
        SubjectTopic(
            id = "gun_threat_defense",
            titleHeb = "הגנות מאיום אקדח",
            description = "הגנות ואילוצים כנגד איומי אקדח במצבי עמידה שונים.",
            belts = listOf(
                Belt.BROWN,
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                // ההגנות יושבות תחת הנושא "הגנות"
                Belt.BROWN to listOf("הגנות"),
                Belt.BLACK to listOf("הגנות")
            ),
            // ✅ תופס גם "אקדח" וגם "תמ\"ק" (בשחורה יש תת־נושא כזה)
            includeItemKeywords = listOf("אקדח", "תמ\"ק"),
            subTopicHint = "אקדח",
            excludeItemKeywords = listOf("סכין", "מקל")
        ),

        // ================== הגנות נגד מקל ==================
        SubjectTopic(
            id = "stick_defense",
            titleHeb = "הגנות נגד מקל",
            description = "עבודה מול תקיפות במקל – בלימות, כניסות וניטרול.",
            belts = listOf(
                Belt.GREEN,
                Belt.BROWN,
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                // גם כאן – כל התרגילים הרלוונטיים נמצאים תחת הנושא "הגנות"
                Belt.GREEN to listOf("הגנות"),
                Belt.BROWN to listOf("הגנות"),
                Belt.BLACK to listOf("הגנות")
            ),
            // ✅ מסנן תתי־נושאים/שמות פריטים של מקל
            subTopicHint = "מקל",
            // ✅ מונע זליגה לסכין/אקדח/תמ"ק
            excludeItemKeywords = listOf("סכין", "אקדח", "תמ\"ק")
        )

    ) // ✅ סוגר listOf(...) של all

    /** כל הנושאים (לפי נושא, לא לפי חגורה). */
    fun allSubjects(): List<SubjectTopic> =
        all.filter { it.parentId == null }

    fun subjectById(id: String): SubjectTopic? =
        all.firstOrNull { it.id == id }

    /** נושאים רלוונטיים לחגורה למסך הראשי (לא מחזירים ילדים). */
    fun subjectsForBelt(belt: Belt): List<SubjectTopic> =
        all.filter { it.parentId == null && belt in it.belts }

    /** ילדים (תתי־נושאים) של נושא הורה, מסוננים לפי חגורה. */
    fun subSubjectsFor(parentId: String, belt: Belt): List<SubjectTopic> =
        all.filter { it.parentId == parentId && belt in it.belts }

    // ------------------------------------------------------------------
    // ✅ NEW: לוגיקה אחידה לסינון/ספירה של תרגילים השייכים ל-SubjectTopic
    // ------------------------------------------------------------------

    /**
     * האם פריט (תרגיל) שייך לנושא SubjectTopic לפי כללי הסינון:
     * - subTopicHint (אם קיים)
     * - includeItemKeywords (OR)
     * - requireAllItemKeywords (AND)
     * - excludeItemKeywords
     *
     * @param itemTitle הכותרת/שם התרגיל (מומלץ "raw" אם יש def:...::)
     * @param subTopicTitle תת-נושא של הפריט (אם יש אצלך), אחרת null
     */
    // ------------------------------------------------------------------
    // ✅ FIX: התאמת פריטים ל-SubjectTopic בצורה שמבינה def tags בכל הפורמטים
    // ------------------------------------------------------------------

    private fun String.normHebLocal(): String = this
        .replace("\u200F", "")        // RLM
        .replace("\u200E", "")        // LRM
        .replace("\u00A0", " ")       // NBSP -> space
        .replace(Regex("[\u0591-\u05C7]"), "") // ניקוד
        .replace('\u05BE', '-')       // מקאף עברי ־
        .replace('\u2010', '-')
        .replace('\u2011', '-')
        .replace('\u2012', '-')
        .replace('\u2013', '-')
        .replace('\u2014', '-')
        .replace('\u2015', '-')
        .replace('\u2212', '-')
        .replace(Regex("\\s*-\\s*"), "-")
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()

    // תומך בשני פורמטים: "tag::name" וגם "name::tag"
    private fun splitTagAndName(raw: String): Pair<String?, String> {
        val s = raw.trim()
        if (!s.contains("::")) return null to s

        val left = s.substringBeforeLast("::").trim()
        val right = s.substringAfterLast("::").trim()

        fun isTag(x: String): Boolean =
            x.startsWith("def:", ignoreCase = true) || x.startsWith("def_", ignoreCase = true)

        return when {
            isTag(left)  -> left to right
            isTag(right) -> right to left
            else         -> null to right.ifBlank { s }
        }
    }

    // מנרמל def_* ל-def:* כדי ש-"def:internal:punch" יתפוס גם "def_internal_punches"
    private fun normalizeDefenseTag(tagRaw: String?): String {
        val t = tagRaw?.trim().orEmpty().lowercase()
        if (t.isBlank()) return ""
        if (t.startsWith("def:")) return t

        return when (t) {
            "def_internal_punches" -> "def:internal:punch"
            "def_external_punches" -> "def:external:punch"
            "def_internal_kicks"   -> "def:internal:kick"
            "def_external_kicks"   -> "def:external:kick"
            else -> t
        }
    }

    // מנרמל גם keyword שהגיע מה-SubjectTopic (כדי שתוכל לשים def_internal_punches או def:internal:punch)
    private fun normalizeKeyword(kw: String): String {
        val n = kw.normHebLocal()
        if (n.isBlank()) return ""
        return normalizeDefenseTag(n).normHebLocal()
    }

    /**
     * האם פריט (תרגיל) שייך ל-SubjectTopic לפי כללי הסינון:
     * - subTopicHint (אם קיים)
     * - includeItemKeywords (OR)
     * - requireAllItemKeywords (AND)
     * - excludeItemKeywords
     *
     * @param itemTitle הכותרת/שם התרגיל (רצוי raw – כולל def:...::)
     * @param subTopicTitle תת-נושא של הפריט (אם יש), אחרת null
     */
    fun SubjectTopic.matchesItem(
        itemTitle: String,
        subTopicTitle: String? = null
    ): Boolean {
        val (tagRaw, nameRaw) = splitTagAndName(itemTitle)

        val tag = normalizeDefenseTag(tagRaw).normHebLocal()
        val name = nameRaw.normHebLocal()
        val st = subTopicTitle?.normHebLocal().orEmpty()

        // "haystack" כולל גם tag וגם name וגם subTopicTitle כדי שכל הכללים יתפסו נכון
        val haystack = buildString {
            append(tag)
            append(' ')
            append(name)
            if (st.isNotBlank()) {
                append(' ')
                append(st)
            }
        }.trim()

        // 1) subTopicHint: אם מוגדר – חייב להתאים לתת-נושא או לשם התרגיל או לתג (למשל "פנימיות/חיצוניות")
        subTopicHint?.let { hintRaw ->
            val hint = hintRaw.normHebLocal()
            val ok = hint.isBlank() || haystack.contains(hint)
            if (!ok) return false
        }

        // normalize keywords פעם אחת
        val exclude = excludeItemKeywords.map(::normalizeKeyword).filter { it.isNotBlank() }
        val requireAll = requireAllItemKeywords.map(::normalizeKeyword).filter { it.isNotBlank() }
        val includeOr = includeItemKeywords.map(::normalizeKeyword).filter { it.isNotBlank() }

        // 2) exclude: אם אחד מהם מופיע – נפסל
        if (exclude.any { haystack.contains(it) }) return false

        // 3) requireAll (AND): חייב להכיל את כולן
        if (requireAll.isNotEmpty() && !requireAll.all { haystack.contains(it) }) return false

        // 4) include (OR): אם הרשימה לא ריקה – צריך לפחות אחת
        if (includeOr.isNotEmpty() && !includeOr.any { haystack.contains(it) }) return false

        return true
    }

    /**
     * סופר מתוך רשימה מוכנה של פריטים.
     * Pair(itemTitleRaw, subTopicTitle?)
     */
    fun SubjectTopic.countMatchingItems(
        items: List<Pair<String, String?>>
    ): Int {
        if (items.isEmpty()) return 0
        return items.count { (rawTitle, sub) -> matchesItem(rawTitle, sub) }
    }
}
