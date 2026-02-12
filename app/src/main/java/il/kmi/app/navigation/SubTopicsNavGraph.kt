package il.kmi.app.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.Route
import il.kmi.shared.domain.Belt
import il.kmi.app.screens.SubTopicsScreen

/**
 * גרף תתי־נושאים.
 * נתיב: subtopics/{beltId}/{topic}
 */
fun NavGraphBuilder.subTopicsNavGraph(
    nav: NavHostController
) {
    composable(
        route = Route.SubTopics.route,   // נניח שזה "subtopics/{beltId}/{topic}"
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topic")  { type = NavType.StringType },
        )
    ) { backStackEntry ->
        val beltId: String = backStackEntry.arguments?.getString("beltId").orEmpty()
        val topicArg: String  = backStackEntry.arguments?.getString("topic").orEmpty()
        val belt: Belt     = Belt.fromId(beltId) ?: Belt.GREEN

        // מפענחים topic מהנתיב (למקרה שהגיע מקודד)
        val topicDecoded: String = runCatching { Uri.decode(topicArg) }.getOrElse { topicArg }

        SubTopicsScreen(
            belt = belt,
            topic = topicDecoded,
            onBack = { nav.popBackStack() },
            onHome = {
                // קודם ננסה לחזור לבית אם הוא כבר ב־back stack; אם לא – ננווט אליו
                val popped = nav.popBackStack(Route.Home.route, inclusive = false)
                if (!popped) {
                    nav.navigate(Route.Home.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            // לחיצה על כפתור של תת־נושא -> נפתח את החומר של התת־נושא
            onOpenSubTopic = { subTopicTitle: String ->
                // ✅ חשוב: לא עושים encode כאן! Route.MaterialsSub.make כבר מקודד בעצמו.
                val route = Route.MaterialsSub.make(
                    belt = belt,
                    topic = topicDecoded,
                    subTopic = subTopicTitle
                )

                runCatching {
                    nav.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }.onFailure { e ->
                    android.util.Log.e(
                        "KMI-NAV",
                        "navigate MaterialsSub failed: topic=$topicDecoded sub=$subTopicTitle",
                        e
                    )
                }
            },
            // בחירת תרגיל מהחיפוש → ניווט ל־Exercise (נשאר כפי שהיה)
            onOpenExercise = { key: String ->
                val trimmed = key.trim()
                if (trimmed.isEmpty()) return@SubTopicsScreen

                val safeId = runCatching { android.net.Uri.encode(trimmed) }.getOrElse { trimmed }
                val route  = Route.Exercise.make(safeId)

                // ✅ אם כבר יש את המסך הזה בסטאק – נחזור אליו במקום לפתוח חדש
                val popped = nav.popBackStack(route, inclusive = false)
                if (!popped) {
                    runCatching {
                        nav.navigate(route) {
                            launchSingleTop = true
                        }
                    }.onFailure { e ->
                        android.util.Log.e("KMI-NAV", "navigate Exercise failed: id=$safeId", e)
                    }
                }
            }
        )
    }
}