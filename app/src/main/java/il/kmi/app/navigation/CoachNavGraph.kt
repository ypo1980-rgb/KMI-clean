package il.kmi.app.navigation

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.attendance.ui.AttendanceScreen
import il.kmi.app.attendance.ui.AttendanceViewModel
import il.kmi.app.training.TrainingCatalog
import java.time.LocalDate

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.coachNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // --- שידור מאמן ---
    composable(Route.CoachBroadcast.route) {
        val regionDefault = kmiPrefs.region
        val branchDefault = kmiPrefs.branch

        val ctx = LocalContext.current

        il.kmi.app.screens.coach.CoachBroadcastScreen(
            branchesByRegion = TrainingCatalog.branchesByRegion,
            defaultRegion = regionDefault,
            defaultBranch = branchDefault,
            onBack = { nav.popBackStack() },
            onHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            },

            // פתיחת אפליקציית SMS עם כל המספרים המסומנים
            onOpenSms = { numbers, message ->
                if (numbers.isEmpty()) return@CoachBroadcastScreen

                val uri = "smsto:" + numbers.joinToString(";")
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(uri)
                    putExtra("sms_body", message)
                }

                runCatching {
                    ctx.startActivity(intent)
                }.onFailure { e ->
                    android.util.Log.e(
                        "CoachBroadcast",
                        "failed to open SMS app numbers=${numbers.size}",
                        e
                    )
                }
            },

            // שיתוף טקסט כללי: וואטסאפ / מייל / טלגרם וכו'
            onShareText = { message ->
                if (message.isBlank()) return@CoachBroadcastScreen

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }

                val chooser = Intent.createChooser(
                    shareIntent,
                    "שתף הודעת מאמן"
                )

                runCatching {
                    ctx.startActivity(chooser)
                }.onFailure { e ->
                    android.util.Log.e(
                        "CoachBroadcast",
                        "failed to open share sheet",
                        e
                    )
                }
            }
        )
    }

    // --- נוכחות מאמן (שמרנו על ה-route הישן "attendance") ---
    composable(route = "attendance") {
        val userBranch   = kmiPrefs.branch.orEmpty()
        val userGroupKey = TrainingCatalog.normalizeGroupName(name = kmiPrefs.ageGroup.orEmpty())
        val today        = LocalDate.now()

        val attendVm: AttendanceViewModel = viewModel(
            key = "attendance_${userBranch}_${userGroupKey}"
        )

        AttendanceScreen(
            vm = attendVm,
            date = today,
            branch = userBranch,
            groupKey = userGroupKey,

            onOpenMemberStats = { memberId: Long?, memberName: String ->
                val route = if (memberId != null && memberId > 0L) {
                    Route.AttendanceStats.make(
                        branch = userBranch,
                        groupKey = userGroupKey,
                        memberId = memberId,
                        memberName = memberName
                    )
                } else {
                    Route.AttendanceStats.make(
                        branch = userBranch,
                        groupKey = userGroupKey,
                        memberName = memberName
                    )
                }
                nav.navigate(route)
            },

            // ✅ חדש: סטטיסטיקת קבוצה (שנה אחורה)
            onOpenGroupStats = { b, g ->
                nav.navigate(Route.AttendanceGroupStats.make(b, g)) {
                    launchSingleTop = true
                }
            },

            onHomeClick = {
                val popped = nav.popBackStack()
                if (!popped) {
                    nav.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}
