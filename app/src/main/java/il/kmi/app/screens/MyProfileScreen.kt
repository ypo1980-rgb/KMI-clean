@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.shared.domain.Belt
import il.kmi.shared.prefs.KmiPrefs
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import il.kmi.app.screens.registration.CoachBranchAssignment
import il.kmi.app.screens.registration.CoachBranchAssignmentsCodec
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.database.KmiDatabaseProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.R
import il.kmi.app.KmiCalendarSync
import il.kmi.app.hasCalendarPermission
import il.kmi.app.reminders.TrainingReminderScheduler
import il.kmi.app.training.TrainingAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale


//-----------------------------------------------------------------------------

// ----- מודל נתונים להזנה נוחה -----
data class ProfileBranchEntry(
    val branch: String,
    val address: String,
    val group: String = "—",
    val coach: String = "—"
)

data class UserProfileInfo(
    val userName: String = "שם המשתמש",
    val belt: String = "חגורה XXX",

    // ✅ משמש לציור תמונת החגורה לפי צבע הדרגה הנוכחית
    val currentBeltId: String = "",

    // ✅ משמש לציור תמונת החגורה הבאה בכרטיס התחתון
    val trainingTowardsBeltId: String = "",

    val branchEntries: List<ProfileBranchEntry> = emptyList(),
    val branch: String = "סניף - XXX",
    val branchAddress: String = "כתובת הסניף - XXX",
    val group: String = "קבוצה - XXX",
    val coach: String = "מאמן - XXXX",
    val nextTraining: String = "אימון הבא - XXX",
    val trainingTowardsBelt: String = "מתאמן לחגורה - XXX",
    val email: String = "name@example.com",
    val phone: String = "050-0000000",
    val accountUserName: String = "user_123",
    val password: String = "••••••••"
)

private data class FirestoreProfileInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val username: String = "",
    val region: String = "",
    val branch: String = "",
    val group: String = "",
    val belt: String = "",
    val role: String = ""
)

private fun profileTr(isEnglish: Boolean, he: String, en: String): String {
    return if (isEnglish) en else he
}

private fun profileTextAlign(isEnglish: Boolean): TextAlign {
    return if (isEnglish) TextAlign.Left else TextAlign.Right
}

private fun profileHorizontalAlignment(isEnglish: Boolean): Alignment.Horizontal {
    return if (isEnglish) Alignment.Start else Alignment.End
}

private fun profileLayoutDirection(isEnglish: Boolean): LayoutDirection {
    return if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl
}

private fun profileBeltDrawableForRawId(
    rawId: String?
): Int {
    return when (
        rawId
            ?.trim()
            ?.lowercase()
            .orEmpty()
    ) {
        "white",
        "לבנה" ->
            R.drawable.intro_belt_white

        "yellow",
        "צהובה" ->
            R.drawable.intro_belt_yellow

        "orange",
        "כתומה" ->
            R.drawable.intro_belt_orange

        "green",
        "ירוקה" ->
            R.drawable.intro_belt_green

        "blue",
        "כחולה" ->
            R.drawable.intro_belt_blue

        "brown",
        "חומה" ->
            R.drawable.intro_belt_brown

        "black",
        "black_dan_1",
        "שחורה",
        "שחורה דאן 1" ->
            R.drawable.intro_belt_black

        "black_dan_2",
        "שחורה דאן 2" ->
            R.drawable.intro_belt_black_dan_2

        "black_dan_3",
        "שחורה דאן 3" ->
            R.drawable.intro_belt_black_dan_3

        "black_dan_4",
        "שחורה דאן 4" ->
            R.drawable.intro_belt_black_dan_4

        "black_dan_5",
        "שחורה דאן 5" ->
            R.drawable.intro_belt_black_dan_5

        "black_dan_6",
        "black_dan_7",
        "black_dan_8",
        "שחורה דאן 6",
        "שחורה דאן 7",
        "שחורה דאן 8" ->
            R.drawable.intro_belt_red_white_dan_6_7_8

        "black_dan_9",
        "black_dan_10",
        "שחורה דאן 9",
        "שחורה דאן 10" ->
            R.drawable.intro_belt_red_dan_9_10

        else ->
            R.drawable.intro_belt_white
    }
}

