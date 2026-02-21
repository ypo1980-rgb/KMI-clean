package il.kmi.app.navigation

import android.content.SharedPreferences
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.Route
import il.kmi.app.DataStoreManager
import il.kmi.app.StatsVm
import il.kmi.app.screens.SettingsScreenModern
import kotlinx.coroutines.runBlocking

/**
 * גרף למסכי ההגדרות.
 * שים לב: אינו משנה התנהגות כל עוד הדגל nav_split_enabled כבוי.
 */
fun NavGraphBuilder.settingsNavGraph(
    nav: NavHostController,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs,
    themeMode: String,
    onThemeChange: (String) -> Unit
) {
    composable(Route.Settings.route) {
        val ctx = LocalContext.current

        val spTrainingSummary = remember {
            ctx.getSharedPreferences("kmi_training_summary", android.content.Context.MODE_PRIVATE)
        }

        // ✅ VM הראשי שלך (מה שאתה כבר משתמש בו בשאר האפליקציה)
        val kmiVm: il.kmi.app.KmiViewModel = viewModel(
            factory = il.kmi.app.KmiViewModelFactory(
                dataStoreManager = DataStoreManager(context = ctx),
                spTrainingSummary = spTrainingSummary
            )
        )

        // ✅ Adapter: KmiViewModel -> StatsVm (מה ש-SettingsScreenModern מצפה לו)
        val statsVm: StatsVm = remember(kmiVm) {
            object : StatsVm {

                override fun getItemStatusNullable(
                    belt: il.kmi.shared.domain.Belt,
                    topic: String,
                    item: String
                ): Boolean? = runBlocking {
                    runCatching { kmiVm.getItemStatusNullable(belt, topic, item) }.getOrNull()
                }

                override fun isMastered(
                    belt: il.kmi.shared.domain.Belt,
                    topic: String,
                    item: String
                ): Boolean = runBlocking {
                    runCatching { kmiVm.isMastered(belt, topic, item) }.getOrDefault(false)
                }
            }
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
            onOpenProgress = { nav.navigate(Route.Progress.route) },
            onOpenCoachBroadcast = { nav.navigate(Route.CoachBroadcast.route) },

            vm = statsVm
        )
    }
}
