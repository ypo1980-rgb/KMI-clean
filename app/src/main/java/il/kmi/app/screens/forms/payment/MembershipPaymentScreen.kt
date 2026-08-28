package il.kmi.app.screens.forms.payment

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.border
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import il.kmi.app.ui.DrawerBridge
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import il.kmi.app.ui.KmiLanguageDirection
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.app.ui.pdf.KmiPdfDirection
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

//==========================================================================

@Composable
private fun MembershipPaymentPremiumLoading(
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

private const val MISSING_BRANCH_HE = "הסניף שלי לא מופיע"
private const val MISSING_BRANCH_EN = "My branch is not listed"

private val membershipBirthDateFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun parseMembershipBirthDate(
    raw: String
): LocalDate? {
    val cleanRaw = raw.trim()

    if (cleanRaw.isBlank()) {
        return null
    }

    return runCatching {
        LocalDate.parse(
            cleanRaw,
            membershipBirthDateFormatter
        )
    }.getOrNull()
        ?: runCatching {
            LocalDate.parse(
                cleanRaw,
                DateTimeFormatter.ISO_LOCAL_DATE
            )
        }.getOrNull()
}

private fun membershipBirthDateParts(
    raw: String
): Triple<String, String, String> {
    val date =
        parseMembershipBirthDate(raw)
            ?: return Triple("", "", "")

    return Triple(
        date.dayOfMonth
            .toString()
            .padStart(2, '0'),
        date.monthValue
            .toString()
            .padStart(2, '0'),
        date.year.toString()
    )
}

private fun buildMembershipBirthDate(
    day: String,
    month: String,
    year: String
): String {
    if (
        day.length != 2 ||
        month.length != 2 ||
        year.length != 4
    ) {
        return ""
    }

    val date =
        runCatching {
            LocalDate.of(
                year.toInt(),
                month.toInt(),
                day.toInt()
            )
        }.getOrNull()
            ?: return ""

    if (date.isAfter(LocalDate.now())) {
        return ""
    }

    return date.format(
        membershipBirthDateFormatter
    )
}

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

private fun createMembershipPaymentPdf(
    context: Context,
    formData: MembershipPaymentFormData,
    isEnglish: Boolean
): File {
    val document = PdfDocument()

    try {
        val pageWidth = 595
        val pageHeight = 842

        val pageInfo =
            PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                1
            ).create()

        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(
            android.graphics.Color.WHITE
        )

        val contentLeft = 42f
        val contentRight = pageWidth - 42f

        fun startX(): Float =
            KmiPdfDirection.startX(
                isEnglish = isEnglish,
                left = contentLeft,
                right = contentRight
            )

        val regularTypeface =
            Typeface.create(
                Typeface.SANS_SERIF,
                Typeface.NORMAL
            )

        val boldTypeface =
            Typeface.create(
                Typeface.SANS_SERIF,
                Typeface.BOLD
            )

        val sectionPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(
                    79,
                    55,
                    139
                )
                textSize = 16f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
                textAlign =
                    KmiPdfDirection.textAlign(
                        isEnglish = isEnglish
                    )
            }

        val rowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(
                    23,
                    32,
                    51
                )
                textSize = 13f
                typeface = Typeface.DEFAULT
                textAlign =
                    KmiPdfDirection.textAlign(
                        isEnglish = isEnglish
                    )
            }

        val boldRowPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(
                    23,
                    32,
                    51
                )
                textSize = 13f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
                textAlign =
                    KmiPdfDirection.textAlign(
                        isEnglish = isEnglish
                    )
            }

        val dividerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(
                    213,
                    222,
                    229
                )
                strokeWidth = 1.2f
            }

        fun tr(
            hebrew: String,
            english: String
        ): String {
            return if (isEnglish) {
                english
            } else {
                hebrew
            }
        }

        fun drawGlobalPdfHeader() {
            val traineeName =
                listOf(
                    formData.traineeFirstName,
                    formData.traineeLastName
                )
                    .filter {
                        it.isNotBlank()
                    }
                    .joinToString(" ")
                    .ifBlank {
                        tr(
                            hebrew = "פרטי החניך",
                            english = "Trainee Details"
                        )
                    }

            KmiPdfHeader.draw(
                context = context,
                canvas = canvas,
                pageWidth = pageWidth,
                isEnglish = isEnglish,
                titleHebrew = "טופס תשלום דמי חבר",
                titleEnglish = "Membership Payment Form",
                subtitleHebrew = traineeName,
                subtitleEnglish = traineeName
            )
        }

        fun cleanValue(
            value: String
        ): String {
            return value
                .trim()
                .ifBlank { "—" }
        }

        fun drawSection(
            title: String,
            currentY: Float
        ): Float {
            canvas.drawText(
                title,
                startX(),
                currentY,
                sectionPaint
            )

            canvas.drawLine(
                contentLeft,
                currentY + 9f,
                contentRight,
                currentY + 9f,
                dividerPaint
            )

            return currentY + 34f
        }

        fun drawRow(
            label: String,
            value: String,
            currentY: Float,
            bold: Boolean = false
        ): Float {
            val resolvedText =
                if (isEnglish) {
                    "$label: ${cleanValue(value)}"
                } else {
                    "$label: ${cleanValue(value)}"
                }

            canvas.drawText(
                resolvedText,
                startX(),
                currentY,
                if (bold) {
                    boldRowPaint
                } else {
                    rowPaint
                }
            )

            return currentY + 25f
        }

        drawGlobalPdfHeader()

        var y = KmiPdfHeader.CONTENT_TOP

        y = drawSection(
            title = tr(
                hebrew = "פרטי החניך",
                english = "Trainee Details"
            ),
            currentY = y
        )

        y = drawRow(
            label = tr("שם פרטי", "First name"),
            value = formData.traineeFirstName,
            currentY = y
        )

        y = drawRow(
            label = tr("שם משפחה", "Last name"),
            value = formData.traineeLastName,
            currentY = y
        )

        y = drawRow(
            label = tr("מספר זהות", "ID number"),
            value = formData.traineeIdNumber,
            currentY = y
        )

        y = drawRow(
            label = tr("תאריך לידה", "Date of birth"),
            value = formData.traineeBirthDate,
            currentY = y
        )

        y = drawRow(
            label = tr("דוא״ל", "Email"),
            value = formData.traineeEmail,
            currentY = y
        )

        y = drawRow(
            label = tr("טלפון", "Phone"),
            value = formData.traineePhone,
            currentY = y
        )

        val resolvedBranch =
            if (
                formData.traineeOtherBranch
                    .isNotBlank()
            ) {
                formData.traineeOtherBranch
            } else {
                formData.traineeBranch
            }

        y = drawRow(
            label = tr("סניף", "Branch"),
            value = resolvedBranch,
            currentY = y
        )

        y += 10f

        y = drawSection(
            title = tr(
                hebrew = "פרטי המשלם",
                english = "Payer Details"
            ),
            currentY = y
        )

        y = drawRow(
            label = tr("שם פרטי", "First name"),
            value = formData.payerFirstName,
            currentY = y
        )

        y = drawRow(
            label = tr("שם משפחה", "Last name"),
            value = formData.payerLastName,
            currentY = y
        )

        y = drawRow(
            label = tr("דוא״ל", "Email"),
            value = formData.payerEmail,
            currentY = y
        )

        y = drawRow(
            label = tr("טלפון", "Phone"),
            value = formData.payerPhone,
            currentY = y
        )

        y += 10f

        y = drawSection(
            title = tr(
                hebrew = "סיכום התשלום",
                english = "Payment Summary"
            ),
            currentY = y
        )

        val formattedAmount =
            String.format(
                Locale.US,
                "%.2f",
                formData.amount
            )

        y = drawRow(
            label = tr("סכום לתשלום", "Amount"),
            value =
                if (isEnglish) {
                    "$formattedAmount NIS"
                } else {
                    "$formattedAmount ₪"
                },
            currentY = y,
            bold = true
        )

        y = drawRow(
            label = tr(
                "אישור מדיניות ביטולים והחזרים",
                "Cancellation and refund policy"
            ),
            value =
                if (formData.policyAccepted) {
                    tr("אושר", "Approved")
                } else {
                    tr("טרם אושר", "Not approved")
                },
            currentY = y,
            bold = true
        )

        val footerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(
                    71,
                    84,
                    103
                )
                textSize = 10.5f
                textAlign = Paint.Align.CENTER
            }

        canvas.drawText(
            tr(
                hebrew = "מסמך זה הוא סיכום פרטי הטופס ואינו מהווה אישור לביצוע התשלום.",
                english = "This document summarizes the form and is not a payment confirmation."
            ),
            pageWidth / 2f,
            pageHeight -
                    KmiPdfFooter.CONTENT_BOTTOM_PADDING -
                    8f,
            footerPaint
        )

        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = 1,
            totalPages = 1,
            isEnglish = isEnglish
        )

        document.finishPage(page)

        val pdfDirectory =
            File(
                context.cacheDir,
                "shared_pdfs"
            ).apply {
                if (!exists()) {
                    mkdirs()
                }
            }

        val pdfFileName =
            if (isEnglish) {
                "KAMI Membership Payment Form.pdf"
            } else {
                "טופס תשלום דמי חבר קמי.pdf"
            }

        val pdfFile =
            File(
                pdfDirectory,
                pdfFileName
            )

        FileOutputStream(
            pdfFile,
            false
        ).use { outputStream ->
            document.writeTo(outputStream)
        }

        return pdfFile
    } finally {
        document.close()
    }
}

