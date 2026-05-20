@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package il.kmi.app.screens.registration

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import il.kmi.shared.domain.Belt
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.database.KmiDatabaseProvider
import il.kmi.app.ui.ext.color

private data class TraineeRankOption(
    val id: String,
    val heb: String,
    val color: Color
)

private fun traineeRankOptions(): List<TraineeRankOption> {
    return listOf(
        TraineeRankOption(
            id = "white",
            heb = "לבנה",
            color = Belt.WHITE.color
        ),
        TraineeRankOption(
            id = "yellow",
            heb = "צהובה",
            color = Belt.YELLOW.color
        ),
        TraineeRankOption(
            id = "orange",
            heb = "כתומה",
            color = Belt.ORANGE.color
        ),
        TraineeRankOption(
            id = "green",
            heb = "ירוקה",
            color = Belt.GREEN.color
        ),
        TraineeRankOption(
            id = "blue",
            heb = "כחולה",
            color = Belt.BLUE.color
        ),
        TraineeRankOption(
            id = "brown",
            heb = "חומה",
            color = Belt.BROWN.color
        ),
        TraineeRankOption(
            id = "black",
            heb = "שחורה דאן 1",
            color = Belt.BLACK.color
        ),
        TraineeRankOption(
            id = "black_dan_2",
            heb = "שחורה דאן 2",
            color = Belt.BLACK.color
        ),
        TraineeRankOption(
            id = "black_dan_3",
            heb = "שחורה דאן 3",
            color = Belt.BLACK.color
        ),
        TraineeRankOption(
            id = "black_dan_4",
            heb = "שחורה דאן 4",
            color = Belt.BLACK.color
        ),
        TraineeRankOption(
            id = "black_dan_5",
            heb = "שחורה דאן 5",
            color = Belt.BLACK.color
        ),
        TraineeRankOption(
            id = "black_dan_6",
            heb = "שחורה דאן 6",
            color = Belt.BLACK.color
        ),
        TraineeRankOption(
            id = "black_dan_7",
            heb = "שחורה דאן 7",
            color = Belt.BLACK.color
        ),
        TraineeRankOption(
            id = "black_dan_8",
            heb = "שחורה דאן 8",
            color = Belt.BLACK.color
        ),
        TraineeRankOption(
            id = "black_dan_9",
            heb = "שחורה דאן 9",
            color = Belt.BLACK.color
        ),
        TraineeRankOption(
            id = "black_dan_10",
            heb = "שחורה דאן 10",
            color = Belt.BLACK.color
        )
    )
}

