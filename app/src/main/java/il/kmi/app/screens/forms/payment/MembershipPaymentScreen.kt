package il.kmi.app.screens.forms.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.LocalPhone
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val MISSING_BRANCH_HE = "הסניף שלי לא מופיע"
private const val MISSING_BRANCH_EN = "My branch is not listed"

data class MembershipPaymentPrefill(
    val traineeFirstName: String = "",
    val traineeLastName: String = "",
    val traineeIdNumber: String = "",
    val traineeBirthDate: String = "",
    val traineeEmail: String = "",
    val traineePhone: String = "",
    val traineeBranch: String = "",
    val traineeOtherBranch: String = "",
    val payerFirstName: String = "",
    val payerLastName: String = "",
    val payerEmail: String = "",
    val payerPhone: String = ""
)

data class MembershipPaymentFormData(
    val traineeFirstName: String,
    val traineeLastName: String,
    val traineeIdNumber: String,
    val traineeBirthDate: String,
    val traineeEmail: String,
    val traineePhone: String,
    val traineeBranch: String,
    val traineeOtherBranch: String,
    val payerSameAsTrainee: Boolean,
    val payerFirstName: String,
    val payerLastName: String,
    val payerEmail: String,
    val payerPhone: String,
    val policyAccepted: Boolean,
    val amount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipPaymentScreen(
    isEnglish: Boolean = false,
    prefill: MembershipPaymentPrefill = MembershipPaymentPrefill(),
    onClose: () -> Unit = {},
    onReadFullPolicy: () -> Unit = {},
    onContinueToPayment: (MembershipPaymentFormData) -> Unit = {}
) {
    val title = if (isEnglish) "Membership Payment" else "תשלום דמי חבר"
    val traineeTitle = if (isEnglish) "Trainee Details" else "פרטי חניך"
    val payerTitle = if (isEnglish) "Payer Details for Invoice" else "פרטי המשלם לשליחת חשבונית"
    val productTitle = if (isEnglish) "Payment Summary" else "סיכום תשלום"
    val policyTitle = if (isEnglish) "Cancellation & Refund Policy" else "מדיניות ביטולים והחזרים"
    val continueText = if (isEnglish) "Continue to Payment" else "המשך לתשלום"
    val readPolicyText = if (isEnglish) "Read Full Policy" else "קרא מדיניות מלאה"
    val payerSameToggleText = if (isEnglish) "Payer is the same as trainee" else "המשלם זהה לפרטי החניך"

    val branchOptions = remember {
        il.kmi.app.training.TrainingCatalog.allVisibleBranches() +
                listOf(if (isEnglish) MISSING_BRANCH_EN else MISSING_BRANCH_HE)
    }

    var traineeFirstName by rememberSaveable { mutableStateOf(prefill.traineeFirstName) }
    var traineeLastName by rememberSaveable { mutableStateOf(prefill.traineeLastName) }
    var traineeIdNumber by rememberSaveable { mutableStateOf(prefill.traineeIdNumber) }
    var traineeBirthDate by rememberSaveable { mutableStateOf(prefill.traineeBirthDate) }
    var traineeEmail by rememberSaveable { mutableStateOf(prefill.traineeEmail) }
    var traineePhone by rememberSaveable { mutableStateOf(prefill.traineePhone) }

    var traineeBranch by rememberSaveable {
        mutableStateOf(
            prefill.traineeBranch.takeIf { it.isNotBlank() } ?: branchOptions.first()
        )
    }
    var traineeOtherBranch by rememberSaveable { mutableStateOf(prefill.traineeOtherBranch) }

    var payerSameAsTrainee by rememberSaveable { mutableStateOf(true) }

    var payerFirstName by rememberSaveable {
        mutableStateOf(
            prefill.payerFirstName.ifBlank { prefill.traineeFirstName }
        )
    }
    var payerLastName by rememberSaveable {
        mutableStateOf(
            prefill.payerLastName.ifBlank { prefill.traineeLastName }
        )
    }
    var payerEmail by rememberSaveable {
        mutableStateOf(
            prefill.payerEmail.ifBlank { prefill.traineeEmail }
        )
    }
    var payerPhone by rememberSaveable {
        mutableStateOf(
            prefill.payerPhone.ifBlank { prefill.traineePhone }
        )
    }

    var policyAccepted by rememberSaveable { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    var showPolicySheet by rememberSaveable { mutableStateOf(false) }

    val missingBranchValue = if (isEnglish) MISSING_BRANCH_EN else MISSING_BRANCH_HE
    val shouldShowOtherBranch = traineeBranch == missingBranchValue

    LaunchedEffect(
        payerSameAsTrainee,
        traineeFirstName,
        traineeLastName,
        traineeEmail,
        traineePhone
    ) {
        if (payerSameAsTrainee) {
            payerFirstName = traineeFirstName
            payerLastName = traineeLastName
            payerEmail = traineeEmail
            payerPhone = traineePhone
        }
    }

    val isFormValid =
        traineeFirstName.isNotBlank() &&
                traineeLastName.isNotBlank() &&
                traineeIdNumber.isNotBlank() &&
                traineeBirthDate.isNotBlank() &&
                traineeEmail.isNotBlank() &&
                traineePhone.isNotBlank() &&
                traineeBranch.isNotBlank() &&
                (!shouldShowOtherBranch || traineeOtherBranch.isNotBlank()) &&
                payerFirstName.isNotBlank() &&
                payerLastName.isNotBlank() &&
                payerEmail.isNotBlank() &&
                payerPhone.isNotBlank() &&
                policyAccepted

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumPaymentHeader(
                    title = title,
                    subtitle = if (isEnglish) {
                        "Association membership fee • ₪150"
                    } else {
                        "דמי חבר לעמותה • 150 ₪"
                    },
                    onClose = onClose
                )

                ProductHeroCard(
                    isEnglish = isEnglish,
                    amountText = if (isEnglish) "₪150.00" else "150.00 ₪"
                )

                SectionCard(
                    title = traineeTitle,
                    icon = Icons.Default.AccountCircle
                ) {
                    FormTextField(
                        value = traineeFirstName,
                        onValueChange = { traineeFirstName = it },
                        label = if (isEnglish) "First Name" else "שם פרטי",
                        leadingIcon = Icons.Default.AccountCircle
                    )

                    FormTextField(
                        value = traineeLastName,
                        onValueChange = { traineeLastName = it },
                        label = if (isEnglish) "Last Name" else "שם משפחה",
                        leadingIcon = Icons.Default.AccountCircle
                    )

                    FormTextField(
                        value = traineeIdNumber,
                        onValueChange = { traineeIdNumber = it },
                        label = if (isEnglish) "ID Number" else "מספר ת.ז.",
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Default.Badge
                    )

                    FormTextField(
                        value = traineeBirthDate,
                        onValueChange = { traineeBirthDate = it },
                        label = if (isEnglish) "Birth Date" else "תאריך לידה",
                        placeholder = "DD/MM/YYYY",
                        leadingIcon = Icons.Default.Event
                    )

                    FormTextField(
                        value = traineeEmail,
                        onValueChange = { traineeEmail = it },
                        label = if (isEnglish) "Email" else "כתובת דוא\"ל",
                        keyboardType = KeyboardType.Email,
                        leadingIcon = Icons.Default.MarkEmailRead
                    )

                    FormTextField(
                        value = traineePhone,
                        onValueChange = { traineePhone = it },
                        label = if (isEnglish) "Mobile Phone" else "מספר טלפון נייד",
                        keyboardType = KeyboardType.Phone,
                        leadingIcon = Icons.Default.LocalPhone
                    )

                    ExposedDropdownMenuBox(
                        expanded = branchExpanded,
                        onExpandedChange = { branchExpanded = !branchExpanded }
                    ) {
                        OutlinedTextField(
                            value = traineeBranch,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            label = {
                                Text(
                                    text = if (isEnglish) "Branch Name" else "שם הסניף",
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.72f)
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = branchExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF24365E),
                                unfocusedContainerColor = Color(0xFF24365E),
                                disabledContainerColor = Color(0xFF24365E),

                                focusedBorderColor = Color(0xFF5E7CE2),
                                unfocusedBorderColor = Color(0xFF3A4A7A),
                                disabledBorderColor = Color(0xFF2A355A),

                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.White.copy(alpha = 0.55f),

                                focusedLabelColor = Color.White.copy(alpha = 0.82f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.62f),
                                disabledLabelColor = Color.White.copy(alpha = 0.42f),

                                focusedLeadingIconColor = Color.White.copy(alpha = 0.82f),
                                unfocusedLeadingIconColor = Color.White.copy(alpha = 0.62f),
                                disabledLeadingIconColor = Color.White.copy(alpha = 0.38f),

                                focusedTrailingIconColor = Color.White.copy(alpha = 0.82f),
                                unfocusedTrailingIconColor = Color.White.copy(alpha = 0.62f),
                                disabledTrailingIconColor = Color.White.copy(alpha = 0.38f),

                                cursorColor = Color.White
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = branchExpanded,
                            onDismissRequest = { branchExpanded = false }
                        ) {
                            branchOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        traineeBranch = option
                                        if (option != missingBranchValue) {
                                            traineeOtherBranch = ""
                                        }
                                        branchExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (shouldShowOtherBranch) {
                        FormTextField(
                            value = traineeOtherBranch,
                            onValueChange = { traineeOtherBranch = it },
                            label = if (isEnglish) {
                                "Other Branch Name"
                            } else {
                                "שם סניף נוסף אם חסר ברשימה"
                            },
                            leadingIcon = Icons.Default.Domain
                        )
                    }
                }

                SectionCard(
                    title = payerTitle,
                    icon = Icons.Default.Receipt
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF24365E)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                text = payerSameToggleText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            )

                            Switch(
                                checked = payerSameAsTrainee,
                                onCheckedChange = { payerSameAsTrainee = it }
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    )

                    FormTextField(
                        value = payerFirstName,
                        onValueChange = { if (!payerSameAsTrainee) payerFirstName = it },
                        label = if (isEnglish) "First Name" else "שם פרטי",
                        enabled = !payerSameAsTrainee,
                        leadingIcon = Icons.Default.Person
                    )

                    FormTextField(
                        value = payerLastName,
                        onValueChange = { if (!payerSameAsTrainee) payerLastName = it },
                        label = if (isEnglish) "Last Name" else "שם משפחה",
                        enabled = !payerSameAsTrainee,
                        leadingIcon = Icons.Default.Person
                    )

                    FormTextField(
                        value = payerEmail,
                        onValueChange = { if (!payerSameAsTrainee) payerEmail = it },
                        label = if (isEnglish) "Email Address" else "כתובת דוא\"ל",
                        keyboardType = KeyboardType.Email,
                        enabled = !payerSameAsTrainee,
                        leadingIcon = Icons.Default.Email
                    )

                    FormTextField(
                        value = payerPhone,
                        onValueChange = { if (!payerSameAsTrainee) payerPhone = it },
                        label = if (isEnglish) "Phone Number" else "מספר טלפון",
                        keyboardType = KeyboardType.Phone,
                        enabled = !payerSameAsTrainee,
                        leadingIcon = Icons.Default.PhoneIphone
                    )
                }

                SectionCard(
                    title = productTitle,
                    icon = Icons.Default.Wallet
                ) {
                    ProductPriceRow(
                        label = if (isEnglish) "Product" else "מוצר",
                        value = if (isEnglish) "Association Membership Fee" else "דמי חבר לעמותה"
                    )

                    ProductPriceRow(
                        label = if (isEnglish) "Price" else "מחיר",
                        value = if (isEnglish) "₪150.00" else "150.00 ₪",
                        emphasize = true
                    )
                }

                SectionCard(
                    title = policyTitle,
                    icon = Icons.Default.Description
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF91A0BF).copy(alpha = 0.26f)
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "Payment of membership fees is final after approval, except in cases such as duplicate payment or another good-faith mistake, subject to review by the association."
                            } else {
                                "תשלום דמי חבר הוא סופי לאחר אישור הפעולה, למעט מקרים של תשלום כפול בטעות או טעות אחרת בתום לב, בכפוף לבדיקת העמותה."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    TextButton(
                        onClick = { showPolicySheet = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(readPolicyText)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = policyAccepted,
                            onCheckedChange = { policyAccepted = it }
                        )

                        Text(
                            text = if (isEnglish) {
                                "I have read and agree to the cancellation and refund policy."
                            } else {
                                "קראתי ואני מאשר/ת את מדיניות הביטולים וההחזרים."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        onContinueToPayment(
                            MembershipPaymentFormData(
                                traineeFirstName = traineeFirstName.trim(),
                                traineeLastName = traineeLastName.trim(),
                                traineeIdNumber = traineeIdNumber.trim(),
                                traineeBirthDate = traineeBirthDate.trim(),
                                traineeEmail = traineeEmail.trim(),
                                traineePhone = traineePhone.trim(),
                                traineeBranch = traineeBranch.trim(),
                                traineeOtherBranch = traineeOtherBranch.trim(),
                                payerSameAsTrainee = payerSameAsTrainee,
                                payerFirstName = payerFirstName.trim(),
                                payerLastName = payerLastName.trim(),
                                payerEmail = payerEmail.trim(),
                                payerPhone = payerPhone.trim(),
                                policyAccepted = policyAccepted,
                                amount = 150.0
                            )
                        )
                    },
                    enabled = isFormValid,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = Color(0xFF3A5D8F),
                        disabledContentColor = Color.White.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null
                    )
                    Text(
                        text = "  $continueText",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumPaymentHeader(
    title: String,
    subtitle: String,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF314875)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.10f)
            ) {
                IconButton(
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductHeroCard(
    isEnglish: Boolean,
    amountText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF314875)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isEnglish) "Association Membership" else "חברות בעמותה",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = if (isEnglish) {
                        "Secure registration before payment"
                    } else {
                        "רישום מאובטח לפני מעבר לתשלום"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A3D66)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 10.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.12f)
            )

            content()
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        label = {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.78f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f)
            )
        },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.42f)
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF24365E),
            unfocusedContainerColor = Color(0xFF24365E),
            disabledContainerColor = Color(0xFF24365E),

            focusedBorderColor = Color(0xFF5E7CE2),
            unfocusedBorderColor = Color(0xFF3A4A7A),
            disabledBorderColor = Color(0xFF2A355A),

            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.55f),

            focusedLabelColor = Color.White.copy(alpha = 0.82f),
            unfocusedLabelColor = Color.White.copy(alpha = 0.62f),
            disabledLabelColor = Color.White.copy(alpha = 0.42f),

            focusedLeadingIconColor = Color.White.copy(alpha = 0.82f),
            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.62f),
            disabledLeadingIconColor = Color.White.copy(alpha = 0.38f),

            cursorColor = Color.White
        )
    )
}

@Composable
private fun ProductPriceRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.76f),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (emphasize) {
                Color(0xFF9B7BFF)
            } else {
                Color.White
            }
        )
    }
}