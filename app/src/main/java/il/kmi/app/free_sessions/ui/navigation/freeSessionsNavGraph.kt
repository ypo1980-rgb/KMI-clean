package il.kmi.app.free_sessions.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.Route
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

        val selectedCalendarDateIso =
            backStackEntry.savedStateHandle
                .getStateFlow(
                    "free_session_selected_date",
                    ""
                )
                .collectAsState()
                .value

        FreeSessionsScreen(
            branch = branch,
            groupKey = groupKey,
            currentUid = uid,
            currentName = name,
            selectedCalendarDateIso = selectedCalendarDateIso,
            onOpenCalendar = {
                backStackEntry.savedStateHandle[
                    "monthly_calendar_mode"
                ] = "free_session_date_picker"

                nav.navigate(Route.MonthlyCalendar.route) {
                    launchSingleTop = true
                }
            },
            onCalendarDateConsumed = {
                backStackEntry.savedStateHandle[
                    "free_session_selected_date"
                ] = ""
            },
            onBack = {
                nav.popBackStack()
            }
        )
    }
}
