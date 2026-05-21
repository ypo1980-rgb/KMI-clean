package il.kmi.app

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenManager {

    fun refreshTokenForCurrentUser(context: Context) {
        refreshTokenForCurrentUser()
    }

    fun refreshTokenForCurrentUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            return
        }

        refreshTokenForUserDocId(uid)
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
        val cleanUserDocId = userDocId.trim()

        if (cleanUserDocId.isBlank()) {
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    return@addOnSuccessListener
                }

                saveTokenToFirestore(cleanUserDocId, token)
            }
            .addOnFailureListener {
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
            }
            .addOnFailureListener {
            }
    }
}