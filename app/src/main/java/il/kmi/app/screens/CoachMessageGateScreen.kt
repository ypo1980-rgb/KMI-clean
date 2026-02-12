package il.kmi.app.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import il.kmi.app.notifications.CoachGate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachMessageGateScreen(
    onApprove: () -> Unit
) {
    val ctx = LocalContext.current

    val sp = remember {
        ctx.getSharedPreferences(CoachGate.SP_NAME, Context.MODE_PRIVATE)
    }

    val text = remember { sp.getString(CoachGate.SP_TEXT, "")?.trim().orEmpty() }
    val from = remember { sp.getString(CoachGate.SP_FROM, "")?.trim().orEmpty() }
    val sentAt = remember { sp.getLong(CoachGate.SP_SENT_AT, 0L) }

    val metaLine = remember(from, sentAt) {
        val fromLabel = from.ifBlank { "המאמן" }
        if (sentAt > 0L) {
            val fmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("he", "IL"))
            "$fromLabel · ${fmt.format(java.util.Date(sentAt))}"
        } else fromLabel
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("הודעה חדשה", fontWeight = FontWeight.Bold) }
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "הודעת המאמן",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (text.isBlank()) "—" else text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    }
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("אישור והמשך", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
