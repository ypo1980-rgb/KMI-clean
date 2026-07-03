package il.kmi.shared.free_sessions.data

import il.kmi.shared.free_sessions.model.FreeSession
import il.kmi.shared.free_sessions.model.FreeSessionPart
import il.kmi.shared.free_sessions.model.ParticipantState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlin.Double
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

private class IosFreeSessionsRepository : FreeSessionsRepository {

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
        return "ios_free_session_${Clock.System.now().toEpochMilliseconds()}"
    }

    override fun observeUpcoming(
        branch: String,
        groupKey: String,
        nowMillis: Long
    ): Flow<List<FreeSession>> {
        return flowOf(emptyList())
    }

    override fun observeParticipants(
        branch: String,
        groupKey: String,
        sessionId: String
    ): Flow<List<FreeSessionPart>> {
        return flowOf(emptyList())
    }

    override suspend fun setParticipantState(
        branch: String,
        groupKey: String,
        sessionId: String,
        uid: String,
        name: String,
        state: ParticipantState
    ): Unit {
        // iOS placeholder
    }

    override suspend fun closeSession(
        branch: String,
        groupKey: String,
        sessionId: String
    ): Unit {
        // iOS placeholder
    }

    override suspend fun deleteFreeSession(
        branch: String,
        groupKey: String,
        sessionId: String
    ): Unit {
        // iOS placeholder
    }
}

actual fun freeSessionsRepository(): FreeSessionsRepository {
    return IosFreeSessionsRepository()
}

actual fun systemNowMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}