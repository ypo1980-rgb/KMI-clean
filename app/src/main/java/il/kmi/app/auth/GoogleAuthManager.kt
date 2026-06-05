package il.kmi.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
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

            val googleSignInOption = GetSignInWithGoogleOption.Builder(
                context.getString(R.string.default_web_client_id)
            )
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleSignInOption)
                .build()

            val response = credentialManager.getCredential(
                context = context,
                request = request
            )

            val googleCredential = GoogleIdTokenCredential.createFrom(
                response.credential.data
            )

            val idToken = googleCredential.idToken

            if (idToken.isBlank()) {
                return Result.failure(IllegalStateException("Google ID token is blank"))
            }

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

            val authResult = FirebaseAuth.getInstance()
                .signInWithCredential(firebaseCredential)
                .await()

            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Firebase user is null"))

            val resultUser = GoogleAuthUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString()
            )

            Result.success(resultUser)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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