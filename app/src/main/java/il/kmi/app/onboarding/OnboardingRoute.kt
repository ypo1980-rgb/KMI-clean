package il.kmi.app.onboarding

import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object OnboardingRoute {

    const val manualArg = "manual"

    const val route =
        "onboarding?$manualArg={$manualArg}"

    fun build(
        manual: Boolean
    ): String {
        return "onboarding?$manualArg=$manual"
    }
}

fun NavGraphBuilder.onboardingNavGraph(
    nav: NavHostController,
    isEnglish: Boolean,
    onFinished: () -> Unit
) {
    composable(
        route = OnboardingRoute.route,
        arguments = listOf(
            navArgument(OnboardingRoute.manualArg) {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { entry ->
        val context = LocalContext.current

        val manual = entry.arguments
            ?.getBoolean(OnboardingRoute.manualArg)
            ?: false

        fun closeOnboarding() {
            val popped = nav.popBackStack()

            if (!popped) {
                onFinished()
            }
        }

        OnboardingScreen(
            isEnglish = isEnglish,
            allowSkip = true,
            onFinish = {
                /*
                 * בפתיחה אוטומטית מסמנים שההדרכה הושלמה.
                 * בפתיחה ידנית אין צורך לשנות את הדגל הקיים.
                 */
                if (!manual) {
                    OnboardingPreferences.markCompleted(context)
                }

                closeOnboarding()
            },
            onSkip = {
                /*
                 * גם דילוג בהפעלה הראשונה נחשב להשלמת ההדרכה,
                 * כדי שהיא לא תיפתח שוב בכל כניסה.
                 */
                if (!manual) {
                    OnboardingPreferences.markCompleted(context)
                }

                closeOnboarding()
            }
        )
    }
}