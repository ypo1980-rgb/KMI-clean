package il.kmi.app.catalog

import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.shared.catalog.BeltDto
import il.kmi.shared.catalog.ExerciseContentDto
import il.kmi.shared.catalog.TopicDto
import il.kmi.shared.catalog.SubTopicDto
import il.kmi.shared.catalog.ExerciseDto
import il.kmi.shared.catalog.InMemoryCatalog

object CatalogBootstrapper {

    fun bootstrapFromContentRepo() {

        // ✅ חשוב: לוודא שה-SharedContentRepo נטען לפני שמוציאים רשימות
        runCatching { ContentRepo.initIfNeeded() }

        // 1) Belts
        val belts: List<Belt> = ContentRepo.listBeltsInOrder()
        InMemoryCatalog.setBelts(
            belts.mapIndexed { index, belt ->
                BeltDto(
                    id = belt.name.lowercase(),
                    title = belt.heb,
                    order = index + 1
                )
            }
        )

        // 2) Topics / SubTopics / Items
        belts.forEach { belt ->
            val beltId = belt.name.lowercase()

            val topicTitles = ContentRepo.listTopicTitles(belt)

            // ✅ ID צפוי: נגזר מהכותרת עצמה (כדי להתאים למסכים/ניווט שמסתמכים על title/ids פשוטים)
            InMemoryCatalog.setTopics(
                beltId = beltId,
                list = topicTitles.map { title ->
                    TopicDto(
                        id = normId(title),
                        title = title
                    )
                }
            )

            topicTitles.forEach { topicTitle ->
                val topicId = normId(topicTitle)

                // SubTopics
                val subTitles = ContentRepo.listSubTopicTitles(belt, topicTitle)
                InMemoryCatalog.setSubTopics(
                    beltId = beltId,
                    topicId = topicId,
                    list = subTitles.map { st ->
                        SubTopicDto(
                            id = normId(st),
                            title = st
                        )
                    }
                )

                // Direct items (topic-level)
                val directItems =
                    ContentRepo.listItemTitles(belt, topicTitle, subTopicTitle = null)

                if (directItems.isNotEmpty()) {
                    InMemoryCatalog.setExercises(
                        beltId = beltId,
                        topicId = topicId,
                        subTopicId = null,
                        list = directItems.map { itemTitle ->
                            val id = ContentRepo.makeItemKey(
                                belt = belt,
                                topicTitle = topicTitle,
                                subTopicTitle = null,
                                itemTitle = itemTitle
                            )

                            // ✅ גם תוכן HTML אם קיים
                            ExerciseHtmlProvider
                                .tryGetHtmlForExerciseId(id)
                                ?.let { html ->
                                    InMemoryCatalog.setExerciseContent(
                                        ExerciseContentDto(
                                            id = id,
                                            title = itemTitle,
                                            mimeType = "text/html",
                                            contents = html
                                        )
                                    )
                                }

                            ExerciseDto(
                                id = id,
                                title = itemTitle
                            )
                        }
                    )
                }

                // SubTopic items
                subTitles.forEach { stTitle ->
                    val subId = normId(stTitle)

                    val items =
                        ContentRepo.listItemTitles(belt, topicTitle, subTopicTitle = stTitle)

                    if (items.isNotEmpty()) {
                        InMemoryCatalog.setExercises(
                            beltId = beltId,
                            topicId = topicId,
                            subTopicId = subId,
                            list = items.map { itemTitle ->
                                val id = ContentRepo.makeItemKey(
                                    belt = belt,
                                    topicTitle = topicTitle,
                                    subTopicTitle = stTitle,
                                    itemTitle = itemTitle
                                )

                                // ✅ קריטי: עד עכשיו לא הוזן HTML ל-subTopic exercises
                                ExerciseHtmlProvider
                                    .tryGetHtmlForExerciseId(id)
                                    ?.let { html ->
                                        InMemoryCatalog.setExerciseContent(
                                            ExerciseContentDto(
                                                id = id,
                                                title = itemTitle,
                                                mimeType = "text/html",
                                                contents = html
                                            )
                                        )
                                    }

                                ExerciseDto(
                                    id = id,
                                    title = itemTitle
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun normId(raw: String): String = raw
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), "_")
        .replace(Regex("[^a-z0-9\\u0590-\\u05FF_\\-]+"), "")
        .replace(Regex("_+"), "_")
        .trim('_', '-')
}
