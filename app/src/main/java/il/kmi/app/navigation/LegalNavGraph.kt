package il.kmi.app.navigation

import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.Route
import il.kmi.app.screens.legal.LegalScreen

/**
 * גרף למסכי Legal (פרטיות/תנאים וכו').
 * כרגע מסך בודד — אפשר להרחיב בהמשך לפי הצורך.
 */
fun NavGraphBuilder.legalNavGraph(
    nav: NavHostController
) {
    composable(
        route = Route.Legal.route + "?tab={tab}",
        arguments = listOf(
            navArgument("tab") {
                type = NavType.StringType
                defaultValue = "terms"
            }
        )
    ) { entry ->
        val tab = entry.arguments?.getString("tab") ?: "terms"

        val initialTab = when (tab.lowercase()) {
            "privacy" -> 1
            "accessibility", "a11y" -> 2
            else -> 0 // terms
        }

        LegalScreen(
            onBack = { nav.popBackStack() },
            initialTab = initialTab
        )
    }
}
