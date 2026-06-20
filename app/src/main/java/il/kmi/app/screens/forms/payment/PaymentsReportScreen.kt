package il.kmi.app.screens.admin

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
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.app.ui.KmiTopBar
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp


//=====================================================================

private const val MEMBERSHIP_REQUIRED_AMOUNT = 150.0

private fun paymentNowDateText(): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
}

private fun paymentCurrentYear(): Int {
    return SimpleDateFormat("yyyy", Locale.getDefault())
        .format(Date())
        .toIntOrNull()
        ?: 0
}

private fun paymentStatusFromAmount(
    paidAmount: Double,
    requiredAmount: Double = MEMBERSHIP_REQUIRED_AMOUNT
): PaymentStatus {
    return when {
        paidAmount <= 0.0 -> PaymentStatus.UNPAID
        paidAmount < requiredAmount -> PaymentStatus.PARTIAL
        else -> PaymentStatus.PAID
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
    val db = Firebase.firestore

    val usersDocs = db.collection("users")
        .get()
        .await()
        .documents
        .filter { it.isPaymentRelevantTrainee() }

    val paymentDocs = db.collection("membershipPayments")
        .get()
        .await()
        .documents

    val paymentDocsByTraineeId = buildMap<String, DocumentSnapshot> {
        paymentDocs.forEach { doc ->
            val keys = listOf(
                doc.id,
                doc.getString("traineeId"),
                doc.getString("userDocId"),
                doc.getString("uid"),
                doc.getString("authUid")
            )
                .mapNotNull { it?.trim()?.takeIf { key -> key.isNotBlank() } }
                .distinct()

            keys.forEach { key ->
                put(key, doc)
            }
        }
    }

    return usersDocs
        .map { userDoc ->
            val traineeId = userDoc.getString("uid")
                ?: userDoc.getString("authUid")
                ?: userDoc.id

            val paymentDoc = paymentDocsByTraineeId[traineeId]
                ?: paymentDocsByTraineeId[userDoc.id]

            val requiredAmount = paymentDoc?.getDouble("requiredAmount")
                ?: MEMBERSHIP_REQUIRED_AMOUNT

            val paidAmount = paymentDoc?.getDouble("paidAmount")
                ?: 0.0

            val status = paymentStatusFromAmount(
                paidAmount = paidAmount,
                requiredAmount = requiredAmount
            )

            val method = paymentMethodFromString(
                paymentDoc?.getString("paymentMethod")
            )

            PaymentReportItem(
                traineeId = traineeId,
                fullName = paymentDoc?.getString("fullName").validDisplayNameOrNull()
                    ?: paymentDoc?.getString("full_name").validDisplayNameOrNull()
                    ?: paymentDoc?.getString("traineeName").validDisplayNameOrNull()
                    ?: paymentDoc?.getString("trainee_name").validDisplayNameOrNull()
                    ?: userDoc.paymentUserName(),
                branchName = paymentDoc?.getString("branchName").cleanPaymentText()
                    .ifBlank { userDoc.paymentUserBranch() },
                phone = paymentDoc?.getString("phone").cleanPaymentText()
                    .ifBlank { userDoc.paymentUserPhone() },
                requiredAmount = requiredAmount,
                paidAmount = paidAmount,
                status = status,
                paymentMethod = method,
                paymentDate = paymentDoc?.getString("paymentDate").orEmpty(),
                notes = paymentDoc?.getString("notes").orEmpty()
            )
        }
        .filter { item ->
            item.fullName.isNotBlank() &&
                    item.fullName != "שם חסר" &&
                    !item.fullName.looksLikeTechnicalId()
        }
        .sortedWith(
            compareBy<PaymentReportItem> { it.branchName }
                .thenBy { it.fullName }
        )
}

private suspend fun saveManualMembershipPaymentToFirestore(
    item: PaymentReportItem,
    amountToAdd: Double,
    method: PaymentMethod,
    notes: String
): PaymentReportItem {
    val db = Firebase.firestore

    val newPaidAmount = item.paidAmount + amountToAdd
    val newStatus = paymentStatusFromAmount(
        paidAmount = newPaidAmount,
        requiredAmount = item.requiredAmount
    )

    val paymentDate = paymentNowDateText()

    val cleanMethod = method

    val updatedItem = item.copy(
        paidAmount = newPaidAmount,
        status = newStatus,
        paymentMethod = cleanMethod,
        paymentDate = paymentDate,
        notes = notes
    )

    val data = mapOf(
        "traineeId" to updatedItem.traineeId,
        "userDocId" to updatedItem.traineeId,
        "fullName" to updatedItem.fullName,
        "branchName" to updatedItem.branchName,
        "phone" to updatedItem.phone,
        "requiredAmount" to updatedItem.requiredAmount,
        "paidAmount" to updatedItem.paidAmount,
        "status" to paymentStatusToFirestore(updatedItem.status),
        "paymentMethod" to paymentMethodToFirestore(cleanMethod),
        "paymentDate" to updatedItem.paymentDate,
        "paymentYear" to paymentCurrentYear(),
        "lastPaymentAmount" to amountToAdd,
        "notes" to updatedItem.notes,
        "updatedAt" to FieldValue.serverTimestamp(),
        "updatedAtMillis" to System.currentTimeMillis(),
        "source" to "android_payments_report"
    )

    val paymentDocRef = db.collection("membershipPayments")
        .document(updatedItem.traineeId)

    paymentDocRef
        .set(data, SetOptions.merge())
        .await()

    val historyData = mapOf(
        "traineeId" to updatedItem.traineeId,
        "fullName" to updatedItem.fullName,
        "branchName" to updatedItem.branchName,
        "amount" to amountToAdd,
        "paidAmountAfterUpdate" to updatedItem.paidAmount,
        "requiredAmount" to updatedItem.requiredAmount,
        "statusAfterUpdate" to paymentStatusToFirestore(updatedItem.status),
        "paymentMethod" to paymentMethodToFirestore(cleanMethod),
        "paymentDate" to updatedItem.paymentDate,
        "paymentYear" to paymentCurrentYear(),
        "notes" to notes,
        "createdAt" to FieldValue.serverTimestamp(),
        "createdAtMillis" to System.currentTimeMillis(),
        "source" to "android_payments_report_history"
    )

    paymentDocRef
        .collection("history")
        .document()
        .set(historyData)
        .await()

    return updatedItem
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

    var isLoadingPayments by remember { mutableStateOf(true) }
    var paymentsError by remember { mutableStateOf<String?>(null) }
    val screenScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoadingPayments = true
        paymentsError = null

        runCatching {
            loadRealPaymentsReportItems()
        }.onSuccess { realItems ->
            items = realItems
            isLoadingPayments = false
        }.onFailure { error ->
            val rawMessage = error.localizedMessage.orEmpty()

            paymentsError = if (rawMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
                rawMessage.contains("insufficient permissions", ignoreCase = true)
            ) {
                if (isEnglish) {
                    "You do not have permission to load payment data. Please check Firestore rules."
                } else {
                    "אין הרשאה לטעון את נתוני התשלומים מהשרת. צריך לבדוק הרשאות Firestore למנהל/מאמן."
                }
            } else {
                rawMessage.ifBlank {
                    if (isEnglish) "Unknown loading error" else "שגיאה לא ידועה בטעינת התשלומים"
                }
            }

            isLoadingPayments = false
        }
    }

    val title = if (isEnglish) "Payments Report" else "דו\"ח תשלומים"
    val paidText = if (isEnglish) "Paid 150" else "שילמו"
    val unpaidText = if (isEnglish) "Not paid 150" else "לא שילמו"

    val screenLayoutDirection =
        if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    val screenTextAlign =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    val screenHorizontalAlign =
        if (isEnglish) Alignment.Start else Alignment.End

    val allBranchesLabel = if (isEnglish) "All Branches" else "כל הסניפים"

    val branchOptions = remember(isEnglish, items) {
        val catalogBranches = il.kmi.app.training.TrainingCatalog.allVisibleBranches()

        val realBranches = items
            .map { it.branchName.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        listOf(allBranchesLabel) +
                (catalogBranches + realBranches)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
    }

    var selectedBranch by rememberSaveable {
        mutableStateOf(branchOptions.first())
    }

    LaunchedEffect(branchOptions, allBranchesLabel) {
        if (selectedBranch !in branchOptions) {
            selectedBranch = allBranchesLabel
        }
    }

    val filteredItems = items.filter { item ->
        val matchesQuery =
            query.isBlank() ||
                    item.fullName.contains(query, ignoreCase = true) ||
                    item.phone.contains(query, ignoreCase = true) ||
                    item.branchName.contains(query, ignoreCase = true)

        val matchesFilter = when (filter) {
            // ✅ שילמו דמי חבר מלאים
            "PAID" -> item.paidAmount >= item.requiredAmount

            // ✅ לא שילמו מלא:
            // כולל מי שלא שילם בכלל וגם מי ששילם חלקית.
            "UNPAID" -> item.paidAmount < item.requiredAmount

            else -> true
        }

        val matchesBranch =
            selectedBranch == allBranchesLabel ||
                    item.branchName == selectedBranch

        matchesQuery && matchesFilter && matchesBranch
    }

    val totalRequired = items.sumOf { it.requiredAmount }
    val totalPaid = items.sumOf { it.paidAmount }

    // ✅ שילמו = שילמו את מלוא הסכום הנדרש.
    val paidCount = items.count { it.paidAmount >= it.requiredAmount }

    // ✅ לא שילמו = כל מי שלא הגיע למלוא הסכום, כולל חלקי.
    val unpaidCount = items.count { it.paidAmount < it.requiredAmount }

    val collectionPercent =
        if (totalRequired > 0.0) {
            ((totalPaid / totalRequired) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            KmiTopBar(
                title = title,
                onHome = onClose,
                showTopHome = false,
                lockSearch = false,
                showBottomActions = true,
                currentLang = if (isEnglish) "en" else "he"
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
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides screenLayoutDirection
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = screenHorizontalAlign,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) {
                                        "Premium payments dashboard"
                                    } else {
                                        "דשבורד תשלומים פרימיום"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 17.sp,
                                        lineHeight = 21.sp
                                    ),
                                    color = Color(0xFF1E2A3D),
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = screenTextAlign,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1
                                )

                                Text(
                                    text = if (isEnglish) {
                                        "For trainees, coaches and managers"
                                    } else {
                                        "למתאמנים, למאמנים ולמנהלים"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp
                                    ),
                                    color = Color(0xFF5E6C80),
                                    textAlign = screenTextAlign,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1
                                )

                                Text(
                                    text = if (isEnglish) {
                                        "Collected ₪${"%.0f".format(totalPaid)} of ₪${"%.0f".format(totalRequired)}"
                                    } else {
                                        "נגבה ₪${"%.0f".format(totalPaid)} מתוך ₪${"%.0f".format(totalRequired)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp
                                    ),
                                    color = Color(0xFF0F5E9C),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = screenTextAlign,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SummaryCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp),
                                title = if (isEnglish) "Collection" else "אחוז גבייה",
                                value = "${"%.0f".format(collectionPercent)}%",
                                icon = Icons.Default.TrendingUp,
                                selected = false,
                                baseColor = Color(0xFF1DA1F2),
                                selectedColor = Color(0xFF0284C7),
                                onClick = {}
                            )

                            SummaryCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp),
                                title = if (isEnglish) "Trainees" else "מתאמנים",
                                value = items.size.toString(),
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp),
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp),
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Search & filters" else "חיפוש וסינון",
                            color = Color(0xFF1E2A3D),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                lineHeight = 18.sp
                            ),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            singleLine = true,
                            label = {
                                Text(
                                    text = if (isEnglish)
                                        "Search by name / phone / branch"
                                    else
                                        "חיפוש לפי שם / טלפון / סניף",
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                                color = Color(0xFF1E2A3D)
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
                            color = Color(0xFF5E6C80),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 15.sp
                            ),
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
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.94f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isEnglish) {
                                            "Loading real payment data..."
                                        } else {
                                            "טוען נתוני תשלום אמיתיים..."
                                        },
                                        color = Color(0xFF1F2A52),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        paymentsError != null -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFE4E6)
                                )
                            ) {
                                Text(
                                    text = if (isEnglish) {
                                        "Failed loading payments: $paymentsError"
                                    } else {
                                        "טעינת התשלומים נכשלה: $paymentsError"
                                    },
                                    color = Color(0xFF991B1B),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                                )
                            }
                        }

                        filteredItems.isEmpty() -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.94f)
                                )
                            ) {
                                Text(
                                    text = if (isEnglish) {
                                        "No trainees matched the current filters."
                                    } else {
                                        "לא נמצאו מתאמנים בהתאם לסינון הנוכחי."
                                    },
                                    color = Color(0xFF1F2A52),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                                )
                            }
                        }

                        else -> {
                            filteredItems.forEach { item ->
                                PaymentReportRow(
                                    item = item,
                                    isEnglish = isEnglish,
                                    onManualUpdate = { manualDialogItem = item }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(36.dp))
                }
            }

                manualDialogItem?.let { selected ->
                    ManualPaymentDialog(
                        isEnglish = isEnglish,
                        item = selected,
                        onDismiss = { manualDialogItem = null },
                        onSave = { amount, method, notes ->
                            screenScope.launch {
                                runCatching {
                                    saveManualMembershipPaymentToFirestore(
                                        item = selected,
                                        amountToAdd = amount,
                                        method = method,
                                        notes = notes
                                    )
                                }.onSuccess { updatedItem ->
                                    items = items.map { current ->
                                        if (current.traineeId == selected.traineeId) {
                                            updatedItem
                                        } else {
                                            current
                                        }
                                    }

                                    onSaveManualPayment(
                                        selected.traineeId,
                                        amount,
                                        method,
                                        notes
                                    )

                                    manualDialogItem = null
                                }.onFailure { error ->
                                    paymentsError = error.localizedMessage ?: "Failed saving payment"
                                    manualDialogItem = null
                                }
                            }
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
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 10.dp else 6.dp
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = title,
                color = Color.White.copy(alpha = 0.90f),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 22.sp
                ),
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
            text = if (isEnglish) "Paid\n150" else "שילמו\n150",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BranchDropdown(
    isEnglish: Boolean,
    selectedBranch: String,
    branchOptions: List<String>,
    onBranchSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val dropdownLayoutDirection =
        if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    val dropdownTextAlign =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    CompositionLocalProvider(
        LocalLayoutDirection provides dropdownLayoutDirection
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedBranch,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = {
                    Text(
                        text = if (isEnglish) "Branch" else "סניף",
                        textAlign = dropdownTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = dropdownTextAlign,
                    color = Color(0xFF1E2A3D)
                ),
                colors = reportFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                branchOptions.forEach { branch ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = branch,
                                textAlign = dropdownTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        onClick = {
                            onBranchSelected(branch)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
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
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0xFF7B57D1) else Color.White,
        tonalElevation = if (selected) 5.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFF7B57D1) else Color(0xFFD8E3F5)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else Color(0xFF1E2A3D),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                ),
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3D66)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    if (!isEnglish) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = statusColor(item.status).copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = statusLabel(item.status, isEnglish),
                                color = statusColor(item.status),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                    ) {
                        Text(
                            text = item.fullName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                lineHeight = 17.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = listOf(item.branchName, item.phone)
                                .filter { it.isNotBlank() }
                                .joinToString(" • "),
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            ),
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )
                    }

                    if (isEnglish) {
                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = statusColor(item.status).copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = statusLabel(item.status, isEnglish),
                                color = statusColor(item.status),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            Text(
                text = if (isEnglish)
                    "Fee: ₪${"%.0f".format(item.paidAmount)} / ₪${"%.0f".format(item.requiredAmount)}"
                else
                    "דמי חבר: ₪${"%.0f".format(item.paidAmount)} / ₪${"%.0f".format(item.requiredAmount)}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                fontWeight = FontWeight.Bold,
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            if (item.paymentDate.isNotBlank()) {
                Text(
                    text = if (isEnglish)
                        "Last update: ${item.paymentDate}"
                    else
                        "עדכון אחרון: ${item.paymentDate}",
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onManualUpdate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7B57D1),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.AddCard, contentDescription = null)
                Spacer(Modifier.height(0.dp).width(8.dp))
                Text(
                    text = if (isEnglish) "Add Membership Payment" else "הוסף דמי חבר",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 15.sp
                    ),
                    fontWeight = FontWeight.Bold
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
                text = if (isEnglish) "Manual Payment Update" else "עדכון תשלום ידני",
                color = Color.White,
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = item.fullName,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isEnglish) "Amount" else "סכום") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                    ),
                    colors = reportFieldColors()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    ) {
                        Text(
                            text = if (isEnglish) "Payment Method" else "אמצעי תשלום",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = if (isEnglish) 4.dp else 0.dp,
                                    end = if (isEnglish) 0.dp else 4.dp,
                                    bottom = 4.dp
                                )
                        )

                        Surface(
                            onClick = { expanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF24365E)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isEnglish) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expanded
                                    )

                                    Spacer(Modifier.width(8.dp))
                                }

                                Text(
                                    text = paymentMethodLabel(method, isEnglish),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isEnglish) {
                                    Spacer(Modifier.width(8.dp))

                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expanded
                                    )
                                }
                            }
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        PaymentMethod.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = paymentMethodLabel(option, isEnglish),
                                        fontSize = 14.sp,
                                        lineHeight = 17.sp,
                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
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
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                    ),
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
    unfocusedLabelColor = Color(0xFF5E6C80),
    disabledLabelColor = Color(0xFF5E6C80),

    focusedLeadingIconColor = Color(0xFF6B778B),
    unfocusedLeadingIconColor = Color(0xFF6B778B),
    disabledLeadingIconColor = Color(0xFF6B778B),

    focusedTrailingIconColor = Color(0xFF6B778B),
    unfocusedTrailingIconColor = Color(0xFF6B778B),
    disabledTrailingIconColor = Color(0xFF6B778B),

    cursorColor = Color(0xFF1E2A3D)
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