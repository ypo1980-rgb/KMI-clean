package il.kmi.app.navigation

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.screens.BeltQuestions.BeltQuestionsByTopicScreen
import il.kmi.shared.domain.Belt
import il.kmi.app.subscription.KmiAccess

fun isLockedPremiumTopic(raw: String): Boolean {
    val t = raw.trim().lowercase()

    return t == "שחרורים" ||
            t == "הגנות" ||

            // 🔒 כל מה שמכיל הגנות
            t.contains("הגנה") ||
            t.contains("הגנות") ||
            t.contains("defense") ||
            t.contains("defence") ||

            // 🔒 כל מה שמכיל שחרורים
            t.contains("שחרור") ||
            t.contains("release") ||

            // 🔒 קטגוריות נשק
            t.contains("סכין") ||
            t.contains("knife") ||
            t.contains("אקדח") ||
            t.contains("gun") ||
            t.contains("מקל") ||
            t.contains("stick") ||
            t.contains("תוקפים") ||

            // 🔒 מזהים פנימיים
            t.startsWith("def_") ||
            t.startsWith("releases_")
}

fun NavGraphBuilder.topicsNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    composable("hard_subject/{subjectId}") { backStackEntry ->
        val subjectId = backStackEntry.arguments
            ?.getString("subjectId")
            ?.let { Uri.decode(it) }
            .orEmpty()

        il.kmi.app.screens.UnifiedSubjectExercisesScreen(
            subjectId = subjectId,
            sectionId = null,
            onOpenSection = { nextSubjectId, sectionId ->
                val encodedSubject = Uri.encode(nextSubjectId)

                if (sectionId == null) {
                    nav.navigate("hard_subject/$encodedSubject") {
                        launchSingleTop = true
                        restoreState = false
                    }
                } else {
                    val encodedSection = Uri.encode(sectionId)
                    nav.navigate("hard_subject/$encodedSubject/$encodedSection") {
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            },
            onBack = {
                nav.popBackStack()
            },
            vm = vm
        )
    }

    composable("hard_subject/{subjectId}/{sectionId}") { backStackEntry ->
        val subjectId = backStackEntry.arguments
            ?.getString("subjectId")
            ?.let { Uri.decode(it) }
            .orEmpty()

        val sectionId = backStackEntry.arguments
            ?.getString("sectionId")
            ?.let { Uri.decode(it) }

        il.kmi.app.screens.UnifiedSubjectExercisesScreen(
            subjectId = subjectId,
            sectionId = sectionId,
            onOpenSection = { nextSubjectId, nextSectionId ->
                val encodedSubject = Uri.encode(nextSubjectId)

                if (nextSectionId == null) {
                    nav.navigate("hard_subject/$encodedSubject") {
                        launchSingleTop = true
                        restoreState = false
                    }
                } else {
                    val encodedSection = Uri.encode(nextSectionId)
                    nav.navigate("hard_subject/$encodedSubject/$encodedSection") {
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            },
            onBack = {
                nav.popBackStack()
            },
            vm = vm
        )
    }

    composable(Route.Topics.route) {

        val appCtx = LocalContext.current
        val userSp = remember(appCtx) {
            appCtx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
        }

        val subsSp = remember(appCtx) {
            appCtx.getSharedPreferences("kmi_subs", Context.MODE_PRIVATE)
        }

        val legacySp = remember(appCtx) {
            appCtx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE)
        }

        fun hasPremiumAccess(): Boolean {
            return KmiAccess.hasFullAccess(userSp) ||
                    KmiAccess.hasFullAccess(subsSp) ||
                    KmiAccess.hasFullAccess(legacySp)
        }

        fun shouldBlockPremiumTopic(raw: String): Boolean {
            return isLockedPremiumTopic(raw) && !hasPremiumAccess()
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

        fun openSubTopics(belt: Belt, topic: String) {
            vm.setSelectedBelt(belt)

            val route = il.kmi.app.screens.SubTopics.SubTopicsByTopicRoute.build(
                belt = belt,
                topic = topic
            )

            nav.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        }

        fun openChosenSubTopic(
            belt: Belt,
            topic: String,
            subTopic: String
        ) {
            vm.setSelectedBelt(belt)

            val route = Route.MaterialsSub.make(
                belt = belt,
                topic = topic,
                subTopic = subTopic
            )

            if (route != null) {
                nav.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        BeltQuestionsByTopicScreen(
            onOpenSubscription = {
                nav.navigate(Route.Subscription.route) {
                    launchSingleTop = true
                    restoreState = true
                }
            },

            onOpenHardSubjectRoute = { belt, subjectId ->
                vm.setSelectedBelt(belt)

                val cleanSubjectId = subjectId.trim()

                when (cleanSubjectId) {
                    "def_internal",
                    "def_external",
                    "kicks",
                    "kicks_hard",
                    "knife_defense",
                    "knife_rifle_defense",
                    "gun_threat_defense",
                    "stick_defense",
                    "multiple_attackers_defense",
                    "releases",
                    "releases_hugs",
                    "hands_all",
                    "hands_strikes",
                    "hands_elbows",
                    "hands_stick_rifle",
                    "topic_kavaler",
                    "topic_general",
                    "topic_kicks",
                    "topic_breakfalls_rolls",
                    "topic_ready_stance",
                    "topic_ground_prep" -> {
                        nav.navigate("hard_subject/${Uri.encode(cleanSubjectId)}") {
                            launchSingleTop = true
                            restoreState = false
                        }
                    }

                    else -> {
                        openSubTopics(
                            belt = belt,
                            topic = cleanSubjectId
                        )
                    }
                }
            },

            onOpenSubject = { belt: Belt, subject ->
                if (shouldBlockPremiumTopic(subject.titleHeb) || shouldBlockPremiumTopic(subject.id)) {
                    nav.navigate(Route.Subscription.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    openSubTopics(
                        belt = belt,
                        topic = subject.titleHeb
                    )
                }
            },

            onOpenTopic = { belt: Belt, topicTitle: String ->
                if (shouldBlockPremiumTopic(topicTitle)) {
                    nav.navigate(Route.Subscription.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                } else {
                    openSubTopics(
                        belt = belt,
                        topic = topicTitle
                    )
                }
            },

            onOpenTopicWithSub = { belt: Belt, topicTitle: String, subTopicTitle: String ->
                // במסך "לפי נושא" לא חוסמים שוב תת־נושא שכבר נבחר מתוך דיאלוג פתוח.
                openChosenSubTopic(
                    belt = belt,
                    topic = topicTitle,
                    subTopic = subTopicTitle
                )
            },
            onOpenDefenseList = { belt, kind, pick ->
                // ✅ המסלול הזה מגיע רק אחרי שהמשתמש עבר את שער הגישה במסך הנושאים.
                // לא חוסמים כאן שוב, כדי לא להפיל מנוי פעיל בגלל SharedPreferences לא מסונכרנים.
                openChosenSubTopic(
                    belt = belt,
                    topic = kind,
                    subTopic = pick
                )
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
                nav.navigate(Route.VoiceAssistant.route) {
                    launchSingleTop = true
                }
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
}