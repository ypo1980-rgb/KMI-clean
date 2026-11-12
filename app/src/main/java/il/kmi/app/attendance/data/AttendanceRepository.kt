package il.kmi.app.attendance.data

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.app.training.TrainingOverrideRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

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
    val noResponseCount: Int = 0,
    val comingNames: List<String> = emptyList(),
    val notComingNames: List<String> = emptyList(),
    val noResponseNames: List<String> = emptyList()
)

class AttendanceRepository private constructor(
    @Suppress("unused")
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

    private fun normalizeAttendanceIdentity(
        value: String
    ): String {
        return value
            .trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    private fun groupDocId(
        branch: String,
        groupKey: String
    ): String {

        val normalizedBranch =
            normalizeAttendanceIdentity(branch)

        val normalizedGroup =
            normalizeAttendanceIdentity(groupKey)

        return "g_${
            stablePositiveLong(
                "$normalizedBranch|$normalizedGroup"
            )
        }"
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

        val groupDocument =
            groupRef(cleanBranch, cleanGroup)

        firestore.runTransaction { transaction ->
            val existingGroup =
                transaction.get(groupDocument)

            if (!existingGroup.exists()) {
                transaction.set(
                    groupDocument,
                    data
                )
            }

            true
        }.await()
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

    /**
     * בדיקה לקריאה בלבד:
     * האם קיימים מתאמנים משויכים לקבוצה.
     *
     * אין יצירת קבוצה, שינוי נתונים או כתיבה ל-Firestore.
     *
     * שגיאת רשת אינה מתורגמת לקבוצה ריקה.
     */
    suspend fun hasAssignedMembers(
        branch: String,
        groupKey: String
    ): Boolean {

        val cleanBranch = branch.trim()
        val cleanGroup = groupKey.trim()

        if (
            cleanBranch.isBlank() ||
            cleanGroup.isBlank()
        ) {
            return false
        }

        val snapshot =
            membersRef(
                cleanBranch,
                cleanGroup
            )
                .get()
                .await()

        return snapshot.documents.any { document ->
            document.getBoolean("isActive") != false &&
                    document.getString("displayName")
                        ?.trim()
                        ?.isNotBlank() == true
        }
    }

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

        val registration: ListenerRegistration = membersRef(branch, groupKey)
            .orderBy("displayName")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snap?.documents
                    ?.mapNotNull { doc ->
                        if (doc.getBoolean("isActive") == false) {
                            return@mapNotNull null
                        }

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
         * מתאמן קיים מזוהה רק באמצעות Firebase UID.
         *
         * אין לקשר או למזג רשומה לפי שם בלבד:
         * ייתכנו שני מתאמנים בעלי אותו שם.
         *
         * רשומות ישנות ללא UID נשארות ללא שינוי,
         * עד לביצוע קישור זהות מאומת.
         */
        if (existingByUid != null) {
            val existingMemberId =
                existingByUid.getLong("id")
                    ?: existingByUid.id.toLongOrNull()
                    ?: stablePositiveLong(
                        existingByUid.id
                    )

            /*
             * מעדכנים רק רשומה שכבר מקושרת
             * לאותו Firebase UID.
             *
             * לא משנים את isActive:
             * מתאמן שהוסר נשאר לא פעיל עד
             * להחזרה מפורשת על ידי מאמן מורשה.
             *
             * ה-memberId והיסטוריית הנוכחות נשמרים.
             */
            val identityUpdate =
                mutableMapOf<String, Any>(
                    "updatedAtMillis" to
                            System.currentTimeMillis(),
                    "updatedAt" to
                            FieldValue.serverTimestamp()
                )

            if (cleanPhone.isNotBlank()) {
                identityUpdate["phone"] =
                    cleanPhone
            }

            existingByUid.reference
                .set(
                    identityUpdate,
                    SetOptions.merge()
                )
                .await()

            return existingMemberId
        }

        /*
         * מתאמן מחובר: מזהה יציב לפי Firebase UID.
         * מתאמן ידני: מזהה חדש שאינו תלוי בשם.
         *
         * כך שני מתאמנים בעלי אותו שם
         * יכולים להתקיים באותה קבוצה.
         */
        val memberId =
            if (cleanAuthUid.isNotBlank()) {
                stablePositiveLong(
                    "$branch|$groupKey|uid:$cleanAuthUid"
                )
            } else {
                stablePositiveLong(
                    UUID.randomUUID().toString()
                )
            }

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
                "isActive" to true,
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

        /*
         * יצירת מתאמן חדש בצורה אטומית.
         *
         * אם המסמך כבר קיים, אין לדרוס אותו:
         * ייתכן שהוא שייך למתאמן אחר או שהוא
         * מכיל היסטוריית נוכחות קיימת.
         */
        firestore.runTransaction { transaction ->

            val existingDocument =
                transaction.get(docRef)

            check(!existingDocument.exists()) {
                "Attendance member document already exists"
            }

            transaction.set(
                docRef,
                data
            )

            true
        }.await()

        return memberId
    }

    /**
     * הסרה לוגית של מתאמן מהקבוצה.
     *
     * רשומת המתאמן והיסטוריית הנוכחות נשמרות.
     * מתאמנים ללא שדה isActive נחשבים פעילים.
     */
    suspend fun removeMember(branch: String, groupKey: String, memberId: Long) {
        if (branch.isBlank() || groupKey.isBlank() || memberId <= 0L) return

        membersRef(branch, groupKey)
            .document(memberId.toString())
            .update(
                mapOf(
                    "isActive" to false,
                    "updatedAtMillis" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
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

        Log.d(
            "KMI_ATTENDANCE_REPO",
            "ensureSession: starting group metadata check, " +
                    "groupId=${groupDocId(branch, groupKey)}"
        )

        try {
            ensureGroupMetadata(branch, groupKey)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(
                "KMI_ATTENDANCE_REPO",
                "ensureSession: GROUP_METADATA_FAILED, " +
                        "groupId=${groupDocId(branch, groupKey)}",
                error
            )
            throw error
        }

        Log.d(
            "KMI_ATTENDANCE_REPO",
            "ensureSession: GROUP_METADATA_OK"
        )

        // Firestore הוא מקור האמת לשמות הסניף והקבוצה.
        // מונע כשל הרשאות בגלל הבדלי מקפים ורווחים.
        val groupSnapshot = groupRef(branch, groupKey)
            .get()
            .await()

        check(groupSnapshot.exists()) {
            "Attendance group document does not exist"
        }

        val canonicalBranch = groupSnapshot
            .getString("branch")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: error("Attendance group branch is missing")

        val canonicalGroup = groupSnapshot
            .getString("groupKey")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: error("Attendance group key is missing")

        check(
            normalizeAttendanceIdentity(canonicalBranch) ==
                    normalizeAttendanceIdentity(branch) &&
                    normalizeAttendanceIdentity(canonicalGroup) ==
                    normalizeAttendanceIdentity(groupKey)
        ) {
            "Attendance group identity mismatch"
        }

        Log.d(
            "KMI_ATTENDANCE_REPO",
            "ensureSession: CANONICAL_IDENTITY_OK, " +
                    "branch=$canonicalBranch, group=$canonicalGroup"
        )

        val sessionDocId = sessionDocId(date)
        val sessionId = stablePositiveLong("$branch|$groupKey|$sessionDocId")

        val groupId = groupDocId(branch, groupKey)
        sessionPathById[sessionId] = groupId to sessionDocId

        val now = System.currentTimeMillis()
        val sessionData = mapOf(
            "id" to sessionId,
            "date" to date.toString(),
            "branch" to canonicalBranch,
            "groupKey" to canonicalGroup,
            "coachUid" to currentCoachUidOrNull().orEmpty(),
            "coachEmail" to currentCoachEmailOrNull().orEmpty(),
            "status" to "open",
            "createdAtMillis" to now,
            "updatedAtMillis" to now,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            sessionsRef(branch, groupKey)
                .document(sessionDocId)
                .set(sessionData, SetOptions.merge())
                .await()

            Log.d(
                "KMI_ATTENDANCE_REPO",
                "ensureSession: SESSION_WRITE_OK, " +
                        "groupId=$groupId, date=$sessionDocId"
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(
                "KMI_ATTENDANCE_REPO",
                "ensureSession: SESSION_WRITE_FAILED, " +
                        "groupId=$groupId, date=$sessionDocId",
                error
            )
            throw error
        }

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

        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        if (
            cleanBranch.isBlank() ||
            cleanGroup.isBlank()
        ) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val sessionDocumentId =
            sessionDocId(date)

        val sessionId =
            stablePositiveLong(
                "$cleanBranch|$cleanGroup|$sessionDocumentId"
            )

        sessionPathById[sessionId] =
            groupDocId(
                cleanBranch,
                cleanGroup
            ) to sessionDocumentId

        /*
         * שומרים את מסמכי הנוכחות האחרונים כדי שכל שינוי
         * ב-members או ב-records ירענן מיד את המסך.
         */
        var latestRecordDocuments:
                List<com.google.firebase.firestore.DocumentSnapshot> =
            emptyList()

        fun emitAttendance() {

            /*
             * עובדים בתוך coroutine משום ששעת תחילת
             * האימון נקראת מ-trainingOccurrences.
             */
            launch {

                /*
                 * לוקחים snapshot יציב של הרשומות
                 * שעליהן אנחנו עומדים לעבוד.
                 */
                val recordDocuments =
                    latestRecordDocuments

                /*
                 * אוספים את כל occurrenceId שמופיעים
                 * ברשומות הנוכחות.
                 */
                val occurrenceIds =
                    recordDocuments
                        .mapNotNull { document ->
                            document
                                .getString("occurrenceId")
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                        }
                        .distinct()

                /*
                 * occurrenceId -> effectiveStartMillis
                 *
                 * זהו מקור האמת לשעת תחילת האימון.
                 */
                val effectiveStartByOccurrenceId =
                    mutableMapOf<String, Long>()

                occurrenceIds.forEach { occurrenceId ->

                    val occurrenceDocument =
                        runCatching {
                            firestore
                                .collection(
                                    "trainingOccurrences"
                                )
                                .document(
                                    occurrenceId
                                )
                                .get()
                                .await()
                        }
                            .getOrNull()

                    val effectiveStartMillis =
                        occurrenceDocument
                            ?.takeIf {
                                it.exists()
                            }
                            ?.getLong(
                                "effectiveStartMillis"
                            )

                    if (
                        effectiveStartMillis != null &&
                        effectiveStartMillis > 0L
                    ) {
                        effectiveStartByOccurrenceId[
                            occurrenceId
                        ] =
                            effectiveStartMillis
                    }
                }

                val nowMillis =
                    System.currentTimeMillis()

                val records =
                    recordDocuments
                        .mapNotNull { document ->

                            val coachStatusRaw =
                                document
                                    .getString(
                                        "coachStatus"
                                    )
                                    ?.trim()
                                    .orEmpty()

                            val traineeStatusRaw =
                                document
                                    .getString(
                                        "traineeStatus"
                                    )
                                    ?.trim()
                                    .orEmpty()

                            val legacyStatusRaw =
                                document
                                    .getString(
                                        "status"
                                    )
                                    ?.trim()
                                    .orEmpty()

                            /*
                             * מזהה האימון הספציפי.
                             */
                            val occurrenceId =
                                document
                                    .getString(
                                        "occurrenceId"
                                    )
                                    ?.trim()
                                    .orEmpty()

                            /*
                             * מקור האמת:
                             * trainingOccurrences.effectiveStartMillis
                             *
                             * fallback:
                             * trainingStartMillis נשאר רק עבור
                             * רשומות ישנות שעדיין אינן מקושרות
                             * ל-occurrence.
                             */
                            val trainingStartMillis =
                                effectiveStartByOccurrenceId[
                                    occurrenceId
                                ]
                                    ?: document
                                        .getLong(
                                            "trainingStartMillis"
                                        )
                                    ?: Long.MAX_VALUE

                            val isTrainingStarted =
                                nowMillis >=
                                        trainingStartMillis

                            /*
                             * סריקת QR היא אישור נוכחות בפועל.
                             *
                             * לכן היא גוברת על בחירת המתאמן
                             * גם בחצי השעה שלפני תחילת האימון.
                             *
                             * ללא QR:
                             * לפני האימון — בחירת המתאמן.
                             * מתחילת האימון — בחירת המאמן.
                             */
                            /*
                             * אימות QR נפרד מסימון ההגעה המוקדם.
                             *
                             * האישור תקף לרשומה רק כאשר
                             * מזהה האימון המאומת תואם
                             * למזהה האימון של הרשומה.
                             *
                             * אין להסתמך על markedBy בלבד:
                             * הוא עשוי להשתנות בעקבות פעולה נוספת.
                             */
                            val isQrAttendance =
                                document
                                    .getBoolean("qrVerified") == true &&
                                        document
                                            .getString("qrVerifiedOccurrenceId")
                                            ?.trim()
                                            ?.takeIf { it.isNotBlank() } ==
                                        occurrenceId &&
                                        coachStatusRaw ==
                                        AttendanceStatus.PRESENT.name

                            /*
                             * סדר עדיפות:
                             *
                             * 1. נוכחות QR מאומתת.
                             * 2. לפני האימון — בחירת המתאמן.
                             * 3. מתחילת האימון — בחירת המאמן.
                             *
                             * ללא QR ההתנהגות נשארת ללא שינוי.
                             */
                            val markedBy =
                                document
                                    .getString("markedBy")
                                    ?.trim()
                                    .orEmpty()

                            val statusRaw =
                                when {
                                    isQrAttendance ->
                                        AttendanceStatus.PRESENT.name

                                    !isTrainingStarted ->
                                        traineeStatusRaw
                                            .ifBlank {
                                                legacyStatusRaw
                                            }

                                    coachStatusRaw.isNotBlank() ->
                                        coachStatusRaw

                                    markedBy == "trainee" ->
                                        AttendanceStatus.ABSENT.name

                                    else ->
                                        legacyStatusRaw
                                }

                            android.util.Log.d(
                                "KMI_ATTENDANCE_RECORD",
                                buildString {
                                    append("docId=")
                                    append(document.id)

                                    append(" | memberId=")
                                    append(
                                        document.getLong("memberId")
                                    )

                                    append(" | traineeUid=")
                                    append(
                                        document
                                            .getString("traineeUid")
                                            .orEmpty()
                                    )

                                    append(" | traineeStatus=")
                                    append(traineeStatusRaw)

                                    append(" | coachStatus=")
                                    append(coachStatusRaw)

                                    append(" | legacyStatus=")
                                    append(legacyStatusRaw)

                                    append(" | trainingStart=")
                                    append(trainingStartMillis)

                                    append(" | now=")
                                    append(System.currentTimeMillis())

                                    append(" | started=")
                                    append(isTrainingStarted)

                                    append(" | chosenStatus=")
                                    append(statusRaw)
                                }
                            )

                            val status =
                                runCatching {
                                    AttendanceStatus
                                        .valueOf(
                                            statusRaw
                                        )
                                }
                                    .getOrDefault(
                                        AttendanceStatus.ABSENT
                                    )

                            val storedMemberId =
                                document
                                    .getLong(
                                        "memberId"
                                    )
                                    ?: document.id
                                        .toLongOrNull()
                                    ?: return@mapNotNull null

                            /*
                             * רשומת נוכחות היסטורית שומרת
                             * תמיד על memberId המקורי.
                             *
                             * אין להעביר אותה אוטומטית
                             * למתאמן אחר לפי UID או שם.
                             *
                             * מתאמן שהוסר לוגית עדיין
                             * שומר על מזהה הנוכחות שלו.
                             */
                            val resolvedMemberId =
                                storedMemberId

                            android.util.Log.d(
                                "KMI_ATTENDANCE_MEMBER",
                                "stored=$storedMemberId" +
                                        " | resolved=$resolvedMemberId"
                            )

                            AttendanceRecord(
                                id =
                                    document
                                        .getLong("id")
                                        ?: stablePositiveLong(
                                            "$sessionId|" +
                                                    "$resolvedMemberId"
                                        ),
                                sessionId =
                                    sessionId,
                                memberId =
                                    resolvedMemberId,
                                status =
                                    status,
                                markedAtMillis =
                                    document
                                        .getLong(
                                            "markedAtMillis"
                                        )
                                        ?: 0L
                            )
                        }
                        /*
                         * אם במקרה קיימות כמה רשומות
                         * לאותו member — לוקחים את האחרונה.
                         */
                        .groupBy {
                            it.memberId
                        }
                        .mapNotNull {
                                (_, recordsForMember) ->

                            recordsForMember
                                .maxByOrNull {
                                    it.markedAtMillis
                                }
                        }

                trySend(
                    records
                )
            }
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

                    /*
                     * רענון הנוכחות בעקבות שינוי ברשימת
                     * המתאמנים, ללא מיפוי מזהים לפי
                     * שם או Firebase UID.
                     */
                    emitAttendance()
                }

        val recordsRegistration =
            sessionsRef(
                cleanBranch,
                cleanGroup
            )
                .document(
                    sessionDocumentId
                )
                .collection("records")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    latestRecordDocuments =
                        snapshot
                            ?.documents
                            .orEmpty()

                    /*
                     * כל שינוי בסימוני הנוכחות
                     * מרענן מיד את הנתונים במסך.
                     */
                    emitAttendance()
                }

        awaitClose {
            membersRegistration.remove()
            recordsRegistration.remove()
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
         * מזהה מתאמן -> שם תצוגה.
         *
         * כך אפשר להציג למאמן לא רק את הכמות,
         * אלא גם את רשימת השמות בכל סטטוס.
         */
        var memberNames:
                Map<Long, String> =
            emptyMap()

        /*
         * מזהי כל חברי הקבוצה הפעילים.
         */
        var memberIds:
                Set<Long> =
            emptySet()

        /*
         * בחירות שהמתאמנים סימנו בעצמם
         * עבור האימון הנוכחי.
         */
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

            val comingMemberIds =
                validChoices
                    .filterValues { status ->
                        status == AttendanceStatus.PRESENT
                    }
                    .keys

            val notComingMemberIds =
                validChoices
                    .filterValues { status ->
                        status == AttendanceStatus.ABSENT
                    }
                    .keys

            val noResponseMemberIds =
                memberIds - validChoices.keys

            val comingNames =
                comingMemberIds
                    .mapNotNull { memberId ->
                        memberNames[memberId]
                    }
                    .sorted()

            val notComingNames =
                notComingMemberIds
                    .mapNotNull { memberId ->
                        memberNames[memberId]
                    }
                    .sorted()

            val noResponseNames =
                noResponseMemberIds
                    .mapNotNull { memberId ->
                        memberNames[memberId]
                    }
                    .sorted()

            trySend(
                TrainingAttendanceForecast(
                    comingCount =
                        comingMemberIds.size,
                    notComingCount =
                        notComingMemberIds.size,
                    noResponseCount =
                        noResponseMemberIds.size,
                    comingNames =
                        comingNames,
                    notComingNames =
                        notComingNames,
                    noResponseNames =
                        noResponseNames
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

                    /*
                     * צפי ההגעה כולל מתאמנים פעילים בלבד.
                     *
                     * רשומות ישנות ללא isActive
                     * נחשבות פעילות לצורך תאימות.
                     *
                     * הסינון אינו משנה או מוחק
                     * היסטוריית נוכחות.
                     */
                    val memberDocuments =
                        snapshot
                            ?.documents
                            .orEmpty()
                            .filter { document ->
                                document.getBoolean("isActive") != false
                            }

                    memberIds =
                        memberDocuments
                            .mapNotNull { document ->
                                document.getLong("id")
                                    ?: document.id.toLongOrNull()
                            }
                            .toSet()

                    memberNames =
                        memberDocuments
                            .mapNotNull { document ->

                                val memberId =
                                    document.getLong("id")
                                        ?: document.id.toLongOrNull()
                                        ?: return@mapNotNull null

                                val displayName =
                                    (
                                            document.getString("displayName")
                                                ?: document.getString("fullName")
                                                ?: document.getString("name")
                                            )
                                        ?.trim()
                                        .orEmpty()

                                if (displayName.isBlank()) {
                                    return@mapNotNull null
                                }

                                memberId to displayName
                            }
                            .toMap()

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
                                 * הצפי מבוסס רק על בחירה
                                 * שהמתאמן ביצע בעצמו.
                                 */
                                val hasTraineeChoice =
                                    document
                                        .getString("traineeStatus")
                                        ?.trim()
                                        ?.isNotBlank() == true

                                val isLegacyTraineeChoice =
                                    document
                                        .getString("markedBy")
                                        ?.trim() == "trainee"

                                if (
                                    !hasTraineeChoice &&
                                    !isLegacyTraineeChoice
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

                                val statusRaw =
                                    (
                                            document.getString(
                                                "traineeStatus"
                                            )
                                                ?: document.getString(
                                                    "status"
                                                )
                                            )
                                        ?.trim()
                                        .orEmpty()

                                val status =
                                    when (statusRaw) {
                                        AttendanceStatus.PRESENT.name ->
                                            AttendanceStatus.PRESENT

                                        AttendanceStatus.ABSENT.name ->
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
                            val coachStatusRaw =
                                recordDocument
                                    .getString("coachStatus")
                                    ?.trim()
                                    .orEmpty()

                            val occurrenceId =
                                recordDocument
                                    .getString("occurrenceId")
                                    ?.trim()
                                    .orEmpty()

                            val isQrVerified =
                                occurrenceId.isNotBlank() &&
                                        recordDocument
                                            .getBoolean("qrVerified") == true &&
                                        recordDocument
                                            .getString("qrVerifiedOccurrenceId")
                                            ?.trim() == occurrenceId &&
                                        coachStatusRaw ==
                                        AttendanceStatus.PRESENT.name

                            val isLegacyCoachRecord =
                                coachStatusRaw.isBlank() &&
                                        recordDocument
                                            .getString("markedBy")
                                            ?.trim() == "coach"

                            val finalStatusRaw =
                                when {
                                    isQrVerified ->
                                        AttendanceStatus.PRESENT.name

                                    coachStatusRaw.isNotBlank() ->
                                        coachStatusRaw

                                    isLegacyCoachRecord ->
                                        recordDocument
                                            .getString("status")
                                            ?.trim()
                                            .orEmpty()

                                    else -> ""
                                }

                            runCatching {
                                AttendanceStatus.valueOf(finalStatusRaw)
                            }.getOrNull()
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

        val currentAuthUid =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid
                ?.trim()
                .orEmpty()

        if (
            cleanBranch.isBlank() ||
            cleanGroup.isBlank() ||
            cleanUid.isBlank() ||
            currentAuthUid.isBlank() ||
            cleanUid != currentAuthUid
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
   * אין member המשויך ל-Firebase UID המאומת.
   *
   * אין ליצור או לעדכן שיוך לפי שם או טלפון.
   * קישור רשומה ישנה מחייב אימות בצד השרת.
   */
        return null
    }

    /**
     * טוען את הבחירה הקיימת של המתאמן
     * * עבור אימון מסוים.
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
            (
                    recordDoc.getString("traineeStatus")
                        ?: recordDoc.getString("status")
                    )
                ?.trim()
                .orEmpty()

        return runCatching {
            AttendanceStatus.valueOf(
                statusRaw
            )
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

        if (occurrenceId.isNotBlank()) {
            val occurrenceDoc =
                firestore
                    .collection("trainingOccurrences")
                    .document(occurrenceId)
                    .get()
                    .await()

            android.util.Log.d(
                "KMI_ATTENDANCE_RULE_CHECK",
                buildString {
                    append("occurrenceExists=")
                    append(occurrenceDoc.exists())

                    append(" | keyMatches=")
                    append(
                        occurrenceDoc.getString("occurrenceKey") ==
                                cleanOccurrenceKey
                    )

                    append(" | sessionDateMatches=")
                    append(
                        occurrenceDoc.getString("sessionDate") ==
                                sessionDocId
                    )

                    append(" | isCancelled=")
                    append(occurrenceDoc.getBoolean("isCancelled"))

                    append(" | effectiveStartMillis=")
                    append(occurrenceDoc.getLong("effectiveStartMillis"))

                    append(" | occurrenceBranch=")
                    append(occurrenceDoc.getString("branch"))

                    append(" | occurrenceGroup=")
                    append(occurrenceDoc.getString("group"))
                }
            )
        } else {
            android.util.Log.e(
                "KMI_ATTENDANCE_RULE_CHECK",
                "Missing occurrenceId"
            )
        }

        val data =
            mutableMapOf<String, Any>(
                "id" to recordId,
                "sessionId" to sessionId,
                "memberId" to memberId,

                /*
                 * דיווח מוקדם של המתאמן בלבד.
                 *
                 * traineeStatus = צפי הגעה.
                 * coachStatus = נוכחות שסימן המאמן.
                 * qrVerified = נוכחות שאומתה בשרת.
                 *
                 * אין לעדכן כאן status או coachStatus.
                 */
                "traineeStatus" to status.name,
                "traineeUid" to cleanUid,

                /*
                 * שעת האימון שאליה מתייחס הדיווח.
                 */
                "trainingStartMillis" to
                        trainingStartMillis,

                /*
                 * זמן הדיווח העצמי נשמר בנפרד.
                 *
                 * markedAtMillis שייך לרישום הנוכחות
                 * ואסור לדרוס אותו בדיווח מקדים.
                 */
                "traineeMarkedAtMillis" to now,

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

        val recordRef =
            sessionsRef(
                cleanBranch,
                cleanGroup
            )
                .document(sessionDocId)
                .collection("records")
                .document(memberId.toString())

        firestore.runTransaction { transaction ->

            val existing =
                transaction.get(recordRef)

            /*
             * אימות נוסף בתוך ה-Transaction:
             * אין לקבל דיווח עצמי לאחר תחילת האימון.
             */
            check(System.currentTimeMillis() < trainingStartMillis) {
                "Training has already started"
            }

            /*
             * שומרים את המקור הקיים אם המאמן
             * כבר סימן נוכחות או אם QR אומת.
             */
            val hasCoachStatus =
                !existing
                    .getString("coachStatus")
                    .isNullOrBlank()

            val hasVerifiedQr =
                existing.getBoolean("qrVerified") == true

            val hasLegacyCoachMark =
                existing
                    .getString("markedBy")
                    ?.trim() == "coach"

            val update =
                data.toMutableMap()

            /*
             * markedBy הוא שדה תאימות ישן.
             *
             * ברשומה חדשה מציינים שהמתאמן
             * ביצע את הדיווח.
             *
             * ברשומה קיימת לא דורסים את
             * מקור הסימון של המאמן או ה-QR.
             */
            if (
                !hasCoachStatus &&
                !hasVerifiedQr &&
                !hasLegacyCoachMark
            ) {
                update["markedBy"] = "trainee"
            }

            /*
             * אם מדובר ברשומה חדשה בלבד,
             * שומרים זמן סימון ראשוני.
             *
             * אין להחליף markedAtMillis
             * של סריקה או סימון מאמן.
             */
            if (!existing.exists()) {
                update["markedAtMillis"] = now
            }

            /*
             * רישום זמן נפרד לדיווח המתאמן.
             */
            update["traineeMarkedAtMillis"] = now

            transaction.set(
                recordRef,
                update,
                SetOptions.merge()
            )

            true
        }.await()
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

            /*
             * הנוכחות בפועל שקבע המאמן.
             */
            "coachStatus" to status.name,

            /*
             * נשאר לצורכי תאימות.
             */
            "status" to status.name,

            "coachUid" to
                    currentCoachUidOrNull().orEmpty(),
            "coachEmail" to
                    currentCoachEmailOrNull().orEmpty(),

            "markedAtMillis" to ts,
            "updatedAtMillis" to ts,
            "updatedAt" to
                    FieldValue.serverTimestamp()
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

                val coachStatusRaw =
                    doc.getString("coachStatus")
                        ?.trim()
                        .orEmpty()

                val occurrenceId =
                    doc.getString("occurrenceId")
                        ?.trim()
                        .orEmpty()

                val isQrVerified =
                    occurrenceId.isNotBlank() &&
                            doc.getBoolean("qrVerified") == true &&
                            doc.getString("qrVerifiedOccurrenceId")
                                ?.trim() == occurrenceId &&
                            coachStatusRaw == AttendanceStatus.PRESENT.name

                val isLegacyCoachRecord =
                    coachStatusRaw.isBlank() &&
                            doc.getString("markedBy")
                                ?.trim() == "coach"

                val finalStatusRaw =
                    when {
                        isQrVerified ->
                            AttendanceStatus.PRESENT.name

                        coachStatusRaw.isNotBlank() ->
                            coachStatusRaw

                        isLegacyCoachRecord ->
                            doc.getString("status")
                                ?.trim()
                                .orEmpty()

                        else -> ""
                    }

                val finalStatus =
                    runCatching {
                        AttendanceStatus.valueOf(finalStatusRaw)
                    }.getOrNull()

                if (finalStatus != null) {
                    markedByMember[memberId] =
                        (markedByMember[memberId] ?: 0) + 1

                    if (finalStatus == AttendanceStatus.PRESENT) {
                        presentByMember[memberId] =
                            (presentByMember[memberId] ?: 0) + 1
                    }
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