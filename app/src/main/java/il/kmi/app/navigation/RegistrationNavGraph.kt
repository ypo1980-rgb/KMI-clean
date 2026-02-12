package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.registration.RegistrationNavHost
import il.kmi.app.screens.registration.ExistingUserCoachScreen
import il.kmi.app.screens.registration.ExistingUserTraineeScreen
import il.kmi.app.screens.NewUserCoachScreen
import il.kmi.app.screens.registration.RegistrationFormScreen
import androidx.compose.runtime.LaunchedEffect

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.registrationNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // --- מסך נחיתה לרישום (NavHost פנימי כפי שהיה אצלך) ---
    composable(Route.RegistrationLanding.route) {
        val regNav = androidx.navigation.compose.rememberNavController()
        RegistrationNavHost(
            nav = regNav,
            vm = vm,
            sp = sp,
            kmiPrefs = kmiPrefs,
            onOpenDrawer = { il.kmi.app.ui.DrawerBridge.open() },
            onOpenLegal = { nav.navigate(Route.Legal.route) },
            onOpenTerms = { nav.navigate(Route.Legal.route) },
            onRegistrationDone = {
                nav.navigate(Route.Home.route) {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
        )
    }

    // --- New user (trainee) ---
    composable(
        route = Route.NewUserTrainee.route + "?step={step}&skipOtp={skipOtp}",
        arguments = listOf(
            navArgument("step")    { type = NavType.StringType;  nullable = true; defaultValue = null },
            navArgument("skipOtp") { type = NavType.StringType;  defaultValue = "false" }
        )
    ) { entry ->
        val stepArg        = entry.arguments?.getString("step")
        val skipOtp        = entry.arguments?.getString("skipOtp") == "true"
        val startAtProfile = skipOtp || stepArg == "profile"

        il.kmi.app.screens.registration.RegistrationFormScreen(
            initial = "trainee",
            onBack = { nav.popBackStack() },
            onRegistrationComplete = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.RegistrationLanding.route) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onOpenTerms = { nav.navigate(Route.Legal.route) },
            onOpenLegal = { nav.navigate(Route.Legal.route) },
            onOpenDrawer = { il.kmi.app.ui.DrawerBridge.open() },
            vm = vm,
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
                nav.navigate(Route.Home.route) {
                    popUpTo(0)
                    launchSingleTop = true
                    restoreState = false
                }
            },
            onOpenRecovery = { /* ... */ },
            vm = vm,
            sp = sp,
            kmiPrefs = kmiPrefs,
            onOpenDrawer = { il.kmi.app.ui.DrawerBridge.open() }
        )
    }

    // --- Existing user (coach) ---
    composable(route = Route.ExistingUserCoach.route) {
        ExistingUserCoachScreen(
            onBack = { nav.popBackStack() },
            onLoginComplete = {
                nav.navigate(Route.Home.route) {
                    popUpTo(0)
                    launchSingleTop = true
                    restoreState = false
                }
            },
            vm = vm,
            sp = sp,
            kmiPrefs = kmiPrefs,
            onOpenDrawer = { il.kmi.app.ui.DrawerBridge.open() }
        )
    }

    // --- New user (coach) ---
    composable(route = Route.NewUserCoach.route) {
        NewUserCoachScreen(
            onBack = { nav.popBackStack() },
            onRegistrationComplete = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.RegistrationLanding.route) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onOpenLegal = { nav.navigate(Route.Legal.route) },
            vm = vm,
            sp = sp,
            kmiPrefs = kmiPrefs,
            skipOtp = false
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
