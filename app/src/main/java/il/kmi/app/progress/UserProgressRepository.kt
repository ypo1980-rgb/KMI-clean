package il.kmi.app.progress

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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
        val coachUid =
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid
                ?.trim()
                .orEmpty()

        val cleanBeltId =
            beltId.trim()

        if (
            coachUid.isBlank() ||
            cleanBeltId.isBlank()
        ) {
            return null
        }

        val firestore =
            FirebaseFirestore.getInstance()

        /*
         * נרמול שמות הסניפים והקבוצות.
         * כך מקף עברי, מקף רגיל ורווחים כפולים
         * לא גורמים לאיבוד מתאמנים בחישוב.
         */
        fun normalizeAssignment(
            value: String
        ): String {
            return value
                .trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .lowercase()
        }

        fun readAssignments(
            document:
            com.google.firebase.firestore.DocumentSnapshot,
            listFields: List<String>,
            singleFields: List<String>,
            csvFields: List<String>
        ): Set<String> {
            val values =
                mutableListOf<String>()

            listFields.forEach { fieldName ->
                (document.get(fieldName) as? List<*>)
                    ?.forEach { item ->
                        item
                            ?.toString()
                            ?.let(values::add)
                    }
            }

            singleFields.forEach { fieldName ->
                document.getString(fieldName)
                    ?.let(values::add)
            }

            csvFields.forEach { fieldName ->
                document.getString(fieldName)
                    ?.split(",")
                    ?.let(values::addAll)
            }

            return values
                .map(::normalizeAssignment)
                .filter { value ->
                    value.isNotBlank()
                }
                .toSet()
        }

        val coachDocument =
            firestore
                .collection("users")
                .document(coachUid)
                .get(Source.SERVER)
                .await()

        if (!coachDocument.exists()) {
            return null
        }

        val coachBranches =
            readAssignments(
                document = coachDocument,
                listFields = listOf(
                    "branches",
                    "selected_branches"
                ),
                singleFields = listOf(
                    "activeBranch",
                    "active_branch",
                    "branch",
                    "coachBranch",
                    "coach_branch",
                    "selected_branch",
                    "current_branch"
                ),
                csvFields = listOf(
                    "branchesCsv",
                    "branches_csv"
                )
            )

        val coachGroups =
            readAssignments(
                document = coachDocument,
                listFields = listOf(
                    "groups",
                    "selected_groups"
                ),
                singleFields = listOf(
                    "primaryGroup",
                    "activeGroup",
                    "active_group",
                    "groupKey",
                    "group_key",
                    "group",
                    "age_group",
                    "coachGroupKey",
                    "coach_groupKey",
                    "selected_groupKey",
                    "current_groupKey"
                ),
                csvFields = listOf(
                    "groupsCsv",
                    "groups_csv"
                )
            )

        /*
         * ללא שיוך לקבוצה אין דרך בטוחה לדעת
         * אילו מתאמנים שייכים למאמן.
         */
        if (coachGroups.isEmpty()) {
            return CoachGroupProgressSummary(
                beltId = cleanBeltId,
                groupsCount = 0,
                totalTrainees = 0,
                traineesWithProgress = 0,
                averageKnownPercent = 0
            )
        }

        val usersSnapshot =
            firestore
                .collection("users")
                .get(Source.SERVER)
                .await()

        val traineeUids =
            usersSnapshot.documents
                .mapNotNull { document ->
                    val role =
                        document.getString("role")
                            ?.trim()
                            ?.lowercase()
                            .orEmpty()

                    val isTrainee =
                        role == "trainee" ||
                                role.contains("trainee") ||
                                role.contains("מתאמן")

                    if (!isTrainee) {
                        return@mapNotNull null
                    }

                    val traineeBranches =
                        readAssignments(
                            document = document,
                            listFields = listOf(
                                "branches",
                                "selected_branches"
                            ),
                            singleFields = listOf(
                                "activeBranch",
                                "active_branch",
                                "branch",
                                "selected_branch",
                                "current_branch"
                            ),
                            csvFields = listOf(
                                "branchesCsv",
                                "branches_csv"
                            )
                        )

                    val traineeGroups =
                        readAssignments(
                            document = document,
                            listFields = listOf(
                                "groups",
                                "selected_groups"
                            ),
                            singleFields = listOf(
                                "primaryGroup",
                                "activeGroup",
                                "active_group",
                                "groupKey",
                                "group_key",
                                "group",
                                "age_group",
                                "selected_groupKey",
                                "current_groupKey"
                            ),
                            csvFields = listOf(
                                "groupsCsv",
                                "groups_csv"
                            )
                        )

                    val belongsToCoachGroup =
                        traineeGroups.any { group ->
                            group in coachGroups
                        }

                    /*
                     * כאשר למאמן קיימים סניפים,
                     * נדרש גם שיוך לסניף משותף.
                     *
                     * כאשר אין למאמן סניף שמור,
                     * השיוך לקבוצה מספיק.
                     */
                    val belongsToCoachBranch =
                        coachBranches.isEmpty() ||
                                traineeBranches.any { branch ->
                                    branch in coachBranches
                                }

                    if (
                        belongsToCoachGroup &&
                        belongsToCoachBranch
                    ) {
                        document.getString("uid")
                            ?.trim()
                            ?.takeIf { uid ->
                                uid.isNotBlank()
                            }
                            ?: document.id
                                .trim()
                                .takeIf { uid ->
                                    uid.isNotBlank()
                                }
                    } else {
                        null
                    }
                }
                .filter { uid ->
                    uid != coachUid
                }
                .distinct()

        if (traineeUids.isEmpty()) {
            return CoachGroupProgressSummary(
                beltId = cleanBeltId,
                groupsCount = coachGroups.size,
                totalTrainees = 0,
                traineesWithProgress = 0,
                averageKnownPercent = 0
            )
        }

        val progressSnapshot =
            firestore
                .collection("userProgress")
                .whereEqualTo(
                    "beltId",
                    cleanBeltId
                )
                .get(Source.SERVER)
                .await()

        /*
         * משתמשים רק במסמכים החדשים:
         *
         *     {uid}__{beltId}
         *
         * מסמכי legacy אינם נכנסים לחישוב.
         */
        val progressByUid =
            progressSnapshot.documents
                .mapNotNull { document ->
                    val uid =
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
                        "${uid}__${cleanBeltId}"

                    if (
                        uid in traineeUids &&
                        document.id == expectedDocumentId &&
                        totalCount > 0 &&
                        knownPercent in 0..100
                    ) {
                        uid to knownPercent
                    } else {
                        null
                    }
                }
                .toMap()

        val averageKnownPercent =
            progressByUid.values
                .takeIf { percentages ->
                    percentages.isNotEmpty()
                }
                ?.average()
                ?.roundToInt()
                ?.coerceIn(0, 100)
                ?: 0

        return CoachGroupProgressSummary(
            beltId = cleanBeltId,
            groupsCount = coachGroups.size,
            totalTrainees = traineeUids.size,
            traineesWithProgress = progressByUid.size,
            averageKnownPercent = averageKnownPercent
        )
    }
}