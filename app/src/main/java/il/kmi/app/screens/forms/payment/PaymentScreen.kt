package il.kmi.app.screens.forms.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.KmiTypography

//==================================================================================

private enum class CheckoutPaymentMethod {
    CREDIT_CARD,
    BIT
}

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
    ) -> Unit,
    onBitPayClicked: (
        cardHolderName: String,
        idNumber: String,
        phone: String,
        email: String
    ) -> Unit = { _, _, _, _ -> }
) {
    var cardHolderName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    val installmentOptions =
        remember {
            listOf(1, 2, 3, 4, 6, 12)
        }

    var selectedPaymentMethod by remember {
        mutableStateOf(CheckoutPaymentMethod.CREDIT_CARD)
    }

    val isCreditCardSelected =
        selectedPaymentMethod == CheckoutPaymentMethod.CREDIT_CARD

    val isBitSelected =
        selectedPaymentMethod == CheckoutPaymentMethod.BIT

    val paymentMethodTitle =
        if (isEnglish) {
            "Choose payment method"
        } else {
            "בחר אמצעי תשלום"
        }

    var installments by remember {
        mutableIntStateOf(1)
    }

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
    val fieldExpiry = if (isEnglish) "MM/YY" else "תוקף"
    val fieldCvv = "CVV"
    val fieldInstallments = if (isEnglish) "Installments" else "מספר תשלומים"

    val screenTextAlign =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    val screenHorizontalAlignment =
        if (isEnglish) Alignment.Start else Alignment.End

    val headerLayoutDirection = LayoutDirection.Ltr

    val cleanCardHolderName = cardHolderName.trim()
    val cleanIdNumber = idNumber.trim()
    val cleanPhone = phone.trim()
    val cleanEmail = email.trim()

    val personalDetailsValid =
        cleanCardHolderName.isNotBlank() &&
                cleanIdNumber.length >= 8 &&
                cleanPhone.length >= 9 &&
                cleanEmail.contains("@")

    val cardDetailsValid =
        cardNumber.filter { it.isDigit() }.length >= 12 &&
                expiry.length >= 4 &&
                cvv.length in 3..4

    val isFormValid =
        if (isBitSelected) {
            personalDetailsValid
        } else {
            personalDetailsValid && cardDetailsValid
        }

    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = colorScheme.background.luminance() < 0.5f

    val screenGradient =
        if (isDarkMode) {
            listOf(
                Color(0xFF06131F),
                Color(0xFF0B2233),
                Color(0xFF10344A)
            )
        } else {
            listOf(
                Color(0xFFF7FBFF),
                Color(0xFFEAF6FF),
                Color(0xFFDDF1FF)
            )
        }

    val primaryTextColor =
        if (isDarkMode) {
            Color.White
        } else {
            colorScheme.onBackground
        }

    val secondaryTextColor =
        if (isDarkMode) {
            Color.White.copy(alpha = 0.74f)
        } else {
            colorScheme.onSurfaceVariant
        }

    val cardContainerColor =
        if (isDarkMode) {
            Color.White.copy(alpha = 0.10f)
        } else {
            colorScheme.surface.copy(alpha = 0.94f)
        }

    val cardBorderColor =
        if (isDarkMode) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color(0xFF8AC9E8).copy(alpha = 0.58f)
        }

    val dividerColor =
        if (isDarkMode) {
            Color.White.copy(alpha = 0.10f)
        } else {
            colorScheme.outlineVariant.copy(alpha = 0.72f)
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = screenGradient
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides headerLayoutDirection
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color =
                            if (isDarkMode) {
                                Color.White.copy(alpha = 0.10f)
                            } else {
                                colorScheme.primaryContainer.copy(alpha = 0.72f)
                            },
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = closeDesc,
                                tint = primaryTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = screenHorizontalAlignment
                    ) {
                        Text(
                            text = title,
                            color = primaryTextColor,
                            style =
                                KmiTypography.screenTitle.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            textAlign = screenTextAlign,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = subtitle,
                            color = secondaryTextColor,
                            style = KmiTypography.secondary,
                            textAlign = screenTextAlign,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardContainerColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = cardBorderColor,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                    tint =
                                        if (isDarkMode) {
                                            Color(0xFF7CFFB2)
                                        } else {
                                            Color(0xFF078B59)
                                        }
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
                                color = primaryTextColor,
                                style =
                                    KmiTypography.cardTitle.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                maxLines = 2
                            )

                            Text(
                                text = amountTitle,
                                color = secondaryTextColor,
                                style = KmiTypography.caption,
                                maxLines = 2
                            )
                        }

                        Text(
                            text = amountToPay,
                            color =
                                if (isDarkMode) {
                                    Color(0xFFFFD66B)
                                } else {
                                    Color(0xFF8A5A00)
                                },
                            style =
                                KmiTypography.metric.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = dividerColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = paymentMethodTitle,
                        color = primaryTextColor,
                        style =
                            KmiTypography.cardTitle.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign =
                            if (isEnglish) {
                                TextAlign.Left
                            } else {
                                TextAlign.Right
                            },
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PaymentMethodChoiceCard(
                            modifier = Modifier.weight(1f),
                            title = if (isEnglish) "Credit card" else "אשראי",
                            icon = { Icon(Icons.Outlined.CreditCard, null) },
                            selected = isCreditCardSelected,
                            onClick = {
                                selectedPaymentMethod = CheckoutPaymentMethod.CREDIT_CARD
                            }
                        )

                        PaymentMethodChoiceCard(
                            modifier = Modifier.weight(1f),
                            title = if (isEnglish) "bit" else "ביט",
                            icon = { Icon(Icons.Outlined.Phone, null) },
                            selected = isBitSelected,
                            onClick = {
                                selectedPaymentMethod = CheckoutPaymentMethod.BIT
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = cardHolderName,
                        onValueChange = { cardHolderName = it },
                        label = fieldCardHolder,
                        leadingIcon = { Icon(Icons.Outlined.Person, null) },
                        isEnglish = isEnglish
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    PremiumTextField(
                        value = idNumber,
                        onValueChange = { idNumber = it.filter { ch -> ch.isDigit() }.take(9) },
                        label = fieldId,
                        leadingIcon = { Icon(Icons.Outlined.Badge, null) },
                        keyboardType = KeyboardType.Number,
                        isEnglish = isEnglish
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    PremiumTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) },
                        label = fieldPhone,
                        leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                        keyboardType = KeyboardType.Phone,
                        isEnglish = isEnglish
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PremiumTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = fieldEmail,
                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                        keyboardType = KeyboardType.Email,
                        isEnglish = isEnglish
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isCreditCardSelected) {
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                PremiumTextField(
                                    value = expiry,
                                    onValueChange = { expiry = formatExpiry(it) },
                                    label = fieldExpiry,
                                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) },
                                    keyboardType = KeyboardType.Number,
                                    isEnglish = isEnglish
                                )
                            }

                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                PremiumTextField(
                                    value = cvv,
                                    onValueChange = {
                                        cvv = it.filter { ch -> ch.isDigit() }.take(4)
                                    },
                                    label = fieldCvv,
                                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                                    keyboardType = KeyboardType.Number,
                                    isEnglish = isEnglish
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        KmiPremiumDropdown(
                            title = fieldInstallments,
                            options =
                                installmentOptions.map { option ->
                                    option.toString()
                                },
                            selectedValue = installments.toString(),
                            isEnglish = isEnglish,
                            placeholder =
                                if (isEnglish) {
                                    "Choose installments"
                                } else {
                                    "בחר מספר תשלומים"
                                },
                            enabled = installmentOptions.isNotEmpty(),
                            onSelected = { selectedInstallments ->
                                selectedInstallments
                                    .toIntOrNull()
                                    ?.takeIf { selectedValue ->
                                        selectedValue in installmentOptions
                                    }
                                    ?.let { selectedValue ->
                                        installments = selectedValue
                                    }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            if (isBitSelected) {
                                onBitPayClicked(
                                    cleanCardHolderName,
                                    cleanIdNumber,
                                    cleanPhone,
                                    cleanEmail
                                )
                            } else {
                                onPayClicked(
                                    cleanCardHolderName,
                                    cleanIdNumber,
                                    cleanPhone,
                                    cleanEmail,
                                    cardNumber.trim(),
                                    expiry.trim(),
                                    cvv.trim(),
                                    installments
                                )
                            }
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF19C37D),
                            disabledContainerColor = Color(0xFF19C37D).copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Text(
                            text =
                                if (isBitSelected) {
                                    if (isEnglish) {
                                        "Continue to bit"
                                    } else {
                                        "המשך לתשלום בביט"
                                    }
                                } else {
                                    payNowText
                                },
                            color = Color(0xFF06251A),
                            style =
                                KmiTypography.action.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text =
                            if (isEnglish) {
                                "Your details are transmitted securely."
                            } else {
                                "הפרטים שלך מועברים בצורה מאובטחת."
                            },
                        color = secondaryTextColor,
                        style = KmiTypography.caption,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PaymentMethodChoiceCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkMode = colorScheme.background.luminance() < 0.5f

    val containerColor =
        if (selected) {
            Color(0xFF19C37D).copy(
                alpha = if (isDarkMode) 0.22f else 0.16f
            )
        } else if (isDarkMode) {
            Color.White.copy(alpha = 0.07f)
        } else {
            colorScheme.surfaceVariant.copy(alpha = 0.72f)
        }

    val borderColor =
        if (selected) {
            if (isDarkMode) {
                Color(0xFF7CFFB2).copy(alpha = 0.75f)
            } else {
                Color(0xFF078B59).copy(alpha = 0.72f)
            }
        } else if (isDarkMode) {
            Color.White.copy(alpha = 0.14f)
        } else {
            colorScheme.outlineVariant
        }

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                color = colorScheme.onSurface,
                style =
                    KmiTypography.caption.copy(
                        fontWeight = FontWeight.Bold
                    ),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
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
    val fieldTextAlign =
        if (isEnglish) TextAlign.Start else TextAlign.End

    val fieldLayoutDirection =
        if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(
        LocalLayoutDirection provides fieldLayoutDirection
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            label = {
                Text(
                    text = label,
                    style = KmiTypography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = fieldTextAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = null,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(16.dp),
            colors = premiumFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            textStyle =
                KmiTypography.body.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = fieldTextAlign
                )
        )
    }
}

@Composable
private fun premiumFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor =
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),

        focusedContainerColor =
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
        unfocusedContainerColor =
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
        disabledContainerColor =
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),

        focusedBorderColor = Color(0xFF19C37D),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        disabledBorderColor =
            MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),

        focusedLabelColor = Color(0xFF078B59),
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor =
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),

        cursorColor = Color(0xFF19C37D),

        focusedLeadingIconColor = Color(0xFF078B59),
        unfocusedLeadingIconColor =
            MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor =
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),

        focusedTrailingIconColor = Color(0xFF078B59),
        unfocusedTrailingIconColor =
            MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor =
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
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
