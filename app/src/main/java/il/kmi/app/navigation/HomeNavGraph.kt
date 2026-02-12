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
import il.kmi.app.screens.BeltQuestions.BeltQuestionsByBeltScreen
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

        // ▼ מצב להצגת פופ־אפ הסבר (belt, topic, item)
        val explainTriple = remember { mutableStateOf<Triple<Belt, String, String>?>(null) }

        // 🎂 שער יום הולדת + מסך הבית + דיאלוגי הסבר
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

                // ✅ NEW: ניווט למסך אימונים חופשיים עם פרמטרים מלאים
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
                    // שומר התוצאה להצגת דיאלוג (כמו בקוד המקורי)
                    fun dec(s: String) =
                        try {
                            java.net.URLDecoder.decode(s, "UTF-8")
                        } catch (_: Exception) {
                            s
                        }

                    val parts = when {
                        '|' in key  -> key.split('|', limit = 3)
                        "::" in key -> key.split("::", limit = 3)
                        '/' in key  -> key.split('/', limit = 3)
                        else        -> listOf("", "", "")
                    }.map(::dec)

                    val belt  = Belt.fromId(parts.getOrNull(0).orEmpty()) ?: Belt.WHITE
                    val topic = parts.getOrNull(1).orEmpty().trim()
                    var item  = parts.getOrNull(2).orEmpty().trim()
                    if (item.startsWith("$topic::")) item = item.removePrefix("$topic::")

                    val display = ExerciseTitleFormatter.displayName(item).ifBlank { item }.trim()
                    item = display
                    item = item.replace(Regex(":+"), ":").replace(Regex("\\s+"), " ").trim()

                    explainTriple.value = Triple(belt, topic, item)
                },

                // ✅ NEW: הוספת שני הפרמטרים החסרים
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

            // דיאלוג ההסבר – ממוחזר 1:1 מהקוד שהיה ב-MainApp
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
        } // ✅ סוגר BirthdayGate
    }     // ✅ סוגר composable(Route.Home.route)

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

        // GREEN — כרגע נשתמש בחגורה שנבחרה במערכת, ואם אין — GREEN
        val belt = remember {
            runCatching { vm.selectedBelt.value }.getOrNull() ?: Belt.GREEN
        }

        val explanation = remember(itemTitle, belt) {
            il.kmi.app.domain.Explanations.get(belt, itemTitle).ifBlank {
                val alt = itemTitle.substringAfter(":", itemTitle).trim()
                il.kmi.app.domain.Explanations.get(belt, alt)
            }.ifBlank { "לא נמצא הסבר עבור \"$itemTitle\"." }
        }

        // מציג דיאלוג הסבר + מועדפים, וסוגר בחזרה אחורה
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

    // ---- בחירת חגורה (BeltQ) ----
    composable(Route.BeltQ.route) {
        val explainTriple = remember { mutableStateOf<Triple<Belt, String, String>?>(null) }

        val isCoach = remember {
            val role = (sp.getString("user_role", "") ?: "").lowercase()
            role == "coach" || role.contains("coach") || role.contains("מאמן") || role.contains("מדריך")
        }

        BeltQuestionsByBeltScreen(
            vm = vm,
            kmiPrefs = kmiPrefs,
            isCoach = isCoach,
            onNext = { nav.navigate(Route.Topics.route) },
            onBackHome = { nav.navigate(Route.Home.route) { popUpTo(0) } },
            onOpenExercise = { key ->
                fun dec(s: String) =
                    try { java.net.URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

                val resolved =
                    runCatching { il.kmi.app.domain.ContentRepo.resolveItemKey(key) }.getOrNull()

                if (resolved != null) {
                    explainTriple.value = Triple(resolved.belt, resolved.topicTitle, resolved.itemTitle)
                } else {
                    val parts = when {
                        '|' in key -> key.split('|', limit = 3)
                        "::" in key -> key.split("::", limit = 3)
                        '/' in key -> key.split('/', limit = 3)
                        else -> listOf("", "", "")
                    }.map(::dec)

                    val beltFromKey = Belt.fromId(parts.getOrNull(0).orEmpty())
                    val topic = parts.getOrNull(1).orEmpty().trim()
                    var item = parts.getOrNull(2).orEmpty().trim()
                    if (item.startsWith("$topic::")) item = item.removePrefix("$topic::")

                    item = ExerciseTitleFormatter.displayName(item).ifBlank { item }.trim()
                    item = item.replace(Regex(":+"), ":").replace(Regex("\\s+"), " ").trim()

                    val beltResolved = beltFromKey
                        ?: il.kmi.app.search.KmiSearchBridge.resolveBeltByTopicItem(
                            topicTitle = topic,
                            itemTitle = item,
                            hint = null
                        )

                    explainTriple.value = Triple(beltResolved, topic, item)
                }
            },

            onOpenTopic = { belt, topic ->
                val topicEnc = Uri.encode(topic)
                val subs: List<String> = il.kmi.app.domain.AppSubTopicRegistry.getSubTopicsFor(belt, topic)

                val hasRealSubs = subs.any { st ->
                    val t = st.trim()
                    t.isNotEmpty() && !t.equals(topic.trim(), ignoreCase = true)
                }

                val route =
                    if (hasRealSubs) Route.SubTopics.make(belt, topicEnc)
                    else Route.Materials.make(belt, topicEnc)

                runCatching { nav.navigate(route) }
            },

            onOpenDefenseMenu = { belt, topic ->
                val topicEnc = Uri.encode(topic)
                nav.navigate(Route.SubTopics.make(belt, topicEnc))
            },

            onOpenSubject = { subject ->
                val route = Route.SubjectExercises.make(
                    subjectId = subject.id,   // ✅ לא מקודדים כאן כדי לא לעשות encoding כפול
                    beltId = "",
                    title = subject.titleHeb
                )
                nav.navigate(route)
            },

            onOpenVoiceAssistant = { _ ->
                nav.navigate(Route.VoiceAssistant.route) {
                    launchSingleTop = true
                }
            },

            onOpenPdfMaterials = { belt ->
                // אם יש לך כבר פעולה קיימת למסמכי PDF — שים אותה כאן.
                // כרגע משאיר ניווט למסך חומרים (אם זה מה שאתה רוצה), או שנה לפי הצורך.
                nav.navigate(Route.Materials.make(belt, topic = "")) {
                    launchSingleTop = true
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
    } // ✅ סוף BeltQ

    // ---- תרגילים לפי נושא (BeltQByTopic) ----
    composable(Route.BeltQByTopic.route) {
        BeltQuestionsByTopicScreen(
            onOpenSubject = { belt, subject ->
                vm.setSelectedBelt(belt)

                val safeId = Uri.encode(subject.id)
                val title = Uri.encode(subject.titleHeb)
                nav.navigate("subject_exercises/$safeId?beltId=${belt.id}&title=$title")
            },
            onBackHome = {
                nav.navigate(Route.Home.route) { popUpTo(id = 0) }
            }
        )
    }

    // ---- מסך נושאים ----
    composable(Route.Topics.route) {
        val explainTriple = remember { mutableStateOf<Triple<Belt, String, String>?>(null) }

        TopicsScreen(
            vm = vm,
            onOpenTopic = { belt, topic ->
                // לבדוק תתי־נושאים מה־shared דרך העטיפה של האפליקציה
                val subs: List<String> =
                    il.kmi.app.domain.AppSubTopicRegistry.getSubTopicsFor(belt, topic)
                val hasRealSubs = subs.isNotEmpty()

                if (hasRealSubs) {
                    nav.navigate(Route.SubTopics.make(belt, topic)) {
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    nav.navigate(Route.Materials.make(belt, topic)) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            onOpenDefenseMenu = { belt, topic ->
                nav.navigate(Route.SubTopics.make(belt, topic))
            },
            onSummary = { belt -> nav.navigate(Route.Summary.make(belt)) },
            onRandomPractice = { belt -> nav.navigate(Route.Practice.make(belt)) },

            // ✅ NEW: תרגול לפי נושא/נושאים (כולל כמה חגורות)
            onPracticeByTopics = { selection ->
                val token = buildTopicsPickToken(selection)

                // בוחרים חגורה “בסיסית” לצבע/כותרת: הראשונה שנבחרה, אחרת GREEN
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

            // ✅ FIX: החתימה היא () -> Unit (ללא פרמטרים)
            onOpenWeakPoints = {
                // TODO: חבר למסך WeakPoints כשיש Route מוכן
                // כרגע no-op כדי לא להפיל קומפילציה.
            },

            // ⬅️ כאן התיקון: ניקוי עד יעד ההתחלה של הגרף ואז ניווט לבית
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
                // ✅ לחיצה אחת תמיד מחזירה למסך הבית
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

    // תמיד מיישרים לשם תצוגה אחיד (גם אם מגיע def:* או topic::item)
    cleaned = ExerciseTitleFormatter.displayName(cleaned).ifBlank { cleaned }.trim()
    cleaned = cleaned.replace(Regex(":+"), ":").replace(Regex("\\s+"), " ").trim()

    val wanted = norm(cleaned)

    // 1) פריטים ישירים של נושא
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

    // 2) פריטים בתוך תתי-נושאים
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
    // ✅ מועדפים גלובליים – source of truth אחד
    val favorites: Set<String> by FavoritesStore.favoritesFlow.collectAsState(initial = emptySet())

    fun normalizeFavId(raw: String): String =
        raw.substringAfter("::", raw)
            .substringAfter(":", raw)
            .trim()

    val favId = remember(itemTitle) { normalizeFavId(itemTitle) }
    val isFav: Boolean = favorites.contains(favId)

    fun toggleFavorite() {
        // ✅ כרגע FavoritesStore הוא גלובלי (סט אחד לכל האפליקציה) ולכן אין כאן topicFilter.
        // אם תרצה מועדפים "לפי נושא" צריך להרחיב את FavoritesStore (scope/topic) ואז לקרוא לזה כאן.
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
