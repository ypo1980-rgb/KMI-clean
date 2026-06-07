package il.kmi.app.auth

import android.content.Context
import android.content.Intent
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
import il.kmi.app.R
import kotlinx.coroutines.tasks.await

object GoogleAuthManager {

    data class GoogleAuthUser(
        val uid: String,
        val email: String?,
        val displayName: String?,
        val photoUrl: String?
    )

    suspend fun signInWithGoogle(context: Context): Result<GoogleAuthUser> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val serverClientId = context.getString(R.string.default_web_client_id).trim()

            if (serverClientId.isBlank()) {
                return Result.failure(
                    IllegalStateException("GOOGLE_CONFIG_EMPTY_CLIENT_ID")
                )
            }

            val response = getGoogleCredentialResponse(
                context = context,
                credentialManager = credentialManager,
                serverClientId = serverClientId
            )

            signInToFirebaseWithGoogleCredential(response)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(GoogleClassicFallbackRequiredException("GOOGLE_CREDENTIAL_CANCELLED", e))
        } catch (e: NoCredentialException) {
            Result.failure(GoogleClassicFallbackRequiredException("GOOGLE_NO_CREDENTIAL", e))
        } catch (e: GetCredentialException) {
            if (isNoCredentialLike(e) || isCancellationLike(e)) {
                Result.failure(GoogleClassicFallbackRequiredException("GOOGLE_CREDENTIAL_UNAVAILABLE", e))
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getGoogleCredentialResponse(
        context: Context,
        credentialManager: CredentialManager,
        serverClientId: String
    ): GetCredentialResponse {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val googleIdRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            credentialManager.getCredential(
                context = context,
                request = googleIdRequest
            )
        } catch (e: GetCredentialCancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            getGoogleCredentialResponseWithExplicitButtonFlow(
                context = context,
                credentialManager = credentialManager,
                serverClientId = serverClientId
            )
        } catch (e: GetCredentialException) {
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
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId
        ).build()

        val signInWithGoogleRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return credentialManager.getCredential(
            context = context,
            request = signInWithGoogleRequest
        )
    }

    fun shouldUseClassicGoogleFallback(error: Throwable): Boolean {
        return error is GoogleClassicFallbackRequiredException ||
                error is GetCredentialCancellationException ||
                error is NoCredentialException ||
                error is GetCredentialException ||
                isNoCredentialLike(error) ||
                isCancellationLike(error)
    }

    fun classicGoogleSignInIntent(context: Context): Intent {
        val serverClientId = context.getString(R.string.default_web_client_id).trim()

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, options).signInIntent
    }

    suspend fun handleClassicGoogleSignInResult(data: Intent?): Result<GoogleAuthUser> {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)

            val idToken = account.idToken.orEmpty()

            if (idToken.isBlank()) {
                return Result.failure(
                    IllegalStateException("GOOGLE_CLASSIC_ID_TOKEN_BLANK")
                )
            }

            signInToFirebaseWithIdToken(idToken)
        } catch (e: Exception) {
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

    private fun isCancellationLike(error: Throwable): Boolean {
        val clean = listOfNotNull(
            error.localizedMessage,
            error.message,
            error.toString()
        ).joinToString(" ")

        return clean.contains("cancel", ignoreCase = true) ||
                clean.contains("canceled", ignoreCase = true) ||
                clean.contains("cancelled", ignoreCase = true) ||
                clean.contains("12501", ignoreCase = true)
    }

    private suspend fun signInToFirebaseWithGoogleCredential(
        response: GetCredentialResponse
    ): Result<GoogleAuthUser> {
        val credential = response.credential

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return Result.failure(
                IllegalStateException("GOOGLE_INVALID_CREDENTIAL_TYPE")
            )
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(
            credential.data
        )

        val idToken = googleCredential.idToken

        if (idToken.isBlank()) {
            return Result.failure(
                IllegalStateException("GOOGLE_ID_TOKEN_BLANK")
            )
        }

        return signInToFirebaseWithIdToken(idToken)
    }

    private suspend fun signInToFirebaseWithIdToken(
        idToken: String
    ): Result<GoogleAuthUser> {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

        val authResult = FirebaseAuth.getInstance()
            .signInWithCredential(firebaseCredential)
            .await()

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

        return Result.success(resultUser)
    }

    private class GoogleClassicFallbackRequiredException(
        message: String,
        cause: Throwable
    ) : Exception(message, cause)

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    fun currentUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    fun currentEmail(): String? {
        return FirebaseAuth.getInstance().currentUser?.email
    }
}