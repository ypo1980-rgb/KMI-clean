@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets
import il.kmi.app.ui.KmiTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class AdminDiagnosticLog(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val area: String = "",
    val severity: String = "info",
    val userRole: String = "unknown",
    val appVersion: String = "",
    val deviceModel: String = "",
    val language: String = "",
    val createdAt: Timestamp? = null
)

private data class AdminTopScreen(
    val screenName: String = "",
    val count: Int = 0
)

private enum class AdminDiagnosticsRange(
    val days: Int,
    val titleHe: String,
    val titleEn: String
) {
    Today(1, "היום", "Today"),
    Week(7, "7 ימים", "7 days"),
    Month(30, "30 ימים", "30 days")
}

private enum class AdminDiagnosticsType(
    val key: String,
    val titleHe: String,
    val titleEn: String
) {
    All("all", "הכל", "All"),
    Errors("error", "שגיאות", "Errors"),
    Login("login", "כניסות", "Login"),
    Search("search", "חיפוש", "Search"),
    Payments("payment", "תשלומים", "Payments"),
    Attendance("attendance", "נוכחות", "Attendance"),
    Push("push", "Push", "Push")
}

@Composable
fun AdminDiagnosticsScreen(
    isEnglish: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    var adminLogs by remember { mutableStateOf<List<AdminDiagnosticLog>>(emptyList()) }
    var googleAuthLogs by remember { mutableStateOf<List<AdminDiagnosticLog>>(emptyList()) }
    var topScreens by remember { mutableStateOf<List<AdminTopScreen>>(emptyList()) }

    var loadingAdminLogs by remember { mutableStateOf(true) }
    var loadingGoogleLogs by remember { mutableStateOf(true) }
    var loadingScreens by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRange by remember { mutableStateOf(AdminDiagnosticsRange.Week) }
    var selectedType by remember { mutableStateOf(AdminDiagnosticsType.All) }

    val logs = adminLogs + googleAuthLogs
    val loading = loadingAdminLogs || loadingGoogleLogs || loadingScreens

    DisposableEffect(Unit) {
        loadingAdminLogs = true
        loadingGoogleLogs = true
        loadingScreens = true
        errorMessage = null

        val adminRegistration = Firebase.firestore
            .collection("adminLogs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(300)
            .addSnapshotListener { snapshot, error ->
                loadingAdminLogs = false

                if (error != null) {
                    errorMessage = error.localizedMessage
                    adminLogs = emptyList()
                    return@addSnapshotListener
                }

                adminLogs = snapshot?.documents.orEmpty().map { doc ->
                    AdminDiagnosticLog(
                        id = doc.id,
                        type = doc.getString("type").orEmpty(),
                        title = doc.getString("title").orEmpty(),
                        message = doc.getString("message").orEmpty(),
                        area = doc.getString("area").orEmpty(),
                        severity = doc.getString("severity") ?: "info",
                        userRole = doc.getString("userRole") ?: "unknown",
                        appVersion = doc.getString("appVersion").orEmpty(),
                        deviceModel = doc.getString("deviceModel").orEmpty(),
                        language = doc.getString("language").orEmpty(),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
            }

        val googleRegistration = Firebase.firestore
            .collection("google_auth_diagnostics")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(300)
            .addSnapshotListener { snapshot, error ->
                loadingGoogleLogs = false

                if (error != null) {
                    errorMessage = error.localizedMessage
                    googleAuthLogs = emptyList()
                    return@addSnapshotListener
                }

                googleAuthLogs = snapshot?.documents.orEmpty().map { doc ->
                    val rawType = doc.getString("type")
                        ?: doc.getString("event")
                        ?: doc.getString("status")
                        ?: "google_auth"

                    val rawMessage = doc.getString("message")
                        ?: doc.getString("error")
                        ?: doc.getString("exception")
                        ?: doc.getString("reason")
                        ?: ""

                    val isError =
                        rawType.contains("fail", ignoreCase = true) ||
                                rawType.contains("error", ignoreCase = true) ||
                                rawType.contains("cancel", ignoreCase = true) ||
                                rawMessage.contains("fail", ignoreCase = true) ||
                                rawMessage.contains("error", ignoreCase = true) ||
                                rawMessage.contains("exception", ignoreCase = true)

                    AdminDiagnosticLog(
                        id = "google_${doc.id}",
                        type = "google_auth_$rawType",
                        title = if (isError) {
                            if (isEnglish) "Google sign-in issue" else "תקלה בכניסה עם Google"
                        } else {
                            if (isEnglish) "Google sign-in event" else "אירוע כניסה עם Google"
                        },
                        message = rawMessage.ifBlank {
                            if (isEnglish) "Google authentication diagnostic event"
                            else "אירוע אבחון של התחברות Google"
                        },
                        area = "google_auth",
                        severity = if (isError) "error" else "info",
                        userRole = doc.getString("userRole") ?: "unknown",
                        appVersion = doc.getString("appVersion").orEmpty(),
                        deviceModel = doc.getString("deviceModel")
                            ?: doc.getString("device")
                            ?: "",
                        language = doc.getString("language").orEmpty(),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
            }

        val screensRegistration = Firebase.firestore
            .collection("screen_views")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                loadingScreens = false

                if (error != null) {
                    // screen_views הוא נתון סטטיסטי משני.
                    // לא מציגים אותו כשגיאת מסך כללית כדי לא לזהם את הלוגים.
                    topScreens = emptyList()
                    return@addSnapshotListener
                }

                topScreens = snapshot?.documents.orEmpty()
                    .mapNotNull { doc ->
                        val name = doc.getString("screenName")
                            ?: doc.getString("screen")
                            ?: doc.getString("route")
                            ?: doc.id

                        val count = doc.getLong("count")?.toInt()
                            ?: doc.getLong("views")?.toInt()
                            ?: 0

                        if (name.isBlank() || count <= 0) {
                            null
                        } else {
                            AdminTopScreen(
                                screenName = name,
                                count = count
                            )
                        }
                    }
                    .sortedByDescending { it.count }
                    .take(10)
            }

        onDispose {
            adminRegistration.remove()
            googleRegistration.remove()
            screensRegistration.remove()
        }
    }

    val rangeStartMillis = remember(selectedRange) {
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(selectedRange.days.toLong())
    }

    val filteredLogs = remember(logs, selectedRange, selectedType) {
        logs.filter { log ->
            val createdMillis = log.createdAt?.toDate()?.time ?: 0L
            val inRange = createdMillis >= rangeStartMillis
            val inType = selectedType == AdminDiagnosticsType.All ||
                    log.type.contains(selectedType.key, ignoreCase = true) ||
                    log.area.contains(selectedType.key, ignoreCase = true) ||
                    log.severity.contains(selectedType.key, ignoreCase = true)
            inRange && inType
        }
    }

    val errorCount = filteredLogs.count {
        it.severity.equals("error", ignoreCase = true) ||
                it.type.contains("error", ignoreCase = true) ||
                it.type.contains("failed", ignoreCase = true)
    }
    val loginCount = filteredLogs.count { it.type.contains("login", ignoreCase = true) }
    val searchNoResultsCount = filteredLogs.count {
        it.type.contains("search", ignoreCase = true) &&
                it.type.contains("no", ignoreCase = true)
    }
    val successCount = filteredLogs.count {
        it.severity.equals("success", ignoreCase = true) ||
                it.type.contains("success", ignoreCase = true) ||
                it.type.contains("saved", ignoreCase = true)
    }

    Scaffold(
        topBar = {
            KmiTopBar(
                title = tr("מרכז בקרה ולוגים", "Control Center & Logs"),
                onHome = onHome,
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                lockSearch = false,
                centerTitle = true,
                onPickSearchResult = {}
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFFEFFBFF),
                            0.34f to Color(0xFFBDEEFF),
                            0.68f to Color(0xFF21A5DC),
                            1.00f to Color(0xFF006FAE)
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 12.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.72f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFF37B7E8).copy(alpha = 0.45f)
                        )
                    ) {
                        Text(
                            text = tr(
                                "ניתוח פעילות, תקלות ושימוש באפליקציה",
                                "Activity, errors and app diagnostics"
                            ),
                            color = Color(0xFF102033),
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminSummaryCard(
                            title = tr("אירועים", "Events"),
                            value = filteredLogs.size.toString(),
                            color = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f)
                        )
                        AdminSummaryCard(
                            title = tr("שגיאות", "Errors"),
                            value = errorCount.toString(),
                            color = Color(0xFFE11D48),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminSummaryCard(
                            title = tr("כניסות", "Logins"),
                            value = loginCount.toString(),
                            color = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f)
                        )
                        AdminSummaryCard(
                            title = tr("חיפוש ללא תוצאה", "No results"),
                            value = searchNoResultsCount.toString(),
                            color = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    AdminInsightsCard(
                        isEnglish = isEnglish,
                        errorCount = errorCount,
                        loginCount = loginCount,
                        searchNoResultsCount = searchNoResultsCount,
                        successCount = successCount
                    )
                }

                item {
                    TopScreensCard(
                        isEnglish = isEnglish,
                        screens = topScreens
                    )
                }

                item {
                    FilterRow(
                        isEnglish = isEnglish,
                        selectedRange = selectedRange,
                        onRangeSelected = { selectedRange = it },
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it }
                    )
                }

                when {
                    loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }

                    errorMessage != null -> {
                        item {
                            AdminStateCard(
                                icon = Icons.Filled.Assessment,
                                title = tr("לא ניתן לטעון לוגים", "Unable to load logs"),
                                message = errorMessage.orEmpty()
                            )
                        }
                    }

                    filteredLogs.isEmpty() -> {
                        item {
                            AdminStateCard(
                                icon = Icons.Filled.Assessment,
                                title = tr("אין אירועים להצגה", "No events to show"),
                                message = tr(
                                    "לא נמצאו לוגים בטווח והסינון שנבחרו.",
                                    "No logs were found for the selected range and filter."
                                )
                            )
                        }
                    }

                    else -> {
                        items(filteredLogs, key = { it.id }) { log ->
                            AdminLogCard(
                                log = log,
                                isEnglish = isEnglish
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = title,
                color = Color(0xFF102033),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminInsightsCard(
    isEnglish: Boolean,
    errorCount: Int,
    loginCount: Int,
    searchNoResultsCount: Int,
    successCount: Int
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    val insights = buildList {
        add(tr("נמצאו $loginCount אירועי כניסה בטווח שנבחר.", "$loginCount login events found."))
        add(tr("נמצאו $errorCount שגיאות או כשלונות.", "$errorCount errors or failures found."))
        add(
            tr(
                "נמצאו $searchNoResultsCount חיפושים ללא תוצאה.",
                "$searchNoResultsCount searches had no results."
            )
        )
        add(
            tr(
                "נמצאו $successCount פעולות שהסתיימו בהצלחה.",
                "$successCount successful actions found."
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF163524).copy(alpha = 0.58f),
        border = BorderStroke(1.dp, Color(0xFF7DFFB3).copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Assessment,
                    contentDescription = null,
                    tint = Color(0xFF7DFFB3),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = tr("תובנות מהירות", "Quick insights"),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            insights.forEach { insight ->
                Text(
                    text = "• $insight",
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    isEnglish: Boolean,
    selectedRange: AdminDiagnosticsRange,
    onRangeSelected: (AdminDiagnosticsRange) -> Unit,
    selectedType: AdminDiagnosticsType,
    onTypeSelected: (AdminDiagnosticsType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AdminDiagnosticsRange.values().forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { onRangeSelected(range) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.94f),
                        labelColor = Color(0xFF102033),
                        selectedContainerColor = Color(0xFFEDE4FF),
                        selectedLabelColor = Color(0xFF111827)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selectedRange == range) {
                            Color(0xFF7C4DFF)
                        } else {
                            Color(0xFF37B7E8).copy(alpha = 0.70f)
                        }
                    ),
                    label = {
                        Text(
                            text = if (isEnglish) range.titleEn else range.titleHe,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AdminDiagnosticsType.values().take(4).forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.94f),
                        labelColor = Color(0xFF102033),
                        selectedContainerColor = Color(0xFFEDE4FF),
                        selectedLabelColor = Color(0xFF111827)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selectedType == type) {
                            Color(0xFF7C4DFF)
                        } else {
                            Color(0xFF37B7E8).copy(alpha = 0.70f)
                        }
                    ),
                    label = {
                        Text(
                            text = if (isEnglish) type.titleEn else type.titleHe,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminLogCard(
    log: AdminDiagnosticLog,
    isEnglish: Boolean
) {
    val severityColor = when {
        log.severity.equals("error", ignoreCase = true) ||
                log.type.contains("failed", ignoreCase = true) -> Color(0xFFFF8A8A)

        log.type.contains("search", ignoreCase = true) -> Color(0xFFFFD166)
        log.type.contains("success", ignoreCase = true) ||
                log.type.contains("saved", ignoreCase = true) -> Color(0xFF7DFFB3)

        else -> Color(0xFF8FD3FF)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.42f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = severityColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (log.type.contains("search", ignoreCase = true)) {
                                Icons.Filled.Search
                            } else {
                                Icons.Filled.Assessment
                            },
                            contentDescription = null,
                            tint = severityColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.size(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                ) {
                    Text(
                        text = log.title.ifBlank { log.type.ifBlank { "Log event" } },
                        color = Color(0xFF102033),
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatLogTime(log.createdAt, isEnglish),
                        color = Color(0xFF475569),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (log.message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = log.message,
                    color = Color(0xFF1E293B),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.70f))
            Spacer(Modifier.height(8.dp))

            Text(
                text = buildString {
                    append(if (isEnglish) "Area: " else "אזור: ")
                    append(log.area.ifBlank { "-" })
                    append("  |  ")
                    append(if (isEnglish) "Role: " else "תפקיד: ")
                    append(log.userRole.ifBlank { "-" })
                    if (log.appVersion.isNotBlank()) {
                        append("  |  ")
                        append(if (isEnglish) "Version: " else "גרסה: ")
                        append(log.appVersion)
                    }
                },
                color = Color(0xFF475569),
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AdminStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.72f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TopScreensCard(
    isEnglish: Boolean,
    screens: List<AdminTopScreen>
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color(0xFF37B7E8).copy(alpha = 0.55f)),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Assessment,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.size(8.dp))

                Text(
                    text = tr("10 המסכים הכי נצפים", "Top 10 screens"),
                    color = Color(0xFF102033),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            if (screens.isEmpty()) {
                Text(
                    text = tr(
                        "אין עדיין נתוני צפייה במסכים.",
                        "No screen view data yet."
                    ),
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                screens.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            modifier = Modifier.width(26.dp),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = item.screenName,
                            color = Color(0xFF102033),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = if (isEnglish) TextAlign.Start else TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = item.count.toString(),
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            modifier = Modifier.width(52.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (index != screens.lastIndex) {
                        HorizontalDivider(
                            color = Color(0xFFCBD5E1).copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatLogTime(
    timestamp: Timestamp?,
    isEnglish: Boolean
): String {
    val date = timestamp?.toDate() ?: return if (isEnglish) "Unknown time" else "זמן לא ידוע"
    val now = System.currentTimeMillis()
    val diff = now - date.time

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> if (isEnglish) "Now" else "עכשיו"
        minutes < 60 -> if (isEnglish) "$minutes min ago" else "לפני $minutes דקות"
        hours < 24 -> if (isEnglish) "$hours hours ago" else "לפני $hours שעות"
        days < 7 -> if (isEnglish) "$days days ago" else "לפני $days ימים"
        else -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("he", "IL")).format(Date(date.time))
    }
}
