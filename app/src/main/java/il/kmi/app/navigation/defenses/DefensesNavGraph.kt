package il.kmi.app.navigation.defenses

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.HardSectionsCatalog  // ✅ NEW

/**
 * גרף ייעודי להגנות:
 * - פנימיות/חיצוניות
 * - אגרופים/בעיטות
 *
 * ✅ NEW: תצוגה “לפי נושאים” בעזרת רשימות קבועות (hardcoded)
 * ✅ תומך גם kind="all" = פנימיות+חיצוניות
 */

// ---------- shared helpers (ברמת קובץ) ----------

internal fun normalizeDefenseItem(raw: String): String {
    val t0 = raw.trim().trim('"')
    val t = t0
        .replace("def_external_punches", "def:external:punch")
        .replace("def_internal_punches", "def:internal:punch")
        .replace("def_external_kicks", "def:external:kick")
        .replace("def_internal_kicks", "def:internal:kick")

    if (t.startsWith("def:") && t.contains("::")) return t

    if (t.contains("::def:")) {
        val left = t.substringBefore("::def:").trim()
        val tag = "def:" + t.substringAfter("::def:").trim()
        return "$tag::$left"
    }

    return t
}

internal fun displayName(defItem: String): String {
    val t = normalizeDefenseItem(defItem).trim().trim('"')
    if (t.startsWith("def:") && t.contains("::")) return t.substringAfter("::").trim()
    return t
}

// ---------- data source ----------

