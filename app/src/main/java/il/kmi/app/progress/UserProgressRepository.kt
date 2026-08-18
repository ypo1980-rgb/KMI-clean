package il.kmi.app.progress

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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

        val cleanBeltId = beltId.trim()

        if (cleanBeltId.isBlank()) {
            return
        }

        val safePercent = knownPercent.coerceIn(0, 100)
        val safeKnownCount = knownCount.coerceAtLeast(0)
        val safeTotalCount = totalCount.coerceAtLeast(0)

        /*
         * כל משתמש מקבל מסמך נפרד לכל חגורה.
         *
         * בעבר המסמך נשמר רק לפי uid:
         *     userProgress/{uid}
         *
         * ולכן מעבר לחגורה אחרת דרס את נתוני החגורה הקודמת.
         *
         * מעכשיו המבנה הוא:
         *     userProgress/{uid}__{beltId}
         */
        val documentId =
            "${uid}__${cleanBeltId}"

        val data = mapOf(
            "uid" to uid,
            "beltId" to cleanBeltId,
            "knownPercent" to safePercent,
            "knownCount" to safeKnownCount,
            "totalCount" to safeTotalCount,
            "bucket" to bucketForPercent(safePercent),
            "updatedAt" to Timestamp.now()
        )

        FirebaseFirestore.getInstance()
            .collection("userProgress")
            .document(documentId)
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
        /*
         * נתוני ההשוואה חייבים להגיע מהשרת.
         *
         * אחרת מכשיר אחד עלול להשתמש ב-cache שבו
         * ההתקדמות של המשתמש השני עדיין 0%, למרות
         * שבמכשיר השני כבר נשמר אחוז חדש.
         */
        val snapshot =
            FirebaseFirestore.getInstance()
                .collection("userProgress")
                .whereEqualTo(
                    "beltId",
                    cleanBeltId
                )
                .get(Source.SERVER)
                .await()

        /*
   * מאחדים את כל הרשומות של אותה חגורה לפי uid.
   *
   * בתקופת המעבר ייתכן שלמשתמש קיימת גם רשומת legacy
   * וגם רשומת uid__beltId, ולכן תמיד בוחרים את הרשומה
   * העדכנית ביותר לפי updatedAt.
   */
        /*
         * משתמשים רק במסמכים במבנה החדש:
         *
         *     {uid}__{beltId}
         *
         * מסמכי legacy בשם uid בלבד אינם משתתפים יותר
         * בחישוב, כדי שערך ישן לא ידרוס את נתוני החגורה
         * החדשים של אותו משתמש.
         */
        val latestPercentByUid =
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

                    val expectedDocumentId =
                        "${documentUid}__${cleanBeltId}"

                    if (
                        documentUid.isNotBlank() &&
                        document.id == expectedDocumentId &&
                        totalCount > 0 &&
                        knownPercent in 0..100
                    ) {
                        documentUid to knownPercent
                    } else {
                        null
                    }
                }
                .toMap()

        val safeUserPercent =
            userKnownPercent.coerceIn(0, 100)

        /*
         * לצורך "אתה מעל" מוציאים רק את המשתמש הנוכחי.
         */
        val otherTraineePercents =
            latestPercentByUid
                .filterKeys { uid ->
                    uid != currentUid
                }
                .values
                .toList()

        if (otherTraineePercents.isEmpty()) {
            return null
        }

        /*
         * הממוצע ומספר המתאמנים הם נתונים גלובליים:
         * כל המשתמשים בחגורה נלקחים מ-Firestore.
         *
         * אם מסמך המשתמש הנוכחי עדיין לא הגיע ל-query,
         * משתמשים זמנית באחוז המקומי שלו.
         */
        val allTraineePercents =
            if (latestPercentByUid.containsKey(currentUid)) {
                latestPercentByUid.values.toList()
            } else {
                latestPercentByUid.values.toList() +
                        safeUserPercent
            }

        val otherUsersCount =
            otherTraineePercents.size

        val displayedUsersCount =
            allTraineePercents.size

        val averageKnownPercent =
            allTraineePercents
                .average()
                .roundToInt()
                .coerceIn(0, 100)

        /*
         * "אתה מעל" עדיין מחושב רק מול מתאמנים אחרים,
         * כדי שהמשתמש לא ישווה את עצמו לעצמו.
         */
        val traineesBelowUser =
            otherTraineePercents.count { percent ->
                percent < safeUserPercent
            }

        val percentileAbove =
            (
                    traineesBelowUser.toFloat() /
                            otherUsersCount.toFloat() *
                            100f
                    )
                .roundToInt()
                .coerceIn(0, 100)

        return UserProgressComparison(
            beltId = cleanBeltId,
            usersCount = displayedUsersCount,
            userKnownPercent = safeUserPercent,
            averageKnownPercent = averageKnownPercent,
            percentileAbove = percentileAbove,
            hasEnoughData = true
        )
    }
}