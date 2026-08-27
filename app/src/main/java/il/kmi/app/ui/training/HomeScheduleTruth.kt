package il.kmi.app.ui.training

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import il.kmi.app.training.TrainingCatalog
import il.kmi.app.training.TrainingData
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.shared.prefs.KmiPrefs
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import il.kmi.shared.domain.Belt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import il.kmi.app.domain.ContentRepo
import il.kmi.app.search.KmiSearchBridge
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.LocalAppIconScale
import il.kmi.shared.domain.SubTopicRegistry
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter


// ===========================
// Training Summary Palette
// ===========================

private val SummaryBgTop: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.background

private val SummaryBgMid1: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.surface

private val SummaryBgMid2: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.surfaceVariant

private val SummaryBgBottom: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.primary
            .copy(alpha = 0.42f)

private val SummaryCard: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.surface

private val SummaryCardInner: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.surfaceVariant

private val SummaryBorder: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.outlineVariant

private val SummaryDivider: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.outlineVariant
            .copy(alpha = 0.72f)

private val SummaryChip: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.surfaceVariant

private val SummaryChipSelected: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.primaryContainer

private val SummaryTextDark: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.onSurface

private val SummaryTextMuted: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.onSurfaceVariant

private val SummaryPrimaryButton: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.primary

private val SummaryPurpleButton: Color
    @Composable
    get() =
        MaterialTheme.colorScheme.primary

/**
 * פריט תרגיל "לבחירה" שמגיע מהקטלוג (ContentRepo).
 * אתה תבנה את הרשימה הזו מה-ContentRepo אצלך בנקודת החיבור (Route/NavGraph).
 */
data class ExercisePickItem(
    val exerciseId: String,
    val name: String,
    val topic: String
)

@Composable
private fun SummarySectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnglish: Boolean
) {
    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val textDirectionStyle =
        if (isEnglish) {
            TextStyle(
                textDirection = TextDirection.Ltr
            )
        } else {
            TextStyle(
                textDirection = TextDirection.Rtl
            )
        }

    val textAlign =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val horizontalAlignment =
        if (isEnglish) {
            Alignment.Start
        } else {
            Alignment.End
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides
                layoutDirection
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier =
                    Modifier.weight(1f),
                horizontalAlignment =
                    horizontalAlignment
            ) {
                Text(
                    text = title,
                    style =
                        KmiTypography.cardTitle
                            .merge(
                                textDirectionStyle
                            ),
                    fontWeight =
                        FontWeight.ExtraBold,
                    color =
                        SummaryTextDark,
                    textAlign =
                        textAlign,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    text = subtitle,
                    style =
                        KmiTypography.secondary
                            .merge(
                                textDirectionStyle
                            ),
                    color =
                        SummaryTextMuted,
                    fontWeight =
                        FontWeight.SemiBold,
                    textAlign =
                        textAlign,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            Spacer(
                Modifier.width(10.dp)
            )

            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .background(
                            brush =
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme
                                            .colorScheme
                                            .secondary,
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                        MaterialTheme
                                            .colorScheme
                                            .tertiary
                                    )
                                ),
                            shape =
                                CircleShape
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint =
                        MaterialTheme
                            .colorScheme
                            .onPrimary,
                    modifier =
                        Modifier.size(
                            18.dp * LocalAppIconScale.current
                        )
                )
            }
        }
    }
}