@Composable
fun RegistrationFormContent(
    isCoach: Boolean,
    isEnglish: Boolean,
    isGoogleAuth: Boolean = false,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    fullNameError: Boolean,
    phone: String,
    onPhoneChange: (String) -> Unit,
    phoneError: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: Boolean,
    // 👇 חדש – מין
    gender: String,
    onGenderChange: (String) -> Unit,
    genderError: Boolean,
    birthDay: Int,
    birthMonth: Int,
    birthYear: Int,
    onBirthDayChange: (Int) -> Unit,
    onBirthMonthChange: (Int) -> Unit,
    onBirthYearChange: (Int) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    usernameError: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordError: Boolean,
    selectedRegion: String,
    onRegionChange: (String) -> Unit,
    selectedBranches: List<String>,
    onBranchesChange: (List<String>) -> Unit,
    selectedGroups: List<String>,
    onGroupsChange: (List<String>) -> Unit,
    regionError: Boolean,
    branchError: Boolean,
    groupError: Boolean,
    currentBeltId: String,
    onBeltChange: (String) -> Unit,
    subscribeSms: Boolean,
    onSubscribeSmsChange: (Boolean) -> Unit,
    acceptedTerms: Boolean,
    onAcceptedTermsChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    branchType: String,
    onBranchTypeChange: (String) -> Unit,
    submitButtonText: String? = null,
    onSubmitRegistration: () -> Unit,
) {
    val scroll = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val fieldTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val fieldTextDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    val screenLayoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    val allGroupsAcrossBranches by remember(ctx, selectedBranches) {
        derivedStateOf {
            selectedBranches
                .flatMap { branch ->
                    val key = branch.trim()

                    val dbGroups = KmiDatabaseProvider
                        .branchByName(ctx, key)
                        ?.trainingDays
                        ?.map { it.groupHe }
                        ?.filter { it.isNotBlank() }
                        ?.distinct()
                        .orEmpty()

                    if (dbGroups.isNotEmpty()) {
                        dbGroups
                    } else {
                        TrainingCatalog.ageGroupsByBranch[key]
                            ?: TrainingCatalog.ageGroupsByBranch[key.replace("’", "'")]
                            ?: TrainingCatalog.ageGroupsByBranch[key.replace("־", "-")]
                            ?: emptyList()
                    }
                }
                .distinct()
        }
    }

    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodySmall,
        LocalLayoutDirection provides screenLayoutDirection
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ===== Personal =====
            RegistrationSectionCard(
                title = tr("פרטים אישיים", "Personal details"),
                isEnglish = isEnglish
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { onFullNameChange(it) },
                    label = { Text(tr("שם מלא", "Full name"), color = Color.Black) },
                    singleLine = true,
                    isError = fullNameError,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = fieldTextAlign,
                        textDirection = fieldTextDirection
                    ),
                    modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 46.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            if (fullNameError) {
                Text(tr("שדה חובה", "Required field"), color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { onPhoneChange(it) },
                label = { Text(tr("טלפון", "Phone"), color = Color.Black) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 46.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.medium),
                textStyle = LocalTextStyle.current.copy(
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Left
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            if (phoneError) {
                Text(tr("שדה חובה", "Required field"), color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = email,
                onValueChange = { onEmailChange(it) },
                label = { Text(tr("מייל", "Email"), color = Color.Black) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailError,
                textStyle = LocalTextStyle.current.copy(
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Left
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 46.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )
            if (emailError) {
                Text(
                    tr("שדה חובה / פורמט מייל לא תקין", "Required field / invalid email format"),
                    color = MaterialTheme.colorScheme.error
                )
            }

                Text(
                    text = tr("מין המשתמש", "Gender"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = fieldTextAlign,
                    modifier = Modifier.fillMaxWidth()
                )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = gender == "male",
                    onClick = { onGenderChange("male") },
                    label = {
                        Text(
                            tr("זכר", "Male"),
                            textAlign = TextAlign.Center,
                            color = if (gender == "male") Color.White else Color(0xFF475569),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = gender == "male",
                        borderColor = Color(0xFFD2C4E3),
                        selectedBorderColor = Color(0xFF0EA5E9),
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        selectedContainerColor = Color(0xFF0EA5E9),
                        labelColor = Color(0xFF475569),
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = gender == "female",
                    onClick = { onGenderChange("female") },
                    label = {
                        Text(
                            tr("נקבה", "Female"),
                            textAlign = TextAlign.Center,
                            color = if (gender == "female") Color.White else Color(0xFF475569),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = gender == "female",
                        borderColor = Color(0xFFD2C4E3),
                        selectedBorderColor = Color(0xFFEC4899),
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        selectedContainerColor = Color(0xFFEC4899),
                        labelColor = Color(0xFF475569),
                        selectedLabelColor = Color.White
                    )
                )
            }
            if (genderError) {
                Text(tr("יש לבחור מין", "Please select gender"), color = MaterialTheme.colorScheme.error)
            }

                Text(
                    text = tr("תאריך לידה", "Date of birth"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = fieldTextAlign,
                    modifier = Modifier.fillMaxWidth()
                )

                BirthDatePicker(
                    year = birthYear,
                    month = birthMonth,
                    day = birthDay,
                    isEnglish = isEnglish,
                    onYearChange = { onBirthYearChange(it) },
                    onMonthChange = { onBirthMonthChange(it) },
                    onDayChange = { onBirthDayChange(it) }
                )
            }

        // ===== Account =====
        // בכניסה עם Google אין צורך להציג שם משתמש / סיסמה.
        if (!isGoogleAuth) {
            RegistrationSectionCard(
                title = tr("חשבון משתמש", "User account"),
                isEnglish = isEnglish
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { onUsernameChange(it) },
                    label = { Text(tr("שם משתמש", "Username"), color = Color.Black) },
                    singleLine = true,
                    isError = usernameError,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = fieldTextAlign,
                        textDirection = fieldTextDirection
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .background(Color.White, shape = MaterialTheme.shapes.medium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )
                if (usernameError) {
                    Text(tr("שדה חובה", "Required field"), color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { onPasswordChange(it) },
                    label = { Text(tr("סיסמה", "Password"), color = Color.Black) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        val icon =
                            if (passwordVisible) androidx.compose.material.icons.Icons.Filled.VisibilityOff
                            else androidx.compose.material.icons.Icons.Filled.Visibility
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = null, tint = Color.Black)
                        }
                    },
                    isError = passwordError,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = fieldTextAlign,
                        textDirection = fieldTextDirection
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .background(Color.White, shape = MaterialTheme.shapes.medium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )
                if (passwordError) {
                    Text(tr("שדה חובה", "Required field"), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // ===== Branch / Group / Belt =====
            RegistrationSectionCard(
                title = tr("שיוך לסניף", "Branch assignment"),
                isEnglish = isEnglish
            ) {
                Text(
                    text = tr("בחירת סוג סניף", "Branch type"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = fieldTextAlign,
                    modifier = Modifier.fillMaxWidth()
                )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = branchType == "israel",
                    onClick = {
                        onBranchTypeChange("israel")
                        onRegionChange("")
                        onBranchesChange(emptyList())
                        onGroupsChange(emptyList())
                    },
                    label = {
                        Text(
                            tr("ישראל", "Israel"),
                            color = if (branchType == "israel") Color.White else Color(0xFF475569)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = branchType == "israel",
                        borderColor = Color(0xFFD2C4E3),
                        selectedBorderColor = Color(0xFF6C4DFF),
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        selectedContainerColor = Color(0xFF7C4DFF),
                        labelColor = Color(0xFF475569),
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = branchType == "abroad",
                    onClick = {
                        onBranchTypeChange("abroad")
                        onRegionChange("")
                        onBranchesChange(emptyList())
                        onGroupsChange(emptyList())
                    },
                    label = {
                        Text(
                            tr("חו״ל", "Abroad"),
                            color = if (branchType == "abroad") Color.White else Color(0xFF475569)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = branchType == "abroad",
                        borderColor = Color(0xFFD2C4E3),
                        selectedBorderColor = Color(0xFF6C4DFF),
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        selectedContainerColor = Color(0xFF7C4DFF),
                        labelColor = Color(0xFF475569),
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(Modifier.height(6.dp))

                RegionAndMultiBranchPicker(
                    branchType = branchType,
                    selectedRegion = selectedRegion,
                    selectedBranches = selectedBranches,
                    onRegionChange = onRegionChange,
                    onBranchesConfirm = onBranchesChange,
                    onGroupsChange = onGroupsChange,
                    regionError = regionError,
                    branchError = branchError,
                    isEnglish = isEnglish
                )

            if (branchType == "israel" && selectedBranches.isNotEmpty()) {
                MultiGroupsPicker(
                    allGroupsAcrossBranches = allGroupsAcrossBranches,
                    selectedGroups = selectedGroups,
                    onGroupsChange = onGroupsChange,
                    groupError = groupError,
                    isEnglish = isEnglish
                )
            }

                BeltPicker(
                    currentBeltId = currentBeltId,
                    onBeltChange = onBeltChange,
                    isEnglish = isEnglish
                )
            }

            // ===== Preferences =====
            RegistrationSectionCard(
                title = tr("העדפות ואישורים", "Preferences and approvals"),
                isEnglish = isEnglish
            ) {
                Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = subscribeSms,
                    onCheckedChange = onSubscribeSmsChange
                )
                Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr(
                            "ארצה לקבל עדכונים בהודעת SMS לגבי אימונים קרובים",
                            "I would like to receive SMS updates about upcoming trainings"
                        ),
                        textAlign = fieldTextAlign,
                        modifier = Modifier.weight(1f)
                    )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = onAcceptedTermsChange
                )
                Spacer(Modifier.width(8.dp))

                val interaction = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { onOpenTerms() }
                ) {
                    Text(
                        text = tr(
                            "אני מאשר את תנאי השימוש ומדיניות הפרטיות",
                            "I approve the Terms of Use and Privacy Policy"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = fieldTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = tr("קרא עוד", "Read more"),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = fieldTextAlign,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )
                }
            }
        }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onSubmitRegistration,
                enabled = acceptedTerms,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = Color(0xFFB0BEC5),
                    disabledContentColor = Color.Black
                )
            ) {
                Text(
                    text = submitButtonText ?: tr("סיום רישום", "Complete registration"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

    @Composable
    private fun RegistrationSectionCard(
        title: String,
        modifier: Modifier = Modifier,
        isEnglish: Boolean = false,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF4ECF8).copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFD9CCE7))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF1F2937),
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFD9CCE7))
            )

            content()
        }
    }
}

@Composable
private fun registrationLightFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    errorContainerColor = Color.White,

    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    disabledTextColor = Color.Black.copy(alpha = 0.78f),
    errorTextColor = Color.Black,

    focusedLabelColor = Color(0xFF374151),
    unfocusedLabelColor = Color(0xFF475569),
    disabledLabelColor = Color(0xFF64748B),
    errorLabelColor = MaterialTheme.colorScheme.error,

    focusedPlaceholderColor = Color(0xFF64748B),
    unfocusedPlaceholderColor = Color(0xFF64748B),
    disabledPlaceholderColor = Color(0xFF94A3B8),

    focusedBorderColor = Color(0xFF7C4DFF),
    unfocusedBorderColor = Color(0xFFD2C4E3),
    disabledBorderColor = Color(0xFFD2C4E3),
    errorBorderColor = MaterialTheme.colorScheme.error,

    cursorColor = Color(0xFF7C4DFF)
)

@Composable
private fun RegistrationSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    isEnglish: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF1F2937),
            fontWeight = FontWeight.SemiBold,
            textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFB7A9C8).copy(alpha = 0.55f))
        )
    }
}

