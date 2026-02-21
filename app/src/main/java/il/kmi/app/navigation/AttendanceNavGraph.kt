package il.kmi.app.navigation

import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.Route
import il.kmi.app.attendance.AttendanceStatsScreen
import il.kmi.app.attendance.ui.AttendanceScreen
import il.kmi.app.attendance.ui.AttendanceViewModel
import java.time.LocalDate
import il.kmi.app.attendance.ui.AttendanceGroupStatsScreen
import il.kmi.app.attendance.data.AttendanceRepository
import androidx.compose.ui.platform.LocalContext

/**
 * גרף נוכחות:
 *  - סימון יומי:  attendance/mark/{branch}/{groupKey}
 *  - סטטיסטיקות: attendance/stats/{branch}/{groupKey}?memberId={memberId}&memberName={memberName}
 */
fun NavGraphBuilder.attendanceNavGraph(
    nav: NavHostController
) {

    // ----- סימון נוכחות ----- //
    composable(
        route = Route.AttendanceMark.route,
        arguments = listOf(
            navArgument("branch")   { type = NavType.StringType },
            navArgument("groupKey") { type = NavType.StringType }
        )
    ) { e ->
        val branch   = e.arguments?.getString("branch").orEmpty()
        val groupKey = e.arguments?.getString("groupKey").orEmpty()

        val attVm: AttendanceViewModel = viewModel(
            key = "attendance_${branch}_${groupKey}"
        )

        AttendanceScreen(
            vm = attVm,
            date = LocalDate.now(),
            branch = branch,
            groupKey = groupKey,
            onOpenMemberStats = { memberId: Long?, memberName: String ->
                val route = if (memberId != null && memberId > 0L) {
                    Route.AttendanceStats.make(
                        branch = branch,
                        groupKey = groupKey,
                        memberId = memberId,
                        memberName = memberName
                    )
                } else {
                    Route.AttendanceStats.make(
                        branch = branch,
                        groupKey = groupKey,
                        memberName = memberName
                    )
                }
                nav.navigate(route)
            },
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

    // ----- סטטיסטיקות ----- //
    composable(
        route = Route.AttendanceStats.route,
        arguments = listOf(
            navArgument("branch")     { type = NavType.StringType },
            navArgument("groupKey")   { type = NavType.StringType },
            navArgument("memberId")   { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("memberName") { type = NavType.StringType; nullable = true; defaultValue = null }
        )
    ) { e ->
        val branch     = e.arguments?.getString("branch").orEmpty()
        val groupKey   = e.arguments?.getString("groupKey").orEmpty()
        val memberId   = e.arguments?.getString("memberId")?.toLongOrNull()
        val memberName = e.arguments?.getString("memberName")

        AttendanceStatsScreen(
            branch = branch,
            groupKey = groupKey,
            memberId = memberId,
            memberName = memberName,
            onBack = { nav.popBackStack() }
        )
    }

    // ----- סטטיסטיקת קבוצה (שנה אחורה) ----- //
    composable(
        route = Route.AttendanceGroupStats.route,
        arguments = listOf(
            navArgument("branch") { type = NavType.StringType },
            navArgument("groupKey") { type = NavType.StringType }
        )
    ) { e ->
        val branch = e.arguments?.getString("branch").orEmpty().let { android.net.Uri.decode(it) }
        val groupKey = e.arguments?.getString("groupKey").orEmpty().let { android.net.Uri.decode(it) }

        val ctx = LocalContext.current
        val app = ctx.applicationContext as android.app.Application

        // ✅ לא ליצור repo בכל ריקומפוזיציה
        val repo = remember(app) { AttendanceRepository.get(app) }

        AttendanceGroupStatsScreen(
            repo = repo,
            branch = branch,
            groupKey = groupKey,
            onBack = { nav.popBackStack() }
        )
    }
}