internal fun itemsFor(kind: String, pick: String): List<Pair<Belt, List<String>>> {

    fun merge(
        a: List<Pair<Belt, List<String>>>,
        b: List<Pair<Belt, List<String>>>
    ): List<Pair<Belt, List<String>>> {
        val map = linkedMapOf<Belt, MutableSet<String>>()
        (a + b).forEach { (belt, items) ->
            val set = map.getOrPut(belt) { linkedSetOf() }
            items.forEach { set += it }
        }
        return map.entries.map { it.key to it.value.toList() }
    }

    fun norm(s: String): String = s
        .trim()
        .lowercase()
        .replace("%3a", ":")
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")

    val kindN0 = norm(kind)
    val pickN0 = norm(pick)

    val kindN = when {
        kindN0 == "internal" || kindN0.contains("פנימ") -> "internal"
        kindN0 == "external" || kindN0.contains("חיצונ") -> "external"
        kindN0 == "all" -> "all"

        kindN0.startsWith("def_internal") -> "internal"
        kindN0.startsWith("def_external") -> "external"
        kindN0.startsWith("def_all") -> "all"

        // ✅ hard sections
        kindN0 == "kicks_hard" -> "kicks_hard"
        kindN0 == "releases_hard" -> "releases_hard"
        kindN0 == "knife_hard" -> "knife_hard"
        kindN0 == "gun_hard" -> "gun_hard"
        kindN0 == "stick_hard" -> "stick_hard"

        else -> kindN0
    }

    val pickN = when (pickN0) {
        "punches" -> "punch"
        "kicks" -> "kick"
        else -> pickN0
    }

    // ✅ kicks_hard מגיע *רק* מ-HardSectionsCatalog
    if (kindN == "kicks_hard") {

        val sectionTitle = when (pickN) {
            "straight_groin" -> "הגנות נגד בעיטות ישרות / למפשעה"
            "hook_back"      -> "הגנות נגד מגל / מגל לאחור"
            "knee"           -> "הגנות נגד ברך"
            else             -> ""
        }

        val section = HardSectionsCatalog.defensesKicks
            .firstOrNull { it.title.trim() == sectionTitle.trim() }

        if (section == null) return emptyList()

        val orderedBelts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)

        val byBelt: Map<Belt, List<String>> =
            section.beltGroups.associate { bg ->
                bg.belt to bg.items
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .toList()
            }

        return orderedBelts.mapNotNull { belt ->
            val items = byBelt[belt].orEmpty()
            if (items.isNotEmpty()) belt to items else null
        }
    }

    // ✅ releases_hard מגיע *רק* מ-HardSectionsCatalog
    if (kindN == "releases_hard") {

        val sectionTitle = when (pickN) {
            "hands_hair_shirt" -> "שחרור מתפיסות ידיים / שיער / חולצה"
            "chokes"           -> "שחרור מחניקות"
            "hugs"             -> "שחרור מחביקות גוף"
            else               -> ""
        }

        val section = HardSectionsCatalog.releases
            .firstOrNull { it.title.trim() == sectionTitle.trim() }

        if (section == null) return emptyList()

        val orderedBelts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)

        val byBelt: Map<Belt, List<String>> =
            section.beltGroups.associate { bg ->
                bg.belt to bg.items
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .toList()
            }

        return orderedBelts.mapNotNull { belt ->
            val items = byBelt[belt].orEmpty()
            if (items.isNotEmpty()) belt to items else null
        }
    }

    // ✅ NEW: knife/gun/stick – מגיע *רק* מ-HardSectionsCatalog
    if (kindN == "knife_hard" || kindN == "gun_hard" || kindN == "stick_hard") {

        val section = when (kindN) {
            "knife_hard" -> HardSectionsCatalog.defensesKnife.firstOrNull()
            "gun_hard"   -> HardSectionsCatalog.defensesGunThreat.firstOrNull()
            "stick_hard" -> HardSectionsCatalog.defensesStick.firstOrNull()
            else -> null
        }

        if (section == null) return emptyList()

        val orderedBelts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)

        val byBelt: Map<Belt, List<String>> =
            section.beltGroups.associate { bg ->
                bg.belt to bg.items
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .toList()
            }

        return orderedBelts.mapNotNull { belt ->
            val items = byBelt[belt].orEmpty()
            if (items.isNotEmpty()) belt to items else null
        }
    }

    val hardcoded: Map<Belt, List<String>> = when (kindN to pickN) {


        //****************************************************
        // ---------------- הגנות נגד בעיטות (ALL) ----------------
        //****************************************************
        "all" to "kick" -> mapOf(
            Belt.ORANGE to listOf(
                "הגנה חיצונית נגד בעיטה רגילה",
                "הגנה נגד בעיטת ברך",
                "הגנה נגד בעיטה רגילה - עצירה ברגל הקדמית",
                "הגנה נגד בעיטה רגילה - עצירה ברגל האחורית",
                "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בימין",
                "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בשמאל",
                "הגנה נגד בעיטת מגל לפנים באמות הידיים",
                "הגנה חיצונית נגד בעיטת מגל לפנים - אגרוף בימין",
                "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל האחורית",
                "בעיטת עצירה נגד בעיטת מגל - עצירה ברגל הקדמית",
                "בעיטת עצירה נגד בעיטה לצד"
            ),
            Belt.GREEN to listOf(
                "הגנה נגד בעיטה רגילה - בעיטה לצד",
                "הגנה נגד בעיטה רגילה – טיימינג לצד חי",
                "הגנה חיצונית באמת שמאל נגד בעיטה רגילה",
                "הגנה נגד בעיטת מגל לפנים – בעיטה לצד",
                "הגנה נגד בעיטת מגל נמוכה",
                "הגנה נגד בעיטת מגל לאחור - בעיטה בימין",
                "הגנה נגד בעיטת מגל לאחור - בעיטה שמאל",
                "הגנה נגד בעיטת מגל לאחור - אגרוף שמאל",
                "הגנה נגד בעיטת מגל לאחור בסיבוב – בעיטה",
                "הגנה חיצונית באמת ימין נגד בעיטה לצד",
                "הגנה חיצונית באמת שמאל נגד בעיטה לצד",
                "הגנה נגד בעיטת לצד בעיטת סטירה חיצונית"
            ),
            Belt.BLUE to listOf(
                "הגנה נגד בעיטת ברך מלפנים",
                "הגנה נגד בעיטת ברך מהצד",
                "הגנה נגד בעיטה רגילה – סייד-סטפ לצד המת",
                "הגנה נגד בעיטה רגילה – סייד-סטפ לצד החי",
                "הגנה נגד בעיטת מגל לפנים עם השוק",
                "הגנה נגד בעיטת מגל לצלעות",
                "הגנה נגד בעיטת מגל לפנים - בעיטה לצד",
                "הגנה נגד בעיטת מגל לפנים - בעיטה לאחור"
            ),
            Belt.BROWN to listOf(
                "הגנה חיצונית נגד בעיטה רגילה – פריצה",
                "הגנה חיצונית נגד בעיטה רגילה – גזיזה",
                "הגנה חיצונית נגד בעיטה רגילה – טאטוא",
                "הגנה נגד בעיטת מגל – פריצה",
                "הגנה חיצונית נגד מגל לפנים – גזיזה",
                "הגנה חיצונית נגד מגל לפנים – טאטוא",
                "הגנה נגד בעיטת מגל לאחור – פריצה",

                "הגנה פנימית נגד בעיטה לסנטר",
                "הגנה פנימית נגד בעיטה רגילה – טאטוא",
                "הגנה פנימית באמת ימין נגד בעיטה לצד"
            )
        )

        //****************************************************
        // ---------------- פנימיות - אגרופים ----------------
        //****************************************************
        "internal" to "punch" -> mapOf(
            Belt.YELLOW to listOf(
                "הגנות פנימיות - הגנה פנימית רפלקסיבית",
                "הגנה פנימית נגד ימין בכף יד שמאל",
                "הגנה פנימית נגד שמאל בכף יד ימין",
            ),
            Belt.ORANGE to listOf(
                "הגנה פנימית נגד שמאל עם מרפק",
                "הגנה פנימית נגד מכות ישרות למטה",
            ),
            Belt.GREEN to listOf(
                "הגנה פנימית נגד ימין באמת שמאל",
                "הגנה פנימית נגד שמאל באמת שמאל",
            ),
            Belt.BLACK to listOf(
                "הגנה פנימית נגד אגרוף שמאל – בעיטת הגנה",
                "הגנה פנימית נגד אגרוף שמאל – בעיטה לצד",
                "הגנה פנימית נגד אגרוף שמאל – בעיטה רגילה לאחור",
                "הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לאחור",
                "הגנה פנימית נגד אגרוף שמאל – בעיטת סטירה חיצונית",
                "הגנה פנימית נגד אגרוף שמאל – בעיטת מגל לפנים",
                "הגנה פנימית נגד אגרוף שמאל – גזיזה קדמית",
            )
        )

        //***************************************************
        // ---------------- פנימיות - בעיטות ----------------
        //***************************************************
        "internal" to "kick" -> mapOf(
            Belt.YELLOW to listOf(
                "הגנה פנימית נגד בעיטה רגילה למפסעה",
            ),
            Belt.BLUE to listOf(
                "הגנה פנימית באמת ימין נגד בעיטה לצד",
            ),
            Belt.BROWN to listOf(
                "הגנה פנימית נגד בעיטה לסנטר",
                "הגנה פנימית נגד בעיטה רגילה – טאטוא",
            )
        )

        //*****************************************************
        // ---------------- חיצוניות - אגרופים ----------------
        //*****************************************************
        "external" to "punch" -> mapOf(
            Belt.YELLOW to listOf(
                "הגנות חיצוניות רפלקסיבית 360 מעלות",
            ),
            Belt.ORANGE to listOf(
                "הגנה חיצונית מס' 1",
                "הגנה חיצונית מס' 2",
                "הגנה חיצונית מס' 3",
                "הגנה חיצונית מס' 4",
                "הגנה חיצונית מס' 5",
                "הגנה חיצונית מס' 6",

                // ✅ חייב להיות זהה ל-Explanations.getOrange()
                "הגנה נגד מכה גבוהה מהצד - התוקף בצד שמאל",
                "הגנה נגד מכה מהצד לעורף - התוקף בצד שמאל",
                "הגנה נגד מכה מהצד לגב - התוקף בצד שמאל",
                "הגנה נגד מכה גבוהה מהצד - התוקף בצד ימין",
                "הגנה נגד מכה מהצד לגרון - התוקף בצד ימין",
                "הגנה נגד מכה מהצד לבטן - התוקף בצד ימין",
            ),
            Belt.GREEN to listOf(
                "הגנה חיצונית נגד ימין באגרוף מהופך",
                "הגנה חיצונית נגד שמאל",
                "הגנה חיצונית נגד שמאל בהתקדמות",
            )
        )

        //****************************************************
        // ---------------- חיצוניות - בעיטות ----------------
        //****************************************************
        "external" to "kick" -> mapOf(
            Belt.ORANGE to listOf(
                "הגנה חיצונית נגד בעיטה רגילה",
                "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בימין",
                "הגנה חיצונית נגד בעיטת מגל לפנים - בעיטה בשמאל",
                "הגנה חיצונית נגד בעיטת מגל לפנים - אגרוף בימין",
            ),
            Belt.GREEN to listOf(
                "הגנה חיצונית באמת שמאל נגד בעיטה רגילה",
                "הגנה חיצונית באמת ימין נגד בעיטה לצד",
                "הגנה חיצונית באמת שמאל נגד בעיטה לצד",
            ),
            Belt.BROWN to listOf(
                "הגנה חיצונית נגד בעיטה רגילה – פריצה",
                "הגנה חיצונית נגד בעיטה רגילה – גזיזה",
                "הגנה חיצונית נגד בעיטה רגילה – טאטוא",
                "הגנה חיצונית נגד מגל לפנים – גזיזה",
                "הגנה חיצונית נגד מגל לפנים – טאטוא",
            )
        )

        else -> emptyMap()
    }

    // ✅ אם ביקשו all ויש לנו סעיף קשיח — משתמשים בו
    if (kindN == "all" && hardcoded.isNotEmpty()) {
        val orderedBelts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)
        return orderedBelts.mapNotNull { belt ->
            val items = hardcoded[belt].orEmpty().distinct()
            if (items.isNotEmpty()) belt to items else null
        }
    }

    // ✅ fallback: all = merge פנימיות+חיצוניות
    if (kindN == "all") {
        val internal = itemsFor(kind = "internal", pick = pickN)
        val external = itemsFor(kind = "external", pick = pickN)
        return merge(internal, external)
    }

    val orderedBelts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)

    return orderedBelts.mapNotNull { belt ->
        val items = hardcoded[belt].orEmpty().distinct()
        if (items.isNotEmpty()) belt to items else null
    }
}

