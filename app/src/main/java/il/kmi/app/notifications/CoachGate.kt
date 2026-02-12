package il.kmi.app.notifications

object CoachGate {
    const val EXTRA_OPEN = "kmi_open_coach_gate"
    const val EXTRA_BROADCAST_ID = "kmi_broadcast_id"
    const val EXTRA_TEXT = "kmi_broadcast_text"
    const val EXTRA_FROM = "kmi_broadcast_from"
    const val EXTRA_SENT_AT = "kmi_broadcast_sent_at"

    // fallback storage (כשאין לנו גישה קלה ל-NavController מתוך Activity בזמן onCreate)
    const val SP_NAME = "kmi_gate"
    const val SP_HAS_PENDING = "has_pending"
    const val SP_TEXT = "text"
    const val SP_FROM = "from"
    const val SP_SENT_AT = "sent_at"
    const val SP_BROADCAST_ID = "broadcast_id"
}
