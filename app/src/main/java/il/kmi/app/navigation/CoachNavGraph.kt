package il.kmi.app.navigation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.attendance.ui.AttendanceScreen
import il.kmi.app.attendance.ui.AttendanceViewModel
import il.kmi.app.training.TrainingCatalog
import java.time.LocalDate

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.coachNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // --- שידור מאמן ---
    composable(Route.CoachBroadcast.route) {
        val regionDefault = kmiPrefs.region
        val branchDefault = kmiPrefs.branch

        val ctx = LocalContext.current

        // אפשר להביא את שם המאמן מה-SharedPreferences של המשתמש
        val userSp = remember {
            ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
        }
        val coachName = userSp.getString("fullName", "") ?: ""

        il.kmi.app.screens.coach.CoachBroadcastScreen(
            branchesByRegion = TrainingCatalog.branchesByRegion,
            defaultRegion = regionDefault,
            defaultBranch = branchDefault,
            onBack = { nav.popBackStack() },

            // פתיחת אפליקציית SMS עם כל המספרים המסומנים
            onOpenSms = { numbers, message ->
                if (numbers.isEmpty()) return@CoachBroadcastScreen

                // "smsto:" + מספרים מופרדים ב-';'
                val uri = "smsto:" + numbers.joinToString(";")
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse(uri)
                    putExtra("sms_body", message)
                }

                // ננסה לפתוח Activity שתומכת ב-SMS
                runCatching {
                    ctx.startActivity(intent)
                }
            },

            // שיתוף טקסט כללי (וואטסאפ / מייל / טלגרם וכו')
            onShareText = { message ->
                if (message.isBlank()) return@CoachBroadcastScreen

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }

                val chooser = Intent.createChooser(shareIntent, "שתף הודעת מאמן")
                runCatching {
                    ctx.startActivity(chooser)
                }
            },

            // שמירת השידור ב-Firestore כדי ש-Cloud Function תוכל לשלוח FCM
// כולל רשימת ה-UIDs של הנמענים המסומנים
            onPersistBroadcast = { region, branch, message, targetUids ->
                val auth = FirebaseAuth.getInstance()
                val uid = auth.currentUser?.uid

                if (uid == null) {
                    android.util.Log.e(
                        "CoachBroadcast",
                        "No logged-in user, aborting broadcast"
                    )
                } else {
                    // ⭐ groupKey לפי קבוצת הגיל / קבוצה של המשתמש (כמו בפורום)
                    val groupKey = TrainingCatalog.normalizeGroupName(
                        name = kmiPrefs.ageGroup.orEmpty()
                    )

                    // נוסיף גם את המאמן עצמו לרשימת היעד, בלי כפילויות
                    val allTargets = (targetUids + uid).distinct()

                    val db = Firebase.firestore
                    val data = hashMapOf(
                        "region" to region,
                        "branch" to branch,
                        "groupKey" to groupKey,          // ← למסך הבית / פונקציות
                        // שדות זהות למשתמש המאמן
                        "authorUid" to uid,              // 👈 חשוב ל-rules
                        "coachUid" to uid,
                        "coachName" to coachName,
                        // תוכן ההודעה
                        "message" to message,            // 👈 שם כללי ופשוט
                        "text" to message,               // אם ה-Cloud Function משתמש בזה
                        // נמענים (כולל המאמן)
                        "targetUids" to allTargets,
                        // חותמת זמן
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("coachBroadcasts")
                        .add(data)
                        .addOnSuccessListener {
                            android.util.Log.d(
                                "CoachBroadcast",
                                "broadcast saved for branch=$branch, targets=${allTargets.size}"
                            )
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CoachBroadcast", "failed to save broadcast", e)
                        }
                }
            }
        )
    }

    // --- נוכחות מאמן (שמרנו על ה-route הישן "attendance") ---
    composable(route = "attendance") {
        val userBranch   = kmiPrefs.branch.orEmpty()
        val userGroupKey = TrainingCatalog.normalizeGroupName(name = kmiPrefs.ageGroup.orEmpty())
        val today        = LocalDate.now()

        val attendVm: AttendanceViewModel = viewModel(
            key = "attendance_${userBranch}_${userGroupKey}"
        )

        AttendanceScreen(
            vm = attendVm,
            date = today,
            branch = userBranch,
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
            },

            // ✅ חדש: סטטיסטיקת קבוצה (שנה אחורה)
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
}
