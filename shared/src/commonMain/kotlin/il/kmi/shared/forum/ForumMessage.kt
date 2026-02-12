package il.kmi.shared.forum

import kotlinx.datetime.Instant

/**
 * מודל הודעה חוצה פלטפורמה.
 * אנדרואיד משתמש ב-Firebase Timestamp → מומלץ למפות ל-Instant.
 * iOS יוכל להשתמש גם ב-Instant.
 */
data class ForumMessage(
    val id: String = "",
    val branch: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val text: String = "",
    val createdAt: Instant = Instant.DISTANT_PAST
)
