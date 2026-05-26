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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import il.kmi.app.KmiViewModel
import il.kmi.app.domain.Explanations
import il.kmi.app.domain.color
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
    val result = remember(subjectId, sectionId) {
        HardSectionsResolver.resolve(subjectId, sectionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(resultTitle(subjectId = subjectId, result = result))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "חזרה"
                        )
                    }
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
            when (result) {
                is HardSectionsResolver.NodeResult.Sections -> {
                    SectionsContent(
                        subjectId = subjectId,
                        title = result.title,
                        entries = result.entries,
                        isEnglish = isEnglish,
                        onOpen = onOpenSection,
                        modifier = Modifier.fillMaxSize()
                    )
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
        "knife_defense" -> "הגנות מסכין"
        "gun_threat_defense" -> "הגנות מאיום אקדח"
        "stick_defense" -> "הגנות נגד מקל"
        "kicks" -> "הגנות נגד בעיטות"
        else -> "נושאים"
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

    var selectedExercise by remember { mutableStateOf<SelectedHardExercise?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (isEnglish) translateHardTopicTitle(title) else title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isEnglish) "Exercises by belt" else "תרגילים לפי חגורות",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(groups) { group ->
            BeltSectionCard(
                group = group,
                title = title,
                isEnglish = isEnglish,
                hardItemStates = hardItemStates,
                statusIdFor = { belt, topic, raw ->
                    hardStatusIdFor(
                        belt = belt,
                        topic = topic,
                        rawItem = raw
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

@Composable
private fun BeltSectionCard(
    group: HardSectionsResolver.BeltItems,
    title: String,
    isEnglish: Boolean,
    hardItemStates: Map<String, Boolean?>,
    statusIdFor: (belt: Belt, topic: String, rawItem: String) -> String,
    onStatusClick: (belt: Belt, topic: String, rawItem: String) -> Unit,
    onInfoClick: (belt: Belt, topic: String, rawItem: String, displayItem: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
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

            Spacer(Modifier.height(12.dp))

            group.items.forEachIndexed { index, rawItem ->
                val statusId = statusIdFor(group.belt, title, rawItem)
                val mastered = hardItemStates[statusId]
                val displayItem = if (isEnglish) translateHardExerciseTitle(rawItem) else rawItem

                HardExerciseRowCard(
                    belt = group.belt,
                    item = displayItem,
                    mastered = mastered,
                    isEnglish = isEnglish,
                    onStatusClick = {
                        onStatusClick(group.belt, title, rawItem)
                    },
                    onInfoClick = {
                        onInfoClick(group.belt, title, rawItem, displayItem)
                    }
                )

                if (index != group.items.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun HardExerciseRowCard(
    belt: Belt,
    item: String,
    mastered: Boolean?,
    isEnglish: Boolean,
    onStatusClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = Color.White,
        tonalElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            belt.color.copy(alpha = 0.30f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardMasterToggle(
                mastered = mastered,
                onClick = onStatusClick
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = item,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238),
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onInfoClick() }
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = if (isEnglish) "Exercise information" else "מידע על התרגיל",
                    tint = Color(0xFF607D8B)
                )
            }

            Spacer(Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(belt.color.copy(alpha = 0.90f))
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
            .size(42.dp)
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
                    modifier = Modifier.size(28.dp)
                )

                false -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "לא יודע",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
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
