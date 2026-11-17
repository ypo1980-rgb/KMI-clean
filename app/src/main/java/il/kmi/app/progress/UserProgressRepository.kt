package il.kmi.app.progress

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import android.util.Log
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import java.security.MessageDigest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object UserProgressRepository {

    enum class ExerciseMark(val firestoreValue: String) {
        MASTERED("mastered"),
        PARTIALLY_KNOWN("partiallyKnown"),
        UNKNOWN("unknown"),
        UNMARKED("unmarked")
    }

    data class CoachExerciseMark(
        val selectedStatuses: Set<String> = emptySet(),
        val updatedAtByStatus: Map<String, Long> = emptyMap()
    )

    private val selectableCoachStatuses = setOf(
        "taught",
        "practiced",
        "needs_reinforcement"
    )

    fun exerciseMarkDocumentId(
        belt: Belt,
        canonicalTopicKey: String,
        canonicalExerciseId: String
    ): String {
        require(canonicalTopicKey.isNotBlank())
        require(canonicalExerciseId.isNotBlank())
        require('\u0000' !in canonicalTopicKey && '\u0000' !in canonicalExerciseId)

        val key = "${belt.id}\u0000$canonicalTopicKey\u0000$canonicalExerciseId"
        return MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
    }

    private fun exerciseMarksCollection(uid: String) =
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("exerciseMarks")

    fun observeExerciseMarks(uid: String): Flow<Map<String, ExerciseMark>> =
        callbackFlow {
            val auth = FirebaseAuth.getInstance()
            check(auth.currentUser?.uid == uid) {
                "Exercise mark account changed"
            }

            val registration = exerciseMarksCollection(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null || auth.currentUser?.uid != uid) {
                        return@addSnapshotListener
                    }

                    val marks = snapshot.documents.mapNotNull { document ->
                        val status = document.getString("status")
                        ExerciseMark.entries
                            .firstOrNull { it.firestoreValue == status }
                            ?.let { document.id to it }
                    }.toMap()

                    trySend(marks)
                }

            awaitClose { registration.remove() }
        }

    suspend fun saveExerciseMark(
        uid: String,
        belt: Belt,
        canonicalTopicKey: String,
        canonicalExerciseId: String,
        mark: ExerciseMark
    ) {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid) {
            "Exercise mark account changed"
        }

        val documentId = exerciseMarkDocumentId(
            belt = belt,
            canonicalTopicKey = canonicalTopicKey,
            canonicalExerciseId = canonicalExerciseId
        )

        val document = exerciseMarksCollection(uid)
            .document(documentId)

        Log.d(
            "KMI_EXERCISE_MARKS",
            "Firestore write started: " +
                    "path=${document.path} " +
                    "belt=${belt.id} " +
                    "status=${mark.firestoreValue}"
        )

        document.set(
            mapOf(
                "beltId" to belt.id,
                "status" to mark.firestoreValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        Log.d(
            "KMI_EXERCISE_MARKS",
            "Firestore write completed: " +
                    "path=${document.path} " +
                    "status=${mark.firestoreValue}"
        )
    }

    private fun coachExerciseMarksCollection(uid: String) =
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("coachExerciseMarks")

    fun observeCoachExerciseMarks(
        uid: String
    ): Flow<Map<String, CoachExerciseMark>> = callbackFlow {

        val auth = FirebaseAuth.getInstance()

        check(auth.currentUser?.uid == uid) {
            "Coach exercise mark account changed"
        }

        val registration =
            coachExerciseMarksCollection(uid)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (
                        snapshot == null ||
                        auth.currentUser?.uid != uid
                    ) {
                        return@addSnapshotListener
                    }

                    val marks = snapshot.documents.associate { document ->

                        val selected =
                            (document.get("selectedStatuses") as? List<*>)
                                .orEmpty()
                                .mapNotNull { it as? String }
                                .filter { it in selectableCoachStatuses }
                                .take(2)
                                .toSet()

                        val rawDates =
                            document.get("updatedAtByStatus")
                                    as? Map<*, *>

                        val dates = rawDates
                            .orEmpty()
                            .mapNotNull { (key, value) ->

                                val status = key as? String
                                val timestamp = value as? Number

                                if (
                                    status in selected &&
                                    timestamp != null &&
                                    timestamp.toLong() > 0L
                                ) {
                                    status!! to timestamp.toLong()
                                } else {
                                    null
                                }
                            }
                            .toMap()

                        document.id to CoachExerciseMark(
                            selectedStatuses = selected,
                            updatedAtByStatus = dates
                        )
                    }

                    trySend(marks)
                }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun saveCoachExerciseMark(
        uid: String,
        belt: Belt,
        canonicalTopicKey: String,
        canonicalExerciseId: String,
        progress: CoachExerciseMark
    ) {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid) {
            "Coach exercise mark account changed"
        }

        require(
            progress.selectedStatuses.size <= 2 &&
                    progress.selectedStatuses.all {
                        it in selectableCoachStatuses
                    }
        ) {
            "Invalid coach exercise statuses"
        }

        val documentId = exerciseMarkDocumentId(
            belt = belt,
            canonicalTopicKey = canonicalTopicKey,
            canonicalExerciseId = canonicalExerciseId
        )

        val selected = progress.selectedStatuses

        val dates = progress.updatedAtByStatus
            .filterKeys { it in selected }
            .filterValues { it > 0L }

        coachExerciseMarksCollection(uid)
            .document(documentId)
            .set(
                mapOf(
                    "beltId" to belt.id,
                    "selectedStatuses" to selected.sorted(),
                    "updatedAtByStatus" to dates,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

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
                    .toInt()
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