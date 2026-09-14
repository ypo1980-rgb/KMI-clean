package il.kmi.app.attendance.data

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import il.kmi.app.training.TrainingOverrideRepository
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * אימון שהתקיים בפועל ונכלל בחישוב האישי.
 *
 * אם לא נמצאה למתאמן רשומה באימון שנשמר,
 * הסטטוס מוחזר כ־ABSENT.
 */
data class MemberAttendanceSession(
    val date: LocalDate,
    val status: AttendanceStatus
)

/**
 * היסטוריית הנוכחות של מתאמן ממועד הצטרפותו.
 */
data class MemberAttendanceHistory(
    val memberStartDate: LocalDate,
    val sessions: List<MemberAttendanceSession>
)

/**
 * צפי הגעה לאימון לפי הבחירות
 * שהמתאמנים סימנו בעצמם.
 */
data class TrainingAttendanceForecast(
    val comingCount: Int = 0,
    val notComingCount: Int = 0,
    val noResponseCount: Int = 0
)

class AttendanceRepository private constructor(
    private val app: Application
) {
    private val firestore = Firebase.firestore

    private val sessionPathById = mutableMapOf<Long, Pair<String, String>>()

    private fun currentCoachUidOrNull(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun currentCoachEmailOrNull(): String? {
        return FirebaseAuth.getInstance().currentUser?.email?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun groupDocId(branch: String, groupKey: String): String {
        return "g_${stablePositiveLong("${branch.trim()}|${groupKey.trim()}")}"
    }

    private suspend fun ensureGroupMetadata(branch: String, groupKey: String) {
        val cleanBranch = branch.trim()
        val cleanGroup = groupKey.trim()
        if (cleanBranch.isBlank() || cleanGroup.isBlank()) return

        val now = System.currentTimeMillis()
        val coachUid = currentCoachUidOrNull().orEmpty()
        val coachEmail = currentCoachEmailOrNull().orEmpty()

        val data = mapOf(
            "id" to groupDocId(cleanBranch, cleanGroup),
            "branch" to cleanBranch,
            "groupKey" to cleanGroup,
            "coachUid" to coachUid,
            "coachEmail" to coachEmail,
            "source" to "android_firestore_attendance",
            "createdAtMillis" to now,
            "updatedAtMillis" to now,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        groupRef(cleanBranch, cleanGroup)
            .set(data, SetOptions.merge())
            .await()
    }

    private fun sessionDocId(date: LocalDate): String {
        return date.toString()
    }

    private fun stablePositiveLong(raw: String): Long {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(raw.trim().lowercase().toByteArray())

        var value = 0L
        for (i in 0 until 8) {
            value = (value shl 8) or (bytes[i].toLong() and 0xFF)
        }

        val positive = value and Long.MAX_VALUE
        return if (positive == 0L) 1L else positive
    }

    private fun String.nameKey(): String = this
        .trim()
        .replace('־', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replace(Regex("\\s+"), " ")
        .replace(Regex("""[."'\u05F3\u05F4,;:()\[\]{}]"""), "")
        .lowercase()

    private fun groupRef(branch: String, groupKey: String) =
        firestore.collection("attendanceGroups")
            .document(groupDocId(branch, groupKey))

    private fun membersRef(branch: String, groupKey: String) =
        groupRef(branch, groupKey).collection("members")

    private fun sessionsRef(branch: String, groupKey: String) =
        groupRef(branch, groupKey).collection("sessions")

    private fun reportsRef(branch: String, groupKey: String) =
        groupRef(branch, groupKey).collection("reports")

    /**
     * רשימת מתאמנים לקבוצה — מקור אמת Firestore.
     */
    fun members(branch: String, groupKey: String): Flow<List<GroupMember>> = callbackFlow {
        if (branch.isBlank() || groupKey.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val groupId = groupDocId(branch, groupKey)

        val registration: ListenerRegistration = membersRef(branch, groupKey)
            .orderBy("displayName")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snap?.documents
                    ?.mapNotNull { doc ->
                        val name = doc.getString("displayName").orEmpty().trim()
                        if (name.isBlank()) return@mapNotNull null

                        GroupMember(
                            id = doc.getLong("id") ?: stablePositiveLong(doc.id),
                            branch = doc.getString("branch") ?: branch,
                            groupKey = doc.getString("groupKey") ?: groupKey,
                            displayName = name,
                            phone = doc.getString("phone"),
                            notes = doc.getString("notes")
                        )
                    }
                    ?: emptyList()

                trySend(list)
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * הוספת מתאמן ל־Firestore.
     * משתמשים ב־displayNameKey כדי למנוע כפילויות לפי שם.
     */
    suspend fun addMember(
        branch: String,
        groupKey: String,
        displayName: String,
        authUid: String? = null,
        phone: String? = null
    ): Long {
        val cleanName =
            displayName.trim()

        val cleanAuthUid =
            authUid
                ?.trim()
                .orEmpty()

        val cleanPhone =
            phone
                ?.filter { it.isDigit() }
                .orEmpty()

        if (
            branch.isBlank() ||
            groupKey.isBlank() ||
            cleanName.isBlank()
        ) {
            return 0L
        }

        ensureGroupMetadata(
            branch = branch,
            groupKey = groupKey
        )

        val key =
            cleanName.nameKey()

        /*
         * קודם מחפשים לפי Firebase UID.
         *
         * זהו מקור הזהות האמין ביותר של המתאמן
         * ואינו תלוי בשם התצוגה שלו.
         */
        val existingByUid =
            if (cleanAuthUid.isNotBlank()) {
                membersRef(
                    branch,
                    groupKey
                )
                    .whereEqualTo(
                        "authUid",
                        cleanAuthUid
                    )
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
            } else {
                null
            }

        /*
         * תאימות למשתמשים קיימים:
         * אם עדיין אין authUid במסמך ה-member,
         * ממשיכים לחפש לפי displayNameKey.
         */
        val existingByName =
            existingByUid
                ?: membersRef(
                    branch,
                    groupKey
                )
                    .whereEqualTo(
                        "displayNameKey",
                        key
                    )
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()

        if (existingByName != null) {
            val existingMemberId =
                existingByName.getLong("id")
                    ?: stablePositiveLong(
                        existingByName.id
                    )

            /*
             * משדרגים מסמך ישן ומחברים אותו
             * ל-Firebase UID בלי ליצור member נוסף.
             */
            val identityUpdate =
                mutableMapOf<String, Any>(
                    "updatedAtMillis" to
                            System.currentTimeMillis(),
                    "updatedAt" to
                            FieldValue.serverTimestamp()
                )

            if (cleanAuthUid.isNotBlank()) {
                identityUpdate["authUid"] =
                    cleanAuthUid
            }

            if (cleanPhone.isNotBlank()) {
                identityUpdate["phone"] =
                    cleanPhone
            }

            existingByName.reference
                .set(
                    identityUpdate,
                    SetOptions.merge()
                )
                .await()

            return existingMemberId
        }

        val memberId =
            stablePositiveLong(
                "$branch|$groupKey|$key"
            )

        val docRef =
            membersRef(
                branch,
                groupKey
            )
                .document(
                    memberId.toString()
                )

        val now =
            System.currentTimeMillis()

        val data =
            mutableMapOf<String, Any>(
                "id" to memberId,
                "branch" to branch.trim(),
                "groupKey" to groupKey.trim(),
                "displayName" to cleanName,
                "displayNameKey" to key,
                "coachUid" to
                        currentCoachUidOrNull()
                            .orEmpty(),
                "coachEmail" to
                        currentCoachEmailOrNull()
                            .orEmpty(),
                "source" to
                        "manual_or_bootstrap",
                "createdAtMillis" to now,
                "updatedAtMillis" to now,
                "updatedAt" to
                        FieldValue.serverTimestamp()
            )

        if (cleanAuthUid.isNotBlank()) {
            data["authUid"] =
                cleanAuthUid
        }

        if (cleanPhone.isNotBlank()) {
            data["phone"] =
                cleanPhone
        }

        docRef
            .set(
                data,
                SetOptions.merge()
            )
            .await()

        return memberId
    }

    /**
     * מחיקת מתאמן מהקבוצה.
     * מוחק גם סימוני נוכחות שלו מתוך כל השיעורים של הקבוצה.
     */
    suspend fun removeMember(branch: String, groupKey: String, memberId: Long) {
        if (branch.isBlank() || groupKey.isBlank()) return

        ensureGroupMetadata(branch, groupKey)

        membersRef(branch, groupKey)
            .document(memberId.toString())
            .delete()
            .await()

        val sessions = sessionsRef(branch, groupKey)
            .get()
            .await()
            .documents

        sessions.forEach { session ->
            runCatching {
                session.reference
                    .collection("records")
                    .document(memberId.toString())
                    .delete()
                    .await()
            }
        }
    }

    /**
     * מבטיח שתהיה ישיבת אימון ל־(תאריך+סניף+קבוצה) ומחזיר id יציב.
     */
    suspend fun ensureSession(
        date: LocalDate,
        branch: String,
        groupKey: String
    ): Long {
        if (branch.isBlank() || groupKey.isBlank()) return 0L

        ensureGroupMetadata(branch, groupKey)

        val sessionDocId = sessionDocId(date)
        val sessionId = stablePositiveLong("$branch|$groupKey|$sessionDocId")

        val groupId = groupDocId(branch, groupKey)
        sessionPathById[sessionId] = groupId to sessionDocId

        val now = System.currentTimeMillis()
        val sessionData = mapOf(
            "id" to sessionId,
            "date" to date.toString(),
            "branch" to branch.trim(),
            "groupKey" to groupKey.trim(),
            "coachUid" to currentCoachUidOrNull().orEmpty(),
            "coachEmail" to currentCoachEmailOrNull().orEmpty(),
            "status" to "open",
            "createdAtMillis" to now,
            "updatedAtMillis" to now,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        sessionsRef(branch, groupKey)
            .document(sessionDocId)
            .set(sessionData, SetOptions.merge())
            .await()

        return sessionId
    }

    /**
     * רשומות נוכחות ליום מסוים — Firestore.
     */
    fun attendanceForDay(
        branch: String,
        groupKey: String,
        date: LocalDate
    ): Flow<List<AttendanceRecord>> = callbackFlow {
        if (branch.isBlank() || groupKey.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val sessionDocId = sessionDocId(date)
        val sessionId = stablePositiveLong("$branch|$groupKey|$sessionDocId")
        sessionPathById[sessionId] = groupDocId(branch, groupKey) to sessionDocId

        val registration = sessionsRef(branch, groupKey)
            .document(sessionDocId)
            .collection("records")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val records = snap?.documents
                    ?.mapNotNull { doc ->
                        val statusRaw = doc.getString("status").orEmpty()
                        val status = runCatching {
                            AttendanceStatus.valueOf(statusRaw)
                        }.getOrDefault(AttendanceStatus.ABSENT)

                        val memberId = doc.getLong("memberId")
                            ?: doc.id.toLongOrNull()
                            ?: return@mapNotNull null

                        AttendanceRecord(
                            id = doc.getLong("id") ?: stablePositiveLong("${sessionId}|$memberId"),
                            sessionId = sessionId,
                            memberId = memberId,
                            status = status,
                            markedAtMillis = doc.getLong("markedAtMillis") ?: 0L
                        )
                    }
                    ?: emptyList()

                trySend(records)
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * צפי הגעה לאימון עבור כרטיס המאמן.
     *
     * סופרים רק בחירות שבוצעו על ידי המתאמנים עצמם:
     * markedBy == "trainee"
     *
     * PRESENT = מגיע
     * ABSENT = לא מגיע
     * ללא רשומה עצמית = טרם סימן
     */
    fun attendanceForecastForDay(
        branch: String,
        groupKey: String,
        date: LocalDate
    ): Flow<TrainingAttendanceForecast> = callbackFlow {

        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        if (
            cleanBranch.isBlank() ||
            cleanGroup.isBlank()
        ) {
            trySend(
                TrainingAttendanceForecast()
            )

            awaitClose { }
            return@callbackFlow
        }

        val sessionDocId =
            sessionDocId(date)

        /*
         * שומרים בנפרד את רשימת חברי הקבוצה
         * ואת סימוני הצפי האחרונים.
         *
         * כל שינוי באחד מהם מחשב מחדש את הצפי.
         */
        var memberIds: Set<Long> =
            emptySet()

        var traineeChoices:
                Map<Long, AttendanceStatus> =
            emptyMap()

        fun emitForecast() {

            if (memberIds.isEmpty()) {
                trySend(
                    TrainingAttendanceForecast()
                )
                return
            }

            val validChoices =
                traineeChoices.filterKeys { memberId ->
                    memberId in memberIds
                }

            val coming =
                validChoices.values.count { status ->
                    status == AttendanceStatus.PRESENT
                }

            val notComing =
                validChoices.values.count { status ->
                    status == AttendanceStatus.ABSENT
                }

            val responded =
                validChoices.keys.size

            val noResponse =
                (memberIds.size - responded)
                    .coerceAtLeast(0)

            trySend(
                TrainingAttendanceForecast(
                    comingCount = coming,
                    notComingCount = notComing,
                    noResponseCount = noResponse
                )
            )
        }

        val membersRegistration =
            membersRef(
                cleanBranch,
                cleanGroup
            )
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        return@addSnapshotListener
                    }

                    memberIds =
                        snapshot
                            ?.documents
                            .orEmpty()
                            .mapNotNull { document ->
                                document.getLong("id")
                                    ?: document.id.toLongOrNull()
                            }
                            .toSet()

                    emitForecast()
                }

        val recordsRegistration =
            sessionsRef(
                cleanBranch,
                cleanGroup
            )
                .document(
                    sessionDocId
                )
                .collection("records")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        return@addSnapshotListener
                    }

                    traineeChoices =
                        snapshot
                            ?.documents
                            .orEmpty()
                            .mapNotNull { document ->

                                /*
                                 * צפי מבוסס רק על בחירה
                                 * שהמתאמן ביצע בעצמו.
                                 */
                                if (
                                    document
                                        .getString("markedBy")
                                        ?.trim() != "trainee"
                                ) {
                                    return@mapNotNull null
                                }

                                val memberId =
                                    document.getLong(
                                        "memberId"
                                    )
                                        ?: document.id
                                            .toLongOrNull()
                                        ?: return@mapNotNull null

                                val status =
                                    when (
                                        document
                                            .getString("status")
                                            ?.trim()
                                    ) {
                                        AttendanceStatus
                                            .PRESENT
                                            .name ->
                                            AttendanceStatus.PRESENT

                                        AttendanceStatus
                                            .ABSENT
                                            .name ->
                                            AttendanceStatus.ABSENT

                                        else ->
                                            return@mapNotNull null
                                    }

                                memberId to status
                            }
                            .toMap()

                    emitForecast()
                }

        awaitClose {
            membersRegistration.remove()
            recordsRegistration.remove()
        }
    }

    /**
     * מחזיר את כל האימונים שהתקיימו בפועל עבור מתאמן
     * בטווח המבוקש, אך לעולם לא לפני מועד הצטרפותו.
     *
     * מקור האמת לאימון שהתקיים הוא reports:
     * רק לאחר שמירת הנוכחות נוצר דו"ח לאותו תאריך.
     *
     * אם קיים דו"ח אך אין למתאמן רשומה באותו אימון,
     * הוא נחשב כמי שלא הגיע.
     */
    suspend fun memberAttendanceHistory(
        branch: String,
        groupKey: String,
        memberId: Long,
        requestedFrom: LocalDate,
        to: LocalDate
    ): MemberAttendanceHistory {
        if (
            branch.isBlank() ||
            groupKey.isBlank() ||
            memberId <= 0L ||
            requestedFrom.isAfter(to)
        ) {
            return MemberAttendanceHistory(
                memberStartDate = requestedFrom,
                sessions = emptyList()
            )
        }

        /*
         * sessions הוא מקור האמת לכל האימונים הקיימים.
         * reports אינו מתאים לכך, משום שלא לכל אימון
         * היסטורי בהכרח נשמר דו"ח.
         */
        val sessionDocuments =
            sessionsRef(
                branch = branch,
                groupKey = groupKey
            )
                .whereGreaterThanOrEqualTo(
                    "date",
                    requestedFrom.toString()
                )
                .whereLessThanOrEqualTo(
                    "date",
                    to.toString()
                )
                .get()
                .await()
                .documents

        data class SessionWithExplicitRecord(
            val date: LocalDate,
            val explicitStatus: AttendanceStatus?
        )

        val sessionsWithRecords =
            sessionDocuments
                .mapNotNull { sessionDocument ->
                    val date =
                        sessionDocument
                            .getString("date")
                            ?.let { rawDate ->
                                runCatching {
                                    LocalDate.parse(rawDate)
                                }.getOrNull()
                            }
                            ?: runCatching {
                                LocalDate.parse(sessionDocument.id)
                            }.getOrNull()
                            ?: return@mapNotNull null

                    val recordDocument =
                        sessionDocument.reference
                            .collection("records")
                            .document(memberId.toString())
                            .get()
                            .await()

                    val explicitStatus =
                        if (recordDocument.exists()) {
                            recordDocument
                                .getString("status")
                                ?.let { rawStatus ->
                                    runCatching {
                                        AttendanceStatus.valueOf(
                                            rawStatus
                                        )
                                    }.getOrNull()
                                }
                                ?: AttendanceStatus.ABSENT
                        } else {
                            null
                        }

                    SessionWithExplicitRecord(
                        date = date,
                        explicitStatus = explicitStatus
                    )
                }
                .distinctBy { it.date }
                .sortedBy { it.date }

        val memberDocument =
            membersRef(
                branch = branch,
                groupKey = groupKey
            )
                .document(memberId.toString())
                .get()
                .await()

        val documentStartDate =
            memberDocument
                .getLong("createdAtMillis")
                ?.takeIf { it > 0L }
                ?.let { createdAtMillis ->
                    Instant
                        .ofEpochMilli(createdAtMillis)
                        .atZone(
                            ZoneId.of("Asia/Jerusalem")
                        )
                        .toLocalDate()
                }

        /*
         * נתונים שיובאו למערכת עשויים להיות ישנים
         * מ-createdAtMillis. במקרה כזה הרשומה ההיסטורית
         * הראשונה של המתאמן היא מועד ההתחלה האמין יותר.
         */
        val earliestRecordedDate =
            sessionsWithRecords
                .firstOrNull {
                    it.explicitStatus != null
                }
                ?.date

        val detectedStartDate =
            listOfNotNull(
                documentStartDate,
                earliestRecordedDate
            )
                .minOrNull()
                ?: requestedFrom

        val memberStartDate =
            if (detectedStartDate.isBefore(requestedFrom)) {
                requestedFrom
            } else {
                detectedStartDate
            }

        /*
         * מהאימון הראשון של המתאמן:
         * רשומה מפורשת נשמרת כמות שהיא;
         * היעדר רשומה באימון קיים נחשב ABSENT.
         */
        val sessions =
            sessionsWithRecords
                .asSequence()
                .filter { session ->
                    !session.date.isBefore(memberStartDate)
                }
                .map { session ->
                    MemberAttendanceSession(
                        date = session.date,
                        status =
                            session.explicitStatus
                                ?: AttendanceStatus.ABSENT
                    )
                }
                .sortedByDescending { it.date }
                .toList()

        return MemberAttendanceHistory(
            memberStartDate = memberStartDate,
            sessions = sessions
        )
    }

    /**
     * מחזיר את memberId של המשתמש המחובר
     * בקבוצה המסוימת לפי Firebase UID.
     *
     * אם עדיין לא קיים member מקושר,
     * יוצר / מקשר אותו באופן אוטומטי
     * מתוך פרופיל המשתמש.
     */
    suspend fun findMemberIdByAuthUid(
        branch: String,
        groupKey: String,
        authUid: String
    ): Long? {
        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        val cleanUid =
            authUid.trim()

        if (
            cleanBranch.isBlank() ||
            cleanGroup.isBlank() ||
            cleanUid.isBlank()
        ) {
            return null
        }

        /*
         * קודם מנסים למצוא member שכבר
         * מקושר ל-Firebase UID.
         */
        val existingMember =
            membersRef(
                cleanBranch,
                cleanGroup
            )
                .whereEqualTo(
                    "authUid",
                    cleanUid
                )
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

        if (existingMember != null) {
            return existingMember.getLong("id")
                ?: existingMember.id.toLongOrNull()
                ?: stablePositiveLong(
                    existingMember.id
                )
        }

        /*
         * אין עדיין member מקושר.
         * קוראים את פרופיל המשתמש עצמו.
         */
        val userDoc =
            firestore
                .collection("users")
                .document(cleanUid)
                .get()
                .await()

        if (!userDoc.exists()) {
            return null
        }

        val fullName =
            (
                    userDoc.getString("fullName")
                        ?: userDoc.getString("name")
                        ?: userDoc.getString("displayName")
                    )
                ?.trim()
                .orEmpty()
                .ifBlank {
                    listOf(
                        userDoc.getString("firstName")
                            ?.trim()
                            .orEmpty(),
                        userDoc.getString("lastName")
                            ?.trim()
                            .orEmpty()
                    )
                        .filter {
                            it.isNotBlank()
                        }
                        .joinToString(" ")
                        .trim()
                }

        if (fullName.isBlank()) {
            return null
        }

        val displayNameKey =
            fullName.nameKey()

        if (displayNameKey.isBlank()) {
            return null
        }

        /*
         * אותו memberId דטרמיניסטי שבו משתמש
         * גם מנגנון הנוכחות הקיים.
         */
        val memberId =
            stablePositiveLong(
                "$cleanBranch|$cleanGroup|$displayNameKey"
            )

        val phone =
            (
                    userDoc.getString("phone")
                        ?: userDoc.getString("phoneNumber")
                        ?: userDoc.getString("phone_number")
                        ?: userDoc.getString("mobile")
                    )
                ?.filter { it.isDigit() }
                .orEmpty()

        val now =
            System.currentTimeMillis()

        /*
         * חשוב:
         * לא קוראים כאן למסמך member לפי id לפני הכתיבה.
         *
         * אם הוא חדש — Firestore יבצע create.
         * אם הוא כבר קיים כ-member ישן — merge יחבר אליו
         * את ה-authUid בלי למחוק את הנתונים הקיימים.
         */
        val memberData =
            mutableMapOf<String, Any>(
                "id" to memberId,
                "branch" to cleanBranch,
                "groupKey" to cleanGroup,
                "displayName" to fullName,
                "displayNameKey" to displayNameKey,
                "authUid" to cleanUid,
                "updatedAtMillis" to now,
                "updatedAt" to
                        FieldValue.serverTimestamp()
            )

        if (phone.isNotBlank()) {
            memberData["phone"] =
                phone
        }

        membersRef(
            cleanBranch,
            cleanGroup
        )
            .document(
                memberId.toString()
            )
            .set(
                memberData,
                SetOptions.merge()
            )
            .await()

        return memberId
    }

    /**
     * טוען את הבחירה הקיימת של המתאמן
     * עבור אימון מסוים.
     *
     * null = עדיין לא נבחר מגיע / לא מגיע.
     */
    suspend fun getTraineeOwnAttendance(
        branch: String,
        groupKey: String,
        date: LocalDate,
        authUid: String
    ): AttendanceStatus? {
        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        val cleanUid =
            authUid.trim()

        if (
            cleanBranch.isBlank() ||
            cleanGroup.isBlank() ||
            cleanUid.isBlank()
        ) {
            return null
        }

        val memberId =
            findMemberIdByAuthUid(
                branch = cleanBranch,
                groupKey = cleanGroup,
                authUid = cleanUid
            )
                ?: return null

        val recordDoc =
            sessionsRef(
                cleanBranch,
                cleanGroup
            )
                .document(
                    sessionDocId(date)
                )
                .collection("records")
                .document(memberId.toString())
                .get()
                .await()

        if (!recordDoc.exists()) {
            return null
        }

        val statusRaw =
            recordDoc
                .getString("status")
                ?.trim()
                .orEmpty()

        return runCatching {
            AttendanceStatus.valueOf(statusRaw)
        }.getOrNull()
    }

    /**
     * סימון עצמי של המתאמן לפני תחילת האימון.
     *
     * הרשומה נכתבת לאותו records/{memberId}
     * שמסך המאמן כבר מאזין אליו.
     */
    suspend fun markTraineeOwnAttendance(
        branch: String,
        groupKey: String,
        date: LocalDate,
        memberId: Long,
        authUid: String,
        status: AttendanceStatus,
        trainingStartMillis: Long,
        occurrenceKey: String = ""
    ) {
        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        val cleanUid =
            authUid.trim()

        val cleanOccurrenceKey =
            occurrenceKey.trim()

        if (
            cleanBranch.isBlank() ||
            cleanGroup.isBlank() ||
            cleanUid.isBlank() ||
            memberId <= 0L
        ) {
            error("Invalid trainee attendance context")
        }

        val now =
            System.currentTimeMillis()

        /*
         * המתאמן יכול לשנות את הבחירה
         * רק עד שעת תחילת האימון.
         */
        if (now >= trainingStartMillis) {
            error("Training has already started")
        }

        /*
 * מאמתים שה-memberId שהגיע מה-UI
 * הוא בדיוק ה-memberId של המשתמש המחובר.
 *
 * הבדיקה עובדת גם אם מסמך members
 * עדיין לא נוצר על ידי המאמן.
 */
        val resolvedMemberId =
            findMemberIdByAuthUid(
                branch = cleanBranch,
                groupKey = cleanGroup,
                authUid = cleanUid
            )
                ?: error(
                    "Attendance member could not be resolved"
                )

        if (resolvedMemberId != memberId) {
            error(
                "Attendance member does not belong to current user"
            )
        }

        val sessionDocId =
            sessionDocId(date)

        val sessionId =
            stablePositiveLong(
                "$cleanBranch|$cleanGroup|$sessionDocId"
            )

        val recordId =
            stablePositiveLong(
                "$sessionId|$memberId"
            )

        /*
         * occurrenceId הוא מזהה המסמך המאומת
         * בתוך trainingOccurrences.
         *
         * משתמשים באותו מנגנון SHA-256 הגלובלי
         * שכבר משמש את TrainingOverrideRepository.
         */
        val occurrenceId =
            cleanOccurrenceKey
                .takeIf {
                    it.isNotBlank()
                }
                ?.let { key ->
                    TrainingOverrideRepository
                        .documentIdForOccurrenceKey(
                            key
                        )
                }
                .orEmpty()

        val data =
            mutableMapOf<String, Any>(
                "id" to recordId,
                "sessionId" to sessionId,
                "memberId" to memberId,
                "status" to status.name,

                // מי ביצע את הסימון
                "traineeUid" to cleanUid,
                "markedBy" to "trainee",

                /*
                 * נשאר זמנית לצורכי תאימות ואבחון.
                 * לאחר חיבור Rules מלא הוא כבר לא יהיה
                 * מקור האמת לקביעת שעת הנעילה.
                 */
                "trainingStartMillis" to
                        trainingStartMillis,

                "markedAtMillis" to now,
                "updatedAtMillis" to now,
                "updatedAt" to
                        FieldValue.serverTimestamp()
            )

        /*
         * בשלב הבא HomeScreen יעביר occurrenceKey
         * לכל סימון של המתאמן.
         *
         * עד אז לא שוברים את ההתנהגות הקיימת.
         */
        if (
            cleanOccurrenceKey.isNotBlank() &&
            occurrenceId.isNotBlank()
        ) {
            data["occurrenceKey"] =
                cleanOccurrenceKey

            data["occurrenceId"] =
                occurrenceId
        }

        sessionsRef(
            cleanBranch,
            cleanGroup
        )
            .document(sessionDocId)
            .collection("records")
            .document(memberId.toString())
            .set(
                data,
                SetOptions.merge()
            )
            .await()
    }

    /**
     * סימון נוכחות.
     * Firestore הוא מקור האמת.
     */
    suspend fun mark(
        sessionId: Long,
        memberId: Long,
        status: AttendanceStatus
    ) {
        val path = sessionPathById[sessionId]
            ?: error("Missing Firestore session path for sessionId=$sessionId. ensureSession() must run before mark().")

        val (groupDocId, sessionDocId) = path
        val ts = System.currentTimeMillis()
        val recordId = stablePositiveLong("$sessionId|$memberId")

        val data = mapOf(
            "id" to recordId,
            "sessionId" to sessionId,
            "memberId" to memberId,
            "status" to status.name,
            "coachUid" to currentCoachUidOrNull().orEmpty(),
            "coachEmail" to currentCoachEmailOrNull().orEmpty(),
            "markedAtMillis" to ts,
            "updatedAtMillis" to ts,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("attendanceGroups")
            .document(groupDocId)
            .collection("sessions")
            .document(sessionDocId)
            .collection("records")
            .document(memberId.toString())
            .set(data, SetOptions.merge())
            .await()
    }

    /**
     * ביטול סימון נוכחות למתאמן.
     * מוחק את הרשומה של אותו מתאמן מהשיעור הנוכחי.
     */
    suspend fun clearMark(
        sessionId: Long,
        memberId: Long
    ) {
        val path = sessionPathById[sessionId]
            ?: error("Missing Firestore session path for sessionId=$sessionId. ensureSession() must run before clearMark().")

        val (groupDocId, sessionDocId) = path

        firestore.collection("attendanceGroups")
            .document(groupDocId)
            .collection("sessions")
            .document(sessionDocId)
            .collection("records")
            .document(memberId.toString())
            .delete()
            .await()
    }

    /**
     * סטטיסטיקות נוכחות לפי טווח תאריכים.
     * מחשוב מתוך Firestore.
     */
    fun stats(
        branch: String,
        groupKey: String,
        from: LocalDate,
        to: LocalDate
    ): Flow<List<MemberPresenceStat>> = flow {
        if (branch.isBlank() || groupKey.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val members = members(branch, groupKey).first()

        if (members.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val sessions = sessionsRef(branch, groupKey)
            .whereGreaterThanOrEqualTo("date", from.toString())
            .whereLessThanOrEqualTo("date", to.toString())
            .get()
            .await()
            .documents

        val presentByMember = mutableMapOf<Long, Int>()
        val markedByMember = mutableMapOf<Long, Int>()

        sessions.forEach { session ->
            val records = session.reference
                .collection("records")
                .get()
                .await()
                .documents

            records.forEach { doc ->
                val memberId = doc.getLong("memberId")
                    ?: doc.id.toLongOrNull()
                    ?: return@forEach

                val statusRaw = doc.getString("status").orEmpty()
                val status = runCatching {
                    AttendanceStatus.valueOf(statusRaw)
                }.getOrDefault(AttendanceStatus.ABSENT)

                markedByMember[memberId] = (markedByMember[memberId] ?: 0) + 1

                if (status == AttendanceStatus.PRESENT) {
                    presentByMember[memberId] = (presentByMember[memberId] ?: 0) + 1
                }
            }
        }

        val result = members.map { member ->
            val marked = markedByMember[member.id] ?: 0
            val present = presentByMember[member.id] ?: 0

            MemberPresenceStat(
                memberId = member.id,
                presenceRatio = if (marked > 0) present.toDouble() / marked.toDouble() else null
            )
        }.sortedByDescending { it.presenceRatio ?: -1.0 }

        emit(result)
    }

    /**
     * בודק אם כבר קיים דוח שמור עבור
     * התאריך, הסניף והקבוצה שנבחרו.
     *
     * עצם קיומם של סימוני נוכחות אינו מספיק,
     * משום שייתכן שהמאמן התחיל לסמן אך עדיין
     * לא שמר את הדוח.
     */
    suspend fun hasSavedReportForDate(
        branch: String,
        groupKey: String,
        date: LocalDate
    ): Boolean {
        val cleanBranch = branch.trim()
        val cleanGroupKey = groupKey.trim()

        if (
            cleanBranch.isBlank() ||
            cleanGroupKey.isBlank()
        ) {
            return false
        }

        return reportsRef(
            cleanBranch,
            cleanGroupKey
        )
            .whereEqualTo(
                "date",
                date.toString()
            )
            .limit(1)
            .get()
            .await()
            .documents
            .isNotEmpty()
    }

    /**
     * מחשב דו"ח נוכחות ליום מסוים ושומר אותו ב־Firestore.
     */
    suspend fun saveReportForDate(
        branch: String,
        groupKey: String,
        date: LocalDate
    ) {
        ensureGroupMetadata(branch, groupKey)

        val sessionId = ensureSession(date, branch, groupKey)

        val members = members(branch, groupKey).first()
        val records = attendanceForDay(branch, groupKey, date).first()
        val byMember = records.associateBy { it.memberId }

        var present = 0
        var excused = 0
        var absent = 0

        members.forEach { m ->
            val r = byMember[m.id]
            when (r?.status) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.EXCUSED -> excused++
                AttendanceStatus.ABSENT, null -> absent++
            }
        }

        val total = present + excused + absent
        val percent = if (total > 0) (present * 100.0 / total).toInt() else 0
        val now = System.currentTimeMillis()
        val reportId = stablePositiveLong("$branch|$groupKey|${date}|$sessionId")

        val report = AttendanceReport(
            id = reportId,
            branch = branch,
            groupKey = groupKey,
            date = date,
            sessionId = sessionId,
            totalMembers = total,
            presentCount = present,
            excusedCount = excused,
            absentCount = absent,
            percentPresent = percent,
            createdAtMillis = now
        )

        val data = mapOf(
            "id" to report.id,
            "branch" to report.branch,
            "groupKey" to report.groupKey,
            "date" to report.date.toString(),
            "sessionId" to report.sessionId,
            "coachUid" to currentCoachUidOrNull().orEmpty(),
            "coachEmail" to currentCoachEmailOrNull().orEmpty(),
            "totalMembers" to report.totalMembers,
            "presentCount" to report.presentCount,
            "excusedCount" to report.excusedCount,
            "absentCount" to report.absentCount,
            "percentPresent" to report.percentPresent,
            "createdAtMillis" to report.createdAtMillis,
            "updatedAtMillis" to now,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        reportsRef(branch, groupKey)
            .document(reportId.toString())
            .set(data, SetOptions.merge())
            .await()

        deleteReportsOlderThanOneYear(branch, groupKey)
    }

    private suspend fun deleteReportsOlderThanOneYear(branch: String, groupKey: String) {
        val oneYearAgo = System.currentTimeMillis() - 365L * 24L * 60L * 60L * 1000L

        val oldReports = reportsRef(branch, groupKey)
            .whereLessThan("createdAtMillis", oneYearAgo)
            .get()
            .await()
            .documents

        oldReports.forEach { doc ->
            doc.reference.delete().await()
        }
    }

    suspend fun deleteReportsByIds(
        branch: String,
        groupKey: String,
        reportIds: List<Long>
    ): Int {
        if (reportIds.isEmpty()) return 0

        var deleted = 0

        reportIds.forEach { id ->
            runCatching {
                reportsRef(branch, groupKey)
                    .document(id.toString())
                    .delete()
                    .await()
                deleted++
            }
        }

        return deleted
    }

    fun reportsLastYear(branch: String, groupKey: String): Flow<List<AttendanceReport>> {
        val oneYearAgo = System.currentTimeMillis() - 365L * 24L * 60L * 60L * 1000L

        return reportsFlow(
            branch = branch,
            groupKey = groupKey,
            fromMillis = oneYearAgo,
            limit = null
        )
    }

    fun lastReports(
        branch: String,
        groupKey: String,
        limit: Int = 5
    ): Flow<List<AttendanceReport>> {
        return reportsFlow(
            branch = branch,
            groupKey = groupKey,
            fromMillis = null,
            limit = limit
        )
    }

    private fun reportsFlow(
        branch: String,
        groupKey: String,
        fromMillis: Long?,
        limit: Int?
    ): Flow<List<AttendanceReport>> = callbackFlow {
        if (branch.isBlank() || groupKey.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var query: Query = reportsRef(branch, groupKey)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)

        if (fromMillis != null) {
            query = query.whereGreaterThanOrEqualTo("createdAtMillis", fromMillis)
        }

        if (limit != null) {
            query = query.limit(limit.toLong())
        }

        val registration = query.addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val reports = snap?.documents
                ?.mapNotNull { doc ->
                    val dateString = doc.getString("date") ?: return@mapNotNull null
                    val date = runCatching { LocalDate.parse(dateString) }.getOrNull()
                        ?: return@mapNotNull null

                    AttendanceReport(
                        id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: stablePositiveLong(doc.id),
                        branch = doc.getString("branch") ?: branch,
                        groupKey = doc.getString("groupKey") ?: groupKey,
                        date = date,
                        sessionId = doc.getLong("sessionId") ?: 0L,
                        totalMembers = (doc.getLong("totalMembers") ?: 0L).toInt(),
                        presentCount = (doc.getLong("presentCount") ?: 0L).toInt(),
                        excusedCount = (doc.getLong("excusedCount") ?: 0L).toInt(),
                        absentCount = (doc.getLong("absentCount") ?: 0L).toInt(),
                        percentPresent = (doc.getLong("percentPresent") ?: 0L).toInt(),
                        createdAtMillis = doc.getLong("createdAtMillis") ?: 0L
                    )
                }
                ?: emptyList()

            trySend(reports)
        }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun clearReports(branch: String, groupKey: String): Int {
        val docs = reportsRef(branch, groupKey)
            .get()
            .await()
            .documents

        docs.forEach { it.reference.delete().await() }

        return docs.size
    }

    suspend fun resetAttendanceForGroup(branch: String, groupKey: String) {
        ensureGroupMetadata(branch, groupKey)

        val sessions = sessionsRef(branch, groupKey)
            .get()
            .await()
            .documents

        sessions.forEach { session ->
            val records = session.reference
                .collection("records")
                .get()
                .await()
                .documents

            records.forEach { it.reference.delete().await() }
            session.reference.delete().await()
        }

        clearReports(branch, groupKey)
    }

    companion object {
        @Volatile
        private var INSTANCE: AttendanceRepository? = null

        fun get(app: Application): AttendanceRepository =
            INSTANCE ?: synchronized(this) {
                AttendanceRepository(app).also { INSTANCE = it }
            }
    }
}