// ---------- counts (ברמת קובץ) ----------

internal fun defenseCount(kind: String, pick: String): Int =
    itemsFor(kind = kind, pick = pick)
        .asSequence()
        .flatMap { it.second.asSequence() }
        .map { normItemKey(it) }      // ✅ במקום trim()
        .filter { it.isNotBlank() }
        .distinct()
        .count()
internal fun defenseRootCount(kind: String): Int {
    fun norm(s: String): String = s
        .trim()
        .lowercase()
        .replace("%3a", ":")
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")

    val k = norm(kind)

    // ✅ FIX: פנימיות/חיצוניות = *בעיטות בלבד* (בכל החגורות יחד)
    if (k == "internal" || k.contains("פנימ") || k.startsWith("def_internal")) {
        return defenseCount(kind = "internal", pick = "kick")
    }
    if (k == "external" || k.contains("חיצונ") || k.startsWith("def_external")) {
        return defenseCount(kind = "external", pick = "kick")
    }

    // ✅ all נשאר פנימיות+חיצוניות (אגרופים+בעיטות)
    return defenseCount(kind = kind, pick = "punch") + defenseCount(kind = kind, pick = "kick")
}

// ---------- ✅ DEFENSE COUNTS SOURCE OF TRUTH (single place) ----------

private fun normItemKey(s: String): String =
    s.trim()
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")
        .trim()