@Composable
private fun BirthDatePicker(
    year: Int,
    month: Int,
    day: Int,
    isEnglish: Boolean = false,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
) {
    // מציגים דו-ספרתי/ארבע-ספרתי
    var dayText by remember(day) { mutableStateOf(day.coerceIn(1, 31).toString().padStart(2, '0')) }
    var monthText by remember(month) { mutableStateOf(month.coerceIn(1, 12).toString().padStart(2, '0')) }
    var yearText by remember(year) { mutableStateOf(year.coerceIn(1950, 2026).toString().padStart(4, '0')) }

    // צבעים קבועים כדי שהשדות יהיו קריאים גם במצב כהה
    val shape = RoundedCornerShape(14.dp)
    val fieldColors = registrationLightFieldColors()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // יום (2 ספרות)
        OutlinedTextField(
            value = dayText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(2)
                dayText = digits
                digits.toIntOrNull()?.let { v ->
                    if (v in 1..31) onDayChange(v)
                }
            },
            label = { Text(if (isEnglish) "Day" else "יום") },
            singleLine = true,
            shape = shape,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "/",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // חודש (2 ספרות)
        OutlinedTextField(
            value = monthText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(2)
                monthText = digits
                digits.toIntOrNull()?.let { v ->
                    if (v in 1..12) onMonthChange(v)
                }
            },
            label = {
                Text(
                    text = if (isEnglish) "Month" else "חודש",
                    maxLines = 1,
                    softWrap = false
                )
            },
            singleLine = true,
            shape = shape,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "/",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // שנה (4 ספרות)
        OutlinedTextField(
            value = yearText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(4)
                yearText = digits
                digits.toIntOrNull()?.let { v ->
                    if (v in 1950..2026) onYearChange(v)
                }
            },
            label = { Text(if (isEnglish) "Year" else "שנה") },
            singleLine = true,
            shape = shape,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1.3f)
        )
    }
}

