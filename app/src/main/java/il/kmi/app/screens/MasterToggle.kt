package il.kmi.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val backgroundColor = when (mastered) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> Color.White.copy(alpha = 0.96f)
    }

    val borderColor = when (mastered) {
        true -> Color(0xFF1B5E20)
        false -> Color(0xFF8E1B1B)
        null -> Color.Black.copy(alpha = 0.22f)
    }

    val elevation = when (mastered) {
        null -> 2.dp
        else -> 8.dp
    }

    Surface(
        onClick = {
            onSelect(nextValue)
        },
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = elevation,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        )
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            when (mastered) {
                true -> {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "סומן כיודע",
                        modifier = Modifier.size(25.dp),
                        tint = Color.White
                    )
                }

                false -> {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "סומן כלא יודע",
                        modifier = Modifier.size(25.dp),
                        tint = Color.White
                    )
                }

                null -> Unit
            }
        }
    }
}