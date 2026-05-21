@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.drawer

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.Logout
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.unit.sp
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import il.kmi.shared.localization.AppLanguage
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope


//===========================================================================

private const val FORUM_UNREAD_LIMIT = 100L

private fun forumLastReadKey(branch: String): String =
    "forum_last_read_at_${branch.trim()}"

@Composable
fun DrawerMenuCard(
    text: String,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        contentColor = textColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(24.dp),
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
    }
}

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
    onOpenAccessibility: () -> Unit = {},
    onOpenMembershipPayment: () -> Unit = {},
    onOpenContactUs: () -> Unit = {}
) {
    val contextLang = LocalContext.current
    val scope = rememberCoroutineScope()

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

            clean.contains("Profile", ignoreCase = true) ||
                    clean.contains("פרופיל") -> Icons.Filled.Person

            clean.contains("Language", ignoreCase = true) ||
                    clean.contains("שפה") -> Icons.Filled.Language

            clean.contains("Subscription", ignoreCase = true) ||
                    clean.contains("מנוי") -> Icons.Filled.WorkspacePremium

            clean.contains("Rate", ignoreCase = true) ||
                    clean.contains("דרגו") -> Icons.Filled.WorkspacePremium

            clean.contains("Users", ignoreCase = true) ||
                    clean.contains("משתמשים") -> Icons.Filled.Groups

            clean.contains("Logout", ignoreCase = true) ||
                    clean.contains("התנתקות") -> Icons.Outlined.Logout

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
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            shadowElevation = 2.dp,
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
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
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
            color = Color(0xFF25D366),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Black,
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

    // 🎬 דיאלוג סרטוני הדגמה
    var showDemoVideos by rememberSaveable { mutableStateOf(false) }

    // 📄💳 טפסים ותשלומים
    var showFormsPaymentsDialog by rememberSaveable { mutableStateOf(false) }
    var showFormsListDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(authUid) {
        if (authUid.isNullOrBlank()) {
            resolvedIsAdmin = null
            return@LaunchedEffect
        }

        val isAdm = runCatching { AdminAccess.isCurrentUserAdmin() }
            .getOrDefault(false)

        resolvedIsAdmin = isAdm
    }

    // רקע גרדיאנט אטום (ללא שקיפות)
    CompositionLocalProvider(LocalLayoutDirection provides drawerLayoutDirection) {
        val scroll = rememberScrollState()

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
        ) {

            @Composable
            fun DrawerLineItemHe(
                leading: (@Composable (() -> Unit))? = null,
                trailing: (@Composable (() -> Unit))? = null,
                title: String,
                subtitle: String? = null,
                onClick: () -> Unit,
                twoLineTitle: Boolean = false,
                titleTextStyle: TextStyle = MaterialTheme.typography
                    .titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        lineHeight = 19.sp
                    )
            ) {
                val autoIcon = drawerIconForTitle(title)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp)
                        .clickable(onClick = onClick)
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
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
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 13.sp,
                                        lineHeight = 16.sp,
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
                        color = Color.White.copy(alpha = 0.10f)
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
                twoLineTitle: Boolean = false,
                titleTextStyle: TextStyle = MaterialTheme.typography
                    .titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        lineHeight = 19.sp
                    )
            ) {
                val autoIcon = drawerIconForTitle(title)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 16.dp)
                        .clickable(onClick = onClick)
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
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
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 13.sp,
                                        lineHeight = 16.sp,
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
                        color = Color.White.copy(alpha = 0.10f)
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
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = onClick)
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
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
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
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
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.82f),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (showDivider) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color.White.copy(alpha = 0.12f)
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
                        .padding(start = 8.dp, end = 42.dp)
                        .clickable(onClick = onClick)
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFFFF8AD8),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
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
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.82f),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (showDivider) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color.White.copy(alpha = 0.12f)
                        )
                    }
                }
            }

            // עטיפה ב־Box כדי שנוכל ליישר את החץ לתחתית מעל התוכן
            Box(Modifier.fillMaxSize()) {

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(220)) +
                            slideInHorizontally(
                                initialOffsetX = { fullWidth ->
                                    if (isEnglish) fullWidth else -fullWidth
                                },
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                )
                            ),
                    exit = fadeOut(animationSpec = tween(160)) +
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth ->
                                    if (isEnglish) fullWidth else -fullWidth
                                },
                                animationSpec = tween(
                                    durationMillis = 240,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                )
                            )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroll)
                            .padding(start = 6.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // ←—— כותרת + כפתור X באותה שורה, ממוקמים מעט נמוך מה-status bar ——→
                        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = topInset + 12.dp)
                                .height(48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tr("תפריט", "Menu"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = tr("סגור תפריט", "Close menu"),
                                    tint = Color.White
                                )
                    }
                }

                Spacer(Modifier.height(8.dp))

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
                                            colors = listOf(
                                                Color(0xFF1E1B4B).copy(alpha = 0.92f),
                                                Color(0xFF312E81).copy(alpha = 0.78f),
                                                Color(0xFF1D4ED8).copy(alpha = 0.36f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFFF8AD8).copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .padding(vertical = 8.dp)
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
                                            text = tr("אזור מאמן", "Coach area"),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    thickness = 1.dp,
                                    color = Color.White.copy(alpha = 0.16f)
                                )

                                if (isEnglish) {
                                    CoachLineItemEn(
                                        title = "Attendance Report",
                                        icon = Icons.Filled.Assessment,
                                        onClick = {
                                            onClose()
                                            onOpenCoachAttendance()
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
                                        title = "דו״ח נוכחות",
                                        icon = Icons.Filled.Assessment,
                                        onClick = {
                                            onClose()
                                            onOpenCoachAttendance()
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

                            // מפריד אחד בולט בין אזור המאמן לאזור המתאמן
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 2.dp,
                                color = Color.White.copy(alpha = 0.32f)
                            )
                            Spacer(Modifier.height(14.dp))
                        }

                // ===== אזור מנהל – רק למנהל =====
                        if (resolvedIsAdmin == true) {
                            if (isEnglish) {
                                DrawerLineItemEn(
                                    title = "Manage Users",
                                    subtitle = "View all app users",
                                    onClick = {
                                        onClose()
                                        onOpenAdminUsers()
                                    }
                                )
                            } else {
                                DrawerLineItemHe(
                                    title = "ניהול משתמשים",
                                    subtitle = "צפייה בכל המשתמשים באפליקציה",
                                    onClick = {
                                        onClose()
                                        onOpenAdminUsers()
                                    }
                                )
                            }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.12f))
                    Spacer(Modifier.height(8.dp))
                }

                        // ===== כפתור ראשון: אודות אבי אביסידון =====
                        if (isEnglish) {
                            DrawerLineItemEn(
                                title = "About Avi Avisidon",
                                subtitle = "Head of the method",
                                titleTextStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.2).sp
                                ),
                                onClick = {
                                    onClose()
                                    onOpenAboutAvi()
                                }
                            )
                        } else {
                            DrawerLineItemHe(
                                title = "אודות אבי אביסידון",
                                subtitle = "ראש השיטה",
                                titleTextStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.2).sp
                                ),
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
                                titleTextStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.2).sp
                                ),
                                onClick = {
                                    onClose()
                                    onOpenAboutNetworkCoaches()
                                }
                            )
                        } else {
                            DrawerLineItemHe(
                                title = "אודות המאמנים ברשת",
                                subtitle = "דרגות, ותק, הכשרות והסמכות",
                                titleTextStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.2).sp
                                ),
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

                        val context = LocalContext.current
                        if (isEnglish) {
                            DrawerLineItemEn(
                                title = "Forms & Payments",
                                twoLineTitle = true,
                                onClick = {
                                    showFormsPaymentsDialog = true
                                }
                            )
                        } else {
                            DrawerLineItemHe(
                                title = "טפסים ותשלומים",
                                twoLineTitle = true,
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

                        // ✅ עריכת פרופיל — אותו מסך שהיה נפתח מכפתור "ערוך פרטים" בהגדרות
                        if (isEnglish) {
                            DrawerLineItemEn(
                                leading = {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                title = "Edit Profile",
                                subtitle = "Update your personal details",
                                onClick = {
                                    onClose()
                                    onOpenEditProfile()
                                }
                            )
                        } else {
                            DrawerLineItemHe(
                                leading = {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                title = "עריכת פרופיל",
                                subtitle = "עדכון פרטים אישיים",
                                onClick = {
                                    onClose()
                                    onOpenEditProfile()
                                }
                            )
                        }

                        if (isEnglish) {
                            DrawerLineItemEn(
                                leading = {
                                    Icon(
                                        imageVector = Icons.Filled.Language,
                                        contentDescription = null,
                                        tint = Color.White
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
                                        delay(180)

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
                                        tint = Color.White
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
                                        delay(180)

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
                                        imageVector = Icons.Outlined.Logout,
                                        contentDescription = null,
                                        tint = Color.White
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
                                        imageVector = Icons.Outlined.Logout,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                title = "התנתקות",
                                onClick = {
                                    onClose()
                                    onLogout()
                                }
                            )
                        }

                    Spacer(Modifier.height(8.dp))
                        Text(
                            text = "© KAMI",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB8C4DA),
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    Spacer(Modifier.height(8.dp))
                } // end Column
            } // end AnimatedVisibility

                // ─────────────────────────────────────────────
                // 📄💳 דיאלוג: טפסים ותשלומים
                // ─────────────────────────────────────────────
                if (showFormsPaymentsDialog) {
                    val ctx = LocalContext.current

                    AlertDialog(
                        onDismissRequest = { showFormsPaymentsDialog = false },
                        title = {
                            Text(
                                text = tr("טפסים ותשלומים", "Forms & Payments"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
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
                                    color = Color.White.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            text = tr("טפסים", "Forms"),
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = tr(
                                                "פתיחת טופס ההרשמה הקיים לעמותה",
                                                "Open the existing association registration form"
                                            ),
                                            color = Color.White.copy(alpha = 0.78f),
                                            style = MaterialTheme.typography.bodySmall
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
                                    color = Color.White.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            text = tr("תשלומים", "Payments"),
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = tr(
                                                "פתיחת טופס תשלום דמי חבר לעמותה",
                                                "Open the membership fee payment form"
                                            ),
                                            color = Color.White.copy(alpha = 0.78f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showFormsPaymentsDialog = false }) {
                                Text(tr("סגור", "Close"), color = Color.White)
                            }
                        },
                        containerColor = Color(0xFF0E1630),
                        titleContentColor = Color.White,
                        textContentColor = Color.White
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
                                Color.White.copy(alpha = 0.10f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (enabled) Color.White.copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.10f)
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
                                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.72f),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = cardTextAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = subtitle,
                                    color = if (enabled) {
                                        Color.White.copy(alpha = 0.78f)
                                    } else {
                                        Color.White.copy(alpha = 0.55f)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
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
                                    title = tr("טופס רישום לעמותה", "Association Registration Form"),
                                    subtitle = tr(
                                        "פתיחת טופס הרישום הקיים לעמותה",
                                        "Open the existing association registration form"
                                    ),
                                    enabled = true,
                                    onClick = {
                                        val uri = Uri.parse("https://10nokout.com/files/Kami-Register.pdf")
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
                                                    tr("לא ניתן לפתוח את הקובץ", "Unable to open the file"),
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
                            TextButton(onClick = { showFormsListDialog = false }) {
                                Text(tr("סגור", "Close"), color = Color.White)
                            }
                        },
                        containerColor = Color(0xFF0E1630),
                        titleContentColor = Color.White,
                        textContentColor = Color.White
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
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
                                placeholder = { Text(tr("חיפוש…", "Search…")) },
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
                                            val uri = Uri.parse(v.url)
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
                                                        tr("לא ניתן לפתוח את הסרטון", "Unable to open the video"),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }

                                            showDemoVideos = false
                                            onClose() // סוגר גם את התפריט אחרי פתיחה
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
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
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = v.titleFor(isEnglish),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Text(
                                                    text = v.source,
                                                    color = Color.White.copy(alpha = 0.75f),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.End,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Filled.OpenInNew,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDemoVideos = false }) {
                            Text(tr("סגור", "Close"), color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF0E1630), // מתאים לגרדיאנט שלך
                    titleContentColor = Color.White,
                    textContentColor = Color.White
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
                        0.6f to Color.White.copy(alpha = 0.07f),
                        1f to Color.White.copy(alpha = 0.15f)
                    )
                )
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF2E3A59).copy(alpha = alpha),
                    modifier = Modifier
                        .size(36.dp)
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

