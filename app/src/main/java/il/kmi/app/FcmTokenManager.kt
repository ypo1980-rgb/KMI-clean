package il.kmi.app

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    fun refreshTokenForCurrentUser(context: Context) {
        refreshTokenForCurrentUser()
    }

    fun refreshTokenForCurrentUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            Log.w(TAG, "refreshTokenForCurrentUser: no currentUser, skipping")
            return
        }

        refreshTokenForUserDocId(uid)
    }

    fun saveProvidedTokenForCurrentUser(token: String) {
        val cleanToken = token.trim()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            Log.w(TAG, "saveProvidedTokenForCurrentUser: no currentUser, skipping")
            return
        }

        if (cleanToken.isBlank()) {
            Log.w(TAG, "saveProvidedTokenForCurrentUser: blank token, skipping")
            return
        }

        Log.e(
            TAG,
            "saveProvidedTokenForCurrentUser: saving provided token for uid=$uid tokenPrefix=${cleanToken.take(18)}..."
        )

        saveTokenToFirestore(uid, cleanToken)
    }

    fun refreshTokenForUserDocId(userDocId: String) {
        val cleanUserDocId = userDocId.trim()

        if (cleanUserDocId.isBlank()) {
            Log.w(TAG, "refreshTokenForUserDocId: blank userDocId, skipping")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "refreshTokenForUserDocId: got blank token, skipping")
                    return@addOnSuccessListener
                }

                Log.e(
                    TAG,
                    "refreshTokenForUserDocId: saving token for userDocId=$cleanUserDocId tokenPrefix=${token.take(18)}..."
                )

                saveTokenToFirestore(cleanUserDocId, token)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "refreshTokenForUserDocId: failed to get FCM token", e)
            }
    }

    private fun saveTokenToFirestore(userDocId: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(userDocId)

        val now = Timestamp.now()

        val data = mapOf(
            "uid" to userDocId,

            // תאימות אחורה / שליחה מהירה לטוקן האחרון
            "fcmToken" to token,
            "fcmTokenUpdatedAt" to now,

            // מבנה מרובה טוקנים עבור Cloud Functions
            "fcmTokens.$token" to mapOf(
                "token" to token,
                "platform" to "android",
                "updatedAt" to now
            )
        )

        userDoc.set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.e(
                    TAG,
                    "saveTokenToFirestore: fcmToken + fcmTokens saved for userDocId=$userDocId tokenPrefix=${token.take(18)}..."
                )
            }
            .addOnFailureListener { e ->
                Log.e(
                    TAG,
                    "saveTokenToFirestore: failed to save fcmToken/fcmTokens for userDocId=$userDocId",
                    e
                )
            }
    }
}