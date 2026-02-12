package il.kmi.shared.domain

// קובץ נקי – בלי Compose – מתאים ל-KMP (commonMain)

object ContentRepo {

    // ===== חיפוש תרגילים לשימוש בסרגל החיפוש =====
    data class SearchHit(
        val id: String? = null,
        val title: String,
        val subtitle: String? = null
    )

    // --- NEW: מפתח יציב לפריט (Belt::Topic::SubTopic?::Item) + פענוח ---
    private const val SEP = "::"
    private const val ESC = "∷" // תו נדיר במקום "::" בתוך טקסטים
    private fun encPart(s: String) = s.replace(SEP, ESC)

    /** יוצר מפתח יציב לפריט ברשימות (לניווט/גלילה בין מסכים). */
    fun makeItemKey(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String?,
        itemTitle: String
    ): String =
        listOf(
            belt.id,
            encPart(topicTitle),
            encPart(subTopicTitle ?: ""),
            encPart(itemTitle)
        ).joinToString(SEP)

    data class ResolvedItem(
        val belt: Belt,
        val topicTitle: String,
        val subTopicTitle: String?,
        val itemTitle: String
    )

    /** מפענח מפתח יציב שקיבלנו מהחיפוש חזרה לשדות. */
    fun resolveItemKey(key: String): ResolvedItem? {
        val parts = key.split(SEP)
        if (parts.size != 4) return null
        val belt = Belt.fromId(parts[0]) ?: return null
        val topic = parts[1].replace(ESC, SEP)
        val sub = parts[2].replace(ESC, SEP).ifBlank { null }
        val item = parts[3].replace(ESC, SEP)
        return ResolvedItem(belt, topic, sub, item)
    }

    /** מחזיר את שם תת־הנושא שמכיל את הפריט (או null אם לא נמצא). */
    fun findSubTopicTitleForItem(
        belt: Belt,
        topicTitle: String,
        itemTitle: String
    ): String? {
        val wanted = itemTitle.normHeb()

        // ✅ FIX: מתעלמים מ"תת־נושא מזויף" שהוא בעצם אותו שם כמו topic
        val topicTrim = topicTitle.trim()
        val subs = getSubTopicsFor(belt, topicTitle)
            .filter { st -> st.title.trim().isNotBlank() && st.title.trim() != topicTrim }

        if (subs.isEmpty()) return null

        return subs.firstOrNull { st ->
            st.items.any { it.normHeb() == wanted }
        }?.title
    }

    /**
     * חיפוש תרגיל לפי שם (לשימוש בעוזר הקולי/הסברים).
     * מחזיר את שם התרגיל כפי שהוא מופיע במאגר, לאחר התאמה גמישה.
     */
    fun findExerciseByName(name: String): String? {
        val query = name.normHeb()
        if (query.isEmpty()) return null

        data.forEach { (_, beltContent) ->
            beltContent.topics.forEach { topic ->
                topic.subTopics.forEach { st ->
                    st.items.firstOrNull { it.normHeb().contains(query) }?.let { item ->
                        return item
                    }
                }
                topic.items.firstOrNull { it.normHeb().contains(query) }?.let { item ->
                    return item
                }
            }
        }
        return null
    }

    // ---------------------------------------------------------
    // מודלים פנימיים לתוכן
    // ---------------------------------------------------------
    data class SubTopic(
        val title: String,
        val items: List<String>
    )

    data class Topic(
        val title: String,
        val items: List<String> = emptyList(),
        val subTopics: List<SubTopic> = emptyList()
    )

    data class BeltContent(
        val belt: Belt,
        val topics: List<Topic>
    )

    // ---------------------------------------------------------
    // נרמול טקסט עברי (משותף לכל הפלטפורמות)
    // ---------------------------------------------------------
    // ✅ במקום private normHeb מקומי – משתמשים ב-Canonical של shared
    private fun String.normHeb(): String =
        il.kmi.shared.domain.content.Canonical.run { this@normHeb.normHeb() }

    private fun splitDefenseTagAndName(raw: String): Pair<String?, String> {
        val s = raw.trim()
        if (!s.contains("::")) return null to s

        val left = s.substringBeforeLast("::").trim()
        val right = s.substringAfterLast("::").trim()

        fun isTag(x: String): Boolean =
            x.startsWith("def:", ignoreCase = true) || x.startsWith("def_", ignoreCase = true)

        return when {
            isTag(left) -> left to right
            isTag(right) -> right to left
            else -> null to s.substringAfterLast("::").ifBlank { s }
        }
    }

    private fun normalizeDefenseTag(tagRaw: String?): String {
        val t = tagRaw?.trim().orEmpty().lowercase()
        if (t.isBlank()) return ""
        if (t.startsWith("def:")) return t

        return when (t) {
            "def_external_punches" -> "def:external:punch"
            "def_internal_punches" -> "def:internal:punch"
            "def_external_kicks" -> "def:external:kick"
            "def_internal_kicks" -> "def:internal:kick"
            else -> t
        }
    }

    // אם אין tag – נסיק אותו מתוך שם התרגיל (בעיקר לירוקה שבה חלק מהשורות בלי def_*).
    private fun inferDefenseTagFromDisplay(displayName: String): String {
        val d = displayName.normHeb()

        val isExternal = d.contains("חיצונ")
        val isInternal = d.contains("פנימ")
        val isKick = d.contains("בעיט")
        val isPunch = d.contains("אגרו") || d.contains("אגרוף") || d.contains("מכות אגרוף")

        return when {
            isExternal && isKick -> "def:external:kick"
            isInternal && isKick -> "def:internal:kick"
            isExternal && isPunch -> "def:external:punch"
            isInternal && isPunch -> "def:internal:punch"
            else -> ""
        }
    }

