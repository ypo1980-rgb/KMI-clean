package il.kmi.app.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import il.kmi.app.ui.KmiLanguageDirection
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.pdf.KmiPdfDirection
import il.kmi.shared.localization.AppLanguage
import il.kmi.app.privacy.DemoPrivacy
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.shared.localization.AppLanguageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//===================================================================

private suspend fun persistContactRequestToFirestore(
    fullName: String,
    phone: String,
    email: String,
    subject: String,
    message: String
) {
    val authUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    val cleanFullName = fullName.trim()
    val cleanPhone = phone.trim()
    val cleanEmail = email.trim()
    val cleanSubject = subject.trim()
    val cleanMessage = message.trim()

    if (cleanFullName.isBlank()) error("Missing full name")
    if (cleanPhone.isBlank()) error("Missing phone")
    if (cleanSubject.isBlank()) error("Missing subject")
    if (cleanMessage.isBlank()) error("Missing message")

    val nowMillis = System.currentTimeMillis()

    val contactRef = db.collection("contactRequests").document()
    val notificationRef = db.collection("appNotificationQueue").document()

    val contactData = mapOf(
        "requestId" to contactRef.id,
        "fullName" to cleanFullName,
        "phone" to cleanPhone,
        "email" to cleanEmail,
        "subject" to cleanSubject,
        "message" to cleanMessage,
        "userUid" to authUser?.uid.orEmpty(),
        "userEmail" to authUser?.email.orEmpty(),
        "status" to "open",
        "source" to "android_contact_us",

        // הכנה להתראה עתידית לנציג העמותה
        "notifyEnabled" to true,
        "notifyStatus" to "pending",
        "notifyTargetType" to "association_contact_manager",
        "notifyTargetUid" to "",
        "notifyTargetEmail" to "",
        "notifyTargetPhone" to "",
        "notifyCreatedAtMillis" to nowMillis,

        "createdAt" to FieldValue.serverTimestamp(),
        "createdAtMillis" to nowMillis
    )

    val notificationData = mapOf(
        "notificationId" to notificationRef.id,
        "type" to "contact_request",
        "status" to "pending",
        "source" to "android_contact_us",

        // מי אמור לקבל בעתיד — יוגדר בהמשך
        "targetType" to "association_contact_manager",
        "targetUid" to "",
        "targetEmail" to "",
        "targetPhone" to "",

        // קישור לפנייה
        "contactRequestId" to contactRef.id,
        "relatedCollection" to "contactRequests",
        "relatedDocumentId" to contactRef.id,

        // תוכן ההתראה
        "titleHe" to "פנייה חדשה מהאפליקציה",
        "titleEn" to "New contact request from the app",
        "bodyHe" to "התקבלה פנייה חדשה מאת $cleanFullName בנושא: $cleanSubject",
        "bodyEn" to "A new contact request was received from $cleanFullName regarding: $cleanSubject",

        // Snapshot קצר לצפייה מהירה
        "fullName" to cleanFullName,
        "phone" to cleanPhone,
        "email" to cleanEmail,
        "subject" to cleanSubject,
        "messagePreview" to cleanMessage.take(180),
        "userUid" to authUser?.uid.orEmpty(),
        "userEmail" to authUser?.email.orEmpty(),

        "createdAt" to FieldValue.serverTimestamp(),
        "createdAtMillis" to nowMillis
    )

    val batch = db.batch()
    batch.set(contactRef, contactData)
    batch.set(notificationRef, notificationData)
    batch.commit().await()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(
    isEnglish: Boolean? = null,
    onClose: () -> Unit = {},
    onHome: () -> Unit = {},
    onOpenExercise: ((String) -> Unit)? = null,
    onSubmit: (
        fullName: String,
        phone: String,
        email: String,
        subject: String,
        message: String
    ) -> Unit = { _, _, _, _, _ -> }
) {
    val ctx =
        LocalContext.current

    DemoPrivacy.initialize(
        ctx
    )

    val demoPrivacyEnabled =
        DemoPrivacy.isEnabled()

    val langManager =
        remember(ctx) {
            AppLanguageManager(ctx)
        }

    val effectiveEnglish =
        isEnglish
            ?: (
                    langManager
                        .getCurrentLanguage() ==
                            AppLanguage.ENGLISH
                    )

    var fullName by rememberSaveable {
        mutableStateOf("")
    }

    var realFullName by rememberSaveable {
        mutableStateOf("")
    }

    var phone by rememberSaveable {
        mutableStateOf("")
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var subject by rememberSaveable {
        mutableStateOf("")
    }

    var message by rememberSaveable {
        mutableStateOf("")
    }

    var isSubmitting by rememberSaveable {
        mutableStateOf(false)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // מילוי אוטומטי מפרופיל המשתמש בשרת.
    // נשמרים כמה שמות שדות אפשריים כדי להתאים גם לגרסאות שונות של Firestore.
    LaunchedEffect(Unit) {
        val authUser = FirebaseAuth.getInstance().currentUser
        val uid = authUser?.uid

        if (email.isBlank()) {
            email = authUser?.email.orEmpty()
        }

        if (!uid.isNullOrBlank()) {
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()
            }.onSuccess { doc ->
                val serverFullName =
                    doc.getString("fullName")
                        ?: doc.getString("full_name")
                        ?: doc.getString("name")
                        ?: doc.getString("displayName")
                        ?: authUser.displayName
                        ?: ""

                val displayFullName =
                    TraineeDisplayNameMapper
                        .displayName(
                            realName = serverFullName,
                            stableKey =
                                uid.ifBlank {
                                    serverFullName
                                },
                            demoIndex = 0,
                            isEnglish =
                                effectiveEnglish
                        )
                        .ifBlank {
                            if (effectiveEnglish) {
                                "Trainee 1"
                            } else {
                                "מתאמן 1"
                            }
                        }

                val serverPhone =
                    doc.getString("phone")
                        ?: doc.getString("phoneNumber")
                        ?: doc.getString("mobile")
                        ?: doc.getString("mobilePhone")
                        ?: ""

                val serverEmail =
                    doc.getString("email")
                        ?: authUser.email
                        ?: ""

                if (
                    realFullName.isBlank() &&
                    serverFullName.isNotBlank()
                ) {
                    realFullName =
                        serverFullName
                }

                if (
                    fullName.isBlank() &&
                    serverFullName.isNotBlank()
                ) {
                    fullName =
                        if (demoPrivacyEnabled) {
                            displayFullName
                        } else {
                            serverFullName
                        }
                }

                if (
                    phone.isBlank() &&
                    serverPhone.isNotBlank()
                ) {
                    phone = serverPhone
                }

                if (email.isBlank() && serverEmail.isNotBlank()) {
                    email = serverEmail
                }
            }.onFailure {
                // Prefill is optional. The user can still fill the form manually.
            }
        }
    }

    val title = if (effectiveEnglish) "Contact Us" else "צור קשר"
    val subtitle = if (effectiveEnglish) {
        "Leave your details and the association will get back to you"
    } else {
        "השאירו פרטים ונציג העמותה יחזור אליכם"
    }
    val sendText =
        if (effectiveEnglish) "Send Request" else "שלח פנייה"

    val cardContainerColor =
        MaterialTheme.colorScheme.surface

    val innerContainerColor =
        MaterialTheme.colorScheme.surfaceVariant

    val isFormValid =
        fullName.isNotBlank() &&
                phone.isNotBlank() &&
                subject.isNotBlank() &&
                message.isNotBlank()

    KmiLanguageDirection(
        isEnglish = effectiveEnglish
    ) {
        Scaffold(
            topBar = {
                KmiTopBar(
                    title = title,
                    centerTitle = true,

                    // מציג את אייקון סרגל הצד מהטופ־בר הגלובלי
                    showMenu = true,
                    onBack = onClose,

// מפעיל את אייקון הבית בסרגל האייקונים הצדדי
                    onHome = onHome,

                    // מאפשר לחיצה על תוצאת חיפוש, אם המסך שמעל מעביר ניווט לתרגיל
                    onOpenExercise = onOpenExercise,

                    showBottomActions = true,

                    // מציג את מצב המשתמש הגלובלי: מתאמן / מאמן
                    showRoleBadge = true,
                    showModePill = true,

                    // חובה להיות false כדי שאייקון החיפוש בסרגל הצדדי יעבוד
                    lockSearch = false,

                    // הבית לא נעול במסך צור קשר
                    lockHome = false,

                    // שלא יופיעו אייקוני בית/חיפוש בכותרת העליונה עצמה,
                    // אלא רק בסרגל האייקונים הצדדי כמו בשאר המסכים
                    showTopHome = false,
                    showTopSearch = false,
                    showTopShare = true,

                    onShare = {
                        shareContactUsPdf(
                            context = ctx,
                            fullName = fullName,
                            phone =
                                if (demoPrivacyEnabled) {
                                    ""
                                } else {
                                    phone
                                },
                            email =
                                if (demoPrivacyEnabled) {
                                    ""
                                } else {
                                    email
                                },
                            subject = subject,
                            message = message,
                            isEnglish = effectiveEnglish
                        )
                    }
                )
            },
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                ) {
                    // ✅ רק התוכן שמתחת ל־KmiTopBar הגלובלי נגלל
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = 16.dp,
                                vertical = 14.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(16.dp),
                        horizontalAlignment =
                            Alignment.Start
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 4 }
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardContainerColor,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 1.dp,
                                    pressedElevation = 0.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 14.dp,
                                            vertical = 10.dp
                                        ),
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp),
                                    horizontalAlignment =
                                        Alignment.Start
                                ) {
                                    Text(
                                        text = subtitle,
                                        style =
                                            KmiTypography.cardTitle.copy(
                                                fontWeight =
                                                    FontWeight.ExtraBold
                                            ),
                                        color =
                                            MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.28f
                                        )
                                    )

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = innerContainerColor,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector =
                                                    Icons.Default.SupportAgent,
                                                contentDescription = null,
                                                tint =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primary,
                                                modifier = Modifier
                                                    .align(
                                                        Alignment.CenterStart
                                                    )
                                                    .size(
                                                        KmiIconSize.medium
                                                    )
                                            )

                                            Text(
                                                text =
                                                    if (effectiveEnglish) {
                                                        "KAMI representative will contact you soon."
                                                    } else {
                                                        "נציג מטעם ק.מ.י יחזור אליכם בהקדם."
                                                    },
                                                style =
                                                    KmiTypography.body.copy(
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    ),
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurface,
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        start = 34.dp
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 4 }
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(
                                    containerColor = cardContainerColor,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 1.dp,
                                    pressedElevation = 0.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 18.dp,
                                            vertical = 16.dp
                                        ),
                                    verticalArrangement =
                                        Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        label = { ContactFieldLabel(if (effectiveEnglish) "Full Name" else "שם מלא") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(KmiIconSize.medium)
                                            )
                                        },
                                        textStyle = KmiTypography.body.copy(
                                            textAlign = TextAlign.Start
                                        ),
                                        colors = contactFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        label = { ContactFieldLabel(if (effectiveEnglish) "Phone Number" else "טלפון") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(
                                                    KmiIconSize.medium
                                                )
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone
                                        ),
                                        textStyle = KmiTypography.body.copy(
                                            textAlign = TextAlign.Start
                                        ),
                                        colors = contactFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        label = { ContactFieldLabel(if (effectiveEnglish) "Email" else "אימייל") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                modifier = Modifier.size(
                                                    KmiIconSize.medium
                                                )
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email
                                        ),
                                        textStyle = KmiTypography.body.copy(
                                            textAlign = TextAlign.Start
                                        ),
                                        colors = contactFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = subject,
                                        onValueChange = { subject = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        label = {
                                            ContactFieldLabel(
                                                text =
                                                    if (effectiveEnglish) {
                                                        "Subject"
                                                    } else {
                                                        "נושא הפנייה"
                                                    }
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector =
                                                    Icons.AutoMirrored.Filled.Message,
                                                contentDescription = null,
                                                modifier = Modifier.size(
                                                    KmiIconSize.medium
                                                )
                                            )
                                        },
                                        textStyle = KmiTypography.body.copy(
                                            textAlign = TextAlign.Start
                                        ),
                                        colors = contactFieldColors()
                                    )

                                    OutlinedTextField(
                                        value = message,
                                        onValueChange = { message = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 4,
                                        label = {
                                            ContactFieldLabel(
                                                text =
                                                    if (effectiveEnglish) {
                                                        "Message"
                                                    } else {
                                                        "הודעה"
                                                    }
                                            )
                                        },
                                        textStyle = KmiTypography.body.copy(
                                            textAlign = TextAlign.Start
                                        ),
                                        colors = contactFieldColors()
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            if (isSubmitting) return@Button

                                            scope.launch {
                                                isSubmitting = true

                                                val cleanFullName =
                                                    if (
                                                        demoPrivacyEnabled &&
                                                        realFullName.isNotBlank()
                                                    ) {
                                                        realFullName.trim()
                                                    } else {
                                                        fullName.trim()
                                                    }

                                                val cleanPhone =
                                                    phone.trim()

                                                val cleanEmail =
                                                    email.trim()
                                                val cleanSubject = subject.trim()
                                                val cleanMessage = message.trim()

                                                runCatching {
                                                    persistContactRequestToFirestore(
                                                        fullName = cleanFullName,
                                                        phone = cleanPhone,
                                                        email = cleanEmail,
                                                        subject = cleanSubject,
                                                        message = cleanMessage
                                                    )
                                                }.onSuccess {
                                                    onSubmit(
                                                        cleanFullName,
                                                        cleanPhone,
                                                        cleanEmail,
                                                        cleanSubject,
                                                        cleanMessage
                                                    )

                                                    snackbarHostState.showSnackbar(
                                                        if (effectiveEnglish)
                                                            "Your request was sent successfully"
                                                        else
                                                            "הפנייה נשלחה בהצלחה"
                                                    )

                                                    fullName = ""
                                                    phone = ""
                                                    email = ""
                                                    subject = ""
                                                    message = ""
                                                }.onFailure {
                                                    snackbarHostState.showSnackbar(
                                                        if (effectiveEnglish)
                                                            "Sending failed. Please try again."
                                                        else
                                                            "שליחת הפנייה נכשלה. נסה שוב."
                                                    )
                                                }

                                                isSubmitting = false
                                            }
                                        },
                                        enabled = isFormValid && !isSubmitting,
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = ButtonDefaults.buttonElevation(
                                            defaultElevation = 1.dp,
                                            pressedElevation = 0.dp
                                        ),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor =
                                                MaterialTheme.colorScheme.primary,
                                            contentColor =
                                                MaterialTheme.colorScheme.onPrimary,
                                            disabledContainerColor =
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            disabledContentColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.62f
                                                )
                                        ),
                                        shape = MaterialTheme.shapes.extraLarge
                                    ) {
                                        androidx.compose.foundation.layout.Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(KmiIconSize.medium)
                                            )
                                            Text(
                                                text = if (isSubmitting) {
                                                    if (effectiveEnglish) "Sending..." else "שולח..."
                                                } else {
                                                    sendText
                                                },
                                                style = KmiTypography.action,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } // סוף כרטיס הטופס
                    } // סוף התוכן הנגלל
                } // סוף העמודה הראשית
            } // סוף רקע המסך
        } // סוף Scaffold
    } // סוף KmiLanguageDirection
}

