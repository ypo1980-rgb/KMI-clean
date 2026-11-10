package il.kmi.app.navigation

import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import il.kmi.app.KmiViewModel
import il.kmi.app.Route
import il.kmi.app.training.TrainingCatalog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

//========================================================================

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.coachNavGraph(
    nav: NavHostController,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: il.kmi.shared.prefs.KmiPrefs
) {
    // --- שידור מאמן ---
    composable(Route.CoachBroadcast.route) {
        var broadcastAuthorized by remember {
            mutableStateOf<Boolean?>(null)
        }

        LaunchedEffect(Unit) {
            val uid =
                FirebaseAuth.getInstance()
                    .currentUser
                    ?.uid
                    .orEmpty()

            if (uid.isBlank()) {
                broadcastAuthorized = false
                return@LaunchedEffect
            }

            val coachDoc =
                runCatching {
                    FirebaseFirestore.getInstance()
                        .collection("authorizedCoaches")
                        .document(uid)
                        .get()
                        .await()
                }.getOrNull()

            broadcastAuthorized =
                coachDoc?.exists() == true &&
                        coachDoc.getBoolean("active") == true &&
                        coachDoc.getString("role")
                            .orEmpty()
                            .equals(
                                "coach",
                                ignoreCase = true
                            ) &&
                        coachDoc.getBoolean(
                            "canSendBroadcasts"
                        ) == true
        }

        when (broadcastAuthorized) {
            null -> {
                CircularProgressIndicator()
            }

            false -> {
                LaunchedEffect(Unit) {
                    nav.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            }

            true -> {
                val regionDefault = kmiPrefs.region
                val branchDefault = kmiPrefs.branch

                val ctx = LocalContext.current

                il.kmi.app.screens.coach.CoachBroadcastScreen(
                    branchesByRegion = TrainingCatalog.branchesByRegion,
                    defaultRegion = regionDefault,
                    defaultBranch = branchDefault,
                    onBack = {
                        nav.popBackStack()
                    },
                    onHome = {
                        nav.navigate(Route.Home.route) {
                            launchSingleTop = true
                            restoreState = false

                            popUpTo(Route.CoachBroadcast.route) {
                                inclusive = true
                            }
                        }
                    },

                    // הודעות מאמן נשמרות ב-Firestore ונשלחות באמצעות Push.
                    // אין פתיחה של אפליקציית SMS.

                    // שיתוף טקסט כללי: וואטסאפ / מייל / טלגרם וכו'
                    onShareText = { message ->
                        if (message.isBlank()) {
                            return@CoachBroadcastScreen
                        }

                        val shareIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    message
                                )
                            }

                        val chooser =
                            Intent.createChooser(
                                shareIntent,
                                "שתף הודעת מאמן"
                            )

                        runCatching {
                            ctx.startActivity(chooser)
                        }
                    }
                )
            }
        }
    }
}
