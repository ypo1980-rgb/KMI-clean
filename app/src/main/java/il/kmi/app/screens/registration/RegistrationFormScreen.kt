@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package il.kmi.app.screens.registration

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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

private const val REGISTRATION_LOG = "KMI_REGISTRATION"

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

    val isGoogleAuth = remember(sp) {
        sp.getString("authProvider", "") == "google" ||
                sp.getBoolean("google_login", false) ||
                sp.getBoolean("skip_otp", false)
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

        Log.e(
            REGISTRATION_LOG,
            "role tabs gate isAdmin=$isAdmin isSuperTester=$isSuperTester isWhitelistedCoach=$isWhitelistedCoach selectedTab=$selectedTab"
        )
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

    // קטלוג
    val branchesByRegion = TrainingCatalog.branchesByRegion
    val groupsByBranch = TrainingCatalog.ageGroupsByBranch

    // חגורה
    var currentBeltId by rememberSaveable { mutableStateOf(sp.getString("current_belt", "") ?: "") }

    // ==== התאמות ושיחזורים מה-SP ====
    // ✅ השחזור כבר מתבצע ב־rememberSaveable דרך readSavedListFromPrefs.
    // לא מאפסים כאן selectedGroups כדי לא לדרוס groups_json / selected_groups.
    LaunchedEffect(Unit) {
        Log.e(
            REGISTRATION_LOG,
            "initial selectedBranches=${selectedBranches.toList()} selectedGroups=${selectedGroups.toList()}"
        )
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
                Toast.makeText(ctx, "הרישום כמאמן מותר רק למאמנים מורשים", Toast.LENGTH_LONG).show()
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

        // ✅ חגורה סופית למתאמן.
        // אם משום מה לא נבחרה חגורה, לא נשאיר Firestore / SP עם belt ריק,
        // כי זה עלול להחזיר את המשתמש שוב למסך השלמת פרטים בכניסה הבאה.
        val beltFinal =
            if (roleFinal == "trainee") currentBeltId.ifBlank { "white" } else ""

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

        Log.e(
            REGISTRATION_LOG,
            "saved MAIN profile_completed=true uid=$completedUid role=$roleFinal belt=$beltFinal branch=$branchesFinal group=$primaryGroup"
        )

        // חגורה – רק למתאמן
        if (roleFinal != "trainee") {
            sp.edit()
                .remove("current_belt")
                .remove("belt_current")
                .commit()
        }

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

            if (roleFinal == "trainee") {
                putString("current_belt", beltFinal)
                putString("belt_current", beltFinal)
            } else {
                remove("current_belt")
                remove("belt_current")
            }

            commit()
        }

        Log.e(
            REGISTRATION_LOG,
            "saved USER profile_completed=true uid=$completedUid role=$roleFinal belt=$beltFinal"
        )

        // Persist – KMP
        kmiPrefs.fullName = fullName
        kmiPrefs.phone = phoneFinal
        kmiPrefs.email = email.trim()
        kmiPrefs.region = selectedRegion
        kmiPrefs.branch = branchesFinal
        kmiPrefs.ageGroup = primaryGroup
        kmiPrefs.username = if (isGoogleAuth) email.trim() else username
        kmiPrefs.password = if (isGoogleAuth) "" else password

        // --- שמירה ל-Firestore: מאגר המשתמשים המרכזי ---
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            // תאריך לידה בפורמט YYYY-MM-DD
            val birthDate = "%04d-%02d-%02d".format(birthYear, birthMonth, birthDay)

            // ✅ משתמשים באותה רשימה שכבר חושבה ונשמרה ל־SharedPreferences
            val branchesListFinal = branchesListFinalForPrefs
            val groupsListFinal = groupsListFinalForPrefs

            val firestoreData = hashMapOf(
                "uid" to uid,
                "role" to roleFinal,
                "fullName" to fullName,
                "phone" to phoneFinal,
                "phoneNumber" to phoneFinal,
                "phoneRaw" to phone,
                "email" to email.trim(),
                "authProvider" to if (isGoogleAuth) "google" else "local",
                "region" to selectedRegion,

                // ✅ סניפים — רשימה + CSV + פעיל
                "branches" to branchesListFinal,
                "branchesCsv" to branchesFinal,
                "activeBranch" to activeBranchFinal,

                // ✅ קבוצות — רשימה + CSV + פעיל
                "groups" to groupsListFinal,
                "groupsCsv" to groupsCsv,
                "primaryGroup" to primaryGroup,
                "activeGroup" to activeGroupFinal,

                "birthDate" to birthDate,
                "gender" to gender,
                "belt" to beltFinal,
                "currentBelt" to beltFinal,

// ✅ דגלים ישנים — נשארים לתאימות
                "profileCompleted" to true,
                "registrationComplete" to true,
                "profileCompletedAt" to System.currentTimeMillis(),

// ✅ דגל חדש וקשיח: רק מי שסיים את טופס הרישום החדש יקבל אותו
                "registrationFormCompleted" to true,
                "registrationSchemaVersion" to 2,
                "registrationCompletedBy" to "registration_form_v2",

                "subscribeSms" to subscribeSms,
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(firestoreData, SetOptions.merge())

            Log.e(
                REGISTRATION_LOG,
                "firestore set users/$uid profileCompleted=true registrationComplete=true " +
                        "registrationFormCompleted=true schema=2 role=$roleFinal belt=$beltFinal phoneLen=${phoneFinal.length}"
            )
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
            RegistrationTabs(
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    // ✅ ADMIN או Super Tester יכולים לבחור חופשי
                    if (isAdmin || isSuperTester) {
                        selectedTab = newTab
                        Log.e(
                            REGISTRATION_LOG,
                            "role tab manually selected by elevated user newTab=$newTab isAdmin=$isAdmin isSuperTester=$isSuperTester"
                        )
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
                onSubmitRegistration = {
                    submitRegistration()
                }
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

