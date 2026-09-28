package il.kmi.app.progress

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
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

    /*
     * שורה פנימית המשמשת לחישוב התקדמות
     * של כל חומר החגורה.
     */
    private data class BeltProgressRow(
        val topicTitle: String,
        val statusTopicKey: String,
        val item: String,
        val indexInStatusGroup: Int
    )

    private fun normalizeProgressPart(
        value: String
    ): String {
        return value
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanProgressItem(
        topicTitle: String,
        item: String
    ): String {
        var clean =
            item.trim()

        if (
            topicTitle.isNotBlank() &&
            clean.startsWith(
                "$topicTitle::"
            )
        ) {
            clean =
                clean.removePrefix(
                    "$topicTitle::"
                )
                    .trim()
        }

        return normalizeProgressPart(
            clean
        )
    }

    private fun progressStatusIdFor(
        belt: Belt,
        row: BeltProgressRow
    ): String {
        val cleanItem =
            cleanProgressItem(
                topicTitle = row.topicTitle,
                item = row.item
            )

        val resolved =
            ExerciseIdentityRegistry.resolve(
                belt = belt,
                hebrewTitle = cleanItem,
                topicKey =
                    row.statusTopicKey
            )

        return if (resolved.isKnown) {
            resolved.id
        } else {
            "${resolved.id}_row_" +
                    row.indexInStatusGroup
        }
    }

    private fun legacyProgressStatusIdFor(
        belt: Belt,
        row: BeltProgressRow
    ): String {
        val cleanItem =
            normalizeProgressPart(
                row.item
            )

        return buildString {
            append("status_")
            append(belt.id)
            append("_")
            append(row.statusTopicKey)
            append("_")
            append(row.indexInStatusGroup)
            append("_")
            append(cleanItem)
        }
    }

    /*
     * מחשב ושומר את התקדמות המשתמש בכל החגורה.
     *
     * הפונקציה נקראת מיד לאחר שינוי סימון במסך
     * התרגילים, ולכן אין עוד תלות בפתיחת
     * SummaryScreen לצורך הופעה בהשוואה.
     */
    suspend fun syncCurrentUserBeltProgress(
        vm: KmiViewModel,
        belt: Belt
    ) {
        val uid =
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid
                ?.trim()
                .orEmpty()

        if (uid.isBlank()) {
            return
        }

        val beltContent =
            ContentRepo.data[belt]
                ?: return

        val rows =
            mutableListOf<BeltProgressRow>()

        beltContent.topics.forEach {
                topic ->

            val cleanTopicTitle =
                topic.title.trim()

            val directItems =
                topic.items
                    .map { item ->
                        item.trim()
                    }
                    .filter { item ->
                        item.isNotBlank()
                    }
                    .distinct()

            directItems.forEachIndexed {
                    index,
                    item ->

                rows +=
                    BeltProgressRow(
                        topicTitle =
                            cleanTopicTitle,
                        statusTopicKey =
                            cleanTopicTitle,
                        item = item,
                        indexInStatusGroup =
                            index
                    )
            }

            fun addSubTopic(
                subTopic:
                ContentRepo.SubTopic
            ) {
                val cleanSubTopicTitle =
                    subTopic.title.trim()

                val statusTopicKey =
                    "${cleanTopicTitle}__" +
                            cleanSubTopicTitle

                val subItems =
                    subTopic.items
                        .map { item ->
                            item.trim()
                        }
                        .filter { item ->
                            item.isNotBlank()
                        }
                        .distinct()

                subItems.forEachIndexed {
                        index,
                        item ->

                    rows +=
                        BeltProgressRow(
                            topicTitle =
                                cleanTopicTitle,
                            statusTopicKey =
                                statusTopicKey,
                            item = item,
                            indexInStatusGroup =
                                index
                        )
                }

                subTopic.subTopics.forEach {
                        nestedSubTopic ->
                    addSubTopic(
                        nestedSubTopic
                    )
                }
            }

            topic.subTopics.forEach {
                    subTopic ->
                addSubTopic(
                    subTopic
                )
            }
        }

        if (rows.isEmpty()) {
            return
        }

        /*
         * ממתינים עד שכל קבוצות הסימונים נטענו
         * לפני החישוב, בדיוק כמו ב-SummaryScreen.
         */
        val statusGroups =
            rows
                .groupBy { row ->
                    row.statusTopicKey
                }
                .mapValues {
                        (_, groupRows) ->

                    groupRows
                        .map { row ->
                            progressStatusIdFor(
                                belt = belt,
                                row = row
                            )
                        }
                        .distinct()
                }

        vm.warmUpStatusGroupsAndAwait(
            belt = belt,
            groups = statusGroups
        )

        val snapshots =
            rows
                .map { row ->
                    row.statusTopicKey
                }
                .distinct()
                .associateWith {
                        statusTopicKey ->

                    vm.getTopicStatusSnapshot(
                        belt,
                        statusTopicKey
                    )
                }

        var knownCount = 0

        rows.forEach { row ->
            val snapshot =
                snapshots[
                    row.statusTopicKey
                ]
                    .orEmpty()

            val statusId =
                progressStatusIdFor(
                    belt = belt,
                    row = row
                )

            val legacyStatusId =
                legacyProgressStatusIdFor(
                    belt = belt,
                    row = row
                )

            val value =
                snapshot[statusId]
                    ?: snapshot[
                        legacyStatusId
                    ]

            /*
             * רק „יודע” מלא נחשב ידיעה.
             * „יודע חלקית” נשמר כ-false ולכן
             * אינו מגדיל את knownCount.
             */
            if (value == true) {
                knownCount++
            }
        }

        val totalCount =
            rows.size

        val knownPercent =
            if (totalCount <= 0) {
                0
            } else {
                (
                        knownCount *
                                100f /
                                totalCount
                        )
                    .roundToInt()
                    .coerceIn(
                        0,
                        100
                    )
            }

        saveUserProgress(
            beltId = belt.id,
            knownPercent = knownPercent,
            knownCount = knownCount,
            totalCount = totalCount
        )
    }

    suspend fun loadBeltComparison(
        beltId: String,
        userKnownPercent: Int
    ): UserProgressComparison? {

        val currentUser =
            FirebaseAuth.getInstance()
                .currentUser
                ?: return null

        if (currentUser.isAnonymous) {
            return null
        }

        val cleanBeltId =
            beltId.trim()

        if (cleanBeltId.isBlank()) {
            return null
        }

        val result =
            FirebaseFunctions
                .getInstance()
                .getHttpsCallable(
                    "loadSecureBeltComparison"
                )
                .call(
                    mapOf(
                        "beltId" to cleanBeltId
                    )
                )
                .await()

        val payload =
            result.data as? Map<*, *>
                ?: return null

        val returnedBeltId =
            payload["beltId"]
                ?.toString()
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: cleanBeltId

        val usersCount =
            (payload["usersCount"] as? Number)
                ?.toInt()
                ?.coerceAtLeast(0)
                ?: 0

        val returnedUserKnownPercent =
            (payload["userKnownPercent"] as? Number)
                ?.toInt()
                ?.coerceIn(0, 100)
                ?: userKnownPercent.coerceIn(
                    0,
                    100
                )

        val averageKnownPercent =
            (payload["averageKnownPercent"] as? Number)
                ?.toInt()
                ?.coerceIn(0, 100)
                ?: 0

        val percentileAbove =
            (payload["percentileAbove"] as? Number)
                ?.toInt()
                ?.coerceIn(0, 100)
                ?: 0

        val hasEnoughData =
            payload["hasEnoughData"] as? Boolean
                ?: false

        return UserProgressComparison(
            beltId = returnedBeltId,
            usersCount = usersCount,
            userKnownPercent =
                returnedUserKnownPercent,
            averageKnownPercent =
                averageKnownPercent,
            percentileAbove =
                percentileAbove,
            hasEnoughData =
                hasEnoughData
        )
    }

    /*
     * טוען את נתוני ההתקדמות של כל המתאמנים
     * בכל הסניפים והקבוצות שאליהם המאמן משויך.
     *
     * מתאמן שמופיע ביותר מקבוצה אחת נספר פעם אחת
     * בלבד לפי ה-UID שלו.
     *
     * הממוצע מחושב רק מתוך מתאמנים שקיים עבורם
     * מסמך התקדמות תקין בחגורה המוצגת.
     */
    suspend fun loadCoachGroupsBeltProgress(
        beltId: String
    ): CoachGroupProgressSummary? {

        val currentUser =
            FirebaseAuth.getInstance()
                .currentUser
                ?: return null

        if (currentUser.isAnonymous) {
            return null
        }

        val cleanBeltId =
            beltId.trim()

        if (cleanBeltId.isBlank()) {
            return null
        }

        val result =
            FirebaseFunctions
                .getInstance()
                .getHttpsCallable(
                    "loadSecureCoachGroupsBeltProgress"
                )
                .call(
                    mapOf(
                        "beltId" to cleanBeltId
                    )
                )
                .await()

        val payload =
            result.data as? Map<*, *>
                ?: return null

        val returnedBeltId =
            payload["beltId"]
                ?.toString()
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: cleanBeltId

        val groupsCount =
            (payload["groupsCount"] as? Number)
                ?.toInt()
                ?.coerceAtLeast(0)
                ?: 0

        val totalTrainees =
            (payload["totalTrainees"] as? Number)
                ?.toInt()
                ?.coerceAtLeast(0)
                ?: 0

        val traineesWithProgress =
            (payload["traineesWithProgress"] as? Number)
                ?.toInt()
                ?.coerceAtLeast(0)
                ?: 0

        val averageKnownPercent =
            (payload["averageKnownPercent"] as? Number)
                ?.toInt()
                ?.coerceIn(0, 100)
                ?: 0

        return CoachGroupProgressSummary(
            beltId = returnedBeltId,
            groupsCount = groupsCount,
            totalTrainees = totalTrainees,
            traineesWithProgress = traineesWithProgress,
            averageKnownPercent = averageKnownPercent
        )
    }
}