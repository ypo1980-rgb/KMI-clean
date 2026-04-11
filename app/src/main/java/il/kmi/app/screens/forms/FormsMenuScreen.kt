package il.kmi.app.screens.forms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class FormsMenuItemUi(
    val title: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun FormsMenuScreen(
    isEnglish: Boolean = false,
    onOpenAssociationRegistration: () -> Unit,
    onOpenHealthDeclaration: () -> Unit = {},
    onOpenParentApproval: () -> Unit = {},
    onOpenWaiver: () -> Unit = {},
    onOpenMembershipRenewal: () -> Unit = {},
    onOpenPayments: () -> Unit
) {
    val title = if (isEnglish) "Forms & Payments" else "טפסים ותשלומים"
    val formsTitle = if (isEnglish) "Forms" else "טפסים"
    val paymentsTitle = if (isEnglish) "Payments" else "תשלומים"

    val formItems = listOf(
        FormsMenuItemUi(
            title = if (isEnglish) "Association Registration Form" else "טופס רישום לעמותה",
            icon = Icons.Default.PersonAddAlt1,
            enabled = true,
            onClick = onOpenAssociationRegistration
        ),
        FormsMenuItemUi(
            title = if (isEnglish) "Health Declaration" else "הצהרת בריאות",
            icon = Icons.Default.HealthAndSafety,
            enabled = false,
            onClick = onOpenHealthDeclaration
        ),
        FormsMenuItemUi(
            title = if (isEnglish) "Parent Approval" else "אישור הורים",
            icon = Icons.Default.FactCheck,
            enabled = false,
            onClick = onOpenParentApproval
        ),
        FormsMenuItemUi(
            title = if (isEnglish) "Waiver" else "כתב ויתור",
            icon = Icons.Default.Gavel,
            enabled = false,
            onClick = onOpenWaiver
        ),
        FormsMenuItemUi(
            title = if (isEnglish) "Membership Renewal Form" else "טופס חידוש חברות",
            icon = Icons.Default.Refresh,
            enabled = false,
            onClick = onOpenMembershipRenewal
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(title = title)
            }

            item {
                PrimaryEntryCard(
                    title = formsTitle,
                    subtitle = if (isEnglish) "Open available forms" else "פתיחת רשימת הטפסים הזמינים",
                    icon = Icons.Default.Article,
                    onClick = {}
                )
            }

            items(formItems) { item ->
                FormMenuRow(
                    title = item.title,
                    icon = item.icon,
                    enabled = item.enabled,
                    isEnglish = isEnglish,
                    onClick = item.onClick
                )
            }

            item {
                PrimaryEntryCard(
                    title = paymentsTitle,
                    subtitle = if (isEnglish) "Membership fee and payment flow" else "דמי חבר ותהליך תשלום",
                    icon = Icons.Default.Payments,
                    onClick = onOpenPayments
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
        HorizontalDivider()
    }
}

@Composable
private fun PrimaryEntryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FormMenuRow(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.55f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )

                Text(
                    text = when {
                        enabled && isEnglish -> "Available now"
                        enabled && !isEnglish -> "זמין כעת"
                        !enabled && isEnglish -> "Coming soon"
                        else -> "ייפתח בהמשך"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }

            Icon(
                imageVector = if (isEnglish) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
    }
}