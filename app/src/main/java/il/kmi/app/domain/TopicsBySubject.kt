package il.kmi.app.domain

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.HardSectionsCatalog
import il.kmi.shared.domain.defense.DefenseKind as SharedDefenseKind

// ✅ Bridge זמני – כל הקוד באנדרואיד ימשיך לעבוד
typealias DefenseKind = SharedDefenseKind
/**
 * נושא חוצה־חגורות (למשל "הגנות פנימיות", "בעיטות", "הגנות סכין" וכו').
 *
 * @param id          מזהה טכני קצר באנגלית (לוגיקה / ניווט)
 * @param titleHeb    שם בעברית להצגה באפליקציה
 * @param description תיאור קצר (אופציונלי)
 * @param belts       באילו חגורות הנושא הזה קיים
 * @param topicsByBelt מיפוי חגורה -> רשימת נושאים (strings) כפי שהם מופיעים כבר ב־ContentRepo / SubTopicRegistry
 */


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

    // ✅ מקור אמת לשחרורים: רק מהתרגילים הקשיחים (HardSectionsCatalog.releases)
    //    ככה לא צריך לתחזק belts ידנית בעוד קובץ.
    private val releasesBelts: List<Belt> = run {
        val ordered = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)
        val used = linkedSetOf<Belt>()
        HardSectionsCatalog.releases.forEach { section ->
            section.beltGroups.forEach { bg ->
                if (bg.items.isNotEmpty()) used += bg.belt
            }
        }
        ordered.filter { it in used }
    }

    // ✅ map אוטומטי: חגורה -> "שחרורים" רק לחגורות שבאמת קיימות בקובץ הקשיח
    private val releasesTopicsByBelt: Map<Belt, List<String>> =
        releasesBelts.associateWith { listOf("שחרורים") }

    // 🔹 כאן שמים את כל הנושאים החוצי־חגורות
    val all: List<SubjectTopic> = listOf(

        // ================== עבודת ידיים – (כללי) ==================
        SubjectTopic(
            id = "hands_all",
            titleHeb = "עבודת ידיים",
            description = "מכות יד + מכות מרפק",
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE,
                Belt.GREEN
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("עבודת ידיים", "מכות ידיים", "מכות יד"),
                Belt.ORANGE to listOf("עבודת ידיים", "מכות יד", "מכות ידיים"),
                Belt.GREEN to listOf("מכות מרפק")
            ),
            subTopics = listOf(
                "מכות יד",
                "מכות מרפק"
            )
        ),

        // ================== מכות במקל / רובה ==================
        SubjectTopic(
            id = "hands_stick_rifle",
            titleHeb = "מכות במקל / רובה",
            description = "מכות במקל וברובה",
            belts = listOf(
                Belt.GREEN,
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                Belt.GREEN to listOf("מכות במקל / רובה"),
                Belt.BLACK to listOf(
                    "מכות במקל / רובה",
                    "מכות במקל קצר"
                )
            )
        ),

        // ================== בלימות וגלגולים ==================
        SubjectTopic(
            id = "rolls_breakfalls",
            titleHeb = "בלימות וגלגולים",
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE,
                Belt.GREEN,
                Belt.BLUE
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("כללי"),
                Belt.ORANGE to listOf("כללי"),
                Belt.GREEN to listOf("בלימות וגלגולים"),
                Belt.BLUE to listOf("בלימות וגלגולים")
            ),
            includeItemKeywords = listOf("בלימ", "גלגול")
        ),

        // ================== עמידת מוצא ==================
        SubjectTopic(
            id = "topic_ready_stance",
            titleHeb = "עמידת מוצא",
            description = "עמידות מוצא בסיסיות",
            belts = listOf(
                Belt.YELLOW
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("עמידת מוצא")
            )
        ),

        // ================== הכנה לעבודת קרקע ==================
        SubjectTopic(
            id = "topic_ground_prep",
            titleHeb = "עבודת קרקע",
            description = "הוצאת אגן, הרמת אגן ומוצא לעבודת קרקע",
            belts = listOf(
                Belt.YELLOW
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("עבודת קרקע")
            )
        ),

        // ================== קאוולר ==================
        SubjectTopic(
            id = "topic_kavaler",
            titleHeb = "קוואלר",
            description = "תרגילי קוואלר",
            belts = listOf(
                Belt.GREEN
            ),
            topicsByBelt = mapOf(
                Belt.GREEN to listOf("קוואלר")
            )
        ),