private fun shareMembershipPaymentPdf(
    context: Context,
    formData: MembershipPaymentFormData,
    isEnglish: Boolean
) {
    val pdfFile =
        createMembershipPaymentPdf(
            context = context,
            formData = formData,
            isEnglish = isEnglish
        )

    val pdfUri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isEnglish) {
                    "K.M.I Membership Payment Form"
                } else {
                    "ק.מ.י — טופס תשלום דמי חבר"
                }
            )

            putExtra(
                Intent.EXTRA_TEXT,
                if (isEnglish) {
                    "Attached is the K.M.I membership payment form."
                } else {
                    "מצורף טופס תשלום דמי חבר של ק.מ.י."
                }
            )

            putExtra(
                Intent.EXTRA_STREAM,
                pdfUri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    val chooserIntent =
        Intent.createChooser(
            shareIntent,
            if (isEnglish) {
                "Share payment form"
            } else {
                "שיתוף טופס התשלום"
            }
        )

    context.startActivity(chooserIntent)
}

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

private fun Map<String, Any?>.profileStringValue(
    vararg keys: String
): String {
    val profile =
        this["profile"] as? Map<*, *>
            ?: return ""

    for (key in keys) {
        val value = profile[key]
        if (value is String && value.isNotBlank()) {
            return value
        }
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

    val possibleDocuments =
        listOf(
            firestore.collection("users").document(uid),
            firestore.collection("trainees").document(uid),
            firestore.collection("members").document(uid)
        )

    var completedServerRead = false
    var lastServerError: Throwable? = null

    for (documentRef in possibleDocuments) {
        val snapshotResult =
            runCatching {
                documentRef
                    .get(Source.SERVER)
                    .await()
            }

        snapshotResult
            .onSuccess {
                completedServerRead = true
            }
            .onFailure { error ->
                lastServerError = error
            }

        val snapshot =
            snapshotResult.getOrNull()
                ?: continue
        val data = snapshot.data
            ?: continue

        val serverFullName = data.stringValue(
            "fullName",
            "full_name",
            "displayName",
            "display_name",
            "name",
            "traineeName",
            "trainee_name"
        ).ifBlank {
           data.profileStringValue(
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
           data.profileStringValue(
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
           data.profileStringValue(
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
           data.profileStringValue(
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
           data.profileStringValue(
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
           data.profileStringValue(
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
           data.profileStringValue(
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
           data.profileStringValue(
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

    if (!completedServerRead) {
        throw lastServerError
            ?: IllegalStateException(
                "Membership payment prefill could not be loaded"
            )
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
    val context = LocalContext.current

    var serverPrefill by remember {
        mutableStateOf(prefill)
    }
    var didApplyServerPrefill by rememberSaveable {
        mutableStateOf(false)
    }
    var isLoadingServerPrefill by rememberSaveable {
        mutableStateOf(true)
    }
    var serverPrefillLoadFailed by rememberSaveable {
        mutableStateOf(false)
    }
    var serverPrefillReloadKey by rememberSaveable {
        mutableIntStateOf(0)
    }

    LaunchedEffect(
        prefill,
        serverPrefillReloadKey
    ) {
        isLoadingServerPrefill = true
        serverPrefillLoadFailed = false

        try {
            val loadedPrefill =
                loadMembershipPaymentPrefillFromServer()

            serverPrefill =
                MembershipPaymentPrefill(
                    traineeFirstName =
                        loadedPrefill.traineeFirstName.orFallback(
                            prefill.traineeFirstName
                        ),
                    traineeLastName =
                        loadedPrefill.traineeLastName.orFallback(
                            prefill.traineeLastName
                        ),
                    traineeIdNumber =
                        loadedPrefill.traineeIdNumber.orFallback(
                            prefill.traineeIdNumber
                        ),
                    traineeBirthDate =
                        loadedPrefill.traineeBirthDate.orFallback(
                            prefill.traineeBirthDate
                        ),
                    traineeEmail =
                        loadedPrefill.traineeEmail.orFallback(
                            prefill.traineeEmail
                        ),
                    traineePhone =
                        loadedPrefill.traineePhone.orFallback(
                            prefill.traineePhone
                        ),
                    traineeBranch =
                        loadedPrefill.traineeBranch.orFallback(
                            prefill.traineeBranch
                        ),
                    traineeOtherBranch =
                        loadedPrefill.traineeOtherBranch.orFallback(
                            prefill.traineeOtherBranch
                        ),
                    payerFirstName =
                        loadedPrefill.payerFirstName.orFallback(
                            prefill.payerFirstName
                        ),
                    payerLastName =
                        loadedPrefill.payerLastName.orFallback(
                            prefill.payerLastName
                        ),
                    payerEmail =
                        loadedPrefill.payerEmail.orFallback(
                            prefill.payerEmail
                        ),
                    payerPhone =
                        loadedPrefill.payerPhone.orFallback(
                            prefill.payerPhone
                        )
                )
        } catch (_: Exception) {
            serverPrefillLoadFailed = true
        } finally {
            isLoadingServerPrefill = false
        }
    }

    val title = if (isEnglish) "Membership Payment" else "תשלום דמי חבר"
    val traineeTitle = if (isEnglish) "Trainee Details" else "פרטי חניך"
    val payerTitle = if (isEnglish) "Payer Details for Invoice" else "פרטי המשלם לשליחת חשבונית"
    val productTitle = if (isEnglish) "Payment Summary" else "סיכום תשלום"
    val policyTitle = if (isEnglish) "Cancellation & Refund Policy" else "מדיניות ביטולים והחזרים"
    val continueText = if (isEnglish) "Continue to Payment" else "המשך לתשלום"
    val readPolicyText = if (isEnglish) "Read Full Policy" else "קרא מדיניות מלאה"
    val payerSameToggleText =
        if (isEnglish) "Payer is the same as trainee" else "המשלם זהה לפרטי החניך"

    val branchOptions = remember(isEnglish) {
        il.kmi.app.training.TrainingCatalog.allVisibleBranches() +
                listOf(if (isEnglish) MISSING_BRANCH_EN else MISSING_BRANCH_HE)
    }

    var traineeFirstName by rememberSaveable {
        mutableStateOf(prefill.traineeFirstName)
    }
    var traineeLastName by rememberSaveable {
        mutableStateOf(prefill.traineeLastName)
    }
    var traineeIdNumber by rememberSaveable {
        mutableStateOf(prefill.traineeIdNumber)
    }
    var traineeBirthDate by rememberSaveable {
        mutableStateOf(prefill.traineeBirthDate)
    }

    val initialBirthDateParts =
        remember(prefill.traineeBirthDate) {
            membershipBirthDateParts(
                prefill.traineeBirthDate
            )
        }

    var birthDay by rememberSaveable {
        mutableStateOf(initialBirthDateParts.first)
    }
    var birthMonth by rememberSaveable {
        mutableStateOf(initialBirthDateParts.second)
    }
    var birthYear by rememberSaveable {
        mutableStateOf(initialBirthDateParts.third)
    }

    val birthDayFocusRequester =
        remember {
            FocusRequester()
        }
    val birthMonthFocusRequester =
        remember {
            FocusRequester()
        }
    val birthYearFocusRequester =
        remember {
            FocusRequester()
        }
    val focusManager =
        LocalFocusManager.current

    var traineeEmail by rememberSaveable {
        mutableStateOf(prefill.traineeEmail)
    }
    var traineePhone by rememberSaveable {
        mutableStateOf(prefill.traineePhone)
    }

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

    var policyAccepted by rememberSaveable {
        mutableStateOf(false)
    }
    var showFullRefundPolicy by rememberSaveable {
        mutableStateOf(false)
    }

    BackHandler(
        enabled = !showFullRefundPolicy
    ) {
        onClose()
    }

    val missingBranchValue =
        if (isEnglish) {
            MISSING_BRANCH_EN
        } else {
            MISSING_BRANCH_HE
        }
    val shouldShowOtherBranch =
        traineeBranch == missingBranchValue

    LaunchedEffect(serverPrefill, branchOptions) {
        if (!didApplyServerPrefill && serverPrefill.hasAnyValue()) {
            traineeFirstName = serverPrefill.traineeFirstName
            traineeLastName = serverPrefill.traineeLastName
            traineeIdNumber = serverPrefill.traineeIdNumber
            traineeBirthDate = serverPrefill.traineeBirthDate

            val loadedBirthDateParts =
                membershipBirthDateParts(
                    serverPrefill.traineeBirthDate
                )
            birthDay = loadedBirthDateParts.first
            birthMonth = loadedBirthDateParts.second
            birthYear = loadedBirthDateParts.third

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

    val currentFormData =
        MembershipPaymentFormData(
            traineeFirstName =
                traineeFirstName.trim(),
            traineeLastName =
                traineeLastName.trim(),
            traineeIdNumber =
                traineeIdNumber.trim(),
            traineeBirthDate =
                traineeBirthDate.trim(),
            traineeEmail =
                traineeEmail.trim(),
            traineePhone =
                traineePhone.trim(),
            traineeBranch =
                traineeBranch.trim(),
            traineeOtherBranch =
                traineeOtherBranch.trim(),
            payerSameAsTrainee =
                payerSameAsTrainee,
            payerFirstName =
                payerFirstName.trim(),
            payerLastName =
                payerLastName.trim(),
            payerEmail =
                payerEmail.trim(),
            payerPhone =
                payerPhone.trim(),
            policyAccepted =
                policyAccepted,
            amount = 150.0
        )

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

    KmiLanguageDirection(
        isEnglish = isEnglish
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                KmiTopBar(
                    title = title,
                    currentLang =
                        if (isEnglish) {
                            "en"
                        } else {
                            "he"
                        },
                    showMenu = true,
                    showRoleStatus = true,
                    showSettings = true,
                    showBottomActions = true,
                    showModePill = true,
                    showRoleBadge = true,
                    showTopHome = false,
                    showTopSearch = false,
                    showTopShare = true,
                    centerTitle = true,
                    lockHome = false,
                    lockSearch = false,
                    onOpenDrawer = {
                        DrawerBridge.open()
                    },
                    onHome = onClose,
                    onShare = {
                        shareMembershipPaymentPdf(
                            context = context,
                            formData = currentFormData,
                            isEnglish = isEnglish
                        )
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                kmiScreenBackgroundBrush()
                        )
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(
                                horizontal = 14.dp,
                                vertical = 10.dp
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    if (serverPrefillLoadFailed) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .errorContainer
                                ),
                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation = 0.dp
                                ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "We couldn't load your latest details. Check your connection and try again."
                                        } else {
                                            "לא הצלחנו לטעון את הפרטים העדכניים שלך. בדוק את החיבור ונסה שוב."
                                        },
                                    style =
                                        KmiTypography.body.copy(
                                            fontWeight =
                                                FontWeight.SemiBold
                                        ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                TextButton(
                                    onClick = {
                                        serverPrefillReloadKey += 1
                                    },
                                    colors =
                                        ButtonDefaults.textButtonColors(
                                            contentColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onErrorContainer
                                        )
                                ) {
                                    Text(
                                        text =
                                            if (isEnglish) {
                                                "Try again"
                                            } else {
                                                "נסה שוב"
                                            },
                                        style =
                                            KmiTypography.action
                                    )
                                }
                            }
                        }
                    }

                    ProductHeroCard(
                        isEnglish = isEnglish,
                        amountText =
                            if (isEnglish) {
                                "₪150.00"
                            } else {
                                "150.00 ₪"
                            }
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

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement =
                                Arrangement.spacedBy(6.dp),
                            horizontalAlignment =
                                Alignment.Start
                        ) {
                            Text(
                                text =
                                    if (isEnglish) {
                                        "Birth Date"
                                    } else {
                                        "תאריך לידה"
                                    },
                                style =
                                    KmiTypography.body.copy(
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    ),
                                color =
                                    MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )

                            CompositionLocalProvider(
                                LocalLayoutDirection provides
                                        LayoutDirection.Ltr
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment =
                                        Alignment.CenterVertically,
                                    horizontalArrangement =
                                        Arrangement.spacedBy(6.dp)
                                ) {
                                    BirthDatePartField(
                                        value = birthDay,
                                        onValueChange = { value ->
                                            birthDay = value
                                            traineeBirthDate =
                                                buildMembershipBirthDate(
                                                    day = value,
                                                    month = birthMonth,
                                                    year = birthYear
                                                )
                                        },
                                        label =
                                            if (isEnglish) {
                                                "Day"
                                            } else {
                                                "יום"
                                            },
                                        placeholder = "DD",
                                        maxLength = 2,
                                        focusRequester =
                                            birthDayFocusRequester,
                                        imeAction = ImeAction.Next,
                                        onCompleted = {
                                            birthMonthFocusRequester
                                                .requestFocus()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = "/",
                                        style =
                                            KmiTypography.action.copy(
                                                fontWeight =
                                                    FontWeight.Bold
                                            ),
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )

                                    BirthDatePartField(
                                        value = birthMonth,
                                        onValueChange = { value ->
                                            birthMonth = value
                                            traineeBirthDate =
                                                buildMembershipBirthDate(
                                                    day = birthDay,
                                                    month = value,
                                                    year = birthYear
                                                )
                                        },
                                        label =
                                            if (isEnglish) {
                                                "Month"
                                            } else {
                                                "חודש"
                                            },
                                        placeholder = "MM",
                                        maxLength = 2,
                                        focusRequester =
                                            birthMonthFocusRequester,
                                        imeAction = ImeAction.Next,
                                        onCompleted = {
                                            birthYearFocusRequester
                                                .requestFocus()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = "/",
                                        style =
                                            KmiTypography.action.copy(
                                                fontWeight =
                                                    FontWeight.Bold
                                            ),
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )

                                    BirthDatePartField(
                                        value = birthYear,
                                        onValueChange = { value ->
                                            birthYear = value
                                            traineeBirthDate =
                                                buildMembershipBirthDate(
                                                    day = birthDay,
                                                    month = birthMonth,
                                                    year = value
                                                )
                                        },
                                        label =
                                            if (isEnglish) {
                                                "Year"
                                            } else {
                                                "שנה"
                                            },
                                        placeholder = "YYYY",
                                        maxLength = 4,
                                        focusRequester =
                                            birthYearFocusRequester,
                                        imeAction = ImeAction.Done,
                                        onCompleted = {
                                            focusManager.clearFocus()
                                        },
                                        modifier =
                                            Modifier.weight(1.2f)
                                    )
                                }
                            }
                        }

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

                        KmiPremiumDropdown(
                            title =
                                if (isEnglish) {
                                    "Branch Name"
                                } else {
                                    "שם הסניף"
                                },
                            options =
                                branchOptions
                                    .map { option ->
                                        option.trim()
                                    }
                                    .filter { option ->
                                        option.isNotBlank()
                                    }
                                    .distinct(),
                            selectedValue = traineeBranch.trim(),
                            isEnglish = isEnglish,
                            placeholder =
                                if (isEnglish) {
                                    "Select branch"
                                } else {
                                    "בחר סניף"
                                },
                            enabled = branchOptions.isNotEmpty(),
                            onSelected = { selectedBranch ->
                                traineeBranch = selectedBranch

                                if (selectedBranch != missingBranchValue) {
                                    traineeOtherBranch = ""
                                }
                            }
                        )

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
                            color = MaterialTheme.colorScheme.secondaryContainer
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
                                    style = KmiTypography.body,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSecondaryContainer,
                                    textAlign = TextAlign.Start,
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
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = if (isEnglish) {
                                    "Payment of membership fees is final after approval, except in cases such as duplicate payment or another good-faith mistake, subject to review by the association."
                                } else {
                                    "תשלום דמי חבר הוא סופי לאחר אישור הפעולה, למעט מקרים של תשלום כפול בטעות או טעות אחרת בתום לב, בכפוף לבדיקת העמותה."
                                },
                                style = KmiTypography.body,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.Start
                        ) {
                            TextButton(
                                onClick = {
                                    showFullRefundPolicy = true
                                    onReadFullPolicy()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Default.Description,
                                    contentDescription = null
                                )

                                Text(
                                    text = readPolicyText,
                                    style = KmiTypography.body,
                                    fontWeight =
                                        FontWeight.SemiBold,
                                    modifier =
                                        Modifier.padding(
                                            start = 6.dp
                                        )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color =
                                if (policyAccepted) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.5.dp,
                                    color =
                                        if (policyAccepted) {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .outlineVariant
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
                                Checkbox(
                                    checked = policyAccepted,
                                    onCheckedChange = {
                                        policyAccepted = it
                                    },
                                    modifier =
                                        Modifier.size(34.dp),
                                    colors =
                                        CheckboxDefaults.colors(
                                            checkedColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary,
                                            uncheckedColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .outline,
                                            checkmarkColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onPrimary
                                        )
                                )

                                Text(
                                    text =
                                        if (isEnglish) {
                                            "I have read and agree to the cancellation and refund policy."
                                        } else {
                                            "קראתי ואני מאשר/ת את מדיניות הביטולים וההחזרים."
                                        },
                                    style = KmiTypography.body,
                                    color =
                                        if (policyAccepted) {
                                            MaterialTheme
                                                .colorScheme
                                                .onTertiaryContainer
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        },
                                    fontWeight =
                                        FontWeight.SemiBold,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onContinueToPayment(
                                currentFormData
                            )
                        },
                        enabled = isFormValid,
                        shape = MaterialTheme.shapes.extraLarge,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .primary,
                                contentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary,
                                disabledContainerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant,
                                disabledContentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                                        .copy(alpha = 0.55f)
                            ),
                        elevation =
                            ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                disabledElevation = 0.dp
                            ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null
                        )
                        Text(
                            text = "  $continueText",
                            style = KmiTypography.cardTitle,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                if (isLoadingServerPrefill) {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        color =
                            MaterialTheme.colorScheme.background.copy(
                                alpha = 0.96f
                            ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.surface
                                    ),
                                elevation =
                                    CardDefaults.cardElevation(
                                        defaultElevation = 0.dp
                                    ),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                            ) {
                                MembershipPaymentPremiumLoading(
                                    text =
                                        if (isEnglish) {
                                            "Loading your details..."
                                        } else {
                                            "טוען את הפרטים שלך..."
                                        }
                                )
                            }
                        }
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
                        Text(
                            text = if (isEnglish) "Close" else "סגור",
                            style = KmiTypography.body,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                title = {
                    Text(
                        text = policyTitle,
                        style = KmiTypography.sectionTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "1. The membership fee is a registration and association membership payment."
                            } else {
                                "1. דמי החבר הם תשלום עבור רישום וחברות בעמותה."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Text(
                            text = if (isEnglish) {
                                "2. After payment approval, the payment is considered final, except in cases of duplicate payment, technical error, or another good-faith mistake."
                            } else {
                                "2. לאחר אישור התשלום, התשלום נחשב סופי, למעט מקרים של תשלום כפול, תקלה טכנית או טעות אחרת בתום לב."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Text(
                            text = if (isEnglish) {
                                "3. Refund requests will be reviewed by the association according to the payment details, payment date, and the reason for the request."
                            } else {
                                "3. בקשות להחזר ייבחנו על ידי העמותה בהתאם לפרטי התשלום, מועד התשלום וסיבת הבקשה."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Text(
                            text = if (isEnglish) {
                                "4. If a refund is approved, it will be processed using the same payment method or another method approved by the association."
                            } else {
                                "4. אם יאושר החזר, הוא יבוצע באמצעי התשלום המקורי או באמצעי אחר שיאושר על ידי העמותה."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Text(
                            text = if (isEnglish) {
                                "5. Administrative or clearing fees may be deducted if required by the payment provider or applicable rules."
                            } else {
                                "5. ייתכן ניכוי עמלות טיפול או סליקה, ככל שהדבר נדרש על ידי ספק התשלום או לפי הנהלים החלים."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Text(
                            text = if (isEnglish) {
                                "6. By checking the approval box, the payer confirms that they have read and agreed to this cancellation and refund policy before continuing to payment."
                            } else {
                                "6. סימון תיבת האישור מהווה אישור לכך שהמשלם קרא והסכים למדיניות הביטולים וההחזרים לפני המעבר לתשלום."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
        }
    } // סוף KmiLanguageDirection
}

@Composable
private fun ProductHeroCard(
    isEnglish: Boolean,
    amountText: String
) {
    val textAlign = TextAlign.Start
    val horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End

    val compactAmount = amountText
        .replace(".00", "")
        .replace("150 ₪", "₪150")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(6.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF8B5CF6)
                ) {
                    Text(
                        text = compactAmount,
                        style = KmiTypography.sectionTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(
                            horizontal = 22.dp,
                            vertical = 10.dp
                        ),
                        maxLines = 1
                    )
                }

                Surface(
                    modifier = Modifier.align(Alignment.CenterStart),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = if (isEnglish) "Association Membership" else "חברות בעמותה",
                style = KmiTypography.cardTitle,
                color = MaterialTheme.colorScheme.onSurface,
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
                style = KmiTypography.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val textAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color =
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.14f
                        )
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(38.dp)
                                .padding(8.dp),
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
                    style = KmiTypography.cardTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = textAlign,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                color =
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.65f
                    )
            )

            content()
        }
    }
}

@Composable
private fun BirthDatePartField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    maxLength: Int,
    focusRequester: FocusRequester,
    imeAction: ImeAction,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { rawValue ->
            val cleanValue =
                rawValue
                    .filter { character ->
                        character.isDigit()
                    }
                    .take(maxLength)

            onValueChange(cleanValue)

            if (
                cleanValue.length == maxLength &&
                rawValue.length >= value.length
            ) {
                onCompleted()
            }
        },
        modifier =
            modifier
                .heightIn(min = 64.dp)
                .focusRequester(focusRequester),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        label = {
            Text(
                text = label,
                style =
                    KmiTypography.caption.copy(
                        fontWeight =
                            FontWeight.SemiBold
                    ),
                maxLines = 1
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                style = KmiTypography.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        textStyle =
            KmiTypography.body.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = imeAction
            ),
        keyboardActions =
            KeyboardActions(
                onNext = {
                    onCompleted()
                },
                onDone = {
                    onCompleted()
                }
            ),
        colors =
            androidx.compose.material3
                .OutlinedTextFieldDefaults
                .colors(
                    focusedContainerColor =
                        MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor =
                        MaterialTheme.colorScheme.surface,
                    focusedBorderColor =
                        MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor =
                        MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor =
                        MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor =
                        MaterialTheme.colorScheme.onSurface,
                    cursorColor =
                        MaterialTheme.colorScheme.primary
                )
    )
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
    val textAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        label = {
            Text(
                text = label,
                style = KmiTypography.caption,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        },
        leadingIcon =
            if (isEnglish) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                }
            } else {
                null
            },
        trailingIcon =
            if (isEnglish) {
                null
            } else {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                }
            },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(
                    text = placeholder,
                    style = KmiTypography.caption,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.72f
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = keyboardType
            ),
        textStyle =
            KmiTypography.body.copy(
                textAlign = textAlign,
                fontWeight = FontWeight.SemiBold
            ),
        colors =
            androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.45f
                    ),
                unfocusedContainerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.32f
                    ),
                disabledContainerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.22f
                    ),

                focusedBorderColor =
                    MaterialTheme.colorScheme.primary,
                unfocusedBorderColor =
                    MaterialTheme.colorScheme.outlineVariant,
                disabledBorderColor =
                    MaterialTheme.colorScheme.outlineVariant.copy(
                        alpha = 0.45f
                    ),

                focusedTextColor =
                    MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor =
                    MaterialTheme.colorScheme.onSurface,
                disabledTextColor =
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.55f
                    ),

                focusedLabelColor =
                    MaterialTheme.colorScheme.primary,
                unfocusedLabelColor =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.55f
                    ),

                focusedLeadingIconColor =
                    MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.45f
                    ),

                focusedTrailingIconColor =
                    MaterialTheme.colorScheme.primary,
                unfocusedTrailingIconColor =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.45f
                    ),

                cursorColor =
                    MaterialTheme.colorScheme.primary
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
    val labelAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val valueAlign =
        if (isEnglish) {
            TextAlign.Right
        } else {
            TextAlign.Left
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = KmiTypography.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = labelAlign,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style =
                if (emphasize) {
                    KmiTypography.sectionTitle
                } else {
                    KmiTypography.cardTitle
                },
            fontWeight =
                if (emphasize) {
                    FontWeight.Bold
                } else {
                    FontWeight.SemiBold
                },
            color =
                if (emphasize) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            textAlign = valueAlign
        )
    }
}