package il.kmi.app.search

import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import il.kmi.shared.model.KmiBelt
import il.kmi.shared.model.KmiBeltContent
import il.kmi.shared.model.KmiSubTopic
import il.kmi.shared.model.KmiTopic

// המרות בין המודלים של האפליקציה למודלי shared

fun Belt.toShared(): KmiBelt = when (this) {
    Belt.WHITE  -> KmiBelt.WHITE
    Belt.YELLOW -> KmiBelt.YELLOW
    Belt.ORANGE -> KmiBelt.ORANGE
    Belt.GREEN  -> KmiBelt.GREEN
    Belt.BLUE   -> KmiBelt.BLUE
    Belt.BROWN  -> KmiBelt.BROWN
    Belt.BLACK  -> KmiBelt.BLACK
}

private fun ContentRepo.SubTopic.toShared(): KmiSubTopic =
    KmiSubTopic(title = title, items = items)

private fun ContentRepo.Topic.toShared(): KmiTopic =
    if (subTopics.isNotEmpty())
        KmiTopic(title = title, subTopics = subTopics.map { it.toShared() })
    else
        KmiTopic(title = title, items = items)

private fun ContentRepo.BeltContent.toShared(): KmiBeltContent =
    KmiBeltContent(topics = topics.map { it.toShared() })

fun ContentRepo.asSharedRepo(): Map<KmiBelt, KmiBeltContent> =
    data.mapKeys { (belt, _) -> belt.toShared() }
        .mapValues { (_, content) -> content.toShared() }
