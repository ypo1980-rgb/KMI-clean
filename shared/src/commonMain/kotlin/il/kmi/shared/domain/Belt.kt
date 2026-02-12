package il.kmi.shared.domain

// קובץ נקי – בלי Compose

enum class Belt(
    val id: String,
    val heb: String,
    val colorArgb: Long,
    val lightColorArgb: Long
) {
    WHITE ("white",  "חגורה לבנה",  0xFFFFFFFF, 0xFFF5F5F5),
    YELLOW("yellow", "חגורה צהובה", 0xFFFFD54F, 0xFFFFFDE7),
    ORANGE("orange", "חגורה כתומה", 0xFFFFA726, 0xFFFFF3E0),
    GREEN ("green",  "חגורה ירוקה", 0xFF66BB6A, 0xFFE8F5E9),
    BLUE  ("blue",   "חגורה כחולה", 0xFF42A5F5, 0xFFE3F2FD),
    BROWN ("brown",  "חגורה חומה",  0xFF8D6E63, 0xFFEFEBE9),
    BLACK ("black",  "חגורה שחורה", 0xFF212121, 0xFFBDBDBD);

    companion object {
        val order: List<Belt> = listOf(WHITE, YELLOW, ORANGE, GREEN, BLUE, BROWN, BLACK)

        fun nextOf(belt: Belt): Belt? =
            order.getOrNull(order.indexOf(belt) + 1)

        fun fromId(id: Any?): Belt? =
            order.firstOrNull { it.id.equals(id?.toString(), ignoreCase = true) }

        fun fromHeb(heb: String?): Belt? =
            order.firstOrNull { it.heb == heb?.trim() }

        fun fromAny(v: String?): Belt? = fromId(v) ?: fromHeb(v)

        fun nextOfAny(v: String?): Belt? = fromAny(v)?.let { nextOf(it) }

        fun isLast(b: Belt): Boolean = order.lastOrNull() == b

        fun indexOf(b: Belt?): Int = if (b == null) -1 else order.indexOf(b)
    }
}
