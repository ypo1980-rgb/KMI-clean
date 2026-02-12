package il.kmi.app.free_sessions.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.free_sessions.ui.FreeSessionsScreen

fun NavGraphBuilder.freeSessionsNavGraph(
    nav: NavHostController
) {
    composable(
        route = FreeSessionsRoute.route,
        arguments = listOf(
            navArgument("branch") { type = NavType.StringType },
            navArgument("groupKey") { type = NavType.StringType },
            navArgument("uid") { type = NavType.StringType },
            navArgument("name") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val branch = backStackEntry.arguments?.getString("branch").orEmpty()
        val groupKey = backStackEntry.arguments?.getString("groupKey").orEmpty()
        val uid = backStackEntry.arguments?.getString("uid").orEmpty()
        val name = backStackEntry.arguments?.getString("name").orEmpty()

        FreeSessionsScreen(
            branch = branch,
            groupKey = groupKey,
            currentUid = uid,
            currentName = name,
            onBack = { nav.popBackStack() }
        )
    }
}
