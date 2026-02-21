package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.shared.domain.Belt
import il.kmi.app.screens.BeltQuestions.BeltQuestionsByTopicScreen as BeltQuestionsByTopicScreen
import il.kmi.app.training.TrainingCatalog
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import il.kmi.app.favorites.FavoritesStore
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import il.kmi.app.free_sessions.ui.navigation.FreeSessionsRoute
import il.kmi.app.screens.coach.InternalExamEntryScreen
import il.kmi.app.ui.BirthdayGate
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.runtime.getValue
import il.kmi.app.screens.ExercisesTabsScreen
import il.kmi.app.screens.FavoritesScreen
import il.kmi.app.screens.HomeScreen
import il.kmi.app.screens.TopicsScreen

// ✅ היה private -> חייב להיות ציבורי כדי ש-TopicsNavGraph יוכל להשתמש
const val TOPICS_PICK_TOKEN = "__TOPICS_PICK__"

private fun encTopicForToken(s: String): String =
    java.net.URLEncoder.encode(s, Charsets.UTF_8.name()).replace("+", "%20")

/**
 * פורמט:
 * __TOPICS_PICK__:<beltId>|<topicEnc>,<topicEnc>;<beltId>|<topicEnc>
 */
fun buildTopicsPickToken(selection: il.kmi.app.screens.PracticeByTopicsSelection): String {
    val segments = selection.topicsByBelt.entries
        .sortedBy { (belt, _) ->
            Belt.order.indexOf(belt).let { idx -> if (idx >= 0) idx else 999 }
        }
        .mapNotNull { (belt, topics) ->
            val cleaned = topics
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            if (cleaned.isEmpty()) null
            else "${belt.id}|${cleaned.joinToString(",") { encTopicForToken(it) }}"
        }

    return "$TOPICS_PICK_TOKEN:${segments.joinToString(";")}"
}

/**
 * גרף למסכי הבית/שאלות חגורה/נושאים, כולל מסך "כל הרשימות" ex_tabs_all/{beltId}.
 * חתימה נשמרת תואמת ל־MainNavHost (כולל sp/kmiPrefs) לצמצום שינויים.
 */
