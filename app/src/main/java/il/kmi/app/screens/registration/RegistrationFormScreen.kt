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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import il.kmi.app.KmiViewModel
import il.kmi.app.screens.admin.AdminAccess
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.database.KmiDatabaseProvider
import il.kmi.shared.prefs.KmiPrefs
import il.kmi.app.FcmTokenManager

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
        "yonatanmalesa99@gmail.com" to "יוני מלסה",
        "avi.abeceedon@gmail.com" to "אבי אביסדון"
        // ... תוסיף כאן עד ~20
    )
}

private fun isSuperTesterUser(
    email: String,
    phoneDigits: String,
    firebaseUid: String
): Boolean {
    return email.trim().lowercase() == "ypo1980@gmail.com" ||
            phoneDigits.filter { it.isDigit() } == "0526664660" ||
            firebaseUid == "DBoyoVVpsrVUX0ukhKwNyQlKUKY2"
}

@Composable
private fun RegistrationFormLockedTopBar(
    title: String,
    isEnglish: Boolean,
    onLockedAction: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp), // 56 top + 86 bottom — כמו KmiTopBar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // איזון מול כפתור התפריט כדי שהכותרת תישאר במרכז
                    Spacer(Modifier.width(38.dp))

                    Text(
                        text = title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            color = Color(0xFF111827)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE6E6EE))
                            .clickable {
                                onLockedAction(if (isEnglish) "Menu" else "התפריט")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = if (isEnglish) "Menu" else "תפריט",
                            tint = Color(0xFF4B478F),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RegistrationFormLockedTopAction(
                    label = if (isEnglish) "Search" else "חיפוש",
                    icon = Icons.Filled.Search,
                    iconTint = Color(0xFF12B886),
                    circleColor = Color(0xFFD6F5EA),
                    onClick = {
                        onLockedAction(if (isEnglish) "Search" else "אייקון החיפוש")
                    }
                )

                RegistrationFormLockedTopAction(
                    label = if (isEnglish) "Home" else "בית",
                    icon = Icons.Filled.Home,
                    iconTint = Color(0xFF2F6FE4),
                    circleColor = Color(0xFFD8E5FF),
                    onClick = {
                        onLockedAction(if (isEnglish) "Home" else "אייקון הבית")
                    }
                )

                RegistrationFormLockedTopAction(
                    label = if (isEnglish) "Settings" else "הגדרות",
                    icon = Icons.Filled.Settings,
                    iconTint = Color(0xFFFF9800),
                    circleColor = Color(0xFFFFEAC2),
                    onClick = {
                        onLockedAction(if (isEnglish) "Settings" else "אייקון ההגדרות")
                    }
                )

                RegistrationFormLockedTopAction(
                    label = if (isEnglish) "Assistant" else "עוזר",
                    icon = Icons.Filled.Info,
                    iconTint = Color(0xFF7C4DFF),
                    circleColor = Color(0xFFE5D8FF),
                    onClick = {
                        onLockedAction(if (isEnglish) "Assistant" else "אייקון העוזר")
                    }
                )

                RegistrationFormLockedTopAction(
                    label = if (isEnglish) "Share" else "שתף",
                    icon = Icons.Filled.Share,
                    iconTint = Color(0xFFE83E8C),
                    circleColor = Color(0xFFFFD7EA),
                    onClick = {
                        onLockedAction(if (isEnglish) "Share" else "אייקון השיתוף")
                    }
                )
            }
        }
    }
}