    // ---------------------------------------------------------
    // שליפה בטוחה של Topic לפי חגורה וכותרת (קודם התאמה מדויקת)
    // ---------------------------------------------------------
    private fun findTopicSafe(belt: Belt, topicTitle: String): Topic? {
        val topics = data[belt]?.topics ?: return null
        val wn = topicTitle.normHeb()

        // 1) ניסיון התאמה מדויקת אחרי נרמול
        topics.firstOrNull { it.title.normHeb() == wn }?.let { return it }

        // 2) fallback: התאמה רופפת רק אם יש בדיוק פגיעה יחידה
        val candidates = topics.filter { t ->
            val tn = t.title.normHeb()
            tn.startsWith(wn) || wn.startsWith(tn)
        }
        return candidates.singleOrNull()
    }

    // ====== ירוקה: תתי־נושאים ל"הגנות" מהמאגר, עם עטיפה אם יש רק items ======
    private val greenDefenseSubTopicsInternal: List<SubTopic> by lazy {
        val t = findTopicSafe(Belt.GREEN, "הגנות")
        when {
            t?.subTopics?.isNotEmpty() == true -> t.subTopics
            t?.items?.isNotEmpty() == true -> listOf(SubTopic(title = t.title, items = t.items))
            else -> emptyList()
        }
    }

    // ---------------------------------------------------------
// תתי־נושאים לפי חגורה ונושא
// כלל הזהב: אם יש subTopics – מחזירים אותם. אם יש items – עוטפים ל-SubTopic יחיד.
// ---------------------------------------------------------
    fun getSubTopicsFor(belt: Belt, topicTitle: String): List<SubTopic> {
        val topic = findTopicSafe(belt, topicTitle) ?: return emptyList()

        val reqN = topicTitle.normHeb()
        val realIsDefense = topic.title.normHeb() == "הגנות".normHeb()

        // ✅ NEW: תומך בנושאים "מדומים" שה-UI שולח: "הגנות פנימיות" / "הגנות חיצוניות"
        // בפועל התוכן נמצא תחת Topic "הגנות" ומסונן לפי tag/מילות מפתח.
        if (realIsDefense && (reqN.contains("פנימ") || reqN.contains("חיצונ"))) {
            val hint = when {
                reqN.contains("פנימ") -> "הגנות פנימיות"
                else -> "הגנות חיצוניות"
            }
            val items = getDefenseItemsFiltered(
                belt = belt,
                topicTitle = "הגנות",
                subTopicHint = hint
            )
            return listOf(SubTopic(title = hint, items = items))
        }

        val isDefense = realIsDefense && reqN.startsWith("הגנות".normHeb())

        // ✅ ירוקה – טיפול מיוחד כמו שהיה לך
        if (isDefense && belt == Belt.GREEN) return greenDefenseSubTopicsInternal

        // ✅ שחורה – קח בדיוק מה-data (אם אין subTopics אבל יש items, נעטוף כדי לא להחזיר ריק)
        if (isDefense && belt == Belt.BLACK) {
            val t = findTopicSafe(Belt.BLACK, "הגנות")
            return when {
                t?.subTopics?.isNotEmpty() == true -> t.subTopics
                t?.items?.isNotEmpty() == true -> listOf(SubTopic(title = t.title, items = t.items))
                else -> emptyList()
            }
        }

        if (topic.subTopics.isNotEmpty()) return topic.subTopics
        if (topic.items.isNotEmpty()) return listOf(SubTopic(title = topic.title, items = topic.items))
        return emptyList()
    }

    fun getSubTopicTitles(belt: Belt, topicTitle: String): List<String> =
        getSubTopicsFor(belt, topicTitle).map { it.title }

