package il.kmi.app.screens.admin

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.functions.FirebaseFunctions
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.app.privacy.TraineeDisplayNameMapper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiLanguageDirection
import il.kmi.app.ui.pdf.KmiPdfDirection
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBackground

//=====================================================================

@Composable
private fun PaymentsPremiumLoading(
    text: String,
    modifier: Modifier = Modifier
) {
    KmiLoadingRings(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        text = text
    )
}

private const val DEFAULT_MEMBERSHIP_REQUIRED_AMOUNT = 150.0

/**
 * רשומות חישוב לפי סניף.
 *
 * זהות המתאמן נשמרת ללא שינוי.
 * כל סכום שייך לסניף שלו בלבד.
 *
 * רשומות ישנות ללא branchPayments ממשיכות
 * להשתמש בשדות הקיימים.
 */
private fun PaymentReportItem.paymentRowsForBranch(
    selectedBranch: String? = null
): List<PaymentReportItem> {

    if (branchPayments.isEmpty()) {
        return listOf(this).filter { item ->
            selectedBranch == null ||
                    item.branchName == selectedBranch
        }
    }

    return branchPayments
        .filter { payment ->
            selectedBranch == null ||
                    payment.branchName == selectedBranch
        }
        .map { payment ->
            copy(
                branchName = payment.branchName,
                requiredAmount = payment.requiredAmount,
                paidAmount = payment.paidAmount,
                status = payment.status,
                paymentMethod = payment.paymentMethod,
                paymentDate = payment.paymentDate,
                notes = payment.notes,
                branchPayments = emptyList()
            )
        }
}

private fun paymentNowDateText(): String {
    return SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.getDefault()
    ).format(Date())
}

private fun paymentCurrentYear(): Int {
    return SimpleDateFormat("yyyy", Locale.getDefault())
        .format(Date())
        .toIntOrNull()
        ?: 0
}

private fun paymentStatusFromAmount(
    paidAmount: Double,
    requiredAmount: Double
): PaymentStatus {

    val effectiveRequiredAmount =
        requiredAmount.takeIf { it > 0.0 }
            ?: DEFAULT_MEMBERSHIP_REQUIRED_AMOUNT

    return when {
        paidAmount <= 0.0 ->
            PaymentStatus.UNPAID

        paidAmount < effectiveRequiredAmount ->
            PaymentStatus.PARTIAL

        else ->
            PaymentStatus.PAID
    }
}

private fun paymentMethodFromString(value: String?): PaymentMethod {
    val clean = value.orEmpty().trim()

    return PaymentMethod.entries.firstOrNull {
        it.name.equals(clean, ignoreCase = true)
    } ?: PaymentMethod.MANUAL
}

private fun paymentStatusToFirestore(status: PaymentStatus): String {
    return status.name
}

private fun paymentMethodToFirestore(method: PaymentMethod): String {
    return method.name
}

private fun String?.cleanPaymentText(): String {
    return this
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        .orEmpty()
}

private fun PaymentReportItem.demoSafeName(
    isEnglish: Boolean,
    demoIndex: Int? = null
): String {
    val displayName =
        if (demoIndex != null) {
            TraineeDisplayNameMapper.displayName(
                realName = fullName,
                stableKey = traineeId,
                demoIndex = demoIndex,
                isEnglish = isEnglish
            )
        } else {
            TraineeDisplayNameMapper.displayName(
                realName = fullName,
                stableKey = traineeId,
                isEnglish = isEnglish
            )
        }

    return displayName.ifBlank {
        if (isEnglish) {
            "Unnamed trainee"
        } else {
            "מתאמן ללא שם"
        }
    }
}

private fun PaymentReportItem.demoSafePhone(
    isEnglish: Boolean,
    demoIndex: Int? = null
): String {
    val mappedName =
        demoSafeName(
            isEnglish = isEnglish,
            demoIndex = demoIndex
        )

    val isDemoDisplay =
        mappedName.trim() != fullName.trim()

    return if (isDemoDisplay) {
        if (isEnglish) {
            "Hidden in demo"
        } else {
            "מוסתר בהדגמה"
        }
    } else {
        normalizePaymentPhone(phone)
    }
}

private fun String.looksLikeTechnicalId(): Boolean {
    val clean = trim()

    return clean.length >= 18 &&
            clean.none { it.isWhitespace() } &&
            clean.any { it.isDigit() } &&
            clean.any { it.isLetter() }
}

private fun String?.validDisplayNameOrNull(): String? {
    val clean = cleanPaymentText()

    return clean.takeIf {
        it.isNotBlank() &&
                !it.looksLikeTechnicalId() &&
                !it.contains("@") &&
                it.length <= 70
    }
}

private fun DocumentSnapshot.paymentStringFromAny(vararg keys: String): String {
    keys.forEach { key ->
        getString(key)
            ?.cleanPaymentText()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }

    val dataMap = data ?: return ""

    keys.forEach { key ->
        val value = dataMap[key]
        if (value is String && value.trim().isNotBlank()) {
            return value.trim()
        }
    }

    val nestedKeys = listOf("profile", "personal", "trainee", "student", "user", "details")
    nestedKeys.forEach { nestedKey ->
        val nested = dataMap[nestedKey] as? Map<*, *> ?: return@forEach

        keys.forEach { key ->
            val value = nested[key]
            if (value is String && value.trim().isNotBlank()) {
                return value.trim()
            }
        }
    }

    return ""
}

