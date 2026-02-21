package il.kmi.app.navigation.defenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.color
import il.kmi.shared.domain.Belt

@Composable
fun DefensesListScreen(
    title: String,
    groupedItems: List<Pair<Belt, List<String>>>,
    itemTitle: (String) -> String,
    onBack: () -> Unit,
    onItemClick: ((Belt, String) -> Unit)? = null
) {
    val scroll = rememberScrollState()

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

        // מציג לפי סדר חגורות
        val ordered = remember(groupedItems) {
            val order = listOf(
                Belt.YELLOW, Belt.ORANGE, Belt.GREEN, Belt.BLUE, Belt.BROWN, Belt.BLACK
            )
            val map = groupedItems.toMap()
            order.mapNotNull { b -> map[b]?.let { items -> b to items } }
        }

        ordered.forEach { (belt, items) ->
            BeltSection(
                belt = belt,
                items = items,
                itemTitle = itemTitle,
                onItemClick = { raw ->
                    onItemClick?.invoke(belt, raw)
                }
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BeltSection(
    belt: Belt,
    items: List<String>,
    itemTitle: (String) -> String,
    onItemClick: (String) -> Unit
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

            // Header כמו בתמונה: "חגורה X" + "N תרגילים"
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
                    DefenseItemCard(
                        belt = belt,
                        title = itemTitle(raw),
                        subtitle = "כללי",
                        onClick = { onItemClick(raw) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DefenseItemCard(
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
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF607D8B),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Right
                )
            }

            Spacer(Modifier.width(10.dp))

            // פס צבע ימני כמו בתמונה
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