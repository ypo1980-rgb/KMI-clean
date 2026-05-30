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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import il.kmi.app.ui.KmiTopBar

//==============================================================

data class PaymentMenuItemUi(
    val title: String,
    val subtitle: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun PaymentsScreen(
    isEnglish: Boolean = false,
    membershipStatus: String? = null,
    onHome: () -> Unit = {},
    onOpenMembershipPayment: () -> Unit
) {
    val title = if (isEnglish) "Payments report" else "דו״ח תשלומים"
    val membershipTitle = if (isEnglish) "Association Membership Fee" else "דמי חבר לעמותה"
    val screenTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right

    val items = listOf(
        PaymentMenuItemUi(
            title = membershipTitle,
            subtitle = if (isEnglish) {
                "Open registration and payment flow"
            } else {
                "פתיחת מסך רישום והמשך לתשלום"
            },
            enabled = true,
            onClick = onOpenMembershipPayment
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            KmiTopBar(
                title = title,
                onHome = onHome,
                showTopHome = false,
                lockSearch = true,
                showBottomActions = true,
                currentLang = if (isEnglish) "en" else "he"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PaymentHeaderCard(
                    isEnglish = isEnglish,
                    textAlign = screenTextAlign
                )
            }

            item {
                MembershipStatusCard(
                    title = if (isEnglish) "Membership Status" else "סטטוס חברות",
                    status = membershipStatus ?: if (isEnglish) "Not paid yet" else "טרם שולם",
                    isEnglish = isEnglish
                )
            }

            item {
                HorizontalDivider()
            }

            items(items) { item ->
                PaymentRow(
                    title = item.title,
                    subtitle = item.subtitle,
                    enabled = item.enabled,
                    isEnglish = isEnglish,
                    onClick = item.onClick
                )
            }
        }
    }
}

@Composable
private fun PaymentHeaderCard(
    isEnglish: Boolean,
    textAlign: TextAlign
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
        ) {
            Text(
                text = if (isEnglish) {
                    "Premium payments report for trainees and managers"
                } else {
                    "דשבורד תשלומים פרימיום למתאמנים ולמנהלים"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (isEnglish) {
                    "Track payment status, collection progress, paid trainees, and unpaid trainees."
                } else {
                    "מעקב אחר סטטוס תשלום, אחוז גבייה, מתאמנים ששילמו ומתאמנים שטרם שילמו."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )
        }
    }
}

@Composable
private fun MembershipStatusCard(
    title: String,
    status: String,
    isEnglish: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
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
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (isEnglish) {
                    "The payment flow will update this status after confirmation."
                } else {
                    "לאחר אישור תשלום, הסטטוס יתעדכן אוטומטית."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PaymentRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
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
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
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
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = if (isEnglish) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}