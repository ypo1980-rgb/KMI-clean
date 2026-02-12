package il.kmi.app.navigation

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument              // ✅ ADD
import androidx.navigation.NavType                  // ✅ ADD
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.shared.domain.Belt
import il.kmi.app.screens.BeltQuestions.BeltQuestionsByBeltScreen
import il.kmi.app.screens.PracticeByTopicsSelection
import il.kmi.app.screens.SubjectExercisesScreen    // ✅ ADD

fun NavGraphBuilder.topicsNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    composable(Route.Topics.route) {
        // לאסוף הקשרים קומפוזבילים מחוץ ללמבדות
        val appCtx = androidx.compose.ui.platform.LocalContext.current
        val userSp = appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
        val isCoachFlag = runCatching {
            (sp.getString("user_role", null)?.equals("coach", true) == true) ||
                    (userSp.getString("user_role", null)?.equals("coach", true) == true) ||
                    sp.getBoolean("isCoach", false)
        }.getOrDefault(false)

        // ✅ NEW: מסך הנושאים החדש (גלגל חגורות)
        BeltQuestionsByBeltScreen(
            vm = vm,
            kmiPrefs = kmiPrefs,
            isCoach = isCoachFlag,

            onNext = { /* no-op */ },

            onBackHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.startDestinationId) { inclusive = false; saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // ✅ פתיחת נושא (אותה לוגיקה שהייתה ב-TopicsScreen הישן)
            onOpenTopic = { belt, topic ->
                val eb = if (belt != Belt.WHITE) belt else (vm.selectedBelt.value ?: Belt.GREEN)
                val topicEnc = Uri.encode(topic)

                val subs: List<String> =
                    il.kmi.app.domain.AppSubTopicRegistry.getSubTopicsFor(eb, topic)

                val hasRealSubs = subs.any { st ->
                    val t = st.trim()
                    t.isNotEmpty() && !t.equals(topic.trim(), ignoreCase = true)
                }

                Log.d("KMI-NAV", "Topics(NEW): openTopic belt=${eb.id} topic='$topic' enc='$topicEnc' hasSubs=$hasRealSubs")

                if (hasRealSubs) {
                    nav.navigate(Route.SubTopics.make(eb, topicEnc)) {
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    nav.navigate(Route.Materials.make(eb, topicEnc)) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },

            // ✅ תפריט הגנות / תתי-נושאים
            onOpenDefenseMenu = { belt, topic ->
                val eb = if (belt != Belt.WHITE) belt else (vm.selectedBelt.value ?: Belt.GREEN)
                val topicEnc = Uri.encode(topic)

                Log.d("KMI-NAV", "Topics(NEW): openDefenseMenu belt=${eb.id} topic='$topic' enc='$topicEnc'")

                nav.navigate(Route.SubTopics.make(eb, topicEnc)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            // ✅ ניווט למסך "תרגילים לפי נושא"
            onOpenSubject = { subject ->
                val subjectIdEnc = Uri.encode(subject.id)

                // ✅ חשוב: לנווט דרך Route.SubjectExercises.make כדי להעביר title
                val route = Route.SubjectExercises.make(
                    subjectId = subjectIdEnc,
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

            // ... (שאר הפרמטרים שלך נשארים כמו שהם)
            onOpenAllLists = { belt ->
                val eb = if (belt != Belt.WHITE) belt else (vm.selectedBelt.value ?: Belt.GREEN)
                runCatching { nav.navigate(route = "ex_tabs_all/${eb.id}") }
            },
            onOpenRandomPractice = { belt ->
                val eb = if (belt != Belt.WHITE) belt else (vm.selectedBelt.value ?: Belt.GREEN)
                nav.navigate(Route.Practice.make(eb)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenFinalExam = { belt ->
                val eb = if (belt != Belt.WHITE) belt else (vm.selectedBelt.value ?: Belt.GREEN)
                nav.navigate(Route.Exam.make(eb)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onPracticeByTopics = { selection: PracticeByTopicsSelection ->
                Log.d("KMI-NAV", "PracticeByTopics selection=$selection")
            },
            onOpenSummaryScreen = { belt ->
                val eb = if (belt != Belt.WHITE) belt else (vm.selectedBelt.value ?: Belt.GREEN)
                nav.navigate(Route.Summary.make(eb)) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenWeakPoints = { belt ->
                nav.navigate(Route.WeakPoints.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenVoiceAssistant = { belt -> },
            onOpenPdfMaterials = { belt ->
                val eb = if (belt != Belt.WHITE) belt else (vm.selectedBelt.value ?: Belt.GREEN)
                nav.navigate(Route.Materials.make(belt = eb, topic = Uri.encode(""))) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenExercise = { pickedKey ->
                fun extractItemId(key: String): String {
                    val raw = key.trim()
                    if (raw.isBlank()) return raw
                    return when {
                        '|' in raw  -> raw.split('|', limit = 3).getOrNull(2).orEmpty()
                        "::" in raw -> raw.split("::", limit = 3).getOrNull(2).orEmpty()
                        '/' in raw  -> raw.split('/', limit = 3).getOrNull(2).orEmpty()
                        else        -> raw
                    }.trim()
                }

                val itemId = extractItemId(pickedKey)
                if (itemId.isNotBlank()) {
                    nav.navigate(Route.Exercise.make(id = Uri.encode(itemId))) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}
