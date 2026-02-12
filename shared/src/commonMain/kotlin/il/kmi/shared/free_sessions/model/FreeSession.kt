package il.kmi.shared.free_sessions.model

data class FreeSession(
    val id: String,
    val branch: String,
    val groupKey: String,

    val title: String,
    val locationName: String?,
    val lat: Double?,
    val lng: Double?,

    val startsAt: Long,     // epoch millis
    val createdAt: Long,    // epoch millis

    val createdByUid: String,
    val createdByName: String,

    val status: String,     // "OPEN" / "CLOSED"

    // ספירות (נוחות ל-UI; מחושבות מהמשתתפים/או נשמרות)
    val goingCount: Int = 0,
    val onWayCount: Int = 0,
    val arrivedCount: Int = 0,
    val cantCount: Int = 0
)
