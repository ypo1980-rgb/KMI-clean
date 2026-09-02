@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens.drawer

import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import il.kmi.app.screens.admin.AdminAccess
import il.kmi.app.ui.KmiIconSize
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import il.kmi.shared.localization.AppLanguage
import il.kmi.app.ui.KmiTypography
import il.kmi.app.voicecommands.VoiceDrawerDestination
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.net.toUri
import il.yuval.ui.theme.kmiOnSuccessContainerColor
import il.yuval.ui.theme.kmiSectionHeaderBrush
import il.yuval.ui.theme.kmiSectionHeaderContentColor
import il.yuval.ui.theme.kmiSuccessColor
import il.yuval.ui.theme.kmiSuccessContainerColor
import kotlin.time.Duration.Companion.milliseconds

//===========================================================================

private const val FORUM_UNREAD_LIMIT = 100L

/**
 * מאפשר למנגנון הפקודות הקוליות להפעיל את אותן פעולות
 * שמופעלות בלחיצה על פריטי AppDrawerContent.
 */
object DrawerVoiceActionsBridge {

    private var handler: ((VoiceDrawerDestination) -> Boolean)? = null

    fun bind(
        newHandler: ((VoiceDrawerDestination) -> Boolean)?
    ) {
        handler = newHandler
    }

    fun perform(
        destination: VoiceDrawerDestination
    ): Boolean {
        return handler?.invoke(destination) ?: false
    }
}

private fun forumLastReadKey(branch: String): String =
    "forum_last_read_at_${branch.trim()}"

// ─────────────────────────────────────────────
// 🎬 סרטוני הדגמה (אפשר להוסיף עוד בהמשך)
// ─────────────────────────────────────────────
private data class DemoVideo(
    val id: String,
    val titleHe: String,
    val titleEn: String,
    val url: String,
    val source: String = "YouTube"
)

private fun DemoVideo.titleFor(isEnglish: Boolean): String =
    if (isEnglish) titleEn else titleHe

private val DEMO_VIDEOS = listOf(
    DemoVideo(
        id = "yt_byPfByvdjQE",
        titleHe = "הגנה פנימית נגד בעיטה ישרה",
        titleEn = "Inside defense against a straight kick",
        url = "https://www.youtube.com/watch?v=byPfByvdjQE",
        source = "YouTube"
    ),
    DemoVideo(
        id = "yt_v3wY85y1b7U",
        titleHe = "הגנה כנגד שיסוף",
        titleEn = "Defense against a slash attack",
        url = "https://www.youtube.com/shorts/v3wY85y1b7U",
        source = "YouTube"
    ),
    DemoVideo(
        id = "yt_psnF4X9g0L0",
        titleHe = "הגנה כנגד מקל – צד מת",
        titleEn = "Defense against a stick attack – dead side",
        url = "https://www.youtube.com/shorts/psnF4X9g0L0",
        source = "YouTube"
    ),
    DemoVideo(
        id = "yt_YXzJxtIeSRU",
        titleHe = "מספר תוקפים",
        titleEn = "Multiple attackers",
        url = "https://www.youtube.com/shorts/YXzJxtIeSRU",
        source = "YouTube"
    )
)

