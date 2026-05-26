package il.kmi.app.screens.common

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import il.kmi.app.KmiViewModel
import il.kmi.app.ui.color
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.ExerciseIdentityRegistry

@Composable
fun LegacyGroupedExercisesScreen(
    title: String,
    groupedItems: List<Pair<Belt, List<String>>>,
    itemTitle: (String) -> String,
    onBack: () -> Unit,
    onItemClick: ((Belt, String) -> Unit)? = null,
    explanationText: ((Belt, String) -> String)? = null,
    itemCountText: ((String) -> Int)? = null,
    treatAsMenuCards: Boolean = false,
    vm: KmiViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val scroll = rememberScrollState()

    // ✅ הסבר רק אחרי לחיצה (ולא כסאב-טייטל ברשימה)
    val explainFor = remember { mutableStateOf<Pair<Belt, String>?>(null) }

    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
    }

    val marksVersion by vm.marksVersion.collectAsState()
    val itemStates = remember(title) { mutableStateMapOf<String, Boolean?>() }

    fun normalizeStatusPart(s: String): String =
        s.replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .replace("–", "-")
            .replace("—", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun statusIdFor(belt: Belt, rawTitle: String): String {
        val cleanTitle = normalizeStatusPart(rawTitle)

        val resolved = ExerciseIdentityRegistry.resolve(
            belt = belt,
            hebrewTitle = cleanTitle,
            topicKey = normalizeStatusPart(title)
        )

        return resolved.id
    }

    fun statusKeysFor(belt: Belt, rawTitle: String): List<String> {
        val statusId = statusIdFor(belt, rawTitle)

        val identityKeys = ExerciseIdentityRegistry
            .allKnown()
            .firstOrNull { it.id == statusId && it.belt == belt }
            ?.topicKeys
            .orEmpty()

        return (
                identityKeys +
                        title +
                        "כללי"
                )
            .map { normalizeStatusPart(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun setLocalStatus(
        belt: Belt,
        rawTitle: String,
        statusId: String,
        value: Boolean?
    ) {
        statusKeysFor(belt, rawTitle).forEach { key ->
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

    LaunchedEffect(groupedItems, marksVersion, title) {
        groupedItems.forEach { (belt, items) ->
            items.forEach { raw ->
                val rawTitle = itemTitle(raw)
                val cleanTitle =
                    if (treatAsMenuCards) stripReleasesPrefix(rawTitle) else rawTitle

                val statusId = statusIdFor(belt, cleanTitle)

                var valueFromVm: Boolean? = null

                for (key in statusKeysFor(belt, cleanTitle)) {
                    val fromKey: Boolean? =
                        runCatching {
                            vm.getItemStatusNullable(
                                belt = belt,
                                topic = key,
                                item = statusId
                            )
                        }.getOrNull()
                            ?: runCatching {
                                if (
                                    vm.isMastered(
                                        belt = belt,
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
                    for (key in statusKeysFor(belt, cleanTitle)) {
                        val masteredKey = "mastered_${belt.id}_${key}"
                        val unknownKey = "unknown_${belt.id}_${key}"

                        val masteredSet =
                            prefs.getStringSet(masteredKey, emptySet<String>()) ?: emptySet()

                        val unknownSet =
                            prefs.getStringSet(unknownKey, emptySet<String>()) ?: emptySet()

                        val localValue: Boolean? = when {
                            masteredSet.contains(statusId) -> true
                            unknownSet.contains(statusId) -> false
                            else -> null
                        }

                        if (localValue != null) {
                            valueFromVm = localValue

                            vm.setItemStatusNullable(
                                belt = belt,
                                topic = key,
                                item = statusId,
                                value = localValue
                            )

                            break
                        }
                    }
                }

                itemStates[statusId] = valueFromVm
            }
        }
    }

    fun toggleStatus(belt: Belt, rawTitle: String) {
        val statusId = statusIdFor(belt, rawTitle)

        val nextValue = when (itemStates[statusId]) {
            null -> true
            true -> false
            false -> null
        }

        itemStates[statusId] = nextValue

        statusKeysFor(belt, rawTitle).forEach { key ->
            vm.setItemStatusNullable(
                belt = belt,
                topic = key,
                item = statusId,
                value = nextValue
            )
        }

        setLocalStatus(
            belt = belt,
            rawTitle = rawTitle,
            statusId = statusId,
            value = nextValue
        )
    }

    if (!treatAsMenuCards) {
        explainFor.value?.let { (belt, clickedTitle) ->
            val full = explanationText?.invoke(belt, clickedTitle)
                ?: il.kmi.app.domain.Explanations.get(belt, explanationKeyForDefenses(clickedTitle))

            AlertDialog(
                onDismissRequest = { explainFor.value = null },
                title = {
                    Text(
                        text = clickedTitle,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = full,
                            textAlign = TextAlign.Right
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { explainFor.value = null }) {
                        Text("סגור")
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // כותרת מסך
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "חזור",
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = Color(0xFF1E88E5),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )

            Spacer(Modifier.width(42.dp)) // איזון ויזואלי מול "חזור"
        }

        Spacer(Modifier.height(12.dp))

        if (groupedItems.isEmpty()) {
            Text(
                text = "אין תרגילים להצגה",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                textAlign = TextAlign.Center,
                color = Color(0xFF546E7A),
                fontWeight = FontWeight.Medium
            )
            return
        }

        // מציג לפי סדר חגורות
        val ordered = remember(groupedItems) {
            val order = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)
            val map = groupedItems.toMap()
            order.mapNotNull { b -> map[b]?.let { items -> b to items } }
        }
        ordered.forEach { (belt, items) ->
            BeltSection(
                belt = belt,
                items = items,
                itemTitle = itemTitle,
                itemStates = itemStates,
                statusIdFor = { itemTitleForStatus ->
                    statusIdFor(belt, itemTitleForStatus)
                },
                onStatusClick = { itemTitleForStatus ->
                    toggleStatus(belt, itemTitleForStatus)
                },
                onItemClick = { raw -> onItemClick?.invoke(belt, raw) },
                onExplainClick = { clickedTitle ->
                    explainFor.value = belt to clickedTitle
                },
                treatAsMenuCards = treatAsMenuCards,
                itemCountText = itemCountText
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun DefenseItemCard(
    belt: Belt,
    title: String,
    subtitle: String,
    mastered: Boolean?,
    onStatusClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = Color.White,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            belt.color.copy(alpha = 0.28f)
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                MasterStatusToggle(
                    mastered = mastered,
                    onClick = onStatusClick
                )

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onInfoClick() }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF263238),
                        textAlign = TextAlign.Right,
                        maxLines = 3
                    )

                    val sub = subtitle.trim()
                    if (sub.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = sub,
                            color = Color(0xFF607D8B),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Right
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFEFF3F6),
                    shadowElevation = 2.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.clickable { onInfoClick() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "מידע על התרגיל",
                            tint = Color(0xFF607D8B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(belt.color)
                )
            }
        }
    }
}

@Composable
private fun MasterStatusToggle(
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

/**
 * ✅ כרטיס "כמו הגנות" לתפריט שחרורים (או כל תפריט קטגוריות אחר):
 * אייקון עגול משמאל, שם + "X תרגילים", וחץ.
 */
@Composable
fun ReleasesMenuCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        color = Color(0xFFF5F7FA),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        // ✅ LTR: האייקון והחץ יושבים נכון גם בעברית
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ✅ אייקון עגול משמאל
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // ✅ טקסטים מיושרים לימין
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF263238),
                            textAlign = TextAlign.Right,
                            maxLines = 2
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "$count תרגילים",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Right
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = null,
                    tint = Color(0xFF90A4AE)
                )
            }
        }
    }
}

@Composable
private fun BeltSection(
    belt: Belt,
    items: List<String>,
    itemTitle: (String) -> String,
    itemStates: Map<String, Boolean?>,
    statusIdFor: (String) -> String,
    onStatusClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onExplainClick: (String) -> Unit,
    treatAsMenuCards: Boolean,
    itemCountText: ((String) -> Int)? = null
) {
    val sectionShape = RoundedCornerShape(20.dp)
    val headerShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    val headerBg = belt.color.copy(alpha = 0.95f)
    val headerTextColor = if (headerBg.luminance() < 0.5f) Color.White else Color.Black

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = sectionShape,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        color = Color(0xFFF3F5F7)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(headerShape)
                    .background(headerBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "חגורה ${belt.heb}",
                    color = headerTextColor,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
                Text(
                    text = "${items.size} תרגילים",
                    color = headerTextColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                items.forEach { raw ->
                    val rawTitle = itemTitle(raw)

                    val title =
                        if (treatAsMenuCards) stripReleasesPrefix(rawTitle) else rawTitle

                    // ✅ לא מציגים הסבר ברשימה (רק בדיאלוג אחרי קליק)
                    val subtitle = ""

                    if (treatAsMenuCards) {
                        val count = itemCountText?.invoke(title) ?: 0

                        ReleasesMenuCard(
                            title = title,
                            count = count,
                            icon = Icons.Outlined.StarBorder,
                            iconBg = belt.color.copy(alpha = 0.95f),
                            onClick = { onItemClick(raw) }
                        )
                    } else {
                        val statusId = statusIdFor(title)
                        val mastered = itemStates[statusId]

                        DefenseItemCard(
                            belt = belt,
                            title = title,
                            subtitle = subtitle,
                            mastered = mastered,
                            onStatusClick = { onStatusClick(title) },
                            onInfoClick = { onExplainClick(title) }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun stripReleasesPrefix(title: String): String {
    val t = title.trim()

    // נפוצים אצלך: "שחרורים ..." / "שחרורים - ..." / "שחרורים:" / "שחרור ..."
    val prefixes = listOf(
        "שחרורים -",
        "שחרורים:",
        "שחרורים",
        "שחרור -",
        "שחרור:",
        "שחרור"
    )

    val cut = prefixes.firstOrNull { p -> t.startsWith(p) } ?: return t
    return t.removePrefix(cut).trim().trimStart('-', ':').trim()
}

private fun explanationKeyForDefenses(uiTitle: String): String {
    var t = uiTitle.trim()

    // מוריד prefixים שקיימים רק בקטלוג/מסך ולא קיימים ב-Explanations
    val prefixes = listOf(
        "הגנות פנימיות - ",
        "הגנות פנימיות – ",
        "הגנות פנימיות: ",
        "הגנות חיצוניות - ",
        "הגנות חיצוניות – ",
        "הגנות חיצוניות: ",
        "הגנות פנימיות -",
        "הגנות חיצוניות -",
    )
    prefixes.firstOrNull { t.startsWith(it) }?.let { p ->
        t = t.removePrefix(p).trim()
    }

    // נירמול מקפים ורווחים (עוזר להבדלים כמו "סייד סטפ" מול "סייד-סטפ")
    t = t
        .replace("\u200F", "")
        .replace("\u200E", "")
        .replace("\u00A0", " ")
        .replace("—", "-")
        .replace("–", "-")
        .replace(Regex("\\s*-\\s*"), " - ")
        .replace(Regex("\\s+"), " ")
        .trim()

    // תיקון ידני לנקודה הכי נפוצה אצלך
    t = t.replace("סייד סטפ", "סייד-סטפ")

    return t
}
