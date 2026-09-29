package il.kmi.app.free_sessions.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.Route
import il.kmi.app.free_sessions.ui.FreeSessionsScreen
import com.google.firebase.auth.FirebaseAuth

fun NavGraphBuilder.freeSessionsNavGraph(
    nav: NavHostController
) {
    composable(
        route = FreeSessionsRoute.route,
        arguments = listOf(
            navArgument("branch") { type = NavType.StringType },
            navArgument("groupKey") { type = NavType.StringType },
            navArgument("uid") { type = NavType.StringType },
            navArgument("name") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val branch =
            backStackEntry.arguments
                ?.getString("branch")
                .orEmpty()

        val groupKey =
            backStackEntry.arguments
                ?.getString("groupKey")
                .orEmpty()

        val currentAuthUid =
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid
                .orEmpty()

        val authorizedBranchGroupPairs by produceState(
            initialValue = emptyList<Pair<String, String>>(),
            key1 = currentAuthUid
        ) {
            value =
                if (currentAuthUid.isBlank()) {
                    emptyList()
                } else {
                    runCatching {
                        val snap =
                            FirebaseFirestore.getInstance()
                                .collection("authorizedCoaches")
                                .document(currentAuthUid)
                                .get()
                                .await()

                        val authorizedBranchGroups =
                            snap.get("authorizedBranchGroups")
                                    as? List<*>
                                ?: emptyList<Any>()

                        authorizedBranchGroups
                            .mapNotNull { raw ->
                                val value =
                                    raw?.toString()
                                        ?.trim()
                                        .orEmpty()

                                val separatorIndex =
                                    value.indexOf("||")

                                if (
                                    separatorIndex <= 0 ||
                                    separatorIndex >= value.lastIndex - 1
                                ) {
                                    null
                                } else {
                                    val authorizedBranch =
                                        value.substring(
                                            0,
                                            separatorIndex
                                        ).trim()

                                    val authorizedGroup =
                                        value.substring(
                                            separatorIndex + 2
                                        ).trim()

                                    if (
                                        authorizedBranch.isBlank() ||
                                        authorizedGroup.isBlank()
                                    ) {
                                        null
                                    } else {
                                        authorizedBranch to authorizedGroup
                                    }
                                }
                            }
                            .distinct()
                    }.getOrDefault(emptyList())
                }
        }

        val name =
            backStackEntry.arguments
                ?.getString("name")
                .orEmpty()

        val selectedCalendarDateIso =
            backStackEntry.savedStateHandle
                .getStateFlow(
                    "free_session_selected_date",
                    ""
                )
                .collectAsState()
                .value

        FreeSessionsScreen(
            branch = branch,
            groupKey = groupKey,
            currentUid = currentAuthUid,
            currentName = name,
            authorizedBranchGroupPairs = authorizedBranchGroupPairs,
            selectedCalendarDateIso = selectedCalendarDateIso,
            onOpenCalendar = {
                backStackEntry.savedStateHandle[
                    "monthly_calendar_mode"
                ] = "free_session_date_picker"

                nav.navigate(Route.MonthlyCalendar.route) {
                    launchSingleTop = true
                }
            },
            onCalendarDateConsumed = {
                backStackEntry.savedStateHandle[
                    "free_session_selected_date"
                ] = ""
            },
            onBack = {
                nav.popBackStack()
            }
        )
    }
}