/** כל הפריטים של kind/pick כסט ייחודי (מאוחד מכל החגורות) */
internal fun defenseItemSet(kind: String, pick: String): Set<String> =
    itemsFor(kind = kind, pick = pick)
        .asSequence()
        .flatMap { it.second.asSequence() }
        .map { normItemKey(it) }
        .filter { it.isNotBlank() }
        .toSet()

/** ספירת תתי־נושאים של "הגנות נגד בעיטות" מתוך HardSectionsCatalog */
internal fun kicksHardSubCounts(): Map<String, Int> = linkedMapOf(
    "הגנות נגד בעיטות ישרות / למפשעה" to defenseCount("kicks_hard", "straight_groin"),
    "הגנות נגד מגל / מגל לאחור"      to defenseCount("kicks_hard", "hook_back"),
    "הגנות נגד ברך"                  to defenseCount("kicks_hard", "knee"),
)

/** ספירות לדיאלוג הראשי "הגנות" (6 כפתורים) */
internal fun defenseDialogCounts(): Map<String, Int> {
    // ✅ FIX: פנימיות/חיצוניות = אגרופים + בעיטות (כל החגורות יחד)
    val internalRoot = defenseCount("internal", "punch") + defenseCount("internal", "kick")
    val externalRoot = defenseCount("external", "punch") + defenseCount("external", "kick")

    val kicksTotal = defenseItemSet("kicks_hard", "straight_groin")
        .plus(defenseItemSet("kicks_hard", "hook_back"))
        .plus(defenseItemSet("kicks_hard", "knee"))
        .size

    val knifeTotal = defenseCount("knife_hard", "all")
    val gunTotal   = defenseCount("gun_hard", "all")
    val stickTotal = defenseCount("stick_hard", "all")

    return linkedMapOf(
        "הגנות פנימיות"     to internalRoot,
        "הגנות חיצוניות"    to externalRoot,
        "הגנות נגד בעיטות"  to kicksTotal,
        "הגנות מסכין"       to knifeTotal,
        "הגנות מאיום אקדח"  to gunTotal,
        "הגנות נגד מקל"     to stickTotal,
    )
}

