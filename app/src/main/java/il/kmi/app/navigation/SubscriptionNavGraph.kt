package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.subscription.SubscriptionScreen
// 👇 זה מסך המסלולים עם כפתורי "רכישה מאובטחת"
import il.kmi.app.subscription.SubscriptionPlansScreen as SubscriptionPlansGate

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.subscriptionNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // ---------- מסך "ניהול מנוי" ----------
    composable(Route.Subscription.route) {
        SubscriptionScreen(
            onBack = { nav.popBackStack() },
            onOpenPlans = {
                // מעבר למסך תוכניות המנוי
                nav.navigate(Route.SubscriptionPlans.route) {
                    launchSingleTop = true
                }
            },
            onOpenHome = {
                // קודם ננסה לחזור ל-Home אם הוא כבר ב-back stack
                val popped = nav.popBackStack(
                    route = Route.Home.route,
                    inclusive = false
                )
                if (!popped) {
                    // אם אין Home בסטאק – ננווט אליו כיעד יחיד ובטוח
                    nav.navigate(Route.Home.route) {
                        popUpTo(nav.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }

    // ---------- מסך "תוכניות מנוי" (מסלולים ורכישה) ----------
    composable(Route.SubscriptionPlans.route) {
        SubscriptionPlansGate(
            onBack = { nav.popBackStack() },

            // הרכישה עצמה מטופלת בתוך SubscriptionPlansScreen,
            // כאן לא מבצעים ניווט אוטומטי אחרי לחיצה על "רכישה מאובטחת"
            onPurchase = { _ /* productId */ -> },

            onOpenHome = {
                // אותו לוגיקה כמו במסך ניהול מנוי
                val popped = nav.popBackStack(
                    route = Route.Home.route,
                    inclusive = false
                )
                if (!popped) {
                    nav.navigate(Route.Home.route) {
                        popUpTo(nav.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}
