@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package il.kmi.app.screens.registration

import android.annotation.SuppressLint
import android.widget.Toast
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import il.kmi.shared.domain.Belt
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.ui.ext.color

@Composable
fun RegistrationFormContent(
    isCoach: Boolean,
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
) {
    val scroll = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    val allGroupsAcrossBranches by remember(selectedBranches) {
        derivedStateOf {
            selectedBranches
                .flatMap { branch ->
                    val key = branch.trim()
                    TrainingCatalog.ageGroupsByBranch[key]
                        ?: TrainingCatalog.ageGroupsByBranch[key.replace("’", "'")]
                        ?: TrainingCatalog.ageGroupsByBranch[key.replace("־", "-")]
                        ?: emptyList()
                }
                .distinct()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ===== Personal =====
        OutlinedTextField(
            value = fullName,
            onValueChange = { onFullNameChange(it) },
            label = { Text("שם מלא", color = Color.Black) },
            singleLine = true,
            isError = fullNameError,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
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
            Text("שדה חובה", color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = phone,
            onValueChange = { onPhoneChange(it) },
            label = { Text("טלפון", color = Color.Black) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phoneError,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .background(Color.White, shape = MaterialTheme.shapes.medium),
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
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
            Text("שדה חובה", color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = email,
            onValueChange = { onEmailChange(it) },
            label = { Text("מייל", color = Color.Black) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailError,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
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
                "שדה חובה / פורמט מייל לא תקין",
                color = MaterialTheme.colorScheme.error
            )
        }

        // ===== מין המשתמש =====
        Text(
            text = "מין המשתמש",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = gender == "male",
                onClick = { onGenderChange("male") },
                label = { Text("זכר", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = gender == "male",
                    borderColor = Color(0xFF334155),
                    selectedBorderColor = Color(0xFF0EA5E9),
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF0B1220),
                    selectedContainerColor = Color(0xFF0EA5E9),
                    labelColor = Color(0xFFE5E7EB),
                    selectedLabelColor = Color.White
                )
            )

            FilterChip(
                selected = gender == "female",
                onClick = { onGenderChange("female") },
                label = { Text("נקבה", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = gender == "female",
                    borderColor = Color(0xFF334155),
                    selectedBorderColor = Color(0xFFEC4899),
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF0B1220),
                    selectedContainerColor = Color(0xFFEC4899),
                    labelColor = Color(0xFFE5E7EB),
                    selectedLabelColor = Color.White
                )
            )
        }
        if (genderError) {
            Text("יש לבחור מין", color = MaterialTheme.colorScheme.error)
        }

        // ===== תאריך לידה =====
        Text(
            text = "תאריך לידה",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
        )

        BirthDatePicker(
            year = birthYear,
            month = birthMonth,
            day = birthDay,
            onYearChange = { onBirthYearChange(it) },
            onMonthChange = { onBirthMonthChange(it) },
            onDayChange = { onBirthDayChange(it) }
        )

        // ===== Account =====
        OutlinedTextField(
            value = username,
            onValueChange = { onUsernameChange(it) },
            label = { Text("שם משתמש", color = Color.Black) },
            singleLine = true,
            isError = usernameError,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
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
            Text("שדה חובה", color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = password,
            onValueChange = { onPasswordChange(it) },
            label = { Text("סיסמה", color = Color.Black) },
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
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
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
            Text("שדה חובה", color = MaterialTheme.colorScheme.error)
        }

        // ===== Region + Branches =====
        RegionAndMultiBranchPicker(
            selectedRegion = selectedRegion,
            selectedBranches = selectedBranches,
            onRegionChange = onRegionChange,
            onBranchesConfirm = onBranchesChange,
            regionError = regionError,
            branchError = branchError
        )

        // ===== Groups (1–3) =====
        if (selectedBranches.isNotEmpty()) {
            MultiGroupsPicker(
                allGroupsAcrossBranches = allGroupsAcrossBranches,
                selectedGroups = selectedGroups,
                onGroupsChange = onGroupsChange,
                groupError = groupError
            )
        }

        // ===== Belt =====
        if (!isCoach) {
            BeltPicker(
                currentBeltId = currentBeltId,
                onBeltChange = onBeltChange
            )
        }

        // ===== Preferences =====
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = subscribeSms,
                onCheckedChange = onSubscribeSmsChange
            )
            Spacer(Modifier.width(8.dp))
            Text("ארצה לקבל עדכונים בהודעת SMS לגבי אימונים קרובים")
        }

        // ===== Terms =====
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
                Text("אני מאשר את תנאי השימוש ומדיניות הפרטיות")
                Text(
                    "קרא עוד",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BirthDatePicker(
    year: Int,
    month: Int,
    day: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
) {
    // מציגים דו-ספרתי/ארבע-ספרתי
    var dayText by remember(day) { mutableStateOf(day.coerceIn(1, 31).toString().padStart(2, '0')) }
    var monthText by remember(month) { mutableStateOf(month.coerceIn(1, 12).toString().padStart(2, '0')) }
    var yearText by remember(year) { mutableStateOf(year.coerceIn(1950, 2026).toString().padStart(4, '0')) }

    // צבעי רקע מודרניים לכל שדה
    val shape = RoundedCornerShape(14.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )

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
            label = { Text("יום") },
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
            label = { Text("חודש") },
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
            label = { Text("שנה") },
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
    selectedRegion: String,
    selectedBranches: List<String>,
    onRegionChange: (String) -> Unit,
    onBranchesConfirm: (List<String>) -> Unit,
    regionError: Boolean,
    branchError: Boolean,
    fieldHeight: Dp = 52.dp
) {
    val ctx = LocalContext.current

    val regions = remember {
        TrainingCatalog.activeRegions()
    }

    val allBranches = remember(selectedRegion) {
        TrainingCatalog.branchesFor(selectedRegion)
    }

    // 🔹 סטטוס אזור (פעיל / לא פעיל) + הודעה "אין סניפים..."
    val isRegionActive = remember(selectedRegion) {
        selectedRegion.isNotBlank() && TrainingCatalog.isRegionActive(selectedRegion)
    }
    val regionStatusMessage = remember(selectedRegion) {
        TrainingCatalog.regionStatusMessage(selectedRegion)
    }

    var regionExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = regionExpanded,
        onExpandedChange = { regionExpanded = !regionExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedRegion,
            onValueChange = { },
            readOnly = true,
            label = { Text("מחוז / אזור", color = Color.Black) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded)
            },
            isError = regionError,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .defaultMinSize(minHeight = fieldHeight)
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
            placeholder = { Text("בחר/י אזור") }
        )

        ExposedDropdownMenu(
            expanded = regionExpanded,
            onDismissRequest = { regionExpanded = false }
        ) {
            regions.forEach { region ->
                DropdownMenuItem(
                    text = { Text(region) },
                    onClick = {
                        regionExpanded = false
                        onRegionChange(region)
                    }
                )
            }
        }
    }

    if (regionError) {
        Text("שדה חובה", color = MaterialTheme.colorScheme.error)
    }

    // 🔹 הודעת "אין סניפים זמינים לאזור זה"
    if (regionStatusMessage != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = regionStatusMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    Spacer(Modifier.height(8.dp))

    var branchesExpanded by remember { mutableStateOf(false) }
    var tempSelection by remember { mutableStateOf(selectedBranches.toList()) }

    ExposedDropdownMenuBox(
        expanded = branchesExpanded,
        onExpandedChange = { open ->
            // אם האזור לא פעיל – במקום לפתוח רשימת סניפים מציגים הודעה
            if (open && !isRegionActive) {
                Toast.makeText(
                    ctx,
                    regionStatusMessage ?: "אין סניפים זמינים באזור זה",
                    Toast.LENGTH_SHORT
                ).show()
                branchesExpanded = false
            } else {
                branchesExpanded = open
                if (open) tempSelection = selectedBranches.toList()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        val display = if (selectedBranches.isEmpty()) "" else selectedBranches.joinToString("\n")

        OutlinedTextField(
            value = display,
            onValueChange = { },
            readOnly = true,
            label = { Text("סניפים (עד 3)", color = Color.Black) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchesExpanded)
            },
            isError = branchError,
            minLines = (if (selectedBranches.isEmpty()) 1 else selectedBranches.size)
                .coerceAtMost(4),
            maxLines = 6,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
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
            placeholder = { Text("בחר/י 1–3 סניפים") }
        )

        ExposedDropdownMenu(
            expanded = branchesExpanded,
            onDismissRequest = { branchesExpanded = false }
        ) {
            allBranches.forEach { branch ->
                val checked = branch in tempSelection
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(branch)
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
                                        "ניתן לבחור עד 3 סניפים",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    tempSelection
                                }
                            }
                    }
                )
            }

            Divider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { tempSelection = emptyList() }) {
                    Text("נקה")
                }
                Button(
                    onClick = {
                        onBranchesConfirm(tempSelection)
                        branchesExpanded = false
                    }
                ) {
                    Text("אישור")
                }
            }
        }
    }

    if (branchError) {
        Text("יש לבחור לפחות סניף אחד", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun MultiGroupsPicker(
    allGroupsAcrossBranches: List<String>,
    selectedGroups: List<String>,
    onGroupsChange: (List<String>) -> Unit,
    groupError: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    var tempSelection by remember { mutableStateOf(selectedGroups.toList()) }
    val ctx = LocalContext.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { open ->
            expanded = open
            if (open) tempSelection = selectedGroups.toList()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        val display = if (selectedGroups.isEmpty()) "" else selectedGroups.joinToString("\n")

        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text("בחר/י קבוצה/ות (עד 3)", color = Color.Black) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = groupError,
            minLines = (if (selectedGroups.isEmpty()) 1 else selectedGroups.size).coerceAtMost(4),
            maxLines = 6,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
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
            placeholder = { Text("בחר/י 1–3 קבוצות מכל הסניפים") }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            allGroupsAcrossBranches.forEach { g ->
                val checked = g in tempSelection
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(g)
                        }
                    },
                    onClick = {
                        tempSelection =
                            if (checked) tempSelection.filterNot { it == g }
                            else if (tempSelection.size < 3) tempSelection + g
                            else {
                                Toast.makeText(ctx, "ניתן לבחור עד 3 קבוצות", Toast.LENGTH_SHORT).show()
                                tempSelection
                            }
                    }
                )
            }

            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { tempSelection = emptyList() }) { Text("נקה") }
                Button(onClick = {
                    onGroupsChange(tempSelection.take(3))
                    expanded = false
                }) { Text("אישור") }
            }
        }
    }

    if (groupError) {
        Text("חובה לבחור לפחות קבוצה אחת (עד 3)", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun BeltPicker(
    currentBeltId: String,
    onBeltChange: (String) -> Unit
) {
    val beltOptions = Belt.order
    var expanded by remember { mutableStateOf(false) }
    val currentBelt = beltOptions.firstOrNull { it.id == currentBeltId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentBelt?.heb ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("דרגת חגורה נוכחית (ק.מ.י)", color = Color.Black) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
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
            placeholder = { Text("בחר/י דרגה") }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            beltOptions.forEach { belt ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = belt.color,
                                tonalElevation = 0.dp,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(14.dp)
                            ) {}
                            Spacer(Modifier.width(8.dp))
                            Text(belt.heb)
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

