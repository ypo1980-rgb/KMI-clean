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
import il.kmi.app.ui.KmiTopBar
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
        topBar = {
            KmiTopBar(
                title = title,
                centerTitle = true,

                // מציג את אייקון סרגל הצד מהטופ־בר הגלובלי
                showMenu = true,
                onBack = null,

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
                showTopShare = false
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
            ) {
                // ✅ רק התוכן שמתחת ל־KmiTopBar הגלובלי נגלל
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
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFEAF2FF)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 3.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = if (effectiveEnglish) Alignment.Start else Alignment.End
                            ) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = Color(0xFF1E2A3D),
                                    textAlign = if (effectiveEnglish) TextAlign.Start else TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                HorizontalDivider(color = Color(0xFFBFD0E8))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFF1E2A3D),
                                            textAlign = if (effectiveEnglish) TextAlign.Start else TextAlign.Right,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = if (effectiveEnglish) 34.dp else 0.dp,
                                                    end = if (effectiveEnglish) 0.dp else 34.dp
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
                            containerColor = Color(0xFFEAF2FF)
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
                                        color = Color(0xFF5E6C80)
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
                                        color = Color(0xFF5E6C80)
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
                                    containerColor = Color(0xFF7C5CE6),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFFD8E3F5),
                                    disabledContentColor = Color(0xFF6B778B)
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
    color: Color = Color(0xFF5E6C80)
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold
        )
    )
}

@Composable
private fun contactFieldColors() = OutlinedTextFieldDefaults.colors(
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

    cursorColor = Color(0xFF1E2A3D)
)
