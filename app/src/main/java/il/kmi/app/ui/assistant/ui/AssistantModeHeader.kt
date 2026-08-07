package il.kmi.app.ui.assistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.scaledIconSize

@Composable
internal fun AssistantModeHeader(
    assistantMode: AssistantMode?,
    isEnglish: Boolean,
    premiumCardBrush: Brush,
    onBackToModePicker: () -> Unit
) {
    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    val textAlignPrimary =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val assistantHeroShape =
        RoundedCornerShape(
            topStart = 32.dp,
            topEnd = 32.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.62f),
                shape = assistantHeroShape
            ),
        shape = assistantHeroShape,
        tonalElevation = 0.dp,
        shadowElevation = 18.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = premiumCardBrush,
                    shape = assistantHeroShape
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 18.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        when (assistantMode) {
                            AssistantMode.EXERCISE ->
                                Icons.Filled.FitnessCenter

                            AssistantMode.TRAININGS ->
                                Icons.Filled.RecordVoiceOver

                            AssistantMode.KMI_MATERIAL ->
                                Icons.Filled.MenuBook

                            null ->
                                Icons.Filled.AutoAwesome
                        },
                    contentDescription =
                        when (assistantMode) {
                            AssistantMode.EXERCISE ->
                                tr(
                                    "מצב מידע על תרגיל",
                                    "Exercise information mode"
                                )

                            AssistantMode.TRAININGS ->
                                tr(
                                    "מצב מידע על אימונים",
                                    "Training information mode"
                                )

                            AssistantMode.KMI_MATERIAL ->
                                tr(
                                    "מצב חומר ק.מ.י",
                                    "KAMI material mode"
                                )

                            null ->
                                tr(
                                    "בחירת מצב עוזר",
                                    "Assistant mode selection"
                                )
                        },
                    tint = Color.White,
                    modifier = Modifier.size(
                        scaledIconSize(25.dp)
                    )
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    text =
                        when (assistantMode) {
                            null ->
                                tr(
                                    "בחר מצב כדי להתחיל",
                                    "Choose a mode to begin"
                                )

                            AssistantMode.EXERCISE ->
                                tr(
                                    "מצב: מידע / הסבר על תרגיל",
                                    "Mode: Exercise info / explanation"
                                )

                            AssistantMode.TRAININGS ->
                                tr(
                                    "מצב: מידע על אימונים",
                                    "Mode: Training information"
                                )

                            AssistantMode.KMI_MATERIAL ->
                                tr(
                                    "מצב: חומר ק.מ.י",
                                    "Mode: KAMI material"
                                )
                        },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    style = KmiTypography.cardTitle,
                    textAlign = textAlignPrimary,
                    color = Color.White
                )

                Surface(
                    onClick = onBackToModePicker,
                    modifier = Modifier.size(
                        scaledIconSize(48.dp)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.20f),
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.36f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription =
                                tr(
                                    "חזרה לבחירת מצב",
                                    "Back to mode selection"
                                ),
                            tint = Color.White,
                            modifier = Modifier.size(
                                scaledIconSize(25.dp)
                            )
                        )
                    }
                }
            }
        }
    }
}