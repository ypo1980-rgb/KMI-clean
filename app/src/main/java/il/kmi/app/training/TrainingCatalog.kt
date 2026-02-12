package il.kmi.app.training

import java.util.Calendar
import java.util.Locale

/**
 * מקור אמת יחיד ללוגיקת אימונים:
 * - אזורים → סניפים
 * - סניף → קבוצות גיל
 * - לו״ז שבועי (מאוחד)
 * - החזרת אימונים קרובים (upcomingFor/trainingsFor)
 *
 * בתחתית הקובץ יש Shim תאימות ל-TrainingDirectory הישן.
 */
object TrainingCatalog {
    // קריאת משך אימון מ-TrainingData בצורה עמידה לאי-תאמויות
    private fun readDurationMinutes(td: TrainingData): Int {
        val cls = td::class.java

        // 1) נסה שדות אינט מגוונים
        val intNames = arrayOf("durationMinutes", "duration", "dur", "length")
        for (n in intNames) {
            val v = runCatching {
                val f = cls.getDeclaredField(n).apply { isAccessible = true }
                (f.get(td) as? Number)?.toInt()
            }.getOrNull()
            if (v != null && v > 0) return v
        }

        // 2) נסה לגזור לפי end (אם קיים) מול td.cal
        val endAny = runCatching {
            val f = cls.getDeclaredField("end").apply { isAccessible = true }
            f.get(td)
        }.getOrNull()

        val endMillis: Long? = when (endAny) {
            is java.util.Calendar -> endAny.timeInMillis
            is java.util.Date     -> endAny.time
            is Number             -> endAny.toLong()
            else                  -> null
        }

        if (endMillis != null) {
            val diffMin = ((endMillis - td.cal.timeInMillis) / 60_000L).toInt()
            if (diffMin > 0) return diffMin
        }

        // 3) ברירת מחדל בטוחה
        return 90
    }

    /** נרמול שם קבוצה ... */
    fun normalizeGroupLabel(name: String?): String {
        val n = name?.trim().orEmpty()
        return if (n.contains("נוער") && n.contains("בוגר")) "נוער + בוגרים" else n
    }


    // ─────────────────────────────────────────────────────────────
    // אזורים → סניפים
    // ─────────────────────────────────────────────────────────────
    private val BRANCHES_BY_REGION_RAW: Map<String, List<String>> = mapOf(
        "השרון" to listOf(
            "נתניה – מרכז קהילתי אופק",
            "נתניה – מרכז קהילתי סוקולוב",
            "נתניה – נורדאו",
            "עזריאל – מושב עזריאל",
            "רעננה – מרכז קהילתי לב הפארק",
            "הרצליה – מרכז קהילתי נוף ים",
            "כפר סבא – היכל התרבות",
            "הוד השרון – מרכז ספורט עירוני"
        ),
        "מרכז" to listOf(
            "תל אביב – מרכז קהילתי דובנוב",
            "תל אביב – מרכז קהילתי יד אליהו",
            "פתח תקווה – מתנ\"ס עמישב"
        ),
        "ירושלים" to listOf(
            "ירושלים – מרכז קהילתי רמות ספיר",
            "ירושלים – מרכז קהילתי קריית יובל"
        ),
        "צפון" to listOf(
            "חיפה / נשר – מתנ\"ס בת לזר",
            "קריית אתא – ביה\"ס אלונים",
            "קריית ביאליק – רח' דפנה 52",
            "כרמיאל – אשכול פיס",
            "עכו – אשכול פיס",
            "עפולה – חטיבה תשע 25",
            "יאנוח – יאנוח",
            "ג'וליס – ג'וליס"
        ),
        "דרום" to listOf(
            "אשקלון – מרכז קהילתי שמשון",
            "באר שבע – מרכז קהילתי נווה זאב",
            "אשדוד – מתנ\"ס רובע י\"ב"
        ),
    )

    // ─────────────────────────────────────────────────────────────
    // HOLD לאזורים ולסניפים
    // ─────────────────────────────────────────────────────────────
    // אזורים על HOLD (לא מציגים בהם סניפים למשתמש):
    // כרגע *רק* "השרון" פעיל; כל השאר לא פעילים.
    private val INACTIVE_REGIONS: Set<String> = setOf(
        "מרכז",
        "ירושלים",
        "צפון",
        "דרום"
    )

    // הודעה שתוצג באזורים הלא-פעילים
    const val REGION_HOLD_MESSAGE: String = "אין סניפים זמינים באזור זה"

    // סניפים על HOLD בתוך אזור השרון (מוסתרים אך לא נמחקים מהדאטה):
    private val INACTIVE_BRANCHES: Set<String> = setOf(
        "הרצליה – מרכז קהילתי נוף ים",
        "כפר סבא – היכל התרבות",
        "רעננה – מרכז קהילתי לב הפארק",
        "הוד השרון – מרכז ספורט עירוני"
    )

