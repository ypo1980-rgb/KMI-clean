package il.kmi.app.progress

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import il.kmi.app.DataStoreManager
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

            val collection = exerciseMarksCollection(uid)
            val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId

            val registration = collection
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        Log.w(
                            "KMI_EXERCISE_MARKS",
                            "Trainee listener failed: project=$projectId " +
                                    "path=${collection.path} " +
                                    "authMatches=${auth.currentUser?.uid == uid} " +
                                    "code=${error.code}"
                        )
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null || auth.currentUser?.uid != uid) {
                        return@addSnapshotListener
                    }

                    Log.d(
                        "KMI_EXERCISE_MARKS",
                        "Trainee snapshot: project=$projectId " +
                                "path=${collection.path} " +
                                "fromCache=${snapshot.metadata.isFromCache}"
                    )

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

    fun materialExerciseStatusIds(
        belt: Belt,
        topicTitle: String,
        topicKey: String,
        items: List<String>
    ): List<String> {
        val baseIds = items.mapIndexed { index, item ->
            val cleanTitle = il.kmi.app.domain.CanonicalIds
                .cleanItem(topicTitle, item)
                .trim()

            val identity = ExerciseIdentityRegistry.resolve(
                belt = belt,
                hebrewTitle = cleanTitle,
                topicKey = topicKey
            )

            if (identity.isKnown) identity.id else "${identity.id}_row_$index"
        }

        val counts = baseIds.groupingBy { it }.eachCount()

        return baseIds.mapIndexed { index, baseId ->
            if (counts.getValue(baseId) > 1) {
                "${baseId}__${topicKey}__row_$index"
            } else {
                baseId
            }
        }
    }

    suspend fun importLegacyMaterialMarks(
        context: Context,
        uid: String
    ): Int {
        val appContext = context.applicationContext
        val auth = FirebaseAuth.getInstance()

        check(auth.currentUser?.uid == uid)
        check(DataStoreManager(appContext).readLegacyMarksOwner() == uid)

        val preferences = appContext.getSharedPreferences(
            "kmi_settings",
            Context.MODE_PRIVATE
        )

        fun normalizePreferencePart(value: String): String =
            value
                .replace("\u200F", "")
                .replace("\u200E", "")
                .replace("\u00A0", " ")
                .trim()

        suspend fun importScope(
            belt: Belt,
            topicTitle: String,
            topicKey: String,
            rawItems: List<String>
        ): Int {
            val suffix = normalizePreferencePart(topicKey)
            val mastered = preferences
                .getStringSet("mastered_${belt.id}_$suffix", emptySet()).orEmpty()
            val partial = preferences
                .getStringSet("partially_known_${belt.id}_$suffix", emptySet()).orEmpty()
            val unknown = preferences
                .getStringSet("unknown_${belt.id}_$suffix", emptySet()).orEmpty()

            if (mastered.isEmpty() && partial.isEmpty() && unknown.isEmpty()) {
                return 0
            }

            val items = rawItems.map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            val ids = materialExerciseStatusIds(belt, topicTitle, topicKey, items)
            var imported = 0

            fun localMark(id: String): ExerciseMark? = when (id) {
                in mastered -> ExerciseMark.MASTERED
                in partial -> ExerciseMark.PARTIALLY_KNOWN
                in unknown -> ExerciseMark.UNKNOWN
                else -> null
            }

            for ((index, item) in items.withIndex()) {
                val statusId = ids[index]
                val cleanItem = item
                    .replace("\u200F", "")
                    .replace("\u200E", "")
                    .replace("\u00A0", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                val legacyId = "status_${belt.id}_${topicKey}_${index}_$cleanItem"
                val mark = localMark(statusId) ?: localMark(legacyId) ?: continue

                if (importExerciseMarkIfMissing(uid, belt, topicKey, statusId, mark)) {
                    imported++
                }
            }

            return imported
        }

        var imported = 0

        for ((belt, beltContent) in ContentRepo.data) {
            for (topic in beltContent.topics) {
                val root = topic.title.trim()
                if (root.isBlank()) continue

                imported += importScope(
                    belt = belt,
                    topicTitle = root,
                    topicKey = root,
                    rawItems = ContentRepo.getAllItemsFor(belt, root, null)
                )

                for (subTopic in topic.subTopics) {
                    val parent = subTopic.title.trim()
                    if (parent.isBlank()) continue

                    val nestedTitles = ContentRepo
                        .getNestedSubTopicTitles(belt, root, parent)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()

                    val parentKey = "${root}__${parent}"
                    val parentItems = if (nestedTitles.isEmpty()) {
                        ContentRepo.getAllItemsFor(belt, root, parent)
                    } else {
                        nestedTitles.flatMap { nested ->
                            ContentRepo.getNestedItemsFor(belt, root, parent, nested)
                        }
                    }

                    imported += importScope(belt, root, parentKey, parentItems)

                    for (nested in nestedTitles) {
                        imported += importScope(
                            belt = belt,
                            topicTitle = root,
                            topicKey = "${parentKey}__${nested}",
                            rawItems = ContentRepo.getNestedItemsFor(belt, root, parent, nested)
                        )
                    }
                }
            }
        }

        return imported
    }

    suspend fun importExerciseMarkIfMissing(
        uid: String,
        belt: Belt,
        canonicalTopicKey: String,
        canonicalExerciseId: String,
        mark: ExerciseMark
    ): Boolean {
        require(mark != ExerciseMark.UNMARKED)

        val auth = FirebaseAuth.getInstance()
        check(auth.currentUser?.uid == uid) {
            "Exercise mark account changed"
        }

        val documentId = exerciseMarkDocumentId(
            belt = belt,
            canonicalTopicKey = canonicalTopicKey,
            canonicalExerciseId = canonicalExerciseId
        )
        val document = exerciseMarksCollection(uid).document(documentId)

        return FirebaseFirestore.getInstance().runTransaction { transaction ->
            check(auth.currentUser?.uid == uid) {
                "Exercise mark account changed"
            }

            if (transaction.get(document).exists()) {
                return@runTransaction false
            }

            transaction.set(
                document,
                mapOf(
                    "beltId" to belt.id,
                    "status" to mark.firestoreValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            true
        }.await()
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