    // ---------------------------------------------------------
    // כל התרגילים השטוחים בנושא, עם תמיכה בסינון לפי תת־נושא
    // כאשר subTopicTitle סופק ואין התאמה מדויקת — מחזירים ריק.
    // ---------------------------------------------------------
    fun getAllItemsFor(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String? = null
    ): List<String> {
        val subs = getSubTopicsFor(belt, topicTitle)
        if (subs.isEmpty()) return emptyList()

        val isReleasesTopic = topicTitle.normHeb() == "שחרורים".normHeb()

        fun normalizeReleasesPick(wn: String): String {
            // מחזיר "שורש" שמסווג את הבחירה, כדי לעבוד בין חגורות עם כותרות שונות
            return when {
                wn.contains("תפיס") -> "תפיס"
                wn.contains("חניק") -> "חניק"
                wn.contains("חביק") || wn.contains("צוואר") || wn.contains("גוף") -> "חביק"
                wn.contains("חולצ") -> "חולצ"
                wn.contains("שיער") -> "שיער"
                else -> wn
            }
        }

        fun chooseSubTopicForReleases(wanted: String): SubTopic? {
            val wn = wanted.normHeb()
            val key = normalizeReleasesPick(wn)

            // 1) התאמה מדויקת
            subs.firstOrNull { it.title.normHeb() == wn }?.let { return it }

            // 2) התאמה "מכילה" (שחרור/שחרורים, יחיד/רבים וכו')
            subs.firstOrNull { st ->
                val tn = st.title.normHeb()
                tn.contains(wn) || wn.contains(tn)
            }?.let { return it }

            // 3) התאמה לפי שורש סיווג (תפיס/חניק/חביק/חולצ/שיער)
            return subs.firstOrNull { st ->
                st.title.normHeb().contains(key)
            }
        }

        // ✅ NEW: פילטר קשוח לשחרורים כדי למנוע "זליגה" של פריטים לא קשורים בתוך אותו SubTopic
        fun filterReleasesItems(wantedSubTitle: String, items: List<String>): List<String> {
            val w = wantedSubTitle.normHeb()

            fun hasAny(s: String, vararg keys: String): Boolean =
                keys.any { k -> k.isNotBlank() && s.contains(k) }

            fun hasNone(s: String, vararg keys: String): Boolean =
                keys.all { k -> k.isBlank() || !s.contains(k) }

            return items.filter { raw ->
                val t = raw.normHeb()

                when {
                    // ✅ שחרורים מתפיסות ידיים — רק תפיסות יד (ומוציא חניקות/חביקות/חולצה/שיער)
                    w.contains("תפיס") && w.contains("יד") -> {
                        val okGrab = hasAny(t, "תפיס", "אחיז", "אגודל", "פרק") && hasAny(t, "יד", "ידיים", "כף")
                        val notOther = hasNone(t, "חניק", "חניקה", "חביק", "חביקה", "צוואר", "שיער", "חולצ", "חולצה")
                        okGrab && notOther
                    }

                    // ✅ שחרורים מחניקות
                    w.contains("חניק") -> {
                        hasAny(t, "חניק", "חניקה") && hasNone(t, "חביק", "חביקה", "חולצ", "חולצה", "שיער", "תפיס")
                    }

                    // ✅ שחרורים מחביקות (כולל צוואר/גוף)
                    w.contains("חביק") || w.contains("צוואר") || w.contains("גוף") -> {
                        hasAny(t, "חביק", "חביקה", "צוואר", "גוף") && hasNone(t, "חניק", "חניקה", "חולצ", "חולצה", "שיער", "תפיס")
                    }

                    // ✅ שחרורים מחולצה / שיער
                    w.contains("חולצ") || w.contains("שיער") -> {
                        hasAny(t, "חולצ", "חולצה", "שיער") && hasNone(t, "חניק", "חניקה", "חביק", "חביקה", "תפיס")
                    }

                    else -> true
                }
            }
        }

        fun normalizeAndSort(items: List<String>): List<String> {
            // ב"שחרורים" אנחנו גם מנקים כפילויות וגם ממיינים עקבי
            // (הכפילויות אצלך קיימות בפועל במאגר, למשל בצהובה)
            val distinct = items.distinctBy { it.normHeb() }
            return if (isReleasesTopic) {
                distinct.sortedBy { it.normHeb() }
            } else {
                distinct
            }
        }

        subTopicTitle?.let { wanted ->
            val pickedRaw = if (isReleasesTopic) {
                chooseSubTopicForReleases(wanted)?.items ?: emptyList()
            } else {
                val wn = wanted.normHeb()
                subs.firstOrNull { it.title.normHeb() == wn }?.items ?: emptyList()
            }

            // ✅ NEW: רק לשחרורים — מסננים חזק לפי תת־הנושא שנבחר
            val picked = if (isReleasesTopic) filterReleasesItems(wanted, pickedRaw) else pickedRaw

            return normalizeAndSort(picked)
        }

        // ללא subTopicTitle → כל התרגילים בנושא
        val all = subs.flatMap { it.items }
        return normalizeAndSort(all)
    }

    // ---------------------------------------------------------
    // שליפה לתת־נושא ספציפי (התאמה מדויקת אחרי נרמול) – ציבורי
    // ---------------------------------------------------------
    fun getItemsForExactSubTopic(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String
    ): List<String> = getAllItemsFor(belt, topicTitle, subTopicTitle)

    // ---------------------------------------------------------
    // שליפה לנושא “הגנות” עם סינון לפי תתי־נושאים + מילות מפתח
    // ---------------------------------------------------------
    fun getDefenseItemsFiltered(
        belt: Belt,
        topicTitle: String = "הגנות",
        subTopicHint: String? = null,
        includeItemKeywords: List<String> = emptyList(),      // OR
        requireAllItemKeywords: List<String> = emptyList(),   // AND
        excludeItemKeywords: List<String> = emptyList()
    ): List<String> {

        fun normKw(x: String): String {
            val n = x.normHeb()
            if (n.isBlank()) return ""
            return normalizeDefenseTag(n).normHeb()
        }

        val inc = includeItemKeywords.map { normKw(it) }.filter { it.isNotBlank() }
        val req = requireAllItemKeywords.map { normKw(it) }.filter { it.isNotBlank() }
        val exc = excludeItemKeywords.map { normKw(it) }.filter { it.isNotBlank() }

        // ✅ אם המשתמש בחר תת־נושא אמיתי (כפתור UI) – ננעל אליו
        val subs = getSubTopicsFor(belt, topicTitle)
        val exactSubItems: List<String>? = subTopicHint
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { hint ->
                val hn = hint.normHeb()
                subs.firstOrNull { st -> st.title.normHeb() == hn }?.items
            }

        val allItems: List<String> = exactSubItems ?: getAllItemsFor(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = null
        )

        fun matchesHint(hint: String, display: String, tag: String): Boolean {
            val isExternalHint = hint.contains("חיצונ")
            val isInternalHint = hint.contains("פנימ")

            return when {
                isExternalHint -> tag.contains("external")
                isInternalHint -> tag.contains("internal")
                else -> display.contains(hint) || tag.contains(hint)
            }
        }

        val hinted = if (exactSubItems != null) {
            allItems
        } else if (!subTopicHint.isNullOrBlank()) {
            val hint = subTopicHint.normHeb()
            val filtered = allItems.filter { raw ->
                val (tagRaw, displayName) = splitDefenseTagAndName(raw)
                val display = displayName.trim().normHeb()

                val normalized = normalizeDefenseTag(tagRaw).normHeb()
                val inferred = inferDefenseTagFromDisplay(display).normHeb()
                val tag = (if (normalized.isNotBlank()) normalized else inferred).normHeb()

                matchesHint(hint, display, tag)
            }

            if (filtered.isEmpty() && (hint.contains("חיצונ") || hint.contains("פנימ"))) allItems else filtered
        } else {
            allItems
        }

        return hinted.mapNotNull { raw ->
            val (tagRaw, displayName) = splitDefenseTagAndName(raw)
            val t = displayName.trim().normHeb()

            val normalized = normalizeDefenseTag(tagRaw).normHeb()
            val inferred = inferDefenseTagFromDisplay(t).normHeb()
            val tag = if (normalized.isNotBlank()) normalized else inferred

            val passInc = inc.isEmpty() || inc.any { kw -> t.contains(kw) || tag.contains(kw) }
            val passReq = req.isEmpty() || req.all { kw -> t.contains(kw) || tag.contains(kw) }
            val passExc = exc.isEmpty() || exc.none { kw -> t.contains(kw) || tag.contains(kw) }

            if (passInc && passReq && passExc) displayName.trim() else null
        }
    }

