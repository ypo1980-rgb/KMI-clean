package il.kmi.app.screens.SubTopics

import android.net.Uri
import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.Route
import il.kmi.app.screens.SubTopicsScreen
import il.kmi.shared.domain.Belt

private fun buildMaterialsSubRouteByTopic(
    belt: Belt,
    topic: String,
    subTopic: String
): String? {
    val clean = subTopic.trim()
    if (clean.isEmpty()) return null

    val topicNorm = topic.trim().lowercase()
    val subNorm = clean.lowercase()

    val mappedHardSubjectId = when {
        topicNorm == "internal" && subNorm == "punch" -> "def_internal_punch"
        topicNorm == "internal" && subNorm == "kick" -> "def_internal_kick"
        topicNorm == "external" && subNorm == "punch" -> "def_external_punch"
        topicNorm == "external" && subNorm == "kick" -> "def_external_kick"
        else -> null
    }

    if (mappedHardSubjectId != null) {
        return SubTopicsByTopicRoute.build(
            belt = belt,
            topic = mappedHardSubjectId
        )
    }

    val isHardTopic =
        il.kmi.shared.domain.content.HardSectionsCatalog.supportsSubject(topic) ||
                il.kmi.shared.domain.content.HardSectionsCatalog.findAnySectionById(topic) != null

    if (isHardTopic) {
        return SubTopicsByTopicRoute.build(
            belt = belt,
            topic = clean
        )
    }

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

fun NavGraphBuilder.subTopicsByTopicNavGraph(
    nav: NavHostController
) {
    composable(
        route = SubTopicsByTopicRoute.route,
        arguments = listOf(
            navArgument(SubTopicsByTopicRoute.beltArg) { type = NavType.StringType },
            navArgument(SubTopicsByTopicRoute.topicArg) { type = NavType.StringType }
        )
    ) { entry ->
        val beltIdEnc = entry.arguments?.getString(SubTopicsByTopicRoute.beltArg).orEmpty()
        val topicEnc = entry.arguments?.getString(SubTopicsByTopicRoute.topicArg).orEmpty()

        val beltId = Uri.decode(beltIdEnc)
        val belt = Belt.fromId(beltId) ?: Belt.GREEN
        val topic = Uri.decode(topicEnc).trim()

        Log.e(
            "KMI_SUB_TOPIC",
            "SubTopicsByTopicRoute composable belt=${belt.id} topic='$topic'"
        )

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
                val route = buildMaterialsSubRouteByTopic(
                    belt = belt,
                    topic = topic,
                    subTopic = subTitle
                ) ?: return@SubTopicsScreen

                Log.e(
                    "KMI_SUB_TOPIC",
                    "SubTopicsByTopicRoute onOpenSubTopic belt=${belt.id} topic='$topic' sub='$subTitle' route='$route'"
                )

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenExercise = { itemName ->
                val route = Route.Exercise.make(itemName)

                Log.e(
                    "KMI_SUB_TOPIC",
                    "SubTopicsByTopicRoute onOpenExercise item='$itemName' route='$route'"
                )

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}