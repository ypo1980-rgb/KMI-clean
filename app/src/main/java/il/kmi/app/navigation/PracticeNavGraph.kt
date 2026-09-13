package il.kmi.app.navigation

import android.content.Context
import android.content.SharedPreferences
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.shared.domain.Belt
import il.kmi.app.screens.PracticeMenuScreen
import il.kmi.app.screens.RandomPracticeScreen
import il.kmi.app.subscription.KmiAccess

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.practiceNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    /*
     * מסך עצמאי לבחירת מסלול התרגול.
     */
    composable(
        route = Route.PracticeMenu.route
    ) {
        val accessPreferences =
            nav.context.getSharedPreferences(
                "kmi_user",
                Context.MODE_PRIVATE
            )

        val subscriptionPreferences =
            nav.context.getSharedPreferences(
                "kmi_subs",
                Context.MODE_PRIVATE
            )

        val canUseExtras =
            KmiAccess.hasFullAccess(
                accessPreferences
            ) ||
                    KmiAccess.hasFullAccess(
                        subscriptionPreferences
                    )

        val defaultBelt =
            vm.selectedBelt.value
                ?: Belt.GREEN

        PracticeMenuScreen(
            defaultBelt = defaultBelt,
            canUseExtras = canUseExtras,
            onBack = {
                val popped =
                    nav.popBackStack()

                if (!popped) {
                    nav.navigate(
                        Route.Home.route
                    ) {
                        launchSingleTop = true
                    }
                }
            },
            onHome = {
                nav.navigate(
                    Route.Home.route
                ) {
                    popUpTo(
                        nav.graph.startDestinationId
                    ) {
                        saveState = true
                    }

                    launchSingleTop = true
                    restoreState = true
                }
            },
            onLocked = {
                nav.navigate(
                    Route.Subscription.route
                ) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onRandomPractice = { belt ->
                vm.setSelectedBelt(belt)

                nav.navigate(
                    Route.Practice.make(
                        belt = belt
                    )
                )
            },
            onFinalExam = { belt ->
                vm.setSelectedBelt(belt)

                nav.navigate(
                    Route.Exam.make(belt)
                )
            },
            onPracticeByTopicSelected = {
                    belt,
                    topicToken ->

                vm.setSelectedBelt(belt)

                nav.navigate(
                    Route.Practice.make(
                        belt = belt,
                        topic = topicToken
                    )
                )
            }
        )
    }

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
            vm = vm,
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
