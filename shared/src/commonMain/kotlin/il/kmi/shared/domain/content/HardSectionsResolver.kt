package il.kmi.shared.domain.content

import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.HardSectionsCatalog.Section
import il.kmi.shared.domain.content.HardSectionsCatalog.hasItems
import il.kmi.shared.domain.content.HardSectionsCatalog.itemsFor
import il.kmi.shared.domain.content.HardSectionsCatalog.totalItemsCount

object HardSectionsResolver {

    data class SectionEntry(
        val id: String,
        val title: String,
        val totalItemsCount: Int
    )

    data class BeltItems(
        val belt: Belt,
        val items: List<String>
    )

    sealed interface NodeResult {
        data class Sections(
            val subjectId: String,
            val currentSectionId: String?,
            val title: String?,
            val entries: List<SectionEntry>
        ) : NodeResult

        data class BeltGroups(
            val subjectId: String,
            val currentSectionId: String,
            val title: String,
            val groups: List<BeltItems>
        ) : NodeResult
    }

    fun resolve(
        subjectId: String,
        sectionId: String? = null
    ): NodeResult? {
        println("KMI_HARD Resolver.resolve subjectId='$subjectId' sectionId='$sectionId'")

        val roots = HardSectionsCatalog.sectionsForSubject(subjectId).orEmpty()

        println("KMI_HARD Resolver.resolve rootsCount=${roots.size} rootIds=${roots.map { it.id }}")

        if (roots.isEmpty()) return null

        val current: Section? =
            if (sectionId.isNullOrBlank()) {
                val visibleRoots = roots.filter { it.hasItems() }
                if (visibleRoots.size == 1) visibleRoots.first() else null
            } else {
                HardSectionsCatalog.findSectionById(subjectId, sectionId)
            }

        println(
            "KMI_HARD Resolver.resolve current='${current?.id}' title='${current?.title}' hasSub=${current?.subSections?.isNotEmpty() == true}"
        )

        return if (current == null) {
            NodeResult.Sections(
                subjectId = subjectId,
                currentSectionId = null,
                title = null,
                entries = roots
                    .filter { it.hasItems() }
                    .map {
                        SectionEntry(
                            id = it.id,
                            title = it.title,
                            totalItemsCount = it.totalItemsCount()
                        )
                    }
            )
        } else if (current.subSections.isNotEmpty()) {
            NodeResult.Sections(
                subjectId = subjectId,
                currentSectionId = current.id,
                title = current.title,
                entries = current.subSections
                    .filter { it.hasItems() }
                    .map {
                        SectionEntry(
                            id = it.id,
                            title = it.title,
                            totalItemsCount = it.totalItemsCount()
                        )
                    }
            )
        } else {
            NodeResult.BeltGroups(
                subjectId = subjectId,
                currentSectionId = current.id,
                title = current.title,
                groups = HardSectionsCatalog.beltOrder.map { belt ->
                    BeltItems(
                        belt = belt,
                        items = current.itemsFor(belt)
                    )
                }.filter { it.items.isNotEmpty() }
            )
        }
    }
}