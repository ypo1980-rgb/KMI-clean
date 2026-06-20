@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.KmiViewModel
import il.kmi.app.domain.Explanations
import il.kmi.app.domain.color
import il.kmi.app.favorites.FavoritesStore
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.English.ExerciseTitlesEnAliases
import il.kmi.shared.domain.content.English.ExerciseTitlesEnItems
import il.kmi.shared.domain.content.English.ExerciseTitlesEnTopics
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.shared.domain.content.HardSectionsResolver
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.LocalizationRuntime

private val subjectScreenGradientTop = Color(0xFFF2F0FA)
private val subjectScreenGradientMid = Color(0xFFF7F8FC)
private val subjectScreenGradientBottom = Color(0xFFFDFDFE)

@Composable
fun UnifiedSubjectExercisesScreen(
    subjectId: String,
    sectionId: String? = null,
    onOpenSection: (subjectId: String, sectionId: String?) -> Unit,
    onBack: () -> Unit,
    vm: KmiViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val isEnglish = LocalizationRuntime.currentLanguage == AppLanguage.ENGLISH
    val resolverSubjectId = remember(subjectId) {
        when (subjectId.trim()) {
            "kicks" -> "kicks_hard"
            else -> subjectId
        }
    }

    val result = remember(resolverSubjectId, sectionId) {
        HardSectionsResolver.resolve(resolverSubjectId, sectionId)
    }

    val combinedDefenseGroups = remember(resolverSubjectId) {
        combinedDefenseGroupsFor(resolverSubjectId)
    }

    val shouldShowSectionCards = sectionId == null && isRootSubjectId(subjectId)

    val flattenedSectionGroups = remember(subjectId, sectionId, result, shouldShowSectionCards) {
        if (!shouldShowSectionCards && result is HardSectionsResolver.NodeResult.Sections) {
            flattenNestedSectionsToBeltGroups(
                subjectId = resolverSubjectId,
                entries = result.entries
            )
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(resultTitle(subjectId = subjectId, result = result))
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            subjectScreenGradientTop,
                            subjectScreenGradientMid,
                            subjectScreenGradientBottom
                        )
                    )
                )
        ) {
            if (combinedDefenseGroups != null) {
                BeltGroupsContent(
                    title = subjectRootTitle(subjectId),
                    groups = combinedDefenseGroups,
                    isEnglish = isEnglish,
                    vm = vm,
                    modifier = Modifier.fillMaxSize()
                )
                return@Box
            }

            when (result) {
                is HardSectionsResolver.NodeResult.Sections -> {
                    if (shouldShowSectionCards) {
                        SectionsContent(
                            subjectId = subjectId,
                            title = result.title,
                            entries = result.entries,
                            isEnglish = isEnglish,
                            onOpen = onOpenSection,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BeltGroupsContent(
                            title = result.title ?: subjectRootTitle(subjectId),
                            groups = flattenedSectionGroups.orEmpty(),
                            isEnglish = isEnglish,
                            vm = vm,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                is HardSectionsResolver.NodeResult.BeltGroups -> {
                    BeltGroupsContent(
                        title = result.title,
                        groups = result.groups,
                        isEnglish = isEnglish,
                        vm = vm,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "אין נתונים להצגה",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun resultTitle(
    subjectId: String,
    result: HardSectionsResolver.NodeResult?
): String {
    return when (result) {
        is HardSectionsResolver.NodeResult.Sections -> {
            result.title ?: subjectRootTitle(subjectId)
        }
        is HardSectionsResolver.NodeResult.BeltGroups -> result.title
        null -> subjectRootTitle(subjectId)
    }
}

private fun subjectRootTitle(subjectId: String): String =
    when (subjectId) {
        "releases" -> "שחרורים"
        "releases_hugs" -> "שחרור מחביקות"
        "def_internal" -> "הגנות פנימיות"
        "def_external" -> "הגנות חיצוניות"
        "knife_defense" -> "הגנות מסכין"
        "gun_threat_defense" -> "הגנות מאיום אקדח"
        "stick_defense" -> "הגנות נגד מקל"
        "kicks" -> "הגנות נגד בעיטות"
        "kicks_hard" -> "הגנות נגד בעיטות"
        else -> "נושאים"
    }

private fun isRootSubjectId(subjectId: String): Boolean {
    return subjectId in setOf(
        "releases",
        "knife_defense",
        "gun_threat_defense",
        "stick_defense",
        "kicks"
    )
}

private fun combinedDefenseGroupsFor(
    subjectId: String
): List<HardSectionsResolver.BeltItems>? {
    val sectionIds = when (subjectId.trim().lowercase()) {
        "def_internal" -> listOf(
            "def_internal_punch",
            "def_internal_kick"
        )

        "def_external" -> listOf(
            "def_external_punch",
            "def_external_kick"
        )

        else -> return null
    }

    val mergedByBelt = linkedMapOf<Belt, MutableList<String>>()

    fun addGroups(groups: List<HardSectionsResolver.BeltItems>) {
        groups.forEach { group ->
            val items = mergedByBelt.getOrPut(group.belt) { mutableListOf() }
            items.addAll(group.items)
        }
    }

    sectionIds.forEach { sectionId ->
        when (val resolved = HardSectionsResolver.resolve(sectionId, null)) {
            is HardSectionsResolver.NodeResult.BeltGroups -> {
                addGroups(resolved.groups)
            }

            is HardSectionsResolver.NodeResult.Sections -> {
                addGroups(
                    flattenNestedSectionsToBeltGroups(
                        subjectId = sectionId,
                        entries = resolved.entries
                    )
                )
            }

            null -> Unit
        }
    }

    return mergedByBelt.map { (belt, items) ->
        HardSectionsResolver.BeltItems(
            belt = belt,
            items = items.distinct()
        )
    }
}

private fun flattenNestedSectionsToBeltGroups(
    subjectId: String,
    entries: List<HardSectionsResolver.SectionEntry>
): List<HardSectionsResolver.BeltItems> {
    val mergedByBelt = linkedMapOf<Belt, MutableList<String>>()

    fun addGroups(groups: List<HardSectionsResolver.BeltItems>) {
        groups.forEach { group ->
            val items = mergedByBelt.getOrPut(group.belt) { mutableListOf() }
            items.addAll(group.items)
        }
    }

    fun collect(entry: HardSectionsResolver.SectionEntry) {
        when (val resolved = HardSectionsResolver.resolve(subjectId, entry.id)) {
            is HardSectionsResolver.NodeResult.BeltGroups -> {
                addGroups(resolved.groups)
            }

            is HardSectionsResolver.NodeResult.Sections -> {
                resolved.entries.forEach { nestedEntry ->
                    collect(nestedEntry)
                }
            }

            null -> Unit
        }
    }

    entries.forEach { entry ->
        collect(entry)
    }

    return mergedByBelt.map { (belt, items) ->
        HardSectionsResolver.BeltItems(
            belt = belt,
            items = items.distinct()
        )
    }
}

@Composable
private fun SectionsContent(
    subjectId: String,
    title: String?,
    entries: List<HardSectionsResolver.SectionEntry>,
    isEnglish: Boolean,
    onOpen: (subjectId: String, sectionId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (isEnglish) translateHardTopicTitle(title ?: "נושאים") else title ?: "נושאים",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isEnglish) "Choose a sub-topic" else "בחר תת־נושא",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6C6880),
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(entries) { entry ->
            SubjectSectionCard(
                title = if (isEnglish) translateHardTopicTitle(entry.title) else entry.title,
                count = entry.totalItemsCount,
                isEnglish = isEnglish,
                onClick = { onOpen(subjectId, entry.id) }
            )
        }
    }
}

@Composable
private fun SubjectSectionCard(
    title: String,
    count: Int,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFD9D4E8)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.White.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = Color(0xFF7B7593)
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F4F8)
                ) {
                    Text(
                        text = if (isEnglish) {
                            if (count == 1) "1 exercise" else "$count exercises"
                        } else {
                            "$count תרגילים"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF4E6D73),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(21.dp),
                color = Color(0xFFF3F0FA)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(0xFF7A6FA3)
                    )
                }
            }
        }
    }
}