@Composable
private fun PremiumSummaryCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = SummaryCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            SummaryBorder
        )
    ) {
        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/**
 * ✅ שים לב: השם V2 כדי למנוע Conflicting overloads אם כבר קיים אצלך TrainingSummaryScreen אחר בפרויקט.
 */
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingSummaryScreen(
    vm: TrainingSummaryViewModel,
    sp: SharedPreferences,
    summarySp: SharedPreferences,
    kmiPrefs: KmiPrefs,
    belt: Belt,
    pickedDateIso: String? = null,
    pickedBranch: String? = null,
    pickedGroup: String? = null,
    pickedTime: String? = null,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    onOpenCalendar: (() -> Unit)? = null
) {
    val state by vm.state.collectAsState()
    val scrollState = rememberLazyListState()
    var showAddExercisesSheet by rememberSaveable { mutableStateOf(false) }

    val ctx = LocalContext.current
    val languageManager = remember { AppLanguageManager(ctx) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH

    fun tr(he: String, en: String): String = if (isEnglish) en else he

    fun normalizedIsoOrNull(raw: String?): String? {
        val clean = raw?.trim().orEmpty()
        if (clean.isBlank()) return null
        if (clean == "{date}") return null
        if (clean.equals("null", ignoreCase = true)) return null

        return runCatching {
            LocalDate.parse(clean)
            clean
        }.getOrNull()
    }

    // ✅ אם המסך נפתח עם תאריך אמיתי מהלוח – נכניס אותו ל-VM.
    // אם הגיע placeholder כמו {date} – נתעלם ונציג בחירת תאריך.
    val routeDateIso = remember(pickedDateIso) { normalizedIsoOrNull(pickedDateIso) }

    LaunchedEffect(routeDateIso) {
        if (routeDateIso != null && state.dateIso != routeDateIso) {
            vm.setDateIso(routeDateIso)
        }
    }

    // ✅ מקור אמת לסניף/קבוצה/מאמן לפי תאריך (כמו במסך הבית)
    val truth = remember(sp) { HomeScheduleTruth(sp) }
    var branchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(
        state.dateIso,
        pickedBranch,
        pickedGroup,
        pickedTime
    ) {
        val cleanIso = normalizedIsoOrNull(state.dateIso)

        // אם עדיין לא נבחר תאריך – לא מציגים שגיאה ולא מציגים "לא נמצא סניף"
        if (cleanIso == null) {
            branchError = null
            if (state.branchName.isNotBlank()) vm.setBranchName("")
            if (state.groupKey.isNotBlank()) vm.setGroupKey("")
            if (state.coachName.isNotBlank()) vm.setCoachName("")
            return@LaunchedEffect
        }

        val t =
            runCatching {
                truth.trainingForDate(
                    dateIso = cleanIso,
                    preferredBranch =
                        pickedBranch.orEmpty(),
                    preferredGroup =
                        pickedGroup.orEmpty(),
                    preferredTime =
                        pickedTime.orEmpty()
                )
            }.getOrNull()

        if (t == null) {
            branchError = tr(
                "לא נמצא אימון בתאריך הזה לפי הלוז שלך במסך הבית.",
                "No training was found on this date according to your home screen schedule."
            )
            if (state.branchName.isNotBlank()) vm.setBranchName("")
            if (state.groupKey.isNotBlank()) vm.setGroupKey("")
            if (state.coachName.isNotBlank()) vm.setCoachName("")
            return@LaunchedEffect
        }

        branchError = null

        if (state.branchName != t.branchName) vm.setBranchName(t.branchName)
        if (state.groupKey != t.groupKey) vm.setGroupKey(t.groupKey)

        if (t.coachName.isNotBlank() && state.coachName != t.coachName) {
            vm.setCoachName(t.coachName)
        }
    }

    fun beltHebLabel(b: Belt): String {
        return if (isEnglish) {
            when (b) {
                Belt.WHITE -> "White belt"
                Belt.YELLOW -> "Yellow belt"
                Belt.ORANGE -> "Orange belt"
                Belt.GREEN -> "Green belt"
                Belt.BLUE -> "Blue belt"
                Belt.BROWN -> "Brown belt"
                Belt.BLACK -> "Black belt"
            }
        } else {
            b.heb
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color =
                    MaterialTheme
                        .colorScheme
                        .background
            ) {
                KmiTopBar(
                    title =
                        tr(
                            "סיכום אימון",
                            "Training summary"
                        ),
                    showTopHome = false,
                    showTopSearch = false,
                    showBottomActions = true,
                    lockSearch = false,
                    lockHome = false,
                    onHome = {
                        onHome?.invoke()
                    }
                )
            }
        },
        contentWindowInsets =
            WindowInsets(0),
        containerColor =
            MaterialTheme
                .colorScheme
                .background
    ) { padding ->

        val granite =
            Brush.verticalGradient(
                colors =
                    listOf(
                        SummaryBgTop,
                        SummaryBgMid1,
                        SummaryBgMid2,
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(alpha = 0.72f),
                        SummaryBgBottom
                    )
            )

        val graniteNoise = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color.Transparent,
                Color.White.copy(alpha = 0.08f),
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(1200f, 1200f)
        )

        val screenLayoutDirection =
            if (isEnglish) {
                LayoutDirection.Ltr
            } else {
                LayoutDirection.Rtl
            }

        CompositionLocalProvider(
            LocalLayoutDirection provides screenLayoutDirection
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(granite)
                    .background(graniteNoise)
                    .padding(padding)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),
                    horizontalAlignment =
                        if (isEnglish) {
                            Alignment.Start
                        } else {
                            Alignment.End
                        }
                ) {

                // -----------------------------
                // ✅ כרטיס אימון קומפקטי (במקום "פרטי אימון" הגדול)
                // -----------------------------
                item {
                    TrainingInfoCard(
                        dateIso = state.dateIso,
                        branchName = state.branchName,
                        coachName = state.coachName,
                        groupKey = state.groupKey,
                        errorText = branchError,
                        isEnglish = isEnglish,
                        onOpenCalendar = onOpenCalendar
                    )
                }

// הוספת תרגילים – כרטיס קומפקטי + פתיחת Bottom Sheet
// -----------------------------
                item {
                    PremiumSummaryCard {
                        SummarySectionHeader(
                            title =
                                tr(
                                    "הוספת תרגילים",
                                    "Add exercises"
                                ),
                            subtitle =
                                tr(
                                    "בחר תרגילים שבוצעו באימון",
                                    "Choose exercises performed in training"
                                ),
                            icon =
                                Icons.AutoMirrored
                                    .Filled
                                    .PlaylistAddCheck,
                            isEnglish =
                                isEnglish
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SummaryDivider,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Spacer(Modifier.height(2.dp))
                        }

                        Text(
                            text =
                                if (state.selected.isEmpty()) {
                                    tr(
                                        "עדיין לא נוספו תרגילים לאימון הזה",
                                        "No exercises have been added to this training yet"
                                    )
                                } else {
                                    tr(
                                        "נוספו כבר ${state.selected.size} תרגילים לאימון הזה",
                                        "${state.selected.size} exercises have already been added to this training"
                                    )
                                },
                            style =
                                KmiTypography.body.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                            color = SummaryTextMuted,
                            textAlign =
                                if (isEnglish) {
                                    TextAlign.Start
                                } else {
                                    TextAlign.End
                                },
                            modifier = Modifier.fillMaxWidth()
                        )

                        FilledTonalButton(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                            onClick = {
                                showAddExercisesSheet = true
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors =
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor =
                                        SummaryPurpleButton,
                                    contentColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary
                                )
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored
                                        .Filled
                                        .PlaylistAddCheck,
                                contentDescription = null,
                                modifier =
                                    Modifier.size(
                                        18.dp * LocalAppIconScale.current
                                    )
                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    tr(
                                        "הוסף תרגילים",
                                        "Add exercises"
                                    ),
                                style =
                                    KmiTypography.action.copy(
                                        fontWeight =
                                            FontWeight.SemiBold
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SummaryDivider,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Spacer(Modifier.height(2.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                }

// -----------------------------
// תרגילים שנבחרו + עריכה (כרטיס מודרני)
// -----------------------------
                if (state.selected.isNotEmpty()) {
                    item {
                        PremiumSummaryCard {
                            SummarySectionHeader(
                                title =
                                    tr(
                                        "התרגילים שנוספו לאימון",
                                        "Exercises added to training"
                                    ),
                                subtitle =
                                    tr(
                                        "ניהול, עריכה והוספת דגשים לכל תרגיל",
                                        "Manage, edit, and add notes for each exercise"
                                    ),
                                icon =
                                    Icons.Filled.FitnessCenter,
                                isEnglish =
                                    isEnglish
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SummaryDivider,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Spacer(Modifier.height(2.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    if (isEnglish) {
                                        Arrangement.Start
                                    } else {
                                        Arrangement.End
                                    },
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            tr(
                                                "סה\"כ ${state.selected.size} תרגילים",
                                                "Total ${state.selected.size} exercises"
                                            )
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.65f
                                        )
                                    )
                                )
                            }

                            val selectedList = state.selected.values.toList()
                                .sortedBy { it.name.lowercase() }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 560.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = true
                            ) {
                                items(selectedList, key = { it.exerciseId }) { ex ->
                                    SelectedExerciseEditor(
                                        item = ex,
                                        onRemove = { vm.removeExercise(ex.exerciseId) },
                                        onHighlight = { vm.setHighlight(ex.exerciseId, it) }
                                    )
                                }
                            }
                        }
                    }
                }

                // -----------------------------
                // סיכום חופשי (מאמן/מתאמן לפי role)
                // -----------------------------
                item {
                    PremiumSummaryCard {
                        SummarySectionHeader(
                            title =
                                tr(
                                    "סיכום כללי",
                                    "General summary"
                                ),
                            subtitle =
                                tr(
                                    "סיכום חופשי של האימון, תחושות, דגשים ומה לשפר",
                                    "Free summary of the training, feelings, highlights, and what to improve"
                                ),
                            icon =
                                Icons.AutoMirrored.Filled.Notes,
                            isEnglish =
                                isEnglish
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SummaryDivider,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Spacer(Modifier.height(2.dp))
                        }

                        OutlinedTextField(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp),
                            value =
                                state.notes,
                            onValueChange = {
                                vm.setNotes(it)
                            },
                            label = {
                                Text(
                                    text =
                                        if (state.isCoach) {
                                            tr(
                                                "דגשים מקצועיים, ביצוע, מה לשפר…",
                                                "Professional notes, performance, what to improve…"
                                            )
                                        } else {
                                            tr(
                                                "איך היה האימון? מה הרגשת? מה לשפר…",
                                                "How was the training? What did you feel? What should be improved…"
                                            )
                                        },
                                    style =
                                        KmiTypography.caption
                                )
                            },
                            minLines = 6,
                            textStyle =
                                KmiTypography.body.copy(
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                ),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedTextColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,
                                    unfocusedTextColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,
                                    focusedBorderColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    unfocusedBorderColor =
                                        MaterialTheme
                                            .colorScheme
                                            .outlineVariant,
                                    focusedLabelColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                    unfocusedLabelColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                    cursorColor =
                                        MaterialTheme
                                            .colorScheme
                                            .primary,
                                    focusedContainerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant,
                                    unfocusedContainerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceVariant
                                )
                        )
                    }
                }

                // -----------------------------
                // שמירה
                // -----------------------------
                item {
                    PremiumSummaryCard {
                        SummarySectionHeader(
                            title =
                                tr(
                                    "שמירה",
                                    "Save"
                                ),
                            subtitle =
                                tr(
                                    "שמור את הסיכום והתרגילים שנוספו לאימון הזה",
                                    "Save the summary and exercises added to this training"
                                ),
                            icon =
                                Icons.Filled.Check,
                            isEnglish =
                                isEnglish
                        )

                        FilledTonalButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            onClick = {
                                val key = "training_summary_days"
                                val cleanIso = state.dateIso.trim().take(10)

                                fun markSummaryDayLocally() {
                                    if (cleanIso.isBlank()) return

                                    val summaryCur =
                                        summarySp
                                            .getStringSet(
                                                key,
                                                emptySet()
                                            )
                                            ?.toMutableSet()
                                            ?: mutableSetOf()

                                    summaryCur.add(cleanIso)

                                    summarySp.edit {
                                        putStringSet(
                                            key,
                                            summaryCur
                                        )

                                        putLong(
                                            "training_summary_days_updated_at",
                                            System.currentTimeMillis()
                                        )
                                    }
                                }

                                vm.save(
                                    onSuccess = {
                                        markSummaryDayLocally()
                                        onBack?.invoke()
                                    },
                                    onError = {
                                        // כרגע Firestore חסום בהרשאות.
                                        // כדי שה-UX לא ייתקע, נסמן מקומית ונחזור למסך הקודם.
                                        markSummaryDayLocally()
                                        onBack?.invoke()
                                    }
                                )
                            },
                            enabled = !state.isSaving,
                            shape = RoundedCornerShape(999.dp),
                            colors =
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor =
                                        SummaryPrimaryButton,
                                    contentColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary
                                )
                        ) {
                            Text(
                                text =
                                    if (state.isSaving) {
                                        tr(
                                            "שומר...",
                                            "Saving..."
                                        )
                                    } else {
                                        tr(
                                            "שמירת סיכום האימון",
                                            "Save training summary"
                                        )
                                    },
                                style =
                                    KmiTypography.action.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(10.dp)) }
            } // LazyColumn

                if (showAddExercisesSheet) {
                    AddExercisesBottomSheet(
                        vm = vm,
                        state = state,
                        initialBelt = belt,
                        beltHebLabel = ::beltHebLabel,
                        isEnglish = isEnglish,
                        onDismiss = { showAddExercisesSheet = false }
                    )
                }

            } // Box
        } // CompositionLocalProvider
    } // Scaffold
} // TrainingSummaryScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExercisesBottomSheet(
    vm: TrainingSummaryViewModel,
    state: TrainingSummaryUiState,
    initialBelt: Belt,
    beltHebLabel: (Belt) -> String,
    isEnglish: Boolean,
    onDismiss: () -> Unit
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedBelt by rememberSaveable(initialBelt) {
        mutableStateOf<Belt?>(
            initialBelt
        )
    }
    var topic by rememberSaveable { mutableStateOf("") }
    var subTopic by rememberSaveable { mutableStateOf("") }

    var pendingPicks by remember {
        mutableStateOf<Map<String, ExercisePickItem>>(
            emptyMap()
        )
    }

    val topics: List<String> = remember(selectedBelt) {
        val belt = selectedBelt ?: return@remember emptyList()

        runCatching {
            KmiSearchBridge
                .topicTitlesFor(belt)
        }
            .getOrDefault(emptyList())
            .ifEmpty {
                runCatching {
                    val sharedBelt =
                        Belt.fromId(belt.id)
                            ?: Belt.WHITE

                    SubTopicRegistry
                        .allForBelt(sharedBelt)
                        .keys
                        .toList()
                }.getOrDefault(emptyList())
            }
    }

    val subTopics: List<String> =
        remember(
            selectedBelt,
            topic
        ) {
            val belt =
                selectedBelt

            if (
                belt == null ||
                topic.isBlank()
            ) {
                return@remember emptyList()
            }

            val normalizedTopic =
                topic.trim()

            runCatching {
                ContentRepo
                    .listSubTopicTitles(
                        belt,
                        topic
                    )
                    .asSequence()
                    .map { value ->
                        value.trim()
                    }
                    .filter { value ->
                        value.isNotBlank()
                    }
                    .filterNot { value ->
                        value == normalizedTopic
                    }
                    .filterNot { value ->
                        value == "כל תתי הנושאים"
                    }
                    .distinct()
                    .toList()
            }.getOrDefault(
                emptyList()
            )
        }

    val rawItems: List<String> = remember(selectedBelt, topic, subTopic, subTopics) {
        val belt = selectedBelt
        if (belt == null || topic.isBlank()) return@remember emptyList()
        if (subTopics.isNotEmpty() && subTopic.isBlank()) return@remember emptyList()

        runCatching {
            ContentRepo.listItemTitles(
                belt = belt,
                topicTitle = topic,
                subTopicTitle = subTopic.ifBlank { null }
            )
        }.getOrDefault(emptyList())
    }

    val displayItems: List<String> = remember(rawItems) {
        rawItems
            .map {
                ExerciseTitleFormatter
                    .displayName(it)
                    .ifBlank { it }
                    .trim()
            }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val filteredItems: List<String> = remember(displayItems) {
        displayItems
    }

    val showTopicField = selectedBelt != null
    val showSubTopicField = showTopicField && topic.isNotBlank() && subTopics.isNotEmpty()
    val showSearchAndItems = showTopicField && topic.isNotBlank() &&
            (subTopics.isEmpty() || subTopic.isNotBlank())

    LaunchedEffect(selectedBelt) {
        topic = ""
        subTopic = ""
        pendingPicks = emptyMap()
        vm.setSearchQuery("")
    }

    LaunchedEffect(topic) {
        subTopic = ""
        pendingPicks = emptyMap()
        vm.setSearchQuery("")
    }

    LaunchedEffect(subTopic) {
        pendingPicks = emptyMap()
        vm.setSearchQuery("")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SummaryCard,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(54.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(SummaryTextMuted.copy(alpha = 0.35f))
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SummaryBgTop,
                            SummaryBgMid1,
                            SummaryCard
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                contentPadding =
                    PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp),
                horizontalAlignment =
                    if (isEnglish) {
                        Alignment.Start
                    } else {
                        Alignment.End
                    }
            ) {
                item {
                    PremiumSummaryCard(
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        SummarySectionHeader(
                            title =
                                tr(
                                    "הוספת תרגילים",
                                    "Add exercises"
                                ),
                            subtitle =
                                tr(
                                    "בחר חגורה, נושא ותת־נושא והוסף תרגילים לאימון",
                                    "Choose belt, topic, and sub-topic and add exercises to training"
                                ),
                            icon =
                                Icons.AutoMirrored
                                    .Filled
                                    .PlaylistAddCheck,
                            isEnglish =
                                isEnglish
                        )
                    }
                }

                item {
                    val beltOptions =
                        Belt.entries
                            .filterNot { it == Belt.WHITE }
                            .map(beltHebLabel)

                    KmiPremiumDropdown(
                        title =
                            tr(
                                "חגורה",
                                "Belt"
                            ),
                        options = beltOptions,
                        selectedValue =
                            selectedBelt
                                ?.let(beltHebLabel)
                                .orEmpty(),
                        isEnglish = isEnglish,
                        placeholder =
                            tr(
                                "בחר חגורה",
                                "Choose belt"
                            ),
                        enabled =
                            beltOptions.size > 1,
                        onSelected = { selectedLabel ->
                            Belt.entries
                                .filterNot {
                                    it == Belt.WHITE
                                }
                                .firstOrNull { beltItem ->
                                    beltHebLabel(beltItem) ==
                                            selectedLabel
                                }
                                ?.let { selectedItem ->
                                    selectedBelt =
                                        selectedItem
                                }
                        }
                    )
                }

                if (showTopicField) {
                    item {
                        KmiPremiumDropdown(
                            title =
                                tr(
                                    "נושא",
                                    "Topic"
                                ),
                            options = topics,
                            selectedValue = topic,
                            isEnglish = isEnglish,
                            placeholder =
                                tr(
                                    "בחר נושא",
                                    "Choose topic"
                                ),
                            enabled =
                                topics.size > 1,
                            onSelected = { selectedTopic ->
                                topic =
                                    selectedTopic
                            }
                        )
                    }
                }

                if (showSubTopicField) {
                    item {
                        KmiPremiumDropdown(
                            title =
                                tr(
                                    "תת-נושא",
                                    "Sub-topic"
                                ),
                            options = subTopics,
                            selectedValue = subTopic,
                            isEnglish = isEnglish,
                            placeholder =
                                tr(
                                    "בחר תת-נושא",
                                    "Choose sub-topic"
                                ),
                            enabled =
                                subTopics.size > 1,
                            onSelected = { selectedSubTopic ->
                                subTopic =
                                    selectedSubTopic
                            }
                        )
                    }
                }

                if (showSearchAndItems) {
                    item {
                        // שדה חיפוש הוסר — מציגים את כל התרגילים לבחירה ברשימה נקייה.
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = SummaryCardInner,
                            tonalElevation = 0.dp,
                            border = BorderStroke(1.dp, SummaryBorder)
                        ) {
                            Text(
                                text =
                                    tr(
                                        "סה״כ ${filteredItems.size} תרגילים · נוספו ${state.selected.size} · ממתינים לאישור ${pendingPicks.size}",
                                        "Total ${filteredItems.size} exercises · added ${state.selected.size} · waiting for approval ${pendingPicks.size}"
                                    ),
                                style =
                                    KmiTypography.secondary.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                color = SummaryTextDark,
                                textAlign =
                                    if (isEnglish) {
                                        TextAlign.Start
                                    } else {
                                        TextAlign.End
                                    },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 10.dp
                                        )
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                if (isEnglish) {
                                    Arrangement.Start
                                } else {
                                    Arrangement.End
                                },
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            if (pendingPicks.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        pendingPicks = emptyMap()
                                    }
                                ) {
                                    Text(
                                        text =
                                            tr(
                                                "נקה בחירה",
                                                "Clear selection"
                                            ),
                                        style =
                                            KmiTypography.action,
                                        color =
                                            SummaryTextDark.copy(
                                                alpha = 0.82f
                                            )
                                    )
                                }

                                Spacer(
                                    Modifier.width(10.dp)
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    pendingPicks.values.forEach { p ->
                                        if (
                                            !state.selected
                                                .containsKey(p.exerciseId)
                                        ) {
                                            vm.toggleExercise(p)
                                        }
                                    }

                                    pendingPicks = emptyMap()
                                    onDismiss()
                                },
                                enabled =
                                    pendingPicks.isNotEmpty(),
                                shape =
                                    RoundedCornerShape(999.dp),
                                colors =
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor =
                                            SummaryPurpleButton,
                                        contentColor =
                                            MaterialTheme
                                                .colorScheme
                                                .onPrimary
                                    )
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(
                                            18.dp * LocalAppIconScale.current
                                        )
                                )

                                Spacer(
                                    Modifier.width(8.dp)
                                )

                                Text(
                                    text =
                                        tr(
                                            "אשר והוסף",
                                            "Confirm and add"
                                        ),
                                    style =
                                        KmiTypography.action.copy(
                                            fontWeight =
                                                FontWeight.Bold
                                        ),
                                    maxLines = 2,
                                    textAlign =
                                        TextAlign.Center
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(18.dp),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surface,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border =
                                BorderStroke(
                                    1.dp,
                                    SummaryBorder
                                )
                        ) {
                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {
                                filteredItems.forEachIndexed { index, name ->
                                    val belt = selectedBelt ?: return@forEachIndexed
                                    val id = "${belt.id}|$topic|$subTopic|$name"
                                    val alreadySelected = state.selected.containsKey(id)
                                    val isPending = pendingPicks.containsKey(id)

                                    ExercisePickRow(
                                        item = ExercisePickItem(
                                            exerciseId = id,
                                            name = name,
                                            topic = if (subTopic.isBlank()) topic else "$topic · $subTopic"
                                        ),
                                        checked = alreadySelected || isPending,
                                        onToggle = {
                                            if (alreadySelected) {
                                                vm.toggleExercise(
                                                    ExercisePickItem(
                                                        exerciseId = id,
                                                        name = name,
                                                        topic = if (subTopic.isBlank()) topic else "$topic · $subTopic"
                                                    )
                                                )
                                            } else {
                                                pendingPicks =
                                                    if (pendingPicks.containsKey(id)) {
                                                        pendingPicks - id
                                                    } else {
                                                        pendingPicks +
                                                                (
                                                                        id to
                                                                                ExercisePickItem(
                                                                                    exerciseId = id,
                                                                                    name = name,
                                                                                    topic =
                                                                                        if (subTopic.isBlank()) {
                                                                                            topic
                                                                                        } else {
                                                                                            "$topic · $subTopic"
                                                                                        }
                                                                                )
                                                                        )
                                                    }
                                            }
                                        }
                                    )

                                    if (index < filteredItems.lastIndex) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp),
                                            color = SummaryDivider,
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Spacer(Modifier.height(1.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingInfoCard(
    dateIso: String,
    branchName: String,
    coachName: String,
    groupKey: String,
    errorText: String?,
    isEnglish: Boolean,
    onOpenCalendar: (() -> Unit)? = null
) {
    fun tr(he: String, en: String): String = if (isEnglish) en else he

    fun normalizedIsoOrNull(raw: String?): String? {
        val clean = raw?.trim().orEmpty()
        if (clean.isBlank()) return null
        if (clean == "{date}") return null
        if (clean.equals("null", ignoreCase = true)) return null

        return runCatching {
            LocalDate.parse(clean)
            clean
        }.getOrNull()
    }

    fun prettyDate(iso: String): String {
        return runCatching {
            val d = LocalDate.parse(iso.trim())
            val fmt = DateTimeFormatter.ofPattern(
                "EEEE, d MMM yyyy",
                if (isEnglish) Locale.US else Locale("he", "IL")
            )
            d.format(fmt)
        }.getOrDefault(iso)
    }

    val validDateIso = normalizedIsoOrNull(dateIso)

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val textDirectionStyle =
        if (isEnglish) {
            TextStyle(
                textDirection = TextDirection.Ltr
            )
        } else {
            TextStyle(
                textDirection = TextDirection.Rtl
            )
        }

    val textAlignPrimary =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val horizontalAlignment =
        if (isEnglish) {
            Alignment.Start
        } else {
            Alignment.End
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SummaryCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            SummaryBorder
        )
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 10.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = horizontalAlignment
                    ) {
                        Text(
                            text =
                                tr(
                                    "פרטי האימון",
                                    "Training details"
                                ),
                            style =
                                KmiTypography.cardTitle
                                    .merge(textDirectionStyle),
                            fontWeight = FontWeight.ExtraBold,
                            color = SummaryTextDark,
                            textAlign = textAlignPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(
                            Modifier.height(2.dp)
                        )

                        Text(
                            text =
                                validDateIso
                                    ?.let {
                                        prettyDate(it)
                                    }
                                    ?: tr(
                                        "יש לבחור תאריך לסיכום האימון",
                                        "Choose a date for the training summary"
                                    ),
                            style =
                                KmiTypography.secondary
                                    .merge(textDirectionStyle),
                            color = SummaryTextMuted,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = textAlignPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier =
                            Modifier
                                .size(38.dp)
                                .background(
                                    brush =
                                        Brush.radialGradient(
                                            listOf(
                                                MaterialTheme
                                                    .colorScheme
                                                    .secondary,
                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                            )
                                        ),
                                    shape =
                                        CircleShape
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary,
                            modifier =
                                Modifier.size(
                                    16.dp * LocalAppIconScale.current
                                )
                        )
                    }
                }

                FilledTonalButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                    onClick = {
                        onOpenCalendar?.invoke()
                    },
                    shape =
                        RoundedCornerShape(999.dp),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor =
                                SummaryPurpleButton,
                            contentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary
                        )
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier =
                            Modifier.size(
                                15.dp * LocalAppIconScale.current
                            ),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text(
                        text =
                            if (validDateIso == null) {
                                tr(
                                    "בחירת תאריך לסיכום האימון",
                                    "Choose training summary date"
                                )
                            } else {
                                tr(
                                    "שינוי תאריך האימון",
                                    "Change training date"
                                )
                            },
                        style =
                            KmiTypography.action.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                    )
                }

                if (validDateIso == null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SummaryCardInner
                    ) {
                        Text(
                            text =
                                tr(
                                    "בחר תאריך בלוח האימונים החודשי כדי להתחיל למלא את סיכום האימון.",
                                    "Choose a date in the monthly training calendar to start filling out the training summary."
                                ),
                            color = SummaryTextDark,
                            style =
                                KmiTypography.body
                                    .merge(textDirectionStyle),
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = textAlignPrimary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 12.dp
                                    )
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SummaryCardInner
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 8.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(6.dp),
                            horizontalAlignment =
                                horizontalAlignment
                        ) {
                            PremiumInfoRow(
                                label = tr("סניף", "Branch"),
                                value = branchName.ifBlank {
                                    tr("לא נמצא סניף", "Branch not found")
                                }
                            )

                            PremiumInfoRow(
                                label = tr("מאמן", "Coach"),
                                value = coachName.ifBlank {
                                    tr("מאמן לא ידוע", "Unknown coach")
                                }
                            )

                            if (groupKey.isNotBlank()) {
                                PremiumInfoRow(
                                    label = tr("קבוצה", "Group"),
                                    value = groupKey
                                )
                            }
                        }
                    }

                    if (!errorText.isNullOrBlank()) {
                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(16.dp),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .errorContainer,
                            border =
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme
                                        .colorScheme
                                        .error
                                        .copy(alpha = 0.55f)
                                )
                        ) {
                            Text(
                                text = errorText,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onErrorContainer,
                                style =
                                    KmiTypography.caption
                                        .merge(
                                            textDirectionStyle
                                        ),
                                fontWeight =
                                    FontWeight.ExtraBold,
                                textAlign =
                                    textAlignPrimary,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumInfoRow(
    label: String,
    value: String
) {
    val context =
        LocalContext.current

    val languageManager =
        remember {
            AppLanguageManager(context)
        }

    val isEnglish =
        languageManager.getCurrentLanguage() ==
                AppLanguage.ENGLISH

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val textAlignPrimary =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val horizontalAlignment =
        if (isEnglish) {
            Alignment.Start
        } else {
            Alignment.End
        }

    val textDirectionStyle =
        if (isEnglish) {
            TextStyle(
                textDirection = TextDirection.Ltr
            )
        } else {
            TextStyle(
                textDirection = TextDirection.Rtl
            )
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalAlignment =
                horizontalAlignment,
            verticalArrangement =
                Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                style =
                    KmiTypography.caption
                        .merge(textDirectionStyle),
                color = SummaryTextMuted,
                fontWeight = FontWeight.Bold,
                textAlign = textAlignPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = value,
                style =
                    KmiTypography.body
                        .merge(textDirectionStyle),
                color = SummaryTextDark,
                fontWeight = FontWeight.ExtraBold,
                textAlign = textAlignPrimary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ExercisePickRow(
    item: ExercisePickItem,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val languageManager = remember { AppLanguageManager(context) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH
    val textAlignPrimary = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalEnd = if (isEnglish) Alignment.Start else Alignment.End

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    if (checked) {
                        SummaryChipSelected.copy(
                            alpha = 0.38f
                        )
                    } else {
                        Color.Transparent
                    }
                )
                .clickable {
                    onToggle()
                }
                .padding(
                    horizontal = 12.dp,
                    vertical = 7.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            if (isEnglish) {
                Arrangement.Start
            } else {
                Arrangement.End
            }
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = horizontalEnd
        ) {
            Text(
                text = item.name,
                style =
                    KmiTypography.body.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                color = SummaryTextDark,
                textAlign = textAlignPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                Modifier.height(2.dp)
            )

            Text(
                text = item.topic,
                style =
                    KmiTypography.caption.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                color = SummaryTextMuted,
                textAlign = textAlignPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.width(10.dp))

        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.scale(0.82f)
        )
    }
}

@Composable
private fun SelectedExerciseEditor(
    item: SelectedExerciseUi,
    onRemove: () -> Unit,
    onHighlight: (String) -> Unit
) {
    val context = LocalContext.current
    val languageManager = remember { AppLanguageManager(context) }
    val isEnglish = languageManager.getCurrentLanguage() == AppLanguage.ENGLISH

    fun tr(
        he: String,
        en: String
    ): String =
        if (isEnglish) {
            en
        } else {
            he
        }

    val textAlignPrimary =
        if (isEnglish) {
            TextAlign.Left
        } else {
            TextAlign.Right
        }

    val titleDirectionStyle =
        if (isEnglish) {
            TextStyle(
                textDirection = TextDirection.Ltr
            )
        } else {
            TextStyle(
                textDirection = TextDirection.Rtl
            )
        }

    var notesOpen by rememberSaveable(item.exerciseId) {
        mutableStateOf(item.highlight.isNotBlank())
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        color = SummaryCardInner,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            SummaryBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.name,
                style =
                    KmiTypography.cardTitle
                        .merge(titleDirectionStyle),
                fontWeight = FontWeight.ExtraBold,
                color = SummaryTextDark,
                textAlign = textAlignPrimary,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { notesOpen = !notesOpen },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        contentColor = SummaryTextDark
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = if (notesOpen) {
                            tr("סגור הערות", "Close notes")
                        } else {
                            tr("פתח הערות", "Open notes")
                        }
                    )
                }

                Spacer(Modifier.width(8.dp))

                OutlinedIconButton(
                    onClick = onRemove,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.60f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = tr("מחק תרגיל", "Delete exercise"),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SummaryDivider,
                shape = RoundedCornerShape(999.dp)
            ) {
                Spacer(Modifier.height(1.5.dp))
            }

            if (!notesOpen && item.highlight.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = SummaryChip
                ) {
                    Text(
                        text = item.highlight,
                        style = KmiTypography.body,
                        color = SummaryTextDark,
                        textAlign = textAlignPrimary,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (notesOpen) {
                OutlinedTextField(
                    modifier =
                        Modifier.fillMaxWidth(),
                    value =
                        item.highlight,
                    onValueChange = {
                        onHighlight(it)
                    },
                    label = {
                        Text(
                            text =
                                tr(
                                    "דגשים והערות לתרגיל",
                                    "Exercise notes and highlights"
                                ),
                            style =
                                KmiTypography.caption
                        )
                    },
                    minLines = 3,
                    textStyle =
                        KmiTypography.body.copy(
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        ),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface,
                            unfocusedTextColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface,
                            focusedBorderColor =
                                MaterialTheme
                                    .colorScheme
                                    .primary,
                            unfocusedBorderColor =
                                MaterialTheme
                                    .colorScheme
                                    .outlineVariant,
                            focusedLabelColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface,
                            unfocusedLabelColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            cursorColor =
                                MaterialTheme
                                    .colorScheme
                                    .primary,
                            focusedContainerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface,
                            unfocusedContainerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface
                        )
                )
            }
        }
    }
}

/* =========================
   Home “source of truth”
   ========================= */

private class HomeScheduleTruth(
    private val sp: SharedPreferences
) {
    private val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    private fun readSelectedBranches(): List<String> {
        fun splitBranchValue(raw: String?): List<String> {
            val clean = raw?.trim().orEmpty()
            if (clean.isBlank()) return emptyList()

            return if (clean.startsWith("[")) {
                runCatching {
                    val arr = JSONArray(clean)
                    (0 until arr.length())
                        .mapNotNull { index -> arr.optString(index, null) }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                }.getOrDefault(emptyList())
            } else {
                clean
                    .split(',', ';', '|', '\n')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
        }

        return buildList {
            addAll(splitBranchValue(sp.getString("branches_json", null)))
            addAll(splitBranchValue(sp.getString("branches", null)))
            addAll(splitBranchValue(sp.getString("branch", null)))
            addAll(splitBranchValue(sp.getString("active_branch", null)))
            addAll(splitBranchValue(sp.getString("activeBranch", null)))
            addAll(splitBranchValue(sp.getString("selected_branches", null)))
            addAll(splitBranchValue(sp.getString("branch2", null)))
            addAll(splitBranchValue(sp.getString("branch3", null)))
        }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    data class TrainingTruth(
        val branchName: String,
        val groupKey: String,
        val coachName: String
    )

    // ✅ מקור אמת מלא לפי תאריך: סניף + קבוצה + מאמן
    fun trainingForDate(
        dateIso: String,
        preferredBranch: String = "",
        preferredGroup: String = "",
        preferredTime: String = ""
    ): TrainingTruth? {
        val date =
            runCatching {
                LocalDate.parse(
                    dateIso.trim(),
                    isoFmt
                )
            }.getOrNull()
                ?: return null

        val wantedDow =
            date.toCalendarDow()

        fun timeText(
            training: TrainingData
        ): String {
            val hour =
                training.cal.get(
                    java.util.Calendar.HOUR_OF_DAY
                )

            val minute =
                training.cal.get(
                    java.util.Calendar.MINUTE
                )

            return String.format(
                Locale.US,
                "%02d:%02d",
                hour,
                minute
            )
        }

        fun matchesWantedDay(
            training: TrainingData
        ): Boolean {
            return training.cal.get(
                java.util.Calendar.DAY_OF_WEEK
            ) == wantedDow
        }

        fun matchesWantedTime(
            training: TrainingData
        ): Boolean {
            return preferredTime.isBlank() ||
                    timeText(training) ==
                    preferredTime.trim()
        }

        if (preferredBranch.isNotBlank()) {
            val normalizedPreferredGroup =
                TrainingCatalog
                    .normalizeGroupName(
                        preferredGroup
                    )
                    .ifBlank {
                        preferredGroup
                    }

            val preferredTraining =
                TrainingCatalog
                    .trainingsFor(
                        preferredBranch,
                        normalizedPreferredGroup
                            .ifBlank {
                                null
                            }
                    )
                    .firstOrNull { training ->
                        matchesWantedDay(training) &&
                                matchesWantedTime(training)
                    }
                    ?: TrainingCatalog
                        .trainingsFor(
                            preferredBranch,
                            null
                        )
                        .firstOrNull { training ->
                            matchesWantedDay(training) &&
                                    matchesWantedTime(training)
                        }

            if (preferredTraining != null) {
                val coach =
                    preferredTraining.coach
                        .trim()
                        .ifBlank {
                            defaultCoachName()
                                .orEmpty()
                        }

                return TrainingTruth(
                    branchName =
                        preferredBranch,
                    groupKey =
                        normalizedPreferredGroup,
                    coachName =
                        coach
                )
            }
        }

        val branches =
            readSelectedBranches()

        val groups =
            groupsEffective()

        for (branchName in branches) {

            for (grp in groups) {
                val matchingTraining =
                    TrainingCatalog
                        .trainingsFor(
                            branchName,
                            grp
                        )
                        .firstOrNull { training ->
                            matchesWantedDay(training)
                        }

                if (matchingTraining != null) {
                    val coach =
                        matchingTraining.coach
                            .trim()
                            .ifBlank {
                                defaultCoachName()
                                    .orEmpty()
                            }

                    return TrainingTruth(
                        branchName = branchName,
                        groupKey = grp,
                        coachName = coach
                    )
                }
            }

            val fallbackTraining =
                TrainingCatalog
                    .trainingsFor(
                        branchName,
                        null
                    )
                    .firstOrNull { training ->
                        matchesWantedDay(training)
                    }

            if (fallbackTraining != null) {
                val coach =
                    fallbackTraining.coach
                        .trim()
                        .ifBlank {
                            defaultCoachName()
                                .orEmpty()
                        }

                return TrainingTruth(
                    branchName = branchName,
                    groupKey =
                        groups
                            .firstOrNull()
                            .orEmpty(),
                    coachName = coach
                )
            }
        }

        return null
    }

    private fun groupsEffective(): List<String> {

        fun splitGroupValue(raw: String?): List<String> {
            val clean = raw?.trim().orEmpty()

            if (clean.isBlank()) {
                return emptyList()
            }

            return if (clean.startsWith("[")) {
                runCatching {
                    val arr = JSONArray(clean)

                    (0 until arr.length())
                        .mapNotNull { index ->
                            arr.optString(index, null)
                        }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                }.getOrDefault(emptyList())
            } else {
                clean
                    .split(',', ';', '|', '\n')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
        }

        return buildList {
            addAll(
                splitGroupValue(
                    sp.getString("groups_json", null)
                )
            )

            addAll(
                splitGroupValue(
                    sp.getString("selected_groups", null)
                )
            )

            addAll(
                splitGroupValue(
                    sp.getString("groups", null)
                )
            )

            addAll(
                splitGroupValue(
                    sp.getString("age_groups", null)
                )
            )

            addAll(
                splitGroupValue(
                    sp.getString("active_group", null)
                )
            )

            addAll(
                splitGroupValue(
                    sp.getString("age_group", null)
                )
            )

            addAll(
                splitGroupValue(
                    sp.getString("group", null)
                )
            )
        }
            .map { group ->
                TrainingCatalog
                    .normalizeGroupName(group)
                    .ifBlank {
                        group
                    }
            }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // ✅ שם מאמן ברירת מחדל מתוך SharedPreferences (מפתחות נפוצים)
    fun defaultCoachName(): String? {
        val candidates = listOf(
            "coach_name",
            "coachName",
            "trainer_name",
            "trainerName",
            "coach",
            "trainer"
        )
        for (k in candidates) {
            val v = sp.getString(k, null)?.trim()
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    private fun LocalDate.toCalendarDow(): Int {
        return when (this.dayOfWeek) {
            java.time.DayOfWeek.SUNDAY -> java.util.Calendar.SUNDAY
            java.time.DayOfWeek.MONDAY -> java.util.Calendar.MONDAY
            java.time.DayOfWeek.TUESDAY -> java.util.Calendar.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> java.util.Calendar.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> java.util.Calendar.THURSDAY
            java.time.DayOfWeek.FRIDAY -> java.util.Calendar.FRIDAY
            java.time.DayOfWeek.SATURDAY -> java.util.Calendar.SATURDAY
        }
    }
}