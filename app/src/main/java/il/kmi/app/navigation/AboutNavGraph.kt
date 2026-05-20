package il.kmi.app.navigation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.AboutMethodScreen
import il.kmi.app.screens.AboutNetworkScreen
import il.kmi.app.screens.ForumScreen
import il.kmi.app.screens.legal.LegalScreen
import il.kmi.app.screens.AboutAviAbisidonScreen
import il.kmi.app.screens.AboutItzikScreen

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.aboutNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // ---- Legal / Terms / Privacy (בשלב זה כולם מפנים לאותו מסך) ----
    composable(Route.Legal.route) {
        LegalScreen(onBack = { nav.popBackStack() })
    }

    // ---- פורום ----
    composable(Route.Forum.route) {
        val ctx = LocalContext.current

        LaunchedEffect(Unit) {
            val forumPushSp = ctx.applicationContext.getSharedPreferences(
                "kmi_forum_push",
                Context.MODE_PRIVATE
            )

            val roomId = forumPushSp.getString("forum_room_id", "").orEmpty()
            val roomName = forumPushSp.getString("forum_room_name", "").orEmpty()
            val messageId = forumPushSp.getString("forum_message_id", "").orEmpty()
            val branchId = forumPushSp.getString("forum_branch_id", "").orEmpty()
            val groupKey = forumPushSp.getString("forum_group_key", "").orEmpty()
            val senderId = forumPushSp.getString("forum_sender_id", "").orEmpty()
            val receivedAt = forumPushSp.getLong("received_at", 0L)

            val hasForumTarget =
                roomId.isNotBlank() ||
                        messageId.isNotBlank() ||
                        branchId.isNotBlank() ||
                        groupKey.isNotBlank()

            if (hasForumTarget) {
                Log.e(
                    "KMI_FORUM_PUSH",
                    "Route.Forum entered with push target roomId=$roomId messageId=$messageId branchId=$branchId groupKey=$groupKey"
                )

                sp.edit()
                    .putBoolean("forum_open_from_push", true)
                    .putString("forum_push_room_id", roomId)
                    .putString("forum_push_room_name", roomName)
                    .putString("forum_push_message_id", messageId)
                    .putString("forum_push_branch_id", branchId)
                    .putString("forum_push_group_key", groupKey)
                    .putString("forum_push_sender_id", senderId)
                    .putLong("forum_push_received_at", receivedAt)
                    .apply()

                // משאירים את פרטי היעד לזמן קצר ב-sp הראשי,
                // אבל מנקים את דגל ההמתנה כדי שלא יפתח שוב בלולאה.
                forumPushSp.edit()
                    .putBoolean("has_pending_forum_push", false)
                    .apply()
            }
        }

        ForumScreen(
            sp = sp,
            onBack = { nav.popBackStack() },
            onOpenSubscription = {
                nav.navigate(Route.Subscription.route)
            },
            onGoHome = {
                nav.navigate(Route.Home.route) {
                    // לוודא שחוזרים הביתה ולא בונים סטאק מוזר
                    popUpTo(Route.Home.route) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        )
    }

    // ---- אודות הרשת ----
    composable(Route.AboutNetwork.route) {
        AboutNetworkScreen(onBack = { nav.popBackStack() })
    }

    // ---- אודות השיטה ----
    composable(Route.AboutMethod.route) {
        AboutMethodScreen(
            onBack = { nav.popBackStack() },
            onHome = {
                nav.navigate(Route.Home.route) {
                    launchSingleTop = true
                    popUpTo(Route.Home.route) { inclusive = false }
                }
            }
        )
    }

// ---- אודות אבי אביסידון ----
    composable(Route.AboutAvi.route) {
        AboutAviAbisidonScreen(
            onClose = { nav.popBackStack() },
            onHome = {
                nav.navigate(Route.Home.route) {
                    launchSingleTop = true
                    popUpTo(Route.Home.route) { inclusive = false }
                }
            }
        )
    }

    // ---- אודות איציק ----
    composable(Route.AboutItzik.route) {
        AboutItzikScreen(
            onBack = { nav.popBackStack() }
        )
    }

    // ---- דרגו אותנו (לוגיקה קיימת נשארת במסך עצמו; כאן אין יעד נפרד) ----
    // אם בעתיד תרצה מסך ביניים ל-RateUs – נפתח כאן composable ייעודי.
}
