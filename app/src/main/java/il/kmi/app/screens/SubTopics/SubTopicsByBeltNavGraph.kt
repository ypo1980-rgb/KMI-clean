package il.kmi.app.screens.SubTopics

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.SubTopicsScreen
import il.kmi.shared.domain.Belt

private fun buildMaterialsSubRouteByBelt(
    belt: Belt,
    topic: String,
    subTopic: String
): String? {
    val clean = subTopic.trim()
    if (clean.isEmpty()) return null

    val isDefenseTopic = topic.contains("הגנות")

    return if (isDefenseTopic) {
        val fixedSub = when {
            clean.contains("בעיט") &&
                    (clean.contains("פנימ") || clean.contains("חיצונ")) -> {
                "הגנות נגד בעיטות"
            }

            else -> clean
        }

        Route.MaterialsSub.make(
            belt = belt,
            topic = "הגנות",
            subTopic = fixedSub
        )
    } else {
        val fixedSubTopic = when {
            clean.contains("מגל") && clean.contains("סנוקרת") -> "מגל + סנוקרת"
            else -> clean
        }

        Route.MaterialsSub.make(
            belt = belt,
            topic = topic,
            subTopic = fixedSubTopic
        )
    }
}

fun NavGraphBuilder.subTopicsByBeltNavGraph(
    nav: NavHostController,
    vm: KmiViewModel
) {
    composable(
        route = SubTopicsByBeltRoute.route,
        arguments = listOf(
            navArgument(SubTopicsByBeltRoute.beltArg) { type = NavType.StringType },
            navArgument(SubTopicsByBeltRoute.topicArg) { type = NavType.StringType }
        )
    ) { entry ->
        val beltIdEnc = entry.arguments?.getString(SubTopicsByBeltRoute.beltArg).orEmpty()
        val topicEnc = entry.arguments?.getString(SubTopicsByBeltRoute.topicArg).orEmpty()

        val beltId = Uri.decode(beltIdEnc)
        val belt = Belt.fromId(beltId) ?: Belt.GREEN
        val topic = Uri.decode(topicEnc).trim()

        SubTopicsScreen(
            belt = belt,
            topic = topic,
            onBack = { nav.popBackStack() },
            onHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) { inclusive = false }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenSubTopic = { subTitle ->
                val route = buildMaterialsSubRouteByBelt(
                    belt = belt,
                    topic = topic,
                    subTopic = subTitle
                ) ?: return@SubTopicsScreen

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenExercise = { itemName ->
                val route = Route.Exercise.make(itemName)

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            vm = vm
        )
    }
}