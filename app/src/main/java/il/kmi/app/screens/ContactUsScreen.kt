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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            }.onFailure { error ->
                android.util.Log.e(
                    "KMI_CONTACT",
                    "Failed to load contact profile from Firestore: ${error.message}",
                    error
                )
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
                                            "K.M.I representative will contact you soon."
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
                                colors = contactFieldColors()
                            )

                            Spacer(Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    onSubmit(
                                        fullName.trim(),
                                        phone.trim(),
                                        email.trim(),
                                        subject.trim(),
                                        message.trim()
                                    )

                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (effectiveEnglish)
                                                "Your request was sent successfully"
                                            else
                                                "הפנייה נשלחה בהצלחה"
                                        )
                                    }

                                    fullName = ""
                                    phone = ""
                                    email = ""
                                    subject = ""
                                    message = ""
                                },
                                enabled = isFormValid,
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
                                        text = sendText,
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