private fun DocumentSnapshot.paymentUserName(): String {
    val firstName = paymentStringFromAny(
        "firstName",
        "first_name",
        "firstname",
        "traineeFirstName",
        "trainee_first_name",
        "studentFirstName",
        "student_first_name",
        "privateName",
        "private_name"
    )

    val lastName = paymentStringFromAny(
        "lastName",
        "last_name",
        "lastname",
        "familyName",
        "family_name",
        "traineeLastName",
        "trainee_last_name",
        "studentLastName",
        "student_last_name"
    )

    val combinedName = listOf(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .validDisplayNameOrNull()

    return paymentStringFromAny(
        "fullName",
        "full_name",
        "displayName",
        "display_name",
        "name",
        "traineeName",
        "trainee_name",
        "studentName",
        "student_name"
    ).validDisplayNameOrNull()
        ?: combinedName
        ?: ""
}

private fun DocumentSnapshot.paymentUserPhone(): String {
    return getString("phone")?.takeIf { it.isNotBlank() }
        ?: getString("phoneNumber")?.takeIf { it.isNotBlank() }
        ?: getString("phone_number")?.takeIf { it.isNotBlank() }
        ?: ""
}

private data class PaymentUserBundle(
    val primaryDoc: DocumentSnapshot,
    val allDocs: List<DocumentSnapshot>
)

private fun normalizePaymentPhone(raw: String): String {
    val digits =
        raw.filter { it.isDigit() }

    return when {
        digits.startsWith("00972") &&
                digits.length > 5 -> {
            "0" + digits.removePrefix("00972")
        }

        digits.startsWith("972") &&
                digits.length > 3 -> {
            "0" + digits.removePrefix("972")
        }

        else -> digits
    }
}

private fun DocumentSnapshot.paymentUserEmail(): String {
    return (
            getString("email")
                ?: getString("emailLower")
                ?: getString("userEmail")
                ?: getString("user_email")
                ?: paymentStringFromAny(
                    "email",
                    "emailLower",
                    "userEmail",
                    "user_email"
                )
            )
        .trim()
        .lowercase(Locale.ROOT)
}

private fun DocumentSnapshot.paymentUserMergeKey(): String {
    val email = paymentUserEmail()
    val phone = normalizePaymentPhone(paymentUserPhone())

    return when {
        email.isNotBlank() -> "email:$email"
        phone.isNotBlank() -> "phone:$phone"
        else -> "doc:$id"
    }
}

private fun choosePrimaryPaymentUserDoc(
    docs: List<DocumentSnapshot>
): DocumentSnapshot {
    return docs
        .sortedWith(
            compareByDescending<DocumentSnapshot> { it.paymentUserName().isNotBlank() }
                .thenByDescending { normalizePaymentPhone(it.paymentUserPhone()).isNotBlank() }
                .thenByDescending { it.paymentUserBranch().isNotBlank() }
                .thenBy { it.id }
        )
        .first()
}

private fun DocumentSnapshot.paymentIdentityKeys(): List<String> {
    return listOf(
        id,
        getString("uid"),
        getString("authUid"),
        getString("userDocId"),
        getString("traineeId")
    )
        .mapNotNull { it?.trim()?.takeIf { key -> key.isNotBlank() } }
        .distinct()
}

private fun DocumentSnapshot.paymentUserBranch(): String {
    val branchesList = get("branches") as? List<*>
    val firstBranchFromList = branchesList
        ?.mapNotNull { it?.toString()?.trim() }
        ?.firstOrNull { it.isNotBlank() }
        .orEmpty()

    return getString("activeBranch")?.takeIf { it.isNotBlank() }
        ?: getString("active_branch")?.takeIf { it.isNotBlank() }
        ?: getString("branch")?.takeIf { it.isNotBlank() }
        ?: getString("branchesCsv")?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        ?: firstBranchFromList
}

private fun DocumentSnapshot.paymentRequiredAmountFromAny(): Double {
    val storedAmount =
        getDouble("requiredAmount")
            ?: getDouble("membershipRequiredAmount")
            ?: getDouble("membershipFee")
            ?: getDouble("annualMembershipFee")
            ?: getDouble("feeAmount")

    return storedAmount
        ?.takeIf { it > 0.0 }
        ?: DEFAULT_MEMBERSHIP_REQUIRED_AMOUNT
}

private fun DocumentSnapshot.isPaymentRelevantTrainee(): Boolean {
    val role = (
            getString("role")
                ?: getString("userType")
                ?: getString("type")
                ?: paymentStringFromAny("role", "userType", "type")
            ).trim().lowercase()

    val statusText = (
            getString("status")
                ?: getString("active")
                ?: paymentStringFromAny("status", "active")
            ).trim().lowercase()

    val isActive = getBoolean("isActive") != false &&
            statusText != "inactive" &&
            statusText != "disabled" &&
            statusText != "blocked" &&
            statusText != "לא פעיל"

    val isTrainee =
        role == "trainee" ||
                role == "student" ||
                role.contains("trainee") ||
                role.contains("student") ||
                role.contains("מתאמן") ||
                role.contains("חניך")

    val hasUsableProfile =
        paymentUserName().isNotBlank() ||
                paymentUserPhone().isNotBlank() ||
                paymentUserBranch().isNotBlank()

    return isActive && (isTrainee || hasUsableProfile)
}

private suspend fun loadRealPaymentsReportItems(): List<PaymentReportItem> {

    val result =
        FirebaseFunctions
            .getInstance()
            .getHttpsCallable(
                "loadSecurePaymentsReport"
            )
            .call()
            .await()

    val payload =
        result.data as? Map<*, *>
            ?: return emptyList()

    val rawItems =
        payload["items"] as? List<*>
            ?: return emptyList()

    return rawItems
        .mapNotNull { rawItem ->

            val item =
                rawItem as? Map<*, *>
                    ?: return@mapNotNull null

            val traineeId =
                item["traineeId"]
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val fullName =
                item["fullName"]
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            if (
                traineeId.isBlank() ||
                fullName.isBlank()
            ) {
                return@mapNotNull null
            }

            val branchName =
                item["branchName"]
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val phone =
                normalizePaymentPhone(
                    item["phone"]
                        ?.toString()
                        .orEmpty()
                )

            val requiredAmount =
                (item["requiredAmount"] as? Number)
                    ?.toDouble()
                    ?.takeIf {
                        it > 0.0
                    }
                    ?: DEFAULT_MEMBERSHIP_REQUIRED_AMOUNT

            val paidAmount =
                (item["paidAmount"] as? Number)
                    ?.toDouble()
                    ?.coerceAtLeast(0.0)
                    ?: 0.0

            val status =
                runCatching {
                    PaymentStatus.valueOf(
                        item["status"]
                            ?.toString()
                            ?.trim()
                            ?.uppercase(Locale.ROOT)
                            .orEmpty()
                    )
                }
                    .getOrElse {
                        paymentStatusFromAmount(
                            paidAmount = paidAmount,
                            requiredAmount = requiredAmount
                        )
                    }

            val paymentMethod =
                paymentMethodFromString(
                    item["paymentMethod"]
                        ?.toString()
                )

            val paymentDate =
                item["paymentDate"]
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val notes =
                item["notes"]
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val branchPayments =
                (item["branchPayments"] as? List<*>)
                    .orEmpty()
                    .mapNotNull { rawBranch ->
                        val branch =
                            rawBranch as? Map<*, *>
                                ?: return@mapNotNull null

                        val name =
                            branch["branchName"]
                                ?.toString()
                                ?.trim()
                                .orEmpty()

                        if (name.isBlank()) {
                            return@mapNotNull null
                        }

                        val required =
                            (branch["requiredAmount"] as? Number)
                                ?.toDouble()
                                ?.takeIf { it > 0.0 }
                                ?: DEFAULT_MEMBERSHIP_REQUIRED_AMOUNT

                        val paid =
                            (branch["paidAmount"] as? Number)
                                ?.toDouble()
                                ?.coerceAtLeast(0.0)
                                ?: 0.0

                        PaymentBranchItem(
                            branchName = name,
                            requiredAmount = required,
                            paidAmount = paid,
                            status = paymentStatusFromAmount(
                                paidAmount = paid,
                                requiredAmount = required
                            ),
                            paymentMethod = paymentMethodFromString(
                                branch["paymentMethod"]?.toString()
                            ),
                            paymentDate =
                                branch["paymentDate"]
                                    ?.toString()
                                    ?.trim()
                                    .orEmpty(),
                            notes =
                                branch["notes"]
                                    ?.toString()
                                    ?.trim()
                                    .orEmpty(),
                            paymentId =
                                branch["paymentId"]
                                    ?.toString()
                                    ?.trim()
                                    .orEmpty()
                        )
                    }

            PaymentReportItem(
                traineeId = traineeId,
                fullName = fullName,
                branchName = branchName,
                phone = phone,
                requiredAmount = requiredAmount,
                paidAmount = paidAmount,
                status = status,
                paymentMethod = paymentMethod,
                paymentDate = paymentDate,
                notes = notes,
                branchPayments = branchPayments
            )
        }
        .filter { item ->
            item.fullName.isNotBlank() &&
                    item.fullName != "שם חסר" &&
                    !item.fullName.looksLikeTechnicalId()
        }
        .sortedWith(
            compareBy<PaymentReportItem> {
                it.branchName
            }
                .thenBy {
                    it.fullName
                }
        )
}

private suspend fun saveManualMembershipPaymentToFirestore(
    item: PaymentReportItem,
    amountToAdd: Double,
    method: PaymentMethod,
    notes: String,
    requestId: String
): PaymentReportItem {

    require(item.branchName.isNotBlank()) {
        "Payment branch is required."
    }

    val result =
        FirebaseFunctions
            .getInstance()
            .getHttpsCallable(
                "updateSecureMembershipPayment"
            )
            .call(
                mapOf(
                    "traineeId" to
                            item.traineeId,

                    "branchName" to
                            item.branchName.trim(),

                    "amountToAdd" to
                            amountToAdd,

                    "paymentMethod" to
                            paymentMethodToFirestore(
                                method
                            ),

                    "notes" to
                            notes.trim(),

                    "requestId" to
                            requestId
                )
            )
            .await()

    val payload =
        result.data as? Map<*, *>
            ?: error(
                "Invalid payment response"
            )

    val rawItem =
        payload["item"] as? Map<*, *>
            ?: error(
                "Missing payment item"
            )

    val returnedBranch =
        rawItem["branchName"]
            ?.toString()
            ?.trim()
            .orEmpty()

    check(
        returnedBranch.isNotBlank() &&
                returnedBranch.equals(
                    item.branchName.trim(),
                    ignoreCase = true
                )
    ) {
        "Payment response branch mismatch"
    }

    val fullName =
        rawItem["fullName"]
            ?.toString()
            ?.trim()
            .orEmpty()

    val branchName =
        rawItem["branchName"]
            ?.toString()
            ?.trim()
            .orEmpty()

    val phone =
        normalizePaymentPhone(
            rawItem["phone"]
                ?.toString()
                .orEmpty()
        )

    val requiredAmount =
        (rawItem["requiredAmount"] as? Number)
            ?.toDouble()
            ?.takeIf {
                it > 0.0
            }
            ?: item.requiredAmount

    val paidAmount =
        (rawItem["paidAmount"] as? Number)
            ?.toDouble()
            ?.coerceAtLeast(0.0)
            ?: item.paidAmount

    val status =
        runCatching {
            PaymentStatus.valueOf(
                rawItem["status"]
                    ?.toString()
                    ?.trim()
                    ?.uppercase(Locale.ROOT)
                    .orEmpty()
            )
        }
            .getOrElse {
                paymentStatusFromAmount(
                    paidAmount = paidAmount,
                    requiredAmount = requiredAmount
                )
            }

    val paymentMethod =
        paymentMethodFromString(
            rawItem["paymentMethod"]
                ?.toString()
        )

    val paymentDate =
        rawItem["paymentDate"]
            ?.toString()
            ?.trim()
            .orEmpty()

    val updatedNotes =
        rawItem["notes"]
            ?.toString()
            ?.trim()
            .orEmpty()

    /*
     * שומרים על מזהה המתאמן המקורי שהתקבל בדוח.
     *
     * מזהה מסמך התשלום שהשרת מחזיר אינו בהכרח
     * זהה למזהה המשתמש המאוחד.
     *
     * כך עדכון התשלום אינו משנה את זהות
     * הפריט ברשימת התשלומים.
     */
    return item.copy(
        traineeId =
            item.traineeId,

        fullName =
            fullName.ifBlank {
                item.fullName
            },

        branchName =
            branchName.ifBlank {
                item.branchName
            },

        phone =
            phone.ifBlank {
                item.phone
            },

        requiredAmount =
            requiredAmount,

        paidAmount =
            paidAmount,

        status =
            status,

        paymentMethod =
            paymentMethod,

        paymentDate =
            paymentDate,

        notes =
            updatedNotes
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsReportScreen(
    isEnglish: Boolean = false,
    onClose: () -> Unit = {},
    initialItems: List<PaymentReportItem> = emptyList(),
    onSaveManualPayment: (traineeId: String, amount: Double, method: PaymentMethod, notes: String) -> Unit = { _, _, _, _ -> }
) {
    var items by remember { mutableStateOf(initialItems) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var manualDialogItem by remember { mutableStateOf<PaymentReportItem?>(null) }

    var manualPaymentSessionId by remember {
        mutableStateOf(java.util.UUID.randomUUID().toString())
    }

    var isSavingManualPayment by remember { mutableStateOf(false) }
    var manualPaymentError by remember { mutableStateOf<String?>(null) }

    var isLoadingPayments by remember { mutableStateOf(true) }
    var paymentsError by remember { mutableStateOf<String?>(null) }
    val screenScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        isLoadingPayments = true
        paymentsError = null

        runCatching {
            loadRealPaymentsReportItems()
        }.onSuccess { realItems ->
            items = realItems
            isLoadingPayments = false
        }.onFailure { error ->
            android.util.Log.e(
                "KMI_PAYMENTS",
                "Failed loading secure payments report",
                error
            )

            val rawMessage = error.localizedMessage.orEmpty()

            paymentsError =
                if (
                    rawMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
                    rawMessage.contains("permission-denied", ignoreCase = true) ||
                    rawMessage.contains("insufficient permissions", ignoreCase = true)
                ) {
                    if (isEnglish) {
                        "You do not have permission to view this payments report."
                    } else {
                        "אין לך הרשאה לצפות בדוח התשלומים."
                    }
                } else {
                    if (isEnglish) {
                        "Unable to load payments. Please check your connection and try again."
                    } else {
                        "לא ניתן לטעון את התשלומים. יש לבדוק את החיבור ולנסות שוב."
                    }
                }

            isLoadingPayments = false
        }
    }

    val title =
        if (isEnglish) {
            "Payments Report"
        } else {
            "דו\"ח תשלומים"
        }

    val paidText =
        if (isEnglish) {
            "Paid"
        } else {
            "שילמו"
        }

    val unpaidText =
        if (isEnglish) {
            "Not paid"
        } else {
            "לא שילמו"
        }

    val reportPanelColor =
        MaterialTheme.colorScheme.surface
    val reportTitleColor = MaterialTheme.colorScheme.onSurface
    val reportSecondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val reportAccentTextColor = MaterialTheme.colorScheme.primary

    val allBranchesLabel = if (isEnglish) "All Branches" else "כל הסניפים"

    val branchOptions = remember(isEnglish, items) {
        val realBranches =
            items
                .flatMap { item ->
                    if (item.branchPayments.isNotEmpty()) {
                        item.branchPayments.map { payment ->
                            payment.branchName.trim()
                        }
                    } else {
                        listOf(item.branchName.trim())
                    }
                }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

        listOf(allBranchesLabel) + realBranches
    }

    var selectedBranch by rememberSaveable {
        mutableStateOf(branchOptions.first())
    }

    LaunchedEffect(branchOptions, allBranchesLabel) {
        if (selectedBranch !in branchOptions) {
            selectedBranch = allBranchesLabel
        }
    }

    val activePaymentBranch =
        selectedBranch.takeUnless {
            it == allBranchesLabel
        }

    val filteredItems = items.filter { item ->
        val branchRows =
            item.paymentRowsForBranch(activePaymentBranch)

        val matchesQuery =
            query.isBlank() ||
                    item.fullName.contains(query, ignoreCase = true) ||
                    item.phone.contains(query, ignoreCase = true) ||
                    branchRows.any { row ->
                        row.branchName.contains(
                            query,
                            ignoreCase = true
                        )
                    }

        val matchesFilter =
            branchRows.any { row ->
                when (filter) {
                    "PAID" ->
                        row.paidAmount >= row.requiredAmount

                    "UNPAID" ->
                        row.paidAmount < row.requiredAmount

                    else -> true
                }
            }

        branchRows.isNotEmpty() &&
                matchesQuery &&
                matchesFilter
    }

    /*
     * רשומות כספיות לפי סניף, לאחר סינון.
     * המתאמן עצמו נשאר רשומה אחת ברשימה.
     */
    val reportPaymentRows =
        filteredItems
            .flatMap { item ->
                item.paymentRowsForBranch(activePaymentBranch)
            }
            .filter { row ->
                when (filter) {
                    "PAID" ->
                        row.paidAmount >= row.requiredAmount

                    "UNPAID" ->
                        row.paidAmount < row.requiredAmount

                    else -> true
                }
            }

    /*
     * רשומת מתאמן אחת בתצוגה,
     * עם פירוט הסניפים שעומדים בסינון בלבד.
     *
     * items המקורי נשאר ללא שינוי,
     * ולכן דיאלוג התשלום יכול להציע את כל
     * הסניפים שהשרת החזיר למתאמן.
     */
    val visiblePaymentItems =
        filteredItems.map { item ->
            if (item.branchPayments.isEmpty()) {
                item
            } else {
                val visibleBranches =
                    item.branchPayments.filter { payment ->
                        val matchesBranch =
                            activePaymentBranch == null ||
                                    payment.branchName == activePaymentBranch

                        val matchesStatus =
                            when (filter) {
                                "PAID" ->
                                    payment.paidAmount >= payment.requiredAmount

                                "UNPAID" ->
                                    payment.paidAmount < payment.requiredAmount

                                else -> true
                            }

                        matchesBranch && matchesStatus
                    }

                item.copy(
                    branchName =
                        visibleBranches.firstOrNull()?.branchName
                            ?: item.branchName,
                    branchPayments = visibleBranches
                )
            }
        }

    val totalRequired =
        reportPaymentRows.sumOf { it.requiredAmount }

    val totalPaid =
        reportPaymentRows.sumOf { it.paidAmount }

    val paidCount =
        reportPaymentRows.count {
            it.paidAmount >= it.requiredAmount
        }

    val unpaidCount =
        reportPaymentRows.count {
            it.paidAmount < it.requiredAmount
        }

    val collectionPercent =
        if (totalRequired > 0.0) {
            ((totalPaid / totalRequired) * 100.0)
                .coerceIn(0.0, 100.0)
        } else {
            0.0
        }

    KmiLanguageDirection(
        isEnglish = isEnglish
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                KmiTopBar(
                    title = title,
                    onHome = onClose,
                    showTopHome = false,
                    showTopShare = true,
                    lockSearch = false,
                    showBottomActions = true,
                    currentLang = if (isEnglish) "en" else "he",
                    onShare = {
                        if (reportPaymentRows.isNotEmpty()) {
                            val pdfFile = createPaymentsReportPdf(
                                context = context,
                                items = reportPaymentRows,
                                totalRequired = totalRequired,
                                totalPaid = totalPaid,
                                paidCount = paidCount,
                                unpaidCount = unpaidCount,
                                collectionPercent = collectionPercent,
                                selectedBranch = selectedBranch,
                                isEnglish = isEnglish
                            )

                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                pdfFile
                            )

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    if (isEnglish) "Payments report" else "דו״ח תשלומים"
                                )
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                            context.startActivity(
                                Intent.createChooser(
                                    sendIntent,
                                    if (isEnglish) "Share PDF" else "שיתוף PDF"
                                )
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = kmiScreenBackgroundBrush()
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .navigationBarsPadding()
                ) {

                    // =========================================================
                    // כותרת משנה כחולה קבועה — לא חלק מהגלילה
                    // =========================================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .kmiSectionHeaderBackground()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .padding(
                                    start = 16.dp,
                                    top = 2.dp,
                                    end = 16.dp,
                                    bottom = 5.dp
                                ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.Center
                        ) {
                            Text(
                                text =
                                    if (isEnglish) {
                                        "Premium payments dashboard"
                                    } else {
                                        "דשבורד תשלומים פרימיום"
                                    },
                                style =
                                    KmiTypography.secondary.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(1.dp))

                            Text(
                                text =
                                    if (isEnglish) {
                                        "For trainees, coaches and managers"
                                    } else {
                                        "למתאמנים, למאמנים ולמנהלים"
                                    },
                                style = KmiTypography.caption,
                                color =
                                    Color.White.copy(
                                        alpha = 0.92f
                                    ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // =========================================================
                    // רק התוכן שמתחת לכותרת המשנה נגלל
                    // =========================================================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {

                        Spacer(Modifier.height(14.dp))

                        Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(30.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    reportPanelColor
                            ),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement =
                                    Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Collected ₪${"%.0f".format(totalPaid)} of ₪${
                                                "%.0f".format(
                                                    totalRequired
                                                )
                                            }"
                                        } else {
                                            "נגבה ₪${"%.0f".format(totalPaid)} מתוך ₪${
                                                "%.0f".format(
                                                    totalRequired
                                                )
                                            }"
                                        },
                                    style =
                                        KmiTypography.secondary,
                                    color =
                                        reportAccentTextColor,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SummaryCard(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .heightIn(
                                                min = 96.dp
                                            ),
                                    title = if (isEnglish) "Collection" else "אחוז גבייה",
                                    value = "${"%.0f".format(collectionPercent)}%",
                                    icon =
                                        Icons.AutoMirrored.Filled.TrendingUp,
                                    selected = false,
                                    baseColor = Color(0xFF1DA1F2),
                                    selectedColor = Color(0xFF0284C7),
                                    onClick = {}
                                )

                                SummaryCard(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .heightIn(
                                                min = 96.dp
                                            ),
                                    title = if (isEnglish) "Trainees" else "מתאמנים",
                                    value = filteredItems.size.toString(),
                                    icon = Icons.Default.Groups,
                                    selected = filter == "ALL",
                                    baseColor = Color(0xFF8B5CF6),
                                    selectedColor = Color(0xFF7C3AED),
                                    onClick = {
                                        filter = "ALL"
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SummaryCard(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .heightIn(
                                                min = 96.dp
                                            ),
                                    title = unpaidText,
                                    value = unpaidCount.toString(),
                                    icon = Icons.Default.PersonOff,
                                    selected = filter == "UNPAID",
                                    baseColor = Color(0xFFFF7A59),
                                    selectedColor = Color(0xFFFF5A36),
                                    onClick = {
                                        filter = "UNPAID"
                                    }
                                )

                                SummaryCard(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .heightIn(
                                                min = 96.dp
                                            ),
                                    title = paidText,
                                    value = paidCount.toString(),
                                    icon = Icons.Default.Paid,
                                    selected = filter == "PAID",
                                    baseColor = Color(0xFF22C55E),
                                    selectedColor = Color(0xFF16A34A),
                                    onClick = {
                                        filter = "PAID"
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = reportPanelColor
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Search & filters" else "חיפוש וסינון",
                                color = reportTitleColor,
                                style =
                                    KmiTypography.cardTitle,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                            )

                            BranchDropdown(
                                isEnglish = isEnglish,
                                selectedBranch = selectedBranch,
                                branchOptions = branchOptions,
                                onBranchSelected = { selectedBranch = it }
                            )

                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(
                                            min = 52.dp
                                        ),
                                singleLine = true,
                                label = {
                                    Text(
                                        text =
                                            if (isEnglish) {
                                                "Search by name / phone / branch"
                                            } else {
                                                "חיפוש לפי שם / טלפון / סניף"
                                            },
                                        style =
                                            KmiTypography.caption,
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(KmiIconSize.small)
                                    )
                                },
                                textStyle =
                                    KmiTypography.secondary.copy(
                                        textAlign =
                                            if (isEnglish) {
                                                TextAlign.Start
                                            } else {
                                                TextAlign.End
                                            },
                                        color = reportTitleColor
                                    ),
                                colors = reportFieldColors()
                            )

                            FilterRow(
                                isEnglish = isEnglish,
                                selected = filter,
                                onSelect = { filter = it }
                            )

                            Text(
                                text = if (isEnglish)
                                    "Results: ${filteredItems.size}"
                                else
                                    "תוצאות: ${filteredItems.size}",
                                color = reportSecondaryTextColor,
                                style =
                                    KmiTypography.caption,
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when {
                            isLoadingPayments -> {
                                Card(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    shape =
                                        RoundedCornerShape(24.dp),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor =
                                                reportPanelColor
                                        ),
                                    elevation =
                                        CardDefaults.cardElevation(
                                            defaultElevation = 0.dp
                                        )
                                ) {
                                    PaymentsPremiumLoading(
                                        text =
                                            if (isEnglish) {
                                                "Loading payment data..."
                                            } else {
                                                "טוען נתוני תשלום..."
                                            },
                                        modifier =
                                            Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            )
                                    )
                                }
                            }

                            paymentsError != null -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = if (isEnglish) {
                                            "Failed loading payments: $paymentsError"
                                        } else {
                                            "טעינת התשלומים נכשלה: $paymentsError"
                                        },
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onErrorContainer,
                                        style =
                                            KmiTypography.body.copy(
                                                fontWeight =
                                                    FontWeight.Bold
                                            ),
                                        modifier =
                                            Modifier.padding(16.dp),
                                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                                    )
                                }
                            }

                            filteredItems.isEmpty() -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = reportPanelColor
                                    )
                                ) {
                                    Text(
                                        text = if (isEnglish) {
                                            "No trainees matched the current filters."
                                        } else {
                                            "לא נמצאו מתאמנים בהתאם לסינון הנוכחי."
                                        },
                                        color = reportTitleColor,
                                        style =
                                            KmiTypography.body.copy(
                                                fontWeight =
                                                    FontWeight.Bold
                                            ),
                                        modifier =
                                            Modifier.padding(16.dp),
                                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                                    )
                                }
                            }

                            else -> {
                                visiblePaymentItems.forEachIndexed { index, item ->

                                    PaymentReportRow(
                                        item = item,
                                        demoIndex = index + 1,
                                        isEnglish = isEnglish,
                                        onManualUpdate = {
                                            if (!isSavingManualPayment) {
                                                manualPaymentSessionId =
                                                    java.util.UUID.randomUUID().toString()

                                                manualPaymentError = null

                                                // הדיאלוג מקבל את כל הסניפים,
                                                // גם כאשר הכרטיס מסונן.
                                                manualDialogItem =
                                                    filteredItems[index]
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                    }
                }

                manualDialogItem?.let { selected ->
                    val selectedDemoIndex =
                        filteredItems
                            .indexOfFirst { item ->
                                item.traineeId ==
                                        selected.traineeId
                            }
                            .takeIf { index ->
                                index >= 0
                            }
                            ?.plus(1)

                    ManualPaymentDialog(
                        isEnglish = isEnglish,
                        item = selected,
                        demoIndex = selectedDemoIndex,
                        paymentSessionId = manualPaymentSessionId,
                        isSaving = isSavingManualPayment,
                        paymentError = manualPaymentError,
                        onDismiss = {
                            if (!isSavingManualPayment) {
                                manualDialogItem = null
                                manualPaymentError = null
                            }
                        },
                        onSave = { branch, amount, method, notes, requestId ->
                            if (!isSavingManualPayment) {
                                isSavingManualPayment = true
                                manualPaymentError = null

                                screenScope.launch {
                                    runCatching {
                                    val selectedBranchPayment =
                                        selected.branchPayments.firstOrNull {
                                            it.branchName == branch
                                        }

                                    val paymentItem =
                                        selected.copy(
                                            branchName = branch,
                                            requiredAmount =
                                                selectedBranchPayment
                                                    ?.requiredAmount
                                                    ?: selected.requiredAmount,
                                            paidAmount =
                                                selectedBranchPayment
                                                    ?.paidAmount
                                                    ?: selected.paidAmount
                                        )

                                        saveManualMembershipPaymentToFirestore(
                                            item = paymentItem,
                                            amountToAdd = amount,
                                            method = method,
                                            notes = notes,
                                            requestId = requestId
                                        )
                                }.onSuccess { updatedItem ->

                                    items = items.map { current ->

                                        if (current.traineeId != selected.traineeId) {
                                            current
                                        } else {

                                            val updatedBranchPayment =
                                                PaymentBranchItem(
                                                    branchName = branch,
                                                    requiredAmount = updatedItem.requiredAmount,
                                                    paidAmount = updatedItem.paidAmount,
                                                    status = updatedItem.status,
                                                    paymentMethod = updatedItem.paymentMethod,
                                                    paymentDate = updatedItem.paymentDate,
                                                    notes = updatedItem.notes
                                                )

                                            val previousBranches =
                                                current.branchPayments

                                            val branchExists =
                                                previousBranches.any {
                                                    it.branchName == branch
                                                }

                                            val updatedBranches =
                                                if (previousBranches.isEmpty()) {
                                                    listOf(updatedBranchPayment)
                                                } else {
                                                    previousBranches.map { existing ->
                                                        if (existing.branchName == branch) {
                                                            updatedBranchPayment.copy(
                                                                paymentId = existing.paymentId
                                                            )
                                                        } else {
                                                            existing
                                                        }
                                                    } + if (branchExists) {
                                                        emptyList()
                                                    } else {
                                                        listOf(updatedBranchPayment)
                                                    }
                                                }

                                            if (
                                                previousBranches.isEmpty() ||
                                                current.branchName == branch
                                            ) {
                                                updatedItem.copy(
                                                    branchPayments = updatedBranches
                                                )
                                            } else {
                                                current.copy(
                                                    branchPayments = updatedBranches
                                                )
                                            }
                                        }
                                    }

                                        /*
                                         * השמירה בשרת כבר הצליחה.
                                         * כשל ב-callback המקומי אינו
                                         * הופך את התשלום לכישלון.
                                         */
                                        runCatching {
                                            onSaveManualPayment(
                                                selected.traineeId,
                                                amount,
                                                method,
                                                notes
                                            )
                                        }.onFailure {
                                            android.util.Log.w(
                                                "KMI_PAYMENTS",
                                                "Payment callback failed after server confirmation"
                                            )
                                        }

                                        manualDialogItem = null
                                        manualPaymentError = null
                                    }.onFailure {
                                        manualPaymentError =
                                            if (isEnglish) {
                                                "Payment could not be confirmed. Retry the same operation, or verify its status before starting a new payment."
                                            } else {
                                                "לא ניתן לאשר את שמירת התשלום. אפשר לנסות שוב את אותה פעולה, או לבדוק את מצבה לפני יצירת תשלום חדש."
                                            }
                                    }

                                    isSavingManualPayment = false
                                }
                            }
                        }
                    )
                }
            }
        } // סוף Scaffold
    } // סוף KmiLanguageDirection
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    baseColor: Color,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) selectedColor else baseColor
        ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = if (selected) 0.22f else 0.14f)
            ) {
                Box(
                    modifier = Modifier.padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(KmiIconSize.small)
                    )
                }
            }

            Text(
                text = title,
                color = Color.White.copy(alpha = 0.90f),
                style =
                    KmiTypography.caption,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = value,
                color = Color.White,
                style =
                    KmiTypography.metric,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
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
            modifier = Modifier.weight(1f),
            text = if (isEnglish) "All\ntrainees" else "כל\nהמתאמנים",
            selected = selected == "ALL",
            onClick = { onSelect("ALL") }
        )

        FilterChipSimple(
            modifier = Modifier.weight(1f),
            text = if (isEnglish) "Paid" else "שילמו",
            selected = selected == "PAID",
            onClick = { onSelect("PAID") }
        )

        FilterChipSimple(
            modifier = Modifier.weight(1f),
            text = if (isEnglish) "Not\npaid" else "לא\nשילמו",
            selected = selected == "UNPAID",
            onClick = { onSelect("UNPAID") }
        )
    }
}

