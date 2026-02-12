package il.kmi.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MasterToggle(
    mastered: Boolean?,
    onSelect: (Boolean?) -> Unit
) {
    val shape = RoundedCornerShape(50)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // =======================
        // ✅ ירוק
        // =======================

        val greenSelected = mastered == true

        val greenColor by animateColorAsState(
            targetValue = if (greenSelected)
                Color(0xFF2E7D32)
            else
                Color(0x332E7D32),
            label = "greenColor"
        )

        val greenElevation by animateDpAsState(
            targetValue = if (greenSelected) 8.dp else 2.dp,
            label = "greenElevation"
        )

        Surface(
            modifier = Modifier
                .size(38.dp)
                .clickable { onSelect(if (greenSelected) null else true) },
            shape = shape,
            color = greenColor,
            shadowElevation = greenElevation,
            border = BorderStroke(
                1.dp,
                if (greenSelected) Color.Black else Color.Black.copy(alpha = 0.25f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "סומן כנלמד",
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
            }
        }

        // =======================
        // ❌ אדום
        // =======================

        val redSelected = mastered == false

        val redColor by animateColorAsState(
            targetValue = if (redSelected)
                Color(0xFFC62828)
            else
                Color(0x33C62828),
            label = "redColor"
        )

        val redElevation by animateDpAsState(
            targetValue = if (redSelected) 8.dp else 2.dp,
            label = "redElevation"
        )

        Surface(
            modifier = Modifier
                .size(38.dp)
                .clickable { onSelect(if (redSelected) null else false) },
            shape = shape,
            color = redColor,
            shadowElevation = redElevation,
            border = BorderStroke(
                1.dp,
                if (redSelected) Color.Black else Color.Black.copy(alpha = 0.25f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "סומן כלא יודע",
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
            }
        }
    }
}
