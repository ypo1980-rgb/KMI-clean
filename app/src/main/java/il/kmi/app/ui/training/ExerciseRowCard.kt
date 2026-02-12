package il.kmi.app.ui.training

import androidx.compose.foundation.BorderStroke
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.ext.color
import il.kmi.shared.domain.Belt


@Composable
fun ExerciseRowCard(
    belt: Belt,
    canonicalId: String,              // ✅ זה מה שעובר ל-Explanations
    rawTitleForDisplay: String = canonicalId,
    accent: Color = MaterialTheme.colorScheme.primary,
    onOpenExercise: ((String) -> Unit)? = null,   // ✅ אם יש ניווט למסך תרגיל
    onInfo: (Belt, String) -> Unit                // ✅ פותח ExerciseInfoDialog
) {
    val display = ExerciseTitleFormatter.displayName(rawTitleForDisplay).ifBlank { rawTitleForDisplay }.trim()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (onOpenExercise != null) {
                    Modifier.clickable { onOpenExercise(canonicalId) }
                } else Modifier
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f))
    ) {
        // ✅ כדי שהאייקון יהיה "שמאל אמיתי" גם ב-RTL:
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onInfo(belt, canonicalId) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "מידע",
                        tint = accent.copy(alpha = 0.95f)
                    )
                }

                Spacer(Modifier.width(6.dp))

                // ✅ טקסט בעברית RTL בתוך ה-Row
                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                            androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    Text(
                        text = "• $display",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    )
                }

                // נקודת צבע קטנה (אחיד)
                Spacer(Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = belt.color.copy(alpha = 0.95f)
                ) {}
            }
        }
    }
}
