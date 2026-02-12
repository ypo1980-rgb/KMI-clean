package il.kmi.app.data.training

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface TrainingSummaryRepo {

    suspend fun saveForOwner(
        ownerUid: String,
        ownerRole: SummaryAuthorRole,
        summary: TrainingSummaryEntity
    ): String

    suspend fun listForOwner(
        ownerUid: String,
        ownerRole: SummaryAuthorRole,
        limit: Long = 30
    ): List<TrainingSummaryEntity>

    // ✅ חדש: מחזיר רק את התאריכים שיש בהם סיכום בטווח (למשל חודש)
    suspend fun listDatesForOwnerBetween(
        ownerUid: String,
        ownerRole: SummaryAuthorRole,
        startIso: String,          // inclusive  "2026-01-01"
        endIsoExclusive: String    // exclusive  "2026-02-01"
    ): Set<String>
}

/**
 * שמירה "בצד של המשתמש" (כל אחד בצד שלו):
 * users/{ownerUid}/training_summaries_{role}/{summaryId}
 *
 * לדוגמה:
 * users/abc/training_summaries_TRAINEE/{id}
 * users/abc/training_summaries_COACH/{id}
 */
class FirestoreTrainingSummaryRepo(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TrainingSummaryRepo {

    override suspend fun saveForOwner(
        ownerUid: String,
        ownerRole: SummaryAuthorRole,
        summary: TrainingSummaryEntity
    ): String {
        val uid = ownerUid.trim()
        require(uid.isNotEmpty()) { "ownerUid is required" }

        val col = db.collection("users")
            .document(uid)
            .collection(colName(ownerRole))

        val now = System.currentTimeMillis()

        val docRef = if (summary.id.isNotBlank()) col.document(summary.id) else col.document()

        val toSave = summary.copy(
            id = docRef.id,
            ownerUid = uid,
            ownerRole = ownerRole,
            createdAtMs = summary.createdAtMs.takeIf { it > 0L } ?: now,
            updatedAtMs = now
        )

        docRef.set(toMap(toSave)).await()
        return docRef.id
    }

    override suspend fun listForOwner(
        ownerUid: String,
        ownerRole: SummaryAuthorRole,
        limit: Long
    ): List<TrainingSummaryEntity> {
        val uid = ownerUid.trim()
        require(uid.isNotEmpty()) { "ownerUid is required" }

        val snap = db.collection("users")
            .document(uid)
            .collection(colName(ownerRole))
            .orderBy("dateIso")
            .limit(limit)
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            fromDoc(doc.id, doc.data ?: return@mapNotNull null)
        }
    }

    // ✅ חדש: תאריכים בלבד בחודש/טווח
    override suspend fun listDatesForOwnerBetween(
        ownerUid: String,
        ownerRole: SummaryAuthorRole,
        startIso: String,
        endIsoExclusive: String
    ): Set<String> {
        val uid = ownerUid.trim()
        require(uid.isNotEmpty()) { "ownerUid is required" }

        val s = startIso.trim()
        val e = endIsoExclusive.trim()
        if (s.isBlank() || e.isBlank()) return emptySet()

        val snap = db.collection("users")
            .document(uid)
            .collection(colName(ownerRole))
            .whereGreaterThanOrEqualTo("dateIso", s)
            .whereLessThan("dateIso", e)
            .get()
            .await()

        return snap.documents
            .mapNotNull { it.getString("dateIso")?.trim()?.takeIf { d -> d.isNotBlank() } }
            .toSet()
    }

    // -----------------------------
    // Firestore mapping
    // -----------------------------

    private fun colName(role: SummaryAuthorRole): String = "training_summaries_${role.name}"

    private fun toMap(s: TrainingSummaryEntity): Map<String, Any?> {
        return mapOf(
            "id" to s.id,
            "ownerUid" to s.ownerUid,
            "ownerRole" to s.ownerRole.name,

            "dateIso" to s.dateIso,
            "branchId" to s.branchId,
            "branchName" to s.branchName,

            "coachUid" to s.coachUid,
            "coachName" to s.coachName,

            "groupKey" to s.groupKey,

            "notes" to s.notes,
            "createdAtMs" to s.createdAtMs,
            "updatedAtMs" to s.updatedAtMs,

            "exercises" to s.exercises.map { e ->
                mapOf(
                    "exerciseId" to e.exerciseId,
                    "name" to e.name,
                    "topic" to e.topic,
                    "difficulty" to e.difficulty,
                    "highlight" to e.highlight,
                    "homePractice" to e.homePractice
                )
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun fromDoc(id: String, m: Map<String, Any?>): TrainingSummaryEntity? {

        val roleRaw = (m["ownerRole"] as? String)?.trim().orEmpty()
        val role = runCatching { SummaryAuthorRole.valueOf(roleRaw) }
            .getOrDefault(SummaryAuthorRole.TRAINEE)

        val exList = (m["exercises"] as? List<*>)?.mapNotNull { any ->
            val mm = any as? Map<String, Any?> ?: return@mapNotNull null
            TrainingSummaryExerciseEntity(
                exerciseId = (mm["exerciseId"] as? String).orEmpty(),
                name = (mm["name"] as? String).orEmpty(),
                topic = (mm["topic"] as? String).orEmpty(),
                difficulty = (mm["difficulty"] as? Number)?.toInt(),
                highlight = (mm["highlight"] as? String).orEmpty(),
                homePractice = (mm["homePractice"] as? Boolean) ?: false
            )
        } ?: emptyList()

        return TrainingSummaryEntity(
            id = id,
            ownerUid = (m["ownerUid"] as? String).orEmpty(),
            ownerRole = role,

            dateIso = (m["dateIso"] as? String).orEmpty(),
            branchId = (m["branchId"] as? String).orEmpty(),
            branchName = (m["branchName"] as? String).orEmpty(),

            coachUid = (m["coachUid"] as? String).orEmpty(),
            coachName = (m["coachName"] as? String).orEmpty(),

            groupKey = (m["groupKey"] as? String).orEmpty(),

            exercises = exList,
            notes = (m["notes"] as? String).orEmpty(),

            createdAtMs = (m["createdAtMs"] as? Number)?.toLong() ?: 0L,
            updatedAtMs = (m["updatedAtMs"] as? Number)?.toLong() ?: 0L
        )
    }
}
