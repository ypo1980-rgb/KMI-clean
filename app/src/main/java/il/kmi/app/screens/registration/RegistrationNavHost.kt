@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.registration

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.screens.registration.ExistingUserCoachScreen
import il.kmi.app.screens.NewUserCoachScreen
import il.kmi.app.screens.NewUserTraineeScreen
import il.kmi.shared.prefs.KmiPrefs
import il.kmi.app.ui.DrawerBridge

// ראוטים כמחרוזות פשוטות (אין תלות חיצונית)
private object RRoutes {
    const val Landing             = "registration_landing"
    // const val Auth             = "registration_auth"    // ← כבר לא בשימוש
    const val NewUserTrainee      = "new_user_trainee"
    const val NewUserCoach        = "new_user_coach"
    const val ExistingUserTrainee = "existing_user_trainee"
    const val ExistingUserCoach   = "existing_user_coach"
}

@Composable
fun RegistrationNavHost(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    onOpenDrawer: () -> Unit = { DrawerBridge.open() },
    onRegistrationDone: () -> Unit,
    onOpenLegal: () -> Unit,               // ← חדש
    onOpenTerms: () -> Unit = onOpenLegal  // ← חדש (ברירת מחדל)
) {

    val ctx = LocalContext.current
    fun go(route: String) = nav.navigate(route)

    NavHost(
        navController = nav,
        startDestination = RRoutes.Landing
    ) {
        composable(RRoutes.Landing) {
            RegistrationLandingScreen(
                // במקום לעבור דרך מסך Auth – הולכים ישירות למסכי משתמש חדש
                onNewUserTrainee = { nav.navigate(RRoutes.NewUserTrainee) },
                onExistingUserTrainee = { nav.navigate(RRoutes.ExistingUserTrainee) },
                onNewUserCoach = { nav.navigate(RRoutes.NewUserCoach) },
                onExistingUserCoach = { nav.navigate(RRoutes.ExistingUserCoach) },
                onOpenDrawer = onOpenDrawer,
                showTopBar = true,
                sp = sp,
                onGoHome = onRegistrationDone,
                autoSkipIfLoggedIn = false,
                onOpenLegal = onOpenLegal,
                onOpenTerms = onOpenTerms
            )
        }

        // ===== משתמש חדש – מתאמן =====
        composable(
            route = "${RRoutes.NewUserTrainee}?skipOtp={skipOtp}",
            arguments = listOf(
                navArgument("skipOtp") { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            val skipOtp = entry.arguments?.getBoolean("skipOtp") ?: false

            NewUserTraineeScreen(
                nav = nav,
                vm = vm,
                kmiPrefs = kmiPrefs,
                onBack = { nav.popBackStack() },
                onRegistrationComplete = { onRegistrationDone() },
                onOpenTerms = onOpenTerms,
                onOpenLegal = onOpenLegal,
                onOpenDrawer = onOpenDrawer,
                sp = sp,
                skipOtp = skipOtp
            )
        }

        // ===== משתמש חדש – מאמן =====
        composable(
            route = "${RRoutes.NewUserCoach}?skipOtp={skipOtp}",
            arguments = listOf(
                navArgument("skipOtp") { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            val skipOtp = entry.arguments?.getBoolean("skipOtp") ?: false

            NewUserCoachScreen(
                onBack = { nav.popBackStack() },
                onRegistrationComplete = { onRegistrationDone() },
                onOpenLegal = onOpenLegal,
                onOpenTerms = onOpenTerms,
                vm = vm,
                sp = sp,
                kmiPrefs = kmiPrefs,
                skipOtp = skipOtp
            )
        }

        // ===== משתמש קיים (מתאמן) =====
        composable(route = RRoutes.ExistingUserTrainee) {
            ExistingUserTraineeScreen(
                onBack = { nav.popBackStack() },
                // לאחר התחברות מוצלחת – ניגשים הביתה ולא ללנדינג
                onLoginComplete = { onRegistrationDone() },
                onOpenRecovery = {},
                vm = vm,
                sp = sp,
                kmiPrefs = kmiPrefs,
                onOpenDrawer = onOpenDrawer
            )
        }

        // ===== משתמש קיים – מאמן =====
        composable(RRoutes.ExistingUserCoach) {
            ExistingUserCoachScreen(
                onBack = { nav.popBackStack() },
                // גם למאמן – כניסה הביתה לאחר התחברות
                onLoginComplete = { onRegistrationDone() },
                onOpenRecovery = {},
                vm = vm,
                sp = sp,
                kmiPrefs = kmiPrefs,
                onOpenDrawer = onOpenDrawer
            )
        }
    }
}
