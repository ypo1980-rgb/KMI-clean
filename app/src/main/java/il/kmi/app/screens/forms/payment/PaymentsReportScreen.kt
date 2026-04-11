package il.kmi.app.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsReportScreen(
    isEnglish: Boolean = false,
    onClose: () -> Unit = {},
    initialItems: List<PaymentReportItem> = demoPaymentsReportItems(),
    onSaveManualPayment: (traineeId: String, amount: Double, method: PaymentMethod, notes: String) -> Unit = { _, _, _, _ -> }
) {
    var items by remember { mutableStateOf(initialItems) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var manualDialogItem by remember { mutableStateOf<PaymentReportItem?>(null) }

    val title = if (isEnglish) "Payments Report" else "דו\"ח תשלומים"
    val paidText = if (isEnglish) "Paid" else "שילמו"
    val unpaidText = if (isEnglish) "Unpaid" else "לא שילמו"
    val partialText = if (isEnglish) "Partial" else "חלקי"

    val branchOptions = remember {
        listOf(if (isEnglish) "All Branches" else "כל הסניפים") +
                il.kmi.app.training.TrainingCatalog.allVisibleBranches()
    }

    var selectedBranch by rememberSaveable {
        mutableStateOf(branchOptions.first())
    }

    val filteredItems = items.filter { item ->
        val matchesQuery =
            query.isBlank() ||
                    item.fullName.contains(query, ignoreCase = true) ||
                    item.phone.contains(query, ignoreCase = true) ||
                    item.branchName.contains(query, ignoreCase = true)

        val matchesFilter = when (filter) {
            "PAID" -> item.status == PaymentStatus.PAID
            "UNPAID" -> item.status == PaymentStatus.UNPAID
            "PARTIAL" -> item.status == PaymentStatus.PARTIAL
            else -> true
        }

        val matchesBranch =
            selectedBranch == (if (isEnglish) "All Branches" else "כל הסניפים") ||
                    item.branchName == selectedBranch

        matchesQuery && matchesFilter && matchesBranch
    }

    val totalRequired = items.sumOf { it.requiredAmount }
    val totalPaid = items.sumOf { it.paidAmount }
    val paidCount = items.count { it.status == PaymentStatus.PAID }
    val unpaidCount = items.count { it.status == PaymentStatus.UNPAID }
    val partialCount = items.count { it.status == PaymentStatus.PARTIAL }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0E1630),
                            Color(0xFF1F2A52),
                            Color(0xFF2575BC)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF314875)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (isEnglish) "Track who paid and update payments manually"
                                    else "מעקב אחרי מי ששילם ועדכון תשלומים ידניים",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            ) {
                                IconButton(onClick = onClose) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = paidText,
                        value = paidCount.toString(),
                        icon = Icons.Default.Paid
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = unpaidText,
                        value = unpaidCount.toString(),
                        icon = Icons.Default.PersonOff
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = partialText,
                        value = partialCount.toString(),
                        icon = Icons.Default.WarningAmber
                    )
                }

                Spacer(Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3D66))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(if (isEnglish) "Search by name / phone / branch" else "חיפוש לפי שם / טלפון / סניף") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            colors = reportFieldColors()
                        )

                        FilterRow(
                            isEnglish = isEnglish,
                            selected = filter,
                            onSelect = { filter = it }
                        )

                        Text(
                            text = if (isEnglish)
                                "Collected: ₪${"%.0f".format(totalPaid)} / ₪${"%.0f".format(totalRequired)}"
                            else
                                "נגבה: ₪${"%.0f".format(totalPaid)} / ₪${"%.0f".format(totalRequired)}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.traineeId }) { item ->
                        PaymentReportRow(
                            item = item,
                            isEnglish = isEnglish,
                            onManualUpdate = { manualDialogItem = item }
                        )
                    }
                }
            }

            manualDialogItem?.let { selected ->
                ManualPaymentDialog(
                    isEnglish = isEnglish,
                    item = selected,
                    onDismiss = { manualDialogItem = null },
                    onSave = { amount, method, notes ->
                        val newPaidAmount = (selected.paidAmount + amount)
                        val newStatus = when {
                            newPaidAmount <= 0.0 -> PaymentStatus.UNPAID
                            newPaidAmount < selected.requiredAmount -> PaymentStatus.PARTIAL
                            else -> PaymentStatus.PAID
                        }

                        items = items.map { current ->
                            if (current.traineeId == selected.traineeId) {
                                current.copy(
                                    paidAmount = newPaidAmount,
                                    status = newStatus,
                                    paymentMethod = method,
                                    paymentDate = "10/04/2026",
                                    notes = notes
                                )
                            } else current
                        }

                        onSaveManualPayment(selected.traineeId, amount, method, notes)
                        manualDialogItem = null
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF314875))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = Color.White)
            Text(text = title, color = Color.White.copy(alpha = 0.82f))
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun FilterRow(
    isEnglish: Boolean,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipSimple(
            text = if (isEnglish) "All" else "הכל",
            selected = selected == "ALL",
            onClick = { onSelect("ALL") }
        )
        FilterChipSimple(
            text = if (isEnglish) "Paid" else "שילמו",
            selected = selected == "PAID",
            onClick = { onSelect("PAID") }
        )
        FilterChipSimple(
            text = if (isEnglish) "Unpaid" else "לא שילמו",
            selected = selected == "UNPAID",
            onClick = { onSelect("UNPAID") }
        )
        FilterChipSimple(
            text = if (isEnglish) "Partial" else "חלקי",
            selected = selected == "PARTIAL",
            onClick = { onSelect("PARTIAL") }
        )
    }
}

