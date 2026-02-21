package il.kmi.app.navigation

import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.MaterialsScreen
import il.kmi.shared.domain.Belt

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.materialsNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // ====== ✅ Materials (ללא subTopic) ======
    composable(
        route = Route.Materials.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topic") { type = NavType.StringType },
            navArgument("coach") { type = NavType.BoolType; defaultValue = false }
        )
    ) { backStackEntry ->
        val beltId = backStackEntry.arguments?.getString("beltId").orEmpty()
        val belt = Belt.fromId(beltId) ?: Belt.WHITE

        val topicEnc = backStackEntry.arguments?.getString("topic").orEmpty()
        val topic = remember(topicEnc) {
            runCatching { Uri.decode(topicEnc) }.getOrDefault(topicEnc).trim()
        }

        MaterialsScreen(
            vm = vm,
            belt = belt,
            topic = topic,
            subTopicFilter = null,
            onBack = { nav.popBackStack() },

            // ⚠️ כרגע אנחנו לא “מעבירים” topic/subTopic ל-Summary כי לא בטוח שה-route תומך בזה.
            // אם בהמשך נרצה Summary לפי נושא — נעדכן את Route.Summary.make בהתאם.
            onSummary = { b, _topic, _sub ->
                nav.navigate(Route.Summary.make(b)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onPractice = { b, t ->
                nav.navigate(Route.Practice.make(b, t)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenSettings = {
                nav.navigate(Route.Settings.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.startDestinationId) {
                        inclusive = false
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }

    // ====== ✅ Materials עם subTopic ======
    composable(
        route = Route.MaterialsSub.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topic") { type = NavType.StringType },
            navArgument("subTopic") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val beltId = backStackEntry.arguments?.getString("beltId").orEmpty()
        val belt = Belt.fromId(beltId) ?: Belt.WHITE

        val topicEnc = backStackEntry.arguments?.getString("topic").orEmpty()
        val subEnc = backStackEntry.arguments?.getString("subTopic").orEmpty()

        val topic = remember(topicEnc) {
            runCatching { Uri.decode(topicEnc) }.getOrDefault(topicEnc).trim()
        }
        val subTopic = remember(subEnc) {
            runCatching { Uri.decode(subEnc) }.getOrDefault(subEnc).trim()
        }

        MaterialsScreen(
            vm = vm,
            belt = belt,
            topic = topic,
            subTopicFilter = subTopic,
            onBack = { nav.popBackStack() },

            onSummary = { b, _topic, _sub ->
                nav.navigate(Route.Summary.make(b)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onPractice = { b, t ->
                nav.navigate(Route.Practice.make(b, t)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenSettings = {
                nav.navigate(Route.Settings.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.startDestinationId) {
                        inclusive = false
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}
