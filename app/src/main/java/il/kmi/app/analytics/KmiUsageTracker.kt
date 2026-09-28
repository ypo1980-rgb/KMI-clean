package il.kmi.app.analytics

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object KmiUsageTracker {

    suspend fun markAppOpen() {
        val user =
            FirebaseAuth.getInstance()
                .currentUser
                ?: return

        if (user.isAnonymous) {
            return
        }

        val uid =
            user.uid
                .trim()

        if (uid.isBlank()) {
            return
        }

        val now =
            System.currentTimeMillis()

        val data =
            hashMapOf<String, Any>(
                "uid" to uid,

                "appOpenCount" to
                        FieldValue.increment(1L),

                "lastSeenAtMillis" to
                        now,

                "lastSeenAt" to
                        FieldValue.serverTimestamp()
            )

        Firebase.firestore
            .collection("users")
            .document(uid)
            .set(
                data,
                SetOptions.merge()
            )
            .await()
    }
}