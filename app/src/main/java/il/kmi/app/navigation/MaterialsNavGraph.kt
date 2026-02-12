package il.kmi.app.navigation

import android.content.SharedPreferences
import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.shared.domain.Belt
import il.kmi.app.screens.MaterialsScreen

/**
 * גרף למסכי חומר הלימוד (Materials).
 * נשמרת החתימה עם sp/kmiPrefs כדי להתאים לקריאה הקיימת ב-MainNavHost.
 */
@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.materialsNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // ====== Materials (ללא subTopic) ======
    composable(
        route = Route.Materials.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topic")  { type = NavType.StringType },
            navArgument("coach")  { type = NavType.BoolType; defaultValue = false } // ✅ ADD
        )
    ) { backStackEntry ->
        val beltId = backStackEntry.arguments?.getString("beltId").orEmpty()
        val belt   = Belt.fromId(beltId) ?: Belt.WHITE

        val topicEnc = backStackEntry.arguments?.getString("topic").orEmpty()
        val topic    = Uri.decode(topicEnc)

        val coach = backStackEntry.arguments?.getBoolean("coach") ?: false // ✅ ADD (אם תרצה להשתמש)

        MaterialsScreen(
            vm = vm,
            belt = belt,
            topic = topic,
            onBack = { nav.popBackStack() },
            onSummary = { b, t, sub ->
                nav.navigate(Route.Summary.make(belt = b, topic = t, subTopic = sub))
            },
            onPractice = { b, t -> nav.navigate(Route.Practice.make(b, t)) },
            onOpenSettings = { nav.navigate(Route.Settings.route) },
            onOpenHome = { /* כמו אצלך */ },
            subTopicFilter = null
        )
    }

    // ====== Materials עם subTopic ======
    composable(
        route = Route.MaterialsSub.route,
        arguments = listOf(
            navArgument("beltId")   { type = NavType.StringType },
            navArgument("topic")    { type = NavType.StringType },
            navArgument("subTopic") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val beltId   = backStackEntry.arguments?.getString("beltId").orEmpty()
        val belt     = Belt.fromId(beltId) ?: Belt.WHITE

        val topicEnc   = backStackEntry.arguments?.getString("topic").orEmpty()
        val subTopicEnc = backStackEntry.arguments?.getString("subTopic").orEmpty()

        val topic     = Uri.decode(topicEnc)        // ✅ FIX
        val subTitle  = Uri.decode(subTopicEnc)     // ✅ FIX

        MaterialsScreen(
            vm = vm,
            belt = belt,
            topic = topic,
            onBack = { nav.popBackStack() },
            onSummary = { b, t, sub ->
                nav.navigate(Route.Summary.make(belt = b, topic = t, subTopic = sub))
            },
            onPractice = { b, chosenTopic -> nav.navigate(Route.Practice.make(b, chosenTopic)) },
            onOpenSettings = { nav.navigate(Route.Settings.route) },
            onOpenHome = {
                val popped = nav.popBackStack(
                    route = Route.Home.route,
                    inclusive = false
                )
                if (!popped) {
                    nav.navigate(Route.Home.route) {
                        popUpTo(nav.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            subTopicFilter = subTitle
        )
    }
}
