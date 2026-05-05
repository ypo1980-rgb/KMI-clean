package il.kmi.app.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
    val nextValue = when (mastered) {
        null -> true
        true -> false
        false -> null
    }

    val bgTarget = when (mastered) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> Color.White.copy(alpha = 0.92f)
    }

    val borderTarget = when (mastered) {
        true -> Color(0xFF1B5E20)
        false -> Color(0xFF8E1B1B)
        null -> Color.Black.copy(alpha = 0.22f)
    }

    val iconTint = when (mastered) {
        true, false -> Color.White
        null -> Color.Transparent
    }

    val bg by animateColorAsState(
        targetValue = bgTarget,
        label = "masterToggleBg"
    )

    val borderColor by animateColorAsState(
        targetValue = borderTarget,
        label = "masterToggleBorder"
    )

    val elevation by animateDpAsState(
        targetValue = if (mastered == null) 2.dp else 8.dp,
        label = "masterToggleElevation"
    )

    Surface(
        modifier = Modifier
            .size(42.dp)
            .clickable(role = Role.Button) { onSelect(nextValue) },
        shape = CircleShape,
        color = bg,
        shadowElevation = elevation,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (mastered) {
                true -> {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "סומן כנלמד",
                        modifier = Modifier.size(25.dp),
                        tint = iconTint
                    )
                }

                false -> {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "סומן כלא יודע",
                        modifier = Modifier.size(25.dp),
                        tint = iconTint
                    )
                }

                null -> {
                    // מצב ריק — עיגול נקי ללא אייקון.
                }
            }
        }
    }
}