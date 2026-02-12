package il.kmi.app.navigation

import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.BeltQuestions.BeltQuestionsByBeltScreen
import il.kmi.app.screens.PracticeByTopicsSelection

/**
 * גרף “אימון/תוכן”.
 * בשלב זה כולל רק BeltQ כדי למנוע התנגשות חתימות ושגיאות import.
 * את מסך Exercise נרשום אחרי שנוודא את החבילה/החתימה המדויקת של הקומפוננטה.
 */

fun NavGraphBuilder.trainingNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // ---- בחירת חגורה (BeltQ) ----
    composable(Route.BeltQ.route) {

        // ✅ קביעה אם המשתמש במצב מאמן לפי user_role ב-SharedPreferences
        val isCoach = remember {
            val role = (sp.getString("user_role", "") ?: "").lowercase()
            role == "coach" ||
                    role.contains("coach") ||
                    role.contains("מאמן") ||
                    role.contains("מדריך")
        }

        BeltQuestionsByBeltScreen(
            vm = vm,
            kmiPrefs = kmiPrefs,
            isCoach = isCoach,

            onNext = {
                nav.navigate(Route.Topics.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onBackHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(Route.Home.route) { inclusive = true }
                    launchSingleTop = true
                }
            },

            // פתיחת נושא רגיל (לפי חגורה)
            onOpenTopic = { belt, topic ->
                val subs: List<String> =
                    il.kmi.app.domain.AppSubTopicRegistry.getSubTopicsFor(belt, topic)

                val hasRealSubs = subs.any { st ->
                    val t = st.trim()
                    t.isNotEmpty() && !t.equals(topic.trim(), ignoreCase = true)
                }

                val route =
                    if (hasRealSubs) Route.SubTopics.make(belt, topic)
                    else Route.Materials.make(belt, topic)

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // פתיחת מסך תתי־נושאים להגנות/שחרורים וכד'
            onOpenDefenseMenu = { belt, topic ->
                nav.navigate(Route.SubTopics.make(belt, topic)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // 🔹 פתיחת מסך "תרגילים לפי נושא" (SubjectExercisesScreen)
            onOpenSubject = { subject ->
                val safeId = Uri.encode(subject.id)

                // ✅ אל תבנה מחרוזת ידנית. זה מה שגורם ל-title "ליפול".
                val route = Route.SubjectExercises.make(
                    subjectId = safeId,
                    beltId = "",
                    title = subject.titleHeb
                )

                Log.e(
                    "KMI-TITLE",
                    "NAV -> SubjectExercises: subjectId='${subject.id}' title='${subject.titleHeb}' route='$route'"
                )

                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // ✅ "פתח מסך תתי נושאים" מתוך ההרחבה בכרטיס
            onOpenSubTopic = { belt, topic, subTopic ->
                // ✅ הכי נכון: להיכנס ישירות למסך החומרים של תת־נושא
                nav.navigate(Route.MaterialsSub.make(belt, topic, subTopic)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // דיאלוג הסבר לתרגיל בודד (כמו שהיה קודם)
            onOpenExercise = { key ->
                // פה אתה משאיר בדיוק את הגוף שהיה לך קודם
            },

            // ✅✅✅ חיבור הכפתורים הצפים בדיוק כמו ב-TopicsScreen/NavGraph

            onOpenWeakPoints = { belt ->
                nav.navigate(Route.WeakPoints.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenAllLists = { belt ->
                // זה היעד האמיתי אצלך (כמו topicsNavGraph)
                runCatching {
                    nav.navigate(route = "ex_tabs_all/${belt.id}")
                }
            },

            // ✅ תרגול: 3 מצבים (דיאלוג בחירה)
            onOpenRandomPractice = { belt ->
                nav.navigate(Route.Practice.make(belt)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenFinalExam = { belt ->
                android.util.Log.e("KMI-NAV", "FINAL_EXAM (from trainingNavGraph) -> belt=${belt.id}")

                nav.navigate(Route.Exam.make(belt)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onPracticeByTopics = { selection: PracticeByTopicsSelection ->
                android.util.Log.d("KMI-NAV", "PracticeByTopics selection=$selection")
            },

            onOpenSummaryScreen = { belt ->
                nav.navigate(Route.Summary.make(belt)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenVoiceAssistant = { _ ->
                nav.navigate(Route.VoiceAssistant.route) {
                    launchSingleTop = true
                }
            },

            onOpenPdfMaterials = { belt ->
                // אם יש לך מסלול/מסך ייעודי ל-PDF החלף כאן
                nav.navigate(Route.Materials.make(belt, topic = "")) {
                    launchSingleTop = true
                }
            },
        )
    }
}
