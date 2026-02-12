package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.shared.domain.Belt
import il.kmi.app.screens.RandomPracticeScreen

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.practiceNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    composable(
        route = Route.Practice.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topic")  { type = NavType.StringType; nullable = true; defaultValue = null }
        )
    ) { backStackEntry ->
        val beltId = backStackEntry.arguments?.getString("beltId").orEmpty()
        val belt   = Belt.fromId(beltId) ?: Belt.WHITE
        val topic  = backStackEntry.arguments?.getString("topic")?.takeIf { it.isNotBlank() }

        RandomPracticeScreen(
            belt = belt,
            topicFilter = topic,
            onBack = {
                val popped = nav.popBackStack()
                if (!popped) {
                    nav.navigate(Route.Topics.route) {
                        launchSingleTop = true
                        popUpTo(Route.Home.route) { inclusive = false }
                    }
                }
            },
            onHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onSearch = {
                nav.navigate(Route.Topics.route) {
                    launchSingleTop = true
                }
            }
        )
    }
}