    /** האם האזור פעיל (כלומר *לא* על HOLD) */
    fun isRegionActive(region: String): Boolean = !INACTIVE_REGIONS.contains(region)

    /** הודעת סטטוס לאזור על HOLD; אחרת null */
    fun regionStatusMessage(region: String): String? =
        if (isRegionActive(region)) null else REGION_HOLD_MESSAGE

    /**
     * Map "חי" שמסתיר סניפים באזורים לא פעילים, וגם מסנן סניפים על HOLD בתוך אזור פעיל.
     * הנתונים המקוריים נשמרים ב-BRANCHES_BY_REGION_RAW.
     */
    val branchesByRegion: Map<String, List<String>> = object : Map<String, List<String>> by BRANCHES_BY_REGION_RAW {
        private fun filtered(list: List<String>, region: String): List<String> =
            if (!isRegionActive(region)) emptyList() else list.filter { it !in INACTIVE_BRANCHES }

        override fun get(key: String): List<String>? {
            val base = BRANCHES_BY_REGION_RAW[key] ?: return null
            return filtered(base, key)
        }
        override val entries: Set<Map.Entry<String, List<String>>>
            get() = BRANCHES_BY_REGION_RAW.entries.map { e ->
                object : Map.Entry<String, List<String>> {
                    override val key: String = e.key
                    override val value: List<String> = filtered(e.value, e.key)
                }
            }.toSet()
        override val values: Collection<List<String>>
            get() = BRANCHES_BY_REGION_RAW.map { (k, v) -> filtered(v, k) }
        // keys נשאר ללא שינוי
    }

    /** סניפים נראים למשתמש עבור אזור (מסוננים לפי HOLD אזורי/סניפי) */
    fun branchesFor(region: String): List<String> =
        branchesByRegion[region] ?: emptyList()

    // ─────────────────────────────────────────────────────────────
    // כתובות סניפים + עזרי מיפוי (מחליף את TrainingAddresses הישן)
    // ─────────────────────────────────────────────────────────────

    private val ADDRESS_BY_BRANCH: Map<String, String> = mapOf(
        // ── השרון ──
        "נתניה – מרכז קהילתי סוקולוב" to "רחוב נחום סוקולוב 25, נתניה",
        "נתניה – מרכז קהילתי אופק"     to "רחוב אבא אחימאיר 6, נתניה",
        "נתניה – נורדאו"                to "אריה לוין 3, נתניה",
        "עזריאל – מושב עזריאל"         to "מושב עזריאל, מאחורי מכולת המושב",
        "רעננה – מרכז קהילתי לב הפארק" to "רעננה – מרכז קהילתי לב הפארק",
        "הרצליה – מרכז קהילתי נוף ים"   to "הרצליה – מרכז קהילתי נוף ים",
        "כפר סבא – היכל התרבות"         to "כפר סבא – היכל התרבות",
        "הוד השרון – מרכז ספורט עירוני" to "הוד השרון – מרכז ספורט עירוני",

        // ── מרכז ──
        "תל אביב – מרכז קהילתי דובנוב"   to "תל אביב – מרכז קהילתי דובנוב",
        "תל אביב – מרכז קהילתי יד אליהו" to "תל אביב – מרכז קהילתי יד אליהו",
        "פתח תקווה – מתנ\"ס עמישב"        to "פתח תקווה – מתנ\"ס עמישב",

        // ── ירושלים ──
        "ירושלים – מרכז קהילתי רמות ספיר"  to "ירושלים – מרכז קהילתי רמות ספיר",
        "ירושלים – מרכז קהילתי קריית יובל" to "ירושלים – מרכז קהילתי קריית יובל",

        // ── צפון ──
        "חיפה / נשר – מתנ\"ס בת לזר"      to "חיפה / נשר – מתנ\"ס בת לזר",
        "קריית אתא – ביה\"ס אלונים"       to "קריית אתא – ביה\"ס אלונים",
        "קריית ביאליק – רח' דפנה 52"      to "קריית ביאליק – רח' דפנה 52",
        "כרמיאל – אשכול פיס"              to "כרמיאל – אשכול פיס",
        "עכו – אשכול פיס"                  to "עכו – אשכול פיס",
        "עפולה – חטיבה תשע 25"             to "עפולה – חטיבה תשע 25",
        "יאנוח – יאנוח"                     to "יאנוח – יאנוח",
        "ג'וליס – ג'וליס"                   to "ג'וליס – ג'וליס",

        // ── דרום ──
        "אשקלון – מרכז קהילתי שמשון"      to "אשקלון – מרכז קהילתי שמשון",
        "באר שבע – מרכז קהילתי נווה זאב"  to "באר שבע – מרכז קהילתי נווה זאב",
        "אשדוד – מתנ\"ס רובע י\"ב"         to "אשדוד – מתנ\"ס רובע י\"ב"
    )

