package il.kmi.app.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

    suspend fun signInWithGoogle(context: Context): Result<GoogleAuthUser> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                // חשוב: זה ה-Web Client ID שנוצר דרך google-services.json
                .setServerClientId(context.getString(R.string.default_web_client_id))
                // false = מאפשר לבחור גם חשבון חדש, לא רק חשבונות שכבר התחברו בעבר
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
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
                Log.e(TAG, "Google ID token is blank")
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

            Log.d(
                TAG,
                "Google login success uid=${resultUser.uid}, email=${resultUser.email}, displayName=${resultUser.displayName}"
            )

            Result.success(resultUser)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google login cancelled by user", e)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager failed", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google login failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        Log.d(TAG, "Firebase signOut called")
    }

    fun currentUid(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    fun currentEmail(): String? {
        return FirebaseAuth.getInstance().currentUser?.email
    }
}