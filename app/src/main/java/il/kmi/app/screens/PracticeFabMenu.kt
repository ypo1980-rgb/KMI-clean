// File: app/src/main/java/il/kmi/app/screens/PracticeFabMenu.kt
package il.kmi.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.ext.color
import il.kmi.shared.domain.Belt
import il.kmi.app.domain.ContentRepo
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

@Immutable
data class PracticeByTopicsSelection(
    val belts: Set<Belt>,
    /** topic title strings כפי שמופיעים במערכת */
    val topicsByBelt: Map<Belt, Set<String>>
)

@Composable
fun PracticeMenuDialog(
    contentRepo: ContentRepo = ContentRepo,
    canUseExtras: Boolean,
    defaultBelt: Belt,
    onDismiss: () -> Unit,
    onRandomPractice: (Belt) -> Unit,
    onFinalExam: (Belt) -> Unit,
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit
) {
    var showTopicsPicker by rememberSaveable { mutableStateOf(false) }

    if (showTopicsPicker) {
        PracticeByTopicsPickerDialog(
            contentRepo = contentRepo,
            initialBelts = setOf(defaultBelt),
            onDismiss = { showTopicsPicker = false },
            onConfirm = { selection ->
                showTopicsPicker = false
                onPracticeByTopics(selection)
            }
        )
        return
    }

    // ✅ accent לפי החגורה במסך הנוכחי
    val beltAccent = defaultBelt.color
    val beltName = "(${defaultBelt.heb})"

    // ✅ הכל RTL בתוך הדיאלוג
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        @Composable
        fun GradientDivider(modifier: Modifier = Modifier) {
            Box(
                modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                beltAccent.copy(alpha = 0.22f),
                                beltAccent.copy(alpha = 0.40f),
                                beltAccent.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }

        @Composable
        fun ModernActionRow(
            title: String,
            icon: ImageVector,
            enabled: Boolean,
            onClick: () -> Unit
        ) {
            val shape = RoundedCornerShape(18.dp)
            val border = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)

            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()

            val bg by animateColorAsState(
                targetValue = when {
                    !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
                    pressed -> beltAccent.copy(alpha = 0.08f)
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "row_bg"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .clickable(
                        enabled = enabled,
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = onClick
                    ),
                shape = shape,
                color = bg,
                tonalElevation = 0.dp,
                shadowElevation = if (enabled) 8.dp else 0.dp,
                border = BorderStroke(1.dp, border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start // RTL: Start=ימין
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = beltAccent.copy(alpha = if (enabled) 0.10f else 0.06f),
                        border = BorderStroke(1.dp, beltAccent.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (enabled) beltAccent
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        textAlign = TextAlign.Right
                    )

                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = if (enabled) beltAccent.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                    )
                }
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),

            title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start // RTL: Start=ימין
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = beltAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, beltAccent.copy(alpha = 0.18f))
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Topic,
                                    contentDescription = null,
                                    tint = beltAccent
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = "תרגול",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "בחר פעולה כדי להתחיל",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    GradientDivider()
                }
            },

            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernActionRow(
                        title = "תרגול אקראי - $beltName",
                        icon = Icons.Filled.Casino,
                        enabled = canUseExtras,
                        onClick = { onRandomPractice(defaultBelt) }
                    )

                    ModernActionRow(
                        title = "מבחן מסכם - $beltName",
                        icon = Icons.Filled.AssignmentTurnedIn,
                        enabled = canUseExtras,
                        onClick = { onFinalExam(defaultBelt) }
                    )

                    ModernActionRow(
                        title = "תרגול לפי נושא",
                        icon = Icons.Filled.Topic,
                        enabled = canUseExtras,
                        onClick = { showTopicsPicker = true }
                    )

                    if (!canUseExtras) {
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
                        ) {
                            Text(
                                text = "אפשרויות התרגול זמינות רק בהרשאות Extras/מנוי.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("סגור", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeByTopicsPickerDialog(
    contentRepo: ContentRepo = ContentRepo,
    initialBelts: Set<Belt>,
    onDismiss: () -> Unit,
    onConfirm: (PracticeByTopicsSelection) -> Unit
) {
    // ✅ RTL לכל הדיאלוג
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        @Suppress("UNUSED_PARAMETER")
        val _unused = contentRepo // כרגע אנחנו לא משתמשים בו כאן

        // ✅ בלי חגורה לבנה ברשימה בכלל
        val allBelts = remember { Belt.order.filterNot { it == Belt.WHITE } }

        var selectedBelts by rememberSaveable {
            mutableStateOf(
                (initialBelts.ifEmpty { setOf(Belt.GREEN) })
                    .filterNot { it == Belt.WHITE }
                    .toSet()
                    .ifEmpty { setOf(Belt.GREEN) }
            )
        }

        var topicsByBelt by rememberSaveable {
            mutableStateOf<Map<Belt, Set<String>>>(emptyMap())
        }

        fun topicTitlesForBelt(belt: Belt): List<String> {
            val viaBridge = runCatching {
                il.kmi.app.search.KmiSearchBridge.topicTitlesFor(belt)
            }.getOrDefault(emptyList())
            if (viaBridge.isNotEmpty()) return viaBridge

            return runCatching {
                val sharedBelt = il.kmi.shared.domain.Belt.fromId(belt.id)
                    ?: il.kmi.shared.domain.Belt.WHITE

                il.kmi.shared.domain.SubTopicRegistry
                    .allForBelt(sharedBelt)
                    .keys
                    .toList()
            }.getOrDefault(emptyList())
        }

        val canConfirm =
            selectedBelts.isNotEmpty() &&
                    selectedBelts.any { b -> topicsByBelt[b].orEmpty().isNotEmpty() }

        // ✅ Accordion state
        var beltsExpanded by rememberSaveable { mutableStateOf(true) }
        var topicsExpanded by rememberSaveable { mutableStateOf(false) }

        fun toggleBelts() {
            beltsExpanded = !beltsExpanded
            if (beltsExpanded) topicsExpanded = false
        }

        fun toggleTopics() {
            topicsExpanded = !topicsExpanded
            if (topicsExpanded) beltsExpanded = false
        }

        @Composable
        fun AccordionHeader(
            title: String,
            subtitle: String?,
            expanded: Boolean,
            onClick: () -> Unit
        ) {
            val rot by animateFloatAsState(
                targetValue = if (expanded) 90f else 0f,
                label = "chevRot"
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start // RTL: Start=ימין
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Right,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.graphicsLayer { rotationZ = rot }
                    )
                }
            }
        }

        // ✅ NEW: כרטיס נושא "אפליקציה מובילה" כמו במסך נושאים
        @Composable
        fun PremiumTopicTile(
            title: String,
            subtitle: String,
            badgeText: String,
            accent: Color,
            selected: Boolean,
            onClick: () -> Unit
        ) {
            val shape = RoundedCornerShape(18.dp)
            val bg = if (selected) Color(0xFFEFF6FF) else Color(0xFFF2EFF6)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .clickable(onClick = onClick),
                shape = shape,
                color = bg,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(
                    1.dp,
                    if (selected) accent.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start // RTL: Start=ימין
                ) {
                    // פס צבע מימין
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(54.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        accent.copy(alpha = 0.95f),
                                        accent.copy(alpha = 0.55f)
                                    )
                                ),
                                shape = RoundedCornerShape(999.dp)
                            )
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = accent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(26.dp),

            title = {
                Text(
                    text = "תרגול לפי נושא",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            },

            text = {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // =========================
                    // 1) Accordion: חגורות
                    // =========================
                    val beltsSubtitle = remember(selectedBelts) {
                        when (selectedBelts.size) {
                            0 -> "לא נבחרה חגורה"
                            1 -> "נבחרה: ${selectedBelts.first().heb}"
                            else -> "נבחרו ${selectedBelts.size} חגורות"
                        }
                    }

                    AccordionHeader(
                        title = "בחר חגורה/ות",
                        subtitle = beltsSubtitle,
                        expanded = beltsExpanded,
                        onClick = { toggleBelts() }
                    )

                    AnimatedVisibility(visible = beltsExpanded) {

                        val pickedCount = selectedBelts.size
                        val allCount = allBelts.size
                        val allPicked = pickedCount == allCount

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFFF7F8FC),
                            tonalElevation = 0.dp,
                            shadowElevation = 10.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start // RTL: Start=ימין
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "חגורות",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Right,
                                            color = Color.Black,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = if (pickedCount == 0) "לא נבחרו חגורות"
                                            else "נבחרו $pickedCount מתוך $allCount",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Right,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            selectedBelts = if (allPicked) emptySet() else allBelts.toSet()
                                            if (selectedBelts.isEmpty()) topicsByBelt = emptyMap()
                                        }
                                    ) {
                                        Text(if (allPicked) "נקה" else "בחר הכל")
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    allBelts.forEach { belt ->
                                        val picked = belt in selectedBelts
                                        FilterChip(
                                            selected = picked,
                                            onClick = {
                                                selectedBelts =
                                                    if (picked) selectedBelts - belt
                                                    else selectedBelts + belt

                                                if (belt !in selectedBelts) {
                                                    topicsByBelt = topicsByBelt.toMutableMap().apply { remove(belt) }
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = belt.heb,
                                                    fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))

                    // =========================
                    // 2) Accordion: נושאים
                    // =========================
                    val topicsCount = remember(selectedBelts, topicsByBelt) {
                        selectedBelts.sumOf { b -> topicsByBelt[b].orEmpty().size }
                    }
                    val topicsSubtitle = when {
                        selectedBelts.isEmpty() -> "בחר קודם חגורה אחת לפחות"
                        topicsCount == 0 -> "לא נבחרו נושאים"
                        topicsCount == 1 -> "נבחר נושא אחד"
                        else -> "נבחרו $topicsCount נושאים"
                    }

                    AccordionHeader(
                        title = "בחר נושא",
                        subtitle = topicsSubtitle,
                        expanded = topicsExpanded,
                        onClick = { toggleTopics() }
                    )

                    AnimatedVisibility(visible = topicsExpanded) {

                        val totalPickedTopics = remember(selectedBelts, topicsByBelt) {
                            selectedBelts.sumOf { b -> topicsByBelt[b].orEmpty().size }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            // Summary card
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFFF7F8FC),
                                tonalElevation = 0.dp,
                                shadowElevation = 10.dp,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start // RTL: Start=ימין
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "נושאים",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Right,
                                            color = Color.Black,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = when {
                                                selectedBelts.isEmpty() -> "בחר קודם חגורה אחת לפחות"
                                                totalPickedTopics == 0 -> "לא נבחרו נושאים"
                                                totalPickedTopics == 1 -> "נבחר נושא אחד"
                                                else -> "נבחרו $totalPickedTopics נושאים"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Right,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    TextButton(
                                        enabled = selectedBelts.isNotEmpty(),
                                        onClick = {
                                            topicsByBelt = topicsByBelt.toMutableMap().apply {
                                                selectedBelts.forEach { b -> remove(b) }
                                            }
                                        }
                                    ) { Text("נקה הכל") }
                                }
                            }

                            // Per-belt cards
                            selectedBelts
                                .sortedBy { Belt.order.indexOf(it).let { idx -> if (idx >= 0) idx else 999 } }
                                .forEach { belt ->

                                    val topics = topicTitlesForBelt(belt)
                                    val pickedSet = topicsByBelt[belt].orEmpty()
                                    val allSelected = topics.isNotEmpty() && pickedSet.size == topics.size

                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.White,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 10.dp,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start // RTL: Start=ימין
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    Text(
                                                        text = belt.heb,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        textAlign = TextAlign.Right,
                                                        color = Color.Black,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = if (topics.isEmpty()) "אין נושאים לחגורה הזו"
                                                        else "נבחרו ${pickedSet.size} מתוך ${topics.size}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        textAlign = TextAlign.Right,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }

                                                TextButton(
                                                    enabled = topics.isNotEmpty(),
                                                    onClick = {
                                                        topicsByBelt = topicsByBelt.toMutableMap().apply {
                                                            this[belt] = if (allSelected) emptySet() else topics.toSet()
                                                        }
                                                    }
                                                ) { Text(if (allSelected) "נקה" else "בחר הכל") }
                                            }

                                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))

                                            if (topics.isEmpty()) {
                                                Text(
                                                    text = "אין נושאים להצגה",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    textAlign = TextAlign.Right,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            } else {
                                                // ✅ במקום FlowRow+FilterChip — כרטיסים "כמו במסך נושאים"
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    topics.forEach { t ->
                                                        val checked = t in pickedSet

                                                        // אם יש לך belt.color בפרויקט, אפשר להחליף כאן ל: val accent = belt.color
                                                        val accent = belt.color

                                                        PremiumTopicTile(
                                                            title = t,
                                                            subtitle = if (checked) "נבחר" else "הקש כדי לבחור",
                                                            badgeText = if (checked) "מסומן" else "נושא",
                                                            accent = accent,
                                                            selected = checked,
                                                            onClick = {
                                                                topicsByBelt = topicsByBelt.toMutableMap().apply {
                                                                    val cur = this[belt].orEmpty().toMutableSet()
                                                                    if (!cur.add(t)) cur.remove(t)
                                                                    this[belt] = cur
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            if (!canConfirm) {
                                Text(
                                    text = "כדי להתחיל — בחר לפחות נושא אחד.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(
                    enabled = canConfirm,
                    onClick = {
                        val cleaned: Map<Belt, Set<String>> =
                            selectedBelts
                                .filterNot { it == Belt.WHITE }
                                .associateWith { b ->
                                    topicsByBelt[b].orEmpty()
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .toSet()
                                }
                                .filterValues { it.isNotEmpty() }

                        if (cleaned.isEmpty()) return@TextButton

                        onConfirm(
                            PracticeByTopicsSelection(
                                belts = cleaned.keys,
                                topicsByBelt = cleaned
                            )
                        )

                        onDismiss()
                    }
                ) { Text("התחל") }
            },

            dismissButton = {
                TextButton(onClick = onDismiss) { Text("ביטול") }
            }
        )
    }
}