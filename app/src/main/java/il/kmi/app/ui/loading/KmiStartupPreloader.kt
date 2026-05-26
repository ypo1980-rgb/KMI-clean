package il.kmi.app.ui.loading

import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.screens.admin.AdminUsersPreloadCache
import il.kmi.app.subscription.KmiAccess
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

object KmiStartupPreloader {

    suspend fun preload(context: Context) {
        val appContext = context.applicationContext
        val app = appContext as? Application ?: return

        val settingsSp = appContext.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
        val userSp = appContext.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

        val isEnglish =
            AppLanguageManager(appContext).getCurrentLanguage() == AppLanguage.ENGLISH

        coroutineScope {
            val accessJob = async {
                runCatching {
                    KmiAccess.hasFullAccess(settingsSp)
                    KmiAccess.isAdmin(settingsSp)
                    KmiAccess.hasValidTimedSubscription(settingsSp)
                }
            }

            val authJob = async {
                runCatching {
                    FirebaseAuth.getInstance().currentUser
                }
            }

            val adminUsersJob = async {
                runCatching {
                    withTimeoutOrNull(8_000L) {
                        AdminUsersPreloadCache.preload(isEnglish)
                    }
                }
            }

            val firestoreWarmupJob = async {
                runCatching {
                    withTimeoutOrNull(8_000L) {
                        Firebase.firestore
                            .collection("users")
                            .get()
                            .await()

                        Firebase.firestore
                            .collection("assistantFeedback")
                            .whereEqualTo("liked", false)
                            .limit(50)
                            .get()
                            .await()
                    }
                }
            }

            val attendanceWarmupJob = async {
                runCatching {
                    val branch = firstNonBlank(
                        userSp.getString("branch", null),
                        userSp.getString("branchName", null),
                        userSp.getString("selectedBranch", null),
                        userSp.getString("selectedBranchName", null),
                        userSp.getString("trainingBranch", null),
                        userSp.getString("trainingBranchName", null)
                    )

                    val groupKey = firstNonBlank(
                        userSp.getString("groupKey", null),
                        userSp.getString("group_key", null),
                        userSp.getString("group", null),
                        userSp.getString("selectedGroup", null),
                        userSp.getString("selectedGroupName", null),
                        userSp.getString("trainingGroup", null),
                        userSp.getString("trainingGroupName", null)
                    )

                    if (branch.isNotBlank() && groupKey.isNotBlank()) {
                        withTimeoutOrNull(6_000L) {
                            AttendanceRepository
                                .get(app)
                                .members(branch, groupKey)
                                .first()
                        }
                    }
                }
            }

            accessJob.await()
            authJob.await()
            adminUsersJob.await()
            firestoreWarmupJob.await()
            attendanceWarmupJob.await()
        }
    }

    private fun firstNonBlank(vararg values: String?): String {
        return values
            .asSequence()
            .map { it.orEmpty().trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
    }
}