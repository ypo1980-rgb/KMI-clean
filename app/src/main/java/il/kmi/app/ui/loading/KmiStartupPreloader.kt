package il.kmi.app.ui.loading

import android.app.Application
import android.content.Context
import android.util.Log
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

    private const val TAG_PRELOAD = "KMI_PRELOAD"

    private fun authStateForLog(): String {
        val user = FirebaseAuth.getInstance().currentUser

        return if (user == null) {
            "uid=null, email=null, isAnonymous=null, providers=[]"
        } else {
            val providers = user.providerData
                .map { it.providerId }
                .joinToString("|")

            "uid=${user.uid}, email=${user.email.orEmpty()}, isAnonymous=${user.isAnonymous}, providers=[$providers]"
        }
    }

    suspend fun preload(context: Context) {
        val appContext = context.applicationContext
        val app = appContext as? Application ?: return

        val settingsSp = appContext.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
        val userSp = appContext.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

        val isEnglish =
            AppLanguageManager(appContext).getCurrentLanguage() == AppLanguage.ENGLISH

        Log.d(
            TAG_PRELOAD,
            "stage=preload_start, ${authStateForLog()}"
        )

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
                    val authUser = FirebaseAuth.getInstance().currentUser

                    Log.d(
                        TAG_PRELOAD,
                        "stage=admin_users_preload_check, hasUser=${authUser != null}, isAnonymous=${authUser?.isAnonymous}, ${authStateForLog()}"
                    )

                    if (authUser == null || authUser.isAnonymous) {
                        Log.d(
                            TAG_PRELOAD,
                            "stage=admin_users_preload_skipped, reason=${if (authUser == null) "no_user" else "anonymous_user"}"
                        )
                        return@runCatching
                    }

                    Log.d(TAG_PRELOAD, "stage=admin_users_preload_start")

                    withTimeoutOrNull(8_000L) {
                        AdminUsersPreloadCache.preload(isEnglish)
                    }

                    Log.d(TAG_PRELOAD, "stage=admin_users_preload_finished")
                }.onFailure { error ->
                    Log.e(
                        TAG_PRELOAD,
                        "stage=admin_users_preload_failure, errorClass=${error.javaClass.name}, errorMessage=${error.message.orEmpty()}, ${authStateForLog()}",
                        error
                    )
                }
            }

            val firestoreWarmupJob = async {
                runCatching {
                    val authUser = FirebaseAuth.getInstance().currentUser

                    Log.d(
                        TAG_PRELOAD,
                        "stage=firestore_warmup_check, hasUser=${authUser != null}, isAnonymous=${authUser?.isAnonymous}, ${authStateForLog()}"
                    )

                    // ✅ לא מחממים Firestore לפני התחברות אמיתית.
                    // משתמש אנונימי / אין משתמש = לא עושים users.get / assistantFeedback.
                    if (authUser == null || authUser.isAnonymous) {
                        Log.d(
                            TAG_PRELOAD,
                            "stage=firestore_warmup_skipped, reason=${if (authUser == null) "no_user" else "anonymous_user"}"
                        )
                        return@runCatching
                    }

                    withTimeoutOrNull(8_000L) {
                        Log.d(TAG_PRELOAD, "stage=firestore_warmup_users_start")

                        Firebase.firestore
                            .collection("users")
                            .get()
                            .await()

                        Log.d(TAG_PRELOAD, "stage=firestore_warmup_users_success")

                        Log.d(TAG_PRELOAD, "stage=firestore_warmup_assistant_feedback_start")

                        Firebase.firestore
                            .collection("assistantFeedback")
                            .whereEqualTo("liked", false)
                            .limit(50)
                            .get()
                            .await()

                        Log.d(TAG_PRELOAD, "stage=firestore_warmup_assistant_feedback_success")
                    }

                    Log.d(TAG_PRELOAD, "stage=firestore_warmup_finished")
                }.onFailure { error ->
                    Log.e(
                        TAG_PRELOAD,
                        "stage=firestore_warmup_failure, errorClass=${error.javaClass.name}, errorMessage=${error.message.orEmpty()}, ${authStateForLog()}",
                        error
                    )
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

            Log.d(
                TAG_PRELOAD,
                "stage=preload_finished, ${authStateForLog()}"
            )
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