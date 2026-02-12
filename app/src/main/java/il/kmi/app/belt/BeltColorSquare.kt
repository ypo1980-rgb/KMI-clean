package il.kmi.app.belt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BeltColorSquare(color: Color, isCircle: Boolean = false) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                color = color,
                shape = if (isCircle) CircleShape else RoundedCornerShape(4.dp)
            )
    )
}