    // ── עזרי נרמול לשמות סניפים (מונע פספוסי התאמה בגלל מקפים/רווחים) ──
    private fun normalizeBranchKey(raw: String): String {
        var s = raw.trim()
        // אחידות מקפים בין עיר↔מקום בלבד
        s = s.replace(" — ", " – ").replace(" - ", " – ")
        // הורדת רווחים כפולים
        s = s.replace(Regex("\\s+"), " ")
        return s
    }

    // האם הטקסט כבר נראה כמו כתובת (רחוב/מספר/פסיק)? אם כן – החזר אותו ככתובת.
    private fun looksLikeFullAddress(s: String): Boolean =
        s.any { it.isDigit() } || s.contains(',')

    /** מחזיר כתובת מלאה לסניף:
     *  1) אם הגיעו כמה סניפים בטקסט אחד (שורות/מפרידים) → נמפה כל אחד ונחבר בשורה חדשה.
     *  2) אם ה־input כבר נראה כמו כתובת → החזר כמות שהוא.
     *  3) ניסיון התאמה מדויק/מנורמל למפתח במפה.
     *  4) פולבאק: "<מקום>, <עיר>" אם הטקסט הוא בתבנית "עיר – מקום".
     *  5) אחרת החזר את הטקסט המקורי.
     */
    fun addressFor(branchOrAddress: String): String {
        val src = branchOrAddress.trim()

        // ⬅️ חדש: טיפול ברשימת סניפים (שבורים לשורות/מופרדים)
        if (src.contains('\n') || src.contains('|') || src.contains(';')) {
            val parts = src.split('\n', '|', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return parts.joinToString("\n") { single -> addressForSingleBranch(single) }
        }

        return addressForSingleBranch(src)
    }

    // לוגיקה עבור סניף/ערך בודד (כמו קודם, עם נרמול מקפים ורווחים)
    private fun addressForSingleBranch(src: String): String {
        if (looksLikeFullAddress(src)) return src

        val candidates = buildList {
            add(src)
            add(normalizeBranchKey(src))
            add(src.replace("–", " - "))
            add(src.replace(" - ", " – "))
        }

        for (key in candidates) {
            ADDRESS_BY_BRANCH[key]?.let { mapped ->
                if (mapped.isNotBlank() && mapped != key) return mapped
            }
        }

        val parts = src.split(Regex("\\s*[–-]\\s*"), limit = 2)
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            val city = parts[0].trim()
            val venue = parts[1].trim()
            return "$venue, $city"
        }
        return src
    }

    /** שם המקום מתוך "עיר – מקום" עם נרמול מקפים; אחרת מחזיר src. */
    fun placeFor(branch: String): String {
        val s = normalizeBranchKey(branch)
        val idx = s.indexOf(" – ")
        return if (idx >= 0 && idx + 3 < s.length) s.substring(idx + 3) else branch
    }

    // ─────────────────────────────────────────────────────────────
    // סניף → קבוצות גיל (למסכי רישום/בחירה)
    // ─────────────────────────────────────────────────────────────

    val ageGroupsByBranch: Map<String, List<String>> = mapOf(
        "נתניה – מרכז קהילתי אופק" to listOf("גן חובה - כיתה א", "כיתה ב' - כיתה ה'", "כיתה ו' - כיתה ח'", "נוער + בוגרים", "בוגרים"),
        "נתניה – מרכז קהילתי סוקולוב" to listOf("בוגרים", "ילדים"),
        "נתניה – נורדאו" to listOf("טרום חובה וחובה", "כיתה א' - כיתה ב'", "כיתה ג' - כיתה ו'", "בוגרים"),
        "עזריאל – מושב עזריאל" to listOf("ילדים (גן חובה עד כיתה ב')", "כיתה ג' - כיתה ז'", "נוער + בוגרים"),
        "רעננה – מרכז קהילתי לב הפארק" to listOf("בוגרים"),
        "הרצליה – מרכז קהילתי נוף ים" to listOf("בוגרים"),
        "כפר סבא – היכל התרבות" to listOf("בוגרים"),
        "הוד השרון – מרכז ספורט עירוני" to listOf("בוגרים")
    )

    // ─────────────────────────────────────────────────────────────
    // מודל יחיד לסלוט אימון שבועי
    // ─────────────────────────────────────────────────────────────
    data class TrainingSlot(
        val branch: String,
        val groups: List<String>,          // למשל: "בוגרים (18+)", "נוער (12–17)"
        val dayOfWeek: Int,                // Calendar.SUNDAY .. Calendar.SATURDAY
        val startHour: Int,
        val startMinute: Int,
        val durationMinutes: Int,
        val place: String,
        val address: String,
        val coach: String
    )

