@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.coach

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.attendance.data.AttendanceStatus
import il.kmi.app.attendance.data.GroupMember
import il.kmi.app.privacy.DemoPrivacy
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.app.screens.registration.CoachBranchAssignment
import il.kmi.app.screens.registration.CoachBranchAssignmentsCodec
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.yuval.ui.theme.kmiSectionHeaderBrush
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale


//=========================================================================

@Composable
private fun CoachTraineesPremiumLoading(
    text: String,
    modifier: Modifier = Modifier
) {
    val transition =
        rememberInfiniteTransition(
            label = "coachTraineesLoading"
        )

    val outerRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1_250,
                        easing = FastOutSlowInEasing
                    )
            ),
        label = "coachTraineesOuterRing"
    )

    val innerRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1_750,
                        easing = FastOutSlowInEasing
                    )
            ),
        label = "coachTraineesInnerRing"
    )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(58.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier =
                    Modifier
                        .size(58.dp)
                        .graphicsLayer {
                            rotationZ = outerRotation
                        }
                        .border(
                            width = 4.dp,
                            brush =
                                Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFFA855F7),
                                        Color(0xFF38BDF8),
                                        Color.Transparent
                                    )
                                ),
                            shape = CircleShape
                        )
            )

            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            rotationZ = innerRotation
                        }
                        .border(
                            width = 3.dp,
                            brush =
                                Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFFF59E0B),
                                        Color(0xFF22C55E),
                                        Color.Transparent
                                    )
                                ),
                            shape = CircleShape
                        )
            )

            Box(
                modifier =
                    Modifier
                        .size(14.dp)
                        .background(
                            color =
                                MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color =
                                Color(0xFFA78BFA)
                                    .copy(alpha = 0.55f),
                            shape = CircleShape
                        )
            )
        }

        Text(
            text = text,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            style =
                KmiTypography.secondary.copy(
                    fontWeight = FontWeight.Bold
                ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

//=========================================================================

@Composable
private fun CoachTraineesTopTabs(
    isEnglish: Boolean,
    selectionSelected: Boolean,
    onListClick: () -> Unit,
    onSelectionClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RectangleShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = kmiSectionHeaderBrush()
                )
        ) {

            // קו מפריד במרכז
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(1.dp)
                    .height(28.dp)
                    .background(
                        Color.White.copy(alpha = 0.65f)
                    )
            )

            // מקבעים מיקום פיזי:
            // שמאל = רשימת מתאמנים
            // ימין = בחירת מתאמן
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // שמאל — רשימת מתאמנים
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                onListClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                coachTr(
                                    isEnglish,
                                    "רשימת\nמתאמנים",
                                    "Trainees\nlist"
                                ),
                            style = KmiTypography.caption.copy(
                                fontWeight =
                                    if (!selectionSelected) {
                                        FontWeight.ExtraBold
                                    } else {
                                        FontWeight.Bold
                                    }
                            ),
                            color =
                                if (!selectionSelected) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.90f)
                                },
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )

                        if (!selectionSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .width(88.dp)
                                    .height(3.dp)
                                    .background(Color.White)
                            )
                        }
                    }

                    // ימין — בחירת מתאמן
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                onSelectionClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                coachTr(
                                    isEnglish,
                                    "בחירת\nמתאמן",
                                    "Select\ntrainee"
                                ),
                            style = KmiTypography.caption.copy(
                                fontWeight =
                                    if (selectionSelected) {
                                        FontWeight.ExtraBold
                                    } else {
                                        FontWeight.Bold
                                    }
                            ),
                            color =
                                if (selectionSelected) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.90f)
                                },
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )

                        if (selectionSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .width(88.dp)
                                    .height(3.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoachTraineesScreen(
    branch: String = "",
    groupKey: String = "",
    onBack: () -> Unit = {},
    onOpenDrawer: () -> Unit = { il.kmi.app.ui.DrawerBridge.open() },
    onOpenHome: () -> Unit = onBack
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    val sp = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
    val role = sp.getString("user_role", "trainee").orEmpty()

    val isCoachRole = remember(role) {
        val cleanRole = role.trim().lowercase()
        cleanRole == "coach" ||
                cleanRole.contains("coach") ||
                cleanRole.contains("trainer") ||
                cleanRole.contains("instructor") ||
                cleanRole.contains("מאמן") ||
                cleanRole.contains("מדריך")
    }

    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val isEnglish =
        langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val screenTextAlign = coachTextAlign(isEnglish)
    val screenHorizontalAlignment =
        coachHorizontalAlignment(isEnglish)

    // הסטטיסטיקה הארצית נפתחת כעת מתוך מסך סטטיסטיקת הקבוצה.

    // Trigger לשיתוף מהסרגל העליון.
    // המסך הפנימי יחליט אם לשתף סטטיסטיקת קבוצה או סטטיסטיקה ארצית.
    var statisticsShareTrigger by remember {
        mutableIntStateOf(0)
    }

    // --- סניף / קבוצה שנעשה בהם שימוש בפועל ---
    var effectiveBranch by remember {
        mutableStateOf(branch.trim())
    }

    var effectiveGroupKey by remember {
        mutableStateOf(groupKey.trim())
    }

    // כל הסניפים והקבוצות שהמאמן רשום אליהם.
    var availableBranches by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    /*
     * הרשימה המלאה של קבוצות המאמן נשמרת בנפרד.
     * availableGroups תכיל רק קבוצות השייכות
     * לסניף שנבחר כרגע.
     */
    /*
     * מקור האמת החדש:
     * סניף והקבוצות שאליהן המאמן משויך בו.
     */
    var coachGroupsByBranch by remember {
        mutableStateOf<Map<String, List<String>>>(
            emptyMap()
        )
    }

    /*
     * רשימה שטוחה נשמרת רק עבור fallback
     * למשתמשים במבנה הישן.
     */
    var allCoachGroups by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var availableGroups by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    /*
     * קורא רשימה מ-SharedPreferences.
     *
     * תומך:
     * - String רגיל
     * - CSV
     * - JSON Array
     * - StringSet
     */
    fun readCoachPrefList(
        vararg keys: String
    ): List<String> {
        return keys
            .flatMap { key ->
                when (val raw = sp.all[key]) {
                    is Set<*> ->
                        raw.mapNotNull {
                            it?.toString()
                        }

                    is String -> {
                        val value = raw.trim()

                        if (value.isBlank()) {
                            emptyList()
                        } else if (
                            value.startsWith("[") &&
                            value.endsWith("]")
                        ) {
                            runCatching {
                                val json =
                                    org.json.JSONArray(value)

                                buildList {
                                    for (i in 0 until json.length()) {
                                        json.optString(i)
                                            .trim()
                                            .takeIf {
                                                it.isNotBlank()
                                            }
                                            ?.let(::add)
                                    }
                                }
                            }.getOrElse {
                                value.split(",")
                            }
                        } else {
                            value.split(",")
                        }
                    }

                    else ->
                        emptyList()
                }
            }
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
    }

    /*
     * טוענים תמיד את כל הסניפים והקבוצות של המאמן.
     *
     * לא מסתפקים ב-activeBranch / activeGroup,
     * כי המסך צריך לאפשר מעבר בין כל השיוכים.
     */
    LaunchedEffect(Unit) {

        /*
         * המבנה החדש שנשמר מקומית.
         */
        val localAssignments =
            CoachBranchAssignmentsCodec.decode(
                sp.getString(
                    "coach_branch_assignments_json",
                    ""
                )
            )

        val localBranches =
            (
                    readCoachPrefList(
                        "branches_json",
                        "branches",
                        "selected_branches",
                        "branchesCsv"
                    ) +
                            readCoachPrefList(
                                "active_branch",
                                "activeBranch",
                                "branch",
                                "coach_branch",
                                "selected_branch",
                                "current_branch"
                            )
                    )
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        val localGroups =
            (
                    readCoachPrefList(
                        "groups_json",
                        "groups",
                        "selected_groups",
                        "groupsCsv"
                    ) +
                            readCoachPrefList(
                                "active_group",
                                "activeGroup",
                                "primaryGroup",
                                "groupKey",
                                "group_key",
                                "age_group",
                                "group",
                                "coach_groupKey",
                                "selected_groupKey",
                                "current_groupKey"
                            )
                    )
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        var serverBranches =
            emptyList<String>()

        var serverGroups =
            emptyList<String>()

        var serverAssignments =
            emptyList<CoachBranchAssignment>()

        val uid =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid

        if (!uid.isNullOrBlank()) {
            try {
                val snap =
                    Firebase.firestore
                        .collection("users")
                        .document(uid)
                        .get()
                        .await()

                /*
 * קוראים את המבנה החדש מ-Firestore.
 */
                serverAssignments =
                    (
                            snap.get(
                                "coachBranchAssignments"
                            ) as? List<*>
                            )
                        ?.mapNotNull { rawAssignment ->
                            val assignmentMap =
                                rawAssignment as?
                                        Map<*, *>
                                    ?: return@mapNotNull null

                            val assignmentBranch =
                                assignmentMap["branch"]
                                    ?.toString()
                                    ?.trim()
                                    .orEmpty()

                            if (
                                assignmentBranch.isBlank()
                            ) {
                                return@mapNotNull null
                            }

                            val assignmentGroups =
                                (
                                        assignmentMap["groups"]
                                                as? List<*>
                                        )
                                    ?.mapNotNull { rawGroup ->
                                        rawGroup
                                            ?.toString()
                                            ?.trim()
                                            ?.takeIf { group ->
                                                group.isNotBlank()
                                            }
                                    }
                                    ?.distinct()
                                    .orEmpty()

                            CoachBranchAssignment(
                                branch =
                                    assignmentBranch,
                                groups =
                                    assignmentGroups
                            ).sanitized()
                        }
                        ?.distinctBy { assignment ->
                            assignment.branch
                        }
                        .orEmpty()

                serverBranches =
                    buildList {
                        // רשימת סניפים חדשה
                        (snap.get("branches") as? List<*>)
                            ?.forEach { item ->
                                item
                                    ?.toString()
                                    ?.trim()
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let(::add)
                            }

                        // CSV
                        snap.getString("branchesCsv")
                            ?.split(",")
                            ?.forEach { value ->
                                value
                                    .trim()
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let(::add)
                            }

                        // תאימות לשדות יחידים ישנים
                        listOf(
                            snap.getString("activeBranch"),
                            snap.getString("active_branch"),
                            snap.getString("branch"),
                            snap.getString("coachBranch")
                        )
                            .mapNotNull { it }
                            .forEach { value ->
                                value
                                    .trim()
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let(::add)
                            }
                    }
                        .distinct()

                serverGroups =
                    buildList {
                        // רשימת קבוצות חדשה
                        (snap.get("groups") as? List<*>)
                            ?.forEach { item ->
                                item
                                    ?.toString()
                                    ?.trim()
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let(::add)
                            }

                        // CSV אם קיים
                        snap.getString("groupsCsv")
                            ?.split(",")
                            ?.forEach { value ->
                                value
                                    .trim()
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let(::add)
                            }

                        // תאימות לשדות הישנים
                        listOf(
                            snap.getString("activeGroup"),
                            snap.getString("active_group"),
                            snap.getString("primaryGroup"),
                            snap.getString("groupKey"),
                            snap.getString("group_key"),
                            snap.getString("age_group"),
                            snap.getString("group"),
                            snap.getString("coachGroupKey")
                        )
                            .mapNotNull { it }
                            .forEach { value ->
                                value
                                    .trim()
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let(::add)
                            }
                    }
                        .distinct()

            } catch (_: Exception) {
                // נשארים עם הנתונים המקומיים.
            }
        }

        /*
         * נתוני השרת קודמים לנתונים המקומיים.
         * אם השדה החדש אינו קיים, משתמשים
         * במבנה המקומי החדש.
         */
        val resolvedAssignments =
            serverAssignments.ifEmpty {
                localAssignments
            }

        coachGroupsByBranch =
            resolvedAssignments
                .associate { assignment ->
                    assignment.branch to
                            assignment.groups
                }

        availableBranches =
            if (coachGroupsByBranch.isNotEmpty()) {
                /*
                 * במבנה החדש רק הסניפים שיש להם
                 * שיוך מפורש משתתפים במסך.
                 */
                coachGroupsByBranch
                    .keys
                    .map { branch ->
                        branch.trim()
                    }
                    .filter { branch ->
                        branch.isNotBlank()
                    }
                    .distinct()
            } else {
                /*
                 * תאימות למשתמש שלא שמר עדיין
                 * במבנה החדש.
                 */
                (localBranches + serverBranches)
                    .map { branch ->
                        branch.trim()
                    }
                    .filter { branch ->
                        branch.isNotBlank()
                    }
                    .distinct()
            }

        val mergedLegacyGroups =
            (localGroups + serverGroups)
                .map { group ->
                    group.trim()
                }
                .filter { group ->
                    group.isNotBlank()
                }
                .distinct()

        allCoachGroups =
            if (coachGroupsByBranch.isNotEmpty()) {
                coachGroupsByBranch
                    .values
                    .flatten()
                    .map { group ->
                        group.trim()
                    }
                    .filter { group ->
                        group.isNotBlank()
                    }
                    .distinct()
            } else {
                mergedLegacyGroups
            }

        /*
         * הערך המדויק יסונן לפי הסניף
         * ב-LaunchedEffect הבא.
         */
        availableGroups = allCoachGroups

        /*
         * אם קיבלנו branch/group בכניסה למסך –
         * שומרים אותם.
         *
         * אחרת בוחרים את הפעיל הקיים,
         * ואם אין – את האפשרות הראשונה.
         */
        if (
            effectiveBranch.isBlank() ||
            effectiveBranch.contains(",")
        ) {
            val activeBranch =
                readCoachPrefList(
                    "active_branch",
                    "activeBranch",
                    "branch",
                    "selected_branch",
                    "current_branch"
                )
                    .firstOrNull {
                        it in availableBranches
                    }

            effectiveBranch =
                activeBranch
                    ?: availableBranches.firstOrNull()
                        .orEmpty()
        }

        if (
            effectiveGroupKey.isBlank() ||
            effectiveGroupKey.contains(",")
        ) {
            val activeGroup =
                readCoachPrefList(
                    "active_group",
                    "activeGroup",
                    "primaryGroup",
                    "groupKey",
                    "group_key",
                    "age_group",
                    "group"
                )
                    .firstOrNull {
                        it in availableGroups
                    }

            effectiveGroupKey =
                activeGroup
                    ?: availableGroups.firstOrNull()
                        .orEmpty()
        }
    }

    /*
  * TrainingCatalog הוא מקור האמת לקשר
  * בין סניף לבין הקבוצות הפעילות בו.
  *
  * attendanceGroups עשוי להכיל מסמכים ישנים,
  * ולכן אינו משמש לקביעת רשימת הקבוצות.
  */
    LaunchedEffect(
        effectiveBranch,
        coachGroupsByBranch,
        allCoachGroups
    ) {
        val cleanBranch =
            effectiveBranch.trim()

        if (cleanBranch.isBlank()) {
            availableGroups = emptyList()
            effectiveGroupKey = ""
            return@LaunchedEffect
        }

        fun String.normalizedGroupValue(): String =
            trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .lowercase(Locale("he", "IL"))

        fun String.normalizedBranchValue(): String =
            trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .lowercase(Locale("he", "IL"))

        /*
         * אם קיים המבנה החדש, אין צורך להסיק
         * קבוצות מהקטלוג או מהרשימה השטוחה.
         */
        if (coachGroupsByBranch.isNotEmpty()) {
            val assignedGroups =
                coachGroupsByBranch
                    .entries
                    .firstOrNull { entry ->
                        entry.key
                            .normalizedBranchValue() ==
                                cleanBranch
                                    .normalizedBranchValue()
                    }
                    ?.value
                    .orEmpty()
                    .map { group ->
                        group.trim()
                    }
                    .filter { group ->
                        group.isNotBlank()
                    }
                    .distinct()

            availableGroups = assignedGroups

            val selectedGroupStillExists =
                assignedGroups.any { group ->
                    group.normalizedGroupValue() ==
                            effectiveGroupKey
                                .normalizedGroupValue()
                }

            if (!selectedGroupStillExists) {
                effectiveGroupKey =
                    if (assignedGroups.size == 1) {
                        assignedGroups.first()
                    } else {
                        ""
                    }
            }

            return@LaunchedEffect
        }

        /*
         * הקבוצות המוגדרות בפועל עבור הסניף
         * לפי קטלוג האימונים המרכזי.
         */
        val catalogGroups =
            TrainingCatalog.groupsForBranch(
                branch = cleanBranch,
                isEnglish = false
            )
                .map { group ->
                    group.trim()
                }
                .filter { group ->
                    group.isNotBlank()
                }
                .distinct()

        /*
   * הנרמול משמש רק כדי לבדוק שהקטגוריה
   * קיימת בסניף. את שמות הקבוצות המקוריים
   * שומרים בנפרד ולא מאחדים קבוצות שונות
   * השייכות לאותה קטגוריית-על.
   */
        availableGroups =
            allCoachGroups
                .filter { coachGroup ->
                    val normalizedCoachGroup =
                        TrainingCatalog.normalizeGroupName(
                            coachGroup
                        ).normalizedGroupValue()

                    catalogGroups.any { catalogGroup ->
                        val normalizedCatalogGroup =
                            TrainingCatalog.normalizeGroupName(
                                catalogGroup
                            ).normalizedGroupValue()

                        normalizedCoachGroup ==
                                normalizedCatalogGroup
                    }
                }
                .map { group ->
                    group.trim()
                }
                .filter { group ->
                    group.isNotBlank()
                }
                .distinct()

        /*
         * אם שיוכי המאמן עדיין לא נטענו, משתמשים
         * בקטגוריות הקבוצות מהקטלוג ללא כפילויות.
         */
        if (
            availableGroups.isEmpty() &&
            catalogGroups.isNotEmpty()
        ) {
            /*
             * גם במצב הגיבוי שומרים את שמות
             * הקבוצות המקוריים מהקטלוג.
             */
            availableGroups =
                catalogGroups
                    .map { group ->
                        group.trim()
                    }
                    .filter { group ->
                        group.isNotBlank()
                    }
                    .distinct()
        }

        val selectedGroupStillExists =
            availableGroups.any { group ->
                group.normalizedGroupValue() ==
                        effectiveGroupKey
                            .normalizedGroupValue()
            }

        if (!selectedGroupStillExists) {
            effectiveGroupKey =
                if (availableGroups.size == 1) {
                    availableGroups.first()
                } else {
                    ""
                }
        }
    }

    val backgroundBrush =
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.background
            )
        )

    // --- טעינת מתאמנים אמיתיים + חישוב אחוז נוכחות לכל מתאמן ---
    val app = ctx.applicationContext as Application
    val repo = remember(app) { AttendanceRepository.get(app) }

    var traineeProfiles by remember {
        mutableStateOf<List<TraineeProfile>>(emptyList())
    }

    var isProfilesLoading by remember {
        mutableStateOf(true)
    }

    var didFinishInitialProfilesLoad by remember {
        mutableStateOf(false)
    }

    // מונע הצגת "לא נמצאו מתאמנים" לפני שהסנכרון הראשוני באמת הסתיים
    var isInitialServerSyncRunning by remember {
        mutableStateOf(true)
    }

    // הסניף הנבחר כרגע הוא סניף יחיד מתוך רשימת הסניפים של המאמן.
    val effectiveBranchPrimary = remember(effectiveBranch) {
        effectiveBranch.trim()
    }

    LaunchedEffect(
        effectiveBranch,
        effectiveGroupKey
    ) {
        isProfilesLoading = true
        isInitialServerSyncRunning = true
        didFinishInitialProfilesLoad = false

        fun String.norm(): String = this
            .trim()
            .replace('־', '-')   // maqaf
            .replace(Regex("\\s+"), " ")

        val groupName = effectiveGroupKey.norm()
        if (groupName.isBlank()) {
            isProfilesLoading = false
            isInitialServerSyncRunning = false
            didFinishInitialProfilesLoad = true
            return@LaunchedEffect
        }

        // candidate keys (כולל CSV + סניף ראשון + dash/en-dash)
        val fullBranch = effectiveBranch.norm()
        val primaryBranch = effectiveBranchPrimary.norm()

        val branchKeys = listOf(fullBranch, primaryBranch)
            .flatMap { b ->
                listOf(
                    b,
                    b.replace("-", "–"),
                    b.replace("–", "-")
                )
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // ✅ בוחרים מפתח DB שבאמת יש בו נתונים (כדי ש-% נוכחות לא יהיה 0 סתם)
        suspend fun pickDbBranchKey(): String {
            val today = LocalDate.now()
            for (k in branchKeys) {
                val members = repo.members(k, groupName).firstOrNull().orEmpty()
                if (members.isNotEmpty()) return k

                val rec = repo.attendanceForDay(k, groupName, today).firstOrNull().orEmpty()
                if (rec.isNotEmpty()) return k
            }
            return branchKeys.firstOrNull().orEmpty()
        }

        val branchDbKey = pickDbBranchKey()
        if (branchDbKey.isBlank()) {
            isProfilesLoading = false
            isInitialServerSyncRunning = false
            didFinishInitialProfilesLoad = true
            return@LaunchedEffect
        }

        // --- סנכרון אוטומטי מ-Firestore (users) לטבלת group_members ---
        var serverHasPotentialMembers = false

        try {
            val existingMembers = repo.members(branchDbKey, groupName).firstOrNull().orEmpty()
            val existingNames = existingMembers.map { it.displayName.trim() }.toSet()

            suspend fun fetchUsersByBranchesArray(cand: String) =
                Firebase.firestore.collection("users")
                    .whereArrayContains("branches", cand)
                    .whereArrayContains("groups", groupName)
                    .whereEqualTo("role", "trainee")
                    .get()
                    .await()

            suspend fun fetchUsersByBranchesCsv(cand: String) =
                Firebase.firestore.collection("users")
                    .whereEqualTo("branchesCsv", cand)
                    .whereArrayContains("groups", groupName)
                    .whereEqualTo("role", "trainee")
                    .get()
                    .await()

            suspend fun fetchUsersBySingleBranch(cand: String) =
                Firebase.firestore.collection("users")
                    .whereEqualTo("branch", cand)
                    .whereArrayContains("groups", groupName)
                    .whereEqualTo("role", "trainee")
                    .get()
                    .await()

            var usersSnap =
                runCatching { fetchUsersByBranchesArray(branchKeys.first()) }.getOrNull()

            if (usersSnap == null || usersSnap.isEmpty) {
                for (cand in branchKeys.drop(1)) {
                    val tmp = runCatching { fetchUsersByBranchesArray(cand) }.getOrNull()
                    if (tmp != null && !tmp.isEmpty) {
                        usersSnap = tmp; break
                    }
                }
            }

            if (usersSnap == null || usersSnap.isEmpty) {
                usersSnap = runCatching { fetchUsersByBranchesCsv(branchKeys.first()) }.getOrNull()
                if (usersSnap == null || usersSnap.isEmpty) {
                    for (cand in branchKeys.drop(1)) {
                        val tmp = runCatching { fetchUsersByBranchesCsv(cand) }.getOrNull()
                        if (tmp != null && !tmp.isEmpty) {
                            usersSnap = tmp; break
                        }
                    }
                }
            }

            if (usersSnap == null || usersSnap.isEmpty) {
                usersSnap = runCatching { fetchUsersBySingleBranch(branchKeys.first()) }.getOrNull()
                if (usersSnap == null || usersSnap.isEmpty) {
                    for (cand in branchKeys.drop(1)) {
                        val tmp = runCatching { fetchUsersBySingleBranch(cand) }.getOrNull()
                        if (tmp != null && !tmp.isEmpty) {
                            usersSnap = tmp; break
                        }
                    }
                }
            }

            val docs = usersSnap?.documents.orEmpty()
            serverHasPotentialMembers = docs.isNotEmpty()

            for (doc in docs) {
                val fullName = doc.getString("fullName")
                    ?: doc.getString("name")
                    ?: doc.getString("displayName")
                    ?: continue

                if (fullName.trim() in existingNames) continue

                repo.addMember(
                    branch = branchDbKey,
                    groupKey = groupName,
                    displayName = fullName.trim()
                )
            }
        } catch (_: Exception) {
        } finally {
            isInitialServerSyncRunning = false
        }

        // --- מאזינים ל-DB המקומי ובונים TraineeProfile "עשיר" (כולל Firestore) ---
        repo.members(branchDbKey, groupName).collectLatest { members: List<GroupMember> ->
            if (members.isEmpty()) {
                if (serverHasPotentialMembers || isInitialServerSyncRunning) {
                    isProfilesLoading = true
                    didFinishInitialProfilesLoad = false
                    return@collectLatest
                }

                traineeProfiles = emptyList()
                isProfilesLoading = false
                didFinishInitialProfilesLoad = true
                return@collectLatest
            }

            // ✅ לא מציגים רשימת members זמנית לפני איחוד.
            // קודם מעשירים את הנתונים מ-Firestore ומאחדים לפי טלפון/מייל/שם,
            // ורק בסוף מעדכנים traineeProfiles כדי שלא יופיעו כפולים לרגע.
            isProfilesLoading = true
            isInitialServerSyncRunning = true
            didFinishInitialProfilesLoad = false

            // 1) אחוז נוכחות מה-DB המקומי
            val today = LocalDate.now()
            val from = today.minusDays(59)
            val statsMap = mutableMapOf<Long, Pair<Int, Int>>() // mid -> (present,total)

            var d = from
            while (!d.isAfter(today)) {
                val records =
                    repo.attendanceForDay(branchDbKey, groupName, d).firstOrNull().orEmpty()
                for (record in records) {
                    val mid = extractMemberId(record) ?: continue
                    val status = recordStatus(record) ?: continue
                    val (p, t) = statsMap[mid] ?: (0 to 0)
                    statsMap[mid] =
                        (p + if (status == AttendanceStatus.PRESENT) 1 else 0) to (t + 1)
                }
                d = d.plusDays(1)
            }

            // 2) פרטים מ-Firestore (belt + birthDate -> age) לפי שם, עם נרמול קשוח
            fun String.normKey(): String = this
                .trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("""[."'\u05F3\u05F4,;:()\[\]{}]"""), "")
                .replace(Regex("\\s+"), " ")
                .lowercase(Locale("he", "IL"))

            fun beltHeb(b: String): String {
                return when (b.trim().lowercase(Locale.US)) {
                    "white" -> "לבנה"
                    "yellow" -> "צהובה"
                    "orange" -> "כתומה"
                    "green" -> "ירוקה"
                    "blue" -> "כחולה"
                    "brown" -> "חומה"
                    "black" -> "שחורה"
                    else -> b // אם תרצה להציג באנגלית כמו שהוא
                }
            }

            fun beltFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): String {
                val raw = (
                        doc.getString("belt")
                            ?: doc.getString("currentBelt")
                            ?: doc.getString("current_belt")
                            ?: doc.getString("beltName")
                            ?: doc.getString("belt_name")
                            ?: doc.getString("currentBeltName")
                            ?: doc.getString("currentBeltId")
                            ?: doc.getString("beltId")
                            ?: doc.getString("belt_id")
                            ?: ""
                        ).trim()

                if (raw.isBlank()) return ""

                val clean = raw
                    .lowercase(Locale.US)
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()

                return when {
                    clean == "white" || clean.contains("white") || clean == "לבנה" || clean.contains(
                        "לבנ"
                    ) -> "לבנה"

                    clean == "yellow" || clean.contains("yellow") || clean == "צהובה" || clean.contains(
                        "צהוב"
                    ) -> "צהובה"

                    clean == "orange" || clean.contains("orange") || clean == "כתומה" || clean.contains(
                        "כתומ"
                    ) -> "כתומה"

                    clean == "green" || clean.contains("green") || clean == "ירוקה" || clean.contains(
                        "ירוק"
                    ) -> "ירוקה"

                    clean == "blue" || clean.contains("blue") || clean == "כחולה" || clean.contains(
                        "כחול"
                    ) -> "כחולה"

                    clean == "brown" || clean.contains("brown") || clean == "חומה" || clean.contains(
                        "חומ"
                    ) -> "חומה"

                    clean == "black" || clean.contains("black") || clean == "שחורה" || clean.contains(
                        "שחור"
                    ) -> "שחורה"

                    else -> beltHeb(raw)
                }
            }

            fun seniorityFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): String {
                val textValue = (
                        doc.getString("seniority")
                            ?: doc.getString("trainingSeniority")
                            ?: doc.getString("training_seniority")
                            ?: doc.getString("yearsTraining")
                            ?: doc.getString("years_training")
                            ?: doc.getString("experience")
                            ?: doc.getString("trainingExperience")
                            ?: ""
                        ).trim()

                if (textValue.isNotBlank()) return textValue

                val numericYears = (
                        doc.getLong("seniorityYears")
                            ?: doc.getLong("trainingYears")
                            ?: doc.getLong("yearsTraining")
                            ?: doc.getLong("years_training")
                            ?: doc.getLong("experienceYears")
                            ?: doc.getLong("experience_years")
                        )?.toDouble()

                if (numericYears != null && numericYears > 0.0) {
                    val formatted = if (numericYears % 1.0 == 0.0) {
                        numericYears.toInt().toString()
                    } else {
                        String.format(Locale.US, "%.1f", numericYears)
                    }

                    return "$formatted שנים"
                }

                val startRaw = doc.get("trainingStartDate")
                    ?: doc.get("training_start_date")
                    ?: doc.get("startTrainingDate")
                    ?: doc.get("startedTrainingAt")

                val startAge = when (startRaw) {
                    is String -> runCatching {
                        LocalDate.parse(startRaw.trim())
                    }.getOrNull()

                    is com.google.firebase.Timestamp -> runCatching {
                        startRaw.toDate()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }.getOrNull()

                    is java.util.Date -> runCatching {
                        startRaw.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }.getOrNull()

                    is Number -> runCatching {
                        Instant.ofEpochMilli(startRaw.toLong())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }.getOrNull()

                    else -> null
                }

                if (startAge != null) {
                    val years = java.time.Period.between(startAge, LocalDate.now()).years
                    if (years > 0) return "$years שנים"
                }

                return ""
            }

            fun ageFromBirthDateRaw(raw: Any?): Int {
                fun yearsFromDate(dob: LocalDate): Int {
                    val years = java.time.Period.between(dob, LocalDate.now()).years
                    return years.coerceIn(0, 120)
                }

                return when (raw) {
                    is String -> {
                        val s = raw.trim()
                        if (s.isBlank()) return 0

                        runCatching {
                            yearsFromDate(LocalDate.parse(s))
                        }.getOrElse {
                            runCatching {
                                val parts = s.split("/", ".", "-")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }

                                if (parts.size == 3) {
                                    val a = parts[0].toInt()
                                    val b = parts[1].toInt()
                                    val c = parts[2].toInt()

                                    val dob = if (a > 1900) {
                                        LocalDate.of(a, b, c)
                                    } else {
                                        LocalDate.of(c, b, a)
                                    }

                                    yearsFromDate(dob)
                                } else {
                                    0
                                }
                            }.getOrDefault(0)
                        }
                    }

                    is com.google.firebase.Timestamp -> {
                        runCatching {
                            yearsFromDate(
                                raw.toDate()
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }.getOrDefault(0)
                    }

                    is java.util.Date -> {
                        runCatching {
                            yearsFromDate(
                                raw.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }.getOrDefault(0)
                    }

                    is Number -> {
                        runCatching {
                            yearsFromDate(
                                Instant.ofEpochMilli(raw.toLong())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }.getOrDefault(0)
                    }

                    is Map<*, *> -> {
                        val day = (
                                raw["day"]
                                    ?: raw["birthDay"]
                                    ?: raw["birth_day"]
                                    ?: raw["dd"]
                                )?.toString()?.toIntOrNull()

                        val month = (
                                raw["month"]
                                    ?: raw["birthMonth"]
                                    ?: raw["birth_month"]
                                    ?: raw["mm"]
                                )?.toString()?.toIntOrNull()

                        val year = (
                                raw["year"]
                                    ?: raw["birthYear"]
                                    ?: raw["birth_year"]
                                    ?: raw["yyyy"]
                                )?.toString()?.toIntOrNull()

                        if (day != null && month != null && year != null) {
                            runCatching {
                                yearsFromDate(LocalDate.of(year, month, day))
                            }.getOrDefault(0)
                        } else {
                            0
                        }
                    }

                    else -> 0
                }
            }

            fun ageFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): Int {
                val directAge = (
                        doc.getLong("age")
                            ?: doc.getLong("traineeAge")
                            ?: doc.getLong("ageYears")
                        )?.toInt()

                if (directAge != null && directAge in 1..120) return directAge

                val fromBirthDate = ageFromBirthDateRaw(
                    doc.get("birthDate")
                        ?: doc.get("birth_date")
                        ?: doc.get("dateOfBirth")
                        ?: doc.get("dob")
                )

                if (fromBirthDate > 0) return fromBirthDate

                val day = (
                        doc.getLong("birthDay")
                            ?: doc.getLong("birth_day")
                            ?: doc.getLong("day")
                        )?.toInt()

                val month = (
                        doc.getLong("birthMonth")
                            ?: doc.getLong("birth_month")
                            ?: doc.getLong("month")
                        )?.toInt()

                val year = (
                        doc.getLong("birthYear")
                            ?: doc.getLong("birth_year")
                            ?: doc.getLong("year")
                        )?.toInt()

                return if (day != null && month != null && year != null) {
                    runCatching {
                        java.time.Period.between(
                            LocalDate.of(year, month, day),
                            LocalDate.now()
                        ).years.coerceIn(0, 120)
                    }.getOrDefault(0)
                } else {
                    0
                }
            }

            data class FireUserInfo(
                val userDocId: String,
                val email: String,
                val phone: String,
                val age: Int,
                val beltHeb: String,
                val seniority: String,
                val beltAwardDates: Map<String, String>,
                val beltAwardDescriptions: Map<String, String>,
                val coachNotes: String,
                val seminarDates: Map<String, CoachDateEntry>,
                val campDates: Map<String, CoachDateEntry>,
                val certificationDates: Map<String, CoachDateEntry>
            )

            val userInfoByName = mutableMapOf<String, FireUserInfo>() // nameKey -> full user info

            fun normalizeEmailForMerge(value: String): String =
                value.trim().lowercase(Locale.US)

            fun normalizePhoneForMerge(value: String): String {
                val digits = value.filter { it.isDigit() }
                return when {
                    digits.startsWith("972") && digits.length >= 11 -> "0" + digits.drop(3)
                    digits.startsWith("05") -> digits
                    digits.length == 9 && digits.startsWith("5") -> "0$digits"
                    else -> digits
                }
            }

            fun primaryEmailFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): String =
                listOf(
                    doc.getString("email"),
                    doc.getString("userEmail"),
                    doc.getString("mail"),
                    doc.getString("gmail")
                ).firstOrNull { !it.isNullOrBlank() }
                    ?.trim()
                    .orEmpty()

            fun primaryPhoneFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): String =
                listOf(
                    doc.getString("phone"),
                    doc.getString("phoneNumber"),
                    doc.getString("mobile"),
                    doc.getString("mobilePhone"),
                    doc.getString("cellPhone"),
                    doc.getString("phone_number")
                ).firstOrNull { !it.isNullOrBlank() }
                    ?.trim()
                    .orEmpty()

            fun String.normProfileKey(): String = this
                .trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .lowercase(Locale("he", "IL"))

            fun userDocMatchesBranchAndGroup(
                doc: com.google.firebase.firestore.DocumentSnapshot,
                branchCandidates: List<String>,
                groupCandidate: String
            ): Boolean {
                val branchSet = branchCandidates
                    .map { it.normProfileKey() }
                    .filter { it.isNotBlank() }
                    .toSet()

                val docBranches = buildList {
                    doc.getString("branch")?.let { add(it) }
                    doc.getString("activeBranch")?.let { add(it) }
                    doc.getString("active_branch")?.let { add(it) }
                    doc.getString("branchesCsv")?.split(",")?.forEach { add(it) }
                    (doc.get("branches") as? List<*>)?.forEach { item ->
                        item?.toString()?.let { add(it) }
                    }
                }
                    .map { it.normProfileKey() }
                    .filter { it.isNotBlank() }

                val groupNorm = groupCandidate.normProfileKey()

                val docGroups = buildList {
                    doc.getString("primaryGroup")?.let { add(it) }
                    doc.getString("activeGroup")?.let { add(it) }
                    doc.getString("active_group")?.let { add(it) }
                    doc.getString("groupKey")?.let { add(it) }
                    doc.getString("group_key")?.let { add(it) }
                    doc.getString("group")?.let { add(it) }
                    doc.getString("age_group")?.let { add(it) }
                    (doc.get("groups") as? List<*>)?.forEach { item ->
                        item?.toString()?.let { add(it) }
                    }
                }
                    .map { it.normProfileKey() }
                    .filter { it.isNotBlank() }

                val branchMatches =
                    branchSet.isEmpty() ||
                            docBranches.any { docBranch ->
                                docBranch in branchSet ||
                                        branchSet.any { candidate ->
                                            candidate.length >= 3 &&
                                                    docBranch.length >= 3 &&
                                                    (docBranch.contains(candidate) || candidate.contains(
                                                        docBranch
                                                    ))
                                        }
                            }

                val groupMatches =
                    groupNorm.isBlank() ||
                            docGroups.any { docGroup ->
                                docGroup == groupNorm
                            }

                return branchMatches && groupMatches
            }

            val userDocs = runCatching {
                val directDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()

                for (branchCandidate in branchKeys) {
                    runCatching {
                        directDocs.addAll(
                            Firebase.firestore.collection("users")
                                .whereArrayContains("branches", branchCandidate)
                                .whereArrayContains("groups", groupName)
                                .whereEqualTo("role", "trainee")
                                .get()
                                .await()
                                .documents
                        )
                    }

                    runCatching {
                        directDocs.addAll(
                            Firebase.firestore.collection("users")
                                .whereEqualTo("branch", branchCandidate)
                                .whereArrayContains("groups", groupName)
                                .whereEqualTo("role", "trainee")
                                .get()
                                .await()
                                .documents
                        )
                    }

                    runCatching {
                        directDocs.addAll(
                            Firebase.firestore.collection("users")
                                .whereEqualTo("branchesCsv", branchCandidate)
                                .whereArrayContains("groups", groupName)
                                .whereEqualTo("role", "trainee")
                                .get()
                                .await()
                                .documents
                        )
                    }
                }

                val distinctDirect =
                    directDocs.distinctBy { it.id }

                /*
                 * שאילתה ישירה יכולה למצוא רק חלק מהמתאמנים,
                 * משום שבמסמכי משתמש ישנים הסניף והקבוצה
                 * נשמרו בשמות שדות או במבנים שונים.
                 *
                 * לכן קוראים גם את מסמכי users ומצרפים כל
                 * מסמך ששמו תואם למתאמן ברשימת הקבוצה.
                 */
                val memberNameKeys =
                    members
                        .map { member ->
                            member.displayName.normKey()
                        }
                        .filter { nameKey ->
                            nameKey.isNotBlank()
                        }
                        .toSet()

                val fallbackDocs =
                    Firebase.firestore
                        .collection("users")
                        .get()
                        .await()
                        .documents
                        .filter { doc ->
                            val documentNameKey =
                                (
                                        doc.getString("fullName")
                                            ?: doc.getString("name")
                                            ?: doc.getString(
                                                "displayName"
                                            )
                                            ?: ""
                                        ).normKey()

                            val matchesMemberName =
                                documentNameKey.isNotBlank() &&
                                        documentNameKey in
                                        memberNameKeys

                            val matchesBranchAndGroup =
                                userDocMatchesBranchAndGroup(
                                    doc = doc,
                                    branchCandidates =
                                        branchKeys,
                                    groupCandidate =
                                        groupName
                                )

                            matchesMemberName ||
                                    matchesBranchAndGroup
                        }

                /*
                 * מאחדים את תוצאות השאילתות הישירות
                 * עם מסמכי ההתאמה ומסירים כפילויות.
                 */
                (distinctDirect + fallbackDocs)
                    .distinctBy { document ->
                        document.id
                    }
            }.getOrNull().orEmpty()

            val mergedUserDocs = userDocs
                .groupBy { doc ->
                    val emailKey = normalizeEmailForMerge(primaryEmailFromDoc(doc))
                    val phoneKey = normalizePhoneForMerge(primaryPhoneFromDoc(doc))
                    val nameKey = (
                            doc.getString("fullName")
                                ?: doc.getString("name")
                                ?: doc.getString("displayName")
                                ?: doc.id
                            ).normKey()

                    when {
                        emailKey.isNotBlank() -> "email:$emailKey"
                        phoneKey.isNotBlank() -> "phone:$phoneKey"
                        else -> "name:$nameKey"
                    }
                }
                .map { (_, docs) ->
                    docs.maxWithOrNull(
                        compareBy<com.google.firebase.firestore.DocumentSnapshot> {
                            /*
                             * נותנים עדיפות למסמך המכיל
                             * את נתוני הפרופיל המלאים.
                             */
                            var profileScore = 0

                            if (
                                beltFromDoc(it).isNotBlank()
                            ) {
                                profileScore += 4
                            }

                            if (ageFromDoc(it) > 0) {
                                profileScore += 3
                            }

                            if (
                                seniorityFromDoc(it)
                                    .isNotBlank()
                            ) {
                                profileScore += 2
                            }

                            if (
                                primaryEmailFromDoc(it)
                                    .isNotBlank()
                            ) {
                                profileScore += 1
                            }

                            if (
                                primaryPhoneFromDoc(it)
                                    .isNotBlank()
                            ) {
                                profileScore += 1
                            }

                            profileScore
                        }.thenBy {
                            if (
                                (
                                        it.getString("fullName")
                                            ?: it.getString("name")
                                            ?: it.getString(
                                                "displayName"
                                            )
                                            ?: ""
                                        ).isNotBlank()
                            ) {
                                1
                            } else {
                                0
                            }
                        }.thenBy {
                            it.id
                        }
                    ) ?: docs.first()
                }

            for (doc in mergedUserDocs) {
                val n = (doc.getString("fullName")
                    ?: doc.getString("name")
                    ?: doc.getString("displayName")
                    ?: continue).normKey()

                val email = primaryEmailFromDoc(doc)
                val phone = primaryPhoneFromDoc(doc)

                val belt = beltFromDoc(doc)
                val age = ageFromDoc(doc)
                val seniority = seniorityFromDoc(doc)

                fun readStringMap(fieldName: String): Map<String, String> {
                    val raw = doc.get(fieldName) as? Map<*, *> ?: emptyMap<Any, Any>()
                    return raw.entries.associate { entry ->
                        entry.key.toString() to entry.value.toString()
                    }
                }

                fun readCoachEntryMap(fieldName: String): Map<String, CoachDateEntry> {
                    val raw = doc.get(fieldName) as? Map<*, *> ?: emptyMap<Any, Any>()

                    return raw.entries.associate { entry ->
                        val key = entry.key.toString()

                        val parsed =
                            when (val value = entry.value) {
                                is Map<*, *> -> CoachDateEntry(
                                    date =
                                        value["date"]
                                            ?.toString()
                                            .orEmpty(),
                                    description =
                                        value["description"]
                                            ?.toString()
                                            .orEmpty()
                                )

                                // ✅ תאימות לאחור: אם בעבר נשמר רק תאריך כמחרוזת
                                is String -> CoachDateEntry(
                                    date = value,
                                    description = ""
                                )

                                else -> CoachDateEntry()
                            }

                        key to parsed
                    }
                }

                val beltAwardDates = readStringMap("beltAwardDates")
                val beltAwardDescriptions = readStringMap("beltAwardDescriptions")
                val coachNotes = doc.getString("coachNotes").orEmpty()
                val seminarDates = readCoachEntryMap("seminarDates")
                val campDates = readCoachEntryMap("campDates")
                val certificationDates = readCoachEntryMap("certificationDates")

                val mergedFireUserInfo = FireUserInfo(
                    userDocId = doc.id,
                    email = email,
                    phone = phone,
                    age = age,
                    beltHeb = belt,
                    seniority = seniority,
                    beltAwardDates = beltAwardDates,
                    beltAwardDescriptions =
                        beltAwardDescriptions,
                    coachNotes = coachNotes,
                    seminarDates = seminarDates,
                    campDates = campDates,
                    certificationDates =
                        certificationDates
                )

                val normalizedEmail =
                    normalizeEmailForMerge(email)

                val normalizedPhone =
                    normalizePhoneForMerge(phone)

                /*
                 * mergedUserDocs מכיל רק מסמך ראשי אחד,
                 * אבל userDocs עדיין מכיל את כל המסמכים
                 * המקוריים. מאתרים את כל המסמכים השייכים
                 * לאותו מייל או טלפון ושומרים את המידע
                 * המאוחד תחת כל שמות הכינוי שלהם.
                 */
                val matchingIdentityDocuments =
                    userDocs.filter { candidate ->
                        val candidateEmail =
                            normalizeEmailForMerge(
                                primaryEmailFromDoc(candidate)
                            )

                        val candidatePhone =
                            normalizePhoneForMerge(
                                primaryPhoneFromDoc(candidate)
                            )

                        when {
                            normalizedEmail.isNotBlank() ->
                                candidateEmail ==
                                        normalizedEmail

                            normalizedPhone.isNotBlank() ->
                                candidatePhone ==
                                        normalizedPhone

                            else ->
                                candidate.id == doc.id
                        }
                    }

                val identityNameKeys =
                    matchingIdentityDocuments
                        .mapNotNull { candidate ->
                            (
                                    candidate.getString("fullName")
                                        ?: candidate.getString("name")
                                        ?: candidate.getString(
                                            "displayName"
                                        )
                                    )
                                ?.normKey()
                                ?.takeIf { key ->
                                    key.isNotBlank()
                                }
                        }
                        .plus(n)
                        .distinct()

                identityNameKeys.forEach { nameKey ->
                    userInfoByName[nameKey] =
                        mergedFireUserInfo
                }
            }

            val builtProfiles = members.map { m ->
                val stat = statsMap[m.id]
                val pct = if (stat != null && stat.second > 0) {
                    ((stat.first * 100.0) / stat.second).toInt()
                } else 0

                val key = m.displayName.normKey()
                val info = userInfoByName[key]
                val userDocId = info?.userDocId.orEmpty()
                val email = info?.email.orEmpty()
                val phone = info?.phone.orEmpty()
                val age = info?.age ?: 0
                val belt = info?.beltHeb.orEmpty()
                val seniority = info?.seniority.orEmpty()
                val beltAwardDates = info?.beltAwardDates ?: emptyMap()
                val beltAwardDescriptions = info?.beltAwardDescriptions ?: emptyMap()
                val coachNotes = info?.coachNotes.orEmpty()
                val seminarDates = info?.seminarDates ?: emptyMap()
                val campDates = info?.campDates ?: emptyMap()
                val certificationDates = info?.certificationDates ?: emptyMap()

                TraineeProfile(
                    id = m.id.toString(),
                    fullName = m.displayName,
                    belt = belt,
                    seniority = seniority,
                    age = age,
                    attendancePct = pct,
                    branch = branchDbKey,
                    groupKey = groupName,
                    email = email,
                    phone = phone,
                    userDocId = userDocId,
                    beltAwardDates = beltAwardDates,
                    beltAwardDescriptions = beltAwardDescriptions,
                    coachNotes = coachNotes,
                    seminarDates = seminarDates,
                    campDates = campDates,
                    certificationDates = certificationDates
                )
            }

            val mergedProfiles = builtProfiles
                .groupBy { profile ->
                    val emailKey = normalizeEmailForMerge(profile.email)
                    val phoneKey = normalizePhoneForMerge(profile.phone)
                    val nameKey = profile.fullName.normKey()

                    when {
                        emailKey.isNotBlank() -> "email:$emailKey"
                        phoneKey.isNotBlank() -> "phone:$phoneKey"
                        else -> "name:$nameKey"
                    }
                }
                .map { (_, duplicates) ->
                    duplicates.maxWithOrNull(
                        compareBy<TraineeProfile> {
                            if (it.userDocId.isNotBlank()) 1 else 0
                        }.thenBy {
                            it.attendancePct
                        }.thenBy {
                            it.belt.length
                        }
                    ) ?: duplicates.first()
                }
                .sortedBy { it.fullName.trim() }

            traineeProfiles = mergedProfiles
            isProfilesLoading = false
            isInitialServerSyncRunning = false
            didFinishInitialProfilesLoad = true
        }
    }


    // אם זה לא מאמן – עדיין רוצים טופ-בר עם אייקונים
    if (!isCoachRole) {

        Scaffold(
            topBar = {
                val contextLang = LocalContext.current
                val langManager = remember { AppLanguageManager(contextLang) }

                KmiTopBar(
                    title = coachTr(isEnglish, "רשימת מתאמנים", "Trainees list"),
                    onOpenDrawer = onOpenDrawer,
                    onHome = onOpenHome,
                    showTopHome = false,
                    showRoleStatus = false,
                    lockSearch = false,
                    showBottomActions = true,
                    currentLang = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",
                    onToggleLanguage = {
                        val newLang =
                            if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                                AppLanguage.ENGLISH
                            } else {
                                AppLanguage.HEBREW
                            }

                        langManager.setLanguage(newLang)
                        (contextLang as? Activity)?.recreate()
                    }
                )
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0)
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .background(backgroundBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = coachTr(
                        isEnglish,
                        "המסך זמין למאמנים בלבד",
                        "This screen is available for coaches only"
                    ),
                    style = KmiTypography.sectionTitle.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        return
    }

    var traineeSearchQuery by rememberSaveable {
        mutableStateOf("")
    }

    fun normalizeCoachSearchText(value: String): String =
        value
            .trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale("he", "IL"))

    /*
     * הקריאה מתבצעת מתוך Composition ולכן שינוי המתג
     * במסך ההגדרות גורם לעדכון מיידי של המסך.
     */
    val demoPrivacyEnabled =
        DemoPrivacy.isEnabled()

    fun demoSafeName(
        profile: TraineeProfile,
        demoIndex: Int? = null
    ): String {
        /*
         * userDocId נשאר המזהה היציב לפעולות,
         * שמירה ועדכון. demoIndex משפיע רק
         * על השם שמוצג במצב הדגמה.
         */
        val stableKey =
            profile.userDocId.ifBlank {
                profile.id.ifBlank {
                    profile.fullName
                }
            }

        val displayName =
            if (demoIndex != null) {
                TraineeDisplayNameMapper.displayName(
                    realName = profile.fullName,
                    stableKey = stableKey,
                    demoIndex = demoIndex,
                    isEnglish = isEnglish
                )
            } else {
                TraineeDisplayNameMapper.displayName(
                    realName = profile.fullName,
                    stableKey = stableKey,
                    isEnglish = isEnglish
                )
            }

        return displayName.ifBlank {
            coachTr(
                isEnglish,
                "מתאמן ללא שם",
                "Unnamed trainee"
            )
        }
    }

    val uiProfiles =
        remember(
            traineeProfiles,
            traineeSearchQuery,
            demoPrivacyEnabled,
            isEnglish
        ) {
            val query =
                normalizeCoachSearchText(
                    traineeSearchQuery
                )

            val mergedProfiles =
                traineeProfiles
                    .groupBy { profile ->
                        val emailKey =
                            profile.email
                                .trim()
                                .lowercase(Locale.US)

                        val phoneKey =
                            profile.phone
                                .filter { it.isDigit() }
                                .let { digits ->
                                    when {
                                        digits.startsWith("972") &&
                                                digits.length >= 11 ->
                                            "0" + digits.drop(3)

                                        digits.startsWith("05") ->
                                            digits

                                        digits.length == 9 &&
                                                digits.startsWith("5") ->
                                            "0$digits"

                                        else ->
                                            digits
                                    }
                                }

                        val nameKey =
                            normalizeCoachSearchText(
                                profile.fullName
                            )

                        when {
                            emailKey.isNotBlank() ->
                                "email:$emailKey"

                            phoneKey.isNotBlank() ->
                                "phone:$phoneKey"

                            else ->
                                "name:$nameKey"
                        }
                    }
                    .map { (_, duplicates) ->
                        duplicates.maxWithOrNull(
                            compareBy<TraineeProfile> {
                                if (
                                    it.userDocId.isNotBlank()
                                ) {
                                    1
                                } else {
                                    0
                                }
                            }.thenBy {
                                it.attendancePct
                            }.thenBy {
                                it.belt.length
                            }
                        ) ?: duplicates.first()
                    }
                    .sortedBy { profile ->
                        demoSafeName(profile)
                            .trim()
                    }

            if (query.isBlank()) {
                mergedProfiles
            } else {
                mergedProfiles.filter { trainee ->
                    /*
                     * במצב הדגמה מחפשים לפי שם הדמה בלבד.
                     * השם האמיתי אינו משתתף בחיפוש ואינו
                     * נחשף בעקיפין למי שמציג את האפליקציה.
                     */
                    listOf(
                        demoSafeName(trainee),
                        trainee.belt,
                        trainee.branch,
                        trainee.groupKey,
                        trainee.seniority,
                        trainee.age
                            .takeIf { it > 0 }
                            ?.toString()
                            .orEmpty(),
                        trainee.attendancePct
                            .takeIf { it > 0 }
                            ?.toString()
                            .orEmpty()
                    ).any { value ->
                        normalizeCoachSearchText(
                            value
                        ).contains(query)
                    }
                }
            }
        }

    // בחירה נוכחית
    // לא בוחרים מתאמן אוטומטית בכניסה למסך.
    var selectedId by remember {
        mutableStateOf<String?>(null)
    }

    val selected: TraineeProfile? =
        if (
            isProfilesLoading ||
            isInitialServerSyncRunning ||
            !didFinishInitialProfilesLoad
        ) {
            null
        } else {
            uiProfiles.firstOrNull { trainee ->
                trainee.id == selectedId
            }
        }

    // הערות מאמן לפי מתאמן
    val coachNotes = remember { mutableStateMapOf<String, String>() }

    // תאריכי קבלת חגורות לפי מתאמן
    val beltAwardDatesState = remember { mutableStateMapOf<String, Map<String, String>>() }

    // ✅ תיאור חופשי לכל חגורה לפי מתאמן
    val beltAwardDescriptionsState = remember { mutableStateMapOf<String, Map<String, String>>() }

    // ✅ שדות נוספים למילוי ע"י המאמן לפי מתאמן: תאריך + תיאור
    val seminarDatesState = remember { mutableStateMapOf<String, Map<String, CoachDateEntry>>() }
    val campDatesState = remember { mutableStateMapOf<String, Map<String, CoachDateEntry>>() }
    val certificationDatesState =
        remember { mutableStateMapOf<String, Map<String, CoachDateEntry>>() }

    var showStatsSheet by remember { mutableStateOf(false) }

