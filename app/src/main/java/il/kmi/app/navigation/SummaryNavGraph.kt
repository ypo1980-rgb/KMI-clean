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
import il.kmi.app.screens.SummaryScreen
import java.net.URLDecoder

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.summaryNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    composable(
        route = Route.Summary.route,
        arguments = listOf(
            navArgument(name = "beltId") { type = NavType.StringType },
            // ✅ חדש: פרמטרים אופציונליים — כדי שתאימות לאחור לא תישבר
            navArgument(name = "topic") { type = NavType.StringType; defaultValue = "" },
            navArgument(name = "subTopic") { type = NavType.StringType; defaultValue = "" }
        )
    ) { backStackEntry ->

        fun dec(s: String?): String =
            try { URLDecoder.decode(s ?: "", "UTF-8") } catch (_: Exception) { s.orEmpty() }

        val beltId = backStackEntry.arguments?.getString("beltId") ?: ""
        val belt   = Belt.fromId(beltId) ?: Belt.WHITE

        val topic = dec(backStackEntry.arguments?.getString("topic")).trim()
        val subTopicFilter = dec(backStackEntry.arguments?.getString("subTopic")).trim().ifBlank { null }

        SummaryScreen(
            vm = vm,
            belt = belt,
            topic = topic,
            subTopicFilter = subTopicFilter,

            onBack = {
                // ✅ נוודא שה-VM מכוון לחגורה הזו, כדי שהגלגל ייפתח עליה
                runCatching { vm.setSelectedBelt(belt) }

                // ✅ תמיד חוזרים למסך הגלגל (Topics), בלי להסתמך על BackStack
                nav.navigate(Route.Topics.route) {
                    // מעיף כל מה שמעל Home (כולל מסכי תרגילים/סיכום) כדי שלא "יחזור" אליהם
                    popUpTo(Route.Home.route) { inclusive = false }
                    launchSingleTop = true
                    restoreState = true
                }
            },

                    onBackHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            },

            onOpenProgress = { nav.navigate(Route.Progress.route) },
            onOpenSettings = { nav.navigate(Route.Settings.route) }
        )
    }
}
