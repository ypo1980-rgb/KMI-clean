package il.kmi.app.analytics

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object KmiUsageTracker {

    suspend fun markAppOpen() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val now = System.currentTimeMillis()

        val data = hashMapOf(
            "uid" to uid,
            "email" to user.email.orEmpty(),
            "displayName" to user.displayName.orEmpty(),
            "appOpenCount" to FieldValue.increment(1),
            "lastSeenAtMillis" to now,
            "lastSeenAt" to FieldValue.serverTimestamp(),
            "updatedAtMillis" to now,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        Firebase.firestore
            .collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
            .await()
    }
}