    // ─────────────────────────────────────────────────────────────
    // לו״ז מאוחד (Catalog + Directory שהיו קודם)
    // ─────────────────────────────────────────────────────────────
    private val slots: List<TrainingSlot> = buildList {
        // ========= נתניה – מרכז קהילתי סוקולוב =========
        addAll(
            listOf(
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי סוקולוב",
                    groups = listOf("בוגרים"),
                    dayOfWeek = Calendar.SUNDAY,   // 20:00–21:30
                    startHour = 20, startMinute = 0,
                    durationMinutes = 90,
                    place = placeFor("נתניה – מרכז קהילתי סוקולוב"),
                    address = addressFor("רחוב נחום סוקולוב 25, נתניה"),
                    coach = "אדם הולצמן"
                ),
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי סוקולוב",
                    groups = listOf("בוגרים"),
                    dayOfWeek = Calendar.TUESDAY,  // 20:00–21:30
                    startHour = 20, startMinute = 0,
                    durationMinutes = 90,
                    place = placeFor("נתניה – מרכז קהילתי סוקולוב"),
                    address = addressFor("רחוב נחום סוקולוב 25, נתניה"),
                    coach = "אדם הולצמן"
                )
            )
        )

        add(
            TrainingSlot(
                branch = "נתניה – מרכז קהילתי סוקולוב",
                groups = listOf("ילדים"),
                dayOfWeek = Calendar.WEDNESDAY, // 19:00–20:00
                startHour = 20, startMinute = 0,
                durationMinutes = 90,
                place = placeFor("נתניה – מרכז קהילתי סוקולוב"),
                address = addressFor("רחוב נחום סוקולוב 25, נתניה"),
                coach = "אדם הולצמן"
            )
        )

        // ========= נתניה – מרכז קהילתי אופק =========
        addAll(
            listOf(
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי אופק",
                    groups = listOf("גן חובה - כיתה א'"),
                    dayOfWeek = Calendar.MONDAY,   // 16:45–17:15
                    startHour = 16, startMinute = 45,
                    durationMinutes = 30,
                    place = placeFor("נתניה – מרכז קהילתי אופק"),
                    address = addressFor("רחוב אבא אחימאיר 6, נתניה"),
                    coach = "יוני מלסה"
                ),
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי אופק",
                    groups = listOf("גן חובה - כיתה א'"),
                    dayOfWeek = Calendar.THURSDAY, // 16:45–17:15
                    startHour = 16, startMinute = 45,
                    durationMinutes = 30,
                    place = placeFor("נתניה – מרכז קהילתי אופק"),
                    address = addressFor("רחוב אבא אחימאיר 6, נתניה"),
                    coach = "יוני מלסה"
                )
            )
        )
        addAll(
            listOf(
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי אופק",
                    groups = listOf("כיתה ב' - כיתה ה'"),
                    dayOfWeek = Calendar.MONDAY,   // 17:15–18:00
                    startHour = 17, startMinute = 15,
                    durationMinutes = 45,
                    place = placeFor("נתניה – מרכז קהילתי אופק"),
                    address = addressFor("רחוב אבא אחימאיר 6, נתניה"),
                    coach = "יוני מלסה"
                ),
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי אופק",
                    groups = listOf("כיתה ב' - כיתה ה'"),
                    dayOfWeek = Calendar.THURSDAY, // 17:15–18:00
                    startHour = 17, startMinute = 15,
                    durationMinutes = 45,
                    place = placeFor("נתניה – מרכז קהילתי אופק"),
                    address = addressFor("רחוב אבא אחימאיר 6, נתניה"),
                    coach = "יוני מלסה"
                )
            )
        )
        addAll(
            listOf(
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי אופק",
                    groups = listOf("כיתה ו' - כיתה ח'"),
                    dayOfWeek = Calendar.MONDAY,   // 18:00–19:00
                    startHour = 18, startMinute = 0,
                    durationMinutes = 60,
                    place = placeFor("נתניה – מרכז קהילתי אופק"),
                    address = addressFor("רחוב אבא אחימאיר 6, נתניה"),
                    coach = "יוני מלסה"
                ),
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי אופק",
                    groups = listOf("כיתה ו' - כיתה ח'"),
                    dayOfWeek = Calendar.THURSDAY, // 18:00–19:00
                    startHour = 18, startMinute = 0,
                    durationMinutes = 60,
                    place = placeFor("נתניה – מרכז קהילתי אופק"),
                    address = addressFor("רחוב אבא אחימאיר 6, נתניה"),
                    coach = "יוני מלסה"
                )
            )
        )
        addAll(
            listOf(
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי אופק",
                    groups = listOf("נוער + בוגרים"),
                    dayOfWeek = Calendar.MONDAY,   // 19:00–20:30
                    startHour = 19, startMinute = 0,
                    durationMinutes = 90,
                    place = placeFor("נתניה – מרכז קהילתי אופק"),
                    address = addressFor("רחוב אבא אחימאיר 6, נתניה"),
                    coach = "יוני מלסה"
                ),
                TrainingSlot(
                    branch = "נתניה – מרכז קהילתי אופק",
                    groups = listOf("נוער + בוגרים"),
                    dayOfWeek = Calendar.THURSDAY, // 19:00–20:30
                    startHour = 19, startMinute = 0,
                    durationMinutes = 90,
                    place = placeFor("נתניה – מרכז קהילתי אופק"),
                    address = addressFor("רחוב אבא אחימאיר 6, נתניה"),
                    coach = "יוני מלסה"
                )
            )
        )
        add(
            TrainingSlot(
                branch = "נתניה – מרכז קהילתי אופק",
                groups = listOf("בוגרים"),
                dayOfWeek = Calendar.MONDAY,  // 20:30–22:00
                startHour = 20, startMinute = 30,
                durationMinutes = 90,
                place = placeFor("נתניה – מרכז קהילתי אופק"),
                address = addressFor("רחוב אבא אחימאיר 6, נתניה"),
                coach = "יוני מלסה"
            )
        )

        // ========= נורדאו =========
        addAll(
            listOf(
                TrainingSlot(
                    branch = "נתניה – נורדאו",
                    groups = listOf("טרום חובה וחובה"),
                    dayOfWeek = Calendar.SUNDAY,    // 16:45–17:15
                    startHour = 16, startMinute = 45,
                    durationMinutes = 30,
                    place = placeFor("נתניה – נורדאו"),
                    address = addressFor("אריה לוין 3 נתניה"),
                    coach = "רבקה מסיקה"
                ),
                TrainingSlot(
                    branch = "נתניה – נורדאו",
                    groups = listOf("טרום חובה וחובה"),
                    dayOfWeek = Calendar.WEDNESDAY, // 16:45–17:15
                    startHour = 16, startMinute = 45,
                    durationMinutes = 30,
                    place = placeFor("נתניה – נורדאו"),
                    address = addressFor("אריה לוין 3 נתניה"),
                    coach = "רבקה מסיקה"
                )
            )
        )

        addAll(
            listOf(
                TrainingSlot(
                    branch = "נתניה – נורדאו",
                    groups = listOf("כיתה א' - כיתה ב'"),
                    dayOfWeek = Calendar.SUNDAY,    // 17:15–18:00
                    startHour = 17, startMinute = 15,
                    durationMinutes = 45,
                    place = placeFor("נתניה – נורדאו"),
                    address = addressFor("אריה לוין 3 נתניה"),
                    coach = "רבקה מסיקה"
                ),
                TrainingSlot(
                    branch = "נתניה – נורדאו",
                    groups = listOf("כיתה א' - כיתה ב'"),
                    dayOfWeek = Calendar.WEDNESDAY, // 17:15–18:00
                    startHour = 17, startMinute = 15,
                    durationMinutes = 45,
                    place = placeFor("נתניה – נורדאו"),
                    address = addressFor("אריה לוין 3 נתניה"),
                    coach = "רבקה מסיקה"
                )
            )
        )
        addAll(
            listOf(
                TrainingSlot(
                    branch = "נתניה – נורדאו",
                    groups = listOf("כיתה ג' - כיתה ו'"),
                    dayOfWeek = Calendar.SUNDAY,    // 18:00–18:45
                    startHour = 18, startMinute = 0,
                    durationMinutes = 45,
                    place = placeFor("נתניה – נורדאו"),
                    address = addressFor("אריה לוין 3 נתניה"),
                    coach = "רבקה מסיקה"
                ),
                TrainingSlot(
                    branch = "נתניה – נורדאו",
                    groups = listOf("כיתה ג' - כיתה ו'"),
                    dayOfWeek = Calendar.WEDNESDAY, // 18:00–18:45
                    startHour = 18, startMinute = 0,
                    durationMinutes = 45,
                    place = placeFor("נתניה – נורדאו"),
                    address = addressFor("אריה לוין 3 נתניה"),
                    coach = "רבקה מסיקה"
                )
            )
        )


        // ========= עזריאל =========
        add(
            TrainingSlot(
                branch = "עזריאל – מושב עזריאל",
                groups = listOf("ילדים (גן חובה - כיתה ב')"),
                dayOfWeek = Calendar.SUNDAY,    // 17:00–17:45
                startHour = 17, startMinute = 0,
                durationMinutes = 45,
                place = placeFor("עזריאל – מושב עזריאל"),
                address = addressFor("מושב עזריאל, מאחורי מכולת המושב"),
                coach = "יוני מלסה"
            )
        )
        add(
            TrainingSlot(
                branch = "עזריאל – מושב עזריאל",
                groups = listOf("כיתה ג' - כיתה ז'"),
                dayOfWeek = Calendar.WEDNESDAY, // 17:45–18:45
                startHour = 17, startMinute = 45,
                durationMinutes = 60,
                place = placeFor("עזריאל – מושב עזריאל"),
                address = addressFor("מושב עזריאל, מאחורי מכולת המושב"),
                coach = "יוני מלסה"
            )
        )
        add(
            TrainingSlot(
                branch = "עזריאל – מושב עזריאל",
                groups = listOf("נוער + בוגרים"),
                dayOfWeek = Calendar.WEDNESDAY, // 18:45–20:00
                startHour = 18, startMinute = 45,
                durationMinutes = 75,
                place = placeFor("עזריאל – מושב עזריאל"),
                address = addressFor("מושב עזריאל, מאחורי מכולת המושב"),
                coach = "יוני מלסה"
            )
        )


        // ========= רעננה =========
        add(
            TrainingSlot(
                branch = "רעננה – מרכז קהילתי לב הפארק",
                groups = listOf("בוגרים"),
                dayOfWeek = Calendar.MONDAY,    // 20:00–21:30
                startHour = 20, startMinute = 0,
                durationMinutes = 90,
                place = placeFor("רעננה – מרכז קהילתי לב הפארק"),
                address = addressFor("רעננה – מרכז קהילתי לב הפארק"),
                coach = "דניאל כהן"
            )
        )
        add(
            TrainingSlot(
                branch = "רעננה – מרכז קהילתי לב הפארק",
                groups = listOf("בוגרים"),
                dayOfWeek = Calendar.THURSDAY,  // 20:00–21:30
                startHour = 20, startMinute = 0,
                durationMinutes = 90,
                place = placeFor("רעננה – מרכז קהילתי לב הפארק"),
                address = addressFor("רעננה – מרכז קהילתי לב הפארק"),
                coach = "דניאל כהן"
            )
        )

        // ========= הרצליה =========
        add(
            TrainingSlot(
                branch = "הרצליה – מרכז קהילתי נוף ים",
                groups = listOf("בוגרים"),
                dayOfWeek = Calendar.SUNDAY,    // 19:00–20:30
                startHour = 19, startMinute = 0,
                durationMinutes = 90,
                place = placeFor("הרצליה – מרכז קהילתי נוף ים"),
                address = addressFor("הרצליה – מרכז קהילתי נוף ים"),
                coach = "נטע שלו"
            )
        )
        add(
            TrainingSlot(
                branch = "הרצליה – מרכז קהילתי נוף ים",
                groups = listOf("בוגרים"),
                dayOfWeek = Calendar.WEDNESDAY, // 19:00–20:30
                startHour = 19, startMinute = 0,
                durationMinutes = 90,
                place = placeFor("הרצליה – מרכז קהילתי נוף ים"),
                address = addressFor("הרצליה – מרכז קהילתי נוף ים"),
                coach = "נטע שלו"
            )
        )

        // ========= כפר סבא =========
        add(
            TrainingSlot(
                branch = "כפר סבא – היכל התרבות",
                groups = listOf("בוגרים"),
                dayOfWeek = Calendar.TUESDAY,   // 20:00–21:30
                startHour = 20, startMinute = 0,
                durationMinutes = 90,
                place = placeFor("כפר סבא – היכל התרבות"),
                address = addressFor("כפר סבא – היכל התרבות"),
                coach = "טל לוי"
            )
        )
        add(
            TrainingSlot(
                branch = "כפר סבא – היכל התרבות",
                groups = listOf("בוגרים"),
                dayOfWeek = Calendar.FRIDAY,    // 09:00–10:30
                startHour = 9, startMinute = 0,
                durationMinutes = 90,
                place = placeFor("כפר סבא – היכל התרבות"),
                address = addressFor("כפר סבא – היכל התרבות"),
                coach = "טל לוי"
            )
        )

        // ========= הוד השרון =========
        add(
            TrainingSlot(
                branch = "הוד השרון – מרכז ספורט עירוני",
                groups = listOf("בוגרים"),
                dayOfWeek = Calendar.MONDAY,    // 19:30–21:00
                startHour = 19, startMinute = 30,
                durationMinutes = 90,
                place = placeFor("הוד השרון – מרכז ספורט עירוני"),
                address = addressFor("הוד השרון – מרכז ספורט עירוני"),
                coach = "אלה דן"
            )
        )
        add(
            TrainingSlot(
                branch = "הוד השרון – מרכז ספורט עירוני",
                groups = listOf("בוגרים"),
                dayOfWeek = Calendar.THURSDAY,  // 19:30–21:00
                startHour = 19, startMinute = 30,
                durationMinutes = 90,
                place = placeFor("הוד השרון – מרכז ספורט עירוני"),
                address = addressFor("הוד השרון – מרכז ספורט עירוני"),
                coach = "אלה דן"
            )
        )
    }

    // ─────────────────────────────────────────────────────────────
    // API ציבורי
    // ─────────────────────────────────────────────────────────────

    // עזר להשוואת קבוצות בצורה חסינה לוריאציות בשם
    private fun cleanLabel(s: String?): String {
        // מנקה רווחים/מקפים/גרשים והופך ל־lowercase להשוואה חסינה
        val t = s.orEmpty()
            .replace("’", "'")
            .replace("׳", "'")
            .replace("`", "'")
            .replace("–", "-")
            .replace("—", "-")
            .lowercase(Locale("he", "IL"))
        return buildString(t.length) {
            t.forEach { ch ->
                if (ch.isLetterOrDigit() || ch == '_' ) append(ch)
                // מתעלמים מרווחים, מקפים וגרשים להשוואה “מטושטשת”
            }
        }
    }

    /** נרמול שם קבוצה לקטגוריות-על עקביות + ניקוי גרשיים/רווחים */
    fun normalizeGroupName(name: String?): String {
        val raw = name?.trim().orEmpty()
        val lower = raw.lowercase(Locale("he", "IL"))
        val hasAdult = lower.contains("בוגר")
        val hasYouth = lower.contains("נוער")
        val isKids = lower.contains("ילד") || lower.contains("כיתה") || lower.contains("גן") || lower.contains("טרום")
        return when {
            hasAdult && hasYouth -> "נוער + בוגרים"
            hasAdult             -> "בוגרים"
            hasYouth             -> "נוער"
            isKids               -> "ילדים"
            else                 -> raw
        }
    }

    private fun isBranchInRegion(region: String, branch: String): Boolean {
        val branches = branchesFor(region) // ⬅️ מסנן לפי HOLD
        return branch in branches
    }

    /** אימונים שבועיים קבועים עבור סניף/קבוצה (null=כל הקבוצות) */
    fun trainingsFor(branch: String, group: String?): List<TrainingData> {
        val wantedNorm = normalizeGroupName(group)
        val wantedClean = cleanLabel(wantedNorm)

        val relevant = slots.filter { slot ->
            // התאמה לפי סניף (בדיוק כפי שמוגדר ב-slots)
            slot.branch == branch &&
                    // התאמת קבוצה: אם group ריק → כל הקבוצות; אחרת נבדוק:
                    (group == null || slot.groups.any { sg ->
                        val normSlot = normalizeGroupName(sg)

                        // 1) התאמה לפי קטגוריית־על מדויקת
                        if (normSlot == wantedNorm) return@any true

                        // 2) התאמה “מטושטשת” אחרי ניקוי סימני פיסוק
                        if (cleanLabel(sg) == wantedClean) return@any true

                        // 3) התאמה טקסטואלית ישירה
                        if (sg == group) return@any true

                        // 4) **התאמות מרחיבות:**
                        // נוער → נוער + בוגרים
                        if (wantedNorm == "נוער" && normSlot == "נוער + בוגרים") return@any true

                        // בוגרים → נוער + בוגרים
                        if (wantedNorm == "בוגרים" && normSlot == "נוער + בוגרים") return@any true

                        false
                    })
        }

        return relevant.map { s ->
            TrainingData.nextWeekly(
                dayOfWeek = s.dayOfWeek,
                startHour = s.startHour,
                startMinute = s.startMinute,
                durationMinutes = s.durationMinutes,
                place = s.place,
                address = s.address,
                coach = s.coach
            )
        }.sortedBy { it.cal.timeInMillis }
    }

    /** אימונים קרובים לתצוגה במסך הבית */
    fun upcomingFor(
        region: String,
        branch: String,
        group: String,
        count: Int = 3
    ): List<TrainingData> {
        // אם האזור על HOLD → אין תוצאות (UI יכול להציג regionStatusMessage(region))
        if (!isRegionActive(region)) return emptyList()
        if (!isBranchInRegion(region, branch)) return emptyList()
        val g: String? = group.ifBlank { null }
        return trainingsFor(branch, g).take(count)
    }
}

