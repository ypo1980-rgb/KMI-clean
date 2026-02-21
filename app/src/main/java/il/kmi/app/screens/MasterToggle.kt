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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun MasterToggle(
    mastered: Boolean?,
    onSelect: (Boolean?) -> Unit
) {
    val shape = RoundedCornerShape(50)

    @Composable
    fun ToggleChip(
        selected: Boolean,
        selectedColor: Color,
        unselectedColor: Color,
        iconTint: Color,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        contentDescription: String,
        onClickValue: Boolean?
    ) {
        val bg by animateColorAsState(
            targetValue = if (selected) selectedColor else unselectedColor,
            label = "chipBg"
        )

        val elevation by animateDpAsState(
            targetValue = if (selected) 8.dp else 2.dp,
            label = "chipElevation"
        )

        Surface(
            modifier = Modifier
                .size(38.dp)
                .clickable(role = Role.Button) { onSelect(onClickValue) },
            shape = shape,
            color = bg,
            shadowElevation = elevation,
            border = BorderStroke(
                1.dp,
                if (selected) Color.Black else Color.Black.copy(alpha = 0.25f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(22.dp),
                    tint = iconTint
                )
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ ירוק
        val greenSelected = mastered == true
        ToggleChip(
            selected = greenSelected,
            selectedColor = Color(0xFF2E7D32),
            unselectedColor = Color(0x332E7D32),
            iconTint = Color.White,
            icon = Icons.Filled.Check,
            contentDescription = "סומן כנלמד",
            onClickValue = if (greenSelected) null else true
        )

        // ❌ אדום
        val redSelected = mastered == false
        ToggleChip(
            selected = redSelected,
            selectedColor = Color(0xFFC62828),
            unselectedColor = Color(0x33C62828),
            iconTint = Color.White,
            icon = Icons.Filled.Close,
            contentDescription = "סומן כלא יודע",
            onClickValue = if (redSelected) null else false
        )
    }
}
