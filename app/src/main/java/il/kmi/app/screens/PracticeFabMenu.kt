package il.kmi.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.domain.ContentRepo
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.ext.color
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.catalog.CatalogRepo
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager


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
    @Suppress("UNUSED_PARAMETER")
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit,
    onPracticeByTopicSelected: (belt: Belt, topic: String) -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val textAlignPrimary = if (isEnglish) TextAlign.Start else TextAlign.Right

    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode =
        colorScheme.background.luminance() < 0.5f

    val premiumHeaderBrush = Brush.linearGradient(
        colors = listOf(
            defaultBelt.color,
            defaultBelt.color.copy(alpha = 0.96f),
            MaterialTheme.colorScheme.primary
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
                val topic = selection.topicsByBelt[belt]?.firstOrNull()
                    ?: return@PracticeByTopicsPickerDialog

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
        fun ModernActionRow(
            title: String,
            icon: ImageVector,
            enabled: Boolean,
            onClick: () -> Unit
        ) {
            val shape = RoundedCornerShape(22.dp)

            val interaction = remember {
                MutableInteractionSource()
            }
            val pressed by interaction.collectIsPressedAsState()

            val bg by animateColorAsState(
                targetValue =
                    when {
                        !enabled ->
                            if (isDarkMode) {
                                colorScheme.surfaceVariant.copy(
                                    alpha = 0.55f
                                )
                            } else {
                                Color(0xFFF3F4F6)
                            }

                        pressed ->
                            beltAccent.copy(alpha = 0.14f)

                        else ->
                            if (isDarkMode) {
                                colorScheme.surfaceVariant
                            } else {
                                Color(0xFFF8FAFC)
                            }
                    },
                label = "row_bg"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
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
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color =
                        beltAccent.copy(
                            alpha = if (enabled) {
                                0.18f
                            } else {
                                0.08f
                            }
                        )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        shape = CircleShape,
                        color = beltAccent.copy(
                            alpha = if (enabled) 0.10f else 0.06f
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = beltAccent.copy(alpha = 0.18f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (enabled) {
                                    beltAccent
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                },
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style =
                            KmiTypography.cardTitle.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        },
                        textAlign = if (isEnglish) {
                            TextAlign.Start
                        } else {
                            TextAlign.Right
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = if (enabled) {
                            beltAccent.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                        },
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
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

            title = null,

            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    /*
                     * כרטיס הכותרת הצמוד לרשימת הפעולות.
                     */
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = premiumHeaderBrush,
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 9.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment =
                                    if (isEnglish) {
                                        Alignment.Start
                                    } else {
                                        Alignment.End
                                    },
                                verticalArrangement =
                                    Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = tr(
                                        "תרגול",
                                        "Practice"
                                    ),
                                    color = Color.White,
                                    style =
                                        KmiTypography.sectionTitle.copy(
                                            fontWeight =
                                                FontWeight.ExtraBold
                                        ),
                                    textAlign =
                                        textAlignPrimary,
                                    maxLines = 2,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = tr(
                                        "בחר פעולה כדי להתחיל",
                                        "Choose an action to begin"
                                    ),
                                    color =
                                        Color.White.copy(
                                            alpha = 0.94f
                                        ),
                                    style =
                                        KmiTypography.secondary.copy(
                                            fontWeight =
                                                FontWeight.SemiBold
                                        ),
                                    textAlign =
                                        textAlignPrimary,
                                    maxLines = 2,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                )
                            }

                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape =
                                    RoundedCornerShape(14.dp),
                                color =
                                    Color.White.copy(
                                        alpha = 0.18f
                                    ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        Color.White.copy(
                                            alpha = 0.34f
                                        )
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Box(
                                    contentAlignment =
                                        Alignment.Center
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Filled.Topic,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier =
                                            Modifier.size(21.dp)
                                    )
                                }
                            }
                        }
                    }

                    /*
                     * שלושת כרטיסי הפעולה.
                     */
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(
                                    horizontal = 0.dp,
                                    vertical = 0.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            ModernActionRow(
                                title = tr(
                                    "תרגול אקראי - $beltName",
                                    "Random Practice - $beltName"
                                ),
                                icon = Icons.Filled.Casino,
                                enabled = canUseExtras,
                                onClick = {
                                    onRandomPractice(
                                        defaultBelt
                                    )
                                }
                            )

                            ModernActionRow(
                                title = tr(
                                    "מבחן מסכם - $beltName",
                                    "Final Exam - $beltName"
                                ),
                                icon =
                                    Icons.Filled.AssignmentTurnedIn,
                                enabled = canUseExtras,
                                onClick = {
                                    onFinalExam(
                                        defaultBelt
                                    )
                                }
                            )

                            ModernActionRow(
                                title = tr(
                                    "תרגול לפי נושא",
                                    "Practice by Topic"
                                ),
                                icon = Icons.Filled.Topic,
                                enabled = canUseExtras,
                                onClick = {
                                    showTopicsPicker = true
                                }
                            )

                            if (!canUseExtras) {
                                Surface(
                                    shape =
                                        RoundedCornerShape(
                                            16.dp
                                        ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                            .copy(
                                                alpha = 0.08f
                                            ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .error
                                                .copy(
                                                    alpha = 0.18f
                                                )
                                    )
                                ) {
                                    Text(
                                        text = tr(
                                            "אפשרויות התרגול זמינות רק בהרשאות Extras/מנוי.",
                                            "Practice options are available only with Extras / subscription access."
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            ),
                                        color = colorScheme.error,
                                        style = KmiTypography.caption,
                                        textAlign = textAlignPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = tr(
                            "סגור",
                            "Close"
                        ),
                        style =
                            KmiTypography.action.copy(
                                fontWeight =
                                    FontWeight.SemiBold
                            ),
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

    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode =
        colorScheme.background.luminance() < 0.5f

    val graniteBrush =
        Brush.linearGradient(
            colors =
                if (isDarkMode) {
                    listOf(
                        Color(0xFF171B24),
                        Color(0xFF1D2230),
                        Color(0xFF222737),
                        Color(0xFF181D28)
                    )
                } else {
                    listOf(
                        Color(0xFFF7F2FA),
                        Color(0xFFF1EAF6),
                        Color(0xFFECE5F3),
                        Color(0xFFF8F4FA)
                    )
                }
        )

    // ✅ RTL לכל הדיאלוג
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
    ) {

        // ✅ בלי חגורה לבנה ברשימה בכלל
        val allBelts = remember { Belt.order.filterNot { it == Belt.WHITE } }

        @Composable
        fun topicTitlesForBelt(belt: Belt): List<String> {
            return remember(belt) {
                val sharedBelt =
                    runCatching {
                        Belt.fromId(belt.id)
                    }.getOrNull()
                        ?: Belt.WHITE

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
        var selectedBelt by rememberSaveable(initialBelts) {
            mutableStateOf(
                initialBelts
                    .firstOrNull { belt ->
                        belt != Belt.WHITE
                    }
            )
        }

        var selectedTopic by rememberSaveable {
            mutableStateOf<String?>(null)
        }

        var beltMenuExpanded by rememberSaveable {
            mutableStateOf(false)
        }

        val topics =
            selectedBelt
                ?.let {
                    topicTitlesForBelt(it)
                }
                .orEmpty()

        val selectedBeltAccent =
            selectedBelt?.color
                ?: MaterialTheme.colorScheme.primary

        // כרטיסי הנושאים משתמשים בצבע החגורה שנבחרה.
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
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                    ) {
                        Text(
                            text = tr(
                                "תרגול לפי נושא",
                                "Practice by Topic"
                            ),
                            style =
                                KmiTypography.screenTitle.copy(
                                    fontWeight = FontWeight.Black
                                ),
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
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
                            .background(
                                brush = graniteBrush,
                                shape = RoundedCornerShape(26.dp)
                            )
                            .verticalScroll(scrollState)
                            .padding(
                                horizontal = 7.dp,
                                vertical = 8.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        /*
                         * בחירת חגורה נשארת Dropdown.
                         */
                        ExposedDropdownMenuBox(
                            expanded = beltMenuExpanded,
                            onExpandedChange = {
                                beltMenuExpanded =
                                    !beltMenuExpanded
                            }
                        ) {
                            Surface(
                                modifier = Modifier
                                    .menuAnchor(
                                        type =
                                            MenuAnchorType
                                                .PrimaryNotEditable,
                                        enabled = true
                                    )
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color =
                                    if (isDarkMode) {
                                        colorScheme.surfaceVariant
                                    } else {
                                        Color.White
                                    },
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        selectedBeltAccent.copy(
                                            alpha = 0.30f
                                        )
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 10.dp
                                        ),
                                    verticalAlignment =
                                        Alignment.CenterVertically,
                                    horizontalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = CircleShape,
                                        color =
                                            selectedBeltAccent.copy(
                                                alpha = 0.11f
                                            ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color =
                                                selectedBeltAccent.copy(
                                                    alpha = 0.22f
                                                )
                                        )
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier.fillMaxSize(),
                                            contentAlignment =
                                                Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(
                                                        color =
                                                            selectedBeltAccent,
                                                        shape =
                                                            CircleShape
                                                    )
                                            )
                                        }
                                    }

                                    Text(
                                        text =
                                            selectedBelt?.let { belt ->
                                                if (isEnglish) {
                                                    belt.en
                                                } else {
                                                    belt.heb
                                                }
                                            } ?: tr(
                                                "בחר חגורה",
                                                "Choose Belt"
                                            ),
                                        modifier =
                                            Modifier.weight(1f),
                                        color =
                                            if (selectedBelt != null) {
                                                colorScheme.onSurface
                                            } else {
                                                colorScheme.onSurfaceVariant
                                            },
                                        style =
                                            KmiTypography.cardTitle.copy(
                                                fontWeight =
                                                    if (selectedBelt != null) {
                                                        FontWeight.Bold
                                                    } else {
                                                        FontWeight.Medium
                                                    }
                                            ),
                                        textAlign =
                                            textAlignPrimary,
                                        maxLines = 1
                                    )

                                    Surface(
                                        modifier = Modifier.size(30.dp),
                                        shape = CircleShape,
                                        color =
                                            selectedBeltAccent.copy(
                                                alpha = 0.09f
                                            )
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier.fillMaxSize(),
                                            contentAlignment =
                                                Alignment.Center
                                        ) {
                                            ExposedDropdownMenuDefaults
                                                .TrailingIcon(
                                                    expanded =
                                                        beltMenuExpanded
                                                )
                                        }
                                    }
                                }
                            }

                            ExposedDropdownMenu(
                                expanded = beltMenuExpanded,
                                onDismissRequest = {
                                    beltMenuExpanded = false
                                }
                            ) {
                                allBelts.forEach { belt ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment =
                                                    Alignment.CenterVertically,
                                                horizontalArrangement =
                                                    Arrangement.spacedBy(
                                                        10.dp
                                                    )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(
                                                            color =
                                                                belt.color,
                                                            shape =
                                                                CircleShape
                                                        )
                                                )

                                                Text(
                                                    text =
                                                        if (isEnglish) {
                                                            belt.en
                                                        } else {
                                                            belt.heb
                                                        },
                                                    style =
                                                        KmiTypography.body.copy(
                                                            fontWeight =
                                                                if (
                                                                    selectedBelt ==
                                                                    belt
                                                                ) {
                                                                    FontWeight.Bold
                                                                } else {
                                                                    FontWeight.Medium
                                                                }
                                                        ),
                                                    color =
                                                        colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow =
                                                        TextOverflow.Ellipsis
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

                        AnimatedVisibility(
                            visible = selectedBelt == null
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color =
                                    if (isDarkMode) {
                                        colorScheme.surfaceVariant.copy(
                                            alpha = 0.86f
                                        )
                                    } else {
                                        Color.White.copy(
                                            alpha = 0.72f
                                        )
                                    },
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        colorScheme.outlineVariant.copy(
                                            alpha = 0.65f
                                        )
                                )
                            ) {
                                Text(
                                    text = tr(
                                        "בחר חגורה כדי להציג את הנושאים",
                                        "Choose a belt to display the topics"
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 14.dp,
                                            vertical = 12.dp
                                        ),
                                    color =
                                        colorScheme.onSurfaceVariant,
                                    style =
                                        KmiTypography.secondary.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        /*
                         * לאחר בחירת חגורה מוצגים הנושאים
                         * ככרטיסי פרימיום ולא כ־Dropdown.
                         */
                        AnimatedVisibility(
                            visible = selectedBelt != null
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr(
                                        "בחר נושא",
                                        "Choose a topic"
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 5.dp,
                                            vertical = 2.dp
                                        ),
                                    color = colorScheme.onSurface,
                                    style =
                                        KmiTypography.body.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                    textAlign = textAlignPrimary,
                                    maxLines = 2
                                )

                                topics.forEach { topic ->
                                    val isSelected =
                                        selectedTopic == topic

                                    val topicShape =
                                        RoundedCornerShape(20.dp)

                                    val topicInteraction =
                                        remember(topic) {
                                            MutableInteractionSource()
                                        }

                                    val topicPressed by
                                    topicInteraction
                                        .collectIsPressedAsState()

                                    val topicBackground by
                                    animateColorAsState(
                                        targetValue =
                                            when {
                                                isSelected ->
                                                    selectedBeltAccent
                                                        .copy(
                                                            alpha =
                                                                0.16f
                                                        )

                                                topicPressed ->
                                                    selectedBeltAccent
                                                        .copy(
                                                            alpha =
                                                                0.10f
                                                        )

                                                else ->
                                                    if (isDarkMode) {
                                                        colorScheme.surfaceVariant
                                                    } else {
                                                        Color.White
                                                    }
                                            },
                                        label =
                                            "topic_card_background"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 62.dp)
                                            .clip(topicShape)
                                            .clickable(
                                                interactionSource =
                                                    topicInteraction,
                                                indication =
                                                    LocalIndication.current
                                            ) {
                                                val belt =
                                                    selectedBelt
                                                        ?: return@clickable

                                                selectedTopic = topic

                                                onConfirm(
                                                    PracticeByTopicsSelection(
                                                        belts =
                                                            setOf(belt),
                                                        topicsByBelt =
                                                            mapOf(
                                                                belt to
                                                                        setOf(
                                                                            topic
                                                                        )
                                                            )
                                                    )
                                                )
                                            },
                                        shape = topicShape,
                                        color = topicBackground,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color =
                                                if (isSelected) {
                                                    selectedBeltAccent
                                                        .copy(
                                                            alpha =
                                                                0.48f
                                                        )
                                                } else {
                                                    selectedBeltAccent
                                                        .copy(
                                                            alpha =
                                                                0.17f
                                                        )
                                                }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = 12.dp,
                                                    vertical = 9.dp
                                                ),
                                            verticalAlignment =
                                                Alignment.CenterVertically,
                                            horizontalArrangement =
                                                Arrangement.spacedBy(
                                                    10.dp
                                                )
                                        ) {
                                            Surface(
                                                modifier =
                                                    Modifier.size(40.dp),
                                                shape = CircleShape,
                                                color =
                                                    selectedBeltAccent
                                                        .copy(
                                                            alpha =
                                                                if (
                                                                    isSelected
                                                                ) {
                                                                    0.20f
                                                                } else {
                                                                    0.11f
                                                                }
                                                        ),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color =
                                                        selectedBeltAccent
                                                            .copy(
                                                                alpha =
                                                                    0.25f
                                                            )
                                                )
                                            ) {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxSize(),
                                                    contentAlignment =
                                                        Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector =
                                                            Icons.Filled.Topic,
                                                        contentDescription =
                                                            null,
                                                        tint =
                                                            selectedBeltAccent,
                                                        modifier =
                                                            Modifier.size(
                                                                20.dp
                                                            )
                                                    )
                                                }
                                            }

                                            Text(
                                                text =
                                                    topicDisplayName(
                                                        topic
                                                    ),
                                                modifier =
                                                    Modifier.weight(1f),
                                                color =
                                                    colorScheme.onSurface,
                                                style =
                                                    KmiTypography.cardTitle.copy(
                                                        fontWeight =
                                                            if (isSelected) {
                                                                FontWeight.Black
                                                            } else {
                                                                FontWeight.SemiBold
                                                            }
                                                    ),

                                                textAlign =
                                                    textAlignPrimary,
                                                maxLines = 2,
                                                overflow =
                                                    TextOverflow.Ellipsis
                                            )

                                            Icon(
                                                imageVector =
                                                    Icons.Filled.ChevronLeft,
                                                contentDescription = null,
                                                tint =
                                                    selectedBeltAccent
                                                        .copy(
                                                            alpha = 0.72f
                                                        ),
                                                modifier = Modifier
                                                    .size(19.dp)
                                                    .graphicsLayer {
                                                        scaleX =
                                                            if (
                                                                isEnglish
                                                            ) {
                                                                -1f
                                                            } else {
                                                                1f
                                                            }
                                                    }
                                            )
                                        }
                                    }
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
                    color =
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.82f
                        ),
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme.colorScheme.outline
                                .copy(alpha = 0.14f)
                    )
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            text = tr(
                                "סגור",
                                "Close"
                            ),
                            style =
                                KmiTypography.action.copy(
                                    fontWeight =
                                        FontWeight.SemiBold
                                ),
                            color =
                                MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = 0.80f)
                        )
                    }
                }
            }
        )
    }
}