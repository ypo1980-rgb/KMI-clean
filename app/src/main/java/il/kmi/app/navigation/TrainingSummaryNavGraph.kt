// TrainingSummaryNavGraph.kt
package il.kmi.app.navigation

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.shared.domain.Belt
import il.kmi.app.ui.training.TrainingSummaryScreen
import il.kmi.app.ui.training.TrainingSummaryViewModel
import il.kmi.shared.prefs.KmiPrefs

fun NavGraphBuilder.trainingSummaryNavGraph(
    nav: NavHostController,
    kmiVm: KmiViewModel,
    summaryVm: TrainingSummaryViewModel,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    onBack: (() -> Unit)? = null
) {
    composable(route = Route.TrainingSummary.route) { backStackEntry ->

        val pickedDateIso = backStackEntry.arguments
            ?.getString("date")
            ?.trim()
            .orEmpty()

        // ✅ ב-Vm החגורה בדרך כלל מגיעה כ-il.kmi.shared.domain.Belt (או nullable)
        // לכן אוספים אותה ואז ממירים ל-il.kmi.app.domain.Belt לפי id.
        val sharedBelt = kmiVm.selectedBelt.collectAsState(initial = null).value

        val belt: Belt = run {
            val id = sharedBelt?.id
            if (id.isNullOrBlank()) Belt.GREEN
            else Belt.values().firstOrNull { it.id == id } ?: Belt.GREEN
        }

        // ✅ מקור אמת לסימוני סיכומים בלוח השנה.
        // MonthlyCalendarScreen קורא מכאן את training_summary_days,
        // ולכן גם TrainingSummaryScreen חייב לשמור לאותו מקום.
        val summarySp = nav.context.getSharedPreferences(
            "kmi_training_summary",
            Context.MODE_PRIVATE
        )

        TrainingSummaryScreen(
            vm = summaryVm,
            sp = summarySp,
            kmiPrefs = kmiPrefs,
            belt = belt,
            pickedDateIso = pickedDateIso.ifBlank { null },
            onBack = onBack,
            onHome = {
                nav.navigate(Route.Home.route) {
                    launchSingleTop = true
                    restoreState = false

                    popUpTo(nav.graph.startDestinationId) {
                        inclusive = false
                    }
                }
            },
            onOpenCalendar = {
                nav.navigate(Route.MonthlyCalendar.route) {
                    launchSingleTop = true
                }
            }
        )
    }
}