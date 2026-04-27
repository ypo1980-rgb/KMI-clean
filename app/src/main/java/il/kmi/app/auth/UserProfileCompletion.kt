package il.kmi.app.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserProfileCompletion {

    private const val TAG = "KMI_PROFILE_CHECK"

    data class ProfileStatus(
        val isComplete: Boolean,
        val missingFields: List<String>
    )

    suspend fun checkAndPersistProfileStatus(context: Context): ProfileStatus {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return ProfileStatus(
                isComplete = false,
                missingFields = listOf("uid")
            )

        val sp = context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
        val db = FirebaseFirestore.getInstance()

        val doc = try {
            db.collection("users")
                .document(user.uid)
                .get()
                .await()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to read user profile from Firestore", t)
            null
        }

        if (doc == null || !doc.exists()) {
            persistAuthOnlyUser(sp, user.uid, user.email, user.displayName, user.photoUrl?.toString())

            return ProfileStatus(
                isComplete = false,
                missingFields = listOf(
                    "belt",
                    "region",
                    "branch",
                    "groups",
                    "role",
                    "phone",
                    "gender",
                    "birthDate"
                )
            )
        }

        val data = doc.data.orEmpty()

        val fullName = data.stringAny("fullName", "name", "displayName")
            ?: user.displayName.orEmpty()

        val email = data.stringAny("email")
            ?: user.email.orEmpty()

        val belt = data.stringAny("belt", "belt_current", "current_belt")
        val region = data.stringAny("region")
        val branch = data.stringAny("branch")
            ?: data.stringListAny("branches")?.firstOrNull()
        val groups = data.stringListAny("groups")
        val role = data.stringAny("role")
        val phone = data.stringAny("phone")
        val gender = data.stringAny("gender")
        val birthDate = data.stringAny("birthDate", "birth_date")

        val missing = buildList {
            if (belt.isNullOrBlank()) add("belt")
            if (region.isNullOrBlank()) add("region")
            if (branch.isNullOrBlank()) add("branch")
            if (groups.isNullOrEmpty()) add("groups")
            if (role.isNullOrBlank()) add("role")
            if (phone.isNullOrBlank()) add("phone")
            if (gender.isNullOrBlank()) add("gender")
            if (birthDate.isNullOrBlank()) add("birthDate")
        }

        sp.edit()
            .putString("uid", user.uid)
            .putString("email", email)
            .putString("displayName", user.displayName.orEmpty())
            .putString("photoUrl", user.photoUrl?.toString().orEmpty())
            .putString("fullName", fullName)
            .putString("belt_current", belt.orEmpty())
            .putString("belt", belt.orEmpty())
            .putString("region", region.orEmpty())
            .putString("branch", branch.orEmpty())
            .putStringSet("groups", groups.orEmpty().toSet())
            .putString("role", role.orEmpty())
            .putString("phone", phone.orEmpty())
            .putString("gender", gender.orEmpty())
            .putString("birthDate", birthDate.orEmpty())
            .putBoolean("profile_complete", missing.isEmpty())
            .apply()

        Log.d(
            TAG,
            "Profile status uid=${user.uid}, complete=${missing.isEmpty()}, missing=$missing"
        )

        return ProfileStatus(
            isComplete = missing.isEmpty(),
            missingFields = missing
        )
    }

    private fun persistAuthOnlyUser(
        sp: SharedPreferences,
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?
    ) {
        sp.edit()
            .putString("uid", uid)
            .putString("email", email.orEmpty())
            .putString("displayName", displayName.orEmpty())
            .putString("fullName", displayName.orEmpty())
            .putString("photoUrl", photoUrl.orEmpty())
            .putBoolean("profile_complete", false)
            .apply()
    }

    private fun Map<String, Any>.stringAny(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            this[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.stringListAny(vararg keys: String): List<String>? {
        return keys.firstNotNullOfOrNull { key ->
            val value = this[key]
            when (value) {
                is List<*> -> value.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                is String -> value.trim().takeIf { it.isNotEmpty() }?.let { listOf(it) }
                else -> null
            }
        }
    }
}