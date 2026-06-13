@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.shared.domain.Belt
import il.kmi.shared.prefs.KmiPrefs
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
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
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.R


//-----------------------------------------------------------------------------

// ----- מודל נתונים להזנה נוחה -----
data class UserProfileInfo(
    val userName: String = "שם המשתמש",
    val belt: String = "חגורה XXX",

    // ✅ משמש לציור תמונת החגורה לפי צבע הדרגה הנוכחית
    val currentBeltId: String = "",

    // ✅ משמש לציור תמונת החגורה הבאה בכרטיס התחתון
    val trainingTowardsBeltId: String = "",

    val branch: String = "סניף - XXX",
    val branchAddress: String = "כתובת הסניף - XXX",
    val group: String = "קבוצה - XXX",
    val headCoach: String = "מאמן בכיר - איציק ביטון",
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

private fun profileBeltDrawableForRawId(rawId: String?): Int {
    return when (rawId?.trim().orEmpty()) {
        "white", "לבנה" -> R.drawable.belt_white
        "yellow", "צהובה" -> R.drawable.belt_yellow
        "orange", "כתומה" -> R.drawable.belt_orange
        "green", "ירוקה" -> R.drawable.belt_green
        "blue", "כחולה" -> R.drawable.belt_blue
        "brown", "חומה" -> R.drawable.belt_brown

        "black",
        "שחורה",
        "שחורה דאן 1",
        "black_dan_2",
        "black_dan_3",
        "black_dan_4",
        "black_dan_5",
        "black_dan_6",
        "black_dan_7",
        "black_dan_8",
        "black_dan_9",
        "black_dan_10" -> R.drawable.belt_black

        else -> R.drawable.belt_white
    }
}

private fun shareProfileScreenApp(
    ctx: Context,
    isEnglish: Boolean
) {
    val text = if (isEnglish) {
        "K.M.I app"
    } else {
        "אפליקציית K.M.I"
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    ctx.startActivity(
        Intent.createChooser(
            sendIntent,
            if (isEnglish) "Share" else "שיתוף"
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

    val langManager = remember(ctx) { AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val screenLayoutDirection = profileLayoutDirection(isEnglish)

    var firestoreProfile by remember {
        mutableStateOf(FirestoreProfileInfo())
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
                firestoreProfile = firestoreProfileFromMap(snap.data.orEmpty())

                val p = firestoreProfile

                // מיישר גם את SharedPreferences כדי ששאר המסכים ייהנו מהמידע.
                userSp.edit()
                    .putString("fullName", p.fullName)
                    .putString("email", p.email)
                    .putString("phone", p.phone)
                    .putString("branch", p.branch)
                    .putString("activeBranch", p.branch)
                    .putString("active_branch", p.branch)
                    .putString("group", p.group)
                    .putString("activeGroup", p.group)
                    .putString("active_group", p.group)
                    .putString("groupKey", p.group)
                    .putString("age_group", p.group)
                    .putString("belt", p.belt)
                    .putString("current_belt", p.belt)
                    .putString("user_role", p.role)
                    .apply()
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

        val password = prefStr(
            kmiPrefs.password,
            sp.getString("password", "")
        )

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
        fun splitBranches(s: String): List<String> =
            s.split('\n', '|', ';', ',').map { it.trim() }.filter { it.isNotEmpty() }
        val branchesList: List<String> = splitBranches(branchRaw)
        val primaryBranch: String = branchesList.firstOrNull().orEmpty()

        val group = TrainingCatalog.normalizeGroupName(
            prefStr(
                kmiPrefs.ageGroup,
                sp.getString("activeGroup", ""),
                sp.getString("active_group", ""),
                sp.getString("groupKey", ""),
                sp.getString("group_key", ""),
                sp.getString("age_group", ""),
                sp.getString("group", ""),
                userSp.getString("activeGroup", ""),
                userSp.getString("active_group", ""),
                userSp.getString("groupKey", ""),
                userSp.getString("group_key", ""),
                userSp.getString("age_group", ""),
                userSp.getString("group", ""),
                firestoreProfile.group
            )
        )

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

        // ✅ אימון הבא + מאמן – בודק את כל הסניפים של המשתמש ולא רק את הראשון
        val dbUpcoming = branchesList
            .mapNotNull { branchName ->
                nextTrainingFromDatabase(
                    branchName = branchName,
                    groupName = group
                )
            }
            .minByOrNull { it.cal.timeInMillis }

        val upcoming = if (dbUpcoming == null && branchesList.isNotEmpty()) {
            val savedRegion = prefStr(
                kmiPrefs.region,
                sp.getString("region", ""),
                userSp.getString("region", ""),
                firestoreProfile.region
            ).ifBlank { "השרון" }

            branchesList
                .asSequence()
                .mapNotNull { branchName ->
                    TrainingCatalog
                        .upcomingFor(
                            region = savedRegion,
                            branch = branchName,
                            group = group,
                            count = 1
                        )
                        .firstOrNull()
                }
                .minByOrNull { it.cal.timeInMillis }
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

        // --- תיקון: כתובות לסניפים מרובים (שורה לכל סניף) ---
        fun fallbackCityVenue(b: String): String {
            val parts = b.split('–', '-').map { it.trim() }
            val city  = parts.getOrNull(0)
            val venue = parts.getOrNull(1)
            return if (!city.isNullOrBlank() && !venue.isNullOrBlank()) "$venue, $city" else "—"
        }

        val branchDisplay: String = branchesList.joinToString("\n").ifBlank { "—" }

        val branchAddressResolved: String = if (branchesList.isEmpty()) {
            "—"
        } else {
            branchesList.joinToString("\n") { b ->
                val dbAddress = KmiDatabaseProvider
                    .branchByName(ctx, b)
                    ?.displayAddress(isEnglish = isEnglish)
                    ?.trim()
                    .orEmpty()

                if (dbAddress.isNotBlank() && dbAddress != b.trim()) {
                    dbAddress
                } else {
                    val mapped = TrainingCatalog.addressFor(b).trim()
                    if (mapped.isNotBlank() && mapped != b.trim()) mapped else fallbackCityVenue(b)
                }
            }
        }
        // --- סוף תיקון הכתובת ---

        val info = UserProfileInfo(
            userName = if (fullName.isNotBlank()) {
                fullName
            } else {
                username.ifBlank {
                    profileTr(isEnglish, "שם המשתמש", "User name")
                }
            },
            belt = beltHeb,
            currentBeltId = beltId,
            trainingTowardsBeltId = nextBeltId,
            branch = branchDisplay,
            branchAddress = branchAddressResolved,
            group = group.ifBlank { "—" },
            headCoach = profileTr(isEnglish, "איציק ביטון", "Itzik Biton"),
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
                        runCatching { onClose() }.onFailure {
                            backDispatcher?.onBackPressed()
                        }
                    },
                    showTopHome = false,
                    showTopSearch = false,
                    showBottomActions = true,
                    lockSearch = true,
                    centerTitle = true,
                    currentLang = if (isEnglish) "en" else "he"
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
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        color = Color(0xFFEAF2FF),
        contentColor = Color(0xFF111827),
        shape = shape,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFD8E3F5)
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
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.2).sp,
                                lineHeight = 30.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF111827),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Left
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = info.belt,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFF31528A)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
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
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.2).sp,
                                lineHeight = 30.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF111827),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = info.belt,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF31528A)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
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
                modifier = Modifier.fillMaxWidth(),
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
                    fontWeight = FontWeight.Bold
                )
            }

            // מפריד דק
            Spacer(Modifier.height(16.dp))
            Divider(color = Color(0xFFBFD0E8), thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            // ─────────────────────────────────────────────
            // שורות מידע בסגנון "תגית:" ואז הערך מתחת + מפריד
            // ─────────────────────────────────────────────
            val branchValue = info.branch
                .removePrefix("סניף -").removePrefix("סניף")
                .trim().ifBlank { "—" }

            val addrValue = info.branchAddress
                .removePrefix("כתובת הסניף -").removePrefix("כתובת הסניף")
                .trim().ifBlank { "—" }

            val groupValue = info.group
                .removePrefix("קבוצה -").removePrefix("קבוצה")
                .trim().ifBlank { "—" }

            val headCoachValue = info.headCoach
                .removePrefix("מאמן בכיר -").removePrefix("מאמן בכיר")
                .trim().ifBlank { "—" }

            val coachValue = info.coach
                .removePrefix("מאמן -").removePrefix("מאמן")
                .trim().ifBlank { "—" }

            val nextTrainingValue = info.nextTraining
                .removePrefix("אימון הבא -").removePrefix("אימון הבא")
                .trim().ifBlank { "—" }

            LabeledValueBlock(
                label = profileTr(isEnglish, "סניף:", "Branch:"),
                value = branchValue,
                isEnglish = isEnglish
            )
            LabeledValueBlock(
                label = profileTr(isEnglish, "כתובת הסניף:", "Branch address:"),
                value = addrValue,
                isEnglish = isEnglish
            )
            LabeledValueBlock(
                label = profileTr(isEnglish, "קבוצה:", "Group:"),
                value = groupValue,
                isEnglish = isEnglish
            )
            LabeledValueBlock(
                label = profileTr(isEnglish, "מאמן בכיר:", "Head coach:"),
                value = headCoachValue,
                isEnglish = isEnglish
            )
            LabeledValueBlock(
                label = profileTr(isEnglish, "מאמן:", "Coach:"),
                value = coachValue,
                isEnglish = isEnglish
            )
            LabeledValueBlock(
                label = profileTr(isEnglish, "אימון הבא:", "Next training:"),
                value = nextTrainingValue,
                isEnglish = isEnglish
            )

            // --- פרטי חשבון ---
            Spacer(Modifier.height(6.dp))

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
            Divider(color = Color(0xFFBFD0E8), thickness = 1.dp)
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
                color = Color(0xFFDDEAFF),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFBFD0E8)
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
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF52627A),
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = trainingTargetText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E3A8A)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
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

/**
 * שורת מידע סטנדרטית: תווית מימין וערך מודגש משמאל (RTL)
 */
@Composable
private fun LabeledValueBlock(
    label: String,
    value: String,
    isEnglish: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = profileHorizontalAlignment(isEnglish)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF52627A),
                fontWeight = FontWeight.Medium
            ),
            textAlign = profileTextAlign(isEnglish),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            ),
            textAlign = profileTextAlign(isEnglish),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Divider(color = Color(0xFFBFD0E8), thickness = 1.dp)
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
    var visible by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF52627A),
                fontWeight = FontWeight.Medium
            ),
            textAlign = profileTextAlign(isEnglish)
        )
        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (visible) password else "••••••••",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                ),
                textAlign = if (isEnglish) TextAlign.Right else TextAlign.Left
            )
            Spacer(Modifier.width(8.dp))   // תוקן
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) {
                        profileTr(isEnglish, "הסתר סיסמה", "Hide password")
                    } else {
                        profileTr(isEnglish, "הצג סיסמה", "Show password")
                    },
                    tint = Color(0xFF52627A)
                )
            }
        }
    }
}