    // ---------------------------------------------------------
    // חיפוש (KMP-safe)
    // ---------------------------------------------------------
    fun searchExercises(query: String): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val qi = q.lowercase()
        val hits = mutableListOf<SearchHit>()

        fun String?.norm(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
        fun matches(s: String?): Boolean = s?.lowercase()?.contains(qi) == true

        fun addHit(title: String?, subtitle: String?, id: String? = null) {
            val t = title.norm() ?: return
            val sub = subtitle?.trim().orEmpty().ifBlank { null }
            if (matches(t) || matches(sub)) {
                hits += SearchHit(id = id, title = t, subtitle = sub)
            }
        }

        data.forEach { (belt, beltContent) ->
            beltContent.topics.forEach { topic ->
                if (matches(topic.title)) addHit(topic.title, belt.heb, null)

                topic.subTopics.forEach { st ->
                    if (matches(st.title)) addHit(st.title, "${belt.heb} • ${topic.title}", null)
                    st.items.forEach { item ->
                        if (matches(item)) {
                            val key = makeItemKey(belt, topic.title, st.title, item)
                            addHit(item, "${belt.heb} • ${topic.title} • ${st.title}", key)
                        }
                    }
                }

                topic.items.forEach { item ->
                    if (matches(item)) {
                        val key = makeItemKey(belt, topic.title, null, item)
                        addHit(item, "${belt.heb} • ${topic.title}", key)
                    }
                }
            }
        }

        return hits
            .distinctBy { (it.id ?: it.title).lowercase() }
            .sortedBy { it.title }
    }

    // ---------------------------------------------------------
    // DATA
    // ---------------------------------------------------------

    // ⚠️ חשוב: ב-object אי אפשר להשתמש ב-vals שמוגדרים בהמשך הקובץ בתוך init של val רגיל.
    // לכן עושים lazy כדי שכל ה-topics כבר יהיו מאותחלים בזמן הגישה הראשונה ל-data.
    val data: Map<Belt, BeltContent> by lazy {
        mapOf(
            Belt.WHITE  to BeltContent(Belt.WHITE,  emptyList()),
            Belt.YELLOW to BeltContent(Belt.YELLOW, yellowBeltTopics),
            Belt.ORANGE to BeltContent(Belt.ORANGE, orangeBeltTopics),
            Belt.GREEN  to BeltContent(Belt.GREEN,  greenBeltTopics),
            Belt.BLUE   to BeltContent(Belt.BLUE,   blueBeltTopics),
            Belt.BROWN  to BeltContent(Belt.BROWN,  brownBeltTopics),
            Belt.BLACK  to BeltContent(Belt.BLACK,  blackBeltTopics)
        )
    }

