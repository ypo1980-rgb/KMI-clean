package il.kmi.app.navigation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.BeltQuestions.BeltQuestionsByTopicScreen
import il.kmi.shared.domain.Belt
import il.kmi.app.subscription.KmiAccess

fun isLockedPremiumTopic(raw: String): Boolean {
    val t = raw.trim().lowercase()

    return t == "שחרורים" ||
            t == "הגנות" ||
            t.contains("שחרור") ||
            t.contains("release") ||
            t.contains("הגנה") ||
            t.contains("defense") ||
            t.contains("defence") ||
            t.startsWith("def_") ||
            t.startsWith("releases_")
}

fun NavGraphBuilder.topicsNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    composable(Route.Topics.route) {

        Log.e("KMI_NAV", "ENTER Route.Topics CALLBACK_PROBE_A")

        val appCtx = LocalContext.current
        val userSp = remember(appCtx) {
            appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
        }

        fun shouldBlockPremiumTopic(raw: String): Boolean {
            return isLockedPremiumTopic(raw) && !KmiAccess.hasFullAccess(userSp)
        }

        val isCoachFlag = remember(sp, userSp) {
            runCatching {
                val role1 = sp.getString("user_role", null)?.lowercase().orEmpty()
                val role2 = userSp.getString("user_role", null)?.lowercase().orEmpty()

                role1 == "coach" || role1.contains("coach") || role1.contains("מאמן") || role1.contains("מדריך") ||
                        role2 == "coach" || role2.contains("coach") || role2.contains("מאמן") || role2.contains("מדריך") ||
                        sp.getBoolean("isCoach", false) ||
                        userSp.getBoolean("isCoach", false)
            }.getOrDefault(false)
        }

        Log.e("KMI_WHERE", "ENTER TopicsNavGraph -> Route.Topics isCoach=$isCoachFlag")
        Log.e("KMI_NAV", "ENTER Route.Topics isCoach=$isCoachFlag")
        Log.e("KMI_NAV", "CALL BeltQuestionsByTopicScreen from Route.Topics CALLBACK_PROBE_B")

        fun openSubTopics(belt: Belt, topic: String) {
            vm.setSelectedBelt(belt)

            val route = il.kmi.app.screens.SubTopics.SubTopicsByTopicRoute.build(
                belt = belt,
                topic = topic
            )

            Log.e(
                "KMI_SUB",
                "topicsNavGraph openSubTopics belt=${belt.id} topic='$topic' route='$route'"
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

            val route = Route.MaterialsSub.make(
                belt = belt,
                topic = topic,
                subTopic = subTopic
            )

            Log.e(
                "KMI_SUB",
                "topicsNavGraph openChosenSubTopic belt=${belt.id} topic='$topic' sub='$subTopic' route='$route'"
            )

            if (route != null) {
                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        BeltQuestionsByTopicScreen(
            onOpenHardSubjectRoute = { belt, subjectId ->
                vm.setSelectedBelt(belt)

                Log.e(
                    "KMI_SUB",
                    "topicsNavGraph onOpenHardSubjectRoute belt=${belt.id} subjectId='$subjectId'"
                )

                openSubTopics(
                    belt = belt,
                    topic = subjectId
                )
            },

            onOpenSubject = { belt: Belt, subject ->
                if (shouldBlockPremiumTopic(subject.titleHeb) || shouldBlockPremiumTopic(subject.id)) {
                    nav.navigate(Route.Subscription.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    openSubTopics(
                        belt = belt,
                        topic = subject.titleHeb
                    )
                }
            },

            onOpenTopic = { belt: Belt, topicTitle: String ->
                if (shouldBlockPremiumTopic(topicTitle)) {
                    nav.navigate(Route.Subscription.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    openSubTopics(
                        belt = belt,
                        topic = topicTitle
                    )
                }
            },

            onOpenTopicWithSub = { belt: Belt, topicTitle: String, subTopicTitle: String ->
                openChosenSubTopic(
                    belt = belt,
                    topic = topicTitle,
                    subTopic = subTopicTitle
                )
            },

            onOpenDefenseList = { belt, kind, pick ->
                if (shouldBlockPremiumTopic(kind) || shouldBlockPremiumTopic(pick)) {
                    nav.navigate(Route.Subscription.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    openChosenSubTopic(
                        belt = belt,
                        topic = kind,
                        subTopic = pick
                    )
                }
            },

            onOpenWeakPoints = { belt ->
                vm.setSelectedBelt(belt)
                nav.navigate(Route.WeakPoints.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenAllLists = { belt ->
                vm.setSelectedBelt(belt)
                runCatching { nav.navigate("ex_tabs_all/${belt.id}") }
            },

            onOpenSummaryScreen = { belt ->
                vm.setSelectedBelt(belt)
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
                vm.setSelectedBelt(belt)
                nav.navigate(Route.Materials.make(belt = belt, topic = "")) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}