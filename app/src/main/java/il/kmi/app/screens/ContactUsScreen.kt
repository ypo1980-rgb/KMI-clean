package il.kmi.app.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    onSubmit: (
        fullName: String,
        phone: String,
        email: String,
        subject: String,
        message: String
    ) -> Unit = { _, _, _, _, _ -> }
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

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
                        ?: authUser?.displayName
                        ?: ""

                val serverPhone =
                    doc.getString("phone")
                        ?: doc.getString("phoneNumber")
                        ?: doc.getString("mobile")
                        ?: doc.getString("mobilePhone")
                        ?: ""

                val serverEmail =
                    doc.getString("email")
                        ?: authUser?.email
                        ?: ""

                if (fullName.isBlank() && serverFullName.isNotBlank()) {
                    fullName = serverFullName
                }

                if (phone.isBlank() && serverPhone.isNotBlank()) {
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

    val ctx = LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }
    val effectiveEnglish = isEnglish
        ?: (langManager.getCurrentLanguage() == AppLanguage.ENGLISH)

    val title = if (effectiveEnglish) "Contact Us" else "צור קשר"
    val subtitle = if (effectiveEnglish) {
        "Leave your details and the association will get back to you"
    } else {
        "השאירו פרטים ונציג העמותה יחזור אליכם"
    }
    val sendText = if (effectiveEnglish) "Send Request" else "שלח פנייה"

    val isFormValid =
        fullName.isNotBlank() &&
                phone.isNotBlank() &&
                subject.isNotBlank() &&
                message.isNotBlank()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
            ) {
                // ✅ כותרת גלובלית + סרגל אייקונים נשארים קבועים ולא נגללים
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    SideScreenTopBar(
                        title = title,
                        onClose = onClose
                    )
                }

                // ✅ רק התוכן שמתחת לכותרת נגלל
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = if (effectiveEnglish) Alignment.Start else Alignment.End
                ) {
                    AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 4 }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF314875)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 10.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = if (effectiveEnglish) Alignment.Start else Alignment.End
                        ) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.90f),
                                textAlign = if (effectiveEnglish) TextAlign.Start else TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupportAgent,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(
                                            if (effectiveEnglish) Alignment.CenterStart else Alignment.CenterEnd
                                        )
                                    )

                                    Text(
                                        text = if (effectiveEnglish) {
                                            "KAMI representative will contact you soon."
                                        } else {
                                            "נציג מטעם ק.מ.י יחזור אליכם בהקדם."
                                        },
                                        color = Color.White,
                                        textAlign = if (effectiveEnglish) TextAlign.Start else TextAlign.Right,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = if (effectiveEnglish) 38.dp else 0.dp,
                                                end = if (effectiveEnglish) 0.dp else 38.dp
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
                            containerColor = Color(0xFF2A3D66)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 10.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { contactFieldLabel(if (effectiveEnglish) "Full Name" else "שם מלא") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = if (effectiveEnglish) TextAlign.Start else TextAlign.End
                                ),
                                colors = contactFieldColors()
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { contactFieldLabel(if (effectiveEnglish) "Phone Number" else "טלפון") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = if (effectiveEnglish) TextAlign.Start else TextAlign.End
                                ),
                                colors = contactFieldColors()
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { contactFieldLabel(if (effectiveEnglish) "Email" else "אימייל") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = if (effectiveEnglish) TextAlign.Start else TextAlign.End
                                ),
                                colors = contactFieldColors()
                            )

                            OutlinedTextField(
                                value = subject,
                                onValueChange = { subject = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = {
                                    contactFieldLabel(
                                        text = if (effectiveEnglish) "Subject" else "נושא הפנייה",
                                        color = Color(0xFF52627A)
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = if (effectiveEnglish) TextAlign.Start else TextAlign.End
                                ),
                                colors = contactFieldColors()
                            )

                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                label = {
                                    contactFieldLabel(
                                        text = if (effectiveEnglish) "Message" else "הודעה",
                                        color = Color(0xFF52627A)
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = if (effectiveEnglish) TextAlign.Start else TextAlign.End
                                ),
                                colors = contactFieldColors()
                            )

                            Spacer(Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    if (isSubmitting) return@Button

                                    scope.launch {
                                        isSubmitting = true

                                        val cleanFullName = fullName.trim()
                                        val cleanPhone = phone.trim()
                                        val cleanEmail = email.trim()
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
                                    defaultElevation = 6.dp,
                                    pressedElevation = 2.dp
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = Color(0xFF3A5D8F),
                                    disabledContentColor = Color.White.copy(alpha = 0.65f)
                                ),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null
                                    )
                                    Text(
                                        text = if (isSubmitting) {
                                            if (effectiveEnglish) "Sending..." else "שולח..."
                                        } else {
                                            sendText
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } // ✅ סוף התוכן הנגלל
                } // ✅ סוף המסך עם TopBar קבוע
            }
        }
    }
}

@Composable
private fun contactFieldLabel(
    text: String,
    color: Color = Color.White
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    )
}

@Composable
private fun contactFieldColors() = OutlinedTextFieldDefaults.colors(
    // ✅ רקע בהיר יותר מהכרטיס כדי שהשדות יבלטו ולא ייבלעו ברקע
    focusedContainerColor = Color(0xFFF4F7FF),
    unfocusedContainerColor = Color(0xFFEAF0FF),
    disabledContainerColor = Color(0xFFD8E0F5),

    // ✅ מסגרת ברורה יותר במצב רגיל ובפוקוס
    focusedBorderColor = Color(0xFF8EA7FF),
    unfocusedBorderColor = Color.White.copy(alpha = 0.72f),
    disabledBorderColor = Color.White.copy(alpha = 0.35f),

    // ✅ טקסט כהה על רקע בהיר
    focusedTextColor = Color(0xFF111827),
    unfocusedTextColor = Color(0xFF111827),
    disabledTextColor = Color(0xFF111827).copy(alpha = 0.55f),

    // ✅ תוויות ברורות ונקיות
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.92f),
    disabledLabelColor = Color.White.copy(alpha = 0.55f),

    // ✅ אייקונים כהים יותר כדי שיבלטו על הרקע החדש
    focusedLeadingIconColor = Color(0xFF1E3A8A),
    unfocusedLeadingIconColor = Color(0xFF52627A),
    disabledLeadingIconColor = Color(0xFF52627A).copy(alpha = 0.55f),

    cursorColor = Color(0xFF1E3A8A)
)