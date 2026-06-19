@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package il.kmi.app.screens.coach

import android.app.Application
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import il.kmi.app.attendance.data.AttendanceRepository
import il.kmi.app.attendance.data.AttendanceStatus
import il.kmi.app.attendance.data.GroupMember
import il.kmi.app.ui.KmiTopBar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import android.app.Activity
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

//=========================================================================

data class CoachDateEntry(
    val date: String = "",
    val description: String = ""
)

private fun nextCoachDateItemName(
    sectionTitle: String,
    index: Int
): String {
    return when (sectionTitle) {
        "השתלמויות" -> "השתלמות $index"
        "מחנות אימונים" -> "מחנה אימונים $index"
        "הסמכות" -> "הסמכה $index"
        else -> "פריט $index"
    }
}

data class TraineeProfile(
    val id: String,
    val fullName: String,
    val belt: String,
    val seniority: String,
    val age: Int,
    val attendancePct: Int = 0,
    val branch: String = "",
    val groupKey: String = "",

    // ✅ איחוד מתאמנים כפולים לפי מייל / טלפון
    val email: String = "",
    val phone: String = "",

    // ✅ מזהה מסמך אמיתי ב-Firestore: users/{userDocId}
    // כל שמירה של תאריכים / הערות תתבצע לפיו ולא לפי שם.
    val userDocId: String = "",

    val beltAwardDates: Map<String, String> = emptyMap(),

    // ✅ תיאור חופשי לכל חגורה — נשמר בשרת תחת users/{docId}.beltAwardDescriptions
    val beltAwardDescriptions: Map<String, String> = emptyMap(),

    // ✅ הערות מאמן — נשמרות בשרת תחת users/{docId}.coachNotes
    val coachNotes: String = "",

    // ✅ שדות נוספים למילוי ע"י המאמן ונשמרים בשרת: תאריך + תיאור
    val seminarDates: Map<String, CoachDateEntry> = emptyMap(),
    val campDates: Map<String, CoachDateEntry> = emptyMap(),
    val certificationDates: Map<String, CoachDateEntry> = emptyMap()
)

data class GroupStatsUi(
    val totalTrainees: Int,
    val filteredTrainees: Int,
    val avgAge: Int,
    val avgAttendance: Int,
    val beltCounts: Map<String, Int>,
    val highAttendanceCount: Int,
    val avgSeniority: Double
)

private fun parseYearsFromSeniority(value: String): Int? {
    val digits = Regex("""\d+""").find(value)?.value ?: return null
    return digits.toIntOrNull()
}

private fun formatAvgSeniority(value: Double, isEnglish: Boolean): String {
    if (value <= 0.0) return "—"

    val formatted = String.format(Locale.US, "%.1f", value)

    return if (isEnglish) {
        "$formatted yrs"
    } else {
        "$formatted שנים"
    }
}

private fun beltColorForStats(belt: String): Color {
    val normalized = belt.trim()

    return when {
        normalized.contains("לבנ") -> Color(0xFFE5E7EB)
        normalized.contains("צהוב") -> Color(0xFFFACC15)
        normalized.contains("כתומ") -> Color(0xFFF97316)
        normalized.contains("ירוק") -> Color(0xFF22C55E)
        normalized.contains("כחול") -> Color(0xFF3B82F6)
        normalized.contains("חומ") -> Color(0xFF8B5A2B)
        normalized.contains("שחור") -> Color(0xFF111111)
        else -> Color(0xFF7C3AED)
    }
}

private fun coachTr(isEnglish: Boolean, he: String, en: String): String =
    if (isEnglish) en else he

private fun coachTextAlign(isEnglish: Boolean): TextAlign =
    if (isEnglish) TextAlign.Left else TextAlign.Right

private fun coachHorizontalAlignment(isEnglish: Boolean): Alignment.Horizontal =
    if (isEnglish) Alignment.Start else Alignment.End

private fun coachBeltNameForUi(
    beltName: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return beltName

    return when (beltName.trim()) {
        "לבנה" -> "White"
        "צהובה" -> "Yellow"
        "כתומה" -> "Orange"
        "ירוקה" -> "Green"
        "כחולה" -> "Blue"
        "חומה" -> "Brown"
        "שחורה" -> "Black"
        "ללא דרגה" -> "No rank"
        "חגורה לבנה" -> "White"
        "חגורה צהובה" -> "Yellow"
        "חגורה כתומה" -> "Orange"
        "חגורה ירוקה" -> "Green"
        "חגורה כחולה" -> "Blue"
        "חגורה חומה" -> "Brown"
        "חגורה שחורה" -> "Black"
        else -> beltName
    }
}

private fun coachSectionTitleForUi(
    title: String,
    isEnglish: Boolean
): String {
    return when (title) {
        "השתלמויות" -> coachTr(isEnglish, "השתלמויות", "Seminars")
        "מחנות אימונים" -> coachTr(isEnglish, "מחנות אימונים", "Training camps")
        "הסמכות" -> coachTr(isEnglish, "הסמכות", "Certifications")
        else -> title
    }
}

private fun coachDateItemNameForUi(
    itemName: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return itemName

    val number = Regex("""\d+""").find(itemName)?.value.orEmpty()

    return when {
        itemName.startsWith("השתלמות") -> "Seminar $number"
        itemName.startsWith("מחנה אימונים") -> "Training camp $number"
        itemName.startsWith("הסמכה") -> "Certification $number"
        else -> itemName
    }
}

private fun coachDateSectionIcon(title: String): String {
    return when (title) {
        "השתלמויות" -> "🎓"
        "מחנות אימונים" -> "👥"
        "הסמכות" -> "🏅"
        else -> "⌄"
    }
}

private fun coachDateSectionAccent(title: String): Color {
    return when (title) {
        "השתלמויות" -> Color(0xFF7C3AED)
        "מחנות אימונים" -> Color(0xFF2563EB)
        "הסמכות" -> Color(0xFF0891B2)
        else -> Color(0xFF6D56B8)
    }
}

private fun buildGroupStats(profiles: List<TraineeProfile>, filtered: List<TraineeProfile>): GroupStatsUi {
    val avgAge = filtered
        .map { it.age }
        .filter { it > 0 }
        .average()
        .takeIf { !it.isNaN() }
        ?.roundToInt()
        ?: 0

    val avgAttendance = filtered
        .map { it.attendancePct }
        .filter { it > 0 }
        .average()
        .takeIf { !it.isNaN() }
        ?.roundToInt()
        ?: 0

    val avgSeniority = filtered
        .mapNotNull { parseYearsFromSeniority(it.seniority)?.toDouble() }
        .filter { it > 0.0 }
        .average()
        .takeIf { !it.isNaN() }
        ?.let { (it * 10.0).roundToInt() / 10.0 }
        ?: 0.0

    val beltCounts = filtered
        .groupingBy { it.belt.ifBlank { "ללא דרגה" } }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .toMap()

    val highAttendanceCount = filtered.count { it.attendancePct >= 80 }

    return GroupStatsUi(
        totalTrainees = profiles.size,
        filteredTrainees = filtered.size,
        avgAge = avgAge,
        avgAttendance = avgAttendance,
        beltCounts = beltCounts,
        highAttendanceCount = highAttendanceCount,
        avgSeniority = avgSeniority
    )
}