@Composable
private fun BranchDropdown(
    isEnglish: Boolean,
    selectedBranch: String,
    branchOptions: List<String>,
    onBranchSelected: (String) -> Unit
) {
    KmiPremiumDropdown(
        title =
            if (isEnglish) {
                "Branch"
            } else {
                "סניף"
            },
        options =
            branchOptions
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct(),
        selectedValue = selectedBranch.trim(),
        isEnglish = isEnglish,
        placeholder =
            if (isEnglish) {
                "Select branch"
            } else {
                "בחר סניף"
            },
        enabled = branchOptions.size > 1,
        onSelected = onBranchSelected
    )
}

@Composable
private fun FilterChipSimple(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier =
            modifier.heightIn(
                min = 44.dp
            ),
        shape = RoundedCornerShape(16.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
            )
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 6.dp,
                        vertical = 4.dp
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = text,
                color =
                    if (selected) {
                        Color.White
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    },
                style =
                    KmiTypography.caption,
                fontWeight =
                    if (selected) {
                        FontWeight.ExtraBold
                    } else {
                        FontWeight.Bold
                    },
                textAlign =
                    TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PaymentReportRow(
    item: PaymentReportItem,
    demoIndex: Int,
    isEnglish: Boolean,
    onManualUpdate: () -> Unit
) {
    val cardBackgroundColor =
        MaterialTheme.colorScheme.surfaceVariant

    val cardPrimaryTextColor =
        MaterialTheme.colorScheme.onSurface

    val cardSecondaryTextColor =
        MaterialTheme.colorScheme.onSurfaceVariant

    val cardDividerColor =
        MaterialTheme.colorScheme.outlineVariant
            .copy(alpha = 0.72f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    cardBackgroundColor
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text =
                            item.demoSafeName(
                                isEnglish = isEnglish,
                                demoIndex = demoIndex
                            ),
                        color = cardPrimaryTextColor,
                        style =
                            KmiTypography.cardTitle,
                        fontWeight =
                            FontWeight.ExtraBold,
                        textAlign =
                            TextAlign.Start,
                        modifier =
                            Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Spacer(
                        Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            listOf(
                                item.branchName,
                                item.demoSafePhone(
                                    isEnglish = isEnglish,
                                    demoIndex = demoIndex
                                )
                            )
                                .filter {
                                    it.isNotBlank()
                                }
                                .joinToString(" • "),
                        color =
                            cardSecondaryTextColor,
                        style =
                            KmiTypography.caption,
                        textAlign =
                            TextAlign.Start,
                        modifier =
                            Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                Spacer(
                    Modifier.width(8.dp)
                )

                if (item.branchPayments.size <= 1) {
                    val displayedStatus =
                        item.branchPayments.firstOrNull()
                            ?.status
                            ?: item.status

                    Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = statusColor(displayedStatus)
                                .copy(alpha = 0.18f),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = statusLabel(
                                    displayedStatus,
                                    isEnglish
                                ),
                                color = statusColor(displayedStatus),
                                modifier = Modifier.padding(
                                    horizontal = 9.dp,
                                    vertical = 5.dp
                                ),
                                style = KmiTypography.caption,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                    }
                }
            }

            HorizontalDivider(
                color =
                    cardDividerColor
            )

            if (item.branchPayments.isNotEmpty()) {
                item.branchPayments.forEach { branchPayment ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = branchPayment.branchName,
                                color = cardPrimaryTextColor,
                                style = KmiTypography.secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = statusLabel(
                                    branchPayment.status,
                                    isEnglish
                                ),
                                color = statusColor(branchPayment.status),
                                style = KmiTypography.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text =
                                if (isEnglish) {
                                    "Fee: ₪${"%.0f".format(branchPayment.paidAmount)} / ₪${"%.0f".format(branchPayment.requiredAmount)}"
                                } else {
                                    "דמי חבר: ₪${"%.0f".format(branchPayment.paidAmount)} / ₪${"%.0f".format(branchPayment.requiredAmount)}"
                                },
                            color = cardSecondaryTextColor,
                            style = KmiTypography.secondary,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Text(
                    text =
                        if (isEnglish) {
                            "Fee: ₪${"%.0f".format(item.paidAmount)} / ₪${"%.0f".format(item.requiredAmount)}"
                        } else {
                            "דמי חבר: ₪${"%.0f".format(item.paidAmount)} / ₪${"%.0f".format(item.requiredAmount)}"
                        },
                    color = cardPrimaryTextColor,
                    style = KmiTypography.secondary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (item.paymentDate.isNotBlank() && item.branchPayments.isEmpty()) {
                Text(
                    text =
                        if (isEnglish) {
                            "Last update: ${item.paymentDate}"
                        } else {
                            "עדכון אחרון: ${item.paymentDate}"
                        },
                    color =
                        cardSecondaryTextColor,
                    style =
                        KmiTypography.caption,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onManualUpdate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary
                ),
                elevation =
                    ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.AddCard,
                    contentDescription = null,
                    modifier = Modifier.size(KmiIconSize.medium)
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                Text(
                    text =
                        if (isEnglish) {
                            "Add Membership Payment"
                        } else {
                            "הוסף דמי חבר"
                        },
                    style = KmiTypography.action,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
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
    demoIndex: Int?,
    paymentSessionId: String,
    isSaving: Boolean,
    paymentError: String?,
    onDismiss: () -> Unit,
    onSave: (
        branchName: String,
        amount: Double,
        method: PaymentMethod,
        notes: String,
        requestId: String
    ) -> Unit
) {
    val requestId = paymentSessionId

    var paymentAttempted by rememberSaveable(paymentSessionId) {
        mutableStateOf(false)
    }

    val fieldsEnabled = !isSaving && !paymentAttempted
    val canDismissPayment = !isSaving && !paymentAttempted

    var amountText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable {
        mutableStateOf("")
    }

    var method by remember {
        mutableStateOf(PaymentMethod.MANUAL)
    }

    val paymentBranches =
        if (item.branchPayments.isNotEmpty()) {
            item.branchPayments
                .map { it.branchName.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        } else {
            listOf(item.branchName.trim())
                .filter { it.isNotBlank() }
        }

    var selectedPaymentBranch by remember(item.traineeId, paymentBranches) {
        mutableStateOf(
            paymentBranches.firstOrNull().orEmpty()
        )
    }

    val paymentMethodOptions =
        remember(isEnglish) {
            PaymentMethod.entries.map { option ->
                paymentMethodLabel(
                    option,
                    isEnglish
                )
            }
        }

    AlertDialog(
        onDismissRequest = {
            if (canDismissPayment) {
                onDismiss()
            }
        },
        title = {
            Text(
                text =
                    if (isEnglish) {
                        "Manual Payment Update"
                    } else {
                        "עדכון תשלום ידני"
                    },
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface,
                style =
                    KmiTypography.sectionTitle,
                textAlign =
                    if (isEnglish) {
                        TextAlign.Start
                    } else {
                        TextAlign.End
                    },
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text =
                        item.demoSafeName(
                            isEnglish = isEnglish,
                            demoIndex = demoIndex
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,
                    style =
                        KmiTypography.cardTitle.copy(
                            fontWeight =
                                FontWeight.ExtraBold
                        ),
                    textAlign =
                        if (isEnglish) {
                            TextAlign.Start
                        } else {
                            TextAlign.End
                        },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                KmiPremiumDropdown(
                    title =
                        if (isEnglish) "Payment branch" else "סניף לתשלום",
                    options = paymentBranches,
                    selectedValue = selectedPaymentBranch,
                    isEnglish = isEnglish,
                    placeholder =
                        if (isEnglish) "Select branch" else "בחר סניף",
                    enabled = fieldsEnabled && paymentBranches.size > 1,
                    onSelected = { branch ->
                        selectedPaymentBranch = branch
                    }
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    enabled = fieldsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isEnglish) "Amount" else "סכום") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle =
                        KmiTypography.body.copy(
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Start
                                } else {
                                    TextAlign.End
                                }
                        ),
                    colors = reportFieldColors()
                )

                KmiPremiumDropdown(
                    title =
                        if (isEnglish) {
                            "Payment method"
                        } else {
                            "אמצעי תשלום"
                        },
                    options = paymentMethodOptions,
                    selectedValue =
                        paymentMethodLabel(
                            method,
                            isEnglish
                        ),
                    isEnglish = isEnglish,
                    placeholder =
                        if (isEnglish) {
                            "Select payment method"
                        } else {
                            "בחר אמצעי תשלום"
                        },
                    enabled =
                        fieldsEnabled && paymentMethodOptions.size > 1,
                    onSelected = { selectedLabel ->
                        PaymentMethod.entries
                            .firstOrNull { option ->
                                paymentMethodLabel(
                                    option,
                                    isEnglish
                                ) == selectedLabel
                            }
                            ?.let { selectedMethod ->
                                method = selectedMethod
                            }
                    }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    enabled = fieldsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text(if (isEnglish) "Notes" else "הערות") },
                    textStyle =
                        KmiTypography.body.copy(
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Start
                                } else {
                                    TextAlign.End
                                }
                        ),
                    colors = reportFieldColors()
                )

                if (paymentError != null) {
                    Text(
                        text = paymentError,
                        color = MaterialTheme.colorScheme.error,
                        style = KmiTypography.caption,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                if (paymentAttempted && !isSaving) {
                    Text(
                        text =
                            if (isEnglish) {
                                "This payment attempt is locked. Retry the same operation without changing its details."
                            } else {
                                "ניסיון התשלום נעול. יש לנסות שוב את אותה פעולה ללא שינוי בפרטי התשלום."
                            },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = KmiTypography.caption,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    val amount = amountText.toDoubleOrNull()

                    val validAmount =
                        amount != null &&
                                amount.isFinite() &&
                                amount > 0.0 &&
                                Regex("""^\d+(\.\d{1,2})?$""")
                                    .matches(amountText)

                    if (
                        validAmount &&
                        selectedPaymentBranch.isNotBlank()
                    ) {
                        paymentAttempted = true

                        onSave(
                            selectedPaymentBranch,
                            amount ?: return@TextButton,
                            method,
                            notes.trim(),
                            requestId
                        )
                    }
                }
            ) {
                Text(
                    text =
                        if (isSaving) {
                            if (isEnglish) "Saving..." else "שומר..."
                        } else if (paymentAttempted) {
                            if (isEnglish) "Retry" else "נסה שוב"
                        } else {
                            if (isEnglish) "Save" else "שמור"
                        },
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    style = KmiTypography.action
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = canDismissPayment,
                onClick = onDismiss
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            "Cancel"
                        } else {
                            "ביטול"
                        },
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    style = KmiTypography.action
                )
            }
        },
        containerColor =
            MaterialTheme
                .colorScheme
                .surface
    )
}

@Composable
private fun reportFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,

    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,

    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),

    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),

    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),

    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),

    cursorColor = MaterialTheme.colorScheme.primary
)

