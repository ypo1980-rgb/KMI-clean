@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens

import android.content.Context
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.material.icons.filled.Close
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.database.KmiDatabaseProvider
import android.app.Activity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager

// ----- מודל נתונים להזנה נוחה -----
data class UserProfileInfo(
    val userName: String = "שם המשתמש",
    val belt: String = "חגורה XXX",
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
    onClose: () -> Unit   // חובה, לא אופציונלי
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

        // ✅ אימון הבא + מאמן – קודם branches.json, ואם אין התאמה fallback ל־TrainingCatalog
        val dbUpcoming = if (primaryBranch.isNotBlank()) {
            nextTrainingFromDatabase(primaryBranch, group)
        } else {
            null
        }

        val upcoming = if (dbUpcoming == null && primaryBranch.isNotBlank()) {
            val savedRegion = prefStr(
                kmiPrefs.region,
                sp.getString("region", ""),
                userSp.getString("region", "")
            ).ifBlank { "השרון" }

            TrainingCatalog.upcomingFor(savedRegion, primaryBranch, group, count = 1).firstOrNull()
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

        /// רקע + גלילה + כפתור X לסגירה
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0E1630),
                            Color(0xFF1F2A52),
                            Color(0xFF2575BC)
                        )
                    )
                )
                .verticalScroll(scroll)   // ✅ מאפשר גלילה
                .padding(20.dp)
        ) {
            val activity = LocalContext.current as? Activity
            val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = {
                        runCatching { onClose() }.onFailure {
                            backDispatcher?.onBackPressed()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(start = 6.dp, top = 6.dp)
                        .size(56.dp)
                        .zIndex(10f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "סגור",
                        tint = Color.White
                    )
                }

                if (isLoadingFirestoreProfile) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 10.dp)
                            .zIndex(11f),
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
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.TopCenter)
                ) {
                    UserProfileCard(
                        info = info,
                        isEnglish = isEnglish
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
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        color = Color(0x14FFFFFF), // שקיפות עדינה – אפקט זכוכית
        contentColor = Color.White,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = profileHorizontalAlignment(isEnglish)
        ) {
            // כותרת ראשית – שם המשתמש
            Text(
                text = info.userName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp,
                    lineHeight = 30.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = profileTextAlign(isEnglish)
            )

            // תת-כותרת – חגורה
            Spacer(Modifier.height(6.dp))
            Text(
                text = info.belt,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBFD7FF)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = profileTextAlign(isEnglish)
            )

            // מפריד דק
            Spacer(Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.16f), thickness = 1.dp)
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
            Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
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
            Divider(color = Color.White.copy(alpha = 0.10f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // שורת הדגשה תחתונה – “מתאמן לחגורה”
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profileTr(
                        isEnglish,
                        "מתאמן לחגורה",
                        "Training toward belt"
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = profileTextAlign(isEnglish)
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFBFD7FF).copy(alpha = 0.18f),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFBFD7FF).copy(alpha = 0.9f),
                                Color(0xFF7AB2FF).copy(alpha = 0.6f)
                            )
                        )
                    )
                ) {
                    Text(
                        text = info.trainingTowardsBelt
                            .removePrefix("מתאמן לחגורה")
                            .removePrefix("Training toward belt")
                            .trim()
                            .ifEmpty { info.trainingTowardsBelt },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDAE8FF)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        textAlign = TextAlign.Left
                    )
                }
            }
        }
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
        // תגית
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.78f),
                fontWeight = FontWeight.Medium
            ),
            textAlign = profileTextAlign(isEnglish),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        // ערך
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = profileTextAlign(isEnglish),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
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
                color = Color.White.copy(alpha = 0.78f),
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
                    color = Color.White
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
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
