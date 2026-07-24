package il.kmi.app.domain

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

            // ✅ ניסיון להפעיל warmup ב-Shared גם אם השם המדויק לא ידוע (reflection-safe)
            tryWarmupSharedRepo()

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

    fun listTopicTitles(belt: Belt): List<String> {
        initIfNeeded()

        val cls = SharedContentRepo::class.java

        val directResult = runCatching {
            val method = cls.methods.firstOrNull { method ->
                method.name in listOf(
                    "listTopicTitles",
                    "getTopicTitles",
                    "topicTitlesFor",
                    "topicsForBelt"
                ) && method.parameterTypes.size == 1
            } ?: return@runCatching emptyList<String>()

            val result = method.invoke(SharedContentRepo, belt)

            when (result) {
                is Iterable<*> -> result.mapNotNull { item ->
                    item?.toString()?.trim()?.takeIf { it.isNotBlank() }
                }

                is Array<*> -> result.mapNotNull { item ->
                    item?.toString()?.trim()?.takeIf { it.isNotBlank() }
                }

                else -> emptyList()
            }
        }.getOrElse {
            emptyList()
        }

        if (directResult.isNotEmpty()) {
            return directResult.distinct()
        }

        val dataValue = runCatching {
            cls.methods.firstOrNull { method ->
                method.name == "getData" && method.parameterTypes.isEmpty()
            }?.invoke(SharedContentRepo)
        }.getOrNull()

        val beltData = when (dataValue) {
            is Map<*, *> -> dataValue[belt] ?: dataValue[belt.id] ?: dataValue[belt.name]
            else -> null
        } ?: return emptyList()

        val topicsValue = runCatching {
            beltData.javaClass.methods.firstOrNull { method ->
                method.name in listOf("getTopics", "topics") && method.parameterTypes.isEmpty()
            }?.invoke(beltData)
        }.getOrNull()

        return when (topicsValue) {
            is Iterable<*> -> topicsValue.mapNotNull { topic ->
                runCatching {
                    topic
                        ?.javaClass
                        ?.methods
                        ?.firstOrNull { method ->
                            method.name in listOf("getTitle", "title") && method.parameterTypes.isEmpty()
                        }
                        ?.invoke(topic)
                        ?.toString()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }.getOrNull()
            }

            is Array<*> -> topicsValue.mapNotNull { topic ->
                runCatching {
                    topic
                        ?.javaClass
                        ?.methods
                        ?.firstOrNull { method ->
                            method.name in listOf("getTitle", "title") && method.parameterTypes.isEmpty()
                        }
                        ?.invoke(topic)
                        ?.toString()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }.getOrNull()
            }

            else -> emptyList()
        }.distinct()
    }

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

    /**
     * תרגיל שנאסף ישירות ממאגר התוכן המרכזי.
     */
    data class ExerciseOption(
        val belt: Belt,
        val topicTitle: String,
        val subTopicTitle: String?,
        val itemTitle: String
    )

    /**
     * מחזיר את כל התרגילים ששמם מכיל את כל מילות החיפוש.
     *
     * החיפוש מתבצע ישירות במקור האמת ואינו תלוי
     * בדירוג או בתוצאה הראשונה של מנגנון החיפוש.
     */
    fun findExerciseOptionsContainingAll(
        requiredTerms: List<String>
    ): List<ExerciseOption> {
        initIfNeeded()

        val cleanTerms =
            requiredTerms
                .map { term ->
                    normalizeExerciseSearchText(term)
                }
                .filter { term ->
                    term.isNotBlank()
                }
                .distinct()

        if (cleanTerms.isEmpty()) {
            return emptyList()
        }

        return buildList {
            listBeltsInOrder().forEach { belt ->
                listTopicTitles(belt).forEach { topicTitle ->
                    /*
                     * קוראים גם את פריטי הנושא הראשי וגם את
                     * הפריטים שנמצאים בכל אחד מתתי־הנושאים.
                     */
                    val subTopics =
                        listOf<String?>(null) +
                                listSubTopicTitles(
                                    belt = belt,
                                    topicTitle = topicTitle
                                )

                    subTopics
                        .distinct()
                        .forEach { subTopicTitle ->
                            listItemTitles(
                                belt = belt,
                                topicTitle = topicTitle,
                                subTopicTitle = subTopicTitle
                            )
                                .forEach { itemTitle ->
                                    val normalizedItem =
                                        normalizeExerciseSearchText(
                                            itemTitle
                                        )

                                    val containsAllTerms =
                                        cleanTerms.all { term ->
                                            normalizedItem.contains(term)
                                        }

                                    if (containsAllTerms) {
                                        add(
                                            ExerciseOption(
                                                belt = belt,
                                                topicTitle = topicTitle,
                                                subTopicTitle =
                                                    subTopicTitle,
                                                itemTitle = itemTitle
                                            )
                                        )
                                    }
                                }
                        }
                }
            }
        }
            .distinctBy { option ->
                listOf(
                    option.belt.name,
                    option.topicTitle,
                    option.subTopicTitle.orEmpty(),
                    option.itemTitle
                ).joinToString("|")
            }
    }

    private fun normalizeExerciseSearchText(
        text: String
    ): String {
        return text
            .lowercase()
            .replace("־", " ")
            .replace("–", " ")
            .replace("—", " ")
            .replace("-", " ")
            .replace("\"", " ")
            .replace("'", " ")
            .replace("?", " ")
            .replace("!", " ")
            .replace("(", " ")
            .replace(")", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun readStringProperty(
        target: Any?,
        names: List<String>
    ): String {
        if (target == null) return ""

        return names.firstNotNullOfOrNull { name ->
            runCatching {
                target
                    .javaClass
                    .methods
                    .firstOrNull { method ->
                        method.name == name && method.parameterTypes.isEmpty()
                    }
                    ?.invoke(target)
                    ?.toString()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }.orEmpty()
    }

    fun searchExercises(query: String): List<SearchHit> {
        initIfNeeded()

        val rawResults: Any? = runCatching<Any?> {
            SharedContentRepo.searchExercises(query)
        }.getOrNull()

        val items: List<Any?> = when (rawResults) {
            is Iterable<*> -> rawResults.toList()
            is Array<*> -> rawResults.toList()
            null -> emptyList()
            else -> listOf(rawResults)
        }

        return items.mapNotNull { item ->
            val title = readStringProperty(
                target = item,
                names = listOf("getTitle", "title")
            )

            if (title.isBlank()) {
                null
            } else {
                SearchHit(
                    id = readStringProperty(
                        target = item,
                        names = listOf("getId", "id")
                    ).ifBlank { null },
                    title = title,
                    subtitle = readStringProperty(
                        target = item,
                        names = listOf("getSubtitle", "subtitle")
                    ).ifBlank { null }
                )
            }
        }
    }
}
