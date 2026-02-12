package il.kmi.app.domain

import android.util.Log
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo as SharedContentRepo

/**
 * BRIDGE:
 * ה-APP ממשיך לייבא il.kmi.app.domain.ContentRepo,
 * אבל בפועל הכל רץ דרך ה-Shared (Source of Truth).
 */
object ContentRepo {

    // שמרו על אותו מודל שהיה ב-APP (שלא ישבור שימושים קיימים)
    data class SearchHit(
        val id: String? = null,
        val title: String,
        val subtitle: String? = null
    )

    @Volatile private var inited = false

    /**
     * חובה לקרוא לזה פעם אחת בתחילת האפליקציה (Application / MainApp)
     * כדי לוודא ש-SharedContentRepo.data נטען.
     */
    fun initIfNeeded() {
        if (inited) return
        synchronized(this) {
            if (inited) return

            val keysBefore = SharedContentRepo.data.keys
            Log.e("KMI_DBG", "ContentRepo.initIfNeeded() shared keys BEFORE=${keysBefore.size} -> $keysBefore")

            // ✅ ניסיון להפעיל warmup ב-Shared גם אם השם המדויק לא ידוע (reflection-safe)
            tryWarmupSharedRepo()

            val keysAfter = SharedContentRepo.data.keys
            Log.e("KMI_DBG", "ContentRepo.initIfNeeded() shared keys AFTER=${keysAfter.size} -> $keysAfter")

            inited = true
        }
    }

    private fun tryWarmupSharedRepo() {
        val candidates = listOf(
            "init",
            "ensureLoaded",
            "ensureLoadedIfNeeded",
            "warmUp",
            "warmup",
            "build",
            "bootstrap",
            "load",
            "loadIfNeeded"
        )

        val instance = SharedContentRepo
        val cls = instance::class.java

        candidates.forEach { name ->
            runCatching {
                val m = cls.methods.firstOrNull { it.name == name && it.parameterTypes.isEmpty() }
                    ?: cls.declaredMethods.firstOrNull { it.name == name && it.parameterTypes.isEmpty() }
                    ?: return@runCatching

                m.isAccessible = true
                m.invoke(instance)

                Log.e("KMI_DBG", "ContentRepo.initIfNeeded() invoked SharedContentRepo.$name()")
            }
        }
    }

    // --- Forwarders ---

    fun makeItemKey(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String?,
        itemTitle: String
    ): String =
        SharedContentRepo.makeItemKey(
            belt = belt,
            topicTitle = topicTitle,
            subTopicTitle = subTopicTitle,
            itemTitle = itemTitle
        )

    data class ResolvedItem(
        val belt: Belt,
        val topicTitle: String,
        val subTopicTitle: String?,
        val itemTitle: String
    )

    fun resolveItemKey(key: String): ResolvedItem? =
        SharedContentRepo.resolveItemKey(key)?.let {
            ResolvedItem(
                belt = it.belt,
                topicTitle = it.topicTitle,
                subTopicTitle = it.subTopicTitle,
                itemTitle = it.itemTitle
            )
        }

    fun listBeltsInOrder(): List<Belt> = Belt.order

    fun listTopicTitles(belt: Belt): List<String> =
        SharedContentRepo.data[belt]?.topics?.map { it.title }.orEmpty()

    fun listSubTopicTitles(belt: Belt, topicTitle: String): List<String> =
        SharedContentRepo.getSubTopicTitles(belt, topicTitle)

    fun listItemTitles(
        belt: Belt,
        topicTitle: String,
        subTopicTitle: String?
    ): List<String> =
        SharedContentRepo.getAllItemsFor(belt, topicTitle, subTopicTitle)

    fun findExerciseByName(name: String): String? =
        SharedContentRepo.findExerciseByName(name)

    fun findSubTopicTitleForItem(
        belt: Belt,
        topicTitle: String,
        itemTitle: String
    ): String? =
        SharedContentRepo.findSubTopicTitleForItem(belt, topicTitle, itemTitle)

    fun searchExercises(query: String): List<SearchHit> =
        SharedContentRepo.searchExercises(query).map {
            SearchHit(
                id = it.id,
                title = it.title,
                subtitle = it.subtitle
            )
        }
}