private data class SelectedHardExercise(
    val belt: Belt,
    val topic: String,
    val rawItem: String,
    val displayItem: String
)

@Composable
private fun HardTopStatChip(
    value: String,
    label: String,
    containerColor: Color,
    contentColor: Color = Color.White
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            contentColor.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = contentColor,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Text(
                text = label,
                color = contentColor.copy(alpha = 0.92f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HardExerciseMetaBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            contentColor.copy(alpha = 0.14f)
        ),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 9.sp,
            lineHeight = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun BeltGroupsContent(
    title: String,
    groups: List<HardSectionsResolver.BeltItems>,
    isEnglish: Boolean,
    vm: KmiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("kmi_settings", android.content.Context.MODE_PRIVATE)
    }

    val marksVersion by vm.marksVersion.collectAsState()
    val hardItemStates = remember(title) { mutableStateMapOf<String, Boolean?>() }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun hardStatusIdFor(
        belt: Belt,
        topic: String,
        rawItem: String
    ): String {
        val resolved = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = normalizeStatusPart(rawItem),
            topicKey = normalizeStatusPart(topic)
        )

        return resolved.id
    }

    fun hardStatusKeysFor(
        belt: Belt,
        topic: String,
        rawItem: String
    ): List<String> {
        val statusId = hardStatusIdFor(
            belt = belt,
            topic = topic,
            rawItem = rawItem
        )

        val identityKeys = ExerciseIdentityRegistry
            .allKnown()
            .firstOrNull { it.id == statusId && it.belt == belt }
            ?.topicKeys
            .orEmpty()

        return (
                identityKeys +
                        topic +
                        "כללי"
                )
            .map { normalizeStatusPart(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun setHardLocalStatus(
        belt: Belt,
        topic: String,
        rawItem: String,
        statusId: String,
        value: Boolean?
    ) {
        hardStatusKeysFor(
            belt = belt,
            topic = topic,
            rawItem = rawItem
        ).forEach { key ->
            val masteredKey = "mastered_${belt.id}_${key}"
            val unknownKey = "unknown_${belt.id}_${key}"

            val masteredSet =
                (prefs.getStringSet(masteredKey, emptySet<String>()) ?: emptySet()).toMutableSet()

            val unknownSet =
                (prefs.getStringSet(unknownKey, emptySet<String>()) ?: emptySet()).toMutableSet()

            when (value) {
                true -> {
                    masteredSet.add(statusId)
                    unknownSet.remove(statusId)
                }

                false -> {
                    unknownSet.add(statusId)
                    masteredSet.remove(statusId)
                }

                null -> {
                    masteredSet.remove(statusId)
                    unknownSet.remove(statusId)
                }
            }

            prefs.edit()
                .putStringSet(masteredKey, masteredSet)
                .putStringSet(unknownKey, unknownSet)
                .apply()
        }
    }

    LaunchedEffect(groups, marksVersion) {
        groups.forEach { group ->
            group.items.forEach { rawItem ->
                val statusId = hardStatusIdFor(
                    belt = group.belt,
                    topic = title,
                    rawItem = rawItem
                )

                var valueFromVm: Boolean? = null

                for (key in hardStatusKeysFor(group.belt, title, rawItem)) {
                    val fromKey: Boolean? =
                        runCatching {
                            vm.getItemStatusNullable(
                                belt = group.belt,
                                topic = key,
                                item = statusId
                            )
                        }.getOrNull()
                            ?: runCatching {
                                if (
                                    vm.isMastered(
                                        belt = group.belt,
                                        topic = key,
                                        item = statusId
                                    )
                                ) true else null
                            }.getOrNull()

                    if (fromKey != null) {
                        valueFromVm = fromKey
                        break
                    }
                }

                if (valueFromVm == null) {
                    for (key in hardStatusKeysFor(group.belt, title, rawItem)) {
                        val masteredKey = "mastered_${group.belt.id}_${key}"
                        val unknownKey = "unknown_${group.belt.id}_${key}"

                        val masteredSet = prefs.getStringSet(masteredKey, emptySet<String>()) ?: emptySet()
                        val unknownSet = prefs.getStringSet(unknownKey, emptySet<String>()) ?: emptySet()

                        val localValue: Boolean? = when {
                            masteredSet.contains(statusId) -> true
                            unknownSet.contains(statusId) -> false
                            else -> null
                        }

                        if (localValue != null) {
                            valueFromVm = localValue

                            vm.setItemStatusNullable(
                                belt = group.belt,
                                topic = key,
                                item = statusId,
                                value = localValue
                            )

                            break
                        }
                    }
                }

                hardItemStates[statusId] = valueFromVm
            }
        }
    }

    val favoriteIds: Set<String> by FavoritesStore
        .favoritesFlow
        .collectAsState(initial = emptySet())

    fun hardFavoriteIdFor(
        belt: Belt,
        topic: String,
        rawItem: String
    ): String {
        return hardStatusIdFor(
            belt = belt,
            topic = topic,
            rawItem = rawItem
        )
    }

    val allHardItems = remember(groups, title) {
        groups.flatMap { group ->
            group.items.map { rawItem ->
                Triple(group.belt, title, rawItem)
            }
        }
    }

    val hardTotalCount = allHardItems.size

    val hardKnownCount = allHardItems.count { (belt, topic, rawItem) ->
        val statusId = hardStatusIdFor(belt, topic, rawItem)
        hardItemStates[statusId] == true
    }

    val hardUnknownCount = allHardItems.count { (belt, topic, rawItem) ->
        val statusId = hardStatusIdFor(belt, topic, rawItem)
        hardItemStates[statusId] == false
    }

    val hardUnmarkedCount = allHardItems.count { (belt, topic, rawItem) ->
        val statusId = hardStatusIdFor(belt, topic, rawItem)
        hardItemStates[statusId] == null
    }

    val hardFavoriteCount = allHardItems.count { (belt, topic, rawItem) ->
        hardFavoriteIdFor(belt, topic, rawItem) in favoriteIds
    }

    var selectedExercise by remember { mutableStateOf<SelectedHardExercise?>(null) }

    val isKickDefenseScreen =
        title.trim() == "הגנות נגד בעיטות" ||
                title.trim() == "Defenses against kicks" ||
                title.trim() == "Defenses Against Kicks"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isKickDefenseScreen) {
            item {
                Text(
                    text = if (isEnglish) translateHardTopicTitle(title) else title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        lineHeight = 25.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (isEnglish) "Exercises by belt" else "תרגילים לפי חגורות",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    ),
                    color = Color(0xFF5B6472),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isEnglish) {
                        "← Swipe sideways to see more stats →"
                    } else {
                        "→→ הזז לצד כדי לראות עוד נתונים →→"
                    },
                    color = Color(0xFF5B6472),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HardTopStatChip(
                        value = hardTotalCount.toString(),
                        label = if (isEnglish) "Exercises" else "תרגילים",
                        containerColor = Color(0xFF98A2B3)
                    )

                    HardTopStatChip(
                        value = hardKnownCount.toString(),
                        label = if (isEnglish) "Known" else "יודע",
                        containerColor = Color(0xFF7ACB88)
                    )

                    HardTopStatChip(
                        value = hardUnknownCount.toString(),
                        label = if (isEnglish) "Unknown" else "לא יודע",
                        containerColor = Color(0xFFF1A97A)
                    )

                    HardTopStatChip(
                        value = hardFavoriteCount.toString(),
                        label = if (isEnglish) "Favorites" else "מועדפים",
                        containerColor = Color(0xFFE7A3B5)
                    )

                    HardTopStatChip(
                        value = hardUnmarkedCount.toString(),
                        label = if (isEnglish) "Unmarked" else "לא סומן",
                        containerColor = Color(0xFF8596C9)
                    )
                }
            }
        }

        items(groups) { group ->
            BeltSectionCard(
                group = group,
                title = title,
                isEnglish = isEnglish,
                hardItemStates = hardItemStates,
                favoriteIds = favoriteIds,
                statusIdFor = { belt, topic, raw ->
                    hardStatusIdFor(
                        belt = belt,
                        topic = topic,
                        rawItem = raw
                    )
                },
                favoriteIdFor = { belt, topic, raw ->
                    hardFavoriteIdFor(
                        belt = belt,
                        topic = topic,
                        rawItem = raw
                    )
                },
                onToggleFavorite = { belt, topic, raw ->
                    FavoritesStore.toggle(
                        hardFavoriteIdFor(
                            belt = belt,
                            topic = topic,
                            rawItem = raw
                        )
                    )
                },
                onStatusClick = { belt, topic, raw ->
                    val statusId = hardStatusIdFor(
                        belt = belt,
                        topic = topic,
                        rawItem = raw
                    )

                    val nextValue = when (hardItemStates[statusId]) {
                        null -> true
                        true -> false
                        false -> null
                    }

                    hardItemStates[statusId] = nextValue

                    hardStatusKeysFor(
                        belt = belt,
                        topic = topic,
                        rawItem = raw
                    ).forEach { key ->
                        vm.setItemStatusNullable(
                            belt = belt,
                            topic = key,
                            item = statusId,
                            value = nextValue
                        )
                    }

                    setHardLocalStatus(
                        belt = belt,
                        topic = topic,
                        rawItem = raw,
                        statusId = statusId,
                        value = nextValue
                    )
                },
                onInfoClick = { belt, topic, raw, display ->
                    selectedExercise = SelectedHardExercise(
                        belt = belt,
                        topic = topic,
                        rawItem = raw,
                        displayItem = display
                    )
                }
            )
        }
    }

    selectedExercise?.let { selected ->
        val explanation = remember(selected.belt, selected.rawItem) {
            val raw = Explanations.get(selected.belt, selected.rawItem).trim()
            if (raw.isBlank()) {
                if (isEnglish) {
                    "There is no explanation for this exercise yet."
                } else {
                    "אין כרגע הסבר לתרגיל הזה."
                }
            } else {
                if ("::" in raw) raw.substringAfter("::").trim() else raw
            }
        }

        AlertDialog(
            onDismissRequest = { selectedExercise = null },
            title = {
                Text(
                    text = selected.displayItem,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedExercise = null }) {
                    Text(if (isEnglish) "Close" else "סגור")
                }
            }
        )
    }
}

