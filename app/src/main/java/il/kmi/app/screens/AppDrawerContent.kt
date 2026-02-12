@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.drawer

import android.content.Intent
import android.net.Uri
import android.util.Log
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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
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
    val title: String,
    val url: String,
    val source: String = "YouTube"
)

private val DEMO_VIDEOS = listOf(
    DemoVideo(
        id = "yt_byPfByvdjQE",
        title = "הגנה פנימית נגד בעיטה ישרה",
        url = "https://www.youtube.com/watch?v=byPfByvdjQE",
        source = "YouTube"
    ),
    DemoVideo(
        id = "yt_v3wY85y1b7U",
        title = "הגנה כנגד שיסוף",
        url = "https://www.youtube.com/shorts/v3wY85y1b7U",
        source = "YouTube"
    ),
    DemoVideo(
        id = "yt_psnF4X9g0L0",
        title = "הגנה כנגד מקל – צד מת",
        url = "https://www.youtube.com/shorts/psnF4X9g0L0",
        source = "YouTube"
    ),
    DemoVideo(
        id = "yt_YXzJxtIeSRU",
        title = "מספר תוקפים",
        url = "https://www.youtube.com/shorts/YXzJxtIeSRU",
        source = "YouTube"
    )
)

@Composable
fun AppDrawerContent(
    onOpenAboutNetwork: () -> Unit,
    onOpenAboutMethod: () -> Unit,
    onOpenAboutAvi: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenForum: () -> Unit,
    onOpenMyProfile: () -> Unit,
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
    isAdmin: Boolean = false,
    onOpenAdminUsers: () -> Unit = {},
    onOpenAccessibility: () -> Unit = {}
) {
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

    var resolvedIsAdmin by remember { mutableStateOf<Boolean?>(null) }
    // 🎬 דיאלוג סרטוני הדגמה
    var showDemoVideos by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(authUid) {
        if (authUid.isNullOrBlank()) {
            resolvedIsAdmin = null
            Log.d("KMI_ADMIN", "Drawer check skipped: uid=null (waiting for auth)")
            return@LaunchedEffect
        }

        Log.d("KMI_ADMIN", "Drawer check: uid=$authUid")

        val isAdm = runCatching { AdminAccess.isCurrentUserAdmin() }
            .onFailure { Log.e("KMI_ADMIN", "Admin check failed", it) }
            .getOrDefault(false)

        Log.d("KMI_ADMIN", "Drawer check: isAdmin=$isAdm")
        resolvedIsAdmin = isAdm
    }

    // רקע גרדיאנט אטום (ללא שקיפות)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E1630), // Navy
                        Color(0xFF1F2A52),
                        Color(0xFF2575BC)  // כחול מודרני
                    )
                )
            )
    ) {
        val scroll = rememberScrollState()

        @Suppress("UNUSED_PARAMETER")
        @Composable
        fun DrawerItem(
            leading: (@Composable (() -> Unit))? = null,
            title: String,
            subtitle: String? = null,
            onClick: () -> Unit,
            twoLineTitle: Boolean = false,
            titleTextStyle: TextStyle = MaterialTheme.typography
                .titleMedium.copy(color = Color.White, fontWeight = FontWeight.ExtraBold),
            alignRight: Boolean = false
        ) {
            val itemHeight = 68.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(itemHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onClick),
                color = Color(0x1AFFFFFF),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (alignRight) Arrangement.End else Arrangement.Start
                ) {
                    if (leading != null) {
                        Box(Modifier.padding(end = 12.dp)) { leading() }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = title,
                            style = titleTextStyle,
                            maxLines = if (twoLineTitle) 2 else 1,
                            softWrap = twoLineTitle,
                            textAlign = if (alignRight) TextAlign.Right
                            else if (twoLineTitle) TextAlign.Center
                            else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.72f)
                                ),
                                textAlign = if (alignRight) TextAlign.Right else TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

@Composable
        fun CoachOnlyItem(
            title: String,
            subtitle: String? = null,
            onClick: () -> Unit
        ) {
            // 👇 אותו גובה כמו DrawerItem
            val itemHeight = 68.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(itemHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onClick),
                color = Color(0xFFFF7AB8),   // ורוד מאמן
                contentColor = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        )
                    }
                }
            }
        }

        // עטיפה ב־Box כדי שנוכל ליישר את החץ לתחתית מעל התוכן
        Box(Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ←—— כותרת + כפתור X באותה שורה, ממוקמים מעט נמוך מה-status bar ——→
                val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = topInset + 12.dp, start = 0.dp, end = 8.dp)
                        .height(48.dp)
                ) {
                    Text(
                        "תפריט",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "סגור תפריט",
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                //------------------------------------------------------------------------
                // ===== כפתורי מאמן — ורק למאמן =====
                if (isCoach) {
                    // מפריד עליון של מקטע המאמן
                    Spacer(Modifier.height(8.dp))
                    Divider(color = Color.White.copy(alpha = 0.12f))
                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)   // ✅ מרווח קבוע בין הכפתורים
                    ) {
                        // כפתור: דו״ח נוכחות (מאמן)
                        CoachOnlyItem(
                            title = "דו״ח נוכחות",
                            onClick = {
                                onClose()
                                onOpenCoachAttendance()
                            }
                        )

                        // כפתור: שליחת הודעה לקבוצה (מאמן)
                        CoachOnlyItem(
                            title = "שליחת הודעה",
                            onClick = {
                                onClose()
                                onOpenCoachBroadcast()
                            }
                        )

                        // כפתור: רשימת מתאמנים (מאמן)
                        CoachOnlyItem(
                            title = "רשימת מתאמנים",
                            onClick = {
                                onClose()
                                onOpenCoachTrainees()
                            }
                        )

                        // 👇 חדש: כפתור מבחן פנימי לחגורה (רק למאמן)
                        CoachOnlyItem(
                            title = "מבחן פנימי לחגורה",
                            onClick = {
                                onClose()
                                onOpenCoachInternalExam()
                            }
                        )
                    }

                    // מפריד קטן כמו בשאר הפריטים
                    Spacer(Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.12f))
                    Spacer(Modifier.height(8.dp))
                }

                // ===== אזור מנהל – רק למנהל =====
                if (resolvedIsAdmin == true) {
                    DrawerItem(
                        title = "ניהול משתמשים",
                        subtitle = "צפייה בכל המשתמשים באפליקציה",
                        onClick = {
                            onClose()
                            onOpenAdminUsers()
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.12f))
                    Spacer(Modifier.height(8.dp))
                }

                // ===== כפתור ראשון: אודות אבי אביסידון =====
                DrawerItem(
                    title = "אודות אבי אביסידון",
                    subtitle = "ראש השיטה",
                    titleTextStyle = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,              // היה ברירת מחדל ~16.sp
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp      // מצופף טיפה – נכנס בשורה אחת
                    ),
                    onClick = {
                        onClose()
                        onOpenAboutAvi()
                    }
                )

                // --- אודות איציק ביטון (חדש) ---
                DrawerItem(
                    title = "אודות איציק ביטון",
                    subtitle = "מאמן בכיר",
                    onClick = {
                        onClose()
                        onOpenAboutItzik()
                    }
                )

                // --- אודות הרשת ---
                DrawerItem(
                    title = "אודות הרשת",
                    subtitle = "נוקאאוט",
                    onClick = {
                        onClose()
                        onOpenAboutNetwork()
                    }
                )

                // --- אודות השיטה ---
                DrawerItem(
                    title = "אודות השיטה",
                    subtitle = "ק.מ.י",
                    onClick = {
                        onClose()
                        onOpenAboutMethod()
                    }
                )

                // --- תרגילים – הדגמה (חדש) ---
                DrawerItem(
                    title = "תרגילים – הדגמה",
                    subtitle = "סרטוני הסבר קצרים לתרגילים",
                    onClick = { showDemoVideos = true }
                )

                // --- טופס הרשמה לעמותה (PDF) ---
                val context = LocalContext.current
                DrawerItem(
                    title = "טופס הרשמה לעמותה",
                    twoLineTitle = true,
                    alignRight = true,                  // ← יישור לימין
                    onClick = {
                        val uri = Uri.parse("https://10nokout.com/files/Kami-Register.pdf")
                        try {
                            CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .setUrlBarHidingEnabled(true)
                                .build()
                                .launchUrl(context, uri)
                        } catch (_: Exception) {
                            try {
                                val i = Intent(Intent.ACTION_VIEW, uri)
                                    .addCategory(Intent.CATEGORY_BROWSABLE)
                                context.startActivity(i)
                            } catch (_: Exception) {
                                Toast.makeText(context, "לא ניתן לפתוח את הקובץ", Toast.LENGTH_SHORT).show()
                            }
                        }
                        onClose()
                    }
                )

                // --- פורום הסניף ---
                DrawerItem(
                    title = "פורום הסניף",
                    onClick = {
                        onClose()
                        onOpenForum()
                    }
                )

                // --- לוח אימונים חודשי (חדש) ---
                // DrawerItem(
                //     title = "לוח אימונים חודשי",
                //     subtitle = "האימונים של החודש",
                //     onClick = {
                //         onClose()
                //         onOpenMonthlyCalendar()
                //     }
                // )
                //
                // DrawerItem(
                //     title = "סיכום אימון",
                //     subtitle = "שמירה וצפייה בסיכומי אימון",
                //     onClick = {
                //         onClose()
                //         onOpenTrainingSummary()
                //     }
                // )

                // --- ניהול מנוי ---
                DrawerItem(
                    title = "ניהול מנוי",
                    onClick = {
                        onClose()
                        onOpenSubscriptions()
                    }
                )

                // ⭐ דרגו אותנו — אותו סגנון כמו הכפתורים האחרים
                DrawerItem(
                    title = "⭐ דרגו אותנו ⭐",
                    onClick = {
                        onClose()
                        onOpenRateUs()
                    }
                )

                // --- התנתקות (אותו סגנון כמו כל הכפתורים) ---
                DrawerItem(
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

                Spacer(Modifier.height(8.dp))
                Text(
                    "© K.M.I",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB8C4DA),
                    modifier = Modifier.align(Alignment.End)
                )
                Spacer(Modifier.height(8.dp))
            } // end Column

            // ─────────────────────────────────────────────
            // 🎬 דיאלוג: תרגילים – הדגמה
            // ─────────────────────────────────────────────
            if (showDemoVideos) {
                val ctx = LocalContext.current
                var query by rememberSaveable { mutableStateOf("") }

                val filtered = remember(query) {
                    val q = query.trim()
                    if (q.isBlank()) DEMO_VIDEOS
                    else DEMO_VIDEOS.filter {
                        it.title.contains(q, ignoreCase = true) ||
                                it.source.contains(q, ignoreCase = true)
                    }
                }

                AlertDialog(
                    onDismissRequest = { showDemoVideos = false },
                    title = {
                        Text(
                            text = "תרגילים – הדגמה",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Right,
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
                                placeholder = { Text("חיפוש…") },
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
                                                    Toast.makeText(ctx, "לא ניתן לפתוח את הסרטון", Toast.LENGTH_SHORT).show()
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
                                                    text = v.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Text(
                                                    text = v.source,
                                                    color = Color.White.copy(alpha = 0.75f),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    textAlign = TextAlign.Right,
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
                            Text("סגור", color = Color.White)
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
    }
}

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
                    contentDescription = "גלול למטה",
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

