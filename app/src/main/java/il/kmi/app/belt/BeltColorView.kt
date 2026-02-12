package il.kmi.app.ui.belt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * רכיב כללי להצגת צבע חגורה.
 * @param color הצבע של החגורה
 * @param isCircle האם להציג כעיגול (true) או כריבוע (false)
 */
@Composable
fun BeltColorView(
    color: Color,
    isCircle: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = color,
                shape = if (isCircle) CircleShape else RoundedCornerShape(4.dp)
            )
    )
}
