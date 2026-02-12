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
import android.app.Activity
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex

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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // קריאה מה־Prefs (KmiPrefs מקור אמת; SP/UserSP פולבאק)
        val fullName = prefStr(kmiPrefs.fullName, sp.getString("fullName", ""))
        val email = prefStr(kmiPrefs.email, sp.getString("email", ""))
        val phone = prefStr(kmiPrefs.phone, sp.getString("phone", ""))
        val username = prefStr(kmiPrefs.username, sp.getString("username", ""))
        val password = prefStr(kmiPrefs.password, sp.getString("password", ""))

        val branchRaw = prefStr(kmiPrefs.branch, sp.getString("branch", ""))
        fun splitBranches(s: String): List<String> =
            s.split('\n', '|', ';', ',').map { it.trim() }.filter { it.isNotEmpty() }
        val branchesList: List<String> = splitBranches(branchRaw)
        val primaryBranch: String = branchesList.firstOrNull().orEmpty()

        val group = TrainingCatalog.normalizeGroupName(
            prefStr(kmiPrefs.ageGroup, sp.getString("age_group", ""), sp.getString("group", ""))
        )

        val beltId = prefStr(null, sp.getString("current_belt", ""), userSp.getString("belt_current", ""))
        val currentBelt = Belt.fromAny(beltId)
        val beltHeb = currentBelt?.heb ?: beltId.ifBlank { "לא הוגדר" }

        // ✅ אימון הבא + מאמן – משתמשים בסניף הראשי בלבד
        val upcoming = if (primaryBranch.isNotBlank())
            TrainingCatalog.upcomingFor("השרון", primaryBranch, group, count = 1).firstOrNull()
        else null

        val coachName: String = upcoming?.coach.orEmpty().ifBlank { "—" }

        val nextTraining: String = if (upcoming != null) {
            val fmtDay = java.text.SimpleDateFormat("EEEE", java.util.Locale("he", "IL"))
            val fmtTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale("he", "IL"))
            "${fmtDay.format(upcoming.cal.time)} • ${fmtTime.format(upcoming.cal.time)}\n${upcoming.place}"
        } else "—"

        // ✅ החגורה הבאה בתור (בלי המילה "חגורה" כי השורה למטה כבר אומרת "מתאמן לחגורה")
        val nextBeltText: String = Belt.nextOf(currentBelt ?: Belt.WHITE)
            ?.heb
            ?.removePrefix("חגורה ")
            ?: "—"

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
                val mapped = TrainingCatalog.addressFor(b).trim()
                if (mapped.isNotBlank() && mapped != b.trim()) mapped else fallbackCityVenue(b)
            }
        }
        // --- סוף תיקון הכתובת ---

        val info = UserProfileInfo(
            userName = if (fullName.isNotBlank()) fullName else username.ifBlank { "שם המשתמש" },
            belt = beltHeb,
            branch = branchDisplay,                    // ⬅️ כל הסניפים בשורות
            branchAddress = branchAddressResolved,     // ⬅️ כתובת לכל סניף בשורה
            group = group.ifBlank { "—" },
            headCoach = "איציק ביטון",
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.TopCenter)
                ) {
                    UserProfileCard(info = info)
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
            horizontalAlignment = Alignment.End
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
                textAlign = TextAlign.Right
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
                textAlign = TextAlign.Right
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

            LabeledValueBlock(label = "סניף:",        value = branchValue)
            LabeledValueBlock(label = "כתובת הסניף:", value = addrValue)
            LabeledValueBlock(label = "קבוצה:",       value = groupValue)
            LabeledValueBlock(label = "מאמן בכיר:",   value = headCoachValue)
            LabeledValueBlock(label = "מאמן:",        value = coachValue)
            LabeledValueBlock(label = "אימון הבא:",   value = nextTrainingValue)

            // --- פרטי חשבון ---
            Spacer(Modifier.height(6.dp))
            Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Spacer(Modifier.height(6.dp))

            LabeledValueBlock(label = "מייל:",       value = info.email)
            LabeledValueBlock(label = "טלפון:",      value = info.phone)
            LabeledValueBlock(label = "שם משתמש:",  value = info.accountUserName)
            PasswordRow(label = "סיסמה", password = info.password)

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
                    text = "מתאמן לחגורה",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Right
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
                            .trim().ifEmpty { info.trainingTowardsBelt },
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
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.End
    ) {
        // תגית
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.78f),
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Right,
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
            textAlign = TextAlign.Right,
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
    password: String
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
            textAlign = TextAlign.Right
        )
        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (visible) password else "••••••••",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Left
            )
            Spacer(Modifier.width(8.dp))   // תוקן
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "הסתר סיסמה" else "הצג סיסמה",
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
