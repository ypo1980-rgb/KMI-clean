package il.kmi.app.navigation.defenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import il.kmi.app.ui.color
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.HardSectionsCatalog

@Composable
fun DefensesListScreen(
    title: String,
    groupedItems: List<Pair<Belt, List<String>>>,
    itemTitle: (String) -> String,
    onBack: () -> Unit,
    onItemClick: ((Belt, String) -> Unit)? = null
) {
    val scroll = rememberScrollState()

    // ✅ הסבר רק אחרי לחיצה (ולא כסאב-טייטל ברשימה)
    val explainFor = remember { mutableStateOf<Pair<Belt, String>?>(null) }

    explainFor.value?.let { (belt, clickedTitle) ->
        val key = explanationKeyForDefenses(clickedTitle)
        val full = il.kmi.app.domain.Explanations.get(belt, key)

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
                fontWeight = FontWeight.Bold
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

        // ✅ DEBUG: הוכחה שהנתונים הם בדיוק מהקטלוג הקשיח (רק למסכי שחרורים)
        if (title.contains("שחרור") || title.contains("שחרורים")) {

            val hardMap: Map<Belt, List<String>> = remember {
                val order: List<Belt> = listOf(
                    Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK
                )

                order.associateWith { belt: Belt ->
                    HardSectionsCatalog.releases
                        .flatMap { sec ->
                            // ✅ itemsFor הוא member-extension -> חייבים run { }
                            HardSectionsCatalog.run { sec.itemsFor(belt) }
                        }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                }
            }

            val uiMap: Map<Belt, List<String>> = remember(groupedItems) {
                groupedItems.associate { (b: Belt, items: List<String>) ->
                    b to items.map { it.trim() }.filter { it.isNotBlank() }
                }
            }

            val same: Boolean = hardMap.all { (belt: Belt, hardItems: List<String>) ->
                val uiItems: List<String> = uiMap[belt].orEmpty()
                uiItems.toSet() == hardItems.toSet() // ✅ לא תלוי סדר
            }

            val diffSummary: String = if (!same) {
                hardMap.entries.joinToString(" | ") { (belt: Belt, hardItems: List<String>) ->
                    val uiItems: List<String> = uiMap[belt].orEmpty()
                    val missing = hardItems.toSet() - uiItems.toSet()
                    val extra = uiItems.toSet() - hardItems.toSet()
                    "${belt.name}: missing=${missing.size}, extra=${extra.size}"
                }
            } else ""

            val hardSizes = remember(hardMap) {
                hardMap.entries.joinToString { (b, it) -> "${b.name}=${it.size}" }
            }
            val uiSizes = remember(uiMap) {
                uiMap.entries.joinToString { (b, it) -> "${b.name}=${it.size}" }
            }

            androidx.compose.runtime.LaunchedEffect(same, diffSummary, hardSizes, uiSizes) {
                android.util.Log.e(
                    "KMI_DBG",
                    "RELEASES source-check: sameAsHard=$same " +
                            hardSizes +
                            " | ui=" +
                            uiSizes +
                            (if (diffSummary.isNotBlank()) " | diff=$diffSummary" else "")
                )
            }
        }

        // מציג לפי סדר חגורות
        val ordered = remember(groupedItems) {
            val order = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)
            val map = groupedItems.toMap()
            order.mapNotNull { b -> map[b]?.let { items -> b to items } }
        }
        ordered.forEach { (belt, items) ->
            BeltSection(
                screenTitle = title,
                belt = belt,
                items = items,
                itemTitle = itemTitle,
                onItemClick = { raw -> onItemClick?.invoke(belt, raw) },
                onExplainClick = { clickedTitle ->
                    explainFor.value = belt to clickedTitle
                }
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
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        color = Color(0xFFECEFF1),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = Color(0xFF607D8B),
                    modifier = Modifier.size(22.dp)
                )

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF263238),
                        textAlign = TextAlign.Right,
                        maxLines = 2
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

                Spacer(Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(belt.color)
                )
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
    screenTitle: String,
    belt: Belt,
    items: List<String>,
    itemTitle: (String) -> String,
    onItemClick: (String) -> Unit,
    onExplainClick: (String) -> Unit
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

                    val isReleasesScreen =
                        screenTitle.contains("שחרור") || screenTitle.contains("שחרורים")

                    val title =
                        if (isReleasesScreen) stripReleasesPrefix(rawTitle) else rawTitle

                    // ✅ לא מציגים הסבר ברשימה (רק בדיאלוג אחרי קליק)
                    val subtitle = ""

                    if (isReleasesScreen) {
                        val count = releasesCountFromHard(title)

                        ReleasesMenuCard(
                            title = title,
                            count = count,
                            icon = Icons.Outlined.StarBorder,
                            iconBg = belt.color.copy(alpha = 0.95f),
                            onClick = { onItemClick(raw) }
                        )
                    } else {
                        DefenseItemCard(
                            belt = belt,
                            title = title,
                            subtitle = subtitle,
                            onClick = { onExplainClick(title) }
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

private fun releasesCountFromHard(title: String): Int {
    val wanted = title.trim()

    val sec = HardSectionsCatalog.releases.firstOrNull { s ->
        s.title.trim() == wanted
    } ?: return 0

    val order = listOf(Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK)

    return order.sumOf { belt ->
        HardSectionsCatalog.run { sec.itemsFor(belt) }
            .count { it.isNotBlank() }
    }
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
