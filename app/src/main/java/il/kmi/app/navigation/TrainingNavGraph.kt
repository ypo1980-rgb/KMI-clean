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
import il.kmi.app.screens.BeltQuestions.BeltQuestionsByBeltScreen
import il.kmi.app.screens.PracticeByTopicsSelection
import il.kmi.shared.domain.Belt

/**
 * גרף “אימון/תוכן”.
 *
 * כלל חשוב:
 * כל ניווט של תתי־נושאים עובר רק דרך:
 * 1) SubTopicsRoute.build(...)
 * 2) buildMaterialsSubRoute(...)
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

        fun openSubTopics(
            belt: Belt,
            topic: String
        ) {
            vm.setSelectedBelt(belt)

            val route = il.kmi.app.screens.SubTopics.SubTopicsByBeltRoute.build(
                belt = belt,
                topic = topic
            )

            Log.e(
                "KMI_SUB",
                "training openSubTopics belt=${belt.id} topic='$topic' route='$route'"
            )

            nav.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        }

        fun openChosenSubTopic(
            belt: Belt,
            topic: String,
            subTopic: String
        ) {
            vm.setSelectedBelt(belt)

            Log.e(
                "KMI_SUB",
                "OPEN_CHOSEN_SUB_TOPIC START belt=${belt.id} topic='$topic' sub='$subTopic'"
            )
            println("KMI_SUB OPEN_CHOSEN_SUB_TOPIC START belt=${belt.id} topic='$topic' sub='$subTopic'")

            val route = runCatching {
                if (topic == "שחרורים") {
                    Route.MaterialsSub.make(
                        belt = belt,
                        topic = topic,
                        subTopic = subTopic
                    )
                } else if (topic == "internal" || topic == "external") {

                    val mappedId = when (subTopic.lowercase()) {
                        "punch" -> if (topic == "internal") "def_internal_punch" else "def_external_punch"
                        "kick" -> if (topic == "internal") "def_internal_kick" else "def_external_kick"
                        else -> null
                    }

                    if (mappedId != null) {
                        il.kmi.app.screens.SubTopics.SubTopicsByTopicRoute.build(
                            belt = belt,
                            topic = mappedId
                        )
                    } else {
                        Route.MaterialsSub.make(
                            belt = belt,
                            topic = topic,
                            subTopic = subTopic
                        )
                    }

                } else {
                    Route.MaterialsSub.make(
                        belt = belt,
                        topic = topic,
                        subTopic = subTopic
                    )
                }
            }.onFailure { t ->
                Log.e(
                    "KMI_SUB",
                    "OPEN_CHOSEN_SUB_TOPIC BUILD FAILED belt=${belt.id} topic='$topic' sub='$subTopic'",
                    t
                )
                println("KMI_SUB OPEN_CHOSEN_SUB_TOPIC BUILD FAILED belt=${belt.id} topic='$topic' sub='$subTopic' error='${t.message}'")
            }.getOrNull() ?: return

            Log.e(
                "KMI_SUB",
                "OPEN_CHOSEN_SUB_TOPIC ROUTE belt=${belt.id} topic='$topic' sub='$subTopic' route='$route'"
            )
            println("KMI_SUB OPEN_CHOSEN_SUB_TOPIC ROUTE belt=${belt.id} topic='$topic' sub='$subTopic' route='$route'")

            runCatching {
                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }.onFailure { t ->
                Log.e(
                    "KMI_SUB",
                    "OPEN_CHOSEN_SUB_TOPIC NAVIGATE FAILED route='$route'",
                    t
                )
                println("KMI_SUB OPEN_CHOSEN_SUB_TOPIC NAVIGATE FAILED route='$route' error='${t.message}'")
            }
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

            // נושא רגיל -> MaterialsScreen המעוצב
            onOpenTopic = { belt, topic ->
                vm.setSelectedBelt(belt)

                nav.navigate(Route.Materials.make(belt = belt, topic = topic)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // כל ניווט של "תפריט הגנות / תתי נושאים" עובר דרך המקור המרכזי
            onOpenDefenseMenu = { belt, topic ->
                val cleanTopic = topic.trim()
                val parts = cleanTopic.split(":", limit = 2)

                if (parts.size == 2) {
                    val parentTopic = parts[0].trim()
                    val pickedSubTopic = parts[1].trim()

                    openChosenSubTopic(
                        belt = belt,
                        topic = parentTopic,
                        subTopic = pickedSubTopic
                    )
                } else {
                    openSubTopics(
                        belt = belt,
                        topic = cleanTopic
                    )
                }
            },

            onOpenSubTopic = { belt, topic, subTopic ->
                Log.e(
                    "KMI_SUB",
                    "BeltQuestionsByBeltScreen onOpenSubTopic belt=${belt.id} topic='$topic' sub='$subTopic'"
                )
                println("KMI_SUB BeltQuestionsByBeltScreen onOpenSubTopic belt=${belt.id} topic='$topic' sub='$subTopic'")

                openChosenSubTopic(
                    belt = belt,
                    topic = topic,
                    subTopic = subTopic
                )
            },

            onOpenHardSubjectRoute = { belt, subjectId ->
                vm.setSelectedBelt(belt)

                val route = il.kmi.app.screens.SubTopics.SubTopicsByTopicRoute.build(
                    belt = belt,
                    topic = subjectId
                )

                Log.e(
                    "KMI_SUB",
                    "training onOpenHardSubjectRoute belt=${belt.id} subjectId='$subjectId' route='$route'"
                )

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenSubject = { subject ->
                val selectedBelt = runCatching {
                    vm.selectedBelt.value?.id?.let { Belt.fromId(it) }
                }.getOrNull() ?: Belt.GREEN

                openSubTopics(
                    belt = selectedBelt,
                    topic = subject.titleHeb
                )
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
                nav.navigate(Route.VoiceAssistant.route) {
                    launchSingleTop = true
                }
            },

            onOpenPdfMaterials = { belt ->
                nav.navigate(Route.Materials.make(belt, topic = "")) {
                    launchSingleTop = true
                }
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

        vm.setSelectedBelt(belt)

        il.kmi.app.screens.MaterialsScreen(
            vm = vm,
            belt = belt,
            topic = topic,
            subTopicFilter = null,
            onBack = { nav.popBackStack() },
            onSummary = { b, t, st ->
                nav.navigate(Route.Summary.make(b, t, st)) {
                    launchSingleTop = true
                }
            },
            onPractice = { b, t ->
                nav.navigate(Route.Practice.make(b, t)) {
                    launchSingleTop = true
                }
            },
            onOpenSettings = {
                nav.navigate(Route.Settings.route) {
                    launchSingleTop = true
                }
            },
            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}