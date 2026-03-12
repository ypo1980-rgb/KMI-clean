package il.kmi.app.screens.BeltQuestions

import il.kmi.app.domain.SubjectTopic
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.SubjectTopic as SharedSubjectTopic
import il.kmi.shared.domain.content.SubjectItemsResolver
import il.kmi.shared.domain.content.SubjectItemsResolver.UiSection

internal object SubjectTopicsEngine {

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
                subTopicHint = p,
                topicsByBelt = mapOf(
                    Belt.GREEN to listOf("מכות מרפק")
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
        subTopicHint = this.subTopicHint
    )