    // ---------------- חגורה צהובה ----------------
    private val yellowBeltTopics = listOf(
        Topic(
            "כללי",
            listOf(
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
        Topic(
            "עמידת מוצא",
            listOf(
                "עמידת מוצא רגילה",
                "עמידת מוצא להגנות פנימיות",
                "עמידת מוצא להגנות חיצוניות",
                "עמידת מוצא צידית",
                "עמידת מוצא כללית מספר 1",
                "עמידת מוצא כללית מספר 2"
            )
        ),
        Topic(
            title = "עבודת ידיים",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    "מרפק",
                    listOf(
                        "מכת מרפק אופקית לאחור",
                        "מכת מרפק אופקית לצד",
                        "מכת מרפק אופקית לפנים",
                        "מכת מרפק לאחור",
                        "מכת מרפק לאחור למעלה",
                        "מכת מרפק אנכי למטה",
                        "מכת מרפק אנכי למעלה"
                    )
                ),
                SubTopic(
                    "פיסת יד",
                    listOf(
                        "מכת פיסת יד שמאל לפנים",
                        "מכת פיסת יד ימין לפנים",
                        "מכת פיסת יד שמאל-ימין לפנים",
                        "מכת פיסת יד שמאל-ימין-שמאל לפנים",
                        "מכת פיסת יד מהצד"
                    )
                ),
                SubTopic(
                    "אגרופים ישרים",
                    listOf(
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
                        "אגרוף ימין למטה בהגנה"
                    )
                ),
                SubTopic(
                    "מגל + סנוקרת",
                    listOf(
                        "מכת מגל שמאל",
                        "מכת מגל ימין",
                        "מכת מגל למטה ולמעלה בהתחלפות",
                        "מכת סנוקרת שמאל",
                        "מכת סנוקרת ימין"
                    )
                )
            )
        ),
        Topic(
            title = "שחרורים",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    "שחרורים מתפיסות ידיים",
                    listOf(
                        "שחרור מתפיסת יד מול יד",
                        "שחרור מתפיסת יד נגדית",
                        "שחרור מתפיסת שתי ידיים למטה",
                        "שחרור מתפיסת שתי ידיים למעלה"
                    )
                ),
                SubTopic(
                    "שחרורים מחניקות",
                    listOf(
                        "מניעת התקרבות תוקף",
                        "מניעת חניקה",
                        "שחרור מחניקה מלפנים בכף היד",
                        "שחרור מחניקה מאחור במשיכה"
                    )
                )
            )
        ),
        Topic(
            "הכנה לעבודת קרקע",
            listOf(
                "הוצאת אגן",
                "הרמת אגן והפניית גוף לכיון ההפלה",
                "מוצא לעבודת קרקע"
            )
        ),
        Topic(
            "בעיטות",
            listOf(
                "בעיטה ישירה למפסעה",
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
        Topic(
            "הגנות",
            listOf(
                "def:external:punch::הגנות חיצוניות - רפלקסיבית 360 מעלות",
                "def:internal:punch::הגנות פנימיות - הגנה פנימית רפלקסיבית",
                "def:internal:punch::הגנות פנימיות - הגנה פנימית נגד ימין בכף יד שמאל",
                "def:internal:punch::הגנות פנימיות - הגנה פנימית נגד שמאל בכף יד ימין",
                "def:internal:kick::הגנות פנימיות - הגנה פנימית נגד בעיטה רגילה למפסעה"
            )
        )
    )

    // ---------------- חגורה כתומה ----------------
    private val orangeBeltTopics = listOf(
        Topic(
            "כללי",
            listOf(
                "גלגול לאחור צד ימין",
                "גלגול לאחור צד שמאל",
                "גלגול לפנים צד שמאל",
                "שילובי ידיים רגליים",
                "בלימה לצד ימין",
                "בלימה לצד שמאל"
            )
        ),
        Topic(
            "עבודת ידיים",
            listOf(
                "מכת גב יד בהצלפה",
                "מכת גב יד בהצלפה בסיבוב",
                "מכת פטיש",
                "מכת פטיש מהצד"
            )
        ),
        Topic(
            "בעיטות",
            listOf(
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
        Topic(
            title = "שחרורים",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    "שחרור מתפיסות ידיים",
                    listOf(
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
                SubTopic(
                    "שחרורים מתפיסות חולצה",
                    listOf(
                        "שחרור חולצה - בריח על האגודל",
                        "שחרור חולצה - מכת פרקי אצבעות",
                        "שחרור חולצה - שתי ידיים"
                    )
                ),
                SubTopic(
                    "שחרורים מתפיסות שיער",
                    listOf(
                        "שחרור מתפיסת שיער מלפנים",
                        "שחרור מתפיסת שיער מלפנים בשתי ידיים"
                    )
                ),
                SubTopic(
                    "שחרורים מתפיסות צוואר וגוף",
                    listOf(
                        "שחרור חביקת צואר מלפנים",
                        "שחרור מחביקה פתוחה מלפנים",
                        "שחרור מחביקה פתוחה מאחור",
                        "שחרור מחביקת צואר מהצד בשכיבה",
                        "שחרור מחביקת צואר ויד מהצד בשכיבה"
                    )
                ),
                SubTopic(
                    "שחרורים מחניקות",
                    listOf(
                        "שחרור מחניקה מלפנים בדחיפה",
                        "שחרור מחניקה מאחור בדחיפה",
                        "שחרור מחניקה מהצד - מרחוק",
                        "שחרור מחניקה מהצד - מקרוב",
                        "שחרור מחנקה מהצד בשכיבה"
                    )
                )
            )
        ),
        Topic(
            title = "הגנות",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    "הגנות נגד מכות ישרות",
                    listOf(
                        "הגנות נגד מכות עם הטיות גוף",
                        "def:internal:punch::הגנות פנימיות - הגנה פנימית נגד שמאל עם מרפק",
                        "def:internal:punch::הגנות פנימיות - הגנה פנימית נגד מכות ישרות למטה",
                        "הגנה נגד שמאל-ימין - אגרוף מהופך",
                        "הגנה נגד שמאל-ימין - הטייה לאחור",
                        "הגנה נגד שמאל-ימין (כמו חיצוניות)"
                    )
                ),
                SubTopic(
                    "הגנות חיצוניות נגד מכות",
                    listOf(
                        "def:external:punch::הגנות חיצוניות - הגנה חיצונית מס' 1",
                        "def:external:punch::הגנות חיצוניות - הגנה חיצונית מס' 2",
                        "def:external:punch::הגנות חיצוניות - הגנה חיצונית מס' 3",
                        "def:external:punch::הגנות חיצוניות - הגנה חיצונית מס' 4",
                        "def:external:punch::הגנות חיצוניות - הגנה חיצונית מס' 5",
                        "def:external:punch::הגנות חיצוניות - הגנה חיצונית מס' 6"
                    )
                ),
                SubTopic(
                    "הכנה לעבודת קרקע",
                    listOf("הגנה נגד אגרופים בשכיבה")
                ),
                SubTopic(
                    title = "הגנות חיצוניות נגד מכות מהצד",
                    items = listOf(
                        "def:external:punch::הגנה חיצונית נגד מכה גבוהה מהצד - התוקף בצד שמאל",
                        "def:external:punch::הגנה חיצונית נגד מכה גבוהה מהצד לעורף - התוקף בצד שמאל",
                        "def:external:punch::הגנה חיצונית נגד מכה מהצד לגב - התוקף בצד שמאל",
                        "def:external:punch::הגנה חיצונית נגד מכה גבוהה מהצד - התוקף בצד ימין",
                        "def:external:punch::הגנה חיצונית נגד מכה מהצד לגרון - התוקף בצד ימין",
                        "def:external:punch::הגנה חיצונית נגד מכה מהצד לבטן - התוקף בצד ימין"
                    )
                ),
                SubTopic(
                    "הגנות נגד בעיטות",
                    listOf(
                        "def:external:kick::הגנה חיצונית נגד בעיטה רגילה",
                        "הגנה נגד בעיטת ברך",
                        "הגנה נגד בעיטה רגילה - עצירה ברגל הקדמית",
                        "הגנה נגד בעיטה רגילה - עצירה ברגל האחורית",
                        "def:external:kick::הגנות חיצוניות - הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בימין",
                        "def:external:kick::הגנות חיצוניות - הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בשמאל",
                        "הגנה נגד בעיטת מגל לפנים באמות הידיים",
                        "הגנות חיצוניות - הגנה חיצונית נגד בעיטת מגל לפנים - אגרוף בימין::def:external:kick",
                        "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל האחורית",
                        "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל הקדמית",
                        "בעיטת עצירה נגד בעיטה לצד"
                    )
                ),
                SubTopic(
                    "הגנות נגד סכין",
                    listOf(
                        "הגנות יד רפלקסיביות נגד דקירות רגילות",
                        "הגנות יד רפלקסיביות נגד דקירות מזרחיות",
                        "הגנות יד רפלקסיביות נגד דקירה ישרה"
                    )
                )
            )
        )
    )

    // ------------------------------------- חגורה ירוקה ------------------------
    private val greenBeltTopics = listOf(
        Topic(
            "בלימות וגלגולים",
            listOf(
                "בלימה לאחור מגובה",
                "בלימה לצד כהכנה לגזיזות",
                "גלגול לפנים ובלימה לאחור – שמאל",
                "גלגול לפנים ובלימה לאחור – ימין",
                "גלגול לפנים ולאחור – ימין",
                "גלגול לפנים ולאחור – שמאל",
                "גלגול ביד אחת - ימין",
                "גלגול ביד אחת - שמאל",
                "גלגול לפנים – קימה לפנים"
            )
        ),
        Topic(
            title = "קאוולר",
            items = listOf(
                "קאוולר - הליכה לאחור",
                "קאוולר נגד התנגדות - הליכה לפנים",
                "קאוולר - אגודלים",
                "קאוולר – מרפק"
            )
        ),
        Topic("מכות מרפק", listOf("מכת מרפק נגד קבוצה")),
        Topic("מכות במקל / רובה", listOf("התקפה עם מקל לנקודות תורפה")),
        Topic(
            "בעיטות",
            listOf(
                "בעיטה רגילה ובעיטת מגל באותה רגל",
                "בעיטת מגל לאחור בשיכול אחורי",
                "בעיטה לצד בסיבוב",
                "בעיטת סטירה חיצונית"
            )
        ),
        Topic(
            "שחרורים",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    "שחרור מתפיסות",
                    listOf(
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
                SubTopic(
                    "שחרור מחביקות",
                    listOf(
                        "שחרור מחביקה פתוחה מהצד",
                        "שחרור מחביקה פתוחה מלפנים בהרמה",
                        "שחרור מחביקה סגורה מהצד",
                        "שחרור מחביקה סגורה מלפנים בהרמה",
                        "שחרור מחביקה פתוחה מאחור בהרמה",
                        "שחרור מחביקה פתוחה מאחור עם תפיסת אצבע",
                        "שחרור מחביקה פתוחה מאחור - בריח על האצבעות"
                    )
                )
            )
        ),
        Topic(
            title = "הגנות",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    "הגנות נגד מקל",
                    listOf(
                        "הגנה נגד מקל - צד חי",
                        "הגנה נגד מקל - צד מת"
                    )
                ),
                SubTopic(
                    "הגנות נגד מכות אגרוף",
                    listOf(
                        "הגנות חיצוניות - הגנה חיצונית נגד ימין באגרוף מהופך",
                        "def_external_punches::הגנות חיצוניות - הגנה חיצונית נגד שמאל",
                        "def_external_punches::הגנות חיצוניות - הגנה חיצונית נגד שמאל בהתקדמות",
                        "הגנות פנימיות - הגנה פנימית נגד ימין באמת שמאל::def_internal_punches",
                        "def_internal_punches::הגנות פנימיות - הגנה פנימית נגד שמאל באמת שמאל"
                    )
                ),
                SubTopic(
                    "הגנה נגד בעיטות",
                    listOf(
                        "הגנה נגד בעיטה רגילה - בעיטה לצד",
                        "הגנה נגד בעיטה רגילה – טיימינג לצד חי",
                        "הגנות חיצוניות - הגנה חיצונית באמת שמאל נגד בעיטה רגילה::def_external_kicks",
                        "הגנה נגד בעיטת מגל לפנים – בעיטה לצד",
                        "הגנה נגד בעיטת מגל נמוכה",
                        "הגנה נגד בעיטת מגל לאחור - בעיטה בימין",
                        "הגנה נגד בעיטת מגל לאחור - בעיטה שמאל",
                        "הגנה נגד בעיטת מגל לאחור - אגרוף שמאל",
                        "הגנה נגד בעיטת מגל לאחור בסיבוב – בעיטה",
                        "def_external_kicks::הגנות חיצוניות - הגנה חיצונית באמת ימין נגד בעיטה לצד",
                        "הגנות חיצוניות - הגנה חיצונית באמת שמאל נגד בעיטה לצד::def_external_kicks",
                        "הגנה נגד בעיטת לצד בעיטת סטירה חיצונית"
                    )
                ),
                SubTopic(
                    "הגנות מאיום סכין",
                    listOf(
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
                )
            )
        )
    )

    // ---------------- חגורה כחולה ----------------
    private val blueBeltTopics = listOf(
        Topic(
            "בלימות וגלגולים",
            listOf(
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
        Topic(
            "בעיטות",
            listOf(
                "בעיטת פטיש",
                "בעיטת גזיזה אחורית",
                "בעיטת גזיזה קדמית",
                "בעיטת גזיזה קדמית ובעיטת גזיזה אחורית בסיבוב",
                "בעיטת מגל לאחור בסיבוב",
                "בעיטת סטירה חיצונית בסיבוב"
            )
        ),
        Topic(
            title = "הגנות",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    "הגנה נגד בעיטות",
                    listOf(
                        "הגנה נגד בעיטת ברך מלפנים",
                        "הגנה נגד בעיטת ברך מהצד",
                        "הגנה נגד בעיטה רגילה - סייד סטפ לצד המת",
                        "הגנה נגד בעיטה רגילה - סייד סטפ לצד החי",
                        "הגנה נגד בעיטת מגל לפנים עם השוק",
                        "הגנה נגד בעיטת מגל לצלעות",
                        "הגנה נגד בעיטת מגל לפנים - בעיטה לצד",
                        "הגנה נגד בעיטת מגל לפנים - בעיטה לאחור",
                        "def:internal:kick::הגנה פנימית באמת ימין נגד בעיטה לצד"
                    )
                ),
                SubTopic(
                    "הגנות מאיום סכין",
                    listOf(
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
                )
            )
        ),
        Topic(
            title = "שחרורים",
            items = emptyList(),
            subTopics = listOf(
                SubTopic("שחרורים מתפיסות ידיים", listOf("שחרור תפיסת ידיים בשכיבה")),
                SubTopic(
                    "שחרורים מחביקות",
                    listOf(
                        "שחרור מחביקת צוואר מהצד והפלה",
                        "שחרור מחביקת צוואר מאחור עם נעילה",
                        "שחרור מחביקת צואר בשכיבה ברכיבה צמודה"
                    )
                ),
                SubTopic(
                    "שחרורים מחניקות",
                    listOf(
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
        )
    )

    // ---------------- חגורה חומה ----------------
    private val brownBeltTopics = listOf(
        Topic("גלגולים", listOf("גלגול עם רובה")),
        Topic(
            "בעיטות בניתור",
            listOf(
                "בעיטת מגל בניתור",
                "בעיטה רגילה ובעיטת מגל בניתור",
                "בעיטת מגל כפולה בניתור"
            )
        ),
        Topic(
            "שחרורים",
            listOf("חביקת צוואר מאחור – בריח על העורף, המגן כפוף לפנים")
        ),
        Topic(
            title = "הגנות",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    title = "הגנה – בעיטה",
                    items = listOf(
                        "def:internal:kick::הגנה פנימית נגד בעיטה לסנטר",
                        "def:external:kick::הגנה חיצונית נגד בעיטה רגילה – פריצה",
                        "def:external:kick::הגנה חיצונית נגד בעיטה רגילה – גזיזה",
                        "def:external:kick::הגנה חיצונית נגד בעיטה רגילה – טאטוא",
                        "def:internal:kick::הגנה פנימית נגד בעיטה רגילה – טאטוא",
                        "הגנה נגד בעיטת מגל – פריצה",
                        "def:external:kick::הגנה חיצונית נגד מגל לפנים – גזיזה",
                        "def:external:kick::הגנה חיצונית נגד מגל לפנים – טאטוא",
                        "הגנה נגד בעיטת מגל לאחור – פריצה"
                    )
                ),
                SubTopic(
                    title = "הגנה – סכין",
                    items = listOf(
                        "הגנה נגד סכין בשיסוף – הטיה והגנה לצד החי",
                        "הגנה נגד סכין בשיסוף – הטיה והגנה לצד המת",
                        "הגנה נגד סכין בשיסוף – פריצה והגנה לצד החי",
                        "הגנה נגד סכין בשיסוף – פריצה והגנה לצד המת"
                    )
                ),
                SubTopic(
                    title = "הגנה – מקל",
                    items = listOf(
                        "הגנה נגד מקל בסיבוב – צד חי",
                        "הגנה נגד מקל עם קוואלר – צד מת",
                        "הגנה נגד מקל נקודת תורפה – לצד המת"
                    )
                ),
                SubTopic(
                    title = "הגנה – איום אקדח",
                    items = listOf(
                        "הגנה מאיום אקדח מלפנים",
                        "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד ימין",
                        "הגנה מאיום אקדח מהצד הפנימי – תוקף בצד שמאל",
                        "הגנה מאיום אקדח מהצד החיצוני",
                        "הגנה מאיום אקדח מאחור"
                    )
                )
            )
        )
    )

    // ---------------- חגורה שחורה ----------------
    private val blackBeltTopics = listOf(
        Topic(
            title = "בעיטות בניתור",
            items = listOf(
                "ניתור ברגל שמאל ובעיטה רגילה ברגל ימין",
                "ניתור ברגל שמאל ובעיטה לצד ברגל ימין",
                "ניתור ברגל שמאל ובעיטה לצד ברגל שמאל",
                "בעיטת לצד בסיבוב מלא בניתור",
                "בעיטת מגל לאחור בסיבוב בניתור",
                "בעיטת הגנה לאחור בניתור"
            )
        ),
        Topic(
            title = "מכות במקל קצר",
            items = listOf(
                "מכת מקל לראש",
                "מכת מקל לרקה",
                "מכת מקל ללסת / צוואר",
                "מכת מקל לעצם הבריח",
                "מכת מקל למפרק",
                "מכת מקל לשורש כף היד",
                "מכת מקל לפרקי האצבעות",
                "מכת מקל לברך",
                "מכת מקל למפסעה",
                "הצלפת מקל לצלעות",
                "דקירת מקל חיצונית לצלעות",
                "דקירת מקל ישרה לבטן / לגרון",
                "דקירת מקל הפוכה"
            )
        ),
        Topic(
            title = "שחרורים",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    "שחרורים מחביקות צואר",
                    listOf(
                        "שחרור מחביקת צואר מהצד - משיכה לאחור",
                        "שחרור מחביקת צואר מהצד - יד תפוסה",
                        "שחרור מחביקת צואר מהצד - זריקת רגל",
                        "שחרור מחביקת צואר מהצד - מהברך"
                    )
                ),
                SubTopic("שחרורים מתפיסות נלסון", listOf("שחרור מתפיסת נלסון")),
                SubTopic(
                    "שחרורים מחביקות גוף",
                    listOf(
                        "שחרור מחביקה סגורה מהצד",
                        "שחרור מחביקה סגורה מהצד - היד הרחוקה משוחררת",
                        "שחרור מחביקה פתוחה מהצד",
                        "שחרור מחביקה פתוחה מאחור - הטלה",
                        "שחרור מחביקה סגורה מאחור - הטלה"
                    )
                )
            )
        ),
        Topic(
            title = "מכות במקל / רובה",
            items = listOf(
                "מכה אופקית לצוואר",
                "דקירה",
                "מכת מגל",
                "שיסוף",
                "מכה למפסעה",
                "מכת סנוקרת",
                "מכה לצד",
                "מכה לאחור",
                "מכה אופקית לאחור",
                "מכה אופקית ובעיטה רגילה למפסעה",
                "מכה אופקית ובעיטת הגנה לפנים",
                "מכה לצד ובעיטה לצד"
            )
        ),
        Topic(
            title = "הגנות",
            items = emptyList(),
            subTopics = listOf(
                SubTopic(
                    "הגנות - מספר תוקפים",
                    listOf(
                        "1 מקל 1 סכין – מקל בצד חי",
                        "1 מקל 1 סכין – מקל בצד מת",
                        "1 מקל 1 סכין – במקרה והסכין קרוב",
                        "הדמיה כנגד 2 תוקפים"
                    )
                ),
                SubTopic(
                    "הגנות מאיום אקדח",
                    listOf(
                        "הגנה נגד איום אקדח לראש מלפנים",
                        "הגנה נגד איום אקדח צמוד לראש מלפנים",
                        "הגנה נגד איום אקדח מלפנים – קנה קצר",
                        "הגנה נגד איום אקדח לראש – צד ימין",
                        "הגנה נגד איום אקדח לראש – צד שמאל",
                        "הגנה נגד איום אקדח לראש מהצד מאחור – צד שמאל",
                        "הגנה מאיום אקדח בהובלה",
                        "הגנה נגד איום אקדח לראש מאחור",
                        "הגנה נגד איום אקדח מאחור בידיים מורמות"
                    )
                ),
                SubTopic(
                    "הגנות נגד מכות בשילוב בעיטות",
                    listOf(
                        "def:internal:punch::הגנה פנימית נגד אגרוף שמאל – בעיטת הגנה",
                        "def:internal:punch::הגנה פנימית נגד אגרוף שמאל – בעיטה לצד",
                        "def:internal:punch::הגנה פנימית נגד אגרוף שמאל – בעיטה רגילה לאחור",
                        "def:internal:punch::הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לאחור",
                        "def:internal:punch::הגנה פנימית נגד אגרוף שמאל – בעיטת סטירה חיצונית",
                        "def:internal:punch::הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לפנים",
                        "def:internal:punch::הגנה פנימית נגד אגרוף שמאל – גזיזה קדמית",
                        "הגנה נגד בעיטה רגילה – התחמקות בסיבוב",
                        "הגנה נגד בעיטת מגל לפנים לראש – הדיפה באמת שמאל",
                        "הגנה נגד בעיטת מגל לפנים לראש – רגל עברה מעל הראש",
                        "הגנה נגד מגל לפנים לראש – התחמקות גוף בסיבוב וגזיזה",
                        "הגנה נגד בעיטת סטירה – גזיזה"
                    )
                ),
                SubTopic(
                    "הגנות נגד מקל",
                    listOf(
                        "הגנה נגד מקל ארוך – התקפה לצד ימין מגן",
                        "הגנה נגד מקל ארוך – התקפה לצד שמאל מגן",
                        "הגנה נגד מקל ארוך מצד ימין",
                        "הגנה נגד מקל ארוך מצד שמאל",
                        "הגנה נגד דקירה במקל ארוך – הצד החי",
                        "הגנה נגד דקירה במקל ארוך – הצד המת"
                    )
                ),
                SubTopic(
                    "הגנות נגד סכין",
                    listOf(
                        "הגנה נגד סכין שיסוף מהצד החי – בצד ימין",
                        "הגנה נגד סכין שיסוף מהצד החי – בצד שמאל",
                        "הגנה נגד סכין שיסוף מהצד המת – בצד ימין",
                        "הגנה נגד סכין שיסוף מהצד המת – בצד שמאל"
                    )
                ),
                SubTopic(
                    "הגנות – מקל נגד סכין",
                    listOf(
                        "הגנת מקל נגד סכין בתפיסה רגילה",
                        "הגנת מקל נגד סכין בתפיסה מזרחית",
                        "הגנת מקל נגד סכין בתפיסה ישרה",
                        "הגנת מקל נגד סכין בתפיסה רגילה – צד ימין",
                        "הגנת מקל נגד סכין בתפיסה רגילה – צד שמאל",
                        "הגנת מקל נגד סכין בתפיסה מזרחית – צד ימין",
                        "הגנת מקל נגד סכין בתפיסה מזרחית – צד שמאל",
                        "הגנה פנימית במקל נגד סכין בתפיסה ישרה – צד ימין",
                        "הגנה חיצונית במקל נגד סכין בתפיסה ישרה – צד ימין",
                        "הגנה פנימית במקל נגד סכין בתפיסה ישרה – צד שמאל",
                        "הגנה חיצונית במקל נגד סכין בתפיסה ישרה – צד שמאל",
                        "מקל בתנועה",
                        "שימוש בסכין"
                    )
                ),
                SubTopic(
                    "הגנה עם רובה נגד סכין",
                    listOf(
                        "הגנות עם רובה נגד דקירות סכין",
                        "רובה נגד דקירה ישירה גבוהה",
                        "רובה נגד דקירה ישירה נמוכה",
                        "רובה נגד דקירה רגילה",
                        "רובה נגד דקירה מזרחית מימין",
                        "רובה נגד דקירה מזרחית משמאל",
                        "רובה נגד דקירה מזרחית מלמטה"
                    )
                ),
                SubTopic("הגנה מאיום תמ\"ק", listOf("הגנה נגד איום תת־מקלע"))
            )
        )
    )
}
