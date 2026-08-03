package il.kmi.app.progress

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

object UserProgressRepository {

    fun bucketForPercent(percent: Int): Int {
        val safePercent = percent.coerceIn(0, 100)

        return when {
            safePercent < 10 -> 0
            safePercent < 20 -> 10
            safePercent < 30 -> 20
            safePercent < 40 -> 30
            safePercent < 50 -> 40
            safePercent < 60 -> 50
            safePercent < 70 -> 60
            safePercent < 80 -> 70
            safePercent < 90 -> 80
            safePercent < 100 -> 90
            else -> 100
        }
    }

    suspend fun saveUserProgress(
        beltId: String,
        knownPercent: Int,
        knownCount: Int,
        totalCount: Int
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return

        val safePercent = knownPercent.coerceIn(0, 100)
        val safeKnownCount = knownCount.coerceAtLeast(0)
        val safeTotalCount = totalCount.coerceAtLeast(0)

        val data = mapOf(
            "uid" to uid,
            "beltId" to beltId,
            "knownPercent" to safePercent,
            "knownCount" to safeKnownCount,
            "totalCount" to safeTotalCount,
            "bucket" to bucketForPercent(safePercent),
            "updatedAt" to Timestamp.now()
        )

        FirebaseFirestore.getInstance()
            .collection("userProgress")
            .document(uid)
            .set(data)
            .await()
    }

    suspend fun loadBeltComparison(
        beltId: String,
        userKnownPercent: Int
    ): UserProgressComparison? {
        val currentUid =
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid
                .orEmpty()

        val cleanBeltId = beltId.trim()

        if (
            currentUid.isBlank() ||
            cleanBeltId.isBlank()
        ) {
            return null
        }

        /*
         * קוראים את נתוני ההתקדמות האמיתיים של המשתמשים
         * באותה חגורה. אין תלות במסמך beltStats חיצוני
         * שאינו מתעדכן מתוך האפליקציה.
         */
        val snapshot =
            FirebaseFirestore.getInstance()
                .collection("userProgress")
                .whereEqualTo(
                    "beltId",
                    cleanBeltId
                )
                .get()
                .await()

        /*
         * המשתמש הנוכחי אינו משתתף בממוצע.
         * בנוסף מתעלמים ממסמכים ריקים או לא תקינים.
         */
        val otherTraineePercents =
            snapshot.documents
                .mapNotNull { document ->
                    val documentUid =
                        document.getString("uid")
                            ?.trim()
                            .orEmpty()

                    val totalCount =
                        (document.getLong("totalCount") ?: 0L)
                            .toInt()

                    val knownPercent =
                        (document.getLong("knownPercent") ?: -1L)
                            .toInt()

                    if (
                        documentUid.isNotBlank() &&
                        documentUid != currentUid &&
                        totalCount > 0 &&
                        knownPercent in 0..100
                    ) {
                        documentUid to knownPercent
                    } else {
                        null
                    }
                }
                .distinctBy { (uid, _) ->
                    uid
                }
                .map { (_, percent) ->
                    percent
                }

        if (otherTraineePercents.isEmpty()) {
            return null
        }

        val safeUserPercent =
            userKnownPercent.coerceIn(0, 100)

        val usersCount =
            otherTraineePercents.size

        val averageKnownPercent =
            otherTraineePercents
                .average()
                .roundToInt()
                .coerceIn(0, 100)

        /*
         * "אתה מעל" מחושב רק מול מתאמנים שהאחוז שלהם
         * נמוך ממש מאחוז המשתמש, ולא כולל שוויון.
         */
        val traineesBelowUser =
            otherTraineePercents.count { percent ->
                percent < safeUserPercent
            }

        val percentileAbove =
            (
                    traineesBelowUser.toFloat() /
                            usersCount.toFloat() *
                            100f
                    )
                .roundToInt()
                .coerceIn(0, 100)

        return UserProgressComparison(
            beltId = cleanBeltId,
            usersCount = usersCount,
            userKnownPercent = safeUserPercent,
            averageKnownPercent = averageKnownPercent,
            percentileAbove = percentileAbove,
            hasEnoughData = true
        )
    }
}