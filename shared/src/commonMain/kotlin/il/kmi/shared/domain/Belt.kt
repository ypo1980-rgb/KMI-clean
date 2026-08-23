package il.kmi.shared.domain

// קובץ נקי – בלי Compose

enum class Belt(
    val id: String,
    val heb: String,
    val en: String,
    val colorArgb: Long,
    val lightColorArgb: Long
) {
    WHITE ("white",  "חגורה לבנה",  "White Belt",  0xFFFFFFFF, 0xFFF5F5F5),
    YELLOW("yellow", "חגורה צהובה", "Yellow Belt", 0xFFFFD54F, 0xFFFFFDE7),
    ORANGE("orange", "חגורה כתומה", "Orange Belt", 0xFFFFA726, 0xFFFFF3E0),
    GREEN ("green",  "חגורה ירוקה", "Green Belt",  0xFF66BB6A, 0xFFE8F5E9),
    BLUE  ("blue",   "חגורה כחולה", "Blue Belt",   0xFF42A5F5, 0xFFE3F2FD),
    BROWN ("brown",  "חגורה חומה",  "Brown Belt",  0xFF8D6E63, 0xFFEFEBE9),
    BLACK ("black",  "חגורה שחורה", "Black Belt",  0xFF212121, 0xFFBDBDBD);

    companion object {

        val order: List<Belt> =
            listOf(
                WHITE,
                YELLOW,
                ORANGE,
                GREEN,
                BLUE,
                BROWN,
                BLACK
            )

        fun nextOf(belt: Belt): Belt? =
            order.getOrNull(
                order.indexOf(belt) + 1
            )

        private fun normalize(raw: Any?): String {
            return raw
                ?.toString()
                ?.trim()
                ?.lowercase()
                ?.replace('־', '-')
                ?.replace('–', '-')
                ?.replace('—', '-')
                ?.replace('_', ' ')
                ?.replace('-', ' ')
                ?.replace(Regex("\\s+"), " ")
                .orEmpty()
        }

        fun fromId(id: Any?): Belt? {
            val raw = normalize(id)

            if (raw.isBlank()) {
                return null
            }

            // התאמה רגילה לפי ID
            order.firstOrNull { belt ->
                normalize(belt.id) == raw
            }?.let {
                return it
            }

            /*
             * חגורה שחורה עם דרגת דאן.
             *
             * דוגמאות נתמכות:
             * black dan 1
             * black_dan_5
             * black-dan-10
             * dan 5
             * dan_5
             */
            val isBlackDan =
                raw.matches(
                    Regex(
                        """(?:black\s*)?(?:dan)\s*(10|[1-9])"""
                    )
                )

            if (isBlackDan) {
                return BLACK
            }

            return null
        }

        fun fromHeb(heb: String?): Belt? {
            val raw = normalize(heb)

            if (raw.isBlank()) {
                return null
            }

            // התאמה רגילה לשם החגורה בעברית
            order.firstOrNull { belt ->
                normalize(belt.heb) == raw
            }?.let {
                return it
            }

            /*
             * חגורה שחורה דאן 1–10 בעברית.
             *
             * דוגמאות:
             * שחורה דאן 5
             * חגורה שחורה דאן 5
             * שחורה דן 5
             * חגורה שחורה דן 10
             */
            val isBlackDan =
                (
                        raw.contains("שחור") &&
                                (
                                        raw.contains("דאן") ||
                                                raw.contains("דן")
                                        )
                        ) &&
                        Regex("""(?:^|\s)(10|[1-9])(?:\s|$)""")
                            .containsMatchIn(raw)

            if (isBlackDan) {
                return BLACK
            }

            return null
        }

        fun fromAny(v: String?): Belt? {
            return fromId(v)
                ?: fromHeb(v)
        }

        fun nextOfAny(v: String?): Belt? =
            fromAny(v)?.let { belt ->
                nextOf(belt)
            }

        fun isLast(b: Belt): Boolean =
            order.lastOrNull() == b

        fun indexOf(b: Belt?): Int =
            if (b == null) {
                -1
            } else {
                order.indexOf(b)
            }
    }
}