package il.kmi.app.navigation.defenses

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.shared.domain.Belt

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

    val hardcoded: Map<Belt, List<String>> = when (kind to pick) {

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
                "הגנה נגד בעיטה רגילה - סייד סטפ לצד המת",
                "הגנה נגד בעיטה רגילה - סייד סטפ לצד החי",
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

                // אם אתה רוצה לכלול גם פנימיות בתוך "הגנות נגד בעיטות" (ALL) — תשאיר פה:
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
                "הגנה חיצונית נגד מכה גבוהה מהצד - התוקף בצד שמאל",
                "הגנה חיצונית נגד מכה גבוהה מהצד לעורף - התוקף בצד שמאל",
                "הגנה חיצונית נגד מכה מהצד לגב - התוקף בצד שמאל",
                "הגנה חיצונית נגד מכה גבוהה מהצד - התוקף בצד ימין",
                "הגנה חיצונית נגד מכה מהצד לגרון - התוקף בצד ימין",
                "הגנה חיצונית נגד מכה מהצד לבטן - התוקף בצד ימין",
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
        // ✅ השאר פה רק חיצוניות "טהור", בלי פנימיות
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
    if (kind == "all" && hardcoded.isNotEmpty()) {
        val orderedBelts = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)
        return orderedBelts.mapNotNull { belt ->
            val items = hardcoded[belt].orEmpty().distinct()
            if (items.isNotEmpty()) belt to items else null
        }
    }

    // ✅ fallback: all = merge פנימיות+חיצוניות
    if (kind == "all") {
        val internal = itemsFor(kind = "internal", pick = pick)
        val external = itemsFor(kind = "external", pick = pick)
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
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .count()

internal fun defenseRootCount(kind: String): Int =
    defenseCount(kind = kind, pick = "punch") + defenseCount(kind = kind, pick = "kick")

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
        val kindRaw = entry.arguments?.getString("kind").orEmpty()
        val pickRaw = entry.arguments?.getString("pick").orEmpty()

        val belt = Belt.fromId(Uri.decode(beltIdEnc)) ?: Belt.GREEN

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
                else -> t
            }
        }

        fun canonPick(raw: String): String {
            val t = norm(raw)
            return when {
                t == "punch" || t.contains("אגרופ") -> "punch"
                t == "kick" || t.contains("בעיט") -> "kick"
                else -> t
            }
        }

        val kind = canonKind(kindRaw)
        val pick = canonPick(pickRaw)

        val grouped = itemsFor(kind = kind, pick = pick)

        DefensesListScreen(
            title = when (kind to pick) {
                "internal" to "punch" -> "הגנות פנימיות - אגרופים"
                "internal" to "kick"  -> "הגנות פנימיות - בעיטות"
                "external" to "punch" -> "הגנות חיצוניות - אגרופים"
                "external" to "kick"  -> "הגנות חיצוניות - בעיטות"
                "all" to "kick"       -> "הגנות נגד בעיטות"
                "all" to "punch"      -> "הגנות נגד אגרופים"
                else -> "הגנות"
            },
            groupedItems = grouped,
            itemTitle = { displayName(it) },
            onBack = { nav.popBackStack() }
        )
    }
}