// ✅ סטטיסטיקה עליונה: מחזירים את הכרטיסים היפים, אבל ככרטיס מתקפל נפרד.
    var isTopStatsExpanded by rememberSaveable {
        mutableStateOf(false)
    }

// ✅ בחירת מתאמן: פתוח רק כשצריך לבחור / לחפש.
// אחרי בחירת מתאמן הכרטיס נסגר אוטומטית כדי לפנות מקום למסך.
    var isTraineePickerExpanded by rememberSaveable {
        mutableStateOf(true)
    }

    /*
     * רשימת המתאמנים נפתחת כתפריט צף.
     * לכן היא אינה מגדילה את גובה כרטיס הבחירה.
     */
    var isTraineeMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }

// ✅ החלק התחתון עובד כאקורדיון: רק נושא אחד פתוח בכל רגע.
    var expandedCoachSection by rememberSaveable(selectedId) {
        mutableStateOf<String?>(null)
    }

    val beltDatesSectionKey = "belt_dates"
    val seminarsSectionKey = "seminars"
    val campsSectionKey = "camps"
    val certificationsSectionKey = "certifications"

    val groupStats =
        remember(
            traineeProfiles
        ) {
            buildGroupStats(
                profiles = traineeProfiles,
                filtered = traineeProfiles
            )
        }

    val screenScope = rememberCoroutineScope()

    suspend fun resolveUserDocIdForSelected(
        selectedProfile: TraineeProfile
    ): String {
        val directDocId = selectedProfile.userDocId.trim()
        if (directDocId.isNotBlank()) return directDocId

        fun String.normSaveKey(): String = this
            .trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale("he", "IL"))

        val targetName = selectedProfile.fullName.normSaveKey()
        val targetBranch = selectedProfile.branch.normSaveKey()
        val targetGroup = selectedProfile.groupKey.normSaveKey()

        val docs = Firebase.firestore.collection("users")
            .whereEqualTo("role", "trainee")
            .get()
            .await()
            .documents

        val matched = docs.firstOrNull { doc ->
            val docName = (
                    doc.getString("fullName")
                        ?: doc.getString("name")
                        ?: doc.getString("displayName")
                        ?: ""
                    ).normSaveKey()

            val docGroups = (doc.get("groups") as? List<*>)
                ?.mapNotNull { it?.toString()?.normSaveKey() }
                .orEmpty()

            val docBranches = buildList {
                doc.getString("branch")?.let { add(it.normSaveKey()) }
                doc.getString("branchesCsv")?.split(",")?.forEach { add(it.normSaveKey()) }
                (doc.get("branches") as? List<*>)?.forEach { item ->
                    item?.toString()?.let { add(it.normSaveKey()) }
                }
            }

            val nameMatches = docName == targetName
            val groupMatches = targetGroup.isBlank() || targetGroup in docGroups
            val branchMatches = targetBranch.isBlank() || docBranches.any { branch ->
                branch == targetBranch ||
                        branch.contains(targetBranch) ||
                        targetBranch.contains(branch)
            }

            nameMatches && groupMatches && branchMatches
        } ?: docs.firstOrNull { doc ->
            val docName = (
                    doc.getString("fullName")
                        ?: doc.getString("name")
                        ?: doc.getString("displayName")
                        ?: ""
                    ).normSaveKey()

            docName == targetName
        }

        val resolvedDocId = matched?.id.orEmpty()

        if (resolvedDocId.isBlank()) {
            error("Missing userDocId for trainee: ${selectedProfile.fullName}")
        }

        return resolvedDocId
    }

    suspend fun saveBeltAwardDatesForSelected(
        selectedProfile: TraineeProfile,
        dates: Map<String, String>,
        descriptions: Map<String, String>
    ) {
        val userDocId = resolveUserDocIdForSelected(selectedProfile)

        val cleanedDates = dates
            .mapValues { it.value.trim() }
            .filterValues { it.isNotBlank() }

        val cleanedDescriptions = descriptions
            .mapValues { it.value.trim() }
            .filterValues { it.isNotBlank() }

        if (cleanedDates.isEmpty() && cleanedDescriptions.isEmpty()) return

        val dateUpdates = cleanedDates.entries.associate { (beltName, dateValue) ->
            "beltAwardDates.$beltName" to dateValue
        }

        val descriptionUpdates =
            cleanedDescriptions.entries.associate { (beltName, descriptionValue) ->
                "beltAwardDescriptions.$beltName" to descriptionValue
            }

        Firebase.firestore.collection("users")
            .document(userDocId)
            .update(dateUpdates + descriptionUpdates)
            .await()
    }

    suspend fun saveCoachDateSectionForSelected(
        selectedProfile: TraineeProfile,
        firestoreFieldName: String,
        entries: Map<String, CoachDateEntry>
    ) {
        val userDocId = resolveUserDocIdForSelected(selectedProfile)

        val cleanedEntries = entries
            .mapValues { (_, value) ->
                mapOf(
                    "date" to value.date.trim(),
                    "description" to value.description.trim()
                )
            }
            .filterValues { value ->
                value["date"].orEmpty().isNotBlank() ||
                        value["description"].orEmpty().isNotBlank()
            }

        if (cleanedEntries.isEmpty()) return

        val updates = cleanedEntries.entries.associate { (itemName, value) ->
            "$firestoreFieldName.$itemName" to value
        }

        Firebase.firestore.collection("users")
            .document(userDocId)
            .update(updates)
            .await()
    }

    suspend fun saveCoachNotesForSelected(
        selectedProfile: TraineeProfile,
        note: String
    ) {
        val userDocId = resolveUserDocIdForSelected(selectedProfile)

        val cleanNote = note.trim()

        Firebase.firestore.collection("users")
            .document(userDocId)
            .update(
                mapOf(
                    "coachNotes" to cleanNote,
                    "coachNotesUpdatedAtMillis" to System.currentTimeMillis()
                )
            )
            .await()
    }

    LaunchedEffect(uiProfiles) {
        /*
         * טעינת הרשימה אינה בוחרת מתאמן אוטומטית.
         * מתאמן נבחר רק בעקבות לחיצה מפורשת של המאמן.
         */
        uiProfiles.forEach { trainee ->
            if (beltAwardDatesState[trainee.id] == null) {
                beltAwardDatesState[trainee.id] = trainee.beltAwardDates
            }

            if (beltAwardDescriptionsState[trainee.id] == null) {
                beltAwardDescriptionsState[trainee.id] = trainee.beltAwardDescriptions
            }

            if (coachNotes[trainee.id] == null) {
                coachNotes[trainee.id] = trainee.coachNotes
            }

            if (seminarDatesState[trainee.id] == null) {
                seminarDatesState[trainee.id] = trainee.seminarDates
            }

            if (campDatesState[trainee.id] == null) {
                campDatesState[trainee.id] = trainee.campDates
            }

            if (certificationDatesState[trainee.id] == null) {
                certificationDatesState[trainee.id] = trainee.certificationDates
            }
        }
    }

    Scaffold(
        topBar = {
            val contextLang = LocalContext.current
            val langManager = remember { AppLanguageManager(contextLang) }

            KmiTopBar(
                title = if (showStatsSheet) {
                    coachTr(
                        isEnglish,
                        "סטטיסטיקה",
                        "Statistics"
                    )
                } else {
                    coachTr(
                        isEnglish,
                        "רשימת מתאמנים",
                        "Trainees list"
                    )
                },
                onOpenDrawer = onOpenDrawer,
                onHome = onOpenHome,

                showTopShare =
                    showStatsSheet,

                onShare = {

                    if (showStatsSheet) {

                        /*
                         * אנחנו במסך הסטטיסטיקה.
                         *
                         * CoachGroupStatsPremiumScreen יחליט
                         * לפי הטאב הפעיל אם ליצור:
                         *
                         * 1. PDF סטטיסטיקת הקבוצה
                         * 2. PDF סטטיסטיקה ארצית
                         */
                        statisticsShareTrigger++

                    } else {

                        /*
                         * אנחנו במסך רשימת המתאמנים.
                         *
                         * השיתוף כאן הוא PDF של כל הקבוצה,
                         * ולא רק של תוצאות החיפוש המוצגות כרגע.
                         */
                        if (
                            effectiveBranchPrimary
                                .isNotBlank() &&
                            effectiveGroupKey
                                .isNotBlank() &&
                            traineeProfiles
                                .isNotEmpty()
                        ) {

                            val pdfProfiles =
                                traineeProfiles
                                    .distinctBy { profile ->
                                        profile.userDocId.ifBlank {
                                            profile.id.ifBlank {
                                                profile.fullName
                                            }
                                        }
                                    }
                                    .sortedBy { profile ->
                                        demoSafeName(profile)
                                            .trim()
                                    }

                            val pdfStats =
                                buildGroupStats(
                                    profiles = pdfProfiles,
                                    filtered = pdfProfiles
                                )

                            val pdfFile =
                                createCoachTraineesPdf(
                                    context = ctx,
                                    profiles =
                                        pdfProfiles.mapIndexed { index,
                                                                 profile ->
                                            profile.copy(
                                                fullName =
                                                    demoSafeName(
                                                        profile = profile,
                                                        demoIndex = index + 1
                                                    )
                                            )
                                        },
                                    stats = pdfStats,
                                    branch =
                                        effectiveBranchPrimary,
                                    groupKey =
                                        effectiveGroupKey,
                                    isEnglish =
                                        isEnglish
                                )

                            val uri =
                                FileProvider.getUriForFile(
                                    ctx,
                                    "${ctx.packageName}.fileprovider",
                                    pdfFile
                                )

                            val sendIntent =
                                Intent(
                                    Intent.ACTION_SEND
                                ).apply {

                                    type =
                                        "application/pdf"

                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        coachTr(
                                            isEnglish,
                                            "דו״ח רשימת מתאמנים",
                                            "Trainees list report"
                                        )
                                    )

                                    putExtra(
                                        Intent.EXTRA_STREAM,
                                        uri
                                    )

                                    addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }

                            ctx.startActivity(
                                Intent.createChooser(
                                    sendIntent,
                                    coachTr(
                                        isEnglish,
                                        "שיתוף PDF",
                                        "Share PDF"
                                    )
                                )
                            )
                        }
                    }
                },

                showTopHome = false,
                showRoleStatus = false,
                lockSearch = false,
                showBottomActions = true,
                currentLang = if (langManager.getCurrentLanguage() == AppLanguage.ENGLISH) "en" else "he",
                onToggleLanguage = {
                    val newLang =
                        if (langManager.getCurrentLanguage() == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    (contextLang as? Activity)?.recreate()
                }
            )
        },
        bottomBar = {
            if (!showStatsSheet && !isKeyboardVisible) {
                val statsBubbleTransition = rememberInfiniteTransition(
                    label = "coachStatsBubbleTransition"
                )

                val statsBubbleOffset by statsBubbleTransition.animateFloat(
                    initialValue = -120f,
                    targetValue = 320f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 2600,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "coachStatsBubbleOffset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E4B86),
                                    Color(0xFF0EA5E9)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Surface(
                        onClick = { showStatsSheet = true },
                        shape = RoundedCornerShape(15.dp),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.72f),
                                        Color.White.copy(alpha = 0.18f),
                                        Color.White.copy(alpha = 0.72f)
                                    )
                                ),
                                shape = RoundedCornerShape(15.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF7F00FF),
                                            Color(0xFF3F51B5),
                                            Color(0xFF03A9F4)
                                        )
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = statsBubbleOffset.dp)
                                    .size(96.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.34f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape =
                                            CircleShape
                                    )
                            )

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Assessment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(
                                            KmiIconSize.small
                                        )
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    Text(
                                        text =
                                            coachTr(
                                                isEnglish,
                                                "סטטיסטיקה",
                                                "Statistics"
                                            ),
                                        fontWeight =
                                            FontWeight.ExtraBold,
                                        color = Color.White,
                                        style = KmiTypography.action,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0)
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundBrush)
        ) {

            if (!showStatsSheet) {
                CoachTraineesTopTabs(
                    isEnglish = isEnglish,
                    selectionSelected = isTraineePickerExpanded,
                    onListClick = {
                        isTraineePickerExpanded = false
                        isTraineeMenuExpanded = false
                    },
                    onSelectionClick = {
                        isTraineePickerExpanded = true
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 12.dp,
                        top = if (showStatsSheet) {
                            0.dp
                        } else {
                            54.dp
                        },
                        end = 12.dp
                    )
                    .imePadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = if (
                        showStatsSheet ||
                        isKeyboardVisible
                    ) {
                        8.dp
                    } else {
                        24.dp
                    }
                ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                if (!isTraineePickerExpanded) {

                    // =====================================================
                    // סיכום הקבוצה
                    // =====================================================

                    item {
                        CoachTopStatsCard(
                            stats = groupStats,
                            searchQuery = traineeSearchQuery,
                            onSearchQueryChange = { traineeSearchQuery = it },
                            isEnglish = isEnglish,
                            showSearch = false
                        )
                    }

                    // =====================================================
                    // רשימת כל המתאמנים בקבוצה
                    // =====================================================

                    uiProfiles.forEachIndexed { index, trainee ->

                        item(
                            key = "trainee_list_${trainee.id}"
                        ) {
                            Surface(
                                onClick = {
                                    selectedId = trainee.id

                                    /*
                                     * לחיצה על מתאמן מהרשימה עוברת
                                     * לטאב "בחירת מתאמן", כאשר אותו
                                     * מתאמן כבר מסומן ומוצגים פרטיו.
                                     */
                                    isTraineePickerExpanded = true
                                    isTraineeMenuExpanded = false
                                    expandedCoachSection = null
                                },
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .surface,
                                shape = RoundedCornerShape(18.dp),
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .outline
                                            .copy(alpha = 0.28f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 14.dp,
                                            vertical = 12.dp
                                        ),
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    // שם המתאמן
                                    Text(
                                        text =
                                            demoSafeName(
                                                profile = trainee,
                                                demoIndex = index + 1
                                            ),
                                        modifier = Modifier.fillMaxWidth(),
                                        style =
                                            KmiTypography.cardTitle.copy(
                                                fontWeight =
                                                    FontWeight.ExtraBold
                                            ),
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurface,
                                        textAlign = screenTextAlign,
                                        maxLines = 1,
                                        overflow =
                                            TextOverflow.Ellipsis
                                    )

                                    HorizontalDivider(
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .outline
                                                .copy(alpha = 0.18f)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {

                                        // חגורה
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment =
                                                Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🥋",
                                                style = KmiTypography.body
                                            )

                                            Text(
                                                text =
                                                    trainee.belt
                                                        .ifBlank { "—" },
                                                style =
                                                    KmiTypography.caption.copy(
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    ),
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurface,
                                                textAlign =
                                                    TextAlign.Center,
                                                maxLines = 1,
                                                overflow =
                                                    TextOverflow.Ellipsis
                                            )

                                            Text(
                                                text =
                                                    coachTr(
                                                        isEnglish,
                                                        "חגורה",
                                                        "Belt"
                                                    ),
                                                style =
                                                    KmiTypography.caption,
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant,
                                                textAlign =
                                                    TextAlign.Center
                                            )
                                        }

                                        // גיל
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment =
                                                Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🎂",
                                                style = KmiTypography.body
                                            )

                                            Text(
                                                text =
                                                    if (trainee.age > 0) {
                                                        trainee.age.toString()
                                                    } else {
                                                        "—"
                                                    },
                                                style =
                                                    KmiTypography.caption.copy(
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    ),
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurface,
                                                textAlign =
                                                    TextAlign.Center
                                            )

                                            Text(
                                                text =
                                                    coachTr(
                                                        isEnglish,
                                                        "גיל",
                                                        "Age"
                                                    ),
                                                style =
                                                    KmiTypography.caption,
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant,
                                                textAlign =
                                                    TextAlign.Center
                                            )
                                        }

                                        // ותק
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment =
                                                Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "🕒",
                                                style = KmiTypography.body
                                            )

                                            Text(
                                                text =
                                                    trainee.seniority
                                                        .ifBlank { "—" },
                                                style =
                                                    KmiTypography.caption.copy(
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    ),
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurface,
                                                textAlign =
                                                    TextAlign.Center,
                                                maxLines = 1,
                                                overflow =
                                                    TextOverflow.Ellipsis
                                            )

                                            Text(
                                                text =
                                                    coachTr(
                                                        isEnglish,
                                                        "ותק",
                                                        "Seniority"
                                                    ),
                                                style =
                                                    KmiTypography.caption,
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant,
                                                textAlign =
                                                    TextAlign.Center
                                            )
                                        }

                                        // נוכחות
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment =
                                                Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "📊",
                                                style = KmiTypography.body
                                            )

                                            Text(
                                                text =
                                                    if (
                                                        trainee.attendancePct > 0
                                                    ) {
                                                        "${trainee.attendancePct}%"
                                                    } else {
                                                        "—"
                                                    },
                                                style =
                                                    KmiTypography.caption.copy(
                                                        fontWeight =
                                                            FontWeight.Bold
                                                    ),
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurface,
                                                textAlign =
                                                    TextAlign.Center
                                            )

                                            Text(
                                                text =
                                                    coachTr(
                                                        isEnglish,
                                                        "נוכחות",
                                                        "Attendance"
                                                    ),
                                                style =
                                                    KmiTypography.caption,
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant,
                                                textAlign =
                                                    TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isTraineePickerExpanded) {
                    item {
                        Surface(
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surface,
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .outline
                                        .copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 8.dp
                                    ),
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp),
                                horizontalAlignment =
                                    screenHorizontalAlignment
                            ) {

                                // =====================================================
                                // בחירת סניף
                                // =====================================================

                                KmiPremiumDropdown(
                                    title =
                                        coachTr(
                                            isEnglish,
                                            "סניף",
                                            "Branch"
                                        ),
                                    options = availableBranches,
                                    selectedValue = effectiveBranch,
                                    isEnglish = isEnglish,
                                    placeholder =
                                        coachTr(
                                            isEnglish,
                                            "בחר סניף",
                                            "Select branch"
                                        ),
                                    enabled =
                                        availableBranches.size > 1,
                                    onSelected = { branchItem ->
                                        if (
                                            branchItem !=
                                            effectiveBranch
                                        ) {
                                            effectiveBranch =
                                                branchItem

                                            /*
                                             * החלפת סניף מאפסת
                                             * את הקבוצה ואת המתאמן,
                                             * משום שהם עלולים לא להיות
                                             * תקפים בסניף החדש.
                                             */
                                            effectiveGroupKey = ""
                                            availableGroups =
                                                emptyList()
                                            selectedId = null
                                            traineeSearchQuery = ""
                                            expandedCoachSection = null
                                        }
                                    }
                                )

                                // =====================================================
                                // בחירת קבוצה
                                // =====================================================

                                KmiPremiumDropdown(
                                    title =
                                        coachTr(
                                            isEnglish,
                                            "קבוצה",
                                            "Group"
                                        ),
                                    options = availableGroups,
                                    selectedValue =
                                        effectiveGroupKey,
                                    isEnglish = isEnglish,
                                    placeholder =
                                        coachTr(
                                            isEnglish,
                                            "בחר קבוצה",
                                            "Select group"
                                        ),
                                    enabled =
                                        availableGroups.size > 1,
                                    onSelected = { groupItem ->
                                        if (
                                            groupItem !=
                                            effectiveGroupKey
                                        ) {
                                            effectiveGroupKey =
                                                groupItem
                                            selectedId = null
                                            traineeSearchQuery = ""
                                            expandedCoachSection = null
                                        }
                                    }
                                )

// =====================================================
// בחירת מתאמן – Dropdown גלובלי
// =====================================================

                                val canChooseTrainee =
                                    effectiveBranch.isNotBlank() &&
                                            effectiveGroupKey.isNotBlank()

                                val traineeDropdownOptions =
                                    uiProfiles.mapIndexed { index, trainee ->
                                        demoSafeName(
                                            profile = trainee,
                                            demoIndex = index + 1
                                        )
                                    }

                                val selectedTraineeDisplayName =
                                    selected
                                        ?.let(::demoSafeName)
                                        .orEmpty()

                                KmiPremiumDropdown(
                                    title =
                                        coachTr(
                                            isEnglish,
                                            "מתאמן",
                                            "Trainee"
                                        ),
                                    options = traineeDropdownOptions,
                                    selectedValue = selectedTraineeDisplayName,
                                    isEnglish = isEnglish,
                                    placeholder =
                                        when {
                                            !canChooseTrainee ->
                                                coachTr(
                                                    isEnglish,
                                                    "בחר סניף וקבוצה",
                                                    "Select branch and group"
                                                )

                                            isProfilesLoading ||
                                                    isInitialServerSyncRunning ||
                                                    !didFinishInitialProfilesLoad ->
                                                coachTr(
                                                    isEnglish,
                                                    "טוען מתאמנים...",
                                                    "Loading trainees..."
                                                )

                                            else ->
                                                coachTr(
                                                    isEnglish,
                                                    "בחר מתאמן",
                                                    "Select trainee"
                                                )
                                        },
                                    enabled =
                                        canChooseTrainee &&
                                                !isProfilesLoading &&
                                                !isInitialServerSyncRunning &&
                                                didFinishInitialProfilesLoad &&
                                                traineeDropdownOptions.isNotEmpty(),
                                    onSelected = { selectedName ->

                                        uiProfiles
                                            .mapIndexed { index, trainee ->
                                                trainee to
                                                        demoSafeName(
                                                            profile = trainee,
                                                            demoIndex = index + 1
                                                        )
                                            }
                                            .firstOrNull { (_, displayName) ->
                                                displayName == selectedName
                                            }
                                            ?.first
                                            ?.let { trainee ->
                                                selectedId = trainee.id
                                                traineeSearchQuery = ""
                                                isTraineeMenuExpanded = false
                                                expandedCoachSection = null
                                            }
                                    }
                                )
                            }
                        }
                    }

                    /*
                     * הטעינה מוצגת מתחת לכרטיס הבחירה,
                     * כדי שכרטיס סניף / קבוצה / מתאמן
                     * יישאר תמיד באותו מיקום.
                     */
                    if (
                        isTraineePickerExpanded &&
                        (
                                isProfilesLoading ||
                                        isInitialServerSyncRunning ||
                                        !didFinishInitialProfilesLoad
                                )
                    ) {
                        item {
                            CoachTraineesPremiumLoading(
                                text =
                                    coachTr(
                                        isEnglish,
                                        "טוען את רשימת המתאמנים...",
                                        "Loading trainees..."
                                    )
                            )
                        }
                    }
                }

                    /*
                     * כרטיס פרטי המתאמן מוצג לאחר
                     * בחירת מתאמן ובתוך טאב בחירת מתאמן.
                     */
                    if (isTraineePickerExpanded && selected != null) {
                        item {
                            // כרטיס פרטי מתאמן
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp),
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        MaterialTheme.colorScheme.outline
                                            .copy(alpha = 0.28f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surface,
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                        .copy(alpha = 0.45f),
                                                    MaterialTheme.colorScheme.surface
                                                )
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 16.dp,
                                                vertical = 16.dp
                                            ),
                                        verticalArrangement =
                                            Arrangement.spacedBy(10.dp),
                                        horizontalAlignment =
                                            screenHorizontalAlignment
                                    ) {
                                        Text(
                                            text = demoSafeName(selected),
                                            style =
                                                KmiTypography.sectionTitle.copy(
                                                    fontWeight =
                                                        FontWeight.ExtraBold
                                                ),
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface,
                                            textAlign = screenTextAlign,
                                            maxLines = 2,
                                            overflow =
                                                TextOverflow.Ellipsis,
                                            modifier =
                                                Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text =
                                                coachTr(
                                                    isEnglish,
                                                    "פרופיל מתאמן",
                                                    "Trainee profile"
                                                ),
                                            style =
                                                KmiTypography.caption.copy(
                                                    fontWeight =
                                                        FontWeight.SemiBold
                                                ),
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant,
                                            textAlign = screenTextAlign,
                                            modifier =
                                                Modifier.fillMaxWidth()
                                        )

                                        HorizontalDivider(
                                            color =
                                                MaterialTheme.colorScheme.outline
                                                    .copy(alpha = 0.28f)
                                        )

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LabeledField(
                                                    label = coachTr(isEnglish, "גיל", "Age"),
                                                    value = if (selected.age > 0) "${selected.age}" else "—",
                                                    isEnglish = isEnglish,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                LabeledField(
                                                    label = coachTr(isEnglish, "ותק", "Seniority"),
                                                    value = selected.seniority.ifBlank { "—" },
                                                    isEnglish = isEnglish,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LabeledField(
                                                    label = coachTr(isEnglish, "דרגה", "Rank"),
                                                    value = coachBeltNameForUi(
                                                        selected.belt.ifBlank { "—" },
                                                        isEnglish
                                                    ),
                                                    isEnglish = isEnglish,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                LabeledField(
                                                    label = coachTr(isEnglish, "סניף", "Branch"),
                                                    value = selected.branch.ifBlank { "—" },
                                                    isEnglish = isEnglish,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LabeledField(
                                                    label = coachTr(isEnglish, "קבוצה", "Group"),
                                                    value = selected.groupKey.ifBlank { "—" },
                                                    isEnglish = isEnglish,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                LabeledField(
                                                    label = coachTr(
                                                        isEnglish,
                                                        "נוכחות",
                                                        "Attendance"
                                                    ),
                                                    value = if (selected.attendancePct > 0) "${selected.attendancePct}%" else "—",
                                                    isEnglish = isEnglish,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        HorizontalDivider(
                                            color =
                                                MaterialTheme.colorScheme.outline
                                                    .copy(alpha = 0.28f)
                                        )

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val isBeltDatesSectionExpanded =
                                                expandedCoachSection == beltDatesSectionKey

                                            PremiumCoachCompactSectionHeader(
                                                title = coachTr(
                                                    isEnglish,
                                                    "תאריכי קבלת חגורות",
                                                    "Belt award dates"
                                                ),
                                                subtitle = if (isBeltDatesSectionExpanded) {
                                                    coachTr(
                                                        isEnglish,
                                                        "עדכון תאריכים לפי חגורה",
                                                        "Update dates by belt"
                                                    )
                                                } else {
                                                    coachTr(
                                                        isEnglish,
                                                        "לחצו לפתיחת רשימת החגורות",
                                                        "Tap to open the belt list"
                                                    )
                                                },
                                                iconText = "📅",
                                                isExpanded = isBeltDatesSectionExpanded,
                                                accent = MaterialTheme.colorScheme.primary,
                                                isEnglish = isEnglish,
                                                onClick = {
                                                    expandedCoachSection =
                                                        if (expandedCoachSection == beltDatesSectionKey) {
                                                            null
                                                        } else {
                                                            beltDatesSectionKey
                                                        }
                                                }
                                            )

                                            if (isBeltDatesSectionExpanded) {
                                                val beltOrder = listOf(
                                                    "צהובה",
                                                    "כתומה",
                                                    "ירוקה",
                                                    "כחולה",
                                                    "חומה",
                                                    "שחורה"
                                                )

                                                val beltAccentMap = mapOf(
                                                    "צהובה" to Color(0xFFFACC15),
                                                    "כתומה" to Color(0xFFF97316),
                                                    "ירוקה" to Color(0xFF22C55E),
                                                    "כחולה" to Color(0xFF3B82F6),
                                                    "חומה" to Color(0xFF8B5A2B),
                                                    "שחורה" to Color(0xFF111111)
                                                )

                                                val selectedDates =
                                                    beltAwardDatesState[selected.id] ?: emptyMap()
                                                var expandedBelt by remember(selected.id) {
                                                    mutableStateOf<String?>(
                                                        null
                                                    )
                                                }

                                                beltOrder.forEach { beltName ->
                                                    val beltAccent =
                                                        beltAccentMap[beltName]
                                                            ?: MaterialTheme.colorScheme.primary
                                                    val currentDate =
                                                        selectedDates[beltName].orEmpty()
                                                    val hasDate = currentDate.isNotBlank()
                                                    val isExpanded = expandedBelt == beltName
                                                    val selectedDescriptions =
                                                        beltAwardDescriptionsState[selected.id]
                                                            ?: emptyMap()

                                                    val currentDescription =
                                                        selectedDescriptions[beltName].orEmpty()

                                                    Surface(
                                                        color =
                                                            MaterialTheme
                                                                .colorScheme
                                                                .surface,
                                                        shape = RoundedCornerShape(16.dp),
                                                        shadowElevation = 0.dp,
                                                        tonalElevation = 0.dp,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            beltAccent.copy(alpha = 0.24f)
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                expandedBelt =
                                                                    if (isExpanded) null else beltName
                                                            }
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(
                                                                    horizontal = 10.dp,
                                                                    vertical = 8.dp
                                                                ),
                                                            verticalArrangement = Arrangement.spacedBy(
                                                                6.dp
                                                            )
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(10.dp)
                                                                        .clip(CircleShape)
                                                                        .background(beltAccent)
                                                                )

                                                                Spacer(Modifier.width(8.dp))

                                                                Column(
                                                                    modifier = Modifier.weight(1f),
                                                                    verticalArrangement = Arrangement.spacedBy(
                                                                        2.dp
                                                                    )
                                                                ) {
                                                                    Text(
                                                                        text =
                                                                            if (isEnglish) {
                                                                                coachBeltNameForUi(
                                                                                    beltName,
                                                                                    true
                                                                                )
                                                                            } else {
                                                                                "חגורה $beltName"
                                                                            },
                                                                        style =
                                                                            KmiTypography.secondary.copy(
                                                                                fontWeight =
                                                                                    FontWeight.Bold
                                                                            ),
                                                                        color =
                                                                            MaterialTheme
                                                                                .colorScheme
                                                                                .onSurface
                                                                    )

                                                                    Text(
                                                                        text =
                                                                            if (hasDate) {
                                                                                coachTr(
                                                                                    isEnglish,
                                                                                    "תאריך קבלה: $currentDate",
                                                                                    "Award date: $currentDate"
                                                                                )
                                                                            } else {
                                                                                coachTr(
                                                                                    isEnglish,
                                                                                    "אין תאריך קבלה",
                                                                                    "No award date"
                                                                                )
                                                                            },
                                                                        style =
                                                                            KmiTypography.caption,
                                                                        color =
                                                                            if (hasDate) {
                                                                                MaterialTheme
                                                                                    .colorScheme
                                                                                    .primary
                                                                            } else {
                                                                                MaterialTheme
                                                                                    .colorScheme
                                                                                    .onSurfaceVariant
                                                                            }
                                                                    )
                                                                }

                                                                Icon(
                                                                    imageVector =
                                                                        if (hasDate) {
                                                                            Icons.Default.CheckCircle
                                                                        } else {
                                                                            Icons.Default.Cancel
                                                                        },
                                                                    contentDescription = null,
                                                                    tint =
                                                                        if (hasDate) {
                                                                            MaterialTheme
                                                                                .colorScheme
                                                                                .primary
                                                                        } else {
                                                                            MaterialTheme
                                                                                .colorScheme
                                                                                .error
                                                                        },
                                                                    modifier =
                                                                        Modifier.size(
                                                                            KmiIconSize.small
                                                                        )
                                                                )

                                                                Spacer(Modifier.width(6.dp))

                                                                Icon(
                                                                    imageVector =
                                                                        if (isExpanded) {
                                                                            Icons.Default.KeyboardArrowUp
                                                                        } else {
                                                                            Icons.Default.KeyboardArrowDown
                                                                        },
                                                                    contentDescription = null,
                                                                    tint =
                                                                        MaterialTheme
                                                                            .colorScheme
                                                                            .onSurfaceVariant,
                                                                    modifier =
                                                                        Modifier.size(
                                                                            KmiIconSize.small
                                                                        )
                                                                )
                                                            }

                                                            if (isExpanded) {
                                                                var showBeltDatePicker by remember(
                                                                    selected.id,
                                                                    beltName
                                                                ) {
                                                                    mutableStateOf(false)
                                                                }

                                                                PremiumCoachDateField(
                                                                    label = coachTr(
                                                                        isEnglish,
                                                                        "תאריך קבלה",
                                                                        "Award date"
                                                                    ),
                                                                    value = currentDate,
                                                                    placeholder = coachTr(
                                                                        isEnglish,
                                                                        "בחר תאריך מלוח השנה",
                                                                        "Choose a date from calendar"
                                                                    ),
                                                                    accent = beltAccent,
                                                                    isEnglish = isEnglish,
                                                                    onClick = {
                                                                        showBeltDatePicker = true
                                                                    }
                                                                )

                                                                if (showBeltDatePicker) {
                                                                    PremiumCoachDatePickerDialog(
                                                                        title = coachTr(
                                                                            isEnglish,
                                                                            "בחר תאריך קבלת חגורה",
                                                                            "Choose belt award date"
                                                                        ),
                                                                        selectedDate = currentDate,
                                                                        isEnglish = isEnglish,
                                                                        onDismiss = {
                                                                            showBeltDatePicker =
                                                                                false
                                                                        },
                                                                        onDateSelected = { newDate ->
                                                                            val current =
                                                                                beltAwardDatesState[selected.id]
                                                                                    .orEmpty()
                                                                                    .toMutableMap()

                                                                            current[beltName] =
                                                                                newDate
                                                                            beltAwardDatesState[selected.id] =
                                                                                current
                                                                        }
                                                                    )
                                                                }

                                                                OutlinedTextField(
                                                                    value = currentDescription,
                                                                    onValueChange = { newValue ->
                                                                        val current =
                                                                            beltAwardDescriptionsState[selected.id]
                                                                                .orEmpty()
                                                                                .toMutableMap()

                                                                        current[beltName] = newValue
                                                                        beltAwardDescriptionsState[selected.id] =
                                                                            current
                                                                    },
                                                                    label = {
                                                                        Text(
                                                                            coachTr(
                                                                                isEnglish,
                                                                                "תיאור",
                                                                                "Description"
                                                                            )
                                                                        )
                                                                    },
                                                                    placeholder = {
                                                                        Text(
                                                                            coachTr(
                                                                                isEnglish,
                                                                                "לדוגמה: מבחן חגורה, הערת מאמן, הערכה מיוחדת",
                                                                                "Example: belt test, coach note, special remark"
                                                                            ),
                                                                            textAlign = coachTextAlign(
                                                                                isEnglish
                                                                            ),
                                                                            modifier = Modifier.fillMaxWidth()
                                                                        )
                                                                    },
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    minLines = 2,
                                                                    textStyle =
                                                                        KmiTypography.body.copy(
                                                                            textAlign =
                                                                                coachTextAlign(
                                                                                    isEnglish
                                                                                )
                                                                        ),
                                                                    shape = RoundedCornerShape(16.dp)
                                                                )
                                                            }
                                                        }

                                                    }
                                                }

                                                var isSavingBeltDates by remember(selected.id) {
                                                    mutableStateOf(
                                                        false
                                                    )
                                                }
                                                var beltDatesSaveMessage by remember(selected.id) {
                                                    mutableStateOf<String?>(
                                                        null
                                                    )
                                                }

                                                Surface(
                                                    onClick = {
                                                        if (!isSavingBeltDates) {
                                                            val datesToSave =
                                                                beltAwardDatesState[selected.id]
                                                                    .orEmpty()
                                                                    .filterValues {
                                                                        it.isNotBlank()
                                                                    }

                                                            val descriptionsToSave =
                                                                beltAwardDescriptionsState[selected.id]
                                                                    .orEmpty()
                                                                    .filterValues {
                                                                        it.isNotBlank()
                                                                    }

                                                            if (datesToSave.isEmpty() && descriptionsToSave.isEmpty()) {
                                                                beltDatesSaveMessage = coachTr(
                                                                    isEnglish,
                                                                    "אין תאריכים או תיאורים לשמירה",
                                                                    "No dates or descriptions to save"
                                                                )
                                                                return@Surface
                                                            }

                                                            screenScope.launch {
                                                                isSavingBeltDates = true
                                                                beltDatesSaveMessage = null

                                                                runCatching {
                                                                    saveBeltAwardDatesForSelected(
                                                                        selectedProfile = selected,
                                                                        dates = datesToSave,
                                                                        descriptions = descriptionsToSave
                                                                    )
                                                                }.onSuccess {
                                                                    beltDatesSaveMessage =
                                                                        coachTr(
                                                                            isEnglish,
                                                                            "תאריכי החגורות נשמרו",
                                                                            "Belt dates saved"
                                                                        )
                                                                }.onFailure {
                                                                    beltDatesSaveMessage =
                                                                        coachTr(
                                                                            isEnglish,
                                                                            "שמירת תאריכי החגורות נכשלה",
                                                                            "Failed to save belt dates"
                                                                        )
                                                                }

                                                                isSavingBeltDates = false
                                                            }
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(18.dp),
                                                    color = Color.Transparent,
                                                    shadowElevation = 0.dp,
                                                    tonalElevation = 0.dp,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(
                                                                brush = Brush.horizontalGradient(
                                                                    colors = listOf(
                                                                        Color(0xFF7C3AED),
                                                                        Color(0xFF6366F1),
                                                                        Color(0xFF0EA5E9)
                                                                    )
                                                                ),
                                                                shape = RoundedCornerShape(18.dp)
                                                            )
                                                            .padding(vertical = 14.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text =
                                                                if (isSavingBeltDates) {
                                                                    coachTr(
                                                                        isEnglish,
                                                                        "שומר...",
                                                                        "Saving..."
                                                                    )
                                                                } else {
                                                                    coachTr(
                                                                        isEnglish,
                                                                        "שמור תאריכי חגורות",
                                                                        "Save belt dates"
                                                                    )
                                                                },
                                                            style =
                                                                KmiTypography.action.copy(
                                                                    fontWeight =
                                                                        FontWeight.ExtraBold
                                                                ),
                                                            color = Color.White,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }

                                                beltDatesSaveMessage?.let { msg ->
                                                    val isSuccess =
                                                        msg.contains("נשמרו") ||
                                                                msg.contains(
                                                                    "saved",
                                                                    ignoreCase = true
                                                                )

                                                    Surface(
                                                        color =
                                                            if (isSuccess) {
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .primaryContainer
                                                            } else {
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .errorContainer
                                                            },
                                                        shape = RoundedCornerShape(14.dp),
                                                        tonalElevation = 0.dp,
                                                        shadowElevation = 0.dp,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = msg,
                                                            style =
                                                                KmiTypography.caption.copy(
                                                                    fontWeight =
                                                                        FontWeight.Bold
                                                                ),
                                                            color =
                                                                if (isSuccess) {
                                                                    MaterialTheme
                                                                        .colorScheme
                                                                        .onPrimaryContainer
                                                                } else {
                                                                    MaterialTheme
                                                                        .colorScheme
                                                                        .onErrorContainer
                                                                },
                                                            textAlign =
                                                                coachTextAlign(
                                                                    isEnglish
                                                                ),
                                                            modifier =
                                                                Modifier.padding(
                                                                    horizontal = 12.dp,
                                                                    vertical = 10.dp
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        CoachDateSectionCard(
                                            title = "השתלמויות",
                                            collapsedSubtitle = coachTr(
                                                isEnglish,
                                                "לחצו לפתיחת רשימת השתלמויות",
                                                "Tap to open the seminar list"
                                            ),
                                            expandedSubtitle = coachTr(
                                                isEnglish,
                                                "הוסיפו תאריך ותיאור להשתלמויות שהמתאמן עבר",
                                                "Add a date and description for seminars completed by the trainee"
                                            ),
                                            defaultItems = listOf(
                                                "השתלמות 1",
                                                "השתלמות 2",
                                                "השתלמות 3"
                                            ),
                                            selectedId = selected.id,
                                            stateMap = seminarDatesState,
                                            firestoreFieldName = "seminarDates",
                                            selectedProfile = selected,
                                            screenScope = screenScope,
                                            isEnglish = isEnglish,
                                            isExpanded = expandedCoachSection == seminarsSectionKey,
                                            onToggleExpanded = {
                                                expandedCoachSection =
                                                    if (expandedCoachSection == seminarsSectionKey) {
                                                        null
                                                    } else {
                                                        seminarsSectionKey
                                                    }
                                            },
                                            onSave = ::saveCoachDateSectionForSelected
                                        )

                                        CoachDateSectionCard(
                                            title = "מחנות אימונים",
                                            collapsedSubtitle = coachTr(
                                                isEnglish,
                                                "לחצו לפתיחת רשימת מחנות אימונים",
                                                "Tap to open the training camp list"
                                            ),
                                            expandedSubtitle = coachTr(
                                                isEnglish,
                                                "הוסיפו תאריך ותיאור למחנות אימונים שבהם המתאמן השתתף",
                                                "Add a date and description for training camps attended by the trainee"
                                            ),
                                            defaultItems = listOf(
                                                "מחנה אימונים 1",
                                                "מחנה אימונים 2",
                                                "מחנה אימונים 3"
                                            ),
                                            selectedId = selected.id,
                                            stateMap = campDatesState,
                                            firestoreFieldName = "campDates",
                                            selectedProfile = selected,
                                            screenScope = screenScope,
                                            isEnglish = isEnglish,
                                            isExpanded = expandedCoachSection == campsSectionKey,
                                            onToggleExpanded = {
                                                expandedCoachSection =
                                                    if (expandedCoachSection == campsSectionKey) {
                                                        null
                                                    } else {
                                                        campsSectionKey
                                                    }
                                            },
                                            onSave = ::saveCoachDateSectionForSelected
                                        )

                                        CoachDateSectionCard(
                                            title = "הסמכות",
                                            collapsedSubtitle = coachTr(
                                                isEnglish,
                                                "לחצו לפתיחת רשימת הסמכות",
                                                "Tap to open the certification list"
                                            ),
                                            expandedSubtitle = coachTr(
                                                isEnglish,
                                                "הוסיפו תאריך ותיאור להסמכות שהמתאמן קיבל",
                                                "Add a date and description for certifications received by the trainee"
                                            ),
                                            defaultItems = listOf(
                                                "הסמכה 1",
                                                "הסמכה 2",
                                                "הסמכה 3"
                                            ),
                                            selectedId = selected.id,
                                            stateMap = certificationDatesState,
                                            firestoreFieldName = "certificationDates",
                                            selectedProfile = selected,
                                            screenScope = screenScope,
                                            isEnglish = isEnglish,
                                            isExpanded = expandedCoachSection == certificationsSectionKey,
                                            onToggleExpanded = {
                                                expandedCoachSection =
                                                    if (expandedCoachSection == certificationsSectionKey) {
                                                        null
                                                    } else {
                                                        certificationsSectionKey
                                                    }
                                            },
                                            onSave = ::saveCoachDateSectionForSelected
                                        )

                                        var isSavingCoachNotes by remember(selected.id) {
                                            mutableStateOf(false)
                                        }

                                        var coachNotesSaveMessage by remember(selected.id) {
                                            mutableStateOf<String?>(null)
                                        }

                                        val coachNotesContainerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .surface

                                        val coachNotesBorderColor =
                                            MaterialTheme
                                                .colorScheme
                                                .outline
                                                .copy(alpha = 0.55f)

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            horizontalAlignment = screenHorizontalAlignment
                                        ) {
                                            Surface(
                                                color = coachNotesContainerColor,
                                                shape = RoundedCornerShape(20.dp),
                                                shadowElevation = 0.dp,
                                                tonalElevation = 0.dp,
                                                border = BorderStroke(
                                                    1.dp,
                                                    coachNotesBorderColor
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            horizontal = 12.dp,
                                                            vertical = 12.dp
                                                        ),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    horizontalAlignment = screenHorizontalAlignment
                                                ) {
                                                    Text(
                                                        text =
                                                            coachTr(
                                                                isEnglish,
                                                                "הערות מאמן",
                                                                "Coach notes"
                                                            ),
                                                        style =
                                                            KmiTypography.caption.copy(
                                                                fontWeight =
                                                                    FontWeight.ExtraBold
                                                            ),
                                                        color =
                                                            MaterialTheme
                                                                .colorScheme
                                                                .onSurfaceVariant,
                                                        textAlign =
                                                            screenTextAlign,
                                                        modifier =
                                                            Modifier.fillMaxWidth()
                                                    )

                                                    OutlinedTextField(
                                                        value = coachNotes[selected.id] ?: "",
                                                        onValueChange = {
                                                            coachNotes[selected.id] = it
                                                            coachNotesSaveMessage = null
                                                        },
                                                        placeholder = {
                                                            Text(
                                                                text = coachTr(
                                                                    isEnglish,
                                                                    "כתוב כאן הערות על המתאמן...",
                                                                    "Write coach notes here..."
                                                                ),
                                                                color =
                                                                    MaterialTheme
                                                                        .colorScheme
                                                                        .onSurfaceVariant,
                                                                style =
                                                                    KmiTypography.body.copy(
                                                                        textAlign =
                                                                            screenTextAlign
                                                                    ),
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        },
                                                        textStyle =
                                                            KmiTypography.body.copy(
                                                                color =
                                                                    MaterialTheme
                                                                        .colorScheme
                                                                        .onSurface,
                                                                textAlign =
                                                                    screenTextAlign
                                                            ),
                                                        modifier =
                                                            Modifier.fillMaxWidth(),
                                                        minLines = 4,
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedTextColor =
                                                                MaterialTheme.colorScheme.onSurface,
                                                            unfocusedTextColor =
                                                                MaterialTheme.colorScheme.onSurface,
                                                            disabledTextColor =
                                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                                            cursorColor =
                                                                MaterialTheme.colorScheme.primary,
                                                            focusedBorderColor =
                                                                MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor =
                                                                coachNotesBorderColor,
                                                            focusedContainerColor =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .surface,
                                                            unfocusedContainerColor =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .surface,
                                                            disabledContainerColor =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .surfaceVariant,
                                                            focusedPlaceholderColor =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .onSurfaceVariant,
                                                            unfocusedPlaceholderColor =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .onSurfaceVariant
                                                        )
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = if (isEnglish) {
                                                    Arrangement.Start
                                                } else {
                                                    Arrangement.End
                                                }
                                            ) {
                                                Surface(
                                                    onClick = {
                                                        if (!isSavingCoachNotes) {
                                                            val noteToSave =
                                                                coachNotes[selected.id]
                                                                    .orEmpty()

                                                            screenScope.launch {
                                                                isSavingCoachNotes = true
                                                                coachNotesSaveMessage = null

                                                                runCatching {
                                                                    saveCoachNotesForSelected(
                                                                        selectedProfile = selected,
                                                                        note = noteToSave
                                                                    )
                                                                }.onSuccess {
                                                                    coachNotesSaveMessage = coachTr(
                                                                        isEnglish,
                                                                        "הערות המאמן נשמרו",
                                                                        "Coach notes saved"
                                                                    )
                                                                }.onFailure {
                                                                    coachNotesSaveMessage = coachTr(
                                                                        isEnglish,
                                                                        "שמירת הערות המאמן נכשלה",
                                                                        "Failed to save coach notes"
                                                                    )
                                                                }

                                                                isSavingCoachNotes = false
                                                            }
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(999.dp),
                                                    color =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary,
                                                    shadowElevation = 0.dp,
                                                    tonalElevation = 0.dp,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary
                                                            .copy(alpha = 0.75f)
                                                    ),
                                                    modifier = Modifier.widthIn(min = 118.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 7.dp
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text =
                                                                if (
                                                                    isSavingCoachNotes
                                                                ) {
                                                                    coachTr(
                                                                        isEnglish,
                                                                        "שומר...",
                                                                        "Saving..."
                                                                    )
                                                                } else {
                                                                    coachTr(
                                                                        isEnglish,
                                                                        "שמור",
                                                                        "Save"
                                                                    )
                                                                },
                                                            style =
                                                                KmiTypography.action.copy(
                                                                    fontWeight =
                                                                        FontWeight.Bold
                                                                ),
                                                            color =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .onPrimary,
                                                            textAlign =
                                                                TextAlign.Center,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        coachNotesSaveMessage?.let { msg ->
                                            val isSuccess =
                                                msg.contains("נשמרו") ||
                                                        msg.contains(
                                                            "saved",
                                                            ignoreCase = true
                                                        )

                                            Surface(
                                                color =
                                                    if (isSuccess) {
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primaryContainer
                                                    } else {
                                                        MaterialTheme
                                                            .colorScheme
                                                            .errorContainer
                                                    },
                                                shape =
                                                    RoundedCornerShape(
                                                        14.dp
                                                    ),
                                                tonalElevation = 0.dp,
                                                shadowElevation = 0.dp,
                                                modifier =
                                                    Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = msg,
                                                    style =
                                                        KmiTypography.caption.copy(
                                                            fontWeight =
                                                                FontWeight.Bold
                                                        ),
                                                    color =
                                                        if (isSuccess) {
                                                            MaterialTheme
                                                                .colorScheme
                                                                .onPrimaryContainer
                                                        } else {
                                                            MaterialTheme
                                                                .colorScheme
                                                                .onErrorContainer
                                                        },
                                                    textAlign =
                                                        screenTextAlign,
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                horizontal = 12.dp,
                                                                vertical = 10.dp
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

            if (showStatsSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundBrush)
                ) {
                    CoachGroupStatsPremiumScreen(
                        stats = groupStats,
                        profiles =
                            traineeProfiles.mapIndexed { index,
                                                         profile ->

                                profile.copy(
                                    fullName =
                                        demoSafeName(
                                            profile = profile,
                                            demoIndex = index + 1
                                        )
                                )
                            },
                        isEnglish = isEnglish,
                        shareTrigger = statisticsShareTrigger,

                        onShareGroupStatistics = {

                            if (
                                effectiveBranchPrimary
                                    .isNotBlank() &&
                                effectiveGroupKey
                                    .isNotBlank() &&
                                traineeProfiles
                                    .isNotEmpty()
                            ) {
                                val demoSafePdfProfiles =
                                    traineeProfiles.mapIndexed { index,
                                                                 profile ->
                                        profile.copy(
                                            fullName =
                                                demoSafeName(
                                                    profile = profile,
                                                    demoIndex = index + 1
                                                )
                                        )
                                    }

                                val pdfFile =
                                    createCoachGroupStatsPdf(
                                        context = ctx,
                                        stats = groupStats,
                                        profiles = demoSafePdfProfiles,
                                        branch =
                                            effectiveBranchPrimary,
                                        groupKey =
                                            effectiveGroupKey,
                                        isEnglish =
                                            isEnglish
                                    )

                                val uri =
                                    FileProvider.getUriForFile(
                                        ctx,
                                        "${ctx.packageName}.fileprovider",
                                        pdfFile
                                    )

                                val sendIntent =
                                    Intent(
                                        Intent.ACTION_SEND
                                    ).apply {

                                        type =
                                            "application/pdf"

                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            coachTr(
                                                isEnglish,
                                                "דו״ח סטטיסטיקת הקבוצה",
                                                "Group statistics report"
                                            )
                                        )

                                        putExtra(
                                            Intent.EXTRA_STREAM,
                                            uri
                                        )

                                        addFlags(
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }

                                ctx.startActivity(
                                    Intent.createChooser(
                                        sendIntent,
                                        coachTr(
                                            isEnglish,
                                            "שיתוף PDF",
                                            "Share PDF"
                                        )
                                    )
                                )
                            }
                        },

                        onClose = {
                            showStatsSheet = false
                        },
                        onOpenDrawer = onOpenDrawer,
                        onOpenHome = onOpenHome
                    )
                }
            }
        }
    }

    @Composable
    private fun PremiumCoachCompactSectionHeader(
        title: String,
        subtitle: String,
        iconText: String,
        isExpanded: Boolean,
        accent: Color,
        isEnglish: Boolean,
        onClick: () -> Unit
    ) {
        val headerColor =
            MaterialTheme.colorScheme.surface

        val headerBorderColor =
            MaterialTheme.colorScheme.outline
                .copy(alpha = 0.28f)

        Surface(
            onClick = onClick,
            color = headerColor,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                headerBorderColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant
                                    .copy(alpha = 0.42f),
                                accent.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.08f),
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(
                        KmiIconSize.medium
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iconText,
                            style =
                                KmiTypography.action.copy(
                                    fontWeight =
                                        FontWeight.Bold
                                ),
                            color = accent,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.width(7.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = coachHorizontalAlignment(isEnglish),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign =
                            coachTextAlign(isEnglish),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style =
                            KmiTypography.cardTitle.copy(
                                fontWeight =
                                    FontWeight.Black,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                            )
                    )

                    Text(
                        text = subtitle,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign =
                            coachTextAlign(isEnglish),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style =
                            KmiTypography.caption.copy(
                                fontWeight =
                                    FontWeight.Medium,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                    )
                }

                Spacer(Modifier.width(5.dp))

                Icon(
                    imageVector =
                        if (isExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                    contentDescription = null,
                    tint = accent,
                    modifier =
                        Modifier.size(
                            KmiIconSize.small
                        )
                )
            }
        }
    }

    @Composable
    private fun CoachDateSectionCard(
        title: String,
        collapsedSubtitle: String,
        expandedSubtitle: String,
        defaultItems: List<String>,
        selectedId: String,
        stateMap: MutableMap<String, Map<String, CoachDateEntry>>,
        firestoreFieldName: String,
        selectedProfile: TraineeProfile,
        screenScope: kotlinx.coroutines.CoroutineScope,
        isEnglish: Boolean,
        isExpanded: Boolean,
        onToggleExpanded: () -> Unit,
        onSave: suspend (
            selectedProfile: TraineeProfile,
            firestoreFieldName: String,
            entries: Map<String, CoachDateEntry>
        ) -> Unit
    ) {
        var expandedItem by remember(selectedId, title) { mutableStateOf<String?>(null) }
        var isSaving by remember(selectedId, title) { mutableStateOf(false) }
        var saveMessage by remember(selectedId, title) { mutableStateOf<String?>(null) }

        val selectedEntries = stateMap[selectedId].orEmpty()

        val dynamicItems = remember(selectedEntries, defaultItems, title) {
            val mergedItems = (defaultItems + selectedEntries.keys)
                .distinct()
                .toMutableList()

            val lastItemName = mergedItems.lastOrNull()
            val lastEntry = lastItemName?.let { selectedEntries[it] }

            val shouldAddNextRow =
                lastItemName != null &&
                        lastEntry != null &&
                        (
                                lastEntry.date.isNotBlank() ||
                                        lastEntry.description.isNotBlank()
                                )

            if (shouldAddNextRow) {
                mergedItems += nextCoachDateItemName(title, mergedItems.size + 1)
            }

            mergedItems
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val sectionIcon = coachDateSectionIcon(title)
            val sectionAccent = coachDateSectionAccent(title)
            val sectionTitleUi = coachSectionTitleForUi(title, isEnglish)

            PremiumCoachCompactSectionHeader(
                title = sectionTitleUi,
                subtitle = if (isExpanded) expandedSubtitle else collapsedSubtitle,
                iconText = sectionIcon,
                isExpanded = isExpanded,
                accent = sectionAccent,
                isEnglish = isEnglish,
                onClick = {
                    expandedItem = null
                    onToggleExpanded()
                }
            )

            if (isExpanded) {
                dynamicItems.forEach { itemName ->
                    val currentEntry = selectedEntries[itemName] ?: CoachDateEntry()
                    val hasContent =
                        currentEntry.date.isNotBlank() ||
                                currentEntry.description.isNotBlank()

                    val isItemExpanded = expandedItem == itemName
                    val accent =
                        MaterialTheme.colorScheme.primary

                    Surface(
                        color =
                            MaterialTheme
                                .colorScheme
                                .surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            1.dp,
                            accent.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedItem =
                                    if (isItemExpanded) {
                                        null
                                    } else {
                                        itemName
                                    }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )

                                Spacer(Modifier.width(8.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text =
                                            coachDateItemNameForUi(
                                                itemName,
                                                isEnglish
                                            ),
                                        style =
                                            KmiTypography.secondary.copy(
                                                fontWeight =
                                                    FontWeight.Bold
                                            ),
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurface,
                                        textAlign =
                                            coachTextAlign(
                                                isEnglish
                                            ),
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text =
                                            when {
                                                currentEntry.date.isNotBlank() &&
                                                        currentEntry.description.isNotBlank() ->
                                                    coachTr(
                                                        isEnglish,
                                                        "תאריך: ${currentEntry.date} • ${currentEntry.description}",
                                                        "Date: ${currentEntry.date} • ${currentEntry.description}"
                                                    )

                                                currentEntry.date.isNotBlank() ->
                                                    coachTr(
                                                        isEnglish,
                                                        "תאריך: ${currentEntry.date}",
                                                        "Date: ${currentEntry.date}"
                                                    )

                                                currentEntry.description.isNotBlank() ->
                                                    currentEntry.description

                                                else ->
                                                    coachTr(
                                                        isEnglish,
                                                        "אין מידע",
                                                        "No information"
                                                    )
                                            },
                                        style =
                                            KmiTypography.caption,
                                        color =
                                            if (hasContent) {
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                            } else {
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                            },
                                        textAlign =
                                            coachTextAlign(
                                                isEnglish
                                            ),
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )
                                }

                                Icon(
                                    imageVector =
                                        if (hasContent) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.Cancel
                                        },
                                    contentDescription = null,
                                    tint =
                                        if (hasContent) {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .error
                                        },
                                    modifier =
                                        Modifier.size(
                                            KmiIconSize.small
                                        )
                                )

                                Spacer(Modifier.width(6.dp))

                                Icon(
                                    imageVector =
                                        if (isItemExpanded) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                    contentDescription = null,
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                    modifier =
                                        Modifier.size(
                                            KmiIconSize.small
                                        )
                                )
                            }

                            if (isItemExpanded) {
                                var showItemDatePicker by remember(selectedId, title, itemName) {
                                    mutableStateOf(false)
                                }

                                PremiumCoachDateField(
                                    label = coachTr(isEnglish, "תאריך", "Date"),
                                    value = currentEntry.date,
                                    placeholder = coachTr(
                                        isEnglish,
                                        "בחר תאריך מלוח השנה",
                                        "Choose a date from calendar"
                                    ),
                                    accent = sectionAccent,
                                    isEnglish = isEnglish,
                                    onClick = {
                                        showItemDatePicker = true
                                    }
                                )

                                if (showItemDatePicker) {
                                    PremiumCoachDatePickerDialog(
                                        title = coachTr(
                                            isEnglish,
                                            "בחר תאריך עבור ${
                                                coachDateItemNameForUi(
                                                    itemName,
                                                    false
                                                )
                                            }",
                                            "Choose date for ${
                                                coachDateItemNameForUi(
                                                    itemName,
                                                    true
                                                )
                                            }"
                                        ),
                                        selectedDate = currentEntry.date,
                                        isEnglish = isEnglish,
                                        onDismiss = {
                                            showItemDatePicker = false
                                        },
                                        onDateSelected = { newDate ->
                                            val current = stateMap[selectedId]
                                                .orEmpty()
                                                .toMutableMap()

                                            val oldEntry = current[itemName] ?: CoachDateEntry()
                                            current[itemName] = oldEntry.copy(date = newDate)
                                            stateMap[selectedId] = current
                                        }
                                    )
                                }

                                OutlinedTextField(
                                    value = currentEntry.description,
                                    onValueChange = { newValue ->
                                        val current = stateMap[selectedId]
                                            .orEmpty()
                                            .toMutableMap()

                                        val oldEntry =
                                            current[itemName]
                                                ?: CoachDateEntry()

                                        current[itemName] =
                                            oldEntry.copy(
                                                description = newValue
                                            )

                                        stateMap[selectedId] = current
                                    },
                                    label = {
                                        Text(
                                            text =
                                                coachTr(
                                                    isEnglish,
                                                    "תיאור",
                                                    "Description"
                                                ),
                                            style =
                                                KmiTypography.caption
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            text =
                                                coachTr(
                                                    isEnglish,
                                                    "לדוגמה: השתלמות מדריכים / מחנה קיץ / הסמכת עוזר מדריך",
                                                    "Example: instructor seminar / summer camp / assistant instructor certification"
                                                ),
                                            style =
                                                KmiTypography.caption,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant,
                                            textAlign =
                                                coachTextAlign(
                                                    isEnglish
                                                ),
                                            modifier =
                                                Modifier.fillMaxWidth()
                                        )
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    textStyle =
                                        KmiTypography.body.copy(
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface,
                                            textAlign =
                                                coachTextAlign(
                                                    isEnglish
                                                )
                                        ),
                                    shape =
                                        RoundedCornerShape(16.dp),
                                    colors =
                                        OutlinedTextFieldDefaults.colors(
                                            focusedTextColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface,
                                            unfocusedTextColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface,
                                            cursorColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary,
                                            focusedBorderColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary,
                                            unfocusedBorderColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .outline,
                                            focusedContainerColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .surface,
                                            unfocusedContainerColor =
                                                MaterialTheme
                                                    .colorScheme
                                                    .surface
                                        )
                                )
                            }
                        }
                    }
                }

                Surface(
                    onClick = {
                        if (!isSaving) {
                            val entriesToSave = stateMap[selectedId].orEmpty()
                                .filterValues { entry ->
                                    entry.date.isNotBlank() || entry.description.isNotBlank()
                                }

                            if (entriesToSave.isEmpty()) {
                                saveMessage =
                                    coachTr(isEnglish, "אין נתונים לשמירה", "No data to save")
                                return@Surface
                            }

                            screenScope.launch {
                                isSaving = true
                                saveMessage = null

                                runCatching {
                                    onSave(
                                        selectedProfile,
                                        firestoreFieldName,
                                        entriesToSave
                                    )
                                }.onSuccess {
                                    saveMessage = coachTr(
                                        isEnglish,
                                        "${coachSectionTitleForUi(title, false)} נשמרו",
                                        "${coachSectionTitleForUi(title, true)} saved"
                                    )
                                }.onFailure {
                                    saveMessage = coachTr(
                                        isEnglish,
                                        "שמירת ${coachSectionTitleForUi(title, false)} נכשלה",
                                        "Failed to save ${coachSectionTitleForUi(title, true)}"
                                    )
                                }

                                isSaving = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF7C3AED),
                                        Color(0xFF6366F1),
                                        Color(0xFF0EA5E9)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                if (isSaving) {
                                    coachTr(
                                        isEnglish,
                                        "שומר...",
                                        "Saving..."
                                    )
                                } else {
                                    coachTr(
                                        isEnglish,
                                        "שמור ${coachSectionTitleForUi(title, false)}",
                                        "Save ${coachSectionTitleForUi(title, true)}"
                                    )
                                },
                            style =
                                KmiTypography.action.copy(
                                    fontWeight =
                                        FontWeight.ExtraBold
                                ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                saveMessage?.let { msg ->
                    val isSuccess =
                        msg.contains("נשמרו") ||
                                msg.contains(
                                    "saved",
                                    ignoreCase = true
                                )

                    Surface(
                        color =
                            if (isSuccess) {
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .errorContainer
                            },
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style =
                                KmiTypography.caption.copy(
                                    fontWeight =
                                        FontWeight.Bold
                                ),
                            color =
                                if (isSuccess) {
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimaryContainer
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .onErrorContainer
                                },
                            textAlign =
                                coachTextAlign(
                                    isEnglish
                                ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LabeledField(
        label: String,
        value: String,
        isEnglish: Boolean,
        modifier: Modifier = Modifier
    ) {
        val textAlign = coachTextAlign(isEnglish)

        val horizontalAlignment =
            coachHorizontalAlignment(isEnglish)

        val cleanValue =
            value.ifBlank { "—" }

        val fieldContainerColor =
            MaterialTheme
                .colorScheme
                .surface

        val fieldBorderColor =
            MaterialTheme
                .colorScheme
                .outline
                .copy(alpha = 0.55f)

        val iconContainerColor =
            MaterialTheme
                .colorScheme
                .primaryContainer

        val iconContentColor =
            MaterialTheme
                .colorScheme
                .onPrimaryContainer

        val iconText = when {
            label.contains("גיל") || label.contains("Age") -> "🎂"
            label.contains("ותק") || label.contains("Seniority") -> "🕒"
            label.contains("דרגה") || label.contains("Rank") -> "🏅"
            label.contains("סניף") || label.contains("Branch") -> "📍"
            label.contains("קבוצה") || label.contains("Group") -> "👥"
            label.contains("נוכחות") || label.contains("Attendance") -> "📊"
            else -> "•"
        }

        Surface(
            color = fieldContainerColor,
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color = fieldBorderColor
            ),
            modifier = modifier.heightIn(min = 78.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme
                                        .colorScheme
                                        .surface,
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant,
                                    MaterialTheme
                                        .colorScheme
                                        .surface
                                )
                            )
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isEnglish) {
                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    iconContainerColor
                                ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iconText,
                            style = KmiTypography.action,
                            color = iconContentColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp),
                    horizontalAlignment =
                        horizontalAlignment
                ) {
                    Text(
                        text = label,
                        style =
                            KmiTypography.caption.copy(
                                fontWeight =
                                    FontWeight.ExtraBold
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        textAlign = textAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = cleanValue,
                        style =
                            KmiTypography.secondary.copy(
                                fontWeight =
                                    FontWeight.ExtraBold
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,
                        textAlign = textAlign,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isEnglish) {
                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    iconContainerColor
                                ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iconText,
                            style = KmiTypography.action,
                            color = iconContentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    /* ===== עזר רפלקטיבי ל-AttendanceRecord ===== */

    private fun extractMemberId(record: Any): Long? {
        val cls = record.javaClass
        // קודם מחפשים שדה בשם שמרמז על memberId
        cls.declaredFields.forEach { f ->
            try {
                if (!f.name.contains("member", ignoreCase = true)) return@forEach
                f.isAccessible = true

                when (val v = f.get(record)) {
                    is Long -> return v
                    is Int -> return v.toLong()
                    is String -> return v.toLongOrNull()
                }
            } catch (_: Throwable) {
            }
        }
        // אם לא נמצא – מחפשים סתם Long יחיד
        cls.declaredFields.forEach { f ->
            try {
                f.isAccessible = true
                val v = f.get(record)
                if (v is Long) return v
            } catch (_: Throwable) {
            }
        }
        return null
    }

    private fun recordStatus(record: Any): AttendanceStatus? {
        val cls = record.javaClass
        // קודם מנסים שדה מסוג AttendanceStatus
        cls.declaredFields.forEach { f ->
            try {
                f.isAccessible = true
                val v = f.get(record)
                if (v is AttendanceStatus) return v
            } catch (_: Throwable) {
            }
        }
        // אחר כך מנסים String בשם status
        cls.declaredFields.forEach { f ->
            try {
                if (!f.name.contains("status", ignoreCase = true)) return@forEach
                f.isAccessible = true
                val v = f.get(record) as? String ?: return@forEach
                return runCatching { AttendanceStatus.valueOf(v) }.getOrNull()
            } catch (_: Throwable) {
            }
        }
        return null
    }