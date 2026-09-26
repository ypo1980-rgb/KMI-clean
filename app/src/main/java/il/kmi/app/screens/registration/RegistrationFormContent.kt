@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package il.kmi.app.screens.registration

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import il.kmi.shared.domain.Belt
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.database.KmiDatabaseProvider
import il.kmi.app.ui.ext.color
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import java.util.Calendar
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

private val LocalRegistrationFieldPosition =
    compositionLocalOf<(String, LayoutCoordinates) -> Unit> {
        { _, _ -> }
    }

@Composable
private fun Modifier.registrationFieldPosition(field: String): Modifier {
    val reportPosition = LocalRegistrationFieldPosition.current
    return onGloballyPositioned { coordinates ->
        reportPosition(field, coordinates)
    }
}

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

private fun registrationGroupLabelForUi(
    group: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return group

    return TrainingCatalog.groupDisplayName(
        group = group,
        isEnglish = true
    )
}

@Composable
fun RegistrationFormContent(
    isCoach: Boolean,
    isEnglish: Boolean,
    isGoogleAuth: Boolean = false,
    startAtProfile: Boolean = false,
    scrollToMissingField: String? = null,
    onMissingFieldScrollHandled: () -> Unit = {},
    fullName: String,
    onFullNameChange: (String) -> Unit,
    fullNameError: Boolean,
    phone: String,
    onPhoneChange: (String) -> Unit,
    phoneError: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: Boolean,
    gender: String,
    onGenderChange: (String) -> Unit,
    genderError: Boolean,
    birthDateError: Boolean = false,
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
    availableBranches: List<String>,
    selectedRegions: List<String>,
    onRegionsChange: (List<String>) -> Unit,
    selectedBranches: List<String>,
    onBranchesChange: (List<String>) -> Unit,

    /*
     * הרשימה השטוחה נשארת זמנית לתאימות.
     */
    selectedGroups: List<String>,
    onGroupsChange: (List<String>) -> Unit,

    /*
     * מקור האמת החדש: קבוצות לפי סניף.
     */
    selectedGroupsByBranch:
    Map<String, List<String>>,
    onGroupsByBranchChange:
        (Map<String, List<String>>) -> Unit,

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
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val fieldPositions = remember { mutableStateMapOf<String, Int>() }
    var formTopInRoot by remember { mutableIntStateOf(0) }
    var extraBottomSpacePx by remember { mutableIntStateOf(0) }
    var beltMissingRequested by remember { mutableStateOf(false) }
    var initialPhoneScrollHandled by remember { mutableStateOf(false) }
    var previousGroupsByBranch by remember {
        mutableStateOf(selectedGroupsByBranch)
    }

    val reportFieldPosition: (String, LayoutCoordinates) -> Unit =
        { field, coordinates ->
            val positionInContent =
                scroll.value +
                        coordinates.positionInRoot().y.roundToInt() -
                        formTopInRoot

            if (fieldPositions[field] != positionInContent) {
                fieldPositions[field] = positionInContent
            }
        }

    LaunchedEffect(scrollToMissingField, phoneError, isGoogleAuth) {
        val initialPhoneTarget =
            !initialPhoneScrollHandled &&
                    phone.isBlank() &&
                    (phoneError || isGoogleAuth)

        val target = scrollToMissingField
            ?: if (initialPhoneTarget) "phone" else return@LaunchedEffect

        if (initialPhoneTarget) initialPhoneScrollHandled = true
        if (target == "belt") beltMissingRequested = true

        focusManager.clearFocus(force = true)
        keyboardController?.hide()

        // מודדים אחרי שהשגיאות והמקלדת עדכנו את פריסת המסך.
        withFrameNanos { }
        withFrameNanos { }

        val topOffset = with(density) { 12.dp.roundToPx() }

        val fieldY = snapshotFlow { fieldPositions[target] }
            .filterNotNull()
            .first()

        val neededSpace = (fieldY - topOffset - scroll.maxValue).coerceAtLeast(0)
        if (neededSpace > 0) {
            extraBottomSpacePx += neededSpace
            withFrameNanos { }
            withFrameNanos { }
        }

        scroll.animateScrollTo(
            (fieldY - topOffset).coerceIn(0, scroll.maxValue)
        )

        // פריסת הכרטיס עשויה להשתנות בזמן הגלילה.
        withFrameNanos { }
        val updatedFieldY = fieldPositions[target] ?: fieldY
        scroll.scrollTo(
            (updatedFieldY - topOffset).coerceIn(0, scroll.maxValue)
        )

        if (target == "phone") {
            withFrameNanos { }
            withFrameNanos { }
            val phoneY = fieldPositions["phone"] ?: updatedFieldY
            scroll.scrollTo(
                (phoneY - topOffset).coerceIn(0, scroll.maxValue)
            )
        }

        if (scrollToMissingField != null) {
            onMissingFieldScrollHandled()
        }
    }

    LaunchedEffect(selectedGroupsByBranch, selectedBranches) {
        val previous = previousGroupsByBranch
        previousGroupsByBranch = selectedGroupsByBranch

        val groupJustSelected = selectedBranches.any { branch ->
            previous[branch].isNullOrEmpty() &&
                    !selectedGroupsByBranch[branch].isNullOrEmpty()
        }
        if (!groupJustSelected) return@LaunchedEffect

        val nextBranch = selectedBranches.firstOrNull { branch ->
            selectedGroupsByBranch[branch].isNullOrEmpty()
        } ?: return@LaunchedEffect

        val target = "group:$nextBranch"
        val fieldY = snapshotFlow { fieldPositions[target] }
            .filterNotNull()
            .first()
        val topOffset = with(density) { 12.dp.roundToPx() }

        val neededSpace = (fieldY - topOffset - scroll.maxValue).coerceAtLeast(0)
        if (neededSpace > 0) {
            extraBottomSpacePx += neededSpace
            withFrameNanos { }
            withFrameNanos { }
        }

        scroll.animateScrollTo(
            (fieldY - topOffset).coerceIn(0, scroll.maxValue)
        )
    }

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val fieldTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val fieldTextDirection = if (isEnglish) TextDirection.Ltr else TextDirection.Rtl
    val screenLayoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    val missingFieldBackground = MaterialTheme.colorScheme.errorContainer
    val normalFieldBackground = MaterialTheme.colorScheme.surface

    // ✅ בכניסה עם Google מציגים מיד שדות חובה חסרים,
    // גם לפני שהמשתמש לחץ על סיום רישום.
    val highlightMissingRequired = isGoogleAuth

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
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }
    }

    /*
     * לכל סניף שנבחר חייבת להיות לפחות
     * קבוצה אחת שנבחרה עבורו.
     */
    val shouldShowGroupsPicker =
        selectedBranches.isNotEmpty()

    val hasBranchWithoutGroups =
        selectedBranches.any { branch ->
            selectedGroupsByBranch[
                branch
            ].isNullOrEmpty()
        }

    val showFullNameMissing =
        (fullNameError || highlightMissingRequired) &&
                fullName.isBlank()
    val showPhoneMissing = (phoneError || highlightMissingRequired) && phone.isBlank()
    val showEmailMissing = (emailError || highlightMissingRequired) && email.isBlank()
    val showGenderMissing = (genderError || highlightMissingRequired) && gender.isBlank()
    val showRegionMissing =
        (regionError || highlightMissingRequired) && selectedRegions.isEmpty()
    val showBranchMissing = (branchError || highlightMissingRequired) && selectedBranches.isEmpty()
    val showGroupMissing =
        (groupError || highlightMissingRequired) &&
                shouldShowGroupsPicker &&
                hasBranchWithoutGroups

    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodySmall,
        LocalLayoutDirection provides screenLayoutDirection,
        LocalRegistrationFieldPosition provides reportFieldPosition
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    formTopInRoot = coordinates.positionInRoot().y.roundToInt()
                }
                .background(kmiScreenBackgroundBrush())
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
                    label = { Text(tr("שם מלא", "Full name"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    isError = fullNameError,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = fieldTextAlign,
                        textDirection = fieldTextDirection
                    ),
                    modifier = Modifier
                        .registrationFieldPosition("fullName")
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .background(
                            if (showFullNameMissing) missingFieldBackground else normalFieldBackground,
                            shape = MaterialTheme.shapes.medium
                        ),
                    colors = registrationRequiredFieldColors(
                        showMissing = showFullNameMissing
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onSubmitRegistration() }
                ),
                isError = phoneError,
                modifier = Modifier
                    .registrationFieldPosition("phone")
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 46.dp)
                    .background(
                        if (showPhoneMissing) missingFieldBackground else normalFieldBackground,
                        shape = MaterialTheme.shapes.medium
                    ),
                textStyle = LocalTextStyle.current.copy(
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Left
                ),
                colors = registrationRequiredFieldColors(
                    showMissing = showPhoneMissing
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
                    .registrationFieldPosition("email")
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 46.dp)
                    .background(
                        if (showEmailMissing) missingFieldBackground else normalFieldBackground,
                        shape = MaterialTheme.shapes.medium
                    ),
                colors = registrationRequiredFieldColors(
                    showMissing = showEmailMissing
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = fieldTextAlign,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .registrationFieldPosition("gender")
                        .fillMaxWidth()
                        .background(
                            color = if (showGenderMissing) missingFieldBackground else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(if (showGenderMissing) 6.dp else 0.dp),
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
                            color = if (gender == "male") {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = gender == "male",
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = gender == "female",
                    onClick = { onGenderChange("female") },
                    label = {
                        Text(
                            tr("נקבה", "Female"),
                            textAlign = TextAlign.Center,
                            color = if (gender == "female") {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = gender == "female",
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
                    showMissing = birthDateError,
                    onYearChange = { onBirthYearChange(it) },
                    onMonthChange = { onBirthMonthChange(it) },
                    onDayChange = { onBirthDayChange(it) },
                    onYearDone = onSubmitRegistration
                )
            }

            // ===== Account =====
// שם משתמש / סיסמה מוצגים רק ברישום חדש רגיל.
// בעריכת פרופיל שינוי סיסמה צריך להיעשות במסך ייעודי עם re-auth.
            if (!isGoogleAuth && !startAtProfile) {
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
                        .registrationFieldPosition("username")
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .background(
                            if (usernameError && username.isBlank()) missingFieldBackground else normalFieldBackground,
                            shape = MaterialTheme.shapes.medium
                        ),
                    colors = registrationRequiredFieldColors(
                        showMissing = usernameError && username.isBlank()
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
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    isError = passwordError,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = fieldTextAlign,
                        textDirection = fieldTextDirection
                    ),
                    modifier = Modifier
                        .registrationFieldPosition("password")
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 46.dp)
                        .background(
                            if (passwordError && password.isBlank()) missingFieldBackground else normalFieldBackground,
                            shape = MaterialTheme.shapes.medium
                        ),
                    colors = registrationRequiredFieldColors(
                        showMissing = passwordError && password.isBlank()
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
                        onRegionsChange(emptyList())
                        onBranchesChange(emptyList())
                        onGroupsChange(emptyList())
                    },
                    label = {
                        Text(
                            tr("ישראל", "Israel"),
                            color = if (branchType == "israel") {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = branchType == "israel",
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                FilterChip(
                    selected = branchType == "abroad",
                    onClick = {
                        onBranchTypeChange("abroad")
                        onRegionsChange(emptyList())
                        onBranchesChange(emptyList())
                        onGroupsChange(emptyList())
                    },
                    label = {
                        Text(
                            tr("חו״ל", "Abroad"),
                            color = if (branchType == "abroad") {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = branchType == "abroad",
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 2.dp
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(Modifier.height(6.dp))

                RegionAndMultiBranchPicker(
                    branchType = branchType,
                    availableBranches = availableBranches,
                    selectedRegions = selectedRegions,
                    selectedBranches = selectedBranches,
                    onRegionsChange = onRegionsChange,
                    onBranchesConfirm = onBranchesChange,
                    onGroupsChange = onGroupsChange,
                    regionError = regionError,
                    branchError = branchError,
                    highlightMissingRequired = highlightMissingRequired,
                    isEnglish = isEnglish
                )

                if (shouldShowGroupsPicker) {
                    BranchGroupsAssignmentsPicker(
                        selectedBranches =
                            selectedBranches,
                        selectedGroupsByBranch =
                            selectedGroupsByBranch,
                        onGroupsByBranchChange =
                            onGroupsByBranchChange,
                        showGroupMissing =
                            showGroupMissing,
                        isEnglish = isEnglish
                    )
                }

                BeltPicker(
                    currentBeltId = currentBeltId,
                    onBeltChange = onBeltChange,
                    showMissing = beltMissingRequested || highlightMissingRequired || phoneError,
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
                        .registrationFieldPosition("terms")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = submitButtonText ?: tr("סיום רישום", "Complete registration"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(
                    20.dp + with(density) { extraBottomSpacePx.toDp() }
                )
            )
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
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            content()
        }
    }
}

@Composable
private fun registrationRequiredFieldColors(
    showMissing: Boolean
): TextFieldColors {
    val colors = MaterialTheme.colorScheme
    val container = if (showMissing) colors.errorContainer else colors.surface
    val content = if (showMissing) colors.onErrorContainer else colors.onSurface
    val secondaryContent =
        if (showMissing) colors.onErrorContainer else colors.onSurfaceVariant
    val border = if (showMissing) colors.error else colors.outlineVariant

    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        errorContainerColor = colors.errorContainer,

        focusedTextColor = content,
        unfocusedTextColor = content,
        disabledTextColor = content.copy(alpha = 0.78f),
        errorTextColor = colors.onErrorContainer,

        focusedLabelColor = if (showMissing) colors.onErrorContainer else colors.primary,
        unfocusedLabelColor = secondaryContent,
        disabledLabelColor = secondaryContent,
        errorLabelColor = colors.onErrorContainer,

        focusedPlaceholderColor = secondaryContent,
        unfocusedPlaceholderColor = secondaryContent,
        disabledPlaceholderColor = secondaryContent,

        focusedBorderColor = if (showMissing) colors.error else colors.primary,
        unfocusedBorderColor = border,
        disabledBorderColor = border,
        errorBorderColor = colors.error,

        cursorColor = colors.primary
    )
}

@Composable
private fun registrationLightFieldColors() =
    registrationRequiredFieldColors(showMissing = false)

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
    showMissing: Boolean = false,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    onYearDone: () -> Unit,
) {
    val currentYear = remember {
        Calendar.getInstance().get(Calendar.YEAR)
    }

    // ✅ 01/01/2000 הוא תאריך ברירת מחדל פנימי בלבד,
    // ולכן לא מציגים אותו למשתמש כתאריך שכבר מולא.
    val startsFromDefaultBirthDate = remember {
        day == 1 && month == 1 && year == 2000
    }

    fun displayDayValue(): String =
        day.coerceIn(1, 31).toString().padStart(2, '0')

    fun displayMonthValue(): String =
        month.coerceIn(1, 12).toString().padStart(2, '0')

    fun displayYearValue(): String =
        year.coerceIn(1950, currentYear).toString().padStart(4, '0')

    // ✅ כל שדה נשמר בנפרד.
    // מילוי יום לא גורם לחודש/שנה לקבל שוב 01/2000.
    var dayText by remember {
        mutableStateOf(
            if (startsFromDefaultBirthDate) "" else displayDayValue()
        )
    }

    var monthText by remember {
        mutableStateOf(
            if (startsFromDefaultBirthDate) "" else displayMonthValue()
        )
    }

    var yearText by remember {
        mutableStateOf(
            if (startsFromDefaultBirthDate) "" else displayYearValue()
        )
    }

    // ✅ אם בעתיד ייטען תאריך אמיתי מבחוץ, למשל מפרופיל קיים,
    // נמלא אותו רק אם המשתמש עדיין לא התחיל למלא שום שדה.
    LaunchedEffect(day, month, year) {
        val incomingIsDefault = day == 1 && month == 1 && year == 2000
        val userHasNotStartedTyping =
            dayText.isBlank() && monthText.isBlank() && yearText.isBlank()

        if (!incomingIsDefault && userHasNotStartedTyping) {
            dayText = displayDayValue()
            monthText = displayMonthValue()
            yearText = displayYearValue()
        }
    }

    // צבעים קבועים כדי שהשדות יהיו קריאים גם במצב כהה
    val shape = RoundedCornerShape(14.dp)
    val fieldColors = registrationRequiredFieldColors(showMissing)

    val dayFocusRequester = remember { FocusRequester() }
    val monthFocusRequester = remember { FocusRequester() }
    val yearFocusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .registrationFieldPosition("birthDate")
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // שנה (4 ספרות)
        // בעברית השדה הזה מוצג בצד שמאל בגלל RTL,
        // ולכן סדר התצוגה בפועל הוא: יום / חודש / שנה.
        OutlinedTextField(
            value = yearText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(4)
                yearText = digits

                val yearValue = digits.toIntOrNull()
                if (yearValue != null && yearValue in 1950..currentYear) {
                    onYearChange(yearValue)
                }
            },
            label = {
                Text(
                    text = if (isEnglish) "Year" else "שנה",
                    maxLines = 1,
                    softWrap = false
                )
            },
            singleLine = true,
            shape = shape,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onYearDone() }
            ),
            modifier = Modifier
                .weight(1.05f)
                .focusRequester(yearFocusRequester)
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

                val monthValue = digits.toIntOrNull()
                if (monthValue != null && monthValue in 1..12) {
                    onMonthChange(monthValue)

                    if (digits.length == 2) {
                        yearFocusRequester.requestFocus()
                    }
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
            modifier = Modifier
                .weight(1.15f)
                .focusRequester(monthFocusRequester)
        )

        Text(
            text = "/",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // יום (2 ספרות)
        // בעברית זה השדה הימני, ולכן לאחר יום תקין עוברים לחודש.
        OutlinedTextField(
            value = dayText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(2)
                dayText = digits

                val dayValue = digits.toIntOrNull()
                if (dayValue != null && dayValue in 1..31) {
                    onDayChange(dayValue)

                    if (digits.length == 2) {
                        monthFocusRequester.requestFocus()
                    }
                }
            },
            label = {
                Text(
                    text = if (isEnglish) "Day" else "יום",
                    maxLines = 1,
                    softWrap = false
                )
            },
            singleLine = true,
            shape = shape,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(0.90f)
                .focusRequester(dayFocusRequester)
        )
    }
}

@Composable
private fun RegionAndMultiBranchPicker(
    branchType: String,
    availableBranches: List<String>,
    selectedRegions: List<String>,
    selectedBranches: List<String>,
    onRegionsChange: (List<String>) -> Unit,
    onBranchesConfirm: (List<String>) -> Unit,
    onGroupsChange: (List<String>) -> Unit,
    regionError: Boolean,
    branchError: Boolean,
    highlightMissingRequired: Boolean = false,
    isEnglish: Boolean = false,
    fieldHeight: Dp = 52.dp
) {
    val ctx = LocalContext.current
    val fieldShape = RoundedCornerShape(14.dp)

    val showRegionMissing =
        (regionError || highlightMissingRequired) && selectedRegions.isEmpty()
    val showBranchMissing = (branchError || highlightMissingRequired) && selectedBranches.isEmpty()

    val regionFieldColors = registrationRequiredFieldColors(
        showMissing = showRegionMissing
    )
    val branchFieldColors = registrationRequiredFieldColors(
        showMissing = showBranchMissing
    )

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

    /*
     * מקור האמת לסניפים מגיע מ־RegistrationFormScreen,
     * שם הרשימה כבר מחושבת מכל האזורים שנבחרו
     * ומשלבת את branches.json עם TrainingCatalog.
     *
     * כך בחירת "השרון" תציג את כל סניפי השרון,
     * וגם בחירה של כמה אזורים תציג את האיחוד שלהם.
     */
    val allBranches = remember(
        availableBranches,
        selectedRegions,
        branchType
    ) {
        if (selectedRegions.isEmpty()) {
            emptyList()
        } else {
            availableBranches
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }
    }

    var regionExpanded by remember {
        mutableStateOf(false)
    }

    var tempRegionSelection by remember(selectedRegions) {
        mutableStateOf(selectedRegions.toList())
    }

    ExposedDropdownMenuBox(
        expanded = regionExpanded,
        onExpandedChange = { open ->
            regionExpanded = open

            if (open) {
                tempRegionSelection =
                    selectedRegions.toList()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        val regionDisplay =
            if (selectedRegions.isEmpty()) {
                ""
            } else {
                selectedRegions.joinToString("\n")
            }

        OutlinedTextField(
            value = regionDisplay,
            onValueChange = {},
            readOnly = true,
            isError = regionError,
            minLines =
                if (selectedRegions.isEmpty()) {
                    1
                } else {
                    selectedRegions.size.coerceAtMost(4)
                },
            maxLines = 6,
            label = {
                Text(
                    text =
                        if (branchType == "abroad") {
                            trLocal(
                                "מדינות",
                                "Countries"
                            )
                        } else {
                            trLocal(
                                "בחר/י אזורים",
                                "Select regions"
                            )
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = regionExpanded
                )
            },
            modifier = Modifier
                .menuAnchor()
                .registrationFieldPosition("region")
                .fillMaxWidth()
                .heightIn(min = fieldHeight)
                .background(
                    if (showRegionMissing) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = fieldShape
                ),
            colors = regionFieldColors,
            shape = fieldShape,
            textStyle = LocalTextStyle.current.copy(
                color = if (showRegionMissing) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = align
            ),
            placeholder = {
                Text(
                    text =
                        if (branchType == "abroad") {
                            trLocal(
                                "בחר/י מדינות",
                                "Select countries"
                            )
                        } else {
                            trLocal(
                                "בחר/י אזורים",
                                "Select regions"
                            )
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        if (regionExpanded) {
            Dialog(
                onDismissRequest = {
                    regionExpanded = false
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .heightIn(max = 520.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.45f
                        )
                    ),
                    shadowElevation = 2.dp
                ) {
                    Column {
                        Text(
                            text =
                                if (branchType == "abroad") {
                                    trLocal("בחר מדינה", "Select a country")
                                } else {
                                    trLocal("בחר אזור", "Select a region")
                                },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = align,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(16.dp)
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            items(regions.size) { index ->
                                val region = regions[index]
                                val checked =
                                    region in tempRegionSelection

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            tempRegionSelection =
                                                if (checked) {
                                                    tempRegionSelection.filterNot {
                                                        it == region
                                                    }
                                                } else {
                                                    tempRegionSelection + region
                                                }
                                        }
                                        .padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = null
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = region,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = align,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    tempRegionSelection = emptyList()
                                }
                            ) {
                                Text(trLocal("נקה", "Clear"))
                            }

                            Button(
                                onClick = {
                                    regionExpanded = false
                                    onRegionsChange(tempRegionSelection)

                                    // שינוי אזורים מאפס בחירות התלויות בהם.
                                    onBranchesConfirm(emptyList())
                                    onGroupsChange(emptyList())
                                }
                            ) {
                                Text(trLocal("אישור", "Confirm"))
                            }
                        }
                    }
                }
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
                    text = trLocal("סניפים", "Branches"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchesExpanded)
            },
            modifier = Modifier
                .menuAnchor()
                .registrationFieldPosition("branch")
                .fillMaxWidth()
                .heightIn(min = fieldHeight)
                .background(
                    if (showBranchMissing) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = fieldShape
                ),
            colors = branchFieldColors,
            shape = fieldShape,
            textStyle = LocalTextStyle.current.copy(
                color = if (showBranchMissing) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = align
            ),
            placeholder = {
                Text(
                    text = if (branchType == "abroad") {
                        trLocal("בחר/י סניפים בחו״ל", "Select abroad branches")
                    } else {
                        trLocal("בחר/י סניפים", "Select branches")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

    }

    if (branchesExpanded) {
        Dialog(onDismissRequest = { branchesExpanded = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .heightIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                ),
                shadowElevation = 2.dp
            ) {
                Column {
                    Text(
                        text = trLocal("בחר סניפים", "Select branches"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = align,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                            .padding(16.dp)
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(allBranches.size) { index ->
                            val branch = allBranches[index]
                            val checked = branch in tempSelection

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        tempSelection =
                                            when {
                                                checked -> tempSelection.filterNot {
                                                    it == branch
                                                }

                                                tempSelection.size < 10 ->
                                                    tempSelection + branch

                                                else -> {
                                                    Toast.makeText(
                                                        ctx,
                                                        trLocal(
                                                            "ניתן לבחור עד 10 סניפים",
                                                            "You can select up to 10 branches"
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    tempSelection
                                                }
                                            }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = branch,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = align,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { tempSelection = emptyList() }
                        ) {
                            Text(trLocal("נקה", "Clear"))
                        }

                        Button(
                            onClick = {
                                branchesExpanded = false
                                onBranchesConfirm(tempSelection)

                                if (branchType == "abroad") {
                                    onGroupsChange(emptyList())
                                }
                            }
                        ) {
                            Text(trLocal("אישור", "Confirm"))
                        }
                    }
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
private fun BranchGroupsAssignmentsPicker(
    selectedBranches: List<String>,
    selectedGroupsByBranch:
    Map<String, List<String>>,
    onGroupsByBranchChange:
        (Map<String, List<String>>) -> Unit,
    showGroupMissing: Boolean,
    isEnglish: Boolean
) {
    val ctx = LocalContext.current

    fun trLocal(
        he: String,
        en: String
    ): String {
        return if (isEnglish) en else he
    }

    fun String.normalizedBranchValue(): String =
        trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")

    Column(
        modifier = Modifier
            .registrationFieldPosition("group")
            .fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text =
                trLocal(
                    "בחירת קבוצות לפי סניף",
                    "Select groups by branch"
                ),
            color = MaterialTheme.colorScheme.onSurface,
            style =
                MaterialTheme
                    .typography
                    .titleSmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign =
                if (isEnglish) {
                    TextAlign.Left
                } else {
                    TextAlign.Right
                },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text =
                trLocal(
                    "פתח כל סניף ובחר את הקבוצות שבהן אתה מאמן",
                    "Open each branch and select the groups you coach"
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            textAlign =
                if (isEnglish) {
                    TextAlign.Left
                } else {
                    TextAlign.Right
                },
            modifier = Modifier.fillMaxWidth()
        )

        selectedBranches
            .take(10)
            .forEach { branch ->

                key(branch) {
                    /*
                     * הבחירות כפי שהגיעו מהנתונים
                     * השמורים, כולל ערכי legacy.
                     */
                    val storedSelectedForBranch =
                        selectedGroupsByBranch[
                            branch
                        ].orEmpty()

                    val availableGroups =
                        remember(ctx, branch) {
                            val cleanBranch =
                                branch.trim()

                            val normalizedBranch =
                                cleanBranch
                                    .normalizedBranchValue()

                            val isOfek =
                                normalizedBranch.contains(
                                    "מרכז קהילתי אופק",
                                    ignoreCase = true
                                )

                            fun filterAvailableGroups(
                                groups: List<String>
                            ): List<String> {
                                return groups
                                    .map { group ->
                                        group.trim()
                                    }
                                    .filter { group ->
                                        group.isNotBlank()
                                    }
                                    .filterNot { group ->
                                        isOfek &&
                                                TrainingCatalog
                                                    .normalizeGroupName(group) ==
                                                "בוגרים"
                                    }
                                    .distinct()
                            }

                            val databaseGroups =
                                KmiDatabaseProvider
                                    .branchByName(
                                        ctx,
                                        cleanBranch
                                    )
                                    ?.trainingDays
                                    ?.map { trainingDay ->
                                        trainingDay
                                            .groupHe
                                            .trim()
                                    }
                                    .orEmpty()

                            if (
                                databaseGroups
                                    .isNotEmpty()
                            ) {
                                filterAvailableGroups(
                                    databaseGroups
                                )
                            } else {
                                val catalogGroups =
                                    TrainingCatalog
                                        .ageGroupsByBranch
                                        .entries
                                        .firstOrNull { entry ->
                                            entry.key
                                                .normalizedBranchValue() ==
                                                    normalizedBranch
                                        }
                                        ?.value
                                        .orEmpty()

                                filterAvailableGroups(
                                    catalogGroups
                                )
                            }
                        }

                    fun String.normalizedGroupOption(): String =
                        trim()
                            .replace('־', '-')
                            .replace('–', '-')
                            .replace('—', '-')
                            .replace('’', '\'')
                            .replace(Regex("\\s+"), " ")

                    /*
                     * סופרים ומציגים רק בחירות שקיימות
                     * בפועל ברשימת הקבוצות של הסניף.
                     */
                    val selectedForBranch =
                        storedSelectedForBranch
                            .filter { selectedGroup ->
                                availableGroups.any {
                                        availableGroup ->

                                    availableGroup
                                        .normalizedGroupOption() ==
                                            selectedGroup
                                                .normalizedGroupOption()
                                }
                            }
                            .distinct()

                    /*
                     * מנקים אוטומטית בחירות ישנות
                     * שאינן קיימות יותר ברשימה.
                     *
                     * לדוגמה: ערך legacy כללי "ילדים"
                     * כאשר האפשרויות החדשות הן לפי כיתות.
                     */
                    LaunchedEffect(
                        branch,
                        availableGroups,
                        storedSelectedForBranch
                    ) {
                        if (
                            selectedForBranch !=
                            storedSelectedForBranch
                        ) {
                            val cleanedMap =
                                selectedGroupsByBranch
                                    .toMutableMap()

                            cleanedMap[branch] =
                                selectedForBranch

                            onGroupsByBranchChange(
                                cleanedMap
                            )
                        }
                    }

                    var expanded by
                    rememberSaveable(branch) {
                        mutableStateOf(
                            selectedForBranch
                                .isEmpty()
                        )
                    }

                    val branchMissing =
                        showGroupMissing &&
                                selectedForBranch.isEmpty()

                    val cardShape =
                        RoundedCornerShape(18.dp)

                    Surface(
                        color =
                            if (branchMissing) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        shape = cardShape,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        border =
                            BorderStroke(
                                width =
                                    if (branchMissing) {
                                        1.5.dp
                                    } else {
                                        1.dp
                                    },
                                color =
                                    if (branchMissing) {
                                        MaterialTheme.colorScheme.error
                                    } else if (expanded) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                            ),
                        modifier =
                            Modifier
                                .registrationFieldPosition("group:$branch")
                                .fillMaxWidth()
                    ) {
                        Column(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expanded =
                                                !expanded
                                        }
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 10.dp
                                        ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Surface(
                                    color =
                                        if (selectedForBranch.isNotEmpty()) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                    shape = CircleShape,
                                    shadowElevation = 0.dp,
                                    tonalElevation = 0.dp,
                                    modifier =
                                        Modifier.size(36.dp)
                                ) {
                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {
                                        Text(
                                            text =
                                                selectedForBranch
                                                    .size
                                                    .toString(),
                                            color =
                                                if (selectedForBranch.isNotEmpty()) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            fontWeight =
                                                FontWeight
                                                    .ExtraBold
                                        )
                                    }
                                }

                                Spacer(
                                    Modifier.width(10.dp)
                                )

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = branch,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium,
                                        fontWeight =
                                            FontWeight
                                                .ExtraBold,
                                        maxLines = 2,
                                        overflow =
                                            TextOverflow
                                                .Ellipsis,
                                        textAlign =
                                            if (isEnglish) {
                                                TextAlign.Left
                                            } else {
                                                TextAlign.Right
                                            },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                    )

                                    Text(
                                        text =
                                            when {
                                                branchMissing ->
                                                    trLocal(
                                                        "חובה לבחור לפחות קבוצה אחת",
                                                        "Select at least one group"
                                                    )

                                                selectedForBranch
                                                    .isEmpty() ->
                                                    trLocal(
                                                        "לא נבחרו קבוצות",
                                                        "No groups selected"
                                                    )

                                                selectedForBranch
                                                    .size == 1 ->
                                                    trLocal(
                                                        "נבחרה קבוצה אחת",
                                                        "One group selected"
                                                    )

                                                else ->
                                                    trLocal(
                                                        "נבחרו ${selectedForBranch.size} קבוצות",
                                                        "${selectedForBranch.size} groups selected"
                                                    )
                                            },
                                        color =
                                            if (branchMissing) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelSmall,
                                        textAlign =
                                            if (isEnglish) {
                                                TextAlign.Left
                                            } else {
                                                TextAlign.Right
                                            },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                    )
                                }

                                Spacer(
                                    Modifier.width(8.dp)
                                )

                                Icon(
                                    imageVector =
                                        if (expanded) {
                                            Icons.Default
                                                .KeyboardArrowUp
                                        } else {
                                            Icons.Default
                                                .KeyboardArrowDown
                                        },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier =
                                        Modifier.size(22.dp)
                                )
                            }

                            if (expanded) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 1.dp
                                )

                                if (
                                    availableGroups.isEmpty()
                                ) {
                                    Text(
                                        text =
                                            trLocal(
                                                "לא נמצאו קבוצות בסניף זה",
                                                "No groups were found for this branch"
                                            ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall,
                                        textAlign =
                                            if (isEnglish) {
                                                TextAlign.Left
                                            } else {
                                                TextAlign.Right
                                            },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    14.dp
                                                )
                                    )
                                } else {
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal =
                                                        8.dp,
                                                    vertical =
                                                        8.dp
                                                ),
                                        verticalArrangement =
                                            Arrangement.spacedBy(
                                                6.dp
                                            )
                                    ) {
                                        availableGroups.forEach {
                                                group ->

                                            val checked =
                                                group in
                                                        selectedForBranch

                                            val groupShape =
                                                RoundedCornerShape(
                                                    14.dp
                                                )

                                            Row(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .clip(
                                                            groupShape
                                                        )
                                                        .background(
                                                            if (checked) {
                                                                MaterialTheme.colorScheme.primaryContainer
                                                            } else {
                                                                MaterialTheme.colorScheme.surface
                                                            }
                                                        )
                                                        .clickable {
                                                            val updatedGroups =
                                                                if (
                                                                    checked
                                                                ) {
                                                                    selectedForBranch
                                                                        .filterNot {
                                                                                selected ->
                                                                            selected ==
                                                                                    group
                                                                        }
                                                                } else {
                                                                    selectedForBranch +
                                                                            group
                                                                }
                                                                    .distinct()

                                                            val updatedMap =
                                                                selectedGroupsByBranch
                                                                    .toMutableMap()

                                                            updatedMap[
                                                                branch
                                                            ] =
                                                                updatedGroups

                                                            onGroupsByBranchChange(
                                                                updatedMap
                                                            )
                                                        }
                                                        .padding(
                                                            horizontal =
                                                                8.dp,
                                                            vertical =
                                                                4.dp
                                                        ),
                                                verticalAlignment =
                                                    Alignment
                                                        .CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked =
                                                        checked,
                                                    onCheckedChange =
                                                        null,
                                                    colors =
                                                        CheckboxDefaults.colors(
                                                            checkedColor = MaterialTheme.colorScheme.primary,
                                                            uncheckedColor = MaterialTheme.colorScheme.outline
                                                        )
                                                )

                                                Spacer(
                                                    Modifier.width(
                                                        6.dp
                                                    )
                                                )

                                                Text(
                                                    text =
                                                        registrationGroupLabelForUi(
                                                            group =
                                                                group,
                                                            isEnglish =
                                                                isEnglish
                                                        ),
                                                    color =
                                                        if (checked) {
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        },
                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .bodySmall,
                                                    fontWeight =
                                                        if (
                                                            checked
                                                        ) {
                                                            FontWeight
                                                                .ExtraBold
                                                        } else {
                                                            FontWeight
                                                                .SemiBold
                                                        },
                                                    textAlign =
                                                        if (
                                                            isEnglish
                                                        ) {
                                                            TextAlign
                                                                .Left
                                                        } else {
                                                            TextAlign
                                                                .Right
                                                        },
                                                    modifier =
                                                        Modifier
                                                            .weight(
                                                                1f
                                                            )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun MultiGroupsPicker(
    allGroupsAcrossBranches: List<String>,
    selectedGroups: List<String>,
    onGroupsChange: (List<String>) -> Unit,
    groupError: Boolean,
    highlightMissingRequired: Boolean = false,
    isEnglish: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var tempSelection by remember(selectedGroups) {
        mutableStateOf(selectedGroups.toList())
    }
    val ctx = LocalContext.current

    fun trLocal(he: String, en: String): String = if (isEnglish) en else he
    val align = if (isEnglish) TextAlign.Left else TextAlign.Right

    val showGroupMissing = (groupError || highlightMissingRequired) && selectedGroups.isEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { open ->
            expanded = open
            if (open) tempSelection = selectedGroups.toList()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        val display =
            if (selectedGroups.isEmpty()) {
                ""
            } else {
                selectedGroups.joinToString("\n") { group ->
                    registrationGroupLabelForUi(
                        group = group,
                        isEnglish = isEnglish
                    )
                }
            }

        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    text = trLocal("בחר/י קבוצה/ות", "Select group(s)"),
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
                .background(
                    if (showGroupMissing) Color(0xFFFFE4E6) else Color.White,
                    shape = MaterialTheme.shapes.medium
                ),
            textStyle = LocalTextStyle.current.copy(
                color = Color.Black,
                textAlign = align
            ),
            colors = registrationRequiredFieldColors(
                showMissing = showGroupMissing
            ),
            placeholder = {
                Text(
                    text = trLocal(
                        "בחר/י קבוצות מכל הסניפים",
                        "Select groups"
                    ),
                    textAlign = align,
                    modifier = Modifier.fillMaxWidth()
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
                                text = registrationGroupLabelForUi(
                                    group = g,
                                    isEnglish = isEnglish
                                ),
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
                                tempSelection + g
                            }

                        tempSelection = newSelection

                        // ⭐ שמירה מיידית ללא מגבלת כמות
                        onGroupsChange(newSelection)
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
                        onGroupsChange(tempSelection)
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
                "חובה לבחור לפחות קבוצה אחת",
                "Please select at least one group"
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
    showMissing: Boolean,
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
    val beltIsMissing = showMissing && currentBelt == null

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
            label = {
                Text(
                    if (isEnglish) "Current KAMI belt rank" else "דרגת חגורה נוכחית (ק.מ.י)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .registrationFieldPosition("belt")
                .fillMaxWidth()
                .defaultMinSize(minHeight = 46.dp)
                .background(
                    if (beltIsMissing) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                ),
            colors = registrationRequiredFieldColors(showMissing = beltIsMissing),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
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

    }

    if (expanded) {
        Dialog(onDismissRequest = { expanded = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .heightIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                ),
                shadowElevation = 2.dp
            ) {
                Column {
                    Text(
                        text = if (isEnglish) "Select belt rank" else "בחר דרגת חגורה",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                            .padding(16.dp)
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(beltOptions.size) { index ->
                            val belt = beltOptions[index]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onBeltChange(belt.id)
                                        expanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = belt.color,
                                    tonalElevation = 0.dp,
                                    shape = RoundedCornerShape(50),
                                    border = if (belt.id == "white") {
                                        BorderStroke(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        null
                                    },
                                    modifier = Modifier.size(14.dp)
                                ) {}

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = beltLabel(belt),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = if (isEnglish) {
                                        TextAlign.Left
                                    } else {
                                        TextAlign.Right
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { expanded = false }) {
                            Text(if (isEnglish) "Close" else "סגור")
                        }
                    }
                }
            }
        }
    }
}



