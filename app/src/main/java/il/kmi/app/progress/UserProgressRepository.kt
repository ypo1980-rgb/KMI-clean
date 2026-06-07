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
        val cleanBeltId = beltId.trim()
        if (cleanBeltId.isBlank()) return null

        val snap = FirebaseFirestore.getInstance()
            .collection("beltStats")
            .document(cleanBeltId)
            .get()
            .await()

        if (!snap.exists()) return null

        fun intField(name: String): Int {
            return when (val value = snap.get(name)) {
                is Long -> value.toInt()
                is Int -> value
                is Double -> value.toInt()
                is Number -> value.toInt()
                else -> 0
            }.coerceAtLeast(0)
        }

        val usersCount = intField("usersCount")
        val averageKnownPercent = intField("averageKnownPercent")
            .coerceIn(0, 100)

        val safeUserPercent = userKnownPercent.coerceIn(0, 100)
        val userBucket = bucketForPercent(safeUserPercent)

        val lowerUsersCount = when (userBucket) {
            0 -> 0
            10 -> intField("bucket_0_10")
            20 -> intField("bucket_0_10") +
                    intField("bucket_10_20")
            30 -> intField("bucket_0_10") +
                    intField("bucket_10_20") +
                    intField("bucket_20_30")
            40 -> intField("bucket_0_10") +
                    intField("bucket_10_20") +
                    intField("bucket_20_30") +
                    intField("bucket_30_40")
            50 -> intField("bucket_0_10") +
                    intField("bucket_10_20") +
                    intField("bucket_20_30") +
                    intField("bucket_30_40") +
                    intField("bucket_40_50")
            60 -> intField("bucket_0_10") +
                    intField("bucket_10_20") +
                    intField("bucket_20_30") +
                    intField("bucket_30_40") +
                    intField("bucket_40_50") +
                    intField("bucket_50_60")
            70 -> intField("bucket_0_10") +
                    intField("bucket_10_20") +
                    intField("bucket_20_30") +
                    intField("bucket_30_40") +
                    intField("bucket_40_50") +
                    intField("bucket_50_60") +
                    intField("bucket_60_70")
            80 -> intField("bucket_0_10") +
                    intField("bucket_10_20") +
                    intField("bucket_20_30") +
                    intField("bucket_30_40") +
                    intField("bucket_40_50") +
                    intField("bucket_50_60") +
                    intField("bucket_60_70") +
                    intField("bucket_70_80")
            90, 100 -> intField("bucket_0_10") +
                    intField("bucket_10_20") +
                    intField("bucket_20_30") +
                    intField("bucket_30_40") +
                    intField("bucket_40_50") +
                    intField("bucket_50_60") +
                    intField("bucket_60_70") +
                    intField("bucket_70_80") +
                    intField("bucket_80_90")
            else -> 0
        }.coerceAtLeast(0)

        val percentileAbove = if (usersCount <= 0) {
            0
        } else {
            ((lowerUsersCount.toFloat() / usersCount.toFloat()) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }

        return UserProgressComparison(
            beltId = cleanBeltId,
            usersCount = usersCount,
            userKnownPercent = safeUserPercent,
            averageKnownPercent = averageKnownPercent,
            percentileAbove = percentileAbove,
            hasEnoughData = usersCount >= 5
        )
    }
}