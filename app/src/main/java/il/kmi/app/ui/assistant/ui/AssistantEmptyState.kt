package il.kmi.app.ui.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.scaledIconSize

@Composable
internal fun AssistantEmptyState(
    assistantMode: AssistantMode?,
    isEnglish: Boolean,
    emptyStateText: String
) {
    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 18.dp,
                vertical = 20.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.size(
                scaledIconSize(64.dp)
            ),
            shape = CircleShape,
            color =
                MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
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
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(
                        scaledIconSize(30.dp)
                    )
                )
            }
        }

        Text(
            text =
                when (assistantMode) {
                    AssistantMode.EXERCISE ->
                        tr(
                            "איזה תרגיל תרצה להכיר?",
                            "Which exercise would you like to explore?"
                        )

                    AssistantMode.TRAININGS ->
                        tr(
                            "מה תרצה לדעת על האימונים?",
                            "What would you like to know about training?"
                        )

                    AssistantMode.KMI_MATERIAL ->
                        tr(
                            "איזה חומר ק.מ.י נחפש?",
                            "Which KAMI material should we find?"
                        )

                    null ->
                        tr(
                            "איך אוכל לעזור?",
                            "How can I help?"
                        )
                },
            color = MaterialTheme.colorScheme.onSurface,
            style = KmiTypography.sectionTitle,
            textAlign = TextAlign.Center
        )

        Text(
            text = emptyStateText,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            style = KmiTypography.body.copy(
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color =
                MaterialTheme.colorScheme.primaryContainer,
            border =
                androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color =
                        MaterialTheme.colorScheme.outlineVariant
                )
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(
                        KmiIconSize.small
                    )
                )

                Spacer(Modifier.width(7.dp))

                Text(
                    text =
                        tr(
                            "אפשר לדבר באופן טבעי — יובל יבין את ההקשר",
                            "Speak naturally — Yuval will understand the context"
                        ),
                    color =
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    style = KmiTypography.secondary.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}