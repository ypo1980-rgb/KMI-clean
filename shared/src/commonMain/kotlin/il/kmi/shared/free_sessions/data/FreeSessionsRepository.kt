package il.kmi.shared.free_sessions.data

import il.kmi.shared.free_sessions.model.FreeSession
import il.kmi.shared.free_sessions.model.FreeSessionPart
import il.kmi.shared.free_sessions.model.ParticipantState
import kotlinx.coroutines.flow.Flow

interface FreeSessionsRepository {

    /**
     * יצירת אימון עתידי (OPEN).
     * מחזיר sessionId (docId).
     */
    suspend fun createFreeSession(
        branch: String,
        groupKey: String,
        title: String,
        locationName: String?,
        lat: Double?,
        lng: Double?,
        startsAt: Long,
        createdByUid: String,
        createdByName: String
    ): String

    /**
     * צפייה באימונים עתידיים (OPEN) מהיום והלאה.
     */
    fun observeUpcoming(
        branch: String,
        groupKey: String,
        nowMillis: Long = systemNowMillis()
    ): Flow<List<FreeSession>>

    /**
     * צפייה במשתתפים של אימון מסוים
     */
    fun observeParticipants(
        branch: String,
        groupKey: String,
        sessionId: String
    ): Flow<List<FreeSessionPart>>

    /**
     * שינוי סטטוס משתתף (GOING / ON_WAY / ARRIVED / CANT)
     */
    suspend fun setParticipantState(
        branch: String,
        groupKey: String,
        sessionId: String,
        uid: String,
        name: String,
        state: ParticipantState
    )

    /**
     * סגירת אימון (CLOSED) – כולם עדיין רואים אותו בהיסטוריה אם תרצה, אבל לא יופיע ב-Upcoming
     */
    suspend fun closeSession(
        branch: String,
        groupKey: String,
        sessionId: String
    )

    // ✅ NEW
    suspend fun deleteFreeSession(
        branch: String,
        groupKey: String,
        sessionId: String
    )
}

/** Factory (expect/actual) */
expect fun freeSessionsRepository(): FreeSessionsRepository

/** זמן מערכת בקומון (לשימוש בדפדוף Upcoming) */
expect fun systemNowMillis(): Long
