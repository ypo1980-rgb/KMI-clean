@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.navigation

import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.attendance.ui.AttendanceScreen
import il.kmi.app.attendance.ui.AttendanceViewModel
import il.kmi.shared.domain.Belt
import il.kmi.app.screens.*
import il.kmi.app.screens.registration.RegistrationNavHost
import il.kmi.app.ui.DrawerBridge
import il.kmi.shared.prefs.KmiPrefs
import java.net.URLDecoder
import java.time.LocalDate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope

/**
 * גרף Legacy מרוכז: משתמש בכל הגרפים החדשים (home/topics/materials/…),
 * ומוסיף יעדים בודדים שלא קיימים בגרפים המודולרים (DeepLinks, Attendance וכו').
 *
 * שים לב: זהו NavGraphBuilder-Ext – מחובר מ-MainApp במצב useNewNav=false.
 */
fun NavGraphBuilder.legacyNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
) {
    // ---- Intro → RegistrationLanding ----
    composable(Route.Intro.route) {
        IntroScreen(
            onContinue = {
                nav.navigate(Route.RegistrationLanding.route) {
                    popUpTo(Route.Intro.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    // ---- Registration landing (הוסט פנימי) ----
    composable(Route.RegistrationLanding.route) {
        val regNav = androidx.navigation.compose.rememberNavController()
        val scope = rememberCoroutineScope()

        RegistrationNavHost(
            nav = regNav,
            vm = vm,
            sp = sp,
            kmiPrefs = kmiPrefs,
            onOpenDrawer = { DrawerBridge.open() },
            onOpenLegal = { nav.navigate(Route.Legal.route) },
            onOpenTerms = { nav.navigate(Route.Legal.route) },
            onRegistrationDone = {
                nav.navigate(Route.Home.route) {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
        )
    }

    // -------- גרפים מודולריים (שכבר בנית) --------
    // מנוע אימונים (BeltQ וכו')
    trainingNavGraph(nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)

    // בית / נושאים / חומר לימוד / סיכום / אימון / מבחן / התקדמות / הגדרות / חוקי / אודות / רישום / מאמן / מנוי
    homeNavGraph(        nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    topicsNavGraph(      nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    materialsNavGraph(   nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    summaryNavGraph(     nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    practiceNavGraph(    nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    examNavGraph(        nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    progressNavGraph(    nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    settingsNavGraph(
        nav = nav,
        sp = sp,
        kmiPrefs = kmiPrefs,
        // ערך התחלתי מתוך KmiPrefs (או "system" אם ריק),
        // ו־onThemeChange ריק כי ב־legacy הניווט לא משתמש במצב החדש של MainNavHost.
        themeMode = kmiPrefs.themeMode.ifBlank { "system" },
        onThemeChange = { /* legacy path – שינוי נושא מטופל ברמה אחרת */ }
    )
    legalNavGraph(       nav = nav)
    aboutNavGraph(       nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    registrationNavGraph(nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    coachNavGraph(       nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)
    subscriptionNavGraph(nav = nav, vm = vm, sp = sp, kmiPrefs = kmiPrefs)

    // -------- יעדים משלימים (שאינם כלולים בגרפים למעלה) --------

    // 1) מסך נוכחות למאמן (Attendance)
    composable(route = "attendance") {
        val userBranch   = kmiPrefs.branch.orEmpty()
        val userGroupKey = il.kmi.app.training.TrainingCatalog
            .normalizeGroupName(name = kmiPrefs.ageGroup.orEmpty())
        val today        = LocalDate.now()

        val attendVm: AttendanceViewModel = viewModel()

        AttendanceScreen(
            vm = attendVm,
            branch = userBranch,
            date = today,
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
            }
        )
    }

    // 2) DeepLink: פתיחת Materials לפי מזהה תרגיל שנבחר בחיפוש
    //    נתיב: Exercise/{id}  כאשר id = "beltId|topic|item" (או "::" / "/")
    composable(
        route = "${Route.Exercise.route}/{id}",
        arguments = listOf(navArgument("id") { type = NavType.StringType })
    ) { backStackEntry ->
        fun dec(s: String?) = try { URLDecoder.decode(s ?: "", "UTF-8") } catch (_: Exception) { s.orEmpty() }
        val raw = backStackEntry.arguments?.getString("id").orEmpty()
        val parts = when {
            raw.contains('|')  -> raw.split('|',  limit = 3)
            raw.contains("::") -> raw.split("::", limit = 3)
            else               -> raw.split('/',  limit = 3)
        }.map(::dec)

        val beltId = parts.getOrNull(0).orEmpty()
        val topic  = parts.getOrNull(1).orEmpty()
        val item   = parts.getOrNull(2).orEmpty()
        val belt   = Belt.fromId(beltId) ?: Belt.WHITE

        // הדגשת פריט ב-Materials דרך ה-ViewModel (כמו בקוד הישן)
        LaunchedEffect(item) {
            runCatching {
                val f = vm::class.java.getDeclaredField("highlightItem").apply { isAccessible = true }
                val flow = f.get(vm) as? kotlinx.coroutines.flow.MutableStateFlow<String?>
                flow?.value = item
            }.onFailure {
                runCatching {
                    val m = vm::class.java.methods.firstOrNull { it.name == "setHighlightItem" && it.parameterTypes.size == 1 }
                    m?.isAccessible = true
                    m?.invoke(vm, item)
                }
            }
        }

        MaterialsScreen(
            vm = vm,
            belt = belt,
            topic = topic,
            onBack = { nav.popBackStack() },

            // ✅ FIX: חתימה חדשה + מעבירים גם topic + subTopic
            onSummary = { b, t, sub ->
                nav.navigate(Route.Summary.make(belt = b, topic = t, subTopic = sub))
            },

            onPractice = { b, t -> nav.navigate(Route.Practice.make(b, t)) },
            onOpenSettings = { nav.navigate(Route.Settings.route) },
            onOpenHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            subTopicFilter = null
        )
    }

    composable(Route.MyProfile.route) {
        MyProfileScreen(
            sp = sp,
            kmiPrefs = kmiPrefs,
            onClose = { nav.popBackStack() }
        )
    }

    // 3) מסך "כל הרשימות" (משתמש באותו ExercisesTabsScreen עם topic="__ALL__")
    composable(
        route = "ex_tabs_all/{beltId}",
        arguments = listOf(navArgument("beltId"){ type = NavType.StringType })
    ) { backStackEntry ->
        val beltId = backStackEntry.arguments?.getString("beltId").orEmpty()
        val belt   = Belt.fromId(beltId) ?: Belt.WHITE
        ExercisesTabsScreen(
            vm = vm,
            belt = belt,
            topic = "__ALL__",
            onPractice = { b, t -> nav.navigate(Route.Practice.make(b, t)) },
            subTopicFilter = null,
            onHome = {
                nav.navigate(Route.Home.route) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onSearch = { nav.navigate(Route.Topics.route) { launchSingleTop = true } }
        )
    }

    // 4) Rate-Us – פתיחה ל-Play Store ואז חזרה
    composable(Route.RateUs.route) {
        val ctx = LocalContext.current
        LaunchedEffect(Unit) {
            val pkg = ctx.packageName
            val market = Uri.parse("market://details?id=$pkg")
            val web    = Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
            val i = android.content.Intent(android.content.Intent.ACTION_VIEW, market).apply {
                addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NO_HISTORY or
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
            }
            runCatching { ctx.startActivity(i) }
                .onFailure { runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, web)) } }
            nav.popBackStack()
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    composable(route = Route.MonthlyCalendar.route) {
        il.kmi.app.screens.MonthlyCalendarScreen(
            kmiPrefs = kmiPrefs,
            onBack   = { nav.popBackStack() }
        )
    }

    // 5) מיפוי ישן → חדש: Route.Registration ⇒ מסלול הרישום החדש
    composable(Route.Registration.route) {
        LaunchedEffect(Unit) {
            nav.navigate(Route.NewUserTrainee.route) {
                popUpTo(Route.Registration.route) { inclusive = true }
                launchSingleTop = true
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