private fun kickDefenseSectionTitleFor(
    rawItem: String,
    isEnglish: Boolean
): String? {
    val clean = rawItem
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")
        .trim()

    return when {
        clean.contains("ברך") -> {
            if (isEnglish) "Defenses Against Knee Strikes" else "הגנות נגד ברך"
        }

        clean.contains("מגל") -> {
            if (isEnglish) "Defenses Against Round Kicks" else "הגנות נגד בעיטות מגל"
        }

        clean.contains("לצד") ||
                clean.contains("בעיטת צד") ||
                clean.contains("בעיטה צד") -> {
            if (isEnglish) "Defenses Against Side Kicks" else "הגנות נגד בעיטות לצד"
        }

        clean.contains("בעיטה ישרה") ||
                clean.contains("בעיטה רגילה") ||
                clean.contains("רגילה") ||
                clean.contains("ישרה") -> {
            if (isEnglish) "Defenses Against Regular Kicks" else "הגנות נגד בעיטה רגילה"
        }

        else -> null
    }
}

@Composable
private fun HardExerciseSectionHeader(
    text: String,
    isEnglish: Boolean
) {
    Text(
        text = text,
        color = Color(0xFF4CAF50),
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun BeltSectionCard(
    group: HardSectionsResolver.BeltItems,
    title: String,
    isEnglish: Boolean,
    hardItemStates: Map<String, Boolean?>,
    favoriteIds: Set<String>,
    statusIdFor: (belt: Belt, topic: String, rawItem: String) -> String,
    favoriteIdFor: (belt: Belt, topic: String, rawItem: String) -> String,
    onToggleFavorite: (belt: Belt, topic: String, rawItem: String) -> Unit,
    onStatusClick: (belt: Belt, topic: String, rawItem: String) -> Unit,
    onInfoClick: (belt: Belt, topic: String, rawItem: String, displayItem: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = group.belt.color.copy(alpha = 0.07f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                color = group.belt.color.copy(alpha = 0.18f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = beltTitle(group.belt, isEnglish),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = if (isEnglish) {
                            if (group.items.size == 1) "1 exercise" else "${group.items.size} exercises"
                        } else {
                            "${group.items.size} תרגילים"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = group.belt.color
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val groupTotalCount = group.items.size

            val groupKnownCount = group.items.count { rawItem ->
                hardItemStates[statusIdFor(group.belt, title, rawItem)] == true
            }

            val groupUnknownCount = group.items.count { rawItem ->
                hardItemStates[statusIdFor(group.belt, title, rawItem)] == false
            }

            val groupFavoriteCount = group.items.count { rawItem ->
                favoriteIdFor(group.belt, title, rawItem) in favoriteIds
            }

            val groupUnmarkedCount = group.items.count { rawItem ->
                hardItemStates[statusIdFor(group.belt, title, rawItem)] == null
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HardTopStatChip(
                    value = groupTotalCount.toString(),
                    label = if (isEnglish) "Exercises" else "תרגילים",
                    containerColor = Color(0xFF98A2B3)
                )

                HardTopStatChip(
                    value = groupKnownCount.toString(),
                    label = if (isEnglish) "Known" else "יודע",
                    containerColor = Color(0xFF7ACB88)
                )

                HardTopStatChip(
                    value = groupUnknownCount.toString(),
                    label = if (isEnglish) "Unknown" else "לא יודע",
                    containerColor = Color(0xFFF1A97A)
                )

                HardTopStatChip(
                    value = groupFavoriteCount.toString(),
                    label = if (isEnglish) "Favorites" else "מועדפים",
                    containerColor = Color(0xFFE7A3B5)
                )

                HardTopStatChip(
                    value = groupUnmarkedCount.toString(),
                    label = if (isEnglish) "Unmarked" else "לא סומן",
                    containerColor = Color(0xFF8596C9)
                )
            }

            var lastSectionTitle: String? = null

            group.items.forEachIndexed { index, rawItem ->
                val statusId = statusIdFor(group.belt, title, rawItem)
                val favoriteId = favoriteIdFor(group.belt, title, rawItem)
                val mastered = hardItemStates[statusId]
                val displayItem = if (isEnglish) translateHardExerciseTitle(rawItem) else rawItem
                val isFavorite = favoriteId in favoriteIds

                val sectionTitle = if (
                    title.trim() == "הגנות נגד בעיטות" ||
                    title.trim() == "Defenses against kicks" ||
                    title.trim() == "Defenses Against Kicks"
                ) {
                    kickDefenseSectionTitleFor(rawItem, isEnglish)
                } else {
                    null
                }

                if (sectionTitle != null && sectionTitle != lastSectionTitle) {
                    HardExerciseSectionHeader(
                        text = sectionTitle,
                        isEnglish = isEnglish
                    )

                    lastSectionTitle = sectionTitle
                }

                HardExerciseRowCard(
                    exerciseNumber = index + 1,
                    belt = group.belt,
                    item = displayItem,
                    mastered = mastered,
                    isFavorite = isFavorite,
                    isEnglish = isEnglish,
                    onStatusClick = {
                        onStatusClick(group.belt, title, rawItem)
                    },
                    onToggleFavorite = {
                        onToggleFavorite(group.belt, title, rawItem)
                    },
                    onInfoClick = {
                        onInfoClick(group.belt, title, rawItem, displayItem)
                    }
                )

                if (index != group.items.lastIndex) {
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun HardExerciseRowCard(
    exerciseNumber: Int,
    belt: Belt,
    item: String,
    mastered: Boolean?,
    isFavorite: Boolean,
    isEnglish: Boolean,
    onStatusClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(
            1.dp,
            belt.color.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardMasterToggle(
                mastered = mastered,
                onClick = onStatusClick
            )

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onInfoClick() },
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        HardExerciseMetaBadge(
                            text = "No. $exerciseNumber",
                            containerColor = belt.color.copy(alpha = 0.14f),
                            contentColor = Color(0xFF1F2937)
                        )

                        if (isFavorite) {
                            Spacer(Modifier.width(5.dp))
                            HardExerciseMetaBadge(
                                text = "Favorite",
                                containerColor = Color(0xFFF9D9B8),
                                contentColor = Color(0xFF9A5A00)
                            )
                        }

                        Spacer(Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))

                        if (isFavorite) {
                            HardExerciseMetaBadge(
                                text = "מועדף",
                                containerColor = Color(0xFFF9D9B8),
                                contentColor = Color(0xFF9A5A00)
                            )
                            Spacer(Modifier.width(5.dp))
                        }

                        HardExerciseMetaBadge(
                            text = "מס׳ $exerciseNumber",
                            containerColor = belt.color.copy(alpha = 0.14f),
                            contentColor = Color(0xFF1F2937)
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 13.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF263238),
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.width(6.dp))

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = if (isEnglish) "Exercise information" else "מידע על התרגיל",
                    tint = Color(0xFF607D8B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(3.dp))

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) {
                        if (isEnglish) "Remove from favorites" else "הסר ממועדפים"
                    } else {
                        if (isEnglish) "Add to favorites" else "הוסף למועדפים"
                    },
                    tint = if (isFavorite) {
                        Color(0xFFFFC107)
                    } else {
                        Color(0xFF9CA3AF)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(3.dp))

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 38.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(belt.color.copy(alpha = 1f))
            )
        }
    }
}

@Composable
private fun HardMasterToggle(
    mastered: Boolean?,
    onClick: () -> Unit
) {
    val bg = when (mastered) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> Color.White
    }

    val border = when (mastered) {
        true -> Color(0xFF1B5E20)
        false -> Color(0xFF8E1B1B)
        null -> Color(0xFFCBD5E1)
    }

    Surface(
        modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.5.dp, border),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (mastered) {
                true -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "יודע",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )

                false -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "לא יודע",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )

                null -> Spacer(Modifier.size(1.dp))
            }
        }
    }
}

