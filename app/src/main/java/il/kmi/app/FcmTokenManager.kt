package il.kmi.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    /**
     * קריאה ידידותית לשירותים שצריכים Context (למשל FirebaseMessagingService),
     * בפועל מעבירה לקריאה הרגילה ללא context.
     */
    fun refreshTokenForCurrentUser(context: Context) {
        refreshTokenForCurrentUser()
    }

    /**
     * קורא את ה־FCM token של המכשיר ושומר אותו במסמך המשתמש ב־Firestore.
     * יש לקרוא לפונקציה הזו רק אחרי שיש currentUser (אחרי התחברות / רישום).
     */
    fun refreshTokenForCurrentUser() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w(TAG, "refreshTokenForCurrentUser: no currentUser, skipping")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "refreshTokenForCurrentUser: got blank token, skipping")
                    return@addOnSuccessListener
                }
                Log.d(TAG, "refreshTokenForCurrentUser: FCM token = $token")
                saveTokenToFirestore(user.uid, token)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "refreshTokenForCurrentUser: failed to get FCM token", e)
            }
    }

    /**
     * שומר את ה־token בשדה fcmToken ובשדה uid במסמך users/{uid}.
     * קודם מנסה update, ואם המסמך לא קיים – עושה set(merge).
     */
    private fun saveTokenToFirestore(uid: String, token: String) {
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(uid)

        val data = mapOf(
            "uid" to uid,
            "fcmToken" to token
        )

        userDoc.update(data)
            .addOnSuccessListener {
                Log.d(TAG, "saveTokenToFirestore: fcmToken + uid updated for user $uid")
            }
            .addOnFailureListener { firstError ->
                Log.w(
                    TAG,
                    "saveTokenToFirestore: update failed (maybe doc missing), trying set(merge)",
                    firstError
                )

                userDoc.set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "saveTokenToFirestore: fcmToken + uid set(merge) for user $uid")
                    }
                    .addOnFailureListener { e2 ->
                        Log.e(
                            TAG,
                            "saveTokenToFirestore: failed to save fcmToken for user $uid",
                            e2
                        )
                    }
            }
    }
}
