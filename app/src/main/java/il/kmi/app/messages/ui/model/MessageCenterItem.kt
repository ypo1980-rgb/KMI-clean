package il.kmi.app.messages.model

data class MessageCenterItem(
    val id: String,
    val titleHe: String,
    val titleEn: String,
    val message: String,
    val createdAtMillis: Long,
    val read: Boolean,
    val deleted: Boolean = false,
    val senderNameHe: String = "צוות ק.מ.י",
    val senderNameEn: String = "K.M.I Team",
    val region: String = "",
    val branch: String = "",
    val groups: List<String> = emptyList()
)