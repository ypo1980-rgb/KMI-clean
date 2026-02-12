package il.kmi.app.search

import il.kmi.shared.domain.Belt

data class AppSearchHit(
    val belt: Belt,
    val topic: String,
    val item: String? = null // null => פגיעה בכותרת נושא
)