@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.homeNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {

    // ---- מסך הבית ----
    composable(Route.Home.route) {
        val userRegion = kmiPrefs.region.orEmpty()
        val userBranch = kmiPrefs.branch.orEmpty()
        val userGroupRaw = kmiPrefs.ageGroup.orEmpty()
        val userGroup = TrainingCatalog.normalizeGroupName(userGroupRaw)

        val trainingsForUser: List<il.kmi.app.training.TrainingData> =
            remember(userRegion, userBranch, userGroup) {
                if (userRegion.isNotBlank() && userBranch.isNotBlank() && userGroup.isNotBlank()) {
                    TrainingCatalog.upcomingFor(userRegion, userBranch, userGroup, count = 3)
                } else emptyList()
            }

        val explainTriple = remember { mutableStateOf<Triple<Belt, String, String>?>(null) }

        BirthdayGate(sp = sp) {
            HomeScreen(
                onContinue = {
                    nav.navigate(Route.BeltQ.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(Route.Home.route) {
                            inclusive = false
                            saveState = true
                        }
                    }
                },
                onSettings = { nav.navigate(Route.Settings.route) },
                trainings = trainingsForUser,

                onOpenFreeSessions = { branch, groupKey, uid, name ->
                    nav.navigate(
                        FreeSessionsRoute.build(
                            branch = branch,
                            groupKey = groupKey,
                            uid = uid,
                            name = name
                        )
                    ) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },

                onOpenExercise = { key: String ->
                    fun dec(s: String) =
                        try {
                            java.net.URLDecoder.decode(s, "UTF-8")
                        } catch (_: Exception) {
                            s
                        }

                    val parts = when {
                        '|' in key -> key.split('|', limit = 3)
                        "::" in key -> key.split("::", limit = 3)
                        '/' in key -> key.split('/', limit = 3)
                        else -> listOf("", "", "")
                    }.map(::dec)

                    val belt = Belt.fromId(parts.getOrNull(0).orEmpty()) ?: Belt.WHITE
                    val topic = parts.getOrNull(1).orEmpty().trim()
                    var item = parts.getOrNull(2).orEmpty().trim()
                    if (item.startsWith("$topic::")) item = item.removePrefix("$topic::")

                    val display = ExerciseTitleFormatter.displayName(item).ifBlank { item }.trim()
                    item = display
                    item = item.replace(Regex(":+"), ":").replace(Regex("\\s+"), " ").trim()

                    explainTriple.value = Triple(belt, topic, item)
                },

                onOpenMonthlyCalendar = {
                    nav.navigate(Route.MonthlyCalendar.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenTrainingSummary = {
                    nav.navigate(Route.TrainingSummary.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            explainTriple.value?.let { (b, t, iRaw) ->
                val canonical = resolveCanonicalItemTitle(
                    belt = b,
                    topicTitle = t,
                    itemRaw = iRaw
                )

                val explanation = il.kmi.app.domain.Explanations.get(b, canonical).ifBlank {
                    val alt = canonical.substringAfter(":", canonical).trim()
                    il.kmi.app.domain.Explanations.get(b, alt)
                }.ifBlank { "לא נמצא הסבר עבור \"$canonical\"." }

                ExplanationDialogWithFavorite(
                    belt = b,
                    topic = t,
                    itemTitle = canonical,
                    explanation = explanation,
                    onDismiss = { explainTriple.value = null }
                )
            }
        }
    }

    // ---- יעד "תרגיל" (exercise/{id}) ----
    composable(
        route = Route.Exercise.route,
        arguments = listOf(navArgument(name = "id") { type = NavType.StringType })
    ) { backStackEntry ->
        val idEnc = backStackEntry.arguments?.getString("id").orEmpty()

        val itemTitle = remember(key1 = idEnc) {
            runCatching { java.net.URLDecoder.decode(idEnc, "UTF-8") }.getOrDefault(idEnc)
                .replace(oldValue = "+", newValue = " ")
                .trim()
        }

        val belt = remember {
            runCatching { vm.selectedBelt.value }.getOrNull() ?: Belt.GREEN
        }

        val explanation = remember(itemTitle, belt) {
            il.kmi.app.domain.Explanations.get(belt, itemTitle).ifBlank {
                val alt = itemTitle.substringAfter(":", itemTitle).trim()
                il.kmi.app.domain.Explanations.get(belt, alt)
            }.ifBlank { "לא נמצא הסבר עבור \"$itemTitle\"." }
        }

        ExplanationDialogWithFavorite(
            belt = belt,
            topic = "",
            itemTitle = itemTitle,
            explanation = explanation,
            onDismiss = { nav.popBackStack() }
        )
    }

    // ---- מסך מועדפים (Favorites) ----
    composable(Route.Favorites.route) {
        FavoritesScreen(
            onHome = { nav.navigate(Route.Home.route) }
        )
    }

    // ---- תרגילים לפי נושא (BeltQByTopic) ----
    composable(Route.BeltQByTopic.route) {

        BeltQuestionsByTopicScreen(

            onOpenSubject = { belt: Belt, subject: il.kmi.app.domain.SubjectTopic ->
                vm.setSelectedBelt(belt)

                nav.navigate(
                    Route.SubjectExercises.make(
                        subjectId = subject.id,
                        beltId = belt.id,
                        title = subject.titleHeb
                    )
                )
            },

            onOpenTopic = { belt: Belt, topicTitle: String ->
                vm.setSelectedBelt(belt)

                // ✅ FIX: לפתוח את Materials (ולא topic_repo)
                nav.navigate(
                    Route.Materials.make(
                        belt = belt,
                        topic = topicTitle
                    )
                ) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // ✅ FIX (שינוי 1): הגנות -> לא Route.Defenses (לא קיים בגרף), אלא SubjectExercises (כן קיים)
            onOpenDefenseList = { belt, kind, pick ->
                vm.setSelectedBelt(belt)

                val subjectId = "def_${kind}_${pick}" // למשל: def_internal_punch
                val title = when (kind) {
                    "internal" -> if (pick == "punch") "הגנות פנימיות - אגרופים" else "הגנות פנימיות - בעיטות"
                    "external" -> if (pick == "punch") "הגנות חיצוניות - אגרופים" else "הגנות חיצוניות - בעיטות"
                    else -> "הגנות"
                }

                Log.e("KMI_TOPICS", "onOpenDefenseList belt=${belt.id} kind=$kind pick=$pick subjectId=$subjectId")

                nav.navigate(
                    Route.SubjectExercises.make(
                        subjectId = Uri.encode(subjectId),
                        beltId = belt.id,
                        title = title
                    )
                ) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onBackHome = {
                nav.navigate(Route.Home.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }

    // ---- מסך נושאים ----
    composable(Route.Topics.route) {
        val explainTriple = remember { mutableStateOf<Triple<Belt, String, String>?>(null) }

        TopicsScreen(
            vm = vm,

            // ✅ FIX: גם כאן TopicExercises (לא topic_repo, לא Materials)
            onOpenTopic = { belt, topic ->
                nav.navigate(Route.Materials.make(belt = belt, topic = topic)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // ✅ FIX: תפריט הגנות -> פותח Materials על "הגנות" בלי לבחור תת־נושא ראשון
            onOpenDefenseMenu = onOpenDefenseMenu@{ belt, topic ->
                val catalogTopic = "הגנות"

                val topicEnc = Uri.encode(catalogTopic)

                nav.navigate(
                    Route.Materials.make(
                        belt = belt,
                        topic = topicEnc
                    )
                ) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onSummary = { belt -> nav.navigate(Route.Summary.make(belt)) },
            onRandomPractice = { belt -> nav.navigate(Route.Practice.make(belt)) },
            onPracticeByTopics = { selection ->
                val token = buildTopicsPickToken(selection)
                val baseBelt = selection.belts
                    .sortedBy { Belt.order.indexOf(it).let { idx -> if (idx >= 0) idx else 999 } }
                    .firstOrNull() ?: Belt.GREEN

                nav.navigate(Route.Practice.make(baseBelt, token)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onBack = { nav.popBackStack() },
            onOpenSettings = { nav.navigate(Route.Settings.route) },
            onExam = { belt -> nav.navigate(Route.Exam.make(belt)) },

            onOpenWeakPoints = {
                // TODO: חבר למסך WeakPoints כשיש Route מוכן
            },

            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.startDestinationId) {
                        inclusive = false
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenLists = { belt -> nav.navigate("ex_tabs_all/${belt.id}") },

            onOpenExercise = { key ->
                fun dec(s: String) =
                    try {
                        java.net.URLDecoder.decode(s, "UTF-8")
                    } catch (_: Exception) {
                        s
                    }

                val resolved =
                    runCatching { il.kmi.app.domain.ContentRepo.resolveItemKey(key) }.getOrNull()
                if (resolved != null) {
                    explainTriple.value =
                        Triple(resolved.belt, resolved.topicTitle, resolved.itemTitle)
                } else {
                    val parts = when {
                        key.contains('|') -> key.split('|', limit = 3)
                        key.contains("::") -> key.split("::", limit = 3)
                        key.contains('/') -> key.split('/', limit = 3)
                        else -> listOf("", "", "")
                    }.map(::dec)
                    val b = Belt.fromId(parts.getOrNull(0).orEmpty()) ?: Belt.WHITE
                    val t = parts.getOrNull(1).orEmpty().trim()
                    var i = parts.getOrNull(2).orEmpty().trim()
                    if (i.startsWith("$t::")) i = i.removePrefix("$t::")
                    if ("::" in i) i = i.substringAfterLast("::")
                    i = i.replace(Regex(":+"), ":").replace(Regex("\\s+"), " ").trim()
                    explainTriple.value = Triple(b, t, i)
                }
            }
        )

        explainTriple.value?.let { (b, t, iRaw) ->
            val canonical = resolveCanonicalItemTitle(
                belt = b,
                topicTitle = t,
                itemRaw = iRaw
            )

            val explanation = il.kmi.app.domain.Explanations.get(b, canonical).ifBlank {
                val alt = canonical.substringAfter(":", canonical).trim()
                il.kmi.app.domain.Explanations.get(b, alt)
            }.ifBlank { "לא נמצא הסבר עבור \"$canonical\"." }

            ExplanationDialogWithFavorite(
                belt = b,
                topic = t,
                itemTitle = canonical,
                explanation = explanation,
                onDismiss = { explainTriple.value = null }
            )
        }
    }

    // ---- מסך מבחן פנימי ----
    composable(route = Route.InternalExam.route) {
        InternalExamEntryScreen(
            onBack = { nav.popBackStack() }
        )
    }

    // ---- מסך "כל הרשימות" (ex_tabs_all/{beltId}) ----
    composable(
        route = "ex_tabs_all/{beltId}",
        arguments = listOf(navArgument("beltId") { type = NavType.StringType })
    ) { backStackEntry ->
        val beltId = backStackEntry.arguments?.getString("beltId").orEmpty()
        val belt = Belt.fromId(beltId) ?: Belt.WHITE

        ExercisesTabsScreen(
            vm = vm,
            belt = belt,
            topic = "__ALL__",
            onPractice = { b, t -> nav.navigate(Route.Practice.make(b, t)) },
            subTopicFilter = null,
            onHome = {
                val popped = nav.popBackStack(
                    Route.Home.route,
                    inclusive = false
                )
                if (!popped) {
                    nav.navigate(Route.Home.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            onSearch = {
                nav.navigate(Route.Topics.route) { launchSingleTop = true }
            }
        )
    }
}

/* =========================
   Canonical resolver (NEW API)
   ========================= */
private fun resolveCanonicalItemTitle(
    belt: Belt,
    topicTitle: String,
    itemRaw: String
): String {
    fun norm(s: String) = s
        .replace("\u200F", "").replace("\u200E", "").replace("\u00A0", " ")
        .replace(Regex("[\u0591-\u05C7]"), "")
        .replace('[', ' ').replace(']', ' ')
        .replace("[\\-–—:_]".toRegex(), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .lowercase()

    var cleaned = itemRaw.trim()
    if (cleaned.startsWith("$topicTitle::")) cleaned = cleaned.removePrefix("$topicTitle::")

    cleaned = ExerciseTitleFormatter.displayName(cleaned).ifBlank { cleaned }.trim()
    cleaned = cleaned.replace(Regex(":+"), ":").replace(Regex("\\s+"), " ").trim()

    val wanted = norm(cleaned)

    val direct = runCatching {
        il.kmi.app.domain.ContentRepo.listItemTitles(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = null
        )
    }.getOrDefault(emptyList())

    direct.firstOrNull { raw ->
        val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
        norm(disp) == wanted || norm(raw) == wanted
    }?.let { raw ->
        return ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
    }

    val subTitles = runCatching {
        il.kmi.app.domain.ContentRepo.listSubTopicTitles(belt, topicTitle)
    }.getOrDefault(emptyList())

    for (st in subTitles) {
        val items = runCatching {
            il.kmi.app.domain.ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = topicTitle,
                subTopicTitle = st
            )
        }.getOrDefault(emptyList())

        items.firstOrNull { raw ->
            val disp = ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
            norm(disp) == wanted || norm(raw) == wanted
        }?.let { raw ->
            return ExerciseTitleFormatter.displayName(raw).ifBlank { raw }.trim()
        }
    }

    return cleaned
}

/* =========================
   Shared dialog (top-level)
   ========================= */
@Composable
private fun ExplanationDialogWithFavorite(
    belt: Belt,
    topic: String,
    itemTitle: String,
    explanation: String,
    onDismiss: () -> Unit
) {
    val favorites: Set<String> by FavoritesStore.favoritesFlow.collectAsState(initial = emptySet())

    fun normalizeFavId(raw: String): String =
        raw.substringAfter("::", raw)
            .substringAfter(":", raw)
            .trim()

    val favId = remember(itemTitle) { normalizeFavId(itemTitle) }
    val isFav: Boolean = favorites.contains(favId)

    fun toggleFavorite() {
        FavoritesStore.toggle(favId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { toggleFavorite() }) {
                    if (isFav) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "הסר ממועדפים",
                            tint = Color(0xFFFFC107)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.StarBorder,
                            contentDescription = "הוסף למועדפים"
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = itemTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${belt.heb}${if (topic.isNotBlank()) " · $topic" else ""})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        text = {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("סגור") } }
    )
}
