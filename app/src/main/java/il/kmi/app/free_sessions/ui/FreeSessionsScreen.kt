package il.kmi.app.free_sessions.ui

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.kmi.app.ui.KmiTopBar
import il.kmi.shared.free_sessions.data.FreeSessionsRepository
import il.kmi.shared.free_sessions.data.freeSessionsRepository
import il.kmi.shared.free_sessions.model.FreeSession
import il.kmi.shared.free_sessions.model.FreeSessionPart
import il.kmi.shared.free_sessions.model.ParticipantState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeSessionsScreen(
    branch: String,
    groupKey: String,
    currentUid: String,
    currentName: String,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repo: FreeSessionsRepository = remember { freeSessionsRepository() }
    val vm = remember { FreeSessionsViewModel(repo) }
    val ctx = LocalContext.current   // ✅ חובה לטוסטים

    LaunchedEffect(branch, groupKey, currentUid, currentName) {
        vm.setContext(
            branch = branch,
            groupKey = groupKey,
            myUid = currentUid,
            myName = currentName
        )
    }

    val sessions by vm.upcoming.collectAsStateCompat()

    // ===== Create dialog =====
    var showCreate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }

    // ברירת מחדל: עוד שעה
    var startsAt by remember { mutableLongStateOf(System.currentTimeMillis() + 60 * 60 * 1000L) }

    // ===== Details sheet =====
    var selected by remember { mutableStateOf<FreeSession?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            KmiTopBar(
                title = "אימונים חופשיים",
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = false,
                lockSearch = true,
                lockHome = true,
                centerTitle = true
            )
        },
        floatingActionButton = {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0EA5E9),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                IconButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "יצירת אימון", tint = Color.White)
                }
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { p ->

        // ✅ חובה: RTL אמיתי לכל התוכן במסך (מסדר Row/Start/End/TopEnd וכו')
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF020617),
                                Color(0xFF111827),
                                Color(0xFF1D4ED8),
                                Color(0xFF22D3EE)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(p)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    HeaderCard(branch = branch, groupKey = groupKey, count = sessions.size)

                    if (sessions.isEmpty()) {
                        EmptyStateCard(
                            title = "אין עדיין אימונים מתוכננים",
                            subtitle = "אפשר ליצור אימון חדש ולשלוח הזמנה לכל המתאמנים בקבוצה."
                        )
                    } else {
                        // ✅ NEW: Delete confirm dialog state
                        var pendingDelete by remember { mutableStateOf<FreeSession?>(null) }

                        sessions.forEach { s ->
                            FreeSessionCard(
                                session = s,
                                onClick = { selected = s },

                                // ✅ NEW: רק יוצר האימון רואה עריכה/מחיקה
                                canManage = (s.createdByUid == currentUid),

                                // ✅ NEW: כרגע "עריכה" פשוט פותחת את ה-Details sheet
                                onEdit = { selected = s },

                                // ✅ NEW: דיאלוג אישור לפני מחיקה
                                onDelete = { pendingDelete = s }
                            )
                        }

                        // ✅ NEW: Confirm delete dialog
                        pendingDelete?.let { s ->
                            AlertDialog(
                                onDismissRequest = { pendingDelete = null },
                                title = {
                                    Text(
                                        text = "מחיקת אימון",
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                text = {
                                    Text(
                                        text = "למחוק את האימון \"${s.title}\"?",
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val sid = s.id
                                        scope.launch {
                                            val res = runCatching {
                                                repo.deleteFreeSession(
                                                    branch = branch,
                                                    groupKey = groupKey,
                                                    sessionId = sid
                                                )
                                            }

                                            if (res.isSuccess) {
                                                Toast.makeText(ctx, "האימון נמחק ✅", Toast.LENGTH_SHORT).show()
                                                pendingDelete = null
                                                selected = null // אם במקרה פתוח שיט של אותו אימון
                                            } else {
                                                val e = res.exceptionOrNull()
                                                android.util.Log.e("FREE_SESSIONS", "deleteFreeSession failed", e)
                                                val msg = e?.message?.takeIf { it.isNotBlank() } ?: "מחיקת אימון נכשלה"
                                                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                                                pendingDelete = null
                                            }
                                        }
                                    }) { Text("מחק") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { pendingDelete = null }) { Text("ביטול") }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(90.dp))
                }
            }
        }
    }

    // ===== Create dialog UI =====
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = {
                Text(
                    text = "יצירת אימון חדש",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                val scroll = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        // ✅ נותן מקום למקלדת, כדי שהצ'יפים לא ייחתכו
                        .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        label = { Text("כותרת") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { })
                    )

                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        singleLine = true,
                        label = { Text("מקום (אופציונלי)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { })
                    )

                    TimeQuickPicker(
                        startsAt = startsAt,
                        onPick = { startsAt = it }
                    )

                    Text(
                        text = "זמן שנבחר: ${fmtTimeHeb(startsAt)}",
                        color = Color(0xFF334155),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isBlank()) {
                        Toast.makeText(ctx, "נא להזין כותרת", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    scope.launch {
                        val result = runCatching {
                            repo.createFreeSession(
                                branch = branch,
                                groupKey = groupKey,
                                title = cleanTitle,
                                locationName = locationName.trim().ifBlank { null },
                                lat = null,
                                lng = null,
                                startsAt = startsAt,
                                createdByUid = currentUid,
                                createdByName = currentName
                            )
                        }

                        if (result.isSuccess) {
                            title = ""
                            locationName = ""
                            startsAt = System.currentTimeMillis() + 60 * 60 * 1000L
                            showCreate = false

                            Toast
                                .makeText(ctx, "האימון נוצר בהצלחה ✅", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            val e = result.exceptionOrNull()
                            android.util.Log.e("FREE_SESSIONS", "createFreeSession failed", e)

                            // אם זה PERMISSION_DENIED / חסר אינדקס / רשת וכו' – תראה לפחות טקסט מועיל
                            val msg = e?.message?.takeIf { it.isNotBlank() } ?: "יצירת אימון נכשלה"
                            Toast
                                .makeText(ctx, msg, Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }) {
                    Text("צור")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("ביטול") }
            }
        )
    }

    // ===== Details bottom sheet =====
    selected?.let { session ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheetState,
            containerColor = Color(0xFF0B1220)
        ) {
            FreeSessionDetailsSheet(
                repo = repo,
                session = session,
                branch = branch,
                groupKey = groupKey,
                currentUid = currentUid,
                currentName = currentName,
                onClose = { selected = null }
            )
        }
    }
}

/* ---------------- UI blocks ---------------- */

@Composable
private fun HeaderCard(
    branch: String,
    groupKey: String,
    count: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFF1E3A8A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Text(
                text = "סניף: ${branch.trim()}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "קבוצה: ${groupKey.trim()}",
                color = Color(0xFFBFDBFE),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Divider(color = Color(0xFF1F2937))

            Text(
                text = "אימונים עתידיים: $count",
                color = Color(0xFFECFEFF),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = subtitle,
                color = Color(0xFFE5E7EB),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FreeSessionCard(
    session: FreeSession,
    onClick: () -> Unit,
    canManage: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color(0xFF1D4ED8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            /* ---------- שורה עליונה: טקסט + פעולות ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                // ---- טקסטים (ימין) ----
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = session.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "נוצר ע״י ${session.createdByName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5F5),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---- פעולות (שמאל, גובה קבוע) ----
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .widthIn(min = 84.dp)        // ✅ לא ימחץ את הטקסט (מונע "שורה אנכית")
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF7DD3FC),
                        modifier = Modifier.size(20.dp)
                    )

                    if (canManage) {
                        IconButton(
                            onClick = { onEdit?.invoke() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "עריכה",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { onDelete?.invoke() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "מחיקה",
                                tint = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            }

            /* ---------- זמן + משתתפים (בלי שבירה אנכית) ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fmtTimeHeb(session.startsAt),
                    color = Color(0xFFBFDBFE),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.width(14.dp))

                Icon(Icons.Filled.Group, contentDescription = null, tint = Color(0xFF22D3EE))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${session.goingCount + session.onWayCount + session.arrivedCount + session.cantCount} משתתפים",
                    color = Color(0xFFE5E7EB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            /* ---------- מיקום (שמאל) ---------- */
            session.locationName?.takeIf { it.isNotBlank() }?.let { loc ->
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = Color(0xFFF97316))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = loc,
                            color = Color(0xFFE5E7EB),
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = "לחץ כדי לבחור סטטוס (מגיע / לא יכול / וכו׳)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Start,   // ✅ ב-RTL זה ימין
                modifier = Modifier.fillMaxWidth()
            )

            val total =
                (session.goingCount + session.onWayCount + session.arrivedCount + session.cantCount)
                    .coerceAtLeast(1)
            val progress by animateFloatAsState(
                (session.goingCount + session.onWayCount + session.arrivedCount).toFloat() / total
            )

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Color(0xFF22C55E),
                trackColor = Color(0xFF0B1220)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeSessionDetailsSheet(
    repo: FreeSessionsRepository,
    session: FreeSession,
    branch: String,
    groupKey: String,
    currentUid: String,
    currentName: String,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var parts by remember { mutableStateOf<List<FreeSessionPart>>(emptyList()) }
    var myState by remember { mutableStateOf<ParticipantState?>(null) }

    LaunchedEffect(session.id) {
        repo.observeParticipants(branch, groupKey, session.id).collectLatest { list ->
            parts = list
            myState = list.firstOrNull { it.uid == currentUid }?.state
        }
    }

    // ✅ חדש: יצירת לינק וויז (ניווט/שיתוף) לפי lat/lng אם קיימים, אחרת לפי שם מקום
    fun buildWazeUrl(navigate: Boolean): String {
        val nav = if (navigate) "true" else "false"
        val lat = runCatching { session.lat }.getOrNull()
        val lng = runCatching { session.lng }.getOrNull()
        val name = session.locationName?.trim().orEmpty()

        return when {
            lat != null && lng != null -> "https://waze.com/ul?ll=$lat,$lng&navigate=$nav"
            name.isNotBlank() -> "https://waze.com/ul?q=${Uri.encode(name)}&navigate=$nav"
            else -> "https://waze.com/ul"
        }
    }

    fun openWaze() {
        val lat = runCatching { session.lat }.getOrNull()
        val lng = runCatching { session.lng }.getOrNull()
        val name = session.locationName?.trim().orEmpty()

        val wazeDeepLink = when {
            lat != null && lng != null -> "waze://?ll=$lat,$lng&navigate=yes"
            name.isNotBlank() -> "waze://?q=${Uri.encode(name)}&navigate=yes"
            else -> "waze://"
        }

        val ok = runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(wazeDeepLink)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess

        if (!ok) {
            // fallback לדפדפן (יעבוד גם בלי האפליקציה)
            val url = buildWazeUrl(navigate = true)
            runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure {
                Toast.makeText(ctx, "לא הצלחתי לפתוח את וויז", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareWazeToGroup() {
        val url = buildWazeUrl(navigate = true)
        val title = session.title.trim()
        val place = session.locationName?.trim().orEmpty()

        val text = buildString {
            append("מיקום לאימון: ")
            if (place.isNotBlank()) append(place).append("\n")
            if (title.isNotBlank()) append("אימון: ").append(title).append("\n")
            append("וויז: ").append(url)
        }

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching {
            ctx.startActivity(Intent.createChooser(send, "שיתוף מיקום לקבוצה").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            Toast.makeText(ctx, "שיתוף נכשל", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = session.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "זמן: ${fmtTimeHeb(session.startsAt)}",
                    color = Color(0xFFBFDBFE),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(onClick = onClose) { Text("סגור") }
        }

        session.locationName?.takeIf { it.isNotBlank() }?.let { loc ->
            Text(
                text = "מקום: $loc",
                color = Color(0xFFE5E7EB),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            // ✅ חדש: כפתורי Waze (ניווט + שיתוף לקבוצה)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { shareWazeToGroup() }) {
                    Icon(Icons.Filled.Group, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("שתף לקבוצה")
                }
                FilledTonalButton(onClick = { openWaze() }) {
                    Icon(Icons.Filled.Place, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("פתח בוויז")
                }
            }
        }

        Divider(color = Color(0xFF1F2937))

        Text(
            text = "מה הסטטוס שלך?",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StateChip(
                text = "אני מגיע",
                selected = myState == ParticipantState.GOING,
                selectedColor = Color(0xFF22C55E),
                onClick = {
                    scope.launch {
                        repo.setParticipantState(branch, groupKey, session.id, currentUid, currentName, ParticipantState.GOING)
                        // TODO: תזמון תזכורות 30/10 דק׳ (נחבר אחרי שהכל מתקמפל)
                    }
                }
            )

            StateChip(
                text = "בדרך",
                selected = myState == ParticipantState.ON_WAY,
                selectedColor = Color(0xFF0EA5E9),
                onClick = {
                    scope.launch {
                        repo.setParticipantState(branch, groupKey, session.id, currentUid, currentName, ParticipantState.ON_WAY)
                    }
                }
            )

            StateChip(
                text = "הגעתי",
                selected = myState == ParticipantState.ARRIVED,
                selectedColor = Color(0xFF8B5CF6),
                onClick = {
                    scope.launch {
                        repo.setParticipantState(branch, groupKey, session.id, currentUid, currentName, ParticipantState.ARRIVED)
                    }
                }
            )

            StateChip(
                text = "לא יכול",
                selected = myState == ParticipantState.CANT,
                selectedColor = Color(0xFFEF4444),
                onClick = {
                    scope.launch {
                        repo.setParticipantState(branch, groupKey, session.id, currentUid, currentName, ParticipantState.CANT)
                        // TODO: ביטול תזכורות לאימון הזה
                    }
                }
            )
        } // ✅ סגירה נכונה של ה-Row של הצ'יפים

        Divider(color = Color(0xFF1F2937))

        Text(
            text = "מי מגיע?",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        if (parts.isEmpty()) {
            Text(
                text = "עדיין אין משתתפים.",
                color = Color(0xFFCBD5F5),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            parts.sortedByDescending { it.updatedAt }.take(50).forEach { p ->
                ParticipantRow(p)
                Divider(color = Color(0xFF0F172A))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StateChip(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor.copy(alpha = 0.22f),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF0B1220),
            labelColor = Color(0xFFE5E7EB)
        ),
        border = BorderStroke(1.dp, if (selected) selectedColor else Color(0xFF334155))
    )
}

@Composable
private fun ParticipantRow(p: FreeSessionPart) {
    val stateLabel = when (p.state) {
        ParticipantState.GOING -> "מגיע"
        ParticipantState.ON_WAY -> "בדרך"
        ParticipantState.ARRIVED -> "הגיע"
        ParticipantState.CANT -> "לא יכול"
        ParticipantState.INVITED -> "הוזמן"
    }

    val stateColor = when (p.state) {
        ParticipantState.GOING -> Color(0xFF22C55E)
        ParticipantState.ON_WAY -> Color(0xFF0EA5E9)
        ParticipantState.ARRIVED -> Color(0xFF8B5CF6)
        ParticipantState.CANT -> Color(0xFFEF4444)
        ParticipantState.INVITED -> Color(0xFF64748B)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                text = p.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stateLabel,
                color = stateColor,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(stateColor, CircleShape)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeQuickPicker(
    startsAt: Long,
    onPick: (Long) -> Unit
) {
    val scroll = rememberScrollState()

    // ✅ NEW: "אחר…" (בחירת תאריך+שעה חופשיים)
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var customDateMillis by remember { mutableStateOf<Long?>(null) }
    var customHour by remember { mutableStateOf(19) }
    var customMinute by remember { mutableStateOf(0) }

    fun isClose(a: Long, b: Long): Boolean =
        abs(a - b) <= 60_000L // דקה סבילות (כדי שה-"עוד שעה" יסומן נכון)

    val now = System.currentTimeMillis()
    val targetPlus1h = now + 60 * 60 * 1000L
    val targetPlus2h = now + 2 * 60 * 60 * 1000L
    val targetToday20 = forceTodayAtHour(20)
    val targetTomorrow18 = forceTomorrowAtHour(18)

    fun buildMillisFromDateAndTime(dateMillis: Long, hour: Int, minute: Int): Long {
        val z = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(dateMillis).atZone(z).toLocalDate()
        return date
            .atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            .atZone(z)
            .toInstant()
            .toEpochMilli()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "בחר זמן במהירות",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF111827),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll, reverseScrolling = true),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
        ) {
            QuickTimeChip(
                text = "אחר…",
                selected = false
            ) {
                if (customDateMillis == null) customDateMillis = System.currentTimeMillis()
                showDatePicker = true
            }

            QuickTimeChip(
                text = "עוד שעה",
                selected = isClose(startsAt, targetPlus1h)
            ) {
                onPick(System.currentTimeMillis() + 60 * 60 * 1000L)
            }

            QuickTimeChip(
                text = "עוד 2 שעות",
                selected = isClose(startsAt, targetPlus2h)
            ) {
                onPick(System.currentTimeMillis() + 2 * 60 * 60 * 1000L)
            }

            QuickTimeChip(
                text = "היום בערב (20:00)",
                selected = isClose(startsAt, targetToday20)
            ) {
                onPick(forceTodayAtHour(20))
            }

            QuickTimeChip(
                text = "מחר (18:00)",
                selected = isClose(startsAt, targetTomorrow18)
            ) {
                onPick(forceTomorrowAtHour(18))
            }
        }
    }

    // ✅ DatePickerDialog
    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = customDateMillis ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    customDateMillis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("אישור") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("ביטול") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    // ✅ TimePicker dialog (AlertDialog)
    if (showTimePicker) {
        val tp = rememberTimePickerState(
            initialHour = customHour,
            initialMinute = customMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    customHour = tp.hour
                    customMinute = tp.minute
                    showTimePicker = false

                    val d = customDateMillis ?: System.currentTimeMillis()
                    onPick(buildMillisFromDateAndTime(d, customHour, customMinute))
                }) { Text("אישור") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("ביטול") }
            },
            text = { TimePicker(state = tp) }
        )
    }
}

@Composable
private fun QuickTimeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF0EA5E9).copy(alpha = 0.20f),
            selectedLabelColor = Color(0xFF0B1220),
            containerColor = Color(0xFFF1F5F9),
            labelColor = Color(0xFF0B1220)
        )
    )
}

/* ---------------- helpers ---------------- */

private fun fmtTimeHeb(millis: Long): String {
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val fmt = DateTimeFormatter.ofPattern("EEEE · d.M.yyyy · HH:mm", Locale("he", "IL"))
    return dt.format(fmt)
}

private fun forceTodayAtHour(hour24: Int): Long {
    val z = ZoneId.systemDefault()
    val now = Instant.now().atZone(z)
    val today = now.toLocalDate()
    val dt = today.atTime(hour24.coerceIn(0, 23), 0).atZone(z)
    return dt.toInstant().toEpochMilli()
}

private fun forceTomorrowAtHour(hour24: Int): Long {
    val z = ZoneId.systemDefault()
    val now = Instant.now().atZone(z)
    val tomorrow = now.toLocalDate().plusDays(1)
    val dt = tomorrow.atTime(hour24.coerceIn(0, 23), 0).atZone(z)
    return dt.toInstant().toEpochMilli()
}

/**
 * תחליף קטן כדי לא להוסיף פה import של collectAsStateWithLifecycle.
 * עובד טוב למסכים שלך.
 */
@Composable
private fun <T> StateFlow<T>.collectAsStateCompat() =
    collectAsState()
