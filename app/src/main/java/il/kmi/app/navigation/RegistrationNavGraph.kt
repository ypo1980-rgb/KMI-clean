package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.registration.ExistingUserCoachScreen
import il.kmi.app.screens.registration.ExistingUserTraineeScreen
import androidx.compose.runtime.LaunchedEffect

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.registrationNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {

    // --- New user (trainee) ---
    composable(
        route = Route.NewUserTrainee.route + "?step={step}",
        arguments = listOf(
            navArgument("step") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { entry ->
        val stepArg =
            entry.arguments
                ?.getString("step")
                ?.trim()
                .orEmpty()

        val startAtProfile =
            stepArg.equals(
                "profile",
                ignoreCase = true
            ) ||
                    stepArg.equals(
                        "edit_profile",
                        ignoreCase = true
                    )

        il.kmi.app.screens.registration.RegistrationFormScreen(
            initial = "trainee",

            onBack = {
                nav.popBackStack()
            },

            onRegistrationComplete = {
                nav.navigate(
                    Route.Splash.route
                ) {
                    popUpTo(0) {
                        inclusive = true
                    }

                    launchSingleTop = true
                    restoreState = false
                }
            },

            onOpenHome = {
                nav.navigate(
                    Route.Home.route
                ) {
                    launchSingleTop = true
                    restoreState = false
                }
            },

            onOpenTerms = {
                nav.navigate(
                    Route.Legal.route
                )
            },

            onOpenDrawer = {
                il.kmi.app.ui.DrawerBridge.open()
            },

            sp = sp,
            kmiPrefs = kmiPrefs,
            startAtProfile = startAtProfile
        )
    }

    // --- Existing user (trainee) ---
    composable(Route.ExistingUserTrainee.route) {
        ExistingUserTraineeScreen(
            onBack = { nav.popBackStack() },
            onLoginComplete = {
                nav.navigate(Route.Splash.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            },
            sp = sp,
            kmiPrefs = kmiPrefs
        )
    }

    // --- Existing user (coach) ---
    composable(route = Route.ExistingUserCoach.route) {
        ExistingUserCoachScreen(
            onBack = { nav.popBackStack() },
            onLoginComplete = {
                nav.navigate(Route.Splash.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            },
            sp = sp,
            kmiPrefs = kmiPrefs
        )
    }

    // --- הפניה מהמסלול הישן לחדש (אם עדיין קיימת אפליקציה בשטח) ---
    composable(Route.Registration.route) {
        LaunchedEffect(Unit) {
            nav.navigate(Route.NewUserTrainee.route) {
                popUpTo(Route.Registration.route) { inclusive = true }
                launchSingleTop = true
            }
        }
        androidx.compose.material3.CircularProgressIndicator()
    }
}
