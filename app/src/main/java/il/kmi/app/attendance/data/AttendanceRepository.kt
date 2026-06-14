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
import java.security.MessageDigest
import java.time.LocalDate

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
    suspend fun addMember(branch: String, groupKey: String, displayName: String): Long {
        val cleanName = displayName.trim()
        if (branch.isBlank() || groupKey.isBlank() || cleanName.isBlank()) return 0L

        ensureGroupMetadata(branch, groupKey)

        val key = cleanName.nameKey()
        val existing = membersRef(branch, groupKey)
            .whereEqualTo("displayNameKey", key)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()

        if (existing != null) {
            return existing.getLong("id") ?: stablePositiveLong(existing.id)
        }

        val memberId = stablePositiveLong("$branch|$groupKey|$key")
        val docRef = membersRef(branch, groupKey).document(memberId.toString())

        val now = System.currentTimeMillis()
        val data = mapOf(
            "id" to memberId,
            "branch" to branch.trim(),
            "groupKey" to groupKey.trim(),
            "displayName" to cleanName,
            "displayNameKey" to key,
            "coachUid" to currentCoachUidOrNull().orEmpty(),
            "coachEmail" to currentCoachEmailOrNull().orEmpty(),
            "source" to "manual_or_bootstrap",
            "createdAtMillis" to now,
            "updatedAtMillis" to now,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(data, SetOptions.merge()).await()

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