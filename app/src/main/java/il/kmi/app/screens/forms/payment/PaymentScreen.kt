package il.kmi.app.screens.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun PaymentScreen(
    isEnglish: Boolean,
    amountToPay: String = "150 ₪",
    onClose: () -> Unit,
    onPayClicked: (
        cardHolderName: String,
        idNumber: String,
        phone: String,
        email: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        installments: Int
    ) -> Unit
) {
    var cardHolderName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    val installmentOptions = listOf(1, 2, 3, 4, 6, 12)
    var installmentsExpanded by remember { mutableStateOf(false) }
    var installments by remember { mutableStateOf(1) }

    val title = if (isEnglish) "Payment Details" else "פרטי תשלום"
    val subtitle = if (isEnglish) "Complete your membership payment securely" else "השלם את תשלום דמי החבר בצורה מאובטחת"
    val amountTitle = if (isEnglish) "Amount to pay" else "סכום לתשלום"
    val payNowText = if (isEnglish) "Pay Now" else "ביצוע תשלום"
    val secureText = if (isEnglish) "Secure payment" else "תשלום מאובטח"
    val closeDesc = if (isEnglish) "Close" else "סגור"

    val fieldCardHolder = if (isEnglish) "Card holder name" else "שם בעל הכרטיס"
    val fieldId = if (isEnglish) "ID number" else "תעודת זהות"
    val fieldPhone = if (isEnglish) "Phone number" else "טלפון"
    val fieldEmail = if (isEnglish) "Email" else "אימייל"
    val fieldCardNumber = if (isEnglish) "Card number" else "מספר כרטיס"
    val fieldExpiry = if (isEnglish) "Expiry (MM/YY)" else "תוקף (MM/YY)"
    val fieldCvv = if (isEnglish) "CVV" else "CVV"
    val fieldInstallments = if (isEnglish) "Installments" else "מספר תשלומים"

    val isFormValid =
        cardHolderName.isNotBlank() &&
                idNumber.length >= 8 &&
                phone.length >= 9 &&
                email.contains("@") &&
                cardNumber.filter { it.isDigit() }.length >= 12 &&
                expiry.length >= 4 &&
                cvv.length in 3..4

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF06131F),
                        Color(0xFF0B2233),
                        Color(0xFF10344A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.10f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = closeDesc,
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 13.sp,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.10f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF19C37D).copy(alpha = 0.16f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF7CFFB2)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                        ) {
                            Text(
                                text = secureText,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = amountTitle,
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = amountToPay,
                            color = Color(0xFFFFD66B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                    Spacer(modifier = Modifier.height(16.dp))

                    PremiumTextField(
                        value = cardHolderName,
                        onValueChange = { cardHolderName = it },
                        label = fieldCardHolder,
                        leadingIcon = { Icon(Icons.Outlined.Person, null) },
                        isEnglish = isEnglish
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PremiumTextField(
                        value = idNumber,
                        onValueChange = { idNumber = it.filter { ch -> ch.isDigit() }.take(9) },
                        label = fieldId,
                        leadingIcon = { Icon(Icons.Outlined.Badge, null) },
                        keyboardType = KeyboardType.Number,
                        isEnglish = isEnglish
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PremiumTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) },
                        label = fieldPhone,
                        leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                        keyboardType = KeyboardType.Phone,
                        isEnglish = isEnglish
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PremiumTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = fieldEmail,
                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                        keyboardType = KeyboardType.Email,
                        isEnglish = isEnglish
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PremiumTextField(
                        value = cardNumber,
                        onValueChange = {
                            cardNumber = formatCardNumber(it)
                        },
                        label = fieldCardNumber,
                        leadingIcon = { Icon(Icons.Outlined.CreditCard, null) },
                        keyboardType = KeyboardType.Number,
                        isEnglish = isEnglish
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = 2
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.48f)) {
                            PremiumTextField(
                                value = expiry,
                                onValueChange = { expiry = formatExpiry(it) },
                                label = fieldExpiry,
                                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) },
                                keyboardType = KeyboardType.Number,
                                isEnglish = isEnglish
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            PremiumTextField(
                                value = cvv,
                                onValueChange = { cvv = it.filter { ch -> ch.isDigit() }.take(4) },
                                label = fieldCvv,
                                leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                                keyboardType = KeyboardType.Number,
                                isEnglish = isEnglish
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = installmentsExpanded,
                        onExpandedChange = { installmentsExpanded = !installmentsExpanded }
                    ) {
                        OutlinedTextField(
                            value = installments.toString(),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text(fieldInstallments) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = installmentsExpanded)
                            },
                            colors = premiumFieldColors(),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = installmentsExpanded,
                            onDismissRequest = { installmentsExpanded = false }
                        ) {
                            installmentOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.toString()) },
                                    onClick = {
                                        installments = option
                                        installmentsExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onPayClicked(
                                cardHolderName,
                                idNumber,
                                phone,
                                email,
                                cardNumber,
                                expiry,
                                cvv,
                                installments
                            )
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF19C37D),
                            disabledContainerColor = Color(0xFF19C37D).copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = payNowText,
                            color = Color(0xFF06251A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isEnglish)
                            "Your details are transmitted securely."
                        else
                            "הפרטים שלך מועברים בצורה מאובטחת.",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isEnglish: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        visualTransformation = VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = premiumFieldColors(),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = Color.White,
            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
        )
    )
}

@Composable
private fun premiumFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color.White.copy(alpha = 0.08f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
    disabledContainerColor = Color.White.copy(alpha = 0.04f),
    focusedBorderColor = Color(0xFF7CFFB2),
    unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
    focusedLabelColor = Color(0xFFB9FFD7),
    unfocusedLabelColor = Color.White.copy(alpha = 0.72f),
    cursorColor = Color(0xFF7CFFB2),
    focusedLeadingIconColor = Color(0xFFB9FFD7),
    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.72f)
)

private fun formatCardNumber(input: String): String {
    val digits = input.filter { it.isDigit() }.take(16)
    return digits.chunked(4).joinToString(" ")
}

private fun formatExpiry(input: String): String {
    val digits = input.filter { it.isDigit() }.take(4)
    return when {
        digits.length <= 2 -> digits
        else -> digits.substring(0, 2) + "/" + digits.substring(2)
    }
}