/**
 * עטיפה ל"גלגל" אחד – עושה:
 * 1. רקע לבן ומעוגל
 * 2. שתי רצועות לבנות דקות למעלה/למטה שמוחקות את הקווים האופקיים של ה-NumberPicker
 * 3. פס בהיר באמצע שמדגיש את הבחירה
 */
@Composable
private fun WheelBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // ✅ רקע "כרטיס" עדין שמתאים גם לרקע כהה/גרדיאנט
    val cardBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val coverBg = cardBg // אותו צבע כדי שלא ייראו "חורים" למעלה/למטה

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(cardBg, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 2.dp, vertical = 6.dp)
    ) {
        // 🔹 פס הדגשה באמצע (מאחורי המספר)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp)
                )
        )

        // 🔹 ה-NumberPicker עצמו – המספרים יופיעו מעל הפס
        content()

        // רצועה עליונה שמעלימה קווי picker
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    color = coverBg,
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                )
        )

        // רצועה תחתונה שמעלימה קווי picker
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    color = coverBg,
                    shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                )
        )
    }
}
@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Color(0xFFE0E0E0))
    )
}

@SuppressLint("SoonBlockedPrivateApi")
private class NoDividerNumberPicker(context: android.content.Context) :
    android.widget.NumberPicker(context) {

    init {
        try {
            val pickerClass = android.widget.NumberPicker::class.java

            // להעלים את הקו האפור
            val dividerField = pickerClass.getDeclaredField("mSelectionDivider")
            dividerField.isAccessible = true
            dividerField.set(
                this,
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )

            val heightField = pickerClass.getDeclaredField("mSelectionDividerHeight")
            heightField.isAccessible = true
            heightField.setInt(this, 0)

            // צבע וגודל הציור של הגלגל
            val wheelPaintField = pickerClass.getDeclaredField("mSelectorWheelPaint")
            wheelPaintField.isAccessible = true
            val wheelPaint = wheelPaintField.get(this) as android.graphics.Paint
            wheelPaint.color = android.graphics.Color.BLACK
            wheelPaint.textSize = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                18f,
                resources.displayMetrics
            )
            wheelPaint.isFakeBoldText = true
        } catch (_: Exception) {
        }

        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        styleChildren()
    }

    // נוודא שכל פעם שנוסף/מוחלף View פנימי – הוא מעוצב כמו שצריך
    override fun addView(child: android.view.View?) {
        super.addView(child)
        updateView(child)
    }

    override fun addView(child: android.view.View?, index: Int, params: android.view.ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        updateView(child)
    }

    private fun updateView(view: android.view.View?) {
        if (view is android.widget.EditText) {
            view.setTextColor(android.graphics.Color.BLACK)
            view.setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                18f
            )
            view.typeface = android.graphics.Typeface.DEFAULT_BOLD
            view.isCursorVisible = false
            view.isFocusable = false
            view.isFocusableInTouchMode = false
        }
    }

    override fun setValue(value: Int) {
        super.setValue(value)
        styleChildren()
    }

    private fun styleChildren() {
        post {
            for (i in 0 until childCount) {
                updateView(getChildAt(i))
            }
            invalidate()
        }
    }
}

