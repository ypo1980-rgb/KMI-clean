package il.kmi.app.screens.admin

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AdminAccess {

    suspend fun isCurrentUserAdmin(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            return false
        }


        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        val settings = firestore.firestoreSettings

        Log.d(
            "KMI_ADMIN_ACCESS",
            "SDK connection: " +
                    "app=${firestore.app.name}, " +
                    "project=${firestore.app.options.projectId}, " +
                    "host=${settings.host}, " +
                    "ssl=${settings.isSslEnabled}, " +
                    "authProject=${auth.app.options.projectId}, " +
                    "sameFirebaseApp=${auth.app.name == firestore.app.name}"
        )

        val sdkAuthorized = try {
            val documentRef = firestore
                .collection("admins")
                .document(uid)

            val snap = documentRef
                .get(Source.SERVER)
                .await()

            val exists = snap.exists()
            val enabled = snap.getBoolean("enabled")
            val authorized = exists && enabled == true

            Log.d(
                "KMI_ADMIN_ACCESS",
                "SDK SERVER authorization: " +
                        "project=${firestore.app.options.projectId}, " +
                        "path=${documentRef.path}, " +
                        "uidSuffix=${uid.takeLast(6)}, " +
                        "exists=$exists, " +
                        "enabled=$enabled, " +
                        "fromCache=${snap.metadata.isFromCache}, " +
                        "pendingWrites=${snap.metadata.hasPendingWrites()}, " +
                        "authorized=$authorized"
            )

            authorized

        } catch (cancelled: CancellationException) {
            throw cancelled

        } catch (error: Exception) {
            Log.e(
                "KMI_ADMIN_ACCESS",
                "SDK authorization failed",
                error
            )
            false
        }

        if (sdkAuthorized) {
            return auth.currentUser?.uid == uid
        }

        /*
         * גיבוי מאומת באמצעות Firestore REST.
         * אין שימוש ב-UID ללא Firebase ID Token.
         */
        val restAuthorized = try {
            val token = auth.currentUser
                ?.takeIf { it.uid == uid }
                ?.getIdToken(false)
                ?.await()
                ?.token
                .orEmpty()

            val projectId =
                firestore.app.options.projectId.orEmpty()

            if (token.isBlank() || projectId.isBlank()) {
                Log.w(
                    "KMI_ADMIN_ACCESS",
                    "REST skipped: missing token or project"
                )
                false
            } else {
                withContext(Dispatchers.IO) {
                    val documentPath =
                        "projects/$projectId/" +
                                "databases/(default)/documents/" +
                                "admins/$uid"

                    val connection = (
                            URL(
                                "https://firestore.googleapis.com/v1/" +
                                        documentPath
                            ).openConnection() as HttpURLConnection
                            )

                    try {
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer $token"
                        )

                        connection.setRequestProperty(
                            "Accept",
                            "application/json"
                        )

                        val status = connection.responseCode

                        if (status != HttpURLConnection.HTTP_OK) {
                            Log.w(
                                "KMI_ADMIN_ACCESS",
                                "REST authorization HTTP $status"
                            )
                            false
                        } else {
                            val response = connection
                                .inputStream
                                .bufferedReader(Charsets.UTF_8)
                                .use { it.readText() }

                            val document = JSONObject(response)
                            val fields =
                                document.optJSONObject("fields")

                            val enabled =
                                fields
                                    ?.optJSONObject("enabled")
                                    ?.optBoolean(
                                        "booleanValue",
                                        false
                                    ) == true

                            val nameMatches =
                                document.optString("name") ==
                                        documentPath

                            val authorized =
                                nameMatches && enabled

                            Log.d(
                                "KMI_ADMIN_ACCESS",
                                "REST authorization: " +
                                        "nameMatches=$nameMatches, " +
                                        "enabled=$enabled, " +
                                        "authorized=$authorized"
                            )

                            authorized
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
            }

        } catch (cancelled: CancellationException) {
            throw cancelled

        } catch (error: Exception) {
            Log.e(
                "KMI_ADMIN_ACCESS",
                "REST authorization failed",
                error
            )
            false
        }

        /*
         * אין לקבל תוצאת הרשאה של משתמש קודם
         * אם המשתמש התחלף בזמן הבדיקה.
         */
        val sameUser =
            auth.currentUser?.uid == uid

        val authorized =
            sameUser && restAuthorized

        Log.d(
            "KMI_ADMIN_ACCESS",
            "Final authorization: " +
                    "sdk=$sdkAuthorized, " +
                    "rest=$restAuthorized, " +
                    "sameUser=$sameUser, " +
                    "authorized=$authorized"
        )

        return authorized
    }
}