private fun statusLabel(
    status: PaymentStatus,
    isEnglish: Boolean
): String = when (status) {
    PaymentStatus.PAID -> if (isEnglish) "Paid" else "שולם"
    PaymentStatus.UNPAID -> if (isEnglish) "Unpaid" else "לא שולם"
    PaymentStatus.PARTIAL -> if (isEnglish) "Partial" else "שולם חלקית"
}

private fun statusColor(status: PaymentStatus): Color = when (status) {
    PaymentStatus.PAID -> Color(0xFF66D17A)
    PaymentStatus.UNPAID -> Color(0xFFFF7A7A)
    PaymentStatus.PARTIAL -> Color(0xFFFFC857)
}

private fun paymentMethodLabel(
    method: PaymentMethod,
    isEnglish: Boolean
): String {
    return when (method.name.uppercase(Locale.ROOT)) {
        "CASH" -> if (isEnglish) "Cash" else "מזומן"
        "CREDIT_CARD" -> if (isEnglish) "Credit card" else "כרטיס אשראי"
        "BANK_TRANSFER" -> if (isEnglish) "Bank transfer" else "העברה בנקאית"
        "BIT" -> if (isEnglish) "bit" else "ביט"
        "WEBSITE" -> if (isEnglish) "Website payment" else "תשלום באתר"
        "MANUAL" -> if (isEnglish) "Manual" else "ידני"
        else -> method.name
            .lowercase(Locale.ROOT)
            .replace("_", " ")
            .replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}

private fun createPaymentsReportPdf(
    context: Context,
    items: List<PaymentReportItem>,
    totalRequired: Double,
    totalPaid: Double,
    paidCount: Int,
    unpaidCount: Int,
    collectionPercent: Double,
    selectedBranch: String,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36f

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val document = PdfDocument()

    val textDark = android.graphics.Color.rgb(15, 23, 42)

    val regularTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    val boldTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regularTypeface
            textSize = 10.5f
            color = textDark
            textAlign =
                KmiPdfDirection.textAlign(
                    isEnglish = isEnglish
                )
        }

    val sectionPaint = Paint(textPaint).apply {
        typeface = boldTypeface
        textSize = 15f
        color = textDark
    }

    val headerPaint = Paint(textPaint).apply {
        typeface = boldTypeface
        textSize = 9.8f
        color = android.graphics.Color.WHITE
    }

    /*
     * העמוד הראשון מכיל גם את כרטיסי הסיכום ולכן נכנסות בו
     * עד 14 שורות. בכל עמוד המשך נכנסות עד 18 שורות.
     */
    val firstPageRows = 14
    val continuationPageRows = 18

    val totalPages =
        if (items.size <= firstPageRows) {
            1
        } else {
            1 +
                    kotlin.math.ceil(
                        (items.size - firstPageRows) /
                                continuationPageRows.toDouble()
                    ).toInt()
        }

    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    )
    var canvas = page.canvas
    var y = KmiPdfHeader.CONTENT_TOP

    fun textX(): Float =
        KmiPdfDirection.startX(
            isEnglish = isEnglish,
            left = margin,
            right = pageWidth.toFloat() - margin
        )

    fun columnXs(
        vararg paddings: Float
    ): List<Float> =
        paddings.map { padding ->
            KmiPdfDirection.startPaddingX(
                isEnglish = isEnglish,
                left = margin,
                right = pageWidth.toFloat() - margin,
                padding = padding
            )
        }

    fun statusPdfColor(status: PaymentStatus): Int {
        return when (status) {
            PaymentStatus.PAID -> android.graphics.Color.rgb(22, 163, 74)
            PaymentStatus.UNPAID -> android.graphics.Color.rgb(220, 38, 38)
            PaymentStatus.PARTIAL -> android.graphics.Color.rgb(245, 158, 11)
        }
    }

    fun drawHeader() {
        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "דו״ח תשלומים",
            titleEnglish = "Payments Report",
            subtitleHebrew =
                "סניף: ${
                    selectedBranch.ifBlank {
                        "כל הסניפים"
                    }
                }",
            subtitleEnglish =
                "Branch: ${
                    selectedBranch.ifBlank {
                        "All branches"
                    }
                }",
            generatedDate = paymentNowDateText()
        )

        y = KmiPdfHeader.CONTENT_TOP
    }

    fun drawFooter() {
        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = pageNumber,
            totalPages = totalPages,
            isEnglish = isEnglish
        )
    }

    fun newPage() {
        drawFooter()
        document.finishPage(page)

        pageNumber++
        page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        canvas = page.canvas
        y = KmiPdfHeader.CONTENT_TOP

        drawHeader()
    }

    fun ensureSpace(height: Float) {
        if (
            y + height >
            pageHeight - KmiPdfFooter.CONTENT_BOTTOM_PADDING
        ) {
            newPage()
        }
    }

    fun drawSummaryTile(index: Int, label: String, value: String) {
        val gap = 8f
        val tileWidth = ((pageWidth - margin * 2f) - gap * 3f) / 4f
        val left = if (isEnglish) {
            margin + index * (tileWidth + gap)
        } else {
            pageWidth - margin - tileWidth - index * (tileWidth + gap)
        }

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(248, 251, 255)
        }

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(214, 226, 241)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawRoundRect(left, y, left + tileWidth, y + 58f, 14f, 14f, bg)
        canvas.drawRoundRect(left, y, left + tileWidth, y + 58f, 14f, 14f, border)

        val valuePaint = Paint(textPaint).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 16f
            color = android.graphics.Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }

        val labelPaint = Paint(textPaint).apply {
            textSize = 8.8f
            color = android.graphics.Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(value, left + tileWidth / 2f, y + 25f, valuePaint)
        canvas.drawText(label, left + tileWidth / 2f, y + 43f, labelPaint)
    }

    fun drawTableHeader() {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(2, 43, 74)
        }

        canvas.drawRoundRect(
            margin,
            y,
            pageWidth - margin,
            y + 30f,
            10f,
            10f,
            bg
        )

        headerPaint.textAlign =
            KmiPdfDirection.textAlign(
                isEnglish = isEnglish
            )

        val cols =
            columnXs(
                14f,
                168f,
                250f,
                324f,
                394f,
                462f
            )

        listOf(
            tr("שם", "Name"),
            tr("סניף", "Branch"),
            tr("נדרש", "Required"),
            tr("שולם", "Paid"),
            tr("סטטוס", "Status"),
            tr("טלפון", "Phone")
        ).forEachIndexed { index, title ->
            canvas.drawText(title, cols[index], y + 20f, headerPaint)
        }

        y += 42f
    }

    fun drawPaymentRow(
        index: Int,
        item: PaymentReportItem,
        demoIndex: Int
    ) {
        ensureSpace(38f)

        val rowBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (index % 2 == 0) {
                android.graphics.Color.rgb(248, 251, 255)
            } else {
                android.graphics.Color.rgb(234, 244, 255)
            }
        }

        canvas.drawRoundRect(
            margin,
            y - 18f,
            pageWidth - margin,
            y + 12f,
            8f,
            8f,
            rowBg
        )

        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusPdfColor(item.status)
        }

        val dotX =
            KmiPdfDirection.startPaddingX(
                isEnglish = isEnglish,
                left = margin,
                right = pageWidth.toFloat() - margin,
                padding = 7f
            )

        canvas.drawCircle(
            dotX,
            y - 3f,
            4f,
            dot
        )

        textPaint.textAlign =
            KmiPdfDirection.textAlign(
                isEnglish = isEnglish
            )
        textPaint.color =
            android.graphics.Color.rgb(
                15,
                23,
                42
            )
        textPaint.typeface = regularTypeface
        textPaint.textSize = 10f

        val cols =
            columnXs(
                16f,
                168f,
                250f,
                324f,
                394f,
                462f
            )

        val values = listOf(
            item
                .demoSafeName(
                    isEnglish = isEnglish,
                    demoIndex = demoIndex
                )
                .take(22),
            item.branchName
                .ifBlank { "—" }
                .take(12),
            "₪${"%.0f".format(item.requiredAmount)}",
            "₪${"%.0f".format(item.paidAmount)}",
            statusLabel(
                item.status,
                isEnglish
            ).take(12),
            item.demoSafePhone(
                isEnglish = isEnglish,
                demoIndex = demoIndex
            )
                .ifBlank { "—" }
                .take(13)
        )

        values.forEachIndexed { colIndex, value ->
            canvas.drawText(value, cols[colIndex], y, textPaint)
        }

        y += 34f
    }

    drawHeader()

    sectionPaint.textAlign =
        KmiPdfDirection.textAlign(
            isEnglish = isEnglish
        )
    canvas.drawText(
        tr("סיכום גבייה", "Collection Summary"),
        textX(),
        y,
        sectionPaint
    )

    y += 14f

    drawSummaryTile(
        index = 0,
        label = tr(
            "גבייה",
            "Collection"
        ),
        value =
            "${"%.0f".format(collectionPercent)}%"
    )

    drawSummaryTile(
        index = 1,
        label = tr(
            "נגבה / נדרש",
            "Paid / Required"
        ),
        value =
            "₪${"%.0f".format(totalPaid)}/" +
                    "₪${"%.0f".format(totalRequired)}"
    )

    drawSummaryTile(
        index = 2,
        label = tr(
            "שילמו",
            "Paid"
        ),
        value = paidCount.toString()
    )

    drawSummaryTile(
        index = 3,
        label = tr(
            "לא שילמו",
            "Unpaid"
        ),
        value = unpaidCount.toString()
    )

    y += 82f

    sectionPaint.textAlign =
        KmiPdfDirection.textAlign(
            isEnglish = isEnglish
        )
    canvas.drawText(
        tr("פירוט מתאמנים", "Trainee Details"),
        textX(),
        y,
        sectionPaint
    )

    y += 16f
    drawTableHeader()

    val demoIndexByTrainee =
        items
            .map { it.traineeId }
            .distinct()
            .withIndex()
            .associate { indexed ->
                indexed.value to indexed.index + 1
            }

    items.forEachIndexed { index, item ->
        drawPaymentRow(
            index = index,
            item = item,
            demoIndex = demoIndexByTrainee[item.traineeId] ?: index + 1
        )
    }

    drawFooter()
    document.finishPage(page)

    val dir =
        File(
            context.cacheDir,
            "shared_pdfs"
        )

    check(
        dir.exists() ||
                dir.mkdirs()
    ) {
        "Unable to create PDF sharing directory"
    }

    val reportFileName =
        if (isEnglish) {
            "Payments Report.pdf"
        } else {
            "דוח תשלומים.pdf"
        }

    val file =
        File(
            dir,
            reportFileName
        )

    try {
        FileOutputStream(
            file,
            false
        ).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }

    return file
}