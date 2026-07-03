package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.ProgressScreen

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.progressNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    composable(Route.Progress.route) {
        ProgressScreen(
            vm = vm,
            onBack = { nav.popBackStack() },
            onHome = {
                nav.navigate(Route.Home.route) {
                    launchSingleTop = true
                    restoreState = false
                    popUpTo(nav.graph.startDestinationId) {
                        inclusive = false
                    }
                }
            },
            onOpenCarousel = {
                nav.navigate(Route.BeltQ.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}