/* =============================================================================
   Shim תאימות ל-TrainingDirectory הישן (אפשר להשאיר זמנית)
   ============================================================================= */
@Deprecated("Use TrainingCatalog.* APIs. This is a thin compatibility shim.")
object TrainingDirectory {

    // ── מודלים ישנים לשימור תאימות ────────────────────────────────────────────
    data class TrainingSlot(
        val dayOfWeek: Int,
        val startHour: Int,
        val startMinute: Int,
        val durationMinutes: Int,
        val locationOverride: String? = null
    )
    data class GroupSchedule(
        val coachName: String,
        val slots: List<TrainingSlot>
    )
    data class UpcomingTraining(
        val start: java.util.Calendar,
        val end: java.util.Calendar,
        val coachName: String,
        val placeName: String,
        val fullAddress: String
    )

    // ── עזר: קריאת משך אימון בצורה עמידה להבדלי מודלים ───────────────────────
    private fun readDurationMinutes(td: TrainingData): Int {
        val cls = td::class.java

        // 1) נסה שדה אינט מוכר (durationMinutes / duration / dur / length)
        val intNames = arrayOf("durationMinutes", "duration", "dur", "length")
        for (n in intNames) {
            val v = runCatching {
                val f = cls.getDeclaredField(n).apply { isAccessible = true }
                (f.get(td) as? Number)?.toInt()
            }.getOrNull()
            if (v != null && v > 0) return v
        }

        // 2) נסה לגזור מ-end אם קיים מול td.cal
        val endAny = runCatching {
            val f = cls.getDeclaredField("end").apply { isAccessible = true }
            f.get(td)
        }.getOrNull()

        val endMillis: Long? = when (endAny) {
            is java.util.Calendar -> endAny.timeInMillis
            is java.util.Date     -> endAny.time
            is Number             -> endAny.toLong()
            else                  -> null
        }

        if (endMillis != null) {
            val diffMin = ((endMillis - td.cal.timeInMillis) / 60_000L).toInt()
            if (diffMin > 0) return diffMin
        }

        // 3) ברירת מחדל
        return 90
    }

