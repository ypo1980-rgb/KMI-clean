package il.kmi.app.screens.BeltQuestions.ByTopic

import il.kmi.app.domain.ContentRepo
import il.kmi.app.domain.SubjectTopic
import il.kmi.app.domain.TopicsBySubjectRegistry
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.HardSectionsCatalog
import il.kmi.shared.domain.SubjectTopic as SharedSubjectTopic
import il.kmi.shared.domain.content.SubjectItemsResolver
import il.kmi.shared.domain.content.SubjectItemsResolver.UiSection

internal object SubjectTopicsEngine {

    data class ByTopicCoverageReport(
        val total: Int,
        val found: Int,
        val missing: List<ContentRepo.ExerciseOption>
    ) {
        val missingCount: Int
            get() = missing.size

        val coveragePercent: Double
            get() =
                if (total == 0) {
                    100.0
                } else {
                    found.toDouble() * 100.0 / total.toDouble()
                }
    }

    private fun normalizeCoverageText(
        raw: String
    ): String {
        return raw
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")

            // מקפים
            .replace("–", "-")
            .replace("—", "-")
            .replace("־", "-")

            // וריאנטים ידועים בין מקור האמת לקטלוג לפי נושא
            .replace("מס'", "מספר")
            .replace("לכיון", "לכיוון")
            .replace("מחנקה", "מחניקה")
            .replace("בסבוב", "בסיבוב")
            .replace("נגד ההתנגדות", "נגד התנגדות")
            .replace("שילובי ידיים ורגליים", "שילובי ידיים רגליים")

            // prefixes שקיימים ב-HardSectionsCatalog
            .replace(
                Regex(
                    """^\s*מכות במקל קצר\s*-\s*""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(
                Regex(
                    """^\s*מכות במקל\s*/\s*רובה\s*-\s*""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )

            // רווחים סביב /
            .replace(
                Regex("""\s*/\s*"""),
                "/"
            )

            // רווחים סביב מקף
            .replace(
                Regex("""\s*-\s*"""),
                " - "
            )

            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }

    /**
     * בודק אילו תרגילים ממקור האמת נגישים דרך
     * SubjectTopicsEngine של מסך "תרגילים לפי נושא".
     */
    fun auditByTopicCoverage(
        onProgress: ((checked: Int, total: Int) -> Unit)? = null
    ): ByTopicCoverageReport {

        ContentRepo.initIfNeeded()

        val allExercises =
            ContentRepo
                .listAllExerciseOptions()
                .distinctBy { exercise ->
                    listOf(
                        exercise.belt.name,
                        normalizeCoverageText(exercise.topicTitle),
                        normalizeCoverageText(exercise.itemTitle)
                    ).joinToString("|")
                }

        /*
  * כל התרגילים שנגישים בפועל במסלול "לפי נושא".
  *
  * 1. נושאים רגילים דרך SubjectItemsResolver.
  * 2. נושאים קשיחים דרך HardSectionsCatalog.
  */
        val resolvedItems =
            buildList<Pair<Belt, String>> {

                // -------------------------------------------------
                // נושאים רגילים
                // -------------------------------------------------
                TopicsBySubjectRegistry
                    .allSubjects()
                    .forEach { subject ->

                        subject.topicsByBelt.keys.forEach { belt ->

                            resolveSectionsForSubject(
                                belt = belt,
                                subject = subject
                            )
                                .forEach { section ->
                                    section.items.forEach { item ->

                                        val resolved =
                                            ContentRepo.resolveItemKey(
                                                item.canonicalId
                                            )

                                        val title =
                                            resolved
                                                ?.itemTitle
                                                ?.trim()
                                                .orEmpty()

                                        if (title.isNotBlank()) {
                                            add(
                                                belt to title
                                            )
                                        }
                                    }
                                }
                        }
                    }

                // -------------------------------------------------
                // נושאים קשיחים:
                // הגנות / סכין / אקדח / מקל / בעיטות / שחרורים וכו'
                // -------------------------------------------------
                HardSectionsCatalog
                    .supportedSubjectIds
                    .forEach { subjectId ->

                        Belt.order.forEach { belt ->

                            HardSectionsCatalog
                                .subjectItemsFor(
                                    subjectId = subjectId,
                                    belt = belt
                                )
                                .forEach { itemTitle ->

                                    val cleanTitle =
                                        itemTitle.trim()

                                    if (cleanTitle.isNotBlank()) {
                                        add(
                                            belt to cleanTitle
                                        )
                                    }
                                }
                        }
                    }
            }
                .distinctBy { (belt, title) ->
                    "${belt.name}|${normalizeCoverageText(title)}"
                }

        val missing =
            mutableListOf<ContentRepo.ExerciseOption>()

        allExercises.forEachIndexed { index, exercise ->

            val expectedTitle =
                normalizeCoverageText(
                    exercise.itemTitle
                )

            val found =
                resolvedItems.any { (belt, itemTitle) ->

                    belt == exercise.belt &&
                            normalizeCoverageText(
                                itemTitle
                            ) == expectedTitle
                }

            if (!found) {
                missing += exercise
            }

            onProgress?.invoke(
                index + 1,
                allExercises.size
            )
        }

        return ByTopicCoverageReport(
            total = allExercises.size,
            found = allExercises.size - missing.size,
            missing = missing
        )
    }

    fun resolveSectionsForSubject(
        belt: Belt,
        subject: SubjectTopic
    ): List<UiSection> {
        return SubjectItemsResolver.resolveBySubject(
            belt = belt,
            subject = subject.toSharedSubject()
        )
    }

    fun countUiTitlesForSubject(subject: SubjectTopic): Int {
        val all = mutableSetOf<String>()

        subject.topicsByBelt.keys.forEach { belt ->
            resolveSectionsForSubject(belt, subject)
                .asSequence()
                .flatMap { it.items.asSequence() }
                .map { it.canonicalId }
                .forEach { all += it }
        }

        return all.size
    }

    fun beltsWithItemsForSubject(subject: SubjectTopic): List<Belt> {
        return subject.topicsByBelt.keys
            .asSequence()
            .filter { belt ->
                resolveSectionsForSubject(belt, subject)
                    .asSequence()
                    .flatMap { it.items.asSequence() }
                    .any()
            }
            .toList()
    }

    fun handsSubjectForPick(base: SubjectTopic, pick: String): SubjectTopic {
        val p = pick.trim()

        return when (p) {
            "מכות יד" -> base.copy(
                titleHeb = "${base.titleHeb} - $p",
                subTopicHint = p,
                topicsByBelt = mapOf(
                    Belt.YELLOW to listOf("עבודת ידיים", "מכות ידיים", "מכות יד"),
                    Belt.ORANGE to listOf("עבודת ידיים", "מכות יד", "מכות ידיים")
                )
            )

            "מכות מרפק" -> base.copy(
                titleHeb = "${base.titleHeb} - $p",
                subTopicHint = "מרפק",
                topicsByBelt = mapOf(
                    Belt.YELLOW to listOf("מכות מרפק")
                )
            )

            else -> base.copy(
                titleHeb = "${base.titleHeb} - $p",
                subTopicHint = p
            )
        }
    }

    fun subjectForPick(base: SubjectTopic, pick: String): SubjectTopic {
        return base.copy(
            titleHeb = "${base.titleHeb} - $pick",
            subTopicHint = pick
        )
    }
}

internal fun SubjectTopic.toSharedSubject(): SharedSubjectTopic =
    SharedSubjectTopic(
        id = this.id,
        titleHeb = this.titleHeb,
        topicsByBelt = this.topicsByBelt,
        subTopicHint = this.subTopicHint,
        includeItemKeywords = this.includeItemKeywords.orEmpty(),
        requireAllItemKeywords = this.requireAllItemKeywords.orEmpty(),
        excludeItemKeywords = this.excludeItemKeywords.orEmpty()
    )