@Composable
private fun ContactFieldLabel(
    text: String,
    color: Color =
        MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        color = color,
        style =
            KmiTypography.caption.copy(
                fontWeight =
                    FontWeight.ExtraBold
            )
    )
}

@Composable
private fun contactFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor =
        MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor =
        MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor =
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),

    focusedBorderColor =
        MaterialTheme.colorScheme.primary,
    unfocusedBorderColor =
        MaterialTheme.colorScheme.outline.copy(alpha = 0.60f),
    disabledBorderColor =
        MaterialTheme.colorScheme.outline.copy(alpha = 0.36f),

    focusedTextColor =
        MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor =
        MaterialTheme.colorScheme.onSurface,
    disabledTextColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),

    focusedLabelColor =
        MaterialTheme.colorScheme.primary,
    unfocusedLabelColor =
        MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor =
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),

    focusedLeadingIconColor =
        MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor =
        MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLeadingIconColor =
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),

    cursorColor = MaterialTheme.colorScheme.primary
)

//===================================================================
// PDF — Contact Us
//===================================================================

private fun shareContactUsPdf(
    context: Context,
    fullName: String,
    phone: String,
    email: String,
    subject: String,
    message: String,
    isEnglish: Boolean
) {
    val pdfFile =
        createContactUsPdf(
            context = context,
            fullName = fullName,
            phone = phone,
            email = email,
            subject = subject,
            message = message,
            isEnglish = isEnglish
        )

    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isEnglish) {
                    "KAMI Contact Request"
                } else {
                    "פנייה לק.מ.י"
                }
            )

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    context.startActivity(
        Intent.createChooser(
            sendIntent,
            if (isEnglish) {
                "Share PDF"
            } else {
                "שיתוף PDF"
            }
        )
    )
}