private fun shareProfilePdf(
    ctx: Context,
    info: UserProfileInfo,
    isEnglish: Boolean
) {
    val pdfFile = createProfilePdf(
        context = ctx,
        info = info,
        isEnglish = isEnglish
    )

    val uri = FileProvider.getUriForFile(
        ctx,
        "${ctx.packageName}.fileprovider",
        pdfFile
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(
            Intent.EXTRA_SUBJECT,
            if (isEnglish) "My KAMI profile" else "הפרופיל שלי - KAMI"
        )
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    ctx.startActivity(
        Intent.createChooser(
            sendIntent,
            if (isEnglish) "Share PDF" else "שיתוף PDF"
        )
    )
}

private fun traineeRankDisplayName(rawId: String?): String {
    return when (rawId?.trim().orEmpty()) {
        "white" -> "לבנה"
        "yellow" -> "צהובה"
        "orange" -> "כתומה"
        "green" -> "ירוקה"
        "blue" -> "כחולה"
        "brown" -> "חומה"

        "black",
        "שחורה",
        "שחורה דאן 1" -> "שחורה דאן 1"

        "black_dan_2" -> "שחורה דאן 2"
        "black_dan_3" -> "שחורה דאן 3"
        "black_dan_4" -> "שחורה דאן 4"
        "black_dan_5" -> "שחורה דאן 5"
        "black_dan_6" -> "שחורה דאן 6"
        "black_dan_7" -> "שחורה דאן 7"
        "black_dan_8" -> "שחורה דאן 8"
        "black_dan_9" -> "שחורה דאן 9"
        "black_dan_10" -> "שחורה דאן 10"

        else -> ""
    }
}

private fun nextTraineeRankDisplayName(rawId: String?): String {
    return when (rawId?.trim().orEmpty()) {
        "white" -> "צהובה"
        "yellow" -> "כתומה"
        "orange" -> "ירוקה"
        "green" -> "כחולה"
        "blue" -> "חומה"
        "brown" -> "שחורה דאן 1"

        "black",
        "שחורה",
        "שחורה דאן 1" -> "שחורה דאן 2"

        "black_dan_2" -> "שחורה דאן 3"
        "black_dan_3" -> "שחורה דאן 4"
        "black_dan_4" -> "שחורה דאן 5"
        "black_dan_5" -> "שחורה דאן 6"
        "black_dan_6" -> "שחורה דאן 7"
        "black_dan_7" -> "שחורה דאן 8"
        "black_dan_8" -> "שחורה דאן 9"
        "black_dan_9" -> "שחורה דאן 10"
        "black_dan_10" -> "—"

        else -> "—"
    }
}

private fun nextTraineeRankId(rawId: String?): String {
    return when (rawId?.trim().orEmpty()) {
        "white", "לבנה" -> "yellow"
        "yellow", "צהובה" -> "orange"
        "orange", "כתומה" -> "green"
        "green", "ירוקה" -> "blue"
        "blue", "כחולה" -> "brown"
        "brown", "חומה" -> "black"

        "black",
        "שחורה",
        "שחורה דאן 1" -> "black_dan_2"

        "black_dan_2", "שחורה דאן 2" -> "black_dan_3"
        "black_dan_3", "שחורה דאן 3" -> "black_dan_4"
        "black_dan_4", "שחורה דאן 4" -> "black_dan_5"
        "black_dan_5", "שחורה דאן 5" -> "black_dan_6"
        "black_dan_6", "שחורה דאן 6" -> "black_dan_7"
        "black_dan_7", "שחורה דאן 7" -> "black_dan_8"
        "black_dan_8", "שחורה דאן 8" -> "black_dan_9"
        "black_dan_9", "שחורה דאן 9" -> "black_dan_10"

        else -> ""
    }
}

private fun traineeRankDisplayNameForUi(
    rawId: String?,
    isEnglish: Boolean
): String {
    if (!isEnglish) {
        return traineeRankDisplayName(rawId)
    }

    return when (rawId?.trim().orEmpty()) {
        "white", "לבנה" -> "White"
        "yellow", "צהובה" -> "Yellow"
        "orange", "כתומה" -> "Orange"
        "green", "ירוקה" -> "Green"
        "blue", "כחולה" -> "Blue"
        "brown", "חומה" -> "Brown"

        "black",
        "שחורה",
        "שחורה דאן 1" -> "Black Dan 1"

        "black_dan_2", "שחורה דאן 2" -> "Black Dan 2"
        "black_dan_3", "שחורה דאן 3" -> "Black Dan 3"
        "black_dan_4", "שחורה דאן 4" -> "Black Dan 4"
        "black_dan_5", "שחורה דאן 5" -> "Black Dan 5"
        "black_dan_6", "שחורה דאן 6" -> "Black Dan 6"
        "black_dan_7", "שחורה דאן 7" -> "Black Dan 7"
        "black_dan_8", "שחורה דאן 8" -> "Black Dan 8"
        "black_dan_9", "שחורה דאן 9" -> "Black Dan 9"
        "black_dan_10", "שחורה דאן 10" -> "Black Dan 10"

        else -> ""
    }
}

private fun nextTraineeRankDisplayNameForUi(
    rawId: String?,
    isEnglish: Boolean
): String {
    if (!isEnglish) {
        return nextTraineeRankDisplayName(rawId)
    }

    return when (rawId?.trim().orEmpty()) {
        "white" -> "Yellow"
        "yellow" -> "Orange"
        "orange" -> "Green"
        "green" -> "Blue"
        "blue" -> "Brown"
        "brown" -> "Black Dan 1"

        "black",
        "שחורה",
        "שחורה דאן 1" -> "Black Dan 2"

        "black_dan_2", "שחורה דאן 2" -> "Black Dan 3"
        "black_dan_3", "שחורה דאן 3" -> "Black Dan 4"
        "black_dan_4", "שחורה דאן 4" -> "Black Dan 5"
        "black_dan_5", "שחורה דאן 5" -> "Black Dan 6"
        "black_dan_6", "שחורה דאן 6" -> "Black Dan 7"
        "black_dan_7", "שחורה דאן 7" -> "Black Dan 8"
        "black_dan_8", "שחורה דאן 8" -> "Black Dan 9"
        "black_dan_9", "שחורה דאן 9" -> "Black Dan 10"
        "black_dan_10", "שחורה דאן 10" -> "—"

        else -> "—"
    }
}

private fun firestoreProfileFirstString(
    data: Map<String, Any?>,
    vararg keys: String
): String {
    for (key in keys) {
        val value = data[key]

        when (value) {
            is String -> {
                if (value.trim().isNotBlank()) return value.trim()
            }

            is List<*> -> {
                val joined = value
                    .mapNotNull { it?.toString()?.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString(", ")

                if (joined.isNotBlank()) return joined
            }
        }
    }

    return ""
}

private fun firestoreProfileStringList(
    data: Map<String, Any?>,
    vararg keys: String
): List<String> {
    val out = mutableListOf<String>()

    keys.forEach { key ->
        when (val value = data[key]) {
            is String -> {
                val raw = value.trim()

                if (raw.isBlank()) {
                    // no-op
                } else if (raw.startsWith("[")) {
                    runCatching {
                        val arr = org.json.JSONArray(raw)
                        for (i in 0 until arr.length()) {
                            arr.optString(i)
                                .trim()
                                .takeIf { it.isNotBlank() }
                                ?.let { out += it }
                        }
                    }
                } else {
                    raw.split(',', ';', '|', '\n')
                        .map { it.trim().trim('"') }
                        .filter { it.isNotBlank() }
                        .forEach { out += it }
                }
            }

            is List<*> -> {
                value
                    .mapNotNull { it?.toString()?.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { out += it }
            }

            is Set<*> -> {
                value
                    .mapNotNull { it?.toString()?.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { out += it }
            }
        }
    }

    return out
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun firestoreProfileBranchAssignments(
    data: Map<String, Any?>
): List<CoachBranchAssignment> {
    return (
            data["coachBranchAssignments"]
                    as? List<*>
            )
        ?.mapNotNull { rawAssignment ->
            val assignmentMap =
                rawAssignment as? Map<*, *>
                    ?: return@mapNotNull null

            val branch =
                assignmentMap["branch"]
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            if (branch.isBlank()) {
                return@mapNotNull null
            }

            val groups =
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
                branch = branch,
                groups = groups
            ).sanitized()
        }
        ?.distinctBy { assignment ->
            assignment.branch
        }
        .orEmpty()
}

private fun firestoreProfileFromMap(data: Map<String, Any?>): FirestoreProfileInfo {
    return FirestoreProfileInfo(
        fullName = firestoreProfileFirstString(
            data,
            "fullName",
            "name",
            "displayName"
        ),
        email = firestoreProfileFirstString(
            data,
            "email"
        ),
        phone = firestoreProfileFirstString(
            data,
            "phone",
            "phoneNumber",
            "phone_number"
        ),
        username = firestoreProfileFirstString(
            data,
            "username",
            "userName",
            "accountUserName"
        ),
        region = firestoreProfileFirstString(
            data,
            "region",
            "activeRegion",
            "active_region"
        ),
        branch = firestoreProfileFirstString(
            data,
            "activeBranch",
            "active_branch",
            "branch",
            "branchesCsv",
            "branches"
        ),
        group = firestoreProfileFirstString(
            data,
            "activeGroup",
            "active_group",
            "primaryGroup",
            "groupKey",
            "group_key",
            "age_group",
            "group",
            "groupsCsv",
            "groups"
        ),
        belt = firestoreProfileFirstString(
            data,
            "current_belt",
            "belt_current",
            "belt",
            "rank"
        ),
        role = firestoreProfileFirstString(
            data,
            "role",
            "user_role",
            "userType",
            "type"
        )
    )
}

/**
 * מסך פרופיל – בונה את המידע מתוך ה־Prefs ומציג כרטיס יוקרתי
 */
@Composable
fun MyProfileScreen(
    sp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    onClose: () -> Unit,
    onHome: () -> Unit = onClose,
    onEditProfile: () -> Unit = {}
) {
    // עזר: בוחר מחרוזת לא ריקה מהמקורות הנתונים
    fun prefStr(primary: String?, vararg fallbacks: String?): String {
        val p = primary ?: ""
        if (p.isNotBlank()) return p
        fallbacks.forEach { fb -> if (!fb.isNullOrBlank()) return fb }
        return ""
    }

    val ctx = LocalContext.current
    val userSp = remember(key1 = ctx) { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
    val scroll = rememberScrollState()   // ✅ גלילה

    val langManager = remember(ctx) {
        AppLanguageManager(ctx)
    }

    val isEnglish =
        langManager.getCurrentLanguage() == AppLanguage.ENGLISH

    val screenLayoutDirection =
        profileLayoutDirection(isEnglish)

    val isDarkMode =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    var firestoreProfile by remember {
        mutableStateOf(FirestoreProfileInfo())
    }

    /*
     * מקור האמת החדש לסניפים ולקבוצות.
     */
    var firestoreBranchAssignments by remember {
        mutableStateOf<
                List<CoachBranchAssignment>
                >(emptyList())
    }

    var isLoadingFirestoreProfile by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (!uid.isNullOrBlank()) {
            isLoadingFirestoreProfile = true

            runCatching {
                Firebase.firestore
                    .collection("users")
                    .document(uid)
                    .get()
                    .await()
            }.onSuccess { snap ->
                val data = snap.data.orEmpty()

                firestoreProfile =
                    firestoreProfileFromMap(data)

                firestoreBranchAssignments =
                    firestoreProfileBranchAssignments(
                        data
                    )

                val p = firestoreProfile

                /*
                 * בוחר את המקור העדכני הראשון שאינו ריק.
                 * אין לאחד את כל השדות, משום שחלקם עשויים
                 * להכיל ערכים שנשארו לפני עריכת הפרופיל.
                 */
                fun preferredFirestoreList(
                    vararg keys: String
                ): List<String> {
                    keys.forEach { key ->
                        val values = firestoreProfileStringList(
                            data,
                            key
                        )

                        if (values.isNotEmpty()) {
                            return values
                        }
                    }

                    return emptyList()
                }

                /*
    * תחילה מחפשים את השדות הרשימתיים שמכילים את כל
    * הסניפים והקבוצות. השדות היחידים משמשים רק כ־fallback.
    */
                /*
                 * המבנה החדש קודם לרשימות הישנות.
                 */
                val branchesFromFirestore =
                    if (
                        firestoreBranchAssignments
                            .isNotEmpty()
                    ) {
                        CoachBranchAssignmentsCodec
                            .flattenBranches(
                                firestoreBranchAssignments
                            )
                    } else {
                        preferredFirestoreList(
                            "branches",
                            "branches_json",
                            "selected_branches",
                            "branchesCsv",
                            "branch",
                            "activeBranch",
                            "active_branch"
                        )
                            .map { branch ->
                                branch.trim()
                            }
                            .filter { branch ->
                                branch.isNotBlank()
                            }
                            .distinct()
                    }

                val groupsFromFirestore =
                    if (
                        firestoreBranchAssignments
                            .isNotEmpty()
                    ) {
                        CoachBranchAssignmentsCodec
                            .flattenGroups(
                                firestoreBranchAssignments
                            )
                    } else {
                        preferredFirestoreList(
                            "age_groups",
                            "groups",
                            "groups_json",
                            "selected_groups",
                            "groupsCsv",
                            "group",
                            "age_group",
                            "primaryGroup",
                            "activeGroup",
                            "active_group",
                            "groupKey",
                            "group_key"
                        )
                            .map { group ->
                                group.trim()
                            }
                            .filter { group ->
                                group.isNotBlank()
                            }
                            .distinct()
                    }

                val branchAssignmentsJson =
                    CoachBranchAssignmentsCodec.encode(
                        firestoreBranchAssignments
                    )

                val branchesCsv =
                    branchesFromFirestore
                        .joinToString(", ")

                val groupsCsv =
                    groupsFromFirestore
                        .joinToString(", ")

                val branchesJson =
                    org.json.JSONArray(
                        branchesFromFirestore
                    ).toString()

                val groupsJson =
                    org.json.JSONArray(
                        groupsFromFirestore
                    ).toString()

// מיישר גם את SharedPreferences כדי ששאר המסכים ייהנו מהמידע.
                val profilePrefsSaved = userSp.edit()
                    .putString("fullName", p.fullName)
                    .putString("email", p.email)
                    .putString("phone", p.phone)

                    .putString("branch", branchesCsv)
                    .putString("branches", branchesCsv)
                    .putString("branches_json", branchesJson)
                    .putString("selected_branches", branchesCsv)
                    .putString("activeBranch", branchesFromFirestore.firstOrNull().orEmpty())
                    .putString("active_branch", branchesFromFirestore.firstOrNull().orEmpty())

                    .putString("group", groupsCsv)
                    .putString("groups", groupsCsv)
                    .putString("groups_json", groupsJson)
                    .putString("selected_groups", groupsCsv)
                    .putString("groupKey", groupsFromFirestore.firstOrNull().orEmpty())
                    .putString("age_group", groupsCsv)
                    .putString(
                        "activeGroup",
                        groupsFromFirestore
                            .firstOrNull()
                            .orEmpty()
                    )
                    .putString(
                        "active_group",
                        groupsFromFirestore
                            .firstOrNull()
                            .orEmpty()
                    )
                    .putString(
                        "coach_branch_assignments_json",
                        branchAssignmentsJson
                    )

                    .putString("belt", p.belt)
                    .putString("current_belt", p.belt)
                    .putString("user_role", p.role)
                    .commit()

                if (profilePrefsSaved) {
                    /*
                     * רק לאחר שהסניף והקבוצה החדשים נשמרו בפועל,
                     * מרעננים את המנגנונים הפעילים.
                     */
                    withContext(Dispatchers.IO) {
                        val calendarSyncEnabled = sp.getBoolean(
                            "calendar_sync_selected_enabled",
                            false
                        )

                        val selectedCalendarId = sp.getLong(
                            "calendar_sync_selected_calendar_id",
                            -1L
                        )

                        if (
                            calendarSyncEnabled &&
                            selectedCalendarId > 0L &&
                            hasCalendarPermission(ctx)
                        ) {
                            KmiCalendarSync.upsertAllToSelectedCalendar(
                                context = ctx.applicationContext,
                                selectedCalendarId = selectedCalendarId
                            )
                        }

                        val trainingRemindersEnabled = sp.getBoolean(
                            "training_reminders_enabled",
                            true
                        )

                        if (trainingRemindersEnabled) {
                            val leadMinutes = sp.getInt(
                                "training_reminder_minutes",
                                sp.getInt("lead_minutes", 60)
                            ).takeIf { it > 0 } ?: 60

                            /*
                             * הגשר מבטל תחילה תזכורות מהמנגנון הישן
                             * ולאחר מכן בונה את התזכורות החדשות.
                             */
                            TrainingAlarmReceiver.scheduleWeeklyAlarms(
                                ctx = ctx.applicationContext,
                                leadMinutes = leadMinutes
                            )
                        } else {
                            /*
                             * כאשר התזכורות כבויות, מנקים גם את המנגנון
                             * הישן וגם את המנגנון החדש.
                             */
                            TrainingAlarmReceiver.cancelWeeklyAlarms(
                                ctx.applicationContext
                            )

                            TrainingReminderScheduler.cancelWeeklyTrainingAlarms(
                                context = ctx.applicationContext
                            )
                        }
                    }
                }
            }.onFailure {
                // לא מפילים את המסך — ממשיכים עם KmiPrefs/SharedPreferences.
            }

            isLoadingFirestoreProfile = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides screenLayoutDirection) {
        // קריאה מה־Prefs (KmiPrefs מקור אמת; SP/UserSP פולבאק)
        val fullName = prefStr(
            kmiPrefs.fullName,
            sp.getString("fullName", ""),
            userSp.getString("fullName", ""),
            firestoreProfile.fullName
        )

        val email = prefStr(
            kmiPrefs.email,
            sp.getString("email", ""),
            userSp.getString("email", ""),
            firestoreProfile.email
        )

        val phone = prefStr(
            kmiPrefs.phone,
            sp.getString("phone", ""),
            userSp.getString("phone", ""),
            firestoreProfile.phone
        )

        val username = prefStr(
            kmiPrefs.username,
            sp.getString("username", ""),
            userSp.getString("username", ""),
            firestoreProfile.username
        )

        /*
         * שם המשתמש המוצג בפרופיל וב־PDF.
         * הנתון האמיתי אינו משתנה ב־Firestore או בהעדפות.
         */
        val profileDisplayName =
            TraineeDisplayNameMapper.displayName(
                realName = fullName,
                stableKey =
                    FirebaseAuth.getInstance()
                        .currentUser
                        ?.uid
                        .orEmpty()
                        .ifBlank {
                            username.ifBlank {
                                fullName
                            }
                        },
                isEnglish = isEnglish
            ).ifBlank {
                profileTr(
                    isEnglish,
                    "משתמש ללא שם",
                    "Unnamed user"
                )
            }

        val password = prefStr(
            kmiPrefs.password,
            sp.getString("password", "")
        )

        /*
         * סדר העדיפות:
         * 1. השיוכים שהתקבלו עכשיו מהשרת.
         * 2. המבנה החדש שנשמר מקומית.
         * 3. הרשימות הישנות.
         */
        val savedBranchAssignments =
            firestoreBranchAssignments
                .ifEmpty {
                    CoachBranchAssignmentsCodec
                        .decode(
                            userSp.getString(
                                "coach_branch_assignments_json",
                                ""
                            )
                        )
                }
                .ifEmpty {
                    CoachBranchAssignmentsCodec
                        .decode(
                            sp.getString(
                                "coach_branch_assignments_json",
                                ""
                            )
                        )
                }

        val branchRaw = prefStr(
            kmiPrefs.branch,
            sp.getString("activeBranch", ""),
            sp.getString("active_branch", ""),
            sp.getString("branch", ""),
            userSp.getString("activeBranch", ""),
            userSp.getString("active_branch", ""),
            userSp.getString("branch", ""),
            firestoreProfile.branch
        )

        fun splitBranches(
            value: String
        ): List<String> =
            value
                .split('\n', '|', ';', ',')
                .map { branch ->
                    branch.trim()
                }
                .filter { branch ->
                    branch.isNotEmpty()
                }
                .distinct()

        val branchesList: List<String> =
            if (savedBranchAssignments.isNotEmpty()) {
                CoachBranchAssignmentsCodec
                    .flattenBranches(
                        savedBranchAssignments
                    )
            } else {
                splitBranches(branchRaw)
            }

        val primaryBranch: String =
            branchesList.firstOrNull()
                .orEmpty()

        fun profilePrefsList(vararg values: String?): List<String> {
            val out = mutableListOf<String>()

            values.forEach { value ->
                val raw = value?.trim().orEmpty()

                if (raw.isBlank()) {
                    // no-op
                } else if (raw.startsWith("[")) {
                    runCatching {
                        val arr = org.json.JSONArray(raw)
                        for (i in 0 until arr.length()) {
                            arr.optString(i)
                                .trim()
                                .takeIf { it.isNotBlank() }
                                ?.let { out += it }
                        }
                    }
                } else {
                    raw.split(',', ';', '|', '\n')
                        .map { it.trim().trim('"') }
                        .filter { it.isNotBlank() }
                        .forEach { out += it }
                }
            }

            return out
                .map {
                    TrainingCatalog
                        .normalizeGroupName(it)
                        .ifBlank { it }
                }
                .filter { it.isNotBlank() }
                .distinct()
        }

        val groupsList =
            if (savedBranchAssignments.isNotEmpty()) {
                CoachBranchAssignmentsCodec
                    .flattenGroups(
                        savedBranchAssignments
                    )
            } else {
                profilePrefsList(
                    kmiPrefs.ageGroup,
                    sp.getString(
                        "groups_json",
                        ""
                    ),
                    sp.getString(
                        "selected_groups",
                        ""
                    ),
                    sp.getString(
                        "groups",
                        ""
                    ),
                    sp.getString(
                        "age_groups",
                        ""
                    ),
                    sp.getString(
                        "activeGroup",
                        ""
                    ),
                    sp.getString(
                        "active_group",
                        ""
                    ),
                    sp.getString(
                        "groupKey",
                        ""
                    ),
                    sp.getString(
                        "group_key",
                        ""
                    ),
                    sp.getString(
                        "age_group",
                        ""
                    ),
                    sp.getString(
                        "group",
                        ""
                    ),
                    userSp.getString(
                        "groups_json",
                        ""
                    ),
                    userSp.getString(
                        "selected_groups",
                        ""
                    ),
                    userSp.getString(
                        "groups",
                        ""
                    ),
                    userSp.getString(
                        "age_groups",
                        ""
                    ),
                    userSp.getString(
                        "activeGroup",
                        ""
                    ),
                    userSp.getString(
                        "active_group",
                        ""
                    ),
                    userSp.getString(
                        "groupKey",
                        ""
                    ),
                    userSp.getString(
                        "group_key",
                        ""
                    ),
                    userSp.getString(
                        "age_group",
                        ""
                    ),
                    userSp.getString(
                        "group",
                        ""
                    ),
                    firestoreProfile.group
                )
            }

        val group =
            groupsList.firstOrNull()
                .orEmpty()

        val groupDisplay =
            groupsList
                .joinToString("\n")
                .ifBlank { "—" }

        /*
         * זוגות מדויקים של סניף וקבוצה.
         *
         * במבנה החדש כל קבוצה נבדקת רק מול
         * הסניף שאליו היא משויכת.
         *
         * משתמשים במכפלה הישנה רק כ-fallback
         * למשתמש שטרם נשמר במבנה החדש.
         */
        val branchGroupPairs:
                List<Pair<String, String>> =
            if (savedBranchAssignments.isNotEmpty()) {
                savedBranchAssignments
                    .flatMap { assignment ->
                        assignment.groups.map { groupName ->
                            assignment.branch to
                                    groupName
                        }
                    }
                    .filter { (branchName, groupName) ->
                        branchName.isNotBlank() &&
                                groupName.isNotBlank()
                    }
                    .distinct()
            } else {
                branchesList.flatMap { branchName ->
                    groupsList.map { groupName ->
                        branchName to groupName
                    }
                }
            }

        fun dbGroupMatches(
            selectedGroup: String,
            databaseGroupHe: String,
            databaseGroupEn: String
        ): Boolean {
            val wanted = TrainingCatalog
                .normalizeGroupName(selectedGroup)
                .ifBlank { selectedGroup }
                .trim()

            val dbHe = TrainingCatalog
                .normalizeGroupName(databaseGroupHe)
                .ifBlank { databaseGroupHe }
                .trim()

            val dbEn = databaseGroupEn.trim()

            if (wanted.equals(dbHe, ignoreCase = true)) return true
            if (selectedGroup.trim().equals(databaseGroupHe.trim(), ignoreCase = true)) return true
            if (selectedGroup.trim().equals(dbEn, ignoreCase = true)) return true

            if (wanted == "נוער" && dbHe == "נוער + בוגרים") return true
            if (wanted == "בוגרים" && dbHe == "נוער + בוגרים") return true

            return false
        }

        fun calendarDowFromDb(dayOfWeek: String): Int {
            return when (dayOfWeek.trim().uppercase(java.util.Locale.US)) {
                "SUNDAY" -> java.util.Calendar.SUNDAY
                "MONDAY" -> java.util.Calendar.MONDAY
                "TUESDAY" -> java.util.Calendar.TUESDAY
                "WEDNESDAY" -> java.util.Calendar.WEDNESDAY
                "THURSDAY" -> java.util.Calendar.THURSDAY
                "FRIDAY" -> java.util.Calendar.FRIDAY
                "SATURDAY" -> java.util.Calendar.SATURDAY
                else -> java.util.Calendar.MONDAY
            }
        }

        fun hourFromDbTime(time: String): Int {
            return time.substringBefore(":").trim().toIntOrNull() ?: 19
        }

        fun minuteFromDbTime(time: String): Int {
            return time.substringAfter(":", "").trim().toIntOrNull() ?: 0
        }

        data class DbNextTrainingForProfile(
            val cal: java.util.Calendar,
            val place: String,
            val address: String,
            val coach: String
        )

        fun nextTrainingFromDatabase(
            branchName: String,
            groupName: String
        ): DbNextTrainingForProfile? {
            val dbBranch = KmiDatabaseProvider.branchByName(ctx, branchName) ?: return null

            val matchingDays = dbBranch.trainingDays.filter { trainingDay ->
                dbGroupMatches(
                    selectedGroup = groupName,
                    databaseGroupHe = trainingDay.groupHe,
                    databaseGroupEn = trainingDay.groupEn
                )
            }

            if (matchingDays.isEmpty()) return null

            val now = java.util.Calendar.getInstance()

            return matchingDays
                .map { trainingDay ->
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                        set(java.util.Calendar.DAY_OF_WEEK, calendarDowFromDb(trainingDay.dayOfWeek))
                        set(java.util.Calendar.HOUR_OF_DAY, hourFromDbTime(trainingDay.startTime))
                        set(java.util.Calendar.MINUTE, minuteFromDbTime(trainingDay.startTime))

                        if (!after(now)) {
                            add(java.util.Calendar.DAY_OF_YEAR, 7)
                        }
                    }

                    DbNextTrainingForProfile(
                        cal = cal,
                        place = dbBranch.displayPlace(isEnglish = isEnglish),
                        address = dbBranch.displayAddress(isEnglish = isEnglish),
                        coach = trainingDay.displayCoachName(isEnglish = isEnglish)
                    )
                }
                .minByOrNull { it.cal.timeInMillis }
        }

        val beltId = prefStr(
            null,
            sp.getString("current_belt", ""),
            sp.getString("belt_current", ""),
            sp.getString("belt", ""),
            userSp.getString("current_belt", ""),
            userSp.getString("belt_current", ""),
            userSp.getString("belt", ""),
            firestoreProfile.belt
        )

        val currentBelt = Belt.fromAny(
            when {
                beltId.startsWith("black_dan_") -> "black"
                beltId == "שחורה דאן 1" -> "black"
                else -> beltId
            }
        )

        val beltHeb = traineeRankDisplayNameForUi(beltId, isEnglish)
            .ifBlank {
                if (isEnglish) {
                    currentBelt?.id ?: beltId.ifBlank { "Not set" }
                } else {
                    currentBelt?.heb ?: beltId.ifBlank { "לא הוגדר" }
                }
            }

        /*
         * האימון הבא מחושב רק מתוך שיוכים
         * חוקיים של סניף וקבוצה.
         */
        val dbUpcoming =
            branchGroupPairs
                .mapNotNull {
                        (branchName, groupName) ->

                    nextTrainingFromDatabase(
                        branchName = branchName,
                        groupName = groupName
                    )
                }
                .minByOrNull { training ->
                    training.cal.timeInMillis
                }

        val upcoming =
            if (
                dbUpcoming == null &&
                branchGroupPairs.isNotEmpty()
            ) {
                val savedRegion =
                    prefStr(
                        kmiPrefs.region,
                        sp.getString(
                            "region",
                            ""
                        ),
                        userSp.getString(
                            "region",
                            ""
                        ),
                        firestoreProfile.region
                    ).ifBlank {
                        "השרון"
                    }

                branchGroupPairs
                    .asSequence()
                    .mapNotNull {
                            (branchName, groupName) ->

                        TrainingCatalog
                            .upcomingFor(
                                region = savedRegion,
                                branch = branchName,
                                group = groupName,
                                count = 1
                            )
                            .firstOrNull()
                    }
                    .minByOrNull { training ->
                        training.cal.timeInMillis
                    }
            } else {
                null
            }

        val coachName: String =
            dbUpcoming?.coach.orEmpty()
                .ifBlank { upcoming?.coach.orEmpty() }
                .ifBlank { "—" }

        val nextTraining: String = when {
            dbUpcoming != null -> {
                val locale = if (isEnglish) {
                    java.util.Locale.US
                } else {
                    java.util.Locale("he", "IL")
                }

                val fmtDay = java.text.SimpleDateFormat("EEEE", locale)
                val fmtTime = java.text.SimpleDateFormat("HH:mm", locale)
                "${fmtDay.format(dbUpcoming.cal.time)} • ${fmtTime.format(dbUpcoming.cal.time)}\n${dbUpcoming.place}"
            }

            upcoming != null -> {
                val locale = if (isEnglish) {
                    java.util.Locale.US
                } else {
                    java.util.Locale("he", "IL")
                }

                val fmtDay = java.text.SimpleDateFormat("EEEE", locale)
                val fmtTime = java.text.SimpleDateFormat("HH:mm", locale)
                "${fmtDay.format(upcoming.cal.time)} • ${fmtTime.format(upcoming.cal.time)}\n${upcoming.place}"
            }

            else -> "—"
        }

        // ✅ הדרגה הבאה בתור, כולל דאן 2–10
        val nextBeltText: String = nextTraineeRankDisplayNameForUi(beltId, isEnglish)
        val nextBeltId: String = nextTraineeRankId(beltId)

        // --- תיקון: בניית רשימת סניפים + כתובת מתחת לכל סניף ---
        fun fallbackCityVenue(b: String): String {
            val parts = b.split('–', '-').map { it.trim() }
            val city = parts.getOrNull(0)
            val venue = parts.getOrNull(1)
            return if (!city.isNullOrBlank() && !venue.isNullOrBlank()) "$venue, $city" else "—"
        }

        fun normalizedProfileBranchKey(
            value: String
        ): String {
            return value
                .trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .lowercase(Locale("he", "IL"))
        }

        val branchEntriesResolved: List<ProfileBranchEntry> = if (branchesList.isEmpty()) {
            emptyList()
        } else {
            branchesList.map { b ->
                val dbAddress = KmiDatabaseProvider
                    .branchByName(ctx, b)
                    ?.displayAddress(isEnglish = isEnglish)
                    ?.trim()
                    .orEmpty()

                val resolvedAddress = when {
                    dbAddress.isNotBlank() && dbAddress != b.trim() -> dbAddress
                    else -> {
                        val mapped = TrainingCatalog.addressFor(b).trim()
                        if (mapped.isNotBlank() && mapped != b.trim()) {
                            mapped
                        } else {
                            fallbackCityVenue(b)
                        }
                    }
                }

                /*
                 * הקבוצות השייכות לסניף הזה בלבד.
                 */
                val groupsForThisBranch =
                    if (
                        savedBranchAssignments
                            .isNotEmpty()
                    ) {
                        savedBranchAssignments
                            .firstOrNull { assignment ->
                                normalizedProfileBranchKey(
                                    assignment.branch
                                ) ==
                                        normalizedProfileBranchKey(
                                            b
                                        )
                            }
                            ?.groups
                            .orEmpty()
                            .map { groupName ->
                                groupName.trim()
                            }
                            .filter { groupName ->
                                groupName.isNotBlank()
                            }
                            .distinct()
                    } else {
                        /*
                         * fallback למשתמש ישן:
                         * משאירים רק קבוצות שבאמת
                         * נמצאו בלוח של הסניף.
                         */
                        groupsList
                            .filter { groupName ->
                                nextTrainingFromDatabase(
                                    branchName = b,
                                    groupName =
                                        groupName
                                ) != null
                            }
                            .distinct()
                            .ifEmpty {
                                if (group.isNotBlank()) {
                                    listOf(group)
                                } else {
                                    emptyList()
                                }
                            }
                    }

                val branchMatches =
                    groupsForThisBranch
                        .mapNotNull { groupName ->
                            val upcomingForGroup =
                                nextTrainingFromDatabase(
                                    branchName = b,
                                    groupName =
                                        groupName
                                )

                            if (
                                upcomingForGroup != null
                            ) {
                                groupName to
                                        upcomingForGroup
                            } else {
                                null
                            }
                        }

                val branchUpcoming =
                    branchMatches
                        .map { match ->
                            match.second
                        }
                        .minByOrNull { training ->
                            training.cal.timeInMillis
                        }

                val branchCoach =
                    branchUpcoming
                        ?.coach
                        .orEmpty()
                        .ifBlank { "—" }

                ProfileBranchEntry(
                    branch = b.trim(),
                    address =
                        resolvedAddress
                            .ifBlank { "—" },
                    group =
                        groupsForThisBranch
                            .joinToString("\n")
                            .ifBlank { "—" },
                    coach = branchCoach
                )
            }
        }

        val branchDisplay: String =
            branchEntriesResolved.joinToString("\n") { it.branch }.ifBlank { "—" }

        val branchAddressResolved: String =
            branchEntriesResolved.joinToString("\n") { it.address }.ifBlank { "—" }
// --- סוף תיקון הכתובת ---

        val info = UserProfileInfo(
            userName =
                profileDisplayName.ifBlank {
                    username.ifBlank {
                        profileTr(
                            isEnglish,
                            "שם המשתמש",
                            "User name"
                        )
                    }
                },
            belt = beltHeb,
            currentBeltId = beltId,
            trainingTowardsBeltId = nextBeltId,
            branchEntries = branchEntriesResolved,
            branch = branchDisplay,
            branchAddress = branchAddressResolved,
            group = groupDisplay,
            coach = coachName,
            nextTraining = nextTraining,
            trainingTowardsBelt = nextBeltText,
            email = email.ifBlank { "—" },
            phone = phone.ifBlank { "—" },
            accountUserName = username.ifBlank { "—" },
            password = password.ifBlank { "••••••••" }
        )

        val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

        Scaffold(
            topBar = {
                KmiTopBar(
                    title = profileTr(
                        isEnglish,
                        "הפרופיל שלי",
                        "My Profile"
                    ),
                    onHome = {
                        runCatching { onHome() }.onFailure {
                            backDispatcher?.onBackPressed()
                        }
                    },
                    showTopHome = false,
                    showTopSearch = false,
                    showTopShare = false,
                    showBottomActions = true,
                    lockSearch = false,
                    lockHome = false,
                    centerTitle = true,
                    currentLang = if (isEnglish) "en" else "he",
                    onShare = {
                        shareProfilePdf(
                            ctx = ctx,
                            info = info,
                            isEnglish = isEnglish
                        )
                    }
                )
                     },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0)
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        Brush.verticalGradient(
                            colors =
                                if (isDarkMode) {
                                    listOf(
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.surface,
                                        Color(0xFF10243A),
                                        Color(0xFF0A3657),
                                        Color(0xFF041E33)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFF8FBFF),
                                        Color(0xFFEAF4FF),
                                        Color(0xFFB7DDF7),
                                        Color(0xFF1F78B4),
                                        Color(0xFF062B4A)
                                    )
                                }
                        )
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 20.dp,
                            bottom = 20.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoadingFirestoreProfile) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.24f)
                            )
                        ) {
                            Text(
                                text = profileTr(
                                    isEnglish,
                                    "מסנכרן פרופיל...",
                                    "Syncing profile..."
                                ),
                                style = KmiTypography.secondary.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 8.dp
                                )
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                    }

                    UserProfileCard(
                        info = info,
                        isEnglish = isEnglish,
                        onEditProfile = onEditProfile,
                        onClose = {
                            runCatching { onClose() }.onFailure {
                                backDispatcher?.onBackPressed()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * כרטיס “זכוכית” עם קווי מתאר גרדיאנטיים וטיפוגרפיה מודרנית
 */
@Composable
private fun UserProfileCard(
    info: UserProfileInfo,
    isEnglish: Boolean,
    onEditProfile: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)

    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val cardContainerColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = 0.96f)
        } else {
            Color(0xFFEAF2FF)
        }

    val cardContentColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color(0xFF111827)
        }

    val cardBorderColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        } else {
            Color(0xFFD8E3F5)
        }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        color = cardContainerColor,
        contentColor = cardContentColor,
        shape = shape,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(
            width = 1.dp,
            color = cardBorderColor
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = profileHorizontalAlignment(isEnglish)
        ) {
            // כותרת + חגורה באלכסון בצד כדי לחסוך מקום אנכי
            if (isEnglish) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(118.dp)
                            .height(76.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        ProfileBeltImage(
                            rawBeltId = info.currentBeltId,
                            modifier = Modifier
                                .fillMaxWidth()
                                // ✅ מוריד מעט את החגורה כדי שלא תישב גבוה מדי
                                .offset(x = 4.dp, y = (-16).dp),
                            imageHeight = 84.dp,
                            horizontalPadding = 0.dp,
                            rotateDegrees = -24f,
                            flipHorizontally = false
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = info.userName,
                            style = KmiTypography.screenTitle.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = cardContentColor,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Left
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = info.belt,
                            style = KmiTypography.secondary.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color =
                                if (isDarkMode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0xFF31528A)
                                },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Left,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = info.userName,
                            style = KmiTypography.screenTitle.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = cardContentColor,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = info.belt,
                            style = KmiTypography.secondary.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color =
                                if (isDarkMode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color(0xFF31528A)
                                },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(104.dp)
                            .height(82.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        ProfileBeltImage(
                            rawBeltId = info.currentBeltId,
                            modifier = Modifier
                                .fillMaxWidth()
                                // ✅ מוריד את החגורה מעט למטה כדי שלא תיצמד לחלק העליון
                                .offset(x = 4.dp, y = (-12).dp),
                            imageHeight = 84.dp,
                            horizontalPadding = 0.dp,
                            rotateDegrees = -24f,
                            flipHorizontally = false
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Button(
                onClick = onEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6D55AA),
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFF8E7BC4)
                )
            ) {
                Text(
                    text = profileTr(
                        isEnglish,
                        "עריכת פרופיל",
                        "Edit profile"
                    ),
                    style = KmiTypography.action.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // מפריד דק
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = cardBorderColor
            )
            Spacer(Modifier.height(10.dp))

            // ─────────────────────────────────────────────
            // שורות מידע בסגנון "תגית:" ואז הערך מתחת + מפריד
            // ─────────────────────────────────────────────

            BranchAddressListBlock(
                label = profileTr(isEnglish, "סניפים וכתובות:", "Branches and addresses:"),
                entries = info.branchEntries,
                isEnglish = isEnglish
            )

            // --- פרטי אימון וחשבון ---
            Spacer(Modifier.height(6.dp))

            LabeledValueBlock(
                label = profileTr(
                    isEnglish,
                    "האימון הבא:",
                    "Next training:"
                ),
                value = info.nextTraining,
                isEnglish = isEnglish
            )

            LabeledValueBlock(
                label = profileTr(isEnglish, "מייל:", "Email:"),
                value = info.email,
                isEnglish = isEnglish
            )
            LabeledValueBlock(
                label = profileTr(isEnglish, "טלפון:", "Phone:"),
                value = info.phone,
                isEnglish = isEnglish
            )
            LabeledValueBlock(
                label = profileTr(isEnglish, "שם משתמש:", "Username:"),
                value = info.accountUserName,
                isEnglish = isEnglish
            )
            PasswordRow(
                label = profileTr(isEnglish, "סיסמה", "Password"),
                password = info.password,
                isEnglish = isEnglish
            )

            // מפריד קטן לפני השורה התחתונה
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = cardBorderColor
            )
            Spacer(Modifier.height(8.dp))

            // שורת הדגשה תחתונה – “מתאמן לחגורה”
            val trainingTargetText = info.trainingTowardsBelt
                .removePrefix("מתאמן לחגורה")
                .removePrefix("Training toward belt")
                .trim()
                .ifEmpty { info.trainingTowardsBelt }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color =
                    if (isDarkMode) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color(0xFFDDEAFF)
                    },
                border = BorderStroke(
                    width = 1.dp,
                    color = cardBorderColor
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = profileTr(
                            isEnglish,
                            "מתאמן לחגורה",
                            "Training toward belt"
                        ),
                        style = KmiTypography.secondary.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color =
                            if (isDarkMode) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                Color(0xFF52627A)
                            },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = trainingTargetText,
                        style = KmiTypography.cardTitle.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color =
                            if (isDarkMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color(0xFF1E3A8A)
                            },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    // ✅ תמונת חגורה לפי החגורה הבאה שאליה המתאמן מתקדם
                    ProfileBeltImage(
                        rawBeltId = info.trainingTowardsBeltId,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBeltImage(
    rawBeltId: String?,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 70.dp,
    horizontalPadding: Dp = 18.dp,
    rotateDegrees: Float = 0f,
    flipHorizontally: Boolean = false
) {
    val beltDrawable = profileBeltDrawableForRawId(rawBeltId)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = beltDrawable),
            contentDescription = "Belt image",
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .padding(horizontal = horizontalPadding)
                .graphicsLayer {
                    scaleX = if (flipHorizontally) -1f else 1f
                    rotationZ = rotateDegrees
                },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun BranchAddressListBlock(
    label: String,
    entries: List<ProfileBranchEntry>,
    isEnglish: Boolean
) {
    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val labelColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color(0xFF52627A)
        }

    val valueColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color(0xFF111827)
        }

    val accentTextColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(0xFF1E3A8A)
        }

    val dividerColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        } else {
            Color(0xFFBFD0E8)
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = profileHorizontalAlignment(isEnglish)
    ) {
        Text(
            text = label,
            style = KmiTypography.secondary.copy(
                fontWeight = FontWeight.Medium
            ),
            color = labelColor,
            textAlign = profileTextAlign(isEnglish),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))

        if (entries.isEmpty()) {
            Text(
                text = "—",
                style = KmiTypography.cardTitle.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor,
                textAlign = profileTextAlign(isEnglish),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            entries.forEachIndexed { index, entry ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color =
                        if (isDarkMode) {
                            if (index % 2 == 0) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        } else {
                            if (index % 2 == 0) {
                                Color(0xFFDDEAFF)
                            } else {
                                Color(0xFFF3F7FF)
                            }
                        },
                    border = BorderStroke(
                        width = 1.dp,
                        color = dividerColor
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = profileHorizontalAlignment(isEnglish)
                    ) {
                        Text(
                            text = entry.branch.ifBlank { "—" },
                            style = KmiTypography.cardTitle.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = accentTextColor,
                            textAlign = profileTextAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = entry.address.ifBlank { "—" },
                            style = KmiTypography.body.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color =
                                if (isDarkMode) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    Color(0xFF374151)
                                },
                            textAlign = profileTextAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = profileTr(
                                isEnglish,
                                "קבוצה:",
                                "Group:"
                            ),
                            style = KmiTypography.secondary.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = labelColor,
                            textAlign = profileTextAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = profileHorizontalAlignment(isEnglish),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            entry.group
                                .split('\n', ',', ';', '|')
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .ifEmpty { listOf("—") }
                                .forEach { groupLine ->
                                    Text(
                                        text = groupLine,
                                        style = KmiTypography.body.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = valueColor,
                                        textAlign = profileTextAlign(isEnglish),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = profileTr(
                                isEnglish,
                                "מאמן:",
                                "Coach:"
                            ),
                            style = KmiTypography.secondary.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = labelColor,
                            textAlign = profileTextAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = entry.coach.ifBlank { "—" },
                            style = KmiTypography.body.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = valueColor,
                            textAlign = profileTextAlign(isEnglish),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (index != entries.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        HorizontalDivider(
            thickness = 1.dp,
            color = dividerColor
        )
    }
}

/**
 * שורת מידע סטנדרטית: תווית מימין וערך מודגש משמאל (RTL)
 */
@Composable
private fun LabeledValueBlock(
    label: String,
    value: String,
    isEnglish: Boolean
) {
    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment =
            profileHorizontalAlignment(isEnglish)
    ) {
        Text(
            text = label,
            style = KmiTypography.secondary.copy(
                fontWeight = FontWeight.Medium
            ),
            color =
                if (isDarkMode) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color(0xFF52627A)
                },
            textAlign = profileTextAlign(isEnglish),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = value,
            style = KmiTypography.cardTitle.copy(
                fontWeight = FontWeight.Bold
            ),
            color =
                if (isDarkMode) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color(0xFF111827)
                },
            textAlign = profileTextAlign(isEnglish),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        HorizontalDivider(
            thickness = 1.dp,
            color =
                if (isDarkMode) {
                    MaterialTheme.colorScheme.outline.copy(
                        alpha = 0.45f
                    )
                } else {
                    Color(0xFFBFD0E8)
                }
        )
    }
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? =
    when (this) {
        is android.app.Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/**
 * שורת סיסמה עם הצגה/הסתרה (טופ־לבל)
 */
@Composable
private fun PasswordRow(
    label: String,
    password: String,
    isEnglish: Boolean
) {
    var visible by remember {
        mutableStateOf(false)
    }

    val isDarkMode =
        MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val labelColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            Color(0xFF52627A)
        }

    val valueColor =
        if (isDarkMode) {
            MaterialTheme.colorScheme.onSurface
        } else {
            Color(0xFF111827)
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = KmiTypography.secondary.copy(
                fontWeight = FontWeight.Medium
            ),
            color = labelColor,
            textAlign = profileTextAlign(isEnglish)
        )

        Spacer(Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text =
                    if (visible) {
                        password
                    } else {
                        "••••••••"
                    },
                style = KmiTypography.cardTitle.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor,
                textAlign =
                    if (isEnglish) {
                        TextAlign.Right
                    } else {
                        TextAlign.Left
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false
                )
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = {
                    visible = !visible
                }
            ) {
                Icon(
                    imageVector =
                        if (visible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                    contentDescription =
                        if (visible) {
                            profileTr(
                                isEnglish,
                                "הסתר סיסמה",
                                "Hide password"
                            )
                        } else {
                            profileTr(
                                isEnglish,
                                "הצג סיסמה",
                                "Show password"
                            )
                        },
                    tint = labelColor,
                    modifier = Modifier.size(
                        KmiIconSize.medium
                    )
                )
            }
        }
    }
}

private fun createProfilePdf(
    context: Context,
    info: UserProfileInfo,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val document = PdfDocument()
    val page = document.startPage(
        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    )
    val canvas = page.canvas

    val navy = android.graphics.Color.rgb(2, 43, 74)
    val blue = android.graphics.Color.rgb(12, 78, 130)
    val lightBlue = android.graphics.Color.rgb(234, 246, 255)
    val softBlue = android.graphics.Color.rgb(244, 250, 255)
    val borderBlue = android.graphics.Color.rgb(191, 213, 232)
    val textDark = android.graphics.Color.rgb(15, 23, 42)

    val regular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regular,
        align: Paint.Align = if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        this.typeface = typeface
        textAlign = align
    }

    val titlePaint = paint(29f, android.graphics.Color.WHITE, bold)
    val subTitlePaint = paint(14f, android.graphics.Color.WHITE, regular)
    val sectionPaint = paint(17f, blue, bold)
    val labelPaint = paint(10.5f, blue, bold)
    val valuePaint = paint(13f, textDark, regular)
    val boldValuePaint = paint(13f, textDark, bold)
    val smallPaint = paint(9f, android.graphics.Color.rgb(80, 100, 120), regular)

    fun rightX() = pageWidth - margin
    fun leftX() = margin
    fun mainX() = if (isEnglish) leftX() else rightX()

    fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 12f,
        stroke: Boolean = false
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, p)
    }

    fun drawKmiLogo(cx: Float, cy: Float, radius: Float) {
        val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy }
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            typeface = bold
            textSize = radius * 0.62f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawCircle(cx, cy, radius, outer)
        canvas.drawCircle(cx, cy, radius - 4f, inner)
        canvas.drawText("KAMI", cx, cy + radius * 0.22f, text)
    }

    fun drawHeader() {
        canvas.drawColor(android.graphics.Color.WHITE)

        val diagonal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navy }
        val accent1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(36, 103, 158) }
        val accent2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(128, 183, 220) }

        val path = android.graphics.Path().apply {
            moveTo(pageWidth.toFloat(), 0f)
            lineTo(pageWidth.toFloat(), 122f)
            lineTo(178f, 122f)
            lineTo(238f, 0f)
            close()
        }
        canvas.drawPath(path, diagonal)

        canvas.drawPath(android.graphics.Path().apply {
            moveTo(208f, 122f)
            lineTo(224f, 122f)
            lineTo(284f, 0f)
            lineTo(268f, 0f)
            close()
        }, accent1)

        canvas.drawPath(android.graphics.Path().apply {
            moveTo(230f, 122f)
            lineTo(238f, 122f)
            lineTo(298f, 0f)
            lineTo(290f, 0f)
            close()
        }, accent2)

        drawKmiLogo(78f, 58f, 42f)

        titlePaint.textAlign = Paint.Align.RIGHT
        subTitlePaint.textAlign = Paint.Align.RIGHT

        canvas.drawText(tr("הפרופיל שלי", "My Profile"), pageWidth - 34f, 52f, titlePaint)
        canvas.drawText(tr("כרטיס אישי למתאמן", "Personal trainee card"), pageWidth - 34f, 78f, subTitlePaint)

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            tr("תאריך הפקה:", "Generated:") + " " +
                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
            pageWidth - 34f,
            142f,
            smallPaint
        )
    }

    fun drawFooter() {
        val footerY = 804f

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy
            strokeWidth = 2f
        }

        canvas.drawLine(0f, footerY, pageWidth.toFloat(), footerY, line)

        drawKmiLogo(38f, footerY + 22f, 13f)

        smallPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Together We Protect", 62f, footerY + 25f, smallPaint)

        smallPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(tr("עמוד 1 מתוך 1", "Page 1 of 1"), pageWidth / 2f, footerY + 25f, smallPaint)

        smallPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Krav Maga Israel", pageWidth - 66f, footerY + 18f, smallPaint)
        canvas.drawText("www.kmi.org.il", pageWidth - 66f, footerY + 31f, smallPaint)

        val flag = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(20, 85, 200)
        }

        canvas.drawRect(pageWidth - 48f, footerY + 14f, pageWidth - 20f, footerY + 18f, flag)
        canvas.drawRect(pageWidth - 48f, footerY + 28f, pageWidth - 20f, footerY + 32f, flag)
    }

    fun drawPersonalDetails(top: Float): Float {
        drawRoundRect(margin, top, pageWidth - margin, top + 166f, android.graphics.Color.WHITE, 12f)
        drawRoundRect(margin, top, pageWidth - margin, top + 166f, borderBlue, 12f, stroke = true)

        sectionPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(tr("פרטים אישיים", "Personal Details"), pageWidth - margin - 22f, top + 34f, sectionPaint)

        val mid = pageWidth / 2f
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderBlue
            strokeWidth = 1f
        }

        canvas.drawLine(margin + 14f, top + 52f, pageWidth - margin - 14f, top + 52f, divider)
        canvas.drawLine(mid, top + 48f, mid, top + 142f, divider)

        fun rightItem(label: String, value: String, y: Float) {
            labelPaint.textAlign = Paint.Align.RIGHT
            boldValuePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, pageWidth - margin - 54f, y, labelPaint)
            canvas.drawText(value.ifBlank { "—" }, pageWidth - margin - 54f, y + 20f, boldValuePaint)
        }

        fun leftItem(label: String, value: String, y: Float) {
            labelPaint.textAlign = Paint.Align.RIGHT
            valuePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, mid - 48f, y, labelPaint)
            canvas.drawText(value.ifBlank { "—" }, mid - 48f, y + 20f, valuePaint)
        }

        rightItem(tr("שם", "Name"), info.userName, top + 76f)
        rightItem(tr("דרגה נוכחית", "Current rank"), info.belt, top + 116f)
        rightItem(tr("מתאמן לחגורה", "Training toward"), info.trainingTowardsBelt, top + 150f)

        leftItem(tr("מייל", "Email"), info.email, top + 76f)
        leftItem(tr("טלפון", "Phone"), info.phone, top + 116f)
        leftItem(tr("שם משתמש", "Username"), info.accountUserName, top + 150f)

        return top + 184f
    }

    fun drawBranches(top: Float): Float {
        sectionPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            tr("סניפים, קבוצות ומאמנים", "Branches, Groups and Coaches"),
            pageWidth / 2f,
            top,
            sectionPaint
        )

        var y = top + 26f

        info.branchEntries.forEachIndexed { index, entry ->
            val groups = entry.group
                .split('\n', ',', ';', '|')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf("—") }

            val cardHeight = 104f + groups.size * 13f

            drawRoundRect(
                margin,
                y,
                pageWidth - margin,
                y + cardHeight,
                if (index % 2 == 0) lightBlue else softBlue,
                12f
            )
            drawRoundRect(margin, y, pageWidth - margin, y + cardHeight, borderBlue, 12f, true)

            val mid = pageWidth / 2f
            val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderBlue
                strokeWidth = 1f
            }
            canvas.drawLine(mid, y + 24f, mid, y + cardHeight - 18f, divider)

            sectionPaint.textAlign = Paint.Align.RIGHT
            sectionPaint.textSize = 13f
            canvas.drawText(entry.branch.ifBlank { "—" }, pageWidth - margin - 22f, y + 30f, sectionPaint)

            labelPaint.textAlign = Paint.Align.RIGHT
            boldValuePaint.textAlign = Paint.Align.RIGHT
            valuePaint.textAlign = Paint.Align.RIGHT

            canvas.drawText(tr("כתובת:", "Address:"), pageWidth - margin - 22f, y + 66f, labelPaint)
            canvas.drawText(entry.address.ifBlank { "—" }.take(34), pageWidth - margin - 22f, y + 84f, valuePaint)

            canvas.drawText(tr("קבוצות:", "Groups:"), mid - 22f, y + 30f, labelPaint)

            var gy = y + 48f
            groups.forEach { group ->
                valuePaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("• ${group.take(18)}", mid - 28f, gy, valuePaint)
                gy += 15f
            }

            val coachLineY = y + cardHeight - 18f
            val coachDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderBlue
                strokeWidth = 1f
            }
            canvas.drawLine(margin + 36f, coachLineY - 24f, mid - 34f, coachLineY - 24f, coachDivider)

            labelPaint.textAlign = Paint.Align.RIGHT
            boldValuePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(tr("מאמן:", "Coach:"), mid - 92f, coachLineY, labelPaint)
            canvas.drawText(entry.coach.ifBlank { "—" }.take(18), mid - 132f, coachLineY, boldValuePaint)

            y += cardHeight + 7f
        }

        return y
    }

    drawHeader()

    var y = 136f
    y = drawPersonalDetails(y)
    drawBranches(y)

    drawFooter()
    document.finishPage(page)

    val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
    val file = File(dir, "my_profile_${System.currentTimeMillis()}.pdf")

    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }

    document.close()
    return file
}