package il.kmi.app.navigation

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.Route
import il.kmi.app.attendance.AttendanceStatsScreen
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.attendance.ui.AttendanceGroupStatsScreen
import il.kmi.app.attendance.ui.AttendanceScreen
import il.kmi.app.attendance.ui.AttendanceViewModel
import java.time.LocalDate
import androidx.compose.ui.platform.LocalContext

/**
 * גרף נוכחות:
 *  - סימון יומי:  attendance/mark/{branch}/{groupKey}
 *  - סטטיסטיקות: attendance/stats/{branch}/{groupKey}?memberId={memberId}&memberName={memberName}
 */
fun NavGraphBuilder.attendanceNavGraph(
    nav: NavHostController
) {

    fun String.cleanAttendancePart(): String =
        trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")

    fun firstAttendanceValue(raw: String?): String =
        raw.orEmpty()
            .replace(" • ", ",")
            .replace("|", ",")
            .replace("\n", ",")
            .split(',', ';', '；')
            .map { it.cleanAttendancePart() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

    fun safeAttendanceMarkRoute(branch: String, groupKey: String): String? {
        val b = firstAttendanceValue(branch)
        val g = firstAttendanceValue(groupKey)

        if (b.isBlank() || g.isBlank()) return null

        return Route.AttendanceMark.make(
            branch = Uri.encode(b),
            groupKey = Uri.encode(g)
        )
    }

    fun safeAttendanceGroupStatsRoute(branch: String, groupKey: String): String? {
        val b = firstAttendanceValue(branch)
        val g = firstAttendanceValue(groupKey)

        if (b.isBlank() || g.isBlank()) return null

        return Route.AttendanceGroupStats.make(
            branch = Uri.encode(b),
            groupKey = Uri.encode(g)
        )
    }

    fun defaultAttendanceContextFromPrefs(context: android.content.Context): Pair<String, String> {
        val sp = context.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)

        val branch = firstAttendanceValue(
            sp.getString("active_branch", null)
                ?: sp.getString("branch", null)
                ?: sp.getString("branches", null)
                ?: sp.getString("coach_branch", null)
                ?: sp.getString("coach_branches", null)
                ?: sp.getString("coachBranches", null)
        )

        val group = firstAttendanceValue(
            sp.getString("active_group", null)
                ?: sp.getString("groupKey", null)
                ?: sp.getString("group", null)
                ?: sp.getString("groups", null)
                ?: sp.getString("coach_group", null)
                ?: sp.getString("coach_groups", null)
                ?: sp.getString("coachGroups", null)
        )

        return branch to group
    }

    // ----- כניסה בטוחה מסרגל צד ללא פרמטרים ----- //
    composable(route = "attendance/mark") {
        val ctx = LocalContext.current
        val (branch, groupKey) = remember(ctx) {
            defaultAttendanceContextFromPrefs(ctx)
        }

        LaunchedEffect(branch, groupKey) {
            val route = safeAttendanceMarkRoute(branch, groupKey)

            if (route != null) {
                nav.navigate(route) {
                    popUpTo("attendance/mark") { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }

    composable(route = "attendance/groupStats") {
        val ctx = LocalContext.current
        val (branch, groupKey) = remember(ctx) {
            defaultAttendanceContextFromPrefs(ctx)
        }

        LaunchedEffect(branch, groupKey) {
            val route = safeAttendanceGroupStatsRoute(branch, groupKey)

            if (route != null) {
                nav.navigate(route) {
                    popUpTo("attendance/groupStats") { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }

    // ----- סימון נוכחות ----- //
    composable(
        route = Route.AttendanceMark.route,
        arguments = listOf(
            navArgument("branch")   { type = NavType.StringType },
            navArgument("groupKey") { type = NavType.StringType }
        )
    ) { e ->
        val branch = e.arguments
            ?.getString("branch")
            .orEmpty()
            .let { Uri.decode(it) }
            .cleanAttendancePart()

        val groupKey = e.arguments
            ?.getString("groupKey")
            .orEmpty()
            .let { Uri.decode(it) }
            .cleanAttendancePart()

        val vmKey = "attendance_${branch}_${groupKey}"
            .replace("/", "_")
            .replace("\\", "_")
            .replace("|", "_")
            .replace(" ", "_")
            .replace("+", "_")

        val attVm: AttendanceViewModel = viewModel(
            key = vmKey
        )

        AttendanceScreen(
            vm = attVm,
            date = LocalDate.now(),
            branch = branch,
            groupKey = groupKey,
            onOpenMemberStats = { memberId: Long?, memberName: String ->
                val current = attVm.uiState.value
                val currentBranch = current.branch.ifBlank { branch }
                val currentGroup = current.groupKey.ifBlank { groupKey }

                val route = if (memberId != null && memberId > 0L) {
                    Route.AttendanceStats.make(
                        branch = Uri.encode(currentBranch),
                        groupKey = Uri.encode(currentGroup),
                        memberId = memberId,
                        memberName = Uri.encode(memberName)
                    )
                } else {
                    Route.AttendanceStats.make(
                        branch = Uri.encode(currentBranch),
                        groupKey = Uri.encode(currentGroup),
                        memberName = Uri.encode(memberName)
                    )
                }

                nav.navigate(route) {
                    launchSingleTop = true
                }
            },
            onOpenGroupStats = { b, g ->
                val route = safeAttendanceGroupStatsRoute(b, g)
                if (route != null) {
                    nav.navigate(route) {
                        launchSingleTop = true
                    }
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
        val branch = e.arguments
            ?.getString("branch")
            .orEmpty()
            .let { Uri.decode(it) }
            .cleanAttendancePart()

        val groupKey = e.arguments
            ?.getString("groupKey")
            .orEmpty()
            .let { Uri.decode(it) }
            .cleanAttendancePart()

        val memberId = e.arguments
            ?.getString("memberId")
            ?.toLongOrNull()

        val memberName = e.arguments
            ?.getString("memberName")
            ?.let { Uri.decode(it) }

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
        val branch = e.arguments
            ?.getString("branch")
            .orEmpty()
            .let { Uri.decode(it) }
            .cleanAttendancePart()

        val groupKey = e.arguments
            ?.getString("groupKey")
            .orEmpty()
            .let { Uri.decode(it) }
            .cleanAttendancePart()

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