    /** שליפה לפי סניף+קבוצה בפורמט הקטלוג ("בוגרים (18+)" וכו׳) */
    fun getSchedule(branch: String?, group: String?): GroupSchedule? {
        if (branch.isNullOrBlank() || group.isNullOrBlank()) return null
        val list = TrainingCatalog.trainingsFor(branch, group)
        if (list.isEmpty()) return null

        val coach = list.first().coach
        val slots = list.map { td ->
            TrainingSlot(
                dayOfWeek       = td.cal.get(java.util.Calendar.DAY_OF_WEEK),
                startHour       = td.cal.get(java.util.Calendar.HOUR_OF_DAY),
                startMinute     = td.cal.get(java.util.Calendar.MINUTE),
                durationMinutes = readDurationMinutes(td)
            )
        }
        return GroupSchedule(coachName = coach, slots = slots)
    }

    /** אימונים עתידיים לפי בחירת המשתמש ב-SP (עם גרייס שעה), מוגבלים ל.limit */
    fun getUserUpcomingTrainings(context: android.content.Context, limit: Int = 3): List<UpcomingTraining> {
        val sp = context.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
        val branch = sp.getString("branch", "") ?: return emptyList()
        val groupRaw  = sp.getString("group", "") ?: return emptyList()
        val group = TrainingCatalog.normalizeGroupLabel(groupRaw)   // ⬅️ נרמול שם הקבוצה

        val base = TrainingCatalog.trainingsFor(branch, group)
        if (base.isEmpty()) return emptyList()

        // גרייס של שעה אחורה
        val nowMinus = System.currentTimeMillis() - 60 * 60_000L

        // ✅ השתמש בפונקציות מתוך TrainingCatalog
        val plc  = TrainingCatalog.placeFor(branch)
        val addr = TrainingCatalog.addressFor(branch)

        return base.map { td ->
            // מקדם קדימה עד שיהיה בעתיד (עם גרייס)
            val start = (td.cal.clone() as Calendar).apply {
                while (timeInMillis <= nowMinus) add(Calendar.DAY_OF_YEAR, 7)
            }

            // חישוב משך אימון מתוך מחרוזות HH:mm של start/end; נפילה ל-90ד׳ אם אין התאמה
            val durationMin: Int = run {
                val hhmm = Regex("""\b(\d{1,2}):(\d{2})\b""")
                val sM = hhmm.find(td.start)
                val eM = hhmm.find(td.end)
                if (sM != null && eM != null) {
                    val sh = sM.groupValues[1].toInt()
                    val sm = sM.groupValues[2].toInt()
                    val eh = eM.groupValues[1].toInt()
                    val em = eM.groupValues[2].toInt()
                    val startTotal = sh * 60 + sm
                    val endTotal   = eh * 60 + em
                    val diff = (endTotal - startTotal).let { if (it <= 0) it + 24 * 60 else it }
                    diff.coerceAtLeast(1)
                } else {
                    90
                }
            }

            val end = (start.clone() as Calendar).apply {
                add(Calendar.MINUTE, durationMin)
            }

            UpcomingTraining(
                start = start,
                end = end,
                coachName = td.coach,
                placeName = plc,
                fullAddress = addr
            )
        }.sortedBy { it.start.timeInMillis }.take(n = limit)
    }

    /** עזר פורמט כמו קודם */
    fun formatUpcoming(t: UpcomingTraining): String {
        val dateFmt = java.text.SimpleDateFormat("EEEE, d/M/yyyy", java.util.Locale("he", "IL"))
        val timeFmt = java.text.SimpleDateFormat("HH:mm",         java.util.Locale("he", "IL"))
        return buildString {
            append(dateFmt.format(t.start.time)); append("  ")
            append(timeFmt.format(t.start.time)); append(" – "); append(timeFmt.format(t.end.time))
            append("\nבמקום: ").append(t.placeName)
            append("\nמאמן: ").append(t.coachName)
        }
    }
}
