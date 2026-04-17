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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import il.kmi.shared.domain.catalog.CatalogRepo
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import java.util.LinkedHashSet

//============================================================================

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
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit,
    onPracticeByTopicSelected: (belt: Belt, topic: String) -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val textAlignPrimary = if (isEnglish) TextAlign.Start else TextAlign.Right

    val graniteBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF7F2FA),
            Color(0xFFF1EAF6),
            Color(0xFFECE5F3),
            Color(0xFFF8F4FA)
        )
    )

    val premiumHeaderBrush = Brush.linearGradient(
        colors = listOf(
            defaultBelt.color.copy(alpha = 0.92f),
            defaultBelt.color.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
        )
    )

    var showTopicsPicker by rememberSaveable { mutableStateOf(false) }

    if (showTopicsPicker) {
        PracticeByTopicsPickerDialog(
            contentRepo = contentRepo,
            initialBelts = emptySet(),
            isEnglish = isEnglish,
            onDismiss = { showTopicsPicker = false },
            onConfirm = { selection ->
                val belt = selection.belts.firstOrNull() ?: return@PracticeByTopicsPickerDialog
                val topic = selection.topicsByBelt[belt]?.firstOrNull() ?: return@PracticeByTopicsPickerDialog

                showTopicsPicker = false
                onPracticeByTopicSelected(belt, topic)
            }
        )
        return
    }

    // ✅ accent לפי החגורה במסך הנוכחי
    val beltAccent = defaultBelt.color
    val beltName = tr("(${defaultBelt.heb})", "(${defaultBelt.en})")

    // ✅ הכל RTL בתוך הדיאלוג
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {

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
            val shape = RoundedCornerShape(22.dp)
            val border = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)

            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()

            val bg by animateColorAsState(
                targetValue = when {
                    !enabled -> Color.White.copy(alpha = 0.60f)
                    pressed -> beltAccent.copy(alpha = 0.10f)
                    else -> Color.White.copy(alpha = 0.84f)
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
                shadowElevation = if (enabled) 12.dp else 0.dp,
                border = BorderStroke(1.dp, border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
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
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right
                    )

                    Icon(
                        imageVector = if (isEnglish) Icons.Filled.ChevronLeft else Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = if (enabled) beltAccent.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                        modifier = Modifier.graphicsLayer {
                            scaleX = if (isEnglish) -1f else 1f
                        }
                    )
                }
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(30.dp),

            title = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(premiumHeaderBrush, RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Topic,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                ) {
                                    Text(
                                        text = tr("תרגול", "Practice"),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = textAlignPrimary,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = tr("בחר פעולה כדי להתחיל", "Choose an action to begin"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.88f),
                                        textAlign = textAlignPrimary,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            GradientDivider()
                        }
                    }
                }
            },

            text = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(graniteBrush, RoundedCornerShape(28.dp))
                            .padding(horizontal = 2.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernActionRow(
                            title = tr("תרגול אקראי - $beltName", "Random Practice - $beltName"),
                            icon = Icons.Filled.Casino,
                            enabled = canUseExtras,
                            onClick = { onRandomPractice(defaultBelt) }
                        )

                        ModernActionRow(
                            title = tr("מבחן מסכם - $beltName", "Final Exam - $beltName"),
                            icon = Icons.Filled.AssignmentTurnedIn,
                            enabled = canUseExtras,
                            onClick = { onFinalExam(defaultBelt) }
                        )

                        ModernActionRow(
                            title = tr("תרגול לפי נושא", "Practice by Topic"),
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
                                    text = tr(
                                        "אפשרויות התרגול זמינות רק בהרשאות Extras/מנוי.",
                                        "Practice options are available only with Extras / subscription access."
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = textAlignPrimary
                                )
                            }
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        tr("סגור", "Close"),
                        fontWeight = FontWeight.SemiBold,
                        color = beltAccent
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeByTopicsPickerDialog(
    @Suppress("UNUSED_PARAMETER") contentRepo: ContentRepo = ContentRepo,
    initialBelts: Set<Belt>,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PracticeByTopicsSelection) -> Unit
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val textAlignPrimary = if (isEnglish) TextAlign.Start else TextAlign.Right

    val graniteBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF7F2FA),
            Color(0xFFF1EAF6),
            Color(0xFFECE5F3),
            Color(0xFFF8F4FA)
        )
    )

    // ✅ RTL לכל הדיאלוג
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {

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

        @Composable
        fun topicTitlesForBelt(belt: Belt): List<String> {
            return remember(belt) {
                val sharedBelt = runCatching {
                    il.kmi.shared.domain.Belt.fromId(belt.id)
                }.getOrNull() ?: il.kmi.shared.domain.Belt.WHITE

                val ordered = LinkedHashSet<String>()

                val viaBridge = runCatching {
                    il.kmi.app.search.KmiSearchBridge.topicTitlesFor(belt)
                }.getOrDefault(emptyList())

                val viaCatalog = runCatching {
                    CatalogRepo.listTopicTitles(sharedBelt)
                }.getOrDefault(emptyList())

                val viaSubTopics = runCatching {
                    il.kmi.shared.domain.SubTopicRegistry
                        .allForBelt(sharedBelt)
                        .keys
                        .toList()
                }.getOrDefault(emptyList())

                fun addAll(items: List<String>) {
                    items.asSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { ordered.add(it) }
                }

                addAll(viaBridge)
                addAll(viaCatalog)
                addAll(viaSubTopics)

                ordered.toList()
            }
        }

        // מצב הבחירה החדש – חגורה אחת ונושא אחד
        var selectedBelt by rememberSaveable { mutableStateOf<Belt?>(null) }
        var selectedTopic by rememberSaveable { mutableStateOf<String?>(null) }

        var beltMenuExpanded by rememberSaveable { mutableStateOf(false) }
        var topicMenuExpanded by rememberSaveable { mutableStateOf(false) }

        val topics = selectedBelt?.let { topicTitlesForBelt(it) }.orEmpty()

        val selectedBeltAccent = selectedBelt?.color ?: MaterialTheme.colorScheme.primary
        val selectedBeltFieldBg = selectedBeltAccent.copy(alpha = 0.08f)
        val selectedBeltFieldBorder = selectedBeltAccent.copy(alpha = 0.22f)

        // ✅ NEW: כרטיס נושא "אפליקציה מובילה" כמו במסך נושאים
        fun topicDisplayName(topic: String): String {
            if (!isEnglish) return topic
            return when (topic.trim()) {
                "כללי" -> "General"
                "עבודת ידיים" -> "Hand techniques"
                "בעיטות" -> "Kicks"
                "שחרורים" -> "Releases"
                "הגנות" -> "Defenses"
                "נפילות" -> "Breakfalls"
                "קרקע" -> "Ground"
                "כושר" -> "Fitness"
                "קוואלר" -> "Kavaler"
                else -> topic
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.Transparent,

            title = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.82f)
                                    )
                                ),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = tr("תרגול לפי נושא", "Practice by Topic"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },

            text = {
                val scrollState = rememberScrollState()

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(graniteBrush, RoundedCornerShape(26.dp))
                            .verticalScroll(scrollState)
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = beltMenuExpanded,
                            onExpandedChange = { beltMenuExpanded = !beltMenuExpanded }
                        ) {
                            Surface(
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = selectedBeltFieldBg,
                                shadowElevation = 8.dp,
                                border = BorderStroke(1.dp, selectedBeltFieldBorder)
                            ) {
                                TextField(
                                    value = selectedBelt?.let { if (isEnglish) it.en else it.heb }
                                        ?: tr("בחר חגורה", "Choose Belt"),
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = textAlignPrimary
                                    ),
                                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    ),
                                    leadingIcon = {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = selectedBeltAccent.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, selectedBeltAccent.copy(alpha = 0.20f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                                    .width(10.dp)
                                                    .height(10.dp)
                                                    .background(
                                                        selectedBeltAccent,
                                                        RoundedCornerShape(999.dp)
                                                    )
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = beltMenuExpanded)
                                    }
                                )
                            }

                            ExposedDropdownMenu(
                                expanded = beltMenuExpanded,
                                onDismissRequest = { beltMenuExpanded = false }
                            ) {
                                allBelts.forEach { belt ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(10.dp)
                                                        .height(10.dp)
                                                        .background(
                                                            belt.color,
                                                            RoundedCornerShape(999.dp)
                                                        )
                                                )
                                                Text(
                                                    text = if (isEnglish) belt.en else belt.heb,
                                                    fontWeight = if (selectedBelt == belt) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedBelt = belt
                                            selectedTopic = null
                                            beltMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = topicMenuExpanded,
                            onExpandedChange = {
                                if (selectedBelt != null) topicMenuExpanded = !topicMenuExpanded
                            }
                        ) {
                            Surface(
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = selectedBeltFieldBg,
                                shadowElevation = 8.dp,
                                border = BorderStroke(1.dp, selectedBeltFieldBorder)
                            ) {
                                TextField(
                                    value = selectedTopic?.let { topicDisplayName(it) }
                                        ?: tr("בחר נושא", "Choose Topic"),
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = selectedBelt != null,
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = textAlignPrimary
                                    ),
                                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    ),
                                    leadingIcon = {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = selectedBeltAccent.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, selectedBeltAccent.copy(alpha = 0.20f))
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Topic,
                                                    contentDescription = null,
                                                    tint = selectedBeltAccent
                                                )
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicMenuExpanded)
                                    }
                                )
                            }

                            ExposedDropdownMenu(
                                expanded = topicMenuExpanded,
                                onDismissRequest = { topicMenuExpanded = false }
                            ) {
                                topics.forEach { topic ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = topicDisplayName(topic),
                                                fontWeight = if (selectedTopic == topic) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        onClick = {
                                            val belt = selectedBelt ?: return@DropdownMenuItem

                                            selectedTopic = topic
                                            topicMenuExpanded = false

                                            onConfirm(
                                                PracticeByTopicsSelection(
                                                    belts = setOf(belt),
                                                    topicsByBelt = mapOf(belt to setOf(topic))
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },

            confirmButton = {},

            dismissButton = {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            tr("סגור", "Close"),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        )
    }
}