/** ספירות לדיאלוג "אגרופים/בעיטות" אחרי שבוחרים פנימי/חיצוני */
internal fun defensePickCounts(): Map<String, Int> = linkedMapOf(
    "INTERNAL:אגרופים" to defenseCount("internal", "punch"),
    "INTERNAL:בעיטות"  to defenseCount("internal", "kick"),
    "EXTERNAL:אגרופים" to defenseCount("external", "punch"),
    "EXTERNAL:בעיטות"  to defenseCount("external", "kick"),
)

/**
 * ספירה כוללת ל-"הגנות" (לשורה הראשית במסך נושאים):
 * מאחד:
 * - פנימי/חיצוני אגרופים+בעיטות (hardcoded)
 * - kicks_hard (3 תתי־נושאים)
 * - סכין/אקדח/מקל (HardSectionsCatalog)
 */
internal fun totalDefenseCount(): Int {
    val all = linkedSetOf<String>()

    // פנימי/חיצוני: אגרופים+בעיטות
    all += defenseItemSet("internal", "punch")
    all += defenseItemSet("internal", "kick")
    all += defenseItemSet("external", "punch")
    all += defenseItemSet("external", "kick")

    // kicks_hard: 3 תתי־נושאים
    all += defenseItemSet("kicks_hard", "straight_groin")
    all += defenseItemSet("kicks_hard", "hook_back")
    all += defenseItemSet("kicks_hard", "knee")

    // נשקים: HardSectionsCatalog
    all += defenseItemSet("knife_hard", "all")
    all += defenseItemSet("gun_hard", "all")
    all += defenseItemSet("stick_hard", "all")

    return all.size
}

// ---------- nav graph ----------

