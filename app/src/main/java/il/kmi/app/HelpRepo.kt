package il.kmi.app

import il.kmi.shared.domain.Belt
object HelpRepo {
    private val data: Map<String, Map<String, Map<String, String>>> = mapOf(
        "yellow" to mapOf(
            "עבודת ידיים" to mapOf(
                "אגרוף שמאל / ימין לפנים / בהתקדמות" to "זהו תרגיל יסוד באמנות לחימה. עמדו בעמידת קרב, ימין מאחור. בעת ביצוע האגרוף, בצעו סיבוב מהיר של הירך והכתף הקדמית קדימה. כף היד של היד הנגדית צריכה להישאר צמודה לפנים כדי להגן על הסנטר.",
                "מכת מגל שמאל / ימין" to "מכה זו מגיעה מהצד בתנועה קשתית. התנועה מתחילה עם הירך והכתף הנגדית, ומסתיימת עם הטיית פנים האגרוף כלפי מעלה. חשוב לשמור על יציבות ולא להישען קדימה יותר מדי."
            )
        )
    )

    fun helpTextFor(belt: Belt, topic: String, item: String): String {
        return data[belt.id]?.get(topic)?.get(item)
            ?: "בקרוב יתווסף הסבר לתרגיל \"$item\"."
    }
}