// TrainingSummaryNavGraph.kt
package il.kmi.app.navigation

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
    composable(route = Route.TrainingSummary.route) {

        // ✅ ב-Vm החגורה בדרך כלל מגיעה כ-il.kmi.shared.domain.Belt (או nullable)
        // לכן אוספים אותה ואז ממירים ל-il.kmi.app.domain.Belt לפי id.
        val sharedBelt = kmiVm.selectedBelt.collectAsState(initial = null).value

        val belt: Belt = run {
            val id = sharedBelt?.id
            if (id.isNullOrBlank()) Belt.GREEN
            else Belt.values().firstOrNull { it.id == id } ?: Belt.GREEN
        }

        TrainingSummaryScreen(
            vm = summaryVm,
            sp = sp,
            kmiPrefs = kmiPrefs,
            belt = belt,
            onBack = onBack
        )
    }
}