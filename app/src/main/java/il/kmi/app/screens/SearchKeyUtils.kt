package il.kmi.app.screens

import il.kmi.shared.domain.Belt

internal fun parseSearchKey(key: String): Triple<Belt, String, String> {
    fun decode(s: String): String {
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hex = s.substring(i + 1, i + 3)
                val v = hex.toIntOrNull(16)
                if (v != null) {
                    out.append(v.toChar())
                    i += 3
                    continue
                }
            }
            out.append(if (c == '+') ' ' else c)
            i++
        }
        return out.toString()
    }

    val parts0 = when {
        '|' in key -> key.split('|', limit = 3)
        "::" in key -> key.split("::", limit = 3)
        '/' in key -> key.split('/', limit = 3)
        else -> listOf("", "", "")
    }

    val parts = (parts0 + listOf("", "", "")).take(3)
    val belt = Belt.fromId(parts[0]) ?: Belt.WHITE
    val topic = decode(parts[1])
    val item = decode(parts[2])

    return Triple(belt, topic, item)
}