@Composable
private fun RegionAndMultiBranchPicker(
    branchType: String,
    selectedRegion: String,
    selectedBranches: List<String>,
    onRegionChange: (String) -> Unit,
    onBranchesConfirm: (List<String>) -> Unit,
    onGroupsChange: (List<String>) -> Unit,
    regionError: Boolean,
    branchError: Boolean,
    isEnglish: Boolean = false,
    fieldHeight: Dp = 52.dp
) {
    val ctx = LocalContext.current
    val fieldColors = registrationLightFieldColors()
    val fieldShape = RoundedCornerShape(14.dp)

    fun trLocal(he: String, en: String): String = if (isEnglish) en else he
    val align = if (isEnglish) TextAlign.Left else TextAlign.Right

    val regions = remember(ctx, branchType, isEnglish) {
        val dbRegions = KmiDatabaseProvider
            .regions(ctx)
            .filter { region ->
                if (branchType == "abroad") {
                    region.country != "IL"
                } else {
                    region.country == "IL"
                }
            }
            .map { region ->
                if (isEnglish) {
                    region.nameEn.ifBlank { region.nameHe }
                } else {
                    region.nameHe.ifBlank { region.nameEn }
                }
            }
            .filter { it.isNotBlank() }
            .distinct()

        if (dbRegions.isNotEmpty()) {
            dbRegions
        } else {
            if (branchType == "abroad") {
                TrainingCatalog.abroadRegions()
            } else {
                TrainingCatalog.activeRegions()
            }
        }
    }

    val allBranches = remember(ctx, branchType, selectedRegion, isEnglish) {
        if (selectedRegion.isBlank()) {
            emptyList()
        } else {
            val dbBranches = KmiDatabaseProvider
                .branches(ctx)
                .filter { branch ->
                    branch.regionHe == selectedRegion ||
                            branch.regionEn.equals(selectedRegion, ignoreCase = true) ||
                            branch.regionId.equals(selectedRegion, ignoreCase = true)
                }
                .map { dbBranch ->
                    if (isEnglish) {
                        dbBranch.nameEn.ifBlank { dbBranch.nameHe }
                    } else {
                        dbBranch.nameHe.ifBlank { dbBranch.nameEn }
                    }
                }
                .filter { it.isNotBlank() }
                .distinct()

            if (dbBranches.isNotEmpty()) {
                dbBranches
            } else {
                TrainingCatalog.branchesFor(selectedRegion)
            }
        }
    }

    var regionExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = regionExpanded,
        onExpandedChange = { regionExpanded = !regionExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedRegion,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = regionError,
            label = {
                Text(
                    text = if (branchType == "abroad") {
                        trLocal("מדינה", "Country")
                    } else {
                        trLocal("מחוז / אזור", "District / Region")
                    },
                    color = Color(0xFF374151)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .heightIn(min = fieldHeight)
                .background(Color.White, shape = fieldShape),
            colors = fieldColors,
            shape = fieldShape,
            textStyle = LocalTextStyle.current.copy(
                color = Color.Black,
                textAlign = align
            ),
            placeholder = {
                Text(
                    text = if (branchType == "abroad") {
                        trLocal("בחר/י מדינה", "Select country")
                    } else {
                        trLocal("בחר/י אזור", "Select region")
                    },
                    color = Color(0xFF64748B)
                )
            }
        )

        ExposedDropdownMenu(
            expanded = regionExpanded,
            onDismissRequest = { regionExpanded = false },
            containerColor = Color.White
        ) {
            regions.forEach { region ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = region,
                            color = Color.Black,
                            textAlign = align,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        regionExpanded = false
                        onRegionChange(region)
                    }
                )
            }
        }
    }

    if (regionError) {
        Text(
            text = trLocal("חובה לבחור אזור", "Region is required"),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(8.dp))

    var branchesExpanded by remember { mutableStateOf(false) }
    var tempSelection by remember { mutableStateOf(selectedBranches.toList()) }

    ExposedDropdownMenuBox(
        expanded = branchesExpanded,
        onExpandedChange = { open ->
            branchesExpanded = open
            if (open) tempSelection = selectedBranches.toList()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        val display =
            if (selectedBranches.isEmpty()) "" else selectedBranches.joinToString("\n")

        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            isError = branchError,
            label = {
                Text(
                    text = trLocal("סניפים (עד 3)", "Branches (up to 3)"),
                    color = Color(0xFF374151)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchesExpanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .heightIn(min = fieldHeight)
                .background(Color.White, shape = fieldShape),
            colors = fieldColors,
            shape = fieldShape,
            textStyle = LocalTextStyle.current.copy(
                color = Color.Black,
                textAlign = align
            ),
            placeholder = {
                Text(
                    text = if (branchType == "abroad") {
                        trLocal("בחר/י 1–3 סניפים בחו״ל", "Select 1–3 abroad branches")
                    } else {
                        trLocal("בחר/י 1–3 סניפים", "Select 1–3 branches")
                    },
                    color = Color(0xFF64748B)
                )
            }
        )

        ExposedDropdownMenu(
            expanded = branchesExpanded,
            onDismissRequest = { branchesExpanded = false },
            containerColor = Color.White
        ) {
            allBranches.forEach { branch ->
                val checked = branch in tempSelection

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = branch,
                                color = Color.Black,
                                textAlign = align,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    },
                    onClick = {
                        tempSelection =
                            if (checked) {
                                tempSelection.filterNot { it == branch }
                            } else {
                                if (tempSelection.size < 3) {
                                    tempSelection + branch
                                } else {
                                    Toast.makeText(
                                        ctx,
                                        trLocal("ניתן לבחור עד 3 סניפים", "You can select up to 3 branches"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    tempSelection
                                }
                            }
                    }
                )
            }

            Divider(color = Color(0xFFE5E7EB))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { tempSelection = emptyList() }
                ) {
                    Text(trLocal("נקה", "Clear"), color = Color(0xFF374151))
                }

                Button(
                    onClick = {
                        onBranchesConfirm(tempSelection)

                        if (branchType == "abroad") {
                            onGroupsChange(
                                if (tempSelection.isNotEmpty()) listOf("חו״ל") else emptyList()
                            )
                        }

                        branchesExpanded = false
                    }
                ) {
                    Text(trLocal("אישור", "Confirm"))
                }
            }
        }
    }

    if (branchError) {
        Text(
            text = trLocal("חובה לבחור לפחות סניף אחד", "Please select at least one branch"),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MultiGroupsPicker(
    allGroupsAcrossBranches: List<String>,
    selectedGroups: List<String>,
    onGroupsChange: (List<String>) -> Unit,
    groupError: Boolean,
    isEnglish: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var tempSelection by remember(selectedGroups) {
        mutableStateOf(selectedGroups.toList())
    }
    val ctx = LocalContext.current

    fun trLocal(he: String, en: String): String = if (isEnglish) en else he
    val align = if (isEnglish) TextAlign.Left else TextAlign.Right

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { open ->
            expanded = open
            if (open) tempSelection = selectedGroups.toList()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        val display =
            if (selectedGroups.isEmpty()) "" else selectedGroups.joinToString("\n")

        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    text = trLocal("בחר/י קבוצה/ות (עד 3)", "Select group(s) - up to 3"),
                    color = Color.Black
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            isError = groupError,
            minLines = (if (selectedGroups.isEmpty()) 1 else selectedGroups.size).coerceAtMost(4),
            maxLines = 6,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .background(Color.White, shape = MaterialTheme.shapes.medium),
            textStyle = LocalTextStyle.current.copy(
                color = Color.Black,
                textAlign = align
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = MaterialTheme.colorScheme.error
            ),
            placeholder = {
                Text(
                    text = trLocal(
                        "בחר/י 1–3 קבוצות מכל הסניפים",
                        "Select 1–3 groups from all branches"
                    )
                )
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White
        ) {
            allGroupsAcrossBranches.forEach { g ->
                val checked = g in tempSelection

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = g,
                                color = Color.Black,
                                textAlign = align,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    },
                    onClick = {
                        val newSelection =
                            if (checked) {
                                tempSelection.filterNot { it == g }
                            } else {
                                if (tempSelection.size < 3) {
                                    tempSelection + g
                                } else {
                                    Toast.makeText(
                                        ctx,
                                        trLocal("ניתן לבחור עד 3 קבוצות", "You can select up to 3 groups"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    tempSelection
                                }
                            }

                        tempSelection = newSelection

                        // ⭐ התיקון הקריטי — שמירה מיידית
                        onGroupsChange(newSelection.take(3))
                    }
                )
            }

            Divider(color = Color(0xFFE5E7EB))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        tempSelection = emptyList()
                        onGroupsChange(emptyList())
                    }
                ) {
                    Text(trLocal("נקה", "Clear"))
                }

                Button(
                    onClick = {
                        onGroupsChange(tempSelection.take(3))
                        expanded = false
                    }
                ) {
                    Text(trLocal("אישור", "Confirm"))
                }
            }
        }
    }

    if (groupError) {
        Text(
            text = trLocal(
                "חובה לבחור לפחות קבוצה אחת (עד 3)",
                "Please select at least one group (up to 3)"
            ),
            color = MaterialTheme.colorScheme.error,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BeltPicker(
    currentBeltId: String,
    onBeltChange: (String) -> Unit,
    isEnglish: Boolean = false
) {
    val beltOptions = remember { traineeRankOptions() }
    var expanded by remember { mutableStateOf(false) }

    val normalizedCurrentBeltId = when (currentBeltId.trim()) {
        // תאימות לאחור אם נשמר בעבר "black"
        "black" -> "black"
        "שחורה" -> "black"
        "שחורה דאן 1" -> "black"
        else -> currentBeltId.trim()
    }

    val currentBelt = beltOptions.firstOrNull { it.id == normalizedCurrentBeltId }

    fun beltLabel(option: TraineeRankOption): String {
        if (!isEnglish) return option.heb

        return when (option.id) {
            "white" -> "White"
            "yellow" -> "Yellow"
            "orange" -> "Orange"
            "green" -> "Green"
            "blue" -> "Blue"
            "brown" -> "Brown"
            "black" -> "Black Dan 1"
            "black_dan_2" -> "Black Dan 2"
            "black_dan_3" -> "Black Dan 3"
            "black_dan_4" -> "Black Dan 4"
            "black_dan_5" -> "Black Dan 5"
            "black_dan_6" -> "Black Dan 6"
            "black_dan_7" -> "Black Dan 7"
            "black_dan_8" -> "Black Dan 8"
            "black_dan_9" -> "Black Dan 9"
            "black_dan_10" -> "Black Dan 10"
            else -> option.heb
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentBelt?.let { beltLabel(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(if (isEnglish) "Current K.A.M.I belt rank" else "דרגת חגורה נוכחית (ק.מ.י)", color = Color.Black) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .defaultMinSize(minHeight = 46.dp)
                .background(Color.White, shape = MaterialTheme.shapes.medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = MaterialTheme.colorScheme.error
            ),
            textStyle = LocalTextStyle.current.copy(
                color = Color.Black,
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                textDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
            ),
            placeholder = {
                Text(
                    text = if (isEnglish) "Select rank" else "בחר/י דרגה",
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White
        ) {
            beltOptions.forEach { belt ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = belt.color,
                                tonalElevation = 0.dp,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(14.dp)
                            ) {}

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = beltLabel(belt),
                                color = Color.Black,
                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    },
                    onClick = {
                        onBeltChange(belt.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

// 🔹 פונקציית עזר – שמירת המשתמש ל-Firestore בקולקציה "users"
private fun saveUserToFirestore(
    fullName: String,
    phone: String,
    email: String,
    beltId: String,
    selectedRegion: String,
    selectedBranches: List<String>,
    selectedGroups: List<String>,
    birthDay: Int,
    birthMonth: Int,
    birthYear: Int
) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return

    val db = FirebaseFirestore.getInstance()

    // נבנה מחרוזת תאריך לידה בפורמט YYYY-MM-DD
    val birthDate = if (birthDay > 0 && birthMonth > 0 && birthYear > 0) {
        "%04d-%02d-%02d".format(birthYear, birthMonth, birthDay)
    } else {
        ""
    }

    val data = hashMapOf(
        "uid" to uid,
        "fullName" to fullName,
        "phone" to phone,
        "email" to email,
        "belt" to beltId,
        "region" to selectedRegion,
        "branches" to selectedBranches,
        "groups" to selectedGroups,
        "birthDate" to birthDate,
        "isActive" to true,
        "createdAt" to System.currentTimeMillis(),
        "lastLoginAt" to System.currentTimeMillis()
    )

    db.collection("users")
        .document(uid)
        .set(data, SetOptions.merge())
}

