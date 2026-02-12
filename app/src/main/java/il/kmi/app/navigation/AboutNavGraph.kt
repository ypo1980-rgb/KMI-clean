package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.AboutMethodScreen
import il.kmi.app.screens.AboutNetworkScreen
import il.kmi.app.screens.ForumScreen
import il.kmi.app.screens.legal.LegalScreen
import il.kmi.app.screens.AboutAviAbisidonScreen
import il.kmi.app.screens.AboutItzikScreen

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.aboutNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // ---- Legal / Terms / Privacy (בשלב זה כולם מפנים לאותו מסך) ----
    composable(Route.Legal.route) {
        LegalScreen(onBack = { nav.popBackStack() })
    }

    // ---- פורום ----
    composable(Route.Forum.route) {
        ForumScreen(
            sp = sp,
            onBack = { nav.popBackStack() },
            onOpenSubscription = {
                nav.navigate(Route.Subscription.route)
            },
            onGoHome = {
                nav.navigate(Route.Home.route) {
                    // לוודא שחוזרים הביתה ולא בונים סטאק מוזר
                    popUpTo(Route.Home.route) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        )
    }

    // ---- אודות הרשת ----
    composable(Route.AboutNetwork.route) {
        AboutNetworkScreen(onBack = { nav.popBackStack() })
    }

    // ---- אודות השיטה ----
    composable(Route.AboutMethod.route) {
        AboutMethodScreen(onBack = { nav.popBackStack() })
    }

    // ---- אודות אבי אביסידון ----
    composable(Route.AboutAvi.route) {
        AboutAviAbisidonScreen(
            onClose = { nav.popBackStack() }
        )
    }

    // ---- אודות איציק ----
    composable(Route.AboutItzik.route) {
        AboutItzikScreen(
            onBack = { nav.popBackStack() }
        )
    }

    // ---- דרגו אותנו (לוגיקה קיימת נשארת במסך עצמו; כאן אין יעד נפרד) ----
    // אם בעתיד תרצה מסך ביניים ל-RateUs – נפתח כאן composable ייעודי.
}
