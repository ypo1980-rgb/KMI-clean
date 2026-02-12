package il.kmi.shared.free_sessions.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.shared.free_sessions.model.FreeSession
import il.kmi.shared.free_sessions.model.FreeSessionPart
import il.kmi.shared.free_sessions.model.ParticipantState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

actual fun freeSessionsRepository(): FreeSessionsRepository =
    AndroidFreeSessionsRepository(Firebase.firestore)

actual fun systemNowMillis(): Long = System.currentTimeMillis()

private class AndroidFreeSessionsRepository(
    private val db: FirebaseFirestore
) : FreeSessionsRepository {

    private suspend fun resolveUserName(uid: String): String? {
        return runCatching {
            val snap = db.collection("users").document(uid).get().await()
            // תומך בכמה שמות שדות נפוצים אצלך/במערכות שונות
            snap.getString("name")
                ?: snap.getString("fullName")
                ?: snap.getString("displayName")
                ?: snap.getString("userName")
        }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }

    override suspend fun createFreeSession(
        branch: String,
        groupKey: String,
        title: String,
        locationName: String?,
        lat: Double?,
        lng: Double?,
        startsAt: Long,
        createdByUid: String,
        createdByName: String
    ): String {
        val colPath = FreeSessionsPaths.freeSessionsCol(branch, groupKey)
        val col = db.collection(colPath)

        val now = System.currentTimeMillis()
        val doc = col.document() // id מראש

        // ✅ FIX: אם השם ריק – נשלוף אותו מהמשתמש ב-Firestore
        val safeName = createdByName.trim().ifBlank {
            resolveUserName(createdByUid).orEmpty()
        }.ifBlank {
            "משתמש"
        }

        android.util.Log.d(
            "FREE_SESSIONS_PATH",
            "createFreeSession -> $colPath (uid=$createdByUid, name='$safeName')"
        )

        val data = hashMapOf<String, Any?>(
            "id" to doc.id,
            "branch" to branch.trim(),
            "groupKey" to groupKey.trim(),
            "title" to title.trim(),
            "locationName" to locationName?.trim(),
            "lat" to lat,
            "lng" to lng,
            "startsAt" to startsAt,
            "createdAt" to now,
            "createdByUid" to createdByUid,
            "createdByName" to safeName,
            "status" to "OPEN",

            // counters (אופציונלי)
            "goingCount" to 0,
            "onWayCount" to 0,
            "arrivedCount" to 0,
            "cantCount" to 0
        )

        doc.set(data).await()

        // יוצר האימון נסמן כברירת מחדל כ-GOING (אפשר לשנות אם לא רוצים)
        setParticipantState(
            branch = branch,
            groupKey = groupKey,
            sessionId = doc.id,
            uid = createdByUid,
            name = safeName,
            state = ParticipantState.GOING
        )

        return doc.id
    }

    override fun observeUpcoming(
        branch: String,
        groupKey: String,
        nowMillis: Long
    ): Flow<List<FreeSession>> = callbackFlow {
        val colPath = FreeSessionsPaths.freeSessionsCol(branch, groupKey)

        // ⚠️ חשוב:
        // השאילתה עם status + startsAt + orderBy דורשת לפעמים אינדקס קומפוזיט.
        // כדי להימנע מזה לגמרי – נשאב לפי startsAt בלבד ונפילטר status בצד לקוח.
        val q = db.collection(colPath)
            .whereGreaterThanOrEqualTo("startsAt", nowMillis)
            .orderBy("startsAt", Query.Direction.ASCENDING)

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.e("FREE_SESSIONS", "observeUpcoming failed", err)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val out = snap?.documents.orEmpty().mapNotNull { d ->
                val id = d.getString("id") ?: d.id
                val b = d.getString("branch") ?: branch
                val g = d.getString("groupKey") ?: groupKey
                val title = d.getString("title") ?: ""
                val locationName = d.getString("locationName")
                val lat = d.getDouble("lat")
                val lng = d.getDouble("lng")
                val startsAt = (d.getLong("startsAt") ?: 0L)
                val createdAt = (d.getLong("createdAt") ?: 0L)
                val createdByUid = d.getString("createdByUid") ?: ""
                val createdByName = d.getString("createdByName") ?: ""
                val status = d.getString("status") ?: "OPEN"

                if (title.isBlank() || createdByUid.isBlank()) return@mapNotNull null

                FreeSession(
                    id = id,
                    branch = b,
                    groupKey = g,
                    title = title,
                    locationName = locationName,
                    lat = lat,
                    lng = lng,
                    startsAt = startsAt,
                    createdAt = createdAt,
                    createdByUid = createdByUid,
                    createdByName = createdByName,
                    status = status,
                    goingCount = (d.getLong("goingCount") ?: 0L).toInt(),
                    onWayCount = (d.getLong("onWayCount") ?: 0L).toInt(),
                    arrivedCount = (d.getLong("arrivedCount") ?: 0L).toInt(),
                    cantCount = (d.getLong("cantCount") ?: 0L).toInt()
                )
            }
                // ✅ סינון OPEN בצד לקוח (כדי לא לדרוש אינדקס קומפוזיט)
                .filter { it.status == "OPEN" }

            trySend(out)
        }

        awaitClose { reg.remove() }
    }

    override fun observeParticipants(
        branch: String,
        groupKey: String,
        sessionId: String
    ): Flow<List<FreeSessionPart>> = callbackFlow {
        val colPath = FreeSessionsPaths.freeSessionsCol(branch, groupKey)
        val partsCol = db.collection(colPath)
            .document(sessionId)
            .collection(FreeSessionsPaths.COL_PARTICIPANTS)

        val q = partsCol.orderBy("updatedAt", Query.Direction.DESCENDING)

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val out = snap?.documents.orEmpty().mapNotNull { d ->
                val uid = d.getString("uid") ?: d.id
                val name = d.getString("name") ?: return@mapNotNull null
                val state = ParticipantState.fromId(d.getString("state"))
                val updatedAt = d.getLong("updatedAt") ?: 0L
                FreeSessionPart(
                    uid = uid,
                    name = name,
                    state = state,
                    updatedAt = updatedAt
                )
            }
            trySend(out)
        }

        awaitClose { reg.remove() }
    }

    override suspend fun setParticipantState(
        branch: String,
        groupKey: String,
        sessionId: String,
        uid: String,
        name: String,
        state: ParticipantState
    ) {
        val colPath = FreeSessionsPaths.freeSessionsCol(branch, groupKey)
        val sessionDoc = db.collection(colPath).document(sessionId)
        val partDoc = sessionDoc.collection(FreeSessionsPaths.COL_PARTICIPANTS).document(uid)

        val now = System.currentTimeMillis()

        // נשמור משתתף
        partDoc.set(
            hashMapOf(
                "uid" to uid,
                "name" to name.trim(),
                "state" to state.name,
                "updatedAt" to now
            )
        ).await()

        // ✅ עדכון counters (בלי Transaction כדי להימנע מ-tx.get(Query) אצלך)
        val partsSnap = sessionDoc
            .collection(FreeSessionsPaths.COL_PARTICIPANTS)
            .get()
            .await()

        var going = 0
        var onWay = 0
        var arrived = 0
        var cant = 0

        for (p in partsSnap.documents) {
            when (ParticipantState.fromId(raw = p.getString("state"))) {
                ParticipantState.GOING -> going++
                ParticipantState.ON_WAY -> onWay++
                ParticipantState.ARRIVED -> arrived++
                ParticipantState.CANT -> cant++
                ParticipantState.INVITED -> Unit
            }
        }

        sessionDoc.update(
            mapOf(
                "goingCount" to going,
                "onWayCount" to onWay,
                "arrivedCount" to arrived,
                "cantCount" to cant,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun closeSession(
        branch: String,
        groupKey: String,
        sessionId: String
    ) {
        val colPath = FreeSessionsPaths.freeSessionsCol(branch, groupKey)
        db.collection(colPath)
            .document(sessionId)
            .update(
                mapOf(
                    "status" to "CLOSED",
                    "closedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    // ✅ NEW: מחיקה מלאה של אימון + participants
    override suspend fun deleteFreeSession(
        branch: String,
        groupKey: String,
        sessionId: String
    ) {
        val colPath = FreeSessionsPaths.freeSessionsCol(branch, groupKey)
        val sessionDoc = db.collection(colPath).document(sessionId)
        val partsCol = sessionDoc.collection(FreeSessionsPaths.COL_PARTICIPANTS)

        // 1) מוחקים את תת-הקולקציה participants בבאצ'ים (Firestore לא מוחק תת-קולקציות לבד)
        while (true) {
            val snap = partsCol.limit(450).get().await() // מתחת ל-500 לבטחון
            if (snap.isEmpty) break

            db.runBatch { batch ->
                for (d in snap.documents) {
                    batch.delete(d.reference)
                }
            }.await()
        }

        // 2) מוחקים את מסמך האימון עצמו
        sessionDoc.delete().await()
    }
}

