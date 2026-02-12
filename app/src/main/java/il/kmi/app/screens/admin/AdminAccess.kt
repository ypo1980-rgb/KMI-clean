package il.kmi.app.screens.admin

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AdminAccess {

    suspend fun isCurrentUserAdmin(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.d("KMI_ADMIN", "AdminAccess: no uid (not signed in)")
            return false
        }

        return try {
            Log.d("KMI_ADMIN", "AdminAccess: checking admins/$uid ...")

            val snap = FirebaseFirestore.getInstance()
                .collection("admins")
                .document(uid)
                .get()
                .await()

            val enabled = (snap.getBoolean("enabled") == true)
            Log.d(
                "KMI_ADMIN",
                "AdminAccess: exists=${snap.exists()} enabled=$enabled data=${snap.data}"
            )

            snap.exists() && enabled
        } catch (t: Throwable) {
            Log.e("KMI_ADMIN", "AdminAccess: FAILED to read admins/$uid", t)
            false
        }
    }
}
