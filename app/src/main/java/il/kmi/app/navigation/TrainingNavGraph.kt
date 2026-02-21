package il.kmi.app.navigation

import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.exercises.TopicRepoExercisesScreen
import il.kmi.app.screens.BeltQuestions.BeltQuestionsByBeltScreen
import il.kmi.app.screens.PracticeByTopicsSelection
import il.kmi.shared.domain.Belt

/**
 * גרף “אימון/תוכן”.
 */
fun NavGraphBuilder.trainingNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {

    // ---- בחירת חגורה (BeltQ) ----
    composable(Route.BeltQ.route) {

        val isCoach = remember {
            val role = (sp.getString("user_role", "") ?: "").lowercase()
            role == "coach" ||
                    role.contains("coach") ||
                    role.contains("מאמן") ||
                    role.contains("מדריך")
        }

        BeltQuestionsByBeltScreen(
            vm = vm,
            kmiPrefs = kmiPrefs,
            isCoach = isCoach,

            onNext = {
                nav.navigate(Route.Topics.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onBackHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) { inclusive = true }
                    launchSingleTop = true
                }
            },

            // ✅ פתיחת נושא רגיל (לפי חגורה) -> נשאר MaterialsScreen המעוצב
            onOpenTopic = { belt, topic ->
                nav.navigate(Route.Materials.make(belt = belt, topic = topic)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // ✅ הגנות: פותח דרך TopicRepoExercises
            // ✅ הגנות: פותח דרך TopicRepoExercises
            onOpenDefenseMenu = { belt, topic ->

                // ✅ NEW: אם הגיע "kind:pick" (ממצב "לפי נושא") -> פתח מסך Defenses
                val parts = topic.split(":", limit = 2)
                if (parts.size == 2) {
                    val kind = parts[0].trim()
                    val pick = parts[1].trim()

                    Log.e("DEF_DEBUG", "NAVIGATE -> Defenses belt=${belt.id} kind='$kind' pick='$pick'")

                    nav.navigate(Route.Defenses.make(belt = belt, kind = kind, pick = pick)) {
                        launchSingleTop = true
                        restoreState = true
                    }
                    return@BeltQuestionsByBeltScreen
                }

                val subs: List<String> =
                    il.kmi.app.domain.AppSubTopicRegistry.getSubTopicsFor(belt, topic)

                val cleanSubs = subs
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.equals(topic.trim(), ignoreCase = true) }

                val pickedSub = cleanSubs.firstOrNull() ?: return@BeltQuestionsByBeltScreen

                // ✅ חשוב: בקטלוג ה-topic האמיתי הוא "הגנות"
                val catalogTopic = "הגנות"

                val route = Route.TopicRepoExercises.make(
                    belt = belt,
                    topicId = catalogTopic,  // ✅ בלי Uri.encode
                    subTopicId = pickedSub    // ✅ בלי Uri.encode
                )

                Log.e("DEF_DEBUG", "NAVIGATE -> TopicRepoExercises belt=${belt.id} uiTopic='$topic' catalogTopic='$catalogTopic' sub='$pickedSub' route='$route'")

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenSubTopic = { belt, topic, subTopic ->
                val clean = subTopic.trim()
                if (clean.isNotEmpty()) {
                    val isDefenseTopic = topic.contains("הגנות")

                    if (isDefenseTopic) {
                        val catalogTopic = "הגנות"

                        val fixedSub = when {
                            clean.contains("בעיט") &&
                                    (clean.contains("פנימ") || clean.contains("חיצונ")) ->
                                "הגנות נגד בעיטות"
                            else -> clean
                        }

                        // ✅ FIX: תתי־נושאים של "הגנות" חייבים להיפתח ב-MaterialsScreen (כרטיסיות)
                        val route = Route.MaterialsSub.make(
                            belt = belt,
                            topic = catalogTopic,
                            subTopic = fixedSub
                        )

                        Log.e(
                            "DEF_DEBUG",
                            "NAVIGATE -> MaterialsSub belt=${belt.id} uiTopic='$topic' catalogTopic='$catalogTopic' sub='$fixedSub' route='$route'"
                        )

                        nav.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {

                        // ✅ FIX: מגל+סנוקרת נקרא ב-ContentRepo "מגל + סנוקרת"
                        val fixedSubTopic = when {
                            clean.contains("מגל") && clean.contains("סנוקרת") -> "מגל + סנוקרת"
                            else -> clean
                        }

                        nav.navigate(
                            Route.MaterialsSub.make(
                                belt = belt,
                                topic = topic,
                                subTopic = fixedSubTopic
                            )
                        ) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            },

            onOpenSubject = { subject ->
                val safeId = Uri.encode(subject.id)
                val route = Route.SubjectExercises.make(
                    subjectId = safeId,
                    beltId = "",
                    title = subject.titleHeb
                )

                Log.e(
                    "KMI-TITLE",
                    "NAV -> SubjectExercises: subjectId='${subject.id}' title='${subject.titleHeb}' route='$route'"
                )

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenExercise = { _ ->
                // נשאר כמו שהיה אצלך
            },

            onOpenWeakPoints = { _ ->
                nav.navigate(Route.WeakPoints.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenAllLists = { belt ->
                runCatching { nav.navigate(route = "ex_tabs_all/${belt.id}") }
            },

            onOpenRandomPractice = { belt ->
                nav.navigate(Route.Practice.make(belt)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenFinalExam = { belt ->
                nav.navigate(Route.Exam.make(belt)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onPracticeByTopics = { selection: PracticeByTopicsSelection ->
                Log.d("KMI-NAV", "PracticeByTopics selection=$selection")
            },

            onOpenSummaryScreen = { belt ->
                nav.navigate(Route.Summary.make(belt)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenVoiceAssistant = { _ ->
                nav.navigate(Route.VoiceAssistant.route) { launchSingleTop = true }
            },

            onOpenPdfMaterials = { belt ->
                nav.navigate(Route.Materials.make(belt, topic = "")) { launchSingleTop = true }
            },
        )
    }

    // ---- Materials (מסך התרגילים המעוצב) ----
    composable(
        route = Route.Materials.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topic") { type = NavType.StringType },
            navArgument("coach") {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { entry ->
        val beltId = entry.arguments?.getString("beltId").orEmpty()
        val topicEnc = entry.arguments?.getString("topic").orEmpty()
        val coach = entry.arguments?.getBoolean("coach") ?: false

        val belt = Belt.fromId(beltId) ?: Belt.GREEN
        val topic = Uri.decode(topicEnc)

        // חשוב כדי לשמור עקביות בין מסכים
        vm.setSelectedBelt(belt)

        il.kmi.app.screens.MaterialsScreen(
            vm = vm,
            belt = belt,
            topic = topic,
            subTopicFilter = null,
            onBack = { nav.popBackStack() },
            onSummary = { b, t, st ->
                nav.navigate(Route.Summary.make(b, t, st)) { launchSingleTop = true }
            },
            onPractice = { b, t ->
                nav.navigate(Route.Practice.make(b, t)) { launchSingleTop = true }
            },
            onOpenSettings = { nav.navigate(Route.Settings.route) { launchSingleTop = true } },
            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    // ---- MaterialsSub (אותו מסך, עם תת־נושא) ----
    composable(
        route = Route.MaterialsSub.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topic") { type = NavType.StringType },
            navArgument("subTopic") { type = NavType.StringType }
        )
    ) { entry ->
        val beltId = entry.arguments?.getString("beltId").orEmpty()
        val topicEnc = entry.arguments?.getString("topic").orEmpty()
        val subEnc = entry.arguments?.getString("subTopic").orEmpty()

        val belt = Belt.fromId(beltId) ?: Belt.GREEN
        val topic = Uri.decode(topicEnc)
        val subTopicRaw = Uri.decode(subEnc).trim()

        // ✅ FIX: מגל+סנוקרת נקרא ב-ContentRepo "מגל + סנוקרת"
        val subTopic = when {
            subTopicRaw.contains("מגל") && subTopicRaw.contains("סנוקרת") -> "מגל + סנוקרת"
            else -> subTopicRaw
        }

        vm.setSelectedBelt(belt)

        il.kmi.app.screens.MaterialsScreen(
            vm = vm,
            belt = belt,
            topic = topic,
            subTopicFilter = subTopic.ifBlank { null },
            onBack = { nav.popBackStack() },
            onSummary = { b, t, st ->
                nav.navigate(Route.Summary.make(b, t, st)) { launchSingleTop = true }
            },
            onPractice = { b, t ->
                nav.navigate(Route.Practice.make(b, t)) { launchSingleTop = true }
            },
            onOpenSettings = { nav.navigate(Route.Settings.route) { launchSingleTop = true } },
            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    // ✅✅✅ חשוב: היעד topic_repo חייב להיות בתוך אותה פונקציה כדי להירשם בגרף
    composable(
        route = Route.TopicRepoExercises.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topicId") { type = NavType.StringType },
            navArgument("subTopicId") { type = NavType.StringType }
        )
    ) { entry ->
        val beltIdEnc = entry.arguments?.getString("beltId").orEmpty()
        val topicIdEnc = entry.arguments?.getString("topicId").orEmpty()
        val subTopicIdEnc = entry.arguments?.getString("subTopicId").orEmpty()

        val beltId = Uri.decode(beltIdEnc)
        val belt = Belt.fromId(beltId) ?: Belt.GREEN

        TopicRepoExercisesScreen(
            belt = belt,
            topicId = topicIdEnc,
            subTopicId = subTopicIdEnc,
            onBack = { nav.popBackStack() }
        )
    }
}
