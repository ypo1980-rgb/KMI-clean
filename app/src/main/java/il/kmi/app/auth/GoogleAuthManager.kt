package il.kmi.app.auth

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import il.kmi.app.R
import kotlinx.coroutines.tasks.await

object GoogleAuthManager {

    private const val TAG = "KMI_GOOGLE_AUTH"

    data class GoogleAuthUser(
        val uid: String,
        val email: String?,
        val displayName: String?,
        val photoUrl: String?
    )

    private fun safeString(block: () -> String): String {
        return try {
            block()
        } catch (e: Throwable) {
            "ERROR:${e.javaClass.simpleName}:${e.message.orEmpty()}"
        }
    }

    private fun packageVersionSummary(context: Context): String {
        return safeString {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }

            "${packageInfo.versionName}/$versionCode"
        }
    }

    private fun firebaseUserSummary(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return if (user == null) {
            "firebaseUser=null"
        } else {
            "firebaseUser.uid=${user.uid}, firebaseUser.email=${user.email.orEmpty()}, firebaseUser.isAnonymous=${user.isAnonymous}"
        }
    }

    private fun baseAppSummary(context: Context): String {
        val defaultWebClientId = safeString {
            context.getString(R.string.default_web_client_id).trim()
        }

        val googleAppId = safeString {
            val resId = context.resources.getIdentifier(
                "google_app_id",
                "string",
                context.packageName
            )

            if (resId != 0) {
                context.getString(resId).trim()
            } else {
                "MISSING_GOOGLE_APP_ID_RESOURCE"
            }
        }

        return "applicationId=${context.packageName}, " +
                "version=${packageVersionSummary(context)}, " +
                "default_web_client_id=$defaultWebClientId, " +
                "google_app_id=$googleAppId, " +
                "device=${Build.MANUFACTURER} ${Build.MODEL}, " +
                "android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}, " +
                firebaseUserSummary()
    }

    private fun logStage(
        context: Context?,
        stage: String,
        message: String = "",
        error: Throwable? = null
    ) {
        val appInfo = if (context != null) baseAppSummary(context) else firebaseUserSummary()

        val errorInfo = if (error != null) {
            val apiStatus = if (error is ApiException) {
                ", apiStatusCode=${error.statusCode}, apiStatus=${error.status}"
            } else {
                ""
            }

            ", errorClass=${error.javaClass.name}, errorMessage=${error.message.orEmpty()}$apiStatus"
        } else {
            ""
        }

        val line = "stage=$stage, $appInfo" +
                if (message.isNotBlank()) ", $message" else "" +
                        errorInfo

        if (error == null) {
            Log.d(TAG, line)
        } else {
            Log.e(TAG, line, error)
        }

        if (context != null) {
            writeGoogleAuthDiagnosticToServer(
                context = context,
                stage = stage,
                message = message,
                error = error
            )
        }
    }

    private fun writeGoogleAuthDiagnosticToServer(
        context: Context,
        stage: String,
        message: String,
        error: Throwable?
    ) {
        runCatching {
            val authUser = FirebaseAuth.getInstance().currentUser

            val defaultWebClientId = safeString {
                context.getString(R.string.default_web_client_id).trim()
            }

            val googleAppId = safeString {
                val resId = context.resources.getIdentifier(
                    "google_app_id",
                    "string",
                    context.packageName
                )

                if (resId != 0) {
                    context.getString(resId).trim()
                } else {
                    "MISSING_GOOGLE_APP_ID_RESOURCE"
                }
            }

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val apiStatusCode = if (error is ApiException) {
                error.statusCode
            } else {
                null
            }

            val cleanError = listOfNotNull(
                error?.localizedMessage,
                error?.message,
                error?.toString(),
                error?.cause?.localizedMessage,
                error?.cause?.message,
                error?.cause?.toString()
            ).joinToString(" ")

            val isReauth16 =
                cleanError.contains("Account reauth failed", ignoreCase = true) ||
                        cleanError.contains("reauth failed", ignoreCase = true) ||
                        cleanError.contains("[16]", ignoreCase = true)

            val isRealUserCancel =
                (
                        cleanError.contains("User cancelled", ignoreCase = true) ||
                                cleanError.contains("Cancelled by user", ignoreCase = true) ||
                                cleanError.contains("cancelled the selector", ignoreCase = true) ||
                                cleanError.contains("canceled", ignoreCase = true)
                        ) && !isReauth16

            val isGoogleAuthError =
                error != null && !isRealUserCancel

            val data = mutableMapOf<String, Any?>(
                "createdAt" to FieldValue.serverTimestamp(),
                "stage" to stage,
                "message" to message.take(800),

                "applicationId" to context.packageName,
                "versionName" to packageInfo.versionName.orEmpty(),
                "versionCode" to versionCode,

                "defaultWebClientId" to defaultWebClientId,
                "googleAppId" to googleAppId,

                "deviceManufacturer" to Build.MANUFACTURER,
                "deviceModel" to Build.MODEL,
                "androidRelease" to Build.VERSION.RELEASE,
                "androidSdk" to Build.VERSION.SDK_INT,

                "firebaseUid" to authUser?.uid.orEmpty(),
                "firebaseEmail" to authUser?.email.orEmpty(),
                "firebaseIsAnonymous" to (authUser?.isAnonymous ?: false),

                "errorClass" to (error?.javaClass?.name ?: ""),
                "errorMessage" to error?.message.orEmpty().take(800),
                "apiStatusCode" to apiStatusCode,

                "source" to "android_google_auth"
            )

            FirebaseFirestore.getInstance()
                .collection("google_auth_diagnostics")
                .add(data)
                .addOnFailureListener { writeError ->
                    Log.e(
                        TAG,
                        "stage=diagnostic_server_write_failure, errorClass=${writeError.javaClass.name}, errorMessage=${writeError.message.orEmpty()}",
                        writeError
                    )
                }
        }.onFailure { localError ->
            Log.e(
                TAG,
                "stage=diagnostic_local_build_failure, errorClass=${localError.javaClass.name}, errorMessage=${localError.message.orEmpty()}",
                localError
            )
        }
    }

    fun logUiStage(
        context: Context,
        stage: String,
        message: String = "",
        error: Throwable? = null
    ) {
        logStage(
            context = context,
            stage = stage,
            message = message,
            error = error
        )
    }

    suspend fun signInWithGoogle(
        context: Context
    ): Result<GoogleAuthUser> {
        return try {
            clearAnonymousFirebaseSessionBeforeGoogleAuth(
                context
            )

            val serverClientId =
                context
                    .getString(
                        R.string.default_web_client_id
                    )
                    .trim()

            if (serverClientId.isBlank()) {
                return Result.failure(
                    IllegalStateException(
                        "GOOGLE_CONFIG_EMPTY_CLIENT_ID"
                    )
                )
            }

            val credentialManager =
                CredentialManager.create(
                    context
                )

            val googleOption =
                GetSignInWithGoogleOption.Builder(
                    serverClientId
                ).build()

            val request =
                GetCredentialRequest.Builder()
                    .addCredentialOption(
                        googleOption
                    )
                    .build()

            val response =
                credentialManager.getCredential(
                    context = context,
                    request = request
                )

            signInToFirebaseWithGoogleCredential(
                context = context,
                response = response
            )
        } catch (
            e: GetCredentialCancellationException
        ) {
            Result.failure(e)
        } catch (
            e: GetCredentialException
        ) {
            Result.failure(e)
        } catch (
            e: Exception
        ) {
            Result.failure(e)
        }
    }

    private fun clearAnonymousFirebaseSessionBeforeGoogleAuth(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser?.isAnonymous == true) {
            logStage(
                context = context,
                stage = "google_auth_clear_anonymous_before_start",
                message = "anonymousUid=${currentUser.uid}"
            )

            FirebaseAuth.getInstance().signOut()
        }
    }

    private suspend fun signInToFirebaseWithGoogleCredential(
        context: Context,
        response: GetCredentialResponse
    ): Result<GoogleAuthUser> {
        return try {
            logStage(context, "firebase_google_credential_parse_start")

            val credential = response.credential

            logStage(
                context = context,
                stage = "firebase_google_credential_type",
                message = "credentialClass=${credential.javaClass.name}, credentialType=${(credential as? CustomCredential)?.type.orEmpty()}"
            )

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val error = IllegalStateException("GOOGLE_INVALID_CREDENTIAL_TYPE")
                logStage(
                    context = context,
                    stage = "firebase_google_credential_invalid_type",
                    error = error
                )
                return Result.failure(error)
            }

            val googleCredential = GoogleIdTokenCredential.createFrom(
                credential.data
            )

            val idToken = googleCredential.idToken

            logStage(
                context = context,
                stage = "firebase_google_credential_token_parsed",
                message = "idTokenBlank=${idToken.isBlank()}, googleEmail=${googleCredential.id.orEmpty()}"
            )

            if (idToken.isBlank()) {
                val error = IllegalStateException("GOOGLE_ID_TOKEN_BLANK")
                logStage(
                    context = context,
                    stage = "firebase_google_credential_id_token_blank",
                    error = error
                )
                return Result.failure(error)
            }

            signInToFirebaseWithIdToken(
                context = context,
                source = "credential_manager",
                idToken = idToken
            )
        } catch (e: Exception) {
            logStage(
                context = context,
                stage = "firebase_google_credential_parse_failure",
                error = e
            )
            Result.failure(e)
        }
    }

    private suspend fun signInToFirebaseWithIdToken(
        context: Context,
        source: String,
        idToken: String
    ): Result<GoogleAuthUser> {
        return try {
            logStage(
                context = context,
                stage = "firebase_sign_in_with_id_token_start",
                message = "source=$source, idTokenBlank=${idToken.isBlank()}"
            )

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

            val authResult = FirebaseAuth.getInstance()
                .signInWithCredential(firebaseCredential)
                .await()

            logStage(
                context = context,
                stage = "firebase_sign_in_with_credential_success_raw",
                message = "source=$source, authResultUserNull=${authResult.user == null}"
            )

            val firebaseUser = authResult.user
                ?: return Result.failure(
                    IllegalStateException("FIREBASE_USER_NULL")
                )

            val resultUser = GoogleAuthUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString()
            )

            logStage(
                context = context,
                stage = "firebase_result_user_ready",
                message = "source=$source, uid=${resultUser.uid}, email=${resultUser.email.orEmpty()}, displayNameBlank=${resultUser.displayName.isNullOrBlank()}"
            )

            Result.success(resultUser)
        } catch (e: Exception) {
            logStage(
                context = context,
                stage = "firebase_sign_in_with_id_token_failure",
                message = "source=$source",
                error = e
            )
            Result.failure(e)
        }
    }

    fun signOut() {
        Log.d(TAG, "stage=sign_out_before, ${firebaseUserSummary()}")
        FirebaseAuth.getInstance().signOut()
        Log.d(TAG, "stage=sign_out_after, ${firebaseUserSummary()}")
    }

    fun currentUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    fun currentEmail(): String? {
        return FirebaseAuth.getInstance().currentUser?.email
    }
}