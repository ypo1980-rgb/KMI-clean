package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.Route
import il.kmi.shared.domain.Belt
import il.kmi.app.screens.ExamScreen
import il.kmi.app.KmiViewModel

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.examNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    composable(
        route = Route.Exam.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val beltId = backStackEntry.arguments?.getString("beltId").orEmpty()
        val belt   = Belt.fromId(beltId) ?: Belt.WHITE

        ExamScreen(
            belt = belt,
            onBack = { nav.popBackStack() },
            onHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(0)
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onSearch = {
                nav.navigate(Route.BeltQ.route) { launchSingleTop = true }
            }
        )
    }
}
