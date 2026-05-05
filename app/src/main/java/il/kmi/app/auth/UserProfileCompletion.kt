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

        val mainSp = context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
        val userSp = context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

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

        // פעם ראשונה עם Google: אין עדיין מסמך משתמש באפליקציה.
        // נשמור רק נתוני Auth בסיסיים כדי שהטופס יתמלא בשם/אימייל אם קיימים.
        if (doc == null || !doc.exists()) {
            persistAuthOnlyUser(
                mainSp = mainSp,
                userSp = userSp,
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString(),
                phoneNumber = user.phoneNumber
            )

            return ProfileStatus(
                isComplete = false,
                missingFields = listOf(
                    "phone",
                    "birthDate",
                    "gender",
                    "region",
                    "branch",
                    "groups",
                    "belt",
                    "role"
                )
            )
        }

        val data = doc.data.orEmpty()

        val role = data.stringAny(
            "role",
            "user_role"
        ).orEmpty()

        val roleLower = role.lowercase()
        val isCoach =
            roleLower == "coach" ||
                    roleLower.contains("coach") ||
                    roleLower.contains("מאמן") ||
                    roleLower.contains("מדריך")

        val fullName =
            data.stringAny("fullName", "full_name", "name", "displayName", "display_name")
                ?: user.displayName.orEmpty()

        val email =
            data.stringAny("email", "user_email")
                ?: user.email.orEmpty()

        val phone =
            (
                    data.stringAny("phone", "phoneNumber", "phone_number")
                        ?: user.phoneNumber.orEmpty()
                    ).filter { it.isDigit() }

        val region = data.stringAny("region").orEmpty()

        val branchesList =
            data.stringListAny("branches")
                ?: splitCsv(
                    data.stringAny(
                        "branchesCsv",
                        "branch",
                        "selected_branches",
                        "branches"
                    ).orEmpty()
                )

        val branchesFinal = branchesList
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")

        val activeBranchFinal =
            data.stringAny("activeBranch", "active_branch")
                ?.takeIf { it.isNotBlank() }
                ?: branchesList.firstOrNull().orEmpty()

        val groupsList =
            data.stringListAny("groups")
                ?: splitCsv(
                    data.stringAny(
                        "groupsCsv",
                        "primaryGroup",
                        "activeGroup",
                        "age_group",
                        "group",
                        "selected_groups",
                        "groups"
                    ).orEmpty()
                )

        val groupsFinal = groupsList
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")

        val primaryGroup =
            data.stringAny("primaryGroup", "activeGroup", "age_group", "group")
                ?.takeIf { it.isNotBlank() }
                ?: groupsList.firstOrNull().orEmpty()

        val activeGroupFinal =
            data.stringAny("activeGroup", "active_group")
                ?.takeIf { it.isNotBlank() }
                ?: primaryGroup

        val gender = data.stringAny("gender").orEmpty()

        val beltFinal = data.stringAny(
            "belt",
            "currentBelt",
            "current_belt",
            "belt_current"
        ).orEmpty()

        val birthDate = data.stringAny(
            "birthDate",
            "birth_date"
        ).orEmpty()

        val birthParts = birthDate.split("-")
        val birthYear = birthParts.getOrNull(0)?.toIntOrNull()?.toString() ?: ""
        val birthMonth = birthParts.getOrNull(1)?.toIntOrNull()?.toString() ?: ""
        val birthDay = birthParts.getOrNull(2)?.toIntOrNull()?.toString() ?: ""

        val profileCompleted = data.booleanAny(
            "profileCompleted",
            "profile_completed"
        )

        val registrationComplete = data.booleanAny(
            "registrationComplete",
            "registration_complete"
        )

        val registrationFormCompleted = data.booleanAny(
            "registrationFormCompleted",
            "registration_form_completed"
        )

        val registrationSchemaVersion =
            data.longAny("registrationSchemaVersion", "registration_schema_version")?.toInt() ?: 0

        val hasNewFormCompletion =
            registrationFormCompleted && registrationSchemaVersion >= 2

        val hasCoreProfile =
            fullName.isNotBlank() &&
                    email.isNotBlank() &&
                    phone.length >= 9 &&
                    region.isNotBlank() &&
                    branchesFinal.isNotBlank() &&
                    (isCoach || primaryGroup.isNotBlank()) &&
                    gender.isNotBlank() &&
                    (isCoach || beltFinal.isNotBlank()) &&
                    birthDate.isNotBlank()

        val legacyComplete =
            (profileCompleted || registrationComplete) && hasCoreProfile

        val isComplete =
            hasCoreProfile && (hasNewFormCompletion || legacyComplete)

        val missing = buildList {
            if (fullName.isBlank()) add("fullName")
            if (email.isBlank()) add("email")
            if (phone.length < 9) add("phone")
            if (region.isBlank()) add("region")
            if (branchesFinal.isBlank()) add("branch")
            if (!isCoach && primaryGroup.isBlank()) add("groups")
            if (role.isBlank()) add("role")
            if (gender.isBlank()) add("gender")
            if (!isCoach && beltFinal.isBlank()) add("belt")
            if (birthDate.isBlank()) add("birthDate")
            if (!hasNewFormCompletion && !legacyComplete) add("registrationFormCompleted")
        }

        persistProfileLocally(
            mainSp = mainSp,
            userSp = userSp,
            uid = user.uid,
            fullName = fullName,
            email = email,
            phone = phone,
            photoUrl = user.photoUrl?.toString().orEmpty(),
            role = role,
            region = region,
            branchesFinal = branchesFinal,
            activeBranchFinal = activeBranchFinal,
            groupsFinal = groupsFinal,
            primaryGroup = primaryGroup,
            activeGroupFinal = activeGroupFinal,
            gender = gender,
            beltFinal = beltFinal,
            birthDate = birthDate,
            birthDay = birthDay,
            birthMonth = birthMonth,
            birthYear = birthYear,
            isComplete = isComplete
        )

        Log.e(
            TAG,
            "Profile status uid=${user.uid} complete=$isComplete missing=$missing " +
                    "fullName=${fullName.isNotBlank()} email=${email.isNotBlank()} " +
                    "phoneLen=${phone.length} role=$role region=${region.isNotBlank()} " +
                    "branch=${branchesFinal.isNotBlank()} group=${primaryGroup.isNotBlank()} " +
                    "gender=${gender.isNotBlank()} belt=$beltFinal birthDate=${birthDate.isNotBlank()} " +
                    "registrationFormCompleted=$registrationFormCompleted schema=$registrationSchemaVersion"
        )

        return ProfileStatus(
            isComplete = isComplete,
            missingFields = missing
        )
    }

    private fun persistAuthOnlyUser(
        mainSp: SharedPreferences,
        userSp: SharedPreferences,
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
        phoneNumber: String?
    ) {
        val cleanPhone = phoneNumber.orEmpty().filter { it.isDigit() }

        fun SharedPreferences.Editor.putAuthOnly(): SharedPreferences.Editor {
            putString("uid", uid)
            putString("firebase_uid", uid)

            putString("email", email.orEmpty())
            putString("user_email", email.orEmpty())

            putString("displayName", displayName.orEmpty())
            putString("fullName", displayName.orEmpty())
            putString("name", displayName.orEmpty())
            putString("user_name", displayName.orEmpty())

            putString("photoUrl", photoUrl.orEmpty())

            if (cleanPhone.isNotBlank()) {
                putString("phone", cleanPhone)
                putString("phone_number", cleanPhone)
            }

            putString("authProvider", "google")
            putBoolean("google_login", true)
            putBoolean("skip_otp", true)

            putBoolean("profile_complete", false)
            putBoolean("profile_completed", false)
            putBoolean("registration_complete", false)
            putBoolean("registration_form_completed", false)

            return this
        }

        mainSp.edit()
            .putAuthOnly()
            .apply()

        userSp.edit()
            .putAuthOnly()
            .apply()

        Log.e(
            TAG,
            "persistAuthOnlyUser uid=$uid email=${email.orEmpty().isNotBlank()} " +
                    "displayName=${displayName.orEmpty().isNotBlank()} phoneLen=${cleanPhone.length}"
        )
    }

    private fun persistProfileLocally(
        mainSp: SharedPreferences,
        userSp: SharedPreferences,
        uid: String,
        fullName: String,
        email: String,
        phone: String,
        photoUrl: String,
        role: String,
        region: String,
        branchesFinal: String,
        activeBranchFinal: String,
        groupsFinal: String,
        primaryGroup: String,
        activeGroupFinal: String,
        gender: String,
        beltFinal: String,
        birthDate: String,
        birthDay: String,
        birthMonth: String,
        birthYear: String,
        isComplete: Boolean
    ) {
        val completedAt = System.currentTimeMillis()

        fun SharedPreferences.Editor.putProfileCore(): SharedPreferences.Editor {
            remove("groups")
            remove("branches")
            remove("selected_groups")
            remove("selected_branches")

            putString("uid", uid)
            putString("firebase_uid", uid)

            putString("fullName", fullName)
            putString("name", fullName)
            putString("user_name", fullName)
            putString("displayName", fullName)

            putString("email", email)
            putString("user_email", email)

            putString("phone", phone)
            putString("phone_number", phone)

            putString("photoUrl", photoUrl)

            putString("role", role)
            putString("user_role", role)

            putString("region", region)

            putString("branch", branchesFinal)
            putString("branches", branchesFinal)
            putString("selected_branches", branchesFinal)
            putString("active_branch", activeBranchFinal)

            putString("age_groups", groupsFinal)
            putString("groups", groupsFinal)
            putString("selected_groups", groupsFinal)
            putString("age_group", primaryGroup)
            putString("group", primaryGroup)
            putString("active_group", activeGroupFinal)

            putString("gender", gender)

            putString("belt", beltFinal)
            putString("current_belt", beltFinal)
            putString("belt_current", beltFinal)

            putString("birthDate", birthDate)
            putString("birth_date", birthDate)

            if (birthDay.isNotBlank()) putString("birth_day", birthDay)
            if (birthMonth.isNotBlank()) putString("birth_month", birthMonth)
            if (birthYear.isNotBlank()) putString("birth_year", birthYear)

            putString("authProvider", "google")
            putBoolean("google_login", true)
            putBoolean("skip_otp", true)

            putBoolean("profile_complete", isComplete)
            putBoolean("profile_completed", isComplete)
            putBoolean("registration_complete", isComplete)
            putBoolean("registration_form_completed", isComplete)

            if (isComplete) {
                putInt("registration_schema_version", 2)
                putString("profile_completed_uid", uid)
                putLong("profile_completed_at", completedAt)
            }

            return this
        }

        mainSp.edit()
            .putProfileCore()
            .commit()

        userSp.edit()
            .putProfileCore()
            .commit()

        Log.e(
            TAG,
            "persistProfileLocally uid=$uid isComplete=$isComplete " +
                    "fullName=${fullName.isNotBlank()} email=${email.isNotBlank()} " +
                    "phoneLen=${phone.length} role=$role region=${region.isNotBlank()} " +
                    "branch=${branchesFinal.isNotBlank()} group=${primaryGroup.isNotBlank()} " +
                    "gender=${gender.isNotBlank()} belt=$beltFinal birthDate=${birthDate.isNotBlank()}"
        )
    }

    private fun splitCsv(raw: String): List<String> {
        return raw
            .split(',', ';', '|', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun Map<String, Any>.stringAny(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            this[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }
    }

    private fun Map<String, Any>.longAny(vararg keys: String): Long? {
        return keys.firstNotNullOfOrNull { key ->
            when (val value = this[key]) {
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            }
        }
    }

    private fun Map<String, Any>.booleanAny(vararg keys: String): Boolean {
        return keys.any { key ->
            when (val value = this[key]) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                is Number -> value.toInt() == 1
                else -> false
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.stringListAny(vararg keys: String): List<String>? {
        return keys.firstNotNullOfOrNull { key ->
            val value = this[key]

            when (value) {
                is List<*> -> {
                    value
                        .mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                        .distinct()
                        .takeIf { it.isNotEmpty() }
                }

                is Set<*> -> {
                    value
                        .mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
                        .distinct()
                        .takeIf { it.isNotEmpty() }
                }

                is String -> {
                    splitCsv(value)
                        .takeIf { it.isNotEmpty() }
                }

                else -> null
            }
        }
    }
}