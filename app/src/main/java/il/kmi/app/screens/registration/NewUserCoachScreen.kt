@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package il.kmi.app.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import il.kmi.app.KmiViewModel
import il.kmi.app.screens.registration.RegionBranchGroupPicker
import il.kmi.shared.prefs.KmiPrefs
import java.security.SecureRandom
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import il.kmi.app.screens.registration.CoachWhitelist

/**
 * "משתמש חדש – מאמן"
 * 1) אימות (SMS/Email) דרך RegistrationAuth
 * 2) בחירת אזור/סניף/קבוצה (אותו רכיב כמו אצל מתאמן)
 * 3) סיום רישום ושמירה ל-SP + kmi_user + KmiPrefs
 */
@Composable
fun NewUserCoachScreen(
    onBack: () -> Unit,
    onRegistrationComplete: () -> Unit,
    onOpenLegal: () -> Unit,
    onOpenTerms: () -> Unit = onOpenLegal,
    vm: KmiViewModel,
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    skipOtp: Boolean
) {

    // ---- State שמופיע פעם אחת בלבד ----
    var showCodeDialog by rememberSaveable { mutableStateOf(false) }
    var coachCode     by rememberSaveable { mutableStateOf("") }

    // אם הגיע skipOtp=true (מהניווט אחרי האימות) – נתחיל כמאומתים
    var isLoginVerified by rememberSaveable { mutableStateOf(skipOtp) }   // ⬅️ תיקון

    BackHandler {
        if (!showCodeDialog) onBack()
    }

    val ctx    = LocalContext.current
    val userSp = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }

    // --- UI/Theme ---
    val scroll = rememberScrollState()
    val fieldHeight = 52.dp
    val backgroundBrush = remember {
        Brush.linearGradient(
            listOf(Color(0xFFFF6EA7), Color(0xFFFF8EC7), Color(0xFFFFC4DD)),
            start = Offset(0f, 0f), end = Offset(1000f, 3000f)
        )
    }

    // --- State לבחירות המאמן (כמו אצל מתאמן) ---
    var selectedRegion by rememberSaveable { mutableStateOf(userSp.getString("region", "") ?: "") }
    var selectedBranch by rememberSaveable { mutableStateOf(userSp.getString("branch", "") ?: "") }
    var selectedGroup  by rememberSaveable {
        mutableStateOf(
            userSp.getString("age_group", null)?.takeIf { it.isNotBlank() }
                ?: userSp.getString("group", "").orEmpty()
        )
    }

    // Errors
    var regionError by remember { mutableStateOf(false) }
    var branchError by remember { mutableStateOf(false) }
    var groupError  by remember { mutableStateOf(false) }

    // קטלוגים
    val branchesByRegion = il.kmi.app.training.TrainingCatalog.branchesByRegion
    val groupsByBranch   = il.kmi.app.training.TrainingCatalog.ageGroupsByBranch

    // נגזרות + שמירת עקביות
    val branchesForRegion by remember(selectedRegion) {
        mutableStateOf(branchesByRegion[selectedRegion].orEmpty())
    }
    val groupsForBranch by remember(selectedBranch) {
        mutableStateOf(groupsByBranch[selectedBranch].orEmpty())
    }
    LaunchedEffect(selectedRegion) {
        if (selectedBranch.isNotBlank() && !branchesForRegion.contains(selectedBranch)) {
            selectedBranch = ""; selectedGroup = ""
        }
    }
    LaunchedEffect(selectedBranch) {
        if (selectedGroup.isNotBlank() && !groupsForBranch.contains(selectedGroup)) {
            selectedGroup = ""
        }
    }

    // מחולל קוד מאמן
    fun generateCoachCode(): String {
        val n = SecureRandom().nextInt(1_000_000)
        return "%06d".format(n)
    }

    Scaffold(
        topBar = {
            il.kmi.app.ui.KmiTopBar(
                title = "רישום מאמן",
                showRoleStatus = false,
                showBottomActions = true,
                onOpenDrawer = {},
                lockSearch = true,
                showCoachBroadcastFab = true
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            // ===== סרגל תחתון דביק לכפתור סיום =====
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Button(
                    onClick = {
                        var valid = true
                        if (selectedRegion.isBlank()) { regionError = true; valid = false }
                        if (selectedBranch.isBlank()) { branchError = true; valid = false }
                        if (selectedGroup.isBlank())  { groupError  = true; valid = false }
                        if (!valid) return@Button

                        fun toCatalogGroup(raw: String): String {
                            val r = raw.trim()
                            return if (r.contains("נוער") && r.contains("בוגר")) "נוער + בוגרים" else r
                        }
                        val groupCatalog = toCatalogGroup(selectedGroup)

                        val coachFromCatalog = il.kmi.app.training.TrainingDirectory
                            .getSchedule(selectedBranch, groupCatalog)
                            ?.coachName.orEmpty()

                        sp.edit()
                            .putString("user_role", "coach")
                            .putString("region", selectedRegion)
                            .putString("branch", selectedBranch)
                            .putString("age_group", groupCatalog)
                            .putString("group",     groupCatalog)
                            .apply()

                        userSp.edit()
                            .putString("user_role", "coach")
                            .putString("region", selectedRegion)
                            .putString("branch", selectedBranch)
                            .putString("age_group", groupCatalog)
                            .putString("group",     groupCatalog)
                            .putString("coach_name", coachFromCatalog)
                            .apply()

                        kmiPrefs.region   = selectedRegion
                        kmiPrefs.branch   = selectedBranch
                        kmiPrefs.ageGroup = groupCatalog

                        // --- שמירה ל-Firestore: מאגר משתמשים עבור מאמן ---
                        val auth = FirebaseAuth.getInstance()
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            val fullName = sp.getString("fullName", "").orEmpty()
                            val phone    = sp.getString("phone", "").orEmpty()
                            val email    = sp.getString("email", "").orEmpty()

                            val firestoreData = hashMapOf(
                                "uid" to uid,
                                "role" to "coach",
                                "fullName" to fullName,
                                "phone" to phone,
                                "email" to email,
                                "region" to selectedRegion,
                                "branches" to listOf(selectedBranch),
                                "branchesCsv" to selectedBranch,
                                "groups" to listOf(groupCatalog),
                                "primaryGroup" to groupCatalog,
                                "coachName" to coachFromCatalog,
                                "isActive" to true,
                                "createdAt" to System.currentTimeMillis(),
                                "updatedAt" to System.currentTimeMillis()
                            )

                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(firestoreData, SetOptions.merge())
                        }

                        coachCode = generateCoachCode()
                        sp.edit().putString("coach_code", coachCode).apply()
                        userSp.edit().putString("coach_code", coachCode).apply()
                        showCodeDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("סיום רישום")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ---------- בחירת אזור/סניף/קבוצה ----------
                RegionBranchGroupPicker(
                    selectedRegion = selectedRegion,
                    selectedBranch = selectedBranch,
                    selectedGroup  = selectedGroup,
                    onRegionChange = { region ->
                        selectedRegion = region
                        selectedBranch = ""; selectedGroup = ""
                        regionError = false
                    },
                    onBranchChange = { branch ->
                        selectedBranch = branch
                        selectedGroup  = ""
                        branchError = false
                    },
                    onGroupChange = { group ->
                        selectedGroup = group
                        groupError = false
                    },
                    regionError = regionError,
                    branchError = branchError,
                    groupError  = groupError,
                    branchesByRegion = branchesByRegion,
                    groupsByBranch   = groupsByBranch,
                    fieldWidth = 0.98f,
                    fieldHeight = fieldHeight
                )

                Spacer(Modifier.height(16.dp))
                // אין צורך ב-Spacer ענק — ה-bottomBar דואג לחפיפה
            }

            // ---------- דיאלוג קוד מאמן ----------
            if (showCodeDialog) {
                val clipboard = LocalClipboardManager.current

                // === הפקת שם המאמן להצגה בדיאלוג ===
                val phoneForName = sp.getString("phone", "").orEmpty()
                val emailForName = sp.getString("email", "").orEmpty()
                val fullNameForName = sp.getString("fullName", "").orEmpty()

                val normalizedPhone = phoneForName.filter { it.isDigit() }
                val nameFromPhone = CoachWhitelist.allowedPhones[normalizedPhone]
                val nameFromEmail = CoachWhitelist.allowedEmails[emailForName.trim()]
                val coachDisplayName = nameFromPhone ?: nameFromEmail ?: fullNameForName.ifBlank { "מאמן" }

                AlertDialog(
                    onDismissRequest = { /* אל תאפשר סגירה בטעות */ },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCodeDialog = false
                                onRegistrationComplete()
                            }
                        ) { Text("אישור") }
                    },
                    title = { Text("שלום, $coachDisplayName") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "קוד המאמן שלך:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 1.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = coachCode,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    TextButton(
                                        onClick = { clipboard.setText(AnnotatedString(coachCode)) }
                                    ) { Text("העתקה") }
                                }
                            }
                            Text(
                                text = "עליך לשמור את קוד המאמן שהתקבל לכניסה למערכת ולפעולות מתקדמות (כמו שליחת הודעות לקבוצה).",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                )
            }
        }
    }
}
