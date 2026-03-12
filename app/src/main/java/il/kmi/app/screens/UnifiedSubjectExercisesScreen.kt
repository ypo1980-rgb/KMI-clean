@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.HardSectionsResolver

private val subjectScreenGradientTop = Color(0xFFF2F0FA)
private val subjectScreenGradientMid = Color(0xFFF7F8FC)
private val subjectScreenGradientBottom = Color(0xFFFDFDFE)

@Composable
fun UnifiedSubjectExercisesScreen(    subjectId: String,
    sectionId: String? = null,
    onOpenSection: (subjectId: String, sectionId: String?) -> Unit,
    onBack: () -> Unit
) {
    android.util.Log.e(
        "KMI_HARD",
        "UnifiedSubjectExercisesScreen subjectId='$subjectId' sectionId='$sectionId'"
    )

    val result = remember(subjectId, sectionId) {
        HardSectionsResolver.resolve(subjectId, sectionId)
    }

    android.util.Log.e(
        "KMI_HARD",
        "UnifiedSubjectExercisesScreen result='${result?.javaClass?.simpleName}'"
    )

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
                    android.util.Log.e(
                        "KMI_HARD",
                        "UnifiedSubjectExercisesScreen render Sections title='${result.title}' entries=${result.entries.map { it.id }}"
                    )

                    SectionsContent(
                        subjectId = subjectId,
                        title = result.title,
                        entries = result.entries,
                        onOpen = onOpenSection,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is HardSectionsResolver.NodeResult.BeltGroups -> {
                    android.util.Log.e(
                        "KMI_HARD",
                        "UnifiedSubjectExercisesScreen render BeltGroups title='${result.title}' groups=${result.groups.map { it.belt.id to it.items.size }}"
                    )

                    BeltGroupsContent(
                        title = result.title,
                        groups = result.groups,
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
                text = title ?: "נושאים",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "בחר תת־נושא",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6C6880),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(entries) { entry ->
            SubjectSectionCard(
                title = entry.title,
                count = entry.totalItemsCount,
                onClick = { onOpen(subjectId, entry.id) }
            )
        }
    }
}

@Composable
private fun SubjectSectionCard(
    title: String,
    count: Int,
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
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F4F8)
                ) {
                    Text(
                        text = "$count תרגילים",
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

@Composable
private fun BeltGroupsContent(
    title: String,
    groups: List<HardSectionsResolver.BeltItems>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "תרגילים לפי חגורות",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        items(groups) { group ->
            BeltSectionCard(group = group)
        }
    }
}

@Composable
private fun BeltSectionCard(
    group: HardSectionsResolver.BeltItems
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = beltTitle(group.belt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            group.items.forEachIndexed { index, item ->
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (index != group.items.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun beltTitle(belt: Belt): String =
    when (belt) {
        Belt.YELLOW -> "חגורה צהובה"
        Belt.ORANGE -> "חגורה כתומה"
        Belt.GREEN -> "חגורה ירוקה"
        Belt.BLUE -> "חגורה כחולה"
        Belt.BROWN -> "חגורה חומה"
        Belt.BLACK -> "חגורה שחורה"
        else -> belt.name
    }