@Composable
private fun CoachTopStatsCard(
    stats: GroupStatsUi,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isEnglish: Boolean,
    showSearch: Boolean = true
) {

    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp,
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.10f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = coachHorizontalAlignment(isEnglish)
        ) {
            Text(
                text = coachTr(isEnglish, "רשימת המתאמנים", "Trainees list"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 20.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = coachTextAlign(isEnglish),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = coachTr(
                    isEnglish,
                    "נתוני נוכחות וקבוצה בזמן אמת",
                    "attendance and group data"
                ),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                ),
                color = Color.White.copy(alpha = 0.72f),
                textAlign = coachTextAlign(isEnglish),
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CoachTopStatTile(
                        value = stats.totalTrainees.toString(),
                        label = coachTr(isEnglish, "מתאמנים", "Trainees"),
                        modifier = Modifier.weight(1f)
                    )

                    CoachTopStatTile(
                        value = stats.beltCounts
                            .filterValues { it > 0 }
                            .size
                            .toString(),
                        label = coachTr(isEnglish, "דרגות", "Ranks"),
                        modifier = Modifier.weight(1f)
                    )

                    CoachTopStatTile(
                        value = if (stats.avgAttendance > 0) "${stats.avgAttendance}%" else "—",
                        label = coachTr(isEnglish, "נוכחות", "Attendance"),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CoachTopStatTile(
                        value = if (stats.avgAge > 0) stats.avgAge.toString() else "—",
                        label = coachTr(isEnglish, "גיל ממוצע", "Avg age"),
                        modifier = Modifier.weight(1f)
                    )

                    CoachTopStatTile(
                        value = stats.highAttendanceCount.toString(),
                        label = coachTr(isEnglish, "נוכחות גבוהה", "High attendance"),
                        modifier = Modifier.weight(1f)
                    )

                    CoachTopStatTile(
                        value = formatAvgSeniority(stats.avgSeniority, isEnglish),
                        label = coachTr(isEnglish, "וותק ממוצע", "Avg seniority"),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        textAlign = coachTextAlign(isEnglish),
                        color = Color.White
                    ),
                    placeholder = {
                        Text(
                            text = coachTr(
                                isEnglish,
                                "חיפוש מתאמן",
                                "Search trainee"
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.62f)
                            )
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "🔎",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = coachTr(isEnglish, "נקה חיפוש", "Clear search"),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        onSearchQueryChange("")
                                    },
                                tint = Color.White.copy(alpha = 0.82f)
                            )
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.White.copy(alpha = 0.60f),
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFFA78BFA),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedPlaceholderColor = Color.White.copy(alpha = 0.62f),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.62f),
                        focusedLeadingIconColor = Color.White.copy(alpha = 0.82f),
                        unfocusedLeadingIconColor = Color.White.copy(alpha = 0.82f),
                        focusedTrailingIconColor = Color.White.copy(alpha = 0.82f),
                        unfocusedTrailingIconColor = Color.White.copy(alpha = 0.82f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CoachTopStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val iconText = when {
        label.contains("מתאמנים") || label.contains("Trainees") -> "👥"
        label.contains("דרגות") || label.contains("Ranks") -> "🎖️"
        label.contains("נוכחות גבוהה") || label.contains("High attendance") -> "✅"
        label.contains("נוכחות") || label.contains("Attendance") -> "📊"
        label.contains("גיל") || label.contains("Age") -> "🎂"
        label.contains("וותק") || label.contains("seniority", ignoreCase = true) -> "⏱️"
        else -> "⭐"
    }

    Surface(
        color = Color(0xFF243044),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.10f)
        ),
        modifier = modifier.heightIn(min = 72.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = iconText,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 17.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(1.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    lineHeight = 10.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.76f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PremiumCoachDateField(
    label: String,
    value: String,
    placeholder: String,
    accent: Color,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF8FAFC),
        shadowElevation = 4.dp,
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.72f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isEnglish) {
                            listOf(
                                Color.White,
                                accent.copy(alpha = 0.05f),
                                Color.White
                            )
                        } else {
                            listOf(
                                Color.White,
                                accent.copy(alpha = 0.05f),
                                Color.White
                            )
                        }
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅",
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    ),
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = coachTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = value.ifBlank { placeholder },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 20.sp
                    ),
                    color = if (value.isBlank()) {
                        Color(0xFF64748B)
                    } else {
                        Color(0xFF0F172A)
                    },
                    fontWeight = FontWeight.Black,
                    textAlign = coachTextAlign(isEnglish),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PremiumCoachDatePickerDialog(
    title: String,
    selectedDate: String,
    accent: Color,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val initialDate = remember(selectedDate) {
        runCatching { LocalDate.parse(selectedDate.trim()) }.getOrElse { LocalDate.now() }
    }

    var visibleMonth by remember(initialDate) {
        mutableStateOf(YearMonth.from(initialDate))
    }

    val selectedLocalDate = initialDate

    val firstDayOfMonth = remember(visibleMonth) {
        visibleMonth.atDay(1)
    }

    // Sunday = 0, Monday = 1 ... Saturday = 6
    val leadingEmptyDays = remember(firstDayOfMonth) {
        firstDayOfMonth.dayOfWeek.value % 7
    }

    val daysInMonth = remember(visibleMonth) {
        visibleMonth.lengthOfMonth()
    }

    val monthLocale = if (isEnglish) Locale.ENGLISH else Locale("he", "IL")

    val monthTitle = remember(visibleMonth, monthLocale) {
        visibleMonth.atDay(1)
            .format(DateTimeFormatter.ofPattern("MMMM yyyy", monthLocale))
    }

    val selectedTitle = remember(selectedLocalDate, monthLocale) {
        selectedLocalDate.format(
            DateTimeFormatter.ofPattern("EEEE • d MMMM yyyy", monthLocale)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .heightIn(max = 690.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color.Transparent,
            shadowElevation = 20.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF07152E),
                                Color(0xFF0B1E48),
                                Color(0xFF103C89),
                                Color(0xFF18BDEB)
                            )
                        )
                    )
                    .padding(1.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(29.dp),
                    color = Color(0xFF061832).copy(alpha = 0.96f)
                ) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (isEnglish) {
                            LayoutDirection.Ltr
                        } else {
                            LayoutDirection.Rtl
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = coachHorizontalAlignment(isEnglish)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.09f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                ) {
                                    Text(
                                        text = "📅",
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(9.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = coachHorizontalAlignment(isEnglish)
                                ) {
                                    Text(
                                        text = title,
                                        color = Color(0xFFBFDBFE),
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = coachTextAlign(isEnglish),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = selectedTitle,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 22.sp,
                                            lineHeight = 25.sp
                                        ),
                                        textAlign = coachTextAlign(isEnglish),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.14f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                                    shape = CircleShape,
                                    color = Color(0xFF0A234A),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isEnglish) "‹" else "›",
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Text(
                                    text = monthTitle,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 21.sp,
                                        lineHeight = 24.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )

                                Surface(
                                    onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                                    shape = CircleShape,
                                    color = Color(0xFF0A234A),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isEnglish) "›" else "‹",
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            val weekDays = if (isEnglish) {
                                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            } else {
                                listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                weekDays.forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        color = Color(0xFF67E8F9),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            val cells = buildList<Int?> {
                                repeat(leadingEmptyDays) { add(null) }
                                for (day in 1..daysInMonth) add(day)
                                while (size % 7 != 0) add(null)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                cells.chunked(7).forEach { week ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        week.forEach { day ->
                                            val cellDate = day?.let { visibleMonth.atDay(it) }
                                            val isSelected = cellDate == selectedLocalDate
                                            val isToday = cellDate == LocalDate.now()

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (day != null && cellDate != null) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .size(30.dp)
                                                            .clickable {
                                                                onDateSelected(cellDate.toString())
                                                                onDismiss()
                                                            },
                                                        shape = CircleShape,
                                                        color = when {
                                                            isSelected -> Color(0xFF22D3EE)
                                                            isToday -> Color.White.copy(alpha = 0.14f)
                                                            else -> Color.Transparent
                                                        },
                                                        border = when {
                                                            isSelected -> null
                                                            isToday -> BorderStroke(1.dp, Color(0xFF22D3EE))
                                                            else -> null
                                                        }
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = day.toString(),
                                                                color = if (isSelected) {
                                                                    Color(0xFF031226)
                                                                } else {
                                                                    Color.White
                                                                },
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 15.sp,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White.copy(alpha = 0.07f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = if (isEnglish) {
                                        Arrangement.End
                                    } else {
                                        Arrangement.Start
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Text(
                                            text = coachTr(isEnglish, "ביטול", "Cancel"),
                                            color = Color(0xFFBFDBFE),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Surface(
                                        onClick = {
                                            onDateSelected(LocalDate.now().toString())
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color(0xFF22D3EE),
                                        shadowElevation = 5.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = coachTr(isEnglish, "היום", "Today"),
                                                color = Color(0xFF04101F),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp
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
    val isEnglish = langManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val screenTextAlign = coachTextAlign(isEnglish)
    val screenHorizontalAlignment = coachHorizontalAlignment(isEnglish)

    // --- branch / groupKey שנעשה בהם שימוש בפועל ---
    var effectiveBranch by remember { mutableStateOf(branch) }
    var effectiveGroupKey by remember { mutableStateOf(groupKey) }

    // מנסים קודם מה-SharedPreferences, ואם לא – מה-Firestore (users/{uid})
    LaunchedEffect(Unit) {
        // 1. SharedPreferences – תומך במפתחות הישנים והחדשים
        if (effectiveBranch.isBlank()) {
            effectiveBranch =
                sp.getString("active_branch", null)
                    ?: sp.getString("activeBranch", null)
                            ?: sp.getString("branch", null)
                            ?: sp.getString("branchesCsv", null)
                            ?: sp.getString("coach_branch", null)
                            ?: sp.getString("selected_branch", null)
                            ?: sp.getString("current_branch", null)
                            ?: ""
        }

        if (effectiveGroupKey.isBlank()) {
            effectiveGroupKey =
                sp.getString("active_group", null)
                    ?: sp.getString("activeGroup", null)
                            ?: sp.getString("primaryGroup", null)
                            ?: sp.getString("groupKey", null)
                            ?: sp.getString("group_key", null)
                            ?: sp.getString("age_group", null)
                            ?: sp.getString("group", null)
                            ?: sp.getString("coach_groupKey", null)
                            ?: sp.getString("selected_groupKey", null)
                            ?: sp.getString("current_groupKey", null)
                            ?: ""
        }

        // 2. אם עדיין חסר – שליפה מ-Firestore users/{uid}
        if (effectiveBranch.isBlank() || effectiveGroupKey.isBlank()) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                try {
                    val snap = Firebase.firestore
                        .collection("users")
                        .document(uid)
                        .get()
                        .await()

                    if (effectiveBranch.isBlank()) {
                        val branchesList = snap.get("branches") as? List<*>
                        val branchesFromList = branchesList
                            ?.mapNotNull { it?.toString()?.trim() }
                            ?.firstOrNull { it.isNotBlank() }
                            .orEmpty()

                        effectiveBranch =
                            snap.getString("activeBranch")
                                ?: snap.getString("active_branch")
                                        ?: snap.getString("branch")
                                        ?: snap.getString("branchesCsv")
                                        ?: snap.getString("coachBranch")
                                        ?: branchesFromList
                    }

                    if (effectiveGroupKey.isBlank()) {
                        val groupsList = snap.get("groups") as? List<*>
                        val groupFromList = groupsList
                            ?.mapNotNull { it?.toString()?.trim() }
                            ?.firstOrNull { it.isNotBlank() }
                            .orEmpty()

                        effectiveGroupKey =
                            snap.getString("activeGroup")
                                ?: snap.getString("active_group")
                                        ?: snap.getString("primaryGroup")
                                        ?: snap.getString("groupKey")
                                        ?: snap.getString("group_key")
                                        ?: snap.getString("age_group")
                                        ?: snap.getString("group")
                                        ?: snap.getString("coachGroupKey")
                                        ?: groupFromList
                    }
                } catch (_: Exception) {
                    // במקרה של שגיאה לא מפילים את האפליקציה – פשוט נשארים עם מה שיש
                }
            }
        }
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FBFF),
                Color(0xFFEAF4FF),
                Color(0xFFB7DDF7),
                Color(0xFF1F78B4),
                Color(0xFF062B4A)
            )
        )
    }

    // --- טעינת מתאמנים אמיתיים + חישוב אחוז נוכחות לכל מתאמן ---
    val app = ctx.applicationContext as Application
    val repo = remember(app) { AttendanceRepository.get(app) }

    var traineeProfiles by remember { mutableStateOf<List<TraineeProfile>>(emptyList()) }
    var isProfilesLoading by remember { mutableStateOf(true) }
    var didFinishInitialProfilesLoad by remember { mutableStateOf(false) }

    // מונע הצגת "לא נמצאו מתאמנים" לפני שהסנכרון הראשוני באמת הסתיים
    var isInitialServerSyncRunning by remember { mutableStateOf(true) }

    // ✅ אם effectiveBranch מגיע כ-CSV ("סניף1, סניף2") – עובדים בפועל עם הסניף הראשון
    val effectiveBranchPrimary = remember(effectiveBranch) {
        effectiveBranch
            .split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: effectiveBranch.trim()
    }

    LaunchedEffect(effectiveBranch, effectiveGroupKey) {
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
                    if (tmp != null && !tmp.isEmpty) { usersSnap = tmp; break }
                }
            }

            if (usersSnap == null || usersSnap.isEmpty) {
                usersSnap = runCatching { fetchUsersByBranchesCsv(branchKeys.first()) }.getOrNull()
                if (usersSnap == null || usersSnap.isEmpty) {
                    for (cand in branchKeys.drop(1)) {
                        val tmp = runCatching { fetchUsersByBranchesCsv(cand) }.getOrNull()
                        if (tmp != null && !tmp.isEmpty) { usersSnap = tmp; break }
                    }
                }
            }

            if (usersSnap == null || usersSnap.isEmpty) {
                usersSnap = runCatching { fetchUsersBySingleBranch(branchKeys.first()) }.getOrNull()
                if (usersSnap == null || usersSnap.isEmpty) {
                    for (cand in branchKeys.drop(1)) {
                        val tmp = runCatching { fetchUsersBySingleBranch(cand) }.getOrNull()
                        if (tmp != null && !tmp.isEmpty) { usersSnap = tmp; break }
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

            // ✅ יש members בסיסיים מהשרת/DB:
            // מציגים מיד רשימה חלקית כדי שלא יהיה מסך ריק,
            // ורק אחר כך ממשיכים ברקע להעשיר גיל/חגורה/נוכחות/הערות.
            val existingProfilesById = traineeProfiles.associateBy { it.id }

            traineeProfiles = members.map { m ->
                val existing = existingProfilesById[m.id.toString()]

                existing?.copy(
                    fullName = m.displayName,
                    branch = branchDbKey,
                    groupKey = groupName
                ) ?: TraineeProfile(
                    id = m.id.toString(),
                    fullName = m.displayName,
                    belt = "",
                    seniority = "",
                    age = 0,
                    attendancePct = 0,
                    branch = branchDbKey,
                    groupKey = groupName,
                    email = "",
                    phone = ""
                )
            }

            isProfilesLoading = false
            isInitialServerSyncRunning = false
            didFinishInitialProfilesLoad = true

            // 1) אחוז נוכחות מה-DB המקומי
            val today = LocalDate.now()
            val from = today.minusDays(59)
            val statsMap = mutableMapOf<Long, Pair<Int, Int>>() // mid -> (present,total)

            var d = from
            while (!d.isAfter(today)) {
                val records = repo.attendanceForDay(branchDbKey, groupName, d).firstOrNull().orEmpty()
                for (record in records) {
                    val mid = extractMemberId(record) ?: continue
                    val status = recordStatus(record) ?: continue
                    val (p, t) = statsMap[mid] ?: (0 to 0)
                    statsMap[mid] = (p + if (status == AttendanceStatus.PRESENT) 1 else 0) to (t + 1)
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
                    clean == "white" || clean.contains("white") || clean == "לבנה" || clean.contains("לבנ") -> "לבנה"
                    clean == "yellow" || clean.contains("yellow") || clean == "צהובה" || clean.contains("צהוב") -> "צהובה"
                    clean == "orange" || clean.contains("orange") || clean == "כתומה" || clean.contains("כתומ") -> "כתומה"
                    clean == "green" || clean.contains("green") || clean == "ירוקה" || clean.contains("ירוק") -> "ירוקה"
                    clean == "blue" || clean.contains("blue") || clean == "כחולה" || clean.contains("כחול") -> "כחולה"
                    clean == "brown" || clean.contains("brown") || clean == "חומה" || clean.contains("חומ") -> "חומה"
                    clean == "black" || clean.contains("black") || clean == "שחורה" || clean.contains("שחור") -> "שחורה"
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
                        java.time.LocalDate.parse(startRaw.trim())
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
                    val years = java.time.Period.between(startAge, java.time.LocalDate.now()).years
                    if (years > 0) return "$years שנים"
                }

                return ""
            }

            fun ageFromBirthDateRaw(raw: Any?): Int {
                fun yearsFromDate(dob: java.time.LocalDate): Int {
                    val years = java.time.Period.between(dob, java.time.LocalDate.now()).years
                    return years.coerceIn(0, 120)
                }

                return when (raw) {
                    is String -> {
                        val s = raw.trim()
                        if (s.isBlank()) return 0

                        runCatching {
                            yearsFromDate(java.time.LocalDate.parse(s))
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
                                        java.time.LocalDate.of(a, b, c)
                                    } else {
                                        java.time.LocalDate.of(c, b, a)
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
                                yearsFromDate(java.time.LocalDate.of(year, month, day))
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
                            java.time.LocalDate.of(year, month, day),
                            java.time.LocalDate.now()
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
                                                    (docBranch.contains(candidate) || candidate.contains(docBranch))
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

                val distinctDirect = directDocs.distinctBy { it.id }

                if (distinctDirect.isNotEmpty()) {
                    distinctDirect
                } else {
                    Firebase.firestore.collection("users")
                        .get()
                        .await()
                        .documents
                        .filter { doc ->
                            userDocMatchesBranchAndGroup(
                                doc = doc,
                                branchCandidates = branchKeys,
                                groupCandidate = groupName
                            )
                        }
                }
            }.getOrNull().orEmpty()

            for (doc in userDocs) {
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
                        val value = entry.value

                        val parsed = when (value) {
                            is Map<*, *> -> CoachDateEntry(
                                date = value["date"]?.toString().orEmpty(),
                                description = value["description"]?.toString().orEmpty()
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

                userInfoByName[n] = FireUserInfo(
                    userDocId = doc.id,
                    email = email,
                    phone = phone,
                    age = age,
                    beltHeb = belt,
                    seniority = seniority,
                    beltAwardDates = beltAwardDates,
                    beltAwardDescriptions = beltAwardDescriptions,
                    coachNotes = coachNotes,
                    seminarDates = seminarDates,
                    campDates = campDates,
                    certificationDates = certificationDates
                )
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

            traineeProfiles = builtProfiles
                .groupBy { profile ->
                    val emailKey = normalizeEmailForMerge(profile.email)
                    val phoneKey = normalizePhoneForMerge(profile.phone)
                    val nameKey = profile.fullName.normKey()

                    when {
                        phoneKey.isNotBlank() -> "phone:$phoneKey"
                        emailKey.isNotBlank() -> "email:$emailKey"
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
                    title = coachTr(isEnglish, "רשימת המתאמנים", "Trainees list"),
                    onOpenDrawer = onOpenDrawer,
                    onHome = onOpenHome,
                    showTopHome = false,
                    showRoleStatus = false,
                    lockSearch = true,
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
                    text = coachTr(isEnglish, "המסך זמין למאמנים בלבד", "This screen is available for coaches only"),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White
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

    val uiProfiles = remember(traineeProfiles, traineeSearchQuery) {
        val query = normalizeCoachSearchText(traineeSearchQuery)

        val mergedProfiles = traineeProfiles
            .groupBy { profile ->
                val phoneKey = profile.phone.filter { it.isDigit() }
                    .let { digits ->
                        when {
                            digits.startsWith("972") && digits.length >= 11 -> "0" + digits.drop(3)
                            digits.startsWith("05") -> digits
                            digits.length == 9 && digits.startsWith("5") -> "0$digits"
                            else -> digits
                        }
                    }

                val emailKey = profile.email.trim().lowercase(Locale.US)
                val nameKey = normalizeCoachSearchText(profile.fullName)

                when {
                    phoneKey.isNotBlank() -> "phone:$phoneKey"
                    emailKey.isNotBlank() -> "email:$emailKey"
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

        if (query.isBlank()) {
            mergedProfiles
        } else {
            mergedProfiles.filter { trainee ->
                listOf(
                    trainee.fullName,
                    trainee.belt,
                    trainee.branch,
                    trainee.groupKey,
                    trainee.seniority,
                    trainee.age.takeIf { it > 0 }?.toString().orEmpty(),
                    trainee.attendancePct.takeIf { it > 0 }?.toString().orEmpty()
                ).any { value ->
                    normalizeCoachSearchText(value).contains(query)
                }
            }
        }
    }

    // בחירה נוכחית
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected: TraineeProfile? = uiProfiles.firstOrNull { it.id == selectedId }
        ?: uiProfiles.firstOrNull()

    // הערות מאמן לפי מתאמן
    val coachNotes = remember { mutableStateMapOf<String, String>() }

    // תאריכי קבלת חגורות לפי מתאמן
    val beltAwardDatesState = remember { mutableStateMapOf<String, Map<String, String>>() }

    // ✅ תיאור חופשי לכל חגורה לפי מתאמן
    val beltAwardDescriptionsState = remember { mutableStateMapOf<String, Map<String, String>>() }

    // ✅ שדות נוספים למילוי ע"י המאמן לפי מתאמן: תאריך + תיאור
    val seminarDatesState = remember { mutableStateMapOf<String, Map<String, CoachDateEntry>>() }
    val campDatesState = remember { mutableStateMapOf<String, Map<String, CoachDateEntry>>() }
    val certificationDatesState = remember { mutableStateMapOf<String, Map<String, CoachDateEntry>>() }

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

// ✅ החלק התחתון עובד כאקורדיון: רק נושא אחד פתוח בכל רגע.
    var expandedCoachSection by rememberSaveable(selectedId) {
        mutableStateOf<String?>(null)
    }

    val beltDatesSectionKey = "belt_dates"
    val seminarsSectionKey = "seminars"
    val campsSectionKey = "camps"
    val certificationsSectionKey = "certifications"
    val notesSectionKey = "coach_notes"

    val groupStats = remember(traineeProfiles, uiProfiles) {
        buildGroupStats(
            profiles = traineeProfiles,
            filtered = uiProfiles
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

        val descriptionUpdates = cleanedDescriptions.entries.associate { (beltName, descriptionValue) ->
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
        if (selectedId == null && uiProfiles.isNotEmpty()) {
            selectedId = uiProfiles.first().id
        } else if (selectedId != null && uiProfiles.isNotEmpty() && uiProfiles.none { it.id == selectedId }) {
            selectedId = uiProfiles.firstOrNull()?.id
        }

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
                    coachTr(isEnglish, "סטטיסטיקת קבוצה", "Group statistics")
                } else {
                    coachTr(isEnglish, "רשימת המתאמנים", "Trainees list")
                },
                onOpenDrawer = onOpenDrawer,
                onHome = onOpenHome,
                showTopHome = false,
                showRoleStatus = false,
                lockSearch = true,
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
                var statsBubbleOffset by remember { mutableStateOf(0f) }

                LaunchedEffect(Unit) {
                    while (true) {
                        androidx.compose.animation.core.animate(
                            initialValue = -120f,
                            targetValue = 320f,
                            animationSpec = androidx.compose.animation.core.tween(2600)
                        ) { value, _ ->
                            statsBubbleOffset = value
                        }
                    }
                }

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
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .shadow(2.dp, RoundedCornerShape(15.dp))
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
                                        shape = androidx.compose.foundation.shape.CircleShape
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
                                        modifier = Modifier.size(14.dp)
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    Text(
                                        text = coachTr(isEnglish, "סטטיסטיקה לקבוצה", "Group statistics"),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 15.sp,
                                            lineHeight = 17.sp
                                        )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = if (showStatsSheet || isKeyboardVisible) 8.dp else 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                isTopStatsExpanded = !isTopStatsExpanded
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF111827).copy(alpha = 0.92f),
                            shadowElevation = 5.dp,
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier
                                .widthIn(min = 150.dp)
                                .height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isTopStatsExpanded) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    text = if (isTopStatsExpanded) {
                                        coachTr(isEnglish, "הסתר נתונים", "Hide data")
                                    } else {
                                        coachTr(isEnglish, "הצג נתונים", "Show data")
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (isTopStatsExpanded) {
                            CoachTopStatsCard(
                                stats = groupStats,
                                searchQuery = traineeSearchQuery,
                                onSearchQueryChange = { traineeSearchQuery = it },
                                isEnglish = isEnglish,
                                showSearch = false
                            )
                        }
                    }
                }

                item {
                    Surface(
                        color = Color(0xFF111827).copy(alpha = 0.96f),
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 8.dp,
                        border = BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = screenHorizontalAlignment
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isTraineePickerExpanded = !isTraineePickerExpanded
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = if (isTraineePickerExpanded) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(Modifier.width(8.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = screenHorizontalAlignment
                                ) {
                                    Text(
                                        text = coachTr(isEnglish, "בחירת מתאמן", "Select trainee"),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 17.sp,
                                            lineHeight = 20.sp
                                        ),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = screenTextAlign,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = selected?.fullName
                                            ?: coachTr(isEnglish, "לא נבחר מתאמן", "No trainee selected"),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            lineHeight = 14.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.72f),
                                        textAlign = screenTextAlign,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            if (isTraineePickerExpanded) {
                                OutlinedTextField(
                                    value = traineeSearchQuery,
                                    onValueChange = { traineeSearchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        textAlign = coachTextAlign(isEnglish),
                                        color = Color.White
                                    ),
                                    placeholder = {
                                        Text(
                                            text = coachTr(isEnglish, "חיפוש מתאמן", "Search trainee"),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.62f)
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Text(
                                            text = "🔎",
                                            fontSize = 15.sp,
                                            color = Color.White.copy(alpha = 0.82f)
                                        )
                                    },
                                    trailingIcon = {
                                        if (traineeSearchQuery.isNotBlank()) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = coachTr(isEnglish, "נקה חיפוש", "Clear search"),
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clickable {
                                                        traineeSearchQuery = ""
                                                    },
                                                tint = Color.White.copy(alpha = 0.82f)
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        disabledTextColor = Color.White.copy(alpha = 0.60f),
                                        cursorColor = Color.White,
                                        focusedBorderColor = Color(0xFFA78BFA),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedPlaceholderColor = Color.White.copy(alpha = 0.62f),
                                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.62f),
                                        focusedLeadingIconColor = Color.White.copy(alpha = 0.82f),
                                        unfocusedLeadingIconColor = Color.White.copy(alpha = 0.82f),
                                        focusedTrailingIconColor = Color.White.copy(alpha = 0.82f),
                                        unfocusedTrailingIconColor = Color.White.copy(alpha = 0.82f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                when {
                                    effectiveBranch.isBlank() || effectiveGroupKey.isBlank() -> {
                                        Text(
                                            text = coachTr(
                                                isEnglish,
                                                "לא אותרו סניף או קבוצה עבור המאמן.",
                                                "No branch or group was found for this coach."
                                            ),
                                            color = Color(0xFFFCA5A5),
                                            textAlign = screenTextAlign,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    isProfilesLoading || isInitialServerSyncRunning || !didFinishInitialProfilesLoad -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Text(
                                                text = coachTr(
                                                    isEnglish,
                                                    "טוען מתאמנים מהשרת...",
                                                    "Loading trainees from the server..."
                                                ),
                                                color = Color.White.copy(alpha = 0.82f),
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    uiProfiles.isEmpty() -> {
                                        Text(
                                            text = coachTr(
                                                isEnglish,
                                                "לא נמצאו מתאמנים פעילים לסניף ולקבוצה שנבחרו.",
                                                "No active trainees were found for the selected branch and group."
                                            ),
                                            color = Color.White.copy(alpha = 0.72f),
                                            textAlign = screenTextAlign,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    else -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            uiProfiles.take(8).forEach { trainee ->
                                                val isSelected = selectedId == trainee.id

                                                Surface(
                                                    color = if (isSelected) {
                                                        Color.White.copy(alpha = 0.18f)
                                                    } else {
                                                        Color.White.copy(alpha = 0.08f)
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    border = if (isSelected) {
                                                        BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                                                    } else {
                                                        BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedId = trainee.id
                                                            isTraineePickerExpanded = false
                                                            traineeSearchQuery = ""
                                                            expandedCoachSection = null
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                tint = Color(0xFF86EFAC),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        } else {
                                                            Spacer(Modifier.size(18.dp))
                                                        }

                                                        Spacer(Modifier.width(8.dp))

                                                        Text(
                                                            text = trainee.fullName,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontSize = 15.sp,
                                                                lineHeight = 17.sp
                                                            ),
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color.White,
                                                            textAlign = screenTextAlign,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }

                                            if (uiProfiles.size > 8) {
                                                Text(
                                                    text = coachTr(
                                                        isEnglish,
                                                        "מוצגים 8 ראשונים. השתמש בחיפוש למציאת מתאמן נוסף.",
                                                        "Showing first 8. Use search to find another trainee."
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        lineHeight = 13.sp
                                                    ),
                                                    color = Color.White.copy(alpha = 0.62f),
                                                    textAlign = screenTextAlign,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiProfiles.isNotEmpty()) {
                    item {
                        // כרטיס פרטי מתאמן
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(26.dp),
                            shadowElevation = 8.dp,
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF0B1730),
                                                Color(0xFF132A52),
                                                Color(0xFF1E4B86)
                                            )
                                        ),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                            ) {
                                if (selected == null) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = coachTr(
                                                isEnglish,
                                                "בחר מתאמן מהרשימה למעלה",
                                                "Select a trainee from the list above"
                                            ),
                                            color = Color.White.copy(alpha = 0.78f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalAlignment = screenHorizontalAlignment
                                    ) {
                                        Text(
                                            text = selected.fullName,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 23.sp,
                                                lineHeight = 27.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = Color.White,
                                            textAlign = screenTextAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text = coachTr(
                                                isEnglish,
                                                "פרופיל מתאמן",
                                                "Trainee profile"
                                            ),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 12.sp,
                                                lineHeight = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = Color.White.copy(alpha = 0.68f),
                                            textAlign = screenTextAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Divider(
                                            color = Color.White.copy(alpha = 0.12f)
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
                                                    value = coachBeltNameForUi(selected.belt.ifBlank { "—" }, isEnglish),
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

                                        Divider(
                                            color = Color.White.copy(alpha = 0.12f)
                                        )

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val isBeltDatesSectionExpanded = expandedCoachSection == beltDatesSectionKey

                                        PremiumCoachCompactSectionHeader(
                                            title = coachTr(isEnglish, "תאריכי קבלת חגורות", "Belt award dates"),
                                            subtitle = if (isBeltDatesSectionExpanded) {
                                                coachTr(isEnglish, "עדכון תאריכים לפי חגורה", "Update dates by belt")
                                            } else {
                                                coachTr(isEnglish, "לחצו לפתיחת רשימת החגורות", "Tap to open the belt list")
                                            },
                                            iconText = "📅",
                                            isExpanded = isBeltDatesSectionExpanded,
                                            accent = Color(0xFF6D56B8),
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

                                            val selectedDates = beltAwardDatesState[selected.id] ?: emptyMap()
                                            var expandedBelt by remember(selected.id) { mutableStateOf<String?>(null) }

                                            beltOrder.forEach { beltName ->
                                                val beltAccent = beltAccentMap[beltName] ?: Color(0xFF6366F1)
                                                val currentDate = selectedDates[beltName].orEmpty()
                                                val hasDate = currentDate.isNotBlank()
                                                val isExpanded = expandedBelt == beltName
                                                val selectedDescriptions =
                                                    beltAwardDescriptionsState[selected.id] ?: emptyMap()

                                                val currentDescription =
                                                    selectedDescriptions[beltName].orEmpty()

                                                Surface(
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(16.dp),
                                                    shadowElevation = 1.dp,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        beltAccent.copy(alpha = 0.16f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            expandedBelt = if (isExpanded) null else beltName
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
                                                                    .background(beltAccent)
                                                            )

                                                            Spacer(Modifier.width(8.dp))

                                                            Column(
                                                                modifier = Modifier.weight(1f),
                                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (isEnglish) {
                                                                        coachBeltNameForUi(beltName, true)
                                                                    } else {
                                                                        "חגורה $beltName"
                                                                    },
                                                                    style = MaterialTheme.typography.titleSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF1F2937)
                                                                )

                                                                Text(
                                                                    text = if (hasDate) {
                                                                        coachTr(isEnglish, "תאריך קבלה: $currentDate", "Award date: $currentDate")
                                                                    } else {
                                                                        coachTr(isEnglish, "אין תאריך קבלה", "No award date")
                                                                    },
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = if (hasDate) Color(0xFF0F766E) else Color(0xFF94A3B8)
                                                                )
                                                            }

                                                            Icon(
                                                                imageVector = if (hasDate) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                                contentDescription = null,
                                                                tint = if (hasDate) Color(0xFF16A34A) else Color(0xFFDC2626)
                                                            )

                                                            Spacer(Modifier.width(6.dp))

                                                            Icon(
                                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                                contentDescription = null,
                                                                tint = Color(0xFF64748B)
                                                            )
                                                        }

                                                        if (isExpanded) {
                                                            var showBeltDatePicker by remember(selected.id, beltName) {
                                                                mutableStateOf(false)
                                                            }

                                                            PremiumCoachDateField(
                                                                label = coachTr(isEnglish, "תאריך קבלה", "Award date"),
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
                                                                    accent = beltAccent,
                                                                    isEnglish = isEnglish,
                                                                    onDismiss = {
                                                                        showBeltDatePicker = false
                                                                    },
                                                                    onDateSelected = { newDate ->
                                                                        val current = beltAwardDatesState[selected.id]
                                                                            .orEmpty()
                                                                            .toMutableMap()

                                                                        current[beltName] = newDate
                                                                        beltAwardDatesState[selected.id] = current
                                                                    }
                                                                )
                                                            }

                                                            OutlinedTextField(
                                                                value = currentDescription,
                                                                onValueChange = { newValue ->
                                                                    val current = beltAwardDescriptionsState[selected.id]
                                                                        .orEmpty()
                                                                        .toMutableMap()

                                                                    current[beltName] = newValue
                                                                    beltAwardDescriptionsState[selected.id] = current
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
                                                                        textAlign = coachTextAlign(isEnglish),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    )
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                minLines = 2,
                                                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                                    textAlign = coachTextAlign(isEnglish)
                                                                ),
                                                                shape = RoundedCornerShape(16.dp)
                                                            )
                                                        }
                                                            }

                                                }
                                            }

                                            var isSavingBeltDates by remember(selected.id) { mutableStateOf(false) }
                                            var beltDatesSaveMessage by remember(selected.id) { mutableStateOf<String?>(null) }

                                            Surface(
                                                onClick = {
                                                    val selectedProfile = selected
                                                    if (selectedProfile != null && !isSavingBeltDates) {
                                                        val datesToSave = beltAwardDatesState[selectedProfile.id]
                                                            .orEmpty()
                                                            .filterValues { it.isNotBlank() }

                                                        val descriptionsToSave = beltAwardDescriptionsState[selectedProfile.id]
                                                            .orEmpty()
                                                            .filterValues { it.isNotBlank() }

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
                                                                    selectedProfile = selectedProfile,
                                                                    dates = datesToSave,
                                                                    descriptions = descriptionsToSave
                                                                )
                                                            }.onSuccess {
                                                                beltDatesSaveMessage = coachTr(isEnglish, "תאריכי החגורות נשמרו", "Belt dates saved")
                                                            }.onFailure {
                                                                beltDatesSaveMessage = coachTr(
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
                                                shadowElevation = 6.dp,
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
                                                        text = if (isSavingBeltDates) {
                                                            coachTr(isEnglish, "שומר...", "Saving...")
                                                        } else {
                                                            coachTr(isEnglish, "שמור תאריכי חגורות", "Save belt dates")
                                                        },
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                                beltDatesSaveMessage?.let { msg ->
                                                    Surface(
                                                        color = if (msg.contains("נשמרו") || msg.contains("saved", ignoreCase = true)) {
                                                            Color(0xFFDCFCE7)
                                                        } else {
                                                            Color(0xFFFEE2E2)
                                                        },
                                                        shape = RoundedCornerShape(14.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = msg,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (msg.contains("נשמרו") || msg.contains("saved", ignoreCase = true)) Color(0xFF166534) else Color(0xFF991B1B),
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                    }

                                    CoachDateSectionCard(
                                        title = "השתלמויות",
                                        collapsedSubtitle = coachTr(isEnglish, "לחצו לפתיחת רשימת השתלמויות", "Tap to open the seminar list"),
                                        expandedSubtitle = coachTr(isEnglish, "הוסיפו תאריך ותיאור להשתלמויות שהמתאמן עבר", "Add a date and description for seminars completed by the trainee"),
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
                                        collapsedSubtitle = coachTr(isEnglish, "לחצו לפתיחת רשימת מחנות אימונים", "Tap to open the training camp list"),
                                        expandedSubtitle = coachTr(isEnglish, "הוסיפו תאריך ותיאור למחנות אימונים שבהם המתאמן השתתף", "Add a date and description for training camps attended by the trainee"),
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
                                        collapsedSubtitle = coachTr(isEnglish, "לחצו לפתיחת רשימת הסמכות", "Tap to open the certification list"),
                                        expandedSubtitle = coachTr(isEnglish, "הוסיפו תאריך ותיאור להסמכות שהמתאמן קיבל", "Add a date and description for certifications received by the trainee"),
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

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalAlignment = screenHorizontalAlignment
                                    ) {
                                        Surface(
                                            color = Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(20.dp),
                                            shadowElevation = 3.dp,
                                            border = BorderStroke(
                                                1.dp,
                                                Color(0xFFD6E2F1)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalAlignment = screenHorizontalAlignment
                                            ) {
                                                Text(
                                                    text = coachTr(isEnglish, "הערות מאמן", "Coach notes"),
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontSize = 12.sp,
                                                        lineHeight = 14.sp
                                                    ),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF475569),
                                                    textAlign = screenTextAlign,
                                                    modifier = Modifier.fillMaxWidth()
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
                                                            color = Color(0xFF94A3B8),
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                textAlign = screenTextAlign
                                                            ),
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    },
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        color = Color(0xFF0F172A),
                                                        textAlign = screenTextAlign,
                                                        fontSize = 14.sp,
                                                        lineHeight = 18.sp
                                                    ),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    minLines = 4,
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = Color(0xFF0F172A),
                                                        unfocusedTextColor = Color(0xFF0F172A),
                                                        disabledTextColor = Color(0xFF64748B),
                                                        cursorColor = Color(0xFF2563EB),
                                                        focusedBorderColor = Color(0xFF60A5FA),
                                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                                        focusedContainerColor = Color.White,
                                                        unfocusedContainerColor = Color.White,
                                                        disabledContainerColor = Color(0xFFF8FAFC),
                                                        focusedPlaceholderColor = Color(0xFF94A3B8),
                                                        unfocusedPlaceholderColor = Color(0xFF94A3B8)
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
                                                    val selectedProfile = selected
                                                    if (selectedProfile != null && !isSavingCoachNotes) {
                                                        val noteToSave = coachNotes[selectedProfile.id].orEmpty()

                                                        screenScope.launch {
                                                            isSavingCoachNotes = true
                                                            coachNotesSaveMessage = null

                                                            runCatching {
                                                                saveCoachNotesForSelected(
                                                                    selectedProfile = selectedProfile,
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
                                                color = Color(0xFFF1F5F9),
                                                shadowElevation = 0.dp,
                                                border = BorderStroke(
                                                    1.dp,
                                                    Color(0xFFCBD5E1)
                                                ),
                                                modifier = Modifier.widthIn(min = 118.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (isSavingCoachNotes) {
                                                            coachTr(isEnglish, "שומר...", "Saving...")
                                                        } else {
                                                            coachTr(isEnglish, "שמור", "Save")
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 12.sp,
                                                            lineHeight = 14.sp
                                                        ),
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF334155),
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    coachNotesSaveMessage?.let { msg ->
                                        Surface(
                                            color = if (
                                                msg.contains("נשמרו") ||
                                                msg.contains("saved", ignoreCase = true)
                                            ) {
                                                Color(0xFFDCFCE7)
                                            } else {
                                                Color(0xFFFEE2E2)
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = msg,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (
                                                    msg.contains("נשמרו") ||
                                                    msg.contains("saved", ignoreCase = true)
                                                ) {
                                                    Color(0xFF166534)
                                                } else {
                                                    Color(0xFF991B1B)
                                                },
                                                textAlign = screenTextAlign,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
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
                        profiles = uiProfiles,
                        isEnglish = isEnglish,
                        onClose = { showStatsSheet = false }
                    )
                }
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
    Surface(
        onClick = onClick,
        color = Color(0xFFFCFDFF),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            Color(0xFFE7ECF7)
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
                            Color.White,
                            Color(0xFFFAFBFF),
                            accent.copy(alpha = 0.025f)
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
                modifier = Modifier.size(28.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
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
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                )

                Text(
                    text = subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7A879A)
                    )
                )
            }

            Spacer(Modifier.width(5.dp))

            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
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
                    val accent = Color(0xFF6366F1)

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 1.dp,
                        border = BorderStroke(
                            1.dp,
                            accent.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedItem = if (isItemExpanded) null else itemName
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
                                        text = coachDateItemNameForUi(itemName, isEnglish),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937),
                                        textAlign = coachTextAlign(isEnglish),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text(
                                        text = when {
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

                                            else -> coachTr(isEnglish, "אין מידע", "No information")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasContent) Color(0xFF0F766E) else Color(0xFF94A3B8),
                                        textAlign = coachTextAlign(isEnglish),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Icon(
                                    imageVector = if (hasContent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (hasContent) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )

                                Spacer(Modifier.width(6.dp))

                                Icon(
                                    imageVector = if (isItemExpanded) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
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
                                            "בחר תאריך עבור ${coachDateItemNameForUi(itemName, false)}",
                                            "Choose date for ${coachDateItemNameForUi(itemName, true)}"
                                        ),
                                        selectedDate = currentEntry.date,
                                        accent = sectionAccent,
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

                                        val oldEntry = current[itemName] ?: CoachDateEntry()
                                        current[itemName] = oldEntry.copy(description = newValue)
                                        stateMap[selectedId] = current
                                    },
                                    label = { Text(coachTr(isEnglish, "תיאור", "Description")) },
                                    placeholder = {
                                        Text(
                                            coachTr(
                                                isEnglish,
                                                "לדוגמה: השתלמות מדריכים / מחנה קיץ / הסמכת עוזר מדריך",
                                                "Example: instructor seminar / summer camp / assistant instructor certification"
                                            ),
                                            textAlign = coachTextAlign(isEnglish),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        textAlign = coachTextAlign(isEnglish)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
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
                                saveMessage = coachTr(isEnglish, "אין נתונים לשמירה", "No data to save")
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
                    shadowElevation = 6.dp,
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
                            text = if (isSaving) {
                                coachTr(isEnglish, "שומר...", "Saving...")
                            } else {
                                coachTr(
                                    isEnglish,
                                    "שמור ${coachSectionTitleForUi(title, false)}",
                                    "Save ${coachSectionTitleForUi(title, true)}"
                                )
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                saveMessage?.let { msg ->
                    Surface(
                        color = if (msg.contains("נשמרו") || msg.contains("saved", ignoreCase = true)) {
                            Color(0xFFDCFCE7)
                        } else {
                            Color(0xFFFEE2E2)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.contains("נשמרו") || msg.contains("saved", ignoreCase = true)) Color(0xFF166534) else Color(0xFF991B1B),
                            textAlign = coachTextAlign(isEnglish),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
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
    val horizontalAlignment = coachHorizontalAlignment(isEnglish)
    val cleanValue = value.ifBlank { "—" }

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
        color = Color(0xFFF4F7FB),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
        border = BorderStroke(
            1.dp,
            Color(0xFFD7E2F0)
        ),
        modifier = modifier.heightIn(min = 78.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFEAF3FF),
                            Color(0xFFF8FBFF)
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isEnglish) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = horizontalAlignment
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color(0xFF64748B),
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = cleanValue,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color(0xFF0F172A),
                    textAlign = textAlign,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isEnglish) {
                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF607D8B)
        )

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Text(
                text = value.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1F2937),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CoachGroupStatsPremiumScreen(
    stats: GroupStatsUi,
    profiles: List<TraineeProfile>,
    isEnglish: Boolean,
    onClose: () -> Unit
) {
    val statsTextAlign = coachTextAlign(isEnglish)
    val statsHorizontalAlignment = coachHorizontalAlignment(isEnglish)

    val statsAccent = Color(0xFF4F46E5)
    val glassTop = Color.White.copy(alpha = 0.72f)
    val glassBottom = Color.White.copy(alpha = 0.38f)

    val statsBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FBFF),
            Color(0xFFEAF4FF),
            Color(0xFFB7DDF7),
            Color(0xFF1F78B4),
            Color(0xFF062B4A)
        )
    )

    val minAge = profiles.map { it.age }.filter { it > 0 }.minOrNull()?.toString() ?: "—"
    val maxAge = profiles.map { it.age }.filter { it > 0 }.maxOrNull()?.toString() ?: "—"
    val minSeniority = profiles.mapNotNull { parseYearsFromSeniority(it.seniority) }.minOrNull()?.toString() ?: "—"
    val maxSeniority = profiles.mapNotNull { parseYearsFromSeniority(it.seniority) }.maxOrNull()?.toString() ?: "—"

    val avgAttendanceValue = profiles
        .map { it.attendancePct }
        .filter { it > 0 }
        .average()
        .takeIf { !it.isNaN() }
        ?.toInt()
        ?: 0

    var startAvgAttendanceAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAvgAttendanceAnimation = true
    }

    val animatedAvgAttendance by animateFloatAsState(
        targetValue = if (startAvgAttendanceAnimation) avgAttendanceValue.toFloat() else 0f,
        animationSpec = tween(
            durationMillis = 1400,
            easing = FastOutSlowInEasing
        ),
        label = "avgAttendanceAnim"
    )

    val glowTransition = rememberInfiniteTransition(label = "glassGlow")
    val glowShift by glowTransition.animateFloat(
        initialValue = -160f,
        targetValue = 460f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glassGlowShift"
    )

    val sparklineData = remember(profiles) {
        profiles
            .map { it.attendancePct.coerceIn(0, 100) }
            .filter { it > 0 }
            .sorted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(statsBgBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Color(0xFFFCFDFF),
                        shape = RoundedCornerShape(28.dp),
                        shadowElevation = 7.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFE7ECF7)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White,
                                            Color(0xFFFAFBFF),
                                            Color(0xFFF7FAFF)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = statsHorizontalAlignment,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = coachTr(isEnglish, "מידע על הקבוצה", "Group information"),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = statsTextAlign,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 22.sp,
                                        lineHeight = 25.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                )

                                Text(
                                    text = coachTr(
                                        isEnglish,
                                        "סיכום נוכחות, גיל, ותק וחגורות",
                                        "Attendance, age, seniority and belt summary"
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = statsTextAlign,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                            }
                                Surface(
                                    color = Color(0xFFFCFDFF),
                                    shape = RoundedCornerShape(24.dp),
                                    shadowElevation = 5.dp,
                                    tonalElevation = 0.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE7ECF7)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(132.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color.White,
                                                        Color(0xFFFAFBFF),
                                                        Color(0xFF4F46E5).copy(alpha = 0.07f)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalAlignment = statsHorizontalAlignment,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stats.highAttendanceCount.toString(),
                                                textAlign = statsTextAlign,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontSize = 32.sp,
                                                    lineHeight = 34.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF4F46E5)
                                                )
                                            )

                                            Text(
                                                text = "🏆",
                                                fontSize = 22.sp,
                                                textAlign = TextAlign.Left
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = coachTr(isEnglish, "מעל 80%", "Above 80%"),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = statsTextAlign,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                lineHeight = 17.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF4F46E5)
                                            )
                                        )

                                        Spacer(Modifier.height(2.dp))

                                        Text(
                                            text = coachTr(isEnglish, "נוכחות", "Attendance"),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = statsTextAlign,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                lineHeight = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFFFCFDFF),
                                    shape = RoundedCornerShape(24.dp),
                                    shadowElevation = 5.dp,
                                    tonalElevation = 0.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE7ECF7)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(132.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color.White,
                                                        Color(0xFFFAFBFF),
                                                        Color(0xFF22C55E).copy(alpha = 0.07f)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        AttendanceRing(
                                            percent = animatedAvgAttendance.toInt(),
                                            modifier = Modifier.size(82.dp)
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        Text(
                                            text = coachTr(isEnglish, "נוכחות ממוצעת", "Average attendance"),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                lineHeight = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        )
                                    }
                                }
                            }

                            AttendanceSparkline(
                                values = sparklineData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumStatChip(
                            label = coachTr(isEnglish, "גיל מינ׳", "Min age"),
                            value = minAge,
                            icon = "📅",
                            gradient = listOf(
                                Color(0xFF1D4ED8),
                                Color(0xFF3B82F6),
                                Color(0xFF93C5FD)
                            ),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )

                        PremiumStatChip(
                            label = coachTr(isEnglish, "גיל מקס׳", "Max age"),
                            value = maxAge,
                            icon = "🎂",
                            gradient = listOf(
                                Color(0xFF0F766E),
                                Color(0xFF14B8A6),
                                Color(0xFF5EEAD4)
                            ),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumStatChip(
                            label = coachTr(isEnglish, "ותק מינ׳", "Min seniority"),
                            value = minSeniority,
                            icon = "⏱",
                            gradient = listOf(
                                Color(0xFF7C3AED),
                                Color(0xFFA855F7),
                                Color(0xFFD8B4FE)
                            ),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )

                        PremiumStatChip(
                            label = coachTr(isEnglish, "ותק מקס׳", "Max seniority"),
                            value = maxSeniority,
                            icon = "🔥",
                            gradient = listOf(
                                Color(0xFFBE185D),
                                Color(0xFFEC4899),
                                Color(0xFFF9A8D4)
                            ),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumMiniPill(
                            title = coachTr(isEnglish, "מתאמנים", "Trainees"),
                            value = stats.totalTrainees.toString(),
                            icon = "👥",
                            accent = Color(0xFF4F46E5),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )

                        PremiumMiniPill(
                            title = coachTr(isEnglish, "חגורות שונות", "Belt types"),
                            value = stats.beltCounts.size.toString(),
                            icon = "🥋",
                            accent = Color(0xFF0891B2),
                            isEnglish = isEnglish,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFFCFDFF),
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 7.dp,
                    tonalElevation = 0.dp,
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFE7ECF7)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color(0xFFFAFBFF),
                                        Color(0xFFF7FAFF)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = coachTr(isEnglish, "התפלגות חגורות בסניף", "Belt distribution"),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = statsTextAlign,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 20.sp,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        )

                        if (stats.beltCounts.isEmpty()) {
                            Text(
                                text = coachTr(isEnglish, "אין נתונים להצגה", "No data to display"),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = statsTextAlign,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                        } else {
                            stats.beltCounts.forEach { (belt, count) ->
                                val progress =
                                    if (profiles.isNotEmpty()) count.toFloat() / profiles.size.toFloat() else 0f

                                val beltColor = beltColorForStats(belt)
                                val labelColor =
                                    if (beltColor == Color(0xFFE5E7EB)) Color(0xFF475569) else beltColor

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = beltColor.copy(alpha = 0.14f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$count",
                                                    color = labelColor,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Text(
                                            text = coachBeltNameForUi(belt, isEnglish),
                                            modifier = Modifier.weight(1f),
                                            textAlign = statsTextAlign,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontSize = 18.sp,
                                                lineHeight = 21.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = labelColor
                                            )
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(beltColor)
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = beltColor,
                                        trackColor = beltColor.copy(alpha = 0.18f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


@Composable
private fun PremiumStatChip(
    label: String,
    value: String,
    icon: String,
    gradient: List<Color>,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val valueNumber = value.filter { it.isDigit() }.toFloatOrNull() ?: 0f

    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val animatedValue by animateFloatAsState(
        targetValue = if (startAnimation) valueNumber else 0f,
        animationSpec = tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "premiumChipValue"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val chipScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "statChipPressScale"
    )

    val accent = gradient.getOrNull(1) ?: Color(0xFF4F46E5)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFCFDFF),
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE7ECF7)
        ),
        modifier = modifier
            .height(96.dp)
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFAFBFF),
                            accent.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(start = 6.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = accent.copy(alpha = 0.13f),
                shadowElevation = 2.dp,
                modifier = Modifier.size(34.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = if (valueNumber > 0f) animatedValue.toInt().toString() else value,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 28.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                )
            }
        }
    }
}

@Composable
private fun AttendanceSparkline(
    values: List<Int>,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    val lineAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "sparklineReveal"
    )

    Canvas(modifier = modifier) {
        val maxValue = 100f
        val minValue = 0f
        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width

        val points = values.mapIndexed { index, value ->
            val normalized = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
            Offset(
                x = index * stepX * lineAnim,
                y = size.height - (normalized * size.height)
            )
        }

        for (i in 0..3) {
            val y = size.height * (i / 3f)
            drawLine(
                color = Color(0xFFCBD5E1).copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.2f
            )
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { point ->
                lineTo(point.x, point.y)
            }
            lineTo(points.last().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF6366F1).copy(alpha = 0.24f),
                    Color(0xFF6366F1).copy(alpha = 0.03f)
                )
            )
        )

        val linePath = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }

        drawPath(
            path = linePath,
            color = Color(0xFF4F46E5),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        points.forEachIndexed { index, point ->
            if (index == points.lastIndex) {
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = point
                )
                drawCircle(
                    color = Color(0xFF4F46E5),
                    radius = 5f,
                    center = point
                )
            }
        }
    }
}

@Composable
private fun PremiumMiniPill(
    title: String,
    value: String,
    icon: String,
    accent: Color,
    isEnglish: Boolean,
    modifier: Modifier = Modifier
) {
    val valueNumber = value.filter { it.isDigit() }.toFloatOrNull() ?: 0f

    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val animatedValue by animateFloatAsState(
        targetValue = if (startAnimation) valueNumber else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "miniPillValue"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "pillScale"
    )

    Surface(
        color = Color(0xFFFCFDFF),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 7.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            Color(0xFFE7ECF7)
        ),
        modifier = modifier
            .height(78.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {}
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFAFBFF),
                            accent.copy(alpha = 0.06f)
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = coachHorizontalAlignment(isEnglish),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                )

                Text(
                    text = if (valueNumber > 0f) animatedValue.toInt().toString() else value,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = coachTextAlign(isEnglish),
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                )
            }
        }
    }
}

@Composable
private fun AttendanceRing(
    percent: Int,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "attendanceRing"
    )

    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            drawCircle(
                color = Color(0xFFE5E7EB),
                style = Stroke(width = 12f)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF22C55E),
                        Color(0xFF4ADE80),
                        Color(0xFF16A34A)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(
                    width = 12f,
                    cap = StrokeCap.Round
                )
            )
        }

        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
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
            val v = f.get(record)
            when (v) {
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
