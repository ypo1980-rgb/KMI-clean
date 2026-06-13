package il.kmi.app.screens.forms.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.LocalPhone
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.border
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import il.kmi.app.ui.DrawerBridge
import il.kmi.app.ui.KmiTopBar

//==========================================================================

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

private fun MembershipPaymentPrefill.hasAnyValue(): Boolean {
    return traineeFirstName.isNotBlank() ||
            traineeLastName.isNotBlank() ||
            traineeIdNumber.isNotBlank() ||
            traineeBirthDate.isNotBlank() ||
            traineeEmail.isNotBlank() ||
            traineePhone.isNotBlank() ||
            traineeBranch.isNotBlank() ||
            traineeOtherBranch.isNotBlank() ||
            payerFirstName.isNotBlank() ||
            payerLastName.isNotBlank() ||
            payerEmail.isNotBlank() ||
            payerPhone.isNotBlank()
}

private fun String?.orFallback(fallback: String): String {
    return this?.takeIf { it.isNotBlank() } ?: fallback
}

private fun Map<String, Any?>.stringValue(vararg keys: String): String {
    for (key in keys) {
        val value = this[key]
        if (value is String && value.isNotBlank()) return value
    }
    return ""
}

private fun Map<String, Any?>.nestedStringValue(
    nestedKey: String,
    vararg keys: String
): String {
    val nested = this[nestedKey] as? Map<*, *> ?: return ""
    for (key in keys) {
        val value = nested[key]
        if (value is String && value.isNotBlank()) return value
    }
    return ""
}

private fun splitFullName(fullName: String): Pair<String, String> {
    val parts = fullName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }

    if (parts.isEmpty()) return "" to ""
    if (parts.size == 1) return parts.first() to ""

    val firstName = parts.first()
    val lastName = parts.drop(1).joinToString(" ")

    return firstName to lastName
}