@Composable
fun AppDrawerContent(
    languageRefreshKey: Int = 0,
    isEnglish: Boolean,
    onLanguageChanged: (AppLanguage) -> Unit = {},
    onOpenAboutNetwork: () -> Unit,
    onOpenAboutMethod: () -> Unit,
    onOpenAboutAvi: () -> Unit,
    onOpenAboutNetworkCoaches: () -> Unit = {},
    onOpenSubscriptions: () -> Unit,
    onOpenForum: () -> Unit,
    onOpenMyProfile: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenAboutItzik: () -> Unit,
    onOpenMonthlyCalendar: () -> Unit,
    onOpenTrainingSummary: () -> Unit,
    onOpenRateUs: () -> Unit,
    onClose: () -> Unit,
    onLogout: () -> Unit = {},
    isCoach: Boolean = false,
    onOpenCoachAttendance: () -> Unit = {},
    onOpenCoachBroadcast: () -> Unit = {},
    onOpenCoachTrainees: () -> Unit = {},
    onOpenCoachInternalExam: () -> Unit = {},
    onOpenCoachPaymentsReport: () -> Unit = {},
    isAdmin: Boolean = false,
    onOpenAdminUsers: () -> Unit = {},
    onOpenAdminDiagnostics: () -> Unit = {},
    onOpenAccessibility: () -> Unit = {},
    onOpenMembershipPayment: () -> Unit = {},
    onOpenContactUs: () -> Unit = {}
) {
    val contextLang = LocalContext.current
    val scope = rememberCoroutineScope()

    val drawerContentColor =
        kmiSectionHeaderContentColor()

    val dialogContainerColor =
        MaterialTheme.colorScheme.tertiaryContainer

    val dialogContentColor =
        MaterialTheme.colorScheme.onTertiaryContainer

    // ✅ ה-Drawer לא מחליט לבד מה השפה.
    // הוא מקבל isEnglish ישירות מ-MainApp.
    val drawerLayoutDirection =
        if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val userSp = remember(contextLang) {
        contextLang.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    val drawerBranch = remember {
        userSp.getString("branch", "").orEmpty().trim()
    }

    var forumUnreadCount by remember { mutableIntStateOf(0) }

    fun drawerIconForTitle(title: String): androidx.compose.ui.graphics.vector.ImageVector? {
        val clean = title.trim()

        return when {
            clean.contains("Avi", ignoreCase = true) ||
                    clean.contains("אבי") -> Icons.Filled.Person

            clean.contains("Network Coaches", ignoreCase = true) ||
                    clean.contains("מאמנים") -> Icons.Filled.Groups

            clean.contains("Method", ignoreCase = true) ||
                    clean.contains("שיטה") -> Icons.Filled.WorkspacePremium

            clean.contains("Demo", ignoreCase = true) ||
                    clean.contains("הדגמה") -> Icons.Filled.PlayArrow

            clean.contains("Forms", ignoreCase = true) ||
                    clean.contains("Payments", ignoreCase = true) ||
                    clean.contains("טפסים") ||
                    clean.contains("תשלומים") -> Icons.Filled.Assessment

            clean.contains("Contact", ignoreCase = true) ||
                    clean.contains("צור קשר") -> Icons.Filled.Campaign

            clean.contains("Forum", ignoreCase = true) ||
                    clean.contains("פורום") -> Icons.Filled.Groups

            clean.contains("Edit Profile", ignoreCase = true) ||
                    clean.contains("עריכת פרופיל") ->
                Icons.Filled.Edit

            clean.contains("Profile", ignoreCase = true) ||
                    clean.contains("פרופיל") ->
                Icons.Filled.Person

            clean.contains("Calendar", ignoreCase = true) ||
                    clean.contains("לוח שנה") ->
                Icons.Filled.CalendarMonth

            clean.contains("Training Summary", ignoreCase = true) ||
                    clean.contains("סיכום אימון") ->
                Icons.Filled.Description

            clean.contains("Accessibility", ignoreCase = true) ||
                    clean.contains("נגישות") ->
                Icons.Filled.AccessibilityNew

            clean.contains("Language", ignoreCase = true) ||
                    clean.contains("שפה") -> Icons.Filled.Language

            clean.contains("Subscription", ignoreCase = true) ||
                    clean.contains("מנוי") -> Icons.Filled.WorkspacePremium

            clean.contains("Rate", ignoreCase = true) ||
                    clean.contains("דרגו") -> Icons.Filled.WorkspacePremium

            clean.contains("Users", ignoreCase = true) ||
                    clean.contains("משתמשים") -> Icons.Filled.Groups

            clean.contains("Logout", ignoreCase = true) ||
                    clean.contains("התנתקות") -> Icons.AutoMirrored.Outlined.Logout

            else -> null
        }
    }

    @Composable
    fun DrawerMenuIconBubble(
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        content: (@Composable () -> Unit)? = null
    ) {
        Surface(
            shape = CircleShape,
            color =
                kmiSectionHeaderContentColor()
                    .copy(alpha = 0.12f),
            border =
                BorderStroke(
                    1.dp,
                    kmiSectionHeaderContentColor()
                        .copy(alpha = 0.18f)
                ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.size(30.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (content != null) {
                    content()
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint =
                            kmiSectionHeaderContentColor(),
                        modifier = Modifier.size(KmiIconSize.small)
                    )
                }
            }
        }
    }

    @Composable
    fun DrawerUnreadBadge(
        count: Int,
        modifier: Modifier = Modifier
    ) {
        if (count <= 0) return

        val label = if (count > 99) "99+" else count.toString()

        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = kmiSuccessContainerColor(),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border =
                BorderStroke(
                    1.dp,
                    kmiSuccessColor()
                )
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = KmiTypography.caption.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color =
                        kmiOnSuccessContainerColor(),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }

    // 🔐 בדיקת אדמין שקטה (לא "נתקעים" על false אם uid היה null בזמן הבנייה)
    val auth = remember { FirebaseAuth.getInstance() }
    var authUid by remember { mutableStateOf(auth.currentUser?.uid) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { a ->
            authUid = a.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    DisposableEffect(drawerBranch, authUid) {
        if (drawerBranch.isBlank()) {
            forumUnreadCount = 0
            onDispose { }
        } else {
            val lastReadMillis = userSp.getLong(forumLastReadKey(drawerBranch), 0L)

            if (lastReadMillis <= 0L) {
                forumUnreadCount = 0
                onDispose { }
            } else {
                val registration = Firebase.firestore
                    .collection("branches")
                    .document(drawerBranch)
                    .collection("messages")
                    .whereGreaterThan("createdAt", Timestamp(java.util.Date(lastReadMillis)))
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(FORUM_UNREAD_LIMIT)
                    .addSnapshotListener { snap, error ->
                        if (error != null) {
                            forumUnreadCount = 0
                            return@addSnapshotListener
                        }

                        val currentUid = auth.currentUser?.uid ?: authUid

                        forumUnreadCount = snap?.documents
                            ?.count { doc ->
                                val authorUid = doc.getString("authorUid")
                                authorUid.isNullOrBlank() || authorUid != currentUid
                            }
                            ?: 0
                    }

                onDispose {
                    registration.remove()
                }
            }
        }
    }

    var resolvedIsAdmin by remember { mutableStateOf<Boolean?>(null) }

    val effectiveIsAdmin = isAdmin || resolvedIsAdmin == true

    // 🎬 דיאלוג סרטוני הדגמה
    var showDemoVideos by rememberSaveable {
        mutableStateOf(false)
    }

    // 📄💳 טפסים ותשלומים
    var showFormsPaymentsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showFormsListDialog by rememberSaveable {
        mutableStateOf(false)
    }

    /*
     * התנתקות קולית אינה מתבצעת מיד.
     * המשתמש חייב לאשר אותה בדיאלוג.
     */
    var showVoiceLogoutConfirmation by rememberSaveable {
        mutableStateOf(false)
    }

    /*
     * effectiveIsAdmin הוא מפתח של האפקט כדי שה־Bridge
     * יתחבר מחדש מיד לאחר השלמת בדיקת הרשאת המנהל.
     *
     * בלי המפתח הזה ה־handler עלול לשמור את הערך false
     * שנקלט בזמן הטעינה הראשונית.
     */
    DisposableEffect(
        isEnglish,
        effectiveIsAdmin,
        onOpenMyProfile,
        onOpenCoachAttendance,
        onOpenCoachBroadcast,
        onOpenCoachTrainees,
        onOpenCoachPaymentsReport,
        onOpenCoachInternalExam,
        onOpenAdminUsers,
        onOpenAdminDiagnostics,
        onOpenAboutAvi,
        onOpenAboutNetworkCoaches,
        onOpenAboutMethod,
        onOpenContactUs,
        onOpenForum,
        onOpenSubscriptions,
        onOpenRateUs,
        onLanguageChanged,
        onLogout,
        onClose
    ) {
        DrawerVoiceActionsBridge.bind { destination ->
            when (destination) {
                VoiceDrawerDestination.MY_PROFILE -> {
                    onClose()
                    onOpenMyProfile()
                    true
                }

                VoiceDrawerDestination.COACH_ATTENDANCE -> {
                    onClose()
                    onOpenCoachAttendance()
                    true
                }

                VoiceDrawerDestination.COACH_BROADCAST -> {
                    onClose()
                    onOpenCoachBroadcast()
                    true
                }

                VoiceDrawerDestination.COACH_TRAINEES -> {
                    onClose()
                    onOpenCoachTrainees()
                    true
                }

                VoiceDrawerDestination.COACH_PAYMENTS_REPORT -> {
                    onClose()
                    onOpenCoachPaymentsReport()
                    true
                }

                VoiceDrawerDestination.COACH_INTERNAL_EXAM -> {
                    onClose()
                    onOpenCoachInternalExam()
                    true
                }

                VoiceDrawerDestination.ADMIN_USERS -> {
                    if (!effectiveIsAdmin) {
                        false
                    } else {
                        onClose()
                        onOpenAdminUsers()
                        true
                    }
                }

                VoiceDrawerDestination.ADMIN_DIAGNOSTICS -> {
                    if (!effectiveIsAdmin) {
                        false
                    } else {
                        onClose()
                        onOpenAdminDiagnostics()
                        true
                    }
                }

                VoiceDrawerDestination.ABOUT_AVI -> {
                    onClose()
                    onOpenAboutAvi()
                    true
                }

                VoiceDrawerDestination.NETWORK_COACHES -> {
                    onClose()
                    onOpenAboutNetworkCoaches()
                    true
                }

                VoiceDrawerDestination.ABOUT_METHOD -> {
                    onClose()
                    onOpenAboutMethod()
                    true
                }

                VoiceDrawerDestination.EXERCISES_DEMO -> {
                    showDemoVideos = true
                    true
                }

                VoiceDrawerDestination.FORMS_AND_PAYMENTS -> {
                    showFormsPaymentsDialog = true
                    true
                }

                VoiceDrawerDestination.CONTACT_US -> {
                    onClose()
                    onOpenContactUs()
                    true
                }

                VoiceDrawerDestination.BRANCH_FORUM -> {
                    onClose()
                    onOpenForum()
                    true
                }

                VoiceDrawerDestination.LANGUAGE -> {
                    val newLanguage =
                        if (isEnglish) {
                            AppLanguage.HEBREW
                        } else {
                            AppLanguage.ENGLISH
                        }

                    onLanguageChanged(newLanguage)
                    onClose()
                    true
                }

                /*
                 * פקודה מפורשת לשפה תמיד מגדירה מחדש
                 * את השפה המבוקשת. לא מסתמכים על isEnglish,
                 * שעלול עדיין להכיל את הערך שלפני ההחלפה.
                 */
                VoiceDrawerDestination.LANGUAGE_HEBREW -> {
                    onLanguageChanged(
                        AppLanguage.HEBREW
                    )
                    onClose()
                    true
                }

                VoiceDrawerDestination.LANGUAGE_ENGLISH -> {
                    onLanguageChanged(
                        AppLanguage.ENGLISH
                    )
                    onClose()
                    true
                }

                VoiceDrawerDestination.MANAGE_SUBSCRIPTION -> {
                    onClose()
                    onOpenSubscriptions()
                    true
                }

                VoiceDrawerDestination.RATE_US -> {
                    onClose()
                    onOpenRateUs()
                    true
                }

                VoiceDrawerDestination.LOGOUT -> {
                    showVoiceLogoutConfirmation = true
                    true
                }
            }
        }

        onDispose {
            DrawerVoiceActionsBridge.bind(null)
        }
    }

    LaunchedEffect(authUid, isAdmin) {
        if (isAdmin) {
            resolvedIsAdmin = true
            return@LaunchedEffect
        }

        if (authUid.isNullOrBlank()) {
            resolvedIsAdmin = null
            return@LaunchedEffect
        }

        val isAdm = runCatching { AdminAccess.isCurrentUserAdmin() }
            .getOrDefault(false)

        resolvedIsAdmin = isAdm
    }

    // רקע כחול־נייבי אחיד ועדין לכל המגירה
    CompositionLocalProvider(
        LocalLayoutDirection provides drawerLayoutDirection
    ) {
        val scroll = rememberScrollState()

        // כשמחליפים שפה או מרעננים את הסרגל,
        // פותחים את התפריט מההתחלה ולא מאמצע הרשימה.
        LaunchedEffect(
            isEnglish,
            languageRefreshKey,
            isCoach,
            effectiveIsAdmin
        ) {
            scroll.scrollTo(0)
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush = kmiSectionHeaderBrush()
                    )
        ) {

            @Composable
            fun DrawerLineItemHe(
                leading: (@Composable (() -> Unit))? = null,
                trailing: (@Composable (() -> Unit))? = null,
                title: String,
                subtitle: String? = null,
                onClick: () -> Unit,
                titleTextStyle: TextStyle =
                    KmiTypography.cardTitle.copy(
                        color = drawerContentColor,
                        fontWeight = FontWeight.ExtraBold
                    )
            ) {
                val autoIcon = drawerIconForTitle(title)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(start = 16.dp, end = 16.dp)
                        .clickable(onClick = onClick)
                        .padding(top = 2.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (leading != null || autoIcon != null) {
                            DrawerMenuIconBubble(
                                icon = autoIcon,
                                content = leading
                            )
                            Spacer(Modifier.width(10.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title,
                                style = titleTextStyle,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = TextAlign.Right,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!subtitle.isNullOrBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    maxLines = 2,
                                    softWrap = true,
                                    overflow = TextOverflow.Ellipsis,
                                    style = KmiTypography.secondary.copy(
                                        color =
                                            drawerContentColor.copy(
                                                alpha = 0.72f
                                            ),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (trailing != null) {
                            Spacer(Modifier.width(8.dp))
                            trailing()
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = drawerContentColor.copy(alpha = 0.10f)
                    )
                }
            }

            @Composable
            fun DrawerLineItemEn(
                leading: (@Composable (() -> Unit))? = null,
                trailing: (@Composable (() -> Unit))? = null,
                title: String,
                subtitle: String? = null,
                onClick: () -> Unit,
                titleTextStyle: TextStyle =
                    KmiTypography.cardTitle.copy(
                        color = drawerContentColor,
                        fontWeight = FontWeight.ExtraBold
                    )
            ) {
                val autoIcon = drawerIconForTitle(title)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(start = 8.dp, end = 16.dp)
                        .clickable(onClick = onClick)
                        .padding(top = 2.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (leading != null || autoIcon != null) {
                            DrawerMenuIconBubble(
                                icon = autoIcon,
                                content = leading
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title,
                                style = titleTextStyle,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = TextAlign.Start,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!subtitle.isNullOrBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    maxLines = 2,
                                    softWrap = true,
                                    overflow = TextOverflow.Ellipsis,
                                    style = KmiTypography.secondary.copy(
                                        color = drawerContentColor.copy(alpha = 0.72f),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (trailing != null) {
                            Spacer(Modifier.width(8.dp))
                            trailing()
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color =
                            drawerContentColor.copy(alpha = 0.10f)
                    )
                }
            }

            @Composable
            fun CoachLineItemHe(
                title: String,
                subtitle: String? = null,
                icon: androidx.compose.ui.graphics.vector.ImageVector,
                showDivider: Boolean = true,
                onClick: () -> Unit
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = onClick)
                        .padding(top = 2.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawerMenuIconBubble(
                            icon = icon
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title,
                                style =
                                    KmiTypography.cardTitle.copy(
                                        color = drawerContentColor,
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    ),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!subtitle.isNullOrBlank()) {
                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = subtitle,
                                    style = KmiTypography.secondary.copy(
                                        color =
                                            drawerContentColor.copy(alpha = 0.82f),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    softWrap = true,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (showDivider) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = drawerContentColor.copy(alpha = 0.12f)
                        )
                    }
                }
            }

            @Composable
            fun CoachLineItemEn(
                title: String,
                subtitle: String? = null,
                icon: androidx.compose.ui.graphics.vector.ImageVector,
                showDivider: Boolean = true,
                onClick: () -> Unit
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(start = 8.dp, end = 42.dp)
                        .clickable(onClick = onClick)
                        .padding(top = 2.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(KmiIconSize.medium)
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title,
                                style =
                                    KmiTypography.cardTitle.copy(
                                        color = drawerContentColor,
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    ),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!subtitle.isNullOrBlank()) {
                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = subtitle,
                                    style = KmiTypography.secondary.copy(
                                        color =
                                            drawerContentColor.copy(alpha = 0.82f),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    softWrap = true,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (showDivider) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = drawerContentColor.copy(alpha = 0.12f)
                        )
                    }
                }
            }

            // עטיפה ב־Box כדי שנוכל ליישר את החץ לתחתית מעל התוכן
            Box(Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ←—— כותרת + כפתור X קבועים מעל אזור הגלילה ——→
                    val topInset =
                        WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding()

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = topInset + 8.dp
                                )
                                .heightIn(min = 42.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("תפריט", "Menu"),
                            style = KmiTypography.screenTitle,
                            color = drawerContentColor,
                            maxLines = 1
                        )
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = tr("סגור תפריט", "Close menu"),
                                tint = drawerContentColor,
                                modifier = Modifier.size(KmiIconSize.medium)
                            )
                        }
                    }

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(scroll)
                                .navigationBarsPadding()
                                .padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = 0.dp,
                                    bottom = 72.dp
                                ),
                        horizontalAlignment =
                            Alignment.Start
                    ) {

                        //------------------------------------------------------------------------
                        // ===== כפתורי מאמן — ורק למאמן =====
                        if (isCoach) {
                            Spacer(Modifier.height(8.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors =                                                 listOf(
                                                MaterialTheme.colorScheme
                                                    .primaryContainer
                                                    .copy(alpha = 0.92f),
                                                MaterialTheme.colorScheme
                                                    .primary
                                                    .copy(alpha = 0.78f),
                                                MaterialTheme.colorScheme
                                                    .secondary
                                                    .copy(alpha = 0.36f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Spacer(Modifier.width(10.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                    ) {
                                        Text(
                                            text = tr(
                                                "אזור מאמן",
                                                "Coach area"
                                            ),
                                            style =
                                                KmiTypography.sectionTitle.copy(
                                                    fontWeight =
                                                        FontWeight.Black
                                                ),
                                            color = drawerContentColor,
                                            textAlign =
                                                if (isEnglish) {
                                                    TextAlign.Start
                                                } else {
                                                    TextAlign.Right
                                                },
                                            maxLines = 1,
                                            modifier =
                                                Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp
                                    ),
                                    thickness = 1.dp,
                                    color = drawerContentColor.copy(alpha = 0.16f)
                                )

                                if (isEnglish) {
                                    CoachLineItemEn(
                                        title = "Mark Attendance",
                                        icon = Icons.Filled.Assessment,
                                        onClick = {
                                            onClose()

                                            runCatching {
                                                onOpenCoachAttendance()
                                            }.onFailure {
                                                Toast.makeText(
                                                    contextLang,
                                                    tr(
                                                        "לא ניתן לפתוח דו״ח נוכחות כרגע",
                                                        "Unable to open attendance report right now"
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                    CoachLineItemEn(
                                        title = "Send Message",
                                        icon = Icons.Filled.Campaign,
                                        onClick = {
                                            onClose()
                                            onOpenCoachBroadcast()
                                        }
                                    )
                                    CoachLineItemEn(
                                        title = "Trainees List",
                                        icon = Icons.Filled.Groups,
                                        onClick = {
                                            onClose()
                                            onOpenCoachTrainees()
                                        }
                                    )
                                    CoachLineItemEn(
                                        title = "Payments Report",
                                        icon = Icons.Filled.Assessment,
                                        onClick = {
                                            onClose()
                                            onOpenCoachPaymentsReport()
                                        }
                                    )
                                    CoachLineItemEn(
                                        title = "Internal Belt Exam",
                                        icon = Icons.Filled.WorkspacePremium,
                                        showDivider = false,
                                        onClick = {
                                            onClose()
                                            onOpenCoachInternalExam()
                                        }
                                    )
                                } else {
                                    CoachLineItemHe(
                                        title = "עדכון נוכחות",
                                        icon = Icons.Filled.Assessment,
                                        onClick = {
                                            onClose()

                                            runCatching {
                                                onOpenCoachAttendance()
                                            }.onFailure {
                                                Toast.makeText(
                                                    contextLang,
                                                    tr(
                                                        "לא ניתן לפתוח דו״ח נוכחות כרגע",
                                                        "Unable to open attendance report right now"
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                    CoachLineItemHe(
                                        title = "שליחת הודעה",
                                        icon = Icons.Filled.Campaign,
                                        onClick = {
                                            onClose()
                                            onOpenCoachBroadcast()
                                        }
                                    )
                                    CoachLineItemHe(
                                        title = "רשימת מתאמנים",
                                        icon = Icons.Filled.Groups,
                                        onClick = {
                                            onClose()
                                            onOpenCoachTrainees()
                                        }
                                    )
                                    CoachLineItemHe(
                                        title = "דו״ח תשלומים",
                                        icon = Icons.Filled.Assessment,
                                        onClick = {
                                            onClose()
                                            onOpenCoachPaymentsReport()
                                        }
                                    )
                                    CoachLineItemHe(
                                        title = "מבחן פנימי לחגורה",
                                        icon = Icons.Filled.WorkspacePremium,
                                        showDivider = false,
                                        onClick = {
                                            onClose()
                                            onOpenCoachInternalExam()
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                        }

                        // ===== אזור מנהל – רק למנהל =====
                        if (effectiveIsAdmin) {
                            Spacer(Modifier.height(6.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors =                                                 listOf(
                                                kmiSuccessContainerColor()
                                                    .copy(alpha = 0.96f),
                                                kmiSuccessColor()
                                                    .copy(alpha = 0.82f),
                                                kmiSuccessColor()
                                                    .copy(alpha = 0.42f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color =
                                            kmiSuccessColor()
                                                .copy(alpha = 0.24f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                    ) {
                                        Text(
                                            text = tr(
                                                "אזור מנהל",
                                                "Admin area"
                                            ),
                                            style =
                                                KmiTypography.sectionTitle.copy(
                                                    fontWeight =
                                                        FontWeight.Black
                                                ),
                                            color = drawerContentColor,
                                            textAlign =
                                                if (isEnglish) {
                                                    TextAlign.Start
                                                } else {
                                                    TextAlign.Right
                                                },
                                            maxLines = 1,
                                            modifier =
                                                Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp
                                    ),
                                    thickness = 1.dp,
                                    color = drawerContentColor.copy(alpha = 0.16f)
                                )

                                if (isEnglish) {
                                    CoachLineItemEn(
                                        title = "Manage Users",
                                        subtitle = "View all app users",
                                        icon = Icons.Filled.Groups,
                                        showDivider = true,
                                        onClick = {
                                            onClose()
                                            onOpenAdminUsers()
                                        }
                                    )

                                    CoachLineItemEn(
                                        title = "Control Center & Logs",
                                        subtitle = "Activity, errors and app diagnostics",
                                        icon = Icons.Filled.Assessment,
                                        showDivider = false,
                                        onClick = {
                                            onClose()
                                            onOpenAdminDiagnostics()
                                        }
                                    )
                                } else {
                                    CoachLineItemHe(
                                        title = "ניהול משתמשים",
                                        subtitle = "צפייה בכל המשתמשים\nבאפליקציה",
                                        icon = Icons.Filled.Groups,
                                        showDivider = true,
                                        onClick = {
                                            onClose()
                                            onOpenAdminUsers()
                                        }
                                    )

                                    CoachLineItemHe(
                                        title = "מרכז בקרה ולוגים",
                                        subtitle = "ניתוח פעילות, תקלות\nושימוש באפליקציה",
                                        icon = Icons.Filled.Assessment,
                                        showDivider = false,
                                        onClick = {
                                            onClose()
                                            onOpenAdminDiagnostics()
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }

                        // ===== אזור מתאמן =====
                        Spacer(Modifier.height(6.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors =                                             listOf(
                                            MaterialTheme.colorScheme
                                                .secondaryContainer
                                                .copy(alpha = 0.94f),
                                            MaterialTheme.colorScheme
                                                .secondary
                                                .copy(alpha = 0.82f),
                                            MaterialTheme.colorScheme
                                                .tertiary
                                                .copy(alpha = 0.42f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color =
                                        MaterialTheme.colorScheme
                                            .secondary
                                            .copy(alpha = 0.24f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Spacer(Modifier.width(10.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                ) {
                                    Text(
                                        text = tr(
                                            "אזור מתאמן",
                                            "Trainee area"
                                        ),
                                        style =
                                            KmiTypography.sectionTitle.copy(
                                                fontWeight =
                                                    FontWeight.Black
                                            ),
                                        color = drawerContentColor,
                                        textAlign =
                                            if (isEnglish) {
                                                TextAlign.Start
                                            } else {
                                                TextAlign.Right
                                            },
                                        maxLines = 1,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 4.dp
                                ),
                                thickness = 1.dp,
                                color = drawerContentColor.copy(alpha = 0.16f)
                            )

                            // ✅ הפרופיל שלי — מסך אמת שמציג נתונים מהמשתמש / Firestore / Preferences
                            if (isEnglish) {
                                DrawerLineItemEn(
                                    leading = {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = drawerContentColor
                                        )
                                    },
                                    title = "My Profile",
                                    subtitle = "View your personal K.M.I details",
                                    onClick = {
                                        onClose()
                                        onOpenMyProfile()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    leading = {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = drawerContentColor
                                        )
                                    },
                                    title = "הפרופיל שלי",
                                    subtitle = "צפייה בפרטים האישיים שלך",
                                    onClick = {
                                        onClose()
                                        onOpenMyProfile()
                                    }
                                )
                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "Edit Profile",
                                    subtitle = "Update your personal details",
                                    onClick = {
                                        onClose()
                                        onOpenEditProfile()
                                    }
                                )

                                DrawerLineItemEn(
                                    title = "Monthly Calendar",
                                    subtitle = "Trainings, holidays and summaries",
                                    onClick = {
                                        onClose()
                                        onOpenMonthlyCalendar()
                                    }
                                )

                                DrawerLineItemEn(
                                    title = "Training Summary",
                                    subtitle = "Add or view a training summary",
                                    onClick = {
                                        onClose()
                                        onOpenTrainingSummary()
                                    }
                                )

                                DrawerLineItemEn(
                                    title = "Accessibility",
                                    subtitle = "Display and accessibility settings",
                                    onClick = {
                                        onClose()
                                        onOpenAccessibility()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "עריכת פרופיל",
                                    subtitle = "עדכון הפרטים האישיים שלך",
                                    onClick = {
                                        onClose()
                                        onOpenEditProfile()
                                    }
                                )

                                DrawerLineItemHe(
                                    title = "לוח שנה חודשי",
                                    subtitle = "אימונים, חגים וסיכומים",
                                    onClick = {
                                        onClose()
                                        onOpenMonthlyCalendar()
                                    }
                                )

                                DrawerLineItemHe(
                                    title = "סיכום אימון",
                                    subtitle = "הוספה או צפייה בסיכום אימון",
                                    onClick = {
                                        onClose()
                                        onOpenTrainingSummary()
                                    }
                                )

                                DrawerLineItemHe(
                                    title = "נגישות",
                                    subtitle = "הגדרות תצוגה ונגישות",
                                    onClick = {
                                        onClose()
                                        onOpenAccessibility()
                                    }
                                )
                            }

                            // ===== כפתור ראשון: אודות אבי אביסידון =====
                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "About Avi Avisidon",
                                    subtitle = "Head of the method",
                                    onClick = {
                                        onClose()
                                        onOpenAboutAvi()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "אודות אבי אביסידון",
                                    subtitle = "ראש השיטה",
                                    onClick = {
                                        onClose()
                                        onOpenAboutAvi()
                                    }
                                )
                            }

                            // ===== אודות המאמנים ברשת =====
                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "About Network Coaches",
                                    subtitle = "Ranks, experience and certifications",
                                    onClick = {
                                        onClose()
                                        onOpenAboutNetworkCoaches()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "אודות המאמנים ברשת",
                                    subtitle = "דרגות, ותק, הכשרות והסמכות",
                                    onClick = {
                                        onClose()
                                        onOpenAboutNetworkCoaches()
                                    }
                                )
                            }

                            val showHiddenAboutItems = false

                            if (showHiddenAboutItems) {

                                if (isEnglish) {
                                    DrawerLineItemEn(
                                        title = "About Itzik Biton",
                                        subtitle = "Senior coach",
                                        onClick = {
                                            onClose()
                                            onOpenAboutItzik()
                                        }
                                    )
                                } else {
                                    DrawerLineItemHe(
                                        title = "אודות איציק ביטון",
                                        subtitle = "מאמן בכיר",
                                        onClick = {
                                            onClose()
                                            onOpenAboutItzik()
                                        }
                                    )
                                }

                                if (isEnglish) {
                                    DrawerLineItemEn(
                                        title = "About the Network",
                                        subtitle = "Knockout",
                                        onClick = {
                                            onClose()
                                            onOpenAboutNetwork()
                                        }
                                    )
                                } else {
                                    DrawerLineItemHe(
                                        title = "אודות הרשת",
                                        subtitle = "Knockout",
                                        onClick = {
                                            onClose()
                                            onOpenAboutNetwork()
                                        }
                                    )
                                }

                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "About the Method",
                                    subtitle = "KAMI",
                                    onClick = {
                                        onClose()
                                        onOpenAboutMethod()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "אודות השיטה",
                                    subtitle = "KAMI",
                                    onClick = {
                                        onClose()
                                        onOpenAboutMethod()
                                    }
                                )
                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "Exercises – Demo",
                                    subtitle = "Short demo videos for exercises",
                                    onClick = { showDemoVideos = true }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "תרגילים – הדגמה",
                                    subtitle = "סרטוני הסבר קצרים לתרגילים",
                                    onClick = { showDemoVideos = true }
                                )
                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "Forms & Payments",
                                    onClick = {
                                        showFormsPaymentsDialog = true
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "טפסים ותשלומים",
                                    onClick = {
                                        showFormsPaymentsDialog = true
                                    }
                                )
                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "Contact Us",
                                    subtitle = "Leave details and we will get back to you",
                                    onClick = {
                                        onClose()
                                        onOpenContactUs()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "צור קשר",
                                    subtitle = "השאירו פרטים ונחזור אליכם",
                                    onClick = {
                                        onClose()
                                        onOpenContactUs()
                                    }
                                )
                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "Branch Forum",
                                    trailing = {
                                        DrawerUnreadBadge(forumUnreadCount)
                                    },
                                    onClick = {
                                        onClose()
                                        onOpenForum()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "פורום הסניף",
                                    trailing = {
                                        DrawerUnreadBadge(forumUnreadCount)
                                    },
                                    onClick = {
                                        onClose()
                                        onOpenForum()
                                    }
                                )
                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    leading = {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = null,
                                            tint = drawerContentColor
                                        )
                                    },
                                    title = "Language / שפה",
                                    onClick = {
                                        val newLang = AppLanguage.HEBREW

                                        // ✅ רק MainApp שומר ומעדכן את ה-State.
                                        // לא שומרים כאן ישירות כדי למנוע כפילות וערכים ישנים.
                                        onLanguageChanged(newLang)

                                        onClose()

                                        scope.launch {
                                            delay(180.milliseconds)

                                            Toast.makeText(
                                                contextLang,
                                                "שפה: עברית",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    leading = {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = null,
                                            tint = drawerContentColor
                                        )
                                    },
                                    title = "שפה / Language",
                                    onClick = {
                                        val newLang = AppLanguage.ENGLISH

                                        // ✅ רק MainApp שומר ומעדכן את ה-State.
                                        // לא שומרים כאן ישירות כדי למנוע כפילות וערכים ישנים.
                                        onLanguageChanged(newLang)

                                        onClose()

                                        scope.launch {
                                            delay(180.milliseconds)

                                            Toast.makeText(
                                                contextLang,
                                                "Language: English",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "Manage Subscription",
                                    onClick = {
                                        onClose()
                                        onOpenSubscriptions()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "ניהול מנוי",
                                    onClick = {
                                        onClose()
                                        onOpenSubscriptions()
                                    }
                                )
                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "⭐ Rate Us ⭐",
                                    onClick = {
                                        onClose()
                                        onOpenRateUs()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "⭐ דרגו אותנו ⭐",
                                    onClick = {
                                        onClose()
                                        onOpenRateUs()
                                    }
                                )
                            }

                            if (isEnglish) {
                                DrawerLineItemEn(
                                    leading = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                                            contentDescription = null,
                                            tint = drawerContentColor
                                        )
                                    },
                                    title = "Logout",
                                    onClick = {
                                        onClose()
                                        onLogout()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    leading = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                                            contentDescription = null,
                                            tint = drawerContentColor
                                        )
                                    },
                                    title = "התנתקות",
                                    onClick = {
                                        onClose()
                                        onLogout()
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "© KAMI",
                            style = KmiTypography.caption,
                            color =
                                drawerContentColor.copy(alpha = 0.72f),
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    } // end scroll Column
                } // end drawer content Column

                // ─────────────────────────────────────────────
                // 📄💳 דיאלוג: טפסים ותשלומים
                // ─────────────────────────────────────────────
                if (showFormsPaymentsDialog) {
                    AlertDialog(
                        onDismissRequest = { showFormsPaymentsDialog = false },
                        title = {
                            Text(
                                text = tr("טפסים ותשלומים", "Forms & Payments"),
                                style = KmiTypography.screenTitle.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = drawerContentColor,
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        showFormsPaymentsDialog = false
                                        showFormsListDialog = true
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    color = drawerContentColor.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, drawerContentColor.copy(alpha = 0.18f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            text = tr("טפסים", "Forms"),
                                            color = drawerContentColor,
                                            style = KmiTypography.cardTitle.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = tr(
                                                "פתיחת טופס ההרשמה הקיים לעמותה",
                                                "Open the existing association registration form"
                                            ),
                                            color = drawerContentColor.copy(alpha = 0.78f),
                                            style = KmiTypography.secondary
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        showFormsPaymentsDialog = false
                                        onClose()
                                        onOpenMembershipPayment()
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    color = drawerContentColor.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, drawerContentColor.copy(alpha = 0.18f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            text = tr("תשלומים", "Payments"),
                                            color = drawerContentColor,
                                            style = KmiTypography.cardTitle.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = tr(
                                                "פתיחת טופס תשלום דמי חבר לעמותה",
                                                "Open the membership fee payment form"
                                            ),
                                            color = drawerContentColor.copy(alpha = 0.78f),
                                            style = KmiTypography.secondary
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showFormsPaymentsDialog = false
                                }
                            ) {
                                Text(
                                    text = tr("סגור", "Close"),
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = drawerContentColor
                                )
                            }
                        },
                        containerColor = dialogContainerColor,
                        titleContentColor = drawerContentColor,
                        textContentColor = drawerContentColor
                    )
                }

                // ─────────────────────────────────────────────
                // 📄 דיאלוג: רשימת טפסים
                // ─────────────────────────────────────────────
                if (showFormsListDialog) {
                    val ctx = LocalContext.current

                    @Composable
                    fun FormCard(
                        title: String,
                        subtitle: String,
                        enabled: Boolean,
                        onClick: () -> Unit = {}
                    ) {
                        val cardTextAlign = if (isEnglish) TextAlign.Start else TextAlign.End
                        Surface(
                            onClick = {
                                if (enabled) onClick()
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (enabled) {
                                drawerContentColor.copy(alpha = 0.10f)
                            } else {
                                drawerContentColor.copy(alpha = 0.06f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (enabled) drawerContentColor.copy(alpha = 0.18f)
                                else drawerContentColor.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (enabled) {
                                        drawerContentColor
                                    } else {
                                        drawerContentColor.copy(alpha = 0.72f)
                                    },
                                    style = KmiTypography.cardTitle.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    textAlign = cardTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = subtitle,
                                    color = if (enabled) {
                                        drawerContentColor.copy(alpha = 0.78f)
                                    } else {
                                        drawerContentColor.copy(alpha = 0.55f)
                                    },
                                    style = KmiTypography.secondary,
                                    textAlign = cardTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { showFormsListDialog = false },
                        title = {
                            Text(
                                text = tr("טפסים", "Forms"),
                                style = KmiTypography.screenTitle.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = drawerContentColor,
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FormCard(
                                    title = tr(
                                        "טופס רישום לעמותה",
                                        "Association Registration Form"
                                    ),
                                    subtitle = tr(
                                        "פתיחת טופס הרישום הקיים לעמותה",
                                        "Open the existing association registration form"
                                    ),
                                    enabled = true,
                                    onClick = {
                                        val uri =
                                            "https://10nokout.com/files/Kami-Register.pdf".toUri()
                                        try {
                                            CustomTabsIntent.Builder()
                                                .setShowTitle(true)
                                                .setUrlBarHidingEnabled(true)
                                                .build()
                                                .launchUrl(ctx, uri)
                                        } catch (_: Exception) {
                                            try {
                                                val i = Intent(Intent.ACTION_VIEW, uri)
                                                    .addCategory(Intent.CATEGORY_BROWSABLE)
                                                ctx.startActivity(i)
                                            } catch (_: Exception) {
                                                Toast.makeText(
                                                    ctx,
                                                    tr(
                                                        "לא ניתן לפתוח את הקובץ",
                                                        "Unable to open the file"
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                        showFormsListDialog = false
                                        onClose()
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showFormsListDialog = false
                                }
                            ) {
                                Text(
                                    text = tr("סגור", "Close"),
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = drawerContentColor
                                )
                            }
                        },
                        containerColor = dialogContainerColor,
                        titleContentColor = drawerContentColor,
                        textContentColor = drawerContentColor
                    )
                }

                // ─────────────────────────────────────────────
                // 🔐 אישור התנתקות שהופעלה בפקודה קולית
                // ─────────────────────────────────────────────
                if (showVoiceLogoutConfirmation) {
                    AlertDialog(
                        onDismissRequest = {
                            showVoiceLogoutConfirmation = false
                        },
                        title = {
                            Text(
                                text = tr(
                                    "אישור התנתקות",
                                    "Confirm logout"
                                ),
                                style = KmiTypography.screenTitle.copy(
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        },
                        text = {
                            Text(
                                text = tr(
                                    "האם אתה בטוח שברצונך להתנתק מהחשבון?",
                                    "Are you sure you want to log out?"
                                ),
                                style = KmiTypography.body
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showVoiceLogoutConfirmation = false
                                    onClose()
                                    onLogout()
                                }
                            ) {
                                Text(
                                    text = tr(
                                        "התנתק",
                                        "Log out"
                                    ),
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showVoiceLogoutConfirmation = false
                                }
                            ) {
                                Text(
                                    text = tr(
                                        "ביטול",
                                        "Cancel"
                                    ),
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    )
                }

                // ─────────────────────────────────────────────
                // 🎬 דיאלוג: תרגילים – הדגמה
                // ─────────────────────────────────────────────
                if (showDemoVideos) {
                    val ctx = LocalContext.current
                    var query by rememberSaveable { mutableStateOf("") }

                    val filtered = remember(query, isEnglish) {
                        val q = query.trim()
                        if (q.isBlank()) {
                            DEMO_VIDEOS
                        } else {
                            DEMO_VIDEOS.filter {
                                it.titleHe.contains(q, ignoreCase = true) ||
                                        it.titleEn.contains(q, ignoreCase = true) ||
                                        it.source.contains(q, ignoreCase = true)
                            }
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { showDemoVideos = false },
                        title = {
                            Text(
                                text = tr("תרגילים – הדגמה", "Exercises – Demo"),
                                style = KmiTypography.screenTitle.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = drawerContentColor,
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    singleLine = true,
                                    textStyle = KmiTypography.body.copy(
                                        color = drawerContentColor
                                    ),
                                    placeholder = {
                                        Text(
                                            text = tr("חיפוש…", "Search…"),
                                            style = KmiTypography.secondary,
                                            color = drawerContentColor.copy(
                                                alpha = 0.65f
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 360.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filtered, key = { it.id }) { v ->
                                        Surface(
                                            onClick = {
                                                val uri = v.url.toUri()
                                                try {
                                                    CustomTabsIntent.Builder()
                                                        .setShowTitle(true)
                                                        .setUrlBarHidingEnabled(true)
                                                        .build()
                                                        .launchUrl(ctx, uri)
                                                } catch (_: Exception) {
                                                    try {
                                                        ctx.startActivity(
                                                            Intent(Intent.ACTION_VIEW, uri)
                                                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                                        )
                                                    } catch (_: Exception) {
                                                        Toast.makeText(
                                                            ctx,
                                                            tr(
                                                                "לא ניתן לפתוח את הסרטון",
                                                                "Unable to open the video"
                                                            ),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }

                                                showDemoVideos = false
                                                onClose() // סוגר גם את התפריט אחרי פתיחה
                                            },
                                            shape = RoundedCornerShape(18.dp),
                                            color = drawerContentColor.copy(alpha = 0.10f),
                                            border = BorderStroke(
                                                1.dp,
                                                drawerContentColor.copy(alpha = 0.18f)
                                            ),
                                            tonalElevation = 0.dp,
                                            shadowElevation = 0.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = null,
                                                    tint = drawerContentColor,
                                                    modifier = Modifier.size(KmiIconSize.medium)
                                                )
                                                Spacer(Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = v.titleFor(isEnglish),
                                                        color = drawerContentColor,
                                                        style = KmiTypography.cardTitle.copy(
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = if (isEnglish) {
                                                            TextAlign.Start
                                                        } else {
                                                            TextAlign.End
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    Text(
                                                        text = v.source,
                                                        color = drawerContentColor.copy(alpha = 0.75f),
                                                        style = KmiTypography.caption,
                                                        textAlign = if (isEnglish) {
                                                            TextAlign.Start
                                                        } else {
                                                            TextAlign.End
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = null,
                                                    tint = drawerContentColor.copy(alpha = 0.85f),
                                                    modifier = Modifier.size(KmiIconSize.small)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDemoVideos = false
                                }
                            ) {
                                Text(
                                    text = tr("סגור", "Close"),
                                    style = KmiTypography.action.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = drawerContentColor
                                )
                            }
                        },
                        containerColor = dialogContainerColor, // מתאים לגרדיאנט שלך
                        titleContentColor = dialogContentColor,
                        textContentColor = dialogContentColor
                    )
                }

                // ←— רמז לגלילה בתחתית —→
                DrawerScrollAffordance(
                    scroll = scroll,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } // end inner Box
        } // end Box
    } // end CompositionLocalProvider
} // end AppDrawerContent

@Composable
private fun DrawerScrollAffordance(
    scroll: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val visible by remember { derivedStateOf { scroll.canScrollForward } }

    val floatAnim = rememberInfiniteTransition(label = "arrowFloat")
    val offsetY by floatAnim.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )
    val alpha by floatAnim.animateFloat(
        initialValue = 1f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.6f to
                                kmiSectionHeaderContentColor()
                                    .copy(alpha = 0.07f),
                        1f to
                                kmiSectionHeaderContentColor()
                                    .copy(alpha = 0.15f)
                    )
                )
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = CircleShape,
                color =
                    kmiSectionHeaderContentColor()
                        .copy(alpha = 0.92f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = alpha),
                    modifier = Modifier
                        .size(KmiIconSize.extraLarge)
                        .offset(y = offsetY.dp)
                        .clickable {
                            scope.launch {
                                val delta = 220
                                val target = (scroll.value + delta).coerceAtMost(scroll.maxValue)
                                try {
                                    scroll.animateScrollBy(delta.toFloat())
                                } catch (_: Throwable) {
                                    scroll.animateScrollTo(target)
                                }
                            }
                        }
                )
            }
        }
    }
}

