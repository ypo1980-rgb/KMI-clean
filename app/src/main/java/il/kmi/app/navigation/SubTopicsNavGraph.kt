package il.kmi.app.navigation

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

object SubTopicsRoute {
    const val beltArg = "beltId"
    const val topicArg = "topic"

    const val route = "sub_topics/{$beltArg}/{$topicArg}"

    fun build(
        belt: Belt,
        topic: String
    ): String {
        return "sub_topics/${Uri.encode(belt.id)}/${Uri.encode(topic)}"
    }
}

fun buildMaterialsSubRoute(
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
        return SubTopicsRoute.build(
            belt = belt,
            topic = mappedHardSubjectId
        )
    }

    val isHardTopic =
        il.kmi.shared.domain.content.HardSectionsCatalog.supportsSubject(topic) ||
                il.kmi.shared.domain.content.HardSectionsCatalog.findAnySectionById(topic) != null

    if (isHardTopic) {
        return SubTopicsRoute.build(
            belt = belt,
            topic = clean
        )
    }

    val isDefenseTopic = topic.contains("הגנות")

    return if (isDefenseTopic) {
        val fixedSub = when {
            clean.contains("בעיט") &&
                    (clean.contains("פנימ") || clean.contains("חיצונ")) ->
                "הגנות נגד בעיטות"

            else -> clean
        }

        Route.MaterialsSub.make(
            belt = belt,
            topic = "הגנות",
            subTopic = fixedSub
        )
    } else {
        val fixedSubTopic = when {
            clean.contains("מגל") && clean.contains("סנוקרת") ->
                "מגל + סנוקרת"

            else -> clean
        }

        Route.MaterialsSub.make(
            belt = belt,
            topic = topic,
            subTopic = fixedSubTopic
        )
    }
}

/**
 * מקור אמת יחיד לניווט תתי־נושאים.
 *
 * זרימה:
 * 1) כל מסך שרוצה לפתוח תתי־נושאים -> nav.navigate(SubTopicsRoute.build(...))
 * 2) הבחירה בתוך SubTopicsScreen -> buildMaterialsSubRoute(...)
 */
fun NavGraphBuilder.subTopicsNavGraph(
    nav: NavHostController
) {
    composable(
        route = SubTopicsRoute.route,
        arguments = listOf(
            navArgument(SubTopicsRoute.beltArg) { type = NavType.StringType },
            navArgument(SubTopicsRoute.topicArg) { type = NavType.StringType }
        )
    ) { entry ->
        val beltIdEnc = entry.arguments?.getString(SubTopicsRoute.beltArg).orEmpty()
        val topicEnc = entry.arguments?.getString(SubTopicsRoute.topicArg).orEmpty()

        val beltId = Uri.decode(beltIdEnc)
        val belt = Belt.fromId(beltId) ?: Belt.GREEN
        val topic = Uri.decode(topicEnc).trim()

        Log.e(
            "KMI_SUB",
            "SubTopicsRoute composable belt=${belt.id} topic='$topic'"
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
                val route = buildMaterialsSubRoute(
                    belt = belt,
                    topic = topic,
                    subTopic = subTitle
                ) ?: return@SubTopicsScreen

                Log.e(
                    "KMI_SUB",
                    "SubTopicsRoute onOpenSubTopic belt=${belt.id} topic='$topic' sub='$subTitle' route='$route'"
                )

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenExercise = { itemName ->
                val route = Route.Exercise.make(itemName)

                Log.e(
                    "KMI_SUB",
                    "SubTopicsRoute onOpenExercise item='$itemName' route='$route'"
                )

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}