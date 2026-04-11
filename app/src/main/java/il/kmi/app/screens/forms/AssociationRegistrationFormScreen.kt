package il.kmi.app.screens.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AssociationRegistrationFormScreen(
    isEnglish: Boolean = false,
    onOpenPdf: () -> Unit = {},
    onDownloadPdf: () -> Unit = {}
) {
    val title = if (isEnglish) "Association Registration Form" else "טופס רישום לעמותה"
    val description = if (isEnglish) {
        "This screen opens the current PDF form. Later, you can keep the PDF option here and also add a digital version."
    } else {
        "מסך זה פותח את טופס ה-PDF הקיים. בהמשך אפשר להשאיר כאן גם את ה-PDF וגם להוסיף גרסה דיגיטלית."
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenPdf
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null
                        )
                        Text(
                            text = if (isEnglish) " Open PDF" else " פתח PDF"
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDownloadPdf
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null
                        )
                        Text(
                            text = if (isEnglish) " Download PDF" else " הורד PDF"
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Next upgrade" else "השדרוג הבא",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isEnglish) {
                            "A smart digital form can be added here later, while keeping the PDF option."
                        } else {
                            "בהמשך אפשר להוסיף כאן גם טופס דיגיטלי חכם, במקביל ל-PDF."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}