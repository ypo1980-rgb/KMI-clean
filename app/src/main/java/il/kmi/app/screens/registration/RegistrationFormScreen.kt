@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package il.kmi.app.screens.registration

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import il.kmi.app.KmiViewModel
import il.kmi.app.screens.admin.AdminAccess
import il.kmi.app.training.TrainingCatalog
import il.kmi.shared.prefs.KmiPrefs

// === רשימת מאמנים מורשים ===
object CoachWhitelist {
    // מפה: טלפון → שם
    val allowedPhones: Map<String, String> = mapOf(
        "0526664660" to "יובל פולק",
        "0524887178" to "יוני מלסה",
        "0526969287" to "איציק ביטון",
        "0585911518" to "אדם הולצמן",
        "0526319090" to "גל חג'ג'",
        "0505300596" to "אבי אביסדון"
    )

    // מפה: אימייל → שם
    val allowedEmails: Map<String, String> = mapOf(
        "ypo1980@gmail.com" to "יובל פולק",
        "yonatanmalesa99.com" to "יוני מלסה",
        "coach3@example.com" to "מאמן 3"
        // ... תוסיף כאן עד ~20
    )
}

@Composable
fun RegistrationFormScreen(
    initial: String = "trainee",
    onBack: () -> Unit,
    onRegistrationComplete: () -> Unit,
    onOpenLegal: () -> Unit,
    onOpenTerms: () -> Unit,
    vm: KmiViewModel,
    onOpenDrawer: () -> Unit = { il.kmi.app.ui.DrawerBridge.open() },
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    startAtProfile: Boolean = false
) {
    // 0=מתאמן, 1=מאמן
    val initialIsCoach = initial.equals("coach", ignoreCase = true)
    var selectedTab by rememberSaveable { mutableIntStateOf(if (initialIsCoach) 1 else 0) }
    val isCoach = selectedTab == 1

    val ctx = LocalContext.current
    val userSp = remember(ctx) { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }

    // דיאלוג קוד מאמן
    var showCodeDialog by rememberSaveable { mutableStateOf(false) }
    var coachCode by rememberSaveable { mutableStateOf("") }

    // ======== STATE של הטופס ========
    var fullName by rememberSaveable { mutableStateOf(sp.getString("fullName", "") ?: "") }
    var phone by rememberSaveable { mutableStateOf(sp.getString("phone", "") ?: "") }
    var email by rememberSaveable { mutableStateOf(sp.getString("email", "") ?: "") }

    // ✅ הרשאות רישום לפי whitelist
    val normalizedPhone = remember(phone) { phone.filter { it.isDigit() } }
    val normalizedEmail = remember(email) { email.trim().lowercase() }

    val isWhitelistedCoach = remember(normalizedPhone, normalizedEmail) {
        CoachWhitelist.allowedPhones.containsKey(normalizedPhone) ||
                CoachWhitelist.allowedEmails.containsKey(normalizedEmail)
    }

    // ✅ חדש: ADMIN (Firestore: admins/{uid}.enabled)
    var isAdmin by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isAdmin = runCatching { AdminAccess.isCurrentUserAdmin() }.getOrDefault(false)
    }

    // ✅ נעילה אוטומטית:
    // - אם ADMIN → לא נועלים כלום
    // - אם מורשה מאמן → מעבירים לטאב מאמן ונועלים
    // - אם לא מורשה → מעבירים לטאב מתאמן ונועלים
    LaunchedEffect(isWhitelistedCoach, isAdmin) {
        if (!isAdmin) {
            selectedTab = if (isWhitelistedCoach) 1 else 0
        }
    }

    var username by rememberSaveable { mutableStateOf(sp.getString("username", "") ?: "") }
    var password by rememberSaveable { mutableStateOf(sp.getString("password", "") ?: "") }

    // תאריך לידה
    var birthDay by rememberSaveable {
        mutableStateOf(sp.getString("birth_day", "1")?.toIntOrNull() ?: 1)
    }
    var birthMonth by rememberSaveable {
        mutableStateOf(sp.getString("birth_month", "1")?.toIntOrNull() ?: 1)
    }
    var birthYear by rememberSaveable {
        mutableStateOf(sp.getString("birth_year", "2000")?.toIntOrNull() ?: 2000)
    }

    // מין (gender) – נשמר ב-SP
    var gender by rememberSaveable {
        mutableStateOf(sp.getString("gender", "") ?: "")
    }

    // אזור / סניפים / קבוצות
    var selectedRegion by rememberSaveable { mutableStateOf(sp.getString("region", "") ?: "") }
    var selectedBranch by rememberSaveable { mutableStateOf(sp.getString("branch", "") ?: "") }
    var selectedGroup by rememberSaveable { mutableStateOf(sp.getString("age_group", "") ?: "") }

    // ✅ חדש: סניף/קבוצה פעילים (ערך יחיד)
    var activeBranch by rememberSaveable { mutableStateOf(sp.getString("active_branch", "") ?: "") }
    var activeGroup by rememberSaveable { mutableStateOf(sp.getString("active_group", "") ?: "") }

    // --- סניפים נבחרים (עד 3) — מקור אמת יחיד ---
    val selectedBranches = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        val saved = (sp.getString("branch", "") ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        mutableStateListOf<String>().apply { addAll(saved.take(3)) }
    }

    // קבוצות נבחרות (עד 3)
    val selectedGroups = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }

    // ✅ שמירה אוטומטית של activeBranch/activeGroup
    LaunchedEffect(selectedBranches.toList(), selectedGroups.toList()) {
        if (activeBranch.isBlank()) activeBranch = selectedBranches.firstOrNull()?.trim().orEmpty()
        if (activeGroup.isBlank()) activeGroup = selectedGroups.firstOrNull()?.trim().orEmpty()
    }

    // העדפות
    var subscribeSms by rememberSaveable { mutableStateOf(sp.getBoolean("subscribeSms", false)) }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

    // שגיאות
    var fullNameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var regionError by remember { mutableStateOf(false) }
    var branchError by remember { mutableStateOf(false) }
    var groupError by remember { mutableStateOf(false) }
    var termsError by remember { mutableStateOf(false) }
    var genderError by remember { mutableStateOf(false) }

    // קטלוג
    val branchesByRegion = TrainingCatalog.branchesByRegion
    val groupsByBranch = TrainingCatalog.ageGroupsByBranch

    // חגורה
    var currentBeltId by rememberSaveable { mutableStateOf(sp.getString("current_belt", "") ?: "") }

    // ==== התאמות ושיחזורים מה-SP ====
    LaunchedEffect(Unit) {
        val raw = sp.getString("age_groups", null) ?: sp.getString("age_group", "") ?: ""
        val saved = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        selectedGroups.clear()
        selectedGroups.addAll(saved.take(3))
    }

    LaunchedEffect(selectedRegion) {
        val branchesForRegion = branchesByRegion[selectedRegion].orEmpty()
        if (selectedBranch.isNotBlank() && !branchesForRegion.contains(selectedBranch)) {
            selectedBranch = ""
            selectedGroup = ""
        }
    }

    LaunchedEffect(isCoach) {
        if (isCoach) currentBeltId = ""
    }

    LaunchedEffect(selectedBranches.toList(), groupsByBranch) {
        val unionGroups = selectedBranches
            .flatMap { branch ->
                val normalized = branch.trim().replace("’", "'").replace("־", "-")
                groupsByBranch[normalized].orEmpty()
            }
            .distinct()

        val filtered = selectedGroups.filter { it in unionGroups }.take(3)
        if (filtered.size != selectedGroups.size) {
            selectedGroups.clear()
            selectedGroups.addAll(filtered)
        }
    }

    // רקע לפי הטאב
    val headerBrush = remember(isCoach) {
        if (isCoach) {
            Brush.linearGradient(
                colors = listOf(Color(0xFF141E30), Color(0xFF243B55), Color(0xFF0EA5E9))
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFF7F00FF), Color(0xFF3F51B5), Color(0xFF03A9F4))
            )
        }
    }

    // --------------------------------
    // פונקציית שליחה – יחידה אחת בלבד
    // --------------------------------
    fun submitRegistration() {
        var valid = true

        // ✅ אכיפה קשיחה רק למי שלא ADMIN
        if (!isAdmin) {
            if (isWhitelistedCoach && !isCoach) {
                Toast.makeText(ctx, "מאמן מורשה חייב להירשם כמאמן בלבד", Toast.LENGTH_LONG).show()
                return
            }
            if (!isWhitelistedCoach && isCoach) {
                Toast.makeText(ctx, "הרישום כמאמן מותר רק למאמנים מורשים", Toast.LENGTH_LONG).show()
                return
            }
        }

        if (fullName.isBlank()) {
            fullNameError = true; valid = false
        }
        if (phone.filter { it.isDigit() }.length !in 9..12) {
            phoneError = true; valid = false
        }
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailError = true; valid = false
        }
        if (username.isBlank()) {
            usernameError = true; valid = false
        }
        if (password.isBlank()) {
            passwordError = true; valid = false
        }
        if (selectedRegion.isBlank()) {
            regionError = true; valid = false
        }

        val hasAtLeastOneBranch = selectedBranch.isNotBlank() || selectedBranches.isNotEmpty()
        if (!hasAtLeastOneBranch) {
            branchError = true; valid = false
        }

        if (selectedGroups.isEmpty()) {
            groupError = true; valid = false
        }

        // מין חובה
        if (gender.isBlank()) {
            genderError = true; valid = false
        }

        if (!acceptedTerms) {
            termsError = true; valid = false
        }

        // אימות מאמן (רק אם לא ADMIN)
        if (isCoach && !isAdmin) {
            val normalizedPhoneLocal = phone.filter { it.isDigit() }
            val normalizedEmailLocal = email.trim().lowercase()

            val phoneOk = CoachWhitelist.allowedPhones.containsKey(normalizedPhoneLocal)
            val emailOk = CoachWhitelist.allowedEmails.containsKey(normalizedEmailLocal)

            if (!phoneOk && !emailOk) {
                Toast.makeText(ctx, "הרישום כמאמן מותר רק למאמנים מורשים", Toast.LENGTH_LONG).show()
                return
            }
        }

        if (!valid) return

        // ✅ role סופי:
        // ADMIN בוחר לפי הטאב, אחרת לפי whitelist
        val roleFinal = if (isAdmin) {
            if (isCoach) "coach" else "trainee"
        } else {
            if (isWhitelistedCoach) "coach" else "trainee"
        }

        val groupsCsv = selectedGroups.joinToString(", ")
        val primaryGroup = selectedGroups.firstOrNull() ?: ""

        val activeBranchFinal =
            (activeBranch.takeIf { it.isNotBlank() }
                ?: selectedBranches.firstOrNull()
                ?: selectedBranch).trim()

        val activeGroupFinal =
            (activeGroup.takeIf { it.isNotBlank() }
                ?: selectedGroups.firstOrNull()
                ?: primaryGroup).trim()

        // ✅ branchesFinal תמיד עקבי מול מקור האמת (selectedBranches)
        val branchesFinal =
            if (selectedBranches.isNotEmpty()) selectedBranches.joinToString(", ")
            else selectedBranch.trim()

        // שמירה ב-SP הראשי
        sp.edit()
            .putString("fullName", fullName)
            .putString("phone", phone)
            .putString("email", email)
            .putString("region", selectedRegion)
            .putString("branch", branchesFinal)              // CSV
            .putString("active_branch", activeBranchFinal)   // ✅ חדש
            .putString("age_groups", groupsCsv)              // CSV
            .putString("age_group", primaryGroup)            // תאימות
            .putString("group", primaryGroup)                // תאימות
            .putString("active_group", activeGroupFinal)     // ✅ חדש
            .putString("username", username)
            .putString("password", password)
            .putBoolean("subscribeSms", subscribeSms)
            .putString("user_role", roleFinal)
            .putString("gender", gender)                     // 👈 חדש
            // תאריך לידה
            .putString("birth_day", birthDay.toString())
            .putString("birth_month", birthMonth.toString())
            .putString("birth_year", birthYear.toString())
            .apply()

        // חגורה – רק למתאמן
        if (roleFinal == "trainee") {
            sp.edit().putString("current_belt", currentBeltId).apply()
        } else {
            sp.edit().remove("current_belt").apply()
        }

        // userSp – אחידות
        userSp.edit().apply {
            putString("user_role", roleFinal)
            putString("region", selectedRegion)
            putString("branch", branchesFinal)
            putString("active_branch", activeBranchFinal)    // ✅ חדש
            putString("age_groups", groupsCsv)
            putString("age_group", primaryGroup)
            putString("group", primaryGroup)
            putString("active_group", activeGroupFinal)      // ✅ חדש
            putString("birth_day", birthDay.toString())
            putString("birth_month", birthMonth.toString())
            putString("birth_year", birthYear.toString())
            putString("gender", gender)                      // 👈 חדש
            if (roleFinal == "trainee") putString("belt_current", currentBeltId) else remove("belt_current")
            apply()
        }

        // Persist – KMP
        kmiPrefs.fullName = fullName
        kmiPrefs.phone = phone
        kmiPrefs.email = email
        kmiPrefs.region = selectedRegion
        kmiPrefs.branch = branchesFinal
        kmiPrefs.ageGroup = primaryGroup
        kmiPrefs.username = username
        kmiPrefs.password = password

        // --- שמירה ל-Firestore: מאגר המשתמשים המרכזי ---
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            // תאריך לידה בפורמט YYYY-MM-DD
            val birthDate = "%04d-%02d-%02d".format(birthYear, birthMonth, birthDay)

            // ✅ מקור אמת לסניפים: קודם selectedBranches (רשימה), ואם ריק – selectedBranch (בודד)
            val branchesListFinal: List<String> =
                if (selectedBranches.isNotEmpty()) {
                    selectedBranches.map { it.trim() }.filter { it.isNotBlank() }.distinct()
                } else {
                    listOf(selectedBranch.trim()).filter { it.isNotBlank() }
                }

            val firestoreData = hashMapOf(
                "uid" to uid,
                "role" to roleFinal,
                "fullName" to fullName,
                "phone" to phone,
                "email" to email,
                "region" to selectedRegion,
                "branches" to branchesListFinal,
                "branchesCsv" to branchesFinal,
                "activeBranch" to activeBranchFinal,   // ✅ חדש
                "groups" to selectedGroups.toList(),
                "primaryGroup" to primaryGroup,
                "activeGroup" to activeGroupFinal,     // ✅ חדש
                "birthDate" to birthDate,
                "gender" to gender,                    // 👈 חדש
                "belt" to if (roleFinal == "trainee") currentBeltId else "",
                "subscribeSms" to subscribeSms,
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(firestoreData, SetOptions.merge())
        }

        Toast.makeText(ctx, "הרישום נשמר בהצלחה ✅", Toast.LENGTH_SHORT).show()

        if (roleFinal == "coach") {
            val code = "%06d".format(java.security.SecureRandom().nextInt(1_000_000))
            coachCode = code
            sp.edit().putString("coach_code", code).apply()
            userSp.edit().putString("coach_code", code).apply()
            showCodeDialog = true
        } else {
            onRegistrationComplete()
        }
    }

    // ====== UI ======
    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "טופס רישום",
                showRoleStatus = false,
                onOpenDrawer = onOpenDrawer,
                lockSearch = true,
                showBottomActions = true
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Divider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                    Button(
                        onClick = { submitRegistration() },
                        enabled = acceptedTerms,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = Color(0xFFB0BEC5),
                            disabledContentColor = Color.Black
                        )
                    ) {
                        Text("סיום רישום")
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(headerBrush)
                .padding(padding)
        ) {
            Spacer(Modifier.height(8.dp))

            // טאבים חיצוניים
            RegistrationTabs(
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    // ✅ ADMIN יכול לבחור חופשי
                    if (isAdmin) {
                        selectedTab = newTab
                        return@RegistrationTabs
                    }

                    when {
                        // ✅ מאמן מורשה: מותר רק טאב מאמן
                        isWhitelistedCoach -> {
                            if (newTab != 1) {
                                Toast.makeText(ctx, "מאמן מורשה נרשם רק כמאמן", Toast.LENGTH_SHORT).show()
                            }
                            selectedTab = 1
                        }

                        // ✅ לא מורשה: מותר רק טאב מתאמן
                        else -> {
                            if (newTab == 1) {
                                Toast.makeText(ctx, "הרישום כמאמן מותר רק למאמנים מורשים", Toast.LENGTH_SHORT).show()
                            }
                            selectedTab = 0
                        }
                    }
                }
            )

            Spacer(Modifier.height(6.dp))