private fun createContactUsPdf(
    context: Context,
    fullName: String,
    phone: String,
    email: String,
    subject: String,
    message: String,
    isEnglish: Boolean
): File {

    val pageWidth = 595
    val pageHeight = 842

    val margin = 30f

    val document = PdfDocument()

    val navy =
        android.graphics.Color.rgb(
            2,
            43,
            74
        )

    val blue =
        android.graphics.Color.rgb(
            36,
            103,
            158
        )

    val lightBlue =
        android.graphics.Color.rgb(
            234,
            246,
            255
        )

    val borderBlue =
        android.graphics.Color.rgb(
            191,
            213,
            232
        )

    val textDark =
        android.graphics.Color.rgb(
            15,
            23,
            42
        )

    val textMuted =
        android.graphics.Color.rgb(
            80,
            100,
            120
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

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regularTypeface,
        align: Paint.Align =
            KmiPdfDirection.textAlign(
                isEnglish = isEnglish
            )
    ): Paint {
        return Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            textSize = size
            this.color = color
            this.typeface = typeface
            textAlign = align
        }
    }

    fun drawRoundedRect(
        canvas: android.graphics.Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 10f
    ) {
        val rectPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
            }

        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radius,
            radius,
            rectPaint
        )
    }

    fun drawRoundedBorder(
        canvas: android.graphics.Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 10f
    ) {
        val rectPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
            }

        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radius,
            radius,
            rectPaint
        )
    }

    var pageNumber = 0
    lateinit var currentPage: PdfDocument.Page
    lateinit var pageCanvas: android.graphics.Canvas
    var hasActivePage = false
    var y = 0f

    fun currentCanvas(): android.graphics.Canvas =
        pageCanvas

    fun newPage() {
        if (hasActivePage) {
            document.finishPage(currentPage)
        }

        pageNumber++

        val pageInfo =
            PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()

        currentPage =
            document.startPage(
                pageInfo
            )

        pageCanvas = currentPage.canvas
        hasActivePage = true

        pageCanvas.drawColor(
            android.graphics.Color.WHITE
        )

        // =========================================================
        // Header — זהה ל-PDF של הסטטיסטיקה / מסך הבית
        // =========================================================

        val headerBottom = 122f

        val navyPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = navy
            }

        val accent1 =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.rgb(
                        36,
                        103,
                        158
                    )
            }

        val accent2 =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.rgb(
                        128,
                        183,
                        220
                    )
            }

        currentCanvas().drawPath(
            android.graphics.Path().apply {
                moveTo(
                    pageWidth.toFloat(),
                    0f
                )
                lineTo(
                    pageWidth.toFloat(),
                    headerBottom
                )
                lineTo(
                    178f,
                    headerBottom
                )
                lineTo(
                    238f,
                    0f
                )
                close()
            },
            navyPaint
        )

        currentCanvas().drawPath(
            android.graphics.Path().apply {
                moveTo(
                    208f,
                    headerBottom
                )
                lineTo(
                    224f,
                    headerBottom
                )
                lineTo(
                    284f,
                    0f
                )
                lineTo(
                    268f,
                    0f
                )
                close()
            },
            accent1
        )

        currentCanvas().drawPath(
            android.graphics.Path().apply {
                moveTo(
                    230f,
                    headerBottom
                )
                lineTo(
                    238f,
                    headerBottom
                )
                lineTo(
                    298f,
                    0f
                )
                lineTo(
                    290f,
                    0f
                )
                close()
            },
            accent2
        )

        // לוגו KAMI
        val logoX = 78f
        val logoY = 58f
        val logoRadius = 42f

        currentCanvas().drawCircle(
            logoX,
            logoY,
            logoRadius,
            navyPaint
        )

        currentCanvas().drawCircle(
            logoX,
            logoY,
            logoRadius - 4f,
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.WHITE
            }
        )

        currentCanvas().drawText(
            "KAMI",
            logoX,
            logoY + logoRadius * 0.22f,
            paint(
                size = logoRadius * 0.62f,
                color = navy,
                typeface = boldTypeface,
                align = Paint.Align.CENTER
            )
        )

        val headerX =
            pageWidth - 34f

        currentCanvas().drawText(
            if (isEnglish) {
                "Contact Us"
            } else {
                "צור קשר"
            },
            headerX,
            52f,
            paint(
                size = 25f,
                color =
                    android.graphics.Color.WHITE,
                typeface = boldTypeface,
                align = Paint.Align.RIGHT
            )
        )

        currentCanvas().drawText(
            if (isEnglish) {
                "KAMI Contact Request"
            } else {
                "פנייה לעמותת ק.מ.י"
            },
            headerX,
            78f,
            paint(
                size = 11f,
                color =
                    android.graphics.Color.WHITE,
                typeface = regularTypeface,
                align = Paint.Align.RIGHT
            )
        )

        val generatedDate =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(
                Date()
            )

        currentCanvas().drawText(
            if (isEnglish) {
                "Generated: $generatedDate"
            } else {
                "תאריך הפקה: $generatedDate"
            },
            headerX,
            142f,
            paint(
                size = 8.5f,
                color = textMuted,
                typeface = regularTypeface,
                align = Paint.Align.RIGHT
            )
        )

        y = 170f
    }

    fun ensureSpace(
        requiredHeight: Float
    ) {
        if (
            y + requiredHeight >
            pageHeight - 35f
        ) {
            newPage()
        }
    }

    fun drawField(
        label: String,
        value: String
    ) {
        if (value.isBlank()) {
            return
        }

        ensureSpace(62f)

        val right =
            pageWidth.toFloat() - margin

        val top = y
        val bottom = y + 50f

        drawRoundedRect(
            canvas = currentCanvas(),
            left = margin,
            top = top,
            right = right,
            bottom = bottom,
            color = lightBlue
        )

        drawRoundedBorder(
            canvas = currentCanvas(),
            left = margin,
            top = top,
            right = right,
            bottom = bottom,
            color = borderBlue
        )

        val textX =
            KmiPdfDirection.startPaddingX(
                isEnglish = isEnglish,
                left = margin,
                right = right,
                padding = 12f
            )

        currentCanvas().drawText(
            label,
            textX,
            top + 17f,
            paint(
                size = 9f,
                color = textMuted,
                typeface = boldTypeface
            )
        )

        currentCanvas().drawText(
            value,
            textX,
            top + 37f,
            paint(
                size = 11f,
                color = textDark,
                typeface = regularTypeface
            )
        )

        y += 60f
    }

    fun drawWrappedText(
        text: String,
        maxCharsPerLine: Int
    ) {
        val cleanText =
            text.trim()

        if (cleanText.isBlank()) {
            return
        }

        val words =
            cleanText.split(
                Regex("\\s+")
            )

        val lines =
            mutableListOf<String>()

        var currentLine = ""

        words.forEach { word ->
            val candidate =
                if (currentLine.isBlank()) {
                    word
                } else {
                    "$currentLine $word"
                }

            if (
                candidate.length >
                maxCharsPerLine &&
                currentLine.isNotBlank()
            ) {
                lines += currentLine
                currentLine = word
            } else {
                currentLine = candidate
            }
        }

        if (currentLine.isNotBlank()) {
            lines += currentLine
        }

        lines.forEach { line ->
            ensureSpace(18f)

            currentCanvas().drawText(
                line,
                KmiPdfDirection.startPaddingX(
                    isEnglish = isEnglish,
                    left = margin,
                    right =
                        pageWidth.toFloat() - margin,
                    padding = 12f
                ),
                y,
                paint(
                    size = 10.5f,
                    color = textDark
                )
            )

            y += 16f
        }
    }

    newPage()

    currentCanvas().drawText(
        if (isEnglish) {
            "Request details"
        } else {
            "פרטי הפנייה"
        },
        KmiPdfDirection.startX(
            isEnglish = isEnglish,
            left = margin,
            right =
                pageWidth.toFloat() - margin
        ),
        y,
        paint(
            size = 16f,
            color = blue,
            typeface = boldTypeface
        )
    )

    y += 22f

    drawField(
        label =
            if (isEnglish) {
                "Full Name"
            } else {
                "שם מלא"
            },
        value = fullName.trim()
    )

    drawField(
        label =
            if (isEnglish) {
                "Phone"
            } else {
                "טלפון"
            },
        value = phone.trim()
    )

    drawField(
        label =
            if (isEnglish) {
                "Email"
            } else {
                "אימייל"
            },
        value = email.trim()
    )

    drawField(
        label =
            if (isEnglish) {
                "Subject"
            } else {
                "נושא הפנייה"
            },
        value = subject.trim()
    )

    ensureSpace(80f)

    currentCanvas().drawText(
        if (isEnglish) {
            "Message"
        } else {
            "הודעה"
        },
        KmiPdfDirection.startX(
            isEnglish = isEnglish,
            left = margin,
            right =
                pageWidth.toFloat() - margin
        ),
        y,
        paint(
            size = 13f,
            color = blue,
            typeface = boldTypeface
        )
    )

    y += 22f

    drawWrappedText(
        text = message,
        maxCharsPerLine =
            if (isEnglish) {
                78
            } else {
                62
            }
    )

    if (hasActivePage) {
        document.finishPage(currentPage)
    }

    val pdfDirectory =
        File(
            context.cacheDir,
            "shared_pdfs"
        ).apply {
            mkdirs()
        }

    /*
     * שם קבוע לפי השפה:
     * הפקה חדשה באותה שפה מחליפה את הקובץ הקודם.
     */
    val fileName =
        if (isEnglish) {
            "Contact Us.pdf"
        } else {
            "צור קשר.pdf"
        }

    val pdfFile =
        File(
            pdfDirectory,
            fileName
        )

    try {
        FileOutputStream(
            pdfFile,
            false
        ).use { outputStream ->
            document.writeTo(outputStream)
        }
    } finally {
        document.close()
    }

    return pdfFile
}