@Composable
private fun RegistrationFormLockedTopAction(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    circleColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = circleColor,
            modifier = Modifier.size(50.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(27.dp)
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                color = Color(0xFF4B4F5C)
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
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

    val langManager = remember(ctx) {
        il.kmi.shared.localization.AppLanguageManager(ctx)
    }

    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    fun finishRegistrationFlow() {
        if (startAtProfile) {
            // ✅ עריכת פרופיל — חוזרים למסך הקודם ולא מציגים שוב מסך טעינה
            onBack()
        } else {
            // ✅ כניסה ראשונה / השלמת רישום — ממשיכים למסך הטעינה הדינמי
            onRegistrationComplete()
        }
    }

    val isGoogleAuth = remember(sp, startAtProfile) {
        val authProvider = sp.getString("authProvider", "").orEmpty()
        val googleLogin = sp.getBoolean("google_login", false)
        val skipOtp = sp.getBoolean("skip_otp", false)

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val firebaseIsGoogle = firebaseUser
            ?.providerData
            ?.any { provider -> provider.providerId == "google.com" } == true

        // ✅ שדות שם משתמש / סיסמה מוסתרים רק במסלול Google אמיתי.
        // ברישום רגיל אסור שדגלים ישנים מ־SharedPreferences יסתירו אותם.
        firebaseIsGoogle &&
                authProvider == "google" &&
                googleLogin &&
                skipOtp &&
                !startAtProfile
    }

    // דיאלוג קוד מאמן
    var showCodeDialog by rememberSaveable { mutableStateOf(false) }
    var coachCode by rememberSaveable { mutableStateOf("") }

    // ======== STATE של הטופס ========
    var fullName by rememberSaveable { mutableStateOf(sp.getString("fullName", "") ?: "") }
    var phone by rememberSaveable { mutableStateOf(sp.getString("phone", "") ?: "") }
    var email by rememberSaveable { mutableStateOf(sp.getString("email", "") ?: "") }

    LaunchedEffect(isGoogleAuth) {
        if (isGoogleAuth) {
            val firebaseUser = FirebaseAuth.getInstance().currentUser

            if (fullName.isBlank()) {
                fullName = firebaseUser?.displayName.orEmpty()
            }

            if (email.isBlank()) {
                email = firebaseUser?.email.orEmpty()
            }

            if (phone.isBlank()) {
                val firebasePhone = firebaseUser?.phoneNumber.orEmpty().filter { it.isDigit() }
                if (firebasePhone.isNotBlank()) {
                    phone = firebasePhone
                }
            }
        }
    }

    // ✅ הרשאות רישום לפי whitelist
    val normalizedPhone = remember(phone) { phone.filter { it.isDigit() } }
    val normalizedEmail = remember(email) { email.trim().lowercase() }

    val isWhitelistedCoach = remember(normalizedPhone, normalizedEmail) {
        CoachWhitelist.allowedPhones.containsKey(normalizedPhone) ||
                CoachWhitelist.allowedEmails.containsKey(normalizedEmail)
    }

    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    val isSuperTester = remember(normalizedPhone, normalizedEmail, firebaseUid) {
        isSuperTesterUser(
            email = normalizedEmail,
            phoneDigits = normalizedPhone,
            firebaseUid = firebaseUid
        )
    }

    // ✅ חדש: ADMIN (Firestore: admins/{uid}.enabled)
    var isAdmin by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAdmin = runCatching { AdminAccess.isCurrentUserAdmin() }.getOrDefault(false)
    }

    // ✅ נעילה אוטומטית:
    // - אם ADMIN או Super Tester → לא נועלים כלום, מאפשרים בחירה חופשית
    // - אם מורשה מאמן → מעבירים לטאב מאמן ונועלים
    // - אם לא מורשה → מעבירים לטאב מתאמן ונועלים
    LaunchedEffect(isWhitelistedCoach, isAdmin, isSuperTester) {
        if (!isAdmin && !isSuperTester) {
            selectedTab = if (isWhitelistedCoach) 1 else 0
        }
    }

    var username by rememberSaveable {
        mutableStateOf(
            sp.getString("username", "")?.takeIf { it.isNotBlank() }
                ?: sp.getString("email", "")?.takeIf { it.isNotBlank() }
                ?: ""
        )
    }

    var password by rememberSaveable {
        mutableStateOf(
            if (isGoogleAuth) {
                "GOOGLE_AUTH"
            } else {
                sp.getString("password", "") ?: ""
            }
        )
    }

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

// ✅ סוג הסניף שנבחר בטופס: israel / abroad
    var branchType by rememberSaveable {
        mutableStateOf(sp.getString("branch_type", "israel") ?: "israel")
    }

    // ✅ חדש: סניף/קבוצה פעילים (ערך יחיד)
    var activeBranch by rememberSaveable { mutableStateOf(sp.getString("active_branch", "") ?: "") }
    var activeGroup by rememberSaveable { mutableStateOf(sp.getString("active_group", "") ?: "") }

    fun readSavedListFromPrefs(
        vararg keys: String
    ): List<String> {
        fun splitCsv(raw: String): List<String> {
            return raw
                .split(',', ';', '|', '\n')
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }

        fun readAnyPrefAsList(key: String): List<String> {
            val value = sp.all[key] ?: return emptyList()

            return when (value) {
                is String -> {
                    val raw = value.trim()

                    if (raw.isBlank()) {
                        emptyList()
                    } else if (raw.startsWith("[")) {
                        runCatching {
                            val arr = org.json.JSONArray(raw)
                            (0 until arr.length())
                                .mapNotNull { index -> arr.optString(index, null) }
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                        }.getOrDefault(emptyList())
                    } else {
                        splitCsv(raw)
                    }
                }

                is Set<*> -> {
                    value
                        .mapNotNull { it?.toString()?.trim() }
                        .filter { it.isNotBlank() }
                }

                else -> emptyList()
            }
        }

        keys.forEach { key ->
            val list = readAnyPrefAsList(key)
            if (list.isNotEmpty()) {
                return list.distinct().take(3)
            }
        }

        return emptyList()
    }

    // --- סניפים נבחרים (עד 3) — מקור אמת יחיד ---
    val selectedBranches = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        val saved = readSavedListFromPrefs(
            "branches_json",
            "selected_branches",
            "branches",
            "branch"
        )

        mutableStateListOf<String>().apply {
            addAll(saved.take(3))
        }
    }

    // קבוצות נבחרות (עד 3)
    val selectedGroups = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        val saved = readSavedListFromPrefs(
            "groups_json",
            "selected_groups",
            "groups",
            "age_groups",
            "age_group",
            "group"
        )

        mutableStateListOf<String>().apply {
            addAll(saved.take(3))
        }
    }

    // ✅ שמירה אוטומטית של activeBranch/activeGroup
    LaunchedEffect(selectedBranches.toList(), selectedGroups.toList()) {
        val branchesNow = selectedBranches.map { it.trim() }.filter { it.isNotBlank() }
        val groupsNow = selectedGroups.map { it.trim() }.filter { it.isNotBlank() }

        if (activeBranch.isBlank() || activeBranch !in branchesNow) {
            activeBranch = branchesNow.firstOrNull().orEmpty()
        }

        if (activeGroup.isBlank() || activeGroup !in groupsNow) {
            activeGroup = groupsNow.firstOrNull().orEmpty()
        }
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

    // קטלוג — קודם branches.json, ואם חסר משהו אז fallback ל־TrainingCatalog הישן
    val databaseBranches = remember(ctx) {
        KmiDatabaseProvider.branches(ctx)
    }

    val branchesByRegion: Map<String, List<String>> = remember(databaseBranches) {
        val fromDatabase = databaseBranches
            .filter { it.country == "IL" }
            .groupBy { it.regionHe }
            .mapValues { (_, branches) ->
                branches
                    .map { it.nameHe }
                    .filter { it.isNotBlank() }
                    .distinct()
            }
            .filterKeys { it.isNotBlank() }

        TrainingCatalog.branchesByRegion + fromDatabase
    }

    val groupsByBranch: Map<String, List<String>> = remember(databaseBranches) {
        val merged = TrainingCatalog.ageGroupsByBranch.toMutableMap()

        databaseBranches.forEach { branch ->
            val dbGroups = branch.trainingDays
                .map { it.groupHe }
                .filter { it.isNotBlank() }
                .distinct()

            if (dbGroups.isNotEmpty()) {
                merged[branch.nameHe] = dbGroups
            }
        }

        merged
    }

    // חגורה
    var currentBeltId by rememberSaveable { mutableStateOf(sp.getString("current_belt", "") ?: "") }

    // ==== התאמות ושיחזורים מה-SP ====
    // ✅ השחזור כבר מתבצע ב־rememberSaveable דרך readSavedListFromPrefs.
    // לא מאפסים כאן selectedGroups כדי לא לדרוס groups_json / selected_groups.

    LaunchedEffect(selectedRegion) {
        val branchesForRegion = branchesByRegion[selectedRegion].orEmpty()
        if (selectedBranch.isNotBlank() && !branchesForRegion.contains(selectedBranch)) {
            selectedBranch = ""
            selectedGroup = ""
        }
    }

    // ✅ גם מאמן בוחר דרגת חגורה.
    // לכן לא מאפסים currentBeltId במעבר לטאב מאמן.

    LaunchedEffect(branchType, selectedBranches.toList(), groupsByBranch) {
        // ✅ בחו״ל אין קבוצות גיל מתוך TrainingCatalog,
        // לכן לא מסננים את קבוצת ברירת המחדל "חו״ל".
        if (branchType == "abroad") {
            if (selectedBranches.isNotEmpty() && selectedGroups.isEmpty()) {
                selectedGroups.clear()
                selectedGroups.add("חו״ל")
            }
            return@LaunchedEffect
        }

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

        // ✅ אכיפה קשיחה רק למי שלא ADMIN ולא Super Tester
        if (!isAdmin && !isSuperTester) {
            if (isWhitelistedCoach && !isCoach) {
                Toast.makeText(
                    ctx,
                    if (isEnglish) "An authorized coach must register as a coach only" else "מאמן מורשה חייב להירשם כמאמן בלבד",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            if (!isWhitelistedCoach && isCoach) {
                Toast.makeText(
                    ctx,
                    if (isEnglish) "Coach registration is allowed only for authorized coaches" else "הרישום כמאמן מותר רק למאמנים מורשים",
                    Toast.LENGTH_LONG
                ).show()
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
        if (!isGoogleAuth && username.isBlank()) {
            usernameError = true; valid = false
        }
        if (!isGoogleAuth && password.isBlank()) {
            passwordError = true; valid = false
        }
        if (selectedRegion.isBlank()) {
            regionError = true; valid = false
        }

        val hasAtLeastOneBranch = selectedBranch.isNotBlank() || selectedBranches.isNotEmpty()
        if (!hasAtLeastOneBranch) {
            branchError = true; valid = false
        }

        // ✅ אם זה חו״ל – לא דורשים קבוצות
        if (branchType != "abroad" && selectedGroups.isEmpty()) {
            groupError = true
            valid = false
        }

        // מין חובה
        if (gender.isBlank()) {
            genderError = true; valid = false
        }

        // ✅ דרגת חגורה חובה גם למתאמן וגם למאמן
        if (currentBeltId.isBlank()) {
            Toast.makeText(
                ctx,
                if (isEnglish) "You must select a belt rank" else "חובה לבחור דרגת חגורה",
                Toast.LENGTH_LONG
            ).show()
            valid = false
        }

        if (!acceptedTerms) {
            termsError = true; valid = false
        }

        // אימות מאמן (רק אם לא ADMIN ולא Super Tester)
        if (isCoach && !isAdmin && !isSuperTester) {
            val normalizedPhoneLocal = phone.filter { it.isDigit() }
            val normalizedEmailLocal = email.trim().lowercase()

            val phoneOk = CoachWhitelist.allowedPhones.containsKey(normalizedPhoneLocal)
            val emailOk = CoachWhitelist.allowedEmails.containsKey(normalizedEmailLocal)

            if (!phoneOk && !emailOk) {
                Toast.makeText(
                    ctx,
                    if (isEnglish) "Coach registration is allowed only for authorized coaches" else "הרישום כמאמן מותר רק למאמנים מורשים",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        if (!valid) return

        // ✅ role סופי:
        // ADMIN או Super Tester בוחרים לפי הטאב, אחרת לפי whitelist
        val roleFinal = if (isAdmin || isSuperTester) {
            if (isCoach) "coach" else "trainee"
        } else {
            if (isWhitelistedCoach) "coach" else "trainee"
        }

        val roleLockedBy = when {
            isAdmin -> "admin"
            isSuperTester -> "super_tester"
            isWhitelistedCoach -> "coach_whitelist"
            else -> "trainee_default"
        }

        // ✅ מקור אמת לטלפון: ספרות בלבד.
        // חשוב במיוחד ב-Google Login, כי שאר המסכים קוראים גם phone וגם phone_number.
        val phoneFinal = phone.filter { it.isDigit() }

        val branchesListFinalForPrefs: List<String> =
            if (selectedBranches.isNotEmpty()) {
                selectedBranches.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(3)
            } else {
                selectedBranch
                    .split(',', ';', '|', '\n')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(3)
            }

        val groupsListFinalForPrefs: List<String> =
            if (selectedGroups.isNotEmpty()) {
                selectedGroups.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(3)
            } else {
                selectedGroup
                    .split(',', ';', '|', '\n')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(3)
            }

        val branchesJson = org.json.JSONArray(branchesListFinalForPrefs).toString()
        val groupsJson = org.json.JSONArray(groupsListFinalForPrefs).toString()

        val groupsCsv = groupsListFinalForPrefs.joinToString(", ")
        val primaryGroup = groupsListFinalForPrefs.firstOrNull() ?: ""

        // ✅ חגורה סופית גם למתאמן וגם למאמן.
        // אם משום מה לא נבחרה חגורה, לא נשאיר Firestore / SP עם belt ריק,
        // כי זה עלול להחזיר את המשתמש שוב למסך השלמת פרטים בכניסה הבאה.
        val beltFinal = currentBeltId.ifBlank { "white" }

        val activeBranchFinal =
            (activeBranch.takeIf { it.isNotBlank() && it in branchesListFinalForPrefs }
                ?: branchesListFinalForPrefs.firstOrNull()
                ?: "").trim()

        val activeGroupFinal =
            (activeGroup.takeIf { it.isNotBlank() && it in groupsListFinalForPrefs }
                ?: groupsListFinalForPrefs.firstOrNull()
                ?: "").trim()

        // ✅ branchesFinal תמיד עקבי מול מקור האמת הרשימתי
        val branchesFinal = branchesListFinalForPrefs.joinToString(", ")

        val completedUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val completedAt = System.currentTimeMillis()

        // שמירה ב-SP הראשי
        sp.edit()
            // ✅ ניקוי ערכים ישנים שאולי נשמרו בעבר כ־StringSet / HashSet
            .remove("groups")
            .remove("branches")
            .remove("selected_groups")
            .remove("selected_branches")

            .putString("fullName", fullName)
            .putString("phone", phoneFinal)
            .putString("phone_number", phoneFinal)
            .putString("email", email.trim())
            .putString("region", selectedRegion)

            // ✅ סניפים — שומרים גם CSV וגם JSON גם ב־sp הראשי
            .putString("branch", branchesFinal)
            .putString("branches", branchesFinal)
            .putString("branches_json", branchesJson)
            .putString("selected_branches", branchesFinal)
            .putString("active_branch", activeBranchFinal)

            // ✅ קבוצות — שומרים גם CSV וגם JSON גם ב־sp הראשי
            .putString("age_groups", groupsCsv)
            .putString("groups", groupsCsv)
            .putString("groups_json", groupsJson)
            .putString("selected_groups", groupsCsv)
            .putString("age_group", primaryGroup)
            .putString("group", primaryGroup)
            .putString("active_group", activeGroupFinal)

            .putString("username", if (isGoogleAuth) email.trim() else username)
            .putString("authProvider", if (isGoogleAuth) "google" else "local")
            .putBoolean("google_login", isGoogleAuth)
            .putString("password", if (isGoogleAuth) "" else password)
            .putBoolean("subscribeSms", subscribeSms)
            .putString("user_role", roleFinal)
            .putString("role_locked_by", roleLockedBy)
            .putString("gender", gender)
            .putString("branch_type", branchType)
            .putString("current_belt", beltFinal)
            .putString("belt_current", beltFinal)
            .putBoolean("profile_completed", true)
            .putBoolean("registration_complete", true)
            .putBoolean("registration_form_completed", true)
            .putInt("registration_schema_version", 2)
            .putString("profile_completed_uid", completedUid)
            .putLong("profile_completed_at", completedAt)
            // תאריך לידה
            .putString("birth_day", birthDay.toString())
            .putString("birth_month", birthMonth.toString())
            .putString("birth_year", birthYear.toString())
            .commit()

        // ✅ חגורה נשמרת גם למתאמן וגם למאמן.

        // userSp – אחידות
        userSp.edit().apply {
            // ✅ ניקוי ערכים ישנים שאולי נשמרו בעבר כ־StringSet / HashSet
            remove("groups")
            remove("branches")
            remove("selected_groups")
            remove("selected_branches")

            putString("fullName", fullName)
            putString("phone", phoneFinal)
            putString("phone_number", phoneFinal)
            putString("email", email.trim())
            putString("user_role", roleFinal)
            putString("role_locked_by", roleLockedBy)
            putString("region", selectedRegion)

            // ✅ סניפים — שומרים גם CSV וגם JSON
            putString("branch", branchesFinal)
            putString("branches", branchesFinal)
            putString("branches_json", branchesJson)
            putString("selected_branches", branchesFinal)
            putString("active_branch", activeBranchFinal)

            // ✅ קבוצות — שומרים גם CSV וגם JSON
            putString("age_groups", groupsCsv)
            putString("groups", groupsCsv)
            putString("groups_json", groupsJson)
            putString("selected_groups", groupsCsv)
            putString("age_group", primaryGroup)
            putString("group", primaryGroup)
            putString("active_group", activeGroupFinal)
            putString("birth_day", birthDay.toString())
            putString("birth_month", birthMonth.toString())
            putString("birth_year", birthYear.toString())
            putString("gender", gender)
            putString("branch_type", branchType)
            putBoolean("profile_completed", true)
            putBoolean("registration_complete", true)
            putBoolean("registration_form_completed", true)
            putInt("registration_schema_version", 2)
            putString("profile_completed_uid", completedUid)
            putLong("profile_completed_at", completedAt)

            // ✅ גם מתאמן וגם מאמן שומרים דרגת חגורה
            putString("current_belt", beltFinal)
            putString("belt_current", beltFinal)

            commit()
        }

        // Persist – KMP
        kmiPrefs.fullName = fullName
        kmiPrefs.phone = phoneFinal
        kmiPrefs.email = email.trim()
        kmiPrefs.region = selectedRegion
        kmiPrefs.branch = branchesFinal
        kmiPrefs.ageGroup = primaryGroup
        kmiPrefs.username = if (isGoogleAuth) email.trim() else username
        kmiPrefs.password = if (isGoogleAuth) "" else password

        fun persistRegistrationToFirestore(finalUid: String) {
            if (finalUid.isBlank()) {
                Toast.makeText(
                    ctx,
                    if (isEnglish) "User identification failed. Please try again." else "שגיאה בזיהוי המשתמש. נסה שוב.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val birthDate = "%04d-%02d-%02d".format(birthYear, birthMonth, birthDay)

            val branchesListFinal = branchesListFinalForPrefs
            val groupsListFinal = groupsListFinalForPrefs

            val firestoreData = hashMapOf(
                "uid" to finalUid,
                "role" to roleFinal,
                "roleLockedBy" to roleLockedBy,
                "fullName" to fullName,
                "phone" to phoneFinal,
                "phoneNumber" to phoneFinal,
                "phoneRaw" to phone,
                "email" to email.trim(),
                "emailLower" to email.trim().lowercase(),
                "authProvider" to if (isGoogleAuth) "google" else "local",
                "region" to selectedRegion,

                "branches" to branchesListFinal,
                "branchesCsv" to branchesFinal,
                "activeBranch" to activeBranchFinal,
                "branch" to activeBranchFinal,

                "groups" to groupsListFinal,
                "groupsCsv" to groupsCsv,
                "primaryGroup" to primaryGroup,
                "activeGroup" to activeGroupFinal,
                "group" to activeGroupFinal,
                "age_group" to activeGroupFinal,

                "birthDate" to birthDate,
                "gender" to gender,
                "belt" to beltFinal,
                "currentBelt" to beltFinal,

                "profileCompleted" to true,
                "registrationComplete" to true,
                "profileCompletedAt" to System.currentTimeMillis(),

                "registrationFormCompleted" to true,
                "registrationSchemaVersion" to 2,
                "registrationCompletedBy" to "registration_form_v2",

                "subscribeSms" to subscribeSms,
                "isActive" to true,
                "archived" to false,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(finalUid)
                .set(firestoreData, SetOptions.merge())
                .addOnSuccessListener {
                    sp.edit()
                        .putString("uid", finalUid)
                        .putString("profile_completed_uid", finalUid)
                        .apply()

                    userSp.edit()
                        .putString("uid", finalUid)
                        .putString("profile_completed_uid", finalUid)
                        .apply()

                    FcmTokenManager.refreshTokenForUserDocId(finalUid)

                    Toast.makeText(
                        ctx,
                        if (isEnglish) "Registration saved successfully ✅" else "הרישום נשמר בהצלחה ✅",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (roleFinal == "coach") {
                        val code = "%06d".format(java.security.SecureRandom().nextInt(1_000_000))
                        coachCode = code
                        sp.edit().putString("coach_code", code).apply()
                        userSp.edit().putString("coach_code", code).apply()
                        showCodeDialog = true
                    } else {
                        finishRegistrationFlow()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        ctx,
                        if (isEnglish) "Saving registration failed" else "שמירת הרישום נכשלה",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        val auth = FirebaseAuth.getInstance()
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (!startAtProfile && !isGoogleAuth) {
            auth.signOut()

            auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
                .addOnSuccessListener { result ->
                    val newUid = result.user?.uid.orEmpty()
                    persistRegistrationToFirestore(newUid)
                }
                .addOnFailureListener { e ->
                    if (e is FirebaseAuthUserCollisionException) {
                        auth.signInWithEmailAndPassword(cleanEmail, cleanPassword)
                            .addOnSuccessListener { result ->
                                val existingUid = result.user?.uid.orEmpty()
                                persistRegistrationToFirestore(existingUid)
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    ctx,
                                    if (isEnglish) {
                                        "The email already exists, but the password does not match"
                                    } else {
                                        "המייל כבר קיים אך הסיסמה אינה תואמת"
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            ctx,
                            if (isEnglish) {
                                "Creating a new user failed. Please try again."
                            } else {
                                "יצירת משתמש חדש נכשלה. נסה שוב."
                            },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        persistRegistrationToFirestore(uid)
    }

    // ====== UI ======
    Scaffold(
        topBar = {
            if (startAtProfile) {
                // ✅ עריכת פרופיל מתוך האפליקציה:
                // המשתמש כבר מזוהה, לכן האייקונים נשארים פעילים כרגיל.
                il.kmi.app.ui.KmiTopBar(
                    title = if (isEnglish) "Edit Profile" else "עריכת פרופיל",
                    showRoleStatus = false,
                    onOpenDrawer = onOpenDrawer,
                    onHome = {
                        il.kmi.app.ui.DrawerBridge.openHome()
                    },
                    lockSearch = true,
                    lockHome = false,
                    showTopHome = false,
                    showBottomActions = true
                )
            } else {
                // ✅ רישום ראשוני:
                // לפני הזדהות מלאה — האייקונים צבעוניים אבל חסומים.
                RegistrationFormLockedTopBar(
                    title = if (isEnglish) "Registration Form" else "טופס רישום",
                    isEnglish = isEnglish,
                    onLockedAction = { actionName ->
                        Toast.makeText(
                            ctx,
                            if (isEnglish) {
                                "$actionName will be available after sign in or registration"
                            } else {
                                "$actionName יהיה זמין לאחר כניסה / רישום"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(headerBrush)
                .padding(padding)
        ) {
            Spacer(Modifier.height(8.dp))

            // טאבים חיצוניים
            RegistrationTabsBilingual(
                selectedTab = selectedTab,
                isEnglish = isEnglish,
                onTabSelected = { newTab ->
                    // ✅ ADMIN או Super Tester יכולים לבחור חופשי
                    if (isAdmin || isSuperTester) {
                        selectedTab = newTab
                        return@RegistrationTabsBilingual
                    }

                    when {
                        // ✅ מאמן מורשה: מותר רק טאב מאמן
                        isWhitelistedCoach -> {
                            if (newTab != 1) {
                                Toast.makeText(
                                    ctx,
                                    if (isEnglish) "An authorized coach must register as a coach" else "מאמן מורשה נרשם רק כמאמן",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            selectedTab = 1
                        }

                        // ✅ לא מורשה: מותר רק טאב מתאמן
                        else -> {
                            if (newTab == 1) {
                                Toast.makeText(
                                    ctx,
                                    if (isEnglish) "Coach registration is allowed only for authorized coaches" else "הרישום כמאמן מותר רק למאמנים מורשים",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                isEnglish = isEnglish,
                isGoogleAuth = isGoogleAuth,
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
                    val clean = list
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(3)

                    selectedBranches.clear()
                    selectedBranches.addAll(clean)

                    // תאימות ישנה
                    selectedBranch = clean.joinToString(", ")

                    if (activeBranch.isBlank() || activeBranch !in clean) {
                        activeBranch = clean.firstOrNull().orEmpty()
                    }

                    branchError = clean.isEmpty()
                },
                selectedGroups = selectedGroups,
                onGroupsChange = { list ->
                    val clean = list
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(3)

                    selectedGroups.clear()
                    selectedGroups.addAll(clean)

                    selectedGroup = clean.joinToString(", ")

                    if (activeGroup.isBlank() || activeGroup !in clean) {
                        activeGroup = clean.firstOrNull().orEmpty()
                    }

                    groupError = clean.isEmpty()
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
                onOpenTerms = onOpenTerms,
                branchType = branchType,
                onBranchTypeChange = { newType ->
                    branchType = newType
                    sp.edit().putString("branch_type", newType).apply()
                },
                submitButtonText = if (startAtProfile) {
                    if (isEnglish) "Save profile" else "שמירת פרופיל"
                } else {
                    if (isEnglish) "Complete registration" else "סיום רישום"
                },
                onSubmitRegistration = {
                    submitRegistration()
                }
            )

            if (!acceptedTerms && termsError) {
                Text(
                    text = if (isEnglish) {
                        "You must accept the Terms of Use and Privacy Policy"
                    } else {
                        "חובה לאשר תנאי שימוש ומדיניות פרטיות"
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }

    // --- דיאלוג קוד מאמן (אם רלוונטי) ---
    if (showCodeDialog) {
        val normalizedPhone = phone.filter { it.isDigit() }
        val nameFromPhone = CoachWhitelist.allowedPhones[normalizedPhone]
        val nameFromEmail = CoachWhitelist.allowedEmails[email.trim()]
        val coachDisplayName = nameFromPhone ?: nameFromEmail ?: fullName.ifBlank {
            if (isEnglish) "Coach" else "מאמן"
        }

        AlertDialog(
            onDismissRequest = { /* לא נסגור לבד */ },
            confirmButton = {
                Button(
                    onClick = {
                        showCodeDialog = false
                        finishRegistrationFlow()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (isEnglish) "OK" else "אישור")
                }
            },
            title = {
                Text(
                    text = if (isEnglish) "Hello, $coachDisplayName" else "שלום, $coachDisplayName",
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                ) {
                    Text(
                        text = if (isEnglish) "Your coach code:" else "קוד המאמן שלך:",
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        text = if (isEnglish) {
                            "Please save your coach code for system access and advanced actions, such as sending messages to a group."
                        } else {
                            "עליך לשמור את קוד המאמן שהתקבל לכניסה למערכת ולפעולות מתקדמות, כמו שליחת הודעות לקבוצה."
                        },
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

@Composable
private fun RegistrationTabsBilingual(
    selectedTab: Int,
    isEnglish: Boolean,
    onTabSelected: (Int) -> Unit
) {
    val traineeLabel = if (isEnglish) "Trainee" else "מתאמן"
    val coachLabel = if (isEnglish) "Coach" else "מאמן"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(0.dp),
        color = Color(0xFF6D4FE8).copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        // ✅ תמיד: מתאמן בצד ימין, מאמן בצד שמאל
        // נשאר תואם גם באנגלית: Trainee מימין, Coach משמאל.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RegistrationRoleTabButton(
                    text = traineeLabel,
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.45f))
                )

                RegistrationRoleTabButton(
                    text = coachLabel,
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RegistrationRoleTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .background(
                if (selected) {
                    Color.White.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.72f)
                    .height(3.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                    )
            )
        }
    }
}