@Composable
private fun FilterChipSimple(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PaymentReportRow(
    item: PaymentReportItem,
    isEnglish: Boolean,
    onManualUpdate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3D66))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = item.fullName,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = if (isEnglish)
                    "${item.branchName} • ${item.phone}"
                else
                    "${item.branchName} • ${item.phone}",
                color = Color.White.copy(alpha = 0.78f)
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            Text(
                text = if (isEnglish)
                    "Paid ₪${"%.0f".format(item.paidAmount)} מתוך ₪${"%.0f".format(item.requiredAmount)}"
                else
                    "שולם ₪${"%.0f".format(item.paidAmount)} מתוך ₪${"%.0f".format(item.requiredAmount)}",
                color = Color.White
            )

            Text(
                text = if (isEnglish)
                    "Status: ${statusLabel(item.status)}"
                else
                    "סטטוס: ${statusLabel(item.status)}",
                color = statusColor(item.status)
            )

            Button(
                onClick = onManualUpdate,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AddCard, null)
                Text(
                    text = if (isEnglish) "  Add Manual Payment" else "  הוסף תשלום ידני"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualPaymentDialog(
    isEnglish: Boolean,
    item: PaymentReportItem,
    onDismiss: () -> Unit,
    onSave: (amount: Double, method: PaymentMethod, notes: String) -> Unit
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf(PaymentMethod.MANUAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEnglish) "Manual Payment Update" else "עדכון תשלום ידני",
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = item.fullName,
                    color = Color.White.copy(alpha = 0.9f)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isEnglish) "Amount" else "סכום") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = reportFieldColors()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = method.name,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text(if (isEnglish) "Payment Method" else "אמצעי תשלום") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = reportFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        PaymentMethod.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    method = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text(if (isEnglish) "Notes" else "הערות") },
                    colors = reportFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) onSave(amount, method, notes.trim())
                }
            ) {
                Text(if (isEnglish) "Save" else "שמור", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Cancel" else "ביטול", color = Color.White)
            }
        },
        containerColor = Color(0xFF0E1630)
    )
}

@Composable
private fun reportFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF24365E),
    unfocusedContainerColor = Color(0xFF24365E),
    disabledContainerColor = Color(0xFF24365E),
    focusedBorderColor = Color(0xFF5E7CE2),
    unfocusedBorderColor = Color(0xFF3A4A7A),
    disabledBorderColor = Color(0xFF2A355A),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color.White.copy(alpha = 0.82f),
    unfocusedLabelColor = Color.White.copy(alpha = 0.62f),
    cursorColor = Color.White
)

private fun statusLabel(status: PaymentStatus): String = when (status) {
    PaymentStatus.PAID -> "שולם"
    PaymentStatus.UNPAID -> "לא שולם"
    PaymentStatus.PARTIAL -> "שולם חלקית"
}

private fun statusColor(status: PaymentStatus): Color = when (status) {
    PaymentStatus.PAID -> Color(0xFF66D17A)
    PaymentStatus.UNPAID -> Color(0xFFFF7A7A)
    PaymentStatus.PARTIAL -> Color(0xFFFFC857)
}

private fun demoPaymentsReportItems(): List<PaymentReportItem> = listOf(
    PaymentReportItem(
        traineeId = "1",
        fullName = "יובל פולק",
        branchName = "מרכז קהילתי אופק נתניה",
        phone = "050-1234567",
        requiredAmount = 150.0,
        paidAmount = 150.0,
        status = PaymentStatus.PAID,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        paymentDate = "09/04/2026"
    ),
    PaymentReportItem(
        traineeId = "2",
        fullName = "אופק פולק",
        branchName = "מרכז קהילתי סוקולוב נתניה",
        phone = "050-7654321",
        requiredAmount = 150.0,
        paidAmount = 0.0,
        status = PaymentStatus.UNPAID
    ),
    PaymentReportItem(
        traineeId = "3",
        fullName = "מתאמן לדוגמה",
        branchName = "מרכז קהילתי אופק נתניה",
        phone = "052-1112233",
        requiredAmount = 150.0,
        paidAmount = 80.0,
        status = PaymentStatus.PARTIAL,
        paymentMethod = PaymentMethod.CASH,
        paymentDate = "08/04/2026"
    )
)