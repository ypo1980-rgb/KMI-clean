package il.kmi.app.search

import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.shared.model.KmiBelt
import il.kmi.shared.model.KmiBeltContent
import il.kmi.shared.model.KmiSubTopic
import il.kmi.shared.model.KmiTopic

/* ------------------------------------------------------------
   רפלקציה בטוחה – בלי תלות בטיפוסים דומייניים ספציפיים
   ------------------------------------------------------------ */
private fun readField(obj: Any, name: String): Any? = runCatching {
    obj.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(obj)
}.getOrNull()

@Suppress("UNCHECKED_CAST")
private fun readList(any: Any?, name: String): List<Any> =
    (runCatching {
        (any ?: return emptyList())
            .javaClass.getDeclaredField(name).apply { isAccessible = true }
            .get(any) as? List<*>
    }.getOrNull() ?: emptyList<Any?>()).filterNotNull().map { it as Any }

/* ------------------------------------------------------------
   מיפוי Belt <-> KmiBelt
   ------------------------------------------------------------ */
fun Belt.toShared(): KmiBelt = when (this) {
    Belt.WHITE  -> KmiBelt.WHITE
    Belt.YELLOW -> KmiBelt.YELLOW
    Belt.ORANGE -> KmiBelt.ORANGE
    Belt.GREEN  -> KmiBelt.GREEN
    Belt.BLUE   -> KmiBelt.BLUE
    Belt.BROWN  -> KmiBelt.BROWN
    Belt.BLACK  -> KmiBelt.BLACK
}

fun KmiBelt.toApp(): Belt = when (this) {
    KmiBelt.WHITE  -> Belt.WHITE
    KmiBelt.YELLOW -> Belt.YELLOW
    KmiBelt.ORANGE -> Belt.ORANGE
    KmiBelt.GREEN  -> Belt.GREEN
    KmiBelt.BLUE   -> Belt.BLUE
    KmiBelt.BROWN  -> Belt.BROWN
    KmiBelt.BLACK  -> Belt.BLACK
}

/* ------------------------------------------------------------
   ממפים אובייקטים “כלליים” (Any) של התוכן למודלים המשותפים
   ------------------------------------------------------------ */
private fun toSharedSubTopic(subAny: Any): KmiSubTopic =
    KmiSubTopic(
        title = readField(subAny, "title") as? String ?: "",
        items = readList(subAny, "items").map { it.toString() }
    )

private fun toSharedTopic(topicAny: Any): KmiTopic {
    val subTopics = readList(topicAny, "subTopics")
    return if (subTopics.isNotEmpty()) {
        KmiTopic(
            title = readField(topicAny, "title") as? String ?: "",
            subTopics = subTopics.map { toSharedSubTopic(it) }
        )
    } else {
        KmiTopic(
            title = readField(topicAny, "title") as? String ?: "",
            items = readList(topicAny, "items").map { it.toString() }
        )
    }
}

private fun toSharedBeltContent(contentAny: Any): KmiBeltContent =
    KmiBeltContent(
        topics = readList(contentAny, "topics").map { toSharedTopic(it) }
    )

/* ------------------------------------------------------------
   התאמה של ContentRepo כולו למבנה המשותף לחיפוש (KMP)
   תומך גם במצב שהשדה data הוא top-level (ContentRepoKt)
   ------------------------------------------------------------ */
fun ContentRepo.asSharedRepo(): Map<KmiBelt, KmiBeltContent> {
    // 1) ניסיון בתוך object ContentRepo (שדה/גטר פנימי)
    val dataAnyFromObject = runCatching {
        ContentRepo::class.java
            .getDeclaredField("data")
            .apply { isAccessible = true }
            .get(ContentRepo)
    }.getOrNull()

    // 2) פולבאק: data כ־top-level בקובץ ContentRepo.kt (מחלקת ContentRepoKt)
    val dataAnyTopLevel = if (dataAnyFromObject == null) {
        runCatching {
            val topLevel = Class.forName("il.kmi.app.domain.ContentRepoKt")
            topLevel.getDeclaredField("data").apply { isAccessible = true }.get(null)
        }.getOrNull()
    } else null

    val dataAny = dataAnyFromObject ?: dataAnyTopLevel
    val dataMap = dataAny as? Map<*, *> ?: emptyMap<Any?, Any?>()

    val out = mutableMapOf<KmiBelt, KmiBeltContent>()
    for ((beltAny, contentAny) in dataMap) {
        val belt = beltAny as? Belt ?: continue
        if (contentAny != null) {
            out[belt.toShared()] = toSharedBeltContent(contentAny)
        }
    }
    return out
}

/* ------------------------------------------------------------
   עזר: זיהוי חגורה לפי התוכן בפועל (מונע נפילה קבועה ל־WHITE)
   שימושי ב-KmiSearchBridge בעת מיפוי תוצאות מנוע החיפוש.
   ------------------------------------------------------------ */
internal fun resolveBeltByContent(
    topicTitle: String,
    itemTitle: String,
    hint: Belt? = null
): Belt {

    fun existsInBelt(b: Belt): Boolean {
        // 1) תרגילים ישירים
        val direct = runCatching {
            ContentRepo.listItemTitles(
                belt = b,
                topicTitle = topicTitle,
                subTopicTitle = null
            )
        }.getOrDefault(emptyList())

        if (direct.any { it == itemTitle }) return true

        // 2) תרגילים דרך תתי-נושאים
        val subTitles = runCatching {
            ContentRepo.listSubTopicTitles(b, topicTitle)
        }.getOrDefault(emptyList())

        if (subTitles.isEmpty()) return false

        return subTitles.any { stTitle ->
            val items = runCatching {
                ContentRepo.listItemTitles(
                    belt = b,
                    topicTitle = topicTitle,
                    subTopicTitle = stTitle
                )
            }.getOrDefault(emptyList())

            items.any { it == itemTitle }
        }
    }

    // קודם ננסה hint
    if (hint != null && existsInBelt(hint)) return hint

    // ואז סריקה מלאה
    for (b in Belt.values()) {
        if (existsInBelt(b)) return b
    }

    return hint ?: Belt.WHITE
}
