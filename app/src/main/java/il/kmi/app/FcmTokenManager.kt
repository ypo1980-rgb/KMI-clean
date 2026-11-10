package il.kmi.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    private val tokenMutex = Mutex()

    private val tokenScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    @Volatile
    private var signingOutUid: String? = null

    fun refreshTokenForCurrentUser(context: Context) {
        refreshTokenForCurrentUser()
    }

    fun refreshTokenForCurrentUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            signingOutUid = null
            return
        }

        if (signingOutUid == uid) {
            return
        }

        refreshTokenForUserDocId(uid)
    }

    /**
     * נקרא רק לאחר התחברות מאושרת
     * ולאחר בדיקת שיוך המכשיר.
     */
    fun refreshTokenAfterAuthorizedLogin() {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return

        if (user.isAnonymous || user.uid.isBlank()) {
            return
        }

        // מאפסים חסימת התנתקות קודמת
        // רק במסלול ההתחברות המאושרת.
        signingOutUid = null

        refreshTokenForUserDocId(user.uid)
    }

    fun saveProvidedTokenForCurrentUser(token: String) {
        val cleanToken = token.trim()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            return
        }

        if (cleanToken.isBlank()) {
            return
        }

        saveTokenToFirestore(uid, cleanToken)
    }

    fun refreshTokenForUserDocId(userDocId: String) {
        val firebaseUser =
            FirebaseAuth.getInstance().currentUser
                ?: return

        if (firebaseUser.isAnonymous) {
            return
        }

        val authUid =
            firebaseUser.uid.trim()

        val cleanUserDocId =
            userDocId.trim()

        if (
            authUid.isBlank() ||
            cleanUserDocId.isBlank() ||
            cleanUserDocId != authUid
        ) {
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                val cleanToken =
                    token.trim()

                if (cleanToken.isBlank()) {
                    return@addOnSuccessListener
                }

                val currentUser =
                    FirebaseAuth.getInstance().currentUser

                if (
                    currentUser == null ||
                    currentUser.isAnonymous ||
                    currentUser.uid != authUid
                ) {
                    return@addOnSuccessListener
                }

                saveTokenToFirestore(
                    userDocId = authUid,
                    token = cleanToken
                )
            }
            .addOnFailureListener {
                // FCM token refresh is non-blocking.
            }
    }

    suspend fun removeCurrentDeviceTokenBeforeSignOut(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return true

        if (user.isAnonymous) {
            return true
        }

        val uid = user.uid.trim()

        if (uid.isBlank()) {
            return false
        }

        signingOutUid = uid

        val success = try {
            val token = FirebaseMessaging
                .getInstance()
                .token
                .await()
                .trim()

            val deviceId =
                FirebaseApp.getInstance()
                    .applicationContext
                    .getSharedPreferences(
                        "kmi_device_identity",
                        Context.MODE_PRIVATE
                    )
                    .getString("device_id", null)
                    .orEmpty()
                    .trim()

            if (token.isBlank() || deviceId.isBlank()) {
                false
            } else {
                tokenMutex.withLock {
                    if (auth.currentUser?.uid != uid) {
                        false
                    } else {
                        val result =
                            FirebaseFunctions
                                .getInstance("europe-west1")
                                .getHttpsCallable(
                                    "unregisterKmiFcmToken"
                                )
                                .call(
                                    mapOf(
                                        "deviceId" to deviceId,
                                        "token" to token
                                    )
                                )
                                .await()

                        (result.data as? Map<*, *>)
                            ?.get("accepted") == true
                    }
                }
            }

        } catch (e: kotlinx.coroutines.CancellationException) {
            // אין לאפשר רישום מחדש בזמן התנתקות.
            throw e

        } catch (e: Exception) {
            android.util.Log.w(
                "KMI_FCM",
                "FCM token removal failed",
                e
            )
            false
        }

        /*
         * גם אם ניקוי הטוקן נכשל,
         * אין לפתוח מחדש רישום FCM
         * במהלך תהליך ההתנתקות.
         *
         * החסימה מתאפסת רק לאחר
         * התחברות חדשה שאושרה.
         */
        return success
    }

    private fun saveTokenToFirestore(userDocId: String, token: String) {
        tokenScope.launch {
            tokenMutex.withLock {
                val currentUser =
                    FirebaseAuth.getInstance().currentUser

                if (
                    currentUser == null ||
                    currentUser.isAnonymous ||
                    currentUser.uid != userDocId ||
                    signingOutUid == userDocId
                ) {
                    return@withLock
                }

                // משתמשים במזהה המכשיר שכבר קיים בפרויקט.
                val deviceId =
                    FirebaseApp.getInstance()
                        .applicationContext
                        .getSharedPreferences(
                            "kmi_device_identity",
                            Context.MODE_PRIVATE
                        )
                        .getString("device_id", null)
                        .orEmpty()
                        .trim()

                // אין ליצור מזהה חדש מתוך מנהל ה-FCM.
                if (deviceId.isBlank()) {
                    return@withLock
                }

                try {
                    val result =
                        FirebaseFunctions
                            .getInstance("europe-west1")
                            .getHttpsCallable("registerKmiFcmToken")
                            .call(
                                mapOf(
                                    "deviceId" to deviceId,
                                    "token" to token,
                                    "platform" to "android"
                                )
                            )
                            .await()

                    val accepted =
                        (result.data as? Map<*, *>)
                            ?.get("accepted") == true

                    if (!accepted) {
                        android.util.Log.w(
                            "KMI_FCM",
                            "Server did not confirm FCM registration"
                        )
                    }

                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e

                } catch (e: Exception) {
                    android.util.Log.w(
                        "KMI_FCM",
                        "FCM registration failed",
                        e
                    )
                }
            }
        }
    }
}