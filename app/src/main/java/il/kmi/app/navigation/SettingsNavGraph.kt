package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.composable
import il.kmi.app.Route
import il.kmi.app.screens.SettingsScreenModern

/**
 * גרף למסכי ההגדרות.
 * שים לב: אינו משנה התנהגות כל עוד הדגל nav_split_enabled כבוי.
 */
fun NavGraphBuilder.settingsNavGraph(
    nav: NavHostController,
    vm: il.kmi.app.KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs,
    themeMode: String,
    onThemeChange: (String) -> Unit,
    onFontSizeChange: (String) -> Unit
) {
    composable(
        route = Route.Settings.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(280)
            ) + fadeIn(animationSpec = tween(280))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(220)
            ) + fadeOut(animationSpec = tween(220))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(280)
            ) + fadeIn(animationSpec = tween(280))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(220)
            ) + fadeOut(animationSpec = tween(220))
        }
    ) {

        val ctx = LocalContext.current

        val spTrainingSummary = remember {
            ctx.getSharedPreferences("kmi_training_summary", android.content.Context.MODE_PRIVATE)
        }

        SettingsScreenModern(
            sp = sp,
            kmiPrefs = kmiPrefs,
            themeMode = themeMode,
            onThemeChange = onThemeChange,
            onBack = { nav.popBackStack() },

            onOpenRegistration = {
                nav.navigate(Route.NewUserTrainee.route + "?step=profile") {
                    launchSingleTop = true
                }
            },

            onOpenPrivacy = { nav.navigate(Route.Legal.route + "?tab=privacy") },
            onOpenTerms = { nav.navigate(Route.Legal.route + "?tab=terms") },
            onOpenAccessibility = { nav.navigate(Route.Legal.route + "?tab=accessibility") },
            onOpenProgress = {
                nav.navigate(Route.Progress.route)
            },
            onOpenCoachBroadcast = {
                nav.navigate(Route.CoachBroadcast.route)
            },
            onFontSizeChange = onFontSizeChange,

            vm = vm
        )
    }
}
