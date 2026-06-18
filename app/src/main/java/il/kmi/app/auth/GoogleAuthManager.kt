package il.kmi.app.auth

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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

    suspend fun signInWithGoogle(context: Context): Result<GoogleAuthUser> {
        return try {
            logStage(context, "credential_manager_start")

            val credentialManager = CredentialManager.create(context)
            val serverClientId = context.getString(R.string.default_web_client_id).trim()

            logStage(
                context = context,
                stage = "credential_manager_config_loaded",
                message = "serverClientIdBlank=${serverClientId.isBlank()}"
            )

            if (serverClientId.isBlank()) {
                val error = IllegalStateException("GOOGLE_CONFIG_EMPTY_CLIENT_ID")
                logStage(
                    context = context,
                    stage = "credential_manager_config_failed",
                    error = error
                )
                return Result.failure(error)
            }

            val response = getGoogleCredentialResponse(
                context = context,
                credentialManager = credentialManager,
                serverClientId = serverClientId
            )

            logStage(context, "credential_manager_response_received")

            val firebaseResult = signInToFirebaseWithGoogleCredential(
                context = context,
                response = response
            )

            firebaseResult
                .onSuccess {
                    logStage(
                        context = context,
                        stage = "credential_manager_firebase_success",
                        message = "uid=${it.uid}, email=${it.email.orEmpty()}"
                    )
                }
                .onFailure { error ->
                    logStage(
                        context = context,
                        stage = "credential_manager_firebase_failure",
                        error = error
                    )
                }

            firebaseResult
        } catch (e: GetCredentialCancellationException) {
            logStage(
                context = context,
                stage = "credential_manager_cancelled",
                error = e
            )

            // ✅ אם המשתמש לחץ ביטול — עוצרים ולא נכנסים לאפליקציה.
            // ✅ אם זו תקלת [16] Account reauth failed — כן מפעילים Classic fallback.
            if (isReauth16Like(e)) {
                Result.failure(
                    GoogleClassicFallbackRequiredException(
                        "GOOGLE_REAUTH_16_FALLBACK_REQUIRED",
                        e
                    )
                )
            } else {
                Result.failure(e)
            }
        } catch (e: NoCredentialException) {
            logStage(
                context = context,
                stage = "credential_manager_no_credential",
                error = e
            )
            Result.failure(GoogleClassicFallbackRequiredException("GOOGLE_NO_CREDENTIAL", e))
        } catch (e: GetCredentialException) {
            logStage(
                context = context,
                stage = "credential_manager_get_credential_exception",
                error = e
            )

            if (isNoCredentialLike(e) || isCancellationLike(e)) {
                Result.failure(GoogleClassicFallbackRequiredException("GOOGLE_CREDENTIAL_UNAVAILABLE", e))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            logStage(
                context = context,
                stage = "credential_manager_unexpected_failure",
                error = e
            )
            Result.failure(e)
        }
    }

    private suspend fun getGoogleCredentialResponse(
        context: Context,
        credentialManager: CredentialManager,
        serverClientId: String
    ): GetCredentialResponse {
        logStage(context, "credential_manager_google_id_option_build_start")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val googleIdRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            logStage(context, "credential_manager_google_id_get_credential_start")

            credentialManager.getCredential(
                context = context,
                request = googleIdRequest
            ).also {
                logStage(context, "credential_manager_google_id_get_credential_success")
            }
        } catch (e: GetCredentialCancellationException) {
            logStage(
                context = context,
                stage = "credential_manager_google_id_cancelled",
                error = e
            )
            throw e
        } catch (e: NoCredentialException) {
            logStage(
                context = context,
                stage = "credential_manager_google_id_no_credential_try_explicit",
                error = e
            )

            getGoogleCredentialResponseWithExplicitButtonFlow(
                context = context,
                credentialManager = credentialManager,
                serverClientId = serverClientId
            )
        } catch (e: GetCredentialException) {
            logStage(
                context = context,
                stage = "credential_manager_google_id_get_credential_exception",
                error = e
            )

            if (isNoCredentialLike(e)) {
                getGoogleCredentialResponseWithExplicitButtonFlow(
                    context = context,
                    credentialManager = credentialManager,
                    serverClientId = serverClientId
                )
            } else {
                throw e
            }
        }
    }

    private suspend fun getGoogleCredentialResponseWithExplicitButtonFlow(
        context: Context,
        credentialManager: CredentialManager,
        serverClientId: String
    ): GetCredentialResponse {
        logStage(context, "credential_manager_explicit_button_build_start")

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId
        ).build()

        val signInWithGoogleRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        logStage(context, "credential_manager_explicit_button_get_credential_start")

        return credentialManager.getCredential(
            context = context,
            request = signInWithGoogleRequest
        ).also {
            logStage(context, "credential_manager_explicit_button_get_credential_success")
        }
    }

    private fun isRealUserCancelLike(error: Throwable): Boolean {
        val clean = listOfNotNull(
            error.localizedMessage,
            error.message,
            error.toString(),
            error.cause?.localizedMessage,
            error.cause?.message,
            error.cause?.toString()
        ).joinToString(" ")

        val isReauth16 =
            clean.contains("Account reauth failed", ignoreCase = true) ||
                    clean.contains("reauth failed", ignoreCase = true) ||
                    clean.contains("reauth", ignoreCase = true) ||
                    clean.contains("[16]", ignoreCase = true)

        val isCancel =
            clean.contains("cancel", ignoreCase = true) ||
                    clean.contains("canceled", ignoreCase = true) ||
                    clean.contains("cancelled", ignoreCase = true) ||
                    clean.contains("12501", ignoreCase = true)

        return isCancel && !isReauth16
    }

    fun shouldUseClassicGoogleFallback(error: Throwable): Boolean {
        val clean = listOfNotNull(
            error.localizedMessage,
            error.message,
            error.toString(),
            error.cause?.localizedMessage,
            error.cause?.message,
            error.cause?.toString()
        ).joinToString(" ")

        val isFallbackWrapper =
            error is GoogleClassicFallbackRequiredException

        val isCredentialManagerError =
            error is NoCredentialException ||
                    (
                            error is GetCredentialException &&
                                    error !is GetCredentialCancellationException &&
                                    !isRealUserCancelLike(error)
                            )

        val isNoCredential =
            isNoCredentialLike(error)

        val isCancellation =
            isCancellationLike(error) && isReauth16Like(error)

        val isReauth16 = isReauth16Like(error)

        val shouldFallback =
            !isRealUserCancelLike(error) &&
                    (
                            isFallbackWrapper ||
                                    isCredentialManagerError ||
                                    isNoCredential ||
                                    isCancellation ||
                                    isReauth16
                            )

        Log.d(
            TAG,
            "stage=should_use_classic_fallback, shouldFallback=$shouldFallback, " +
                    "isFallbackWrapper=$isFallbackWrapper, " +
                    "isCredentialManagerError=$isCredentialManagerError, " +
                    "isNoCredential=$isNoCredential, " +
                    "isCancellation=$isCancellation, " +
                    "isReauth16=$isReauth16, " +
                    "errorClass=${error.javaClass.name}, " +
                    "errorMessage=${error.message.orEmpty()}, " +
                    "causeClass=${error.cause?.javaClass?.name.orEmpty()}, " +
                    "causeMessage=${error.cause?.message.orEmpty()}"
        )

        return shouldFallback
    }

    fun classicGoogleSignInIntent(context: Context): Intent {
        logStage(context, "classic_intent_build_start")

        val serverClientId = context.getString(R.string.default_web_client_id).trim()

        logStage(
            context = context,
            stage = "classic_intent_config_loaded",
            message = "serverClientIdBlank=${serverClientId.isBlank()}"
        )

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, options).signInIntent.also {
            logStage(context, "classic_intent_build_success")
        }
    }

    suspend fun handleClassicGoogleSignInResult(
        context: Context,
        data: Intent?
    ): Result<GoogleAuthUser> {
        return try {
            logStage(
                context = context,
                stage = "classic_result_start",
                message = "intentDataNull=${data == null}"
            )

            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)

            logStage(
                context = context,
                stage = "classic_result_account_received",
                message = "email=${account.email.orEmpty()}, displayNameBlank=${account.displayName.isNullOrBlank()}, idTokenBlank=${account.idToken.isNullOrBlank()}"
            )

            val idToken = account.idToken.orEmpty()

            if (idToken.isBlank()) {
                val error = IllegalStateException("GOOGLE_CLASSIC_ID_TOKEN_BLANK")
                logStage(
                    context = context,
                    stage = "classic_result_id_token_blank",
                    error = error
                )
                return Result.failure(error)
            }

            val firebaseResult = signInToFirebaseWithIdToken(
                context = context,
                source = "classic",
                idToken = idToken
            )

            firebaseResult
                .onSuccess {
                    logStage(
                        context = context,
                        stage = "classic_firebase_success",
                        message = "uid=${it.uid}, email=${it.email.orEmpty()}"
                    )
                }
                .onFailure { error ->
                    logStage(
                        context = context,
                        stage = "classic_firebase_failure",
                        error = error
                    )
                }

            firebaseResult
        } catch (e: Exception) {
            logStage(
                context = context,
                stage = "classic_result_failure",
                error = e
            )
            Result.failure(e)
        }
    }

    private fun isNoCredentialLike(error: Throwable): Boolean {
        val clean = listOfNotNull(
            error.localizedMessage,
            error.message,
            error.toString()
        ).joinToString(" ")

        return clean.contains("NoCredential", ignoreCase = true) ||
                clean.contains("No credentials", ignoreCase = true) ||
                clean.contains("credentials available", ignoreCase = true) ||
                clean.contains("no available credentials", ignoreCase = true)
    }

    private fun isReauth16Like(error: Throwable): Boolean {
        val clean = listOfNotNull(
            error.localizedMessage,
            error.message,
            error.toString(),
            error.cause?.localizedMessage,
            error.cause?.message,
            error.cause?.toString()
        ).joinToString(" ")

        return clean.contains("Account reauth failed", ignoreCase = true) ||
                clean.contains("reauth failed", ignoreCase = true) ||
                clean.contains("reauth", ignoreCase = true) ||
                clean.contains("[16]", ignoreCase = true)
    }

    private fun isCancellationLike(error: Throwable): Boolean {
        val clean = listOfNotNull(
            error.localizedMessage,
            error.message,
            error.toString()
        ).joinToString(" ")

        return clean.contains("cancel", ignoreCase = true) ||
                clean.contains("canceled", ignoreCase = true) ||
                clean.contains("cancelled", ignoreCase = true) ||
                clean.contains("12501", ignoreCase = true) ||

                // ✅ Credential Manager sometimes fails on tester devices with:
                // androidx.credentials.exceptions.GetCredentialCancellationException:
                // [16] Account reauth failed.
                // This should not be shown as a configuration error.
                // It should trigger the classic Google Sign-In fallback.
                clean.contains("Account reauth failed", ignoreCase = true) ||
                clean.contains("reauth failed", ignoreCase = true) ||
                clean.contains("reauth", ignoreCase = true) ||
                clean.contains("[16]", ignoreCase = true)
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

    private class GoogleClassicFallbackRequiredException(
        message: String,
        cause: Throwable
    ) : Exception(message, cause)

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