private fun beltTitle(belt: Belt, isEnglish: Boolean): String =
    if (isEnglish) {
        when (belt) {
            Belt.YELLOW -> "Yellow Belt"
            Belt.ORANGE -> "Orange Belt"
            Belt.GREEN -> "Green Belt"
            Belt.BLUE -> "Blue Belt"
            Belt.BROWN -> "Brown Belt"
            Belt.BLACK -> "Black Belt"
            else -> belt.name
        }
    } else {
        when (belt) {
            Belt.YELLOW -> "חגורה צהובה"
            Belt.ORANGE -> "חגורה כתומה"
            Belt.GREEN -> "חגורה ירוקה"
            Belt.BLUE -> "חגורה כחולה"
            Belt.BROWN -> "חגורה חומה"
            Belt.BLACK -> "חגורה שחורה"
            else -> belt.name
        }
    }

private fun translateHardExerciseTitle(raw: String): String {
    val clean = normalizeHardTitle(raw)

    ExerciseTitlesEnItems.map[clean]?.let { return it }
    ExerciseTitlesEnAliases.map[clean]?.let { return it }

    val normalizedItemsMap = ExerciseTitlesEnItems.map.entries.associateBy { normalizeHardTitle(it.key) }
    normalizedItemsMap[clean]?.value?.let { return it }

    val normalizedAliasesMap = ExerciseTitlesEnAliases.map.entries.associateBy { normalizeHardTitle(it.key) }
    normalizedAliasesMap[clean]?.value?.let { return it }

    return raw
}

private fun translateHardTopicTitle(raw: String): String {
    val clean = normalizeHardTitle(raw)

    ExerciseTitlesEnTopics.map[clean]?.let { return it }

    val normalizedTopicsMap = ExerciseTitlesEnTopics.map.entries.associateBy { normalizeHardTitle(it.key) }
    normalizedTopicsMap[clean]?.value?.let { return it }

    return raw
}

private fun normalizeHardTitle(raw: String): String {
    return raw
        .trim()
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("–", "-")
        .replace("—", "-")
        .replace(Regex("\\s+"), " ")
}