// ================== הגנות ==================
        SubjectTopic(
            id = "defenses",
            titleHeb = "הגנות",
            description = "הגנות פנימיות, חיצוניות, בעיטות, סכין, אקדח, מקל ומספר תוקפים",
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE,
                Belt.GREEN,
                Belt.BLUE,
                Belt.BROWN,
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("הגנות"),
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN to listOf("הגנות"),
                Belt.BLUE to listOf("הגנות"),
                Belt.BROWN to listOf("הגנות"),
                Belt.BLACK to listOf("הגנות")
            )
        ),

        // ================== הגנות פנימיות – אגרופים ==================
        SubjectTopic(
            id = "def_internal_punches",
            parentId = "defenses",
            titleHeb = "הגנות פנימיות – אגרופים",
            description = "הגנות פנימיות נגד אגרופים.",
            belts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("הגנות"),
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN to listOf("הגנות"),
                Belt.BLUE to listOf("הגנות"),
                Belt.BROWN to listOf("הגנות"),
                Belt.BLACK to listOf("הגנות")
            ),
            // ✅ AND: חובה לתפוס את התגית (עובד גם עם def_internal_punches וגם def:internal:punch)
            requireAllItemKeywords = listOf("def:internal:punch")
        ),

        // ================== הגנות פנימיות – בעיטות ==================
        SubjectTopic(
            id = "def_internal_kicks",
            parentId = "defenses",
            titleHeb = "הגנות פנימיות – בעיטות",
            description = "הגנות פנימיות נגד בעיטות.",
            belts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("הגנות"),
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN to listOf("הגנות"),
                Belt.BLUE to listOf("הגנות"),
                Belt.BROWN to listOf("הגנות"),
                Belt.BLACK to listOf("הגנות")
            ),
            requireAllItemKeywords = listOf("def:internal:kick")
        ),

        // ================== הגנות חיצוניות – אגרופים ==================
        SubjectTopic(
            id = "def_external_punches",
            parentId = "defenses",
            titleHeb = "הגנות חיצוניות – אגרופים",
            description = "הגנות חיצוניות נגד אגרופים.",
            belts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("הגנות"),
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN to listOf("הגנות"),
                Belt.BLUE to listOf("הגנות"),
                Belt.BROWN to listOf("הגנות"),
                Belt.BLACK to listOf("הגנות")
            ),
            requireAllItemKeywords = listOf("def:external:punch")
        ),

        // ================== הגנות חיצוניות – בעיטות ==================
        SubjectTopic(
            id = "def_external_kicks",
            parentId = "defenses",
            titleHeb = "הגנות חיצוניות – בעיטות",
            description = "הגנות חיצוניות נגד בעיטות.",
            belts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("הגנות"),
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN to listOf("הגנות"),
                Belt.BLUE to listOf("הגנות"),
                Belt.BROWN to listOf("הגנות"),
                Belt.BLACK to listOf("הגנות")
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
                Belt.YELLOW to listOf("בעיטות"),
                Belt.ORANGE to listOf("בעיטות"),
                Belt.GREEN to listOf("בעיטות"),
                Belt.BLUE to listOf("בעיטות"),
                Belt.BROWN to listOf("בעיטות"),
                Belt.BLACK to listOf("בעיטות")
            )
        ),

        // ================== שחרורים ==================
        SubjectTopic(
            id = "releases",
            titleHeb = "שחרורים",
            description = "מתפיסות ידיים, שיער, חולצה, מחניקות, מחביקות ונלסון",
            belts = releasesBelts,
            topicsByBelt = releasesTopicsByBelt,
            subTopics = listOf(
                "שחרור מתפיסות ידיים",
                "שחרור מתפיסות שיער",
                "שחרור מתפיסות חולצה",
                "שחרור מחניקות",
                "שחרור מחביקות",
                "שחרור מתפיסת נלסון"
            )
        ),

        // ✅ ילד: שחרור מתפיסות ידיים
        SubjectTopic(
            id = "releases_hands",
            parentId = "releases",
            titleHeb = "שחרור מתפיסות ידיים",
            description = "שחרורים מתפיסות ידיים וזרוע",
            belts = releasesBelts,
            topicsByBelt = releasesTopicsByBelt
        ),

        // ✅ ילד: שחרור מתפיסות שיער
        SubjectTopic(
            id = "releases_hair",
            parentId = "releases",
            titleHeb = "שחרור מתפיסות שיער",
            description = "שחרורים מתפיסות שיער",
            belts = releasesBelts,
            topicsByBelt = releasesTopicsByBelt
        ),

        // ✅ ילד: שחרור מתפיסות חולצה
        SubjectTopic(
            id = "releases_shirt",
            parentId = "releases",
            titleHeb = "שחרור מתפיסות חולצה",
            description = "שחרורים מתפיסות חולצה",
            belts = releasesBelts,
            topicsByBelt = releasesTopicsByBelt
        ),

        // ✅ ילד: שחרור מחניקות
        SubjectTopic(
            id = "releases_chokes",
            parentId = "releases",
            titleHeb = "שחרור מחניקות",
            description = "חניקות צואר מלפנים/מאחור",
            belts = releasesBelts,
            topicsByBelt = releasesTopicsByBelt,
            includeItemKeywords = listOf("חניק", "חניקה", "חניקות", "צואר"),
            excludeItemKeywords = listOf("תפיס", "אחיז", "חביק", "חולצ", "שיער")
        ),

        // ✅ ילד: שחרור מחביקות
        SubjectTopic(
            id = "releases_hugs",
            parentId = "releases",
            titleHeb = "שחרור מחביקות",
            description = "חביקות גוף / צואר / זרוע",
            belts = releasesBelts,
            topicsByBelt = releasesTopicsByBelt,
            subTopics = listOf(
                "חביקות גוף",
                "חביקות צואר",
                "חביקות זרוע"
            ),
            includeItemKeywords = listOf("חביק", "חיבוק", "חיבוקים", "חביקות"),
            excludeItemKeywords = listOf("חניק", "תפיס", "אחיז", "חולצ", "שיער")
        ),

        // ✅ ילד: שחרור מתפיסת נלסון
        SubjectTopic(
            id = "releases_nelson",
            parentId = "releases",
            titleHeb = "שחרור מתפיסת נלסון",
            description = "שחרור מתפיסת נלסון",
            belts = listOf(Belt.BLACK),
            topicsByBelt = mapOf(
                Belt.BLACK to listOf("שחרורים")
            )
        ),

        // ================== אגרופים ==================
        SubjectTopic(
            id = "punches",
            titleHeb = "עבודת ידיים",
            description = "עבודת אגרופים ומכות יד – ישרים, מגל, פיסת יד ועוד.",
            belts = listOf(
                Belt.YELLOW,
                Belt.ORANGE
            ),
            topicsByBelt = mapOf(
                Belt.YELLOW to listOf("עבודת ידיים"),
                Belt.ORANGE to listOf("עבודת ידיים")
            ),
            includeItemKeywords = listOf(
                "אגרוף",
                "פיסת",
                "מגל",
                "סנוקרת"
            )
        ),

        // ================== הגנות נגד סכין ==================
        SubjectTopic(
            id = "knife_defense",
            parentId = "defenses",
            titleHeb = "הגנות נגד סכין",
            description = "הגנה מאיום סכין והגנה נגד דקירה / שיסוף.",
            belts = listOf(
                Belt.ORANGE,
                Belt.GREEN,
                Belt.BLUE,
                Belt.BROWN,
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                Belt.ORANGE to listOf("הגנות"),
                Belt.GREEN to listOf("הגנות"),
                Belt.BLUE to listOf("הגנות"),
                Belt.BROWN to listOf("הגנות"),
                Belt.BLACK to listOf("הגנות")
            ),
            subTopicHint = "סכין",
            subTopics = listOf(
                "הגנה מאיום סכין",
                "הגנה נגד דקירה / שיסוף"
            ),
            excludeItemKeywords = listOf("מקל", "אקדח", "תמ\"ק", "רובה")
        ),

        // ================== הגנות עם רובה נגד דקירות סכין ==================
        SubjectTopic(
            id = "knife_rifle_defense",
            parentId = "defenses",
            titleHeb = "הגנות עם רובה נגד דקירות סכין",
            belts = listOf(
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                Belt.BLACK to listOf("הגנות")
            ),
            includeItemKeywords = listOf("רובה"),
            subTopicHint = "סכין"
        ),

// ================== הגנות נגד מספר תוקפים ==================
        SubjectTopic(
            id = "multiple_attackers_defense",
            parentId = "defenses",
            titleHeb = "הגנות נגד מספר תוקפים",
            belts = listOf(
                Belt.BLACK
            ),
            topicsByBelt = mapOf(
                Belt.BLACK to listOf("הגנות")
            ),
            includeItemKeywords = listOf("1 מקל", "2 תוקפים")
        ),

        // ================== הגנות מאיום אקדח ==================
        SubjectTopic(
            id = "gun_threat_defense",
            parentId = "defenses",
            titleHeb = "הגנות מאיום אקדח",
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
            parentId = "defenses",
            titleHeb = "הגנות נגד מקל",
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

    fun SubjectTopic.countMatchingItems(
        items: List<Pair<String, String?>>
    ): Int {
        if (items.isEmpty()) return 0
        return items.count { (rawTitle, sub) -> matchesItem(rawTitle, sub) }
    }
}