fun NavGraphBuilder.defensesNavGraph(
    nav: NavHostController
) {
    composable(
        route = "defenses/{beltId}/{kind}/{pick}",
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("kind") { type = NavType.StringType },
            navArgument("pick") { type = NavType.StringType },
        )
    ) { entry ->
        val beltIdEnc = entry.arguments?.getString("beltId").orEmpty()
        val kindEnc = entry.arguments?.getString("kind").orEmpty()
        val pickEnc = entry.arguments?.getString("pick").orEmpty()

        val belt = Belt.fromId(Uri.decode(beltIdEnc)) ?: Belt.GREEN

        // ✅ FIX: חובה decode גם ל-kind/pick כי Route.Defenses.make עושה Uri.encode לסגמנטים
        val kindRaw = Uri.decode(kindEnc)
        val pickRaw = Uri.decode(pickEnc)

        fun norm(s: String): String = s
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()

        fun canonKind(raw: String): String {
            val t = norm(raw)
            return when {
                t == "all" -> "all"
                t == "internal" || t.contains("פנימ") -> "internal"
                t == "external" || t.contains("חיצונ") -> "external"
                t == "kicks_hard" -> "kicks_hard"
                t == "releases_hard" -> "releases_hard"
                else -> t
            }
        }

        fun canonPick(raw: String): String {
            val t = norm(raw)

            // kicks_hard
            if (t in setOf("straight_groin", "hook_back", "knee")) return t

            // ✅ releases_hard
            if (t in setOf("hands_hair_shirt", "chokes", "hugs")) return t

            return when {
                t == "punch" || t == "punches" || t.contains("אגרופ") -> "punch"
                t == "kick" || t == "kicks" || t.contains("בעיט") -> "kick"
                else -> t
            }
        }

        val kind = canonKind(kindRaw)
        val pick = canonPick(pickRaw)

        androidx.compose.runtime.LaunchedEffect(kindRaw, pickRaw, kind, pick) {
            android.util.Log.e(
                "KMI_DEF",
                "DEF NAV kindRaw='$kindRaw' pickRaw='$pickRaw' -> kind='$kind' pick='$pick'"
            )
        }

        val grouped = itemsFor(kind = kind, pick = pick)

        // ✅ כותרת מסך (כולל שחרורים)
        val screenTitle = when (kind to pick) {
            "internal" to "punch" -> "הגנות פנימיות - אגרופים"
            "internal" to "kick"  -> "הגנות פנימיות - בעיטות"
            "external" to "punch" -> "הגנות חיצוניות - אגרופים"
            "external" to "kick"  -> "הגנות חיצוניות - בעיטות"
            "all" to "kick"       -> "הגנות נגד בעיטות"
            "all" to "punch"      -> "הגנות נגד אגרופים"

            // ✅ releases_hard
            "releases_hard" to "hands_hair_shirt" -> "שחרור מתפיסות ידיים / שיער / חולצה"
            "releases_hard" to "chokes"           -> "שחרור מחניקות"
            "releases_hard" to "hugs"             -> "שחרור מחביקות גוף"

            else -> "הגנות"
        }

        fun stripPrefixForReleases(full: String): String {
            val t = full.trim()
            if (kind != "releases_hard") return t

            // מוריד prefix שמתחיל בשם תת־הנושא (אם הוא הוכנס לפריט עצמו)
            val prefixes = listOf(
                "שחרור מתפיסות ידיים / שיער / חולצה - ",
                "שחרור מתפיסות ידיים / שיער / חולצה – ",
                "שחרור מתפיסות ידיים / שיער / חולצה: ",
                "שחרור מחניקות - ",
                "שחרור מחניקות – ",
                "שחרור מחניקות: ",
                "שחרור מחביקות גוף - ",
                "שחרור מחביקות גוף – ",
                "שחרור מחביקות גוף: "
            )

            val hit = prefixes.firstOrNull { t.startsWith(it) }
            return if (hit != null) t.removePrefix(hit).trim() else t
        }

        DefensesListScreen(
            title = screenTitle,
            groupedItems = grouped,
            itemTitle = { raw ->
                stripPrefixForReleases(displayName(raw))
            },
            onBack = { nav.popBackStack() }
        )
    }
}