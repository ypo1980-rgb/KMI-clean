package il.kmi.app.screens.admin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AdminAccess {

    suspend fun isCurrentUserAdmin(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            return false
        }

        return try {
            val snap = FirebaseFirestore.getInstance()
                .collection("admins")
                .document(uid)
                .get()
                .await()

            val enabled = (snap.getBoolean("enabled") == true)

            snap.exists() && enabled
        } catch (_: Throwable) {
            false
        }
    }
}