// כל התוכן עבר לפה:
            RegistrationFormContent(
                isCoach = isCoach,
                fullName = fullName,
                onFullNameChange = {
                    fullName = it
                    fullNameError = false
                },
                fullNameError = fullNameError,
                phone = phone,
                onPhoneChange = {
                    phone = it
                    phoneError = false
                },
                phoneError = phoneError,
                email = email,
                onEmailChange = {
                    email = it
                    emailError = false
                },
                emailError = emailError,

                // 👇 חדש – שדה מין
                gender = gender,
                onGenderChange = {
                    gender = it
                    genderError = false
                },
                genderError = genderError,

                birthDay = birthDay,
                birthMonth = birthMonth,
                birthYear = birthYear,
                onBirthDayChange = { birthDay = it },
                onBirthMonthChange = { birthMonth = it },
                onBirthYearChange = { birthYear = it },
                username = username,
                onUsernameChange = {
                    username = it
                    usernameError = false
                },
                usernameError = usernameError,
                password = password,
                onPasswordChange = {
                    password = it
                    passwordError = false
                },
                passwordError = passwordError,
                selectedRegion = selectedRegion,
                onRegionChange = {
                    selectedRegion = it
                    selectedBranches.clear()
                    selectedBranch = ""
                    selectedGroups.clear()
                    regionError = false
                },
                selectedBranches = selectedBranches,
                onBranchesChange = { list ->
                    selectedBranches.clear()
                    selectedBranches.addAll(list.take(3))
                    // תאימות ישנה
                    selectedBranch = selectedBranches.singleOrNull() ?: ""
                    branchError = selectedBranches.isEmpty()
                },
                selectedGroups = selectedGroups,
                onGroupsChange = { list ->
                    selectedGroups.clear()
                    selectedGroups.addAll(list.take(3))
                    groupError = selectedGroups.isEmpty()
                },
                regionError = regionError,
                branchError = branchError,
                groupError = groupError,
                currentBeltId = currentBeltId,
                onBeltChange = { currentBeltId = it },
                subscribeSms = subscribeSms,
                onSubscribeSmsChange = { subscribeSms = it },
                acceptedTerms = acceptedTerms,
                onAcceptedTermsChange = {
                    acceptedTerms = it
                    if (it) termsError = false
                },
                onOpenTerms = onOpenTerms
            )

            if (!acceptedTerms && termsError) {
                Text(
                    "חובה לאשר תנאי שימוש ומדיניות פרטיות",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    // --- דיאלוג קוד מאמן (אם רלוונטי) ---
    if (showCodeDialog) {
        val normalizedPhone = phone.filter { it.isDigit() }
        val nameFromPhone = CoachWhitelist.allowedPhones[normalizedPhone]
        val nameFromEmail = CoachWhitelist.allowedEmails[email.trim()]
        val coachDisplayName = nameFromPhone ?: nameFromEmail ?: fullName.ifBlank { "מאמן" }

        AlertDialog(
            onDismissRequest = { /* לא נסגור לבד */ },
            confirmButton = {
                Button(
                    onClick = {
                        showCodeDialog = false
                        onRegistrationComplete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("אישור") }
            },
            title = { Text(text = "שלום, $coachDisplayName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "קוד המאמן שלך:")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = coachCode,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Text(
                        text = "עליך לשמור את קוד המאמן שהתקבל לכניסה למערכת ולפעולות מתקדמות (כמו שליחת הודעות לקבוצה)."
                    )
                }
            }
        )
    }
}