private suspend fun loadMembershipPaymentPrefillFromServer(): MembershipPaymentPrefill {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return MembershipPaymentPrefill()
    val uid = currentUser.uid
    val authEmail = currentUser.email.orEmpty()
    val authPhone = currentUser.phoneNumber.orEmpty()
    val authDisplayName = currentUser.displayName.orEmpty()
    val authNameParts = splitFullName(authDisplayName)

    val firestore = FirebaseFirestore.getInstance()

    val possibleDocuments = listOf(
        firestore.collection("users").document(uid),
        firestore.collection("trainees").document(uid),
        firestore.collection("members").document(uid)
    )

    for (documentRef in possibleDocuments) {
        val snapshot = runCatching { documentRef.get().await() }.getOrNull()
        val data = snapshot?.data ?: continue

        val serverFullName = data.stringValue(
            "fullName",
            "full_name",
            "displayName",
            "display_name",
            "name",
            "traineeName",
            "trainee_name"
        ).ifBlank {
            data.nestedStringValue(
                "profile",
                "fullName",
                "full_name",
                "displayName",
                "display_name",
                "name",
                "traineeName",
                "trainee_name"
            )
        }

        val serverNameParts = splitFullName(serverFullName)

        val firstName = data.stringValue(
            "firstName",
            "first_name",
            "traineeFirstName",
            "trainee_first_name"
        ).ifBlank {
            data.nestedStringValue(
                "profile",
                "firstName",
                "first_name",
                "traineeFirstName",
                "trainee_first_name"
            )
        }.ifBlank {
            serverNameParts.first
        }.ifBlank {
            authNameParts.first
        }

        val lastName = data.stringValue(
            "lastName",
            "last_name",
            "traineeLastName",
            "trainee_last_name",
            "familyName",
            "family_name"
        ).ifBlank {
            data.nestedStringValue(
                "profile",
                "lastName",
                "last_name",
                "traineeLastName",
                "trainee_last_name",
                "familyName",
                "family_name"
            )
        }.ifBlank {
            serverNameParts.second
        }.ifBlank {
            authNameParts.second
        }

        val idNumber = data.stringValue(
            "idNumber",
            "id_number",
            "identityNumber",
            "identity_number",
            "tz",
            "teudatZehut",
            "traineeIdNumber"
        ).ifBlank {
            data.nestedStringValue(
                "profile",
                "idNumber",
                "id_number",
                "identityNumber",
                "identity_number",
                "tz",
                "teudatZehut"
            )
        }

        val birthDate = data.stringValue(
            "birthDate",
            "birth_date",
            "dateOfBirth",
            "date_of_birth",
            "dob",
            "traineeBirthDate"
        ).ifBlank {
            data.nestedStringValue(
                "profile",
                "birthDate",
                "birth_date",
                "dateOfBirth",
                "date_of_birth",
                "dob"
            )
        }

        val email = data.stringValue(
            "email",
            "emailAddress",
            "email_address",
            "traineeEmail"
        ).ifBlank {
            data.nestedStringValue(
                "profile",
                "email",
                "emailAddress",
                "email_address"
            )
        }.ifBlank {
            authEmail
        }

        val phone = data.stringValue(
            "phone",
            "phoneNumber",
            "phone_number",
            "mobile",
            "mobilePhone",
            "traineePhone"
        ).ifBlank {
            data.nestedStringValue(
                "profile",
                "phone",
                "phoneNumber",
                "phone_number",
                "mobile",
                "mobilePhone"
            )
        }.ifBlank {
            authPhone
        }

        val branch = data.stringValue(
            "branch",
            "branchName",
            "branch_name",
            "selectedBranch",
            "selected_branch",
            "traineeBranch"
        ).ifBlank {
            data.nestedStringValue(
                "profile",
                "branch",
                "branchName",
                "branch_name",
                "selectedBranch",
                "selected_branch"
            )
        }

        val prefill = MembershipPaymentPrefill(
            traineeFirstName = firstName,
            traineeLastName = lastName,
            traineeIdNumber = idNumber,
            traineeBirthDate = birthDate,
            traineeEmail = email,
            traineePhone = phone,
            traineeBranch = branch,
            traineeOtherBranch = "",
            payerFirstName = firstName,
            payerLastName = lastName,
            payerEmail = email,
            payerPhone = phone
        )

        if (prefill.hasAnyValue()) {
            return prefill
        }
    }

    return MembershipPaymentPrefill(
        traineeFirstName = authNameParts.first,
        traineeLastName = authNameParts.second,
        traineeEmail = authEmail,
        traineePhone = authPhone,
        payerFirstName = authNameParts.first,
        payerLastName = authNameParts.second,
        payerEmail = authEmail,
        payerPhone = authPhone
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipPaymentScreen(
    isEnglish: Boolean = false,
    prefill: MembershipPaymentPrefill = MembershipPaymentPrefill(),
    onClose: () -> Unit = {},
    onReadFullPolicy: () -> Unit = {},
    onContinueToPayment: (MembershipPaymentFormData) -> Unit = {}
) {
    var serverPrefill by remember { mutableStateOf(prefill) }
    var didApplyServerPrefill by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loadedPrefill = loadMembershipPaymentPrefillFromServer()

        serverPrefill = MembershipPaymentPrefill(
            traineeFirstName = loadedPrefill.traineeFirstName.orFallback(prefill.traineeFirstName),
            traineeLastName = loadedPrefill.traineeLastName.orFallback(prefill.traineeLastName),
            traineeIdNumber = loadedPrefill.traineeIdNumber.orFallback(prefill.traineeIdNumber),
            traineeBirthDate = loadedPrefill.traineeBirthDate.orFallback(prefill.traineeBirthDate),
            traineeEmail = loadedPrefill.traineeEmail.orFallback(prefill.traineeEmail),
            traineePhone = loadedPrefill.traineePhone.orFallback(prefill.traineePhone),
            traineeBranch = loadedPrefill.traineeBranch.orFallback(prefill.traineeBranch),
            traineeOtherBranch = loadedPrefill.traineeOtherBranch.orFallback(prefill.traineeOtherBranch),
            payerFirstName = loadedPrefill.payerFirstName.orFallback(prefill.payerFirstName),
            payerLastName = loadedPrefill.payerLastName.orFallback(prefill.payerLastName),
            payerEmail = loadedPrefill.payerEmail.orFallback(prefill.payerEmail),
            payerPhone = loadedPrefill.payerPhone.orFallback(prefill.payerPhone)
        )
    }

    val title = if (isEnglish) "Membership Payment" else "תשלום דמי חבר"
    val traineeTitle = if (isEnglish) "Trainee Details" else "פרטי חניך"
    val payerTitle = if (isEnglish) "Payer Details for Invoice" else "פרטי המשלם לשליחת חשבונית"
    val productTitle = if (isEnglish) "Payment Summary" else "סיכום תשלום"
    val policyTitle = if (isEnglish) "Cancellation & Refund Policy" else "מדיניות ביטולים והחזרים"
    val continueText = if (isEnglish) "Continue to Payment" else "המשך לתשלום"
    val readPolicyText = if (isEnglish) "Read Full Policy" else "קרא מדיניות מלאה"
    val payerSameToggleText = if (isEnglish) "Payer is the same as trainee" else "המשלם זהה לפרטי החניך"

    val branchOptions = remember(isEnglish) {
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
    var showFullRefundPolicy by rememberSaveable { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }

    val missingBranchValue = if (isEnglish) MISSING_BRANCH_EN else MISSING_BRANCH_HE
    val shouldShowOtherBranch = traineeBranch == missingBranchValue

    LaunchedEffect(serverPrefill, branchOptions) {
        if (!didApplyServerPrefill && serverPrefill.hasAnyValue()) {
            traineeFirstName = serverPrefill.traineeFirstName
            traineeLastName = serverPrefill.traineeLastName
            traineeIdNumber = serverPrefill.traineeIdNumber
            traineeBirthDate = serverPrefill.traineeBirthDate
            traineeEmail = serverPrefill.traineeEmail
            traineePhone = serverPrefill.traineePhone

            val loadedBranch = serverPrefill.traineeBranch.trim()
            if (loadedBranch.isBlank()) {
                traineeBranch = branchOptions.first()
                traineeOtherBranch = ""
            } else if (branchOptions.contains(loadedBranch)) {
                traineeBranch = loadedBranch
                traineeOtherBranch = ""
            } else {
                traineeBranch = missingBranchValue
                traineeOtherBranch = loadedBranch
            }

            payerFirstName = serverPrefill.payerFirstName.ifBlank { serverPrefill.traineeFirstName }
            payerLastName = serverPrefill.payerLastName.ifBlank { serverPrefill.traineeLastName }
            payerEmail = serverPrefill.payerEmail.ifBlank { serverPrefill.traineeEmail }
            payerPhone = serverPrefill.payerPhone.ifBlank { serverPrefill.traineePhone }

            didApplyServerPrefill = true
        }
    }

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
        containerColor = Color.Transparent,
        topBar = {
            KmiTopBar(
                title = title,
                currentLang = if (isEnglish) "en" else "he",
                showMenu = true,
                showRoleStatus = true,
                showSettings = true,
                showBottomActions = true,
                showModePill = true,
                showRoleBadge = true,
                showTopHome = false,
                showTopSearch = false,
                showTopShare = false,
                centerTitle = true,
                lockHome = false,
                lockSearch = false,
                onOpenDrawer = {
                    DrawerBridge.open()
                },
                onHome = {
                    DrawerBridge.openHome()
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFEAF4FF),
                            Color(0xFFB7DDF7),
                            Color(0xFF1F78B4),
                            Color(0xFF062B4A)
                        )
                    )
                    )
                ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProductHeroCard(
                    isEnglish = isEnglish,
                    amountText = if (isEnglish) "₪150.00" else "150.00 ₪",
                    onClose = onClose
                )

                SectionCard(
                    title = traineeTitle,
                    icon = Icons.Default.AccountCircle,
                    isEnglish = isEnglish
                ) {
                    FormTextField(
                        value = traineeFirstName,
                        onValueChange = { traineeFirstName = it },
                        label = if (isEnglish) "First Name" else "שם פרטי",
                        leadingIcon = Icons.Default.AccountCircle,
                        isEnglish = isEnglish
                    )

                    FormTextField(
                        value = traineeLastName,
                        onValueChange = { traineeLastName = it },
                        label = if (isEnglish) "Last Name" else "שם משפחה",
                        leadingIcon = Icons.Default.AccountCircle,
                        isEnglish = isEnglish
                    )

                    FormTextField(
                        value = traineeIdNumber,
                        onValueChange = { traineeIdNumber = it },
                        label = if (isEnglish) "ID Number" else "מספר ת.ז.",
                        keyboardType = KeyboardType.Number,
                        leadingIcon = Icons.Default.Badge,
                        isEnglish = isEnglish
                    )

                    FormTextField(
                        value = traineeBirthDate,
                        onValueChange = { traineeBirthDate = it },
                        label = if (isEnglish) "Birth Date" else "תאריך לידה",
                        placeholder = "DD/MM/YYYY",
                        leadingIcon = Icons.Default.Event,
                        isEnglish = isEnglish
                    )

                    FormTextField(
                        value = traineeEmail,
                        onValueChange = { traineeEmail = it },
                        label = if (isEnglish) "Email" else "כתובת דוא\"ל",
                        keyboardType = KeyboardType.Email,
                        leadingIcon = Icons.Default.MarkEmailRead,
                        isEnglish = isEnglish
                    )

                    FormTextField(
                        value = traineePhone,
                        onValueChange = { traineePhone = it },
                        label = if (isEnglish) "Mobile Phone" else "מספר טלפון נייד",
                        keyboardType = KeyboardType.Phone,
                        leadingIcon = Icons.Default.LocalPhone,
                        isEnglish = isEnglish
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
                                    color = Color(0xFF5E6C80),
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                    },
                            leadingIcon = if (isEnglish) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.72f),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            } else {
                                null
                            },
                            trailingIcon = if (isEnglish) {
                                {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = branchExpanded
                                    )
                                }
                            } else {
                                {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = branchExpanded
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.72f),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .menuAnchor(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                            ),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                disabledContainerColor = Color(0xFFF4F7FB),

                                focusedBorderColor = Color(0xFFBFD0E8),
                                unfocusedBorderColor = Color(0xFFD8E3F5),
                                disabledBorderColor = Color(0xFFD8E3F5),

                                focusedTextColor = Color(0xFF1E2A3D),
                                unfocusedTextColor = Color(0xFF1E2A3D),
                                disabledTextColor = Color(0xFF1E2A3D).copy(alpha = 0.50f),

                                focusedLabelColor = Color(0xFF5E6C80),
                                unfocusedLabelColor = Color(0xFF6B778B),
                                disabledLabelColor = Color(0xFF6B778B).copy(alpha = 0.45f),

                                focusedLeadingIconColor = Color(0xFF5E6C80),
                                unfocusedLeadingIconColor = Color(0xFF6B778B),
                                disabledLeadingIconColor = Color(0xFF6B778B).copy(alpha = 0.40f),

                                focusedTrailingIconColor = Color(0xFF5E6C80),
                                unfocusedTrailingIconColor = Color(0xFF6B778B),
                                disabledTrailingIconColor = Color(0xFF6B778B).copy(alpha = 0.40f),

                                cursorColor = Color(0xFF1E2A3D)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = branchExpanded,
                            onDismissRequest = { branchExpanded = false }
                        ) {
                            branchOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
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
                            leadingIcon = Icons.Default.Domain,
                            isEnglish = isEnglish
                        )
                    }
                }

                SectionCard(
                    title = payerTitle,
                    icon = Icons.Default.Receipt,
                    isEnglish = isEnglish
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF24365E)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(7.dp),
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
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
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
                        leadingIcon = Icons.Default.Person,
                        isEnglish = isEnglish
                    )

                    FormTextField(
                        value = payerLastName,
                        onValueChange = { if (!payerSameAsTrainee) payerLastName = it },
                        label = if (isEnglish) "Last Name" else "שם משפחה",
                        enabled = !payerSameAsTrainee,
                        leadingIcon = Icons.Default.Person,
                        isEnglish = isEnglish
                    )

                    FormTextField(
                        value = payerEmail,
                        onValueChange = { if (!payerSameAsTrainee) payerEmail = it },
                        label = if (isEnglish) "Email Address" else "כתובת דוא\"ל",
                        keyboardType = KeyboardType.Email,
                        enabled = !payerSameAsTrainee,
                        leadingIcon = Icons.Default.Email,
                        isEnglish = isEnglish
                    )

                    FormTextField(
                        value = payerPhone,
                        onValueChange = { if (!payerSameAsTrainee) payerPhone = it },
                        label = if (isEnglish) "Phone Number" else "מספר טלפון",
                        keyboardType = KeyboardType.Phone,
                        enabled = !payerSameAsTrainee,
                        leadingIcon = Icons.Default.PhoneIphone,
                        isEnglish = isEnglish
                    )
                }

                SectionCard(
                    title = productTitle,
                    icon = Icons.Default.Wallet,
                    isEnglish = isEnglish
                ) {
                    ProductPriceRow(
                        label = if (isEnglish) "Product" else "מוצר",
                        value = if (isEnglish) "Association Membership Fee" else "דמי חבר לעמותה",
                        isEnglish = isEnglish
                    )

                    ProductPriceRow(
                        label = if (isEnglish) "Price" else "מחיר",
                        value = if (isEnglish) "₪150.00" else "150.00 ₪",
                        emphasize = true,
                        isEnglish = isEnglish
                    )
                }

                SectionCard(
                    title = policyTitle,
                    icon = Icons.Default.Description,
                    isEnglish = isEnglish
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFD8E3F5)
                        )
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "Payment of membership fees is final after approval, except in cases such as duplicate payment or another good-faith mistake, subject to review by the association."
                            } else {
                                "תשלום דמי חבר הוא סופי לאחר אישור הפעולה, למעט מקרים של תשלום כפול בטעות או טעות אחרת בתום לב, בכפוף לבדיקת העמותה."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E2A3D),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isEnglish) {
                            Arrangement.Start
                        } else {
                            Arrangement.End
                        }
                    ) {
                        TextButton(
                            onClick = {
                                showFullRefundPolicy = true
                                onReadFullPolicy()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF7C5CE6)
                            )
                        ) {
                            if (isEnglish) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(readPolicyText)
                            } else {
                                Text(readPolicyText)
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (policyAccepted) {
                            Color(0xFFE8FFF3)
                        } else {
                            Color.White
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = if (policyAccepted) {
                                    Color(0xFF19C37D)
                                } else {
                                    Color(0xFFBFD0E8)
                                },
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEnglish) {
                                Checkbox(
                                    checked = policyAccepted,
                                    onCheckedChange = { policyAccepted = it },
                                    modifier = Modifier.size(34.dp),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF19C37D),
                                        uncheckedColor = Color(0xFF7CFFB2),
                                        checkmarkColor = Color.White
                                    )
                                )

                                Text(
                                    text = "I have read and agree to the cancellation and refund policy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1E2A3D),
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Left,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                )
                            } else {
                                Text(
                                    text = "קראתי ואני מאשר/ת את מדיניות הביטולים וההחזרים.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1E2A3D),
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )

                                Checkbox(
                                    checked = policyAccepted,
                                    onCheckedChange = { policyAccepted = it },
                                    modifier = Modifier.size(34.dp),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF19C37D),
                                        uncheckedColor = Color(0xFF7CFFB2),
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
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
                        containerColor = Color(0xFF7C5CE6),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFD8E3F5),
                        disabledContentColor = Color(0xFF6B778B)
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

    if (showFullRefundPolicy) {
        AlertDialog(
            onDismissRequest = {
                showFullRefundPolicy = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFullRefundPolicy = false
                    }
                ) {
                    Text(if (isEnglish) "Close" else "סגור")
                }
            },
            title = {
                Text(
                    text = policyTitle,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                ) {
                    Text(
                        text = if (isEnglish) {
                            "1. The membership fee is a registration and association membership payment."
                        } else {
                            "1. דמי החבר הם תשלום עבור רישום וחברות בעמותה."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )

                    Text(
                        text = if (isEnglish) {
                            "2. After payment approval, the payment is considered final, except in cases of duplicate payment, technical error, or another good-faith mistake."
                        } else {
                            "2. לאחר אישור התשלום, התשלום נחשב סופי, למעט מקרים של תשלום כפול, תקלה טכנית או טעות אחרת בתום לב."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )

                    Text(
                        text = if (isEnglish) {
                            "3. Refund requests will be reviewed by the association according to the payment details, payment date, and the reason for the request."
                        } else {
                            "3. בקשות להחזר ייבחנו על ידי העמותה בהתאם לפרטי התשלום, מועד התשלום וסיבת הבקשה."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )

                    Text(
                        text = if (isEnglish) {
                            "4. If a refund is approved, it will be processed using the same payment method or another method approved by the association."
                        } else {
                            "4. אם יאושר החזר, הוא יבוצע באמצעי התשלום המקורי או באמצעי אחר שיאושר על ידי העמותה."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )

                    Text(
                        text = if (isEnglish) {
                            "5. Administrative or clearing fees may be deducted if required by the payment provider or applicable rules."
                        } else {
                            "5. ייתכן ניכוי עמלות טיפול או סליקה, ככל שהדבר נדרש על ידי ספק התשלום או לפי הנהלים החלים."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )

                    Text(
                        text = if (isEnglish) {
                            "6. By checking the approval box, the payer confirms that they have read and agreed to this cancellation and refund policy before continuing to payment."
                        } else {
                            "6. סימון תיבת האישור מהווה אישור לכך שהמשלם קרא והסכים למדיניות הביטולים וההחזרים לפני המעבר לתשלום."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = Color(0xFFF8F5FB)
        )
    }
}

@Composable
private fun PremiumPaymentHeader(
    title: String,
    subtitle: String,
    isEnglish: Boolean,
    onClose: () -> Unit
) {
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = horizontalAlignment
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
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
                        contentDescription = if (isEnglish) "Close" else "סגור",
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
    amountText: String,
    onClose: () -> Unit
) {
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

    val compactAmount = amountText
        .replace(".00", "")
        .replace("150 ₪", "₪150")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEAF2FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFDCE7F7)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isEnglish) "Close" else "סגור",
                            tint = Color(0xFF5D6F89)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF8B5CF6)
                ) {
                    Text(
                        text = compactAmount,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                        maxLines = 1
                    )
                }

                Surface(
                    modifier = Modifier.align(Alignment.CenterStart),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFDCE7F7)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .padding(11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFF6E59B5)
                        )
                    }
                }
            }

            Text(
                text = if (isEnglish) "Association Membership" else "חברות בעמותה",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1E2A3D),
                modifier = Modifier.fillMaxWidth(),
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )

            Text(
                text = if (isEnglish) {
                    "Secure payment registration before continuing"
                } else {
                    "רישום מאובטח לתשלום לפני מעבר לסליקה"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5E6C80),
                modifier = Modifier.fillMaxWidth(),
                textAlign = textAlign,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    isEnglish: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEAF2FF)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 10.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            tint = Color(0xFF0F5E9C)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1E2A3D),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = textAlign,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                color = Color(0xFFBFD0E8)
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
    placeholder: String = "",
    isEnglish: Boolean
) {
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        label = {
            Text(
                text = label,
                color = Color(0xFF5E6C80),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        },
        leadingIcon = if (isEnglish) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color(0xFF6B778B),
                    modifier = Modifier.size(19.dp)
                )
            }
        } else {
            null
        },
        trailingIcon = if (isEnglish) {
            null
        } else {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color(0xFF6B778B),
                    modifier = Modifier.size(19.dp)
                )
            }
        },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF7A879A),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            textAlign = textAlign,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E2A3D)
        ),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,

            focusedBorderColor = Color(0xFFBFD0E8),
            unfocusedBorderColor = Color(0xFFD8E3F5),
            disabledBorderColor = Color(0xFFD8E3F5),

            focusedTextColor = Color(0xFF1E2A3D),
            unfocusedTextColor = Color(0xFF1E2A3D),
            disabledTextColor = Color(0xFF1E2A3D),

            focusedLabelColor = Color(0xFF5E6C80),
            unfocusedLabelColor = Color(0xFF6B778B),
            disabledLabelColor = Color(0xFF6B778B),

            focusedLeadingIconColor = Color(0xFF5E6C80),
            unfocusedLeadingIconColor = Color(0xFF6B778B),
            disabledLeadingIconColor = Color(0xFF6B778B),

            focusedTrailingIconColor = Color(0xFF5E6C80),
            unfocusedTrailingIconColor = Color(0xFF6B778B),
            disabledTrailingIconColor = Color(0xFF6B778B),

            cursorColor = Color(0xFF1E2A3D)
        )
    )
}

@Composable
private fun ProductPriceRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
    isEnglish: Boolean
) {
    val labelAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val valueAlign = if (isEnglish) TextAlign.Right else TextAlign.Left

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5E6C80),
            textAlign = labelAlign,
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
                Color(0xFF7C5CE6)
            } else {
                Color(0xFF1E2A3D)
            },
            textAlign = valueAlign
        )
    }
}