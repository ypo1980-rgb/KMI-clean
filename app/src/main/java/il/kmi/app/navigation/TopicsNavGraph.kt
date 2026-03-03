package il.kmi.app.navigation

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.exercises.TopicRepoExercisesScreen
import il.kmi.app.screens.BeltQuestions.BeltQuestionsByTopicScreen
import il.kmi.shared.domain.Belt

fun NavGraphBuilder.topicsNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // ✅ זה המסך שמציג "נושאים (קטגוריות)" + תתי-נושאים
    composable(Route.Topics.route) {

        val appCtx = LocalContext.current
        val userSp = remember(appCtx) {
            appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
        }

        val isCoachFlag = remember(sp, userSp) {
            runCatching {
                val role1 = sp.getString("user_role", null)?.lowercase().orEmpty()
                val role2 = userSp.getString("user_role", null)?.lowercase().orEmpty()

                role1 == "coach" || role1.contains("coach") || role1.contains("מאמן") || role1.contains("מדריך") ||
                        role2 == "coach" || role2.contains("coach") || role2.contains("מאמן") || role2.contains("מדריך") ||
                        sp.getBoolean("isCoach", false) ||
                        userSp.getBoolean("isCoach", false)
            }.getOrDefault(false)
        }

        // ✅ DEBUG קטן שיעזור לנו לדעת שהגענו למסך הנכון
        Log.e("KMI_TOPICS", "ENTER Route.Topics isCoach=$isCoachFlag")

        BeltQuestionsByTopicScreen(

            // ✅ חובה למסך "לפי נושא" – פתיחה למסך SubjectExercises
            onOpenSubject = { belt: Belt, subject ->
                vm.setSelectedBelt(belt)

                val route = Route.SubjectExercises.make(
                    subjectId = Uri.encode(subject.id),
                    beltId = belt.id,
                    title = subject.titleHeb
                )

                Log.e("KMI_TOPICS", "onOpenSubject belt=${belt.id} subjectId='${subject.id}' route='$route'")

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // ✅ NEW: חיבור הגנות פנימיות/חיצוניות למסך ההגנות החדש (Route.SubjectExercises כבר הוגדר אצלך ב-MainNavHost)
            onOpenDefenseList = { belt, kind, pick ->
                vm.setSelectedBelt(belt)

                // ✅ ניווט למסך ההגנות הייעודי (המסך שיודע לפלח/להציג נכון)
                val route = Route.Defenses.make(
                    belt = belt,
                    kind = kind,   // "internal" / "external" / "all" / וכו' (כמו שאתה שולח)
                    pick = pick    // "punch" / "kick" / "kick:straight_groin" / וכו'
                )

                Log.e(
                    "KMI_TOPICS",
                    "onOpenDefenseList belt=${belt.id} kind=$kind pick=$pick route='$route'"
                )

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // ✅ אם תרצה שנושא רגיל יפתח Materials המעוצב - השאר כך.
            // אם אתה רוצה שכל נושא יפתח TopicRepoExercises - תגיד ואשנה.
            onOpenTopic = { belt: Belt, topicTitle: String ->
                vm.setSelectedBelt(belt)

                val route = Route.Materials.make(belt = belt, topic = topicTitle)

                Log.e("KMI_TOPICS", "onOpenTopic belt=${belt.id} topic='$topicTitle' route='$route'")

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onBackHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.startDestinationId) {
                        inclusive = false
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenWeakPoints = { belt ->
                vm.setSelectedBelt(belt)
                nav.navigate(Route.WeakPoints.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenAllLists = { belt ->
                vm.setSelectedBelt(belt)
                runCatching { nav.navigate("ex_tabs_all/${belt.id}") }
            },

            onOpenSummaryScreen = { belt ->
                vm.setSelectedBelt(belt)
                nav.navigate(Route.Summary.make(belt)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenVoiceAssistant = { _ ->
                nav.navigate(Route.VoiceAssistant.route) { launchSingleTop = true }
            },

            onOpenPdfMaterials = { belt ->
                vm.setSelectedBelt(belt)
                nav.navigate(Route.Materials.make(belt = belt, topic = "")) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }

    // ✅ יעד תרגילים לפי TopicRepo (נשאר אצלך)
    composable(
        route = Route.TopicRepoExercises.route,
        arguments = listOf(
            navArgument("beltId") { type = NavType.StringType },
            navArgument("topicId") { type = NavType.StringType },
            navArgument("subTopicId") { type = NavType.StringType }
        )
    ) { entry ->
        val beltIdEnc = entry.arguments?.getString("beltId").orEmpty()
        val topicIdEnc = entry.arguments?.getString("topicId").orEmpty()
        val subTopicIdEnc = entry.arguments?.getString("subTopicId").orEmpty()

        // ✅ FIX: לפענח לפני שמעבירים למסך
        val beltId = Uri.decode(beltIdEnc)
        val belt = Belt.fromId(beltId) ?: Belt.GREEN

        val topicId = Uri.decode(topicIdEnc).trim()
        val subTopicId = Uri.decode(subTopicIdEnc).trim()

        TopicRepoExercisesScreen(
            belt = belt,
            topicId = topicId,
            subTopicId = subTopicId,
            onBack = { nav.popBackStack() }
        )
    }
}
