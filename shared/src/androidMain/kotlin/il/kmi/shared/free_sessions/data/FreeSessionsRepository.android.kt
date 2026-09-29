package il.kmi.shared.free_sessions.data

import com.google.firebase.functions.FirebaseFunctions
import il.kmi.shared.free_sessions.model.FreeSession
import il.kmi.shared.free_sessions.model.FreeSessionPart
import il.kmi.shared.free_sessions.model.ParticipantState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

actual fun freeSessionsRepository(): FreeSessionsRepository =
    AndroidFreeSessionsRepository()

actual fun systemNowMillis(): Long = System.currentTimeMillis()

private class AndroidFreeSessionsRepository :
    FreeSessionsRepository {

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
        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        val cleanTitle =
            title.trim()

        require(
            cleanBranch.isNotBlank() &&
                    cleanGroup.isNotBlank() &&
                    cleanTitle.isNotBlank()
        ) {
            "Missing branch, group or title"
        }

        val payload =
            mutableMapOf<String, Any>(
                "branch" to cleanBranch,
                "group" to cleanGroup,
                "title" to cleanTitle,
                "startsAt" to startsAt
            )

        locationName
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                payload["locationName"] = it
            }

        lat?.let {
            payload["lat"] = it
        }

        lng?.let {
            payload["lng"] = it
        }

        val result =
            FirebaseFunctions
                .getInstance()
                .getHttpsCallable(
                    "createSecureFreeSession"
                )
                .call(payload)
                .await()

        val response =
            result.data as? Map<*, *>
                ?: error(
                    "Invalid createSecureFreeSession response"
                )

        val sessionId =
            response["sessionId"]
                ?.toString()
                ?.trim()
                .orEmpty()

        check(sessionId.isNotBlank()) {
            "Missing sessionId from createSecureFreeSession"
        }

        return sessionId
    }

    override fun observeUpcoming(
        branch: String,
        groupKey: String,
        nowMillis: Long
    ): Flow<List<FreeSession>> = callbackFlow {

        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        if (
            cleanBranch.isBlank() ||
            cleanGroup.isBlank()
        ) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val job =
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.Dispatchers.IO
            ).launch {

                val result =
                    runCatching {
                        FirebaseFunctions
                            .getInstance()
                            .getHttpsCallable(
                                "loadSecureFreeSessions"
                            )
                            .call(
                                mapOf(
                                    "branch" to cleanBranch,
                                    "group" to cleanGroup,
                                    "nowMillis" to nowMillis
                                )
                            )
                            .await()
                    }

                if (result.isFailure) {
                    trySend(emptyList())
                    return@launch
                }

                val payload =
                    result.getOrNull()
                        ?.data as? Map<*, *>

                val rawItems =
                    payload
                        ?.get("items")
                            as? List<*>
                        ?: emptyList<Any>()

                val items =
                    rawItems
                        .mapNotNull { rawItem ->

                            val item =
                                rawItem as? Map<*, *>
                                    ?: return@mapNotNull null

                            val id =
                                item["id"]
                                    ?.toString()
                                    ?.trim()
                                    .orEmpty()

                            val title =
                                item["title"]
                                    ?.toString()
                                    ?.trim()
                                    .orEmpty()

                            val createdByUid =
                                item["createdByUid"]
                                    ?.toString()
                                    ?.trim()
                                    .orEmpty()

                            if (
                                id.isBlank() ||
                                title.isBlank() ||
                                createdByUid.isBlank()
                            ) {
                                return@mapNotNull null
                            }

                            FreeSession(
                                id = id,
                                branch =
                                    item["branch"]
                                        ?.toString()
                                        ?.trim()
                                        .orEmpty(),

                                groupKey =
                                    item["groupKey"]
                                        ?.toString()
                                        ?.trim()
                                        .orEmpty(),

                                title = title,

                                locationName =
                                    item["locationName"]
                                        ?.toString()
                                        ?.trim()
                                        ?.takeIf {
                                            it.isNotBlank()
                                        },

                                lat =
                                    (item["lat"] as? Number)
                                        ?.toDouble(),

                                lng =
                                    (item["lng"] as? Number)
                                        ?.toDouble(),

                                startsAt =
                                    (item["startsAt"] as? Number)
                                        ?.toLong()
                                        ?: 0L,

                                createdAt =
                                    (item["createdAt"] as? Number)
                                        ?.toLong()
                                        ?: 0L,

                                createdByUid =
                                    createdByUid,

                                createdByName =
                                    item["createdByName"]
                                        ?.toString()
                                        ?.trim()
                                        .orEmpty(),

                                status =
                                    item["status"]
                                        ?.toString()
                                        ?.trim()
                                        .orEmpty()
                                        .ifBlank {
                                            "OPEN"
                                        },

                                goingCount =
                                    (item["goingCount"] as? Number)
                                        ?.toInt()
                                        ?: 0,

                                onWayCount =
                                    (item["onWayCount"] as? Number)
                                        ?.toInt()
                                        ?: 0,

                                arrivedCount =
                                    (item["arrivedCount"] as? Number)
                                        ?.toInt()
                                        ?: 0,

                                cantCount =
                                    (item["cantCount"] as? Number)
                                        ?.toInt()
                                        ?: 0
                            )
                        }
                        .sortedBy {
                            it.startsAt
                        }

                trySend(items)
            }

        awaitClose {
            job.cancel()
        }
    }

    override fun observeParticipants(
        branch: String,
        groupKey: String,
        sessionId: String
    ): Flow<List<FreeSessionPart>> = callbackFlow {

        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        val cleanSessionId =
            sessionId.trim()

        if (
            cleanBranch.isBlank() ||
            cleanGroup.isBlank() ||
            cleanSessionId.isBlank()
        ) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val job =
            kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.Dispatchers.IO
            ).launch {

                val result =
                    runCatching {
                        FirebaseFunctions
                            .getInstance()
                            .getHttpsCallable(
                                "loadSecureFreeSessionParticipants"
                            )
                            .call(
                                mapOf(
                                    "branch" to cleanBranch,
                                    "group" to cleanGroup,
                                    "sessionId" to cleanSessionId
                                )
                            )
                            .await()
                    }

                if (result.isFailure) {
                    trySend(emptyList())
                    return@launch
                }

                val payload =
                    result.getOrNull()
                        ?.data as? Map<*, *>

                val rawItems =
                    payload
                        ?.get("items")
                            as? List<*>
                        ?: emptyList<Any>()

                val items =
                    rawItems
                        .mapNotNull { rawItem ->

                            val item =
                                rawItem as? Map<*, *>
                                    ?: return@mapNotNull null

                            val uid =
                                item["uid"]
                                    ?.toString()
                                    ?.trim()
                                    .orEmpty()

                            val name =
                                item["name"]
                                    ?.toString()
                                    ?.trim()
                                    .orEmpty()

                            if (
                                uid.isBlank() ||
                                name.isBlank()
                            ) {
                                return@mapNotNull null
                            }

                            FreeSessionPart(
                                uid = uid,

                                name = name,

                                state =
                                    ParticipantState.fromId(
                                        item["state"]
                                            ?.toString()
                                    ),

                                updatedAt =
                                    (item["updatedAt"] as? Number)
                                        ?.toLong()
                                        ?: 0L
                            )
                        }
                        .sortedByDescending {
                            it.updatedAt
                        }

                trySend(items)
            }

        awaitClose {
            job.cancel()
        }
    }

    override suspend fun setParticipantState(
        branch: String,
        groupKey: String,
        sessionId: String,
        uid: String,
        name: String,
        state: ParticipantState
    ) {
        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        val cleanSessionId =
            sessionId.trim()

        require(
            cleanBranch.isNotBlank() &&
                    cleanGroup.isNotBlank() &&
                    cleanSessionId.isNotBlank()
        ) {
            "Missing branch, group or sessionId"
        }

        FirebaseFunctions
            .getInstance()
            .getHttpsCallable(
                "setSecureFreeSessionParticipantState"
            )
            .call(
                mapOf(
                    "branch" to cleanBranch,
                    "group" to cleanGroup,
                    "sessionId" to cleanSessionId,
                    "state" to state.name
                )
            )
            .await()
    }

    override suspend fun closeSession(
        branch: String,
        groupKey: String,
        sessionId: String
    ) {
        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        val cleanSessionId =
            sessionId.trim()

        require(
            cleanBranch.isNotBlank() &&
                    cleanGroup.isNotBlank() &&
                    cleanSessionId.isNotBlank()
        ) {
            "Missing branch, group or sessionId"
        }

        FirebaseFunctions
            .getInstance()
            .getHttpsCallable(
                "closeSecureFreeSession"
            )
            .call(
                mapOf(
                    "branch" to cleanBranch,
                    "group" to cleanGroup,
                    "sessionId" to cleanSessionId
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
        val cleanBranch =
            branch.trim()

        val cleanGroup =
            groupKey.trim()

        val cleanSessionId =
            sessionId.trim()

        require(
            cleanBranch.isNotBlank() &&
                    cleanGroup.isNotBlank() &&
                    cleanSessionId.isNotBlank()
        ) {
            "Missing branch, group or sessionId"
        }

        FirebaseFunctions
            .getInstance()
            .getHttpsCallable(
                "deleteSecureFreeSession"
            )
            .call(
                mapOf(
                    "branch" to cleanBranch,
                    "group" to cleanGroup,
                    "sessionId" to cleanSessionId
                )
            )
            .await()
    }
}

